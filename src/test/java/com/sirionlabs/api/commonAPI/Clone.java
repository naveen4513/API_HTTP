package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Clone extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(Clone.class);

    public String hitClone(String entityName, int entityId) {
		return hitClone(entityName, entityId, null);
    }

    public String hitClone(String entityName, int entityId, String version) {
        String cloneResponseStr = null;
        try {
            HttpGet getRequest;

            String entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
            String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            String urlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingFilePath, entityIdMappingFileName, entityName, "url_name");

            String queryString = "/" + urlName + "/clone/" + entityId;
            if (version != null) {
                queryString = queryString.concat("?version=" + version);
            }

            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            cloneResponseStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clone response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Clone Api for Entity {} and Id {}. {}", entityName, entityId, e.getStackTrace());
        }
        return cloneResponseStr;
    }

    public String hitCloneV2(String entityName, int entityId) {
        String cloneResponseStr = null;
        try {
            HttpGet getRequest;

            String entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
            String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            String urlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingFilePath, entityIdMappingFileName, entityName, "url_name");

            String queryString = "/" + urlName + "/clone/" + entityId + "?version=2.0";
            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            cloneResponseStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clone response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Clone Api for Entity {} and Id {}. {}", entityName, entityId, e.getStackTrace());
        }
        return cloneResponseStr;
    }

    public int createEntityFromClone(String entityName,int entityId){

        int newEntity = -1;
        String cloneResponse;
        String createPayload = null;
        cloneResponse = hitCloneV2(entityName,entityId);
        try {
            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            cloneResponseJson.remove("header");
            cloneResponseJson.remove("session");
            cloneResponseJson.remove("actions");
            cloneResponseJson.remove("createLinks");
            cloneResponseJson.getJSONObject("body").remove("layoutInfo");
            cloneResponseJson.getJSONObject("body").remove("globalData");
            cloneResponseJson.getJSONObject("body").remove("errors");

            createPayload = cloneResponseJson.toString();

            Create create = new Create();
            create.hitCreate(entityName, createPayload);
            String createResponse = create.getCreateJsonStr();

            //new Entity
            newEntity = CreateEntity.getNewEntityId(createResponse, entityName);

        }catch (Exception e){
            logger.error("Exception while creating payload for Create for " + entityName + " ID " + entityId);
        }

        return newEntity;
    }

}
