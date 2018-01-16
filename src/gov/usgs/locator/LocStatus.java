package gov.usgs.locator;

/**
 * Status and exit conditions.
 * 
 * @author Ray Buland
 *
 */
public enum LocStatus {
	// Iteration status:
	SUCCESS (0),							// Success
	INITIAL_HYPOCENTER (1),		// Flag for the initial audit hypocenter
	HELD_HYPOCENTER (2),			// Flag for a held hypocenter
	SINGULAR_MATRIX (10),			// The close out matrix decomposition failed
	INSUFFICIENT_DATA (11),		// Not enough data to iterate
	UNSTABLE_SOLUTION (12),		// Unable to improve the solution
	DID_NOT_CONVERGE (13),		// Unable to improve, but not close to converging
	NEARLY_CONVERGED (14),		// Unable to improve, but close to converging
	PHASEID_CHANGED (15),			// Phase identification has changed
	BAD_DEPTH (16),					// Depth out of range
	// Exit conditions:
	SUCESSFUL_LOCATION (0),		// Normal completion
	DID_NOT_MOVE (2),					// Normal completion, but didn't change location
	UNKNOWN_STATUS (3),				// Just in case...
	BAD_INPUT (100),					// Unable to interpret the input
	LOCATION_FAILED (101),		// Location failed (singular or insufficient data)
	BAD_EVENT_INPUT (110),		// Bad read (or other input) on the event data
	BAD_READ_TT_DATA (113),		// Unable to read travel-time data
	BAD_READ_AUX_DATA (114);	// Unable to read auxiliary data
	
	private final int status;	// Exit flag
	
	/**
	 * The constructor sets up the exit values.
	 * 
	 * @param status Exit value
	 */
	LocStatus(int status) {
		this.status = status;
	}
	
	/**
	 * Get the exit value.
	 * 
	 * @return Exit value
	 */
	int status() {
		return status;
	}
}
