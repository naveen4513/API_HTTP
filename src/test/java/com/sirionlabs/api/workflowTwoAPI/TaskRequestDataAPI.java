package com.sirionlabs.api.workflowTwoAPI;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TaskRequestDataAPI {
    public static String domain = "";


    private static final String TaskRequestDataFilePath;
    private static final String TaskRequestDataFileName;

    private static Map<String, String> map;

    static {
        TaskRequestDataFilePath = ConfigureConstantFields.getConstantFieldsProperty("TaskRequestDataFilePath");
        TaskRequestDataFileName = ConfigureConstantFields.getConstantFieldsProperty("TaskRequestDataFileName");
    }

    public String getPayloadTaskRequestData(String taskId, String clientId, String entityTypeId, String action) {

        return "{\n" +
                "    \"taskIds\": [\n" +
                taskId + "\n" +
                "    ],\n" +
                "    \"action\": \"" + action + "\",\n" +
                "    \"globalInfo\": {\n" +
                "        \"entityTypeId\":" + entityTypeId + ",\n" +
                "        \"clientId\":" + clientId + ",\n" +
                "        \"userId\": 1250,\n" +
                "        \"languageId\": 1,\n" +
                "        \"sirionBaseUrl\": \"http://qa.dev.com:8080\"\n" +
                "    }\n" +
                "}\n" +
                "\n";
    }

    public static String generateToken(String secretKey, String issuer, int expiryTimeMin) {
        String token = "";
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        token = JWT.create()
                .withIssuer(issuer)
                .withExpiresAt(new Date(System.currentTimeMillis() + (expiryTimeMin * 60 * 1000)))
                .sign(algorithm);
        return token;
    }

    public APIValidator hitPostTaskRequestDataAPICall(APIExecutor executor, String domain, String payload, String authToken, String isAuth) {
        HashMap<String, String> headers = new HashMap<>();
        if (isAuth.equalsIgnoreCase("yes")) {
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", authToken);
        }
        String queryString = "/workflow-v2/task-request-data";
        return executor.postWithoutMandatoryHeaders(domain, queryString, headers, payload, null);
    }

    public static Map<String, String> getAllConfigForTaskRequesrDataAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(TaskRequestDataFilePath, TaskRequestDataFileName, section_name);
        return map;
    }
}
