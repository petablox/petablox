/*
 * Created on Jan 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;



public class TrialGuess {
    public Order order;

    public TrialPrediction prediction;

    public TrialGuess(Order o, TrialPrediction p) {
        order = o;
        prediction = p;
    }

    public Order getOrder() {
        return order;
    }

    public TrialPrediction getPrediction() {
        return prediction;
    }

    public String toString() {
        return "Order: " + order.toString() + " prediction: " + prediction.toString();
    }
}