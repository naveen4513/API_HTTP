package com.sirionlabs.api.workflowTwoAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NodeDataDetailsAPI {

    private static final String NodeDataDetailsFilePath;
    private static final String NodeDataDetailsFileName;

    private static Map<String, String> map;

    static {
        NodeDataDetailsFilePath = ConfigureConstantFields.getConstantFieldsProperty("NodeDataDetailsFilePath");
        NodeDataDetailsFileName = ConfigureConstantFields.getConstantFieldsProperty("NodeDataDetailsFileName");
    }

    public String getPayloadNodeDataDetails(String nodeId, String clientId, String entityTypeId, String action) {

        return "{\n" +
                "    \"action\": \"" + action + "\",\n" +
                "    \"nodeIds\": [\n" +
                nodeId     +"\n" +
                "    ],\n" +
                "\n" +
                "    \"globalInfo\": {\n" +
                "        \"entityTypeId\":" + entityTypeId + ",\n" +
                "        \"clientId\":" + clientId + ",\n" +
                "        \"userId\": 1250,\n" +
                "        \"languageId\": 1,\n" +
                "        \"sirionBaseUrl\": \"http://qavf.rc.office/\"\n" +
                "    }\n" +
                "}\n";
    }

    public APIValidator hitPostNodeDataDetailsAPICall(APIExecutor executor, String domain, String payload, String authToken, String isAuth) {
        HashMap<String, String> headers = new HashMap<>();
        if (isAuth.equalsIgnoreCase("yes")) {
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", authToken);
        }
        String queryString = "/workflow-v2/node-details";
        return executor.postWithoutMandatoryHeaders(domain, queryString, headers, payload, null);
    }

    public static int[] stringToIntArray(String inputString){
       return Arrays.stream(inputString.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    public static Map<String, String> getAllConfigForNodeDataDetailsAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(NodeDataDetailsFilePath, NodeDataDetailsFileName, section_name);
        return map;
    }
}
