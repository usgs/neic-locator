package gov.usgs.locator;

/**
 * Keep all the data for one pick together.
 * 
 * @author Ray Buland
 *
 */
public class Pick implements Comparable<Pick> {
	// Hydra specific:
	String dbID;					// Hydra aid to database update
	// Inputs:
	Station station;			// Station information
	String chaCode;				// Channel code
	double arrivalTime;		// Arrival time in seconds since the epoch
	double quality;				// Pick quality (standard error) in seconds
	boolean use;					// If true the phase may be used
	String obsCode;				// Original phase identification
	String authority;			// Authority for the original phase identification
	double affinity;			// Higher numbers make it harder to re-identify the phase
	// Input/Output:
	String phCode;				// Current phase identification
	// Outputs:
	double residual;			// Observed-theoretical arrival time in seconds
	double delta;					// Source-receiver distance in degrees
	double azimuth;				// Receiver azimuth from the source in degrees
	boolean used;					// True if the pick was used in the location
	double weight;				// Pick weight
	double importance;		// pick data importance
	
	/**
	 * Create the pick with just enough information to be useful.
	 * 
	 * @param station Station ID
	 * @param latitude Geographic latitude in degrees
	 * @param longitude Longitude in degrees
	 * @param elevation Elevation in kilometers
	 * @param arrivalTime Arrival time in seconds since the epoch
	 * @param use If true, the pick may be used in a a location
	 * @param phCode Current locator or associator phase code
	 */
	public Pick(Station station, String chaCode, double arrivalTime, 
			boolean use, String phCode) {
		// Remember the inputs.
		this.station = station;
		this.chaCode = chaCode;
		this.arrivalTime = arrivalTime;
		this.use = use;
		this.phCode = phCode;
		// Set defaults.
		dbID = null;
		quality = 0d;
		obsCode = null;
		authority = null;
		affinity = 3d;
		used = use;
		residual = Double.NaN;
		delta = Double.NaN;
		azimuth = Double.NaN;
		weight = Double.NaN;
		importance = Double.NaN;
	}
	
	/**
	 * Additional information to help in phase association or location.
	 * 
	 * @param dbID Data base ID (convenience for Hydra)
	 * @param quality Pick uncertainty in seconds (not currently used)
	 * @param obsCode Original pick identification (associator or analyst)
	 * @param authority Source of the original phase identification
	 * @param affinity Higher numbers make it harder to re-identify the phase
	 */
	public void addIdAids(String dbID, double quality, String obsCode, 
			String authority, double affinity) {
		this.dbID = dbID;
		this.quality = quality;
		this.obsCode = obsCode;
		this.authority = authority;
		this.affinity = affinity;
	}
	
	/**
	 * When the phase is re-identified, these parameters need to change.
	 * 
	 * @param phCode Current phase code
	 * @param weight Pick regression weight
	 */
	public void updateID(String phCode, double weight) {
		this.phCode = phCode;
		this.weight = weight;
	}

	/**
	 * The used flag can only change from true to false. 
	 */
	public void dontUse() {
		used = false;
	}
	
	/**
	 * These parameters need to be updated each time the travel time is 
	 * recomputed.
	 * 
	 * @param hypo Hypocenter object
	 */
	public void updatePick(Hypocenter hypo) {
		delta = LocUtil.delAz(hypo, station);
		azimuth = LocUtil.azimuth;
	}
	
	/**
	 * Update the station used flag for this pick.
	 */
	public void updateUsed() {
		if(used) station.used = true;
	}
	
	/**
	 * Print out the input pick information in a format similar to 
	 * the Hydra event input file.
	 */
	public void printIn() {
		System.out.format("%10s %-5s %3s %2s %2s %8.4f %9.4f %5.2f %3.1f "+
				"%-8s %12s %5b %-11s %-8s %3.1f\n", dbID, station.staID.staCode, 
				chaCode, station.staID.netCode, station.staID.locCode, 
				station.latitude, station.longitude, station.elevation, quality, 
				phCode, LocUtil.getRayTime(arrivalTime), use, authority, obsCode, 
				affinity);
	}
	
	/**
	 * Print the pick part of a Bulletin Hydra style output file.
	 */
	public void printHydra() {
		System.out.format("%-10s %-5s %-3s %-2s %-2s %-8s%6.1f %5.1f %3.0f "+
				"%1s %4.2f %6.4f\n", dbID, station.staID.staCode, chaCode, 
				station.staID.netCode, station.staID.locCode, phCode, residual, 
				delta, azimuth, LocUtil.getBoolChar(used), weight, importance);
	}

	/**
	 * Apparently, implementing Comparable allows sorting of arrays, but not 
	 * ArrayLists.
	 */
	@Override
	public int compareTo(Pick pick) {
		// Sort into arrival time order.
		if(this.arrivalTime < pick.arrivalTime) return +1;
		else if(this.arrivalTime == pick.arrivalTime) return 0;
		else return -1;
	}
}
