package gov.usgs.locator;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.ArrayList;
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
		char held, heldDep, prefDep, rstt, noSvd, moved, cmndUse;
		int auth;
		String dbID, authType;
		double origin, lat, lon, depth, bDep, bSe, elev, qual, 
			arrival, aff;
		String staCode, chaCode, netCode, locCode, curPh, obsPh;
		Scanner scan;
		Pattern affinity = Pattern.compile("\\d*\\.\\d*");

		// Set up the IO.
		try {
			in = new BufferedInputStream(new FileInputStream(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		scan = new Scanner(in);
		try {
			// Get the hypocenter information.
			origin = scan.nextDouble();
			lat = scan.nextDouble();
			lon = scan.nextDouble();
			depth = scan.nextDouble();
			// Get the analyst commands.
			held = scan.next().charAt(0);
			heldDep = scan.next().charAt(0);
			prefDep = scan.next().charAt(0);
			bDep = scan.nextDouble();
			bSe = scan.nextDouble();
			rstt = scan.next().charAt(0);
			noSvd = scan.next().charAt(0);
			// Fiddle because the last flag is omitted in earlier data.
			if(scan.hasNextInt()) {
				moved = 'F';
			} else {
				moved = scan.next().charAt(0);
			}

			// create the request
			setSourceOriginTime(new Date(LocUtil.toJavaTime(origin)));
			setSourceLatitude(lat);
			setSourceLongitude(lon);
			setSourceDepth(depth);
			setIsLocationHeld(LocUtil.getBoolean(held));
			setIsDepthHeld(LocUtil.getBoolean(heldDep));
			setIsBayesianDepth(LocUtil.getBoolean(prefDep));
			setBayesianDepth(bDep);
			setBayesianSpread(bSe);
			setUseRSTT(LocUtil.getBoolean(rstt));
			setUseSVD(!LocUtil.getBoolean(noSvd)); // True when noSvd is false
			setIsLocationNew(LocUtil.getBoolean(moved));
			
			// create the pick list
			ArrayList<gov.usgs.processingformats.Pick> pickList 
				= new ArrayList<gov.usgs.processingformats.Pick>();

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
				// Get the rest of the pick information.  Note that some 
				// fiddling is required as some of the positional arguments 
				// are sometimes omitted.
				qual = scan.nextDouble();
				if(scan.hasNextDouble()) {
					curPh = null;
				} else {
					curPh = scan.next();
				}
				arrival = scan.nextDouble();
				cmndUse = scan.next().charAt(0);
				auth = scan.nextInt();
				if (auth == 0) {
					authType = "ContributedAutomatic";
				} else if (auth == 1) {
					authType = "LocalAutomatic";
				} else if (auth == 2) {
					authType = "ContributedHuman";
				} else if (auth == 3) {
					authType = "LocalHuman";
				} else {
					authType = "ContributedAutomatic";
				}

				if(scan.hasNextInt() || !scan.hasNext()) {
					obsPh = null;
					aff = 0d;
				} else if(scan.hasNext(affinity)) {
					obsPh = null;
					aff = scan.nextDouble();
				} else {
					obsPh = scan.next();
					if(scan.hasNext(affinity)) {
						aff = scan.nextDouble();
					} else {
						aff = 0d;
					}
				}

				// Create the pick.
				gov.usgs.processingformats.Pick newPick = 
					new gov.usgs.processingformats.Pick(dbID, staCode, chaCode,
						netCode, locCode, lat, lon, elev, "US", "Hydra", 
						authType, new Date(LocUtil.toJavaTime(arrival)), aff, 
						qual, LocUtil.getBoolean(cmndUse), curPh, obsPh, null, 
						null, null, null, null, null);
				
				if (newPick.isValid()) {
					// Add the pick to the list
					pickList.add(newPick);
				} else {
					ArrayList<String> errorList = newPick.getErrors();

					// combine the errors into a single string
					String errorString = new String();
					for (int i = 0; i < errorList.size(); i++) {
						errorString += " " + errorList.get(i);
					}

					System.out.println("Invalid pick: " + errorString);
				}
			}

			// add the pick list to the request
			setInputData(pickList);

			// done with file
			scan.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
