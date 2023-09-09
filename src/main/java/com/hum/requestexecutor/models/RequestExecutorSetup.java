package com.hum.requestexecutor.models;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RequestExecutorSetup {
    private Integer scaleLoadWholePart;
    private Float fractionalPart;
    private boolean isScaleLoad;
    private final boolean closeConnectionAfterFirstByte;
    private final ThreadPoolExecutor requestExecutor;
    private final CloseableHttpClient httpClient;

    public RequestExecutorSetup(Float scaleLoad, int requestThreads, int connectTimeout, int socketTimeout, boolean ignoreSsl, int queueCapacity, RejectedExecutionHandler queuePolicy, boolean closeConnectionAfterFirstByte) {
        this.requestExecutor = initRequestExecutor(requestThreads, queueCapacity, queuePolicy);
        this.httpClient = initHttpClient(requestThreads, connectTimeout, socketTimeout, ignoreSsl);
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

    private CloseableHttpClient initHttpClient(int requestThreads, int connectTimeout, int socketTimeout, boolean ignoreSsl) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(requestThreads + 1);
        connectionManager.setDefaultMaxPerRoute(requestThreads + 1);

        return getHttpClient(connectTimeout, socketTimeout, ignoreSsl, connectionManager);
    }

    private void initScaleLoad(Float scaleLoad) {
        Optional<Float> optScaleLoad = Optional.ofNullable(scaleLoad);
        this.isScaleLoad = optScaleLoad.isPresent();

        this.scaleLoadWholePart = optScaleLoad.map(Float::intValue).orElse(null);
        this.fractionalPart = optScaleLoad.map(sl -> sl - this.scaleLoadWholePart).orElse(null);
    }

    private CloseableHttpClient getHttpClient(int connectTimeout, int socketTimeout, boolean ignoreSsl, PoolingHttpClientConnectionManager connectionManager) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(connectTimeout * 1000)
                    .setSocketTimeout(socketTimeout * 1000)
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

    public Integer getScaleLoadWholePart() {
        return scaleLoadWholePart;
    }

    public Float getFractionalPart() {
        return fractionalPart;
    }

    public boolean isScaleLoad() {
        return isScaleLoad;
    }

    public boolean isCloseConnectionAfterFirstByte() {
        return closeConnectionAfterFirstByte;
    }

    public ThreadPoolExecutor getRequestExecutor() {
        return requestExecutor;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }
}
