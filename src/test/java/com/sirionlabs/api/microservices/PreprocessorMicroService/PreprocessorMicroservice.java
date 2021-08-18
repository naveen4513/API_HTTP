package com.sirionlabs.api.microservices.PreprocessorMicroService;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PreprocessorMicroservice {

    private static final String CreateGeneralTaskPreprocessorFilePath;
    private static final String CreateGeneralTaskPreprocessorFileName;


    private static Map<String, String> map;

    static {
        CreateGeneralTaskPreprocessorFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateGeneralTaskPreprocessorFilePath");
        CreateGeneralTaskPreprocessorFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateGeneralTaskPreprocessorFileName");
    }

    public APIValidator hitPostPreprocessorAPICall(APIExecutor executor, String domain, String payload, String authToken, String isAuth, String apiPath) {
        HashMap<String, String> headers = new HashMap<>();
        if (isAuth.equalsIgnoreCase("yes")) {
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", authToken);
        }
        String queryString = "/pre-process/" + apiPath;
        return executor.postWithoutMandatoryHeaders(domain, queryString, headers, payload, null);
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

    public String getPayloadCreateGeneralTaskPreprocessor(String entityType, String entityName, String clientId, String action) {

        return "{\n" +
                "    \"requestId\": \"08e9688a-81d7-4ddd-8d10-cf0089c756e8\",\n" +
                "    \"entityDetails\": {\n" +
                "        \"entity\": {\n" +
                "            \"id\": null,\n" +
                "            \"entityType\": \"" + entityType + "\",\n" +
                "            \"value\": {\n" +
                "\n" +
                "                \"address\": {\n" +
                "                    \"type\": \"TEXT\",\n" +
                "                    \"value\": \"\"\n" +
                "                },\n" +
                "                \"functions\": {\n" +
                "                    \"type\": \"MULTI_REF\",\n" +
                "                    \"value\": [\n" +
                "                        {\n" +
                "                            \"id\": 1129,\n" +
                "                            \"name\": \"CRM\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"parentEntityId\": {\n" +
                "                    \"type\": \"INTEGER\",\n" +
                "                    \"value\": 1384\n" +
                "                },\n" +
                "                \"services\": {\n" +
                "                    \"type\": \"MULTI_REF\",\n" +
                "                    \"value\": [\n" +
                "                        {\n" +
                "                            \"id\": 1251,\n" +
                "                            \"name\": \"abcd\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"parentEntityTypeId\": {\n" +
                "                    \"type\": \"INTEGER\",\n" +
                "                    \"value\": 3\n" +
                "                },\n" +
                "                \"globalRegions\": {\n" +
                "                    \"type\": \"MULTI_REF\",\n" +
                "                    \"value\": [\n" +
                "                        {\n" +
                "                            \"id\": 1012,\n" +
                "                            \"name\": \"APAC\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"globalCountries\": {\n" +
                "                    \"type\": \"MULTI_REF\",\n" +
                "                    \"value\": [\n" +
                "                        {\n" +
                "                            \"id\": 15,\n" +
                "                            \"name\": \"Australia\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"id\": 20,\n" +
                "                            \"name\": \"Bangladesh\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "               \n" +
                "                \"name\": {\n" +
                "                    \"type\": \"TEXT\",\n" +
                "                    \"value\": \"" + entityName + "\"\n" +
                "                },\n" +
                "                \"alias\": {\n" +
                "                    \"type\": \"TEXT\",\n" +
                "                    \"value\": \"1\"\n" +
                "                },\n" +
                "                \"entityTypeId\": {\n" +
                "                    \"type\": \"INTEGER\",\n" +
                "                    \"value\": 1\n" +
                "                },\n" +
                "                \"parentEntityIds\": {\n" +
                "                    \"type\": \"ARRAY_INT\",\n" +
                "                    \"value\": [\n" +
                "                        1384\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"creationParamsId\": {\n" +
                "                    \"type\": \"INTEGER\",\n" +
                "                    \"value\": 1002\n" +
                "                }\n" +
                "            }\n" +
                "        },\n" +
                "        \"persistedEntity\": null,\n" +
                "        \"parents\": [\n" +
                "        ],\n" +
                "        \"grandParents\": [],\n" +
                "        \"relations\": [],\n" +
                "        \"vendors\": []\n" +
                "    },\n" +
                "    \"globalInfo\": {\n" +
                "        \"entityTypeId\": 1,\n" +
                "        \"clientId\": " + clientId + ",\n" +
                "        \"userId\": 1201,\n" +
                "        \"languageId\": 1,\n" +
                "        \"sirionBaseUrl\": \"http://qavf.rc.office\"\n" +
                "    },\n" +
                "    \"task\": {\n" +
                "        \"type\": \"General\",\n" +
                "        \"nodeId\": 3,\n" +
                "        \"openNodeIds\": null,\n" +
                "        \"nextNodeIds\": null,\n" +
                "        \"action\": \"" + action + "\",\n" +
                "        \"status\": 100005\n" +
                "    },\n" +
                "    \"taskId\": 3,\n" +
                "    \"taskHistoryList\": [],\n" +
                "    \"fields\": [\n" +
                "        {\n" +
                "            \"id\": 101541,\n" +
                "            \"apiName\": \"rg_2296\",\n" +
                "            \"apiModel\": \"stakeHolders.values\",\n" +
                "            \"label\": \"Supplier Contract Manager\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    public String updatePayloadId(String payload, int idToBeUpdated) {
        JSONObject jObject = new JSONObject(payload);
        jObject.getJSONObject("entityDetails").getJSONObject("entity").put("id", idToBeUpdated);

        return jObject.toString();
    }

    public static Map<String, String> getAllConfigForCreatePreprocessorForGeneralTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateGeneralTaskPreprocessorFilePath, CreateGeneralTaskPreprocessorFileName, section_name);
        return map;
    }
}
