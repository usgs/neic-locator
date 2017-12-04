package gov.usgs.locator;

//import java.util.ArrayList;
import gov.usgs.traveltime.TauUtil;
import gov.usgs.traveltime.AuxTtRef;
import gov.usgs.traveltime.TTime;
import gov.usgs.traveltime.AllBrnVol;
import gov.usgs.traveltime.TTimeData;

/**
 * Associate theoretical seismic phases with observed seismic picks.
 *
 * @author Ray Buland
 *
 */
public final class PhaseID {

  Event event;
  AllBrnVol allBrn;
  AuxTtRef auxtt;
  Hypocenter hypo;
  PickGroup group;
  Pick lastPick = null;
  TTime ttList;
  double otherWeight;				// Weight for phases that don't match
  double stickyWeight;			// Weight to resist changing identification
  boolean generic = false, primary = false;
  String phGroup = null;

  /**
   * Remember the event and travel-time machinery.
   *
   * @param event Event object
   * @param allBrn All branches travel-time object
   * @param auxtt
   */
  public PhaseID(Event event, AllBrnVol allBrn, AuxTtRef auxtt) {
    this.event = event;
    this.allBrn = allBrn;
    this.auxtt = auxtt;
    hypo = event.hypo;
  }

  /**
   * Driver for the phase identification.
   *
   * @param otherWeight
   * @param stickyWeight
   */
  public void doID(double otherWeight, double stickyWeight) {
    Station station;

    // Remember the figure-of-merit controls.
    this.otherWeight = otherWeight;
    this.stickyWeight = stickyWeight;

    // Do the travel-time calculation.
    for (int j = 0; j < event.groups.size(); j++) {
      group = event.groups.get(j);
      if (group.picksUsed() > 0) {
        // For the first pick in the group, get the travel times.
        station = group.station;
        System.out.println("\n" + station + ":");
        ttList = allBrn.getTT(station.latitude, station.longitude,
                station.elevation, group.delta, group.azimuth, true,
                false, false, false);
        // Print them.
        ttList.print(event.hypo.depth, group.delta);
        reID();
      }
    }
  }

  /**
   * This first crude phase identification is used to get a decent starting location. Only trusted
   * phases are used and they are generally associated with the theoretical arrival of the same
   * name.
   */
  private void pFirst() {

  }

  /**
   * During the location iteration, we don't want to re-identify phases, but sometimes
   * re-identification is thrust upon us (e.g., when the depth or distance changes and the former
   * identification no longer exists).
   */
  private void noReID() {

  }

  /**
   * This more sophisticated phase identification is used once we have a decent initial location.
   */
  private void reID() {
    int ttBeg, ttLen, pickBeg, pickLen;
    double winMin, winMax;
    TTimeData tTime;
    Pick pick;

    // Pre-identify surface waves identified by trusted sources.
    for (int j = 0; j < group.picks.size(); j++) {
      pick = group.picks.get(j);
      if ((pick.idCode.equals("Lg") || pick.idCode.equals("LR"))
              && (pick.authType == AuthorType.CONTRIB_HUMAN
              || pick.authType == AuthorType.LOCAL_HUMAN)) {
        for (int i = 0; i < ttList.size(); i++) {
          if (ttList.get(i).phCode.equals(pick.idCode)) {
            pick.mapStat = i;
            pick.surfWave = true;
            break;
          }
        }
      }
    }

    // Split the theoretical phase into clusters (groups isolated in 
    // travel time).
    int i = 0;
    System.out.println("\n\tClusters:");
    tTime = ttList.get(0);
    winMin = tTime.getTT() - tTime.getWindow();
    winMax = tTime.getTT() + tTime.getWindow();
    ttBeg = 0;
    ttLen = 1;
    pickBeg = -1;
    pickLen = 0;

    // Loop over theoretical arrivals.
    for (int j = 1; j < ttList.size(); j++) {
      tTime = ttList.get(j);
      // If this is part of the same cluster, extend the window.
      if (tTime.getTT() - tTime.getWindow() <= winMax) {
        winMin = Math.min(winMin, tTime.getTT() - tTime.getWindow());
        winMax = Math.max(winMax, tTime.getTT() + tTime.getWindow());
        ttLen++;
      } else {

        // This theoretical cluster is done, now associate picks within 
        // the current pick group.
        for (; i < group.picks.size(); i++) {
          pick = group.picks.get(i);
          if (pick.tt <= winMax) {
            if (pick.tt >= winMin) {
              if (pickLen == 0) {
                pickBeg = i;
              }
              pickLen++;
            }
          } else {
            break;
          }
        }

        // If this cluster has picks, do the identification.
        if (pickLen > 0) {
          // Print the current cluster.
          System.out.format("TT: %2d %2d  Pick: %2d %2d  Win: %7.2f %7.2f\n",
                  ttBeg, ttLen, pickBeg, pickLen, winMin, winMax);
          permut(pickBeg, pickLen, ttBeg, ttLen);
        }
        // Quit if we're out of picks.
        if (i >= group.picks.size()) {
          break;
        }
        // Otherwise, set up the next cluster.
        System.out.println("");
        winMin = tTime.getTT() - tTime.getWindow();
        winMax = tTime.getTT() + tTime.getWindow();
        ttBeg = j;
        ttLen = 1;
        pickBeg = -1;
        pickLen = 0;
      }
    }

    // Apply the distance correction to the first arriving phase.
    group.picks.get(0).fomStat /= LocUtil.deltaCorr(group.delta);
  }

  /**
   * Generate combinations of picks or theoretical arrivals to compare with all possible
   * combinations of theoretical arrivals or picks. For example, if there are 3 picks and 5
   * theoretical arrivals, the theoretical arrivals will be taken 3 at a time until all possible
   * combinations in the original order have been generated. Each combination will be treated as a
   * trial phase identification of the picks to those theoretical arrivals.
   *
   * @param pickBeg Index of the first phase within this phase group that will be part of this phase
   * identification
   * @param pickNo The number of picks to include in this phase identification
   * @param ttBeg Index of the first theoretical arrival that will be part of this phase
   * identification
   * @param ttNo The number of theoretical arrivals to include in this phase identification
   */
  private void permut(int pickBeg, int pickLen, int ttBeg, int ttLen) {
    // Set up some pointer arrays to work with internally.
    Pick[] pickGrp = new Pick[pickLen];
    for (int j = 0, i = pickBeg; j < pickLen; j++, i++) {
      pickGrp[j]
              = group.picks.get(i);
    }
    TTimeData[] ttGrp = new TTimeData[ttLen];
    for (int j = 0, i = ttBeg; j < ttLen; j++, i++) {
      ttGrp[j] = ttList.get(i);
    }

    System.out.format("\n Permut: %2d Picks, %2d TTs\n", pickLen, ttLen);
    // The algorithm depends on which group is the most numerous.
    if (ttLen >= pickLen) {
      // Generate the combinations.
      TTimeData[] ttPermut = new TTimeData[pickLen];
      kPermutOfN(ttGrp, pickLen, 0, ttPermut, pickGrp);
    } else {
      // Generate the combinations.
      Pick[] pickPermut = new Pick[ttLen];
      kPermutOfN(pickGrp, ttLen, 0, pickPermut, ttGrp);
    }
  }

  /**
   * Create all k-permutations of n objects, where k is the length of ttPermut and n is the length
   * of ttGrp. Note that this algorithm is recursive. The variables len and beg are primarily for
   * internal use. For the caller, len should be the length of the result (ttPermut) and beg should
   * be 0. This algorithm has been taken from StackOverflow. It was posted by user935714 on 20 April
   * 2016.
   *
   * @param ttGrp An array of theoretical arrivals
   * @param len The length of the permutation subset
   * @param beg The starting pointer of the permutation subset
   * @param ttPermut The result of the permutation
   */
  private void kPermutOfN(TTimeData[] ttGrp, int len, int beg, TTimeData[] ttPermut,
          Pick[] pickGrp) {
    if (len == 0) {
      setFoM(pickGrp, ttPermut);
      return;
    }
    for (int i = beg; i <= ttGrp.length - len; i++) {
      ttPermut[ttPermut.length - len] = ttGrp[i];
      kPermutOfN(ttGrp, len - 1, i + 1, ttPermut, pickGrp);
    }
  }

  /**
   * Create all k-permutations of n objects, where k is the length of pickPermut and n is the length
   * of pickGrp. Note that this algorithm is recursive. The variables len and beg are primarily for
   * internal use. For the caller, len should be the length of the result (pickPermut) and beg
   * should be 0. This algorithm has been taken from StackOverflow. It was posted by user935714 on
   * 20 April 2016.
   *
   * @param pickGrp An array of observed picks
   * @param len The length of the permutation subset
   * @param beg The starting pointer of the permutation subset
   * @param pickPermut The result of the permutation
   */
  private void kPermutOfN(Pick[] pickGrp, int len, int beg, Pick[] pickPermut,
          TTimeData[] ttGrp) {
    if (len == 0) {
      setFoM(pickPermut, ttGrp);
      return;
    }
    for (int i = beg; i <= pickGrp.length - len; i++) {
      pickPermut[pickPermut.length - len] = pickGrp[i];
      kPermutOfN(pickGrp, len - 1, i + 1, pickPermut, ttGrp);
    }
  }

  /**
   * For each trial set of phase identifications, compute the combined figure(s)-of-merit and save
   * the best identification results in the picks for later processing.
   *
   * @param pickGrp An array of picks to test
   * @param ttGrp An array of theoretical arrivals to test against
   */
  private void setFoM(Pick[] pickGrp, TTimeData[] ttGrp) {
    double prob, amp, aff;

    for (int j = 0; j < ttGrp.length; j++) {
      prob = LocUtil.ttResModel(pickGrp[j].tt - ttGrp[j].getTT(), 0d, ttGrp[j].getSpread());
      amp = idAmplitude(pickGrp[j], ttGrp[j]);
      aff = idAffinity(pickGrp[j], ttGrp[j]);
      System.out.format("\t%8s %8s: %10.4e %10.4e %3.1f\n", pickGrp[j].idCode,
              ttGrp[j].phCode, prob, amp, aff);
    }
  }

  /**
   * Compute the modified theoretical phase "amplitude". This is the phase observability with
   * empirical modifiers to reflect how closely it matches the observed phase. Note that this is a
   * complicated function of who identified the observed phase, if they are in the same phase group,
   * and if they have the same phase type. The sticky weight promotes stability by tending to keep
   * the old identification all else being equal.
   *
   * @return
   */
  private double idAmplitude(Pick pick, TTimeData tTime) {
    double amp;

    // Set up the observed pick phase group.
    if (pick != lastPick) {
      lastPick = pick;
      phGroup = auxtt.findGroup(pick.idCode, (pick.authType
              == AuthorType.CONTRIB_AUTO));
      primary = auxtt.isPrimary();
      if (pick.idCode.equals("Reg") || pick.idCode.equals(phGroup)) {
        generic = true;
      } else {
        generic = false;
      }
      System.out.print("New " + phGroup);
      if (primary) {
        System.out.print(" Pri");
      }
      if (generic) {
        System.out.print(" Gen");
      }
    } else {
      System.out.print("Old");
    }

    // initialize the amplitude.
    if (!tTime.getDis()) {
      amp = tTime.getObserv();
    } else {
      amp = LocUtil.DOWNWEIGHT * tTime.getObserv();
      System.out.print(" Down");
    }

    // Do the group logic.  If the phase codes match drop through 
    // unless the phase might be generic.
    if ((!pick.idCode.equals(tTime.phCode) || generic)
            && !phGroup.equals("all")) {
      // Handle primary groups differently for generic phase codes.
      if (generic && primary) {
        /*
				 * If the observed phase group matches the primary or auxiliary 
				 * groups of the theoretical phase use the group weighting.  That 
				 * is, a generic P might be either a P or a PKP.  The "Reg" group
				 * is a special case for contributed automatic picks (typically 
				 * from regional networks) which are assumed to be regional.
         */
        if (phGroup.equals(tTime.getPhGroup()) || phGroup.equals(tTime.getAuxGroup())
                || (phGroup.equals("Reg") && tTime.isRegional())) {
          amp *= LocUtil.GROUPWEIGHT;
          System.out.print(" Group");
        } // Otherwise use the other (non-group) weighting.
        else {
          amp *= otherWeight;
          System.out.print(" Other");
          // If we trust the phase identification and the arrival types 
          // of the phases don't match, make re-identifying even harder
          if ((pick.authType == AuthorType.CONTRIB_HUMAN
                  || pick.authType == AuthorType.LOCAL_HUMAN)
                  && TauUtil.arrivalType(phGroup)
                  != TauUtil.arrivalType(tTime.phCode)) {
            amp *= LocUtil.TYPEWEIGHT;
            System.out.print(" Type");
          }
        }
      } else {
        /*
				 * If the observed phase group matches the primary group of the 
				 * theoretical phase use the group weighting.  That is, a Pn would 
				 * be in the same group as Pg, but not PKPdf.  Note that a generic 
				 * PKP would only match PKP phases.
         */
        if (phGroup.equals(tTime.getPhGroup())) {
          amp *= LocUtil.GROUPWEIGHT;
          System.out.print(" Group");
        } // Otherwise use the other (non-group) weighting.
        else {
          amp *= otherWeight;
          System.out.print(" Other");
          // If we trust the phase identification and the arrival types 
          // of the phases don't match, make re-identifying even harder
          if ((pick.authType == AuthorType.CONTRIB_HUMAN
                  || pick.authType == AuthorType.LOCAL_HUMAN)
                  && TauUtil.arrivalType(phGroup)
                  != TauUtil.arrivalType(tTime.phCode)) {
            amp *= LocUtil.TYPEWEIGHT;
            System.out.print(" Type");
          }
        }
      }
    }

    // Account for the affinity.
    if (pick.idCode.equals(tTime.phCode)) {
      amp *= pick.affinity;
      System.out.print(" Aff");
    }

    // Make the existing identification harder to change.
    if (pick.phCode.equals(tTime.phCode)) {
      amp *= stickyWeight;
      System.out.print(" Sticky");
    }
    System.out.println("");
    return amp;
  }

  /**
   * Get the trial affinity. This is the affinity if the observed and theoretical phases match and
   * unity otherwise.
   *
   * @return The trial affinity.
   */
  private double idAffinity(Pick pick, TTimeData tTime) {
    if (pick.idCode.equals(tTime.phCode)) {
      return pick.affinity;
    } else {
      return LocUtil.NULLAFFINITY;
    }
  }
}

/**
 * Helper class keeping track of the limits and contents of one cluster of theoretical and observed
 * phases. Clusters are defined by groups of theoretical phases that are isolated from each other in
 * time. Observed phases are then associated with clusters.
 *
 * @author Ray Buland
 *
 */
class AssocCluster {

  protected int ttBeg;			// Index of first theoretical phase in the cluster
  protected int ttLen;			// Number of theoretical phase in the cluster
  protected int pickBeg;		// Index of first pick in the cluster
  protected int pickLen;		// Number of picks in the cluster
  protected double winMin;	// Minimum travel time in the cluster
  protected double winMax;	// Maximum travel time in the cluster

  protected AssocCluster(int ttBeg, double winMin, double winMax) {
    this.ttBeg = ttBeg;
    this.winMin = winMin;
    this.winMax = winMax;
    ttLen = 1;
    pickBeg = -1;
    pickLen = 0;
  }

  protected void addPick(int pickBeg) {
    this.pickBeg = pickBeg;
    pickLen++;
  }

  protected void printCluster() {
    System.out.format("TT: %2d %2d  Pick: %2d %2d  Win: %7.2f %7.2f\n",
            ttBeg, ttLen, pickBeg, pickLen, winMin, winMax);
  }
}
