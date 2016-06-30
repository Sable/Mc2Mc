package mc2mc.analysis;

import ast.*;
import mc2mc.mc2lib.CommonFunction;
import mc2mc.mc2lib.PrintMessage;
import mc2mc.mc2lib.TopologicalSort;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.TIRCommentStmt;
import natlab.tame.tir.TIRForStmt;
import natlab.tame.tir.TIRIfStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.ValueSet;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.tame.valueanalysis.components.constant.Constant;
import natlab.tame.valueanalysis.components.shape.Shape;

import java.util.*;
import java.util.List;


/**
 * Created by wukefe on 6/1/16.
 */
public class TirAnalysisLoop extends TIRAbstractNodeCaseHandler {

    public Map<TIRForStmt, Set<Expr>> exprRead = new HashMap<>();
    public Map<TIRForStmt, Set<Expr>> exprWrite= new HashMap<>();
    public Map<TIRNode, Map<String, Set<TIRNode>>> fUDMap = null;
    private Map<TIRNode, Set<TIRNode>> loopDep = null;
    private Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> fValueMap;
    private AnalysisEngine localEngine = null;
    private Map<ASTNode, List<String>> stmtHashMap = null; //<TIRForStmt, list<String>>

    public TirAnalysisLoop(AnalysisEngine engine, Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> valueMap){
        fUDMap = engine.getUDChainAnalysis().getChain(); //UDChain
        fValueMap = valueMap;
        localEngine = engine;
        stmtHashMap = new HashMap<>();
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
        PrintMessage.See("entering");
        findInnerFor(node);
    }

    private void collectRW(TIRForStmt node){
        ast.List<Stmt> allStmt = node.getStmtList();
        Set<Expr> stmtRead  = new HashSet<>();
        Set<Expr> stmtWrite = new HashSet<>();
        String iterator = node.getAssignStmt().getLHS().getVarName();
//        PrintMessage.See(iterator, "iterator");
        for(Stmt s : allStmt){
            if(s instanceof AssignStmt){
                // collect left and right site
                stmtRead.addAll(collectRead(((AssignStmt) s).getRHS()));
                stmtWrite.addAll((collectWrite(((AssignStmt) s).getLHS())));
            }
        }
        processIndex(iterator,stmtRead,stmtWrite,fUDMap.get(node));
        exprRead.put(node, stmtRead);
        exprWrite.put(node, stmtWrite);
    }

    private Set<Expr> collectRead(Expr rhs){
        Set<Expr> rtn = new HashSet<>();
        if(rhs instanceof ast.ParameterizedExpr){
            String opName = rhs.getVarName();
            PrintMessage.See(rhs.getPrettyPrinted());
            PrintMessage.See(opName);
            if(CommonFunction.isBuiltIn(opName)) {
                for(Expr e : ((ParameterizedExpr) rhs).getArgList()){
                    addInSet(rtn, e); // add arguments
                }
            }
            else {
                // not a builtin --> array indexing
                addInSet(rtn, rhs);
            }
        }
        else if(rhs instanceof NameExpr){
            addInSet(rtn, rhs);
        }
        return rtn;
    }

    private Set<Expr> collectWrite(Expr lhs){
        Set<Expr> rtn = new HashSet<>();
        if(lhs instanceof MatrixExpr){
            addInSetAll(rtn, lhs.getNameExpressions());
        }
        else {
            addInSet(rtn, lhs); // maybe more cases
        }
        return rtn;
    }

    public Map<ASTNode, List<String>> getStmtHashMap(){
        return stmtHashMap;
    }

    /*
     See the example in the folder: in/test_loop.m
    */
    private int findInnerFor(ASTNode node) {
        int len = node.getNumChild();
        boolean isFor = node instanceof TIRForStmt;
        int numberFor = isFor?1:0;
        for(int i=0;i<len;i++){
            ASTNode t = node.getChild(i);
            numberFor += findInnerFor(t);
        }
        int op = 1;
        if(isFor && numberFor == 1 && innermostLoop(node)) {
//            collectRW((TIRForStmt)node);
//            buildDepGraph((TIRForStmt)node);
//            processStmt((TIRForStmt) node, fValueMap.get(node));
            PrintMessage.See("Getting outFlow", "AnalysisLoop");
            TirAnalysisPropagateShape tirPS = new TirAnalysisPropagateShape(node, fValueMap);
            tirPS.analyze();
            node.analyze(new TirAnalysisLoopPrint(tirPS));
            Map<ASTNode, Map<String, PromotedShape>> outFlow = tirPS.getOutFlowSets();
            PrintMessage.See("Getting depGraph", "AnalysisLoop");
            TirAnalysisDep tirDep = new TirAnalysisDep(localEngine, node);
            tirDep.run();
            PrintMessage.See("Getting newFor", "AnalysisLoop");
            Map<ASTNode, DepNode> depGraph = tirDep.getDepGraph();
            java.util.List<String> newFor = vectorizeFor(node, outFlow, depGraph);
            stmtHashMap.put(node, newFor);
        }
        return numberFor;
    }

    private java.util.List<String> vectorizeFor(ASTNode iFor,
                                             Map<ASTNode, Map<String, PromotedShape>> outFlow,
                                             Map<ASTNode, DepNode> depGraph){
        TIRForStmt tfor = (TIRForStmt)iFor;
        String iterator = tfor.getLoopVarName().getID();
        String colonExpr= tfor.getAssignStmt().getRHS().getPrettyPrinted();
        java.util.List<ASTNode> loopStmts = new ArrayList<>();
//        java.util.List<ASTNode> outStmts  = new ArrayList<>();
        java.util.List<String>  outString = new ArrayList<>();

        // debug depGraph
//        for(ASTNode a : depGraph.keySet()){
//            PrintMessage.See("key: " + a.getPrettyPrinted().trim());
//            PrintMessage.See(depGraph.get(a).printChild());
//        }

        TopologicalSort tSort = new TopologicalSort(depGraph);
        Map<ASTNode, Map<ASTNode, DepNode>> node2Map = tSort.getGroups();
        Map<Map<ASTNode, DepNode>, Boolean> nodeFlag = new HashMap<>();
        Map<Map<ASTNode, DepNode>, Boolean> hasCyclicMap= tSort.getIsCyclic();
        java.util.List<String> newFor = new ArrayList<>();
        Map<ASTNode, String> modifiedMap = new HashMap<>(); //special patterns
        Set<Map<ASTNode, DepNode>> groupSpecial = new HashSet<>();

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
                        if(!groupSpecial.contains(group))
                            groupSpecial.add(group);
                    }
                    else {
                        loopStmts.add(stmt);
                    }
                }
            }
        }
        //special group
        for(Map<ASTNode, DepNode> g : groupSpecial){
            if(tSort.matchIdiom1(g, null, null)) {
                java.util.List<String> remainedString = new ArrayList<>();
                tSort.matchIdiom1(g, modifiedMap, remainedString);
                addVectorizableGroup(outString, g, modifiedMap);
                for (String s : remainedString) {
                    outString.add(s);
                }
            }
        }

        for(Map<ASTNode, DepNode> group : hasCyclicMap.keySet()){
            if(!hasCyclicMap.get(group)){
                // is acyclic
                addVectorizableGroup(outString, group, null);
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
                                     Map<ASTNode, String> modifiedMap){
        TopologicalSort ts = new TopologicalSort(group);
        java.util.List<ASTNode> sortedList = ts.sort();
        for(ASTNode a : sortedList){
            if(modifiedMap!=null && modifiedMap.containsKey(a))
                outString.add(modifiedMap.get(a));
            else
                outString.add(vectorizeStmt(a));
        }
//        outStmts.addAll(sortedList);
    }

    public String vectorizeStmt(ASTNode node){
        return node.getPrettyPrinted().trim();
    }

    public void addInSet(Set<Expr> x, Expr e){
        if(e instanceof NameExpr){
            if(CommonFunction.isTemp(((NameExpr) e).getName().getID())) return ;
        }
        x.add(e);
    }

    public void addInSetAll(Set<Expr> x, Set<NameExpr> eAll){
        for(Expr e:eAll){
            addInSet(x,e);
        }
    }

    // process

    private void processIndex(String iter, Set<Expr> setRead, Set<Expr> setWrite,Map<String, Set<TIRNode>> def){
        for(Expr r : setRead){
            for(Expr w : setWrite) {
                if(r instanceof ast.ParameterizedExpr
                        && w instanceof ast.ParameterizedExpr
                        && r.getVarName().equals(w.getVarName())){ //same array
                    // we test one vector first
                    if(((ParameterizedExpr) r).getNumArg()==1
                            && ((ParameterizedExpr) w).getNumArg()==1){
                        Expr rp0 = ((ParameterizedExpr) r).getArg(0);
                        Expr lp0 = ((ParameterizedExpr) w).getArg(0);
                        int[] rc = new int[2];
                        int[] lc = new int[2];
                        boolean f1 = findPattern(iter, rp0, def, rc);
                        boolean f2 = findPattern(iter, lp0, def, lc);
                        if(f1 && f2){
                            if(rc[1] == lc[1] && rc[1] == 0){
                                // a1 * i1 + b1 = a2 * i2 + b2
                            }
                        }
                    }
                }
            }
        }
    }

    /*
    a*i+b or a*i-b or b+a*i or b-a*i
     */
    private boolean findPattern(String iter, Expr x, Map<String, Set<TIRNode>> def, int[] rtn){
        rtn[0] = rtn[1] = 0; boolean f = false;
        if(x instanceof NameExpr){
            String nameX = x.getVarName();
            if(nameX.equals(iter)) { rtn[0] = 1; f=true; } //(a,b) -> (1,0)
            else if(CommonFunction.isTemp(nameX)){
                Set<TIRNode> pre1 = def.get(nameX);
                if(pre1.size() == 1){
                    TIRNode preNode = (TIRNode)(pre1.toArray()[0]);
                    if(preNode instanceof ast.ParameterizedExpr){
                        String op = ((ParameterizedExpr) preNode).getVarName(); // plus or minus
                        if(op.equals("plus") || op.equals("minus")){
                            boolean fop = op.equals("minus");
                            Expr arg0 = ((ParameterizedExpr) preNode).getArg(0);
                            Expr arg1 = ((ParameterizedExpr) preNode).getArg(1);
                            if(arg0 instanceof IntLiteralExpr
                                    && Constant.get((IntLiteralExpr)arg0).isScalar()){ // b +/- a*i
                                rtn[1] = ((IntLiteralExpr) arg0).getValue().getValue().intValue();
                                if(arg1 instanceof NameExpr
                                        && CommonFunction.isTemp(arg1.getVarName())){
                                    f = findMult(iter, fUDMap.get(preNode).get(arg1.getVarName()),rtn);
                                }
                                if(f&&fop) rtn[0]=-rtn[0];
                            }
                            else if(arg1 instanceof IntLiteralExpr
                                    && Constant.get((IntLiteralExpr)arg1).isScalar()){ // a*i +/- b
                                rtn[1] = ((IntLiteralExpr) arg1).getValue().getValue().intValue();
                                if(arg0 instanceof NameExpr
                                        && CommonFunction.isTemp(arg0.getVarName())){
                                    f = findMult(iter, fUDMap.get(preNode).get(arg0.getVarName()),rtn);
                                }
                                if(f&&fop) rtn[1]=-rtn[1];
                            }
                        }
                    }
                }
            }
        }
        return f;
    }

    private boolean findMult(String iter, Set<TIRNode> pre2, int[] rtn){
        boolean f = false;
        if(pre2.size() == 1){
            TIRNode preNode = (TIRNode)(pre2.toArray()[0]);
            if(preNode instanceof ast.ParameterizedExpr){
                String op = ((ParameterizedExpr) preNode).getVarName();
                if(op.equals("mtimes") || op.equals("times")){ // times?
                    Expr arg0 = ((ParameterizedExpr) preNode).getArg(0);
                    Expr arg1 = ((ParameterizedExpr) preNode).getArg(1);
                    if(arg0 instanceof NameExpr
                            && arg0.getVarName().equals(iter)){
                        if(arg1 instanceof IntLiteralExpr
                                && Constant.get((IntLiteralExpr)arg1).isScalar()){
                            rtn[0] = ((IntLiteralExpr) arg1).getValue().getValue().intValue();
                            f = true;
                        }
                    }
                    else if(arg1 instanceof NameExpr
                            && arg0.getVarName().equals(iter)){
                        if(arg0 instanceof IntLiteralExpr
                                && Constant.get((IntLiteralExpr)arg0).isScalar()){
                            rtn[0] = ((IntLiteralExpr) arg0).getValue().getValue().intValue();
                            f = true;
                        }
                    }
                }
            }
        }
        return f;
    }


    public void printRWSet(){
        PrintMessage.See("Read set:");
        printSet0(exprRead);
        PrintMessage.See("Write set:");
        printSet0(exprWrite);
    }

    public void printSet0(Map<TIRForStmt, Set<Expr>> exprSet){
        for(TIRForStmt f : exprSet.keySet()){
            PrintMessage.See(f.getPrettyPrinted(),"For statement");
            for(Expr e : exprSet.get(f)){
                PrintMessage.See(e.getPrettyPrinted());
            }
            PrintMessage.delimiter();
        }
    }

    // think more

    /*
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

    /*
    Block
     */
    public void buildDepGraph(TIRForStmt node){
        Set<ASTNode> allAssignment = gatherDefs(node);
        loopDep = new HashMap<>();
        buildDepGraphSub(node, allAssignment);
//        PrintMessage.printMap2(loopDep);
    }

    public void buildDepGraphSub(ASTNode node, Set<ASTNode> all){
        boolean isNestStmt = (node instanceof TIRForStmt || node instanceof TIRIfStmt);

        Map<String, Set<TIRNode>> def0 = fUDMap.get(node);
        if(def0 == null){
            PrintMessage.See("debug: " + node.getPrettyPrinted());
            return ;
        }

        Set<TIRNode> from = new HashSet<>();
        for(String s : def0.keySet()){
            for(TIRNode t : def0.get(s)){
                if(all.contains(t)){
                    from.add(t);
                }
            }
        }
        loopDep.put((TIRNode)node, from);

        if(isNestStmt) {
            ASTNode stmtList = node.getChild(1);
            int len = stmtList.getNumChild();
            for (int i = 0; i < len; i++) {
                buildDepGraphSub(stmtList.getChild(i), all);
            }
        }
    }

    private Set<ASTNode> gatherDefs(ASTNode node){
        boolean isNestStmt = (node instanceof TIRForStmt || node instanceof TIRIfStmt);
        Set<ASTNode> res = new HashSet<>();
        if(node instanceof AssignStmt) res.add(node);

        if(isNestStmt){
            res.add(node.getChild(0));
            ASTNode stmtList = node.getChild(1);
            int len = stmtList.getNumChild();
            for(int i=0;i<len;i++){
                res.addAll(gatherDefs(stmtList.getChild(i)));
            }
        }
        return res;
    }

    /*
    Under construction
     */

    private void processStmt(TIRForStmt node, ValueFlowMap<AggrValue<BasicMatrixValue>> valueMap){
        int len = node.getNumChild();
        String iter = node.getLoopVarName().getID();
        for(int i=0;i<len;i++){
            initStmt(node.getChild(i), iter, (RangeExpr)node.getAssignStmt().getRHS(), valueMap);
        }
    }

    private Map<ASTNode, Map<String, PromotedShape>> initStmt(ASTNode node, String iter, RangeExpr re,
                                                              ValueFlowMap<AggrValue<BasicMatrixValue>> valueMap){
        boolean isNestStmt = (node instanceof TIRForStmt || node instanceof TIRIfStmt);
        int len = node.getNumChild();
        Map<ASTNode, Map<String, PromotedShape>> res = new HashMap<>();
        if(node instanceof AssignStmt){
            for(int i=0;i<len;i++){
                ASTNode currentNode = node.getChild(i);
                if(currentNode instanceof NameExpr){
                    Map<String, PromotedShape> value = new HashMap<>();
                    PromotedShape ps = new PromotedShape();
                    String name = node.getVarName();
                    ps.setB();
                    // first, should test whether it is a P
                    if(name.equals(iter)){
                        ps.setP(re, 0); //change later
                    }
                    else {
                        Shape psShape = getValueShape(name, valueMap);
                        if(psShape != null){
                            // do something
                            if(psShape.isScalar()) ps.setS();
                        }
                    }
                }
            }
        }

        if(isNestStmt) {
            ASTNode stmtList = node.getChild(1);
            int lenNew = stmtList.getNumChild();
            for(int i=0;i<lenNew;i++){

            }
        }
        return null;
    }

    private Shape getValueShape(String name, ValueFlowMap<AggrValue<BasicMatrixValue>> valueMap){
        if(valueMap.containsKey(name)){
            ValueSet<AggrValue<BasicMatrixValue>> currentValue =valueMap.get(name);
            if(currentValue.size() == 1){
                for(AggrValue<BasicMatrixValue> one : currentValue){
                    return ((BasicMatrixValue)one).getShape();  //get one
                }
            }
            return null;
        }
        return null;
    }

}
