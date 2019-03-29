package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Pick groups contain all the picks observed at one station for 
 * one event.  This construct is useful because the station 
 * geometry is the same for all picks in the group, so travel times 
 * are only computed once for each group.  Note that the Locator 
 * flow depends on the Bulletin Hydra style input file.  In 
 * particular, that picks for each station are together.  In 
 * preserving the Hydra order, picks in each group are also in 
 * time order.  The Locator doesn't actually require this, but it 
 * is convenient for bulltin style output.  Because the Locator 
 * server may get input for all sorts of different sources, the 
 * Hydra order is imposed on all event input.
 * 
 * @author Ray Buland
 *
 */
public class PickGroup {
	// Inputs:
	Station station;			// Station information
	// Outputs:
	double delta;					// Source-receiver distance in degrees
	double azimuth;				// Receiver azimuth from the source in degrees
	// Picks:
	ArrayList<Pick> picks;
	// Internal use:
	double fomMax;				// Maximum figure-of-merit

	/**
	 * Initialize the pick group with the station and the first pick.
	 * 
	 * @param station Station information
	 * @param pick Pick information
	 */
	public PickGroup(Station station, Pick pick) {
		this.station = station;
		picks = new ArrayList<Pick>();
		picks.add(pick);
		delta = Double.NaN;
		azimuth = Double.NaN;
	}
	
	/**
	 * Add another pick to the group.
	 * 
	 * @param pick Pick information
	 */
	public void add(Pick pick) {
		picks.add(pick);
	}
	
	/**
	 * Both the hypocenter and origin time have changed.  
	 * Update the distance, azimuth, and travel times.
	 * 
	 * @param hypo Hypocenter information.
	 */
	public void updateEvent(Hypocenter hypo) {
		// Distance and azimuth are group level parameters.
		delta = LocUtil.computeDistAzm(hypo, station);
		azimuth = LocUtil.azimuth;
		// Update travel times.
		for(int j=0; j<picks.size(); j++) {
			picks.get(j).updateTt(hypo);
		}
	}
	
	/**
	 * Update the pick group when the hypocenter is updated, 
	 * but not the origin time.
	 * 
	 * @param hypo Hypocenter information
	 */
	public void updateHypo(Hypocenter hypo) {
		// Distance and azimuth are group level parameters.
		delta = LocUtil.computeDistAzm(hypo, station);
		azimuth = LocUtil.azimuth;
	}
	
	/**
	 * Update the travel time for picks in the group when the 
	 * origin time has changed.
	 * 
	 * @param hypo Hypocenter information
	 */
	public void updateOrigin(Hypocenter hypo) {
		for(int j=0; j<picks.size(); j++) {
			picks.get(j).updateTt(hypo);
		}
	}
	
	/**
	 * Update the phase identifications for all picks in this group.
	 * 
	 * @param reWeight If true, recompute the residual weights
	 * @param wResiduals ArrayList of weighted residuals
	 * @return True if any used pick in the group has changed 
	 * significantly
	 */
	public boolean updateID(boolean reWeight, 
			ArrayList<Wresidual> wResiduals) {
		boolean changed = false;
		
		if(picks.get(0).updateID(true, reWeight, azimuth, wResiduals)) 
			changed = true;
		for(int j=1; j<picks.size(); j++) {
			if(picks.get(j).updateID(false, reWeight, azimuth, wResiduals)) 
				changed = true;
		}
		return changed;
	}
	
	/**
	 * Initialize the figure-of-merit variables for all picks in the 
	 * group.
	 * 
	 * @param pickBeg Index of the first pick in the group to be 
	 * initialized
	 * @param pickEnd Index of the last pick in the group to be 
	 * initialized
	 */
	public void initFoM(int pickBeg, int pickEnd) {
		fomMax = 0d;
		for(int j=pickBeg; j<pickEnd; j++) {
			picks.get(j).initFoM();
		}
	}
	
	/**
	 * Get the number of picks in the group.
	 * 
	 * @return Number of picks in the group
	 */
	public int noPicks() {
		return picks.size();
	}
	
	/**
	 * Count the number of picks in the group that are used 
	 * in the location.
	 * 
	 * @return Number of used picks
	 */
	public int picksUsed() {
		int noUsed = 0;
		for(int j=0; j<picks.size(); j++) {
			if(picks.get(j).getIsUsed()) noUsed++;
		}
		return noUsed;
	}
	
	/**
	 * Get the jth pick in the group.
	 * 
	 * @param j Pick index
	 * @return Return the pick with index j
	 */
	public Pick getPick(int j) {
		return picks.get(j);
	}
	
	/**
	 * Print out the input pick information in a format similar to 
	 * the Hydra event input file.
	 */
	public void printIn() {
		Pick pick;
		
		for(int j=0; j<picks.size(); j++) {
			pick = picks.get(j);
			System.out.format("%10s %-5s %3s %2s %2s %8.4f %9.4f %5.2f "+
					"%3.1f %-8s %12s %5b %-13s %-8s %3.1f\n", pick.getPickID(), 
					station.staID.staCode, pick.getChannelCode(), station.staID.netCode, 
					station.staID.locCode, station.latitude, station.longitude, 
					station.elevation, pick.getQuality(), pick.getCurrentPhaseCode(), 
					LocUtil.getTimeString(pick.getArrivalTime()), pick.getExternalUse(), 
					pick.getAuthorType(), pick.getOriginalPhaseCode(), 
					pick.getOriginalPhaseAffinity());
		}
	}
	
	/**
	 * Print the picks in a group in a more user friendly manner.
	 * 
	 * @param first If true only print the first arrival in the group
	 */
	public void printArrivals(boolean first) {
		Pick pick;
		
		pick = picks.get(0);
		System.out.format("%-5s %-8s %-8s %7.2f %6.2f %3.0f\n", 
				station.staID.staCode, pick.getCurrentPhaseCode(), pick.getOriginalPhaseCode(), pick.getTravelTime(), 
				delta, azimuth);
		if(!first) {
			for(int j=1; j<picks.size(); j++) {
				System.out.format("      %-8s %-8s %7.2f\n", pick.getCurrentPhaseCode(), 
						pick.getOriginalPhaseCode(), pick.getTravelTime());
			}
		}
	}
	
	/**
	 * Print the pick part of a Bulletin Hydra style output file.
	 */
	public void printHydra() {
		Pick pick;
		
		for(int j=0; j<picks.size(); j++) {
			pick = picks.get(j);
			System.out.format("%10s %-5s %-3s %-2s %-2s %-8s%6.1f %5.1f "+
					"%3.0f %1s %4.2f %6.4f\n", pick.getPickID(), station.staID.staCode, 
					pick.getChannelCode(), station.staID.netCode, station.staID.locCode, 
					pick.getCurrentPhaseCode(), pick.getResidual(), delta, azimuth, 
					LocUtil.getBoolChar(pick.getIsUsed()), pick.getWeight(), pick.getImportance());
		}
	}
	
	/**
	 * Print out picks in the group in a way similar to the NEIC web format.
	 */
	public void printNEIC() {
		Pick pick;
    String locCode = station.staID.locCode;
    if (locCode == null) {
      locCode = "";
    }
		for(int j=0; j<picks.size(); j++) {
			pick = picks.get(j);
			switch(pick.getAuthorType()) {
				case CONTRIB_HUMAN: case LOCAL_HUMAN:
					System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s "+
							" manual    %6.1f    %4.2f\n", station.staID.netCode, 
							station.staID.staCode, pick.getChannelCode(), locCode, 
							delta, azimuth, pick.getCurrentPhaseCode(), LocUtil.getNEICTimeString(pick.getArrivalTime()), 
							pick.getResidual(), pick.getWeight());
					break;
				default:
					System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s  "+
							"automatic %6.1f    %4.2f\n", station.staID.netCode, 
							station.staID.staCode, pick.getChannelCode(), locCode, 
							delta, azimuth, pick.getCurrentPhaseCode(), LocUtil.getNEICTimeString(pick.getArrivalTime()), 
							pick.getResidual(), pick.getWeight());
					break;
			}
		}
	}
}
