package com.sirionlabs.dto.notificationAlert;


public class NotifAlertExecutionStatusDTO {

    private String testCaseId;
    private String description;
    private int expectedStatusCode;
    private Object entityId;
    private Object ruleId;
    private String responseBody;


    public NotifAlertExecutionStatusDTO(String testCaseId, String description, int expectedStatusCode, Object entityId, Object ruleId, String responseBody) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setEntityId(entityId);
        this.setRuleId(ruleId);
        this.setResponseBody(responseBody);
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

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public Object getEntityId() {
        return entityId;
    }

    public void setEntityId(Object entityId) {
        this.entityId = entityId;
    }

    public Object getRuleId() {
        return ruleId;
    }

    public void setRuleId(Object ruleId) {
        this.ruleId = ruleId;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}