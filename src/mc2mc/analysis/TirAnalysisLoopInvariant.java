package mc2mc.analysis;


import ast.ASTNode;
import ast.AssignStmt;
import ast.Stmt;
import mc2mc.mc2lib.CommonFunction;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tamerplus.analysis.ReachingDefinitions;
import natlab.tame.tir.TIRForStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;

import java.util.Map;
import java.util.Set;

public class TirAnalysisLoopInvariant extends TIRAbstractNodeCaseHandler {

    private ASTNode root;
    private ReachingDefinitions localReachingDef;

    public TirAnalysisLoopInvariant(ASTNode tree, AnalysisEngine engine){
        root = tree;
        localReachingDef = engine.getReachingDefinitionsAnalysis();
    }

    @Override
    public void caseASTNode(ASTNode node) {
        int totalNode = node.getNumChild();
        for(int i=0;i<totalNode;i++){
            ASTNode currentNode = node.getChild(i);
            //PrintMessage.See("current node: " + currentNode.dumpString());
            if(currentNode instanceof TIRNode){
                ((TIRNode)currentNode).tirAnalyze(this);
            }
            else{
                currentNode.analyze(this);
            }
        }
    }

    @Override
    public void caseTIRForStmt(TIRForStmt node){
        PrintMessage.See("Entering caseForStmt statement " + node.getStartLine());

        Map<String, Set<TIRNode>> inputs = localReachingDef.getReachingDefinitionsForNode(node);
        //PrintMessage.PrintTirNodeSet(localReachingDef.getReachingDefinitionsForNode(node));

        ast.List<Stmt> nodeList = node.getStmtList();
        for(Stmt s : nodeList){
            if(s instanceof AssignStmt){
                Set<String> nameList = CommonFunction.ExtractName(((AssignStmt)s).getRHS());
                //PrintMessage.StringList(nameList);
                for(String one : nameList){
                    Set<TIRNode> temp = inputs.get(one);
                    if(temp != null) {
                        PrintMessage.See(one + "  size of set " + temp.size());
                        for (TIRNode tnode : temp) {
                            PrintMessage.GetPrettyPrintTirNode(tnode);
                        }
                    }
                }
            }
        }

        // go to next node
        caseASTNode(node);
    }

    /**
     * Change it to analyze?
    */
    public void run(){
        //caseASTNode(root); //run the root
        //PrintMessage.See("root: " + root.dumpString());
        //PrintMessage.See("Is tir node? " + (root instanceof TIRNode));
        root.analyze(this);
    }

}
