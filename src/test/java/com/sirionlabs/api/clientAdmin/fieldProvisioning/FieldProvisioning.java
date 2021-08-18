package com.sirionlabs.api.clientAdmin.fieldProvisioning;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldProvisioning extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(FieldProvisioning.class);

    //This method return HTML Response.
    public String hitFieldProvisioning() {
        String response = null;

        try {
            HttpGet getRequest;
            String queryString = "/fieldprovisioning";
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            logger.info("Hitting FieldProvisioning API");

            HttpResponse httpResponse = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
            response = EntityUtils.toString(httpResponse.getEntity());

            Header[] headers = httpResponse.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Admin Field Provisioning Response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Admin Field Provisioning Api. {}", e.getMessage());
        }
        return response;
    }

    //This method returns JSON Response. GET API for Global Supplier
    public String hitFieldProvisioning(int entityTypeId) {
        return hitFieldProvisioning(entityTypeId, -1);
    }

    //This method returns JSON Response. GET API for Specific Supplier
    public String hitFieldProvisioning(int entityTypeId, int supplierId) {
        String response = null;

        try {
            HttpGet getRequest;
            String queryString = "/fieldprovisioning/" + entityTypeId;

            if (supplierId != -1) {
                queryString = queryString.concat("/" + supplierId);
            }

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            logger.info("Hitting FieldProvisioning API for EntityTypeId {}", entityTypeId);

            HttpResponse httpResponse = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
            response = EntityUtils.toString(httpResponse.getEntity());

            Header[] headers = httpResponse.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Admin Field Provisioning Response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Admin Field Provisioning Api for EntityTypeId {}. {}", entityTypeId, e.getMessage());
        }
        return response;
    }

    public Integer hitFieldProvisioning(int entityTypeId, String payload) {
        return hitFieldProvisioning(entityTypeId, -1, payload);
    }

    //Returns Response Code
    public Integer hitFieldProvisioning(int entityTypeId, int supplierId, String payload) {
        try {
            HttpPost postRequest;
            String queryString = "/fieldprovisioning/" + entityTypeId;

            if (supplierId != -1) {
                queryString = queryString.concat("/" + supplierId);
            }

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);

            Header[] headers = httpResponse.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Field Provisioning Post API Response header {}", oneHeader.toString());
            }

            return httpResponse.getStatusLine().getStatusCode();
        } catch (Exception e) {
            logger.error("Exception while hitting Admin Field Provisioning Api for EntityTypeId {}. {}", entityTypeId, e.getMessage());
        }
        return null;
    }

    public String getFieldProvisioningResponse(int entityTypeId) {
        return getFieldProvisioningResponse(entityTypeId, -1);
    }

    public String getFieldProvisioningResponse(int entityTypeId, int supplierId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        AdminHelper adminHelperObj = new AdminHelper();

        //Logging with Client Admin User
        if (!adminHelperObj.loginWithClientAdminUser()) {
            return null;
        }

        String fieldProvisioningResponse = hitFieldProvisioning(entityTypeId, supplierId);

        //Logging back with End User
        adminHelperObj.loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);

        return fieldProvisioningResponse;
    }

    public List<Map<String, String>> getAllBulkCreateFields(int entityTypeId) {
        logger.info("Getting All Bulk Create Fields for Invoice Entity");
        return getAllBulkCreateFields(getFieldProvisioningResponse(entityTypeId));
    }

    public List<Map<String, String>> getAllBulkCreateFields(int entityTypeId, int supplierId) {
        logger.info("Getting All Bulk Create Fields for Invoice Entity");
        return getAllBulkCreateFields(getFieldProvisioningResponse(entityTypeId, supplierId));
    }

    public List<Map<String, String>> getAllBulkCreateFields(String fieldProvisioningResponse) {
        List<Map<String, String>> allBulkCreateFields = new ArrayList<>();

        try {
            logger.info("Getting All Bulk Create Fields from FieldProvisioning Response.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if (jsonObj.has("bulkCreate") && !jsonObj.isNull("bulkCreate") && jsonObj.getBoolean("bulkCreate")) {
                    Map<String, String> fieldMap = new HashMap<>();

                    if (jsonObj.isNull("apiName")) {
                        fieldMap.put("apiName", "null");
                    } else {
                        fieldMap.put("apiName", jsonObj.getString("apiName"));
                    }

                    fieldMap.put("fieldName", jsonObj.getString("fieldName"));
                    fieldMap.put("mandatory", String.valueOf(jsonObj.getBoolean("bulkCreateMandatory")));
                    fieldMap.put("id", String.valueOf(jsonObj.getJSONObject("field").getInt("id")));
                    fieldMap.put("active", String.valueOf(jsonObj.getBoolean("active")));

                    allBulkCreateFields.add(fieldMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Bulk Create Fields from FieldProvisioning Response. {}", e.getMessage());
            return null;
        }

        return allBulkCreateFields;
    }

    public List<Map<String, String>> getAllDisabledBulkCreateFields(int entityTypeId) {
        return getAllDisabledBulkCreateFields(getFieldProvisioningResponse(entityTypeId));
    }

    public List<Map<String, String>> getAllDisabledBulkCreateFields(String fieldProvisioningResponse) {
        List<Map<String, String>> allDisabledBulkCreateFields = new ArrayList<>();

        try {
            logger.info("Getting All Disabled Bulk Create Fields from FieldProvisioning Response.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if (jsonObj.has("bulkCreateNotAvailable") && jsonObj.getBoolean("bulkCreateNotAvailable")) {
                    Map<String, String> fieldMap = new HashMap<>();

                    if (jsonObj.isNull("apiName")) {
                        fieldMap.put("apiName", "null");
                    } else {
                        fieldMap.put("apiName", jsonObj.getString("apiName"));
                    }

                    fieldMap.put("fieldName", jsonObj.getString("fieldName"));
                    fieldMap.put("id", String.valueOf(jsonObj.getJSONObject("field").getInt("id")));

                    allDisabledBulkCreateFields.add(fieldMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Disabled Bulk Create Fields from FieldProvisioning Response. {}", e.getMessage());
            return null;
        }

        return allDisabledBulkCreateFields;
    }

    public Integer getSupplierIdApplicableForEntity(String entityName, String supplierName) {
        try {
            logger.info("Getting Applicable Supplier Provisioning Id for Entity {} for Supplier {}.", entityName, supplierName);
            String lastLoggedInUserName = Check.lastLoggedInUserName;
            String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

            AdminHelper adminHelperObj = new AdminHelper();

            //Logging with Client Admin User
            if (!adminHelperObj.loginWithClientAdminUser()) {
                return null;
            }

            String fieldProvisioningResponse = hitFieldProvisioning();

            //Logging back with End User
            adminHelperObj.loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);

            Document html = Jsoup.parse(fieldProvisioningResponse);
            String dropDownString = html.getElementById("mainContainer").toString();
            dropDownString = dropDownString.substring(dropDownString.indexOf("var dropDown"));
            dropDownString = dropDownString.substring(dropDownString.indexOf("["), dropDownString.indexOf("];") + 1);

            JSONArray jsonArr = new JSONArray(dropDownString);

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);

                if (jsonObj.getJSONObject("relation").getString("name").equalsIgnoreCase(supplierName)) {
                    jsonArr = jsonObj.getJSONArray("entityTypes");

                    for (int j = 0; j < jsonArr.length(); j++) {
                        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

                        if (jsonArr.getJSONObject(j).getInt("id") == entityTypeId) {
                            return jsonObj.getJSONObject("relation").getInt("id");
                        }
                    }

                    break;
                }
            }

            return -1;
        } catch (Exception e) {
            logger.error("Exception while Getting Applicable Supplier Provisioning Id for Entity {} for Supplier {}. {}", entityName, supplierName, e.getMessage());
        }

        return null;
    }

    public int getNewlyCreatedProvisioningId(int clientId, int supplierId, int entityTypeId) {
        int provisioningId = -1;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select id from client_field_provisioning where entity_type_id = " + entityTypeId + " and relation_id = " + supplierId + " and client_id=" +
                    clientId + " order by id desc limit 1";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                provisioningId = Integer.parseInt(results.get(0).get(0));
            }

            sqlObj.closeConnection();
            return provisioningId;
        } catch (Exception e) {
            logger.error("Exception while Getting Newly Created Provisioning Id for Client Id {}, Supplier Id {} and EntityTypeId {}. {}", clientId, supplierId,
                    entityTypeId, e.getStackTrace());
        }

        return provisioningId;
    }

    public boolean deleteProvisioningData(int provisioningId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

            String query = "delete from client_field_provisioning_data where provisioning_id=" + provisioningId;
            sqlObj.deleteDBEntry(query);

            query = "delete from client_field_provisioning where id = " + provisioningId;
            int deleteEntriesFromClientFieldProvisioningTable = sqlObj.deleteDBEntry(query);

            sqlObj.closeConnection();

            return (deleteEntriesFromClientFieldProvisioningTable != 0);
        } catch (Exception e) {
            logger.error("Exception while deleting all Data in for Provisioning Id. " + e.getMessage());
        }

        return false;
    }

    public List<Map<String, String>> getAllBulkUpdateAvailableFields(String fieldProvisioningResponse) {
        List<Map<String, String>> allBulkUpdateFields = new ArrayList<>();

        try {
            logger.info("Getting All Bulk Update Available Fields from FieldProvisioning Response.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if (jsonObj.has("bulkUpdateAvailable") && !jsonObj.isNull("bulkUpdateAvailable") && jsonObj.getBoolean("bulkUpdateAvailable")) {
                    Map<String, String> fieldMap = new HashMap<>();

                    if (jsonObj.isNull("apiName")) {
                        fieldMap.put("apiName", "null");
                    } else {
                        fieldMap.put("apiName", jsonObj.getString("apiName"));
                    }

                    fieldMap.put("fieldName", jsonObj.getString("fieldName"));
                    fieldMap.put("id", String.valueOf(jsonObj.getJSONObject("field").getInt("id")));
                    fieldMap.put("active", String.valueOf(jsonObj.getBoolean("active")));

                    allBulkUpdateFields.add(fieldMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Bulk Update Available Fields from FieldProvisioning Response. {}", e.getMessage());
            return null;
        }

        return allBulkUpdateFields;
    }

    public List<Map<String, String>> getAllBulkUpdateEnabledFields(String fieldProvisioningResponse) {
        List<Map<String, String>> allBulkUpdateEnabledFields = new ArrayList<>();

        try {
            logger.info("Getting All Bulk Update Enabled Fields from FieldProvisioning Response.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if ((jsonObj.has("bulkUpdate") && !jsonObj.isNull("bulkUpdate") && jsonObj.getBoolean("bulkUpdate")) &&
                        (jsonObj.has("bulkUpdateAvailable") && !jsonObj.isNull("bulkUpdateAvailable") && jsonObj.getBoolean("bulkUpdateAvailable"))) {
                    Map<String, String> fieldMap = new HashMap<>();

                    if (jsonObj.isNull("apiName")) {
                        fieldMap.put("apiName", "null");
                    } else {
                        fieldMap.put("apiName", jsonObj.getString("apiName"));
                    }

                    fieldMap.put("fieldName", jsonObj.getString("fieldName"));
                    fieldMap.put("id", String.valueOf(jsonObj.getJSONObject("field").getInt("id")));
                    fieldMap.put("active", String.valueOf(jsonObj.getBoolean("active")));

                    allBulkUpdateEnabledFields.add(fieldMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Bulk Update Enabled Fields from FieldProvisioning Response. {}", e.getMessage());
            return null;
        }

        return allBulkUpdateEnabledFields;
    }

    public List<Map<String, String>> getAllBulkUpdateReadOnlyFields(String fieldProvisioningResponse) {
        List<Map<String, String>> allBulkUpdateReadOnlyFields = new ArrayList<>();

        try {
            logger.info("Getting All Bulk Update Read Only Fields from FieldProvisioning Response.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if (jsonObj.has("bulkUpdateReadOnly") && !jsonObj.isNull("bulkUpdateReadOnly") && jsonObj.getBoolean("bulkUpdateReadOnly")) {
                    Map<String, String> fieldMap = new HashMap<>();

                    if (jsonObj.isNull("apiName")) {
                        fieldMap.put("apiName", "null");
                    } else {
                        fieldMap.put("apiName", jsonObj.getString("apiName"));
                    }

                    fieldMap.put("fieldName", jsonObj.getString("fieldName"));
                    fieldMap.put("id", String.valueOf(jsonObj.getJSONObject("field").getInt("id")));
                    fieldMap.put("active", String.valueOf(jsonObj.getBoolean("active")));

                    allBulkUpdateReadOnlyFields.add(fieldMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Bulk Update Read Only Fields from FieldProvisioning Response. {}", e.getMessage());
            return null;
        }

        return allBulkUpdateReadOnlyFields;
    }

    public List<Map<String, String>> getAllFields(String fieldProvisioningResponse) {
        List<Map<String, String>> allFields = new ArrayList<>();

        try {
            logger.info("Getting All Fields from FieldProvisioning Response.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                Map<String, String> fieldMap = new HashMap<>();

                if (jsonObj.isNull("apiName")) {
                    fieldMap.put("apiName", "null");
                } else {
                    fieldMap.put("apiName", jsonObj.getString("apiName"));
                }

                fieldMap.put("fieldName", jsonObj.getString("fieldName"));
                fieldMap.put("id", String.valueOf(jsonObj.getJSONObject("field").getInt("id")));
                fieldMap.put("active", String.valueOf(jsonObj.getBoolean("active")));

                allFields.add(fieldMap);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Fields from FieldProvisioning Response. {}", e.getMessage());
            return null;
        }

        return allFields;
    }

    public Map<String, String> getFieldPropertiesFromFieldId(String fieldProvisioningResponse, int fieldId) {
        Map<String, String> fieldPropertiesMap = new HashMap<>();

        try {
            logger.info("Getting Properties of Field Id {}", fieldId);
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if (jsonObj.getJSONObject("field").getInt("id") == fieldId) {
                    if (jsonObj.isNull("apiName")) {
                        fieldPropertiesMap.put("apiName", "null");
                    } else {
                        fieldPropertiesMap.put("apiName", jsonObj.getString("apiName"));
                    }

                    fieldPropertiesMap.put("fieldName", jsonObj.getString("fieldName"));
                    fieldPropertiesMap.put("id", String.valueOf(jsonObj.getJSONObject("field").getInt("id")));
                    fieldPropertiesMap.put("active", String.valueOf(jsonObj.getBoolean("active")));
                    fieldPropertiesMap.put("bulkUpdate", String.valueOf(jsonObj.getBoolean("bulkUpdate")));
                    fieldPropertiesMap.put("bulkUpdateAvailable", String.valueOf(jsonObj.getBoolean("bulkUpdateAvailable")));

                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Properties of Field Id {}. {}", fieldId, e.getMessage());
            return null;
        }

        return fieldPropertiesMap;
    }

    public List<Integer> getAllFieldIds(String fieldProvisioningResponse) {
        List<Integer> allFieldIds = new ArrayList<>();

        try {
            logger.info("Getting All Field Ids from FieldProvisioning Response.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                allFieldIds.add(jsonObj.getJSONObject("field").getInt("id"));
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Field Ids from FieldProvisioning Response. {}", e.getMessage());
            return null;
        }

        return allFieldIds;
    }

    public String getFieldAliasLabelIdFromApiName(String apiName, int entityTypeId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

            String query = "select alias_label_id from entity_field where api_name = '" + apiName + "' and entity_type_id = " + entityTypeId;
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                sqlObj.closeConnection();
                return results.get(0).get(0);
            } else {
                logger.error("Couldn't get Alias Label Id for Field having Api Name {} of EntityTypeId {}", apiName, entityTypeId);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Alias Label Id for Field having Api Name {} of EntityTypeId {}. {}", apiName, entityTypeId, e.getMessage());
        }

        return null;
    }

    public Boolean fieldProvisioningPresentForSupplierOfEntity(String supplierId, int entityTypeId, int clientId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select count(*) from client_field_provisioning where client_id = " + clientId + " and entity_type_id = " + entityTypeId +
                    " and relation_id = " + supplierId;

            List<List<String>> results = sqlObj.doSelect(query);
            sqlObj.closeConnection();

            if (!results.isEmpty()) {
                return !results.get(0).get(0).equalsIgnoreCase("0");
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Field Provisioning is Present for Supplier Id {} and EntityTypeId {}. {}", supplierId, entityTypeId, e.getMessage());
        }

        return null;
    }

    public List<Map<String, String>> getAllInactiveFields(String fieldProvisioningResponse) {
        List<Map<String, String>> allInactiveFields = new ArrayList<>();

        try {
            logger.info("Getting All Inactive Fields from FieldProvisioning Response.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if (!jsonObj.getBoolean("active")) {
                    Map<String, String> fieldMap = new HashMap<>();

                    if (jsonObj.isNull("apiName")) {
                        fieldMap.put("apiName", "null");
                    } else {
                        fieldMap.put("apiName", jsonObj.getString("apiName"));
                    }

                    fieldMap.put("fieldName", jsonObj.getString("fieldName"));
                    fieldMap.put("id", String.valueOf(jsonObj.getJSONObject("field").getInt("id")));

                    allInactiveFields.add(fieldMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Inactive Fields from FieldProvisioning Response. {}", e.getMessage());
            return null;
        }

        return allInactiveFields;
    }
}