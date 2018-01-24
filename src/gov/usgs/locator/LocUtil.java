package gov.usgs.locator;
import gov.usgs.traveltime.TauUtil;
import java.util.Calendar;
import java.util.Date;

/**
 * Locator static constants and utilities.
 * 
 * @author Ray Buland
 *
 */
public class LocUtil {
	/**
	 * Maximum the epicenter can move and still be considered 
	 * to be the same as the starting epicenter.
	 */
	public static final double DELTATOL = 3d;
	/**
	 * Maximum the depth can move and still be considered to 
	 * be the same as the starting depth.
	 */
	public static final double DEPTHTOL = 5d;
	/**
	 * Minimum depth the Locator will allow.
	 */
	public static final double DEPTHMIN = 1d;
	/**
	 * Maximum depth the Locator will allow.
	 */
	public static final double DEPTHMAX = 700d;
	/**
	 * Default Bayesian depth standard error in kilometers.
	 */
	public static final double DEFDEPSE = 15d;
	/**
	 * Factor to down weight undesirable phase identifications.
	 */
	public static final double DOWNWEIGHT = 0.5d;
	/**
	 * Factor to down weight phases that don't match, but are 
	 * in the same group.
	 */
	public static final double GROUPWEIGHT = 0.5d;
	/**
	 * Factor to down weight phases that don't even match in type.
	 */
	public static final double TYPEWEIGHT = 0.1d;
	/**
	 * Default trial affinity when the phases don't match.
	 */
	public static final double NULLAFFINITY = 1d;
	/**
	 * Minimum acceptable observability for an alternative phase 
	 * identification.
	 */
	public static final double MINOBSERV = 1d;
	/**
	 * Association tolerance in seconds.
	 */
	public static final double ASSOCTOL = 60d;
	/**
	 * The maximum acceptable ratio of apparently misidentified first 
	 * arrivals that are being used to total used stations.
	 */
	public static final double BADRATIO = 0.1d;
	/**
	 * Conversion from degrees to kilometers (assumes the 
	 * radius of the Earth model is 6371 kilometers).
	 */
	public static final double DEG2KM = 6371d*Math.PI/180d;
	/**
	 * Normalization to make the median absolute deviation (MAD, also 
	 * referred to as the spread in the Locator) match the standard 
	 * deviation for a Gaussian distribution.
	 */
	public static final double MADNORM = 1.482580d;
	/**
	 * Maximum number of iteration stages to attempt.
	 */
	public static int STAGELIM = 5;
	/**
	 * Start each iteration loop with this step length.
	 */
	public static final double INITSTEP = 50d;
	/**
	 * Maximum number of iterations for each stage.
	 */
	public static final int[] ITERLIM = {15, 20, 20, 20, 20};
	/**
	 * Convergence criteria in kilometers for each stage.
	 */
	public static final double[] CONVLIM = {1d, 0.1d, 0.1d, 0.1d, 0.1d};
	/**
	 * Maximum step length in kilometers to allow for each stage.
	 */
	public static final double[] STEPLIM = {200d, 50d, 20d, 20d, 20d};
	/**
	 * Step tolerance dividing "did not converge" from "unstable solution".
	 */
	public static final double STEPTOL = 20d;
	/**
	 * Chi-squared test for "nearly converged".
	 */
	public static final double ALMOST = 1.1d;
	/**
	 * The Locator always uses all phases (i.e., a null phase list).
	 */
	public static final String[] PHLIST = null;
	/**
	 * If true suppress phases that are unlikely to be observed.
	 */
	public static final boolean USEFUL = true;
	/**
	 * If true, suppress back branches.
	 */
	public static final boolean NOBACKBRN = true;
	/**
	 * True if the decorrelation algorithm is to be used.  Note that 
	 * it is never used in the initial pass, hence the default.
	 */
	public static boolean deCorrelate = false;
	/**
	 * If true use the RSTT 2.5D model for local phases.  Note that 
	 * it is never used in the initial pass, hence the default.
	 */
	public static boolean rstt = false;
	/**
	 * If false the event is in a craton with a well defined Conrad 
	 * discontinuity and distinct Pb and Sb phases.  If true, the 
	 * event is in a tectonic area where Pb and Sb are extensions 
	 * of Pg and Sg respectively.
	 */
	public static boolean tectonic = false;
	/**
	 * If true, compute a hypocenter.  If false, compute only an epicenter.  
	 * If the depth is held, epicenter is always true, but it may also be 
	 * true if there is insufficient data to compute a hypocenter.
	 */
	public static boolean epicenter = false;
	/**
	 * The higher the debug level, the more output you get.
	 */
	public static int deBugLevel = 0;
	/**
	 * Receiver azimuth relative to the source in degrees clockwise from 
	 * north (available after calling delAz).
	 */
	public static double azimuth = Double.NaN;
	/**
	 * The jiggle value is a semi-random number used to make step length 
	 * damping behave better in pathological cases.
	 */
	public static double dampVal = 0.45d;
	/**
	 * The jiggle limit is used to keep the jiggle value in a reasonable 
	 * range.
	 */
	public static final double DAMPLIM = 0.58984375d+(dampVal-0.375d);
	/**
	 * Constants needed by ttResModel.
	 */
	private final static double ttResWidth = 1.001691d;								// Model spread
	private final static double cauchyFraction = 0.45d;								// Fraction of Cauchy/Gaussian
	private final static double cauchyWidth = 0.78d/ttResWidth;				// Cauchy spread
	private final static double cauchyNorm = cauchyFraction/Math.PI;	// Cauchy normalization
	private final static double gaussWidth = 0.92d/ttResWidth;				// Gaussian spread
	private final static double gaussNorm = (1d-cauchyFraction)/			// Gaussian normalization
			Math.sqrt(2d*Math.PI);
	/**
	 * Constants needed by deltaCorr.
	 */
	private final static double DELCORRMIN = 20d;				// Minimum distance to boost the FoM
	private final static double DELCORRFAC = 0.067d;		// Factor to boost the FoM
	/**
	 * Constants needed by the validLim.
	 */
	private final static double VALIDSLOPE = 2.27d;
	private final static double VALIDOFFSET = 5d;
	
	/**
	 * Compute the source-receiver distance and the receiver azimuth.  
	 * An historically significant subroutine from deep time (1962)!  This 
	 * routine was written by Bob Engdahl in Fortran (actually in the days 
	 * before subroutines) and beaten into it's current Fortran form by 
	 * Ray Buland in the early 1980s.  It's optimized with respect to 
	 * computing sines and cosines (probably still worthwhile) and it 
	 * computes exactly what's needed--no more, no less.  Note that the 
	 * azimuth is returned in static variable azimuth.
	 * 
	 * @param hypo Hypocenter object
	 * @param sta Station object
	 * @return Distance (delta) in degrees
	 */
	public static double delAz(Hypocenter hypo, Station sta) {
		double cosdel, sindel, tm1, tm2;	// Use Bob Engdahl's variable names
		
		// South Pole:
		if(sta.sinLat <= TauUtil.DTOL) {
			azimuth = 180d;
			return Math.toDegrees(Math.PI-Math.acos(hypo.cosLat));
		}
		
		// Compute some intermediate variables.
		cosdel = hypo.sinLat*sta.sinLat*(sta.cosLon*hypo.cosLon+
				sta.sinLon*hypo.sinLon)+hypo.cosLat*sta.cosLat;
		tm1 = sta.sinLat*(sta.sinLon*hypo.cosLon-sta.cosLon*hypo.sinLon);
		tm2 = hypo.sinLat*sta.cosLat-hypo.cosLat*sta.sinLat*
				(sta.cosLon*hypo.cosLon+sta.sinLon*hypo.sinLon);
		sindel = Math.sqrt(Math.pow(tm1,2d)+Math.pow(tm2,2d));
		
		// Do the azimuth.
		if(Math.abs(tm1) <= TauUtil.DTOL && Math.abs(tm2) <= TauUtil.DTOL) {
			azimuth = 0d;		// North Pole.
		} else {
			azimuth = Math.toDegrees(Math.atan2(tm1,tm2));
			if(azimuth < 0d) azimuth += 360;
		}
		
		// Do delta.
		if(sindel <= TauUtil.DTOL && Math.abs(cosdel) <= TauUtil.DTOL) {
			return 0d;
		} else {
			return Math.toDegrees(Math.atan2(sindel,cosdel));
		}
	}
	
	/**
	 * Compute the epicentral distance between two hypocenters.
	 * 
	 * @param hypo Hypocenter information
	 * @param audit Hypocenter audit information
	 * @return Distance between hypocenters in kilometers
	 */
	public static double delStep(Hypocenter hypo, HypoAudit audit) {
		double cosdel, sindel, tm1, tm2;	// Use Bob Engdahl's variable names
		
		// South Pole:
		if(audit.sinLat <= TauUtil.DTOL) {
			return DEG2KM*Math.toDegrees(Math.PI-Math.acos(hypo.cosLat));
		}
		
		// Compute some intermediate variables.
		cosdel = hypo.sinLat*audit.sinLat*(audit.cosLon*hypo.cosLon+
				audit.sinLon*hypo.sinLon)+hypo.cosLat*audit.cosLat;
		tm1 = audit.sinLat*(audit.sinLon*hypo.cosLon-audit.cosLon*hypo.sinLon);
		tm2 = hypo.sinLat*audit.cosLat-hypo.cosLat*audit.sinLat*
				(audit.cosLon*hypo.cosLon+audit.sinLon*hypo.sinLon);
		sindel = Math.sqrt(Math.pow(tm1,2d)+Math.pow(tm2,2d));
		
		// Do delta.
		if(sindel <= TauUtil.DTOL && Math.abs(cosdel) <= TauUtil.DTOL) {
			return 0d;
		} else {
			return DEG2KM*Math.toDegrees(Math.atan2(sindel,cosdel));
		}
	}
	
	/**
	 * Calculate the derivative of travel time with respect to latitude.
	 * 
	 * @param dTdD Derivative of travel time with respect to distance in 
	 * seconds/degree
	 * @param azimuth Azimuth of the receiver from the source in degrees
	 * @return Derivative of travel time with respect to distance in 
	 * seconds/kilometer
	 */
	public static double dTdLat(double dTdD, double azimuth) {
		return Math.cos(Math.toRadians(azimuth))*dTdD/DEG2KM;
	}
	
	/**
	 * Calculate the derivative of travel time with respect to longitude.
	 * 
	 * @param dTdD Derivative of travel time with respect to distance in 
	 * seconds/degree
	 * @param azimuth Azimuth of the receiver from the source in degrees
	 * @return Derivative of travel time with respect to distance in 
	 * seconds/kilometer
	 */
	public static double dTdLon(double dTdD, double azimuth) {
		return -Math.sin(Math.toRadians(azimuth))*dTdD/DEG2KM;
	}
	
	/**
	 * The canonical Buland statistical model for travel-time residuals 
	 * is a linear combination of a Gaussian and a Cauchy distribution.  
	 * In practice, the canonical model must be adapted for the median 
	 * and spread of the phase of interest.  This method then calculates 
	 * the value of the phase probability density function at the 
	 * desired residual.
	 * 
	 * @param residual Travel-time residual in seconds
	 * @param median Median probability density function time in seconds 
	 * for the desired phase
	 * @param spread Probability density function spread in seconds for 
	 * the desired phase
	 * @return Probability density function value for the desired residual
	 */
	public static double ttResModel(double residual, double median, 
			double spread) {
		double gaussSpread, gaussVar, cauchySpread, cauchyVar, ttResNorm;
		
		// Account for the current distribution median and spread.
		gaussSpread = spread*gaussWidth;
		gaussVar = (residual-median)/gaussSpread;
		cauchySpread = spread*cauchyWidth;
		cauchyVar = (residual-median)/cauchySpread;
		// Calculate the overall normalization.
		ttResNorm = gaussNorm/gaussSpread+cauchyNorm/cauchySpread;
		// Return the result.
		return (gaussNorm*Math.exp(-0.5d*Math.pow(gaussVar, 2d))/gaussSpread+
				cauchyNorm/(cauchySpread*(1d+Math.pow(cauchyVar, 2d))))/ttResNorm;
	}
	
	/**
	 * Compute a crude correction to the figure-of-merit to make phase 
	 * identifications at near distances more likely.  This is principally  
	 * a problem for the closest station to a subduction event due to the 
	 * complex structure.  It should only be applied to the first arrival 
	 * in any pick group.
	 * 
	 * @param delta Distance in degrees
	 * @return Correction to the phase association figure-of-merit
	 */
	public static double deltaCorr(double delta) {
		if(delta < DELCORRMIN) {
			return 1d+DELCORRFAC*(DELCORRMIN-delta);
		} else {
			return 1d;
		}
	}
	
	/**
	 * To be a valid association, the travel-time residual must be smaller 
	 * than the validity limit determined from the spread.  This is, of 
	 * course, a purely empirical limit based on years of Hydra experience.
	 * 
	 * @param spread Statistical spread in seconds
	 * @return Empirical validity limit
	 */
	public static double validLim(double spread) {
		return VALIDSLOPE*(spread-1d)+ VALIDOFFSET;
	}
	
	/**
	 * Sometimes you can jiggle a machine to jog it out of a rut.  In this 
	 * case the damping factor is jiggled semi-randomly to avoid loops when 
	 * step length damping is required.  Note that, the step length will be 
	 * damped by multiplying it by the damping factor.
	 * 
	 * @return Updated damping factor
	 */
	public static double dampFactor() {
		if(dampVal <= DAMPLIM) {
			dampVal += 0.0390625d;
		} else {
			dampVal -= 0.21875d;
		}
		return dampVal;
	}
	
	/**
	 * Compare two hypocenters.
	 * 
	 * @param hypo Hypocenter information
	 * @param audit Hypocenter audit information
	 * @return True if the hypocenters are (nearly) the same
	 */
	public static boolean hypoCompare(Hypocenter hypo, HypoAudit audit) {
		if(Math.abs(hypo.originTime-audit.originTime) <= 0.01d && 
				Math.abs(hypo.latitude-audit.latitude) <= 0.0001d && 
				Math.abs(hypo.longitude-audit.longitude) <= 0.0001d && 
				Math.abs(hypo.depth-audit.depth) <= 0.01d) return true;
		else return false;
	}
	
	/**
	 * Convert an arbitrary vector to a 2-norm unit vector.
	 * 
	 * @param vector Vector
	 */
	public static double[] unitVector(double[] vector) {
		double sum = 0d;
		
		// Be sure we have a valid vector.
		if(vector == null) return vector;
		if(vector.length < 1) return vector;
		
		// Compute the 2-norm.
		for(int j=0; j<vector.length; j++) {
			sum += Math.pow(vector[j], 2d);
		}
		// Bail if the vector is all zeros.
		if(sum == 0d) return vector;
		// Remove the norm.
		sum = Math.sqrt(sum);
		for(int j=0; j<vector.length; j++) {
			vector[j] /= sum;
		}
		return vector;
	}
	
	/**
	 * Produce a time string from a Hydra time suitable for printing.  
	 * Hydra uses doubles instead of longs, but (conveniently) the same 
	 * epoch.  The string returned is valid to milliseconds and uses 
	 * 24-hour times.
	 * 
	 * @param time Hydra date-time stamp
	 * @return Time string
	 */
	public static String getRayTime(double time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date((long)(1000d*time)));
		return String.format("%1$tH:%1$tM:%1$tS.%1$tL", cal);
	}
	
	/**
	 * Produce a date-time string from a Hydra time suitable for printing.  
	 * Hydra uses doubles instead of longs, but (conveniently) the same 
	 * epoch.  The string returned is valid to milliseconds and uses 
	 * 24-hour times.
	 * 
	 * @param time Hydra date-time stamp
	 * @return Date-time string
	 */
	public static String getRayDate(double time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date((long)(1000d*time)));
		return String.format("%1$td-%1$tb-%1$ty %1$tH:%1$tM:%1$tS.%1$tL", cal);
	}
	
	/**
	 * Produce a time string from a Hydra time suitable for printing in 
	 * the NEIC web bulletin style.
	 * 
	 * @param time Hydra date-time stamp
	 * @return Time string
	 */
	public static String getNEICtime(double time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date((long)(1000d*time)));
		return String.format("%1$tH:%1$tM:%1$tS.%1$tL", cal).substring(0, 11);
	}
	
	/**
	 * Produce a date-time string from a Hydra time suitable for printing 
	 * in the NEIC web bulletin style.
	 * 
	 * @param time Hydra date-time stamp
	 * @return Date-time string
	 */
	public static String getNEICdate(double time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date((long)(1000d*time)));
		return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL", cal);
	}
	
	/**
	 * Format latitude for printing.
	 * 
	 * @param latitude Geographic latitude in degrees
	 * @return Latitude string suitable for a bulletin
	 */
	public static String niceLat(double latitude) {
		if(latitude >= 0) {
			return String.format("%6.3f°N", latitude);
		} else {
			return String.format("%6.3f°S", -latitude);
		}
	}
	
	/**
	 * Format longitude for printing.
	 * 
	 * @param longitude Longitude in degrees
	 * @return Longitude string suitable for a bulletin
	 */
	public static String niceLon(double longitude) {
		if(longitude >= 0) {
			return String.format("%7.3f°E", longitude);
		} else {
			return String.format("%7.3f°W", -longitude);
		}
	}
	
	/**
	 *  Get the numeric authority code from the enumerated types.
	 *  
	 * @param author AuthorType
	 * @return Numeric authority code
	 */
	public static int getNumAuth(AuthorType author) {
		return author.ordinal();
	}
	
	/**
	 * Get the AuthorType from the numeric code.
	 * 
	 * @param authCode Numeric authority code
	 * @return AuthorType
	 */
	public static AuthorType getAuthCode(int authCode) {
		for(AuthorType author : AuthorType.values()) {
			if(author.ordinal() == authCode) return author;
		}
		return AuthorType.UNKNOWN;
	}
	
	/**
	 * Translate the FORTRAN style 'T'/'F' to Java style true/false.
	 * 
	 * @param log 'T' for true or 'F' for false
	 * @return equivalent boolean value
	 */
	public static boolean getBoolean(char log) {
		if(log == 'T') return true;
		else return false;
	}
	
	/**
	 * Translate the Java style true/false to FORTRAN style 'T'/'F'.
	 * 
	 * @param log Boolean value
	 * @return 'T' for true or 'F' for false
	 */
	public static char getBoolChar(boolean log) {
		if(log) return 'T';
		else return 'F';
	}
}
