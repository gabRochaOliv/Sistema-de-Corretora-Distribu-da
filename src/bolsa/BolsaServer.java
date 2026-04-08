package bolsa;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.net.InetAddress;

public class BolsaServer {
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String CYAN = "\u001B[36m";
    public static final String RED = "\u001B[31m";

    public static void main(String[] args) {
        try {
            String ipLocalSuggestion = InetAddress.getLocalHost().getHostAddress();
            Scanner sc = new Scanner(System.in);
            
            System.out.println(CYAN + "=== CONFIGURACAO DO SERVIDOR RMI ===" + RESET);
            System.out.println("Para clientes externos conectarem (como seu amigo),");
            System.out.println("o Servidor precisa se expor num IP IPV4 Valido (Hamachi/LAN).");
            System.out.print("IP a ser usado (apenas de ENTER para usar " + ipLocalSuggestion + "): ");
            
            String ip = sc.nextLine().trim();
            if (ip.isEmpty()) {
                ip = ipLocalSuggestion;
            }
            
            System.setProperty("java.rmi.server.hostname", ip);
            
            LocateRegistry.createRegistry(1099);
            Registry registry = LocateRegistry.getRegistry(ip, 1099);
            
            BolsaService servico = new BolsaServiceImpl();
            registry.rebind("BolsaService", servico);
            
            System.out.println(GREEN + "\n#########################################################");
            System.out.println("     SERVIDOR DA CORRETORA ONLINE E OPERACIONAL!");
            System.out.println("     IP para entregar ao seu amigo: " + ip);
            System.out.println("#########################################################" + RESET);
            
        } catch (Exception e) {
            System.err.println(RED + "Erro Critico no Servidor:" + RESET);
            e.printStackTrace();
        }
    }
}
