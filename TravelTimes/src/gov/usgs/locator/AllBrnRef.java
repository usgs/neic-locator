package gov.usgs.locator;

/**
 * Umbrella storage for all non-volatile travel-time data.  
 * Currently, the data is loaded into appropriate classes 
 * from the FORTRAN-ish ReadTau class in the constructor.  
 * However, in the future, it will read files written by 
 * other Java classes.  Note that after the data is loaded, 
 * ReadTau and all it's storage may be sent to the garbage 
 * collector.
 * 
 * @author Ray Buland
 *
 */
public class AllBrnRef {
	String modelName;									// Earth model name
	ModDataRef pModel, sModel;				// Earth model data
	BrnDataRef[] branches;						// Surface focus branch data
	UpDataRef pUp, sUp;								// Up-going branch data
	ModConvert cvt;
	
	/**
	 * Load all data from TauRead into more convenient Java classes.
	 * 
	 * @param in The TauRead data source
	 * @param cvt The Earth model units converter
	 */
	public AllBrnRef(ReadTau in, ModConvert cvt) {
		String[] segCode;
		this.cvt = cvt;
		
		this.modelName = in.modelName;
		// Set up the normalization.
		cvt.xNorm = in.xNorm;
		cvt.pNorm = in.pNorm;
		cvt.tNorm = in.tNorm;
		// Compute a couple of useful constants.
		cvt.vNorm = cvt.xNorm*cvt.pNorm;
		cvt.dTdDelta = Math.toRadians(1d/(cvt.vNorm));
		cvt.rSurface = in.rSurface;
		// Compute some useful depths.
		cvt.zUpperMantle = cvt.rSurface-in.rUpperMantle;
		cvt.zMoho = cvt.rSurface-in.rMoho;
		cvt.zConrad = cvt.rSurface-in.rConrad;
		cvt.zNewUp = cvt.zMoho;
		
		// Set up the Earth model.
		pModel = new ModDataRef(in, cvt, 'P');
		sModel = new ModDataRef(in, cvt, 'S');
		
		// Set up the branch data.
		segCode = new String[in.numSeg];
		int i = -1;
		int endSeg = 0;
		// Loop over branches setting the segment codes.
		for(int j=0; j<in.numBrn; j++) {
			// Look for a new segment.
			if(in.indexBrn[j][0] > endSeg) {
				endSeg = in.indexSeg[++i][1];
			}
			// Load the branch data.
			segCode[i] = TauUtil.phSeg(in.phCode[j]);
		}
		
		branches = new BrnDataRef[in.numBrn];
		Diffracted diff = new Diffracted();
		i = -1;
		endSeg = 0;
		// Loop over branches setting them up.
		for(int j=0; j<in.numBrn; j++) {
			// Look for a new segment.
			if(in.indexBrn[j][0] > endSeg) {
				endSeg = in.indexSeg[++i][1];
			}
			// Load the branch data.
			branches[j] = new BrnDataRef(in, j, i, segCode[i], diff);
		}
		
		// Set up the up-going branch data.
		pUp = new UpDataRef(in, 'P');
		sUp = new UpDataRef(in, 'S');
	}
	
	/**
	 * Get the number of travel-time branches loaded.
	 * 
	 * @return Number of travel-time branches loaded.
	 */
	public int getNoBranches() {
		return branches.length;
	}
	
	/**
	 * Print global or header data for debugging purposes.
	 */
	public void dumpHead() {
		System.out.println("\n     "+modelName);
		System.out.format("Normalization: xNorm =%11.4e  pNorm =%11.4e  "+
				"tNorm =%11.4e vNorm =%11.4e\n", cvt.xNorm, cvt.pNorm, cvt.tNorm, 
				cvt.vNorm);
		System.out.format("Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  "+
				"zConrad =%7.1f\n", cvt.zUpperMantle, cvt.zMoho, cvt.zConrad);
		System.out.format("Derived: rSurface =%8.1f  zNewUp = %7.1f  "+
				"dTdDel2P =%11.4e\n", cvt.rSurface, cvt.zNewUp, cvt.dTdDelta);
	}
	
	/**
	 * Print model parameters for debugging purposes.
	 * 
	 * @param typeMod Wave type ('P' or 'S')
	 * @param nice If true print the model in dimensional units
	 */
	public void dumpMod(char typeMod, boolean nice) {
		if(typeMod == 'P') {
			pModel.dumpMod(nice);
		} else if(typeMod == 'S') {
			sModel.dumpMod(nice);
		}
	}
	
	/**
	 * Print data for one travel-time branch for debugging purposes.  
	 * 
	 * @param iBrn Branch number
	 * @param full If true print the detailed branch specification as well
	 */
	public void dumpBrn(int iBrn, boolean full) {
		branches[iBrn].dumpBrn(full);
	}
	
	/**
	 * Print data for one up-going branch for debugging purposes.
	 * 
	 * @param typeUp Wave type ('P' or 'S')
	 * @param iUp Depth index
	 */
	public void dumpUp(char typeUp, int iUp) {
		if(typeUp == 'P') {
			pUp.dumpUp(iUp);
		} else if(typeUp == 'S') {
			sUp.dumpUp(iUp);
		}
	}
}
