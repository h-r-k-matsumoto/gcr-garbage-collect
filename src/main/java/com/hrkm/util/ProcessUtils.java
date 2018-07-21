package com.hrkm.util;

import static org.springframework.boot.logging.LogLevel.*;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import com.hrkm.Application;

public final class ProcessUtils {

  private static Logger log = LoggerFactory.getLogger(Application.class);


  private ProcessUtils() {}

  public static void process(Consumer<String> lineParser, List<String> args) {
    ProcessBuilder processBuilder = null;
    processBuilder = new ProcessBuilder(args);
    // processBuilder.redirectErrorStream(true);
    Process process = null;
    Thread in = null;
    Thread err = null;
    try {
      String command = String.join(" ", args);
      log.info("process execution: " + command);
      process = processBuilder.start();

      in = new StreamReaderThread(process.getInputStream(), lineParser, DEBUG);
      err = new StreamReaderThread(process.getErrorStream(), null, ERROR);
      in.start();
      err.start();
      process.waitFor();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      try {
        if (process != null)
          process.destroy();
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

  public static class StreamReaderThread extends Thread {

    private InputStream inputStream;

    private Consumer<String> lineReader;

    private LogLevel level;

    public StreamReaderThread(InputStream inputStream, Consumer<String> lineReader,
        LogLevel level) {
      this.inputStream = inputStream;
      this.lineReader = lineReader;
      this.level = level;
    }

    @Override
    public void run() {
      try (InputStream in = this.inputStream; Scanner scanner = new Scanner(in);) {
        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          if (level == INFO && log.isInfoEnabled()) {
            log.info(line);
          } else if (level == ERROR) {
            log.error(line);
          }
          if (lineReader != null) {
            lineReader.accept(line);
          }
        }
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
