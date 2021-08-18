package com.sirionlabs.dto.email;


public class LanguagesForEmailActionDTO {

    private String testCaseId;
    private String description;
    private int expectedStatusCode;
    private Object entityTypeId;
    private Object emailId;
    private String responseBody;


    public LanguagesForEmailActionDTO(String testCaseId, String description, int expectedStatusCode, Object entitytypeid, Object emailId, String responseBody) {
        this.setDescription(description);
        this.setEntityTypeId(entitytypeid);
        this.setEmailId(emailId);
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

    public void setEmailId(Object emailId) {
        this.emailId = emailId;
    }

    public Object getEmailId() {
        return emailId;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }



}