package mc2mc.analysis;

import ast.*;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.*;

import java.util.*;

/**
 *
 */
public class TirAnalysisDep {
    AnalysisEngine localEngine = null;
    Map<TIRNode, Map<String, Set<TIRNode>>> localUDMap = null;
    Map<TIRNode, HashMap<String, HashSet<TIRNode>>> localDUMap = null;
    ASTNode root = null;
    String iterator = "";
    boolean isLoop = false;
    boolean isCollect = true;
    Map<ASTNode, DepNode> stmtMap = null;
    Map<ASTNode, Boolean> stmtBool= null;

    public TirAnalysisDep(AnalysisEngine engine, ASTNode node){
        PrintMessage.delimiter();
        PrintMessage.See("Entering data dependence analysis");
        PrintMessage.delimiter();
        localEngine = engine;
        localUDMap = engine.getUDChainAnalysis().getChain();
        localDUMap = engine.getDUChainAnalysis().getChain();
        root = node;
        iterator = getIterator(root);
        isLoop = !iterator.isEmpty();
    }

    public void run(){
        stmtMap = new HashMap<>();
        stmtBool= new HashMap<>();
        astNodeTraversal(root);
        isCollect = false;
        astNodeTraversal(root);
        printStmtMap();
    }

    public void astNodeTraversal(ASTNode node) {
        if(node instanceof AssignStmt){
            if(localDUMap.get(node)!=null) { //e.g. i=1:n in for statement
                if (isCollect) {
                    stmtMap.put(node, new DepNode(node));
                    stmtBool.put(node, false);
                }
                else
                    processStmt((AssignStmt) node);
            }
        }
        else {
            int len = node.getNumChild();
            for (int i = 0; i < len; i++) {
                astNodeTraversal(node.getChild(i));
            }
        }
    }

    public void processStmt(AssignStmt node){
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

        for(Expr e : allWrite){
            if(e instanceof NameExpr){ // e.g. a
                String lhsName = ((NameExpr) e).getName().getID();
                if(useSet.get(lhsName)==null){
                    PrintMessage.See("[null node] " + node.getPrettyPrinted().trim());
                }
                else {
                    for (TIRNode t : useSet.get(lhsName)) {
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
            }
            else if(e instanceof ParameterizedExpr) { // e.g. a(i)
                String lhsName = e.getVarName();
                ArrayList<NumValue> defList =
                        processArrayIndexing((ParameterizedExpr)e, localUDMap.get(node), iterator);
                for(String var : useSet.keySet()){
                    if(var.equals(lhsName)){
                        for(TIRNode t : useSet.get(var)){
                            if(t instanceof TIRArrayGetStmt) {
                                if(((TIRArrayGetStmt) t).getArrayName().getID().equals(lhsName)){
                                    boolean before = stmtBool.get(t);
                                    PrintMessage.See("[related] " + before + " - " + ((ASTNode)t).getPrettyPrinted().trim());
                                    ArrayList<NumValue> useList =
                                            processArrayIndexing((ParameterizedExpr)((TIRArrayGetStmt)t).getRHS(), localUDMap.get(t), iterator);
                                    if(defList.size()==useList.size()){
                                        boolean isDep = true;
                                        for(int k = 0; k<defList.size();k+=2) {
                                            NumValue[] d2 = {defList.get(k), defList.get(k+1)};
                                            NumValue[] u2 = {useList.get(k), useList.get(k+1)};
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
            else if(op.equals("multiply") || op.equals("mtimes")){ // mtimes or multiply
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

    public int myGCD(int a, int b){
        if(b==0) return a;
        return myGCD(b, a%b);
    }

    public boolean gcdTest(int a1, int b1, int a2, int b2){
        PrintMessage.See(" (" + a1 + "," + b1 + "," + a2 + "," + b2 + ")");
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
