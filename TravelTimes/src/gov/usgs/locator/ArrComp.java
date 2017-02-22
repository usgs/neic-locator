package gov.usgs.locator;

import java.util.Comparator;

/**
 * Comparator to sort travel-time results into ascending 
 * arrival time order
 * 
 * @author Ray Buland
 *
 */
public class ArrComp implements Comparator<TTime> {
	@Override
	/**
	 * Compare the arrival time fields of two arrival results.
	 */
	public int compare(TTime arr1, TTime arr2) {
		return (int)Math.signum(arr2.tt-arr1.tt);
	}
}