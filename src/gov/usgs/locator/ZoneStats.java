package gov.usgs.locator;

/**
 * Implements the historical free depth statistics portion of the 
 * SEDAS zone statistics subsystem.  Note that the zone keys was a 
 * crude SEDAS era accelerator for the statistics.  This had two 
 * advantages.  First, being Marsden square based, it was fast.  
 * Second the statistics themselves were compact as most Marsden 
 * squares don't have enough earthquakes to do statistics on.  To 
 * save precious memory, both the keys and statistics were accessed 
 * from random access files.
 * 
 * @author Ray Buland
 *
 */
public class ZoneStats {
	int size = 0, index = 0;
	int noYears;			// Number of years used to compute the statistics
	int[][] zoneKey;	// Index into the statistics based on Marsden squares
	ZoneStat[] stats;	// Statistics for one Marsden square
	double bayesSpread = Double.NaN;
	
	/**
	 * Set up the zone keys and allocate the zone statistics.
	 * 
	 * @param zoneKey Marsden square based pointer to the statistics
	 */
	public ZoneStats(int[][] zoneKey) {
		this.zoneKey = zoneKey;
		// Find the biggest key.
		for(int j=0; j<zoneKey.length; j++) {
			for(int i=0; i<zoneKey[j].length; i++) {
				size = Math.max(size, zoneKey[j][i]);
			}
		}
	}
	
	/**
	 * Get the size of the zone statistics array.
	 * 
	 * @return Size of the zone statistics array.
	 */
	public int size() {
		return size;
	}
	
	/**
	 * Add statistics for a zone.
	 * 
	 * @param noYears Number of years in the statistics
	 * @param stats Zone depth statistics
	 */
	public void addStats(int noYears, ZoneStat[] stats) {
		this.noYears = noYears;
		this.stats = stats;
	}
	
	/**
	 * Get the depth statistics for an epicenter.
	 * 
	 * @param latitude Geographic latitude in degrees
	 * @param longitude Geographic longitude in degrees
	 * @return Zone statistics
	 */
	public ZoneStat getStats(double latitude, double longitude) {
		int nlat, nlon, key;
		
		// Set up the zone key indices.  First we need colatitude and 
		// longitude in the range of 0-360.
		latitude = 90d-latitude;
		if(longitude == 0d) longitude = 360d;
		// Bail on bad coordinates.
		if(latitude > 180d || latitude < 0d || longitude > 360d || 
				longitude < 0d) return null;
		// Now get the indices.
		if(latitude < 180d) nlat = (int)latitude;
		else nlat = 179;
		if(latitude > 0d && latitude < 180d) nlon = (int)longitude;
		else nlon = 0;
		
		// Get the statistics.
		key = zoneKey[nlon][nlat];
		if(key >= 0) return stats[key];
		else return null;
	}
	
	/**
	 * Get the best Bayesian depth from the zone statistics.
	 * 
	 * @param latitude Geographic epicenter latitude in degrees
	 * @param longitude Geographic epicenter longitude in degrees
	 * @return Bayesian depth in kilometers
	 */
	public double bayesDepth(double latitude, double longitude) {
		double meanDepth, minDepth, maxDepth;
		ZoneStat stat;
		
		// Get the raw statistics.
		stat = getStats(latitude, longitude);
		if(stat != null) {
			// Trap bad depths.
			meanDepth = Math.min(Math.max(stat.meanDepth, LocUtil.DEPTHMIN), 
					LocUtil.DEPTHMAX);
			minDepth = Math.min(Math.max(stat.minDepth, LocUtil.DEPTHMIN), 
					LocUtil.DEPTHMAX);
			maxDepth = Math.min(Math.max(stat.maxDepth, LocUtil.DEPTHMIN), 
					LocUtil.DEPTHMAX);
			// Trap other ugly errors.
			if(minDepth >= maxDepth || meanDepth <= minDepth || 
					meanDepth >= maxDepth) {
				if(meanDepth < 400d) {
					minDepth = Math.max(meanDepth-0.5*LocUtil.DEFDEPSE, 
							LocUtil.DEPTHMIN);
					maxDepth = minDepth+LocUtil.DEFDEPSE;
				} else {
					maxDepth = Math.min(meanDepth+0.5*LocUtil.DEFDEPSE, 
							LocUtil.DEPTHMAX);
					minDepth = maxDepth-LocUtil.DEFDEPSE;
				}
			}
			// Compute the Bayesian depth and spread.
			bayesSpread = Math.max(Math.max(maxDepth-meanDepth, 
					meanDepth-minDepth), LocUtil.DEFDEPSE);
			return meanDepth;
		} else {
			bayesSpread = LocUtil.DEFDEPSE;
			return LocUtil.DEFDEPSE;
		}
	}
	
	/**
	 * Get the best Bayesian spread from the zone statistics.  Note that this 
	 * value is computed by the latest call to bayesDepth.
	 * 
	 * @return Bayesian spread in kilometers
	 */
	public double bayesSpread() {
		return bayesSpread;
	}
}
