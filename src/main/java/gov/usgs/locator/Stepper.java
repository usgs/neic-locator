package gov.usgs.locator;

import java.util.logging.Logger;

import gov.usgs.traveltime.BadDepthException;
import gov.usgs.traveltime.tables.TauIntegralException;

/**
 * The Stepper class manages the rank-sum-estimator logic needed to refine the hypocenter.
 *
 * @author Ray Buland
 */
public class Stepper {
  /**
   * We need to force the decorrelation to be done at least once even if the phase identification
   * didn't change.
   */
  private boolean firstDecorrelationDone = false;

  /** An Event object containing the event to use when performing Stepper calculations. */
  private Event event;

  /**
   * A Hypocenter object containing the hypocenter of the event to use when performing Stepper
   * calculations.
   */
  private Hypocenter hypo;

  /** A Cratons object holding the geographic boundaries of continental cratons. */
  private Cratons cratons;

  /** A ZoneStats object containing earthquake statistics by geographic location. */
  private ZoneStats zoneStats;

  /** A PhaseID object containing the phase identification logic. */
  private PhaseID phaseIDLogic;

  /** A LinearStep object used for various travel time computations. */
  private LinearStep linearStep;

  /** A RSumEstResult object holding the most recent rank-sum estimation rSumEstResult. */
  private RSumEstResult rSumEstResult;

  /** A RankSumEstimator object used for the rank-sum estimation of the raw picks. */
  private RankSumEstimator rawRankSumEstimator;

  /** A RankSumEstimator object used for the rank-sum estimation of the projected picks. */
  private RankSumEstimator projectedRankSumEstimator;

  /** A Decorrelator object used when decorrelating the event picks. */
  private Decorrelator decorrelator;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(Stepper.class.getName());

  /** A double containing the median of the residuals used by the derivative test */
  private double residualsMedian;

  /**
   * The Stepper constructor. Set the event, phaseID logic, and auxiliary locator information to the
   * provided values
   *
   * @param event An Event object containing the event to use when performing Stepper calculations.
   * @param phaseIDLogic A PhaseID object containing the phase identification logic.
   * @param auxLoc An AuxLocRef object containing auxiliary locator information used when performing
   *     Stepper calculations.
   */
  public Stepper(Event event, PhaseID phaseIDLogic, AuxLocRef auxLoc) {
    this.event = event;
    hypo = event.getHypo();
    cratons = auxLoc.getCratons();
    zoneStats = auxLoc.getZoneStats();
    this.phaseIDLogic = phaseIDLogic;
    rawRankSumEstimator = event.getRawRankSumEstimator();
    projectedRankSumEstimator = event.getProjectedRankSumEstimator();
    linearStep = new LinearStep(event);
    decorrelator = event.getDecorrelator();
  }

  /**
   * The Stepper phase identification function. Sets the tectonic flag and Bayesian depth
   * parameters. Calculate the median residual (origin time correction), rank-sum-estimator
   * dispersion, and rank-sum-estimator direction of steepest descents. This initial version also
   * sets the reference dispersion value in the hypocenter.
   *
   * @param otherWeight A double value holding the weight for phases that don't match the current
   *     phase identification or the current phase group (higher weights make changing to an "other"
   *     phase easier)
   * @param stickyWeight A double value holding the weight for an exact match (higher weights make
   *     changing the current identification harder.
   * @param reidentifyPhases A boolean flag indicating whether to perform a full phase
   *     re-identification, if false try not to change identifications
   * @param updateResWeights A boolean flag indicating whether to the residual weights
   * @return A LocStatus object containing the status value.
   * @throws BadDepthException If source depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public LocStatus doPhaseIdentification(
      double otherWeight, double stickyWeight, boolean reidentifyPhases, boolean updateResWeights)
      throws BadDepthException, TauIntegralException {
    // Reidentify phases.
    LocStatus status =
        internalPhaseID(otherWeight, stickyWeight, reidentifyPhases, updateResWeights);

    if (status == LocStatus.SUCCESS) {
      updateStepDirection();
      hypo.setEstimatorDispersionValue(rSumEstResult.getDispersion());
    }

    return status;
  }

  /**
   * The Stepper internal phase identification function. Sets the tectonic flag and Bayesian depth
   * parameters. Then calculates the median residual (origin time correction), and 
   * rank-sum-estimator dispersion. For calls from makeStep, we don't want to update the reference 
   * dispersion in the hypocenter just yet.
   *
   * @param otherWeight A double value holding the weight for phases that don't match the current
   *     phase identification or the current phase group (higher weights make changing to an "other"
   *     phase easier)
   * @param stickyWeight A double value holding the weight for an exact match (higher weights make
   *     changing the current identification harder.
   * @param reidentifyPhases A boolean flag indicating whether to perform a full phase
   *     re-identification, if false try not to change identifications
   * @param updateResWeights A boolean flag indicating whether to the residual weights
   * @return A LocStatus object containing the status value.
   * @throws BadDepthException If source depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  private LocStatus internalPhaseID(
      double otherWeight, double stickyWeight, boolean reidentifyPhases, boolean updateResWeights) 
      		throws BadDepthException, TauIntegralException {

    // Set the location environment.
    if (updateResWeights) {
      setLocEnvironment();
    }
    
    // Reidentify phases.
    event.setHasPhaseIdChanged(
        phaseIDLogic.phaseID(otherWeight, stickyWeight, reidentifyPhases, updateResWeights));

    // Bail on insufficient data.
    if (event.getNumStationsUsed() < 3) {
      return LocStatus.INSUFFICIENT_DATA;
    }

    double dispersion;
    if (LocUtil.useDecorrelation) {
      // Demedian the raw residuals.
      residualsMedian = rawRankSumEstimator.computeMedian();
      rawRankSumEstimator.deMedianResiduals();

      LOGGER.fine(String.format("Lsrt: EL av = %8.4f", residualsMedian));

      // Decorrelate the raw data.
      if (event.getHasPhaseIdChanged() || !firstDecorrelationDone) {
        firstDecorrelationDone = true;
        decorrelator.decorrelate();
      }
      decorrelator.projectPicks();

      // Get the median of the projected data.
      double projectedMedian = projectedRankSumEstimator.computeMedian();

      // Demedian the projected design matrix.
      projectedRankSumEstimator.deMedianDesignMatrix();

      // Get the rank-sum-estimator dispersion of the projected data.
      dispersion = projectedRankSumEstimator.computeDispersionValue();

      LOGGER.fine(String.format("Lsrt: ST av chisq = %8.4f %10.4f", projectedMedian, dispersion));
    } else {
      // Demedian the raw residuals.
      residualsMedian = rawRankSumEstimator.computeMedian();

      rawRankSumEstimator.deMedianResiduals();

      // Demedian the raw design matrix.
      rawRankSumEstimator.deMedianDesignMatrix();

      // Get the rank-sum-estimator dispersion of the raw data.
      dispersion = rawRankSumEstimator.computeDispersionValue();

      LOGGER.fine(String.format("Lsrt: ST av chisq = %8.4f %10.4f", residualsMedian, dispersion));
    }

    rSumEstResult = new RSumEstResult(0d, residualsMedian, 0d, dispersion);

    return LocStatus.SUCCESS;
  }
  
  /**
   * Compute the rank-sum-estimator direction of steepest descents.  This has been separated from 
   * internalPhaseID because we don't want to update the direction during step length damping.
   */
  private void updateStepDirection() {
    if (LocUtil.useDecorrelation) {
      hypo.setStepDirectionUnitVector(
          projectedRankSumEstimator.compSteepestDescDir(hypo.getDegreesOfFreedom()));
    } else {
      hypo.setStepDirectionUnitVector(
          rawRankSumEstimator.compSteepestDescDir(hypo.getDegreesOfFreedom()));
    }

	    String adderString = "Adder: b =";
	    for (int j = 0; j < hypo.getStepDirectionUnitVector().length; j++) {
	      adderString += String.format(" %7.4f", hypo.getStepDirectionUnitVector()[j]);
	    }
	    LOGGER.fine(adderString);
  }

  /**
   * This function makes a step from the current hypocenter to the optimal rank-sum-estimator
   * dispersion minimum based on linearized residual estimates.
   *
   * @param stage An int containing the current stage
   * @param iteration An int containing the current iteration within the stage
   * @return A LocStatus object holding the current stepper status
   * @throws BadDepthException If source depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public LocStatus makeStep(int stage, int iteration) 
  		throws BadDepthException, TauIntegralException {
    LocStatus status = LocStatus.SUCCESS;

    // Save the current hypocenter as a reference for the step length damping.
    HypoAudit lastHypoAudit = new HypoAudit(hypo, 0, 0, event.getNumPhasesUsed(), status);

    // Get the linearized step.
    hypo.setNumOfTimesStepLengthDampening(0);
    double damp = LocUtil.computeDampeningFactor();
    hypo.setStepLength(Math.max(hypo.getStepLength(), 2d * LocUtil.CONVERGENCESTAGELIMITS[stage]));
    rSumEstResult =
        linearStep.stepLength(
            hypo.getStepDirectionUnitVector(),
            hypo.getStepLength(),
            LocUtil.CONVERGENCESTAGELIMITS[stage],
            LocUtil.STEPLENSTAGELIMITS[stage],
            hypo.getEstimatorDispersionValue());

    // If the dispersion didn't change, don't make the step (meaning we've converged).
    if (rSumEstResult.getDispersion() >= hypo.getEstimatorDispersionValue()
        && rSumEstResult.getStepLength() < LocUtil.CONVERGENCESTAGELIMITS[stage]) {
      hypo.setHorizontalStepLength(0d);
      hypo.setVerticalStepLength(0d);
      hypo.setStepLength(0d);

      logStep("Step", stage, iteration, status);

      return status;
    }

    // Update the hypocenter.
    hypo.setLinearTimeShiftEstimate(rSumEstResult.getMedianResidual());
    event.updateHypo(rSumEstResult.getStepLength(), rSumEstResult.getMedianResidual());

    // Reidentify phases and get the non-linear rank-sum-estimator parameters
    // for the new hypocenter.
    if (internalPhaseID(0.01d, 5d, false, true)
        == LocStatus.INSUFFICIENT_DATA) { // ReWeight always true 9/16/19.
      return LocStatus.INSUFFICIENT_DATA;
    }
    event.updateOriginTime(rSumEstResult.getMedianResidual());

    // If the phase identification has changed, we have to start over.
    if (event.getHasPhaseIdChanged()) {
      hypo.setEstimatorDispersionValue(rSumEstResult.getDispersion());
//    setLocEnvironment();
      updateStepDirection();
      status = LocStatus.PHASEID_CHANGED;
      logStep("ReID", stage, iteration, status);
      return status;
    }

    // If we're headed down hill, this iteration is done.
    if (rSumEstResult.getDispersion() < hypo.getEstimatorDispersionValue()) {
    	// Delay resetting the Bayesian depth for stability reasons.
      hypo.setEstimatorDispersionValue(rSumEstResult.getDispersion());
//    setLocEnvironment();
      updateStepDirection();
      logStep("Step", stage, iteration, status);
      return status;
    }

    // Damp the solution.  Damping is necessary if the linearized step increases
    // the rank-sum-estimator dispersion (variously called
    // computeDispersionValue and dispersion here).  However, it is observed to
    // be highly unstable, hence the complicated factor to determine the damping
    // factor and the elaborate means to trap a failure.  Note that the damping
    // factor is only updated once per call to makeStep.  This is because the
    // infinite loop we're avoiding depends on a cycle comprised of a normal
    // step and a damped step.
    status = LocStatus.DAMP_STEP_LENGTH;
    do {
      // Trap a failed damping strategy.
      if (damp * hypo.getStepLength() <= LocUtil.CONVERGENCESTAGELIMITS[stage]
          || (hypo.getNumOfTimesStepLengthDampening() > 0
              && LocUtil.compareHypos(hypo, lastHypoAudit))) {
        // We've damped the solution into oblivion.  Give up.
        hypo.resetHypo(lastHypoAudit);
        hypo.setHorizontalStepLength(0d);
        hypo.setVerticalStepLength(0d);

        // Set the exit status.
        if (rSumEstResult.getDispersion()
                <= LocUtil.ALMOSTCONVERGED * hypo.getEstimatorDispersionValue()
            && hypo.getStepLength() <= LocUtil.CONVERGENCESTAGELIMITS[stage]) {
          status = LocStatus.NEARLY_CONVERGED;
        } else if (hypo.getStepLength() <= LocUtil.STEPTOLERANCE) {
          status = LocStatus.DID_NOT_CONVERGE;
        } else {
          status = LocStatus.UNSTABLE_SOLUTION;
        }

        logStep("Fail", stage, iteration, status);

        return status;
      }

      // Do the damping.
      hypo.setNumOfTimesStepLengthDampening(hypo.getNumOfTimesStepLengthDampening() + 1);
      LOGGER.fine(String.format("Damping: %d %6.4f", hypo.getNumOfTimesStepLengthDampening(), 
      		damp));
      hypo.resetHypo(lastHypoAudit);
      hypo.setStepLength(hypo.getStepLength() * damp);
      hypo.setLinearTimeShiftEstimate(hypo.getLinearTimeShiftEstimate() * damp);

      // Update the hypocenter.
      event.updateHypo(hypo.getStepLength(), hypo.getLinearTimeShiftEstimate());

      // Reidentify phases and get the non-linear rank-sum-estimator parameters
      // for the new hypocenter.  Don't update the Bayesian depth.  If the depth 
      // residual is large the epicenter will never move much.
      if (internalPhaseID(0.01d, 5d, false, true)
          == LocStatus.INSUFFICIENT_DATA) { // ReWeight always true 9/16/19.
        return LocStatus.INSUFFICIENT_DATA;
      }
      event.updateOriginTime(rSumEstResult.getMedianResidual());

      // If the phase identification has changed, we have to start over.
      if (event.getHasPhaseIdChanged()) {
        hypo.setEstimatorDispersionValue(rSumEstResult.getDispersion());
        status = LocStatus.PHASEID_CHANGED;

        logStep("ReID", stage, iteration, status);

        return status;
      }

      logStep("Damp", stage, iteration, status);
    } while (rSumEstResult.getDispersion() >= hypo.getEstimatorDispersionValue());
    
    // The step length damping actually worked!
    return LocStatus.SUCCESS;
  }

  /**
   * This function sets the location environment by determining if the location is in a craton or
   * tectonic area and setting the Bayesian depth.
   */
  protected void setLocEnvironment() {
    // Set the tectonic flag.  Note that everything outside cratons
    // is considered tectonic.
    if (cratons.isCraton(hypo.getLatitude(), hypo.getLongitude())) {
      LocUtil.isTectonic = false;
    } else {
      LocUtil.isTectonic = true;
    }

    LOGGER.fine("Tectonic = " + LocUtil.isTectonic);

    if (!event.getIsDepthManual()) {
      // Update the Bayesian depth if it wasn't set by the analyst.
      double bayesDepth = zoneStats.getBayesDepth(hypo.getLatitude(), hypo.getLongitude());
      double bayesSpread = zoneStats.getBayesSpread();
      hypo.updateBayes(bayesDepth, bayesSpread);
//    rawRankSumEstimator.updateBayesianResidual(hypo.getBayesianDepthResidual(), 
//    		hypo.getBayesianDepthWeight());
    }
    LOGGER.fine(
        String.format(
            "Bayes: %5.1f %5.3f %b",
            hypo.getBayesianDepth(), hypo.getBayesianDepthWeight(), event.getIsDepthManual()));
  }

  /**
   * This function logs the current step. Note that this logging is the principle means of debugging
   * Locator problems. In the long run, this output should be entered into the error log and only
   * printed for a debug level of at least one.
   *
   * @param id A string containing the id for the log entry
   * @param stage An int containing the current stage
   * @param iteration An int containing the current iteration within the stage
   * @param status A LocStatus object holding the current stepper status
   */
  private void logStep(String id, int stage, int iteration, LocStatus status) {
    int used;
    if (LocUtil.useDecorrelation) {
      used = event.getNumProjectedPhasesUsed();
    } else {
      used = event.getNumPhasesUsed();
    }

    if (used >= hypo.getDegreesOfFreedom()) {
      hypo.setEstimatorRMSEquivalent(
      		rSumEstResult.getDispersion() / (used - hypo.getDegreesOfFreedom() + 1));
    } else {
      hypo.setEstimatorRMSEquivalent(0d);
    }

    LOGGER.fine(
        String.format(
            "%s: %1d %2d %5d %8.4f %8.4f %6.2f del= %5.1f %6.1f " + "rms= %6.2f %s",
            id,
            stage,
            iteration,
            used,
            hypo.getLatitude(),
            hypo.getLongitude(),
            hypo.getDepth(),
            hypo.getHorizontalStepLength(),
            hypo.getVerticalStepLength(),
            hypo.getEstimatorRMSEquivalent(),
            status));
  }
}
