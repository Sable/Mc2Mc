package mc2mc.analysis;


import ast.*;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tamerplus.analysis.ReachingDefinitions;
import natlab.tame.tir.TIRAssignLiteralStmt;
import natlab.tame.tir.TIRCallStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractSimpleStructuralForwardAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//public class TirAnalysisSubExpr extends TIRAbstractSimpleStructuralForwardAnalysis<HashMap<String, Set<TIRNode>>> {
public class TirAnalysisSubExpr extends TIRAbstractSimpleStructuralForwardAnalysis<Map<TIRNode, Integer>> {

    ReachingDefinitions localReachingDef = null;
    Map<String, Set<TIRNode>> currentReachingDef = null;
    int labelID = 0;

    public TirAnalysisSubExpr(ASTNode tree, AnalysisEngine engine) {
        super(tree);
        localReachingDef = engine.getReachingDefinitionsAnalysis();
        labelID = 0;
    }


//    @Override
//    public HashMap<String, Set<TIRNode>> merge(HashMap<String, Set<TIRNode>> map1, HashMap<String, Set<TIRNode>> map2) {
//        HashMap<String, Set<TIRNode>> union = new HashMap<>();
//        for(String s1 : map1.keySet()){
//            if(map2.containsKey(s1)){
//                union.put(s1, Sets.union(map1.get(s1),map2.get(s1)));
//            }
//            else union.put(s1, map1.get(s1));
//        }
//        return union;
//    }
//
//    @Override
//    public HashMap<String, Set<TIRNode>> copy(HashMap<String, Set<TIRNode>> map1) {
//        HashMap<String, Set<TIRNode>> out = new HashMap<>();
//        for(String s : map1.keySet()){
//            out.put(s, new HashSet<>(map1.get(s)));
//        }
//        return out;
//    }
//    @Override
//    public HashMap<String, Set<TIRNode>> newInitialFlow() {
//        return new HashMap<>();
//    }

    @Override
    public Map<TIRNode, Integer> merge(Map<TIRNode, Integer> map1, Map<TIRNode, Integer> map2) {
        Map<TIRNode, Integer> union = new HashMap<>();
        for(TIRNode t1:map1.keySet()){
            int v1 = map1.get(t1);
            if(map2.containsKey(t1) && map2.get(t1) == v1){
                union.put(t1, v1);
            }
        }
        return union;
    }

    @Override
    public Map<TIRNode, Integer> copy(Map<TIRNode, Integer> map1) {
        Map<TIRNode, Integer> out = new HashMap<>();
        for(TIRNode t : map1.keySet()){
            out.put(t, map1.get(t));
        }
        return out;
    }


    @Override
    public Map<TIRNode, Integer> newInitialFlow() {
        return new HashMap<>();
    }


//    @Override
//    public void caseTIRAbstractAssignStmt(TIRAbstractAssignStmt node){
//        PrintMessage.See("entering" + node.dumpString());
//        if(node instanceof TIRCallStmt) {
//            TIRCallStmt s = (TIRCallStmt) node;
//            PrintMessage.See(s.getPrettyPrinted());
//        }
//    }

    @Override
    public void caseTIRCallStmt(TIRCallStmt node){
        PrintMessage.See("TIRCallStmt");
        PrintMessage.See(node.getPrettyPrinted());
        currentOutSet = copy(currentInSet);
        String stringrhs = node.getRHS().getPrettyPrinted();
        String stringlhs = node.getLHS().getPrettyPrinted(); // assume only one var on lhs
        currentReachingDef = localReachingDef.getReachingDefinitionsForNode(node);

        int groupID = matchExpr(currentOutSet, node); // find group ID

        currentOutSet.put(node, groupID);
        associateInSet(node, getCurrentInSet());
        associateOutSet(node, getCurrentOutSet());
    }

    // match AssignLiteralStmt

    private int matchExpr(Map<TIRNode, Integer> outSet, TIRCallStmt node){
        int id = 0;
        for(TIRNode t : outSet.keySet()){
            if(t instanceof TIRCallStmt){
                if(lookUpNode(t,node)){
                    PrintMessage.See("found!!!!!!!!");
                    id = outSet.get(t);
                    if(id == 0){
                        id = ++labelID;
                        outSet.put(t,    id); //update oldID
//                        outSet.put(node, labelID); //insert a new ID
                    }
                }
                else {
                    PrintMessage.See("nooot");
                }
            }
        }
//        PrintMessage.printMap(outSet);
        return id;
    }


//    @Override
//    public void caseTIRAbstractAssignToListStmt(TIRAbstractAssignToListStmt node){
//        currentOutSet = copy(currentInSet);
//        printHashMapSubExpr(currentInSet);
//        PrintMessage.See("TIRAbstractAssignToListStmt");
//        PrintMessage.See(node.getPrettyPrinted());
//        PrintMessage.See("Right: " + node.getRHS().getPrettyPrinted());
//    }

    private boolean isTmp(String x){
        return x.startsWith("mc_t");
    }

    /*
      true: match
     false: not match
     */

    public boolean lookUpNode(TIRNode n1, TIRNode n2){
        if(!n1.getClass().equals(n2.getClass())) return false;
        if(!(n1 instanceof TIRCallStmt || n2 instanceof TIRAssignLiteralStmt)) return false;
        if(currentInSet.containsKey(n1) && currentInSet.containsKey(n2)){
            if(currentInSet.get(n1)>0 && currentInSet.get(n1) == currentInSet.get(n2)) return true;
        }
        if(n1 instanceof TIRCallStmt) {
            Expr tRHS = ((TIRCallStmt) n1).getRHS();
            Expr nRHS = ((TIRCallStmt) n2).getRHS();
            boolean flag = false;
            if(tRHS.getClass().equals(nRHS.getClass())){
                if(tRHS instanceof ast.ParameterizedExpr){
                    if(tRHS.getVarName().equals(nRHS.getVarName())){
                        ParameterizedExpr tPar = (ParameterizedExpr) tRHS;
                        ParameterizedExpr nPar = (ParameterizedExpr) nRHS;
                        if(tPar.getNumArg() == nPar.getNumArg()){
                            int num = tPar.getNumArg();
                            if(num == 1){
                                flag = lookUpExpr(tPar.getArg(0), nPar.getArg(0));
                            }
                            else if(num==2){
                                flag = lookUpExpr(tPar.getArg(0), nPar.getArg(0))
                                        && lookUpExpr(tPar.getArg(1), nPar.getArg(1));
                                // special case for plus and multiply
                                if(!flag && (tRHS.getVarName().equals("plus") || tRHS.getVarName().equals("mtimes"))){
                                    flag = lookUpExpr(tPar.getArg(1), nPar.getArg(0))
                                            && lookUpExpr(tPar.getArg(0), nPar.getArg(1));
                                }
                            }
                        }
                    }
                }
            }
            return flag;
        }
        else if(n1 instanceof TIRAssignLiteralStmt){
            LiteralExpr tRHS = ((TIRAssignLiteralStmt) n1).getRHS();
            LiteralExpr nRHS = ((TIRAssignLiteralStmt) n2).getRHS();
            return lookUpExpr(tRHS,nRHS);
        }
        return false;
    }

    public boolean lookUpExpr(Expr e1, Expr e2){
        boolean rtn = false;
        if(e1 instanceof NameExpr){
            rtn = lookUpString(((NameExpr) e1).getName().getID(),(((NameExpr)e2).getName().getID()));
        }
        else {
            rtn = lookUpString(e1.getPrettyPrinted().trim(), e2.getPrettyPrinted().trim());
        }
        return rtn;
    }


    public boolean lookUpString(String a1, String b1){
        boolean rtn = false;
        if(isTmp(a1) && isTmp(b1)){
            Set<TIRNode> s1 = currentReachingDef.get(a1);
            Set<TIRNode> s2 = currentReachingDef.get(b1);
            TIRNode n1 = null;
            TIRNode n2 = null;
            if(s1!=null && s2!=null && s1.size()==1 && s2.size() == 1) {
                for(TIRNode t:s1) n1 = t;
                for(TIRNode t:s2) n2 = t;
                rtn = lookUpNode(n1, n2);
            }
        }
        else if(!isTmp(a1) && !isTmp(b1)){
            rtn = a1.equals(b1);
        }
        return rtn;
    }

    public void getFinalInfo(){
        Map<TIRNode, Integer> inSet = currentInSet;
        for(TIRNode t : inSet.keySet()){
            int v = inSet.get(t);
            if(v!=0){
                PrintMessage.See(t.toString() + " : " + v);
            }
        }
        PrintMessage.See("" + labelID);
        PrintMessage.See("" + currentInSet.size());
    }


    private Expr exprUnion(Expr left, Expr right){
        return null;
    }

    public void printHashMapSubExpr(HashMap<String, Set<TIRNode>> inputset){
        for(Map.Entry<String, Set<TIRNode>> entry : inputset.entrySet()){
            PrintMessage.See("key: " + entry.getKey());
            PrintMessage.See("value: ");
            for(TIRNode t : entry.getValue()){
                PrintMessage.See(((ASTNode)t).getPrettyPrinted());
            }
        }
    }
}
