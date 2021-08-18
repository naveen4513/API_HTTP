package com.sirionlabs.dto.AutoExtraction.ContractDocumentGetEntities;

public class ContractDocumentGetEntitiesDTO {

    private String testCaseId;
    private String description;
    private boolean validAuthorization;
    private String acceptHeader;
    private boolean validAcceptHeader;
    private int expectedStatusCode;

    public ContractDocumentGetEntitiesDTO(String testCaseId,String description,boolean validAuthorization,String acceptHeader,boolean validAcceptHeader,int expectedStatusCode){
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setAuthorization(validAuthorization);
        this.setAcceptHeader(acceptHeader);
        this.setValidAcceptHeader(validAcceptHeader);
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

    public boolean getAuthorization(){
        return  this.validAuthorization;
    }

    public void setAuthorization(boolean validAuthorization){
        this.validAuthorization = validAuthorization;
    }

    public String getAcceptHeader() {
        return this.acceptHeader;
    }

    public void setAcceptHeader(String acceptHeader){
        this.acceptHeader = acceptHeader;
    }

    public boolean getValidAcceptHeader(){
        return  this.validAcceptHeader;
    }

    public void setValidAcceptHeader(boolean validAcceptHeader){
        this.validAcceptHeader = validAcceptHeader;
    }

    public int getExpectedStatusCode() {
        return this.expectedStatusCode;
    }

    public void setExpectedStatusCode(int expectedStatusCode){
        this.expectedStatusCode = expectedStatusCode;
    }
}
