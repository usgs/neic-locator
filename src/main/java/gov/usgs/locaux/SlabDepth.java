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
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Double containing the slab depth triplet
   *
   * <p>For slab visualization (option 1), the three depths correspond to the slab surface depth,
   * the approximate earthquake depth, and the slab bottom depth. For earthquake location (option
   * 2), the three depths correspond to the shallow error bar, the earthquake depth, and the deeper
   * error bar.
   */
  private double[] eqDepth;

  /**
   * Function to return the earthquake depth
   *
   * @return Double containing the earthquake depth in km
   */
  public double getEqDepth() {
    return eqDepth[1];
  }

  /**
   * Function to return the shallow earthquake depth error bar
   *
   * @return Double containing the shallow earthquake depth error bar in km
   */
  public double getLower() {
    return eqDepth[0];
  }

  /**
   * Function to return the deep earthquake depth error bar
   *
   * @return Double containing the deep earthquake depth error bar in km
   */
  public double getUpper() {
    return eqDepth[2];
  }

  /**
   * SlabDepth constructor, create a new slab depth triplet from provided values.
   *
   * @param center Double containing the earthquake depth in km
   * @param lower Double containing the shallow earthquake depth error bar in km
   * @param upper Double containing the deep earthquake depth error bar in km
   */
  public SlabDepth(double center, double lower, double upper) {
    eqDepth = new double[3];
    eqDepth[0] = Math.abs(lower);
    eqDepth[1] = Math.abs(center);
    eqDepth[2] = Math.abs(upper);
  }

  /**
   * SlabDepth constructor, create a new slab depth triplet from provided error bars
   *
   * @param depths Slab earthquake depths and error bars in km
   */
  public SlabDepth(double[] depths) {
    eqDepth = Arrays.copyOf(depths, 3);
  }

  /**
   * Function to get three 3-vectors for interpolation
   *
   * @param lat A double containing Geographic colatitude in degrees (0-180 degrees)
   * @param lon A double containing Geographic longitude in degrees (0-360 degrees)
   * @return A two dimensional array of doubles containing spatial three 3-vectors
   */
  public double[][] getVectors(double lat, double lon) {
    double[][] v = new double[3][3];

    for (int j = 0; j < 3; j++) {
      v[j] = Linear.vector(lon, lat, eqDepth[j]);
    }

    return v;
  }

  /** Function to provide a toString suitable for traditional printing and logging. */
  @Override
  public String toString() {
    return String.format("%6.2f < %6.2f < %6.2f", eqDepth[0], eqDepth[1], eqDepth[2]);
  }

  /** Comparison function used to put the depth zones in order of increasing depth. */
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
