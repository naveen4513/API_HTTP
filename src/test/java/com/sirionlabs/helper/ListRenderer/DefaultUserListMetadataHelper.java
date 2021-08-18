package com.sirionlabs.helper.ListRenderer;

import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultUserListMetadataHelper {

    private final static Logger logger = LoggerFactory.getLogger(DefaultUserListMetadataHelper.class);

    public static boolean hasFilterMetadata(String listJsonResponse, String filterQueryName) {
        return hasField(listJsonResponse, "filterMetadatas", filterQueryName);
    }

    public static boolean hasColumn(String listJsonResponse, String columnQueryName) {
        return hasField(listJsonResponse, "columns", columnQueryName);
    }

    public static boolean hasExcelColumn(String listJsonResponse, String excelQueryName) {
        return hasField(listJsonResponse, "ecxelColumns", excelQueryName);
    }

    private static boolean hasField(String listJsonResponse, String arrayName, String queryName) {
        JSONObject jsonObj = new JSONObject(listJsonResponse);

        if (!jsonObj.has(arrayName) || jsonObj.isNull(arrayName)) {
            return false;
        }

        JSONArray jsonArr = jsonObj.getJSONArray(arrayName);

        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonArr.getJSONObject(i).getString("queryName").equalsIgnoreCase(queryName)) {
                return true;
            }
        }

        return false;
    }

    public String getDefaultUserListMetadataResponse(String entityName) {
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);

        logger.info("Hitting DefaultUserListMetadata API for Entity {}", entityName);
        ListRendererDefaultUserListMetaData metadataObj = new ListRendererDefaultUserListMetaData();
        metadataObj.hitListRendererDefaultUserListMetadata(listId);
        return metadataObj.getListRendererDefaultUserListMetaDataJsonStr();
    }

    public String getDefaultUserListMetadataResponse(int listId, Map<String, String> params) {
        logger.info("Hitting DefaultUserListMetadata API for List Id {}", listId);
        ListRendererDefaultUserListMetaData metadataObj = new ListRendererDefaultUserListMetaData();
        metadataObj.hitListRendererDefaultUserListMetadata(listId, params);
        return metadataObj.getListRendererDefaultUserListMetaDataJsonStr();
    }

    public String getFilterMetadataPropertyFromName(String defaultUserListMetaDataResponse, String filterMetadataName, String property) {
        logger.info("Getting Property [{}] using FilterMetadata Name [{}] from DefaultUserListMetadata API Response", property, filterMetadataName);

        JSONObject jsonObj = new JSONObject(defaultUserListMetaDataResponse);
        JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject fieldJsonObj = jsonArr.getJSONObject(i);

            if (fieldJsonObj.getString("name").trim().equalsIgnoreCase(filterMetadataName.trim())) {
                if (fieldJsonObj.has(property.trim()) && fieldJsonObj.get(property.trim()) != null) {
                    return fieldJsonObj.get(property.trim()).toString().trim();
                } else if (fieldJsonObj.get(property.trim()) == null) {
                    return null;
                } else {
                    logger.error("Couldn't find Property [{}] in FilterMetadata with Name [{}]", property, filterMetadataName);
                }
                break;
            }
        }

        logger.error("Couldn't find FilterMetadata with Name [{}]", filterMetadataName);
        return null;
    }

    public String getFilterMetadataPropertyFromQueryName(String defaultUserListMetaDataResponse, String filterMetadataQueryName, String property) {
        logger.info("Getting Property [{}] using FilterMetadata Query Name [{}] from DefaultUserListMetadata API Response", property, filterMetadataQueryName);

        JSONObject jsonObj = new JSONObject(defaultUserListMetaDataResponse);
        JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject fieldJsonObj = jsonArr.getJSONObject(i);

            if (fieldJsonObj.getString("queryName").trim().equalsIgnoreCase(filterMetadataQueryName.trim())) {
                if (fieldJsonObj.has(property.trim()) && fieldJsonObj.get(property.trim()) != null) {
                    return fieldJsonObj.get(property.trim()).toString().trim();
                } else if (fieldJsonObj.get(property.trim()) == null) {
                    return null;
                } else {
                    logger.error("Couldn't find Property [{}] in FilterMetadata with QueryName [{}]", property, filterMetadataQueryName);
                }
                break;
            }
        }

        logger.error("Couldn't find FilterMetadata with QueryName [{}]", filterMetadataQueryName);
        return null;
    }

    public String getFilterName(String defaultUserListMetaDataResponse, String queryName) {
        logger.info("Getting filter data name which is actually displayed on UI or Excel");

        JSONObject defaultUserListMetaDataResponseJson = new JSONObject(defaultUserListMetaDataResponse);
        JSONArray filterMetaDataArray = defaultUserListMetaDataResponseJson.getJSONArray("filterMetadatas");
        String filterName = null;
        for (int i = 0; i < filterMetaDataArray.length(); i++) {
            if (filterMetaDataArray.getJSONObject(i).get("queryName").equals(queryName)) {
                filterName = filterMetaDataArray.getJSONObject(i).get("name").toString();
            }
        }
        return filterName;
    }

    public String getColumnPropertyValueFromQueryName(String defaultUserListResponse, String columnQueryName, String propertyName) {
        logger.info("Getting Value of Property {} from Query Name {}", propertyName, columnQueryName);

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                String queryName = jsonArr.getJSONObject(i).getString("queryName");

                if (queryName.trim().equalsIgnoreCase(columnQueryName)) {
                    if (jsonArr.getJSONObject(i).has(propertyName)) {
                        return jsonArr.getJSONObject(i).get(propertyName).toString();
                    }

                    logger.info("Couldn't find Property {} for Column Query Name {}", propertyName, columnQueryName);
                    return null;
                }
            }
        } else {
            logger.error("DefaultUserList Metadata Response is an Invalid JSON.");
        }

        return null;
    }

    public String getColumnPropertyValueFromName(String defaultUserListResponse, String columnName, String propertyName) {
        logger.info("Getting Value of Property {} from Name {}", propertyName, columnName);

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                String queryName = jsonArr.getJSONObject(i).getString("name");

                if (queryName.trim().equalsIgnoreCase(columnName)) {
                    if (jsonArr.getJSONObject(i).has(propertyName)) {
                        return jsonArr.getJSONObject(i).get(propertyName).toString();
                    }

                    logger.info("Couldn't find Property {} for Column Name {}", propertyName, columnName);
                    return null;
                }
            }
        } else {
            logger.error("DefaultUserList Metadata Response is an Invalid JSON.");
        }

        return null;
    }

    public Boolean hasPermissionToPerformBulkAction(String entityName) {
        try {
            ListRendererDefaultUserListMetaData defaultListDataObj = new ListRendererDefaultUserListMetaData();
            int listId = ConfigureConstantFields.getListIdForEntity(entityName);
            defaultListDataObj.hitListRendererDefaultUserListMetadata(listId);

            String defaultListDataResponse = defaultListDataObj.getListRendererDefaultUserListMetaDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(defaultListDataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultListDataResponse);
                return jsonObj.getBoolean("bulkAction");
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Permission to Perform Bulk Action for Entity {}. {}", entityName, e.getStackTrace());
        }

        return false;
    }

    public Boolean hasColumnQueryName(String defaultUserListResponse, String columnQueryName) {
        logger.info("Checking if Column Query Name {} is present or not.", columnQueryName);

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                String queryName = jsonArr.getJSONObject(i).getString("queryName");

                if (queryName.trim().equalsIgnoreCase(columnQueryName)) {
                    return true;
                }
            }
        } else {
            logger.error("DefaultUserList Metadata Response is an Invalid JSON.");
        }

        return false;
    }

    public List<String> getAllColumnNames(String defaultUserListResponse) {
        List<String> allColumnNames = new ArrayList<>();

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                allColumnNames.add(jsonArr.getJSONObject(i).getString("name"));
            }
        }

        return allColumnNames;
    }

    public List<String> getAllColumnQueryNames(String defaultUserListResponse) {
        List<String> allColumnQueryNames = new ArrayList<>();

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                allColumnQueryNames.add(jsonArr.getJSONObject(i).getString("queryName"));
            }
        }

        return allColumnQueryNames;
    }

    public List<String> getAllFilterMetadataNames(String defaultUserListResponse) {
        List<String> allFilterMetadataNames = new ArrayList<>();

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

            for (int i = 0; i < jsonArr.length(); i++) {
                allFilterMetadataNames.add(jsonArr.getJSONObject(i).getString("name"));
            }
        }

        return allFilterMetadataNames;
    }

    public List<String> getAllEnabledColumnQueryNames(String defaultUserListResponse) {
        List<String> allEnabledColumnQueryNames = new ArrayList<>();

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (!jsonArr.getJSONObject(i).getBoolean("deleted")) {
                    allEnabledColumnQueryNames.add(jsonArr.getJSONObject(i).getString("queryName"));
                }
            }
        }

        return allEnabledColumnQueryNames;
    }

    public List<String> getAllEnabledFilterMetadataQueryNames(String defaultUserListResponse) {
        List<String> allEnabledFilterMetadataQueryNames = new ArrayList<>();

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (!jsonArr.getJSONObject(i).getBoolean("deleted")) {
                    allEnabledFilterMetadataQueryNames.add(jsonArr.getJSONObject(i).getString("queryName"));
                }
            }
        }

        return allEnabledFilterMetadataQueryNames;
    }

    public List<String> getAllTemplateTypeList(String defaultUserListResponse) {
        List<String> allTemplateTypesList = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse).getJSONObject("draftMetadata");
            JSONArray jsonArr = jsonObj.getJSONArray("templateTypeList");

            for (int i = 0; i < jsonArr.length(); i++) {
                allTemplateTypesList.add(jsonArr.getJSONObject(i).getString("name"));
            }
        } catch (Exception e) {
            return null;
        }

        return allTemplateTypesList;
    }
}