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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LogParser {
    private final Path filePath;
    private final String logFormat;
    private final long startTimestamp;
    private final String destinationHost;
    private final SimpleDateFormat dateFormat;
    private final List<LogEntry> logEntries;

    public LogParser(Path filePath, String logFormat, SimpleDateFormat formatTime, long startTimestamp, String destinationHost) {
        this.filePath = filePath;
        this.logFormat = logFormat;
        this.startTimestamp = startTimestamp;
        this.destinationHost = destinationHost;
        this.dateFormat = formatTime;
        logEntries = Collections.synchronizedList(new ArrayList<>());

        parseLog();
    }

    public List<LogEntry> getLogEntries() {
        return logEntries;
    }

    private void parseLog() {
        Reader reader;
        CSVParser csvParser;
        CSVParser formatLogParser;
        CSVRecord logFormatFields;

        try {
            reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
            csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(' ').withQuote('"').withNullString("-").withIgnoreSurroundingSpaces());
            formatLogParser = new CSVParser(new CharArrayReader(logFormat.toCharArray()), CSVFormat.DEFAULT.withDelimiter(' ').withQuote('"').withNullString("-").withIgnoreSurroundingSpaces());
            logFormatFields = formatLogParser.getRecords().get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Integer requestTimeFieldId = null;
        Integer requestFieldId = null;
        Integer statusCodeFieldId = null;
        Integer responseTimeFieldId = null;
        Integer requestHostFieldId = null;
        Integer refererHeaderFieldId = null;
        Integer userAgentHeaderFieldId = null;

        for (int i = 0; i < logFormatFields.size(); i++) {
            if (logFormatFields.get(i).contains("$requestTime")) requestTimeFieldId = i;
            else if (logFormatFields.get(i).contains("$request")) requestFieldId = i;
            else if (logFormatFields.get(i).contains("$statusCode")) statusCodeFieldId = i;
            else if (logFormatFields.get(i).contains("$responseTime")) responseTimeFieldId = i;
            else if (logFormatFields.get(i).contains("$requestHost")) requestHostFieldId = i;
            else if (logFormatFields.get(i).contains("$refererHeader")) refererHeaderFieldId = i;
            else if (logFormatFields.get(i).contains("$userAgentHeader")) userAgentHeaderFieldId = i;
        }

        fillLogEntries(
                csvParser,
                requestTimeFieldId,
                requestFieldId,
                statusCodeFieldId,
                responseTimeFieldId,
                requestHostFieldId,
                refererHeaderFieldId,
                userAgentHeaderFieldId
        );

        logEntries.sort(Comparator.comparing(LogEntry::getRequestTime));
    }

    private void fillLogEntries(CSVParser csvParser,
                                Integer requestTimeFieldId,
                                Integer requestFieldId,
                                Integer statusCodeFieldId,
                                Integer responseTimeFieldId,
                                Integer requestHostFieldId,
                                Integer refererHeaderFieldId,
                                Integer userAgentHeaderFieldId) {

        int availableProcessors = Math.max(Runtime.getRuntime().availableProcessors(), 8);
        ExecutorService executor = Executors.newFixedThreadPool(availableProcessors);

        for (CSVRecord csvRecord : csvParser) {
            executor.execute(() -> {
                try {
                    long requestTime = dateFormat.parse(csvRecord.get(requestTimeFieldId)).getTime();

                    if (requestTime < startTimestamp) {
                        return;
                    }

                    String request = csvRecord.get(requestFieldId);
                    String[] requestParts = request.split(" ");
                    String method = requestParts[0];
                    String endpoint = requestParts[1];

                    Integer statusCode = getIntegerValue(getValueFromRow(csvRecord, statusCodeFieldId));
                    Float responseTime = getFloatValue(getValueFromRow(csvRecord, responseTimeFieldId));

                    String destinationHost;
                    if (this.destinationHost != null) destinationHost = this.destinationHost;
                    else destinationHost = getValueFromRow(csvRecord, requestHostFieldId);

                    String refererHeader = getValueFromRow(csvRecord, refererHeaderFieldId);
                    String userAgentHeader = getValueFromRow(csvRecord, userAgentHeaderFieldId);

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
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                System.err.println("Shutdown threads is to long");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getValueFromRow(CSVRecord csvRecord, Integer fieldId) {
        if (fieldId == null) return null;

        String valueFromRow = csvRecord.get(fieldId);

        if (valueFromRow.equals("-") || valueFromRow.isEmpty() || valueFromRow.isBlank()) {
            return null;
        }

        return valueFromRow;
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