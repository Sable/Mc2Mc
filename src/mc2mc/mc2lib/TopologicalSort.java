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
    List<ASTNode> outputList = null;

    public TopologicalSort(Map<ASTNode, DepNode> stmtMap){
        localStmtMap = stmtMap;
    }

    public List<ASTNode> sort(){
        init();
        mainSort(localStmtMap);
        printOutputList();
        return outputList;
    }

    private void init(){
        outputList = new ArrayList<>();
    }

    private void mainSort(Map<ASTNode, DepNode> smallMap){
        Map<ASTNode, Integer> degreeIn = new HashMap<>();
        Map<ASTNode, DepNode> nextMap = new HashMap<>();
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
                outputList.add(a);
            }
            else nextMap.put(a, smallMap.get(a));
        }
        if(nextMap.size() > 0)
            mainSort(nextMap);
    }

    public void printOutputList(){
        PrintMessage.See("Print outputlist after topological sort:");
        for(int i = 0;i < outputList.size();i++){
            PrintMessage.See("["+i+"] " + outputList.get(i).getPrettyPrinted().trim());
        }
    }
}
