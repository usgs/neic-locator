package gov.usgs.locator;

import gov.usgs.traveltime.TTimeData;
import gov.usgs.traveltime.TauUtil;
import java.util.ArrayList;

/**
 * The Pick class stores the data making up one pick.
 * 
 * @author Ray Buland
 *
 */
public class Pick implements Comparable<Pick> {
  /**
   * A String containing the pick ID.
   */
  private String pickID;

  /**
   * A String containing the ID of the source.
   */
  private String sourceID;

  /**
   * A Station object containing station the pick was generated at.
   */
  private Station station;

  /**
   * A String containing the code for the channel the pick was made on.
   */
  private String channelCode;  

  /**
   * A double containing the pick arrival time in decimal seconds since the 
   * epoch.
   */
  private double arrivalTime;

  /**
   * A double containing the pick quality (standard error) in seconds.
   */
  private double quality;      

  /**
   * A boolean indicating whether the pick may be used in the location (external 
   * command).
   */
  private boolean externalUse;

  /**
   * A String containing the original phase identification for this pick.
   */
  private String originalPhaseCode;

  /**
   * An AuthorType object containing the author type for the original phase 
   * identification.
   */
  private AuthorType originalAuthorType;
  
  /**
   * A double containing the affinity (strength) of the original phase
   * identification. Higher numbers make it harder to re-identify the phase.
   */
  private double originalPhaseAffinity;

  /**
   * A String containing the current phase identification for this pick.
   */
  private String currentPhaseCode;

  /**
   * A double containing the observed-theoretical arrival time in seconds for the
   * current phase identification.
   */  
  private double residual;

  /**
   * A boolean indicating whether the pick is currently used in the location.
   */
  private boolean isUsed;

  /**
   * A double containing the pick weight in the location for the current phase 
   * identification.
   */  
  private double weight;  

  /**
   * A double containing the pick data importance to the location for the 
   * current phase identification.
   */  
  private double importance;

  /**
   * A String containing the best code to use for phase identification.
   */
  private String bestPhaseCode;    

  /**
   * A double containing the current pick travel time in seconds.
   */  
  private double travelTime;        
  
  /**
   * A boolean flag indicating whether the pick was automaticially made.
   */
  private boolean isAutomatic;

  /**
   * A boolean flag indicating this pick was eliminated by the triage method.
   */
  private boolean isTriage;
  
  /**
   * A TTimeData object holding the theoretical arrival with the minimum 
   * statisticalFoM used in phase identification.
   */
  private TTimeData ttStatisticalMinFoM;

  /**
   * A double containing the current statistical figure-of-merit used in phase 
   * identification.
   */  
  private double statisticalFoM;      

  /**
   * A boolean flag indicating the association should be forced during phase 
   * identification.
   */
  private boolean forceAssociation;

  /**
   * A TTimeData object holding the theoretical arrival with the minimum 
   * alternateFoM used in phase identification.
   */
  private TTimeData ttAlternateMinFoM;

  /**
   * A double containing the alternate statistical figure-of-merit used in phase 
   * identification.
   */  
  private double alternateFoM;

  /**
   * A boolean flag indicating this pick is a surface wave.  Surface wave phases 
   * can't be re-identified.
   */
  private boolean isSurfaceWave;

  /** 
   * A Wresidual object containing the weighted residual for this picks.
   */
  private Wresidual weightedResidual;
  
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
   * @return A double containing the pick arrival time in decimal seconds since
   *          the epoch.
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
   * @return A boolean indicating whether the pick should be used or not 
   *         (external) command
   */
  public boolean getExternalUse() {
    return externalUse;
  }  
  
  /**
   * Function to return the original phase code for the pick.
   * 
   * @return A String containing  the original phase code for the pick 
   */
  public String getOriginalPhaseCode() {
    return originalPhaseCode;
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
   * @return A double containing the affinity (strength) of the original 
   *          phase identification. Higher numbers make it harder to re-identify 
   *          the phase.
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
   * @return A double containing the observed-theoretical arrival time in 
   *          seconds for the current phase identification
   */
  public double getResidual() {
    return residual;
  }  

  /**
   * Function to get whether the pick is currently used.
   * 
   * @return A boolean indicating whether the pick is currenty used or not
   */
  public boolean getIsUsed() {
    return isUsed;
  }  
  
  /**
   * Function to get the pick weight in the location.
   * 
   * @return A double containing the pick weight in the location for the current 
   *          phase identification
   */
  public double getWeight() {
    return weight;
  }  

  /**
   * Function to get the pick data importance to the location.
   * 
   * @return A double containing the pick data importance to the location for the 
   *          current phase identification
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
   * Function to get whether the pick was automaticially made.
   * 
   * @return A boolean flag indicating whether the pick was automaticially made
   */
  public boolean getIsAutomatic() {
    return isAutomatic;
  }  
  
  /**
   * Function to get whether the pick was eliminated by the triage method.
   * 
   * @return A boolean flag indicating whether the pick was eliminated by the 
   *          triage method
   */
  public boolean getIsTriage() {
    return isTriage;
  }  
  
  /**
   * Function to get the theoretical arrival object with the minimum 
   * statisticalFoM.
   * 
   * @return A TTimeData object holding the theoretical arrival with the minimum 
   *          statisticalFoM
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
   * Function to get the theoretical arrival object with the minimum 
   * alternateFoM.
   * 
   * @return A TTimeData object holding the theoretical arrival with the minimum 
   *          alternateFoM
   */
  public TTimeData getTTAlternateMinFoM() {
    return ttAlternateMinFoM;
  }  

  /**
   * Function to get the current alternate figure-of-merit.
   * 
   * @return A double containing the current alternate figure-of-merit
   */
  public double getAlternateFoM() {
    return alternateFoM;
  }  

  /**
   * Function to get whether this pick is a surface wave.  Surface wave phases 
   * can't be re-identified
   * 
   * @return A boolean flag indicating whether this pick is a surface wave.
   */
  public boolean getIsSurfaceWave() {
    return isSurfaceWave;
  }    

  /**
   * Function to set the observed-theoretical arrival time in seconds.
   * 
   * @param residual A double containing the observed-theoretical arrival time in 
   *                   seconds for the current phase identification
   */
  public void setResidual(double residual) {
    this.residual = residual;
  }  

  /**
   * Function to set whether the pick is currently used.
   * 
   * @param isUsed A boolean indicating whether the pick is currenty used or not
   */
  public void setIsUsed(boolean isUsed) {
    this.isUsed = isUsed;
  }  

  /**
   * Function to set the pick weight in the location.
   * 
   * @param weight A double containing the pick weight in the location for the 
   *                current phase identification
   */
  public void setWeight(double weight) {
    this.weight = weight;
  }  

  /**
   * Function to set the pick data importance to the location.
   * 
   * @param importance A double containing the pick data importance to the 
   *                    location for the current phase identification
   */
  public void setImportance(double importance) {
    this.importance = importance;
  }  

  /**
   * Function to set whether the pick was eliminated by the triage method.
   * 
   * @param isTriage A boolean flag indicating whether the pick was eliminated
   *                  by the triage method
   */
  public void setIsTriage(boolean isTriage) {
    this.isTriage = isTriage;
  }  

  /**
   * Function to set whether the association should be forced.
   * 
   * @param forceAssociation A boolean flag indicating whether the association 
   *                          should be forced.
   */
  public void setForceAssociation(boolean forceAssociation) {
    this.forceAssociation = forceAssociation;
  }  

  /**
   * Function to set the theoretical arrival object with the minimum 
   * statisticalFoM.
   * 
   * @param ttStatisticalMinFoM A TTimeData object holding the theoretical 
   *                             arrival with the minimum statisticalFoM
   */
  public void setTTStatisticalMinFoM(TTimeData ttStatisticalMinFoM) {
    this.ttStatisticalMinFoM = ttStatisticalMinFoM;
  }  
  
  /**
   * Function to set the current statistical figure-of-merit.
   * 
   * @param statisticalFoM A double containing the current statistical 
   *                       figure-of-merit
   */
  public void setStatisticalFoM(double statisticalFoM) {
    this.statisticalFoM = statisticalFoM;
  }  

  /**
   * This function sets the statistical figure-of-merit variables.
   * 
   * @param statisticalTravelTime A TTimeData object containing the travel-time 
   *                              information
   * @param statisticalFoM A double containing the statistical Figure-of-merit 
   *                       metric
   */
  public void setStatisticalFoM(TTimeData statisticalTravelTime, 
      double statisticalFoM) {
    ttStatisticalMinFoM = statisticalTravelTime;
    this.statisticalFoM = statisticalFoM;
  }
  
  /**
   * Function to set the current alternate figure-of-merit.
   * 
   * @param alternateFoM A double containing the current alternate 
   *                     figure-of-merit
   */
  public void setAlternateFoM(double alternateFoM) {
    this.alternateFoM = alternateFoM;
  }  

  /**
   * This function sets the alternate figure-of-merit variables.
   * 
   * @param alternateTravelTime A TTimeData object containing the travel-time 
   *                            information
   * @param alternateFoM A double containing the altermate Figure-of-merit 
   *                     metric
   */
  public void setAlternateFoM(TTimeData alternateTravelTime, 
      double alternateFoM) {
    ttAlternateMinFoM = alternateTravelTime;
    this.alternateFoM = alternateFoM;
  }  

  /**
   * The Pick constructor. This constructor creates the pick with the minimum
   * required provided information.
   * 
   * @param station A Station object containing the information on the station 
   *                 the pick was made at.
   * @param channelCode A String containing the pick channel code
   * @param arrivalTime A double holding the pick arrival time in decimal 
   *                     seconds since the epoch
   * @param externalUse A boolean flag indicating whether the is requested to be
   *                     used externally
   * @param currentPhaseCode A string containing the current locator or 
   *                          associator phase identification
   */
  public Pick(Station station, String channelCode, double arrivalTime, 
      boolean externalUse, String currentPhaseCode) {
    // Remember the inputs.
    this.station = station;
    this.channelCode = channelCode;
    this.arrivalTime = arrivalTime;
    this.externalUse = externalUse;
    this.currentPhaseCode = currentPhaseCode;

    // Set defaults.
    pickID = "0";
    quality = 0d;
    originalPhaseCode = null;
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
    weightedResidual = new Wresidual();
  }
  
  /**
   * This function sets additional information to help with phase identification 
   * and location.
   * 
   * @param sourceID A String identifiying the pick source. 
   * @param pickID A String containing the pick's external ID
   * @param quality A double containing the pick uncertainty in seconds (not 
   *                 currently used in phase identification and location)
   * @param originalPhaseCode A String containing the original associator or 
   *                           other external pick phase identification
   * @param originalAuthorType An AuthorType object containing the type (e.g.,
   *                           human or auto) of the original phase 
   *                           identification
   * @param originalPhaseAffinity Higher numbers make it harder to re-identify 
   *                              the phase
   */
  public void setPhaseIdInfo(String sourceID, String pickID, double quality, 
      String originalPhaseCode, AuthorType originalAuthorType, double originalPhaseAffinity) {
    this.sourceID = sourceID;
    this.pickID = pickID;
    this.quality = quality;
    this.originalPhaseCode = originalPhaseCode;
    this.originalAuthorType = originalAuthorType;
    this.originalPhaseAffinity = originalAuthorType.affinity(
        originalPhaseAffinity);

    // Use the enum for the author type.
    switch (originalAuthorType) {
      case CONTRIB_HUMAN: case LOCAL_HUMAN:
        currentPhaseCode = originalPhaseCode;
        bestPhaseCode = originalPhaseCode;
        isAutomatic = false;
        break;

      default:
        bestPhaseCode = currentPhaseCode;
        break;
    }

    if ((bestPhaseCode.equals("Lg") || bestPhaseCode.equals("LR")) 
        && !isAutomatic) {
      isSurfaceWave = true;
    }
  }
  
  /**
   * This function recomputes the travel time for this pick when the hypocenter 
   * is updated.
   * 
   * @param hypo A Hypocenter object holding the Hypocenter information
   */
  public void updateTravelTime(Hypocenter hypo) {
    travelTime = arrivalTime - hypo.getOriginTime();
  }
  
  /**
   * This function updates the phase identification.  This abbreviated version 
   * is only used for the initial phase identification.
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
   * This function updates the phase identification. When the phase is 
   * re-identified, we need to update the phase code, residual, derivatives, and 
   * optionally the weight.
   * 
   * @param isFirstPhase A boolean flag indicating if this is the first phase in 
   *                      a group
   * @param shouldReweight A boolean flag indicating whether weights are to be 
   *                        recomputed
   * @param azimuth A double holding the azimuth of the station from the source 
   *                 in degrees
   * @param weightedResiduals An ArrayList of Wresidual objects conatining the
   *                           weighted residual information used in the Rank-Sum 
   *                           routines.
   * @return True if a used phase has changed identification or is no longer 
   *          used
   */
  public boolean updatePhaseIdentification(boolean isFirstPhase, 
      boolean shouldReweight, double azimuth, 
      ArrayList<Wresidual> weightedResiduals) {
    boolean idChanged = false;
    
    if (ttStatisticalMinFoM != null) {
      // We have an identification.  Set up some key variables.
      String ttCode = ttStatisticalMinFoM.getPhCode();

      boolean reID = false;
      if (!currentPhaseCode.equals(ttCode)) {
        reID = true;
      }

      if (LocUtil.deBugLevel > 0 && reID) {
        System.out.format("=====> Phase re-ID: %-5s %-8s -> %-8s\n", 
            station.getStationID().staCode, currentPhaseCode, ttCode);
      }
    
      currentPhaseCode = ttCode;
      
      if (isAutomatic) {
        bestPhaseCode = currentPhaseCode;
      }

      if (!currentPhaseCode.equals("LR")) {
        residual = travelTime - ttStatisticalMinFoM.getTT();
      } else {
        residual = 0d;
      }

      // If this phase is still being used, set it for processing.
      if (isUsed && ttStatisticalMinFoM.canUse() && (statisticalFoM 
          <= LocUtil.computeValidityLimit(ttStatisticalMinFoM.getSpread()) 
          || forceAssociation)) {
        if (shouldReweight) {
          weight = 1d / Math.max(ttStatisticalMinFoM.getSpread(), 0.2d);
        }

        // Add it to weighted residual storage.
        weightedResidual.reInit(this, residual, weight, false, 
            LocUtil.computeTTLatDerivative(ttStatisticalMinFoM.getDTdD(), azimuth), 
            LocUtil.computeTTLonDerivative(ttStatisticalMinFoM.getDTdD(), azimuth), 
            ttStatisticalMinFoM.getDTdZ());
        weightedResiduals.add(weightedResidual);

        if (reID) {
          idChanged = true;
        }
      } else {
        // Otherwise, see if it was used before.
        if (isUsed) {
          if (LocUtil.deBugLevel > 0) {
            System.out.format("=====> Phase no use set (wt): %-5s %-8s %5b %5.2f\n", 
                station.getStationID().staCode, currentPhaseCode, 
                ttStatisticalMinFoM.canUse(), ttStatisticalMinFoM.getSpread());
          }

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
      if (LocUtil.deBugLevel > 0 && !currentPhaseCode.equals("")) {
        System.out.format("=====> Phase re-ID: %-5s %-8s -> null\n", 
            station.getStationID().staCode, currentPhaseCode);
      }

      // See if it was used before.
      if (isUsed) {
        if (LocUtil.deBugLevel > 0) {
          System.out.format("=====> Phase no use set (no ID): %-5s %-8s\n", 
              station.getStationID().staCode, currentPhaseCode);
        }

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
    if (LocUtil.deBugLevel > 1) {
      System.out.format("  IDphas: %-5s %-8s %6.2f %7.4f %b\n", 
          station.getStationID().staCode, currentPhaseCode, residual, weight, isUsed);
    }

    return idChanged;
  }
  
  /**
   * This function initializes figure-of-merit variables.
   */
  public void initializeFoM() {
    statisticalFoM = 0d;
    ttStatisticalMinFoM = null;
    forceAssociation = false;
    alternateFoM = TauUtil.DMAX;
    ttAlternateMinFoM = null;
  }
  
  /**
   * This function converts the pick into a string.
   */
  @Override
  public String toString() {
    return String.format("%-5s %-8s %6.2f %b", station.getStationID().staCode, 
        currentPhaseCode, residual, isUsed);
  }

  /**
   * Comparison function used to sort picks by travel time.
   * @param pick A Pick object containing the second pick to compare
   * @return +1 if this Pick object travelTime varible is greater than the 
   *         second Pick object Pick varible; -1 if this Pick object travelTime 
   *         varible is less than the second Pick object travelTime varible; and 
   *         0 if this Pick object travelTime varible is equal to the second  
   *         Pick object travelTime varible;
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
