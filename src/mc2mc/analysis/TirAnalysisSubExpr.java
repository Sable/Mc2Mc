package mc2mc.analysis;


import ast.ASTNode;
import ast.Expr;
import com.google.common.collect.Sets;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tir.TIRCallStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractSimpleStructuralForwardAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TirAnalysisSubExpr extends TIRAbstractSimpleStructuralForwardAnalysis<HashMap<String, Set<TIRNode>>> {

    public TirAnalysisSubExpr(ASTNode tree) {
        super(tree);
    }

    @Override
    public HashMap<String, Set<TIRNode>> merge(HashMap<String, Set<TIRNode>> map1, HashMap<String, Set<TIRNode>> map2) {
        HashMap<String, Set<TIRNode>> union = new HashMap<>();
        for(String s1 : map1.keySet()){
            if(map2.containsKey(s1)){
                union.put(s1, Sets.union(map1.get(s1),map2.get(s1)));
            }
            else union.put(s1, map1.get(s1));
        }
        return union;
    }

    @Override
    public HashMap<String, Set<TIRNode>> copy(HashMap<String, Set<TIRNode>> map1) {
        HashMap<String, Set<TIRNode>> out = new HashMap<>();
        for(String s : map1.keySet()){
            out.put(s, new HashSet<>(map1.get(s)));
        }
        return out;
    }

    @Override
    public HashMap<String, Set<TIRNode>> newInitialFlow() {
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
        currentOutSet = copy(currentInSet);
        String stringrhs = node.getRHS().getPrettyPrinted();
        String stringlhs = node.getLHS().getPrettyPrinted(); // assume only one var on lhs
        Set<TIRNode> myset = new HashSet<>();
        myset.add(node);
        currentOutSet.put(stringrhs, myset);
        PrintMessage.See(node.getPrettyPrinted());
        associateInSet(node, getCurrentInSet());
        associateOutSet(node, getCurrentOutSet());
    }

//    @Override
//    public void caseTIRAbstractAssignToListStmt(TIRAbstractAssignToListStmt node){
//        currentOutSet = copy(currentInSet);
//        printHashMapSubExpr(currentInSet);
//        PrintMessage.See("TIRAbstractAssignToListStmt");
//        PrintMessage.See(node.getPrettyPrinted());
//        PrintMessage.See("Right: " + node.getRHS().getPrettyPrinted());
//    }


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
