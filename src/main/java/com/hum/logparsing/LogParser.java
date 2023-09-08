package com.hum.logparsing;

import com.hum.logparsing.models.FieldData;
import com.hum.logparsing.models.LogEntry;
import com.hum.logparsing.models.LogFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LogParser {
    private static final Logger LOGGER = LogManager.getLogger(LogParser.class);
    private final Path filePath;
    private final String logFormat;
    private final Long startTimestamp;
    private final String destinationHost;
    private final String httpProtocol;
    private final DateTimeFormatter dateFormat;
    private final ExecutorService executor;
    private final List<LogEntry> logEntries;

    public LogParser(Path filePath, String logFormat, DateTimeFormatter formatTime, Long startTimestamp, String destinationHost, String httpProtocol, int parserThreads) throws RuntimeException {
        this.filePath = filePath;
        this.logFormat = logFormat;
        this.startTimestamp = startTimestamp;
        this.destinationHost = destinationHost;
        this.httpProtocol = httpProtocol;
        this.dateFormat = formatTime;
        this.executor = Executors.newFixedThreadPool(parserThreads);
        logEntries = Collections.synchronizedList(new ArrayList<>());

        parseLog();
    }

    public List<LogEntry> getLogEntries() {
        return logEntries;
    }

    private void parseLog() throws RuntimeException {
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader,
                     CSVFormat.DEFAULT.withDelimiter(' ')
                             .withQuote('"')
                             .withNullString("-")
                             .withIgnoreSurroundingSpaces()
             );

             CSVParser formatLogParser = new CSVParser(new CharArrayReader(
                     this.logFormat.toCharArray()),
                     CSVFormat.DEFAULT
                             .withDelimiter(' ')
                             .withQuote('"')
                             .withNullString("-")
                             .withIgnoreSurroundingSpaces()
             )
        ) {

            CSVRecord logFormatFields = formatLogParser.getRecords().get(0);
            LogFormat logFormat = new LogFormat(logFormatFields);

            System.out.println("Start parsing logs");
            fillLogEntries(csvParser, logFormat);
        } catch (IOException e) {
            LOGGER.error("An error occurred when parsing log: ", e);
            throw new RuntimeException();
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                LOGGER.error("Shutdown threads is to long:");
                System.err.println();
                System.exit(1);
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            System.exit(1);
        }

        System.out.println("Sorting");
        logEntries.sort(Comparator.comparing(LogEntry::getRequestTime));

        System.out.println("Set delay");
        fillDelay();

        System.out.println("Parsing logs finished");
        System.out.println("Count of requests to execute: " + logEntries.size());
    }

    private void fillLogEntries(CSVParser csvParser, LogFormat logFormat) {
        for (CSVRecord csvRecord : csvParser) {
            executor.execute(() -> {
                try {
                    String dateString = getValueFromRow(csvRecord, logFormat.getRequestTimeFieldData());
                    if (dateString == null) return;

                    long requestTime = ZonedDateTime.parse(dateString, dateFormat).toInstant().getEpochSecond();
                    if (startTimestamp != null && requestTime < startTimestamp) return;

                    String request = getValueFromRow(csvRecord, logFormat.getRequestUrlFieldData());

                    if (request == null || (request.split(" ").length < 2)) {
                        return;
                    }

                    String[] requestParts = request.split(" ");
                    String method = requestParts[0];
                    String endpoint = requestParts[1];

                    if (method.equals("UNKOWN") || method.equals("UNKNOWN")) {
                        return;
                    }

                    Integer statusCode = getIntegerValue(getValueFromRow(csvRecord, logFormat.getStatusCodeFieldData()));
                    Float responseTime = getFloatValue(getValueFromRow(csvRecord, logFormat.getResponseTimeFieldData()));

                    String destinationHost;
                    if (this.destinationHost != null) destinationHost = this.destinationHost;
                    else destinationHost = getValueFromRow(csvRecord, logFormat.getDestinationHostFieldData());

                    if (!destinationHost.contains("https://") && !destinationHost.contains("http://")) {
                        destinationHost = httpProtocol + "://" + destinationHost;
                    }

                    String refererHeader = getValueFromRow(csvRecord, logFormat.getRefererHeaderFieldData());
                    String userAgentHeader = getValueFromRow(csvRecord, logFormat.getUserAgentHeaderFieldData());

                    LogEntry logEntry = new LogEntry(
                            requestTime,
                            method,
                            endpoint,
                            statusCode,
                            responseTime,
                            destinationHost,
                            refererHeader,
                            userAgentHeader
                    );

                    logEntries.add(logEntry);
                } catch (Exception e) {
                    LOGGER.error("An error occurred: ", e);
                    throw new RuntimeException();
                }
            });
        }
    }

    private void fillDelay() {
        long timeDiff = System.currentTimeMillis() - logEntries.get(0).getRequestTime();
        long prevRespTime = System.currentTimeMillis();

        for (LogEntry logEntry : logEntries) {
            long delay = logEntry.getRequestTime() + timeDiff - prevRespTime;

            logEntry.setDelay(delay);
            prevRespTime = logEntry.getRequestTime() + timeDiff;
        }
    }

    private static String getValueFromRow(CSVRecord csvRecord, FieldData fieldData) {
        if (fieldData.getFieldId() == null) return null;

        String valueFromRow = csvRecord.get(fieldData.getFieldId());

        if (valueFromRow == null) return null;

        if (valueFromRow.equals("-") || valueFromRow.isEmpty() || valueFromRow.equals(" ")) {
            return null;
        }

        return fieldData.substringByFormat(valueFromRow);
    }

    private static Integer getIntegerValue(String stringValue) {
        if (stringValue == null) return null;

        return Integer.parseInt(stringValue);
    }

    private static Float getFloatValue(String stringValue) {
        if (stringValue == null) return null;

        return Float.parseFloat(stringValue);
    }
}