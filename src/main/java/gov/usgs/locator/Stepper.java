package gov.usgs.locator;

import gov.usgs.locaux.AuxLocRef;
import gov.usgs.locaux.Cratons;
import gov.usgs.locaux.LocUtil;
import gov.usgs.locaux.NewZoneStats;
import gov.usgs.locaux.Slabs;
import gov.usgs.locaux.ZoneStats;
import gov.usgs.traveltime.BadDepthException;
import gov.usgs.traveltime.TauUtil;
import gov.usgs.traveltime.tables.TauIntegralException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

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

  /** A ZoneStats object containing earthquake statistics by geographic location. */
  private NewZoneStats newZoneStats;
  
  /** A slabs object containing slab depths by geographic location. */
  private Slabs slabStats;

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
   * A double containing the contribution of the Bayesian constraint to the previous dispersion
   * computed.
   */
  private double lastContribution;
  /**
   * A double containing the contribution of the Bayesian constraint to the latest dispersion
   * computed.
   */
  private double bayesianContribution = 0d;

  /**
   * The Stepper constructor. Set the event, phaseID logic, and auxiliary locator information to the
   * provided values
   *
   * @param event An Event object containing the event to use when performing Stepper calculations.
   * @param phaseIDLogic A PhaseID object containing the phase identification logic.
   * @param auxLoc An AuxLocRef object containing auxiliary locator information used when performing
   *     Stepper calculations.
   * @param slabs A Slabs object containing the slab part of the auxiliary data
   */
  public Stepper(Event event, PhaseID phaseIDLogic, AuxLocRef auxLoc, Slabs slabStats) {
    this.event = event;
    hypo = event.getHypo();
    cratons = auxLoc.getCratons();
    zoneStats = auxLoc.getZoneStats();
    newZoneStats = auxLoc.getNewZoneStats();
    this.slabStats = slabStats;
    this.phaseIDLogic = phaseIDLogic;
    rawRankSumEstimator = event.getRawRankSumEstimator();
    projectedRankSumEstimator = event.getProjectedRankSumEstimator();
    linearStep = new LinearStep(event);
    decorrelator = event.getDecorrelator();
  }
  
  public Stepper() {
  	
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
    lastContribution = bayesianContribution;
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
      bayesianContribution = projectedRankSumEstimator.getContribution();

      LOGGER.fine(
          String.format(
              "Lsrt: ST av chisq = %8.4f %10.4f %10.4f",
              projectedMedian,
              dispersion,
              dispersion - Math.max(bayesianContribution - lastContribution, 0d)));
    } else {
      // Demedian the raw residuals.
      residualsMedian = rawRankSumEstimator.computeMedian();

      rawRankSumEstimator.deMedianResiduals();

      // Demedian the raw design matrix.
      rawRankSumEstimator.deMedianDesignMatrix();

      // Get the rank-sum-estimator dispersion of the raw data.
      dispersion = rawRankSumEstimator.computeDispersionValue();
      bayesianContribution = rawRankSumEstimator.getContribution();

      LOGGER.fine(
          String.format(
              "Lsrt: ST av chisq = %8.4f %10.4f %10.4f",
              residualsMedian,
              dispersion,
              dispersion - Math.max(bayesianContribution - lastContribution, 0d)));
    }

    rSumEstResult = new RSumEstResult(0d, residualsMedian, 0d, dispersion);

    return LocStatus.SUCCESS;
  }

  /**
   * Compute the rank-sum-estimator direction of steepest descents. This has been separated from
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
      updateStepDirection();
      status = LocStatus.PHASEID_CHANGED;
      logStep("ReID", stage, iteration, status);
      return status;
    }

    /**
     * If we're headed down hill, this iteration is done. Note that following slabs along their
     * slope constantly changes the Bayesian condition. This results in a failure to converge that
     * has nothing to do with fitting the residuals. By accounting for the change in the Bayesian
     * contribution to the dispersion, this problem can be side stepped without messing with the
     * rest of the algorithm.
     */
    if (rSumEstResult.getDispersion() - Math.max(bayesianContribution - lastContribution, 0d)
        < hypo.getEstimatorDispersionValue()) {
      // Delay resetting the Bayesian depth for stability reasons.
      hypo.setEstimatorDispersionValue(rSumEstResult.getDispersion());
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
        hypo.setStepLength(0d);
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
      LOGGER.fine(
          String.format("Damping: %d %6.4f", hypo.getNumOfTimesStepLengthDampening(), damp));
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
        updateStepDirection();
        status = LocStatus.PHASEID_CHANGED;
        logStep("ReID", stage, iteration, status);
        return status;
      }

      logStep("Damp", stage, iteration, status);
    } while (rSumEstResult.getDispersion() - Math.max(bayesianContribution - lastContribution, 0d)
        >= hypo.getEstimatorDispersionValue());

    // The step length damping actually worked!
    updateStepDirection();
    return LocStatus.SUCCESS;
  }

  /**
   * This function sets the location environment by determining if the location is in a craton or
   * tectonic area and setting the Bayesian depth.
   */
  protected void setLocEnvironment() {
    // Set the tectonic flag.  Note that everything outside cratons is considered tectonic.
    if (cratons.isCraton(hypo.getLatitude(), hypo.getLongitude())) {
      LocUtil.isTectonic = false;
    } else {
      LocUtil.isTectonic = true;
    }

    LOGGER.fine("Tectonic = " + LocUtil.isTectonic);

    if (!event.getIsDepthManual()) {
      // Update the Bayesian depth if it wasn't set by the analyst.
      ArrayList<BayesianDepth> bayesList = getBayesDepth(hypo.getLatitude(), 
      		hypo.getLongitude(), false);
  		/*
  		 * At this point, we should have a complete set of possible depths (in increasing 
  		 * depth order).  If the Bayesian condition was set up as a sum of Gaussians, we 
  		 * would be done.
  		 */
			LOGGER.fine("Bayesian depths:");
  		for(BayesianDepth bayes : bayesList) {
  			LOGGER.fine("\t" + bayes);
  		}
  		/*
  		 * In the current incarnation, the Locator can only handle one Bayesian 
  		 * condition, so we need to pick one based on the current hypocentral depth.
  		 */
      BayesianDepth bayesDepth = bestBayesDepth(bayesList, hypo.getDepth());
      hypo.updateBayes(bayesDepth.getDepth(), bayesDepth.getSpread());
//    LocUtil.record("\tBayesian depth: " + bayesDepth);
    }
    LOGGER.fine(
        String.format(
            "Bayes: %5.1f %5.3f %b",
            hypo.getBayesianDepth(), hypo.getBayesianDepthWeight(), event.getIsDepthManual()));
  }
	
	/**
	 * Compile a list of all possible Bayesian depths by combining the Slab model and 
	 * the ZoneStats statistics.  There are three depth categories: 1) shallow, 2) 
	 * deeper, but presumably inducing earthquakes on both sides of the crust-mantle 
	 * interface, and 3) clearly deeper.
	 * 
	 * @param latitude Trial hypocenter geographic latitude in degrees
	 * @param longitude Trial hypocenter geographic longitude in degrees
	 * @param debug True if this is a random hypocenter test
	 * @return A list of Bayesian depths and errors in kilometers
	 */
	private ArrayList<BayesianDepth> getBayesDepth(double latitude, double longitude, 
			boolean debug) {
		double deepest;
		BayesianDepth oldZone = null;
		BayesianDepth newZone = null;
		ArrayList<BayesianDepth> slabDepths;
		ArrayList<BayesianDepth> bayesList;
		
		// Create the empty list.
		bayesList = new ArrayList<BayesianDepth>();
		// First add the default shallow zone.
		bayesList.add(new BayesianDepth(LocUtil.DEFAULTDEPTH, LocUtil.DEFAULTDEPTHSE, 
				DepthSource.SHALLOW));
		
		// Get the slab depths.
		slabDepths = slabStats.getDepth(latitude, longitude);
		if(slabDepths != null) {
			for(BayesianDepth slab : slabDepths) {
				if(slab.getDepth() <= LocUtil.SLABMERGEDEPTH) {
					/**
					 * The slab is shallow and there may be vertical faults from the slab to the 
					 * surface, so allow the depth to be anywhere between the deepest slab error 
					 * and the free surface.
					 */
					deepest = slab.getDepth() + 3d * (slab.getUpperBound() - slab.getDepth());
					if(!debug) {
						// The normal earthquake location version.
						bayesList.set(0, new BayesianDepth(deepest / 2d, deepest / 6d, 
								DepthSource.SLABINTERFACE));
					} else {
						// For the random test, we need to be able to untangle the interface depth 
						// to recover the actual slab depth.
						bayesList.set(0, new BayesianDepth(deepest / 2d, slab.getUpperBound() - 
								slab.getDepth(), DepthSource.SLABINTERFACE));
					}
				} else {
					// Set up a deep zone.
					bayesList.add(new BayesianDepth(slab.getDepth(), 
							Math.max(slab.getDepth() - slab.getLowerBound(), slab.getUpperBound() - 
							slab.getDepth()), DepthSource.SLABMODEL));
				}
			}
			// Do new ZoneStats anyway for comparison.  Treat it as a deep zone.
			if(debug) {
				newZone = newZoneStats.interpolateBayesDepth(latitude, longitude);
				if(newZone != null) {
					// See if the deepest new ZoneStats depth is actually deep.
					if(newZone.getUpperBound() >= LocUtil.DEEPESTSHALLOW) {
						// If so, see if we should do a slab merge.
						if(newZone.getUpperBound() <= LocUtil.SLABMERGEDEPTH) {
							// Do a slab merge.
							deepest = newZone.getUpperBound() + 1.5d * LocUtil.DEFAULTSLABSE;
							newZone = new BayesianDepth(deepest / 2d, deepest / 6d, 
									DepthSource.NEWZONEINTERFACE);
						// Otherwise, add a deep zone.
						} else {
							newZone = new BayesianDepth(newZone.getUpperBound(), LocUtil.DEFAULTSLABSE, 
									DepthSource.NEWZONESTATS);
						}
					} else {
						// For comparison purposes, there's no point in a shallow zone.
						newZone = null;
					}
				}
			}
		} else {
			// If there aren't any slab depths, see what we can do with new ZoneStats.
			newZone = newZoneStats.interpolateBayesDepth(latitude, longitude);
			/*
			 * Because new ZoneStats does one Gaussian per grid sample, it can be fooled 
			 * when there are both shallow and deep earthquakes in the same area.  This 
			 * results in a mean between the earthquake zones in depth and an artificially 
			 * large standard error.  To combat this, the mean plus standard error is used 
			 * for depth rather than the mean.  This seems to compare pretty closely with 
			 * the slab model.
			 */
			if(newZone != null) {
				// See if the deepest new ZoneStats depth is actually deep.
				if(newZone.getUpperBound() >= LocUtil.DEEPESTSHALLOW) {
					// If so, see if we should do a slab merge.
					if(newZone.getUpperBound() <= LocUtil.SLABMERGEDEPTH) {
						// Do the slab merge. 
						deepest = newZone.getUpperBound() + 1.5d * LocUtil.DEFAULTSLABSE;
						bayesList.set(0, new BayesianDepth(deepest / 2d, deepest / 6d, 
								DepthSource.NEWZONEINTERFACE));
					// Otherwise, add a new deep zone.
					} else {
						bayesList.add(new BayesianDepth(newZone.getUpperBound(), LocUtil.DEFAULTSLABSE, 
								DepthSource.NEWZONESTATS));
					}
				} else {
					// If the ZoneStats are shallow, replace the default shallow.
					if(newZone.getDepth() < LocUtil.MIDCRUSTDEPTH) {
						bayesList.set(0, new BayesianDepth(newZone.getDepth(), LocUtil.DEFAULTDEPTHSE, 
								DepthSource.NEWZONESHALLOW));
					} else {
						bayesList.set(0, new BayesianDepth(newZone.getDepth(), LocUtil.LOWERCRUSTSE, 
								DepthSource.NEWZONESHALLOW));
					}
				}
				newZone = null;
			}
		}
		
		// For debugging we want to see both the slab, zone and new zone values.
		if(debug) {
			// Show the new ZoneStats value in addition to the slab values.
			if(newZone != null) {
				bayesList.add(newZone);
			}
			// Do old ZoneStats for comparison.
			oldZone = zoneStats.interpolateBayesDepth(latitude, longitude);
			/*
			 * Because old ZoneStats does one mean and range per grid cell, it can be fooled 
			 * when there are both shallow and deep earthquakes in the same area.  This 
			 * results in a mean between the earthquake zones in depth and an artificially 
			 * large standard error.  Oddly, the mean free depth seems to compare better with 
			 * the slab model than the mean plus standard error.
			 */
			if(oldZone != null) {
				// See if the deepest ZoneStats depth is actually deep.
				if(oldZone.getDepth() >= LocUtil.DEEPESTSHALLOW) {
					// If so, see if we should do a slab merge.
					if(oldZone.getDepth() <= LocUtil.SLABMERGEDEPTH) {
						// Add a zone interface entry. 
						deepest = oldZone.getDepth() + 1.5d * LocUtil.DEFAULTSLABSE;
						bayesList.add(new BayesianDepth(deepest / 2d, deepest / 6d, 
								DepthSource.ZONEINTERFACE));
					// Otherwise, add a new deep zone.
					} else {
						oldZone.setSpread(LocUtil.DEFAULTSLABSE);
						bayesList.add(oldZone);
					}
				} else {
					// Add a shallow ZoneStats.
					if(oldZone.getDepth() < LocUtil.MIDCRUSTDEPTH) {
						bayesList.add(new BayesianDepth(oldZone.getDepth(), LocUtil.DEFAULTDEPTHSE, 
								DepthSource.ZONESHALLOW));
					} else {
						bayesList.add(new BayesianDepth(oldZone.getDepth(), LocUtil.LOWERCRUSTSE, 
								DepthSource.ZONESHALLOW));
					}
				}
				oldZone = null;
			} 
		}
		return bayesList;
	}
	
	/**
	 * Deciding which Bayesian depth to use is complicated.  There is always the 
	 * possibility of shallow events.  The possibility of deeper events is based 
	 * on a combination of the slab model and depths from earthquake statistics 
	 * summarized in ZoneStats.  The slab model will be used if there is one.  If 
	 * not, a deep zone will be derived from the mean free earthquake depth in 
	 * ZoneStats provided it is deep enough.  There are four possibilities: 1) 
	 * there is no deep zone (use the shallow depth), 2) the deep zone is close to 
	 * the surface (use a combination of the deep and shallow depth), 3) the trial 
	 * depth is deep (use the deep zone depth), or 4) use the shallow or deep zone 
	 * depth depending on which is closest to the trial depth.
	 * 
	 * @param bayesianDepths List of potential Bayesian depths
	 * @param depth Hypocenter depth in kilometers
	 * @return Best Bayesian depth
	 */
	private BayesianDepth bestBayesDepth(ArrayList<BayesianDepth> bayesList, 
			double depth) {
		double slabDiff = TauUtil.DMAX;
		BayesianDepth slabDepth = null;
		
		if(bayesList.size() > 1) {
			// Find the zone closest to the trial depth.
			for(int j = 1; j < bayesList.size(); j++) {
				if(Math.abs(bayesList.get(j).getDepth() - depth) < slabDiff) {
					slabDepth = bayesList.get(j);
					slabDiff = Math.abs(slabDepth.getDepth() - depth);
				}
			}
			// If the event is clearly not shallow, choose the closest deep zone.
			if(depth > LocUtil.SLABMAXSHALLOWDEPTH) {
				if(slabDepth.getDepth() > LocUtil.DEEPESTSHALLOW) {
					return slabDepth;
				} else {
					return bayesList.get(0);
				}
			// If the event might be shallow, choose the closest zone.
			} else {
				// If the trial depth is closer to the shallow zone, go shallow.
				if(Math.abs(bayesList.get(0).getDepth() - depth) <= slabDiff) {
					return bayesList.get(0);
				// Otherwise, go deep.
				} else {
					return slabDepth;
				}
			}
		} else {
			// If there's only one depth, it will have to do.
			return bayesList.get(0);
		}
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
/*  LocUtil.record(
        String.format(
            "\t%s: %1d %2d %5d %8.4f %8.4f %6.2f del= %5.1f %6.1f " + "rms= %6.2f %s",
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
            status)); */
  }
  
  /**
   * Driver for a test of the ZoneStats/SlabModel subsystem.
   * 
   * @param args Not used
   * @throws ClassNotFoundException If reloading the slab model fails
   * @throws IOException If rereading the slab model fails
   */
  public static void main(String[] args) throws ClassNotFoundException, IOException {
  	Stepper test = new Stepper();
  	test.doTest();
  }
  
  /**
   * Generates random hypocenters to test the ZoneStats/SlabModel subsystem and 
   * compiles statistics.
   * 
   * @throws ClassNotFoundException If reloading the slab model fails
   * @throws IOException If rereading the slab model fails
   */
  private void doTest() throws ClassNotFoundException, IOException {
  	// Number of random trials to run.
		int maxTrials = 100000;
		// We want to look at shallow zones and compare deeper events.
		int defaultShallow = 0, zoneShallow = 0, newZoneShallow = 0, zoneCrust = 0, 
				newZoneCrust = 0, slabCrust = 0, slabOnly = 0, zoneOnly = 0, newZoneOnly = 0, 
				slabNzones = 0, slabNzone = 0, slabNnew = 0, zoneNnew = 0, histIndex;
		double slabDepth, zoneDepth, newZoneDepth, dDepth;
		// The histogram shows a comparison between the slab and zone depths.
		int[] hist = new int[201];
		double lat, lon, depth;
		// The model path is hard wired because all the LocMain input is bypassed.
		String modelPath = "../../LocRun/models/";
		ArrayList<BayesianDepth> bayesList;
		
		// Read in the ZoneStats and Slab model.
		LocSessionLocal locLocal = new LocSessionLocal(modelPath);
		zoneStats = locLocal.getZoneStats();
		newZoneStats = locLocal.getNewZoneStats();
		slabStats = locLocal.getSlabRes("2spd");
		
		// Initialize the histogram.
		for(int j = 0; j < hist.length; j++) {
			hist[j] = 0;
		}
    
		// Run the random trials.
		for(int i = 0; i < maxTrials; i++) {
			// Generate the hypocenter.
			lat = 180d * (Math.random() - 0.5d);
			lon = 360d * (Math.random() - 0.5d);
			depth = 700d * Math.random();
			// Run the Bayesian depth algorithm.
      bayesList = getBayesDepth(lat, lon, true);
      // Log it if there might be something to compare.
      if(bayesList.size() > 1) {
				System.out.format("Random trial %4d: lat = %6.2f lon = %7.2f depth = %6.2f\n", 
						i, lat, lon, depth);
				System.out.println("Bayesian depths:");
				for(BayesianDepth bayes : bayesList) {
					System.out.println("\t" + bayes);
				}
      } else {
      	// If there was only one depth, see where it came from.
      	switch(bayesList.get(0).getSource()) {
      	// The 10 km depth default.
      	case SHALLOW:
      		defaultShallow++;
      		break;
      	// Also shallow, but from ZoneStats.
      	case ZONESHALLOW:
      		zoneShallow++;
      		break;
      	// Also shallow, but from NewZoneStats.
      	case NEWZONESHALLOW:
      		newZoneShallow++;
      		break;
      	// Deeper than 25 km, but still shallowish from ZoneStats.
      	case ZONEINTERFACE:
      		zoneCrust++;
      		break;
        // Deeper than 25 km, but still shallowish from ZoneStats.
      	case NEWZONEINTERFACE:
      		newZoneCrust++;
      		break;
        // Deeper than 25 km, but still shallowish from the Slab model.
      	case SLABINTERFACE:
      		slabCrust++;
      		break;
      	default:
      		System.out.println("Unexpected singleton: " + bayesList.get(0));
      		break;
      	}
      }
      // Compile some statistics.
      slabDepth = -100d;
      zoneDepth = -100d;
      newZoneDepth = -100d;
      for(BayesianDepth bayes : bayesList) {
      	switch(bayes.getSource() ) {
      	case SHALLOW:
      	case ZONESHALLOW:
      	case NEWZONESHALLOW:
      		break;
      	// We have an intermediate depth from the Slab model.
      	case SLABINTERFACE:
      		slabDepth = 2d * bayes.getDepth() - 3d * bayes.getSpread();
      		break;
      	// We have an intermediate depth from ZoneStats.
      	case ZONEINTERFACE:
      		zoneDepth = 2d * bayes.getDepth() - 1.5d * LocUtil.DEFAULTSLABSE;
      		break;
      	case NEWZONEINTERFACE:
      		newZoneDepth = 2d * bayes.getDepth() - 1.5d * LocUtil.DEFAULTSLABSE;
      		break;
      	// We have a deep depth from the Slab model.
      	case SLABMODEL:
      		slabDepth = bayes.getDepth();
      		break;
      	// We have a deep depth from ZoneStats.
      	case ZONESTATS:
      		zoneDepth = bayes.getDepth();
      		break;
      	case NEWZONESTATS:
      		newZoneDepth = bayes.getDepth();
      		break;
      	default:
        	System.out.println("\tWEIRD: unknown depth source = " + bayes);
      		break;
      	}
      }
      // Do some ZoneStats vs SlabModel comparisons.
    	if(slabDepth >= 0d) {
    		if(newZoneDepth >= 0d) {
    			// We have both a slab and a new ZoneStats.  Add to the histogram.
    			if(zoneDepth >= 0) {
    				slabNzones++;
    			} else {
    				slabNnew++;
    			}
    			dDepth = slabDepth - newZoneDepth;
    			histIndex = Math.min(Math.max((int) (dDepth + 100.5d), 0), 200);
  //  		System.out.format("\tIndex: %6.2f => %d\n", dDepth, histIndex);
    			hist[histIndex]++;
    		} else if(zoneDepth >= 0d) {
    			slabNzone++;
    		} else {
    			// We have a Slab model depth, but no comparable ZoneStats depth.
    			slabOnly++;
    		}
    	} else if(newZoneDepth > 0d) {
    		if(zoneDepth >= 0d) {
    			zoneNnew++;
    		} else {
    			newZoneOnly++;
    		}
    	} else if(zoneDepth > 0d) {
    		// We have a NewZoneStats depth, but no comparable Slab model depth.
    		zoneOnly++;
    	}
		}
		// Dump the statistics:
		System.out.format("\nShallow: default = %d zone = %d new = %d\nCrust: zone = %d " + 
				"new = %d slab = %d\nDeep: slab = %d new = %d zone = %d all = %d slab&new = %d " + 
				"slab&zone = %d zone&new = %d\n", defaultShallow, zoneShallow, newZoneShallow, 
				zoneCrust, newZoneCrust, slabCrust, slabOnly, newZoneOnly, zoneOnly, slabNzones, 
				slabNnew, slabNzone, zoneNnew);
		System.out.println("Histogram:");
		for(int j = 0; j < hist.length; j++) {
			System.out.format(" %5d", hist[j]);
			if(j > 0 && j % 20 == 0) {
				System.out.println();
			}
		}
  }
}
