package com.sirionlabs.api.microservices.Email;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OrchestratorMicroService {

    public static String domain = "";


    private static final String MicroserviceConfigFilePath;
    private static final String MicroserviceConfigFileName;
    private static final String DeleteCallConfigFileName;
    private static final String DeleteCallConfigFilePath;
    private static final String FetchClientDeletionDataFileName;
    private static final String FetchClientDeletionDataFilePath;
    private static Map<String, String> map;

    static {
        MicroserviceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("OrchestratorMicroServiceConfigFilePath");
        MicroserviceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("OrchestratorMicroServiceConfigFileName");
        DeleteCallConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DeleteCallConfigFilePath");
        DeleteCallConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DeleteCallConfigFileName");
        FetchClientDeletionDataFileName = ConfigureConstantFields.getConstantFieldsProperty("FetchClientDeletionDataFileName");
        FetchClientDeletionDataFilePath = ConfigureConstantFields.getConstantFieldsProperty("FetchClientDeletionDataFilePath");
    }

    public APIValidator hitPostClientDataDeletion(APIExecutor executor, String domain, String payload, String authToken, String isAuth) {
        HashMap<String, String> headers = new HashMap<>();
        if (isAuth.equalsIgnoreCase("yes") || isAuth.equalsIgnoreCase("invalid")) {
            headers = ApiHeaders.getEmailDefaultHeaders();
            headers.put("Authorization", authToken);
        }

        String queryString = "/client-data-deletion/delete";
        return executor.post(domain, queryString, headers, payload, null);
    }

    public APIValidator hitPostDeleteAPICall(APIExecutor executor, String domain, String payload, String authToken, String isAuth) {
        HashMap<String, String> headers = new HashMap<>();
        if (isAuth.equalsIgnoreCase("yes")) {
            headers = ApiHeaders.getEmailDefaultHeaders();
            headers.put("Authorization", authToken);
        }
        String queryString = "/client-data-deletion/deletion-api-callback";
        return executor.post(domain, queryString, headers, payload, null);
    }

    public APIValidator hitGetClientDataDeletionDetailsAPI(APIExecutor executor, String domain, String clientId, String authToken) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", authToken);

        String queryString = "/client-data-deletion/fetch-details/";
        return executor.get(domain, queryString + clientId, headers);
    }

    public APIValidator hitGetClientDataDeletionDetailswithoutAuth(APIExecutor executor, String domain, String clientId) {
        HashMap<String, String> headers = new HashMap<>();
        String queryString = "/client-data-deletion/fetch-details/";
        return executor.getWithoutAuthorization(domain, queryString + clientId, headers);
    }

    public static Map<String, String> getAllPropertiesOfSection(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(MicroserviceConfigFilePath, MicroserviceConfigFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForDeleteAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(DeleteCallConfigFilePath, DeleteCallConfigFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForGetClientDataAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(FetchClientDeletionDataFilePath, FetchClientDeletionDataFileName, section_name);
        return map;
    }

    public String getPayloadPostDeletionRequest(String clientId, int requestedBy) {
        return "{\n" +
                "  \"clientId\":" + clientId + ",\n" +
                "  \"requestedBy\":" + requestedBy + "\n" +
                "}";

    }

    public String getPayloadPostDeleteAPICall(String clientId, String serviceId, String status) {

        return "{\n" +
                "  \"clientId\": " + clientId + ",\n" +
                "  \"serviceId\": " + serviceId + ",\n" +
                "  \"status\": \"" + status + "\"\n" +
                "}";
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

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String removeKeyFromBody(String payload, String keyToRemove) {
        if (payload.contains(keyToRemove)) {

            String tempWord = keyToRemove + " ";
            payload = payload.replaceAll(tempWord, "");

            tempWord = " " + keyToRemove;
            payload = payload.replaceAll(tempWord, "");
        }

        return payload;

    }
}
