package gov.usgs.locator;

import gov.usgs.locaux.LocUtil;
import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.LocationService;
import gov.usgs.traveltime.TTSessionLocal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class LocService implements LocationService {
  /** Class to manage the travel-time external files. */
  private TTSessionLocal ttLocal = null;

  /** Class to manage the locator external files. */
  private LocSessionLocal locLocal = null;

  /** A String containing the serialized path for the locator, null to use default. */
  private String serializedPath = null;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(LocService.class.getName());

  /**
   * The LocService constructor. Sets up the travel-times and locator external files.
   *
   * @param modelPath A String containing the earth model path to use
   * @throws gov.usgs.processingformats.LocationException Throws a LocationException upon certain
   *     severe errors.
   */
  public LocService(String modelPath, String serializedPath) throws LocationException {
    long ttStartTime = System.currentTimeMillis();
    // init the tt models
    try {
      ttLocal = new TTSessionLocal(true, true, true, modelPath, serializedPath);
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.severe("Unable to read travel-time auxiliary data.");
      e.printStackTrace();
      throw new LocationException("Unable to read travel-time auxiliary data.");
    }

    LOGGER.info(LocUtil.endTimer("Time to load tt models", ttStartTime));

    // Read the Locator auxiliary files.
    long auxStartTime = System.currentTimeMillis();
    try {
      locLocal = new LocSessionLocal(modelPath, serializedPath);
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.severe("Unable to read Locator auxiliary data.");
      e.printStackTrace();
      throw new LocationException("Unable to read Locator auxiliary data.");
    }

    LOGGER.info(LocUtil.endTimer("Time to load aux files", auxStartTime));
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

    // create locInput from LocationRequest
    LocInput in = new LocInput(request);

    // compute result
    LocationResult result = (LocationResult) getLocation(in);

    LOGGER.info(
        "Event: "
            + request.ID
            + ", Result: "
            + result.LocatorExitCode
            + ", "
            + LocUtil.endLocationTimer()
            + ", numData: "
            + request.InputData.size());

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
    long validStartTime = System.currentTimeMillis();
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

    LOGGER.info(LocUtil.endTimer("Time to check if input is valid", validStartTime));

    // make sure we have an earth model
    if (in.EarthModel == null) {
      in.EarthModel = "ak135";
    }

    // setup the event
    long setupStartTime = System.currentTimeMillis();

    Event event = new Event(in.EarthModel);
    event.input(in);

    LOGGER.info(LocUtil.endTimer("Time to setup event for location", setupStartTime));

    // print input for debugging
    LOGGER.info("Input: \n" + event.getHydraInput(false));

    // make sure we have a slab resolution
    if (in.SlabResolution == null) {
      in.SlabResolution = "2spd";
    }

    // Get a locator with the required slab model resolution
    long slabStartTime = System.currentTimeMillis();
    Locate loc = null;
    try {
      loc = locLocal.getLocate(event, ttLocal, in.SlabResolution);
    } catch (ClassNotFoundException | IOException e) {
      LOGGER.severe("Unable to read slab model data.");
      e.printStackTrace();
      throw new LocationException("Unable to read slab model data.");
    }

    LOGGER.info(LocUtil.endTimer("Time to get locator with right slab", slabStartTime));

    // perform the location
    long locationStartTime = System.currentTimeMillis();

    LocStatus status = loc.doLocation();

    LOGGER.info(LocUtil.endTimer("Time to compute location", locationStartTime));

    // convert exit code
    long outputStartTime = System.currentTimeMillis();
    event.setLocatorExitCode(status);

    // get the output
    LocOutput out = event.output();

    LOGGER.info(LocUtil.endTimer("Time to generate output", outputStartTime));

    // print output for debugging
    LOGGER.info("Results: \n" + event.getHydraOutput() + "\n" + event.getNEICOutput());

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
