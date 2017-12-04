package gov.usgs.locator;

/**
 * Holds everything known about one seismic phase at a particular 
 * source depth and source-receiver distance.
 * 
 * @author Ray Buland
 *
 */
public class TTimeData implements Comparable<TTimeData> {
	String phCode;					// Phase code
	double tt;							// Travel time (s)
	double dTdD;						// Derivative of time with respect to 
													// distance (s/degree)
	double dTdZ;						// Derivative of time with respect to depth
													// (s/km)
	double dXdP;						// Derivative of distance with respect to 
													// ray parameter (degree-s)
	double spread;					// Statistical spread (s)
	double observ;					// Relative statistical observability
	double window;					// Association window in seconds
	String phGroup;					// Teleseismic phase group
	String auxGroup;				// Auxiliary phase group
	boolean isRegional;			// If true, phase is regional
	boolean isDepth;				// If true, phase is depth sensitive
	boolean canUse;					// If true, can use the phase for location
	boolean dis;						// Disrespect (down weight) this phase
	boolean corrTt;					// If true, get the arrival time from the phase statistics
	
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
	public TTimeData(String phCode, double tt, double dTdD, 
			double dTdZ, double dXdP) {
		this.phCode = phCode;
		this.tt = tt;
		this.dTdD = dTdD;
		this.dTdZ = dTdZ;
		this.dXdP = dXdP;
		corrTt = false;
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
		window = Math.max(TauUtil.ASSOCFACTOR*spread, TauUtil.WINDOWMIN);
	}
	
	/**
	 * Add phase flags.
	 * 
	 * @param phGroup Base phase group
	 * @param auxGroup Auxiliary phase group
	 * @param isRegional True if this is a regional phase
	 * @param isDepth True if this is a depth phase
	 * @param canUse True if this phase can be used in an 
	 * earthquake location
	 * @param dis True if this phase should be down weighted 
	 * during phase identification
	 */
	public void addFlags(String phGroup, String auxGroup, 
			boolean isRegional, boolean isDepth, boolean canUse, 
			boolean dis) {
		this.phGroup = phGroup;
		this.auxGroup = auxGroup;
		this.isRegional = isRegional;
		this.isDepth = isDepth;
		this.canUse = canUse;
		this.dis = dis;
	}
	
	/**
	 * Make arrival times sortable into time order.
	 * 
	 * @param arrival An travel-time data object.
	 * @return +1, 0, or -1 if arrival is later, the same time 
	 * or earlier
	 */
	@Override
	public int compareTo(TTimeData arrival) {
		// Sort into arrival time order.
		if(this.tt < arrival.tt) return +1;
		else if(this.tt == arrival.tt) return 0;
		else return -1;
	}
	
	/**
	 * Return this arrival formatted similarly to the arrival 
	 * time list produced by the Locator version of Ttim.
	 */
	@Override
	public String toString() {
		return String.format("%-8s %7.2f %10.2e %10.2e %5.2f %7.1f  "+
				"%-6s %-6s %-5b %-5b %-5b %-5b\n", phCode, tt, dTdD, 
				dTdZ, spread, observ, phGroup, auxGroup, canUse, isRegional, 
				isDepth, dis);
	}
}