package com.sirionlabs.dto.WorkFlowDownload;

public class WorkFlowDownloadDto {
    private String testCaseId;
    private String description;
    private String shortCodeId;


    private String expectedStatusCode;
    private String expectedResponseMessage;
    private String status;
    public WorkFlowDownloadDto(String testCaseId, String description, String shortCodeId, String expectedStatusCode, String expectedResponseMessage,String status) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setShortCodeId(shortCodeId);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setExpectedResponseMessage(expectedResponseMessage);
        this.setStatus(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getShortCodeId() {
        return shortCodeId;
    }

    public void setShortCodeId(String shortCodeId) {
        this.shortCodeId = shortCodeId;
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
