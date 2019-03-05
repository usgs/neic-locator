package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.Date;

import gov.usgs.processingformats.*;

/**
 * Locator inputs needed to relocate an event.  This class is designed to contain 
 * all inputs needed for a location pass.  An object of this class should be 
 * created from the users inputs and will drive subsequent processing.
 * 
 * @author Ray Buland
 *
 */
public class LocInput extends LocationRequest {

	public LocInput() {
		super();
	}

	public LocInput(final LocationRequest request) {
		setType(request.getType());
		setEarthModel(request.getEarthModel());
		setSourceLatitude(request.getSourceLatitude());
		setSourceLongitude(request.getSourceLongitude());
		setSourceOriginTime(request.getSourceOriginTime());
		setSourceDepth(request.getSourceDepth());
		setInputData(request.getInputData());
		setIsLocationNew(request.getIsLocationNew());
		setIsLocationHeld(request.getIsLocationHeld());
		setIsDepthHeld(request.getIsDepthHeld());
		setIsBayesianDepth(request.getIsBayesianDepth());
		setBayesianDepth(request.getBayesianDepth());
		setBayesianSpread(request.getBayesianSpread());
		setUseRSTT(request.getUseRSTT());
		setUseSVD(request.getIsLocationNew());
		setOutputData(request.getOutputData());
	}

	/**
	 * Read a Bulletin Hydra style event input file.  File open and 
	 * read exceptions are trapped.
	 * 
	 * @param filePath path to hydra file
	 * @return True if the read was successful
	 */
	public boolean readHydra(String filePath) {
		BufferedInputStream in;
		Scanner scan;
		Pattern affinity = Pattern.compile("\\d*\\.\\d*");

		// Set up the IO.
		try {
			in = new BufferedInputStream(new FileInputStream(
				filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		scan = new Scanner(in);
		try {
			// Get the hypocenter information.
			setSourceOriginTime(new Date(LocUtil.toJavaTime(scan.nextDouble())));
			setSourceLatitude(scan.nextDouble());
			setSourceLongitude(scan.nextDouble());
			setSourceDepth(scan.nextDouble());
			// Get the analyst commands.
			setIsLocationHeld(LocUtil.getBoolean(scan.next().charAt(0)));
			setIsDepthHeld(LocUtil.getBoolean(scan.next().charAt(0)));
			setIsBayesianDepth(LocUtil.getBoolean(scan.next().charAt(0)));
			setBayesianDepth(scan.nextDouble());
			setBayesianSpread(scan.nextDouble());
			setUseRSTT(LocUtil.getBoolean(scan.next().charAt(0)));
			setUseSVD(!LocUtil.getBoolean(scan.next().charAt(0))); // True when noSvd is false
			// Fiddle because the last flag is omitted in earlier data.
			if(scan.hasNextInt()) {
				setIsLocationNew(false);
			} else {
				setIsLocationNew(LocUtil.getBoolean(scan.next().charAt(0)));
			}
			
			// Get the pick information.
			while(scan.hasNext()) {
				gov.usgs.processingformats.Pick newPick = 
					new gov.usgs.processingformats.Pick();

				// Get the station information.
				newPick.setId(scan.next());
				newPick.getSite().setStation(scan.next());
				newPick.getSite().setChannel(scan.next());
				newPick.getSite().setNetwork(scan.next());
				newPick.getSite().setLocation(scan.next());
				newPick.getSite().setLatitude(scan.nextDouble());
				newPick.getSite().setLongitude(scan.nextDouble());
				newPick.getSite().setElevation(scan.nextDouble());
				// Get the rest of the pick information.  Note that some 
				// fiddling is required as some of the positional arguments 
				// are sometimes omitted.
				newPick.setQuality(scan.nextDouble());
				if(scan.hasNextDouble()) {
					newPick.setLocatedPhase("");
				} else {
					newPick.setLocatedPhase(scan.next());
				}
				newPick.setTime(new Date(LocUtil.toJavaTime(scan.nextDouble())));
				newPick.setUse(LocUtil.getBoolean(scan.next().charAt(0)));
				
				int auth = scan.nextInt();
				if (auth == 0) {
					newPick.getSource().setType("ContributedAutomatic");
				} else if (auth == 1) {
					newPick.getSource().setType("LocalAutomatic");
				} else if (auth == 2) {
					newPick.getSource().setType("ContributedHuman");
				} else if (auth == 3) {
					newPick.getSource().setType("LocalHuman");
				}

				if(scan.hasNextInt() || !scan.hasNext()) {
					newPick.setAssociatedPhase("");
					newPick.setAffinity(0d);
				} else if(scan.hasNext(affinity)) {
					newPick.setAssociatedPhase("");
					newPick.setAffinity(scan.nextDouble());
				} else {
					newPick.setAssociatedPhase(scan.next());
					if(scan.hasNext(affinity)) {
						newPick.setAffinity(scan.nextDouble());
					} else {
						newPick.setAffinity(0d);
					}
				}
				// Add the pick to the list
				getInputData().add(newPick);
			}
			scan.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
