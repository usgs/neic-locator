package gov.usgs.locator;
import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;

import gov.usgs.traveltime.*;
import gov.usgs.processingformats.*;

public class LocService implements LocationService {

    String modelPath = null;

    /**
	 * Implement the location service interface
	 * 
	 * @param request a Final LocationRequest containing the location request
	 */
    @Override
    public LocationData getLocation(final LocationRequest request) 
        throws LocationException {
        
        // create locInput from LocationRequest
        LocInput in = new LocInput(request);

        // return result as a LocationData
        return((LocationData)getLocation(in));
    }
    
    public LocOutput getLocation(final LocInput in) 
        throws LocationException {
        
        // init the tt models
        TTSessionLocal ttLocal = null;
        try {
            ttLocal = new TTSessionLocal(true, true, true, modelPath);
        } catch (IOException e) {
            System.out.println("Unable to read travel-time auxiliary data.");
            e.printStackTrace();
        }

        // Read the Locator auxiliary files.
        AuxLocRef auxLoc = null;
        try {
			auxLoc = new AuxLocRef(modelPath);
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Unable to read Locator auxiliary data.");
            e.printStackTrace();
        }

        // make sure we have an earth model
        if (in.getEarthModel() == null) {
            in.setEarthModel("ak135");
        }

        // setup the event
        Event event = new Event(in.getEarthModel());
        event.serverIn(in);

        // setup the locator
        Locate loc = new Locate(event, ttLocal, auxLoc);;
        
        // perform the location
        LocStatus status = loc.doLoc();
        event.setExitCode(status);

        // print results for debugging
        event.printNEIC();

        // only return on a successful completion
        if (status.status() > 3) {
            return(null);
        }

        // return the result
        return(event.serverOut());
    }
}