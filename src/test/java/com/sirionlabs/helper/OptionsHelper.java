package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionsHelper {

    private final static Logger logger = LoggerFactory.getLogger(OptionsHelper.class);
    private String optionsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath");
    private String optionsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName");
    private Options optionObj = new Options();

    public int getDropDownId(String fieldName) {
        String dropDownId = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropDownType", fieldName.trim());

        return (dropDownId == null || dropDownId.isEmpty()) ? -1 : Integer.parseInt(dropDownId);
    }

    public Integer getIdOfAnyStakeHolder(String stakeHolderName) {
        int id = -1;

        try {
            logger.info("Getting Id of StakeHolder {}", stakeHolderName);
            int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "dropDownType",
                    "stakeholders"));

            String pageType = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "pageType", "filterData");
            Map<String, String> params = new HashMap<>();
            params.put("pageType", pageType);
            params.put("query", stakeHolderName);

            logger.info("Hitting Options API for StakeHolder {}", stakeHolderName);
            optionObj.hitOptions(dropDownType, params);
            String optionsStr = optionObj.getOptionsJsonStr();

            if (ParseJsonResponse.validJsonResponse(optionsStr)) {
                JSONObject jsonObj = new JSONObject(optionsStr);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                if (jsonArr.length() > 1) {
                    boolean idFound = false;

                    for (int i = 0; i < jsonArr.length(); i++) {
                        jsonObj = jsonArr.getJSONObject(i);

                        if (jsonObj.getString("name").trim().equalsIgnoreCase(stakeHolderName.trim())) {
                            idFound = true;
                            id = jsonObj.getInt("id");
                            break;
                        }
                    }

                    if (!idFound) {
                        jsonObj = jsonArr.getJSONObject(0);
                        logger.info("Multiple Records found for StakeHolder {}. Hence selecting first StakeHolder i.e. {}", stakeHolderName, jsonObj.getString("name"));
                        id = jsonObj.getInt("id");
                    }
                } else {
                    jsonObj = jsonArr.getJSONObject(0);
                    id = jsonObj.getInt("id");
                }
            } else {
                logger.error("Options API Response for StakeHolder {} is an Invalid JSON.", stakeHolderName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Id of StakeHolder {}. {}", stakeHolderName, e.getStackTrace());
        }
        return id;
    }

    public String hitOptionsForAllStakeholders() {
        return hitOptionsForAllStakeholdersOfAGroup(null);
    }

    public String hitOptionsForAllStakeholdersOfAGroup(String roleGroupId) {
        return hitOptionsForAllStakeholdersOfAGroup(roleGroupId, "");
    }

    private String hitOptionsForAllStakeholdersOfAGroup(String roleGroupId, String query) {
        String optionsResponse = null;

        try {
            int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "dropDownType",
                    "stakeholders"));

            String pageType = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "pageType", "listData");
            Map<String, String> params = new HashMap<>();
            params.put("pageType", pageType);

            if (roleGroupId != null)
                params.put("roleGroupId", roleGroupId);

            params.put("query", query);

            logger.info("Hitting Options API for Stakeholder Role Group Id {} and Query [{}]", roleGroupId, query);
            optionObj.hitOptions(dropDownType, params);
            optionsResponse = optionObj.getOptionsJsonStr();
        } catch (Exception e) {
            logger.error("Exception while Getting All Stakeholders of Role Group Id {} and Query [{}]. {}", roleGroupId, query, e.getStackTrace());
        }
        return optionsResponse;
    }

    public String hitOptionsForAutoCompleteFilter(String filterName, int entityTypeId) {
        return hitOptionsForAutoCompleteFilter(filterName, entityTypeId, "");
    }

    public String hitOptionsForAutoCompleteFilter(String filterName, int entityTypeId, String query) {
        String optionsResponse = null;

        try {
            String dropDownType = ParseConfigFile.getValueFromConfigFileCaseSensitive(optionsConfigFilePath, optionsConfigFileName,
                    "filter name dropdown id mapping", filterName.trim());

            if (NumberUtils.isParsable(dropDownType)) {
                int dropDownId = Integer.parseInt(dropDownType);
                String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
                int listId = ConfigureConstantFields.getListIdForEntity(entityName);

                String pageType = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "pageType", "listData");
                Map<String, String> params = new HashMap<>();
                params.put("pageType", pageType);
                params.put("pageEntityTypeId", String.valueOf(entityTypeId));
                params.put("query", query);
                params.put("listId", String.valueOf(listId));

                logger.info("Hitting Options API for Filter [{}] and Query [{}]", filterName, query);
                optionObj.hitOptions(dropDownId, params);
                optionsResponse = optionObj.getOptionsJsonStr();
            } else {
                logger.error("Couldn't get DropDownId for Filter [{}]", filterName);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Options for Filter {}. {}", filterName, e.getStackTrace());
        }
        return optionsResponse;
    }

    public String hitOptionsForCalendarFilter(String filterName, int entityTypeId, String query) {
        String optionsResponse = null;

        try {
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
            int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "dropDownType",
                    entityName));

            String pageType = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "pageType", "filterdata");
            Map<String, String> params = new HashMap<>();
            params.put("pageType", pageType);
            params.put("query", query);

            logger.info("Hitting Options API for Calendar Filter [{}] and Query [{}]", filterName, query);
            optionObj.hitOptions(dropDownType, params);
            optionsResponse = optionObj.getOptionsJsonStr();
        } catch (Exception e) {
            logger.error("Exception while hitting Options API for Calendar Filter {} and Query [{}]. {}", filterName, query, e.getStackTrace());
        }
        return optionsResponse;
    }

    public List<Map<String, String>> getAllOptionsForAutoCompleteField(String entityName, int entityTypeId, String pageType, String query) {
        List<Map<String, String>> allOptions = new ArrayList<>();

        try {
            String dropDownType = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "dropdowntype",
                    entityName.trim());

            if (NumberUtils.isParsable(dropDownType)) {
                int dropDownId = Integer.parseInt(dropDownType);
                String pageTypeId = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "pageType", pageType);

                Map<String, String> params = new HashMap<>();
                params.put("pageType", pageTypeId);
                params.put("entityTypeId", String.valueOf(entityTypeId));
                params.put("query", query);

                logger.info("Hitting Options API for Query [{}]", query);
                optionObj.hitOptions(dropDownId, params);
                String optionsResponse = optionObj.getOptionsJsonStr();

                if (ParseJsonResponse.validJsonResponse(optionsResponse)) {
                    JSONObject jsonObj = new JSONObject(optionsResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        Map<String, String> optionMap = new HashMap<>();

                        optionMap.put("id", String.valueOf(jsonArr.getJSONObject(i).getInt("id")));
                        optionMap.put("name", jsonArr.getJSONObject(i).getString("name"));

                        allOptions.add(optionMap);
                    }
                }
            } else {
                logger.error("Couldn't get DropDownId for Entity [{}]", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Options. {}", e.getMessage());
            return null;
        }
        return allOptions;
    }

    Integer getIdForUser(String userName) {
        try {
            logger.info("Getting Id for User [{}]", userName);
            String dropDownType = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "dropdowntype",
                    "userConfigurationList");

            if (NumberUtils.isParsable(dropDownType)) {
                int dropDownId = Integer.parseInt(dropDownType);
                String pageTypeId = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "pageType", "listData");

                Map<String, String> params = new HashMap<>();
                params.put("pageType", pageTypeId);
                params.put("entityTypeId", "20");
                params.put("pageEntityTypeId", "20");
                params.put("query", userName);
                params.put("listId", "328");

                logger.info("Hitting Options API for Query [{}]", userName);
                optionObj.hitOptions(dropDownId, params);
                String optionsResponse = optionObj.getOptionsJsonStr();

                if (ParseJsonResponse.validJsonResponse(optionsResponse)) {
                    JSONObject jsonObj = new JSONObject(optionsResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        if (jsonArr.getJSONObject(i).getString("name").equalsIgnoreCase(userName)) {
                            return jsonArr.getJSONObject(i).getInt("id");
                        }
                    }

                    logger.error("Couldn't find Id for User {}", userName);
                } else {
                    logger.error("Options API Response is an Invalid JSON.");
                }
            } else {
                logger.error("Couldn't get DropDownId for Property [userConfigurationList]");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Id for User [{}]. {}", userName, e.getStackTrace());
        }
        return null;
    }

    public List<Map<String, String>> getAllSupplierTypeUsersFromOptionsResponse(String optionResponse) {
        List<Map<String, String>> allSupplierTypeUsers = new ArrayList<>();

        try {
            if (ParseJsonResponse.validJsonResponse(optionResponse)) {
                JSONObject jsonObj = new JSONObject(optionResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    int idType = 0;
                    try {
                        idType = jsonArr.getJSONObject(i).getInt("idType");
                    } catch(Exception e){
                        jsonArr.getJSONObject(i).getJSONArray("idTypes").getInt(0);
                    }

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
                logger.error("Option Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Supplier Type Users from Option Response. {}", e.getMessage());
        }

        return null;
    }

    public List<Map<String, String>> getAllSupplierOptions(int pageEntityTypeId, String query, boolean expandAutoComplete) {
        List<Map<String, String>> allSupplierOptions = new ArrayList<>();

        try {
            Map<String, String> params = new HashMap<>();
            params.put("pageType", "1");
            params.put("entityTypeId", "1");
            params.put("pageEntityTypeId", String.valueOf(pageEntityTypeId));

            query = query == null ? "" : query;

            params.put("query", query);
            params.put("expandAutoComplete", String.valueOf(expandAutoComplete));

            logger.info("Hitting Options API for Query [{}]", query);
            optionObj.hitOptions(2, params);
            String optionsResponse = optionObj.getOptionsJsonStr();

            if (ParseJsonResponse.validJsonResponse(optionsResponse)) {
                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    Map<String, String> optionMap = new HashMap<>();

                    optionMap.put("id", String.valueOf(jsonArr.getJSONObject(i).getInt("id")));
                    optionMap.put("name", jsonArr.getJSONObject(i).getString("name"));

                    allSupplierOptions.add(optionMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Supplier Options. {}", e.getMessage());
            return null;
        }
        return allSupplierOptions;
    }

    public List<Map<String, String>> getAllContractOptionsForSupplierAndCustomer(int parentEntityId, int relationId, String query, boolean expandAutoComplete) {
        return getAllContractOptionsForSupplierAndCustomer(parentEntityId, -1, relationId, query, expandAutoComplete);
    }

    public List<Map<String, String>> getAllContractOptionsForSupplierAndCustomer(int parentEntityId, int customerId, int relationId, String query,
                                                                                 boolean expandAutoComplete) {
        List<Map<String, String>> allContractOptions = new ArrayList<>();

        try {
            Map<String, String> params = new HashMap<>();
            params.put("pageType", "1");
            params.put("entityTpeId", "315");
            params.put("parentEntityId", String.valueOf(parentEntityId));
            params.put("parentEntityTypeId", String.valueOf(parentEntityId));
            params.put("relationId", String.valueOf(relationId));

            if (customerId != -1) {
                params.put("customerId", String.valueOf(customerId));
            }

            query = query == null ? "" : query;

            params.put("query", query);
            params.put("expandAutoComplete", String.valueOf(expandAutoComplete));

            logger.info("Hitting Options API for Query [{}]", query);
            int offset = 0;
            int totalSize = -1;
            int sizeLimit = -1;

            do {
                params.put("offset", String.valueOf(offset));
                optionObj.hitOptions(41, params);
                String optionsResponse = optionObj.getOptionsJsonStr();

                if (ParseJsonResponse.validJsonResponse(optionsResponse)) {
                    JSONObject jsonObj = new JSONObject(optionsResponse);

                    if (totalSize == -1) {
                        totalSize = jsonObj.getInt("size");
                    }

                    if (sizeLimit == -1) {
                        sizeLimit = jsonObj.getInt("sizeLimit");
                    }

                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        Map<String, String> optionMap = new HashMap<>();

                        optionMap.put("id", String.valueOf(jsonArr.getJSONObject(i).getInt("id")));
                        optionMap.put("name", jsonArr.getJSONObject(i).getString("name"));

                        allContractOptions.add(optionMap);
                    }

                    offset += sizeLimit;
                }
            } while (totalSize > offset);
        } catch (Exception e) {
            logger.error("Exception while Getting All Contract Options for Supplier Id {} and Customer Id {}. {}", relationId, customerId, e.getMessage());
            return null;
        }
        return allContractOptions;
    }

    public String hitOptionsAPIForTags(String query, String languageId) {
        Map<String, String> optionParams = new HashMap<>();
        optionParams.put("pageType", "6");
        optionParams.put("entityTpeId", "206");
        optionParams.put("pageEntityTypeId", "206");
        optionParams.put("query", query);
        optionParams.put("languageType", languageId);

        optionObj.hitOptions(18, optionParams);
        return optionObj.getOptionsJsonStr();
    }
}
