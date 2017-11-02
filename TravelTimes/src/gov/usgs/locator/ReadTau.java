package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Read the FORTRAN generated travel-time files.  ReadTau is temporary code 
 * to aid in the porting of the seismic travel-time package and the earthquake 
 * locator.  By reading the FORTRAN generated travel-time header and table 
 * information, it should facilitate testing and allow the travel-time port to 
 * a Java server to be completed in a third of the development time.
 * 
 * @author Ray Buland
 *
 */
public class ReadTau {
	/**
	 * FORTRAN array limit for the shallow part of the Earth model.
	 */
	public final int JSRC = 250;
	/**
	 * FORTRAN array limit for travel-time segments.
	 */
	public final int JSEG = 30;
	/**
	 * FORTRAN array limit for travel-time branches.
	 */
	public final int JBRN = 120;
	/**
	 * FORTRAN array limit for complete branch descriptions.
	 */
	public final int JOUT = 2500;
	/**
	 * FORTRAN array limit for up-going branches.
	 */
	public final int JTSM = 500;
	
	// Set up the reader.
	final String modelPath = "../../../Documents/Work/Models/";
	String modelName;
	final int bufLen = 100020;
	byte[] byteArray;
	ByteBuffer byteBuf;
	BufferedInputStream in;

	/*
	 *    Header variables:
	 * 
	 *    Global parameters
	 * nasgr -> recSizeUp									Record size in the table file
	 * len2																Not used (5 X nl)
	 * xn -> xNorm												Distance normalization
	 * pn	-> pNorm												Slowness normalization
	 * tn	-> tNorm												Time normalization
	 * rum ->	rUpperMantle								Upper mantle radius (discontinuity near 
	 * 																			410 km depth in km)
	 * rmh ->	rMoho												Moho radius (km)
	 * rcn -> rConrad											Conrad radius (km)
	 * 
	 *    Segment parameters (record 2)
	 * nseg	-> numSeg											Number of travel-time segments
	 * fcs -> countSeg[numSeg][3]					Number of traversals of each region
	 * nafl -> typeSeg[numSeg][3]					Phase type per region (1=P, 2=S)
	 * indx -> indexSeg[numSeg][2]				Segment indices into the branch specification
	 * kndx																Not used
	 * 
	 *    Branch parameters (record 4)
	 * nbrn -> numBrn											Number of travel-time branches
	 * phcd -> phCode[numBrn]							Branch phase codes
	 * jndx -> indexBrn[numBrn][2]				Branch indices into the branch specification
	 * px -> pBrn[numBrn][2]							Slowness range for branches
	 * xt -> xBrn[numBrn][2]							Distance ranges for branches
	 * 
	 *    Model parameters for upper 800 km (record 2)
	 * mt -> numMod[2]										Number of model samples
	 * zm -> zMod[2][numMod+3]						Model depths
	 * pm -> pMod[2][numMod+3]						Model slownesses
	 * ndex -> indexMod[2][numMod]				Record number in the table file
	 * 
	 *    Up-going branches (record 3 & table file)
	 * ku -> numTauUp[2]									Number of up-going tau samples
	 * pu -> pTauUp[2][numTauUp+1]				Slowness sampling for up-going branches
	 * tauu -> tauUp[2][numRec][numTauUp] Up-going tau for all depths
	 * km -> numXUp[2]										Number of up-going distance samples
	 * pux -> pXUp[2][numXUp+1]						Distances for up-going branch ends
	 * xu -> xUp[2][numRec][numXUp]				Up-going x for all depths
	 * 		numRec[2]												Derived from ndex (indexMod)
	 * 
	 *    Tau(p), etc. for all branches (records 5 and 6)
	 * nl -> numSpec											Number of samples for all branches
	 * pt -> pSpec[numSpec+1]							Slowness sampling for all branches
	 * taut -> tauSpec[numSpec]						Surface focus tau(p) for all branches
	 * coef -> basisSpec[numSpec][5]			Interpolation basis functions for tau(p)
	 */
	String[] phCode;
	int recSizeUp, numSpec, numSeg, numBrn = 0;
// int len2;
	int[] numMod = null, numTauUp = null, numXUp = null, numRec;
	int[][] typeSeg, indexSeg, indexMod, indexBrn;
// int[][] kndx;
	double xNorm, pNorm, tNorm, rUpperMantle, rMoho, rConrad, rSurface;
	double[] pSpec, tauSpec;
	double[][] countSeg, pMod, zMod, pTauUp, pXUp, pBrn, xBrn, basisSpec;
	double[][][] tauUp, xUp;
	
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
	 * @throws IOException Throw an exception if the input file is not laid out 
	 * as expected
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
			recSizeUp = byteBuf.getInt();
			numSpec = byteBuf.getInt();
	//	This variable is never used in the following code, so just skip it.
	//	len2 = byteBuf.getInt();
			byteBuf.position(byteBuf.position()+Integer.BYTES);
			xNorm = byteBuf.getFloat();
			pNorm = byteBuf.getFloat();
			tNorm = byteBuf.getFloat();
			// This was apparently upside down.
			tNorm = 1d/tNorm;
			// This will be useful.
			rSurface = 1d/xNorm;
			// Get on with it.
			numMod = new int[2];
			numMod[0] = byteBuf.getInt();
			numMod[1] = byteBuf.getInt();
			numSeg = byteBuf.getInt();
			numBrn = byteBuf.getInt();
			numTauUp = new int[2];
			numTauUp[0] = byteBuf.getInt();
			numTauUp[1] = byteBuf.getInt();
			numXUp = new int[2];
			numXUp[0] = byteBuf.getInt();
			numXUp[1] = byteBuf.getInt();
			
			// Read segment level parameters, etc.: fcs, nafl, indx, kndx.
			countSeg = new double[numSeg][3];
			for(int i=0; i<3; i++) {
				for(int j=0; j<countSeg.length; j++) {
					countSeg[j][i] = byteBuf.getFloat();
				}
				byteBuf.position(byteBuf.position()+Float.BYTES*(JSEG-numSeg));
			}
			
			typeSeg = new int[numSeg][3];
			for(int i=0; i<3; i++) {
				for(int j=0; j<typeSeg.length; j++) {
					typeSeg[j][i] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-numSeg));
			}
			
			indexSeg = new int[numSeg][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<indexSeg.length; j++) {
					indexSeg[j][i] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-numSeg));
			}
			
	/*  This variable is apparently never used in the later code, so just skip it.
			kndx = new int[numSeg][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<kndx.length; j++) {
					kndx[j][i] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-numSeg));
			} */
			byteBuf.position(byteBuf.position()+2*Integer.BYTES*JSEG);
			
			// Read the radiuses of critical internal boundaries: rum, rmh, rcn.
			rUpperMantle = byteBuf.getFloat();
			rMoho = byteBuf.getFloat();
			rConrad = byteBuf.getFloat();
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
			pMod = new double[2][];
			for(int i=0; i<2; i++) {
				pMod[i] = new double[numMod[i]+3];
				for(int j=0; j<pMod[i].length; j++) {
					pMod[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JSRC-numMod[i]-3));
			}
			
			zMod = new double[2][];
			for(int i=0; i<2; i++) {
				zMod[i] = new double[numMod[i]+3];
				for(int j=0; j<zMod[i].length; j++) {
					zMod[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JSRC-numMod[i]-3));
			}
			
			indexMod = new int[2][];
			for(int i=0; i<2; i++) {
				indexMod[i] = new int[numMod[i]];
				for(int j=0; j<indexMod[i].length; j++) {
					indexMod[i][j] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSRC-numMod[i]));
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
			pTauUp = new double[2][];
			for(int i=0; i<2; i++) {
				pTauUp[i] = new double[numTauUp[i]+1];
				for(int j=0; j<pTauUp[i].length; j++) {
					pTauUp[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JTSM-numTauUp[i]));
			}
			
			pXUp = new double[2][];
			for(int i=0; i<2; i++) {
				pXUp[i] = new double[numXUp[i]+1];
				for(int j=0; j<pXUp[i].length; j++) {
					pXUp[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-numXUp[i]-1));
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
			phCode = new String[numBrn];
			for(int j=0; j<phCode.length; j++) {
				byteBuf.get(temp);
				phCode[j] = new String(temp).trim();
			}
			byteBuf.position(byteBuf.position()+temp.length*(JBRN-numBrn));
			
			pBrn = new double[numBrn][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<pBrn.length; j++) {
					pBrn[j][i] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-numBrn));
			}
			
			xBrn = new double[numBrn][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<xBrn.length; j++) {
					xBrn[j][i] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-numBrn));
			}
			
			indexBrn = new int[numBrn][2];
			for(int i=0; i<2; i++) {
				for(int j=0; j<indexBrn.length; j++) {
					indexBrn[j][i] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JBRN-numBrn));
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
			pSpec = new double[numSpec+1];
			for(int j=0; j<pSpec.length; j++) {
				pSpec[j] = byteBuf.getDouble();
			}
			byteBuf.position(byteBuf.position()+Double.BYTES*(JOUT-numSpec-1));
			
			tauSpec = new double[numSpec];
			for(int j=0; j<tauSpec.length; j++) {
				tauSpec[j] = byteBuf.getDouble();
			}
			byteBuf.position(byteBuf.position()+Double.BYTES*(JOUT-numSpec));
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
			basisSpec = new double[5][numSpec];
			for(int j=0; j<numSpec; j++) {
				for(int i=0; i<5; i++) {
					basisSpec[i][j] = byteBuf.getDouble();
				}
			}
			byteBuf.position(byteBuf.position()+5*Double.BYTES*(JOUT-numSpec));
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) {
			System.out.println("Mismatch - recLen recLast: " + recLen + " " + recLast);
			throw new IOException();
		}
		in.close();
//	repairHed();
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
	 * @throws IOException Throw an exception if the input file is not laid out as 
	 * expected
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
		recSize = 4*recSizeUp;
		
		// Set up table record limits.
		numRec = new int[2];
		upSize = new int[2];
		for(int i=0; i<2; i++){
			numRec[i] = indexMod[i][indexMod[i].length-1];
			upSize[i] = Double.BYTES*(numTauUp[i]+numXUp[i]);
		}
		numRec[1] = numRec[1]-numRec[0];
		
		// Set up the data arrays.
		tauUp = new double[2][][];
		xUp = new double[2][][];
		for(int i=0; i<2; i++) {
			tauUp[i] = new double[numRec[i]][];
			xUp[i] = new double[numRec[i]][];
			for(int j=0; j<numRec[i]; j++) {
				tauUp[i][j] = new double[numTauUp[i]];
				xUp[i][j] = new double[numXUp[i]];
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
					for(int k=0; k<xUp[i][j].length; k++) {
						xUp[i][j][k] = byteBuf.getDouble();
					}
				}
				byteBuf.position(byteBuf.position()+recSize-upSize[i]);
		//	System.out.println("New Position: "+byteBuf.position()+" "+recSize+" "+
		//			upSize[i]);
				byteBuf.compact();
			}
	//	recBase = numRec[i];
		}
		in.close();
//	repairTbl();
		byteArray = null;
	}
		
	/**
	 * Print the contents of the first header record to the console.
	 * 
	 * @param full If true, print everything.  Otherwise, just print a summary.
	 */
	public void dumpRec1(boolean full) {
		
		System.out.println("\nrecSizeUp numSpec = " + recSizeUp + " " + numSpec);
		System.out.println("xNorm pNorm tNorm = " + xNorm + " " + pNorm + " " + tNorm);
		System.out.println("numMod numSeg numBrn numTauUp numXUp = " + numMod[0] + 
				" " + numMod[1] + " " + numSeg + " " + numBrn + " " + numTauUp[0] + " " + 
				numTauUp[1] + " " + numXUp[0] + " " + numXUp[1]);
		
		if(full) {
			System.out.println("\ncountSeg:");
			for(int j=0; j<countSeg.length; j++) 
			System.out.println("" + j + ": " + (float)countSeg[j][0] + ", " + 
					(float)countSeg[j][1] + ", " + (float)countSeg[j][2]);
			
			System.out.println("\ntypeSeg:");
			for(int j=0; j<typeSeg.length; j++) 
				System.out.println("" + j + ": " + typeSeg[j][0] + ", " + 
					typeSeg[j][1] + ", " + typeSeg[j][2]);
			
			System.out.println("\nindexSeg:");
			for(int j=0; j<indexSeg.length; j++) 
				System.out.println("" + j + ": " + indexSeg[j][0] + ", " + 
					indexSeg[j][1]);
	
	/*	System.out.println("\nKndx:");
			for(int j=0; j<kndx.length; j++) 
				System.out.println("" + j + ": " + kndx[j][0] + ", " + 
					kndx[j][1]); */
		}
		
		System.out.println("\nrUpperMantle RMoho rConrad = " + rUpperMantle + " " + 
				rMoho + " " + rConrad);
	}
	
	/**
	 * Print the contents of the second header record to the console.
	 */
	public void dumpRec2() {
		
		System.out.println("\npMod:");
		for(int j=0; j<pMod[0].length; j++) 
			System.out.println("" + j + ": " + (float)pMod[0][j] + ", " + 
					(float)pMod[1][j]);
		for(int j=pMod[0].length; j<pMod[1].length; j++) 
			System.out.println("" + j + ":                     " + (float)pMod[1][j]);
		
		System.out.println("\nzMod:");
		for(int j=0; j<zMod[0].length; j++) 
			System.out.println("" + j + ": " + (float)zMod[0][j] + ", " + 
					(float)zMod[1][j]);
		for(int j=zMod[0].length; j<zMod[1].length; j++) 
			System.out.println("" + j + ":                     " + (float)zMod[1][j]);
		
		System.out.println("\nindexMod:");
		for(int j=0; j<indexMod[0].length; j++) 
			System.out.println("" + j + ": " + indexMod[0][j] + ", " + indexMod[1][j]);
		for(int j=indexMod[0].length; j<indexMod[1].length; j++) 
			System.out.println("" + j + ":           " + indexMod[1][j]);
	}
	
	/**
	 * Print the contents of the third header record to the console.
	 */
	public void dumpRec3() {
		
		System.out.println("\npTauUp:");
		for(int j=0; j<pTauUp[0].length; j++) 
			System.out.println("" + j + ": " + (float)pTauUp[0][j] + ", " + 
					(float)pTauUp[1][j]);
		for(int j=pTauUp[0].length; j<pTauUp[1].length; j++) 
			System.out.println("" + j + ":                     " + (float)pTauUp[1][j]);
		
		System.out.println("\npXUp:");
		for(int j=0; j<pXUp[0].length; j++) 
			System.out.println("" + j + ": " + (float)pXUp[0][j] + ", " + 
					(float)pXUp[1][j]);
		for(int j=pXUp[0].length; j<pXUp[1].length; j++) 
			System.out.println("" + j + ":                     " + (float)pXUp[1][j]);
	}
	
	/**
	 * Print the contents of the fourth header record to the console.
	 */
	public void dumpRec4() {
		
		System.out.println("\nphCode:");
		for(int j=0; j<phCode.length; j++) 
			System.out.println("" + j + ": " + phCode[j]);
		
		System.out.println("\npBrn:");
		for(int j=0; j<pBrn.length; j++) 
			System.out.println("" + j + ": " + (float)pBrn[j][0] + ", " + 
					(float)pBrn[j][1]);
		
		System.out.println("\nxBrn:");
		for(int j=0; j<xBrn.length; j++) 
			System.out.println("" + j + ": " + (float)xBrn[j][0] + ", " + 
					(float)xBrn[j][1]);
		
		System.out.println("\nindexBrn:");
		for(int j=0; j<indexBrn.length; j++) 
			System.out.println("" + j + ": " + indexBrn[j][0] + ", " + 
					indexBrn[j][1]);
	}
	
	/**
	 * Print the contents of the fifth header record to the console.
	 */
	public void dumpRec5() {
		
		System.out.println("\npSpec:");
		for(int j=0; j<pSpec.length; j++) 
			System.out.println("" + j + ": " + (float)pSpec[j]);
		
		System.out.println("\ntauSpec:");
		for(int j=0; j<tauSpec.length; j++) 
			System.out.println("" + j + ": " + (float)tauSpec[j]);
	}
	
	/**
	 * Print the contents of the sixth header record to the console.
	 */
	public void dumpRec6() {
		
		System.out.println("\nbasisSpec:");
		for(int j=0; j<basisSpec.length; j++) 
			System.out.println("" +j+": "+(float)basisSpec[j][0]+", "+
					(float)basisSpec[j][1]+", "+(float)basisSpec[j][2]+", "+
					(float)basisSpec[j][3]+", "+(float)basisSpec[j][4]);
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
		
		System.out.println("\nxUp:");
		for(int k=0; k<xUp[i][j].length; k++) {
			System.out.println("" + k + " " + (float)xUp[i][j][k]);
		}
	}
	
	/**
	 * Print the global variables in a nicer format.
	 */
	public void dumpGlobal() {
		System.out.println("\n     Global Varaiables");
		System.out.format("Limits: recSizeUp =%5d  numSpec =%5d  numSeg =%3d  "+
				"numBrn =%3d\n",recSizeUp,numSpec,numSeg,numBrn);
		System.out.format("Normalization: xNorm =%11.4e  pNorm =%11.4e  "+
				"tNorm =%11.4e\n",xNorm,pNorm,tNorm);
		System.out.format("Radii: rUpperMantle =%7.1f  RMoho =%7.1f  rConrad =%7.1f\n",
				rUpperMantle,rMoho,rConrad);
	}
	
	/**
	 * Print the segment variables all together mimicking the Setbrn formatting.
	 */
	public void dumpSegments() {
		System.out.println("\n     Segment Summary (typeSeg, indexSeg, and countSeg)");
		for(int j=0; j<numSeg; j++) {
			System.out.format("%2d:  %2d  %2d  %2d  %4d  %4d  %3.1f  %3.1f  %3.1f\n",
					j,typeSeg[j][0],typeSeg[j][1],typeSeg[j][2],indexSeg[j][0],indexSeg[j][1],
					countSeg[j][0],countSeg[j][1],countSeg[j][2]);
		}
	}
	
	/**
	 * Print the branch variables all together mimicking the Setbrn formatting.
	 */
	public void dumpBranches() {		
		System.out.println("\n     Branch Summary (indexBrn, pBrn, xBrn, and phCode)");
		for(int j=0; j<numBrn; j++) {
			System.out.format("%2d:  %4d  %4d  %8.6f  %8.6f  %6.2f  %6.2f  %s\n",j,
					indexBrn[j][0],indexBrn[j][1],pBrn[j][0],pBrn[j][1],
					Math.toDegrees(xBrn[j][0]),Math.toDegrees(xBrn[j][1]),phCode[j]);
		}
	}
	
	/**
	 * Print the model parameters all together mimicking the Setbrn formatting.
	 * 
	 * @param nice If true print the depth and velocity in dimensional units 
	 * instead of the normalized values.
	 */
	public void dumpModel(boolean nice) {
		System.out.println("\n     Model Summary (zMod, pMod, and indexMod)");
		if(nice) {
			for(int j=0; j<numMod[0]; j++) {
				System.out.format("%3d: %6.1f  %5.2f  %3d  %6.1f %5.2f  %3d\n",
						j,realZ(zMod[0][j]),realV(pMod[0][j],zMod[0][j]),
						indexMod[0][j],realZ(zMod[1][j]),realV(pMod[1][j],
						zMod[1][j]),indexMod[1][j]);
			}
			for(int j=numMod[0]; j<numMod[0]+3; j++) {
				System.out.format("%3d: %6.1f  %5.2f       %6.1f %5.2f  %3d\n",
						j,realZ(zMod[0][j]),realV(pMod[0][j],zMod[0][j]),
						realZ(zMod[1][j]),realV(pMod[1][j],zMod[1][j]),indexMod[1][j]);
			}
			for(int j=numMod[0]+3; j<numMod[1]; j++) {
				System.out.format("%3d:                     %6.1f %5.2f  %3d\n",j,
						realZ(zMod[1][j]),realV(pMod[1][j],zMod[1][j]),
						indexMod[1][j]);
			}
			for(int j=numMod[1]; j<numMod[1]+3; j++) {
				System.out.format("%3d:                     %6.1f %5.2f\n",j,
						realZ(zMod[1][j]),realV(pMod[1][j],zMod[1][j]));
			}
		} else {
			for(int j=0; j<numMod[0]; j++) {
				System.out.format("%3d: %9.6f  %8.6f  %3d  %9.6f %8.6f  %3d\n",
						j,zMod[0][j],pMod[0][j],indexMod[0][j],zMod[1][j],pMod[1][j],
						indexMod[1][j]);
			}
			for(int j=numMod[0]; j<numMod[0]+3; j++) {
				System.out.format("%3d: %9.6f  %8.6f       %9.6f %8.6f  %3d\n",
						j,zMod[0][j],pMod[0][j],zMod[1][j],pMod[1][j],indexMod[1][j]);
			}
			for(int j=numMod[0]+3; j<numMod[1]; j++) {
				System.out.format("%3d:                           "+
						"%9.6f %8.6f  %3d\n",j,zMod[1][j],pMod[1][j],indexMod[1][j]);
			}
			for(int j=numMod[1]; j<numMod[1]+3; j++) {
				System.out.format("%3d:                           "+
						"%9.6f %8.6f\n",j,zMod[1][j],pMod[1][j]);
			}
		}
	}
	
	/**
	 * Print the up-going variables all together for one table record.
	 * 
	 * @param rec Table record to print.
	 */
	public void dumpUp(int rec) {
		int i, j;
		
		System.out.println("\n     Up-going Summary for record "+rec+
				" (pTauUp, tauUp, pXUp, and xUp)");
		
		if(rec < numRec[0]) {
			i = 0;
			j = rec;
		}
		else {
			i = 1;
			j = rec-numRec[0];
		}
		
		for(int k=0; k<xUp[i][j].length; k++) {
			System.out.format("%3d  %8.6f  %8.6f  %8.6f  %9.6f\n",k,
					pTauUp[i][k],tauUp[i][j][k],pXUp[i][k],xUp[i][j][k]);
		}
		for(int k=xUp[i][j].length; k<tauUp[i][j].length; k++) {
			System.out.format("%3d  %8.6f  %8.6f\n",k,pTauUp[i][k],tauUp[i][j][k]);
		}
	}
	
	/**
	 * Print the surface focus p, tau, etc. for all branches mimicking the 
	 * Setbrn formatting.
	 */
	public void dumpAll() {
		System.out.println("\n     All Branches (pSpec, tauSpec, and basisSpec)");
		for(int j=0; j<numSpec; j++) {
			System.out.format("%3d: %8.6f  %8.6f  %9.2e  %9.2e  %9.2e  %9.2e  %9.2e\n",
					j,pSpec[j],tauSpec[j],basisSpec[j][0],basisSpec[j][1],basisSpec[j][2],
					basisSpec[j][3],basisSpec[j][4]);
		}
	}
	
	/**
	 * Given a normalized, Earth flattened depth, return the 
	 * dimensional depth.  This version is needed because the static 
	 * normalization constants haven't been set up yet.
	 * 
	 * @param z Normalized, Earth flattened depth
	 * @return Depth in kilometers
	 */
	private double realZ(double z) {
		return (1d-Math.exp(z))/xNorm;
	}

	/**
	 * Given the normalized slowness and depth, return the 
	 * dimensional velocity at that depth.  This version is needed 
	 * because the static normalization constants haven't been set 
	 * up yet.
	 * 
	 * @param p Normalized slowness
	 * @param z Normalized, Earth flattened depth
	 * @return Velocity at that depth in kilometers/second
	 */
	private double realV(double p, double z) {
		return Math.exp(z)/(tNorm*xNorm*p);
	}
}
