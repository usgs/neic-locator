package gov.usgs.locator;

import gov.usgs.locaux.LocUtil;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The LinearStep class simplifies travel time computations. Computing seismic travel times is
 * expensive, so instead set the steepest descent step length using the linear term of a Taylor
 * series. The residual estimates and hence the estimated penalty function value will be accurate as
 * long as the curvature of the Earth is negligible within the step length (and, of course, the
 * seismic phases still exist at the new epicentral distance and hypocentral depth).
 *
 * @author Ray Buland
 */
public class LinearStep {
  /** A double[] containing the spatial steepest descents direction unit vector. */
  private double[] stepDirectionUnitVector;

  /** A double[] containing the trial step vector in kilometers. */
  private double[] trialStepVector;

  /**
   * A Hypocenter object containing the hypocenter of the event to use when calculating the linear
   * step.
   */
  private Hypocenter hypo;

  /**
   * An ArrayList of WeightedResidual objects containing the raw (sorted) weighted residuals of the
   * picks.
   */
  private ArrayList<WeightedResidual> rawWeightedResiduals;

  /** A RankSumEstimator object used for the rank-sum estimation of the raw picks. */
  private RankSumEstimator rawRankSumEstimator;

  /** A RankSumEstimator object used for the rank-sum estimation of the projected picks. */
  private RankSumEstimator projectedRankSumEstimator;

  /** A Decorrelator object used when decorrelating the event picks. */
  private Decorrelator decorrelator;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(LinearStep.class.getName());

  /**
   * The LinearStep constructor. This constructor sets the event (and thus the hypo, residuals, rank
   * sum estimators, and decorrelator) to the provided value.
   *
   * @param event Event information
   */
  public LinearStep(Event event) {
    hypo = event.getHypo();
    rawWeightedResiduals = event.getRawWeightedResiduals();
    rawRankSumEstimator = event.getRawRankSumEstimator();
    projectedRankSumEstimator = event.getProjectedRankSumEstimator();
    decorrelator = event.getDecorrelator();
  }

  /**
   * This function determines the step length in the steepest descents step direction that results
   * in the minimum dispersion based on linear estimates of the travel-time residuals. This
   * algorithm is a messy bisection.
   *
   * @param stepDirectionUnitVector A double[] containing the spatial steepest descents direction
   *     unit vector
   * @param startStepLen A double containing the starting step length in kilometers
   * @param minStepLen A double containing the minimum step length. If the step length is less than
   *     the minimum the current estimate is good enough
   * @param maxStepLen A double containing the maximum step length. If the final step length is
   *     greater than the maximum, give up
   * @param currentDispersionValue A double containing the current R-estimator dispersion value
   * @return A RSumEstResult containing the step length in kilometers needed to reach the minimum
   *     dispersion
   */
  public RSumEstResult stepLength(
      double[] stepDirectionUnitVector,
      double startStepLen,
      double minStepLen,
      double maxStepLen,
      double currentDispersionValue) {
    // The trial vector has to be the same size as the step vector.
    trialStepVector = new double[stepDirectionUnitVector.length];

    // Initialize the bisection.
    this.stepDirectionUnitVector = stepDirectionUnitVector;
    double initialStepLen = startStepLen;
    double currentStepLen = startStepLen;

    // Make a default step.
    RSumEstResult[] sample = new RSumEstResult[3];
    sample[0] = new RSumEstResult(0d, 0d, 0d, currentDispersionValue);
    //  sample[0] = estDispersionValue(0d);
    sample[1] = estDispersionValue(currentStepLen);

    // Do some preliminary hunting to surround the minimum.
    if (sample[0].getDispersion() >= sample[1].getDispersion()) {
      // If the trial step was too short, keep doubling it.
      currentStepLen = 2d * currentStepLen;
      sample[2] = estDispersionValue(currentStepLen);

      // Keep stepping until the penalty gets bigger.
      while (sample[1].getDispersion() >= sample[2].getDispersion()) {
        // If we've gone too far, return what we've got.
        if (sample[2].getStepLength() >= maxStepLen) {
          createTrialStepVector(sample[2].getStepLength());

          LOGGER.fine(
              String.format(
                  "Lintry: x dsp = %6.2f %9.4f %5.2f",
                  sample[2].getStepLength(),
                  sample[2].getDispersion(),
                  sample[2].getMedianResidual()));

          return sample[2];
        }

        sample[0] = sample[1];
        sample[1] = sample[2];

        // The steps keep getting bigger.
        initialStepLen *= 2d;
        currentStepLen = Math.min(sample[1].getStepLength() + initialStepLen, maxStepLen);
        sample[2] = estDispersionValue(currentStepLen);
      }
    } else {
      // If trial step was too long, find a lower limit.
      do {
        sample[2] = sample[1];
        currentStepLen = 0.5 * (sample[0].getStepLength() + sample[2].getStepLength());
        sample[1] = estDispersionValue(currentStepLen);

        // See if we've converged.
        if (sample[1].getStepLength() <= minStepLen) {
          if (sample[1].getDispersion() >= sample[0].getDispersion()) {
            sample[1] = new RSumEstResult(0d, 0d, 0d, currentDispersionValue);
          }

          createTrialStepVector(sample[1].getStepLength());

          LOGGER.fine(
              String.format(
                  "Lintry: x dsp = %7.3f %9.4f %5.2f",
                  sample[1].getStepLength(),
                  sample[1].getDispersion(),
                  sample[1].getMedianResidual()));

          return sample[1];
        }
      } while (sample[0].getDispersion() < sample[1].getDispersion());
    }

    // Now we can start homing in from both sides.
    while ((sample[2].getStepLength() - sample[0].getStepLength()) / sample[1].getStepLength()
            > 0.15d
        && sample[2].getStepLength() - sample[0].getStepLength() > minStepLen) {
      // Try the lower half first.
      currentStepLen = 0.5 * (sample[0].getStepLength() + sample[1].getStepLength());
      RSumEstResult testSample = estDispersionValue(currentStepLen);

      if (testSample.getDispersion() >= sample[1].getDispersion()) {
        // That didn't work, try the upper half.
        sample[0] = testSample;
        currentStepLen = 0.5 * (sample[1].getStepLength() + sample[2].getStepLength());
        testSample = estDispersionValue(currentStepLen);

        if (testSample.getDispersion() >= sample[1].getDispersion()) {
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
    createTrialStepVector(sample[1].getStepLength());

    LOGGER.fine(
        String.format(
            "Lintry: x dsp = %7.3f %9.4f %5.2f",
            sample[1].getStepLength(), sample[1].getDispersion(), sample[1].getMedianResidual()));

    return sample[1];
  }

  /**
   * This function gets the final trial step vector. The current hypocenter will be moved by this
   * much for this iteration.
   *
   * @return A double[] containing the trial step vector in kilometers
   */
  public double[] getTrialStepVector() {
    return trialStepVector;
  }

  /**
   * This function calculates the estimated dispersion at the trial step length based on linear
   * estimates of the travel-time residuals.
   *
   * @param currentStepLen A double containing the current Trial step length in kilometers
   * @return A RSumEstResult object containing the results of the R-estimator algorithm
   */
  private RSumEstResult estDispersionValue(double currentStepLen) {
    // Do the initial pass to project and demean the correlated residuals.
    createTrialStepVector(currentStepLen);
    for (int j = 0; j < rawWeightedResiduals.size(); j++) {
      rawWeightedResiduals.get(j).updateEstResiduals(trialStepVector);
      rawWeightedResiduals.get(j).updateEstWeights(trialStepVector); // Added 9/16/19.
    }

    double median = rawRankSumEstimator.computeLinEstMedian();
    rawRankSumEstimator.deMedianEstResiduals();
    double dispRaw =
        rawRankSumEstimator.computeEstDispersionValue(true); // Went live with true 9/16/19.

    // Finish up.
    if (LocUtil.useDecorrelation) {
      // If we're decorrelating, we have more to do.
      decorrelator.projectEstimatedPicks();
      projectedRankSumEstimator.computeLinEstMedian();
      double dispProj =
          projectedRankSumEstimator.computeEstDispersionValue(true); // Went live with true 9/16/19.

      LOGGER.fine(
          String.format(
              "Estlin: x dsp = %7.3f %9.4f %9.4f %5.2f",
              currentStepLen, dispProj, dispRaw, median));

      return new RSumEstResult(currentStepLen, median, 0d, dispProj);
    } else {
      // Otherwise, we're pretty much done.
      LOGGER.fine(
          String.format("Estlin: x dsp = %7.3f %9.4f %5.2f", currentStepLen, dispRaw, median));

      return new RSumEstResult(currentStepLen, median, 0d, dispRaw);
    }
  }

  /**
   * This function creates a trial step vector in the steepest descent direction with the length of
   * stepLen. It also ensures that the depth is still in a valid earthquake depth range.
   *
   * @param currentStepLen A double containing the length of the current trial step in kilometers.
   */
  private void createTrialStepVector(double currentStepLen) {
    // Make the trial step vector.
    for (int j = 0; j < stepDirectionUnitVector.length; j++) {
      trialStepVector[j] = currentStepLen * stepDirectionUnitVector[j];
    }

    // Make sure the depth is OK.
    if (hypo.getDegreesOfFreedom() > 2) {
      if (hypo.getDepth() + trialStepVector[2] < LocUtil.DEPTHMIN) {
        // Trap air quakes.
        trialStepVector[2] = LocUtil.DEPTHMIN - hypo.getDepth();
      } else if (hypo.getDepth() + trialStepVector[2] > LocUtil.DEPTHMAX) {
        // Trap lower mantle quakes.
        trialStepVector[2] = LocUtil.DEPTHMAX - hypo.getDepth();
      }
    }
  }
}
