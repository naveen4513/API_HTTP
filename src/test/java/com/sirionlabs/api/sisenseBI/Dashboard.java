package com.sirionlabs.api.sisenseBI;

import com.sirionlabs.config.ConfigureEnvironment;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Dashboard {

    private final static Logger logger = LoggerFactory.getLogger(DataSecurity.class);
    private String DashboardResponse;

    public HttpResponse hitDashboard(String dashboardId, String accesstoken) throws IOException {

        HttpClient httpClient;

        httpClient = HttpClientBuilder.create().build();
        HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("SisenseHost"),
                Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("SisensePort")), ConfigureEnvironment.getEnvironmentProperty("SisenseScheme"));

        HttpResponse httpResponse;
        String queryString = "/analytics/api/dashboards/" + dashboardId;
        logger.debug("Query string url formed is {}", queryString);

        HttpGet httpGetRequest = new HttpGet(queryString);

        httpGetRequest.addHeader("Accept", "application/json, text/plain, */*");
        httpGetRequest.addHeader("Authorization", "Bearer " + accesstoken);

        httpResponse = httpClient.execute(target, httpGetRequest);

        DashboardResponse = EntityUtils.toString(httpResponse.getEntity());
        logger.debug("The Response is : [ {} ]", httpResponse);

        logger.debug("response json is: {}", DashboardResponse);

        logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
        return httpResponse;
    }

    public String getDashboardResponse(){
        return this.DashboardResponse;
    }
}
