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
	TreeMap<StationID, Station> stations;
	ArrayList<Pick> allPicks;
	ArrayList<Pick> usedPicks;
	StationID maxID = new StationID("~", "", "");
	
	/**
	 * Allocate some storage.
	 */
	public Event() {
		stations = new TreeMap<StationID, Station>();
		allPicks = new ArrayList<Pick>();
		usedPicks = new ArrayList<Pick>();
	}
	
	/**
	 * Read a Bulletin Hydra style event input file.
	 * 
	 * @param inFile File path
	 * @param sort If true sort the picks into arrival time order
	 * @return True if the read was successful
	 * @throws IOException If the file open fails
	 */
	public boolean readHydra(String inFile, boolean sort) throws IOException {
		BufferedInputStream in;
		Scanner scan;
		char heldLoc, heldDep, analDep, rstt, noSvd, use;
		int auth;
		double origin, lat, lon, depth, bDep, bSe, elev, qual, arrival, aff;
		String dbID, staCode, chaCode, netCode, locCode, curPh, obsPh;
		StationID staID;
		Station sta;
		Pick pick;
		
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
			analDep = scan.next().charAt(0);
			bDep = scan.nextDouble();
			bSe = scan.nextDouble();
			rstt = scan.next().charAt(0);
			noSvd = scan.next().charAt(0);
			if(analDep == 'T') depth = bDep;
			hypo = new Hypocenter(origin, lat, lon, depth);
			hypo.addFlags(LocUtil.getBoolean(heldLoc), LocUtil.getBoolean(heldDep), 
					LocUtil.getBoolean(rstt), LocUtil.getBoolean(noSvd));
			if(analDep == 'T') hypo.addBayes(bDep, bSe);
			
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
				// Create the station ID and station object.
				staID = new StationID(staCode, locCode, netCode);
				sta = new Station(staID, lat, lon, elev);
				// Remember this station.
				stations.put(staID, sta);
				// Get the arrival information.
				qual = scan.nextDouble();
				if(scan.hasNextDouble()) {
					curPh = "";
				} else {
					curPh = scan.next();
				}
				arrival = scan.nextDouble();
				use = scan.next().charAt(0);
				auth = scan.nextInt();
				obsPh = scan.next();
				aff = scan.nextDouble();
				// Create the pick.
				pick = new Pick(sta, chaCode, arrival, LocUtil.getBoolean(use), 
						curPh);
				// Remember this pick.
				pick.addIdAids(dbID, qual, obsPh, LocUtil.getAuthCode(auth), aff);
				allPicks.add(pick);
				if(use == 'T') usedPicks.add(pick);
			}
			scan.close();
			in.close();
			// If requested sort the picks into arrival time order.
			if(sort) {
				allPicks.sort(new PickComp());
				usedPicks.sort(new PickComp());
			}
			// Do the initial delta-azimuth calculation.
			for(int j=0; j<usedPicks.size(); j++) {
				usedPicks.get(j).updatePick(hypo);
			}
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Getter for origin time.
	 * 
	 * @return Origin time in seconds
	 */
	public double getOriginTime() {return hypo.originTime;}
	
	/**
	 * Getter for latitude.
	 * 
	 * @return Geographic latitude in degrees
	 */
	public double getLatitude() {return hypo.latitude;}
	
	/**
	 * Getter for longitude.
	 * 
	 * @return Longitude in degrees
	 */
	public double getLongitude() {return hypo.longitude;}
	
	/**
	 * Getter for depth.
	 * 
	 * @return Depth in kilometers
	 */
	public double getDepth() {return hypo.depth;}
	
	/**
	 * Update event parameters when the hypocenter changes.
	 * 
	 * @param originTime Updated origin time in seconds
	 * @param latitude Updated geographic latitude in degrees
	 * @param longitude Updated longitude in degrees
	 * @param depth Updated depth in kilometers
	 * @param all If true, update all picks rather than used picks
	 */
	public void updateEvent(double originTime, double latitude, 
			double longitude, double depth, boolean all) {
		// Update the hypocenter.
		hypo.updateHypo(originTime, latitude, longitude, depth);
		// Update the picks.
		if(all) {
			for(int j=0; j<allPicks.size(); j++) {
				usedPicks.get(j).updatePick(hypo);
			}
		} else {
			for(int j=0; j<usedPicks.size(); j++) {
				usedPicks.get(j).updatePick(hypo);
			}
		}
	}
	
	public void staStats() {
		Station sta;
		
		// Most of these are easy.
		noStations = stations.size();
		noPicks = allPicks.size();
		picksUsed = usedPicks.size();
		// The number of stations used is a little harder.  First clear 
		// the used flag in all stations.
		stationsUsed = 0;
		if(stations.size() > 0) {
			NavigableMap<StationID, Station> map = stations.headMap(maxID, true);
			for(@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
				sta = (Station)entry.getValue();
				sta.used = false;
			}
			// Then reset the station used flag from the pick used flags.
			for(int j=0; j<usedPicks.size(); j++) {
				usedPicks.get(j).updateUsed();
			}
			// Finally, count the stations used.
			for(@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
				sta = (Station)entry.getValue();
				if(sta.used) stationsUsed++;
			}
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
		for(int j=0; j<allPicks.size(); j++) {
			allPicks.get(j).printIn();
		}
	}
	
	/**
	 * Print a Bulletin Hydra style output file.
	 */
	public void printHydra() {
		hypo.printHydra(noStations, stationsUsed, noPicks, picksUsed);
		System.out.println();
		for(int j=0; j<allPicks.size(); j++) {
			allPicks.get(j).printHydra();
		}
	}
}
