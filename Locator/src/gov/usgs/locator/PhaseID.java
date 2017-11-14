package gov.usgs.locator;

/**
 * Associate theoretical seismic phases with observed seismic picks.
 * 
 * @author Ray Buland
 *
 */
public class PhaseID {
	String[] phList = null;
	Event event;
	Hypocenter hypo;
	AllBrnVol allBrn;

	/**
	 * Remember the event and travel-time machinery.
	 * 
	 * @param event Event object
	 * @param allBrn All branches travel-time object
	 */
	public PhaseID(Event event, AllBrnVol allBrn) {
		this.event = event;
		this.allBrn = allBrn;
		hypo = event.hypo;
	}
	
	/**
	 * Driver for the phase identification.
	 * 
	 * @param all If true, identify all phases rather than just 
	 * used phases
	 */
	public void doID(boolean all) {
		Pick pick;
		Station sta;
		TTime ttList;
		
		// Set up a new session.
		try {
			allBrn.newSession(event.hypo.latitude, event.hypo.longitude, 
					event.hypo.depth, phList);
//		allBrn.dumpCorrUp('P', true);
//		allBrn.dumpDecUp('P', true);
			
			// Do the travel-time calculation.
			if(all) {
				// For the event close out, identify all phases.
				for(int j=0; j<event.allPicks.size(); j++) {
					pick = event.allPicks.get(j);
					sta = pick.station;
					// Get the travel times.
					ttList = allBrn.getTT(sta.latitude, sta.longitude, sta.elevation, 
							pick.delta, pick.azimuth, true, false, false, false);
					// Print them.
					ttList.print(event.hypo.depth, pick.delta);
				}
			} else {
				// For the location iteration we only need used phases.
				for(int j=0; j<event.usedPicks.size(); j++) {
					pick = event.usedPicks.get(j);
					sta = pick.station;
					System.out.println("\n"+sta.staID.staCode+": "+(float)pick.delta+
							" "+(float)pick.azimuth);
					// Get the travel times.
					ttList = allBrn.getTT(sta.latitude, sta.longitude, sta.elevation, 
							pick.delta, pick.azimuth, true, false, false, false);
					// Print them.
					ttList.print(event.hypo.depth, pick.delta);
				}
			}
		} catch(Exception e) {
			System.out.println("Source depth out of range");
			e.printStackTrace();
		}
	}
}
