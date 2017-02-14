package gov.usgs.locator;

/**
 * Utility methods for the travel-time package.
 * 
 * @author Ray Buland
 *
 */
public class TauUtil {
	/**
	 * Global tolerance value
	 */
	static final double DTOL = 1e-9d;
	/**
	 * Global minimum positive value
	 */
	static final double DMIN = 1e-30d;
	/**
	 * Global maximum positive value
	 */
	static final double DMAX = 1e30d;
	
	/**
	 * Create a segment code by stripping a phase code of unnecessary 
	 * frippery.
	 * 
	 * @param phCode Phase code
	 * @return Segment code
	 */
	public static String phSeg(String phCode) {
		int index;
		String phGen;
		
		if((index = phCode.indexOf("df")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("ab")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("ac")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("g")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("g")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		if((index = phCode.indexOf("b")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("b")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		if((index = phCode.indexOf("n")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("n")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		return phCode;
	}
	
	/**
	 * Algorithmically identify phases that are unlikely to be useful for 
	 * earthquake location.  These are crustal phases that always appear in 
	 * coda of the first arriving P or S phases.
	 * 
	 * @param phCode Phase code
	 * @return True if phase is in the crustal P or S coda
	 */
	public static boolean setUseless(String phCode) {
		if((phCode.contains("Pg") || phCode.contains("Pb") || phCode.contains("Sg") || 
				phCode.contains("Sb")) && phCode.length() >= 3) return true;
		else return false;
	}
}
