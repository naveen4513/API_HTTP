package com.sirionlabs.dto.BulkEditAttendance;

public class BulkEditAttendanceDTO {
    private String testCaseId;
    private String description;
    private String expectedStatusCode;
    private String expectedResponseMessage;
    private String payload;

    public BulkEditAttendanceDTO(String testCaseId, String description, String payload,String expectedStatusCode, String expectedResponseMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setPayload(payload);
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
