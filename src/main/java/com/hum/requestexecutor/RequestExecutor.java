package com.hum.requestexecutor;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.*;

import com.hum.logparsing.models.LogEntry;
import com.hum.requestexecutor.models.RequestExecutorSetup;
import com.hum.requestexecutor.models.TTFBResponseHandler;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RequestExecutor {
    private static final Logger LOGGER = LogManager.getLogger(RequestExecutor.class);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm:ss");
    private final Integer scaleLoadWholePart;
    private final Float fractionalPart;
    private final boolean isScaleLoad;
    private final boolean closeConnectionAfterFirstByte;
    private final ThreadPoolExecutor requestExecutor;
    private final CloseableHttpClient httpClient;

    public RequestExecutor(RequestExecutorSetup requestExecutorSetup) {
        this.scaleLoadWholePart = requestExecutorSetup.getScaleLoadWholePart();
        this.fractionalPart = requestExecutorSetup.getFractionalPart();
        this.isScaleLoad = requestExecutorSetup.isScaleLoad();
        this.closeConnectionAfterFirstByte = requestExecutorSetup.isCloseConnectionAfterFirstByte();
        this.requestExecutor = requestExecutorSetup.getRequestExecutor();
        this.httpClient = requestExecutorSetup.getHttpClient();
    }

    public void executeRequest(LogEntry logEntry) throws RuntimeException {
        if (isScaleLoad) {
            for (int i = 0; i < scaleLoadWholePart; i++) sendRequest(logEntry);

            if (Math.random() < fractionalPart) sendRequest(logEntry);
        } else {
            sendRequest(logEntry);
        }
    }

    private void sendRequest(LogEntry logEntry) throws RuntimeException {
        requestExecutor.execute(() -> {
                    String timeNow = dateFormat.format(LocalDateTime.now());
                    LocalDateTime originalTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(logEntry.getRequestTime()), ZoneId.systemDefault());
                    long timeNowMills = System.currentTimeMillis();

                    TTFBResponseHandler responseHandler = new TTFBResponseHandler(closeConnectionAfterFirstByte);
                    try (CloseableHttpResponse ignored = httpClient.execute(logEntry.getHttpRequestBase(), responseHandler)) {
                        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
                                logEntry.getStatusCode(),
                                responseHandler.getStatusCode(),
                                dateFormat.format(originalTime),
                                timeNow,
                                logEntry.getResponseTime(),
                                responseHandler.getTtfb(),
                                logEntry.getEndpoint()
                        );
                    } catch (Exception e) {
                        String logMessage = String.format(
                                Locale.US,
                                "%d\t%d\t%s\t%s\t%f\t%f\t%s",
                                logEntry.getStatusCode(),
                                0,
                                dateFormat.format(originalTime),
                                timeNow,
                                logEntry.getResponseTime(),
                                (float) (System.currentTimeMillis() - timeNowMills) / 1000,
                                logEntry.getEndpoint()
                        );

                        LOGGER.error(
                                String.format(
                                        "Error executing request:%s%s%s",
                                        System.lineSeparator(),
                                        logMessage,
                                        System.lineSeparator()
                                ),
                                e
                        );

                        LOGGER.info(logMessage);
                        throw new RuntimeException();
                    }
                }
        );
    }

    public void shutDown() {
        try {
            requestExecutor.shutdown();
            try {
                if (!requestExecutor.awaitTermination(600, TimeUnit.SECONDS)) {
                    requestExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                requestExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
