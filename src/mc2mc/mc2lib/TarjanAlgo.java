package mc2mc.mc2lib;

import ast.ASTNode;
import mc2mc.analysis.DepNode;

import java.util.*;

/**
 * Tarjan's strongly connected components algorithm
 * https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
 *
 * Input with a graph
 */
public class TarjanAlgo {
    Map<ASTNode, DepNode> localStmtMap = null;
    Map<ASTNode, Boolean> cycleMap     = null;
    Map<ASTNode, Integer> number       = null;
    Map<ASTNode, Integer> lowerLink    = null;
    Stack<ASTNode> localStack = null;
    int label = 0;
    public static boolean debug = false;

    public TarjanAlgo(Map<ASTNode, DepNode> stmtMap){
        localStmtMap = stmtMap;
    }

    public boolean hasCycle(){
        for(ASTNode a : cycleMap.keySet()){
            if(cycleMap.get(a))
                return true;
        }
        return false;
    }

    public Map<ASTNode, Boolean> solve(){
        mainAlgo();
        return cycleMap;
    }

    private void init(){
        cycleMap = new HashMap<>();
        number = new HashMap<>();
        lowerLink = new HashMap<>();
        for(ASTNode a : localStmtMap.keySet()){
            number.put(a, 0); //set 0;
            lowerLink.put(a, 0);
            cycleMap.put(a, false);
        }
        label = 0;
        localStack = new Stack<>();
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
            List<ASTNode> queue = new LinkedList<>();
            ASTNode w;
            if(debug)
                PrintMessage.See("[before] size = " + localStack.size());
            do{
                w = localStack.pop();
                queue.add(w);
                if(debug)
                    PrintMessage.See(": " + w.getPrettyPrinted().trim());
            }while (!w.equals(a));
//             skip cnt == 1, single node is not in a cycle
            if(queue.size() > 1){
                for(ASTNode q : queue){
                    cycleMap.put(q, true);
                }
            }
            else if(queue.size() == 1){
                ASTNode one = queue.get(0);
                List<DepNode> child = localStmtMap.get(one).getChild();
                if(child.size()==1 && one.equals(child.get(0).getStmt())){
                    cycleMap.put(one, true);
                }
            }
//            while(!localStack.pop().equals(a)) cnt++; // add pop() to current strongly connected component
//            PrintMessage.See("[after] size = " + localStack.size());
        }
    }

}
