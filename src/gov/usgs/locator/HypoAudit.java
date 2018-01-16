package gov.usgs.locator;

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
	
	public void printAudit() {
		System.out.format("Audit: %1d %2d %4d %22s %8.4f %9.4f %6.2f "+
				"del = %6.1f %6.1f %6.1f rms = %6.2f\n", stage, iteration, picksUsed, 
				LocUtil.getRayDate(originTime), latitude, longitude, depth, 
				stepLen, delH, delZ, rms);
	}
}
