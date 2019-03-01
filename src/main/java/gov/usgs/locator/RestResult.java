package gov.usgs.locator;

/**
 * Save the results of one pass through the R-estimator algorithm.
 * 
 * @author Ray Buland
 *
 */
public class RestResult {
	double stepLen;			// Step length in kilometers
	double median;			// Median residual in seconds
	double spread;			// Residual spread in seconds
	double chiSq;				// Dispersion in seconds
	
	/**
	 * Remember the results of the R-estimator algorithm.
	 * 
	 * @param stepLen Step length in kilometers
	 * @param median Median residual in seconds
	 * @param spread Residual spread in seconds
	 * @param penalty Dispersion in seconds
	 */
	public RestResult(double stepLen, double median, double spread, 
			double penalty) {
		this.stepLen = stepLen;
		this.median = median;
		this.spread = spread;
		this.chiSq = penalty;
	}
}
