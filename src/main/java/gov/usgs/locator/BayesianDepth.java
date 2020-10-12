package gov.usgs.locator;

public class BayesianDepth {
	double depth;					// Earthquake depth in km
	double spread;				// Standard deviation of the earthquake depth in km
	DepthSource source;		// Source of this Bayesian statistic
	
	/**
	 * @return Earthquake depth in km
	 */
	public double getDepth() {return depth;}
	
	/**
	 * @param depth Earthquake depth in km
	 */
	public void setDepth(double depth) {this.depth = depth;}
	
	/**
	 * @return Standard deviation of the earthquake depth in km
	 */
	public double getSpread() {return spread;}
	
	/**
	 * @param spread Standard deviation of the earthquake depth in km
	 */
	public void setSpread(double spread) {this.spread = spread;}
	
	/**
	 * @return Source of the Bayesian statistic
	 */
	public DepthSource getSource() {return source;}
	
	/**
	 * @param source New earthquake source
	 */
	public void setSource(DepthSource source) {this.source = source;}
	
	/**
	 * Construct this object with all the data it will ever need.
	 * 
	 * @param depth Earthquake depth in km
	 * @param spread Standard deviation of the earthquake depth in km
	 * @param source Source of the Bayesian statistic
	 */
	public BayesianDepth(double depth, double spread, DepthSource source) {
		this.depth = depth;
		this.spread = spread;
		this.source = source;
	}
	
	@Override
	public String toString() {
		return String.format("%5.1f +/- %5.1f %s", depth, spread, source);
	}
}