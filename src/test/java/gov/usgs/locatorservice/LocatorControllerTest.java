package gov.usgs.locatorservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.processingformats.Hypocenter;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import io.micronaut.runtime.server.EmbeddedServer;
// import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
public class LocatorControllerTest {

  @Inject private EmbeddedServer server;

  @Test
  public void exampleRequest() throws Exception {
    // read request from json file.
    LocationRequest request = readRequestJson(Paths.get("examples/request.json"));

    LocatorController locator =
        server.getApplicationContext().createBean(LocatorController.class, server.getURL());
    LocationResult response = locator.getLocation(request);
    Hypocenter hypocenter = response.Hypocenter;

    Assertions.assertEquals(1551739921747L, hypocenter.Time.getTime(), 1, "Time");
    Assertions.assertEquals(73.66259656098615, hypocenter.Latitude, 1e-4, "Latitude");
    Assertions.assertEquals(-57.15027045506189, hypocenter.Longitude, 1e-4, "Longitude");
    Assertions.assertEquals(8.271562388098651, hypocenter.Depth, 1e-2, "Depth");
  }

  public LocationRequest readRequestJson(final Path path) throws Exception {
    byte[] requestBytes = Files.readAllBytes(path);
    ObjectMapper mapper = new ObjectMapper();
    try {
      LocationRequest request = mapper.readValue(requestBytes, LocationRequest.class);
      return request;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
