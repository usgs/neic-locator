package gov.usgs.locator;

/**
 * Unit conversion methods for the travel-time package Earth model.
 * 
 * @author Ray Buland
 *
 */
public class ModConvert {
	double xNorm;						// Internal normalization constant for distance.
	double pNorm;						// Internal normalization constant for slowness.
	double tNorm;						// Internal normalization constant for time.
	double vNorm;						// Internal normalization constant for velocity.
	double dTdDelta;				// Convert dT/dDelta to dimensional units.
	double dTdDepth = Double.NaN;		// Convert dT/dDepth to dimensional units.
	double zUpperMantle;		// Depth of the upper mantle in kilometers.
	double zMoho;						// Depth of the Moho in kilometers.
	double zConrad;					// Depth of the Conrad discontinuity in kilometers.
	double rSurface;				// Radius of the free surface of the Earth in kilometers.
	double zNewUp;					// Up-going branch replacement depth.
	
	/**
	 * Given a normalized, Earth flattened depth, return the 
	 * dimensional radius.
	 * 
	 * @param z Normalized, Earth flattened depth
	 * @return Radius in kilometers
	 */
	public double realR(double z) {
		return Math.exp(z)/xNorm;
	}
	
	/**
	 * Given a normalized, Earth flattened depth, return the 
	 * dimensional depth.
	 * 
	 * @param z Normalized, Earth flattened depth
	 * @return Depth in kilometers
	 */
	public double realZ(double z) {
		return (1d-Math.exp(z))/xNorm;
	}

	/**
	 * Given the normalized slowness and depth, return the 
	 * dimensional velocity at that depth.
	 * 
	 * @param p Normalized slowness
	 * @param z Normalized, Earth flattened depth
	 * @return Velocity at that depth in kilometers/second
	 */
	public double realV(double p, double z) {
		return realR(z)/(tNorm*p);
	}
	
	/**
	 * Given a dimensional radius, return the normalized, Earth 
	 * flattened depth.
	 * 
	 * @param r Radius in kilometers
	 * @return Normalized, Earth flattened depth
	 */
	public double flatZ(double r) {
		return Math.log(xNorm*r);
	}
	
	/**
	 * Given the normalized velocity and radius, return the normalized 
	 * slowness.
	 * 
	 * @param v Velocity at radius r in kilometers/second
	 * @param r Radius in kilometers
	 * @return Normalized slowness
	 */
	public double flatP(double v, double r) {
		return vNorm*r/v;
	}
}
