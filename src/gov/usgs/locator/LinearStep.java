package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Computing seismic travel times is expensive, so instead set the 
 * steepest descent step length using the linear term of a Taylor 
 * series.  The residual estimates and hence the estimated penalty 
 * function value will be accurate as long as the curvature of the 
 * Earth is negligible within the step length (and, of course, the 
 * seismic phases still exist at the new epicentral distance and 
 * hypocentral depth).
 * 
 * @author Ray Buland
 *
 */
public class LinearStep {
	double[] stepDir, trialVector;
	Event event;
	Restimator rEst;
	Hypocenter hypo;
	ArrayList<Wresidual> wRes;
	
	/**
	 * Remember the event.
	 * 
	 * @param event Event information
	 */
	public LinearStep(Event event) {
		this.event = event;
		hypo = event.getHypo();
		wRes = event.wResiduals;
		rEst = event.rEst;
	}
	
	/**
	 * Determine the step length in the steepest descents step 
	 * direction that results in the minimum dispersion based on 
	 * linear estimates of the travel-time residuals.  This 
	 * algorithm is a messy bisection.
	 * 
	 * @param stepDir Spatial steepest descents direction in 
	 * kilometers
	 * @param startLen Starting step length in kilometers
	 * @param stepMin If the step length is less than the minimum 
	 * the current estimate is good enough
	 * @param stepMax If the final step length is greater than the 
	 * maximum, give up
	 * @param curPenalty Current R-estimator dispersion
	 * @return The step length in kilometers needed to reach the 
	 * minimum dispersion
	 * @throws Exception If the bisection doesn't make sense
	 */
	public RestResult stepLength(double[] stepDir, double startLen, 
			double stepMin, double stepMax, double curPenalty) 
			throws Exception {
		double initLen, stepLen;
		RestResult testSample;
		RestResult[] sample = new RestResult[3]; 
		
		if(LocUtil.deBugLevel > 0) System.out.println();
		
		// The trial vector has to be the same size as the step vector.
		trialVector = new double[stepDir.length];
		
		// Initialize the bisection.
		this.stepDir = stepDir; 
		initLen = startLen;
		stepLen = startLen;
		sample[0] = new RestResult(0d, 0d, 0d, curPenalty);
		// Make a default step.
		sample[1] = estPenalty(stepLen);
		
		// Do some preliminary hunting to surround the minimum.
		if(sample[0].chiSq >= sample[1].chiSq) {
			// If the trial step was too short, keep doubling it.
			stepLen = 2d*stepLen;
			sample[2] = estPenalty(stepLen);
			// Keep stepping until the penalty gets bigger.
			while(sample[1].chiSq >= sample[2].chiSq) {
				// If we've gone too far, return what we've got.
				if(sample[2].stepLen >= stepMax) {
					trialStep(sample[2].stepLen);
					if(LocUtil.deBugLevel > 0) System.out.format("Lintry: x dsp = "+
							"%6.2f %9.4f %5.2f\n", sample[2].stepLen, sample[2].chiSq, 
							sample[2].median);
					return sample[2];
				}
				sample[0] = sample[1];
				sample[1] = sample[2];
				// The steps keep getting bigger.
				initLen *= 2d;
				stepLen = Math.min(sample[1].stepLen+initLen, stepMax);
				sample[2] = estPenalty(stepLen);
			}
		} else {
			// If trial step was too long, find a lower limit.
			do {
				sample[2] = sample[1];
				stepLen = 0.5*(sample[0].stepLen+sample[2].stepLen);
				sample[1] = estPenalty(stepLen);
				// See if we've converged.
				if(sample[1].stepLen <= stepMin) {
					if(sample[1].chiSq >= sample[0].chiSq) {
						sample[1] = new RestResult(0d, 0d, 0d, curPenalty);
					}
					trialStep(sample[1].stepLen);
					if(LocUtil.deBugLevel > 0) System.out.format("Lintry: x dsp = "+
							"%7.3f %9.4f %5.2f\n", sample[1].stepLen, sample[1].chiSq, 
							sample[1].median);
					return sample[1];
				}
			} while(sample[0].chiSq < sample[1].chiSq);
		}
		
		// Now we can start homing in from both sides.
		while((sample[2].stepLen-sample[0].stepLen)/sample[1].stepLen > 
				0.15d && sample[2].stepLen-sample[0].stepLen > stepMin) {
			// Try the lower half first.
			stepLen = 0.5*(sample[0].stepLen+sample[1].stepLen);
			testSample = estPenalty(stepLen);
			if(testSample.chiSq >= sample[1].chiSq) {
				// That didn't work, try the upper half.
				sample[0] = testSample;
				stepLen = 0.5*(sample[1].stepLen+sample[2].stepLen);
				testSample = estPenalty(stepLen);
				if(testSample.chiSq >= sample[1].chiSq) {
					// The minimum's in the lower part of the upper half.
					sample[2] = testSample;
				} else {
					// The minimum's in the upper part of the upper half.
					sample[0] = sample[1];
					sample[1] = testSample;
				}
			} else {
				// The minimum's in the lower part of the lower half.
				sample[2] = sample[1];
				sample[1] = testSample;
			}
		}
		// Done.
		trialStep(sample[1].stepLen);
		if(LocUtil.deBugLevel > 0) System.out.format("Lintry: x dsp = "+
				"%7.3f %9.4f %5.2f\n", sample[1].stepLen, sample[1].chiSq, 
				sample[1].median);
		return sample[1];
	}
	
	/**
	 * Get the final trial vector.  The current hypocenter will be 
	 * moved by this much for this iteration.
	 * 
	 * @return Trial spatial vector in kilometers
	 */
	public double[] getTrialVector() {
		return trialVector;
	}
	
	/**
	 * Estimated dispersion at the trial step length based on 
	 * linear estimates of the travel-time residuals.
	 * 
	 * @param stepLen Trial step length in kilometers
	 * @return The results of the R-estimator algorithm
	 */
	private RestResult estPenalty(double stepLen) {
		double median, penalty;
		
		// Do the initial pass to project and demean the correlated residuals.
		trialStep(stepLen);
		for(int j=0; j<wRes.size(); j++) {
			wRes.get(j).updateEst(trialVector);
		}
		median = rEst.estMedian();
		rEst.deMedianEstRes();
		
		// Run the projection algorithm here...
		if(LocUtil.deCorrelate) {
			System.out.println("The decorrelation is not yet implemented.");
			return null;
		// ...unless we're not decorrelating.
		} else {
			penalty = rEst.estPenalty();
			if(LocUtil.deBugLevel > 0) System.out.format("Estlin: x dsp = "+
					"%7.3f %9.4f %5.2f\n", stepLen, penalty, median);
			return new RestResult(stepLen, median, 0d, penalty);
		}
	}
	
	/**
	 * Create a trial step vector in the steepest descent direction 
	 * with the length of stepLen.  Ensure that the depth is still 
	 * in a valid earthquake range.
	 * 
	 * @param stepLen Length of the trial step in kilometers.
	 */
	private void trialStep(double stepLen) {
		// Make the trial step vector.
		for(int j=0; j<stepDir.length; j++) {
			trialVector[j] = stepLen*stepDir[j];
		}
		// Make sure the depth is OK.
		if(hypo.degOfFreedom > 2) {
			// Trap air quakes.
			if(hypo.depth+trialVector[2] < LocUtil.DEPTHMIN) 
				trialVector[2] = LocUtil.DEPTHMIN-hypo.depth;
			// Trap lower mantle quakes.
			else if(hypo.depth+trialVector[2] > LocUtil.DEPTHMAX) 
				trialVector[2] = LocUtil.DEPTHMAX-hypo.depth;
		}
	}
}
