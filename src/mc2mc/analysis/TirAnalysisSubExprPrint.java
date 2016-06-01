package mc2mc.analysis;

import ast.ASTNode;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tir.TIRCallStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;

import java.util.Set;

public class TirAnalysisSubExprPrint extends TIRAbstractNodeCaseHandler {

    private TirAnalysisSubExpr subexpranalysis;
    public TirAnalysisSubExprPrint(TirAnalysisSubExpr analysis){
        subexpranalysis = analysis;
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
    public void caseTIRCallStmt(TIRCallStmt callstmt){
        String stringrhs = callstmt.getRHS().getPrettyPrinted();
        PrintMessage.See("======");
//        PrintMessage.See("Input set:");
//        PrintMessage.See()
//        printSet(subexpranalysis.getInFlowSets().get(callstmt).get(stringrhs));
        PrintMessage.See("Output set: (" + callstmt.getPrettyPrinted().trim() + ")");
        PrintMessage.printMap(subexpranalysis.getOutFlowSets().get(callstmt));
//        printSet(subexpranalysis.getOutFlowSets().get(callstmt).get(stringrhs));
    }


    private void printSet(Set<TIRNode> defs) {
        if(defs == null){
            System.out.println("set: null");
            return;
        }
        int cnt = 0;
        for (TIRNode def : defs) {
            System.out.printf(" [%d] %s\n",cnt++,((ASTNode)def).getPrettyPrinted().trim());
            //System.out.printf("%s at [%s, %s]\n", ((NameExpr)def.getLHS()).getName().getID(), def.getStartLine(), def.getStartColumn());
        }
    }
}
