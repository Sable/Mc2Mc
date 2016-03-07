package mc2mc.analysis;

import natlab.tame.BasicTamerTool;
import natlab.tame.callgraph.SimpleFunctionCollection;
import natlab.tame.classes.reference.PrimitiveClassReference;
import natlab.tame.tamerplus.transformation.TransformationEngine;
import natlab.tame.tir.TIRFunction;
import natlab.tame.valueanalysis.IntraproceduralValueAnalysis;
import natlab.tame.valueanalysis.ValueAnalysis;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValueFactory;
import natlab.tame.valueanalysis.value.Args;
import natlab.tame.valueanalysis.value.ValueFactory;
import natlab.toolkits.filehandling.GenericFile;
import natlab.toolkits.path.FileEnvironment;

import java.util.ArrayList;

/**
 * TirAnalyses
 */
public class TirAnalyses {
    private String inputFile;
    private String parameters;
    private String[] shapeDesc;

    GenericFile localFile;
    FileEnvironment localEnv;
    SimpleFunctionCollection localCallgraph;
    ValueAnalysis<AggrValue<BasicMatrixValue>> localAnalysis = null;

    public TirAnalyses(String file, String args){
        inputFile = file;
        parameters = args;
        shapeDesc = new String[1];
        shapeDesc[0] = parameters; //change it later
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

    /*
    * Tamer value analysis
     */
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
}
