package bolsa;
import java.io.Serializable;

public class Acao implements Serializable {
    private static final long serialVersionUID = 1L;
    private String simbolo;
    private double preco;

    public Acao(String simbolo, double preco) {
        this.simbolo = simbolo;
        this.preco = preco;
    }

    public String getSimbolo() { return simbolo; }
    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    @Override
    public String toString() {
        return simbolo + " - Valor Atual: R$ " + String.format("%.2f", preco);
    }
}
