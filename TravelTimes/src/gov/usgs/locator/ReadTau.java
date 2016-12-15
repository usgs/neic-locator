package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 
 * @author Ray
 *
 * ReadTau is temporary code to aid in the porting of the seismic travel-time 
 * package and the earthquake locator.  By reading the FORTRAN generated 
 * travel-time header and table information, it should facilitate testing and 
 * allow the travel-time port to a Java server to be completed in a third of 
 * the development time.
 */
public class ReadTau {
	// Global Fortran array limits.
	public final int JSRC = 250;	// Maximum slowness points in earthquake depth range.
	public final int JSEG = 30;		// Maximum number of travel-time segments.
	public final int JBRN = 120;	// Maximum number of travel-time branches.
	public final int JOUT = 2500;
	public final int JTSM = 500;
	// Set up the reader.
	final String modelPath = "../../Documents/Work/Models/";
	String modelName;
	final int bufLen = 100020;
	byte[] byteArray;
	ByteBuffer byteBuf;
	BufferedInputStream in;
	int[] numRec;

	/*
	 *    Header variables:
	 * 
	 *    Global parameters
	 * nasgr					Record size in the table file (recSizeUp)
	 * len2						Not used? (5 X nl)
	 * xn 						Distance normalization (xNorm)
	 * pn							Slowness normalization (pNorm)
	 * tn							Time normalization (tNorm)
	 * rum						Upper mantle radius (above the ~410 km discontinuity; km) (rUpperMantle)
	 * rmh						Moho radius (km) (rMoho)
	 * rcn						Conrad radius (km) (rConrad)
	 * 
	 *    Segment parameters (record 2)
	 * nseg						Number of travel-time segments (numSeg)
	 * fcs[nseg][3]		Number of traversals of each region (countReg)
	 * nafl[nseg][3]	Phase type per region (1=P, 2=S) (typeReg)
	 * indx[nseg][2]	Segment indices into pt, etc. (indexSeg)
	 * kndx[nseg][2]	Not used?
	 * 
	 *    Branch parameters (record 4)
	 * nbrn						Number of travel-time branches (numBrn)
	 * phcd[nbrn]			Branch phase codes (phCode)
	 * jndx[nbrn][2]	Branch indices into pt, etc. (indexBrn)
	 * px[nbrn][2]		Slowness range for branches (pBrn)
	 * xt[nbrn][2]		Distance ranges for branches (xBrn)
	 * 
	 *    Model parameters for upper 800 km (record 2)
	 * mt[2]					Upper limit for pm, zm, ndex (numMod)
	 * pm[2][mt+3]		Model slownesses (pMod)
	 * zm[2][mt+3]		Model depths (zMod)
	 * ndex[2][mt]		Record number in the table file (indexMod)
	 * 
	 *    Slownesses associated with up-going branches (record 3)
	 * ku[2]					Upper limit for pu and tauu (numTauUp)
	 * km[2]					Upper limit for pux and xu (numXUp)
	 * pu[2][ku+1]		Slowness sampling for up-going branches (pTauUp)
	 * pux[2][km+1]		Distances for up-going branch ends (pXUp)
	 * 
	 *    Tau(p), etc. for all branches (records 5 and 6)
	 * nl							Upper limit for pt, taut, coef (ptLen) (numAll)
	 * pt[nl+1]				Slowness sampling for all branches (pAll)
	 * taut[nl]				Base tau(pt) for all branches (tauAll)
	 * coef[nl][5]		Spline coefficients for taut(pt) (basisAll)
	 */
	String[] phcd;
	int nasgr, nl, nseg, nbrn = 0;
// int len2;
	int[] mt = null, ku = null, km = null;
	int[][] nafl, indx, ndex, jndx;
// int[][] kndx;
	static double xn, pn, tn;
	double rum, rmh, rcn;
	double[] pt, taut;
	double[][] fcs, pm, zm, pu, pux, px, xt, coef;
	
	/* 
	 * Table variables:
	 *
	 * buf(nasgr) -> tauu(ku) and xu(km)
	 * tauu[2][nrec][ku] 		Up-going tau(pt) for all branches (tauUp)
	 * xu[2][nrec][km]			up-going x(pt) for all branches (xUp)
	 */
	double[][][] tauUp, delUp;
	
	/**
	 * The constructor sets up the read array and ByteBuffer to interpret 
	 * the binary data in the FORTRAN files.
	 * 
	 * @param modelName The name of the Earth model to be read
	 */
	public ReadTau(String modelName) {
		// Remember the model.
		this.modelName = modelName;
		// Set up the buffer.
		byteArray = new byte[bufLen];
		byteBuf = ByteBuffer.wrap(byteArray);
		byteBuf.order(ByteOrder.LITTLE_ENDIAN);
	}
	
	/**
	 * Read the travel-time header file ("model_name".hed).  This file contains 
	 * travel-time branch information including the surface focus tau interpolation 
	 * for a wide variety of seismic phases.
	 * 
	 * @throws IOException
	 */
	public void readHeader() throws IOException {
		int bytesRead, recLen = 0, recLast;
		
		// Open the file.
		in = new BufferedInputStream(new FileInputStream(modelPath + 
				modelName + ".hed"));
		
		/*
		 * Read the first record.
		 */
//	System.out.println("Record 1:");
		bytesRead = in.read(byteArray, 0, byteArray.length);
//	System.out.println("Bytes read: " + bytesRead);
		if(bytesRead >= 4) {
			recLen = byteBuf.getInt();
//		System.out.println("RecLen = " + recLen);
		}
		if(bytesRead >= recLen+4) {
			// Read array limits and normalization constants: nasgr, nl, len2, 
			// xn, pn, tn, mt, nseg, nbrn, ku, km.
			nasgr = byteBuf.getInt();
			nl = byteBuf.getInt();
	//	This variable is never used in the following code, so just skip it.
	//	len2 = byteBuf.getInt();
			byteBuf.position(byteBuf.position()+Integer.BYTES);
			xn = byteBuf.getFloat();
			pn = byteBuf.getFloat();
			tn = byteBuf.getFloat();
			mt = new int[2];
			mt[0] = byteBuf.getInt();
			mt[1] = byteBuf.getInt();
			nseg = byteBuf.getInt();
			nbrn = byteBuf.getInt();
			ku = new int[2];
			ku[0] = byteBuf.getInt();
			ku[1] = byteBuf.getInt();
			km = new int[2];
			km[0] = byteBuf.getInt();
			km[1] = byteBuf.getInt();
			
			// Read segment level parameters, etc.: fcs, nafl, indx, kndx.
			fcs = new double[nseg][3];
			for(int i=0; i<3; i++) {
				for(int j=0; j<fcs.length; j++) {
					fcs[j][i] = byteBuf.getFloat();
				}
				byteBuf.position(byteBuf.position()+Float.BYTES*(JSEG-nseg));
			}
			
			nafl = new int[nseg][3];
			for(int i=0; i<3; i++) {
				for(int j=0; j<nafl.length; j++) {
					nafl[j][i] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-nseg));
			}
			
			indx = new int[nseg][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<indx.length; j++) {
					indx[j][i] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-nseg));
			}
			
	/*  This variable is apparently never used in the later code, so just skip it.
			kndx = new int[nseg][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<kndx.length; j++) {
					kndx[j][i] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-nseg));
			} */
			byteBuf.position(byteBuf.position()+2*Integer.BYTES*JSEG);
			
			// Read the radiuses of critical internal boundaries: rum, rmh, rcn.
			rum = byteBuf.getFloat();
			rmh = byteBuf.getFloat();
			rcn = byteBuf.getFloat();
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) {
			System.out.println("Mismatch - recLen recLast: " +recLen + " " + recLast);
			throw new IOException();
		}
		
		/*
		 * Read the second record.
		 */
//	System.out.println("\nRecord 2:");
		byteBuf.compact();
//	System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
//	System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
//		System.out.println("RecLen = " + recLast + " " + recLen + " " 
//			+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
			// Model parameters down to the deepest earthquake: pm, zm, ndex.
			pm = new double[2][];
			for(int i=0; i<2; i++) {
				pm[i] = new double[mt[i]+3];
				for(int j=0; j<pm[i].length; j++) {
					pm[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JSRC-mt[i]-3));
			}
			
			zm = new double[2][];
			for(int i=0; i<2; i++) {
				zm[i] = new double[mt[i]+3];
				for(int j=0; j<zm[i].length; j++) {
					zm[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JSRC-mt[i]-3));
			}
			
			ndex = new int[2][];
			for(int i=0; i<2; i++) {
				ndex[i] = new int[mt[i]];
				for(int j=0; j<ndex[i].length; j++) {
					ndex[i][j] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSRC-mt[i]));
			}
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) {
			System.out.println("Mismatch - recLen recLast: " + recLen + " " + recLast);
			throw new IOException();
		}
		
		/*
		 * Read the third record.
		 */
//	System.out.println("\nRecord 3:");
		byteBuf.compact();
//	System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
//	System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
//		System.out.println("RecLen = " + recLast + " " + recLen + " " 
//			+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
			// Slownesses associated with the up-going branch information in the 
			// table file: pu, pux.
			pu = new double[2][];
			for(int i=0; i<2; i++) {
				pu[i] = new double[ku[i]+1];
				for(int j=0; j<pu[i].length; j++) {
					pu[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JTSM-ku[i]));
			}
			
			pux = new double[2][];
			for(int i=0; i<2; i++) {
				pux[i] = new double[km[i]+1];
				for(int j=0; j<pux[i].length; j++) {
					pux[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-km[i]-1));
			}
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) {
			System.out.println("Mismatch - recLen recLast: " + recLen + " " + recLast);
			throw new IOException();
		}
		
		/*
		 * Read the forth record.
		 */
//	System.out.println("\nRecord 4:");
		byteBuf.compact();
//	System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
//	System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
//		System.out.println("RecLen = " + recLast + " " + recLen + " " 
//			+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
			// Branch level information: phcd, px, xt, jndx.
			byte[] temp = new byte[8];
			phcd = new String[nbrn];
			for(int j=0; j<phcd.length; j++) {
				byteBuf.get(temp);
				phcd[j] = new String(temp);
			}
			byteBuf.position(byteBuf.position()+temp.length*(JBRN-nbrn));
			
			px = new double[nbrn][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<px.length; j++) {
					px[j][i] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-nbrn));
			}
			
			xt = new double[nbrn][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<xt.length; j++) {
					xt[j][i] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-nbrn));
			}
			
			jndx = new int[nbrn][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<jndx.length; j++) {
					jndx[j][i] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JBRN-nbrn));
			}
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) {
			System.out.println("Mismatch - recLen recLast: " + recLen + " " + recLast);
			throw new IOException();
		}
		
		/*
		 * Read the fifth record.
		 */
//	System.out.println("\nRecord 5:");
		byteBuf.compact();
//	System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
//	System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
//		System.out.println("RecLen = " + recLast + " " + recLen + " " 
//			+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
			// Slowness and tau for all branches: pt, taut.
			pt = new double[nl+1];
			for(int j=0; j<pt.length; j++) {
				pt[j] = byteBuf.getDouble();
			}
			byteBuf.position(byteBuf.position()+Double.BYTES*(JOUT-nl-1));
			
			taut = new double[nl];
			for(int j=0; j<taut.length; j++) {
				taut[j] = byteBuf.getDouble();
			}
			byteBuf.position(byteBuf.position()+Double.BYTES*(JOUT-nl));
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) {
			System.out.println("Mismatch - recLen recLast: " + recLen + " " + recLast);
			throw new IOException();
		}
		
		/*
		 * Read the sixth record.
		 */
//	System.out.println("\nRecord 6:");
		byteBuf.compact();
//	System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
//	System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
//		System.out.println("RecLen = " + recLast + " " + recLen + " " 
//			+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
			// Coefficients for the tau interpolation basis functions: coef.
			coef = new double[nl][5];
			for(int j=0; j<nl; j++) {
				for(int i=0; i<5; i++) {
					coef[j][i] = byteBuf.getDouble();
				}
			}
			byteBuf.position(byteBuf.position()+5*Double.BYTES*(JOUT-nl));
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) {
			System.out.println("Mismatch - recLen recLast: " + recLen + " " + recLast);
			throw new IOException();
		}
		in.close();
	}
	
	/**
	 * Read the travel-time table file ("model_name".tbl).  This file contains 
	 * slowness, tau, and distance for up-going branches as a function of depth 
	 * to the deepest depth that earthquakes are observed.  The up-going branches
	 * are used to correct all phases for depth.  Note that the distance information 
	 * is only for branch ends as the interpolation provides all other distances.  
	 * 
	 * In the FORTRAN implementation, the up-going branches nearest the source 
	 * depth were random accessed to save memory.  In the Java implementation, the 
	 * entire table is read into memory.
	 * 
	 * @throws IOException
	 */
	public void readTable() throws IOException {
		int bytesRead;
		int recSize;
		int[] upSize;
		
		// Open the table file.
		in = new BufferedInputStream(new FileInputStream(modelPath + 
				modelName + ".tbl"));
		
		// Set the random access record length.  Note: the factor of four reflects 
		// a bug in the Fortran code (the record length for direct access binary 
		// files is set in words, not bytes).
		recSize = 4*nasgr;
		
		// Set up table record limits.
		numRec = new int[2];
		upSize = new int[2];
		for(int i=0; i<2; i++){
			numRec[i] = ndex[i][ndex[i].length-1];
			upSize[i] = Double.BYTES*(ku[i]+km[i]);
		}
		numRec[1] = numRec[1]-numRec[0];
		
		// Set up the data arrays.
		tauUp = new double[2][][];
		delUp = new double[2][][];
		for(int i=0; i<2; i++) {
			tauUp[i] = new double[numRec[i]][];
			delUp[i] = new double[numRec[i]][];
			for(int j=0; j<numRec[i]; j++) {
				tauUp[i][j] = new double[ku[i]];
				delUp[i][j] = new double[km[i]];
			}
		}
					
		/*
		 * Read the up-going phase records.
		 */
//	int recBase = 0;
		byteBuf.clear();
		for(int i=0; i<2; i++) {
			for(int j=0; j<numRec[i]; j++) {
		//	System.out.println("Record " + (recBase+j) + ":");
		//	System.out.println("Position: " + byteBuf.position());
				bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
						byteBuf.position());
		//	System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
				bytesRead = bytesRead + byteBuf.position();
				byteBuf.position(0);
		//	System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
				
				if(bytesRead >= upSize[i]) {
					// Copy the data into internal arrays.
					for(int k=0; k<tauUp[i][j].length; k++) {
						tauUp[i][j][k] = byteBuf.getDouble();
					}
					for(int k=0; k<delUp[i][j].length; k++) {
						delUp[i][j][k] = byteBuf.getDouble();
					}
				}
				byteBuf.position(byteBuf.position()+recSize-upSize[i]);
		//	System.out.println("New Position: "+byteBuf.position()+" "+recSize+" "+upSize[i]);
				byteBuf.compact();
			}
	//	recBase = numRec[i];
		}
		in.close();
	}
	
	/**
	 * Print the contents of the first header record to the console.
	 * 
	 * @param full If true, print everything.  Otherwise, just print a summary.
	 */
	public void dumpRec1(boolean full) {
		
		System.out.println("\nNasgr nl = " + nasgr + " " + nl);
		System.out.println("Xn pn tn = " + xn + " " + pn + " " + tn);
		System.out.println("Mt nseg nbrn ku km = " + mt[0] + " " + 
				mt[1] + " " + nseg + " " + nbrn + " " + ku[0] + " " + 
				ku[1] + " " + km[0] + " " + km[1]);
		
		if(full) {
			System.out.println("\nFcs:");
			for(int j=0; j<fcs.length; j++) 
			System.out.println("" + j + ": " + (float)fcs[j][0] + ", " + 
					(float)fcs[j][1] + ", " + (float)fcs[j][2]);
			
			System.out.println("\nNafl:");
			for(int j=0; j<nafl.length; j++) 
				System.out.println("" + j + ": " + nafl[j][0] + ", " + 
					nafl[j][1] + ", " + nafl[j][2]);
			
			System.out.println("\nIndx:");
			for(int j=0; j<indx.length; j++) 
				System.out.println("" + j + ": " + indx[j][0] + ", " + 
					indx[j][1]);
	
	/*	System.out.println("\nKndx:");
			for(int j=0; j<kndx.length; j++) 
				System.out.println("" + j + ": " + kndx[j][0] + ", " + 
					kndx[j][1]); */
		}
		
		System.out.println("\nRum rmh rcn = " + rum + " " + rmh + " " + 
				rcn);
	}
	
	/**
	 * Print the contents of the second header record to the console.
	 */
	public void dumpRec2() {
		
		System.out.println("\nPm:");
		for(int j=0; j<pm[0].length; j++) 
			System.out.println("" + j + ": " + (float)pm[0][j] + ", " + (float)pm[1][j]);
		for(int j=pm[0].length; j<pm[1].length; j++) 
			System.out.println("" + j + ":                     " + (float)pm[1][j]);
		
		System.out.println("\nZm:");
		for(int j=0; j<zm[0].length; j++) 
			System.out.println("" + j + ": " + (float)zm[0][j] + ", " + (float)zm[1][j]);
		for(int j=zm[0].length; j<zm[1].length; j++) 
			System.out.println("" + j + ":                     " + (float)zm[1][j]);
		
		System.out.println("\nNdex:");
		for(int j=0; j<ndex[0].length; j++) 
			System.out.println("" + j + ": " + ndex[0][j] + ", " + ndex[1][j]);
		for(int j=ndex[0].length; j<ndex[1].length; j++) 
			System.out.println("" + j + ":           " + ndex[1][j]);
	}
	
	/**
	 * Print the contents of the third header record to the console.
	 */
	public void dumpRec3() {
		
		System.out.println("\nPu:");
		for(int j=0; j<pu[0].length; j++) 
			System.out.println("" + j + ": " + (float)pu[0][j] + ", " + (float)pu[1][j]);
		for(int j=pu[0].length; j<pu[1].length; j++) 
			System.out.println("" + j + ":                     " + (float)pu[1][j]);
		
		System.out.println("\nPux:");
		for(int j=0; j<pux[0].length; j++) 
			System.out.println("" + j + ": " + (float)pux[0][j] + ", " + (float)pux[1][j]);
		for(int j=pux[0].length; j<pux[1].length; j++) 
			System.out.println("" + j + ":                     " + (float)pux[1][j]);
	}
	
	/**
	 * Print the contents of the fourth header record to the console.
	 */
	public void dumpRec4() {
		
		System.out.println("\nPhcd:");
		for(int j=0; j<phcd.length; j++) 
			System.out.println("" + j + ": " + phcd[j]);
		
		System.out.println("\nPx:");
		for(int j=0; j<px.length; j++) 
			System.out.println("" + j + ": " + (float)px[j][0] + ", " + (float)px[j][1]);
		
		System.out.println("\nXt:");
		for(int j=0; j<xt.length; j++) 
			System.out.println("" + j + ": " + (float)xt[j][0] + ", " + (float)xt[j][1]);
		
		System.out.println("\nJndx:");
		for(int j=0; j<jndx.length; j++) 
			System.out.println("" + j + ": " + jndx[j][0] + ", " + jndx[j][1]);
	}
	
	/**
	 * Print the contents of the fifth header record to the console.
	 */
	public void dumpRec5() {
		
		System.out.println("\nPt:");
		for(int j=0; j<pt.length; j++) 
			System.out.println("" + j + ": " + (float)pt[j]);
		
		System.out.println("\nTaut:");
		for(int j=0; j<taut.length; j++) 
			System.out.println("" + j + ": " + (float)taut[j]);
	}
	
	/**
	 * Print the contents of the sixth header record to the console.
	 */
	public void dumpRec6() {
		
		System.out.println("\nCoef:");
		for(int j=0; j<coef.length; j++) 
			System.out.println("" +j+": "+(float)coef[j][0]+", "+(float)coef[j][1]+
					", "+(float)coef[j][2]+", "+(float)coef[j][3]+", "+(float)coef[j][4]);
	}
	
	/**
	 * Print the contents of a table record to the console.
	 * 
	 * @param rec Table record to print.
	 */
	public void dumpTable(int rec) {
		int i, j;
		
		if(rec < numRec[0]) {
			i = 0;
			j = rec;
		}
		else {
			i = 1;
			j = rec-numRec[0];
		}
		
		System.out.println("\nTauUp: "+i+" "+j);
		for(int k=0; k<tauUp[i][j].length; k++) {
			System.out.println("" + k + " " + (float)tauUp[i][j][k]);
		}
		
		System.out.println("\nDelUp:");
		for(int k=0; k<delUp[i][j].length; k++) {
			System.out.println("" + k + " " + (float)delUp[i][j][k]);
		}
	}
	
	/**
	 * Print the global variables in a nicer format.
	 */
	public void dumpGlobal() {
		System.out.println("\n     Global Varaiables");
		System.out.format("Limits: nasgr =%5d  nl =%5d  nseg =%3d  nbrn =%3d\n",
				nasgr,nl,nseg,nbrn);
		System.out.format("Normalization: xn =%11.4e  pn =%11.4e  tn =%11.4e\n",
				xn,pn,tn);
		System.out.format("Radii: rum =%7.1f  rmh =%7.1f  rcn =%7.1f\n",rum,rmh,rcn);
	}
	
	/**
	 * Print the segment variables all together mimicking the Setbrn formatting.
	 */
	public void dumpSegments() {
		System.out.println("\n     Segment Summary (nafl, indx, and fcs)");
		for(int j=0; j<nseg; j++) {
			System.out.format("%2d:  %2d  %2d  %2d  %4d  %4d  %3.1f  %3.1f  %3.1f\n",
					j,nafl[j][0],nafl[j][1],nafl[j][2],indx[j][0],indx[j][1],fcs[j][0],
					fcs[j][1],fcs[j][2]);
		}
	}
	
	/**
	 * Print the branch variables all together mimicking the Setbrn formatting.
	 */
	public void dumpBranches() {
		final double rad2Deg = 180d/Math.PI;
		
		System.out.println("\n     Branch Summary (jndx, px, xt, and phcd)");
		for(int j=0; j<nbrn; j++) {
			System.out.format("%2d:  %4d  %4d  %8.6f  %8.6f  %6.2f  %6.2f  %s\n",j,
					jndx[j][0],jndx[j][1],px[j][0],px[j][1],rad2Deg*xt[j][0],
					rad2Deg*xt[j][1],phcd[j]);
		}
	}
	
	/**
	 * Print the model parameters all together mimicking the Setbrn formatting.
	 */
	public void dumpModel(boolean nice) {
		System.out.println("\n     Model Summary (zm, pm, and ndex)");
		if(nice) {
			for(int j=0; j<mt[0]; j++) {
				System.out.format("%3d: %6.1f  %5.2f  %3d  %6.1f %5.2f  %3d\n",
						j,TauUtil.realZ(zm[0][j]),TauUtil.realV(pm[0][j],zm[0][j]),ndex[0][j],
						TauUtil.realZ(zm[1][j]),TauUtil.realV(pm[1][j],zm[1][j]),ndex[1][j]);
			}
			for(int j=mt[0]; j<mt[1]; j++) {
				System.out.format("%3d:                     %6.1f %5.2f  %3d\n",j,
						TauUtil.realZ(zm[1][j]),TauUtil.realV(pm[1][j],zm[1][j]),ndex[1][j]);
			}
		} else {
			for(int j=0; j<mt[0]; j++) {
				System.out.format("%3d: %9.6f  %8.6f  %3d  %9.6f %8.6f  %3d\n",
						j,zm[0][j],pm[0][j],ndex[0][j],zm[1][j],pm[1][j],ndex[1][j]);
			}
			for(int j=mt[0]; j<mt[1]; j++) {
				System.out.format("%3d:                           "+
						"%9.6f %8.6f  %3d\n",j,zm[1][j],pm[1][j],ndex[1][j]);
			}
		}
	}
	
	/**
	 * Print the up-going variables all together for one table record.
	 */
	public void dumpUp(int rec) {
		int i, j;
		
		System.out.println("\n     Up-going Summary for record "+rec+
				" (pu, tauu, pux, and xu)");
		
		if(rec < numRec[0]) {
			i = 0;
			j = rec;
		}
		else {
			i = 1;
			j = rec-numRec[0];
		}
		
		for(int k=0; k<km[i]; k++) {
			System.out.format("%3d  %8.6f  %8.6f  %8.6f  %9.6f\n",k,
					pu[i][k],tauUp[i][j][k],pux[i][k],delUp[i][j][k]);
		}
		for(int k=km[i]; k<ku[i]; k++) {
			System.out.format("%3d  %8.6f  %8.6f\n",k,pu[i][k],tauUp[i][j][k]);
		}
	}
	
	/**
	 * Print the surface focus p, tau, etc. for all branches mimicking the 
	 * Setbrn formatting.
	 */
	public void dumpAll() {
		System.out.println("\n     All Branches (pt, taut, and coef)");
		for(int j=0; j<nl; j++) {
			System.out.format("%3d: %8.6f  %8.6f  %9.2e  %9.2e  %9.2e  %9.2e  %9.2e\n",
					j,pt[j],taut[j],coef[j][0],coef[j][1],coef[j][2],coef[j][3],coef[j][4]);
		}
	}
}
