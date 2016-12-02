package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ReadTau {
	// Global Fortran array limits.
	public final int JSRC = 150;	// Maximum slowness points in earthquake depth range.
	public final int JSEG = 30;		// Maximum number of travel-time segments.
	public final int JBRN = 120;	// Maximum number of travel-time branches.
	public final int JOUT = 2500;
	public final int JTSM = 400;
	// Set up the reader.
	final int bufLen = 100020;
	final String modelPath = "../../Documents/Work/Models/";
	String modelName;
	byte[] byteArray;
	ByteBuffer byteBuf;
	
	public ReadTau(String modelName) {
		this.modelName = modelName;
		byteArray = new byte[bufLen];
		byteBuf = ByteBuffer.wrap(byteArray);
		byteBuf.order(ByteOrder.LITTLE_ENDIAN);
	}
	
	public void readHeader() throws IOException {
		/*
		 * phcd[nbrn]		Phase codes
		 * 
		 * nasgr
		 * nl				Upper limit for pt, taut, coef
		 * len2
		 * mt				Upper limit for pm, zm, ndex
		 * nseg
		 * nbrn
		 * ku				Upper limit for pu
		 * km				Upper limit for pux
		 * nafl[3][nseg]
		 * indx[2][nseg]
		 * kndx[2][nseg]	Not used?
		 * ndex[2][mt]		
		 * jndx[2][nbrn]
		 * 
		 * xn 				Distance normalization
		 * pn				Slowness normalization
		 * tn				Time normalization
		 * fcs[3][nseg]		
		 * rum				Upper mantle radius (above the 410 km discontinuity; km)
		 * rmh				Moho radius (km)
		 * rcn				Contrad radius (km)
		 * 
		 * pm[2][mt+3]		
		 * zm[2][mt+3]		
		 * pu[2][ku+1]	
		 * pux[2][km+1]		
		 * px[2][nbrn]		Slowness range for branches
		 * xt[2][nbrn]		Distancd ranges for branches
		 * pt[nl+1]			Slowness sampling for all branches
		 * taut[nl]			Base tau(pt) for all branches
		 * coef[5][nl]		Spline coefficients for taut(pt)
		 */
		String[] phcd;
		int recLen = 0, recLast;
		int nasgr, nl, len2, nseg, nbrn = 0;
		int[] mt = null, ku = null, km = null;
		int[][] nafl, indx, kndx, ndex, jndx;
		double xn, pn, tn, rum, rmh, rcn;
		double[] pt, taut;
		double[][] fcs, pm, zm, pu, pux, px, xt, coef;
		int bytesRead;
		BufferedInputStream in;
		
		// Open the file and read the first buffer.
		in = new BufferedInputStream(new FileInputStream(modelPath + 
				modelName + ".hed"));
		/*
		 * Read the first record.
		 */
		System.out.println("Record 1:");
		bytesRead = in.read(byteArray, 0, byteArray.length);
		System.out.println("Bytes read: " + bytesRead);
		if(bytesRead >= 4) {
			recLen = byteBuf.getInt();
			System.out.println("RecLen = " + recLen);
		}
		if(bytesRead >= recLen+4) {
			
// Nasgr, nl, len2, xn, pn, tn, mt, nseg,nbrn, ku, km
			nasgr = byteBuf.getInt();
			nl = byteBuf.getInt();
			len2 = byteBuf.getInt();
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
			System.out.println("Nasgr nl len2 = " + nasgr + " " + nl + 
					" " + len2);
			System.out.println("Xn pn tn = " + xn + " " + pn + " " + tn);
			System.out.println("Mt nseg nbrn ku km = " + mt[0] + " " + 
					mt[1] + " " + nseg + " " + nbrn + " " + ku[0] + " " + 
					ku[1] + " " + km[0] + " " + km[1]);
			
// Fcs, nafl, indx, kndx
			fcs = new double[3][];
			for(int i=0; i<3; i++) {
				fcs[i] = new double[nseg];
				for(int j=0; j<nseg; j++) {
					fcs[i][j] = byteBuf.getFloat();
				}
				byteBuf.position(byteBuf.position()+Float.BYTES*(JSEG-nseg));
			}
			System.out.println("Fcs:");
			for(int j=0; j<nseg; j++) 
				System.out.println("" + j + ": " + fcs[0][j] + ", " + 
						fcs[1][j] + ", " + fcs[2][j]);
			
			nafl = new int[3][];
			for(int i=0; i<3; i++) {
				nafl[i] = new int[nseg];
				for(int j=0; j<nafl[i].length; j++) {
					nafl[i][j] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-nseg));
			}
			System.out.println("Nafl:");
			for(int j=0; j<nafl[0].length; j++) 
				System.out.println("" + j + ": " + nafl[0][j] + ", " + 
						nafl[1][j] + ", " + nafl[2][j]);
			
			indx = new int[2][];
			for(int i=0; i<2; i++) {
				indx[i] = new int[nseg];
				for(int j=0; j<indx[i].length; j++) {
					indx[i][j] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-nseg));
			}
			System.out.println("Indx:");
			for(int j=0; j<indx[0].length; j++) 
				System.out.println("" + j + ": " + indx[0][j] + ", " + 
						indx[1][j]);
			
			kndx = new int[2][];
			for(int i=0; i<2; i++) {
				kndx[i] = new int[nseg];
				for(int j=0; j<kndx[i].length; j++) {
					kndx[i][j] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSEG-nseg));
			}
			System.out.println("Kndx:");
			for(int j=0; j<kndx[0].length; j++) 
				System.out.println("" + j + ": " + kndx[0][j] + ", " + 
						kndx[1][j]);	
			
// Rum, rmh, rcn
			rum = byteBuf.getFloat();
			rmh = byteBuf.getFloat();
			rcn = byteBuf.getFloat();
			System.out.println("Rum rmh rcn = " + rum + " " + rmh + " " + 
					rcn);
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) System.out.println("Mismatch - recLen recLast: " +
				recLen + " " + recLast);
		
		/*
		 * Read the second record.
		 */
		System.out.println("\nRecord 2:");
		byteBuf.compact();
		System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
		System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
			System.out.println("RecLen = " + recLast + " " + recLen + " " 
				+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
// pm, zm, ndex
			pm = new double[2][];
			for(int i=0; i<2; i++) {
				pm[i] = new double[mt[i]+3];
				for(int j=0; j<pm[i].length; j++) {
					pm[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JSRC-mt[i]-3));
			}
			System.out.println("Pm:");
			for(int j=0; j<pm[0].length; j++) 
				System.out.println("" + j + ": " + pm[0][j] + ", " + pm[1][j]);
			for(int j=pm[0].length; j<pm[1].length; j++) 
				System.out.println("" + j + ":                     " + pm[1][j]);
			
			zm = new double[2][];
			for(int i=0; i<2; i++) {
				zm[i] = new double[mt[i]+3];
				for(int j=0; j<zm[i].length; j++) {
					zm[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JSRC-mt[i]-3));
			}
			System.out.println("Zm:");
			for(int j=0; j<zm[0].length; j++) 
				System.out.println("" + j + ": " + zm[0][j] + ", " + zm[1][j]);
			for(int j=zm[0].length; j<zm[1].length; j++) 
				System.out.println("" + j + ":                     " + zm[1][j]);
			
			ndex = new int[2][];
			for(int i=0; i<2; i++) {
				ndex[i] = new int[mt[i]];
				for(int j=0; j<ndex[i].length; j++) {
					ndex[i][j] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JSRC-mt[i]));
			}
			System.out.println("Ndex:");
			for(int j=0; j<ndex[0].length; j++) 
				System.out.println("" + j + ": " + ndex[0][j] + ", " + ndex[1][j]);
			for(int j=ndex[0].length; j<ndex[1].length; j++) 
				System.out.println("" + j + ":           " + ndex[1][j]);
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) System.out.println("Mismatch - recLen recLast: " +
				recLen + " " + recLast);
		
		/*
		 * Read the third record.
		 */
		System.out.println("\nRecord 3:");
		byteBuf.compact();
		System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
		System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
			System.out.println("RecLen = " + recLast + " " + recLen + " " 
				+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
// Pu, pux
			pu = new double[2][];
			for(int i=0; i<2; i++) {
				pu[i] = new double[ku[i]+1];
				for(int j=0; j<pu[i].length; j++) {
					pu[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JTSM-ku[i]));
			}
			System.out.println("Pu:");
			for(int j=0; j<pu[0].length; j++) 
				System.out.println("" + j + ": " + pu[0][j] + ", " + pu[1][j]);
			for(int j=pu[0].length; j<pu[1].length; j++) 
				System.out.println("" + j + ":                     " + pu[1][j]);
			
			pux = new double[2][];
			for(int i=0; i<2; i++) {
				pux[i] = new double[km[i]+1];
				for(int j=0; j<pux[i].length; j++) {
					pux[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-km[i]-1));
			}
			System.out.println("Pux:");
			for(int j=0; j<pux[0].length; j++) 
				System.out.println("" + j + ": " + pux[0][j] + ", " + pux[1][j]);
			for(int j=pux[0].length; j<pux[1].length; j++) 
				System.out.println("" + j + ":                     " + pux[1][j]);
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) System.out.println("Mismatch - recLen recLast: " +
				recLen + " " + recLast);
		
		/*
		 * Read the forth record.
		 */
		System.out.println("\nRecord 4:");
		byteBuf.compact();
		System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
		System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
			System.out.println("RecLen = " + recLast + " " + recLen + " " 
				+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
// phcd, px, xt, jndx
			byte[] temp = new byte[8];
			phcd = new String[nbrn];
			for(int j=0; j<phcd.length; j++) {
				byteBuf.get(temp);
				phcd[j] = new String(temp);
			}
			byteBuf.position(byteBuf.position()+temp.length*(JBRN-nbrn));
			System.out.println("Phcd:");
			for(int j=0; j<phcd.length; j++) 
				System.out.println("" + j + ": " + phcd[j]);
			
			px = new double[2][];
			for(int i=0; i<2; i++) {
				px[i] = new double[nbrn];
				for(int j=0; j<px[i].length; j++) {
					px[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-nbrn));
			}
			System.out.println("Px:");
			for(int j=0; j<px[0].length; j++) 
				System.out.println("" + j + ": " + px[0][j] + ", " + px[1][j]);
			for(int j=px[0].length; j<px[1].length; j++) 
				System.out.println("" + j + ":                     " + px[1][j]);
			
			xt = new double[2][];
			for(int i=0; i<2; i++) {
				xt[i] = new double[nbrn];
				for(int j=0; j<xt[i].length; j++) {
					xt[i][j] = byteBuf.getDouble();
				}
				byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-nbrn));
			}
			System.out.println("Xt:");
			for(int j=0; j<xt[0].length; j++) 
				System.out.println("" + j + ": " + xt[0][j] + ", " + xt[1][j]);
			for(int j=xt[0].length; j<xt[1].length; j++) 
				System.out.println("" + j + ":                     " + xt[1][j]);
			
			jndx = new int[2][];
			for(int i=0; i<2; i++) {
				jndx[i] = new int[nbrn];
				for(int j=0; j<jndx[i].length; j++) {
					jndx[i][j] = byteBuf.getInt();
				}
				byteBuf.position(byteBuf.position()+Integer.BYTES*(JBRN-nbrn));
			}
			System.out.println("Jndx:");
			for(int j=0; j<jndx[0].length; j++) 
				System.out.println("" + j + ": " + jndx[0][j] + ", " + jndx[1][j]);
			for(int j=jndx[0].length; j<jndx[1].length; j++) 
				System.out.println("" + j + ":                     " + jndx[1][j]);
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) System.out.println("Mismatch - recLen recLast: " +
				recLen + " " + recLast);
		
		/*
		 * Read the fifth record.
		 */
		System.out.println("\nRecord 5:");
		byteBuf.compact();
		System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
		System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
			System.out.println("RecLen = " + recLast + " " + recLen + " " 
				+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
// Pt, taut
			pt = new double[JOUT];
			for(int j=0; j<pt.length; j++) {
				pt[j] = byteBuf.getDouble();
			}
		//		byteBuf.position(byteBuf.position()+Double.BYTES*(JTSM-ku[i]));
			System.out.println("Pt:");
			for(int j=0; j<pt.length; j++) 
				System.out.println("" + j + ": " + pt[j]);
			
			taut = new double[JOUT];
			for(int j=0; j<taut.length; j++) {
				taut[j] = byteBuf.getDouble();
		//	byteBuf.position(byteBuf.position()+Double.BYTES*(JBRN-km[i]-1));
			}
			System.out.println("Taut:");
			for(int j=0; j<taut.length; j++) 
				System.out.println("" + j + ": " + taut[j]);
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) System.out.println("Mismatch - recLen recLast: " +
				recLen + " " + recLast);
		
		/*
		 * Read the sixth record.
		 */
		System.out.println("\nRecord 6:");
		byteBuf.compact();
		System.out.println("Position: " + byteBuf.position());
		bytesRead = in.read(byteArray, byteBuf.position(), byteArray.length-
				byteBuf.position());
		System.out.println("Bytes read: " + bytesRead + " " + byteBuf.position());
		bytesRead = bytesRead + byteBuf.position();
		byteBuf.position(0);
		
		// Set up the next Fortran record.
		if(bytesRead-byteBuf.position() >= 4) {
			recLen = byteBuf.getInt();
			System.out.println("RecLen = " + recLast + " " + recLen + " " 
				+ byteBuf.position());
		}
		if(bytesRead-byteBuf.position() >= recLen+4) {
			
// Coef
			coef = new double[5][];
			for(int i=0; i<5; i++) {
				coef[i] = new double[JOUT];
				for(int j=0; j<coef[i].length; j++) {
					coef[i][j] = byteBuf.getDouble();
				}
		//		byteBuf.position(byteBuf.position()+Double.BYTES*(JTSM-ku[i]));
			}
			System.out.println("Coef:");
			for(int j=0; j<coef[0].length; j++) 
				System.out.println("" + j + ": " + coef[0][j] + ", " + coef[1][j] + 
						coef[2][j] + " " + coef[3][j] + " " + coef[4][j]);
		}
		recLast = byteBuf.getInt();
		if(recLast != recLen) System.out.println("Mismatch - recLen recLast: " +
				recLen + " " + recLast);
		in.close();
	}
}
