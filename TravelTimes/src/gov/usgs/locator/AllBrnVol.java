package gov.usgs.locator;

/**
 * Umbrella storage for all volatile branch level travel-time 
 * data.  
 * 
 * @author Ray Buland
 *
 */
public class AllBrnVol {	
	BrnDataVol[] branches;						// Volatile branch data
	ModDataVol pModel, sModel;				// Volatile model data
	UpDataVol pUp, sUp;								// Volatile up-going branch data
	double dSource;										// Dimensional source depth in kilometers
	double zSource;										// Flat Earth source depth
	double dTdDepth;									// Derivative of travel time with respect to depth
	double eqLat;											// Geographical earthquake latitude in degrees
	double eqLon;											// Earthquake longitude in degrees
	double eqDepth;										// Earthquake depth in kilometers
	double staLat;										// Geographical station latitude in degrees
	double staLon;										// Station longitude in degrees
	double staDelta;									// Source-receiver distance in degrees
	double staAzim;										// Receiver azimuth at the source in degrees
	boolean complex;									// True if this is a "complex" request
	AllBrnRef ref;
	ModConvert cvt;
	int lastBrn = -1, upBrnP = -1, upBrnS = -1;
	
	/**
	 * Set up volatile copies of data that changes with depth.
	 * 
	 * @param ref The reference data source
	 */
	public AllBrnVol(AllBrnRef ref) {
		this.ref = ref;
		this.cvt = ref.cvt;
		
		// Set up the volatile piece of the model.
		pModel = new ModDataVol(ref.pModel, cvt);
		sModel = new ModDataVol(ref.sModel, cvt);
		// Set up the up-going branch data.
		pUp = new UpDataVol(ref.pUp, pModel, sModel, cvt);
		sUp = new UpDataVol(ref.sUp, sModel, pModel, cvt);
		
		// Set up the branch data.
		branches = new BrnDataVol[ref.branches.length];
		for(int j=0; j<branches.length; j++) {
			branches[j] = new BrnDataVol(ref.branches[j], pUp, sUp, cvt);
		}
	}
	
	/**
	 * Set up a new session.  Note that this just sets up the 
	 * simple session parameters of use to the travel-time package.
	 * 
	 * @param latitude Source geographical latitude in degrees
	 * @param longitude Source longitude in degrees
	 * @param depth Source depth in kilometers
	 * @param phList Array of phase use commands
	 * @throws Exception If the depth is out of range
	 */
	public void newSession(double latitude, double longitude, double depth, 
			String[] phList) throws Exception {
		complex = true;
		eqLat = latitude;
		eqLon = longitude;
		setSession(depth, phList);
	}
		
		/**
		 * Set up a new session.  Note that this just sets up the 
		 * simple session parameters of use to the travel-time package.
		 * 
		 * @param depth Source depth in kilometers
		 * @param phList Array of phase use commands
		 * @throws Exception If the depth is out of range
		 */
	public void newSession(double depth, String[] phList) throws Exception {
		complex = false;
		eqLat = Double.NaN;
		eqLon = Double.NaN;
		setSession(depth, phList);
	}
	
	/**
	 * Set up a new session.  Note that this just sets up the 
	 * simple session parameters of use to the travel-time package.
	 * 
	 * @param depth Source depth in kilometers
	 * @param phList Array of phase use commands
	 * @throws Exception If the depth is out of range
	 */
	private void setSession(double depth, String[] phList) throws Exception {
		char tagBrn;
		double xMin;
		
		// Set up the new source depth.
		dSource = Math.max(depth, 0.011d);
		// The interpolation gets squirrelly for very shallow sources.
		if(depth < 0.011d) {
			zSource = 0d;
			dTdDepth = 1d/cvt.pNorm;
		} else {
			zSource = Math.min(Math.log(Math.max(1d-dSource*cvt.xNorm, 
					TauUtil.DMIN)), 0d);
			dTdDepth = 1d/(cvt.pNorm*(1d-dSource*cvt.xNorm));
		}
		
		// Fake up the phase list commands for now.
		for(int j=0; j<branches.length; j++) {
			branches[j].setCompute(true);
		}
		// If there are no commands, we're done.
//	if(phList == null) return;
		
		// Correct the up-going branch data.
		pUp.newDepth(zSource);
		sUp.newDepth(zSource);
		
		// To correct each branch we need a few depth dependent pieces.
		xMin = cvt.xNorm*Math.min(Math.max(2d*dSource, 2d), 25d);
		if(dSource <= cvt.zConrad) tagBrn = 'g';
		else if(dSource <= cvt.zMoho) tagBrn = 'b';
		else if(dSource <= cvt.zUpperMantle) tagBrn = 'n';
		else tagBrn = ' ';
		// Now correct each branch.
		for(int j=0; j<branches.length; j++) {
			branches[j].depthCorr(dTdDepth, xMin, tagBrn);
		}
	}
		
	/**
	 * Get the arrival times from all branches for a "complex" 
	 * request (i.e., including latitude and longitude).  This 
	 * will include the ellipticity and bounce point corrections 
	 * as well as the elevation correction.
	 * 
	 * @param latitude Receiver geographic latitude in degrees
	 * @param longitude Receiver longitude in degrees
	 * @param elev Station elevation in kilometers
	 * @param delta Source receiver distance desired in degrees
	 * @param azimuth Receiver azimuth at the source in degrees
	 * @param useful If true, only provide "useful" crustal phases
	 * @param tectonic If true, map Pb and Sb onto Pg and Sg
	 * @param noBackBrn If true, suppress back branches
	 * @param rstt If true, use RSTT crustal phases
	 * @return An array list of travel times
	 */
	public TTime getTT(double latitude, double longitude, double elev, 
			double delta, double azimuth, boolean useful, boolean tectonic, 
			boolean noBackBrn, boolean rstt) {
		
		staLat = latitude;
		staLon = longitude;
		if(delta < 0d || delta == Double.NaN || azimuth < 0 || 
				azimuth == Double.NaN) {
			staDelta = TauUtil.delAz(eqLat, eqLon, staLat, staLon);
			staAzim = TauUtil.azimuth;
		} else {
			staDelta = delta;
			staAzim = azimuth;
		}
		return doTT(elev, useful, tectonic, noBackBrn, rstt);
	}
			
	/**
	 * Get the arrival times from all branches for a "simple" 
	 * request (i.e., without latitude and longitude.  This will 
	 * only include the elevation correction.
	 * 
	 * @param elev Station elevation in kilometers
	 * @param delta Source receiver distance desired in degrees
	 * @param useful If true, only provide "useful" crustal phases
	 * @param tectonic If true, map Pb and Sb onto Pg and Sg
	 * @param noBackBrn If true, suppress back branches
	 * @param rstt If true, use RSTT crustal phases
	 * @return An array list of travel times
	 */
	public TTime getTT(double elev, double delta, boolean useful, 
			boolean tectonic, boolean noBackBrn, boolean rstt) {
		
		staLat = Double.NaN;
		staLon = Double.NaN;
		staDelta = delta;
		staAzim = Double.NaN;
		return doTT(elev, useful, tectonic, noBackBrn, rstt);
	}
	
	/**
	 * Get the arrival times from all branches including all 
	 * applicable travel-time corrections.
	 * 
	 * @param elev Station elevation in kilometers
	 * @param useful If true, only provide "useful" crustal phases
	 * @param tectonic If true, map Pb and Sb onto Pg and Sg
	 * @param noBackBrn If true, suppress back branches
	 * @param rstt If true, use RSTT crustal phases
	 * @return An array list of travel times
	 */
	private TTime doTT(double elev, boolean useful, boolean tectonic, 
			boolean noBackBrn, boolean rstt) {
		int lastTT;
		double delCorr;
		double[] xs;
		TTime ttList;
		TTimeData tTime;
		
		ttList = new TTime();
		lastTT = 0;
		// The desired distance might translate to up to three 
		// different distances (as the phases wrap around the Earth).
		xs = new double[3];
		xs[0] = Math.abs(Math.toRadians(staDelta))%(2d*Math.PI);
		if(xs[0] > Math.PI) xs[0] = 2d*Math.PI-xs[0];
		xs[1] = 2d*Math.PI-xs[0];
		xs[2] = xs[0]+2d*Math.PI;
		if(Math.abs(xs[0]) <= TauUtil.DTOL) {
			xs[0] = TauUtil.DTOL;
			xs[2] = -10d;
		}
		if(Math.abs(xs[0]-Math.PI) <= TauUtil.DTOL) {
			xs[0] = Math.PI-TauUtil.DTOL;
			xs[1] = -10d;
		}
		
		// Set up the correction to surface focus.
		findUpBrn();
		
		// Go get the arrivals.  This is a little convoluted because 
		// of the correction to surface focus needed for the statistics.
		for(int j=0; j<branches.length; j++) {
			// Loop over possible distances.
			for(int i=0; i<3; i++) {
				branches[j].getTT(i, xs[i], dSource, useful, ttList);
				// We have to add the phase statistics at this level.
				if(ttList.size() > lastTT) {
					for(int k=lastTT; k<ttList.size(); k++) {
						tTime = ttList.get(k);
						if(tTime.phCode.charAt(0) == 'L') {
							// Travel-time corrections and correction to surface focus 
							// don't make sense for surface waves.
							delCorr = 0d;
						} else {
							// This is the normal case.  Do various travel-time corrections.
							tTime.tt = tTime.tt+branches[j].elevCorr(elev, tTime.dTdD, 
									rstt);
				//		double elevCorr = branches[j].elevCorr(elev, tTime.dTdD, 
				//			rstt);
							// If this was a complex request, do the ellipticity correction.
							if(complex) {
								tTime.tt = tTime.tt+branches[j].ellipCorr(eqLat, dSource, 
									staDelta, staAzim);
				//			double ellipCorr = branches[j].ellipCorr(eqLat, dSource, 
				//				staDelta, staAzim);
				//			System.out.format("%-8s elev = %6.4f ellip = %6.4f\n",tTime.phCode, 
				//					elevCorr, ellipCorr);
				//		} else {
				//			System.out.println("phCode = "+tTime.phCode+" elev = "+
				//					(float)elevCorr);
							}
							// Add auxiliary information.
							try {
								delCorr = upRay(tTime.phCode, tTime.dTdD);
							} catch (Exception e) {
								// This should never happen.
								e.printStackTrace();
								delCorr = 0d;
							}
						}
						branches[j].addAux(tTime.phCode, xs[i], delCorr, tTime);
					}
					lastTT = ttList.size();
				}
			}
		}
		
		// Sort the arrivals into increasing time order, filter, etc.
		ttList.finish(tectonic, noBackBrn);
		return ttList;
	}
	
	/**
	 * Compute the distance and travel-time for one surface focus ray.
	 * 
	 * @param phCode Phase code for the desired branch
	 * @param dTdD Desired ray parameter in seconds/degree
	 * @return Source-receiver distance in degrees
	 * @throws Exception If the desired arrival doesn't exist
	 */
	public double oneRay(String phCode, double dTdD) throws Exception {
		String tmpCode;
		
		if(phCode.contains("bc")) tmpCode = TauUtil.phSeg(phCode)+"ab";
		else tmpCode = phCode;
		for(lastBrn=0; lastBrn<branches.length; lastBrn++) {
			if(tmpCode.equals(branches[lastBrn].phCode)) {
					return branches[lastBrn].oneRay(dTdD);
			}
		}
		throw new Exception();
	}
	
	/**
	 * Compute the distance and travel-time for the portion of a 
	 * surface focus ray cut off by the source depth.  This provides 
	 * the distance needed to correct a down-going ray to surface 
	 * focus.
	 * 
	 * @param phCode Phase code of the phase being corrected
	 * @param dTdD Desired ray parameter in seconds/degree
	 * @return Distance cut off in degrees
	 * @throws Exception If the up-going arrival doesn't exist
	 */
	public double upRay(String phCode, double dTdD) throws Exception {
		char type = phCode.charAt(0);
		if(type == 'p' || type == 'P') lastBrn = upBrnP;
		else if(type == 's' || type == 'S') lastBrn = upBrnS;
		else throw new Exception();
		return branches[lastBrn].oneRay(dTdD);
	}
	
	/**
	 * Getter for the time correction computed by the last call to oneRay 
	 * or upRay.
	 * 
	 * @return travel-time in seconds
	 */
	public double getTimeCorr() {
		return branches[lastBrn].getTimeCorr();
	}
	
	/**
	 * Find the indices of the up-going P and S phases.
	 */
	private void findUpBrn() {
		// Initialize the pointers just in case.
		upBrnP = -1;
		upBrnS = -1;
		// Find the up-going P type branch.
		for(int j=0; j<branches.length; j++) {
			if(branches[j].exists && branches[j].ref.isUpGoing && 
					branches[j].ref.typeSeg[0] == 'P') {
				upBrnP = j;
				break;
			}
		}
		// Fine the up-going S type branch.
		for(int j=0; j<branches.length; j++) {
			if(branches[j].exists && branches[j].ref.isUpGoing && 
					branches[j].ref.typeSeg[0] == 'S') {
				upBrnS = j;
				break;
			}
		}
	}
		
	/**
	 * Print global or header data for debugging purposes.
	 */
	public void dumpHead() {
		System.out.println("\n     "+ref.modelName);
		System.out.format("Normalization: xNorm =%11.4e  pNorm =%11.4e  "+
				"tNorm =%11.4e vNorm =%11.4e\n", cvt.xNorm, cvt.pNorm, cvt.tNorm, 
				cvt.vNorm);
		System.out.format("Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  "+
				"zConrad =%7.1f\n", cvt.zUpperMantle, cvt.zMoho, cvt.zConrad);
		System.out.format("Derived: rSurface =%8.1f  zNewUp = %7.1f  "+
				"dTdDel2P =%11.4e  dTdDepth = %11.4e\n", cvt.rSurface, cvt.zNewUp, 
				cvt.dTdDelta, dTdDepth);
	}
	
	/**
	 * Print a summary table of branch information similar to the FORTRAN 
	 * Ttim range list.
	 * 
	 * @param useful If true, only print "useful" crustal phases
	 */
	public void dumpTable(boolean useful) {
		System.out.println("\nPhase          pRange          xRange    "+
				"pCaustic difLim    Flags");
		for(int j=0; j<branches.length; j++) {
			branches[j].forTable(useful);
		}
	}
	
	/**
	 * Print model parameters for debugging purposes.
	 * 
	 * @param typeMod Wave type ('P' or 'S')
	 * @param nice If true print the model in dimensional units
	 */
	public void dumpMod(char typeMod, boolean nice) {
		if(typeMod == 'P') {
			ref.pModel.dumpMod(nice);
		} else if(typeMod == 'S') {
			ref.sModel.dumpMod(nice);
		}
	}
	
	/**
	 * Print data for one travel-time branch for debugging purposes.  
	 * 
	 * @param iBrn Branch number
	 * @param full If true, print the detailed branch specification as well
	 * @param all If true print even more specifications
	 * @param sci if true, print in scientific notation
	 * @param useful If true, only print "useful" crustal phases
	 */
	public void dumpBrn(int iBrn, boolean full, boolean all, boolean sci, 
			boolean useful) {
		branches[iBrn].dumpBrn(full, all, sci, useful);
	}
	
	/**
	 * Print data for one travel-time segment for debugging purposes.
	 * 
	 * @param seg Segment phase code
	 * @param full If true, print the detailed branch specification as well
	 * @param all If true print even more specifications
	 * @param sci if true, print in scientific notation
	 * @param useful If true, only print "useful" crustal phases
	 */
	public void dumpBrn(String seg, boolean full, boolean all, boolean sci,
			boolean useful) {
		for(int j=0; j<branches.length; j++) {
			if(branches[j].getPhSeg().equals(seg)) 
				branches[j].dumpBrn(full, all, sci, useful);
		}
	}
	
	/**
	 * Print data for all travel-time segments for debugging purposes.
	 * 
	 * @param full If true, print the detailed branch specification as well
	 * @param all If true print even more specifications
	 * @param sci if true, print in scientific notation
	 * @param useful If true, only print "useful" crustal phases
	 */
	public void dumpBrn(boolean full, boolean all, boolean sci, 
			boolean useful) {
		for(int j=0; j<branches.length; j++) {
			branches[j].dumpBrn(full, all, sci, useful);
		}
	}
	
	/**
	 * Print data for one corrected up-going branch for debugging purposes.
	 * 
	 * @param typeUp Wave type ('P' or 'S')
	 * @param full If true print the up-going tau values as well
	 */
	public void dumpCorrUp(char typeUp, boolean full) {
		if(typeUp == 'P') {
			pUp.dumpCorrUp(full);
		} else if(typeUp == 'S') {
			sUp.dumpCorrUp(full);
		}
	}
	
	/**
	 * Print data for one decimated up-going branch for debugging purposes.
	 * 
	 * @param typeUp Wave type ('P' or 'S')
	 * @param full If true print the up-going tau values as well
	 */
	public void dumpDecUp(char typeUp, boolean full) {
		if(typeUp == 'P') {
			pUp.dumpDecUp(full);
		} else if(typeUp == 'S') {
			sUp.dumpDecUp(full);
		}
	}
}
