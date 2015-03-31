/*
 * Created on Jan 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.bddbddb.BDDInferenceRule;
import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.Solver;

import org.jdom.Element;


/**
 * A collection of trials on the same test. (The same BDD operation.)
 * 
 * @author John Whaley
 * @version $Id: EpisodeCollection.java 551 2005-05-19 01:53:49Z cs343 $
 */
public class EpisodeCollection {
    /**
     * Name of the test.
     */
    public String name;

    /**
     * Time of the test.
     */
    //long timeStamp;
    
    public static String RULE_CONST = "rule";
    public static String SPACER = "_";
    public static String UPDATE_CONST = "update";
    public static String OP_CONST = "op";
    /**
     * Map from orders to their trial information.
     */
    public Map/*<Order,TrialInfo>*/ trials;

    List episodes;
    
    /**
     * Best trial so far.
     */
    TrialInfo best;

    /**
     * Trial info sorted by cost. Updated automatically when necessary.
     */
    transient TrialInfo[] sorted;

    /**
     * Construct a new collection of trials.
     * 
     * @param n  test name
     */
    public EpisodeCollection(BDDInferenceRule rule, int opNum) {
        name = RULE_CONST + rule.id + SPACER + UPDATE_CONST + rule.updateCount + SPACER + OP_CONST + opNum;     
        trials = new LinkedHashMap();
        sorted = null;
        episodes = new LinkedList();
    }

    EpisodeCollection(String n){
        name = n;     
        trials = new LinkedHashMap();
        sorted = null;
        episodes = new LinkedList();
    }
    
    /* TODO stamp episodes instead of the collection */
/*    public void setTimeStamp(long stamp){
        timeStamp = stamp;
    }
*/
        /**
     * Add the information about a trial to this collection.
     * 
     * @param i  trial info
     */
    public void addTrial(TrialInfo i) {
        if (FindBestDomainOrder.TRACE > 2) FindBestDomainOrder.out.println(this+": Adding trial "+i);
        trials.put(i.order, i);
        if (best == null || best.cost > i.cost) {
            best = i;
        }
        sorted = null;
    }

    /**
     * Add the information about a trial to this collection.
     * 
     * @param o  order
     * @param p predicted value for this trial
     * @param cost  cost of operation
     */
    public void addTrial(Order o, TrialPrediction p, long cost, long timestamp) {
        Episode ep = getCurrEpisode();
        if(ep == null) ep = startNewEpisode(System.currentTimeMillis());
        
        addTrial(new TrialInfo(o, p, cost, ep, timestamp));
    }
    
    

    /**
     * Returns true if this collection contains a trial with the given order,
     * false otherwise.
     * 
     * @param o  order
     * @return true if this collection contains it, false otherwise
     */
    public boolean contains(Order o) {
        return trials.containsKey(o);
    }

    /**
     * Calculates the standard deviation of a collection of trials.
     * 
     * @param trials  collection of trials
     * @return variance
     */
    public static double getStdDev(Collection trials) {
        return Math.sqrt(getVariance(trials));
    }

    /**
     * @return the standard deviation of the trials
     */
    public double getStdDev() {
        return getStdDev(trials.values());
    }

    /**
     * Calculates the variance of a collection of trials.
     * 
     * @param trials  collection of trials
     * @return variance
     */
    public static double getVariance(Collection trials) {
        double sum = 0.;
        double sumOfSquares = 0.;
        int n = 0;
        for (Iterator i = trials.iterator(); i.hasNext(); ++n) {
            TrialInfo t = (TrialInfo) i.next();
            double c = (double) t.cost;
            sum += c;
            sumOfSquares += c * c;
        }
        // variance = (n*sum(X^2) - (sum(X)^2))/n^2
        double variance = (sumOfSquares * n - sum * sum) / (n * n);
        return variance;
    }

    /**
     * @return the variance of the trials
     */
    public double getVariance() {
        return getVariance(trials.values());
    }

    /**
     * @return the minimum cost trial
     */
    public TrialInfo getMinimum() {
        return best;
    }

    /**
     * @return the maximum cost trial
     */
    public TrialInfo getMaximum() {
        TrialInfo best = null;
        for (Iterator i = trials.values().iterator(); i.hasNext();) {
            TrialInfo t = (TrialInfo) i.next();
            if (best == null || t.cost > best.cost)
                best = t;
        }
        return best;
    }

    /**
     * @return the mean of the trials
     */
    public long getAverage() {
        long total = 0;
        int count = 0;
        for (Iterator i = trials.values().iterator(); i.hasNext(); ++count) {
            TrialInfo t = (TrialInfo) i.next();
            total += t.cost;
        }
        if (count == 0) return 0L;
        else return total / count;
    }

    /**
     * @return the trials sorted by increasing cost
     */
    public TrialInfo[] getSorted() {
        if (sorted == null) {
            sorted = (TrialInfo[]) trials.values().toArray(new TrialInfo[trials.size()]);
            Arrays.sort(sorted);
        }
        return sorted;
    }

    /**
     * @return the collection of trials
     */
    public Collection getTrials() {
        return trials.values();
    }

    /**
     * @return the number of trials in this collection
     */
    public int getNumTrials() {
        return trials.size();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name;
        /*+ "@" + FindBestDomainOrder.dateFormat.format(new Date(timeStamp));*/
    }

    /**
     * Returns this EpisodeCollection as an XML element.
     * 
     * @return XML element
     */
    public Element toXMLElement() {
        Element dis = new Element("episodeCollection");
        dis.setAttribute("name", name);
        /* dis.setAttribute("timeStamp", Long.toString(timeStamp));*/
        for(Iterator it = episodes.iterator(); it.hasNext(); ){
            Episode ep = (Episode) it.next();
            dis.addContent(ep.toXMLElement());
        }
     
        return dis;
    }

    static InferenceRule parseRule(Solver solver, String s) {
        int updateIndex = s.indexOf(UPDATE_CONST);
        
        int ruleNum = -1;
        if(updateIndex > -1)
            ruleNum = Integer.parseInt(s.substring(RULE_CONST.length(),updateIndex - 1));
        else
            ruleNum = Integer.parseInt(s.substring(RULE_CONST.length()));
        
        InferenceRule rule = solver.getRule(ruleNum);
        return rule;
    }
    
    static int parseUpdateCount(String s){
       int updateIndex = s.indexOf(UPDATE_CONST);
       int opIndex = s.indexOf(OP_CONST);
       if(opIndex > -1){
           return Integer.parseInt(s.substring(updateIndex + UPDATE_CONST.length(), opIndex - 1));
       }else if(updateIndex > -1){
           return Integer.parseInt(s.substring(updateIndex + UPDATE_CONST.length()));
       }
        return -1;
    }
    
    static int parseOpNumber(String s){
        int opIndex = s.indexOf(OP_CONST);
        if(opIndex > -1)
            return Integer.parseInt(s.substring(opIndex + OP_CONST.length()));
        return -1;
    }

    public InferenceRule getRule(Solver solver) {
        return parseRule(solver, name);
    }
    
    public int getUpdateCount(){ return parseUpdateCount(name); }

    public int getOpNumber(){ return parseOpNumber(name); }
    
    public static EpisodeCollection fromXMLElement(Element e, Solver solver) {
        String name = e.getAttributeValue("name");
        InferenceRule rule = parseRule(solver, name);
        Map nameToVar = rule.getVarNameMap();
  /*      long timeStamp;
        try {
            timeStamp = Long.parseLong(e.getAttributeValue("timeStamp"));
        } catch (NumberFormatException _) {
            timeStamp = 0L;
        }
    */   
        EpisodeCollection ec = new EpisodeCollection(name);
        /* backwards compatible: try to parse out trial info's and if it fails
         * parse episodes instead.
         */
        try{
            long timestamp = Long.parseLong(e.getAttributeValue("timestamp"));
            Episode ep = ec.startNewEpisode(timestamp);
            for (Iterator i = e.getContent().iterator(); i.hasNext();) {
                Object e2 = i.next();
                if (e2 instanceof Element) {
                    TrialInfo ti = TrialInfo.fromXMLElement((Element) e2, nameToVar, ep);
                    ep.addTrial(ti);
                }
            }
        }catch(IllegalArgumentException ex){
            ec.episodes.clear();
            for(Iterator it = e.getContent().iterator(); it.hasNext(); ){
                Object elem2 = it.next();
                if(elem2 instanceof Element)
                    episodeFromXMLElement((Element) elem2,nameToVar, ec);
            }
        }
      
        return ec;
    }
    public Collection getOrders(){ return trials.keySet(); }
    public Episode startNewEpisode(long timestamp){
        Episode ep = new Episode(timestamp);
        episodes.add(ep);
        return ep;
    }
    /* will throw an expection if no episodes have been started */
    public Episode getCurrEpisode(){
        if(episodes.size() == 0) return null;
        return (Episode) episodes.get(episodes.size() - 1);
    }
    /* has side effects on trialcollection */
    public static Episode episodeFromXMLElement(Element elem1, Map nameToVar, EpisodeCollection tc){
        
        long timestamp = Long.parseLong(elem1.getAttributeValue("timestamp"));
        Episode ep = tc.startNewEpisode(timestamp);
        for (Iterator i = elem1.getContent().iterator(); i.hasNext();) {
            Object elem2 = i.next();
            if (elem2 instanceof Element) {
                TrialInfo ti = TrialInfo.fromXMLElement((Element) elem2, nameToVar, ep);
                ep.addTrial(ti);
            }
        }
        return ep;
    }
    

    
    public class Episode{
      Map trials;
      TrialInfo best;
      long timestamp;
      public Episode(long timestamp){
          this.timestamp = timestamp;
          trials = new LinkedHashMap();
      }
      public void addTrial(TrialInfo ti){
          trials.put(ti.order, ti);
          if (best == null || best.cost > ti.cost) {
              best = ti;
          }
          EpisodeCollection.this.addTrial(ti);
      }
      
      public Element toXMLElement(){
          Element e = new Element("episode");
          e.setAttribute("timestamp", Long.toString(timestamp));
         for (Iterator i = trials.values().iterator(); i.hasNext();) {
              TrialInfo info = (TrialInfo) i.next();
              e.addContent(info.toXMLElement());
          }
          return e;
      }
      public EpisodeCollection getEpisodeCollection(){ return EpisodeCollection.this; }
    }    
}