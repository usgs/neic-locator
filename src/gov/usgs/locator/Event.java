package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Keep all data for one seismic event (earthquake usually).
 * 
 * @author Ray Buland
 *
 */
public class Event {
	int noStations;					// Number of stations associated
	int stationsUsed;				// Number of stations used
	int noPicks;						// Number of picks associated
	int picksUsed;					// Number of picks used
	Hypocenter hypo;
	ArrayList<HypoAudit> audit;
	TreeMap<StationID, Station> stations;
	ArrayList<PickGroup> groups;
	ArrayList<Pick> picks;
	ArrayList<Pick> usedPicks = null;
//	ArrayList<Pick> unusedPicks = null;
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
		audit = new ArrayList<HypoAudit>();
		stations = new TreeMap<StationID, Station>();
		groups = new ArrayList<PickGroup>();
		picks = new ArrayList<Pick>();
		wResiduals = new ArrayList<Wresidual>();
		rEst = new Restimator(wResiduals);
	}
	
	/**
	 * Read a Bulletin Hydra style event input file.
	 * 
	 * @param inFile File path
	 * @return True if the read was successful
	 * @throws IOException If the file open fails
	 */
	public boolean readHydra(String inFile) throws IOException {
		BufferedInputStream in;
		Scanner scan;
		char heldLoc, heldDep, prefDep, rstt, noSvd, restart, cmndUse;
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
			scan = new Scanner(in);
			
			// Get the hypocenter information.
			origin = scan.nextDouble();
			lat = scan.nextDouble();
			lon = scan.nextDouble();
			depth = scan.nextDouble();
			// Get the analyst commands.
			heldLoc = scan.next().charAt(0);
			heldDep = scan.next().charAt(0);
			prefDep = scan.next().charAt(0);
			bDep = scan.nextDouble();
			bSe = scan.nextDouble();
			rstt = scan.next().charAt(0);
			noSvd = scan.next().charAt(0);
			if(scan.hasNextInt()) {
				restart = 'F';
			} else {
				restart = scan.next().charAt(0);
			}
			// Create the hypocenter.
			hypo = new Hypocenter(origin, lat, lon, depth);
			hypo.addFlags(LocUtil.getBoolean(heldLoc), 
					LocUtil.getBoolean(heldDep), LocUtil.getBoolean(rstt), 
					LocUtil.getBoolean(noSvd), LocUtil.getBoolean(restart));
			if(prefDep == 'T') hypo.addBayes(bDep, bSe);
			
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
				// Get the rest of the pick information.
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
				pick = new Pick(station, chaCode, arrival, LocUtil.getBoolean(cmndUse), 
						curPh);
				pick.addIdAids(dbID, qual, obsPh, LocUtil.getAuthCode(auth), 
						aff);
				picks.add(pick);
			}
			scan.close();
			in.close();
			
			// Sort the picks into "Hydra" input order.
//		picks.sort(new PickComp());
			// Reorganize the picks into groups.
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
			// Do the initial station/pick statistics.
			staStats();
			// Do the initial delta-azimuth calculation.
			for(int j=0; j<groups.size(); j++) {
				groups.get(j).update(hypo);
			}
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Create a list of used picks.  This will correspond to the indices 
	 * in the weighted residual list.
	 */
	public void makeUsedPicks() {
		// Initialize or reinitialize usedPicks.
		if(usedPicks == null) usedPicks = new ArrayList<Pick>();
		else usedPicks.clear();
		// Populate the list.
		for(int j=0; j<picks.size(); j++) {
			if(picks.get(j).used) usedPicks.add(picks.get(j));
		}
	}
	
	/**
	 * Create a list of residuals and weights for used picks and 
	 * the Bayesian depth condition.
	 */
	public void makeWresiduals() {
		Pick pick;
		
		// Reinitialize the weighted residual storage.
		if(wResiduals.size() > 0) wResiduals.clear();
		// Make up the used picks list.
		makeUsedPicks();
		
		// Populate the weighted residual storage.
		for(int j=0; j<usedPicks.size(); j++) {
			pick = usedPicks.get(j);
			wResiduals.add(new Wresidual(false, j, pick.residual, pick.weight));
		}
		// Add the Bayesian depth condition.
		wResiduals.add(new Wresidual(true, wResiduals.size(), hypo.depthRes, 
				hypo.depthWeight));
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
			groups.get(j).update(hypo);
		}
	}
	
	/**
	 * Add a hypocenter audit record.  These double as fall-back 
	 * hypocenters in case the statistical dispersion increases.
	 * 
	 * @param stage Iteration stage
	 * @param iteration Iteration in this stage
	 * @param delH Tangential epicentral change since the last stage in 
	 * kilometers
	 * @param delZ Depth change since the last stage in kilometers
	 */
	public void addAudit(int stage, int iteration, double delH, double delZ) {
		audit.add(new HypoAudit(hypo, stage, iteration, noPicks, delH, delZ));
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
	 * Count the number of stations and picks.
	 */
	public void staStats() {
		int picksUsedGrp;
		
		noStations = stations.size();
		stationsUsed = 0;
		noPicks = 0;
		picksUsed = 0;
		for(int j=0; j<groups.size(); j++) {
			noPicks += groups.get(j).picks.size();
			picksUsedGrp = groups.get(j).picksUsed();
			picksUsed += picksUsedGrp;
			if(picksUsedGrp > 0) stationsUsed++;
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
	 * Print the input event information in a format similar to 
	 * the Hydra event input file.
	 */
	public void printIn() {
		hypo.printIn();
		System.out.println();
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).printIn();
		}
	}
	
	public void printArrivals(boolean first) {
		System.out.println();
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).printArrivals(first);
		}
	}
	
	/**
	 * Print a Bulletin Hydra style output file.
	 */
	public void printHydra() {
		hypo.printHydra(noStations, stationsUsed, noPicks, picksUsed);
		System.out.println();
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).printHydra();
		}
	}
	
	/**
	 * Print an NEIC style web output.
	 */
	public void printNEIC() {
		// Sort the pick groups by distance.
		groups.sort(new GroupComp());
		// Print the hypocenter.
		hypo.printNEIC(noStations, noPicks);
		System.out.println("\n    Channel     Distance Azimuth Phase  "+
				"   Arrival Time Status    Residual Weight");
		// Print the picks.
		for(int j=0; j<groups.size(); j++) {
			groups.get(j).printNEIC();
		}
	}
}
