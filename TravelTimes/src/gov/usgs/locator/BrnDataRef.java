package gov.usgs.locator;

import java.util.Arrays;

/**
 * Store all non-volatile information associated with one travel-time 
 * branch.  Note that all data are normalized and transformed for internal 
 * use by the travel-time classes.
 * 
 * @author Ray Buland
 *
 */
public class BrnDataRef {
	final String phCode;					// Branch phase code
	final String phSeg;						// Generic phase code for all branches in this segment
	final String phDiff;					// Phase code of an associated diffracted phase
	final boolean isUpGoing;			// True if this is an up-going branch
	final boolean isDiff;					// True if this branch is also diffracted
	final boolean isUseless;			// True if this phase is always in the coda of another phase
	final char[] typeSeg;					// Phase type for correction, descending, ascending
	final int signSeg;						// Sign of the up-going correction
	final int countSeg;						// Number of mantle traversals
	final double[] pRange;				// Slowness range for this branch
	final double[] xRange;				// Distance range for this branch
	final double xDiff;						// Maximum distance of an associated diffracted phase
	final double[] pBrn;					// Slowness grid for this branch
	final double[] tauBrn;				// Tau for each grid point
//	final double[][] basisBrn;		// Basis function coefficients for each grid point
	double[][] basisBrn;		// Basis function coefficients for each grid point
	
	/**
	 * Load data from the FORTRAN file reader for one branch.  The file 
	 * data should have already been loaded from the *.hed and *.tbl files.
	 * 
	 * @param in Branch input data source.
	 * @param indexBrn FORTRAN branch index
	 * @param indexSeg FORTRAN segment index
	 * @param phCodeDiff Array of phase codes for diffracted branches
	 * @param phCodeUsed Array of use codes for diffracted branches
	 */
	public BrnDataRef(ReadTau in, int indexBrn, int indexSeg, String segCode, 
			Diffracted diff) {
		// Do phase codes.
		phCode = in.phCode[indexBrn];
		
		// Do segment summary information.
		phSeg = segCode;
		if(in.typeSeg[indexSeg][1] <= 0) isUpGoing = true;
		else isUpGoing = false;
		isUseless = TauUtil.setUseless(phCode);
		typeSeg = new char[3];
		for(int j=0; j<3; j++) {
			if(Math.abs(in.typeSeg[indexSeg][j]) == 1) typeSeg[j] = 'P';			// P in this region
			else if(Math.abs(in.typeSeg[indexSeg][j]) == 2) typeSeg[j] = 'S';	// S in this region
			else typeSeg[j] = ' ';
		}
		if(in.typeSeg[indexSeg][0] > 0) signSeg = 1;						// We need the sign of the correction
		else signSeg = -1;
		countSeg = (int) Math.round(in.countSeg[indexSeg][0]);	// We only need the up-going count
		
		// Do branch summary information.
		pRange = new double[2];
		xRange = new double[2];
		for(int j=0; j<2; j++) {
			pRange[j] = in.pBrn[indexBrn][j];
			xRange[j] = in.xBrn[indexBrn][j];
		}
		if(!isUpGoing) isDiff = diff.isDiff(phCode);
		else isDiff = false;
		if(isDiff) {
			phDiff = diff.getPhDiff();
			xDiff = diff.getPhLim();
		} else {
			phDiff = "";
			xDiff = 0d;
		}
		
		// Do branch specification.
//	int len = in.indexBrn[indexBrn][1]-in.indexBrn[indexBrn][0]+1;
//	int base = in.indexBrn[indexBrn][0]-1;
		int start = in.indexBrn[indexBrn][0]-1;
		int end = in.indexBrn[indexBrn][1];
/*	pBrn = new double[len];
		tauBrn = new double[len];
		basisBrn = new double[5][len];
		for(int j=0; j<len; j++) {
			pBrn[j] = in.pSpec[j+base];
			tauBrn[j] = in.tauSpec[j+base];
			for(int k=0; k<5; k++) {
				basisBrn[j][k] = in.basisSpec[k][j+base];
			}
		} */
		pBrn = Arrays.copyOfRange(in.pSpec, start, end);
		tauBrn = Arrays.copyOfRange(in.tauSpec, start, end);
		basisBrn = new double[5][];
		for(int k=0; k<5; k++) {
			basisBrn[k] = Arrays.copyOfRange(in.basisSpec[k], start, end);
		}
	}
	
	/**
	 * Test code for basisSpline.
	 */
	protected void reCompute() {
		Spline spl;
		
		for(int i=0; i<5; i++) {
			for(int j=0; j<basisBrn[i].length; j++) {
				basisBrn[i][j] = Double.NaN;
			}
		}
		spl = new Spline();
		spl.basisSet(pBrn, basisBrn);
	}
	
	/**
	 * get the branch segment code.
	 * 
	 * @return Branch segment code
	 */
	public String getPhSeg() {
		return phSeg;
	}
	
	/**
	 * Print out branch information for debugging purposes.
	 * 
	 * @param full If true print the detailed branch specification as well
	 */
	public void dumpBrn(boolean full) {
		if(isUpGoing) {
			if(isDiff) System.out.format("\n          phase = %sup  diff = %s  "+
					"isUseless = %b\n", phCode, phDiff, isUseless);
			else System.out.format("\n          phase = %sup  isUseless = %b\n", 
					phCode, isUseless);
			System.out.format("Segment: code = %s  type = %c        sign = %2d"+
					"  count = %d\n", phSeg, typeSeg[0], signSeg, countSeg);
		} else {
			if(isDiff) System.out.format("\n          phase = %s  diff = %s  "+
					"isUseless = %b\n", phCode, phDiff, isUseless);
			else System.out.format("\n          phase = %s  isUseless = %b\n", 
					phCode, isUseless);
			System.out.format("Segment: code = %s  type = %c, %c, %c  "+
					"sign = %2d  count = %d\n", phSeg, typeSeg[0], typeSeg[1], typeSeg[2], 
					signSeg, countSeg);
		}
		System.out.format("Branch: pRange = %8.6f - %8.6f  xRange = %6.2f - %6.2f\n", 
				pRange[0], pRange[1], Math.toDegrees(xRange[0]), Math.toDegrees(xRange[1]));
		if(isDiff) System.out.format("        phDiff = %s  xDiff = %6.2f\n", 
				phDiff, Math.toDegrees(xDiff));
		if(full) {
			System.out.println("\n         p        tau                 "+
					"basis function coefficients");
			for(int j=0; j<pBrn.length; j++) {
				System.out.format("%3d: %8.6f  %8.6f  %9.2e  %9.2e  %9.2e  %9.2e  "+
						"%9.2e\n", j, pBrn[j], tauBrn[j], basisBrn[0][j], basisBrn[1][j], 
						basisBrn[2][j], basisBrn[3][j], basisBrn[4][j]);
			}
		}
	}
}
