package gov.usgs.locaux;

import gov.usgs.traveltime.FileChanged;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileLock;
import java.util.Scanner;
// import java.util.logging.Logger;

/**
 * The AuxLocRef class manages auxiliary data files that support the Locator such as the continental
 * craton boundries and earthquake statistics by geographic location files. Note that this data is
 * static (does not change during a location run) and will be common to all events subsequently
 * located.
 *
 * @author Ray Buland
 */
public class AuxLocRef {
  /** A Cratons object containing the continental craton boundaries. */
  private final Cratons cratons;

  /** A ZoneStats object containing earthquake statistics by geographic location. */
  private final ZoneStats zoneStats;

  /** A Slabs object containing the geometry of earthquakes in slabs. */
  private final Slabs slabs;
  
  /**
   * An integer containing the number of years read from the earthquake statistics zonestats file.
   */
  private int numberOfYears = -1;

  /** Default path for model files. */
  public static final String DEFAULT_MODEL_PATH = "./models/";

  /** A String containing the path for the model files. */
  public String modelPath;

  /** A String containing the serialized file name. */
  private String serializedFileName = "locaux.ser";

  /** An array of String objects containing the raw input model file names. */
  private String[] modelFileNames = {"cratons.txt", "zonekey.dat", "zonestat.dat", 
  		"slabmaster.txt"};

  /**
   * A Scanner object used to read continental craton boundaries and earthquake statistics by
   * geographic location.
   */
  private Scanner scan;

  /** Private logging object. */
  // private static final Logger LOGGER = Logger.getLogger(LocMain.class.getName());

  /**
   * Read the cratons and zone statistics files and make the data available to the Locator.
   *
   * @param modelPath If not null, path to model files
   * @throws IOException On any read error
   * @throws ClassNotFoundException In input serialization is hosed
   */
  public AuxLocRef(String modelPath) throws IOException, ClassNotFoundException {
    int[][] zoneKeys;
    String[] absNames;
    ZoneStat[] stats;
    BufferedInputStream inCratons, inSlabs;
    RandomAccessFile inZones;
    FileInputStream serIn;
    FileOutputStream serOut;
    ObjectInputStream objIn;
    ObjectOutputStream objOut;
    FileLock lock;

    if (modelPath == null) {
      this.modelPath = DEFAULT_MODEL_PATH;
    } else {
      this.modelPath = modelPath;
    }

    // Create absolute path names.
    absNames = new String[modelFileNames.length];
    for (int j = 0; j < modelFileNames.length; j++) {
      absNames[j] = modelPath + modelFileNames[j];
    }

    // If any of the raw input files have changed, regenerate the
    // serialized file.
    if (FileChanged.isChanged(modelPath + serializedFileName, absNames)) {
      // Open and read the cratons file.
      inCratons = new BufferedInputStream(new FileInputStream(absNames[0]));
      scan = new Scanner(inCratons);
      cratons = new Cratons();
      while (scan.hasNext()) {
        readCraton();
      }
      scan.close();
      inCratons.close();

      // Open and read the zone key file.
      inZones = new RandomAccessFile(absNames[1], "r");
      zoneKeys = readZoneKeys(inZones);
      zoneStats = new ZoneStats(zoneKeys);
      inZones.close();

      // Open and read the zone statistics file.
      inZones = new RandomAccessFile(absNames[2], "r");
      stats = readZoneStats(inZones);
      zoneStats.addStats(numberOfYears, stats);
      inZones.close();
      
      // Open and read the slab geometry model file.
      inSlabs = new BufferedInputStream(new FileInputStream(absNames[3]));
      scan = new Scanner(inSlabs);
      slabs = new Slabs();
      readSlabs();
  	  inSlabs.close();

      // Write out the serialized file.
  	  System.out.println("Recreate the serialized file.");
      serOut = new FileOutputStream(modelPath + serializedFileName);
      objOut = new ObjectOutputStream(serOut);

      // Wait for an exclusive lock for writing.
      lock = serOut.getChannel().lock();
  //   LOGGER.fine(
   //      "AuxLocRef write lock: valid = " + lock.isValid() + " shared = " + lock.isShared());

      // The auxiliary data can be read and written very quickly, so for persistent
      // applications such as the travel time or location server, serialization is
      // not necessary.  However, if the travel times are needed for applications
      // that start and stop frequently, the serialization should save some set up
      // time.
      objOut.writeObject(cratons);
      objOut.writeObject(zoneStats);
      objOut.writeObject(slabs);

      if (lock.isValid()) {
        lock.release();
      }

      // close files
      objOut.close();
      serOut.close();
    } else {
      // Read in the serialized file.
    	System.out.println("Read the serialized file.");
      serIn = new FileInputStream(modelPath + serializedFileName);
      objIn = new ObjectInputStream(serIn);

      // Wait for a shared lock for reading.
      lock = serIn.getChannel().lock(0, Long.MAX_VALUE, true);
   //	LOGGER.fine(
   //     "AuxLocRef read lock: valid = " + lock.isValid() + " shared = " + lock.isShared());

      // load the cratons and zoneStats
      cratons = (Cratons) objIn.readObject();
      zoneStats = (ZoneStats) objIn.readObject();
      slabs = (Slabs) objIn.readObject();

      if (lock.isValid()) {
        lock.release();
      }

      // close files
      objIn.close();
      serIn.close();
    }
  }

  /** Function to read a craton from the file. */
  private void readCraton() {
    // read the region name
    String region = scan.next();
    while (!scan.hasNextInt()) {
      region = region + " " + scan.next();
    }

    // get the number of region boundary points
    int len = scan.nextInt();

    // read the region boundary points
    double[] lats = new double[len];
    double[] lons = new double[len];
    for (int j = 0; j < len; j++) {
      lats[j] = scan.nextDouble();
      lons[j] = scan.nextDouble();
    }

    // add craton to list
    cratons.add(region, lats, lons);
  }

  /**
   * Function to read the zone keys file. Because the earthquake statistics are sparse with respect
   * to Marsden squares, it was convenient to index them through a separate file. Note that the zone
   * keys array is organized by longitude (Greenwich to Greenwich heading east) and co-latitude
   * (north pole to south pole going south).
   *
   * @param inKeys RandomAccessFile object for the zone keys file
   * @return An int[][] containing the zone keys
   * @throws IOException On any read error
   */
  private int[][] readZoneKeys(RandomAccessFile inKeys) throws IOException {
    // Read the file.
    int length = (int) inKeys.length();
    byte[] byteArray = new byte[length];
    inKeys.read(byteArray);
    ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
    byteBuf.order(ByteOrder.LITTLE_ENDIAN);
    IntBuffer intBuf = byteBuf.asIntBuffer();

    // Create the zoneKeys array.
    int[][] zoneKeys = new int[360][180];
    int k = 0;
    for (int i = 0; i < 180; i++) {
      for (int j = 0; j < 360; j++) {
        // Note that the key is off by 2 because of Fortran indexing and the
        // special first record in the statistics file.
        zoneKeys[j][i] = intBuf.get(k++) - 2;
      }
    }
    return zoneKeys;
  }

  /**
   * Function to read the zone statistics file. Note that only the small part of the statistics
   * contained in the file are relevant to the Locator, and are retained.
   *
   * @param inZones File handle for the zone statistics file
   * @return A ZoneStat[] conatining the zone statistics
   * @throws IOException On any read error
   */
  private ZoneStat[] readZoneStats(RandomAccessFile inZones) throws IOException {
    // Read the file.
    int length = (int) inZones.length();
    byte[] byteArray = new byte[length];
    inZones.read(byteArray);
    ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
    byteBuf.order(ByteOrder.LITTLE_ENDIAN);
    numberOfYears = byteBuf.getInt();
    // System.out.println("ZoneStats years = " + numberOfYears);
    byteBuf.position(40); // WHY

    // Create the zoneStats array by reading the zonestats information
    // skipping the fields we don't need
    ZoneStat[] stats = new ZoneStat[zoneStats.size()];
    for (int j = 0; j < stats.length; j++) {
      byteBuf.getInt(); // ndeg, skip
      byteBuf.getFloat(); // events per year, skip
      byteBuf.getFloat(); // maximum magnitude, skip
      byteBuf.getFloat(); // minimum magnitude, skip
      double minDepth = byteBuf.getFloat(); // minimum depth
      double maxDepth = byteBuf.getFloat(); // maximum depth
      double pctfre = byteBuf.getFloat(); // pctfre
      double meanDepth = byteBuf.getFloat(); // average depth
      byteBuf.getInt(); // dpmode, skip
      byteBuf.getFloat(); // pctge, skip

      if (pctfre > 0. && minDepth < 900d) {
        // locator only cares about zone stats depth statistics
        stats[j] = new ZoneStat(meanDepth, minDepth, maxDepth);
        // System.out.format("\t%3d %4.1f %4.1f %4.1f %5.1f %5.1f %4.1f %5.1f %3d %4.1f\n",
        //  ndeg, peryr, maxmag, minmag, minDepth, maxDepth, pctfre, meanDepth, dpmode,
        //  pctge);
      } else {
        stats[j] = null;
      }
    }
    return stats;
  }
  
  /**
   * Read in the slab model.
   */
  private void readSlabs() {
  	boolean first = true;
  	double firstLon, lastLon, tol = 1e-3d;
  	SlabArea area;
  	SlabRow row;
  	SlabPoint point;
  	
	  // Prime the pump.
	  area = new SlabArea();
	  row = new SlabRow();
	  // Read in the first point.
	  point = scanLine();
	  row.add(point);
	  firstLon = point.getLon();
	  lastLon = firstLon;
	  
	  // As long as there's still data, keep on trucking.
	  while(scan.hasNextDouble()) {
	  	point = scanLine();
	  	// Look for the end of a latitude row.
	  	if(Math.abs(point.getLon() - lastLon) > LocUtil.SLABINCREMENT + tol) {
	  		// Print out the first and last points in each area.
	  		if(first || Math.abs(point.getLon() - firstLon) > tol) {
	  			row.printRaw();
	  			if(first) {
	  				first = false;
	  			} else {
	  				first = true;
	  			}
	  		}
	  		// Squeeze out the NaNs and save the remaining data in segments.
	  		row.squeeze();
	  		// Look for the start of a new area.
	  		if(Math.abs(point.getLon() - firstLon) > tol) {
	  			// Print a summary of the last area.
	  			area.printArea(false);
	  			// Add the last area to all.
	  			slabs.add(area);
	  			// Start a new area.
	  			area = new SlabArea();
	  			firstLon = point.getLon();
	  		}
	  		// Add the last row to the current area.
	  		area.add(row);
	  		// Start a new row.
	  		row = new SlabRow();
	  	}
	  	// Add the current point to the current row.
	  	row.add(point);
  		lastLon = point.getLon();
	  }
	  // Deal with the last point, which closes the last row and area.
		row.printRaw();
		row.squeeze();
		area.add(row);
		slabs.add(area);
		// Print the summary for the last area.
		area.printArea(false);
  }
  
  /**
   * Scan an input line.
   * 
   * @return Slab depth point
   */
  private SlabPoint scanLine() {
  	double lat, lon, lower, center, upper;
  	
  	// Leave the longitude in the 0-360 degree format because the date 
  	// line is in the middle of slab areas.
  	lon = scan.nextDouble();
  	// Convert latitude to colatitude to make the access rounding 
  	// consistent.
  	lat = 90d - scan.nextDouble();
  	// Note that the depths in the file are all negative.
  	/**
  	 * The center is where the earthquakes are.  The lower bound (smaller 
  	 * depth) is shallower.  The upper bound (larger depth) is deeper.
  	 */
		center = scan.nextDouble();
  	lower = scan.nextDouble();
  	upper = scan.nextDouble();
  	return new SlabPoint(lat, lon, center, lower, upper);
  }

  /**
   * Function to return the cratons managed by AuxLocRef.
   *
   * @return Returns a Cratons Object containing the cratons
   */
  public Cratons getCratons() {
    return cratons;
  }

  /**
   * Function to return the zone statistics managed by AuxLocRef.
   *
   * @return A ZoneStats object containing the zone statistics
   */
  public ZoneStats getZoneStats() {
    return zoneStats;
  }
}
