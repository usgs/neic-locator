package gov.usgs.locator;

/**
 * Associate theoretical seismic phases with observed seismic picks.
 * 
 * @author Ray Buland
 *
 */
public class PhaseID {
	Event event;
	AllBrnVol allBrn;
	AuxTtRef auxtt;
	Hypocenter hypo;
	Pick pick, lastPick = null;
	TTimeData tTime;
	double otherWeight;				// Weight for phases that don't match
	double stickyWeight;			// Weight to resist changing identification
	boolean generic = false;
	String phGroup = null;
	double trialAff = Double.NaN;

	/**
	 * Remember the event and travel-time machinery.
	 * 
	 * @param event Event object
	 * @param allBrn All branches travel-time object
	 */
	public PhaseID(Event event, AllBrnVol allBrn, AuxTtRef auxtt) {
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
	public void doID(double otherWeight, double stickyWeight) {
		TTime ttList;
		Station station;
		PickGroup group;
		double prob, amp, aff;
		
		// Remember the figure-of-merit controls.
		this.otherWeight = otherWeight;
		this.stickyWeight = stickyWeight;
		
		// Do the travel-time calculation.
		for(int j=0; j<event.groups.size(); j++) {
			group = event.groups.get(j);
			if(group.picksUsed() > 0) {
				// For the first pick in the group, get the travel times.
				station = group.station;
				ttList = allBrn.getTT(station.latitude, station.longitude, 
						station.elevation, group.delta, group.azimuth, true, 
						false, false, false);
				// Print them.
		//	ttList.print(event.hypo.depth, pick.delta);
				pick = group.picks.get(0);
				if(pick.used) {
					/*
					 * For now, just exercise the figure-of-merit machinery.
					 */
					for(int k=0; k<ttList.size(); k++) {
						tTime = ttList.get(k);
						prob = LocUtil.ttResModel(pick.tt-tTime.tt, 0d, tTime.spread);
						amp = idAmplitude();
						aff = idAffinity();
						System.out.format("%-8s (%-8s) vs %-8s: %8.2e %8.2e %3.1f\n", 
								pick.idCode, pick.phCode, tTime.phCode, prob, amp, aff);
					}
				}
				// For the rest, we've already done part of the work.
				for(int i=1; i<group.picks.size(); i++) {
					pick = group.picks.get(i);
						if(pick.used) {
						for(int k=0; k<ttList.size(); k++) {
							tTime = ttList.get(k);
							prob = LocUtil.ttResModel(pick.tt-tTime.tt, 0d, tTime.spread);
							amp = idAmplitude();
							aff = idAffinity();
							System.out.format("%-8s (%-8s) vs %-8s: %8.2e %8.2e %3.1f\n", 
									pick.idCode, pick.phCode, tTime.phCode, prob, amp, aff);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Compute the modified theoretical phase "amplitude".  This is the phase 
	 * observability with empirical modifiers to reflect how closely it matches 
	 * the observed phase.  Note that this is a complicated function of who 
	 * identified the observed phase, if they are in the same phase group, and 
	 * if they have the same phase type.  The sticky weight promotes stability 
	 * by tending to keep the old identification all else being equal.
	 * 
	 * @return
	 */
	private double idAmplitude() {
		double amp;
		
		// Set up the observed pick phase group.
		if(pick != lastPick) {
			lastPick = pick;
			phGroup = auxtt.findGroup(pick.idCode, (pick.authType == 
					AuthorType.CONTRIB_AUTO));
			if(pick.idCode.equals("") || pick.idCode.equals(phGroup)) 
				generic = true;
			else generic = false;
		}
		
		// initialize the amplitude.
		if(!tTime.dis) amp = tTime.observ;
		else amp = LocUtil.DOWNWEIGHT*tTime.observ;
		
		// Do the group logic.  If the phase codes match drop through 
		// unless the phase might be generic.
		if((!pick.idCode.equals(tTime.phCode) || generic) && 
			!phGroup.equals("all")) {
			// If the observed phase is in the same group as the 
			// theoretical phase use the group weighting.
			if(phGroup.equals(tTime.phGroup) || 
					phGroup.equals(tTime.auxGroup)) {
						amp *= LocUtil.GROUPWEIGHT;
			}
			// Otherwise use the other (non-group) weighting.
			else {
				amp *= otherWeight;
				// If we trust the phase identification and the arrival types 
				// of the phases don't match, make re-identifying even harder
				if((pick.authType == AuthorType.CONTRIB_HUMAN || 
					pick.authType == AuthorType.LOCAL_HUMAN) && 
					TauUtil.arrivalType(phGroup) != 
					TauUtil.arrivalType(tTime.phCode)) {
						amp *= LocUtil.TYPEWEIGHT;
				}
			}
		}
		
		// Account for the affinity.
		if(pick.idCode.equals(tTime.phCode)) {
			trialAff = pick.affinity;
			amp *= trialAff;
		} else {
			trialAff = LocUtil.NULLAFFINITY;
		}
		
		// Make the existing identification harder to change.
		if(pick.phCode.equals(tTime.phCode)) {
			amp *= stickyWeight;
		}
		return amp;
	}
	
	/**
	 * Get the trial affinity computed by the last call to idAmplitude.
	 * 
	 * @return The trial affinity.
	 */
	private double idAffinity() {
		return trialAff;
	}
}
