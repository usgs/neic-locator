package gov.usgs.locator;

import java.util.ArrayList;
import java.util.Arrays;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import gov.usgs.traveltime.TauUtil;

/**
 * After an event is located, the close out phase computes all 
 * the errors and heuristics used to evaluate the location.
 * 
 * @author Ray Buland
 *
 */
public class CloseOut {
	int n;
	double comp;
	Event event;
	Hypocenter hypo;
	ArrayList<Wresidual> wResRaw;
	ArrayList<Wresidual> wResProj;
	Restimator rEst;
	
	/**
	 * Remember the event.
	 * 
	 * @param event Event information
	 */
	public CloseOut(Event event) {
		this.event = event;
		hypo = event.hypo;
		wResRaw = event.wResRaw;
		wResProj = event.wResProj;
		rEst = event.rEstRaw;
	}

	/**
	 * Compute 90% marginal confidence intervals, the 90% error 
	 * ellipse or ellipsoid, and the pick data importances.  Note 
	 * that held locations are treated as though they were free 
	 * for the purposes of error evaluation.
	 * 
	 * @param status Event status coming into the close out
	 * @return Event status after the close out
	 */
	public LocStatus endStats(LocStatus status) {
		double perPt;
		double[] c;
		double[][] a;
		Matrix matrix, inverse;
		
		// Get the azimuthal gaps.
		event.azimuthGap();
		
		// If there isn't enough data, zero out all statistics.
		if(status == LocStatus.INSUFFICIENT_DATA) {
			event.zeroStats(true);
			event.zeroWeights();
			event.setQualFlags(status);
			return LocStatus.INSUFFICIENT_DATA;
		}
		
		// Get the residual spread.
		event.seResid = rEst.spread();
		
		// Force the number of degrees of freedom pretending everything 
		// was always free.
		n = 3;
		
		/* 
		 * Compensate for the effective number of data.  This is needed to 
		 * make the errors for events comparable whether they have been 
		 * decorrelated or not.
		 */
		if(event.cmndCorr) comp = 1d;
		else comp = Math.sqrt(LocUtil.EFFOFFSET-LocUtil.EFFSLOPE*
				Math.log10((double)(event.phUsed+1)));
		
		/*
		 * For the parameter errors, we need a "normal" matrix using the 
		 * demedianed derivatives of the projected data.
		 */
		// Initialize the "normal" matrix.
		a = new double[n][n];
		for(int i=0; i<n; i++) {
			for(int j=0; j<n; j++) {
				a[i][j] = 0d;
			}
		}
		// Construct the lower half of the "normal" matrix.
		if(LocUtil.deCorrelate) {
			// If we're decorrelating, use the projected data.
			for(int k=0; k<wResProj.size(); k++) {
				c = wResProj.get(k).getWdeDeriv(n);
				for(int i=0; i<n; i++) {
					for(int j=0; j<n; j++) {
						a[i][j] += c[i]*c[j];
					}
				}
			}
		} else {
			// Otherwise, use the raw pick data.
			for(int k=0; k<wResRaw.size(); k++) {
				c = wResRaw.get(k).getWdeDeriv(n);
				for(int i=0; i<n; i++) {
					for(int j=0; j<n; j++) {
						a[i][j] += c[i]*c[j];
					}
				}
			}
		}
		// Make the "normal" matrix symmetric.
		for(int i=0; i<n-1; i++) {
			for(int j=i+1; j<n; j++) {
				a[i][j] = a[j][i];
			}
		}
		if(LocUtil.deBugLevel > 1) LocUtil.printMatrix(a, "Projected Matrix");
		
		// Set up the matrix computations.
		matrix = new Matrix(a);
		try {
			// Compute the inverse (the correlation matrix).
			inverse = matrix.inverse();
			// Get the inverse matrix back to compute errors.
			a = inverse.getArray();
			if(LocUtil.deBugLevel > 1) LocUtil.printMatrix(a, "Correlation Matrix");
		} catch(RuntimeException e) {
			// Oops!  The matrix is singular.
			System.out.println("\n***** Projected normal matrix is singular!*****\n");
			event.zeroStats(false);
			event.zeroWeights();
			return LocStatus.SINGULAR_MATRIX;
		}
		// Do the marginal confidence intervals.
		perPt = LocUtil.PERPT1D/comp;
		event.seTime = perPt*event.seResid;
		event.seLat = perPt*Math.sqrt(Math.max(a[0][0], 0d));
		event.seLon = perPt*Math.sqrt(Math.max(a[1][1], 0d));
		event.seDepth = perPt*Math.sqrt(Math.max(a[2][2], 0d));
		
		try {
			// Do the error ellipsoid.
			errElp(inverse);
		} catch(RuntimeException e) {
			// Oops!  Something bad happened to the eigenvalue problem.
			System.out.println("\n***** Failure computing the error ellipsoid!*****\n");
			event.zeroStats(false);
			event.zeroWeights();
			return LocStatus.ELLIPSOID_FAILED;
		}
		
		/*
		 * For the data importances, we need the actual "normal" 
		 * matrix (i.e., using the derivatives of the original  
		 * pick data).
		 */
		// Initialize the "normal" matrix.
		a = new double[n][n];
		for(int i=0; i<n; i++) {
			for(int j=0; j<n; j++) {
				a[i][j] = 0d;
			}
		}
		// Construct the lower half of the "normal" matrix.
		for(int k=0; k<wResRaw.size(); k++) {
			c = wResRaw.get(k).getWderiv(n);
			for(int i=0; i<n; i++) {
				for(int j=0; j<n; j++) {
					a[i][j] += c[i]*c[j];
				}
			}
		}
		// Make the "normal" matrix symmetric.
		for(int i=0; i<n-1; i++) {
			for(int j=i+1; j<n; j++) {
				a[i][j] = a[j][i];
			}
		}
		if(LocUtil.deBugLevel > 1) LocUtil.printMatrix(a, "Normal Matrix");
		
		// Set up the matrix computations.
		matrix = new Matrix(a);
		try {
			// Compute the inverse (the correlation matrix).
			inverse = matrix.inverse();
			// Get the inverse matrix back to compute data importances.
			a = inverse.getArray();
			if(LocUtil.deBugLevel > 1) LocUtil.printMatrix(a, "Correlation Matrix");
		} catch(RuntimeException e) {
			// Oops!  The matrix is singular.
			System.out.println("\n***** Pick normal matrix is singular!*****\n");
			event.zeroStats(false);
			event.zeroWeights();
			return LocStatus.SINGULAR_MATRIX;
		}
		// Do the data importances.
		importance(a);
		
		// Set the quality flags.
		event.setQualFlags(status);
		return status;
	}
	
	/**
	 * Decompose the correlation matrix into eigenvalues and eigenvectors 
	 * in order to compute the error ellipse (epicenter) or ellipsoid 
	 * (hypocenter).  AveH, the radius of a circle with the same area as 
	 * the error ellipse is also computed as it may require a second 
	 * eigenvalue solution.
	 * 
	 * @param inverse Correlation matrix
	 */
	private void errElp(Matrix inverse) {
		double semiLen, azimuth, plunge, perPt, sgn;
		double[] e;
		double[][] u;
		EllipAxis[] ellip = event.errEllip;
		Matrix subInverse;
		EigenvalueDecomposition eigen;
		
		// Do the eigenvalue/vector decomposition.
		eigen = inverse.eig();
		// Get the eigenvalues.
		e = eigen.getRealEigenvalues();
		// Get the eigenvectors.
		u = eigen.getV().getArray();
		
		// Mash the eigenvalues/vectors into something more useful.
		if(n < 3) {
			// If the depth is held, do the error ellipse.
			perPt = LocUtil.PERPT2D/comp;
			for(int j=0; j<2; j++) {
				// Do the axis half length.
				semiLen = perPt*Math.sqrt(Math.max(e[j], 0d));
				// Do the azimuth.
				if(Math.abs(u[0][j])+Math.abs(u[1][j]) > TauUtil.DTOL) 
					azimuth = Math.toDegrees(Math.atan2(u[1][j], -u[0][j]));
				else azimuth = 0d;
				if(azimuth < 0d) azimuth += 360d;
				if(azimuth > 180d) azimuth -= 180d;
				ellip[j] = new EllipAxis(semiLen, azimuth, 0d);
			}
			ellip[2] = new EllipAxis(0d, 0d, 0d);
			// Do aveH (the equivalent radius of the error ellipse).
			event.aveH = LocUtil.PERPT1D*Math.sqrt(ellip[0].semiLen*
					ellip[1].semiLen)/LocUtil.PERPT2D;
		} else {
			
			// Otherwise, do the error ellipsoid.
			perPt = LocUtil.PERPT3D/comp;
			for(int j=0; j<3; j++) {
				// Do the axis half length.
				semiLen = perPt*Math.sqrt(Math.max(e[j], 0d));
				// Do the azimuth.
				sgn = Math.signum(u[2][j]);
				if(Math.abs(u[0][j])+Math.abs(u[1][j]) > TauUtil.DTOL) 
					azimuth = Math.toDegrees(Math.atan2(sgn*u[1][j], 
							-sgn*u[0][j]));
				else azimuth = 0d;
				if(azimuth < 0d) azimuth += 360d;
				if(Math.abs(u[2][j]) <= TauUtil.DTOL && azimuth > 180d) 
					azimuth -= 180d;
				// Do the plunge.
				plunge = Math.toDegrees(Math.asin(Math.min(sgn*u[2][j], 1d)));
				ellip[j] = new EllipAxis(semiLen, azimuth, plunge);
			}
			// Do aveH.  First, extract the error ellipse.
			subInverse = inverse.getMatrix(0, 1, 0, 1);
			// Do the eigenvalues again.
			eigen = subInverse.eig();
			e = eigen.getRealEigenvalues();
			// Finally, get the equivalent radius of the error ellipse.
			event.aveH = LocUtil.PERPT1D*Math.sqrt(Math.sqrt(Math.max(e[0]*e[1], 
					0d)))/comp;
		}
		// Sort the error ellipsoid axis by semiLen.
		Arrays.sort(ellip);
		// Do the summary errors, which also depend on the error ellipsoid.
		event.sumErrors();
	}
	
	/**
	 * Compute the data importances.
	 * 
	 * @param a The correlation matrix
	 */
	private void importance(double[][] a) {
		double sum, sumImport;
		double[] c, s;
		
		sumImport = 0d;
		s = new double[n];
		// The data importances are just the inner product of the derivative 
		// vector with the correlation matrix.
		for(int k=0; k<wResRaw.size(); k++) {
			if(!wResRaw.get(k).isDepth) {
				c = wResRaw.get(k).getWderiv(n);
				for(int i=0; i<n; i++) {
					s[i] = 0d;
					for(int j=0; j<n; j++) {
						s[i] += a[i][j]*c[j];
					}
				}
				sum = 0d;
				for(int j=0; j<n; j++) {
					sum += c[j]*s[j];
				}
				sumImport += sum;
				wResRaw.get(k).updateImport(sum);
			}
		}
		// Do the Bayesian depth data important separately.
		event.bayesImport = a[2][2]*Math.pow(hypo.depthWeight, 2d);
		if(LocUtil.deBugLevel >0) System.out.format("Normeq: qsum qsum+ "+
				"= %4.2f %4.2f\n", sumImport, sumImport+event.bayesImport);
	}
}
