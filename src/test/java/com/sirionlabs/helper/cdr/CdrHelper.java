package com.sirionlabs.helper.cdr;

import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CdrHelper {


    private final static Logger logger = LoggerFactory.getLogger(CdrHelper.class);

    public String uploadDocument(int cdrId, String filePath,String fileName, String randomKeyForFileUpload) {
        FileUploadDraft fileUploadDraft = new FileUploadDraft();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("name", fileName.split("\\.")[0]);
        queryParameters.put("extension", fileName.split("\\.")[1]);
        queryParameters.put("entityTypeId", "160");
        queryParameters.put("entityId", String.valueOf(cdrId));
        queryParameters.put("key", randomKeyForFileUpload);
        queryParameters.put("documentFileData", ("binary"));

        return fileUploadDraft.hitFileUpload(filePath, fileName, queryParameters);
    }

    public String getPayloadForSubmitDraft(int cdrId, String documentFileId, int documentStatusId, String documentKey) {
        String payload = null;

        try {
            String showPageResponse = ShowHelper.getShowResponseVersion2(160, cdrId);

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                JSONObject commentJsonObj = jsonData.getJSONObject("comment");
                JSONObject draftObj = commentJsonObj.getJSONObject("draft");
                draftObj.put("values", true);
                commentJsonObj.put("draft", draftObj);

                String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":" + documentFileId +
                        ",\"documentTags\":[],\"documentSize\":26,\"key\":\"" + documentKey + "\",\"documentStatusId\":" + documentStatusId +
                        ",\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false," +
                        "\"shareWithSupplierFlag\":false}]}";
                commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

                jsonData.put("comment", commentJsonObj);

                JSONObject finalPayload = new JSONObject();
                JSONObject body = new JSONObject();
                body.put("data", jsonData);
                finalPayload.put("body", body);

                payload = finalPayload.toString();
            }
        } catch (Exception e) {
            logger.error("Exception while getting payload for Submit draft. error : {}", e.getMessage());
        }

        return payload;
    }

    public String getPayloadForSubmitFinal(int cdrId, String documentFileId, String documentKey) {
        String payload = null;

        try {
            String showPageResponse = ShowHelper.getShowResponseVersion2(160, cdrId);

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                JSONObject commentJsonObj = jsonData.getJSONObject("comment");
                JSONObject draftObj = commentJsonObj.getJSONObject("draft");
                draftObj.put("values", true);
                commentJsonObj.put("draft", draftObj);

                String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":" + documentFileId +
                        ",\"documentTags\":[],\"documentSize\":26,\"key\":\"" + documentKey + "\",\"documentStatusId\":2," +
                        "\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false," +
                        "\"shareWithSupplierFlag\":false}]}";
                commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

                jsonData.put("comment", commentJsonObj);

                JSONObject finalPayload = new JSONObject();
                JSONObject body = new JSONObject();
                body.put("data", jsonData);
                finalPayload.put("body", body);

                payload = finalPayload.toString();
            }
        } catch (Exception e) {
            logger.error("Exception while getting payload for Submit draft. error : {}", e.getMessage());
        }

        return payload;
    }

}
