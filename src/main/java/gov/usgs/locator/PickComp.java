package gov.usgs.locator;

import java.util.Comparator;

/**
 * Pick comparator.  This will sort picks into "Hydra" input 
 * order.  That is, the picks will be sorted alphabetically 
 * by station ID and by arrival time within each station.
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
		// Sort into station and then arrival time order.
		int order = pick1.getStation().staID.staID.
				compareTo(pick2.getStation().staID.staID);

		if(order != 0) {
			return order;
		} else {
			if(pick1.getArrivalTime() < pick2.getArrivalTime()) {
				return -1;
			} else if(pick1.getArrivalTime() > pick2.getArrivalTime()) {
				return +1;
			}	else {
				return 0;
			}
		}
	}
}
