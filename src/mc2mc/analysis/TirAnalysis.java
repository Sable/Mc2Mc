package mc2mc.analysis;

import ast.ASTNode;
import ast.Stmt;
import mc2mc.mc2lib.*;
import natlab.tame.BasicTamerTool;
import natlab.tame.callgraph.SimpleFunctionCollection;
import natlab.tame.callgraph.StaticFunction;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tamerplus.analysis.ReachingDefinitions;
import natlab.tame.tamerplus.transformation.TransformationEngine;
import natlab.tame.tir.*;
import natlab.tame.valueanalysis.IntraproceduralValueAnalysis;
import natlab.tame.valueanalysis.ValueAnalysis;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.toolkits.filehandling.GenericFile;
import natlab.toolkits.path.FileEnvironment;

import java.util.*;

/**
 * TirAnalyses
 */
public class TirAnalysis {
    private String inputFile;
    private String[] shapeDesc;

    GenericFile localFile;
    FileEnvironment localEnv;
    SimpleFunctionCollection localCallgraph;
    ValueAnalysis<AggrValue<BasicMatrixValue>> localAnalysis = null;
    boolean noChange = true;
    String mainFuncName = "";
    String outDir = "";

    public TirAnalysis(String file, String[] args, String out){
        inputFile = file;
        shapeDesc = args;
        outDir = out;
        Init();
    }

    private void Init(){
        BasicTamerTool.setDoIntOk(false);
        localFile      = GenericFile.create(inputFile); //input file
        localEnv       = new FileEnvironment(localFile);
        localCallgraph = new SimpleFunctionCollection(localEnv); //contains all functions
        localAnalysis  = BasicTamerTool.analyze(shapeDesc, localEnv);
    }

    /*
     * Experimental function (deletable)
     */
    public void TestTirFunction(){
        if(localAnalysis == null)
            return;
        System.out.println("test analysis");
        for(int i=0;i<localAnalysis.getNodeList().size();i++){
            IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis = localAnalysis.getNodeList().get(i).getAnalysis();
            TIRFunction tirfunc = funcanalysis.getTree();
            System.out.println("function " + tirfunc.getName().getID());
            System.out.println(tirfunc.getPrettyPrinted());
            System.out.println("transformed back");
            TransformationEngine transengine = TransformationEngine.forAST(tirfunc);
            System.out.println(transengine.getTIRToMcSAFIRWithoutTemp().getTransformedTree().getPrettyPrinted());
        }
    }

    public void TestLocalAnalysis(){
        System.out.println("start output:");
        System.out.println(localFile.toString());
        System.out.println(localAnalysis.toString());
    }

//    public void RunLoopInvariant(){
    public void runAnalysis() {
        int op = 2;
        boolean printFunc = false;
        boolean debug =false;
        mainFuncName = localAnalysis.getFunctionCollection().getMain().getName();
        String outPath = getOutPath();
        List<String> newProgram = new ArrayList<>();
        newProgram.add("main slot");

        CommonFunction.initFnHashMap();

        // Add all function names
        for (StaticFunction f : localAnalysis.getFunctionCollection().getAllFunctions()) {
            CommonFunction.addFuncName(f.getName());
        }
        CommonFunction.setValueAnalysis(localAnalysis);

        for (int i = 0; i < localAnalysis.getNodeList().size(); i++) {
            IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis =
                    localAnalysis.getNodeList().get(i).getAnalysis();
            TIRFunction tirfunc = funcanalysis.getTree();

            List<String> oldFn = CommonFunction.transformFunction(tirfunc, null, 2);

            if (printFunc) {
                PrintMessage.See(tirfunc.getPrettyPrinted());
                PrintMessage.See(funcanalysis.getResult().toString());
            }
            AnalysisEngine engine = AnalysisEngine.forAST(tirfunc);
            if(debug)
                constructLoopInvariant(engine.getReachingDefinitionsAnalysis());

            Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> funcValueMap =
                    funcanalysis.getOutFlowSets();

//            ValueFlowMap<AggrValue<BasicMatrixValue>> currentFlows = funcanalysis.getOutFlowSets().get(tirfunc);
//            // Print
//            for(String s : currentFlows.keySet()){
//                PrintMessage.See("s = " + s);
//                ValueSet<AggrValue<BasicMatrixValue>> x = currentFlows.get(s);
//                for(AggrValue<BasicMatrixValue> one : x.values()){
//                    BasicMatrixValue t = (BasicMatrixValue)one;
//                    Shape s0 =t.getShape();
//                    PrintMessage.See("Shape = " + s0.toString());
//                }
//            }


            if (op == 0) {
                // Loop invariant
                PrintMessage.See("Run loop invariant");
                TirAnalysisLoopInvariant tirloop = new TirAnalysisLoopInvariant(tirfunc, engine);
                PrintMessage.delimiter();
                tirloop.run();
                PrintMessage.delimiter();
            } else if (op == 1) {
                // Available sub-expression
                PrintMessage.See("** Run available sub-expression **");
                TirAnalysisSubExpr tirsub = new TirAnalysisSubExpr(tirfunc, engine);
                tirsub.analyze();
//                tirsub.getFinalInfo();
//                PrintMessage.See("start tirfun analyze");
                tirfunc.analyze(new TirAnalysisSubExprPrint(tirsub));
            } else if (op == 2) {

                RenameSpecialName rs = new RenameSpecialName(funcValueMap.get(tirfunc));
                tirfunc.analyze(rs); // rename mtimes and mrdivde

                String currentFuncName = tirfunc.getName().getID();
                PrintMessage.See("** [Phase 1] Run loop analysis **", currentFuncName);
                TirAnalysisLoop tirLoop = new TirAnalysisLoop(engine, funcValueMap, currentFuncName);
                tirfunc.analyze(tirLoop);
                if(currentFuncName.equals("seidel")){
                    int xx = 10;
                }
                List<String> newFn = null;
                if(!CommonFunction.getFnHashMap().containsKey(tirfunc)) {
                    for(ASTNode a : tirLoop.getStmtHashMap().keySet()){
                        PrintMessage.See(a.getPrettyPrinted(), "stmt map");
                    }
                    newFn = CommonFunction.transformFunction(tirfunc, tirLoop.getStmtHashMap(), 2);
                }
                else {
                    newFn = CommonFunction.getFnHashMap().get(tirfunc);
                }
                if (!CommonFunction.sameFunction(oldFn, newFn)) {
                    //save
                    CommonFunction.setFnHashMap(tirfunc, newFn);
                }

                if(CommonFunction.getFnHashMap().size() > 0){
                    noChange = false;
                }

//                tirLoop.printRWSet();
//                PrintMessage.See("** [Phase 2] Code generation **", tirfunc.getName().getID());
                TirAnalysisTrim tirTrim = new TirAnalysisTrim(engine);
                tirfunc.analyze(tirTrim);
                tirTrim.printSets();
            }
        }

        Set<String> fnNameSet = new HashSet<>();
        for (int i = 0; i < localAnalysis.getNodeList().size(); i++) {
            IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis =
                    localAnalysis.getNodeList().get(i).getAnalysis();
            TIRFunction tirfunc = funcanalysis.getTree();
            String fName = tirfunc.getName().getID();
            boolean isMainFunc = mainFuncName.equals(fName);
            if(fnNameSet.contains(fName)) continue;
            fnNameSet.add(fName);

            String flatString = null;
            if(CommonFunction.getFnHashMap().containsKey(tirfunc)){
                List<String> newFn = CommonFunction.getFnHashMap().get(tirfunc);
                flatString = flatListString(newFn);
            }
            else{
                List<String> oldFn = CommonFunction.transformFunction(tirfunc, null, 2);
                flatString = flatListString(oldFn);
            }

            if(isMainFunc)
                newProgram.set(0, flatString);
            else
                newProgram.add(flatString);
        }
        // update new functions
        if(debug)
            CommonFunction.printFnHashMap();

        PrintMessage.See("noChange? " + noChange, "check change");

//      TempFactory.setPrefix("set temp prefix");
        if (noChange) {
            int writeOp = 2; // 1: TameIR; 2:Plus; 3:Trick
            if(writeOp == 1) {
                List<String> resultList = new ArrayList<>();
                resultList.add("main");
                for (StaticFunction f : localAnalysis.getFunctionCollection().getAllFunctions()) {
//                String fnString = TransformationEngine.forAST(f.getAst())
//                        .getTIRToMcSAFIRWithoutTemp()
//                        .getTransformedTree()
//                        .getPrettyPrinted();
                    String fnString = f.getAst().getPrettyPrinted();
                    if (f.getName().equals(mainFuncName)) { //main function
                        resultList.set(0, fnString);
                    } else {
                        resultList.add(fnString);
                    }
                }
                CommonFunction.save2File(resultList, outPath); //save to a final file
                PrintMessage.See("Wrote to a final file");
            }
            else if(writeOp==2) {
                List<String> finalFn = new ArrayList<>();
                finalFn.add("main");
                for (int i = 0; i < localAnalysis.getNodeList().size(); i++) {
                    IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis =
                            localAnalysis.getNodeList().get(i).getAnalysis();
                    TIRFunction tirfunc = funcanalysis.getTree();
                    AnalysisEngine engine = AnalysisEngine.forAST(tirfunc);

                    TirAnalysisTrick tirTrick = new TirAnalysisTrick(engine);
                    tirfunc.analyze(tirTrick);

                    TamerPrint tp = new TamerPrint(engine, tirfunc);
                    tp.processBlocks();
//                    tp.printBlocks();
                    List<String> oneFn = CommonFunction.transformFunction(tirfunc, tp.getMap2String(), tp.getMapped(), tirTrick.trickMap);
                    String fnString = flatListString(oneFn);
                    if (tirfunc.getName().getID().equals(mainFuncName)) { //main function
                        finalFn.set(0, fnString);
                    }
                    else {
                        finalFn.add(fnString);
                    }
                }
                CommonFunction.save2File(finalFn, outPath); //save to a final file
                PrintMessage.See("Wrote to a final file");
            }
            else {
                List<String> finalFn = new ArrayList<>();
                finalFn.add("main");
                for (int i = 0; i < localAnalysis.getNodeList().size(); i++) {
                    IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis =
                            localAnalysis.getNodeList().get(i).getAnalysis();
                    TIRFunction tirfunc = funcanalysis.getTree();
                    AnalysisEngine engine = AnalysisEngine.forAST(tirfunc);

                    TirAnalysisTrick tirTrick = new TirAnalysisTrick(engine);
                    tirfunc.analyze(tirTrick);
//                    tirTrick.printTrickMap();


                    List<String> oneFn = CommonFunction.transformFunction(tirfunc, tirTrick.trickMap);
                    String fnString = flatListString(oneFn);
                    if (tirfunc.getName().getID().equals(mainFuncName)) { //main function
                        finalFn.set(0, fnString);
                    }
                    else {
                        finalFn.add(fnString);
                    }
                }
                CommonFunction.save2File(finalFn, outPath); //save to a final file
                PrintMessage.See("Wrote to a final file 3");
            }
        }
        else {
            CommonFunction.save2File(newProgram, outPath);
            PrintMessage.See("Wrote to a temp file");
        }

    }

//    public List<String> transformFunction(ASTNode node, Map<ASTNode, List<String>> stmtHashMap){
//        List<String> rtn = new ArrayList<>();
//        if(node instanceof TIRForStmt){
//            if(stmtHashMap!=null && stmtHashMap.containsKey(node)){
//                rtn.addAll(stmtHashMap.get(node));
//            }
//            else {
//                rtn.add("for " + ((TIRForStmt) node).getAssignStmt().getPrettyPrinted().trim());
//                for(Stmt s : ((TIRForStmt) node).getStatements()){
//                    rtn.addAll(transformFunction(s, stmtHashMap));
//                }
//                rtn.add("end");
//            }
//        }
//        else
//            rtn.addAll(CommonFunction.generateCommonNode(node));
//        return rtn;
//    }




    public boolean getNoChange(){
        return noChange;
    }

    public String getMainFuncName(){
        return mainFuncName;
    }

    public String getOutPath(){
        return outDir + "/" + mainFuncName + ".m";
    }

    String flatListString(List<String> listString){
        String rtn = "";
        for(String s : listString){
            String newS = trimCallStmt(s);
            if(!newS.isEmpty())
                rtn += newS + "\n";
        }
        return rtn;
    }
    String trimCallStmt(String s){
        if(s.startsWith("[")){
            int bracket = s.indexOf(']');
            if(bracket>=0){
                String names = s.substring(1,bracket);
                if(names.indexOf(',') < 0){
//                    PrintMessage.See("pre: " + s);
//                    PrintMessage.See("aft: " + names + s.substring(bracket+1));
                    return names + s.substring(bracket+1);
                }
            }
        }
        else if(s.startsWith("function")){
            return "function " + trimCallStmt(s.substring(8).trim());
        }
        return  s;
    }

    private void constructLoopInvariant(ReachingDefinitions rds){
        for(TIRNode visitedStmt : rds.getVisitedStmtsOrderedList()){
            if(visitedStmt instanceof TIRFunction){
                // sth.
            }
            else if(visitedStmt instanceof TIRForStmt){
                TIRForStmt tfor = (TIRForStmt)visitedStmt;
//                PrintTirStmts(tfor.getStatements());
                //PrintMessage.See(tfor.getPrettyPrinted());
                //PrintNodeSet(rds.getReachingDefinitionsForNode(visitedStmt));
            }
            else if(visitedStmt instanceof TIRWhileStmt){
                // the same as for stmt?
            }
        }


    }

    void PrintTirStmts(TIRStatementList tlist){
        for(Stmt s : tlist){
            PrintMessage.See("- " + s.getPrettyPrinted());
//            PrintMessage.See("getStartLine = " + s.getStartLine());
//            PrintMessage.See("getTranslatedEndLine =  " + s.getTranslatedEndLine());
//            PrintMessage.See("getEndLine =  " + s.getEndLine());
        }
    }

    void PrintNodeSet(Map<String, Set<TIRNode>> input){
        for (Map.Entry myentry : input.entrySet()){
            System.out.println(myentry.getKey() + ":");
            Set<TIRNode> mynode = (Set<TIRNode>)myentry.getValue();
            for(TIRNode t : mynode){
                TIRStmt s = (TIRStmt)t;
                System.out.println("- " + s.toString());
            }
        }
    }


    /*
    * Tamer value analysis (deprecated)
     */
    /*
    public void TirValueAnalysis(){
        ArrayList<AggrValue<BasicMatrixValue>> inputValues = getListOfInputValues(shapeDesc);
        ValueFactory<AggrValue<BasicMatrixValue>> factory = new BasicMatrixValueFactory();
        ValueAnalysis<AggrValue<BasicMatrixValue>> FuncAnalysis = new ValueAnalysis<AggrValue<BasicMatrixValue>>(
                localCallgraph, Args.newInstance(inputValues) , factory);
        System.out.println("++++++++++++ Result: (Interprocedural analysis) +++++++++++");
        System.out.println(FuncAnalysis.toString());
        // see isFuncVectorOK to figure out how to fetch each variable in FuncAnalysis
        System.out.println("------------ Done with interprocedural analysis -----------");
    }

    public ArrayList<AggrValue<BasicMatrixValue>> getListOfInputValues(String[] args) {
        ArrayList<AggrValue<BasicMatrixValue>> list = new ArrayList<AggrValue<BasicMatrixValue>>(args.length);
        for (String argSpecs : args) {
            String delims = "[\\&]";
            String[] specs = argSpecs.split(delims);
            PrimitiveClassReference clsType = PrimitiveClassReference.valueOf(specs[0]);
            list.add(new BasicMatrixValue(null, clsType, specs[1], specs[2]));
        }
        return list;
    }
    */

    public void RunTamerViewer(){
        PrintMessage.See("Run Tamer viewer");
        for(int i=0;i<localAnalysis.getNodeList().size();i++){
            IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis = localAnalysis.getNodeList().get(i).getAnalysis();
            TIRFunction tirfunc = funcanalysis.getTree();
            TamerViewer tv = new TamerViewer(tirfunc);
            tv.GetViewer();
            PrintMessage.See(tirfunc.getPrettyPrinted());
        }
    }

}
