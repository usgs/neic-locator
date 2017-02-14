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
	final char typeMod;								// Type of model ('P' or 'S')
	final double[] zMod;							// Flat Earth depths
	final double[] pMod;							// Slowness samples
	final int[] indexUp;							// Index into the up-going branch data
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
		} else {
			zMod = Arrays.copyOf(in.zMod[1], in.zMod[1].length);
			pMod = Arrays.copyOf(in.pMod[1], in.pMod[1].length);
			indexUp = Arrays.copyOf(in.indexMod[1], in.indexMod[1].length);
			indexUp[0] = -1;
			for(int j=indexUp.length-1; j>=1; j--) {
				indexUp[j] = indexUp[j]-indexUp[1];
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
