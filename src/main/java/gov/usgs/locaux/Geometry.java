package gov.usgs.locaux;

/**
 * Miscellaneous geometric support functions.
 *
 * @author Ray Buland
 */
public class Geometry {

  /**
   * Create a 3-vector a = (x, y, z).
   *
   * @param x Distance in the X direction
   * @param y Distance in the Y direction
   * @param z Distance in the Z direction
   * @return Position vector
   */
  public static double[] vector(double x, double y, double z) {
    double[] a = new double[3];
    a[0] = x;
    a[1] = y;
    a[2] = z;
    return a;
  }

  /**
   * Create a 3-vector a = (x, y, z).
   *
   * @param x Distance in the X direction
   * @param y Distance in the Y direction
   * @param z Distance in the Z direction
   * @return Position vector
   */
  public static float[] vector(float x, float y, float z) {
    float[] a = new float[3];
    a[0] = x;
    a[1] = y;
    a[2] = z;
    return a;
  }
}
