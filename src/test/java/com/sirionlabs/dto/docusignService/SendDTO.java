package com.sirionlabs.dto.docusignService;


public class SendDTO {

    private String testCaseId;
    private String description;
    private String serviceType;
    private int expectedStatusCode;
    private String path;
    private Object entityId;
    private Object entityTypeId;
    private Object documentName;
    private Object documentId;
    private Object type;
    private Object docOrigin;
    private Object name;
    private Object userId;
    private String responseBody;


    public SendDTO(String testCaseId, String description, String serviceType, int expectedStatusCode, String path, Object entityid, Object entitytypeid, Object documentname, Object documentid, Object type, Object docOrigin, Object name, Object userId, String responseBody) {
        this.setDescription(description);
        this.setDocumentId(documentid);
        this.setDocumentName(documentname);
        this.setEntityId(entityid);
        this.setEntityTypeId(entitytypeid);
        this.setExpectedStatusCode(expectedStatusCode);
        this.setName(name);
        this.setPath(path);
        this.setServiceType(serviceType);
        this.setTestCaseId(testCaseId);
        this.setType(type);
        this.setDocOrigin(docOrigin);
        this.setUserId(userId);
        this.setResponseBody(responseBody);

    }


    public String getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getEntityId() {
        return entityId;
    }

    public void setEntityId(Object entityId) {
        this.entityId = entityId;
    }

    public Object getEntityTypeId() {
        return entityTypeId;
    }

    public void setEntityTypeId(Object entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public Object getDocumentName() {
        return documentName;
    }

    public void setDocumentName(Object documentName) {
        this.documentName = documentName;
    }

    public Object getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Object documentId) {
        this.documentId = documentId;
    }

    public Object getType() {
        return type;
    }

    public void setType(Object type) {
        this.type = type;
    }

    public Object getDocOrigin() {
        return docOrigin;
    }

    public void setDocOrigin(Object docOrigin) {
        this.docOrigin = docOrigin;
    }

    public Object getName() {
        return name;
    }

    public void setName(Object name) {
        this.name = name;
    }

    public Object getUserId() {
        return userId;
    }

    public void setUserId(Object userId) {
        this.userId = userId;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }



}