package gov.usgs.locator;

/**
 * Set diffracted phases programmatically.
 * 
 * @author Ray Buland
 *
 */
public class Diffracted {
	double mantleLim = Math.toRadians(132d);
	double coreLim = Math.toRadians(164d);
	String[] diffCode = {"P", "S", "pP", "sP", "pS", "sS", 
			"PKPab", "pPKPab", "sPKPab"};
	boolean[] diffUsed = {false, false, false, false, false, 
			false, false, false, false};
	String phDiff;
	double phLim;
	
	/**
	 * Given a phase code, determine if the phase should be diffracted.  
	 * Note that this determination depends on the order that the 
	 * phases are presented because there are multiple mantle P and S 
	 * branches, but only the deepest is diffracted.  For the same 
	 * reason, the up-going branch must not be presented to this 
	 * method.
	 * 
	 * @param phCode Branch phase code
	 * @return True if there is a diffracted branch
	 */
	public boolean isDiff(String phCode) {
		for(int j=0; j<diffCode.length; j++) {
			if(phCode.equals(diffCode[j])) {
				if(!diffUsed[j]) {
					diffUsed[j] = true;
					phDiff = TauUtil.phSeg(diffCode[j])+"diff";
					if(phCode.contains("ab")) phLim = coreLim;
					else phLim = mantleLim;
					return true;
				}
				return false;
			}
		}
		return false;
	}
	
	/**
	 * If there is a diffracted branch, get it's phase code.
	 * 
	 * @return The phase code of the associated diffracted branch.
	 */
	public String getPhDiff() {
		return phDiff;
	}
	
	/**
	 * If there is a diffracted branch, get it's maximum distance.
	 * 
	 * @return The maximum distance in radians that the diffracted 
	 * phase can be observed.
	 */
	public double getPhLim() {
		return phLim;
	}
}
