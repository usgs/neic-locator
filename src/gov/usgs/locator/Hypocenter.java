package gov.usgs.locator;

import gov.usgs.traveltime.TauUtil;

/**
 * Keep all the data for one hypocenter together.
 * 
 * @author Ray Buland
 *
 */
public class Hypocenter {
	// Input/Outputs:
	double originTime;		// Origin time in seconds since the epoch
	double latitude;			// Geographic latitude in degrees
	double longitude;			// Longitude in degrees
	double depth;					// Depth in kilometers
	double bayesDepth;		// Bayesian depth in kilometers
	double bayesSpread;		// Bayesian uncertainty in kilometers
	// Internal use:
	int degOfFreedom;			// Degrees of freedom
	double coLat;					// Geocentric colatitude in degrees
	double sinLat;				// Sine of the geocentric colatitude
	double cosLat;				// Cosine of the geocentric colatitude
	double sinLon;				// Sine of the longitude
	double cosLon;				// Cosine of the longitude
	double depthRes;			// Bayesian depth residual in kilometers
	double depthWeight;		// Bayesian depth weight
	// Iteration parameters:
	int noDamp;						// Number of times step length damping has been applied
	double medianRes;			// Linear estimate of the origin time shift in seconds
	double chiSq;					// R-estimator dispersion or penalty value
	double rms;						// R-estimator equivalent of the least squares RMS
	double stepLen;				// Step length in kilometers
	double delH;					// Horizontal (tangential) step length in kilometers
	double delZ;					// Vertical (depth) step length in kilometers
	double[] stepDir;			// Spatial local Cartesian step direction unit vector
	// Travel-time control:
	double ttDepth;
	// Getters:
	public double getOrigin() { return originTime;}
	public double getLatitude() {return latitude;}
  public double getLongitude() {return longitude;}
  public double getDepth() {return depth;}
  
	/**
	 * Initializes a hypocenter with enough information to start a location pass.
	 * 
	 * @param originTime Origin time in seconds
	 * @param latitude Geographic latitude in degrees
	 * @param longitude Longitude in degrees
	 * @param depth Depth in kilometers
	 */
	public Hypocenter(double originTime, double latitude, double longitude, 
			double depth) {
		// Set up the hypocenter.
		depth = Math.min(Math.max(depth, LocUtil.DEPTHMIN), 
				LocUtil.DEPTHMAX);
		updateHypo(originTime, latitude, longitude, depth);
		// Set defaults for the rest.
		bayesDepth = Double.NaN;
		bayesSpread = Double.NaN;
		depthRes = Double.NaN;
		depthWeight = Double.NaN;
		medianRes = 0d;
		stepLen = 0d;
		delH = 0d;
		delZ = 0d;
		ttDepth = Double.NaN;
	}
	
	/**
	 * Set an analyst requested Bayesian depth.
	 * 
	 * @param bayesDepth Bayesian depth in kilometers
	 * @param bayesSpread Uncertainty of the Bayesian depth in kilometers
	 */
	public void addBayes(double bayesDepth, double bayesSpread) {
		this.bayesDepth = Math.min(Math.max(bayesDepth, LocUtil.DEPTHMIN), 
				LocUtil.DEPTHMAX);
		this.bayesSpread = bayesSpread;
		depth = bayesDepth;
		depthRes = 0d;
		depthWeight = 1d/bayesSpread;
	}
	
	/**
	 * Set the number of event degrees of freedom to be determined.
	 * 
	 * @param heldLoc True if the hypocenter will be held constant
	 * @param heldDepth True if the depth will be held constant
	 */
	public void setDegrees(boolean heldLoc, boolean heldDepth) {
		if(heldLoc) degOfFreedom = 0;
		else if(heldDepth) degOfFreedom = 2;
		else degOfFreedom = 3;
		if(degOfFreedom > 0) stepDir = new double[degOfFreedom];
	}
	
	/**
	 * Update the hypocentral parameters and recompute the sines and cosines.
	 * 
	 * @param originTime Event origin time in seconds since the epoch
	 * @param latitude Geographic latitude in degrees
	 * @param longitude Longitude in degrees
	 * @param depth Depth in kilometers
	 */
	public void updateHypo(double originTime, double latitude, double longitude, 
			double depth) {
		// Update the hypocenter.
		this.originTime = originTime;
		this.latitude = latitude;
		this.longitude = longitude;
		this.depth = depth;
		// Update the sines and cosines.
		coLat = TauUtil.geoCen(latitude);
		updateSines();
		// Update the Bayesian depth residual.
		if(!Double.isNaN(bayesDepth)) depthRes = bayesDepth-depth;
	}
	
	/**
	 * Move the hypocenter based on the linearized optimal step.
	 * 
	 * @param stepLen Step length in kilometers
	 * @param dT Origin time shift in seconds
	 */
	public void updateHypo(double stepLen, double dT) {
		double tmpDepth;
		
		// Save the convergence variable.
		this.stepLen = stepLen;
		// Update the origin time.
		originTime += dT;
		// Compute the tangential step length for tracking purposes.
		delH = Math.sqrt(Math.pow(stepLen*stepDir[0], 2d)+
				Math.pow(stepLen*stepDir[1], 2d));
		// Update the colatitude and longitude.
		coLat += stepLen*stepDir[0]/LocUtil.DEG2KM;
		longitude += stepLen*stepDir[1]/(LocUtil.DEG2KM*sinLat);
		// Make sure the colatitude is legal.
		if(coLat < 0d) {
			coLat = Math.abs(coLat);
			longitude += 180d;
		} else if(coLat > 180d) {
			coLat = 360d-coLat;
			longitude += 180d;
		}
		// Make sure the longitude is legal.
		if(longitude < -180d) {
			longitude += 360d;
		} else if(longitude > 180d) {
			longitude -= 360d;
		}
		// Deal with depth separately.
		if(degOfFreedom > 0) {
			tmpDepth = Math.min(Math.max(depth+stepLen*stepDir[2], 
					LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);
			delZ = tmpDepth-depth;
			depth = tmpDepth;
		}
		// Compute the geographic latitude.
		latitude = TauUtil.geoLat(coLat);
		// Update the sines and cosines.
		updateSines();
		// Update the Bayesian depth residual.
		if(!Double.isNaN(bayesDepth)) depthRes = bayesDepth-depth;
	}
	
	/**
	 * Compute the sines and cosines of colatitude and longitude.
	 */
	private void updateSines() {
		sinLat = Math.sin(Math.toRadians(coLat));
		cosLat = Math.cos(Math.toRadians(coLat));
		sinLon = Math.sin(Math.toRadians(longitude));
		cosLon = Math.cos(Math.toRadians(longitude));
	}
	
	/**
	 * Update the origin time.
	 * 
	 * @param dT Shift in the origin time in seconds
	 */
	public void updateOrigin(double dT) {
		originTime += dT;
	}
	
	/**
	 * Update the Bayesian depth (if not set by an analyst).
	 * 
	 * @param bayesDepth Bayesian depth in kilometers
	 * @param bayesSpread Uncertainty of the Bayesian depth in kilometers
	 */
	public void updateBayes(double bayesDepth, double bayesSpread) {
		this.bayesDepth = bayesDepth;
		this.bayesSpread = bayesSpread;
		depthRes = bayesDepth-depth;
		// The Bayesian spread is actually taken as a 90th percentile.
		depthWeight = 3d/bayesSpread;
	}
	
	/**
	 * Reset key hypocentral parameters to a backup (used for step length 
	 * damping).
	 * 
	 * @param backup Hypocenter audit record
	 */
	public void resetHypo(HypoAudit backup) {
		originTime = backup.originTime;
		latitude = backup.latitude;
		longitude = backup.longitude;
		depth = backup.depth;
	}
	
	/**
	 * A convenient way of dumping the current hypocenter.
	 */
	@Override
	public String toString() {
		return String.format("%14.3f %8.4f %9.4f %6.2f", originTime, latitude, 
				longitude, depth);
	}
}
