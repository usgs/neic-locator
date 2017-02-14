package gov.usgs.locator;

/**
 * Store Earth model data for one wave type.  Note that the model is 
 * normalized and the depths have undergone a flat Earth transformation.  
 * Also, only the upper 800 kilometers of the Earth model are available 
 * here.  The reference version contains only information that is 
 * invariant.
 * 
 * @author Ray Buland
 *
 */
public class ModDataVol {
	int iSource;								// Model index of the current source depth
	double pFound, zFound, pMax = Double.NaN;
	ModDataRef ref;
	ModConvert cvt;

	/**
	 * Load data from the FORTRAN file reader for the Earth model for one 
	 * wave type.  The file data should have already been loaded from the 
	 * *.hed and *.tbl files.
	 * 
	 * @param in Branch input data source.
	 * @param cvt The Earth model units converter
	 * @param typeMod Wave type ('P' or 'S')
	 */
	public ModDataVol(ModDataRef ref, ModConvert cvt) {
		this.ref = ref;
		this.cvt = cvt;
	}
	
	/**
	 * Find the model slowness for a desired depth.
	 * 
	 * @param z Desired normalized, flattened depth
	 * @return Normalized slowness at the desired depth
	 * @throws Exception If the desired depth is too deep
	 */
	public double findP(double z) throws Exception {
		// Search the model to bracket the source depth.
		for(iSource=0; iSource<ref.indexUp.length; iSource++) {
			if(ref.zMod[iSource] <= z) break;
		}
		// If we went off the end of the model, throw and exception.
		if(iSource >= ref.indexUp.length) {
			System.out.println("findP: source depth is too deep");
			throw new Exception();
		}
		zFound = z;
		pMax = Double.NaN;
		// If we're on a grid point, return that.
		if(Math.abs(z-ref.zMod[iSource]) <= TauUtil.DTOL) pFound = ref.pMod[iSource];
		// Otherwise interpolate to find the correct slowness.
		else pFound = ref.pMod[iSource-1]+(ref.pMod[iSource]-ref.pMod[iSource-1])*
				(Math.exp(z-ref.zMod[iSource-1])-1d)/
				(Math.exp(ref.zMod[iSource]-ref.zMod[iSource-1])-1d);
		return pFound;
	}
	
	/**
	 * Find the model depth for a desired slowness.
	 * 
	 * @param p Desired normalized model slowness
	 * @param first If true, find the top of a low velocity zone, 
	 * if false, find the bottom
	 * @return Normalized depth at the desired slowness
	 * @throws Exception If the desired slowness is too small
	 */
	public double findZ(double p, boolean first) throws Exception {
		// Search the model to bracket the source depth.
		if(first) {
			if(p > ref.pMod[0]) throw new Exception();
			for(iSource=0; iSource<ref.indexUp.length; iSource++) {
				if(ref.pMod[iSource] <= p) break;
			}
		} else {
			for(iSource=ref.indexUp.length-1; iSource>=0; iSource--) {
				if(ref.pMod[iSource] >= p) {
					if(Math.abs(ref.pMod[iSource]-p) <= TauUtil.DTOL) iSource++;
					break;
				}
			}
		}
		// If we went off the end of the model, throw and exception.
		if(iSource >= ref.indexUp.length || iSource < 0) {
			System.out.println("findZ: source depth not found.");
			throw new Exception();
		}
		pFound = p;
		// If we're on a grid point, return that.
		if(Math.abs(p-ref.pMod[iSource]) <= TauUtil.DTOL) zFound = ref.zMod[iSource];
		// Otherwise interpolate to find the correct slowness.
		else zFound = ref.zMod[iSource-1]+Math.log(Math.max((p-ref.pMod[iSource-1])*
				(Math.exp(ref.zMod[iSource]-ref.zMod[iSource-1])-1d)/
				(ref.pMod[iSource]-ref.pMod[iSource-1])+1d, TauUtil.DMIN));
		return zFound;
	}
	
	/**
	 * Find the maximum slowness between the surface and the source.  If 
	 * the source is in a low velocity zone, this will be the slowness 
	 * at the top.  Otherwise, it will be the source slowness.  Note that 
	 * the parameters determined by the last call to findP is assumed.
	 * 
	 * @return The normalized maximum slowness above the source
	 */
	public double findMaxP() {
		pMax = pFound;
		for(int j=0; j<iSource; j++) {
			pMax = Math.min(pMax, ref.pMod[j]);
		}
		return pMax;
	}
	
	/**
	 * Print the result of the latest findP or findZ call.
	 */
	public void printFind(boolean nice) {
		if(nice) {
			if(pMax == Double.NaN) {
				System.out.format("\nFind: type = %c  isource = %d  z = %5.1f  "+
						"v = %4.1f\n", ref.typeMod, iSource, cvt.realZ(zFound), 
						cvt.realV(pFound, zFound));
			} else {
				System.out.format("\nFind: type = %c  isource = %d  z = %5.1f  "+
						"v = %4.1f  vMax = %4.1f\n", ref.typeMod, iSource, cvt.realZ(zFound), 
						cvt.realV(pFound, zFound), cvt.realV(pMax, zFound));
			}
		} else {
			if(pMax == Double.NaN) {
				System.out.format("\nFind: type = %c  isource = %d  z = %9.6f  "+
						"p = %8.6f\n", ref.typeMod, iSource, zFound, pFound);
			} else {
				System.out.format("\nFind: type = %c  isource = %d  z = %9.6f  "+
						"p = %8.6f  pMax = %8.6f\n", ref.typeMod, iSource, zFound, pFound, 
						pMax);
			}
		}
	}
}
