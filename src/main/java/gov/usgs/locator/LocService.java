package gov.usgs.locator;
import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;

import gov.usgs.traveltime.*;
import gov.usgs.processingformats.*;

public class LocService implements LocationService {

    String modelPath = null;

    /**
	 * Implement the location service interface
	 * 
	 * @param request a Final LocationRequest containing the location request
	 */
    @Override
    public LocationData getLocation(final LocationRequest request) 
        throws LocationException {
           
        LocInput in = new LocInput(request);
        return(getLocation(in));
    }
    
    public LocationData getLocation(final LocInput in) 
        throws LocationException {
        
        // init the tt models
        TTSessionLocal ttLocal = null;
        try {
            ttLocal = new TTSessionLocal(true, true, true, modelPath);
        } catch (IOException e) {
            System.out.println("Unable to read travel-time auxiliary data.");
            e.printStackTrace();
        }

        // Read the Locator auxiliary files.
        AuxLocRef auxLoc = null;
        try {
			auxLoc = new AuxLocRef(modelPath);
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Unable to read Locator auxiliary data.");
            e.printStackTrace();
        }

        // make sure we have an earth model
        if (in.getEarthModel() == null) {
            in.setEarthModel("ak135");
        }

        // setup the event
        Event event = new Event(in.getEarthModel());
        event.serverIn(in);

        // setup the locator
        Locate loc = new Locate(event, ttLocal, auxLoc);;
        
        // perform the location
        LocStatus status = loc.doLoc();
        event.setExitCode(status);

        event.printNEIC();

        // only return on a successful completion
        if (status.status() > 3) {
            return(null);
        }

        // convert output to json
        return(convertOutputToData(event.serverOut()));
    }

    public LocationData convertOutputToData(LocOutput out) {
        ArrayList<gov.usgs.processingformats.Pick> picks = 
            new ArrayList<gov.usgs.processingformats.Pick>();

        // for each pick we have
        for (Iterator<PickOutput> pickIterator = out.picks.iterator(); 
            pickIterator.hasNext();) {

            // get the next pick
            PickOutput aPick = (PickOutput) pickIterator.next();

            // source conversion
            String[] sourceArray = aPick.source.split("\\|", -1);

            // source type conversion
            String typeString = "ContributedAutomatic";
            switch(aPick.authorType) {
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
            // add the pick to the array
            picks.add(new gov.usgs.processingformats.Pick(aPick.pickID, 
                aPick.stationCode, aPick.componentCode, aPick.networkCode,
                aPick.locationCode, aPick.latitude, aPick.longitude, 
                aPick.elevation, sourceArray[0], sourceArray[1], typeString, 
                new Date(aPick.pickTime), aPick.pickAffinity, aPick.pickQuality, 
                aPick.useFlag, "", aPick.originalPhase, aPick.locatorPhase, 
                aPick.residual, aPick.delta, aPick.azimuth, aPick.weight, 
                aPick.pickImport));
        }

        // now the actual location
        LocationData data = new LocationData(out.sourceLat, out.sourceLon, 
            new Date(out.originTime), out.sourceDepth, out.latError, out.lonError,
            out.timeError, out.depthError, picks, out.noStations, out.noPicks, 
            out.stationsUsed, out.picksUsed, out.azimuthGap, out.azimuthGap2,
            out.minDelta, out.stdError, out.qualityFlags, out.bayesDepth, 
            out.bayesSpread, out.depthImport, out.ellipsoid[0].semiLen,
            out.ellipsoid[0].azimuth, out.ellipsoid[0].plunge, 
            out.ellipsoid[1].semiLen, out.ellipsoid[1].azimuth, 
            out.ellipsoid[1].plunge, out.ellipsoid[2].semiLen,
            out.ellipsoid[2].azimuth, out.ellipsoid[2].plunge, out.errh,
            out.errz, out.avh);

        return(data);
    }
}