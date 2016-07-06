package mc2mc.analysis;

import ast.ASTNode;
import ast.Expr;
import ast.NameExpr;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.*;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class TirAnalysisTrick extends TIRAbstractNodeCaseHandler {

    public Map<TIRNode, Map<String, Set<TIRNode>>> localUDMap = null;
    public Map<ASTNode, String> trickMap = null;

    public TirAnalysisTrick(AnalysisEngine engine){
        localUDMap = engine.getUDChainAnalysis().getChain();
        trickMap = new HashMap<>();
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
    public void caseTIRArrayGetStmt(TIRArrayGetStmt node){
        // a = b(i);
        String arrayName = node.getArrayName().getID();
        String lhsName = node.getLValues().iterator().next();
        String trueString = lhsName + "=" + arrayName + ";";
        String falseString= node.getPrettyPrinted().trim();
        String rtn = genIfBlock(arrayName,node.getIndices(),localUDMap.get(node),trueString, falseString);
        if(!rtn.isEmpty()){
            trickMap.put(node, rtn);
        }
    }

    @Override
    public void caseTIRArraySetStmt(TIRArraySetStmt node){
        // a(i) = b;
        String arrayName = node.getArrayName().getID();
        String rhsName = ((NameExpr)(node.getRHS())).getVarName();
        String trueString = arrayName + "=" + rhsName + ";";
        String falseString= node.getPrettyPrinted().trim();
        String rtn = genIfBlock(arrayName,node.getIndices(),localUDMap.get(node),trueString,falseString);
        if(!rtn.isEmpty()){
            trickMap.put(node, rtn);
        }
    }

    private String genIfBlock(String arrayName,
                              TIRCommaSeparatedList args,
                              Map<String, Set<TIRNode>> defSet,
                              String trueString,
                              String falseString){
        if(arrayName.equals("sampleOut")){
            int xx=10;
        }
        int cnt = 1, cnt0 = 0;
        String cond = "", str="";
        boolean isFail = false;
        boolean oneItem= args.getNumChild()==1;
        String op = oneItem?"length":"size";
        for(Expr e : args){
            if(e instanceof NameExpr){
                String argName = e.getVarName();
                Set<TIRNode> defs = defSet.get(argName);
                if(defs==null
                        || defs.size() != 1
                        || !checkIndex(argName, ((ASTNode)(defs.iterator().next())))){
                    isFail = true;
                    break;
                }
                if(cnt0 > 0)
                    cond += "and("+cond+",";
                if(oneItem)
                    cond += "length("+arrayName+")";
                else
                    cond += "size(" + arrayName + "," + cnt +")";
                cond += "==length(" + e.getVarName() + ")";
                if(cnt0 > 0)
                    cond += ")";
                cnt0++;
            }
            cnt++;
        }
        if(!isFail) {
//            str = "if " + cond + "\n" + trueString + "\nelse\n" + falseString + "\nend";
            str = cond;
        }
        return str;
    }

    private boolean checkIndex(String n, ASTNode currentNode){
        if(currentNode!=null
                && currentNode instanceof TIRCallStmt
                && ((TIRCallStmt) currentNode).getFunctionName().getID().equals("colon")){
            return true;
        }
        return false;
    }

    public void printTrickMap(){
        for(ASTNode a : trickMap.keySet()){
            PrintMessage.See(a.getPrettyPrinted()+" -> " + trickMap.get(a));
        }
    }
}
