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
	Station station;			// Station
	String chaCode;				// Channel code
	double arrivalTime;		// Arrival time in seconds since the epoch
	double quality;				// Pick quality (standard error) in seconds
	boolean use;					// If true, the phase may be used
	String obsCode;				// Original phase identification
	AuthorType authType;	// Author type for the original phase identification
	double affinity;			// Higher numbers make it harder to re-identify the phase
	// Input/Output:
	String phCode;				// Current phase identification
	// Outputs:
	double residual;			// Observed-theoretical arrival time in seconds
	boolean used;					// True if the pick was used in the location
	double weight;				// Pick weight
	double importance;		// Pick data importance
	// Internal use:
	String idCode;				// Best code to use for phase identification
	double tt;						// Travel-time
	double fomStat;				// Statistical figure-of-merit
	int mapStat;					// Travel-time index with the maximum fomStat
	double fomAlt;				// Alternate figure-of-merit
	boolean hide;					// If true, hide the phase from the identification logic
	int mapAlt;						// Travel-time index with the maximum fomAlt
	
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
		authType = null;
		affinity = 3d;
		used = use;
		residual = Double.NaN;
		weight = Double.NaN;
		importance = Double.NaN;
		// Initialize internal variables too.
		idCode = phCode;
		tt = Double.NaN;
		fomStat = 0d;
		mapStat = -1;
		fomAlt = TauUtil.DMAX;
		mapAlt = -1;
		hide = false;
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
			AuthorType authType, double affinity) {
		this.dbID = dbID;
		this.quality = quality;
		this.obsCode = obsCode;
		this.authType = authType;
		this.affinity = authType.affinity(affinity);
		// Use an enum for the author type.
		switch(authType) {
			case CONTRIB_HUMAN: case LOCAL_HUMAN:
				idCode = obsCode;
				break;
			default:
				idCode = phCode;
				break;
		}
	}
	
	/**
	 * Recompute the travel time for this pick when the hypocenter 
	 * is updated.
	 * 
	 * @param hypo Hypocenter information
	 */
	public void updateTt(Hypocenter hypo) {
		tt = arrivalTime-hypo.originTime;
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
		// The phase code used for phase identification only changes 
		// for automatic picks.
		switch(authType) {
		case CONTRIB_HUMAN: case LOCAL_HUMAN:
			break;
		default:
			idCode = phCode;
			break;
		}
	}

	/**
	 * The used flag can only change from true to false. 
	 */
	public void dontUse() {
		used = false;
	}

	/**
	 * Sort picks by arrival time.
	 */
	@Override
	public int compareTo(Pick pick) {
		// Sort into arrival time order.
		if(this.arrivalTime < pick.arrivalTime) return +1;
		else if(this.arrivalTime == pick.arrivalTime) return 0;
		else return -1;
	}
}
