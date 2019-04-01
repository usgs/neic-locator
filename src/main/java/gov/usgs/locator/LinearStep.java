package gov.usgs.locator;

import java.util.ArrayList;

/**
 * The LinearStep class simplifies travel time computations. Computing seismic 
 * travel times is expensive, so instead set the steepest descent step length 
 * using the linear term of a Taylor series.  The residual estimates and hence 
 * the estimated penalty function value will be accurate as long as the 
 * curvature of the Earth is negligible within the step length (and, of course, 
 * the seismic phases still exist at the new epicentral distance and 
 * hypocentral depth).
 * 
 * @author Ray Buland
 *
 */
public class LinearStep {
  /**
   * A double[] containing the spatial steepest descents direction unit vector.
   */
  private double[] stepDirectionUnitVector;
  
  /**
   * A double[] containing the trial spatial vector in kilometers.
   */
  private double[] trialVector;

  /**
   * An Event object containing the event to use when calculating the linear 
   * step.
   */
  private Event event;

  /**
   * A Hypocenter object containing the hypocenter of the event to use when 
   * calculating the linear step.
   */  
  private Hypocenter hypo;
    
  /** 
   * An ArrayList of Wresidual objects containing the raw (sorted) weighted 
   * residuals of the picks.
   */
  private ArrayList<Wresidual> rawWeightedResiduals;

  /**
   * A RankSumEstimator object used for the rank-sum estimation of the raw picks.
   */
  private RankSumEstimator rawRankSumEstimator;

  /**
   * A RankSumEstimator object used for the rank-sum estimation of the projected
   * picks.
   */
  private RankSumEstimator projectedRankSumEstimator;
  
  /**
   * A Decorrelator object used when decorrelating the event picks.
   */
  private Decorrelator decorrelator;
  
  /**
   * The LinearStep constructor. This constructor sets the event (and thus the 
   * hypo, residuals, rank sum estimators, and decorrelator) to the provided 
   * value.
   * 
   * @param event Event information
   */
  public LinearStep(Event event) {
    this.event = event;
    hypo = event.getHypo();
    rawWeightedResiduals = event.getRawWeightedResiduals();
    rawRankSumEstimator = event.getRawRankSumEstimator();
    projectedRankSumEstimator = event.getProjectedRankSumEstimator();
    decorrelator = event.getDecorrelator();
  }
  
  /**
   * This function determines the step length in the steepest descents step 
   * direction that results in the minimum dispersion based on linear estimates 
   * of the travel-time residuals.  This algorithm is a messy bisection.
   * 
   * @param stepDirectionUnitVector A double[] containing the spatial steepest 
   *                                descents direction unit vector
   * @param startStepLen A double containing the starting step length in 
   *                     kilometers
   * @param minStepLen A double containing the minimum step length. If the step 
   *                   length is less than the minimum the current estimate is 
   *                   good enough
   * @param maxStepLen A double containing the maximum step length. If the final 
   *                   step length is greater than the maximum, give up
   * @param currentDispersionValue A double containing the current R-estimator 
   *                               dispersion value
   * @return A RestResult containing the step length in kilometers needed to  
   *         reach the minimum dispersion
   * @throws Exception If the bisection doesn't make sense
   */
  public RestResult stepLength(double[] stepDirectionUnitVector, 
      double startStepLen, double minStepLen, double maxStepLen, 
      double currentDispersionValue) throws Exception {
    if (LocUtil.deBugLevel > 0) {
      System.out.println();
    }
    
    // The trial vector has to be the same size as the step vector.
    trialVector = new double[stepDirectionUnitVector.length];
    
    // Initialize the bisection.
    this.stepDirectionUnitVector = stepDirectionUnitVector; 
    double initialStepLen = startStepLen;
    double currentStepLen = startStepLen;

    // Make a default step.
    RestResult[] sample = new RestResult[3]; 
    sample[0] = new RestResult(0d, 0d, 0d, currentDispersionValue);
    sample[1] = estDispersionValue(currentStepLen);
    
    // Do some preliminary hunting to surround the minimum.
    if (sample[0].chiSq >= sample[1].chiSq) {
      // If the trial step was too short, keep doubling it.
      currentStepLen = 2d * currentStepLen;
      sample[2] = estDispersionValue(currentStepLen);

      // Keep stepping until the penalty gets bigger.
      while (sample[1].chiSq >= sample[2].chiSq) {
        // If we've gone too far, return what we've got.
        if (sample[2].stepLen >= maxStepLen) {
          createTrialStepVector(sample[2].stepLen);

          if (LocUtil.deBugLevel > 0) {
            System.out.format("Lintry: x dsp = %6.2f %9.4f %5.2f\n", 
                sample[2].stepLen, sample[2].chiSq, sample[2].median);
          }
          return sample[2];
        }

        sample[0] = sample[1];
        sample[1] = sample[2];
        
        // The steps keep getting bigger.
        initialStepLen *= 2d;
        currentStepLen = Math.min(sample[1].stepLen + initialStepLen, 
            maxStepLen);
        sample[2] = estDispersionValue(currentStepLen);
      }
    } else {
      // If trial step was too long, find a lower limit.
      do {
        sample[2] = sample[1];
        currentStepLen = 0.5 * (sample[0].stepLen + sample[2].stepLen);
        sample[1] = estDispersionValue(currentStepLen);

        // See if we've converged.
        if (sample[1].stepLen <= minStepLen) {
          if (sample[1].chiSq >= sample[0].chiSq) {
            sample[1] = new RestResult(0d, 0d, 0d, currentDispersionValue);
          }

          createTrialStepVector(sample[1].stepLen);

          if (LocUtil.deBugLevel > 0) {
            System.out.format("Lintry: x dsp = %7.3f %9.4f %5.2f\n", 
                sample[1].stepLen, sample[1].chiSq, sample[1].median);
          }
          return sample[1];
        }
      } while (sample[0].chiSq < sample[1].chiSq);
    }
    
    // Now we can start homing in from both sides.
    while ((sample[2].stepLen - sample[0].stepLen) / sample[1].stepLen > 0.15d 
        && sample[2].stepLen - sample[0].stepLen > minStepLen) {
      // Try the lower half first.
      currentStepLen = 0.5 * (sample[0].stepLen + sample[1].stepLen);
      RestResult testSample = estDispersionValue(currentStepLen);

      if (testSample.chiSq >= sample[1].chiSq) {
        // That didn't work, try the upper half.
        sample[0] = testSample;
        currentStepLen = 0.5 * (sample[1].stepLen + sample[2].stepLen);
        testSample = estDispersionValue(currentStepLen);
      
        if (testSample.chiSq >= sample[1].chiSq) {
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
    createTrialStepVector(sample[1].stepLen);

    if (LocUtil.deBugLevel > 0) {
      System.out.format("Lintry: x dsp = %7.3f %9.4f %5.2f\n", 
          sample[1].stepLen, sample[1].chiSq, sample[1].median);
    }

    return sample[1];
  }
  
  /**
   * This function gets the final trial vector.  The current hypocenter will be 
   * moved by this much for this iteration.
   * 
   * @return A double[] containing the trial spatial vector in kilometers
   */
  public double[] getTrialVector() {
    return trialVector;
  }
  
  /**
   * This function calculates the estimated dispersion at the trial step length 
   * based on linear estimates of the travel-time residuals.
   * 
   * @param currentStepLen A double containing the current Trial step length in 
   *                       kilometers
   * @return A RestResult object containing the results of the R-estimator 
   *         algorithm
   */
  private RestResult estDispersionValue(double currentStepLen) {
    // Do the initial pass to project and demean the correlated residuals.
    createTrialStepVector(currentStepLen);
    for (int j = 0; j < rawWeightedResiduals.size(); j++) {
      rawWeightedResiduals.get(j).updateEst(trialVector);
    }

    double median = rawRankSumEstimator.computeLinEstMedian();
    rawRankSumEstimator.deMedianEstResiduals();
    double dispRaw = rawRankSumEstimator.computeEstDispersionValue();
    
    // Finish up.
    if (LocUtil.useDecorrelation) {
      // If we're decorrelating, we have more to do.
      decorrelator.projectEstimatedPicks();
      projectedRankSumEstimator.computeLinEstMedian();
      double dispProj = projectedRankSumEstimator.computeEstDispersionValue();
    
      if (LocUtil.deBugLevel > 0) {
        System.out.format("Estlin: x dsp = %7.3f %9.4f %9.4f %5.2f\n", 
            currentStepLen, dispProj, dispRaw, median);
      }

      return new RestResult(currentStepLen, median, 0d, dispProj);
    } else {
      // Otherwise, we're pretty much done.
      if (LocUtil.deBugLevel > 0) {
        System.out.format("Estlin: x dsp = %7.3f %9.4f %5.2f\n", 
            currentStepLen, dispRaw, median);
      }

      return new RestResult(currentStepLen, median, 0d, dispRaw);
    }
  }
  
  /**
   * This function creates a trial step vector in the steepest descent direction 
   * with the length of stepLen. It also ensures that the depth is still in a 
   * valid earthquake depth range.
   * 
   * @param currentStepLen A double containing the length of the current trial 
   *                       step in kilometers.
   */
  private void createTrialStepVector(double currentStepLen) {
    // Make the trial step vector.
    for (int j = 0; j < stepDirectionUnitVector.length; j++) {
      trialVector[j] = currentStepLen * stepDirectionUnitVector[j];
    }

    // Make sure the depth is OK.
    if (hypo.getDegreesOfFreedom() > 2) {
      if (hypo.getDepth() + trialVector[2] < LocUtil.DEPTHMIN) {
        // Trap air quakes.
        trialVector[2] = LocUtil.DEPTHMIN - hypo.getDepth();
      } else if (hypo.getDepth() + trialVector[2] > LocUtil.DEPTHMAX) {
        // Trap lower mantle quakes.
        trialVector[2] = LocUtil.DEPTHMAX - hypo.getDepth();
      }
    }
  }
}
