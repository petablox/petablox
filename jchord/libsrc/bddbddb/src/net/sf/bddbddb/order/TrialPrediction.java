/*
 * Created on Jan 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import net.sf.bddbddb.FindBestDomainOrder;


public class TrialPrediction {
    
    public double score;
    
    public double[][] predictions;

    public static int VARIABLE = 0;

    public static int ATTRIBUTE = 1;

    public static int DOMAIN = 2;

    public static int LOW = 0;

    public static int HIGH = 1;
    public TrialPrediction(double score,
            double varLowerBound, double varUpperBound, double attrLowerBound,
            double attrUpperBound,  double domLowerBound, double domUpperBound) {
        this.score = score;
        predictions = new double[3][];
        predictions[VARIABLE] = new double[2];
        predictions[VARIABLE][LOW] = varLowerBound;
        predictions[VARIABLE][HIGH] = varUpperBound;
        predictions[ATTRIBUTE] = new double[2];
        predictions[ATTRIBUTE][LOW] = attrLowerBound;
        predictions[ATTRIBUTE][HIGH] = attrUpperBound;
        predictions[DOMAIN] = new double[2];
        predictions[DOMAIN][LOW] = domLowerBound;
        predictions[DOMAIN][HIGH] = domUpperBound;
    }
    public double getVarLowerBound(){ return predictions[VARIABLE][LOW]; }
    public double getVarUpperBound(){ return predictions[VARIABLE][HIGH]; }
    public double getAttrLowerBound(){ return predictions[ATTRIBUTE][LOW]; }
    public double getAttrUpperBound(){ return predictions[ATTRIBUTE][HIGH]; }
    public double getDomLowerBound(){ return predictions[DOMAIN][LOW]; }
    public double getDomUpperBound(){ return predictions[DOMAIN][HIGH]; }
    public String toString() {
        return "score="+FindBestDomainOrder.format(score)+", var=("+FindBestDomainOrder.format(predictions[VARIABLE][LOW])+".."+FindBestDomainOrder.format(predictions[VARIABLE][HIGH])+"),"+
           "attr=("+FindBestDomainOrder.format(predictions[ATTRIBUTE][LOW])+".."+FindBestDomainOrder.format(predictions[ATTRIBUTE][HIGH])+"),"+
           "domain=("+FindBestDomainOrder.format(predictions[DOMAIN][LOW])+".."+FindBestDomainOrder.format(predictions[DOMAIN][HIGH])+")";
    }
    public double getScore() {
        return score;
    }
}