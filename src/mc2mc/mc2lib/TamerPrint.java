package mc2mc.mc2lib;

import ast.ASTNode;
import ast.AssignStmt;
import ast.Expr;
import ast.NameExpr;
import natlab.tame.tamerplus.analysis.AnalysisEngine;
import natlab.tame.tir.*;

import java.util.*;

/**
 * Created by wukefe
 */
public class TamerPrint {

    public Map<TIRNode, Map<String, Set<TIRNode>>> localUDMap = null;
    public Map<TIRNode, HashMap<String, HashSet<TIRNode>>> localDUMap = null;
    public ASTNode root = null;
    public Map<ASTNode, Map<String, String>> localMap2 = null;
    public static boolean debug = false;

    public TamerPrint(AnalysisEngine engine, TIRFunction node){
        localUDMap = engine.getUDChainAnalysis().getChain();
        localDUMap = engine.getDUChainAnalysis().getChain();
//        useMap = new HashMap<>();
//        tempSet= new HashSet<>();
        currentNodeSet = new ArrayList<>();
        basicBlock = new ArrayList<>();
        root = node;
    }

    private List<ASTNode> currentNodeSet = null;
    private List<List<ASTNode>> basicBlock = null;
//    private Map<ASTNode, Map<String, ASTNode>> useMap = null;
//    private Set<ASTNode> tempSet = null;

    Map<ASTNode, String> map2String = null;
    Set<ASTNode> mapped = null;

    public void getBlock(){
        traversalNode(root);
    }

    public void traversalNode(ASTNode node){
        if(node instanceof TIRFunction){
            for(ASTNode a : ((TIRFunction) node).getStmtList()){
                traversalNode(a);
            }
            saveSet();
        }
        else if(node instanceof TIRForStmt){
//            currentNodeSet.add(node); // for head
//            saveSet();
            for(ASTNode a : ((TIRForStmt) node).getStatements()){
                traversalNode(a);
            }
        }
        else if(node instanceof TIRIfStmt){
//            currentNodeSet.add(node); // for head
//            saveSet();
            for(ASTNode a : ((TIRIfStmt) node).getIfStatements()){
                traversalNode(a);
            }
            saveSet();
            for(ASTNode a : ((TIRIfStmt) node).getElseStatements()){
                traversalNode(a);
            }
            saveSet();
        }
        else if(node instanceof TIRStmt){
            currentNodeSet.add(node);
        }
    }

    private void saveSet(){
        if(currentNodeSet.size() > 0) {
            basicBlock.add(currentNodeSet);
            currentNodeSet = new ArrayList<>();
        }
    }

    public void printBlocks(){
        PrintMessage.See("print blocks:");
        int blockID = 0;
        for(List<ASTNode> la : basicBlock){
            PrintMessage.See("ID " + blockID);
            int k = 0;
            for(ASTNode a : la){
                PrintMessage.See(a.getPrettyPrinted().trim(), k+"");
                k++;
            }
            blockID++;
        }
    }


    public void processBlocks(){
        getBlock(); // initalize
//        PrintMessage.See("block size = " + basicBlock.size());
        map2String = new HashMap<>();
        mapped = new HashSet<>();
        localMap2 = new HashMap<>();
        for(List<ASTNode> b : basicBlock){

            for(ASTNode a : b){
                if(a instanceof AssignStmt){
                    map2String.put(a, ((AssignStmt) a).getRHS().getPrettyPrinted().trim());
                }
                else {
                    map2String.put(a, a.getPrettyPrinted().trim());
                }
            }

            for(ASTNode a : b) {
                if (a instanceof AssignStmt) {
                    Set<String> lhsNames = ((AssignStmt)a).getLValues();
                    if(lhsNames.size() == 1) {
                        String s = lhsNames.iterator().next();
                        if(localDUMap.get(a)!=null) {
                            Set<TIRNode> useSet = localDUMap.get(a).get(s);
                            if (useSet != null && useSet.size() == 1) {
                                ASTNode use0 = (ASTNode) (useSet.iterator().next());
                                if (b.contains(use0)) {
                                    if(use0.getPrettyPrinted().trim().equals("[mc_t12] = times(mc_t13, mc_t14);")){
                                        int xx=10;
                                    }
                                    processDefStmt(s, use0, b, map2String, mapped);
//                                    if(!tempSet.contains(use0))
//                                        tempSet.add(a);
//
//                                    if (!useMap.containsKey(use0)) {
//                                        Map<String, ASTNode> newUse = new HashMap<>();
//                                        newUse.put(s, a);
//                                        useMap.put(use0, newUse);
//                                    } else {
//                                        Map<String, ASTNode> oldUse = useMap.get(use0);
//                                        oldUse.put(s, a);
//                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(debug) {
            PrintMessage.See("Update map2String:");
            int k = 0;
            for (ASTNode a : map2String.keySet()) {
                PrintMessage.See(a.getPrettyPrinted().trim() + " -> " + map2String.get(a), k + "," + mapped.contains(a));
                k++;
            }
        }
    }

    public void processDefStmt(String s, ASTNode b, List<ASTNode> oneBlock,
                               Map<ASTNode,String> map2String,
                               Set<ASTNode> mapped){
        List<String> rhsNames = new ArrayList<>();
        String leftHand = "";
        String rightHand= "";
        if(b instanceof AssignStmt){
            rhsNames = getAllNames(((AssignStmt) b).getRHS());
            leftHand = ((AssignStmt) b).getLHS().getPrettyPrinted();
            rightHand= ((AssignStmt) b).getRHS().getPrettyPrinted();
        }
        else{
            rhsNames = getAllNames(b);
            rightHand = b.getPrettyPrinted();
        }
//        Set<String> skipSet = new HashSet<>();
        Map<String, String> map2 = null;
        if(localMap2.containsKey(b)){
            map2 = localMap2.get(b);
        }
        else {
            map2 = new HashMap<>();
            localMap2.put(b, map2);
        }

        Set<String> dangerNames = new HashSet<>();
        if(b instanceof AssignStmt){
            dangerNames.addAll(getAllNames(((AssignStmt) b).getLHS()));
        }

        String n = s;
//        for(String n : rhsNames) {
//            if(skipSet.contains(n)) continue;
            Set<TIRNode> defSet = localUDMap.get(b).get(n);
            if(defSet!=null && defSet.size() == 1){
                TIRNode def0 = defSet.iterator().next();
                if(((ASTNode)def0).getPrettyPrinted().trim().equals("[y] = minus(mc_t15, n);")){
                    int xx=10;
                }
                if(oneBlock.contains(def0)){
                    List<String> lhsNames = getAllNames(((AssignStmt)def0).getLHS());
                    String oneName = "";
                    if(lhsNames.size() == 1)
                        oneName = lhsNames.iterator().next();
//                    rightHand = rightHand.replace(n, map2String.get(def0));
                    if(!mapped.contains(def0)
                            && !oneName.isEmpty()
                            && !dangerNames.contains(n)  // a = zeros(...);  a(i) = xxx;
                            && localDUMap.get(def0).get(oneName).size()==1){
                        if(n.equals("prices")){
                            int xx=10;
                        }
                        map2.put(n, map2String.get(def0));
                        mapped.add((ASTNode) def0);
                    }
                }
            }
//            skipSet.add(n);
//        }
        if(b.getPrettyPrinted().trim().equals("mc_t121 = times(thenCond, new_t9);")){
            int xx=10;
        }
        if(map2.size() > 0)
            map2String.put(b, replaceStmt(b, map2)); //update
    }

    public Map<ASTNode, String> getMap2String(){
        return map2String;
    }

    public Set<ASTNode> getMapped(){
        return mapped;
    }

    public List<String> getAllNames(ASTNode e){
        List<String> names = new ArrayList<>();
        if(e instanceof NameExpr){
            names.add(e.getVarName());
        }
        else {
            for (int i = 0; i < e.getNumChild(); i++) {
                ASTNode currentExpr = e.getChild(i);
                names.addAll(getAllNames(currentExpr));
            }
        }
        return names;
    }

    public String replaceStmt(ASTNode old, Map<String,String> map2){
        String rtn = "% unknown";
        if(old instanceof TIRCallStmt){
            rtn =  ((TIRCallStmt) old).getFunctionName().getID()
                    + "(";
            int cnt = 0;
            for(Expr e : ((TIRCallStmt) old).getArguments()){
                if(cnt > 0)
                    rtn += ",";
                if(e instanceof NameExpr){
                    String n = e.getVarName();
                    rtn += (map2.containsKey(n)?map2.get(n):n);
                }
                else {
                    rtn += e.getPrettyPrinted().trim();
                }
                cnt ++;
            }
            rtn += ")";
        }
        else if(old instanceof TIRArraySetStmt){
            String n = ((NameExpr)(((TIRArraySetStmt) old).getRHS())).getVarName();
            rtn =  map2.get(n);
        }
        else if(old instanceof TIRArrayGetStmt){
            rtn = ((TIRArrayGetStmt) old).getArrayName().getID()
                    + "(";
            int cnt = 0;
            for(Expr e : ((TIRArrayGetStmt) old).getIndices()){
                if(cnt > 0)
                    rtn += ",";
                if(e instanceof NameExpr){
                    String n = e.getVarName();
                    rtn += (map2.containsKey(n)?map2.get(n):n);
                }
                else {
                    rtn += e.getPrettyPrinted().trim();
                }
                cnt++;
            }
            rtn += ")";
        }
        else if(old instanceof TIRCopyStmt){
            String n = ((TIRCopyStmt) old).getSourceName().getID();
            rtn = map2.get(n);
        }
        else {
            PrintMessage.See(old.dumpString(),"dump");
        }
        if(rtn == null){
            int xx=10;
        }
        if(rtn.contains("ast.")){
            PrintMessage.See(old.getPrettyPrinted(),"dumpxx");
        }
        return rtn;
    }

//    public Map<ASTNode, Map<String, ASTNode>> getUseMap(){
//        return useMap;
//    }
//
//    public Set<ASTNode> getTempSet(){
//        return tempSet;
//    }
}
