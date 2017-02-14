package gov.usgs.locator;

/**
 * Umbrella storage for all volatile travel-time data.  
 * 
 * @author Ray Buland
 *
 */
public class AllBrnVol {	
	BrnDataVol[] branches;						// Volatile branch data
	ModDataVol pModel, sModel;				// Volatile model data
	UpDataVol pUp, sUp;								// Volatile up-going branch data
	double dSource;										// Dimensional source depth in kilometers
	double zSource;										// Flat Earth source depth
	double dTdDepth;									// Derivative of travel time with respect to depth
	AllBrnRef ref;
	ModConvert cvt;
	
	/**
	 * Set up volatile copies of data that changes with depth.
	 * 
	 * @param ref The reference data source
	 * @param cvt The Earth model units converter
	 */
	public AllBrnVol(AllBrnRef ref, ModConvert cvt) {
		this.ref = ref;
		this.cvt = cvt;
		
		// Set up the volatile piece of the model.
		pModel = new ModDataVol(ref.pModel, cvt);
		sModel = new ModDataVol(ref.sModel, cvt);
		// Set up the up-going branch data.
		pUp = new UpDataVol(ref.pUp, pModel, sModel, cvt);
		sUp = new UpDataVol(ref.sUp, sModel, pModel, cvt);
		
		// Set up the branch data.
		branches = new BrnDataVol[ref.branches.length];
		for(int j=0; j<branches.length; j++) {
			branches[j] = new BrnDataVol(ref.branches[j], pUp, sUp);
		}
	}
	
	/**
	 * Set up a new session.  Note that this just sets up the 
	 * simple session parameters of use to the travel-time package.
	 * 
	 * @param depth Source depth in kilometers
	 * @param phaseList Array of phase use commands
	 * @throws Exception 
	 */
	public void newSession(double depth, String[] phList) throws Exception {
		double xMin;
		
		// Set up the new source depth.
		dSource = Math.max(depth, 0.011d);
		// The interpolation gets squirrelly for very shallow sources.
		if(depth < 0.011d) {
			zSource = 0d;
			dTdDepth = 1d/cvt.pNorm;
		} else {
			zSource = Math.min(Math.log(Math.max(1d-dSource*cvt.xNorm, 
					TauUtil.DMIN)), 0d);
			dTdDepth = 1d/(cvt.pNorm*(1d-dSource*cvt.xNorm));
		}
		System.out.println("zSource = "+zSource+" dTdDepth = "+dTdDepth);
		
		// Fake up the phase list commands for now.
		for(int j=0; j<branches.length; j++) {
			branches[j].setIsUsed(true);
		}
		// If there are no commands, we're done.
//	if(phList == null) return;
		
		// Correct the up-going branch data.
		pUp.newDepth(zSource);
		sUp.newDepth(zSource);
		
		// Now correct each branch.
		xMin = cvt.xNorm*Math.min(Math.max(2d*dSource, 2d), 25d);
		for(int j=0; j<branches.length; j++) {
			branches[j].depthCorr(xMin);
		}
	}
		
	/**
	 * Print global or header data for debugging purposes.
	 */
	public void dumpHead() {
		System.out.println("\n     "+ref.modelName);
		System.out.format("Normalization: xNorm =%11.4e  pNorm =%11.4e  "+
				"tNorm =%11.4e vNorm =%11.4e\n", cvt.xNorm, cvt.pNorm, cvt.tNorm, 
				cvt.vNorm);
		System.out.format("Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  "+
				"zConrad =%7.1f\n", cvt.zUpperMantle, cvt.zMoho, cvt.zConrad);
		System.out.format("Derived: rSurface =%8.1f  zNewUp = %7.1f  "+
				"dTdDel2P =%11.4e  dTdDepth = %11.4e\n", cvt.rSurface, cvt.zNewUp, 
				cvt.dTdDelta, dTdDepth);
	}
	
	/**
	 * Print a summary table of branch information similar to the FORTRAN 
	 * Ttim range list.
	 */
	public void dumpTable() {
		System.out.println("");
		for(int j=0; j<branches.length; j++) {
			System.out.println(branches[j].toString());
		}
	}
	
	/**
	 * Print model parameters for debugging purposes.
	 * 
	 * @param typeMod Wave type ('P' or 'S')
	 * @param nice If true print the model in dimensional units
	 */
	public void dumpMod(char typeMod, boolean nice) {
		if(typeMod == 'P') {
			ref.pModel.dumpMod(nice);
		} else if(typeMod == 'S') {
			ref.sModel.dumpMod(nice);
		}
	}
	
	/**
	 * Print data for one travel-time branch for debugging purposes.  
	 * 
	 * @param iBrn Branch number
	 * @param full If true, print the detailed branch specification as well
	 */
	public void dumpBrn(int iBrn, boolean full, boolean sci) {
		branches[iBrn].dumpBrn(full, sci);
	}
	
	/**
	 * Print data for one travel-time segment for debugging purposes.
	 * 
	 * @param seg Segment phase code
	 * @param full If true, print the detailed specification for each branch
	 * as well
	 */
	public void dumpBrn(String seg, boolean full, boolean sci) {
		for(int j=0; j<branches.length; j++) {
			if(branches[j].getPhSeg().equals(seg)) 
				branches[j].dumpBrn(full, sci);
		}
	}
	
	/**
	 * Print data for all travel-time segments for debugging purposes.
	 * 
	 * @param full If true, print the detailed specification for each branch
	 * as well
	 */
	public void dumpBrn(boolean full, boolean sci) {
		for(int j=0; j<branches.length; j++) {
			branches[j].dumpBrn(full, sci);
		}
	}
	
	/**
	 * Print data for one up-going branch for debugging purposes.
	 * 
	 * @param typeUp Wave type ('P' or 'S')
	 * @param full If true print the up-going tau values as well
	 */
	public void dumpUp(char typeUp, boolean full) {
		if(typeUp == 'P') {
			pUp.dumpUp(full);
		} else if(typeUp == 'S') {
			sUp.dumpUp(full);
		}
	}
}
