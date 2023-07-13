package br.ufscar.dc.compiladores.compiladorT5;

import java.util.*;

public class Escopos {
    
    private final LinkedList<TabelaDeSimbolos> escopoTabSimb;
    
    public Escopos() {
        escopoTabSimb = new LinkedList<>();
        criarNovoEscopo();
    }
    
    public final void criarNovoEscopo() {
        escopoTabSimb.push(new TabelaDeSimbolos());
    }
    
    public List<TabelaDeSimbolos> percorrerEscopo() {
        return escopoTabSimb;
    }
    
    public void abandonarEscopo() {
        escopoTabSimb.pop();
    }
    
    public TabelaDeSimbolos verEscopo() {
        return escopoTabSimb.peek();
    }
}