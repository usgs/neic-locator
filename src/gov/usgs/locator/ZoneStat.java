package gov.usgs.locator;

import java.io.Serializable;

/**
 * Keep depth statistics for one Marsden square.
 * 
 * @author Ray Buland
 *
 */
public class ZoneStat implements Serializable {
	private static final long serialVersionUID = 1L;
	double meanDepth;	// Average historical free depth in kilometers
	double minDepth;	// Shallowest historical free depth in kilometers
	double maxDepth;	// Deepest historical free depth in kilometers
	
	/**
	 * Set the zone statistics for this Marsden square.
	 * 
	 * @param meanDepth Historical mean depth in kilometers
	 * @param minDepth Historical minimum depth in kilometers
	 * @param maxDepth Historical maximum depth in kilometers
	 */
	public ZoneStat(double meanDepth, double minDepth, double maxDepth) {
		this.meanDepth = meanDepth;
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
	}
	
}
