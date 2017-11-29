package gov.usgs.locator;

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
	 * Array for translating the authority flag to a value.
	 */
	public final static String[] authList = {"Auto_other", "Auto_local", 
			"Human_other", "Human_local"};
	
	/**
	 * Receiver azimuth relative to the source in degrees clockwise from 
	 * north (available after calling delAz).
	 */
	public static double azimuth = Double.NaN;
	
	/**
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
	 *  Get the numeric authority code from the string code.
	 *  
	 * @param auth Authority string
	 * @return Authority number
	 */
	public static int getNumAuth(String auth) {
		for(int j=0; j<authList.length; j++) {
			if(authList[j].equals(auth)) return ++j;
		}
		return 0;
	}
	
	/**
	 * Get the string authority code from the numeric code.
	 * 
	 * @param auth Authority number
	 * @return Authority string
	 */
	public static String getAuthCode(int auth) {
		if(auth > 0 && auth <=4) return authList[auth-1];
		else return null;
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
