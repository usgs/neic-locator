package gov.usgs.locator;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Store all volatile information associated with one travel-time branch.  
 * 
 * @author Ray Buland
 *
 */
public class BrnDataVol {
	boolean isUsed;					// True if travel times should be computed
	boolean exists;					// True if the corrected branch still exists
	double[] pRange;				// Corrected slowness range for this branch
	double[] xRange;				// Corrected distance range for this branch
	double pCaustic;				// Corrected slowness of a caustic, if any
	ArrayList<BrnPoint> brnGrid;
	BrnDataRef ref;
	UpDataVol pUp, sUp;
	
	/**
	 * Set up volatile copies of data that changes with depth
	 * 
	 * @param ref The branch reference data source
	 * @param pUp The corrected P up-going branch source
	 * @param sUp The corrected S up-going branch source
	 */
	public BrnDataVol(BrnDataRef ref, UpDataVol pUp, UpDataVol sUp) {
		this.ref = ref;
		this.pUp = pUp;
		this.sUp = sUp;
		
		// Do branch summary information.
		isUsed = true;
		exists = true;
		pRange = Arrays.copyOf(ref.pRange, ref.pRange.length);
		xRange = Arrays.copyOf(ref.xRange, ref.xRange.length);
		pCaustic = pRange[1];
	}
	
	/**
	 * Setter for the branch used flag.  The branch flags will need to 
	 * be reset for each new session.
	 * 
	 * @param isUsed If true compute travel times from this branch
	 */
	public void setIsUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}
	
	/**
	 * Correct this branch for source depth using the corrected 
	 * up-going branch ray parameters and tau values.
	 * @throws Exception 
	 */
	public void depthCorr(double xMin) throws Exception {
//	boolean gotLast;
		int i;
		double[] pDec, tauDec;
		
		if(isUsed) {
			exists = true;
			brnGrid = new ArrayList<BrnPoint>();
			
			// Do phases that start as P.
			if(ref.typeSeg[0] == 'P') {
				// Correct ray parameter.
				pRange[1] = Math.min(pRange[1], pUp.pMax);
				
				// Correct distance.
		//	gotLast = false;
				int m = 0;
				for(i=0; i<xRange.length; i++) {
					for(; m<pUp.ref.pXUp.length; m++) {
						if(Math.abs(pRange[i]-pUp.ref.pXUp[m]) <= TauUtil.dTol) {
							xRange[i] += ref.signSeg*pUp.xUp[m];
		//				if(i == xRange.length) gotLast = true;
							break;
						}
					}
					if(m >= pUp.ref.pXUp.length) xRange[i] = lastX();
				}
		//	if(!gotLast) xRange[1] = lastX();
				
				if(ref.isUpGoing) {
					// Decimate the up-going branch.
					pDec = pUp.realUp(xRange, xMin);
					tauDec = pUp.getDecTau();
					brnGrid.add(new BrnPoint(pDec[0], tauDec[0], xRange[0]));
					for(int j=1; j<pDec.length-1; j++) {
						brnGrid.add(new BrnPoint(pDec[j],tauDec[j], 0d));
					}
					brnGrid.add(new BrnPoint(pDec[pDec.length-1], 
							tauDec[pDec.length-1], xRange[1]));
					
				} else {
					// Correct tau for down-going branches.
					i = 0;
					for(int j=0; j<ref.pBrn.length; j++) {
						// Add the last point.
						if(ref.pBrn[j]-pUp.pMax >= TauUtil.dTol) {
							brnGrid.add(new BrnPoint(pUp.pMax, lastTau(), xRange[1]));
							break;
						// Otherwise, correct the next point.
						} else {
							// pTauUp is a superset of pBrn so we need to sync them.
							while(Math.abs(ref.pBrn[j]-pUp.pUp[i]) > TauUtil.dTol) {
								i++;
				/*			if(i >= pUp.pUp.length) {
									System.out.println("depthCorr: j i brnLen pUpLen = "+
											j+" "+i+" "+ref.pBrn.length+" "+pUp.brnLen+" "+
											pUp.pUp.length);
									for(int k=0; k<ref.pBrn.length; k++) 
										System.out.format("%3d  %13.6e %13.6e\n", k, pUp.pUp[k], 
												ref.pBrn[k]);
									for(int k=ref.pBrn.length; k<pUp.pUp.length; k++)
										System.out.format("%3d  %13.6e\n", k, pUp.pUp[k]);
								} */
							};
							// Correct the tau and x values.
							if(j == 0) brnGrid.add(new BrnPoint(ref.pBrn[j], 
									ref.tauBrn[j]+ref.signSeg*pUp.tauUp[i], xRange[0]));
							else if(j< ref.pBrn.length-1) 
									brnGrid.add(new BrnPoint(ref.pBrn[j],ref.tauBrn[j]+
									ref.signSeg*pUp.tauUp[i], 0d));
							else brnGrid.add(new BrnPoint(ref.pBrn[j], ref.tauBrn[j]+
									ref.signSeg*pUp.tauUp[i], xRange[1]));
						}
					}
				}
			// Do phases that start as S.
			} else {
				// Correct ray parameter.
				pRange[1] = Math.min(pRange[1], sUp.pMax);
				
				// Correct distance.
		//	gotLast = false;
				int m = 0;
				for(i=0; i<xRange.length; i++) {
					for(; m<sUp.ref.pXUp.length; m++) {
						if(Math.abs(pRange[i]-sUp.ref.pXUp[m]) <= TauUtil.dTol) {
							xRange[i] += ref.signSeg*sUp.xUp[m];
		//				if(i == xRange.length) gotLast = true;
							break;
						}
					}
					if(m >= sUp.ref.pXUp.length) xRange[i] = lastX();
				}
		//	if(!gotLast) xRange[1] = lastX();
				
				if(ref.isUpGoing) {
					// Decimate the up-going branch.
					pDec = sUp.realUp(xRange, xMin);
					tauDec = sUp.getDecTau();
					brnGrid.add(new BrnPoint(pDec[0], tauDec[0], xRange[0]));
					for(int j=1; j<pDec.length-1; j++) {
						brnGrid.add(new BrnPoint(pDec[j],tauDec[j], 0d));
					}
					brnGrid.add(new BrnPoint(pDec[pDec.length-1], 
							tauDec[pDec.length-1], xRange[1]));
					
				} else {
					// Correct tau for down-going branches.
					i = 0;
					for(int j=0; j<ref.pBrn.length; j++) {
						// Add the last point.
						if(ref.pBrn[j]-sUp.pMax >= TauUtil.dTol) {
							brnGrid.add(new BrnPoint(sUp.pMax, lastTau(), xRange[1]));
							break;
						// Otherwise, correct the next point.
						} else {
							// pTauUp is a superset of pBrn so we need to sync them.
							while(Math.abs(ref.pBrn[j]-sUp.pUp[i]) > TauUtil.dTol) {
								i++;
							};
							// Correct the tau value.
							if(j == 0) brnGrid.add(new BrnPoint(ref.pBrn[j], 
									ref.tauBrn[j]+ref.signSeg*sUp.tauUp[i], xRange[0]));
							else if(j< ref.pBrn.length-1) 
									brnGrid.add(new BrnPoint(ref.pBrn[j],ref.tauBrn[j]+
									ref.signSeg*sUp.tauUp[i], 0d));
							else brnGrid.add(new BrnPoint(ref.pBrn[j], ref.tauBrn[j]+
									ref.signSeg*sUp.tauUp[i], xRange[1]));
						}
					}
				}
			}
			if(brnGrid.size() == 0) exists = false;
		}
	}
	
	/**
	 * The tau value corresponding to the largest ray parameter (usually 
	 * the slowness at the source) is computed from the end integrals 
	 * computed as part of the up-going branch corrections.
	 * 
	 * @return Normalized tau for the maximum ray parameter for this branch
	 */
	private double lastTau() {
		double tau;
		
		if(ref.typeSeg[0] == 'P') {
			// Add or subtract the up-going piece.  For a surface reflection 
			// it would be added.  For a down-going branch it would be 
			// subtracted (because that part of the branch is cut off by the 
			// source depth).
			tau = ref.signSeg*pUp.tauEndUp;
			// Add the down-going part, which may not be the same as the 
			// up-going piece (e.g., sP).
			if(ref.typeSeg[1] == 'P') {
				tau += ref.countSeg*(pUp.tauEndUp+pUp.tauEndLvz);
			} else {
				tau += ref.countSeg*(pUp.tauEndCnv);
			}
			// Add the coming-back-up part, which may not be the same as the 
			// down-going piece (e.g., ScP).
			if(ref.typeSeg[2] == 'P') {
				tau += ref.countSeg*(pUp.tauEndUp+pUp.tauEndLvz);
			} else {
				tau += ref.countSeg*(pUp.tauEndCnv);
			}
		}
		else {
			// Add or subtract the up-going piece.  For a surface reflection 
			// it would be added.  For a down-going branch it would be 
			// subtracted (because that part of the branch is cut off by the 
			// source depth).
			tau = ref.signSeg*sUp.tauEndUp;
			// Add the down-going part, which may not be the same as the 
			// up-going piece (e.g., sP).
			if(ref.typeSeg[1] == 'S') {
				tau += ref.countSeg*(sUp.tauEndUp+sUp.tauEndLvz);
			} else {
				tau += ref.countSeg*(sUp.tauEndCnv);
			}
			// Add the coming-back-up part, which may not be the same as the 
			// down-going piece (e.g., ScP).
			if(ref.typeSeg[2] == 'S') {
				tau += ref.countSeg*(sUp.tauEndUp+sUp.tauEndLvz);
			} else {
				tau += ref.countSeg*(sUp.tauEndCnv);
			}
		}
		return tau;
	}
	
	/**
	 * The distance value corresponding to the largest ray parameter 
	 * (usually the slowness at the source) is computed from the end 
	 * integrals computed as part of the up-going branch corrections.
	 * 
	 * @return Normalized distance for the maximum ray parameter for this 
	 * branch
	 */
	private double lastX() {
		double x;
		
		if(ref.typeSeg[0] == 'P') {
			// Add or subtract the up-going piece.  For a surface reflection 
			// it would be added.  For a down-going branch it would be 
			// subtracted (because that part of the branch is cut off by the 
			// source depth).
			x = ref.signSeg*pUp.xEndUp;
			// Add the down-going part, which may not be the same as the 
			// up-going piece (e.g., sP).
			if(ref.typeSeg[1] == 'P') {
				x += ref.countSeg*(pUp.xEndUp+pUp.xEndLvz);
			} else {
				x += ref.countSeg*(pUp.xEndCnv);
			}
			// Add the coming-back-up part, which may not be the same as the 
			// down-going piece (e.g., ScP).
			if(ref.typeSeg[2] == 'P') {
				x += ref.countSeg*(pUp.xEndUp+pUp.xEndLvz);
			} else {
				x += ref.countSeg*(pUp.xEndCnv);
			}
		}
		else {
			// Add or subtract the up-going piece.  For a surface reflection 
			// it would be added.  For a down-going branch it would be 
			// subtracted (because that part of the branch is cut off by the 
			// source depth).
			x = ref.signSeg*sUp.xEndUp;
			// Add the down-going part, which may not be the same as the 
			// up-going piece (e.g., sP).
			if(ref.typeSeg[1] == 'S') {
				x += ref.countSeg*(sUp.xEndUp+sUp.xEndLvz);
			} else {
				x += ref.countSeg*(sUp.xEndCnv);
			}
			// Add the coming-back-up part, which may not be the same as the 
			// down-going piece (e.g., ScP).
			if(ref.typeSeg[2] == 'S') {
				x += ref.countSeg*(sUp.xEndUp+sUp.xEndLvz);
			} else {
				x += ref.countSeg*(sUp.xEndCnv);
			}
		}
		return x;
	}
	
	public String getPhSeg() {
		return ref.phSeg;
	}
	
	/**
	 * Print out branch information for debugging purposes.  Note that this 
	 * partly duplicates the print function in AllBrnRef, but includes 
	 * volatile data as well.
	 * 
	 * @param full If true print the detailed branch specification as well
	 */
	public void dumpBrn(boolean full, boolean sci) {
		if(isUsed & exists) {
			if(ref.isUpGoing) {
				if(ref.isDiff) System.out.format("\n          phase = %sup"+
						"  diff = %s\n", ref.phCode, ref.phDiff);
				else System.out.format("\n          phase = %sup\n", ref.phCode);
				System.out.format("Segment: code = %s  type = %c        sign = %2d"+
						"  count = %d\n", ref.phSeg, ref.typeSeg[0], ref.signSeg, 
						ref.countSeg);
			} else {
				if(ref.isDiff) System.out.format("\n          phase = %s"+
						"  diff = %s\n", ref.phCode, ref.phDiff);
				else System.out.format("\n          phase = %s\n", ref.phCode);
				System.out.format("Segment: code = %s  type = %c, %c, %c  "+
						"sign = %2d  count = %d\n", ref.phSeg, ref.typeSeg[0], 
						ref.typeSeg[1], ref.typeSeg[2], ref.signSeg, ref.countSeg);
			}
			System.out.format("Branch: pRange = %8.6f - %8.6f  xRange = %6.2f - %6.2f\n", 
					pRange[0], pRange[1], Math.toDegrees(xRange[0]), 
					Math.toDegrees(xRange[1]));
			if(ref.isDiff) System.out.format("        pCaustic = %8.6f  xDiff = %6.2f\n", 
					pCaustic, Math.toDegrees(ref.xDiff));
			else System.out.format("        pCaustic = %8.6f\n", pCaustic);
			if(full) {
				if(sci) {
					System.out.println("\n         p        tau                 "+
							"basis function coefficients");
					if(brnGrid.get(0).polyBrn != null) {
						for(int j=0; j<brnGrid.size(); j++) {
							System.out.format("%3d: %13.6e  %13.6e  %13.6e  %13.6e  %13.6e\n", 
									j, brnGrid.get(j).pBrn, brnGrid.get(j).tauBrn, 
									brnGrid.get(j).xBrn, brnGrid.get(j).polyBrn[0], 
									brnGrid.get(j).polyBrn[1]);
						}
					} else {
						System.out.format("%3d: %13.6e  %13.6e  %13.6e\n", 0, brnGrid.get(0).pBrn, 
								brnGrid.get(0).tauBrn, brnGrid.get(0).xBrn);
						for(int j=1; j<brnGrid.size()-1; j++) {
							System.out.format("%3d: %13.6e  %13.6e\n", j, brnGrid.get(j).pBrn, 
									brnGrid.get(j).tauBrn);
						}
						System.out.format("%3d: %13.6e  %13.6e  %13.6e\n", brnGrid.size()-1, 
								brnGrid.get(brnGrid.size()-1).pBrn, 
								brnGrid.get(brnGrid.size()-1).tauBrn, 
								brnGrid.get(brnGrid.size()-1).xBrn);
					}
				} else {
					System.out.println("\n         p        tau                 "+
							"basis function coefficients");
					if(brnGrid.get(0).polyBrn != null) {
						for(int j=0; j<brnGrid.size(); j++) {
							System.out.format("%3d: %8.6f  %8.6f  %8.6f  %9.2e  %9.2e\n", 
									j, brnGrid.get(j).pBrn, brnGrid.get(j).tauBrn, 
									brnGrid.get(j).xBrn, brnGrid.get(j).polyBrn[0], 
									brnGrid.get(j).polyBrn[1]);
						}
					} else {
						System.out.format("%3d: %8.6f  %8.6f  %8.6f\n", 0, brnGrid.get(0).pBrn, 
								brnGrid.get(0).tauBrn, brnGrid.get(0).xBrn);
						for(int j=1; j<brnGrid.size()-1; j++) {
							System.out.format("%3d: %8.6f  %8.6f\n", j, brnGrid.get(j).pBrn, 
									brnGrid.get(j).tauBrn);
						}
						System.out.format("%3d: %8.6f  %8.6f  %8.6f\n", brnGrid.size()-1, 
								brnGrid.get(brnGrid.size()-1).pBrn, 
								brnGrid.get(brnGrid.size()-1).tauBrn, 
								brnGrid.get(brnGrid.size()-1).xBrn);
					}
				}
			}
		} else if(!isUsed) {
			System.out.format("\n          phase = %sup not used\n", ref.phCode);
		} else if(!exists) {
			System.out.format("\n          phase = %sup doesn't exist\n", ref.phCode);
		}
	}
	
	/**
	 * Override the default toString function.  Used to generate a branch 
	 * summary table.
	 */
	public String toString() {
		if(ref.isUpGoing) {
			return String.format("%1sup      %7.4f %7.4f %7.2f %7.2f %7.4f"+
					"          %c %c %c %2d %d %b", ref.phCode, pRange[0], pRange[1], 
					Math.toDegrees(xRange[0]), Math.toDegrees(xRange[1]), pCaustic, 
					ref.typeSeg[0], ref.typeSeg[1], ref.typeSeg[2], ref.signSeg, 
					ref.countSeg, isUsed);
		}
		if(ref.isDiff) {
			return String.format("%-8s %7.4f %7.4f %7.2f %7.2f %7.4f %7.2f"+
					"  %c %c %c %2d %d %b", ref.phCode, pRange[0], pRange[1], 
					Math.toDegrees(xRange[0]), Math.toDegrees(xRange[1]), pCaustic, 
					Math.toDegrees(ref.xDiff), ref.typeSeg[0], ref.typeSeg[1], 
					ref.typeSeg[2], ref.signSeg, ref.countSeg, isUsed);
		} else {
			return String.format("%-8s %7.4f %7.4f %7.2f %7.2f %7.4f"+
					"          %c %c %c %2d %d %b", ref.phCode, pRange[0], pRange[1], 
					Math.toDegrees(xRange[0]), Math.toDegrees(xRange[1]), pCaustic, 
					ref.typeSeg[0], ref.typeSeg[1], ref.typeSeg[2], ref.signSeg, 
					ref.countSeg, isUsed);
		}
	}
}

/**
 * Internal class used to store one ray parameter of the volatile corrected 
 * branch data needed to find travel-time as a function of distance.
 * 
 * @author Ray Buland
 *
 */
class BrnPoint {
	double pBrn;						// Corrected slowness for this grid point
	double tauBrn;					// Corrected tau for this grid point
	double xBrn;						// Corrected distance for this grid point
	double[] polyBrn = null;	// Interpolation polynomial for this grid interval
	
	/**
	 * Sets up the basic parameters for one branch grid point.  Note that 
	 * distance is a place holder except at the end points.  The rest will 
	 * be filled in when the interpolation is completed.
	 * 
	 * @param p Normalized ray parameter
	 * @param tau Normalized, depth corrected tau
	 * @param x Normalized, depth corrected distance
	 */
	BrnPoint(double p, double tau, double x) {
		pBrn = p;
		tauBrn = tau;
		xBrn = x;
	}
	
	/**
	 * Make up the interpolation polynomial for the ray parameter interval 
	 * starting with this grid point.
	 */
	void makePoly() {
		polyBrn = new double[2];
	}
}