package gov.usgs.locator;

import gov.usgs.traveltime.TauUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The Event class keeps all data for one seismic event (earthquake usually).
 *
 * @author Ray Buland
 */
public class Event {
  /** A String containing the earth model used for this event. */
  private String earthModel;

  /** A boolean flag that if true indicates that the hypocenter will be held constant. */
  private boolean isLocationHeld;

  /** A boolean flag that if true indicates that the depth will be held constant. */
  private boolean isDepthHeld;

  /** A boolean flag that if true indicates that the Bayesian depth was set by an analyst. */
  private boolean isDepthManual;

  /**
   * A boolean flag that if true indicates that this event should use the decorrelation algorithm.
   */
  private boolean useDecorrelation;

  /** A boolean flag that if true indicates that the location has been moved externally. */
  private boolean isLocationRestarted;

  /** A boolean flag that if true indicates that the phase identification has changed. */
  private boolean hasPhaseIdChanged;

  /** An int containing the number of stationList associated with this event. */
  private int numStationsAssociated;

  /** An int containing the number of stationList used in this event. */
  private int numStationsUsed;

  /** An int containing the number of phases associated with this event. */
  private int numPhasesAssociated;

  /** An int containing the number of phases used in this event. */
  private int numPhasesUsed;

  /** An int containing the number of virtual (projected) phases used in this event. */
  private int numProjectedPhasesUsed;

  /** An int containing the number of local phases used. */
  private int numLocalPhasesUsed;

  /** A double containing the azimuthal gap in degrees. */
  private double azimuthalGap;

  /** A double containing the robust (L-estimator) azimuthal gap in degrees. */
  private double azimuthalGapLEst;

  /** A double containing the minimum station distance in degress. */
  private double minStationDistance;

  /** A String containing the summary event quality flags. */
  private String qualityFlags;

  /** A LocStatus object holding the locator exit code. */
  private LocStatus locatorExitCode;

  /** A double containing the standard error of the origin time in seconds. */
  private double timeStandardError;

  /** A double containing the standard error of the latitude in kilometers. */
  private double latitudeStandardError;

  /** A double containing the standard error of the latitude in kilometers. */
  private double longitudeStandardError;

  /** A double containing the standard error of the depth in kilometers. */
  private double depthStandardError;

  /** A double containing the standard error of the residuals in seconds. */
  private double residualsStandardError;

  /** A double containing the maximum horizontal projection of the error ellipsoid in kilometers. */
  private double maxHorizontalError;

  /** A double containing the maximum vertical projection of the error ellipsoid in kilometers. */
  private double maxVerticalError;

  /** A double containing the equivalent radius of the error ellipse in kilometers. */
  private double equivalentErrorRadius;

  /** An array of EllipseAxis objects representing the error ellipse. */
  private EllipseAxis[] errorEllipse;

  /** A double containing the Bayesian depth in kilometers. */
  private double bayesianDepth;

  /** A double containing the Bayesian depth spread in kilometers. */
  private double bayesianDepthSpread;

  /** A double containing the data importance of the baysian depth. */
  private double bayesianDepthDataImportance;

  /** A Hypocenter object containing the event hypocenter. */
  private Hypocenter hypo;

  /** A String containing the optional ID for the event */
  private String id;

  /**
   * An ArrayList of HypoAudit objects containing the hypocenter auditing information for logging
   * purposes.
   */
  private ArrayList<HypoAudit> hypoAuditList;

  /** A TreeMap of StationID and Station objects used as a station list for this event. */
  private TreeMap<StationID, Station> stationList;

  /**
   * An ArrayList of PickGroup objects, which group all the picks observed at a single station in
   * this event.
   */
  private ArrayList<PickGroup> pickGroupList;

  /** An ArrayList of Pick objects used as a list of all picks in this event. */
  private ArrayList<gov.usgs.locator.Pick> pickList;

  /**
   * An ArrayList of WeightedResidual objects containing the raw (sorted) weighted residuals of the
   * picks.
   */
  private ArrayList<WeightedResidual> rawWeightedResiduals = null;

  /**
   * An ArrayList of WeightedResidual objects containing the original weighted residuals of the
   * picks.
   */
  private ArrayList<WeightedResidual> originalWeightedResiduals = null;

  /**
   * An ArrayList of WeightedResidual objects containing the projected weighted residuals of the
   * picks.
   */
  private ArrayList<WeightedResidual> projectedWeightedResiduals = null;

  /** A RankSumEstimator object used for the rank-sum estimation of the raw picks. */
  private RankSumEstimator rawRankSumEstimator;

  /** A RankSumEstimator object used for the rank-sum estimation of the projected picks. */
  private RankSumEstimator projectedRankSumEstimator;

  /** A Decorrelator object used when decorrelating the event picks. */
  private Decorrelator decorrelator;

  /**
   * Function to return the event hypocenter object.
   *
   * @return A Hypocenter object containing the hypocenter information
   */
  public Hypocenter getHypo() {
    return hypo;
  }

  /**
   * Function to return the event origin time.
   *
   * @return A double containing the event origin time in double precision seconds since the epoch
   */
  public double getOriginTime() {
    return hypo.getOriginTime();
  }

  /**
   * Function to return the event latitude.
   *
   * @return A double containing the epicenter geographic latitude in degrees
   */
  public double getLatitude() {
    return hypo.getLatitude();
  }

  /**
   * Function to return the event longitude.
   *
   * @return A double containing the epicenter geographic longitude in degrees
   */
  public double getLongitude() {
    return hypo.getLongitude();
  }

  /**
   * Function to return the event depth.
   *
   * @return A double containing the hypocenter depth in kilometers
   */
  public double getDepth() {
    return hypo.getDepth();
  }

  /**
   * Function to return the earth model name.
   *
   * @return A String containing the earth model name
   */
  public String getEarthModel() {
    return earthModel;
  }

  /**
   * Function to return whether the hypocenter for this event will be held constant.
   *
   * @return A boolean flag indicating whether the hypocenter for this event will be held constant.
   */
  public boolean getIsLocationHeld() {
    return isLocationHeld;
  }

  /**
   * Function to return whether to use decorrelation.
   *
   * @return A boolean flag indicating whether to use decorrelation
   */
  public boolean getUseDecorrelation() {
    return useDecorrelation;
  }

  /**
   * Function to return whether the Bayesian depth was set by an analyst.
   *
   * @return A boolean flag indicating whether the Bayesian depth was set by an analyst.
   */
  public boolean getIsDepthManual() {
    return isDepthManual;
  }

  /**
   * Function to return whether the location has been moved externally.
   *
   * @return A boolean flag indicating whether the location has been moved externally.
   */
  public boolean getIsLocationRestarted() {
    return isLocationRestarted;
  }

  /**
   * Function to return whether the event phase ID's have changed.
   *
   * @return A boolean flag indicating whether the event phase ID's have changed
   */
  public boolean getHasPhaseIdChanged() {
    return hasPhaseIdChanged;
  }

  /**
   * Function to return the number of stationList used by the event.
   *
   * @return A int containing the number of stationList used by the event
   */
  public int getNumStationsUsed() {
    return numStationsUsed;
  }

  /**
   * Function to return the number of phases used by the event.
   *
   * @return A int containing the number of phases used by the event
   */
  public int getNumPhasesUsed() {
    return numPhasesUsed;
  }

  /**
   * Function to get the number of projected phases used.
   *
   * @return An int containing the number of projected phases used
   */
  public int getNumProjectedPhasesUsed() {
    return numProjectedPhasesUsed;
  }

  /**
   * Function to get the residuals standard error.
   *
   * @return A double containing the residuals standard error
   */
  public double getResidualsStandardError() {
    return residualsStandardError;
  }

  /**
   * Function to return the event error ellipse.
   *
   * @return An array of EllipseAxis objects containing the event error ellipse
   */
  public EllipseAxis[] getErrorEllipse() {
    return errorEllipse;
  }

  /**
   * Function to get the Bayesian depth data importance.
   *
   * @return A double containing the Bayesian depth data importance.
   */
  public double getBayesianDepthDataImportance() {
    return bayesianDepthDataImportance;
  }

  /**
   * Function to retrive the ArrayList of HypoAudit objects containing the audit history for this
   * event.
   *
   * @return An ArrayList of HypoAudit objects containing the audit history for this event
   */
  public ArrayList<HypoAudit> getHypoAuditList() {
    return hypoAuditList;
  }

  /**
   * Function to return the event list of pick groups.
   *
   * @return An ArrayList of PickGroup objects containing the pick groups
   */
  public ArrayList<PickGroup> getPickGroupList() {
    return pickGroupList;
  }

  /**
   * Function to return the event list of raw weighted residuals.
   *
   * @return An ArrayList of WeightedResidual objects containing the raw weighted residuals
   */
  public ArrayList<WeightedResidual> getRawWeightedResiduals() {
    return rawWeightedResiduals;
  }

  /**
   * Function to return the event list of original weighted residuals.
   *
   * @return An ArrayList of WeightedResidual objects containing the original weighted residuals
   */
  public ArrayList<WeightedResidual> getOriginalWeightedResiduals() {
    return originalWeightedResiduals;
  }

  /**
   * Function to return the event list of projected weighted residuals.
   *
   * @return An ArrayList of WeightedResidual objects containing the projected weighted residuals
   */
  public ArrayList<WeightedResidual> getProjectedWeightedResiduals() {
    return projectedWeightedResiduals;
  }

  /**
   * Function to retrieve the event raw rank-sum estimator.
   *
   * @return A RankSumEstimator object containing the event raw rank-sum estimator
   */
  public RankSumEstimator getRawRankSumEstimator() {
    return rawRankSumEstimator;
  }

  /**
   * Function to retrieve the event projected rank-sum estimator.
   *
   * @return A RankSumEstimator object containing the event projected rank-sum estimator
   */
  public RankSumEstimator getProjectedRankSumEstimator() {
    return projectedRankSumEstimator;
  }

  /**
   * Function to get the Decorrelator object used when decorrelating the event picks.
   *
   * @return A double containing Decorrelator object used when decorrelating the event picks.
   */
  public Decorrelator getDecorrelator() {
    return decorrelator;
  }

  /**
   * Retrieves the number of stations used by this event.
   *
   * @return An int containing the number of stations.
   */
  public int getNumStations() {
    return pickGroupList.size();
  }

  /**
   * Function to set whether the event phase ID's have changed.
   *
   * @param hasPhaseIdChanged A boolean flag indicating whether the event phase ID's have changed
   */
  public void setHasPhaseIdChanged(boolean hasPhaseIdChanged) {
    this.hasPhaseIdChanged = hasPhaseIdChanged;
  }

  /**
   * Function to set the number of projected phases used.
   *
   * @param numProjectedPhasesUsed An int containing the number of projected phases used
   */
  public void setNumProjectedPhasesUsed(int numProjectedPhasesUsed) {
    this.numProjectedPhasesUsed = numProjectedPhasesUsed;
  }

  /**
   * Function to set the time standard error.
   *
   * @param timeStandardError A double containing the time standard error
   */
  public void setTimeStandardError(double timeStandardError) {
    this.timeStandardError = timeStandardError;
  }

  /**
   * Function to set the latitude standard error.
   *
   * @param latitudeStandardError A double containing the latitude standard error
   */
  public void setLatitudeStandardError(double latitudeStandardError) {
    this.latitudeStandardError = latitudeStandardError;
  }

  /**
   * Function to set the longitude standard error.
   *
   * @param longitudeStandardError A double containing the longitude standard error
   */
  public void setLongitudeStandardError(double longitudeStandardError) {
    this.longitudeStandardError = longitudeStandardError;
  }

  /**
   * Function to set the depth standard error.
   *
   * @param depthStandardError A double containing the depth standard error
   */
  public void setDepthStandardError(double depthStandardError) {
    this.depthStandardError = depthStandardError;
  }

  /**
   * Function to set the residuals standard error.
   *
   * @param residualsStandardError A double containing the residuals standard error
   */
  public void setResidualsStandardError(double residualsStandardError) {
    this.residualsStandardError = residualsStandardError;
  }

  /**
   * Function to set the event error ellipse.
   *
   * @param errorEllipse - An array of EllipseAxis objects containing the error ellipse
   */
  public void setErrorEllipse(EllipseAxis[] errorEllipse) {
    this.errorEllipse = errorEllipse;
  }

  /**
   * Function to set the equivalent radius of the error ellipse in kilometers.
   *
   * @param equivalentErrorRadius A double containing the equivalent radius of the error ellipse in
   *     kilometers.
   */
  public void setEquivalentErrorRadius(double equivalentErrorRadius) {
    this.equivalentErrorRadius = equivalentErrorRadius;
  }

  /**
   * Function to set the Bayesian depth data importance.
   *
   * @param bayesianDepthDataImportance A double containing the Bayesian depth data importance.
   */
  public void setBayesianDepthDataImportance(double bayesianDepthDataImportance) {
    this.bayesianDepthDataImportance = bayesianDepthDataImportance;
  }

  /**
   * The Event constructor. This constructor allocate some storage and initializes the earth model
   * to the provided vaue.
   *
   * @param earthModel A String containing the name of earth model to be used
   */
  public Event(String earthModel) {
    this.earthModel = earthModel;
    id = null;
    stationList = new TreeMap<StationID, Station>();
    pickGroupList = new ArrayList<PickGroup>();
    pickList = new ArrayList<gov.usgs.locator.Pick>();
    hypoAuditList = new ArrayList<HypoAudit>();
    rawWeightedResiduals = new ArrayList<WeightedResidual>();
    rawRankSumEstimator = new RankSumEstimator(rawWeightedResiduals);
  }

  /**
   * Event input function. This function takes location input information and populates this event
   * object.
   *
   * @param in A LocInput object containing location input information
   */
  public void input(LocInput in) {
    // Create the hypocenter.
    id = in.getID();
    hypo =
        new Hypocenter(
            LocUtil.toHydraTime(in.getSourceOriginTime().getTime()),
            in.getSourceLatitude(),
            in.getSourceLongitude(),
            in.getSourceDepth());

    // Get the analyst commands.
    isLocationHeld = in.getIsLocationHeld();
    isDepthHeld = in.getIsDepthHeld();
    isDepthManual = in.getIsBayesianDepth();

    if (isDepthManual) {
      bayesianDepth = in.getBayesianDepth();
      bayesianDepthSpread = in.getBayesianSpread();
    }

    useDecorrelation = in.getUseSVD(); // True when noSvd is false
    isLocationRestarted = in.getIsLocationNew();

    // process the input pick data.
    for (int j = 0; j < in.getInputData().size(); j++) {
      gov.usgs.processingformats.Pick pickIn = in.getInputData().get(j);

      // make sure phCode and obsCode are not null
      String phCode = "";
      if (pickIn.getPickedPhase() != null) {
        phCode = pickIn.getPickedPhase();
      }
      String obsCode = "";
      if (pickIn.getAssociatedPhase() != null) {
        obsCode = pickIn.getAssociatedPhase();
      }

      // source type conversion
      int authorType = 1; // default to automatic contributed
      String typeString = pickIn.getSource().getType();
      if ("ContributedAutomatic".equals(typeString)) {
        authorType = 1; // automatic contributed
      } else if ("LocalAutomatic".equals(typeString)) {
        authorType = 2; // automatic NEIC
      } else if ("ContributedHuman".equals(typeString)) {
        authorType = 3; // analyst contributed
      } else if ("LocalHuman".equals(typeString)) {
        authorType = 4; // NEIC analyst
      }

      // source conversion
      String sourceStr = pickIn.getSource().getAgencyID() + "|" + pickIn.getSource().getAuthor();

      // Create the station.
      StationID stationID =
          new StationID(
              pickIn.getSite().getStation(),
              pickIn.getSite().getLocation(),
              pickIn.getSite().getNetwork());
      Station station =
          new Station(
              stationID,
              pickIn.getSite().getLatitude(),
              pickIn.getSite().getLongitude(),
              pickIn.getSite().getElevation());
      gov.usgs.locator.Pick pick =
          new gov.usgs.locator.Pick(
              station,
              pickIn.getSite().getChannel(),
              LocUtil.toHydraTime(pickIn.getTime().getTime()),
              pickIn.getUse(),
              phCode);
      pick.setPhaseIdInfo(
          sourceStr,
          pickIn.getID(),
          pickIn.getQuality(),
          obsCode,
          LocUtil.getAuthCodeFromNumericCode(authorType),
          pickIn.getAffinity());
      pickList.add(pick);
    }

    // Take care of some event initialization.
    initEvent();
  }

  /**
   * Event output function. This function populates a LocOutput object from the location results
   * stored within this event
   *
   * @return A LocOutput object containing Location output information
   */
  public LocOutput output() {
    LocOutput out =
        new LocOutput(
            id,
            LocUtil.toJavaTime(hypo.getOriginTime()),
            hypo.getLatitude(),
            hypo.getLongitude(),
            hypo.getDepth(),
            numStationsAssociated,
            numPhasesAssociated,
            numStationsUsed,
            numPhasesUsed,
            azimuthalGap,
            azimuthalGapLEst,
            minStationDistance,
            qualityFlags);
    out.addErrors(
        timeStandardError,
        latitudeStandardError,
        longitudeStandardError,
        depthStandardError,
        residualsStandardError,
        maxHorizontalError,
        maxVerticalError,
        equivalentErrorRadius,
        hypo.getBayesianDepth(),
        hypo.getBayesianDepthSpread(),
        bayesianDepthDataImportance,
        errorEllipse,
        locatorExitCode);

    // Sort the pick groups (stations) by distance.
    pickGroupList.sort(new PickGroupComp());

    // Pack up the picks.
    for (int i = 0; i < pickGroupList.size(); i++) {
      PickGroup group = pickGroupList.get(i);

      // for each pick in the group
      for (int j = 0; j < group.getNumPicks(); j++) {
        gov.usgs.locator.Pick pick = group.getPicks().get(j);
        StationID stationID = pick.getStation().getStationID();

        out.addPick(
            pick.getSourceID(),
            pick.getOriginalAuthorType(),
            pick.getPickID(),
            stationID.getStationCode(),
            pick.getChannelCode(),
            stationID.getNetworkCode(),
            stationID.getLocationCode(),
            pick.getStation().getLatitude(),
            pick.getStation().getLongitude(),
            pick.getStation().getElevation(),
            LocUtil.toJavaTime(pick.getArrivalTime()),
            pick.getCurrentPhaseCode(),
            pick.getOriginalPhaseCode(),
            pick.getResidual(),
            group.getDistance(),
            group.getAzimuth(),
            pick.getWeight(),
            pick.getImportance(),
            pick.getIsUsed(),
            pick.getOriginalPhaseAffinity(),
            pick.getQuality());
      }
    }

    return out;
  }

  /**
   * This function initializes the event commands, hasPhaseIdChanged flag, pick and station counts,
   * etc., and compute distances and azimuths for the picks. This routine needs to be called for any
   * new event, no matter how it's created.
   */
  private void initEvent() {
    // If the location is held, it can't be moved by the Locator, but
    // errors will be computed as though it was free to provide a
    // meaningful comparison with the NEIC location.  For this reason,
    // it makes sense to have a held location with a held depth.
    // Either way, a Bayesian depth is simulated, again for error
    // estimation reasons.  The free depth spread assumes that the held
    // location is from a crustal event located by a regional network.
    if (isLocationHeld) {
      isDepthManual = true;
      bayesianDepth = hypo.getDepth();

      if (isDepthHeld) {
        bayesianDepthSpread = LocUtil.HELDDEPTHSE;
      } else {
        bayesianDepthSpread = LocUtil.DEFAULTDEPTHSE;
      }
    } else if (isDepthHeld) {
      // Although a held depth will actually hold the depth, simulate a
      // Bayesian depth for error computation reasons.
      isDepthManual = true;
      bayesianDepth = hypo.getDepth();
      bayesianDepthSpread = LocUtil.HELDDEPTHSE;
    }

    // Treat analyst and simulated Bayesian depth commands the same.
    // Set the starting depth to the Bayesian depth, but don't let
    // the analysts get carried away and try to set the Bayesian
    // spread smaller than the default for a held depth.
    if (isDepthManual) {
      if (bayesianDepthSpread > 0d) {
        bayesianDepthSpread = Math.max(bayesianDepthSpread, LocUtil.HELDDEPTHSE);
        hypo.addBayes(bayesianDepth, bayesianDepthSpread);
      } else {
        isDepthManual = false; // Trap a bad command
      }
    }

    // If we're decorrelating, instantiate some more classes.
    if (useDecorrelation) {
      projectedWeightedResiduals = new ArrayList<WeightedResidual>();
      projectedRankSumEstimator = new RankSumEstimator(projectedWeightedResiduals);
      decorrelator = new Decorrelator(this);
    }

    // Sort the picks into "Hydra" input order.
    pickList.sort(new PickComp());

    // Reorganize the picks into groups from the same station.
    String lastSta = "";
    PickGroup group = null;

    for (int j = 0; j < pickList.size(); j++) {
      gov.usgs.locator.Pick pick = pickList.get(j);
      if (!pick.getStation().getStationID().getStationID().equals(lastSta)) {
        // Remember this station.
        lastSta = pick.getStation().getStationID().getStationID();
        stationList.put(pick.getStation().getStationID(), pick.getStation());

        // Initialize the pick group.
        group = new PickGroup(pick.getStation(), pick);
        pickGroupList.add(group);
      } else {
        group.addPick(pick);
      }
    }

    // Initialize the solution degrees-of-freedom.
    hypo.setDegreesOfFreedom(isDepthHeld);

    // Initialize changed and the depth importance.
    hasPhaseIdChanged = false;
    bayesianDepthDataImportance = 0d;

    // Allocate storage for the error ellipsoid.
    errorEllipse = new EllipseAxis[3];

    // Do the initial station/pick statistics.
    computeStationStats();
    numProjectedPhasesUsed = 0;

    // Do the initial delta-azimuth calculation.
    for (int j = 0; j < pickGroupList.size(); j++) {
      pickGroupList.get(j).updateHypoAndTime(hypo);
    }
  }

  /**
   * This function updates the event parameters when the hypocenter changes.
   *
   * @param originTime A double containing the updated origin time in seconds
   * @param latitude A double containing the updated geographic latitude in degrees
   * @param longitude A double containing the updated geographic longitude in degrees
   * @param depth A double containing the updated depth in kilometers
   */
  public void updateEvent(double originTime, double latitude, double longitude, double depth) {
    // Update the hypocenter.
    hypo.updateHypo(originTime, latitude, longitude, depth);

    // Update the picks.
    for (int j = 0; j < pickGroupList.size(); j++) {
      pickGroupList.get(j).updateHypoAndTime(hypo);
    }
  }

  /**
   * This function updates the event parameters when the hypocenter changes based on a given
   * linearized step.
   *
   * @param stepLen A double containuing the step length in kilometers
   * @param timeShift A double containing the shift in the origin time in seconds
   */
  public void updateHypo(double stepLen, double timeShift) {
    // Update the hypocenter.
    hypo.updateHypo(stepLen, timeShift);

    // Update the picks.
    for (int j = 0; j < pickGroupList.size(); j++) {
      pickGroupList.get(j).updateHypoAndTime(hypo);
    }
  }

  /**
   * This function updates the event origin time based on a given time shift.
   *
   * @param timeShift A double containing the shift in the origin time in seconds
   */
  public void updateOriginTime(double timeShift) {
    // update the origin time
    hypo.updateOriginTime(timeShift);

    // Update the picks.
    // Note that we don't need to recompute the distance and azimuth.
    for (int j = 0; j < pickGroupList.size(); j++) {
      pickGroupList.get(j).updateTime(hypo);
    }
  }

  /**
   * This function adds a hypocenter audit record. These double as fall-back hypocenters in case the
   * solution gets worse.
   *
   * @param stage An int containing the audit stage
   * @param iteration An int containing the iteration in this stage
   * @param status A LocStatus object containing the status at this audit stage
   */
  public void addAudit(int stage, int iteration, LocStatus status) {
    if (LocUtil.useDecorrelation) {
      hypoAuditList.add(new HypoAudit(hypo, stage, iteration, numProjectedPhasesUsed, status));
    } else {
      hypoAuditList.add(new HypoAudit(hypo, stage, iteration, numPhasesUsed, status));
    }
  }

  /**
   * This function makes a shallow copy of of the raw weighted residuals stored as
   * originalWeightedResiduals so that the original order is preserved for the decorrelation
   * projection.
   */
  @SuppressWarnings("unchecked")
  public void saveWeightedResiduals() {
    if (useDecorrelation) {
      originalWeightedResiduals = (ArrayList<WeightedResidual>) rawWeightedResiduals.clone();
    }
  }

  /** This function resets all the triage flags for when triage needs to be repeated. */
  public void resetTriage() {
    for (int j = 0; j < pickList.size(); j++) {
      pickList.get(j).setIsTriage(false);
    }
  }

  /**
   * This function generates the station statistics such as counting the number of stations and
   * picks and finding the distance to the closest station.
   */
  public void computeStationStats() {
    numStationsAssociated = stationList.size();
    numStationsUsed = 0;
    numPhasesAssociated = 0;
    numPhasesUsed = 0;
    numLocalPhasesUsed = 0;
    minStationDistance = TauUtil.DMAX;

    for (int j = 0; j < pickGroupList.size(); j++) {
      PickGroup group = pickGroupList.get(j);
      numPhasesAssociated += group.getNumPicks();
      int picksUsedGrp = group.getNumPicksUsed();
      numPhasesUsed += picksUsedGrp;

      if (group.getDistance() <= LocUtil.GT5LOCALDISTMAX) {
        numLocalPhasesUsed += picksUsedGrp;
      }

      if (picksUsedGrp > 0) {
        numStationsUsed++;
        minStationDistance = Math.min(minStationDistance, group.getDistance());
      }
    }
  }

  /** This function computes the azimuthal gap and robust (L-estimator) azimuthal gap in degrees. */
  public void computeAzimuthalGap() {
    // Trap a bad call.
    if (numStationsUsed == 0) {
      azimuthalGap = 360d;
      azimuthalGapLEst = 360d;
      return;
    }

    // Collect and sort the azimuths.
    double[] azimuths = new double[numStationsUsed];
    int i = 0;
    for (int j = 0; j < pickGroupList.size(); j++) {
      if (pickGroupList.get(j).getNumPicksUsed() > 0) {
        azimuths[i++] = pickGroupList.get(j).getAzimuth();
      }
    }
    Arrays.sort(azimuths);

    // Do the azimuthal gap.
    azimuthalGap = 0d;
    double lastAzimuth = azimuths[azimuths.length - 1] - 360d;
    for (int j = 0; j < azimuths.length; j++) {
      azimuthalGap = Math.max(azimuthalGap, azimuths[j] - lastAzimuth);
      lastAzimuth = azimuths[j];
    }

    // Do the robust azimuthal gap.
    if (numStationsUsed == 1) {
      azimuthalGapLEst = 360d;
    } else {
      lastAzimuth = azimuths[azimuths.length - 2] - 360d;
      azimuthalGapLEst = azimuths[0] - lastAzimuth;
      lastAzimuth = azimuths[azimuths.length - 1] - 360d;
      for (int j = 1; j < azimuths.length; j++) {
        azimuthalGapLEst = Math.max(azimuthalGapLEst, azimuths[j] - lastAzimuth);
        lastAzimuth = azimuths[j - 1];
      }
    }
  }

  /**
   * This function sets the traditional NEIC qualityFlags flags. The summary flag uses the algorithm
   * of Buland and Presgrave. The secondary flags break down the qualityFlags by epicenter and
   * depth.
   *
   * @param status A LocStatus object containing the event status
   */
  public void setQualityFlags(LocStatus status) {
    // If there is insufficient data, the qualityFlags can only be "D".
    if (status == LocStatus.INSUFFICIENT_DATA) {
      qualityFlags = "D  ";
    } else {
      char summary = ' ';

      // If this is a GT5 event, the summary is done.
      if (LocUtil.isGT5(numLocalPhasesUsed, minStationDistance, azimuthalGap, azimuthalGapLEst)) {
        summary = 'G';
      } else {
        // Otherwise, set the summary qualityFlags based on the errors.
        if ((equivalentErrorRadius <= LocUtil.HORIZONTALQUALIMITS[0])
            && (depthStandardError <= LocUtil.VERTICALQUALIMITS[0])
            && (numPhasesUsed > LocUtil.NPHASESQUALIMITS[0])) {
          summary = 'A';
        } else if ((equivalentErrorRadius <= LocUtil.HORIZONTALQUALIMITS[1])
            && (depthStandardError <= LocUtil.VERTICALQUALIMITS[1])
            && (numPhasesUsed > LocUtil.NPHASESQUALIMITS[1])) {
          summary = 'B';
        } else if ((equivalentErrorRadius <= LocUtil.HORIZONTALQUALIMITS[2])
            && (depthStandardError <= LocUtil.VERTICALQUALIMITS[2])) {
          summary = 'C';
        } else {
          summary = 'D';
        }

        // Revise the summary flag down if the error ellipse aspect ration is
        // large.
        if ((summary == 'A') && (errorEllipse[0].getSemiLen() > LocUtil.SEMILENQUALIMITS[0])) {
          summary = 'B';
        }
        if (((summary == 'A') || (summary == 'B'))
            && (errorEllipse[0].getSemiLen() > LocUtil.SEMILENQUALIMITS[1])) {
          summary = 'C';
        }
        if (errorEllipse[0].getSemiLen() > LocUtil.SEMILENQUALIMITS[2]) {
          summary = 'D';
        }
      }

      // Set the epicenter flag based on equivalentErrorRadius.
      char epicenter = '?';
      if ((equivalentErrorRadius <= LocUtil.HORIZONTALQUALIMITS[0])
          && (numPhasesUsed > LocUtil.NPHASESQUALIMITS[0])) {
        epicenter = ' ';
      } else if ((equivalentErrorRadius <= LocUtil.HORIZONTALQUALIMITS[1])
          && (numPhasesUsed > LocUtil.NPHASESQUALIMITS[1])) {
        epicenter = '*';
      } else if (equivalentErrorRadius <= LocUtil.HORIZONTALQUALIMITS[2]) {
        epicenter = '?';
      } else {
        summary = '!';
      }

      // Set the depth flag based on depthStandardError.
      char depth = ' ';
      if (isDepthHeld) {
        depth = 'G';
      } else {
        if ((depthStandardError <= LocUtil.VERTICALQUALIMITS[0])
            && (numPhasesUsed > LocUtil.NPHASESQUALIMITS[0])) {
          depth = ' ';
        } else if ((depthStandardError <= LocUtil.VERTICALQUALIMITS[1])
            && (numPhasesUsed > LocUtil.NPHASESQUALIMITS[1])) {
          depth = '*';
        } else if (depthStandardError <= LocUtil.VERTICALQUALIMITS[2]) {
          depth = '?';
        } else {
          depth = '!';
        }
      }

      // build quality flags string
      qualityFlags = "" + summary + epicenter + depth;
    }
  }

  /**
   * This function computes the maximum tangential (horizontal) and vertical (depth) projections of
   * the error ellipsoid in kilometers. While not statistically valid, these measures are commonly
   * used by the regional networks.
   */
  public void computeSummaryErrors() {
    maxHorizontalError = 0d;
    maxVerticalError = 0d;

    for (int j = 0; j < errorEllipse.length; j++) {
      maxHorizontalError = Math.max(maxHorizontalError, errorEllipse[j].tangentialProj());
      maxVerticalError = Math.max(maxVerticalError, errorEllipse[j].verticalProj());
    }
  }

  /**
   * This function zeros out most errors when no solution is possible.
   *
   * @param all A boolean flag indicating whether to also zero out the residual standard error
   */
  public void zeroOutStatistics(boolean all) {
    timeStandardError = 0d;
    latitudeStandardError = 0d;
    longitudeStandardError = 0d;
    depthStandardError = 0d;
    maxHorizontalError = 0d;
    maxVerticalError = 0d;
    equivalentErrorRadius = 0d;

    for (int j = 0; j < errorEllipse.length; j++) {
      errorEllipse[j] = new EllipseAxis(0d, 0d, 0d);
    }

    if (all) {
      residualsStandardError = 0d;
    }
  }

  /**
   * This function zeros out all data importances (and weights) if the importances cannot be
   * computed.
   */
  public void zeroOutWeights() {
    hypo.setBayesianDepthWeight(0d);

    for (int j = 0; j < pickList.size(); j++) {
      pickList.get(j).setWeight(0d);
    }
  }

  /**
   * This function sets the location exit status from the more detailed internal status flag.
   *
   * @param status A LocStatus object containing the final status from locator
   */
  public void setLocatorExitCode(LocStatus status) {
    // Set the exit status.
    switch (status) {
      case SUCCESS:
      case NEARLY_CONVERGED:
      case UNSTABLE_SOLUTION:
        if ((hypo.getHorizontalStepLength() > LocUtil.DISTANCETOLERANCE)
            || (hypo.getVerticalStepLength() > LocUtil.DEPTHTOLERANCE)) {
          locatorExitCode = LocStatus.SUCCESSFUL_LOCATION;
        } else {
          locatorExitCode = LocStatus.DID_NOT_MOVE;
        }
        break;

      case SINGULAR_MATRIX:
      case ELLIPSOID_FAILED:
        locatorExitCode = LocStatus.ERRORS_NOT_COMPUTED;
        break;

      case DID_NOT_CONVERGE:
        locatorExitCode = LocStatus.DID_NOT_CONVERGE;
        break;

      case INSUFFICIENT_DATA:
      case BAD_DEPTH:
        locatorExitCode = LocStatus.LOCATION_FAILED;
        break;

      default:
        locatorExitCode = LocStatus.UNKNOWN_STATUS;
        break;
    }
  }

  /**
   * This function writes the station list to a string.
   *
   * @return A String containing the station list
   */
  public String printStationList() {
    String listString = "No stations found.";

    if (stationList.size() > 0) {
      StationID maxID = new StationID("~", "", "");
      NavigableMap<StationID, Station> map = stationList.headMap(maxID, true);
      listString = "Station List:";

      for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
        Station sta = (Station) entry.getValue();
        listString += sta + "\n";
      }
    }

    return listString;
  }

  /**
   * Function to write the arrivals associated with this event in a nicely formatted string.
   *
   * @param first A boolean flag, if true only print the first arrival in each pick group
   * @return A String containing the picks
   */
  public String printPicks(boolean first) {
    String pickString = "";

    for (int j = 0; j < pickGroupList.size(); j++) {
      pickString += pickGroupList.get(j).printPicks(first);
    }

    return pickString;
  }

  /**
   * This function converts the weighted residual storage to a string.
   *
   * @param type A String identifying the weighted residual storage desired ("Raw" for the sorted
   *     picks, "Proj" for the projected picks, and "Org" for the unsorted picks)
   * @param full A boolean flag, if true, print the derivatives as well
   * @return A String containing the formatted hydra input.
   */
  public String printWeightedResiduals(String type, boolean full) {
    String weiResString = "";

    if ("Raw".equals(type)) {
      weiResString += "RawWeightedResiduals:\n";

      for (int j = 0; j < rawWeightedResiduals.size(); j++) {
        weiResString += String.format("%4d ", j);
        weiResString += rawWeightedResiduals.get(j).printWeightedResiduals(full);
      }
    } else if ("Proj".equals(type)) {
      weiResString += "ProjectedWeightedResiduals:\n";

      for (int j = 0; j < projectedWeightedResiduals.size(); j++) {
        weiResString += String.format("%4d ", j);
        weiResString += projectedWeightedResiduals.get(j).printWeightedResiduals(full);
      }
    } else {
      weiResString += "OriginalWeightedResiduals:\n";

      for (int j = 0; j < originalWeightedResiduals.size(); j++) {
        weiResString += String.format("%4d ", j);
        weiResString += originalWeightedResiduals.get(j).printWeightedResiduals(full);
      }
    }

    return weiResString;
  }

  /**
   * This function generates a string containing all the audit records.
   *
   * @return A String containing the audit records.
   */
  public String printHypoAudit() {
    String auditString = "";

    for (int j = 0; j < hypoAuditList.size(); j++) {
      auditString += hypoAuditList.get(j).toString() + "\n";
    }

    return auditString;
  }

  /**
   * This function converts the input event information to a string formatted similarly to the
   * "Hydra" event input file for debugging.
   *
   * @param humanReadable If true make date-times and booleans readable for a human
   * @return A String containing the formatted hydra input.
   */
  public String getHydraInput(boolean humanReadable) {
    String hydraInput = "";
    if (humanReadable) {
      hydraInput +=
          String.format(
              "%22s %8.4f %9.4f %6.2f %5b %5b %5b " + "%5.1f %5.1f %5b\n",
              LocUtil.getDateTimeString(hypo.getOriginTime()),
              hypo.getLatitude(),
              hypo.getLongitude(),
              hypo.getDepth(),
              isLocationHeld,
              isDepthHeld,
              isDepthManual,
              hypo.getBayesianDepth(),
              hypo.getBayesianDepthSpread(),
              useDecorrelation);
      hydraInput += "\n";
    } else {
      hydraInput +=
          String.format(
              "%14.3f %8.4f %9.4f %6.2f %c %c %c " + "%5.1f %5.1f %c \n",
              hypo.getOriginTime(),
              hypo.getLatitude(),
              hypo.getLongitude(),
              hypo.getDepth(),
              LocUtil.getBoolChar(isLocationHeld),
              LocUtil.getBoolChar(isDepthHeld),
              LocUtil.getBoolChar(isDepthManual),
              !Double.isNaN(hypo.getBayesianDepth()) ? hypo.getBayesianDepth() : 0d,
              !Double.isNaN(hypo.getBayesianDepthSpread()) ? hypo.getBayesianDepthSpread() : 0d,
              LocUtil.getBoolChar(!useDecorrelation));
    }

    for (int j = 0; j < pickGroupList.size(); j++) {
      hydraInput += pickGroupList.get(j).getInputPicks(humanReadable);
    }

    return hydraInput;
  }

  /**
   * This function converts the output event information to a string formatted similarly to the
   * "Hydra" event output file for debugging.
   *
   * @return A String containing the formatted hydra output.
   */
  public String getHydraOutput() {
    String hydraOutput = "";
    hydraOutput +=
        String.format(
            "%14.3f %8.4f %9.4f %6.2f %4d %4d %4d %4d %3.0f %8.4f\n",
            hypo.getOriginTime(),
            hypo.getLatitude(),
            hypo.getLongitude(),
            hypo.getDepth(),
            numStationsAssociated,
            numPhasesAssociated,
            numStationsUsed,
            numPhasesUsed,
            azimuthalGap,
            minStationDistance);
    hydraOutput +=
        String.format(
            "%6.2f %6.1f %6.1f %6.1f %6.2f %6.1f %6.1f %6.1f %3s %5.1f %5.1f %6.4f\n",
            timeStandardError,
            latitudeStandardError,
            longitudeStandardError,
            depthStandardError,
            residualsStandardError,
            maxHorizontalError,
            maxVerticalError,
            equivalentErrorRadius,
            qualityFlags,
            hypo.getBayesianDepth(),
            hypo.getBayesianDepthSpread(),
            bayesianDepthDataImportance);
    hydraOutput +=
        String.format(
            "%14s %14s %14s  %3.0f\n\n",
            errorEllipse[0], errorEllipse[1], errorEllipse[2], azimuthalGapLEst);

    for (int j = 0; j < pickGroupList.size(); j++) {
      hydraOutput += pickGroupList.get(j).getOutputPicks();
    }

    return hydraOutput;
  }

  /**
   * This function converts the output event information to a string formatted similarly to the NEIC
   * style web output for debugging.
   *
   * @return A String containing the formatted NEIC output.
   */
  public String getNEICOutput() {
    String neicOutput = "";
    // Print the hypocenter.
    neicOutput +=
        String.format(
            "Location:             %-7s %-8s ±%6.1f km\n",
            LocUtil.formatLat(hypo.getLatitude()),
            LocUtil.formatLon(hypo.getLongitude()),
            maxHorizontalError);
    neicOutput +=
        String.format("Depth:                %5.1f ±%6.1f km\n", hypo.getDepth(), maxVerticalError);
    neicOutput +=
        String.format(
            "Origin Time:          %23s UTC\n",
            LocUtil.getNEICDateTimeString(hypo.getOriginTime()));
    neicOutput += String.format("Number of Stations:     %4d\n", numStationsAssociated);
    neicOutput += String.format("Number of Phases:       %4d\n", numPhasesAssociated);
    neicOutput += String.format("Minimum Distance:     %6.1f\n", minStationDistance);
    neicOutput += String.format("Travel Time Residual:  %5.2f\n", timeStandardError);
    neicOutput += String.format("Azimuthal Gap:           %3.0f\n", azimuthalGap);
    neicOutput +=
        "\n    Channel     Distance Azimuth Phase     Arrival Time Status    Residual Weight\n";

    // Sort the pick groups by distance.
    pickGroupList.sort(new PickGroupComp());

    // Print the picks.
    for (int j = 0; j < pickGroupList.size(); j++) {
      neicOutput += pickGroupList.get(j).getNEIC();
    }

    return neicOutput;
  }
}
