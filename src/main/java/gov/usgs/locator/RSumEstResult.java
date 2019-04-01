package gov.usgs.locator;

/**
 * The RSumEstResult class saves the results of one pass through the rank-sum
 * estimator algorithm.
 * 
 * @author Ray Buland
 *
 */
public class RSumEstResult {
  /**
   * A double containing the step length in kilometers.
   */
  private double stepLength;
  
  /**
   * A double containing the median residual in seconds.
   */
  private double medianResidual;

  /**
   * A double containing the residual spread in seconds.
   */
  private double residualSpread;
  
  /** 
   * A double containing the dispersion in seconds.
   */
  private double dispersion;

  /**
   * Function to return the step length.
   * 
   * @return A double containing the step length in kilometers.
   */
  public double getStepLength() {
    return stepLength;
  }
  
  /**
   * Function to return the median residual.
   * 
   * @return A double containing the median residual in seconds.
   */
  public double getMedianResidual() {
    return medianResidual;
  }

  /**
   * Function to return the residual spread.
   * 
   * @return A double containing the residual spread in seconds.
   */
  public double getResidualSpread() {
    return residualSpread;
  }

  /**
   * Function to return the dispersion.
   * 
   * @return A double containing the dispersion in seconds.
   */
  public double getDispersion() {
    return dispersion;
  }

  /**
   * The RSumEstResult constructor. Initializes the class to a set of results from
   * the rank-sum estimator algorithm.
   * 
   * @param stepLength Step length in kilometers
   * @param medianResidual Median residual in seconds
   * @param residualSpread Residual spread in seconds
   * @param dispersion Dispersion in seconds
   */
  public RSumEstResult(double stepLength, double medianResidual, double residualSpread, 
      double dispersion) {
    this.stepLength = stepLength;
    this.medianResidual = medianResidual;
    this.residualSpread = residualSpread;
    this.dispersion = dispersion;
  }
}
