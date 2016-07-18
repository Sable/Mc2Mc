package mc2mc.analysis;

import ast.*;
import mc2mc.mc2lib.BuildinList;
import mc2mc.mc2lib.CommonFunction;
import mc2mc.mc2lib.PrintMessage;
import mc2mc.mc2lib.UDFCheck;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tamerplus.analysis.ReachingDefinitions;
import natlab.tame.tir.*;
import natlab.tame.tir.analysis.TIRAbstractSimpleStructuralForwardAnalysis;
import natlab.tame.valueanalysis.IntraproceduralValueAnalysis;
import natlab.tame.valueanalysis.ValueFlowMap;
import natlab.tame.valueanalysis.ValueSet;
import natlab.tame.valueanalysis.aggrvalue.AggrValue;
import natlab.tame.valueanalysis.basicmatrix.BasicMatrixValue;
import natlab.tame.valueanalysis.components.shape.Shape;

import java.util.*;
import java.util.List;

/**
 * Created by wukefe on 6/6/16.
 */

public class TirAnalysisPropagateShape extends TIRAbstractSimpleStructuralForwardAnalysis<Map<String, PromotedShape>> {

    public TirAnalysisPropagateShape(ASTNode tree, Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> fValueMap){
        super(tree);
        localValueMap = fValueMap.get(tree);
        if(tree instanceof TIRForStmt) {
            iter = ((TIRForStmt)tree).getLoopVarName().getID();
            rExpr = (RangeExpr) ((TIRForStmt)tree).getAssignStmt().getRHS();
        }
        globalIfCond = new PromotedShape();
        globalIfCond.setB();
        transposeMap = new HashMap<>();
        // for i = low:inc:up --> [getLower, getIncr=null getUpper]
    }

    String iter;
    RangeExpr rExpr;
    ValueFlowMap<AggrValue<BasicMatrixValue>> localValueMap;


    public TirAnalysisPropagateShape(ASTNode tree, Map<ASTNode,
            ValueFlowMap<AggrValue<BasicMatrixValue>>> fValueMap,
                          List<PromotedShape> argPS,
                             AnalysisEngine engine) {
        super(tree);
        inputPS = argPS;
        fnode = (TIRFunction)tree;
        isFunction = true;
        localValueMap = fValueMap.get(tree);
        localUDMap = engine.getUDChainAnalysis().getChain();
        localRDef = engine.getReachingDefinitionsAnalysis();
        localDUMap = engine.getDUChainAnalysis().getChain();
        localOutFlow = engine.getReachingDefinitionsAnalysis().getOutFlowSets();
        localInFlow = engine.getReachingDefinitionsAnalysis().getInFlowSets();
        ifLock = false;
        hasTop = false;
        globalIfCond = new PromotedShape();
        globalIfCond.setB();
        transposeMap = new HashMap<>();
    }

    private boolean isFunction = false;
    private TIRFunction fnode = null;
    private List<PromotedShape>  inputPS = null;
    private List<PromotedShape> outputPS = null;
    private AnalysisEngine localEngine = null;
    public Map<TIRNode, Map<String, Set<TIRNode>>> localUDMap = null;
    public ReachingDefinitions localRDef = null;
    public Map<TIRNode, HashMap<String, HashSet<TIRNode>>> localDUMap = null;
    private boolean ifLock = false;
//    private boolean ifPBranch = false;
    private PromotedShape globalIfCond = null;
    public boolean hasTop;

    public boolean oldDebug = false; //old debug stmts, turn off
    static boolean debug = false;
    public Map<ASTNode, Map<String, Set<TIRNode>>> localOutFlow = null;
    public Map<ASTNode, Map<String, Set<TIRNode>>> localInFlow = null;
    public Map<ASTNode, String> transposeMap = null;

    @Override
    public Map<String, PromotedShape> merge(Map<String, PromotedShape> p1, Map<String, PromotedShape> p2) {
        Map<String, PromotedShape> res = new HashMap<>(p1);
        PrintMessage.See(" In function? " + (isFunction ? "yes" : "no"));
        for (String name : p2.keySet()) {
            if (p1.containsKey(name)) {
                PromotedShape ps = BuildinList.mergeTwo(p1.get(name), p2.get(name));
                if(!globalIfCond.isB()){
                    ps = BuildinList.mergeTwo(ps, globalIfCond);
                }
                res.put(name, ps); // update
//                if (!p1.get(name).equals(p2.get(name))) {
//                    PromotedShape p = p1.get(name);
//                    p.setT();
//                    res.put(name, p);
//                }
            }
            else res.put(name, p2.get(name));
        }
        PrintMessage.See("size of set: " + res.size());
        return res;
    }

    @Override
    public Map<String, PromotedShape> copy(Map<String, PromotedShape> p1) {
        return new HashMap<>(p1);
    }

    @Override
    public Map<String, PromotedShape> newInitialFlow() {
        if(isFunction){
            Map<String, PromotedShape> res = new HashMap<>();
            for(int i=0;i<inputPS.size();i++){
                res.put(fnode.getInputParam(i).getID(), inputPS.get(i)); //save input ps
            }
            return res;
        }
        return new HashMap<>();
    }

    @Override
    public void caseTIRCallStmt(TIRCallStmt node){
        saveNode(node);
//        PrintMessage.See("size in = " + getCurrentInSet().size() + "; size out = " + getCurrentOutSet().size());
    }

    @Override
    public void caseTIRCopyStmt(TIRCopyStmt node){
        saveNode(node);
    }

    @Override
    public void caseTIRArrayGetStmt(TIRArrayGetStmt node){
        saveNode(node);
    }

    @Override
    public void caseTIRArraySetStmt(TIRArraySetStmt node){
        saveNode(node);
    }

    @Override
    public void caseTIRAssignLiteralStmt(TIRAssignLiteralStmt node){
        saveNode(node);
    }

    @Override
    public void caseTIRIfStmt(TIRIfStmt node){
        // 1-level if-conversion
//        String cond = node.getConditionVarName().getID();
//        if(currentOutSet.get(cond).isP())
//            ifConversion(node);
        propagateShape(node);
    }



    private void propagateShape(TIRIfStmt node){
        boolean localLock = false;
        if(isFunction){
            String cond = node.getConditionVarName().getID(); //get condition name
            if(globalIfCond.isB()){ //remove ifPBranch
                localLock = true;
                globalIfCond = getPSbyName(cond);
            }
            else{
                PromotedShape currentIfCondPS = getPSbyName(cond);
                globalIfCond = BuildinList.mergeTwo(globalIfCond, currentIfCondPS);
            }
        }
        currentOutSet = copy(currentInSet);
        associateInSet(node, getCurrentInSet());
//        caseASTNode(node); // doesn't work (no merge)
        caseIfStmt(node); // merge as expected
        associateOutSet(node, getCurrentOutSet());
        if(localLock) {
            localLock = false;
            globalIfCond.setB();
        }
    }

    private void updateIfStmts(ASTNode node){
        Set<String> lhsNames = getLHSName(node);
        for(String n : lhsNames){
            PromotedShape p1 = getPSbyName(n);
            if(p1.isS()){
                p1.setP(globalIfCond);
            }
            else if(p1.equals(globalIfCond)){
                ; // do nothing
            }
            else {
                p1.setT();
            }
        }
    }

    public void saveNode(ASTNode node){
//        PrintMessage.See("hello world from " + this.toString());
        currentOutSet = copy(currentInSet);
//        initStmt(node);
//        propShape(node);
        initializeStmt(node);
        propagateStmt(node);
//        if(propagateStmt(node)){
//            updateIfStmts(node); //check all of them ?? may move it to later
//        }
//        else{
//            PrintMessage.Warning("Fail in propagateStmt - check [TirAnalysisVector]");
//        }
        associateInSet(node, getCurrentInSet());
        associateOutSet(node, getCurrentOutSet());
    }

    // colorVariable -> initializeStmt
    public void initializeStmt(ASTNode stmt){
        if(debug)
            PrintMessage.See("[from initializeStmt " + isFunction + "] " + stmt.getPrettyPrinted());
        Set<String> names = getAllName(stmt);
        if(stmt.getPrettyPrinted().trim().equals("row_start = csr_Ap(row);")) {
            int xx = 10;
        }
        for(String n : names){
//            PrintMessage.See("name " + n);
            if(!currentOutSet.containsKey(n)) {
                PromotedShape ps = new PromotedShape();
                if(!isFunction && n.equals(iter)){ // in a loop
                    ps.setP(rExpr);
                }
                else {
                    Shape x = getValueShape(n, localValueMap);
                    if(x!=null){
                        if(x.isScalar()) ps.setS();
                        else ps.setN(x);
                    }
                    else {
                        ps.setT();
                    }
                }
                currentOutSet.put(n, ps);  //set output
            }
        }
    }

    // processBasicStmt -> propagateStmt
    public void propagateStmt(ASTNode stmt) {
        PromotedShape ps = null;
        String psName = "unknown";
        Map<String, PromotedShape> psSet = new HashMap<>();
        if(stmt.getPrettyPrinted().contains("mc_t57 = conn(k, j);")){
            int xx=10;
        }
        if (stmt instanceof TIRCallStmt) {
            Expr lhs = ((AssignStmt) stmt).getLHS();
            Expr rhs = ((AssignStmt) stmt).getRHS();
            int tag = CommonFunction.decideStmt(stmt);
            String op = ((TIRCallStmt) stmt).getFunctionName().getID();
            TIRCommaSeparatedList args = ((TIRCallStmt) stmt).getArguments();
            if (tag == 1 || tag == 2) {
                // function call
                if (tag == 1) { // BIF
                    if (op.equals("mtimes")) { // special case
                        if (isScalar(args.getName(0).getID()) || isScalar(args.getName(1).getID()))
                            op = "times";
                    }
                    if(op.equals("transpose")){
                        int xx=10;
                    }
                    if (args.size() == 1 && BuildinList.isUnaryEBIF(op)) {
                        // unary eBIF
                        PromotedShape p1 = currentOutSet.get(args.getName(0).getID());
                        ps = BuildinList.unaryTable(p1);
                    } else if (args.size() == 2 && BuildinList.isBinaryEBIF(op)) {
                        // binary eBIF
                        PromotedShape p1 = currentOutSet.get(args.getName(0).getID());
                        PromotedShape p2 = currentOutSet.get(args.getName(1).getID());
                        if (isFunction && oldDebug) {
                            PrintMessage.See(stmt.getPrettyPrinted());
                            PrintMessage.See("0 arg " + args.getName(0).getID());
                            PrintMessage.See("1 arg " + args.getName(1).getID());
                        }
                        String lhsName = ((TIRCallStmt)stmt).getLValues().iterator().next();
                        if(lhsName.equals("mc_t168") && op.equals("times")){
                            int xx = 10;
                        }
                        ps = BuildinList.binaryTable(p1, p2);
                        if(ps.isT()){
                            int pos = p1.acceptDimTransp(p2);
                            if(pos!=0){
                                int doa = pos==1?0:1;
                                int dob = pos==1?1:0;
                                String tempVar = "mc_transp";
                                String tempStmt = tempVar + " = transpose(" +  args.getName(doa).getID() + ");";
                                String newString = lhs.getPrettyPrinted().trim()
                                        + " = " + op + "(" + tempVar + ", "
                                        +args.getName(dob).getID()
                                        +");";
                                ps.setP(pos==1?p1:p2); //revive
                                ps.changeLoc(pos==1?(p1.getLoc()==0?1:0):(p2.getLoc()==0?1:0)); //transpose
                                transposeMap.put(stmt, tempStmt+"\n"+newString);
                            }
                        }
                    }
                    else if(args.size() == 2 && op.equals("colon")){
                        PromotedShape p1 = currentOutSet.get(args.getName(0).getID());
                        PromotedShape p2 = currentOutSet.get(args.getName(1).getID());
                        ps = new PromotedShape();
                        if(p1.isS() && p2.isS()){
                            String lhsName = ((TIRCallStmt)stmt).getLValues().iterator().next();
                            Shape s = getValueShape(lhsName, localValueMap);
                            ps.setN(s);
                        }
                        else {
                            ps.setT();
                        }
                    }
                    else if(args.size()==1 && op.equals("transpose")){
                        PromotedShape p1 = currentOutSet.get(args.getName(0).getID());
                        ps = new PromotedShape();
                        if(p1.isS()){
                            ;
                        }
                        else if(p1.isP()){
                            if(p1.getDim()==1);
                            else if(p1.getDim()==2){
                                PrintMessage.See(args.getName(0).getID());
                                ps.setP(p1);
                                ps.changeLoc(p1.getLoc()==0?1:0);
                            }
                            else ps.setT();
                        }
                        else ps.setT();
                    }
                    else {
                        ps = new PromotedShape();
                        ps.setT();
                    }
                    psName = getLHSName(stmt).iterator().next();
                } else if (tag == 2) { // UDF analysis
                    boolean printFuncIR = false;
                    IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> calledAnalysis
                            = CommonFunction.getFunction(op);
                    TIRFunction tirFunc = calledAnalysis.getTree();
                    UDFCheck uCheck = new UDFCheck(tirFunc);
                    List<String> lhsNames = getLHSNameList(stmt);
                    if(uCheck.isValid()) {
                        AnalysisEngine engine = AnalysisEngine.forAST(tirFunc);
                        PrintMessage.delimiter();
                        PrintMessage.See("Entering UDF analysis", tirFunc.getName().getID());
                        PrintMessage.delimiter();
                        if (printFuncIR)
                            PrintMessage.See(tirFunc.getPrettyPrinted());
                        TirAnalysisPropagateShape tirUDF = new TirAnalysisPropagateShape(tirFunc,
                                CommonFunction.getValueAnalysis(tirFunc),
                                getInputShape(args),
                                engine);
                        tirUDF.analyze();
//                    tirUDF.printResult(); //print all outflow information
                        // maybe interprocedural analysis?
                        if (tirUDF.decideUDF()) {
                            tirUDF.convertIfStmt(); //
                            // ps = // return shape
                            psSet = tirUDF.getReturns(lhsNames, tirFunc.getOutputParamList());
//                        if (psSet.size() == 1) {
//                            String s0 = psSet.keySet().iterator().next();
//                            ps = psSet.get(s0);
//                            PrintMessage.See("returned " + s0 + " with ps " + ps.getPrettyPrinted());
//                        }
                        }
                        else {
                            CommonFunction.addToSkipSet(tirFunc);
                            psSet = setReturnsTop(lhsNames);
                        }
                    }
                    else {
                        psSet = setReturnsTop(lhsNames);
                    }
                }
            }
        } else if (stmt instanceof TIRArrayGetStmt) { // t = a(x);
            String arrayName = ((TIRArrayGetStmt) stmt).getArrayName().getID();
            TIRCommaSeparatedList arrayList = ((TIRArrayGetStmt) stmt).getIndices();
            String lhsName = ((MatrixExpr) ((TIRArrayGetStmt) stmt).getLHS()).getRow(0).getElement(0).getVarName();
            // array indexing
            ps = getArrayIndexShape(lhsName, arrayName, arrayList);
            psName = lhsName;
        } else if (stmt instanceof TIRArraySetStmt) { // a(x) = t;
            String arrayName = ((TIRArraySetStmt) stmt).getArrayName().getID();
            TIRCommaSeparatedList arrayList = ((TIRArraySetStmt) stmt).getIndices();
            String rhsName = ((NameExpr) ((TIRArraySetStmt) stmt).getRHS()).getName().getID();
            if(stmt.getPrettyPrinted().contains("sampleOut(k)")){
                int xx=10;
            }
            ps = getArrayIndexShape(rhsName, arrayName, arrayList);
            psName = ((TIRArraySetStmt) stmt).getLHS().getPrettyPrinted().trim(); //lhsName
            // check left and right either match
            PromotedShape rhsNamePS = currentOutSet.get(rhsName);
            if(!rhsNamePS.acceptArraySet(ps)){
                if(ps.acceptArrayTransp(rhsNamePS)){
                    String tempVar = "mc_transp_array";
                    String tempStmt = tempVar + " = transpose(" + rhsName + ");";
                    String newString = ((TIRArraySetStmt) stmt).getLHS().getPrettyPrinted().trim()+" = " + tempVar + ";";
                    transposeMap.put(stmt, tempStmt+"\n"+newString);
                }
                else {
                    ps.setT();
                }
            }
        } else if (stmt instanceof TIRCopyStmt) {
            Expr rhs = ((TIRCopyStmt) stmt).getRHS();
            Expr lhs = ((TIRCopyStmt) stmt).getLHS();
            String rhsName = ((NameExpr) rhs).getName().getID();
            String lhsName = ((NameExpr) lhs).getName().getID();
            if(stmt.getPrettyPrinted().trim().equals("mc_t189 = j;")){
                int xx=10;
            }
            ps = currentOutSet.get(rhsName);
            psName = lhsName;
        }
        if (ps != null) {
            psSet.put(psName, ps);
        }
        for (String s0 : psSet.keySet()) {
            PromotedShape ps0 = psSet.get(s0);
//            if (!globalIfCond.isB()) {
//                ps0 = BuildinList.mergeTwo(globalIfCond, ps0);
//            }
            if (!currentOutSet.containsKey(s0)
                    || !ps0.equals(currentOutSet.get(s0))) {
                currentOutSet.put(s0, ps0); // copy
            }
        }
    }

    public boolean isBasicStmt(ASTNode node){
        if(node instanceof TIRCallStmt)
            return true;
        else if(node instanceof TIRArrayGetStmt)
            return true;
        else if(node instanceof TIRArraySetStmt)
            return true;
        else if(node instanceof TIRCopyStmt)
            return true;
        return false;
    }

    private PromotedShape getArrayIndexShape(String lhsName, String arrayName, TIRCommaSeparatedList arrayList){
        int count = 0, pos = -1, c = 0;
        PromotedShape ps = null;
        for(Expr n : arrayList){
            if(n instanceof NameExpr) { // other: ast.ColonExpr
                String name = ((NameExpr) n).getVarName();
                if(isPSP(name)){
                    count++;
                    pos = c;
                }
//                if (currentOutSet.containsKey(name)) {
//                    if (currentOutSet.get(name).isP()) {
//                        count++;
//                        pos = c;
//                    }
//                }
            }
            c++;
        }
        if(arrayName.equals("reference")){
            int xx=10;
        }
        if(count == 1){
//            PromotedShape namePS = currentOutSet.get(arrayList.getName(pos).getID());
//            PromotedShape namePS = currentOutSet.get(lhsName);
            Shape nameShape = getValueShape(lhsName, localValueMap);
            int dim = arrayList.size();
            ps = new PromotedShape();
            if(nameShape != null) {
                ps.setP(nameShape, pos, dim);
            }
            else
                ps.setT();
//            PromotedShape lhsPS  = currentOutSet.get(lhsName);
//            lhsPS.setP(namePS, pos);
//            currentOutSet.put(lhsName, lhsPS); //save back
        }
        else if(count > 1){
            ps = new PromotedShape();
            ps.setT();
//            lhsPS  = currentOutSet.get(lhsName);
//            lhsPS.setT(); //set to Top
//            currentOutSet.put(lhsName, lhsPS);
        }
        else {
            ps  = currentOutSet.get(lhsName);
        }
        return ps;
    }

    private Set<String> getAllName(ASTNode node){
        Set<String> res = new HashSet<>();
        if(node instanceof AssignStmt){
            res.addAll(getAllName(((AssignStmt) node).getLHS(), false));
            res.addAll(getAllName(((AssignStmt) node).getRHS(), true));
        }
        else
            res.addAll(getAllName(node, true));
        return res;
    }

    private Set<String> getAllName(ASTNode node, boolean check){
        int len = node.getNumChild();
        Set<String> res = new HashSet<>();
        if(node instanceof ast.Name){
            String str = ((Name) node).getID();
            if(check && !CommonFunction.isBuildinOrUDF(str)) //RHS or all expr
                res.add(str);
            else if(!check) // LHS
                res.add(str);
        }
        else {
            for (int i = 0; i < len; i++) {
                res.addAll(getAllName(node.getChild(i), check));
            }
        }
        return res;
    }

    private boolean isScalar(String var){
        Shape p1 = getValueShape(var, localValueMap);
        return p1 == null?false:p1.isScalar();
    }

    private Shape getValueShape(String name, ValueFlowMap<AggrValue<BasicMatrixValue>> valueMap){
        if(valueMap.containsKey(name)){
            ValueSet<AggrValue<BasicMatrixValue>> currentValue =valueMap.get(name);
            if(currentValue.size() == 1){
                for(AggrValue<BasicMatrixValue> one : currentValue){
                    return ((BasicMatrixValue)one).getShape();  //get one
                }
            }
            return null;
        }
        return null;
    }

    // UDF analysis

    private java.util.List<PromotedShape> getInputShape(TIRCommaSeparatedList args){
        java.util.List<PromotedShape> res = new ArrayList<>();
        for(int i=0;i<args.size();i++){
            String nameA = args.getName(i).getID();
            res.add(currentOutSet.get(nameA));
        }
        return res;
    }

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
        if(ps.isT())
            return false;
        return true;
    }

    public Map<String, PromotedShape> getReturns(List<String> lhsNames, ast.List<Name> outputArgs){
//        printList(fnode.getInputParamList(),  "input  parameter");
//        printList(fnode.getOutputParamList(), "output parameter");
        Map<String, PromotedShape> outputP = getPSbyName(fnode.getOutputParamList());
        Map<String, PromotedShape> newP = new HashMap<>();
        for(int i=0;i< lhsNames.size(); i++){
            newP.put(lhsNames.get(i), outputP.get(outputArgs.getChild(i).getID()));
        }
        return newP;
//        return getPSbyName(fnode.getOutputParamList());
    }

    public Map<String, PromotedShape> setReturnsTop(List<String> lhsNames){
        // UDF fails
        Map<String, PromotedShape> res = new HashMap<>();
        for(int i=0;i<lhsNames.size();i++){
            PromotedShape ps = new PromotedShape();
            ps.setT();
            res.put(lhsNames.get(i), ps);
        }
//        for(Name n : node.getOutputParamList()){
//            PromotedShape ps = new PromotedShape();
//            ps.setT();
//            res.put(n.getID(), ps);
//        }
        return res;
    }

    public void checkCurrentOutFlow(){
        if(!isFunction || hasTop) return;
        Map<String, PromotedShape> curOutFlow = getCurrentOutSet();
        for(String s0:curOutFlow.keySet()){
            if(curOutFlow.get(s0).isT()) {
                hasTop = true;
                break;
            }
        }
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

    public PromotedShape getPSbyName(String name){
        return currentOutSet.get(name);
    }

    public boolean isPSS(String name){
        PromotedShape p1 = getPSbyName(name);
        return (p1==null?false:p1.isS());
    }

    public boolean isPSP(String name){
        PromotedShape p1 = getPSbyName(name);
        return (p1==null?false:p1.isP());
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

    public void convertIfStmt(){
        convertIfStmt(fnode);
    }

    ast.List ifThenList = null;
    ast.List ifElseList = null; // do it later
    public void convertIfStmt(ASTNode node){
        Map<ASTNode, List<String>> newIfStmt = new HashMap<>();
        findInnermostIfStmt(node, newIfStmt);
        List<String> newFn = CommonFunction.transformFunction(node, newIfStmt, transposeMap, 1); //generateNewFn
        PrintMessage.See(newIfStmt.size()+"", "newIfStmt");
//        PrintMessage.arrayList(newFn);
        CommonFunction.setFnHashMap(node, newFn);
    }

    private void findInnermostIfStmt(ASTNode node, Map<ASTNode, List<String>> newIfStmt){
        int len = node.getNumChild();
        boolean isEnter = false;
        for(int i=0;i<len;i++){
            ASTNode currentNode = node.getChild(i);
            if(currentNode instanceof TIRIfStmt){
                isEnter = true;
            }
            findInnermostIfStmt(currentNode, newIfStmt);
        }

        if(node instanceof TIRIfStmt && !isEnter){
            ifThenList = new ast.List();
            ifElseList = new ast.List();
            List<String> newContent = new ArrayList<>();
            ifConversion((TIRIfStmt)node, newContent);
            newIfStmt.put(node, newContent);
        }
    }

//    private List<String> generateNewFn(ASTNode node, Map<ASTNode, List<String>> newIfStmt){
//        List<String> rtn = new ArrayList<>();


//        if(node instanceof TIRFunction){
//            for(Stmt s : ((TIRFunction) node).getStmtList()){
//                rtn.addAll(generateNewFn(s, newIfStmt));
//            }
//        }
//        else if(node instanceof AssignStmt){
//            rtn.add(node.getPrettyPrinted().trim());
//        }
//        else
//        if(node instanceof TIRIfStmt){
//            if(newIfStmt.containsKey(node)){
//                rtn.addAll(newIfStmt.get(node));
//            }
//            else {
//                rtn.add("if " + ((TIRIfStmt) node).getConditionVarName().getID());
//                for (Stmt s : ((TIRIfStmt) node).getIfStatements()) {
//                    rtn.addAll(generateNewFn(s, newIfStmt));
//                }
//                rtn.add("else");
//                if(((TIRIfStmt) node).hasElseBlock()){
//                    for(Stmt s : ((TIRIfStmt) node).getElseStatements()){
//                        rtn.addAll(generateNewFn(s, newIfStmt));
//                    }
//                }
//                rtn.add("end");
//            }
//        }
//        else
//            rtn.addAll(CommonFunction.generateCommonNode(node));
//        return rtn;
//    }
//
//    private boolean ifConversion(TIRIfStmt node, boolean f){
//        PrintMessage.See(node.getConditionVarName().getID(), "if conv name");
//        PrintMessage.See(outFlowSets.get(node).get(node.getConditionVarName().getID()).isP()?"true":"false");
//        if(f==true || outFlowSets.get(node).get(node.getConditionVarName().getID()).isP()){
//            nextIfStmt(node.getIfStatements()  , true);
//            nextIfStmt(node.getElseStatements(), true);
////            doIfConversion(node);
//            return doIfConversion2(node);
//        }
//        else {
//            nextIfStmt(node.getIfStatements()  , f);
//            nextIfStmt(node.getElseStatements(), f);
//        }
//        return false;
//    }
//
//    public void nextIfStmt(ASTNode node, boolean f){
//        int num = node.getNumChild();
//        for(int i=0;i<num;i++){
//            ASTNode currentNode = node.getChild(i);
//            if(currentNode instanceof TIRIfStmt)
//                ifConversion((TIRIfStmt)currentNode, f);
//        }
//    }

    ////// if conversion v2
//    private boolean doIfConversion2(TIRIfStmt node){
    private boolean ifConversion(TIRIfStmt node, List<String> newContent) {
        Map<String, String> mapThen = new HashMap<>();
        Map<String, String> mapElse = new HashMap<>();
        Map<String, String> mapSave = new HashMap<>();
        String thenCond = node.getConditionVarName().getID();
        Map<ASTNode, Boolean> blockThen = mapIfBlock(node.getIfStatements());
        Map<ASTNode, Boolean> blockElse = mapIfBlock(node.getElseStatements());
        PrintMessage.delimiter();
        PrintMessage.See("Print if conversion", "AnalysisPropagateShape");
        gen(newContent, "thenCond", thenCond);
        gen(newContent, "elseCond", "not(thenCond)");
        Map<String, Set<TIRNode>> ifOutFlowSet = localOutFlow.get(node);
        Map<String, PromotedShape> ifInFlowSet = getInFlowSets().get(node);
        Set<String> nameSet = getNameSet(node);
        // save
        for(String n : nameSet){
            if(ifInFlowSet.containsKey(n)){
                String saveT = genTempSave();
                gen(newContent, saveT, n);
                mapSave.put(n, saveT);
            }
        }
        for(Stmt s : node.getIfStatements()){
            if(s instanceof AssignStmt){
                Set<String> lhsName = getLHSName(s);
                if(lhsName.size() != 1){
                    PrintMessage.Warning("Left hand side variable in the IF block must be 1.");
                    return false;
                }
                String lhs = lhsName.iterator().next(); // lhs name0
                Set<TIRNode> defSet = ifOutFlowSet.get(lhs);
//                if(isTempLike(lhs,s, blockThen)){
                if(defSet.size() == 1) {
                    String t0 = genTemp();
                    gen(newContent, t0, genRHS(s));
                    gen(newContent, lhs, "times(thenCond, " + t0 + ")");
                }
                else {
                    String t1 = genTemp();
                    String t2 = genTemp();
                    gen(newContent, t1, genRHS(s));
                    gen(newContent, t2, "times(thenCond, " + t1 + ")");
                    mapThen.put(lhs, t2);
                }
            }
            else return  false;
        }
        // restore temp save
        for(String n : mapSave.keySet()){
            gen(newContent, n, mapSave.get(n));
        }
        if(node.hasElseBlock()){
            for(Stmt s : node.getElseStatements()){
                if(s instanceof AssignStmt){
                    Set<String> lhsName = getLHSName(s);
                    if(lhsName.size() != 1){
                        PrintMessage.Warning("Left hand side variable in the ELSE block must be 1.");
                        return false;
                    }
                    String lhs = lhsName.iterator().next(); // lhs name0
                    Set<TIRNode> defSet = ifOutFlowSet.get(lhs);
//                    if(isTempLike(lhs,s,blockElse)){
                    if(defSet.size()==1){
                        String t3 = genTemp();
                        gen(newContent, t3, genRHS(s));
                        gen(newContent, lhs, "times(elseCond, " + t3 + ")");
                    }
                    else if(mapThen.containsKey(lhs)){
                        String t4 = genTemp();
                        String t5 = genTemp();
                        gen(newContent, t4, genRHS(s));
                        gen(newContent, t5, "times(elseCond, " + t4 + ")");
                        gen(newContent, lhs,"plus(" + t5 + ", " + mapThen.get(lhs) + ")");
                        mapThen.remove(lhs);
                    }
                    else {
                        String t6 = genTemp();
                        String t7 = genTemp();
                        gen(newContent, t6, genRHS(s));
                        gen(newContent, t7, "times(elseCond, " + t6 + ")");
                        mapElse.put(lhs, t7);
                    }
                }
                else return  false;
            }
        }
        if(mapThen.size() > 0){
            for(String n : mapThen.keySet()){
                String t8 = genTemp();
                gen(newContent, t8, "times(" + n + ", elseCond)");
                gen(newContent, n, "plus(" + t8 + ", " + mapThen.get(n) + ")");
            }
        }
        if(mapElse.size() > 0){
            for(String n : mapElse.keySet()){
                String t9 = genTemp();
                gen(newContent, t9, "times(" + n + ", thenCond)");
                gen(newContent, n, "plus(" + t9 + ", " + mapElse.get(n) + ")");
            }
        }
        return true;
    }

    private Set<String> getNameSet(TIRIfStmt node){
        Set<String> rtn = new HashSet<>();
        for(Stmt s : node.getIfStatements()){
            if(s instanceof AssignStmt){
                rtn.addAll(getLHSName(s));
            }
        }
        if(node.hasElseBlock()){
            for(Stmt s : node.getElseStatements()){
                if(s instanceof AssignStmt){
                    rtn.addAll(getLHSName(s));
                }
            }
        }
        return rtn;
    }

    private Map<ASTNode, Boolean> mapIfBlock(TIRStatementList body){
        Map<ASTNode, Boolean> rtn = new HashMap<>();
        for(Stmt s : body){
            rtn.put(s, true);
        }
        return rtn;
    }

    private boolean isTempLike(String varName, ASTNode varNode, Map<ASTNode, Boolean> block){
        if(localDUMap.get(varNode)==null){
            return true;
        }
        HashSet<TIRNode> defs = localDUMap.get(varNode).get(varName);
        for(TIRNode t : defs){
            if(!block.containsKey(t)) return false;
        }
        return true;
    }

    private int tempIndex = 0;
    private String genTemp(){
        return "new_t"+tempIndex++;
    }
    private String genTempSave() {return "save_t"+tempIndex++; }

    private String genRHS(ASTNode node){
        String rtn = "";
        if(node instanceof AssignStmt)
            rtn = ((AssignStmt) node).getRHS().getPrettyPrinted();
        if(node instanceof TIRCallStmt){
            String op = ((TIRCallStmt) node).getFunctionName().getID();
            if(op.equals("mtimes")){
                if(isScalar(((TIRCallStmt) node).getArguments().getName(0).getID())
                || isScalar(((TIRCallStmt) node).getArguments().getName(1).getID()))
                    rtn = rtn.replace("mtimes", "times"); //update
            }
        }
        return rtn;
    }

//    private void gen(String lhs, String rhs){
//        PrintMessage.See(lhs.trim() + " = " + rhs.trim() + ";");
//    }

    private void gen(List<String> newContent, String lhs, String rhs){
        newContent.add(lhs.trim() + " = " + rhs.trim() + ";");
    }

    ////// if conversion v1
    private void doIfConversion(TIRIfStmt node){
        List<ASTNode> thenAssignStmt = collectAssign(node.getIfStatements());
        List<ASTNode> elseAssignStmt = new ArrayList<>();
        Set<ASTNode> thenNoTempVar  = getNoTempSet(thenAssignStmt);
        Set<ASTNode> elseNoTempVar  = new HashSet<>();
        String thenCond = node.getConditionVarName().getID();
        String elseCond = "~" + thenCond;
        if(node.hasElseBlock()){
            elseAssignStmt = collectAssign(node.getElseStatements());
            elseNoTempVar = getNoTempSet(elseAssignStmt);
        }
        Map<ASTNode, ASTNode> linkA2B = new HashMap<>();
        Map<ASTNode, ASTNode> linkB2A = new HashMap<>();
        setUnionByLHS(thenNoTempVar, elseNoTempVar, linkA2B, linkB2A);
        Map<String, Set<TIRNode>> inFlowDef  = localRDef.getInFlowSets().get(node);
//        Map<String, Set<TIRNode>> outFlowDef = localRDef.getOutFlowSets().get(node);
        genTempVarStmt(thenAssignStmt, thenNoTempVar, thenCond, inFlowDef);
        genTempVarStmt(elseAssignStmt, elseNoTempVar, elseCond, inFlowDef);
        genNoTempVar(linkA2B,linkB2A,thenCond,elseCond);
    }

    private void genTempVarStmt(List<ASTNode> stmts, Set<ASTNode> noTempVarStmt, String cond, Map<String, Set<TIRNode>> inFlow){
        for(ASTNode a : stmts){
            if(a instanceof AssignStmt){
                if(!noTempVarStmt.contains(a)){
                    Set<String> names = getLHSName(a);
                    for(String lhsName : names){
                        if(inFlow.containsKey(lhsName)){ //with inflow
                            PrintMessage.genCode(lhsName + " = times(" + cond + "," +
                                    ((AssignStmt) a).getRHS().getPrettyPrinted().trim() + ");");
                        }
                        else{
                            genPattern1(lhsName, ((AssignStmt) a).getRHS().getPrettyPrinted(), cond);
//                            PrintMessage.genCode(lhsName + " = times(" + cond + "," +
//                                    ((AssignStmt) a).getRHS().getPrettyPrinted().trim() + ") + " + lhsName +";");
                        }
                    }
                }
            }
        }
    }

    private void genNoTempVar(Map<ASTNode, ASTNode> linkA2B, Map<ASTNode, ASTNode> linkB2A,
                              String condThen, String condElse){
        for(ASTNode a : linkA2B.keySet()){
            ASTNode b = linkA2B.get(a);
            Set<String> namesA = getLHSName(a);
            if(namesA.size()!=1) continue; //
            String namesa = namesA.iterator().next();
            if(b == null){
                genPattern1(namesa, ((AssignStmt)a).getRHS().getPrettyPrinted(),condThen);
            }
            else {
                genPattern2(namesa, ((AssignStmt)a).getRHS().getPrettyPrinted(),
                        ((AssignStmt)b).getRHS().getPrettyPrinted(), condThen, condElse);
            }
        }
        for(ASTNode b : linkB2A.keySet()){
            ASTNode a = linkB2A.get(b);
            Set<String> namesB = getLHSName(b);
            if(namesB.size()!=1) continue; //
            String namesb = namesB.iterator().next();
            if(a==null){
                genPattern1(namesb, ((AssignStmt)b).getRHS().getPrettyPrinted(),condElse);
            }
        }
    }

    private void genPattern1(String v, String rhs, String cond){
        PrintMessage.genCode(v + " = times(" + cond + "," + rhs.trim() + ") + " + v + "; //pattern1");
    }

    private void genPattern2(String v, String rhs1, String rhs2, String condThen, String condElse){
        PrintMessage.genCode(v + " = times(" + condThen + "," + rhs1 + ") + times(" + condElse + "," + rhs2 + "); //pattern2");
    }

    private Set<String> getLHSName(ASTNode node){
        Set<String> names = new HashSet<>();
        if(node instanceof AssignStmt){
            Expr tempLHS = ((AssignStmt) node).getLHS();
            if(tempLHS instanceof NameExpr){
                names.add(((NameExpr) tempLHS).getName().getID());
            }
            else if(tempLHS instanceof MatrixExpr){
                ast.List<Expr> nameList = ((MatrixExpr)tempLHS).getRow(0).getElementList();
                for (Expr n : nameList) {
                    if (n instanceof NameExpr) {
                        names.add(((NameExpr) n).getName().getID());
                    }
                }
            }
        }
        return names;
    }

    private List<String> getLHSNameList(ASTNode node){
        List<String> names = new ArrayList<>();
        if(node instanceof AssignStmt){
            Expr tempLHS = ((AssignStmt) node).getLHS();
            if(tempLHS instanceof NameExpr){
                names.add(((NameExpr) tempLHS).getName().getID());
            }
            else if(tempLHS instanceof MatrixExpr){
                ast.List<Expr> nameList = ((MatrixExpr)tempLHS).getRow(0).getElementList();
                for (Expr n : nameList) {
                    if (n instanceof NameExpr) {
                        names.add(((NameExpr) n).getName().getID());
                    }
                }
            }
        }
        return names;
    }


    private void setUnionByLHS(Set<ASTNode> s1, Set<ASTNode> s2,
                               Map<ASTNode, ASTNode> mapA2B, Map<ASTNode, ASTNode> mapB2A){
        Map<String, ASTNode> ss1 = getLHSString(s1);
        Map<String, ASTNode> ss2 = getLHSString(s2);
        for(String n : ss1.keySet()){
            if(ss2.containsKey(n)){
                mapA2B.put(ss1.get(n), ss2.get(n));
            }
            else {
                mapA2B.put(ss1.get(n), null);
            }
        }
        for(String n : ss2.keySet()){
            if(!ss1.containsKey(n)){
                mapB2A.put(ss2.get(n), null);
            }
        }
    }

    private Map<String, ASTNode> getLHSString(Set<ASTNode> s){
        Map<String, ASTNode> rtn = new HashMap<>();
        for(ASTNode t : s){
            if(t instanceof AssignStmt){
                rtn.put(((AssignStmt) t).getLHS().getPrettyPrinted().trim(), t);
            }
        }
        return rtn;
    }

    private Set<ASTNode> getNoTempSet(List<ASTNode> blockAssignStmt){
        Set<ASTNode> noTempSet = new HashSet<>();
        for(ASTNode a : blockAssignStmt){
            Map<String, Set<TIRNode>> defs = localUDMap.get(a);
            if(defs!=null) {
                for (String s : defs.keySet()) {
                    Set<TIRNode> defSet = defs.get(s);
                    if(defSet.size()==1){
                        ASTNode d0 = (ASTNode)defSet.iterator().next();
                        if(!blockAssignStmt.contains(d0)){
                            noTempSet.add(a);
                        }
                    }
                    else noTempSet.add(a);
                }
            }
        }
        return noTempSet;
    }

    private List<ASTNode> collectAssign(TIRStatementList body){
        List<ASTNode> rtn = new ArrayList<>();
        for(Stmt s : body){
            if(s instanceof AssignStmt){
                rtn.add(s);
            }
        }
        return rtn;
    }

    ////// if conversion v1


    /*
    // 1-level if conversion
    public void ifConversion(TIRIfStmt node){
        PrintMessage.See("start if conversion");
        Set<String> allWrite = new HashSet<>();
        Set<String> ifNode   = new HashSet<>();
        Set<String> elseNode = new HashSet<>();
        Set<TIRNode> ifTirNode   = new HashSet<>();
        Set<TIRNode> elseTirNode = new HashSet<>();

        for(ast.Stmt x : node.getIfStatements()){
            ifNode.addAll(saveWrite(x));
            ifTirNode.add((TIRNode)x); //save to list
        }
        if(node.hasElseBlock()){
            for(ast.Stmt x : node.getElseStatements()){
                elseNode.addAll(saveWrite(x));
                elseTirNode.add((TIRNode)x);
                for(String s : ifNode){
                    if(elseNode.contains(s)){
                        allWrite.add(s);
                    }
                }
            }
        }
        else {
            allWrite.addAll(ifNode);
        }

        Map<String, PromotedShape> outIfNode = getOutFlowSets().get(node);
        Set<String> canWrite = new HashSet<>();
        // check output set
        for(String s : allWrite){
            if(outIfNode.get(s).isP())
                canWrite.add(s);
        }

        if(canWrite.size() > 0){

            Map<String, Boolean> flag = new HashMap<>();
            for(String s : canWrite) flag.put(s, true);

            checkStmtInBlock(ifTirNode, elseTirNode, canWrite, flag);
            if(node.hasElseBlock()){
                checkStmtInBlock(elseTirNode, ifTirNode, canWrite, flag);
            }
            // list[0]: true  block
            // list[1]: false block
            Map<String, List<TIRNode>> commonSet = new HashMap<>();
            for(String s : canWrite) commonSet.put(s, new ArrayList<>());
            Set<TIRNode> ifVecSet = findAllDefs(ifTirNode, flag, commonSet);
            Set<TIRNode> elseVecSet = findAllDefs(elseTirNode, flag, commonSet);
            printTIRSet(commonSet, "Common write");
            printTIRSet(ifVecSet, "if vectorizable set");
            printTIRSet(elseVecSet, "else vectorizable set");
        }
    }

    // save all dependent
    public boolean checkStmtInBlock(Set<TIRNode> oneSet, Set<TIRNode> otherSet,
                                        Set<String> canWrite, Map<String, Boolean> flag){
        for(TIRNode t : oneSet){
            Set<String> tempWrite = saveWrite((ASTNode) t);
            for(String s : tempWrite){
                if(flag.containsKey(s) && flag.get(s) && canWrite.contains(s)){
                    flag.put(s, checkAllDefs(t, oneSet, otherSet));
                }
            }
        }
        return true;
    }

    public boolean checkAllDefs(TIRNode node, Set<TIRNode> oneSet, Set<TIRNode> otherSet){
        if(localUDMap.get(node)!=null) {
            for (String s : localUDMap.get(node).keySet()) {
                Set<TIRNode> defSet = localUDMap.get(node).get(s);
                for (TIRNode t : defSet) {
                    if (otherSet.contains(t)) {
                        return false;
                    } else if (oneSet.contains(t)) {
                        if (!checkAllDefs(t, oneSet, otherSet))
                            return false;
                    }
                }
            }
        }
        return true;
    }*/

    /*
    fetch all possible stmt
     */
    public Set<TIRNode> findAllDefs(Set<TIRNode> oneSet,
                                    Map<String, Boolean> flag,
                                    Map<String, List<TIRNode>> commonSet){
        Set<TIRNode> res = new HashSet<>();
        for(TIRNode t : oneSet){
            Set<String> tempWrite = saveWrite((ASTNode) t);
            for(String w : tempWrite){
                if(flag.containsKey(w) && flag.get(w)){
                    // add t into common set
                    List<TIRNode> tempList = commonSet.get(w);
                    tempList.add(t);
                    commonSet.put(w, tempList);
                    // add other dependent stmts
                    res.addAll(findOtherDefs(t, oneSet));  // add other stmt within block
                }
            }
        }
        return res;
    }

    public Set<TIRNode> findOtherDefs(TIRNode node, Set<TIRNode> oneSet){
        Set<TIRNode> res = new HashSet<>();
        if(localUDMap.get(node)!=null) {
            for (String s : localUDMap.get(node).keySet()) {
                Set<TIRNode> defSet = localUDMap.get(node).get(s);
                for (TIRNode t : defSet) {
                    if (oneSet.contains(t)) { // within block set
                        res.add(t);
                    }
                }
            }
        }
        return res;
    }

    public Set<String> saveWrite(ASTNode x){
        Set<String> write = new HashSet<>();
        if(x instanceof AssignStmt){
            Expr lhs = ((AssignStmt) x).getLHS();
            if(lhs instanceof MatrixExpr
                    && ((MatrixExpr) lhs).getNumRow() == 1){
                write.add(((MatrixExpr) lhs).getRow(0).getElement(0).getVarName());
            }
            else if(lhs instanceof NameExpr){
                write.add(((NameExpr) lhs).getName().getID());
            }
        }
        return write;
    }

    public void printTIRSet(Map<String, List<TIRNode>> x, String word){
        PrintMessage.See(word);
        for(String s : x.keySet()){
            List<TIRNode> tempList = x.get(s);
            for(TIRNode t : tempList){
                PrintMessage.See(((ASTNode)t).getPrettyPrinted());
            }
        }
        PrintMessage.delimiter();
    }

    public void printTIRSet(Set<TIRNode> x, String word){
        PrintMessage.See(word);
        printTIRSet(x);
    }

    public void printTIRSet(Set<TIRNode> x){
        PrintMessage.delimiter();
        for(TIRNode t : x){
            PrintMessage.See(((ASTNode)t).getPrettyPrinted().trim());
        }
        PrintMessage.delimiter();
    }

    // print the result of analysis
    public void printResult(){
        PrintMessage.See("printResult: ifstmt");
        Map<ASTNode, Map<String, PromotedShape>> x = getOutFlowSets();
        for(ASTNode a : x.keySet()){
            if(a instanceof TIRIfStmt){
                Map<String, PromotedShape> xValue = x.get(a);
                for(String s : xValue.keySet()){
                    PrintMessage.See("string: " + s + "; " + xValue.get(s).getPrettyPrinted());
                }
            }
        }
    }

}
