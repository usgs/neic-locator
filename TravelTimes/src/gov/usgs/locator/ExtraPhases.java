package gov.usgs.locator;

/**
 * Provides a programmatic way of getting information about 
 * diffracted and add-on phases.
 * 
 * @author Ray Buland
 *
 */
public class ExtraPhases {
	// Set up diffracted phases.
	final double mantleLim = Math.toRadians(132d);
	final double coreLim = Math.toRadians(164d);
	final String[] diffCode = {"P", "S", "pP", "sP", "pS", "sS", 
			"PKPab", "pPKPab", "sPKPab"};
	final boolean[] diffUsed = {false, false, false, false, false, 
			false, false, false, false};
	// Set up add-on phases.
	final String[] baseCode = {"PKPdf", "pP", "Sn", "SKiKP", "SKPdf"};
	final String[] addOnCode = {"PKPpre", "pwP", "Lg", "LR", "LR"};
	final boolean[] addOnUsed = {false, false, false, false, false};
	String phDiff, phAddOn;
	double phLim;
	
	/**
	 * Given a phase code, determine if the phase should be diffracted.  
	 * Note that this determination depends on the order that the 
	 * phases are presented because there are multiple mantle P and S 
	 * branches, but only the deepest is diffracted.  For the same 
	 * reason, the up-going branch must not be presented to this 
	 * method.
	 * 
	 * @param phCode Phase code
	 * @return True if there is a diffracted branch
	 */
	public boolean hasDiff(String phCode) {
		for(int j=0; j<diffCode.length; j++) {
			if(phCode.equals(diffCode[j])) {
				if(!diffUsed[j]) {
					diffUsed[j] = true;
					phDiff = TauUtil.phSeg(diffCode[j])+"dif";
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
	
	/**
	 * Add-on phases are made up out of whole cloth when a base 
	 * phase is processed for arrivals.
	 * 
	 * @param phCode Phase code
	 * @param xMin Minimum distance for this phase in radians
	 * @return True if this phase code has an add-on associated 
	 * with it
	 */
	public boolean hasAddOn(String phCode, double xMin) {
		for(int j=0; j<baseCode.length; j++) {
			if(phCode.equals(baseCode[j])) {
				if(!addOnUsed[j]) {
					if(!baseCode[j].equals("Sn")) {
						// In the general case, we want the first branch.
						addOnUsed[j] = true;
						phAddOn = addOnCode[j];
						return true;
					} else {
						if(xMin < TauUtil.SNDELMIN) {
							// For Sn we want the second branch.
							addOnUsed[j] = true;
							phAddOn = addOnCode[j];
							return true;
						} else return false;
					}
				}
				return false;
			}
		}
		return false;
	}
	
	/**
	 * If there is an add-on get it's phase code.
	 * 
	 * @return Phase code of the associated add-on phase
	 */
	public String getPhAddOn() {
		return phAddOn;
	}
}
