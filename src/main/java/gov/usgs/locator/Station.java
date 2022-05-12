package gov.usgs.locator;

import gov.usgs.traveltime.TauUtilities;

/**
 * Keep all data for one seismic station here.
 *
 * @author Ray
 */
public class Station {
  /** A StationID object containing the full station designation. */
  private StationID stationID;

  /** A double containing the geographic station latitude in degrees. */
  private double latitude;

  /** A double containing the station longitude in degrees. */
  private double longitude;

  /** A double containing the elevation in kilometers. */
  private double elevation;

  /** A double containing the geocentric colatitude in degrees. */
  private double coLatitude;

  /** A double containing the sine of the geocentric colatitude in degrees. */
  private double coLatitudeSine;

  /** A double containing the cosine of the geocentric colatitude in degrees. */
  private double coLatitudeCosine;

  /** A double containing the sine of the longitude in degrees. */
  private double longitudeSine;

  /** A double containing the cosine of the longitude in degrees. */
  private double longitudeCosine;

  /**
   * Function to return the full station designation.
   *
   * @return A StationID object containing the full station designation
   */
  public StationID getStationID() {
    return stationID;
  }

  /**
   * Function to get the geographic station latitude.
   *
   * @return A double containing the geographic station latitude in degrees.
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * Function to get the station longitude.
   *
   * @return A double containing the station longitude in degrees.
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * Function to get the station elevation.
   *
   * @return A double containing the station elevation in kilometers.
   */
  public double getElevation() {
    return elevation;
  }

  /**
   * Function to return the colatitude.
   *
   * @return A double containing the colatitude in degrees
   */
  public double getCoLatitude() {
    return coLatitude;
  }

  /**
   * Function to return the sine of the colatitude.
   *
   * @return A double containing the sine of the colatitude in degrees
   */
  public double getCoLatitudeSine() {
    return coLatitudeSine;
  }

  /**
   * Function to return the cosine of the colatitude.
   *
   * @return A double containing the cosine of the colatitude in degrees
   */
  public double getCoLatitudeCosine() {
    return coLatitudeCosine;
  }

  /**
   * Function to return the sine of the longitude.
   *
   * @return A double containing the sine of the longitude in degrees
   */
  public double getLongitudeSine() {
    return longitudeSine;
  }

  /**
   * Function to return the cosine of the longitude.
   *
   * @return A double containing the cosine of the longitude in degrees
   */
  public double getLongitudeCosine() {
    return longitudeCosine;
  }

  /**
   * The Station constructor. Initializes the station, latitude, longitude, and elevation to
   * provided values, and computes the sines and cosines.
   *
   * @param stationID A StationID object containing the station designation.
   * @param latitude A double containing the geographic station latitude in degrees.
   * @param longitude A double containing the station longitude in degrees.
   * @param elevation A double containing the elevation in kilometers
   */
  public Station(StationID stationID, double latitude, double longitude, double elevation) {
    // Remember the inputs.
    this.stationID = stationID;
    this.latitude = latitude;
    this.longitude = longitude;
    this.elevation = elevation;

    // Set up the sines and cosines.
    coLatitude = TauUtilities.computeGeocentricColatitude(latitude);
    coLatitudeSine = Math.sin(Math.toRadians(coLatitude));
    coLatitudeCosine = Math.cos(Math.toRadians(coLatitude));
    longitudeSine = Math.sin(Math.toRadians(longitude));
    longitudeCosine = Math.cos(Math.toRadians(longitude));
  }

  /** This fuction converts the station contents into a string. */
  @Override
  public String toString() {
    return String.format(
        "%-5s %-2s %-2s %8.4f %9.4f %6.2f",
        stationID.getStationCode(),
        stationID.getLocationCode(),
        stationID.getNetworkCode(),
        latitude,
        longitude,
        elevation);
  }
}
