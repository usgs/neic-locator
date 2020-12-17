package gov.usgs.locator;

import gov.usgs.locaux.AuxLocRef;
import gov.usgs.locaux.LocUtil;
import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.LocationService;
import gov.usgs.processingformats.Utility;
import gov.usgs.traveltime.TTSessionLocal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class LocService implements LocationService {
  /** A String containing the earth model path for the locator, null to use default. */
  private String modelPath = null;

  /** A String containing the serialized path for the locator, null to use default. */
  private String serializedPath = null;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(LocService.class.getName());

  /**
   * The LocService constructor. Sets up the earth model path.
   *
   * @param modelPath A String containing the earth model path to use
   */
  public LocService(String modelPath, String serializedPath) {
    this.modelPath = modelPath;
    this.serializedPath = serializedPath;
  }

  /**
   * Function to get a location using the provided input, implementing the location service
   * interface.
   *
   * @param request a Final LocationRequest containing the location request
   * @return A LocationResult containing the resulting location
   * @throws gov.usgs.processingformats.LocationException Throws a LocationException upon certain
   *     severe errors.
   */
  @Override
  public LocationResult getLocation(final LocationRequest request) throws LocationException {
    if (request == null) {
      LOGGER.severe("Null request.");
      throw new LocationException("Null request");
    }
    LocUtil.startLocationTimer();

    // always print request as json to log for debugging
    LOGGER.fine("JSON Request: \n" + Utility.toJSONString(request.toJSON()));

    // create locInput from LocationRequest
    LocInput in = new LocInput(request);

    LocationResult result = (LocationResult) getLocation(in);

    LOGGER.info(
        "Event: "
            + request.ID
            + ", "
            + LocUtil.endLocationTimer()
            + ", "
            + request.InputData.size()
            + " numData.");

    // always print result as json to log for debugging, if it is valid
    if (result != null) {
      LOGGER.fine("JSON Result: \n" + Utility.toJSONString(result.toJSON()));
    } else {
      LOGGER.severe("Null result.");
      throw new LocationException("Null Result");
    }

    return result;
  }

  /**
   * Function to get a location using the provided input.
   *
   * @param in a Final LocInput containing the location input
   * @return A LocOutput containing the resulting location output
   * @throws gov.usgs.processingformats.LocationException Throws a LocationException upon certain
   *     severe errors.
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
      throw new LocationException("Invalid Input");
    }

    // init the tt models
    TTSessionLocal ttLocal = null;
    try {
      ttLocal = new TTSessionLocal(true, true, true, modelPath, serializedPath);
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.severe("Unable to read travel-time auxiliary data.");
      e.printStackTrace();
      throw new LocationException("Unable to read travel-time auxiliary data.");
    }

    // Read the Locator auxiliary files.
    AuxLocRef auxLoc = null;
    try {
      auxLoc = new AuxLocRef(modelPath);
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.severe("Unable to read Locator auxiliary data.");
      e.printStackTrace();
      throw new LocationException("Unable to read Locator auxiliary data.");
    }

    // make sure we have an earth model
    if (in.EarthModel == null) {
      in.EarthModel = "ak135";
    }

    // setup the event
    Event event = new Event(in.EarthModel);
    event.input(in);

    // print input for debugging
    LOGGER.info("Input: \n" + event.getHydraInput(false));

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
