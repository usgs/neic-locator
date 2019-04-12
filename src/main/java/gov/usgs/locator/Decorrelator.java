package gov.usgs.locator;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import gov.usgs.traveltime.TauUtil;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The Decorrelator class handles the decorrelation of the pick data. This involves a singular
 * valued decomposition of the correlation matrix.
 *
 * @author Ray Buland
 */
public class Decorrelator {
  /** An int containing the number of decorrelation data. */
  private int numData;

  /** An int containing the number of pick data. */
  private int numPickData;

  /** An int containing the number of projected data. */
  private int numProjectedData;

  /** A double[][] containing the eigenvectors. */
  private double[][] eigenvectors;

  /** A double array containing the projected weights. */
  private double[] projectedWeights;

  /** A double[][] containing the covariance matrix. */
  private double[][] covMatrix;

  /** An Event object containing the event to decorrelate. */
  private Event event;

  /** An ArrayList of WeightedResidual objects containing the original weighted residuals. */
  private ArrayList<WeightedResidual> weightedResidualsOrg;

  /** An ArrayList of WeightedResidual objects containing the projected weighted residuals. */
  private ArrayList<WeightedResidual> weightedResidualsProj;

  /**
   * An ArrayList of WeightedResidual objects containing the projected weighted residuals in their
   * original order.
   */
  private ArrayList<WeightedResidual> weightedResidualsProjOrg;

  /** A Jama.Matrix object containing the final covariance matrix. */
  private Matrix covMatrixFinal;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(Decorrelator.class.getName());

  /**
   * The Decorrelator constructor. This constructor initializes the event and projected residuals to
   * the provided event and residuals.
   *
   * @param event An Event object containing event information to decorrelate
   */
  public Decorrelator(Event event) {
    this.event = event;
    this.weightedResidualsProj = event.getProjectedWeightedResiduals();
  }

  /** This function performs the decorrelation. */
  public void decorrelate() {
    // We can't remember the original sort of the raw residuals
    // because it keeps changing when cloned.
    weightedResidualsOrg = event.getOriginalWeightedResiduals();

    LOGGER.finer(event.printWeightedResiduals("Org", true));

    // We'll use the dimension of the picks a lot!
    numData = weightedResidualsOrg.size();
    numPickData = numData - 1;

    // Create the covariance matrix.
    makeCovariance();

    // If the matrix is too big, get rid of the most correlated
    // data.
    triagePicks();

    // Do the eigenvalue problem.
    doEigen();
  }

  /** This function projects the raw pick data into a set of decorrelated virtual picks. */
  @SuppressWarnings("unchecked")
  public void projectPicks() {
    WeightedResidual weightedResiduals;

    // Get rid of triaged picks.
    weightedResidualsOrg = event.getOriginalWeightedResiduals();
    for (int j = weightedResidualsOrg.size() - 2; j >= 0; j--) {
      if (weightedResidualsOrg.get(j).getPick().getIsTriage()) {
        weightedResidualsOrg.remove(j);
      }
    }

    // Project the pick data.
    if (weightedResidualsProj.size() > 0) {
      weightedResidualsProj.clear();
    }
    for (int i = numProjectedData; i < numPickData; i++) {
      weightedResiduals =
          new WeightedResidual(null, 0d, projectedWeights[i - numProjectedData], false, 0d, 0d, 0d);

      for (int j = 0; j < numPickData; j++) {
        weightedResiduals.project(weightedResidualsOrg.get(j), eigenvectors[j][i]);
      }

      if ((event.getHasPhaseIdChanged()) && !checkEigenSigns(i, weightedResiduals)) {
        // If so, fix the residual and derivatives.
        weightedResiduals.changeSign();

        // Fix the eigenvector so it will be right next time.
        for (int j = 0; j < numPickData; j++) {
          eigenvectors[j][i] = -eigenvectors[j][i];
        }
      }
      weightedResidualsProj.add(weightedResiduals);
    }

    // Set the projected or virtual number of picks.
    event.setNumProjectedPhasesUsed(weightedResidualsProj.size());

    // Add the Bayesian depth here since it doesn't correlate with
    // anything else.
    weightedResidualsProj.add(weightedResidualsOrg.get(numPickData));

    // Because the weighted residuals get sorted, we need a copy in
    // the original order to project the estimated residuals.
    weightedResidualsProjOrg = (ArrayList<WeightedResidual>) weightedResidualsProj.clone();

    LOGGER.finest(event.printWeightedResiduals("Proj", true));
  }

  /**
   * This function projects the estimated pick residuals into a set of decorrelated virtual
   * estimated residuals.
   */
  public void projectEstimatedPicks() {
    WeightedResidual weightedResiduals;

    // Project the pick data.
    if (weightedResidualsProj.size() > 0) {
      weightedResidualsProj.clear();
    }
    for (int i = numProjectedData; i < numPickData; i++) {
      weightedResiduals = weightedResidualsProjOrg.get(i - numProjectedData);
      weightedResiduals.setLinEstResidual(0d);

      for (int j = 0; j < numPickData; j++) {
        weightedResiduals.projectEstimated(weightedResidualsOrg.get(j), eigenvectors[j][i]);
      }

      weightedResidualsProj.add(weightedResiduals);
    }

    // Add the Bayesian depth here since it doesn't correlate with
    // anything else.
    weightedResidualsProj.add(weightedResidualsOrg.get(numPickData));
  }

  /**
   * This function creates the covariance matrix. Note that the Bayesian depth is excluded from the
   * covariance matrix. This makes it easier to trim the eigenvalues. Since it doesn't correlate
   * with anything else anyway, it can be added back into the projected data later.
   */
  private void makeCovariance() {
    Pick pickI;

    // Do the pick covariance.
    covMatrix = new double[numPickData][numPickData];
    for (int i = 0; i < numPickData; i++) {
      pickI = weightedResidualsOrg.get(i).getPick();

      for (int j = i; j < numPickData; j++) {
        covMatrix[i][j] = LocUtil.computeCovariance(pickI, weightedResidualsOrg.get(j).getPick());
        covMatrix[j][i] = covMatrix[i][j];
      }
    }
  }

  /**
   * This function removes the most correlated picks until the eigenvalue problem is a reasonable
   * size.
   */
  private void triagePicks() {
    if (weightedResidualsOrg.size() > LocUtil.MAXPICKSTODECORRELATE) {
      LOGGER.finest(LocUtil.printMatrix(covMatrix, "Raw Covariance Matrix"));

      // Reset all the triage flags.
      event.resetTriage();

      // We need to eliminate the most correlated data.  Create a list
      // to help out.
      ArrayList<CorrelationsSum> corrSums = new ArrayList<CorrelationsSum>();

      // Populate the list.
      for (int i = 0; i < covMatrix.length; i++) {
        double sum = 0d;

        for (int j = 0; j < covMatrix.length; j++) {
          if (i != j) {
            // Don't include the autocorrelation
            sum += covMatrix[i][j];
          }
        }
        corrSums.add(new CorrelationsSum(i, sum));
      }

      // Sort on the correlation sums.
      corrSums.sort(null);

      // Eliminate the biggest correlation sums.
      for (int i = corrSums.size() - 1; i >= LocUtil.MAXPICKSTODECORRELATE; i--) {
        String corrSumsStr = "F:";
        for (int j = 0; j < corrSums.size(); j++) {
          corrSumsStr += "\t" + corrSums.get(j);
        }
        LOGGER.finest(corrSumsStr);

        LOGGER.fine(String.format("\tTriage: eliminate %3d %s", i, corrSums.get(i)));

        int k = corrSums.get(i).getRowIndex();
        corrSums.remove(i);

        // Now compensate the sums for the row and column eliminated.
        for (int j = 0; j < corrSums.size(); j++) {
          int l = corrSums.get(j).getRowIndex();
          if (k != l) {
            corrSums.get(j).decSum(covMatrix[l][k]);
          }
        }

        // And re-sort.
        corrSums.sort(null);
      }

      // Sort the correlation sums back into order.
      for (int j = 0; j < corrSums.size(); j++) {
        corrSums.get(j).rowSort();
      }
      corrSums.sort(null);

      // Finally remove the most highly correlated rows and columns.
      int[] keep = new int[corrSums.size()];
      for (int j = 0; j < keep.length; j++) {
        keep[j] = corrSums.get(j).getRowIndex();
      }

      // Use the JAMA Matrix to eliminate the most correlated rows and
      // columns.
      Matrix covRaw = new Matrix(covMatrix);
      covMatrixFinal = covRaw.getMatrix(keep, keep);
      covRaw = null;
      covMatrix = covMatrixFinal.getArray();

      LOGGER.finest(LocUtil.printMatrix(covMatrix, "Final Covariance Matrix"));

      // We're not quite done.  We need to eliminate the same picks
      // from the weighted residuals.  And make sure they don't come
      // back.
      int k = keep.length - 1;
      for (int j = weightedResidualsOrg.size() - 2; j >= 0; j--) {
        if (j != keep[k]) {
          weightedResidualsOrg.get(j).getPick().setIsTriage(true);
          weightedResidualsOrg.remove(j);
        } else {
          k--;
        }
      }

      numData = weightedResidualsOrg.size();
      numPickData = numData - 1;

      LOGGER.finer(event.printWeightedResiduals("Org", true));
    } else {
      // We're OK.  Just create the correlation matrix in a form
      // suitable for extracting the eigenvalues.
      covMatrixFinal = new Matrix(covMatrix);
    }

    // We're done with the covariance matrix.
    covMatrix = null;
  }

  /**
   * This function gets the eigenvalues and eigenvectors of the covariance matrix. Note that using
   * the JAMA package computes all eigenvectors every time. This is less efficient than the Fortran
   * Linpak routines that allowed the computation of only the eigenvectors corresponding to
   * eigenvalues being kept.
   */
  private void doEigen() {
    // Do the eigenvalue problem (and time it).
    LocUtil.startTimer();
    EigenvalueDecomposition eig = covMatrixFinal.eig();

    LOGGER.fine(LocUtil.endTimer("Eigenvalue"));

    double[] eigenvalues = eig.getRealEigenvalues();

    LOGGER.fine(LocUtil.printVector(eigenvalues, "Eigenvalues Vector"));

    eigenvectors = eig.getV().getArray();

    LOGGER.finest(testEig(covMatrixFinal, eig));

    // We don't need the covariance matrix any more.
    covMatrixFinal = null;
    eig = null;

    // Work out the eigenvalue elimination.
    double evSum = 0d;

    // Add up all the eigenvalues.
    for (int j = 0; j < numPickData; j++) {
      evSum += eigenvalues[j];
    }

    // The largest eigenvalue is always the last.
    double evMax = eigenvalues[numPickData - 1];

    // Now set the limits.
    double evLim = LocUtil.EIGENVALUESLIMIT * evSum;
    double evThresh = LocUtil.EIGENVALUESTHRESH * evMax;

    // Find how many eigenvalues to keep.
    evSum = 0d;
    for (numProjectedData = numPickData - 1; numProjectedData >= 0; numProjectedData--) {
      evSum += eigenvalues[numProjectedData];

      if ((evSum > evLim) && (eigenvalues[numProjectedData] <= evThresh)) {
        break;
      }
    }
    numProjectedData++;

    // Print it out.
    LOGGER.fine(
        String.format(
            "Project: wsum wlim elim: %10.3e %10.3e %10.3e numProjectedData: %5d",
            evSum, evLim, evThresh, numProjectedData));

    // Get the corresponding weights.
    projectedWeights = new double[numData - numProjectedData];
    for (int j = numProjectedData; j < numPickData; j++) {
      projectedWeights[j - numProjectedData] = 1d / Math.sqrt(eigenvalues[j]);
    }

    projectedWeights[numPickData - numProjectedData] =
        weightedResidualsOrg.get(numPickData).getWeight();
  }

  /**
   * This function corrects the eigenvector's signs. The signs of the eigenvectors are arbitrary,
   * but not the linear combination of picks implied. If the sign is wrong, physically the
   * correlated cluster of stations will appear to be at an azimuth of 180 degrees from where it
   * actually is. Alternatively, the residual will have the wrong sign, which, of course, will wreak
   * havoc on the location.
   *
   * @param index An int containing the index of the eigenvector
   * @param weightedResiduals A WeightedResidual object containing the weighted residual storage for
   *     the projected pick
   * @return True if the sign is OK, false otherwise
   */
  private boolean checkEigenSigns(int index, WeightedResidual weightedResiduals) {
    double corrMax = -1d;
    double corrMin = 1d;
    double depthSum = 0d;

    for (int j = 0; j < numPickData; j++) {
      if (Math.abs(eigenvectors[j][index]) > TauUtil.DTOL) {
        double corr = weightedResidualsOrg.get(j).calculateCorrelation(weightedResiduals);
        corrMax = Math.max(corrMax, corr);
        corrMin = Math.min(corrMin, corr);
        depthSum += weightedResidualsOrg.get(j).getSpatialDerivatives()[2];
      }
    }

    // For a one sided station distribution, the minimum and maximum can
    // have the same sign.
    if (corrMax * corrMin >= 0d) {
      // If the depth derivatives agree, we're probably OK.
      return depthSum * weightedResiduals.getSpatialDerivatives()[2] >= 0d;
    }

    // Otherwise, see if the azimuth needs to be flipped 180 degrees.
    if (corrMax > -corrMin) {
      // If the depth derivatives agree, we're probably OK.
      if (depthSum * weightedResiduals.getSpatialDerivatives()[2] >= 0d) {
        return true;
      } else {
        // See if the correlations are small.
        if (Math.abs(corrMax + corrMin) < 0.05d) {
          // If so, the results are problematic.
          if (Math.abs(weightedResiduals.getSpatialDerivatives()[2]) > 1e-4d) {
            return false;
          }
           
          return true;
        } else {
          // If not, believe the correlation.
          return true;
        }
      }
      // Otherwise, the sign is probably wrong.
    } else {
      // If the depth derivatives disagree, the sign is probably wrong.
      if (depthSum * weightedResiduals.getSpatialDerivatives()[2] <= 0d) {
        return false;
      } else {
        // See if the correlations are small.
        if (Math.abs(corrMax + corrMin) < 0.05d) {
          // If so, the results are problematic.
          return Math.abs(weightedResiduals.getSpatialDerivatives()[2]) > 1e-4d;
          // If not, believe the correlation.
        } else {
          return false;
        }
      }
    }
  }

  /**
   * This function tests the eigenvectors. Test the eigenvectors by computing the norm of (A - value
   * * I) * vector for each eigenvalue-eigenvector pair.
   *
   * @param a A Matrix object for which eigenvalues have been computed
   * @param eig An EigenvalueDecomposition object containing the Eigenvalues and eigenvectors
   * @return A String containing the rest results
   */
  private String testEig(Matrix a, EigenvalueDecomposition eig) {
    boolean bad = false;
    double[] values = eig.getRealEigenvalues();
    int numValues = values.length;
    Matrix vectors = eig.getV();
    String results = "";

    // Loop over the eigenvalues.
    for (int j = 0; j < numValues; j++) {
      // Construct A-value*I.
      Matrix test = a.minus(Matrix.identity(numValues, numValues).times(values[j]));

      // Multiply by the eigenvector.
      Matrix result = test.times(vectors.getMatrix(0, numValues - 1, j, j));

      // Only print something if it doesn't look right.
      if (result.norm2() > TauUtil.DTOL) {
        if (!bad) {
          bad = true;
          results += "Bad eigenvector(s):";
        }

        results +=
            String.format(
                "\t%3d %8.2e %8.2e %8.2e", j, result.norm1(), result.norm2(), result.normInf());
      }
    }

    return results;
  }
}
