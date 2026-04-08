package bolsa;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BolsaServiceImpl extends UnicastRemoteObject implements BolsaService {
    private static final long serialVersionUID = 1L;
    private Map<String, Acao> acoes;
    // Tabela mapeando o Callback (Referencia) -> IP do Infeliz pra sabermos quem foi!
    private Map<ClientCallback, String> clientesIps;
    private final String ARQUIVO_DADOS = "bd_corretora.dat";

    public BolsaServiceImpl() throws RemoteException {
        super();
        clientesIps = new ConcurrentHashMap<>();
        carregarAcoes();
        
        // --- THREAD INSPETORA DO SERVIDOR (Heartbeat Ativo) ---
        Thread inspetor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    verificarClientesDerrubados();
                } catch (InterruptedException e) {}
            }
        });
        inspetor.setDaemon(true);
        inspetor.start();
    }

    private String capturarIP() {
        try {
            return java.rmi.server.RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "Desconhecido";
        }
    }
    
    private synchronized void verificarClientesDerrubados() {
        List<ClientCallback> inativos = new ArrayList<>();
        // Tenta dar 1 ping em todo mundo que ta logado
        for (ClientCallback c : clientesIps.keySet()) {
            try {
                c.ping();
            } catch (RemoteException e) {
                inativos.add(c);
            }
        }
        
        for (ClientCallback fantasma : inativos) {
            String ipInativo = clientesIps.remove(fantasma);
            if (ipInativo == null) ipInativo = "Desconhecido";

            System.out.println("O Inspetor notou que [IP: " + ipInativo + "] Caiu/Deu CTRL-C no Terminal! Total online agora: " + clientesIps.size());
            notificarTodos("[IP " + ipInativo + "] caiu, fechou ou disconectou repentinamente a sua maquina da rede!");
        }
    }

    private void salvarAcoes() {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(ARQUIVO_DADOS))) {
            oos.writeObject(acoes);
        } catch (Exception e) {}
    }

    @SuppressWarnings("unchecked")
    private void carregarAcoes() {
        java.io.File file = new java.io.File(ARQUIVO_DADOS);
        if (file.exists()) {
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(ARQUIVO_DADOS))) {
                acoes = (Map<String, Acao>) ois.readObject();
                System.out.println("--> Banco de DADOS VINCULADO! (" + acoes.size() + " acoes carregadas).");
            } catch (Exception e) {
                iniciarAcoesPadrao();
            }
        } else {
            iniciarAcoesPadrao();
        }
    }
    
    private void iniciarAcoesPadrao() {
        acoes = new ConcurrentHashMap<>();
        acoes.put("BTC", new Acao("BTC", 350000.0));
        acoes.put("ETH", new Acao("ETH", 15000.0));
        acoes.put("SOL", new Acao("SOL", 800.0));
        salvarAcoes();
    }

    @Override
    public void ping() throws RemoteException { }

    @Override
    public double consultarPreco(String simbolo) throws RemoteException {
        System.out.println("[LOG SERVIDOR - " + capturarIP() + "] Cliente Consultou Sigla (Apenas Visao): " + simbolo.toUpperCase());
        Acao acao = acoes.get(simbolo.toUpperCase());
        if (acao != null) return acao.getPreco();
        return -1;
    }

    @Override
    public List<Acao> listarAcoes() throws RemoteException {
        System.out.println("[LOG SERVIDOR - " + capturarIP() + "] Cliente listou Todos os ativos Gerais do Mercado");
        return new ArrayList<>(acoes.values());
    }

    @Override
    public boolean atualizarPreco(String simbolo, double novoPreco) throws RemoteException {
        String ipTrator = capturarIP();
        System.out.println("[LOG SERVIDOR - " + ipTrator + "] Manipulando Mercado e Alterando Preco da " + simbolo.toUpperCase() + " para: R$ " + novoPreco);
        Acao acao = acoes.get(simbolo.toUpperCase());
        if (acao != null && novoPreco > 0) {
            double precoAntigo = acao.getPreco();
            acao.setPreco(novoPreco);
            salvarAcoes(); 
            
            String msg = "[Alterado via IP " + ipTrator + "] A acao " + simbolo.toUpperCase() + " sofreu alteracao! De R$ " + 
                         String.format("%.2f", precoAntigo) + " para R$ " + String.format("%.2f", novoPreco) + "!";
            notificarTodos(msg);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean criarAcao(String simbolo, double precoInicial) throws RemoteException {
        String ipTrator = capturarIP();
        System.out.println("[LOG SERVIDOR - " + ipTrator + "] Lancando (IPO) e Criando a Nova Acao " + simbolo.toUpperCase() + " na Bolsa!");
        simbolo = simbolo.toUpperCase();
        if (!acoes.containsKey(simbolo) && precoInicial > 0) {
            acoes.put(simbolo, new Acao(simbolo, precoInicial));
            salvarAcoes(); 
            
            String msg = "[Cadastrado pelo IP " + ipTrator + "] Uma NOVA acao foi listada na Corretora! A " + simbolo + " estreou cotada a R$ " + String.format("%.2f", precoInicial) + "!";
            notificarTodos(msg); 
            return true;
        }
        return false;
    }

    @Override
    public boolean excluirAcao(String simbolo) throws RemoteException {
        String ipTrator = capturarIP();
        System.out.println("[LOG SERVIDOR - " + ipTrator + "] Deletou Completamente a acao " + simbolo.toUpperCase() + " de nossas limitacoes!");
        simbolo = simbolo.toUpperCase();
        if (acoes.remove(simbolo) != null) {
            salvarAcoes(); 
            String msg = "[Atingida pelo IP " + ipTrator + "] A acao " + simbolo + " acaba de ser EXCLUIDA da Bolsa e as negociacoes dela foram encerradas!";
            notificarTodos(msg);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void registrarCliente(ClientCallback cliente) throws RemoteException {
        String ipTrator = capturarIP();
        if (!clientesIps.containsKey(cliente)) {
            // Emite pro resto ANTES de botar ele pra ele nao receber!
            notificarTodos("[IP " + ipTrator + "] acaba de acessar e plugar terminal na Corretora!");
            
            clientesIps.put(cliente, ipTrator);
            System.out.println("Novo Conectado [IP: " + ipTrator + "]! Total online operando agora: " + clientesIps.size());
        }
    }

    @Override
    public synchronized void removerCliente(ClientCallback cliente) throws RemoteException {
        String ipQueSaiu = clientesIps.remove(cliente);
        if (ipQueSaiu != null) {
            notificarTodos("[IP " + ipQueSaiu + "] deslogou pacificamente com o Comando (6) e saiu da Corretora!");
            System.out.println("Saida Registrada [IP: " + ipQueSaiu + "] Comando(6). Total online agora: " + clientesIps.size());
        }
    }

    private synchronized void notificarTodos(String mensagem) {
        List<ClientCallback> inativos = new ArrayList<>();
        // Atravessa o mapa inteiro e dispara callback
        for (ClientCallback c : clientesIps.keySet()) {
            try {
                c.receberMensagemGeral(mensagem);
            } catch (RemoteException e) {
                inativos.add(c);
            }
        }
        // Se durante a NOTIFICACAO a gente der erro ao enviar, tira eles do mapa
        for(ClientCallback lixo : inativos) {
            clientesIps.remove(lixo);
        }
    }
}
