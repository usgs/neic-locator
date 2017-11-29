package gov.usgs.locator;

/**
 * Holds one travel-time statistics linear interpolation segment 
 * for one statistics variable.
 * 
 * @author Ray Buland
 *
 */
public class TtStatSeg {
	double minDelta;						// Minimum distance in degrees
	double maxDelta;						// Maximum distance in degrees
	double slope;								// Slope of linear interpolation
	double offset;							// Offset of linear interpolation
	
	/**
	 * Create the linear segment.
	 * 
	 * @param minDelta Minimum distance in degrees
	 * @param maxDelta Maximum distance in degrees
	 * @param slope Slope of the linear fit
	 * @param offset Offset of the linear fit
	 */
	protected TtStatSeg(double minDelta, double maxDelta, 
			double slope, double offset) {
		this.minDelta = minDelta;
		this.maxDelta = maxDelta;
		this.slope = slope;
		this.offset = offset;
	}
	
	/**
	 * Interpolate the linear fit at one distance.
	 * 
	 * @param delta Distance in degrees where statistics are desired
	 * @return Interpolated parameter
	 */
	protected double interp(double delta) {
		if(delta >= minDelta && delta <= maxDelta) {
			return offset+delta*slope;
		} else return(Double.NaN);
	}
}