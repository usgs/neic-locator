package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Internal class to store the Locator outputs for a single pick.
 * 
 * @author Ray
 *
 */
class PickOutput {
	String source;				// Source of the pick ID
	int pickID;					// Hydra database ID
	String stationCode;			// Station code.
	String componentCode;		// Component code.
	String networkCode;			// Network code.
	String locationCode;		// Location code.
	String locatorPhase;		// Phase code identified by the Locator.
	double residual;			// Pick residual in seconds.
	double delta;				// Source-receiver distance in degrees.
	double azimuth;				// Receiver azimuth in degrees.
	double weight;				// Final weight applied to the pick.
	double pickImport;			// Pick data importance.
	String useFlag;				// Pick use flag.
	String errorCode;			// Pick summary error code (RSTT)
	
	/**
	 * The following Locator outputs are produced for each pick.
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
	 * @param azimuth Receiver azimuth (clockwise from north) in degrees.
	 * @param weight Pick weight.
	 * @param pickImport Pick data importance.
	 * @param useFlag True if the pick was used in the location.
	 * @param errorCode Summary pick error code.
	 */
	PickOutput(String source, int pickID, String stationCode, 
			String componentCode, String networkCode, String locationCode, 
			String locatorPhase, double residual, double delta, double azimuth, 
			double weight, double pickImport, String useFlag, String errorCode) {
		this.source = source;
		this.pickID = pickID;
		this.stationCode = stationCode;
		this.componentCode = componentCode;
		this.networkCode = networkCode;
		this.locationCode = locationCode;
		this.locatorPhase = locatorPhase;
		this.residual = residual;
		this.delta = delta;
		this.azimuth = azimuth;
		this.weight = weight;
		this.pickImport = pickImport;
		this.useFlag = useFlag;
		this.errorCode = errorCode;
	}
}

/**
 * Internal class containing the location quality flags.
 * 
 * @author Ray
 *
 */
class QualityFlags {
	char summaryQual;
	char epicenterQual;
	char depthQual;
	
	/**
	 * The following Locator flags summarize the location quality.
	 * 
	 * @param summaryQual The overall event quality flag ("G" = GT5, "A" = good, 
	 * "B" = fair, "C" = poor, "D" = bad).
	 * @param epicenterQual The epicenter quality flag ("&" = held, " " = good, 
	 * "*" = fair, "?" = poor, "!" = bad).
	 * @param depthQual The depth quality flag ("&" = held, " " = good, 
	 * "*" = fair, "?" = poor, "!" = bad).
	 */
	QualityFlags(char summaryQual, char epicenterQual, char depthQual) {
		this.summaryQual = summaryQual;
		this.epicenterQual = epicenterQual;
		this.depthQual = depthQual;
	}
}

/**
 * Internal class containing a single error ellipsoid axis.
 * 
 * @author Ray
 *
 */
class EllipsoidAxis {
	double azimuth;				// Axis azimuth (clockwise from north) in degrees.
	double dip;					// Axis dip (down from the horizontal) in degrees.
	double length;				// Semi-axis length in kilometers.
	
	/**
	 * The following parameters define each error ellipsoid axis.
	 * 
	 * @param azimuth Axis azimuth (clockwise from north) in degrees.
	 * @param dip Axis dip (down from horizontal) in degrees.
	 * @param length Semi-axis length in kilometers.
	 */
	EllipsoidAxis(double azimuth, double dip, double length) {
		this.azimuth = azimuth;
		this.dip = dip;
		this.length = length;
	}
}

/**
 * Locator outputs from an event relocation.  This class is designed to contain 
 * all parameters resulting from a Locator pass.  An object of this class should 
 * be handed to the output routines to be returned to the caller.
 * 
 * @author Ray
 *
 */
public class LocOutput {
	long originTime;			// Source origin time.
	double sourceLat;			// Geographic source latitude in degrees.
	double sourceLon;			// Geographic source longitude in degrees.
	double sourceDepth;			// Source depth relative to the WGS84 datum in kilometers.
	int stations;				// Number of stations associated.
	int picks;					// Number of phases associated.
	int stationsUsed;			// Number of stations used in the location.
	int picksUsed;				// Number of phases used in the location.
	double azimuthGap;			// Azimuthal gap in degrees.
	double azimuthGap2;			// The robust azimuthal gap in degrees.
	double minDelta;			// Distance to the closest station in degrees.
	QualityFlags qualityFlags;	// Location quality flags.
	double timeError;			// Origin time marginal confidence interval in seconds.
	double latError;			// Latitude marginal confidence interval in kilometers.
	double lonError;			// Longitude marginal confidence interval in kilometers.
	double depthError;			// Depth marginal confidence interval in kilometers.
	double stdError;			// Standard error of the residuals in seconds.
	double errh;				// Maximum projection of the horizontal error in kilometers.
	double errz;				// Maximum projection of the depth error in kilometers.
	double avh;					// Equivalent radius of the error ellipse.
	double bayesDepth;			// Final Bayesian depth in kilometers.
	double bayesSpread;			// Final Bayesian spread in kilometers.
	double depthImport;			// Depth data importance.
	EllipsoidAxis[] ellipsoid;	// 3-D error ellipsoid in kilometers.
	ArrayList<PickOutput> pick;
	
	/**
	 * The following hypocenter parameters are produced by an event relocation.
	 * 
	 * @param originTime Source origin time in milliseconds.
	 * @param sourceLat Geographic source latitude in degrees.
	 * @param sourceLon Geographic source longitude in degrees.
	 * @param sourceDepth Source depth in kilometers.
	 * @param stations Number of stations associated.
	 * @param picks Number of picks associated.
	 * @param stationsUsed Number of stations used.
	 * @param picksUsed Number of picks used.
	 * @param azimuthGap Standard azimuthal gap in degrees.
	 * @param azimuthGap2 Robust azimuthal gap in degrees.
	 * @param minDelta Minimum source-receiver distance in degrees.
	 * @param qualityFlags Location quality flags.
	 */
	public void addLoc(long originTime, double sourceLat, double sourceLon, 
			double sourceDepth, int stations, int picks, 
			int stationsUsed, int picksUsed, double azimuthGap, 
			double azimuthGap2, double minDelta, QualityFlags qualityFlags) {
		this.originTime = originTime;
		this.sourceLat = sourceLat;
		this.sourceLon = sourceLon;
		this.sourceDepth = sourceDepth;
		this.stations = stations;
		this.picks = picks;
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
	 */
	public void addErrors(double timeError, double latError, double lonError, 
			double depthError, double stdError, double errh, double errz, 
			double avh, double bayesDepth, double bayesSpread, double depthImport, 
			EllipsoidAxis[] ellipsoid) {
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
		this.ellipsoid = new EllipsoidAxis[3];
		this.ellipsoid = ellipsoid;
		
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
			double weight, double pickImport, String useFlag, String errorCode) {
		if(pick == null) pick = new ArrayList<PickOutput>();
		pick.add(new PickOutput(source, pickID, stationCode, componentCode, 
			networkCode, locationCode, locatorPhase, residual, delta, azimuth, 
			weight, pickImport, useFlag, errorCode));
	}
}
