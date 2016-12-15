package gov.usgs.locator;

public class TauUtil {
	
	/**
	 * Given a normalized, Earth flattened depth, return the 
	 * dimensional radius.
	 * 
	 * @param z Normalized, Earth flattened depth
	 * @return Radius in km
	 */
	static double realR(double z) {
		return Math.exp(z)/ReadTau.xn;
	}
	
	/**
	 * Given a normalized, Earth flattened depth, return the 
	 * dimensional depth.
	 * 
	 * @param z Normalized, Earth flattened depth
	 * @return Depth in km
	 */
	static double realZ(double z) {
		return (1d-Math.exp(z))/ReadTau.xn;
	}

	/**
	 * Given the normalized slowness and depth, return the 
	 * dimensional velocity at that depth.
	 * @param p Normalized slowness
	 * @param z Normalized, Earth flattened depth
	 * @return Velocity at that depth in km/s
	 */
	static double realV(double p, double z) {
		return ReadTau.tn*realZ(z)/p;
	}
}
