package gov.usgs.locator;

/**
 * A description of one axis of an error ellipse.
 * 
 * @author Ray Buland
 *
 */
public class EllipAxis implements Comparable<EllipAxis> {
	double semiLen;			// Length of the axis in kilometers
	double azimuth;			// Azimuth of the axis in degrees
	double plunge;			// Plunge of the axis in degrees
	
	/**
	 * Set the values of one axis of an error ellipse.
	 * 
	 * @param semiLen Standard error in kilometers
	 * @param azimuth Azimuth in degrees (clockwise from north)
	 * @param plunge Plunge in degrees (down from the horizontal)
	 */
	public EllipAxis(double semiLen, double azimuth, double plunge) {
		this.semiLen = semiLen;
		this.azimuth = azimuth;
		this.plunge = plunge;
	}
	
	/**
	 * Get the tangential (horizontal) projection of this axis.
	 * 
	 * @return The tangential projection of this axis in kilometers
	 */
	public double tangentialProj() {
		return semiLen*Math.cos(Math.toRadians(plunge));
	}
	
	/**
	 * Get the vertical (depth) projection of this axis.
	 * 
	 * @return The vertical projection of this axis in kilometers
	 */
	public double verticalProj() {
		return semiLen*Math.sin(Math.toRadians(plunge));
	}
	
	/**
	 * Provide a toString suitable for traditional printing.
	 */
	@Override
	public String toString() {
		return String.format("%6.1f %3.0f %3.0f", semiLen, azimuth, plunge);
	}

	/**
	 * Sort the axes into descending order of the half length.
	 */
	@Override
	public int compareTo(EllipAxis axis) {
		if(this.semiLen > axis.semiLen) return -1;
		else if(this.semiLen < axis.semiLen) return +1;
		else return 0;
	}
}
