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
		// Set up the earth model.
		String earthModel = "ak135";
		// Set up the earthquake file.
		String eventID = "1000555699_19";
//	String eventID = "1000010563_23";
		// Objects we'll need.
		AuxTtRef auxTT = null;
		ReadTau readTau = null;
		LocInput in = null;
		LocOutput out = null;
		AllBrnRef allRef;
		AllBrnVol allBrn;
		AuxLocRef auxLoc = null;
		Event event = null;
		Locate loc;
		LocStatus status = null;
		
		// Read in data common to all models.
		try {
			auxTT = new AuxTtRef(false, false, false, false);
			auxLoc = new AuxLocRef();
		} catch (IOException e1) {
			System.out.println("Unable to read auxiliary data.");
			e1.printStackTrace();
			System.exit(LocStatus.BAD_READ_AUX_DATA.status());
		}
	
		// If server, get LocInput here.
		try {
			if(LocUtil.server) {
				in = new LocInput();		// In a server this would be provided externally
				earthModel = in.earthModel;
			}
			// Read in the model (should get the earth model from LocInput).
			readTau = new ReadTau(earthModel);
			readTau.readHeader();
			readTau.readTable();
		} catch(IOException e) {
			System.out.println("Unable to read Earth model "+earthModel+".");
			System.exit(LocStatus.BAD_READ_TT_DATA.status());
		}
		
		// Reorganize the reference data.
		allRef = new AllBrnRef(readTau, auxTT);
		// Set up the (depth dependent) volatile part.
		allBrn = new AllBrnVol(allRef);
		
		// Set the debug level.
		LocUtil.deBugLevel = 1;
			
		// Set up the event.
		event = new Event(earthModel);
		if(LocUtil.server) {
			event.serverIn(in);
		} else {
			if(event.readHydra(eventID)) {
				if(LocUtil.deBugLevel > 0) event.printIn();
			} else {
				System.out.println("Unable to read event.");
				System.exit(LocStatus.BAD_EVENT_INPUT.status());
			}
		}
		
		// Do the location.
		loc = new Locate(event, allBrn, auxLoc, auxTT);
		status = loc.doLoc();
		event.setExitCode(status);
		
		// Wrap up.
		if(LocUtil.server) {
			out = event.serverOut();		// JSON output
			out.printNEIC();
		} else {
			event.printHydra();
			event.printNEIC();
		}
		
		// Exit.
		System.exit(event.exitCode);
	}
}
