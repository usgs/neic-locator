package gov.usgs.locatorservice;

import gov.usgs.processingformats.Hypocenter;
import java.util.Date;

public class LocatorHypocenter {

  public Double Latitude;
  public Double LatitudeError;
  public Double Longitude;
  public Double LongitudeError;
  public Double Depth;
  public Double DepthError;
  public Date Time;
  public Double TimeError;

  public static LocatorHypocenter fromHypocenter(final Hypocenter that) {
    LocatorHypocenter hypo = new LocatorHypocenter();
    hypo.Latitude = that.getLatitude();
    hypo.LatitudeError = that.getLatitudeError();
    hypo.Longitude = that.getLongitude();
    hypo.LongitudeError = that.getLongitudeError();
    hypo.Depth = that.getDepth();
    hypo.DepthError = that.getDepthError();
    hypo.Time = that.getTime();
    hypo.TimeError = that.getTimeError();
    return hypo;
  }
}
