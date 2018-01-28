package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Scanner;

/**
 * Read in auxiliary data to support the Locator.  Note that this 
 * data can never change and will be common to all events subsequently 
 * located.
 * 
 * @author Ray Buland
 *
 */
public class AuxLocRef {
	final Cratons cratons;			// Continental craton boundaries
	final ZoneStats zoneStats;	// Earthquake statistics by geographic location
	final String cratonsPath = "../../Documents/Work/Models/cratons.txt";
	final String zoneKeyPath = "../../Documents/Work/Models/zonkey.dat";
	final String zoneStatPath = "../../Documents/Work/Models/zonstt.dat";
	int noYears = -1;
	Scanner scan;
	
	/**
	 * Read the cratons and zone statistics files and make the data available 
	 * to the Loctor.
	 * 
	 * @throws IOException On any read error
	 */
	public AuxLocRef() throws IOException {
		int[][] zoneKeys;
		ZoneStat[] stats;
		BufferedInputStream inCratons;
		RandomAccessFile inZones;
		
		// Open and read the cratons file.
		inCratons = new BufferedInputStream(new FileInputStream(cratonsPath));
		scan = new Scanner(inCratons);
		cratons = new Cratons();
		while(scan.hasNext()) {
			readCraton();
		}
		scan.close();
		inCratons.close();
//	cratons.printCratons();
		
		// Open and read the zone key file.
		inZones = new RandomAccessFile(zoneKeyPath, "r");
		zoneKeys = readZoneKeys(inZones);
		zoneStats = new ZoneStats(zoneKeys);
		inZones.close();
		
		// Open and read the zone statistics file.
		inZones = new RandomAccessFile(zoneStatPath, "r");
		stats = readZoneStats(inZones);
		zoneStats.addStats(noYears, stats);
		inZones.close();
		
		// Set up the zone statistics.
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
		noYears = byteBuf.getInt();
//	System.out.println("ZoneStats years = "+noYears);
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
	//		System.out.format("\t%3d %4.1f %4.1f %4.1f %5.1f %5.1f %4.1f %5.1f %3d %4.1f\n", 
	//				ndeg, peryr, maxmag, minmag, minDepth, maxDepth, pctfre, meanDepth, dpmode, 
	//				pctge);
			}
			else stats[j] = null;
		}
		return stats;
	}
}
