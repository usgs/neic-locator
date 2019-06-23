package gov.usgs.locator;

import gov.usgs.traveltime.TTSessionLocal;
import gov.usgs.traveltime.TTime;
import gov.usgs.traveltime.TTimeData;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The InitialPhaseID class performs an initial phase identification before any location iterations
 * or real phase identification takes place. This initial pass ensures that we have something
 * reasonable to work with by emphasizing crust and mantle P waves and manually identified phases.
 * If there are a lot of apparently misidentified first arrivals, the algorithm gets even more
 * draconian.
 *
 * @author Ray Buland
 */
public class InitialPhaseID {
  /** An Event object containing the event to perform initial phase identification upon. */
  private Event event;

  /**
   * A Hypocenter object containing the hypocenter of event to perform initial phase identification
   * upon.
   */
  private Hypocenter hypo;

  /**
   * A TTSessionLocal object containing a local travel-time manager used to perform initial phase
   * identification.
   */
  private TTSessionLocal ttLocalSession;

  /**
   * A PhaseID object containing Phase identification logic used in performing initial phase
   * identification.
   */
  private PhaseID phaseID;

  /**
   * An ArrayList of WeightedResidual objects containing the weighted residuals of the event picks.
   */
  private ArrayList<WeightedResidual> weightedResiduals;

  /**
   * A RankSumEstimator object used for the rank-sum estimation of the picks to refine the initial
   * phase identification.
   */
  private RankSumEstimator rankSumEstimator;

  /**
   * A Stepper object used to manage the rank-sum estimation logic needed to refine the initial
   * phase identification.
   */
  private Stepper stepper;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(InitialPhaseID.class.getName());

  /**
   * The InitialPhaseID constructor. This constructor sets the event, tt session, phase
   * identification logic, and rank-sum estimator to the provided values.
   *
   * @param event An Event object containing the information for the event to perform initial phase
   *     identification upon.
   * @param ttLocalSession A TTSessionLocal object holding the local travel-time manager
   * @param phaseID A PhaseID object containing the phase identification logic for initial phase
   *     identification
   * @param stepper A RankSumEstimator object containing the rank-sum estimation driver logic
   */
  public InitialPhaseID(
      Event event, TTSessionLocal ttLocalSession, PhaseID phaseID, Stepper stepper) {
    this.event = event;
    hypo = event.getHypo();
    this.ttLocalSession = ttLocalSession;
    this.phaseID = phaseID;
    weightedResiduals = event.getRawWeightedResiduals();
    rankSumEstimator = event.getRawRankSumEstimator();
    this.stepper = stepper;
  }

  /**
   * The phaseID function performs a tentative phase identification to see if the event is making
   * sense.
   *
   * @throws Exception On an illegal source depth
   */
  public void phaseID() throws Exception {
    int badPs = 0;

    // Reinitialize the weighted residual storage.
    if (weightedResiduals.size() > 0) {
      weightedResiduals.clear();
    }

    // Set up a new travel-time session if the depth has changed.
    ttLocalSession.newSession(
        event.getEarthModel(),
        hypo.getDepth(),
        LocUtil.PHASELIST,
        hypo.getLatitude(),
        hypo.getLongitude(),
        LocUtil.SUPRESSUNLIKELYPHASES,
        LocUtil.SUPRESSBACKBRANCHES,
        LocUtil.isTectonic,
        false);

    // Loop over picks in the groups.
    for (int j = 0; j < event.getNumStations(); j++) {
      PickGroup group = event.getPickGroupList().get(j);

      if (group.getNumPicksUsed() > 0) {
        // For the first pick in the group, get the travel times.
        Station station = group.getStation();
        LOGGER.finer("InitialPhaseID: " + station + ":");

        // Do the travel-time calculation.
        TTime ttList =
            ttList =
                ttLocalSession.getTT(
                    station.getLatitude(),
                    station.getLongitude(),
                    station.getElevation(),
                    group.getDistance(),
                    group.getAzimuth());

        // Print them.
        // ttList.print(event.hypo.depth, group.getDistance());
        TTimeData travelTime = ttList.getPhase(0);

        // Based on a tentative ID, just compute residuals and weights so
        // that a robust estimate of the origin time correction can be
        // made.  Without this step, the actual phase identification may
        // not work correctly.  Note that only some of the first arrivals
        // that are being used are considered and that the tentative ID is
        // not remembered.
        if (group.getDistance() <= 100d) {
          Pick pick = group.getPicks().get(0);
          boolean found;

          if (pick.getIsUsed()) {
            String phCode = pick.getCurrentPhaseCode();

            if (!"PK".equals(phCode.substring(0, 1))
                && !"P'".equals(phCode.substring(0, 1))
                && !"Sc".equals(phCode.substring(0, 1))
                && !"Sg".equals(phCode)
                && !"Sb".equals(phCode)
                && !"Sn".equals(phCode)
                && !"Lg".equals(phCode)) {
              if (pick.getIsAutomatic()) {
                travelTime = ttList.getPhase(0);

                if (!phCode.equals(travelTime.getPhCode())) {
                  badPs++;
                }

                pick.setResidual(pick.getTravelTime() - travelTime.getTT());
                pick.setWeight(1d / travelTime.getSpread());

                if (!phCode.equals(travelTime.getPhCode())) {
                  LOGGER.finer(
                      String.format(
                          "InitialPhaseID: %s -> %s auto", phCode, travelTime.getPhCode()));
                }
              } else {
                found = false;

                for (int i = 0; i < ttList.getNumPhases(); i++) {
                  travelTime = ttList.getPhase(i);

                  if (phCode.equals(travelTime.getPhCode())) {
                    // Note that this is slightly different from the Fortran
                    // version where the weight is always from the first arrival.
                    pick.setResidual(pick.getTravelTime() - travelTime.getTT());
                    pick.setWeight(1d / travelTime.getSpread());
                    found = true;
                    break;
                  }
                }

                if (!found) {
                  travelTime = ttList.getPhase(0);
                  pick.setResidual(pick.getTravelTime() - travelTime.getTT());
                  pick.setWeight(1d / travelTime.getSpread());

                  LOGGER.finer(
                      String.format(
                          "InitialPhaseID: " + "%s -> %s human", phCode, travelTime.getPhCode()));
                }
              }

              weightedResiduals.add(
                  new WeightedResidual(
                      pick, pick.getResidual(), pick.getWeight(), false, 0d, 0d, 0d));

              LOGGER.finer(
                  String.format(
                      "InitialPhaseID push: %s %s %5.2f %7.4f %5.2f" + "%5.2f",
                      pick.getStation().getStationID().getStationCode(),
                      pick.getCurrentPhaseCode(),
                      pick.getResidual(),
                      pick.getWeight(),
                      travelTime.getTT(),
                      travelTime.getSpread()));
            }
          }
        }
      }
    }

    // Add in the Bayesian depth because the R-estimator code expects it.
    weightedResiduals.add(
        new WeightedResidual(
            null,
            hypo.getBayesianDepthResidual(),
            hypo.getBayesianDepthWeight(),
            true,
            0d,
            0d,
            0d));

    // Update the hypocenter origin time based on the residuals and weights
    // pushed by the phaseID method.  Adjusting the origin time to something
    // reasonable ensures that succeeding phase identifications have a chance.
    double median = rankSumEstimator.computeMedian();
    event.updateOriginTime(median);

    LOGGER.fine(
        String.format(
            "Update origin: %f %f %f %d",
            hypo.getOriginTime(), median, hypo.getOriginTime() + median, badPs));

    // On a restart, reidentify all phases to be consistent with the new hypocenter.
    // Note that we still needed the logic above to reset the origin time.
    if (event.getIsLocationRestarted()) {
      stepper.setLocEnvironment();
      phaseID.phaseID(0.1d, 1d, true, true);
      event.computeStationStats();
      return;
    }

    // Based on the number of probably misidentified first arrivals:
    if (badPs < LocUtil.BADRATIO * event.getNumStationsUsed()) {
      // Just make the obvious re-identifications (i.e., autos).
      simplePhaseID();
    } else {
      // Re-identify any first arrivals that don't look right.
      complexPhaseID();
    }
  }

  /**
   * The simplePhaseID function is run if the initial phase identification seems to be making sense
   * for the event (i.e., not too many misidentified first arrivals), we can go easy on the initial
   * phase identification.
   */
  private void simplePhaseID() {
    // Loop over groups assessing automatic picks.
    for (int j = 0; j < event.getNumStations(); j++) {
      PickGroup group = event.getPickGroupList().get(j);

      if (group.getNumPicksUsed() > 0) {
        Pick pick = group.getPicks().get(0);

        // If the first arrival is automatic and not a crust or mantle P, don't
        // use it.
        if (pick.getIsAutomatic() && pick.getIsUsed()) {
          String phCode = pick.getCurrentPhaseCode();

          if (!"Pg".equals(phCode)
              && !"Pb".equals(phCode)
              && !"Pn".equals(phCode)
              && !"P".equals(phCode)) {
            pick.setIsUsed(false);

            LOGGER.finer(
                String.format(
                    "\tIdEasy: don't use %s %s",
                    group.getStation().getStationID().getStationCode(),
                    pick.getCurrentPhaseCode()));
          }
        }

        // Don't use any secondary automatic phases.
        for (int i = 1; i < group.getNumPicks(); i++) {
          pick = group.getPicks().get(i);

          if (pick.getIsAutomatic() && pick.getIsUsed()) {
            pick.setIsUsed(false);

            LOGGER.finer(
                String.format(
                    "\tIdEasy: don't use %s %s",
                    group.getStation().getStationID().getStationCode(),
                    pick.getCurrentPhaseCode()));
          }
        }
      }
    }
  }

  /**
   * The complexPhaseID function is run if the initial phase identification doesn't seem to be
   * making sense for the event (i.e., too many misidentified first arrivals), we need to be
   * stricter about the initial phase identification.
   */
  private void complexPhaseID() {
    Station station;

    // Loop over groups forcing automatic phases to conform.
    for (int j = 0; j < event.getNumStations(); j++) {
      PickGroup group = event.getPickGroupList().get(j);

      if (group.getNumPicksUsed() > 0) {
        Pick pick = group.getPicks().get(0);

        // If the first arrival is automatic and might be a misidentified first
        // arrival, force it to be the first theoretical arrival.
        if (pick.getIsAutomatic() && pick.getIsUsed()) {
          String phCode = pick.getCurrentPhaseCode();

          if (group.getDistance() <= 100d
              && !"PK".equals(phCode.substring(0, 1))
              && !"P'".equals(phCode.substring(0, 1))
              && !"Sc".equals(phCode.substring(0, 1))
              && !"Sg".equals(phCode)
              && !"Sb".equals(phCode)
              && !"Sn".equals(phCode)
              && !"Lg".equals(phCode)) {
            // For the first pick in the group, get the travel times.
            station = group.getStation();

            LOGGER.finer("" + station + ":");

            // Do the travel-time calculation.
            TTime ttList =
                ttLocalSession.getTT(
                    station.getLatitude(),
                    station.getLongitude(),
                    station.getElevation(),
                    group.getDistance(),
                    group.getAzimuth());

            // Print them.
            // ttList.print(event.hypo.depth, group.getDistance());

            // Set the phase code.  The travel time was already set in phaseID.
            pick.updatePhaseIdentification(ttList.getPhase(0).getPhCode());

            LOGGER.finer(
                String.format(
                    "IdHard: %s %s -> %s auto",
                    group.getStation().getStationID().getStationCode(),
                    phCode,
                    ttList.getPhase(0).getPhCode()));
          } else {
            // If it's a core phase or not a common mis-identification, just
            // don't use it.
            pick.setIsUsed(false);

            LOGGER.finer(
                String.format(
                    "IdHard: don't use %s %s",
                    group.getStation().getStationID().getStationCode(),
                    pick.getCurrentPhaseCode()));
          }
        }

        // Don't use any secondary automatic phases.
        for (int i = 1; i < group.getNumPicks(); i++) {
          pick = group.getPicks().get(i);

          if (pick.getIsAutomatic() && pick.getIsUsed()) {
            pick.setIsUsed(false);

            LOGGER.finer(
                String.format(
                    "\tIdHard: don't use %s %s",
                    group.getStation().getStationID().getStationCode(),
                    pick.getCurrentPhaseCode()));
          }
        }
      }
    }
  }

  /**
   * The resetUseFlags function puts some of phases that were temporarily taken out of the location
   * back in after refining the starting location based on the initial phase identification and
   * doing a more rigorous phase identification.
   */
  public void resetUseFlags() {
    // This simply resets no-used phases back to their initial input state.
    for (int j = 0; j < event.getNumStations(); j++) {
      PickGroup group = event.getPickGroupList().get(j);

      for (int i = 0; i < group.getNumPicks(); i++) {
        Pick pick = group.getPicks().get(i);
        if (!pick.getIsUsed()) {
          pick.setIsUsed(pick.getExternalUse());
        }
      }
    }
  }

  /**
   * The printInitialID function writes the phases used in the initial relocation to a string for
   * logging. Note that they may have been re-identified after the initialID algorithm.
   *
   * @return A String containing the initial phase ids
   */
  public String printInitialID() {
    String initialString = "Initial phase identification:\n";

    for (int j = 0; j < event.getNumStations(); j++) {
      PickGroup group = event.getPickGroupList().get(j);

      if (group.getNumPicksUsed() > 0) {
        Station station = group.getStation();

        for (int i = 0; i < group.getNumPicks(); i++) {
          Pick pick = group.getPicks().get(i);

          if (pick.getIsUsed()) {
            initialString +=
                String.format(
                    "%-5s %-8s %6.1f %6.1f %3.0f %5.2f\n",
                    station.getStationID().getStationCode(),
                    pick.getCurrentPhaseCode(),
                    pick.getResidual(),
                    group.getDistance(),
                    group.getAzimuth(),
                    pick.getWeight());
          }
        }
      }
    }

    return initialString;
  }
}
