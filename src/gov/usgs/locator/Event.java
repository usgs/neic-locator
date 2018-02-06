package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Pattern;

import gov.usgs.traveltime.TauUtil;

/**
 * Keep all data for one seismic event (earthquake usually).
 * 
 * @author Ray Buland
 *
 */
public class Event {
	// Commands:
	boolean heldLoc;			// True if the hypocenter will be held constant
	boolean heldDepth;		// True if the depth will be held constant
	boolean prefDepth;		// True if the Bayesian depth was set by an analyst
	boolean cmndRstt;			// True if regional phases will use the RSTT model
	boolean cmndCorr;			// True to use the de-correlation algorithm
	boolean restart;			// True if the hypocenter has been moved externally
	// Outputs:
	int staAssoc;					// Number of stations associated
	int staUsed;					// Number of stations used
	int phAssoc;					// Number of phases associated
	int phUsed;						// Number of phases used
	double azimGap;				// Azimuthal gap in degrees
	double lestGap;				// Robust (L-estimator) azimuthal gap in degrees
	double delMin;				// Minimum station distance in degrees
	String quality;				// Summary event quality flags for the analysts
	// Statistics:
	double seTime;				// Standard error in the origin time in seconds
	double seLat;					// Standard error in latitude in kilometers
	double seLon;					// Standard error in longitude in kilometers
	double seDepth;				// Standard error in depth in kilometers
	double seResid;				// Standard error of the residuals in seconds
	double errH;					// Maximum horizontal projection of the error ellipsoid (km)
	double errZ;					// Maximum vertical projection of the error ellipsoid (km)
	double aveH;					// Equivalent radius of the error ellipse in kilometers
	EllipAxis[] errEllip;	// Error ellipse
	double bayesImport;		// Data importance of the Bayesian depth
	//Internal use:
	int locPhUsed;				// Number of local phases used
	boolean changed;			// True if the phase identification has changed
	// Other information needed:
	Hypocenter hypo;
	ArrayList<HypoAudit> audit;
	TreeMap<StationID, Station> stations;
	ArrayList<PickGroup> groups;
	ArrayList<Pick> picks;
	ArrayList<Wresidual> wResiduals = null;
	Restimator rEst;
	StationID maxID = new StationID("~", "", "");
	// Getters:
	public Hypocenter getHypo() {return hypo;}
	public double getOriginTime() {return hypo.originTime;}
	public double getLatitude() {return hypo.latitude;}
	public double getLongitude() {return hypo.longitude;}
	public double getDepth() {return hypo.depth;}
	
	/**
	 * Allocate some storage.
	 */
	public Event() {
		stations = new TreeMap<StationID, Station>();
		groups = new ArrayList<PickGroup>();
		picks = new ArrayList<Pick>();
		audit = new ArrayList<HypoAudit>();
		wResiduals = new ArrayList<Wresidual>();
		rEst = new Restimator(wResiduals);
	}
	
	/**
	 * Read a Bulletin Hydra style event input file.  File open and 
	 * read exceptions are trapped.
	 * 
	 * @param inFile File path
	 * @return True if the read was successful
	 */
	public boolean readHydra(String inFile) {
		BufferedInputStream in;
		Scanner scan;
		char held, heldDep, prefDep, rstt, noSvd, moved, cmndUse;
		int auth;
		double origin, lat, lon, depth, bDep, bSe, elev, qual, 
			arrival, aff;
		String dbID, staCode, chaCode, netCode, locCode, curPh, 
			obsPh, lastSta = "";
		StationID staID;
		Station station;
		Pick pick;
		PickGroup group = null;
		Pattern affinity = Pattern.compile("\\d*\\.\\d*");
		
		// Set up the IO.
		try {
			in = new BufferedInputStream(new FileInputStream(inFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		scan = new Scanner(in);
		try {
			// Get the hypocenter information.
			origin = scan.nextDouble();
			lat = scan.nextDouble();
			lon = scan.nextDouble();
			depth = scan.nextDouble();
			// Get the analyst commands.
			held = scan.next().charAt(0);
			heldDep = scan.next().charAt(0);
			prefDep = scan.next().charAt(0);
			bDep = scan.nextDouble();
			bSe = scan.nextDouble();
			rstt = scan.next().charAt(0);
			noSvd = scan.next().charAt(0);
			// Fiddle because the last flag is omitted in earlier data.
			if(scan.hasNextInt()) {
				moved = 'F';
			} else {
				moved = scan.next().charAt(0);
			}
			// Create the hypocenter.
			hypo = new Hypocenter(origin, lat, lon, depth);
			// Deal with analyst commands.
			heldLoc = LocUtil.getBoolean(held);
			heldDepth = LocUtil.getBoolean(heldDep);
			prefDepth = LocUtil.getBoolean(prefDep);
			if(prefDepth) hypo.addBayes(bDep, bSe);
			cmndRstt = LocUtil.getBoolean(rstt);
			cmndCorr = !LocUtil.getBoolean(noSvd);	// True when noSvd is false
			restart = LocUtil.getBoolean(moved);
			
			// Get the pick information.
			while(scan.hasNext()) {
				// Get the station information.
				dbID = scan.next();
				staCode = scan.next();
				chaCode = scan.next();
				netCode = scan.next();
				locCode = scan.next();
				lat = scan.nextDouble();
				lon = scan.nextDouble();
				elev = scan.nextDouble();
				// Create the station.
				staID = new StationID(staCode, locCode, netCode);
				station = new Station(staID, lat, lon, elev);
				// Get the rest of the pick information.  Note that some 
				// fiddling is required as some of the positional arguments 
				// are sometimes omitted.
				qual = scan.nextDouble();
				if(scan.hasNextDouble()) {
					curPh = "";
				} else {
					curPh = scan.next();
				}
				arrival = scan.nextDouble();
				cmndUse = scan.next().charAt(0);
				auth = scan.nextInt();
				if(scan.hasNextInt() || !scan.hasNext()) {
					obsPh = "";
					aff = 0d;
				} else if(scan.hasNext(affinity)) {
					obsPh = "";
					aff = scan.nextDouble();
				} else {
					obsPh = scan.next();
					if(scan.hasNext(affinity)) {
						aff = scan.nextDouble();
					} else {
						aff = 0d;
					}
				}
				// Create the pick.
				pick = new Pick(station, chaCode, arrival, 
						LocUtil.getBoolean(cmndUse), curPh);
				pick.addIdAids(dbID, qual, obsPh, LocUtil.getAuthCode(auth), 
						aff);
				picks.add(pick);
			}
			scan.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
			
		// Sort the picks into "Hydra" input order.
		picks.sort(new PickComp());
		// Reorganize the picks into groups from the same station.
		for(int j=0; j<picks.size(); j++) {
			pick = picks.get(j);
			if(!pick.station.staID.staID.equals(lastSta)) {
				lastSta = pick.station.staID.staID;
				// Remember this station.
				stations.put(pick.station.staID, pick.station);
				// Initialize the pick group.
				group = new PickGroup(pick.station, pick);
				groups.add(group);
			} else {
				group.add(pick);
			}
		}
		// Take care of some event initialization.
		initEvent();
		return true;
	}
	
	/**
	 * Initialize the changed flag, pick and station counts, and 
	 * compute distances and azimuths.  This routine needs to be 
	 * called for any new event, no matter how it's created.
	 */
	private void initEvent() {
		/*
		 * If the location is held, it can't be moved by the Locator, but 
		 * errors will be computed as though it was free to provide a 
		 * meaningful comparison with the NEIC location.  For this reason, 
		 * it makes sense to have a held location with a held depth.  
		 * Either way, a Bayesian depth is simulated, again for error 
		 * estimation reasons.  The free depth spread assumes that the held 
		 * location is from a crustal event located by a regional network.
		 */
		if(heldLoc) {
			prefDepth = true;
			hypo.bayesDepth = hypo.depth;
			if(heldDepth) hypo.bayesSpread = LocUtil.HELDEPSE;
			else hypo.bayesSpread = LocUtil.DEFDEPSE;
		/*
		 * Although a held depth will actually hold the depth, simulate a 
		 * Bayesian depth for error computation reasons.
		 */
		} else if(heldDepth) {
			prefDepth = true;
			hypo.bayesDepth = hypo.depth;
			hypo.bayesSpread = LocUtil.HELDEPSE;
		}
		/*
		 * Treat analyst and simulated Bayesian depth commands the same.  
		 * Set the starting depth to the Bayesian depth, but don't let 
		 * the analysts get carried away and try to set the Bayesian 
		 * spread smaller than the default for a held depth.
		 */
		if(prefDepth) {
			if(hypo.bayesSpread > 0d) {
				hypo.depth = hypo.bayesDepth;
				hypo.bayesSpread = Math.max(hypo.bayesSpread, LocUtil.HELDEPSE);
			} else {
				prefDepth = false;		// Trap a bad command
			}
		}
		// Initialize the solution degrees-of-freedom.
		hypo.setDegrees(heldLoc, heldDepth);
		// Initialize changed and the depth importance.
		changed = false;
		bayesImport = 0d;
		// Allocate storage for the error ellipsoid.
		if(heldLoc) errEllip = null;
		else if(heldDepth) errEllip = new EllipAxis[2];
		else errEllip = new EllipAxis[3];
		// Do the initial station/pick statistics.
		staStats();
		// Do the initial delta-azimuth calculation.
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).updateEvent(hypo);
		}
	}
	
	/**
	 * Update event parameters when the hypocenter changes.
	 * 
	 * @param originTime Updated origin time in seconds
	 * @param latitude Updated geographic latitude in degrees
	 * @param longitude Updated longitude in degrees
	 * @param depth Updated depth in kilometers
	 */
	public void updateEvent(double originTime, double latitude, 
			double longitude, double depth) {
		// Update the hypocenter.
		hypo.updateHypo(originTime, latitude, longitude, depth);
		// Update the picks.
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).updateHypo(hypo);
			groups.get(j).updateOrigin(hypo);
		}
	}
	
	/**
	 * Update event parameters when the hypocenter changes based on a 
	 * linearized step.
	 * 
	 * @param stepLen Step length in kilometers
	 * @param dT Shift in the origin time in seconds
	 */
	public void updateHypo(double stepLen, double dT) {
		// Update the hypocenter.
		hypo.updateHypo(stepLen, dT);
		// Update the picks.
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).updateHypo(hypo);
			groups.get(j).updateOrigin(hypo);
		}
	}
	
	/**
	 * If we're just updating the origin time, we don't need to recompute 
	 * distance and azimuth.
	 * 
	 * @param dT Shift in the origin time in seconds
	 */
	public void updateOrigin(double dT) {
		hypo.updateOrigin(dT);
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).updateOrigin(hypo);
		}
	}
	
	/**
	 * Add a hypocenter audit record.  These double as fall-back 
	 * hypocenters in case the solution gets worse.
	 * 
	 * @param stage Iteration stage
	 * @param iter Iteration in this stage
	 * @param status LocStatus at the point this audit was created
	 */
	public void addAudit(int stage, int iter, LocStatus status) {
		audit.add(new HypoAudit(hypo, stage, iter, phUsed, status));
	}
	
	/**
	 * Get the number of stations.
	 * 
	 * @return Number of stations.
	 */
	public int noStations() {
		return groups.size();
	}
	
	/**
	 * Count the number of stations and picks and find the 
	 * distance to the closest station.
	 */
	public void staStats() {
		int picksUsedGrp;
		PickGroup group;
		
		staAssoc = stations.size();
		staUsed = 0;
		phAssoc = 0;
		phUsed = 0;
		locPhUsed = 0;
		delMin = TauUtil.DMAX;
		for(int j=0; j<groups.size(); j++) {
			group = groups.get(j);
			phAssoc += group.picks.size();
			picksUsedGrp = group.picksUsed();
			phUsed += picksUsedGrp;
			if(group.delta <= LocUtil.DELTALOC) locPhUsed += 
					picksUsedGrp;
			if(picksUsedGrp > 0) staUsed++;
			delMin = Math.min(delMin, group.delta);
		}
	}
	
	/**
	 * Compute the azimuthal gap and robust (L-estimator) azimuthal 
	 * gap in degrees.
	 */
	public void azimuthGap() {
		int i = 0;
		double lastAzim;
		double[] azimuths;
		
		// Trap a bad call.
		if(staUsed == 0) {
			azimGap = 360d;
			lestGap = 360d;
			return;
		}
		
		// Collect and sort the azimuths.
		azimuths = new double[staUsed];
		for(int j=0; j<groups.size(); j++) {
			if(groups.get(j).picksUsed() > 0) azimuths[i++] = 
					groups.get(j).azimuth;
		}
		Arrays.sort(azimuths);
		
		// Do the azimuthal gap.
		azimGap = 0d;
		lastAzim = azimuths[azimuths.length-1]-360d;
		for(int j=0; j<azimuths.length; j++) {
			azimGap = Math.max(azimGap, azimuths[j]-lastAzim);
			lastAzim = azimuths[j];
		}
		
		// Do the robust azimuthal gap.
		if(staUsed == 1) lestGap = 360d;
		else {
			lastAzim = azimuths[azimuths.length-2]-360d;
			lestGap = azimuths[0]-lastAzim;
			lastAzim = azimuths[azimuths.length-1]-360d;
			for(int j=1; j<azimuths.length; j++) {
				lestGap = Math.max(lestGap, azimuths[j]-lastAzim);
				lastAzim = azimuths[j-1];
			}
		}
	}
	
	/**
	 * Set the traditional NEIC quality flags.  The summary flag uses 
	 * the algorithm of Buland and Presgrave.  The secondary flags 
	 * break down the quality by epicenter and depth.
	 * 
	 * @param status Event status
	 */
	public void setQualFlags(LocStatus status) {
		char summary, epicenter, depth;
		
		// If there is insufficient data, the quality can only be "D".
		if(status == LocStatus.INSUFFICIENT_DATA) {
			quality = "D  ";
		} else {
			// If this is a GT5 event, the summary is done.
			if(LocUtil.isGT5(locPhUsed, delMin, azimGap, lestGap)) {
				summary = 'G';
			// Otherwise, set the summary quality based on the errors.
			} else {
				if(aveH <= LocUtil.HQUALIM[0] && seDepth <= LocUtil.VQUALIM[0] && 
						phUsed > LocUtil.NQUALIM[0]) summary = 'A';
				else if(aveH <= LocUtil.HQUALIM[1] && seDepth <= LocUtil.VQUALIM[1] && 
						phUsed > LocUtil.NQUALIM[1]) summary = 'B';
				else if(aveH <= LocUtil.HQUALIM[2] && seDepth <= LocUtil.VQUALIM[2]) 
					summary = 'C';
				else summary = 'D';
				// Revise the quality down if the error ellipse aspect ration is large.
				if(summary == 'A' && errEllip[0].semiLen > LocUtil.AQUALIM[0]) 
					summary = 'B';
				if((summary == 'A' || summary == 'B') && errEllip[0].semiLen > 
					LocUtil.AQUALIM[1]) summary = 'C';
				if(errEllip[0].semiLen > LocUtil.AQUALIM[2]) summary = 'D';
			}
				
			// Set the epicenter quality based on aveH.
			epicenter = '?';
			if(aveH <= LocUtil.HQUALIM[0] && phUsed > LocUtil.NQUALIM[0]) 
				epicenter = ' ';
			else if(aveH <= LocUtil.HQUALIM[1] && phUsed > LocUtil.NQUALIM[1]) 
				epicenter = '*';
			else if(aveH <= LocUtil.HQUALIM[2]) epicenter = '?';
			else summary = '!';
				
			// Set the depth quality based on seDepth.
			if(heldDepth) {
				depth = 'G';
			} else {
				if(seDepth <= LocUtil.VQUALIM[0] && phUsed > LocUtil.NQUALIM[0]) 
					depth = ' ';
				else if(seDepth <= LocUtil.VQUALIM[1] && phUsed > LocUtil.NQUALIM[1]) 
					depth = '*';
				else if(seDepth <= LocUtil.VQUALIM[2]) depth = '?';
				else depth = '!';
			}
			quality = ""+summary+epicenter+depth;
		}
	}
	
	/**
	 * Compute the maximum tangential (horizontal) and vertical (depth) 
	 * projections of the error ellipsoid in kilometers.  While not 
	 * statistically valid, these measures are commonly used by the 
	 * regional networks.
	 */
	public void sumErrors() {
		errH = 0d;
		errZ = 0d;
		for(int j=0; j<errEllip.length; j++) {
			errH = Math.max(errH, errEllip[j].tangentialProj());
			if(!heldDepth) errZ = Math.max(errZ, errEllip[j].verticalProj());
		}
	}
	
	/**
	 * Zero out most errors when no solution is possible.
	 * 
	 * @param all If true zero out everything
	 */
	public void zeroStats(boolean all) {
		seTime = 0d;
		seLat = 0d;
		seLon = 0d;
		seDepth = 0d;
		errH = 0d;
		errZ = 0d;
		aveH = 0d;
		for(int j=0; j<errEllip.length; j++) {
			errEllip[j] = new EllipAxis(0d, 0d, 0d);
		}
		if(all) seResid = 0d;
	}
	
	/**
	 * Zero out all data importances (and weights) if the importances 
	 * cannot be computed.
	 */
	public void zeroWeights() {
		hypo.depthWeight = 0d;
		for(int j=0; j<picks.size(); j++) {
			picks.get(j).weight = 0d;
		}
	}
	
	/**
	 * Print a station list.
	 */
	public void stationList() {
		Station sta;
		
		if(stations.size() > 0) {
			NavigableMap<StationID, Station> map = stations.headMap(maxID, true);
			System.out.println("\n     Station List:");
			for(@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
				sta = (Station)entry.getValue();
				System.out.println(sta);
			}
		} else {
			System.out.print("No stations found.");
		}
	}
	
	/**
	 * Print the arrivals associated with this event in a nice format.
	 * 
	 * @param first If true only print the first arrival in each pick 
	 * group
	 */
	public void printArrivals(boolean first) {
		System.out.println();
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).printArrivals(first);
		}
	}
	
	/**
	 * Dump the weighted residual storage.
	 * 
	 * @param full If true, print the derivatives as well
	 */
	public void printWres(boolean full) {
		System.out.println("\nwResiduals:");
		for(int j=0; j<wResiduals.size(); j++) {
			wResiduals.get(j).printWres(full);
		}
	}
	
	/**
	 * Print the input event information in a format similar to 
	 * the Hydra event input file.
	 */
	public void printIn() {
		System.out.format("\n%22s %8.4f %9.4f %6.2f %5b %5b %5b "+
				"%5.1f %5.1f %5b %5b\n", LocUtil.getRayDate(hypo.originTime), 
				hypo.latitude, hypo.longitude, hypo.depth, heldLoc, heldDepth, 
				prefDepth, hypo.bayesDepth, hypo.bayesSpread, cmndRstt, cmndCorr);
		System.out.println();
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).printIn();
		}
	}
	
	/**
	 * Print a Bulletin Hydra style output file.
	 */
	public void printHydra() {
		System.out.format("\n%14.3f %8.4f %9.4f %6.2f %4d %4d %4d %4d %3.0f "+
				"%8.4f\n", hypo.originTime, hypo.latitude, hypo.longitude, 
				hypo.depth, staAssoc, phAssoc, staUsed, phUsed, azimGap, delMin);
		System.out.format("%6.2f %6.1f %6.1f %6.1f %6.2f %6.1f %6.1f %6.1f "+
					"%3s %5.1f %5.1f %6.4f\n", seTime, seLat, seLon, seDepth, seResid, 
					errH, errZ, aveH, quality, hypo.bayesDepth, hypo.bayesSpread, 
					bayesImport);
		System.out.format("%14s %14s %14s  %3.0f\n", errEllip[0], errEllip[1], 
				errEllip[2], lestGap);
		System.out.println();
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).printHydra();
		}
	}
	
	/**
	 * Print an NEIC style web output.
	 */
	public void printNEIC() {
		// Print the hypocenter.
		System.out.format("\nLocation:             %-7s %-8s ±%6.1f km\n", 
				LocUtil.niceLat(hypo.latitude), LocUtil.niceLon(hypo.longitude), 
				errH);
		System.out.format("Depth:                %5.1f ±%6.1f km\n", 
				hypo.depth, errZ);
		System.out.format("Origin Time:          %23s UTC\n", 
				LocUtil.getNEICdate(hypo.originTime));
		System.out.format("Number of Stations:     %4d\n", staAssoc);
		System.out.format("Number of Phases:       %4d\n", phAssoc);
		System.out.format("Minimum Distance:     %6.1f\n", delMin);
		System.out.format("Travel Time Residual:  %5.2f\n", seTime);
		System.out.format("Azimuthal Gap:           %3.0f\n", azimGap);
		System.out.println("\n    Channel     Distance Azimuth Phase  "+
				"   Arrival Time Status    Residual Weight");
		// Sort the pick groups by distance.
		groups.sort(new GroupComp());
		// Print the picks.
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).printNEIC();
		}
	}
}
