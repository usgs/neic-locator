package gov.usgs.locaux;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Store slab depth information for one latitude-longitude area.
 * 
 * @author Ray Buland
 *
 */
public class SlabArea implements Serializable {
	private static final long serialVersionUID = 1L;
	int rowFound = -1;
	double[] latRange;
	double[] lonRange;
	ArrayList<SlabRow> slabRows;
	
	/**
	 * Set up storage for the rows, segments, and depths.
	 */
	public SlabArea() {
		latRange = new double[2];
		lonRange = new double[2];
		latRange[0] = 90d;
		latRange[1] = -90d;
		lonRange[0] = 360d;
		lonRange[1] = 0d;
		slabRows = new ArrayList<SlabRow>();
	}
	
	/**
	 * Add a new slab latitude row.  After squeezing the row will 
	 * be comprised of one or more segments.
	 * 
	 * @param slabRow Row of slab depths
	 */
	public void add(SlabRow slabRow) {
		if(slabRow.getLonRange() != null) {
			latRange[0] = Math.min(latRange[0], slabRow.getLat());
			latRange[1] = Math.max(latRange[1], slabRow.getLat());
			lonRange[0] = Math.min(lonRange[0], slabRow.getLonRange()[0]);
			lonRange[1] = Math.max(lonRange[1], slabRow.getLonRange()[1]);
			slabRows.add(slabRow);
		}
	}
	
	/**
	 * Test to see if the desired position is within this area.
	 * 
	 * @param lat Geographic colatitude in degrees (0-180 degrees)
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @return True if the point falls in this area
	 */
	public boolean isFound(double lat, double lon) {
		if(lat >= latRange[0] && lat <= latRange[1] && lon >= lonRange[0] 
				&& lon <= lonRange[1]) {
			for(int j = 0; j < slabRows.size(); j++) {
				if(slabRows.get(j).isFound(lat, lon)) {
					rowFound = j;
					System.out.println("Area: row = " + j + " " + slabRows.get(j));
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the slab depth triplet at the desired point.  Note that the slab 
	 * areas have been split up so that no more than one depth value should 
	 * be possible from each area.
	 * 
	 * @param lat Geographic colatitude in degrees (0-180 degrees)
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @return Slab depth triplet
	 */
	public SlabDepth getDepth(double lat, double lon) {
		double[] v;
		double[][][] v0, v1;
		
		if(rowFound >= 0 && rowFound < slabRows.size()-1) {
			if(slabRows.get(rowFound + 1).getLat() - slabRows.get(rowFound).getLat() 
					<= SlabSeg.latIncrement) {
				// Get the 3-vectors for interpolation.
				v = Geometry.vector(lon, lat, Double.NaN);
				v0 = new double[2][][];
				v1 = new double[2][][];
				slabRows.get(rowFound++).getVectors(lon, v0);
				slabRows.get(rowFound).getVectors(lon, v1);
				rowFound = -1;
				return interp(v0, v1, v);
			}
		}
		rowFound = -1;
		return null;
	}
	
	/**
	 * Interpolate the slab depth triplet.  The input vector is a 4-vector of 
	 * the depth points surrounding the desired point.  For each point, there 
	 * is a 3-vector of for the triplet.  For each depth, there is a three 
	 * vector for the position.
	 * 
	 * @param v0 Vector describing the triplet for the longitude points 
	 * surrounding the desired point in the first row
	 * @param v1 Vector describing the triplet for the longitude points 
	 * surrounding the desired point in the second row
	 * @param v Position vector for the desired point
	 * @return Interpolated slab depth triplet
	 */
	private SlabDepth interp(double[][][] v0, double[][][] v1, double[] v) {
		int nulls = 0, lastNull = -1;
		double[] depths;
		
		// Survey the points to see if we have enough for the interpolation.
		for(int j = 0; j < 2; j++) {
			if(v0[j] == null) {
				nulls++;
				lastNull = j;
			System.out.println("v0[" + j + "]: null");
		} else {
			System.out.format("v0[%d]: (%7.3f, %7.3f, %7.4f\n", j, v0[j][1][0], 
					v0[j][1][1], v0[j][1][2]);
			}
	}
	for(int j = 0; j < 2; j++) {
			if(v1[j] == null) {
				nulls++;
				lastNull = j + 2;
			System.out.println("v1[" + j + "]: null");
		} else {
			System.out.format("v1[%d]: (%7.3f, %7.3f, %7.4f\n", j, v1[j][1][0], 
					v1[j][1][1], v1[j][1][2]);
			}
		}
	System.out.format("    v: (%7.3f, %7.3f, %7.4f\n", v[0], v[1], v[2]);
		
		if(nulls == 0) {
			// We can do bilinear interpolation.
			depths = new double[3];
			for(int j = 0; j < 3; j++) {
				depths[j] = Linear.threeD(v0[0][j], v0[1][j], v1[0][j], v1[1][j], v);
			}
			return new SlabDepth(depths);
		} else if(nulls == 1) {
			// We can handle an edge by doing linear interpolation on the three 
			// points we have.
			depths = new double[3];
			switch(lastNull) {
			case 0:
				// Triangle is in the -/- quadrant.
				for(int j = 0; j < 3; j++) {
					depths[j] = Linear.twoD(v1[1][j], v1[0][j], v0[1][j], v);
				}
				return new SlabDepth(depths);
			case 1:
				// Triangle is in the +/- quadrant.
				for(int j = 0; j < 3; j++) {
					depths[j] = Linear.twoD(v1[0][j], v1[1][j], v0[0][j], v);
				}
				return new SlabDepth(depths);
			case 2:
				// Triangle is in the -/+ quadrant.
				for(int j = 0; j < 3; j++) {
					depths[j] = Linear.twoD(v0[1][j], v0[0][j], v1[1][j], v);
				}
				return new SlabDepth(depths);
			case 3:
				// Triangle is in the +/+ quadrant.
				for(int j = 0; j < 3; j++) {
					depths[j] = Linear.twoD(v0[0][j], v0[1][j], v1[0][j], v);
				}
				return new SlabDepth(depths);
			default:
				System.out.println("How did lastNull get to be " + lastNull + "?");
			}
		}
		return null;
	}
	
	/**
	 * Print a summary of the slab area.
	 * 
	 * @param full If true, print row and segment summaries as well
	 */
	public void printArea(boolean full) {
		System.out.println("Area: " + toString());
		if(full) {
			for(SlabRow row : slabRows) {
				row.printRow();
			}
		}
	}
	
	@Override
	public String toString() {
		return String.format("(%6.2f,%6.2f) - (%6.2f,%6.2f)", latRange[0], 
				lonRange[0], latRange[1], lonRange[1]);
	}
}
