package mc2mc.analysis;

import ast.ASTNode;
import natlab.tame.tir.TIRForStmt;
import natlab.tame.tir.TIRIfStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;

/**
 * Code generator
 */
public class CodeGenerator extends TIRAbstractNodeCaseHandler {

    public CodeGenerator(){

    }

    @Override
    public void caseASTNode(ASTNode node) {
        int len = node.getNumChild();
        for(int i=0;i<len;i++){
            ASTNode currentNode = node.getChild(i);
            if(currentNode instanceof TIRNode){
                ((TIRNode) currentNode).tirAnalyze(this);
            }
            else {
                currentNode.analyze(this);
            }
        }
    }

    @Override
    public void caseTIRForStmt(TIRForStmt node){

    }

    @Override
    public void caseTIRIfStmt(TIRIfStmt node){

    }
}
