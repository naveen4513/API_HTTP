package com.sirionlabs.dto.workflowRuleTemplate;

public class WorkflowRuleTemplateCreateDTO {

    private String testCaseId;
    private String description;
    private String workflowRuleTemplateName;
    private String entityName;
    private Boolean active;
    private String workflowRuleTemplateJson;
    private int expectedStatusCode;
    private String expectedErrorMessage;

    public WorkflowRuleTemplateCreateDTO(String testCaseId, String description, String workflowRuleTemplateName, String entityName, Boolean active,
                                         String workflowRuleTemplateJson, int expectedStatusCode, String expectedErrorMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setWorkflowRuleTemplateName(workflowRuleTemplateName);
        this.setEntityName(entityName);
        this.setActive(active);
        this.setWorkflowRuleTemplateJson(workflowRuleTemplateJson);
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

    public String getWorkflowRuleTemplateName() {
        return workflowRuleTemplateName;
    }

    private void setWorkflowRuleTemplateName(String workflowRuleTemplateName) {
        this.workflowRuleTemplateName = workflowRuleTemplateName;
    }

    public String getEntityName() {
        return entityName;
    }

    private void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Boolean getActive() {
        return active;
    }

    private void setActive(Boolean active) {
        this.active = active;
    }

    public String getWorkflowRuleTemplateJson() {
        return workflowRuleTemplateJson;
    }

    private void setWorkflowRuleTemplateJson(String workflowRuleTemplateJson) {
        this.workflowRuleTemplateJson = workflowRuleTemplateJson;
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