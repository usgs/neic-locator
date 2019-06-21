package gov.usgs.locatorservice;

import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.LocationService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import java.net.URI;

@Controller("/locator")
public class LocatorController implements LocationService {
  @Get("/")
  @Produces(MediaType.TEXT_HTML)
  HttpResponse index() {
    return HttpResponse.redirect(URI.create("/locator/index.html"));
  }

  @Override
  @Post(uri="/locate", consumes=MediaType.APPLICATION_JSON)
  public LocationResult getLocation(@Body("request") LocationRequest request) throws LocationException {
    return new LocationResult();
  }
}
