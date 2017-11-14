package gov.usgs.locator;

/**
 * Virtual array of ellipticity correction sample depths.  In this 
 * case the virtual array maps to a physical array.
 * 
 * @author Ray Buland
 *
 */
public class EllipDeps implements GenIndex {
	// Ellipticity depths in kilometers.
	final double[] ellipDeps = {0d, 100d, 200d, 300d, 500d, 700d};	
	
	@Override
	public int getIndex(double value) {
		// Get the array index for this value.
		int indDep = 1;
		for(; indDep<ellipDeps.length; indDep++) {
			if(value <= ellipDeps[indDep]) break;
		}
		return Math.min(--indDep, ellipDeps.length-2);
	}
	
	@Override
	public double getValue(int index) {
		// Get the value for this index.
		return ellipDeps[index];
	}
}