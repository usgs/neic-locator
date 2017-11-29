package gov.usgs.locator;

import java.util.ArrayList;

/**
 * Holds one phase group.  Each phase group is comprised of a 
 * generic group name and a set of related phases.  Phase groups 
 * are primarily used in the Locator to facilitate identification 
 * when phases come and go (as the location changes changing the 
 * depth and distance to each station).  Although there are 
 * some special purpose phase groups, they generally come in 
 * pairs: a crust-mantle group and a core group.  For example, 
 * the P group (Pg, Pb, Pn, P and Pdif) are complemented by the 
 * PKP group (PKPdf, PKPab, PKPbc, and PKPdif).  All possible 
 * Locator phases much be in one and only one general group 
 * (although they will be duplicated in the special groups).
 * 
 * @author Ray Buland
 *
 */
public class PhGroup {
	String groupName;							// Name of the phase group
	ArrayList<String> phases;			// List of phases in the group
// boolean useInLoc;							// True if can be used in a location
	
	/**
	 * Initialize the phase group.
	 * 
	 * @param groupName Name of the phase group
	 * @param useInLoc May be used in an earthquale location if true
	 */
	protected PhGroup(String groupName) {
// protected PhGroup(String groupName, boolean useInLoc) {
		this.groupName = groupName;
//	this.groupName = groupName.trim();
//	this.useInLoc = useInLoc;
		phases = new ArrayList<String>();
	}
	
	/**
	 * Add one phase to the group.
	 * 
	 * @param phase Phase code to be added
	 */
	protected void addPhase(String phase) {
		phases.add(phase);
//	phases.add(phase.trim());
	}
	
	/**
	 * Print the contents of this phase group.
	 */
	protected void dumpGroup() {
		System.out.print(groupName+": ");
		for(int j=0; j<phases.size(); j++) {
			if(j > 0 && j%15 == 0) System.out.println();
			System.out.print(phases.get(j)+" ");
		}
		System.out.println();
//	System.out.println(" "+useInLoc);
	}
}