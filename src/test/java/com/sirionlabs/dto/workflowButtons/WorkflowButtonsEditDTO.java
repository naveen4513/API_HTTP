package com.sirionlabs.dto.workflowButtons;

public class WorkflowButtonsEditDTO extends WorkflowButtonsCreateDTO {

    private int buttonId;

    public WorkflowButtonsEditDTO(String testCaseId, String description, int buttonId, String buttonName, String color, String buttonDescription, Boolean active,
                                  int entityTypeId, int expectedStatusCode, String expectedErrorMessage) {
        super(testCaseId, description, buttonName, color, buttonDescription, active, entityTypeId, expectedStatusCode, expectedErrorMessage);

        this.setButtonId(buttonId);
    }

    public int getButtonId() {
        return buttonId;
    }

    private void setButtonId(int buttonId) {
        this.buttonId = buttonId;
    }
}