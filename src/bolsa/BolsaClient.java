package bolsa;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class BolsaClient {
    private static BolsaService servico;
    private static ClientCallback callback;
    private static volatile boolean servidorOnline = false;

    // Cores ANSI para deixar bonito
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(CYAN + "=== BEM VINDO A CORRETORA ===" + RESET);
        System.out.print("Qual IP do servidor do seu amigo? (Aperte ENTER se localhost): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "127.0.0.1";

        if (!conectar(host)) {
            System.out.println(RED + "Erro Critico: Nao foi possivel conectar ao servidor RMI." + RESET);
            return;
        }

        // --- TOLERANCIA A FALHAS E DETECCAO EM TEMPO REAL ---
        Thread pingThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000); 
                    if (servico != null && servidorOnline) {
                        servico.ping();
                    }
                } catch (Exception e) {
                    if (servidorOnline) {
                        servidorOnline = false;
                        System.out.println(RED + "\n\n#####################################################");
                        System.out.println("   ALERTA URGENTE: A CORRETORA SAIU DO AR / CAIU!");
                        System.out.println("   Aguarde ela voltar ou reinicie o aplicativo.");
                        System.out.println("#####################################################\n" + RESET);
                        System.out.print(YELLOW + "Escolha uma opcao (Comandos bloqueados): " + RESET);
                    }
                }
            }
        });
        pingThread.setDaemon(true);
        pingThread.start();

        // Loop de Menu
        while (true) {
            try {
                System.out.println(CYAN + "\n--- MODULO CLIENTE ---" + RESET);
                System.out.println("1. Listar todas Acoes do Mercado");
                System.out.println("2. Consultar Preco de 1 Acao Especifica");
                System.out.println("3. Atualizar Preco Livremente (Sem Token)");
                System.out.println("4. Sair do App");
                System.out.print(YELLOW + "Escolha uma opcao: " + RESET);
                
                String opcao = scanner.nextLine().trim();
                
                if (!servidorOnline && !opcao.equals("4")) {
                    System.out.println(RED + "Acao bloqueada! Voce esta offline do servidor." + RESET);
                    continue;
                }

                if (opcao.equals("1")) {
                    List<Acao> acoes = servico.listarAcoes();
                    System.out.println(GREEN + "\n>> MERCADO ATUAL:" + RESET);
                    for (Acao acao : acoes) {
                        System.out.println(acao);
                    }
                } else if (opcao.equals("2")) {
                    System.out.print("Digite a Acao (ex: BTC): ");
                    String sigla = scanner.nextLine().toUpperCase();
                    double p = servico.consultarPreco(sigla);
                    if (p == -1) System.out.println(RED + "Essa acao nao existe na corretora." + RESET);
                    else System.out.println(GREEN + "PRECO DO " + sigla + ": R$ " + String.format("%.2f", p) + RESET);
                    
                } else if (opcao.equals("3")) {
                    System.out.print("Digite Qual Acao vai atualizar: ");
                    String sig = scanner.nextLine().toUpperCase();
                    System.out.print("Digite o novo preco R$: ");
                    
                    try {
                        double val = Double.parseDouble(scanner.nextLine());
                        boolean ok = servico.atualizarPreco(sig, val); 
                        if (!ok) System.out.println(RED + "Erro: Essa acao nao existe." + RESET);
                    } catch (NumberFormatException nfe) {
                        System.out.println(RED + "Ops, este valor nao e um numero valido." + RESET);
                    }
                    
                } else if (opcao.equals("4")) {
                    System.out.println(CYAN + "Saindo da corretora..." + RESET);
                    if (servidorOnline) {
                         try { servico.removerCliente(callback); } catch (Exception ignored) {}
                    }
                    System.exit(0);
                } else {
                    System.out.println(RED + "Opcao errada..." + RESET);
                }
            } catch (Exception e) {
                if (servidorOnline) {
                     servidorOnline = false;
                     System.out.println(RED + "\nA operacao falhou na hora do envio. Queda detectada de conexao." + RESET);
                }
            }
        }
    }

    private static boolean conectar(String host) {
        try {
            String meuIpLocal = java.net.InetAddress.getLocalHost().getHostAddress();
            System.setProperty("java.rmi.server.hostname", meuIpLocal); 
            
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
