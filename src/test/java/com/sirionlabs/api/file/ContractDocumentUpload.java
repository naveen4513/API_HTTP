package com.sirionlabs.api.file;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class ContractDocumentUpload extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(ContractDocumentUpload.class);

    public String hitFileUpload(String filePath, String fileName, Map<String, String> payloadMap) {
        String uploadResponse = null;

        try {
            String queryString = "/file/upload/contractDocument";
            logger.debug("Query string url formed is {}", queryString);

            String acceptHeader = "application/json, text/plain, */*";

            HttpPost postRequest = super.generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");
            File fileToUpload = new File(filePath + "/" + fileName);
            HttpEntity entity = APIUtils.createMultipartEntityBuilder("documentFileData", fileToUpload, payloadMap);
            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            uploadResponse = uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting File Upload Api. {}", e.getMessage());
        }
        return uploadResponse;
    }

    public String hitFileUploadForGlobal(String filePath, String fileName, Map<String, String> payloadMap) {
        String uploadResponse = null;

        try {
            String queryString = "/file/upload";
            logger.debug("Query string url formed is {}", queryString);

            String acceptHeader = "application/json, text/plain, */*";

            HttpPost postRequest = super.generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");
            File fileToUpload = new File(filePath + "/" + fileName);
            HttpEntity entity = APIUtils.createMultipartEntityBuilder("documentFileData", fileToUpload, payloadMap);
            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            uploadResponse = uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting File Upload Api. {}", e.getMessage());
        }
        return uploadResponse;
    }

    public String hitFileUploadForGlobalDRS(String filePath, String fileName, Map<String, String> payloadMap) {
        String uploadResponse = null;

        try {
            String queryString = "/file/v1/upload";
            logger.debug("Query string url formed is {}", queryString);

            String acceptHeader = "application/json, text/plain, */*";

            HttpPost postRequest = super.generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");
            File fileToUpload = new File(filePath + "/" + fileName);
            HttpEntity entity = APIUtils.createMultipartEntityBuilder("documentFileData", fileToUpload, payloadMap);
            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            uploadResponse = uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting File Upload Api. {}", e.getMessage());
        }
        return uploadResponse;
    }
}
