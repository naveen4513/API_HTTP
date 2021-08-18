package com.sirionlabs.dto.workflowRoleGroupFlowDown;

public class WorkflowRoleGroupFlowDownCreateDTO {

    private String testCaseId;
    private String description;
    private String parentEntityTypeId;
    private String childEntityTypeId;
    private String roleGroupId;
    private String clientId;
    private String deleted;
    private int expectedStatusCode;
    private String expectedErrorMessage;

    public WorkflowRoleGroupFlowDownCreateDTO(String testCaseId, String description, String parentEntityTypeId, String childEntityTypeId, String roleGroupId, String clientId,
                                              String deleted, int expectedStatusCode, String expectedErrorMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setParentEntityTypeId(parentEntityTypeId);
        this.setChildEntityTypeId(childEntityTypeId);
        this.setRoleGroupId(roleGroupId);
        this.setClientId(clientId);
        this.setDeleted(deleted);
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

    public String getParentEntityTypeId() {
        return parentEntityTypeId;
    }

    private void setParentEntityTypeId(String parentEntityTypeId) {
        this.parentEntityTypeId = parentEntityTypeId;
    }

    public String getChildEntityTypeId() {
        return childEntityTypeId;
    }

    private void setChildEntityTypeId(String childEntityTypeId) {
        this.childEntityTypeId = childEntityTypeId;
    }

    public String getRoleGroupId() {
        return roleGroupId;
    }

    private void setRoleGroupId(String roleGroupId) {
        this.roleGroupId = roleGroupId;
    }

    public String getClientId() {
        return clientId;
    }

    private void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDeleted() {
        return deleted;
    }

    private void setDeleted(String deleted) {
        this.deleted = deleted;
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