package gov.usgs.locator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.Utility;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.json.simple.parser.ParseException;
import org.junit.Test;

/**
 * Regression Test driver for the locator.
 *
 * @author John Patton
 */
public class LocatorRegressionTest {
  public static String EMPTYSTRING = "";

  /**
   * This test is designed as a overall regression test for the locator. If the behavior of the
   * locator is changed in such a that the location result is expected to change, this test
   * (specifically verification.json) will need to be updated
   */
  @Test
  public void testLocator() {
    // parse input string into request
    String inputString = loadFromFile("src/test/resources/input.json");
    assertNotEquals("Loaded Input", inputString, EMPTYSTRING);
    LocationRequest request = null;
    try {
      request = new LocationRequest(Utility.fromJSONString(inputString));
    } catch (ParseException e) {
      // parse failure
      fail(e.toString());
    }
    assertNotEquals("Location Request", request, null);

    // parse verification string into result
    String verificationString = loadFromFile("src/test/resources/verification.json");
    assertNotEquals("Loaded Verification", verificationString, EMPTYSTRING);
    LocationResult verificationResult = null;
    try {
      verificationResult = new LocationResult(Utility.fromJSONString(verificationString));
    } catch (ParseException e) {
      // parse failure
      fail(e.toString());
    }
    assertNotEquals("Verification Result", verificationResult, null);

    // do location
    LocationResult result = null;
    LocService service = new LocService("build/models/");
    try {
      result = service.getLocation(request);
    } catch (LocationException e) {
      fail(e.toString());
    }
    assertNotEquals("Location Result", result, null);

    // check location result parameters against verification result
    assertEquals(
        "Latitude:",
        verificationResult.getHypocenter().getLatitude(),
        result.getHypocenter().getLatitude());

    assertEquals(
        "Latitude Error:",
        verificationResult.getHypocenter().getLatitudeError(),
        result.getHypocenter().getLatitudeError());

    assertEquals(
        "Longitude: ",
        verificationResult.getHypocenter().getLongitude(),
        result.getHypocenter().getLongitude());

    assertEquals(
        "Longitude Error:",
        verificationResult.getHypocenter().getLongitudeError(),
        result.getHypocenter().getLongitudeError());

    assertEquals(
        "Depth: ",
        verificationResult.getHypocenter().getDepth(),
        result.getHypocenter().getDepth());

    assertEquals(
        "Depth Error: ",
        verificationResult.getHypocenter().getDepthError(),
        result.getHypocenter().getDepthError());

    assertEquals(
        "Time: ", verificationResult.getHypocenter().getTime(), result.getHypocenter().getTime());

    assertEquals(
        "Time Error: ",
        verificationResult.getHypocenter().getTimeError(),
        result.getHypocenter().getTimeError());

    assertEquals(
        "Minimum Distance: ", verificationResult.getMinimumDistance(), result.getMinimumDistance());

    assertEquals("RMS: ", verificationResult.getRMS(), result.getRMS());

    assertEquals("Gap: ", verificationResult.getGap(), result.getGap());

    assertEquals(
        "Used Stations: ",
        verificationResult.getNumberOfUsedStations(),
        result.getNumberOfUsedStations());

    assertEquals(
        "Used Phases: ",
        verificationResult.getNumberOfUsedPhases(),
        result.getNumberOfUsedPhases());

    assertEquals("Quality: ", verificationResult.getQuality(), result.getQuality());

    // not currently examining the phase list.
  }

  /**
   * This function loads the data in the given file path as a string.
   *
   * @param filePath A String containing the path to the file to load
   * @return A String containing the data in the file
   */
  String loadFromFile(String filePath) {
    // read the input
    BufferedReader inputReader = null;
    String fileString = "";
    try {
      inputReader = new BufferedReader(new FileReader(filePath));
      String text = null;

      // each line is assumed to be part of the input
      while ((text = inputReader.readLine()) != null) {
        fileString += text;
      }
    } catch (FileNotFoundException e) {
      // no file
      System.out.println("File not found");
      return "";
    } catch (IOException e) {
      // problem reading
      return "";
    } finally {
      try {
        if (inputReader != null) {
          inputReader.close();
        }
      } catch (IOException e) {
        // can't close
      }
    }

    return fileString;
  }
}
