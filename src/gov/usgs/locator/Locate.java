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
	 * @param auxLoc Auxiliary location information
	 * @param auxTT Auxiliary travel-time information
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
		close = new CloseOut(event);
		LocUtil.deCorrelate = false;
		LocUtil.rstt = false;
	}
	
	/**
	 * Location driver.
	 */
	public LocStatus doLoc() {
		int stage = -1, iter = 0;		// Initialize for tracking purposes.
		boolean bail;
		LocStatus status;
		
		// Save the essentials of this event for comparison.
		event.addAudit(0, 0, LocStatus.INITIAL_HYPOCENTER);
		
		// Bail on insufficient data.
		if(event.staUsed < 3) {
			close.endStats(LocStatus.INSUFFICIENT_DATA);
			return LocStatus.INSUFFICIENT_DATA;
		}
		
		try {
			// Handle a held solution.
			if(event.heldLoc) {
				// Allow RSTT if requested and reidentify and reweight phases.
				LocUtil.rstt = event.cmndRstt;
				stepper.setInitDir(0.1d, 1d, true, true);
				close.endStats(LocStatus.HELD_HYPOCENTER);
				return LocStatus.SUCCESS;
			}
			
			// Prepare the event for relocation.
			initialID.survey();
			// Print out the result.
			if(LocUtil.deBugLevel > 0) initialID.printInitialID();
			
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
						// Unless this is a restart, allow phases initially removed.
						if(!event.restart) {
							initialID.resetUseFlags();
						}
						// Allow RSTT and force decorrelation.
			//		LocUtil.rstt = event.cmndRstt;
						LocUtil.deCorrelate = true;
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
				// Initialize for iteration zero.
				hypo.stepLen = LocUtil.INITSTEP;
				bail = false;
				// Iterate to convergence (or to the iteration limit).
				for(iter = 0; iter < LocUtil.ITERLIM[stage]; iter++) {
					// Step.
					stepper.makeStep(stage, iter);
					switch(status) {
					// Bail on insufficient data.
					case INSUFFICIENT_DATA:
						close.endStats(status);
						return status;
					// If the damping failed, go to the next stage.
					case NEARLY_CONVERGED:
					case DID_NOT_CONVERGE:
					case UNSTABLE_SOLUTION:
						bail = true;
						break;
					// Otherwise, keep on trucking!  (This includes phase 
					// re-identification).
					default:
						break;
					}
					// Check for convergence.
					if(hypo.stepLen <= LocUtil.CONVLIM[stage] || bail) break;
				}
				// We're done with this stage.  Collect information for a stage 
				// level audit instance.
				hypo.delH = LocUtil.delStep(hypo, audit.get(audit.size()-1));
				hypo.delZ = Math.abs(hypo.depth-audit.get(audit.size()-1).depth);
				hypo.stepLen = Math.sqrt(Math.pow(hypo.delH,2d)+
						Math.pow(hypo.delZ,2d));
				// If we've converged, create a final location level audit.
				if(stage > 0 && hypo.stepLen <= LocUtil.CONVLIM[stage]) {
					hypo.delH = LocUtil.delStep(hypo, audit.get(0));
					hypo.delZ = Math.abs(hypo.depth-audit.get(0).depth);
					hypo.stepLen = Math.sqrt(Math.pow(hypo.delH,2d)+
							Math.pow(hypo.delZ,2d));
					event.addAudit(stage, iter, status);
					System.out.println("Final wrapup:");
					event.audit.get(audit.size()-1).printAudit();
					status = close.endStats(status);
					return status;
				// Otherwise, create the stage level audit.
				} else {
					event.addAudit(stage, iter, status);
					System.out.println("Stage wrapup:");
					event.audit.get(audit.size()-1).printAudit();
				}
			}
			// If we go to full interations on the last stage, give up.
			return LocStatus.DID_NOT_CONVERGE;
			
		} catch (Exception e) {
			// This should never happen.
			System.out.println("Source depth out of range");
			e.printStackTrace();
			return LocStatus.BAD_DEPTH;
		}
	}
}
