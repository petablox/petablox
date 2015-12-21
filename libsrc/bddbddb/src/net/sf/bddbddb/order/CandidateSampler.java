/*
 * Created on Jan 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;

import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.order.WekaInterface.OrderInstance;

/**
 * @author mcarbin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public  abstract class CandidateSampler {
    //probably should do something else for these, but it's better than not moving the code
    static int TRACE = FindBestDomainOrder.TRACE;
    static PrintStream out = FindBestDomainOrder.out;
    static Random random = new Random(System.currentTimeMillis());
    int sampleSize;
    public static String CPE = "net.sf.bddbddb.order.BaggedId3";
    
    public static Collection sample(int sampleSize, Order [] orders, double [] distribution, double normalFact){
        Collection ordersToTry = new LinkedHashSet();
        for(int i = 0; i < sampleSize; ++i){
            double choice = random.nextDouble() * normalFact;
            double current = 0.;
            for(int j = 0; j < distribution.length; ++j) {
                current += distribution[j];
                if (current > choice) {
                    ordersToTry.add(orders[j]);
                    break;
                }
            }
        }
        return ordersToTry;
    }
    
    public abstract Collection sample(Collection orders,
            TrialInstances vData, TrialInstances aData, TrialInstances dData, InferenceRule ir, boolean force);
    
    public static class UncertaintySampler extends CandidateSampler{
        double uncertaintyThreshold;
        double vCenter, aCenter, dCenter;
        double maxScore;
        public UncertaintySampler(int sampleSize, double uncertaintyThreshold, double vCenter, double aCenter, double dCenter){
            this.sampleSize = sampleSize;
            this.uncertaintyThreshold = uncertaintyThreshold;
            this.vCenter = vCenter;
            this.aCenter = aCenter;
            this.dCenter = dCenter;
            double vFurthest = Math.max(vCenter, 1 - vCenter);
            double aFurthest = Math.max(aCenter, 1 - aCenter);
            double dFurthest = Math.max(dCenter, 1 - dCenter);
            this.maxScore = RMS(vFurthest, vCenter, aFurthest, aCenter, dFurthest, dCenter);
            
        }
        
        
        public static double RMS(double vProb, double vCent, double aProb, double aCent, double dProb, double dCent) {
            double vDiff = Math.abs(vProb - vCent);
            double aDiff = Math.abs(aProb - aCent);
            double dDiff = Math.abs(dProb - dCent);
            return Math.sqrt(((vDiff * vDiff) + (aDiff * aDiff) + (dDiff * dDiff)) / 3);
            
        }
        
        public Collection sample(Collection orders,
                TrialInstances vData, TrialInstances aData, TrialInstances dData, InferenceRule ir, boolean force) {
            ClassProbabilityEstimator vCPE = null, aCPE  = null, dCPE = null;
            vCPE = (ClassProbabilityEstimator) WekaInterface.buildClassifier(CPE, WekaInterface.binarize(0, vData));
            aCPE = (ClassProbabilityEstimator) WekaInterface.buildClassifier(CPE, WekaInterface.binarize(0, aData));
            dCPE = (ClassProbabilityEstimator) WekaInterface.buildClassifier(CPE, WekaInterface.binarize(0, dData));
            
            OrderTranslator v2a = new VarToAttribTranslator(ir);
            OrderTranslator a2d = AttribToDomainTranslator.INSTANCE;
            
            Order best = null;
            double bestScore = Double.POSITIVE_INFINITY;
            double [] distribution = new double[orders.size()];
            double normalFact = 0;
            Order [] orderArr = new Order[orders.size()];
            int i = 0;
            for (Iterator it = orders.iterator(); it.hasNext(); ++i){
                Order o_v = (Order) it.next();
                orderArr[i] = o_v;
                OrderInstance vInstance = TrialInstance.construct(o_v, vData);
                Order o_a = v2a.translate(o_v);
                OrderInstance aInstance = TrialInstance.construct(o_a, aData);
                Order o_d = a2d.translate(o_a);
                OrderInstance dInstance = TrialInstance.construct(o_d, dData);
                
                double vScore = vCPE != null ? vCPE.classProbability(vInstance, 0) : vCenter;
                double aScore = aCPE != null ? aCPE.classProbability(aInstance, 0) : aCenter;
                double dScore = dCPE != null ? dCPE.classProbability(dInstance, 0) : dCenter;
                
                double score = RMS(vScore, vCenter, aScore, aCenter, dScore, dCenter);
                distribution[i] = maxScore - score;
                normalFact += maxScore - score;
                
                if (score < bestScore) {
                    if (TRACE > 1){ 
                        out.println("Uncertain order "+o_v+" score: "+FindBestDomainOrder.format(score)
                                + " (v="+FindBestDomainOrder.format(vScore)+",a="
                                + FindBestDomainOrder.format(aScore)+",d="
                                + FindBestDomainOrder.format(dScore)+")");
                        double vVariance = vCPE != null ? vCPE.classVariance(vInstance, 0) : 0;
                        double aVariance = aCPE != null ? aCPE.classVariance(aInstance, 0) : 0;
                        double dVariance = dCPE != null ? dCPE.classVariance(dInstance, 0) : 0;
                        /*out.println("\tVariances: var:" + FindBestDomainOrder.format(vVariance) 
                         + " attrib:"
                         + FindBestDomainOrder.format(aVariance)
                         + " dom:" +dVariance);
                         */
                    }
                    
                    bestScore = score;
                    best = o_v;
                }
            }
            Collection ordersToTry = new LinkedList();
            if (force || bestScore < uncertaintyThreshold) {
                if (sampleSize > 1) {
                    return sample(sampleSize, orderArr, distribution, normalFact);
                }
                if(best != null) //don't add null order to list
                    ordersToTry.add(best);
            }
            return ordersToTry;
        }
        
    }
    
    public static class LocalVarianceSampler{
        int numEstimators;
        int sampleSize;
        
        /**
         * @param numEstimators
         */
        public LocalVarianceSampler(int sampleSize, int numEstimators) {
            this.sampleSize = sampleSize;
            this.numEstimators = numEstimators;
        }
        public Collection localVariance(Collection orders, TrialInstances vData, TrialInstances aData, TrialInstances dData, InferenceRule ir) {
            ClassProbabilityEstimator [] vEstimators = new ClassProbabilityEstimator[numEstimators];
            ClassProbabilityEstimator [] aEstimators = new ClassProbabilityEstimator[numEstimators];
            ClassProbabilityEstimator [] dEstimators = new ClassProbabilityEstimator[numEstimators];
            
            for(int i = 0; i < numEstimators; ++i){
                TrialInstances vBootData = (TrialInstances) vData.resample(random);
                TrialInstances aBootData = (TrialInstances) aData.resample(random);
                TrialInstances dBootData = (TrialInstances) dData.resample(random);
                vEstimators[i] = (ClassProbabilityEstimator) WekaInterface.buildClassifier(CPE, WekaInterface.binarize(0, vBootData));
                aEstimators[i] = (ClassProbabilityEstimator) WekaInterface.buildClassifier(CPE, WekaInterface.binarize(0, aBootData));
                dEstimators[i] = (ClassProbabilityEstimator) WekaInterface.buildClassifier(CPE, WekaInterface.binarize(0, dBootData));
            }
            
            double [] distribution = new double[orders.size()];
            Order[] orderArr = new Order[orders.size()];
            
            double normalFact = 0;
            double [][] estimates = new double[numEstimators + 1][3];
            
            OrderTranslator v2a = new VarToAttribTranslator(ir);
            OrderTranslator a2d = AttribToDomainTranslator.INSTANCE;
            
            int i = 0;
            for(Iterator it = orders.iterator(); it.hasNext(); ++i){
                Order o_v = (Order) it.next();
                //a little sketchy, since this is different data but it should work
                OrderInstance vInstance = TrialInstance.construct(o_v, vData);
                Order o_a = v2a.translate(o_v);
                OrderInstance aInstance = TrialInstance.construct(o_a, aData);
                Order o_d = a2d.translate(o_a);
                OrderInstance dInstance = TrialInstance.construct(o_d, dData);
                
                orderArr[i] = o_v;
                estimates[numEstimators][0] = 0;
                estimates[numEstimators][1] = 0;
                estimates[numEstimators][2] = 0;
                for(int j = 0; j < numEstimators; ++j){
                    estimates[j][0] = vEstimators[j] != null ? vEstimators[j].classProbability(vInstance,0) : 0.5;
                    estimates[j][1] = aEstimators[j] != null ? aEstimators[j].classProbability(aInstance,0) : 0.5;
                    estimates[j][2] = dEstimators[j] != null ? dEstimators[j].classProbability(dInstance,0) : 0.5;
                    estimates[numEstimators][0] += vEstimators[j] != null ? estimates[j][0] : 0;
                    estimates[numEstimators][1] += aEstimators[j] != null ? estimates[j][1] : 0;
                    estimates[numEstimators][2] += dEstimators[j] != null ? estimates[j][2] : 0;
                }
                estimates[numEstimators][0] /= numEstimators;
                estimates[numEstimators][1] /= numEstimators;
                estimates[numEstimators][2] /= numEstimators;
                
                for(int j = 0; j < numEstimators; ++j){
                    double vDiff = estimates[j][0] - estimates[numEstimators][0];
                    double aDiff = estimates[j][1] - estimates[numEstimators][1];
                    double dDiff = estimates[j][2] - estimates[numEstimators][2];
                    distribution[i] += (vDiff * vDiff) + (aDiff * aDiff) + (dDiff * dDiff);
                }
                normalFact += distribution[i];
            }
            
            /*   Collection ordersToTry = new LinkedHashSet();
             for(i = 0; i < SAMPLE_SIZE; ++i){
             double choice = random.nextDouble() * normalFact;
             double current = 0.;
             for(int j = 0; j < distribution.length; ++j) {
             current += distribution[j];
             if (current > choice) {
             ordersToTry.add(orderArr[j]);
             break;
             }
             }
             }
             return ordersToTry;*/
            return sample(sampleSize, orderArr, distribution, normalFact);
        }
    }
}
        