package mc2mc.analysis;

import ast.ASTNode;
import ast.AssignStmt;
import mc2mc.mc2lib.PrintMessage;
import mc2mc.mc2lib.TopologicalSort;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.TIRCommentStmt;
import natlab.tame.tir.TIRForStmt;
import natlab.tame.tir.TIRIfStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;

import java.util.*;


/**
 * Created by wukefe on 6/1/16.
 */
public class TirAnalysisLoop extends TIRAbstractNodeCaseHandler {

    public Map<TIRNode, Map<String, Set<TIRNode>>> fUDMap = null;
    private Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> fValueMap;
    private AnalysisEngine localEngine = null;
    private Map<ASTNode, List<String>> stmtHashMap = null; //<TIRForStmt, list<String>>
    private String currentFuncName;
    public static boolean debug = false;

    public TirAnalysisLoop(AnalysisEngine engine,
                           Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> valueMap,
                           String funcName){
        fUDMap = engine.getUDChainAnalysis().getChain(); //UDChain
        fValueMap = valueMap;
        localEngine = engine;
        stmtHashMap = new HashMap<>();
        currentFuncName = funcName;
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
    public void caseTIRForStmt(TIRForStmt node) {
        // do something
//        PrintMessage.See("entering");
        findInnerFor(node);
    }

    public Map<ASTNode, List<String>> getStmtHashMap(){
        return stmtHashMap;
    }

    /**
     * Find the innermost loop
     *
     * Example: in/test_loop.m
    */
    private int findInnerFor(ASTNode node) {
        int len = node.getNumChild();
        boolean isFor = node instanceof TIRForStmt;
        int numberFor = isFor?1:0;
        for(int i=0;i<len;i++){
            ASTNode t = node.getChild(i);
            numberFor += findInnerFor(t);
        }
        if(isFor && numberFor == 1 && innermostLoop(node)) {
            PrintMessage.delimiter();
            PrintMessage.See("Getting outFlow", "AnalysisLoop");
            TirAnalysisPropagateShape tirPS = new TirAnalysisPropagateShape(node, fValueMap);
            tirPS.analyze();
            if(debug)
                node.analyze(new TirAnalysisLoopPrint(tirPS));
            Map<ASTNode, Map<String, PromotedShape>> outFlow = tirPS.getOutFlowSets();
            Map<ASTNode, String> transposeMap = tirPS.transposeMap;
            PrintMessage.See("Getting depGraph", "AnalysisLoop");
            TirAnalysisDep tirDep = new TirAnalysisDep(localEngine, node);
            tirDep.run();
            PrintMessage.See("Getting newFor", "AnalysisLoop");
            Map<ASTNode, DepNode> depGraph = tirDep.getDepGraph();
            java.util.List<String> newFor = vectorizeFor(node, outFlow, depGraph, transposeMap);
            stmtHashMap.put(node, newFor);
        }
        return numberFor;
    }

    private java.util.List<String> vectorizeFor(ASTNode iFor,
                                             Map<ASTNode, Map<String, PromotedShape>> outFlow,
                                             Map<ASTNode, DepNode> depGraph,
                                             Map<ASTNode, String>transposeMap){
        TIRForStmt tfor = (TIRForStmt)iFor;
        java.util.List<ASTNode> loopStmts = new ArrayList<>();
        java.util.List<String>  outString = new ArrayList<>();

        TopologicalSort tSort = new TopologicalSort(depGraph);
        Map<ASTNode, Map<ASTNode, DepNode>> node2Map = tSort.getGroups();
        Map<Map<ASTNode, DepNode>, Boolean> nodeFlag = new HashMap<>();
        Map<Map<ASTNode, DepNode>, Boolean> hasCyclicMap= tSort.getIsCyclic();
        java.util.List<String> newFor = new ArrayList<>();
        Map<ASTNode, String> modifiedMap = new HashMap<>(); //special patterns
        Map<Map<ASTNode, DepNode>, Boolean> groupSpecial = new HashMap<>();

        for(ASTNode stmt : tfor.getStatements()){
            // generate sequential code here
            if(stmt instanceof TIRCommentStmt) continue;; // block it?
            Map<ASTNode, DepNode> group = node2Map.get(stmt);
            if(!nodeFlag.containsKey(group) || nodeFlag.get(group)){
                boolean notTop = true;
                Map<String, PromotedShape> allValue = outFlow.get(stmt);
                for(String s : allValue.keySet()){
                    if(allValue.get(s).isT()) {
                        notTop = false;
                        break;
                    }
                }
                nodeFlag.put(group, notTop);
                boolean isAcyclic = hasCyclicMap.get(group);
                if(!notTop || isAcyclic){
                    // add all of statements
                    if(notTop && isAcyclic){
                        if(!groupSpecial.containsKey(group)) {
                            boolean status = false;
                            if(tSort.matchIdiom1(group, null, null)) {
                                java.util.List<String> remainedString = new ArrayList<>();
                                tSort.matchIdiom1(group, modifiedMap, remainedString);
                                addVectorizableGroup(outString, group, modifiedMap, transposeMap);
                                for (String s : remainedString) {
                                    outString.add(s);
                                }
                                status = true;
                            }
                            else {
                                status = false;
                                loopStmts.add(stmt);
                            }
                            groupSpecial.put(group, status);
                        }
                        else if(!groupSpecial.get(group)){
                            loopStmts.add(stmt);
                        }
                    }
                    else {
                        loopStmts.add(stmt);
                    }
                }
            }
            else {
                loopStmts.add(stmt);
            }
        }

        for(Map<ASTNode, DepNode> group : hasCyclicMap.keySet()){
            if(!hasCyclicMap.get(group) && nodeFlag.get(group)){
                // is acyclic
                addVectorizableGroup(outString, group, null, transposeMap);
            }
        }
        // generate code
        if(outString.size() > 0) {
            newFor.add(vectorizeStmt(tfor.getAssignStmt()));
            for (String s : outString) {
                newFor.add(s);
            }
        }
        if(loopStmts.size() > 0){
            // remained for
            newFor.add("for "+vectorizeStmt(tfor.getAssignStmt()));
            for(ASTNode a : loopStmts) {
                newFor.add(vectorizeStmt(a));
            }
            newFor.add("end");
        }

        return newFor;
    }

    public void addVectorizableGroup(java.util.List<String> outString,
                                     Map<ASTNode, DepNode> group,
                                     Map<ASTNode, String> modifiedMap,
                                     Map<ASTNode, String> transposeMap){
        TopologicalSort ts = new TopologicalSort(group);
        java.util.List<ASTNode> sortedList = ts.sort();
        for(ASTNode a : sortedList){
            if(modifiedMap!=null && modifiedMap.containsKey(a))
                outString.add(modifiedMap.get(a));
            else {
                if(transposeMap.containsKey(a)){
                    for(String s : transposeMap.get(a).split("\\\n")){
                        outString.add(s);
                    }
                }
                else {
                    outString.add(vectorizeStmt(a));
                }
            }
        }
//        outStmts.addAll(sortedList);
    }

    public String vectorizeStmt(ASTNode node){
        return node.getPrettyPrinted().trim();
    }

    // think more

    /**
     * Input with an innermost loop, ifor
     *  1) check valid innermostLoop
     *  2) only assignment and comment statems allowed
     *  3) Control statements are not allowed (mentioned in the diagram)
     */
    public boolean innermostLoop(ASTNode ifor){
        boolean isNestStmt = (ifor instanceof TIRForStmt || ifor instanceof TIRIfStmt);
        ASTNode newNode = isNestStmt?ifor.getChild(1):ifor;
        int len = newNode.getNumChild();
        boolean res = true;
        for(int i=0;i<len;i++){
            ASTNode currentNode = newNode.getChild(i);
            if(currentNode instanceof AssignStmt);
            else if(currentNode instanceof TIRCommentStmt); // %comment
            else res = false;

            if(res == false) {
                PrintMessage.See("No interesting loop found at: " + currentNode.dumpString());
                return false;
            }
        }
        return true;
    }

}
