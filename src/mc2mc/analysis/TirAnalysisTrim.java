package mc2mc.analysis;

import ast.ASTNode;
import ast.AssignStmt;
import ast.Expr;
import ast.NameExpr;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.TIRArraySetStmt;
import natlab.tame.tir.TIRCallStmt;
import natlab.tame.tir.TIRCommaSeparatedList;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * COE: Colon operation elimination
 * PAE: Preallocation eliminationx
 * Test on <Test4>
 */
public class TirAnalysisTrim extends TIRAbstractNodeCaseHandler {

    public Map<TIRNode, Map<String, Set<TIRNode>>> localUDMap = null;
    String unknownUse = "?";
    String unknownDef = "??";
    public Set<TIRNode> removableStmt;
    public Set<TIRNode> reduciableStmt;

    public TirAnalysisTrim(AnalysisEngine engine){
        localUDMap = engine.getUDChainAnalysis().getChain();
        removableStmt = new HashSet<>();
        reduciableStmt= new HashSet<>();
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
    public void caseTIRArraySetStmt(TIRArraySetStmt node){
        String arrayName = node.getArrayName().getID();
        Set<TIRNode> defSet = localUDMap.get(node).get(arrayName);
        if(defSet.size() == 1){
            TIRNode oneDef = defSet.iterator().next();
            String[] defSide = isPrealloc((ASTNode)oneDef);
            String[] useSide = collectIndices(node);
            if(defSide == null || useSide == null)
                return; // do nothing
            int defNum = defSide.length;
            int useNum = useSide.length / 2;
            boolean doable = false;
            if(useNum == 1 && defNum == 2){
                boolean fullColon = useSide[0].equals("1") && !useSide[1].equals(unknownDef);
                if(fullColon){
                    // check vector's shape: 1xn or nx1
                    if(defSide[0].equals("1") && defSide[1].equals(useSide[1]))
                        doable = true;
                    else if(defSide[1].equals("1") && defSide[0].equals(useSide[1]))
                        doable = true;
                }
            }
            else if(useNum == defNum){
                for(int i=0;i<useNum;i++){
                    boolean fullColon = useSide[i*2].equals("1") && !useSide[i*2+1].equals(unknownDef);
                    if(fullColon && useSide[i*2+1].equals(defSide[i])){
                        doable = true;
                    }
                    else {
                        doable = false;
                        break;
                    }
                }
            }
            if(doable){
                removableStmt.add(oneDef);
                reduciableStmt.add(node);
            }
        }
    }

    // x = zeros(1,n) ==>
    // 1) t0 = 1
    // 2) x = zeros(t0, n)
    private String[] isPrealloc(ASTNode node) {
        if (node instanceof TIRCallStmt) {
            String op = ((TIRCallStmt) node).getFunctionName().getID();
            if (isPreBuiltin(op)) {
                TIRCommaSeparatedList args = ((TIRCallStmt) node).getArguments();
                int num = args.size();
                String[] dims = new String[num == 1 ? 2 : num];
                if (num == 1) { // zeros(n)
                    String name0 = args.getName(0).getID();
                    String dim0 = lookupOne(node, name0);
                    dims[0] = dim0;
                    dims[1] = dim0;
                } else {
                    for (int i = 0; i < num; i++) {
                        String name0 = args.getName(i).getID();
                        String dim0 = lookupOne(node, name0);
                        dims[i] = dim0;
                    }
                }
                return dims;
            }
        }
        return null;
    }

    private boolean isPreBuiltin(String n){
        return n.equals("zeros") || n.equals("ones") || n.equals("rand");
    }

    // ? A(:)
    // A(a,b) ==> a = 1:2:n
    private String[] collectIndices(TIRArraySetStmt node) {
        String[] rtn = new String[node.getIndices().size() * 2];
        int k = 0;
        for (Expr e : node.getIndices()) {
            String low = unknownUse;
            String high = unknownUse;
            if (e instanceof NameExpr) {
                TIRNode n0 = lookupNode(node, e.getVarName());
                if (n0 instanceof TIRCallStmt) {
                    String op = ((TIRCallStmt) n0).getFunctionName().getID();
                    if (op.equals("colon") && ((TIRCallStmt) n0).getArguments().size() == 2) {
                        low  = lookupOne((ASTNode)n0, ((TIRCallStmt) n0).getArguments().getName(0).getID());
                        high = lookupOne((ASTNode)n0, ((TIRCallStmt) n0).getArguments().getName(1).getID());
                    }
                }
            }
            rtn[k] = low;
            rtn[k + 1] = high;
            k += 2;
        }
        return rtn;
    }

    // loopup one level
    private String lookupOne(ASTNode node, String name){
        TIRNode n0 = lookupNode(node, name);
        if(n0 == null)
            return name; //nullName
        else
            return ((AssignStmt) n0).getRHS().getPrettyPrinted().trim();
    }

    private TIRNode lookupNode(ASTNode node, String name){
        Set<TIRNode> defSet = localUDMap.get(node).get(name);
        if(defSet.size() == 1){
            TIRNode n0 = defSet.iterator().next();
            if(n0 instanceof AssignStmt){
                return n0;
            }
        }
        return null;
    }

    public void printSets(){
        PrintMessage.printTirNodeSet(removableStmt, "removableStmt");
        PrintMessage.printTirNodeSet(reduciableStmt, "reduciableStmt");
    }

}
