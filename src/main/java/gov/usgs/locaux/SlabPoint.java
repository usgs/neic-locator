package gov.usgs.locaux;

import java.io.Serializable;

/**
 * Store slab depth information for one latitude and longitude.
 *
 * @author Ray Buland
 */
public class SlabPoint implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** A double containing the Geographic colatitude in degrees (0-180 degrees) */
  private double lat;

  /** A double containing the Geographic longitude in degrees (0-360 degrees) */
  private double lon;

  /** A SlabDepth object containing the Depth center and range */
  private SlabDepth dep;

  /**
   * Function to get the Geographic colatitude
   *
   * @return A double containing the Geographic colatitude in degrees (0-180 degrees)
   */
  public double getLat() {
    return lat;
  }

  /**
   * Function to get the Geographic longitude
   *
   * @return A double containing the Geographic longitude in degrees (0-360 degrees)
   */
  public double getLon() {
    return lon;
  }

  /**
   * Function to get the earthquake depth
   *
   * @return A double containing the Earthquake depth in km
   */
  public double getEqDepth() {
    return dep.getEqDepth();
  }

  /**
   * Function to retrive the slab depth information.
   *
   * @return A SlabDepth object containing the Slab depth information
   */
  public SlabDepth getDep() {
    return dep;
  }

  /**
   * SlabPoint Constructor, Create a new slab depth point from provided values
   *
   * @param lat A double containing the Geographic colatitude in degrees (0-180 degrees)
   * @param lon A double containing the Geographic longitude in degrees (0-360 degrees)
   * @param center A double containing the Earthquake depth in km
   * @param lower A double containing the Lower (shallower) earthquake depth standard deviation in
   *     km
   * @param upper A double containing the Upper (deeper) earthquake depth standard deviation in km
   */
  public SlabPoint(double lat, double lon, double center, double lower, double upper) {
    this.lat = lat;
    this.lon = lon;
    lower = Math.min(lower, 0d);
    dep = new SlabDepth(center, lower, upper);
  }

  /**
   * SlabPoint Constructor, Create a new slab depth point from a tilted slabs grid point.
   *
   * @param gridPoint A TiltedSample object containing tilted slab data associated with one grid
   *     point
   */
  public SlabPoint(TiltedSample gridPoint) {
    lat = gridPoint.getLat();
    lon = gridPoint.getLon();
    dep = gridPoint.merge();
  }

  /** Function to provide a toString suitable for traditional printing and logging. */
  @Override
  public String toString() {
    return String.format("(%6.2f, %6.2f): %s", lat, lon, dep);
  }
}
