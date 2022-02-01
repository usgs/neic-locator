package gov.usgs.locator;

import gov.usgs.locaux.AuthorType;
import gov.usgs.locaux.LocUtil;
import gov.usgs.traveltime.TTimeData;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The Pick class stores the data making up one pick.
 *
 * @author Ray Buland
 */
public class Pick implements Comparable<Pick> {
  /** A String containing the pick ID. */
  private String pickID;

  /** A String containing the ID of the source. */
  private String sourceID;

  /** A Station object containing station the pick was generated at. */
  private Station station;

  /** A String containing the code for the channel the pick was made on. */
  private String channelCode;

  /** A double containing the pick arrival time in decimal seconds since the epoch. */
  private double arrivalTime;

  /** A double containing the pick quality (standard error) in seconds. */
  private double quality;

  /** A boolean indicating whether the pick may be used in the location (external command). */
  private boolean externalUse;

  /** A String containing the original Assoc phase identification for this pick. */
  private String originalAssocPhaseCode;

  /** A String containing the original picked phase identification for this pick. */
  private String originalPickedPhaseCode;

  /** A String containing the original located phase identification for this pick. */
  private String originalLocPhaseCode;

  /** An AuthorType object containing the author type for the original phase identification. */
  private AuthorType originalAuthorType;

  /**
   * A double containing the affinity (strength) of the original phase identification. Higher
   * numbers make it harder to re-identify the phase.
   */
  private double originalPhaseAffinity;

  /** A String containing the current phase identification for this pick. */
  private String currentPhaseCode;

  /**
   * A double containing the observed-theoretical arrival time in seconds for the current phase
   * identification.
   */
  private double residual;

  /** A boolean indicating whether the pick is currently used in the location. */
  private boolean isUsed;

  /** A double containing the pick weight in the location for the current phase identification. */
  private double weight;

  /**
   * A double containing the pick data importance to the location for the current phase
   * identification.
   */
  private double importance;

  /** A String containing the best code to use for phase identification. */
  private String bestPhaseCode;

  /** A double containing the current pick travel time in seconds. */
  private double travelTime;

  /** A boolean flag indicating whether the pick was automatically made. */
  private boolean isAutomatic;

  /** A boolean flag indicating this pick was eliminated by the triage method. */
  private boolean isTriage;

  /**
   * A TTimeData object holding the theoretical arrival with the minimum statisticalFoM used in
   * phase identification.
   */
  private TTimeData ttStatisticalMinFoM;

  /** A double containing the current statistical figure-of-merit used in phase identification. */
  private double statisticalFoM;

  /** A boolean flag indicating the association should be forced during phase identification. */
  private boolean forceAssociation;

  /**
   * A boolean flag indicating this pick is a surface wave. Surface wave phases can't be
   * re-identified.
   */
  private boolean isSurfaceWave;

  /** A WeightedResidual object containing the weighted residual for this picks. */
  private WeightedResidual weightedResidual;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(Pick.class.getName());

  /**
   * Function to return the pick identifier.
   *
   * @return A String containing the pick identifier
   */
  public String getPickID() {
    return pickID;
  }

  /**
   * Function to return the pick source identifier.
   *
   * @return A String containing the pick source identifier
   */
  public String getSourceID() {
    return sourceID;
  }

  /**
   * Function to return the pick station.
   *
   * @return A Station containing the pick station
   */
  public Station getStation() {
    return station;
  }

  /**
   * Function to return the pick channel code.
   *
   * @return A String containing the pick channel code
   */
  public String getChannelCode() {
    return channelCode;
  }

  /**
   * Function to get the pick arrival time.
   *
   * @return A double containing the pick arrival time in decimal seconds since the epoch.
   */
  public double getArrivalTime() {
    return arrivalTime;
  }

  /**
   * Function to get the pick quality.
   *
   * @return A double containing the pick quality
   */
  public double getQuality() {
    return quality;
  }

  /**
   * Function to get the pick external use flag.
   *
   * @return A boolean indicating whether the pick should be used or not (external) command
   */
  public boolean getExternalUse() {
    return externalUse;
  }

  /**
   * Function to return the original assoc phase code for the pick.
   *
   * @return A String containing the original assoc phase code for the pick
   */
  public String getOriginalAssocPhaseCode() {
    return originalAssocPhaseCode;
  }

  /**
   * Function to return the original phase code for the pick.
   *
   * @return A String containing the original pick phase code for the pick
   */
  public String getOriginalPickedPhaseCode() {
    return originalPickedPhaseCode;
  }

  /**
   * Function to return the original loc phase code for the pick.
   *
   * @return A String containing the original loc pick phase code for the pick
   */
  public String getOriginalLocPhaseCode() {
    return originalLocPhaseCode;
  }

  /**
   * Function to get the pick author type.
   *
   * @return A AuthorType object holding the pick author type
   */
  public AuthorType getOriginalAuthorType() {
    return originalAuthorType;
  }

  /**
   * Function to get the original phase affinity.
   *
   * @return A double containing the affinity (strength) of the original phase identification.
   *     Higher numbers make it harder to re-identify the phase.
   */
  public double getOriginalPhaseAffinity() {
    return originalPhaseAffinity;
  }

  /**
   * Function to return the current phase code for the pick.
   *
   * @return A String containing the current phase code for the pick
   */
  public String getCurrentPhaseCode() {
    return currentPhaseCode;
  }

  /**
   * Function to get the observed-theoretical arrival time in seconds.
   *
   * @return A double containing the observed-theoretical arrival time in seconds for the current
   *     phase identification
   */
  public double getResidual() {
    return residual;
  }

  /**
   * Function to get whether the pick is currently used.
   *
   * @return A boolean indicating whether the pick is currently used or not
   */
  public boolean getIsUsed() {
    return isUsed;
  }

  /**
   * Function to get the pick weight in the location.
   *
   * @return A double containing the pick weight in the location for the current phase
   *     identification
   */
  public double getWeight() {
    return weight;
  }

  /**
   * Function to get the pick data importance to the location.
   *
   * @return A double containing the pick data importance to the location for the current phase
   *     identification
   */
  public double getImportance() {
    return importance;
  }

  /**
   * Function to return the best phase code for the pick.
   *
   * @return A String containing the best phase code for the pick
   */
  public String getBestPhaseCode() {
    return bestPhaseCode;
  }

  /**
   * Function to get the current pick travel time in seconds.
   *
   * @return A double containing the current pick travel time in seconds
   */
  public double getTravelTime() {
    return travelTime;
  }

  /**
   * Function to get whether the pick was automatically made.
   *
   * @return A boolean flag indicating whether the pick was automatically made
   */
  public boolean getIsAutomatic() {
    return isAutomatic;
  }

  /**
   * Function to get whether the pick was eliminated by the triage method.
   *
   * @return A boolean flag indicating whether the pick was eliminated by the triage method
   */
  public boolean getIsTriage() {
    return isTriage;
  }

  /**
   * Function to get the theoretical arrival object with the minimum statisticalFoM.
   *
   * @return A TTimeData object holding the theoretical arrival with the minimum statisticalFoM
   */
  public TTimeData getTTStatisticalMinFoM() {
    return ttStatisticalMinFoM;
  }

  /**
   * Function to get the current statistical figure-of-merit.
   *
   * @return A double containing the current statistical figure-of-merit
   */
  public double getStatisticalFoM() {
    return statisticalFoM;
  }

  /**
   * Function to get whether this pick is a surface wave. Surface wave phases can't be re-identified
   *
   * @return A boolean flag indicating whether this pick is a surface wave.
   */
  public boolean getIsSurfaceWave() {
    return isSurfaceWave;
  }

  /**
   * Function to set the observed-theoretical arrival time in seconds.
   *
   * @param residual A double containing the observed-theoretical arrival time in seconds for the
   *     current phase identification
   */
  public void setResidual(double residual) {
    this.residual = residual;
  }

  /**
   * Function to set whether the pick is currently used.
   *
   * @param isUsed A boolean indicating whether the pick is currently used or not
   */
  public void setIsUsed(boolean isUsed) {
    this.isUsed = isUsed;
  }

  /**
   * Function to set the pick weight in the location.
   *
   * @param weight A double containing the pick weight in the location for the current phase
   *     identification
   */
  public void setWeight(double weight) {
    this.weight = weight;
  }

  /**
   * Function to set the pick data importance to the location.
   *
   * @param importance A double containing the pick data importance to the location for the current
   *     phase identification
   */
  public void setImportance(double importance) {
    this.importance = importance;
  }

  /**
   * Function to set whether the pick was eliminated by the triage method.
   *
   * @param isTriage A boolean flag indicating whether the pick was eliminated by the triage method
   */
  public void setIsTriage(boolean isTriage) {
    this.isTriage = isTriage;
  }

  /**
   * Function to set whether the association should be forced.
   *
   * @param forceAssociation A boolean flag indicating whether the association should be forced.
   */
  public void setForceAssociation(boolean forceAssociation) {
    this.forceAssociation = forceAssociation;
  }

  /**
   * Function to set the theoretical arrival object with the minimum statisticalFoM.
   *
   * @param ttStatisticalMinFoM A TTimeData object holding the theoretical arrival with the minimum
   *     statisticalFoM
   */
  public void setTTStatisticalMinFoM(TTimeData ttStatisticalMinFoM) {
    this.ttStatisticalMinFoM = ttStatisticalMinFoM;
  }

  /**
   * Function to set the current statistical figure-of-merit.
   *
   * @param statisticalFoM A double containing the current statistical figure-of-merit
   */
  public void setStatisticalFoM(double statisticalFoM) {
    this.statisticalFoM = statisticalFoM;
  }

  /**
   * This function sets the statistical figure-of-merit variables.
   *
   * @param statisticalTravelTime A TTimeData object containing the travel-time information
   * @param statisticalFoM A double containing the statistical Figure-of-merit metric
   */
  public void setStatisticalFoM(TTimeData statisticalTravelTime, double statisticalFoM) {
    ttStatisticalMinFoM = statisticalTravelTime;
    this.statisticalFoM = statisticalFoM;
  }

  /**
   * The Pick constructor. This constructor creates the pick with the minimum required provided
   * information.
   *
   * @param station A Station object containing the information on the station the pick was made at.
   * @param channelCode A String containing the pick channel code
   * @param arrivalTime A double holding the pick arrival time in decimal seconds since the epoch
   * @param externalUse A boolean flag indicating whether the is requested to be used externally
   * @param currentPhaseCode A string containing the current locator or associator phase
   *     identification
   */
  public Pick(
      Station station,
      String channelCode,
      double arrivalTime,
      boolean externalUse,
      String currentPhaseCode) {
    // Remember the inputs.
    this.station = station;
    if (channelCode != null) {
      this.channelCode = channelCode;
    } else {
      this.channelCode = "--";
    }
    if (this.channelCode.contentEquals("unknown")) this.channelCode = "--";
    this.arrivalTime = arrivalTime;
    this.externalUse = externalUse;
    this.currentPhaseCode = currentPhaseCode;

    // Set defaults.
    pickID = "0";
    quality = 0d;
    originalAssocPhaseCode = null;
    originalPickedPhaseCode = null;
    originalLocPhaseCode = null;
    originalAuthorType = null;
    originalPhaseAffinity = 3d;
    isUsed = externalUse;
    residual = Double.NaN;
    weight = 0d;
    importance = 0d;

    // Initialize internal variables too.
    bestPhaseCode = currentPhaseCode;
    travelTime = Double.NaN;
    isAutomatic = true;
    isSurfaceWave = false;
    isTriage = false;
    initializeFoM();

    // Create an empty weighted residual.
    weightedResidual = new WeightedResidual();
  }

  /**
   * This function sets additional information to help with phase identification and location.
   *
   * @param sourceID A String identifying the pick source.
   * @param pickID A String containing the pick's external ID
   * @param quality A double containing the pick uncertainty in seconds (not currently used in phase
   *     identification and location)
   * @param originalAssocPhaseCode A String containing the original associator external pick phase
   *     identification
   * @param originalPickedPhaseCode A String containing the original picker external pick phase
   *     identification
   * @param originalLocPhaseCode A String containing the original locator external pick phase
   *     identification
   * @param originalAuthorType An AuthorType object containing the type (e.g., human or auto) of the
   *     original phase identification
   * @param originalPhaseAffinity Higher numbers make it harder to re-identify the phase
   */
  public void setPhaseIdInfo(
      String sourceID,
      String pickID,
      double quality,
      String originalAssocPhaseCode,
      String originalPickedPhaseCode,
      String originalLocPhaseCode,
      AuthorType originalAuthorType,
      double originalPhaseAffinity) {
    this.sourceID = sourceID;
    this.pickID = pickID;
    this.quality = quality;
    this.originalAssocPhaseCode = originalAssocPhaseCode;
    this.originalPickedPhaseCode = originalPickedPhaseCode;
    this.originalLocPhaseCode = originalLocPhaseCode;
    this.originalAuthorType = originalAuthorType;
    this.originalPhaseAffinity = originalAuthorType.affinity(originalPhaseAffinity);

    // Use the enum for the author type.
    switch (originalAuthorType) {
      case LOCAL_HUMAN:
        // for a local human we always trust their phase id
        // we ignore the previous locator phase id
        currentPhaseCode = originalAssocPhaseCode;
        bestPhaseCode = originalAssocPhaseCode;
        isAutomatic = false;
        break;

      case CONTRIB_HUMAN:
      case CONTRIB_AUTO:
      case LOCAL_AUTO:
        // for all other known types, we use the locator phase
        // id if we have it, otherwise we use the associator's
        // phase id if we do not
        if ((originalLocPhaseCode != null) && (originalLocPhaseCode != "")) {
          currentPhaseCode = originalLocPhaseCode;
          bestPhaseCode = originalLocPhaseCode;
        } else {
          currentPhaseCode = originalAssocPhaseCode;
          bestPhaseCode = originalAssocPhaseCode;
        }
        isAutomatic = true;
        break;

      default:
        // if we do no have an author type, we default
        // to what the current phase code was set to
        // in the constructor, which is currently set the
        // by Event.java to be the picker's phase id.
        bestPhaseCode = currentPhaseCode;
        break;
    }

    if (("Lg".equals(bestPhaseCode) || "LR".equals(bestPhaseCode)) && !isAutomatic) {
      isSurfaceWave = true;
    }
  }

  /**
   * This function recomputes the travel time for this pick when the hypocenter is updated.
   *
   * @param hypo A Hypocenter object holding the Hypocenter information
   */
  public void updateTravelTime(Hypocenter hypo) {
    travelTime = arrivalTime - hypo.getOriginTime();
  }

  /**
   * This function updates the phase identification. This abbreviated version is only used for the
   * initial phase identification.
   *
   * @param currentPhaseCode A string containing the new phase code
   */
  public void updatePhaseIdentification(String currentPhaseCode) {
    this.currentPhaseCode = currentPhaseCode;

    if (isAutomatic) {
      bestPhaseCode = currentPhaseCode;
    }
  }

  /**
   * This function updates the phase identification. When the phase is re-identified, we need to
   * update the phase code, residual, derivatives, and optionally the weight.
   *
   * @param isFirstPhase A boolean flag indicating if this is the first phase in a group
   * @param shouldReweight A boolean flag indicating whether weights are to be recomputed
   * @param azimuth A double holding the azimuth of the station from the source in degrees
   * @param weightedResiduals An ArrayList of WeightedResidual objects containing the weighted
   *     residual information used in the Rank-Sum routines.
   * @return True if a used phase has changed identification or is no longer used
   */
  public boolean updatePhaseIdentification(
      boolean isFirstPhase,
      boolean shouldReweight,
      double azimuth,
      ArrayList<WeightedResidual> weightedResiduals) {
    boolean idChanged = false;

    if (ttStatisticalMinFoM != null) {
      // We have an identification.  Set up some key variables.
      String ttCode = ttStatisticalMinFoM.getPhCode();

      boolean reID = false;
      if (!currentPhaseCode.equals(ttCode)) {
        reID = true;
      }

      if (reID) {
        LOGGER.fine(
            String.format(
                "=====> Phase re-ID: %-5s %s -> %s",
                station.getStationID().getStationCode(), currentPhaseCode, ttCode));
      }

      currentPhaseCode = ttCode;

      if (isAutomatic) {
        bestPhaseCode = currentPhaseCode;
      }

      if (!"LR".equals(currentPhaseCode)) {
        residual = travelTime - ttStatisticalMinFoM.getTT();
      } else {
        residual = 0d;
      }

      // If this phase is still being used, set it for processing.
      if (isUsed
          && ttStatisticalMinFoM.canUse()
          && (statisticalFoM <= LocUtil.computeValidityLimit(ttStatisticalMinFoM.getSpread())
              || forceAssociation)) {
        if (shouldReweight) {
          weight = 1d / Math.max(ttStatisticalMinFoM.getSpread(), 0.2d);

          // Check if weight is NaN. If so warn and set to zero.
          // NOTE: This indicates something went wrong in the travel time libraries
          // We should consider investigating
          Double weightCheck = weight;
          if (weightCheck.isNaN()) {
            weight = 0d;
            LOGGER.fine(
                "IDphas: Computed weight from travel time spread is NaN, so set weight 0 and isUsed to false");
            isUsed = false;
          }
        }

        // Add it to weighted residual storage.
        weightedResidual.reInit(
            this,
            residual,
            weight,
            false,
            LocUtil.computeTTLatDerivative(ttStatisticalMinFoM.getDTdD(), azimuth),
            LocUtil.computeTTLonDerivative(ttStatisticalMinFoM.getDTdD(), azimuth),
            ttStatisticalMinFoM.getDTdZ(),
            LocUtil.computeTTLatDerivative(ttStatisticalMinFoM.getDSdD(), azimuth),
            LocUtil.computeTTLonDerivative(ttStatisticalMinFoM.getDSdD(), azimuth));
        weightedResiduals.add(weightedResidual);

        if (reID) {
          idChanged = true;
        }
      } else {
        // Otherwise, see if it was used before.
        if (isUsed) {
          LOGGER.fine(
              String.format(
                  "=====> Phase no use set (wt): %s %s %5b %5.2f",
                  station.getStationID().getStationCode(),
                  currentPhaseCode,
                  ttStatisticalMinFoM.canUse(),
                  ttStatisticalMinFoM.getSpread()));

          isUsed = false;

          // Prevents initially identified first arrivals from coming back.
          if (isFirstPhase) {
            externalUse = false;
          }

          idChanged = true;
        }
        weight = 0d;
      }
    } else {
      // We don't have an identification.
      if (!"".equals(currentPhaseCode)) {
        LOGGER.fine(
            String.format(
                "=====> Phase re-ID: %s %s -> null",
                station.getStationID().getStationCode(), currentPhaseCode));
      }

      // See if it was used before.
      if (isUsed) {
        LOGGER.fine(
            String.format(
                "=====> Phase no use set (no ID): %s %s",
                station.getStationID().getStationCode(), currentPhaseCode));

        isUsed = false;

        // Prevents initially identified first arrivals from coming back.
        if (isFirstPhase) {
          externalUse = false;
        }

        idChanged = true;
      }

      // Close it out.
      currentPhaseCode = "";
      residual = 0d;
      weight = 0d;
    }

    LOGGER.finer(
        String.format(
            "IDphas: %s %s %6.2f %7.4f %b",
            station.getStationID().getStationCode(), currentPhaseCode, residual, weight, isUsed));

    return idChanged;
  }

  /** This function initializes figure-of-merit variables. */
  public void initializeFoM() {
    statisticalFoM = 0d;
    ttStatisticalMinFoM = null;
    forceAssociation = false;
  }

  /** This function converts the pick into a string. */
  @Override
  public String toString() {
    return String.format(
        "%s %s %6.2f %b",
        station.getStationID().getStationCode(), currentPhaseCode, residual, isUsed);
  }

  /**
   * Comparison function used to sort picks by travel time.
   *
   * @param pick A Pick object containing the second pick to compare
   * @return +1 if this Pick object travelTime variable is greater than the second Pick object Pick
   *     variable; -1 if this Pick object travelTime variable is less than the second Pick object
   *     travelTime variable; and 0 if this Pick object travelTime variable is equal to the second
   *     Pick object travelTime variable;
   */
  @Override
  public int compareTo(Pick pick) {
    // Sort into arrival time order.
    if (this.travelTime < pick.travelTime) {
      return -1;
    } else if (this.travelTime == pick.travelTime) {
      return 0;
    } else {
      return +1;
    }
  }
}
