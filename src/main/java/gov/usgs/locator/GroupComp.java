package gov.usgs.locator;

import java.util.Comparator;

/**
 * The GroupComp class is the PickGroup comparator.  The compare function will 
 * sort pick groups by distance.
 * 
 * @author Ray Buland
 *
 */
public class GroupComp implements Comparator<PickGroup> {
  /**
   * Comparison function used to sort the PickGroup objects by distance.
   * @param group1 A PickGroup object containing the first pick group to compare
   * @param group2 A PickGroup object containing the second pick group to compare
   * @return +1 if the first PickGroup object delta varible is greater than the 
   *         second PickGroup object delta varible; -1 if the first PickGroup  
   *         object delta varible is less than the second PickGroup object 
   *         delta varible; and 0 if the first PickGroup object delta varible
   *         is equal to the second PickGroup object delta varible;
   */
  @Override
  public int compare(PickGroup group1, PickGroup group2) {
    if (group1.delta < group2.delta) { 
      return -1;
    } else if (group1.delta > group2.delta) {
      return +1;
    } else {
      return 0;
    }
  }
}