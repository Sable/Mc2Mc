package mc2mc.mc2lib;

import ast.ASTNode;
import ast.NameExpr;

import java.util.HashSet;
import java.util.Set;

public class CommonFunction {

    /**
     * Input with an expr (i.e getRHS)
     * Return a list of name
     */
    public static Set<String> ExtractName(ASTNode e){
        Set<String> rtn = new HashSet<>();
        for(int i=0;i<e.getNumChild();i++){
            ASTNode currentNode = e.getChild(i);
            if(currentNode instanceof NameExpr){
                rtn.add(((NameExpr)currentNode).getName().getVarName());
            }
            else {
                rtn.addAll(ExtractName(currentNode));
            }
        }
        return rtn;
    }
}
