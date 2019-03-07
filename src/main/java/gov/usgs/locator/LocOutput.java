package gov.usgs.locator;

import java.util.ArrayList;
import java.util.Date;
import java.io.PrintWriter;

import gov.usgs.processingformats.*;


/**
 * Locator outputs from an event relocation.  This class is designed to contain 
 * all parameters resulting from a Locator pass.  An object of this class should 
 * be handed to the output routines to be returned to the caller.
 * 
 * @author Ray Buland
 *
 */
public class LocOutput extends LocationData {
	int exitCode;						// Exit code

	public LocOutput() {
		super();
	}
	
	/**
	 * The following hypocenter parameters are produced by an event relocation.
	 * 
	 * @param originTime Source origin time in milliseconds.
	 * @param sourceLat Geographic source latitude in degrees.
	 * @param sourceLon Geographic source longitude in degrees.
	 * @param sourceDepth Source depth in kilometers.
	 * @param noStations Number of stations associated.
	 * @param noPicks Number of picks associated.
	 * @param stationsUsed Number of stations used.
	 * @param picksUsed Number of picks used.
	 * @param azimuthGap Standard azimuthal gap in degrees.
	 * @param azimuthGap2 Robust azimuthal gap in degrees.
	 * @param minDelta Minimum source-receiver distance in degrees.
	 * @param qualityFlags Location quality flags.
	 */
	public LocOutput(long originTime, double sourceLat, double sourceLon, 
			double sourceDepth, int noStations, int noPicks, 
			int stationsUsed, int picksUsed, double azimuthGap, 
			double azimuthGap2, double minDelta, String qualityFlags) {

		// create subobjects
		setHypocenter(new gov.usgs.processingformats.Hypocenter());
		setErrorEllipse(new gov.usgs.processingformats.ErrorEllipse());
		setAssociatedData(new ArrayList<gov.usgs.processingformats.Pick>());

		// fill in information
		getHypocenter().setTime(new Date(originTime));
		getHypocenter().setLatitude(sourceLat);
		getHypocenter().setLongitude(sourceLon);
		getHypocenter().setDepth(sourceDepth);
		setNumberOfAssociatedStations(noStations);
		setNumberOfAssociatedPhases(noPicks);
		setNumberOfUsedStations(stationsUsed);
		setNumberOfUsedPhases(picksUsed);
		setGap(azimuthGap);
		setSecondaryGap(azimuthGap2);
		setMinimumDistance(minDelta);
		setQuality(qualityFlags);
	}
	
	/**
	 * The following error parameters are produced by an event relocation.
	 * 
	 * @param timeError Origin time error in seconds.
	 * @param latError Latitude 90% confidence interval in kilometers.
	 * @param lonError Longitude 90% confidence interval in kilometers.
	 * @param depthError Depth 90% confidence interval in kilometers.
	 * @param stdError Median travel-time residual in seconds.
	 * @param errh Summary horizontal (tangential) error in kilometers.
	 * @param errz Summary vertical (radial) error in kilometers.
	 * @param avh Equivalent radius of the error ellipse in kilometers.
	 * @param bayesDepth Final Bayesian depth in kilometers.
	 * @param bayesSpread Final Bayesian standard deviation in kilometers.
	 * @param depthImport Data important for the depth.
	 * @param ellipsoid 90% error ellipsoid in kilometers.
	 * @param exitCode Final location exit code
	 */
	public void addErrors(double timeError, double latError, double lonError, 
			double depthError, double stdError, double errh, double errz, 
			double avh, double bayesDepth, double bayesSpread, double depthImport, 
			EllipAxis[] ellipsoid, int exitCode) {

		getHypocenter().setTimeError(timeError);
		getHypocenter().setLatitudeError(latError);
		getHypocenter().setLongitudeError(lonError);
		getHypocenter().setDepthError(depthError);
		setRms(stdError);
		getErrorEllipse().setMaximumHorizontalProjection(errh);
		getErrorEllipse().setMaximumVerticalProjection(errz);
		getErrorEllipse().setEquivalentHorizontalRadius(avh);
		getErrorEllipse().setE0Error(ellipsoid[0].semiLen);
		getErrorEllipse().setE0Azimuth(ellipsoid[0].azimuth);
		getErrorEllipse().setE0Dip(ellipsoid[0].plunge);
		getErrorEllipse().setE1Error(ellipsoid[1].semiLen);
		getErrorEllipse().setE1Azimuth(ellipsoid[1].azimuth);
		getErrorEllipse().setE1Dip(ellipsoid[1].plunge);
		getErrorEllipse().setE2Error(ellipsoid[2].semiLen);
		getErrorEllipse().setE2Azimuth(ellipsoid[2].azimuth);
		getErrorEllipse().setE2Dip(ellipsoid[2].plunge);
		setBayesianDepth(bayesDepth);
		setBayesianRange(bayesSpread);
		setDepthImportance(depthImport);

		this.exitCode = exitCode;
	}
	
	/**
	 * The following parameters are produced for each pick.
	 * 
	 * @param source Source of the database pick ID (optional).
	 * @param authType Type (e.g., human or auto) of the original phase 
	 * identification
	 * @param pickID Hydra database pick ID (optional)
	 * @param stationCode Station code.
	 * @param componentCode Component code.
	 * @param networkCode Network code.
	 * @param locationCode Location code.
	 * @param stationLatitude station latitude
	 * @param stationLongitude station longitude
	 * @param stationElevation station elevation 
	 * @param pickTime Pick time in milliseconds.
	 * @param locatorPhase Final seismic phase code.
	 * @param residual Pick residual in seconds.
	 * @param delta Source-receiver distance in degrees.
	 * @param azimuth Receiver azimuth (clockwise from north) in degrees.
	 * @param weight Pick weight.
	 * @param pickImport Pick data importance.
	 * @param useFlag True if the pick was used in the location.
	 * @param pickAffinity The higher the affinity, the harder it is to re-identify 
	 * a pick.  By default, the affinity for the four author types would be 
	 * 1.0, 1.0, 1.5, and 3.0 respectively.
	 * @param pickQuality The pick standard deviation in seconds.
	 * @param errorCode Summary pick error code.
	 */
	public void addPick(String source, AuthorType authType, String pickID, 
			String stationCode, String componentCode, String networkCode, 
			String locationCode, double stationLatitude, double stationLongitude, 
			double stationElevation, long pickTime, String locatorPhase, 
			String originalPhase, double residual, double delta, double azimuth, 
			double weight, double pickImport, boolean useFlag, 
			double pickAffinity, double pickQuality, String errorCode) {
	
		// source conversion
		String[] sourceArray = source.split("\\|", -1);

		// source type conversion
		String typeString = "ContributedAutomatic";
		switch(authType) {
			case CONTRIB_AUTO: // automatic contributed
				typeString = "ContributedAutomatic"; 
				break;
			case LOCAL_AUTO: // automatic NEIC
				typeString = "LocalAutomatic"; 
				break;
			case CONTRIB_HUMAN: // analyst contributed
				typeString = "ContributedHuman"; 
				break;
			case LOCAL_HUMAN: // NEIC analyst
				typeString = "LocalHuman"; 
				break;
		}

    // empty phases become null in proc formats
    if(originalPhase.equals("")) {
      originalPhase = null;
    }
    if(locatorPhase.equals("")) {
      locatorPhase = null;
    }

		// note no place for pick quality or pick error code
		getAssociatedData().add(new gov.usgs.processingformats.Pick(pickID, 
			stationCode, componentCode, networkCode, locationCode, 
			stationLatitude, stationLongitude, stationElevation, sourceArray[0], 
			sourceArray[1], typeString, new Date(pickTime), pickAffinity, 
			pickQuality, useFlag, null, originalPhase, locatorPhase, residual,
			delta, azimuth, weight, pickImport));
	}
	
	/**
	 * Write a Bulletin Hydra style output file.
	 */
	public boolean writeHydra(String filePath) {
		try {
			PrintWriter fileWriter = new PrintWriter(filePath, "UTF-8");

			fileWriter.format("\n%14.3f %8.4f %9.4f %6.2f %4d %4d %4d %4d %3.0f " +
					"%8.4f\n", 
					LocUtil.toHydraTime(getHypocenter().getTime().getTime()),
					getHypocenter().getLatitude(), getHypocenter().getLongitude(),
					getHypocenter().getDepth(), getNumberOfAssociatedStations(),
					getNumberOfAssociatedPhases(), getNumberOfUsedStations(),
					getNumberOfUsedPhases(), getGap(), getMinimumDistance());
			fileWriter.format("%6.2f %6.1f %6.1f %6.1f %6.2f %6.1f %6.1f %6.1f "+
				"%3s %5.1f %5.1f %6.4f\n", getHypocenter().getTimeError(), 
				getHypocenter().getLatitudeError(), 
				getHypocenter().getLongitudeError(), 
				getHypocenter().getDepthError(), getRMS(), 
				getErrorEllipse().getMaximumHorizontalProjection(), 
				getErrorEllipse().getMaximumVerticalProjection(), 
				getErrorEllipse().getEquivalentHorizontalRadius(), 
				getQuality(), getBayesianDepth(), getBayesianRange(), 
				getDepthImportance());
			fileWriter.format("%6.1f %3.0f %3.0f ", getErrorEllipse().getE0Error(), 
				getErrorEllipse().getE0Azimuth(), getErrorEllipse().getE0Dip());	
			fileWriter.format("%6.1f %3.0f %3.0f ", getErrorEllipse().getE1Error(), 
			getErrorEllipse().getE1Azimuth(), getErrorEllipse().getE1Dip());
			fileWriter.format("%6.1f %3.0f %3.0f  ", getErrorEllipse().getE2Error(), 
			getErrorEllipse().getE1Azimuth(), getErrorEllipse().getE2Dip());
			fileWriter.format("%3.0f\n", 0.0); //lestGap); LocationDat does not have Robust (L-estimator) azimuthal gap  

			// picks
			for(int j=0; j<getAssociatedData().size(); j++) {
				fileWriter.print(writeHydraPick(getAssociatedData().get(j)));
			}

			// done with file
			fileWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Write the pick part of a Bulletin Hydra style output file.
	 */
	public String writeHydraPick(gov.usgs.processingformats.Pick pick) {
		return(new String().format("%-10s %-5s %-3s %-2s %-2s %-8s%6.1f %5.1f "+
				"%3.0f %1s %4.2f %6.4f\n", pick.getID(), 
				pick.getSite().getStation(), pick.getSite().getChannel(),
				pick.getSite().getNetwork(), pick.getSite().getLocation(),
				pick.getLocatedPhase(), pick.getResidual(), pick.getDistance(),
				pick.getAzimuth(), LocUtil.getBoolChar(pick.getUse()),
				pick.getWeight(), pick.getImportance()));
	}

/**
	 * Print an NEIC style web output.
	 */
	/*public void printNEIC() {
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
	}*/

	/**
	 * Print out picks in the group in a way similar to the NEIC web format.
	 */
	/*public void printNEIC() {
		switch(authorType) {
			case CONTRIB_HUMAN: case LOCAL_HUMAN:
				System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s "+
						" manual    %6.1f    %4.2f\n", networkCode, stationCode, 
						componentCode, locationCode, delta, azimuth, locatorPhase, 
						LocUtil.getNEICtime(pickTime), residual, weight);
				break;
			default:
				System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s "+
						" automatic  %6.1f    %4.2f\n", networkCode, stationCode, 
						componentCode, locationCode, delta, azimuth, locatorPhase, 
						LocUtil.getNEICtime(pickTime), residual, weight);
				break;
		}
	}*/
}
