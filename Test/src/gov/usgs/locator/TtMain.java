package gov.usgs.locator;

import java.io.IOException;
import java.util.ArrayList;

public class TtMain {
	public static void main(String[] args) throws Exception {
		// Simulate a simple session request.
		String earthModel = "ak135";
		double sourceDepth = 12;
		String[] phList = null;
		// Directory of known models.
		ArrayList<String> knownModels = null;
		ArrayList<AllBrnRef> modelData = null;
		ArrayList<ModConvert> converts = null;
		ReadTau readTau = null;
		AllBrnRef allRef = null;
		ModConvert convert = null;
		AllBrnVol allBrn;

		// Initialize model storage if necessary.
		if(knownModels == null) {
			knownModels = new ArrayList<String>();
			modelData = new ArrayList<AllBrnRef>();
			converts = new ArrayList<ModConvert>();
		}
		
		// See if we know this model.
		allRef = null;
		for(int j=0; j<knownModels.size(); j++) {
			if(knownModels.get(j).equals(earthModel)) {
				allRef = modelData.get(j);
				convert = converts.get(j);
				break;
			}
		}
		
		// If not, set it up.
		if(allRef == null) {
			try {
				readTau = new ReadTau(earthModel);
				readTau.readHeader();
		//	readTau.dumpSegments();
		//	readTau.dumpBranches();
				readTau.readTable();
		//	readTau.dumpUp(8);
			} catch(IOException e) {
				System.out.println("Unable to read Earth model "+earthModel);
				System.exit(1);
			}
			knownModels.add(earthModel);
			convert = new ModConvert(readTau);
			allRef = new AllBrnRef(readTau, convert);
			modelData.add(allRef);
			converts.add(convert);
			allRef.dumpBrn(true);
		}
		
		// At this point, we've either found the reference part of the model 
		// or read it in.  Now Set up the (depth dependent) volatile part.
		allBrn = new AllBrnVol(allRef, convert);
		// See what we've got.
		allBrn.dumpHead();
		allBrn.dumpTable();
//	allBrn.dumpMod('P', true);
//	allBrn.dumpMod('S', true);
//	allRef.dumpUp('P', 8);
		// Set up a new session.
		allBrn.newSession(sourceDepth, phList);
//	allBrn.dumpUp('P', true);
//	allBrn.dumpUp('S', true);
//	allBrn.dumpBrn("P", true, false);
//	allBrn.dumpBrn(true, true);
	}

}
