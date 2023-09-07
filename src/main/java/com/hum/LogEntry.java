package com.hum;

import org.apache.http.client.methods.*;

import java.net.URI;
import java.net.URISyntaxException;

public class LogEntry {
    private final long requestTime;
    private final String endpoint;
    private final HttpRequestBase httpRequestBase;
    private final Integer statusCode;
    private final Float responseTime;
    private long delay;

    public LogEntry(
            long requestTime,
            String method,
            String endpoint,
            Integer statusCode,
            Float responseTime,
            String destinationHost,
            String refererHeader,
            String userAgentHeader) throws RuntimeException {
        this.requestTime = requestTime * 1000;
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

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    private HttpRequestBase createHttpRequest(String method, String destinationHost) {
        URI uri = getUri(destinationHost + endpoint);

        if (uri == null) {
            throw new RuntimeException();
        }

        switch (method) {
            case "GET":
                return new HttpGet(uri);
            case "POST":
                return new HttpPost(uri);
            case "HEAD":
                return new HttpHead(uri);
            case "PUT":
                return new HttpPut(uri);
            case "OPTIONS":
                return new HttpOptions(uri);
            case "PATCH":
                return new HttpPatch(uri);
            case "DELETE":
                return new HttpDelete(uri);
            default:
                throw new UnsupportedOperationException("HTTP method not supported: " + method);
        }
    }

    public static URI getUri(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            url = url.replace("<", "%3C")
                    .replace(">", "%3E")
                    .replace("\"", "%22")
                    .replace("#", "%23")
                    .replace("{", "%7B")
                    .replace("}", "%7D")
                    .replace("|", "%7C")
                    .replace("\\", "%5C")
                    .replace("^", "%5E")
                    .replace("~", "%7E")
                    .replace("[", "%5B")
                    .replace("]", "%5D")
                    .replace("`", "%60");
            try {
                uri = new URI(url);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
        return uri;
    }
}