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
	static double dTol = 1-9d;
	/**
	 * Global minimum positive value
	 */
	static double dMin = 1-30d;
		
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
}
