package gov.usgs.locator;

/**
 * A description of one axis of an error ellipse.
 * 
 * @author Ray Buland
 *
 */
public class EllipAxis {
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
	 * Provide a toString suitable for traditional printing.
	 */
	@Override
	public String toString() {
		return String.format("%6.1f %3.1f %3.0f", semiLen, azimuth, plunge);
	}
}
