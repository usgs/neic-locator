package gov.usgs.locatorservice;

import gov.usgs.processingformats.LocationResult;
import java.util.List;
import java.util.stream.Collectors;

public class LocatorResponse {

  public String ID;
  public LocatorHypocenter Hypocenter;
  public List<LocatorPick> SupportingData;
  public Integer NumberOfAssociatedStations;
  public Integer NumberOfAssociatedPhases;
  public Integer NumberOfUsedStations;
  public Integer NumberOfUsedPhases;
  public Double Gap;
  public Double SecondaryGap;
  public Double MinimumDistance;
  public Double RMS;
  public String Quality;
  public Double BayesianDepth;
  public Double BayesianRange;
  public Double DepthImportance;
  public String LocatorExitCode;
  public LocatorErrorEllipse ErrorEllipse;

  public static LocatorResponse fromLocationResult(final LocationResult that) {
    LocatorResponse response = new LocatorResponse();
    response.ID = that.getID();
    response.Hypocenter = LocatorHypocenter.fromHypocenter(that.getHypocenter());
    List<LocatorPick> supportingData =
        that.getSupportingData()
            .stream()
            .map(p -> LocatorPick.fromPick(p))
            .collect(Collectors.toList());
    response.SupportingData = supportingData;
    response.NumberOfAssociatedStations = that.getNumberOfAssociatedStations();
    response.NumberOfAssociatedPhases = that.getNumberOfAssociatedPhases();
    response.NumberOfUsedStations = that.getNumberOfUsedStations();
    response.NumberOfUsedPhases = that.getNumberOfUsedPhases();
    response.Gap = that.getGap();
    response.SecondaryGap = that.getSecondaryGap();
    response.MinimumDistance = that.getMinimumDistance();
    response.RMS = that.getRMS();
    response.Quality = that.getQuality();
    response.BayesianDepth = that.getBayesianDepth();
    response.BayesianRange = that.getBayesianRange();
    response.DepthImportance = that.getDepthImportance();
    response.LocatorExitCode = that.getLocatorExitCode();
    response.ErrorEllipse = LocatorErrorEllipse.fromErrorEllipse(that.getErrorEllipse());
    return response;
  }
}
