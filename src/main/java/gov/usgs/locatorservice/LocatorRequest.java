package gov.usgs.locatorservice;

import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.Pick;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class LocatorRequest {

  public String ID;
  public String Type;

  public String EarthModel;
  public Double SourceLatitude;
  public Double SourceLongitude;
  public Date SourceOriginTime;
  public Double SourceDepth;

  public Boolean IsLocationNew;
  public Boolean IsLocationHeld;
  public Boolean IsDepthHeld;
  public Boolean IsBayesianDepth;
  public Double BayesianDepth;
  public Double BayesianSpread;
  public Boolean UseSVD;

  public List<LocatorPick> InputData;

  public LocationRequest toLocationRequest() {
    LocationRequest request = new LocationRequest();
    request.setID(ID);
    request.setType(Type);
    request.setEarthModel(EarthModel);
    request.setSourceLatitude(SourceLatitude);
    request.setSourceLongitude(SourceLongitude);
    request.setSourceOriginTime(SourceOriginTime);
    request.setSourceDepth(SourceDepth);
    request.setIsLocationNew(IsLocationNew);
    request.setIsLocationHeld(IsLocationHeld);
    request.setIsDepthHeld(IsDepthHeld);
    request.setIsBayesianDepth(IsBayesianDepth);
    request.setBayesianDepth(BayesianDepth);
    request.setBayesianSpread(BayesianSpread);
    request.setUseSVD(UseSVD);

    List<Pick> picks = InputData.stream()
        .map(p -> p.toPick())
        .collect(Collectors.toList());
    request.setInputData(new ArrayList<Pick>(picks));

    return request;
  }

}
