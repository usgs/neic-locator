package gov.usgs.locator;

/**
 * Station identifier (expanded station code).  This designation 
 * has expanded even further now, but this was complete during the 
 * Hydra design.
 * 
 * @author Ray Buland
 *
 */
public class StationID implements Comparable<StationID>{
	String staCode;				// Station code
	String locCode;				// Location code
	String netCode;				// Network code
	String staID;					// Comparator string
	
	/**
	 * Set the station designation.
	 * 
	 * @param staCode Station code
	 * @param locCode Location code
	 * @param netCode Network code
	 */
	public StationID(String staCode, String locCode, String netCode) {
		this.staCode = staCode;
		this.locCode = locCode;
		this.netCode = netCode;
		staID = String.format("%-5s%-2s%-2s", staCode, locCode, netCode);
	}
	
	/**
	 * Make station IDs sortable into a sensible order.
	 * 
	 * @param arrival An travel-time data object.
	 * @return +1, 0, or -1 if arrival is later, the same time 
	 * or earlier
	 */
	@Override
	public int compareTo(StationID id) {
		return this.staID.compareTo(id.staID);
	}
	
	/**
	 * Make a printable string to represent the station.
	 */
	@Override
	public String toString () {
		return staID;
	}
}
