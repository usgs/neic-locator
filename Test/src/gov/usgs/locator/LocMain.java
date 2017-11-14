package gov.usgs.locator;

/**
 * Test driver for the Locator.
 * 
 * @author Ray Buland
 *
 */
public class LocMain {
	public static void main(String[] args) throws Exception {
		String inFile = "../../../Documents/Work/Events/RayLocInput1000655616_3.txt";
		Event event;
		
		// Create an event from a Hydra Locator input file.
		event = new Event();
		if(event.readHydra(inFile, false)) {
			event.printIn();
		} else {
			System.out.println("Event read failed");
		}
		
		// Update and print the event.
		event.updateEvent(event.getOriginTime(), event.getLatitude(), 
				event.getLongitude(), event.getDepth(), true);
		event.staStats();
		event.printHydra();
		
		// Print a station list
//	event.stationList();
	}
}
