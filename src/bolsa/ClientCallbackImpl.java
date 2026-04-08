package bolsa;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    private static final long serialVersionUID = 1L;

    public static final String RESET = "\u001B[0m";
    public static final String YELLOW = "\u001B[33m";

    protected ClientCallbackImpl() throws RemoteException {
        super();
    }
    
    @Override
    public void receberMensagemGeral(String mensagem) throws RemoteException {
        System.out.println(YELLOW + "\n\n>>> [NOTIFICACAO EM TEMPO REAL] <<<");
        System.out.println(mensagem);
        System.out.println("-----------------------------------" + RESET);
    }
}
