package gov.usgs.locator;

/**
 * Internal class that holds travel time versus distance data for one 
 * travel-time branch.
 * 
 * @author Ray
 *
 */
class TtBranch {
	String phase;			// Phase code.
	double[] delta;			// Array of source-receiver distances in degrees.
	double[] tt;			// Array of travel times in seconds.
	
	/**
	 * Store travel time versus distance data for one travel-time branch.
	 * 
	 * @param phase Branch phase code.
	 * @param delta Array of source-receiver distances in degrees.
	 * @param tt Corresponding array of travel times in seconds.
	 */
	TtBranch(String phase, double[] delta, double[] tt) {
		this.phase = phase;
		this.delta = delta;
		this.tt = tt;
	}
}

/**
 * Internal class that holds travel time versus distance data for all 
 * requested travel-time branches.
 * 
 * @author Ray
 *
 */
class TtCurves {
	int branches;			// Number of travel-time branches.
	double ttMax;			// Maximum travel time in any branch in seconds.
	TtBranch[] ttData;		// Data defining each travel-time branch.
	
	/**
	 * Store travel time versus distance data for all travel-time branches 
	 * requested.
	 * 
	 * @param ttMax Maximum travel time in any branch in seconds.
	 * @param ttData Array of travel time versus distance data for all branches.
	 */
	TtCurves(double ttMax, TtBranch[] ttData) {
		branches = ttData.length;
		this.ttMax = ttMax;
		this.ttData = ttData;
	}
}

/**
 * Internal class that holds travel-time statistics versus distance data 
 * for one travel-time branch.
 * 
 * @author Ray
 *
 */
class StatBranch {
	String phase;			// Phase code.
	double[] delta;			// Array of source-receiver distances in degrees.
	double[] spread;		// Array of travel-times spreads in seconds.
	double[] observ;		// Array of travel-time observabilities.
	
	/**
	 * Store travel-time statistics versus distance data for one travel-time 
	 * branch.
	 * 
	 * @param phase Branch phase code.
	 * @param delta Array of source-receiver distances in degrees.
	 * @param spread Corresponding array of travel-time spreads in seconds.
	 * @param observ Corresponding array of travel-time observabilities.
	 */
	StatBranch(String phase, double[] delta, double[] spread, double[] observ) {
		this.phase = phase;
		this.delta = delta;
		this.spread = spread;
		this.observ = observ;
	}
}

/**
 * Internal class that holds travel-time statistics versus distance for 
 * all travel-time branches.
 * 
 * @author Ray
 *
 */
class StatCurves {
	int branches;			// Number of travel-time branches.
	double maxSpread;		// Maximum spread in any branch in seconds.
	double maxObserv;		// Maximum observability in any branch.
	StatBranch[] statData;	// Data defining travel-time statistics.
	
	/**
	 * Store travel-time statistics versus distance data for all travel-time 
	 * branches requested.
	 * 
	 * @param maxSpread Maximum spread in any branch in seconds.
	 * @param maxObserv Maximum observability in any branch.
	 * @param statData Array of travel-time statistics versus distance data 
	 * for all branches.
	 */
	StatCurves(double maxSpread, double maxObserv, StatBranch[] statData) {
		branches = statData.length;
		this.maxSpread = maxSpread;
		this.maxObserv = maxObserv;
		this.statData = statData;
	}
}

/**
 * Results of a travel-time calculation.  An object of this type will be passed 
 * to the output routines to be returned to the user.
 * 
 * @author Ray
 *
 */
public class TtResult {
	String phase;			// Seismic phase code.
	double tt;				// Phase travel time in seconds.
	double dtdd;			// dT/dDelta in seconds/degree.
	double dtdh;			// dT/dDepth in seconds/kilometers.
	double dddp;			// dDelta/dP.
	double spread;			// Statistical scatter in seconds.
	double observ;			// Observability based on the relative number of hits.
	int groupTeleseism;		// Teleseismic phase group number.
	int groupAuxiliary;		// Auxiliary phase group number.
	boolean canUse;			// Phase may be used if true.
	boolean downWeight;		// Phase should be down weighted if true.
	TtCurves ttCurves;		// Travel time versus distance data.
	StatCurves statCurves;	// Travel-time statistics versus distance data.
	
	/**
	 * Collect travel-time, statistics, and flag information generated to 
	 * fulfill a user request.
	 * 
	 * @param phase Seismic phase code.
	 * @param tt Travel time in seconds.
	 * @param dtdd Derivative of travel time with respect to distance.
	 * @param dtdh Derivative of travel time with respect to depth.
	 * @param dddp Derivative of distance with respect to ray parameter.
	 * @param spread Statistical scatter observed for this phase at this distance.
	 * @param observ Relative observability of this phase determined statistically.
	 * @param groupTeleseism Teleseismic phase group number.
	 * @param groupAuxiliary Auxiliary phase group number.
	 * @param canUse If true the phase may be used for earthquake location.
	 * @param downWeight If true down weight this phase during phase identification.
	 */
	public void tt(String phase, double tt, double dtdd, double dtdh, 
			double dddp, double spread, double observ, int groupTeleseism, 
			int groupAuxiliary, boolean canUse, boolean downWeight) {
		// Remember basic travel-time information.
		this.phase = phase;
		this.tt = tt;
		this.dtdd = dtdd;
		this.dtdh = dtdh;
		this.dddp = dddp;
		// Remember the statistical parameters.
		this.spread = spread;
		this.observ = observ;
		// Remember the flag parameters.
		this.groupTeleseism = groupTeleseism;
		this.groupAuxiliary = groupAuxiliary;
		/* Note that if canUse if false, spread and observ are not meaningful and 
		 * don't need to be returned to the user. */
		this.canUse = canUse;
		this.downWeight = downWeight;
	}
	
	/**
	 * Store travel time versus distance data for all requested branches.
	 * 
	 * @param ttCurves Travel time versus distance data for all requested 
	 * branches.
	 */
	public void ttPlot(TtCurves ttCurves) {
		this.ttCurves = ttCurves;
	}
	
	/**
	 * Store travel-time statistics versus distance data for all requested 
	 * branches.
	 * 
	 * @param statCurves Travel-time statistics versus distance data for all 
	 * requested branches.
	 */
	public void statPlot(StatCurves statCurves) {
		this.statCurves = statCurves;
	}

}
