package gov.usgs.locator;

import java.util.ArrayList;

import gov.usgs.traveltime.TTimeData;
import gov.usgs.traveltime.TauUtil;

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
	 * A String containing the code for the channel the pick was made on
	 */
	private String channelCode;	

	/**
	 * A double containing the pick arrival time in decimal seconds since the 
	 * epoch
	 */
	private double arrivalTime;

	/**
	 * A double containing the pick quality (standard error) in seconds.
	 */
	private double quality;			

	/**
	 * A boolean indicating whether the pick may be used in the location (external 
	 * command)
	 */
	private boolean externalUse;

	/**
	 * A String containing the original phase identification for this pick
	 */
	private String originalPhaseCode;

	/**
	 * An AuthorType object containing the author type for the original phase 
	 * identification
	 */
	private AuthorType authorType;
	
	/**
	 * A double containing the affinity (strength) of the original phase
	 * identification. Higher numbers make it harder to re-identify the phase.
	 */
	private double originalPhaseAffinity;

	/**
	 * A String containing the current phase identification for this pick
	 */
	private String currentPhaseCode;

	/**
	 * A double containing the observed-theoretical arrival time in seconds for the
	 * current phase identification
	 */	
	private double residual;

	/**
	 * A boolean indicating whether the pick is currently used in the location.
	 */
	private boolean isUsed;

	/**
	 * A double containing the pick weight in the location for the current phase 
	 * identification
	 */	
	private double weight;	

	/**
	 * A double containing the pick data importance to the location for the 
	 * current phase identification
	 */	
	private double importance;

	/**
	 * A String containing the best code to use for phase identification
	 */
	private String bestPhaseCode;		

	/**
	 * A double containing the current pick travel time in seconds
	 */	
	private double travelTime;				
	
	/**
	 * A boolean flag indicating whether the pick was automaticially made
	 */
	private boolean isAutomatic;

	/**
	 * A boolean flag indicating this pick was eliminated by the triage method
	 */
	private boolean isTriage;
	
	/**
	 * A TTimeData object holding the theoretical arrival with the minimum 
	 * statisticalFoM
	 */
	private TTimeData statisticalMinFoMTT;

	/**
	 * A double containing the current statistical figure-of-merit
	 */	
	private double statisticalFoM;			

	boolean forceStat;		// If true, force the association
	TTimeData mapAlt;			// Theoretical arrival with the minimum fomAlt
	double fomAlt;				// Alternate figure-of-merit
	boolean surfWave;			// If true, this phase can't be re-identified
	// A reusable weighted residual object:
	Wresidual wRes;
	
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
   * Function to get the pick arrival time
   * 
   * @return A double containing the pick arrival time in decimal seconds since
	 * 				 the epoch.
   */
  public double getArrivalTime() {
    return arrivalTime;
  }	

	/**
   * Function to get the pick quality
   * 
   * @return A double containing the pick quality
   */
  public double getQuality() {
    return quality;
  }	

	/**
   * Function to get the pick external use flag
   * 
   * @return A boolean indicating whether the pick should be used or not 
	 * (external) command
   */
  public boolean getExternalUse() {
    return externalUse;
  }	
	
	/**
   * Function to return the original phase code for the pick 
   * 
   * @return A String containing  the original phase code for the pick 
   */
  public String getOriginalPhaseCode() {
    return originalPhaseCode;
  }	

	/**
   * Function to get the pick author type
   * 
   * @return A AuthorType object holding the pick author type
	 */
  public AuthorType getAuthorType() {
    return authorType;
  }

	/**
   * Function to get the original phase affinity
   * 
   * @return A double containing the affinity (strength) of the original 
	 * 				 phase identification. Higher numbers make it harder to re-identify 
	 * 				 the phase.
   */
  public double getOriginalPhaseAffinity() {
    return originalPhaseAffinity;
  }	

	/**
   * Function to return the current phase code for the pick 
   * 
   * @return A String containing the current phase code for the pick 
   */
  public String getCurrentPhaseCode() {
    return currentPhaseCode;
  }	

	/**
   * Function to get the observed-theoretical arrival time in seconds
   * 
   * @return A double containing the observed-theoretical arrival time in 
	 * 				 seconds for the current phase identification
   */
  public double getResidual() {
    return residual;
  }	

	/**
   * Function to get whether the pick is currently used
   * 
   * @return A boolean indicating whether the pick is currenty used or not
   */
  public boolean getIsUsed() {
    return isUsed;
  }	
	
	/**
   * Function to get the pick weight in the location
   * 
   * @return A double containing the pick weight in the location for the current 
	 * 				 phase identification
   */
  public double getWeight() {
    return weight;
  }	

	/**
   * Function to get the pick data importance to the location
   * 
   * @return A double containing the pick data importance to the location for the 
	 * 				 current phase identification
   */
  public double getImportance() {
    return importance;
  }	

	/**
   * Function to return the best phase code for the pick 
   * 
   * @return A String containing the best phase code for the pick 
   */
  public String getBestPhaseCode() {
    return bestPhaseCode;
  }	

	/**
   * Function to get the current pick travel time in seconds
   * 
   * @return A double containing the current pick travel time in seconds
   */
  public double getTravelTime() {
    return travelTime;
  }	

	/**
   * Function to get whether the pick was automaticially made
   * 
   * @return A boolean flag indicating whether the pick was automaticially made
   */
  public boolean getIsAutomatic() {
    return isAutomatic;
  }	
	
	/**
   * Function to get whether the pick was eliminated by the triage method
   * 
   * @return A boolean flag indicating whether the pick was eliminated by the 
	 * 			   triage method
   */
  public boolean getIsTriage() {
    return isTriage;
  }	
	
	/**
   * Function to get the theoretical arrival object with the minimum 
	 * statisticalFoM
   * 
   * @return A TTimeData object holding the theoretical arrival with the minimum 
	 * 				 statisticalFoM
   */
  public TTimeData getStatisticalMinFoMTT() {
    return statisticalMinFoMTT;
	}	
	
	/**
   * Function to get the current statistical figure-of-merit
   * 
   * @return A double containing the current statistical figure-of-merit
   */
  public double getStatisticalFoM() {
    return statisticalFoM;
  }	
	





	/**
   * Function to set the observed-theoretical arrival time in seconds
   * 
   * @param residual A double containing the observed-theoretical arrival time in 
	 * 				 				 seconds for the current phase identification
   */
  public void setResidual(double residual) {
    this.residual = residual;
  }	

	/**
   * Function to set whether the pick is currently used
   * 
   * @param isUsed A boolean indicating whether the pick is currenty used or not
   */
  public void setIsUsed(boolean isUsed) {
    this.isUsed = isUsed;
  }	

	/**
   * Function to set the pick weight in the location
   * 
   * @param weight A double containing the pick weight in the location for the 
	 * 							 current phase identification
   */
  public void setWeight(double weight) {
    this.weight = weight;
  }	

	/**
   * Function to set the pick data importance to the location
   * 
   * @param importance A double containing the pick data importance to the 
	 * 									 location for the current phase identification
   */
  public void setImportance(double importance) {
    this.importance = importance;
  }	

	/**
   * Function to set whether the pick was eliminated by the triage method
   * 
   * @param isTriage A boolean flag indicating whether the pick was eliminated
	 * 								 by the triage method
   */
  public void setIsTriage(boolean isTriage) {
    this.isTriage = isTriage;
  }	


	/**
   * Function to set the theoretical arrival object with the minimum 
	 * statisticalFoM
   * 
   * @param statisticalMinFoMTT A TTimeData object holding the theoretical 
	 * 														arrival with the minimum statisticalFoM
   */
  public void setStatisticalMinFoMTT(TTimeData statisticalMinFoMTT) {
    this.statisticalMinFoMTT = statisticalMinFoMTT;
	}	
	
	/**
   * Function to set the current statistical figure-of-merit
   * 
   * @param statisticalFoM A double containing the current statistical 
	 * figure-of-merit
   */
  public void setStatisticalFoM(double statisticalFoM) {
    this.statisticalFoM = statisticalFoM;
  }	

	/**
	 * Create the pick with just enough information to be useful.
	 * 
	 * @param station Station information
	 * @param channelCode Channel code
	 * @param arrivalTime Arrival time in seconds since the epoch
	 * @param externalUse If true, an analyst wants the pick to be used
	 * @param currentPhaseCode Current locator or associator phase code
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
		authorType = null;
		originalPhaseAffinity = 3d;
		isUsed = externalUse;
		residual = Double.NaN;
		weight = 0d;
		importance = 0d;

		// Initialize internal variables too.
		bestPhaseCode = currentPhaseCode;
		travelTime = Double.NaN;
		isAutomatic = true;
		surfWave = false;
		isTriage = false;
		initFoM();

		// Create an empty weighted residual.
		wRes = new Wresidual();
	}
	
	/**
	 * Additional information to help in phase association or location.
	 * 
	 * @param sourceID User created string identifying the server user
	 * @param pickID Data base ID (convenience for Hydra)
	 * @param quality Pick uncertainty in seconds (not currently used)
	 * @param originalPhaseCode Original pick identification (associator or analyst)
	 * @param authorType Type (e.g., human or auto) of the original phase 
	 * identification
	 * @param originalPhaseAffinity Higher numbers make it harder to re-identify the phase
	 */
	public void addIdAids(String sourceID, String pickID, double quality, 
			String originalPhaseCode, AuthorType authorType, double originalPhaseAffinity) {
		this.sourceID = sourceID;
		this.pickID = pickID;
		this.quality = quality;
		this.originalPhaseCode = originalPhaseCode;
		this.authorType = authorType;
		this.originalPhaseAffinity = authorType.affinity(originalPhaseAffinity);
		// Use an enum for the author type.
		switch(authorType) {
			case CONTRIB_HUMAN: case LOCAL_HUMAN:
				currentPhaseCode = originalPhaseCode;
				bestPhaseCode = originalPhaseCode;
				isAutomatic = false;
				break;
			default:
				bestPhaseCode = currentPhaseCode;
				break;
		}
    if ((bestPhaseCode.equals("Lg") || bestPhaseCode.equals("LR")) && !isAutomatic) 
    	surfWave = true;
	}
	
	/**
	 * Recompute the travel time for this pick when the hypocenter 
	 * is updated.
	 * 
	 * @param hypo Hypocenter information
	 */
	public void updateTt(Hypocenter hypo) {
		travelTime = arrivalTime-hypo.getOriginTime();
	}
	
	/**
	 * Update the phase code.  This abbreviated version is only used for 
	 * the initial phase ID.
	 * 
	 * @param currentPhaseCode New phase code
	 */
	public void updateID(String currentPhaseCode) {
		this.currentPhaseCode = currentPhaseCode;
		if(isAutomatic) bestPhaseCode = currentPhaseCode;
	}
	
	/**
	 * When the phase is re-identified, we need to update the phase code, 
	 * residual, derivatives, and optionally the weight.
	 * 
	 * @param first True if this is the first phase in a group
	 * @param reWeight True if weights are to be recomputed
	 * @param azimuth Azimuth of the station from the source in degrees
	 * @param wResiduals The weighted residual information used in 
	 * the Rank-Sum routines is updated here
	 * @return True if a used phase has changed identification or is no 
	 * longer used
	 */
	public boolean updateID(boolean first, boolean reWeight, double azimuth, 
			ArrayList<Wresidual> wResiduals) {
		boolean changed = false, reID = false;
		String ttCode;
		
		if(statisticalMinFoMTT != null) {
			// We have an identification.  Set up some key variables.
			ttCode = statisticalMinFoMTT.getPhCode();
			if(!currentPhaseCode.equals(ttCode)) reID = true;
			if(LocUtil.deBugLevel > 0 && reID) System.out.format("=====> Phase "+
					"re-ID: %-5s %-8s -> %-8s\n", station.staID.staCode, currentPhaseCode, ttCode);
			currentPhaseCode = ttCode;
			if(isAutomatic) bestPhaseCode = currentPhaseCode;
			if(!currentPhaseCode.equals("LR")) {
				residual = travelTime-statisticalMinFoMTT.getTT();
			} else {
				residual = 0d;
			}
			// If this phase is still being used, set it for processing.
			if(isUsed && statisticalMinFoMTT.canUse() && (statisticalFoM <= 
					LocUtil.computeValidityLimit(statisticalMinFoMTT.getSpread()) || forceStat)) {
				if(reWeight) weight = 1d/Math.max(statisticalMinFoMTT.getSpread(), 0.2d);
				// Add it to weighted residual storage.
				wRes.reInit(this, residual, weight, false, 
						LocUtil.computeTTLatDerivative(statisticalMinFoMTT.getDTdD(), azimuth), 
						LocUtil.computeTTLonDerivative(statisticalMinFoMTT.getDTdD(), azimuth), statisticalMinFoMTT.getDTdZ());
				wResiduals.add(wRes);
				if(reID) changed = true;
			} else {
				// Otherwise, see if it was used before.
				if(isUsed) {
					if(LocUtil.deBugLevel > 0) System.out.format("=====> Phase no "+
							"use set (wt): %-5s %-8s %5b %5.2f\n", station.staID.staCode, 
							currentPhaseCode, statisticalMinFoMTT.canUse(), statisticalMinFoMTT.getSpread());
					isUsed = false;
				  // Prevents initially identified first arrivals from coming back.
					if(first) externalUse = false; 
					changed = true;
				}
				weight = 0d;
			}
			
		} else {
			// We don't have an identification.
			if(LocUtil.deBugLevel > 0 && !currentPhaseCode.equals("")) {
				System.out.format("=====> Phase re-ID: %-5s %-8s -> null\n", 
						station.staID.staCode, currentPhaseCode);
			}
			// See if it was used before.
			if(isUsed) {
				if(LocUtil.deBugLevel > 0) System.out.format("=====> Phase no "+
						"use set (no ID): %-5s %-8s\n", station.staID.staCode, currentPhaseCode);
				isUsed = false;
			  // Prevents initially identified first arrivals from coming back.
				if(first) externalUse = false; 
				changed = true;
			}
			// Close it out.
			currentPhaseCode = "";
			residual = 0d;
			weight = 0d;
		}
		if(LocUtil.deBugLevel > 1) System.out.format("  IDphas: %-5s %-8s "+
				"%6.2f %7.4f %b\n", station.staID.staCode, currentPhaseCode, residual, weight, 
				isUsed);
		return changed;
	}
	
	/**
	 * Initialize figure-of-merit variables.
	 */
	public void initFoM() {
		statisticalFoM = 0d;
		statisticalMinFoMTT = null;
		forceStat = false;
		fomAlt = TauUtil.DMAX;
		mapAlt = null;
	}
	
	/**
	 * Set the statistical figure-of-merit variables.
	 * 
	 * @param tTime Travel-time information
	 * @param statisticalFoM Figure-of-merit metric
	 */
	public void setFomStat(TTimeData tTime, double statisticalFoM) {
		statisticalMinFoMTT = tTime;
		this.statisticalFoM = statisticalFoM;
	}
	
	/**
	 * Set the alternate figure-of-merit variables.
	 * 
	 * @param tTime Travel-time information
	 * @param fomAlt Figure-of-merit metric
	 */
	public void setFomAlt(TTimeData tTime, double fomAlt) {
		mapAlt = tTime;
		this.fomAlt = fomAlt;
	}
	
	/**
	 * A convenient string identifying this pick.
	 */
	@Override
	public String toString() {
		return String.format("%-5s %-8s %6.2f %b", station.staID.staCode, 
				currentPhaseCode, residual, isUsed);
	}

	/**
	 * Sort picks by travel time.
	 */
	@Override
	public int compareTo(Pick pick) {
		// Sort into arrival time order.
		if(this.travelTime < pick.travelTime) {
			return -1;
		}	else if(this.travelTime == pick.travelTime) {
			return 0;
		}	else {
			return +1;
		}
	}
}
