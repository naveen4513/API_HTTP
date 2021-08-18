package com.sirionlabs.api.clientAdmin.invoiceCreateRule;

import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class InvoiceCreateRule extends TestAPIBase {

    private String showResponse, updateResponse, newResponse, createResponse, createPayload, editResponse;
    private final static Logger logger = LoggerFactory.getLogger(InvoiceCreateRule.class);

    private Map<String, String> getHeaders() {

        Map<String, String> headers = new HashMap<>();

        try {
            headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Content-Type", "application/json;charset=UTF-8");
            headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        } catch (Exception e) {
            logger.info("Exception occurred in creating headers for request");
        }

        return headers;
    }

    public void hitNew(CustomAssert customAssert) {
        try {
            String newUrl = "/workflowrule/new";
            APIValidator apiValidator = executor.get(newUrl, getHeaders());
            APIResponse apiResponse = apiValidator.getResponse();
            newResponse = apiResponse.getResponseBody();

            logger.debug("Response status is {}", apiResponse.getResponseCode());

        } catch (Exception e) {
            logger.error("Exception while hitting NEW in invoice create rule . {}", e.toString());
            customAssert.assertTrue(false, "Exception while hitting NEW in invoice create rule " + e.toString());
        }
    }

    public void createRule(CustomAssert customAssert) {

        try {
            String createUrl = "/workflowrule/create";
            APIValidator apiValidator = executor.post(createUrl, getHeaders(), createPayload);
            APIResponse apiResponse = apiValidator.getResponse();
            createResponse = apiResponse.getResponseBody();
        } catch (Exception e) {
            logger.info("Exception found in creating Invoice Rule");
            customAssert.assertTrue(false, "Exception found in creating Invoice Rule");
        }
    }

    public void hitShow(int dataId) {

        try {
            HttpGet getRequest;

            String showUrl = "/workflowrule/show";
            String queryString = "/" + showUrl + dataId;
            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            showResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while editing invoice create rule Id {}. {}", dataId, e.getStackTrace());
        }
    }

    public void hitEdit(String ruleID) {
        try {
            String updateUrl = "/workflowrule/edit/" + ruleID;

            APIValidator apiValidator = executor.get(updateUrl, getHeaders());
            APIResponse apiResponse = apiValidator.getResponse();
            editResponse = apiResponse.getResponseBody();

        } catch (Exception e) {
            logger.error("Exception while updating invoice create rule . {}", e.toString());
        }
    }

    public void hitUpdate(String payload) {
        try {
            String updateUrl = "/workflowrule/edit";

            APIValidator apiValidator = executor.post(updateUrl, getHeaders(), payload);
            APIResponse apiResponse = apiValidator.getResponse();
            updateResponse = apiResponse.getResponseBody();

        } catch (Exception e) {
            logger.error("Exception while updating invoice create rule . {}", e.toString());
        }
    }

    public String getShowResponse() {
        return showResponse;
    }

    public String getUpdateResponse() {
        return updateResponse;
    }

    public String getCreateResponse() {
        return createResponse;
    }

    public String getNewResponse() {
        return newResponse;
    }

    public void setCreatePayload(String name, String rule, String sectionName, int priority, CustomAssert customAssert) {
        Map<String, String> payload = ParseConfigFile.getAllConstantPropertiesCaseSensitive(ConfigureConstantFields.getConstantFieldsProperty("invoiceCreateRulePayloadFilePath"), ConfigureConstantFields.getConstantFieldsProperty("invoiceCreateRulePayloadFileName"), sectionName);
        String supplier = payload.get("supplier");
        String ruleType = payload.get("ruleType");
        String entityType = payload.get("entityType");
        setCreatePayload(name, priority, true, supplier, "[]", "", ruleType, rule, entityType, "[]", customAssert);
    }

    public void setCreatePayload(String name, String rule, String sectionName, CustomAssert customAssert) {
        Map<String, String> payload = ParseConfigFile.getAllConstantPropertiesCaseSensitive(ConfigureConstantFields.getConstantFieldsProperty("invoiceCreateRulePayloadFilePath"), ConfigureConstantFields.getConstantFieldsProperty("invoiceCreateRulePayloadFileName"), sectionName);
        String supplier = payload.get("supplier");
        String ruleType = payload.get("ruleType");
        String entityType = payload.get("entityType");
        setCreatePayload(name, 1, true, supplier, "[]", "", ruleType, rule, entityType, "[]", customAssert);
    }

    public void setCreatePayloadWithCondition(String name, String rule, String sectionName, String condition, CustomAssert customAssert) {
        Map<String, String> payload = ParseConfigFile.getAllConstantPropertiesCaseSensitive(ConfigureConstantFields.getConstantFieldsProperty("invoiceCreateRulePayloadFilePath"), ConfigureConstantFields.getConstantFieldsProperty("invoiceCreateRulePayloadFileName"), sectionName);
        String supplier = payload.get("supplier");
        String ruleType = payload.get("ruleType");
        String entityType = payload.get("entityType");
        setCreatePayload(name, 1, true, supplier, "[]", condition, ruleType, rule, entityType, "[]", customAssert);
    }

    private void removeOptionsNodeFromJson(JSONObject jsonObject) {
        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()) {
            nextKey = iterator.next();
            if (nextKey.equalsIgnoreCase("options"))
                jsonObject.put("options", JSONObject.NULL);
            else {
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    removeOptionsNodeFromJson((JSONObject) jsonObject.get(nextKey));
                } else if (jsonObject.get(nextKey) instanceof JSONArray)
                    removeOptionsNodeFromJson((JSONArray) jsonObject.get(nextKey));
            }
        }
    }

    private void removeOptionsNodeFromJson(JSONArray jsonArray) {

        for (int index = 0; index < jsonArray.length(); index++) {
            if (jsonArray.get(index) instanceof JSONArray)
                removeOptionsNodeFromJson((JSONArray) jsonArray.get(index));
            else if (jsonArray.get(index) instanceof JSONObject)
                removeOptionsNodeFromJson((JSONObject) jsonArray.get(index));
        }
    }

    public void setCreatePayload(String name, int priority, boolean active, String suppliers, String contracts, String condition, String ruleType, String rule, String entityType, String commentDocuments, CustomAssert customAssert) {

        try {
            String newResponseLocal = "";
            hitNew(customAssert);
            if (newResponse != null) {
                if (newResponse.contains("\"status\":\"success\"")) {
                    newResponseLocal = newResponse;
                    customAssert.assertTrue(true, "newResponse contains success");
                } else {
                    logger.info("newResponse contains failure cannot proceed further");
                    customAssert.assertTrue(false, "newResponse contains failure cannot proceed further");
                }
            } else {
                logger.info("newResponse is null cannot proceed further");
                customAssert.assertTrue(false, "newResponse is null cannot proceed further");
                customAssert.assertAll();
            }

            JSONObject jsonObject = new JSONObject(newResponseLocal);
            jsonObject.remove("header");
            jsonObject.remove("session");
            jsonObject.remove("actions");
            jsonObject.remove("createLinks");
            jsonObject.getJSONObject("body").remove("layoutInfo");
            jsonObject.getJSONObject("body").remove("globalData");
            jsonObject.getJSONObject("body").remove("errors");

            removeOptionsNodeFromJson(jsonObject);
//            Iterator<String> iterator = jsonObject.getJSONObject("body").getJSONObject("data").keys();
//            String nextKey;
//            while (iterator.hasNext()){
//                nextKey = iterator.next();
//                //jsonObject.getJSONObject(nextKey);
//                if(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(nextKey).has("options"))
//                    jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(nextKey).put("options",JSONObject.NULL);
//            }

            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values", active);
            //jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values",new JSONArray(commentDocuments));
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").put("commentDocuments", new JSONObject("{\"values\": " + new JSONArray(commentDocuments) + "}"));
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("condition").put("values", condition);
            //jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contracts").put("values",new JSONArray(contracts));
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("entityType").put("values", new JSONObject(entityType));
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values", name);
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("priority").put("values", priority);
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("rule").put("values", rule);
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("ruleType").put("values", new JSONObject(ruleType));
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("suppliers").put("values", new JSONArray(suppliers));

            createPayload = jsonObject.toString();
        } catch (Exception e) {
            logger.info("Exception found in creating payload for Invoice Rule {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception found in creating payload for Invoice Rule");
        }
    }

    public void setRuleInactiveForSupplier(String supplierName, CustomAssert customAssert) {
        try {
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListData(243, 0, 15000, "id", "desc", 421, "");
            String listResponse = listRendererListData.getListDataJsonStr();

            JSONObject jsonObject = new JSONObject(listResponse);
            int size = jsonObject.getJSONArray("data").length();
            for (int index = 0; index < size; index++) {
                String active = jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject("18157").getString("value");
                if (active.equalsIgnoreCase("true")) {
                    if (jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject("18164").has("value")) {
                        String supplier = jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject("18164").getString("value");
                        if (supplier.equalsIgnoreCase(supplierName)) {
                            String id = jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject("18152").getString("value").split(":;")[1];
                            this.hitEdit(id);
                            String response = this.editResponse;
                            JSONObject jsonObject1 = new JSONObject(response);
                            jsonObject1.remove("header");
                            jsonObject1.remove("session");
                            jsonObject1.remove("actions");
                            jsonObject1.remove("createLinks");
                            jsonObject1.getJSONObject("body").remove("layoutInfo");
                            jsonObject1.getJSONObject("body").remove("globalData");
                            jsonObject1.getJSONObject("body").remove("errors");

                            jsonObject1.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values", false);

                            this.hitUpdate(jsonObject1.toString());
                            if (this.updateResponse.contains("success")) {
                                logger.info("Invoice rule with id [{}] is successfully updated", id);
                                customAssert.assertTrue(true, "Invoice rule with id [" + id + "] successfully updated");
                            } else {
                                logger.info("Invoice rule with id [{}] is not updated", id);
                                //customAssert.assertTrue(false,"Invoice rule with id ["+id+"] not updated");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception caught in setRuleInactive() {}", (Object) e.getStackTrace());
            //customAssert.assertTrue(false, "Exception caught in setRuleInactive() " + Arrays.toString(e.getStackTrace()));
        }
    }

    public void deactivateRules(List<String> rulesToBeDeactivated){
        logger.info("Deactivating all the created Rules.");

        new AdminHelper().loginWithClientAdminUser();
        for(String ruleId : rulesToBeDeactivated) {
            this.hitEdit(ruleId);

            JSONObject jsonObject1 = new JSONObject(editResponse);
            jsonObject1.remove("header");
            jsonObject1.remove("session");
            jsonObject1.remove("actions");
            jsonObject1.remove("createLinks");
            jsonObject1.getJSONObject("body").remove("layoutInfo");
            jsonObject1.getJSONObject("body").remove("globalData");
            jsonObject1.getJSONObject("body").remove("errors");
            removeOptionsNodeFromJson(jsonObject1);
            jsonObject1.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values", false);

            hitUpdate(jsonObject1.toString());
            logger.info("Deactivating rule {} is {}",ruleId, updateResponse.contains("success"));
        }
    }

    public void deactivateRule(String ruleToBeDeactivated){
        logger.info("Deactivating all the created line items.");

        ListRendererListData rulesList = new ListRendererListData();
        rulesList.hitListRendererListData(421,"{\"filterMap\":{}}");
        String rulesListResponse = rulesList.getListDataJsonStr();

        JSONObject jsonObject = new JSONObject(rulesListResponse);
        JSONArray jsonArray = jsonObject.getJSONArray("data");

        String fieldIdForRuleName = "18090", fieldIdForRuleId = "18089";
        for(Object object : jsonArray){
            JSONObject item = (JSONObject) object;
            if(ruleToBeDeactivated.equalsIgnoreCase(item.getJSONObject(fieldIdForRuleName).getString("value"))){
                this.hitEdit(item.getJSONObject(fieldIdForRuleId).getString("value").split(":;")[1]);


                JSONObject jsonObject1 = new JSONObject(editResponse);
                jsonObject1.remove("header");
                jsonObject1.remove("session");
                jsonObject1.remove("actions");
                jsonObject1.remove("createLinks");
                jsonObject1.getJSONObject("body").remove("layoutInfo");
                jsonObject1.getJSONObject("body").remove("globalData");
                jsonObject1.getJSONObject("body").remove("errors");
                removeOptionsNodeFromJson(jsonObject1);
                jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values", false);

                hitUpdate(jsonObject1.toString());
                logger.info("Deactivating rule {} is {}",item.getJSONObject(fieldIdForRuleName).getString("value"),updateResponse.contains("success"));
            }
        }
    }
}
