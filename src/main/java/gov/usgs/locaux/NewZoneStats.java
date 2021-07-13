package gov.usgs.locaux;

import java.io.IOException;
import java.io.Serializable;

import gov.usgs.locator.BayesianDepth;
import gov.usgs.locator.DepthSource;

/**
 * This class implements the new zone statistics generated by Will Yeck using the EHB data set.  
 * Note that this version supports the first cut with only one mean depth per sample point, 
 * making it a drop in replacement for the old zone statistics.  Like the old zone statistics, 
 * the new zone statistics are sparse.  That is, most points will have no earthquakes within their 
 * averaging radius and hence no statistics.  Despite the sparseness, it turned out to be 
 * convenient to initialize all latitude rows.  However, rows with no statistics don't allocate 
 * storage for the longitude points.  Also, by extending an abstract zone statistics class, it 
 * will be possible to run the new and old zone statistics in parallel.
 * 
 * @author Ray Buland
 *
 */
public class NewZoneStats extends AbstractZoneStats implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;
  
	/** Array of latitude rows. */
	private NewZoneRow[] latRows;

	/**
	 * Initialize ZoneStats storage.
	 * 
	 * @param firstRowLat Geographic latitude of the first row in degrees
	 * @param lastRowLat Geographic latitude of the last row in degrees
	 * @param latSpacing Latitude grid spacing in degrees
	 * @param numLats Number of latitude rows
	 */
	public NewZoneStats(double firstRowLat, double lastRowLat, double latSpacing, 
			int numLats) {
		// Internally, everything is in colatitude.
		this.firstRowLat = 90d - firstRowLat;
		this.lastRowLat = 90d - lastRowLat;
		this.latSpacing = latSpacing;
		latRows = new NewZoneRow[numLats];
	}
	
	/**
	 * Initialize a new latitude row.
	 * 
	 * @param row Row index
	 * @param lat The geographical latitude in degrees
	 * @param lonSpacing The spacing of longitude points in degrees
	 * @param numLons The total number of longitude samples
	 */
	public void initRow(int row, double lat, double lonSpacing, int numLons) {
		// Internally, everything is in colatitude.
		latRows[row] = new NewZoneRow(90d - lat, lonSpacing, numLons);
	}
	
	/**
	 * Get the row colatitude.
	 * 
	 * @param row Latitude row index
	 * @return Row geographic colatitude in degrees
	 */
	public double getLat(int row) {
		return latRows[row].getLat();
	}
	
	/**
	 * Get the number of longitudes in a latitude row.
	 * 
	 * @param row Latitude row index
	 * @return The number of longitudes in the latitude row
	 */
	public int getNumLons(int row) {
		return latRows[row].getNumLons();
	}
	
	/**
	 * Get the longitude spacing in a latitude row.
	 * 
	 * @param row Latitude row index
	 * @return The longitude spacing in degrees in the latitude row
	 */
	public double getLonSpacing(int row) {
		return latRows[row].getLonSpacing();
	}
	
	/**
	 * Put a longitude sample point statistic in this latitude row.
	 * 
	 * @param row Latitude row index
	 * @param column The longitude column index
	 * @param stats ZoneStats statistics for this longitude
	 */
	public void putSample(int row, int column, NewZonePoint stats) {
		latRows[row].putSample(column, stats);
	}
	
	/**
	 * Get a longitude sample point statistic from this latitude row.
	 * 
	 * @param row Latitude row index
	 * @param column The longitude column index
	 * @return ZoneStats statistics for this longitude
	 */
	public NewZonePoint getSample(int row, int column) {
		return latRows[row].getSample(column);
	}
	
  /**
   * Function to get the raw depth statistics by latitude/longitude.
   *
   * @param latitude A double containing the geographic latitude in degrees
   * @param longitude A double containing the geographic longitude in degrees
   * @return A NewZonePoint object containing the zone statistics
   */
	public NewZonePoint getStats(double latitude, double longitude) {
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
   * @return A NewZonePoint object containing the zone statistics
   */
	public NewZonePoint getStats(int latIndex, int lonIndex) {
			return latRows[latIndex].getSample(lonIndex);
	}

	@Override
	public double getBayesDepth(int latIndex, int lonIndex) {
		NewZonePoint point;
		
		point = getStats(latIndex, lonIndex);
		if(point != null) {
			bayesSpread = point.getDepthError();
			return point.getDepth();
		} else {
			bayesSpread = Double.NaN;
			return Double.NaN;
		}
	}
  
	@Override
  protected DepthSource getDepthSource() {
  	return DepthSource.NEWZONESTATS;
  }

	@Override
	protected void canonicalCoords(double lat, double lon) {
	  // We need colatitude (0-180) and longitude in the range of 0-360.
	  coLat = 90d - lat;
	  coLon = (lon >= 0d) ? lon : lon + 360d;
	
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
		return Math.min((int) (coLat / latSpacing), latRows.length - 1);
	}
	
	@Override
	protected int newLonIndex(int latIndex, double coLon) {
		return Math.min((int) (((coLon + 0.5d * latRows[latIndex].lonSpacing) 
				% 360d) / latRows[latIndex].lonSpacing), latRows[latIndex].size() - 1);
	}
	
	@Override
	protected int wrapLonIndex(int latIndex, int lonIndex) {
		if(lonIndex >= 0 && lonIndex < latRows[latIndex].numLons) {
			return lonIndex;
		} else {
			if(lonIndex < 0) {
//				System.out.format("\tWrap: lonIndex = %d => %d (%d)\n", lonIndex, 
//						latRows[latIndex].numLons + lonIndex, latRows[latIndex].numLons);
				return latRows[latIndex].numLons + lonIndex;
			} else {
//				System.out.format("\tWrap: lonIndex = %d => %d (%d)\n", lonIndex, 
//						lonIndex - latRows[latIndex].numLons, latRows[latIndex].numLons);
				return lonIndex - latRows[latIndex].numLons;
			}
		}
	}
	
	@Override
	protected double latFromIndex(int latIndex) {
		double coLat = firstRowLat + latIndex * latSpacing;
		if(coLat < 0d || coLat > 180d) {
			coLat = Double.NaN;
		}
		return coLat;
	}
	
	@Override
	protected double lonFromIndex(int latIndex, int lonIndex) {
		return (lonIndex * latRows[latIndex].lonSpacing) % 360d;
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
  	NewZoneStats test = auxLoc.getNewZoneStats();
  	test.doTest();
  }
  
  /**
   * Do the actual tests here to avoid making everything static.
   */
  public void doTest() {
//  	double lat = 42.6040, lon = 132.0885;
//  	double lat = -63.8000, lon = 298.1089;
//  	double lat = -89.9d, lon = 90d;
  	int maxTrials = 1000000, depthFound = 0;
  	double lat, lon;
  	BayesianDepth bayes;
  	
/*  	System.out.format("Init: firstRowLat = %8.4f lastRowLat = %8.4f latSpacing = %6.4f lastRowLat = %8.4f\n", 
  			firstRowLat, lastRowLat, latSpacing, lastRowLat);
  	canonicalCoords(lat, lon);
  	getIndices();
  	System.out.format("lat = %8.4f lon = %9.4f coLat = %8.4f coLon = %9.4f latIndex " + 
  			"= %d lonIndex = %d\n", lat, lon, coLat, coLon, latIndex, lonIndex);
  	getCoords(latIndex, lonIndex);
  	System.out.format("Sample center: lat = %8.4f lon = %9.4f\n", coLat, coLon);
  	System.out.println(latRows[latIndex]);
  	NewZonePoint sample = latRows[latIndex].getSample(lonIndex);
  	if(sample != null) {
    	System.out.println(sample);
  	} else {
  		System.out.println("NewZonePoint: null");
  	}
  	
  	BayesianDepth bayes = interpolateBayesDepth(lat, lon);
  	System.out.println(bayes); */
  	
		for(int i = 0; i < maxTrials; i++) {
			// Generate the hypocenter.
			lat = 180d * (Math.random() - 0.5d);
			lon = 360d * (Math.random() - 0.5d);
			// Run the Bayesian depth algorithm.
      bayes = interpolateBayesDepth(lat, lon);
      if(!Double.isNaN(bayes.getDepth())) {
      	depthFound++;
	      System.out.format("i = %5d lat = %8.4f lon = %9.4f depth = %6.2f\n", 
	      		i, lat, lon,bayes.getDepth());
      }
		}
		System.out.format("A valid depth was found in %6.2f%% of cases.\n", 
				(100d * depthFound) / maxTrials);
  }
}
