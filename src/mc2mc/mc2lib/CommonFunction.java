package mc2mc.mc2lib;

import ast.*;
import natlab.tame.builtin.Builtin;
import natlab.tame.callgraph.StaticFunction;
import natlab.tame.tir.TIRCallStmt;
import natlab.tame.tir.TIRFunction;
import natlab.tame.tir.TIRStmt;
import natlab.tame.valueanalysis.IntraproceduralValueAnalysis;
import natlab.tame.valueanalysis.ValueAnalysis;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.toolkits.path.BuiltinQuery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommonFunction {

    private static Set<String> funcNameList = new HashSet<>();
    private static ValueAnalysis<AggrValue<BasicMatrixValue>> localAnalysis;

    /**
     * Input with an expr (i.e getRHS)
     * Return a list of name
     */
    public static Set<String> ExtractName(ASTNode e){
        Set<String> rtn = new HashSet<>();
        for(int i=0;i<e.getNumChild();i++){
            ASTNode currentNode = e.getChild(i);
            if(currentNode instanceof ast.Name){
                //rtn.add(((NameExpr)currentNode).getName().getVarName());
                rtn.add(currentNode.getVarName());
            }
            else {
                rtn.addAll(ExtractName(currentNode));
            }
        }
        return rtn;
    }

    public static int FindLineNo(ASTNode e){
        int lno = 0;
        for(int i=0;i<e.getNumChild();i++){
            ASTNode currentNode = e.getChild(i);
            int localLno = 0;
            if(currentNode instanceof TIRStmt){
                localLno = currentNode.getStartLine();
            }
            if(localLno !=0){
                lno = localLno;
            }
        }
        return lno;
    }

    public static boolean isBuiltIn(String name){
        if(name.equals("i") || name.equals("j"))
            return false;
        else {
            BuiltinQuery query = Builtin.getBuiltinQuery();
            return query.isBuiltin(name);
        }
    }

    public static Set<String> VarNameOnly(Set<String> inputSet){
        Set<String> rtn = new HashSet<>();
        for(String s : inputSet){
            if(isBuiltIn(s)) ;
            else rtn.add(s);
        }
        return rtn;
    }

    public static void addFuncName(String n){
        funcNameList.add(n);
    }
    public static void setValueAnalysis(ValueAnalysis<AggrValue<BasicMatrixValue>> analysis){
        localAnalysis = analysis;
        func2ValueMap = new HashMap<>();
        for(int i=0;i<localAnalysis.getNodeList().size();i++){
            IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis =
                    localAnalysis.getNodeList().get(i).getAnalysis();
            TIRFunction tirfunc = funcanalysis.getTree();
            func2ValueMap.put(tirfunc, funcanalysis.getOutFlowSets()); //save flow information
        }
    }

    public static Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> getValueAnalysis(TIRFunction node){
        return func2ValueMap.get(node);
    }

    public static Map<TIRFunction, Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>>> func2ValueMap;

    public static IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> getFunction(String fname){
        for(int i=0;i<localAnalysis.getNodeList().size();i++) {
            StaticFunction sf = localAnalysis.getNodeList().get(i).getFunction();
            if(sf.getName().equals(fname)){
                return localAnalysis.getNodeList().get(i).getAnalysis();
            }
        }
        return null;
    }

    public static int decideStmt(ASTNode node){
        if(node instanceof TIRCallStmt){
            String op = ((TIRCallStmt) node).getFunctionName().getID();
            if(funcNameList.contains(op)) return 2; //UDF
            else if(isBuiltIn(op)) return 1; //BIF
            return 3; //not-handled functions
        }
        return -1;
    }

    // debug a local node
    public static void DebugNodeStructure(ASTNode node){
        PrintMessage.See("node : " + node.dumpString());
        PrintMessage.See("     : " + node.getNumChild());
        for(int i = 0; i < node.getNumChild(); i++){
            PrintMessage.See("   + : " + node.getChild(i).dumpString());
        }
    }

    public static boolean isTemp(String x){
        return x.startsWith("mc_t");
    }

}
