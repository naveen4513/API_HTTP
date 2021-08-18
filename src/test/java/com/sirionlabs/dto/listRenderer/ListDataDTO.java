package com.sirionlabs.dto.listRenderer;

public class ListDataDTO {

    private String testCaseId;
    private String description;
    private String listId;
    private String contractId;
    private String relationId;
    private String vendorId;
    private String version;
    private String offset;
    private String size;
    private String entityTypeId;
    private String orderByColumnName;
    private String orderDirection;
    private String isFirstCall;
    private String filterJson;
    private String selectedColumns;
    private String expectedStatusCode;


    public ListDataDTO(String testCaseId, String description, String listId, String entityTypeId, String expectedStatusCode) {
        this(testCaseId, description, listId, null, null, null, null, null, null, entityTypeId,
                null, null, null, null, null, expectedStatusCode);
    }

    public ListDataDTO(String testCaseId, String description, String listId, String contractId, String relationId, String vendorId, String version, String offset,
                       String size, String entityTypeId, String orderByColumnName, String orderDirection, String isFirstCall, String filterJson,
                       String selectedColumns, String expectedStatusCode) {

        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setListId(listId);
        this.setContractId(contractId);
        this.setRelationId(relationId);
        this.setVendorId(vendorId);
        this.setVersion(version);
        this.setOffset(offset);
        this.setSize(size);
        this.setEntityTypeId(entityTypeId);
        this.setOrderByColumnName(orderByColumnName);
        this.setOrderDirection(orderDirection);
        this.setIsFirstCall(isFirstCall);
        this.setFilterJson(filterJson);
        this.setSelectedColumns(selectedColumns);
        this.setExpectedStatusCode(expectedStatusCode);
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

    public String getOffset() {
        return this.offset;
    }

    private void setOffset(String offset) {
        //Default Value is 0.
        this.offset = (offset == null || offset.trim().equalsIgnoreCase("")) ? "0" : offset;
    }

    public String getSize() {
        return this.size;
    }

    private void setSize(String size) {
        //Default Value is 20.
        this.size = (size == null || size.trim().equalsIgnoreCase("")) ? "20" : size;
    }

    public String getListId() {
        return this.listId;
    }

    private void setListId(String listId) {
        this.listId = listId;
    }

    public String getContractId() {
        return this.contractId;
    }

    private void setContractId(String contractId) {
        this.contractId = (contractId == null || contractId.trim().equalsIgnoreCase("")) ? "" : contractId;
    }

    public String getRelationId() {
        return this.relationId;
    }

    private void setRelationId(String relationId) {
        this.relationId = (relationId == null || relationId.trim().equalsIgnoreCase("")) ? "" : relationId;
    }

    public String getVendorId() {
        return this.vendorId;
    }

    private void setVendorId(String vendorId) {
        this.vendorId = (vendorId == null || vendorId.trim().equalsIgnoreCase("")) ? "" : vendorId;
    }

    public String getVersion() {
        return this.version;
    }

    private void setVersion(String version) {
        //Default Version is 2.0
        this.version = (version == null || version.trim().equalsIgnoreCase("")) ? "2.0" : version;
    }

    public String getEntityTypeId() {
        return this.entityTypeId;
    }

    private void setEntityTypeId(String entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public String getOrderByColumnName() {
        return this.orderByColumnName;
    }

    private void setOrderByColumnName(String orderByColumnName) {
        //Default Order by Column is ID.
        this.orderByColumnName = (orderByColumnName == null || orderByColumnName.trim().equalsIgnoreCase("")) ? "id" : orderByColumnName;
    }

    public String getOrderDirection() {
        return this.orderDirection;
    }

    private void setOrderDirection(String orderDirection) {
        //Default Direction is desc nulls last
        this.orderDirection = (orderDirection == null || orderDirection.trim().equalsIgnoreCase("")) ? "desc nulls last" : orderDirection;
    }

    public String getIsFirstCall() {
        return this.isFirstCall;
    }

    private void setIsFirstCall(String isFirstCall) {
        //Default Value of isFirstCall is false
        this.isFirstCall = (isFirstCall == null || isFirstCall.trim().equalsIgnoreCase("")) ? "false" : isFirstCall;
    }

    public String getFilterJson() {
        return this.filterJson;
    }

    private void setFilterJson(String filterJson) {
        //Default Value of FilterJson is empty JSON i.e. {}
        this.filterJson = (filterJson == null || filterJson.trim().equalsIgnoreCase("")) ? "{}" : filterJson;
    }

    public String getSelectedColumns() {
        return this.selectedColumns;
    }

    private void setSelectedColumns(String selectedColumns) {
        //Default Value of SelectedColumns is empty JSON Array i.e. []
        this.selectedColumns = (selectedColumns == null || selectedColumns.trim().equalsIgnoreCase("")) ? "[]" : selectedColumns;
    }

    public String getExpectedStatusCode() {
        return expectedStatusCode;
    }

    private void setExpectedStatusCode(String expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

}