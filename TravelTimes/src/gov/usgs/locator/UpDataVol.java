package gov.usgs.locator;

import java.util.Arrays;

/**
 * Store volatile up-going branch data for one wave type.  Note that all 
 * data have been normalized.
 * 
 * @author Ray Buland
 *
 */
public class UpDataVol {
	double[] tauUp;				// Corrected up-going branch tau
	double tauEndUp;			// Tau integral from surface to LVZ for this wave type
	double tauEndLvz;			// Tau integral from LVZ to source for this wave type
	double tauEndCnv;			// Tau integral from surface to source for other wave type
	double xEndUp;				// Distance integral from surface to LVZ for this wave type
	double xEndLvz;				// Distance integral from LVZ to source for this wave type
	double xEndCnv;				// Distance integral from surface to source for other wave type
	UpDataRef ref;
	ModDataRef modPri, modSec;
	TauInt intPri, intSec;

	/**
	 * Set up volatile copies of data that changes with depth.  Note 
	 * that both P and S models are needed. If this is handling the 
	 * up-going data for P, the primary model would be for P and the 
	 * secondary model would be for S.
	 * 
	 * @param ref The up-going reference data source
	 * @param modPri The primary Earth model data source
	 * @param modSec the secondary Earth model data source
	 */
	public UpDataVol(UpDataRef ref, ModDataRef modPri, ModDataRef modSec) {
		this.ref = ref;
		this.modPri = modPri;
		this.modSec = modSec;
		
		// Set up the integration routines.
		intPri = new TauInt(modPri);
		intSec = new TauInt(modSec);
	}
		
	/**
	 * 
	 * @param zSource Normalized source depth
	 * @throws Exception If the source depth is too deep
	 */
	public void newDepth(double zSource) throws Exception {
		int iSrc, iBot;
		double pSource, pMax, zMax, xInt = 0d;
		
		// Get the source slowness.
		pSource = modPri.findP(zSource);
		iSrc = modPri.iSource;
		pMax = modPri.findMaxP();
		modPri.printFind(false);
		// Copy the desired data into temporary storage.
		tauUp = Arrays.copyOf(ref.tauUp[iSrc], 
				ref.tauUp[iSrc].length);
		
		// Correct the up-going values to the exact source depth.
		System.out.println("Integrate: "+(float)pSource+" - "+
				(float)modPri.pMod[iSrc]+"  "+(float)zSource+" - "+
				(float)modPri.zMod[iSrc]);
		for(int j=0; j<tauUp.length; j++) {
			if(ref.pTauUp[j] <= pMax) {
				System.out.println("i  p tau (before): "+(j+1)+" "+
						(float)ref.pTauUp[j]+" "+(float)tauUp[j]);
				tauUp[j] -= intPri.intLayer(ref.pTauUp[j], pSource, 
						modPri.pMod[iSrc], zSource, 
						modPri.zMod[iSrc], xInt);
				System.out.println("     tau (after): "+(float)tauUp[j]);
			} else break;
		}
		
		/**
		 * Compute tau and distance for the ray parameter equal to the 
		 * source slowness (i.e., horizontal take-off angle from the source).
		 */
		tauEndUp = intPri.intRange(pMax, 0, iSrc-1, pSource, 
				zSource, xEndUp);
		
		/**
		 * If the source depth is in a low velocity zone, we need to 
		 * compute tau and distance down to the shallowest turning ray 
		 * (the horizontal ray is trapped).
		 */
		if(pMax > pSource ) {
			zMax = modPri.findZ(pMax, false);
			iBot = modPri.iSource;
			tauEndLvz = intPri.intRange(pMax, iSrc, iBot, pSource, 
					zSource, pMax, zMax, xEndLvz);
		}
		
		/**
		 * Compute tau and distance for the other wave type for the ray 
		 * parameter equal to the source slowness.
		 */
		zMax = modSec.findZ(pMax, true);
		iBot = modSec.iSource;
		tauEndCnv = intSec.intRange(pMax, 0, iBot-1, pMax, zMax, xEndCnv);
	}
	
	/**
	 * Print out the up-going branch data corrected for the source depth.
	 * 
	 * @param full If true print the corrected tau array as well.
	 */
	public void dumpUp(boolean full) {
		System.out.println("\n     Up-going "+ref.typeUp+" corrected");
		System.out.format("TauEnd: %8.6f %8.6f %8.6f  XEnd: %8.6f %8.6f %8.6f\n", 
				tauEndUp, tauEndLvz, tauEndCnv, xEndUp, xEndLvz, xEndCnv);
		if(full) {
		System.out.println("          p        tau");
			for(int k=0; k<ref.tauUp.length; k++) {
				System.out.format("%3d  %8.6f  %8.6f\n",k,ref.pTauUp[k],tauUp[k]);
			}
		}
	}
}
