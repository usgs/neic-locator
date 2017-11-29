package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Internal class to store the Locator inputs needed for a single pick.
 * 
 * @author Ray
 *
 */
class PickInput {
	String source;				// Source of the pick ID
	int pickID;					// Hydra database ID
	String stationCode;			// Station code.
	String componentCode;		// Component code.
	String networkCode;			// Network code.
	String locationCode;		// Location code.
	long pickTime;				// Pick arrival time.
	String locatorPhase;		// Phase code identified by the Locator.
	String originalPhase;		// Phase code identified externally.
	int authorType;				// Code for human vs automatic, NEIC vs contributed.
	boolean usePick;			// Use this pick in the location.
	double pickAffinity;		// How good the analyst identification should be.
	double pickQuality;			// Quality of the pick.
	
	/**
	 * The following Locator input parameters are expected for each pick.
	 * 
	 * @param source Source of the database pick ID (optional).
	 * @param pickID Hydra database pick ID (optional).
	 * @param stationCode Station code.
	 * @param componentCode Component code.
	 * @param networkCode Network code.
	 * @param locationCode Location code.
	 * @param pickTime Pick time in milliseconds.
	 * @param locatorPhase Current locator seismic phase code.
	 * @param originalPhase Original seismic phase code.
	 * @param usePick If true, this pick may be used in the location.
	 * @param authorType 1 = automatic contributed, 2 = automatic NEIC, 
	 * 3 = analyst contributed, 4 = NEIC analyst.
	 * @param pickAffinity The higher the affinity, the harder it is to re-identify 
	 * a pick.  By default, the affinity for the four author types would be 
	 * 1.0, 1.0, 1.5, and 3.0 respectively.
	 * @param pickQuality The pick standard deviation in seconds.
	 */
	PickInput(String source, int pickID, String stationCode, 
			String componentCode, String networkCode, String locationCode, 
			long pickTime, String locatorPhase, String originalPhase, 
			boolean usePick, int authorType, double pickAffinity, 
			double pickQuality) {
		this.source = source;
		this.pickID = pickID;
		this.stationCode = stationCode;
		this.componentCode = componentCode;
		this.networkCode = networkCode;
		this.locationCode = locationCode;
		this.pickTime = pickTime;
		this.locatorPhase = locatorPhase;
		this.originalPhase = originalPhase;
		this.usePick = usePick;
		this.authorType = authorType;
		this.pickAffinity = pickAffinity;
		this.pickQuality = pickQuality;
	}
}

/**
 * Locator inputs needed to relocate an event.  This class is designed to contain 
 * all inputs needed for a location pass.  An object of this class should be 
 * created from the users inputs and will drive subsequent processing.
 * 
 * @author Ray
 *
 */
public class LocInput {
	String earthModel;			// Earth model to be used (optional)
	long originTime;			// Source origin time.
	double sourceLat;			// Geographic source latitude in degrees.
	double sourceLon;			// Geographic source longitude in degrees.
	double sourceDepth;			// Source depth relative to the WGS84 datum in kilometers.
	double bayesDepth;			// Analyst set Bayesian depth in kilometers.
	double bayesSpread;			// Analyst set Bayesian spread in kilometers.
	boolean heldLoc;			// The location is held (compute residuals, etc.)
	boolean heldDepth;			// The depth is held.
	boolean useBayes;			// Use the analyst set Bayesian depth.
	boolean useRstt;			// Use the RSTT regional travel-time model.
	boolean noSvd;				// Don't use the singular valued decomposition.
	boolean newLoc;				// The starting location has been changed externally.
	ArrayList<PickInput> pick = null;
	
	/**
	 * The following Locator input parameters control an event relocation.
	 * 
	 * @param earthModel The name of the Earth model to be used (default AK135).
	 * @param originTime The current source origin time in milliseconds.
	 * @param sourceLat The current geographic source latitude in degrees.
	 * @param sourceLon The current geographic source longitude in degrees.
	 * @param sourceDepth The current source depth in kilometers.
	 * @param bayesDepth The analyst specified Bayesian depth in kilometers.
	 * @param bayesSpread The analyst specified Bayesian depth standard deviation 
	 * in kilometers.
	 * @param heldLoc True if the current location cannot be changed.
	 * @param heldDepth True if the current depth cannot be changed.
	 * @param useBayes True if the analyst supplied Bayesian depth and spread 
	 * should be used.
	 * @param useRstt True if the RSTT regional Earth model should be used.
	 * @param noSvd True if the singular valued decomposition (i.e., pick 
	 * de-correlation) should not be used.
	 * @param newLoc True if the current location has changed outside of the Locator.
	 */
	public void addLoc(String earthModel, long originTime, double sourceLat, 
			double sourceLon, double sourceDepth, double bayesDepth, 
			double bayesSpread, boolean heldLoc, boolean heldDepth, 
			boolean useBayes, boolean useRstt, boolean noSvd, boolean newLoc) {
		if(earthModel != null) this.earthModel = earthModel;
		else this.earthModel = "AK135";
		this.originTime = originTime;
		this.sourceLat = sourceLat;
		this.sourceLon = sourceLon;
		this.sourceDepth = sourceDepth;
		this.bayesDepth = bayesDepth;
		this.bayesSpread = bayesSpread;
		this.heldLoc = heldLoc;
		this.heldDepth = heldDepth;
		this.useBayes = useBayes;
		this.useRstt = useRstt;
		this.noSvd = noSvd;
		this.newLoc = newLoc;
	}
	
	/**
	 * The following Locator input parameters are expected for each pick.
	 * 
	 * @param source Source of the database pick ID (optional).
	 * @param pickID Hydra database pick ID (optional).
	 * @param stationCode Station code.
	 * @param componentCode Component code.
	 * @param networkCode Network code.
	 * @param locationCode Location code.
	 * @param pickTime Pick time in milliseconds.
	 * @param locatorPhase Current locator seismic phase code.
	 * @param originalPhase Original seismic phase code.
	 * @param usePick If true, this pick may be used in the location.
	 * @param authorType 1 = automatic contributed, 2 = automatic NEIC, 
	 * 3 = analyst contributed, 4 = NEIC analyst.
	 * @param pickAffinity The higher the affinity, the harder it is to re-identify 
	 * a pick.  By default, the affinity for the four author types would be 
	 * 1.0, 1.0, 1.5, and 3.0 respectively.
	 * @param pickQuality The pick standard deviation in seconds.
	 */
	public void addPick(String source, int pickID, String stationCode, 
			String componentCode, String networkCode, String locationCode, 
			long pickTime, String locatorPhase, String originalPhase, 
			boolean usePick, int authorType, double pickAffinity, 
			double pickQuality) {
		if(pick == null) pick = new ArrayList<PickInput>();
		pick.add(new PickInput(source, pickID, stationCode, componentCode, 
			networkCode, locationCode, pickTime, locatorPhase, originalPhase, 
			usePick, authorType, pickAffinity, pickQuality));
	}
}
