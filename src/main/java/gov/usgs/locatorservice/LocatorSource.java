package gov.usgs.locatorservice;

import gov.usgs.processingformats.Source;

public class LocatorSource {

  public String AgencyID;
  public String Author;
  public String Type;

  public Source toSource() {
    Source source = new Source();
    source.setAgencyID(AgencyID);
    source.setAuthor(Author);
    source.setType(Type);
    return source;
  }

  public static LocatorSource fromSource(final Source that) {
    LocatorSource source = new LocatorSource();
    source.AgencyID = that.getAgencyID();
    source.Author = that.getAuthor();
    source.Type = that.getType();
    return source;
  }
}
