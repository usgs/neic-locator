package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Utility methods for the travel-time package.
 * 
 * @author Ray Buland
 *
 */
public class TauUtil {
	/**
	 * Global tolerance value.
	 */
	static final double DTOL = 1e-9d;
	/**
	 * Global minimum positive value.
	 */
	static final double DMIN = 1e-30d;
	/**
	 * Global maximum positive value.
	 */
	static final double DMAX = 1e30d;
	/**
	 * Minimum distance in radians for an Sn branch to proxy for Lg.
	 */
	static final double SNDELMIN = 0.035d;
	/**
	 * Lg group velocity in kilometers/second.
	 */
	static final double LGGRPVEL = 3.4d;
	/**
	 * Maximum depth in kilometers for which Lg will be added.
	 */
	static final double LGDEPMAX = 35d;
	/**
	 * LR group velocity in kilometers/second.
	 */
	static final double LRGRPVEL = 3.5d;
	/**
	 * Maximum depth in kilometers for which LR will be added.
	 */
	static final double LRDEPMAX = 55d;
	/**
	 * Maximum distance in radians for which LR will be added.
	 */
	static final double LRDELMAX = 0.698d;
	/**
	 * Global default travel-time statistical bias in seconds.
	 */
	static final double DEFBIAS = 0d;
	/**
	 * Global default travel-time statistical spread in seconds.
	 */
	static final double DEFSPREAD = 12d;
	/**
	 * Global default travel-time statistical relative observability.
	 */
	static final double DEFOBSERV = 0d;
	/**
	 * Global default surface P velocity in km/s (from ak135).
	 */
	static final double DEFVP = 05.80d;
	/**
	 * Global default surface S velocity in km/s (from ak135).
	 */
	static final double DEFVS = 3.46d;
	
	/**
	 * Minimum time in seconds that phases with the same name should 
	 * be separated.
	 */
	static final double DTCHATTER = 0.005d;
	
	/**
	 * Time interval following a phase where it's nearly impossible 
	 * to pick another phase.
	 */
	static final double DTOBSERV = 3d;
	
	/**
	 * Observability threshold to ensure observability doesn't go to zero.
	 */
	static final double MINOBSERV = 1d;
	
	/**
	 * Frequency for which DTOBSERV is half a cycle.
	 */
	static final double FREQOBSERV = Math.PI/DTOBSERV;
	
	/**
	 * Create a segment code by stripping a phase code of unnecessary 
	 * frippery.
	 * 
	 * @param phCode Phase code
	 * @return Segment code
	 */
	public static String phSeg(String phCode) {
		int index;
		String phGen;
		
		if((index = phCode.indexOf("df")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("ab")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("ac")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("g")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("g")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		if((index = phCode.indexOf("b")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("b")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		if((index = phCode.indexOf("n")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("n")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		return phCode;
	}
	
	/**
	 * Algorithmically identify phases that are unlikely to be useful for 
	 * earthquake location.  These are crustal phases that always appear in 
	 * coda of the first arriving P or S phases.
	 * 
	 * @param phCode Phase code
	 * @param strict True if Pn and Sn reflections should be included as 
	 * useless phases
	 * @return True if phase is in the crustal P or S coda
	 */
	public static boolean setUseless(String phCode, boolean strict) {
		if(strict) {
			if((phCode.contains("Pg") || phCode.contains("Sg") || 
					phCode.contains("Pb") || phCode.contains("Sb") || 
					phCode.contains("Pn") || phCode.contains("Sn")) && 
					phCode.length() > 2) return true;
			else return false;
		} else {
			if((phCode.contains("Pg") || phCode.contains("Sg") || 
					phCode.contains("Pb") || phCode.contains("Sb")) && 
					phCode.length() > 2) return true;
			else return false;
		}
	}

	/**
	 * By default, filter out phases of the same name that are so 
	 * close in time as to be useless.  This is particularly useful 
	 * when there are small instabilities in the tau interpolation.
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void filterDef(ArrayList<TTimeData> tTimes) {
		for(int j=1; j<tTimes.size(); j++) {
			if(tTimes.get(j).phCode.equals(tTimes.get(j-1).phCode) && 
					tTimes.get(j).tt-tTimes.get(j-1).tt <= DTCHATTER) {
				tTimes.remove(j--);
			}
		}
	}
	
	/**
	 * Modify the of observability of phases closely following other 
	 * phases (in time).  This is a real seat-of-the-pants hack.  The 
	 * problem is that the proximity of phases in time depends on 
	 * depth, but the statistics include all depths (corrected to 
	 * surface focus).
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void modObserv(ArrayList<TTimeData> tTimes) {
		TTimeData tTimeJ, tTimeI;
		
		// Loop backwards over the phases.
		for(int j=tTimes.size()-2; j>=0; j--) {
			tTimeJ = tTimes.get(j);
			// Loop over the phases later than the current phase.
			for(int i=j+1; i<tTimes.size(); i++) {
				tTimeI = tTimes.get(i);
				// If the phases are close together, modify the later phase.
				if(tTimeI.tt-tTimeJ.tt < DTOBSERV) {
					// If the later phase has some observability, make sure it 
					// might still be used.
					if(tTimeI.observ >= MINOBSERV) {
						tTimeI.observ = Math.max(tTimeI.observ-0.5d*tTimeJ.observ*
								(Math.cos(FREQOBSERV*(tTimeI.tt-tTimeJ.tt))+1d), 
								MINOBSERV);
					// Otherwise, let the later phase observability go to zero.
					} else {
						tTimeI.observ = Math.max(tTimeI.observ-0.5d*tTimeJ.observ*
								(Math.cos(FREQOBSERV*(tTimeI.tt-tTimeJ.tt))+1d), 
								0d);
					}
				}
			}
		}
	}
	
	/**
	 * Optionally, all seismic back branches can be filtered out.  
	 * Triplication back branches confuse the Locator, so this 
	 * filter removes all secondary phases of the same name no 
	 * matter how big the arrival time differences.
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void filterBack(ArrayList<TTimeData> tTimes) {
		for(int j=1; j<tTimes.size(); j++) {
			if(tTimes.get(j).phCode.equals(tTimes.get(j-1).phCode)) {
				tTimes.remove(j--);
			}
		}
	}
	
	/**
	 * Optionally, Pb and Sb can be renamed Pg and Sg respectively.  
	 * By default, the travel-time calculations are done for 
	 * continental cratons where there may be clear Pb (P*) and 
	 * Sb (S*) phases.  However, these just annoy the seismic 
	 * analysts in tectonic areas where they are rarely if ever 
	 * observed.  The crude hack implemented here is to rename Pb 
	 * and Sb to Pg and Sg respectively.  This has the effect of 
	 * pretending that the modeled Conrad discontinuity is a smooth 
	 * transition and that Pg and Sg may turn all the way to the 
	 * Moho.  Note that this may create new back branches, so it 
	 * should be run before the default or back branch filters.
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void filterTect(ArrayList<TTimeData> tTimes) {
		for(int j=0; j<tTimes.size(); j++) {
			// Turn Pbs into Pgs.
			if(tTimes.get(j).phCode.contains("Pb") && 
					tTimes.get(j).phCode.charAt(1) != 'K') {
				tTimes.get(j).phCode.replace("Pb", "Pg");
				tTimes.get(j).dis = false;
			}
			// Turn Sbs into Sgs.
			if(tTimes.get(j).phCode.contains("Sb") && 
					tTimes.get(j).phCode.charAt(1) != 'K') {
				tTimes.get(j).phCode.replace("Sb", "Sg");
				tTimes.get(j).dis = false;
			}
		}
	}
	
	/**
	 * By default, canUse only reflects phase types that make sense to 
	 * use in an earthquake Location.  However, phases can't be used 
	 * if they have no statistics either.  Setting canUse to reflect 
	 * both conditions makes it easier for the Locator.
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void modCanUse(ArrayList<TTimeData> tTimes) {
		for(int j=0; j<tTimes.size(); j++) {
			if(tTimes.get(j).spread >= DEFSPREAD || tTimes.get(j).observ <= 
					DEFOBSERV) tTimes.get(j).canUse = false;
		}
	}
}
