package gov.usgs.locaux;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Store slab depth points in one latitude row. One latitude row of one area may have multiple
 * segments because of the typical arc structure of slabs or because there are multiple slabs in the
 * area.
 *
 * @author Ray Buland
 */
public class SlabRow implements Serializable {
  /**
   * A long containing the serializable class version number, used during deserialization to verify
   * compatibility.
   */
  private static final long serialVersionUID = 1L;

  /** An integer containing the last segment where the epicenter was found */
  private int segFound = -1;

  /** A double containing the colatitude of this row, it is constant */
  private double lat;

  /** An array of doubles containing the Longitude range spanned by the segments */
  private double[] lonRange = null;

  /** An ArrayList of SlabPoint objects containing the slab points */
  private ArrayList<SlabPoint> slabPoints = null;

  /** An ArrayList of SlabSeg objects containing the slab segments */
  private ArrayList<SlabSeg> slabSegs = null;

  /**
   * Function to retrieve the Geographic colatitude of this row
   *
   * @return A double containing the Geographic colatitude in degrees (0-180 degrees)
   */
  public double getLat() {
    return lat;
  }

  /**
   * Function to retrieve the Geographic longitude range
   *
   * @return An array of doubles containing Geographic longitude range in degrees (0-360 degrees)
   */
  public double[] getLonRange() {
    return lonRange;
  }

  /**
   * Function to test if this row is a dummy (i.e., has no data).
   *
   * @return A boolean flag, True if it is a dummy (empty) row, false otherwise
   */
  public boolean isDummyRow() {
    if (slabSegs != null) {
      return false;
    } else {
      return true;
    }
  }

  /** SlabRow constructor, sets up storage for raw slab depth points. */
  public SlabRow() {
    slabPoints = new ArrayList<SlabPoint>();
  }

  /**
   * SlabRow alternate constructor is used to create place holder rows. The place holder rows are
   * needed to ensure completeness in longitude.
   *
   * @param lat A double containing the latitude
   */
  public SlabRow(double lat) {
    this.lat = lat;
  }

  /**
   * Function to add a new slab depth point for this latitude row.
   *
   * @param point A SlabPoint object containing the slab depth point
   */
  public void add(SlabPoint point) {
    slabPoints.add(point);
  }

  /**
   * Function to create segments that are contiguous in longitude
   *
   * <p>When the row is complete, create segments that are contiguous in longitude. The intermediate
   * list of points is then freed for garbage collection.
   *
   * @param slabInc A double containing the slab latitude-longitude grid spacing in degrees
   */
  public void squeeze(double slabInc) {
    int start = 0;

    // Squeeze out points where the earthquake depth is NaN.
    lat = slabPoints.get(0).getLat();

    for (int j = 0; j < slabPoints.size(); j++) {
      if (!Double.isNaN(slabPoints.get(j).getEqDepth())) {
        start = j++;

        for (; j < slabPoints.size(); j++) {
          if (Double.isNaN(slabPoints.get(j).getEqDepth())) {
            addSegment(start, j, slabInc);
            break;
          }
        }
      }
    }

    // Be sure we get segments that go to the end of the area.
    if (!Double.isNaN(slabPoints.get(slabPoints.size() - 1).getEqDepth())) {
      addSegment(start, slabPoints.size(), slabInc);
    }

    // We don't need the slabPoints any more.
    slabPoints = null;
  }

  /**
   * Function to test to see if the desired point falls in this row. If it does, remember which
   * segment it's in.
   *
   * @param lon A double containing the Geographic longitude in degrees (0-360 degrees)
   * @return A boolean flag, True if the desired point is within in this row, false otherwise
   */
  public boolean isFound(double lon) {
    if (slabSegs != null) {
      if (lon >= lonRange[0] && lon <= lonRange[1]) {
        for (int j = 0; j < slabSegs.size(); j++) {
          if (slabSegs.get(j).isFound(lon)) {
            segFound = j;
            return true;
          }
        }
      }
    }

    segFound = -1;
    return false;
  }

  /**
   * Function Get the slab depth triplet at a point.
   *
   * @param lon Double containing the Geographic longitude in degrees (0-360 degrees)
   * @param slabInc Double contaiing the slab latitude-longitude grid spacing in degrees
   * @return A SlabDepth object containing the slab depth triplet
   */
  public SlabDepth getDepth(double lon, double slabInc) {
    // If we haven't found the segment yet, get it.
    if (segFound < 0) {
      segFound = -1;

      // If this row was filler, it may have no segments.
      if (slabSegs != null) {
        for (int j = 0; j < slabSegs.size(); j++) {
          if (slabSegs.get(j).isFound(lon)) {
            segFound = j;
          }
        }
      }
    }

    // Find the point in this segment.
    if (segFound >= 0) {
      return slabSegs.get(segFound).getDepth(lon, slabInc);
    } else {
      return null;
    }
  }

  /**
   * Function to get 3-vectors for interpolation. Note that we can only get the two position
   * 3-vectors for the longitude values surrounding the desired point since the other two are in the
   * next row (hopefully).
   *
   * @param lon A double containing the Geographic longitude in degrees (0-360 degrees)
   * @param v A three dimiensional array containing the the position 3-vectors for the two bounding
   *     longitude
   * @param slabInc A double containing the slab latitude-longitude grid spacing in degrees values
   */
  public void getVectors(double lon, double[][][] v, double slabInc) {
    SlabDepth[] depths;

    // Get the first vector using the longitude.
    depths = new SlabDepth[2];
    depths[0] = getDepth(lon, slabInc);

    if (depths[0] != null) {
      v[0] = depths[0].getVectors(lat, slabSegs.get(segFound).getLon(slabInc));
    } else {
      v[0] = null;
    }

    // Get the second vector using the next depth for robustness.
    if (segFound >= 0) {
      depths[1] = slabSegs.get(segFound).getNextDepth();
      if (depths[1] != null) {
        v[1] = depths[1].getVectors(lat, slabSegs.get(segFound).getLon(slabInc));
      } else {
        v[1] = null;
      }
    } else {
      depths[1] = null;
    }

    segFound = -1;
  }

  /**
   * Function to add a new segment to this row
   *
   * <p>Copies a run of non-null points into a new segment.
   *
   * @param start An integer containing the start index of raw points
   * @param end An integer containing the end index of raw points
   * @param @param slabInc A double containing the slab latitude-longitude grid spacing in degrees
   *     values
   */
  private void addSegment(int start, int end, double slabInc) {
    if (slabSegs == null) {
      slabSegs = new ArrayList<SlabSeg>();
    }

    slabSegs.add(new SlabSeg(slabPoints.subList(start, end), slabInc));

    if (lonRange == null) {
      lonRange = new double[2];
      lonRange[0] = slabPoints.get(start).getLon() - slabInc / 2d;
    }

    lonRange[1] = slabPoints.get(end - 1).getLon() + slabInc / 2d;
  }

  /** Function to generate a string containing a summary of the points before squeezing. */
  public String printRaw() {
    String pointString = "";

    if (slabPoints != null) {
      int last = slabPoints.size() - 1;

      pointString +=
          String.format(
              "Raw: (%6.2f,%6.2f) - (%6.2f,%6.2f)\n",
              slabPoints.get(0).getLat(),
              slabPoints.get(0).getLon(),
              slabPoints.get(last).getLat(),
              slabPoints.get(last).getLon());
    }

    return pointString;
  }

  /** Function to generate a string containing a summary of the segments created by squeezing. */
  public String printRow() {
    String rowString = "";

    if (lonRange != null) {
      rowString += "Row: " + toString();

      if (slabSegs != null) {
        for (int j = 0; j < slabSegs.size(); j++) {
          rowString += "tSeg: " + slabSegs.get(j) + "\n";
        }
      }
    }

    return rowString + "\n";
  }

  /** Function to provide a toString suitable for traditional printing and logging. */
  @Override
  public String toString() {
    // If slabPoints is still around, show the raw summary.
    if (slabPoints != null) {
      if (slabPoints.size() > 0) {
        int last = slabPoints.size() - 1;

        return String.format(
            "Raw: (%6.2f,%6.2f) - (%6.2f,%6.2f)\n",
            slabPoints.get(0).getLat(),
            slabPoints.get(0).getLon(),
            slabPoints.get(last).getLat(),
            slabPoints.get(last).getLon());
      } else {
        return "No points have been added";
      }
      // Otherwise, show the final processed summary.
    } else {
      if (lonRange != null) {
        return String.format("(%6.2f,%6.2f-%6.2f)", lat, lonRange[0], lonRange[1]);
      } else {
        return "No slabs";
      }
    }
  }
}
