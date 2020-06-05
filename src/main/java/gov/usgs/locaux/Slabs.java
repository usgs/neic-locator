package gov.usgs.locaux;

import java.io.Serializable;
import java.util.ArrayList;

public class Slabs implements Serializable {
	private static final long serialVersionUID = 1L;
	ArrayList<SlabArea> slabs;
	
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
		
		lat0 = 90d - lat;
		if(lon < 0d) {
			lon0 = 360d + lon;
		} else {
			lon0 = lon;
		}
		System.out.format("Input: %7.3f %7.3f\n", lat0, lon0);
		for(SlabArea area : slabs) {
			if(area.isFound(lat0, lon0)) {
				System.out.println("\tGot one: " + area);
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
