package gov.usgs.locatorservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
public class LocatorControllerTest {

  @Inject EmbeddedServer server;

  @Test
  public void exampleRequest() throws Exception {
    // read request from json file.
    LocatorRequest request = readRequestJson(Paths.get("examples/request.json"));

    LocatorController locator =
        server.getApplicationContext().createBean(LocatorController.class, server.getURL());
    LocatorResponse response = locator.getLocation(request);
    LocatorHypocenter hypocenter = response.Hypocenter;

    Assertions.assertEquals(1551739921747L, hypocenter.Time.getTime(), 1, "Time");
    Assertions.assertEquals(73.7174, hypocenter.Latitude, 1e-4, "Latitude");
    Assertions.assertEquals(-57.2109, hypocenter.Longitude, 1e-4, "Longitude");
    Assertions.assertEquals(12.50, hypocenter.Depth, 1e-2, "Depth");
  }

  public LocatorRequest readRequestJson(final Path path) throws Exception {
    byte[] requestBytes = Files.readAllBytes(path);
    ObjectMapper mapper = new ObjectMapper();
    try {
      LocatorRequest request = mapper.readValue(requestBytes, LocatorRequest.class);
      return request;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
