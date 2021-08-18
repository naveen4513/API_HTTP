package com.sirionlabs.api.auditLogs;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuditLog  extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(AuditLog.class);

    private  String getAuditLogPath() {
        return "/listRenderer/list/61/tablistdata/";
    }

    public String hitAuditLogDataApi(String entityTypeId , String entityId, String payload){

        String response;
        String queryString = getAuditLogPath()+entityTypeId+"/"+entityId+"?version=2.0";
        response = executor.post(queryString, getHeaders(), payload).getResponse().getResponseBody();
        return response;

    }

    public APIResponse hitAuditLogDataApi(String entityTypeId , String entityId, int offset, int size, String orderByColumnName, String orderDirection){
        String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
                offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
                "\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";
        APIResponse response;
        String queryString = getAuditLogPath()+entityTypeId+"/"+entityId+"?version=2.0";
        response = executor.post(queryString, getHeaders(), listDataPayload).getResponse();
        return response;

    }



    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public String getAuditLogLatestAction(int entityTypeId,int entityId, CustomAssert customAssert){

        String latestAction = "";

        AuditLog auditLog = new AuditLog();

        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String auditLogResponse = auditLog.hitAuditLogDataApi(String.valueOf(entityTypeId), String.valueOf(entityId),payload);

            JSONObject auditLogResponseJson = new JSONObject(auditLogResponse);
            JSONObject auditLogFirstRowJson = auditLogResponseJson.getJSONArray("data").getJSONObject(0);
            Iterator<String> keys = auditLogFirstRowJson.keys();

            while (keys.hasNext()){
                String key = keys.next();

                String columnName = auditLogFirstRowJson.getJSONObject(key).get("columnName").toString();

                if(columnName.equals("action_name")){
                    latestAction = auditLogFirstRowJson.getJSONObject(key).get("value").toString();
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting latest action from audit log tab");
        }
        return latestAction;
    }

    public Map<String,String> getAuditLogMap(int entityTypeId, int entityId, CustomAssert customAssert){

        AuditLog auditLog = new AuditLog();
        Map<String,String> auditLogMap = new HashMap<>();

        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String auditLogResponse = auditLog.hitAuditLogDataApi(String.valueOf(entityTypeId), String.valueOf(entityId),payload);

            JSONObject auditLogResponseJson = new JSONObject(auditLogResponse);
            JSONObject auditLogFirstRowJson = auditLogResponseJson.getJSONArray("data").getJSONObject(0);
            Iterator<String> keys = auditLogFirstRowJson.keys();

            while (keys.hasNext()){
                String key = keys.next();

                String columnName = auditLogFirstRowJson.getJSONObject(key).get("columnName").toString();
                String columnValue = auditLogFirstRowJson.getJSONObject(key).get("value").toString();

                auditLogMap.put(columnName,columnValue);

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting latest action from audit log tab");
        }

        return auditLogMap;
    }

}
