// MyDiscretize.java, created Nov 10, 2004 10:24:08 PM by jwhaley
// Copyright (C) 2004 jwhaley
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.Enumeration;
import java.util.Vector;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;

/**
 * Discretizes numeric attributes using equal frequency binning where the
 * number of bins is equal to the square root of the number of non-missing
 * values.<p>
 *
 * Valid filter-specific options are: <p>
 *
 * -R col1,col2-col4,... <br>
 * Specifies list of columns to Discretize. First
 * and last are valid indexes. (default: first-last) <p>
 *
 * -V <br>
 * Invert matching sense.<p>
 *
 * -D <br>
 * Make binary nominal attributes. <p>
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 531 $
 */
public class MyDiscretize extends Discretize {

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3833183631698114354L;
    
    double power;
    
    public MyDiscretize() {
        this(0.5);
    }
    public MyDiscretize(double p) {
        this.power = p;
    }
    
  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the input format can't be set successfully
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    // alter child behaviour to do what we want
    m_FindNumBins = true;
    return super.setInputFormat(instanceInfo);
  }

  /**
   * Finds the number of bins to use and creates the cut points.
   *
   * @param index the attribute index
   */
 
  protected void findNumBins(int index) {

    Instances toFilter = getInputFormat();

    // Find number of instances for attribute where not missing
    int numOfInstances = toFilter.numInstances();
    for (int i = 0; i < toFilter.numInstances(); i++) {
      if (toFilter.instance(i).isMissing(index))
    numOfInstances--;
    }

    m_NumBins = (int)(Math.pow(numOfInstances, power));
    System.out.println("Actual num instances: " + numOfInstances);
    if (m_NumBins > 0) {
        
      calculateCutPointsByEqualFrequencyBinning(index);
    }
  }

  /**
   * Gets an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(7);

    newVector.addElement(new Option(
              "\tSpecifies list of columns to Discretize. First"
          + " and last are valid indexes.\n"
          + "\t(default: first-last)",
              "R", 1, "-R <col1,col2-col4,...>"));

    newVector.addElement(new Option(
              "\tInvert matching sense of column indexes.",
              "V", 0, "-V"));

    newVector.addElement(new Option(
              "\tOutput binary attributes for discretized attributes.",
              "D", 0, "-D"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -R col1,col2-col4,... <br>
   * Specifies list of columns to Discretize. First
   * and last are valid indexes. (default none) <p>
   *
   * -V <br>
   * Invert matching sense.<p>
   *
   * -D <br>
   * Make binary nominal attributes. <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setMakeBinary(Utils.getFlag('D', options));
    setInvertSelection(Utils.getFlag('V', options));
    
    String convertList = Utils.getOption('R', options);
    if (convertList.length() != 0) {
      setAttributeIndices(convertList);
    } else {
      setAttributeIndices("first-last");
    }

    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }
  }
  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [12];
    int current = 0;

    if (getMakeBinary()) {
      options[current++] = "-D";
    }
    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    if (!getAttributeIndices().equals("")) {
      options[current++] = "-R"; options[current++] = getAttributeIndices();
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Discretizes numeric attributes using equal frequency binning,"
      + " where the number of bins is equal to the square root of the"
      + " number of non-missing values.";
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String findNumBinsTipText() {

    return "Ignored.";
  }

  /**
   * Get the value of FindNumBins.
   *
   * @return Value of FindNumBins.
   */
  public boolean getFindNumBins() {
    
    return false;
  }
  
  /**
   * Set the value of FindNumBins.
   *
   * @param newFindNumBins Value to assign to FindNumBins.
   */
  public void setFindNumBins(boolean newFindNumBins) {
    
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useEqualFrequencyTipText() {

    return "Always true.";
  }

  /**
   * Get the value of UseEqualFrequency.
   *
   * @return Value of UseEqualFrequency.
   */
  public boolean getUseEqualFrequency() {
    
    return true;
  }
  
  /**
   * Set the value of UseEqualFrequency.
   *
   * @param newUseEqualFrequency Value to assign to UseEqualFrequency.
   */
  public void setUseEqualFrequency(boolean newUseEqualFrequency) {
    
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String binsTipText() {

    return "Ignored.";
  }

  /**
   * Ignored
   *
   * @return the number of bins.
   */
  public int getBins() {

    return 0;
  }

  /**
   * Ignored
   *
   * @param numBins the number of bins
   */
  public void setBins(int numBins) {
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
    Filter.batchFilterFile(new MyDiscretize(), argv);
      } else {
    Filter.filterFile(new MyDiscretize(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}

