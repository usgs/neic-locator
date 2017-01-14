package gov.usgs.locator;

import java.io.IOException;
import java.util.ArrayList;

public class TtMain {
	public static void main(String[] args) throws Exception {
		// Simulate a simple session request.
		String earthModel = "ak135";
		double sourceDepth = 10;
		String[] phList = null;
		// Simulate the session hand off.
		ArrayList<String> knownModels = null;
		ArrayList<AllBrnRef> modelData = null;
		ArrayList<ModConvert> converts = null;
		ReadTau readTau = null;
		AllBrnRef allRef = null;
		ModConvert convert = null;
		AllBrnVol allBrn;

		if(knownModels == null) {
			knownModels = new ArrayList<String>();
			modelData = new ArrayList<AllBrnRef>();
			converts = new ArrayList<ModConvert>();
		}
		
		for(int j=0; j<knownModels.size(); j++) {
			if(knownModels.get(j).equals(earthModel)) {
				allRef = modelData.get(j);
				convert = converts.get(j);
				break;
			}
		}
		
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
			convert = new ModConvert();
			allRef = new AllBrnRef(readTau, convert);
			modelData.add(allRef);
			converts.add(convert);
		}
		
		// Set up the session.
		allBrn = new AllBrnVol(allRef, convert);
		// See what we've got.
		allBrn.dumpTable();
//	for(int j=0; j<allRef.getNoBranches(); j++) {
//		allRef.dumpBrn(j, false);
//		allBrn.dumpBrn(j, false);
//	}
//	allRef.dumpUp('P', 8);
		// Set up a new session.
		allBrn.newSession(sourceDepth, phList);
		allBrn.dumpUp('P', true);
//	allBrn.dumpUp('S', false);
	}

}
