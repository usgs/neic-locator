package gov.usgs.locator;

import java.util.ArrayList;

import gov.usgs.traveltime.TTSessionLocal;

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
	TTSessionLocal ttLocal;
	InitialID initialID;
	PhaseID phaseID;
	Stepper stepper;
	CloseOut close;

	/**
	 * Set up the machinery to locate one event.
	 * 
	 * @param event Event information
	 * @param ttLocal Travel-time information for a local implementation
	 * @param auxLoc Auxiliary location information
	 */
	public Locate(Event event, TTSessionLocal ttLocal, AuxLocRef auxLoc) {
		this.event = event;
		hypo = event.getHypo();
		audit = event.getHypoAuditList();
		this.ttLocal = ttLocal;
		phaseID = new PhaseID(event, ttLocal);
		stepper = new Stepper(event, phaseID, auxLoc);
		initialID = new InitialID(event, ttLocal, phaseID, stepper);
		close = new CloseOut(event);
		LocUtil.deCorrelate = false;
	}
	
	/**
	 * Location driver.
	 * 
	 * @return Final location status
	 */
	public LocStatus doLoc() {
		int stage = -1, iter = 0;		// Initialize for tracking purposes.
		boolean bail;
		LocStatus status;
		
		// Save the essentials of this event for comparison.
		event.addAudit(0, 0, LocStatus.INITIAL_HYPOCENTER);
		
		// Bail on insufficient data.
		if(event.getNumStationsUsed() < 3) {
			close.computeFinalStatistics(LocStatus.INSUFFICIENT_DATA);
			return LocStatus.INSUFFICIENT_DATA;
		}
		
		try {
			// Handle a held solution.
			if(event.getIsLocationHeld()) {
				// Rreidentify and reweight phases.
				LocUtil.deCorrelate = event.getUseDecorrelation();
				stepper.setInitDir(0.1d, 1d, true, true);
				close.computeFinalStatistics(LocStatus.HELD_HYPOCENTER);
				return LocStatus.SUCCESS;
			}
			
			// Prepare the event for relocation.
			initialID.survey();
			if(LocUtil.deBugLevel > 3) initialID.printInitialID();
			
			/*
			 * Do the multistage iteration to refine the hypocenter.
			 */
			for(stage = 0; stage < LocUtil.STAGELIM; stage++) {
				switch(stage) {
					case 0:
						// Do the stage 0 phase identification (no reID, but re-weight).
						status = stepper.setInitDir(0.01d,  5d, false, true);
						break;
					case 1:
						// Unless this is a restart, allow phases initially removed.
						if(!event.getIsLocationRestarted()) {
							initialID.resetUseFlags();
						}
						// Force decorrelation.
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
					close.computeFinalStatistics(status);
					return status;
				}
				// Initialize for iteration zero.
				hypo.setStepLength(LocUtil.INITSTEP);
				bail = false;
				// Iterate to convergence (or to the iteration limit).
				for(iter = 0; iter < LocUtil.ITERLIM[stage]; iter++) {
					// Step.
					stepper.makeStep(stage, iter);
					switch(status) {
					// Bail on insufficient data.
					case INSUFFICIENT_DATA:
						close.computeFinalStatistics(status);
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
					if(hypo.getStepLength() <= LocUtil.CONVLIM[stage] || bail) break;
				}
				// We're done with this stage.  Collect information for a stage 
				// level audit instance.
				if(iter >= LocUtil.ITERLIM[stage]) status = LocStatus.FULL_ITERATIONS;
				hypo.setHorizontalStepLength(LocUtil.delStep(hypo, audit.get(audit.size()-1)));
				hypo.setVerticalStepLength(Math.abs(hypo.getDepth()-audit.get(audit.size()-1).getDepth()));
				hypo.setStepLength(Math.sqrt(Math.pow(hypo.getHorizontalStepLength(),2d)+
						Math.pow(hypo.getVerticalStepLength(),2d)));
				
				// If we've converged, create a final location level audit.
				if(stage > 0 && hypo.getStepLength() <= LocUtil.CONVLIM[stage]) {
					hypo.setHorizontalStepLength(LocUtil.delStep(hypo, audit.get(0)));
					hypo.setVerticalStepLength(Math.abs(hypo.getDepth()-audit.get(0).getDepth()));
					hypo.setStepLength(Math.sqrt(Math.pow(hypo.getHorizontalStepLength(),2d)+
							Math.pow(hypo.getVerticalStepLength(),2d)));
					event.addAudit(stage, iter, status);
					System.out.println("\nFinal wrapup:");
					event.printHypoAudit();
					status = close.computeFinalStatistics(status);
					return status;
				// Otherwise, create the stage level audit.
				} else {
					event.addAudit(stage, iter, status);
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
