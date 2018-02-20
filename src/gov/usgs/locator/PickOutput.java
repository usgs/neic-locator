package gov.usgs.locator;

/**
 * Pick outputs after a relocation.
 * 
 * @author Ray Buland
 *
 */
class PickOutput {
	String source;					// Source of the pick ID
	int pickID;							// Hydra database ID
	String stationCode;			// Station code.
	String componentCode;		// Component code.
	String networkCode;			// Network code.
	String locationCode;		// Location code.
	String locatorPhase;		// Phase code identified by the Locator.
	double residual;				// Pick residual in seconds.
	double delta;						// Source-receiver distance in degrees.
	double azimuth;					// Receiver azimuth in degrees.
	double weight;					// Final weight applied to the pick.
	double pickImport;			// Pick data importance.
	boolean useFlag;				// Pick use flag.
	String errorCode;				// Pick summary error code (RSTT)
	
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
			double weight, double pickImport, boolean useFlag, String errorCode) {
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