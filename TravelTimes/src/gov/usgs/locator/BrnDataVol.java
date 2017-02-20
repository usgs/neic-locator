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
	String phCode;					// Corrected phase code
	double[] pRange;				// Corrected slowness range for this branch
	double[] xRange;				// Corrected distance range for this branch
	double pCaustic;				// Corrected slowness of a caustic, if any
	double[] pBrn;					// Updated ray parameter grid
	double[] tauBrn;				// Corrected tau values
	double[][] polyBrn;			// Interpolation polynomial for tau(p)
	double[][] xLim;				// Distance limits for each ray parameter interval
	int iMin;								// Count of caustic minimums in this branch
	int iMax;								// Count of caustic maximums in this branch
	String[] flag;					// Min/max flag highlighting caustics for printing
	int[] xTries;						// Number of valid distance ranges for travel times
	double dTdDepth;				// Normalization for the depth derivative
	BrnDataRef ref;
	UpDataVol pUp, sUp;
	ModConvert cvt;
	
	/**
	 * Set up volatile copies of data that changes with depth
	 * 
	 * @param ref The branch reference data source
	 * @param pUp The corrected P up-going branch source
	 * @param sUp The corrected S up-going branch source
	 */
	public BrnDataVol(BrnDataRef ref, UpDataVol pUp, UpDataVol sUp, ModConvert cvt) {
		this.ref = ref;
		this.pUp = pUp;
		this.sUp = sUp;
		this.cvt = cvt;
		
		// Do branch summary information.
		isUsed = true;
		exists = true;
		phCode = ref.phCode;
		pRange = Arrays.copyOf(ref.pRange, ref.pRange.length);
		xRange = Arrays.copyOf(ref.xRange, ref.xRange.length);
		pCaustic = pRange[1];
	}
	
	/**
	 * Correct this branch for source depth using the corrected 
	 * up-going branch ray parameters and tau values.
	 * @throws Exception 
	 */
	public void depthCorr(double dTdDepth, double xMin, char tagBrn) 
			throws Exception {
		int i, len = 0;
		double[][] basisBrn;
		Spline spline = new Spline();
		this.dTdDepth = dTdDepth;
		
		if(isUsed) {
			exists = true;
			
			// Do phases that start as P.
			if(ref.typeSeg[0] == 'P') {
				// Correct ray parameter.
				pRange[1] = Math.min(pRange[1], pUp.pMax);
				
				// Correct distance.
				int m = 0;
				for(i=0; i<xRange.length; i++) {
					for(; m<pUp.ref.pXUp.length; m++) {
						if(Math.abs(pRange[i]-pUp.ref.pXUp[m]) <= TauUtil.DTOL) {
							xRange[i] += ref.signSeg*pUp.xUp[m];
							break;
						}
					}
					if(m >= pUp.ref.pXUp.length) xRange[i] = lastX();
				}
				
				// Correct an up-going branch separately.
				if(ref.isUpGoing) {
					// Decimate the up-going branch.
					pBrn = pUp.realUp(xRange, xMin);
					tauBrn = pUp.getDecTau();
					len = pBrn.length;
					basisBrn = new double[5][len];
					spline.basisSet(pBrn, basisBrn);
					polyBrn = new double[4][len-1];
					spline.tauSpline(pBrn, tauBrn, xRange, basisBrn, polyBrn);
				
				// Otherwise,correct a down-going branch.
				} else {
					// See how long we need the corrected arrays to be.
					for(int j=0; j<ref.pBrn.length; j++) {
						// See if we need this point.
						if(ref.pBrn[j] < pUp.pMax+TauUtil.DTOL) {
							len++;
							// If this point is equal to pMax, we're done.
							if(Math.abs(ref.pBrn[j]-pUp.pMax) <= TauUtil.DTOL) break;
						// Otherwise, add one more point and quit.
						} else {
							len++;
							break;
						}
					}
					// If the branch is empty, it doesn't exist for this source 
					// depth.
					if(len == 0) {
						exists = false;
						return;
					}
					// Otherwise, allocate the arrays.
					pBrn = new double[len];
					tauBrn = new double[len];
					
					// Correct tau for down-going branches.
					i = 0;
					for(int j=0; j<ref.pBrn.length; j++) {
						// See if we need this point.
						if(ref.pBrn[j] < pUp.pMax+TauUtil.DTOL) {
							// pTauUp is a superset of pBrn so we need to sync them.
							while(Math.abs(ref.pBrn[j]-pUp.pUp[i]) > TauUtil.DTOL) {
								i++;
							};
							// Correct the tau and x values.
							pBrn[j] = ref.pBrn[j];
							tauBrn[j] = ref.tauBrn[j]+ref.signSeg*pUp.tauUp[i];
							// If this point is equal to pMax, we're done.
							if(Math.abs(ref.pBrn[j]-pUp.pMax) <= TauUtil.DTOL) break;
						// Otherwise, add one more point and quit.
						} else {
							pBrn[j] = pUp.pMax;
							tauBrn[j] = lastTau();
							break;
						}
					}
					polyBrn = new double[4][len-1];
					spline.tauSpline(pBrn, tauBrn, xRange, ref.basisBrn, polyBrn);
				}
			// Do phases that start as S.
			} else {
				// Correct ray parameter.
				pRange[1] = Math.min(pRange[1], sUp.pMax);
				
				// Correct distance.
				int m = 0;
				for(i=0; i<xRange.length; i++) {
					for(; m<sUp.ref.pXUp.length; m++) {
						if(Math.abs(pRange[i]-sUp.ref.pXUp[m]) <= TauUtil.DTOL) {
							xRange[i] += ref.signSeg*sUp.xUp[m];
							break;
						}
					}
					if(m >= sUp.ref.pXUp.length) xRange[i] = lastX();
				}
				
				// Correct an up-going branch separately.
				if(ref.isUpGoing) {
					// Decimate the up-going branch.
					pBrn = sUp.realUp(xRange, xMin);
					tauBrn = sUp.getDecTau();
					len = pBrn.length;
					basisBrn = new double[5][len];
					spline.basisSet(pBrn, basisBrn);
					polyBrn = new double[4][len-1];
					spline.tauSpline(pBrn, tauBrn, xRange, basisBrn, polyBrn);
					
				// Otherwise,correct a down-going branch.
				} else {
					// See how long we need the corrected arrays to be.
					for(int j=0; j<ref.pBrn.length; j++) {
						// See if we need this point.
						if(ref.pBrn[j] < sUp.pMax+TauUtil.DTOL) {
							len++;
							// If this point is equal to pMax, we're done.
							if(Math.abs(ref.pBrn[j]-sUp.pMax) <= TauUtil.DTOL) break;
						// Otherwise, add one more point and quit.
						} else {
							len++;
							break;
						}
					}
					// If the branch is empty, it doesn't exist for this source 
					// depth.
					if(len == 0) {
						exists = false;
						return;
					}
					// Otherwise, allocate the arrays.
					pBrn = new double[len];
					tauBrn = new double[len];
					
					// Correct tau for down-going branches.
					i = 0;
					for(int j=0; j<ref.pBrn.length; j++) {
						// See if we need this point.
						if(ref.pBrn[j] < sUp.pMax+TauUtil.DTOL) {
							// pTauUp is a superset of pBrn so we need to sync them.
							while(Math.abs(ref.pBrn[j]-sUp.pUp[i]) > TauUtil.DTOL) {
								i++;
							};
							// Correct the tau and x values.
							pBrn[j] = ref.pBrn[j];
							tauBrn[j] = ref.tauBrn[j]+ref.signSeg*sUp.tauUp[i];
							// If this point is equal to pMax, we're done.
							if(Math.abs(ref.pBrn[j]-sUp.pMax) <= TauUtil.DTOL) break;
						// Otherwise, add one more point and quit.
						} else {
							pBrn[j] = sUp.pMax;
							tauBrn[j] = lastTau();
							break;
						}
					}
					polyBrn = new double[4][len-1];
					spline.tauSpline(pBrn, tauBrn, xRange, ref.basisBrn, polyBrn);
				}
			}
			// Complete everything we'll need to compute a travel time.
			xLim = new double[2][len-1];
			flag = new String[len-1];
			tauPoly(tagBrn);
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
	
	private void tauPoly(char tagBrn) {
		int n;
		double pEnd, dp, dtau, xMin, xMax, pExt, sqrtPext, xCaustic;
		double[] x, dpe, sqrtDp, sqrt3Dp;
		
		// Fill in the rest of the interpolation polynomial.  Note that 
		// distance will be overwritten with the linear polynomial 
		// coefficient.
		n = tauBrn.length;
		pEnd = pBrn[n-1];
		x = new double[2];
		dpe = new double[2];
		sqrtDp = new double[2];
		sqrt3Dp = new double[2];
		x[1] = polyBrn[1][0];
		dpe[1] = pEnd-pBrn[0];
		sqrtDp[1] = Math.sqrt(dpe[1]);
		sqrt3Dp[1] = dpe[1]*sqrtDp[1];
		// Set up variables for tracking caustics.
		iMin = 0;
		iMax = 0;
		pCaustic = pBrn[n-1];
		xMin = xRange[0];
		xMax = xMin;
		
		// Loop over ray parameter intervals.
		for(int j=0; j<n-2; j++) {			
			// Complete the interpolation polynomial.
			x[0] = x[1];
			dpe[0] = dpe[1];
			sqrtDp[0] = sqrtDp[1];
			sqrt3Dp[0] = sqrt3Dp[1];
			x[1] = polyBrn[1][j+1];
			dpe[1] = pEnd-pBrn[j+1];
			sqrtDp[1] = Math.sqrt(dpe[1]);
			sqrt3Dp[1] = dpe[1]*sqrtDp[1];
			dp = pBrn[j]-pBrn[j+1];
			dtau = tauBrn[j+1]-tauBrn[j];
			polyBrn[3][j] = (2d*dtau-dp*(x[1]+x[0]))/
					(0.5d*(sqrt3Dp[1]-sqrt3Dp[0])-1.5d*sqrtDp[1]*sqrtDp[0]*
					(sqrtDp[1]-sqrtDp[0]));
			polyBrn[2][j] = (dtau-dp*x[0]-(sqrt3Dp[1]+0.5d*sqrt3Dp[0]-
					1.5d*dpe[1]*sqrtDp[0])*polyBrn[3][j])/Math.pow(dp,2d);
			polyBrn[1][j] = (dtau-(Math.pow(dpe[1],2d)-Math.pow(dpe[0],2d))*
					polyBrn[2][j]-(sqrt3Dp[1]-sqrt3Dp[0])*polyBrn[3][j])/dp;
			polyBrn[0][j] = tauBrn[j]-sqrt3Dp[0]*polyBrn[3][j]-
					dpe[0]*(dpe[0]*polyBrn[2][j]+polyBrn[1][j]);
			
			// Set up the distance limits.
			xLim[0][j] = Math.min(x[0],x[1]);
			xLim[1][j] = Math.max(x[0],x[1]);
			if(xLim[0][j] < xMin) {
				xMin = xLim[0][j];
				if(x[0] <= x[1]) pCaustic = pBrn[j];
				else pCaustic = pBrn[j+1];
			}
			// See if there's a caustic in this interval.
			flag[j] = "";
			if(Math.abs(polyBrn[2][j]) > TauUtil.DMIN) {
				sqrtPext = -0.375d*polyBrn[3][j]/polyBrn[2][j];
				pExt = Math.pow(sqrtPext,2d);
				if(sqrtPext > 0d && pExt > dpe[1] && pExt < dpe[0]) {
					xCaustic = polyBrn[1][j]+sqrtPext*(2d*sqrtPext*
							polyBrn[2][j]+1.5d*polyBrn[3][j]);
					xLim[0][j] = Math.min(xLim[0][j],xCaustic);
					xLim[1][j] = Math.max(xLim[1][j],xCaustic);
					if(xCaustic < xMin) {
						xMin = xCaustic;
						pCaustic = pEnd-pExt;
					}
					if(polyBrn[3][j] < 0d) {
						flag[j] = "min";
						iMin++;
					}
					else {
						flag[j] = "max";
						iMax++;
					}
				}
			}
			xMax = Math.max(xMax,xLim[1][j]);
		}
		
		// Fix ranges.
		xRange[0] = xMin;
		xRange[1] = xMax;
		// Set the distances to try (see findTt for details).
		for(int j=0; j<2; j++) {
			if(xRange[j] <= Math.PI) xTries[j] = 0;
			else if(xRange[j] <= 2d*Math.PI) xTries[j] = 1;
			else xTries[j] = 2;
		}
		// Fix the phase code for the up-going branch.
		if(ref.isUpGoing && tagBrn != ' ') phCode = ""+phCode.charAt(0)+
				tagBrn;
	}
	
	/**
	 * Get the travel times for this branch.
	 * 
	 * @param An array of possible distances to search
	 * @param TTimes A list of travel times
	 */
	public void getTT(double[] xs, ArrayList<TTime> TTimes) {
		String tmpCode;
		boolean found = false;
		int l;
		double xSign, zSign = Double.NaN, pSourceSq = Double.NaN, 
				pEnd = Double.NaN, pTol, dps = Double.NaN, dp, ps;
		
		// Set up the conversion for dT/dDelta.
		xSign = cvt.dTdDelta*Math.pow(-1d, xTries[0]+1);
		
		// Loop over possible distances.
		for(int i=xTries[0]; i<=xTries[1]; i++) {
			xSign = -xSign;
			// See if we have an arrival at this distance.
			if(xs[i] >= xRange[0] && xs[i] <= xRange[1]) {
				// Set up some useful variables.
				if(pEnd == Double.NaN) {
					pEnd = pBrn[pBrn.length-1];
					zSign = dTdDepth*ref.signSeg;
					if(ref.typeSeg[0] == 'P') pSourceSq = Math.pow(pUp.pSource,2d);
					else pSourceSq = Math.pow(sUp.pSource,2d);
				}
				// Loop over ray parameter intervals looking for arrivals.
				for(int j=0; j<xLim[0].length; j++) {
					if(xs[i] > xLim[0][j] && xs[i] <= xLim[1][j]) {
						// pTol is a totally empirically tolerance.
						pTol = Math.max(3e-6d*(pBrn[j+1]-pBrn[j]),1e-4d);
						
						// This is the general case.
						if(Math.abs(polyBrn[2][j]) > TauUtil.DMIN) {
							// There should be two solutions.
							for(int k=0; k<2; k++) {
								if(k == 0) {
									dps = -(3d*polyBrn[3][j]+
											Math.copySign(Math.sqrt(Math.abs(9d*
											Math.pow(polyBrn[3][j],2d)+32d*polyBrn[2][j]*
											(xs[i]-polyBrn[1][j]))),polyBrn[3][j]))/
											(8d*polyBrn[2][j]);
								} else {
									dps = (polyBrn[1][j]-xs[i])/(2d*polyBrn[2][j]*dps);
								}
								dp = Math.copySign(Math.pow(dps,2d),dps);
								// Arrivals outside the interval aren't real.
								if(dp < pEnd-pBrn[j+1]-pTol  || dp > pEnd-pBrn[j]+pTol) 
									break;
								// Add the arrival.
								found = true;
								ps = pEnd-dp;
								tmpCode = phCode;
								l = tmpCode.indexOf("ab");
								if(l >= 0 && ps <= pCaustic) tmpCode = 
										tmpCode.substring(0, l-1)+"bc";
								if(TTimes == null) TTimes = new ArrayList<TTime>();
								TTimes.add(new TTime(tmpCode,cvt.tNorm*(polyBrn[0][j]+
										dp*(polyBrn[1][j]+dp*polyBrn[2][j]+
										dps*polyBrn[3][j])+ps*xs[i]),xSign*ps,zSign*
										Math.sqrt(Math.abs(pSourceSq-Math.pow(ps,2d))),
										-(2d*polyBrn[2][j]+0.75d*polyBrn[3][j]/
										Math.max(Math.abs(dps),TauUtil.DTOL))/cvt.tNorm));
							}
						// We have to be careful if the quadratic term is zero.
						} else {
							// On the plus side, there's only one solution.
							dps = (xs[i]-polyBrn[1][j])/(1.5d*polyBrn[3][j]);
							dp = Math.copySign(Math.pow(dps,2d),dps);
							// Arrivals outside the interval aren't real.
							if(dp < pEnd-pBrn[j+1]-pTol  || dp > pEnd-pBrn[j]+pTol) 
								break;
							// Add the arrival.
							found = true;
							ps = pEnd-dp;
							tmpCode = phCode;
							l = tmpCode.indexOf("ab");
							if(l >= 0 && ps <= pCaustic) tmpCode = 
									tmpCode.substring(0, l-1)+"bc";
							if(TTimes == null) TTimes = new ArrayList<TTime>();
							TTimes.add(new TTime(tmpCode,cvt.tNorm*(polyBrn[0][j]+
									dp*(polyBrn[1][j]+dps*polyBrn[3][j])+ps*xs[i]),
									xSign*ps,zSign*Math.sqrt(Math.abs(pSourceSq-
									Math.pow(ps,2d))),-(0.75d*polyBrn[3][j]/
									Math.max(Math.abs(dps),TauUtil.DTOL))/cvt.tNorm));
						}
						if(!found) {
							if(ref.isUpGoing) System.out.format("%sup not found for "+
									"distance %6.2f\n",phCode.trim(),xs[i]);
							else System.out.format("%s not found for distance %6.2f\n",
									phCode.trim(),xs[i]);
						}
					}
				}
			}
			if(ref.isDiff) {
				
			}
		}
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
	 * get the branch segment code.
	 * 
	 * @return Branch segment code
	 */
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
						"  diff = %s  isUsed = %b  \n", ref.phCode, ref.phDiff, isUsed);
				else System.out.format("\n          phase = %sup  isUsed = %b  \n", 
						ref.phCode, isUsed);
				System.out.format("Segment: code = %s  type = %c        sign = %2d"+
						"  count = %d\n", ref.phSeg, ref.typeSeg[0], ref.signSeg, 
						ref.countSeg);
			} else {
				if(ref.isDiff) System.out.format("\n          phase = %s"+
						"  diff = %s  isUsed = %b\n", ref.phCode, ref.phDiff, isUsed);
				else System.out.format("\n          phase = %s  isUsed = %b  \n", 
						ref.phCode, isUsed);
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
					if(polyBrn != null) {
						for(int j=0; j<pBrn.length; j++) {
							System.out.format("%3d: %13.6e  %13.6e  %13.6e  %13.6e  %13.6e  "+
									"%13.6e\n", j, pBrn[j], tauBrn[j], polyBrn[0][j], polyBrn[1][j], 
									polyBrn[2][j], polyBrn[3][j]);
						}
					} else {
						System.out.format("%3d: %13.6e  %13.6e  %13.6e\n", 0, pBrn[0], 
								tauBrn[0], xRange[0]);
						for(int j=1; j<pBrn.length-1; j++) {
							System.out.format("%3d: %13.6e  %13.6e\n", j, pBrn[j], tauBrn[j]);
						}
						System.out.format("%3d: %13.6e  %13.6e  %13.6e\n", pBrn.length-1, 
								pBrn[pBrn.length-1], tauBrn[pBrn.length-1], xRange[1]);
					}
				} else {
					System.out.println("\n         p        tau                 "+
							"basis function coefficients");
					if(polyBrn != null) {
						for(int j=0; j<pBrn.length; j++) {
							System.out.format("%3d: %8.6f  %8.6f  %9.2e  %9.2e  %9.2e  %9.2e\n", 
									j, pBrn[j], tauBrn[j], polyBrn[0][j], polyBrn[1][j], 
									polyBrn[2][j], polyBrn[3][j]);
						}
					} else {
						System.out.format("%3d: %8.6f  %8.6f  %8.6f\n", 0, pBrn[0], 
								tauBrn[0], xRange[0]);
						for(int j=1; j<pBrn.length-1; j++) {
							System.out.format("%3d: %8.6f  %8.6f\n", j, pBrn[j], tauBrn[j]);
						}
						System.out.format("%3d: %8.6f  %8.6f  %8.6f\n", pBrn.length-1, 
								pBrn[pBrn.length-1], tauBrn[pBrn.length-1], xRange[1]);
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