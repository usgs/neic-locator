package gov.usgs.locaux;

import java.io.Serializable;

/**
 * Keep track of statistics for one new ZoneStats grid point.
 *
 * @author Ray Buland
 */
public class NewZonePoint implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** Geographic longitude of this zone point in degrees. */
  private double lon;

  /** Number of data behind the statistic for this zone point. */
  private int dataCount;

  /** Mean depth this zone point in in kilometers. */
  private double depth;

  /** Depth standard error this zone point in in kilometers. */
  private double depthError;

  /** @return The geographic longitude in degrees */
  public double getLon() {
    return lon;
  }

  /**
   * Function to return the of data behind the statistic
   *
   * @return An integer containing number of data behind the statistic
   */
  public int getDataCount() {
    return dataCount;
  }

  /**
   * Function to return the mean depth
   *
   * @return A double containing the mean depth in kilometers
   */
  public double getDepth() {
    return depth;
  }

  /**
   * Function to return the depth standard error
   *
   * @return A double containing the depth standard error in kilometers
   */
  public double getDepthError() {
    return depthError;
  }

  /**
   * The NewZonePoint constructor, sets this zone point's statistics to the provided values.
   *
   * @param lon The geographical longitude in degrees
   * @param count The number of data behind the statistic
   * @param depth The mean depth in kilometers
   * @param depthError The depth standard error in kilometers
   */
  public NewZonePoint(double lon, int count, double depth, double depthError) {
    this.lon = lon;
    this.dataCount = count;
    this.depth = depth;
    this.depthError = depthError;
  }

  /** Function to provide a toString suitable for traditional printing and logging. */
  @Override
  public String toString() {
    return String.format(
        "NewZonePoint: lon = %9.4f depth = %6.2f spread = %6.2f", lon, depth, depthError);
  }
}
