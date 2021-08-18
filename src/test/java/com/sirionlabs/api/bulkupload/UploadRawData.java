package com.sirionlabs.api.bulkupload;

import com.sirionlabs.api.file.ContractDocumentUpload;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UploadRawData extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(UploadBulkData.class);
    private String uploadRawDataJsonStr = null;

    public void hitUploadRawData(String filePath, String fileName, Map<String, String> payloadMap) {
        try {
            String queryString = "/sldetails/rawDataUpload";

            logger.debug("Query string url formed is {}", queryString);

            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
            HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

            File fileToUpload = new File(filePath + "/" + fileName);
            HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, payloadMap);
            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            this.uploadRawDataJsonStr = uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting UploadRawData Api. {}", e.getMessage());
        }
    }

    public void hitBulkUploadRawData(String filePath,String fileName, Map<String, String> payloadMap) {
        try {
            String queryString = "/slRawData/uploadBulk";

            logger.debug("Query string url formed is {}", queryString);

            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
            HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

            File fileToUpload = new File(filePath + "/" + fileName);
            HttpEntity entity = APIUtils.createMultipartEntityBuilder(fileToUpload, payloadMap);
            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            this.uploadRawDataJsonStr = uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting UploadRawData Api. {}", e.getMessage());
        }
    }

    public void hitBulkUploadRawDataDiffCsl(Map<String, String> textBodyMap, Map<String, File> fileToUpload) {
        try {
            String queryString = "/slRawData/v1/singleFormatBulkUpload";

            logger.debug("Query string url formed is {}", queryString);

            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
            HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

            APIUtils apiUtils = new APIUtils();
            HttpEntity entity = apiUtils.multiPartFormData(textBodyMap, fileToUpload);

            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            this.uploadRawDataJsonStr = uploadFileToServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting UploadRawData Api. {}", e.getMessage());
        }
    }

    public static String fileUpload(String filePath,String fileName) {
        Map<String, String> payloadMap = new HashMap<>();
        String response = "";
        try {

            payloadMap.put("key", RandomStringUtils.randomAlphabetic(18));
            payloadMap.put("name", fileName.split("\\.")[0]);
            payloadMap.put("extension", fileName.split("\\.")[1]);

            ContractDocumentUpload contractDocumentUpload = new ContractDocumentUpload();
            response=contractDocumentUpload.hitFileUploadForGlobal(filePath, fileName, payloadMap);

        } catch (Exception e) {
            logger.error("Exception while Uploading File",
                    filePath + "/" + fileName,e.getStackTrace());
        }
        return response;
    }

    public String uploadCLITemplate(String templateId,String payload) {
        HttpResponse response = null;
        String uploadResponse = "";
        try {
            HttpPost postRequest;
            String queryString = "/rawdata/metadata/template/" + templateId;

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            uploadResponse = EntityUtils.toString(response.getEntity());

            logger.info("Upload CLI response : [{}]",uploadResponse);

        } catch (Exception e) {
            logger.error("Exception while hitting Upload CLI Api. {}", e.getMessage());
        }
        return uploadResponse;
    }

    public String getUploadRawDataJsonStr() {
        return uploadRawDataJsonStr;
    }
}