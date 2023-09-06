package com.hum;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;


public class TTFBResponseHandler implements ResponseHandler<CloseableHttpResponse> {

    private final long startTime;
    private float ttfb;

    public TTFBResponseHandler() {
        this.startTime = System.currentTimeMillis();
    }

    public float getTtfb() {
        return ttfb;
    }

    @Override
    public CloseableHttpResponse handleResponse(HttpResponse httpResponse) {
        this.ttfb = (float) (System.currentTimeMillis() - this.startTime) / 1000;
        return (CloseableHttpResponse) httpResponse;
    }
}