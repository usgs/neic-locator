package gov.usgs.locator;
import gov.usgs.traveltime.AllBrnVol;
import gov.usgs.traveltime.AuxTtRef;

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
	InitialID initialID;
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
		initialID = new InitialID(event, allBrn);
		phaseID = new PhaseID(event, allBrn, auxtt);
	}
	
	/**
	 * Location driver.
	 */
	public void doLoc() {
		// Print out some event stuff for comparison.
		System.out.println("\n"+event.hypo);
		event.printArrivals(true);
		// Set the travel-times up for this event location.
		try {
			// Set up a new session.
			allBrn.newSession(event.hypo.latitude, event.hypo.longitude, 
					event.hypo.depth, phList);
			// Mimic the start of the location driver phase identification.
			initialID.survey();
			
			// Do the stage 1 phase identification (no reID, but re-weight).
			phaseID.doID(0.01d,  5d, false, true);
			
			// Do a full stage 2 reID and re-weight.  Start by resetting the 
			// use flags.
			event.updateEvent(1217617571.841d, 50.1900d, -114.7276d, 1.00d);
			System.out.println("\n"+event.hypo);
			initialID.resetUseFlags();
			initialID.printInitialID();
			// Do the phase identification.
			phaseID.doID(0.1d, 1.0d, true, true);
		} catch (Exception e) {
			// This should never happen.
			System.out.println("Source depth out of range");
			e.printStackTrace();
		}
	}
}
