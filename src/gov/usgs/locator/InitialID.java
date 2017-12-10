package gov.usgs.locator;

import gov.usgs.traveltime.AllBrnVol;
import gov.usgs.traveltime.TTime;
import gov.usgs.traveltime.TTimeData;

/**
 * Before any location iteration or real phase identification 
 * takes place, this initial pass ensures that we have something 
 * reasonable to work with by emphasizing crust and mantle P 
 * waves and manually identified phases.  If there are a lot of 
 * apparently misidentified first arrivals, the algorithm gets 
 * even more draconian.
 * 
 * @author Ray Buland
 *
 */
public class InitialID {
	Event event;
	AllBrnVol allBrn;

	/**
	 * Remember the event and travel times.
	 * 
	 * @param event Event information
	 * @param allBrn Travel-time information
	 */
	public InitialID(Event event, AllBrnVol allBrn) {
		this.event = event;
		this.allBrn = allBrn;
	}
	
	/**
	 * Do a tentative phase identification to see if the event is 
	 * making sense.
	 * 
	 * @return Count of automatic first arrivals that appear to be 
	 * incorrectly identified
	 */
	public int survey() {
		int badPs = 0;
		boolean found = false;
		PickGroup group;
    Station station;
    Pick pick;
    String phCode;
		TTime ttList;
    TTimeData tTime;

    // Loop over picks in the group.
    for (int j = 0; j < event.groups.size(); j++) {
      group = event.groups.get(j);
      if (group.picksUsed() > 0) {
        // For the first pick in the group, get the travel times.
        station = group.station;
        System.out.println("\n" + station + ":");
        // Do the travel-time calculation.
        ttList = allBrn.getTT(station.latitude, station.longitude,
                station.elevation, group.delta, group.azimuth, LocUtil.USEFUL,
                LocUtil.tectonic, LocUtil.NOBACKBRN, LocUtil.rstt);
        // Print them.
        ttList.print(event.hypo.depth, group.delta);
        
        /*
         * Based on a tentative ID, just compute residuals and weights so 
         * that a robust estimate of the origin time correction can be 
         * made.  Without this step, the actual phase identification may 
         * not work correctly.  Note that only some of the first arrivals 
         * that are being used are considered and that the tentative ID is 
         * not remembered.
         */
        if(group.delta <= 100d) {
	        pick = group.picks.get(0);
	        if(pick.used) {
	        	phCode = pick.phCode;
	        	if(!phCode.substring(0,1).equals("PK") &&
	        			!phCode.substring(0,1).equals("P'") && 
	        			!phCode.substring(0,1).equals("Sc") && 
	        			!phCode.equals("Sg") && !phCode.equals("Sb") && 
	        			!phCode.equals("Sn") && !phCode.equals("Lg")) {
	        		if(pick.auto) {
		        		tTime = ttList.get(0);
	        			if(!phCode.equals(tTime.getPhCode())) badPs++;
	        			pick.residual = pick.tt-tTime.getTT();
        				pick.weight = 1d/tTime.getSpread();
	        		} else {
	        			int i;
	        			for(i=0; i<ttList.size(); i++) {
	  	        		tTime = ttList.get(i);
	        				if(phCode.equals(tTime.getPhCode())) {
	        					// Note that this is slightly different from the Fortran 
	        					// version where the weight is always from the first arrival.
	        					pick.residual = pick.tt-tTime.getTT();
	        					pick.weight = 1d/tTime.getSpread();
	        					found = true;
	        					break;
	        				}
	        			}
	        			if(!found) {
	  	        		tTime = ttList.get(i);
	        				pick.residual = pick.tt-tTime.getTT();
	        				pick.weight = 1d/tTime.getSpread();
	        			}
	        		}
	        		/*
	        		 * Push the first arrival phase residuals and weights here.  The 
	        		 * origin time correction will be computed below.
	        		 */
	        		System.out.format("InitialID push: %-8s %5.1f %5.1f\n", pick.phCode, 
	        				pick.residual, pick.weight);
	        	}
	        }
        }
      }
    }
  	/*
  	 * Update the hypocenter origin time based on the residuals and weights pushed 
  	 * by the survey method.  Adjusting the origin time to something reasonable 
  	 * ensures that succeeding phase identifications have a chance.
  	 */
    /*
     * Compute the origin time correction here.
     */
		return badPs;
	}
	
	/**
	 * if the event seems to be making sense (i.e., not too many misidentified first 
	 * arrivals), we can go easy on the initial phase identification.
	 */
	public void doIdEasy() {
		PickGroup group;
		Pick pick;
		String phCode;
		
		// Loop over groups assessing automatic picks.
    for (int j = 0; j < event.groups.size(); j++) {
      group = event.groups.get(j);
      if (group.picksUsed() > 0) {
      	pick = group.picks.get(0);
      	// If the first arrival is automatic and not a crust or mantle P, don't use it.
      	if(pick.auto && pick.used) {
      		phCode = pick.phCode;
      		if(!phCode.equals("Pg") && !phCode.equals("Pb") && !phCode.equals("Pn") && 
      				!phCode.equals("P")) pick.used = false;
      	}
      	// Don't use any secondary automatic phases.
      	for(int i=1; i<group.picks.size(); i++) {
      		pick = group.picks.get(i);
      		if(pick.auto && pick.used) pick.used = false;
      	}
      }
    }
	}
	
	/**
	 * If the event doesn't seem to be making sense (i.e., too many misidentified 
	 * first arrivals), we need to be stricter about the initial phase identification.
	 */
	public void doIdHard() {
		PickGroup group;
		Station station;
		Pick pick;
		String phCode;
		TTime ttList;
		
		// Loop over groups forcing automatic phases to conform.
    for (int j = 0; j < event.groups.size(); j++) {
      group = event.groups.get(j);
      if (group.picksUsed() > 0) {
      	pick = group.picks.get(0);
      	// If the first arrival is automatic and might be a misidentified first arrival, 
      	// force it to be the first theoretical arrival.
      	if(pick.auto && pick.used) {
      		phCode = pick.phCode;
        	if(group.delta <= 100d && !phCode.substring(0,1).equals("PK") &&
        			!phCode.substring(0,1).equals("P'") && 
        			!phCode.substring(0,1).equals("Sc") && 
        			!phCode.equals("Sg") && !phCode.equals("Sb") && 
        			!phCode.equals("Sn") && !phCode.equals("Lg")) {
        		// For the first pick in the group, get the travel times.
        		station = group.station;
        		System.out.println("\n" + station + ":");
        		ttList = allBrn.getTT(station.latitude, station.longitude,
                station.elevation, group.delta, group.azimuth, true,
                false, false, false);
        		// Print them.
        		ttList.print(event.hypo.depth, group.delta);
        		// Set the phase code.  The travel time was already set in survey.
        		pick.phCode = ttList.get(0).getPhCode();
        	// If it's a core phase or not a common misidentification, just don't use it.
        	} else {
        		pick.used = false;
        	}
        }
      	// Don't use any secondary automatic phases.
      	for(int i=1; i<group.picks.size(); i++) {
      		pick = group.picks.get(i);
      		if(pick.auto && pick.used) pick.used = false;
      	}
      }
    }
	}
	
	/**
	 * After refining the starting location based on the initial phase identification 
	 * and doing a more rigorous phase identification, we can put some of phases that 
	 * were temporarily taken out of the location back in.
	 */
	public void resetUseFlags() {
		PickGroup group;
		Pick pick;
		
		// This simply resets no-used phases back to their initial input state.
		for(int j=0; j<event.groups.size(); j++) {
			group = event.groups.get(j);
			for(int i=0; i<group.picks.size(); i++) {
				pick = group.picks.get(i);
				if(!pick.used) pick.used = pick.cmndUse;
			}
		}
	}
	
	/**
	 * List the phases used in the initial relocation.  Note that them may have been 
	 * re-identified after the initialID algorithm.
	 */
	public void printInitialID() {
		PickGroup group;
		Station station;
		Pick pick;
		
		System.out.println("\n\tInitial phase identification:");
		for(int j=0; j<event.groups.size(); j++) {
			group = event.groups.get(j);
      if (group.picksUsed() > 0) {
      	station = group.station;
				for(int i=0; i<group.picks.size(); i++) {
					pick = group.picks.get(i);
					if(pick.used) System.out.format("%-5s %-8s %6.1f %6.1f %3.0f %5.2f\n", 
							station.staID.staCode, pick.phCode, pick.residual, group.delta, 
							group.azimuth, pick.weight);
				}
      }
		}
	}
}
