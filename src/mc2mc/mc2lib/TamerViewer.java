package mc2mc.mc2lib;

import ast.ASTNode;
import natlab.tame.tir.*;

public class TamerViewer {

    private ASTNode root;
    private String[] structFunction = {"Output parameters","Function name","Input parameters","Help comments","Function body","Nested functions"};

    public TamerViewer(ASTNode node){
        root = node;
    }

    public void GetViewer(){
        PrintMessage.delimiter();
        TraversalNode(root, 0, -1);
        PrintMessage.delimiter();
    }

    private void TraversalNode(ASTNode node, int depth, int funcLayer) {
        int ident = depth * 2;
        int size = node.getNumChild();
        PrintIdent('>', ident);
        Msg(PrintNode(node) + ((funcLayer >= 0) ? " (" + structFunction[funcLayer] + ")" : (size > 0) ? " (children:" + size + ")" : ""));

        if (CanGoNext(node)) {
            for (int i = 0; i < size; i++) {
                TraversalNode(node.getChild(i), depth + 1, (node instanceof TIRFunction) ? i : -1);
            }
        }

    }

    private String PrintNode(ASTNode node){
        String rtn = "";
        int len = node.getNumChild();
        String sign = " --> ";
        if(node instanceof ast.Name){
            rtn = "ast.Name" + sign + node.getVarName();
        }
        else if(node instanceof ast.NameExpr){
            rtn = "ast.NameExpr" + sign + node.getVarName();
        }
        else if(node instanceof ast.List) {
            if (node instanceof TIRStatementList) {
                rtn = "TIRStatementList";
            }
            else{
                rtn = "ast.List"; // normal ast.list
            }
        }
        else if(node instanceof TIRAssignLiteralStmt){
            rtn = GenNodeString("TIRAssignLiteralStmt",sign,node);
        }
        else if(node instanceof TIRCallStmt){
            rtn = GenNodeString("TIRCallStmt",sign,node);
        }
        else if(node instanceof TIRForStmt){
            rtn = "TIRForStmt";
        }
        else if(node instanceof TIRWhileStmt){
            rtn = "TIRWhileStmt";
        }
        else if(node instanceof TIRIfStmt){
            rtn = "TIRIfStmt";
        }
        else if(node instanceof TIRArraySetStmt){
            rtn = GenNodeString("TIRArraySetStmt",sign,node);
        }
        else if(node instanceof TIRCopyStmt){
            rtn = GenNodeString("TIRCopyStmt",sign,node);
        }
        else if(node instanceof TIRFunction){
            rtn = "TIRFunction";
        }
        else if(node instanceof TIRCommentStmt){
            rtn = "TIRCommentStmt //";
        }
        else if(node instanceof TIRBreakStmt){
            rtn = "TIRBreakStmt";
        }
        else if(node instanceof TIRContinueStmt){
            rtn = "TIRContinueStmt";
        }
        else if(node instanceof TIRGlobalStmt){
            rtn = "TIRGlobalStmt";
        }
        else if(node instanceof ast.AssignStmt){ //should be put at the end
            rtn = GenNodeString("ast.AssignStmt",sign,node);
        }
        else if(node instanceof ast.IfBlock){
            rtn = "ast.IfBlock";
        }
        else if(node instanceof ast.ElseBlock){
            rtn = "ast.ElseBlock";
        }
        else if(node instanceof ast.Opt){
            rtn = "ast.Opt";
        }
        else{
            rtn = node.dumpString();
        }
        return rtn;
    }

    private boolean CanGoNext(ASTNode node){
        if(node instanceof TIRStatementList)
            return true;
        if(node instanceof TIRForStmt)
            return true;
        if(node instanceof TIRFunction)
            return true;
        if(node instanceof TIRWhileStmt)
            return true;
        if(node instanceof TIRIfStmt)
            return true;
        if(node instanceof TIRCallStmt)
            return true;
        if(node instanceof ast.List)
            return true;
        if(node instanceof ast.IfBlock)
            return true;
        if(node instanceof ast.ElseBlock)
            return true;
        if(node instanceof ast.Opt)
            return true;
        return false;
    }

    private void PrintIdent(char c, int n){
        for(int i=0;i<n;i++)
            System.out.print(' ');
        System.out.print(c);
    }

    private void Msg(String s){
        System.out.println(s);
    }

    private String GenNodeString(String name, String sign, ASTNode node){
        int size = 25 - name.length();
        String rtn = name;
        for(int i=0;i<size;i++) rtn += " ";
        rtn += (sign + "(" + node.getPrettyPrinted().trim() + ")");
        return rtn;
    }

    // do something here

}
