package com.hum;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class RequestExecutor {

    private static final Logger logger = LogManager.getLogger();
    private final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy:HH:mm:ss");

    private final ScheduledExecutorService scheduler;
    private final Float ratio;
    private final Integer scaleLoadWholePart;
    private final Float fractionalPart;
    private final boolean isScaleLoad;
    private final boolean isSpeedChanged;

    public RequestExecutor(Float ratio, Float scaleLoad) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.ratio = ratio;

        this.isSpeedChanged = ratio != null;

        if (scaleLoad != null) {
            this.scaleLoadWholePart = scaleLoad.intValue();
            this.fractionalPart = scaleLoad - this.scaleLoadWholePart;
            this.isScaleLoad = true;
        } else {
            this.scaleLoadWholePart = null;
            this.fractionalPart = null;
            this.isScaleLoad = false;
        }
    }

    public void executeRequest(LogEntry logEntry) {
        long delay = System.currentTimeMillis() - logEntry.getRequestTime();

        if (isSpeedChanged) delay *= ratio;

        scheduler.schedule(() -> {
            if (isScaleLoad) {
                for (int i = 0; i < scaleLoadWholePart; i++) sendRequest(logEntry);

                if (Math.random() < fractionalPart) sendRequest(logEntry);
            } else {
                sendRequest(logEntry);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void sendRequest(LogEntry logEntry) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            long startTime = System.currentTimeMillis();
            TTFBResponseHandler responseHandler = new TTFBResponseHandler(startTime);
            CloseableHttpResponse response = httpClient.execute(logEntry.getHttpRequestBase(), responseHandler);

            logger.info("{}\t{}\t{}\t{}\t{}\t{}\t{}", logEntry.getStatusCode(), response.getStatusLine().getStatusCode(), dateFormat.format(new Date(logEntry.getRequestTime())), dateFormat.format(new Date()), logEntry.getResponseTime(), responseHandler.getTtfb(), logEntry.getEndpoint());

            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
