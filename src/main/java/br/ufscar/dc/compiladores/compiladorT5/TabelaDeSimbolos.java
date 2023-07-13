package br.ufscar.dc.compiladores.compiladorT5;


import br.ufscar.dc.compiladores.compiladorT5.AlgumaParser;
import java.util.*;

public class TabelaDeSimbolos {    
    private final HashMap<String, Variavel> Tabela;

    public TabelaDeSimbolos() {
        Tabela = new HashMap<>();
    }

    public Tipo getTipo(String nome) {
        return Tabela.get(nome).tipo;
    }

    public Variavel getVar(String nome) {
        return Tabela.get(nome);
    }
    public void adicionar(Variavel v) {
        Tabela.put(v.varNome, v);
    }
    public boolean contem(String nome) {
        return Tabela.containsKey(nome);
    }

    void adicionar(AlgumaParser.Declaracao_localContext v) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}