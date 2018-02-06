package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Rank-sum estimator implementation for the NEIC Locator.
 * 
 * @author Ray Buland
 *
 */
public class Restimator {
	int nLast = -1, length = -1, half = -1;
	double median = 0d, estMedian = 0d;
	double[] scores;
	ArrayList<Wresidual> wResiduals;
	
	/**
	 * Remember the weighted residual storage.
	 */
	public Restimator(ArrayList<Wresidual> wResiduals) {
		this.wResiduals = wResiduals;
	}
	
	/**
	 * Compute the median of the travel-time residuals (excluding the 
	 * Bayesian depth residual, of course).
	 * 
	 * @return Median of travel-time residuals
	 */
	public double median() {
		// Make sure we have enough data to do something.
		if(wResiduals.size() < 2) {
			// If there's no data or only a depth constraint return zero.
			if(wResiduals.size() == 0) return 0d;
			else if(wResiduals.size() == 1 && wResiduals.get(0).isDepth) 
				return 0d;
		}
		
		// Set up for the median.
		for(int j=0; j<wResiduals.size(); j++) {
				wResiduals.get(j).sortRes();
		}
		// Sort the travel-time residuals.
		wResiduals.sort(null);
		
		// Do the median.
		length = wResiduals.size()-1;
		half = length/2;
		if(length%2 == 0) {
			median = 0.5d*(wResiduals.get(half-1).residual+
					wResiduals.get(half).residual);
			return median;
		}
		else {
			median = wResiduals.get(half).residual;
			return median;
		}
	}
	
	/**
	 * Compute the normalized median absolute deviation or spread of the 
	 * travel-time residuals (excluding the Bayesian depth residual).  Note 
	 * that the median is remembered from the median method.
	 * 
	 * @return The spread of the travel-time residuals
	 */
	public double spread() {
		// Trap insufficient data.
		if(wResiduals.size() < 2) return 0d;
		
		// Set up for the spread.
		for(int j=0; j<length; j++) {
			wResiduals.get(j).sortSpread(median);
		}
		wResiduals.sort(null);
		
		// Do the median of the demeaned absolute residuals.
		length = wResiduals.size()-1;
		half = length/2;
		if(length%2 == 0) {
			return LocUtil.MADNORM*0.5d*(wResiduals.get(half-1).sortValue+
					wResiduals.get(half).sortValue);
		}
		else {
			return LocUtil.MADNORM*wResiduals.get(half).sortValue;
		}
	}
	
	/**
	 * Remove the median from the residuals.  Note that the median is 
	 * remembered from the median method.
	 */
	public void deMedianRes() {
		for(int j=0; j<wResiduals.size(); j++) {
			wResiduals.get(j).deMedianRes(median);
		}
		// Since we've already demedianed, we don't need to do it again 
		// for the penalty function.
		median = 0d;
	}
	
	/**
	 * Remove the medians from each row of the design matrix.  Note that 
	 * the positions of the median values corresponds to the positions 
	 * for the residual, not their design values.  Obviously, the median 
	 * method must be invoked before deMedianDesign to determine these 
	 * positions and no other method that re-sorts the residuals (i.e., 
	 * spread and penalty) may be invoked in between.
	 */
	public void deMedianDesign() {
		double[] dMed = new double[3];
		
		// Set up the medians.
		if(length%2 == 0) {
			for(int i=0; i<dMed.length; i++) {
				dMed[i] = 0.5d*(wResiduals.get(half-1).deriv[i]+
						wResiduals.get(half).deriv[i]);
			}
		} else {
			for(int i=0; i<dMed.length; i++) {
				dMed[i] = wResiduals.get(half
						).deriv[i];
			}
		}
		// Remove the median values from the matrix.
		for(int j=0; j<wResiduals.size(); j++) {
			wResiduals.get(j).deMedianDeriv(dMed);
		}
	}
	
	/**
	 * Compute the R-estimator penalty function or dispersion.  Note that 
	 * the median is remembered from the median method.
	 * 
	 * @return the R-estimator penalty function
	 */
	public double penalty() {
		// Trap insufficient data.
		if(wResiduals.size() < 2) return 0d;
		
		// Set up the penalty.
		for(int j=0; j<wResiduals.size(); j++) {
				wResiduals.get(j).sortDisp(median);
		}
		wResiduals.sort(null);
		// Compute the penalty function.
		return dispersion();
	}
	
	/**
	 * Compute the dispersion of the current set of weighted residuals.  
	 * The dispersion is a convenient summary of the current data fit.
	 * 
	 * @return Dispersion
	 */
	private double dispersion() {
		double disp = 0d;
		
		// Interpolate the scores.
		if(wResiduals.size() != nLast) {
			nLast = wResiduals.size();
			makeScores(nLast);
		}
		// The dispersion is just a dot product.
		for(int j=0; j<nLast; j++) {
			disp += scores[j]*wResiduals.get(j).sortValue;
		}
		return disp;
	}
	
	/**
	 * Compute the steepest descents direction.  This depends on the 
	 * wResiduals sort order created the penalty method, so median and 
	 * spread methods should not be invoked between.
	 * 
	 * @param n Number of degrees of freedom (n = 2 for fixed depth, n=3 
	 * for free depth)
	 * @return The steepest descents direction unit vector
	 */
	public double[] steepest(int n) {
		double[] stepDir;
		Wresidual wResidual;
		
		// Initialize the step length.
		stepDir = new double[n];
		for(int j=0; j<n; j++) {
			stepDir[j] = 0d;
		}
		/*
		 * The step direction is the sum of weighted, demedianed derivatives.  
		 * We have to process the weighted residuals in sort order for the 
		 * scores to get the right direction.
		 */
		for(int j=0; j<wResiduals.size(); j++) {
			wResidual = wResiduals.get(j);
			for(int i=0; i<n; i++) {
				stepDir[i] += scores[j]*wResidual.weight*wResidual.deDeriv[i];
			}
		}
		return LocUtil.unitVector(stepDir);
	}
	
	/**
	 * Compute the median of the linear estimates of the travel-time 
	 * residuals (excluding the Bayesian depth residual).
	 * 
	 * @return Median of estimated travel-time residuals
	 */
	public double estMedian() {
		// Make sure we have enough data to do something.
		if(wResiduals.size() < 2) {
			// If there's no data or only a depth constraint return zero.
			if(wResiduals.size() == 0) return 0d;
			else if(wResiduals.size() == 1 && wResiduals.get(0).isDepth) 
				return 0d;
		}
		
		// Set up for the median.
		length = 0;
		for(int j=0; j<wResiduals.size(); j++) {
				wResiduals.get(j).sortEst();
		}
		// Sort the estimated travel-time residuals.
		wResiduals.sort(null);
		
		// Do the median.
		length = wResiduals.size()-1;
		half = length/2;
		if(length%2 == 0) {
			estMedian = 0.5d*(wResiduals.get(half-1).estResidual+
					wResiduals.get(half).estResidual);
			return estMedian;
		}
		else {
			estMedian = wResiduals.get(half).estResidual;
			return estMedian;
		}
	}
	
	/**
	 * Remove the median from the estimated residuals.
	 */
	public void deMedianEstRes() {
		for(int j=0; j<wResiduals.size(); j++) {
			wResiduals.get(j).deMedianEst(estMedian);
		}
		// Since we've already demedianed, we don't need to do it again 
		// for the penalty function.
		estMedian = 0d;
	}
	
	/**
	 * Compute the R-estimator penalty function or dispersion.  Note that 
	 * the median is remembered from the estMedian method.
	 * 
	 * @return the R-estimator penalty function
	 */
	public double estPenalty() {
		// Trap insufficient data.
		if(wResiduals.size() < 2) return 0d;
		
		// Set up the penalty.
		for(int j=0; j<wResiduals.size(); j++) {
				wResiduals.get(j).sortEstDisp(estMedian);
		}
		wResiduals.sort(null);
		// Compute the penalty function.
		return dispersion();
	}

	/**
	 * Interpolate the score generating function onto the data available.
	 * 
	 * @param nData Number of data we need scores for
	 */
	private void makeScores(int nData) {
		/*
		 * Piecewise linear interpolation of optimal non-decreasing score 
		 * generating function created by Steve Heustis using a simplex 
		 * algorithm.  Note that the theoretically optimal weights depend 
		 * only on the travel-time residual distribution.  However, these turn 
		 * out to be non-monotonic which, in practice, cause convergence 
		 * problems.
		 */
		final double[] scoreGenP = {0d, 0.1375d ,0.1625d, 0.1875d, 0.2125d, 
				0.2375d, 0.2625d, 0.2875d, 0.3125d, 0.3375d, 0.3625d, 0.3875d, 
				0.4125d, 0.4375d, 0.4625d, 0.4875d, 0.5125d, 0.5375d, 0.5625d, 
				0.5875d, 0.6125d, 0.6375d, 0.6625d, 0.6875d, 0.7125d, 0.7375d, 
				0.7625d, 0.7875d, 1d};
		final double[] scoreGenF = {0.0775d, 0.0775d, 0.1546d, 0.5328d, 0.8679d, 
				1.1714d, 1.4542d, 1.7266d, 1.9987d, 2.2802d, 2.5803d, 2.9068d, 
				3.2657d, 3.6603d, 4.0912d, 4.5554d, 5.0470d, 5.5572d, 6.0754d, 
				6.5906d, 7.0919d, 7.5702d, 8.0194d, 8.4365d, 8.8223d, 9.1812d, 
				9.5207d, 9.5974d, 9.5974d};
		int k;
		double dp, p, ave;
		
		// Set up the interpolation.
		nLast = nData;
		p = 0d;
		dp = 1d/(nData+1);
		ave = 0d;
		k = 1;
		// Create an array to store the scores.
		scores = new double[nData];
		
		// Do the interpolation
		for(int j=0; j<nData; j++) {
			p += dp;
			while(p > scoreGenP[k]) {
				k++;
			}
			scores[j] = (p-scoreGenP[k-1])*(scoreGenF[k]-scoreGenF[k-1])/
					(scoreGenP[k]-scoreGenP[k-1]) + scoreGenF[k-1];
			ave += scores[j];
		}
		
		// Give the scores a zero mean.
		ave /= nData;
		for(int j=0; j<nData; j++) {
			scores[j] -= ave;
		}
		
		/*
		 * The optimal non-decreasing scores, like the travel-time residual 
		 * distribution, turn out to be asymmetric.  Unfortunately, this causes 
		 * problems for the projected decorrelated data.  Forcing the scores 
		 * to be symmetric is a quick and dirty fix for this vexed problem.
		 */
		for(int j=0; j<nData/2; j++) {
			scores[j] = 0.5d*(scores[j]-scores[nData-j-1]);
			scores[nData-j-1] = -scores[j];
		}
		if(nData%2 > 0) scores[nData/2] = 0d;
	}
}