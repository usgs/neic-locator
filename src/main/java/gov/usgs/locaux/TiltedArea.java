package gov.usgs.locaux;

import java.util.ArrayList;

/**
 * Regrid the tilted slab data in a form suitable for the Locator. This involves summarizing the
 * very complex and highly detailed tilted slab information into a slab depth triplet on a much
 * sparser grid.
 *
 * @author Ray Buland
 */
public class TiltedArea {
  /** A double containing the tilted slab latitude-longitude grid spacing in degrees */
  private double slabInc;

  /** A double containing half of the tilted slab latitude-longitude grid spacing in degrees */
  private double slabHalfInc;

  /** An array of doubles containing the latitude range of the tilted slab area in degrees */
  private double[] latRange;

  /** An array of doubles containing the longitude range of the tilted slab area in degrees */
  private double[] lonRange;

  /** A two dimensional array TiltedSample objects containing the grid for this tilted slab area */
  private TiltedSample[][] grid;

  /** An ArrayList of SlabPoint objects containing the sample pool for this tilted slab area */
  private ArrayList<SlabPoint> pool;

  /**
   * Function to retrieve the grid for this tilted slab area
   *
   * @return A two dimensional array TiltedSample objects containing the sorted grid data.
   */
  public TiltedSample[][] getGrid() {
    return grid;
  }

  /**
   * Constructor for TiltedArea. Sets up the tilted slab area.
   *
   * @param slabInc A double containing the tilted slab latitude-longitude grid spacing in degrees
   */
  public TiltedArea(double slabInc) {
    this.slabInc = slabInc;
    slabHalfInc = slabInc / 2d;
    latRange = new double[2];
    lonRange = new double[2];
    latRange[0] = 500d;
    latRange[1] = 0d;
    lonRange[0] = 500d;
    lonRange[1] = 0d;

    pool = new ArrayList<SlabPoint>();
  }

  /**
   * Function to add a point to the tilted slab pool. Note that all tilted slab samples go into the
   * pool for later sorting.
   *
   * @param point A SlabPoint object containing the tilted slab sample
   */
  public void add(SlabPoint point) {
    pool.add(point);

    latRange[0] = Math.min(latRange[0], point.getLat());
    latRange[1] = Math.max(latRange[1], point.getLat());
    lonRange[0] = Math.min(lonRange[0], point.getLon());
    lonRange[1] = Math.max(lonRange[1], point.getLon());
  }

  /**
   * Function to create a grid on half degree intervals that spans the range and domain of the
   * tilted slab samples in this area.
   */
  public void makeGrid() {
    latRange[0] = slabInc * (int) ((latRange[0] + slabHalfInc) / slabInc);
    latRange[1] = slabInc * (int) ((latRange[1] + slabHalfInc) / slabInc);
    lonRange[0] = slabInc * (int) ((lonRange[0] + slabHalfInc) / slabInc);
    lonRange[1] = slabInc * (int) ((lonRange[1] + slabHalfInc) / slabInc);

    grid =
        new TiltedSample[(int) ((latRange[1] - latRange[0]) / slabInc + 1)]
            [(int) ((lonRange[1] - lonRange[0]) / slabInc + 1)];

    // Create the grid.
    double lat = latRange[0];
    for (int i = 0; i < grid.length; i++) {
      double lon = lonRange[0];

      for (int j = 0; j < grid[i].length; j++) {
        grid[i][j] = new TiltedSample(lat, lon);
        lon += slabInc;
      }

      lat += slabInc;
    }

    fillGrid();
  }

  /**
   * Function to transform a tilted slabs area into a normal slab area.
   *
   * @return A SlabArea object contaiing the normal slab area
   */
  public SlabArea getSlabArea() {
    SlabArea area;
    SlabRow row;

    area = new SlabArea(slabInc);
    for (int i = 0; i < grid.length; i++) {
      row = new SlabRow();

      for (int j = 0; j < grid[i].length; j++) {
        row.add(new SlabPoint(grid[i][j]));
      }

      if (i == 0 || i == grid.length - 1) {
        row.printRaw();
      }

      row.squeeze(slabInc);
      area.add(row);
    }

    area.printArea(false);
    return area;
  }

  /**
   * Function to fill the grid by running through all the points in the pool and assigning each one
   * to a grid point.
   */
  private void fillGrid() {
    int i, j;

    for (SlabPoint point : pool) {
      i =
          Math.min(
              Math.max((int) ((point.getLat() - latRange[0] + slabHalfInc) / slabInc), 0),
              grid.length);

      j =
          Math.min(
              Math.max((int) ((point.getLon() - lonRange[0] + slabHalfInc) / slabInc), 0),
              grid[i].length);
      grid[i][j].add(point);
    }
  }

  /** Function to provide a toString suitable for traditional printing and logging. */
  @Override
  public String toString() {
    return String.format(
        "(%6.2f,%6.2f) - (%6.2f,%6.2f)", latRange[0], lonRange[0], latRange[1], lonRange[1]);
  }
}
