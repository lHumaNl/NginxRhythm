package com.hum;

import org.apache.commons.cli.ParseException;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        try {
            Arguments arguments = new Arguments(args);

            LogParser logParser = new LogParser(
                    arguments.getNginxLogPath(),
                    arguments.getLogFormat(),
                    arguments.getFormatTime(),
                    arguments.getStartTimestamp(),
                    arguments.getDestinationHost()
            );

            RequestExecutor requestExecutor = new RequestExecutor(arguments.getSpeed(), arguments.getScaleLoad());

            int availableProcessors = Math.max(Runtime.getRuntime().availableProcessors(), 8);
            ExecutorService executor = Executors.newFixedThreadPool(availableProcessors);

            for (LogEntry logEntry : logParser.getLogEntries()) {
                executor.execute(() -> {
                            requestExecutor.executeRequest(logEntry);
                        }
                );
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(3600, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    System.err.println("Shutdown threads is to long");
                    System.exit(1);
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                e.printStackTrace();
                System.exit(1);
            }

        } catch (ParseException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}