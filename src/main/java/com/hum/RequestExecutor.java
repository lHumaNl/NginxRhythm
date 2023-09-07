package com.hum;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;

public class RequestExecutor {
    private static final Logger logger = LogManager.getLogger(RequestExecutor.class);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm:ss");
    private final Integer scaleLoadWholePart;
    private final Float fractionalPart;
    private final boolean isScaleLoad;
    private final ThreadPoolExecutor requestExecutor;
    private final CloseableHttpClient httpClient;

    public RequestExecutor(Float scaleLoad, int requestThreads, int timeout, boolean ignoreSsl, int queueCapacity, RejectedExecutionHandler queuePolicy) {
        this.requestExecutor = new ThreadPoolExecutor(
                requestThreads,
                requestThreads,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                queuePolicy
        );

        this.httpClient = getHttpClient(timeout, ignoreSsl);

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

    private CloseableHttpClient getHttpClient(int timeout, boolean ignoreSsl) {
        try {
            if (ignoreSsl) {
                SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(new TrustSelfSignedStrategy())
                        .build();

                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(timeout * 1000)
                        .setConnectionRequestTimeout(timeout * 1000)
                        .setSocketTimeout(timeout * 1000)
                        .build();

                return HttpClients.custom()
                        .setSslcontext(sslContext)
                        .setHostnameVerifier(new AllowAllHostnameVerifier())
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            } else {
                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(timeout * 1000)
                        .setConnectionRequestTimeout(timeout * 1000)
                        .setSocketTimeout(timeout * 1000)
                        .build();

                return HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HttpClient", e);
        }
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
                    long timeNowMills = System.currentTimeMillis();
                    LocalDateTime originalTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(logEntry.getRequestTime()), ZoneId.systemDefault());

                    try {
                        TTFBResponseHandler responseHandler = new TTFBResponseHandler();
                        CloseableHttpResponse response = httpClient.execute(logEntry.getHttpRequestBase(), responseHandler);

                        logger.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
                                logEntry.getStatusCode(),
                                response.getStatusLine().getStatusCode(),
                                dateFormat.format(originalTime),
                                timeNow,
                                logEntry.getResponseTime(),
                                responseHandler.getTtfb(),
                                logEntry.getEndpoint()
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                        logger.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
                                logEntry.getStatusCode(),
                                500,
                                dateFormat.format(originalTime),
                                timeNow,
                                logEntry.getResponseTime(),
                                (float) (System.currentTimeMillis() - timeNowMills) / 1000,
                                logEntry.getEndpoint()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public void shutDown() {
        requestExecutor.shutdown();
        try {
            if (!requestExecutor.awaitTermination(600, TimeUnit.SECONDS)) {
                requestExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            requestExecutor.shutdownNow();
        }

        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
