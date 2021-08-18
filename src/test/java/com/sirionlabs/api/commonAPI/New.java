package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class New extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(New.class);
    private Map<String, String> allRequiredFields = new HashMap<>();
    private String newJsonStr = null;
    private String fieldsJsonStr = null;
    private boolean fieldsJsonSet = false;

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Content-Type", "application/json;charset=UTF-8");

        return headers;
    }

    public static List<Map<String, String>> getAllSupplierTypeUsersFromNewResponse(String newResponse) {
        List<Map<String, String>> allSupplierTypeUsers = new ArrayList<>();

        try {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                JSONObject jsonObj = new JSONObject(newResponse);
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("options");
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    int idType = jsonArr.getJSONObject(i).getInt("idType");

                    if (idType == 4) {
                        Map<String, String> userMap = new HashMap<>();

                        String userName = jsonArr.getJSONObject(i).getString("name");
                        int userId = jsonArr.getJSONObject(i).getInt("id");

                        userMap.put("name", userName);
                        userMap.put("id", String.valueOf(userId));

                        allSupplierTypeUsers.add(userMap);
                    }
                }

                return allSupplierTypeUsers;
            } else {
                logger.error("New Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Supplier Type Users from New Response. {}", e.getMessage());
        }

        return null;
    }

    //Post New
    public void hitNew(String entityName, String payload) {
        HttpResponse response;

        try {
            HttpPost postRequest;

            String urlName = ConfigureConstantFields.getUrlNameForEntity(entityName);

            String queryString = "/" + urlName + "/new?version=2.0";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("New response header {}", oneHeader.toString());
            }

        } catch (Exception e) {
            logger.error("Exception while hitting New Api. {}", e.getMessage());
        }
    }

    //Post New for Multi Supplier Parent
    public void hitNewForMultiSupplierParent(String parentEntityName, String childEntityName, int parentRecordId, String payload) {
        HttpResponse response;

        try {
            HttpPost postRequest;

            int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);
            String createLinksResponse = CreateLinks.getCreateLinksV2Response(parentEntityTypeId, parentRecordId);

            int childEntityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
            String queryString = CreateLinks.getCreateLinkForEntity(createLinksResponse, childEntityTypeId);
            logger.debug("Query string url formed is {}", queryString);

            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.postRequest(postRequest, payload);
            this.newJsonStr = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            logger.error("Exception while hitting New Api. {}", e.getMessage());
        }
    }

    public void hitNew(String entityName, String sourceEntityName, int sourceEntityId) {
        hitNew(entityName, sourceEntityName, sourceEntityId, null, null);
    }

    public void hitNew(String entityName, String sourceEntityName, int sourceEntityId, String type) {
        hitNew(entityName, sourceEntityName, sourceEntityId, type, null);
    }

    public void hitNew(String entityName) {
        hitNew(entityName, "", -1, null, null);
    }

    //Get New
    public void hitNew(String entityName, String sourceEntityName, int sourceEntityId, String type, String lineItemTypeId) {
        hitNew(entityName, sourceEntityName, sourceEntityId, type, lineItemTypeId, false, -1);
    }

    public void hitNew(String entityName, String sourceEntityName, int sourceEntityId, String type, String lineItemTypeId, boolean isMultiSupplier, int multiParentSupplierId) {
        HttpResponse response;

        try {
            HttpGet getRequest;
            String queryString = null;
            int sourceEntityTypeId;
            boolean getQueryString = true;

            if (sourceEntityName != null) {
                String showResponse = null;

                sourceEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntityName);

                if (!entityName.equalsIgnoreCase("contract templates")) {
                    showResponse = ShowHelper.getShowResponse(sourceEntityTypeId, sourceEntityId);
                }

                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

                switch (entityName.trim()) {
                    case "contracts":
                        entityTypeId = ConfigureConstantFields.getEntityIdByName(type);
                        break;

                    case "contract draft request":
                        getQueryString = sourceEntityName.equalsIgnoreCase("contracts");

                        if (!getQueryString) {
                            queryString = "/cdr/new?version=2.0";
                        }

                        break;

                    case "clauses":
                    case "definition":
                    case "contract template structure":
                        queryString = "/" + ConfigureConstantFields.getUrlNameForEntity(entityName.trim()) + "/new";
                        getQueryString = false;
                        break;

                    case "contract templates":
                        queryString = "/contracttemplate/new/2/" + new AdminHelper().getClientId();
                        getQueryString = false;
                        break;

                    case "vendors":
                        queryString = "/vendors/new/" + sourceEntityId;
                        getQueryString = false;
                        break;

                    case "purchase orders":
                        if (sourceEntityName.trim().equalsIgnoreCase("suppliers")) {
                            queryString = "/pos/new/1/" + sourceEntityId;
                            getQueryString = false;
                        }
                        break;

                    default:
                        break;
                }

                if (getQueryString) {
                    queryString = ShowHelper.getCreateLinkForEntity(showResponse, entityTypeId, lineItemTypeId);
                }
            } else {
                return;
            }

            logger.info("Query string url formed is {}", queryString);

            if (queryString == null) {
                return;
            }

            if (isMultiSupplier) {
                queryString = queryString.concat("?multiParentSupplierId=" + multiParentSupplierId + "&version=2.0");
            }

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("New response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api. {}", e.getMessage());
        }
    }

    //For Contract where Source Entity & Parent Source Entity is different.
    public void hitNewForSubContract(int sourceEntityId, String type, int sourceParentEntityId) {
        HttpResponse response;

        try {
            HttpGet getRequest;

            String entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
            String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            String urlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingFilePath, entityIdMappingFileName, "contracts", "url_name");

            String queryString = "/" + urlName + "/new";
            int sourceEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
            int typeId = ConfigureConstantFields.getEntityIdByName(type);
            queryString += "/" + typeId + "/" + sourceEntityTypeId + "/" + sourceParentEntityId + "/" + sourceEntityTypeId + "/" + sourceEntityId + "?version=2.0";
            logger.info("queryString is : {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("New response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api. {}", e.getMessage());
        }
    }

    //For Credit EarnBack
    public void hitNewForCreditEarnBack(int contractId) {
        HttpResponse response;

        try {
            HttpGet getRequest;
            String queryString = "/creditearnbacks/new/" + contractId;
            logger.info("queryString is : {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("New response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api. {}", e.getMessage());
        }
    }

    //For Application Groups
    public void hitNewForApplicationGroup(int contractId) {
        HttpResponse response;

        try {
            HttpGet getRequest;
            String queryString = "/applicationgroups/new/" + contractId;
            logger.info("queryString is : {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("New response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api. {}", e.getMessage());
        }
    }

    public String getNewJsonStr() {
        return this.newJsonStr;
    }

    private String getFieldsJsonStr() {
        return this.fieldsJsonStr;
    }

    private void setFieldsJsonStr(String newJsonStr) {
        try {
            JSONObject jsonObj = new JSONObject(newJsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            this.fieldsJsonStr = jsonArr.get(0).toString();
        } catch (Exception e) {
            logger.error("Exception while setting Fields JsonString in New Api. {}", e.getMessage());
        }
    }

    public Map<String, String> setAllRequiredFields(String jsonStr) {
        try {
            if (!this.fieldsJsonSet) {
                this.setFieldsJsonStr(this.getNewJsonStr());
                this.fieldsJsonSet = true;
                setAllRequiredFields(this.getFieldsJsonStr());
            }

            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONUtility jsonUtil = new JSONUtility(jsonObj);
            JSONArray jsonArr = null;

            if (jsonObj.has("fields"))
                jsonArr = new JSONArray(jsonUtil.getStringArrayValueFromJSONObject("fields"));
            logger.debug("Current Json Array is : [ {} ]", jsonArr);
            if (jsonArr != null) {
                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());

                    if (jsonObj.has("fields"))
                        allRequiredFields = this.setAllRequiredFields(jsonArr.get(i).toString());

                    else {
                        jsonUtil = new JSONUtility(jsonObj);

                        if (jsonObj.has("validations")) {
                            String name = jsonUtil.getStringJsonValue("name");
                            JSONArray validationsArray = jsonObj.getJSONArray("validations");

                            for (int j = 0; j < validationsArray.length(); j++) {
                                JSONObject validationsObj = validationsArray.getJSONObject(j);

                                if (validationsObj.has("type") && validationsObj.getString("type").equalsIgnoreCase("required")) {
                                    allRequiredFields.put(name, null);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting All Required Fields in New Api. {}", e.getMessage());
            e.printStackTrace();
        }
        return allRequiredFields;
    }

    public Map<String, String> getAllRequiredFields() {
        return this.allRequiredFields;
    }

    //Get New
    public void hitNew(String entityName, String sourceEntityName, int sourceEntityId, String queryString, String type, String lineItemTypeId) {
        HttpResponse response;

        try {
            HttpGet getRequest;
            int sourceEntityTypeId;
            boolean getQueryString = true;

            if (queryString == null) {
                if (sourceEntityName != null) {
                    sourceEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntityName);
                    String showResponse = ShowHelper.getShowResponse(sourceEntityTypeId, sourceEntityId);

                    int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

                    switch (entityName.trim()) {
                        case "contracts":
                            entityTypeId = ConfigureConstantFields.getEntityIdByName(type);
                            break;

                        case "contract draft request":
                            getQueryString = sourceEntityName.equalsIgnoreCase("contracts");

                            if (!getQueryString) {
                                queryString = "/cdr/new";
                            }

                            break;

                        case "clauses":
                        case "definition":
                        case "contract template structure":
                            queryString = "/" + ConfigureConstantFields.getUrlNameForEntity(entityName.trim()) + "/new";
                            getQueryString = false;
                            break;

                        case "contract templates":
                            queryString = "/contracttemplate/new/2/1002";
                            getQueryString = false;
                            break;

                        case "vendors":
                            queryString = "/vendors/new/" + sourceEntityId;
                            getQueryString = false;
                            break;

                        case "purchase orders":
                            if (sourceEntityName.trim().equalsIgnoreCase("suppliers")) {
                                queryString = "/pos/new/1/" + sourceEntityId;
                                getQueryString = false;
                            }
                            break;

                        default:
                            break;
                    }

                    if (getQueryString) {
                        queryString = ShowHelper.getCreateLinkForEntity(showResponse, entityTypeId, lineItemTypeId);
                    }
                } else {
                    return;
                }
            }

            logger.info("Query string url formed is {}", queryString);

            if (queryString == null) {
                return;
            }

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("New response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api. {}", e.getMessage());
        }
    }

    public void hitNewV1ForMultiSupplier(String entityName, String payload) {
        try {
            HttpPost postRequest;

            String urlName = ConfigureConstantFields.getUrlNameForEntity(entityName);

            String queryString = "/" + urlName + "/v1/new";
            logger.debug("Query string url formed is {}", queryString);

            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            logger.error("Exception while hitting New V1 Api. {}", e.getMessage());
        }
    }

    public void hitNewForGlobalCDR() {
        try {
            HttpGet getRequest;

            String queryString = "/tblcdr/create/rest?version=2.0";
            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            logger.error("Exception while hitting Create API for Global CDR. {}", e.getMessage());
        }
    }

    public void hitNewFromSource(String destinationEntityName, String queryStringToAppend) {
        HttpResponse response;
        try {
            HttpGet getRequest;
            String queryString = null;

            if (destinationEntityName != null) {
                queryString = "/" + ConfigureConstantFields.getUrlNameForEntity(destinationEntityName.trim()) + "/new";

            logger.info("Query string url formed is {}", queryString);

            if (queryString == null) {
                return;
            }

                queryString = queryString.concat("?"+queryStringToAppend);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.newJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("New response header {}", oneHeader.toString());
            }
        }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api. {}", e.getMessage());
        }
    }
}
