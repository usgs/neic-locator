package gov.usgs.locaux;

import java.util.ArrayList;

/**
 * Keep track of tilted slab points near this grid point.
 *
 * @author Ray Buland
 */
public class TiltedSample {
  double lat; // Grid point geographic colatitude in degrees (0-180 degrees)
  double lon; // Grid point geographic longitude in degrees (0-360 degrees)
  ArrayList<SlabPoint> samples;

  /** @return Grid point geographic colatitude in degrees (0-180) */
  public double getLat() {
    return lat;
  }

  /** @return Grid point geographic longitude in degrees (0-360) */
  public double getLon() {
    return lon;
  }

  /**
   * Set up the grid point.
   *
   * @param lat Grid point geographic colatitude in degrees (0-180 degrees)
   * @param lon Grid point geographic longitude in degrees (0-360 degrees)
   */
  public TiltedSample(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
    samples = new ArrayList<SlabPoint>();
  }

  /**
   * Add a tilted slab sample to this grid point.
   *
   * @param point Tilted slab sample
   */
  public void add(SlabPoint point) {
    samples.add(point);
  }

  /**
   * Summarize the data collected for this grid point. If no samples fell into this grid, return
   * null.
   *
   * @return Slab depth information
   */
  public SlabDepth merge() {
    double lower = 1000d, center = 0d, upper = 0d;

    if (samples.size() > 0) {
      /*
       * The Locator requirements are simple minded, so just return the mean
       * of the slab depths in this grid point and the extremes of the error
       * bars.
       */
      //			System.out.format("\tMerge (%6.2f, %6.2f):\n", lat, lon);
      for (SlabPoint point : samples) {
        //				System.out.println("\t\t" + point);
        lower = Math.min(point.getDep().getLower(), lower);
        center += point.getDep().getEqDepth();
        upper = Math.max(point.getDep().getUpper(), upper);
      }
      center /= samples.size();
      //			SlabDepth depth = new SlabDepth(center, lower, upper);
      //			System.out.println("\t\t\tSummary: n = " + samples.size() + " " + depth);
      //			return depth;
      return new SlabDepth(center, lower, upper);
    } else {
      return new SlabDepth(Double.NaN, Double.NaN, Double.NaN);
    }
  }

  @Override
  public String toString() {
    return "" + samples.size() + " samples.";
  }
}
