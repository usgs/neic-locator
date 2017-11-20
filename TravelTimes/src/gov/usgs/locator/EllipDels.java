package gov.usgs.locator;

/**
 * Virtual array of ellipticity correction sample distances.  This 
 * is needed for the bilinear interpolation because the ellipticity 
 * tables are set up to go from the minimum to the maximum distance 
 * in implied 5 degree increments.
 * 
 * @author Ray Buland
 *
 */
public class EllipDels implements GenIndex {
	final double dDel = 5d;					// Ellipticity distance increment 
	double minDelta;								// Minimum distance in degrees
	double maxDelta;								// Maximum distance in degrees
	int maxInd;											// Maximum index
	
	/**
	 * Set up the distance range.
	 * 
	 * @param minDelta Minimum distance in degrees
	 * @param maxDelta Maximum distance in degrees
	 */
	public EllipDels(double minDelta, double maxDelta) {
		this.minDelta = minDelta;
		this.maxDelta = maxDelta;
		maxInd = (int)((maxDelta-minDelta)/dDel-0.5d);
	}
	
	@Override
	public int getIndex(double value) {
		// Get the virtual array index.
		return Math.min((int)Math.max((value-minDelta)/dDel, 0d), 
				maxInd);
	}
	
	@Override
	public double getValue(int index) {
		// Get the virtual array value.
		return minDelta+index*dDel;
	}
}