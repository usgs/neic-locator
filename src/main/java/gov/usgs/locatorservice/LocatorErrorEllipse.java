package gov.usgs.locatorservice;

import gov.usgs.processingformats.ErrorEllipse;

public class LocatorErrorEllipse {

  public LocatorErrorEllipseAxis E0;
  public LocatorErrorEllipseAxis E1;
  public LocatorErrorEllipseAxis E2;
  public Double MaximumHorizontalProjection;
  public Double MaximumVerticalProjection;
  public Double EquivalentHorizontalRadius;

  public static LocatorErrorEllipse fromErrorEllipse(final ErrorEllipse that) {
    LocatorErrorEllipse ellipse = new LocatorErrorEllipse();

    LocatorErrorEllipseAxis axis;

    axis = new LocatorErrorEllipseAxis();
    axis.Azimuth = that.getE0Azimuth();
    axis.Dip = that.getE0Dip();
    axis.Error = that.getE0Error();
    ellipse.E0 = axis;

    axis = new LocatorErrorEllipseAxis();
    axis.Azimuth = that.getE1Azimuth();
    axis.Dip = that.getE1Dip();
    axis.Error = that.getE1Error();
    ellipse.E1 = axis;

    axis = new LocatorErrorEllipseAxis();
    axis.Azimuth = that.getE2Azimuth();
    axis.Dip = that.getE2Dip();
    axis.Error = that.getE2Error();
    ellipse.E2 = axis;

    ellipse.MaximumHorizontalProjection = that.getMaximumHorizontalProjection();
    ellipse.MaximumVerticalProjection = that.getMaximumVerticalProjection();
    ellipse.EquivalentHorizontalRadius = that.getEquivalentHorizontalRadius();
    return ellipse;
  }
}
