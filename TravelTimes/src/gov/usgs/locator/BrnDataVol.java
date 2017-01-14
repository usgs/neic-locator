package gov.usgs.locator;

import java.util.Arrays;

/**
 * Store all volatile information associated with one travel-time branch.  
 * 
 * @author Ray Buland
 *
 */
public class BrnDataVol {
	boolean isUsed;					// True if travel times should be computed
	double[] pRange;				// Slowness range for this branch
	double[] xRange;				// Distance range for this branch
	double pCaustic;				// Slowness of a caustic, if any
	double[] pBrn;					// Slowness grid for this branch
	double[] tauBrn;				// Tau for each grid point
	double[][] polyBrn;			// Interpolation polynomial for each grid interval
	BrnDataRef ref;
	
	/**
	 * Set up volatile copies of data that changes with depth
	 * 
	 * @param ref The branch reference data source
	 */
	public BrnDataVol(BrnDataRef ref) {
		this.ref = ref;
		// Do phase codes.
		polyBrn = new double[ref.pBrn.length][4];
		
		// Do branch summary information.
		isUsed = true;
		pRange = Arrays.copyOf(ref.pRange, ref.pRange.length);
		xRange = Arrays.copyOf(ref.xRange, ref.xRange.length);
		if(xRange[1] < xRange[0]) {
			xRange[0] = ref.xRange[1];
			xRange[1] = ref.xRange[0];
		}
		pCaustic = pRange[1];
		
		// Do branch specification.
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
	 * Print out branch information for debugging purposes.  Note that this 
	 * partly duplicates the print function in AllBrnRef, but includes 
	 * volatile data as well.
	 * 
	 * @param full If true print the detailed branch specification as well
	 */
	public void dumpBrn(boolean full) {
		System.out.format("\n          phase = %s  segment = %s\n", ref.phCode, 
				ref.phSeg);
		if(ref.isUpGoing) {
			System.out.format("Segment: type = %c        sign = %2d  count = %d  "+
					"isUpGoing = %b  isUsed = %b\n", ref.typeSeg[0], ref.signSeg, 
					ref.countSeg, ref.isUpGoing, isUsed);
		} else {
			System.out.format("Segment: type = %c, %c, %c  sign = %2d  count = %d  "+
					"isUpGoing = %b  isUsed = %b\n", ref.typeSeg[0], ref.typeSeg[1], 
					ref.typeSeg[2], ref.signSeg, ref.countSeg, ref.isUpGoing, isUsed);
		}
		System.out.format("Branch: pRange = %8.6f - %8.6f  xRange = %6.2f - %6.2f\n", 
				pRange[0], pRange[1], Math.toDegrees(xRange[0]), 
				Math.toDegrees(xRange[1]));
		if(ref.isDiff) System.out.format("        pCaustic = %8.6f  phDiff = %s  "+
				"xDiff = %6.2f\n", pCaustic, ref.phDiff, Math.toDegrees(ref.xDiff));
		else System.out.format("        pCaustic = %8.6f\n", pCaustic);
		if(full) {
			System.out.println("\n         p        tau                 "+
					"basis function coefficients");
			for(int j=0; j<pBrn.length; j++) {
				System.out.format("%3d: %8.6f  %8.6f  %9.2e  %9.2e  %9.2e  %9.2e\n", 
						j, pBrn[j], tauBrn[j], polyBrn[j][0], polyBrn[j][1], polyBrn[j][2], 
						polyBrn[j][3]);
			}
		}
	}
	
	public String toString() {
		if(ref.isUpGoing) {
			return String.format("%1sup      %7.4f %7.4f %7.2f %7.2f %7.4f"+
					"          %b", ref.phCode, pRange[0], pRange[1], 
					Math.toDegrees(xRange[0]), Math.toDegrees(xRange[1]), pCaustic, isUsed);
		}
		if(ref.isDiff) {
			return String.format("%-8s %7.4f %7.4f %7.2f %7.2f %7.4f %7.2f  %b", 
					ref.phCode, pRange[0], pRange[1], Math.toDegrees(xRange[0]), 
					Math.toDegrees(xRange[1]), pCaustic, Math.toDegrees(ref.xDiff), 
					isUsed);
		} else {
			return String.format("%-8s %7.4f %7.4f %7.2f %7.2f %7.4f          %b", 
					ref.phCode, pRange[0], pRange[1], Math.toDegrees(xRange[0]), 
					Math.toDegrees(xRange[1]), pCaustic, isUsed);
		}
	}
}
