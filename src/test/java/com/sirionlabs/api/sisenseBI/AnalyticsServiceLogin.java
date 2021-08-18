package com.sirionlabs.api.sisenseBI;

import com.sirionlabs.config.ConfigureEnvironment;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class AnalyticsServiceLogin  {

    private final static Logger logger = LoggerFactory.getLogger(AnalyticsServiceLogin.class);
    private static String statusCode;
    public HttpResponse hitAnalyticsServiceLogin(String payload) throws IOException {

        HttpClient httpClient;

        httpClient = HttpClientBuilder.create().build();

        HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("analyticsservice_host"),
                Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("analyticsservice_portnumber")), ConfigureEnvironment.getEnvironmentProperty("analyticsservice_scheme"));

        HttpResponse httpResponse = null;
        String queryString = "/analytics/sirion/analytics/login";
        logger.debug("Query string url formed is {}", queryString);

        HttpPost httpPostRequest = new HttpPost(queryString);

        httpPostRequest.addHeader("Accept", "*/*");
        httpPostRequest.addHeader("Content-Type", "application/json");

        httpPostRequest.setEntity(new StringEntity(payload));

        httpResponse = httpClient.execute(target, httpPostRequest);

        logger.debug("The Response is : [ {} ]", httpResponse);

        Header[] headers = httpResponse.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("Response header {}", headers[i].toString());
        }
        statusCode = httpResponse.getStatusLine().toString();
        logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
        return httpResponse;
    }

    public String getStatusCode(){
        return statusCode;
    }
}
