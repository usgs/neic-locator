package gov.usgs.locator;

import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test driver for the locator.
 *
 * @author John Patton
 */
public class WholeLocatorTest {
  public static String EMPTYSTRING = "";

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
    LocService service = new LocService("/build/models/");
    try {
      result = service.getLocation(request);
    } catch (LocationException e) {
      fail(e.toString());
    }
    assertNotEquals("Location Result", result, null);

    // check location result against verification result
    assertEquals("Latitude:", verificationResult.getHypocenter().getLatitude(), 
        result.getHypocenter().getLatitude());

    assertEquals("Longitude: ", verificationResult.getHypocenter().getLongitude(), 
        result.getHypocenter().getLongitude());        
  }


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