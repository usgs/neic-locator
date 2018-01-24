package gov.usgs.locator;

import java.util.ArrayList;

import gov.usgs.traveltime.AllBrnVol;
import gov.usgs.traveltime.AuxTtRef;

/**
 * Locate drives the location of one earthquake.
 * 
 * @author Ray Buland
 *
 */
public class Locate {
	Event event;
	Hypocenter hypo;
	ArrayList<HypoAudit> audit;
	AllBrnVol allBrn;
	AuxTtRef auxtt;
	InitialID initialID;
	PhaseID phaseID;
	Stepper stepper;
	CloseOut close;

	/**
	 * Set up the machinery to locate one event.
	 * 
	 * @param event Event information
	 * @param allBrn Travel-time information
	 */
	public Locate(Event event, AllBrnVol allBrn, AuxLocRef auxLoc, 
			AuxTtRef auxTT) {
		this.event = event;
		hypo = event.hypo;
		audit = event.audit;
		this.allBrn = allBrn;
		phaseID = new PhaseID(event, allBrn, auxTT);
		initialID = new InitialID(event, allBrn, phaseID);
		stepper = new Stepper(event, allBrn, phaseID, auxLoc);
		close = new CloseOut();
	}
	
	/**
	 * Location driver.
	 */
	public LocStatus doLoc() {
		int stage = -1, iter = 0;		// Initialize for tracking purposes.
		LocStatus status;
		
		// Save the essentials of this event for comparison.
		event.addAudit(0, 0, LocStatus.INITIAL_HYPOCENTER);
		
		// Bail on insufficient data.
		if(event.stationsUsed < 3) {
			close.endStats(LocStatus.INSUFFICIENT_DATA);
			return LocStatus.INSUFFICIENT_DATA;
		}
		
		try {
			// Handle a held solution.
			if(hypo.heldLoc) {
				// Allow RSTT if requested and reidentify and reweight phases.
				LocUtil.rstt = hypo.cmndRstt;
				stepper.setInitDir(0.1d, 1d, true, true);
				close.endStats(LocStatus.HELD_HYPOCENTER);
				return LocStatus.SUCCESS;
			}
			
			// Print out some event stuff for comparison.
			System.out.println("\n"+event.hypo);
			event.printArrivals(true);
			// Prepare the event for relocation.
			initialID.survey();
			
			/*
			 * Do the multistage iteration to refine the hypocenter.
			 */
			for(stage = 0; stage < LocUtil.STAGELIM; stage++) {
				iter = 0;		// Initialize for tracking purposes.
				switch(stage) {
					case 0:
						// Do the stage 0 phase identification (no reID, but re-weight).
						status = stepper.setInitDir(0.01d,  5d, false, true);
						break;
					case 1:
			//		event.updateEvent(1217617571.841d, 50.1900d, -114.7276d, 1.00d);
			//		System.out.println("\n"+event.hypo);
						// Unless this is a restart, allow phases initially removed.
						if(!hypo.restart) {
							initialID.resetUseFlags();
							initialID.printInitialID();
						}
						// Allow RSTT and force decorrelation.
						LocUtil.rstt = hypo.cmndRstt;
			//		LocUtil.deCorrelate = true;
						// Do a looser phase identification.
						status = stepper.setInitDir(0.1d, 1.0d, true, true);
						break;
					default:
						// Continue using a looser phase identification.
						status = stepper.setInitDir(0.1d, 1.0d, true, true);
						break;
				}
				// Be sure we still have enough data.
				if(status == LocStatus.INSUFFICIENT_DATA) {
					close.endStats(status);
					return status;
				}
				hypo.stepLen = LocUtil.INITSTEP;
				
				// Iterate to convergence (or to the iteration limit).
				step: for(iter = 0; iter < LocUtil.ITERLIM[stage]; iter++) {
					stepper.makeStep(stage, iter);
					switch(status) {
					// Bail on insufficient data.
					case INSUFFICIENT_DATA:
						close.endStats(status);
						return status;
					// If the phase identifications have changed, start the 
					//iteration over.
					case PHASEID_CHANGED:
						iter = -1;
						break;
					// If the damping failed, go to the next stage.
					case NEARLY_CONVERGED:
					case DID_NOT_CONVERGE:
					case UNSTABLE_SOLUTION:
						break step;
					// Otherwise, keep on trucking!
					default:
						break;
					}
					// Check for convergence.
					if(hypo.stepLen <= LocUtil.CONVLIM[stage]) break;
				}
				hypo.delH = LocUtil.delStep(hypo, audit.get(audit.size()-1));
				hypo.delZ = Math.abs(hypo.depth-audit.get(audit.size()-1).depth);
				hypo.stepLen = Math.sqrt(Math.pow(hypo.delH,2d)+
						Math.pow(hypo.delZ,2d));
				if(stage >= 1 && hypo.stepLen <= LocUtil.CONVLIM[stage]) {
					hypo.delH = LocUtil.delStep(hypo, audit.get(0));
					hypo.delZ = Math.abs(hypo.depth-audit.get(0).depth);
					hypo.stepLen = Math.sqrt(Math.pow(hypo.delH,2d)+
							Math.pow(hypo.delZ,2d));
					event.addAudit(stage, iter, status);
					status = close.endStats(status);
					return status;
				} else {
					event.addAudit(stage, iter, status);
				}
			}
			return LocStatus.DID_NOT_CONVERGE;
			
		} catch (Exception e) {
			// This should never happen.
			System.out.println("Source depth out of range");
			e.printStackTrace();
			return LocStatus.BAD_DEPTH;
		}
	}
}
