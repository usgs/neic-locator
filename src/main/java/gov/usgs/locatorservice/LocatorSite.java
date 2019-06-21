package gov.usgs.locatorservice;

import gov.usgs.processingformats.Site;

public class LocatorSite {

  public String Station;
  public String Network;
  public String Channel;
  public String Location;
  public Double Latitude;
  public Double Longitude;
  public Double Elevation;

  public Site toSite() {
    Site site = new Site();
    site.setStation(Station);
    site.setNetwork(Network);
    site.setChannel(Channel);
    site.setLocation(Location);
    site.setLatitude(Latitude);
    site.setLongitude(Longitude);
    site.setElevation(Elevation);
    return site;
  }

}