package gov.usgs.locator;

import java.util.Comparator;

/**
 * The PickComp class is the Pick comparator. The compare fundtion will sort picks into the "Hydra"
 * input order. That is, the picks will be sorted alphabetically by station ID and by arrival time
 * within each station.
 *
 * @author Ray Buland
 */
public class PickComp implements Comparator<Pick> {
  /**
   * Comparison function used to sort the Pick objects by distance.
   *
   * @param pick1 A Pick object containing the first pick to compare
   * @param pick2 A Pick object containing the second pick to compare
   * @return +1 if the first Pick object station ID / arrival time varible is greater than the
   *     second Pick object station ID / arrival time varible; -1 if the first Pick object station
   *     ID / arrival time varible is less than the second Pick object station ID / arrival time
   *     varible; and 0 if the first Pick object station ID / arrival time varible is equal to the
   *     second Pick object station ID / arrival time varible;
   */
  @Override
  public int compare(Pick pick1, Pick pick2) {
    // Sort into station and then arrival time order.
    int order =
        pick1
            .getStation()
            .getStationID()
            .getStationID()
            .compareTo(pick2.getStation().getStationID().getStationID());

    if (order != 0) {
      return order;
    } else {
      if (pick1.getArrivalTime() < pick2.getArrivalTime()) {
        return -1;
      } else if (pick1.getArrivalTime() > pick2.getArrivalTime()) {
        return +1;
      } else {
        return 0;
      }
    }
  }
}
