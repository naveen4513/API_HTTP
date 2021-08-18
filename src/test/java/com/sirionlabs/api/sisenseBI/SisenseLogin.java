package com.sirionlabs.api.sisenseBI;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SisenseLogin extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(SisenseLogin.class);
    private static String loginResponseStr;
    public static String access_token;
    public static String userId;

    public HttpResponse hitSisenseLogin() throws IOException {

        String username = ConfigureEnvironment.getEnvironmentProperty("SisenseUsername");
        String password = ConfigureEnvironment.getEnvironmentProperty("SisensePassword");

        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            httpClient = super.getHttpsClient();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }

        HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("SisenseHost"),
                Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("SisensePort")), ConfigureEnvironment.getEnvironmentProperty("SisenseScheme"));

        HttpResponse httpResponse = null;
        String queryString = "/analytics/api/v1/authentication/login";
        logger.debug("Query string url formed is {}", queryString);

        HttpPost httpPostRequest = new HttpPost(queryString);

        httpPostRequest.addHeader("Accept", "*/*");
        httpPostRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("username", username);
        parameters.put("password", password);

        String params = UrlEncodedString.getUrlEncodedString(parameters);
        httpPostRequest.setEntity(new StringEntity(params));

        httpResponse = httpClient.execute(target, httpPostRequest);

        loginResponseStr = EntityUtils.toString(httpResponse.getEntity());
        logger.debug("The Response is : [ {} ]", httpResponse);

        logger.debug("response json is: {}", loginResponseStr);

        Header[] headers = httpResponse.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("Response header {}", headers[i].toString());
        }
        logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
        return httpResponse;
    }

    @Test
    public void testSisenseLogin() throws IOException{

        hitSisenseLogin();

        JSONObject loginResponseStrJson = new JSONObject(loginResponseStr);
        access_token = loginResponseStrJson.get("access_token").toString();
        userId = loginResponseStrJson.get("userId").toString();
    }
}
