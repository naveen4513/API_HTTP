package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.TestContractDocumentUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DocumentUploadHelper extends TestAPIBase {
    public static Logger logger = LoggerFactory.getLogger(DocumentUploadHelper.class);
    //API to Upload a Document in AE Listing
    public static String getAPIPath()
    {
        return globalUploadAPI.getApiPath();
    }
    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public static String getGlobalUploadAPIPayload(int newlyCreatedProjectId)
    {
        String payload = null;
        try{
            // File Upload API to get the key of file uploaded
            logger.info("File Upload API to get the key of file that has been uploaded");
            String templateFilePath ="src/test/resources/TestConfig/AutoExtraction/UploadFiles";
            String templateFileName="KAMADALTD_DRSADraftR_3252013.docx";
            Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);
            logger.info("Hit Global Upload API");
            payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":["+newlyCreatedProjectId+"]}]";
        }
        catch (Exception e)
        {
            logger.error("File Upload API is not working because of :" + e.getMessage());

        }
        return payload;
    }
    public static APIResponse documentUploadAPIResponse(String apiPath, HashMap<String, String> headers, String payload) {
        return executor.post(apiPath, headers, payload).getResponse();
    }
}
