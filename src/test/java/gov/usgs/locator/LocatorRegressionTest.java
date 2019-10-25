package gov.usgs.locator;

import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.Utility;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    String inputString = loadFromFile("build/resources/test/input.json");
    Assertions.assertNotEquals(EMPTYSTRING, inputString, "Loaded Input");
    LocationRequest request = null;
    try {
      request = new LocationRequest(Utility.fromJSONString(inputString));
    } catch (ParseException e) {
      // parse failure
      Assertions.fail(e.toString());
    }
    Assertions.assertNotNull(request, "Location Request");

    // parse verification string into result
    String verificationString = loadFromFile("build/resources/test/verification.json");
    Assertions.assertNotEquals(EMPTYSTRING, verificationString, "Loaded Verification");
    LocationResult verificationResult = null;
    try {
      verificationResult = new LocationResult(Utility.fromJSONString(verificationString));
    } catch (ParseException e) {
      // parse failure
      Assertions.fail(e.toString());
    }
    Assertions.assertNotNull(verificationResult, "Verification Result");

    // do location
    LocationResult result = null;
    LocService service = new LocService("build/models/");
    try {
      result = service.getLocation(request);
    } catch (LocationException e) {
      Assertions.fail(e.toString());
    }
    Assertions.assertNotNull(result, "Location Result");

    // check location result parameters against verification result
    Assertions.assertEquals(
        verificationResult.getHypocenter().getLatitude(),
        result.getHypocenter().getLatitude(),
        0.0001,
        "Latitude");

    Assertions.assertEquals(
        verificationResult.getHypocenter().getLatitudeError(),
        result.getHypocenter().getLatitudeError(),
        0.0001,
        "Latitude Error");

    Assertions.assertEquals(
        verificationResult.getHypocenter().getLongitude(),
        result.getHypocenter().getLongitude(),
        0.0001,
        "Longitude");

    Assertions.assertEquals(
        verificationResult.getHypocenter().getLongitudeError(),
        result.getHypocenter().getLongitudeError(),
        0.0001,
        "Longitude Error");

    Assertions.assertEquals(
        verificationResult.getHypocenter().getDepth(),
        result.getHypocenter().getDepth(),
        0.0001,
        "Depth");

    Assertions.assertEquals(
        verificationResult.getHypocenter().getDepthError(),
        result.getHypocenter().getDepthError(),
        0.0001,
        "Depth Error");

    Assertions.assertEquals(
        verificationResult.getHypocenter().getTime().getTime(),
        result.getHypocenter().getTime().getTime(),
        "Time");

    Assertions.assertEquals(
        verificationResult.getHypocenter().getTimeError(),
        result.getHypocenter().getTimeError(),
        0.0001,
        "Time Error");

    Assertions.assertEquals(
        verificationResult.getMinimumDistance(),
        result.getMinimumDistance(),
        0.0001,
        "Minimum Distance");

    Assertions.assertEquals(verificationResult.getRMS(), result.getRMS(), 0.0001, "RMS");

    Assertions.assertEquals(verificationResult.getGap(), result.getGap(), 0.0001, "Gap");

    Assertions.assertEquals(
        verificationResult.getNumberOfUsedStations(),
        result.getNumberOfUsedStations(),
        "Used Stations");

    Assertions.assertEquals(
        verificationResult.getNumberOfUsedPhases(), result.getNumberOfUsedPhases(), "Used Phases");

    Assertions.assertEquals(verificationResult.getQuality(), result.getQuality(), "Quality");

    // not currently examining the phase list.
  }

  /**
   * This function loads the data in the given file path as a string.
   *
   * @param filePath A String containing the path to the file to load
   * @return A String containing the data in the file
   */
  private String loadFromFile(String filePath) {
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
