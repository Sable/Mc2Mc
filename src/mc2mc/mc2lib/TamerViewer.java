package mc2mc.mc2lib;

import ast.ASTNode;
import natlab.tame.tir.*;

public class TamerViewer {

    private ASTNode root;

    public TamerViewer(ASTNode node){
        root = node;
    }

    public TamerViewer(ASTNode node, int depthLimit){
        root = node;
    }

    public void GetViewer(){
        PrintMessage.Delimiter();
        TravesalNode(root, 0);
        PrintMessage.Delimiter();
    }

    private void TravesalNode(ASTNode node, int depth){
        int ident = depth * 2;
        int size = node.getNumChild();
        PrintIdent('>', ident); Msg(PrintNode(node) + " (children:" + size + ")");

        if(CanGoNext(node)) {
            for (int i = 0; i < size; i++) {
                ASTNode currentNode = node.getChild(i);
                //PrintIdent('|',ident); Msg(PrintNode(currentNode));
                //if (CanGoNext(currentNode)) {
                    TravesalNode(currentNode, depth + 1);
                //}
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
                // normal ast.list
                rtn = "ast.List";
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
        else if(node instanceof TIRArraySetStmt){
            rtn = GenNodeString("TIRArraySetStmt",sign,node);
        }
        else if(node instanceof TIRCopyStmt){
            rtn = GenNodeString("TIRCopyStmt",sign,node);
        }
        else if(node instanceof TIRFunction){
            rtn = "TIRFunction";
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
        if(node instanceof ast.List)
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
