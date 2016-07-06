package mc2mc.mc2lib;

import ast.ASTNode;
import ast.AssignStmt;
import natlab.tame.tir.*;

/**
 * Created by wukefe on 7/2/16.
 */
public class UDFCheck {

    public UDFCheck(ASTNode node){
        valid = true;
        astTraversal(node);
    }

    public boolean isValid(){
        return valid;
    }

    private boolean valid = true;

    /**
     * Only allow:
     *    TIRIfStmt, AssignStmt
     */
    private void astTraversal(ASTNode node){
        if(node instanceof TIRIfStmt){
            ; //
        }
        else if(node instanceof AssignStmt){
            ;//
        }
        else if(node instanceof TIRForStmt
                || node instanceof TIRWhileStmt
                || node instanceof TIRReturnStmt
                || node instanceof TIRTryStmt
                || node instanceof TIRBreakStmt
                || node instanceof TIRGlobalStmt
                || node instanceof TIRPersistentSmt){
            valid = false;
            return;
        }
        int len = node.getNumChild();
        for(int i=0;i<len;i++){
            ASTNode currentNode = node.getChild(i);
            astTraversal(currentNode);
        }
    }


}
