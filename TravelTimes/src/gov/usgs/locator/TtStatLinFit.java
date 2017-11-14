package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Acquire the travel-time statistics data in one degree bins, 
 * do the linear fits between the break flags, and release 
 * the raw statistics data.
 * 
 * @author Ray Buland
 *
 */
public class TtStatLinFit {
	double[] res, spd, obs;
	boolean[] resBrk, spdBrk, obsBrk;
	TtStat ttStat;
	int minDelta, maxDelta;
	
	/**
	 * Set up the 1 degree arrays.
	 * 
	 * @param ttStat Phase statistics object
	 */
	protected TtStatLinFit(TtStat ttStat) {
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
	protected void doFits(ArrayList<TtStatSeg> interp, double[] value, 
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
				interp.add(new TtStatSeg(startDelta, endDelta, b[0], b[1]));
			}
		}
		// Fit the last segment.
		start = end;
		end = value.length-1;
		startDelta = endDelta;
		endDelta = minDelta+(double)end;
		b = do1Fit(start, end, minDelta, value);
		interp.add(new TtStatSeg(startDelta, endDelta, b[0], b[1]));
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
	protected void fixEnds(ArrayList<TtStatSeg> interp) {
		TtStatSeg last, cur;
		
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