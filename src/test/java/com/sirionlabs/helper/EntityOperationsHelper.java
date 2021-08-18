package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityOperationsHelper {

    private final static Logger logger = LoggerFactory.getLogger(EntityOperationsHelper.class);

    public static Boolean restoreRecord(String entityName, int entityId, String originalPayload) {
        boolean recordRestored = false;
        try {
            logger.info("Restoring Record of Entity {} having Id {}", entityName, entityId);
            String payload = "{\"body\": {\"data\":";
            JSONObject jsonObj = new JSONObject(originalPayload);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

            logger.info("Removing Options from the Original Payload");
            JSONArray allObjectNames = jsonObj.names();
            for (int i = 0; i < allObjectNames.length(); i++) {
                String temp = jsonObj.get(allObjectNames.get(i).toString()).toString();

                if (temp.startsWith("{") && jsonObj.getJSONObject(allObjectNames.get(i).toString()).has("options")) {
                    JSONObject tempObj = jsonObj.getJSONObject(allObjectNames.get(i).toString());
                    tempObj.remove("options");
                    jsonObj.put(allObjectNames.get(i).toString(), tempObj);
                }
            }

            payload += jsonObj.toString() + "}}";
            logger.info("Hitting Edit Post API to Restore Record of Entity {} having Id {}", entityName, entityId);
            Edit editObj = new Edit();
            String editResponseStr = editObj.hitEdit(entityName, payload);

            if (ParseJsonResponse.validJsonResponse(editResponseStr)) {
                jsonObj = new JSONObject(editResponseStr);
                String editStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

                if (!editStatus.trim().equalsIgnoreCase("success")) {
                    logger.error("Record of Entity {}, Id {} couldn't be restored due to {}.", entityName, entityId, editStatus);
                } else {
                    recordRestored = true;
                    logger.info("Record of Entity {} having Id {} restored Successfully.", entityName, entityId);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while trying to Restore Record having Id {} of Entity {} to original state. {}", entityId, entityName, e.getStackTrace());
            return false;
        }
        return recordRestored;
    }

    public static Boolean deleteEntityRecord(String entityName, int entityId) {
        String urlName = getUrlNameOfEntity(entityName);
        return deleteEntityRecord(entityName, entityId, urlName);
    }

    public static Boolean deleteEntityRecord(String entityName, int entityId, String urlName) {
        boolean entityDeleted = false;
        try {
            logger.info("Hitting Show API for Entity {} having Id {}.", entityName, entityId);
            Show showObj = new Show();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            showObj.hitShow(entityTypeId, entityId);
            if (ParseJsonResponse.validJsonResponse(showObj.getShowJsonStr())) {
                String deletePayload = getPayloadForDeletingRecord(showObj.getShowJsonStr());

                logger.info("Deleting Entity {} having Id {}.", entityName, entityId);
                Delete deleteObj = new Delete();
                deleteObj.hitDelete(entityName, deletePayload, urlName);
                String deleteJsonStr = deleteObj.getDeleteJsonStr();
                JSONObject jsonObj = new JSONObject(deleteJsonStr);
                String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
                if (status.trim().equalsIgnoreCase("success")) {
                    entityDeleted = true;
                }

                if (!entityDeleted && entityName.equalsIgnoreCase("suppliers")) {
                    //Delete supplier data from DB.
                    PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
                    postgresObj.deleteDBEntry("delete from relation where id = " + entityId);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while deleting Entity {} having Id {}. {}", entityName, entityId, e.getStackTrace());
        }
        return entityDeleted;
    }

    public static String getDeleteEntityResponse(String entityName, int entityId) {
        String urlName = getUrlNameOfEntity(entityName);
        return getDeleteEntityResponse(entityName, entityId, urlName);
    }

    public static String getDeleteEntityResponse(String entityName, int entityId, String urlName) {
        String deleteResponse = null;

        try {
            logger.info("Hitting Show API for Entity {} having Id {}.", entityName, entityId);
            Show showObj = new Show();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            showObj.hitShow(entityTypeId, entityId);
            if (ParseJsonResponse.validJsonResponse(showObj.getShowJsonStr())) {
                String deletePayload = getPayloadForDeletingRecord(showObj.getShowJsonStr());

                logger.info("Deleting Entity {} having Id {}.", entityName, entityId);
                Delete deleteObj = new Delete();
                deleteObj.hitDelete(entityName, deletePayload, urlName);
                deleteResponse = deleteObj.getDeleteJsonStr();
            }
        } catch (Exception e) {
            logger.error("Exception while deleting Entity {} having Id {}. {}", entityName, entityId, e.getStackTrace());
        }
        return deleteResponse;
    }

    public static String getPayloadForDeletingRecord(String showResponse) {
        String payload = null;

        try {
            JSONObject jsonObj = new JSONObject(showResponse);
            String prefix = "{\"body\":{\"data\":";
            String suffix = "}}";
            String showBodyStr = jsonObj.getJSONObject("body").getJSONObject("data").toString();
            payload = prefix + showBodyStr + suffix;
        } catch (Exception e) {
            logger.error("Exception while getting Payload for Deleting Record. {}", e.getMessage());
        }
        return payload;
    }

    public static void deleteMultipleRecords(String entityName, List<Integer> entityIdsList) {
        try {
            String urlName = getUrlNameOfEntity(entityName);

            for (Integer entityId : entityIdsList) {
                Boolean result = deleteEntityRecord(entityName, entityId, urlName);
                if(!result){
                    logger.error("Unable to delete entity" + entityId);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while deleting Multiple Records for Entity {}. {}", entityName, e.getStackTrace());
        }
    }

    /*
    cloneRecord method clones a record and returns the id of newly created record. If it fails to create new record then it returns -1.
     */
    public static Integer cloneRecord(String entityName, int entityId) {
        return cloneRecord(entityName, entityId, null);
    }

    public static Integer cloneRecord(String entityName, int entityId, String version) {
        Integer newRecordId = -1;
        try {
            logger.info("Hitting Clone API for Entity {} and Id {}", entityName, entityId);
            Clone cloneObj = new Clone();
            String cloneResponse = cloneObj.hitClone(entityName, entityId, version);
            String cloneStatus = ParseJsonResponse.getStatusFromResponse(cloneResponse);

            if (cloneStatus != null && cloneStatus.equalsIgnoreCase("success")) {
                String createPayload = PayloadUtils.removeOptionsFromPayload(cloneResponse);

                logger.info("Creating new record from Entity {} and Id {}", entityName, entityId);
                String createResponse = CreateEntity.create(entityName, createPayload);
                String createStatus = ParseJsonResponse.getStatusFromResponse(createResponse);

                if (createStatus != null && createStatus.equalsIgnoreCase("success")) {
                    logger.info("New Record Cloned Successfully from Entity {} and Id {}", entityName, entityId);
                    newRecordId = CreateEntity.getNewEntityId(createResponse, entityName);
                    logger.info("Id of cloned Record is {}", newRecordId);
                } else {
                    logger.error("Couldn't Clone Record From Entity {} and Id {}. Create API Response is [{}]", entityName, entityId, createStatus);
                }
            } else {
                logger.error("Couldn't Clone Record having Id {} of Entity {}. Clone API Response is [{}]", entityId, entityName, cloneStatus);
            }
        } catch (Exception e) {
            logger.error("Exception while cloning record having Id {} of Entity {}. {}", entityId, entityName, e.getStackTrace());
        }
        return newRecordId;
    }

    public static List<Integer> cloneMultipleRecords(String entityName, List<Integer> entityIdsList) {
        List<Integer> newRecordsIdList = new ArrayList<>();

        try {
            for (Integer entityId : entityIdsList) {
                logger.info("Cloning Record having Id {} of Entity {}", entityId, entityName);

                Integer newRecordId = cloneRecord(entityName, entityId);
                if (newRecordId != -1) {
                    newRecordsIdList.add(newRecordId);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while cloning Multiple Records for Entity {}. {}", entityName, e.getStackTrace());
        }
        return newRecordsIdList;
    }

    public static String getUrlNameOfEntity(String entityName) {
        String urlName = null;
        try {
            urlName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath"),
                    ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "url_name");
        } catch (Exception e) {
            logger.error("Exception while getting Url Name of Entity {}. {}", entityName, e.getStackTrace());
        }
        return urlName;
    }

    public static String createPayloadForEditPost(String editGetJsonStr, Map<String, String> fieldsPayloadMap) {
        String payload;

        try {
            payload = "{\"body\":{\"data\":";
            JSONObject jsonObj = new JSONObject(editGetJsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

            List<String> fieldNames = new ArrayList<>(fieldsPayloadMap.keySet());

            for (String field : fieldNames) {
                jsonObj = jsonObj.put(field.trim(), new JSONObject(fieldsPayloadMap.get(field).trim()));
            }

            payload += jsonObj.toString();
            payload += "}}";
        } catch (Exception e) {
            logger.error("Exception while Creating Edit Post Payload. {}", e.getMessage());
            return null;
        }
        return payload;
    }
}
