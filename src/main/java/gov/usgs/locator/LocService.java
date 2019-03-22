package gov.usgs.locator;

import gov.usgs.processingformats.LocationResult;
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

  public LocService(String modelPath) {
    this.modelPath = modelPath;
  }

  /**
   * Implement the location service interface.
   * 
   * @param request a Final LocationRequest containing the location request
   * @return A LocationResult containing the resulting location
   */
  @Override
  public LocationResult getLocation(final LocationRequest request) 
      throws LocationException {    
    // create locInput from LocationRequest via cast
    LocInput in = new LocInput(request);

    // return result as a LocationResult via cast
    return (LocationResult)getLocation(in);
  }
  
  /**
   * Get a location.
   * 
   * @param in a Final LocInput containing the location input
   * @return A LocOutput containing the resulting location output
   */
  public LocOutput getLocation(final LocInput in) 
      throws LocationException {
    // check to see if the input is valid
    if (in.isValid() == false) {
      ArrayList<String> errorList = in.getErrors();

      // combine the errors into a single string
      String errorString = new String();
      for (int i = 0; i < errorList.size(); i++) {
        errorString += " " + errorList.get(i);
      }

      System.out.println("getLocation: Invalid input: " + errorString);
      return null;
    }

    // init the tt models
    TTSessionLocal ttLocal = null;
    try {
      ttLocal = new TTSessionLocal(true, true, true, modelPath);
    } catch (IOException e) {
      System.out.println("getLocation: Unable to read travel-time auxiliary data.");
      e.printStackTrace();
      return null;
    }

    // Read the Locator auxiliary files.
    AuxLocRef auxLoc = null;
    try {
      auxLoc = new AuxLocRef(modelPath);
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("getLocation: Unable to read Locator auxiliary data.");
      e.printStackTrace();
      return null;
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
    System.out.println("\nResults:");
    event.printNEIC();
    System.out.println("");

    // get the output
    LocOutput out = event.serverOut();

    // check output
    if (out.isValid() == false) {
      ArrayList<String> errorList = out.getErrors();

      // combine the errors into a single string
      String errorString = new String();
      for (int i = 0; i < errorList.size(); i++) {
        errorString += " " + errorList.get(i);
      }

      System.out.println("getLocation: Invalid output: " + errorString);
    }

    // return the result
    return out;
  }
}