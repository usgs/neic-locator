package gov.usgs.locator;

import java.util.Arrays;

/**
 * Store all non-volatile information associated with one travel-time 
 * branch.  Note that all data are normalized and transformed for internal 
 * use.
 * 
 * @author Ray Buland
 *
 */
public class BrnDataRef {
	final String phCode;					// Branch phase code
	final String phSeg;						// Generic phase code for all branches in this segment
	final String phDiff;					// Phase code of an associated diffracted phase
	final String phAddOn;					// Phase code of an associated add-on phase
	final boolean isUpGoing;			// True if this is an up-going branch
	final boolean hasDiff;				// True if this branch is also diffracted
	final boolean hasAddOn;				// True if this branch has an associated add-on phase
	final boolean isUseless;			// True if this phase is always in the coda of another phase
	final char[] typeSeg;					// Phase type for correction, descending, ascending
	final int signSeg;						// Sign of the up-going correction
	final int countSeg;						// Number of mantle traversals
	final double[] pRange;				// Slowness range for this branch
	final double[] xRange;				// Distance range for this branch
	final double xDiff;						// Maximum distance of an associated diffracted phase
	final double[] pBrn;					// Slowness grid for this branch
	final double[] tauBrn;				// Tau for each grid point
	final double[][] basis;				// Basis function coefficients for each grid point
	final String phGroup;					// Phase group
	final String auxGroup;				// Auxiliary phase group
	final boolean isRegional;			// True if this is a regional phase
	final boolean isDepth;				// True if this is a depth phase
	final boolean canUse;					// True if this phase can be used for earthquake location
	final boolean dis;						// True if disrespected (down weight) this phase
	final String phGroupAdd;			// Phase group for an add-on phase
	final String auxGroupAdd;			// Auxiliary group for an add-on phase
	final boolean canUseAdd;			// Use flag for an add-on phase
	final AuxTtRef auxtt;					// Auxiliary data
	final TtStat ttStat, bcStat, diffStat, addStat;		// We potentially need four different 
																// sets of statistics: base phase, bc branch, diffracted 
	                              // branch, and add-on branch
	
	/**
	 * Load data from the FORTRAN file reader for one branch.  The file 
	 * data should have already been loaded from the *.hed and *.tbl files.
	 * 
	 * @param in Branch input data source.
	 * @param indexBrn FORTRAN branch index
	 * @param indexSeg FORTRAN segment index
	 * @param segCode Segment code for this branch
	 * @param extra List of extra phases
	 * @param auxtt Auxiliary data source
	 */
	public BrnDataRef(ReadTau in, int indexBrn, int indexSeg, String segCode, 
			ExtraPhases extra, AuxTtRef auxtt) {
		
		// Do phase code.
		phCode = in.phCode[indexBrn];
		
		// Remember the auxiliary data and the branch statistics.
		this.auxtt = auxtt;
		ttStat = auxtt.findStats(phCode);
		// If this is an ab branch we also need the statistics for the bc 
		// branch (they're the same at this level).
		if(phCode.contains("ab")) {
			String tmpCode = TauUtil.phSeg(phCode)+"bc";
			bcStat = auxtt.findStats(tmpCode);
		} else bcStat = null;
		
		// Do segment summary information.
		phSeg = segCode;
		if(in.typeSeg[indexSeg][1] <= 0) isUpGoing = true;
		else isUpGoing = false;
		isUseless = TauUtil.setUseless(phCode, false);
		// The three types are: 1) initial, 2) down-going, and 3) up-coming.
		// For example, sP would be S, P, P, while ScP would be S, S, P.
		typeSeg = new char[3];
		if(!isUpGoing) {
			// For the normal case, set all three flags.
			for(int j=0; j<3; j++) {
				if(Math.abs(in.typeSeg[indexSeg][j]) == 1) typeSeg[j] = 'P';
				else if(Math.abs(in.typeSeg[indexSeg][j]) == 2) typeSeg[j] = 'S';
				else typeSeg[j] = ' ';
			}
		} else {
			// For up-going phases, it's convenient to make all three flags 
			// the same.
			if(Math.abs(in.typeSeg[indexSeg][0]) == 1) typeSeg[0] = 'P';
			else typeSeg[0] = 'S';
			typeSeg[1] = typeSeg[0];
			typeSeg[2] = typeSeg[0];
		}
		// We need to know whether to add or subtract the up-going correction.
		// For example, the up-going correction would be subtracted for P, but 
		// added for pP.
		if(in.typeSeg[indexSeg][0] > 0) signSeg = 1;
		else signSeg = -1;
		// We might need to add or subtract the up-going correction more than 
		// once.
		countSeg = (int) Math.round(in.countSeg[indexSeg][0]);
		
		// Do branch summary information.
		pRange = new double[2];
		xRange = new double[2];
		for(int j=0; j<2; j++) {
			pRange[j] = in.pBrn[indexBrn][j];
			xRange[j] = in.xBrn[indexBrn][j];
		}
		
		// Set up the parameters for any extra phases associated with this 
		// phase.
		if(!isUpGoing) {
			hasDiff = extra.hasDiff(phCode);
			hasAddOn = extra.hasAddOn(phCode, xRange[1]);
		}
		else {
			hasDiff = false;
			hasAddOn = false;
		}
		// Handle a diffracted branch.
		if(hasDiff) {
			phDiff = extra.getPhDiff();
			xDiff = extra.getPhLim();
			diffStat = auxtt.findStats(phDiff);
		} else {
			phDiff = "";
			xDiff = 0d;
			diffStat = null;
		}
		// Handle an add-on phase.
		if(hasAddOn) {
			phAddOn = extra.getPhAddOn();
			addStat = auxtt.findStats(phAddOn);
		} else {
			phAddOn = "";
			addStat = null;
		}

		// Set up the branch specification.
		int start = in.indexBrn[indexBrn][0]-1;
		int end = in.indexBrn[indexBrn][1];
		pBrn = Arrays.copyOfRange(in.pSpec, start, end);
		tauBrn = Arrays.copyOfRange(in.tauSpec, start, end);
		basis = new double[5][];
		for(int k=0; k<5; k++) {
			basis[k] = Arrays.copyOfRange(in.basisSpec[k], start, end);
		}
		
		// Add phase group and use flags.
		phGroup = auxtt.findGroup(phCode);
		auxGroup = auxtt.compGroup(phGroup);
		isRegional = auxtt.isRegional(phCode);
		isDepth = auxtt.isDepthPh(phCode);
		canUse = auxtt.useGroup(phGroup);
		dis = auxtt.isDisPh(phCode);
		// Some of this stuff is different for an add-on phase.
		if(hasAddOn) {
			phGroupAdd = auxtt.findGroup(phAddOn);
			auxGroupAdd = auxtt.compGroup(phGroupAdd);
			canUseAdd = auxtt.useGroup(phGroupAdd);
		} else {
			phGroupAdd = null;
			auxGroupAdd = null;
			canUseAdd = false;
		}
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
	 * Get the type of the phase arriving at the station.
	 * 
	 * @return 'P' or 'S' depending on the type of the phase 
	 * when it arrives at the station.
	 */
	public char getArrivalType() {
		if(isUpGoing) return typeSeg[0];
		else return typeSeg[2];
	}
	
	/**
	 * Print out branch information for debugging purposes.
	 * 
	 * @param full If true print the detailed branch specification as well
	 */
	public void dumpBrn(boolean full) {
		if(isUpGoing) {
			System.out.format("\n          phase = %s up  ", phCode);
			if(hasDiff) System.out.format("diff = %s  ", phDiff);
			if(hasAddOn) System.out.format("add-on = %s  ", phAddOn);
			System.out.format("isUseless = %b\n", isUseless);
			System.out.format("Segment: code = %s  type = %c        sign = %2d"+
					"  count = %d\n", phSeg, typeSeg[0], signSeg, countSeg);
		} else {
			System.out.format("\n          phase = %s  ", phCode);
			if(hasDiff) System.out.format("diff = %s  ", phDiff);
			if(hasAddOn) System.out.format("add-on = %s  ", phAddOn);
			System.out.format("isUseless = %b\n", isUseless);
			System.out.format("Segment: code = %s  type = %c, %c, %c  "+
					"sign = %2d  count = %d\n", phSeg, typeSeg[0], typeSeg[1], typeSeg[2], 
					signSeg, countSeg);
		}
		System.out.format("Branch: pRange = %8.6f - %8.6f  xRange = %6.2f - %6.2f\n", 
				pRange[0], pRange[1], Math.toDegrees(xRange[0]), Math.toDegrees(xRange[1]));
		if(hasDiff) System.out.format("        xDiff = %6.2f\n", Math.toDegrees(xDiff));
		System.out.format("Flags: group = %s %s  flags = %b %b %b %b\n", phGroup, 
				auxGroup, isRegional, isDepth, canUse, dis);
		if(full) {
			System.out.println("\n         p        tau                 "+
					"basis function coefficients");
			for(int j=0; j<pBrn.length; j++) {
				System.out.format("%3d: %8.6f  %8.6f  %9.2e  %9.2e  %9.2e  %9.2e  "+
						"%9.2e\n", j, pBrn[j], tauBrn[j], basis[0][j], basis[1][j], 
						basis[2][j], basis[3][j], basis[4][j]);
			}
		}
	}
}
