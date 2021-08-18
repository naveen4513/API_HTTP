package com.sirionlabs.helper.auditlog;


import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DownloadAuditLog extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(DownloadAuditLog.class);

   private  String queryString;
   private String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");


   public int HitDownloadAuditLog(String entityId,String entityTypeId,boolean tabList, String clientEntitySeqId,Map<String, String> formParam,String outputFilePath, String outputFileName){
       HttpResponse response = null;
       try {
           HttpPost postRequest;
           queryString = "/listRenderer/download/61/data?entityId=" + entityId +
                   "&entityTypeId=" + entityTypeId + "&tabList=" + tabList + "&clientEntitySeqId=" + clientEntitySeqId;
           logger.debug("Query string url formed is {}", queryString);
           postRequest = new HttpPost(queryString);
           String params = UrlEncodedString.getUrlEncodedString(formParam);
           postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
           postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
           postRequest.addHeader("Accept-Encoding", "gzip, deflate");

           response = super.postRequest(postRequest, params);
           logger.debug("Response status is {}", response.getStatusLine().toString());

       }catch (Exception e) {
           logger.error("Exception while hitting DownloadAuditLog Api. {}", e.getMessage());
       }

       if (response.getStatusLine().toString().contains("200")) {

           String entityName = ConfigureConstantFields.getEntityNameById(Integer.valueOf(entityTypeId));

         dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "auditLog", entityName, outputFileName);

       }
       return response.getStatusLine().getStatusCode();
   }


   public Map<String, String> getFormParam(String jsonData){
       Map<String, String> formParam = new HashMap<String, String>();
       System.out.println("json for downloading auditLog : "+jsonData);
       formParam.put("jsonData", jsonData);
       formParam.put("_csrf_token", csrfToken);

       return formParam;

   }

    private void dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String fileName) {
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + fileName;
            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status)
                logger.info("DownloadAuditLog file generated at {}", outputFile);
        }
    }



}
