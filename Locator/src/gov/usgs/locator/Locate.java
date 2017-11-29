package gov.usgs.locator;

/**
 * Locate drives the location of one earthquake.
 * 
 * @author Ray Buland
 *
 */
public class Locate {
	String[] phList = null;
	Event event;
	AllBrnVol allBrn;
	AuxTtRef auxtt;
	PhaseID phaseID;

	/**
	 * Set up the machinery to locate one event.
	 * 
	 * @param event Event information
	 * @param allBrn Travel-time information
	 * @param auxtt Auxiliary travel-time information
	 */
	public Locate(Event event, AllBrnVol allBrn, AuxTtRef auxtt) {
		this.event = event;
		this.allBrn = allBrn;
		this.auxtt = auxtt;
		phaseID = new PhaseID(event, allBrn, auxtt);
	}
	
	/**
	 * Location driver.
	 */
	public void doLoc() {
		// Set the travel-times up for this event location.
		try {
			// Set up a new session.
			allBrn.newSession(event.hypo.latitude, event.hypo.longitude, 
					event.hypo.depth, phList);
			// Use a skeleton PhaseID to test travel-time corrections.
			phaseID.doID(0.1d, 1.0d);
		} catch (Exception e) {
			// This should never happen.
			System.out.println("Source depth out of range");
			e.printStackTrace();
		}
	}
}
