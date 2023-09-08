package com.hum.requestexecutor.models;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;


public class TTFBResponseHandler implements ResponseHandler<CloseableHttpResponse> {
    private final boolean closeConnectionAfterFirstByte;
    private final long startTime;
    private float ttfb;
    private int statusCode;

    public TTFBResponseHandler(boolean closeConnectionAfterFirstByte) {
        this.closeConnectionAfterFirstByte = closeConnectionAfterFirstByte;
        this.startTime = System.currentTimeMillis();
    }

    public float getTtfb() {
        return ttfb;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public CloseableHttpResponse handleResponse(HttpResponse httpResponse) {
        this.statusCode = httpResponse.getStatusLine().getStatusCode();
        this.ttfb = (float) (System.currentTimeMillis() - this.startTime) / 1000;

        if (closeConnectionAfterFirstByte) return null;
        else return (CloseableHttpResponse) httpResponse;
    }
}