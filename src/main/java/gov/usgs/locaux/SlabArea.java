package gov.usgs.locaux;

import gov.usgs.traveltime.TauUtil;
import java.io.Serializable;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Store slab depth information for one latitude-longitude area.
 *
 * @author Ray Buland
 */
public class SlabArea implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** An integer containing which row the epicenter was in */
  private int rowFound = -1;

  /** A double containing the latitude-longitude grid spacing in degrees */
  private double slabInc;

  /** A double containing the latitude-longitude grid spacing in degrees */
  private double slabHalfInc;

  /** A double containing the minimum latitude in degrees for all rows */
  private double latBase;

  /** An array of doubles containing the slab area latitude search range in degrees */
  private double[] latRange;

  /** An array of doubles containing the slab area longitude search range in degrees */
  private double[] lonRange;

  /** An ArrayList of SlabRow objects containing the slab rows */
  private ArrayList<SlabRow> slabRows;

  /** Private logging object. */
  private static final Logger LOGGER = LogManager.getLogger(SlabArea.class.getName());

  /**
   * SlabArea constructor, sets up storage for the rows, segments, and depths.
   *
   * @param slabInc A double containing the slab latitude-longitude grid spacing in degrees
   */
  public SlabArea(double slabInc) {
    this.slabInc = slabInc;
    slabHalfInc = slabInc / 2d;
    latBase = 180d;
    latRange = new double[2];
    lonRange = new double[2];
    latRange[0] = 180d;
    latRange[1] = 0d;
    lonRange[0] = 360d;
    lonRange[1] = 0d;
    slabRows = new ArrayList<SlabRow>();
  }

  /**
   * Function to add a new slab latitude row.
   *
   * <p>After squeezing the row will be comprised of one or more segments.
   *
   * @param slabRow A SlabRow object containing a row of slab depths
   */
  public void add(SlabRow slabRow) {
    if (slabRow.getLonRange() != null) {
      latBase = Math.min(latBase, slabRow.getLat());

      latRange[0] = Math.min(latRange[0], slabRow.getLat() - slabHalfInc);
      latRange[1] = Math.max(latRange[1], slabRow.getLat() + slabHalfInc);
      lonRange[0] = Math.min(lonRange[0], slabRow.getLonRange()[0]);
      lonRange[1] = Math.max(lonRange[1], slabRow.getLonRange()[1]);

      slabRows.add(slabRow);
    }
  }

  /**
   * Function to test if the desired geographic position is within this area.
   *
   * @param lat A double containing the geographic colatitude in degrees (0-180 degrees)
   * @param lon A double containing the geographic longitude in degrees (0-360 degrees)
   * @return A boolean flag, true if the point falls in this area, false otherwise
   */
  public boolean isFound(double lat, double lon) {
    if (lat >= latRange[0] && lat <= latRange[1] && lon >= lonRange[0] && lon <= lonRange[1]) {
      int j = (int) ((lat - latBase) / slabInc);

      if (slabRows.get(j).isFound(lon)) {
        rowFound = j;
        return true;
      } else if (++j < slabRows.size()) {
        if (slabRows.get(j).isFound(lon)) {
          rowFound = --j;
          return true;
        }
      }
    }

    rowFound = -1;
    return false;
  }

  /**
   * Function to retrieve the slab depth triplet at the desired point. Note that the slab areas have
   * been split up so that no more than one depth value should be possible from each area.
   *
   * @param lat A double containing the Geographic colatitude in degrees (0-180 degrees)
   * @param lon A double containing the Geographic longitude in degrees (0-360 degrees)
   * @return A SlabDepth object containing the Slab depth triplet
   */
  public SlabDepth getDepth(double lat, double lon) {
    double[] v;
    double[][][] v0, v1;

    if (rowFound >= 0 && rowFound < slabRows.size()) {
      // Get the 3-vectors for interpolation.
      v = Linear.vector(lon, lat, Double.NaN);
      v0 = new double[2][][];

      slabRows.get(rowFound++).getVectors(lon, v0, slabInc);

      if (rowFound < slabRows.size()) {
        v1 = new double[2][][];
        slabRows.get(rowFound).getVectors(lon, v1, slabInc);
      } else {
        v1 = null;
      }

      // Make sure v0 and v1 are looking at the same place.
      align(v0, v1);
      return interp(v0, v1, v);
    }

    rowFound = -1;
    return null;
  }

  /**
   * A function to align the vectors surrounding the desired geographic point
   *
   * <p>We've found the vectors surrounding the desired geographic point, but because we've loosened
   * the criteria for narrow slabs, the longitudes may not line up in the two latitude rows.
   *
   * @param v0 A three dimensional double array containing the top latitude row vectors
   * @param v1 A three dimensional double array containing the bottom latitude row vectors
   */
  private void align(double[][][] v0, double[][][] v1) {
    if (v0 == null || v1 == null) {
      return;
    }
    if (v0[0] == null || v1[0] == null) {
      return;
    }
    if (v0[0][0][0] == v1[0][0][0]) {
      return;
    }

    // Apparently, not aligned.
    if (v0[0][0][0] < v1[0][0][0]) {
      // V1 is offset.  Fix it.
      if (v1[1] == null) {
        v1[1] = new double[3][3];
      }

      for (int j = 0; j < v1[0].length; j++) {
        v1[1][j] = v1[0][j];
      }

      v1[0] = null;
    } else {
      // V0 is offset.  Fix it.
      if (v0[1] == null) {
        v0[1] = new double[3][3];
      }

      for (int j = 0; j < v0[0].length; j++) {
        v0[1][j] = v0[0][j];
      }

      v0[0] = null;
    }
  }

  /**
   * Function to interpolate the slab depth triplet.
   *
   * <p>The four input vectors should be the depth points surrounding the desired point. For each
   * point, there is a three vector for the spatial position (i.e., longitude, latitude, and depth).
   * For each depth, there is a 3-vector for the depth triplet (i.e., minimum depth, earthquake
   * depth, and maximum depth).
   *
   * @param v0 A three dimensional double array containing the vector describing the triplet for the
   *     longitude points surrounding the desired point in the first latitude row
   * @param v1 A three dimensional double array containing the vector describing the triplet for the
   *     longitude points surrounding the desired point in the second latitude row
   * @param v A double array containing the position vector for the desired point
   * @return A SlabDepth object containing the interpolated slab depth triplets
   */
  private SlabDepth interp(double[][][] v0, double[][][] v1, double[] v) {
    int nulls = 0, k;
    double[] depths;
    double[][][] vTemp;

    // Survey the points to see how to do the interpolation.
    for (int j = 0; j < 2; j++) {
      if (v0[j] == null) {
        nulls++;
        LOGGER.debug("v0[" + j + "]: null");
      } else {
        LOGGER.debug(
            String.format(
                "v0[%d]: (%7.3f, %7.3f, %7.4f", j, v0[j][1][0], v0[j][1][1], v0[j][1][2]));
      }
    }

    if (v1 != null) {
      for (int j = 0; j < 2; j++) {
        if (v1[j] == null) {
          nulls++;
          LOGGER.debug("v1[" + j + "]: null");
        } else {
          LOGGER.debug(
              String.format(
                  "v1[%d]: (%7.3f, %7.3f, %7.4f", j, v1[j][1][0], v1[j][1][1], v1[j][1][2]));
        }
      }
    } else {
      nulls += 2;
    }
    LOGGER.debug(String.format("    v: (%7.3f, %7.3f, %7.4f", v[0], v[1], v[2]));

    switch (nulls) {
      case 0:
        // No nulls.  We can do bilinear interpolation.
        depths = new double[3];

        for (int j = 0; j < 3; j++) {
          depths[j] = Linear.threeD(v0[0][j], v0[1][j], v1[0][j], v1[1][j], v);
        }

        return new SlabDepth(depths);
      case 1:
        // We can handle an edge by doing linear interpolation on the three
        // points we have.
        vTemp = new double[3][][];
        k = 0;

        for (int i = 0; i < 2; i++) {
          if (v0[i] != null) {
            vTemp[k++] = v0[i];
          }
          if (v1 != null) {
            if (v1[i] != null) {
              vTemp[k++] = v1[i];
            }
          }
        }

        depths = new double[3];

        for (int j = 0; j < 3; j++) {
          depths[j] = Linear.twoD(vTemp[0][j], vTemp[1][j], vTemp[2][j], v);
        }

        return new SlabDepth(depths);
      case 2:
        // We still have two points, find them.
        vTemp = new double[2][][];
        k = 0;

        for (int i = 0; i < 2; i++) {
          if (v0[i] != null) {
            vTemp[k++] = v0[i];
          }
          if (v1 != null) {
            if (v1[i] != null) {
              vTemp[k++] = v1[i];
            }
          }
        }

        depths = new double[3];

        for (int j = 0; j < 3; j++) {
          depths[j] = Linear.oneD(vTemp[0][j], vTemp[1][j], v);
        }

        // Now inflate the errors by 50%.
        depths[0] = Math.max(depths[0] - 0.5d * (depths[1] - depths[0]), 0d);
        depths[2] += 0.5d * (depths[2] - depths[1]);

        return new SlabDepth(depths);
      case 3:
        // We only have one point, so use it and inflate the errors even more.
        depths = new double[3];

        for (int i = 0; i < 2; i++) {
          if (v0[i] != null) {
            for (int j = 0; j < 3; j++) {
              depths[j] = v0[i][j][2];
            }
            break;
          }
          if (v1 != null) {
            if (v1[i] != null) {
              for (int j = 0; j < 3; j++) {
                depths[j] = v1[i][j][2];
              }
              break;
            }
          }
        }

        // Now inflate the errors by 100%.
        depths[0] = Math.max(depths[0] - (depths[1] - depths[0]), 0d);
        depths[2] += (depths[2] - depths[1]);

        return new SlabDepth(depths);
      default:
        // There is no slab.
        break;
    }

    return null;
  }

  /**
   * Function to fix gaps in latitude.
   *
   * <p>In order to jump to the right latitude row, the latitudes need to be complete. If there are
   * latitude rows missing, add dummies to ensure completeness.
   */
  public void fixGaps() {
    double lastLat = latBase - slabInc;

    for (int j = 0; j < slabRows.size(); j++) {
      if (slabRows.get(j).getLat() - lastLat > slabInc + TauUtil.DTOL) {
        lastLat += slabInc;
        slabRows.add(j, new SlabRow(lastLat));

        LOGGER.debug(String.format("Dummy row added: lat = %6.2f", lastLat));
      }

      lastLat = slabRows.get(j).getLat();
    }
  }

  /** Function to check the status of all rows in this area for debugging purposes. */
  public void rowCensus() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("\t" + this);
      double lat = slabRows.get(0).getLat() - slabInc;

      for (int j = 0; j < slabRows.size(); j++) {
        lat += slabInc;

        while (Math.abs(slabRows.get(j).getLat() - lat) > TauUtil.DTOL) {
          LOGGER.debug(String.format("Missing row (lat = %6.2f)", lat));
          lat += slabInc;
        }

        if (slabRows.get(j).isDummyRow()) {
          LOGGER.debug(String.format("Dummy row (lat = %6.2f)", lat));
        }
      }
    }
  }

  /**
   * Function to print a summary of the slab area.
   *
   * @param full If true, print row and segment summaries as well
   * @return A String containing the area
   */
  public String printArea(boolean full) {
    String areaString = "Area: " + toString();

    if (full) {
      for (SlabRow row : slabRows) {
        areaString += row.printRow();
      }
    }

    return areaString;
  }

  /** Function to provide a toString suitable for traditional printing and logging. */
  @Override
  public String toString() {
    return String.format(
        "(%6.2f,%6.2f) - (%6.2f,%6.2f)", latRange[0], lonRange[0], latRange[1], lonRange[1]);
  }
}
