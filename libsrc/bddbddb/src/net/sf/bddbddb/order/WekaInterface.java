// WekaInterface.java, created Oct 31, 2004 1:17:46 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import jwutil.collections.Pair;
import jwutil.util.Assert;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.Variable;
import net.sf.bddbddb.order.OrderConstraint.AfterConstraint;
import net.sf.bddbddb.order.OrderConstraint.BeforeConstraint;
import net.sf.bddbddb.order.OrderConstraint.InterleaveConstraint;
import weka.classifiers.Classifier;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * WekaInterface
 * 
 * @author jwhaley
 * @version $Id: WekaInterface.java 531 2005-04-29 06:39:10Z joewhaley $
 */
public abstract class WekaInterface {
    
    public static OrderAttribute makeOrderAttribute(OrderConstraint c) {
        return new OrderAttribute(c.a, c.b);
    }
    
    public static OrderAttribute makeOrderAttribute(Object a, Object b) {
        if (OrderConstraint.compare(a, b)) {
            return new OrderAttribute(a, b);
        } else {
            return new OrderAttribute(b, a);
        }
    }
    
    public static int INTERLEAVE = 1;
    public static int getType(OrderConstraint oc) {
        if (oc instanceof BeforeConstraint) return 0;
        else if (oc instanceof InterleaveConstraint) return INTERLEAVE;
        else if (oc instanceof AfterConstraint) return 2;
        else return -1;
    }
    
    public static class OrderAttribute extends weka.core.Attribute {
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3257291339690555447L;

        Object a, b;
        
        static FastVector my_nominal_values = new FastVector(3);
        static {
            my_nominal_values.addElement("<");
            my_nominal_values.addElement("~");
            my_nominal_values.addElement(">"); 
        }
        
        private OrderAttribute(Object a, Object b) {
            super(a+","+b, my_nominal_values);
            this.a = a;
            this.b = b;
        }
        
        public OrderConstraint getConstraint(int k) {
            switch (k) {
                case 0: return OrderConstraint.makePrecedenceConstraint(a, b);
                case 1: return OrderConstraint.makeInterleaveConstraint(a, b);
                case 2: return OrderConstraint.makePrecedenceConstraint(b, a);
                default: return null;
            }
        }
        
        public OrderConstraint getConstraint(weka.core.Instance i) {
            int k = (int) i.value(this);
            return getConstraint(k);
        }

    }
    
    public static void addAllPairs(FastVector v, Collection c) {
        Collection pairs = new HashSet();
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            Object a = i.next();
            Iterator j = c.iterator();
            while (j.hasNext() && j.next() != a) ;
            while (j.hasNext()) {
                Object b = j.next();
                UnorderedPair pair = new UnorderedPair(a,b);
                if(pairs.contains(pair)) continue;
                OrderAttribute oa = makeOrderAttribute(a, b);
                v.addElement(oa);
                pairs.add(pair);
            }
        }
   //     System.out.println(new HashSet(c) + " Size: " + v.size());
        
    }
    
    public static Collection /*UnorderedPair*/ generateAllPairs(Collection c){
        Collection pairs = new HashSet();
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            Object a = i.next();
            Iterator j = c.iterator();
            while (j.hasNext() && j.next() != a) ;
            while (j.hasNext()) {
                Object b = j.next();
                Pair pair = new UnorderedPair(a, b);
                pairs.add(pair);
            }
        }
        return pairs;
    }

    public static FastVector constructVarAttributes(Collection vars) {
        FastVector v = new FastVector();
        addAllPairs(v, vars);
        return v;
    }
    
    public static FastVector constructAttribAttributes(InferenceRule ir, Collection vars) {
        Collection attribs = new LinkedList();
        for (Iterator i = vars.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            Attribute a = ir.getAttribute(v);
            if (a != null) attribs.add(a);
        }
        FastVector v = new FastVector();
        addAllPairs(v, attribs);
        return v;
    }
    
    public static FastVector constructDomainAttributes(InferenceRule ir, Collection vars) {
        Collection domains = new LinkedList();
        for (Iterator i = vars.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            Attribute a = ir.getAttribute(v);
            if (a != null) domains.add(a.getDomain());
        }
        FastVector v = new FastVector();
        addAllPairs(v, domains);
        return v;
    }
    
    public static weka.core.Attribute makeBucketAttribute(int numClusters) {
        FastVector clusterValues = new FastVector(numClusters);
        for (int i = 0; i < numClusters; ++i)
            clusterValues.addElement(Integer.toString(i));
        return new weka.core.Attribute("costBucket", clusterValues);
    }

    public static Classifier buildClassifier(String cClassName, Instances data) {
        // Build the classifier.
        Classifier classifier = null;
        try {
            long time = System.currentTimeMillis();
            classifier = (Classifier) Class.forName(cClassName).newInstance();
            classifier.buildClassifier(data);
            if (FindBestDomainOrder.TRACE > 1) System.out.println("Classifier "+cClassName+" took "+(System.currentTimeMillis()-time)+" ms to build.");
            if (FindBestDomainOrder.TRACE > 2) System.out.println(classifier);
        } catch (Exception x) {
            FindBestDomainOrder.out.println(cClassName + ": " + x.getLocalizedMessage());
            return null;
        }
        return classifier;
    }

    public static double leaveOneOutCV(Instances data, String cClassName) {
        return WekaInterface.cvError(data.numInstances(), data, cClassName);
    }

    public static double cvError(int numFolds, Instances data0, String cClassName) {
        if (data0.numInstances() < numFolds)
            return Double.NaN; //more folds than elements
        if (numFolds == 0)
            return Double.NaN; // no folds
        if (data0.numInstances() == 0)
            return 0; //no instances
    
        Instances data = new Instances(data0);
        //data.randomize(new Random(System.currentTimeMillis()));
        data.stratify(numFolds);
        Assert._assert(data.classAttribute() != null);
        double[] estimates = new double[numFolds];
        for (int i = 0; i < numFolds; ++i) {
            Instances trainData = data.trainCV(numFolds, i);
            Assert._assert(trainData.classAttribute() != null);
            Assert._assert(trainData.numInstances() != 0, "Cannot train classifier on 0 instances.");
    
            Instances testData = data.testCV(numFolds, i);
            Assert._assert(testData.classAttribute() != null);
            Assert._assert(testData.numInstances() != 0, "Cannot test classifier on 0 instances.");
    
            int temp = FindBestDomainOrder.TRACE;
            FindBestDomainOrder.TRACE = 0;
            Classifier classifier = buildClassifier(cClassName, trainData);
            FindBestDomainOrder.TRACE = temp;
            int count = testData.numInstances();
            double loss = 0;
            double sum = 0;
            for (Enumeration e = testData.enumerateInstances(); e.hasMoreElements();) {
                Instance instance = (Instance) e.nextElement();
                Assert._assert(instance != null);
                Assert._assert(instance.classAttribute() != null && instance.classAttribute() == trainData.classAttribute());
                try {
                    double testClass = classifier.classifyInstance(instance);
                    double weight = instance.weight();
                    if (testClass != instance.classValue())
                        loss += weight;
                    sum += weight;
                } catch (Exception ex) {
                    FindBestDomainOrder.out.println("Exception while classifying: " + instance + "\n" + ex);
                }
            }
            estimates[i] = 1 - loss / sum;
        }
        double average = 0;
        for (int i = 0; i < numFolds; ++i)
            average += estimates[i];
    
        return average / numFolds;
    }

    public static TrialInstances binarize(double classValue, TrialInstances data) {
        TrialInstances newInstances = data.infoClone();
        weka.core.Attribute newAttr = makeBucketAttribute(2);
        TrialInstances.setIndex(newAttr, newInstances.classIndex());
        newInstances.setClass(newAttr);
        newInstances.setClassIndex(data.classIndex());
        for (Enumeration e = data.enumerateInstances(); e.hasMoreElements();) {
            TrialInstance instance = (TrialInstance) e.nextElement();
            TrialInstance newInstance = TrialInstance.cloneInstance(instance);
            newInstance.setDataset(newInstances);
            if (instance.classValue() <= classValue) {
                newInstance.setClassValue(0);
            } else {
                newInstance.setClassValue(1);
            }
            newInstances.add(newInstance);
        }
        return newInstances;
    }

    public static class OrderInstance extends Instance {
        
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3258412811553093939L;

        public static OrderInstance construct(Order o, Instances dataSet) {
            return construct(o, dataSet, 1);
        }
        
        public static OrderInstance construct(Order o, Instances dataSet, double weight) {
            double[] d = new double[dataSet.numAttributes()];
            for (int i = 0; i < d.length; ++i) {
                d[i] = Instance.missingValue();
            }
            for (Iterator i = o.getConstraints().iterator(); i.hasNext(); ) {
                OrderConstraint oc = (OrderConstraint) i.next();
                // TODO: use a map from Pair to int instead of building String and doing linear search.
            
                String cName = oc.getFirst()+","+oc.getSecond();
                OrderAttribute oa = (OrderAttribute) dataSet.attribute(cName);
                if (oa != null) {
                    
                    if(oc.getFirst().equals(oc.getSecond()) && d[oa.index()] == INTERLEAVE) 
                        continue;
                    /* TODO should only one type of constraint for
                     * when first == second and they are not interleaved
                     */  
                    d[oa.index()] = getType(oc);
                } else {
                    
                    System.out.println("Warning: while building OrderInstance for " + o + " couldn't find constraint "+oc+" in data set");
                    System.out.println("dataset\n: " + dataSet);
                    Assert.UNREACHABLE();
                }
            }
            return new OrderInstance(weight, d, o);
        }
        
        protected Order o;
        
        protected OrderInstance(double w, double[] d, Order o) {
            super(w, d);
            this.o = o;
        }
        protected OrderInstance(OrderInstance that) {
            super(that);
            this.o = that.o;
        }
        
        public Object copy() {
            return new OrderInstance(this);
        }
        
        public Order getOrder() {
            return o;
        }
        
    }

}
