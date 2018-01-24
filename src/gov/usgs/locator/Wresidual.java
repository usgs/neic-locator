package gov.usgs.locator;

import java.util.Arrays;

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
	double[] deDeriv;		// Demedianed spatial derivatives in kilometers
	
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
		// Initialize the demedianed derivatives.
		deDeriv = Arrays.copyOf(deriv, deriv.length);
	}
	
	/**
	 * Remove the median from the derivatives.  Note that this isn't the 
	 * median of the derivatives, but the derivative corresponding to the 
	 * median of the residuals.
	 * 
	 * @param medians Array of derivative medians
	 */
	public void deMedianDeriv(double[] medians) {
		for(int j=0; j<medians.length; j++) {
			deDeriv[j] = deriv[j]-medians[j];
		}
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
	
	/**
	 * Print the contents of the weighted residuals.
	 * 
	 * @param full If true, print the derivatives as well
	 */
	public void printWres(boolean full) {
		if(!full || deriv == null) {
			System.out.format("\t%4d Res: %7.2f %7.2f Wt: %7.4f %b\n", sortIndex, 
					residual, estResidual, weight, isDepth);
		} else {
			System.out.format("\t%4d Res: %7.2f %7.2f Wt: %7.4f deriv: %10.3e "+
					"%10.3e %10.3e %b\n", sortIndex, residual, estResidual, weight, 
					deriv[0], deriv[1], deriv[2], isDepth);
		}
	}
}
