package mc2mc;

/*
* Reaching definition
*/

import ast.ASTNode;
import ast.AssignStmt;
import ast.Stmt;

import natlab.tame.tir.analysis.TIRAbstractSimpleStructuralForwardAnalysis;

import java.util.HashSet;
import java.util.Set;

public class ReachingDef extends TIRAbstractSimpleStructuralForwardAnalysis<Set<AssignStmt>> {
    public ReachingDef(ASTNode tree) {
        super(tree);
    }

    @Override
    public Set<AssignStmt> merge(Set<AssignStmt> in1, Set<AssignStmt> in2) {
        Set<AssignStmt> out = new HashSet<>(in1);
        out.addAll(in2);
        return out;
    }

    @Override
    public Set<AssignStmt> copy(Set<AssignStmt> in) {
        return new HashSet<>(in);
    }

    @Override
    public Set<AssignStmt> newInitialFlow() {
        return new HashSet<>();
    }

    @Override
    public void caseStmt(Stmt node){
        // superclass variables:
        // currentInSet: A
        // currentOutSet: A
        // inFlowSets: Map<ASTNode, A>
        // outFlowSets: Map<ASTNode, A>
        System.out.println("node = " + node.getPrettyPrinted());
        inFlowSets.put(node, copy(currentInSet));
        currentOutSet = copy(currentInSet);
        outFlowSets.put(node, copy(currentOutSet));
    }

    @Override
    public void caseAssignStmt(AssignStmt node){
        //System.out.println("assigned node = " + node.getPrettyPrinted());
        inFlowSets.put(node, copy(currentInSet));

        // out = in
        currentOutSet = copy(currentInSet);
        // out = out - kill
        currentOutSet.removeAll(kill(node));
        // out = out + gen
        currentOutSet.addAll(gen(node));

        outFlowSets.put(node, copy(currentOutSet));
    }

    private Set<AssignStmt> gen(AssignStmt node){
        Set<AssignStmt> s = new HashSet<>();
        s.add(node);
        return s;
    }

    private Set<AssignStmt> kill(AssignStmt node){
        Set<AssignStmt> r = new HashSet<>();
        Set<String> namesToKill = node.getLValues();
        for (AssignStmt def : currentInSet) {
            Set<String> names = def.getLValues();
            names.retainAll(namesToKill);
            if (!names.isEmpty()) {
                r.add(def);
            }
        }
        return r;
    }

}
