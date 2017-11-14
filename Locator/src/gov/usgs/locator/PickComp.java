package gov.usgs.locator;

import java.util.Comparator;

/**
 * Pick comparator.  This allows picks to be sorted in to increasing 
 * time order inside an ArrayList.
 * 
 * @author Ray Buland
 *
 */
public class PickComp implements Comparator<Pick> {
	/**
	 * Compare the arrival times of two picks.
	 */
	@Override
	public int compare(Pick pick1, Pick pick2) {
		// Sort into arrival time order.
		if(pick1.arrivalTime < pick2.arrivalTime) return -1;
		else if(pick1.arrivalTime == pick2.arrivalTime) return 0;
		else return +1;
	}
}
