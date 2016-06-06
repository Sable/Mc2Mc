package mc2mc.mc2lib;

import ast.ASTNode;
import natlab.tame.tir.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrintMessage {
    public static void Error(String text){
        System.err.println("Error: " + text);
        System.exit(99);
    }

    public static void Warning(String text){
        System.err.println("Warning: " + text);
    }

    public static void See(String text){
        System.out.println(text);
    }

    public static void See(String text, String other){
        System.out.println("[" + other + "] : " + text);
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
    
    public static void arrayList(List<String> x){
        System.out.println("length = " + x.size());
        int c = 0;
        for(String x0 : x) {
            System.out.println("[" + c + "] " + x0);
            c = c + 1;
        }

    }
    public static void arrayList(List<String> x, String text){
        System.out.println(text);
        arrayList(x);
        delimiter();
    }

    // debugging
    public static void delimiter(){
        delimiter('-', 10);
    }

    public static void delimiter(char x, int n){
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
                System.out.println("- " + ((ASTNode)t).getPrettyPrinted());
            }
        }
    }

    public static void printMap2(Map<TIRNode, Set<TIRNode>> input){
        See("printMap2:");
        for(TIRNode t : input.keySet()){
            int n = 0;
            See("- " + printString(t).trim() );
            for(TIRNode one : input.get(t)){
                See(n++ + ": " + printString(one));
            }
            See(" total " + n);
        }
        See("printMap2 ends");
    }

    public static void printMap(Map<TIRNode, Integer> x){
        for(TIRNode t : x.keySet()){
            PrintMessage.See(((TIRCallStmt)t).getPrettyPrinted().trim() + " : " + x.get(t));
        }
    }

    public static String printString(TIRNode x){
        return ((ASTNode)x).getPrettyPrinted();
    }

    /**
     * Print tir node directly
     *
     * isSynthetic: node.getStartLine() == 0 && node is not a list
     *
     */
    public static void GetPrettyPrintTirNode(TIRNode t){
        System.out.println("- " + t.toString() + Tag("GetPrettyPrintTirNode"));
        String rtn = "NOT FOUND";
        //int lno = ((ASTNode)t).getStartLine();
        int lno = CommonFunction.FindLineNo((ASTNode)t);
        if(t instanceof TIRCallStmt){
            rtn = ((TIRCallStmt)t).getPrettyPrinted();
        }
        else if(t instanceof TIRForStmt){
            rtn = ((TIRForStmt)t).getPrettyPrinted();
        }
        else if(t instanceof TIRAssignLiteralStmt){
            rtn = ((TIRAssignLiteralStmt)t).getPrettyPrinted();
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


    private static String Tag(String funcName){
        return " : from PrintMessage." + funcName;
    }
}
