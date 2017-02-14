package gov.usgs.locator;

import java.util.Arrays;

/**
 * Decimation methods used at various places in the travel-time package 
 * are implemented here.  One integrated slowness grid for P and S is 
 * constructed and all other branches are derived by combining P and S.  
 * The resulting slowness sampling often suffers interpolation instability 
 * due to inhomogeneity in the grid.  In practice, the best results are 
 * obtained by decimating the slowness grid such that the sampling in distance 
 * is reasonably uniform.
 * 
 * @author Ray Buland
 *
 */
public class Decimate {
	boolean isDec;			// true if the array has changed
	double[] pDec;			// Decimated ray parameter grid
	double[] tauDec;		// Decimated tau values

	/**
	 * The "fast" method was used in the FORTRAN real-time travel-time 
	 * calculation for the up-going branches only to save time.  The 
	 * "slow", but more precise decimation was only used in the front end 
	 * table generation.  Although, the slower method is recommended for 
	 * all uses in the Java version, the fast version has been implemented 
	 * for debugging comparison purposes.
	 * 
	 * @param p Normalized ray parameter grid
	 * @param tau Normalized tau on the same grid
	 * @param xRange Normalized distance at the branch end points
	 * @param xMin Normalized minimum distance interval desired
	 * @return Decimated, normalized ray parameter grid
	 */
	public double[] decXFast(double[] p, double[] tau, double[] xRange, double xMin) {
		boolean[] keep;
		int n, iBeg, iEnd, len;
		double xCur, xLast, dx, dx2, sgn, rnd, xTarget, xLeast;
		
		// Scan the current sampling to see if it is already OK.
		xCur = xRange[1];
		for(int i=p.length-2; i>=0; i--) {
			xLast = xCur;
			xCur = calcX(p, tau, xRange, i);
			if(Math.abs(xCur-xLast) <= xMin) {
				
				// It's not OK.  Set up the flag array.
				keep = new boolean[p.length];
				for(int j=0; j<keep.length; j++) keep[j] = true;
				// Set up the decimation algorithm.
				if(Math.abs(xCur-xLast) <= 0.75d*xMin) {
					xCur = xLast;
					i++;
				}
				n = Math.max((int)(Math.abs(xCur-xRange[0])/xMin+0.8d), 1);
				dx = (xCur-xRange[0])/n;
				dx2 = Math.abs(dx/2d);
				if(dx >= 0d) {
					sgn = 1d;
					rnd = 1d;
				} else {
					sgn = -1d;
					rnd = 0d;
				}
				xTarget = xRange[0]+dx;
				iBeg = 1;
				iEnd = 0;
				xLeast = TauUtil.DMAX;
				
				// Scan the ray parameter grid looking for points to kill.
				for(int j=1; j<=i; j++) {
					xCur = calcX(p, tau, xRange, j);
					if(sgn*(xCur-xTarget) > dx2) {
						// This point looks OK.  See if we have points to kill.
						if(iEnd >= iBeg) {
							for(int k=iBeg; k<=iEnd; k++) keep[k] = false;
						}
						// Reset the kill pointers.
						iBeg = iEnd+2;
						iEnd = j-1;
						xLeast = TauUtil.DMAX;
						xTarget += (int)((xCur-xTarget-dx2)/dx+rnd)*dx;
					}
					// Look for the best points to kill.
					if(Math.abs(xCur-xTarget) < xLeast) {
						xLeast = Math.abs(xCur-xTarget);
						iEnd = j-1;
					}
				}
				// See if there's one more range to kill.
				if(iEnd >= iBeg) {
					for(int k=iBeg; k<=iEnd; k++) keep[k] = false;
				}
				// Do the decimation.
				len = 0;
				for(int k=0; k<keep.length; k++) if(keep[k]) len++;
				pDec = new double[len];
				tauDec = new double[len];
				for(int k=0, l=0; k<keep.length; k++) {
					if(keep[k]) {
						pDec[l] = p[k];
						tauDec[l++] = tau[k];
					}
				}
				isDec = true;
				return Arrays.copyOf(pDec, pDec.length);
			}
		}
		isDec = false;
		tauDec = Arrays.copyOf(tau, tau.length);
		return p;
	}
	
	/**
	 * Get the tau values corresponding to the decimated ray parameter 
	 * grid.
	 * 
	 * @return Decimated, normalized tau on the decimated ray parameter 
	 * grid
	 */
	public double[] getDecTau() {
		if(isDec) return Arrays.copyOf(tauDec, tauDec.length);
		else return tauDec;
	}
	
	/**
	 * Return distance as a function of tau (distance is minus the derivative 
	 * of tau).  The method uses a simple three point approximation of the 
	 * derivative except at the end points where distance is already known.
	 * 
	 * @param p Normalized ray parameter grid
	 * @param tau Normalized tau on the same grid
	 * @param xRange Normalized distance at the branch end points
	 * @param i Grid point where the derivative is required
	 * @return Distance corresponding to tau(p_i)
	 */
	public double calcX(double[] p, double[] tau, double[] xRange, int i) {
		double h1, h2, hh;
		
		if(i == 0) return xRange[0];
		else if(i == p.length-1) return xRange[1];
		else {
			h1 = p[i-1]-p[i];
			h2 = p[i+1]-p[i];
			hh = h1*h2*(p[i-1]-p[i+1]);
			return (Math.pow(h2, 2d)*tau[i-1]+(h2+h1)*tau[i]-Math.pow(h1, 2d)*tau[i+1])/hh;
		}
	}
}
