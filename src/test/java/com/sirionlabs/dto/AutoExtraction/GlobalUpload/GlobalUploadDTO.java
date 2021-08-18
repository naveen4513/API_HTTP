package com.sirionlabs.dto.AutoExtraction.GlobalUpload;

import org.json.JSONArray;

public class GlobalUploadDTO {

    private String testCaseId;
    private String description;
    private boolean useJsonParam;
    private String key;
    private String extension;
    private String name;
    private int numberOfFiles;
    private JSONArray projectIds;
    private JSONArray groupIds;
    private JSONArray tagIds;
    private String errors;
    private String isSuccess;
    private String expectedStatusCode;

    public GlobalUploadDTO(String testCaseId,String description,boolean useJsonParam,String key,String extension,String name,int numberOfFiles,JSONArray projectIds,JSONArray groupIds,JSONArray tagIds,String errors,String isSuccess,String expectedStatusCode){
        this.setTestCaseId(testCaseId);
        this.setDescription(description);
        this.setJsonParam(useJsonParam);
        this.setKey(key);
        this.setExtension(extension);
        this.setName(name);
        this.setNumberOfFiles(numberOfFiles);
        this.setProjectIds(projectIds);
        this.setGroupIds(groupIds);
        this.setTagIds(tagIds);
        this.setErrors(errors);
        this.setIfGlobalUploadSuccess(isSuccess);
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

    public boolean getUseJsonParam() {
        return this.useJsonParam;
    }

    private void setJsonParam(boolean useJsonParam) {
        this.useJsonParam = useJsonParam;
    }

    public String getKey() {
        return key;
    }

    private void setKey(String key) { this.key = key; }

    public String getExtension() {
        return extension;
    }

    private void setExtension(String extension) { this.extension = extension; }

    public String getName() {
        return name;
    }

    private void setName(String name) { this.name = name; }

    public int getNumberOfFiles() { return numberOfFiles; }

    private void setNumberOfFiles(int numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }

    public String getExpectedStatusCode() { return expectedStatusCode; }

    private void setExpectedStatusCode(String expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public JSONArray getProjectIds() { return projectIds; }

    private void setProjectIds(JSONArray projectIds) {
        this.projectIds = projectIds;
    }

    public JSONArray getGroupIds() { return groupIds; }

    private void setGroupIds(JSONArray groupIds) {
        this.groupIds = groupIds;
    }

    public JSONArray getTagIds() { return tagIds; }

    private void setTagIds(JSONArray tagIds) {
        this.tagIds = tagIds;
    }

    public String getErrors() {
        return this.errors;
    }

    private void setErrors(String errors) {
        this.errors = errors;
    }

    public String getIsGlobalUploadSuccess() {
        return this.isSuccess;
    }

    private void setIfGlobalUploadSuccess(String isSuccess) {
        this.isSuccess = isSuccess;
    }
}
