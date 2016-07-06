package mc2mc.mc2lib;

import ast.ASTNode;
import ast.Expr;
import ast.NameExpr;
import natlab.tame.tir.TIRCallStmt;
import natlab.tame.tir.TIRNode;
import natlab.tame.tir.analysis.TIRAbstractNodeCaseHandler;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.ValueSet;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wukefe on 7/2/16.
 */
public class RenameSpecialName extends TIRAbstractNodeCaseHandler {

    ValueFlowMap<AggrValue<BasicMatrixValue>> localValueMap;
    private Map<String, String> changeList;

    public RenameSpecialName(ValueFlowMap<AggrValue<BasicMatrixValue>> valueMap){
        localValueMap = valueMap;
        changeList = new HashMap<>();
        changeList.put("mtimes", "times");
        changeList.put("mrdivide", "rdivide");
        changeList.put("mpower", "power");
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
    public void caseTIRCallStmt(TIRCallStmt node){
        rename(node);
    }


    public void rename(TIRCallStmt node){
        // if any side is a scalar
        // mtimes -> times
        // mrdivide -> rdivide
        String op = node.getFunctionName().getID();
        if(changeList.containsKey(op)){
            boolean fid = false;
            if(node.getLHS().getPrettyPrinted().trim().equals("[mc_t150]")){
                int xx= 10;
            }
            for(Expr e : node.getArguments()){
                if(e instanceof NameExpr){
                    if(isSingleAndScalar(e.getVarName())){
                        fid = true;
                        break;
                    }
                }
            }
            if(fid) {
                node.getFunctionName().setID(changeList.get(op));
            }
        }
    }

    boolean isSingleAndScalar(String name){
        if(localValueMap.containsKey(name)){
            ValueSet<AggrValue<BasicMatrixValue>> currentValue =localValueMap.get(name);
            if(currentValue.size() == 1){
                return ((BasicMatrixValue)currentValue.iterator().next()).getShape().isScalar();
            }
        }
        return false;
    }

}
