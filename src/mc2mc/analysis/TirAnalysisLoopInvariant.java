package mc2mc.analysis;


import ast.ASTNode;
import ast.AssignStmt;
import ast.Stmt;
import mc2mc.mc2lib.CommonFunction;
import mc2mc.mc2lib.PrintMessage;
import mc2mc.mc2lib.TamerViewer;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tamerplus.analysis.ReachingDefinitions;
import natlab.tame.tir.TIRForStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;

import java.util.*;

public class TirAnalysisLoopInvariant extends TIRAbstractNodeCaseHandler {

    private ASTNode root;
    private ReachingDefinitions localReachingDef;

    private Set<ASTNode> skips;
    private boolean debug = false;
    private TamerViewer tv = null;

    public TirAnalysisLoopInvariant(ASTNode tree, AnalysisEngine engine){
        root = tree;
        localReachingDef = engine.getReachingDefinitionsAnalysis();
        skips = new HashSet<>();
        tv = new TamerViewer(tree);
    }

    @Override
    public void caseASTNode(ASTNode node) {
        int totalNode = node.getNumChild();

//        if(node.getStartLine() != 0) {
//            PrintMessage.See("== line " + node.getStartLine());
//            PrintMessage.See(node.dumpString());
//        }

//        if(node instanceof TIRFunction){
//            CommonFunction.DebugNodeStructure(node);
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
        PrintMessage.See("Entering caseForStmt statement ");

        Set<String> nameList = new HashSet<>();
        Map<String, Set<TIRNode>> inputs = localReachingDef.getReachingDefinitionsForNode(node);
        //PrintMessage.PrintTirNodeSet(localReachingDef.getReachingDefinitionsForNode(node));
        Set<Stmt> insideLoop = new HashSet<>();

        ast.List<Stmt> nodeList = node.getStmtList();
        // fetch information from for loops
        for(Stmt s : nodeList){
            if(s instanceof AssignStmt) {
                insideLoop.add(s);
                // nameList : all names
                nameList.addAll(CommonFunction.VarNameOnly(CommonFunction.ExtractName(((AssignStmt)s).getRHS())));
            }
        }

        if(debug) {
            PrintMessage.StringList(nameList);
        }

        for(Stmt s : nodeList){ // Are statements in order?
            if(s instanceof AssignStmt) {
                Set<String> localNameList = CommonFunction.VarNameOnly(CommonFunction.ExtractName(((AssignStmt) s).getRHS()));
                boolean canMove = true;
                for (String oneName : localNameList) {
                    PrintMessage.See("one name : " + oneName);
                    Set<TIRNode> stmtList = inputs.get(oneName);
                    for (TIRNode tnode : stmtList) {
                        if (insideLoop.contains(tnode)) {
                            canMove = false;
                            break;
                        }
                    }
                    if (!canMove) break;     // can move when all vars defined outside
                }
                if (canMove) {
                    PrintMessage.See("before: " + nodeList.getNumChild());
                    PrintMessage.See("can move : " + s.getPrettyPrinted());
                    int rmv = MoveNodes((ASTNode) s);  // move statement out of the loop
                    if(rmv > 0) {
                        insideLoop.remove(s);   // update the set
                    }
                    PrintMessage.See("after: " + nodeList.getNumChild());
                }
            }
        }

        skips.add(node);
        // go to next node
        caseASTNode(node);
    }

    private int MoveNodes(ASTNode node){
        ASTNode closepNode = node.getParent(); //close parent
        if (closepNode == null) return -1;
        ASTNode parentNode = node.getParent().getParent(); //get stmt FOR
        if (parentNode == null) return -1;
        ASTNode grandpNode = parentNode.getParent(); // statement lists
        if (grandpNode == null) return -1;
        int grandpCursor = grandpNode.getIndexOfChild(parentNode);
        int removeCursor = node.getParent().getIndexOfChild(node);
        PrintMessage.See("grandpCursor: " + grandpCursor + "; removeCursor: " + removeCursor);
        grandpNode.insertChild(node, grandpCursor);
        //closepNode.removeChild(removeCursor); //remove old node
        return removeCursor;
    }


    /**
     * Change it to analyze?
    */
    public void run(){
        //caseASTNode(root); //run the root
        //PrintMessage.See("root: " + root.dumpString());
        //PrintMessage.See("Is tir node? " + (root instanceof TIRNode));
        root.analyze(this);
        PrintMessage.See("After loop invariants:");
        tv.GetViewer(); // final output
    }

}
