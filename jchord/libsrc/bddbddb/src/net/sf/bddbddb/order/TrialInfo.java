/*
 * Created on Jan 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import java.util.Map;

import net.sf.bddbddb.BDDInferenceRule;
import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.order.EpisodeCollection.Episode;

import org.jdom.Element;


/**
 * Information about a particular trial.
 * 
 * @author John Whaley
 * @version $Id: TrialInfo.java 551 2005-05-19 01:53:49Z cs343 $
 */
public class TrialInfo implements Comparable {
    /**
     * Order tried.
     */
    public Order order;

    /**
     * Cost of this trial.
     */
    public long cost;

    /**
     * Collection that contains this trial.
     */
    public Episode episode;

    /**
     * The predicted results for this trial.
     */
    public TrialPrediction pred;

    public long timestamp;
    /**
     * Construct a new TrialInfo.
     * 
     * @param o  order
     * @param p predict value for this trial
     * @param c  cost
     */
    public TrialInfo(Order o, TrialPrediction p, long c, Episode ep, long timestamp) {
        this.order = o;
        this.pred = p;
        this.cost = c;
        this.episode = ep;
        this.timestamp = timestamp;
    }

    /**
     * @return Returns the trial collection that this is a member of.
     */
    public EpisodeCollection getCollection() {
        return episode.getEpisodeCollection();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return order + ": cost " + cost;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
        return compareTo((TrialInfo) arg0);
    }

    /**
     * Comparison operator for TrialInfo objects.  Comparison is based on cost.
     * If the cost is equal, we compare the order lexigraphically.
     * 
     * @param that  TrialInfo to compare to
     * @return  -1, 0, or 1 if this TrialInfo is less than, equal to, or greater than the other
     */
    public int compareTo(TrialInfo that) {
        if (this == that) return 0;
        int result = FindBestDomainOrder.signum(this.cost - that.cost);
        if (result == 0) {
            result = this.order.compareTo(that.order);
        }
        if(result == 0){
            result = FindBestDomainOrder.signum(this.timestamp - that.timestamp);
        }
        return result;
    }

    public boolean isMax() {
        return cost == BDDInferenceRule.LONG_TIME;
    }

    public static String PREDICTION_VAR1 = "LowerBound";

    public static String PREDICTION_VAR2 = "UpperBound";

    /**
     * Returns this TrialInfo as an XML element.
     * 
     * @return XML element
     */
    public Element toXMLElement() {
        Element dis = new Element("trialInfo");
        dis.setAttribute("order", order.toString());
        dis.setAttribute("cost", Long.toString(cost));
        dis.setAttribute("timestamp", Long.toString(timestamp));
        dis.setAttribute("var" + PREDICTION_VAR1, Double.toString(pred.getVarLowerBound()));
        dis.setAttribute("var" + PREDICTION_VAR2, Double.toString(pred.getVarUpperBound()));
        dis.setAttribute("attr" + PREDICTION_VAR1, Double.toString(pred.getAttrLowerBound()));
        dis.setAttribute("attr" + PREDICTION_VAR2, Double.toString(pred.getAttrUpperBound()));
        dis.setAttribute("dom" + PREDICTION_VAR1, Double.toString(pred.getDomLowerBound()));
        dis.setAttribute("dom" + PREDICTION_VAR2, Double.toString(pred.getDomUpperBound()));
        return dis;
    }

    public static TrialInfo fromXMLElement(Element e, Map nameToVar, Episode ep) {
        String o_str = e.getAttributeValue("order");
        if(o_str == null) throw new IllegalArgumentException();
        Order o = Order.parse(o_str, nameToVar);
        
        long c = Long.parseLong(e.getAttributeValue("cost"));
        long timestamp = Long.parseLong(e.getAttributeValue("timestamp"));
        String score1 = e.getAttributeValue("score");
        double score = score1 == null ? 0 : Double.parseDouble(score1);
        String var1 = e.getAttributeValue("var" + PREDICTION_VAR1);
        String var2 = e.getAttributeValue("var" + PREDICTION_VAR2);
        double vVar1 = var1 == null ? 0 : Double.parseDouble(var1);
        double vVar2 = var2 == null ? Double.MAX_VALUE : Double.parseDouble(var2);
        var1 = e.getAttributeValue("attr" + PREDICTION_VAR1);
        var2 = e.getAttributeValue("attr" + PREDICTION_VAR2);
        double aVar1 = var1 == null ? 0 : Double.parseDouble(var1);
        double aVar2 = var2 == null ? Double.MAX_VALUE : Double.parseDouble(var2);
        var1 = e.getAttributeValue("dom" + PREDICTION_VAR1);
        var2 = e.getAttributeValue("dom" + PREDICTION_VAR2);
        double dVar1 = var1 == null ? 0 : Double.parseDouble(var1);
        double dVar2 = var2 == null ? Double.MAX_VALUE : Double.parseDouble(var2);
        return new TrialInfo(o, new TrialPrediction(score, vVar1, vVar2, aVar1, aVar2, dVar1, dVar2), c, ep, timestamp);
    }
}