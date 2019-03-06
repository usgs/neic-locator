package gov.usgs.locator;

import java.util.ArrayList;

import gov.usgs.processingformats.*;

/**
 * Locator outputs from an event relocation.  This class is designed to contain 
 * all parameters resulting from a Locator pass.  An object of this class should 
 * be handed to the output routines to be returned to the caller.
 * 
 * @author Ray Buland
 *
 */
public class LocOutput extends LocationData {
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
	 * @param authType Type (e.g., human or auto) of the original phase 
	 * identification
	 * @param pickID Hydra database pick ID (optional)
	 * @param stationCode Station code.
	 * @param componentCode Component code.
	 * @param networkCode Network code.
	 * @param locationCode Location code.
	 * @param stationLatitude station latitude
	 * @param stationLongitude station longitude
	 * @param stationElevation station elevation 
	 * @param pickTime Pick time in milliseconds.
	 * @param locatorPhase Final seismic phase code.
	 * @param residual Pick residual in seconds.
	 * @param delta Source-receiver distance in degrees.
	 * @param azimuth Receiver azimuth (clockwise from north) in degrees.
	 * @param weight Pick weight.
	 * @param pickImport Pick data importance.
	 * @param useFlag True if the pick was used in the location.
	 * @param pickAffinity The higher the affinity, the harder it is to re-identify 
	 * a pick.  By default, the affinity for the four author types would be 
	 * 1.0, 1.0, 1.5, and 3.0 respectively.
	 * @param pickQuality The pick standard deviation in seconds.
	 * @param errorCode Summary pick error code.
	 */
	public void addPick(String source, AuthorType authType, String pickID, 
			String stationCode, String componentCode, String networkCode, 
			String locationCode, double stationLatitude, double stationLongitude, 
			double stationElevation, long pickTime, String locatorPhase, 
			String originalPhase, double residual, double delta, double azimuth, 
			double weight, double pickImport, boolean useFlag, 
			double pickAffinity, double pickQuality, String errorCode) {
		if(picks == null) picks = new ArrayList<PickOutput>();

		picks.add(new PickOutput(source, authType, pickID, stationCode, 
			componentCode, networkCode, locationCode, stationLatitude, 
			stationLongitude, stationElevation, pickTime, locatorPhase, 
			originalPhase, residual, delta, azimuth, weight, pickImport, 
			useFlag, pickAffinity, pickQuality, errorCode));
	}
	
	/**
	 * Print an NEIC style web output.
	 */
	public void printNEIC() {
		// Print the hypocenter.
		System.out.format("\nLocation:             %-7s %-8s ±%6.1f km\n", 
				LocUtil.niceLat(sourceLat), LocUtil.niceLon(sourceLon), 
				errh);
		System.out.format("Depth:                %5.1f ±%6.1f km\n", 
				sourceDepth, errz);
		System.out.format("Origin Time:          %23s UTC\n", 
				LocUtil.getNEICdate(originTime));
		System.out.format("Number of Stations:     %4d\n", noStations);
		System.out.format("Number of Phases:       %4d\n", noPicks);
		System.out.format("Minimum Distance:     %6.1f\n", minDelta);
		System.out.format("Travel Time Residual:  %5.2f\n", timeError);
		System.out.format("Azimuthal Gap:           %3.0f\n", azimuthGap);
		System.out.println("\n    Channel     Distance Azimuth Phase  "+
				"   Arrival Time Status    Residual Weight");
		// Print the picks.
		for(int j=0; j<picks.size(); j++) {
			picks.get(j).printNEIC();
		}
	}

	/**
	 * Print out picks in the group in a way similar to the NEIC web format.
	 */
/*	public void printNEICPicks() {
		switch(authorType) {
			case CONTRIB_HUMAN: case LOCAL_HUMAN:
				System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s "+
						" manual    %6.1f    %4.2f\n", networkCode, stationCode, 
						componentCode, locationCode, delta, azimuth, locatorPhase, 
						LocUtil.getNEICtime(pickTime), residual, weight);
				break;
			default:
				System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s "+
						" automatic  %6.1f    %4.2f\n", networkCode, stationCode, 
						componentCode, locationCode, delta, azimuth, locatorPhase, 
						LocUtil.getNEICtime(pickTime), residual, weight);
				break;
		}
	}*/

}
