package gov.usgs.locator;

import java.io.Serializable;

/**
 * The ZoneStats class implements the historical free depth statistics portion 
 * of the SEDAS zone statistics subsystem. Note that the zone keys was a crude 
 * SEDAS era accelerator for the statistics. This had two advantages.  First, 
 * being Marsden square based, it was fast. Second the statistics themselves 
 * were compact as most Marsden squares don't have enough earthquakes to do 
 * statistics on. To save precious memory, both the keys and statistics were 
 * accessed from random access files.
 * 
 * @author Ray Buland
 *
 */
public class ZoneStats implements Serializable {
  /**
   * A long containing the serializable class version number, used during 
   * deserialization to verify compatibility.
   */
  private static final long serialVersionUID = 1L;

  /**
   * An int containing the size of the zone statistics array.
   */
  private int size;

  /**
   * An int containing the number of years used to compute the statistics.
   */
  private int numYears;

  /**
   * An int[][] used to index into the statistics based on Marsden squares.
   */
  private int[][] zoneKeys;

  /** 
   * An array of ZoneStat objects containing the statistics for one Marsden 
   * square.
   */
  private ZoneStat[] zoneStats;

  /** 
   * A double containing the best Bayesian spread from the zone statistics in
   * kilometers.
   */
  private double bayesSpread;
  
  /**
   * The ZoneStats constructor. Sets up the zone keys and allocate the zone 
   * statistics.
   * 
   * @param zoneKeys An int[][] used to index into the statistics based on 
   *                  Marsden squares
   */
  public ZoneStats(int[][] zoneKeys) {
    this.zoneKeys = zoneKeys;
    size = 0;
    bayesSpread = Double.NaN;

    // Find the biggest key.
    for (int j = 0; j < zoneKeys.length; j++) {
      for (int i = 0; i < zoneKeys[j].length; i++) {
        size = Math.max(size, zoneKeys[j][i]);
      }
    }
  }
  
  /**
   * Function to get the size of the zone statistics array.
   * 
   * @return An int containing the size of the zone statistics array.
   */
  public int size() {
    return size;
  }
  
  /**
   * The function to get the best Bayesian spread from the zone statistics.  
   * Note that this value is computed by the latest call to bayesDepth.
   * 
   * @return A double containing the best Bayesian spread from the zone 
   *          statistics in kilometers.
   */
  public double getBayesSpread() {
    return bayesSpread;
  }

  /**
   * Function to add sdd statistics for a zone.
   * 
   * @param numYears An int holding the number of years in the statistics
   * @param zoneStats A ZoneStat[] containing the Zone depth statistics
   */
  public void addStats(int numYears, ZoneStat[] zoneStats) {
    this.numYears = numYears;
    this.zoneStats = zoneStats;
  }
  
  /**
   * Function to get the depth statistics for an epicenter.
   * 
   * @param latitude A double containing the geographic latitude in degrees
   * @param longitude A double containing the geographic longitude in degrees
   * @return A ZoneStat object containing the zone statistics
   */
  public ZoneStat getStats(double latitude, double longitude) {
    // Set up the zone key indices.  First we need colatitude and 
    // longitude in the range of 0-360.
    latitude = 90d - latitude;
    if (longitude < 0d) {
      longitude += 360d;
    }
    if (longitude == 0d) {
      longitude = 360d;
    }

    // Bail on bad coordinates.
    if (latitude > 180d || latitude < 0d || longitude > 360d || longitude < 0d) {
      return null;
    }

    // Now get the indices.
    int latIndex;
    if (latitude < 180d) {
      latIndex = (int) latitude;
    } else {
      latIndex = 179;
    }
    int lonIndex;
    if (latitude > 0d && latitude < 180d) {
      lonIndex = (int) longitude;
    }  else {
      lonIndex = 0;
    }
    
    // Get the statistics.
    int key = zoneKeys[lonIndex][latIndex];
    if (key >= 0) {
      return zoneStats[key];
    } else {
      return null;
    }
  }
  
  /**
   * This function gets the best Bayesian depth from the zone statistics.
   * 
   * @param latitude A double containing the geographic latitude in degrees
   * @param longitude A double containing the geographic longitude in degrees
   * @return A double holding the bayesian depth in kilometers
   */
  public double getBayesDepth(double latitude, double longitude) {
    // Get the raw statistics.
    ZoneStat stat = getStats(latitude, longitude);

    if (stat != null) {
      // Trap bad depths.
      double meanDepth = Math.min(Math.max(stat.getMeanFreeDepth(), 
          LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);
      double minDepth = Math.min(Math.max(stat.getMinimumFreeDepth(), 
          LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);
      double maxDepth = Math.min(Math.max(stat.getMaximumFreeDepth(), 
          LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);

      // Trap other ugly errors.
      if (minDepth >= maxDepth || meanDepth <= minDepth 
          || meanDepth >= maxDepth) {
        if (meanDepth < 400d) {    // There's nothing magic about 400 km.
          minDepth = Math.max(meanDepth - 0.5 * LocUtil.DEFAULTDEPTHSE, 
              LocUtil.DEPTHMIN);
          maxDepth = minDepth + LocUtil.DEFAULTDEPTHSE;
        } else {
          maxDepth = Math.min(meanDepth + 0.5 * LocUtil.DEFAULTDEPTHSE, 
              LocUtil.DEPTHMAX);
          minDepth = maxDepth - LocUtil.DEFAULTDEPTHSE;
        }
      }
      // Compute the Bayesian depth and spread.
      bayesSpread = Math.max(Math.max(maxDepth - meanDepth, meanDepth - minDepth), 
          LocUtil.DEFAULTDEPTHSE);

      return meanDepth;
    } else {
      bayesSpread = LocUtil.DEFAULTDEPTHSE;
      return LocUtil.DEFAULTDEPTHSE;
    }
  }
}
