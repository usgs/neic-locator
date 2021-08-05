package gov.usgs.locator;

import gov.usgs.locaux.AuthorType;
import gov.usgs.locaux.LocUtil;
import gov.usgs.processingformats.ErrorEllipseAxis;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.Utility;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

/**
 * The LocOutput class stores the outputs from an event relocation. This class is designed to
 * contain all parameters resulting from a Locator pass. An object of this class should be handed to
 * the output routines to be returned to the caller.
 *
 * @author jpatton@usgs.gov
 */
public class LocOutput extends LocationResult {
  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(LocOutput.class.getName());

  /** The LocOutput default constructor. */
  public LocOutput() {
    super();
  }

  /**
   * The LocOutput constructor. This constructor populates the LocOutput class with the givin
   * parameters that are produced by an event relocation.
   *
   * @param id A String containing the optional id, null to omit.
   * @param originTime A double containing the source origin time of this location in location in
   *     milliseconds.
   * @param sourceLatitude A Double containing the geographic source latitude of this location in
   *     degrees.
   * @param sourceLongitude A double containing the geographic source longitude of this location in
   *     degrees.
   * @param sourceDepth A double containing the source depth of this location in kilometers.
   * @param numStationsAssociated An int containing the number of stations associated with this
   *     location.
   * @param numPhasesAssociated An int containing the number of phases associated with this
   *     location.
   * @param numStationsUsed An int containing the number of stations used in this location.
   * @param numPhasesUsed An int containing the number of picks used in this location.
   * @param azimuthGap A double containing the standard azimuthal gap in degrees for this location.
   * @param azimuthalGapLEst A double containing the Robust (L-estimator) azimuthal gap in degrees
   *     for this location.
   * @param minStationDistance A double containing the minimum source-receiver distance in degrees
   *     for this location.
   * @param qualityFlags A String containing the summary event quality flags.
   */
  public LocOutput(
      String id,
      long originTime,
      double sourceLatitude,
      double sourceLongitude,
      double sourceDepth,
      int numStationsAssociated,
      int numPhasesAssociated,
      int numStationsUsed,
      int numPhasesUsed,
      double azimuthGap,
      double azimuthalGapLEst,
      double minStationDistance,
      String qualityFlags) {

    this.ID = id;

    // create subobjects
    this.Hypocenter = new gov.usgs.processingformats.Hypocenter();
    this.ErrorEllipse = new gov.usgs.processingformats.ErrorEllipse();
    this.SupportingData = new ArrayList<gov.usgs.processingformats.Pick>();

    // fill in information
    this.Hypocenter.Time = new Date(originTime);
    this.Hypocenter.Latitude = sourceLatitude;
    this.Hypocenter.Longitude = sourceLongitude;
    this.Hypocenter.Depth = sourceDepth;
    this.NumberOfAssociatedStations = numStationsAssociated;
    this.NumberOfAssociatedPhases = numPhasesAssociated;
    this.NumberOfUsedStations = numStationsUsed;
    this.NumberOfUsedPhases = numPhasesUsed;
    this.Gap = azimuthGap;
    this.SecondaryGap = azimuthalGapLEst;
    this.MinimumDistance = minStationDistance;
    this.Quality = qualityFlags;
  }

  /**
   * This function sets the following error parameters which are produced by an event relocation.
   *
   * @param timeStandardError A double containing the standard error of the origin time in seconds.
   * @param latitudeStandardError A double containing the standard error of the latitude in
   *     kilometers.
   * @param longitudeStandardError A double containing the standard error of the latitude in
   *     kilometers.
   * @param depthStandardError A double containing the standard error of the depth in kilometers.
   * @param residualsStandardError A double containing the standard error of the residuals in
   *     seconds.
   * @param maxHorizontalError A double containing the maximum horizontal projection of the error
   *     ellipsoid in kilometers.
   * @param maxVerticalError A double containing the maximum vertical projection of the error
   *     ellipsoid in kilometers.
   * @param equivalentErrorRadius A double containing the equivalent radius of the error ellipse in
   *     kilometers.
   * @param bayesianDepth A double containing the Bayesian depth in kilometers.
   * @param bayesianDepthSpread A double containing the Bayesian depth spread in kilometers.
   * @param bayesianDepthDataImportance A double containing the data importance of the baysian
   *     depth.
   * @param errorEllipse An array of EllipseAxis objects representing the error ellipse.
   * @param locatorExitCode A LocStatus object holding the final locator exit code.
   */
  public void addErrors(
      double timeStandardError,
      double latitudeStandardError,
      double longitudeStandardError,
      double depthStandardError,
      double residualsStandardError,
      double maxHorizontalError,
      double maxVerticalError,
      double equivalentErrorRadius,
      double bayesianDepth,
      double bayesianDepthSpread,
      double bayesianDepthDataImportance,
      EllipseAxis[] errorEllipse,
      LocStatus locatorExitCode) {

    this.Hypocenter.TimeError = timeStandardError;
    this.Hypocenter.LatitudeError = latitudeStandardError;
    this.Hypocenter.LongitudeError = longitudeStandardError;
    this.Hypocenter.DepthError = depthStandardError;

    this.RMS = residualsStandardError;
    this.BayesianDepth = bayesianDepth;
    this.BayesianRange = 3d * bayesianDepthSpread;
    this.DepthImportance = bayesianDepthDataImportance;

    if (errorEllipse != null) {
      this.ErrorEllipse.MaximumHorizontalProjection = maxHorizontalError;
      this.ErrorEllipse.MaximumVerticalProjection = maxVerticalError;
      this.ErrorEllipse.EquivalentHorizontalRadius = equivalentErrorRadius;

      if (errorEllipse[0] != null) {
        this.ErrorEllipse.E0 =
            new ErrorEllipseAxis(
                errorEllipse[0].getSemiLen(),
                errorEllipse[0].getAzimuth(),
                errorEllipse[0].getPlunge());
      }

      if (errorEllipse[1] != null) {
        this.ErrorEllipse.E1 =
            new ErrorEllipseAxis(
                errorEllipse[1].getSemiLen(),
                errorEllipse[1].getAzimuth(),
                errorEllipse[1].getPlunge());
      }

      if (errorEllipse[2] != null) {
        this.ErrorEllipse.E2 =
            new ErrorEllipseAxis(
                errorEllipse[2].getSemiLen(),
                errorEllipse[2].getAzimuth(),
                errorEllipse[2].getPlunge());
      }
    }

    // exit code conversion
    if (locatorExitCode == LocStatus.SUCCESSFUL_LOCATION) {
      this.LocatorExitCode = "Success";
    } else if (locatorExitCode == LocStatus.DID_NOT_MOVE) {
      this.LocatorExitCode = "DidNotMove";
    } else if (locatorExitCode == LocStatus.ERRORS_NOT_COMPUTED) {
      this.LocatorExitCode = "ErrorsNotComputed";
    } else if (locatorExitCode == LocStatus.NOT_ENOUGH_DATA) {
      this.LocatorExitCode = "NotEnoughData";
    } else if (locatorExitCode == LocStatus.DID_NOT_CONVERGE) {
      this.LocatorExitCode = "DidNotConverge";
    } else if (locatorExitCode == LocStatus.BAD_EVENT_INPUT) {
      this.LocatorExitCode = "BadInput";
    } else if (locatorExitCode == LocStatus.BAD_READ_TT_DATA) {
      this.LocatorExitCode = "CouldNotReadTTData";
    } else if (locatorExitCode == LocStatus.BAD_READ_AUX_DATA) {
      this.LocatorExitCode = "CouldNotReadAuxData";
    } else if (locatorExitCode == LocStatus.LOCATION_FAILED) {
      this.LocatorExitCode = "Failed";
    } else {
      this.LocatorExitCode = "Unknown";
    }
  }

  /**
   * This function adds a pick to LocOutput, using the following parameters which are produced for
   * each located pick.
   *
   * @param source A String containing the source of the database pick ID
   * @param authType An AuthorType object containing the type (e.g., human or auto) of the original
   *     phase identification
   * @param pickID A String containing the pick ID
   * @param stationCode A String containing the station code.
   * @param componentCode A String containing the component code.
   * @param networkCode A String containing the network code.
   * @param locationCode A String containing the location code.
   * @param stationLatitude A double containing the station latitude in degrees
   * @param stationLongitude A double containing the station longitude in degrees
   * @param stationElevation A double containing the station elevation in meters
   * @param pickTime A double containing the pick time in milliseconds.
   * @param locatorPhase A String containing the final seismic phase code.
   * @param originalAssocPhase A String containing the original assoc seismic phase code.
   * @param originalPickedPhase A String containing the original picked seismic phase code.
   * @param residual A double containing the pick residual in seconds.
   * @param delta A double containing the source-receiver distance in degrees.
   * @param azimuth A double containing the receiver azimuth (clockwise from north) in degrees.
   * @param weight A double containing the wick weight.
   * @param pickImport A double containing the pick data importance.
   * @param useFlag A boolean flag, true if the pick was used in the location.
   * @param pickAffinity A double containing the pick affinity, the higher the affinity, the harder
   *     it is to re-identify a pick. By default, the affinity for the four author types would be
   *     1.0, 1.0, 1.5, and 3.0 respectively.
   * @param pickQuality A double containing the pick standard deviation in seconds.
   */
  public void addPick(
      String source,
      AuthorType authType,
      String pickID,
      String stationCode,
      String componentCode,
      String networkCode,
      String locationCode,
      double stationLatitude,
      double stationLongitude,
      double stationElevation,
      long pickTime,
      String locatorPhase,
      String originalAssocPhase,
      String originalPickedPhase,
      double residual,
      double delta,
      double azimuth,
      double weight,
      double pickImport,
      boolean useFlag,
      double pickAffinity,
      double pickQuality) {

    // source type conversion
    String typeString;
    switch (authType) {
      case CONTRIB_AUTO: // automatic contributed
        typeString = "ContributedAutomatic";
        break;
      case LOCAL_AUTO: // automatic NEIC
        typeString = "LocalAutomatic";
        break;
      case CONTRIB_HUMAN: // analyst contributed
        typeString = "ContributedHuman";
        break;
      case LOCAL_HUMAN: // NEIC analyst
        typeString = "LocalHuman";
        break;
      default:
        typeString = "ContributedAutomatic";
        break;
    }

    // empty phases become null in proc formats
    String originalPickedPhaseCode = null;
    if (!"".equals(originalPickedPhase)) {
      originalPickedPhaseCode = originalPickedPhase;
    }
    String originalAssocPhaseCode = null;
    if (!"".equals(originalAssocPhase)) {
      originalAssocPhaseCode = originalAssocPhase;
    }
    String locatorPhaseCode = null;
    if (!"".equals(locatorPhase)) {
      locatorPhaseCode = locatorPhase;
    }
    // agencyid/author conversion
    String[] sourceArray = source.split("\\|", -1);

    SupportingData.add(
        new gov.usgs.processingformats.Pick(
            pickID,
            stationCode,
            componentCode,
            networkCode,
            locationCode,
            stationLatitude,
            stationLongitude,
            stationElevation,
            sourceArray[0],
            sourceArray[1],
            typeString,
            new Date(pickTime),
            pickAffinity,
            pickQuality,
            useFlag,
            originalPickedPhaseCode,
            originalAssocPhaseCode,
            locatorPhaseCode,
            residual,
            delta,
            azimuth,
            weight,
            pickImport));
  }

  /**
   * This function generates a Bulletin Hydra style output file.
   *
   * @param filePath A String containing the file name and path to write the hydra output to.
   * @return Returns true if successful, false otherwise
   */
  public boolean writeHydra(String filePath) {
    LOGGER.fine("Writing a hydra file to: " + filePath);
    try {
      PrintWriter fileWriter = new PrintWriter(filePath, "UTF-8");

      fileWriter.format(
          "\n%14.3f %8.4f %9.4f %6.2f %4d %4d %4d %4d %3.0f " + "%8.4f\n",
          LocUtil.toHydraTime(Hypocenter.Time.getTime()),
          Hypocenter.Latitude,
          Hypocenter.Longitude,
          Hypocenter.Depth,
          NumberOfAssociatedStations,
          NumberOfAssociatedPhases,
          NumberOfUsedStations,
          NumberOfUsedPhases,
          Gap,
          MinimumDistance);
      fileWriter.format(
          "%6.2f %6.1f %6.1f %6.1f %6.2f %6.1f %6.1f %6.1f " + "%3s %5.1f %5.1f %6.4f\n",
          Hypocenter.TimeError,
          Hypocenter.LatitudeError,
          Hypocenter.LongitudeError,
          Hypocenter.DepthError,
          RMS,
          ErrorEllipse.MaximumHorizontalProjection,
          ErrorEllipse.MaximumVerticalProjection,
          ErrorEllipse.EquivalentHorizontalRadius,
          Quality,
          BayesianDepth,
          BayesianRange,
          DepthImportance);
      fileWriter.format(
          "%6.1f %3.0f %3.0f ",
          ErrorEllipse.E0.Error, ErrorEllipse.E0.Azimuth, ErrorEllipse.E0.Dip);
      fileWriter.format(
          "%6.1f %3.0f %3.0f ",
          ErrorEllipse.E1.Error, ErrorEllipse.E1.Azimuth, ErrorEllipse.E1.Dip);
      fileWriter.format(
          "%6.1f %3.0f %3.0f  ",
          ErrorEllipse.E2.Error, ErrorEllipse.E2.Azimuth, ErrorEllipse.E2.Dip);
      fileWriter.format("%3.0f\n", SecondaryGap);

      // picks
      for (int j = 0; j < SupportingData.size(); j++) {
        fileWriter.print(writeHydraPick(SupportingData.get(j)));
      }

      // done with file
      fileWriter.close();
    } catch (Exception e) {
      LOGGER.severe(e.toString());
      return false;
    }
    return true;
  }

  /**
   * This function converts the provided pick into a Bulletin Hydra style output pick line.
   *
   * @param pick A gov.usgs.processingformats.Pick containing the pick to write
   * @return Returns a string containing the Bulletin Hydra style output pick line.
   */
  public String writeHydraPick(gov.usgs.processingformats.Pick pick) {
    return String.format(
        "%-10s %-5s %-3s %-2s %-2s %-8s%6.1f %5.1f %3.0f %1s %4.2f %6.4f\n",
        pick.ID,
        pick.Site.Station,
        pick.Site.Channel,
        pick.Site.Network,
        pick.Site.Location,
        pick.LocatedPhase,
        pick.Residual,
        pick.Distance,
        pick.Azimuth,
        LocUtil.getBoolChar(pick.Use),
        pick.Weight,
        pick.Importance);
  }

  /**
   * This function generates a json formatted output file.
   *
   * @param filePath A String containing the file name and path to write the json output to.
   * @return Returns true if successful, false otherwise
   */
  public boolean writeJSON(String filePath) {
    LOGGER.info("Writing a json file to: " + filePath);
    try {
      String outputString = Utility.toJSONString(toJSON());

      PrintWriter fileWriter = new PrintWriter(filePath, "UTF-8");
      fileWriter.print(outputString);
      fileWriter.close();
    } catch (Exception e) {
      LOGGER.severe(e.toString());
      return false;
    }

    return true;
  }

  /** Print an NEIC style web output. */
  /*
  public void printNEIC() {
    // Print the hypocenter.
    System.out.format("\nLocation:             %-7s %-8s ±%6.1f km\n",
        LocUtil.niceLat(hypo.latitude), LocUtil.niceLon(hypo.longitude),
        errH);
    System.out.format("Depth:                %5.1f ±%6.1f km\n",
        hypo.depth, errZ);
    System.out.format("Origin Time:          %23s UTC\n",
        LocUtil.getNEICdate(hypo.originTime));
    System.out.format("Number of Stations:     %4d\n", staAssoc);
    System.out.format("Number of Phases:       %4d\n", phAssoc);
    System.out.format("Minimum Distance:     %6.1f\n", delMin);
    System.out.format("Travel Time Residual:  %5.2f\n", seTime);
    System.out.format("Azimuthal Gap:           %3.0f\n", azimGap);
    System.out.println("\n    Channel     Distance Azimuth Phase  "+
        "   Arrival Time Status    Residual Weight");
    // Sort the pick groups by distance.
    groups.sort(new GroupComp());
    // Print the picks.
    for(int j=0; j<groups.size(); j++) {
      groups.get(j).printNEIC();
    }
  }
  */

  /** Print out picks in the group in a way similar to the NEIC web format. */
  /*
  public void printNEIC() {
    switch(authorType) {
      case CONTRIB_HUMAN: case LOCAL_HUMAN:
        System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s "+
            " manual    %6.1f    %4.2f\n", networkCode, stationCode,
            componentCode, locationCode, delta, azimuth, locatorPhase,
            LocUtil.getNEICtime(pickTime), residual, weight);
        break;
      default:
        System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s "+
            " automatic  %6.1f    %4.2f\n", networkCode, stationCode,
            componentCode, locationCode, delta, azimuth, locatorPhase,
            LocUtil.getNEICtime(pickTime), residual, weight);
        break;
    }
  }
  */
}
