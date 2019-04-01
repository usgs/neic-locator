package gov.usgs.locator;

import java.util.ArrayList;

/**
 * The RankSumEstimator class is the Rank-sum estimator implementation for the
 * NEIC Locator.
 * 
 * @author Ray Buland
 *
 */
public class RankSumEstimator {
  /** 
   * An int containing the last index of the weightedResiduals array, cached for 
   * use in dispersion().
   */
  private int lastIndex;

  /**
   * An int containing the length of the weightedResiduals ArrayList, cached for 
   * use in deMedianDesignMatrix().
   */
  private int weightedResidualsLength;

  /** 
   * An int containing the half index of the weightedResiduals array, cached for 
   * use in deMedianDesignMatrix().
   */
  private int halfIndex;

  /* A double containing the median of the residuals stored in the 
   * weightedResiduals array, cached for use in spread(), deMedianResiduals() and 
   * computeDispersionValue().
   */
  private double residualsMedian;

  /* A double containing the median of the linear estimates of the residuals 
   * stored in the weightedResiduals array, cached for use in 
   * deMedianEstResiduals() and computeEstDispersionValue().
   */  
  private double linearEstimatesMedian;

  /** 
   * A double[] holding the interpolation scores used in dispersion(), 
   * compSteepestDescDir(), and makeScores().
   */
  private double[] scores;

  /** 
   * An ArrayList of WeightedResidual objects containing the weighted residuals of the 
   * picks.
   */  
  private ArrayList<WeightedResidual> weightedResiduals;
  
  /**
   * The RankSumEstimator constructor. Initializes various cached variables, 
   * and stores the weighted residuals.
   * 
   * @param weightedResiduals An ArrayList of WeightedResidual objects containing the 
   *                           weighted residuals of the picks.
   */
  public RankSumEstimator(ArrayList<WeightedResidual> weightedResiduals) {
    lastIndex = -1;
    halfIndex = -1;
    weightedResidualsLength = -1;
    residualsMedian = 0d;
    linearEstimatesMedian = 0d;
    this.weightedResiduals = weightedResiduals;
  }
  
  /**
   * This function computes the median of the travel-time residuals sored in 
   * weightedResiduals (excluding the Bayesian depth residual, of course).
   * 
   * @return A double containing the median of travel-time residuals stored in
   *          weightedResiduals.
   */
  public double computeMedian() {
    // Make sure we have enough data to do something.
    if (weightedResiduals.size() < 2) {
      // If there's no data or only a depth constraint return zero.
      if (weightedResiduals.size() == 0) {
        return 0d;
      }  else if (weightedResiduals.size() == 1 
          && weightedResiduals.get(0).isDepth) {
        return 0d;
      }
    }
    
    // Set up for the median.
    for (int j = 0; j < weightedResiduals.size(); j++) {
      weightedResiduals.get(j).sortRes();
    }

    // Sort the travel-time residuals.
    weightedResiduals.sort(null);
    
    // Do the median.
    weightedResidualsLength = weightedResiduals.size() - 1;
    halfIndex = weightedResidualsLength / 2;

    if (weightedResidualsLength % 2 == 0) {
      residualsMedian = 0.5d * (weightedResiduals.get(halfIndex - 1).residual
          + weightedResiduals.get(halfIndex).residual);
      return residualsMedian;
    } else {
      residualsMedian = weightedResiduals.get(halfIndex).residual;
      return residualsMedian;
    }
  }
  
  /**
   * This function computes the normalized median absolute deviation or spread 
   * of the travel-time residuals (excluding the Bayesian depth residual).  Note 
   * that the median is remembered from the median method.
   * 
   * @return A double containing the spread of travel-time residuals stored in
   *          weightedResiduals.
   */
  public double computeSpread() {
    // Trap insufficient data.
    if (weightedResiduals.size() < 2) {
      return 0d;
    }
    
    // Set up for the spread.
    for (int j = 0; j < weightedResiduals.size(); j++) {
      weightedResiduals.get(j).sortSpread(residualsMedian);
    }
    weightedResiduals.sort(null);
    
    // Do the median of the demeaned absolute residuals.
    weightedResidualsLength = weightedResiduals.size() - 1;
    halfIndex = weightedResidualsLength / 2;
    if (weightedResidualsLength % 2 == 0) {
      return LocUtil.MADNORM * 0.5d 
          * (weightedResiduals.get(halfIndex - 1).sortValue
          + weightedResiduals.get(halfIndex).sortValue);
    } else {
      return LocUtil.MADNORM * weightedResiduals.get(halfIndex).sortValue;
    }
  }
  
  /**
   * This function removes the median from the residuals.  Note that the median 
   * is remembered from the median method.
   */
  public void deMedianResiduals() {
    for (int j = 0; j < weightedResiduals.size(); j++) {
      weightedResiduals.get(j).deMedianRes(residualsMedian);
    }

    // Since we've already demedianed, we don't need to do it again 
    // for the computeDispersionValue function.
    residualsMedian = 0d;
  }
  
  /**
   * This function removes the medians from each row of the design matrix.  
   * Note that the positions of the median values corresponds to the positions 
   * for the residual, not their design values.  Obviously, the median 
   * method must be invoked before deMedianDesignMatrix to determine these 
   * positions and no other method that re-sorts the residuals (i.e., 
   * spread and penalty) may be invoked in between.
   */
  public void deMedianDesignMatrix() {
    double[] medianValues = new double[3];
    
    // Set up the medians.
    if (weightedResidualsLength % 2 == 0) {
      for (int i = 0; i < medianValues.length; i++) {
        medianValues[i] = 0.5d * (weightedResiduals.get(halfIndex - 1).deriv[i]
            + weightedResiduals.get(halfIndex).deriv[i]);
      }
    } else {
      for (int i = 0; i < medianValues.length; i++) {
        medianValues[i] = weightedResiduals.get(halfIndex).deriv[i];
      }
    }

    // Remove the median values from the matrix.
    for (int j = 0; j < weightedResiduals.size(); j++) {
      weightedResiduals.get(j).deMedianDeriv(medianValues);
    }
  }
  
  /**
   * This function computes the rank-sum estimator penalty function or 
   * dispersion.  Note that the median is remembered from the median method.
   * 
   * @return A double containing the the rank-sum estimator dispersion / penalty 
   *          function
   */
  public double computeDispersionValue() {
    // Trap insufficient data.
    if (weightedResiduals.size() < 2) {
      return 0d;
    }
    
    // Set up the computeDispersionValue.
    for (int j = 0; j < weightedResiduals.size(); j++) {
      weightedResiduals.get(j).sortDisp(residualsMedian);
    }

    weightedResiduals.sort(null);

    // Compute the dispersion function.
    return computeWRDispersion();
  }
  
  /**
   * This function computes the dispersion of the current set of weighted 
   * residuals.  The dispersion is a convenient summary of the current data fit.
   * 
   * @return A double containing the the dispersion / penalty function of the 
   *         current set of weighted residuals
   */
  private double computeWRDispersion() {
    double dispersion = 0d;
    
    // Interpolate the scores.
    if (weightedResiduals.size() != lastIndex) {
      lastIndex = weightedResiduals.size();
      makeScores(lastIndex);
    }

    // The dispersion is just a dot product.
    for (int j = 0; j < lastIndex; j++) {
      dispersion += scores[j] * weightedResiduals.get(j).sortValue;
    }

    return dispersion;
  }
  
  /**
   * This function computes the steepest descents direction.  This depends on 
   * the weightedResiduals sort order created the computeDispersionValue method, 
   * so median and spread methods should not be invoked between.
   * 
   * @param degreesOfFreedom Number of degrees of freedom (degreesOfFreedom = 2 
   *                          for fixed depth, degreesOfFreedom = 3 for free 
   *                          depth)
   * @return A double[] containing the the steepest descents direction unit 
   *          vector
   */
  public double[] compSteepestDescDir(int degreesOfFreedom) {
    // Initialize the step direction unit vector.
    double[] stepUnitVector = new double[degreesOfFreedom];
    for (int j = 0; j < degreesOfFreedom; j++) {
      stepUnitVector[j] = 0d;
    }

    // The step direction is the sum of weighted, demedianed derivatives.  
    // We have to process the weighted residuals in sort order for the 
    // scores to get the right direction.
    for (int j = 0; j < weightedResiduals.size(); j++) {
      WeightedResidual weightedResidual = weightedResiduals.get(j);

      for (int i = 0; i < degreesOfFreedom; i++) {
        stepUnitVector[i] += scores[j] * weightedResidual.weight
            * weightedResidual.deDeriv[i];
      }
    }

    return LocUtil.normalizeUnitVector(stepUnitVector);
  }
  
  /**
   * This function computes the median of the linear estimates of the travel-time 
   * residuals (excluding the Bayesian depth residual).
   * 
   * @return A double containing the median of estimated travel-time residuals
   */
  public double computeLinEstMedian() {
    // Make sure we have enough data to do something.
    if (weightedResiduals.size() < 2) {
      // If there's no data or only a depth constraint return zero.
      if (weightedResiduals.size() == 0) {
        return 0d;
      }  else if (weightedResiduals.size() == 1 
          && weightedResiduals.get(0).isDepth) {
        return 0d;
      }
    }
    
    // Set up for the median.
    weightedResidualsLength = 0;
    for (int j = 0; j < weightedResiduals.size(); j++) {
      weightedResiduals.get(j).sortEst();
    }

    // Sort the estimated travel-time residuals.
    weightedResiduals.sort(null);
    
    // Do the median.
    weightedResidualsLength = weightedResiduals.size() - 1;
    halfIndex = weightedResidualsLength / 2;
    
    if (weightedResidualsLength % 2 == 0) {
      linearEstimatesMedian = 0.5d 
          * (weightedResiduals.get(halfIndex - 1).estResidual
          + weightedResiduals.get(halfIndex).estResidual);
      return linearEstimatesMedian;
    } else {
      linearEstimatesMedian = weightedResiduals.get(halfIndex).estResidual;
      return linearEstimatesMedian;
    }
  }
  
  /**
   * This function removes the median from the estimated residuals.
   */
  public void deMedianEstResiduals() {
    for (int j = 0; j < weightedResiduals.size(); j++) {
      weightedResiduals.get(j).deMedianEst(linearEstimatesMedian);
    }

    // Since we've already demedianed, we don't need to do it again 
    // for the computeDispersionValue function.
    linearEstimatesMedian = 0d;
  }
  
  /**
   * This function computes the dispersion of the rank-sum estimator.
   * 
   * @return A double containing the dispersion / penalty function of the 
   *         rank-sum estimator 
   */
  public double computeEstDispersionValue() {
    // Trap insufficient data.
    if (weightedResiduals.size() < 2) {
      return 0d;
    }
    
    // Set up the penalty.
    for (int j = 0; j < weightedResiduals.size(); j++) {
      weightedResiduals.get(j).sortEstDisp(linearEstimatesMedian);
    }

    weightedResiduals.sort(null);

    // Compute the penalty function.
    return computeWRDispersion();
  }

  /**
   * This funtion interpolates the score generating function onto the data 
   * available.
   * 
   * @param numData An int containing number of data we need scores for
   */
  private void makeScores(int numData) {
    // Piecewise linear interpolation of optimal non-decreasing score 
    // generating function created by Steve Heustis using a simplex 
    // algorithm.  Note that the theoretically optimal weights depend 
    // only on the travel-time residual distribution.  However, these turn 
    // out to be non-monotonic which, in practice, cause convergence 
    // problems.
    final double[] scoreGenP = {0d, 0.1375d, 0.1625d, 0.1875d, 0.2125d, 
        0.2375d, 0.2625d, 0.2875d, 0.3125d, 0.3375d, 0.3625d, 0.3875d, 
        0.4125d, 0.4375d, 0.4625d, 0.4875d, 0.5125d, 0.5375d, 0.5625d, 
        0.5875d, 0.6125d, 0.6375d, 0.6625d, 0.6875d, 0.7125d, 0.7375d, 
        0.7625d, 0.7875d, 1d};
    final double[] scoreGenF = {0.0775d, 0.0775d, 0.1546d, 0.5328d, 0.8679d, 
        1.1714d, 1.4542d, 1.7266d, 1.9987d, 2.2802d, 2.5803d, 2.9068d, 
        3.2657d, 3.6603d, 4.0912d, 4.5554d, 5.0470d, 5.5572d, 6.0754d, 
        6.5906d, 7.0919d, 7.5702d, 8.0194d, 8.4365d, 8.8223d, 9.1812d, 
        9.5207d, 9.5974d, 9.5974d};
    
    // Set up the interpolation.
    lastIndex = numData;
    double p = 0d;
    double dp = 1d / (numData + 1);
    double average = 0d;
    int k = 1;

    // Create an array to store the scores.
    scores = new double[numData];
    
    // Do the interpolation
    for (int j = 0; j < numData; j++) {
      p += dp;

      while (p > scoreGenP[k]) {
        k++;
      }

      scores[j] = (p - scoreGenP[k - 1]) * (scoreGenF[k] - scoreGenF[k - 1])
          / (scoreGenP[k] - scoreGenP[k - 1]) + scoreGenF[k - 1];
      average += scores[j];
    }
    
    // Give the scores a zero mean.
    average /= numData;
    for (int j = 0; j < numData; j++) {
      scores[j] -= average;
    }
    
    // The optimal non-decreasing scores, like the travel-time residual 
    // distribution, turn out to be asymmetric.  Unfortunately, this causes 
    // problems for the projected decorrelated data.  Forcing the scores 
    // to be symmetric is a quick and dirty fix for this vexed problem.
    for (int j = 0; j < numData / 2; j++) {
      scores[j] = 0.5d * (scores[j] - scores[numData - j - 1]);
      scores[numData - j - 1] = -scores[j];
    }

    if (numData % 2 > 0) {
      scores[numData / 2] = 0d;
    }
  }
}