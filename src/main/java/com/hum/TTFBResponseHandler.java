package com.hum;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHttpResponse;

import java.io.IOException;


public class TTFBResponseHandler implements ResponseHandler<CloseableHttpResponse> {

    private final long startTime;
    private long ttfb;

    public TTFBResponseHandler(long startTime) {
        this.startTime = startTime;
    }

    public long getTtfb() {
        return ttfb;
    }

    @Override
    public CloseableHttpResponse handleResponse(HttpResponse httpResponse) {
        this.ttfb = System.currentTimeMillis() - this.startTime;
        return (CloseableHttpResponse) httpResponse;
    }
}