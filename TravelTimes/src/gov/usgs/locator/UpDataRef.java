package gov.usgs.locator;

import java.util.Arrays;

/**
 * Store non-volatile up-going branch data for one wave type.  Note that all 
 * data have been normalized.
 * 
 * @author Ray Buland
 *
 */
public class UpDataRef {
	char typeUp;							// Type of up-going branches
	double[] pTauUp;					// Slowness grid for this branch
	double[][] tauUp;					// Tau for up-going branches by depth
	double[] pXUp;						// Slownesses for branch end points
	double[][] xUp;						// Distances for branch ends by depth

	/**
	 * Load data from the FORTRAN file reader up-going branchs of one type.  
	 * The file data should have already been loaded from the *.hed and 
	 * *.tbl files.
	 * 
	 * @param in Branch input data source.
	 * @param typeUp Wave type ('P' or 'S')
	 */
	public UpDataRef(ReadTau in, char typeUp) {
		this.typeUp = typeUp;
		int i =-1, k;
		if(typeUp == 'P') {
			i = 0;
		} else if(typeUp == 'S') {
			i = 1;
		} else {
			System.out.println("Unknown up-going wave type ("+typeUp+").");
			return;
		}
		
		// Copy the slowness grids.
		pTauUp = Arrays.copyOf(in.pTauUp[i], in.pTauUp[i].length);
		pXUp = Arrays.copyOf(in.pXUp[i], in.pXUp[i].length);
		
		// The ray parameter for the up-going branches should be truncated 
		// at the source slowness, but are not due to the way FORTRAN arrays 
		// are dimensioned.
		tauUp = new double[in.numRec[i]][];
		xUp = new double[in.numRec[i]][];
		for(int j=0; j<in.numRec[i]; j++) {
			for(k=1; k<in.tauUp[i][j].length; k++) {
				if(in.tauUp[i][j][k] == 0d) {
					break;
				}
			}
			tauUp[j] = Arrays.copyOf(in.tauUp[i][j], k);
			for(k=1; k<in.xUp[i][j].length; k++) {
				if(in.xUp[i][j][k] == 0d) {
					break;
				}
			}
			xUp[j] = Arrays.copyOf(in.xUp[i][j], k);
		}
	}
	
	/**
	 * Print out the up-going branch data for one depth.
	 * 
	 * @param rec Depth record number
	 */
	public void dumpUp(int rec) {
		System.out.println("\n     Up-going "+typeUp+" record "+rec);
		System.out.println("          p        tau        p           X");
		for(int k=0; k<xUp[rec].length; k++) {
			System.out.format("%3d  %8.6f  %8.6f  %8.6f  %9.6f\n",k,
					pTauUp[k],tauUp[rec][k],pXUp[k],xUp[rec][k]);
		}
		for(int k=xUp[rec].length; k<tauUp[rec].length; k++) {
			System.out.format("%3d  %8.6f  %8.6f\n",k,pTauUp[k],tauUp[rec][k]);
		}
	}
}
