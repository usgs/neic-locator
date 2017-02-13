package gov.usgs.locator;

import java.util.Arrays;

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
public class ModDataRef {
	char typeMod;								// Type of model ('P' or 'S')
	double[] zMod;							// Flat Earth depths
	double[] pMod;							// Slowness samples
	int[] indexUp;							// Index into the up-going branch data
	int iSource;								// Model index of the current source depth
	double pFound, zFound, pMax = Double.NaN;
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
	public ModDataRef(ReadTau in, ModConvert cvt, char typeMod) {
		this.typeMod = typeMod;
		this.cvt = cvt;
		
		if(typeMod == 'P') {
			zMod = Arrays.copyOf(in.zMod[0], in.zMod[0].length);
			pMod = Arrays.copyOf(in.pMod[0], in.pMod[0].length);
			indexUp = Arrays.copyOf(in.indexMod[0], in.indexMod[0].length);
			for(int j=0; j<indexUp.length; j++) {
				indexUp[j]--;
			}
		} else if(typeMod == 'S') {
			zMod = Arrays.copyOf(in.zMod[1], in.zMod[1].length);
			pMod = Arrays.copyOf(in.pMod[1], in.pMod[1].length);
			indexUp = Arrays.copyOf(in.indexMod[1], in.indexMod[1].length);
			indexUp[0] = -1;
			for(int j=indexUp.length-1; j>=1; j--) {
				indexUp[j] = indexUp[j]-indexUp[1];
			}
		} else {
			System.out.println("Unknown model type ("+typeMod+").");
		}
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
		for(iSource=0; iSource<indexUp.length; iSource++) {
			if(zMod[iSource] <= z) break;
		}
		// If we went off the end of the model, throw and exception.
		if(iSource >= indexUp.length) {
			System.out.println("findP: source depth is too deep");
			throw new Exception();
		}
		zFound = z;
		pMax = Double.NaN;
		// If we're on a grid point, return that.
		if(Math.abs(z-zMod[iSource]) <= TauUtil.dTol) pFound = pMod[iSource];
		// Otherwise interpolate to find the correct slowness.
		else pFound = pMod[iSource-1]+(pMod[iSource]-pMod[iSource-1])*
				(Math.exp(z-zMod[iSource-1])-1d)/
				(Math.exp(zMod[iSource]-zMod[iSource-1])-1d);
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
			if(p > pMod[0]) throw new Exception();
			for(iSource=0; iSource<indexUp.length; iSource++) {
				if(pMod[iSource] <= p) break;
			}
		} else {
			for(iSource=indexUp.length-1; iSource>=0; iSource--) {
				if(pMod[iSource] >= p) {
					if(Math.abs(pMod[iSource]-p) <= TauUtil.dTol) iSource++;
					break;
				}
			}
		}
		// If we went off the end of the model, throw and exception.
		if(iSource >= indexUp.length || iSource < 0) {
			System.out.println("findZ: source depth not found.");
			throw new Exception();
		}
		pFound = p;
		// If we're on a grid point, return that.
		if(Math.abs(p-pMod[iSource]) <= TauUtil.dTol) zFound = zMod[iSource];
		// Otherwise interpolate to find the correct slowness.
		else zFound = zMod[iSource-1]+Math.log(Math.max((p-pMod[iSource-1])*
				(Math.exp(zMod[iSource]-zMod[iSource-1])-1d)/
				(pMod[iSource]-pMod[iSource-1])+1d, TauUtil.dMin));
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
			pMax = Math.min(pMax, pMod[j]);
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
						"v = %4.1f\n", typeMod, iSource, cvt.realZ(zFound), 
						cvt.realV(pFound, zFound));
			} else {
				System.out.format("\nFind: type = %c  isource = %d  z = %5.1f  "+
						"v = %4.1f  vMax = %4.1f\n", typeMod, iSource, cvt.realZ(zFound), 
						cvt.realV(pFound, zFound), cvt.realV(pMax, zFound));
			}
		} else {
			if(pMax == Double.NaN) {
				System.out.format("\nFind: type = %c  isource = %d  z = %9.6f  "+
						"p = %8.6f\n", typeMod, iSource, zFound, pFound);
			} else {
				System.out.format("\nFind: type = %c  isource = %d  z = %9.6f  "+
						"p = %8.6f  pMax = %8.6f\n", typeMod, iSource, zFound, pFound, 
						pMax);
			}
		}
	}
	
	/**
	 * Print out model data for debugging purposes.
	 * 
	 * @param nice If true print in dimensional units.
	 */
	public void dumpMod(boolean nice) {
		System.out.println("\n     "+typeMod+" Model:");
		if(nice) {
			System.out.println("         Z      p   index");
			for(int j=0; j<indexUp.length; j++) {
				System.out.format("%3d: %6.1f  %5.2f  %3d\n",j,cvt.realZ(zMod[j]),
						cvt.realV(pMod[j],zMod[j]),indexUp[j]);
			}
			for(int j=indexUp.length; j<pMod.length-1; j++) {
				System.out.format("%3d: %6.1f  %5.2f\n",j,cvt.realZ(zMod[j]),
						cvt.realV(pMod[j],zMod[j]));
			}
			System.out.format("%3d: center  %5.2f\n",pMod.length-1,
					cvt.realV(pMod[pMod.length-1],zMod[zMod.length-1]));
		} else {
			System.out.println("          Z         p     index");
			for(int j=0; j<indexUp.length; j++) {
				System.out.format("%3d: %9.6f  %8.6f  %3d\n",
						j,zMod[j],pMod[j],indexUp[j]);
			}
			for(int j=indexUp.length; j<pMod.length-1; j++) {
				System.out.format("%3d: %9.6f  %8.6f\n",j,zMod[j],pMod[j]);
			}
			System.out.format("%3d: -infinity  %8.6f\n",pMod.length-1,
					pMod[pMod.length-1]);
		}
	}
}
