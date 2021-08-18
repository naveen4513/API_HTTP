package com.sirionlabs.dto.email;


public class EmailDataDTO {

    private String testCaseId;
    private String description;
    private int expectedStatusCode;
    private Object entityTypeId;
    private Object emailAction;
    private Object languageId;
    private String subjectData;
    private String bodyData;
    private String isNonWorkflowEmail;
    private String workFlowActionAdded;


    public EmailDataDTO(String testCaseId, String description, int expectedStatusCode, Object entitytypeid, Object emailAction, Object languageId, String subjectData, String bodyData, String isNonWorkflowEmail, String workFlowActionAdded) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setEntityTypeId(entitytypeid);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setEmailAction(emailAction);
        this.setLanguageId(languageId);
        this.setSubjectData(subjectData);
        this.setBodyData(bodyData);
        this.setIsNonWorkflowEmail(isNonWorkflowEmail);
        this.setWorkFlowActionAdded(workFlowActionAdded);
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

    public Object getEmailAction() {
        return emailAction;
    }

    public void setEmailAction(Object emailAction) {
        this.emailAction = emailAction;
    }

    public Object getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Object languageId) {
        this.languageId = languageId;
    }

    public String getSubjectData() {
        return subjectData;
    }

    public void setSubjectData(String subjectData) {
        this.subjectData = subjectData;
    }

    public String getBodyData() {
        return bodyData;
    }

    public void setBodyData(String bodyData) {
        this.bodyData = bodyData;
    }

    public String getIsNonWorkflowEmail() {
        return isNonWorkflowEmail;
    }

    public void setIsNonWorkflowEmail(String isNonWorkflowEmail) {
        this.isNonWorkflowEmail = isNonWorkflowEmail;
    }

    public String getWorkFlowActionAdded() {
        return workFlowActionAdded;
    }

    public void setWorkFlowActionAdded(String workFlowActionAdded) {
        this.workFlowActionAdded = workFlowActionAdded;
    }
}