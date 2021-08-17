package gov.usgs.locaux;

import java.io.Serializable;

/**
 * Keep track of statistics for one latitude row. Note that in this model grid points are in
 * latitude rows with a fixed row spacing. However, for each row, the longitude points will have a
 * different fixed point spacing specific to that row. This strategy samples the surface of the
 * Earth with many fewer points than 1 x 1 cells (i.e., Marsden squares).
 *
 * @author Ray Buland
 */
public class NewZoneRow implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** An integer containing the number of longitude samples */
  private int numLons;

  /** A double containing the geographical latitude in degrees */
  private double lat;

  /** A double containing the longitude spacing in degrees */
  private double lonSpacing;

  /** An array containing the longitude sample statistics */
  private NewZonePoint[] samples = null;

  /**
   * Function to get the geographic longitude for this zone row
   *
   * @return A double containing the geographic latitude in degrees for this row
   */
  public double getLat() {
    return lat;
  }

  /**
   * Function to return the number of longitude samples for this zone row
   *
   * @return An integer containing the number of longitude samples for this row
   */
  public int getNumLons() {
    return numLons;
  }

  /**
   * Function to get the longitude sample spacing for this zone row
   *
   * @return A double containing the longitude sample spacing in degrees for this row
   */
  public double getLonSpacing() {
    return lonSpacing;
  }

  /**
   * The NewZoneRow constructor, sets this zone row's statistics to the provided values.
   *
   * @param lat The geographical latitude in degrees
   * @param lonSpacing The spacing of longitude points in degrees
   * @param numLons The total number of longitude samples
   */
  public NewZoneRow(double lat, double lonSpacing, int numLons) {
    this.lat = lat;
    this.lonSpacing = lonSpacing;
    this.numLons = numLons;
  }

  /**
   * Function to add one longitude sample point into this latitude row at the provided index.
   *
   * @param column The longitude column index
   * @param stats ZoneStats statistics for this longitude
   */
  public void putSample(int column, NewZonePoint stats) {
    // Allocate space if necessary.
    if (samples == null) {
      samples = new NewZonePoint[numLons];
    }
    samples[column] = stats;
  }

  /**
   * Function to retrieve one longitude sample point from this latitude row at the provided index.
   *
   * @param column The longitude column index
   * @return ZoneStats statistics for this longitude
   */
  public NewZonePoint getSample(int column) {
    if (samples != null) {
      return samples[column];
    } else {
      return null;
    }
  }

  /** Function to provide a toString suitable for traditional printing and logging. */
  @Override
  public String toString() {
    return String.format("NewZoneRow: lat = %8.4f lonSpacing = %8.4f", lat, lonSpacing);
  }
}
