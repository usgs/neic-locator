package gov.usgs.locator;

import gov.usgs.detectionformats.Detection;
import gov.usgs.processingformats.LocationRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;

/**
 * The LocInput class stores the inputs needed to relocate an event. This class is designed to
 * contain all inputs needed for a location pass. An object of this class should be created from the
 * users inputs and will drive subsequent processing.
 *
 * @author jpatton@usgs.gov
 */
public class LocInput extends LocationRequest {
  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(LocInput.class.getName());

  /** The LocInput default constructor. */
  public LocInput() {
    super();
  }

  /**
   * The LocInput constructor. This constructor populates the LocInput class with the given
   * LocationRequest parameters
   *
   * @param request A LocationRequest object containing the input data
   */
  public LocInput(final LocationRequest request) {
    setID(request.getID());
    setType(request.getType());
    setEarthModel(request.getEarthModel());
    setSourceLatitude(request.getSourceLatitude());
    setSourceLongitude(request.getSourceLongitude());
    setSourceOriginTime(request.getSourceOriginTime());
    setSourceDepth(request.getSourceDepth());
    setInputData(request.getInputData());
    setIsLocationNew(request.getIsLocationNew());
    setIsLocationHeld(request.getIsLocationHeld());
    setIsDepthHeld(request.getIsDepthHeld());
    setIsBayesianDepth(request.getIsBayesianDepth());
    setBayesianDepth(request.getBayesianDepth());
    setBayesianSpread(request.getBayesianSpread());
    setUseSVD(request.getUseSVD());
    setOutputData(request.getOutputData());
  }

  /**
   * The LocInput constructor. This constructor populates the LocInput class with the given
   * Detection parameters
   *
   * @param detection A Detection object containing the input data
   */
  public LocInput(final Detection detection, JSONObject locationConfig) {
    setType("Locator");
    setID(detection.getID());
    setSourceLatitude(detection.getHypocenter().getLatitude());
    setSourceLongitude(detection.getHypocenter().getLongitude());
    setSourceDepth(detection.getHypocenter().getDepth());
    setSourceOriginTime(detection.getHypocenter().getTime());

    // optionally configurable values because there are no corresponding entries
    // in detection formats
    if ((locationConfig != null) && (locationConfig.containsKey("EarthModel"))) {
      setEarthModel(locationConfig.get("EarthModel").toString());
    } else {
      setEarthModel("ak135");
    }

    if ((locationConfig != null) && (locationConfig.containsKey("IsLocationNew"))) {
      setIsLocationNew((boolean) locationConfig.get("IsLocationNew"));
    } else {
      setIsLocationNew(true);
    }

    if ((locationConfig != null) && (locationConfig.containsKey("IsLocationHeld"))) {
      setIsLocationHeld((boolean) locationConfig.get("IsLocationHeld"));
    } else {
      setIsLocationHeld(false);
    }

    if ((locationConfig != null) && (locationConfig.containsKey("IsDepthHeld"))) {
      setIsDepthHeld((boolean) locationConfig.get("IsDepthHeld"));
    } else {
      setIsDepthHeld(false);
    }

    if ((locationConfig != null) && (locationConfig.containsKey("IsBayesianDepth"))) {
      setIsBayesianDepth((boolean) locationConfig.get("IsBayesianDepth"));
    } else {
      setIsBayesianDepth(false);
    }

    if ((locationConfig != null) && (locationConfig.containsKey("BayesianDepth"))) {
      setBayesianDepth((double) locationConfig.get("BayesianDepth"));
    } else {
      setBayesianDepth(0.0);
    }

    if ((locationConfig != null) && (locationConfig.containsKey("BayesianSpread"))) {
      setBayesianSpread((double) locationConfig.get("BayesianSpread"));
    } else {
      setBayesianSpread(0.0);
    }

    if ((locationConfig != null) && (locationConfig.containsKey("UseSVD"))) {
      setUseSVD((boolean) locationConfig.get("UseSVD"));
    } else {
      setUseSVD(true);
    }

    // default pick values
    String pickSourceType = "LocalAutomatic";
    if ((locationConfig != null) && (locationConfig.containsKey("PickSourceType"))) {
      pickSourceType = locationConfig.get("PickSourceType").toString();
    }

    double pickAffinity = 1.0;
    if ((locationConfig != null) && (locationConfig.containsKey("PickAffinity"))) {
      pickAffinity = (double) locationConfig.get("PickAffinity");
    }

    double pickQuality = 0.0;
    if ((locationConfig != null) && (locationConfig.containsKey("PickQuality"))) {
      pickQuality = (double) locationConfig.get("PickQuality");
    }

    boolean pickUse = true;
    if ((locationConfig != null) && (locationConfig.containsKey("PickUse"))) {
      pickUse = (boolean) locationConfig.get("PickUse");
    }

    // picks
    ArrayList<gov.usgs.processingformats.Pick> requestInputData =
        new ArrayList<gov.usgs.processingformats.Pick>();

    // for loop while converting picks
    for (Iterator<gov.usgs.detectionformats.Pick> picksIterator =
            detection.getPickData().iterator();
        picksIterator.hasNext(); ) {
      gov.usgs.detectionformats.Pick detectPick = picksIterator.next();
      gov.usgs.processingformats.Pick requestPick = new gov.usgs.processingformats.Pick();

      // check to see if we have a pick
      if (detectPick == null) {
        continue;
      }

      // check to see if we have a lat/lon/elev
      if ((detectPick.getSite().getLatitude() == null)
          || (detectPick.getSite().getLongitude() == null)
          || (detectPick.getSite().getElevation() == null)) {
        continue;
      }

      requestPick.setId(detectPick.getID());

      requestPick.setSite(
          new gov.usgs.processingformats.Site(
              detectPick.getSite().getStation(),
              detectPick.getSite().getChannel(),
              detectPick.getSite().getNetwork(),
              detectPick.getSite().getLocation(),
              detectPick.getSite().getLatitude(),
              detectPick.getSite().getLongitude(),
              detectPick.getSite().getElevation()));

      requestPick.setSource(
          new gov.usgs.processingformats.Source(
              detectPick.getSource().getAgencyID(),
              detectPick.getSource().getAuthor(),
              pickSourceType));

      requestPick.setTime(detectPick.getTime());
      requestPick.setAffinity(pickAffinity);
      requestPick.setQuality(pickQuality);
      requestPick.setUse(pickUse);
      requestPick.setPickedPhase(detectPick.getPhase());
      requestPick.setAssociatedPhase(detectPick.getAssociationInfo().getPhase());

      requestInputData.add(requestPick);
    }

    setInputData(requestInputData);
  }

  /**
   * This function read a Bulletin Hydra style event input file. File open and read exceptions are
   * trapped.
   *
   * @param fileString A String containing the input file contents to parse
   * @return True if the read was successful
   */
  public boolean readHydra(String fileString) {
    Scanner scan = new Scanner(fileString);
    Pattern affinity = Pattern.compile("\\d*\\.\\d*");

    // Get the hypocenter information.
    setSourceOriginTime(new Date(LocUtil.toJavaTime(scan.nextDouble())));
    setSourceLatitude(scan.nextDouble());
    setSourceLongitude(scan.nextDouble());
    setSourceDepth(scan.nextDouble());

    // Get the analyst commands.
    setIsLocationHeld(LocUtil.getBoolean(scan.next().charAt(0)));
    setIsDepthHeld(LocUtil.getBoolean(scan.next().charAt(0)));
    setIsBayesianDepth(LocUtil.getBoolean(scan.next().charAt(0)));
    setBayesianDepth(scan.nextDouble());
    setBayesianSpread(scan.nextDouble());
    scan.next().charAt(0); // rstt (not used)
    setUseSVD(!LocUtil.getBoolean(scan.next().charAt(0))); // True when noSvd is false

    // Fiddle because the analyst command last flag is omitted in earlier
    // data.
    char moved;
    if (scan.hasNextInt()) {
      moved = 'F';
    } else {
      moved = scan.next().charAt(0);
    }
    setIsLocationNew(LocUtil.getBoolean(moved));

    // create the pick list
    ArrayList<gov.usgs.processingformats.Pick> pickList =
        new ArrayList<gov.usgs.processingformats.Pick>();

    // Get the pick information.
    while (scan.hasNext()) {
      gov.usgs.processingformats.Pick newPick = new gov.usgs.processingformats.Pick();

      newPick.setId(scan.next());

      // Get the station information.
      gov.usgs.processingformats.Site newSite = new gov.usgs.processingformats.Site();
      newSite.setStation(scan.next());
      newSite.setChannel(scan.next());
      newSite.setNetwork(scan.next());
      newSite.setLocation(scan.next());
      newSite.setLatitude(scan.nextDouble());
      newSite.setLongitude(scan.nextDouble());
      newSite.setElevation(scan.nextDouble());
      newPick.setSite(newSite);

      // Get the rest of the pick information.  Note that some
      // fiddling is required as some of the positional arguments
      // are sometimes omitted.
      newPick.setQuality(scan.nextDouble());
      String curPh = null;
      if (!scan.hasNextDouble()) {
        curPh = scan.next();
      }
      newPick.setPickedPhase(curPh);

      newPick.setTime(new Date(LocUtil.toJavaTime(scan.nextDouble())));
      newPick.setUse(LocUtil.getBoolean(scan.next().charAt(0)));

      // convert author type
      // 1 = automatic contributed, 2 = automatic NEIC,
      // 3 = analyst contributed, 4 = NEIC analyst.
      int auth = scan.nextInt();
      String authType = null;
      if (auth == 1) {
        authType = "ContributedAutomatic";
      } else if (auth == 2) {
        authType = "LocalAutomatic";
      } else if (auth == 3) {
        authType = "ContributedHuman";
      } else if (auth == 4) {
        authType = "LocalHuman";
      } else {
        authType = "ContributedAutomatic";
      }
      // make up agency/author because a hydra input file does not have that
      // information, only author type
      gov.usgs.processingformats.Source newSource =
          new gov.usgs.processingformats.Source("US", "Hydra", authType);
      newPick.setSource(newSource);

      String obsPh = null;
      double aff = 0d;
      if (scan.hasNextInt() || !scan.hasNext()) {
        aff = 0d;
      } else if (scan.hasNext(affinity)) {
        aff = scan.nextDouble();
      } else {
        obsPh = scan.next();
        if (scan.hasNext(affinity)) {
          aff = scan.nextDouble();
        } else {
          aff = 0d;
        }
      }
      newPick.setAffinity(aff);
      newPick.setAssociatedPhase(obsPh);

      if (newPick.isValid()) {
        // Add the pick to the list
        pickList.add(newPick);
      } else {
        ArrayList<String> errorList = newPick.getErrors();

        // combine the errors into a single string
        String errorString = "";
        for (int i = 0; i < errorList.size(); i++) {
          errorString += " " + errorList.get(i);
        }

        LOGGER.warning("Invalid pick: " + errorString);
      }
    }

    // add the pick list to the request
    setInputData(pickList);

    // done with scanning
    scan.close();

    return true;
  }
}
