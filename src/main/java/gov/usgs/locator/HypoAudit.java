package gov.usgs.locator;

/**
 * The HypoAudit class is intended to provide a snapshot of the event at different points in the
 * location process for logging purposes. It also provides a handy way of backing down to a previous
 * hypocenter should the location iteration go awry.
 *
 * @author Ray Buland
 */
public class HypoAudit {
  /** An int containing the location stage of this audit record. */
  private int stage;

  /** An int containing the location iteration of this audit record. */
  private int iteration;

  /** An int containing the number of picks used for this audit record. */
  private int numPicksUsed;

  /** A double containing the origin time in seconds since the epoch for this audit record. */
  private double originTime;

  /** A double containing the geographic latitude in degrees for this audit record. */
  private double latitude;

  /** A double containing the geographic longitude in degrees for this audit record. */
  private double longitude;

  /** A double containing the depth in kilometers for this audit record. */
  private double depth;

  /** A double containing the hypocentral change in kilometers for this audit record. */
  private double hypocentralChange;

  /** A double containing the epicentral change in kilometers for this audit record. */
  private double epicentralChange;

  /** A double containing the depth change in kilometers for this audit record. */
  private double depthChange;

  /** A double containing the standard error of the origin time in seconds. */
  private double timeStandardError;

  /** A LocStatus object holding the hypocenter status for this audit record. */
  private LocStatus locationStatus;

  /** A double containing the geocentric colatitude in degrees. */
  private double coLatitude;

  /** A double containing the sine of the geocentric colatitude in degrees. */
  private double coLatitudeSine;

  /** A double containing the cosine of the geocentric colatitude in degrees. */
  private double coLatitudeCosine;

  /** A double containing the sine of the geocentric longitude in degrees. */
  private double longitudeSine;

  /** A double containing the cosine of the geocentric longitude in degrees. */
  private double longitudeCosine;

  /**
   * The HypoAudit constructor. This counstructor creates an audit record from the provided values.
   *
   * @param hypo A Hypocenter object containing the hypocentral information for this audit.
   * @param stage An int containing the current location stage
   * @param iteration An int containing the current location iteration
   * @param numPicksUsed An int containing the number of picks currently being used
   * @param locationStatus A LocStatus object holding the current location status
   */
  public HypoAudit(
      Hypocenter hypo, int stage, int iteration, int numPicksUsed, LocStatus locationStatus) {
    this.stage = stage;
    this.iteration = iteration;
    this.numPicksUsed = numPicksUsed;
    this.locationStatus = locationStatus;
    originTime = hypo.getOriginTime();
    latitude = hypo.getLatitude();
    longitude = hypo.getLongitude();
    depth = hypo.getDepth();
    hypocentralChange = hypo.getStepLength();
    epicentralChange = hypo.getHorizontalStepLength();
    depthChange = hypo.getVerticalStepLength();
    timeStandardError = hypo.getEstimatorRMSEquivalent();
    coLatitude = hypo.getCoLatitude();
    coLatitudeSine = hypo.getCoLatitudeSine();
    coLatitudeCosine = hypo.getCoLatitudeCosine();
    longitudeSine = hypo.getLongitudeSine();
    longitudeCosine = hypo.getLongitudeCosine();
  }

  /** This function converts the audit record to a string. */
  @Override
  public String toString() {
    return String.format(
        "Audit: %1d %2d %4d %22s %8.4f %9.4f %6.2f "
            + "del = %6.1f %6.1f %6.1f timeStandardError = %6.2f %s",
        stage,
        iteration,
        numPicksUsed,
        LocUtil.getDateTimeString(originTime),
        latitude,
        longitude,
        depth,
        epicentralChange,
        depthChange,
        hypocentralChange,
        timeStandardError,
        locationStatus);
  }

  /**
   * Function to return the audit origin time.
   *
   * @return A double containing the audit origin time in double precision seconds since the epoch
   */
  public double getOriginTime() {
    return originTime;
  }

  /**
   * Function to return the audit latitude.
   *
   * @return A double containing the audit geographic latitude in degrees
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * Function to return the audit longitude.
   *
   * @return A double containing the audit geographic longitude in degrees
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * Function to return the audit depth.
   *
   * @return A double containing the audit depth in kilometers
   */
  public double getDepth() {
    return depth;
  }

  /**
   * Function to return the audit colatitude.
   *
   * @return A double containing the audit colatitude in degrees
   */
  public double getCoLatitude() {
    return coLatitude;
  }  

  /**
   * Function to return the sine of the audit colatitude.
   *
   * @return A double containing the sine of the audit colatitude in degrees
   */
  public double getCoLatitudeSine() {
    return coLatitudeSine;
  }

  /**
   * Function to return the cosine of the audit colatitude.
   *
   * @return A double containing the cosine of the audit colatitude in degrees
   */
  public double getCoLatitudeCosine() {
    return coLatitudeCosine;
  }

  /**
   * Function to return the sine of the audit longitude.
   *
   * @return A double containing the sine of the audit longitude in degrees
   */
  public double getLongitudeSine() {
    return longitudeSine;
  }

  /**
   * Function to return the cosine of the audit longitude.
   *
   * @return A double containing the cosine of the audit longitude in degrees
   */
  public double getLongitudeCosine() {
    return longitudeCosine;
  }
}
