package br.ufscar.dc.compiladores.compiladorT5;
import java.util.*;

public class GeraCodigo extends AlgumaBaseVisitor<Void> {    
    private final Escopos escopo;
    public final StringBuilder outputFinal = new StringBuilder();
    private Variavel varAux;

    // Construtor da classe, define o escopo
    public GeraCodigo(Escopos escopo){ 
        this.escopo = escopo;
    }


    @Override
    public Void visitPrograma(AlgumaParser.ProgramaContext ctx) { 
        // Escreve o início do código C no arquivo de saida final e visita as declarações
        outputFinal.append("#include <stdio.h>\n#include <stdlib.h>\n#include <string.h>\n#include <math.h>\n\n");
        if (!ctx.declaracoes().isEmpty()) 
            for (AlgumaParser.Decl_local_globalContext declaracao : ctx.declaracoes().decl_local_global())
                visitDecl_local_global(declaracao);
        outputFinal.append("int main() {\n");
        for (AlgumaParser.Declaracao_localContext declaracao : ctx.corpo().declaracao_local())
            visitDeclaracao_local(declaracao);
        for (AlgumaParser.CmdContext cmd : ctx.corpo().cmd())
            visitCmd(cmd);
        outputFinal.append("return 0;\n}\n");
        return null;
    }

    // Verifica o tipo de decaração e chama visitDeclaracao correspondente
    @Override
    public Void visitDecl_local_global(AlgumaParser.Decl_local_globalContext ctx) {
        if (ctx.declaracao_local() != null)
            visitDeclaracao_local(ctx.declaracao_local());
        else
            visitDeclaracao_global(ctx.declaracao_global());
        return null;
    }

    @Override
    public Void visitDeclaracao_global(AlgumaParser.Declaracao_globalContext ctx) {
        //Vê se é uma funcao e faz as devidas verificações
        if(ctx.getChild(0).getText().equals("funcao")){
        Variavel funcao = escopo.verEscopo().getVar(ctx.IDENT().getText());
        outputFinal.append(funcao.getFuncao().getTipoRetorno().getFormat()).append(" ").append(funcao.varNome).append("(");
        ArrayList<Variavel> parametros = funcao.getFuncao().getParametros();
        
        if (parametros.get(0).tipo.nativos == Tipo.Nativos.LITERAL)
            outputFinal.append(parametros.get(0).tipo.getFormat()).append(" *").append(parametros.get(0).varNome);
        else
            outputFinal.append(parametros.get(0).tipo.getFormat()).append(" ").append(parametros.get(0).varNome);
    
        for (int i = 1; i < parametros.size(); i++) {
            outputFinal.append(", ");
            if (parametros.get(i).tipo.nativos == Tipo.Nativos.LITERAL)
                outputFinal.append(parametros.get(i).tipo.getFormat()).append(" *").append(parametros.get(i).varNome);
            else
                geraVariavel(parametros.get(i));
        }
        outputFinal.append(") {\n");
        for (AlgumaParser.Declaracao_localContext declaracao : ctx.declaracao_local())
            visitDeclaracao_local(declaracao);
        
        escopo.criarNovoEscopo();

        for (Variavel v : parametros) {
            escopo.verEscopo().adicionar(v);
        }
        
        for (AlgumaParser.Declaracao_localContext v : funcao.getFuncao().getLocals())
            escopo.verEscopo().adicionar(v);

        for (AlgumaParser.CmdContext cmd : ctx.cmd())
            visitCmd(cmd);
        
        escopo.abandonarEscopo();
        outputFinal.append("}\n");
    }   //Vê se é um procedimento e faz as devidas verificações
        else if(ctx.getChild(0).getText().equals("procedimento")){
            Variavel proc = escopo.verEscopo().getVar(ctx.IDENT().getText());
            outputFinal.append("void ").append(proc.varNome).append("(");
            ArrayList<Variavel> parametros = proc.getProcedimento().getParametros();
            if (Tipo.Nativos.LITERAL == parametros.get(0).tipo.nativos)
                outputFinal.append(parametros.get(0).tipo.getFormat()).append(" *").append(parametros.get(0).varNome);
            else
                outputFinal.append(parametros.get(0).tipo.getFormat()).append(" ").append(parametros.get(0).varNome);
            for (int i = 1; i < parametros.size(); i++) {
                outputFinal.append(", ");
                if (parametros.get(i).tipo.nativos == Tipo.Nativos.LITERAL)
                    outputFinal.append(parametros.get(i).tipo.getFormat()).append(" *").append(parametros.get(0).varNome);
                else
                    geraVariavel(parametros.get(i));   
            }
            outputFinal.append(") {\n");
            for (AlgumaParser.Declaracao_localContext declaracao : ctx.declaracao_local())
                visitDeclaracao_local(declaracao);
            escopo.criarNovoEscopo();
            for (Variavel v : parametros)
                escopo.verEscopo().adicionar(v);
            for (Variavel v : proc.getProcedimento().getLocals()) 
                escopo.verEscopo().adicionar(v);
            for (AlgumaParser.CmdContext cmd : ctx.cmd())
                visitCmd(cmd);
            escopo.abandonarEscopo();
            outputFinal.append("}\n");
        }
        return null;
    }
    // Verifica se é uma constante, tipo, ou declare.
    // Em cada caso, faz as devidas adições no arquivo final
    @Override
    public Void visitDeclaracao_local(AlgumaParser.Declaracao_localContext ctx) {
        switch (ctx.getChild(0).getText()) {
            case "constante":
                outputFinal.append("#define ").append(ctx.IDENT().getText()).append(" "); 
                visitValor_constante(ctx.valor_constante());
                break;
            case "tipo":
                outputFinal.append("typedef struct {\n");
                this.varAux = escopo.verEscopo().getVar(ctx.IDENT().getText());
                if (ctx.tipo().registro() != null)
                    for (Variavel v : varAux.getRegistro().getAll()) {
                        geraVariavel(v);
                        outputFinal.append(";\n");
                    }
                outputFinal.append("} ").append(ctx.IDENT().getText()).append(";\n");
                break;
            case "declare":
                visitVariavel(ctx.variavel());
                break;
            default:
                break;
        }
        return null;
    }

    @Override
    public Void visitVariavel(AlgumaParser.VariavelContext ctx) {
        for (AlgumaParser.IdentificadorContext id : ctx.identificador()) {
            String varNome = id.IDENT(0).getText();
            for (int i = 1; i < id.IDENT().size(); i++)
                varNome += "." + id.IDENT(i).getText();
            Variavel ident = escopo.verEscopo().getVar(varNome);
            geraVariavel(ident);
            if (!id.dimensao().exp_aritmetica().isEmpty())
                visitDimensao(id.dimensao());
            outputFinal.append(";\n");
        }
        return null;
    }    

    // Verifica o tipo do comando
    // Em cada caso, faz as validações do comando e faz as adições no arquivo final C.
    @Override 
    public Void visitCmd(AlgumaParser.CmdContext ctx) {
        if (ctx.cmdLeia() != null){
            Variavel ident = escopo.verEscopo().getVar(ctx.cmdLeia().identificador(0).getText());
            outputFinal.append(String.format("scanf(\"%s\", &%s);\n", ident.tipo.getFormatSpec(), ident.varNome));}
        else if (ctx.cmdEscreva() != null){
            for (AlgumaParser.ExpressaoContext exp : ctx.cmdEscreva().expressao()) {
                Tipo tipoExp = Visitante.I.validaExpressao(escopo.verEscopo(), exp);
                outputFinal.append(String.format("printf(\"%s\", ", tipoExp.getFormatSpec()));
                visitExpressao(exp);
                outputFinal.append(");\n");
            }
        }
        else if (ctx.cmdSe() != null){
            outputFinal.append("if (");
            visitExpressao(ctx.cmdSe().expressao());
            outputFinal.append(") {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd1)
                visitCmd(cmd);
            outputFinal.append("}\n");
            if (ctx.cmdSe().cmd2.size() > 0) {
                outputFinal.append("else {\n");
                for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd2)
                    visitCmd(cmd);
                outputFinal.append("}\n");
            }
        }
        else if (ctx.cmdAtribuicao() != null){
            if (ctx.cmdAtribuicao().getChild(0).getText().equals("^"))
            outputFinal.append("*");
            Variavel ident = Visitante.I.validaIdent(escopo.verEscopo(), ctx.cmdAtribuicao().identificador());
            if (ident.tipo != null && ident.tipo.nativos != Tipo.Nativos.LITERAL) {
                visitIdentificador(ctx.cmdAtribuicao().identificador());
                outputFinal.append(" = ");
                visitExpressao(ctx.cmdAtribuicao().expressao());
            } else {
                outputFinal.append("strcpy(").append(ctx.cmdAtribuicao().identificador().getText()).append(",");
                visitExpressao(ctx.cmdAtribuicao().expressao());
                outputFinal.append(")");
            }
            outputFinal.append(";\n");
        }
        else if (ctx.cmdCaso() != null){
            outputFinal.append("switch (");
            visitExp_aritmetica(ctx.cmdCaso().exp_aritmetica());
            outputFinal.append(") {\n");
            for (AlgumaParser.Item_selecaoContext i : ctx.cmdCaso().selecao().item_selecao()) {
                visitConstantes(i.constantes());
                for (AlgumaParser.CmdContext cmd : i.cmd())
                    visitCmd(cmd);
                outputFinal.append("break;\n");
            }
            if (!ctx.cmdCaso().cmd().isEmpty()) {
                outputFinal.append("default:\n");
                for (AlgumaParser.CmdContext cmd : ctx.cmdCaso().cmd())
                    visitCmd(cmd);  
            }
            outputFinal.append("}\n");
        }
        else if (ctx.cmdPara() != null){
            outputFinal.append("for (");
            Variavel ident = escopo.verEscopo().getVar(ctx.cmdPara().IDENT().getText());
            outputFinal.append(ident.varNome).append(" = ");
            visitExp_aritmetica(ctx.cmdPara().a); 
            outputFinal.append("; ");
            outputFinal.append(ident.varNome).append(" <= ");
            visitExp_aritmetica(ctx.cmdPara().b);
            outputFinal.append("; ");
            outputFinal.append(ident.varNome).append("++) {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdPara().cmd())
                visitCmd(cmd);
            outputFinal.append("}\n");
        }
        else if (ctx.cmdEnquanto() != null){
            outputFinal.append("while (");
            visitExpressao(ctx.cmdEnquanto().expressao());
            outputFinal.append(") {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdEnquanto().cmd())
                visitCmd(cmd);
            outputFinal.append("}\n");
        }
        else if (ctx.cmdFaca() != null){
            outputFinal.append("do {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdFaca().cmd())
                visitCmd(cmd);
            outputFinal.append("} while (");
            visitExpressao(ctx.cmdFaca().expressao());
            outputFinal.append(");\n");
        }   
        else if (ctx.cmdChamada() != null){
            outputFinal.append(ctx.cmdChamada().IDENT().getText()).append("(");
            visitExpressao(ctx.cmdChamada().expressao(0));
            for (int i = 1; i < ctx.cmdChamada().expressao().size(); i++) {
                outputFinal.append(", ");
                visitExpressao(ctx.cmdChamada().expressao(i));
            }
            outputFinal.append(");\n");
        }
        else if (ctx.cmdRetorne() != null){
            outputFinal.append("return ");
            visitExpressao(ctx.cmdRetorne().expressao());
            outputFinal.append(";\n");
        }
        return null;
    }

    @Override
    public Void visitConstantes(AlgumaParser.ConstantesContext ctx) {
        int inicio = ctx.numero_intervalo(0).opu1 != null ? -Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(0).getText()) : Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(0).getText());
        int fim;
        if (ctx.numero_intervalo(0).opu2 != null)
            fim = -Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(1).getText());
        else if (ctx.numero_intervalo(0).NUM_INT(1) != null)
            fim = Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(1).getText());
        else
            fim = inicio;
        for (int i = inicio; i <= fim; i++)
            outputFinal.append("case ").append(i).append(":\n");

        for (int i = 1; i < ctx.numero_intervalo().size(); i++){
            inicio = ctx.numero_intervalo(i).opu1 != null ? -Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(0).getText()) : Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(0).getText());
            if (ctx.numero_intervalo(i).opu2 != null)
                fim = -Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(1).getText());
            else if (ctx.numero_intervalo(0).NUM_INT(1) != null)
                fim = Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(1).getText());
            else
                fim = inicio;
            for (int j = inicio; i <= fim; i++)
                outputFinal.append("case ").append(j).append(":\n");
            }
        return null;
    }    

    @Override
    public Void visitTermo_logico(AlgumaParser.Termo_logicoContext ctx) {
        visitFator_logico(ctx.fator_logico(0));
        for (int i = 0; i < ctx.op_logico_2().size(); i++) {
            outputFinal.append(" && ");
            visitFator_logico(ctx.fator_logico(i + 1));
        }
        return null;
    }

    @Override
    public Void visitFator_logico(AlgumaParser.Fator_logicoContext ctx) {
        // Se o operador for não, imprime '!' no arquivo final
        if (ctx.getChild(0).getText().equals("nao"))
            outputFinal.append("!");
        visitParcela_logica(ctx.parcela_logica());
        return null;
    }

    @Override
    public Void visitParcela_logica(AlgumaParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            visitExp_relacional(ctx.exp_relacional());
            return null;
        }
        if (ctx.getText().equals("verdadeiro"))
            outputFinal.append(" true ");
        else
            outputFinal.append(" false ");
        
        return null;
    }

    @Override
    public Void visitIdentificador(AlgumaParser.IdentificadorContext ctx) {
        outputFinal.append(ctx.IDENT(0).getText());
        for (int i = 1; i < ctx.IDENT().size(); i++) {
            outputFinal.append(".");
            outputFinal.append(ctx.IDENT(i).getText());
        }
        if (ctx.dimensao().getChild(0) != null) 
            visitDimensao(ctx.dimensao());
        return null;
    }

    @Override
    public Void visitDimensao(AlgumaParser.DimensaoContext ctx) {
        outputFinal.append("[");
        for (AlgumaParser.Exp_aritmeticaContext exp : ctx.exp_aritmetica())
            visitExp_aritmetica(exp);
        outputFinal.append("]");
        return null;
    }

    @Override
    public Void visitExp_relacional(AlgumaParser.Exp_relacionalContext ctx) {
        visitExp_aritmetica(ctx.exp_aritmetica(0));
        if (ctx.op_relacional() != null) {
            visitOp_relacional(ctx.op_relacional());
            visitExp_aritmetica(ctx.exp_aritmetica(1));
        }
        return null;
    }
    // Verifica o tipo do operador e adiciona o equivalente de C no arquivo de saida final
    @Override
    public Void visitOp_relacional(AlgumaParser.Op_relacionalContext ctx){
        switch (ctx.getText()){
            case "=":
                outputFinal.append(" == ");
                break;
            case "<>":
                outputFinal.append(" != ");
                break;
            default:
                outputFinal.append(ctx.getText());
                break;
        }
        return null;
    }

    @Override
    public Void visitTermo(AlgumaParser.TermoContext ctx) {
        visitFator(ctx.fator(0));
        for (int i = 0; i < ctx.op2().size(); i++) {
            outputFinal.append(" ").append(ctx.op2(i).getText()).append(" ");
            visitFator(ctx.fator(i + 1));
        }
       
        return null;
    }

    @Override
    public Void visitFator(AlgumaParser.FatorContext ctx) {
        visitParcela(ctx.parcela(0));
        for (int i = 0; i < ctx.op3().size(); i++) {
            outputFinal.append(" ").append(ctx.op3(i).getText()).append(" ");
            visitParcela(ctx.parcela(i));
        }
        return null;
    }

    @Override
    public Void visitParcela(AlgumaParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            if (ctx.op_unario() != null)
                outputFinal.append(" ").append(ctx.op_unario().getText());   
            visitParcela_unario(ctx.parcela_unario());
        } else
            visitParcela_nao_unario(ctx.parcela_nao_unario());
        return null;
    }

    @Override
    public Void visitExp_aritmetica(AlgumaParser.Exp_aritmeticaContext ctx) {
        visitTermo(ctx.termo(0));
        for (int i = 0; i < ctx.op1().size(); i++) {
            outputFinal.append(" ").append(ctx.op1(i).getText()).append(" ");
            visitTermo(ctx.termo(i + 1));
        }
        return null;
    }

    @Override
    public Void visitParcela_unario(AlgumaParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            if (ctx.getChild(0).getText().equals("^"))
                outputFinal.append("*");
            visitIdentificador(ctx.identificador());
        } else if (ctx.IDENT() != null) {
            outputFinal.append(ctx.IDENT().getText()).append("(");
            visitExpressao(ctx.expressao(0));
            for (int i = 1; i < ctx.expressao().size(); i++) {
                outputFinal.append(", ");
                visitExpressao(ctx.expressao(i));
            }
            outputFinal.append(")");
        }
        else if (ctx.NUM_INT() != null)
            outputFinal.append(ctx.NUM_INT().getText());
        else if (ctx.NUM_REAL() != null)
            outputFinal.append(ctx.NUM_REAL().getText());
        else {
            outputFinal.append("(");
            for (AlgumaParser.ExpressaoContext exp : ctx.expressao())
                visitExpressao(exp);
            
            outputFinal.append(")");
        }
        return null;
    }

    @Override
    public Void visitParcela_nao_unario(AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            if (ctx.getChild(0).getText().equals("&"))
                outputFinal.append("&");
            visitIdentificador(ctx.identificador());
        } else
            outputFinal.append(ctx.CADEIA().getText());
        return null;
    }

    // Verifica a o tipo da variável e adiciona a sintaxe correta no arquivo final
    public void geraVariavel(Variavel v) {
        if (v.tipo != null && v.tipo.nativos != null) {
            if(null != v.tipo.nativos)
                switch (v.tipo.nativos) {
                case LITERAL:
                    outputFinal.append(String.format("%s %s[100]", v.tipo.getFormat(), v.varNome));
                    break;
                case PONTEIRO:
                    outputFinal.append(String.format("%s *%s", v.getTipoPonteiroAninhado().getFormat(), v.varNome));
                    break;
                case REGISTRO:
                    outputFinal.append("struct {\n");
                    for (Variavel i : v.getRegistro().getAll()) {
                        geraVariavel(i);
                        outputFinal.append(";\n");
                    }   outputFinal.append("} ").append(v.varNome);
                    break;
                case INTEIRO:
                    outputFinal.append(String.format("%s %s", v.tipo.getFormat(), v.varNome));
                    break;
                case REAL:
                    outputFinal.append(String.format("%s %s", v.tipo.getFormat(), v.varNome));
                    break;
                default:
                    break;
            }
        } else
            outputFinal.append(String.format("%s %s", v.tipo.criados, v.varNome));
    }

    // Verifica o valor da constante e adiciona o equivalente de C no arquivo final
    @Override
    public Void visitValor_constante(AlgumaParser.Valor_constanteContext ctx) {
        if (ctx.CADEIA() != null)
            outputFinal.append("\"").append(ctx.CADEIA().getText()).append("\"\n");
        else if (ctx.NUM_INT() != null)
            outputFinal.append(Integer.parseInt(ctx.NUM_INT().getText())).append("\n");
        else if (ctx.NUM_REAL() != null) 
            outputFinal.append(Float.parseFloat(ctx.NUM_REAL().getText())).append("\n");
        else if (ctx.getChild(0).getText().equals("verdadeiro"))
            outputFinal.append("1\n");
        else
            outputFinal.append("0\n");
        
        return null;
    }
  

    @Override
    public Void visitExpressao(AlgumaParser.ExpressaoContext ctx) {
        visitTermo_logico(ctx.termo_logico(0));
        for (int i = 0; i < ctx.op_logico_1().size(); i++) {
            outputFinal.append(" || ");
            visitTermo_logico(ctx.termo_logico(i + 1));
        }
        return null;
    }
    
}
