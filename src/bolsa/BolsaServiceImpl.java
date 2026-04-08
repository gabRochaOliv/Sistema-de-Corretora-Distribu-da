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
        } catch (Exception e) {
            System.err.println("Erro Critico ao salvar acoes no disco: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void carregarAcoes() {
        java.io.File file = new java.io.File(ARQUIVO_DADOS);
        if (file.exists()) {
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(ARQUIVO_DADOS))) {
                acoes = (Map<String, Acao>) ois.readObject();
                System.out.println("--> Banco de DADOS VINCULADO! (" + acoes.size() + " acoes carregadas).");
            } catch (Exception e) {
                System.err.println("Erro ao ler banco de dados corrompido: " + e.getMessage());
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
            salvarAcoes(); // Atualiza o Banco de Dados Fisico
            
            String msg = "A acao " + simbolo.toUpperCase() + " sofreu uma alteracao! De R$ " + 
                         String.format("%.2f", precoAntigo) + " para R$ " + String.format("%.2f", novoPreco) + "!";
            System.out.println("--> Atualizando " + clientes.size() + " clientes simultaneos: " + msg);
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
            salvarAcoes(); // Atualiza o Banco de Dados Fisico
            
            String msg = "Uma NOVA acao foi listada na Corretora! A " + simbolo + " estreou cotada a R$ " + String.format("%.2f", precoInicial) + "!";
            System.out.println("--> Broadcast para clientes: Nova acao cadastrada (" + simbolo + ")");
            notificarTodos(msg); 
            
            return true;
        }
        return false;
    }

    @Override
    public boolean excluirAcao(String simbolo) throws RemoteException {
        simbolo = simbolo.toUpperCase();
        if (acoes.remove(simbolo) != null) {
            salvarAcoes(); // Remove do Banco Fisico
            String msg = "A acao " + simbolo + " acaba de ser EXCLUIDA da Bolsa e as negociacoes dela foram encerradas!";
            System.out.println("--> Broadcast para clientes: Acao excluida (" + simbolo + ")");
            notificarTodos(msg);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void registrarCliente(ClientCallback cliente) throws RemoteException {
        if (!clientes.contains(cliente)) {
            // Emite aviso na rede que tem gente nova ANTES de encaixar ele, para ele não receber a própria notificação.
            notificarTodos("Um novo cliente investidor acaba de acessar e plugar na Corretora!");
            
            clientes.add(cliente);
            System.out.println("Um novo cliente investidor se conectou! Total de ativos online: " + clientes.size());
        }
    }

    @Override
    public synchronized void removerCliente(ClientCallback cliente) throws RemoteException {
        clientes.remove(cliente);
        System.out.println("Cliente se desconectou do painel. Total online agora: " + clientes.size());
    }

    private synchronized void notificarTodos(String mensagem) {
        List<ClientCallback> inativos = new ArrayList<>();
        // Itera clonando para evitar concorrencia pesada
        for (ClientCallback c : new ArrayList<>(clientes)) {
            try {
                c.receberMensagemGeral(mensagem);
            } catch (RemoteException e) {
                // Se der exception, esse IP especifico caiu
                inativos.add(c);
            }
        }
        clientes.removeAll(inativos);
    }
}
