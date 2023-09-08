package com.hum.logparsing.models;

public class FieldData {
    private final String fieldVar;
    private String fieldFormat;
    private Integer fieldId;
    private int startIndex;

    public FieldData(String fieldVar) {
        this.fieldVar = fieldVar;
    }

    public String getFieldVar() {
        return fieldVar;
    }

    public Integer getFieldId() {
        return fieldId;
    }

    public void setFieldFormatAndId(String fieldFormat, Integer fieldId) {
        this.fieldFormat = fieldFormat;
        this.fieldId = fieldId;
        this.startIndex = this.fieldFormat.indexOf(this.fieldVar);

        if (startIndex == -1) {
            throw new IllegalArgumentException("Format string does not contain " + fieldVar);
        }
    }

    public String substringByFormat(String value) {
        int endIndex = value.length() - (fieldFormat.length() - (startIndex + fieldVar.length()));

        return value.substring(startIndex, endIndex);
    }
}
