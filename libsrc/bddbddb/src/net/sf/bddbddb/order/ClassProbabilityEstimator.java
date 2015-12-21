/*
 * Created on Nov 9, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class ClassProbabilityEstimator extends Classifier{
    public abstract double classProbability(Instance instance, double targetClass);
    public abstract double classVariance(Instance instaance, double targetClass);
    public abstract Instances getData();
}
