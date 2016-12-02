package gov.usgs.locator;

/**
 * Request a set of travel times or plot data within the current session.  An 
 * object of this type should be passed to the tau-p package for each user 
 * request for travel-time points or curves.
 * 
 * @author Ray
 *
 */
public class TtRequest {
	double stationLat;			// Geographical receiver latitude in degrees.
	double stationLon;			// Geographical receiver longitude in degrees.
	double stationElev;			// Receiver elevation relative to WGS84 in kilometers.
	double delta;				// Source-Receiver distance in degrees.
	boolean ttPlot;				// If true, travel-time plot data has been requested.
	SessionRequest source = null;		// The source information from session initiation.
	
	/**
	 * The session request will be needed for source-receiver calculations and 
	 * to cross check that the right information is available.
	 * 
	 * @param source The session initiation information will be needed later.
	 */
	public TtRequest(SessionRequest source) {
		this.source = source;
	}
	
	/**
	 * Request a set of travel times by distance.  Only the elevation correction 
	 * is added.
	 * 
	 * @param delta Source-receiver distance in degrees.
	 * @param stationElev Receiver elevation relative to WGS84 in kilometers.
	 */
	public void simple(double delta, double stationElev) {
		this.delta = delta;
		/* Flag that this is a simple request in case the user made a full session 
		 * request. */
		this.stationLat = Double.NaN;
		this.stationLon = Double.NaN;
		this.stationElev = stationElev;
	}
	
	/**
	 * Request a set of travel times by station coordinates.  In this case, 
	 * ellipticity and bounce point corrections will be added to the travel 
	 * time results as well as the elevation correction.
	 * 
	 * @param delta Source-receiver distance in degrees (optional).
	 * @param stationLat Geographical receiver latitude in degrees.
	 * @param stationLon Geographical receiver longitude in degrees.
	 * @param stationElev Receiver elevation relative to WGS84 in kilometers.
	 */
	public void full(double delta, double stationLat, double stationLon, 
			double stationElev) {
		/* The plan is to compute delta if it is NaN or out of range.  This 
		 * requires equivalents of the geocen and delaz routines from the 
		 * FORTRAN program, which haven't been ported yet.  If the session 
		 * request was simple, an error will be returned to the user. */
		this.delta = delta;
		this.stationLat = stationLat;
		this.stationLon = stationLon;
		this.stationElev = stationElev;
	}
	
	/**
	 * Request the return of data suitable for plotting travel times versus 
	 * distance.
	 */
	public void ttPlot() {
		ttPlot = true;
	}
	
	/**
	 * Request the return of data suitable for plotting travel time statistics 
	 * versus distance.
	 */
	public void statPlot() {
		ttPlot = false;
	}

}
