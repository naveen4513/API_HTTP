package com.sirionlabs.api.autoExtraction;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.json.JSONObject;
import org.testng.SkipException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
public class ProjectCreationAPI extends TestAPIBase {
    public static String getAPIPath()
    {
        return "/metadataautoextraction/create";
    }
    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public static String getPayload() {
        String projectName = "TestDeDuplicationService" + RandomString.getRandomAlphaNumericString(5);
        String metadataFields = ProjectCreationAPI.getAllMetadataFields();
        return "{\"name\":\""+projectName+"\",\"description\":\"Automation\",\"projectLinkedFieldIds\":["+metadataFields+"],\"clientId\":1002}";
    }
    public static APIResponse projectCreateAPIResponse(String apiPath, HashMap<String, String> headers, String payload) {
        return executor.post(apiPath, headers, payload).getResponse();
    }
    public static String getAllMetadataFields()
    {
        APIResponse fetchFieldsApiResponse = GetAllFieldsAPI.fetchAllMetadataFields(GetAllFieldsAPI.getAPIPath(), GetAllFieldsAPI.getHeaders());
        String fetchFieldIdApiResponseStr = fetchFieldsApiResponse.getResponseBody();
        JSONObject fetchFieldIdResponseObj=new JSONObject(fetchFieldIdApiResponseStr);
        int metadataFieldsLength = fetchFieldIdResponseObj.getJSONArray("response").length();
        List<String> metadataFieldsInProject = new LinkedList<>();
        for(int i=0;i<metadataFieldsLength;i++){
            metadataFieldsInProject.add(fetchFieldIdResponseObj.getJSONArray("response").getJSONObject(i).get("id").toString());
        }
        if(metadataFieldsInProject.size()<1){
            throw new SkipException("No Meta Data Fields are there to select in project");
        }
        String metadataFields = metadataFieldsInProject.stream().map(Object::toString).collect(Collectors.joining(","));
        return metadataFields;
    }

    public static String getPayloadWithCategories()
    {
        String projectName = "TestDeDuplicationService" + RandomString.getRandomAlphaNumericString(5);
        APIResponse fetchFieldsApiResponse = GetAllFieldsAPI.fetchAllMetadataFields(GetAllFieldsAPI.getAPIPath(), GetAllFieldsAPI.getHeaders());
        String fetchFieldIdApiResponseStr = fetchFieldsApiResponse.getResponseBody();
        JSONObject fetchFieldIdResponseObj=new JSONObject(fetchFieldIdApiResponseStr);
        int metadataFieldsLength = fetchFieldIdResponseObj.getJSONArray("response").length();
        List<String> metadataFieldsInProject = new LinkedList<>();
        for(int i=0;i<metadataFieldsLength;i++){
            metadataFieldsInProject.add(fetchFieldIdResponseObj.getJSONArray("response").getJSONObject(i).get("id").toString());
        }
        if(metadataFieldsInProject.size()<1){
            throw new SkipException("No Meta Data Fields are there to select in project");
        }
        String metadataFields = metadataFieldsInProject.stream().map(Object::toString).collect(Collectors.joining(","));

        return "{\"name\":\""+projectName+"\",\"description\":\"Automation\",\"projectLinkedFieldIds\":["+metadataFields+"]," +
                "\"projectLinkedCategories\":[{\"id\":1046,\"name\":\"General\",\"baseClauseText\":\"General Clause Test Deviation Analysis\"}],\"clientId\":1002}";

    }
}