package gov.usgs.locaux;

import gov.usgs.locator.BayesianDepth;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Provides the primary interface to all the slab data.
 *
 * @author Ray Buland
 */
public class Slabs implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** A double containing the The latitude-longitude grid spacing for the slab model. */
  private double slabInc = Double.NaN;

  /** An ArrayList of SlabArea object containing the areas for this slab */
  ArrayList<SlabArea> slabAreas;

  /**
   * A double containing the bayesian spread. Used for analyst input, the spread is interpreted as a
   * 99th percentile. Here the raw numbers are one sigma (68%). To make the slab statistics
   * compatible, they must be multiplied by 3.
   */
  double bayesSpread;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(Slabs.class.getName());

  /**
   * Function to retrieve the slab latitude-longitude grid spacing
   *
   * @return A double containing the slab latitude-longitude grid spacing.
   */
  public double getSlabInc() {
    return slabInc;
  }

  /**
   * Function to retrieve the bayesian spread
   *
   * @return A double containing the 99th percentile depth error in kilometers.
   */
  public double getBayesSpread() {
    return bayesSpread;
  }

  /** The Slabs Constructor. Sets up storage for the slab areas. */
  public Slabs() {
    slabAreas = new ArrayList<SlabArea>();
  }

  /**
   * Function to set the slab latitude-longitude grid spacing
   *
   * @param slabInc A double containing the slab latitude-longitude grid spacing.
   */
  public void setSlabInc(double slabInc) {
    this.slabInc = slabInc;
  }

  /**
   * Function to add a slab area.
   *
   * @param slabArea A SlabArea object containing the slab area to add
   */
  public void add(SlabArea slabArea) {
    slabArea.fixGaps();
    slabAreas.add(slabArea);
  }

  /**
   * Function to add a tilted slab area.
   *
   * @param tiltedArea A TiltedArea object containing the tilted slab area to add
   */
  public void add(TiltedArea tiltedArea) {
    slabAreas.add(tiltedArea.getSlabArea());
  }

  /**
   * Function to retrieve the bayesian depth list for a geographic point
   *
   * <p>Gets a list of slab depths for a point. Note that with overturned slabs, etc. it is possible
   * to sample at least two slabs for one geographic point.
   *
   * @param lat A double containing the geographic latitude in degrees
   * @param lon A double containing the geographic longitude in degrees
   * @return An ArrayList of BayesianDepth objects containing the slab depths
   */
  public ArrayList<BayesianDepth> getDepth(double lat, double lon) {
    double lat0, lon0;
    SlabDepth depth;
    ArrayList<BayesianDepth> depths = null;

    // The slab lookup works in colatitude and longitude from 0 to 360
    // degrees
    lat0 = 90d - lat;
    if (lon < 0d) {
      lon0 = 360d + lon;
    } else {
      lon0 = lon;
    }

    for (SlabArea area : slabAreas) {
      if (area.isFound(lat0, lon0)) {
        depth = area.getDepth(lat0, lon0);

        if (depth != null) {
          if (depths == null) {
            depths = new ArrayList<BayesianDepth>();
          }

          depths.add(new BayesianDepth(depth));
        }
      }
    }

    // Sort the deep earthquake zones into the order of increasing
    // earthquake depth.
    if (depths != null) {
      depths.sort(null);
    }

    return depths;
  }

  /** Function to perform a debug check on the status of all rows in all areas. */
  public void doRowCensus() {
    LOGGER.fine("Row Census by Area");

    for (SlabArea area : slabAreas) {
      area.rowCensus();
    }
  }

  /**
   * Function Print a summary of all slab areas.
   *
   * @param full If true, print summaries of rows and segments as well
   * @return A String containing the area summary
   */
  public String printAllAreas(boolean full) {
    String areaString = "";

    for (SlabArea area : slabAreas) {
      areaString += area.printArea(full) + "\n";
    }

    return areaString;
  }
}
