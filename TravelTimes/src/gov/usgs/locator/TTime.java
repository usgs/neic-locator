package gov.usgs.locator;

/**
 * This class will contain everything known about one seismic 
 * phase at the desired source depth and source receiver 
 * distance.
 * 
 * @author Ray Buland
 *
 */
public class TTime {
	String phCode;		// Phase code
	double tt;				// Travel time (s)
	double dTdD;			// Derivative of time with respect to 
										// distance (s/degree)
	double dTdZ;			// Derivative of time with respect to depth
										// (s/km)
	double dXdP;			// Derivative of distance with respect to 
										// ray parameter (degree-s)
	double spread;		// Statistical spread (s)
	double observ;		// Relative statistical observability
	String telGroup;	// Teleseismic phase group
	String auxGroup;	// Auxiliary phase group
	boolean canUse;		// If true, can use the phase for location
	boolean dis;			// Disrespect (down weight) this phase
	
	/**
	 * The constructor accepts basic travel time information.
	 * 
	 * @param phCode Phase code
	 * @param tt Travel time
	 * @param dTdD Derivative of time with respect to distance
	 * @param dTdZ Derivative of time with respect to depth
	 * @param dXdP Derivative of distance with respect to ray 
	 * parameter
	 */
	public TTime(String phCode, double tt, double dTdD, double dTdZ, 
			double dXdP) {
		this.phCode = phCode;
		this.tt = tt;
		this.dTdD = dTdD;
		this.dTdZ = dTdZ;
		this.dXdP = dXdP;
	}
	
	/**
	 * Add phase statistical parameters.
	 * 
	 * @param spread Statistical spread
	 * @param observ Relative statistical observability
	 */
	public void addStats(double spread, double observ) {
		this.spread = spread;
		this.observ = observ;
	}
	
	/**
	 * Add phase flags.
	 * 
	 * @param telGroup Teleseismic phase group
	 * @param auxGroup Auxiliary phase group
	 * @param canUse True if this phase can be used in an 
	 * earthquake location
	 * @param dis True if this phase should be down weighted 
	 * during phase identification
	 */
	public void addFlags(String telGroup, String auxGroup, 
			boolean canUse, boolean dis) {
		this.telGroup = telGroup;
		this.auxGroup = auxGroup;
		this.canUse = canUse;
		this.dis = dis;
	}
}
