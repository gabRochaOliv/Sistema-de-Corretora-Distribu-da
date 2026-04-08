package bolsa;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BolsaServiceImpl extends UnicastRemoteObject implements BolsaService {
    private static final long serialVersionUID = 1L;
    private Map<String, Acao> acoes;
    private List<ClientCallback> clientes;
    private final String ARQUIVO_DADOS = "bd_corretora.dat";

    public BolsaServiceImpl() throws RemoteException {
        super();
        clientes = new ArrayList<>();
        carregarAcoes();
        
        // --- THREAD INSPETORA DO SERVIDOR (Heartbeat Ativo) ---
        // O servidor procura ativamente a cada X segundos se alguem fechou o terminal no CTRL+C
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
    
    private synchronized void verificarClientesDerrubados() {
        List<ClientCallback> inativos = new ArrayList<>();
        // Tenta dar 1 ping em todo mundo que a gente acha que ta online
        for (ClientCallback c : new ArrayList<>(clientes)) {
            try {
                c.ping();
            } catch (RemoteException e) {
                // Se der RemoteException nesse ping, o cara morreu / deu CTRL+C
                inativos.add(c);
            }
        }
        
        for (ClientCallback fantasma : inativos) {
            clientes.remove(fantasma);
            System.out.println("O Inspetor notou que um Cliente Caiu/Deu CTRL-C no Terminal! Total online agora: " + clientes.size());
            notificarTodos("Um cliente investidor caiu ou fechou o terminal repentinamente!");
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
        System.out.println("[LOG SERVIDOR] Cliente Vendo/Consultando apenas o preco da acao: " + simbolo.toUpperCase());
        Acao acao = acoes.get(simbolo.toUpperCase());
        if (acao != null) return acao.getPreco();
        return -1;
    }

    @Override
    public List<Acao> listarAcoes() throws RemoteException {
        System.out.println("[LOG SERVIDOR] Cliente Acionou/Listou Todas as Acoes de todo o Mercado!");
        return new ArrayList<>(acoes.values());
    }

    @Override
    public boolean atualizarPreco(String simbolo, double novoPreco) throws RemoteException {
        System.out.println("[LOG SERVIDOR] Cliente Disparou Comando p/ Editar Valor da Acao " + simbolo.toUpperCase() + " para: R$ " + novoPreco);
        Acao acao = acoes.get(simbolo.toUpperCase());
        if (acao != null) {
            double precoAntigo = acao.getPreco();
            acao.setPreco(novoPreco);
            salvarAcoes(); 
            
            String msg = "A acao " + simbolo.toUpperCase() + " sofreu uma alteracao! De R$ " + 
                         String.format("%.2f", precoAntigo) + " para R$ " + String.format("%.2f", novoPreco) + "!";
            notificarTodos(msg);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean criarAcao(String simbolo, double precoInicial) throws RemoteException {
        System.out.println("[LOG SERVIDOR] Cliente Disparou I.P.O Criando Inserindo a Nova Acao " + simbolo.toUpperCase() + " na Bolsa!");
        simbolo = simbolo.toUpperCase();
        if (!acoes.containsKey(simbolo)) {
            acoes.put(simbolo, new Acao(simbolo, precoInicial));
            salvarAcoes(); 
            
            String msg = "Uma NOVA acao foi listada na Corretora! A " + simbolo + " estreou cotada a R$ " + String.format("%.2f", precoInicial) + "!";
            notificarTodos(msg); 
            return true;
        }
        return false;
    }

    @Override
    public boolean excluirAcao(String simbolo) throws RemoteException {
        System.out.println("[LOG SERVIDOR] Cliente Assassino Deletou e Excluiu Completamente a acao " + simbolo.toUpperCase() + " de nossas operacoes!");
        simbolo = simbolo.toUpperCase();
        if (acoes.remove(simbolo) != null) {
            salvarAcoes(); 
            String msg = "A acao " + simbolo + " acaba de ser EXCLUIDA da Bolsa e as negociacoes dela foram encerradas!";
            notificarTodos(msg);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void registrarCliente(ClientCallback cliente) throws RemoteException {
        if (!clientes.contains(cliente)) {
            notificarTodos("Um novo cliente investidor acaba de acessar e plugar na Corretora!");
            clientes.add(cliente);
            System.out.println("Novo cliente investidor Acessou/Plugou! Total online operando agora: " + clientes.size());
        }
    }

    @Override
    public synchronized void removerCliente(ClientCallback cliente) throws RemoteException {
        if (clientes.remove(cliente)) {
            notificarTodos("Um cliente investidor deslogou pacificamente com o Comando(6) e saiu da Corretora!");
            System.out.println("Um Cliente escolheu sair do Terminal com Comando (6). Total online agora: " + clientes.size());
        }
    }

    private synchronized void notificarTodos(String mensagem) {
        List<ClientCallback> inativos = new ArrayList<>();
        for (ClientCallback c : new ArrayList<>(clientes)) {
            try {
                c.receberMensagemGeral(mensagem);
            } catch (RemoteException e) {
                inativos.add(c);
            }
        }
        // Se durante a NOTIFICACAO acharmos um fantasma, só arranca calado, 
        // pois a Thread Inspetora ja cuida dos recados oficiais.
        clientes.removeAll(inativos);
    }
}
