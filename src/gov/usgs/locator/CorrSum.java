package gov.usgs.locator;

/**
 * Support the triage algorithm by holding the sum of 
 * correlations among picks for one pick in a sortable 
 * way.
 * 
 * @author Ray Buland
 *
 */
public class CorrSum implements Comparable<CorrSum> {
	int row;					// Index of the row in the covariance matrix
	double sum;				// Sum of correlations in the row
	double sort;			// Variable to sort on
	
	/**
	 * Set up this correlation sum element.
	 * 
	 * @param row Row of the covariance matrix
	 * @param sum Sum of correlations in the row
	 */
	public CorrSum(int row, double sum) {
		this.row = row;
		this.sum = sum;
		sort = sum;
	}
	
	/**
	 * Decrement the correlation sum.
	 * 
	 * @param dec Decrement value
	 */
	public void decSum(double dec) {
		sum -= dec;
		sort = sum;
	}
	
	/**
	 * Change the sort variable so that we can sort back into 
	 * the original order.
	 */
	public void rowSort() {
		sort = (double)row;
	}
	
	/**
	 * Print the contents of this correlation sum element.
	 */
	@Override
	public String toString() {
		return String.format("%4d %7.2f", row, sum);
	}

	/**
	 * Sort on the sort variable.
	 */
	@Override
	public int compareTo(CorrSum corrSum) {
		if(sort > corrSum.sort) return +1;
		else if(sort < corrSum.sort) return -1;
		else return 0;
	}
}
