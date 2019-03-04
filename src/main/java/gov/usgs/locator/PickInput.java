package gov.usgs.locator;

/**
 * Pick inputs needed to relocate an event.
 * 
 * @author Ray Buland
 *
 */
public class PickInput {
	String source;					// Source of the pick ID
	String pickID;							// Hydra database ID
	String stationCode;			// Station code.
	String componentCode;		// Component code.
	String networkCode;			// Network code.
	String locationCode;		// Location code.
	double latitude;			// Station latitude 
	double longitude;			// Station longitude
	double elevation;			// Station elevation 		
	long pickTime;					// Pick arrival time.
	String locatorPhase;		// Phase code identified by the Locator.
	String originalPhase;		// Phase code identified externally.
	int authorType;					// Code for human vs automatic, NEIC vs contributed.
	boolean usePick;				// Use this pick in the location.
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
	 * @param stationLatitude station latitude
	 * @param stationLongitude station longitude
	 * @param stationElevation station elevation
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
	PickInput(String source, String pickID, String stationCode, 
			String componentCode, String networkCode, String locationCode, 
			double stationLatitude, double stationLongitude, 
			double stationElevation, long pickTime, String locatorPhase, 
			String originalPhase, boolean usePick, int authorType, 
			double pickAffinity, double pickQuality) {
		this.source = source;
		this.pickID = pickID;
		this.stationCode = stationCode;
		this.componentCode = componentCode;
		this.networkCode = networkCode;
		this.locationCode = locationCode;
		this.latitude = stationLatitude;
		this.longitude = stationLongitude;
		this.elevation = stationElevation;
		this.pickTime = pickTime;
		this.locatorPhase = locatorPhase;
		this.originalPhase = originalPhase;
		this.usePick = usePick;
		this.authorType = authorType;
		this.pickAffinity = pickAffinity;
		this.pickQuality = pickQuality;
	}
}