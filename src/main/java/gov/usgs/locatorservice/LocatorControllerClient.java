package gov.usgs.locatorservice;

import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import javax.validation.constraints.NotBlank;

@Client("${locator.controller.path:/locator}")
public interface LocatorControllerClient {

  @Post("/locate")
  public LocationResult getLocation(@NotBlank LocationRequest request) throws LocationException;
}
