/*
 * Created on Jan 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Random;

import jwutil.util.Assert;
import net.sf.bddbddb.FindBestDomainOrder;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;


public class TrialInstances extends Instances {

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 4049922649398589241L;

    /**
     * @param name
     * @param attInfo
     * @param capacity
     */
    public TrialInstances(String name, FastVector attInfo, int capacity) {
        super(name, attInfo, capacity);
    }

    public Discretization threshold(double thres) {
        return threshold(thres, this.classIndex());
    }

    public Discretization threshold(double thres, int index) {
        if (numInstances() == 0) return null;
        FastVector clusterValues = new FastVector(2);
        TrialInstances[] buckets = new TrialInstances[2];
        FastVector origAttributes = (FastVector) this.m_Attributes.copy(); //shared across all buckets

        buckets[0] = new TrialInstances(this.m_RelationName + "_bucket_0", origAttributes, 30);
        buckets[0].setClassIndex(classIndex());
        buckets[1] = new TrialInstances(this.m_RelationName + "_bucket_1", origAttributes, 30);
        buckets[1].setClassIndex(classIndex());
        double[] cutPoint = new double[1];
        cutPoint[0] = thres;

        clusterValues.addElement("<" + FindBestDomainOrder.format(thres));
        clusterValues.addElement(">" + FindBestDomainOrder.format(thres));
        weka.core.Attribute a = new weka.core.Attribute("costThres" + FindBestDomainOrder.format(thres), clusterValues);
        m_Attributes.setElementAt(a, index);
        setIndex(a, index);
        Enumeration f = m_Instances.elements();
        while (f.hasMoreElements()) {
            TrialInstance old_i = (TrialInstance) f.nextElement();
            double oldVal = old_i.value(index);
            double val = oldVal < thres ? 0 : 1;
            //deep copy order and trial?
            double[] old_i_arr = old_i.toDoubleArray();
            double[] old_i_copy = new double[old_i_arr.length];
            System.arraycopy(old_i_arr, 0, old_i_copy, 0, old_i_arr.length);
            buckets[(int) val].add(new TrialInstance(old_i.weight(), old_i_copy, old_i.getOrder(), old_i.getTrialInfo()));
            old_i.setValue(index, val);
        }

        return new Discretization(cutPoint, buckets);
    }

    public Discretization discretize(double power) {
        int numBins = (int) Math.pow(numInstances(), power);
        return discretize(new MyDiscretize(power), numBins, this.classIndex());
    }

    public Discretization discretize(Discretize d, int numBins, int index) {
        if (numInstances() <= 1) return null;
        try {
            int classIndex = this.classIndex();
            Assert._assert(classIndex >= 0);
            setClassIndex(-1); // clear class instance for discretization.
            d.setAttributeIndices(Integer.toString(index+1)); // RANGE IS 1-BASED!!!
            d.setInputFormat(this); // NOTE: this must be LAST because it calls setUpper
            Instances newInstances;
            newInstances = Filter.useFilter(this, d);
            
            if (d.getFindNumBins()) 
                numBins = d.getBins();
            
            TrialInstances[] buckets = new TrialInstances[numBins];
            System.out.println("Num trials: " + numInstances() + " Num bins: " + numBins);
            //System.out.println("me: " + this);
            FastVector origAttributes = (FastVector) this.m_Attributes.copy(); //shared across all buckets
        
            for (int i = 0; i < numBins; ++i) {
                buckets[i] = new TrialInstances(this.m_RelationName + "_bucket_" + i, origAttributes, this.numInstances() / numBins);
                buckets[i].setClassIndex(classIndex);
            }
            double[] result = d.getCutPoints(index);
           
            weka.core.Attribute a = WekaInterface.makeBucketAttribute(numBins);
            m_Attributes.setElementAt(a, index);
            setIndex(a, index);
            Enumeration e = newInstances.enumerateInstances();
            Enumeration f = m_Instances.elements();
           
           // System.out.println("New Instances: " + newInstances);
            while (e.hasMoreElements()) {
                Instance new_i = (Instance) e.nextElement();
                TrialInstance old_i = (TrialInstance) f.nextElement();
                double val = new_i.value(index);
                double[] old_i_arr = old_i.toDoubleArray();
                double[] old_i_copy = new double[old_i_arr.length];
                System.arraycopy(old_i_arr, 0, old_i_copy, 0, old_i_arr.length);
                buckets[(int) val].add(new TrialInstance(old_i.weight(), old_i_copy, old_i.getOrder(), old_i.getTrialInfo()));
                old_i.setValue(index, val);
            }
            Assert._assert(!f.hasMoreElements());
            setClassIndex(classIndex); // reset class index.
            return new Discretization(result, buckets);
        } catch (Exception x) {
            System.out.flush();
            x.printStackTrace();
            System.exit(-1);
            return null;
        }
    }


    public static void setIndex(weka.core.Attribute a, int i) {
        try {
            Class c = Class.forName("weka.core.Attribute");
            Field f = c.getDeclaredField("m_Index");
            f.setAccessible(true);
            f.setInt(a, i);
        } catch (Exception x) {
            Assert.UNREACHABLE("weka sucks: " + x);
        }
    }
    public TrialInstances infoClone(){
        return new TrialInstances(this.m_RelationName, (FastVector) this.m_Attributes.copy(), this.numInstances());
    }
    
    public Instances resample(Random random) {
        TrialInstances newData = infoClone();
        while (newData.numInstances() < numInstances()) {
          newData.add(instance(random.nextInt(numInstances())));
        }
        newData.setClassIndex(classIndex());
        return newData;
      }
    
    /* Deep copy attributes and instances */
    public TrialInstances copy(){
        TrialInstances newInstances = infoClone();
        for (Enumeration e = enumerateInstances(); e.hasMoreElements();) {
            TrialInstance instance = (TrialInstance) e.nextElement();
            TrialInstance newInstance = TrialInstance.cloneInstance(instance);
            newInstance.setDataset(newInstances);
            newInstances.add(newInstance);
        }
        newInstances.setClassIndex(this.classIndex());
        return newInstances;
    }
    
}