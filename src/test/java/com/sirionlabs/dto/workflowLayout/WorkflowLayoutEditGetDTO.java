package com.sirionlabs.dto.workflowLayout;

public class WorkflowLayoutEditGetDTO {

    private String testCaseId;
    private String description;
    private int workflowLayoutId;
    private int expectedStatusCode;
    private String expectedErrorMessage;

    public WorkflowLayoutEditGetDTO(String testCaseId, String description, int workflowLayoutId, int expectedStatusCode, String expectedErrorMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setWorkflowLayoutId(workflowLayoutId);
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

    public int getWorkflowLayoutId() {
        return workflowLayoutId;
    }

    private void setWorkflowLayoutId(int workflowLayoutId) {
        this.workflowLayoutId = workflowLayoutId;
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