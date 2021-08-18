package com.sirionlabs.dto.EmailTokenAuthentication;

public class EmailTokenAuthenticationAPIDto {
    private String testCaseId;
    private String description;
    private String authToken;
    private String expectedStatusCode;
    private String expectedResponseMessage;
    public EmailTokenAuthenticationAPIDto(String testCaseId, String description, String authToken, String expectedStatusCode, String expectedResponseMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setAuthToken(authToken);
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

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
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
