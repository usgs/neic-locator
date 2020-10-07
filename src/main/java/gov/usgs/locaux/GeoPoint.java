package gov.usgs.locaux;

/**
 * Holds one geographical point and the ZoneStats indices for the cell 
 * it is in.
 * 
 * @author Ray Buland
 *
 */
public class GeoPoint {
	double lat;			// Geographical colatitude (0-180 degrees)
	double lon;			// Geographical longitude (0-360 degrees)
	double depth = Double.NaN;		// ZoneStats depth value
	int latIndex;		// ZoneStats cell colatitude index
	int lonIndex;		// ZoneStats cell longitude index
	double x;				// Earth flattened coordinate in degrees positive east
	double y;				// Earth flattened coordinate in degrees positive north
	
	/**
	 * @return Geographical colatitude (0-180 degrees)
	 */
	public double getLat() {return lat;}
	
	/**
	 * @return Geographical longitude (0-360 degrees)
	 */
	public double getLon() {return lon;}
	
	/**
	 * @return ZoneStats cell latitude index
	 */
	public int getLatIndex() {return latIndex;}
	
	/**
	 * @return ZoneStats cell longitude index
	 */
	public int getLonIndex() {return lonIndex;}
	
	/**
	 * @return The Earth flattened x coordinate in degrees.
	 */
	public double getX() {return x;}
	
	/**
	 * @return The Earth flattened y coordinate in degrees.
	 */
	public double getY() {return y;}
	
	/**
	 * @param depth ZoneStats maximum earthquake depth
	 */
	public void setDepth(double depth) {
		this.depth = depth;
	}
	
	/**
	 * Construct a new geographical point by colatitude/longitude.
	 * 
	 * @param lat Geographical colatitude (0-180 degrees)
	 * @param lon Geographical longitude (0-360 degrees)
	 */
	public GeoPoint(double lat, double lon) {
		// Make sure the geographic coordinates make sense.
		this.lat = wrapMod(lat, 180d);
		this.lon = wrapMod(lon, 360d);
		// Get the ZoneStats indices.
		getIndices();
		// Initialize the Earth flattening reference coordinates.
		x = 0d;
		y = 0d;
	}
	
	/**
	 * Construct a new geographical point by ZoneStats colatitude/
	 * longitude indices.
	 * 
	 * @param latIndex ZoneStats colatitude index
	 * @param lonIndex ZoneStats longitude index
	 */
	public GeoPoint(int latIndex, int lonIndex, GeoPoint reference) {
		// The actual coordinates are offset from the indices.
		lat = wrapMod(latIndex + 0.5d, 180d);
		lon = wrapMod(lonIndex + 0.5d, 360d);
		// Remember the indices.
		this.latIndex = latIndex;
		this.lonIndex = lonIndex;
		// Do the Earth flattening transformation.
		x = Math.sin(Math.toRadians(lat)) * (lon - reference.getLon());
		y = reference.getLat() - lat;
	}
	
	/**
	 * Using the Earth flattening, compute the Cartesian distance in degrees 
	 * between this GeoPoint and a trial GeoPoint.
	 * 
	 * @param trial Trial geographical point
	 * @return Distance between this geographical point and the trial 
	 * geographical point in degrees
	 */
	public double getDistance(GeoPoint trial) {
		return Math.sqrt(Math.pow(trial.getX() - x,  2d) + 
				Math.pow(trial.getY() - y, 2d));
	}
	
	/**
	 * Set the Bayesian depth from the ZoneStats information.
	 * 
	 * @param zoneStats Zone statistics class
	 * @param dist Distance of this cell center from the trial point (debug only)
	 */
	public void setBayesDepth(ZoneStats zoneStats, double dist) {
    // Get the raw statistics.
    ZoneStat stat = zoneStats.getStats(latIndex, lonIndex);
    
    if (stat != null) {
      // Trap bad depths.
      double meanDepth =
          Math.min(Math.max(stat.getMeanFreeDepth(), LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);
      double minDepth =
          Math.min(Math.max(stat.getMinimumFreeDepth(), LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);
      double maxDepth =
          Math.min(Math.max(stat.getMaximumFreeDepth(), LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);

      // Trap other ugly errors.
      if (minDepth >= maxDepth || meanDepth <= minDepth || meanDepth >= maxDepth) {
        if (meanDepth < 400d) { // There's nothing magic about 400 km.
          minDepth = Math.max(meanDepth - 0.5 * LocUtil.DEFAULTDEPTHSE, LocUtil.DEPTHMIN);
          maxDepth = minDepth + LocUtil.DEFAULTDEPTHSE;
        } else {
          maxDepth = Math.min(meanDepth + 0.5 * LocUtil.DEFAULTDEPTHSE, LocUtil.DEPTHMAX);
          minDepth = maxDepth - LocUtil.DEFAULTDEPTHSE;
        }
      }
      // Print the Bayesian depth and spread.
      double bayesSpread =
          Math.max(Math.max(maxDepth - meanDepth, meanDepth - minDepth) / 3d, 
          		LocUtil.DEFAULTDEPTHSE);
      System.out.format("BayesDepth: %6.2f < %6.2f < %6.2f +/- %6.2f (%4.2)\n", minDepth, 
      		meanDepth, maxDepth, bayesSpread, dist);
      /*
       * Oddly, the mean depth seems to be a better indicator of a deep earthquake zone than 
       * the deepest depth.  There seem to be several historical reasons for this behavior: 
       * 1) if there are shallow earthquakes in this cell, they must have all had their depths 
       * held so that the mean just sampled the deep zone and 2) the deepest depth is typically 
       * way too deep because of poor depth control (events with depth controlled by P-pP times 
       * were held to the average P-pP depth) and so were not counted in the mean free depth).
       */
      depth = meanDepth;
    } else {
    	// If there are no statistics, default to a shallow source.
    	depth = LocUtil.DEFAULTDEPTH;
    }
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
	 * Compute the ZoneStats indices from the geographic coordinates.
	 */
	private void getIndices() {
	    if (lat < 180d) {
	      latIndex = (int) lat;
	    } else {
	      latIndex = 179;
	    }
	    if (lat > 0d && lat < 180d) {
	      lonIndex = (int) lon;
	    } else {
	      lonIndex = 0;
	    }
//	    System.out.format("New indices: %2d %3d\n", latIndex, lonIndex);
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
	  public String toString() {
		  if(Double.isNaN(depth)) {
			  return String.format("%5.2f %6.2f (%2d %3d) => (%4.2f, %4.2f)", 
					  lat, lon, latIndex, lonIndex, x, y);
		  } else {
			  return String.format("%5.2f %6.2f (%2d %3d) => (%4.2f, %4.2f) depth = %6.2f", 
					  lat, lon, latIndex, lonIndex, x, y, depth);
		  }

	  }
}
