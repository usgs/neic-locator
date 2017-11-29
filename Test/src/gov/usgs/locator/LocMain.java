package gov.usgs.locator;

import java.io.IOException;

/**
 * Test driver for the Locator.
 * 
 * @author Ray Buland
 *
 */
public class LocMain {
	public static void main(String[] args) throws Exception {
		// Set up the earthquake file.
		String inFile = "../../../Documents/Work/Events/RayLocInput1000655616_3.txt";
		// Objects we'll need.
		AuxTtRef auxtt;
		ReadTau readTau;
		AllBrnRef allRef;
		AllBrnVol allBrn;
		Event event = null;
		Locate loc;

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
				System.out.println("Unable to read event.");
				System.exit(1);;
			}
			
			loc = new Locate(event, allBrn, auxtt);
			loc.doLoc();
			
		} catch(IOException e) {
			System.out.println("Unable to read Earth model ak135.");
			System.exit(1);
		}
		
		// Print the event.
		event.staStats();
//	event.printHydra();
		event.printNEIC();
		
		// Print a station list
//	event.stationList();
	}
}
