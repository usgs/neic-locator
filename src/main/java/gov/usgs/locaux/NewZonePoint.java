package gov.usgs.locaux;

import java.io.Serializable;

/**
 * Keep track of statistics for one new ZoneStats grid point.
 * 
 * @author Ray Buland
 *
 */
public class NewZonePoint implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
	private static final long serialVersionUID = 1L;
	
	/** Geographic longitude in degrees. */
	double lon;
	
	/** Number of data behind the statistic. */
	int count;
	
	/** Mean depth in kilometers. */
	double depth;
	
	/** Depth standard error in kilometers. */
	double depthError;

	/**
	 * @return The geographic longitude in degrees
	 */
	public double getLon() {
		return lon;
	}

	/**
	 * @return The number of data behind the statistic
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @return The mean depth in kilometers
	 */
	public double getDepth() {
		return depth;
	}

	/**
	 * @return The depth standard error in kilometers
	 */
	public double getDepthError() {
		return depthError;
	}

	/**
	 * The constructor just remembers the statistics.
	 * 
	 * @param lon The geographical longitude in degrees
	 * @param count The number of data behind the statistic
	 * @param depth The mean depth in kilometers
	 * @param depthError The depth standard error in kilometers
	 */
	public NewZonePoint(double lon, int count, double depth, double depthError) {
		this.lon = lon;
		this.count = count;
		this.depth = depth;
		this.depthError = depthError;
	}
	
	@Override
	public String toString() {
		return String.format("NewZonePoint: lon = %9.4f depth = %6.2f spread = %6.2f", 
				lon, depth, depthError);
	}
}
