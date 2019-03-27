package gov.usgs.locator;


/**
 * Manage the R-estimator logic needed to refine the hypocenter.
 * 
 * @author Ray Buland
 *
 */
public class Stepper {
	Event event;
	Hypocenter hypo;
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
	 * @param phaseID Phase identification logic
	 * @param auxLoc Auxiliary locator information
	 */
	public Stepper(Event event, PhaseID phaseID, AuxLocRef auxLoc) {
		this.event = event;
		hypo = event.getHypo();
		this.auxLoc = auxLoc;
		cratons = auxLoc.getCratons();
		zones = auxLoc.getZoneStats();
		this.phaseID = phaseID;
		rEstRaw = event.getRawRankSumEstimator();
		rEstProj = event.getProjectedRankSumEstimator();
		linStep = new LinearStep(event);
		deCorr = event.getDecorrelator();
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
		if(status == LocStatus.SUCCESS) hypo.setEstimatorDispersionValue(result.chiSq);
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
		double medianRes, medianProj, chiSq;
		
		// Set the location environment.
		if(reWeight) setEnviron();
		
		// Reidentify phases.
		event.setHasPhaseIdChanged(phaseID.doID(otherWeight, stickyWeight, reID, reWeight));
		// Bail on insufficient data.
		if(event.getNumStationsUsed() < 3) return LocStatus.INSUFFICIENT_DATA;
		
		if(LocUtil.useDecorrelation) {
			// Demedian the raw residuals.
			medianRes = rEstRaw.median();
			rEstRaw.deMedianRes();
			if(LocUtil.deBugLevel > 0) System.out.format("Lsrt: EL av = "+
					"%8.4f\n", medianRes);
			// Decorrelate the raw data.
			if(event.getHasPhaseIdChanged()) deCorr.deCorr();
			deCorr.projectPicks();
			// Get the median of the projected data.
			medianProj = rEstProj.median();
			// Demedian the projected design matrix.
			rEstProj.deMedianDesign();
			// Get the R-estimator dispersion of the projected data.
			chiSq = rEstProj.penalty();
			if(LocUtil.deBugLevel > 0) System.out.format("Lsrt: ST av chisq"+
					" = %8.4f %10.4f\n", medianProj, chiSq);
			// Get the steepest descent direction.
			hypo.setStepDirectionUnitVector(rEstProj.steepest(hypo.getDegreesOfFreedom()));
		} else {
			// Demedian the raw residuals.
			medianRes = rEstRaw.median();
			rEstRaw.deMedianRes();
			// Demedian the raw design matrix.
			rEstRaw.deMedianDesign();
			// Get the R-estimator dispersion of the raw data.
			chiSq = rEstRaw.penalty();
			if(LocUtil.deBugLevel > 0) System.out.format("Lsrt: ST av chisq"+
					" = %8.4f %10.4f\n", medianRes, chiSq);
			// Get the steepest descent direction.
			hypo.setStepDirectionUnitVector(rEstRaw.steepest(hypo.getDegreesOfFreedom()));
		}
		
		if(LocUtil.deBugLevel > 0) {
			System.out.print("Adder: b =");
			for(int j=0; j<hypo.getStepDirectionUnitVector().length; j++) {
				System.out.format(" %7.4f", hypo.getStepDirectionUnitVector()[j]);
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
		lastHypo = new HypoAudit(hypo, 0, 0, event.getNumPhasesUsed(), status);
		
		// Get the linearized step.
		hypo.setNumOfTimesStepLengthDampening(0);
		damp = LocUtil.computeDampeningFactor();
		hypo.setStepLength(Math.max(hypo.getStepLength(), 2d*LocUtil.CONVERGENCESTAGELIMITS[stage]));
		result = linStep.stepLength(hypo.getStepDirectionUnitVector(), hypo.getStepLength(), 
				LocUtil.CONVERGENCESTAGELIMITS[stage], LocUtil.STEPLENSTAGELIMITS[stage], hypo.getEstimatorDispersionValue());
		// This weird special case appears once in a while.
		if(result.chiSq >= hypo.getEstimatorDispersionValue() && result.stepLen < 
				LocUtil.CONVERGENCESTAGELIMITS[stage]) {
			hypo.setStepLength(result.stepLen);
			hypo.setHorizontalStepLength(0d);
			hypo.setVerticalStepLength(0d);
			logStep("Step", stage, iter, status);
			return status;
		}
		
		// Update the hypocenter.
		hypo.setLinearTimeShiftEstimate(result.median);
		event.updateHypo(result.stepLen, result.median);
		// Reidentify phases and get the non-linear R-estimator parameters 
		// for the new hypocenter.
		if(setDir(0.01d, 5d, false, false) == LocStatus.INSUFFICIENT_DATA) {
			return LocStatus.INSUFFICIENT_DATA;
		}
		event.updateOriginTime(result.median);
		// If the phase identification has changed, we have to start over.
		if(event.getHasPhaseIdChanged()) {
			hypo.setEstimatorDispersionValue(result.chiSq);
			status = LocStatus.PHASEID_CHANGED;
			logStep("ReID", stage, iter, status);
			return status;
		}
		
		// If we're headed down hill, this iteration is done.
		if(result.chiSq < hypo.getEstimatorDispersionValue()) {
			hypo.setEstimatorDispersionValue(result.chiSq);
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
			if(damp*hypo.getStepLength() <= LocUtil.CONVERGENCESTAGELIMITS[stage] || (hypo.getNumOfTimesStepLengthDampening() > 0 && 
					LocUtil.compareHypos(hypo, lastHypo))) {
				// We've damped the solution into oblivion.  Give up.
				hypo.resetHypo(lastHypo);
				hypo.setHorizontalStepLength(0d);
				hypo.setVerticalStepLength(0d);
				// Set the exit status.
				if(result.chiSq <= LocUtil.ALMOSTCONVERGED*hypo.getEstimatorDispersionValue() && hypo.getStepLength() <= 
						LocUtil.CONVERGENCESTAGELIMITS[stage]) status = LocStatus.NEARLY_CONVERGED;
				else if(hypo.getStepLength() <= LocUtil.STEPTOLERANCE) status = 
						LocStatus.DID_NOT_CONVERGE;
				else status = LocStatus.UNSTABLE_SOLUTION;
				logStep("Fail", stage, iter, status);
				return status;
			}
			
			// Do the damping.
			hypo.setNumOfTimesStepLengthDampening(hypo.getNumOfTimesStepLengthDampening() + 1);
			hypo.resetHypo(lastHypo);
			hypo.setStepLength(hypo.getStepLength() * damp);
			hypo.setLinearTimeShiftEstimate(hypo.getLinearTimeShiftEstimate() * damp);
			// Update the hypocenter.
			event.updateHypo(hypo.getStepLength(), hypo.getLinearTimeShiftEstimate());
			// Reidentify phases and get the non-linear R-estimator parameters 
			// for the new hypocenter.
			if(setDir(0.01d, 5d, false, false) == LocStatus.INSUFFICIENT_DATA) {
				return LocStatus.INSUFFICIENT_DATA;
			}
			event.updateOriginTime(result.median);
			// If the phase identification has changed, we have to start over.
			if(event.getHasPhaseIdChanged()) {
				hypo.setEstimatorDispersionValue(result.chiSq);
				status = LocStatus.PHASEID_CHANGED;
				logStep("ReID", stage, iter, status);
				return status;
			}
			logStep("Damp", stage, iter, status);
		} while(result.chiSq >= hypo.getEstimatorDispersionValue());
		
		return status;
	}
	
	/**
	 * Set the location environment by determining if the location is in 
	 * a craton or tectonic area and setting the Bayesian depth.
	 */
	protected void setEnviron() {
		double bayesDepth, bayesSpread;
		
		// Set the tectonic flag.  Note that everything outside cratons 
		// is considered tectonic.
		if(auxLoc.getCratons().isCraton(hypo.getLatitude(), hypo.getLongitude())) {
			LocUtil.isTectonic = false;
		} else {
			LocUtil.isTectonic = true;
		}
		if(LocUtil.deBugLevel > 0) System.out.println("\n\tTectonic = "+
				LocUtil.isTectonic);
		if(!event.getIsDepthManual()) {
			// Update the Bayesian depth if it wasn't set by the analyst.
			bayesDepth = zones.bayesDepth(hypo.getLatitude(), hypo.getLongitude());
			bayesSpread = zones.bayesSpread();
			hypo.updateBayes(bayesDepth, bayesSpread);
		}
		if(LocUtil.deBugLevel > 0) System.out.format("\tBayes: %5.1f %5.3f %b\n", 
				hypo.getBayesianDepth(), hypo.getBayesianDepthWeight(), event.getIsDepthManual());
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
		
		if(LocUtil.useDecorrelation) used = event.getNumProjectedPhasesUsed();
		else used = event.getNumPhasesUsed();
		if(used >= hypo.getDegreesOfFreedom()) hypo.setEstimatorRMSEquivalent(hypo.getEstimatorDispersionValue() /
				(used-hypo.getDegreesOfFreedom() + 1));
		else hypo.setEstimatorRMSEquivalent(0d);
		if(LocUtil.deBugLevel > 0) System.out.format("\n%s: %1d %2d %5d %8.4f "+
				"%8.4f %6.2f del= %5.1f %6.1f rms= %6.2f %s\n", tag, stage, iter, 
				used, hypo.getLatitude(), hypo.getLongitude(), hypo.getDepth(), hypo.getHorizontalStepLength(), 
				hypo.getVerticalStepLength(), hypo.getEstimatorRMSEquivalent(), status);
	}
}
