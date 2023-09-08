package com.hum.requestexecutor;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.*;

import com.hum.logparsing.models.LogEntry;
import com.hum.requestexecutor.models.TTFBResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RequestExecutor {
    private static final Logger LOGGER = LogManager.getLogger(RequestExecutor.class);
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm:ss");
    private Integer scaleLoadWholePart;
    private Float fractionalPart;
    private boolean isScaleLoad;
    private final boolean closeConnectionAfterFirstByte;
    private final ThreadPoolExecutor requestExecutor;
    private final CloseableHttpClient httpClient;
    private static final float CONNECTION_POOL_MULTIPLIER = 1.5f;

    public RequestExecutor(Float scaleLoad, int requestThreads, int timeout, boolean ignoreSsl, int queueCapacity, RejectedExecutionHandler queuePolicy, boolean closeConnectionAfterFirstByte) {
        this.requestExecutor = initRequestExecutor(requestThreads, queueCapacity, queuePolicy);
        this.httpClient = initHttpClient(requestThreads, timeout, ignoreSsl);
        this.closeConnectionAfterFirstByte = closeConnectionAfterFirstByte;

        initScaleLoad(scaleLoad);
    }

    private ThreadPoolExecutor initRequestExecutor(int requestThreads, int queueCapacity, RejectedExecutionHandler queuePolicy) {
        return new ThreadPoolExecutor(
                requestThreads,
                requestThreads,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                queuePolicy
        );
    }

    private CloseableHttpClient initHttpClient(int requestThreads, int timeout, boolean ignoreSsl) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal((int) (requestThreads * CONNECTION_POOL_MULTIPLIER));
        connectionManager.setDefaultMaxPerRoute((int) (requestThreads * CONNECTION_POOL_MULTIPLIER));

        return getHttpClient(timeout, ignoreSsl, connectionManager);
    }

    private void initScaleLoad(Float scaleLoad) {
        Optional<Float> optScaleLoad = Optional.ofNullable(scaleLoad);
        this.isScaleLoad = optScaleLoad.isPresent();

        this.scaleLoadWholePart = optScaleLoad.map(Float::intValue).orElse(null);
        this.fractionalPart = optScaleLoad.map(sl -> sl - this.scaleLoadWholePart).orElse(null);
    }

    private CloseableHttpClient getHttpClient(int timeout, boolean ignoreSsl, PoolingHttpClientConnectionManager connectionManager) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(timeout * 1000)
                    .setConnectionRequestTimeout(timeout * 1000)
                    .setSocketTimeout(timeout * 1000)
                    .build();

            HttpClientBuilder httpClientBuilder = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig);

            if (ignoreSsl) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                TrustManager[] trustAllCertificates = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                sslContext.init(null, trustAllCertificates, new SecureRandom());
                httpClientBuilder.setSslcontext(sslContext);
                httpClientBuilder.setHostnameVerifier(new AllowAllHostnameVerifier());
            }

            return httpClientBuilder.build();

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
                        LOGGER.error("Error executing request: ", e);
                        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
                                logEntry.getStatusCode(),
                                0,
                                dateFormat.format(originalTime),
                                timeNow,
                                logEntry.getResponseTime(),
                                (float) (System.currentTimeMillis() - timeNowMills) / 1000,
                                logEntry.getEndpoint()
                        );
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
