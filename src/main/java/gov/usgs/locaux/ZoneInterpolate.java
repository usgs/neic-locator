package gov.usgs.locaux;

import java.util.ArrayList;

import gov.usgs.locator.BayesianDepth;
import gov.usgs.locator.DepthSource;

/**
 * ZoneStats interpolation methods.  Note that this code should be common 
 * for all different ZoneStats implementations.
 * 
 * @author Ray Buland
 *
 */
public class ZoneInterpolate {
  
  /**
   * This function computes an interpolated version of the mean free earthquake 
   * depth in the ZoneStats file.  Note that processing the latitude and 
   * longitude and generating the indices is implementation specific.
   * 
   * @param latitude Geographic latitude in degrees (-90, 90)
   * @param longitude Geographic longitude in degrees (-180, 180)
   * @param zoneStats Any ZoneStats object that extends the abstract model
   * @return Probable deepest depth of the earthquake zone
   */
  public BayesianDepth interpolateBayesDepth(double latitude, double longitude, 
  		AbstractZoneStats zoneStats) {
  	DepthSource source;
  	GeoPoint trial;
  	ArrayList<GeoPoint> coords;
  	
		// Get the canonical coordinates.
    zoneStats.canonicalCoords(latitude, longitude);
    // Get the zone indices.
    zoneStats.getIndices();
		// Check the coordinates.
		if(Double.isNaN(zoneStats.getCoLat()) || Double.isNaN(zoneStats.getCoLon())) {
			return null;
		}
		// Set up the epicenter.
		trial = new GeoPoint(zoneStats.coLat, zoneStats.coLon);
//	LOGGER.fine("Epicenter: " + trial);
		// Generate surrounding Zone cells.
		coords = getCenters(trial, zoneStats);
		// Sort them by distance to the earthquake
		coords.sort(null);
		// Debug print.
//	for(GeoPoint sample : coords) {
//		LOGGER.fine(sample);
//	}
		// Interpolate from the three closest cell centers to get the depth at the 
		// epicenter.
		source = zoneStats.getDepthSource();
		return zoneInterp(coords, trial, source);
  }
  
  /**
   * Generate the centers of the ZoneStats cells surrounding a trial point.
   * 
   * @param trial Geographic trial point (typically an earthquake epicenter)
   * @param zoneStats Any ZoneStats object that extends the abstract model
   * @return A list of ZoneStats points (or cell centers)
   */
   public ArrayList<GeoPoint> getCenters(GeoPoint trial, AbstractZoneStats zoneStats) {
  	int latIndex, lonIndex, jWrapped;
  	double coLat;
	  ArrayList<GeoPoint> coords;
	  
	  // Allocate temporary storage.
  	coords = new ArrayList<GeoPoint>();
  	
	  // Add longitude points bracketing the epicenter in the base latitude row.
	  latIndex = zoneStats.latIndex;
	  lonIndex = zoneStats.lonIndex;
	  coLat = zoneStats.latFromIndex(latIndex);
  	for(int j = lonIndex - 1; j <= lonIndex + 1; j++) {
  		jWrapped = zoneStats.wrapLonIndex(latIndex, j);
  		coords.add(new GeoPoint(coLat, zoneStats.lonFromIndex(latIndex, jWrapped), 
  				trial));
  		coords.get(coords.size() - 1).setBayesDepth(zoneStats.getBayesDepth(latIndex, 
  				jWrapped));
  	}
  	
  	// Decide which latitude row will best bracket the trial point.  Note that we 
  	// have to be careful about trials near the first and last latitude rows.
  	if((trial.getLat() < coLat  && trial.getLat() >= zoneStats.firstRowLat) || 
  			trial.getLat() >= zoneStats.lastRowLat) {
  		// Add longitude points in the latitude row above the base latitude row.
  		latIndex--;
  		lonIndex = zoneStats.newLonIndex(latIndex, trial.getLon());
  		coLat = zoneStats.latFromIndex(latIndex);
	  	for(int j = lonIndex - 1; j <= lonIndex + 1; j++) {
	  		jWrapped = zoneStats.wrapLonIndex(latIndex, j);
	  		coords.add(new GeoPoint(coLat, zoneStats.lonFromIndex(latIndex, jWrapped), 
	  				trial));
	  		coords.get(coords.size() - 1).setBayesDepth(zoneStats.getBayesDepth(latIndex, 
	  				jWrapped));
	  	}
  	} else {
  		// Add the longitude points in the latitude row below the base latitude row.
  		latIndex++;
  		lonIndex = zoneStats.newLonIndex(latIndex, trial.getLon());
  		coLat = zoneStats.latFromIndex(latIndex);
	  	for(int j = lonIndex - 1; j <= lonIndex + 1; j++) {
	  		jWrapped = zoneStats.wrapLonIndex(latIndex, j);
	  		coords.add(new GeoPoint(coLat, zoneStats.lonFromIndex(latIndex, jWrapped), 
	  				trial));
	  		coords.get(coords.size() - 1).setBayesDepth(zoneStats.getBayesDepth(latIndex, 
	  				jWrapped));
	  	}
  	}
  	return coords;
  }
  	
  /**
   * Interpolate the maximum earthquake depth from ZoneStats cells surrounding 
   * the trial point.
   * 
   * @param coords List of GeoPoints surrounding the trial point
   * @param trial Geographic trial point (typically an earthquake epicenter)
   * @param source An enumeration value identifying the ZoneStats source
   * @return Interpolated maximum earthquake depth statistics
   */
 	private BayesianDepth zoneInterp(ArrayList<GeoPoint> coords, GeoPoint trial, 
			DepthSource source) {
		int nulls = 0;
		double deepest = 0d;
		double[] depths;
//	String[] label = {"Depth", "Lower", "Upper", "Spread"};
		// The trial point is always at Earth flattening coordinates (0, 0).
		double[] result = {0d, 0d, Double.NaN}, intersect;
		double[][] vectors = new double[3][];
		BayesianDepth bayesDepth;
		
		// Be careful about ending up with three points in a line and oddness 
		// around the poles.
		if(coords.get(0).getLat() == coords.get(1).getLat() && 
				coords.get(1).getLat() == coords.get(2).getLat()) {
			coords.remove(2);
		}
		// Sort coordinates with non-null Bayesian depth statistics to the top.
		sortOutNulls(coords, 3);
		
		// Count coordinates with no depth and compute the deepest depth.
		depths = new double[3];
		for(int j = 0; j < 3; j++) {
			depths[j] = coords.get(j).getDepth();
			if(!Double.isNaN(depths[j])) {
				deepest = Math.max(deepest, depths[j]);
			} else {
				nulls++;
			}
		}
		if(nulls >= 3) {
//		LOGGER.fine("All nulls before filtering.");
			return null;
		}
		
		// Filter our points that don't fit (presumably on the edge of a structure).
		if(deepest <= LocUtil.SHALLOWESTDEEP) {
			// For shallow and intermediate depths, filter points that don't fit the nearest.
			for(int j = 1; j < depths.length; j++) {
				if(!Double.isNaN(depths[j])) {
					if(Math.abs(depths[j] - depths[0]) > LocUtil.STRUCTURETOL[0]) {
						coords.get(j).setBayesDepth(null);
						nulls++;
					}
				}
			}
		} else {
			// For deep zones, just use the deep points since there is always a shallow zone.
			for(int j = 0; j < vectors.length; j++) {
				if(!Double.isNaN(depths[j])) {
					if(deepest - depths[j] > LocUtil.STRUCTURETOL[1]) {
						coords.get(j).setBayesDepth(null);
						nulls++;
					}
				}
			}
		}
		if(nulls >= 3) {
//		LOGGER.fine("All nulls after filtering.");
			return null;
		}
		
		// Sort coordinates with non-null Bayesian depth statistics to the top in case 
		// we added some.
		sortOutNulls(coords, 3);
//	for(int j = 0; j < 3; j++) {
//		LOGGER.fine("Poly\": " + j + " " + coords.get(j));
//	}
		
		// Instantiate a mostly empty BayesianDepth object.
		bayesDepth = new BayesianDepth(source);
		
		// Do linear interpolation.
		switch(nulls) {
		case 0:
			// We have three points.  Fit a plane to the triangle defined by the polygon 
			// for mean depth first.
			for(int i = 0; i <= 3; i++) {
				for(int j = 0; j < 3; j++) {
					vectors[j] = coords.get(j).getVector(i);
				}
				Linear.twoD(vectors[0], vectors[1], vectors[2], result);
				bayesDepth.setByIndex(i, result[2]);
//			LOGGER.finer(String.format("%-6s: values = (%6.2f, %6.2f, %6.2f) result = %6.2f\n", 
//					label[i], vectors[0][2], vectors[1][2], vectors[2][2], result[2]));
			}
//		LOGGER.fine("3-point interpolation: " + bayesDepth);
			return bayesDepth;
		case 1:
			// We have two points.  Interpolate using the intersection between the line and 
			// a perpendicular through the trial point for the mean depth first.
			for(int i = 0; i <= 3; i++) {
				for(int j = 0; j < 2; j++) {
					vectors[j] = coords.get(j).getVector(i);
				}
				intersect = Linear.intersect(vectors[0], vectors[1], result);
				Linear.oneD(vectors[0], vectors[1], intersect);
				bayesDepth.setByIndex(i, intersect[2]);
//			LOGGER.finer(String.format("%-6s: values = (%6.2f, %6.2f) result = %6.2f\n", 
//					label[i], vectors[0][2], vectors[1][2], intersect[2]));
			}
			// Inflate the error to reflect the edge uncertainty.
			bayesDepth.inflateSpread(1.5d);
//		LOGGER.fine("2-point interpolation: " + bayesDepth);
			return bayesDepth;
		case 2:
			// We only have one point, so use it.
			bayesDepth = coords.get(0).getBayesDepth();
			// Inflate the errors even more as we're hanging a lot on one point.
			bayesDepth.inflateSpread(2d);
//		LOGGER.fine("1-point interpolation: " + bayesDepth);
			return bayesDepth;
		default:
			// This should never happen!
			System.out.println("How can there be too many nulls?");
			return null;
		}
	}
 	
 	/**
 	 * Sort GeoPoints with non-null BayesianDepths to the top.
 	 * 
 	 * @param coords ArrayList of GeoPoints
 	 * @param length Number of values to sort
 	 */
 	private void sortOutNulls(ArrayList<GeoPoint> coords, int length) {
		int k = 0;
		for(int j = 0; j < length; j++) {
			if(coords.get(j).getBayesDepth() != null) {
				if(j != k) {
					GeoPoint temp = coords.get(j);
					coords.set(j, coords.get(k));
					coords.set(k, temp);
				}
				k++;
			}
		}
 	}
}
