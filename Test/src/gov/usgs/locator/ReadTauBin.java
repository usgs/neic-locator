package gov.usgs.locator;

import java.io.IOException;

public class ReadTauBin {

	public static void main(String[] args) throws IOException {
		int depIndex = 100;
		ReadTau reader;
		
		reader = new ReadTau("ak135");
		reader.readHeader();
	//	reader.dumpRec1(false);
	//	reader.dumpRec6();
		reader.dumpGlobal();
		reader.dumpSegments();
		reader.dumpBranches();
	//	reader.dumpModel(false);
	//	reader.dumpAll();
		reader.readTable();
	//	reader.dumpTable(depIndex);
		reader.dumpUp(depIndex);
	}
}
