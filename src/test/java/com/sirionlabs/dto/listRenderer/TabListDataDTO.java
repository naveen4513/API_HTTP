package com.sirionlabs.dto.listRenderer;

public class TabListDataDTO {

    private String testCaseId;
    private String description;
    private int tabId;
    private int entityTypeId;
    private int recordId;
    private int clientId;
    private boolean isAdmin;
    private int offset;
    private int size;
    private String orderByColumnName;
    private String orderDirection;
    private String filterJson;
    private int expectedStatusCode;
    private String expectedErrorMessage;

    public TabListDataDTO(String testCaseId, String description, int tabId, int entityTypeId, int recordId, int clientId, boolean isAdmin, int expectedStatusCode,
                          String expectedErrorMessage) {
        this(testCaseId, description, tabId, entityTypeId, recordId, clientId, isAdmin, 0, 20, "id",
                "desc", "{}", expectedStatusCode, expectedErrorMessage);
    }

    public TabListDataDTO(String testCaseId, String description, int tabId, int entityTypeId, int recordId, int clientId, boolean isAdmin, int offset,
                          int size, String orderByColumnName, String orderDirection, String filterJson, int expectedStatusCode, String expectedErrorMessage) {
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setTabId(tabId);
        this.setEntityTypeId(entityTypeId);
        this.setRecordId(recordId);
        this.setClientId(clientId);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setExpectedErrorMessage(expectedErrorMessage);
        this.setAdmin(isAdmin);
        this.setOffset(offset);
        this.setSize(size);
        this.setOrderByColumnName(orderByColumnName);
        this.setOrderDirection(orderDirection);
        this.setFilterJson(filterJson);
    }

    public int getOffset() {
        return offset;
    }

    private void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    private void setSize(int size) {
        this.size = size;
    }

    public String getOrderByColumnName() {
        return orderByColumnName;
    }

    private void setOrderByColumnName(String orderByColumnName) {
        this.orderByColumnName = orderByColumnName;
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    private void setOrderDirection(String orderDirection) {
        this.orderDirection = orderDirection;
    }

    public String getFilterJson() {
        return filterJson;
    }

    private void setFilterJson(String filterJson) {
        this.filterJson = filterJson;
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

    public int getTabId() {
        return tabId;
    }

    private void setTabId(int tabId) {
        this.tabId = tabId;
    }

    public int getEntityTypeId() {
        return entityTypeId;
    }

    private void setEntityTypeId(int entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public int getRecordId() {
        return recordId;
    }

    private void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getClientId() {
        return clientId;
    }

    private void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public boolean getAdmin() {
        return isAdmin;
    }

    private void setAdmin(boolean admin) {
        isAdmin = admin;
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