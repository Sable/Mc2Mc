package mc2mc.analysis;

import ast.Stmt;
import mc2mc.mc2lib.PrintMessage;
import mc2mc.mc2lib.TamerViewer;
import natlab.tame.BasicTamerTool;
import natlab.tame.callgraph.SimpleFunctionCollection;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tamerplus.analysis.ReachingDefinitions;
import natlab.tame.tamerplus.transformation.TransformationEngine;
import natlab.tame.tir.*;
import natlab.tame.valueanalysis.IntraproceduralValueAnalysis;
import natlab.tame.valueanalysis.ValueAnalysis;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.toolkits.filehandling.GenericFile;
import natlab.toolkits.path.FileEnvironment;

import java.util.Map;
import java.util.Set;

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

    public TirAnalysis(String file, String[] args){
        inputFile = file;
        shapeDesc = args;
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

    public void RunLoopInvariant(){
        PrintMessage.See("Run loop invariant");
        for(int i=0;i<localAnalysis.getNodeList().size();i++){
            IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis = localAnalysis.getNodeList().get(i).getAnalysis();
            TIRFunction tirfunc = funcanalysis.getTree();

            //PrintMessage.See(tirfunc.getPrettyPrinted());
            AnalysisEngine engine = AnalysisEngine.forAST(tirfunc);
            constructLoopInvariant(engine.getReachingDefinitionsAnalysis());

            TirAnalysisLoopInvariant tirloop = new TirAnalysisLoopInvariant(tirfunc, engine);
            PrintMessage.Delimiter();
            tirloop.run();
            PrintMessage.Delimiter();
        }

    }

    private void constructLoopInvariant(ReachingDefinitions rds){
        for(TIRNode visitedStmt : rds.getVisitedStmtsOrderedList()){
            if(visitedStmt instanceof TIRFunction){
                // sth.
            }
            else if(visitedStmt instanceof TIRForStmt){
                TIRForStmt tfor = (TIRForStmt)visitedStmt;
                PrintTirStmts(tfor.getStatements());
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
