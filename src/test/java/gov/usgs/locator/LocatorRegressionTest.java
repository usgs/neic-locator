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
   * These tests are designed as a overall regression tests for the locator. If the behavior of the
   * locator is changed in such a that the location results are expected to change, these tests
   * (specifically the *verification.json files) will need to be updated
   */


  @Test
  public void runGlobalTest() {
    System.out.println("runGlobalTest:");
    // Chile 4.5
    runLocTest(
        "build/resources/test/globalInput.json", "build/resources/test/globalVerification.json");
  }

  @Test
  public void runDeepTest() {
    System.out.println("runDeepTest:");
    // Fiji 5.5 520km
    runLocTest("build/resources/test/deepInput.json", "build/resources/test/deepVerification.json");
  }

  @Test
  public void runBigTest() {
    System.out.println("runBigTest:");
    // South Sandwich Islands 7.5
    runLocTest("build/resources/test/bigInput.json", "build/resources/test/bigVerification.json");
  }

  @Test
  public void runWUSTest() {
    System.out.println("runWUSTest:");
    // Texas 3.0
    runLocTest(
        "build/resources/test/wusInput.json", "build/resources/test/wusVerification.json");
  }

  @Test
  public void runCUSTest() {
    System.out.println("runCUSTest:");
    // North Carolina
    runLocTest("build/resources/test/cusInput.json", "build/resources/test/cusVerification.json");
  }

  public void runLocTest(String inputFile, String verificationFile) {
    // parse input string into request
    String inputString = loadFromFile(inputFile);
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
    String verificationString = loadFromFile(verificationFile);
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
    try {
      LocService service = new LocService("build/models/", "build/models/");
      result = service.getLocation(request);
    } catch (LocationException e) {
      Assertions.fail(e.toString());
    }
    Assertions.assertNotNull(result, "Location Result");

    // print result
    System.out.println(Utility.toJSONString(result.toJSON()));

    // check location result parameters against verification result
    Assertions.assertEquals(
        verificationResult.Hypocenter.Latitude, result.Hypocenter.Latitude, 0.0001, "Latitude");

    Assertions.assertEquals(
        verificationResult.Hypocenter.LatitudeError,
        result.Hypocenter.LatitudeError,
        0.0001,
        "Latitude Error");

    Assertions.assertEquals(
        verificationResult.Hypocenter.Longitude, result.Hypocenter.Longitude, 0.0001, "Longitude");

    Assertions.assertEquals(
        verificationResult.Hypocenter.LongitudeError,
        result.Hypocenter.LongitudeError,
        0.0001,
        "Longitude Error");

    Assertions.assertEquals(
        verificationResult.Hypocenter.Depth, result.Hypocenter.Depth, 0.0001, "Depth");

    Assertions.assertEquals(
        verificationResult.Hypocenter.DepthError,
        result.Hypocenter.DepthError,
        0.0001,
        "Depth Error");

    Assertions.assertEquals(
        verificationResult.Hypocenter.Time.getTime(), result.Hypocenter.Time.getTime(), "Time");

    Assertions.assertEquals(
        verificationResult.Hypocenter.TimeError, result.Hypocenter.TimeError, 0.0001, "Time Error");

    Assertions.assertEquals(
        verificationResult.MinimumDistance, result.MinimumDistance, 0.0001, "Minimum Distance");

    Assertions.assertEquals(verificationResult.RMS, result.RMS, 0.0001, "RMS");

    Assertions.assertEquals(verificationResult.Gap, result.Gap, 0.0001, "Gap");

    Assertions.assertEquals(
        verificationResult.NumberOfUsedStations, result.NumberOfUsedStations, "Used Stations");

    Assertions.assertEquals(
        verificationResult.NumberOfUsedPhases, result.NumberOfUsedPhases, "Used Phases");

    Assertions.assertEquals(verificationResult.Quality, result.Quality, "Quality");

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
