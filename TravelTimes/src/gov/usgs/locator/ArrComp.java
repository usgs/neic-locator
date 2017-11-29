package gov.usgs.locator;

import java.util.Comparator;

/**
 * Comparator to sort travel-time results into ascending 
 * arrival time order
 * 
 * @author Ray Buland
 *
 */
public class ArrComp implements Comparator<TTimeData> {
	@Override
	/**
	 * Compare the arrival time fields of two arrival results.
	 * 
	 * @param arr1 Travel-time data for the first phase
	 * @param arr2 Travel-time data for the second phase
	 */
	public int compare(TTimeData arr1, TTimeData arr2) {
		return (int)Math.signum(arr1.tt-arr2.tt);
	}
}