package gov.usgs.locaux;

import java.io.Serializable;

/**
 * Keep track of statistics for one latitude row.  Note that in this model 
 * grid points are in latitude rows with a fixed row spacing.  However, for 
 * each row, the longitude points will have a different fixed point spacing 
 * specific to that row.  This strategy samples the surface of the Earth with 
 * many fewer points than 1 x 1 cells (i.e., Marsden squares).
 * 
 * @author Ray Buland
 *
 */
public class NewZoneRow implements Serializable {
  /**
   * A long containing the serializable class version number, used during 
   * deserialization to verify compatibility.
   */
	private static final long serialVersionUID = 1L;
	int numLons;					// Number of longitude samples
	double lat;						// Geographical latitude in degrees
	double lonSpacing;		// Longitude spacing in degrees
	NewZonePoint[] samples = null;		// Array of longitude sample statistics
	
	/**
	 * @return The geographic latitude in degrees for this row
	 */
	public double getLat() {
		return lat;
	}
	
	/**
	 * @return The number of longitude samples for this row
	 */
	public int getNumLons() {
		return numLons;
	}
	
	/**
	 * @return The longitude sample spacing in degrees for this row
	 */
	public double getLonSpacing() {
		return lonSpacing;
	}
	
	/**
	 * Put one longitude sample point into this latitude row.
	 * 
	 * @param column The longitude column index
	 * @param stats ZoneStats statistics for this longitude
	 */
	public void putSample(int column, NewZonePoint stats) {
		// Allocate space if necessary.
		if(samples == null) {
			samples = new NewZonePoint[numLons];
		}
		samples[column] = stats;
	}
	
	/**
	 * Get one longitude sample point from this latitude row.
	 * 
	 * @param column The longitude column index
	 * @return ZoneStats statistics for this longitude
	 */
	public NewZonePoint getSample(int column) {
		if(samples != null) {
			return samples[column];
		} else {
			return null;
		}
	}
	
	/**
	 * @return The number of longitude samples in this row.
	 */
	public int size() {
		return numLons;
	}
	
	/**
	 * The constructor initializes this latitude row
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
	
	@Override
	public String toString() {
		return String.format("NewZoneRow: lat = %8.4f lonSpacing = %8.4f", lat, lonSpacing);
	}
}
