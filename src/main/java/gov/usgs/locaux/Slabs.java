package gov.usgs.locaux;

import java.io.Serializable;
import java.util.ArrayList;

import gov.usgs.traveltime.TauUtil;

/**
 * Provides the primary interface to all the slab data.
 * 
 * @author Ray Buland
 *
 */
public class Slabs implements Serializable {
	private static final long serialVersionUID = 1L;
	ArrayList<SlabArea> slabs;
	/**
	 * For analyst input, the spread is interpreted as a 99th percentile.  
	 * Here the raw numbers are one sigma (68%).  To make the slab statistics 
	 * compatible, they must be multiplied by 3.
	 */
	double bayesSpread;
	
	/**
	 * @return the 99th percentile depth error in kilometers.
	 */
	public double getBayesSpread() {
		return bayesSpread;
	}
	
	/**
	 * Set up storage for the slab areas.
	 */
	public Slabs() {
		slabs = new ArrayList<SlabArea>();
	}
	
	/**
	 * Add a slab area.
	 * 
	 * @param slabArea Slab area
	 */
	public void add(SlabArea slabArea) {
		slabArea.fixGaps();
		slabs.add(slabArea);
	}
	
	/**
	 * Get a list of slab depths for a point.  Note that with overturned 
	 * slabs, etc. it is possible to sample at least two slabs for one 
	 * geographic point.
	 * 
	 * @param lat Geographic latitude in degrees
	 * @param lon Geographic longitude in degrees
	 * @return List of slab depths
	 */
	public ArrayList<SlabDepth> getDepth(double lat, double lon) {
		double lat0, lon0;
		SlabDepth depth;
		ArrayList<SlabDepth> depths = null;
		
		// The slab lookup works in colatitude and longitude from 0 to 360 
		// degrees
		lat0 = 90d - lat;
		if(lon < 0d) {
			lon0 = 360d + lon;
		} else {
			lon0 = lon;
		}
		for(SlabArea area : slabs) {
			if(area.isFound(lat0, lon0)) {
				depth = area.getDepth(lat0, lon0);
				if(depth != null) {
					if(depths == null) {
						depths = new ArrayList<SlabDepth>();
					}
					depths.add(depth);
				}
			}
		}
		return depths;
	}
	
	/**
	 * Deciding which Bayesian depth to use is complicated.  There are four 
	 * possibilities: 1) there is no slab (use the shallow depth), 2) the slab 
	 * is close to the surface (use a combination of the slab and shallow depth), 
	 * 3) the trial depth is deep, so always use the slab depth, and 4) use 
	 * the shallow or slab depth depending on which is closest to the trial 
	 * depth.
	 * 
	 * @param latitude Trial hypocenter geographic latitude in degrees
	 * @param longitude Trial hypocenter geographic longitude in degrees
	 * @param depth Trial hypocenter depth in kilometers
	 * @return Bayesian depth in kilometers
	 */
	public double getBayesDepth(double latitude, double longitude, double depth) {
		double slabDiff = TauUtil.DMAX;
		SlabDepth slabDepth = null;
		ArrayList<SlabDepth> depths;
		
		depths = getDepth(latitude, longitude);
/*	if(LOGGER.getLevel() == Level.FINE) {
			LOGGER.fine("Bayesian depths:");
			LOGGER.fine(String.format("\t%6.2f < %6.2f < %6.2f\n", 
					Math.max(LocUtil.DEFAULTDEPTH - LocUtil.DEFAULTDEPTHSE, 0d), 
					LocUtil.DEFAULTDEPTH, LocUtil.DEFAULTDEPTH + LocUtil.DEFAULTDEPTHSE));
		} */
		if(depths == null) {
			// No slab.  Set up an upper crust constraint.
			bayesSpread = LocUtil.DEFAULTDEPTHSE;
			return LocUtil.DEFAULTDEPTH;
		} else {
			// We have a slab.  See which slab segment is the closest.
			for(SlabDepth slab : depths) {
				if(Math.abs(slab.getEqDepth() - depth) < slabDiff) {
					slabDepth = slab;
					slabDiff = Math.abs(slab.getEqDepth() - depth);
				}
			}
			if(slabDepth.getEqDepth() <= LocUtil.SLABMERGEDEPTH) {
				/**
				 * The slab is shallow and there may be vertical faults from the slab to the 
				 * surface, so allow the depth to be anywhere between the deepest slab error 
				 * and the free surface.
				 */
				double deepest = slabDepth.getEqDepth() + 3d * (slabDepth.getUpper() - 
						slabDepth.getEqDepth());
				bayesSpread = 0.5d * deepest;
				return bayesSpread;
			} else if(depth > LocUtil.SLABMAXSHALLOWDEPTH) {
				// If the current trial depth is deep, always use the slab depth.
				bayesSpread = 3d * Math.max(slabDepth.getEqDepth() - slabDepth.getLower(), 
						slabDepth.getUpper() - slabDepth.getEqDepth());
				return slabDepth.getEqDepth();
			} else if(Math.abs(depth - LocUtil.DEFAULTDEPTH) < 
					Math.abs(depth - slabDepth.getEqDepth())) {
				// The trial depth is closer to the upper crust.
				bayesSpread = LocUtil.DEFAULTDEPTHSE;
				return LocUtil.DEFAULTDEPTH;
			} else {
				// The trial depth is closer to the slab depth.
				bayesSpread = 3d * Math.max(slabDepth.getEqDepth() - slabDepth.getLower(), 
						slabDepth.getUpper() - slabDepth.getEqDepth());
				return slabDepth.getEqDepth();
			}
		}
	}
	
	/**
	 * Print a summary of all slab areas.
	 * 
	 * @param full If true, print summaries of rows and segments as well
	 */
	public void printAllAreas(boolean full) {
		for(SlabArea area : slabs) {
			area.printArea(full);
		}
	}
}
