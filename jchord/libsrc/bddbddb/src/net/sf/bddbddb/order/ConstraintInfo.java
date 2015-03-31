/*
 * Created on Jan 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import jwutil.math.Distributions;
import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.Solver;
import net.sf.bddbddb.Variable;
import net.sf.bddbddb.XMLFactory;

import org.jdom.Element;


/**
 * Information about a particular constraint.
 * 
 * @author jwhaley
 * @version $Id: ConstraintInfo.java 448 2005-03-07 06:58:48Z cs343 $
 */
public class ConstraintInfo implements Comparable {

    // Student-t test: requires both populations have equal means and
    // both distributions are normally distributed with equal variances

    // The usual models for confidence intervals:
    //    t tests, ANOVA, linear or curvilinear regression
    // These all require the following assumptions:
    //    independence of observations
    //    normality of sampling distribution
    //    uniformity of residuals

    /**
     * The constraint that this info is about.
     */
    OrderConstraint c;

    /**
     * The collection of trials that are used in the computation of the score.
     */
    Collection trials;

    /** * The rest of the fields are computed based on the trials. ** */

    double sumCost;

    double sumMinimumCost;

    double sumNormalizedCost;

    double sumNormalizedCostSq;

    int numTrials;

    /**
     * Construct a new ConstraintInfo.
     * 
     * @param c  constraint
     */
    public ConstraintInfo(OrderConstraint c) {
        this.c = c;
        this.trials = new LinkedList();
        this.sumCost = 0.;
        this.sumMinimumCost = 0.;
        this.sumNormalizedCost = 0.;
        this.sumNormalizedCostSq = 0.;
        this.numTrials = 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return c + ": score " + FindBestDomainOrder.format(getMean()) + " +- " + FindBestDomainOrder.format(getConfidenceInterval(.1));
    }

    /**
     * A measure of, when using an order with this constraint, how long
     * the operation would take versus the best time for that operation.
     * For example, a score of 2 would mean that on average, orders with
     * this constraint took twice as long on an operation as the best
     * known order for that operation.
     * 
     * Obviously, the best possible score is 1, and lower numbers are better.
     */
    public double getMean() {
        if (numTrials == 0) return 0.;
        return sumNormalizedCost / numTrials;
    }

    /**
     * The number of trials used in the computation of the score.
     */
    public int getNumberOfTrials() {
        return numTrials;
    }

    public static double getVariance(Collection cis) {
        double sum = 0.;
        double sumOfSquares = 0.;
        int n = 0;
        for (Iterator i = cis.iterator(); i.hasNext();) {
            ConstraintInfo ci = (ConstraintInfo) i.next();
            sum += ci.sumNormalizedCost;
            sumOfSquares += ci.sumNormalizedCostSq;
            n += ci.numTrials;
        }
        // variance = (n*sum(X^2) - (sum(X)^2))/n^2
        double variance = (sumOfSquares * n - sum * sum) / (n * n);
        return variance;
    }

    /**
     * The variance of the normalized times used in the computation of the score.
     */
    public double getVariance() {
        // variance = (n*sum(X^2) - (sum(X)^2))/n^2
        int n = numTrials;
        double variance = (sumNormalizedCostSq * n -
            sumNormalizedCost * sumNormalizedCost) / (n * n);
        return variance;
    }

    /**
     * The standard deviation of the normalized times used in the computation
     * of the score.
     */
    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    /**
     * The same as the score, but each trial is weighted by the absolute
     * time spent by the best trial of that operation. This means that
     * operations that took longer will be weighted in this score more
     * heavily.
     */
    public double getWeightedMean() {
        if (sumMinimumCost == 0.) return 0.;
        return sumCost / sumMinimumCost;
    }

    public double getMinimumCost() {
        return sumMinimumCost;
    }

    /**
     * Returns the confidence interval of the normalized times with the given
     * significance level.
     */
    public double getConfidenceInterval(double sigLevel) {
        // sample mean +/- t(a/2,N-1)s/sqrt(N)
        int N = getNumberOfTrials();
        if (N < 2) return Double.POSITIVE_INFINITY;
        double s = getStdDev();
        return Distributions.uc_stDist(sigLevel / 2, N - 1) * s / Math.sqrt(N);
    }

    public void registerTrial(TrialInfo t) {
        registerTrials(Collections.singleton(t));
    }

    /**
     * Register new trials with this ConstraintInfo.
     */
    public void registerTrials(Collection newTrials) {
        if (newTrials.isEmpty()) return;
        EpisodeCollection tc = ((TrialInfo) newTrials.iterator().next()).getCollection();
        long min = tc.getMinimum().cost + 1;
        sumMinimumCost += min;
        for (Iterator i = newTrials.iterator(); i.hasNext();) {
            TrialInfo t = (TrialInfo) i.next();
            Order o = t.order;
            //if (!o.obeysConstraint(c)) continue;
            sumCost += t.cost + 1;
            double normalized = (double) (t.cost + 1) / (double) min;
            sumNormalizedCost += normalized;
            sumNormalizedCostSq += normalized * normalized;
            trials.add(t);
            numTrials++;
        }
    }

    public int compareTo(Object o) {
        return compareTo((ConstraintInfo) o);
    }

    public int compareTo(ConstraintInfo that) {
        if (this == that) return 0;
        int result = FindBestDomainOrder.signum(that.getWeightedMean() - this.getWeightedMean());
        if (result == 0) {
            result = (int) FindBestDomainOrder.signum(this.getVariance() - that.getVariance());
            if (result == 0) {
                result = this.c.compareTo(that.c);
            }
        }
        return result;
    }

    /**
     * Dump this constraint info to the screen.
     */
    public void dump() {
        System.out.println("Constraint: " + c);
        System.out.print("  Average=" + FindBestDomainOrder.format(getMean()) + " (weighted=" + FindBestDomainOrder.format(getWeightedMean()));
        System.out.println(" stddev " + FindBestDomainOrder.format(getStdDev()) + " conf=+-" + FindBestDomainOrder.format(getConfidenceInterval(.1)));
        System.out.println("   Based on " + numTrials + " trials:");
        for (Iterator i = trials.iterator(); i.hasNext();) {
            TrialInfo ti = (TrialInfo) i.next();
            System.out.println("    " + ti.toString());
        }
    }

    public Element toXMLElement(Solver solver) {
        Element dis = new Element("constraintInfo");
        InferenceRule ir = null;
        if (c.isVariableConstraint()) ir = solver.getRuleThatContains((Variable) c.getFirst());
        Element constraint = c.toXMLElement(ir);
        dis.setAttribute("sumCost", Double.toString(sumCost));
        dis.setAttribute("sumMinimumCost", Double.toString(sumMinimumCost));
        dis.setAttribute("sumNormalizedCost", Double.toString(sumNormalizedCost));
        dis.setAttribute("sumNormalizedCostSq", Double.toString(sumNormalizedCostSq));
        dis.setAttribute("numTrials", Integer.toString(numTrials));
        return dis;
    }

    public static ConstraintInfo fromXMLElement(Element e, XMLFactory f) {
        OrderConstraint c = null;
        for (Iterator i = e.getContent().iterator(); i.hasNext();) {
            Element e2 = (Element) i.next();
            Object o = f.fromXML(e2);
            if (o instanceof OrderConstraint) {
                c = (OrderConstraint) o;
                break;
            }
        }
        if (c == null) return null;
        ConstraintInfo ci = new ConstraintInfo(c);
        ci.sumCost = Double.parseDouble(e.getAttributeValue("sumCost", "0."));
        ci.sumMinimumCost = Double.parseDouble(e.getAttributeValue("sumMinimumCost", "0."));
        ci.sumNormalizedCost = Double.parseDouble(e.getAttributeValue("sumNormalizedCost", "0."));
        ci.sumNormalizedCostSq = Double.parseDouble(e.getAttributeValue("sumNormalizedCostSq", "0."));
        ci.numTrials = Integer.parseInt(e.getAttributeValue("numTrials", "0"));
        return ci;
    }
}