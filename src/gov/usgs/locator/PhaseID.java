package gov.usgs.locator;

import gov.usgs.traveltime.TauUtil;
import gov.usgs.traveltime.TTSessionLocal;
import gov.usgs.traveltime.AuxTtRef;
import gov.usgs.traveltime.TTime;

import java.util.ArrayList;

import gov.usgs.traveltime.TTimeData;
import gov.usgs.traveltime.session.TTSession;
import gov.usgs.traveltime.session.TTSessionPool;

/**
 * Associate theoretical seismic phases with observed seismic picks.
 *
 * @author Ray Buland
 *
 */
public class PhaseID {
	double lastDepth = Double.NaN;
  Event event;
  Hypocenter hypo;
  TTSessionLocal ttLocal;
   AuxTtRef auxTT;
  ArrayList<Wresidual> wResiduals;
  PickGroup group;
  Pick lastPick = null;
  TTime ttList = null;
  TTSession session;
  double otherWeight;				// Weight for phases that don't match
  double stickyWeight;			// Weight to resist changing identification
  boolean generic = false, primary = false;
  String phGroup = null;

  /**
   * Remember the event and travel-time machinery.
   *
   * @param event Event object
   * @param ttLocal Travel time information for a local implementation
   */
  public PhaseID(Event event, TTSessionLocal ttLocal) {
    this.event = event;
    hypo = event.hypo;
    this.ttLocal = ttLocal;
    if(ttLocal != null) {
    	this.auxTT = ttLocal.getAuxTT();
    } else {
    	this.auxTT = null;
    }
    wResiduals = event.wResRaw;
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
    
    if(LocUtil.deBugLevel > 0) System.out.println("\nCurr loc: "+hypo+"\n");

    // Remember the figure-of-merit controls.
    this.otherWeight = otherWeight;
    this.stickyWeight = stickyWeight;
    
    // Initialize the changed flag.
    if(reWeight) changed = true;
    else changed = false;
    
		// Reinitialize the weighted residual storage.
		if(wResiduals.size() > 0) wResiduals.clear();

		// Set up a new travel-time session.
		if(LocUtil.server) {
			session = TTSessionPool.getTravelTimeSession(event.earthModel, hypo.depth, 
					LocUtil.PHLIST, hypo.latitude, hypo.longitude, LocUtil.ALLPHASES,
					LocUtil.BACKBRN, LocUtil.tectonic, LocUtil.rstt, false);
			if(auxTT == null) auxTT = session.getAuxTT();
		} else {
			ttLocal.newSession(event.earthModel, hypo.depth, LocUtil.PHLIST, 
					hypo.latitude, hypo.longitude, LocUtil.ALLPHASES, LocUtil.BACKBRN, 
					LocUtil.tectonic, LocUtil.rstt);
		}
		
    // Do the travel-time calculation.
    for (int j = 0; j < event.noStations(); j++) {
      group = event.groups.get(j);
      // For the first pick in the group, get the travel times.
      station = group.station;
      if(LocUtil.deBugLevel > 1) System.out.format("PhaseID: %-5s %6.2f "+
      		"%6.2f %6.2f\n", station.staID.staCode, group.picks.get(0).tt, 
      		group.delta, group.azimuth);
      if(LocUtil.server) {
      	ttList = session.getTT(station.latitude, station.longitude,
      			station.elevation, group.delta, group.azimuth);
      } else {
	      ttList = ttLocal.getTT(station.latitude, station.longitude,
	          station.elevation, group.delta, group.azimuth);
      }
      // Print them.
  //  if(station.staID.staCode.equals("TX11")) {
  //  	ttList.print();
  //  }
      // If reID is true, do a full phase re-identification.
      if(reID) {
      	reID();
      }
      // Otherwise, try not to re-identify the phases.
      else {
      	noReID();
      }
    if(group.updateID(reWeight, wResiduals)) changed = true;
    }
    // Add the Bayesian depth.
    wResiduals.add(new Wresidual(null, hypo.depthRes, hypo.depthWeight, true, 
    		0d, 0d, 1d));
    // Save a copy of wResiduals in the original order.
    event.saveWres();
    // Update the station statistics.
    event.staStats();
    return changed;
  }

  /**
   * During the location iteration, we don't want to re-identify phases, but 
   * sometimes re-identification is thrust upon us (e.g., when the depth or 
   * distance changes and the former identification no longer exists).
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
      		if(LocUtil.deBugLevel > 1) System.out.format("NoReID: got it "+
      				"%-5s %-8s %6.2f %2d\n", pick.station.staID.staCode, phCode, 
      				resMin, m);
      	// If the easy way doesn't work, we have to try harder.
      	} else {
      		phGroup = auxTT.findGroup(phCode, false);
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
        		if(LocUtil.deBugLevel > 1) System.out.format("NoReID: group "+
        				"%-5s %-8s -> %-8s %6.2f %2d\n", pick.station.staID.staCode, 
        				phCode, ttList.get(m).getPhCode(), resMin, m);
        	} else {
        		if(pick.used) {
        			if(LocUtil.deBugLevel > 1) System.out.println("NoReID: give up "+
        					pick.station.staID.staCode);
        			group.initFoM(j, j);
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
   */
  private void reID() {
    int ttBeg, ttLen, pickBeg, pickLen;
    double winMin, winMax;
    TTimeData tTime;
    Pick pick;

    // Initialize the figure-of-merit memory.
  	group.initFoM(0, group.picks.size());
  	
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
    if(LocUtil.deBugLevel > 1) System.out.println("\n\tClusters:");
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
        	if(LocUtil.deBugLevel > 1) System.out.format("TT: %2d %2d"+
        			"  Pick: %2d %2d  Win: %7.2f %7.2f\n", ttBeg, ttLen, 
        			pickBeg, pickLen, winMin, winMax);
          // Initialize the cumulative figure-of-merit.
        	group.fomMax = 0d;
          // Do the identification.
          permut(pickBeg, pickLen, ttBeg, ttLen);
        }
        // Quit if we're out of picks.
        if (i >= group.picks.size()) {
          break;
        }
        // Otherwise, set up the next cluster.
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
    if(deltaCorr > 1d) {
    	if(group.getPick(0).mapStat != null) 
    		group.getPick(0).fomStat /= deltaCorr;
    	if(group.getPick(0).mapAlt != null) 
    		group.getPick(0).fomAlt /= deltaCorr;
    }
    
    // Print out the chosen associations.
    if(LocUtil.deBugLevel > 2) printAssoc();
    
    // Finally, rationalize the two identification methods.
    fomMerge();
  }
  
  /**
   * Print out the associations chosen.  This is messy because of possible 
   * null pointers.
   */
  private void printAssoc() {
  	Pick pick;
  	
  	for(int j=0; j<group.noPicks(); j++) {
  		pick = group.getPick(j);
  		if(pick.mapStat != null) {
  			if(pick.mapAlt != null) {
      		System.out.format("  Sel: %1d %-8s %-8s %5.2f %5.2f\n", j, 
      				pick.mapStat.getPhCode(), pick.mapAlt.getPhCode(), pick.fomStat, 
      				pick.fomAlt);
  			} else {
      		System.out.format("  Sel: %1d %-8s null     %5.2f\n", j, 
      				pick.mapStat.getPhCode(), pick.fomStat);
  			}
  		} else {
  			if(pick.mapAlt != null) {
      		System.out.format("  Sel: %1d null     %-8s       %5.2f\n", j, 
      				pick.mapAlt.getPhCode(), pick.fomAlt);
  			} else {
      		System.out.format("  Sel: %1d null     null\n", j);
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
  	for(int j=0; j<group.noPicks()-1; j++) {
  		pick = group.getPick(j);
  		if(pick.mapStat != null) {
  			for(int i=j+1; i<group.noPicks(); i++) {
  				pick2 = group.getPick(i);
  				if(pick.mapStat == pick2.mapStat) {
  					if(j == 0) {
  						pick2.mapStat = null;
  					} else {
  						// The alternative figure-of-merits have to exist for this 
  						// problem to occur.
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
  	// Sometimes the arrival order of the picks and the order of the 
  	// theoretical phases are at odds.  If we leave it, it can cause problems, 
  	// so, just delete one of the identifications.
  	pick2 = group.getPick(0);
  	for(int j=1; j<group.noPicks(); j++) {
  		pick = pick2;
  		pick2 = group.getPick(j);
  		if(pick.mapStat != null && pick2.mapStat != null) {
  			if(pick.mapStat.getTT() > pick2.mapStat.getTT()) {
  				if(pick.mapStat.getObserv() >= pick2.mapStat.getObserv()) {
  					// Apparently, we don't care if Lg or LR are out of order.
  					if(!pick2.mapStat.getPhCode().equals("Lg") && 
  							!pick2.mapStat.getPhCode().equals("LR")) pick2.mapStat= null;
  				} else {
  					if(!pick.mapStat.getPhCode().equals("Lg") && 
  							!pick.mapStat.getPhCode().equals("LR")) pick.mapStat= null;
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
   * @param pickLen The number of picks to include in this phase identification
   * @param ttBeg Index of the first theoretical arrival that will be part of 
   * this phase identification
   * @param ttLen The number of theoretical arrivals to include in this phase 
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

    if(LocUtil.deBugLevel > 1) System.out.format("\n Permut: %2d Picks, "+
    		"%2d TTs\n", pickLen, ttLen);
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
   * @param ttClust An array of theoretical arrivals
   * @param len The length of the permutation subset
   * @param beg The starting pointer of the permutation subset
   * @param ttPermut The result of the permutation
   * @param pickClust An array of observed picks
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
   * @param pickClust An array of observed picks
   * @param len The length of the permutation subset
   * @param beg The starting pointer of the permutation subset
   * @param pickPermut The result of the permutation
   * @param ttClust An array of theoretical arrivals
   */
  private void kPermutOfN(Pick[] pickClust, int len, int beg, 
  		Pick[] pickPermut, TTimeData[] ttClust) {
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
   * @param pickClust An array of picks to be identified
   * @param ttClust An array of theoretical arrivals to test against
   */
  private void setFoM(Pick[] pickClust, TTimeData[] ttClust) {
    double prob, amp, res, cumFoM;

    cumFoM = 1d;
    // Make a pass computing the cumulative statistical figure-of-merit.
    for (int j = 0; j < ttClust.length; j++) {
    	if(!pickClust[j].surfWave) {
    		// Compute the figure-of-merit for the primary criteria.
	      prob = LocUtil.ttResModel(pickClust[j].tt - ttClust[j].getTT(), 0d, 
	      		ttClust[j].getSpread());
	      amp = idAmplitude(pickClust[j], ttClust[j]);
	      res = idResidual(pickClust[j], ttClust[j]);
	      if(LocUtil.deBugLevel > 1) System.out.format("\t%8s %8s: %10.4e "+
	      		"%10.4e\n", pickClust[j].idCode,ttClust[j].getPhCode(), prob, amp);
	      cumFoM *= amp*prob;
	      // Set up the alternative criteria at the same time.  Note, the 
	      // Fortran version omitted the affinity in this test.
	      if(ttClust[j].getObserv() >= LocUtil.OBSERVMIN && res < 
	      		pickClust[j].fomAlt) {
	      	// Make sure that the phase types match unless the pick is automatic.
	      	if(pickClust[j].auto || TauUtil.arrivalType(pickClust[j].idCode)
              == TauUtil.arrivalType(ttClust[j].getPhCode())) {
	      		pickClust[j].setFomAlt(ttClust[j], res);
	      		if(LocUtil.deBugLevel > 1) System.out.format("\t\tAlt: %4.2f\n", 
	      				res);
	      	}
	      }
    	}
    }
    
    // Make a second pass if this is the highest figure-of-merit yet.  Note, 
    // the Fortran version has greater than or equal to.
    if(LocUtil.deBugLevel > 2) System.out.format("\tCum: %10.4e %10.4e\n", 
    		cumFoM, group.fomMax);
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
   * @param pick Pick information for one pick
   * @param tTime Arrival time information for one arrival
   * @return Observability modified by empirical weights
   */
  private double idAmplitude(Pick pick, TTimeData tTime) {
    double amp;

    // Set up the observed pick phase group.
    if (pick != lastPick) {
      lastPick = pick;
      phGroup = auxTT.findGroup(pick.idCode, (pick.authType
              == AuthorType.CONTRIB_AUTO));
      primary = auxTT.isPrimary();
      if (phGroup.equals("Any") || pick.idCode.equals(phGroup)) {
        generic = true;
      } else {
        generic = false;
      }
      if(LocUtil.deBugLevel > 2) {
	      System.out.print("New " + phGroup);
	      if (primary) {
	        System.out.print(" Pri");
	      }
	      if (generic) {
	        System.out.print(" Gen");
	      }
      }
    } else {
    	if(LocUtil.deBugLevel > 2) System.out.print("Old");
    }

    // initialize the amplitude.
    if (!tTime.getDis()) {
      amp = tTime.getObserv();
    } else {
      amp = LocUtil.DOWNWEIGHT * tTime.getObserv();
      if(LocUtil.deBugLevel > 2) System.out.print(" Down");
    }

    // Do the group logic.  If the phase codes match drop through 
    // unless the phase might be generic.
    if ((!pick.idCode.equals(tTime.getPhCode()) || generic)
            && !phGroup.equals("Any")) {
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
          if(LocUtil.deBugLevel > 2) System.out.print(" Group1");
        } // Otherwise use the other (non-group) weighting.
        else {
          amp *= otherWeight;
          if(LocUtil.deBugLevel > 2) System.out.print(" Other1");
          // If we trust the phase identification and the arrival types 
          // of the phases don't match, make re-identifying even harder
          if (!pick.auto && TauUtil.arrivalType(phGroup)
                  != TauUtil.arrivalType(tTime.getPhCode())) {
            amp *= LocUtil.TYPEWEIGHT;
            if(LocUtil.deBugLevel > 2) System.out.print(" Type1");
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
          if(LocUtil.deBugLevel > 2) System.out.print(" Group2");
        } // Otherwise use the other (non-group) weighting.
        else {
          amp *= otherWeight;
          if(LocUtil.deBugLevel > 2) System.out.print(" Other2");
          // If we trust the phase identification and the arrival types 
          // of the phases don't match, make re-identifying even harder
          if (!pick.auto && TauUtil.arrivalType(phGroup)
                  != TauUtil.arrivalType(tTime.getPhCode())) {
            amp *= LocUtil.TYPEWEIGHT;
            if(LocUtil.deBugLevel > 2) System.out.print(" Type2");
          }
        }
      }
    }

    // Account for the affinity.
    if (pick.idCode.equals(tTime.getPhCode())) {
      amp *= pick.affinity;
      if(LocUtil.deBugLevel > 2) System.out.print(" Aff");
    }

    // Make the existing identification harder to change.
    if (pick.phCode.equals(tTime.getPhCode())) {
      amp *= stickyWeight;
      if(LocUtil.deBugLevel > 2) System.out.print(" Sticky");
    }
    if(LocUtil.deBugLevel > 2) System.out.println("");
    return amp;
  }

  /**
   * Get the affinity weighted travel-time residual.
   *
   * @param pick Pick information for one pick
   * @param tTime Arrival time information for one phase
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