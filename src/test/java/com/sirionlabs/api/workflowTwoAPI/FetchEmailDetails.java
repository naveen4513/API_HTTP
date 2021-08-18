package com.sirionlabs.api.workflowTwoAPI;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FetchEmailDetails {

    public static String domain = "";


    private static final String FetchEmailDetailsFilePath;
    private static final String FetchEmailDetailsFileName;

    private static Map<String, String> map;

    static {
        FetchEmailDetailsFilePath = ConfigureConstantFields.getConstantFieldsProperty("FetchEmailDetailsFilePath");
        FetchEmailDetailsFileName = ConfigureConstantFields.getConstantFieldsProperty("FetchEmailDetailsFileName");
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

    public static String getEmailDetailsRequestBody() throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\nitin.jain\\Desktop\\FetchEmailDetailsRequest.txt"));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        reader.close();

        String content = stringBuilder.toString();
        return content;
    }

    public APIValidator hitPostEmailDetailsAPICall(APIExecutor executor, String domain, String payload, String authToken, String isAuth) {
        HashMap<String, String> headers = new HashMap<>();
        if (isAuth.equalsIgnoreCase("yes")) {
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", authToken);
        }
        String queryString = "/workflow-v2/email-details";
        return executor.postWithoutMandatoryHeaders(domain, queryString, headers, payload, null);
    }

    public static Map<String, String> getAllConfigForFetchEmailDetailsAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(FetchEmailDetailsFilePath, FetchEmailDetailsFileName, section_name);
        return map;
    }

}
