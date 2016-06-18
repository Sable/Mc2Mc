package mc2mc.mc2lib;

import ast.ASTNode;
import mc2mc.analysis.DepNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Tarjan's strongly connected components algorithm
 * https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
 *
 * Input with a graph
 */
public class TarjanAlgo {
    Map<ASTNode, DepNode> localStmtMap = null;
    Map<ASTNode, Integer> number = null;
    Map<ASTNode, Integer> lowerLink = null;
    Stack<ASTNode> localStack = null;
    int label = 0;
    boolean hasCycle = false;

    public TarjanAlgo(Map<ASTNode, DepNode> stmtMap){
        localStmtMap = stmtMap;
    }

    public boolean solve(){
        mainAlgo();
        return hasCycle;
    }

    private void init(){
        number = new HashMap<>();
        lowerLink = new HashMap<>();
        for(ASTNode a : localStmtMap.keySet()){
            number.put(a, 0); //set 0;
            lowerLink.put(a, 0);
        }
        label = 0;
        localStack = new Stack<>();
        hasCycle = false;
    }

    private void mainAlgo(){
        init();
        for(ASTNode a : localStmtMap.keySet()){
            if(number.get(a)==0){
                findCycle(a);
            }
        }
    }

    private void findCycle(ASTNode a){
        label++;
        number.put(a, label);
        lowerLink.put(a, label);
        localStack.add(a);
        for(DepNode d : localStmtMap.get(a).getChild()){
            ASTNode dStmt = d.getStmt();
            if(number.get(dStmt)==0){
                findCycle(dStmt);
                lowerLink.put(a, Math.min(lowerLink.get(a), lowerLink.get(dStmt)));
            }
            else if(number.get(dStmt) < number.get(a)){
                if(localStack.contains(dStmt)){
                    lowerLink.put(a, Math.min(lowerLink.get(a), lowerLink.get(dStmt)));
                }
            }
        }
        if(lowerLink.get(a) == number.get(a)){
            int cnt = 0;
            ASTNode w;
//            PrintMessage.See("[before] size = " + localStack.size());
//            do{
//                w = localStack.pop();
//                PrintMessage.See(": " + w.getPrettyPrinted().trim());
//            }while (!w.equals(a));
            while(!localStack.pop().equals(a)) cnt++; // add pop() to current strongly connected component
//            PrintMessage.See("[after] size = " + localStack.size());
            if(cnt>0) hasCycle = true;
        }
    }

}
