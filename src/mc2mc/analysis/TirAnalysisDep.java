package mc2mc.analysis;

import ast.*;
import mc2mc.mc2lib.CommonFunction;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.TIRForStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;
import natlab.tame.valueanalysis.components.constant.Constant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Created by wukefe on 6/1/16.
 */
public class TirAnalysisDep extends TIRAbstractNodeCaseHandler {

    public Map<TIRForStmt, Set<Expr>> exprRead = new HashMap<>();
    public Map<TIRForStmt, Set<Expr>> exprWrite= new HashMap<>();
    public Map<TIRNode, Map<String, Set<TIRNode>>> fUDMap = null;

    public TirAnalysisDep(AnalysisEngine engine){
        fUDMap = engine.getUDChainAnalysis().getChain(); //UDChain
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
        if(isFor && numberFor == 1){
            collectRW((TIRForStmt)node);
        }
        return numberFor;
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
}
