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
	TtFlags flags;
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
		if(delta < 0d || Double.isNaN(delta) || azimuth < 0 || 
				Double.isNaN(azimuth)) {
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
		int lastTT, delTT = -1;
		double delCorUp, delCorDn, retDelta = Double.NaN, 
				retAzim = Double.NaN, reflCorr = Double.NaN;
		double[] xs;
		String tmpCode;
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
		// Set up distance and azimuth for retrograde phases.
		if(complex) {
			retDelta = 360d-staDelta;
			retAzim = 180d+staAzim;
			if(retAzim > 360d) retAzim -= 360d;
		}
		
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
						System.out.println(tTime);
						flags = ref.auxtt.findFlags(tTime.phCode);
						if(tTime.phCode.charAt(0) == 'L') {
							// Travel-time corrections and correction to surface focus 
							// don't make sense for surface waves.
							delCorUp = 0d;
						} else {
							// Get the correction to surface focus.
							try {
								delCorUp = upRay(tTime.phCode, tTime.dTdD);
							} catch (Exception e) {
								// This should never happen.
								e.printStackTrace();
								delCorUp = 0d;
							}
							// This is the normal case.  Do various travel-time corrections.
							tTime.tt += elevCorr(tTime.phCode, elev, tTime.dTdD, rstt);
							// If this was a complex request, do the ellipticity and bounce 
							// point corrections.
							if(complex) {
								// The ellipticity correction is straightforward.
								tTime.tt += ellipCorr(eqLat, dSource, staDelta, staAzim);
								
								// The bounce point correction is not.  See if there is a bounce.
								if(branches[j].ref.phRefl != null) {
									// If so, we may need to do some preliminary work.
									if(branches[j].ref.phRefl.equals("SP")) {
										tmpCode = "S";
									} else if(branches[j].ref.phRefl.equals("PS")) {
										tmpCode = tTime.phCode.substring(0, 
												tTime.phCode.indexOf('S')-1);
									} else {
										tmpCode = null;
									}
									// If we had an SP or PS, get the distance of the first part.
									if(tmpCode != null) {
										try {
											delCorDn = oneRay(tmpCode, tTime.dTdD);
										} catch (Exception e) {
											// This should never happen.
											e.printStackTrace();
											delCorDn = 0.5d*staDelta;
										}
									} else {
										delCorDn = Double.NaN;
									}
									// Finally, we can do the bounce point correction.
									if(tTime.dTdD > 0d) {
										reflCorr = reflCorr(tTime.phCode, branches[j].ref, eqLat, 
												eqLon, staDelta, staAzim, tTime.dTdD, delCorUp, delCorDn);
									} else {
										reflCorr = reflCorr(tTime.phCode, branches[j].ref, eqLat, 
												eqLon, retDelta, retAzim, tTime.dTdD, delCorUp, delCorDn);
									}
									if(!Double.isNaN(reflCorr)) tTime.tt += reflCorr;
									else if(tTime.phCode.equals("pwP")) delTT = k;
									else System.out.println("Bad travel-time correction for "+
											tTime.phCode);
								} 
							}
						}
						// Add auxiliary information.
						addAux(tTime.phCode, xs[i], delCorUp, tTime);
					}
					if(delTT >= 0) {
						ttList.remove(delTT);
						delTT = -1;
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
	 * Compute the elevation correction for one phase.
	 * 
	 * @param phCode Phase code
	 * @param elev Elevation in kilometers
	 * @param dTdD Ray parameter in seconds/degree
	 * @param rstt True if RSTT is being used for crustal phases
	 * @return Elevation correction in seconds
	 */
	public double elevCorr(String phCode, double elev, double dTdD, 
			boolean rstt) {
		// We don't want any corrections if RSTT is used for regional phases.
		if(rstt && ref.auxtt.phFlags.get(phCode).isRegional) return 0d;
		
		// Otherwise, the correction depends on the phase type at the station.
		for(int j=phCode.length()-1; j>=0; j--) {
			if(phCode.charAt(j) == 'P') {
				return TauUtil.topoTT(elev, TauUtil.DEFVP, dTdD/cvt.deg2km);
			} else if(phCode.charAt(j) == 'S') {
				return TauUtil.topoTT(elev, TauUtil.DEFVS, dTdD/cvt.deg2km);
			}
		}
		// The elevation correction doesn't make sense for surface waves 
		// like LR and Lg.
		return 0d;
	}
	
	/**
	 * Compute the ellipticity correction for one phase.
	 * 
	 * @param eqLat Hypocenter geographic latitude in degrees
	 * @param depth Hypocenter depth in kilometers
	 * @param delta Source-receiver distance in degrees
	 * @param azimuth Receiver azimuth from the source in degrees
	 * @return Ellipticity correction in seconds
	 */
	public double ellipCorr(double eqLat, double depth, double delta, 
			double azimuth) {
		// Do the interpolation.
		if(flags.ellip == null) {
			return 0d;
		} else {
			return flags.ellip.getEllipCorr(eqLat, depth, delta, azimuth);
		}
	}
	
	/**
	 * Compute the bounce point correction due to the reflection 
	 * occurring at some elevation other than zero.  This will provide a 
	 * positive correction for bounces under mountains and a negative 
	 * correction for bounces at the bottom of the ocean.  Note that 
	 * pwP is a very special case as it will only exist if pP bounces 
	 * at the bottom of the ocean.
	 * 
	 * @param phCode Phase code
	 * @param brnRef Branch data reference object
	 * @param eqLat Geographic source latitude in degrees
	 * @param eqLon Source longitude in degrees
	 * @param delta Source-receiver distance in degrees
	 * @param azimuth Receiver azimuth from the source in degrees
	 * @param dTdD Ray parameter in seconds per degree
	 * @param delCorUp Source-bounce point distance in degrees for 
	 * an up-going ray
	 * @param delCorDn Source-bounce point distance in degrees for 
	 * a down-going ray
	 * @return Bounce point correction in seconds
	 */
	public double reflCorr(String phCode, BrnDataRef brnRef, double eqLat, 
			double eqLon, double delta, double azimuth, double dTdD, 
			double delCorUp, double delCorDn) {
		double elev = 0d, refDelta, refLat, refLon;
		
		// Get the distance to the bounce point by type.
		switch(brnRef.phRefl) {
			// For pP etc., we just need to tract the initial up-going ray.
			case "pP": case "sP": case "pS": case "sS":
				refDelta = delCorUp;
				break;
			// For PP etc., we need to tract the initial down-going ray. 
			case "PP": case "SS":
				refDelta = 0.5d*(delta-delCorUp);
				break;
			// For converted phases, we need to track the initial down-going 
			// ray
			case "SP": case "PS":
				refDelta = delCorDn;
				break;
			// Hopefully, this can never happen.
			default:
				System.out.println("Unknown bounce type!");
				return Double.NaN;
		}
		
		// Project the initial part of the ray to the bounce point.
		refLat = TauUtil.projLat(eqLat, eqLon, refDelta, azimuth);
		refLon = TauUtil.projLon;
		// Get the elevation at that point.
		elev = ref.auxtt.topoMap.getElev(refLat, refLon);
		
		// Do the correction.
		switch(brnRef.convRefl) {
			// Handle all reflecting P phases.
			case "PP":
				// pwP is a very special case.
				if(phCode.equals("pwP")) {
					if(elev <= -1.5d) {
						/* Like pP, we need a negative correction for bouncing at the 
						 * bottom of the ocean , but also a positive correction for 
						 * the two way transit of the water layer.  The 4.67 seconds 
						 * at the end compensates for the default delay of pwP behind 
						 * pP. */
						return 2d*(TauUtil.topoTT(elev, TauUtil.DEFVP, dTdD/cvt.deg2km)-
								TauUtil.topoTT(elev, TauUtil.DEFVW, dTdD/cvt.deg2km))-4.67d;
					} else {
						// If we're not under water, there is no pwP.
						return Double.NaN;
					}
				// This is the normal case.
				} else {
					return 2d*TauUtil.topoTT(elev, TauUtil.DEFVP, dTdD/cvt.deg2km);
				}
			// Handle all converted phases.
			case "PS": case "SP":
				return TauUtil.topoTT(elev, TauUtil.DEFVP, dTdD/cvt.deg2km)+
					TauUtil.topoTT(elev, TauUtil.DEFVS, dTdD/cvt.deg2km);
			// Handle all reflecting S phases.
			case "SS":
				return 2d*TauUtil.topoTT(elev, TauUtil.DEFVS, dTdD/cvt.deg2km);
			// Again, this should never happen.
			default:
				System.out.println("Impossible phase conversion: "+brnRef.convRefl);
				return Double.NaN;
		}
	}
	
	/**
	 * Add the phase statistics and phase flags.
	 * 
	 * @param phCode Phase code
	 * @param xs Source-receiver distance in radians
	 * @param delCorUp Surface focus correction in degrees
	 * @param tTime Travel-time object to update
	 */
	protected void addAux(String phCode, double xs, double delCorUp, 
			TTimeData tTime) {		
		double del, spd, obs;
		
		// Add the phase statistics.  First, convert distance to degrees
		del = Math.toDegrees(xs);
		// Apply the surface focus correction.
		if(phCode.charAt(0) == 'p' || phCode.charAt(0) == 's')
			// For surface reflections, just subtract the up-going distance.
			del = Math.max(del-delCorUp, 0.01d);
		// Is there a point to correcting surface waves to surface focus?
		else if(phCode.charAt(0) != 'L') 
			// For a down-going ray.
			del = del+delCorUp;
		
		// No statistics for phase that wrap around the Earth.
		if(del >= 360d || flags.ttStat == null) {
			spd = TauUtil.DEFSPREAD;
			obs = TauUtil.DEFOBSERV;
		} else {
			// Wrap distances greater than 180 degrees.
			if(del > 180d) del = 360d-del;
			// Do the interpolation.
			if(tTime.corrTt) tTime.tt += flags.ttStat.getBias(del);
			spd = flags.ttStat.getSpread(del);
			obs = flags.ttStat.getObserv(del);
		}
		// Add statistics.
		tTime.addStats(spd, obs);
		// Add flags.
//	flags = ref.auxtt.phFlags.get(phCode);
		tTime.addFlags(flags.phGroup, flags.auxGroup, flags.isRegional, flags.isDepth, 
				flags.canUse, flags.dis);
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
