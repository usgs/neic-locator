package gov.usgs.locator;

import gov.usgs.locaux.AuxLocRef;
import gov.usgs.locaux.LocUtil;
import gov.usgs.locaux.NewZoneStats;
import gov.usgs.locaux.SlabArea;
import gov.usgs.locaux.SlabPoint;
import gov.usgs.locaux.SlabRow;
import gov.usgs.locaux.Slabs;
import gov.usgs.locaux.TiltedArea;
import gov.usgs.locaux.ZoneStats;
import gov.usgs.traveltime.FileChanged;
import gov.usgs.traveltime.TTSessionLocal;
import gov.usgs.traveltime.TauUtil;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manage the external Locator files. This includes changing the slab model resolution on demand.
 *
 * @author Ray Buland
 */
public class LocSessionLocal {
  /** An array of String objects containing the raw slab model file names. */
  private String[] modelFileNames = {"slabmaster.txt", "slabtilted.txt"};

  /** A String object containing the name of the last slab model used */
  private String lastSlabRes = "";

  /** A String object containing the path to the slab models, null to use default. */
  private String modelPath = null;

  /** A String containing the serialized path for the locator, null to use default. */
  private String serializedPath = null;

  /** Default path for model files. */
  public static final String DEFAULT_MODEL_PATH = "./models/";

  /** A Tree map containing the Locate instance storage. */
  private TreeMap<String, Locate> locByRes;

  /** An AuxLocRef object containing the invariant external file data. */
  private AuxLocRef auxLoc;

  /** A Locate object containing the last locate instance */
  private Locate lastLocate;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(LocSessionLocal.class.getName());

  /**
   * Read in the invariant external Locator files.
   *
   * @param modelPath Path to the Locator model files
   * @throws ClassNotFoundException If the serialized file objects don't exist
   * @throws IOException If the serialized I/O fails in any way
   */
  public LocSessionLocal(String modelPath, String serializedPath)
      throws ClassNotFoundException, IOException {
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

    // Read in the invariant external file data.
    auxLoc = new AuxLocRef(modelPath, serializedPath);
  }

  /**
   * Get a Locate instantiation with the required slab model resolution.
   *
   * @param event Event object
   * @param ttLocal Travel-time manager object
   * @param slabRes String specifying the slab model resolution in samples per degree
   * @return Locate object
   * @throws ClassNotFoundException If the serialized slab master object doesn't exist
   * @throws IOException If the serialized I/O fails in any way
   */
  public Locate getLocate(Event event, TTSessionLocal ttLocal, String slabRes)
      throws ClassNotFoundException, IOException {
    // If the slab resolution is the same as last time, we're done.
    if (!slabRes.contentEquals(lastSlabRes)) {
      // Otherwise, fetch the proper Locate instantiation.
      lastSlabRes = slabRes;

      // If necessary, initialize the Locate instance storage.
      if (locByRes == null) {
        locByRes = new TreeMap<String, Locate>();
      }

      // Fetch the saved Locate instance.
      Locate locate = locByRes.get(slabRes);

      // If there isn't one, get the required slab resolution, create a new
      // Locate object with that resolution, and save it for next time.
      if (locate == null) {
        Slabs slabStats = getSlabRes(slabRes);

        locate = new Locate(event, ttLocal, auxLoc, slabStats);
        locByRes.put(slabRes, locate);
      }

      lastLocate = locate;
    }

    return lastLocate;
  }

  /**
   * Get the desired slab model resolution. This generally requires a serialization from a file. If
   * the serialization files doesn't exist, the raw input file is read and serialized out for next
   * time.
   *
   * @param slabRes String specifying the slab model resolution in samples per degree
   * @return A slabs object containing all the slab model data for the requested resolution
   * @throws IOException If the serialized I/O fails in any way
   * @throws ClassNotFoundException If the serialized slab master object doesn't exist
   */
  public Slabs getSlabRes(String slabRes) throws IOException, ClassNotFoundException {
    Slabs slabs;
    String serializedFileName;
    String[] absNames;
    BufferedInputStream inSlabs;
    FileInputStream serIn;
    FileOutputStream serOut;
    ObjectInputStream objIn;
    ObjectOutputStream objOut;
    FileLock lock;

    // Construct path names to the slab files.
    absNames = new String[modelFileNames.length];
    for (int j = 0; j < modelFileNames.length; j++) {
      absNames[j] = modelPath + modelFileNames[j];
    }
    // Fiddle the slab master file to get the required resolution.
    absNames[0] = absNames[0].substring(0, absNames[0].indexOf(".txt")) + slabRes + ".txt";
    LOGGER.info("Slab file: " + absNames[0]);

    // If any of the raw input files have changed, regenerate the serialized file.
    serializedFileName = "slab" + slabRes + ".ser";
    if (FileChanged.isChanged(serializedPath + serializedFileName, absNames)) {

      // Open and read the master slab geometry model file.
      inSlabs = new BufferedInputStream(new FileInputStream(absNames[0]));
      slabs = new Slabs();
      readSlabs(inSlabs, slabs);
      inSlabs.close();

      // Open and read the tilted slab geometry model file.
      inSlabs = new BufferedInputStream(new FileInputStream(absNames[1]));
      readTilted(inSlabs, slabs);
      inSlabs.close();

      // Write out the serialized file.
      LOGGER.fine("Recreate the serialized file.");
      serOut = new FileOutputStream(serializedPath + serializedFileName);
      objOut = new ObjectOutputStream(serOut);

      // Wait for an exclusive lock for writing.
      lock = serOut.getChannel().lock();
      LOGGER.fine(
          "LocSessionLocal write lock: valid = " + lock.isValid() + " shared = " + lock.isShared());

      /*
       * The auxiliary data can be read and written very quickly, so for persistent
       * applications such as the travel time or location server, serialization is
       * not necessary.  However, if the travel times are needed for applications
       * that start and stop frequently, the serialization should save some set up
       * time.
       */
      objOut.writeObject(slabs);

      if (lock.isValid()) {
        lock.release();
      }

      // close files
      objOut.close();
      serOut.close();
    } else {
      // Read in the serialized file.
      LOGGER.fine("Read the serialized file.");
      serIn = new FileInputStream(serializedPath + serializedFileName);
      objIn = new ObjectInputStream(serIn);

      // Wait for a shared lock for reading.
      lock = serIn.getChannel().lock(0, Long.MAX_VALUE, true);
      LOGGER.fine(
          "LocSessionLocal read lock: valid = " + lock.isValid() + " shared = " + lock.isShared());

      // load the cratons and zoneStats
      slabs = (Slabs) objIn.readObject();

      if (lock.isValid()) {
        lock.release();
      }

      // close files
      objIn.close();
      serIn.close();
    }

    return slabs;
  }

  /**
   * Read in the master slab model. Note that each area is done in a rectangular grid, which is
   * sparse (the points outside the slab are flagged by NaNs). The algorithm simple accumulates
   * rows, squeezing the NaNs out of each row, while looking for the start of the next area.
   */
  private void readSlabs(BufferedInputStream inSlabs, Slabs slabs) {
    boolean first = true;
    double firstLon, lastLon, slabInc = Double.NaN;
    SlabArea area = null;
    SlabRow row;
    SlabPoint point;
    Scanner scan;

    // Set up a scanner for the text file.
    scan = new Scanner(inSlabs);

    // Initialize the first row.
    row = new SlabRow();

    // Read in the first point.
    point = scanLine(scan);
    row.add(point);

    // Remember where we started so we will know when the area is done.
    firstLon = point.getLon();
    lastLon = firstLon;

    // As long as there's still data, keep on trucking.
    while (scan.hasNextDouble()) {
      point = scanLine(scan);

      // We need the latitude-longitude grid spacing first
      if (Double.isNaN(slabs.getSlabInc())) {
        slabInc = Math.abs(firstLon - point.getLon());
        slabs.setSlabInc(slabInc);
        area = new SlabArea(slabInc);
        LOGGER.info("Increment set: " + String.valueOf(slabInc));
      }

      // Look for the end of a latitude row.
      if (Math.abs(point.getLon() - lastLon) > slabInc + TauUtil.DTOL) {
        // Print out the first and last points in each area.
        if (LOGGER.getLevel() == Level.FINE) {
          if (first || Math.abs(point.getLon() - firstLon) > TauUtil.DTOL) {
            LOGGER.fine(row.toString());
            row.printRaw();

            if (first) {
              first = false;
            } else {
              first = true;
            }
          }
        }

        // Squeeze out the NaNs and save the remaining data in segments.
        row.squeeze(slabInc);

        // Look for the start of a new area.
        if (Math.abs(point.getLon() - firstLon) > TauUtil.DTOL) {
          // Add the last row.
          area.add(row);

          // Print a summary of the last area.
          LOGGER.fine(area.toString());
          area.printArea(false);

          // Add the last area to slab storage.
          slabs.add(area);

          // Start a new area.
          area = new SlabArea(slabInc);
          row = new SlabRow();
          firstLon = point.getLon();
        } else {
          // Add the row to the current area.
          area.add(row);

          // Start a new row.
          row = new SlabRow();
        }
      }
      // Add the current point to the current row.
      row.add(point);
      lastLon = point.getLon();
    }

    // Deal with the last point, which closes the last row and area.
    LOGGER.fine(row.toString());
    row.printRaw();
    row.squeeze(slabInc);
    area.add(row);
    slabs.add(area);

    // Print the summary for the last area.
    LOGGER.fine(area.toString());
    area.printArea(false);
  }

  /**
   * Read the tilted slab file and append a summary to the end of the master slab areas. Note that
   * the tilted slabs file is huge and very dense. Instead of sorting it out, we read the whole
   * thing in, create a latitude-longitude grid, sort the samples into the grid elements, and
   * extract the handful of numbers we need.
   */
  private void readTilted(BufferedInputStream inSlabs, Slabs slabs) {
    double slabInc;
    SlabPoint point, firstPoint, lastPoint;
    TiltedArea area;
    Scanner scan;

    // Set up a scanner for the text file.
    scan = new Scanner(inSlabs);

    // Set the slab latitude-longitude grid spacing in degrees.
    slabInc = slabs.getSlabInc();

    // Set up the first area.
    area = new TiltedArea(slabInc);

    // Read in the first point.
    point = scanLine(scan);
    area.add(point);
    lastPoint = point;
    firstPoint = lastPoint;

    // As long as there's still data, keep on trucking.
    while (scan.hasNextDouble()) {
      point = scanLine(scan);

      if (Math.abs(point.getLat() - lastPoint.getLat()) > LocUtil.MINSLABINCREMENT
          && Math.abs(point.getLon() - lastPoint.getLon()) > LocUtil.MINSLABINCREMENT) {
        /*
         * New row.  In this case, the rows aren't very interesting because
         * they run at odd angles with odd spacings.  We still need to find
         * them though so we can detect new areas.
         */
        LOGGER.fine("Scan line: " + firstPoint + " - " + lastPoint);
        if (Math.abs(point.getLat() - firstPoint.getLat()) > LocUtil.TILTEDAREAINCREMENT
            || Math.abs(point.getLon() - firstPoint.getLon()) > LocUtil.TILTEDAREAINCREMENT) {
          // New area.  Process the data from the last area and start a new one.
          area.makeGrid();
          slabs.add(area);
          area = new TiltedArea(slabInc);
        }
        firstPoint = point;
      }
      // Don't mess with rows.  Just dump all points into the area pool.
      area.add(point);
      lastPoint = point;
    }

    // Clean up the last area.
    area.makeGrid();
    slabs.add(area);

    // Allow the garbage collector to collect the temporary storage.
    area = null;
  }

  /**
   * Scan an input line.
   *
   * @return Slab depth point
   */
  private SlabPoint scanLine(Scanner scan) {
    double lat, lon, lower, center, upper;

    // Leave the longitude in the 0-360 degree format because the date
    // line is in the middle of slab areas.
    lon = scan.nextDouble();

    // Convert latitude to colatitude to make the access rounding
    // consistent.
    lat = 90d - scan.nextDouble();
    // Note that the depths in the file are all negative.

    /**
     * The center is where the earthquakes are. The lower bound (smaller depth) is shallower. The
     * upper bound (larger depth) is deeper.
     */
    center = scan.nextDouble();
    lower = scan.nextDouble();
    upper = scan.nextDouble();

    return new SlabPoint(lat, lon, center, lower, upper);
  }

  /** @return A pointer to the ZoneStats class */
  public ZoneStats getZoneStats() {
    return auxLoc.getZoneStats();
  }

  /** @return A pointer to the NewZoneStats class */
  public NewZoneStats getNewZoneStats() {
    return auxLoc.getNewZoneStats();
  }
}
