package com.sirionlabs.test;

import com.sirionlabs.api.file.ContractDocumentUpload;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

public class TestContractDocumentUpload {

    private final static Logger logger = LoggerFactory.getLogger(TestContractDocumentUpload.class);

    public static Map<String,String> uploadedContractDocumentData(String filePath,String fileName,String relationId) {
        Map<String, String> payloadMap = new HashMap<>();
        try {
           String response = null;
            payloadMap.put("key", RandomStringUtils.randomAlphabetic(18));
            payloadMap.put("name", fileName.split("\\.")[0]);
            payloadMap.put("extension", fileName.split("\\.")[1]);
            payloadMap.put("relationId", relationId);

            ContractDocumentUpload contractDocumentUpload = new ContractDocumentUpload();
            response=contractDocumentUpload.hitFileUpload(filePath, fileName, payloadMap);
            payloadMap.put("totalPages",response.split("=")[1]);
        } catch (Exception e) {
            logger.error("Exception while Uploading File",
                    filePath + "/" + fileName,e.getStackTrace());
        }
        return payloadMap;
    }

    public static Map<String,String> fileUpload(String filePath,String fileName) {
        Map<String, String> payloadMap = new HashMap<>();
        try {
            String response = null;
            payloadMap.put("key", RandomStringUtils.randomAlphabetic(18));
            payloadMap.put("name", fileName.split("\\.")[0]);
            payloadMap.put("extension", fileName.split("\\.")[1]);

            ContractDocumentUpload contractDocumentUpload = new ContractDocumentUpload();
            response=contractDocumentUpload.hitFileUploadForGlobal(filePath, fileName, payloadMap);
            payloadMap.put("filePathOnServer",response.trim());
        } catch (Exception e) {
            logger.error("Exception while Uploading File",
                    filePath + "/" + fileName,e.getStackTrace());
        }
        return payloadMap;
    }
    public static Map<String,String> fileUploadForDRS(String filePath,String fileName) {
        Map<String, String> payloadMap = new HashMap<>();
        try {
            String response = null;
            payloadMap.put("key", RandomStringUtils.randomAlphabetic(18));
            payloadMap.put("name", fileName.split("\\.")[0]);
            payloadMap.put("extension", fileName.split("\\.")[1]);

            ContractDocumentUpload contractDocumentUpload = new ContractDocumentUpload();
            response=contractDocumentUpload.hitFileUploadForGlobalDRS(filePath, fileName, payloadMap);
            payloadMap.put("filePathOnServer",response.trim());
        } catch (Exception e) {
            logger.error("Exception while Uploading File",
                    filePath + "/" + fileName,e.getStackTrace());
        }
        return payloadMap;
    }
}
