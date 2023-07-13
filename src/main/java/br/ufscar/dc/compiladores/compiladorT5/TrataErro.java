package br.ufscar.dc.compiladores.compiladorT5;
import br.ufscar.dc.compiladores.compiladorT5.AlgumaLexer;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;


public class TrataErro extends BaseErrorListener {    

    public static final TrataErro INSTANCE = new TrataErro();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
            int charPositionInLine, String msg, RecognitionException e) 
                throws ParseCancellationException{
        

        Token token = (Token) offendingSymbol;


        // Cria o padrão de todos os prints de erro.
        String base = "Linha " + token.getLine() + ": "; 

            
        if(eh_erro(token.getType())) {//Parte resposável pelo tratamento dos erros (a checagem é feita pela função eh_erro() )
            if (token.getType() == AlgumaLexer.Caracter_invalido) {
                throw new ParseCancellationException(base + token.getText() + " - simbolo nao identificado");
            }
            else if(AlgumaLexer.VOCABULARY.getSymbolicName(token.getType()).equals("CADEIA_SEM_FIM"))
            {
                throw new ParseCancellationException(base + "cadeia literal nao fechada");
            }
            else {
                throw new ParseCancellationException(base + "comentario nao fechado");
            }

        }
        else if (token.getType() == Token.EOF)//Se o erro não for léxico, ele cai nos casos restantes(sintatico ou EOF )
                throw new ParseCancellationException(base + "erro sintatico proximo a EOF");
        else
                throw new ParseCancellationException(base + "erro sintatico proximo a " + token.getText());
        }

    private static Boolean eh_erro(int tkType) {
        //retorna True quando se trata de um dos 3 erros léxicos
                return tkType == AlgumaLexer.CADEIA_SEM_FIM || tkType == AlgumaLexer.COMENTARIO_SEM_FIM
                        || tkType == AlgumaLexer.Caracter_invalido;
    }
}