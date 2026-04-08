package bolsa;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BolsaService extends Remote {
    void ping() throws RemoteException;
    double consultarPreco(String simbolo) throws RemoteException;
    List<Acao> listarAcoes() throws RemoteException;
    boolean atualizarPreco(String simbolo, double novoPreco) throws RemoteException;
    
    void registrarCliente(ClientCallback cliente) throws RemoteException;
    void removerCliente(ClientCallback cliente) throws RemoteException;
}
