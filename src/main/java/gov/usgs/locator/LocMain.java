package gov.usgs.locator;

import gov.usgs.detectionformats.Detection;
import gov.usgs.processingformats.LocationException;
import gov.usgs.processingformats.LocationRequest;
import gov.usgs.processingformats.LocationResult;
import gov.usgs.processingformats.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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

  /** A String containing the argument for specifying the input file. */
  public static final String INPUTFILE_ARGUMENT = "--inputFile=";

  /** A String containing the argument for specifying the input file type. */
  public static final String INPUTTYPE_ARGUMENT = "--inputType=";

  /** A String containing the argument for specifying the output file. */
  public static final String OUTPUTFILE_ARGUMENT = "--outputFile=";

  /** A String containing the argument for specifying the output file type. */
  public static final String OUTPUTTYPE_ARGUMENT = "--outputType=";

  /** A String containing the argument for requesting the locator version. */
  public static final String VERSION_ARGUMENT = "--version";

  /** A String containing the argument for specifying a log file. */
  public static final String LOGFILE_ARGUMENT = "--logFile=";

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

  /** A String containing the argument for specifying to output a csv file. */
  public static final String CSVFILE_ARGUMENT = "--csvFile=";

  /** Mode to process one file (default) */
  public static final String MODE_SINGLE = "single";
  /** Mode to process batch */
  public static final String MODE_BATCH = "batch";
  /** Mode to run web service. */
  public static final String MODE_SERVICE = "service";

  /** A String containing the argument for specifying the location configuration file path. */
  public static final String LOCCONFIG_ARGUMENT = "--locationConfig=";

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
          "Version: "
              + VERSION
              + "\nUsage:\nneic-locator --modelPath=[model path] --inputType=[json, detection, or hydra] "
              + "\n\t[--logFile=[optional log file]] --logPath=[log file path] --logLevel=[logging level] "
              + "\n\t--inputFile=[input file path] [--outputFile=[optional output file path]] "
              + "\n\t[--outputType=[optional json or hydra]] [--locationConfig='optional config file path']"
              + "\nneic-locator --mode=batch --modelPath=[model path] "
              + "\n\t--inputType=[json, detection, or hydra] [--logFile=[optional log file]] "
              + "\n\t--logPath=[log file path] --logLevel=[logging level] "
              + "\n\t--inputDir=[input directory path] --outputDir=[output directory path] "
              + "\n\t[--archiveDir=[optional archive path]] [--outputType=[optional json or hydra]] "
              + "\n\t[--locationConfig='optional config file path']"
              + "\n\t--csvFile=[optional csv file path]"
              + "\nneic-locator --mode=service");
      System.exit(1);
    }

    // Default paths
    String mode = MODE_SINGLE;
    String logFile = null;
    String logPath = "./";
    String logLevel = "INFO";
    String modelPath = null;
    String inputFile = null;
    String inputType = "hydra";
    String outputFile = null;
    String outputType = "hydra";
    String inputPath = "./input";
    String inputExtension = null;
    String outputPath = "./output";
    String outputExtension = null;
    String archivePath = null;
    String csvFile = null;
    String locationConfigPath = null;

    // process arguments
    StringBuffer argumentList = new StringBuffer();
    for (String arg : args) {
      // save arguments for logging
      argumentList.append(arg).append(" ");

      if (arg.startsWith(MODELPATH_ARGUMENT)) {
        // get model path
        modelPath = arg.replace(MODELPATH_ARGUMENT, "");
      } else if (arg.startsWith(INPUTFILE_ARGUMENT)) {
        // get file path
        inputFile = arg.replace(INPUTFILE_ARGUMENT, "");
      } else if (arg.startsWith(INPUTTYPE_ARGUMENT)) {
        // get file type
        inputType = arg.replace(INPUTTYPE_ARGUMENT, "");
        outputType = inputType;
      } else if (arg.startsWith(OUTPUTFILE_ARGUMENT)) {
        // get file path
        outputFile = arg.replace(OUTPUTFILE_ARGUMENT, "");
      } else if (arg.startsWith(OUTPUTTYPE_ARGUMENT)) {
        // get file type
        outputType = arg.replace(OUTPUTTYPE_ARGUMENT, "");
      } else if (arg.startsWith(LOGFILE_ARGUMENT)) {
        // get log file
        logFile = arg.replace(LOGFILE_ARGUMENT, "");
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
        // get mode
        mode = arg.replace(MODE_ARGUMENT, "");
      } else if (arg.startsWith(CSVFILE_ARGUMENT)) {
        // get csv file
        csvFile = arg.replace(CSVFILE_ARGUMENT, "");
      } else if (arg.startsWith(LOCCONFIG_ARGUMENT)) {
        // get locator configuration
        locationConfigPath = arg.replace(LOCCONFIG_ARGUMENT, "");
      }
    }

    if ("json".equals(inputType)) {
      // we expect json inputs to use the standard processing formats extension
      // for requests
      inputExtension = ".locrequest";
    } else if ("detection".equals(inputType)) {
      // we expect detection inputs to use the standard detection formats extension
      // for detections
      inputExtension = ".jsondetect";
    } else {
      // hydra input files have a .txt extension
      inputExtension = ".txt";
    }

    if ("hydra".equals(outputType)) {
      // hydra output files have a .out extension
      outputExtension = ".out";
    } else {
      // we expect json outputs to use the standard processing formats extension
      // for results, and there is no specified detection formats extension, so
      // we'll use the same as json outputs
      outputExtension = ".locresult";
    }

    LocMain locMain = new LocMain();

    // setup logging
    if (logFile != null) {
      locMain.setupLogging(null, logFile, logLevel);
    } else if (inputFile != null) {
      locMain.setupLogging(logPath, getFileName(inputFile) + ".log", logLevel);
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

    boolean locRC = false;

    // get location config
    JSONObject locationConfig = null;
    if (locationConfigPath != null) {
      locationConfig = loadJSONFromFile(locationConfigPath);
    }

    if (locationConfig != null) {
      LOGGER.info("Loaded locationConfig");
    }

    if (MODE_SERVICE.equals(mode)) {
      gov.usgs.locatorservice.Application.main(args);
      // service runs in separate thread, just return from this method...
      return;
    } else if (MODE_BATCH.equals(mode)) {
      locRC =
          locMain.locateManyEvents(
              modelPath,
              inputPath,
              inputExtension,
              outputPath,
              outputExtension,
              archivePath,
              inputType,
              outputType,
              csvFile,
              locationConfig);
    } else {
      locRC =
          locMain.locateSingleEvent(
              modelPath,
              inputFile,
              inputType,
              outputFile,
              outputType,
              outputPath,
              outputExtension,
              csvFile,
              locationConfig);
    }

    // Exit.
    if (locRC) {
      LOGGER.info("Successful completion of locator (exit 0).");
      System.exit(0);
    }
    LOGGER.info("Unsuccessful completion of locator (exit 1).");
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

    if (logPath != null) {
      LOGGER.config("Log directory '" + logPath + "'");
    }

    if (logFile != null) {
      LOGGER.config("Log file '" + logFile + "'");
    }

    Logger rootLogger = Logger.getLogger("");
    rootLogger.setLevel(level);

    // create log directory, log file, and file handler
    try {
      if (logPath != null) {
        File logDirectoryFile = new File(logPath);
        if (!logDirectoryFile.exists()) {
          LOGGER.fine("Creating log directory");
          if (!logDirectoryFile.mkdirs()) {
            LOGGER.warning("Unable to create log directory");
          }
        }
      }

      FileHandler fileHandler;

      if (logPath != null) {
        fileHandler = new FileHandler(logPath + "/" + logFile);
      } else {
        fileHandler = new FileHandler(logFile);
      }

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

    return filePath.substring(start + 1, end);
  }

  /**
   * This function returns the current local time as a string
   *
   * @return A String containing current local time formatted in the form "yyyyMMdd_HHmmss".
   */
  public static String getCurrentLocalDateTimeStamp() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
  }

  /**
   * This function loads a the contents of a file into a json object
   *
   * @param filePath A String containing the full path to the input file
   * @return A JSONObject containing the file contents, or null if the file was invalid.
   */
  public static JSONObject loadJSONFromFile(String filePath) {
    JSONObject fileJSONObject = null;
    String fileString = loadStringFromFile(filePath);

    if ("".equals(fileString)) {
      LOGGER.severe("String from file is empty.");
      return (null);
    }

    try {
      // use a parser to convert to a string
      JSONParser parser = new JSONParser();
      fileJSONObject = (JSONObject) parser.parse(fileString);
    } catch (ParseException e) {
      LOGGER.severe("JSON string parse exception.");
      LOGGER.severe(e.toString());
      return (null);
    }

    return (fileJSONObject);
  }

  /**
   * This function loads a the contents of a file into a string
   *
   * @param filePath A String containing the full path to the input file
   * @return A String containing the file contents, or empty string if the file was invalid.
   */
  public static String loadStringFromFile(String filePath) {
    String fileString = "";

    if (filePath == null) {
      LOGGER.severe("File Path is not valid.");
      return (null);
    } else if ("".equals(filePath)) {
      LOGGER.severe("File Path is empty.");
      return (null);
    } else {
      BufferedReader fileBufferedReader = null;
      try {
        FileReader stringFile = new FileReader(filePath);
        fileBufferedReader = new BufferedReader(stringFile);

        String line = "";
        while ((line = fileBufferedReader.readLine()) != null) {
        	System.out.println(line);
          fileString += line + "\n";
        }
      } catch (FileNotFoundException e) {
        LOGGER.severe(e.toString());
        return (null);
      } catch (IOException e) {
        LOGGER.severe(e.toString());
        return (null);
      } finally {
        try {
          if (fileBufferedReader != null) {
            fileBufferedReader.close();
          }
        } catch (IOException e) {
          LOGGER.severe(e.toString());
        }
      }
    }

    return (fileString);
  }

  /**
   * This function locates a single event.
   *
   * @param modelPath A String containing the path to the required model files
   * @param inputFile A String containing the full path to the locator input file
   * @param inputType A String containing the type of the locator input file
   * @param outputFile An optional String containing the full path to the locator output file,
   *     overrides output path
   * @param outputType A String containing the type of the locator output file
   * @param outputExtension A String containing the extension to use for output files
   * @param outputPath A String containing the path to write the locator output file
   * @param csvFile An optional String containing full path to the csv formatted file, null to
   *     disable
   * @param locationConfig An optional JSONObject containing the locator config for detections, null
   *     to disable
   * @return A boolean flag indicating whether the locaton was successful
   */
  public boolean locateSingleEvent(
      String modelPath,
      String inputFile,
      String inputType,
      String outputFile,
      String outputType,
      String outputPath,
      String outputExtension,
      String csvFile,
      JSONObject locationConfig) {

    // read the file
    String inputString = loadStringFromFile(inputFile);

    if (inputString == null) {
      LOGGER.severe("String from file is null.");
      return false;
    }

    if ("".equals(inputString)) {
      LOGGER.severe("String from file is empty.");
      return false;
    }

    // parse the file
    LocationRequest request = null;
    if ("json".equals(inputType)) {
      LOGGER.fine("Parsing a json file.");

      // parse into request
      try {
        request = new LocationRequest(Utility.fromJSONString(inputString));
      } catch (ParseException e) {
        // parse failure
        LOGGER.severe("Exception: " + e.toString());
        return false;
      }
    } else if ("detection".equals(inputType)) {
      LOGGER.fine("Parsing a detection file.");

      // parse into detection
      Detection detection = null;
      try {
        detection = new Detection(Utility.fromJSONString(inputString));
      } catch (ParseException e) {
        // parse failure
        LOGGER.severe("Exception: " + e.toString());
        return false;
      }

      // convert to request
      // Use LocInput to get access to proper constructor
      LocInput detectIn = new LocInput(detection, locationConfig);
      request = (LocationRequest) detectIn;
    } else {
      LOGGER.fine("Parsing a hydra file.");

      // Use LocInput to get access to read routine
      LocInput hydraIn = new LocInput();
      if (!hydraIn.readHydra(inputString)) {
        return false;
      }

      request = (LocationRequest) hydraIn;

      // use file name as ID
      request.ID = getFileName(inputFile);
    }

    // do location
    LocationResult result = null;
    if (request != null) {
      try {
        // set up service
        LocService service = new LocService(modelPath);
        result = service.getLocation(request);
      } catch (LocationException e) {
        LOGGER.severe("Exception: " + e.toString());
        return false;
      }
    }

    // Write the result to disk
    if (result != null) {
      // create the output file name
      String outFileName;

      if (outputFile == null) {
        outFileName = outputPath + File.separatorChar + getFileName(inputFile) + outputExtension;
      } else {
        outFileName = outputFile;
      }

      LocOutput locOut = (LocOutput) result;

      try {
        if ("json".equals(outputType)) {
          locOut.writeJSON(outFileName);
        } else {
          locOut.writeHydra(outFileName);
        }
      } catch (Exception e) {
        LOGGER.severe(e.toString());
      }

      // append csv to file
      if (csvFile != null) {
        try {
          FileWriter fileWriter = new FileWriter(csvFile, true); // Set true for append mode
          PrintWriter printWriter = new PrintWriter(fileWriter);
          printWriter.println(result.toCSV());
          printWriter.close();
        } catch (Exception e) {
          LOGGER.severe(e.toString());
        }
      }

      // success
      return true;
    }

    // Exit.
    return false;
  }

  /**
   * This function locates all events in a given input directory
   *
   * @param modelPath A String containing the path to the required model files
   * @param inputPath A String containing the full path to directory containing input files
   * @param inputExtension A String containing the extension of locator input files
   * @param outputPath A String containing the path to write the results
   * @param outputExtension A String containing the extension to use for output files
   * @param archivePath An optional String containing the full path to directory to archive input
   *     files, null to disable, if disabled, input files are deleted
   * @param inputType A String containing the type of the locator input file
   * @param outputType A String containing the type of the locator output file
   * @param csvFile An optional String containing full path to the csv formatted file, null to
   *     disable
   * @param locationConfig An optional JSONObject containing the locator config for detections, null
   *     to disable
   * @return A boolean flag indicating whether the locatons were successful
   */
  public boolean locateManyEvents(
      String modelPath,
      String inputPath,
      String inputExtension,
      String outputPath,
      String outputExtension,
      String archivePath,
      String inputType,
      String outputType,
      String csvFile,
      JSONObject locationConfig) {

    // create the output and archive paths if they don't
    // already exist
    if (outputPath != null) {
      File outputDir = new File(outputPath);
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }
    } else {
      LOGGER.severe("Output Path is not specified, exitting.");
      return false;
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
      return false;
    }

//    LocUtil.record(String.format("%4.2f", LocUtil.BAYESIANSTRENGTH));
    // for all the files currently in the input directory
    for (File inputFile : inputDir.listFiles()) {
      // if the file has the right extension
      if (inputFile.getName().endsWith((inputExtension))) {
        // read the file
        // String fileName = inputFile.getName();
        String filePath = inputFile.getAbsolutePath();
   //   System.out.println("File: " + filePath);
   //   LocUtil.record("\nFile: " + filePath);

        if (locateSingleEvent(
            modelPath,
            filePath,
            inputType,
            null,
            outputType,
            outputPath,
            outputExtension,
            csvFile,
            locationConfig)) {
          // done with the file
          if (archivePath == null) {
            // not archiving, just delete it
            inputFile.delete();
          } else {
            // Move file to archive directory
            inputFile.renameTo(
                new File(
                    archivePath + File.separatorChar + getFileName(filePath) + inputExtension));
          }
        } else {
          // we had an error, rename file as errored so we don't retry the same file
          inputFile.renameTo(new File(filePath + ".error"));
        }
      }
    }
//    LocUtil.record(null);

    // done
    return true;
  }
}
