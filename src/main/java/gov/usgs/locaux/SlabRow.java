package gov.usgs.locaux;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Store slab depth points in one latitude row.  One latitude row of one 
 * area may have multiple segments because of the typical arc structure 
 * of slabs or because there are multiple slabs in the area.
 * 
 * @author Ray Buland
 *
 */
public class SlabRow implements Serializable {
	private static final long serialVersionUID = 1L;
	int segFound = -1;
	double lat;
	double[] lonRange;
	ArrayList<SlabPoint> slabPoints;
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
	 * The constructor just sets up storage for slab depth points.
	 */
	public SlabRow() {
		slabPoints = new ArrayList<SlabPoint>();
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
	 */
	public void squeeze() {
		int start, end;
		
		// Squeeze out points where the earthquake depth is NaN.
		lat = slabPoints.get(0).getLat();
		for(int j = 0; j < slabPoints.size(); j++) {
			if(!Double.isNaN(slabPoints.get(j).getEqDepth())) {
				start = j++;
				for(; j<slabPoints.size(); j++) {
					if(Double.isNaN(slabPoints.get(j).getEqDepth())) {
						end = j;
						if(slabSegs == null) {
							slabSegs = new ArrayList<SlabSeg>();
						}
						slabSegs.add(new SlabSeg(slabPoints.subList(start, end)));
						if(lonRange == null) {
							lonRange = new double[2];
							lonRange[0] = slabPoints.get(start).getLon();
						}
						lonRange[1] = slabPoints.get(end - 1).getLon();
						break;
					}
				}
			}
		}
		slabPoints = null;
	}
	
	/**
	 * Test to see if the desired point falls in this row.  If it does, 
	 * remember which segment it's in.
	 * 
	 * @param lat Geographic colatitude in degrees (0-180 degrees)
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @return True if the desired point is within in this row
	 */
	public boolean isFound(double lat, double lon) {
		if(lat >= this.lat && lat < this.lat + SlabSeg.latIncrement && 
				lon >= lonRange[0] && lon <= lonRange[1]) {
			for(int j = 0; j < slabSegs.size(); j++) {
				if(slabSegs.get(j).isFound(lon)) {
					segFound = j;
					System.out.println("Row0: seg = " + j + " " + slabSegs.get(j));
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the slab depth triplet at a point.
	 * 
	 * @param lon Geographic longitude in degrees (0-360 degrees)
	 * @return Slab depth triplet
	 */
	public SlabDepth getDepth(double lon) {
		// If we haven't found the segment yet, get it.
		if(segFound < 0) {
		segFound = -1;
			for(int j = 0; j < slabSegs.size(); j++) {
				if(slabSegs.get(j).isFound(lon)) {
					segFound = j;
					System.out.println("Row1: seg = " + j + " " + slabSegs.get(j));
				}
			}
		}
		// Find the point in this segment.
		if(segFound >= 0) {
			return slabSegs.get(segFound).getDepth(lon);
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
	 * values
	 */
	public void getVectors(double lon, double[][][] v) {
		SlabDepth[] depths;
		
		double lon0 = SlabSeg.lonIncrement * ((int) (lon / SlabSeg.lonIncrement));
		depths = new SlabDepth[2];
		depths[0] = getDepth(lon0);
		depths[1] = getDepth(lon0 + SlabSeg.lonIncrement);
		
		for(int i = 0; i < depths.length; i++) {
			if(depths[i] != null) {
				v[i] = depths[i].getVectors(lat, lon0 + i * SlabSeg.lonIncrement);
			} else {
				v[i] = null;
			}
		}
		segFound = -1;
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
		if(lonRange != null) {
			return String.format("(%6.2f,%6.2f-%6.2f)", lat, 
					lonRange[0], lonRange[1]);
		} else {
			return "No slabs";
		}
	}
}
