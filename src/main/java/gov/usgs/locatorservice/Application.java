package gov.usgs.locatorservice;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info =
        @Info(
            title = "locator service",
            version = "0.0",
            description = "locator service description"))
public class Application {

  public static void main(String[] args) {
    Micronaut.run(Application.class);
  }
}
