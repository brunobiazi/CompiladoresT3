package br.ufscar.dc.compiladores.compiladorT5;
import java.util.*;

public class Tipo {
       enum Nativos{INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        PONTEIRO,
        ENDERECO,
        REGISTRO,
        PROCEDIMENTO,
        FUNCAO,
        INVALIDO}

    public static ArrayList<String> Criados = new ArrayList<>();
    
    public Nativos nativos = null;
    public String criados = null;   
    public Tipo apontado = null;
    
    public Tipo(){
        nativos = null;
        criados = null;
    }
    public Tipo(Nativos tipo) {
        nativos = tipo;
    }
    
    
    public Tipo(String tipo) {
        criados = tipo;
    }
    
    public Tipo(Tipo filho) {
        nativos = Tipo.Nativos.PONTEIRO;
        apontado = filho;
    }
    
    public Tipo getTipo() {
        if (apontado != null) 
            return apontado.getTipo();        
        return this;
    }
    public boolean tipoVazio(){
           return (this != null && this.nativos != null); 
    } 
    
    
    public static String getTipo(String tipo) {
        String existe = Criados.stream()
                .filter(str -> str.trim().contains(tipo))
                .findAny()
                .orElse("");                    
        if(!"".equals(existe))    
            return existe;
        else
            return null; 
    }
       public Tipo getTipoAninhado() {
        if (apontado == null) 
            return this;
        
        Tipo tipo = apontado;
        while (tipo.apontado != null) 
            tipo = tipo.getTipoAninhado();
        
        return tipo;
    }
   
    
   public Tipo validaTipo(Tipo tipo) {
        if (this.nativos == Tipo.Nativos.PONTEIRO && tipo.nativos == Tipo.Nativos.ENDERECO)
            return new Tipo(Tipo.Nativos.PONTEIRO);
        else if ((this.nativos == Tipo.Nativos.REAL && (tipo.nativos == Tipo.Nativos.REAL || tipo.nativos == Tipo.Nativos.INTEIRO)) 
                    || (this.nativos == Tipo.Nativos.INTEIRO && (tipo.nativos == Tipo.Nativos.REAL||tipo.nativos == Tipo.Nativos.INTEIRO)))
            return new Tipo(Tipo.Nativos.REAL);
        if (this.nativos == Tipo.Nativos.LITERAL && tipo.nativos == Tipo.Nativos.LITERAL)
            return new Tipo(Tipo.Nativos.LITERAL);
        if (this.nativos == Tipo.Nativos.LOGICO && tipo.nativos == Tipo.Nativos.LOGICO)
            return new Tipo(Tipo.Nativos.LOGICO);
        if (this.nativos == Tipo.Nativos.REGISTRO && tipo.nativos == Tipo.Nativos.REGISTRO)
            return new Tipo(Tipo.Nativos.REGISTRO);
        return new Tipo(Tipo.Nativos.INVALIDO);
    }
    
    public Tipo verificaEquivalenciaTipo(Tipo tipo) {

        if (this.nativos == Tipo.Nativos.ENDERECO && tipo.nativos == Tipo.Nativos.PONTEIRO)
            return new Tipo(Tipo.Nativos.ENDERECO);
        if (this.nativos == Tipo.Nativos.REGISTRO && tipo.nativos == Tipo.Nativos.REGISTRO)
            return new Tipo(Tipo.Nativos.REGISTRO);
        if (this.nativos == Tipo.Nativos.REAL && tipo.nativos == Tipo.Nativos.REAL)
            return validaTipo(tipo);
        return new Tipo(Tipo.Nativos.INVALIDO);
    }
    
    public static void adicionaNovoTipo(String tipo) {
        Criados.add(tipo);
    }
    
    public String getFormat() {
        if (nativos != null) {
            if(nativos == Nativos.INTEIRO)
                return "int";
            if(nativos == Nativos.REAL)
                return "float";
            if(nativos == Nativos.LITERAL)
                return "char";
        }

        return criados;
    }
    
    public String getFormatSpec() {
        if (nativos != null) {
            if(nativos == Nativos.INTEIRO)
                return "%d";
            if(nativos == Nativos.REAL)
                return "%f";
            if(nativos == Nativos.LITERAL)
                return "%s";
        }
        return "";
    }
}
