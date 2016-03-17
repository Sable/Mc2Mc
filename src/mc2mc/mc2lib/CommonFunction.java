package mc2mc.mc2lib;

import ast.ASTNode;
import natlab.tame.builtin.Builtin;
import natlab.tame.tir.TIRStmt;
import natlab.toolkits.path.BuiltinQuery;

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
            if(currentNode instanceof ast.Name){
                //rtn.add(((NameExpr)currentNode).getName().getVarName());
                rtn.add(currentNode.getVarName());
            }
            else {
                rtn.addAll(ExtractName(currentNode));
            }
        }
        return rtn;
    }

    public static int FindLineNo(ASTNode e){
        int lno = 0;
        for(int i=0;i<e.getNumChild();i++){
            ASTNode currentNode = e.getChild(i);
            int localLno = 0;
            if(currentNode instanceof TIRStmt){
                localLno = currentNode.getStartLine();
            }
            if(localLno !=0){
                lno = localLno;
            }
        }
        return lno;
    }

    public static boolean IsBuiltIn(String name){
        BuiltinQuery query = Builtin.getBuiltinQuery();
        return query.isBuiltin(name);
    }

    public static Set<String> VarNameOnly(Set<String> inputSet){
        Set<String> rtn = new HashSet<>();
        for(String s : inputSet){
            if(IsBuiltIn(s)) ;
            else rtn.add(s);
        }
        return rtn;
    }

}
