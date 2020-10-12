package gov.usgs.locaux;

/**
 * Holds information about one ZoneStats cell.
 * 
 * @author Ray Buland
 *
 */
public class ZoneSample implements Comparable<ZoneSample> {
	GeoPoint coord;		// Coordinates of the center of a ZoneStats cell
	
	/**
	 * @return The coordinates of the ZoneStats cell center
	 */
	public GeoPoint getCoord() {return coord;}
	
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
	}
	
	/**
	 * Set the Bayesian depth from the ZoneStats information.
	 * 
	 * @param zoneStats Zone statistics class
	 */
	public void setBayesDepth(ZoneStats zoneStats) {
		coord.setBayesDepth(zoneStats);
	}

	@Override
	public int compareTo(ZoneSample sample) {
		if(coord.getDist() > sample.getCoord().getDist()) {
			return +1;
		} else if(coord.getDist() < sample.getCoord().getDist()) {
			return -1;
		} else {
			return 0;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%5.2f %6.2f", coord.getLat(), coord.getLon());
	}
}