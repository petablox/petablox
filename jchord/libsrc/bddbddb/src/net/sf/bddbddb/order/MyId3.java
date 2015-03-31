// MyId3.java, created Oct 31, 2004 2:13:00 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jwutil.util.Assert;
import net.sf.bddbddb.FindBestDomainOrder;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.Id3;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NoSupportForMissingValuesException;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.UnsupportedClassTypeException;
import weka.core.Utils;

/**
 * Class implementing an Id3 decision tree classifier. This version differs from
 * the weka one in that it supports missing attributes.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author John Whaley
 * @version $Revision: 531 $
 */
public class MyId3 extends Classifier {
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3258129154733322289L;
    
    /** The node's successors. */
    private MyId3[] m_Successors;
    /** Attribute used for splitting. */
    private Attribute m_Attribute; // not set for leaf.
    /** Class value if node is leaf. */
    private double m_ClassValue;
    /** Class distribution if node is leaf. */
    private double[] m_Distribution;
    /** Class attribute of dataset. */
    private Attribute m_ClassAttribute;

    public boolean getAttribCombos(Instances i, double cv) {
        List r = getAttribCombos(i.numAttributes(), cv);
        if (r == null) return false;
        for (Iterator ii = r.iterator(); ii.hasNext(); ) {
            double[] d = (double[]) ii.next();
            i.add(new Instance(1., d));
        }
        return true;
    }
    
    public List getAttribCombos(int nAttribs, double cv) {
        if (m_Attribute == null) {
            if (FindBestDomainOrder.compare(m_ClassValue, cv) == 0) {
                List result = new LinkedList();
                double[] i = new double[nAttribs];
                Arrays.fill(i, Double.NaN);
                result.add(i);
                return result;
            } else {
                return null;
            }
        } else {
            List result = new LinkedList(); 
            for (int i = 0; i < m_Successors.length; ++i) {
                List c = m_Successors[i].getAttribCombos(nAttribs, cv);
                if (c != null) {
                    int index = m_Attribute.index();
                    for (Iterator j = c.iterator(); j.hasNext(); ) {
                        double[] d = (double[]) j.next();
                        d[index] = i;
                    }
                    result.addAll(c);
                }
            }
            if (result.isEmpty()) return null;
            else return result;
        }
    }
    
    /**
     * Returns a string describing the classifier.
     * 
     * @return a description suitable for the GUI.
     */
    public String globalInfo() {
        return "Class for constructing an unpruned decision tree based on the ID3 "
            + "algorithm. Can only deal with nominal attributes. "
            + "Empty leaves may result in unclassified instances. For more "
            + "information see: \n\n" + " R. Quinlan (1986). \"Induction of decision "
            + "trees\". Machine Learning. Vol.1, No.1, pp. 81-106";
    }

    /**
     * Builds Id3 decision tree classifier.
     * 
     * @param data
     *            the training data
     * @exception Exception
     *                if classifier can't be built successfully
     */
    public void buildClassifier(Instances data) throws Exception {
        if (!data.classAttribute().isNominal()) {
            throw new UnsupportedClassTypeException("Id3: nominal class, please.");
        }
        Enumeration enumAtt = data.enumerateAttributes();
        while (enumAtt.hasMoreElements()) {
            if (!((Attribute) enumAtt.nextElement()).isNominal()) {
                throw new UnsupportedAttributeTypeException("Id3: only nominal "
                    + "attributes, please.");
            }
        }
        data = new Instances(data);
        data.deleteWithMissingClass();
        makeTree(data);
    }

    /**
     * Method for building an Id3 tree.
     * 
     * @param data
     *            the training data
     * @exception Exception
     *                if decision tree can't be built successfully
     */
    private void makeTree(Instances data) throws Exception {
        // Check if no instances have reached this node.
        if (data.numInstances() == 0) {
            m_Attribute = null;
            m_ClassValue = Instance.missingValue();
            m_Distribution = new double[data.numClasses()];
            double sum = 0;
            laplaceSmooth(m_Distribution, sum, data.numClasses());
            return;
        }
        // Compute attribute with maximum information gain.
        double[] infoGains = new double[data.numAttributes()];
        Enumeration attEnum = data.enumerateAttributes();
        while (attEnum.hasMoreElements()) {
            Attribute att = (Attribute) attEnum.nextElement();
            infoGains[att.index()] = computeInfoGain(data, att);
        }
        m_Attribute = data.attribute(Utils.maxIndex(infoGains));
        boolean makeLeaf;
        makeLeaf = Utils.eq(infoGains[m_Attribute.index()], 0);
        Instances[] splitData = null;
        if (!makeLeaf) {
            splitData = splitData(data, m_Attribute);
            for (int i = 0; i < splitData.length; ++i) {
                if (splitData[i].numInstances() == data.numInstances()) {
                    //System.out.println("When splitting on attrib
                    // "+m_Attribute+", child "+i+" is same size as current,
                    // making into leaf.");
                    makeLeaf = true;
                    break;
                }
            }
        }
        // Make leaf if information gain is zero.
        // Otherwise create successors.
        if (makeLeaf) {
            m_Attribute = null;
            m_Distribution = new double[data.numClasses()];
            Enumeration instEnum = data.enumerateInstances();
            double sum = 0;
            while (instEnum.hasMoreElements()) {
                Instance inst = (Instance) instEnum.nextElement();
                m_Distribution[(int) inst.classValue()]++;
                sum += inst.weight();
            }
            //laplace smooth the distribution instead
            laplaceSmooth(m_Distribution, sum, data.numClasses());
            //Utils.normalize(m_Distribution);
            m_ClassValue = Utils.maxIndex(m_Distribution);
            m_ClassAttribute = data.classAttribute();
        } else {
            m_Successors = new MyId3[m_Attribute.numValues()];
            for (int j = 0; j < m_Attribute.numValues(); j++) {
                m_Successors[j] = new MyId3();
                m_Successors[j].buildClassifier(splitData[j]);
            }
        }
    }
    
    public void laplaceSmooth(double [] dist, double sum, int numClasses){
        for(int i = 0; i < dist.length; ++i){
            dist[i] = (dist[i] + 1)/ (sum + numClasses);
        }
    }

    /**
     * Classifies a given test instance using the decision tree.
     * 
     * @param instance
     *            the instance to be classified
     * @return the classification
     */
    public double classifyInstance(Instance instance) {
        if (m_Attribute == null) {
            return m_ClassValue;
        } else if (instance.isMissing(m_Attribute)) {
            try {
                // Use superclass implementation, which uses distributionForInstance.
                return super.classifyInstance(instance);
            } catch (Exception x) {
                x.printStackTrace();
                Assert.UNREACHABLE();
                return 0.;
            }
        } else {
            return m_Successors[(int) instance.value(m_Attribute)].classifyInstance(instance);
        }
    }

    /**
     * Computes class distribution for instance using decision tree.
     * 
     * @param instance
     *            the instance for which distribution is to be computed
     * @return the class distribution for the given instance
     */
    public double[] distributionForInstance(Instance instance)
        throws NoSupportForMissingValuesException {
        if (m_Attribute == null) {
            return m_Distribution;
        } else if (instance.isMissing(m_Attribute)) {
            double[] d = new double[0];
            for (int i = 0; i < m_Successors.length; ++i) {
                double[] dd = m_Successors[i].distributionForInstance(instance);
                if (d.length == 0 && dd.length > 0) d = new double[dd.length];
                for (int j = 0; j < d.length; ++j) {
                    d[j] += dd[j];
                }
            }
            for (int j = 0; j < d.length; ++j) {
                d[j] /= m_Successors.length;
            }
            return d;
        } else {
            return m_Successors[(int) instance.value(m_Attribute)]
                .distributionForInstance(instance);
        }
    }

    /**
     * Prints the decision tree using the private toString method from below.
     * 
     * @return a textual description of the classifier
     */
    public String toString() {
        if ((m_Distribution == null) && (m_Successors == null)) {
            return "Id3: No model built yet.";
        }
        return "Id3\n\n" + toString(0);
    }

    /**
     * Computes information gain for an attribute.
     * 
     * @param data
     *            the data for which info gain is to be computed
     * @param att
     *            the attribute
     * @return the information gain for the given attribute and data
     */
    private double computeInfoGain(Instances data, Attribute att) throws Exception {
        double infoGain = computeEntropy(data, att);
        Instances[] splitData = splitData(data, att);
        for (int j = 0; j < att.numValues(); j++) {
            if (splitDataSize[j] > 0) {
                infoGain -= ((double) splitDataSize[j] / (double) numI)
                    * computeEntropy(splitData[j], att);
            }
        }
        return infoGain;
    }

    /**
     * Computes the entropy of a dataset.
     * 
     * @param data
     *            the data for which entropy is to be computed
     * @return the entropy of the data's class distribution
     */
    private double computeEntropy(Instances data, Attribute att) throws Exception {
        double[] classCounts = new double[data.numClasses()];
        Enumeration instEnum = data.enumerateInstances();
        int numInstances = 0;
        while (instEnum.hasMoreElements()) {
            Instance inst = (Instance) instEnum.nextElement();
            if (inst.isMissing(att)) continue;
            classCounts[(int) inst.classValue()]++;
            ++numInstances;
        }
        double entropy = 0;
        for (int j = 0; j < data.numClasses(); j++) {
            if (classCounts[j] > 0) {
                entropy -= classCounts[j] * Utils.log2(classCounts[j]);
            }
        }
        entropy /= (double) numInstances;
        return entropy + Utils.log2(numInstances);
    }
    int numI;
    int splitDataSize[];

    /**
     * Splits a dataset according to the values of a nominal attribute.
     * 
     * @param data
     *            the data which is to be split
     * @param att
     *            the attribute to be used for splitting
     * @return the sets of instances produced by the split
     */
    private Instances[] splitData(Instances data, Attribute att) {
        numI = 0;
        splitDataSize = new int[att.numValues()];
        Instances[] splitData = new Instances[att.numValues()];
        for (int j = 0; j < att.numValues(); j++) {
            splitData[j] = new Instances(data, data.numInstances());
        }
        Enumeration instEnum = data.enumerateInstances();
        while (instEnum.hasMoreElements()) {
            Instance inst = (Instance) instEnum.nextElement();
            if (inst.isMissing(att)) {
                // Add to all children.
                for (int k = 0; k < att.numValues(); ++k) {
                    splitData[k].add(inst);
                }
            } else {
                int k = (int) inst.value(att);
                splitData[k].add(inst);
                splitDataSize[k]++;
                numI++;
            }
        }
        return splitData;
    }

    /**
     * Outputs a tree at a certain level.
     * 
     * @param level
     *            the level at which the tree is to be printed
     */
    private String toString(int level) {
        StringBuffer text = new StringBuffer();
        if (m_Attribute == null) {
            if (Instance.isMissingValue(m_ClassValue)) {
                text.append(": null");
            } else {
                text.append(": " + m_ClassAttribute.value((int) m_ClassValue));
            }
        } else {
            for (int j = 0; j < m_Attribute.numValues(); j++) {
                text.append("\n");
                for (int i = 0; i < level; i++) {
                    text.append("|  ");
                }
                text.append(m_Attribute.name() + " = " + m_Attribute.value(j));
                text.append(m_Successors[j].toString(level + 1));
            }
        }
        return text.toString();
    }

    /**
     * Main method.
     *
     * @param args the options for the classifier
     */
    public static void main(String[] args) {
        try {
            System.out.println(Evaluation.evaluateModel(new Id3(), args));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}