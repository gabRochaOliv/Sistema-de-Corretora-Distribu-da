package bolsa;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void receberMensagemGeral(String mensagem) throws RemoteException;
}
