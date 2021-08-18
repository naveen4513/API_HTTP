package com.sirionlabs.api.clientAdmin.invoiceCopy;

import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UploadInvoiceCopy extends APIUtils{


    private final static Logger logger = LoggerFactory.getLogger(UploadInvoiceCopy.class);
    public String uploadFileJsonStr = null;

    public void hitUploadData(String templateId,String filePath, String fileName, Map<String, File> payloadMap) {
        try {
            String queryString = "/invoice-templates/" + templateId + "/logo";

            logger.debug("Query string url formed is {}", queryString);

            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
            HttpPost postRequest = generateHttpPostRequestWithQueryString(queryString, acceptHeader, "");

            File fileToUpload = new File(filePath + "/" + fileName);
            Map<String,File> fileToUploadMap = new HashMap<>();
            fileToUploadMap.put("logo",fileToUpload);

            Map<String,String> textBodyMap = new HashMap<>();

            HttpEntity entity = multiPartFormData(textBodyMap, fileToUploadMap);
            postRequest.setEntity(entity);

            HttpHost target = generateHttpTargetHost();
            this.uploadFileJsonStr = uploadFileToServer(target, postRequest);

            System.out.println();
        } catch (Exception e) {
            logger.error("Exception while hitting UploadRawData Api. {}", e.getMessage());
        }
    }

    public Boolean hitDownload(String outputFilePath, String outputFileName, String templateId) {

        Boolean fileDownloaded = false;
        String queryString = "/invoice-templates/" + templateId + "/logo";
        try {

            HttpHost target = super.generateHttpTargetHost();
            logger.debug("Query string url formed is {}", queryString);
            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";

            HttpGet getRequest = super.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
            fileDownloaded = super.downloadAPIResponseFile(outputFilePath, outputFileName, target, getRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting BulkUpload Download Api using QueryString [{}]. {}", queryString, e.getStackTrace());
        }
        return fileDownloaded;
    }

}
