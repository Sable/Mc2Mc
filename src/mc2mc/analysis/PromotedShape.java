package mc2mc.analysis;

import ast.RangeExpr;
import natlab.tame.valueanalysis.components.shape.Shape;

/**
 * Created by wukefe on 6/3/16.
 */
public class PromotedShape {
    int shape;
    int loc;         // promoted dim (location)
    RangeExpr rExpr; // promoted value
    Shape oldShape;  // old shape before promoted

    int shapeB = 0; //bottom
    int shapeS = 1; //scalar
    int shapeN = 2; //non-scalar
    int shapeP = 3; //promoted
    int shapeT = 4; //top

    public PromotedShape(PromotedShape p1){
        shape = p1.getShape(); //more range
    }

    public PromotedShape(int s){
        shape = s;
    }

    public PromotedShape() {
    }

    // set shape
    public void setB(){
        shape = shapeB;
    }

    public void setS(){
        shape = shapeS;
    }

    public void setN(){
        shape = shapeN;
    }

    public void setP(RangeExpr r, int location){
        shape = shapeP;
        rExpr = r;
        loc   = location;
    }

    public void setP(PromotedShape p1, int location){
        shape = shapeP;
        // todo: update shape with given p1
        loc = location;
        setOldShape(p1.getOldShape());
    }

    public void setP(PromotedShape p1){
        shape = shapeP;
        // more Range
    }

    public void setT(){
        shape = shapeT;
    }

    // is shape
    public boolean isB(){
        return shape == shapeB;
    }

    public boolean isS(){
        return shape == shapeS;
    }

    public boolean isN(){
        return shape == shapeN;
    }

    public boolean isP(){
        return shape == shapeP;
    }

    public boolean isT(){
        return shape == shapeT;
    }

    // get shape
    public int getShape(){
        return shape;
    }
    public Shape getOldShape() { return oldShape; }
    public void setOldShape(Shape p) { oldShape = p; }


    // compare
    public boolean equals(PromotedShape p1){
        if(shape == p1.getShape()){
//            if(shape == shapeP){ // check P
//                return dimv.equals(p1.getDim());
//            }
            return true;
        }
        return false;
    }

    public String getPrettyPrinted(){
        String str = "[Shape: ";
        String sp = "";
        switch (shape){
            case 0:  sp = "unknown"; break;
            case 1:  sp = "scalar"; break;
            case 2:  sp = "non-scalar"; break;
            case 3:  sp = "promoted"; break;
            case 4:  sp = "fail"; break;
        }
        return str + sp + "]";
    }


}
