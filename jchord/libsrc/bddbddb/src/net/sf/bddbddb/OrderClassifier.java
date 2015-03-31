// OrderClassifier.java, created Jul 27, 2004 8:57:36 PM 2004 by mcarbin
// Copyright (C) 2004 Michael Carbin <mcarbin@stanford.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.util.Assert;
import net.sf.javabdd.BDDDomain;
import weka.classifiers.Classifier;
import weka.classifiers.trees.Id3;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.PKIDiscretize;

/**
 * OrderClassifier
 * 
 * @author mcarbin
 * @version $Id: OrderClassifier.java 531 2005-04-29 06:39:10Z joewhaley $
 */
public class OrderClassifier {
    private Instances data = null;
    private Classifier classifier;
    private BDDInferenceRule rule;
    private BDDRelation bottomRelation;
    private List relationAttrPairs;
    private MultiMap domainToAttrPairs;
    private int numAttrs;
    private FastVector attributes;
    private FastVector attrOptions;
    private Filter filter;
    private SortedSet orders;
    private List domainSet;
    private int numClusters;
    public static Set goodClusters;
    private long max_wait_time;
    static{
        goodClusters = new HashSet();
        goodClusters.add("0");
    }
    public OrderClassifier(BDDInferenceRule rule,List domainSet, long max_wait_time){
        this.rule = rule;
        this.bottomRelation = (BDDRelation) rule.bottom.getRelation();
        this.domainSet = domainSet;
        this.max_wait_time = max_wait_time;
        orders = new TreeSet();
        classifier = new Id3();
        relationAttrPairs = new LinkedList();
        domainToAttrPairs = new GenericMultiMap();
        attributes = computeAttributes();
        attributes.addElement( new Attribute("class"));
        /*      FastVector values = new FastVector(2);
         values.addElement("good");
         values.addElement("bad");
         
         attributes.addElement(new Attribute("Time", values));
         */
        
        // data = new Instances("Ordering Constraints", attributes, 30);
        
    }
    
    
    public double importance(weka.core.Attribute attribute, String attrValue){//, String classValue){
        
        int count = 0;
        int goodCount = 0, badCount = 0;
        List newInstances = new LinkedList();
        
        for(Iterator it = orders.iterator(); it.hasNext(); ){
            Instance instance = (Instance) it.next();
            if(//!instance.stringValue(instance.classIndex()).equals(classValue) ||
                !instance.stringValue(attribute).equals(attrValue)) continue;
            
            if(goodClusters.contains(instance.stringValue(instance.classIndex())))
                ++goodCount;
            else
                ++badCount;
            
            Instance newInstance = new Instance(instance);
            newInstance.setDataset(instance.dataset());
            newInstances.add(newInstance);
        }
        goodCount *= attrOptions.size() - 1;
        badCount *= attrOptions.size() - 1;
        for(Iterator it = newInstances.iterator(); it.hasNext(); ){
            Instance instance = (Instance) it.next();
            /*      if(//!instance.stringValue(instance.classIndex()).equals(classValue) || 
             !instance.stringValue(attribute).equals(attrValue)) continue;
             */
            
            String classValue = instance.stringValue(instance.classIndex());
            FastVector newOptions = new FastVector();
            newOptions.appendElements(attrOptions);
            newOptions.removeElementAt(newOptions.indexOf(instance.stringValue(attribute)));
            //int index = Math.abs(LearnedOrder.randomNumGen.nextInt()) % newOptions.size();
            int index = 0;
            while(index < newOptions.size()){
                instance.setValue(attribute, attrOptions.indexOf(newOptions.elementAt(index)));
                String value = classify(instance);
                if(goodClusters.contains(classValue)){
                    if(goodClusters.contains(value)) --goodCount;
                }else if(!goodClusters.contains(classValue)){  
                    if(!goodClusters.contains(value)) --badCount;
                }
                ++index;
            }
            //if(value.equals(classValue)) --count;
        }
        
        count = goodCount - badCount;
        count /= attrOptions.size() - 1;
        
        double importance = ((double) count) / newInstances.size();
        if(Double.isNaN(importance)) return 0;
        return importance;
    }
    
    public double vote(weka.core.Attribute attribute, String attrValue, String classValue){
        int count = 0;
        int numOrders = 0;
        for(Iterator it = orders.iterator(); it.hasNext(); ){
            Instance instance = (Instance) it.next();
            if(!instance.stringValue(instance.classIndex()).equals(classValue)) continue;
            ++numOrders;
            
            if(instance.stringValue(attribute).equals(attrValue)) ++count;
            
        }
        
        return ((double) count) / numOrders;
    }
    
    public String vote(weka.core.Attribute attribute, String classValue){
        
        System.out.print(attribute + " votes: ");
        double before = vote(attribute, "<", classValue);
        System.out.print(" < , " + before);
        double inter = vote(attribute,"=", classValue);
        System.out.print("  = , " + inter);
        double after = vote(attribute,  ">", classValue);
        System.out.println("   >, " + after);
        
        return "";
        
    }
    public FastVector computeAttributes(){   
        for(Iterator it = bottomRelation.getAttributes().iterator(); it.hasNext();){
            net.sf.bddbddb.Attribute a = ( net.sf.bddbddb.Attribute) it.next();
            Pair p = new Pair(bottomRelation,a);
            relationAttrPairs.add(p);
            domainToAttrPairs.add(bottomRelation.getBDDDomain(a), p);
        }
        
        for(Iterator it = rule.top.iterator(); it.hasNext(); ){
            BDDRelation r = (BDDRelation) ((RuleTerm) it.next()).getRelation();
            for(Iterator jt = r.getAttributes().iterator(); jt.hasNext(); ){
                net.sf.bddbddb.Attribute a = (net.sf.bddbddb.Attribute) jt.next();
                Pair p = new Pair(r, a);
                relationAttrPairs.add(p);
                domainToAttrPairs.add(r.getBDDDomain(a), p);
            }
        }
        
        FastVector constraints = new FastVector();
        attrOptions = new FastVector(3);
        attrOptions.addElement("<");
        attrOptions.addElement("=");
        attrOptions.addElement(">");
        for(int i = 0; i < relationAttrPairs.size() - 1; i++){
            Pair p1 = (Pair) relationAttrPairs.get(i);
            for(int j = i + 1; j < relationAttrPairs.size(); j++){
                Pair p2 = (Pair) relationAttrPairs.get(j);
                BDDDomain  d1 = ((BDDRelation) p1.left).getBDDDomain((net.sf.bddbddb.Attribute) p1.right); 
                BDDDomain d2 = ((BDDRelation) p2.left).getBDDDomain((net.sf.bddbddb.Attribute) p2.right);
                if(d1.equals(d2)) continue;
                Pair p = new Pair(p1, p2);
                constraints.addElement(new MyAttribute(p,attrOptions));
                //constraints.addElement(new MyAttribute(p,attrOptions, constraints.size()));
            }
        }      
        return constraints;
    }
    
    public void load(BufferedReader in){
        try{
            String strClusts = in.readLine();
            setNumClasses(Integer.parseInt(strClusts));
            
            for(;;){
                String line = in.readLine();
                if(line == null) break;
                Instance instance = new Instance(attributes.size());              
                StringTokenizer st = new StringTokenizer(line,",");
                int i = 0;
                while(st.hasMoreTokens()){
                    String str = st.nextToken();
                 //   System.out.println("attr " + attributes.elementAt(i) + " val: " + str);
                    instance.setValue((weka.core.Attribute) attributes.elementAt(i),str);
                    ++i;
                }
                line = in.readLine();
                instance = new MyInstance(instance,Long.parseLong(line));
                orders.add(instance);
            }
            buildClassifier();
        }catch(IOException e){
            e.printStackTrace();
            Assert.UNREACHABLE("Could not load instances");
        }catch(Exception e){
            e.printStackTrace();
            Assert.UNREACHABLE("Could not build classifier");
        }
    }
    
    public String toString(){
        return classifier.toString();
    }
    public void save(BufferedWriter out){
        try{
            out.write(numClusters + "\n");
            for(Iterator it = orders.iterator(); it.hasNext(); ){
                MyInstance instance = (MyInstance) it.next();
                out.write(instance.toString() + "\n");
                out.write(instance.time + "\n");
            }
        }catch(IOException e){
            e.printStackTrace();
            Assert.UNREACHABLE("Couldn't save instances");
        }
    }
    
    public void setNumClasses(int numClusters){
        this.numClusters = numClusters;
        attributes.removeElementAt(attributes.size() - 1);
        FastVector clusterValues = new FastVector(numClusters);
        for(int i = 0; i < numClusters; ++i)
            clusterValues.addElement(Integer.toString(i));
        
        //attributes.addElement(new Attribute("class", clusterValues, attributes.size()));
        attributes.addElement(new Attribute("class", clusterValues));
    }
    
    public Instances makeInstances(){
        Instances instances = new Instances("Ordering Constraints", attributes, 30);
        instances.setClassIndex(attributes.size() - 1);
        System.out.println(attributes.elementAt(attributes.size() -1));
        return instances;
    }
    
    public void buildClassifier() throws Exception{
        System.out.println("Discritizing...");
        Discretizer discretizer = new EqualFreqDiscretizer();//new ClusterDiscretizer();
        
        data = discretizer.discretize(orders); 
        System.out.println("Finished Discritization");
        classifier.buildClassifier(data);
        System.out.println("Finished building classifier");
    }
    public abstract class Discretizer {
        public abstract Instances discretize(SortedSet orders);
        
        public Instances timeInstances(SortedSet orders){
            FastVector attributes = new FastVector(1);
            attributes.addElement(new Attribute("time"));
            
            Instances cInstances = new Instances("wiki", attributes,orders.size());
            
            for(Iterator it = orders.iterator(); it.hasNext();){
                // System.out.println("class: " + data.instance(i).getClass());
                MyInstance instance = (MyInstance) it.next();
                double [] values = new double[1]; //instance.toDoubleArray();
                values[values.length  - 1] = instance.getTime();
                Instance newInstance = new Instance(1, values);
                newInstance = new MyInstance(newInstance, instance.getTime());
                cInstances.add(newInstance);
            }
            
            return cInstances;
        }
        
        
    }
    
    public class EqualFreqDiscretizer extends Discretizer{
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.OrderClassifier.Discretizer#discretize(java.util.SortedSet)
         */
        public Instances discretize(SortedSet orders) {
            Instances cInstances = timeInstances(orders);
            PKIDiscretize d = new PKIDiscretize();
            int[] arr = new int[1];
            arr[0] = 0;
            d.setAttributeIndicesArray(arr);
            try {
                d.setInputFormat(cInstances);
                cInstances = Filter.useFilter(cInstances, d);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            System.out.println("num bins: " + d.getBins());
            setNumClasses((int) Math.sqrt(cInstances.numInstances()));
            
            Instances newInstances = makeInstances();
            Enumeration e = cInstances.enumerateInstances();
            for(Iterator it = orders.iterator(); it.hasNext(); ){
                MyInstance instance = (MyInstance) it.next();
                Instance cInstance = (Instance) e.nextElement();
                
                instance.setDataset(newInstances);
                instance.setClassValue(cInstance.value(0));
              //  System.out.println("instance: " + instance + " time: " + instance.getTime());
                newInstances.add(instance);
                
            }
            System.out.println("num instances: " + newInstances.numInstances());
            return newInstances;
        }
        
        
    }
    /*
     public class MDLDiscretizer extends Discretizer{
     public Instances discretize(SortedSet orders){
     
     }
     }*/
    
    public class ClusterDiscretizer extends Discretizer{
        public Instances discretize(SortedSet orders){
            Instances newInstances = null;
            //  return discretize((MyInstance[]) orders.toArray(new MyInstance[orders.size()]), 0, orders.size() -1);
            // double[] times = new double[instances.numInstances()];
            Clusterer clusterer = new EM(); //new Cobweb();
            try{
                
                /*FastVector attributes = new FastVector(classifier.getAttributes().size());
                 attributes.appendElements(classifier.getAttributes());
                 attributes.removeElementAt(classifier.getAttributes().size() - 1);
                 */
                Instances cInstances = timeInstances(orders);
                //data = new Instances(data);
                /*      int index = data.classIndex();
                 data.setClassIndex(-1);
                 data.deleteAttributeAt(index);
                 data.
                 */
                ((EM) clusterer).setMinStdDev(.1);
                clusterer.buildClusterer(cInstances);
                setNumClasses(clusterer.numberOfClusters());
                double[][][] clusterInfo = ((EM) clusterer).getClusterModelsNumericAtts();
                int[] clusterMap = new int[clusterInfo.length];
                Pair[] clusts = new Pair[clusterInfo.length];
                for(int i = 0; i < clusterInfo.length;++i){
                    clusts[i] = new Pair(new Integer(i), new Double(clusterInfo[i][0][0]));
                }
                
                Arrays.sort(clusts, new Comparator(){
                    
                    public int compare(Object o1, Object o2) {
                        Pair p1 = (Pair) o1;
                        Pair p2 = (Pair) o2;
                        
                        return Double.compare(((Double) p1.right).doubleValue(), ((Double) p2.right).doubleValue());
                    }
                });
                
                for(int i = 0; i < clusts.length; ++i){
                    Pair p = clusts[i];
                    clusterMap[((Integer) p.left).intValue()] = i;
                }
                
               // System.out.println(clusterer.toString());
                System.out.println("number of clusters: " + clusterer.numberOfClusters());
                Pair[] ins = new Pair[orders.size()];
                
                
                int j = 0;
                // for(Iterator it = classifier.getOrders().iterator(); it.hasNext();){
                
                newInstances = makeInstances();
                MyInstance[] orderInstances = (MyInstance[]) orders.toArray(new MyInstance[orders.size()]);
                Assert._assert(cInstances.numInstances() == orderInstances.length);
                Assert._assert(newInstances.numInstances() == 0);
                for(int i = 0; i < cInstances.numInstances();++i){
                    Instance cInstance = cInstances.instance(i);
                    MyInstance instance = orderInstances[i];
                    Assert._assert((long) cInstance.value(0) == instance.getTime());
                    int cnum = -1;
                    try{
                        cnum = clusterer.clusterInstance(cInstance);
                        Assert._assert(cnum >= 0 && cnum < clusterer.numberOfClusters());
                        cnum = clusterMap[cnum];
                        Assert._assert(cnum >= 0 && cnum < clusterer.numberOfClusters());
                        
                        
                        instance.setDataset(newInstances);
                        double val = instance.classValue();
                        instance.setClassValue(Integer.toString(cnum));
                        newInstances.add(instance);
                      //  System.out.println("cnum: " + cnum + " instance: " + instance + " old value: "  + val + " new value: " + instance.classValue());
                        Assert._assert(instance.classValue() < clusterer.numberOfClusters(), instance.toString());
                    }catch(Exception ex){
                        ex.printStackTrace();
                        Assert.UNREACHABLE("");
                    }
                    ins[j++] = new Pair(cInstance, new Integer(cnum));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
         /*   for(Enumeration e = newInstances.enumerateInstances(); e.hasMoreElements(); ){
                System.out.println(e.nextElement());
            }*/
            return newInstances;
        }
    }
    
    public class MedianDiscretizer extends Discretizer{
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.OrderClassifier.Discretizer#discretize(java.util.SortedSet)
         */
        public Instances discretize(SortedSet orders) {
            
            setNumClasses(2);
            MyInstance[] instances = (MyInstance[]) orders.toArray(new MyInstance[orders.size( )]);
            Instances newInstances = makeInstances();
            
            int right = orders.size() - 1;
            int left = 0;
            int stride = right - left;
            int median = stride / 2 + left;
            
            while(instances[median].time == max_wait_time && median > 0)
                --median;
            
            for(int i = left; i < median; i++){
                MyInstance instance = instances[i];
                newInstances.add(instance);  
                instance.setDataset(newInstances);
                
                instance.setClassValue("0");
                
            }
            
            for(int i = median; i <= right; i++){
                MyInstance instance = instances[i];
                newInstances.add(instance);
                instance.setDataset(newInstances);
                instance.setClassValue("1");
                
            }
            
            return newInstances;
        }
    }
    
    
    public long medianTime(){
        if(orders.size() == 0) return max_wait_time;
        
        Object[] instances =  orders.toArray();
        return ((MyInstance) instances[instances.length / 2]).time;
    }
    
    
    
    public String classify(Instance instance){
        try{
            double predicted = classifier.classifyInstance(instance);
            
            return data.classAttribute().value((int)predicted);
        }catch(Exception e){
            e.printStackTrace();
            Assert.UNREACHABLE("Could not classify instance");
        }
        return "";
    }
    public String classify(String varOrder){
        Instance instance  = makeInstance(varOrder);
        instance.setDataset(data);
        return classify(instance); 
    }
    public void addOrder(String varOrder, long time){
        Instance core = makeInstance(varOrder);
        MyInstance instance = new MyInstance(core,time);
        orders.add(instance);
        
    }
    
    public FastVector getAttributes(){
        return attributes;
    }
    static class MyAttribute extends weka.core.Attribute{
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3546927987002716980L;
        
        Pair pair;
        /*
        public MyAttribute(Pair pair, FastVector options, int index){
            super(pair.toString(),options, index);
            this.pair = pair;
        }
        */
        
        public MyAttribute(Pair pair, FastVector options){
            super(pair.toString(),options);
            this.pair = pair;
        }
    }
    
    class MyInstance extends Instance implements Comparable{
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3257006536231237427L;
        
        private long time;
        private String order;
        public MyInstance(int numAttributes){
            super(numAttributes);
            this.time = max_wait_time;
        }
        
        public MyInstance(MyInstance instance){
            super(instance);
            this.time = instance.getTime();
        }
        public MyInstance(Instance instance,long time){
            super(instance);
            this.time = time;
        }
        
        public Object copy(){
            Instance instance = (Instance) super.copy();
            MyInstance result = new MyInstance(instance,time);
            return result;
        }
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object o) {
            MyInstance that = (MyInstance) o;
            if(this.time == that.time) return this.hashCode() - that.hashCode();
            return (int) ((this.time - that.time) / Math.abs(this.time - that.time));
        }
        /**
         * @return  time for this instance
         */
        public long getTime() {
            return time;
        }
        
    }
    
    
    public Instance makeInstance(String varOrder){
        Instance instance = new MyInstance(attributes.size());
        for(int i = 0; i < attributes.size() - 1; i++){
            instance.setValue((weka.core.Attribute)attributes.elementAt(i),">");
        }
        StringTokenizer st = new StringTokenizer(varOrder,"_");
        List lefts = new LinkedList();
        while(st.hasMoreTokens()){
            String pdomain = st.nextToken();
            // System.out.println("pdomain: " + pdomain);
            StringTokenizer st2 = new StringTokenizer(pdomain,"x");
            if(st2.countTokens() > 1){
                List interLefts = new LinkedList();
                while(st2.hasMoreTokens()){
                    String idomain = st2.nextToken();
                    //   System.out.println("idomain: " + idomain);
                    
                    BDDDomain d = rule.solver.getBDDDomain(idomain);
                    if(!domainSet.contains(d)) continue;
                    Collection pairs = domainToAttrPairs.getValues(d);
                    //  System.out.println("interlefts: " + interLefts);
                    //  System.out.println("pairs: " + pairs);
                    
                    pair(instance,interLefts,pairs,"=",true);                 
                    interLefts.addAll(pairs);
                    
                    pair(instance,lefts,pairs,"<",false);
                    lefts.addAll(pairs);
                }
                
                continue;
            }
            BDDDomain d = rule.solver.getBDDDomain(pdomain);
            if(!domainSet.contains(d)) continue; 
            Collection pairs = domainToAttrPairs.getValues(d);
            pair(instance,lefts,pairs,"<",false);
            lefts.addAll(pairs);
            
        }
        // System.out.print("attributes:");
        /*   for(int i = 0; i < attributes.size(); i++){
         System.out.print(" " + attributes.elementAt(i));
         }
         */
        //  System.out.println();
        // System.out.println("instance: " + instance);
        return instance;
    }
    
    private void pair(Instance instance, List lefts, Collection pairs, String value, boolean reverse){
        for(Iterator it = lefts.iterator(); it.hasNext(); ){
            Pair left = (Pair) it.next();
            for(Iterator jt = pairs.iterator(); jt.hasNext(); ){
                Pair right = (Pair) jt.next();
                if(right.equals(left)) continue;
                Pair p = new Pair(left,right);
                weka.core.Attribute a = new MyAttribute(p, attrOptions);
                int index = attributes.indexOf(a);
                if(index == -1){
                    if(reverse){
                        Pair r = new Pair(right,left);
                        a = new MyAttribute(r,attrOptions);
                        index = attributes.indexOf(a);
                    }else
                        continue;
                }
                
                Assert._assert(index != -1, " no attribute for " + p + " found. attributes: " + attributes);
                //  System.out.println("Setting " + a.toString() + " to " + value);
                instance.setValue((weka.core.Attribute)attributes.elementAt(index),value);
            }
        }
    }
    
    /**
     * @return  number of attributes
     */
    public int getNumAttributes() {
        return attributes.size() - 1 ;
    }
    
    /**
     * @return  data
     */
    public Instances getData() {
        return data;
    }
    
    /**
     * @return  sorted orders
     */
    public SortedSet getOrders() {
        return orders;
    }
}

