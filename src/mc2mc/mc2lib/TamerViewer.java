package mc2mc.mc2lib;

import ast.ASTNode;
import natlab.tame.tir.TIRStatementList;

public class TamerViewer {

    private ASTNode root;
    private int bound;

    public TamerViewer(ASTNode node){
        root = node;
        bound = 2;
    }

    public TamerViewer(ASTNode node, int depthLimit){
        root = node;
        bound = depthLimit;
    }

    public void GetViewer(){
        TravesalNode(root, 0);
        System.out.println("Viewer has done.");
    }

    private void TravesalNode(ASTNode node, int depth){
        if(depth >= bound) return;
        int ident = depth * 2;
        int size = node.getNumChild();
        PrintIdent(' ', ident); Msg(PrintNode(node));
        PrintIdent('|', ident); Msg(Integer.toString(size));

        for(int i=0;i<size;i++){
            ASTNode currentNode = node.getChild(i);
            PrintIdent('|',ident); Msg(PrintNode(currentNode));
            if(currentNode.getNumChild() > 0){
                TravesalNode(currentNode,depth+1);
            }
        }
    }

    private String PrintNode(ASTNode node){
        String rtn = node.dumpString();
        int len = node.getNumChild();
        String sign = " --> ";
        if(node instanceof ast.Name){
            rtn += sign + node.getVarName();
        }
        else if(node instanceof ast.NameExpr){
            rtn += sign + node.getVarName();
        }
        else if(node instanceof ast.List) {
            if (node instanceof TIRStatementList) {
                TIRStatementList slist = (TIRStatementList) node;
                rtn = "TIRStatementList" + sign + len + "\n";
                /*if (len < 5) {
                    int i = 0;
                    for (ast.Stmt s : slist) {
                        if(i > 0) rtn += "\n";
                        rtn += " - " + s.dumpString();
                        i ++;
                    }
                } else {
                    rtn += " - " + slist.getChild(0).dumpString() + "\n";
                    rtn += " - " + slist.getChild(1).dumpString() + "\n";
                    rtn += " - " + slist.getChild(len - 2).dumpString() + "\n";
                    rtn += " - " + slist.getChild(len - 1).dumpString();
                }*/
            }
            else{
                // normal ast.list
            }
        }
        return rtn;
    }

    private void PrintIdent(char c, int n){
        for(int i=0;i<n;i++)
            System.out.print(' ');
        System.out.print(c);
    }

    private void Msg(String s){
        System.out.println(s);
    }

    // do something here

}
