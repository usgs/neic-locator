package gov.usgs.locator;
import gov.usgs.traveltime.TauUtil;
/**
 * Keep all the data for one hypocenter together.
 * 
 * @author Ray Buland
 *
 */
public class Hypocenter {
	// Inputs:
	boolean heldLoc;			// True if the hypocenter will be held constant
	boolean heldDepth;		// True if the depth will be held constant
	boolean prefDepth;		// True if the Bayesian depth was set by an analyst
	boolean cmndRstt;			// True if regional phases will use the RSTT model
	boolean cmndCorr;			// True to use the de-correlation algorithm
	boolean restart;			// True if the hypocenter has been moved externally
	// Input/Outputs:
	double originTime;		// Origin time in seconds since the epoch
	double latitude;			// Geographic latitude in degrees
	double longitude;			// Longitude in degrees
	double depth;					// Depth in kilometers
	double bayesDepth;		// Bayesian depth in kilometers
	double bayesSpread;		// Bayesian uncertainty in kilometers
	// Outputs:
	double bayesImport;		// Data importance of the Bayesian depth
	int staAssoc;					// Number of stations associated
	int staUsed;					// Number of stations used
	int phAssoc;					// Number of phases associated
	int phUsed;						// Number of phases used
	double azimGap;				// Azimuthal gap in degrees
	double lestGap;				// Robust (L-estimator) azimuthal gap in degrees
	double delMin;				// Minimum station distance in degrees
	double seTime;				// Standard error in the origin time in seconds
	double seLat;					// Standard error in latitude in kilometers
	double seLon;					// Standard error in longitude in kilometers
	double seDepth;				// Standard error in depth in kilometers
	double seResid;				// Standard error of the residuals in seconds
	double errh;					// Maximum horizontal projection of the error ellipsoid (km)
	double errz;					// Maximum vertical projection of the error ellipsoid (km)
	double avh;						// Equivalent radius of the error ellipse in kilometers
	String quality;				// Summary event quality flags for the analysts
	EllipAxis[] errEllip;	// Error ellipse
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
		heldLoc = false;
		heldDepth = false;
		prefDepth = false;
		// RSTT and the decorrelation are both off for initial processing, no 
		// matter what the commands say.
		cmndRstt = false;
		cmndCorr = false;
		restart = false;
		errEllip = new EllipAxis[3];
		depthRes = Double.NaN;
		depthWeight = Double.NaN;
		medianRes = 0d;
		stepLen = 0d;
		delH = 0d;
		delZ = 0d;
		ttDepth = Double.NaN;
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
	 * @param stepLen Step length in kilometers.
	 */
	public void updateHypo(double stepLen, double medianRes) {
		double tmpDepth;
		
		// Save the convergence variable.
		this.stepLen = stepLen;
		// Update the origin time.
		originTime += medianRes;
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
		if(!heldDepth) {
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
	 * Update the origin time.
	 * 
	 * @param dT Shift in the origin time in seconds
	 */
	public void updateOrigin(double dT) {
		originTime += dT;
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
	 * Set an analyst requested Bayesian depth.
	 * 
	 * @param bayesDepth Bayesian depth in kilometers
	 * @param bayesSpread Uncertainty of the Bayesian depth in kilometers
	 */
	public void addBayes(double bayesDepth, double bayesSpread) {
		this.bayesDepth = bayesDepth;
		this.bayesSpread = bayesSpread;
		prefDepth = true;
		depth = bayesDepth;
		depthRes = 0d;
		depthWeight = 1d/bayesSpread;
	}
	
	/**
	 * Update the Bayesian depth (if not set by an anaylst).
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
	 * Add analyst command flags.
	 * 
	 * @param heldLoc True if the hypocenter will be held constant
	 * @param heldDepth True if the depth will be held constant
	 * @param cmndRstt True if regional phases will use the RSTT model
	 * @param cmndCorr True to use the decorrelation algorithm
	 * @param restart True if the hypocenter has been moved outside of the 
	 * Locator
	 */
	public void addFlags(boolean heldLoc, boolean heldDepth, boolean cmndRstt, 
			boolean cmndCorr, boolean restart) {
		this.heldLoc = heldLoc;
		this.heldDepth = heldDepth;
		this.cmndRstt = cmndRstt;
		this.cmndCorr = cmndCorr;
		this.restart = restart;
		if(heldLoc) degOfFreedom = 0;
		else if(heldDepth) degOfFreedom = 2;
		else degOfFreedom = 3;
		if(degOfFreedom > 0) stepDir = new double[degOfFreedom];
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
	 * Print out the input hypocenter information in a format similar to 
	 * the Hydra event input file.
	 */
	public void printIn() {
		System.out.format("\nHypo: %22s %8.4f %9.4f %6.2f %5b %5b %5b "+
				"%5.1f %5.1f %5b %5b\n", LocUtil.getRayDate(originTime), latitude, 
				longitude, depth, heldLoc, heldDepth, prefDepth, bayesDepth, 
				bayesSpread, cmndRstt, cmndCorr);
	}
	
	/**
	 * Print the hypocenter part of a Bulletin Hydra style output file.
	 * 
	 * @param noStations Number of stations
	 * @param stationsUsed Number of stations used in by the Locator
	 * @param noPicks Number of picks
	 * @param picksUsed Number of picks used by the Locator
	 */
	public void printHydra(int noStations, int stationsUsed, int noPicks, 
			int picksUsed) {
		System.out.format("\n%14.3f %8.4f %9.4f %6.2f %4d %4d %4d %4d %3.0f "+
				"%8.4f\n", originTime, latitude, longitude, depth, noStations, 
				noPicks, stationsUsed, picksUsed, azimGap, delMin);
		System.out.format("%6.2f %6.1f %6.1f %6.1f %6.2f %6.1f %6.1f %6.1f "+
					"%3s %5.1f %5.1f %6.4f\n", seTime, seLat, seLon, seDepth, seResid, 
					errh, errz, avh, quality, bayesDepth, bayesSpread, bayesImport);
		System.out.format("%14s %14s %14s  %3.0f\n", errEllip[0], errEllip[1], 
				errEllip[2], lestGap);
	}
	
	/**
	 * Print the hypocenter in a way similar to the NEIC web format.
	 * 
	 * @param noStations Number of stations associated
	 * @param noPicks Number of picks associated
	 */
	public void printNEIC(int noStations, int noPicks) {
		System.out.format("\nLocation:             %-7s %-8s ±%6.1f km\n", 
				LocUtil.niceLat(latitude), LocUtil.niceLon(longitude), errh);
		System.out.format("Depth:                %5.1f ±%6.1f km\n", depth, 
				errz);
		System.out.format("Origin Time:          %23s UTC\n", 
				LocUtil.getNEICdate(originTime));
		System.out.format("Number of Stations:     %4d\n", noStations);
		System.out.format("Number of Phases:       %4d\n", noPicks);
		System.out.format("Minimum Distance:     %6.1f\n", delMin);
		System.out.format("Travel Time Residual:  %5.2f\n", seTime);
		System.out.format("Azimuthal Gap:           %3.0f\n", azimGap);
	}
	
	@Override
	public String toString() {
		return String.format("%14.3f %8.4f %9.4f %6.2f", originTime, latitude, 
				longitude, depth);
	}
}
