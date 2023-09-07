package com.hum;

import org.apache.commons.cli.ParseException;

import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) {
        RequestExecutor requestExecutor = null;
        try {
            Arguments arguments = new Arguments(args);

            LogParser logParser = new LogParser(
                    arguments.getNginxLogPath(),
                    arguments.getLogFormat(),
                    arguments.getFormatTime(),
                    arguments.getStartTimestamp(),
                    arguments.getDestinationHost(),
                    arguments.getHttpProtocol(),
                    arguments.getParserThreads()
            );

            requestExecutor = new RequestExecutor(
                    arguments.getScaleLoad(),
                    arguments.getRequestsThreads(),
                    arguments.getTimeout(),
                    arguments.isIgnoreSsl()
            );

            for (LogEntry logEntry : logParser.getLogEntries()) {
                Thread.sleep(logEntry.getDelay());
                requestExecutor.executeRequest(logEntry);
            }
        } catch (ParseException | URISyntaxException | InterruptedException | RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (requestExecutor != null) requestExecutor.shutDown();
        }
    }
}