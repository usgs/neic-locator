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
    ID = request.ID;
    Type = request.Type;
    EarthModel = request.EarthModel;
    SourceLatitude = request.SourceLatitude;
    SourceLongitude = request.SourceLongitude;
    SourceOriginTime = request.SourceOriginTime;
    SourceDepth = request.SourceDepth;
    InputData = request.InputData;
    IsLocationNew = request.IsLocationNew;
    IsLocationHeld = request.IsLocationHeld;
    IsDepthHeld = request.IsDepthHeld;
    IsBayesianDepth = request.IsBayesianDepth;
    BayesianDepth = request.BayesianDepth;
    BayesianSpread = request.BayesianSpread;
    UseSVD = request.UseSVD;
    OutputData = request.OutputData;
  }

  /**
   * The LocInput constructor. This constructor populates the LocInput class with the given
   * Detection parameters
   *
   * @param detection A Detection object containing the input data
   */
  public LocInput(final Detection detection, JSONObject locationConfig) {
    Type = "Locator";
    ID = detection.getID();
    SourceLatitude = detection.getHypocenter().getLatitude();
    SourceLongitude = detection.getHypocenter().getLongitude();
    SourceDepth = detection.getHypocenter().getDepth();
    SourceOriginTime = detection.getHypocenter().getTime();

    // optionally configurable values because there are no corresponding entries
    // in detection formats
    if ((locationConfig != null) && (locationConfig.containsKey("EarthModel"))) {
      EarthModel = locationConfig.get("EarthModel").toString();
    } else {
      EarthModel = "ak135";
    }

    if ((locationConfig != null) && (locationConfig.containsKey("IsLocationNew"))) {
      IsLocationNew = (boolean) locationConfig.get("IsLocationNew");
    } else {
      IsLocationNew = true;
    }

    if ((locationConfig != null) && (locationConfig.containsKey("IsLocationHeld"))) {
      IsLocationHeld = (boolean) locationConfig.get("IsLocationHeld");
    } else {
      IsLocationHeld = false;
    }

    if ((locationConfig != null) && (locationConfig.containsKey("IsDepthHeld"))) {
      IsDepthHeld = (boolean) locationConfig.get("IsDepthHeld");
    } else {
      IsDepthHeld = false;
    }

    if ((locationConfig != null) && (locationConfig.containsKey("IsBayesianDepth"))) {
      IsBayesianDepth = (boolean) locationConfig.get("IsBayesianDepth");
    } else {
      IsBayesianDepth = false;
    }

    if ((locationConfig != null) && (locationConfig.containsKey("BayesianDepth"))) {
      BayesianDepth = (double) locationConfig.get("BayesianDepth");
    } else {
      BayesianDepth = 0.0;
    }

    if ((locationConfig != null) && (locationConfig.containsKey("BayesianSpread"))) {
      BayesianSpread = (double) locationConfig.get("BayesianSpread");
    } else {
      BayesianSpread = 0.0;
    }

    if ((locationConfig != null) && (locationConfig.containsKey("UseSVD"))) {
      UseSVD = (boolean) locationConfig.get("UseSVD");
    } else {
      UseSVD = true;
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

      requestPick.ID = detectPick.getID();

      requestPick.Site =
          new gov.usgs.processingformats.Site(
              detectPick.getSite().getStation(),
              detectPick.getSite().getChannel(),
              detectPick.getSite().getNetwork(),
              detectPick.getSite().getLocation(),
              detectPick.getSite().getLatitude(),
              detectPick.getSite().getLongitude(),
              detectPick.getSite().getElevation());

      requestPick.Source =
          new gov.usgs.processingformats.Source(
              detectPick.getSource().getAgencyID(),
              detectPick.getSource().getAuthor(),
              pickSourceType);

      requestPick.Time = detectPick.getTime();
      requestPick.Affinity = pickAffinity;
      requestPick.Quality = pickQuality;
      requestPick.Use = pickUse;
      requestPick.PickedPhase = detectPick.getPhase();
      requestPick.AssociatedPhase = detectPick.getAssociationInfo().getPhase();

      requestInputData.add(requestPick);
    }

    InputData = requestInputData;
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
    SourceOriginTime = new Date(LocUtil.toJavaTime(scan.nextDouble()));
    SourceLatitude = scan.nextDouble();
    SourceLongitude = scan.nextDouble();
    SourceDepth = scan.nextDouble();

    // Get the analyst commands.
    IsLocationHeld = LocUtil.getBoolean(scan.next().charAt(0));
    IsDepthHeld = LocUtil.getBoolean(scan.next().charAt(0));
    IsBayesianDepth = LocUtil.getBoolean(scan.next().charAt(0));
    BayesianDepth = scan.nextDouble();
    BayesianSpread = scan.nextDouble();
    scan.next().charAt(0); // rstt (not used)
    UseSVD = !LocUtil.getBoolean(scan.next().charAt(0)); // True when noSvd is false

    // Fiddle because the analyst command last flag is omitted in earlier
    // data.
    char moved;
    if (scan.hasNextInt()) {
      moved = 'F';
    } else {
      moved = scan.next().charAt(0);
    }
    IsLocationNew = LocUtil.getBoolean(moved);

    // create the pick list
    ArrayList<gov.usgs.processingformats.Pick> pickList =
        new ArrayList<gov.usgs.processingformats.Pick>();

    // Get the pick information.
    while (scan.hasNext()) {
      gov.usgs.processingformats.Pick newPick = new gov.usgs.processingformats.Pick();

      newPick.ID = scan.next();

      // Get the station information.
      gov.usgs.processingformats.Site newSite = new gov.usgs.processingformats.Site();
      newSite.Station = scan.next();
      newSite.Channel = scan.next();
      newSite.Network = scan.next();
      newSite.Location = scan.next();
      newSite.Latitude = scan.nextDouble();
      newSite.Longitude = scan.nextDouble();
      newSite.Elevation = scan.nextDouble();
      newPick.Site = newSite;

      // Get the rest of the pick information.  Note that some
      // fiddling is required as some of the positional arguments
      // are sometimes omitted.
      newPick.Quality = scan.nextDouble();
      String curPh = null;
      if (!scan.hasNextDouble()) {
        curPh = scan.next();
      }
      newPick.PickedPhase = curPh;

      newPick.Time = new Date(LocUtil.toJavaTime(scan.nextDouble()));
      newPick.Use = LocUtil.getBoolean(scan.next().charAt(0));

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
      newPick.Source = newSource;

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
      newPick.Affinity = aff;
      newPick.AssociatedPhase = obsPh;

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
    InputData = pickList;

    // done with scanning
    scan.close();

    return true;
  }
}
