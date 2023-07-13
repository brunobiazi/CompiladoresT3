package br.ufscar.dc.compiladores.compiladorT5;
import java.util.ArrayList;



public class Erros {
 
    private final ArrayList<String> erros = new ArrayList<>();

    // Adiciona os erros na lista de erros
    public void adiciona_erro(int id, int linha, String nome){
            String base = "Linha " + linha;
            switch(id)
            {
                case 0:
                    erros.add(base+": identificador "+nome+" nao declarado");
                    break;
                case 1:
                    erros.add(base+": identificador "+nome+" ja declarado anteriormente");
                    break;
                case 2:
                    erros.add(base+": atribuicao nao compativel para "+nome);
                    break;
                case 3:
                    erros.add(base+": tipo "+nome+" nao declarado");
                    break;
                case 4:
                    erros.add(base+": incompatibilidade de parametros na chamada de "+nome);
                    break;
                case 5:            
                    erros.add(base+": comando retorne nao permitido nesse escopo");
                    break;
                default:                        
            }           
}   

    // Retorna todos os erros da lista
     public ArrayList<String> getErros(){
        return erros;
    }

}
