package gov.usgs.locator;

/**
 * The CorrSum class supports the triage algorithm by holding the sum of 
 * correlations among picks for one pick in a sortable way.
 * 
 * @author Ray Buland
 *
 */
public class CorrSum implements Comparable<CorrSum> {
  /**
   * A int double containing the row index in the covariance matrix.
   */
  private int rowIndex; 

  /** 
   * A double value containing the correlation sum for the row identified by
   * rowIndex.
   */
  private double correlationSum; 

  /** 
   * A double value containing the index used when sorting.
   */
  private double sortIndex; 

  /**
   * The CorrSum constructor. This constructor initializes the internal varibles
   * to the provided values. Note that sortIndex is initialized to be equal to
   * sum.
   * 
   * @param row An integer containing the row index of the covariance matrix
   * @param sum A double value containing the sum of correlations in the row
   */
  public CorrSum(int row, double sum) {
    this.rowIndex = row;
    this.correlationSum = sum;
    this.sortIndex = sum;
  }
  
  /**
   * This function decrements the correlation sum amd sort index.
   * 
   * @param decrementValue Decrement value
   */
  public void decSum(double decrementValue) {
    correlationSum -= decrementValue;
    sortIndex = correlationSum;
  }
  
  /**
   * This function changes the sortIndex variable to the row index so that the 
   * sort can be restored to the original order.
   */
  public void rowSort() {
    sortIndex = (double)rowIndex;
  }
  
  /**
   * This function prints the contents of this CorrSum object.
   * @return A String containing the contents of this CorrSum object.
   */
  @Override
  public String toString() {
    return String.format("%4d %7.2f", rowIndex, correlationSum);
  }

  /**
   * This function compares this CorrSum object sortIndex varible to the  
   * provided CorrSum object sortIndex varible.
   * 
   * @param corrSum A CorrSum object to compare to
   * @return +1 if this CorrSum object sortIndex varible is greater than the 
   *         provided CorrSum object sortIndex varible; -1 if this CorrSum  
   *         object sortIndex varible is less than the provided CorrSum object 
   *         sortIndex varible; and 0 if  this CorrSum object sortIndex varible
   *         is equal to the provided CorrSum object sortIndex varible;
   */
  @Override
  public int compareTo(CorrSum corrSum) {
    if (sortIndex > corrSum.sortIndex) {
      return +1;
    } else if (sortIndex < corrSum.sortIndex) {
      return -1;
    } else {
      return 0;
    }
  }

  /**
   * Function to return the row index.
   **/ 
  public int getRowIndex() {
    return rowIndex;
  }
}
