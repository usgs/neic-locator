package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
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
	final PhGroup regional;								// Regional phase group
	final PhGroup depth;									// Depth sensitive phase group
	final PhGroup downWeight;							// Phases to be down weighted
	final ArrayList<PhGroup> phGroups;		// List of primary phase groups
	final ArrayList<PhGroup> auxGroups;		// List of auxiliary phase groups
	final TreeMap<String, TtStat> ttStats;			// List of phase statistics
	// Set up the reader.
	final String phGroupPath = "../../../Documents/Work/Models/phgrp.dat";
	final String ttStatsPath = "../../../Documents/Work/Models/ttstats.lis";
	RandomAccessFile inGroup;
	BufferedInputStream inStats;
	Scanner scan;
	byte[] data;
	int cur = 0;
	String nextCode;

	/**
	 * Read and organize auxiliary data.  Note that for convenience 
	 * some processing is done on the travel-time statistics.  This 
	 * eliminates a stand alone program that processed the raw 
	 * (maintainable) statistics into an intermediate form for the 
	 * Locator.
	 * 
	 * @param printGrp If true, print the phase groups
	 * @param printRaw If true, print the raw statistics data
	 * @param printFit If true, print the fit statistics data
	 * @throws IOException If opens fail
	 */
	public AuxTtRef(boolean printGrp, boolean printRaw, boolean printFit) 
			throws IOException {
		TtStat ttStat;
		
		// Open and read the phase groups file.
		inGroup = new RandomAccessFile(phGroupPath, "r");
		data = new byte[(int)inGroup.length()];
		inGroup.read(data);
		// Handle local-regional phases separately.
		regional = read1Group();
		if(printGrp) {
			regional.dumpGroup();
			System.out.println();
		}
		cur += 2;
		// Handle depth phases separately.
		depth = read1Group();
		if(printGrp) {
			depth.dumpGroup();
			System.out.println();
		}
		cur += 2;
		// Handle down weighted phases separately.
		downWeight = read1Group();
		if(printGrp) {
			downWeight.dumpGroup();
			System.out.println();
		}
		cur += 2;
		
		// Handle everything else.
		phGroups = new ArrayList<PhGroup>();
		auxGroups = new ArrayList<PhGroup>();
		readGroups(printGrp);
		
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
			read1StatData(new LinearFit(ttStat), printRaw, printFit);
		} while(!statEof());
	}
	
	/**
	 * Read one phase group (i.e., one line in the phase group file).
	 * 
	 * @return Phase group just read
	 */
	protected PhGroup read1Group() {
		char[] buffer = new char[8];
		int ind;
		boolean use;
		
		// Check to see if we have a blank line.
		if(cur >= data.length) return null;
		if((char)data[cur] == '\r') {
			cur += 2;
			return null;
		}
		
		// First get the group name.
		ind = 0;
		for(int j=0; j<buffer.length; j++) buffer[j] = ' ';
		// Start by skipping leading blanks.
		while((char)data[cur] == ' ') cur++;
		// Look for the next delimiter.
		while((char)data[cur] != '*' && (char)data[cur] != ':') 
			buffer[ind++] = (char)data[cur++];
		// Check for the use flag.
		if((char)data[cur] == '*') {
			use = true;
			while((char)data[cur] != ':') cur++;
		}
		else use = false;
		// Create the group.
		PhGroup group = new PhGroup(new String(buffer), use);
		
		// Collect the phases in the group.
		do  {
			cur++;
			ind = 0;
			for(int j=0; j<buffer.length; j++) buffer[j] = ' ';
			// Skip leading blanks.
			while((char)data[cur] == ' ') cur++;
			// Look for the next delimiter.
			while((char)data[cur] != ',' && (char)data[cur] != '\r') 
				buffer[ind++] = (char)data[cur++];
			// Add the phase.
			group.addPhase(new String(buffer));
		} while((char)data[cur] != '\r');
		
		// Done with this line.
		cur += 2;
		return group;
	}
	
	/**
	 * Read in all the "normal" phase groups.  Note that they are 
	 * read in pairs, typically crust-mantle phase in the primary 
	 * group and core phases in the auxiliary group.  These pair-
	 * wise groups are used for phase identification in the Locator.
	 * 
	 * @param print List the auxiliary data as it's read if true
	 */
	protected void readGroups(boolean print) {
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
		} while(cur < data.length);
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
					return group.groupName;
				}
			}
			// Search the auxiliary phase group.
			group = auxGroups.get(j);
			if(group != null) {
				for(int k=0; k<group.phases.size(); k++) {
					if(phase.equals(group.phases.get(k))) {
						return group.groupName;
					}
				}
			}
		}
		return null;
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
	public boolean useGroup(String groupName) {
		for(int j=0; j<phGroups.size(); j++) {
			if(groupName.equals(phGroups.get(j).groupName)) {
				return phGroups.get(j).useInLoc;
			}
			if(auxGroups.get(j) != null) {
				if(groupName.equals(auxGroups.get(j).groupName)) {
					return auxGroups.get(j).useInLoc;
				}
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
	protected TtStat read1StatHead() {
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
	protected void read1StatData(LinearFit fit, boolean printRaw, 
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
	 * Check the statistics for an end-of-file.
	 * 
	 * @return True if all the data has been read.
	 */
	protected boolean statEof() {
		
		if(scan.hasNext()) return false;
		else return true;
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
		StatSeg seg;
		
		if(ttStat == null) return 0d;
		
		for(int k=0; k<ttStat.bias.size(); k++) {
			seg = ttStat.bias.get(k);
			if(delta >= seg.minDelta && delta <= seg.maxDelta) {
				return seg.interp(delta);
			}
		}
		return 0d;
	}
	
	/**
	 * Get the phase spread.
	 * 
	 * @param ttStat Pointer to the associated phase statistics
	 * @param delta Distance in degrees
	 * @return Spread in seconds at distance delta
	 */
	public double getSpread(TtStat ttStat, double delta) {
		StatSeg seg;
		
		if(ttStat == null) return 12d;
		
		for(int k=0; k<ttStat.spread.size(); k++) {
			seg = ttStat.spread.get(k);
			if(delta >= seg.minDelta && delta <= seg.maxDelta) {
				return seg.interp(delta);
			}
		}
		return 12d;
	}
	
	/**
	 * Get the phase observability.
	 * 
	 * @param ttStat Pointer to the associated phase statistics
	 * @param delta Distance in degrees
	 * @return Relative observability at distance delta
	 */
	public double getObserv(TtStat ttStat, double delta) {
		StatSeg seg;
		
		if(ttStat == null) return 0d;
		
		for(int k=0; k<ttStat.observ.size(); k++) {
			seg = ttStat.observ.get(k);
			if(delta >= seg.minDelta && delta <= seg.maxDelta) {
				return seg.interp(delta);
			}
		}
		return 0d;
	}
}

/**
 * Helper class holding one phase group.
 * 
 * @author Ray Buland
 *
 */
class PhGroup {
	String groupName;							// Name of the phase group
	ArrayList<String> phases;			// List of phases in the group
	boolean useInLoc;							// True if can be used in a location
	
	/**
	 * Initialize the phase group.
	 * 
	 * @param groupName Name of the phase group
	 * @param useInLoc May be used in an earthquale location if true
	 */
	protected PhGroup(String groupName, boolean useInLoc) {
		this.groupName = groupName.trim();
		this.useInLoc = useInLoc;
		phases = new ArrayList<String>();
	}
	
	/**
	 * Add one phase to the group.
	 * 
	 * @param phase Phase code to be added
	 */
	protected void addPhase(String phase) {
		phases.add(phase.trim());
	}
	
	/**
	 * Print the contents of this phase group.
	 */
	protected void dumpGroup() {
		System.out.print(groupName+": ");
		for(int j=0; j<phases.size(); j++) {
			if(j <= 0) System.out.print(phases.get(j));
			else System.out.print(" ,"+phases.get(j));
		}
		System.out.println(" "+useInLoc);
	}
}

/**
 * Helper class holding one set of phase statistics.
 * 
 * @author Ray Buland
 *
 */
class TtStat {
	String phCode;							// Phase code
	int minDelta;								// Minimum distance in degrees
	int maxDelta;								// Maximum distance in degrees
	ArrayList<StatSeg> bias;		// Measured arrival time bias (s)
	ArrayList<StatSeg> spread;	// Measured residual spread (s)
	ArrayList<StatSeg> observ;	// Measured observability
	
	/**
	 * Initialize the phase statistics.
	 * 
	 * @param phCode Phase code
	 * @param minDelta Minimum observed distance in degrees
	 * @param maxDelta Maximum observed distance in degrees
	 */
	protected TtStat(String phCode, int minDelta, int maxDelta) {
		this.phCode = phCode;
		this.minDelta = minDelta;
		this.maxDelta = maxDelta;
		// set up storage for the linear fits.
		bias = new ArrayList<StatSeg>();
		spread = new ArrayList<StatSeg>();
		observ = new ArrayList<StatSeg>();
	}
	
	/**
	 * Print the travel-time statistics.
	 */
	protected void dumpStats() {
		// Print the header.
		System.out.println("\n"+phCode+"     "+minDelta+"     "+maxDelta);
		
		// Print the data.
		System.out.println("Bias:");
		for(int j=0; j<bias.size(); j++) {
			System.out.format("  %3d  range = %6.2f, %6.2f  fit = %11.4e, "+
					"%11.4e\n",j,bias.get(j).minDelta,bias.get(j).maxDelta,
					bias.get(j).slope,bias.get(j).offset);
		}
		System.out.println("Spread:");
		for(int j=0; j<spread.size(); j++) {
			System.out.format("  %3d  range = %6.2f, %6.2f  fit = %11.4e, "+
					"%11.4e\n",j,spread.get(j).minDelta,spread.get(j).maxDelta,
					spread.get(j).slope,spread.get(j).offset);
		}
		System.out.println("Observability:");
		for(int j=0; j<observ.size(); j++) {
			System.out.format("  %3d  range = %6.2f, %6.2f  fit = %11.4e, "+
					"%11.4e\n",j,observ.get(j).minDelta,observ.get(j).maxDelta,
					observ.get(j).slope,observ.get(j).offset);
		}
	}
}

/**
 * Helper class holding one linear interpolation segment.
 * 
 * @author Ray Buland
 *
 */
class StatSeg {
	double minDelta;						// Minimum distance in degrees
	double maxDelta;						// Maximum distance in degrees
	double slope;								// Slope of linear interpolation
	double offset;							// Offset of linear interpolation
	
	/**
	 * Create the linear segment.
	 * 
	 * @param minDelta Minimum distance in degrees
	 * @param maxDelta Maximum distance in degrees
	 * @param slope Slope of the linear fit
	 * @param offset Offset of the linear fit
	 */
	protected StatSeg(double minDelta, double maxDelta, 
			double slope, double offset) {
		this.minDelta = minDelta;
		this.maxDelta = maxDelta;
		this.slope = slope;
		this.offset = offset;
	}
	
	/**
	 * Interpolate the linear fit at one distance.
	 * 
	 * @param delta Distance in degrees where statistics are desired
	 * @return Interpolated parameter
	 */
	protected double interp(double delta) {
		if(delta >= minDelta && delta <= maxDelta) {
			return offset+delta*slope;
		} else return(Double.NaN);
	}
}

/**
 * Helper class to acquire the 1 degree statistics data and do 
 * the linear fits.
 * 
 * @author Ray Buland
 *
 */
class LinearFit {
	double[] res, spd, obs;
	boolean[] resBrk, spdBrk, obsBrk;
	TtStat ttStat;
	int minDelta, maxDelta;
	
	/**
	 * Set up the 1 degree arrays.
	 * 
	 * @param ttStat Phase statistics object
	 */
	protected LinearFit(TtStat ttStat) {
		this.ttStat = ttStat;
		minDelta = ttStat.minDelta;
		maxDelta = ttStat.maxDelta;
		
		// set up the 1 degree statistics arrays.
		res = new double[maxDelta-minDelta+1];
		resBrk = new boolean[maxDelta-minDelta+1];
		spd = new double[maxDelta-minDelta+1];
		spdBrk = new boolean[maxDelta-minDelta+1];
		obs = new double[maxDelta-minDelta+1];
		obsBrk = new boolean[maxDelta-minDelta+1];
		// Initialize the arrays.
		for(int j=0; j<res.length; j++) {
			res[j] = Double.NaN;
			resBrk[j] = false;
			spd[j] = Double.NaN;
			spdBrk[j] = false;
			obs[j] = Double.NaN;
			obsBrk[j] = false;
		}
	}
	
	/**
	 * Add statistics for one 1 degree distance bin.
	 * 
	 * @param delta Distance in degrees at the bin center
	 * @param res Travel-time residual bias in seconds
	 * @param resBrk Break the interpolation at this bin if true
	 * @param spd Robust estimate of the scatter of travel-time 
	 * residuals
	 * @param spdBrk Break the interpolation at this bin if true
	 * @param obs Number of times this phase was observed in the 
	 * defining study
	 * @param obsBrk Break the interpolation at this bin if true
	 */
	protected void add(int delta, double res, boolean resBrk, 
			double spd, boolean spdBrk, double obs, boolean obsBrk) {
		this.res[delta-minDelta] = res;
		this.resBrk[delta-minDelta] = resBrk;
		this.spd[delta-minDelta] = spd;
		this.spdBrk[delta-minDelta] = spdBrk;
		this.obs[delta-minDelta] = obs;
		this.obsBrk[delta-minDelta] = obsBrk;
	}
	
	/**
	 * Do the linear fits for all statistics variables.
	 */
	protected void fitAll() {
		doFits(ttStat.bias, res, resBrk);
		doFits(ttStat.spread, spd, spdBrk);
		doFits(ttStat.observ, obs, obsBrk);
	}
	
	/**
	 * Do the linear fits for all segments of one statistics variable.
	 * 
	 * @param interp The ArrayList where the fits will be stored
	 * @param value Array of parameter values
	 * @param brk Array of break point flags
	 */
	protected void doFits(ArrayList<StatSeg> interp, double[] value, 
			boolean[] brk) {
		int start, end = 0;
		double startDelta, endDelta;
		double[] b;
		
		// Find the break points and invoke the fitter for each segment.
		endDelta = minDelta;
		for(int j=0; j<value.length; j++) {
			if(brk[j]) {
				start = end;
				end = j;
				startDelta = endDelta;
				endDelta = minDelta+(double)j;
				b = do1Fit(start, end, minDelta, value);
				interp.add(new StatSeg(startDelta, endDelta, b[0], b[1]));
			}
		}
		// Fit the last segment.
		start = end;
		end = value.length-1;
		startDelta = endDelta;
		endDelta = minDelta+(double)end;
		b = do1Fit(start, end, minDelta, value);
		interp.add(new StatSeg(startDelta, endDelta, b[0], b[1]));
		fixEnds(interp);
	}
	
	/**
	 * Do the linear fit for one segment of one statistics variable.
	 * 
	 * @param start Array index of the start of the segment
	 * @param end Array index of the end of the segment
	 * @param minDelta Minimum distance where the phase is observed 
	 * in degrees
	 * @param value Raw statistics data to be fit
	 * @return An array containing the fit slope and offset
	 */
	protected double[] do1Fit(int start, int end, double minDelta, 
			double[] value) {
		double[][] a = new double[2][2];
		double[] y = new double[2];
		double[] b = new double[2];
		double delta, det;
		
		// Initialize temporary storage.
		for(int i=0; i<2; i++) {
			y[i] = 0d;
			for(int j=0; j<2; j++) {
				a[i][j] = 0d;
			}
		}
		
		// Skip null bins and collect the data available.
		for(int j=start; j<=end; j++) {
			if(!Double.isNaN(value[j])) {
				delta = minDelta+j;
				y[0] += value[j]*delta;
				y[1] += value[j];
				a[0][0] += 1d;
				a[0][1] -= delta;
				a[1][1] += Math.pow(delta, 2);
			}
		}
		a[1][0] = a[0][1];
		
		// Do the fit.
		det = a[0][0]*a[1][1]-a[0][1]*a[1][0];
		b[0] = (a[0][0]*y[0]+a[0][1]*y[1])/det;
		b[1] = (a[1][0]*y[0]+a[1][1]*y[1])/det;
		return b;
	}
	
	/**
	 * Successive linear fits don't quite connect with each other at 
	 * exactly the break point distances.  Compute the actual cross-
	 * over distances and apply them to the end of one segment and 
	 * the start of the next.
	 * 
	 * @param interp ArrayList of linear fit segments for one parameter
	 */
	protected void fixEnds(ArrayList<StatSeg> interp) {
		StatSeg last, cur;
		
		cur = interp.get(0);
		for(int j=1; j<interp.size(); j++) {
			last = cur;
			cur = interp.get(j);
			last.maxDelta = -(last.offset-cur.offset)/(last.slope-cur.slope);
			cur.minDelta = last.maxDelta;
		}
	}
	
	/**
	 * Print the travel-time statistics.
	 */
	protected void dumpStats() {
		char[] flag;
		// Print the header.
		System.out.println("\n"+ttStat.phCode+"     "+minDelta+"     "+maxDelta);
		
		// If the arrays still exist, dump the raw statistics.
		flag = new char[3];
		for(int j=0; j<res.length; j++) {
			if(resBrk[j]) flag[0] = '*';
			else flag[0] = ' ';
			if(spdBrk[j]) flag[1] = '*';
			else flag[1] = ' ';
			if(obsBrk[j]) flag[2] = '*';
			else flag[2] = ' ';
			System.out.format("  %3d  %7.2f%c  %7.2f%c  %8.1f%c\n",j+minDelta,
					res[j],flag[0],spd[j],flag[1],obs[j],flag[2]);
		}
	}
}