package gov.usgs.locator;

/**
 * AuthorType is an enumeration that defines the possible pick author types and default pick
 * affinities for the Locator.
 *
 * @author Ray Buland
 */
public enum AuthorType {
  /**
   * This enumeration defines an author type for unknown picks for handing bad input, and
   * initializes the default affinity for those picks to 0.0.
   */
  UNKNOWN(0.0d),

  /**
   * This enumeration defines an author type for automatically generated picks made outside the
   * NEIC, and initializes the default affinity for those picks to 1.0.
   */
  CONTRIB_AUTO(1.0d),

  /**
   * This enumeration defines an author type for automatically generated picks made within the NEIC,
   * and initializes the default affinity for those picks to 1.0.
   */
  LOCAL_AUTO(1.0d),

  /**
   * This enumeration defines an author type for analyst picks made outside the NEIC, and
   * initializes the default affinity for those picks to 1.5.
   */
  CONTRIB_HUMAN(1.5d),

  /**
   * This enumeration defines an author type for analyst picks made within the NEIC, and initializes
   * the default affinity for those picks to 3.0.
   */
  LOCAL_HUMAN(3.0d);

  /** A double containing the default affinity value for an enumeration. */
  private final double defaultAffinity;

  /**
   * The AuthorType constructor. This constructor initializes the default affinity for this author
   * type enumeration to the provided value.
   *
   * @param defaultAffinity A double containing the default affinity to use for this type.
   */
  AuthorType(double defaultAffinity) {
    this.defaultAffinity = defaultAffinity;
  }

  /**
   * This function gets the default affinity for this author type.
   *
   * @return The default affinity as a double.
   */
  public double getDefaultAffinity() {
    return defaultAffinity;
  }

  /**
   * This function gets the author affinity based on a user supplied affinity. If the user affinity
   * is less than or equal zero, the default affinity is provided
   *
   * @param userAffinity A double value containign the affinity provided by the user. If greater
   *     than zero, this affinity will be used instead of the default affinity.
   * @return The final affinity as a double. This value will be equal to the provided user affinity
   *     if the user affinity is greater than zero. Otherwise the default affinity is returned.
   */
  public double affinity(double userAffinity) {
    // check for a positive, nonzero affinity
    if (userAffinity > 0d) {
      // use the provided affinity
      return userAffinity;
    } else {
      // use the default affinity
      return defaultAffinity;
    }
  }
}
