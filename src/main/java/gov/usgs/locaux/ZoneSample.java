package gov.usgs.locaux;

/**
 * Holds information about one ZoneStats cell.
 * 
 * @author Ray Buland
 *
 */
public class ZoneSample implements Comparable<ZoneSample> {
	GeoPoint coord;		// Coordinates of the center of a ZoneStats cell
	double dist;			// Distance from a trial point to the cell center
	
	/**
	 * @return The coordinates of the ZoneStats cell center
	 */
	public GeoPoint getCoord() {return coord;}
	
	/**
	 * @return The distance between this cell center and a trial point
	 */
	public double getDist() {return dist;}
	
	/**
	 * Create a zone sample from the sample indices and calculate the 
	 * distance from the center of this sample to a trial point.
	 * 
	 * @param latIndex ZoneStats cell latitude index
	 * @param lonIndex ZoneStats cell longitude index
	 * @param trial Geographical trial point
	 */
	public ZoneSample(int latIndex, int lonIndex, GeoPoint trial) {
		coord = new GeoPoint(latIndex, lonIndex, trial);
		dist = coord.getDistance(trial);
	}
	
	
	/**
	 * Set the Bayesian depth from the ZoneStats information.
	 * 
	 * @param zoneStats Zone statistics class
	 * @param dist Distance of this cell center from the trial point (debug only)
	 */
	public void setBayesDepth(ZoneStats zoneStats) {
		coord.setBayesDepth(zoneStats, dist);
	}

	@Override
	public int compareTo(ZoneSample sample) {
		if(dist > sample.dist) {
			return +1;
		} else if(dist < sample.dist) {
			return -1;
		} else {
			return 0;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%5.2f %6.2f %4.2f", coord.getLat(), 
				coord.getLon(), dist);
	}
}

/* public class ZoneTest {
	int oldLatIndex, oldLonIndex, newLatIndex, newLonIndex;
	double oldCoLat, oldCoLon, newCoLat, newCoLon;

	public static void main(String[] args) {
		ZoneTest test = new ZoneTest();
		test.doTest();
		
	}
	
	private void doTest() {
		int maxTrials = 10000;
		double tol = 1d;
		double lat, lon, value;
//		double[] values = {1d, 4d, 3d};
		double minVal = 1d, maxVal = 4d;
		GeoPoint trial;
		ArrayList<ZoneSample> coords;
		ArrayList<GeoPoint> poly;
		
		for(int i = 0; i < maxTrials; i++) {
			lat = 180d * (Math.random() - 0.5d);
			lon = 360d * (Math.random() - 0.5d);
			System.out.format("Random trial %4d: lat = %6.2f lon = %7.2f\n", i, lat, lon);
			
			getStats(lat, lon);
			trial = fixCoord(lat, lon);
			if(trial == null) {
				System.exit(100);
			}
			newLatIndex = trial.getLatIndex();
			newLonIndex = trial.getLonIndex();
			if(oldCoLat != newCoLat || oldCoLon != newCoLon) {
				System.out.format("Coordinate Mismatch: %5.2f != %5.2f | %6.2f != %6.2f\n", 
						oldCoLat, newCoLat, oldCoLon, newCoLon);
				System.exit(101);
			}
			if(oldLatIndex != newLatIndex || oldLonIndex != newLonIndex) {
				System.out.format("Index Mismatch: %3d != %3d | %3d != %3d\n", 
						oldLatIndex, newLatIndex, oldLonIndex, newLonIndex);
				System.exit(102);
			}
			
			coords = getCenters(trial);
			coords.sort(null);
			poly = zoneInterp(coords, trial);
			value = facetInterp(poly, trial);
			if(value < minVal || value > maxVal) {
				System.out.format("Interpolated value out of range: %4.2f !in (%4.2f, %4.2f)\n", 
						value, minVal, maxVal);
				System.out.println("\tTrial = " + trial);
				System.out.println("Triangle corners");
				for(int j = 0; j < 3; j++) {
					System.out.println("\t" + j + " " + poly.get(j));
				}
				if(value < minVal - tol || value > maxVal + tol) {
					System.exit(103);
				}
			}
		} */