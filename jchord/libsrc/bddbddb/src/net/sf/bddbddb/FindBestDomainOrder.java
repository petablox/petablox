// FindBestDomainOrder.java, created Aug 21, 2004 1:17:30 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jwutil.collections.FlattenedCollection;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.io.SystemProperties;
import jwutil.util.Assert;
import net.sf.bddbddb.BDDInferenceRule.VarOrderComparator;
import net.sf.bddbddb.order.AttribToDomainTranslator;
import net.sf.bddbddb.order.CandidateSampler;
import net.sf.bddbddb.order.ConstraintInfo;
import net.sf.bddbddb.order.Discretization;
import net.sf.bddbddb.order.EpisodeCollection;
import net.sf.bddbddb.order.MapBasedTranslator;
import net.sf.bddbddb.order.MyId3;
import net.sf.bddbddb.order.Order;
import net.sf.bddbddb.order.OrderConstraint;
import net.sf.bddbddb.order.OrderConstraintSet;
import net.sf.bddbddb.order.OrderTranslator;
import net.sf.bddbddb.order.Queue;
import net.sf.bddbddb.order.StackQueue;
import net.sf.bddbddb.order.TrialDataRepository;
import net.sf.bddbddb.order.TrialGuess;
import net.sf.bddbddb.order.TrialInfo;
import net.sf.bddbddb.order.TrialInstance;
import net.sf.bddbddb.order.TrialInstances;
import net.sf.bddbddb.order.TrialPrediction;
import net.sf.bddbddb.order.VarToAttribTranslator;
import net.sf.bddbddb.order.WekaInterface;
import net.sf.bddbddb.order.CandidateSampler.UncertaintySampler;
import net.sf.bddbddb.order.EpisodeCollection.Episode;
import net.sf.bddbddb.order.TrialDataRepository.TrialDataGroup;
import net.sf.bddbddb.order.WekaInterface.OrderAttribute;
import net.sf.bddbddb.order.WekaInterface.OrderInstance;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.FindBestOrder;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

/**
 * FindBestDomainOrder
 * 
 * Design:
 * 
 * TrialInfo : order, cost
 * EpisodeCollection : collection of TrialInfo, best time, worst time
 * Constraint: a<b or axb or a_b
 * Order : collection of ordering constraints
 * ConstraintInfo : map from a single constraint to score/confidence
 * OrderInfo : order, predicted score and confidence
 * 
 * Maps:
 *  Relation -> ConstraintInfo collection
 *   Rule -> ConstraintInfo collection
 * EpisodeCollection -> ConstraintInfo collection
 * 
 * Algorithm to compute best order:
 * - Combine and sort single constraints from relation, rule, trials so far.
 *   Sort by score*confidence (?)
 *   Combine and adjust opposite constraints (?)
 *   Sort by difference between opposite constraints (?)
 * - Do an A* search.
 *   Keep track of the current score/confidence as we add constraints.
 *   As we add new constraints, flag conflicting ones.
 *   Predict final score by combining top n non-conflicting constraints (?)
 *   If prediction is worse than current best score, return immediately.
 * 
 * @author John Whaley
 * @version $Id: FindBestDomainOrder.java 552 2005-05-19 08:20:05Z cs343 $
 */
public class FindBestDomainOrder {


    public static int TRACE = 2;

    public static PrintStream out;
    public static PrintStream out_t;

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd-HHmmss");

    /**
     * Link back to the solver.
     */
    BDDSolver solver;

    /**
     * Collection of all EpisodeCollections that have been done so far, including
     * ones that have been loaded from disk.
     */
    Collection allTrials;

    TrialDataRepository dataRepository;

    /**
     * Whether we should keep track of per-rule constraints, in addition to global
     * constraints.
     */
    public static boolean PER_RULE_CONSTRAINTS = true;

    public static boolean DUMP_CLASSIFIER_INFO = true;

    
    /**
     * Info collection for each of the constraints.
     */
    ConstraintInfoCollection constraintInfo;

    /**
     * Construct a new empty FindBestDomainOrder object.
     */
    public FindBestDomainOrder(Solver s) {
        constraintInfo = new ConstraintInfoCollection(s);
        allTrials = new LinkedList();
        if (s instanceof BDDSolver){
            solver = (BDDSolver) s;
            dataRepository = new TrialDataRepository(allTrials, solver);
        }
        out = solver.out;
    }
        

    /**
     * Construct a new FindBestDomainOrder object with the given info.
     */
    FindBestDomainOrder(ConstraintInfoCollection c) {
        constraintInfo = c;
        allTrials = new LinkedList();
        if (c.solver instanceof BDDSolver)
            solver = (BDDSolver) c.solver;
        out = solver.out;
    }

    /**
     * Load and incorporate trials from the given XML file.
     * 
     * @param filename  filename
     */
    void loadTrials(String filename) {
        out.println("Trials filename=" + filename);
        File file = new File(filename);
        if (file.exists()) {
            try {
                URL url = file.toURL();
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(url);
                XMLFactory f = new XMLFactory(solver);
                Element e = doc.getRootElement();
                List list = (List) f.fromXML(e);
                if (TRACE > 0) {
                    out.println("Loaded " + list.size() + " trial collections from file.");
                    if (TRACE > 2) {
                        for (Iterator i = list.iterator(); i.hasNext();) {
                            out.println("Loaded from file: " + i.next());
                        }
                    }
                }
                allTrials.addAll(list);
            } catch (Exception e) {
                solver.err.println("Error occurred loading " + filename + ": " + e);
                e.printStackTrace();
            }
        }
        incorporateTrials();
    }

    /**
     * Incorporate all of the trials in allTrials.
     */
    void incorporateTrials() {
        for (Iterator i = allTrials.iterator(); i.hasNext();) {
            EpisodeCollection tc = (EpisodeCollection) i.next();
            constraintInfo.addTrials(tc);
        }
    }

    void incorporateTrial(Episode ep) {
        constraintInfo.addTrials(ep.getEpisodeCollection());

        if (TRACE > 2)
            dump();
    }

    /**
     * Dump the collected order info for rules and relations to standard output.
     */
    public void dump() {
        SortedSet set = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                ConstraintInfo info1 = (ConstraintInfo) ((Map.Entry) o1).getValue();
                ConstraintInfo info2 = (ConstraintInfo) ((Map.Entry) o2).getValue();
                return info1.compareTo(info2);
            }
        });
        set.addAll(constraintInfo.infos.entrySet());
        for (Iterator i = set.iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            OrderConstraint ir = (OrderConstraint) e.getKey();
            out.println("Order feature: " + ir);
            ConstraintInfo info = (ConstraintInfo) e.getValue();
            info.dump();
        }
    }

    public int getNumberOfTrials() {
        int sum = 0;
        for (Iterator i = allTrials.iterator(); i.hasNext();) {
            EpisodeCollection ec = (EpisodeCollection) i.next();
            sum += ec.getNumTrials();
        }
        return sum;
    }
    
    /**
     * Starts a new trial collection and returns it.
     * 
     * @param rule  inference rule of trial collection
     * @param opNumber  operation number of trial collection
     * @param timeStamp  time of trial collection
     * @param newCollection  whether to always return a new collection
     * @return new trial collection
     */
    public Episode getNewEpisode(BDDInferenceRule rule, int opNumber, long timeStamp, boolean newCollection) {
        EpisodeCollection c = newCollection ? findEpisodeCollection(rule, opNumber) : null;

        if(c == null){
           c = new EpisodeCollection(rule, opNumber);
           allTrials.add(c);
       }
       return c.startNewEpisode(timeStamp);
    }
   
    
    public EpisodeCollection findEpisodeCollection(BDDInferenceRule rule, int opNumber){
        for(Iterator it = allTrials.iterator(); it.hasNext(); ){
            EpisodeCollection tc = (EpisodeCollection) it.next();
            if(tc.getRule(solver) == rule && tc.getUpdateCount() == rule.updateCount && tc.getOpNumber() == opNumber){
                if(TRACE > 1) out.println("Found a tc for:  rule " + rule.id + " on update " + rule.updateCount + " and op " + opNumber);
                return tc;
            }
        }
        return null;
    }

    /**
     * Calculated information about an order.  This consists of a score
     * and an estimated information gain.
     * 
     * @author John Whaley
     * @version $Id: FindBestDomainOrder.java 552 2005-05-19 08:20:05Z cs343 $

     */
    public static class OrderInfo implements Comparable {

        /**
         * The order this information is about.
         */
        Order order;

        /**
         * A measure of how good this order is.
         */
        double score;

        /**
         * A measure of the expected information gain from running this order.
         */
        double infoGain;

        /**
         * Construct a new OrderInfo.
         * 
         * @param o  order
         * @param s  score
         * @param c  info gain
         */
        public OrderInfo(Order o, double s, double c) {
            this.order = o;
            this.score = s;
            this.infoGain = c;
        }

        /**
         * Construct a new OrderInfo that is a clone of another.
         * 
         * @param that  other OrderInfo to clone from
         */
        public OrderInfo(OrderInfo that) {
            this.order = that.order;
            this.score = that.score;
            this.infoGain = that.infoGain;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return order + ": score " + format(score) + " info gain " + format(infoGain);
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object arg0) {
            return compareTo((OrderInfo) arg0);
        }

        /**
         * Comparison operator for OrderInfo objects.  Score is most important, followed
         * by info gain.  If both are equal, we compare the order lexigraphically.
         * 
         * @param that  OrderInfo to compare to
         * @return  -1, 0, or 1 if this OrderInfo is less than, equal to, or greater than the other
         */
        public int compareTo(OrderInfo that) {
            if (this == that) return 0;
            int result = signum(that.score - this.score);
            if (result == 0) {
                result = (int) signum(this.infoGain - that.infoGain);
                if (result == 0) {
                    result = this.order.compareTo(that.order);
                }
            }
            return result;
        }

        /**
         * Returns this OrderInfo as an XML element.
         * 
         * @return XML element
         */
        public Element toXMLElement() {
            Element dis = new Element("orderInfo");
            dis.setAttribute("order", order.toString());
            dis.setAttribute("score", Double.toString(score));
            dis.setAttribute("infoGain", Double.toString(infoGain));
            return dis;
        }

        public static OrderInfo fromXMLElement(Element e, Map nameToVar) {
            Order o = Order.parse(e.getAttributeValue("order"), nameToVar);
            double s = Double.parseDouble(e.getAttributeValue("score"));
            double c = Double.parseDouble(e.getAttributeValue("infoGain"));
            return new OrderInfo(o, s, c);
        }
    }
    
    /**
     * Generate all orders of a given list of variables.
     * 
     * @param vars  list of variables
     * @return  list of all orders of those variables
     */
    public static List/*<Order>*/ generateAllOrders(List vars) {
        if (vars.size() == 0) return null;
        LinkedList result = new LinkedList();
        if (vars.size() == 1) {
            result.add(new Order(vars));
            return result;
        }
        Object car = vars.get(0);
        List recurse = generateAllOrders(vars.subList(1, vars.size()));
        for (Iterator i = recurse.iterator(); i.hasNext();) {
            Order order = (Order) i.next();
            for (int j = 0; j <= order.size(); ++j) {
                Order myOrder = new Order(order);
                myOrder.add(j, car);
                result.add(myOrder);
            }
        }
        for (Iterator i = recurse.iterator(); i.hasNext();) {
            Order order = (Order) i.next();
            for (int j = 0; j < order.size(); ++j) {
                Order myOrder = new Order(order);
                Object o = myOrder.get(j);
                List c = new LinkedList();
                c.add(car);
                if (o instanceof Collection) {
                    c.addAll((Collection) o);
                } else {
                    c.add(o);
                }
                myOrder.set(j, c);
                result.add(myOrder);
            }
        }
        return result;
    }

    transient static NumberFormat nf;

    /**
     * Format a double in a nice way.
     * 
     * @param d  double
     * @return string representation
     */
    public static String format(double d) {
        if (nf == null) {
            nf = NumberFormat.getNumberInstance();
            //nf.setMinimumFractionDigits(3);
            nf.setMaximumFractionDigits(3);
        }
        if (d == Double.MAX_VALUE) return "max";
        return nf.format(d);
    }
    
    public static String format(double d, int numFracDigits) {
        if (nf == null) {
            nf = NumberFormat.getNumberInstance();
            //nf.setMinimumFractionDigits(3);
            nf.setMaximumFractionDigits(numFracDigits);
        }
        if (d == Double.MAX_VALUE) return "max";
        return nf.format(d);
    }

    // Only present in JDK1.5
    public static int signum(long d) {
        if (d < 0) return -1;
        if (d > 0) return 1;
        return 0;
    }

    // Only present in JDK1.5
    public static int signum(double d) {
        if (d < 0) return -1;
        if (d > 0) return 1;
        return 0;
    }

    public static class ConstraintInfoCollection {

        Solver solver;

        /**
         * Map from orders to their info.
         */
        Map/* <OrderConstraint,ConstraintInfo> */infos;

        public ConstraintInfoCollection(Solver s) {
            this.solver = s;
            this.infos = new HashMap();
        }

        public ConstraintInfo getInfo(OrderConstraint c) {
            return (ConstraintInfo) infos.get(c);
        }

        public ConstraintInfo getOrCreateInfo(OrderConstraint c) {
            ConstraintInfo ci = (ConstraintInfo) infos.get(c);
            if (ci == null) infos.put(c, ci = new ConstraintInfo(c));
            return ci;
        }

        private void addTrials(EpisodeCollection tc, OrderTranslator trans) {
            MultiMap c2Trials = new GenericMultiMap();

            for (Iterator i = tc.getTrials().iterator(); i.hasNext();) {
                TrialInfo ti = (TrialInfo) i.next();
                Order o = ti.order;
                if (ti.cost >= BDDInferenceRule.LONG_TIME)
                    ((BDDSolver) solver).fbo.neverTryAgain(tc.getRule(solver), o);
                if (trans != null) o = trans.translate(o);
                Collection ocs = o.getConstraints();
                for (Iterator j = ocs.iterator(); j.hasNext();) {
                    OrderConstraint oc = (OrderConstraint) j.next();
                    c2Trials.add(oc, ti);
                }

            }

            for (Iterator i = c2Trials.keySet().iterator(); i.hasNext();) {
                OrderConstraint oc = (OrderConstraint) i.next();
                ConstraintInfo info = getOrCreateInfo(oc);
                info.registerTrials(c2Trials.getValues(oc));
            }
            
        }

        public void addTrials(EpisodeCollection tc) {
            InferenceRule ir = tc.getRule(solver);
            OrderTranslator varToAttrib = new VarToAttribTranslator(ir);
            addTrials(tc, varToAttrib);
            if (PER_RULE_CONSTRAINTS) {
                addTrials(tc, null);
            }
            if (TRACE > 2) {
                out.println("Added trial collection: " + tc);
            }
        }

        public OrderInfo predict(Order o, OrderTranslator trans) {
            if (TRACE > 2) out.println("Predicting order "+o);
            if (trans != null) o = trans.translate(o);
            if (TRACE > 2) out.println("Translated into order "+o);
            double score = 0.;
            int numTrialCollections = 0, numTrials = 0;
            Collection cinfos = new LinkedList();
            for (Iterator i = o.getConstraints().iterator(); i.hasNext();) {
                OrderConstraint c = (OrderConstraint) i.next();
                ConstraintInfo ci = getInfo(c);
                if (ci == null || ci.getNumberOfTrials() == 0) continue;
                cinfos.add(ci);
                score += ci.getWeightedMean();
                numTrialCollections++;
                numTrials += ci.getNumberOfTrials();
            }
            if (numTrialCollections == 0)
                score = 0.;
            else
                score = score / numTrialCollections;
            double infoGain = ConstraintInfo.getVariance(cinfos) / numTrials;
            if (TRACE > 2) out.println("Prediction for "+o+": score "+format(score)+" infogain "+format(infoGain));
            return new OrderInfo(o, score, infoGain);
        }

    }

    public OrderInfo predict(Order o, OrderTranslator trans) {
        return constraintInfo.predict(o, trans);
    }

    /**
     * Returns this FindBestDomainOrder as an XML element.
     * 
     * @return XML element
     */
    public Element toXMLElement() {
        Element constraintInfoCollection = new Element("constraintInfoCollection");
        for (Iterator i = constraintInfo.infos.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            OrderConstraint oc = (OrderConstraint) e.getKey();
            ConstraintInfo c = (ConstraintInfo) e.getValue();
            Element constraintInfo = c.toXMLElement(solver);
            constraintInfoCollection.addContent(constraintInfo);
        }

        Element fbo = new Element("findBestOrder");
        fbo.addContent(constraintInfoCollection);

        return fbo;
    }

    /**
     * Returns the set of all trials performed so far as an XML element.
     * 
     * @return XML element
     */
    public Element trialsToXMLElement() {
        Element trialCollections = new Element("episodeCollections");
        if (solver.inputFilename != null)
            trialCollections.setAttribute("datalog", solver.inputFilename);
        for (Iterator i = allTrials.iterator(); i.hasNext();) {
            EpisodeCollection c = (EpisodeCollection) i.next();
            trialCollections.addContent(c.toXMLElement());
        }
        return trialCollections;
    }

    /**
     */
    public boolean hasOrdersToTry(List allVars, BDDInferenceRule ir) {
        // TODO: improve this code.
        int nTrials = getNumberOfTrials();
        if (nTrials != ir.lastTrialNum) {
            ir.lastTrialNum = nTrials;
            TrialGuess g = this.tryNewGoodOrder(null, allVars, ir, -2, null, false);
            return g != null;
        } else {
            return false;
        }
    }

    // Since JDK1.4 only.
    public static final int compare(double d1, double d2) {
        if (d1 < d2)
            return -1; // Neither val is NaN, thisVal is smaller
        if (d1 > d2)
            return 1; // Neither val is NaN, thisVal is larger

        long thisBits = Double.doubleToLongBits(d1);
        long anotherBits = Double.doubleToLongBits(d2);

        return (thisBits == anotherBits ? 0 : // Values are equal
                (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                        1)); // (0.0, -0.0) or (NaN, !NaN)
    }



    public final static int WEIGHT_WINDOW_SIZE = Integer.MAX_VALUE;

    public final static double DECAY_FACTOR = -.1;

    public double computeWeight(int type, TrialInstances instances) {
        int numTrials = 0;
        double total = 0;
        int losses = 0;
        double weight = 1;
        for (int i = instances.numInstances() - 1; i >= 0 && numTrials < WEIGHT_WINDOW_SIZE; --i) {
            TrialInstance instance = (TrialInstance) instances.instance(i);
            double trueCost = instance.getCost();

            TrialPrediction pred = instance.getTrialInfo().pred;
            double predCost = pred.predictions[type][TrialPrediction.LOW];
            double dev = pred.predictions[type][TrialPrediction.HIGH];

            if(predCost == -1) continue;
            double trialWeight = Math.exp(DECAY_FACTOR * numTrials);
            if (trueCost < predCost - dev || trueCost > predCost + dev) {
                losses += trialWeight;
            }
            total += trialWeight;
            ++numTrials;
        }
        if (numTrials != 0) {
            weight = 1 - losses / (double) total;
        }
        return weight;
    }

    public void adjustWeights(TrialInstances vData, TrialInstances aData, TrialInstances dData) {
        if (vData != null)
            varClassWeight = computeWeight(TrialPrediction.VARIABLE, vData);
        if (aData != null)
            attrClassWeight = computeWeight(TrialPrediction.ATTRIBUTE, aData);
        if (dData != null)
            domClassWeight = computeWeight(TrialPrediction.DOMAIN, dData);
    }

    public static int NUM_CV_FOLDS = 10;

     /**
     * @param data
     * @param cClassName
     * @return Cross validation with number of folds as set by NUM_CV_FOLDS;
     */
    public double constFoldCV(Instances data, String cClassName) {
        return WekaInterface.cvError(NUM_CV_FOLDS, data, cClassName);
    }


    public static boolean DISCRETIZE1 = true;
    public static boolean DISCRETIZE2 = true;
    public static boolean DISCRETIZE3 = true;
    public static String CLASSIFIER1 = "net.sf.bddbddb.order.MyId3";
    public static String CLASSIFIER2 = "net.sf.bddbddb.order.MyId3";
    public static String CLASSIFIER3 = "net.sf.bddbddb.order.MyId3";

    
    public void neverTryAgain(InferenceRule ir, Order o) {
          if (true) {
            if (TRACE > 2) out.println("For rule"+ir.id+", never trying order "+o+" again.");
            neverAgain.add(ir, o);
        }
    }

    MultiMap neverAgain = new GenericMultiMap();

    public double varClassWeight = 1;
    public double attrClassWeight = 1;
    public double domClassWeight = 1;
    public static int DOMAIN_THRESHOLD = 1000;
    public static int NO_CLASS = -1;
    public static int NO_CLASS_SCORE = -1;
    public boolean PROBABILITY = false;
    void dumpClassifierInfo(String name, Classifier c, Instances data) {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(name));
            w.write("Classifier \"name\":\n");
            w.write("Attributes: \n");
            for(Enumeration e = data.enumerateAttributes(); e.hasMoreElements(); ){
                w.write(e.nextElement() +"\n");
            }
            w.write("\n");
            w.write("Based on data from "+data.numInstances()+" instances:\n");
            for (Enumeration e = data.enumerateInstances(); e.hasMoreElements(); ) {
                Instance i = (Instance) e.nextElement();

                if (i instanceof TrialInstance) {
                    TrialInstance ti = (TrialInstance) i;
                    InferenceRule ir = ti.ti.getCollection().getRule(solver);
                    w.write("    "+ti.ti.getCollection().name+" "+ti.getOrder());
                    if (!ti.getOrder().equals(ti.ti.order))
                        w.write(" ("+ti.ti.order+")");
                    if (ti.isMaxTime()) {
                        w.write(" MAX TIME\n");
                    } else {
                        w.write(" "+format(ti.getCost())+" ("+ti.ti.cost+" ms)\n");
                    }
                } else {
                    w.write("    "+i+"\n");
                }
            }
            w.write(c.toString());
            w.write("\n");
        } catch (IOException x) { 
            solver.err.println("IO Exception occurred writing \""+name+"\": "+x);
        } finally {
            if (w != null) try { w.close(); } catch (IOException _) { }
        }
    }
    
    void dumpTrialGuessInfo(String name) {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(name, true));
            w.write("Classifier \"name\":\n");
            w.write("\n");
        } catch (IOException x) { 
            solver.err.println("IO Exception occurred writing \""+name+"\": "+x);
        } finally {
            if (w != null) try { w.close(); } catch (IOException _) { }
        }
    }

    
    private void addTrial(InferenceRule rule, List variables, Episode ep, Order o, TrialPrediction prediction, long time, long timestamp) {
        
        TrialInfo info = new TrialInfo(o,prediction,time,ep, timestamp);
      /*  tc.addTrial(o,guess.prediction, time);
       */
        ep.addTrial(info);
        dataRepository.addTrial(rule, variables, info);
    }
    
    public static int INITIAL_VAR_SET = 10;
    public static int INITIAL_ATTRIB_SET = 16;
    public static int INITIAL_DOM_SET = 10;
    public TrialGuess tryNewGoodOrder(Episode ep, List allVars, InferenceRule ir, int opNum,
            boolean returnBest) {
        return tryNewGoodOrder(ep.getEpisodeCollection(), allVars, ir, opNum, null, returnBest);
    }
    
    public TrialGuess tryNewGoodOrder(EpisodeCollection ec, List allVars, InferenceRule ir, int opNum,
            Order chosenOne,
            boolean returnBest) {

        out.println("Variables: " + allVars);
        TrialDataGroup vDataGroup = this.dataRepository.getVariableDataGroup(ir, allVars);
        TrialDataGroup aDataGroup = dataRepository.getAttribDataGroup(ir,allVars);
        TrialDataGroup dDataGroup = dataRepository.getDomainDataGroup(ir,allVars);
        
        // Build instances based on the experimental data.
        TrialInstances vData, aData, dData;
        vData = vDataGroup.getTrialInstances();
        aData = aDataGroup.getTrialInstances();
        dData = dDataGroup.getTrialInstances();
/* 
        TrialInstances vTest = dataRepository.buildVarInstances(ir, allVars);

        Assert._assert(vData.numInstances() == vTest.numInstances(),"vGot " + vData.numInstances() + " Wanted: " + vTest.numInstances());
        TrialInstances aTest = dataRepository.buildAttribInstances(ir, allVars);
  
        Assert._assert(aData.numInstances() == aTest.numInstances(), "aGot: " + aData.numInstances() + " Wanted: " + aTest.numInstances());
    
        TrialInstances dTest =dataRepository.buildDomainInstances(ir, allVars);
      
        Assert._assert(dData.numInstances() == dTest.numInstances(), "dGot: " + dData.numInstances() + " Wanted: " + dTest.numInstances());
        out.println(aData);
        out.println(vData);
        out.println(dData);
*/
        // Readjust the weights using an exponential decay factor.
        adjustWeights(vData, aData, dData);
        Discretization vDis = null, aDis = null, dDis = null;

        /*
       // Discretize the experimental data.  null if there is no data.
        if (DISCRETIZE1) vDis = vData.discretize(.5);
        if (DISCRETIZE2) aDis = aData.discretize(.25);
        if (DISCRETIZE3) dDis = dData.threshold(DOMAIN_THRESHOLD);
 */
        vDis = vDataGroup.discretize(.5);
        aDis = aDataGroup.discretize(.25);
        dDis = dDataGroup.threshold(DOMAIN_THRESHOLD);
        // Calculate the accuracy of each classifier using cv folds.
        long vCTime = System.currentTimeMillis();
        double vConstCV = -1;//constFoldCV(vData, CLASSIFIER1);
        vCTime = System.currentTimeMillis() - vCTime;

        long aCTime = System.currentTimeMillis();
        double aConstCV = -1;//constFoldCV(aData, CLASSIFIER2);
        aCTime = System.currentTimeMillis() - aCTime;
        
        long dCTime = System.currentTimeMillis();
        double dConstCV = -1;//constFoldCV(dData, CLASSIFIER3);
        dCTime = System.currentTimeMillis() - dCTime;
        
        long vLTime = System.currentTimeMillis();
        double vLeaveCV = -1; //leaveOneOutCV(vData, CLASSIFIER1);
        vLTime = System.currentTimeMillis() - vLTime;
        
        long aLTime = System.currentTimeMillis();
        double aLeaveCV = -1; //leaveOneOutCV(aData, CLASSIFIER2);
        aLTime = System.currentTimeMillis() - aLTime;
        
        long dLTime = System.currentTimeMillis();
        double dLeaveCV = -1; //leaveOneOutCV(dData, CLASSIFIER3);
        dLTime = System.currentTimeMillis() - dLTime;
        
        if (TRACE > 1) {
            out.println(" Var data points: " + vData.numInstances());
            //out.println(" Var Classifier " + NUM_CV_FOLDS + " fold CV Score: " + vConstCV + " took " + vCTime + " ms");
           // out.println(" Var Classifier leave one out CV Score: " + vLeaveCV + " took " + vLTime + " ms");
            out.println(" Var Classifier Weight: " + varClassWeight);
            //out.println(" Var data points: "+vData);
            out.println(" Attrib data points: " + aData.numInstances());
           // out.println(" Attrib Classifier " + NUM_CV_FOLDS + " fold CV Score : " + aConstCV + " took " + aCTime + " ms");
            //out.println(" Attrib Classifier leave one out CV Score: " + aLeaveCV + " took " + aLTime + " ms");
            out.println(" Attrib Classifier Weight: " + attrClassWeight);
            //out.println(" Attrib data points: "+aData);
            out.println(" Domain data points: " + dData.numInstances());
            //out.println(" Domain Classifier " + NUM_CV_FOLDS + " fold CV Score: " + dConstCV + " took " + dCTime + " ms");
            //out.println(" Attrib Classifier leave one out CV Score: " + dLeaveCV + " took " + dLTime + " ms");
            out.println(" Domain Classifier Weight: " + domClassWeight);
            //out.println(" Domain data points: "+dData);

        }

        Classifier vClassifier = null, aClassifier = null, dClassifier = null;
        // Build the classifiers.
   /*    
        if (vData.numInstances() > 0)
            vClassifier = WekaInterface.buildClassifier(CLASSIFIER1, vData);
        if (aData.numInstances() > 0)
            aClassifier = WekaInterface.buildClassifier(CLASSIFIER2, aData);
        if (dData.numInstances() > 0)
            dClassifier = WekaInterface.buildClassifier(CLASSIFIER3, dData);
 */
        vClassifier = vDataGroup.classify();
        aClassifier = aDataGroup.classify();
        dClassifier = dDataGroup.classify();
       
        
        if (DUMP_CLASSIFIER_INFO) {
            String baseName = solver.getBaseName()+"_rule"+ir.id;
            if (vClassifier != null)
                dumpClassifierInfo(baseName+"_vclassifier", vClassifier, vData);
            if (aClassifier != null)
                dumpClassifierInfo(baseName+"_aclassifier", aClassifier, aData);
            if (dClassifier != null)
                dumpClassifierInfo(baseName+"_dclassifier", dClassifier, dData);
            try {
                out_t = new PrintStream(new FileOutputStream(baseName+"_trials"));
            } catch (IOException x) {
                solver.err.println("Error while opening file: "+x);
            }
        } else {
            out_t = null;
        }
        
        if (TRACE > 2) {
            out.println("Var classifier: " + vClassifier);
            out.println("Attrib classifier: " + aClassifier);
            out.println("Domain classifier: " + dClassifier);
        }

       double [][] bucketmeans = getBucketMeans(vDis, aDis, dDis);

       Collection sel = null;
       Collection candidates = null;
        if(chosenOne == null){
            Collection triedOrders = returnBest ? new LinkedList() : getTriedOrders((BDDInferenceRule) ir, opNum);
            if(ec != null){
                triedOrders.addAll(ec.trials.keySet());
              
            }
            Object object = generateCandidateSet( ir, allVars, bucketmeans, 
                    vDataGroup, aDataGroup, dDataGroup,
                    triedOrders, returnBest);
            /*vClassifier,
            aClassifier, dClassifier, vData,
            aData, dData, vDis, aDis,
            dDis,*/
            if(object == null) return null;
            else if(object instanceof Collection)
                candidates = (Collection) object;
            else if(object instanceof TrialGuess)
                return (TrialGuess) object;
        }else {
            sel = Collections.singleton(chosenOne);
        }
        boolean force = (ec != null && ec.getNumTrials() < 2) ||
            vData.numInstances() < INITIAL_VAR_SET ||
            aData.numInstances() < INITIAL_ATTRIB_SET ||
            dData.numInstances() < INITIAL_DOM_SET;
        
        if (!returnBest)
            sel = selectOrder(candidates, vData, aData, dData, ir, force);
        
        if (sel == null || sel.isEmpty()) return null;
        Order o_v = (Order) sel.iterator().next();
        try {
            OrderTranslator v2a = new VarToAttribTranslator(ir);
            OrderTranslator a2d = AttribToDomainTranslator.INSTANCE;
            double vClass = 0, aClass = 0, dClass = 0;
            if (vClassifier != null) {
                OrderInstance vInst = OrderInstance.construct(o_v, vData);
                vClass = vClassifier.classifyInstance(vInst);
            }
            Order o_a = v2a.translate(o_v);
            if (aClassifier != null) {
                OrderInstance aInst = OrderInstance.construct(o_a, aData);
                aClass = aClassifier.classifyInstance(aInst);
            }
            Order o_d = a2d.translate(o_a);
            if (dClassifier != null) {
                OrderInstance dInst = OrderInstance.construct(o_d, dData);
                dClass = dClassifier.classifyInstance(dInst);
            }
            int vi = (int) vClass, ai = (int) aClass, di = (int) dClass;
            double vScore = 0, aScore = 0, dScore = 0;
            if (vi < bucketmeans[VMEAN_INDEX].length) vScore = bucketmeans[VMEAN_INDEX][vi];
            if (ai < bucketmeans[AMEAN_INDEX].length) aScore = bucketmeans[AMEAN_INDEX][ai];
            if (di < bucketmeans[DMEAN_INDEX].length) dScore = bucketmeans[DMEAN_INDEX][di];
            double score = varClassWeight * vScore;
            score += attrClassWeight * aScore;
            score += domClassWeight * dScore;
            return genGuess(o_v, score, vClass, aClass, dClass, vDis, aDis, dDis);
        } catch (Exception x) {
            x.printStackTrace();
            Assert.UNREACHABLE(x.toString());
            return null;
        }
    }
    
    public final static int VMEAN_INDEX = 0;
    public final static int AMEAN_INDEX = 1;
    public final static int DMEAN_INDEX = 2;
    public double[][] getBucketMeans(Discretization vDis, Discretization aDis, Discretization dDis){
        // Calculate the mean value of each of the discretized buckets.

        double[] vBucketMeans = new double[vDis == null ? 0 : vDis.buckets.length];
        double[] aBucketMeans = new double[aDis == null ? 0 : aDis.buckets.length];
        double[] dBucketMeans = new double[dDis == null ? 0 : dDis.buckets.length];
        if(TRACE > 2) out.print("Var Bucket Means: ");
        for (int i = 0; i < vBucketMeans.length; ++i) {
            if (vDis.buckets[i].numInstances() == 0)
                vBucketMeans[i] = Double.MAX_VALUE;
            else
                vBucketMeans[i] = vDis.buckets[i].meanOrMode(vDis.buckets[i].classIndex());
            if(TRACE > 2) out.print(vBucketMeans[i] + " ");
        }
        if (TRACE > 2) {
            out.println();
            out.print("Attr Bucket Means: ");
        }
        for (int i = 0; i < aBucketMeans.length; ++i) {
            if (aDis.buckets[i].numInstances() == 0)
                aBucketMeans[i] = Double.MAX_VALUE;
            else
                aBucketMeans[i] = aDis.buckets[i].meanOrMode(aDis.buckets[i].classIndex());
            if(TRACE > 2) out.print(aBucketMeans[i] + " ");
        }
        if (TRACE > 2) {
            out.println();
            out.print("Domain Bucket Means: ");
        }
        for (int i = 0; i < dBucketMeans.length; ++i) {
            if (dDis.buckets[i].numInstances() == 0)
                dBucketMeans[i] = Double.MAX_VALUE;
            else
                dBucketMeans[i] = dDis.buckets[i].meanOrMode(dDis.buckets[i].classIndex());
            if(TRACE > 2) out.print(dBucketMeans[i] + " ");
        }
        if(TRACE > 2) out.println();
        double [][] means = new double[3][];
        
        means[VMEAN_INDEX] = vBucketMeans;
        means[AMEAN_INDEX] = aBucketMeans;
        means[DMEAN_INDEX] = dBucketMeans;
        
        return means;
    }
    
    public int getCombos(double[][] combos, int start, int numV, int numA, int numD,
            double vBuckets, double aBuckets, double dBuckets,
            double [][] means,
            double maxScore){
        double [] vBucketMeans = means[VMEAN_INDEX];
        double [] aBucketMeans = means[AMEAN_INDEX];
        double [] dBucketMeans = means[DMEAN_INDEX];
        
        int p = 0;
        for (int vi = start; vi < numV; ++vi) {
            for (int ai = start; ai < numA; ++ai) {
                for (int di = 0; di < numD; ++di) { // don't do nulls for domain classifier.
                    double vScore, aScore, dScore;
                    double nullScore = 1;
                    if (vi == -1) vScore = nullScore;
                    else if (vi < vBuckets && vi < vBucketMeans.length) vScore = vBucketMeans[vi];
                    else vScore = maxScore;
                    if (ai == -1) aScore = nullScore;
                    else if (ai < aBuckets && ai < aBucketMeans.length) aScore = aBucketMeans[ai];
                    else aScore = maxScore;
                    if (di == -1) dScore = nullScore;
                    else if (di < dBuckets && di < dBucketMeans.length) dScore = dBucketMeans[di];
                    else dScore = maxScore;
                    double score = varClassWeight * vScore;
                    score += attrClassWeight * aScore;
                    score += domClassWeight * dScore;
                    double[] result = new double[] { score, vi==-1?Double.NaN:vi,
                                                            ai==-1?Double.NaN:ai,
                                                            di==-1?Double.NaN:di };
                    if (TRACE > 2) {
                        out.println("Score for v="+vi+" a="+ai+" d="+di+": "+format(score));
                    }
                    combos[p++] = result;
                }
            }
        }
        Arrays.sort(combos, 0, p, new Comparator() {
            public int compare(Object arg0, Object arg1) {
                double[] a = (double[]) arg0;
                double[] b = (double[]) arg1;
                return FindBestDomainOrder.compare(a[0], b[0]);
            }
        });
        
        return p;
    }

    
    public Object generateCandidateSet(InferenceRule ir, List allVars, double [][] means, /* double [] vBucketMeans,
            double[] aBucketMeans, double [] dBucketMeans, */TrialDataGroup vDataGroup,
            TrialDataGroup aDataGroup, TrialDataGroup dDataGroup, Collection triedOrders, boolean returnBest){
        
        double [] vBucketMeans = means[VMEAN_INDEX];
        double [] aBucketMeans = means[AMEAN_INDEX];
        double [] dBucketMeans = means[DMEAN_INDEX];
        
        // Build multi-map from attributes/domains to variables.
        MultiMap a2v, d2v;
        a2v = new GenericMultiMap();
        d2v = new GenericMultiMap();
        for (Iterator i = allVars.iterator(); i.hasNext();) {
            Variable v = (Variable) i.next();
            Attribute a = (Attribute) ir.getAttribute(v);
            if (a != null) {
                a2v.add(a, v);
                d2v.add(a.getDomain(), v);
            }
        }
        
        Set candidates = null;
            boolean addNullValues = !returnBest;
            // Grab the best from the classifiers and try to build an optimal order.
            if (!returnBest) candidates = new LinkedHashSet();
            Collection never = neverAgain.getValues(ir);
            //MyId3 v = (MyId3) vClassifier, a = (MyId3) aClassifier, d = (MyId3) dClassifier;
            Discretization vDis = vDataGroup.getDiscretization();
            Discretization aDis = aDataGroup.getDiscretization();
            Discretization dDis = dDataGroup.getDiscretization();
            
            int end = 5;
            // Use only top half of buckets.
            int vBuckets = vDis == null ? 1 : vDis.buckets.length / 2 + 1;
            int aBuckets = aDis == null ? 1 : aDis.buckets.length / 2 + 1;
            int dBuckets = dDis == null ? 1 : dDis.buckets.length / 2 + 1;
            double max = (vBucketMeans.length != 0 ? vBucketMeans[vBucketMeans.length-1] : 0);
            max += (aBucketMeans.length != 0 ? aBucketMeans[aBucketMeans.length-1] : 0);
            max += (dBucketMeans.length != 0 ? dBucketMeans[dBucketMeans.length-1] : 0);
            boolean[][][] done = new boolean[vBuckets+1][aBuckets+1][dBuckets+1];
        outermost:
            while (candidates == null || candidates.size() < CANDIDATE_SET_SIZE) {
                int numV = Math.min(end, vBuckets);
                int numA = Math.min(end, aBuckets);
                int numD = Math.min(end, dBuckets);
                if (true && end > vBuckets && end > aBuckets && end > dBuckets) {
                    // Also include the "empty" classification for all of them.
                    numV++; numA++; numD++;
                }
                int maxNum = addNullValues ? ((numV+1)*(numA+1)*(numD+1)) : (numV*numA*numD);
                double[][] combos = new double[maxNum][];
                int start = addNullValues ? -1 : 0; 
                int p = getCombos(combos,start,numV, numA, numD, vBuckets, aBuckets, dBuckets,
                        means, max);
              
                for (int z = 0; z < p; ++z) {
                    double bestScore = combos[z][0];
                    double vClass = combos[z][1]; int vi = (int) vClass;
                    double aClass = combos[z][2]; int ai = (int) aClass;
                    double dClass = combos[z][3]; int di = (int) dClass;
                    // If one of them reaches the highest index, we need to break.
                    if (vi == numV-1 && end <= vBuckets ||
                        ai == numA-1 && end <= aBuckets ||
                        di == numD-1 && end <= dBuckets) {
                        if (TRACE > 1) out.println("reached end ("+vi+","+ai+","+di+"), trying again with a higher cutoff.");
                        break;
                    }
                    if (!Double.isNaN(vClass) && !Double.isNaN(aClass) && !Double.isNaN(dClass)) {
                        if (done[vi][ai][di]) continue;
                        done[vi][ai][di] = true;
                    } else {
                        addNullValues = false;
                    }
                    if (vi == vBuckets) vClass = -1; // any
                    if (ai == aBuckets) aClass = -1;
                    if (di == dBuckets) dClass = -1;
                    if (out_t != null) out_t.println("v="+vClass+" a="+aClass+" d="+dClass+": "+format(bestScore));
                    Collection ocss = tryConstraints(vDataGroup, vClass, aDataGroup, aClass, dDataGroup, dClass, a2v, d2v);
                        //tryConstraints(v, vClass, vData, a, aClass, aData, d, dClass, dData, a2v, d2v);
                    if (ocss == null || ocss.isEmpty()) {
                        if (out_t != null) out_t.println("Constraints cannot be combined.");
                        continue;
                    }
                   // Collection triedOrders = getTriedOrders((BDDInferenceRule)ir);
                    for (Iterator i = ocss.iterator(); i.hasNext(); ) {
                        OrderConstraintSet ocs = (OrderConstraintSet) i.next();
                        if (out_t != null) out_t.println("Constraints: "+ocs);
                        if (returnBest) {
                            TrialGuess guess = genGuess(ocs, bestScore, allVars, bestScore, vClass, aClass, dClass,
                                vDis, aDis, dDis, triedOrders, /*tc,*/ never);
                            if (guess != null) {
                                if (TRACE > 1) out.println("Best Guess: "+guess);
                                return guess;
                            }
                        } else {
                            // Add these orders to the collection.
                            //genOrders(ocs, allVars, tc == null ? null : tc.trials.keySet(), never, candidates);
                            genOrders(ocs, allVars, triedOrders, never, candidates);
                            if (candidates.size() >= CANDIDATE_SET_SIZE) break outermost;
                        }
                    }
                }
                if (end > vBuckets && end > aBuckets && end > dBuckets) {
                    if (TRACE > 1) out.println("Reached end, no more possible guesses!");
                 /*   if (false) {
                        // TODO: we can do something better here!
                        OrderIterator i = new OrderIterator(allVars);
                        while (i.hasNext()) {
                            Order o_v = i.nextOrder();
                            if (tc != null && tc.contains(o_v)) continue;
                            if (never != null && never.contains(o_v)) continue;
                            if (TRACE > 1) out.println("Just trying "+o_v);
                            if (returnBest) {
                                sel = Collections.singleton(o_v);
                                break outermost;
                            } else {
                                // Add this order to the collection.
                                if (TRACE > 1) out.println("Adding to candidate set: "+o_v);
                                candidates.add(o_v);
                                if (candidates.size() >= CANDIDATE_SET_SIZE) break outermost;
                            }
                        }
                    }
                    */
                    if (returnBest) {
                        return null;
                    }
                    break outermost;
                }
                end *= 2;
                if (TRACE > 1) out.println("Cutoff is now "+end);
            }
        
        
        return candidates;
    }
    public static int CANDIDATE_SET_SIZE = Integer.parseInt(SystemProperties.getProperty("candidateset", "500"));
    public static int SAMPLE_SIZE = 1;
    public static double UNCERTAINTY_THRESHOLD = Double.parseDouble(SystemProperties.getProperty("uncertainty", ".25"));
    public static boolean WEIGHT_UNCERTAINTY_SAMPLE = false;
    public static double VCENT = .5, ACENT = .5, DCENT = 1;
    static CandidateSampler candidateSetSampler = new UncertaintySampler(SAMPLE_SIZE, UNCERTAINTY_THRESHOLD, VCENT, ACENT, DCENT);
    
    public Collection selectOrder(Collection orders,
            TrialInstances vData, TrialInstances aData, TrialInstances dData, InferenceRule ir, boolean force) {
        Assert._assert(orders != null); //catch error if happens
        if(orders.size() == 0){
            if(TRACE > 1) out.println("Size of candidate set is 0. No orders to select from");
            return null;
        }
        if (TRACE > 1) out.println("Selecting an order from a candidate set of "+orders.size()+" orders.");
        if (TRACE > 2) out.println("Orders: "+orders);
       
        return candidateSetSampler.sample(orders, vData, aData, dData, ir, force);  
    }
    

    
    /**
     * Returns all the orders that have been tried on a particular rule update
     * (including those in previous runs).
     * 
     * @param rule
     * @return  collection of tried orders
     */
    Collection getTriedOrders(BDDInferenceRule rule, int opNumber){
        Collection triedOrders = new LinkedList();
      for(Iterator it = allTrials.iterator(); it.hasNext(); ){
          EpisodeCollection ec = (EpisodeCollection) it.next();
          if(ec.getRule(solver) == rule && ec.getUpdateCount() == rule.updateCount && ec.getOpNumber() == opNumber)
              triedOrders.addAll(ec.trials.keySet());
      }
      if(TRACE > 2) out.println("Tried Orders: " + triedOrders);
      return triedOrders;  
    }
    static void genOrders(OrderConstraintSet ocs, List allVars, Collection already, Collection never, Collection result) {
        if (out_t != null) out_t.println("Generating orders for "+allVars);
        List orders;
        int nOrders = ocs.approxNumOrders(allVars.size());
        if (nOrders > CANDIDATE_SET_SIZE*20) {
            if (out_t != null) out_t.println("Too many possible orders ("+nOrders+")!  Using random sampling.");
            orders = new LinkedList();
            for (int i = 0; i < CANDIDATE_SET_SIZE; ++i) {
                orders.add(ocs.generateRandomOrder(allVars));
            }
        } else {
            if (out_t != null) out_t.println("Estimated "+nOrders+" orders.");
            orders = ocs.generateAllOrders(allVars);
        }
        for (Iterator m = orders.iterator(); m.hasNext(); ) {
            Order best = (Order) m.next();
            if (never.contains(best)) {
                if (out_t != null) out_t.println("Skipped order "+best+" because it has blown up before.");
                continue;
            }
            if (already == null || !already.contains(best)) {
                if (out_t != null) out_t.println("Adding to candidate set: "+best);
                result.add(best);
                if (result.size() > CANDIDATE_SET_SIZE) {
                    if (out_t != null) out_t.println("Candidate set full.");
                    return;
                }
            } else {
                if (out_t != null) out_t.println("We have already tried order "+best);
            }
        }
    }
     
    Collection /*Pair*/ genConstaints( int num, MultiMap a2v, MultiMap d2v,InferenceRule ir,TrialDataGroup vDataGroup,
            TrialDataGroup aDataGroup,  TrialDataGroup dDataGroup){
        Discretization  vDis = vDataGroup.getDiscretization();
        Discretization aDis = aDataGroup.getDiscretization();
        Discretization dDis = dDataGroup.getDiscretization();
 
        double [][] means = getBucketMeans(vDis,aDis,dDis);
        int numVBuckets = means[VMEAN_INDEX].length;
        int numABuckets = means[AMEAN_INDEX].length;
        int numDBuckets = means[DMEAN_INDEX].length;
        int maxNum = numVBuckets * numABuckets * numDBuckets;
        double [][] combos = new double[maxNum][];
        double [] vBucketMeans = means[VMEAN_INDEX]; 
        double [] aBucketMeans = means[AMEAN_INDEX];
        double [] dBucketMeans = means[DMEAN_INDEX];
        double maxScore = (vBucketMeans.length != 0 ? vBucketMeans[vBucketMeans.length-1] : 0);
        maxScore += (aBucketMeans.length != 0 ? aBucketMeans[aBucketMeans.length-1] : 0);
        maxScore += (dBucketMeans.length != 0 ? dBucketMeans[dBucketMeans.length-1] : 0);
        int numCombos = getCombos(combos, 0, numVBuckets, numABuckets, numDBuckets,
                          numVBuckets, numABuckets, numDBuckets, means, maxScore);
        
        Collection allPairs = new LinkedList();
        int numAdded = 0;
        for(int i = 0; i < numCombos && numAdded < num; ++i){
           double [] combo = combos[i];
           Collection constraints = tryConstraints(vDataGroup, combo[1], aDataGroup, combo[2], dDataGroup,combo[3],a2v,d2v);
           if(constraints == null) continue;
           for(Iterator jt = constraints.iterator(); jt.hasNext(); ){
               allPairs.add(new Pair(new Double(combo[0]) , jt.next()));
               ++numAdded;
           }
          
        }
        
        return allPairs;
    }
  
    static TrialGuess genGuess(Order best, double score,
            double vClass, double aClass, double dClass,
            Discretization vDis, Discretization aDis, Discretization dDis) {
        double vLowerBound, vUpperBound, aLowerBound, aUpperBound, dLowerBound, dUpperBound;
        vLowerBound = vUpperBound = aLowerBound = aUpperBound = dLowerBound = dUpperBound = -1;

        if (vDis != null && !Double.isNaN(vClass) && vClass != NO_CLASS) {
            vLowerBound = vDis.cutPoints == null || vClass <= 0 ? 0 : vDis.cutPoints[(int) vClass - 1];
            vUpperBound = vDis.cutPoints == null || vClass == vDis.cutPoints.length ? Double.MAX_VALUE : vDis.cutPoints[(int) vClass];
        }
        if (aDis != null && !Double.isNaN(aClass) && aClass != NO_CLASS) {
            aLowerBound = aDis.cutPoints == null || aClass <= 0 ? 0 : aDis.cutPoints[(int) aClass - 1];
            aUpperBound = aDis.cutPoints == null || aClass == aDis.cutPoints.length ? Double.MAX_VALUE : aDis.cutPoints[(int) aClass];
        }
        if (dDis != null && !Double.isNaN(dClass) && dClass != NO_CLASS) {
            dLowerBound = dDis.cutPoints == null || dClass <= 0 ? 0 : dDis.cutPoints[(int) dClass - 1];
            dUpperBound = dDis.cutPoints != null || dClass == dDis.cutPoints.length ? Double.MAX_VALUE : dDis.cutPoints[(int) dClass];
        }
        TrialPrediction prediction = new TrialPrediction(score, vLowerBound,vUpperBound,aLowerBound, aUpperBound,dLowerBound,dUpperBound);
        return new TrialGuess(best, prediction);
    }
    
    static TrialGuess genGuess(OrderConstraintSet ocs, double score, List allVars, double bestScore,
        double vClass, double aClass, double dClass,
        Discretization vDis, Discretization aDis, Discretization dDis,
        /* EpisodeCollection tc,*/ Collection triedOrders, Collection never) {
        if (out_t != null) out_t.println("Generating orders for "+allVars);
        // Choose a random one first.
        Order best = ocs.generateRandomOrder(allVars);
        Iterator m = Collections.singleton(best).iterator();
        boolean exhaustive = true;
        while (m.hasNext()) {
            best = (Order) m.next();
            if (never.contains(best)) {
                if (out_t != null) out_t.println("Skipped order "+best+" because it has blown up before.");
                continue;
            }   
                
            //if (tc == null || !tc.contains(best)) {
            if(!triedOrders.contains(best)){
                if (out_t != null) out_t.println("Using order "+best);
                return genGuess(best, score, vClass, aClass, dClass, vDis, aDis, dDis);
            } else {
                if (out_t != null) out.println("We have already tried order "+best);
            }
            if (exhaustive) {
                List orders = ocs.generateAllOrders(allVars);
                m = orders.iterator();
                exhaustive = false;
            }
       }
        return null;
    }

    static Collection/*OrderConstraintSet*/ tryConstraints(
            /* MyId3 v, double vClass, Instances vData,
            MyId3 a, double aClass, Instances aData,
            MyId3 d, double dClass, Instances dData,
            */
            TrialDataGroup vDataGroup, double vClass,
            TrialDataGroup aDataGroup, double aClass,
            TrialDataGroup dDataGroup, double dClass,
            MultiMap a2v, MultiMap d2v) {
        Collection results = new LinkedList();
        Instances vData = vDataGroup.getTrialInstances();
        Instances aData = aDataGroup.getTrialInstances();
        Instances dData = dDataGroup.getTrialInstances();
        MyId3 v = (MyId3) vDataGroup.getClassifier();
        MyId3 a = (MyId3) aDataGroup.getClassifier();
        MyId3 d = (MyId3) dDataGroup.getClassifier();
        Collection vBestAttribs;
        if ((vClass >= 0 || Double.isNaN(vClass)) && v != null)
            vBestAttribs = v.getAttribCombos(vData.numAttributes(), vClass);
        else
            vBestAttribs = Collections.singleton(makeEmptyConstraint());
        if (vBestAttribs == null) return null;
        for (Iterator v_i = vBestAttribs.iterator(); v_i.hasNext(); ) {
            double[] v_c = (double[]) v_i.next();
            OrderConstraintSet ocs = new OrderConstraintSet();
            boolean v_r = constrainOrder(ocs, v_c, vData, null);
            if (!v_r) {
                continue;
            }
            if (out_t != null) out_t.println(" Order constraints (var="+(int)vClass+"): "+ocs);

            Collection aBestAttribs;
            if ((aClass >= 0 || Double.isNaN(aClass)) && a != null)
                aBestAttribs = a.getAttribCombos(aData.numAttributes(), aClass);
            else
                aBestAttribs = Collections.singleton(makeEmptyConstraint());
            if (aBestAttribs == null) continue;
            for (Iterator a_i = aBestAttribs.iterator(); a_i.hasNext(); ) {
                double[] a_c = (double[]) a_i.next();
                OrderConstraintSet ocsBackup = null;
                if (a_i.hasNext()) ocsBackup = ocs.copy();
                boolean a_r = constrainOrder(ocs, a_c, aData, a2v);
                if (!a_r) {
                    ocs = ocsBackup;
                    continue;
                }
                if (out_t != null) out_t.println("  Order constraints (attrib="+(int)aClass+"): "+ocs);

                Collection dBestAttribs;
                if ((dClass >= 0 || Double.isNaN(dClass)) && d != null)
                    dBestAttribs = d.getAttribCombos(dData.numAttributes(), dClass);
                else
                    dBestAttribs = Collections.singleton(makeEmptyConstraint());
                if (dBestAttribs != null) {
                    for (Iterator d_i = dBestAttribs.iterator(); d_i.hasNext(); ) {
                        double[] d_c = (double[]) d_i.next();
                        OrderConstraintSet ocsBackup2 = null;
                        if (d_i.hasNext()) ocsBackup2 = ocs.copy();
                        boolean d_r = constrainOrder(ocs, d_c, dData, d2v);
                        if (d_r) {
                            if (out_t != null) out_t.println("   Order constraints (domain="+(int)dClass+"): "+ocs);
                            results.add(ocs);
                        }
                        ocs = ocsBackup2;
                    }
                }
                ocs = ocsBackup;
            }
        }
        return results;
    }

    static double computeScore(int vC, int aC, int dC,
        double[] vMeans, double[] aMeans, double[] dMeans,
        double vWeight, double aWeight, double dWeight) {
        double score = vMeans[vC] * vWeight;
        score += aMeans[aC] * aWeight;
        score += dMeans[dC] * dWeight;
        return score;
    }

    static double[] makeEmptyConstraint() {
        int size = 0;
        double[] d = new double[size];
        for (int i = 0; i < d.length; ++i) {
            d[i] = Double.NaN;
        }
        return d;
    }
    
    static boolean constrainOrder(OrderConstraintSet ocs, double[] c, Instances data, MultiMap map) {
        for (int iii = 0; iii < c.length; ++iii) {
            if (Double.isNaN(c[iii])) continue;
            int k = (int) c[iii];
            OrderAttribute oa = (OrderAttribute) data.attribute(iii);
            OrderConstraint oc = oa.getConstraint(k);
            if (map != null) {
                Collection c1 = map.getValues(oc.getFirst());
                Collection c2 = map.getValues(oc.getSecond());
                boolean any = false;
                for (Iterator ii = c1.iterator(); ii.hasNext();) {
                    Object a = ii.next();
                    for (Iterator jj = c2.iterator(); jj.hasNext();) {
                        Object b = jj.next();
                        if(a.equals(b)) continue;
                        OrderConstraint cc = OrderConstraint.makeConstraint(oc.getType(), a, b);
                        boolean r = ocs.constrain(cc);
                        if (r) {
                            any = true;
                        }
                    }
                }
                if (!any) {
                    if (TRACE > 3) out.println("Constraint "+oc+" conflicts with "+ocs);
                    return false;
                }
            } else {
                boolean r = ocs.constrain(oc);
                if (!r) {
                    if (TRACE > 3) out.println("Constraint "+oc+" conflicts with "+ocs);
                    return false;
                }
            }
        }
        return true;
    }

    void printGoodOrder(Collection allVars, Instances inst, MyId3 v) {
        Collection vBestAttribs = v.getAttribCombos(inst.numAttributes(), 0.);
        if (vBestAttribs != null) {
            outer:
                for (Iterator ii = vBestAttribs.iterator(); ii.hasNext(); ) {
                double[] c = (double[]) ii.next();
                OrderConstraintSet ocs = new OrderConstraintSet();
                for (int iii = 0; iii < c.length; ++iii) {
                    if (Double.isNaN(c[iii])) continue;
                    int k = (int) c[iii];
                    OrderAttribute oa = (OrderAttribute) inst.attribute(iii);
                    out.println(oa);
                    OrderConstraint oc = oa.getConstraint(k);
                    out.println(oc);
                    boolean r = ocs.constrain(oc);
                    if (!r) {
                        if (TRACE > 1) out.println("Constraint "+oc+" conflicts with "+ocs);
                        continue outer;
                    }
                }
                Order o = ocs.generateRandomOrder(allVars);
                out.println("Good order: " + o);
            }
        }
    }

    TrialGuess evalOrder(Order o, InferenceRule ir) {
        List allVars = o.getFlattened();
        return tryNewGoodOrder(null, allVars, ir, -2,  o, false);
    }
    
    public static class OrderSearchElem implements Comparable{
        public double pathScore;
        public double pathCost;

        public OrderConstraintSet ocs;
        public int nextRule;
        public Collection rulesLeft;
        public OrderSearchElem(OrderSearchElem that){
            this.pathScore = that.pathScore;
            this.ocs = new OrderConstraintSet(that.ocs);
            this.nextRule = that.nextRule;
            this.pathCost = that.pathCost;
        }
        public OrderSearchElem(double score, double cost, OrderConstraintSet ocs, int nextRule){
            this.pathScore = score;
            this.pathCost = cost;
            this.ocs = ocs;
            this.nextRule= nextRule;
        }
        
        public String toString(){
            return "[" + pathScore + ", " + ocs + "]";
        }
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object arg0) {
            OrderSearchElem that = (OrderSearchElem) arg0;
            return Double.compare(this.pathScore, that.pathScore);
        }
    }
        
    void cache(int ruleNum, BDDInferenceRule rule, OrderConstraintSet[][] cachedConstraints, double[][] cachedScores){
            List vars = new LinkedList(rule.getNecessaryVariables());
            Object[] arr = vars.toArray();
            Arrays.sort(arr, rule.new VarOrderComparator(solver.VARORDER));
            vars = Arrays.asList(arr);

            if(true){
                if(TRACE > 1) out.println("Finding Constraints for: " + rule);
            OrderTranslator t = new MapBasedTranslator(rule.variableToBDDDomain);
            EpisodeCollection tc = new EpisodeCollection(rule, 0);
            
            boolean initialized = false;
            /* put these in backwards, so we can a stack */
            for (int i = 0; i < NUM_BEST_ORDERS_PER_RULE; ++i) {
                if(!initialized){
                    cachedConstraints[ruleNum] = new OrderConstraintSet[NUM_BEST_ORDERS_PER_RULE];
                    cachedScores[ruleNum] = new double[NUM_BEST_ORDERS_PER_RULE];
                    initialized = true;
                }
                
                TrialGuess tg = tryNewGoodOrder(tc, vars, rule, -2, null, true);
                if (tg == null) break;
                OrderConstraintSet newOcs = new OrderConstraintSet();
                newOcs.constrain(t.translate(tg.order), null);
                cachedConstraints[ruleNum][i] = newOcs;
                cachedScores[ruleNum][i] = tg.prediction.score; // * (rule.totalTime+1) / 1000;
                tc.addTrial(tg.order, null, 0, System.currentTimeMillis());
            }
        }else{
                MultiMap a2v, d2v;
                a2v = new GenericMultiMap();
                d2v = new GenericMultiMap();
                for (Iterator i = vars.iterator(); i.hasNext();) {
                    Variable v = (Variable) i.next();
                    Attribute a = (Attribute) rule.getAttribute(v);
                    if (a != null) {
                        a2v.add(a, v);
                        d2v.add(a.getDomain(), v);
                    }
                }
                TrialDataGroup vDataGroup = dataRepository.getVariableDataGroup(rule,vars);
                vDataGroup.discretize(.5);
                vDataGroup.classify();
                TrialDataGroup aDataGroup = dataRepository.getAttribDataGroup(rule,vars);
                aDataGroup.discretize(.25);
                vDataGroup.classify();
                TrialDataGroup dDataGroup = dataRepository.getDomainDataGroup(rule,vars);
                dDataGroup.threshold(2);
                dDataGroup.classify();
                Collection pairs = genConstaints(NUM_BEST_ORDERS_PER_RULE,a2v,d2v,rule,vDataGroup,aDataGroup,dDataGroup);
                cachedConstraints[ruleNum] = new OrderConstraintSet[NUM_BEST_ORDERS_PER_RULE];
                cachedScores[ruleNum] = new double[NUM_BEST_ORDERS_PER_RULE];
                int i = 0;
                for(Iterator it = pairs.iterator(); it.hasNext() && i < NUM_BEST_ORDERS_PER_RULE; ++i){
                    Pair pair = (Pair) it.next();
                    double score = ((Double) pair.get(0)).doubleValue();
                    OrderConstraintSet constraints = (OrderConstraintSet) pair.get(1);
                    cachedConstraints[ruleNum][i] = constraints.translate(rule.variableToBDDDomain);
                    cachedScores[ruleNum][i] = score;
                }
        }
    }
    
    static String MAX_CON_ORDERS = System.getProperty("considertrials");
    static int MAX_GEN_ORDERS = 100;
    static final int NUM_BEST_ORDERS_PER_RULE = 3;
    void myPrintBestBDDOrders(StringBuffer sb, Collection domains,List rules) {
       if(rules.size() == 0) return;
       Collection visitedElems = new LinkedList();
       double [][] cachedScores = new double[rules.size()][] ;
       OrderConstraintSet [][] cachedConstraints = new OrderConstraintSet[rules.size()][];
       Queue queue = new StackQueue(); // PriorityQueue(); 
       int numPrintedOrders = 0;
       int nodes = 0, maxQueueSize = 0;
       long allRulesTime = 0;
       TrialDataRepository repository = MAX_CON_ORDERS == null ? 
         dataRepository : dataRepository.reduceByNumTrials(Integer.parseInt(MAX_CON_ORDERS));
       
       for(Iterator it = rules.iterator(); it.hasNext(); )
           allRulesTime += ((BDDInferenceRule) it.next()).totalTime;
       
       OrderSearchElem first = new OrderSearchElem(0,0, new OrderConstraintSet(),0);
       queue.offer(first);
       
       while(!queue.isEmpty() && numPrintedOrders < MAX_GEN_ORDERS){
           maxQueueSize = Math.max(maxQueueSize, queue.size());
           OrderSearchElem elem = (OrderSearchElem) queue.poll();
           Assert._assert(elem != null);
           if(elem.nextRule >= rules.size() || (elem.rulesLeft != null && elem.rulesLeft.isEmpty()) || elem.ocs.onlyOneOrder(domains.size())){
            if(TRACE > 1){
                out.println("No more rules or constraints on this path");
                out.println("Generating orders for: " + elem.ocs);
            } 
             Collection orders;
             if (elem.ocs.approxNumOrders(domains.size()) > MAX_GEN_ORDERS) {
                 if(TRACE > 1) out.println("More than " + MAX_GEN_ORDERS + " orders. Dumping...random sample");
                 orders = new HashSet();
                 for (int i = 0; i < 5; ++i) {
                     orders.add(elem.ocs.generateRandomOrder(domains));
                 }
             } else {
                 orders = elem.ocs.generateAllOrders(domains);
             }
             for (Iterator j = orders.iterator(); j.hasNext(); ) {
                 Order o = (Order) j.next();
                 sb.append("Score "+format(elem.pathScore, 5)+": "+o.toVarOrderString(null));
                 sb.append('\n');
             }
             sb.append("-\n");
             numPrintedOrders += orders.size();
             continue;
           }
           ++nodes;
          // LinkedList elems = new LinkedList();
           if(TRACE > 3) out.println("Expanding: " + elem);
           
           BDDInferenceRule rule = (BDDInferenceRule) rules.get(elem.nextRule);
           boolean cached  = cachedConstraints[elem.nextRule] != null;
           if(!cached) cache(elem.nextRule, rule, cachedConstraints, cachedScores);
           for (int i = 0; i < NUM_BEST_ORDERS_PER_RULE; ++i) {
               OrderConstraintSet constraints = cachedConstraints[elem.nextRule][i];
               OrderSearchElem newElem = new OrderSearchElem(elem);
               if(constraints == null){
                   if(i == 0) {
                       ++newElem.nextRule;
                       queue.offer(newElem);
                       //elems.add(elem);
                   }
                   break;
               }
               double constraintScore = cachedScores[elem.nextRule][i];
               ++newElem.nextRule;
               Collection invalidConstraints = new LinkedList();
               if(TRACE > 3) out.println("Adding constraints: " + constraints);
               boolean worked = newElem.ocs.constrain(constraints, invalidConstraints);
               //newElem.pathCost += (rule.totalTime * constraintScore) * (1 + invalidConstraints.size() / constraints.size()) ;
               newElem.pathCost +=  invalidConstraints.size() * (rule.totalTime / constraintScore) ;
               newElem.pathScore = newElem.pathCost;
               
               //if(!worked) continue;//newElem.ocs = backupOcs;
               if(TRACE > 3)  out.println("Couldn't add: " + invalidConstraints);
               queue.offer(newElem);
               //elems.add(newElem);
           
           }
           /*if we're using the stack, push them in reverse priority */
      /*     if(elems.size() > 0)
           for(ListIterator it = elems.listIterator(elems.size() - 1);  it.hasPrevious();  )
               queue.offer(it.previous());   
        */     
       }
       out.println("Max queue size:  " + maxQueueSize + " Nodes expanded: " + nodes);
    }

   void printBestBDDOrders(StringBuffer sb, double score, Collection domains, OrderConstraintSet ocs,
            MultiMap rulesToTrials, List rules) {
        if (rules == null || rules.isEmpty()) {
            if(TRACE > 1) out.println("No more rules, Generating orders");
            Collection orders;
            if (ocs.approxNumOrders(domains.size()) > 1000) {
                if(TRACE > 1) out.println("More than " + MAX_GEN_ORDERS + " orders. Dumping...random sample");
                orders = new LinkedList();
                for (int i = 0; i < 5; ++i) {
                    orders.add(ocs.generateRandomOrder(domains));
                }
            } else {
                if(TRACE > 1) out.println("Generating orders from constraints: " + ocs);
                orders = ocs.generateAllOrders(domains);
            }
            for (Iterator j = orders.iterator(); j.hasNext(); ) {
                Order o = (Order) j.next();
                sb.append("Score "+format(score)+": "+o.toVarOrderString(null));
                sb.append('\n');
            }
            return;
        }
        if (!ocs.onlyOneOrder(domains.size())) {
            InferenceRule ir = (InferenceRule) rules.get(0);
            List rest = rules.subList(1, rules.size());
            if (ir instanceof BDDInferenceRule && rulesToTrials.containsKey(ir)) {
             
                BDDInferenceRule bddir = (BDDInferenceRule) ir;
                if(TRACE > 1) {
                    out.println("Generating constraints for rule:\n" + ir.toString());
                    out.println("Total rule run time: " + bddir.totalTime);
                }
                OrderTranslator t = new MapBasedTranslator(bddir.variableToBDDDomain);
                EpisodeCollection tc = new EpisodeCollection(bddir, 0);
                for (int i = 0; i < 5; ++i) {
                    TrialGuess tg = tryNewGoodOrder(tc, new ArrayList(bddir.necessaryVariables), bddir, -2, null, true);
                    if (tg == null) break;
                    OrderConstraintSet ocs2 = new OrderConstraintSet(ocs);
                    Order bddOrder = t.translate(tg.order);
                    out.println("Adding constraints for: " + bddOrder);
                    Collection invalidConstraints = new LinkedList();
                    boolean worked = ocs2.constrain(bddOrder, invalidConstraints);
                    double score2 = tg.prediction.score * (bddir.totalTime+1) / 1000;
    
                    /*tc.addTrial(tg.order, null, 0);
                    if (!worked) 
                        out.println("Couldn't add constraints: " + invalidConstraints);
                    */
                  /*  printBestBDDOrders(sb, score + score2, domains, ocs2, rulesToTrials, rest); */
                    
                   if (worked) {
                        
                        printBestBDDOrders(sb, score + score2, domains, ocs2, rulesToTrials, rest);
                    }else{
                        out.println("Couldn't add constraints: " + invalidConstraints);
                    }
                    tc.addTrial(tg.order, null, 0, System.currentTimeMillis());
                
                }
            } else {
                printBestBDDOrders(sb, score, domains, ocs, rulesToTrials, rest);
            }
        }
        
        //Only one order
    
        out.println("Can't add more constraints: " + ocs);
        out.println("Left over rules (" + rules.size() + ": " + rules);
        Order o = ocs.generateRandomOrder(domains);
        for (Iterator k = rules.iterator(); k.hasNext(); ) {
           InferenceRule ir = (InferenceRule) k.next();
           if(!(ir instanceof BDDInferenceRule)) continue;
            BDDInferenceRule bddir = (BDDInferenceRule) ir;
            Order o2;
            if (false) {
                MultiMap d2v = new GenericMultiMap();
                for (Iterator a = bddir.variableToBDDDomain.entrySet().iterator(); a.hasNext(); ) {
                    Map.Entry e = (Map.Entry) a.next();
                    d2v.add(e.getValue(), e.getKey());
                }
                o2 = new MapBasedTranslator(d2v).translate(o);
            } else {
                Map d2v = new HashMap();
                for (Iterator a = bddir.variableToBDDDomain.entrySet().iterator(); a.hasNext(); ) {
                    Map.Entry e = (Map.Entry) a.next();
                    d2v.put(e.getValue(), e.getKey());
                }
                o2 = new MapBasedTranslator(d2v).translate(o);
            }
            TrialGuess tg = tryNewGoodOrder(null, new ArrayList(bddir.necessaryVariables), bddir, -2, o2, true);
            score += tg.prediction.score * (bddir.totalTime+1) / 1000;
        }
        sb.append("Score "+format(score)+": "+o.toVarOrderString(null));
        sb.append('\n');
    }
    
    public Set getVisitedRules(){
        Set visitedRules = new HashSet();
        for(Iterator it = allTrials.iterator(); it.hasNext(); ){
            EpisodeCollection tc = (EpisodeCollection) it.next();
            visitedRules.add(tc.getRule(solver));
        }
    
        if(TRACE > 2) out.println("Visited Rules: " + visitedRules);
        return visitedRules;  
    }
    
    public void printBestBDDOrders() {
        MultiMap ruleToTrials = new GenericMultiMap();
        for (Iterator i = allTrials.iterator(); i.hasNext(); ) {
            EpisodeCollection tc = (EpisodeCollection) i.next();
            ruleToTrials.add(tc.getRule(solver), tc);
        }
        
        // Sort rules by their run time.
        SortedSet sortedRules = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) return 0;
                if (o1 instanceof NumberingRule) return -1;
                if (o2 instanceof NumberingRule) return 1;
                BDDInferenceRule r1 = (BDDInferenceRule) o1;
                BDDInferenceRule r2 = (BDDInferenceRule) o2;
                long diff = r2.totalTime - r1.totalTime; //descending
                //long diff = r1.totalTime - r2.totalTime;  //ascending 
                if (diff != 0)
                    return (int) diff;
                return r1.id - r2.id;
            }
        });
        sortedRules.addAll(filterRules(solver.rules));
        ArrayList list = new ArrayList(sortedRules);
        Collection domains = new FlattenedCollection(solver.getBDDDomains().values());
        out.println("BDD Domains: "+domains);
        OrderConstraintSet ocs = new OrderConstraintSet();
        StringBuffer sb = new StringBuffer();
     //   printBestBDDOrders(sb, 0, domains, ocs, ruleToTrials, list);
        myPrintBestBDDOrders(sb, domains, list);
        out.println(sb);
    }
    
    static Collection filterRules(Collection rules){
        Collection filteredRules = new LinkedList();
        for(Iterator it = rules.iterator(); it.hasNext(); ){
            InferenceRule rule = (InferenceRule) it.next();
            if(rule instanceof BDDInferenceRule) filteredRules.add(rule);
        }
        return filteredRules;
    }
    public void printBestTrials() {
        MultiMap ruleToTrials = new GenericMultiMap();
        for (Iterator i = allTrials.iterator(); i.hasNext(); ) {
            EpisodeCollection tc = (EpisodeCollection) i.next();
            ruleToTrials.add(tc.getRule(solver), tc);
        }
        // Sort rules by their run time.
        SortedSet sortedRules = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) return 0;
                BDDInferenceRule r1 = (BDDInferenceRule) o1;
                BDDInferenceRule r2 = (BDDInferenceRule) o2;
                
                long diff = r2.totalTime - r1.totalTime;
               
                if (diff != 0)
                    return (int) diff;
                return r1.id - r2.id;
            }
        });
        sortedRules.addAll(ruleToTrials.keySet());
        
        for (Iterator i = sortedRules.iterator(); i.hasNext(); ) {
            BDDInferenceRule ir = (BDDInferenceRule) i.next();
            Map scoreboard = new HashMap();
            for (Iterator j = ruleToTrials.getValues(ir).iterator(); j.hasNext(); ) {
                EpisodeCollection tc = (EpisodeCollection) j.next();
                TrialInfo ti = tc.getMinimum();
                if (ti == null || ti.isMax()) continue;
                long[] score = (long[]) scoreboard.get(ti.order);
                if (score == null) scoreboard.put(ti.order, score = new long[2]);
                score[0]++;
                score[1] += ti.cost;
            }
            
            if (scoreboard.isEmpty()) continue;
            
            SortedSet sortedTrials = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    long[] counts1 = (long[]) ((Map.Entry) o1).getValue();
                    long[] counts2 = (long[]) ((Map.Entry) o2).getValue();
                    long diff = counts2[0] - counts1[0];
                    if (diff != 0)
                        return (int) diff;
                    diff = counts2[1] - counts1[1];
                    if (diff != 0)
                        return (int) diff;
                    Order order1 = (Order) ((Map.Entry) o1).getKey();
                    Order order2 = (Order) ((Map.Entry) o2).getKey();
                    return order1.compareTo(order2);
                }
            });
            sortedTrials.addAll(scoreboard.entrySet());
            
            out.println("For rule"+ir.id+": "+ir);
            for (Iterator it = sortedTrials.iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                Order order = (Order) entry.getKey();
                long[] counts = (long[]) entry.getValue();
                double aveTime = (double)counts[1] / (double) counts[0];
                String bddString = order.toVarOrderString(ir.variableToBDDDomain);
                out.println(order + " won " + counts[0] + " time(s), average winning time of "+format(aveTime)+" ms");
                out.println("   BDD order: "+bddString);
            }
            out.println();
        }
        
    }
    
    public void printTrialsDistro() {
        printTrialsDistro(allTrials, solver);
    }

    public static void printTrialsDistro(Collection trials, Solver solver) {
        Map orderToCounts = new HashMap();
        final int numRules = solver.getRules().size();
        int total = 0, distinct = 0;
        for (Iterator it = trials.iterator(); it.hasNext();) {
            EpisodeCollection tc = (EpisodeCollection) it.next();
            Assert._assert(tc != null);
            for (Iterator jt = tc.getTrials().iterator(); jt.hasNext();) {
                TrialInfo ti = (TrialInfo) jt.next();
                Order order = ti.order;
                int[] counts = (int[]) orderToCounts.get(order);
                if (counts == null) {
                    counts = new int[numRules + 1];
                    orderToCounts.put(order, counts);
                    ++distinct;
                }
                ++counts[tc.getRule(solver).id];
                //one extra int at the end to count the total number of trials
                ++counts[numRules];
            }
            total += tc.getNumTrials();
        }

        SortedSet sortedTrials = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                int[] counts1 = (int[]) ((Map.Entry) o1).getValue();
                int[] counts2 = (int[]) ((Map.Entry) o2).getValue();
                int diff = counts2[numRules] - counts1[numRules];
                if (diff != 0)
                    return diff;
                Order order1 = (Order) ((Map.Entry) o1).getKey();
                Order order2 = (Order) ((Map.Entry) o2).getKey();
                return order1.compareTo(order2);
            }
        });

        sortedTrials.addAll(orderToCounts.entrySet());
        out.println(total + " trials  of " + distinct + " distinct orders");
        out.println("tried Orders: ");
        for (Iterator it = sortedTrials.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            Order order = (Order) entry.getKey();
            int[] counts = (int[]) entry.getValue();
            out.println(order + " tried a total of " + counts[numRules] + " time(s) :");
            for (int i = 0; i < counts.length - 1; ++i) {
                int count = counts[i];
                if (count != 0) {
                    out.println("    " + count + " time(s) on \n    " + solver.getRule(i));
                }
            }
            out.println();
        }
    }

    public static void main(String[] args) throws Exception {
        String inputFilename = SystemProperties.getProperty("datalog");
        if (args.length > 0) inputFilename = args[0];
        if (inputFilename == null) {
            return;
        }
        String solverName = SystemProperties.getProperty("solver", "net.sf.bddbddb.BDDSolver");
        Solver s;
        s = (Solver) Class.forName(solverName).newInstance();
        s.load(inputFilename);

        FindBestDomainOrder dis = ((BDDSolver) s).fbo;
        //dis.loadTrials("trials.xml");
        //dis.dump();
/*
        for (Iterator i = s.rules.iterator(); i.hasNext();) {
            InferenceRule ir = (InferenceRule) i.next();
            if (ir.necessaryVariables == null) continue;
            out.println("Computing for rule " + ir);

            List allVars = new LinkedList();
            allVars.addAll(ir.necessaryVariables);
            out.println("Variables = " + allVars);

            TrialGuess guess = dis.tryNewGoodOrder(null, allVars, ir, false);

            out.println("Resulting guess: "+guess);
        }
*/
        //printTrialsDistro(dis.allTrials, s);
        //dis.printBestTrials();
        dis.printBestBDDOrders();
    }

    
    /**
     * Run the find best domain order on the given inputs.
     * 
     * @param bdd  BDD factory
     * @param b1   first input to relprod
     * @param b2   second input to relprod
     * @param b3   third input to relprod
     * @param r1   first rule term
     * @param r2   second rule term
     * @param vars1  variables of b1
     * @param vars2  variables of b2
     */
    static void findBestDomainOrder(BDDSolver solver, BDDInferenceRule rule, int opNum, BDDFactory bdd, BDD b1, BDD b2, BDD b3, RuleTerm r1, RuleTerm r2, Collection vars1, Collection vars2) {
        Set allVarSet = new HashSet(vars1); allVarSet.addAll(vars2);
        allVarSet.removeAll(rule.unnecessaryVariables);
        Object[] a = allVarSet.toArray();
        // Sort the variables by domain so that we will first try orders that are close
        // to the default one.
        Arrays.sort(a, rule.new VarOrderComparator(solver.VARORDER));
        List allVars = Arrays.asList(a);
        
        FindBestDomainOrder fbdo = solver.fbo;
        if (!fbdo.hasOrdersToTry(allVars, rule)) {
            out.println("No more orders to try, skipping find best order for "+vars1+","+vars2);
            return;
        }
        out.println("Finding best order for "+vars1+","+vars2);
        long time = System.currentTimeMillis();
        Episode ep = fbdo.getNewEpisode(rule, opNum, time, true);
        FindBestOrder fbo = new FindBestOrder(solver.BDDNODES, solver.BDDCACHE, solver.BDDNODES / 2, Long.MAX_VALUE, 5000);
        try {
            fbo.init(b1, b2, b3, BDDFactory.and);
        } catch (IOException x) {
            solver.err.println("IO Exception occurred: " + x);
            fbo.cleanup();
            return;
        }
        out.println("Time to initialize FindBestOrder: "+(System.currentTimeMillis()-time));
        int count = BDDInferenceRule.MAX_FBO_TRIALS;
        boolean first = true;
        long bestTime = Long.MAX_VALUE;
        while (--count >= 0) {
            //Order o = fbdo.tryNewGoodOrder(tc, allVars, t);
            TrialGuess guess = fbdo.tryNewGoodOrder(ep, allVars, rule,opNum, first);
            if (guess == null || guess.order == null) break;
            Order o = guess.order;
            String vOrder = o.toVarOrderString(rule.variableToBDDDomain);
            out.println("Trying order "+vOrder);
            vOrder = solver.fixVarOrder(vOrder, false);
            out.println("Complete order "+vOrder);
            time = fbo.tryOrder(true, vOrder);
            time = Math.min(time, BDDInferenceRule.LONG_TIME);
            bestTime = Math.min(time, bestTime);
            fbdo.addTrial(rule, allVars,ep, o,guess.prediction, time, System.currentTimeMillis());
            
            if (time >= BDDInferenceRule.LONG_TIME)
                fbdo.neverTryAgain(rule, o);
            first = false;
        }
        fbo.cleanup();
        
        fbdo.incorporateTrial(ep);
        
        XMLFactory.dumpXML("fbo.xml", fbdo.toXMLElement());
        XMLFactory.dumpXML(solver.TRIALFILE, fbdo.trialsToXMLElement());
    }


}
