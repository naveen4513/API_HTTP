package com.sirionlabs.dto.bulkIntegration;


public class BulkUpdateUserDTO {

    private String testCaseId;
    private String description;
    private String payload;
    private String expectedStatusCode;
    private String expectedResponseMessage;

    public BulkUpdateUserDTO(String testCaseId, String description, String payload, String expectedStatusCode, String expectedResponseMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setPayload(payload);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setExpectedResponseMessage(expectedResponseMessage);
    }

    public String getTestCaseId() {
        return this.testCaseId;
    }

    private void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getDescription() {
        return this.description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    public String getPayload() {
        return this.payload;
    }

    private void setPayload(String payload) {
        this.payload = payload;
    }

    public String getExpectedStatusCode() {
        return expectedStatusCode;
    }

    private void setExpectedStatusCode(String expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getExpectedResponseMessage() {
        return this.expectedResponseMessage;
    }

    private void setExpectedResponseMessage(String expectedResponseMessage) {
        this.expectedResponseMessage = expectedResponseMessage;
    }
}