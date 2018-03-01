package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Locator outputs from an event relocation.  This class is designed to contain 
 * all parameters resulting from a Locator pass.  An object of this class should 
 * be handed to the output routines to be returned to the caller.
 * 
 * @author Ray Buland
 *
 */
public class LocOutput {
	long originTime;				// Source origin time
	double sourceLat;				// Geographic source latitude in degrees
	double sourceLon;				// Geographic source longitude in degrees
	double sourceDepth;			// Source depth relative to the WGS84 datum in kilometers
	int noStations;					// Number of stations associated
	int noPicks;						// Number of phases associated
	int stationsUsed;				// Number of stations used in the location
	int picksUsed;					// Number of phases used in the location
	double azimuthGap;			// Azimuthal gap in degrees
	double azimuthGap2;			// The robust azimuthal gap in degrees
	double minDelta;				// Distance to the closest station in degrees
	String qualityFlags;		// Location quality flags
	double timeError;				// Origin time marginal confidence interval in seconds
	double latError;				// Latitude marginal confidence interval in kilometers
	double lonError;				// Longitude marginal confidence interval in kilometers
	double depthError;			// Depth marginal confidence interval in kilometers
	double stdError;				// Standard error of the residuals in seconds
	double errh;						// Maximum projection of the horizontal error in kilometers
	double errz;						// Maximum projection of the depth error in kilometers
	double avh;							// Equivalent radius of the error ellipse
	double bayesDepth;			// Final Bayesian depth in kilometers
	double bayesSpread;			// Final Bayesian spread in kilometers
	double depthImport;			// Depth data importance
	EllipAxis[] ellipsoid;	// 3-D error ellipsoid in kilometers and degrees
	int exitCode;						// Exit code
	ArrayList<PickOutput> picks;
	
	/**
	 * The following hypocenter parameters are produced by an event relocation.
	 * 
	 * @param originTime Source origin time in milliseconds.
	 * @param sourceLat Geographic source latitude in degrees.
	 * @param sourceLon Geographic source longitude in degrees.
	 * @param sourceDepth Source depth in kilometers.
	 * @param noStations Number of stations associated.
	 * @param noPicks Number of picks associated.
	 * @param stationsUsed Number of stations used.
	 * @param picksUsed Number of picks used.
	 * @param azimuthGap Standard azimuthal gap in degrees.
	 * @param azimuthGap2 Robust azimuthal gap in degrees.
	 * @param minDelta Minimum source-receiver distance in degrees.
	 * @param qualityFlags Location quality flags.
	 */
	public LocOutput(long originTime, double sourceLat, double sourceLon, 
			double sourceDepth, int noStations, int noPicks, 
			int stationsUsed, int picksUsed, double azimuthGap, 
			double azimuthGap2, double minDelta, String qualityFlags) {
		this.originTime = originTime;
		this.sourceLat = sourceLat;
		this.sourceLon = sourceLon;
		this.sourceDepth = sourceDepth;
		this.noStations = noStations;
		this.noPicks = noPicks;
		this.stationsUsed = stationsUsed;
		this.picksUsed = picksUsed;
		this.azimuthGap = azimuthGap;
		this.azimuthGap2 = azimuthGap2;
		this.minDelta = minDelta;
		this.qualityFlags = qualityFlags;
	}
	
	/**
	 * The following error parameters are produced by an event relocation.
	 * 
	 * @param timeError Origin time error in seconds.
	 * @param latError Latitude 90% confidence interval in kilometers.
	 * @param lonError Longitude 90% confidence interval in kilometers.
	 * @param depthError Depth 90% confidence interval in kilometers.
	 * @param stdError Median travel-time residual in seconds.
	 * @param errh Summary horizontal (tangential) error in kilometers.
	 * @param errz Summary vertical (radial) error in kilometers.
	 * @param avh Equivalent radius of the error ellipse in kilometers.
	 * @param bayesDepth Final Bayesian depth in kilometers.
	 * @param bayesSpread Final Bayesian standard deviation in kilometers.
	 * @param depthImport Data important for the depth.
	 * @param ellipsoid 90% error ellipsoid in kilometers.
	 * @param exitCode Final location exit code
	 */
	public void addErrors(double timeError, double latError, double lonError, 
			double depthError, double stdError, double errh, double errz, 
			double avh, double bayesDepth, double bayesSpread, double depthImport, 
			EllipAxis[] ellipsoid, int exitCode) {
		this.timeError = timeError;
		this.latError = latError;
		this.lonError = lonError;
		this.depthError = depthError;
		this.stdError = stdError;
		this.errh = errh;
		this.errz = errz;
		this.avh = avh;
		this.bayesDepth = bayesDepth;
		this.bayesSpread = bayesSpread;
		this.depthImport = depthImport;
		this.ellipsoid = ellipsoid;
		this.exitCode = exitCode;
	}
	
	/**
	 * The following parameters are produced for each pick.
	 * 
	 * @param source Source of the database pick ID (optional).
	 * @param pickID Hydra database pick ID (optional)
	 * @param stationCode Station code.
	 * @param componentCode Component code.
	 * @param networkCode Network code.
	 * @param locationCode Location code.
	 * @param locatorPhase Final seismic phase code.
	 * @param residual Pick residual in seconds.
	 * @param delta Source-receiver distance in degrees.
	 * @param azimuth Receiver azimuth (clockwise from north).
	 * @param weight Pick weight.
	 * @param pickImport Pick data importance.
	 * @param useFlag True if the pick was used in the location.
	 * @param errorCode Summary pick error code.
	 */
	public void addPick(String source, int pickID, String stationCode, 
			String componentCode, String networkCode, String locationCode, 
			String locatorPhase, double residual, double delta, double azimuth, 
			double weight, double pickImport, boolean useFlag, String errorCode) {
		if(picks == null) picks = new ArrayList<PickOutput>();
		picks.add(new PickOutput(source, pickID, stationCode, componentCode, 
			networkCode, locationCode, locatorPhase, residual, delta, azimuth, 
			weight, pickImport, useFlag, errorCode));
	}
}
