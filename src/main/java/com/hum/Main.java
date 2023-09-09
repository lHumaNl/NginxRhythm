package com.hum;

import com.hum.logparsing.LogParser;
import com.hum.logparsing.models.LogEntry;
import com.hum.requestexecutor.RequestExecutor;
import com.hum.requestexecutor.models.RequestExecutorSetup;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) {
        Arguments arguments;
        LogParser logParser;
        RequestExecutor requestExecutor = null;

        try {
            arguments = new Arguments(args);
            logParser = initializeLogParser(arguments);
            requestExecutor = initializeRequestExecutor(arguments);
            executeRequests(logParser, requestExecutor);
        } catch (ParseException | URISyntaxException | InterruptedException | RuntimeException e) {
            e.printStackTrace(System.err);
        } finally {
            if (requestExecutor != null) {
                requestExecutor.shutDown();
            }
        }
    }

    private static LogParser initializeLogParser(Arguments arguments) throws RuntimeException {
        return new LogParser(
                arguments.getNginxLogPath(),
                arguments.getLogFormat(),
                arguments.getFormatTime(),
                arguments.getStartTimestamp(),
                arguments.getDestinationHost(),
                arguments.getHttpProtocol(),
                arguments.getParserThreads()
        );
    }

    private static RequestExecutor initializeRequestExecutor(Arguments arguments) {
        return new RequestExecutor(
                new RequestExecutorSetup(
                        arguments.getScaleLoad(),
                        arguments.getRequestsThreads(),
                        arguments.getConnectTimeout(),
                        arguments.getSocketTimeout(),
                        arguments.isIgnoreSsl(),
                        arguments.getRequestQueueCapacity(),
                        arguments.getQueuePolicy(),
                        arguments.isCloseConnectionAfterFirstByte()
                )
        );
    }

    private static void executeRequests(LogParser logParser, RequestExecutor requestExecutor) throws InterruptedException {
        Logger logger = LogManager.getLogger(Main.class);

        for (LogEntry logEntry : logParser.getLogEntries()) {
            Thread.sleep(logEntry.getDelay());

            try {
                requestExecutor.executeRequest(logEntry);
            } catch (Exception e) {
                logger.error("Error executing request: ", e);
            }
        }
    }
}
