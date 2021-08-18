package com.sirionlabs.helper.preSignature;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.presignature.Template;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EnvironmentHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PreSignatureHelper {
    private final static Logger logger = LoggerFactory.getLogger(PreSignatureHelper.class);

    private String editResponse;

    public static Template template = new Template();

    public static List<List<String>> getTemplateStyleListingData(int tableRowsSize, Document wordStyleDocument) {
        List<List<String>> tableRowsData = new LinkedList<>();
        for (int i = 1; i <= tableRowsSize; i++) {
            int tableColumnSize = wordStyleDocument.select("#mainContainer #_title_pl_com_sirionlabs_model_MasterGroup_id tbody tr:nth-of-type(" + i + ") td").size();
            List<String> rowData = new LinkedList<>();
            for (int j = 1; j <= tableColumnSize; j++) {
                rowData.add(wordStyleDocument.select("#mainContainer #_title_pl_com_sirionlabs_model_MasterGroup_id tbody tr:nth-of-type(" + i + ") td:nth-of-type(" + j + ")").text());
            }
            tableRowsData.add(rowData);
        }
        return tableRowsData;
    }

    public static int isTemplateAddedToListing(String templateName) throws IOException {
        int templateId = 0;
        String url = "/wordStyle/list";
        HttpResponse httpResponse = template.verifyTemplateFormattingClientAdmin(url);
        String wordStyleResponseStr = EntityUtils.toString(httpResponse.getEntity());

        Document wordStyleDocument = Jsoup.parse(wordStyleResponseStr);
        int tableRowsSize = wordStyleDocument.select("#mainContainer #_title_pl_com_sirionlabs_model_MasterGroup_id tbody tr").size();
        List<List<String>> listingData = getTemplateStyleListingData(tableRowsSize, wordStyleDocument);

        for (List<String> data : listingData) {
            if (data.contains(templateName)) {
                templateId = Integer.valueOf(data.stream().findFirst().get());
            }
        }
        return templateId;
    }

    public static List<HashMap<String, Object>> getFieldOptions(String jsonStr, String field) {
        List<HashMap<String, Object>> allOptions = new LinkedList<>();
        JSONObject jsonObject = new JSONObject(jsonStr);
        if (jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).has("options")) {
            if (!jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).getJSONObject("options").getBoolean("autoComplete")) {
                int allOptionsData = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).getJSONObject("options").getJSONArray("data").length();
                HashMap<String, Object> option;
                for (int i = 0; i < allOptionsData; i++) {
                    option = new LinkedHashMap<>();

                    option.put("name", jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).getJSONObject("options").getJSONArray("data").getJSONObject(i).get("name").toString());
                    option.put("id", Integer.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).getJSONObject("options").getJSONArray("data").getJSONObject(i).get("id").toString()));
                    allOptions.add(option);
                }
            }
        }
        return allOptions;
    }

    public static JSONObject createJsonForField(String jsonStr, String field, HashMap<String, Object> option, HashMap<String, Object> allfieldsData) {
        JSONObject jsonObject = new JSONObject(jsonStr);
        JSONObject fieldsJsonObject = new JSONObject();
        fieldsJsonObject.put("name", jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).get("name").toString());
        fieldsJsonObject.put("id", Integer.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).get("id").toString()));

        JSONObject selectedValues = new JSONObject("{}");
        fieldsJsonObject.put("values", selectedValues);
        if (option != null) {
            for (Map.Entry<String, Object> entry : option.entrySet()) {
                selectedValues.put(entry.getKey(), entry.getValue());
            }
        } else {
            for (Map.Entry<String, Object> fieldData : allfieldsData.entrySet()) {
                selectedValues.put(fieldData.getKey(), fieldData.getValue());
            }
        }
        return fieldsJsonObject;
    }

    public static JSONObject createJsonForMultiSelectField(String jsonStr, String field, HashMap<String, Object> option, HashMap<String, Object> allfieldsData) {
        JSONObject jsonObject = new JSONObject(jsonStr);
        JSONObject fieldsJsonObject = new JSONObject();
        fieldsJsonObject.put("name", jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).get("name").toString());
        fieldsJsonObject.put("id", Integer.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).get("id").toString()));

        JSONArray jsonArray = new JSONArray();
        JSONObject selectedValues = new JSONObject("{}");
        if (option != null) {
            for (Map.Entry<String, Object> entry : option.entrySet()) {
                selectedValues.put(entry.getKey(), entry.getValue());
            }
        } else {
            for (Map.Entry<String, Object> fieldData : allfieldsData.entrySet()) {
                selectedValues.put(fieldData.getKey(), fieldData.getValue());
            }
        }
        jsonArray.put(selectedValues);
        fieldsJsonObject.put("values", jsonArray);
        return fieldsJsonObject;
    }

    public static int getNewlyCreatedId(String responseString) {
        int newlyCreatedEntityId = 0;
        JSONObject jsonObject = new JSONObject(responseString);
        try {
            newlyCreatedEntityId = Integer.valueOf(jsonObject.getJSONObject("header").getJSONObject("response").get("entityId").toString());
        } catch (Exception ex) {
            logger.info("Error in fetching newly created Entity Id " + ex);
        }
        return newlyCreatedEntityId;
    }

    public static HttpResponse createTag(String payload) {
        String query = "/clauseTag/create";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Tag API. {}", e.getMessage());
        }
        return response;
    }

    public static JSONObject getJsonObjectForResponse(HttpResponse response) {
        JSONObject jsonObject = null;
        try {
            String responseString = EntityUtils.toString(response.getEntity());
            jsonObject = new JSONObject(responseString);
        } catch (Exception e) {
            logger.error("Error in converting http response to json error " + e.getStackTrace());
        }
        return jsonObject;
    }

    public static JSONArray getJsonArrayForResponse(HttpResponse response) {
        JSONArray jsonArray = null;
        try {
            String responseString = EntityUtils.toString(response.getEntity());
            jsonArray = new JSONArray(responseString);
        } catch (Exception e) {
            logger.error("Error in converting http response to json error " + e.getStackTrace());
        }
        return jsonArray;
    }

    public static HttpResponse getClause(int clauseId) {
        String query = "/clause/edit/" + clauseId + "?version=2.0";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse editClause(String payload) {
        String query = "/clause/edit?version=2.0";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getListOfContractDraftRequest() {
        //http://dft.auto.office/listRenderer/list/279/listdata?version=2.0&isFirstCall=true&contractId=&relationId=&vendorId=&_t=1579789442031
        String query = "/listRenderer/list/279/listdata?version=2.0&isFirstCall=true&contractId=&relationId=&vendorId=&_t=1579789442031";
        String payload = "{\"filterMap\":{}}";


        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse verifyWhetherTagIsAlreadyPresent(String tagText) {
        String query = "/options/18?pageType=6&entityTpeId=206&pageEntityTypeId=206&query=" + tagText + "&languageType=1";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static void setFieldsInConfigForEntities(String configFilePath, String configFileName, String entity, String section, int entityIndex) {
        New newObj = new New();

        try {
            newObj.hitNew(entity);
            String newResponse = newObj.getNewJsonStr();

            List<String> allFields = ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, section);

            // Set required fields to config file at runtime
            for (String field : allFields) {
                List<HashMap<String, Object>> allfieldOptions = PreSignatureHelper.getFieldOptions(newResponse, field);
                JSONObject jsonForField;
                if (field.equals("group")) {
                    jsonForField = PreSignatureHelper.createJsonForField(newResponse, field, allfieldOptions.get(entityIndex), null);
                } else if (field.equals("agreementTypes")) {
                    if (section.equals("definition fields") || section.equals("contract template fields")) {
                        ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, section, "agreementTypes", ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "fields", "agreementTypes"));
                        jsonForField = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "definition fields", "agreementTypes"));
                    } else {
                        jsonForField = PreSignatureHelper.createJsonForMultiSelectField(newResponse, field, allfieldOptions.get(RandomNumbers.getRandomNumberWithinRange(0, allfieldOptions.size())), null);
                    }
                } else {
                    if (allfieldOptions.size() >= 1) {
                        if (section.equals("contract template fields") && field.equals("agreementType")) {
                            int index = 0;
                            JSONObject options = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "fields", "agreementTypes"));
                            for (int i = 0; i < allfieldOptions.size(); i++) {
                                if (allfieldOptions.get(i).get("name").toString().equals(options.getJSONArray("values").getJSONObject(0).get("name").toString())) {
                                    index = i;
                                }
                            }
                            jsonForField = PreSignatureHelper.createJsonForField(newResponse, field, allfieldOptions.get(index), null);
                        } else {
                            jsonForField = PreSignatureHelper.createJsonForField(newResponse, field, allfieldOptions.get(RandomNumbers.getRandomNumberWithinRange(0, allfieldOptions.size())), null);
                        }
                    } else {
                        List<String> fieldsData = ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, "fields constant values");
                        HashMap<String, Object> fieldText = new HashMap<>();
                        for (String data : fieldsData) {
                            fieldText.put(data, ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", data));
                        }
                        jsonForField = PreSignatureHelper.createJsonForField(newResponse, field, null, fieldText);
                    }
                }
                ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, section, field, jsonForField.toString());
            }
        } catch (Exception e) {
            logger.error("Error in creating json from new api " + e.getMessage());
        }
    }

    public static void activateEntity(int entityId, SoftAssert softAssert) {
        try {
            // Get Owner Work flow action to perform
            HttpResponse ownerWorkflowResponse = getOwnerWorkFlowStep(entityId);
            softAssert.assertTrue(ownerWorkflowResponse.getStatusLine().getStatusCode() == 200, "Response Code for fetching Owner work flow step is not valid");
            JSONArray ownerWorkflowJson = getJsonArrayForResponse(ownerWorkflowResponse);
            logger.info("Work flow step to be performed " + ownerWorkflowJson.getJSONObject(0).get("workFlowActionDisplayName").toString());
            int taskId = Integer.valueOf(ownerWorkflowJson.getJSONObject(0).get("task_id").toString());
            logger.info("Task Id " + taskId + " For Work flow step to be performed " + ownerWorkflowJson.getJSONObject(0).get("workFlowActionDisplayName").toString());

            // Get Entity on which action has to be performed
            HttpResponse getEntityResponse = getClause(entityId);
            softAssert.assertTrue(getEntityResponse.getStatusLine().getStatusCode() == 200, "Response Code for fetching created entity is not valid");
            JSONObject getEntityJson = getJsonObjectForResponse(getEntityResponse);
            String payloadForWorkflowStepToPerform = "{\"body\":{\"data\":" + getEntityJson.getJSONObject("body").getJSONObject("data").toString() + "}}";

            // Perform Work Flow Action
            HttpResponse performActionResponse = performWorkFlowStep(taskId, payloadForWorkflowStepToPerform);
            softAssert.assertTrue(performActionResponse.getStatusLine().getStatusCode() == 200, "Response Code for performing work flow action is not valid");
            JSONObject performActionJson = getJsonObjectForResponse(performActionResponse);
            softAssert.assertTrue(performActionJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"), "Problem while performing workflow action");
        } catch (Exception e) {
            logger.info("Exception while activating Entity " + e.getMessage());
        }
    }

    public static HttpResponse getOwnerWorkFlowStep(int entityId) {
        String query = "/workflow/action/owner/138/" + entityId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Owner Workflow API Header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching Owner Workflow API Header clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse performWorkFlowStep(int taskId, String payload) {
        String query = "/clause/work-" + taskId + "?version=2.0";

        HttpResponse response = null;
        HttpPost httpPost;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            httpPost = new HttpPost(queryString);

            httpPost.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            httpPost.addHeader("Accept-Encoding", "gzip, deflate");
            httpPost.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(httpPost, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Perform Task API Header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting perform task API. {}", e.getMessage());
        }
        return response;
    }

    public static int getFieldId(String entity, String field) {
        New newObj = new New();
        int fieldId = 0;
        try {
            newObj.hitNew(entity);
            String newResponse = newObj.getNewJsonStr();
            JSONObject jsonObject = new JSONObject(newResponse);
            fieldId = Integer.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(field).get("id").toString());
        } catch (Exception e) {
            logger.error("Error in fetching field Id " + e.getMessage());
        }
        return fieldId;
    }

    public static HttpResponse getContractDraftRequestEditPageResponse(int cdrId) {
        String query = "/cdr/edit/" + cdrId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getCreateContractDraftRequestPage() {
        String query = "/tblcdr/create/rest?version=2.0&_t=1579674621201&_=1579674621135";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getContractDraftRequestResponse(int cdrId) {
        String query = "/cdr/show/" + cdrId + "?version=2.0";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse findMappedContractTemplate(int cdrId) {
        String query = "/tblcdr/findMappedContractTemplate?entityId=" + cdrId + "&entityTypeId=160";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }



    public static HttpResponse getContractTemplateViewerResponse(int contractTemplateId) {

        String query = "tblcontracttemplate/templatePageData/" + contractTemplateId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getContractTemplateResponse(int contractTemplateId) {
        String query = "/contracttemplate/show/" + contractTemplateId + "?version=2.0";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getContractTemplateStructureResponse(int contractTemplateStructureId) {
        String query = "/contracttemplatestructure/show/" + contractTemplateStructureId + "?version=2.0";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse editContractDraftRequest(String payload) {
        String query = "/cdr/edit/";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    //created by Amit on 22 Jan 2020
    public static HttpResponse getContractDraftRequestID(String query, String payload) {


        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse updateTagValue(String payload) {
        String query = "/tagValidation";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse defaultUserListMetaDataAPI(String listId, int entityTypeId, String payload) {
        String query = "/listRenderer/list/" + listId + "/defaultUserListMetaData/?entityTypeId=" + entityTypeId;

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse defaultUserListMetaDataAPI(String listId, String payload) {
        String query = "/listRenderer/list/" + listId + "/defaultUserListMetaData/";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse filterData(String listId, String payload) {
        String query = "/listRenderer/list/" + listId + "/filterData/";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse tabListDataAPI(String listId, String tabListId, int entityId, String payload) {
        String query = "/listRenderer/list/" + listId + "/tablistdata/" + tabListId + "/" + entityId;

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse tabListDataAPIForClause(String listId, String payload, String view) {
        String query;
        HttpResponse response = null;
        HttpPost postRequest;
        if (view.equals("List")) {
            query = "/listRenderer/list/" + listId + "/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        } else {
            query = "/listRenderer/list/" + listId + "/listdata" + "?contractId=&relationId=&vendorId=&am=true";
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
        }

        try {
            logger.debug("Query string url formed is {}", query);

            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Edit API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while editing clause API. {}", e.getMessage());
        }
        return response;
    }

    public static List<Integer> getDefaultColumns(JSONArray jsonArray) {

        List<Integer> columnIds = new LinkedList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            columnIds.add(Integer.valueOf(jsonArray.getJSONObject(i).get("id").toString()));
        }
        return columnIds;
    }

    public static HttpResponse getFieldHistory(int fieldId, int entityTypeId) {
        String query = "/tblauditlogs/fieldHistory/" + fieldId + "/" + entityTypeId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static Boolean getTemplateFromContractDocumentTabCDR(String outputFilePath, String outputFileName, String id, String entityTypeId, String entityType, String fileId) {
        Boolean fileDownloaded = false;
        String query = "/download/communicationdocument?id=" + id + "&entityTypeId=" + entityTypeId + "&entityType.id=" + entityType + "&fileId=" + fileId;
        String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3";
        APIUtils apiUtils = new APIUtils();
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"), Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));
            HttpGet httpGet = apiUtils.generateHttpGetRequestWithQueryString(query, acceptHeader);
            fileDownloaded = apiUtils.downloadAPIResponseFile(outputFilePath, outputFileName, target, httpGet);
        } catch (Exception e) {
            logger.error("Exception while hitting Document Download From Contract Document tab CDR API. {}", e.getMessage());
        }
        return fileDownloaded;
    }

    public static String fileUploadDraft(String documentName, String documentExtension, String key, String entityTypeId, String entityId, String documentId, File file) {
        APIUtils apiUtils = new APIUtils();
        String uploadResponse = null;
        String query = "/file/upload/draft";

        Map<String, String> formDataMap = new LinkedHashMap<>();
        formDataMap.put("name", documentName);
        formDataMap.put("extension", documentExtension);
        formDataMap.put("key", key);
        formDataMap.put("entityTypeId", entityTypeId);
        formDataMap.put("entityId", entityId);
        formDataMap.put("DocumentId", documentId);

        Map<String, File> fileToUpload = new LinkedHashMap<>();
        fileToUpload.put("documentFileData", file);

        HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"), Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);

            postRequest = apiUtils.generateHttpPostRequestWithQueryString(queryString, "application/json, text/plain, */*", "", "gzip, deflate");
            HttpEntity entity = APIUtils.createMultipartEntityBuilder("documentFileData", file.getAbsoluteFile(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document", formDataMap);
            postRequest.setEntity(entity);
            uploadResponse = apiUtils.uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting File Upload Draft API. {}", e.getMessage());
        }
        return uploadResponse;
    }

    public static String fileUploadDraftWithNewDocument(String documentName, String documentExtension, String key, String entityTypeId, String entityId, File file) {
        APIUtils apiUtils = new APIUtils();
        String uploadResponse = null;
        String query = "/file/upload/draft";

        Map<String, String> formDataMap = new LinkedHashMap<>();
        formDataMap.put("name", documentName);
        formDataMap.put("extension", documentExtension);
        formDataMap.put("key", key);
        formDataMap.put("entityTypeId", entityTypeId);
        formDataMap.put("entityId", entityId);

        Map<String, File> fileToUpload = new LinkedHashMap<>();
        fileToUpload.put("documentFileData", file);

        HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"), Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);

            postRequest = apiUtils.generateHttpPostRequestWithQueryString(queryString, "application/json, text/plain, */*", "", "gzip, deflate");
            HttpEntity entity = APIUtils.createMultipartEntityBuilder("documentFileData", file.getAbsoluteFile(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document", formDataMap);
            postRequest.setEntity(entity);
            uploadResponse = apiUtils.uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting File Upload Draft API. {}", e.getMessage());
        }
        return uploadResponse;
    }

    public static HttpResponse submitFileDraft(String payload) {
        String query = "/cdr/submitdraft?version=2.0";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Submit Draft API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Submit Draft API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse moveToTree(String payload) {
        String query = "/moveToTree/save";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Move To Tree API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Move To Tree API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse verifyDocumentOnTree(int contractId, String payload) {
        String query = "/contract-tree/v1/61/" + contractId + "?hierarchy=true&offset=0";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Document On Tree API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Document On Tree API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse showCreatedContract(int contractId) {
        String query = "/contracts/show/" + contractId + "?version=2.0";

        HttpResponse response = null;
        HttpGet httpGet;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            httpGet = new HttpGet(queryString);

            httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.getRequest(httpGet);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Document On Tree API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Document On Tree API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse showCreatedClause(int clauseId) {
        String query = "/clause/show/" + clauseId;

        HttpResponse response = null;
        HttpGet httpGet;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            httpGet = new HttpGet(queryString);

            httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.getRequest(httpGet);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause Show API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Clause Show API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse newAPIContractFromCDR(int supplierId, int sourceEntityTypeId, int entityId) {
        String query = "/contracts/new/4/1/" + supplierId + "/1/" + supplierId + "?sourceEntityTypeId=" + sourceEntityTypeId + "&sourceEntityId=" + entityId + "&version=2.0";

        HttpResponse response = null;
        HttpGet httpGet;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            httpGet = new HttpGet(queryString);

            httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.getRequest(httpGet);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Document On Tree API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Document On Tree API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse apiContractFromCDR(String query,String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Submit Draft API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Submit Draft API. {}", e.getMessage());
        }

        return response;
    }

    public static List<Integer> getAllTaskIds() {
        List<Integer> allTaskIds = new ArrayList<>();
        try {
            logger.info("Hitting Fetch API");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

        } catch (Exception e) {
            logger.error("Exception while getting all task ids from user task api response. error : {}", e.getMessage());
            e.printStackTrace();
        }
        return allTaskIds;
    }

    public static Integer getNewTaskId(List<Integer> allTaskIds) {
        Integer newTaskId = -1;
        try {
            logger.info("Hitting Fetch API to get new Task Id");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();

            newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
        } catch (Exception e) {
            logger.error("Exception while getting new Task id. error : {}", e.getMessage());
            e.printStackTrace();
        }
        return newTaskId;
    }

    public static HttpResponse getMasterUserRoleGroup(String masterUserRoleGroupId) {
        String query = "/masteruserrolegroups/update/" + masterUserRoleGroupId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "text/html, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getListDataForEntities(String entityId, String payload) {
        String query = "/listRenderer/list/" + entityId + "/listdata?version=2.0&isFirstCall=true";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Submit Draft API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Submit Draft API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getDocumentMovementStatus() {
        String query = "/documentMovementStatus/list";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Document Movement Status List API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Document Movement Status List API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getDocumentMovementStatusByDocumentId(String statusId) {
        String query = "/documentMovementStatus/show/" + statusId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Document Movement Status List API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Document Movement Status List API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse updateDocumentMovementStatus(HashMap<String, String> formData) {
        String query = "/documentMovementStatus/update";

        APIUtils apiUtils = new APIUtils();
        HttpResponse response = null;
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpHost httpHost = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"), Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "text/html, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            postRequest.addHeader("Authorization", Check.getAuthorization());
            postRequest.addHeader("X-Requested-With", "XMLHttpRequest");

            HttpEntity nameValuePairFormDataEntity = apiUtils.generateNameValuePairFormDataEntity(formData);
            postRequest.setEntity(nameValuePairFormDataEntity);
            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Update Document Movement Status API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Update Document Movement Status API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getReportsAccessForClient(int clientId) {
        String query = "/reportRenderer/getreportsjson?clientId=" + clientId;
        HttpHost httpHost = new HttpHost("sirion.auto.office", -1, "http");
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Authorization", Check.getAuthorization());
            postRequest.addHeader("X-Requested-With", "XMLHttpRequest");

            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get Reports For Client API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get Reports For Client API. {}", e.getMessage());
        }
        return response;
    }

    public static List<Integer> getAllReportIds(JSONObject jsonObject) {
        List<Integer> reportIds = new LinkedList<>();
        List<String> allEntities = jsonObject.keySet().stream().collect(Collectors.toList());
        for (int i = 0; i < allEntities.size(); i++) {
            int reportsJsonLength = jsonObject.getJSONArray(allEntities.get(i)).length();
            for (int j = 0; j < reportsJsonLength; j++) {
                if (jsonObject.getJSONArray(allEntities.get(i)).getJSONObject(j).get("selected").toString().trim().equals("true")) {
                    reportIds.add(Integer.valueOf(jsonObject.getJSONArray(allEntities.get(i)).getJSONObject(j).get("id").toString()));
                }
            }
        }
        return reportIds;
    }

    public static HttpResponse reportConfigure(int clientId, String payload) {
        String query = "/reportRenderer/clientReportConfigUpdate?clientId=" + clientId;
        HttpHost httpHost = new HttpHost("sirion.auto.office", -1, "http");
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Authorization", Check.getAuthorization());

            if (payload != null) {
                postRequest.setEntity(new StringEntity(payload));
            }

            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Configure Report API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Configure Report API. {}", e.getMessage());
        }
        return response;
    }

    public static List<String> createPayloadForReports(List<Integer> reportIds) {
        List<String> payload = new LinkedList<>();
        for (Integer reportId : reportIds) {
            payload.add("{\"id\":\"" + reportId + "\"}");
        }
        return payload;
    }

    public static String createPayloadForReportsAfterRemovingReportId(List<String> reportIds, int id) {
        for (int i = 0; i < reportIds.size(); i++) {
            if (reportIds.get(i).contains(String.valueOf(id))) {
                reportIds.remove(i);
            }
        }
        return reportIds.toString();
    }

    public static List<String> getAllReportsNameForEntity(String entity, String listReportJsonStr) {
        List<String> allCDRReports = new LinkedList<>();
        JSONArray listReportJson = new JSONArray(listReportJsonStr);
        int listReportLength = listReportJson.length();
        for (int i = 0; i < listReportLength; i++) {
            if (listReportJson.getJSONObject(i).get("name").toString().trim().equals(entity)) {
                int cdrReportsLength = listReportJson.getJSONObject(i).getJSONArray("listMetaDataJsons").length();
                for (int j = 0; j < cdrReportsLength; j++) {
                    allCDRReports.add(listReportJson.getJSONObject(i).getJSONArray("listMetaDataJsons").getJSONObject(j).get("name").toString().trim());
                }
                break;
            }
        }
        return allCDRReports;
    }

    public static HttpResponse updateMasterUserRoleGroup(String params) {
        HttpResponse response = null;
        HttpClient httpClient;
        httpClient = HttpClientBuilder.create().build();
        APIUtils apiUtils = new APIUtils();

        HttpHost httpHost = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("host"), Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("port")), ConfigureEnvironment.getEnvironmentProperty("scheme"));
        try {
            String queryString = "/masteruserrolegroups/update";
            logger.debug("Query string url formed is {}", queryString);

            HttpPost httpPostRequest = apiUtils.generateHttpPostRequestWithQueryStringAndPayload(queryString, "text/html, */*; q=0.01", "application/x-www-form-urlencoded; charset=UTF-8", params);
            response = httpClient.execute(httpHost, httpPostRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Update Master User Role Group API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Update Master User Role Group API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitDownload(String outputFile, int entityTypeId, String cdrId) {
        HttpResponse response = null;
        APIUtils apiUtils = new APIUtils();
        try {
            HttpHost httpHost = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("host"), Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("port")), ConfigureEnvironment.getEnvironmentProperty("scheme"));
            String queryString = "/reportRenderer/activityReport/download/428/" + entityTypeId + "/" + cdrId;
            logger.info("Query string url formed is {}", queryString);
            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";

            HttpGet getRequest = apiUtils.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
            response = apiUtils.downloadAPIResponseFile(outputFile, httpHost, getRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting Activity Report Download Api. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitClauseDownload(String outputFile, int clauseId) {
        HttpResponse response = null;
        APIUtils apiUtils = new APIUtils();
        try {
            HttpHost httpHost = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("host"), Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("port")), ConfigureEnvironment.getEnvironmentProperty("scheme"));
            String queryString = "/clause/download/" + clauseId;
            logger.info("Query string url formed is {}", queryString);
            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";

            HttpGet getRequest = apiUtils.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
            response = apiUtils.downloadAPIResponseFile(outputFile, httpHost, getRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting Activity Report Download Api. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse cdrComment(String payload) {
        String query = "/cdr/comment?version=2.0";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("CDR Comment API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting CDR Comment API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getContractTemplateStructure(int agreementTypeId) {
        String query = "/contracttemplatestructure/getContractTemplateStructure/" + agreementTypeId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get Contract Template Structure API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get Contract Template Structure API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitDefinitionDownload(String outputFile, int definitionIDasCreated) {
        HttpResponse response = null;
        APIUtils apiUtils = new APIUtils();
        try {
            HttpHost httpHost = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("host"),Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("port")),ConfigureEnvironment.getEnvironmentProperty("scheme"));
            String queryString = "/clause/download/" + definitionIDasCreated;
            logger.info("Query string url formed is {}", queryString);
            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";

            HttpGet getRequest = apiUtils.generateHttpGetRequestWithQueryString(queryString,acceptHeader);
            response = apiUtils.downloadAPIResponseFile(outputFile, httpHost, getRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting Activity Report Download Api. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitContractTemplateDownload(String outputFile, int contractTemplateIDasCreated) {
        HttpResponse response = null;
        APIUtils apiUtils = new APIUtils();
        try {
            HttpHost httpHost = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("host"),Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("port")),ConfigureEnvironment.getEnvironmentProperty("scheme"));
            String queryString = "/contracttemplate/download/" + contractTemplateIDasCreated;
            logger.info("Query string url formed is {}", queryString);
            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";

            HttpGet getRequest = apiUtils.generateHttpGetRequestWithQueryString(queryString,acceptHeader);
            response = apiUtils.downloadAPIResponseFile(outputFile, httpHost, getRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting Activity Report Download Api. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getContractTemplateStructureClauseData(int contractTemplateStructureId) {
        String query = "/contracttemplatestructure/getData/" + contractTemplateStructureId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get Contract Template Structure Clause Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get Contract Template Structure Clause Data API. {}", e.getMessage());
        }
        return response;
    }

    public static List<JSONObject> getAllMandatoryClauses(JSONObject getAllClauseMandatoryForContractTemplateStructure) {
        List<JSONObject> allMandateCategories = new LinkedList<>();
        int clauseCategoriesLength = getAllClauseMandatoryForContractTemplateStructure.getJSONObject("body").getJSONArray("data").length();
        for(int i=0;i<clauseCategoriesLength;i++){
            if(getAllClauseMandatoryForContractTemplateStructure.getJSONObject("body").getJSONArray("data").getJSONObject(i).get("mandatory").toString().equals("true")){
                allMandateCategories.add(getAllClauseMandatoryForContractTemplateStructure.getJSONObject("body").getJSONArray("data").getJSONObject(i));
            }
        }
        return allMandateCategories;
    }

    public String getContractDraftRequestEditPageResponseString(int cdrId) {
        String query = "/cdr/edit/" + cdrId;

        HttpResponse response = null;
        HttpGet getRequest;
        String responseString = null;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            responseString = EntityUtils.toString(response.getEntity());
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created clause API. {}", e.getMessage());
        }

        return responseString;
    }

    public String submitFileDraftWithResponse(String payload) {
        String query = "/cdr/submitdraft?version=2.0";

        HttpResponse response = null;
        HttpPost postRequest;
        String submitDraftResponse = null;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);

            submitDraftResponse = EntityUtils.toString(response.getEntity());

            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Submit Draft API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Submit Draft API. {}", e.getMessage());
        }
        return submitDraftResponse;
    }
}