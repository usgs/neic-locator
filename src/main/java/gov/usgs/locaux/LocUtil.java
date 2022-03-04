package gov.usgs.locaux;

import gov.usgs.locator.HypoAudit;
import gov.usgs.locator.Hypocenter;
import gov.usgs.locator.Pick;
import gov.usgs.locator.Station;
import gov.usgs.traveltime.TauUtil;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.json.simple.JSONObject;

/**
 * The LocUtil class maintains the locator static constants and common utilities.
 *
 * @author Ray Buland
 */
public class LocUtil {
  /**
   * A double constant representing the maximum distance the epicenter can move and still be
   * considered to be the same as the starting epicenter.
   */
  public static final double DISTANCETOLERANCE = 3d;

  /**
   * A double constant representing the maximum distance the depth can move and still be considered
   * to be the same as the starting depth.
   */
  public static final double DEPTHTOLERANCE = 5d;

  /** A double constant representing the minimum depth the Locator will allow. */
  public static final double DEPTHMIN = 1d;

  /** A double constant representing the maximum depth the Locator will allow. */
  public static final double DEPTHMAX = 700d;

  /**
   * A double constant representing the default Bayesian depth standard error in kilometers for a
   * held depth solution.
   */
  public static final double HELDDEPTHSE = 1d;

  /**
   * A double constant representing the default Bayesian depth in kilometers for a free depth
   * solution. This is the Bayesian default for all shallow earthquakes.
   */
  public static final double DEFAULTDEPTH = 10d;

  /**
   * A double constant representing the default Bayesian depth error in kilometers (68th percentile)
   * for a free depth solution. Note that this represents the strength of the Bayesian constraint.
   * There is no problem with the default depth minus the error being negative.
   */
  public static final double DEFAULTDEPTHSE = 5d;

  /**
   * A double constant representing the mid-crustal depth in kilometers (roughly the depth to the
   * Conrad discontinuity, if any).
   */
  public static final double MIDCRUSTDEPTH = 20d;

  /**
   * A double constant representing the default standard error for shallow events in the lower
   * crust.
   */
  public static final double LOWERCRUSTSE = 15d;

  /**
   * If the statistics are any good, the deepest shallow event should be no deeper than the default
   * shallow depth plus three standard deviations (i.e., a 99% level).
   */
  public static final double DEEPESTSHALLOW = DEFAULTDEPTH + 3d * DEFAULTDEPTHSE;

  /**
   * The old Zone statistics change behavior between the shallow and deep regimes defined by this
   * boundary in kilometers.
   */
  public static final double SHALLOWESTDEEP = 150d;

  /**
   * Tolerance in kilometers across a facet of the global ZoneStat tessellation. Zone depths greater
   * than the tolerance from the closest point will be dropped.
   */
  public static final double[] STRUCTURETOL = {60d, 150d};

  /**
   * A double containing the maximum trial depth in kilometers where the Bayesian depth can be
   * shallow if a slab is present.
   */
  public static final double SLABMAXSHALLOWDEPTH = 50d;

  /**
   * A double containing the minimum slab depth in kilometers where the Bayesian depth spread will
   * cover both the slab and shallow earthquakes.
   */
  public static final double SLABMERGEDEPTH = 80d;

  /**
   * A double constant representing the typical slab earthquake depth error in kilometers (99th
   * percentile).
   */
  public static final double DEFAULTSLABSE = 30d;

  /** The minimum slab latitude-longitude grid spacing. Used to separate tilted slabs rows. */
  public static final double MINSLABINCREMENT = 0.05d;

  /** Minimum slab latitude-longitude spacing to define a new tilted slabs area */
  public static final double TILTEDAREAINCREMENT = 7.0d;

  /** The strength of the Bayesian condition relative to the pick data. */
  public static final double BAYESIANSTRENGTH = 1d;

  /** A double constant representing the factor to down weight undesirable phase identifications. */
  public static final double DOWNWEIGHT = 0.5d;

  /**
   * A double constant representing the factor to down weight phases that don't match, but are in
   * the same group.
   */
  public static final double GROUPWEIGHT = 0.5d;

  /**
   * A double constant representing the factor to down weight phases that don't even match in type.
   */
  public static final double TYPEWEIGHT = 0.1d;

  /** A double constant representing the default trial affinity when the phases don't match. */
  public static final double NULLAFFINITY = 1d;

  /**
   * A double constant representing the minimum acceptable observability for an alternative phase
   * identification.
   */
  public static final double OBSERVABILITYMIN = 1d;

  /** A double constant representing the association tolerance in seconds. */
  public static final double ASSOCTOLERANCE = 60d;

  /**
   * A double constant representing the maximum acceptable ratio of apparently misidentified first
   * arrivals that are being used to total used stations.
   */
  public static final double BADRATIO = 0.1d;

  /**
   * A double constant representing the conversion factor from degrees to kilometers (assumes the
   * radius of the Earth model is 6371 kilometers).
   */
  public static final double DEG2KM = 6371d * Math.PI / 180d;

  /** An int constant representing the maximum number of iteration stages to attempt. */
  /** JMP 1/13/2022 Added a third stage due to large number of events that did not converge */
  public static final int STAGELIMIT = 3;

  /** A double constant representing the initial step length to start each iteration loop with. */
  public static final double INITIALSTEPLEN = 50d;

  /** An array of int constants representing the maximum number of iterations for each stage. */
  /** JMP 1/13/2022 Added a third stage due to large number of events that did not converge */
  public static final int[] ITERATIONSTAGELIMITS = {15, 20, 30};

  /**
   * An array of double constants representing the convergence criteria in kilometers for each
   * stage.
   */
  /** JMP 1/13/2022 Added a third stage due to large number of events that did not converge */
  public static final double[] CONVERGENCESTAGELIMITS = {1d, 0.1d, 0.1d};

  /**
   * An array of double constants representing the maximum step length in kilometers to allow for
   * each stage.
   */
  /** JMP 1/13/2022 Added a third stage due to large number of events that did not converge */
  public static final double[] STEPLENSTAGELIMITS = {200d, 50d, 25d};

  /**
   * A double constant representing the step tolerance dividing "did not converge" from "unstable
   * solution".
   */
  public static final double STEPTOLERANCE = 20d;

  /** A double constant representing the chi-squared test for "nearly converged". */
  public static final double ALMOSTCONVERGED = 1.1d;

  /**
   * A double constant representing the maximum distance from the epicenter in degrees considered
   * local for the purposes of GT5.
   */
  public static final double GT5LOCALDISTMAX = 250 / DEG2KM;

  /**
   * A double constant representing the normalization to make the median absolute deviation (MAD,
   * also referred to as the spread in the Locator) match the standard deviation for a Gaussian
   * distribution.
   */
  public static final double MADNORM = 1.482580d;

  /**
   * A double constant representing the scale raw marginal confidence intervals to 90th percentiles.
   */
  public static final double PERPT1D = 2.0285161d;

  /**
   * A double constant representing the scale 2-D (i.e., epicenter) confidence intervals to 90th
   * percentiles.
   */
  public static final double PERPT2D = 2.6465147d;

  /**
   * A double constant representing the scale 3-D (i.e., hypocenter) confidence intervals to 90th
   * percentiles.
   */
  public static final double PERPT3D = 3.0834703d;

  /**
   * An array of double constants representing the tangential (horizontal) limits on the equivilent
   * error radius of the error ellipse for quality levels.
   */
  public static final double[] HORIZONTALQUALIMITS = {8.5d, 16d, 60d};

  /**
   * An array of double constants representing the vertical (radial) limits on the depth standard
   * error for quality levels.
   */
  public static final double[] VERTICALQUALIMITS = {16d, 30d, 75d};

  /**
   * An array of double constants representing the limits on the maximum length of the error ellipse
   * axis for quality levels.
   */
  public static final double[] SEMILENQUALIMITS = {42.5d, 80d, 300d};

  /**
   * An array of int constants representing the limits on the number of phases used for quality
   * levels.
   */
  public static final int[] NPHASESQUALIMITS = {6, 2};

  /**
   * A double constant representing the slope for compensating for the effective number of data if
   * decorrelation is not used.
   */
  public static final double EFFSLOPE = 0.309d;

  /**
   * A double constant representing the offset for compensating for the effective number of data if
   * decorrelation is not used.
   */
  public static final double EFFOFFSET = 1.22d;

  /**
   * An int constant representing the maximum number of picks to decorrelate. Note that the magic
   * number in the Fortran version was 450, but that included the Bayesian depth, which is excluded
   * here.
   */
  public static final int MAXPICKSTODECORRELATE = 449;

  /**
   * A double constant representing the limit of largest eigenvalues (adding up to at least 95%) to
   * keep when decorrelating.
   */
  public static final double EIGENVALUESLIMIT = 0.95d;

  /**
   * A double constant representing the limit for eliminating eigenvalues larger than 1% of the size
   * of the largest eigenvalue When decorrelating.
   */
  public static final double EIGENVALUESTHRESH = 0.01d;

  /**
   * An array of string constants representing the phases to use in locations. The Locator always
   * uses all phases (i.e., a null phase list).
   */
  public static final String[] PHASELIST = null;

  /**
   * A boolean constant that if true indicates that the locator should suppress phases that are
   * unlikely to be observed.
   */
  public static final boolean SUPRESSUNLIKELYPHASES = false;

  /** A boolean constant that if true indicates that the locator should suppress back branches. */
  public static final boolean SUPRESSBACKBRANCHES = false;

  /**
   * A boolean flag indicating if the decorrelation algorithm is to be used. Note that it is never
   * used in the initial pass, hence the default.
   */
  public static boolean useDecorrelation = false;

  /**
   * A boolean flag indicating if the the event is in a tectonic area where Pb and Sb are extensions
   * of Pg and Sg respectively or in a craton with a well defined Conrad discontinuity and distinct
   * Pb and Sb phases.
   */
  public static boolean isTectonic = false;

  /** Normally false. Set true only if this is a synthetic bayesian depth test. */
  public static final boolean isSynthetic = true;

  /**
   * A double containing the receiver azimuth relative to the source in degrees clockwise from north
   * (available after calling computeDistAzm).
   */
  public static double azimuth = Double.NaN;

  // The following are private constants and variables used by static
  // methods below:

  /**
   * A double constant representing the covariance delta offset needed by computeCovariance
   * calculations.
   */
  private static final double COVOFFSET = 15d;

  /**
   * A double constant representing the covariance constant needed by computeCovariance
   * calculations.
   */
  private static final double COVCONST = 37.5d;

  /**
   * A double constant representing the covariance power needed by computeCovariance calculations.
   */
  private static final double COVPOWER = 0.4d;

  /**
   * A double constant representing the model spread power needed by computePDFResValue
   * calculations.
   */
  private static final double TTMODELSPREAD = 1.001691d;

  /**
   * A double constant representing the Fraction of Cauchy/Gaussian needed by computePDFResValue
   * calculations.
   */
  private static final double CAUCHYFRACTION = 0.45d;

  /**
   * A double constant representing the the Cauchy spread needed by computePDFResValue calculations.
   */
  private static final double CAUCHYSPREAD = 0.78d / TTMODELSPREAD;

  /**
   * A double constant representing the Cauchy normalization needed by computePDFResValue
   * calculations.
   */
  private static final double CAUCHYNORM = CAUCHYFRACTION / Math.PI;

  /**
   * A double constant representing the the Cauchy Gaussian needed by computePDFResValue
   * calculations.
   */
  private static final double GAUSSIANSPREAD = 0.92d / TTMODELSPREAD;

  /**
   * A double constant representing the Gaussian normalization needed by computePDFResValue
   * calculations.
   */
  private static final double GAUSSIANNORM = (1d - CAUCHYFRACTION) / Math.sqrt(2d * Math.PI);

  /**
   * A double constant representing the minimum distance to boost the figure-of-merit, used in
   * computeDistCorr calculations.
   */
  private static final double CORRELATIONDISTMIN = 20d;

  /**
   * A double constant representing the factor to boost the figure-of-merit, used in computeDistCorr
   * calculations.
   */
  private static final double CORRELATIONDISTFACTOR = 0.067d;

  /**
   * A double constant representing the residual validity limit slope, used in the
   * computeValidityLimit calculation.
   */
  private static final double RESVALIDITYSLOPE = 2.27d;

  /**
   * A double constant representing the residual validity limit offset, used in the
   * computeValidityLimit calculation.
   */
  private static final double RESVALIDITYOFFSET = 5d;

  /**
   * A double constant representing the anti-loop jiggle dampening value, used by
   * computeDampeningFactor.
   */
  private static double DAMPENINGVALUE = 0.45d;

  /**
   * A double constant representing the anti-loop jiggle dampening limit, used by
   * computeDampeningFactor.
   */
  private static final double DAMPENINGLIMIT = 0.58984375d + (DAMPENINGVALUE - 0.375d);

  /** Constants needed by isGT5. */
  /**
   * An int constant representing the minimum number of used local phases for a GT5 solution, used
   * by isGT5.
   */
  private static final int MINGT5LOCALPHASES = 10;

  /**
   * A double constant representing the maximum distance to the nearest used station in degrees for
   * a GT5 solution, used by isGT5.
   */
  private static final double MAXGT5STADIST = 30d / DEG2KM;

  /**
   * A double constant representing the maximum azimuthal gap in degrees for a GT5 solution, used by
   * isGT5.
   */
  private static final double MAXGT5AZMGAP = 110d;

  /**
   * A double constant representing the maximum robust azimuthal gap in degrees for a GT5 solution,
   * used by isGT5.
   */
  private static final double MAXGT5AZMLESTGAP = 160d;

  /**
   * A buffered writer handle for recording bits and pieces from around the Locator for debugging
   * experiments.
   */
  private static BufferedWriter recordOut = null;
  /*
   * A double representing the system time, used as a timer. Needed by
   * startTimer and endTimer.
   */
  private static long systemTime;

  /*
   * A double representing the system time, used as a timer. Needed by
   * startLocationTimer and endLocationTimer.
   */
  private static long locationTime;

  /**
   * This function computes the source-receiver distance and the receiver azimuth. An historically
   * significant subroutine from deep time (1962)! This routine was written by Bob Engdahl in
   * Fortran (actually in the days before subroutines) and beaten into it's current Fortran form by
   * Ray Buland in the early 1980s. It's optimized with respect to computing sines and cosines
   * (probably still worthwhile) and it computes exactly what's needed--no more, no less. Note that
   * the azimuth is returned in static variable azimuth.
   *
   * @param hypo A Hypocenter object containing the hypocenter to compute the the source-receiver
   *     distance and the receiver azimuth
   * @param sta A Station object containing the station to compute the the source-receiver distance
   *     and the receiver azimuth
   * @return A double containing the distance (delta) in degrees. the distance in degrees is
   *     returned in the azimuth class variable
   */
  public static double computeDistAzm(Hypocenter hypo, Station sta) {
    // South Pole (only tests the station because the South Pole is aseismic).
    if (sta.getCoLatitudeSine() <= TauUtil.DTOL) {
      azimuth = 180d;
      return Math.toDegrees(Math.PI - Math.acos(hypo.getCoLatitudeCosine()));
    }

    // Compute some intermediate variables.
    // Use Bob Engdahl's variable names
    double cosdel =
        hypo.getCoLatitudeSine()
                * sta.getCoLatitudeSine()
                * (sta.getLongitudeCosine() * hypo.getLongitudeCosine()
                    + sta.getLongitudeSine() * hypo.getLongitudeSine())
            + hypo.getCoLatitudeCosine() * sta.getCoLatitudeCosine();

    double tm1 =
        sta.getCoLatitudeSine()
            * (sta.getLongitudeSine() * hypo.getLongitudeCosine()
                - sta.getLongitudeCosine() * hypo.getLongitudeSine());

    double tm2 =
        hypo.getCoLatitudeSine() * sta.getCoLatitudeCosine()
            - hypo.getCoLatitudeCosine()
                * sta.getCoLatitudeSine()
                * (sta.getLongitudeCosine() * hypo.getLongitudeCosine()
                    + sta.getLongitudeSine() * hypo.getLongitudeSine());

    double sindel = Math.sqrt(Math.pow(tm1, 2d) + Math.pow(tm2, 2d));

    // Compute the azimuth.
    if (Math.abs(tm1) <= TauUtil.DTOL && Math.abs(tm2) <= TauUtil.DTOL) {
      // North Pole.
      azimuth = 0d;
    } else {
      azimuth = Math.toDegrees(Math.atan2(tm1, tm2));

      if (azimuth < 0d) {
        azimuth += 360;
      }
    }

    // Compute distance (delta).
    if (sindel <= TauUtil.DTOL && Math.abs(cosdel) <= TauUtil.DTOL) {
      return 0d;
    } else {
      return Math.toDegrees(Math.atan2(sindel, cosdel));
    }
  }

  /**
   * This function computes the epicentral distance between a hypocenter and an audit record.
   *
   * @param hypo A Hypocenter object containing the hypocenter information
   * @param audit A HypoAudit object containing the Hypocenter audit information
   * @return A double containing the distance between the hypocenter and the audit record in
   *     kilometers
   */
  public static double computeDistance(Hypocenter hypo, HypoAudit audit) {
    // Compute some intermediate variables.
    // Use Bob Engdahl's variable names
    double cosdel =
        hypo.getCoLatitudeSine()
                * audit.getCoLatitudeSine()
                * (audit.getLongitudeCosine() * hypo.getLongitudeCosine()
                    + audit.getLongitudeSine() * hypo.getLongitudeSine())
            + hypo.getCoLatitudeCosine() * audit.getCoLatitudeCosine();

    double tm1 =
        audit.getCoLatitudeSine()
            * (audit.getLongitudeSine() * hypo.getLongitudeCosine()
                - audit.getLongitudeCosine() * hypo.getLongitudeSine());

    double tm2 =
        hypo.getCoLatitudeSine() * audit.getCoLatitudeCosine()
            - hypo.getCoLatitudeCosine()
                * audit.getCoLatitudeSine()
                * (audit.getLongitudeCosine() * hypo.getLongitudeCosine()
                    + audit.getLongitudeSine() * hypo.getLongitudeSine());

    double sindel = Math.sqrt(Math.pow(tm1, 2d) + Math.pow(tm2, 2d));

    // Compute distance (delta).
    if (sindel <= TauUtil.DTOL && Math.abs(cosdel) <= TauUtil.DTOL) {
      return 0d;
    } else {
      return DEG2KM * Math.toDegrees(Math.atan2(sindel, cosdel));
    }
  }

  /**
   * This function compares a hypocenter to an audit record.
   *
   * @param hypo A Hypocenter object containing the hypocenter information
   * @param audit A HypoAudit object containing the Hypocenter audit information
   * @return True if the hypocenter and audit record are (nearly) the same
   */
  public static boolean compareHypos(Hypocenter hypo, HypoAudit audit) {
    return Math.abs(hypo.getOriginTime() - audit.getOriginTime()) <= 0.01d
        && Math.abs(hypo.getLatitude() - audit.getLatitude()) <= 0.0001d
        && Math.abs(hypo.getLongitude() - audit.getLongitude()) <= 0.0001d
        && Math.abs(hypo.getDepth() - audit.getDepth()) <= 0.01d;
  }

  /**
   * This function computes the empirical covariance between two picks. The covariance form was
   * developed by Bondar and McLauglin (BSSA, vol. 99, pp 172-193). The constants were fit by Buland
   * based on first arriving data from four years of Chicxulub data.
   *
   * @param pick1 A Pick object containing information for the first pick
   * @param pick2 A Pick object containing information for the second pick
   * @return A double containing the covariance between pick1 and pick2
   */
  public static double computeCovariance(Pick pick1, Pick pick2) {
    // Do the autocorrelation.
    if (pick1.equals(pick2)) {
      return 1d / (pick1.getWeight() * pick2.getWeight());
    }

    // Assume the correlation between different phases is zero.
    if (!pick1.getCurrentPhaseCode().equals(pick2.getCurrentPhaseCode())) {
      return 0d;
    }

    // Otherwise, we have to compute it.
    Station sta1 = pick1.getStation();
    Station sta2 = pick2.getStation();

    // South Pole.
    double delta;
    if (sta1.getCoLatitudeSine() <= TauUtil.DTOL) {
      delta = Math.toDegrees(Math.PI - Math.acos(sta2.getCoLatitudeCosine()));
    } else if (sta2.getCoLatitudeSine() <= TauUtil.DTOL) {
      delta = Math.toDegrees(Math.PI - Math.acos(sta1.getCoLatitudeCosine()));
    } else {
      // Compute some intermediate variables.
      // Use Bob Engdahl's variable names
      double cosdel =
          sta1.getCoLatitudeSine()
                  * sta2.getCoLatitudeSine()
                  * (sta2.getLongitudeCosine() * sta1.getLongitudeCosine()
                      + sta2.getLongitudeSine() * sta1.getLongitudeSine())
              + sta1.getCoLatitudeCosine() * sta2.getCoLatitudeCosine();

      double tm1 =
          sta2.getCoLatitudeSine()
              * (sta2.getLongitudeSine() * sta1.getLongitudeCosine()
                  - sta2.getLongitudeCosine() * sta1.getLongitudeSine());

      double tm2 =
          sta1.getCoLatitudeSine() * sta2.getCoLatitudeCosine()
              - sta1.getCoLatitudeCosine()
                  * sta2.getCoLatitudeSine()
                  * (sta2.getLongitudeCosine() * sta1.getLongitudeCosine()
                      + sta2.getLongitudeSine() * sta1.getLongitudeSine());

      double sindel = Math.sqrt(Math.pow(tm1, 2d) + Math.pow(tm2, 2d));

      // Compute distance (delta).
      if (sindel <= TauUtil.DTOL && Math.abs(cosdel) <= TauUtil.DTOL) {
        delta = 0d;
      } else {
        delta = Math.toDegrees(Math.atan2(sindel, cosdel));
      }
    }

    // Compute covariance.
    double covariance =
        (1d - Math.pow(delta / (Math.abs(delta - COVOFFSET) + COVCONST), COVPOWER))
            / (pick1.getWeight() * pick2.getWeight());

    return covariance;
  }

  /**
   * This function calculates the derivative of travel time with respect to latitude.
   *
   * @param ttDerivativeDistance A double containing the derivative of travel time with respect to
   *     distance in seconds/degree
   * @param azimuth A double containing the azimuth of the receiver from the source in degrees
   * @return A double containing the derivative of travel time with respect to latitude in
   *     seconds/kilometer
   */
  public static double computeTTLatDerivative(double ttDerivativeDistance, double azimuth) {
    return Math.cos(Math.toRadians(azimuth)) * ttDerivativeDistance / DEG2KM;
  }

  /**
   * This function calculates the derivative of travel time with respect to longitude.
   *
   * @param ttDerivativeDistance A double containing the derivative of travel time with respect to
   *     distance in seconds/degree
   * @param azimuth A double containing the azimuth of the receiver from the source in degrees
   * @return A double containing the derivative of travel time with respect to longitude in
   *     seconds/kilometer
   */
  public static double computeTTLonDerivative(double ttDerivativeDistance, double azimuth) {
    return -Math.sin(Math.toRadians(azimuth)) * ttDerivativeDistance / DEG2KM;
  }

  /**
   * This function computes the probability density function value for the desired residual using
   * the Buland statistical model. The canonical Buland statistical model for travel-time residuals
   * is a linear combination of a Gaussian and a Cauchy distribution. In practice, the canonical
   * model must be adapted for the median and spread of the phase of interest. This method then
   * calculates the value of the phase probability density function at the desired residual.
   *
   * @param residual A double containing the travel-time residual in seconds
   * @param median A double containing the median probability density function time in seconds
   *     relative to the theoretical travel time (usually zero)
   * @param spread A double containing the probability density function spread in seconds for the
   *     desired phase
   * @return A double containing the probability density function value for the desired residual
   */
  public static double computePDFResValue(double residual, double median, double spread) {
    // Account for the current distribution median and spread.
    double gaussSpread = spread * GAUSSIANSPREAD;
    double gaussVar = (residual - median) / gaussSpread;
    double cauchySpread = spread * CAUCHYSPREAD;
    double cauchyVar = (residual - median) / cauchySpread;

    // Calculate the overall normalization.
    double ttResNorm = GAUSSIANNORM / gaussSpread + CAUCHYNORM / cauchySpread;

    // Return the result.
    return (GAUSSIANNORM * Math.exp(-0.5d * Math.pow(gaussVar, 2d)) / gaussSpread
            + CAUCHYNORM / (cauchySpread * (1d + Math.pow(cauchyVar, 2d))))
        / ttResNorm;
  }

  /**
   * The proximity boost is designed to boost the probability of a set of phases if a lower
   * probability phase happens to have a freakishly small residual. The phase probability should be
   * multiplied by the boost factor. Note that if the most probable phase also has a small residual,
   * both would get a boost and the more probably phase would (probably) still win.
   *
   * @param residual Travel-time residual in seconds
   * @return Proximity boost factor
   */
  public static double computeProximityBoost(double residual) {
    // This gives a boost of 15 for a zero residual and no boost for residuals over 3 s.
    return Math.max(15d - 4.67d * Math.abs(residual), 1d);
  }

  /**
   * This function computes a crude correction to the figure-of-merit to make phase identifications
   * at near distances more likely. This is principally a problem for the closest station to a
   * subduction event due to the complex structure. It should only be applied to the first arrival
   * in any pick group.
   *
   * @param distance A double containing the distance in degrees
   * @return Correction to the phase association figure-of-merit
   */
  public static double computeDistCorr(double distance) {
    if (distance < CORRELATIONDISTMIN) {
      return 1d + CORRELATIONDISTFACTOR * (CORRELATIONDISTMIN - distance);
    } else {
      return 1d;
    }
  }

  /**
   * This function computes the emperical validity limit. To be a valid association, the travel-time
   * residual must be smaller than the validity limit determined from the spread. This is, of
   * course, a purely empirical limit based on years of Hydra experience.
   *
   * @param spread A double containing the statistical spread in seconds
   * @return A double containing the empirical validity limit
   */
  public static double computeValidityLimit(double spread) {
    return RESVALIDITYSLOPE * (spread - 1d) + RESVALIDITYOFFSET;
  }

  /**
   * This function computes an updated dampening factor. Sometimes you can jiggle a machine to jog
   * it out of a rut. In this case the damping factor is jiggled to avoid loops when step length
   * damping is required. Note that, the step length will be damped by multiplying it by the damping
   * factor.
   *
   * @return A double containing an updated damping factor
   */
  public static double computeDampeningFactor() {
    if (DAMPENINGVALUE <= DAMPENINGLIMIT) {
      DAMPENINGVALUE += 0.0390625d;
    } else {
      DAMPENINGVALUE -= 0.21875d;
    }

    return DAMPENINGVALUE;
  }

  /**
   * This function tests whether this event meets the criteria for the old "ground truth within 5
   * kilometers" (GT5) algorithm.
   *
   * @param locPhUsed An int containing the number of local phases used
   * @param delMin A double containing the distance to the closest station used in degrees
   * @param azimGap A double containing the azimuthal gap in degrees
   * @param azimLestGap A double containing the robust azimuthal gap in degrees
   * @return A boolean flag, True if this event qualifies as GT5
   */
  public static boolean isGT5(int locPhUsed, double delMin, double azimGap, double azimLestGap) {
    return locPhUsed >= MINGT5LOCALPHASES
        && delMin <= MAXGT5STADIST
        && azimGap < MAXGT5AZMGAP
        && azimLestGap < MAXGT5AZMLESTGAP;
  }

  /**
   * This function normalizes an arbitrary vector to a 2-norm unit vector.
   *
   * @param vector A double[] containing the unit vector
   * @return A double[] containing the normalized unit vector
   */
  public static double[] normalizeUnitVector(double[] vector) {
    // Be sure we have a valid vector.
    if (vector == null) {
      return vector;
    }
    if (vector.length < 1) {
      return vector;
    }

    // Compute the 2-norm.
    double sum = 0d;
    for (int j = 0; j < vector.length; j++) {
      sum += Math.pow(vector[j], 2d);
    }

    // Bail if the vector is all zeros.
    if (sum == 0d) {
      return vector;
    }

    // Remove the norm.
    sum = Math.sqrt(sum);
    for (int j = 0; j < vector.length; j++) {
      vector[j] /= sum;
    }

    return vector;
  }

  /**
   * This fuction converts from Java standard time in milliseconds since the epoch as a long to
   * Hydra time in seconds since the epoch as a double.
   *
   * @param time A long containing the Java standard time (epoch milliseconds)
   * @return A double containing the Hydra standard time (epoch decimal seconds)
   */
  public static double toHydraTime(long time) {
    return 0.001d * time;
  }

  /**
   * This fuction converts from Hydra time in seconds since the epoch as a double to Java standard
   * time in milliseconds since the epoch as a long.
   *
   * @param time A double containing the Hydra standard time
   * @return A double containing the Hydra standard time
   */
  public static long toJavaTime(double time) {
    return (long) (1000d * time);
  }

  /**
   * This function produces a time string from a Hydra time suitable for printing. Hydra uses
   * doubles instead of longs, but (conveniently) the same epoch. The string returned is valid to
   * milliseconds and uses 24-hour times.
   *
   * @param time A double containing the Hydra standard time (epoch decimal seconds)
   * @return A String containing the formatted Time string
   */
  public static String getTimeString(double time) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.setTime(new Date(toJavaTime(time)));

    return String.format("%1$tH:%1$tM:%1$tS.%1$tL", cal);
  }

  /**
   * This function produces a date-time string from a Hydra time suitable for printing. Hydra uses
   * doubles instead of longs, but (conveniently) the same epoch. The string returned is valid to
   * milliseconds and uses 24-hour times.
   *
   * @param time A double containing the Hydra standard time (epoch decimal seconds)
   * @return A String containing the formatted Date-time string
   */
  public static String getDateTimeString(double time) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.setTime(new Date(toJavaTime(time)));

    return String.format("%1$td-%1$tb-%1$ty %1$tH:%1$tM:%1$tS.%1$tL", cal);
  }

  /**
   * This function produces a time string from a Hydra time suitable for printing in the NEIC web
   * bulletin style.
   *
   * @param time A double containing the Hydra standard time (epoch decimal seconds)
   * @return A String containing the NEIC formatted Time string
   */
  public static String getNEICTimeString(double time) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.setTime(new Date(toJavaTime(time)));

    return String.format("%1$tH:%1$tM:%1$tS.%1$tL", cal).substring(0, 11);
  }

  /**
   * This function produces a date-time string from a Hydra time suitable for printing in the NEIC
   * web bulletin style.
   *
   * @param time A double containing the Hydra standard time (epoch decimal seconds)
   * @return A String containing the NEIC formatted Date-time string
   */
  public static String getNEICDateTimeString(double time) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.setTime(new Date(toJavaTime(time)));

    return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL", cal);
  }

  /**
   * This function formats latitude for printing.
   *
   * @param latitude A double containing the signed geographic latitude in degrees
   * @return A String containing the latitude formatted suitably for a bulletin
   */
  public static String formatLat(double latitude) {
    if (latitude >= 0) {
      return String.format("%6.3f°N", latitude);
    } else {
      return String.format("%6.3f°S", -latitude);
    }
  }

  /**
   * Format longitude for printing.
   *
   * @param longitude A double containing the signed geographic longitude in degrees
   * @return A String containing the longitude formatted suitably for a bulletin
   */
  public static String formatLon(double longitude) {
    if (longitude >= 0) {
      return String.format("%7.3f°E", longitude);
    } else {
      return String.format("%7.3f°W", -longitude);
    }
  }

  /**
   * This function get the numeric authority code from the enumerated author types.
   *
   * @param author An AuthorType object containing the author type
   * @return An int containing the numeric authority code
   */
  public static int getNumericAuthCode(AuthorType author) {
    return author.ordinal();
  }

  /**
   * This function gets the AuthorType from the numeric code.
   *
   * @param authCode An int containing the numeric authority code
   * @return An AuthorType object containing the author type
   */
  public static AuthorType getAuthCodeFromNumericCode(int authCode) {

    if (authCode == 1) {
      return AuthorType.CONTRIB_AUTO;
    } else if (authCode == 2) {
      return AuthorType.LOCAL_AUTO;
    } else if (authCode == 3) {
      return AuthorType.CONTRIB_HUMAN;
    } else if (authCode == 4) {
      return AuthorType.LOCAL_HUMAN;
    }

    return AuthorType.UNKNOWN;
  }

  /**
   * This function translates the FORTRAN style 'T' / 'F' to Java style true / false.
   *
   * @param log A char containing the boolean style character, 'T' for true or 'F' for false
   * @return A boolean containing the equivalent boolean value
   */
  public static boolean getBoolean(char log) {
    return log == 'T';
  }

  /**
   * This function translates the Java style true / false to FORTRAN style 'T' / 'F'.
   *
   * @param log A boolean value to translate
   * @return A char containing the boolean style character, 'T' for true or 'F' for false
   */
  public static char getBoolChar(boolean log) {
    if (log) {
      return 'T';
    } else {
      return 'F';
    }
  }

  /**
   * This function extracts a number from a JSON object and returns it as an int. JSON only knows
   * about String, Double, and Long. Getting a primitive int generally requires converting a Long
   * into an int.
   *
   * @param JSONobj A JSON object
   * @param key Key string for the desired field
   * @return The field converted to an int
   */
  public static int getJSONInt(JSONObject JSONobj, String key) {
    Number tempNumber;

    try {
      // Assume we have a number (or a null).
      tempNumber = (Number) JSONobj.get(key);
      if (tempNumber != null) {
        return tempNumber.intValue();
      } else {
        return -1;
      }
    } catch (ClassCastException e) {
      // Apparently, we have something else (probably a String).
      return -1;
    }
  }

  /**
   * This function extracts a number from a JSON object and returns it as a double. JSON only knows
   * about String, Double, and Long. Getting a primitive double requires converting either a Double
   * or Long into a double.
   *
   * @param JSONobj A JSON object
   * @param key Key string for the desired field
   * @return The field converted to a double
   */
  public static double getJSONDouble(JSONObject JSONobj, String key) {
    Number tempNumber;

    try {
      // Assume we have a number (or a null).
      tempNumber = (Number) JSONobj.get(key);
      if (tempNumber != null) {
        return tempNumber.doubleValue();
      } else {
        return Double.NaN;
      }
    } catch (ClassCastException e) {
      // Apparently, we have something else (probably a String).
      return Double.NaN;
    }
  }

  /**
   * This function prints a double vector to the screen for debugging purposes.
   *
   * @param a A double[] containing the vector to print
   * @param label A String containing the label to print as a header
   * @return A String containing the vector
   */
  public static String printVector(double[] a, String label) {
    String vectorString = label + ":\n\t";
    int count = 1;

    for (int j = 0; j < a.length; j++) {
      if (count > 8) {
        vectorString += "\n\t";
        count = 1;
      }

      vectorString += String.format(" %10.3e", a[j]);
      count++;
    }

    return vectorString;
  }

  /**
   * This function prints an int vector to the screen for debugging purposes.
   *
   * @param a an int[] containing the vector to print
   * @param label A String containing the label to print as a header
   * @return A String containing the vector
   */
  public static String printVector(int[] a, String label) {
    String vectorString = label + ":\n\t";

    int count = 1;
    for (int j = 0; j < a.length; j++) {
      if (count > 17) {
        vectorString += "\n\t";
        count = 1;
      }

      vectorString += String.format(" %4d", a[j]);
      count++;
    }

    return vectorString;
  }

  /**
   * This function prints a double matrix to the screen for debugging purposes.
   *
   * @param a A double[][] containing the matrix to print
   * @param label A String containing the label to print as a header
   * @return A String containing the matrix
   */
  public static String printMatrix(double[][] a, String label) {
    String matrixString = label + ":\n\t";

    for (int i = 0; i < a.length; i++) {
      int count = 0;
      for (int j = 0; j < a[i].length; j++) {
        if (count > 8) {
          matrixString += "\n\t";
          count = 1;
        }

        matrixString += String.format(" %10.3e", a[i][j]);
        count++;
      }

      matrixString += "\n";
    }

    return matrixString;
  }

  /**
   * Simple file writer for gathering information from throughout the Locator for various
   * experiments. Note this is quick and dirty. The file name is hard wired, so it will be
   * overwritten each time it is opened. If the string input is null, the file will be closed.
   *
   * @param line Text to write the the Record.txt file.
   */
  public static void record(String line) {
    if (line != null) {
      if (recordOut == null) {
        try {
          recordOut = new BufferedWriter(new FileWriter("../../LocRun/output/Record.txt"));
        } catch (IOException e) {
          System.out.println("Unable to open the Record.txt file");
        }
      }
      try {
        recordOut.write(line + "\n");
      } catch (IOException e) {
        System.out.println("Unable to record: " + line);
      }
    } else {
      if (recordOut != null) {
        try {
          recordOut.close();
        } catch (IOException e) {
          System.out.println("Unable to close the Record.txt file");
        }
      }
    }
  }

  /**
   * This timer function sets the systemTime variable to the current system time in milliseconds.
   * This function is used in conjunction with endTimer
   */
  public static void startTimer() {
    systemTime = System.currentTimeMillis();
  }

  /**
   * This timer function ends the timer and returns a string holding the result in seconds.
   *
   * @param label A String used to identify the timer
   * @return A String containing the timer results
   */
  public static String endTimer(String label) {
    //    String timerString = label + " time: " + 0.001 * (System.currentTimeMillis() -
    // systemTime);
    String timerString =
        String.format("%s time:%7.3f", label, 0.001 * (System.currentTimeMillis() - systemTime));

    return (timerString);
  }

  /**
   * This timer function sets the locationTime variable to the current system time in milliseconds.
   * This function is used in conjunction with endLocationTimer
   */
  public static void startLocationTimer() {
    locationTime = System.currentTimeMillis();
  }

  /**
   * This timer function ends the location timer and returns a string holding the result in seconds.
   *
   * @return A String containing the timer results
   */
  public static String endLocationTimer() {
    String timerString =
        String.format(
            "Compute time %7.3f seconds", 0.001 * (System.currentTimeMillis() - locationTime));

    return (timerString);
  }

  /**
   * This timer function returns a string holding the result in seconds.
   *
   * @param label A String used to identify the timer
   * @param startTime A long value containing the start time of the timer
   * @return A String containing the timer results
   */
  public static String endTimer(String label, long startTime) {
    long currentTime = System.currentTimeMillis();
    String dateString = getNEICDateTimeString(toHydraTime(currentTime));

    String timerString =
        String.format("%s: %s: %7.3f", dateString, label, 0.001 * (currentTime - startTime));

    return (timerString);
  }
}
