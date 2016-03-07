package mc2mc;

import ast.Program;
import com.google.common.base.Joiner;
import natlab.CompilationProblem;
import natlab.Parse;
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

public class Mc2McTranslator {
    private ValueAnalysis<AggrValue<BasicMatrixValue>> FuncAnalysis;
    public Mc2McTranslator(){
        // constructor
    }

    public static void main(String[] args) {
        if(args.length == 1) {
            Program ast = parseOrDie(args[0]);
            System.out.println(ast.getPrettyPrinted());
//            MatlabVec analysis = new MatlabVec(ast);
//            ast.analyze(analysis);
            // Tamer analysis
            Mc2McTranslator analysis = new Mc2McTranslator();
            //analysis.initAnalysis(args[0]);

            // reaching definition
            //ReachingDef rd = new ReachingDef(ast);
            //rd.analyze();
            //System.out.println("print reaching definition result:");
            //ast.analyze(new ReachingDefPrint(rd));
            //
            //ReachingDefinitions rds = new ReachingDefinitions(ast);
            //ReachingDefinitions.DEBUG = true;
            //AnalysisEngine engine = AnalysisEngine.forAST(ast);
            //rds.analyze(engine);
            //UDDUWeb web = engine.getUDDUWebAnalysis();
            //DUChain.DEBUG = true;
            //DUChain du = web.getDUChain();
//          UDChain ud = web.getUDChain();
            //du.analyze(engine);

            analysis.TestTIR(args[0]);
        }
    }

    public void TestTIR(String file){
        GenericFile gFile = GenericFile.create(file); //input file
        FileEnvironment env = new FileEnvironment(gFile);
        SimpleFunctionCollection callgraph = new SimpleFunctionCollection(env); //contains all functions
        BasicTamerTool.setDoIntOk(false);

        String parametern = "DOUBLE&1*1&REAL";
        String[] shapeDesc = {parametern};
        ValueAnalysis<AggrValue<BasicMatrixValue>> analysis = BasicTamerTool.analyze(shapeDesc, env);

        //ReachingDefinitions.DEBUG = true;
        //UDChain.DEBUG = true;
        for(int i=0;i<analysis.getNodeList().size();i++){
            IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> funcanalysis = analysis.getNodeList().get(i).getAnalysis();
            TIRFunction tirfunc = funcanalysis.getTree();
            System.out.println("function " + tirfunc.getName().getID());
            System.out.println(tirfunc.getPrettyPrinted());
            //AnalysisEngine engine = AnalysisEngine.forAST(tirfunc);
            //engine.getReachingDefinitionsAnalysis().analyze();
            //engine.getUDChainAnalysis().analyze(engine);
            System.out.println("transformed back");
            TransformationEngine transengine = TransformationEngine.forAST(tirfunc);
            System.out.println(transengine.getTIRToMcSAFIRWithoutTemp().getTransformedTree().getPrettyPrinted());
        }
    }

    public void initAnalysis(String filepath){
        GenericFile gFile = GenericFile.create(filepath); //input file
        FileEnvironment env = new FileEnvironment(gFile);
        SimpleFunctionCollection callgraph = new SimpleFunctionCollection(env); //contains all functions
        BasicTamerTool.setDoIntOk(false);

        String parametern = "DOUBLE&1*1&REAL";
        String[] parameters = {parametern};
        ArrayList<AggrValue<BasicMatrixValue>> inputValues = getListOfInputValues(parameters);
        ValueFactory<AggrValue<BasicMatrixValue>> factory = new BasicMatrixValueFactory();
        FuncAnalysis = new ValueAnalysis<AggrValue<BasicMatrixValue>>(
                callgraph, Args.newInstance(inputValues) , factory);
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
    private static Program parseOrDie(String path){
        java.util.List<CompilationProblem> errors = new ArrayList<>();
        Program ast = Parse.parseMatlabFile(path, errors);
        if(!errors.isEmpty()){
            System.err.println("Parse error: " + Joiner.on("\n").join(errors));
            System.exit(1);
        }
        return ast;
    }
}
