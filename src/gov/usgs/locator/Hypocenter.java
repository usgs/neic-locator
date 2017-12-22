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
	double coLat;					// Geocentric colatitude in degrees
	double sinLat;				// Sine of the geocentric colatitude
	double cosLat;				// Cosine of the geocentric colatitude
	double sinLon;				// Sine of the longitude
	double cosLon;				// Cosine of the longitude
	double depthRes;			// Bayesian depth residual in kilometers
	double depthWeight;		// Bayesian depth weight
	// Getters:
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
		updateHypo(originTime, latitude, longitude, depth);
		// Set defaults for the rest.
		bayesDepth = Double.NaN;
		bayesSpread = Double.NaN;
		heldLoc = false;
		heldDepth = false;
		prefDepth = false;
		cmndRstt = false;
		cmndCorr = true;
		restart = false;
		// RSTT and the decorrelation are both off for initial processing, no 
		// matter what the commands say.
		errEllip = new EllipAxis[3];
		depthRes = Double.NaN;
		depthWeight = Double.NaN;
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
		sinLat = Math.sin(Math.toRadians(coLat));
		cosLat = Math.cos(Math.toRadians(coLat));
		sinLon = Math.sin(Math.toRadians(longitude));
		cosLon = Math.cos(Math.toRadians(longitude));
		// Update the Bayesian depth residual.
		if(!Double.isNaN(bayesDepth)) depthRes = bayesDepth-depth;
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
		depthWeight = 1d/bayesSpread;
	}
	
	/**
	 * Add analyst command flags.
	 * 
	 * @param heldLoc True if the hypocenter will be held constant
	 * @param heldDepth True if the depth will be held constant
	 * @param rstt True if regional phases will use the RSTT model
	 * @param noSvd True to not use the de-correlation algorithm
	 */
	public void addFlags(boolean heldLoc, boolean heldDepth, boolean cmndRstt, 
			boolean cmndCorr, boolean restart) {
		this.heldLoc = heldLoc;
		this.heldDepth = heldDepth;
		this.cmndRstt = cmndRstt;
		this.cmndCorr = cmndCorr;
		this.restart = restart;
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
