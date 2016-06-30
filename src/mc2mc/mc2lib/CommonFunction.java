package mc2mc.mc2lib;

import ast.*;
import natlab.tame.builtin.Builtin;
import natlab.tame.callgraph.StaticFunction;
import natlab.tame.tir.*;
import natlab.tame.valueanalysis.IntraproceduralValueAnalysis;
import natlab.tame.valueanalysis.ValueAnalysis;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.toolkits.path.BuiltinQuery;

import java.io.*;
import java.util.*;
import java.util.List;

public class CommonFunction {

    private static Set<String> funcNameList = new HashSet<>();
    private static ValueAnalysis<AggrValue<BasicMatrixValue>> localAnalysis;
    private static Map<ASTNode, List<String>> fnHashMap = null;
    private static String mainFile = "";
    private static String tempFile = "";

    public static void initFnHashMap(){
        fnHashMap = new HashMap<>();
    }

    public static void setFnHashMap(ASTNode node, List<String> content){
        fnHashMap.put(node, content);
    }

    public static void printFnHashMap(){
        PrintMessage.See("new functions: " + fnHashMap.size());
        for(ASTNode f : fnHashMap.keySet()){
            PrintMessage.See(((TIRFunction)f).getName().getID(), "Function Name");
            PrintMessage.arrayList(fnHashMap.get(f));
        }
    }

    public static void setInfo(String mFile, String tFile){
        mainFile = mFile;
        tempFile = tFile;
    }

    public static String getMainFile(){
        return mainFile;
    }

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

    public static boolean isBuildinOrUDF(String name){
        return (funcNameList.contains(name) || isBuiltIn(name));
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

    public static void save2File(List<String> input, String outPath){
        Writer writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(outPath), "utf-8"));
            PrintMessage.See("writing to " + outPath);
            for(String s : input) {
                writer.write(s);
                writer.write("\n");
            }
        } catch (IOException ex) {
            // report
        } finally {
            try {writer.close();} catch (Exception ex) {/*ignore*/}
        }
    }

    //
    public static List<String> transformFunction(ASTNode node, Map<ASTNode, List<String>> stmtHashMap, int op){
        List<String> rtn = new ArrayList<>();
        if(node instanceof TIRFunction) {
            String inputP = genParameterList(((TIRFunction) node).getInputParamList());
            String outputP= genParameterList(((TIRFunction) node).getOutputParamList());
            rtn.add("function "
                    + (outputP.isEmpty()?"":"["+outputP+"] = ")
                    + ((TIRFunction) node).getName().getID()
                    +"(" + inputP + ")");
            for (Stmt s : ((TIRFunction)node).getStmtList()) {
                rtn.addAll(transformFunction(s, stmtHashMap, op));
            }
            rtn.add("end");
        }
//        else if(node instanceof TIRCallStmt) {
//            Expr lhs = ((TIRCallStmt) node).getLHS();
//            String str = "";
//            if (lhs instanceof MatrixExpr) {
//                int numRow = ((MatrixExpr) lhs).getNumRow();
//                int numElement = numRow < 1 ? 0 : ((MatrixExpr) lhs).getRow(0).getNumElement();
//                if (numRow == 1 && numElement == 1) {
//                    str = ((NameExpr) ((MatrixExpr) lhs).getRow(0).getElement(0)).getName().getID()
//                            + " = "
//                            + ((TIRCallStmt) node).getRHS().getPrettyPrinted();
//                } else
//                    str = node.getPrettyPrinted();
//            } else
//                str = node.getPrettyPrinted();
//            rtn.add(str.trim());
//        }
        else if(op == 1 && node instanceof TIRIfStmt){
            Map<ASTNode, List<String>> newIfStmt = stmtHashMap;
            if(newIfStmt.containsKey(node)){
                rtn.addAll(newIfStmt.get(node));
            }
            else {
                rtn.add("if " + ((TIRIfStmt) node).getConditionVarName().getID());
                for (Stmt s : ((TIRIfStmt) node).getIfStatements()) {
                    rtn.addAll(transformFunction(s, newIfStmt, op));
                }
                rtn.add("else");
                if(((TIRIfStmt) node).hasElseBlock()){
                    for(Stmt s : ((TIRIfStmt) node).getElseStatements()){
                        rtn.addAll(transformFunction(s, newIfStmt, op));
                    }
                }
                rtn.add("end");
            }
        }
        else if(op == 2 && node instanceof TIRForStmt){
            if(stmtHashMap!=null && stmtHashMap.containsKey(node)){
                rtn.addAll(stmtHashMap.get(node));
            }
            else {
                rtn.add("for " + ((TIRForStmt) node).getAssignStmt().getPrettyPrinted().trim());
                for(Stmt s : ((TIRForStmt) node).getStatements()){
                    rtn.addAll(transformFunction(s, stmtHashMap, op));
                }
                rtn.add("end");
            }
        }
        else {
            String[] mutipleLine = node.getPrettyPrinted().trim().split("\\\n");
            for(String line : mutipleLine) {
                rtn.add(line.trim());
            }
        }
        return rtn;
    }

    private static String genParameterList(ast.List<Name> input){
        String rtn = "";
        int i = 0;
        for(Name n : input){
            if(i>0) rtn += ",";
            rtn += n.getID();
            i++;
        }
        return rtn;
    }

}
