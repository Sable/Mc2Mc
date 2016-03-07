package mc2mc;

import ast.ASTNode;
import ast.AssignStmt;
import ast.NameExpr;
import ast.Stmt;
import nodecases.AbstractNodeCaseHandler;

import java.util.Set;

/**
 * Print reaching definition
 * Usage:
 *   ast.analyze(new ReachingDefPrint(analysis));
 */
public class ReachingDefPrint extends AbstractNodeCaseHandler {

    private ReachingDef rd;
    public ReachingDefPrint(ReachingDef analysis){
        rd = analysis;
    }

    @Override
    public void caseASTNode(ASTNode node) {
        for(int i = 0;i<node.getNumChild();i++){
            node.getChild(i).analyze(this);
        }
    }

    @Override
    public void caseStmt(Stmt node){
        //caseASTNode(node);

        System.out.println("========");
        System.out.println("in set:");
        System.out.println("");
        printSet(rd.getInFlowSets().get(node));

        System.out.println("--------");
        System.out.printf("stmt covering line(s) %s to %s:\n", node.getStartLine(), node.getEndLine());
        System.out.println("");
        System.out.println(node.getPrettyPrinted());

        System.out.println("--------");
        System.out.println("out set:");
        System.out.println("");
        printSet(rd.getOutFlowSets().get(node));
        System.out.println("========");
    }

    private void printSet(Set<AssignStmt> defs) {
        for (AssignStmt def : defs) {
            System.out.printf("%s at [%s, %s]\n", ((NameExpr)def.getLHS()).getName().getID(), def.getStartLine(), def.getStartColumn());
        }
    }
}
