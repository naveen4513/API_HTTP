package com.sirionlabs.dto.CheckEligibleForAdhocMeeting;

public class CheckEligibleForAdhocMeetingAPIDto {
    private String testCaseId;
    private String description;
    private String gbStatus;
    private String expectedStatusCode;
    private String expectedResponseMessage;
    public CheckEligibleForAdhocMeetingAPIDto(String testCaseId, String description, String gbStatus, String expectedStatusCode, String expectedResponseMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setGbStatus(gbStatus);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setExpectedResponseMessage(expectedResponseMessage);
    }
    public void setGbStatus(String gbStatus) {
        this.gbStatus = gbStatus;
    }
    public String getGbStatus() {
        return gbStatus;
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

}
