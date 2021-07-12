package gov.usgs.locaux;

/**
 * Holds one geographical point for the nearest ZoneStats grid point 
 * (or cell center).
 * 
 * @author Ray Buland
 *
 */
public class GeoPoint implements Comparable<GeoPoint> {
	
	/** Geographic colatitude (0-180 degrees). */
	private double lat;
	
	/** Geographic longitude (0-360 degrees). */
	private double lon;
	
	/** Earth flattened coordinate in degrees positive east. */
	private double x;
	
	/** Earth flattened coordinate in degrees positive north. */
	private double y;
	
	/** Cartesian distance from the trial point. */
	private double dist;
	
	/** Bayesian depth in kilometers. */
	private double depth;
	
	/**
	 * @return Geographical colatitude (0-180 degrees)
	 */
	public double getLat() {return lat;}
	
	/**
	 * @return Geographical longitude (0-360 degrees)
	 */
	public double getLon() {return lon;}
	
	/**
	 * @return The Earth flattened x coordinate in degrees.
	 */
	public double getX() {return x;}
	
	/**
	 * @return The Earth flattened y coordinate in degrees.
	 */
	public double getY() {return y;}
	
	/**
	 * @return The Earth flattened Cartesian distance from the trial 
	 * point in degrees
	 */
	public double getDist() {return dist;}
	
	/**
	 * @param depth Bayesian depth in kilometers to set
	 */
	public void setDepth(double depth) {
		this.depth = depth;
	}
	
	/**
	 * @return Bayesian depth in kilometers
	 */
	public double getDepth() {
		return depth;
	}
	
	/**
	 * Construct a new geographical point by colatitude and longitude.
	 * 
	 * @param lat Geographical colatitude (0-180 degrees)
	 * @param lon Geographical longitude (0-360 degrees)
	 */
	public GeoPoint(double lat, double lon) {
		// Make sure the geographic coordinates make sense.
		this.lat = wrapMod(lat, 180d);
		this.lon = wrapMod(lon, 360d);
		// Initialize the Earth flattening reference coordinates.
		x = 0d;
		y = 0d;
		dist = 0d;
		depth = Double.NaN;
	}
	
	/**
	 * Construct a new geographical point by colatitude and longitude and 
	 * compute the Earth flattened distance to a reference point.
	 * 
	 * @param lat Geographical colatitude (0-180 degrees)
	 * @param lon Geographical longitude (0-360 degrees)
	 * @param reference Origin for the Earth flattening algorithm
	 */
	public GeoPoint(double lat, double lon, GeoPoint reference) {
		// Make sure the geographic coordinates make sense.  Note that wrapping 
		// was useful for the old ZoneStats, but is avoided in the new ZoneStats.
		this.lat = wrapMod(lat, 180d);
		this.lon = wrapMod(lon, 360d);
		// Do the Earth flattening transformation.
		x = Math.sin(Math.toRadians(lat)) * (lon - reference.getLon());
		y = reference.getLat() - lat;
		dist = getDistance();
		depth = Double.NaN;
	}
	
	/**
	 * Using the Earth flattening, compute the Cartesian distance in degrees 
	 * between this GeoPoint and a trial GeoPoint.
	 * 
	 * @param trial Trial geographical point
	 * @return Distance between this geographic point and the trial 
	 * geographic point in degrees
	 */
	public double getDistance(GeoPoint trial) {
		return Math.sqrt(Math.pow(trial.getX() - x,  2d) + 
				Math.pow(trial.getY() - y, 2d));
	}
	
	/**
	 * Using the Earth flattening, compute the Cartesian distance in degrees 
	 * between this GeoPoint and the Earth flattening origin (which is always 
	 * the reference point).
	 * 
	 * @return Distance between this geographical point and the Earth flattening 
	 * origin in degrees
	 */
	public double getDistance() {
		return Math.sqrt(Math.pow(x,  2d) + Math.pow(y, 2d));
	}
	
	/**
	 * Construct a 3-vector suitable for the linear interpolation.
	 * 
	 * @return A 3-vector containing colatitude, longitude, and maximum 
	 * ZoneStats earthquake depth
	 */
	public double[] getVector() {
		double[] vector = new double[3];
		vector[0] = x;
		vector[1] = y;
		vector[2] = depth;
		return vector;
	}

	/**
	 * Do a custom modulo ensuring the result is in the range (0, base).
	 * 
	 * @param x Value to modulo
	 * @param base Modulo base
	 * @return Result of the modulo operation
	 */
	  private double wrapMod(double x, double base) {
		  while(x > base) {
			  x -= base;
		  }
		  while(x < 0d) {
			  x += base;
		  }
		  return x;
	  }

		@Override
		public int compareTo(GeoPoint sample) {
			if(dist > sample.getDist()) {
				return +1;
			} else if(dist < sample.getDist()) {
				return -1;
			} else {
				return 0;
			}
		}
	  
	  @Override
	  public String toString() {
	  	if(Double.isNaN(depth)) {
			  return String.format("%5.2f %6.2f => (%5.2f, %5.2f) dist = %4.2f", 
					  lat, lon, x, y, dist);
	  	} else {
			  return String.format("%5.2f %6.2f => (%5.2f, %5.2f) dist = %4.2f" + 
			  		" depth = %6.2f", lat, lon, x, y, dist, depth);
	  	}
	  }
}
