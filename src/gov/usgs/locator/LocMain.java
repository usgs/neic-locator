package gov.usgs.locator;

import java.io.IOException;

import gov.usgs.traveltime.*;

/**
 * Test driver for the locator.
 *
 * @author Ray Buland
 *
 */
public class LocMain {

	public static void main(String[] args) {
		// Set up the earthquake file.
		String inFile = "../../../Documents/Work/Events/RayLocInput1000010563_23.txt";
		// Objects we'll need.
		AuxTtRef auxtt = null;
		ReadTau readTau;
		AllBrnRef allRef;
		AllBrnVol allBrn;
		AuxLocRef auxLoc;
		Event event = null;
		Locate loc;

		// Read in data common to all models.
		try {
			auxtt = new AuxTtRef(false, false, false, false);
		} catch (IOException e1) {
			System.out.println("Unable to read auxiliary data.");
			e1.printStackTrace();
			System.exit(1);
		}

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
			// Set up aux loc
			auxLoc = new AuxLocRef();

			// Set the debug level.
			LocUtil.deBugLevel = 2;
			// Set up the event.
			event = new Event("ak135");
			if(event.readHydra(inFile)) {
				event.printIn();
			} else {
				System.out.println("Unable to read event.");
				System.exit(3);;
			}

			loc = new Locate(event, allBrn, auxLoc, auxtt);
			loc.doLoc();

		} catch(IOException e) {
			System.out.println("Unable to read Earth model ak135.");
			System.exit(2);
		}

		// Print the event.
		event.staStats();
//	event.printHydra();
		event.printNEIC();

		// Print a station list
//	event.stationList();
	}
}
