package gov.usgs.locator;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import gov.usgs.traveltime.TauUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * The CloseOut class computes all the errors and heuristics used to evaluate the location after an
 * event is located.
 *
 * @author Ray Buland
 */
public class CloseOut {
  /** An integer containing the degrees of freedom allowed for close out computations. */
  private int degreesOfFreedom;

  /** A double containing the compensation factor for the effective number of data. */
  private double compFactor;

  /** An Event object containing the event to perform closeout calculations for. */
  private Event event;

  /** A Hypo object containing the hypocenter of the event to perform closeout calculations for. */
  private Hypocenter hypo;

  /**
   * An ArrayList of WeightedResidual objects containing the raw (sorted) weighted residuals of the
   * event picks to perform closeout calculations with.
   */
  private ArrayList<WeightedResidual> rawWeightedResiduals;

  /**
   * An ArrayList of WeightedResidual objects containing the projected weighted residuals of the
   * event picks to perform closeout calculations with when decorrelating.
   */
  private ArrayList<WeightedResidual> projectedWeightedResiduals;

  /**
   * An RankSumEstimator objects containing rank-sum exstimator of the event to perform closeout
   * calculations for.
   */
  private RankSumEstimator rankSumEstimator;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(CloseOut.class.getName());

  /**
   * The CloseOut construtor. This constructor
   *
   * @param event An Event object containing the event to perform closeout calculations for.
   */
  public CloseOut(Event event) {
    this.event = event;
    hypo = event.getHypo();
    rawWeightedResiduals = event.getRawWeightedResiduals();
    projectedWeightedResiduals = event.getProjectedWeightedResiduals();
    rankSumEstimator = event.getRawRankSumEstimator();
  }

  /**
   * This function computes the 90% marginal confidence intervals, the 90% error ellipse or
   * ellipsoid, and the pick data importances. Note that held locations are treated as though they
   * were free for the purposes of error evaluation.
   *
   * @param status LocStatus containing the event status before the close out
   * @return A LocStatus object containing event status after the close out
   */
  public LocStatus compFinalStats(LocStatus status) {
    // Get the azimuthal gaps.
    event.computeAzimuthalGap();

    // If there isn't enough data, zero out all statistics.
    if (status == LocStatus.INSUFFICIENT_DATA) {
      event.zeroOutStatistics(true);
      event.zeroOutWeights();
      event.setQualityFlags(status);
      return LocStatus.INSUFFICIENT_DATA;
    }

    // Get the residual spread.
    event.setResidualsStandardError(rankSumEstimator.computeSpread());

    // Force the number of degrees of freedom, pretending everything
    // was always free.
    degreesOfFreedom = 3;

    // Compensate for the effective number of data.  This is needed to
    // make the errors for events comparable whether they have been
    // decorrelated or not.
    if (event.getUseDecorrelation()) {
      compFactor = 1d;
    } else {
      compFactor =
          Math.sqrt(
              LocUtil.EFFOFFSET
                  - LocUtil.EFFSLOPE * Math.log10((double) (event.getNumPhasesUsed() + 1)));
    }

    // For the parameter errors, we need a "normal" matrix using the
    // demedianed derivatives of the projected data.
    // Initialize the "normal" matrix to zero.
    double[][] correlationMatrix = new double[degreesOfFreedom][degreesOfFreedom];
    for (int i = 0; i < degreesOfFreedom; i++) {
      for (int j = 0; j < degreesOfFreedom; j++) {
        correlationMatrix[i][j] = 0d;
      }
    }

    // Construct the lower half of the "normal" matrix.
    if (LocUtil.useDecorrelation) {
      // If we're decorrelating, use the projected pick data.
      for (int k = 0; k < projectedWeightedResiduals.size(); k++) {
        double[] derivatives =
            projectedWeightedResiduals.get(k).calculateWeightedDeMedDeriv(degreesOfFreedom);
        for (int i = 0; i < degreesOfFreedom; i++) {
          for (int j = 0; j < degreesOfFreedom; j++) {
            correlationMatrix[i][j] += derivatives[i] * derivatives[j];
          }
        }
      }
    } else {
      // Otherwise, use the raw pick data.
      for (int k = 0; k < rawWeightedResiduals.size(); k++) {
        double[] derivatives =
            rawWeightedResiduals.get(k).calculateWeightedDeMedDeriv(degreesOfFreedom);
        for (int i = 0; i < degreesOfFreedom; i++) {
          for (int j = 0; j < degreesOfFreedom; j++) {
            correlationMatrix[i][j] += derivatives[i] * derivatives[j];
          }
        }
      }
    }

    // Make the "normal" matrix symmetric.
    for (int i = 0; i < degreesOfFreedom - 1; i++) {
      for (int j = (i + 1); j < degreesOfFreedom; j++) {
        correlationMatrix[i][j] = correlationMatrix[j][i];
      }
    }

    LOGGER.finer(LocUtil.printMatrix(correlationMatrix, "Projected Matrix"));

    // Set up the matrix computations.
    Matrix matrix = new Matrix(correlationMatrix);
    Matrix inverseMatrix;
    try {
      // Compute the inverse matrix (the correlation matrix).
      inverseMatrix = matrix.inverse();

      // Get the inverse matrix back to compute errors.
      correlationMatrix = inverseMatrix.getArray();

      LOGGER.finer(LocUtil.printMatrix(correlationMatrix, "Correlation Matrix"));
    } catch (RuntimeException e) {
      // Oops!  The matrix is singular.
      LOGGER.warning("***** Projected normal matrix is singular!*****");
      event.zeroOutStatistics(false);
      event.zeroOutWeights();

      return LocStatus.SINGULAR_MATRIX;
    }

    // Do the marginal confidence intervals.
    double confidenceInterval = LocUtil.PERPT1D / compFactor;
    event.setTimeStandardError(confidenceInterval * event.getResidualsStandardError());
    event.setLatitudeStandardError(
        confidenceInterval * Math.sqrt(Math.max(correlationMatrix[0][0], 0d)));
    event.setLongitudeStandardError(
        confidenceInterval * Math.sqrt(Math.max(correlationMatrix[1][1], 0d)));
    event.setDepthStandardError(
        confidenceInterval * Math.sqrt(Math.max(correlationMatrix[2][2], 0d)));

    try {
      LOGGER.info("Computing the error ellipsoid");
      // Do the error ellipsoid.
      computeErrorEllipsoid(inverseMatrix);
    } catch (RuntimeException e) {
      // Oops!  Something bad happened to the eigenvalue problem.
      LOGGER.severe("***** Failure computing the error ellipsoid!*****");
      event.zeroOutStatistics(false);
      event.zeroOutWeights();
      return LocStatus.ELLIPSOID_FAILED;
    }

    // For the data importances, we need the actual "normal"
    // matrix (i.e., using the derivatives of the original
    // pick data).
    // Initialize the "normal" matrix to zero
    correlationMatrix = new double[degreesOfFreedom][degreesOfFreedom];
    for (int i = 0; i < degreesOfFreedom; i++) {
      for (int j = 0; j < degreesOfFreedom; j++) {
        correlationMatrix[i][j] = 0d;
      }
    }

    // Construct the lower half of the "normal" matrix.
    for (int k = 0; k < rawWeightedResiduals.size(); k++) {
      double[] derivatives = rawWeightedResiduals.get(k).calculateWeightedDeriv(degreesOfFreedom);
      for (int i = 0; i < degreesOfFreedom; i++) {
        for (int j = 0; j < degreesOfFreedom; j++) {
          correlationMatrix[i][j] += derivatives[i] * derivatives[j];
        }
      }
    }

    // Make the "normal" matrix symmetric.
    for (int i = 0; i < (degreesOfFreedom - 1); i++) {
      for (int j = (i + 1); j < degreesOfFreedom; j++) {
        correlationMatrix[i][j] = correlationMatrix[j][i];
      }
    }

    LOGGER.finer(LocUtil.printMatrix(correlationMatrix, "Normal Matrix"));

    // Set up the matrix computations.
    matrix = new Matrix(correlationMatrix);
    try {
      // Compute the inverse (the correlation matrix).
      inverseMatrix = matrix.inverse();

      // Get the inverse matrix back to compute data importances.
      correlationMatrix = inverseMatrix.getArray();

      LOGGER.finer(LocUtil.printMatrix(correlationMatrix, "Correlation Matrix"));
    } catch (RuntimeException e) {
      // Oops!  The matrix is singular.
      LOGGER.warning("***** Pick normal matrix is singular!*****");
      event.zeroOutStatistics(false);
      event.zeroOutWeights();
      return LocStatus.SINGULAR_MATRIX;
    }

    // Do the data importances.
    computeImportance(correlationMatrix);

    // Set the quality flags.
    event.setQualityFlags(status);
    return status;
  }

  /**
   * This function decomposes the correlation matrix into eigenvalues and eigenvectors in order to
   * compute the error ellipse (epicenter) or ellipsoid (hypocenter). AveH, the radius of a circle
   * with the same area as the error ellipse is also computed as it may require a second eigenvalue
   * solution.
   *
   * @param correlationMatrix A Matrix object containing the correlation matrix
   */
  private void computeErrorEllipsoid(Matrix correlationMatrix) {
    EllipseAxis[] ellip = event.getErrorEllipse();

    // Do the eigenvalue/vector decomposition.
    EigenvalueDecomposition eigen = correlationMatrix.eig();

    // Get the eigenvalues.
    double[] eigenvalues = eigen.getRealEigenvalues();

    // Get the eigenvectors.
    double[][] eigenvectors = eigen.getV().getArray();

    // Mash the eigenvalues/vectors into something more useful.
    if (degreesOfFreedom < 3) {
      // If the depth is held, do the error ellipse.
      double confidenceInterval = LocUtil.PERPT2D / compFactor;

      for (int j = 0; j < 2; j++) {
        // Do the axis half length.
        double semiLen = confidenceInterval * Math.sqrt(Math.max(eigenvalues[j], 0d));

        // Do the azimuth.
        double azimuth = 0d;
        if (Math.abs(eigenvectors[0][j]) + Math.abs(eigenvectors[1][j]) > TauUtil.DTOL) {
          azimuth = Math.toDegrees(Math.atan2(eigenvectors[1][j], -eigenvectors[0][j]));
        }

        // azimuth bounds checks
        if (azimuth < 0d) {
          azimuth += 360d;
        }
        if (azimuth > 180d) {
          azimuth -= 180d;
        }

        // set the axis
        ellip[j] = new EllipseAxis(semiLen, azimuth, 0d);
      }

      // third axis is zero for error ellipse
      ellip[2] = new EllipseAxis(0d, 0d, 0d);

      // Do aveH (the equivalent radius of the error ellipse).
      event.setEquivalentErrorRadius(
          LocUtil.PERPT1D
              * Math.sqrt(ellip[0].getSemiLen() * ellip[1].getSemiLen())
              / LocUtil.PERPT2D);
    } else {
      // Otherwise, do the error ellipsoid.
      double confidenceInterval = LocUtil.PERPT3D / compFactor;

      for (int j = 0; j < 3; j++) {
        // Do the axis half length.
        double semiLen = confidenceInterval * Math.sqrt(Math.max(eigenvalues[j], 0d));

        double signum = Math.signum(eigenvectors[2][j]);

        // Do the azimuth.
        double azimuth = 0d;
        if (Math.abs(eigenvectors[0][j]) + Math.abs(eigenvectors[1][j]) > TauUtil.DTOL) {
          azimuth =
              Math.toDegrees(Math.atan2(signum * eigenvectors[1][j], -signum * eigenvectors[0][j]));
        }

        // azimuth bounds checks
        if (azimuth < 0d) {
          azimuth += 360d;
        }
        if (Math.abs(eigenvectors[2][j]) <= TauUtil.DTOL && azimuth > 180d) {
          azimuth -= 180d;
        }

        // Do the plunge.
        double plunge = Math.toDegrees(Math.asin(Math.min(signum * eigenvectors[2][j], 1d)));

        // set the axis
        ellip[j] = new EllipseAxis(semiLen, azimuth, plunge);
      }

      // Do aveH.  First, extract the error ellipse.
      Matrix subInverse = correlationMatrix.getMatrix(0, 1, 0, 1);

      // Do the eigenvalues again.
      eigen = subInverse.eig();
      eigenvalues = eigen.getRealEigenvalues();

      // Finally, get the equivalent radius of the error ellipse.
      event.setEquivalentErrorRadius(
          LocUtil.PERPT1D
              * Math.sqrt(Math.sqrt(Math.max(eigenvalues[0] * eigenvalues[1], 0d)))
              / compFactor);
    }

    // Sort the error ellipsoid axis by semiLen.
    Arrays.sort(ellip);

    // update the event error ellipsoid
    event.setErrorEllipse(ellip);

    // Do the summary errors, which also depend on the error ellipsoid.
    event.computeSummaryErrors();
  }

  /**
   * Compute the data importances.
   *
   * @param correlationMatrix A double[][] containing the correlation matrix
   */
  private void computeImportance(double[][] correlationMatrix) {
    double sumImportance = 0d;

    // The data importances are just the inner product of the derivative
    // vector with the correlation matrix.
    for (int k = 0; k < rawWeightedResiduals.size(); k++) {
      if (!rawWeightedResiduals.get(k).getIsBayesianDepth()) {
        double[] weightedDerivatives =
            rawWeightedResiduals.get(k).calculateWeightedDeriv(degreesOfFreedom);
        double[] dotProduct = new double[degreesOfFreedom];

        for (int i = 0; i < degreesOfFreedom; i++) {
          // init to zero
          dotProduct[i] = 0d;

          for (int j = 0; j < degreesOfFreedom; j++) {
            dotProduct[i] += correlationMatrix[i][j] * weightedDerivatives[j];
          }
        }

        // compute importance
        double importance = 0d;
        for (int j = 0; j < degreesOfFreedom; j++) {
          importance += weightedDerivatives[j] * dotProduct[j];
        }

        // add to summary importance
        sumImportance += importance;
        rawWeightedResiduals.get(k).updateImportance(importance);
      }
    }

    // Do the Bayesian depth data importance separately.
    event.setBayesianDepthDataImportance(
        correlationMatrix[2][2] * Math.pow(hypo.getBayesianDepthWeight(), 2d));

    LOGGER.fine(
        String.format(
            "Normeq: qsum qsum+ = %4.2f %4.2f",
            sumImportance, sumImportance + event.getBayesianDepthDataImportance()));
  }
}
