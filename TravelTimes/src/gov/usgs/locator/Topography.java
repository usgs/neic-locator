package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Handle corrections for reflections from the free surface.  
 * These may be either positive (i.e., reflecting under a 
 * mountain) or negative (i.e., reflecting from the bottom of 
 * the ocean).  The pwP time (relative to pP) is also computed.
 * 
 * @author Ray Buland
 *
 */
public class Topography {
	short[][] topo;						// Global topography on a 20" grid in kilometers
	final String topoFile = "../../../Documents/Work/Models/ETOPObase.smth";
	TopoLons topoLons;				// Virtual array of longitude sample points
	TopoLats topoLats;				// Virtual array of latitude sample points
	
	/**
	 * The constructor reads in the topography file.
	 * 
	 * @throws IOException On any I/O error or data mismatch
	 */
	public Topography() throws IOException {
		// Read the topography data.
		readTopo();
		// Set up the virtual arrays of latitude and longitude sample points.
		topoLons = new TopoLons();
		topoLats = new TopoLats();
	}
	
	/**
	 * Read the topography file.
	 * 
	 * @throws IOException On any I/O error or data mismatch
	 */
	private void readTopo() throws IOException {
			byte[] byteArray;
			int bytesRead, recLen = 0, recLast;
			int iData, nLon, nLat;
			double dLatdLon, lonMin, dLon, lonMax, latMin, dLat, latMax;
			ByteBuffer byteBuf;
			BufferedInputStream in;
			
			// Set up the byte buffer.
			byteArray = new byte[2164];
			byteBuf = ByteBuffer.wrap(byteArray);
			byteBuf.order(ByteOrder.LITTLE_ENDIAN);
			ShortBuffer shorts = byteBuf.asShortBuffer();
			
			// Open the topo file.
			in = new BufferedInputStream(new FileInputStream(topoFile));
			
			// Read the record header.
			bytesRead = in.read(byteArray, 0, 4);
			if(bytesRead == 4) {
				recLen = byteBuf.getInt();
			} else {
				System.out.println("Unable to read header record length.");
				in.close();
				throw new IOException();
			}
			// Read the header record.
			byteBuf.clear();
			bytesRead = in.read(byteArray, 0, recLen+4);
			if(bytesRead >= recLen+4) {
				iData = byteBuf.getInt();
				nLon = byteBuf.getInt();
				nLat = byteBuf.getInt();
				dLatdLon = byteBuf.getFloat();
				lonMin = byteBuf.getFloat();
				dLon = byteBuf.getFloat();
				lonMax = byteBuf.getFloat();
				latMin = byteBuf.getFloat();
				dLat = byteBuf.getFloat();
				latMax = byteBuf.getFloat();
	//		System.out.format("Dims: %4d %4d %4d rat: %4.1f X: %9.4f %6.4f "+
	//				"%8.4f X: %8.4f %6.4f %7.4f\n", iData, nLon, nLat, dLatdLon, 
	//				lonMin, dLon, lonMax, latMin, dLat, latMax);
				// Check the record length.
				recLast = byteBuf.getInt();
				if(recLast != recLen) {
					System.out.println("Header record length mismatch.");
					in.close();
					throw new IOException();
				}
			} else {
				System.out.println("Insufficient data for header record.");
				in.close();
				throw new IOException();
			}
			
			// Allocate the topography storage.  Make the longitude two bigger 
			// to accommodate the wrap around at +/-180 degrees.
			topo = new short[nLon+2][nLat];
			
			// Loop over the latitudes.
			for(int j=0; j<nLat; j++) {
				// Get the record length.
				byteBuf.clear();
				bytesRead = in.read(byteArray, 0, 4);
				if(bytesRead == 4) {
					recLen = byteBuf.getInt();
				} else {
					System.out.println("Unable to read data record "+j+" length.");
					in.close();
					throw new IOException();
				}
				// Read the data record.
				byteBuf.clear();
				bytesRead = in.read(byteArray, 0, recLen+4);
				if(bytesRead == recLen+4 && recLen == nLon*2) {
					// Transfer the data.
					for(int i=0; i<nLon; i++) {
						topo[i+1][j] = shorts.get(i);
					}
					// Handle the wrap around.
					topo[0][j] = topo[nLon][j];
					topo[nLon+1][j] = topo[1][j];
		//		if(j%100 == 0) System.out.format("Rec %4d: %5d %5d\n", 
		//				j, topo[1][j], topo[nLon][j]);
					// Check the record length.
					byteBuf.position(recLen);
					recLast = byteBuf.getInt();
					if(recLast != recLen) {
						System.out.println("Data record "+j+" length mismatch.");
						in.close();
						throw new IOException();
					}
				} else {
					System.out.println("Insufficient data for data record "+j+".");
					in.close();
					throw new IOException();
				}
			}
			
			// Close the topography file.
			in.close();
	}
	
	/**
	 * Get the elevation at any point on Earth.  Note that under the 
	 * ocean, this will be minus the ocean depth.
	 * 
	 * @param latitude Geographic latitude in degrees
	 * @param longitude Longitude in degrees
	 * @return Elevation in kilometers
	 */
	public double getElev(double latitude, double longitude) {
		// The data is stored as meters of elevation in short integers.
		return 0.001d*TauUtil.biLinear(longitude, latitude, topoLons, 
				topoLats, topo);
	}
}
