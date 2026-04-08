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
        Acao acao = acoes.get(simbolo.toUpperCase());
        if (acao != null) return acao.getPreco();
        return -1;
    }

    @Override
    public List<Acao> listarAcoes() throws RemoteException {
        return new ArrayList<>(acoes.values());
    }

    @Override
    public boolean atualizarPreco(String simbolo, double novoPreco) throws RemoteException {
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
            clientes.add(cliente);
            System.out.println("Novo acesso! Total online: " + clientes.size());
        }
    }

    @Override
    public synchronized void removerCliente(ClientCallback cliente) throws RemoteException {
        if (clientes.remove(cliente)) {
            System.out.println("Um Cliente escolheu sair. Total online agora: " + clientes.size());
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
        clientes.removeAll(inativos);
    }
}
