package gov.usgs.locator;

import java.util.ArrayList;

/**
 * The PickGroup class contains all the picks observed at one station for one 
 * event. This construct is useful because the station geometry is the same for 
 * all picks in the group, so travel times are only computed once for each 
 * group. Note that the Locator flow depends on the "Hydra" style input file. 
 * In particular, that picks for each station are together. In preserving the 
 * "Hydra" order, picks in each group are also in time order.  The Locator 
 * doesn't actually require this, but it is convenient for bulltin style output.
 * Because the Locator may get input for all sorts of different sources, the 
 * "Hydra" order is imposed on all event input.
 * 
 * @author Ray Buland
 *
 */
public class PickGroup {
  /**
   * A Station object containing the station information for this pick group.
   */
  private Station station;  

  /** 
   * A double containing the source-receiver distance in degrees.
   */
  private double distance;      

  /** 
   * A double containing the receiver azimuth from the source in degrees.
   */
  private double azimuth;  

  /** 
   * An ArrayList of Pick objects containing the picks observed in this group 
   * (station).
   */
  private ArrayList<Pick> picks;

  /**
   * A double containing the cumulative figure-of-merit for this pick group.
   */
  private double cumulativeFoM;

  /**
   * Function to return the pick group station.
   * 
   * @return A Station containing the pick group station
   */
  public Station getStation() {
    return station;
  }  

  /**
   * Function to get the source-receiver distance.
   * 
   * @return A double containing the source-receiver distance in degrees
   */
  public double getDistance() {
    return distance;
  }
  
  /**
   * Function to get the receiver azimuth from the source.
   * 
   * @return A double containing the receiver azimuth from the source in degrees
   */
  public double getAzimuth() {
    return azimuth;
  }

  /**
   * Function to get he picks observed in this group (station). 
   * 
   * @return An ArrayList of Pick objects containing the picks observed in this  
   *         group (station).
   */
  public ArrayList<Pick> getPicks() {
    return picks;
  }

  /**
   * Get the number of picks in the group.
   * 
   * @return An int containing the number of picks for this group
   */
  public int getNumPicks() {
    return picks.size();
  }

  /**
   * This function counts the number of picks in the group that are used in the 
   * location.
   * 
   * @return An int containing the number of used picks for this group
   */
  public int getNumPicksUsed() {
    int numUsed = 0;
    for (int j = 0; j < picks.size(); j++) {
      if (picks.get(j).getIsUsed()) {
        numUsed++;
      }
    }
    return numUsed;
  }
  
  /**
   * Get the pick at a given index in the group.
   * 
   * @param index An int containing the index of the pick to get
   * @return A Pick object containing the pick in the group at the given index
   */
  public Pick getPick(int index) {
    return picks.get(index);
  }  
  
  /**
   * Function to get the maximum figure-of-merit.
   * 
   * @return A double containing the maximum figure-of-merit
   */
  public double getCumulativeFoM() {
    return cumulativeFoM;
  }  

  /**
   * Function to set the maximum figure-of-merit.
   * 
   * @param cumulativeFoM A double containing the maximum figure-of-merit
   */
  public void setCumulativeFoM(double cumulativeFoM) {
    this.cumulativeFoM = cumulativeFoM;
  }  

  /**
   * The PickGroup constructor. This constructor initializes the pick group with 
   * the station and the first pick.
   * 
   * @param station A Station object containing the station information
   * @param pick A Pick object containing the first pick in the group
   */
  public PickGroup(Station station, Pick pick) {
    this.station = station;
    picks = new ArrayList<Pick>();
    addPick(pick);
    distance = Double.NaN;
    azimuth = Double.NaN;
  }
  
  /**
   * This function adds a pick to the group.
   * 
   * @param pick  Pick object containing the pick to add to the group
   */
  public void addPick(Pick pick) {
    picks.add(pick);
  }
  
  /**
   * This function updates the event parameters (Both the hypocenter and origin 
   * time have changed) for the pick group: distance, azimuth, and travel times.
   * 
   * @param hypo A Hypocenter object containing the location and origin time
   *             information.
   */
  public void updateHypoAndTime(Hypocenter hypo) {
    updateHypo(hypo);
    updateTime(hypo);
  }
  
  /**
   * This function updates the distance and azimuth parameters only (Just the 
   * hypocenter has changed) for the pick group.
   * 
   * @param hypo A Hypocenter object containing the location information.
   */
  public void updateHypo(Hypocenter hypo) {
    // Distance and azimuth are group level parameters.
    distance = LocUtil.computeDistAzm(hypo, station);
    
    // note computeDistAzm() stores computed azimuth result via LocUtil.azimuth
    azimuth = LocUtil.azimuth;
  }
  
  /**
   * This function updates the travel time for picks only (Just the 
   * origin time has changed) for the pick group.
   * 
   * @param hypo A Hypocenter object containing the origin time information.
   */
  public void updateTime(Hypocenter hypo) {
    for (int j = 0; j < picks.size(); j++) {
      picks.get(j).updateTravelTime(hypo);
    }
  }
  
  /**
   * This function updates the phase identifications for all picks in this group.
   * 
   * @param reweightResiduals A boolean indicating whether to recompute the 
   *                          residual weights
   * @param weightedResiduals ArrayList of Wresidual objects containing the 
   *                          weighted residuals
   * @return True if any used pick in the group has changed significantly
   */
  public boolean updatePhaseIdentifications(boolean reweightResiduals, 
      ArrayList<Wresidual> weightedResiduals) {
    boolean isChanged = false;
    
    if (picks.get(0).updatePhaseIdentification(true, reweightResiduals, azimuth, 
        weightedResiduals)) {
      isChanged = true;
    }

    for (int j = 1; j < picks.size(); j++) {
      if (picks.get(j).updatePhaseIdentification(false, reweightResiduals, 
          azimuth, weightedResiduals)) {
        isChanged = true;
      }
    }
    
    return isChanged;
  }
  
  /**
   * This function initializes the figure-of-merit variables for all picks in  
   * the group.
   * 
   * @param startPickIndex An int containin the index of the first pick in the 
   *                       group to be initialized
   * @param endPickIndex An int index of the last pick in the group to be 
   *                     initialized
   */
  public void initializeFoM(int startPickIndex, int endPickIndex) {
    cumulativeFoM = 0d;

    for (int j = startPickIndex; j < endPickIndex; j++) {
      picks.get(j).initializeFoM();
    }
  }
  
  /**
   * This function prints out the input pick information in a format similar to 
   * the "Hydra" event input file.
   */
  public void printInputPicks() {
    Pick pick;
    
    for (int j = 0; j < picks.size(); j++) {
      pick = picks.get(j);
      System.out.format("%10s %-5s %3s %2s %2s %8.4f %9.4f %5.2f "
          + "%3.1f %-8s %12s %5b %-13s %-8s %3.1f\n", pick.getPickID(), 
          station.getStationID().staCode, pick.getChannelCode(), station.getStationID().netCode, 
          station.getStationID().locCode, station.getLatitude(), station.getLongitude(), 
          station.getElevation(), pick.getQuality(), pick.getCurrentPhaseCode(), 
          LocUtil.getTimeString(pick.getArrivalTime()), pick.getExternalUse(), 
          pick.getOriginalAuthorType(), pick.getOriginalPhaseCode(), 
          pick.getOriginalPhaseAffinity());
    }
  }
  
  /**
   * This function print the picks in a group in a user friendly manner.
   * 
   * @param first A boolean flag indicating that the function should only print 
   *              the first pick in the group
   */
  public void printPicks(boolean first) {
    // print the first pick
    Pick pick = picks.get(0);
    System.out.format("%-5s %-8s %-8s %7.2f %6.2f %3.0f\n", 
        station.getStationID().staCode, pick.getCurrentPhaseCode(), 
        pick.getOriginalPhaseCode(), pick.getTravelTime(), distance, azimuth);

    // print the rest
    if (!first) {
      for (int j = 1; j < picks.size(); j++) {
        System.out.format("      %-8s %-8s %7.2f\n", pick.getCurrentPhaseCode(), 
            pick.getOriginalPhaseCode(), pick.getTravelTime());
      }
    }
  }
  
  /**
   * This function prints the pick part of a Bulletin "Hydra" style output file.
   */
  public void printOutputPicks() {
    for (int j = 0; j < picks.size(); j++) {
      Pick pick = picks.get(j);

      System.out.format("%10s %-5s %-3s %-2s %-2s %-8s%6.1f %5.1f "
          + "%3.0f %1s %4.2f %6.4f\n", pick.getPickID(), station.getStationID().staCode, 
          pick.getChannelCode(), station.getStationID().netCode, station.getStationID().locCode, 
          pick.getCurrentPhaseCode(), pick.getResidual(), distance, azimuth, 
          LocUtil.getBoolChar(pick.getIsUsed()), pick.getWeight(), 
          pick.getImportance());
    }
  }
  
  /**
   * This function print out picks in the group in a way similar to the NEIC web 
   * format.
   */
  public void printNEIC() {
    String locCode = station.getStationID().locCode;
    if (locCode == null) {
      locCode = "";
    }
    
    for (int j = 0; j < picks.size(); j++) {
      Pick pick = picks.get(j);

      switch (pick.getOriginalAuthorType()) {
        case CONTRIB_HUMAN: 
        case LOCAL_HUMAN:
          System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s "
              + " manual    %6.1f    %4.2f\n", station.getStationID().netCode, 
              station.getStationID().staCode, pick.getChannelCode(), locCode, 
              distance, azimuth, pick.getCurrentPhaseCode(), 
              LocUtil.getNEICTimeString(pick.getArrivalTime()), 
              pick.getResidual(), pick.getWeight());
          break;

        default:
          System.out.format("%-2s %-5s %-3s %-2s  %5.1f     %3.0f   %-8s %12s  "
              + "automatic %6.1f    %4.2f\n", station.getStationID().netCode, 
              station.getStationID().staCode, pick.getChannelCode(), locCode, 
              distance, azimuth, pick.getCurrentPhaseCode(), 
              LocUtil.getNEICTimeString(pick.getArrivalTime()), 
              pick.getResidual(), pick.getWeight());
          break;
      }
    }
  }
}
