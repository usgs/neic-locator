package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Keep track of the geographic boundaries of continental cratons.  
 * Both travel times and magnitudes are sensitive to the age and 
 * stability of the crust.  For example, in cratons, Pg and Pb are 
 * distinct phases and mbLg is a preferred magnitude.  In active 
 * areas, Pb is an extension of Pg instead of a distinct phase 
 * and Ml is preferred.
 * 
 * @author Ray Buland
 *
 */
public class Cratons {
	ArrayList<Craton> cratons;
	
	/**
	 * Allocate an array list of cratons.
	 */
	public Cratons() {
		cratons = new ArrayList<Craton>();
	}
	
	/**
	 * Add a new craton.
	 * 
	 * @param region Region name
	 * @param lats Geographic latitude points defining the boundary 
	 * polygon of the craton 
	 * @param lons Geographic longitude points defining the boundary 
	 * polygon of the craton
	 */
	public void add(String region, double[] lats, double[] lons) {
		cratons.add(new Craton(region, lats, lons));
	}
	
	/**
	 * Determine if an epicenter is inside a craton.
	 * 
	 * @param latitude Geographical epicenter latitude in degrees
	 * @param longitude Geographical epicenter longitude in degrees
	 * @return True if the epicenter is inside a craton
	 */
	public boolean isCraton(double latitude, double longitude) {
		for(int j=0; j<cratons.size(); j++) {
			if(cratons.get(j).isCraton(latitude, longitude)) return true;
		}
		return false;
	}
	
	/**
	 * Dump the data for all cratons.
	 */
	public void printCratons() {
		for(int j=0; j<cratons.size(); j++) {
			cratons.get(j).printCraton();
		}
	}
}
