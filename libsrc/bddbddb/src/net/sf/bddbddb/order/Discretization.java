/*
 * Created on Jan 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

public class Discretization {
    public double[] cutPoints;

    public TrialInstances[] buckets;

    public Discretization(double[] cutPoints, TrialInstances[] buckets) {
        this.cutPoints = cutPoints;
        this.buckets = buckets;
    }
}