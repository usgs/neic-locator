package gov.usgs.locator;

//import java.util.ArrayList;
import gov.usgs.traveltime.TauUtil;
import gov.usgs.traveltime.AuxTtRef;
import gov.usgs.traveltime.TTime;

import java.util.ArrayList;

import gov.usgs.traveltime.AllBrnVol;
import gov.usgs.traveltime.TTimeData;

/**
 * Associate theoretical seismic phases with observed seismic picks.
 *
 * @author Ray Buland
 *
 */
public class PhaseID {
	double lastDepth = Double.NaN;
  Event event;
  AllBrnVol allBrn;
  AuxTtRef auxtt;
  Hypocenter hypo;
  ArrayList<Wresidual> wResiduals;
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
   */
  public PhaseID(Event event, AllBrnVol allBrn) {
    this.event = event;
    this.allBrn = allBrn;
    hypo = event.hypo;
    wResiduals = event.wResiduals;
  }

  /**
   * Driver for the phase identification.
   *
   * @param otherWeight Weight for phases that don't match the current 
   * phase identification or the current phase group (higher weights 
   * make changing to an "other" phase easier)
   * @param stickyWeight Weight for an exact match (higher weights make 
   * changing the current identification harder.
   * @param reID If true, do the full phase re-identification, if false 
   * try not to change identifications
   * @param reWeight If true, update the residual weights
	 * @return True if any used pick in the group has changed significantly
   * @throws Exception On an illegal source depth
   */
  public boolean doID(double otherWeight, double stickyWeight, boolean reID, 
  		boolean reWeight) throws Exception {
  	boolean changed;
    Station station;
    Wresidual wRes;

    // Remember the figure-of-merit controls.
    this.otherWeight = otherWeight;
    this.stickyWeight = stickyWeight;
    
    // Initialize the changed flag.
    if(reWeight) changed = true;
    else changed = false;
    
		// Reinitialize the weighted residual storage.
		if(wResiduals.size() > 0) wResiduals.clear();

		if(hypo.depth != lastDepth) {
			// Set up a new travel-time session if the depth has changed.
			allBrn.newSession(hypo.latitude, hypo.longitude, hypo.depth, 
					LocUtil.PHLIST);
		} else {
			// Otherwise, just update the epicenter coordinates.
			allBrn.newEpicenter(hypo.latitude, hypo.longitude);
		}
		
    // Do the travel-time calculation.
    for (int j = 0; j < event.noStations(); j++) {
      group = event.groups.get(j);
      if (group.picksUsed() > 0) {
        // For the first pick in the group, get the travel times.
        station = group.station;
        System.out.println("\n" + station + ":");
        ttList = allBrn.getTT(station.latitude, station.longitude,
                station.elevation, group.delta, group.azimuth, LocUtil.USEFUL,
                LocUtil.tectonic, LocUtil.NOBACKBRN, LocUtil.rstt);
        // Print them.
    //   ttList.print(event.hypo.depth, group.delta);
        // If reID is true, do a full phase re-identification.
        if(reID) {
        	reID();
        }
        // Otherwise, try not to re-identify the phases.
        else {
        	noReID();
        }
      }
      if(group.updateID(reWeight, wResiduals)) changed = true;
    }
    // Add the Bayesian depth.
    wRes = new Wresidual(true, wResiduals.size(), hypo.depthRes, 
    		hypo.depthWeight);
    wResiduals.add(wRes);
    // Create a list of used picks that will be indexed by the weighted 
    // residuals (before and after sorting).
    event.makeUsedPicks();
    event.staStats();
    return changed;
  }

  /**
   * During the location iteration, we don't want to re-identify phases, but 
   * sometimes re-identification is thrust upon us (e.g., when the depth or 
   * distance changes and the former identification no longer exists).
   * 
   * @param reWeight If true update the residual weights
	 * @return True if any used pick in the group has changed significantly
   */
  private void noReID() {
  	int m;
  	double resMin;
  	Pick pick;
  	String phCode, phGroup;
  	TTimeData tTime;
  	
  	// Loop over picks in the group.
  	for(int j=0; j<group.noPicks(); j++) {
      pick = group.getPick(j);
      phCode = pick.phCode;
      if(!phCode.equals("")) {
      	// If we have a non-blank phase code, find the phase of the same name 
      	// that is closest to the pick in time.
      	m = -1;
      	resMin = TauUtil.DMAX;
      	for(int i=0; i<ttList.size(); i++) {
      		tTime = ttList.get(i);
      		if(phCode.equals(tTime.getPhCode())) {
      			if(Math.abs(pick.tt-tTime.getTT()) < resMin) {
	      			m = i;
	      			resMin = Math.abs(pick.tt-tTime.getTT());
      			}
      		}
      	}
      	// If it's not too out of whack, force the association.
      	if(m >= 0 && (resMin <= LocUtil.ASSOCTOL || phCode.equals("Lg") || 
      			phCode.equals("LR"))) {
      		pick.mapStat = ttList.get(m);
      		pick.fomStat = resMin;
      		pick.forceStat = true;
      	// If the easy way doesn't work, we have to try harder.
      	} else {
      		phGroup = auxtt.findGroup(pick.idCode, false);
        	// If we have a non-blank phase code, find the phase of the same name 
        	// that is closest to the pick in time.
        	m = -1;
        	resMin = TauUtil.DMAX;
        	for(int i=0; i<ttList.size(); i++) {
        		tTime = ttList.get(i);
        		if(phGroup.equals(tTime.getPhGroup())) {
        			if(Math.abs(pick.tt-tTime.getTT()) < resMin) {
  	      			m = i;
  	      			resMin = Math.abs(pick.tt-tTime.getTT());
        			}
        		}
        	}
        	// If it's not too out of whack, force the association.
        	if(m >= 0 && resMin <= LocUtil.ASSOCTOL) {
        		pick.mapStat = ttList.get(m);
        		pick.fomStat = resMin;
        		pick.forceStat = true;
        	} else {
        		if(pick.used) {
        			group.initFoM();
        			reID();
        		} else {
        			pick.mapStat = null;
        		}
        	}
      	}
      }
    }
  }

  /**
   * This more sophisticated phase identification is used once we have a 
   * decent initial location.  Note that for a full phase re-identification 
   * the weights are always updated.
   * 
	 * @return True if any used pick in the group has changed significantly
   */
  private void reID() {
    int ttBeg, ttLen, pickBeg, pickLen;
    double winMin, winMax;
    TTimeData tTime;
    Pick pick;

    // Initialize the figure-of-merit memory.
    group.initFoM();

    // Pre-identify surface waves identified by trusted sources.
    for (int j = 0; j < group.noPicks(); j++) {
      pick = group.getPick(j);
      if (pick.surfWave) {
        for (int i = 0; i < ttList.size(); i++) {
          if (pick.idCode.equals(ttList.get(i).getPhCode())) {
            pick.mapStat = ttList.get(i);
            pick.forceStat = true;
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
        for (; i < group.noPicks(); i++) {
          pick = group.getPick(i);
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
    double deltaCorr = LocUtil.deltaCorr(group.delta);
    group.getPick(0).fomStat /= deltaCorr;
    group.getPick(0).fomAlt /= deltaCorr;
    
    // Print out the chosen associations.
    printAssoc();
    
    // Finally, rationalize the two identification methods.
    fomMerge();
  }
  
  /**
   * Print out the associations chosen.  This is messy because of possible 
   * null pointers.
   */
  private void printAssoc() {
  	Pick pick;
  	
    if(LocUtil.deBugLevel > 1) {
    	for(int j=0; j<group.noPicks(); j++) {
    		pick = group.getPick(j);
    		if(pick.mapStat != null) {
    			if(pick.mapAlt != null) {
        		System.out.format("  Sel: %1d %-8s %-8s %5.2f %5.2f\n", j, 
        				pick.mapStat.getPhCode(), pick.mapAlt.getPhCode(), pick.fomStat, 
        				pick.fomAlt);
    			} else {
        		System.out.format("  Sel: %1d %-8s null     %5.2f %5.2f\n", j, 
        				pick.mapStat.getPhCode(), pick.fomStat, pick.fomAlt);
    			}
    		} else {
    			if(pick.mapAlt != null) {
        		System.out.format("  Sel: %1d null     %-8s %5.2f %5.2f\n", j, 
        				pick.mapAlt.getPhCode(), pick.fomStat, pick.fomAlt);
    			} else {
        		System.out.format("  Sel: %1d null     null     %5.2f %5.2f\n", j, 
        				pick.fomStat, pick.fomAlt);
    			}
    		}
    	}
    }
  }
  
  /**
   * Merge the statistical and nearest theoretical strategies.  This code 
   * has been isolated in the hope that a Bayesian approach will eliminate 
   * the alternate phase identification.
   */
  private void fomMerge() {
  	Pick pick, pick2;
  	
  	for(int j=0; j<group.noPicks(); j++) {
  		pick = group.getPick(j);
  		/*
  		 * The identification will be done using the statistical variables.  
  		 * Therefore, we only need to change the statistical variables if the 
  		 * alternative identification looks better.
  		 */
  		if(pick.mapStat != null) {
  			if(pick.mapAlt != null) {
  				// We have both, now what?
  				if(j == 0) {
  					// Favor the alternate identification for the first arrival.
  					if(pick.fomAlt <= 2d*LocUtil.validLim(pick.mapAlt.getSpread()) && 
  							pick.fomAlt < pick.fomStat-1d && pick.mapAlt.getPhGroup() == 
  							pick.mapStat.getPhGroup()) {
  						pick.mapStat = pick.mapAlt;
  						pick.fomStat = pick.fomAlt;
  					// If that didn't work, see if the statistical identification is 
  					// acceptable.
  					} else if(pick.fomStat > 
  							2d*LocUtil.validLim(pick.mapStat.getSpread())) {
  						// If that that didn't work, go back to the alternate 
  						// identification.
  						if(pick.fomAlt <= 
  								2d*LocUtil.validLim(pick.mapAlt.getSpread())) {
  	 						pick.mapStat = pick.mapAlt;
    						pick.fomStat = pick.fomAlt;
  						} else {
  							// If all else fails, give up.
  							pick.mapStat = null;
  						}
  					}
  				// Treat later phases differently.
  				} else {
  					// Favor the alternate identification, but not quite as strictly.
  					if(pick.fomAlt <= 2d*LocUtil.validLim(pick.mapAlt.getSpread()) && 
  							pick.fomAlt < pick.fomStat-0.5d) {
  						pick.mapStat = pick.mapAlt;
  						pick.fomStat = pick.fomAlt;
  					} else if(pick.fomStat > 
								2d*LocUtil.validLim(pick.mapStat.getSpread())) {
							// If that that didn't work, go back to the alternate 
  						// identification.
							if(pick.fomAlt <= 
									2d*LocUtil.validLim(pick.mapAlt.getSpread())) {
		 						pick.mapStat = pick.mapAlt;
								pick.fomStat = pick.fomAlt;
							} else {
								// If all else fails, give up.
								pick.mapStat = null;
							}
  					}
  				}
  			// We only have a statistical identification.
  			} else if(pick.fomStat > 
								2d*LocUtil.validLim(pick.mapStat.getSpread())) {
  				pick.mapStat = null;
  			}
  		// We don't have a statistical identification, try the alternative.
  		} else if(pick.mapAlt != null) {
				if(pick.fomAlt <= 2d*LocUtil.validLim(pick.mapAlt.getSpread())) {
					pick.mapStat = pick.mapAlt;
					pick.fomStat = pick.fomAlt;
				} else {
					pick.mapStat = null;
				}
			// Neither method found a match.  Give up.
  		} else {
  			pick.mapStat = null;
  		}
  	}
  	
  	// We're not quite done.  Now we need to eliminate duplicate 
  	// identifications.
  	for(int j=0; j<group.noPicks(); j++) {
  		pick = group.getPick(j);
  		if(pick.mapStat != null) {
  			for(int i=j+1; i<group.noPicks(); i++) {
  				pick2 = group.getPick(i);
  				if(pick == pick2) {
  					if(j == 0) {
  						pick2.mapStat = null;
  					} else {
  						if(pick.fomStat <= pick2.fomStat) {
  							pick2.mapStat = null;
  						} else {
  							pick.mapStat = null;
  							break;
  						}
  					}
  				}
  			}
  		}
  	}
  }

  /**
   * Generate combinations of picks or theoretical arrivals to compare with 
   * all possible combinations of theoretical arrivals or picks. For example, 
   * if there are 3 picks and 5 theoretical arrivals, the theoretical arrivals 
   * will be taken 3 at a time until all possible combinations in the original 
   * order have been generated. Each combination will be treated as a trial 
   * phase identification of the picks to those theoretical arrivals.
   *
   * @param pickBeg Index of the first phase within this phase group that will 
   * be part of this phase identification
   * @param pickNo The number of picks to include in this phase identification
   * @param ttBeg Index of the first theoretical arrival that will be part of 
   * this phase identification
   * @param ttNo The number of theoretical arrivals to include in this phase 
   * identification
   */
  private void permut(int pickBeg, int pickLen, int ttBeg, int ttLen) {
    // Set up some pointer arrays to work with internally.
    Pick[] pickClust = new Pick[pickLen];
    for (int j = 0, i = pickBeg; j < pickLen; j++, i++) {
      pickClust[j] = group.picks.get(i);
    }
    TTimeData[] ttClust = new TTimeData[ttLen];
    for (int j = 0, i = ttBeg; j < ttLen; j++, i++) {
      ttClust[j] = ttList.get(i);
    }

    System.out.format("\n Permut: %2d Picks, %2d TTs\n", pickLen, ttLen);
    // The algorithm depends on which group is the most numerous.
    if (ttLen >= pickLen) {
      // Generate the combinations.
      TTimeData[] ttPermut = new TTimeData[pickLen];
      kPermutOfN(ttClust, pickLen, 0, ttPermut, pickClust);
    } else {
      // Generate the combinations.
      Pick[] pickPermut = new Pick[ttLen];
      kPermutOfN(pickClust, ttLen, 0, pickPermut, ttClust);
    }
  }

  /**
   * Create all k-permutations of n objects, where k is the length of ttPermut 
   * and n is the length of ttGrp. Note that this algorithm is recursive. The 
   * variables len and beg are primarily for internal use. For the caller, len 
   * should be the length of the result (ttPermut) and beg should be 0. This 
   * algorithm has been taken from StackOverflow. It was posted by user935714 
   * on 20 April 2016.
   *
   * @param ttGrp An array of theoretical arrivals
   * @param len The length of the permutation subset
   * @param beg The starting pointer of the permutation subset
   * @param ttPermut The result of the permutation
   */
  private void kPermutOfN(TTimeData[] ttClust, int len, int beg, 
  		TTimeData[] ttPermut, Pick[] pickClust) {
    if (len == 0) {
      setFoM(pickClust, ttPermut);
      return;
    }
    for (int i = beg; i <= ttClust.length - len; i++) {
      ttPermut[ttPermut.length - len] = ttClust[i];
      kPermutOfN(ttClust, len - 1, i + 1, ttPermut, pickClust);
    }
  }

  /**
   * Create all k-permutations of n objects, where k is the length of 
   * pickPermut and n is the length of pickGrp. Note that this algorithm is 
   * recursive. The variables len and beg are primarily for internal use. For 
   * the caller, len should be the length of the result (pickPermut) and beg 
   * should be 0. This algorithm has been taken from StackOverflow. It was 
   * posted by user935714 on 20 April 2016.
   *
   * @param pickGrp An array of observed picks
   * @param len The length of the permutation subset
   * @param beg The starting pointer of the permutation subset
   * @param pickPermut The result of the permutation
   */
  private void kPermutOfN(Pick[] pickClust, int len, int beg, 
  		Pick[] pickPermut,TTimeData[] ttClust) {
    if (len == 0) {
      setFoM(pickPermut, ttClust);
      return;
    }
    for (int i = beg; i <= pickClust.length - len; i++) {
      pickPermut[pickPermut.length - len] = pickClust[i];
      kPermutOfN(pickClust, len - 1, i + 1, pickPermut, ttClust);
    }
  }

  /**
   * For each trial set of phase identifications, compute the combined 
   * figure(s)-of-merit and save the best identification results in the picks 
   * for later processing.
   *
   * @param pickGrp An array of picks to test
   * @param ttGrp An array of theoretical arrivals to test against
   */
  private void setFoM(Pick[] pickClust, TTimeData[] ttClust) {
    double prob, amp, wRes, cumFoM;

    cumFoM = 1d;
    // Make a pass computing the cumulative statistical figure-of-merit.
    for (int j = 0; j < ttClust.length; j++) {
    	if(!pickClust[j].surfWave) {
    		// Compute the figure-of-merit for the primary criteria.
	      prob = LocUtil.ttResModel(pickClust[j].tt - ttClust[j].getTT(), 0d, 
	      		ttClust[j].getSpread());
	      amp = idAmplitude(pickClust[j], ttClust[j]);
	      wRes = idResidual(pickClust[j], ttClust[j]);
	      System.out.format("\t%8s %8s: %10.4e %10.4e %3.1f\n", 
	      		pickClust[j].idCode,ttClust[j].getPhCode(), prob, amp, wRes);
	      cumFoM *= amp*prob;
	      // Set up the alternative criteria at the same time.  Note, the 
	      // Fortran version omitted the affinity in this test.
	      if(ttClust[j].getObserv() >= LocUtil.MINOBSERV && wRes < 
	      		pickClust[j].fomAlt) {
	      	// Make sure that the phase types match unless the pick is automatic.
	      	if(pickClust[j].auto || TauUtil.arrivalType(pickClust[j].idCode)
              == TauUtil.arrivalType(ttClust[j].getPhCode())) {
	      		pickClust[j].setFomAlt(ttClust[j], wRes);
	      	}
	      }
    	}
    }
    
    // Make a second pass if this is the highest figure-of-merit yet.  Note, 
    // the Fortran version has greater than or equal to.
    if(cumFoM > group.fomMax) {
    	group.fomMax = cumFoM;
	    for (int j = 0; j < ttClust.length; j++) {
	    	if(!pickClust[j].surfWave) {
	    		pickClust[j].setFomStat(ttClust[j], 
	    				idResidual(pickClust[j], ttClust[j]));
	    	}
	    }
  	}
  }

  /**
   * Compute the modified theoretical phase "amplitude". This is the phase 
   * observability with empirical modifiers to reflect how closely it matches 
   * the observed phase. Note that this is a complicated function of who 
   * identified the observed phase, if they are in the same phase group, and 
   * if they have the same phase type. The sticky weight promotes stability by 
   * tending to keep the old identification all else being equal.
   *
   * @param pick Pick object
   * @param tTime Travel-time object
   * @return Observability modified by empirical weights
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
    if ((!pick.idCode.equals(tTime.getPhCode()) || generic)
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
        if (phGroup.equals(tTime.getPhGroup()) || 
        		phGroup.equals(tTime.getAuxGroup()) || (phGroup.equals("Reg") && 
        		tTime.isRegional())) {
          amp *= LocUtil.GROUPWEIGHT;
          System.out.print(" Group");
        } // Otherwise use the other (non-group) weighting.
        else {
          amp *= otherWeight;
          System.out.print(" Other");
          // If we trust the phase identification and the arrival types 
          // of the phases don't match, make re-identifying even harder
          if (!pick.auto && TauUtil.arrivalType(phGroup)
                  != TauUtil.arrivalType(tTime.getPhCode())) {
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
          if (!pick.auto && TauUtil.arrivalType(phGroup)
                  != TauUtil.arrivalType(tTime.getPhCode())) {
            amp *= LocUtil.TYPEWEIGHT;
            System.out.print(" Type");
          }
        }
      }
    }

    // Account for the affinity.
    if (pick.idCode.equals(tTime.getPhCode())) {
      amp *= pick.affinity;
      System.out.print(" Aff");
    }

    // Make the existing identification harder to change.
    if (pick.phCode.equals(tTime.getPhCode())) {
      amp *= stickyWeight;
      System.out.print(" Sticky");
    }
    System.out.println("");
    return amp;
  }

  /**
   * Get the trial affinity. This is the affinity if the observed and 
   * theoretical phases match and unity otherwise.
   *
   * @param pick Pick object
   * @param tTime Travel-time object
   * @return The affinity weighted residual
   */
  private double idResidual(Pick pick, TTimeData tTime) {
    if (pick.idCode.equals(tTime.getPhCode())) {
      return Math.abs(pick.tt-tTime.getTT())/pick.affinity;
    } else {
      return Math.abs(pick.tt-tTime.getTT())/LocUtil.NULLAFFINITY;
    }
  }
}