package gov.usgs.locatorservice;

import gov.usgs.locator.LocService;
import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Hidden;
import java.net.URI;

@Controller("/locationservices")
public class LocatorController {

  /**
   * A string containing the path to the locator models, automatically populated by Micronaut from
   * the locator.model.path environment varible, defaulting to ./build/models/ if the environment
   * varible is not present.
   */
  @Value("${locator.model.path:./build/models/}")
  protected String modelPath;

  /**
   * A string containing the path to the locator serialized files, automatically populated by
   * Micronaut from the locator.serialized.path environment varible, defaulting to ./build/models/
   * if the environment varible is not present.
   */
  @Value("${locator.serialized.path:./build/models/}")
  protected String serializedPath;

  /**
   * Function to setup the default root endpoint, pointing to index.html
   *
   * @return returns a HttpResponse containing index.html
   */
  @Get(uri = "/", produces = MediaType.TEXT_HTML)
  @Hidden
  public HttpResponse getIndex() {
    return HttpResponse.redirect(URI.create("/locationservices/index.html"));
  }

  /**
   * Function to setup the locate endpoint.
   *
   * @param request a final LocationRequest containing the location request
   * @return A LocationResult with the response section containing the resulting location data
   * @throws gov.usgs.processingformats.LocationException Throws a LocationException upon certain
   *     severe errors.
   */
  @Post(uri = "/locate", consumes = MediaType.APPLICATION_JSON)
  public LocationResult getLocation(@Body LocationRequest request) throws LocationException {
    LocService service = new LocService(modelPath, serializedPath);
    return service.getLocation(request);
  }
}
