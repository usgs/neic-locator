package gov.usgs.locator;

import gov.usgs.traveltime.TauUtil;
import java.util.Arrays;

/**
 * The WeightedResidual class combines the residuals and weights for picks and the Bayesian depth,
 * while providing storage for the dispersion calculation and the spatial derivatives. Note that the
 * residuals and weights are kept separate as some calculations depend only on the residuals.
 *
 * @author Ray Buland
 */
public class WeightedResidual implements Comparable<WeightedResidual> {
  /** A boolean flag indicating whether this WeightedResidual is the Bayesian depth residual. */
  private boolean isBayesianDepth;

  /** A double containing the residual. In seconds if for picks, in kilometers if for depth. */
  private double residual;

  /** A double containing the linearly estimated residual. */
  private double linEstResidual;

  /** A double containing the weight. */
  private double weight;

  /** A double containing the linearly estimated weight. */
  private double linEstWeight;

  /** A double[] containing the spatial derivatives of travel time in seconds/kilometer. */
  private double[] spatialDerivatives;

  /**
   * A double[] containing the demedianed spatial derivatives of travel time in seconds/kilometer.
   */
  private double[] deMedSpaDerivatives;

  /**
   * A double[] containing the spatial derivatives of spread in seconds/kilometer. Note that the
   * depth derivative is always zero.
   */
  private double[] spreadDerivatives;

  /** A double containing the 2-norm of the horizontal derivatives in kilometers. */
  private double twoNorm;

  /** A double containing the value to sort on. */
  private double sortValue;

  /** A Pick object holding the pick the residuals were derived from. */
  private Pick pick;

  /**
   * Function to return whether this WeightedResidual is the Bayesian depth residual.
   *
   * @return A boolean flag indicating whether this WeightedResidual is the Bayesian depth residual
   */
  public boolean getIsBayesianDepth() {
    return isBayesianDepth;
  }

  /**
   * Function to get the residual.
   *
   * @return A double containing the residual. In seconds if for picks, in kilometers if for depth.
   */
  public double getResidual() {
    return residual;
  }

  /**
   * Function to get the linearly estimated residual.
   *
   * @return A double containing the linearly estimated residual
   */
  public double getLinEstResidual() {
    return linEstResidual;
  }

  /**
   * Function to get the weight.
   *
   * @return A double containing the weight
   */
  public double getWeight() {
    return weight;
  }

  /**
   * Function to get the linearly estimated weight.
   *
   * @return A double containing the linearly estimated weight
   */
  public double getLinEstWeight() {
    return linEstWeight;
  }

  /**
   * Function to get the spatial derivatives.
   *
   * @return A double[] containing the spatial derivatives in kilometers
   */
  public double[] getSpatialDerivatives() {
    return spatialDerivatives;
  }

  /**
   * Function to get the demedianed spatial derivatives.
   *
   * @return A double[] containing the demedianed spatial derivatives in kilometers
   */
  public double[] getDeMedSpaDerivatives() {
    return deMedSpaDerivatives;
  }

  /**
   * Function to get the spread derivatives.
   *
   * @return A double[] containing the spread derivatives in kilometers
   */
  public double[] getSpreadDerivatives() {
    return spreadDerivatives;
  }

  /**
   * Function to get the value to sort on.
   *
   * @return A double containing the value to sort on
   */
  public double getSortValue() {
    return sortValue;
  }

  /**
   * Function to get the pick the residuals were derived from.
   *
   * @return A Pick object holding the pick the residuals were derived from
   */
  public Pick getPick() {
    return pick;
  }

  /**
   * Function to set the linearly estimated residual.
   *
   * @param linEstResidual A double containing the linearly estimated residual
   */
  public void setLinEstResidual(double linEstResidual) {
    this.linEstResidual = linEstResidual;
  }

  /** The WeightedResidual default constructor. Creates the object with no initial information. */
  public WeightedResidual() {
    linEstResidual = 0d;
    twoNorm = Double.NaN;
    sortValue = Double.NaN;
  }

  /**
   * The WeightedResidual constructor. Initialize the weighted residual to the provided values.
   *
   * @param pick A Pick object holing the pick associated with this data, if any
   * @param residual A double containing the residual. In seconds if for picks, in kilometers if for
   *     depth.
   * @param weight A double containing the weight
   * @param isBayesianDepth A boolean flag indicating whether this WeightedResidual is the Bayesian
   *     depth residual
   * @param travelTimeLatDeriv A double containing the derivative of the travel time with respect to
   *     latitude in seconds/kilometers
   * @param travelTimeLonDeriv A double containing the derivative of the travel time with respect to
   *     longitude in seconds/kilometers
   * @param travelTimeDepthDeriv A double containing the derivative of the travel time with respect
   *     to depth in seconds/kilometers
   * @param spreadLatDeriv A double containing the derivative of the spread with respect to latitude
   *     in seconds/kilometers
   * @param spreadLonDeriv A double containing the derivative of the spread with respect to
   *     longitude in seconds/kilometers
   */
  public WeightedResidual(
      Pick pick,
      double residual,
      double weight,
      boolean isBayesianDepth,
      double travelTimeLatDeriv,
      double travelTimeLonDeriv,
      double travelTimeDepthDeriv,
      double spreadLatDeriv,
      double spreadLonDeriv) {
    reInit(
        pick,
        residual,
        weight,
        isBayesianDepth,
        travelTimeLatDeriv,
        travelTimeLonDeriv,
        travelTimeDepthDeriv,
        spreadLatDeriv,
        spreadLonDeriv);
  }

  /**
   * This function re-initialize (reuses) the weighted residual.
   *
   * @param pick A Pick object holing the pick associated with this data, if any
   * @param residual A double containing the residual. In seconds if for picks, in kilometers if for
   *     depth.
   * @param weight A double containing the weight
   * @param isBayesianDepth A boolean flag indicating whether this WeightedResidual is the Bayesian
   *     depth residual
   * @param travelTimeLatDeriv A double containing the derivative of the travel time with respect to
   *     latitude in seconds/kilometers
   * @param travelTimeLonDeriv A double containing the derivative of the travel time with respect to
   *     longitude in seconds/kilometers
   * @param travelTimeDepthDeriv A double containing the derivative of the travel time with respect
   *     to depth in seconds/kilometers
   * @param spreadLatDeriv A double containing the derivative of the spread with respect to latitude
   *     in seconds/kilometers
   * @param spreadLonDeriv A double containing the derivative of the spread with respect to
   *     longitude in seconds/kilometers
   */
  public void reInit(
      Pick pick,
      double residual,
      double weight,
      boolean isBayesianDepth,
      double travelTimeLatDeriv,
      double travelTimeLonDeriv,
      double travelTimeDepthDeriv,
      double spreadLatDeriv,
      double spreadLonDeriv) {
    this.pick = pick;
    this.residual = residual;
    this.weight = weight;
    this.isBayesianDepth = isBayesianDepth;

    spatialDerivatives = new double[3];
    spatialDerivatives[0] = travelTimeLatDeriv;
    spatialDerivatives[1] = travelTimeLonDeriv;
    spatialDerivatives[2] = travelTimeDepthDeriv;

    spreadDerivatives = new double[2];
    spreadDerivatives[0] = spreadLatDeriv;
    spreadDerivatives[1] = spreadLonDeriv;

    // Initialize the demedianed derivatives.
    deMedSpaDerivatives = Arrays.copyOf(spatialDerivatives, spatialDerivatives.length);
    linEstResidual = 0d;
    linEstWeight = weight;
    twoNorm = Double.NaN;
    sortValue = Double.NaN;
  }

  /**
   * This function removes the median from the travel-time residuals.
   *
   * @param median A double containing the median travel-time residual in seconds
   */
  public void deMedianResiduals(double median) {
    if (!isBayesianDepth) {
      residual -= median;
    }
  }

  /**
   * This function removes the median from the derivatives. Note that this isn't the median of the
   * derivatives, but the derivative corresponding to the median of the residuals.
   *
   * @param medians A double[] containing the derivative medians
   */
  public void deMedianDerivatives(double[] medians) {
    if (!isBayesianDepth) {
      for (int j = 0; j < medians.length; j++) {
        deMedSpaDerivatives[j] = spatialDerivatives[j] - medians[j];
      }
    }
  }

  /**
   * This function updates the Bayesian depth residual and weight.
   *
   * @param newResidual New Bayesian depth residual in kilometers
   * @param newWeight New Bayesian weight
   */
  public void updateBayesianResidual(double newResidual, double newWeight) {
    if (isBayesianDepth) {
      residual = newResidual;
      weight = newWeight;
    }
  }

  /**
   * This function updates the estimated residual given a trial step vector.
   *
   * @param trialStepVector A double[] contaiing the trial step vector (distance and direction from
   *     the current hypocenter in kilometers)
   */
  public void updateEstResiduals(double[] trialStepVector) {
    linEstResidual = residual;
    for (int j = 0; j < trialStepVector.length; j++) {
      linEstResidual -= trialStepVector[j] * spatialDerivatives[j];
    }
  }

  /**
   * This function updates the estimated weight given a trial step vector.
   *
   * @param trialStepVector A double[] containing the trial step vector (distance and direction from
   *     the current hypocenter in kilometers)
   */
  public void updateEstWeights(double[] trialStepVector) {
    linEstWeight = 1d / weight;
    for (int j = 0; j < spreadDerivatives.length; j++) {
      linEstWeight += trialStepVector[j] * spreadDerivatives[j];
    }
    linEstWeight = 1d / linEstWeight;
  }

  /**
   * This function removes the median from the estimated travel-time residuals.
   *
   * @param median A double containing the median travel-time residual in seconds
   */
  public void deMedianEstResiduals(double median) {
    if (!isBayesianDepth) {
      linEstResidual -= median;
    }
  }

  /**
   * This function sets the sort value to sort by travel-time residual. Note that the depth residual
   * will be sorted to the end to keep it out of the way.
   */
  public void setSortValue() {
    if (isBayesianDepth) {
      sortValue = TauUtil.DMAX;
    } else {
      sortValue = residual;
    }
  }

  /**
   * This function sets the sort value to sort by absolute demedianed travel-time residuals in order
   * to compute the spread, a 1-norm measure of scatter. Note that the depth residual will be sorted
   * to the end to keep it out of the way.
   *
   * @param median A double containing the median travel-time residual in seconds
   */
  public void setSortValueSpread(double median) {
    if (isBayesianDepth) {
      sortValue = TauUtil.DMAX;
    } else {
      sortValue = Math.abs(residual - median);
    }
  }

  /**
   * This function sets the sort value to sort by the demedianed, weighted residuals in order to
   * compute the R-estimator dispersion or penalty function.
   *
   * @param median A double containing the median travel-time residual in seconds
   */
  public void setSortValueDispersion(double median) {
    if (isBayesianDepth) {
      sortValue = residual * weight;
    } else {
      sortValue = (residual - median) * weight;
    }
  }

  /**
   * This function sets the sort value to sort by the estimated travel-time residual. Note that the
   * depth residual will be sorted to the end to keep it out of the way.
   */
  public void setSortValueLinEstRes() {
    if (isBayesianDepth) {
      sortValue = TauUtil.DMAX;
    } else {
      sortValue = linEstResidual;
    }
  }

  /**
   * This function sets the sort value to sort by the demedianed, weighted, estimated residuals in
   * order to compute the R-estimator dispersion or penalty function.
   *
   * @param median Median estimated travel-time residual in seconds
   * @param reWeight If true, use a linear estimate of the weight
   */
  public void setSortValueLinEstDisp(double median, boolean reWeight) {
    if (isBayesianDepth) {
      sortValue = linEstResidual * weight;
    } else {
      if (reWeight) {
        sortValue = (linEstResidual - median) * linEstWeight;
      } else {
        sortValue = (linEstResidual - median) * weight;
      }
    }
  }

  /**
   * This function contributes to projecting the original weighted residuals.
   *
   * @param weightedResidual A WeightedResidual containing the projected weighted residual
   * @param eigenvectorElem A double containing the eigenvector element
   */
  public void project(WeightedResidual weightedResidual, double eigenvectorElem) {
    residual += eigenvectorElem * weightedResidual.residual;

    for (int j = 0; j < spatialDerivatives.length; j++) {
      spatialDerivatives[j] += eigenvectorElem * weightedResidual.getSpatialDerivatives()[j];
    }
  }

  /**
   * This function contributes to projecting the estimated weighted residuals.
   *
   * @param weightedResidual A WeightedResidual containing the projected weighted residual
   * @param eigenvectorElem A double containing the eigenvector element
   */
  public void projectEstimated(WeightedResidual weightedResidual, double eigenvectorElem) {
    linEstResidual += eigenvectorElem * weightedResidual.linEstResidual;
  }

  /**
   * This function changes the sign of the residual and it's derivatives if the eigenvector is
   * backwards.
   */
  public void changeSign() {
    residual = -residual;

    for (int j = 0; j < spatialDerivatives.length; j++) {
      spatialDerivatives[j] = -spatialDerivatives[j];
    }
  }

  /**
   * This function calculates the 2-norm of the horizontal derivatives.
   *
   * @return A double containing the 2-norm of the horizontal derivatives in kilometers
   */
  public double calculateTwoNorm() {
    if (Double.isNaN(twoNorm)) {
      twoNorm =
          Math.sqrt(Math.pow(spatialDerivatives[0], 2d) + Math.pow(spatialDerivatives[1], 2d));
    }

    return twoNorm;
  }

  /**
   * This function calculates the correlation between these horizontal derivatives and another set
   * of horizontal derivatives.
   *
   * @param weightedResidual A WeightedResidual object containing the weighted residual information
   *     to correlate against
   * @return A double containing the correlation between the horizontal derivatives of two picks
   */
  public double calculateCorrelation(WeightedResidual weightedResidual) {
    return (spatialDerivatives[0] * weightedResidual.getSpatialDerivatives()[0]
            + spatialDerivatives[1] * weightedResidual.getSpatialDerivatives()[1])
        / (calculateTwoNorm() * weightedResidual.calculateTwoNorm());
  }

  /**
   * Calculate the weighted derivatives for computing the "normal" matrix.
   *
   * @param degreesOfFreedom An int contaiing the number of degrees of freedom
   * @return A double[] containing the weighted derivative vector
   */
  public double[] calculateWeightedDeriv(int degreesOfFreedom) {
    double[] weightedDeriv = new double[degreesOfFreedom];

    for (int j = 0; j < degreesOfFreedom; j++) {
      weightedDeriv[j] = weight * spatialDerivatives[j];
    }

    return weightedDeriv;
  }

  /**
   * Calculate the weighted, demedianed derivatives for computing the projected "normal" matrix.
   *
   * @param degreesOfFreedom An int contaiing the number of degrees of freedom
   * @return A double[] containing the weighted derivative vector
   */
  public double[] calculateWeightedDeMedDeriv(int degreesOfFreedom) {
    double[] weightedDeMedDeriv = new double[degreesOfFreedom];

    for (int j = 0; j < degreesOfFreedom; j++) {
      weightedDeMedDeriv[j] = weight * deMedSpaDerivatives[j];
    }

    return weightedDeMedDeriv;
  }

  /**
   * This function updates the pick data importance.
   *
   * @param importance Pick data importance
   */
  public void updateImportance(double importance) {
    if (pick != null) {
      pick.setImportance(importance);
    }
  }

  /**
   * This function creates a string holding the contents of the weighted residuals.
   *
   * @param full A boolean flag indicating whether to print the derivatives as well
   * @return A String containing the contents of the weighted residuals
   */
  public String printWeightedResiduals(boolean full) {
    String weiResString = "";

    if (!full || spatialDerivatives == null) {
      weiResString +=
          String.format(
              "res: %7.2f %7.2f wt: %7.4f %b\n", residual, linEstResidual, weight, isBayesianDepth);
    } else {
      weiResString +=
          String.format(
              "res: %7.2f %7.2f wt: %7.4f spatialDerivatives: %10.3e %10.3e %10.3e %b\n",
              residual,
              linEstResidual,
              weight,
              spatialDerivatives[0],
              spatialDerivatives[1],
              spatialDerivatives[2],
              isBayesianDepth);
    }

    return weiResString;
  }

  /**
   * Comparison function used to sort weighted residuals by ascending order.
   *
   * @param weightedResidual A WeightedResidual object containing the second weighted residual to
   *     compare.
   * @return +1 if this WeightedResidual object sortValue variable is greater than the second
   *     WeightedResidual object sortValue variable; -1 if this WeightedResidual object sortValue
   *     variable is less than the second WeightedResidual object sortValue variable; and 0 if this
   *     WeightedResidual object sortValue variable is equal to the second WeightedResidual object
   *     sortValue variable;
   */
  @Override
  public int compareTo(WeightedResidual weightedResidual) {
    // Sort into value order.
    if (this.sortValue < weightedResidual.sortValue) {
      return -1;
    } else if (this.sortValue > weightedResidual.sortValue) {
      return +1;
    } else {
      return 0;
    }
  }
}
