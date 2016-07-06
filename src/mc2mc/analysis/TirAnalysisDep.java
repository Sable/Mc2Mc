package mc2mc.analysis;

import ast.*;
import mc2mc.mc2lib.PrintMessage;
import mc2mc.mc2lib.TarjanAlgo;
import mc2mc.mc2lib.TopologicalSort;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.*;

import java.util.*;
import java.util.List;

/**
 *
 */
public class TirAnalysisDep {
    AnalysisEngine localEngine = null;
    Map<TIRNode, Map<String, Set<TIRNode>>> localUDMap = null;
    Map<TIRNode, HashMap<String, HashSet<TIRNode>>> localDUMap = null;
    TIRForStmt root = null;
    String iterator = "";
    boolean isLoop = false;
    boolean isCollect = true;
    Map<ASTNode, DepNode> stmtMap = null;
    Map<ASTNode, Boolean> stmtBool= null;
    public static boolean debug = false;

    public TirAnalysisDep(AnalysisEngine engine, ASTNode node){
        PrintMessage.delimiter();
        PrintMessage.See("Entering data dependence analysis");
        PrintMessage.delimiter();
        localEngine = engine;
        localUDMap = engine.getUDChainAnalysis().getChain();
        localDUMap = engine.getDUChainAnalysis().getChain();
        root = (TIRForStmt)node;
        iterator = getIterator(root);
        isLoop = !iterator.isEmpty();
    }

    public void run(){
        stmtMap = new HashMap<>();
        stmtBool= new HashMap<>();
        astNodeTraversal(root.getStatements());
        isCollect = false;
        astNodeTraversal(root.getStatements());
        //printStmtMap();
        // solve the graph by the Tarjan's algorithm
        if(debug) {
            TarjanAlgo tAlgo = new TarjanAlgo(stmtMap);
            Map<ASTNode, Boolean> cycleMap = tAlgo.solve();

            TopologicalSort ts = new TopologicalSort(stmtMap, cycleMap);
            List<ASTNode> outputList = ts.sort();

            if (outputList.size() != stmtMap.size()) {
                PrintMessage.See("At least a cycle is found in dependence graph.");
            }
        }
    }

    public Map<ASTNode, DepNode> getDepGraph(){
        return stmtMap;
    }

    public void astNodeTraversal(ASTNode node) {
        if(node instanceof AssignStmt){
//            if(localDUMap.get(node)!=null) { //e.g. i=1:n in for statement
                if (isCollect) {
                    if(((AssignStmt) node).getLHS().getPrettyPrinted().trim().equals("[mc_t253]")){
                        int xx = 10;
                    }
                    stmtMap.put(node, new DepNode(node));
                    stmtBool.put(node, false);
                }
                else
                    processStmt((AssignStmt) node);
//            }
        }
        else {
            int len = node.getNumChild();
            for (int i = 0; i < len; i++) {
                astNodeTraversal(node.getChild(i));
            }
        }
    }

    public void processStmt(AssignStmt node){
        if(debug)
            PrintMessage.See("processing: " + node.getPrettyPrinted().trim());
        findFlowDep(node);
//        Map<String, Set<TIRNode>> useSet = localUDMap.get(node);
//        HashMap<String, HashSet<TIRNode>> useSet = localDUMap.get(node);
//        if(useSet==null){
//            PrintMessage.See("\t[null]");
//        }
//        else {
//            for (String var : useSet.keySet()) {
//                if(isLoop && var.equals(iterator)) continue;
//                PrintMessage.See("\t[def] " + var);
//                for (TIRNode t : useSet.get(var)) {
//                    PrintMessage.See("\t[use] " + ((ASTNode) t).getPrettyPrinted().trim() + " //" + stmtMap.containsKey(t));
//                }
//            }
//        }
    }

    private void findFlowDep(ASTNode node){
        Set<Expr> allWrite = collectWrite(((AssignStmt)node).getLHS());
        HashMap<String, HashSet<TIRNode>> useSet = localDUMap.get(node);
//        if(useSet==null) return; //e.g. i=1:n in for statement
        stmtBool.put(node, true);
        if(useSet==null) { // no use at all
            int kind = 3; //output
            DepNode t = stmtMap.get(node);
            t.setChild(t, kind);
            return;
        }

        for(Expr e : allWrite){
            if(e instanceof NameExpr || e instanceof MatrixExpr){ // e.g. a
                Set<String> writeString = ((AssignStmt)node).getLValues();
                for(String lhsName : writeString) {
                    if (lhsName.equals("q")) {
                        int xx = 10;
                    }
                    if (useSet.get(lhsName) == null) {
                        PrintMessage.See("[null node] " + node.getPrettyPrinted().trim());
                    } else {
                        Set<TIRNode> uses = getOneUses(useSet, lhsName);
                        if (uses != null) {
                            for (TIRNode t : uses) {
                                DepNode child = stmtMap.get(t);
                                int kind = !stmtBool.get(t) ? 1 : t.equals(node) ? 0 : 2;
                                stmtMap.get(node).setChild(child, kind); //forward or anti
                            /*
                            * exception
                            * a = ...
                            * . = a(i);
                            */
                            }
                        }
                        else{
                            Set<TIRNode> blockUses = getBlockUses(useSet, lhsName);
                            if(blockUses.size()==0){ //output
                                int kind = 3;
                                DepNode child = stmtMap.get(node);
                                stmtMap.get(node).setChild(child, kind);
                            }
                            else{
                                for(TIRNode t : blockUses) { //same as above?
                                    DepNode child = stmtMap.get(t);
                                    int kind = !stmtBool.get(t) ? 1 : t.equals(node) ? 0 : 2;
                                    stmtMap.get(node).setChild(child, kind);
                                }
                            }
                        }
                    }
                }
            }
            else if(e instanceof ParameterizedExpr) { // e.g. a(i)
                String lhsName = e.getVarName();
                ArrayList<NumValue> defList =
                        processArrayIndexing((ParameterizedExpr)e, localUDMap.get(node), iterator);
                for(String var : useSet.keySet()){
                    if(var.equals(lhsName)){
                        for(TIRNode t : useSet.get(var)){
                            if(t instanceof TIRArrayGetStmt) { // e.g. a(i) = t0;
                                if(((TIRArrayGetStmt) t).getArrayName().getID().equals(lhsName)){
                                    if(!stmtBool.containsKey(t))
                                        continue; // if not in the current loop, skip
                                    boolean before = stmtBool.get(t);
                                    PrintMessage.See("[related] " + before + " - " + ((ASTNode)t).getPrettyPrinted().trim());
                                    ArrayList<NumValue> useList =
                                            processArrayIndexing((ParameterizedExpr)((TIRArrayGetStmt)t).getRHS(), localUDMap.get(t), iterator);
                                    if(defList.size()==useList.size()){
                                        boolean isDep = true;
                                        NumValue[] nv1 = null;
                                        NumValue[] nv2 = null;
                                        for(int k = 0; k<defList.size();k+=2) {
                                            NumValue[] d2 = {defList.get(k), defList.get(k+1)};
                                            NumValue[] u2 = {useList.get(k), useList.get(k+1)};
                                            // maybe not that simple
                                            nv1 = d2;
                                            nv2 = u2;
                                            if(d2[0].isN() && d2[1].isN() && u2[0].isN() && u2[1].isN()){
                                                // do GCD test
                                                // if safe --> independent
                                                // else    --> dependent
                                                if(!gcdTest(d2[0].getN(),d2[1].getN(),u2[0].getN(),u2[1].getN())){
                                                    isDep = false; break;
                                                }
                                            }
                                            else if(d2[0].isS() && u2[0].isS()){
                                                if(!d2[0].getS().equals(u2[0].getS())){
                                                    isDep = false; break; //independent
                                                }
                                            }
                                        }
                                        if(isDep){
                                            // todo: find out what kind of relationship
                                            int status = 0; // 0: no idea; 1: greater; 2: less; 3: equal
                                            if(nv1[0].isN() && nv2[0].isN()){
                                                int na = nv1[0].getN() - nv2[0].getN();
                                                if(nv1[1].isN() && nv2[1].isN()){
                                                    int nb = nv1[1].getN() - nv2[1].getN();
                                                    status = determineStatus(na, nb);
                                                }
                                                else if(nv1[1].isS() && nv2[1].isN()){
                                                    if(nv1[1].getS().equals(nv2[1].getS())) {
                                                        status = determineStatus(na, 0);
                                                    }
                                                }
                                            }
                                            else if(nv1[0].isS() && nv2[0].isS()){
                                                if(nv1[0].getS().equals(nv2[0].getS())){
                                                    if(nv1[1].isN() && nv2[1].isN()){
                                                        int nb = nv1[1].getN() - nv2[1].getN();
                                                        status = determineStatus(0, nb);
                                                    }
                                                    else if(nv1[1].isS() && nv2[1].isN()){
                                                        if(nv1[1].getS().equals(nv2[1].getS())) {
                                                            status = determineStatus(0, 0);
                                                        }
                                                    }
                                                }
                                            }
                                            DepNode depDef= stmtMap.get(node);
                                            DepNode depUse = stmtMap.get(t);
                                            int kind = 0;
                                            if(status == 0){
                                                kind = 1; //flow
                                                depDef.setChild(depUse,kind); //cyclic
                                                depUse.setChild(depDef,kind);
                                            }
                                            else if(status == 3){
                                                if(before){
                                                    kind = 2;
                                                    depUse.setChild(depDef, kind); //anti
                                                }
                                                else{
                                                    kind = 1;
                                                    depDef.setChild(depUse, kind); //forward
                                                }
                                            }
                                            else if(status == 1){
                                                kind = 1;
                                                depDef.setChild(depUse, kind); //fowrard
                                            }
                                            else if(status == 2){
                                                kind = 2;
                                                depUse.setChild(depDef, kind); //anti
                                            }
                                        }
                                        PrintMessage.See("isDep " + isDep);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int determineStatus(int na, int nb){
        int status = 0;
        if(na == 0) {
            status = nb == 0 ? 3 : nb > 0 ? 1 : 2;
        }
        else if(na > 0) {
            status = nb >= 0 ? 1 : 0;
        }
        else {
            status = nb <= 0 ? 2 : 0;
        }
        return status;
    }

    private Set<Expr> collectWrite(Expr lhs){
        Set<Expr> rtn = new HashSet<>();
        if(lhs instanceof MatrixExpr){
            rtn.addAll(lhs.getNameExpressions());
        }
        else {
            rtn.add(lhs); // maybe more cases
        }
        return rtn;
    }

    private String getIterator(ASTNode node){
        String rtn = "";
        if(node instanceof TIRForStmt){
            rtn = (((TIRForStmt) node).getLoopVarName()).getID();
        }
        return rtn;
    }

    public void printStmtMap(){
        PrintMessage.See("print stmtMap:");
        for(ASTNode a : stmtMap.keySet()){
//            PrintMessage.See(a.getPrettyPrinted().trim());
            PrintMessage.See(stmtMap.get(a).getPrettyPrinted());
        }
    }

    // process a1*i+b1 == a2*i+b2
    public ArrayList<NumValue> processArrayIndexing(ParameterizedExpr node, Map<String, Set<TIRNode>> defSet, String iter){
        ArrayList<NumValue> rtn = new ArrayList<>();
        for(Expr x : node.getArgList()){
            NumValue[] nv = processArrayPar(x, defSet, iter);
            rtn.add(nv[0]);
            rtn.add(nv[1]); // add two items
        }
        return rtn;
    }

    public NumValue[] processArrayPar(Expr node, Map<String, Set<TIRNode>> defSet, String iter){
        NumValue[] nv = new NumValue[2];
        nv[0] = new NumValue();
        nv[1] = new NumValue();
        if(node instanceof NameExpr){
            String indexName = ((NameExpr) node).getName().getID();
            if(indexName.equals(iter)){
                nv[0].setN(1);
                nv[1].setN(0);
            }
            else {
                ASTNode one = getOneNode(defSet, indexName);
                if(one != null) {
                    nv = processParDef(one, iter);
                }
                else {
                    nv[0].setS(indexName);
                }
            }
        }
        else if(node instanceof ColonExpr){
            nv[0].setS(node.getPrettyPrinted().trim());
        }
        return nv;
    }

    public NumValue[] processParDef(ASTNode prev, String iter){
        NumValue[] nv = new NumValue[2];
        nv[0] = new NumValue();
        nv[1] = new NumValue();
        if(prev instanceof TIRAssignLiteralStmt){
            LiteralExpr rhs = ((TIRAssignLiteralStmt) prev).getRHS();
            if(rhs instanceof IntLiteralExpr){
                nv[0].setN(0);
                nv[1].setN(((IntLiteralExpr) rhs).getValue().getValue().intValue());
            }
            else nv[0].setU();
        }
        else if(prev instanceof TIRCallStmt){
            Map<String, Set<TIRNode>> defSet = localUDMap.get(prev);
            String op = ((TIRCallStmt) prev).getFunctionName().getID();
            if(op.equals("plus")){
                String arg1 = ((TIRCallStmt) prev).getArguments().getName(0).getID();
                String arg2 = ((TIRCallStmt) prev).getArguments().getName(1).getID();
                if(arg1.equals(iter)){
                    nv[0].setN(1);
                    saveValue(defSet, arg2, iter, nv);
                }
                else if(arg2.equals(iter)){
                    nv[0].setN(1);
                    saveValue(defSet, arg1, iter, nv);
                }
                else {
                    saveValue(defSet, arg1, iter, nv);
                    saveValue(defSet, arg2, iter, nv);
                }
            }
            else if(op.equals("minus")){
                String arg1 = ((TIRCallStmt) prev).getArguments().getName(0).getID();
                String arg2 = ((TIRCallStmt) prev).getArguments().getName(1).getID();
                if(arg1.equals(iter)){
                    nv[0].setN(1);
                    saveValue(defSet, arg2, iter, nv);
                    nv[1].setN(-nv[1].getN()); // set negative
                }
                else if(arg2.equals(iter)){
                    nv[0].setN(-1);
                    saveValue(defSet, arg1, iter, nv);
                }
                else {
                    saveValue(defSet, arg1, iter, nv);
                    saveValue(defSet, arg2, iter, nv);
                }
            }
            else if(op.equals("times") || op.equals("mtimes")){ // times or mtimes
                String arg1 = ((TIRCallStmt) prev).getArguments().getName(0).getID();
                String arg2 = ((TIRCallStmt) prev).getArguments().getName(1).getID();
                if(arg1.equals(iter)){
                    nv[0].setN(1);
                    saveValueMul(defSet, arg2, iter, nv);
                }
                else if(arg2.equals(iter)){
                    nv[0].setN(1);
                    saveValueMul(defSet, arg1, iter, nv);
                }
                else {
                    saveValueMul(defSet, arg1, iter, nv);
                    saveValueMul(defSet, arg2, iter, nv);
                }
            }
        }
        else {
            nv[0].setU();
        }
        return nv;
    }

    private void saveValue(Map<String, Set<TIRNode>> defSet, String var, String iter, NumValue[] nv){
        if(!nv[0].isN() || !nv[1].isN()) return ; //
        ASTNode one = getOneNode(defSet, var);
        if(one!=null){
            NumValue[] n2 = processParDef(one, iter);
            if(n2[0].isN() && n2[1].isN()){
                nv[0].setN(nv[0].getN() + n2[0].getN());
                nv[1].setN(nv[1].getN() + n2[1].getN());
            }
        }
        else {
            nv[0].setS(var);
        }
    }

    private void saveValueMul(Map<String, Set<TIRNode>> defSet, String var, String iter, NumValue[] nv){
        if(!nv[0].isN() || !nv[1].isN()) return ; //
        ASTNode one = getOneNode(defSet, var);
        if(one!=null){
            NumValue[] n2 = processParDef(one, iter);
            if(n2[0].isN() && n2[1].isN() && n2[0].getN() == 0){
                nv[0].setN(nv[0].getN() * n2[1].getN());
                nv[1].setN(nv[1].getN() * n2[1].getN());
            }
        }
        else {
            nv[0].setS(var);
        }
    }

    /**
     * Get one def from the given var,
     *   if the one is defined only once whithin block,
     *     return the stmt defines the var.
     *   otw, return null.
     */
    private ASTNode getOneNode(Map<String, Set<TIRNode>> defSet, String var){
        Set<TIRNode> defs = defSet.get(var);
        ASTNode one = null;
        if(defs.size() == 1){
            for(TIRNode t : defs)
                one = (ASTNode) t;
            if(!stmtMap.containsKey(one))
                one = null;
        }
        return one;
    }

    /**
     * Get the uses of the given var,
     *   if the uses are within block,
     *     return the stmt which uses the var.
     *   otw, return null.
     */
    private Set<TIRNode> getOneUses(HashMap<String, HashSet<TIRNode>> useSet, String var){
        HashSet<TIRNode> uses = useSet.get(var);
        boolean isAll = true;
        for(TIRNode u : uses){
            if(!stmtMap.containsKey(u)){
                return null;
            }
        }
        return uses;
    }

    private Set<TIRNode> getBlockUses(HashMap<String, HashSet<TIRNode>> useSet, String var){
        HashSet<TIRNode> uses = useSet.get(var);
        Set<TIRNode> newUses = new HashSet<>();
        boolean isAll = true;
        for(TIRNode u : uses){
            if(stmtMap.containsKey(u)){
                newUses.add(u);
            }
        }
        return newUses;
    }

    public int myGCD(int a, int b){
        if(b==0) return a;
        return myGCD(b, a%b);
    }

    public boolean gcdTest(int a1, int b1, int a2, int b2){
        PrintMessage.See(" (" + a1 + "," + b1 + "," + a2 + "," + b2 + ")");
        if(a1 < 1 || a2 < 1) return true; //avoid it
        return ((b2-b1)%(myGCD(a1,a2))==0);
    }

}


class NumValue{
    int valueKind;
    int number;
    String symbol;

    public NumValue(){
        valueKind = 1;
        number = 0;
        symbol = "";
    }

    public boolean isN(){
        return valueKind == 1;
    }

    public boolean isS(){
        return valueKind == 2;
    }

    public int getN(){
        return number;
    }

    public String getS(){
        return symbol;
    }

    public void setN(int n){
        number = n;
        valueKind = 1;
    }

    public void setS(String s){
        symbol = s;
        valueKind = 2;
    }

    public void setU(){
        valueKind = 3;
    }
}
