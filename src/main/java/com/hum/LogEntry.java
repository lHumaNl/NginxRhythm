package com.hum;

import org.apache.http.client.methods.*;

public class LogEntry {

    private final long requestTime;
    private final String endpoint;
    private final HttpRequestBase httpRequestBase;
    private final Integer statusCode;
    private final Float responseTime;

    public LogEntry(
            long requestTime,
            String method,
            String endpoint,
            Integer statusCode,
            Float responseTime,
            String destinationHost,
            String refererHeader,
            String userAgentHeader) {
        this.requestTime = requestTime;
        this.endpoint = endpoint;
        this.httpRequestBase = createHttpRequest(method, destinationHost);
        this.statusCode = statusCode;
        this.responseTime = responseTime;

        if (refererHeader != null) this.httpRequestBase.setHeader("Referer", refererHeader);
        if (userAgentHeader != null) this.httpRequestBase.setHeader("User-Agent", userAgentHeader);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public HttpRequestBase getHttpRequestBase() {
        return httpRequestBase;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Float getResponseTime() {
        return responseTime;
    }

    private HttpRequestBase createHttpRequest(String method, String destinationHost) {
        switch (method) {
            case "GET":
                return new HttpGet(destinationHost + endpoint);
            case "POST":
                return new HttpPost(destinationHost + endpoint);
            case "HEAD":
                return new HttpHead(destinationHost + endpoint);
            case "PUT":
                return new HttpPut(destinationHost + endpoint);
            case "OPTIONS":
                return new HttpOptions(destinationHost + endpoint);
            case "PATCH":
                return new HttpPatch(destinationHost + endpoint);
            case "DELETE":
                return new HttpDelete(destinationHost + endpoint);
            default:
                throw new UnsupportedOperationException("HTTP method not supported: " + method);
        }
    }
}