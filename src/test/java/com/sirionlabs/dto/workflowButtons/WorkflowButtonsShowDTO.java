package com.sirionlabs.dto.workflowButtons;

public class WorkflowButtonsShowDTO {

    private String testCaseId;
    private String description;
    private int buttonId;
    private int expectedStatusCode;
    private String expectedErrorMessage;

    public WorkflowButtonsShowDTO(String testCaseId, String description, int buttonId, int expectedStatusCode, String expectedErrorMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setButtonId(buttonId);
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

    public int getButtonId() {
        return buttonId;
    }

    private void setButtonId(int buttonId) {
        this.buttonId = buttonId;
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