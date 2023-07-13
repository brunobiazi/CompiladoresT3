package br.ufscar.dc.compiladores.compiladorT5;

import br.ufscar.dc.compiladores.compiladorT5.AlgumaBaseVisitor;
import br.ufscar.dc.compiladores.compiladorT5.AlgumaParser;
import java.util.*;
import org.antlr.v4.runtime.tree.TerminalNode;


public class Visitante extends AlgumaBaseVisitor<Void> {
    public Erros errorlist = new Erros();
    private final Escopos escopo = new Escopos();
    public static final Visitante I = new Visitante();

    private boolean retAux;

    @Override
    public Void visitDecl_local_global(AlgumaParser.Decl_local_globalContext ctx) {
        ArrayList<Variavel> entrada = new ArrayList<>();
        if (ctx.declaracao_local() != null) // Se for local,faz a verificação e adiciona em entrada
            entrada = verificaDeclLocal(escopo, ctx.declaracao_local());
        else // Se for glogal, faz a verificação e adiciona em entrada
            entrada.add(verificaDeclGlobal(escopo, ctx.declaracao_global()));
        
        addVarEscopo(entrada);
        
        return null;
    }
    
    public Escopos getEscopo() {
        return escopo;
    }
        
    //Adiciona as variáveis no escopo
    public void addVarEscopo(ArrayList<Variavel> var) {
        for(Variavel auxVar: var) {
            escopo.verEscopo().adicionar(auxVar);
        }
    }

    @Override
    public Void visitCorpo(AlgumaParser.CorpoContext ctx) {
        
        ArrayList<Variavel> variableList = new ArrayList<>();
        
        for (AlgumaParser.Declaracao_localContext x : ctx.declaracao_local()) 
        {
            variableList.addAll(verificaDeclLocal(escopo, x));
            addVarEscopo(variableList);
        }
        for (AlgumaParser.CmdContext cmd : ctx.cmd())
            validaCmd(escopo.verEscopo(), cmd);
             
        return null;
    }
    
    
    public ArrayList<Variavel> verificaDeclLocal(Escopos escopo, AlgumaParser.Declaracao_localContext ctx) {    
        ArrayList<Variavel> retorno = new ArrayList<>();
        switch(Correspondencia(ctx.getStart().getText())){

            case 1:
                Tipo tipo1 = verificaTipo(ctx.tipo());
                if (tipo1.nativos != null && tipo1.nativos == Tipo.Nativos.INVALIDO) {
                    errorlist.adiciona_erro(3,ctx.start.getLine(), ctx.tipo().getText());
                }else{
                    String nome = ctx.IDENT().getText();
                    Tipo.adicionaNovoTipo(nome);
                    Variavel novoTipo = new Variavel(nome, tipo1);

                    if (novoTipo.tipo.nativos == Tipo.Nativos.REGISTRO) 
                        novoTipo.registro = validaRegistro(escopo, ctx.tipo().registro()).registro;

                    retorno.add(novoTipo);
                }
            break;
            case 2:
                Tipo tipo2 = new Tipo(validaTipoNat(ctx.tipo_basico()));

                if (tipo2.nativos != null && tipo2.nativos == Tipo.Nativos.INVALIDO)
                    errorlist.adiciona_erro(3,ctx.start.getLine(), ctx.tipo().getText());
                else
                    retorno.add(new Variavel(ctx.IDENT().getText(), tipo2));
            break;
            case 3:
                retorno = validaVar(escopo, ctx.variavel());
            break;
        }    
        return retorno;
    }

    

    // Faz a verificação da declGlobal
    public Variavel verificaDeclGlobal(Escopos escopo, AlgumaParser.Declaracao_globalContext ctx) {
        Variavel auxVar = null;
        
        switch(Correspondencia(ctx.getStart().getText())){
        
            case 4:    // Se for uma função  
                Tipo tipoRetorno = validaEstendido(ctx.tipo_estendido());   
                escopo.criarNovoEscopo();
                retAux = true;
                auxVar = new Variavel(ctx.IDENT().getText(), new Tipo(Tipo.Nativos.FUNCAO));
                if (ctx.parametros() != null) {
                    
                    ArrayList<Variavel> param = validaParametros(escopo, ctx.parametros());
                    auxVar.funcao.setParametros(param);
                    addVarEscopo(param);
                }
                auxVar.funcao.setTipoRetorno(tipoRetorno);
                ArrayList<Variavel> declara = new ArrayList<>();
                for (AlgumaParser.Declaracao_localContext declaracao : ctx.declaracao_local())
                    declara.addAll(verificaDeclLocal(escopo, declaracao));
                addVarEscopo(declara);
                auxVar.funcao.setLocal(declara);
                for (AlgumaParser.CmdContext cmd : ctx.cmd())
                    validaCmd(escopo.verEscopo(), cmd);
                escopo.abandonarEscopo();
                retAux = false;
            break;
         
            case 5: // Se for um procedimento
                escopo.criarNovoEscopo();
                auxVar = new Variavel(ctx.IDENT().getText(), new Tipo(Tipo.Nativos.PROCEDIMENTO));
                
                if (ctx.declaracao_local() != null) {
                    ArrayList<Variavel> decl = new ArrayList<>();
                    
                    for (AlgumaParser.Declaracao_localContext declaracao : ctx.declaracao_local()) {
                        decl.addAll(verificaDeclLocal(escopo, declaracao));                
                    }
                    addVarEscopo(decl);
                    auxVar.procedimento.setLocal(decl);
                }
                if (ctx.parametros() != null) {
                    ArrayList<Variavel> parametros = validaParametros(escopo, ctx.parametros());
                    
                    addVarEscopo(parametros);
                    auxVar.procedimento.setParametros(parametros);
                }
                if (ctx.cmd() != null)
                    for (AlgumaParser.CmdContext cmd : ctx.cmd())
                        validaCmd(escopo.verEscopo(), cmd);

                escopo.abandonarEscopo();
            break;
        }
        return auxVar;
    }

    
    public Variavel validaRegistro(Escopos escopo, AlgumaParser.RegistroContext ctx) {
        
        Variavel auxRegistro = new Variavel("", new Tipo(Tipo.Nativos.REGISTRO));
        escopo.criarNovoEscopo();
       
        for (int i = 0; i < ctx.variavel().size(); i++) {
            auxRegistro.registro.addRegistro(validaVar(escopo, ctx.variavel(i)));
        }
       
        return auxRegistro;
    }
    
    
    // Verifica o tipo
    public int Correspondencia(String receptor){
        switch(receptor){
            case "tipo": return 1;
            case "constante": return 2;
            case "declare": return 3;
            case "funcao": return 4;
            case "procedimento": return 5;
        }
    return 0;
    }
    //Valida os parametros e retorna uma lista de variáveis correspondetes dos parametros
    public ArrayList<Variavel> validaParametros(Escopos escopo, AlgumaParser.ParametrosContext ctx) {
        
        ArrayList<Variavel> retorno = new ArrayList<>();

        for (AlgumaParser.ParametroContext param : ctx.parametro()){
            ArrayList<Variavel> parametros = new ArrayList<>();
            Tipo tipo = validaEstendido(param.tipo_estendido());
            
            for (AlgumaParser.IdentificadorContext i : param.identificador()) {
                    Variavel auxvar = new Variavel(i.getText(), tipo);
                        for (TabelaDeSimbolos ts : escopo.percorrerEscopo()) {
                            Variavel aux = adicionaNovoTipo(ts, auxvar, tipo.criados);
                            if (aux.tipo != null)
                                auxvar = aux;
                        }
                    parametros.add(auxvar);
                    escopo.verEscopo().adicionar(auxvar);
                }
            retorno.addAll(parametros);
        }
        
        return retorno;
    }

    public Tipo validaTipoIdent(AlgumaParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null) 
            return new Tipo(validaTipoNat(ctx.tipo_basico()));
        
        if ((Tipo.getTipo(ctx.IDENT().getText()))!= null) 
            return new Tipo(Tipo.getTipo(ctx.IDENT().getText()));
        
        return new Tipo(Tipo.Nativos.INVALIDO);
    }
    
    //Valida os comandos do contexto
    //Verifica todos os comandos: Escreva, leia, se, faca, enquanto e atribuição e retorne
    //Caso o comando esteja com erro, adiciona o erro na lista e erros.
    public void validaCmd(TabelaDeSimbolos ts, AlgumaParser.CmdContext ctx) {
        String base = "";
        if (ctx.cmdAtribuicao() != null){
            Variavel left = validaIdent(ts, ctx.cmdAtribuicao().identificador());
            Tipo tipoL = left.tipo;
            if (tipoL == null) {
                errorlist.adiciona_erro(0,ctx.cmdAtribuicao().identificador().start.getLine(), ctx.cmdAtribuicao().identificador().getText());
                return;
            }

            Tipo tipoR = validaExpressao(ts, ctx.cmdAtribuicao().expressao());

            if (ctx.getChild(0).getText().contains("^")) {
                 base += "^";
                tipoL = left.ponteiro.getTipo();
            }
 
             if (tipoL.validaTipo(tipoR).nativos == Tipo.Nativos.INVALIDO && tipoL.nativos != null)  {
                errorlist.adiciona_erro(2,ctx.cmdAtribuicao().identificador().start.getLine(), base + ctx.cmdAtribuicao().identificador().getText());
            }
        }
        else if (ctx.cmdEscreva() != null){
            for (AlgumaParser.ExpressaoContext exp : ctx.cmdEscreva().expressao()) 
                validaExpressao(ts, exp);
            }
        else if (ctx.cmdLeia() != null){
                    for (AlgumaParser.IdentificadorContext i : ctx.cmdLeia().identificador()) {
                        Variavel auxVar = validaIdent(ts, i);
                        if (auxVar != null && auxVar.tipo == null)
                            errorlist.adiciona_erro(0,i.getStart().getLine(), i.getText()); 
                    }
        }
        else if (ctx.cmdEnquanto() != null)
            validaExpressao(ts, ctx.cmdEnquanto().expressao());
        else if (ctx.cmdSe() != null){
            validaExpressao(ts, ctx.cmdSe().expressao());
            for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd())
                validaCmd(ts, cmd);
        }
        else if (ctx.cmdFaca() != null){
            validaExpressao(ts, ctx.cmdFaca().expressao());
            for (AlgumaParser.CmdContext cmd : ctx.cmdFaca().cmd())
                validaCmd(ts, cmd); 
        }
        else if (ctx.cmdRetorne() != null){ 
            if (!retAux)
                errorlist.adiciona_erro(5,ctx.start.getLine(),"");
        }
    }

    public ArrayList<Variavel> validaVar(Escopos escopo, AlgumaParser.VariavelContext ctx) {
        ArrayList<Variavel> retorno = new ArrayList<>();
        Tipo tipo = verificaTipo(ctx.tipo());
               
        for (AlgumaParser.IdentificadorContext ident : ctx.identificador()){
            Variavel auxVar;
            auxVar = validaIdent(escopo.verEscopo(), ident);
            
            // Se já estiver sido declarado anteriormente, adiciona erro na lista de erros
            if (auxVar.tipo != null)
                errorlist.adiciona_erro(1,ident.getStart().getLine(), ident.getText());
            else {
                auxVar = new Variavel(auxVar.varNome, tipo);
                if (tipo.criados != null){
                    auxVar = adicionaNovoTipo(escopo.verEscopo(), auxVar, tipo.criados);
                }
                if (tipo.nativos == Tipo.Nativos.REGISTRO){
                    auxVar.registro = validaRegistro(escopo, ctx.tipo().registro()).registro;
                }
                escopo.verEscopo().adicionar(auxVar);
                retorno.add(auxVar);
            }
        }
        
        // Se tipo for nulo ou inválido, adiciona erro na lista de errros
        if (tipo.nativos != null && tipo.nativos == Tipo.Nativos.INVALIDO)
            errorlist.adiciona_erro(3,ctx.start.getLine(), ctx.tipo().getText());
        
        return retorno;
    }

    public Tipo verificaTipo(AlgumaParser.TipoContext ctx) {
        return ((ctx.registro() != null) ? new Tipo(Tipo.Nativos.REGISTRO) : validaEstendido(ctx.tipo_estendido()));
    }

    public Tipo validaEstendido(AlgumaParser.Tipo_estendidoContext ctx) {
        return ((ctx.getChild(0).getText().contains("^")) ? new Tipo(validaTipoIdent(ctx.tipo_basico_ident())): validaTipoIdent(ctx.tipo_basico_ident()));
    }

    // Valida a parcela lógica da expressão
    // Se for uma expressão relacional, retorna a chamada da função validaExpRelacional. Senão, retorna um novo tipo lógico 
    public Tipo validaParcelaLogica(TabelaDeSimbolos ts, AlgumaParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null)
            return validaExpRelacional(ts, ctx.exp_relacional());
        return new Tipo(Tipo.Nativos.LOGICO);
    }

    private Tipo.Nativos validaTipoNat(AlgumaParser.Tipo_basicoContext ctx) {

        if(ctx.getText().equals("inteiro"))
            return Tipo.Nativos.INTEIRO;   
        if(ctx.getText().equals("real"))
            return Tipo.Nativos.REAL;  
        if(ctx.getText().equals("literal"))
            return Tipo.Nativos.LITERAL;
        if(ctx.getText().equals("logico"))
            return Tipo.Nativos.LOGICO;
        
        return Tipo.Nativos.INVALIDO;
    }

    // Faz a validação da  expressão do contexto
    public Tipo validaExpressao(TabelaDeSimbolos ts, AlgumaParser.ExpressaoContext ctx) {
        Tipo tipo = validaTermosLogicos(ts, ctx.termo_logico(0));
        if (ctx.termo_logico().size() > 1) {
            for (int i = 1; i < ctx.termo_logico().size(); i++) {
                tipo = tipo.validaTipo( validaTermosLogicos(ts, ctx.termo_logico(i)));
            }
            if (tipo.nativos != Tipo.Nativos.INVALIDO)
                tipo.nativos = Tipo.Nativos.LOGICO;
        }
        return tipo;
    }

    public Tipo validaTermosLogicos(TabelaDeSimbolos ts, AlgumaParser.Termo_logicoContext ctx) {
        Tipo tipo = validaFatorLogico(ts, ctx.fator_logico(0));
       
            for (int i = 1; i < ctx.fator_logico().size(); i++)
                tipo = tipo.validaTipo(validaFatorLogico(ts, ctx.fator_logico(i)));
        return tipo;
    }

    public Tipo validaExpRelacional(TabelaDeSimbolos ts, AlgumaParser.Exp_relacionalContext ctx) {
        Tipo tipo = validaExpAritmetica(ts, ctx.exp_aritmetica(0));

        if (ctx.exp_aritmetica().size() > 1) {
            tipo = tipo.validaTipo(validaExpAritmetica(ts, ctx.exp_aritmetica(1)));
            
            if (tipo.nativos != Tipo.Nativos.INVALIDO)
                tipo.nativos = Tipo.Nativos.LOGICO;      
        }

        return tipo;
    }

    public Tipo validaFatorLogico(TabelaDeSimbolos ts, AlgumaParser.Fator_logicoContext ctx) {
        Tipo tipo = validaParcelaLogica(ts, ctx.parcela_logica());
            return ((ctx.getChild(0).getText().contains("nao"))? tipo.validaTipo(new Tipo(Tipo.Nativos.LOGICO)): tipo);
    }

 

    public Tipo validaExpAritmetica(TabelaDeSimbolos ts, AlgumaParser.Exp_aritmeticaContext ctx) {
        Tipo tipo = validaTermo(ts, ctx.termo(0));    
        for (int i = 1; i < ctx.termo().size(); i++)
            tipo = tipo.validaTipo(validaTermo(ts, ctx.termo(i)));

        return tipo;
    }

    public Tipo validaTermo(TabelaDeSimbolos ts, AlgumaParser.TermoContext ctx) {
        Tipo tipo = validaFator(ts, ctx.fator(0));
            for (int i = 1; i < ctx.fator().size(); i++)
                tipo = tipo.validaTipo(validaFator(ts, ctx.fator(i)));
        
        return tipo;
    }

    public Tipo validaFator(TabelaDeSimbolos ts, AlgumaParser.FatorContext ctx) {
        Tipo tipo = validaParcela(ts, ctx.parcela(0));
            for (int i = 1; i < ctx.parcela().size(); i++)
                tipo = tipo.validaTipo(validaParcela(ts, ctx.parcela(i)));

        return tipo;
    }

    public Tipo validaParcela(TabelaDeSimbolos ts, AlgumaParser.ParcelaContext ctx) {
        
        if (ctx.parcela_unario() != null) {
            Tipo tipo = validaParcelaSimples(ts, ctx.parcela_unario());
            if (ctx.op_unario() != null) {
                if (tipo.nativos != Tipo.Nativos.INTEIRO && tipo.nativos != Tipo.Nativos.REAL)
                    return new Tipo(Tipo.Nativos.INVALIDO);
                return tipo;
            }
            return tipo;
        }
        return validaParcelaUnaria(ts, ctx.parcela_nao_unario());
    }

    public Variavel validaIdent(TabelaDeSimbolos ts, AlgumaParser.IdentificadorContext ctx) {
        String nome = ctx.IDENT(0).getText();

        if (ts.contem(nome)) {
            Variavel retorno = ts.getVar(nome);
            if (ctx.IDENT().size() > 1) {
                retorno = retorno.registro.getVariavel(ctx.IDENT(1).getText());
                if (retorno == null)
                    errorlist.adiciona_erro(0,ctx.start.getLine(), ctx.getText());
            }
            return retorno;
        }

        return new Variavel(Adequacao(ctx,nome), null);
    }
    
    public Tipo validaMetodo(TabelaDeSimbolos ts, TerminalNode IDENT, List<AlgumaParser.ExpressaoContext> exprs) {
        
        Tipo retorno = null;   
        Variavel metodo = ts.getVar(IDENT.getText());
        
            for (AlgumaParser.ExpressaoContext exp : exprs) {
                Tipo tipoExp = validaExpressao(ts, exp);
                if (retorno == null || retorno.nativos != Tipo.Nativos.INVALIDO)
                    retorno = tipoExp.verificaEquivalenciaTipo(metodo.funcao.getTipoRetorno());
            }
      
        if (retorno.nativos == Tipo.Nativos.INVALIDO){
            errorlist.adiciona_erro(4,IDENT.getSymbol().getLine(), IDENT.getText());
            return new Tipo(Tipo.Nativos.INVALIDO);
        }

        return retorno;
    }

    public Tipo validaParcelaSimples(TabelaDeSimbolos ts, AlgumaParser.Parcela_unarioContext ctx) {
        
        if (ctx.NUM_INT() != null)
            return new Tipo(Tipo.Nativos.INTEIRO);
        if (ctx.NUM_REAL() != null)
            return new Tipo(Tipo.Nativos.REAL);
        if (ctx.IDENT() != null)
            return validaMetodo(ts, ctx.IDENT(), ctx.expressao());
        
        if (ctx.identificador() != null) {
            Variavel ident = validaIdent(ts, ctx.identificador());

            if (ident.tipo == null) {
                errorlist.adiciona_erro(0,ctx.identificador().start.getLine(), ident.varNome);
                return new Tipo(Tipo.Nativos.INVALIDO);
            }

            return ident.tipo;
        }   

        
        Tipo tipo = validaExpressao(ts, ctx.expressao(0));
            for (int i = 1; i < ctx.expressao().size(); i++)
                tipo = tipo.validaTipo( validaExpressao(ts, ctx.expressao(i)));

        return tipo;
    }

    public Tipo validaParcelaUnaria(TabelaDeSimbolos ts, AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null)
            return new Tipo(Tipo.Nativos.LITERAL);
        else {
            if (ctx.getChild(0).getText().contains("&"))
                return new Tipo(Tipo.Nativos.ENDERECO);

            Variavel ident = validaIdent(ts, ctx.identificador());
            if (ident.tipo == null) {
                errorlist.adiciona_erro(0,ctx.identificador().start.getLine(), ident.varNome);
                return new Tipo(Tipo.Nativos.INVALIDO);
            }
            return ident.tipo;
        }
    }

    

   public Variavel adicionaNovoTipo(TabelaDeSimbolos ts, Variavel auxVar, String nome) {
        if (ts.contem(nome)) {
            Variavel modelo = ts.getVar(nome);
            if (modelo.tipo.nativos == Tipo.Nativos.REGISTRO) {
                Variavel retorno = new Variavel(auxVar.varNome, new Tipo(Tipo.Nativos.REGISTRO));
                retorno.setRegistro(modelo.registro);
                retorno.tipo = auxVar.tipo;
                return retorno;
            }
        }
        return new Variavel(null, null);
    }
    
    public String Adequacao(AlgumaParser.IdentificadorContext base, String Nome){
        for (int i = 1; i < base.IDENT().size(); i++)
            Nome += "." + base.IDENT(i);
    return Nome;

    }
}