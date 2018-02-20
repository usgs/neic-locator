package gov.usgs.locator;

import gov.usgs.traveltime.TauUtil;

/**
 * Keep track of the geometry of one craton.
 * 
 * @author Ray Buland
 *
 */
public class Craton {
	String region;	// Region name
	double[] lats;	// Geographic latitude points of boundary in degrees
	double[] lons;	// Geographic longitude points of boundary in degrees
	
	/**
	 * Save the craton data.
	 * 
	 * @param region Region name
	 * @param lats Geographic latitude points defining the boundary 
	 * polygon of the craton 
	 * @param lons Geographic longitude points defining the boundary 
	 * polygon of the craton
	 */
	public Craton(String region, double[] lats, double[] lons) {
		this.region = region;
		this.lats = lats;
		this.lons = lons;
	}
	
	/**
	 * Determine if an epicenter is inside this craton.
	 * 
	 * @param latitude Geographic epicenter latitude in degrees
	 * @param longitude Geographic epicenter longitude in degrees
	 * @return True if the epicenter is inside this craton
	 */
	public boolean isCraton(double latitude, double longitude) {
		int cross = 0;
		double slope, testLat;
		
		// Stay away from the poles and the date line.
		if(Math.abs(Math.abs(latitude)-90d) < TauUtil.DTOL) 
			latitude -= Math.copySign(TauUtil.DTOL, latitude);
		if(Math.abs(Math.abs(longitude)-180d) < TauUtil.DTOL) 
			longitude -= Math.copySign(TauUtil.DTOL, longitude);
		// Count crossings of the polygon's sides.
		for(int j=1; j<lats.length; j++) {
			slope = (lons[j]-lons[j-1])/(lats[j]-lats[j-1]);
			testLat = (longitude-lons[j-1]+slope*lats[j-1])/slope;
			if(testLat >= latitude && (testLat-lats[j])*(testLat-lats[j-1]) <= 0d) cross++;
		}
		// If the number of crossings is odd, the epicenter is inside.
		if(cross%2 > 0) return true;
		else return false;
	}
	
	/**
	 * Print the data for this craton.
	 */
	public void printCraton() {
		System.out.println("\n"+region);
		for(int j=0; j<lats.length; j++) {
			System.out.format("\t%8.4f %9.4f\n", lats[j], lons[j]);
		}
	}
}
