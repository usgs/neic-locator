package gov.usgs.locator;

public class HypoAudit {
	int stage;					// Iteration stage
	int iteration;			// Iteration in this stage
	int noPicks;				// Number of picks used
	double originTime;	// Origin time in seconds
	double latitude;		// Geographic latitude in degrees
	double longitude;		// Geographic longitude in degrees
	double depth;				// Depth in kilometers
	double delH;				// Epicentral change since last stage in kilometers
	double delZ;				// Depth change since last stage in kilometers
	double seTime;			// Standard error of the origin time in seconds
	
	public HypoAudit(Hypocenter hypo, int stage, int iteration, int noPicks, 
			double delH, double delZ) {
		this.stage = stage;
		this.iteration = iteration;
		this.noPicks = noPicks;
		originTime = hypo.originTime;
		latitude = hypo.latitude;
		longitude = hypo.longitude;
		depth = hypo.depth;
		this.delH = delH;
		this.delZ = delZ;
		seTime = hypo.seTime;
	}
	
	public void printAudit() {
		System.out.format("Audit: %1d %2d %4d %22s %8.4f %9.4f %6.2f "+
				"del = %5.1f %6.1f rms = %6.2f\n", stage, iteration, noPicks, 
				LocUtil.getRayDate(originTime), latitude, longitude, depth, 
				delH, delZ, seTime);
	}
}
