package com.hum.logparsing.models;


import org.apache.commons.csv.CSVRecord;

import java.util.Arrays;
import java.util.List;

public class LogFormat {
    private final FieldData requestTimeFieldData;
    private final FieldData requestUrlFieldData;
    private final FieldData statusCodeFieldData;
    private final FieldData responseTimeFieldData;
    private final FieldData destinationHostFieldData;
    private final FieldData refererHeaderFieldData;
    private final FieldData userAgentHeaderFieldData;

    public LogFormat(CSVRecord logFormatFields) {
        requestTimeFieldData = new FieldData("$requestTime");
        requestUrlFieldData = new FieldData("$requestUrl");
        statusCodeFieldData = new FieldData("$statusCode");
        responseTimeFieldData = new FieldData("$responseTime");
        destinationHostFieldData = new FieldData("$destinationHost");
        refererHeaderFieldData = new FieldData("$refererHeader");
        userAgentHeaderFieldData = new FieldData("$userAgentHeader");

        fillDataFields(logFormatFields);
    }

    public FieldData getRequestTimeFieldData() {
        return requestTimeFieldData;
    }

    public FieldData getRequestUrlFieldData() {
        return requestUrlFieldData;
    }

    public FieldData getStatusCodeFieldData() {
        return statusCodeFieldData;
    }

    public FieldData getResponseTimeFieldData() {
        return responseTimeFieldData;
    }

    public FieldData getDestinationHostFieldData() {
        return destinationHostFieldData;
    }

    public FieldData getRefererHeaderFieldData() {
        return refererHeaderFieldData;
    }

    public FieldData getUserAgentHeaderFieldData() {
        return userAgentHeaderFieldData;
    }

    private void fillDataFields(CSVRecord logFormatFields) {
        List<FieldData> fieldDataArrayList = Arrays.asList(
                requestTimeFieldData,
                requestUrlFieldData,
                statusCodeFieldData,
                responseTimeFieldData,
                destinationHostFieldData,
                refererHeaderFieldData,
                userAgentHeaderFieldData
        );

        for (int i = 0; i < logFormatFields.size(); i++) {
            for (FieldData fieldData : fieldDataArrayList) {
                if (logFormatFields.get(i).contains(fieldData.getFieldVar())) {
                    fieldData.setFieldFormatAndId(logFormatFields.get(i), i);
                    break;
                }
            }
        }
    }
}
