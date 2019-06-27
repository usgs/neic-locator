package gov.usgs.locatorservice;

import gov.usgs.processingformats.Pick;
import java.util.Date;

public class LocatorPick {

  public String ID;
  public String Type;

  public LocatorSite Site;
  public LocatorSource Source;
  public Date Time;
  public Double Affinity;
  public Double Quality;
  public Boolean Use;
  public String PickedPhase;
  public String LocatedPhase;
  public String AssociatedPhase;
  public Double Residual;
  public Double Distance;
  public Double Azimuth;
  public Double Weight;
  public Double Importance;

  public Pick toPick() {
    Pick pick = new Pick();

    pick.setId(ID);
    pick.setSite(Site.toSite());
    pick.setSource(Source.toSource());
    pick.setTime(Time);
    pick.setAffinity(Affinity);
    pick.setQuality(Quality);
    pick.setUse(Use);
    pick.setPickedPhase(PickedPhase);
    pick.setLocatedPhase(LocatedPhase);
    pick.setAssociatedPhase(AssociatedPhase);
    pick.setResidual(Residual);
    pick.setDistance(Distance);
    pick.setAzimuth(Azimuth);
    pick.setWeight(Weight);
    pick.setImportance(Importance);

    return pick;
  }

  public static LocatorPick fromPick(final Pick that) {
    LocatorPick pick = new LocatorPick();
    pick.ID = that.getID();
    pick.Site = LocatorSite.fromSite(that.getSite());
    pick.Source = LocatorSource.fromSource(that.getSource());
    pick.Time = that.getTime();
    pick.Affinity = that.getAffinity();
    pick.Quality = that.getQuality();
    pick.Use = that.getUse();
    pick.PickedPhase = that.getPickedPhase();
    pick.LocatedPhase = that.getLocatedPhase();
    pick.AssociatedPhase = that.getAssociatedPhase();
    pick.Residual = that.getResidual();
    pick.Distance = that.getDistance();
    pick.Azimuth = that.getAzimuth();
    pick.Weight = that.getWeight();
    pick.Importance = that.getImportance();
    return pick;
  }
}
