package mc2mc.analysis;

import ast.ASTNode;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by wukefe on 6/14/16.
 */
public class DepNode {
    List<DepNode> child, parent;
    List<Integer> cedge, pedge; // 1: flow; 2: anti; 3:output
    // 0: reduction (a=a+1;)
    ASTNode stmt;

    public DepNode(ASTNode from){
        child = new LinkedList<>();
        cedge = new LinkedList<>();
        parent= new LinkedList<>();
        pedge = new LinkedList<>();
        stmt  = from;
    }

    public void setChild(DepNode x, int kind){
        child.add(x);
        cedge.add(kind);
        x.setParent(this, kind);
    }

    void setParent(DepNode x, int kind){
        parent.add(x);
        pedge.add(kind);
    }

    public List<DepNode> getChild(){
        return child;
    }

    public ASTNode getStmt(){
        return stmt;
    }

    public String getPrettyPrinted(){
        String rtn = "";
        if(child.size()>0) {
            rtn += printStmt(stmt);
            rtn += printChild();
        }
        return rtn;
    }

    public String printChild(){
        String rtn = "\n";
        for(int i=0;i<child.size();i++){
            rtn += " -> " + "[" + cedge.get(i) + "] " + printStmt(child.get(i).getStmt()) + "\n";
        }
        return rtn;
    }

    public String printStmt(ASTNode stmt){
        return stmt.getPrettyPrinted().trim();
    }
}

