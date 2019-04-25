package gov.usgs.locator;

import gov.usgs.traveltime.TauUtil;
import java.io.Serializable;

/**
 * The Craton class keeps track of the geometry of a single craton.
 *
 * @author Ray Buland
 */
public class Craton implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** A String containing the name of the craton. */
  private String name;

  /**
   * An array of doubles containing the geographic latitude points of the craton boundary in degrees
   */
  private double[] latitudePoints;

  /**
   * An array of doubles containing the geographic longitude points of the craton boundary in
   * degrees
   */
  private double[] longitudePoints;

  /**
   * The Craton constructor. This constructor initializes the Craton to the provided values.
   *
   * @param name A String containing the craton name
   * @param latitudePoints A double array containing the geographic latitude points defining the
   *     boundary polygon of the craton
   * @param longitudePoints A double array containing the geographic longitude points defining the
   *     boundary polygon of the craton
   */
  public Craton(String name, double[] latitudePoints, double[] longitudePoints) {
    this.name = name;
    this.latitudePoints = latitudePoints;
    this.longitudePoints = longitudePoints;
  }

  /**
   * This method determines if an epicenter is inside this craton.
   *
   * @param latitude A double containing the Geographic epicenter latitude in degrees
   * @param longitude A double containing the Geographic epicenter longitude in degrees
   * @return True if the epicenter is inside this craton, false otherwise
   */
  public boolean isCraton(double latitude, double longitude) {
    int numCrossings = 0;

    // Stay away from the poles and the date line.
    double trialLatitude = latitude;
    if (Math.abs(Math.abs(trialLatitude) - 90d) < TauUtil.DTOL) {
      trialLatitude -= Math.copySign(TauUtil.DTOL, trialLatitude);
    }

    double trialLongitude = longitude;
    if (Math.abs(Math.abs(trialLongitude) - 180d) < TauUtil.DTOL) {
      trialLongitude -= Math.copySign(TauUtil.DTOL, trialLongitude);
    }

    // Count crossings of the polygon's sides.
    for (int j = 1; j < latitudePoints.length; j++) {
      double slope =
          (longitudePoints[j] - longitudePoints[j - 1])
              / (latitudePoints[j] - latitudePoints[j - 1]);
      double testLat =
          (trialLongitude - longitudePoints[j - 1] + slope * latitudePoints[j - 1]) / slope;

      if ((testLat >= trialLatitude)
          && (((testLat - latitudePoints[j]) * (testLat - latitudePoints[j - 1])) <= 0d)) {
        numCrossings++;
      }
    }

    // If the number of crossings is odd, the epicenter is inside.
    return (numCrossings % 2) > 0;
  }

  /**
   * This function writes the data for this craton.
   *
   * @return A String containing the craton information
   */
  public String printCraton() {
    String cratonString = "\n" + name;
    for (int j = 0; j < latitudePoints.length; j++) {
      cratonString += String.format("\t%8.4f %9.4f\n", latitudePoints[j], longitudePoints[j]);
    }

    return cratonString;
  }
}
