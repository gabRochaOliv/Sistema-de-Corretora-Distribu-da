package bolsa;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class BolsaClient {
    private static BolsaService servico;
    private static ClientCallback callback;
    private static volatile boolean servidorOnline = false;

    // Cores ANSI
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    public static void imprimirMenu() {
        System.out.println(CYAN + "\n--- MODULO CLIENTE ---" + RESET);
        System.out.println("1. Listar todas Acoes do Mercado Atual");
        System.out.println("2. Consultar Preco de 1 Acao");
        System.out.println("3. Atualizar/Variar Preco Livremente");
        System.out.println("4. Criar e Listar NOVA Acao na Bolsa");
        System.out.println("5. Excluir uma Acao da Bolsa");
        System.out.println("6. Sair da Sessao");
        System.out.print(YELLOW + "Sua opcao: " + RESET);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(CYAN + "=== BEM VINDO A CORRETORA INTERATIVA ===" + RESET);
        System.out.print("Qual IP do servidor publico do alvo? (Aperte ENTER se localhost): ");
        String hostTmp = scanner.nextLine().trim();
        final String host = hostTmp.isEmpty() ? "127.0.0.1" : hostTmp;

        if (!conectar(host)) {
            System.out.println(RED + "Aviso: O servidor parece estar fora do ar neste momento inicial." + RESET);
            System.out.println(YELLOW + "Aguarde, tentando reconectar automaticamente em background..." + RESET);
        }

        // --- SISTEMA ANTI-QUEDA VERTICAL (Captura CTRL+C e fechamento do terminal no X) ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (servidorOnline && servico != null && callback != null) {
                try {
                    servico.removerCliente(callback);
                } catch (Exception ignored) {}
            }
        }));

        Thread pingThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000); 
                    if (servidorOnline) {
                        try {
                            servico.ping();
                        } catch (Exception e) {
                            servidorOnline = false;
                            System.out.println(RED + "\n\n#####################################################");
                            System.out.println("   ALERTA CRITICO: A CONEXAO COM O SERVIDOR CAIU!");
                            System.out.println("   Tentando reconectar...");
                            System.out.println("#####################################################\n" + RESET);
                        }
                    } else {
                        if (conectar(host)) {
                            System.out.println(GREEN + "\n#####################################################");
                            System.out.println("   CONEXAO RE-ESTABELECIDA! SERVIDOR DE VOLTA AO AR!");
                            System.out.println("#####################################################\n" + RESET);
                            imprimirMenu(); 
                        }
                    }
                } catch (InterruptedException ie) {}
            }
        });
        pingThread.setDaemon(true);
        pingThread.start();

        // Print inicial no começo da aplicação inteira
        if (servidorOnline) {
            imprimirMenu();
        }

        while (true) {
            try {
                String opcao = scanner.nextLine().trim();
                
                if (!servidorOnline && !opcao.equals("6")) {
                    continue;
                }

                if (opcao.equals("1")) {
                    List<Acao> acoes = servico.listarAcoes();
                    if (acoes.isEmpty()) {
                        System.out.println(YELLOW + "Mercado parado - Nenhuma acao listada atualmente." + RESET);
                    } else {
                        System.out.println(GREEN + "\n>> BOLSA DE VALORES HOJE:" + RESET);
                        for (Acao acao : acoes) {
                            System.out.println(acao);
                        }
                    }
                    imprimirMenu();
                    
                } else if (opcao.equals("2")) {
                    System.out.print("Me de a sua Sigla (ex: BTC): ");
                    String sigla = scanner.nextLine().toUpperCase();
                    
                    double p = servico.consultarPreco(sigla);
                    if (p == -1) {
                         System.out.println(RED + "Erro: Nao existe acao com a sigla '" + sigla + "'." + RESET);
                    } else {
                         System.out.println(GREEN + "VALOR DO ATIVO - " + sigla + ": R$ " + String.format("%.2f", p) + RESET);
                    }
                    imprimirMenu();
                    
                } else if (opcao.equals("3")) {
                    System.out.print("Sigla da acao a ser modificada: ");
                    String sig = scanner.nextLine().toUpperCase();
                    
                    // Tratamento Instantâneo de Erro - Verificar antes de perguntar o valor
                    if (servico.consultarPreco(sig) == -1) {
                        System.out.println(RED + "Erro Critico: Essa Acao ('" + sig + "') NÃO existe no Banco de Dados! Operacao Interrompida." + RESET);
                        imprimirMenu();
                        continue;
                    }
                    
                    System.out.print("Qual vai ser o novo Preco R$: ");
                    try {
                        double val = Double.parseDouble(scanner.nextLine());
                        boolean ok = servico.atualizarPreco(sig, val);
                        if (!ok) {
                            System.out.println(RED + "Falha desconhecida ao atualizar acao." + RESET);
                            imprimirMenu();
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println(RED + "Erro de Digitacao: Você inseriu Letras ao inves do Preco Numerico da Acao!" + RESET);
                        imprimirMenu();
                    }
                    
                } else if (opcao.equals("4")) {
                    System.out.print("Escolha a Sigla Unica (Ex: PETR4, NVDA): ");
                    String sig = scanner.nextLine().toUpperCase();
                    
                    // Tratamento Instantâneo de Erro - Se a acao ja tem preco, já existe!
                    if (servico.consultarPreco(sig) != -1) {
                        System.out.println(RED + "Erro Critico: Plagio - Essa Acao ('" + sig + "') JA EXISTE no Banco de Dados e esta sendo negociada!" + RESET);
                        imprimirMenu();
                        continue;
                    }
                    
                    System.out.print("Lance o precificamento inicial (IPO) dela para o mercado R$: ");
                    try {
                        double val = Double.parseDouble(scanner.nextLine());
                        boolean ok = servico.criarAcao(sig, val);
                        if (!ok) {
                            System.out.println(RED + "Erro desconhecido ao cadastrar na bolsa." + RESET);
                            imprimirMenu();
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println(RED + "Erro de Digitacao: Você inseriu Letras ao inves do Preco Numerico da Acao!" + RESET);
                        imprimirMenu();
                    }

                } else if (opcao.equals("5")) {
                    System.out.print("Digite a Sigla da acao a ser EXPULSA DA BOLSA: ");
                    String sig = scanner.nextLine().toUpperCase();
                    
                    // Tratamento Instantâneo de Erro - Ver antes de expulsar fantasmas!
                    if (servico.consultarPreco(sig) == -1) {
                         System.out.println(RED + "Erro Critico: Ninguém pode excluir uma Acao ('" + sig + "') que NUNCA FOI CADASTRADA!" + RESET);
                         imprimirMenu();
                         continue;
                    }
                    
                    boolean ok = servico.excluirAcao(sig);
                    if (!ok) {
                        System.out.println(RED + "Erro ao processar delete interno!" + RESET);
                        imprimirMenu();
                    }

                } else if (opcao.equals("6")) {
                    System.out.println(CYAN + "Logout iniciado com sucesso!" + RESET);
                    if (servidorOnline) {
                         try { servico.removerCliente(callback); } catch (Exception ignored) {}
                    }
                    System.exit(0);
                } else {
                    if (!opcao.isEmpty()) {
                        System.out.println(RED + "Digite somente os numeros da lista!" + RESET);
                        imprimirMenu();
                    }
                }
            } catch (Exception e) {
                if (servidorOnline) {
                     servidorOnline = false;
                }
            }
        }
    }

    private static boolean conectar(String host) {
        try {
            try (java.net.Socket socket = new java.net.Socket(host, 1099)) {
                String meuIpReal = socket.getLocalAddress().getHostAddress();
                System.setProperty("java.rmi.server.hostname", meuIpReal);
            } catch (Exception socketException) {
                String padrao = java.net.InetAddress.getLocalHost().getHostAddress();
                System.setProperty("java.rmi.server.hostname", padrao); 
            }
            
            Registry registry = LocateRegistry.getRegistry(host, 1099);
            servico = (BolsaService) registry.lookup("BolsaService");
            
            if (callback == null) {
                callback = new ClientCallbackImpl();
            }
            servico.registrarCliente(callback);
            servidorOnline = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
