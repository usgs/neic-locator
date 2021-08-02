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
	 * are typically within 20 km of the surface.  This is the default 
	 * for all earthquakes.
	 */
	SHALLOW,
	/**
	 * Shallow earthquake depth from ZoneStats.
	 */
	ZONESHALLOW,
	/**
	 * Shallow earthquake depth from NewZoneStats.
	 */
	NEWZONESHALLOW,
	/**
	 * Shallow earthquake depth from the mixed Gaussian ZoneStats.
	 */
	MIXZONESHALLOW,
	/**
	 * For a shallow slab, events may be possible from the slab to the 
	 * surface due to cracking in the crust above the slab.
	 */
	SLABINTERFACE,
	/**
	 * For an old zone near the base of the crust, events may be possible 
	 * from the deep zone to the surface due to cracking in the crust 
	 * above the slab.
	 */
	ZONEINTERFACE,
	/**
	 * For a new zone near the base of the crust, events may be possible 
	 * from the deep zone to the surface due to cracking in the crust 
	 * above the slab.
	 */
	NEWZONEINTERFACE,
	/**
	 * For a mixed Gaussian zone near the base of the crust, events may 
	 * be possible from the deep zone to the surface due to cracking in 
	 * the crust above the slab.
	 */
	MIXZONEINTERFACE,
	/**
	 * Events deeper than the shallow range are typically derived from 
	 * the slab model.
	 */
	SLABMODEL,
	/**
	 * Deep events for which no slab has been modeled should be picked 
	 * up from old ZoneStats earthquake statistics.
	 */
	ZONESTATS,
	/**
	 * Deep events for which no slab has been modeled should be picked 
	 * up from new ZoneStats earthquake statistics.
	 */
	NEWZONESTATS,
	/**
	 * Deep events for which no slab has been modeled should be picked 
	 * up from the mixed Gaussian ZoneStats earthquake statistics.
	 */
	MIXZONESTATS;
}
