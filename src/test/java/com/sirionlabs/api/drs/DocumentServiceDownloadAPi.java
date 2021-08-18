package com.sirionlabs.api.drs;


import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DocumentServiceDownloadAPi extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(DocumentServiceDownloadAPi.class);

   private  String queryString;

   public HttpResponse hitDownloadDRS(HttpHost hostUrl,String outputFilePath, String outputFileName, String clientId, String documentId){
       HttpResponse response = null;
       try {
           HttpGet getRequest;
           Map<String, String> queryStringParams = getFormParam(clientId, documentId);
           queryString = "/drs/document/v1/download";
           logger.debug("Query string url formed is {}", queryString);
           if (queryStringParams != null) {
               String urlParams = UrlEncodedString.getUrlEncodedString(queryStringParams);
               queryString += "?" + urlParams;

           }
           getRequest = new HttpGet(queryString);
       //    getRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
           getRequest.addHeader("Accept", "*/*" );
           getRequest.addHeader("Accept-Encoding", "gzip, deflate");

           response = APIUtils.getRequest(getRequest,hostUrl,false);
           logger.debug("Response status is {}", response.getStatusLine().toString());

       }catch (Exception e) {
           logger.error("Exception while hitting DownloadAuditLog Api. {}", e.getMessage());
       }

       if (response.getStatusLine().toString().contains("200")) {


           dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, outputFileName, documentId);

       }
       return response;
   }

    public HttpResponse hitDownloadDRSWithInvalidPath(HttpHost hostUrl,String outputFilePath, String outputFileName, String clientId, String documentId){
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            Map<String, String> queryStringParams = getFormParam(clientId, documentId);
            queryString = "/drs/document/v1/downloadtest";
            logger.debug("Query string url formed is {}", queryString);
            if (queryStringParams != null) {
                String urlParams = UrlEncodedString.getUrlEncodedString(queryStringParams);
                queryString += "?" + urlParams;

            }
            getRequest = new HttpGet(queryString);
            //    getRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            getRequest.addHeader("Accept", "*/*" );
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest,hostUrl,false);
            logger.debug("Response status is {}", response.getStatusLine().toString());

        }catch (Exception e) {
            logger.error("Exception while hitting DownloadAuditLog Api. {}", e.getMessage());
        }

        return response;
    }




   public Map<String, String> getFormParam(String clientId, String documentId){
       Map<String, String> formParam = new HashMap<String, String>();
       if(clientId!="" && clientId!=null) formParam.put("clientId",clientId);
       if(documentId!="" && documentId!=null) formParam.put("documentId",documentId);
       return formParam;

   }

    private void dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String fileName , String documentId) {
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, documentId);
        if (isFolderSuccessfullyCreated ) {
            outputFile = outputFilePath + "/" + documentId + "/" + fileName;
            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status)
                logger.info("file generated at {}", outputFile);
        }
    }



}
