package mc2mc.analysis;

import ast.ASTNode;
import mc2mc.mc2lib.*;
import natlab.tame.BasicTamerTool;
import natlab.tame.callgraph.SimpleFunctionCollection;
import natlab.tame.callgraph.StaticFunction;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.TIRFunction;
import natlab.tame.valueanalysis.IntraproceduralValueAnalysis;
import natlab.tame.valueanalysis.ValueAnalysis;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.toolkits.filehandling.GenericFile;
import natlab.toolkits.path.FileEnvironment;

import java.io.File;
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
    int writeOp = 2;

    public TirAnalysis(String file, String[] args, String out, int op){
        inputFile = file;
        shapeDesc = args;
        outDir    = out;
        writeOp   = op;
        init();
        checkFolder(outDir);
    }

    /**
     * Initialization
     */
    private void init(){
        BasicTamerTool.setDoIntOk(false);
        localFile      = GenericFile.create(inputFile); //input file
        localEnv       = new FileEnvironment(localFile);
        localCallgraph = new SimpleFunctionCollection(localEnv); //contains all functions
        localAnalysis  = BasicTamerTool.analyze(shapeDesc, localEnv);
    }

    /**
     * Create folder if not exist
     */
    private void checkFolder(String outDir){
        File dir = new File(outDir);
        if(!dir.exists()){
            boolean fileFlag = dir.mkdirs();
            if(fileFlag){
                PrintMessage.See("Folder " + outDir + " has been created successfully.");
            }
        }
    }


    public void runAnalysis(boolean isTameIR){
        if(isTameIR)
            writeTameIR();
        else
            runAnalysis();
    }

    /**
     * Write TameIR if isTameIR is true
     */
    public void writeTameIR(){
        mainFuncName = localAnalysis.getFunctionCollection().getMain().getName();
        String outPath = getOutPath();
        List<String> resultList = new ArrayList<>();
        resultList.add("main");
        for (StaticFunction f : localAnalysis.getFunctionCollection().getAllFunctions()) {
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

//    public void RunLoopInvariant(){
    public void runAnalysis() {
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

            List<String> oldFn = CommonFunction.transformFunction(tirfunc, null, null, 2);

            if (debug) {
                PrintMessage.See(tirfunc.getPrettyPrinted());
                PrintMessage.See(funcanalysis.getResult().toString());
            }
            AnalysisEngine engine = AnalysisEngine.forAST(tirfunc);

            Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> funcValueMap =
                    funcanalysis.getOutFlowSets();

            RenameSpecialName rs = new RenameSpecialName(funcValueMap.get(tirfunc));
            tirfunc.analyze(rs); // rename mtimes and mrdivde
            CommonFunction.getNumofRename(rs.getNumofRenamed());

            String currentFuncName = tirfunc.getName().getID();
            PrintMessage.See("** [Phase 1] Run loop analysis **", currentFuncName);
            TirAnalysisLoop tirLoop = new TirAnalysisLoop(engine, funcValueMap, currentFuncName);
            tirfunc.analyze(tirLoop);
            List<String> newFn = null;
            if (!CommonFunction.getFnHashMap().containsKey(tirfunc)) {
                newFn = CommonFunction.transformFunction(tirfunc, tirLoop.getStmtHashMap(), null, 2);
            } else {
                newFn = CommonFunction.getFnHashMap().get(tirfunc);
            }
            if (!CommonFunction.sameFunction(oldFn, newFn)) {
                //save
                CommonFunction.setFnHashMap(tirfunc, newFn);
            }

            if (CommonFunction.getFnHashMap().size() > 0) {
                noChange = false;
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
                List<String> oldFn = CommonFunction.transformFunction(tirfunc, null, null, 2);
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

//        PrintMessage.See("noChange? " + noChange, "check change");

//      TempFactory.setPrefix("set temp prefix");
        if (noChange) {
//            int writeOp = 2; // 1: TameIR; 2:Plus; 3:Trick
            if(writeOp == 1) { //Vectorized TameIR
                List<String> resultList = new ArrayList<>();
                resultList.add("main");
                for (StaticFunction f : localAnalysis.getFunctionCollection().getAllFunctions()) {
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
            else if(writeOp==2) { //TamePlus with checks
                List<String> finalFn = new ArrayList<>();
                finalFn.add("main");
                boolean flagCheck = false; // plus-no (false) or plus (true)
                Map<ASTNode, String> checkMap;
                for (int i = 0; i < localAnalysis.getNodeList().size(); i++) {
                    IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis =
                            localAnalysis.getNodeList().get(i).getAnalysis();
                    TIRFunction tirfunc = funcanalysis.getTree();
                    AnalysisEngine engine = AnalysisEngine.forAST(tirfunc);

                    if(flagCheck) {
                        Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> funcValueMap =
                                funcanalysis.getOutFlowSets();
                        TirAnalysisCheck tirCheck = new TirAnalysisCheck(engine, funcValueMap);
                        tirfunc.analyze(tirCheck);
                        checkMap = tirCheck.trickMap;
                    }
                    else {
                        checkMap = new HashMap<>();
                    }

                    TamerPrint tp = new TamerPrint(engine, tirfunc);
                    tp.processBlocks();
//                    tp.printBlocks();
                    List<String> oneFn = CommonFunction.transformFunction(tirfunc, tp.getMap2String(), tp.getMapped(), checkMap);
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
            else { //TameIR with checks
                List<String> finalFn = new ArrayList<>();
                finalFn.add("main");
                for (int i = 0; i < localAnalysis.getNodeList().size(); i++) {
                    IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis =
                            localAnalysis.getNodeList().get(i).getAnalysis();
                    TIRFunction tirfunc = funcanalysis.getTree();
                    AnalysisEngine engine = AnalysisEngine.forAST(tirfunc);

                    Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> funcValueMap =
                            funcanalysis.getOutFlowSets();
                    TirAnalysisCheck tirCheck = new TirAnalysisCheck(engine, funcValueMap);
                    tirfunc.analyze(tirCheck);
//                    tirTrick.printTrickMap();


                    List<String> oneFn = CommonFunction.transformFunction(tirfunc, tirCheck.trickMap);
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

        // count how many changes occur
        CommonFunction.setNumofChanges();

    }




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
                    return names + s.substring(bracket+1);
                }
            }
        }
        else if(s.startsWith("function")){
            return "function " + trimCallStmt(s.substring(8).trim());
        }
        return  s;
    }

    public void RunTamerViewer(){
        PrintMessage.See("Run Tamer viewer");
        for(int i=0;i<localAnalysis.getNodeList().size();i++){
            IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis =
                    localAnalysis.getNodeList().get(i).getAnalysis();
            TIRFunction tirfunc = funcanalysis.getTree();
            TamerViewer tv = new TamerViewer(tirfunc);
            tv.GetViewer();
            PrintMessage.See(tirfunc.getPrettyPrinted());
        }
    }

}
