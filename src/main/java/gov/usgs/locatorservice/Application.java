package gov.usgs.locatorservice;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info =
        @Info(
            title = "USGS Location webservice",
            version = "0.4.0",
            description = "Webservice used by the USGS NEIC to request seismic event locations."))
public class Application {

  /**
   * Main program for the locator web service.
   *
   * @param args An array of Strings containing the command line arguments (not used)
   */
  public static void main(String[] args) {
    Micronaut.run(Application.class);
  }
}
