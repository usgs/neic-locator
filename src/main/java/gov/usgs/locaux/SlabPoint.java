package gov.usgs.locaux;

import java.io.Serializable;

/**
 * Store slab depth information for one latitude and longitude.
 * 
 * @author Ray Buland
 *
 */
public class SlabPoint implements Serializable {
	private static final long serialVersionUID = 1L;
	double lat;					// Geographic colatitude in degrees (0-180 degrees)
	double lon;					// Geographic longitude in degrees (0-360 degrees)
	SlabDepth dep;			// Depth center and range
	
	/**
	 * @return Geographic colatitude in degrees (0-180 degrees)
	 */
	public double getLat() {
		return lat;
	}
	
	/**
	 * @return Geographic longitude in degrees (0-360 degrees)
	 */
	public double getLon() {
		return lon;
	}
	
	/**
	 * @return Earthquake depth in km
	 */
	public double getEqDepth() {
		return dep.getEqDepth();
	}
	
	/**
	 * @return Slab depth information
	 */
	public SlabDepth getDep() {
		return dep;
	}
	
	/**
	 * Create a new slab depth point
	 * 
	 * @param lat Geographic colatitude in degrees (0-180 degrees)
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @param center Earthquake depth in km
	 * @param lower Lower (shallower) earthquake depth standard deviation in km
	 * @param upper Upper (deeper) earthquake depth standard deviation in km
	 */
	public SlabPoint(double lat, double lon, double center, double lower, 
			double upper) {
		this.lat = lat;
		this.lon = lon;
		lower = Math.min(lower, 0d);
		dep = new SlabDepth(center, lower, upper);
	}
	
	@Override
	public String toString() {
		return String.format("(%6.2f, %6.2f): %s", lat, lon, dep);
	}
}
