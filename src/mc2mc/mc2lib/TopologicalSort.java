package mc2mc.mc2lib;

import ast.ASTNode;
import ast.AssignStmt;
import ast.Expr;
import ast.NameExpr;
import mc2mc.analysis.DepNode;
import natlab.tame.tir.TIRCallStmt;

import java.util.*;

/**
 * Topological sort
 * https://en.wikipedia.org/wiki/Topological_sorting
 */
public class TopologicalSort {
    Map<ASTNode, DepNode> localStmtMap;
    Map<ASTNode, Boolean> localFlagMap;
    List<ASTNode> outputList = null;
    public static boolean debug = true;
    Set<Map<ASTNode, DepNode>> subGraph = null;
    Map<Map<ASTNode, DepNode>, Boolean> hasCyclicMap = null;
    Map<Map<ASTNode, DepNode>, Map<ASTNode, Boolean>> cyclicMap = null;

    public TopologicalSort(Map<ASTNode, DepNode> stmtMap, Map<ASTNode, Boolean> cycleMap){
        localStmtMap = stmtMap;
        localFlagMap = cycleMap;
        subGraph = new HashSet<>();
    }

    public TopologicalSort(Map<ASTNode, DepNode> stmtMap){
        localStmtMap = stmtMap;
        localFlagMap = new HashMap<>();
        subGraph     = new HashSet<>();
        hasCyclicMap = new HashMap<>();
        cyclicMap    = new HashMap<>();
        // initialize
        for(ASTNode a : localStmtMap.keySet()){
            localFlagMap.put(a, false);
        }
    }

    public List<ASTNode> sort(){
        init();
//        if(debug) {
//            PrintMessage.See("localFlagMap: from [TopologicalSort]");
//            for (ASTNode a : localFlagMap.keySet()) {
//                PrintMessage.See("[" + localFlagMap.get(a) + "] " + a.getPrettyPrinted().trim());
//            }
//        }
        while (mainSort(localStmtMap) > 0);
        if(debug) {
            printOutputList();
        }
        return outputList;
    }

    private void init(){
        outputList = new ArrayList<>();
    }

    private int mainSort(Map<ASTNode, DepNode> smallMap){
        Map<ASTNode, Integer> degreeIn = new HashMap<>();
        int n = 0;
        for(ASTNode a : smallMap.keySet()) {
            degreeIn.put(a, 0);
        }
        for(ASTNode a : smallMap.keySet()){
            if(localFlagMap.get(a)) continue;
            for(DepNode d : smallMap.get(a).getChild()){
                int c = degreeIn.get(d.getStmt()) + 1;
                degreeIn.put(d.getStmt(), c);
            }
        }
        for(ASTNode a : degreeIn.keySet()){
            if(localFlagMap.get(a)) continue;
            if(degreeIn.get(a) == 0){
                outputList.add(a);
                localFlagMap.put(a, true);
                n++;
            }
        }
        return n;
    }

    public void printOutputList(){
        PrintMessage.See("Print outputlist after topological sort: " + outputList.size());
        for(int i = 0;i < outputList.size();i++){
            PrintMessage.See("[" + i + "]" + outputList.get(i).getPrettyPrinted().trim());
        }
    }

    public Map<Map<ASTNode, DepNode>, Boolean> getIsCyclic(){
        return hasCyclicMap;
    }

    public Map<Map<ASTNode, DepNode>, Map<ASTNode, Boolean>> getCyclicMap(){
        return cyclicMap;
    }

    /**
     * Match pattern 1
     */
    public boolean matchIdiom1(Map<ASTNode, DepNode> group, Map<ASTNode, String> modifiedMap, List<String> remainedString){
        int depCount = 0;
        List<ASTNode> node2 = new ArrayList<>();
        Map<ASTNode, Boolean> groupMap = cyclicMap.get(group);
        for(ASTNode a : groupMap.keySet()){
            if(groupMap.get(a)) {
                if(depCount<2)
                    node2.add(a);
                depCount++;
            }
        }
        if(depCount==2){
            ASTNode n1 = node2.get(0);
            ASTNode n2 = node2.get(1);
            boolean kind1 = localStmtMap.get(n1).isKindAnti(0);
            boolean kind2 = localStmtMap.get(n2).isKindAnti(0);

            if(kind1 || kind2){
                if(kind2){
                    ASTNode temp = n1;
                    n1 = n2;
                    n2 = temp;
                }
                // do it
                boolean succeed = false;
                if(modifiedMap!=null)
                    localStmtMap.get(n1).safeRemoveChild(0);
                Set<String> lhs = ((AssignStmt)n1).getLValues();
                Set<String> lhs2= ((AssignStmt)n2).getLValues();
                if(lhs.size()==1 && lhs2.size() == 1) {
                    String lhsName = lhs.iterator().next();
                    String another = "";
                    // Todo: s=s+something --> need to check 's' is not after '-'
                    if (n2 instanceof TIRCallStmt && ((TIRCallStmt) n2).getFunctionName().getID().equals("plus")) {
                        for (Expr n : ((TIRCallStmt) n2).getArguments()) {
                            if (n instanceof NameExpr) {
                                if (((NameExpr) n).getName().getID().equals(lhsName)) {
                                    ; //donothing
                                } else {
                                    another = ((NameExpr) n).getName().getID();
                                }
                            }
                        }
                        if (!another.isEmpty()) {
                            if(modifiedMap!=null) {
                                String lhsName2 = lhs2.iterator().next();
                                modifiedMap.put(n2, lhsName2 + " = " + another + ";");
                                modifiedMap.put(n1, "%"+n1.getPrettyPrinted().trim()); //remove this line
                                String temp0 = "mc_sum0";
                                String temp1 = "mc_sum1";
                                remainedString.add(temp0 + " = " + ((AssignStmt) n1).getRHS().getPrettyPrinted().trim());
                                remainedString.add(temp1 + " = sum(" + temp0 + ");");
                                remainedString.add(lhsName + " = plus(" + lhsName + ", " + temp1 + ");");
                            }
                            succeed = true;
                            PrintMessage.See("Pattern 1 was found");
                        }
                    }
                }
                return succeed;
            }
        }
        return false;
    }

    public Map<ASTNode, Map<ASTNode, DepNode>> getGroups(){
        return getGroups(localStmtMap);
    }

    public Map<ASTNode, Map<ASTNode, DepNode>> getGroups(Map<ASTNode, DepNode> smallMap){
        Map<ASTNode, Integer> degreeIn = new HashMap<>();
        Map<ASTNode, Integer> subGraphCount = new HashMap<>();
        Map<Integer, Map<ASTNode, DepNode>> subGraphClass = new HashMap<>();
        Map<ASTNode, Map<ASTNode, DepNode>> node2Group = new HashMap<>();

        int cnt = 0;
        for(ASTNode a : smallMap.keySet()){
            subGraphCount.put(a, cnt++);
        }

        for(ASTNode a : smallMap.keySet()){
            for(DepNode d : smallMap.get(a).getChild()){
                ASTNode c = d.getStmt();
                int cntA = subGraphCount.get(a);
                int cntC = subGraphCount.get(c);
                if(cntA != cntC){
                    int v = Math.min(cntA, cntC);
                    if(v == cntA) {
                        subGraphCount.put(c, v);
                        updateParentNode(c,smallMap,subGraphCount,v);
                    }
                    else {
                        subGraphCount.put(a, v);
                        updateParentNode(a,smallMap,subGraphCount,v);
                    }
                }
            }
        }

        for(ASTNode a : subGraphCount.keySet()){
            int label = subGraphCount.get(a);
            if(!subGraphClass.containsKey(label)){
                Map<ASTNode, DepNode> one = new HashMap<>();
                one.put(a, smallMap.get(a));
                subGraphClass.put(label, one);
            }
            else {
                Map<ASTNode, DepNode> two = subGraphClass.get(label);
                two.put(a, smallMap.get(a));
                subGraphClass.put(label, two);
            }
        }

        for(int label : subGraphClass.keySet()){
            Map<ASTNode, DepNode> subGraph = subGraphClass.get(label);
            TarjanAlgo ta = new TarjanAlgo(subGraph);
            cyclicMap.put(subGraph, ta.solve());
            hasCyclicMap.put(subGraph, ta.hasCycle());
            for(ASTNode a : subGraphClass.get(label).keySet()){
                node2Group.put(a, subGraphClass.get(label));
            }
        }
        return node2Group;
    }

    void updateParentNode(ASTNode a, Map<ASTNode, DepNode> smallMap, Map<ASTNode, Integer> subGraphCount, int v){
        for(DepNode d : smallMap.get(a).getParent()){
            ASTNode p1 = d.getStmt();
            int numD = subGraphCount.get(p1);
            if(numD > v){
                subGraphCount.put(p1, v);
            }
        }
    }

    public Map<ASTNode, DepNode> getGroupMember(Map<ASTNode, DepNode> smallMap, ASTNode startNode){
        Map<ASTNode, DepNode> rtn = new HashMap<>();
        Set<ASTNode> localStack = new HashSet<>();
        localStack.add(startNode);
        while(localStack.size()>0){
            ASTNode cur = localStack.iterator().next();
            rtn.put(cur, smallMap.get(cur));
            for(DepNode d : smallMap.get(cur).getChild()){
                localStack.add(d.getStmt()); //add pointed nodes
            }
            localStack.remove(cur);
        }
        return rtn;
    }
}
