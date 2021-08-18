package com.sirionlabs.dto.QRCodeGeneration;

public class QrCodeGenerationAPIDto {
    private String testCaseId;
    private String description;
    private String header;
    private String expectedStatusCode;
    private String expectedResponseMessage;
    public QrCodeGenerationAPIDto(String testCaseId, String description, String header, String expectedStatusCode, String expectedResponseMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setHeader(header);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setExpectedResponseMessage(expectedResponseMessage);
    }
    public String getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(String expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getExpectedResponseMessage() {
        return expectedResponseMessage;
    }

    public void setExpectedResponseMessage(String expectedResponseMessage) {
        this.expectedResponseMessage = expectedResponseMessage;
    }




}
