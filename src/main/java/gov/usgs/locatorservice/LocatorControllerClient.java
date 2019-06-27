package gov.usgs.locatorservice;

import gov.usgs.processingformats.LocationException;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import javax.validation.constraints.NotBlank;

@Client("${locator.controller.path:/locator}")
public interface LocatorControllerClient {

  @Post("/locate")
  public LocatorResponse getLocation(@NotBlank LocatorRequest request) throws LocationException;
}
