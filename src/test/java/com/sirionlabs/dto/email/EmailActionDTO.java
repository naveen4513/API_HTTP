package com.sirionlabs.dto.email;


public class EmailActionDTO {

    private String testCaseId;
    private String description;
    private int expectedStatusCode;
    private Object entityTypeId;
    private String responseBody;


    public EmailActionDTO(String testCaseId, String description, int expectedStatusCode, Object entitytypeid, String responseBody) {
        this.setDescription(description);
        this.setEntityTypeId(entitytypeid);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setTestCaseId(testCaseId);
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