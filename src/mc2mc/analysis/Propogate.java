package mc2mc.analysis;

import ast.AssignStmt;
import ast.Expr;
import mc2mc.mc2lib.BuildinList;
import natlab.tame.tir.TIRCallStmt;
import natlab.tame.tir.TIRIfStmt;
import natlab.tame.tir.TIRStmt;

/**
 * Created by wukefe on 6/5/16.
 */
public class Propogate {
    TIRStmt stmt;
    public Propogate(TIRStmt s){
        stmt = s;
    }

    public boolean mainPropogate(){
        if(stmt instanceof AssignStmt){
            Expr lhs = ((AssignStmt) stmt).getLHS();
            Expr rhs = ((AssignStmt) stmt).getRHS();
            if(stmt instanceof TIRCallStmt){
                // call statement
                String op = ((TIRCallStmt) stmt).getFunctionName().getID();
                int num = ((TIRCallStmt) stmt).getArguments().size();
                if(num == 1 && BuildinList.isUnaryEBIF(op)){
                    // propogate shape
                }
                else if(num == 2 && BuildinList.isBinaryEBIF(op)){
                    //
                }
                else if(true){ // UDF analysis

                }
            }
        }
        else if(stmt instanceof TIRIfStmt){
            // if condition
        }
        return true;
    }
}
