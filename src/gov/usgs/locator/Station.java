package gov.usgs.locator;

import gov.usgs.traveltime.TauUtil;

/**
 * Keep all data for one seismic station here.
 * 
 * @author Ray
 *
 */
public class Station {
	// Inputs:
	StationID staID;			// Full station designation
	double latitude;			// Geographic station latitude in degrees
	double longitude;			// Station longitude in degrees
	double elevation;			// Station elevation in kilometers
	// Internal use:
	double coLat;					// Geocentric co-latitude in degrees
	double sinLat;				// Sine of the geocentric co-latitude
	double cosLat;				// Cosine of the geocentric co-latitude
	double sinLon;				// Sine of the longitude
	double cosLon;				// Cosine of the longitude

	/**
	 * Initialize the station and compute the sines and cosines.  This 
	 * version will eventually get the station information from the 
	 * station database.
	 * 
	 * @param staID Station designation
	 */
	public Station(StationID staID) {
		// Remember the station ID.
		this.staID = staID;
		// Get latitude, longitude and elevation from the master.  Dummy 
		// up for now.
		latitude = Double.NaN;
		longitude = Double.NaN;
		elevation = Double.NaN;
		// Set up the sines and cosines.
		coLat = TauUtil.geoCen(latitude);
		updateSines();
	}
	
	/**
	 * Initialize the station and compute the sines and cosines.
	 * 
	 * @param staID Station designation
	 * @param latitude Geographical latitude in degrees
	 * @param longitude Longitude in degrees
	 * @param elevation Elevation in kilometers
	 */
	public Station(StationID staID, double latitude, double longitude, 
			double elevation) {
		// Remember the inputs.
		this.staID = staID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
		// Set up the sines and cosines.
		coLat = TauUtil.geoCen(latitude);
		updateSines();
	}
	
	/**
	 * Compute the sines and cosines of colatitude and longitude.
	 */
	private void updateSines() {
		sinLat = Math.sin(Math.toRadians(coLat));
		cosLat = Math.cos(Math.toRadians(coLat));
		sinLon = Math.sin(Math.toRadians(longitude));
		cosLon = Math.cos(Math.toRadians(longitude));
	}
	
	/**
	 * Create a toString suitable for making a station list.
	 */
	@Override
	public String toString() {
		return String.format("%-5s %-2s %-2s %8.4f %9.4f %6.2f", 
				staID.staCode, staID.locCode, staID.netCode, latitude, 
				longitude, elevation);
	}
}
