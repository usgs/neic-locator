package gov.usgs.locator;

/**
 * Request a session with the tau-p travel time package.  An object of this type 
 * should be passed to the tau-p package to initiate a session as specified by 
 * the user.
 * 
 * @author Ray
 *
 */
public class SessionRequest {
	String earthModel;			// Name of the Earth model to be used.
	double sourceLat;			// Geographical source latitude in degrees.
	double sourceLon;			// Geographical source longitude in degrees.
	double sourceDepth;			// Source depth relative to WGS84 in kilometers.
	String[] phaseList;			// List of desired phase groups.
	boolean useRstt;			// If true, use the RSTT regional model.
	boolean plotRequest;		// If true, this is a plot session.
	
	/**
	 * The simple request only specifies depth.  All results are computed by 
	 * distance.  Ellipticity and bounce point corrections as well as use of the 
	 * RSTT model are not available.
	 * 
	 * @param earthModel Name of the Earth model to be used (optional).
	 * @param sourceDepth Source depth relative to WGS84 in kilometers.
	 * @param phaseList List of desired phase groups (optional)
	 */
	public void simple(String earthModel, double sourceDepth, String[] phaseList) {
		// Handle the default Earth model.
		if(earthModel != null) this.earthModel = earthModel;
		else this.earthModel = "AK135";
		/* Flag the source latitude and longitude to ensure that a full travel-time 
		 * request following a simple session request does the right thing. */
		sourceLat = Double.NaN;
		sourceLon = Double.NaN;
		// Remember the source depth.
		this.sourceDepth = sourceDepth;
		// Handle the default phase list.
		if(phaseList != null) {
			if(phaseList.length > 0) {
				this.phaseList = new String[phaseList.length];
				for(int j=0; j<phaseList.length; j++) this.phaseList[j] = phaseList[j];
			} else{
				this.phaseList = new String[1];
				this.phaseList[0] = "all";
			} 
		} else {
			this.phaseList = new String[1];
			this.phaseList[0] = "all";
		}
		// Make sure that the RSTT model can't be used.
		useRstt = false;
		// This isn't a plot request.
		plotRequest = false;
	}
	
	/**
	 * A more general session request must include the source latitude and longitude 
	 * as well as depth.  In this case, ellipticity and bounce point corrections will 
	 * be added to the travel time results and the RSTT model may be invoked.
	 * 
	 * @param earthModel Name of the Earth model to be used (optional).
	 * @param sourceLat Geographical source latitude in degrees.
	 * @param sourceLon Geographical source longitude in degrees.
	 * @param sourceDepth Source depth relative to WGS84 in kilometers.
	 * @param phaseList List of desired phase groups (optional)
	 * @param useRstt If true, use the RSTT regional travel-time model.
	 */
	public void full(String earthModel, double sourceLat, double sourceLon, 
			double sourceDepth, String[] phaseList, boolean useRstt) {
		// Handle the default Earth model.
		if(earthModel != null) this.earthModel = earthModel;
		else this.earthModel = "AK135";
		// Remember the source parameters.
		this.sourceLat = sourceLat;
		this.sourceLon = sourceLon;
		this.sourceDepth = sourceDepth;
		// Handle the default phase list.
		if(phaseList != null) {
			if(phaseList.length > 0) {
				this.phaseList = new String[phaseList.length];
				for(int j=0; j<phaseList.length; j++) this.phaseList[j] = phaseList[j];
			} else{
				this.phaseList = new String[1];
				this.phaseList[0] = "all";
			} 
		} else {
			this.phaseList = new String[1];
			this.phaseList[0] = "all";
		}
		// Remember the use RSTT flag.
		this.useRstt = useRstt;
		// This isn't a plot request.
		plotRequest = false;
	}
	
	/**
	 * The plot request is identical to the simple request.  However, internally, it 
	 * will be handled differently and the outputs are quite different.  Note that 
	 * plot data can never include station specific or regional corrections.
	 * 
	 * @param earthModel Name of the Earth model to be used (optional).
	 * @param sourceDepth Source depth relative to WGS84 in kilometers.
	 * @param phaseList List of desired phase groups (optional)
	 */
	public void plot(String earthModel, double sourceDepth, String[] phaseList) {
		// Handle the default Earth model.
		if(earthModel != null) this.earthModel = earthModel;
		else this.earthModel = "AK135";
		/* Flag the source latitude and longitude to ensure that a full travel-time 
		 * request following a simple session request does the right thing. */
		sourceLat = Double.NaN;
		sourceLon = Double.NaN;
		// Remember the source depth.
		this.sourceDepth = sourceDepth;
		// Handle the default phase list.
		if(phaseList != null) {
			if(phaseList.length > 0) {
				this.phaseList = new String[phaseList.length];
				for(int j=0; j<phaseList.length; j++) this.phaseList[j] = phaseList[j];
			} else{
				this.phaseList = new String[1];
				this.phaseList[0] = "all";
			} 
		} else {
			this.phaseList = new String[1];
			this.phaseList[0] = "all";
		}
		// Make sure that the RSTT model can't be used.
		useRstt = false;
		// This is a plot request.
		plotRequest = true;
	}
}
