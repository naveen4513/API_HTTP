package com.sirionlabs.dto.workflowRuleTemplate;

public class WorkflowRuleTemplateEditGetDTO {

    private String testCaseId;
    private String description;
    private int workflowRuleTemplateId;
    private int expectedStatusCode;
    private String expectedErrorMessage;

    public WorkflowRuleTemplateEditGetDTO(String testCaseId, String description, int workflowRuleTemplateId, int expectedStatusCode, String expectedErrorMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setWorkflowRuleTemplateId(workflowRuleTemplateId);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setExpectedErrorMessage(expectedErrorMessage);
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    private void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getDescription() {
        return description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    public int getWorkflowRuleTemplateId() {
        return workflowRuleTemplateId;
    }

    private void setWorkflowRuleTemplateId(int workflowRuleTemplateId) {
        this.workflowRuleTemplateId = workflowRuleTemplateId;
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    private void setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getExpectedErrorMessage() {
        return expectedErrorMessage;
    }

    private void setExpectedErrorMessage(String expectedErrorMessage) {
        this.expectedErrorMessage = expectedErrorMessage;
    }
}