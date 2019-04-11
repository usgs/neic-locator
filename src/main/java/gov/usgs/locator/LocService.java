package gov.usgs.locator;

import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.LocationService;
import gov.usgs.traveltime.TTSessionLocal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class LocService implements LocationService {
  /** A String containing the earth model path for the locator, null to use default. */
  private String modelPath = null;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(LocService.class.getName());

  /** The LocService constructor. Sets up the earth model path */
  public LocService(String modelPath) {
    this.modelPath = modelPath;
  }

  /**
   * Function to get a location using the provided input, implementing the location service
   * interface.
   *
   * @param request a Final LocationRequest containing the location request
   * @return A LocationResult containing the resulting location
   */
  @Override
  public LocationResult getLocation(final LocationRequest request) throws LocationException {
    // create locInput from LocationRequest
    LocInput in = new LocInput(request);

    // return result as a LocationResult
    return (LocationResult) getLocation(in);
  }

  /**
   * Function to get a location using the provided input.
   *
   * @param in a Final LocInput containing the location input
   * @return A LocOutput containing the resulting location output
   */
  public LocOutput getLocation(final LocInput in) throws LocationException {
    // check to see if the input is valid
    if (!in.isValid()) {
      ArrayList<String> errorList = in.getErrors();

      // combine the errors into a single string
      String errorString = "";
      for (int i = 0; i < errorList.size(); i++) {
        errorString += " " + errorList.get(i);
      }

      LOGGER.severe("Invalid input: " + errorString);
      return null;
    }

    // init the tt models
    TTSessionLocal ttLocal = null;
    try {
      ttLocal = new TTSessionLocal(true, true, true, modelPath);
    } catch (IOException e) {
      LOGGER.severe("Unable to read travel-time auxiliary data.");
      e.printStackTrace();
      return null;
    }

    // Read the Locator auxiliary files.
    AuxLocRef auxLoc = null;
    try {
      auxLoc = new AuxLocRef(modelPath);
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.severe("Unable to read Locator auxiliary data.");
      e.printStackTrace();
      return null;
    }

    // make sure we have an earth model
    if (in.getEarthModel() == null) {
      in.setEarthModel("ak135");
    }

    // setup the event
    Event event = new Event(in.getEarthModel());
    event.input(in);

    // print input for debugging
    LOGGER.info("Input: \n" + event.getHydraInput());

    // setup the locator
    Locate loc = new Locate(event, ttLocal, auxLoc);

    // perform the location
    LocStatus status = loc.doLocation();
    event.setLocatorExitCode(status);

    // print results for debugging
    LOGGER.info("Results: \n" + event.getHydraOutput() + "\n" + event.getNEICOutput());

    // get the output
    LocOutput out = event.output();

    // check output
    if (!out.isValid()) {
      ArrayList<String> errorList = out.getErrors();

      // combine the errors into a single string
      String errorString = "";
      for (int i = 0; i < errorList.size(); i++) {
        errorString += " " + errorList.get(i);
      }

      LOGGER.severe("Invalid output: " + errorString);
    }

    // return the result
    return out;
  }
}
