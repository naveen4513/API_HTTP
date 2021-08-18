package com.sirionlabs.api.sisenseBI;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
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

public class DataSources extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(SisenseLogin.class);
    private String datasourceAPIResponse;

    public HttpResponse hitSisenseDataSourcesAPI(String title, String payloadDataSourcesAPI, String accessToken) throws IOException {

        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            httpClient = super.getHttpsClient();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }

        HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("SisenseHost"),
                Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("SisensePort")), ConfigureEnvironment.getEnvironmentProperty("SisenseScheme"));

        HttpResponse httpResponse = null;
        String queryString = "/analytics//api/datasources/" + title + "/jaql";
        logger.debug("Query string url formed is {}", queryString);

        HttpPost httpPostRequest = new HttpPost(queryString);

        httpPostRequest.addHeader("Accept", "application/json, text/plain, */*");
        httpPostRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        httpPostRequest.addHeader("Authorization", "Bearer " + accessToken);

        httpPostRequest.setEntity(new StringEntity(payloadDataSourcesAPI));

        httpResponse = httpClient.execute(target, httpPostRequest);

        datasourceAPIResponse = EntityUtils.toString(httpResponse.getEntity());
        logger.debug("The Response is : [ {} ]", httpResponse);

        logger.debug("response json is: {}", datasourceAPIResponse);

        Header[] headers = httpResponse.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("Response header {}", headers[i].toString());
        }
        logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
        return httpResponse;
    }

    public HttpResponse hitAnalyticsDataSourcesAPI(String title, String payloadDataSourcesAPI, String accessToken) throws IOException {

        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            httpClient = super.getHttpsClient();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }

        HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("analyticsservice_host"),
                Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("analyticsservice_portnumber")), ConfigureEnvironment.getEnvironmentProperty("analyticsservice_scheme"));

        HttpResponse httpResponse = null;
        String queryString = "/analytics//api/datasources/" + title + "/jaql";
        logger.debug("Query string url formed is {}", queryString);

        HttpPost httpPostRequest = new HttpPost(queryString);

        httpPostRequest.addHeader("Accept", "application/json, text/plain, */*");
        httpPostRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        httpPostRequest.addHeader("Cookie", "Authorization=" + accessToken + ";");
        httpPostRequest.setEntity(new StringEntity(payloadDataSourcesAPI));

        httpResponse = httpClient.execute(target, httpPostRequest);

        datasourceAPIResponse = EntityUtils.toString(httpResponse.getEntity());
        logger.debug("The Response is : [ {} ]", httpResponse);

        logger.debug("response json is: {}", datasourceAPIResponse);

        Header[] headers = httpResponse.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("Response header {}", headers[i].toString());
        }
        logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
        return httpResponse;
    }

    public String getSisenseDataSourcesAPIResponseStr() {
        return this.datasourceAPIResponse;
    }
}
