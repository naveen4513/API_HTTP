package com.sirionlabs.dto.inbound;


public class WFActionDTO {

    private String testCaseId;
    private String description;
    private int expectedStatusCode;
    private Object entityId;
    private Object entityTypeId;
    private String responseBody;


    public WFActionDTO(String testCaseId, String description, int expectedStatusCode, Object entityTypeId, Object entityId, String responseBody) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setEntityTypeId(entityTypeId);
        this.setEntityId(entityId);
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

    public Object getEntityTypeId() {
        return entityTypeId;
    }

    public void setEntityTypeId(Object entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}