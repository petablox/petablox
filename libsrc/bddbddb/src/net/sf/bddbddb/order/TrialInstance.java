/*
 * Created on Jan 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import java.util.Iterator;
import jwutil.util.Assert;
import net.sf.bddbddb.BDDInferenceRule;
import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.order.WekaInterface.OrderAttribute;
import net.sf.bddbddb.order.WekaInterface.OrderInstance;
import weka.core.Instance;


public class TrialInstance extends OrderInstance implements Comparable {

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3689626995428701492L;

    public static TrialInstance construct(TrialInfo ti, Order o, double cost, TrialInstances dataSet) {
        return construct(ti, o, cost, dataSet, 1);
    }

    public static TrialInstance construct(TrialInfo ti, Order o, double cost, TrialInstances dataSet, double weight) {
        double[] d = new double[dataSet.numAttributes()];
        for (int i = 0; i < d.length; ++i) {
            d[i] = Instance.missingValue();
        }
      
        for (Iterator i = o.getConstraints().iterator(); i.hasNext();) {
            OrderConstraint oc = (OrderConstraint) i.next();
            // TODO: use a map from Pair to int instead of building String and doing linear search.
     
            
            String cName = oc.getFirst() + "," + oc.getSecond();
            OrderAttribute oa = (OrderAttribute) dataSet.attribute(cName);
            if (oa != null) {
                if(oc.getFirst().equals(oc.getSecond()) && d[oa.index()] == WekaInterface.INTERLEAVE) 
                    continue;
               /* TODO should only one type of constraint for
                * when first == second and they are not interleaved
                */  
                d[oa.index()] = WekaInterface.getType(oc);
            } else {
                System.out.println("Warning: cannot find constraint "+oc+" order "+ti.order+" in dataset "+dataSet);
                Assert.UNREACHABLE();
                return null;
            }
        }
        d[d.length - 1] = cost;
        return new TrialInstance(weight, d, o, ti);
    }

    public TrialInfo ti;

    public TrialInstance(double weight, double[] d, Order o, TrialInfo ti) {
        super(weight, d, o);
        this.ti = ti;
    }

    protected TrialInstance(TrialInstance that) {
        super(that);
        this.ti = that.ti;
    }

    public TrialInfo getTrialInfo() { return ti; }

    public double getCost() {
        return value(numAttributes() - 1);
    }
    
    public void setCost(double cost){
        this.setValue(numAttributes() -1 , cost);
    }
    
    public void recomputeCost(double best){
        setCost((double) (ti.cost + 1) / best);
    }

    public Object copy() {
        return new TrialInstance(this);
    }

    public int compareTo(Object arg0) {
        TrialInstance that = (TrialInstance) arg0;
        return FindBestDomainOrder.compare(this.getCost(), that.getCost());
    }

    public boolean isMaxTime() {
        return ti.cost >= BDDInferenceRule.LONG_TIME;
    }
    
    public static TrialInstance cloneInstance(TrialInstance instance){
        return new TrialInstance(instance.weight(), instance.toDoubleArray(), instance.getOrder(), instance.getTrialInfo());
    }
    
    /* just tests if the orders are equal */
    public boolean equals(Object o){
        return this.ti.order.equals(((TrialInstance) o).ti.order);
    }

    public int hashCode() {
        return this.ti.hashCode();
    }
}
