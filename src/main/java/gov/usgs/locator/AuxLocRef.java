package gov.usgs.locator;

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

/**
 * The AuxLocRef class manages auxiliary data files that support the Locator 
 * such as the continental craton boundries and earthquake statistics by 
 * geographic location files.  Note that this data is static (does not change
 * during a location run) and will be common to all events subsequently located.
 * 
 * @author Ray Buland
 *
 */
public class AuxLocRef {
  /** 
   * A Cratons object containing the continental craton boundaries.
   */
  private final Cratons cratons;

  /**
   * A ZoneStats object containing earthquake statistics by geographic location.
   **/ 
  private final ZoneStats zoneStats;

  /** 
   * An integer containing the number of years read from the earthquake 
   * statistics zonestats file.
   */
  private int numberOfYears = -1;
  
  /**
	 * Default path for model files.
	 */
  public static final String DEFAULT_MODEL_PATH = "./models/";

	/**
	 * Path for model files.
	 */
	public String modelPath;      

  /**
   * An array of String objets containing the raw input model file names.
   */
  private String[] modelFileNames = {"cratons.txt", "zonekey.dat", "zonestat.dat"};

	/**
	 * Read the cratons and zone statistics files and make the data available 
	 * to the Locator.
	 * 
	 * @param modelPath If not null, path to model files
	 * 
	 * @throws IOException On any read error
	 * @throws ClassNotFoundException In input serialization is hosed
	 */
	public AuxLocRef(String modelPath) throws IOException, ClassNotFoundException {
//	long time;
		int[][] zoneKeys;
		String[] absNames;
		ZoneStat[] stats;
		BufferedInputStream inCratons;
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
    for(int j=0; j<modelFileNames.length; j++) {
      absNames[j] = modelPath+modelFileNames[j];
    }
    
    // If any of the raw input files have changed, regenerate the 
    // serialized file.
    if(FileChanged.isChanged(modelPath+serializedFileName, absNames)) {
      // Open and read the cratons file.
      inCratons = new BufferedInputStream(new FileInputStream(absNames[0]));
      scan = new Scanner(inCratons);
      cratons = new Cratons();
      while(scan.hasNext()) {
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
      
      // Write out the serialized file.
      serOut = new FileOutputStream(modelPath+serializedFileName);
      objOut = new ObjectOutputStream(serOut);
      // Wait for an exclusive lock for writing.
      lock = serOut.getChannel().lock();
      if(LocUtil.deBugLevel > 0) {
        System.out.println("AuxLocRef write lock: valid = "+lock.isValid()+
            " shared = "+lock.isShared());
      }
      /*
       * The auxiliary data can be read and written very quickly, so for persistent 
       * applications such as the travel time or location server, serialization is 
       * not necessary.  However, if the travel times are needed for applications 
       * that start and stop frequently, the serialization should save some set up 
       * time.
       */
      objOut.writeObject(cratons);
      objOut.writeObject(zoneStats);
      if(lock.isValid()) lock.release();
      objOut.close();
      serOut.close();
    } else {
      // Read in the serialized file.
      serIn = new FileInputStream(modelPath+serializedFileName);
      objIn = new ObjectInputStream(serIn);
      // Wait for a shared lock for reading.
      lock = serIn.getChannel().lock(0, Long.MAX_VALUE, true);
      if(LocUtil.deBugLevel > 0) {
        System.out.println("AuxLocRef read lock: valid = "+lock.isValid()+
            " shared = "+lock.isShared());
      }
      cratons = (Cratons)objIn.readObject();
      zoneStats = (ZoneStats)objIn.readObject();
      if(lock.isValid()) lock.release();
      objIn.close();
      serIn.close();
    }
  }
  
  /**
   * Read the craton file.
   */
  private void readCraton() {
    String region;
    double[] lats, lons;
    
    region = scan.next();
    while(!scan.hasNextInt()) {
      region = region+" "+scan.next();
    }
    int len = scan.nextInt();
    lats = new double[len];
    lons = new double[len];
    
    for(int j=0; j<len; j++) {
      lats[j] = scan.nextDouble();
      lons[j] = scan.nextDouble();
    }
    cratons.add(region, lats, lons);
  }
  
  /**
   * Read the zone keys file.  Because the earthquake statistics are sparse 
   * with respect to Marsden squares, it was convenient to index them 
   * through a separate file.  Note that the zone keys array is organized by 
   * longitude (Greenwich to Greenwich heading east) and co-latitude (north 
   * pole to south pole going south).
   * 
   * @param inZones File handle for the zone keys file
   * @return Zone keys
   * @throws IOException On any read error
   */
  private int[][] readZoneKeys(RandomAccessFile inZones) throws IOException {
    int length;
    byte[] byteArray;
    int[][] zoneKeys;
    ByteBuffer byteBuf;
    IntBuffer intBuf;
    
    // Read the file.
    length = (int)inZones.length();
    byteArray = new byte[length];
    inZones.read(byteArray);
    byteBuf = ByteBuffer.wrap(byteArray);
    byteBuf.order(ByteOrder.LITTLE_ENDIAN);
    intBuf = byteBuf.asIntBuffer();
    
    // Create the zoneKeys array.
    zoneKeys = new int[360][180];
    int k=0;
    for(int i=0; i<180; i++) {
      for(int j=0; j<360; j++) {
        // Note that the key is off by 2 because of Fortran indexing and the 
        // special first record in the statistics file.
        zoneKeys[j][i] = intBuf.get(k++)-2;
      }
    }
    return zoneKeys;
  }
  
  /**
   * Read in the zone statistics file.  Note that only the small part of the 
   * statistics relevant to the Locator is currently retained.
   * 
   * @param inZones File handle for the zone statistics file
   * @return Zone statistics
   * @throws IOException On any read error
   */
  private ZoneStat[] readZoneStats(RandomAccessFile inZones) throws IOException {
    @SuppressWarnings("unused")
    int length, ndeg, dpmode;
    @SuppressWarnings("unused")
    float peryr, maxmag, minmag, pctfre, pctge;
    double meanDepth, minDepth, maxDepth;
    byte[] byteArray;
    ByteBuffer byteBuf;
    ZoneStat[] stats;
    
    // Read the file.
    length = (int)inZones.length();
    byteArray = new byte[length];
    inZones.read(byteArray);
    byteBuf = ByteBuffer.wrap(byteArray);
    byteBuf.order(ByteOrder.LITTLE_ENDIAN);
    numberOfYears = byteBuf.getInt();
//  System.out.println("ZoneStats years = "+numberOfYears);
    byteBuf.position(40);
    
    // Create the zoneStats array.
    stats = new ZoneStat[zoneStats.size()];
    for(int j=0; j<stats.length; j++) {
      ndeg = byteBuf.getInt();
      peryr = byteBuf.getFloat();
      maxmag = byteBuf.getFloat();
      minmag = byteBuf.getFloat();
      minDepth = byteBuf.getFloat();
      maxDepth = byteBuf.getFloat();
      pctfre = byteBuf.getFloat();
      meanDepth = byteBuf.getFloat();
      dpmode = byteBuf.getInt();
      pctge = byteBuf.getFloat();
      if(pctfre > 0. && minDepth < 900d) {
        stats[j] = new ZoneStat(meanDepth, minDepth, maxDepth);
  //    System.out.format("\t%3d %4.1f %4.1f %4.1f %5.1f %5.1f %4.1f %5.1f %3d %4.1f\n", 
  //        ndeg, peryr, maxmag, minmag, minDepth, maxDepth, pctfre, meanDepth, dpmode, 
  //        pctge);
      }
      else stats[j] = null;
    }
    return stats;
  }
}
