package gov.usgs.locator;

import java.util.Arrays;

/**
 * A collection of spline interpolation routines needed for 
 * the computation of travel times.
 * 
 * @author Ray Buland
 *
 */
public class Spline {

	/**
	 * Construct custom spline interpolation basis functions.  
	 * These basis functions depend only on the ray parameter 
	 * grid.  This is a straight port of FORTRAN routine Tauspl.  
	 * If p has dimension N, basis must have dimension N X 5.
	 * 
	 * @param p Normalized ray parameter grid
	 * @param basis Array to receive basis function coefficients
	 */
	public void basisSet(double[] p, double[][] basis) {
		int i = 0;
		double[] dp, sqrtDp, dSqrtDp, dSqrt3Dp, dInvDp, d;
		double pEnd, ali, alr, b1h, b3h, bih, th0p, th2p, th3p, th2m;
		
		// Trap a one point series.
		if(p.length == 1) return;
		
		// Initialize scratch arrays.
		dp = new double[5];
		sqrtDp = new double[5];
		dSqrtDp = new double[4];
		dSqrt3Dp = new double[4];
		dInvDp = new double[4];
		d = new double[4];
		
		// Start the process.
		pEnd = p[p.length-1];
		dp[1] = pEnd-p[0]+3d*(p[1]-p[0]);
		sqrtDp[1] = Math.sqrt(Math.abs(dp[1]));
		for(int k=2; k<5; k++) {
			dp[k] = pEnd-p[0]+(4-k)*(p[k]-p[0]);
			sqrtDp[k] = Math.sqrt(Math.abs(dp[k]));
			dSqrtDp[k-1] = sqrtDp[k]-sqrtDp[k-1];
			dSqrt3Dp[k-1] = dp[k]*sqrtDp[k]-dp[k-1]*sqrtDp[k-1];
			dInvDp[k-1] = 1d/sqrtDp[k]-1d/sqrtDp[k-1];
		}
		
		// Main loop.
		if(p.length > 2) {
			// Loop over the ray parameter array.
			for(i=0; i<p.length-1; i++) {
				// Update the temporary variables.
				for(int k=1; k<5; k++) {
					dp[k-1] = dp[k];
					sqrtDp[k-1] = sqrtDp[k];
					if(k < 4) {
						dSqrtDp[k-1] = dSqrtDp[k];
						dSqrt3Dp[k-1] = dSqrt3Dp[k];
						dInvDp[k-1] = dInvDp[k];
					}
				}
				dp[4] = pEnd-p[i+1];
				sqrtDp[4] = Math.sqrt(Math.abs(dp[4]));
				dSqrtDp[3] = sqrtDp[4]-sqrtDp[3];
				dSqrt3Dp[3] = dp[4]*sqrtDp[4]-dp[3]*sqrtDp[3];
				dInvDp[3] = 1d/sqrtDp[4]-1d/sqrtDp[3];
				// Construct G;i-1.
				ali = 1d/(0.125d*dSqrt3Dp[0]-(0.75d*dSqrtDp[0]+
						0.375d*dInvDp[0]*dp[2])*dp[2]);
				alr = ali*(0.125d*dp[1]*sqrtDp[1]-(0.75d*sqrtDp[1]+
						0.375d*dp[2]/dp[1]-sqrtDp[2])*dp[2]);
				b1h = dSqrtDp[1]+alr*dSqrtDp[0];
				b3h = dSqrt3Dp[1]+alr*dSqrt3Dp[0];
				bih = dInvDp[1]+alr*dInvDp[0];
				th0p = dSqrtDp[0]*b3h-dSqrt3Dp[0]*b1h;
				th2p = dSqrtDp[2]*b3h-dSqrt3Dp[2]*b1h;
				th3p = dSqrtDp[3]*b3h-dSqrt3Dp[3]*b1h;
				th2m = dInvDp[2]*b3h-dSqrt3Dp[2]*bih;
				// The d;i's completely define G;i-1.
				d[3] = ali*((dInvDp[0]*b3h-dSqrt3Dp[0]*bih)*th2p-
						th2m*th0p)/((dInvDp[3]*b3h-dSqrt3Dp[3]*bih)*th2p-
						th2m*th3p);
				d[2] = (th0p*ali-th3p*d[3])/th2p;
				d[1] = (dSqrt3Dp[0]*ali-dSqrt3Dp[2]*d[2]-
						dSqrt3Dp[3]*d[3])/b3h;
				d[0] = alr*d[1]-ali;
				// Construct the contributions G;i-1(p;i-2) and 
				// G;i-1(p;i).  G;i-1(p;i-1) is normalized to unity.
				basis[0][i] = (0.125d*dp[4]*sqrtDp[4]-(0.75d*sqrtDp[4]+
						0.375d*dp[3]/dp[4]-sqrtDp[3])*dp[3])*d[3];
				if(i >= 2) basis[1][i-2] = (0.125d*dp[0]*dSqrtDp[0]-
						(0.75d*dSqrtDp[0]+0.375d*dp[1]/dp[0]-dSqrtDp[1])*
						dp[1])*d[0];
				// Construct the contributions -dG;i-1(p)/dp for p;i-2, 
				// p;i-1, and p;i.
				basis[2][i] = -0.75d*(sqrtDp[4]+dp[3]/dp[4]-
						2d*sqrtDp[3])*d[3];
				if(i >= 1) basis[3][i-1] = -0.75d*(sqrtDp[1]+dp[2]/dp[1]-
						2d*sqrtDp[2])*d[1]-(dSqrtDp[0]+dInvDp[0]*dp[2])*d[0];
				if(i >= 2) basis[4][i-2] = -0.75d*(sqrtDp[0]+dp[1]/dp[0]-
						2d*sqrtDp[1])*d[0];
			}
		}
		for(int j=0; j<4; j++) {
			for(int k=1; k<5; k++) {
				dp[k-1] = dp[k];
				sqrtDp[k-1] = sqrtDp[k];
				if(k < 4) {
					dSqrtDp[k-1] = dSqrtDp[k];
					dSqrt3Dp[k-1] = dSqrt3Dp[k];
					dInvDp[k-1] = dInvDp[k];
				}
			}
			dp[4] = 0d;
			sqrtDp[4] = 0d;
			// Construction of the d;i's is different for each case.  
			// In cases G;i, i=n-1,n,n+1, G;i is truncated at p;n to 
			// avoid patching across the singularity in the second 
			// derivative.
			i++;
			if(j < 3) {
				// For G;i, i=n-2,n-1,n, the condition dG;i(p)/dp|p;i = 0 
				// has been substituted for the second derivative 
				// continuity condition that can no longer be satisfied.
				alr = (sqrtDp[1]+dp[2]/dp[1]-2d*sqrtDp[2])/(dSqrtDp[0]+
						dInvDp[0]*dp[2]);
				d[1] = 1d/(0.125d*dp[1]*sqrtDp[1]-(0.75d*sqrtDp[1]+
						0.375d*dp[2]/dp[1]-sqrtDp[2])*dp[2]-(0.125d*
						dSqrt3Dp[0]-(0.75d*dSqrtDp[0]+0.375d*dInvDp[0]*
						dp[2])*dp[2])*alr);
				d[0] = alr*d[1];
				if(j == 1) {
					// For G;n-1 constrain G;n-1(p;n) to be .25.
					d[2] = (2d+dSqrt3Dp[1]*d[1]+dSqrt3Dp[0]*d[0])/
							(dp[2]*sqrtDp[2]);
				} else if(j == 0) {
					// No additional constraints are required for G;n-2.
					d[2] = -((dSqrt3Dp[1]-dSqrtDp[1]*dp[3])*d[1]+
							(dSqrt3Dp[0]-dSqrtDp[0]*dp[3])*d[0])/(dSqrt3Dp[2]-
							dSqrtDp[2]*dp[3]);
					d[3] = (dSqrt3Dp[2]*d[2]+dSqrtDp[1]*d[1]+
							dSqrt3Dp[0]*d[0])/(dp[3]*sqrtDp[3]);
				}
				// Construct the contributions G;i-1(p;i-2) and 
				// G;i-1(p;i).
				if(j <= 1) basis[0][i] = (0.125d*dp[2]*sqrtDp[2]-
						(0.75d*sqrtDp[2]+0.375d*dp[3]/dp[2]-
						sqrtDp[3])*dp[3])*d[2]-(0.125d*dSqrt3Dp[1]-
						(0.75d*dSqrtDp[1]+0.375d*dInvDp[1]*dp[3])*dp[3])*
						d[1]-(0.125d*dSqrt3Dp[0]-(0.75d*dSqrtDp[0]+
						0.375d*dInvDp[0]*dp[3])*dp[3])*d[0];
				if(i > 0) basis[1][i-2] = (0.125d*dp[0]*sqrtDp[0]-
						(0.75d*sqrtDp[0]+0.375d*dp[1]/dp[0]-
						sqrtDp[1])*dp[1])*d[0];
				// Construct the contributions -dG;i-1(p)/dp | p;i-2, 
				// p;i-1, and p;i.
				if(j <= 1) basis[2][i] = -0.75d*((sqrtDp[2]+dp[3]/dp[2]-
						2d*sqrtDp[3])*d[2]-(dSqrtDp[1]+dInvDp[1]*dp[3])*d[1]-
						(dSqrtDp[0]+dInvDp[0]*dp[3])*d[0]);
				if(j <= 2 || i >= 0) basis[3][i-1] = 0d;
				if(i > 0) basis[4][i-2] = -0.75d*(sqrtDp[0]+dp[1]/dp[0]-
						2d*sqrtDp[1])*d[0];
			}
		}
	}
	
	/**
	 * Use the custom spline basis functions to build an 
	 * interpolation for tau and distance.  Note that the 
	 * interpolation depends of tau at each ray parameter, but 
	 * only depends on distance at the end points.  This is a 
	 * port of FORTRAN routine Fitspl with bits of Splfit added.
	 * 
	 * @param p Normalized ray parameter grid
	 * @param tau Normalized tau at each ray parameter grid point
	 * @param xRange Normalized distance at each end of the ray 
	 * parameter grid
	 * @param basis Ray parameter grid basis functions computed 
	 * in method basisSet
	 * @param Array to receive the coefficients of a polynomial in 
	 * tau suitable for computing travel times
	 */
	public void tauSpline(double[] p, double[] tau, double[] xRange, 
			double[][] basis, double[][] poly) {
		int n;
		double alr, gn, pEnd, dp, dtau;
		double[] ap, b, dpe, sqrtDp, sqrt3Dp;
		double[][] a;
		
		n = tau.length;
		// The first column of the polynomial is just tau.
		poly[0] = tau;
		// Make sure we have a reasonable length branch.
		if(n == 1) {
			poly[1][1] = xRange[0];
			return;
		}
		// Set up the working arrays.
		b = Arrays.copyOf(tau, n);
		a = new double[2][];
		a[0] = Arrays.copyOf(basis[0], n);
		a[1] = Arrays.copyOf(basis[1], n);
		ap = new double[3];
		for(int j=0; j<3; j++) {
			ap[j] = basis[j+2][n-1];
		}
		
		// Arrays ap(*,1), a, and ap(*,2) comprise n+2 x n+2 penta-
		// diagonal symmetric matrix A.  Let x1, tau, and xn comprise 
		// corresponding n+2 vector b.  Then, A * g = b, may be solved 
		// for n+2 vector g such that interpolation I is given by 
		// I(p) = sum(i=0,n+1) g;i * G;i(p).
		
		// First, eliminate the lower triangular portion of A to form A'.
		alr = a[0][0]/basis[2][0];
		a[0][0] = 1d-basis[3][0]*alr;
		a[1][0] -= basis[4][0]*alr;
		b[0] -= xRange[0]*alr;
		
		for(int j=1; j<n; j++) {
			alr = a[0][j]/a[0][j-1];
			a[0][j] = 1d-a[1][j-1]*alr;
			b[j]-= b[j-1]*alr;
		}
		alr = ap[0]/a[0][n-2];
		ap[1] -= a[1][n-2]*alr;
		gn = xRange[1]-b[n-2]*alr;
		
		// Back solve the upper triangular portion of A' for 
		// coefficients g;i.
		alr = ap[1]/a[0][n-1];
		gn = (gn-b[n-1]*alr)/(ap[2]-a[1][n-1]*alr);
		b[n-1] = (b[n-1]-gn*a[1][n-1])/a[0][n-1];
		for(int j=n-2; j>=0; j++) {
			b[j] = (b[j]-b[j+1]*a[1][j])/a[0][j];
		}
		
		// Fill in the interpolated distances.
		poly[1][0] = xRange[0];
		for(int j=1; j<n-1; j++) {
			poly[1][j] = basis[2][j]*b[j-1]+basis[3][j]*b[j]+
					basis[4][j]*b[j+1];
			poly[1][n-1] = xRange[1];
		}
		
		// Fill the rest of the polynomial and rearrange to make it more 
		// convenient for finding travel times.
		pEnd = p[n-1];
		dpe = new double[2];
		sqrtDp = new double[2];
		sqrt3Dp = new double[2];
		dpe[1] = pEnd-p[0];
		sqrtDp[1] = Math.sqrt(dpe[1]);
		sqrt3Dp[1] = dpe[1]*sqrtDp[1];
		
		for(int j=1; j<n-1; j++) {
			dpe[0] = dpe[1];
			sqrtDp[0] = sqrtDp[1];
			sqrt3Dp[0] = sqrt3Dp[1];
			dpe[1] = pEnd-p[j];
			sqrtDp[1] = Math.sqrt(dpe[1]);
			sqrt3Dp[1] = dpe[1]*sqrtDp[1];
			dp = p[j-1]-p[j];
			dtau = poly[0][j]-poly[0][j-1];
			poly[3][j-1] = (2d*dtau-dp*(poly[1][j]+poly[1][j-1]))/
					(0.5d*(sqrt3Dp[1]-sqrt3Dp[0])-
					1.5d*sqrtDp[1]*sqrtDp[0]*(sqrtDp[1]-sqrtDp[0]));
			poly[2][j-1] = (dtau-dp*poly[1][j-1]-
					sqrt3Dp[1]+0.5d*sqrt3Dp[0]-1.5d*dpe[1]*
					sqrtDp[0])*poly[3][j-1]/Math.pow((p[j-1]-p[j]), 2d);
			poly[1][j-1] = (dtau-(Math.pow(dpe[1],2d)-Math.pow(dpe[0],2d))*
					poly[2][j-1]-(sqrt3Dp[1]-sqrt3Dp[0])*poly[3][j-1])/dp;
			poly[0][j-1] = poly[0][j-1]-sqrt3Dp[0]*poly[3][j-1]-
					dpe[0]*(dpe[0]*poly[2][j-1]+poly[1][j-1]);
		}
	}
}
