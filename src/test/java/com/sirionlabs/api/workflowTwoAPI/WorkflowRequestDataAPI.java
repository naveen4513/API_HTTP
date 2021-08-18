package com.sirionlabs.api.workflowTwoAPI;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WorkflowRequestDataAPI {
    public static String domain = "";


    private static final String WorkflowRequestDataFilePath;
    private static final String WorkflowRequestDataFileName;

    private static Map<String, String> map;

    static {
        WorkflowRequestDataFilePath = ConfigureConstantFields.getConstantFieldsProperty("WorkflowRequestDataFilePath");
        WorkflowRequestDataFileName = ConfigureConstantFields.getConstantFieldsProperty("WorkflowRequestDataFileName");
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

    public String getPayloadWorkflowRequestData(String workflowId, String clientId, String entityTypeId) {

        return "{\n" +
                "    \"workflowId\":" + workflowId + ",\n" +
                "    \"globalInfo\": {\n" +
                "        \"entityTypeId\":" + entityTypeId + ",\n" +
                "        \"clientId\":" + clientId + ",\n" +
                "        \"userId\": 1250,\n" +
                "        \"languageId\": 1,\n" +
                "        \"sirionBaseUrl\": \"http://qavf.rc.office\"\n" +
                "    }\n" +
                "}";
    }

    public APIValidator hitPostWorkflowRequestDataAPICall(APIExecutor executor, String domain, String payload, String authToken, String isAuth) {
        HashMap<String, String> headers = new HashMap<>();
        if (isAuth.equalsIgnoreCase("yes")) {
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", authToken);
        }
        String queryString = "/workflow-v2/workflow-data";
        return executor.postWithoutMandatoryHeaders(domain, queryString, headers, payload, null);
    }

    public String removeKeyFromBody(String payload, String jsonPathForKeyToBeRemoved){
        DocumentContext doc = JsonPath.parse(payload);
        doc.delete(jsonPathForKeyToBeRemoved);
        return doc.jsonString();
    }

    public static Map<String, String> getAllConfigForWorkflowRequestDataAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(WorkflowRequestDataFilePath, WorkflowRequestDataFileName, section_name);
        return map;
    }

}
