package mc2mc.analysis;

import ast.RangeExpr;
import natlab.tame.valueanalysis.components.shape.Shape;

/**
 * Created by wukefe on 6/3/16.
 */
public class PromotedShape {
    int shape;
    // pKind = 0;
    RangeExpr rExpr; // promoted value
    // pKind = 1;
    Shape oldShape;  // old shape before promoted (or N)
    int loc, dim;         // promoted dim (location)

    int pKind = 0;

    int shapeB = 0; //bottom
    int shapeS = 1; //scalar
    int shapeN = 2; //non-scalar
    int shapeP = 3; //promoted
    int shapeT = 4; //top

    public PromotedShape(PromotedShape p1){
        copyPS(p1);
    }

    public void copyPS(PromotedShape p1){
        shape = p1.getShape(); //more range
        if(shape == shapeP){
            pKind = p1.getPKind();
            if(pKind == 0) {
                rExpr = p1.getRange();
            }
            else{
                loc = p1.getLoc();
                dim = p1.getDim();
                oldShape = p1.getOldShape();
            }
        }
        else if(shape == shapeN){
            oldShape = p1.getOldShape();
        }
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

    public void setN(Shape s){
        shape = shapeN;
        oldShape = s;
    }

    public void setP(Shape s, int location, int dimension){
        shape    = shapeP;
        oldShape = s;
        loc      = location;
        dim      = dimension;
        pKind    = 1;
    }

//    public void setP(PromotedShape p1, int location){
//        setP(p1.getOldShape(), location);
//    }

    public void setP(RangeExpr re){
        shape = shapeP;
        rExpr = re;
        pKind = 0;
    }

    public void setP(PromotedShape p1){
//        shape = shapeP;
        // more Range
        copyPS(p1);
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

    public boolean acceptArraySet(PromotedShape p1){
        if(shape == p1.getShape() && shape == shapeP){
            if(pKind == p1.getPKind()){
                if(pKind == 1) {
                    if(oldShape.equals(p1.getOldShape())){
                        if(loc == p1.getLoc())
                            return true;
                        else {
                            if(dim==1 && oldShape.isScalar()) // rtnR(i, k) = resR(k);
                                return true;
                            else if(dim==2 && oldShape.isScalar() && p1.getDim()==1) // sampleOut(k) = x(i,k) or x(k, i)
                                return true;
                            return false; // dimension doesn't agree
//                            return oldShape.isScalar();
                        }
                    }
                    else {
                        return false;
                    }
//                    return (oldShape.equals(p1.getOldShape()) && loc == p1.getLoc());
                }
            }
            else if(pKind==0 && p1.getPKind()==1){
                return true;
            }
        }
        else if(shape == shapeS && p1.getShape()==shapeP){
            // P <- S
            return true;
        }
        return false;
    }

    // return the index of which var should be transposed
    public int acceptDimTransp(PromotedShape p1){
        if(shape==shapeP && p1.getShape()==shapeP){
            // 1:n is always ok
            if(pKind==0 && p1.getPKind()==1 && p1.getOldShape().isScalar())
                return 2;
            else if(p1.getPKind()==0 && pKind==1 &&  oldShape.isScalar())
                return 1;
            else if(pKind==p1.getPKind() && pKind==1) {
                if (oldShape.isScalar() && p1.getOldShape().isScalar()) {
                    if (dim == 1 && p1.getDim() == 2 && p1.getLoc() == 0) {
                        return 2;
                    } else if (dim == 2 && loc == 0 && p1.getDim() == 1) {
                        return 1;
                    } else {
                        return (loc == 0 ? 1 : 2);
                    }
                }
            }
        }
        return 0;
    }

    public boolean acceptArrayTransp(PromotedShape p1){
        if(shape == p1.getShape() && shape==shapeP){
            if(pKind == p1.getPKind() && pKind==1){
                if(dim==p1.getDim() && dim==2){
                    if(loc != p1.getLoc())
                        return true;
                }
            }
        }
        return false;
    }

    public void changeLoc(int n){
        if(pKind==1) {
            loc = n;
        }
    }

    // compare
    public boolean equals(PromotedShape p1){
        if(shape == p1.getShape()){
            if(shape == shapeP){ // check P
                if(pKind!=p1.getPKind()){
                    return false;
                }
                else if(pKind == 0){
                    return rExpr.equals(p1.getRange());
                }
                else if(dim != p1.getDim()){
                    if(dim==1 && loc == 0 && p1.getDim()==2 && p1.getLoc()==1){
                        return oldShape.equals(p1.getOldShape());
                    }
                    else if(dim==2 && loc == 1 && p1.getDim()==1 && p1.getLoc() == 0){
                        return oldShape.equals(p1.getOldShape());
                    }
                    return false;
                }
                else {
                    if(oldShape == null || p1.getOldShape() == null){
                        int xx = 10;
                    }
                    return oldShape.equals(p1.getOldShape()) && loc == p1.getLoc();
                }
//                return (pKind!=p1.getPKind()?false:pKind==0?rExpr.equals(p1.getRange()):(oldShape.equals(p1.getOldShape())&&loc==p1.getLoc()));
            }
            else if(shape == shapeN){
                return oldShape.equals(p1.getOldShape());
            }
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

    public int getPKind(){
        return pKind;
    }

    public RangeExpr getRange(){
        return rExpr;
    }

    public int getLoc(){
        return loc;
    }

    public int getDim() {
        return dim;
    }

}
