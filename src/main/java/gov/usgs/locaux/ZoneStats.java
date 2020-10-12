package gov.usgs.locaux;

import java.io.Serializable;
import java.util.ArrayList;

import gov.usgs.locator.BayesianDepth;
import gov.usgs.locator.DepthSource;

/**
 * The ZoneStats class implements the historical free depth statistics portion of the SEDAS zone
 * statistics subsystem. Note that the zone keys was a crude SEDAS era accelerator for the
 * statistics. This had two advantages. First, being Marsden square based, it was fast. Second the
 * statistics themselves were compact as most Marsden squares don't have enough earthquakes to do
 * statistics on. To save precious memory, both the keys and statistics were accessed from random
 * access files.
 *
 * @author Ray Buland
 */
public class ZoneStats implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** An int containing the size of the zone statistics array. */
  private int size;

  /** An int containing the number of years used to compute the statistics. */
  private int numYears;

  /** An int[][] used to index into the statistics based on Marsden squares. */
  private int[][] zoneKeys;

  /** An array of ZoneStat objects containing the statistics for one Marsden square. */
  private ZoneStat[] zoneStats;

  /** A double containing the best Bayesian spread from the zone statistics in kilometers. */
  private double bayesSpread;

  /**
   * The ZoneStats constructor. Sets up the zone keys and allocate the zone statistics.
   *
   * @param zoneKeys An int[][] used to index into the statistics based on Marsden squares
   */
  public ZoneStats(int[][] zoneKeys) {
    this.zoneKeys = zoneKeys;
    size = 0;
    bayesSpread = Double.NaN;

    // Find the biggest key.
    for (int j = 0; j < zoneKeys.length; j++) {
      for (int i = 0; i < zoneKeys[j].length; i++) {
        size = Math.max(size, zoneKeys[j][i]);
      }
    }
  }

  /**
   * Function to get the size of the zone statistics array.
   *
   * @return An int containing the size of the zone statistics array.
   */
  public int size() {
    return size;
  }

  /**
   * The function to get the best Bayesian spread from the zone statistics. Note that this value is
   * computed by the latest call to bayesDepth.
   *
   * @return A double containing the best Bayesian spread from the zone statistics in kilometers.
   */
  public double getBayesSpread() {
    return bayesSpread;
  }

  /**
   * Function to get the number of years in the statistics.
   *
   * @return An int containing the number of years in the statistics
   */
  public int getNumYears() {
    return numYears;
  }

  /**
   * Function to add sdd statistics for a zone.
   *
   * @param numYears An int holding the number of years in the statistics
   * @param zoneStats A ZoneStat[] containing the Zone depth statistics
   */
  public void addStats(int numYears, ZoneStat[] zoneStats) {
    this.numYears = numYears;
    this.zoneStats = zoneStats;
  }

  /**
   * Function to get the raw depth statistics by latitude/longitude.
   *
   * @param latitude A double containing the geographic latitude in degrees
   * @param longitude A double containing the geographic longitude in degrees
   * @return A ZoneStat object containing the zone statistics
   */
  public ZoneStat getStats(double latitude, double longitude) {
  	GeoPoint trial;
  	
		trial = getZoneCoord(latitude, longitude);
    // Bail on bad coordinates.
    if(trial == null) {
      return null;
    }

    // Get the statistics.
    int key = zoneKeys[trial.getLonIndex()][trial.getLatIndex()];
    if (key >= 0) {
      return zoneStats[key];
    } else {
      return null;
    }
  }
  
  /**
   * Function to get the raw depth statistics by latitude/longitude indices.
   *
   * @param latIndex ZoneStats colatitude index
   * @param lonIndex ZoneStats longitude index
   * @return A ZoneStat object containing the zone statistics
   */
  public ZoneStat getStats(int latIndex, int lonIndex) {
    // Get the statistics.
    int key = zoneKeys[lonIndex][latIndex];
    if (key >= 0) {
      return zoneStats[key];
    } else {
      return null;
    }
  }

  /**
   * This function gets a representative Bayesian depth statistic from the zone 
   * statistics.  Note that this covers the entire range of earthquake depths 
   * available in the ZoneStats cell referenced.  Unfortunately, this is less 
   * useful when both shallow and deep earthquakes occur in a cell.
   *
   * @param latitude A double containing the geographic latitude in degrees
   * @param longitude A double containing the geographic longitude in degrees
   * @return A double holding the bayesian depth in kilometers
   */
  public double getBayesDepth(double latitude, double longitude) {
    // Get the raw statistics.
    ZoneStat stat = getStats(latitude, longitude);

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
      // Compute the Bayesian depth and spread.
      bayesSpread =
          Math.max(Math.max(maxDepth - meanDepth, meanDepth - minDepth), LocUtil.DEFAULTDEPTHSE);

      return meanDepth;
    } else {
      bayesSpread = LocUtil.DEFAULTDEPTHSE;
      return LocUtil.DEFAULTDEPTHSE;
    }
  }
  
  /**
   * This function gets an interpolated version of the mean free earthquake 
   * depth in the ZoneStats file.
   * 
   * @param latitude Geographic latitude in degrees
   * @param longitude Geographic longitude in degrees
   * @return Probable deepest depth of the earthquake zone
   */
  public BayesianDepth interpolateBayesDepth(double latitude, double longitude) {
  	GeoPoint trial;
		ArrayList<ZoneSample> coords;
  	
		// Set up the epicenter.
		trial = getZoneCoord(latitude, longitude);
		// Generate surrounding Zone cells.
		coords = getCenters(trial);
		// Sort them by distance to the earthquake
		coords.sort(null);
		// Interpolate from the three closest cell centers to get the depth at the 
		// epicenter.
		return zoneInterp(coords, trial);
  }
  
  /**
   * Stand alone version of the old ZoneStats geographic coordinate 
   * massaging.  Note that the ZoneStats indices are computed inside 
   * the geographic point that is returned.
   * 
   * @param latitude Geographic latitude in degrees
   * @param longitude Geographic longitude in degrees
   * @return A geographic point
   */
  private GeoPoint getZoneCoord(double latitude, double longitude) {
    // We need colatitude and longitude in the range of 0-360.
    latitude = 90d - latitude;
    if (longitude < 0d) {
      longitude += 360d;
    }
    if (longitude == 0d) {
      longitude = 360d;
    }

    // Make sure the coordinates are OK.
    if (latitude <= 180d && latitude >= 0d && longitude <= 360d && longitude > 0d) {
    	// Set the trial point as the origin of the Earth flattening.
    	GeoPoint coord = new GeoPoint(latitude, longitude);
    	return coord;
    } else {
      return null;
    }
  }
  
  /**
   * Generate the centers of the ZoneStats cells surrounding a trial point.
   * 
   * @param trial Geographic trial point (typically an earthquake epicenter)
   * @return A list of ZoneStats cells
   */
  public ArrayList<ZoneSample> getCenters(GeoPoint trial) {
	  ArrayList<ZoneSample> coords;
	  
  	coords = new ArrayList<ZoneSample>();
  	// Add the zone samples.
  	if((trial.getLat() < trial.getLatIndex() + 0.5d  && trial.getLat() >= 0.5d)  || 
  		trial.getLat() > 179.5d) {
  		// We just need six cells above and including the trial point.
  	  for(int i = trial.getLatIndex() - 1; i <= trial.getLatIndex(); i++) {
  	  	for(int j = trial.getLonIndex() - 1; j <= trial.getLonIndex() + 1; j++) {
  	  		coords.add(new ZoneSample(i, j, trial));
  	  	}
  	  }
  	} else {
  		// We just need six cells below and including the trial point.
  	  for(int i = trial.getLatIndex(); i <= trial.getLatIndex() + 1; i++) {
  	  	for(int j = trial.getLonIndex() - 1; j <= trial.getLonIndex() + 1; j++) {
  	  		coords.add(new ZoneSample(i, j, trial));
  	  	}
  	  }
  	}
  	return coords;
  }
  	
  /**
   * Interpolate the maximum earthquake depth from ZoneStats cells surrounding 
   * the trial point.
   * 
   * @param coords List of ZoneSamples surrounding the trial point
   * @param trial Geographic trial point (typically an earthquake epicenter)
   * @return Interpolated maximum earthquake depth
   */
	private BayesianDepth zoneInterp(ArrayList<ZoneSample> coords, GeoPoint trial) {
		int nulls = 0;
		double deepest;
		// The trial point is always at Earth flattening coordinates (0, 0).
		double[] result = {0d, 0d, Double.NaN}, intersect;
		double[][] vectors = new double[3][];
		ArrayList<GeoPoint> poly;
		
		// From the six nearest ZoneStat cells, construct a three point polygon 
		// surrounding the trial point.
		poly = new ArrayList<GeoPoint>();
		for(int j = 0; j < 3; j++) {
			poly.add(coords.get(j).getCoord());
		}
		// Be careful about ending up with three points in a line and oddness 
		// around the poles.
		if(poly.get(0).getLat() == poly.get(1).getLat() && 
				poly.get(1).getLat() == poly.get(2).getLat()) {
			poly.remove(2);
			poly.add(coords.get(3).getCoord());
		}
		// Debug print.
//		for(int j = 0; j < poly.size(); j++) {
//			System.out.println("Poly: " + j + " " + poly.get(j));
//		}
		// Now set the Bayesian depths.
		for(int j = 0; j < poly.size(); j++) {
			poly.get(j).setBayesDepth(this);
		}
		
		// Get the 3-vectors needed by the interpolation.
		deepest = 0d;
		for(int j = 0; j < 3; j++) {
			vectors[j] = poly.get(j).getVector();
			deepest = Math.max(deepest, vectors[j][2]);
		}
		
		// Filter our points that don't fit (presumably on the edge of a structure).
		if(deepest <= LocUtil.SHALLOWESTDEEP) {
			// For shallow and intermediate depths, filter points that don't fit the nearest.
			for(int j = 1; j < vectors.length; j++) {
				if(Math.abs(vectors[j][2] - vectors[0][2]) > LocUtil.STRUCTURETOL[0]) {
					vectors[j] = null;
					nulls++;
				}
			}
		} else {
			// For deep zones, just use the deep points since there is always a shallow zone.
			for(int j = 0; j < vectors.length; j++) {
				if(deepest - vectors[j][2] > LocUtil.STRUCTURETOL[1]) {
					vectors[j] = null;
					nulls++;
				}
			}
		}
		// Debug print.
//		for(int j = 0; j < 3; j++) {
//			if(vectors[j] != null) {
//				System.out.format("Vector: %d (%5.2f, %5.2f, %6.2f)\n", j, vectors[j][0], 
//						vectors[j][1], vectors[j][2]);
//			} else {
//				System.out.format("Vector: %d null\n", j);
//			}
//		}
		
		// Do linear interpolation.
		switch(nulls) {
		case 0:
			// We have three points.  Fit a plane to the triangle defined by the polygon.
			Linear.twoD(vectors[0], vectors[1], vectors[2], result);
//			System.out.format("3-point interpolation: %4.2f\n", result[2]);
			return new BayesianDepth(result[2], LocUtil.DEFAULTSLABSE, DepthSource.ZONESTATS);
		case 1:
			// We have two points.  Interpolate using the intersection between the line and 
			// a perpendicular through the trial point.
			int k = 0;
			for(int j = 0; j < vectors.length; j++) {
				if(vectors[j] != null) {
					vectors[k++] = vectors[j];
				}
			}
			intersect = Linear.intersect(vectors[0], vectors[1], result);
			Linear.oneD(vectors[0], vectors[1], intersect);
//			System.out.format("Intersect: %5.2f %6.2f %4.2f\n", intersect[0], intersect[1], 
//					intersect[2]);
			// Inflate the error to reflect the edge uncertainty.
			return new BayesianDepth(intersect[2], 1.5d * LocUtil.DEFAULTSLABSE, 
					DepthSource.ZONESTATS);
		case 2:
			// We only have one point, so use it.
			for(int j = 0; j < vectors.length; j++) {
				// Inflate the errors even more as we're hanging a lot on one point.
				if(vectors[j] != null) return new BayesianDepth(vectors[j][2], 
						2d * LocUtil.DEFAULTSLABSE, DepthSource.ZONESTATS);
			}
		default:
			System.out.println("How can there be too many nulls?");
			return null;
		}
	}
}
