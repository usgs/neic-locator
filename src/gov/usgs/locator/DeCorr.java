package gov.usgs.locator;

import java.util.ArrayList;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import gov.usgs.traveltime.TauUtil;

/**
 * Handle the decorrelation of the pick data.  This involves 
 * a singular valued decomposition of the correlation matrix.
 * 
 * @author Ray Buland
 *
 */
public class DeCorr {
	int n;								// Number of data
	int n1;								// Number of pick data
	int m;								// number of projected data
	double zSum;					// Sum of the vertical derivatives
	double[] e;						// Eigenvalues
	double[] w;						// Projected weights
	double[][] cov;				// Covariance matrix
	double[][] v;					// Eigenvectors
	Event event;
	ArrayList<Wresidual> wResOrg;
	ArrayList<Wresidual> wResProj;
	ArrayList<Wresidual> wResProjOrg;

	/**
	 * Remember the event.
	 * 
	 * @param event Event information
	 */
	public DeCorr(Event event) {
		this.event = event;
		wResProj = event.wResProj;
	}
	
	/**
	 * Do the decorrelation.
	 */
	public void deCorr() {
		// We can't remember the original sort of the raw residuals 
		// because it keeps changing when cloned.
		wResOrg = event.wResOrg;
		event.printWres("Org", true);
		// We'll use the dimension of the picks a lot!
		n = wResOrg.size();
		n1 = n-1;
		// Create the covariance matrix.
		makeCov();
		// If the matrix is too big, get rid of the most correlated 
		// data.
		if(wResOrg.size() > LocUtil.MAXCORR) triage();
		// Do the eigenvalue problem.
		doEigen();
	}
	
	/**
	 * Project the raw pick data into a set of decorrelated virtual 
	 * picks.
	 */
	@SuppressWarnings("unchecked")
	public void project() {
		Wresidual wRes;
		
		// Project the pick data.
		wResOrg = event.wResOrg;
		if(wResProj.size() > 0) wResProj.clear();
		for(int i=m; i<n1; i++) {
			wRes = new Wresidual(null, 0d, w[i-m], false);
			wRes.addDeriv(0d, 0d, 0d);
			for(int j=0; j<n1; j++) {
				wRes.proj(wResOrg.get(j), v[j][i]);
			}
			if(event.changed) {
				// See if the eigenvector is backwards.
				if(!eigenSign(i,wRes)) {
					// If so, fix the residual and derivatives.
					wRes.changeSign();
					// Fix the eigenvector so it will be right next time.
					for(int j=0; j<n1; j++) {
						v[j][i] = -v[j][i];
					}
				}
			}
			wResProj.add(wRes);
		}
		// Set the projected or virtual number of picks.
		event.vPhUsed = wResProj.size();
		// Add the Bayesian depth here since it doesn't correlate with 
		// anything else.
		wResProj.add(wResOrg.get(n1));
		// Because the weighted residuals get sorted, we need a copy in 
		// the original order to project the estimated residuals.
		wResProjOrg = (ArrayList<Wresidual>)wResProj.clone();
	}
	
	/**
	 * Project the estimated pick residuals into a set of decorrelated 
	 * virtual estimated residuals.
	 */
	public void estProject() {
		Wresidual wRes;
		
		// Project the pick data.
		if(wResProj.size() > 0) wResProj.clear();
		for(int i=m; i<n1; i++) {
			wRes = wResProjOrg.get(i-m);
			wRes.estResidual = 0d;
			for(int j=0; j<n1; j++) {
				wRes.estProj(wResOrg.get(j), v[j][i]);
			}
			wResProj.add(wRes);
		}
		// Add the Bayesian depth here since it doesn't correlate with 
		// anything else.
		wResProj.add(wResOrg.get(n1));
	}
	
	/**
	 * Create the correlation matrix.  Note that the Bayesian depth is 
	 * excluded from the covariance matrix.  This makes it easier to 
	 * trim the eigenvalues.  Since it doesn't correlate with anything 
	 * else anyway, it can be added back into the projected data later.
	 */
	private void makeCov() {
		Pick pickI;
		
		// Do the pick covariance.
		cov = new double[n1][n1];
		for(int i=0; i<n1; i++) {
			pickI = wResOrg.get(i).pick;
			for(int j=i; j<n1; j++) {
				cov[i][j] = LocUtil.covariance(pickI, wResOrg.get(j).pick);
				cov[j][i] = cov[i][j];
			}
		}
		// This is a convenient place to sum the vertical derivatives.
		zSum = 0d;
		for(int j=0; j<n; j++) {
			zSum += wResOrg.get(j).deriv[2];
		}
	}
	
	/**
	 * Get rid of the most correlated picks until the eigenvalue problem 
	 * is a reasonable size.
	 */
	private void triage() {
		
	}
	
	/**
	 * Get the eigenvalues and eigenvectors of the covariance matrix.
	 */
	private void doEigen() {
		double evSum, evMax, evLim, evThresh;
		Matrix matrix;
		EigenvalueDecomposition eig;
		
		// Do the eigenvalue problem (and time it).
		matrix = new Matrix(cov);
		if(LocUtil.deBugLevel > 0) LocUtil.timer();
		eig = matrix.eig();
		if(LocUtil.deBugLevel > 0) LocUtil.timer("Eigenvalue");
		e = eig.getRealEigenvalues();
		if(LocUtil.deBugLevel > 0) LocUtil.printMatrix(e, "Eigenvalues");
		v = eig.getV().getArray();
		// We don't need the covariance matrix any more.
		cov = null;
		matrix = null;
		
		// Work out the eigenvalue elimination.
		evSum = 0d;
		// Add up all the eigenvalues.
		for(int j=0; j<n1; j++) {
			evSum += e[j];
		}
		// The largest eigenvalue is always the last.
		evMax = e[n1-1];
		// Now set the limits.
		evLim = LocUtil.EVLIM*evSum;
		evThresh = LocUtil.EVTHRESH*evMax;
		// Find how many eigenvalues to keep.
		evSum = 0d;
		for(m=n1-1; m>=0; m--) {
			evSum += e[m];
			if(evSum > evLim && e[m] <= evThresh) break;
		}
		m++;
		// Print it out.
		if(LocUtil.deBugLevel > 0) System.out.format("\nProject: wsum wlim"+
				" elim: %10.3e %10.3e %10.3e m: %5d\n\n", evSum, evLim, evThresh, m);
		
		// Get the corresponding weights.
		w = new double[n-m];
		for(int j=m; j<n1; j++) {
			w[j-m] = 1d/Math.sqrt(e[j]);
		}
		w[n1-m] = wResOrg.get(n1).weight;
	}
	
	/**
	 * The signs of the eigenvectors are arbitrary, but not the linear 
	 * combination of picks implied.  If the sign is wrong, physically 
	 * the correlated cluster of stations will appear to be at an 
	 * azimuth of 180 degrees from where it actually is.  Alternatively, 
	 * the residual will have the wrong sign, which, of course, will 
	 * wreak havoc on the location.
	 * 
	 * @param i Index of the eigenvector
	 * @param wRes Weighted residual storage for the projected pick
	 * @return True if the sign is OK
	 */
	private boolean eigenSign(int i, Wresidual wRes) {
		double corr, corrMax = -1d, corrMin = 1d;
		
		for(int j=0; j<n1; j++) {
			if(Math.abs(v[j][i]) > TauUtil.DTOL) {
				corr = wResOrg.get(j).derivCorr(wRes);
				corrMax = Math.max(corrMax, corr);
				corrMin = Math.min(corrMin, corr);
			}
		}
		
		// For a one sided station distribution, the minimum and maximum can 
		// have the same sign.
		if(corrMax*corrMin >= 0d) {
			// If the maximum is bigger than the minimum, we're probably OK.
			if(Math.abs(corrMax) > Math.abs(corrMin)) {
				return true;
			} else {
				return false;
			}
		}
		
		// Otherwise, see if the azimuth needs to be flipped 180 degrees.
		if(corrMax > -corrMin) {
			// If the depth derivatives agree, we're probably OK.
			if(zSum*wRes.deriv[2] >= 0d) {
				return true;
			} else {
				// See if the correlations are small.
				if(Math.abs(corrMax+corrMin) < 0.05d) {
					// If so, the results are problematic.
					if(Math.abs(wRes.deriv[2]) > 1e-4d) return false;
					else return true;
				// If not, believe the correlation.
				} else {
					return true;
				}
			}
		// Otherwise, the sign is probably wrong.
		} else {
			// If the depth derivatives disagree, the sign is probably wrong.
			if(zSum*wRes.deriv[2] <= 0d) {
				return false;
			} else {
				// See if the correlations are small.
				if(Math.abs(corrMax+corrMin) < 0.05d) {
					// If so, the results are problematic.
					if(Math.abs(wRes.deriv[2]) > 1e-4d) return true;
					else return false;
				// If not, believe the correlation.
				} else {
					return false;
				}
			}
		}
	}
}
