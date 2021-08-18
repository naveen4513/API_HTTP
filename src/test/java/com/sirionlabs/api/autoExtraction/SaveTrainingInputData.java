package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class SaveTrainingInputData {

    public static String getAPIPath() {
        return "/autoExtraction/saveTrainingInputData?algoUsed=1002";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getContentTypeAsJsonOnlyHeader();
    }

    public static String getPayload(String fieldId, String clientId, String active, String inputFile,String inputFileName,String tagName,String tagType) {
        return "{\"fieldId\": " + fieldId + ",\"clientId\": " + clientId + ",\"active\": " + active +
                ",\"inputFile\": " + inputFile + ",\"inputFileName\": " + inputFileName +
                ",\"tagName\": " + tagName  + ",\"tagType\": " + tagType + "}";
    }
}
