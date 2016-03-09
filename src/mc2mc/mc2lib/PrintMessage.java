package mc2mc.mc2lib;

import natlab.tame.tir.*;

import java.util.Map;
import java.util.Set;

public class PrintMessage {
    public static void Error(String text){
        System.err.println(text);
        System.exit(99);
    }

    public static void Warning(String text){
        System.err.println(text);
    }

    public static void See(String text){
        System.out.println(text);
    }

    public static void Strings(String[] strs){
        System.out.println("Print strings:");
        for(String s : strs){
            System.out.println("- " + s);
        }
    }

    public static void StringList(Set<String> ones){
        System.out.println("Print a list of strings:");
        for(String x : ones){
            System.out.println("- " + x);
        }
    }

    // debugging
    public static void Delimeter(){
        Delimiter('-', 10);
    }

    public static void Delimiter(char x, int n){
        for(int i=0;i<n;i++){
            System.out.print(x);
        }
        System.out.println();
    }

    public static void PrintTirNodeSet(Map<String, Set<TIRNode>> input){
        for (Map.Entry myentry : input.entrySet()){
            System.out.println(myentry.getKey() + ":");
            Set<TIRNode> mynode = (Set<TIRNode>)myentry.getValue();
            for(TIRNode t : mynode){
                //TIRStmt s = (TIRStmt)t;
                System.out.println("- " + t.toString());
            }
        }
    }

    /**
     * Print tir node directly
     */
    public static void GetPrettyPrintTirNode(TIRNode t){
        System.out.println("- " + t.toString());
        String rtn = "NOT FOUND";
        int lno = -1;
        if(t instanceof TIRCallStmt){
            rtn = ((TIRCallStmt)t).getPrettyPrinted();
            lno = ((TIRCallStmt) t).getStartLine();
        }
        else if(t instanceof TIRForStmt){
            rtn = ((TIRForStmt)t).getPrettyPrinted();
            lno = ((TIRForStmt)t).getStartLine();
        }
        else if(t instanceof TIRAssignLiteralStmt){
            rtn = ((TIRAssignLiteralStmt)t).getPrettyPrinted();
            lno = ((TIRAssignLiteralStmt)t).getStartLine();
        }
        else if(t instanceof TIRIfStmt){
            rtn = ((TIRIfStmt) t).getPrettyPrinted();
        }
        else if(t instanceof TIRBreakStmt){
            rtn = ((TIRBreakStmt) t).getPrettyPrinted();
        }
        else if(t instanceof TIRContinueStmt){
            rtn = ((TIRContinueStmt) t).getPrettyPrinted();
        }
        System.out.println(rtn);
        System.out.println("  line no = " + lno);
    }
}
