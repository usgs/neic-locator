package gov.usgs.locator;

import java.util.ArrayList;

import gov.usgs.traveltime.TTSessionLocal;
import gov.usgs.traveltime.TTime;
import gov.usgs.traveltime.TTimeData;
import gov.usgs.traveltime.session.TTSession;
import gov.usgs.traveltime.session.TTSessionPool;

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
	Hypocenter hypo;
	TTSessionLocal ttLocal;
	PhaseID phaseID;
  ArrayList<Wresidual> wResiduals;
  Restimator rEst;
  Stepper stepper;
  TTSession session;

	/**
	 * Remember the event and travel times.
	 * 
	 * @param event Event information
	 * @param ttLocal Local travel-time manager
	 * @param phaseID Phase identification logic
	 * @param stepper R-estimator driver logic
	 */
	public InitialID(Event event, TTSessionLocal ttLocal, PhaseID phaseID, 
			Stepper stepper) {
		this.event = event;
		hypo = event.hypo;
		this.ttLocal = ttLocal;
		this.phaseID = phaseID;
		wResiduals = event.wResRaw;
		rEst = event.rEstRaw;
		this.stepper = stepper;
	}
	
	/**
	 * Do a tentative phase identification to see if the event is 
	 * making sense.
	 * 
	 * @throws Exception On an illegal source depth
	 */
	public void survey() throws Exception {
		int badPs = 0;
		boolean found;
		PickGroup group;
    Station station;
    Pick pick;
    String phCode;
		TTime ttList = null;
    TTimeData tTime;
		
		// Reinitialize the weighted residual storage.
		if(wResiduals.size() > 0) wResiduals.clear();
		
		// Set up a new travel-time session if the depth has changed.
		if(LocUtil.server) {
			session = TTSessionPool.getTravelTimeSession(event.getEarthModel(), hypo.depth, 
					LocUtil.PHLIST, hypo.latitude, hypo.longitude, LocUtil.ALLPHASES,
					LocUtil.BACKBRN, LocUtil.tectonic, false, false);
		} else {
			ttLocal.newSession(event.getEarthModel(), hypo.depth, LocUtil.PHLIST, hypo.latitude, 
					hypo.longitude, LocUtil.ALLPHASES, LocUtil.BACKBRN, LocUtil.tectonic, 
					false);
		}
		
    // Loop over picks in the groups.
		if(LocUtil.deBugLevel > 1) System.out.println();
    for (int j = 0; j < event.noStations(); j++) {
      group = event.groups.get(j);
      if (group.picksUsed() > 0) {
        // For the first pick in the group, get the travel times.
        station = group.station;
        if(LocUtil.deBugLevel > 1) System.out.println("InitialID: "+
        		station+":");
        // Do the travel-time calculation.
        if(LocUtil.server) {
        	ttList = session.getTT(station.latitude, station.longitude,
        			station.elevation, group.delta, group.azimuth);
        } else {
	        ttList = ttLocal.getTT(station.latitude, station.longitude,
	            station.elevation, group.delta, group.azimuth);
        }
        // Print them.
  //    ttList.print(event.hypo.depth, group.delta);
        tTime = ttList.get(0);
        
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
        				if(LocUtil.deBugLevel > 1 &&!phCode.equals(tTime.getPhCode())) 
        						System.out.format("InitialID: %-8s -> %-8s auto\n", phCode, 
        								tTime.getPhCode());
	        		} else {
	        			found = false;
	        			for(int i=0; i<ttList.size(); i++) {
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
	  	        		tTime = ttList.get(0);
	        				pick.residual = pick.tt-tTime.getTT();
	        				pick.weight = 1d/tTime.getSpread();
	        				if(LocUtil.deBugLevel > 1) System.out.format("InitialID: "+
	        						"%-8s -> %-8s human\n", phCode, tTime.getPhCode());
	        			}
	        		}
	        		wResiduals.add(new Wresidual(pick, pick.residual, pick.weight, false, 
	        				0d, 0d, 0d));
	        		if(LocUtil.deBugLevel > 1) System.out.format("InitialID push: "+
	        				"%-5s %-8s %5.2f %7.4f %5.2f %5.2f\n", pick.station.staID.staCode, 
	        				pick.phCode, pick.residual, pick.weight, tTime.getTT(), 
	        				tTime.getSpread());
	        	}
	        }
        }
      }
    }
    // Add in the Bayesian depth because the R-estimator code expects it.
    wResiduals.add(new Wresidual(null, hypo.depthRes, hypo.depthWeight, true, 
    		0d, 0d, 0d));
  	/*
  	 * Update the hypocenter origin time based on the residuals and weights pushed 
  	 * by the survey method.  Adjusting the origin time to something reasonable 
  	 * ensures that succeeding phase identifications have a chance.
  	 */
    double median = rEst.median();
    event.updateOrigin(median);
    if(LocUtil.deBugLevel > 0) System.out.format("\nUpdate origin: %f %f %f %d\n", 
    		hypo.originTime, median, hypo.originTime+median, badPs);
    
		// On a restart, reidentify all phases to be consistent with the new hypocenter.  
    // Note that we still needed the logic above to reset the origin time.
		if(event.getIsLocationRestarted()) {
			stepper.setEnviron();
			phaseID.doID(0.1d, 1d, true, true);
			event.staStats();
			return;
		}
		
    // Based on the number of probably misidentified first arrivals:
		if(LocUtil.deBugLevel > 1) System.out.println();
		if(badPs < LocUtil.BADRATIO*event.staUsed) {
			// Just make the obvious re-identifications (i.e., autos).
			doIdEasy();
		} else {
			// Re-identify any first arrival that doesn't look right.
			doIdHard();
		}
	}
	
	/**
	 * if the event seems to be making sense (i.e., not too many misidentified first 
	 * arrivals), we can go easy on the initial phase identification.
	 */
	private void doIdEasy() {
		PickGroup group;
		Pick pick;
		String phCode;
		
		// Loop over groups assessing automatic picks.
    for (int j = 0; j < event.noStations(); j++) {
      group = event.groups.get(j);
      if (group.picksUsed() > 0) {
      	pick = group.picks.get(0);
      	// If the first arrival is automatic and not a crust or mantle P, don't use it.
      	if(pick.auto && pick.used) {
      		phCode = pick.phCode;
      		if(!phCode.equals("Pg") && !phCode.equals("Pb") && !phCode.equals("Pn") && 
      				!phCode.equals("P")) {
      			pick.used = false;
      			if(LocUtil.deBugLevel > 1) System.out.format("\tIdEasy: don't use %-5s "+
      					"%-8s\n", group.station.staID.staCode, pick.phCode);
      		}
      	}
      	// Don't use any secondary automatic phases.
      	for(int i=1; i<group.noPicks(); i++) {
      		pick = group.picks.get(i);
      		if(pick.auto && pick.used) {
      			pick.used = false;
      			if(LocUtil.deBugLevel > 1) System.out.format("\tIdEasy: don't use %-5s "+
      					"%-8s\n", group.station.staID.staCode, pick.phCode);
      		}
      	}
      }
    }
	}
	
	/**
	 * If the event doesn't seem to be making sense (i.e., too many misidentified 
	 * first arrivals), we need to be stricter about the initial phase identification.
	 */
	private void doIdHard() {
		PickGroup group;
		Station station;
		Pick pick;
		String phCode;
		TTime ttList;
		
		// Loop over groups forcing automatic phases to conform.
    for (int j = 0; j < event.noStations(); j++) {
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
        		if(LocUtil.deBugLevel > 1) System.out.println("" + station + ":");
            // Do the travel-time calculation.
            if(LocUtil.server) {
            	ttList = session.getTT(station.latitude, station.longitude,
            			station.elevation, group.delta, group.azimuth);
            } else {
    	        ttList = ttLocal.getTT(station.latitude, station.longitude,
    	            station.elevation, group.delta, group.azimuth);
            }
        		// Print them.
    //  		ttList.print(event.hypo.depth, group.delta);
        		// Set the phase code.  The travel time was already set in survey.
        		pick.updateID(ttList.get(0).getPhCode());
        		if(LocUtil.deBugLevel > 1) System.out.format("\tIdHard: %-5s %-8s "+
        				"-> %-8s auto\n", group.station.staID.staCode, phCode, 
        				ttList.get(0).getPhCode());
        	// If it's a core phase or not a common mis-identification, just don't use it.
        	} else {
        		pick.used = false;
        		if(LocUtil.deBugLevel > 1) System.out.format("\tIdHard: don't use "+
        				"%-5s %-8s\n", group.station.staID.staCode, pick.phCode);
        	}
        }
      	// Don't use any secondary automatic phases.
      	for(int i=1; i<group.noPicks(); i++) {
      		pick = group.picks.get(i);
      		if(pick.auto && pick.used) {
      			pick.used = false;
      			if(LocUtil.deBugLevel > 1) System.out.format("\tIdHard: don't use "+
      					"%-5s %-8s\n", group.station.staID.staCode, pick.phCode);
      		}
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
		for(int j=0; j<event.noStations(); j++) {
			group = event.groups.get(j);
			for(int i=0; i<group.noPicks(); i++) {
				pick = group.picks.get(i);
				if(!pick.used) pick.used = pick.cmndUse;
			}
		}
	}
	
	/**
	 * List the phases used in the initial relocation.  Note that they may have been 
	 * re-identified after the initialID algorithm.
	 */
	public void printInitialID() {
		PickGroup group;
		Station station;
		Pick pick;
		
		System.out.println("\nInitial phase identification:");
		for(int j=0; j<event.noStations(); j++) {
			group = event.groups.get(j);
      if (group.picksUsed() > 0) {
      	station = group.station;
				for(int i=0; i<group.noPicks(); i++) {
					pick = group.picks.get(i);
					if(pick.used) System.out.format("%-5s %-8s %6.1f %6.1f %3.0f %5.2f\n", 
							station.staID.staCode, pick.phCode, pick.residual, group.delta, 
							group.azimuth, pick.weight);
				}
      }
		}
	}
}
