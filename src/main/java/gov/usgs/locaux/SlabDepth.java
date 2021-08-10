package gov.usgs.locaux;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Store slab depth information. Note that this is just a convenience for processing the slab ASCII
 * data, but is used for the final storage as having a latitude and longitude for every point is
 * unnecessary.
 *
 * @author Ray Buland
 */
public class SlabDepth implements Serializable, Comparable<SlabDepth> {
  private static final long serialVersionUID = 1L;
  /**
   * For slab visualization (option 1), the three depths correspond to the slab surface depth, the
   * approximate earthquake depth, and the slab bottom depth. For earthquake location (option 2),
   * the three depths correspond to the shallow error bar, the earthquake depth, and the deeper
   * error bar.
   */
  double[] eqDepth;

  /** @return Earthquake depth in km */
  public double getEqDepth() {
    return eqDepth[1];
  }

  /** @return Shallow earthquake depth error bar in km */
  public double getLower() {
    return eqDepth[0];
  }

  /** @return Deep earthquake depth error bar in km */
  public double getUpper() {
    return eqDepth[2];
  }

  /**
   * Create a new slab depth triplet.
   *
   * @param center Earthquake depth in km
   * @param lower Shallow earthquake depth error bar in km
   * @param upper Deep earthquake depth error bar in km
   */
  public SlabDepth(double center, double lower, double upper) {
    eqDepth = new double[3];
    eqDepth[0] = Math.abs(lower);
    eqDepth[1] = Math.abs(center);
    eqDepth[2] = Math.abs(upper);
  }

  /**
   * Create a new slab depth triplet.
   *
   * @param depths Slab earthquake depths and error bars in km
   */
  public SlabDepth(double[] depths) {
    eqDepth = Arrays.copyOf(depths, 3);
  }

  /**
   * Get three 3-vectors for interpolation
   *
   * @param lat Geographic colatitude in degrees (0-180 degrees)
   * @param lon Geographic longitude in degrees (0-360 degrees)
   * @return An array of spatial three 3-vectors
   */
  public double[][] getVectors(double lat, double lon) {
    double[][] v = new double[3][3];
    for (int j = 0; j < 3; j++) {
      v[j] = Linear.vector(lon, lat, eqDepth[j]);
    }
    return v;
  }

  @Override
  public String toString() {
    return String.format("%6.2f < %6.2f < %6.2f", eqDepth[0], eqDepth[1], eqDepth[2]);
  }

  /** The comparison is used to put the depth zones in order of increasing depth. */
  @Override
  public int compareTo(SlabDepth otherDepth) {
    if (eqDepth[1] > otherDepth.getEqDepth()) {
      return +1;
    } else if (eqDepth[1] < otherDepth.getEqDepth()) {
      return -1;
    } else {
      return 0;
    }
  }
}
