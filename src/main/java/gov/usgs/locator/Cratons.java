package gov.usgs.locator;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The Cratons class keeps track of the geographic boundaries of continental 
 * cratons.  
 * Both travel times and magnitudes are sensitive to the age and 
 * stability of the crust.  For example, in cratons, Pg and Pb are 
 * distinct phases and mbLg is a preferred magnitude.  In active 
 * areas, Pb is an extension of Pg instead of a distinct phase 
 * and Ml is preferred.
 * 
 * @author Ray Buland
 *
 */
public class Cratons implements Serializable {
  /**
   * A long containing the serializable class version number, used during 
   * deserialization to verify compatibility.
   */
  private static final long serialVersionUID = 1L;
  
  /** 
   * An ArrayList of Craton objects.
   */
  private ArrayList<Craton> cratons;
  
  /**
   * The Cratons constructor. Allocates an empty array list of Craton objects.
   */
  public Cratons() {
    cratons = new ArrayList<Craton>();
  }
  
  /**
   * Function to add a new craton to the list.
   * 
   * @param name A String containing the craton name
   * @param latitudePoints A double array containing the geographic latitude 
   *                       points defining the boundary polygon of the craton 
   * @param longitudePoints A double array containing the geographic longitude 
   *                        points defining the boundary polygon of the craton
   */
  public void add(String name, double[] latitudePoints, 
      double[] longitudePoints) {
    cratons.add(new Craton(name, latitudePoints, longitudePoints));
  }
  
  /**
   * Function to determine if a given epicenter is inside a craton.
   * 
   * @param latitude A double containing the geographical epicenter latitude in 
   *                 degrees
   * @param longitude A double containing the geographical epicenter longitude 
   *                  in degrees
   * @return True if the epicenter is inside a craton in the list, false otherwise
   */
  public boolean isCraton(double latitude, double longitude) {
    for (int j = 0; j < cratons.size(); j++) {
      if (cratons.get(j).isCraton(latitude, longitude)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Function to print the data for all cratons to the screen.
   */
  public void printCratons() {
    for (int j = 0; j < cratons.size(); j++) {
      cratons.get(j).printCraton();
    }
  }
}
