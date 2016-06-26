package mc2mc.analysis;

import ast.*;
import mc2mc.mc2lib.BuildinList;
import mc2mc.mc2lib.CommonFunction;
import mc2mc.mc2lib.PrintMessage;
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

public class TirAnalysisVector extends TIRAbstractSimpleStructuralForwardAnalysis<Map<String, PromotedShape>> {

    public TirAnalysisVector(ASTNode tree, Map<ASTNode, ValueFlowMap<AggrValue<BasicMatrixValue>>> fValueMap){
        super(tree);
        localValueMap = fValueMap.get(tree);
        if(tree instanceof TIRForStmt) {
            iter = ((TIRForStmt)tree).getLoopVarName().getID();
            rExpr = (RangeExpr) ((TIRForStmt)tree).getAssignStmt().getRHS();
        }
        // for i = low:inc:up --> [getLower, getIncr=null getUpper]
    }

    String iter;
    RangeExpr rExpr;
    ValueFlowMap<AggrValue<BasicMatrixValue>> localValueMap;


    public TirAnalysisVector(ASTNode tree, Map<ASTNode,
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
        ifLock = false;
        ifPBranch = false;
        ifCondPS = null;
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
    private boolean ifPBranch = false;
    private PromotedShape ifCondPS = null;

    public boolean oldDebug = false; //old debug stmts, turn off

    @Override
    public Map<String, PromotedShape> merge(Map<String, PromotedShape> p1, Map<String, PromotedShape> p2) {
        Map<String, PromotedShape> res = new HashMap<>(p1);
        PrintMessage.See(" In function? " + (isFunction ? "no" : "yes"));
        for (String name : p2.keySet()) {
            if (p1.containsKey(name)) {
                if (!p1.get(name).equals(p2.get(name))) {
                    PromotedShape p = p1.get(name);
                    p.setT();
                    res.put(name, p);
                }
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
        boolean localLock = false;
        if(isFunction){
            String cond = node.getConditionVarName().getID(); //get condition name
            if(!ifPBranch && isPSS(cond)){
                localLock = true;
                ifPBranch = true;
                ifCondPS = getPSbyName(cond);
            }
            else if(ifPBranch){
                PromotedShape currentIfCondPS = getPSbyName(cond);
                if(currentIfCondPS.isS() || currentIfCondPS.equals(ifCondPS)){
                    ; // do nothing
                }
                else {
                    ifCondPS.setT();
                }
            }
        }
        currentOutSet = copy(currentInSet);
        associateInSet(node, getCurrentInSet());
//        caseASTNode(node); // doesn't work (no merge)
        caseIfStmt(node); // merge as expected
        associateOutSet(node, getCurrentOutSet());
        if(localLock) {
            ifPBranch = false;
            ifCondPS = null;
        }
    }

    private void updateIfStmts(ASTNode node){
        Set<String> lhsNames = getLHSName(node);
        for(String n : lhsNames){
            PromotedShape p1 = getPSbyName(n);
            if(p1.isS()){
                p1.setP(ifCondPS);
            }
            else if(p1.equals(ifCondPS)){
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
        colorVariable(node);
        if(processBasicStmt(node)){
            if(ifPBranch)
                updateIfStmts(node);
        }
        else{
            PrintMessage.Warning("Fail in processBasicStmt - check [TirAnalysisVector]");
        }
        associateInSet(node, getCurrentInSet());
        associateOutSet(node, getCurrentOutSet());
    }

    public void colorVariable(ASTNode stmt){
        PrintMessage.See("[from colorVariable " + isFunction + "] " + stmt.getPrettyPrinted());
        Set<String> names = getAllName(stmt);
        for(String n : names){
//            PrintMessage.See("name " + n);
            if(!currentOutSet.containsKey(n)) {
                PromotedShape ps = new PromotedShape();
                if(!isFunction && n.equals(iter)){
                    ps.setP(rExpr, 0);
                }
                else {
                    Shape x = getValueShape(n, localValueMap);
                    if(x!=null){
                        if(x.isScalar()) ps.setS();
                        else ps.setN();
                    }
                    else ps.setT();
                }
                currentOutSet.put(n, ps);  //set output
            }
        }
    }

    public boolean processBasicStmt(ASTNode stmt){
        if(stmt instanceof TIRCallStmt){
            Expr lhs = ((AssignStmt) stmt).getLHS();
            Expr rhs = ((AssignStmt) stmt).getRHS();
            int tag = CommonFunction.decideStmt(stmt);
            String op = ((TIRCallStmt)stmt).getFunctionName().getID();
            TIRCommaSeparatedList args = ((TIRCallStmt)stmt).getArguments();
            if(tag==1 || tag==2){
                // function call
                PromotedShape ps = null;
                if(tag == 1) {
                    if(op.equals("mtimes")){ // special case
                        if(isScalar(args.getName(0).getID()) || isScalar(args.getName(1).getID()))
                            op = "times";
                    }
                    if (args.size() == 1 && BuildinList.isUnaryEBIF(op)) {
                        // unary eBIF
                        PromotedShape p1 = currentOutSet.get(args.getName(0).getID());
                        ps = BuildinList.unaryTable(p1);
                    } else if (args.size() == 2 && BuildinList.isBinaryEBIF(op)){
                        // binary eBIF
                        PromotedShape p1 = currentOutSet.get(args.getName(0).getID());
                        PromotedShape p2 = currentOutSet.get(args.getName(1).getID());
                        if(isFunction && oldDebug){
                            PrintMessage.See(stmt.getPrettyPrinted());
                            PrintMessage.See("0 arg " + args.getName(0).getID());
                            PrintMessage.See("1 arg " + args.getName(1).getID());
                        }
                        ps = BuildinList.binaryTable(p1, p2);
                    }
                }
                else if(tag == 2){
                    // UDF analysis
                    boolean printFuncIR = false;
                    IntraproceduralValueAnalysis<AggrValue<BasicMatrixValue>> calledAnalysis
                            = CommonFunction.getFunction(op);
                    TIRFunction tirFunc = calledAnalysis.getTree();
                    AnalysisEngine engine = AnalysisEngine.forAST(tirFunc);
                    PrintMessage.delimiter();
                    PrintMessage.See("Entering UDF analysis", tirFunc.getName().getID());
                    PrintMessage.delimiter();
                    if(printFuncIR)
                        PrintMessage.See(tirFunc.getPrettyPrinted());
                    TirAnalysisVector tirUDF = new TirAnalysisVector(tirFunc,
                            CommonFunction.getValueAnalysis(tirFunc),
                            getInputShape(args),
                            engine);
                    tirUDF.analyze();
//                    tirUDF.printResult(); //print all outflow information
                    tirUDF.convertIfStmt(); //
                    // maybe interprocedural analysis?
                    if(tirUDF.decideUDF()){
                        // ps = // return shape
                        Map<String, PromotedShape> res = tirUDF.getReturns();
                        if(res.size() == 1){
                            for(String s : res.keySet()){
                                ps = res.get(s);
                                PrintMessage.See("returned " + s + " with ps " + ps.getPrettyPrinted());
                            }
                        }
                    }
                }
                String lhsName = ((MatrixExpr)lhs).getRow(0).getElement(0).getVarName();
                if(ps!=null && (!currentOutSet.containsKey(lhsName)
                        || !ps.equals(currentOutSet.get(lhsName)))) {
                    // TODO: 6/19/16 what if the return ps is different? 
                    currentOutSet.put(lhsName, ps);
                }
            }
        }
        else if(stmt instanceof TIRArrayGetStmt){ // t = a(x);
            String arrayName = ((TIRArrayGetStmt) stmt).getArrayName().getID();
            TIRCommaSeparatedList arrayList = ((TIRArrayGetStmt) stmt).getIndices();
            String lhsName = ((MatrixExpr)((TIRArrayGetStmt) stmt).getLHS()).getRow(0).getElement(0).getVarName();
            // array indexing
            getArrayIndexShape(lhsName, arrayName, arrayList);
        }
        else if(stmt instanceof TIRArraySetStmt){ // a(x) = t;
            String arrayName = ((TIRArraySetStmt) stmt).getArrayName().getID();
            TIRCommaSeparatedList arrayList = ((TIRArraySetStmt) stmt).getIndices();
            String rhsName = ((NameExpr)((TIRArraySetStmt) stmt).getRHS()).getName().getID();
            getArrayIndexShape(rhsName, arrayName, arrayList);
        }
        else if(stmt instanceof TIRCopyStmt){
            Expr lhs = ((AssignStmt) stmt).getLHS();
            Expr rhs = ((AssignStmt) stmt).getRHS();
            String rhsName = ((NameExpr)rhs).getName().getID();
            String lhsName = ((NameExpr)lhs).getName().getID();
            PromotedShape ps = currentOutSet.get(rhsName);
            if(!currentOutSet.containsKey(lhsName)
                    || !ps.equals(currentOutSet.get(lhsName))){
                currentOutSet.put(lhsName, ps); // copy
            }
        }
        return true;
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

    private void getArrayIndexShape(String lhsName, String arrayName, TIRCommaSeparatedList arrayList){
        int count = 0, pos = -1, c = 0;
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
        if(count == 1){
            PromotedShape namePS = currentOutSet.get(arrayList.getName(pos).getID());
            PromotedShape lhsPS  = currentOutSet.get(lhsName);
            lhsPS.setP(namePS, pos);
            currentOutSet.put(lhsName, lhsPS); //save back
        }
        else if(count > 1){
            PromotedShape lhsPS  = currentOutSet.get(lhsName);
            lhsPS.setT(); //set to Top
            currentOutSet.put(lhsName, lhsPS);
        }
        return ;
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
        ifThenList = new ast.List();
        ifElseList = new ast.List();
        int len = node.getNumChild();
        for(int i=0;i<len;i++){
            ASTNode currentNode = node.getChild(i);
            if(currentNode instanceof TIRIfStmt){
                ifConversion((TIRIfStmt)currentNode, false);
            }
            else convertIfStmt(currentNode);
        }
    }

    private boolean ifConversion(TIRIfStmt node, boolean f){
        PrintMessage.See(node.getConditionVarName().getID(), "if conv name");
        PrintMessage.See(outFlowSets.get(node).get(node.getConditionVarName().getID()).isP()?"true":"false");
        if(f==true || outFlowSets.get(node).get(node.getConditionVarName().getID()).isP()){
            nextIfStmt(node.getIfStatements()  , true);
            nextIfStmt(node.getElseStatements(), true);
//            doIfConversion(node);
            return doIfConversion2(node);
        }
        else {
            nextIfStmt(node.getIfStatements()  , f);
            nextIfStmt(node.getElseStatements(), f);
        }
        return false;
    }

    public void nextIfStmt(ASTNode node, boolean f){
        int num = node.getNumChild();
        for(int i=0;i<num;i++){
            ASTNode currentNode = node.getChild(i);
            if(currentNode instanceof TIRIfStmt)
                ifConversion((TIRIfStmt)currentNode, f);
        }
    }

    ////// if conversion v2
    private boolean doIfConversion2(TIRIfStmt node){
        Map<String, String> mapThen = new HashMap<>();
        Map<String, String> mapElse = new HashMap<>();
        String thenCond = node.getConditionVarName().getID();
        Map<ASTNode, Boolean> blockThen = mapIfBlock(node.getIfStatements());
        Map<ASTNode, Boolean> blockElse = mapIfBlock(node.getElseStatements());
        PrintMessage.delimiter();
        PrintMessage.See("Print if conversion", "AnalysisVector");
        gen("thenCond", thenCond);
        gen("elseCond", "not(thenCond)");
        for(Stmt s : node.getIfStatements()){
            if(s instanceof AssignStmt){
                Set<String> lhsName = getLHSName(s);
                if(lhsName.size() != 1){
                    PrintMessage.Warning("Left hand side variable in the IF block must be 1.");
                    return false;
                }
                String lhs = lhsName.iterator().next(); // lhs name0
                if(isTempLike(lhs,s, blockThen)){
                    String t0 = genTemp();
                    gen(t0, genRHS(s));
                    gen(lhs, "times(thenCond, " + t0 + ")");
                }
                else {
                    String t1 = genTemp();
                    String t2 = genTemp();
                    gen(t1, genRHS(s));
                    gen(t2, "times(thenCond, " + t1 + ")");
                    mapThen.put(lhs, t2);
                }
            }
            else return  false;
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
                    if(isTempLike(lhs,s,blockElse)){
                        String t3 = genTemp();
                        gen(t3, genRHS(s));
                        gen(lhs, "times(elseCond, " + t3 + ")");
                    }
                    else if(mapThen.containsKey(lhs)){
                        String t4 = genTemp();
                        String t5 = genTemp();
                        gen(t4, genRHS(s));
                        gen(t5, "times(elseCond, " + t4 + ")");
                        gen(lhs, t5 + " + " + mapThen.get(lhs));
                        mapThen.remove(lhs);
                    }
                    else {
                        String t6 = genTemp();
                        String t7 = genTemp();
                        gen(t6, genRHS(s));
                        gen(t7, "times(elseCond, " + t6 + ")");
                        mapElse.put(lhs, t7);
                    }
                }
                else return  false;
            }
        }
        if(mapThen.size() > 0){
            for(String n : mapThen.keySet()){
                String t8 = genTemp();
                gen(t8, "times(" + n + ", elseCond)");
                gen(n, t8 + " + " + mapThen.get(n));
            }
        }
        if(mapElse.size() > 0){
            for(String n : mapElse.keySet()){
                String t9 = genTemp();
                gen(t9, "times(" + n + ", thenCond)");
                gen(n, t9 + " + " + mapElse.get(n));
            }
        }
        return true;
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

    private void gen(String lhs, String rhs){
        PrintMessage.See(lhs.trim() + " = " + rhs.trim() + ";");
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
