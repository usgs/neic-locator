package gov.usgs.locator;

/**
 * Station identifier (expanded station code).  This designation 
 * has expanded even further now, but this was complete during the 
 * Hydra design.
 * 
 * @author Ray Buland
 *
 */
public class StationID implements Comparable<StationID> {
  /** 
   * A String containing the station code. 
   */
  private String stationCode;
  
  /** 
   * A String containing the location code. 
   */
  private String locationCode;  

  /** 
   * A String containing the network code. 
   */
  private String networkCode;
  
  /** 
   * A String containing the station id / comparator string. 
   */
  private String stationID;
  
  /**
   * Function to return the station code.
   * 
   * @return A String containing the station code
   */
  public String getStationCode() {
    return stationCode;
  }

  /**
   * Function to return the location code.
   * 
   * @return A String containing the location code
   */
  public String getLocationCode() {
    return locationCode;
  }

  /**
   * Function to return the network code.
   * 
   * @return A String containing the network code
   */
  public String getNetworkCode() {
    return networkCode;
  }
  
  /**
   * Function to return the station ID.
   * 
   * @return A String containing the station ID
   */
  public String getStationID() {
    return stationID;
  }

  /**
   * The StationID constructor. Initializes the StationID to the provided values
   * 
   * @param stationCode A String containing the station code 
   * @param locationCode A String containing the location code 
   * @param networkCode A String containing the network code
   */
  public StationID(String stationCode, String locationCode, String networkCode) {
    this.stationCode = stationCode;
    this.locationCode = locationCode;
    this.networkCode = networkCode;

    stationID = String.format("%-5s%-2s", stationCode, networkCode);
  }
  
  /**
   * This fuction converts the stationID contents into a string.
   */
  @Override
  public String toString() {
    return stationID;
  }
  
  /**
   * Comparison function used to sort stationIDs into a sensible order.
   * @param id A StationID object containing the second station id to compare
   * @return +1, 0, or -1 if this StationID is alphabetically later, the same  
   *         or earlier
   */
  @Override
  public int compareTo(StationID id) {
    return this.stationID.compareTo(id.getStationID());
  }
}
