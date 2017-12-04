package gov.usgs.locator;

import java.util.Comparator;

/**
 * Group comparator.  This will sort groups by distance.
 * 
 * @author Ray Buland
 *
 */
public class GroupComp implements Comparator<PickGroup> {

	@Override
	public int compare(PickGroup group1, PickGroup group2) {
		if(group1.delta < group2.delta) return -1;
		else if(group1.delta > group2.delta) return +1;
		else return 0;
	}

}
