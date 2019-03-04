package gov.usgs.locator;

/**
 * Pick outputs after a relocation.
 * 
 * @author Ray Buland
 *
 */
public class PickOutput {
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
	AuthorType authorType;					// Code for human vs automatic, NEIC vs contributed.
	double residual;				// Pick residual in seconds.
	double delta;						// Source-receiver distance in degrees.
	double azimuth;					// Receiver azimuth in degrees.
	double weight;					// Final weight applied to the pick.
	double pickImport;			// Pick data importance.
	boolean useFlag;				// Pick use flag.
	double pickAffinity;		// How good the analyst identification should be.
	double pickQuality;			// Quality of the pick.
	String errorCode;

	/**
	 * The following Locator outputs are produced for each pick.
	 * 
	 * @param source Source of the database pick ID (optional).
	 * @param authorType 1 = automatic contributed, 2 = automatic NEIC, 
	 * 3 = analyst contributed, 4 = NEIC analyst.
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
	public PickOutput(String source, AuthorType authType, String pickID, 
			String stationCode, String componentCode, String networkCode, 
			String locationCode, double stationLatitude, double stationLongitude, 
			double stationElevation, long pickTime, String locatorPhase, 
			String originalPhase, double residual, double delta, double azimuth, 
			double weight, double pickImport, boolean useFlag, 
			double pickAffinity, double pickQuality, String errorCode) {
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
		this.authorType = authType;
		this.residual = residual;
		this.delta = delta;
		this.azimuth = azimuth;
		this.weight = weight;
		this.pickImport = pickImport;
		this.useFlag = useFlag;
		this.pickAffinity = pickAffinity;
		this.pickQuality = pickQuality;		
		this.errorCode = errorCode;
	}
	
	/**
	 * Print out picks in the group in a way similar to the NEIC web format.
	 */
	public void printNEIC() {
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
	}
}