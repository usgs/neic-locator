package gov.usgs.locator;

import gov.usgs.locaux.AuxLocRef;
import gov.usgs.locaux.LocUtil;
import gov.usgs.traveltime.BadDepthException;
import gov.usgs.traveltime.TTSessionLocal;
import gov.usgs.traveltime.tables.TauIntegralException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The Locate class drives the location of one earthquake.
 *
 * @author Ray Buland
 */
public class Locate {
  /** An Event object containing the event to locate. */
  private Event event;

  /** A Hypocenter object containing the hypocenter of event to locate. */
  private Hypocenter hypo;

  /**
   * An ArrayList of HypoAudit objects containing the hypocenter auditing information during this
   * location.
   */
  private ArrayList<HypoAudit> hypoAuditList;

  /**
   * A InitialPhaseID object used to perform initial phase identification before any location
   * iterations.
   */
  private InitialPhaseID initialPhaseID;

  /**
   * A Stepper object used to manage the rank-sum estimation logic needed to refine the phase
   * identifications and location.
   */
  private Stepper stepper;

  /**
   * A CloseOut object used to computes all the errors and heuristics used to evaluate the location
   * after an event is located.
   */
  private CloseOut close;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(Locate.class.getName());

  /**
   * The Locate constructor. Sets up the class to locate a single event.
   *
   * @param event An Event object containing the Event to locate
   * @param ttLocalSession A TTSessionLocal object containing the travel-time information for a
   *     local implementation to use in computing the location
   * @param auxLoc An AuxLocRef object containing auxiliary location information such as continental
   *     craton boundaries and earthquake statistics
   */
  public Locate(Event event, TTSessionLocal ttLocalSession, AuxLocRef auxLoc) {
    this.event = event;
    hypo = event.getHypo();
    hypoAuditList = event.getHypoAuditList();
    PhaseID phaseID = new PhaseID(event, ttLocalSession);
    stepper = new Stepper(event, phaseID, auxLoc);
    initialPhaseID = new InitialPhaseID(event, ttLocalSession, phaseID, stepper);
    close = new CloseOut(event);
    LocUtil.useDecorrelation = false;
  }

  /**
   * This function performs the location for the event.
   *
   * @return A LocStatus object containing the final location status
   */
  public LocStatus doLocation() {
    LOGGER.info("Starting Location");

    // Save the essentials of this event for comparison.
    event.addAudit(0, 0, LocStatus.INITIAL_HYPOCENTER);

    // Bail on insufficient data.
    if (event.getNumStationsUsed() < 3) {
      LOGGER.info("Insufficient Data (Stations Used)");
      close.compFinalStats(LocStatus.INSUFFICIENT_DATA);
      return LocStatus.INSUFFICIENT_DATA;
    }

    try {
      // Handle a held solution.
      if (event.getIsLocationHeld()) {
        LOGGER.info("Held Location");
        // Reidentify and reweight phases.
        LocUtil.useDecorrelation = event.getUseDecorrelation();
        stepper.doPhaseIdentification(0.1d, 1d, true, true);
        close.compFinalStats(LocStatus.HELD_HYPOCENTER);
        return LocStatus.SUCCESS;
      }

      // Prepare the event for relocation by performing an initial phase
      // identification
      initialPhaseID.phaseID();
      LOGGER.finest(initialPhaseID.printInitialID());

      // Now do the multistage iteration to refine the hypocenter.  Note that
      // this is now just a two iteration process: once without and once with
      // decorrelation.
      LocStatus status;
      for (int stage = 0; stage < LocUtil.STAGELIMIT; stage++) {
        // check the stage status
        switch (stage) {
          case 0:
            // Do the stage 0 phase identification (no reID, but re-weight).
            status = stepper.doPhaseIdentification(0.01d, 5.0d, false, true);
            break;

          case 1:
            // Unless this is a restart, allow phases initially removed to
            // be added back in.
            if (!event.getIsLocationRestarted()) {
              initialPhaseID.resetUseFlags();
            }

            // Reset the decorrelation flag from the user input.
            // Decorrelation is generally a good idea, particularly if the
            // seismic network being used is "lumpy" (I.e., has dense sub-
            // networks.  On the other hand, the locator is faster and much
            // more stable without the decorrelation.
            LocUtil.useDecorrelation = event.getUseDecorrelation();

            // Do a looser phase identification.
            status = stepper.doPhaseIdentification(0.1d, 1.0d, true, true);
            break;

          default:
            // Continue using a looser phase identification.
            status = stepper.doPhaseIdentification(0.1d, 1.0d, true, true);
            break;
        }

        // Be sure we still have enough data to continue.
        if (status == LocStatus.INSUFFICIENT_DATA) {
          LOGGER.info("Insufficient Data");
          close.compFinalStats(status);
          return status;
        }

        // Initialize for iteration zero.
        hypo.setStepLength(LocUtil.INITIALSTEPLEN);

        // Iterate to convergence (or to the iteration limit).
        int iter;
        boolean dampingFailed = false;
        for (iter = 0; iter < LocUtil.ITERATIONSTAGELIMITS[stage]; iter++) {
          // Make a step.
          stepper.makeStep(stage, iter);

          // check the iteration status
          switch (status) {
            case INSUFFICIENT_DATA:
              LOGGER.info("Insufficient Data");
              // Bail on insufficient data.
              close.compFinalStats(status);
              return status;

            case NEARLY_CONVERGED:
            case DID_NOT_CONVERGE:
            case UNSTABLE_SOLUTION:
              // If the damping failed, go to the next stage.
              dampingFailed = true;
              System.out.println("Damping failed!");
              break;

            default:
              // Otherwise, keep on trucking!  (This includes phase
              // re-identification).
              break;
          }

          // Check for convergence.
          if (hypo.getStepLength() <= LocUtil.CONVERGENCESTAGELIMITS[stage] || dampingFailed) {
            break;
          }
        }

        if (!dampingFailed) {
          // We're done with this stage.  Collect information for a stage
          // level audit instance.
          if (iter >= LocUtil.ITERATIONSTAGELIMITS[stage]) {
            status = LocStatus.FULL_ITERATIONS;
          }

          // We need to save the last sub-step for the convergence test below.
          double lastStepLength = hypo.getStepLength();
          // Over write the step length with the total step over the stage for auditing purposes.
          hypo.setHorizontalStepLength(
              LocUtil.computeDistance(hypo, hypoAuditList.get(hypoAuditList.size() - 1)));
          hypo.setVerticalStepLength(
              Math.abs(hypo.getDepth() - hypoAuditList.get(hypoAuditList.size() - 1).getDepth()));
          hypo.setStepLength(
              Math.sqrt(
                  Math.pow(hypo.getHorizontalStepLength(), 2d)
                      + Math.pow(hypo.getVerticalStepLength(), 2d)));

          // check to see if we've converged
          if (stage > 0 && lastStepLength <= LocUtil.CONVERGENCESTAGELIMITS[stage]) {
            // Create the stage level audit anyway.
            event.addAudit(stage, iter, status);
            // If we've converged, create a final location level audit.  In this case, the step
            // length
            // is from the starting location.
            hypo.setHorizontalStepLength(LocUtil.computeDistance(hypo, hypoAuditList.get(0)));
            hypo.setVerticalStepLength(Math.abs(hypo.getDepth() - hypoAuditList.get(0).getDepth()));
            hypo.setStepLength(
                Math.sqrt(
                    Math.pow(hypo.getHorizontalStepLength(), 2d)
                        + Math.pow(hypo.getVerticalStepLength(), 2d)));
            event.addAudit(stage, iter, LocStatus.FINAL_HYPOCENTER);

            LOGGER.info("Final wrap up: \n" + event.printHypoAudit());

            status = close.compFinalStats(status);
            return status;
          } else {
            // Otherwise, create the stage level audit.
            event.addAudit(stage, iter, status);
          }
        } else {
          // Create the stage level audit so we know what happened.
          event.addAudit(stage, iter, status);
        }
      }

      // If we finish the last stage without converging, give up.  Note that the location is
      // probably in the ball park despite not converging.
      hypoAuditList.get(hypoAuditList.size() - 1).setLocationStatus(LocStatus.FULL_ITERATIONS);
      LOGGER.info("Did Not Converge: \n" + event.printHypoAudit());
      // Since we're probably close anyway, compute the error bars for the analyst to see.
      status = close.compFinalStats(LocStatus.FULL_ITERATIONS);
      return LocStatus.FULL_ITERATIONS;

    } catch (BadDepthException e) {
      // This should never happen.
      LOGGER.severe("Source depth out of range");
      //    e.printStackTrace();
      //    System.out.println(e.toString());
      e.printStackTrace();
      return LocStatus.BAD_DEPTH;
    } catch (TauIntegralException e) {
      // This should never happen either.
      LOGGER.severe("Illegal tau partial integral");
      //    e.printStackTrace();
      //    System.out.println(e.toString());
      e.printStackTrace();
      return LocStatus.BAD_INTEGRAL;
    } catch (Exception e) {
      LOGGER.severe("Unknown error");
      //  	System.out.println(e.toString());
      e.printStackTrace();
      return LocStatus.FAILED;
    }
  }
}
