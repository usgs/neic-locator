package gov.usgs.locaux;

import java.io.Serializable;
import java.util.List;

/**
 * Store slab depth points in one contiguous longitude segment at one latitude. Note that this class
 * is intended to be built and maintained by SlabRow.
 *
 * @author Ray Buland
 */
public class SlabSeg implements Serializable {
  private static final long serialVersionUID = 1L;
  int pointFound = -1; // Remember where in the segment the epicenter was
  double lonBase; // The longitude of the first point in the segment
  double[] lonRange; // The longitude range of the slab segment
  SlabDepth[] depths;

  /**
   * Get the longitude associated with the latest point found.
   *
   * @param slabInc Slab latitude-longitude grid spacing in degrees
   * @return Geographic longitude in degrees (0-360 degrees)
   */
  public double getLon(double slabInc) {
    if (pointFound >= 0) {
      return lonBase + pointFound * slabInc;
    } else {
      return Double.NaN;
    }
  }

  /**
   * Create the segment from a list of points.
   *
   * @param points List of slab depth points
   * @param slabInc Slab latitude-longitude grid spacing in degrees
   */
  public SlabSeg(List<SlabPoint> points, double slabInc) {
    lonBase = points.get(0).getLon();
    lonRange = new double[2];
    lonRange[0] = lonBase - slabInc / 2d;
    lonRange[1] = points.get(points.size() - 1).getLon() + slabInc / 2d;
    depths = new SlabDepth[points.size()];
    for (int j = 0; j < points.size(); j++) {
      depths[j] = points.get(j).getDep();
    }
  }

  /**
   * Test to see if the desired position is in this segment. Note that the latitude has already been
   * tested in the parent row.
   *
   * @param lon Geographic longitude in degrees (0-360 degrees)
   * @return True if the point is within in this segment
   */
  public boolean isFound(double lon) {
    if (lon >= lonRange[0] && lon <= lonRange[1]) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get the slab depth triplet at a point. Note that the longitude here is not the desired
   * longitude, but the longitude truncated to lie on the latitude-longitude grid. This solves an
   * ambiguity that arises when the desired point is on the grid.
   *
   * @param lon Geographic longitude in degrees (0-360 degrees)
   * @param slabInc Slab latitude-longitude grid spacing in degrees
   * @return Slab depth triplet
   */
  public SlabDepth getDepth(double lon, double slabInc) {
    if (lon >= lonRange[0] && lon <= lonRange[1]) {
      pointFound = (int) ((lon - lonBase) / slabInc);
      return depths[pointFound];
    }
    pointFound = -1;
    return null;
  }

  /**
   * Get the next slab depth triplet. Since the slab depths in a segment are accessed in pairs, this
   * guarantees that two adjacent points are found in SlabRow.
   *
   * @return Slab depth triplet
   */
  public SlabDepth getNextDepth() {
    if (pointFound >= 0) {
      if (++pointFound < depths.length) {
        return depths[pointFound];
      }
    }
    pointFound = -1;
    return null;
  }

  @Override
  public String toString() {
    return String.format(
        "%6.2f-%6.2f: %6.2f-%6.2f",
        lonRange[0], lonRange[1], depths[0].getEqDepth(), depths[depths.length - 1].getEqDepth());
  }
}
