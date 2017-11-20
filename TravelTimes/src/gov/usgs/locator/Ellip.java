package gov.usgs.locator;

/**
 * Implements the Dziewonski & Gilbert ellipticity correction 
 * algorithm for one phase.
 * 
 * @author Ray Buland
 *
 */
public class Ellip {
	final String phCode;					// Phase code
	final double minDelta;				// Minimum distance in degrees
	final double maxDelta;				// Maximum distance in degrees
	final double[][] t0, t1, t2;	// Ellipticity correction constants
	EllipDels delValue;						// Virtual array of distance values
	EllipDeps depValue;						// Virtual array of depth values
	
	/**
	 * Save ellipticity correction data for one phase.
	 * 
	 * @param phCode Phase code
	 * @param minDelta Minimum distance in degrees
	 * @param maxDelta Maximum distance in degrees
	 * @param t0 Distance-depth array for parameter t0
	 * @param t1 Distance-depth array for parameter t1
	 * @param t2 Distance-depth array for parameter t2
	 */
	public Ellip(String phCode, double minDelta, double maxDelta, 
			double[][] t0, double[][] t1, double[][] t2) {
		this.phCode = phCode;
		this.minDelta = minDelta;
		this.maxDelta = maxDelta;
		this.t0 = t0;
		this.t1 = t1;
		this.t2 = t2;
		delValue = new EllipDels(minDelta, maxDelta);
		depValue = new EllipDeps();
	}
	
	/**
	 * Calculate the ellipticity correction.
	 * 
	 * @param sourceLat Source latitude in degrees
	 * @param depth Source depth in kilometers
	 * @param delta Source-receiver distance in degrees
	 * @param azim Azimuth of the receiver from the source in degrees
	 * @return Ellipticity correction in seconds
	 */
	public double getEllipCorr(double sourceLat, double depth, 
			double delta, double azim) {
		
		// Create a few parameters.
		double coLat = Math.toRadians(90d-sourceLat);
		double sc0 = 0.25d*(1d+3d*Math.cos(2d*coLat));
		double sc1 = Math.sqrt(3d)*Math.sin(2d*coLat)/2d;
		double sc2 = Math.sqrt(3d)*Math.pow(Math.sin(coLat),2d)/2d;
		// Interpolate the tau functions.
		double tau0 = TauUtil.biLinear(delta, depth, delValue, 
				depValue, t0);
		double tau1 = TauUtil.biLinear(delta, depth, delValue, 
				depValue, t1);
		double tau2 = TauUtil.biLinear(delta, depth, delValue, 
				depValue, t2);
		// Compute the correction.
		double ellipCorr = sc0*tau0+sc1*Math.cos(Math.toRadians(azim))*tau1+
				sc2*Math.cos(2d*Math.toRadians(azim))*tau2;
		return ellipCorr;
	}
}