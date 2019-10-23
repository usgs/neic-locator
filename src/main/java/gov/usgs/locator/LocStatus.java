package gov.usgs.locator;

/**
 * The LocStatus enum manages the internal status and exit conditions.
 *
 * @author Ray Buland
 */
public enum LocStatus {
  /** Internal success status. */
  SUCCESS(0), // Success

  /** Internal status tagging the starting hypocenter. */
  INITIAL_HYPOCENTER(1), // Flag for the initial audit hypocenter

  /** Internal status for a held hypocenter. */
  HELD_HYPOCENTER(2), // Flag for a held hypocenter

  /** Internal status tagging the final hypocenter. */
  FINAL_HYPOCENTER(3), // Flag for the final audit hypocenter

  /**
   * Internal status for a singular matrix occuring in the hypocenter evaluation (i.e., error bars,
   * etc.).
   */
  SINGULAR_MATRIX(10), // The close out matrix decomposition failed

  /**
   * Internal status for insufficient data. A minimum of three stations are needed for a location.
   */
  INSUFFICIENT_DATA(11), // Not enough data to iterate

  /**
   * Internal status for a mismatch between the linear and non-linear steps. This occurs when the
   * linear step results in an increase in the dispersion, but there is no damping that leads to a
   * new minimum.
   */
  UNSTABLE_SOLUTION(12), // Unable to improve the solution

  /** Internal status for a location that can't be improved, but has clearly not converged. */
  DID_NOT_CONVERGE(13), // Unable to improve, but not close to converging

  /** Internal status for a location that can't be improved, but has sort of converged. */
  NEARLY_CONVERGED(14), // Unable to improve, but close to converging

  /**
   * Internal status for a phase re-identification in the middle of a stage iteration. This
   * condition forces the iteration to restart.
   */
  PHASEID_CHANGED(15), // Phase identification has changed

  /**
   * Internal status denoting that the iteration limit was reached during a stage before
   * convergence.
   */
  FULL_ITERATIONS(16), // Stage went to full iterations

  /**
   * Internal status meaning that the source depth was out of range (usually too deep). This is
   * trapped in the travel-time package, so it should never happen.
   */
  BAD_DEPTH(17), // Depth out of range

  /**
   * Internal status meaning that the eigenvalue decomposition to determine the error ellipse or
   * ellipsoid has failed.
   */
  ELLIPSOID_FAILED(18), // Failure in computing the error ellipsoid

  /** External (exit) status for a successful location. */
  SUCESSFUL_LOCATION(0), // Normal completion

  /**
   * External (exit) status for a successful location that was essentially the same as the starting
   * location.
   */
  DID_NOT_MOVE(2), // Normal completion, but didn't change location

  /**
   * External (exit) status representing that there were internal errors related to computing error
   * bars or error ellipsoids.
   */
  ERRORS_NOT_COMPUTED(3), // Normal completion, but error computation failed

  /**
   * External (exit) status catch all. If this is returned, it probably means that a new internal
   * status wasn't converted to an external status.
   */
  UNKNOWN_STATUS(4), // Just in case...

  /**
   * External (exit) status meaning that the location failed due to lack of data or some other
   * problem.
   */
  LOCATION_FAILED(101), // Location failed (singular or insufficient data)

  /**
   * External (exit) status meaning that the event input data was broken in some indecipherable way.
   */
  BAD_EVENT_INPUT(110), // Bad read (or other input) on the event data

  /** External (exit) status meaning that the travel-time data wasn't where it should have been. */
  BAD_READ_TT_DATA(113), // Unable to read travel-time data

  /** External status meaning that the auxiliary data wasn't where it should have been. */
  BAD_READ_AUX_DATA(114); // Unable to read auxiliary data

  /** An int representing the status value as an integer. */
  private final int status;

  /**
   * The LocStatus constructor. This constructor initializes the status value.
   *
   * @param status An int containing the status value
   */
  LocStatus(int status) {
    this.status = status;
  }

  /**
   * This function returns the status value.
   *
   * @return An int containing the status value
   */
  int getStatus() {
    return status;
  }
}
