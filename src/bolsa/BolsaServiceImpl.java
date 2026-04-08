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

    public BolsaServiceImpl() throws RemoteException {
        super();
        acoes = new ConcurrentHashMap<>();
        clientes = new ArrayList<>();
        
        acoes.put("BTC", new Acao("BTC", 350000.0));
        acoes.put("ETH", new Acao("ETH", 15000.0));
        acoes.put("SOL", new Acao("SOL", 800.0));
    }

    @Override
    public void ping() throws RemoteException { }

    @Override
    public double consultarPreco(String simbolo) throws RemoteException {
        Acao acao = acoes.get(simbolo.toUpperCase());
        if (acao != null) {
            return acao.getPreco();
        }
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
            
            String msg = "O preco da acao " + simbolo.toUpperCase() + " sofreu alteracao de R$ " + 
                         String.format("%.2f", precoAntigo) + " para R$ " + String.format("%.2f", novoPreco) + "!";
            System.out.println("--> Atualizando " + clientes.size() + " clientes: " + msg);
            notificarTodos(msg);
            
            return true;
        }
        return false;
    }

    @Override
    public synchronized void registrarCliente(ClientCallback cliente) throws RemoteException {
        if (!clientes.contains(cliente)) {
            clientes.add(cliente);
            System.out.println("Um novo cliente se conectou. Total de ativos: " + clientes.size());
            notificarTodos("Um novo cliente investidor acaba de se conectar a Corretora!");
        }
    }

    @Override
    public synchronized void removerCliente(ClientCallback cliente) throws RemoteException {
        clientes.remove(cliente);
        System.out.println("Cliente se desconectou. Total de ativos: " + clientes.size());
    }

    private synchronized void notificarTodos(String mensagem) {
        List<ClientCallback> inativos = new ArrayList<>();
        for (ClientCallback c : clientes) {
            try {
                c.receberMensagemGeral(mensagem);
            } catch (RemoteException e) {
                inativos.add(c);
            }
        }
        clientes.removeAll(inativos);
    }
}
