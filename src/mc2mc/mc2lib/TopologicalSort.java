package mc2mc.mc2lib;

import ast.ASTNode;
import mc2mc.analysis.DepNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Topological sort
 * https://en.wikipedia.org/wiki/Topological_sorting
 */
public class TopologicalSort {
    Map<ASTNode, DepNode> localStmtMap;
    Map<ASTNode, Boolean> localFlagMap;
    List<ASTNode> outputList = null;
    public static boolean debug = true;

    public TopologicalSort(Map<ASTNode, DepNode> stmtMap, Map<ASTNode, Boolean> cycleMap){
        localStmtMap = stmtMap;
        localFlagMap = cycleMap;
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
}
