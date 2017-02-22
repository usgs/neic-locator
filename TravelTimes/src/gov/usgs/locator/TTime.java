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
	String phCode;					// Phase code
	double tt;							// Travel time (s)
	double dTdD;						// Derivative of time with respect to 
													// distance (s/degree)
	double dTdZ;						// Derivative of time with respect to depth
													// (s/km)
	double dXdP;						// Derivative of distance with respect to 
													// ray parameter (degree-s)
	double spread = 12d;		// Statistical spread (s)
	double observ = 0d;			// Relative statistical observability
	String telGroup = "";		// Teleseismic phase group
	String auxGroup = "";		// Auxiliary phase group
	boolean canUse = true;	// If true, can use the phase for location
	boolean dis = false;		// Disrespect (down weight) this phase
	
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
	
	/**
	 * Make arrival times sortable into time order.
	 * 
	 * @param arrival An arrival object.
	 * @return +1, 0, or -1 if arrival is later, the same time 
	 * or earlier
	 */
	public int compareTo(TTime arrival) {
		// Sort into arrival time order.
		if(this.tt < arrival.tt) return +1;
		else if(this.tt == arrival.tt) return 0;
		else return -1;
	}
	
	/**
	 * Return this arrival formatted similarly to the arrival 
	 * time list produced by the Locator version of Ttim.
	 */
	public String toString() {
		int min = (int)(tt/60d);
		double sec = 0.01d*(int)(100d*(tt-min*60d)+0.5d);
		if(sec >= 60d) {
			min++;
			sec -= 60d;
		}
		return String.format("%8s %7.2f %3d %5.2f %10.2e %10.2e %5.2f "+
				"%7.1f %b\n", 
				phCode, tt, min, sec, dTdD, dTdZ, spread, observ, canUse);
	}
}