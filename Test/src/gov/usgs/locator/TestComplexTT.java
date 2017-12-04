package gov.usgs.locator;

import java.io.IOException;

/**
 * Test routine for travel-time features that need event and 
 * station locations (e.g., the ellipticity and bounce point 
 * corrections).
 * 
 * @author Ray Buland
 *
 */
public class TestComplexTT {
	public static void main(String[] args) throws Exception {
		String inFile = "../../../Documents/Work/Events/RayLocInput1000655616_3.txt";
//	String inFile = "../../../Documents/Work/Events/RayLocInput1000672389_13.txt";
		String[] phList = null;
		AuxTtRef auxtt;
		ReadTau readTau;
		AllBrnRef allRef;
		AllBrnVol allBrn;
		PhaseID phaseID;
		Event event;
		
		// Read in data common to all models.
		auxtt = new AuxTtRef(false, false, false, false);
	
		try {
			// Read in ak135.
			readTau = new ReadTau("ak135");
			readTau.readHeader();
			readTau.readTable();
			// Reorganize the reference data.
			allRef = new AllBrnRef(readTau, auxtt);
//		allRef.dumpBrn(false);
			// Set up the (depth dependent) volatile part.
			allBrn = new AllBrnVol(allRef);
//		allBrn.dumpTable(true);
			
			// Set up the event.
			event = new Event();
			if(event.readHydra(inFile)) {
				event.printIn();
			} else {
				System.out.println("Event read failed");
			}
			
			// Set the travel-times up for this event location.
			allBrn.newSession(event.hypo.latitude, event.hypo.longitude, 
					event.hypo.depth, phList);
//		allBrn.dumpCorrUp('P', true);
//		allBrn.dumpDecUp('P', true);
			
			// Use a skeleton PhaseID to test travel-time corrections.
			phaseID = new PhaseID(event, allBrn, auxtt);
			phaseID.doID(0.1d, 1.0d);
			
		} catch(IOException e) {
			System.out.println("Unable to read Earth model ak135.");
			System.exit(1);
		}
	}
}
