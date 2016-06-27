package mc2mc.mc2lib;

import ast.ASTNode;
import mc2mc.analysis.DepNode;

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

    public TopologicalSort(Map<ASTNode, DepNode> stmtMap, Map<ASTNode, Boolean> cycleMap){
        localStmtMap = stmtMap;
        localFlagMap = cycleMap;
        subGraph = new HashSet<>();
    }

    public TopologicalSort(Map<ASTNode, DepNode> stmtMap){
        localStmtMap = stmtMap;
        subGraph = new HashSet<>();
    }

    public List<ASTNode> sort(){
        init();
        if(debug) {
            PrintMessage.See("localFlagMap: from [TopologicalSort]");
            for (ASTNode a : localFlagMap.keySet()) {
                PrintMessage.See("[" + localFlagMap.get(a) + "] " + a.getPrettyPrinted().trim());
            }
        }
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

    public Set<Map<ASTNode, DepNode>> getGroups(){
        getGroups(localStmtMap);
        Map<ASTNode, Map<ASTNode, DepNode>> node2Group = new HashMap<>();
        for(Map<ASTNode, DepNode> d : subGraph){
            for(ASTNode a : d.keySet()){
                node2Group.put(a, d); // reverse point
            }
        }
        return subGraph; //return node2Group ??
    }

    public void getGroups(Map<ASTNode, DepNode> smallMap){
        Map<ASTNode, Integer> degreeIn = new HashMap<>();
        int n = 0;
        for(ASTNode a : smallMap.keySet()) {
            degreeIn.put(a, 0);
        }
        for(ASTNode a : smallMap.keySet()){
            for(DepNode d : smallMap.get(a).getChild()){
                int c = degreeIn.get(d.getStmt()) + 1;
                degreeIn.put(d.getStmt(), c);
            }
        }
        for(ASTNode a : degreeIn.keySet()){
            if(degreeIn.get(a) == 0){
                // groups
                subGraph.add(getGroupMember(smallMap, a));
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
