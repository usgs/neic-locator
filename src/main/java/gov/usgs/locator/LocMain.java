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
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

  /** A String containing the argument for specifying the input directory. */
  public static final String INPUTDIR_ARGUMENT = "--inputDir=";

  /** A String containing the argument for specifying the output directory. */
  public static final String OUTPUTDIR_ARGUMENT = "--outputDir=";

  /** A String containing the argument for specifying the archive directory. */
  public static final String ARCHIVEDIR_ARGUMENT = "--archiveDir=";

  /** A String containing the argument for specifying the mode. */
  public static final String MODE_ARGUMENT = "--mode=";

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
    String mode = "single";
    String logPath = "./";
    String logLevel = "INFO";
    String modelPath = null;
    String filePath = null;
    String fileType = "hydra";
    String inputPath = "./input";
    String inputExtension = null;
    String outputPath = "./output";
    String outputExtension = null;
    String archivePath = null;

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
      } else if (arg.startsWith(INPUTDIR_ARGUMENT)) {
        // get input path
        inputPath = arg.replace(INPUTDIR_ARGUMENT, "");
      } else if (arg.startsWith(OUTPUTDIR_ARGUMENT)) {
        // get output path
        outputPath = arg.replace(OUTPUTDIR_ARGUMENT, "");
      } else if (arg.startsWith(ARCHIVEDIR_ARGUMENT)) {
        // get archive path
        archivePath = arg.replace(ARCHIVEDIR_ARGUMENT, "");
      } else if (arg.startsWith(MODE_ARGUMENT)) {
        // get archive path
        mode = arg.replace(MODE_ARGUMENT, "");
      }
    }

    if ("json".equals(fileType)) {
      inputExtension = ".locrequest";
      outputExtension = ".locresult";
    } else {
      inputExtension = ".txt";
      outputExtension = ".out";
    }

    LocMain locMain = new LocMain();

    // setup logging
    if (filePath != null) {
      locMain.setupLogging(logPath, getFileName(filePath) + ".log", logLevel);
    } else {
      locMain.setupLogging(logPath, getCurrentLocalDateTimeStamp() + "_locator.log", logLevel);
    }

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

    int locRC = -1;

    if ("batch".equals(mode)) {
      locRC =
          locMain.locateManyEvents(
              modelPath,
              inputPath,
              inputExtension,
              outputPath,
              outputExtension,
              archivePath,
              fileType);
    } else {
      locRC = locMain.locateSingleEvent(modelPath, filePath, fileType, outputExtension);
    }

    // Exit.
    System.exit(locRC);
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
    if (start < 0) {
      start = 0;
    }

    int end = filePath.lastIndexOf(".");
    if (end <= 0) {
      end = filePath.length();
    }

    return filePath.substring(start, end);
  }

  public static String getCurrentLocalDateTimeStamp() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
  }

  public int locateSingleEvent(
      String modelPath, String filePath, String fileType, String outputExtension) {
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
        return (1);
      } catch (IOException e) {
        // problem reading
        LOGGER.severe("Exception: " + e.toString());
        return (1);
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
        return (1);
      }

      // always print input as json for debugging
      String jsonString = Utility.toJSONString(request.toJSON());
      LOGGER.fine("JSON Input: \n" + jsonString);

      // do location
      try {
        result = service.getLocation(request);
      } catch (LocationException e) {
        LOGGER.severe("Exception: " + e.toString());
        return (1);
      }
    } else {
      LOGGER.fine("Reading a hydra file.");

      // run as LocInput/LocOutput to get access to read/write routines
      LocInput hydraIn = new LocInput();
      LocOutput hydraOut = null;
      if (!hydraIn.readHydra(filePath)) {
        return (0);
      }

      // always print input as json for debugging
      String jsonString = Utility.toJSONString(hydraIn.toJSON());
      LOGGER.fine("JSON Input: " + jsonString);

      // do location
      try {
        hydraOut = service.getLocation(hydraIn);
      } catch (LocationException e) {
        LOGGER.severe("Exception: " + e.toString());
        return (1);
      }

      if (hydraOut != null) {
        LOGGER.fine("Writing a hydra file.");
        hydraOut.writeHydra(filePath + ".out");
        result = (LocationResult) hydraOut;
      }
    }

    // always print result as json for debugging
    if (result != null) {
      String resultString = Utility.toJSONString(result.toJSON());
      LOGGER.fine("JSON Output: \n" + resultString);

      if ("json".equals(fileType)) {
        // create the output file name
        String outFileName = "./" + getFileName(filePath) + outputExtension;

        // Write the request to disk
        try {
          PrintWriter outputWriter = new PrintWriter(outFileName, "UTF-8");
          outputWriter.print(resultString);
          outputWriter.close();
        } catch (Exception e) {
          LOGGER.severe(e.toString());
          return (1);
        }
      }

      return (0);
    }

    // Exit.
    return (1);
  }

  public int locateManyEvents(
      String modelPath,
      String inputPath,
      String inputExtension,
      String outputPath,
      String outputExtension,
      String archivePath,
      String fileType) {

    // set up service
    LocService service = new LocService(modelPath);

    // create the output and archive paths if they don't
    // already exist
    if (outputPath != null) {
      File outputDir = new File(outputPath);
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }
    } else {
      LOGGER.severe("Output Path is not specified, exitting.");
      return 0;
    }

    if (archivePath != null) {
      File archiveDir = new File(archivePath);
      if (!archiveDir.exists()) {
        archiveDir.mkdirs();
      }
    }

    // setup the directories
    File inputDir = new File(inputPath);
    if (!inputDir.exists()) {
      LOGGER.severe("Input Path is not valid, exitting.");
      return (1);
    }

    // for all the files in the input directory
    for (File inputFile : inputDir.listFiles()) {
      String fileName = "";

      // if the file has the right extension
      if (inputFile.getName().endsWith((inputExtension))) {
        // read the file
        fileName = inputFile.getName();
        BufferedReader inputReader = null;
        String requestString = "";
        LocationRequest request = null;
        LocationResult result = null;

        if ("json".equals(fileType)) {
          try {
            inputReader = new BufferedReader(new FileReader(inputFile));

            String line = "";
            // each file is assumed to be a single message
            while ((line = inputReader.readLine()) != null) {
              requestString += line;
            }
          } catch (FileNotFoundException e) {
            LOGGER.severe(e.toString());
          } catch (IOException e) {
            LOGGER.severe(e.toString());
          } finally {
            try {
              if (inputReader != null) {
                inputReader.close();
              }
            } catch (IOException e) {
              LOGGER.severe(e.toString());
            }
          }

          // parse into request
          try {
            request = new LocationRequest(Utility.fromJSONString(requestString));
          } catch (ParseException e) {
            // parse failure
            LOGGER.severe("Exception: " + e.toString());
            return (1);
          }
        } else {
          // run as LocInput/LocOutput to get access to read/write routines
          LocInput hydraIn = new LocInput();
          if (!hydraIn.readHydra(inputPath + File.separatorChar + fileName)) {
            LOGGER.severe(
                "Failed to read hydra file: " + inputPath + File.separatorChar + fileName);
            return (0);
          }
          request = (LocationRequest) hydraIn;
        }

        // done with the file
        if (archivePath == null) {
          // not archiving, just delete it
          inputFile.delete();
        } else {
          // Move file to archive directory
          inputFile.renameTo(new File(archivePath + File.separatorChar + fileName));
        }

        // do location
        try {
          result = service.getLocation(request);
        } catch (LocationException e) {
          LOGGER.severe("Exception: " + e.toString());
          return (1);
        }

        // write file
        if (result != null) {
          // create the output file name
          String outFileName =
              outputPath + File.separatorChar + getFileName(fileName) + outputExtension;

          if ("json".equals(fileType)) {
            String resultString = Utility.toJSONString(result.toJSON());

            // Write the request to disk
            try {
              PrintWriter outputWriter = new PrintWriter(outFileName, "UTF-8");
              outputWriter.print(resultString);
              outputWriter.close();
            } catch (Exception e) {
              LOGGER.severe(e.toString());
              continue;
            }
          } else {
            LocOutput hydraOut = (LocOutput) result;
            hydraOut.writeHydra(outFileName);
          }
        }
      }
    }

    // done
    return (1);
  }
}
