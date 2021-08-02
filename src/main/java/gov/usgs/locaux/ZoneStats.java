package gov.usgs.locaux;

import java.io.IOException;
import java.io.Serializable;

import gov.usgs.locator.BayesianDepth;
import gov.usgs.locator.DepthSource;

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

  /** An int containing the size of the zone statistics array. */
  private int size;

  /** An int containing the number of years used to compute the statistics. */
  private int numYears;

  /** An int[][] used to index into the statistics based on Marsden squares. */
  private int[][] zoneKeys;

  /** An array of ZoneStat objects containing the statistics for one Marsden square. */
  private ZoneStat[] zoneStats;

  /**
   * The ZoneStats constructor. Sets up the zone keys and allocate the zone statistics.
   *
   * @param zoneKeys An int[][] used to index into the statistics based on Marsden squares
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
   * @return An int containing the size of the zone statistics array.
   */
  public int size() {
    return size;
  }

  /**
   * Function to get the number of years in the statistics.
   *
   * @return An int containing the number of years in the statistics
   */
  public int getNumYears() {
    return numYears;
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
   * Function to get the raw depth statistics by latitude/longitude.
   *
   * @param latitude A double containing the geographic latitude in degrees
   * @param longitude A double containing the geographic longitude in degrees
   * @return A ZoneStat object containing the zone statistics
   */
  public ZoneStat getStats(double latitude, double longitude) {
  	// Get the ZoneStat indices.
    canonicalCoords(latitude, longitude);
    if(Double.isNaN(coLat) || Double.isNaN(coLon)) {
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
      return new BayesianDepth(meanDepth, minDepth / 3d, maxDepth / 3d, getDepthSource(), 
      		LocUtil.DEFAULTDEPTHSE);
    } else {
    	return null;
    }
	}

	@Override
	protected DepthSource getDepthSource() {
		return DepthSource.ZONESTATS;
	}
	
	@Override
	protected void canonicalCoords(double lat, double lon) {
	  // We need colatitude (0-180) and longitude in the range of 0-360.
	  coLat = 90d - lat;
	  coLon = (lon > 0d) ? lon : lon + 360d;
	
	  // Make sure the coordinates are OK.
	  if(coLat > 180d || coLat < 0d) {
	  	coLat = Double.NaN;
	  }
	  if(coLon > 360d || coLon < 0d) {
	  	coLon = Double.NaN;
	  }
	}

	@Override
	protected int newLatIndex(double coLat) {
    if (coLat < 180d) {
      return (int) coLat;
    } else {
      return 179;
    }
	}

	@Override
	protected int newLonIndex(int latIndex, double coLon) {
    if (coLat > 0d && coLat < 180d) {
      return (int) coLon;
    } else {
      return 0;
    }
	}
	
	@Override
	protected int wrapLonIndex(int latIndex, int lonIndex) {
		if(lonIndex >= 0 && lonIndex < 360) {
			return lonIndex;
		} else {
			if(lonIndex < 0) {
				return 360 + lonIndex;
			} else {
				return lonIndex - 360;
			}
		}
	}

	@Override
	protected double latFromIndex(int latIndex) {
		return latIndex + 0.5d;
	}

	@Override
	protected double lonFromIndex(int latIndex, int lonIndex) {
		return (lonIndex + 0.5d) % 360d;
	}

  /**
   * Driver for a test of the ZoneStats/SlabModel subsystem.
   * 
   * @param args Not used
   * @throws IOException On auxiliary data read error
   * @throws ClassNotFoundException On auxiliary data serialization error
   */
  public static void main(String[] args) throws ClassNotFoundException, IOException {
  	String modelPath = "../../LocRun/models/";
  	
		AuxLocRef auxLoc = new AuxLocRef(modelPath);
  	ZoneStats test = auxLoc.getZoneStats();
  	test.doTest();
  }
  
  public void doTest() {
  	double lat = 42.6040, lon = 132.0885;
  	
  	BayesianDepth bayes = interpolateBayesDepth(lat, lon);
  	System.out.println(bayes);
  }
}
