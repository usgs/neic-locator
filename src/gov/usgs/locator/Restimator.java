package gov.usgs.locator;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Rank-sum estimator implementation for the NEIC Locator.
 * 
 * @author Ray Buland
 *
 */
public class Restimator {
	int nLast = -1, medIndex1 = -1, medIndex2 = -1, length = -1, half = -1;
	double median = 0d, spread = 0d, estMedian = 0d;
	double[] scores;
	ArrayList<Wresidual> wResiduals;
	SortData[] sortData = null;
	
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
		/*
		 * Note that sortData is dimensioned at least one larger than needed 
		 * for pick residuals alone in the median and spread methods.  The 
		 * extra space will be needed for computing the dispersion.
		 */
		if(sortData == null) sortData = new SortData[wResiduals.size()];
		else if(sortData.length < wResiduals.size()) 
			sortData = new SortData[wResiduals.size()];
		
		// Set up for the median.
		length = 0;
		for(int j=0; j<wResiduals.size(); j++) {
			if(!wResiduals.get(j).isDepth) {
				sortData[length] = new SortData(length, wResiduals.get(j).residual);
				length++;
			}
		}
		// Sort the travel-time residuals and their indices.
		Arrays.sort(sortData, 0, length);
		
		// Do the median.
		half = length/2;
		if(length%2 == 0) {
			median = 0.5d*(sortData[half-1].value+sortData[half].value);
			medIndex1 = sortData[half-1].index;
			medIndex2 = sortData[half].index;
			return median;
		}
		else {
			median = sortData[half].value;
			medIndex1 = sortData[half].index;
			medIndex2 = -1;
			return median;
		}
	}
	
	/**
	 * Compute the normalized median absolute deviation or spread of the 
	 * travel-time residuals (excluding the Bayesian depth residual).  Note 
	 * that the median method must be invoked before spread.
	 * 
	 * @return The spread of the travel-time residuals
	 */
	public double spread() {
		// Trap insufficient data.
		if(wResiduals.size() < 2) return 0d;
		
		// Set up for the spread.
		for(int j=0; j<length; j++) {
			sortData[j].value = Math.abs(sortData[j].value-median);
		}
		Arrays.sort(sortData, 0, length);
		
		// Do the median of the demeaned absolute residuals.
		if(length%2 == 0) {
			spread = LocUtil.MADNORM*0.5d*(sortData[half-1].value+
					sortData[half].value);
			return spread;
		}
		else {
			spread = LocUtil.MADNORM*sortData[half].value;
			return spread;
		}
	}
	
	/**
	 * Remove the median from the residuals.
	 */
	public void deMedianRes() {
		for(int j=0; j<wResiduals.size(); j++) {
			if(!wResiduals.get(j).isDepth) {
				wResiduals.get(j).residual -= median;
			}
		}
		// Since we've already demedianed, we don't need to do it again 
		// for the penalty function.
		median = 0d;
	}
	
	/**
	 * Compute the R-estimator penalty function or dispersion.  Note that 
	 * the median method must be invoked before penalty in order to 
	 * compute the median and allocate the sort storage.
	 * 
	 * @param tag String printed as part of the debug output
	 * @return the R-estimator penalty function
	 */
	public double penalty() {
		// Trap insufficient data.
		if(wResiduals.size() < 2) return 0d;
		
		// Set up the penalty.
		for(int j=0; j<wResiduals.size(); j++) {
			// This time keep the Bayesian depth constraint, but, of course, 
			// don't remove the residual median.
			if(!wResiduals.get(j).isDepth) {
				sortData[j] = new SortData(j, (wResiduals.get(j).residual-median)*
						wResiduals.get(j).weight);
			} else {
				sortData[j] = new SortData(j, wResiduals.get(j).residual*
						wResiduals.get(j).weight);
			}
		}
		Arrays.sort(sortData, 0, wResiduals.size());
		// Compute the penalty function.
		double penalty = dispersion();
		// Put the sorted indices back into R-estimator storage.
		for(int j=0; j<wResiduals.size(); j++) {
			wResiduals.get(j).sortIndex = sortData[j].index;
		}
		return penalty;
	}
	
	/**
	 * Remove the medians from each row of the design matrix.  Note that 
	 * the positions of the median values corresponds to the positions 
	 * for the residual, not their design values.  Obviously, the median 
	 * method must be invoked before deMean to determine these positions.
	 * 
	 * @param d Design matrix
	 */
	public void deMedianDesign() {
		double[] dMed = new double[3];
		
		// Set up the medians.
		if(length%2 == 0) {
			for(int i=0; i<dMed.length; i++) {
				dMed[i] = 0.5d*(wResiduals.get(medIndex1).deriv[i]+
						wResiduals.get(medIndex2).deriv[i]);
			}
		} else {
			for(int i=0; i<dMed.length; i++) {
				dMed[i] = wResiduals.get(medIndex1).deriv[i];
			}
		}
		// Remove the median values from the matrix.
		for(int j=0; j<wResiduals.size(); j++) {
			if(!wResiduals.get(j).isDepth) {
				wResiduals.get(j).deMedianDeriv(dMed);
			}
		}
	}
	
	/**
	 * Compute the steepest descents direction.
	 * 
	 * @param n Number of degrees of freedom (n = 2 for fixed depth, n=3 
	 * for free depth)
	 * @return The steepest descents direction unit vector
	 */
	public double[] steepest(int n) {
		double[] stepDir;
		Wresidual wResidual;
		
		// Interpolate the scores.
		if(sortData.length != nLast) {
			nLast = sortData.length;
			makeScores(sortData.length);
		}
		// Initialize the step length.
		stepDir = new double[n];
		for(int j=0; j<n; j++) {
			stepDir[j] = 0d;
		}
		// The step direction is the sum of weighted, demedianed derivatives.  
		// We have to process the residuals in sort order for the scores to 
		// be right.
//	System.out.println("\nAdder:");
		for(int j=0; j<sortData.length; j++) {
			wResidual = wResiduals.get(sortData[j].index);
//		wResidual.printWres(true);
			for(int i=0; i<n; i++) {
				stepDir[i] += scores[j]*wResidual.weight*wResidual.deDeriv[i];
			}
//		System.out.format("\t\tStep: %8.4f %8.4f %8.4f\n", stepDir[0], 
//				stepDir[1], stepDir[2]);
		}
		return LocUtil.unitVector(stepDir);
	}
	
	/**
	 * Compute the median of the linear estimates of the travel-time 
	 * residuals (excluding the Bayesian depth residual).
	 * 
	 * @returnMedian of estimated travel-time residuals
	 */
	public double estMedian() {
		// Make sure we have enough data to do something.
		if(wResiduals.size() < 2) {
			// If there's no data or only a depth constraint return zero.
			if(wResiduals.size() == 0) return 0d;
			else if(wResiduals.size() == 1 && wResiduals.get(0).isDepth) 
				return 0d;
		}
		/*
		 * Note that sortData is dimensioned at least one larger than needed 
		 * for pick residuals alone in the median and spread methods.  The 
		 * extra space will be needed for computing the dispersion.
		 */
		if(sortData == null) sortData = new SortData[wResiduals.size()];
		else if(sortData.length < wResiduals.size()) 
			sortData = new SortData[wResiduals.size()];
		
		// Set up for the median.
		length = 0;
		for(int j=0; j<wResiduals.size(); j++) {
			if(!wResiduals.get(j).isDepth) {
				sortData[length] = new SortData(length, wResiduals.get(j).estResidual);
				length++;
			}
		}
		// Sort the travel-time residuals and their indices.
		Arrays.sort(sortData, 0, length);
		
		// Do the median.
		half = length/2;
		if(length%2 == 0) {
			estMedian = 0.5d*(sortData[half-1].value+sortData[half].value);
			return estMedian;
		}
		else {
			estMedian = sortData[half].value;
			return estMedian;
		}
	}
	
	/**
	 * Remove the median from the estimated residuals.
	 */
	public void deMedianEstRes() {
		for(int j=0; j<wResiduals.size(); j++) {
			if(!wResiduals.get(j).isDepth) {
				wResiduals.get(j).estResidual -= estMedian;
			}
		}
		// Since we've already demedianed, we don't need to do it again 
		// for the penalty function.
		estMedian = 0d;
	}
	
	/**
	 * Compute the R-estimator penalty function or dispersion.  Note that 
	 * the median method must be invoked before penalty in order to 
	 * compute the median and allocate the sort storage.
	 * 
	 * @param tag String printed as part of the debug output
	 * @return the R-estimator penalty function
	 */
	public double estPenalty() {
		// Trap insufficient data.
		if(wResiduals.size() < 2) return 0d;
		
		// Set up the penalty.
		for(int j=0; j<wResiduals.size(); j++) {
			// This time keep the Bayesian depth constraint, but, of course, 
			// don't remove the residual median.
			if(!wResiduals.get(j).isDepth) {
				sortData[j] = new SortData(j, (wResiduals.get(j).estResidual-estMedian)*
						wResiduals.get(j).weight);
			} else {
				sortData[j] = new SortData(j, wResiduals.get(j).estResidual*
						wResiduals.get(j).weight);
			}
		}
		Arrays.sort(sortData, 0, wResiduals.size());
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
		if(sortData.length != nLast) {
			nLast = sortData.length;
			makeScores(sortData.length);
		}
		// The dispersion is just a dot product.
		for(int j=0; j<sortData.length; j++) {
			disp += scores[j]*sortData[j].value;
		}
		return disp;
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
		
/*	System.out.println("\nScores:");
		for(int j=0; j<nData; j++) {
			System.out.format("\t%3d %6.4f\n", j, scores[j]);
		} */
	}
}