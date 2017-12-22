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
	double median = 0d, spread = 0d;
	double[] scores;
	ArrayList<Wresidual> wRes;
	SortData[] sortData = null;
	
	/**
	 * Remember the weighted residual storage.
	 */
	public Restimator(ArrayList<Wresidual> wResiduals) {
		this.wRes = wResiduals;
	}
	
	/**
	 * Compute the median of travel-time residuals (excluding the 
	 * Bayesian depth residual).
	 * 
	 * @param indBayes Index of the Bayesian depth residual
	 * @return Median of travel-time residuals
	 */
	public double median() {
		// Make sure we have enough data to do something.
		if(wRes.size() < 2) {
			// If there's no data or only a depth constraint return zero.
			if(wRes.size() == 0) return 0d;
			else if(wRes.size() == 1 && wRes.get(0).isDepth) 
				return 0d;
		}
		/*
		 * Note that sortData is dimensioned at least one larger than needed 
		 * for pick residuals alone in the median and spread methods.  The 
		 * extra space will be needed for computing the dispersion.
		 */
		if(sortData == null) sortData = new SortData[wRes.size()];
		else if(sortData.length < wRes.size()) 
			sortData = new SortData[wRes.size()];
		
		// Set up for the median.
		length = 0;
		for(int j=0; j<wRes.size(); j++) {
			if(!wRes.get(j).isDepth) {
				sortData[length] = new SortData(length, wRes.get(j).residual);
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
		if(wRes.size() < 2) return 0d;
		
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
	 * Remove the medians from each row of the design matrix.  Note that 
	 * the positions of the median values corresponds to the positions 
	 * for the residual, not their design values.  Obviously, the median 
	 * method must be invoked before deMean to determine these positions.
	 * 
	 * @param d Design matrix
	 */
	public void deMedian() {
		double[] dMed = new double[3];
		
		// Set up the medians.
		if(length%2 == 0) {
			for(int i=0; i<dMed.length; i++) {
				dMed[i] = 0.5d*(wRes.get(medIndex1).deriv[i]+
						wRes.get(medIndex2).deriv[i]);
			}
		} else {
			for(int i=0; i<dMed.length; i++) {
				dMed[i] = wRes.get(medIndex1).deriv[i];
			}
		}
		// Remove the median values from the matrix.
		for(int j=0; j<wRes.size(); j++) {
			if(!wRes.get(j).isDepth) {
				for(int i=0; i<dMed.length; i++) {
					wRes.get(j).deriv[i] -= dMed[i];
				}
			}
		}
	}
	
	/**
	 * Compute the R-estimator penalty function or dispersion.  Note that 
	 * the median method must be invoked before penalty in order to 
	 * compute the median and allocate the sort storage.
	 * 
	 * @return the R-estimator penalty function
	 */
	public double penalty(String tag) {
		// Trap insufficient data.
		if(wRes.size() < 2) return 0d;
		
		// Set up the penalty.
		for(int j=0; j<wRes.size(); j++) {
			// This time keep the Bayesian depth constraint, but, of course, 
			// don't remove the residual median.
			if(!wRes.get(j).isDepth) {
				sortData[j].value = (wRes.get(j).residual-median)*
						wRes.get(j).weight;
			} else {
				sortData[j].value = wRes.get(j).residual*
						wRes.get(j).weight;
			}
			sortData[j].index = j;
		}
		Arrays.sort(sortData, 0, wRes.size());
		// Compute the penalty function.
		double penalty = dispersion();
		// Put the sorted indices back into R-estimator storage.
		for(int j=0; j<wRes.size(); j++) {
			wRes.get(j).sortIndex = sortData[j].index;
		}
		if(LocUtil.deBugLevel > 0 && !tag.equals("EL")) 
			System.out.format("Lsrt: %s av sd chisq = ", tag, median, spread, 
					penalty);
		return penalty;
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
			while(p >scoreGenP[k]) {
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
			scores[j] = 0.5d*(scores[j]+scores[nData+1-j]);
			scores[nData+1-j] = scores[j];
		}
		if(nData%2 > 0) scores[nData/2+1] = 0d;
	}
}