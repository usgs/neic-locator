package gov.usgs.locator;

/**
 * Virtual array of topographic sample longitudes.  It would have 
 * been nicer to have used the limits in the topography file 
 * header, but sadly they weren't accurate enough.  Note that 
 * the first and last points of the array have been duplicated 
 * to avoid the wrap around at +/-180 degrees.
 * 
 * @author Ray Buland
 *
 */
public class TopoLons implements GenIndex {
	final double minLon = -180.1666667d, maxLon = 180.1666667d, 
			dLon = 0.3333333d;
	final int maxInd = 1080;

	@Override
	public int getIndex(double value) {
		// Get the virtual array index.
		return Math.min((int)Math.max((value-minLon)/dLon, 0d), 
				maxInd);
	}

	@Override
	public double getValue(int index) {
		// Get the virtual array value.
		return minLon+index*dLon;
	}

}
