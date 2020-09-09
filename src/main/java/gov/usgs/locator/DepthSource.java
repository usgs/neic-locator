package gov.usgs.locator;

/**
 * Keeping track of where the Bayesian data came from will be 
 * essential going forward.
 * 
 * @author Ray Buland
 *
 */
public enum DepthSource {
	/**
	 * Shallow events may be possible nearly everywhere on Earth and 
	 * are typically within 20 km of the surface.
	 */
	SHALLOW,
	/**
	 * For a shallow slab, events may be possible from the slab to the 
	 * surface due to cracking in the crust above the slab.
	 */
	SLABINTERFACE,
	/**
	 * For a zone near the base of the crust, events may be possible 
	 * from the deep zone to the surface due to cracking in the crust 
	 * above the slab.
	 */
	ZONEINTERFACE,
	/**
	 * Events deeper than the shallow range are typically derived from 
	 * the slab model.
	 */
	SLABMODEL,
	/**
	 * Deep events for which no slab has been modeled should be picked 
	 * up from earthquake statistics.
	 */
	ZONESTATS;
}
