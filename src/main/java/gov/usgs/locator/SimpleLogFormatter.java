package gov.usgs.locator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * The SimpleLogFormatter is a simple(r) log formatter for java.util.logging messages.
 *
 * <p>This class formats log messages such that it outputs unique dates once, with all messages
 * sharing that time listed below.
 *
 * <p>Example Format:
 *
 * <pre>
 * 20190402_16:00:47: Startup
 * [ INFO    ] [ main       ] neic-locator v0.1.0
 * [ FINE    ] [ main       ] Command line arguments: --modelPath=./models/
 * --filePath=./raylocinput1000010563_23.txt --fileType=hydra --logLevel=fine
 * [ CONFIG  ] [ main       ] java.vendor = Oracle Corporation
 * [ CONFIG  ] [ main       ] java.version = 10.0.2-adoptopenjdk
 * [ CONFIG  ] [ main       ] java.home = /usr/local/Cellar/adoptopenjdk-openjdk10/jdk-10.0.2+13
 * [ CONFIG  ] [ main       ] os.arch = x86_64
 * </pre>
 */
public class SimpleLogFormatter extends Formatter {
  /** A long containing the milliseconds in a second. */
  public static final long MILLIS_PER_SECOND = 1000;

  /** A long holding when the last LogRecord was processed. */
  private long lastMillis = 0;

  /**
   * Function to format a LogRecord for output.
   *
   * @param record A LogRecord object to format.
   * @return A String containing the formatted LogRecord
   */
  public final String format(final LogRecord record) {
    StringBuffer buf = new StringBuffer();
    Format timeFormatter = new SimpleDateFormat("yyyyMMdd_HH:mm:ss:");

    // truncate to the nearest second
    long millis = (record.getMillis() / MILLIS_PER_SECOND) * MILLIS_PER_SECOND;

    // is the the first run
    if (lastMillis == 0) {
      lastMillis = millis;

      // add date and startup message
      buf.append(timeFormatter.format(new Date(millis))).append(" Startup\n");
    } else if (millis != lastMillis) {
      lastMillis = millis;

      // add date
      buf.append(timeFormatter.format(new Date(lastMillis))).append("\n");
    }

    // add log level
    buf.append(String.format("[ %s ] ", record.getLevel().getLocalizedName()));

    // add method name
    buf.append(
        String.format("[ %s ] ", record.getSourceClassName() + "." + record.getSourceMethodName()));

    // add log message
    buf.append(record.getMessage());
    buf.append("\n");

    // output any associated exception
    Throwable thrown = record.getThrown();
    if (thrown != null) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      thrown.printStackTrace(new PrintStream(out, true));
      buf.append(new String(out.toByteArray()));
    }

    return buf.toString();
  }
}
