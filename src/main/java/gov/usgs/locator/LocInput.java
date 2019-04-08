package gov.usgs.locator;

import gov.usgs.processingformats.LocationRequest;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
    setUseSVD(request.getIsLocationNew());
    setOutputData(request.getOutputData());
  }

  /**
   * This function sead a Bulletin Hydra style event input file. File open and read exceptions are
   * trapped.
   *
   * @param filePath A String containing the path to hydra file
   * @return True if the read was successful
   */
  public boolean readHydra(String filePath) {
    BufferedInputStream in;
    Scanner scan;
    Pattern affinity = Pattern.compile("\\d*\\.\\d*");

    // Set up the IO.
    try {
      in = new BufferedInputStream(new FileInputStream(filePath));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    }
    scan = new Scanner(in);
    try {
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
      char rstt = scan.next().charAt(0); // (not used)
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
          String errorString = new String();
          for (int i = 0; i < errorList.size(); i++) {
            errorString += " " + errorList.get(i);
          }

          LOGGER.warning("Invalid pick: " + errorString);
        }
      }

      // add the pick list to the request
      setInputData(pickList);

      // done with file
      scan.close();
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
