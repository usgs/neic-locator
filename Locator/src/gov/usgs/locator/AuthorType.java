package gov.usgs.locator;

/**
 * Author types in the Hydra Locator interface are 
 * specified by integer constants from 1 to 4 with 
 * the meanings defined below.
 * 
 * @author Ray Buland
 *
 */
public enum AuthorType {
	UNKNOWN (0.0d),				// Just in case
	CONTRIB_AUTO (1.0d),	// An automatic pick from outside the NEIC
	LOCAL_AUTO (1.0d),		// An automatic pick from the NEIC
	CONTRIB_HUMAN (1.5d),	// An analyst pick from outside the NEIC
	LOCAL_HUMAN (3.0d);		// An analyst pick from the NEIC
	
	private final double defAffinity;	// Default affinity based on type
	
	/**
	 * The constructor just sets up the default affinities.
	 * 
	 * @param defAffinity Default affinity
	 */
	AuthorType(double defAffinity) {
		this.defAffinity = defAffinity;
	}
	
	/**
	 * Get the default affinity.
	 * 
	 * @return Default affinity
	 */
	public double defAffinity() {
		return defAffinity;
	}
	
	/**
	 * Apply the default to a user supplied affinity.
	 * 
	 * @param userAffinity User supplied affinity
	 * @return Final affinity
	 */
	public double affinity(double userAffinity) {
		if(userAffinity > 0d) return userAffinity;
		else return defAffinity;
	}
}
