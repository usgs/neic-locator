package gov.usgs.locator;

/**
 * This generalized index interface allows computed sample 
 * points to be used like they were actual arrays for 
 * bilinear interpolation.
 * 
 * @author Ray Buland
 *
 */
public interface GenIndex {
	
	/**
	 * Get the virtual array index from a value.
	 * 
	 * @param value Generic value
	 * @return Index of virtual array index from value
	 */
	int getIndex(double value);
	
	/**
	 * Get the virtual array value from an index.
	 * 
	 * @param index Virtual array index
	 * @return Value at that virtual array index
	 */
	double getValue(int index);
}
