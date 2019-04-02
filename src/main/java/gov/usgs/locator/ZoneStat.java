package gov.usgs.locator;

import java.io.Serializable;

/**
 * The ZoneStat class mantains depth statistics for one Marsden square.
 *
 * @author Ray Buland
 */
public class ZoneStat implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** A double holding the average historical free depth in kilometers. */
  private double meanFreeDepth;

  /** A double holding the shallowest historical free depth in kilometers. */
  private double miniumFreeDepth;

  /** A double holding the deepest historical free depth in kilometers. */
  private double maximumFreeDepth;

  /**
   * Function to return the average historical free depth.
   *
   * @return A double containing the average historical free depth in kilometers
   */
  public double getMeanFreeDepth() {
    return meanFreeDepth;
  }

  /**
   * Function to return the shallowest historical free depth.
   *
   * @return A double containing the shallowest historical free depth in kilometers
   */
  public double getMinimumFreeDepth() {
    return miniumFreeDepth;
  }

  /**
   * Function to return the deepest historical free depth.
   *
   * @return A double containing the deepest historical free depth in kilometers
   */
  public double getMaximumFreeDepth() {
    return maximumFreeDepth;
  }

  /**
   * The ZoneStat constructor. Sets the zone statistics for this Marsden square.
   *
   * @param meanFreeDepth A double holding the average historical free depth in kilometers
   * @param miniumFreeDepth A double holding the shallowest historical free depth in kilometers
   * @param maximumFreeDepth A double containing the deepest historical free depth in kilometers
   */
  public ZoneStat(double meanFreeDepth, double miniumFreeDepth, double maximumFreeDepth) {
    this.meanFreeDepth = meanFreeDepth;
    this.miniumFreeDepth = miniumFreeDepth;
    this.maximumFreeDepth = maximumFreeDepth;
  }
}
