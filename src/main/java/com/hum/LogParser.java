package com.hum;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

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
        Reader reader;
        CSVParser csvParser;
        CSVParser formatLogParser;
        CSVRecord logFormatFields;

        try {
            reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
            csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(' ').withQuote('"').withNullString("-").withIgnoreSurroundingSpaces());
            formatLogParser = new CSVParser(new CharArrayReader(this.logFormat.toCharArray()), CSVFormat.DEFAULT.withDelimiter(' ').withQuote('"').withNullString("-").withIgnoreSurroundingSpaces());
            logFormatFields = formatLogParser.getRecords().get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LogFormat logFormat = new LogFormat(logFormatFields);

        System.out.println("Start parsing logs");
        fillLogEntries(csvParser, logFormat);

        executor.shutdown();
        try {
            if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                System.err.println("Shutdown threads is to long");
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
    }

    private void fillLogEntries(CSVParser csvParser, LogFormat logFormat) {
        for (CSVRecord csvRecord : csvParser) {
            executor.execute(() -> {
                try {
                    String dateString = getValueFromRow(csvRecord, logFormat.getRequestTimeFieldData());
                    if (dateString == null) throw new RuntimeException();

                    long requestTime = ZonedDateTime.parse(dateString, dateFormat).toInstant().getEpochSecond();

                    if (startTimestamp != null) {
                        if (requestTime < startTimestamp) {
                            return;
                        }
                    }

                    String request = getValueFromRow(csvRecord, logFormat.getRequestUrlFieldData());

                    if (request == null || (request.split(" ").length < 2)) {
                        throw new RuntimeException();
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
                    throw new RuntimeException(e);
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