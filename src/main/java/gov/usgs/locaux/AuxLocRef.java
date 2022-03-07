package gov.usgs.locaux;

import gov.usgs.traveltime.FileChanged;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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

  /** A ZoneStats object containing the classic earthquake statistics by geographic location. */
  private final ZoneStats zoneStats;

  /** A new ZoneStats object built the new updated JSON formatted file. */
  private NewZoneStats newZoneStats = null;

  /**
   * An integer containing the number of years read from the earthquake statistics zonestats file.
   */
  private int numberOfYears = -1;

  /** A String object containing the path to the slab models, null to use default. */
  private String modelPath = null;

  /** A String containing the serialized path for the locator, null to use default. */
  private String serializedPath = null;

  /** Default path for model files. */
  public static final String DEFAULT_MODEL_PATH = "./models/";

  /** A String containing the serialized file name. */
  private String serializedFileName = "locaux.ser";

  /** An array of String objects containing the raw input model file names. */
  private String[] modelFileNames = {
    "cratons.txt", "zonekey.dat", "zonestat.dat", "ZoneStats.json"
  };

  /**
   * A Scanner object used to read continental craton boundaries and earthquake statistics by
   * geographic location.
   */
  private Scanner scan;

  /** Private logging object. */
  private static final Logger LOGGER = LogManager.getLogger(AuxLocRef.class.getName());

  /**
   * Read the cratons and zone statistics files and make the data available to the Locator.
   *
   * @param modelPath A string containing the path to model files, null to use default
   * @param modelPath A string containing the path to the serialized files, null to use default
   * @throws IOException On any read error
   * @throws ClassNotFoundException In input serialization is hosed
   */
  public AuxLocRef(String modelPath, String serializedPath)
      throws IOException, ClassNotFoundException {
    int[][] zoneKeys;
    String[] absNames;
    ZoneStat[] stats;
    BufferedInputStream inCratons;
    BufferedReader inNewZoneStats;
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

    if (serializedPath == null) {
      this.serializedPath = DEFAULT_MODEL_PATH;
    } else {
      this.serializedPath = serializedPath;
    }

    // Create absolute path names.
    absNames = new String[modelFileNames.length];
    for (int j = 0; j < modelFileNames.length; j++) {
      absNames[j] = modelPath + modelFileNames[j];
    }

    // If any of the raw input files have changed, regenerate the
    // serialized file.
    if (FileChanged.isChanged(serializedPath + serializedFileName, absNames)) {
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

      // Open and read the new zone statistics file.
      inNewZoneStats = new BufferedReader(new FileReader(absNames[3]));

      newZoneStats = readNewZoneStats(inNewZoneStats);

      // Write out the serialized file.
      LOGGER.debug("Recreate the serialized file.");
      serOut = new FileOutputStream(serializedPath + serializedFileName);
      objOut = new ObjectOutputStream(serOut);

      // Wait for an exclusive lock for writing.
      lock = serOut.getChannel().lock();
      LOGGER.debug(
          "AuxLocRef write lock: valid = " + lock.isValid() + " shared = " + lock.isShared());

      /*
       * The auxiliary data can be read and written very quickly, so for persistent
       * applications such as the travel time or location server, serialization is
       * not necessary.  However, if the travel times are needed for applications
       * that start and stop frequently, the serialization should save some set up
       * time.
       */
      objOut.writeObject(cratons);
      objOut.writeObject(zoneStats);
      objOut.writeObject(newZoneStats);

      if (lock.isValid()) {
        lock.release();
      }

      // close files
      objOut.close();
      serOut.close();
    } else {
      // Read in the serialized file.
      LOGGER.debug("Read the serialized file.");
      serIn = new FileInputStream(serializedPath + serializedFileName);
      objIn = new ObjectInputStream(serIn);

      // Wait for a shared lock for reading.
      lock = serIn.getChannel().lock(0, Long.MAX_VALUE, true);
      LOGGER.debug(
          "AuxLocRef read lock: valid = " + lock.isValid() + " shared = " + lock.isShared());

      // load the cratons and zoneStats
      cratons = (Cratons) objIn.readObject();
      zoneStats = (ZoneStats) objIn.readObject();
      newZoneStats = (NewZoneStats) objIn.readObject();

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
   * @param inKeys A RandomAccessFile object containing the file handle for the zone keys file
   * @return A two dimensional array of integers containing the zone keys
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
   * @param inZones A RandomAccessFile object containing the file handle for the zone statistics
   *     file
   * @return An array of ZoneStat objects conatining the zone statistics
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
    byteBuf.position(40); // WHY

    // Create the zoneStats array by reading the zonestats information
    // skipping the fields we don't need
    ZoneStat[] stats = new ZoneStat[zoneStats.size() + 1];
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
      } else {
        stats[j] = null;
      }
    }

    return stats;
  }

  /**
   * Function to read in the new zone statistics file. Note that the JSON file may contain other
   * information that isn't relevant to the Locator. Only the parts of interest are picked out.
   *
   * @param inNewZoneStats A BufferedReader object containing the file handle
   * @return A NewZoneStats object containing the new ZoneStats depth data sorted into internal
   *     storage
   * @throws IOException On a read error
   */
  private NewZoneStats readNewZoneStats(BufferedReader inNewZoneStats) throws IOException {
    int len, numLats, numLons, row, column, count;
    double lat, lon, latSpacing, lonSpacing, latSpacingKm, circum, depth, depthError;
    char[] charArray;
    double[] lats;
    StringBuffer jsonString;
    NewZoneStats newZoneStats = null;
    JSONObject zoneStatJSON = null, jsonLatRow, jsonLonStats, jsonAves, jsonSample;
    JSONArray jsonArray = null, jsonLons;
    JSONParser parser;
    Iterator<?> iterLats, iterLatRows, iterLons;

    // Read the JSON file.
    charArray = new char[5000];
    jsonString = new StringBuffer();
    do {
      len = inNewZoneStats.read(charArray, 0, charArray.length);
      if (len > 0) {
        jsonString.append(charArray, 0, len);
      }
    } while (len > 0);

    try {
      // Parse the JSON string.
      parser = new JSONParser();
      zoneStatJSON = (JSONObject) parser.parse(jsonString.substring(0));

      // Pick out the list of possible latitudes.
      jsonArray = (JSONArray) zoneStatJSON.get("Possible Latitudes");

      // Copy the latitude data into a local array.
      numLats = jsonArray.size();
      lats = new double[numLats];
      iterLats = jsonArray.iterator();
      row = 0;

      while (iterLats.hasNext()) {
        lats[row++] = (Double) iterLats.next();
      }

      // Set up the new ZoneStats storage.
      latSpacing = (lats[numLats - 1] - lats[0]) / (numLats - 1);
      newZoneStats = new NewZoneStats(lats[numLats - 1], lats[0], latSpacing, numLats);

      // Debug print.
      LOGGER.trace(
          String.format(
              "FirstRowLat = %8.4f lastRowLat = %8.4f Lat Spacing = %7.4f "
                  + "Number of Lats = %d\n",
              lats[numLats - 1], lats[0], latSpacing, numLats));

      // Initialize all latitude rows in the new ZoneStats storage (including those
      // with no data).  This requires reproducing Will's grid calculation.
      latSpacingKm = latSpacing * LocUtil.DEG2KM;
      for (row = 0; row < numLats; row++) {
        circum = 2d * Math.PI * Math.abs(6371d * Math.cos(Math.toRadians(lats[row])));
        numLons = (int) (circum / latSpacingKm + 0.5d);
        lonSpacing = 360d / numLons;
        newZoneStats.initRow(numLats - row - 1, lats[row], lonSpacing, numLons);
      }

      // Pick apart the sample point statistics.  First make an array of latitude row data.
      jsonArray = (JSONArray) zoneStatJSON.get("ZoneStats");
      iterLatRows = jsonArray.iterator();
      while (iterLatRows.hasNext()) {
        // Get the next latitude row from the array.
        jsonLatRow = (JSONObject) iterLatRows.next();
        lat = LocUtil.getJSONDouble(jsonLatRow, "Latitude");
        lonSpacing = LocUtil.getJSONDouble(jsonLatRow, "Spacing (deg)");
        numLons = LocUtil.getJSONInt(jsonLatRow, "Total Columns");
        row = numLats - (int) ((lat - lats[0]) / latSpacing + 0.5d) - 1;

        // Debug print.
        LOGGER.trace(
            String.format(
                "LatRow: Lat = %8.4f Row = %3d Lon Spacing = %6.4f Number of "
                    + "Lons = %d coLat = %8.4f row' = %d",
                lat, numLats - row - 1, lonSpacing, numLons, 90d - lat, row));

        // Trap rows where Will & Ray differ.  Debug stuff!
        if (numLons != newZoneStats.getNumLons(row)
            || Math.abs(lonSpacing - newZoneStats.getLonSpacing(row)) > 1e-5d
            || Math.abs(90d - lat - newZoneStats.getLat(row)) > 1e-5d) {
          LOGGER.trace(
              String.format(
                  "Ray's spacing: Lat = %8.4f Lon Spacing = %6.4f Number of Lons = %d\n",
                  newZoneStats.getLat(row),
                  newZoneStats.getLonSpacing(row),
                  newZoneStats.getNumLons(row)));
        }

        // Get the array of longitudes.
        jsonLons = (JSONArray) jsonLatRow.get("Longitudes");
        iterLons = jsonLons.iterator();

        while (iterLons.hasNext()) {
          // Get the next longitude sample.
          jsonLonStats = (JSONObject) iterLons.next();
          column = LocUtil.getJSONInt(jsonLonStats, "Column Number");
          lon = LocUtil.getJSONDouble(jsonLonStats, "Longitude");

          // Debug print.
          LOGGER.trace(
              String.format(
                  "LonSample: Lon = %9.4f column = %d coLon = %9.4f\n",
                  lon, column, (lon >= 0d) ? lon : 360d + lon));

          if (lon < 0d) lon = 360d + lon;

          // Get the averaging statistics.
          jsonAves = (JSONObject) jsonLonStats.get("Depth Stats");

          // Get the 100 km average statistics.
          jsonSample = (JSONObject) jsonAves.get("100km");
          count = LocUtil.getJSONInt(jsonSample, "Count");

          // If we have at least two earthquakes, keep it.
          if (count > 1) {
            depth = LocUtil.getJSONDouble(jsonSample, "Mean");
            depthError = LocUtil.getJSONDouble(jsonSample, "STD");
            newZoneStats.putSample(row, column, new NewZonePoint(lon, count, depth, depthError));

            // Debug print.
            LOGGER.trace(
                String.format(
                    "\t\t100km: Count = %4d Depth = %6.2f Depth Error = %6.2f\n",
                    count, depth, depthError));
          }
        }
      }

    } catch (ParseException e) {
      LOGGER.info("NewZoneStats JSON parse failed!");
      e.printStackTrace();
      System.exit(200);
    }
    return newZoneStats;
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

  /**
   * Function to return the new zone statistics managed by AuxLocRef.
   *
   * @return A NewZoneStats object containing the new zone statistics
   */
  public NewZoneStats getNewZoneStats() {
    return newZoneStats;
  }
}
