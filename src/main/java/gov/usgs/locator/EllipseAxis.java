package gov.usgs.locator;

/**
 * The EllipseAxis class describes of one axis of an error ellipse.
 *
 * @author Ray Buland
 */
public class EllipseAxis implements Comparable<EllipseAxis> {
  /** A double containing the length of the axis in kilometers. */
  private double semiLen;

  /** A double containing the azimuth of the axis in degrees (clockwise from north). */
  private double azimuth;

  /** A double containing the plunge of the axis in degrees (down from the horizontal). */
  private double plunge;

  /**
   * The EllipseAxis constructor. Sets the values of one axis of an error ellipse.
   *
   * @param semiLen A double containing the length of the axis in kilometers
   * @param azimuth A double containing the azimuth of the axis in degrees (clockwise from north)
   * @param plunge A double containing the plunge of the axis in degrees (down from the horizontal)
   */
  public EllipseAxis(double semiLen, double azimuth, double plunge) {
    this.semiLen = semiLen;
    this.azimuth = azimuth;
    this.plunge = plunge;
  }

  /**
   * Function to calculate the tangential (horizontal) projection of this axis.
   *
   * @return A double containing the tangential projection of this axis in kilometers
   */
  public double tangentialProj() {
    return semiLen * Math.cos(Math.toRadians(plunge));
  }

  /**
   * Function to calculate the vertical (depth) projection of this axis.
   *
   * @return A double containing the vertical projection of this axis in kilometers
   */
  public double verticalProj() {
    return semiLen * Math.sin(Math.toRadians(plunge));
  }

  /** Function to provide a toString suitable for traditional printing. */
  @Override
  public String toString() {
    return String.format("%6.1f %3.0f %3.0f", semiLen, azimuth, plunge);
  }

  /**
   * Comparison function used to sort the axes into descending order of the half length.
   *
   * @param axis A EllipseAxis to compare to
   * @return +1 if this EllipseAxis object semiLen varible is greater than the provided EllipseAxis
   *     object semiLen varible; -1 if this EllipseAxis object semiLen varible is less than the
   *     provided EllipseAxis object semiLen varible; and 0 if this EllipseAxis object semiLen
   *     varible is equal to the provided EllipseAxis object semiLen varible;
   */
  @Override
  public int compareTo(EllipseAxis axis) {
    if (this.semiLen > axis.semiLen) {
      return -1;
    } else if (this.semiLen < axis.semiLen) {
      return +1;
    } else {
      return 0;
    }
  }

  /**
   * Function to return the length of the axis in kilometers.
   *
   * @return A double containing the length of the axis in kilometers
   */
  public double getSemiLen() {
    return semiLen;
  }

  /**
   * Function to return the azimuth of the axis in degrees (clockwise from north).
   *
   * @return A double containing the azimuth of the axis in degrees (clockwise from north).
   */
  public double getAzimuth() {
    return azimuth;
  }

  /**
   * Function to return the plunge of the axis in degrees (down from the horizontal).
   *
   * @return A double containing the plunge of the axis in degrees (down from the horizontal).
   */
  public double getPlunge() {
    return plunge;
  }
}
