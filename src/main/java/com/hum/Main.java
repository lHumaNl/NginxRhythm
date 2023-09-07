package com.hum;

import org.apache.commons.cli.ParseException;

import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) {
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

            RequestExecutor requestExecutor = new RequestExecutor(
                    arguments.getScaleLoad(),
                    arguments.getRequestsThreads(),
                    arguments.getTimeout(),
                    arguments.isIgnoreSsl(),
                    arguments.getRequestQueueCapacity(),
                    arguments.getQueuePolicy()
            );

            for (LogEntry logEntry : logParser.getLogEntries()) {
                Thread.sleep(logEntry.getDelay());
                try {
                    requestExecutor.executeRequest(logEntry);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            requestExecutor.shutDown();
        } catch (ParseException | URISyntaxException | InterruptedException | RuntimeException e) {
            e.printStackTrace();
        }
    }
}