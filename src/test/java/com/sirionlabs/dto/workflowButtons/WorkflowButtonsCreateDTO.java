package com.sirionlabs.dto.workflowButtons;


public class WorkflowButtonsCreateDTO {

    private String testCaseId;
    private String description;
    private String buttonName;
    private String color;
    private String buttonDescription;
    private Boolean active;
    private int entityTypeId;
    private int expectedStatusCode;
    private String expectedErrorMessage;

    public WorkflowButtonsCreateDTO(String testCaseId, String description, String buttonName, String color, String buttonDescription, Boolean active, int entityTypeId,
                                    int expectedStatusCode, String expectedErrorMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setButtonName(buttonName);
        this.setColor(color);
        this.setButtonDescription(buttonDescription);
        this.setActive(active);
        this.setEntityTypeId(entityTypeId);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setExpectedErrorMessage(expectedErrorMessage);
    }

    public String getExpectedErrorMessage() {
        return expectedErrorMessage;
    }

    private void setExpectedErrorMessage(String expectedErrorMessage) {
        this.expectedErrorMessage = expectedErrorMessage;
    }

    public Boolean getActive() {
        return active;
    }

    private void setActive(Boolean active) {
        this.active = active;
    }

    public int getEntityTypeId() {
        return entityTypeId;
    }

    private void setEntityTypeId(int entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    private void setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getTestCaseId() {
        return this.testCaseId;
    }

    private void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getDescription() {
        return this.description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    public String getButtonName() {
        return this.buttonName;
    }

    private void setButtonName(String buttonName) {
        this.buttonName = buttonName;
    }

    public String getColor() {
        return this.color;
    }

    private void setColor(String color) {
        this.color = color;
    }

    public String getButtonDescription() {
        return this.buttonDescription;
    }

    private void setButtonDescription(String buttonDescription) {
        this.buttonDescription = buttonDescription;
    }
}