package gov.usgs.locator;

/**
 * Virtual array of topographic sample latitudes.  It would have 
 * been nicer to have used the limits in the topography file 
 * header, but sadly they weren't accurate enough.  Note that 
 * instead of interpolating, extrapolation will be needed at 
 * the poles.  Also, note that latitude is stored from the 
 * north pole to the south pole making interpolation a little 
 * odd.
 * 
 * @author Ray Buland
 *
 */
public class TopoLats implements GenIndex {
	final double minLat = -89.8333333d, maxLat = 89.8333333d, 
			dLat = 0.3333333d;
	final int maxInd = 538;
	
	@Override
	public int getIndex(double value) {
		// Get the virtual array index.
		return Math.min((int)Math.max((-value-minLat)/dLat, 0d), 
				maxInd);
	}

	@Override
	public double getValue(int index) {
		// Get the virtual array value.
		return maxLat-index*dLat;
	}

}
