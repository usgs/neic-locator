package gov.usgs.locator;
import java.util.ArrayList;

import gov.usgs.traveltime.TTimeData;
import gov.usgs.traveltime.TauUtil;
/**
 * Keep all the data for one pick together.
 * 
 * @author Ray Buland
 *
 */
public class Pick implements Comparable<Pick> {
	// Hydra specific:
	String dbID;					// Hydra aid to database update
	// Inputs:
	Station station;			// Station
	String chaCode;				// Channel code
	double arrivalTime;		// Arrival time in seconds since the epoch
	double quality;				// Pick quality (standard error) in seconds
	boolean cmndUse;			// If true, the phase may be used (analyst command)
	String obsCode;				// Original phase identification
	AuthorType authType;	// Author type for the original phase identification
	double affinity;			// Higher numbers make it harder to re-identify the phase
	// Input/Output:
	String phCode;				// Current phase identification
	// Outputs:
	double residual;			// Observed-theoretical arrival time in seconds
	boolean used;					// True if the pick is being used in the location
	double weight;				// Pick weight
	double importance;		// Pick data importance
	// Internal use:
	String idCode;				// Best code to use for phase identification
	double tt;						// Travel-time
	boolean auto;					// True if this is an automatic pick
	// Phase identification use:
	TTimeData mapStat;		// Theoretical arrival with the minimum fomStat
	double fomStat;				// Statistical figure-of-merit
	boolean forceStat;		// If true, force the association
	TTimeData mapAlt;			// Theoretical arrival with the minimum fomAlt
	double fomAlt;				// Alternate figure-of-merit
	boolean surfWave;			// If true, this phase can't be re-identified
	
	/**
	 * Create the pick with just enough information to be useful.
	 * 
	 * @param station Station ID
	 * @param latitude Geographic latitude in degrees
	 * @param longitude Longitude in degrees
	 * @param elevation Elevation in kilometers
	 * @param arrivalTime Arrival time in seconds since the epoch
	 * @param use If true, the pick may be used in a a location
	 * @param phCode Current locator or associator phase code
	 */
	public Pick(Station station, String chaCode, double arrivalTime, 
			boolean cmndUse, String phCode) {
		// Remember the inputs.
		this.station = station;
		this.chaCode = chaCode;
		this.arrivalTime = arrivalTime;
		this.cmndUse = cmndUse;
		this.phCode = phCode;
		// Set defaults.
		dbID = null;
		quality = 0d;
		obsCode = null;
		authType = null;
		affinity = 3d;
		used = cmndUse;
		residual = Double.NaN;
		weight = Double.NaN;
		importance = Double.NaN;
		// Initialize internal variables too.
		idCode = phCode;
		tt = Double.NaN;
		auto = true;
		surfWave = false;
		initFoM();
	}
	
	/**
	 * Additional information to help in phase association or location.
	 * 
	 * @param dbID Data base ID (convenience for Hydra)
	 * @param quality Pick uncertainty in seconds (not currently used)
	 * @param obsCode Original pick identification (associator or analyst)
	 * @param authority Source of the original phase identification
	 * @param affinity Higher numbers make it harder to re-identify the phase
	 */
	public void addIdAids(String dbID, double quality, String obsCode, 
			AuthorType authType, double affinity) {
		this.dbID = dbID;
		this.quality = quality;
		this.obsCode = obsCode;
		this.authType = authType;
		this.affinity = authType.affinity(affinity);
		// Use an enum for the author type.
		switch(authType) {
			case CONTRIB_HUMAN: case LOCAL_HUMAN:
				phCode = obsCode;
				idCode = obsCode;
				auto = false;
				break;
			default:
				idCode = phCode;
				break;
		}
    if ((idCode.equals("Lg") || idCode.equals("LR")) && !auto) 
    	surfWave = true;
	}
	
	/**
	 * Recompute the travel time for this pick when the hypocenter 
	 * is updated.
	 * 
	 * @param hypo Hypocenter information
	 */
	public void updateTt(Hypocenter hypo) {
		tt = arrivalTime-hypo.originTime;
	}
	
	/**
	 * When the phase is re-identified, these parameters need to change.
	 * 
	 * @param phCode Current phase code
	 * @param weight Pick regression weight
	 * @return True if a used phase has changed identification or is no 
	 * longer used
	 */
	public boolean updateID(boolean first, boolean reWeight, double azimuth, 
			ArrayList<Wresidual> wResiduals) {
		boolean changed = false;
		String ttCode;
		Wresidual wRes;
		
		if(mapStat != null) {
			// We have an identification.  Set up some key variables.
			ttCode = mapStat.getPhCode();
			if(!phCode.equals(ttCode)) 
				System.out.format("=====> Phase re-ID: %-5s %-8s -> %-8s\n", 
						station.staID.staCode, phCode, ttCode);
			phCode = ttCode;
			if(auto) idCode = phCode;
			if(!phCode.equals("LR")) {
				residual = tt-mapStat.getTT();
			} else {
				residual = 0d;
			}
			// If this phase is still being used, set the weight.
			if((used && mapStat.canUse() && fomStat <= 
					LocUtil.validLim(mapStat.getSpread())) || forceStat) {
				if(reWeight) weight = 1d/Math.max(mapStat.getSpread(), 0.2d);
				wRes = new Wresidual(false, wResiduals.size(), residual, weight);
				wRes.addDeriv(LocUtil.dTdLat(mapStat.getDTdD(), azimuth), 
						LocUtil.dTdLon(mapStat.getDTdD(), azimuth), mapStat.getDTdZ());
				wResiduals.add(wRes);
				if(!phCode.equals(ttCode)) changed = true;
			} else {
				// Otherwise, see if it was used before.
				if(used) {
					System.out.format("=====> Phase no use set (wt): %-5s %-8s %5b "+
							"%5.2f\n", station.staID.staCode, phCode, mapStat.canUse(), 
							mapStat.getSpread());
					used = false;
				  // Prevents initially identified first arrivals from coming back.
					if(first) cmndUse = false; 
					changed = true;
				}
				weight = 0d;
			}
			
		} else {
			// We don't have an identification.
			System.out.format("=====> Phase re-ID: %-5s %-8s -> null\n", 
					station.staID.staCode, phCode);
			// See if it was used before.
			if(used) {
				System.out.format("=====> Phase no use set (no ID): %-5s %-8s\n", 
						station.staID.staCode, phCode);
				used = false;
			  // Prevents initially identified first arrivals from coming back.
				if(first) cmndUse = false; 
				changed = true;
			}
			// Close it out.
			phCode = "";
			residual = 0d;
			weight = 0d;
		}
		System.out.format("  IDphas: %-5s %-8s %6.2f %7.4f %b\n", 
				station.staID.staCode, phCode, residual, weight, used);
		return changed;
	}
	
	/**
	 * Initialize figure-of-merit variables.
	 */
	public void initFoM() {
		fomStat = 0d;
		mapStat = null;
		forceStat = false;
		fomAlt = TauUtil.DMAX;
		mapAlt = null;
	}
	
	/**
	 * Set the statistical figure-of-merit variables.
	 * 
	 * @param tTime Travel-time information
	 * @param fomStat Figure-of-merit metric
	 */
	public void setFomStat(TTimeData tTime, double fomStat) {
		mapStat = tTime;
		this.fomStat = fomStat;
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
	 * The used flag can only change from true to false. 
	 */
	public void dontUse() {
		used = false;
	}

	/**
	 * Sort picks by arrival time.
	 */
	@Override
	public int compareTo(Pick pick) {
		// Sort into arrival time order.
		if(this.arrivalTime < pick.arrivalTime) return +1;
		else if(this.arrivalTime == pick.arrivalTime) return 0;
		else return -1;
	}
}
