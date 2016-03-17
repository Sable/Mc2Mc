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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TirAnalysisLoopInvariant extends TIRAbstractNodeCaseHandler {

    private ASTNode root;
    private ReachingDefinitions localReachingDef;

    private Set<ASTNode> skips;

    public TirAnalysisLoopInvariant(ASTNode tree, AnalysisEngine engine){
        root = tree;
        localReachingDef = engine.getReachingDefinitionsAnalysis();
        skips = new HashSet<>();
    }

    @Override
    public void caseASTNode(ASTNode node) {
        int totalNode = node.getNumChild();

//        if(node.getStartLine() != 0) {
//            PrintMessage.See("== line " + node.getStartLine());
//            PrintMessage.See(node.dumpString());
//        }
        for(int i=0;i<totalNode;i++){
            ASTNode currentNode = node.getChild(i);
            //PrintMessage.See("current node: " + currentNode.dumpString());
            if(currentNode instanceof TIRNode){
                ((TIRNode)currentNode).tirAnalyze(this);
            }
            else{
                currentNode.analyze(this);
            }
            skips.add(currentNode);
        }
    }

    // consider both for and while loops
    @Override
    public void caseTIRForStmt(TIRForStmt node){
        if(skips.contains(node)) return;
        PrintMessage.See("Entering caseForStmt statement " + node.getStartLine());

        Set<String> nameList = new HashSet<>();
        Map<String, Set<TIRNode>> inputs = localReachingDef.getReachingDefinitionsForNode(node);
        //PrintMessage.PrintTirNodeSet(localReachingDef.getReachingDefinitionsForNode(node));
        Set<Stmt> insideLoop = new HashSet<>();

        ast.List<Stmt> nodeList = node.getStmtList();
        // fetch information from for loops
        for(Stmt s : nodeList){
            if(s instanceof AssignStmt) {
                //PrintMessage.See("assignment : " + s.getPrettyPrinted());
                insideLoop.add(s);
                nameList.addAll(CommonFunction.VarNameOnly(CommonFunction.ExtractName(((AssignStmt)s).getRHS())));
            }
        }

        PrintMessage.StringList(nameList);

        for(String oneName : nameList){
            Set<TIRNode> stmtList = inputs.get(oneName);
            for(TIRNode n : stmtList) {
                if(!insideLoop.contains(n)){
                    PrintMessage.See("stmt: " + ((ASTNode)n).getPrettyPrinted());
                }
                //PrintMessage.See("stmt: " + t.getPrettyPrinted());
                //PrintMessage.See("    : " + oneName + " -> " + mapInsideLoop.get(t)); //true or null
            }
        }
        for(Stmt s : nodeList){
            if(s instanceof AssignStmt){
                //PrintMessage.StringList(nameList);
//                for(String one : nameList){
//                    Set<TIRNode> temp = inputs.get(one);
//                    if(temp != null) {
//                        PrintMessage.See(one + "  size of set " + temp.size());
//                        for (TIRNode tnode : temp) {
//                            //PrintMessage.GetPrettyPrintTirNode(tnode);
//                        }
//                    }
//                    else{
//                        if(!CommonFunction.IsBuiltIn(one)) {
//                            PrintMessage.See("- " + one + " is NOT a built-in.");
//                        }
//                    }
//                }
            }
        }

        skips.add(node);
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
