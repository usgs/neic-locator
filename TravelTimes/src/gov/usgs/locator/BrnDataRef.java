package gov.usgs.locator;

/**
 * Store all non-volatile information associated with one travel-time 
 * branch.  Note that all data are normalized and transformed for internal 
 * use by the travel-time classes.
 * 
 * @author Ray Buland
 *
 */
public class BrnDataRef {
	String phCode;					// Branch phase code
	String phSeg;						// Generic phase code for all branches in this segment
	String phDiff = "";			// Phase code of an associated diffracted phase
	boolean isUpGoing;			// True if this is an up-going branch
	boolean isDiff;					// True if this branch is also diffracted
	char[] typeSeg;					// Phase type for correction, descending, ascending
	int signSeg;						// Sign of the up-going correction
	int countSeg;						// Number of mantle traversals
	double[] pRange;				// Slowness range for this branch
	double[] xRange;				// Distance range for this branch
	double xDiff = 0d;			// Maximum distance of an associated diffracted phase
	double[] pBrn;					// Slowness grid for this branch
	double[] tauBrn;				// Tau for each grid point
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
		if(!isUpGoing) {
			isDiff = diff.isDiff(phCode);
			if(isDiff) {
				phDiff = diff.getPhDiff();
				xDiff = diff.getPhLim();
			}
		}
		
		// Do branch specification.
		int len = in.indexBrn[indexBrn][1]-in.indexBrn[indexBrn][0]+1;
		int base = in.indexBrn[indexBrn][0]-1;
		pBrn = new double[len];
		tauBrn = new double[len];
		basisBrn = new double[len][5];
		for(int j=0; j<len; j++) {
			pBrn[j] = in.pSpec[j+base];
			tauBrn[j] = in.tauSpec[j+base];
			for(int k=0; k<5; k++) {
				basisBrn[j][k] = in.basisSpec[j+base][k];
			}
		}
	}
	
	/**
	 * Print out branch information for debugging purposes.
	 * 
	 * @param full If true print the detailed branch specification as well
	 */
	public void dumpBrn(boolean full) {
		System.out.format("\n          phase = %s  segment = %s\n", phCode, phSeg);
		if(isUpGoing) {
			System.out.format("Segment: type = %c        sign = %2d  count = %d  "+
					"isUpGoing = %b\n", typeSeg[0], signSeg, countSeg, isUpGoing);
		} else {
			System.out.format("Segment: type = %c, %c, %c  sign = %2d  count = %d  "+
					"isUpGoing = %b\n", typeSeg[0], typeSeg[1], typeSeg[2], 
					signSeg, countSeg, isUpGoing);
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
						"%9.2e\n", j, pBrn[j], tauBrn[j], basisBrn[j][0], basisBrn[j][1], 
						basisBrn[j][2], basisBrn[j][3], basisBrn[j][4]);
			}
		}
	}
}
