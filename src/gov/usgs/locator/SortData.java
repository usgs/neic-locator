package gov.usgs.locator;

/**
 * This simple class facilitates sorting a data value and 
 * it's original index.
 * 
 * @author Ray Buland
 *
 */
class SortData implements Comparable<SortData> {
	protected int index;					// Data index
	protected double value;				// Data value
	
	/**
	 * Sort data.
	 * 
	 * @param index Original data index
	 * @param value Data value
	 */
	protected SortData(int index, double value) {
		this.index = index;
		this.value = value;
	}

	@Override
	public int compareTo(SortData data) {
		if(value < data.value) return -1;
		else if(value > data.value) return 1;
		return 0;
	}
}