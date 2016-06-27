package mc2mc.analysis;

import ast.ASTNode;
import ast.Name;
import mc2mc.mc2lib.PrintMessage;
import natlab.tame.tir.TIRFunction;
import natlab.tame.tir.TIRIfStmt;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TirAnalysisUDF extends TirAnalysisPropagateShape {

    private TIRFunction fnode = null;
    private Map<String, PromotedShape>  inputPS = null;
    private Map<String, PromotedShape> outputPS = null;

    public TirAnalysisUDF(ASTNode tree, Map<ASTNode,
            ValueFlowMap<AggrValue<BasicMatrixValue>>> fValueMap,
                          Map<String, PromotedShape> argPS) {
        super(tree, fValueMap);
        inputPS = argPS;
        fnode = (TIRFunction)tree;
    }



    @Override
    public Map<String, PromotedShape> newInitialFlow() {
        return new HashMap<>(inputPS);  //initialize with input parameters
    }

    @Override
    public void caseTIRFunction(TIRFunction node){
        PrintMessage.See("UDF: " + node.getName().getID());
        caseASTNode(node);
    }

    @Override
    public void caseTIRIfStmt(TIRIfStmt node){
        // if conversion
    }

//    @Override
//    public void initStmt(ASTNode stmt){
//        // do nothing
//    }

    /*
    * What criteria is set for function vectorizable?
     */
    public boolean decideUDF(){
        for(ASTNode n : getOutFlowSets().keySet()){
            java.util.Map<String, PromotedShape> value = getOutFlowSets().get(n);
            for(String s : value.keySet()){
                if(!checkPS(value.get(s))) return false;
            }
        }
        return true;
    }

    public boolean checkPS(PromotedShape ps){
        if(ps.isT()) return false;
        return true;
    }

    public Map<String, PromotedShape> getReturns(){
//        printList(fnode.getInputParamList(),  "input  parameter");
//        printList(fnode.getOutputParamList(), "output parameter");
        return getPSbyName(fnode.getOutputParamList());
    }

    public Map<String, PromotedShape> getPSbyName(ast.List<Name> ioList){
        Map<String, PromotedShape> finalOutSet = currentOutSet;
        Map<String, PromotedShape> res = new HashMap<>();
        for(Name a : ioList){
            String nameA = a.getID();
            res.put(nameA, finalOutSet.get(nameA));
        }
        return res;
    }

    private void printList(ast.List<Name> x, String w){
        PrintMessage.See(w);
        printList(x);
    }

    private void printList(ast.List<Name> x){
        for(Name a : x){
            PrintMessage.See(a.getID());
        }
    }
}
