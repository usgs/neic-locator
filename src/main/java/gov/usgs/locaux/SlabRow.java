package gov.usgs.locaux;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Store slab depth points in one latitude row. One latitude row of one area may have multiple
 * segments because of the typical arc structure of slabs or because there are multiple slabs in the
 * area.
 *
 * @author Ray Buland
 */
public class SlabRow implements Serializable {
	private static final long serialVersionUID = 1L;
	int segFound = -1;					// Remember the segment where the epicenter was found
	double lat;									// The colatitude of this row is constant
	double[] lonRange = null;		// Longitude range spanned by the segments
	ArrayList<SlabPoint> slabPoints = null;
	ArrayList<SlabSeg> slabSegs = null;
	
	/**
	 * @return Geographic colatitude in degrees (0-180 degrees)
	 */
	public double getLat() {
		return lat;
	}
	
	/**
	 * @return Geographic longitude range  in degrees (0-360 degrees)
	 */
	public double[] getLonRange() {
		return lonRange;
	}
	
	/**
	 * See if this row is a dummy (i.e., has no data).
	 * 
	 * @return True if it is a dummy row
	 */
	public boolean isDummyRow() {
		if(slabSegs != null) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * The constructor just sets up storage for raw slab depth points.
	 */
	public SlabRow() {
		slabPoints = new ArrayList<SlabPoint>();
	}
	/**
	 * This alternate constructor is used to create place holder rows.  
	 * The place holder rows are needed to ensure completeness in 
	 * longitude.
	 * 
	 * @param lat
	 */
	public SlabRow(double lat) {
		this.lat = lat;
	}
	
	/**
	 * Add a new slab depth point in this latitude row.
	 * 
	 * @param point Slab depth point
	 */
	public void add(SlabPoint point) {
		slabPoints.add(point);
	}
	
	/**
	 * When the row is complete, create segments that are contiguous in 
	 * longitude.  The intermediate list of points is then freed for 
	 * garbage collection.
	 * 
	 * @param slabInc Slab latitude-longitude grid spacing in degrees
	 */
	public void squeeze(double slabInc) {
		int start = 0;
		
		// Squeeze out points where the earthquake depth is NaN.
		lat = slabPoints.get(0).getLat();
		for(int j = 0; j < slabPoints.size(); j++) {
			if(!Double.isNaN(slabPoints.get(j).getEqDepth())) {
				start = j++;
				for(; j<slabPoints.size(); j++) {
					if(Double.isNaN(slabPoints.get(j).getEqDepth())) {
						addSegment(start, j, slabInc);
						break;
					}
				}
			}
		}
		// Be sure we get segments that go to the end of the area.
		if(!Double.isNaN(slabPoints.get(slabPoints.size()-1).getEqDepth())) {
			addSegment(start, slabPoints.size(), slabInc);
		}
		// We don't need slabPoints any more.
		slabPoints = null;
	}
	
	/**
	 * Test to see if the desired point falls in this row.  If it does, 
	 * remember which segment it's in.
	 * 
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @return True if the desired point is within in this row
	 */
	public boolean isFound(double lon) {
		if(slabSegs != null) {
			if(lon >= lonRange[0] && lon <= lonRange[1]) {
				for(int j = 0; j < slabSegs.size(); j++) {
					if(slabSegs.get(j).isFound(lon)) {
						segFound = j;
						return true;
					}
				}
			}
		}
		segFound = -1;
		return false;
	}
	
	/**
	 * Get the slab depth triplet at a point.
	 * 
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @param slabInc Slab latitude-longitude grid spacing in degrees
	 * @return Slab depth triplet
	 */
	public SlabDepth getDepth(double lon, double slabInc) {
		// If we haven't found the segment yet, get it.
		if(segFound < 0) {
			segFound = -1;
			// If this row was filler, it may have no segments.
			if(slabSegs != null) {
				for(int j = 0; j < slabSegs.size(); j++) {
					if(slabSegs.get(j).isFound(lon)) {
						segFound = j;
					}
				}
			}		}
		// Find the point in this segment.
		if(segFound >= 0) {
			return slabSegs.get(segFound).getDepth(lon, slabInc);
		} else {
			return null;
		}
	}
	
	/**
	 * Get 3-vectors for interpolation.  Note that we can only get the two 
	 * position 3-vectors for the longitude values surrounding the desired 
	 * point since the other two are in the next row (hopefully).
	 * 
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @param v Get the position 3-vectors for the two bounding longitude
	 * @param slabInc Slab latitude-longitude grid spacing in degrees
	 * values
	 */
	public void getVectors(double lon, double[][][] v, double slabInc) {
		SlabDepth[] depths;
		
		// Get the first vector using the longitude.
		depths = new SlabDepth[2];
		depths[0] = getDepth(lon, slabInc);
		if(depths[0] != null) {
			v[0] = depths[0].getVectors(lat, slabSegs.get(segFound).getLon(slabInc));
		} else {
			v[0] = null;
		}
		
		// Get the second vector using the next depth for robustness.
		if(segFound >= 0) {
			depths[1] = slabSegs.get(segFound).getNextDepth();
			if(depths[1] != null) {
				v[1] = depths[1].getVectors(lat, slabSegs.get(segFound).getLon(slabInc));
			} else {
				v[1] = null;
			}
		} else {
			depths[1] = null;
		}
		segFound = -1;
	}
	
	/**
	 * Copy a run of non-null points into a new segment.
	 * 
	 * @param start Start index of raw points
	 * @param end End index of raw points
	 * @param slabInc Slab latitude-longitude grid spacing in degrees
	 */
	private void addSegment(int start, int end, double slabInc) {
		if(slabSegs == null) {
			slabSegs = new ArrayList<SlabSeg>();
		}
		slabSegs.add(new SlabSeg(slabPoints.subList(start, end), slabInc));
		if(lonRange == null) {
			lonRange = new double[2];
			lonRange[0] = slabPoints.get(start).getLon() - slabInc / 2d;
		}
		lonRange[1] = slabPoints.get(end - 1).getLon() + slabInc / 2d;
	}
	
	/**
	 * Print out a summary of the points before squeezing.
	 */
	public void printRaw() {
		if(slabPoints != null) {
			int last = slabPoints.size() - 1;
			System.out.format("\t\tRaw: (%6.2f,%6.2f) - (%6.2f,%6.2f)\n", 
					slabPoints.get(0).getLat(), slabPoints.get(0).getLon(), 
					slabPoints.get(last).getLat(), slabPoints.get(last).getLon());
		}
	}
	
	/**
	 * Print out a summary of the segments created by squeezing.
	 */
	public void printRow() {
		if(lonRange != null) {
			System.out.println("\tRow: " + toString());
			if(slabSegs != null) {
				for(int j = 0; j < slabSegs.size(); j++) {
					System.out.println("\t\tSeg: " + slabSegs.get(j));
				}
			}
		}
	}
	
	@Override
	public String toString() {
		// If slabPoints is still around, show the raw summary.
		if(slabPoints != null) {
			if(slabPoints.size() > 0) {
			int last = slabPoints.size() - 1;
			return String.format("Raw: (%6.2f,%6.2f) - (%6.2f,%6.2f)\n", 
					slabPoints.get(0).getLat(), slabPoints.get(0).getLon(), 
					slabPoints.get(last).getLat(), slabPoints.get(last).getLon());
			} else {
				return "No points have been added";
			}
		// Otherwise, show the final processed summary.
		} else {
			if(lonRange != null) {
				return String.format("(%6.2f,%6.2f-%6.2f)", lat, 
						lonRange[0], lonRange[1]);
			} else {
				return "No slabs";
			}
		}
	}
}
