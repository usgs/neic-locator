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
		AuxTtRef auxtt;
		ReadTau readTau;
		AllBrnRef allRef;
		AllBrnVol allBrn;
		PhaseID phaseID;
		Event event;
		
		// Read in data common to all models.
		auxtt = new AuxTtRef(false, true, false, false);
	
		try {
			// Read in ak135.
			readTau = new ReadTau("ak135");
			readTau.readHeader();
			readTau.readTable();
			// Reorganize the reference data.
			allRef = new AllBrnRef(readTau, auxtt);
			// Set up the (depth dependent) volatile part.
			allBrn = new AllBrnVol(allRef);
			allBrn.dumpTable(true);
			
			// Set up the event.
			event = new Event();
			if(event.readHydra(inFile, false)) {
				event.printIn();
			} else {
				System.out.println("Event read failed");
			}
			
			// Use a skeleton PhaseID to test travel-time corrections.
			phaseID = new PhaseID(event, allBrn);
			phaseID.doID(false);
			
		} catch(IOException e) {
			System.out.println("Unable to read Earth model ak135.");
			System.exit(1);
		}
	}
}
