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
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;

/**
 * Test driver for the locator.
 *
 * @author John Patton
 */
public class LocMain {
  /** A String containing the locator version */
  public static final String VERSION = "v0.1.0";

  /** A String containing the argument for specifying the model file path. */
  public static final String MODELPATH_ARGUMENT = "--modelPath=";

  /** A String containing the argument for specifying the input file path. */
  public static final String FILEPATH_ARGUMENT = "--filePath=";

  /** A String containing the argument for specifying the input file type. */
  public static final String FILETYPE_ARGUMENT = "--fileType=";

  /** A String containing the argument for requesting the locator version. */
  public static final String VERSION_ARGUMENT = "--version";

  /** A String containing the argument for specifying a log file path. */
  public static final String LOGPATH_ARGUMENT = "--logPath=";

  /** A String containing the argument for specifying a log level. */
  public static final String LOGLEVEL_ARGUMENT = "--logLevel=";

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(LocMain.class.getName());

  /**
   * Main program for running the locator.
   *
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      System.out.println(
          "Usage: neic-locator --modelPath=[model path] "
              + "--filePath=[file path] --fileType=[file type] "
              + "--logPath=[log file path] --logLevel=[logging level]");
      System.exit(1);
    }

    // Default paths
    String modelPath = null;
    String filePath = null;
    String fileType = "hydra";
    String logPath = "./";
    String logLevel = "INFO";

    // process arguments
    StringBuffer argumentList = new StringBuffer();
    for (String arg : args) {
      // save arguments for logging
      argumentList.append(arg).append(" ");

      if (arg.startsWith(MODELPATH_ARGUMENT)) {
        // get model path
        modelPath = arg.replace(MODELPATH_ARGUMENT, "");
      } else if (arg.startsWith(FILEPATH_ARGUMENT)) {
        // get file path
        filePath = arg.replace(FILEPATH_ARGUMENT, "");
      } else if (arg.startsWith(FILETYPE_ARGUMENT)) {
        // get file type
        fileType = arg.replace(FILETYPE_ARGUMENT, "");
      } else if (arg.startsWith(LOGPATH_ARGUMENT)) {
        // get log path
        logPath = arg.replace(LOGPATH_ARGUMENT, "");
      } else if (arg.startsWith(LOGLEVEL_ARGUMENT)) {
        // get log level
        logLevel = arg.replace(LOGLEVEL_ARGUMENT, "");
      } else if (arg.equals(VERSION_ARGUMENT)) {
        // print version
        System.err.println("neic-locator");
        System.err.println(VERSION);
        System.exit(0);
      }
    }

    LocMain locMain = new LocMain();

    // setup logging
    locMain.setupLogging(logPath, getFileName(filePath) + ".log", logLevel);

    // print out version
    LOGGER.info("neic-locator " + VERSION);

    // log args
    LOGGER.fine("Command line arguments: " + argumentList.toString().trim());

    // log java and os information
    LOGGER.config("java.vendor = " + System.getProperty("java.vendor"));
    LOGGER.config("java.version = " + System.getProperty("java.version"));
    LOGGER.config("java.home = " + System.getProperty("java.home"));
    LOGGER.config("os.arch = " + System.getProperty("os.arch"));
    LOGGER.config("os.name = " + System.getProperty("os.name"));
    LOGGER.config("os.version = " + System.getProperty("os.version"));
    LOGGER.config("user.dir = " + System.getProperty("user.dir"));
    LOGGER.config("user.name = " + System.getProperty("user.name"));

    // set up service
    LocService service = new LocService(modelPath);

    // load the event based on the file type
    LocationRequest request = null;
    LocationResult result = null;
    if ("json".equals(fileType)) {
      LOGGER.fine("Reading a json file.");

      // read the file
      BufferedReader inputReader = null;
      String inputString = "";
      try {
        inputReader = new BufferedReader(new FileReader(filePath));
        String text = null;

        // each line is assumed to be part of the input
        while ((text = inputReader.readLine()) != null) {
          inputString += text;
        }
      } catch (FileNotFoundException e) {
        // no file
        LOGGER.severe("Exception: " + e.toString());
        System.exit(1);
      } catch (IOException e) {
        // problem reading
        LOGGER.severe("Exception: " + e.toString());
        System.exit(1);
      } finally {
        try {
          if (inputReader != null) {
            inputReader.close();
          }
        } catch (IOException e) {
          // can't close
          LOGGER.severe("Exception: " + e.toString());
        }
      }

      // parse into request
      try {
        request = new LocationRequest(Utility.fromJSONString(inputString));
      } catch (ParseException e) {
        // parse failure
        LOGGER.severe("Exception: " + e.toString());
        System.exit(1);
      }

      // always print input as json for debugging
      String jsonString = Utility.toJSONString(request.toJSON());
      LOGGER.fine("JSON Input: \n" + jsonString);

      // do location
      try {
        result = service.getLocation(request);
      } catch (LocationException e) {
        LOGGER.severe("Exception: " + e.toString());
        System.exit(1);
      }
    } else {
      LOGGER.fine("Reading a hydra file.");

      // run as LocInput/LocOutput to get access to read/write routines
      LocInput hydraIn = new LocInput();
      LocOutput hydraOut = null;
      if (!hydraIn.readHydra(filePath)) {
        System.exit(0);
      }

      // always print input as json for debugging
      String jsonString = Utility.toJSONString(hydraIn.toJSON());
      LOGGER.fine("JSON Input: " + jsonString);

      // do location
      try {
        hydraOut = service.getLocation(hydraIn);
      } catch (LocationException e) {
        LOGGER.severe("Exception: " + e.toString());
        System.exit(1);
      }

      if (hydraOut != null) {
        LOGGER.fine("Writing a hydra file.");
        hydraOut.writeHydra(filePath + ".out");
        result = (LocationResult) hydraOut;
      }
    }

    // always print result as json for debugging
    if (result != null) {
      String jsonString = Utility.toJSONString(result.toJSON());
      LOGGER.fine("JSON Output: \n" + jsonString);
      System.exit(0);
    }

    // Exit.
    System.exit(1);
  }

  /**
   * This function sets up logging for the locator.
   *
   * @param logPath A String containing the path to write log files to
   * @param logFile A String containing the name of the log file
   * @param logLevel A String holding the desired log level
   */
  public void setupLogging(String logPath, String logFile, String logLevel) {
    LogManager.getLogManager().reset();

    // parse the logging level
    Level level = getLogLevel(logLevel);

    LOGGER.config("Logging Level '" + level + "'");
    LOGGER.config("Log directory '" + logPath + "'");

    Logger rootLogger = Logger.getLogger("");
    rootLogger.setLevel(level);

    // create log directory, log file, and file handler
    try {
      File logDirectoryFile = new File(logPath);
      if (!logDirectoryFile.exists()) {
        LOGGER.fine("Creating log directory");
        if (!logDirectoryFile.mkdirs()) {
          LOGGER.warning("Unable to create log directory");
        }
      }

      FileHandler fileHandler = new FileHandler(logPath + "/" + logFile);
      fileHandler.setLevel(level);

      rootLogger.addHandler(fileHandler);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Unable to create log file handler", e);
    }

    // create console handler
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setLevel(level);
    rootLogger.addHandler(consoleHandler);

    // set all handlers to the same formatter
    for (Handler handler : rootLogger.getHandlers()) {
      handler.setFormatter(new SimpleLogFormatter());
    }
  }

  /**
   * This function converts a log level string into a logger level. This function converts a couple
   * of non-standard logging levels / abbreviations.
   *
   * @param logLevel A String holding the desired log level
   * @return A Level object containing the desired log level.
   */
  private Level getLogLevel(String logLevel) {
    if (logLevel == null) {
      return null;
    }
    try {
      return Level.parse(logLevel.toUpperCase());
    } catch (IllegalArgumentException e) {
      if (logLevel.equalsIgnoreCase("DEBUG")) {
        return Level.FINE;
      }
      if (logLevel.equalsIgnoreCase("WARN")) {
        return Level.WARNING;
      }
      throw new IllegalArgumentException(
          "Unresolved log level " + logLevel + " for java.util.logging", e);
    }
  }

  /**
   * This function extracts the file name from a given file path.
   *
   * @param filePath A String containing the full path to the file
   * @return A String containing the file name extracted from the full path.
   */
  public static String getFileName(String filePath) {
    // get the file name from input file path
    int start = filePath.lastIndexOf("/");
    if (start < 0) {
      start = filePath.lastIndexOf("\\");
    }

    int end = filePath.lastIndexOf(".");
    if (end <= 0) {
      end = filePath.length();
    }

    return filePath.substring(start, end);
  }
}
