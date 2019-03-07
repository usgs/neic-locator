package gov.usgs.locator;

import gov.usgs.processingformats.LocationData;
import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationService;
import gov.usgs.traveltime.TTSessionLocal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class LocService implements LocationService {
  String modelPath = null;

  /**
   * Implement the location service interface.
   * 
   * @param request a Final LocationRequest containing the location request
   * @return A LocationData containing the resulting location
   */
  @Override
  public LocationData getLocation(final LocationRequest request) 
      throws LocationException {    
    // create locInput from LocationRequest
    LocInput in = new LocInput(request);

    // return result as a LocationData
    return (LocationData)getLocation(in);
  }
  
  /**
   * Get a location.
   * 
   * @param in a Final LocInput containing the location input
   * @return A LocOutput containing the resulting location output
   */
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
    event.printIn();

    // setup the locator
    Locate loc = new Locate(event, ttLocal, auxLoc);;
    
    // perform the location
    LocStatus status = loc.doLoc();
    event.setExitCode(status);

    // print results for debugging
    event.printNEIC();

    // only return on a successful completion
    if (status.status() > 3) {
      return null;
    }

    // return the result
    return event.serverOut();
  }
}