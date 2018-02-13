package gov.usgs.locator;

import gov.usgs.traveltime.AllBrnVol;

/**
 * Manage the R-estimator logic needed to refine the hypocenter.
 * 
 * @author Ray Buland
 *
 */
public class Stepper {
	Event event;
	Hypocenter hypo;
	AllBrnVol allBrn;
	AuxLocRef auxLoc;
	Cratons cratons;
	ZoneStats zones;
	PhaseID phaseID;
	Restimator rEstRaw, rEstProj;
	LinearStep linStep;
	RestResult result;
	HypoAudit lastHypo;
	DeCorr deCorr;

	/**
	 * Keep track of data needed.
	 * 
	 * @param event Event information
	 * @param allBrn Travel-time information
	 * @param phaseID Phase identification logic
	 * @param auxLoc Auxiliary locator information
	 */
	public Stepper(Event event, AllBrnVol allBrn, PhaseID phaseID, 
			AuxLocRef auxLoc) {
		this.event = event;
		hypo = event.hypo;
		this.allBrn = allBrn;
		this.auxLoc = auxLoc;
		cratons = auxLoc.cratons;
		zones = auxLoc.zoneStats;
		this.phaseID = phaseID;
		rEstRaw = event.rEstRaw;
		rEstProj = event.rEstProj;
		linStep = new LinearStep(event);
		deCorr = event.deCorr;
	}
	
	/**
	 * Set the tectonic flag and Bayesian depth parameters.  Then calculate 
	 * the median residual (origin time correction), R-estimator dispersion, and 
	 * R-estimator direction of steepest descents.  This initial version also 
	 * sets the reference dispersion value in the hypocenter.
	 * 
   * @param otherWeight Weight for phases that don't match the current 
   * phase identification or the current phase group (higher weights 
   * make changing to an "other" phase easier)
   * @param stickyWeight Weight for an exact match (higher weights make 
   * changing the current identification harder.
   * @param reID If true, do the full phase re-identification, if false 
   * try not to change identifications
   * @param reWeight If true, update the residual weights
	 * @return LocStatus value
	 * @throws Exception On an illegal source depth
	 */
	public LocStatus setInitDir(double otherWeight, double stickyWeight, boolean reID, 
			boolean reWeight) throws Exception {
		LocStatus status;
		
		status = setDir(otherWeight, stickyWeight, reID, reWeight);
		if(status == LocStatus.SUCCESS) hypo.chiSq = result.chiSq;
		return status;
	}
	
	/**
	 * Set the tectonic flag and Bayesian depth parameters.  Then calculate 
	 * the median residual (origin time correction), R-estimator dispersion, and 
	 * R-estimator direction of steepest descents.  For calls from makeStep, 
	 * we don't want to update the reference dispersion in the hypocenter just 
	 * yet.
	 * 
   * @param otherWeight Weight for phases that don't match the current 
   * phase identification or the current phase group (higher weights 
   * make changing to an "other" phase easier)
   * @param stickyWeight Weight for an exact match (higher weights make 
   * changing the current identification harder.
   * @param reID If true, do the full phase re-identification, if false 
   * try not to change identifications
   * @param reWeight If true, update the residual weights
	 * @return LocStatus value
	 * @throws Exception On an illegal source depth
	 */
	private LocStatus setDir(double otherWeight, double stickyWeight, boolean reID, 
  		boolean reWeight) throws Exception {
		double bayesDepth, bayesSpread, medianRes, medianProj, chiSq;
		
		// If we're re-weighting, reset the craton and zone statistics 
		// as well.
		if(reWeight) {
			// Set the tectonic flag.  Note that everything outside cratons 
			// is considered tectonic.
			if(auxLoc.cratons.isCraton(hypo.latitude, hypo.longitude)) {
				LocUtil.tectonic = false;
			} else {
				LocUtil.tectonic = true;
			}
			if(LocUtil.deBugLevel > 0) System.out.println("\n\tTectonic = "+
					LocUtil.tectonic);
			if(!event.prefDepth) {
				// Update the Bayesian depth if it wasn't set by the analyst.
				bayesDepth = zones.bayesDepth(hypo.latitude, hypo.longitude);
				bayesSpread = zones.bayesSpread();
				hypo.updateBayes(bayesDepth, bayesSpread);
				if(LocUtil.deBugLevel > 0) System.out.format("\tBayes: %5.1f %5.1f\n", 
						bayesDepth, hypo.depthWeight);
			}
		}
		
		// Reidentify phases.
		event.changed = phaseID.doID(otherWeight, stickyWeight, reID, reWeight);
		// Bail on insufficient data.
		if(event.staUsed < 3) return LocStatus.INSUFFICIENT_DATA;
		
		if(LocUtil.deCorrelate) {
			// Demedian the raw residuals.
			medianRes = rEstRaw.median();
			rEstRaw.deMedianRes();
			if(LocUtil.deBugLevel > 0) System.out.format("\nLsrt: EL av = "+
					"%8.4f\n", medianRes);
			// Decorrelate the raw data.
			if(event.changed) deCorr.deCorr();
			deCorr.project();
			// Get the median of the projected data.
			medianProj = rEstProj.median();
			// Demedian the projected design matrix.
			rEstProj.deMedianDesign();
			// Get the R-estimator dispersion of the projected data.
			chiSq = rEstProj.penalty();
			if(LocUtil.deBugLevel > 0) System.out.format("Lsrt: ST av chisq"+
					" = %8.4f %10.4f\n", medianProj, chiSq);
			// Get the steepest descent direction.
			hypo.stepDir = rEstProj.steepest(hypo.degOfFreedom);
		} else {
			// Demedian the raw residuals.
			medianRes = rEstRaw.median();
			rEstRaw.deMedianRes();
			// Demedian the raw design matrix.
			rEstRaw.deMedianDesign();
			// Get the R-estimator dispersion of the raw data.
			chiSq = rEstRaw.penalty();
			if(LocUtil.deBugLevel > 0) System.out.format("\nLsrt: ST av chisq"+
					" = %8.4f %10.4f\n", medianRes, chiSq);
			// Get the steepest descent direction.
			hypo.stepDir = rEstRaw.steepest(hypo.degOfFreedom);
		}
		
		if(LocUtil.deBugLevel > 0) {
			System.out.print("Adder: b =");
			for(int j=0; j<hypo.stepDir.length; j++) {
				System.out.format(" %7.4f", hypo.stepDir[j]);
			}
			System.out.println();
		}
		result = new RestResult(0d, medianRes, 0d, chiSq);
		
		return LocStatus.SUCCESS;
	}
	
	/**
	 * Make a step from the current hypocenter to the optimal R-estimator 
	 * dispersion minimum based on linearized residual estimates.
	 * 
	 * @param stage Iteration stage
	 * @param iter Iteration within the stage
	 * @return LocStatus value
	 * @throws Exception If the linearized step length bisection algorithm 
	 * fails
	 */
	public LocStatus makeStep(int stage, int iter) throws Exception {
		double damp;
		LocStatus status = LocStatus.SUCCESS;
		
		// Save the current hypocenter as a reference for the step length damping.
		lastHypo = new HypoAudit(hypo, 0, 0, event.phUsed, status);
		
		// Get the linearized step.
		hypo.noDamp = 0;
		damp = LocUtil.dampFactor();
		hypo.stepLen = Math.max(hypo.stepLen, 2d*LocUtil.CONVLIM[stage]);
		result = linStep.stepLength(hypo.stepDir, hypo.stepLen, 
				LocUtil.CONVLIM[stage], LocUtil.STEPLIM[stage], hypo.chiSq);
		// This weird special case appears once in a while.
		if(result.chiSq >= hypo.chiSq && result.stepLen < 
				LocUtil.CONVLIM[stage]) {
			hypo.stepLen = result.stepLen;
			hypo.delH = 0d;
			hypo.delZ = 0d;
			logStep("Step", stage, iter, status);
			return status;
		}
		
		// Update the hypocenter.
		hypo.medianRes = result.median;
		event.updateHypo(result.stepLen, result.median);
		// Reidentify phases and get the non-linear R-estimator parameters 
		// for the new hypocenter.
		if(setDir(0.01d, 5d, false, false) == LocStatus.INSUFFICIENT_DATA) {
			return LocStatus.INSUFFICIENT_DATA;
		}
		event.updateOrigin(result.median);
		// If the phase identification has changed, we have to start over.
		if(event.changed) {
			hypo.chiSq = result.chiSq;
			status = LocStatus.PHASEID_CHANGED;
			logStep("ReID", stage, iter, status);
			return status;
		}
		
		// If we're headed down hill, this iteration is done.
		if(result.chiSq < hypo.chiSq) {
			hypo.chiSq = result.chiSq;
			logStep("Step", stage, iter, status);
			return status;
		}
		
		/* 
		 * Damp the solution.  Damping is necessary if the linearized step 
		 * increases the R-estimator dispersion (variously called penalty and 
		 * chi-squared here).  However, it is observed to be highly unstable, 
		 * hence the complicated factor to determine the damping factor and 
		 * the elaborate means to trap a failure.  Note that the damping 
		 * factor is only updated once per call to makeStep.  This is because 
		 * the infinite loop we're avoiding depends on a cycle comprised of  
		 * a normal step and a damped step.
		 */
		do {
			// Trap a failed damping strategy.
			if(damp*hypo.stepLen <= LocUtil.CONVLIM[stage] || (hypo.noDamp > 0 && 
					LocUtil.hypoCompare(hypo, lastHypo))) {
				// We've damped the solution into oblivion.  Give up.
				hypo.resetHypo(lastHypo);
				hypo.delH = 0d;
				hypo.delZ = 0d;
				// Set the exit status.
				if(result.chiSq <= LocUtil.ALMOST*hypo.chiSq && hypo.stepLen <= 
						LocUtil.CONVLIM[stage]) status = LocStatus.NEARLY_CONVERGED;
				else if(hypo.stepLen <= LocUtil.STEPTOL) status = 
						LocStatus.DID_NOT_CONVERGE;
				else status = LocStatus.UNSTABLE_SOLUTION;
				logStep("Fail", stage, iter, status);
				return status;
			}
			
			// Do the damping.
			hypo.noDamp++;
			hypo.resetHypo(lastHypo);
			hypo.stepLen *= damp;
			hypo.medianRes *= damp;
			// Update the hypocenter.
			event.updateHypo(hypo.stepLen, hypo.medianRes);
			// Reidentify phases and get the non-linear R-estimator parameters 
			// for the new hypocenter.
			if(setDir(0.01d, 5d, false, false) == LocStatus.INSUFFICIENT_DATA) {
				return LocStatus.INSUFFICIENT_DATA;
			}
			event.updateOrigin(result.median);
			// If the phase identification has changed, we have to start over.
			if(event.changed) {
				hypo.chiSq = result.chiSq;
				status = LocStatus.PHASEID_CHANGED;
				logStep("ReID", stage, iter, status);
				return status;
			}
			logStep("Damp", stage, iter, status);
		} while(result.chiSq >= hypo.chiSq);
		
		return status;
	}
	
	/**
	 * Log the current step.  Note that this logging is the principle means of 
	 * debugging Locator problems.  In the long run, this output should be 
	 * entered into the error log and only printed for a debug level of at 
	 * least one.
	 * 
	 * @param tag Id for the log entry
	 * @param stage Iteration stage
	 * @param iter Iteration within the stage
	 * @param status Stepper status
	 */
	private void logStep(String tag, int stage, int iter, LocStatus status) {
		int used;
		
		if(LocUtil.deCorrelate) used = event.vPhUsed;
		else used = event.phUsed;
		if(used >= hypo.degOfFreedom) hypo.rms = hypo.chiSq/
				(used-hypo.degOfFreedom+1);
		else hypo.rms = 0d;
		if(LocUtil.deBugLevel > 0) System.out.format("\n%s: %1d %2d %5d %8.4f "+
				"%8.4f %6.2f del= %5.1f %6.1f rms= %6.2f %s\n", tag, stage, iter, 
				used, hypo.latitude, hypo.longitude, hypo.depth, hypo.delH, 
				hypo.delZ, hypo.rms, status);
	}
}
