package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Holds a set of observed travel-time statistics for one phase.  
 * There are three statistics in a set: bias from the theoretical 
 * travel time, spread about the mean, and relative observability.  
 * The observability is simply the number of hits each phase 
 * received in the trial data set.  It's absolute value is 
 * meaningless, but the relative value is useful.
 * 
 * Each of the three statistics is represented by a sequence of one 
 * or more linear fit segments.  These segments are constructed on 
 * the fly from the raw observations in one degree bins.
 * 
 * @author Ray Buland
 *
 */
public class TtStat {
	String phCode;							// Phase code
	int minDelta;								// Minimum distance in degrees
	int maxDelta;								// Maximum distance in degrees
	ArrayList<TtStatSeg> bias;		// Measured arrival time bias (s)
	ArrayList<TtStatSeg> spread;	// Measured residual spread (s)
	ArrayList<TtStatSeg> observ;	// Measured observability
	
	/**
	 * Initialize the phase statistics.
	 * 
	 * @param phCode Phase code
	 * @param minDelta Minimum observed distance in degrees
	 * @param maxDelta Maximum observed distance in degrees
	 */
	protected TtStat(String phCode, int minDelta, int maxDelta) {
		this.phCode = phCode;
		this.minDelta = minDelta;
		this.maxDelta = maxDelta;
		// set up storage for the linear fits.
		bias = new ArrayList<TtStatSeg>();
		spread = new ArrayList<TtStatSeg>();
		observ = new ArrayList<TtStatSeg>();
	}
	
	/**
	 * get the phase bias.
	 * 
	 * @param delta Distance in degrees
	 * @return Bias in seconds at distance delta
	 */
	public double getBias(double delta) {
		TtStatSeg seg;
		
		for(int k=0; k<bias.size(); k++) {
			seg = bias.get(k);
			if(delta >= seg.minDelta && delta <= seg.maxDelta) {
				return Math.max(seg.interp(delta), TauUtil.DEFBIAS);
			}
		}
		return TauUtil.DEFBIAS;
	}
	
	/**
	 * Get the phase spread.
	 * 
	 * @param delta Distance in degrees
	 * @return Spread in seconds at distance delta
	 */
	public double getSpread(double delta) {
		TtStatSeg seg;
		
		for(int k=0; k<spread.size(); k++) {
			seg = spread.get(k);
			if(delta >= seg.minDelta && delta <= seg.maxDelta) {
				return Math.min(seg.interp(delta), TauUtil.DEFSPREAD);
			}
		}
		return TauUtil.DEFSPREAD;
	}
	
	/**
	 * Get the phase observability.
	 * 
	 * @param delta Distance in degrees
	 * @return Relative observability at distance delta
	 */
	public double getObserv(double delta) {
		TtStatSeg seg;
		
		for(int k=0; k<observ.size(); k++) {
			seg = observ.get(k);
			if(delta >= seg.minDelta && delta <= seg.maxDelta) {
				return Math.max(seg.interp(delta), TauUtil.DEFOBSERV);
			}
		}
		return TauUtil.DEFOBSERV;
	}
	
	/**
	 * Print the travel-time statistics.
	 */
	protected void dumpStats() {
		// Print the header.
		System.out.println("\n"+phCode+"     "+minDelta+"     "+maxDelta);
		
		// Print the data.
		System.out.println("Bias:");
		for(int j=0; j<bias.size(); j++) {
			System.out.format("  %3d  range = %6.2f, %6.2f  fit = %11.4e, "+
					"%11.4e\n",j,bias.get(j).minDelta,bias.get(j).maxDelta,
					bias.get(j).slope,bias.get(j).offset);
		}
		System.out.println("Spread:");
		for(int j=0; j<spread.size(); j++) {
			System.out.format("  %3d  range = %6.2f, %6.2f  fit = %11.4e, "+
					"%11.4e\n",j,spread.get(j).minDelta,spread.get(j).maxDelta,
					spread.get(j).slope,spread.get(j).offset);
		}
		System.out.println("Observability:");
		for(int j=0; j<observ.size(); j++) {
			System.out.format("  %3d  range = %6.2f, %6.2f  fit = %11.4e, "+
					"%11.4e\n",j,observ.get(j).minDelta,observ.get(j).maxDelta,
					observ.get(j).slope,observ.get(j).offset);
		}
	}
}