package gov.usgs.locaux;

import java.util.ArrayList;

/**
 * Keep track of tilted slab points near this grid point.
 *
 * @author Ray Buland
 */
public class TiltedSample {
  /** A double containing the grid point geographic colatitude in degrees (0-180 degrees) */
  private double lat;

  /** A double containing the grid point geographic longitude in degrees (0-360 degrees) */
  private double lon;

  /** An ArrayList of SlabPoint objects containing the samples for this grid point */
  private ArrayList<SlabPoint> samples;

  /**
   * Function to retrieve the colatitude for this grid point
   *
   * @return A double containing the grid point geographic colatitude in degrees (0-180)
   */
  public double getLat() {
    return lat;
  }

  /**
   * Function to retrieve the longitude for this grid point
   *
   * @return A double containing the grid point geographic longitude in degrees (0-180)
   */
  public double getLon() {
    return lon;
  }

  /**
   * TiltedSample Constructor. Sets up the grid point.
   *
   * @param lat A double containing the grid point geographic colatitude in degrees (0-180 degrees)
   * @param lon A double containing the grid point geographic longitude in degrees (0-360 degrees)
   */
  public TiltedSample(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;

    samples = new ArrayList<SlabPoint>();
  }

  /**
   * Function to add a tilted slab sample to this grid point.
   *
   * @param point A SlabPoint object containing the tilted slab sample
   */
  public void add(SlabPoint point) {
    samples.add(point);
  }

  /**
   * Function to summarize the data collected for this grid point.
   *
   * @return A SlabDepth object containing the slab depth information, If no samples fell into this
   *     grid, return an empty SlabDepth object
   */
  public SlabDepth merge() {
    double lower = 1000d, center = 0d, upper = 0d;

    if (samples.size() > 0) {
      /*
       * The Locator requirements are simple minded, so just return the mean
       * of the slab depths in this grid point and the extremes of the error
       * bars.
       */
      for (SlabPoint point : samples) {
        lower = Math.min(point.getDep().getLower(), lower);
        center += point.getDep().getEqDepth();
        upper = Math.max(point.getDep().getUpper(), upper);
      }

      center /= samples.size();
      return new SlabDepth(center, lower, upper);
    } else {
      return new SlabDepth(Double.NaN, Double.NaN, Double.NaN);
    }
  }

  /** Function to provide a toString suitable for traditional printing and logging. */
  @Override
  public String toString() {
    return "" + samples.size() + " samples.";
  }
}
