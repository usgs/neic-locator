package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
// import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Auxiliary data augmenting the basic travel-times.  This 
 * data is common to all models and only need be read once 
 * during the travel-time server initialization.  Auxiliary 
 * data includes phase groups (needed for association in the 
 * Locator) and travel-time statistics.  The statistics are 
 * used in the travel-time package for the arrival times of 
 * add-on phases (e.g., PKPpre) and in the Locator for 
 * association and phase weighting.
 * 
 * @author Ray Buland
 *
 */
public class AuxTtRef {
	// Phase group storage.
	final PhGroup regional;									// Regional phase group
	final PhGroup depth;										// Depth sensitive phase group
	final PhGroup downWeight;								// Phases to be down weighted
	final PhGroup canUse;										// Phases that can be used for location
	final ArrayList<PhGroup> phGroups;			// List of primary phase groups
	final ArrayList<PhGroup> auxGroups;			// List of auxiliary phase groups
	// Phase statistics storage.
	final TreeMap<String, TtStat> ttStats;	// List of phase statistics
	// Ellipticity storage.
	final TreeMap<String, Ellip> ellips;		// List of ellipticity corrections
	// Flag storage by phase
	final TreeMap<String, TtFlags> phFlags;	// Phase group information by phase
	// Topography.
	final Topography topoMap;									// Topography for bounce points
	TtStat ttStat;					// Phase statistics
	Ellip ellip;						// Ellipticity correction
	// Set up the reader.
	final String phGroupPath = "../../../Documents/Work/Models/phgrp.dat";
	final String ttStatsPath = "../../../Documents/Work/Models/ttstats.lis";
	final String ellipPath = "../../../Documents/Work/Models/tau.table";
	Scanner scan;
	boolean priGroup = false;
	int nDepth;
	String nextCode;

	/**
	 * Read and organize auxiliary data.  Note that for convenience 
	 * some processing is done on the travel-time statistics.  This 
	 * eliminates a stand alone program that processed the raw 
	 * (maintainable) statistics into an intermediate form for the 
	 * Locator.
	 * 
	 * @param printGrp If true, print the phase groups
	 * @param printFlg If true, print the phase flags
	 * @param printRaw If true, print the raw statistics data
	 * @param printFit If true, print the fit statistics data
	 * @throws IOException If opens fail
	 */
	public AuxTtRef(boolean printGrp, boolean printFlg, boolean printRaw, boolean printFit) 
			throws IOException {
		BufferedInputStream inGroup, inStats, inEllip;
		EllipDeps eDepth;
		
		// Open and read the phase groups file.
		inGroup = new BufferedInputStream(new FileInputStream(phGroupPath));
		scan = new Scanner(inGroup);
		// Prime the pump.
		nextCode = scan.next();
		// Handle local-regional phases separately.
		regional = read1Group();
		if(printGrp) {
			regional.dumpGroup();
			System.out.println();
		}
		// Handle depth phases separately.
		depth = read1Group();
		if(printGrp) {
			depth.dumpGroup();
			System.out.println();
		}
		// Handle down weighted phases separately.
		downWeight = read1Group();
		if(printGrp) {
			downWeight.dumpGroup();
			System.out.println();
		}
		// Handle used phases separately.
		canUse = read1Group();
		if(printGrp) {
			canUse.dumpGroup();
			System.out.println();
		}
		
		// Handle "normal" groups.
		phGroups = new ArrayList<PhGroup>();
		auxGroups = new ArrayList<PhGroup>();
		readGroups(printGrp);
		inGroup.close();
		
		// Open and read the travel-time statistics file.
		inStats = new BufferedInputStream(new FileInputStream(ttStatsPath));
		scan = new Scanner(inStats);
		ttStats = new TreeMap<String, TtStat>();
		// Prime the pump.
		nextCode = scan.next();
		// Scan phase statistics until we run out.
		do {
			ttStat = read1StatHead();
			ttStats.put(ttStat.phCode, ttStat);
			read1StatData(new TtStatLinFit(ttStat), printRaw, printFit);
		} while(scan.hasNext());
		inStats.close();
		
		// Open and read the ellipticity correction file.
		inEllip = new BufferedInputStream(new FileInputStream(ellipPath));
		scan = new Scanner(inEllip);
		ellips = new TreeMap<String, Ellip>();
		eDepth = new EllipDeps();
		nDepth = eDepth.ellipDeps.length;
		do {
			ellip = read1Ellip();
			ellips.put(ellip.phCode, ellip);
		} while(scan.hasNext());
		inEllip.close();
		
		// Rearrange group flags, phase flags and statistics and the 
		// ellipticity correction by phase.
		phFlags = new TreeMap<String, TtFlags>();
		makePhFlags(printFlg);
		
		// Set up the topography data.
		topoMap = new Topography();
	}
	
	/**
	 * Read one phase group (i.e., one line in the phase group file).
	 * 
	 * @return Phase group just read
	 */
	private PhGroup read1Group() {
		if(nextCode.contains(":")) {
			PhGroup group = new PhGroup(nextCode.substring(0, 
					nextCode.indexOf(':')));
			nextCode = scan.next();
			while(!nextCode.contains(":") & !nextCode.contains("-")) {
				group.addPhase(nextCode);
				nextCode = scan.next();
			}
			return group;
		} else {
			if(scan.hasNext()) nextCode = scan.next();
			return null;
		}
	}
	
	/**
	 * Read in all the "normal" phase groups.  Note that they are 
	 * read in pairs, typically crust-mantle phase in the primary 
	 * group and core phases in the auxiliary group.  These pair-
	 * wise groups are used for phase identification in the Locator.
	 * 
	 * @param print List the auxiliary data as it's read if true
	 */
	private void readGroups(boolean print) {
		do {
			// Groups are added to the ArrayLists as they are created.
			phGroups.add(read1Group());
			auxGroups.add(read1Group());
			if(print) {
				phGroups.get(phGroups.size()-1).dumpGroup();
				if(auxGroups.get(auxGroups.size()-1) != null) 
					auxGroups.get(auxGroups.size()-1).dumpGroup();
				else System.out.println("    *null*");
			}
		} while(scan.hasNext());
	}
	
	/**
	 * Find the group a phase belongs to.
	 * 
	 * @param phase Phase code
	 * @return Phase group name
	 */
	public String findGroup(String phase) {
		PhGroup group;
		// Search the primary phase group.
		for(int j=0; j<phGroups.size(); j++) {
			group = phGroups.get(j);
			for(int k=0; k<group.phases.size(); k++) {
				if(phase.equals(group.phases.get(k))) {
					priGroup = true;
					return group.groupName;
				}
			}
			// Search the auxiliary phase group.
			group = auxGroups.get(j);
			if(group != null) {
				for(int k=0; k<group.phases.size(); k++) {
					if(phase.equals(group.phases.get(k))) {
						priGroup = false;
						return group.groupName;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Special version of findGroup that deals with real world 
	 * problems like a blank phase code and automatic picks 
	 * that are all identified as P.
	 * 
	 * @param phase Phase code
	 * @param auto True if the pick was done automatically
	 * @return Phase group name
	 */
	public String findGroup(String phase, boolean auto) {
		if(phase.equals("")) return "all";
		else if(auto && phase.equals("P")) return "Ploc";
		else return findGroup(phase);
	}
	
	/**
	 * The phase identification algorithm depends on knowing which 
	 * set of phase groups was found.
	 * 
	 * @return True if the last phase group found was primary
	 */
	public boolean isPrimary() {
		return priGroup;
	}
	
	/**
	 * Find the complementary phase group.  That is, if the phase 
	 * group is primary, return the associated auxiliary phase 
	 * group and vice versa.
	 * 
	 * @param groupName Phase group name
	 * @return Complementary phase group name
	 */
	public String compGroup(String groupName) {
		for(int j=0; j<phGroups.size(); j++) {
			if(groupName.equals(phGroups.get(j).groupName)) {
				if(auxGroups.get(j) != null) return auxGroups.get(j).groupName;
				else return null;
			}
			if(auxGroups.get(j) != null) {
				if(groupName.equals(auxGroups.get(j).groupName)) {
					return phGroups.get(j).groupName;
				}
			}
		}
		return null;
	}
	
	/**
	 * See if this phase group can be used for earthquake location.
	 * 
	 * @param groupName Phase group name
	 * @return True if this group can be used for earthquake location
	 */
	public boolean canUse(String phase) {
		for(int k=0; k<canUse.phases.size(); k++) {
			if(phase.equals(canUse.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * See if this is a local-regional phase.
	 * 
	 * @param phase Phase code
	 * @return True if the phase is local-regional
	 */
	public boolean isRegional(String phase) {
		for(int k=0; k<regional.phases.size(); k++) {
			if(phase.equals(regional.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * See if this is a depth phase.
	 * 
	 * @param phase Phase code
	 * @return True if the phase is depth sensitive
	 */
	public boolean isDepthPh(String phase) {
		for(int k=0; k<depth.phases.size(); k++) {
			if(phase.equals(depth.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * See if this is a down weighted phase.
	 * 
	 * @param phase Phase code
	 * @return True if the phase is to be down weighted
	 */
	public boolean isDisPh(String phase) {
		for(int k=0; k<downWeight.phases.size(); k++) {
			if(phase.equals(downWeight.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Read in the statistics header for one phase.
	 * 
	 * @return Statistics object
	 */
	private TtStat read1StatHead() {
		String phCode;
		int minDelta, maxDelta;
		TtStat ttStat;
		
		// Get the phase header.
		phCode = nextCode;
		minDelta = scan.nextInt();
		maxDelta = scan.nextInt();
		ttStat = new TtStat(phCode, minDelta, maxDelta);
		return ttStat;
	}
	
	/**
	 * Read in the statistics data for one phase.  Rather than reading 
	 * in the linear fits as in the FORTRAN version, the raw statistics 
	 * file is read and the fits are done on the fly.  This makes it 
	 * easier to maintain the statistics as the utility program that did 
	 * the fits becomes redundant.
	 * 
	 * @param fit LinearFit object
	 * @param printRaw If true, print the raw statistics data
	 * @param printFit If true, print the fit statistics data
	 */
	private void read1StatData(TtStatLinFit fit, boolean printRaw, 
			boolean printFit) {
		int delta;
		double res, spd, obs;
		boolean resBrk, spdBrk, obsBrk;
		boolean done;
		
		done = false;
		
		// Scan for the phase bias, spread, and observability.
		do{
			delta = scan.nextInt();
			res = scan.nextDouble();
			// Check for a linear fit break flag.
			if(scan.hasNextDouble()) resBrk = false;
			else {
				resBrk = true;
				if(!scan.next().equals("*")) {
					System.out.println("read1Stat: warning--the next field is "+
							"neither a number nor an astrisk?");
				}
			}
			spd = scan.nextDouble();
			// Check for a linear fit break flag.
			if(scan.hasNextDouble()) spdBrk = false;
			else {
				spdBrk = true;
				if(!scan.next().equals("*")) {
					System.out.println("read1Stat: warning--the next field is "+
							"neither a number nor an astrisk?");
				}
			}
			obs = scan.nextDouble();
			// Check for a linear fit break flag.
			if(scan.hasNextInt()) obsBrk = false;
			else {
				// This is especially fraught at the EOF.
				if(scan.hasNext()) {
					// If it's not an EOF there are still several possibilities.
					nextCode = scan.next();
					if(nextCode.equals("*")) {
						obsBrk = true;
						if(!scan.hasNextInt()) {
							done = true;
							if(scan.hasNext()) {
								nextCode = scan.next();
							} else {
								nextCode = "~";
							}
						}
					} else {
						obsBrk = false;
						done = true;
					}
				} else {
					obsBrk = false;
					done = true;
				}
			}
			fit.add(delta, res, resBrk, spd, spdBrk, obs, obsBrk);
		} while(!done);
		// Optionally, list the raw data (i.e., before the fits are done).
		if(printRaw) fit.dumpStats();
		
		// Crunch the linear fits.
		fit.fitAll();
		if(printFit) fit.ttStat.dumpStats();
		fit = null;
	}
	
	/**
	 * Find the statistics associated with the desired phase.
	 * 
	 * @param phase Phase code
	 * @return A phase statistics object
	 */
	public TtStat findStats(String phase) {
		return ttStats.get(phase);
	}
	
	/**
	 * get the phase bias.
	 * 
	 * @param ttStat Pointer to the associated phase statistics
	 * @param delta Distance in degrees
	 * @return Bias in seconds at distance delta
	 */
	public double getBias(TtStat ttStat, double delta) {
		if(ttStat == null) return TauUtil.DEFBIAS;
		else return ttStat.getBias(delta);
	}
	
	/**
	 * Get the phase spread.
	 * 
	 * @param ttStat Pointer to the associated phase statistics
	 * @param delta Distance in degrees
	 * @return Spread in seconds at distance delta
	 */
	public double getSpread(TtStat ttStat, double delta) {
		if(ttStat == null) return TauUtil.DEFSPREAD;
		else return ttStat.getSpread(delta);
	}
	
	/**
	 * Get the phase observability.
	 * 
	 * @param ttStat Pointer to the associated phase statistics
	 * @param delta Distance in degrees
	 * @return Relative observability at distance delta
	 */
	public double getObserv(TtStat ttStat, double delta) {
		if(ttStat == null) return TauUtil.DEFOBSERV;
		else return ttStat.getObserv(delta);
	}
	
	/**
	 * Read in ellipticity correction data for one phase.
	 * 
	 * @return Ellipticity object
	 */
	private Ellip read1Ellip() {
		String phCode;
		int nDelta;
		@SuppressWarnings("unused")
		double delta;
		double minDelta, maxDelta;
		double[][] t0, t1, t2;
		Ellip ellip;
		
		// Read the header.
		phCode = scan.next();
		nDelta = scan.nextInt();
		minDelta = scan.nextDouble();
		maxDelta = scan.nextDouble();
		
		// Allocate storage.
		t0 = new double[nDelta][nDepth];
		t1 = new double[nDelta][nDepth];
		t2 = new double[nDelta][nDepth];
		// Read in the tau profiles.
		for(int j=0; j<nDelta; j++) {
			delta = scan.nextDouble();		// The distance is cosmetic
			for(int i=0; i<nDepth; i++) t0[j][i] = scan.nextDouble();
			for(int i=0; i<nDepth; i++) t1[j][i] = scan.nextDouble();
			for(int i=0; i<nDepth; i++) t2[j][i] = scan.nextDouble();
		}
		// Return the result.
		ellip = new Ellip(phCode, minDelta, maxDelta, t0, t1, t2);
		return ellip;
	}
	
	/**
	 * Get the ellipticity correction data for the desired phase.
	 * 
	 * @param phCode Phase code
	 * @return Ellipticity data
	 */
	public Ellip findEllip(String phCode) {
		return ellips.get(phCode);
	}
	
	/**
	 * Reorganize the flags from ArrayLists of phases by group to 
	 * a TreeMap of flags by phase.
	 */
	private void makePhFlags(boolean print) {
		String phCode, phGroup;
		TtFlags flags;

		// Search the phase groups for phases.
		for(int j=0; j<phGroups.size(); j++) {
			phGroup = phGroups.get(j).groupName;
			for(int i=0; i<phGroups.get(j).phases.size(); i++) {
				phCode = phGroups.get(j).phases.get(i);
				unTangle(phCode, phGroup);
				phFlags.put(phCode, new TtFlags(phGroup, compGroup(phGroup), 
						isRegional(phCode), isDepthPh(phCode), canUse(phCode), 
						isDisPh(phCode), ttStat, ellip));
			}
		}
		// Search the auxiliary phase groups for phases.
		for(int j=0; j<auxGroups.size(); j++) {
			if(auxGroups.get(j) != null) {
				phGroup = auxGroups.get(j).groupName;
				for(int i=0; i<auxGroups.get(j).phases.size(); i++) {
					phCode = auxGroups.get(j).phases.get(i);
					unTangle(phCode, phGroup);
					phFlags.put(phCode, new TtFlags(phGroup, compGroup(phGroup), 
							isRegional(phCode), isDepthPh(phCode), canUse(phCode), 
							isDisPh(phCode), ttStat, ellip));
				}
			}
		}
		
		if(print) {
			NavigableMap<String, TtFlags> map = phFlags.headMap("~", true);
			System.out.println("\n     Phase Flags:");
			for(@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
				flags = (TtFlags)entry.getValue();
				System.out.format("%8s: %8s %8s  flags = %5b %5b %5b %5b", 
						entry.getKey(), flags.phGroup, flags.auxGroup, flags.canUse, 
						flags.isRegional, flags.isDepth, flags.dis);
				if(flags.ttStat == null) System.out.print("   stats = null    ");
				else System.out.format("   stats = %-8s", flags.ttStat.phCode);
				if(flags.ellip == null) System.out.println(" ellip = null");
				else System.out.format(" ellip = %s\n", flags.ellip.phCode);
			}
		}
	}
	
	/**
	 * Do some fiddling to add the statistics and ellipticity correction.
	 * 
	 * @param phCode Phase code
	 * @param phGroup Group code
	 */
	private void unTangle(String phCode, String phGroup) {		
		// Get the travel-time statistics.
		ttStat = findStats(phCode);
		// The ellipticity is a little messier.
		ellip = findEllip(phCode);
		if(ellip == null) ellip = findEllip(phGroup);
		if(ellip == null) {
			if(phCode.equals("pwP")) ellip = findEllip("pP");
			else if(phCode.equals("PKPpre")) ellip = findEllip("PKPdf");
			else if(phGroup.contains("PKP")) ellip = findEllip(phGroup+"bc");
		}
	}
	
	/**
	 * Get flags, etc. by phase code.
	 * 
	 * @param phCode Phase code
	 * @return Flags object
	 */
	public TtFlags findFlags(String phCode) {
		return phFlags.get(phCode);
	}
}