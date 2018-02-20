package gov.usgs.locator;

/**
 * Audit instances are intended to provide a snapshot of the event at different 
 * points in the location process for logging purposes.  They also provide a 
 * handy way of backing down to a previous hypocenter should the location 
 * iteration go awry.
 * 
 * @author Ray Buland
 *
 */
public class HypoAudit {
	int stage;					// Iteration stage
	int iteration;			// Iteration in this stage
	int picksUsed;			// Number of picks used
	double originTime;	// Origin time in seconds
	double latitude;		// Geographic latitude in degrees
	double longitude;		// Geographic longitude in degrees
	double depth;				// Depth in kilometers
	double stepLen;			// Hypocentral change in kilometers
	double delH;				// Epicentral change in kilometers
	double delZ;				// Depth change in kilometers
	double rms;					// Standard error of the origin time in seconds
	LocStatus status;		// Current hypocenter status
	// Internal use:
	double coLat;				// Geocentric colatitude in degrees
	double sinLat;			// Sine of the geocentric colatitude
	double cosLat;			// Cosine of the geocentric colatitude
	double sinLon;			// Sine of the longitude
	double cosLon;			// Cosine of the longitude
	
	/**
	 * Create an audit record.
	 * 
	 * @param hypo Hypocentral information
	 * @param stage Current location stage
	 * @param iteration Current location iteration
	 * @param picksUsed Number of picks currently being used
	 * @param status Current location status
	 */
	public HypoAudit(Hypocenter hypo, int stage, int iteration, int picksUsed, 
			LocStatus status) {
		this.stage = stage;
		this.iteration = iteration;
		this.picksUsed = picksUsed;
		originTime = hypo.originTime;
		latitude = hypo.latitude;
		longitude = hypo.longitude;
		depth = hypo.depth;
		stepLen = hypo.stepLen;
		delH = hypo.delH;
		delZ = hypo.delZ;
		rms = hypo.rms;
		this.status = status;
		coLat = hypo.coLat;
		sinLat = hypo.sinLat;
		cosLat = hypo.cosLat;
		sinLon = hypo.sinLon;
		cosLon = hypo.cosLon;
	}
	
	/**
	 * Print the audit record.
	 */
	public void printAudit() {
		System.out.format("Audit: %1d %2d %4d %22s %8.4f %9.4f %6.2f "+
				"del = %6.1f %6.1f %6.1f rms = %6.2f %s\n", stage, iteration, picksUsed, 
				LocUtil.getRayDate(originTime), latitude, longitude, depth, 
				delH, delZ, stepLen, rms, status);
	}
}
