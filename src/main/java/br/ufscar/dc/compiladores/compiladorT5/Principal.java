package br.ufscar.dc.compiladores.compiladorT5;

import br.ufscar.dc.compiladores.compiladorT5.AlgumaParser.ProgramaContext;
import java.io.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;


public class Principal {
    public static void main(String[] args) throws IOException {
        // Verifica se foi passado para a funÃ§Ã£o o nÃºmero correto de parÃ¢metros.
        // Em caso de falha, Ã© apresentado um erro na tela e encerra o programa.
         if (args.length < 2) {
            System.out.println("Falha na execuÃ§Ã£o.\nNÃºmero de parÃ¢metros invÃ¡lidos.");
            System.exit(0);
        }

        // Faz a leitura do arquivo e cria o analisador lÃ©xico e sintÃ¡tico do programa.
        AlgumaLexer entrada = new AlgumaLexer(CharStreams.fromFileName(args[0]));
        AlgumaParser parser = new AlgumaParser(new CommonTokenStream(entrada));


        parser.removeErrorListeners();
        parser.addErrorListener(TrataErro.INSTANCE);
        //Cria o analisador semÃ¢ntico
        Visitante analisador = new Visitante();
        

        // abre o arquivo de saida
        try (PrintWriter saida = new PrintWriter(args[1])){

            try{
                // Faz a analise semÃ¢ntica do programa
                ProgramaContext c = parser.programa();
                analisador.visitPrograma(c);
                            
                if (analisador.errorlist.getErros().isEmpty()) {
                    GeraCodigo gerador = new GeraCodigo(analisador.getEscopo());
                    gerador.visit(c);
                    saida.print(gerador.outputFinal.toString());
                }
                // Caso encontre erros, printe todos os erros da lista no arquivo de saÃ­da
                else{
                    for (String retorno : analisador.errorlist.getErros())
                        saida.println(retorno);
                    saida.println("Fim da compilacao");
                }
                saida.close();
            }
           
            catch(ParseCancellationException exception) {
                saida.println(exception.getMessage());     
                saida.println("Fim da compilacao");
                saida.close();   
            }
        }
        catch(IOException exception){
                System.out.println("Falha na execuÃ§Ã£o.\nO programa nÃ£o conseguiu abrir o arquivo: " + args[1]+ ".");
           }
  
    }
}