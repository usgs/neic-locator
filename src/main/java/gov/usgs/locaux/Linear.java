package gov.usgs.locaux;

/**
 * A collection of linear functions including distance, intersection, and interpolation methods in
 * one, two, and three dimensions (e.g., fitting two, three, and four points).
 *
 * @author Ray Buland
 */
public class Linear {

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

  /**
   * Return the Cartesian distance between two points. Note that this assumes a plane (i.e., it only
   * uses the first two coordinates of each input vector.
   *
   * @param v0 First 2-vector
   * @param v1 Second 2-vector
   * @return Distance between the first and second 2-vectors
   */
  public static double distance(double[] v0, double[] v1) {
    return Math.sqrt(Math.pow(v1[0] - v0[0], 2d) + Math.pow(v1[1] - v0[1], 2d));
  }

  /**
   * Interpolate the z-values of two 3-vectors using either the x or y values (they should be
   * interchangeable). Note that the x or y values in the desired interpolation point are used in
   * the interpolation. The interpolated value is returned as the z value of the interpolation
   * vector as well.
   *
   * @param v0 First point vector
   * @param v1 Second point vector
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static double oneD(double[] v0, double[] v1, double[] v) {
    double b = Double.NaN;

    if (Math.abs(v1[0] - v0[0]) > Math.abs(v1[1] - v0[1])) {
      b = (v1[2] - v0[2]) / (v1[0] - v0[0]);
      v[2] = b * (v[0] - v0[0]) + v0[2];
    } else {
      b = (v1[2] - v0[2]) / (v1[1] - v0[1]);
      v[2] = b * (v[1] - v0[1]) + v0[2];
    }

    return v[2];
  }

  /**
   * Interpolate the z-values of two 3-vectors using either the x or y values (they should be
   * interchangeable). Note that the x or y values in the desired interpolation point are used in
   * the interpolation. The interpolated value is returned as the z value of the interpolation
   * vector as well.
   *
   * @param v0 First point vector
   * @param v1 Second point vector
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static float oneD(float[] v0, float[] v1, float[] v) {
    float b = Float.NaN;

    if (Math.abs(v1[0] - v0[0]) > Math.abs(v1[1] - v0[1])) {
      b = (v1[2] - v0[2]) / (v1[0] - v0[0]);
      v[2] = b * (v[0] - v0[0]) + v0[2];
    } else {
      b = (v1[2] - v0[2]) / (v1[1] - v0[1]);
      v[2] = b * (v[1] - v0[1]) + v0[2];
    }

    return v[2];
  }

  /**
   * Another approach to the 1-D interpolation problem. The end point vector has been replaced with
   * increments in x and z. As above, the x value from the interpolated point is needed and the z
   * value computed is returned in the same vector as well.
   *
   * @param v0 First point vector
   * @param dx X increment to the end point
   * @param dz Z increment to the end point
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static double oneD(double[] v0, double dx, double dz, double[] v) {
    v[2] = (dz / dx) * (v[0] - v0[0]) + v0[2];
    return v[2];
  }

  /**
   * Another approach to the 1-D interpolation problem. The end point vector has been replaced with
   * increments in x and z. As above, the x value from the interpolated point is needed and the z
   * value computed is returned in the same vector as well.
   *
   * @param v0 First point vector
   * @param dx X increment to the end point
   * @param dz Z increment to the end point
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static float oneD(float[] v0, float dx, float dz, float[] v) {
    v[2] = (dz / dx) * (v[0] - v0[0]) + v0[2];
    return v[2];
  }

  /**
   * A third approach to the 1-D problem not using vectors.
   *
   * @param x0 First point x value
   * @param z0 First point z value
   * @param dx X increment to the end point
   * @param dz Z increment to the end point
   * @param x X value to be interpolated
   * @return Interpolated z value
   */
  public static double oneD(double x0, double z0, double dx, double dz, double x) {
    return (dz / dx) * (x - x0) + z0;
  }

  /**
   * A third approach to the 1-D problem not using vectors.
   *
   * @param x0 First point x value
   * @param z0 First point z value
   * @param dx X increment to the end point
   * @param dz Z increment to the end point
   * @param x X value to be interpolated
   * @return Interpolated z value
   */
  public static float oneD(float x0, float z0, float dx, float dz, float x) {
    return (dz / dx) * (x - x0) + z0;
  }

  /**
   * Given three points, we can fit a plane and interpolate from that. As above, the x and y values
   * from the interpolated point are used to compute the interpolated z value, which is returned in
   * the same vector as well. The points are assumed to be on a square grid with the first point at
   * the right angle, the second point in the x direction, and the third point in the y direction.
   *
   * @param v0 First point vector
   * @param v1 Second point vector
   * @param v2 Third point vector
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static double twoD(double[] v0, double[] v1, double[] v2, double[] v) {
    double a, b, c;

    if (Math.abs(v2[1] - v0[1]) > Math.abs(v1[1] - v0[1])) {
      a =
          ((v2[1] - v0[1]) * (v1[2] - v0[2]) - (v1[1] - v0[1]) * (v2[2] - v0[2]))
              / ((v1[0] - v0[0]) * (v2[1] - v0[1]) - (v2[0] - v0[0]) * (v1[1] - v0[1]));
      b = ((v2[2] - v0[2]) - a * (v2[0] - v0[0])) / (v2[1] - v0[1]);
    } else {
      a =
          ((v1[1] - v0[1]) * (v2[2] - v0[2]) - (v2[1] - v0[1]) * (v1[2] - v0[2]))
              / ((v2[0] - v0[0]) * (v1[1] - v0[1]) - (v1[0] - v0[0]) * (v2[1] - v0[1]));
      b = ((v1[2] - v0[2]) - a * (v1[0] - v0[0])) / (v1[1] - v0[1]);
    }

    c = v0[2] - a * v0[0] - b * v0[1];
    v[2] = a * v[0] + b * v[1] + c;

    return v[2];
  }

  /**
   * Given three points, we can fit a plane and interpolate from that. As above, the x and y values
   * from the interpolated point are used to compute the interpolated z value, which is returned in
   * the same vector as well. The points are assumed to be on a square grid with the first point at
   * the right angle, the second point in the x direction, and the third point in the y direction.
   *
   * @param v0 First point vector
   * @param v1 Second point vector
   * @param v2 Third point vector
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static float twoD(float[] v0, float[] v1, float[] v2, float[] v) {
    float a, b, c;

    if (Math.abs(v2[1] - v0[1]) > Math.abs(v1[1] - v0[1])) {
      a =
          ((v2[1] - v0[1]) * (v1[2] - v0[2]) - (v1[1] - v0[1]) * (v2[2] - v0[2]))
              / ((v1[0] - v0[0]) * (v2[1] - v0[1]) - (v2[0] - v0[0]) * (v1[1] - v0[1]));
      b = ((v2[2] - v0[2]) - a * (v2[0] - v0[0])) / (v2[1] - v0[1]);
    } else {
      a =
          ((v1[1] - v0[1]) * (v2[2] - v0[2]) - (v2[1] - v0[1]) * (v1[2] - v0[2]))
              / ((v2[0] - v0[0]) * (v1[1] - v0[1]) - (v1[0] - v0[0]) * (v2[1] - v0[1]));
      b = ((v1[2] - v0[2]) - a * (v1[0] - v0[0])) / (v1[1] - v0[1]);
    }

    c = v0[2] - a * v0[0] - b * v0[1];
    v[2] = a * v[0] + b * v[1] + c;

    return v[2];
  }

  /**
   * An alternate approach to the 2-D problem. The x and y values from the interpolated point are
   * used to compute the interpolated z value, which is returned in the same vector as well.
   *
   * @param v0 First point vector
   * @param dx X increment to the second point
   * @param dy Y increment to the third point
   * @param dzx Z increment to the second point
   * @param dzy Z increment to the third point
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static double twoD(double[] v0, double dx, double dy, double dzx, double dzy, double[] v) {
    v[2] = (dzx / dx) * (v[0] - v0[0]) + (dzy / dy) * (v[1] - v0[1]) + v0[2];

    return v[2];
  }

  /**
   * An alternate approach to the 2-D problem. The x and y values from the interpolated point are
   * used to compute the interpolated z value, which is returned in the same vector as well.
   *
   * @param v0 First point vector
   * @param dx X increment to the second point
   * @param dy Y increment to the third point
   * @param dzx Z increment to the second point
   * @param dzy Z increment to the third point
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static float twoD(float[] v0, float dx, float dy, float dzx, float dzy, float[] v) {
    v[2] = (dzx / dx) * (v[0] - v0[0]) + (dzy / dy) * (v[1] - v0[1]) + v0[2];

    return v[2];
  }

  /**
   * Given four points we can fit the product of two linear functions. The result is, of course, not
   * linear, hence the claim to 3-D. The x and y values from the interpolated point are used to
   * compute the interpolated z value, which is returned in the same vector as well. The four points
   * are assumed to be on a square grid in the order: lower left hand corner, lower right hand
   * corner (x increment), upper left hand corner (y increment), and upper right hand corner (x-y
   * increment).
   *
   * @param v0 First point vector
   * @param v1 Second point vector
   * @param v2 Third point vector
   * @param v3 Fourth point vector
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static double threeD(double[] v0, double[] v1, double[] v2, double[] v3, double[] v) {
    double b, c, d;

    b = (v1[2] - v0[2]) / (v1[0] - v0[0]);
    c = (v2[2] - v0[2]) / (v2[1] - v0[1]);
    d = (v3[2] - v2[2] - v1[2] + v0[2]) / ((v1[0] - v0[0]) * (v2[1] - v0[1]));
    v[2] = b * (v[0] - v0[0]) + c * (v[1] - v0[1]) + d * (v[0] - v0[0]) * (v[1] - v0[1]) + v0[2];

    return v[2];
  }

  /**
   * Given four points we can fit the product of two linear functions. The result is, of course, not
   * linear, hence the claim to 3-D. The x and y values from the interpolated point are used to
   * compute the interpolated z value, which is returned in the same vector as well. The four points
   * are assumed to be on a square grid in the order: lower left hand corner, lower right hand
   * corner (x increment), upper left hand corner (y increment), and upper right hand corner (x-y
   * increment).
   *
   * @param v0 First point vector
   * @param v1 Second point vector
   * @param v2 Third point vector
   * @param v3 Fourth point vector
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static float threeD(float[] v0, float[] v1, float[] v2, float[] v3, float[] v) {
    float b, c, d;

    b = (v1[2] - v0[2]) / (v1[0] - v0[0]);
    c = (v2[2] - v0[2]) / (v2[1] - v0[1]);
    d = (v3[2] - v2[2] - v1[2] + v0[2]) / ((v1[0] - v0[0]) * (v2[1] - v0[1]));
    v[2] = b * (v[0] - v0[0]) + c * (v[1] - v0[1]) + d * (v[0] - v0[0]) * (v[1] - v0[1]) + v0[2];

    return v[2];
  }

  /**
   * An alternate approach to the 3-D problem. As above, the x and y values from the interpolated
   * point are used to compute the interpolated z value, which is returned in the same vector as
   * well.
   *
   * @param v0 First vector (lower left hand corner)
   * @param dx X increment
   * @param dy Y increment
   * @param dzx Z value of the second point
   * @param dzy Z value of the third point
   * @param dzxy Z value of the fourth point
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static double threeD(
      double[] v0, double dx, double dy, double dzx, double dzy, double dzxy, double[] v) {
    v[2] =
        (dzx / dx) * (v[0] - v0[0])
            + (dzy / dy) * (v[1] - v0[1])
            + ((dzxy - dzx - dzy) / (dx * dy)) * (v[0] - v0[0]) * (v[1] - v0[1])
            + v0[2];

    return v[2];
  }

  /**
   * An alternate approach to the 3-D problem. As above, the x and y values from the interpolated
   * point are used to compute the interpolated z value, which is returned in the same vector as
   * well.
   *
   * @param v0 First vector (lower left hand corner)
   * @param dx X increment
   * @param dy Y increment
   * @param dzx Z value of the second point
   * @param dzy Z value of the third point
   * @param dzxy Z value of the fourth point
   * @param v Interpolated point vector
   * @return Interpolated z-value
   */
  public static float threeD(
      float[] v0, float dx, float dy, float dzx, float dzy, float dzxy, float[] v) {
    v[2] =
        (dzx / dx) * (v[0] - v0[0])
            + (dzy / dy) * (v[1] - v0[1])
            + ((dzxy - dzx - dzy) / (dx * dy)) * (v[0] - v0[0]) * (v[1] - v0[1])
            + v0[2];

    return v[2];
  }

  /**
   * Find where a line defined by 2-vectors v0 and v1 intersects a perpendicular line through point
   * v.
   *
   * @param v0 2-vector on the first line
   * @param v1 2-vector on the first line
   * @param v 2-vector defining the perpendicular line
   * @return 2-vector of the intersection point
   */
  public static double[] intersect(double[] v0, double[] v1, double[] v) {
    double a0, b0, a1, b1;

    // Define the line through v0 and v1.
    a0 = (v1[1] - v0[1]) / (v1[0] - v0[0]);
    if (Double.isFinite(a0)) {
      b0 = v0[1] - a0 * v0[0];
    } else {
      // Ugg!  The line is vertical (i.e., y = v0[0]).
      b0 = v0[0];
    }

    // Define the perpendicular line through v.
    a1 = -1d / a0;
    if (Double.isFinite(a1)) {
      b1 = v[1] - a1 * v[0];
    } else {
      // Double ugg!  The first line was horizontal.
      b1 = v[0];
    }

    return intersect(a0, b0, a1, b1);
  }

  /**
   * Given the equations for two lines (i.e., y = a0*x + b0 and y = a1*x + b1) find the point where
   * the lines cross. There are two special cases: 1) a0 = 0 and 2) a1 = 0. In the first case, the
   * first line is horizontal and the second is vertical. In this case, b0 should be the fixed y
   * value of the first line and b1 should be the fixed x value of the second line. In the second
   * case, the first line is vertical and the second is horizontal. In this case, b0 should be the
   * fixed x value of the first line and b1 should be the fixed y value of the second line.
   *
   * @param a0 Slope of the first line
   * @param b0 Offset of the first line
   * @param a1 Slope of the second line
   * @param b1 Offset of the second line
   * @return 2-vector of the intersection point
   */
  public static double[] intersect(double a0, double b0, double a1, double b1) {
    double[] v = new double[3];

    if (a0 == 0d) {
      // The first line is horizontal.
      v[0] = b1;
      v[1] = b0;
    } else if (a1 == 0d) {
      // The first line is vertical.
      v[0] = b0;
      v[1] = b1;
    } else {
      v[0] = (b1 - b0) / (a0 - a1);
      v[1] = a0 * v[0] + b0;
    }

    v[2] = Double.NaN;

    return v;
  }
}
