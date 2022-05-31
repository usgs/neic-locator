package gov.usgs.locatorservice;

import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import javax.validation.constraints.NotBlank;

/**
 * The LocatorControllerClient is a required Micronaut interface defining the desired endpoints and
 * functions for location webservices.
 *
 * @author John Patton
 */
@Client("${locator.controller.path:/locator}")
public interface LocatorControllerClient {

  /**
   * Interface for the locate endpoint.
   *
   * @param request a final LocationRequest containing the location request
   * @return A LocationResult with the response section containing the resulting location data
   * @throws gov.usgs.processingformats.LocationException Throws a LocationException upon certain
   *     severe errors.
   */
  @Post("/locate")
  public LocationResult getLocation(@NotBlank LocationRequest request) throws LocationException;
}
