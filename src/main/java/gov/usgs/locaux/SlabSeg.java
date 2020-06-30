package gov.usgs.locaux;

import java.io.Serializable;
import java.util.List;

/**
 * Store slab depth points in one contiguous longitude segment at one 
 * latitude.  Note that this class is intended to be built and 
 * maintained by SlabRow.
 * 
 * @author Ray Buland
 *
 */
public class SlabSeg implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * Increment in the latitude grid.
	 */
	public final static double latIncrement = 0.5d;
	/**
	 * Increment in the longitude grid.
	 */
	public final static double lonIncrement = 0.5d;
	double[] lonRange;
	SlabDepth[] depths;
	
	/**
	 * Create the segment from a list of points.
	 * 
	 * @param points
	 */
	public SlabSeg(List<SlabPoint> points) {
		lonRange = new double[2];
		lonRange[0] = points.get(0).getLon();
		lonRange[1] = points.get(points.size() - 1).getLon();
		depths = new SlabDepth[points.size()];
		for(int j = 0; j < points.size(); j++) {
			depths[j] = points.get(j).getDep();
		}
	}
	
	/**
	 * Test to see if the desired position is in this segment.  Note 
	 * that the latitude has already been tested in the parent row.
	 * 
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @return True if the point is within in this segment
	 */
	public boolean isFound(double lon) {
		if(lon >= lonRange[0] && lon <= lonRange[1]) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the slab depth triplet at a point.  Note that the longitude 
	 * here is not the desired longitude, but the longitude truncated 
	 * to lie on the latitude-longitude grid.  This solves an ambiguity 
	 * that arises when the desired point is on the grid.
	 * 
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @return Slab depth triplet
	 */
	public SlabDepth getDepth(double lon) {
		int j = (int) Math.round((lon - lonRange[0]) / lonIncrement);
		System.out.format("Seg:  %6.2f %2d %s\n", lon, j, toString());
		
		if(j < 0) {
			System.out.println("Oops!");
		}
		if(j < depths.length) {
			return depths[j];
		} else {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%6.2f-%6.2f: %6.2f-%6.2f", 
				lonRange[0], lonRange[1], depths[0].getEqDepth(), 
				depths[depths.length - 1].getEqDepth());
	}
}
