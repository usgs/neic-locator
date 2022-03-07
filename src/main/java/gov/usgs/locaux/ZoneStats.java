package gov.usgs.locaux;

import gov.usgs.locator.BayesianDepth;
import gov.usgs.locator.DepthSource;
import java.io.IOException;
import java.io.Serializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The ZoneStats class implements the historical free depth statistics portion of the SEDAS zone
 * statistics subsystem. Note that the zone keys was a crude SEDAS era accelerator for the
 * statistics. This had two advantages. First, being Marsden square based, it was fast. Second the
 * statistics themselves were compact as most Marsden squares don't have enough earthquakes to do
 * statistics on. To save precious memory, both the keys and statistics were accessed from random
 * access files.
 *
 * @author Ray Buland
 */
public class ZoneStats extends AbstractZoneStats implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** An integer containing the size of the zone statistics array. */
  private int size;

  /** An integer containing the number of years used to compute the statistics. */
  private int numYears;

  /**
   * A two dimensional array of integers used to index into the statistics based on Marsden squares.
   */
  private int[][] zoneKeys;

  /** An array of ZoneStat objects containing the statistics for one Marsden square. */
  private ZoneStat[] zoneStats;

  /** Private logging object. */
  private static final Logger LOGGER = LogManager.getLogger(ZoneStats.class.getName());

  /**
   * The ZoneStats constructor. Sets up the zone keys and allocate the zone statistics.
   *
   * @param zoneKeys A two dimensional array of integers used to index into the statistics based on
   *     Marsden squares.
   */
  public ZoneStats(int[][] zoneKeys) {
    this.zoneKeys = zoneKeys;
    size = 0;

    // Find the biggest key.
    for (int j = 0; j < zoneKeys.length; j++) {
      for (int i = 0; i < zoneKeys[j].length; i++) {
        size = Math.max(size, zoneKeys[j][i]);
      }
    }

    // Set the latitude parameters.
    firstRowLat = 0.5d;
    lastRowLat = 179.5d;
    latSpacing = 1d;
  }

  /**
   * Function to get the size of the zone statistics array.
   *
   * @return An integer containing the size of the zone statistics array.
   */
  public int size() {
    return size;
  }

  /**
   * Function to get the number of years in the statistics.
   *
   * @return An integer containing the number of years in the statistics
   */
  public int getNumYears() {
    return numYears;
  }

  /**
   * Function to add sdd statistics for a zone.
   *
   * @param numYears An integer holding the number of years in the statistics
   * @param zoneStats An array of ZoneStat objects containing the Zone depth statistics
   */
  public void addStats(int numYears, ZoneStat[] zoneStats) {
    this.numYears = numYears;
    this.zoneStats = zoneStats;
  }

  /**
   * Function to get the raw depth statistics by latitude/longitude.
   *
   * @param latitude A double containing the geographic latitude in degrees
   * @param longitude A double containing the geographic longitude in degrees
   * @return A ZoneStat object containing the zone statistics
   */
  public ZoneStat getStats(double latitude, double longitude) {
    // Get the ZoneStat indices.
    computeCanonicalCoords(latitude, longitude);

    if (Double.isNaN(coLat) || Double.isNaN(coLon)) {
      return null;
    }

    getIndices();

    // Get the statistics.
    return getStats(latIndex, lonIndex);
  }

  /**
   * Function to get the raw depth statistics by latitude/longitude indices.
   *
   * @param latIndex ZoneStats colatitude index
   * @param lonIndex ZoneStats longitude index
   * @return A ZoneStat object containing the zone statistics
   */
  public ZoneStat getStats(int latIndex, int lonIndex) {
    // Get the statistics.
    int key = zoneKeys[lonIndex][latIndex];

    if (key >= 0) {
      return zoneStats[key];
    } else {
      return null;
    }
  }

  @Override
  public BayesianDepth getBayesDepth(int latIndex, int lonIndex) {
    // Get the raw statistics.
    ZoneStat stat = getStats(latIndex, lonIndex);

    if (stat != null) {
      // Trap bad depths.
      double meanDepth =
          Math.min(Math.max(stat.getMeanFreeDepth(), LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);
      double minDepth =
          Math.min(Math.max(stat.getMinimumFreeDepth(), LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);
      double maxDepth =
          Math.min(Math.max(stat.getMaximumFreeDepth(), LocUtil.DEPTHMIN), LocUtil.DEPTHMAX);

      // Trap other ugly errors.
      if (minDepth >= maxDepth || meanDepth <= minDepth || meanDepth >= maxDepth) {
        if (meanDepth < 400d) { // There's nothing magic about 400 km.
          minDepth = Math.max(meanDepth - 0.5 * LocUtil.DEFAULTDEPTHSE, LocUtil.DEPTHMIN);
          maxDepth = minDepth + LocUtil.DEFAULTDEPTHSE;
        } else {
          maxDepth = Math.min(meanDepth + 0.5 * LocUtil.DEFAULTDEPTHSE, LocUtil.DEPTHMAX);
          minDepth = maxDepth - LocUtil.DEFAULTDEPTHSE;
        }
      }

      /*
       * Oddly, the mean depth seems to be a better indicator of a deep earthquake zone than
       * the deepest depth.  There seem to be several historical reasons for this behavior:
       * 1) if there are shallow earthquakes in this cell, they must have all had their depths
       * held so that the mean just sampled the deep zone and 2) the deepest depth is typically
       * way too deep because of poor depth control (events with depth controlled by P-pP times
       * were held to the average P-pP depth) and so were not counted in the mean free depth).
       */
      return new BayesianDepth(
          meanDepth, minDepth / 3d, maxDepth / 3d, getDepthSource(), LocUtil.DEFAULTDEPTHSE);
    } else {
      return null;
    }
  }

  /**
   * Function to get depth source type for this ZoneStats
   *
   * @return A DepthSource containing the depth source identification string
   */
  @Override
  protected DepthSource getDepthSource() {
    return DepthSource.ZONESTATS;
  }

  /**
   * Function to convert coordinates from geographic latitude and longitude to geographic colatitude
   * and longitude in the range 0 to 360 degrees. These coordinates are remembered as coLat and
   * coLon.
   *
   * @param lat Geographic latitude in degrees
   * @param lon Geographic longitude in degrees
   */
  @Override
  protected void computeCanonicalCoords(double lat, double lon) {
    // We need colatitude (0-180) and longitude in the range of 0-360.
    coLat = 90d - lat;
    coLon = (lon > 0d) ? lon : lon + 360d;

    // Make sure the coordinates are OK.
    if (coLat > 180d || coLat < 0d) {
      coLat = Double.NaN;
    }
    if (coLon > 360d || coLon < 0d) {
      coLon = Double.NaN;
    }
  }

  /**
   * Function to compute the latitude index of the nearest latitude row.
   *
   * @param coLat Geographical colatitude in degrees
   * @return Integer containing the index of the nearest latitude row
   */
  @Override
  protected int newLatIndex(double coLat) {
    if (coLat < 180d) {
      return (int) coLat;
    } else {
      return 179;
    }
  }

  /**
   * Function to compute the longitude index of the nearest longitude sample.
   *
   * @param latIndex Index of the nearest latitude row
   * @param coLon Geographic longitude in degrees (0-360)
   * @return Integer containing the index of the nearest longitude sample
   */
  @Override
  protected int newLonIndex(int latIndex, double coLon) {
    if (coLat > 0d && coLat < 180d) {
      return (int) coLon;
    } else {
      return 0;
    }
  }

  /**
   * Function to wrap the longitude.
   *
   * <p>When looking for neighboring samples in longitude, it may be necessary to wrap the longitude
   * index.
   *
   * @param latIndex Colatitude row index
   * @param lonIndex Longitude column index
   * @return Integer containing the valid longitude column index
   */
  @Override
  protected int wrapLonIndex(int latIndex, int lonIndex) {
    if (lonIndex >= 0 && lonIndex < 360) {
      return lonIndex;
    } else {
      if (lonIndex < 0) {
        return 360 + lonIndex;
      } else {
        return lonIndex - 360;
      }
    }
  }

  /**
   * Function to compute the geographic colatitude of the indexed latitude row (i.e., the colatitude
   * of the samples in this row).
   *
   * @param latIndex Index of a latitude row
   * @return Double containing geographic colatitude in degrees
   */
  @Override
  protected double latFromIndex(int latIndex) {
    return latIndex + 0.5d;
  }

  /**
   * Function to compute the geographic longitude of the indexed sample point.
   *
   * @param latIndex Index of a latitude row
   * @param lonIndex Index of a longitude sample
   * @return Double containing geographic longitude in degrees (0-360)
   */
  @Override
  protected double lonFromIndex(int latIndex, int lonIndex) {
    return (lonIndex + 0.5d) % 360d;
  }

  /**
   * Main function to act as the driver for a test of the ZoneStats/SlabModel subsystem.
   *
   * @param args Not used
   * @throws IOException On auxiliary data read error
   * @throws ClassNotFoundException On auxiliary data serialization error
   */
  public static void main(String[] args) throws ClassNotFoundException, IOException {
    String modelPath = "../../LocRun/models/";

    AuxLocRef auxLoc = new AuxLocRef(modelPath, modelPath);
    ZoneStats test = auxLoc.getZoneStats();
    test.doTest();
  }

  /**
   * ZoneStats test function
   *
   * <p>Do the actual tests here to avoid making everything static.
   */
  public void doTest() {
    double lat = 42.6040, lon = 132.0885;

    BayesianDepth bayes = interpolateBayesDepth(lat, lon);
    LOGGER.info(bayes);
  }
}
