package gov.usgs.locator;

/**
 * This weighted residuals storage combines the residuals and weights 
 * for picks and the Bayesian depth, while providing storage for the 
 * dispersion calculation and the spatial derivatives.  Note that the 
 * residuals and weights are kept separate as some calculations depend 
 * only on the residuals.
 * 
 * @author Ray Buland
 *
 */
public class Wresidual {
	boolean isDepth;		// True if this is the Bayesian depth residual
	int sortIndex;			// Index of the sorted residual
	double residual;		// Residual in seconds for picks or kilometers for depth
	double estResidual;	// Linearly estimated residual
	double weight;			// Weight
	double[] deriv;			// Spatial derivatives in kilometers
	
	/**
	 * Initialize the weighted residual with minimal information.
	 * 
	 * @param isDepth True if this is the Bayesian depth residual
	 * @param residual Residual in seconds for picks and in kilometers 
	 * for depth
	 * @param weight Weight
	 */
	public Wresidual(boolean isDepth, int sortIndex, double residual, 
			double weight) {
		this.isDepth = isDepth;
		this.sortIndex = sortIndex;
		this.residual = residual;
		this.weight = weight;
		sortIndex = -1;
		deriv = null;
	}
	
	/**
	 * Add the spatial derivatives to the weighted residual.
	 * 
	 * @param dTdLat Derivative of travel time with respect to latitude 
	 * in seconds/kilometers
	 * @param dTdLon Derivative of travel time with respect to longitude 
	 * in seconds/kilometers
	 * @param dTdDepth Derivative of travel time with respect to depth 
	 * in seconds/kilometers
	 */
	public void addDeriv(double dTdLat, double dTdLon, double dTdDepth) {
		deriv = new double[3];
		deriv[0] = dTdLat;
		deriv[1] = dTdLon;
		deriv[2] = dTdDepth;
	}
	
	/**
	 * Update the estimated residual given a trial step vector.
	 * 
	 * @param trialVector Distance and direction from the current hypocenter 
	 * in kilometers
	 */
	public void updateEst(double[] trialVector) {
		estResidual = residual;
		for(int j=0; j<trialVector.length; j++) {
			estResidual -= trialVector[j]*deriv[j];
		}
	}
	
	/**
	 * Get the index of the sorted residual.
	 * 
	 * @return Sorted index.
	 */
	public int getSortIndex() {
		return sortIndex;
	}
}
