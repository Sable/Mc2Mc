package mc2mc.mc2lib;

import mc2mc.analysis.PromotedShape;

import java.util.HashSet;
import java.util.Set;

/**
 * unary:
 *  eBIF1
 *
 * binary:
 *  eBIF2
 */
public class BuildinList {

    private static Set<String> eBIF1 = new HashSet<>();
    private static Set<String> eBIF2 = new HashSet<>();

    private static String[] seteBIF1 = {"floor", "ceil", "round", "lower","fix",
                                        "abs", "uplus", "uminus",
                                        "log","log2","log10","exp","sqrt","pow2",
                                        "sin","sinh","cos","cosh","tan","tanh",
                                        "asin","asinh","acos","acosh","atan",
                                        "isnan","isinf","isfinite","ishandle",
                                        "atan2"};

    private static String[] seteBIF2 = {"and","or","xor","ne","eq",
                                        "bitand","bitor","bitxor",
                                        "plus","minus",
                                        "times","rdivide","power",
                                        "gt","ge","le","lt","eq","ne"};

    static {
        // unary
        for(String s : seteBIF1) eBIF1.add(s);
        // binary
        for(String s : seteBIF2) eBIF2.add(s);
    }

    public static boolean isUnaryEBIF(String op){
        return eBIF1.contains(op);
    }

    public static boolean isBinaryEBIF(String op){
        return eBIF2.contains(op);
    }

    public static PromotedShape unaryTable(PromotedShape p){
        return new PromotedShape(p);
    }

    /*
    B: 0
    S: 1
    N: 2
    P: 3
    T: 4
     */
    private static int[][] tableBIF2 = {
            {0, 0, 4, 4, 4},
            {0, 1, 2, 3, 4},
            {4, 2, 2, 4, 4},
            {4, 3, 4, 3, 4},
            {4, 4, 4, 4, 4}
    };

    public static PromotedShape binaryTable(PromotedShape p1, PromotedShape p2){
        int shape = tableBIF2[p1.getShape()][p2.getShape()];
        if(p1.isN() && p2.isN()){
            //
        }
        else if(p1.isP() && p2.isP()){
            // check dim
        }

        if(shape ==2){
            return new PromotedShape(p1.isN()?p1:p2);
        }
        else if(shape == 3){ // P
            return new PromotedShape(p1.isP()?p1:p2);
        }
        return new PromotedShape(shape);
    }

    public static PromotedShape mergeTwo(PromotedShape p1, PromotedShape p2){
        return binaryTable(p1, p2);
    }
}
