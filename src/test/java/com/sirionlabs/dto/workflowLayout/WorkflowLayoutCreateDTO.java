package com.sirionlabs.dto.workflowLayout;

public class WorkflowLayoutCreateDTO {

    private String testCaseId;
    private String description;
    private String workflowLayoutGroupName;
    private String editableFieldsShowPage;
    private String editableFieldsEditPage;
    private String editPageTabs;
    private String showPageTabs;
    private int entityTypeId;
    private int expectedStatusCode;
    private String expectedErrorMessage;

    public WorkflowLayoutCreateDTO(String testCaseId, String description, String workflowLayoutGroupName, String editableFieldsShowPage, String editableFieldsEditPage,
                                   String editPageTabs, String showPageTabs, int entityTypeId, int expectedStatusCode, String expectedErrorMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setWorkflowLayoutGroupName(workflowLayoutGroupName);
        this.setEditableFieldsShowPage(editableFieldsShowPage);
        this.setEditableFieldsEditPage(editableFieldsEditPage);
        this.setEditPageTabs(editPageTabs);
        this.setShowPageTabs(showPageTabs);
        this.setEntityTypeId(entityTypeId);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setExpectedErrorMessage(expectedErrorMessage);
    }

    public int getEntityTypeId() {
        return entityTypeId;
    }

    private void setEntityTypeId(int entityTypeId) {
        this.entityTypeId = entityTypeId;
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

    public String getWorkflowLayoutGroupName() {
        return workflowLayoutGroupName;
    }

    private void setWorkflowLayoutGroupName(String workflowLayoutGroupName) {
        this.workflowLayoutGroupName = workflowLayoutGroupName;
    }

    public String getEditableFieldsShowPage() {
        return editableFieldsShowPage;
    }

    private void setEditableFieldsShowPage(String editableFieldsShowPage) {
        this.editableFieldsShowPage = editableFieldsShowPage;
    }

    public String getEditableFieldsEditPage() {
        return editableFieldsEditPage;
    }

    private void setEditableFieldsEditPage(String editableFieldsEditPage) {
        this.editableFieldsEditPage = editableFieldsEditPage;
    }

    public String getEditPageTabs() {
        return editPageTabs;
    }

    private void setEditPageTabs(String editPageTabs) {
        this.editPageTabs = editPageTabs;
    }

    public String getShowPageTabs() {
        return showPageTabs;
    }

    private void setShowPageTabs(String showPageTabs) {
        this.showPageTabs = showPageTabs;
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