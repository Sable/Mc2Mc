package mc2mc.analysis;

import ast.ASTNode;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tir.TIRCallStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;

import java.util.Map;

/**
 *
 */
public class TirAnalysisLoopPrint extends TIRAbstractNodeCaseHandler {

    private TirAnalysisPropagateShape localVector;
    private int count = 0;

    public TirAnalysisLoopPrint(TirAnalysisPropagateShape tVector){
        localVector = tVector;
        count = 0;
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
    public void caseTIRCallStmt(TIRCallStmt node){
        PrintMessage.See("count = " + count++);
        PrintMessage.delimiter();
        printMap(localVector.getInFlowSets().get(node));
        PrintMessage.delimiter();
        PrintMessage.See(node.getPrettyPrinted());
        PrintMessage.delimiter();
        printMap(localVector.getOutFlowSets().get(node));
        PrintMessage.delimiter();
    }

    public void printMap(Map<String, PromotedShape> oneMap){
        PrintMessage.See("size = " + oneMap.size());
        for(String s : oneMap.keySet()) {
            PrintMessage.See("[" + s + "] -> " + oneMap.get(s).getPrettyPrinted());
        }
    }
}
