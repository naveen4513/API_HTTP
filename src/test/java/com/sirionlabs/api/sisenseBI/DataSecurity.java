package com.sirionlabs.api.sisenseBI;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class DataSecurity extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(DataSecurity.class);
    private String DataSecurityResponse;

    public HttpResponse hitDataSecurity(String cubeid,String payload,String accesstoken) throws IOException {

        HttpClient httpClient;
        if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
            httpClient = super.getHttpsClient();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }

        HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("SisenseHost"),
                Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("SisensePort")), ConfigureEnvironment.getEnvironmentProperty("SisenseScheme"));

        HttpResponse httpResponse = null;
        String queryString = "/analytics//api/elasticubes/datasecurity/" + cubeid;
        logger.debug("Query string url formed is {}", queryString);

        HttpPut httpPutRequest = new HttpPut(queryString);

        httpPutRequest.addHeader("Accept", "application/json, text/plain, */*");
        httpPutRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        httpPutRequest.addHeader("Authorization", "Bearer " + accesstoken);
        httpPutRequest.setEntity(new StringEntity(payload));

        httpResponse = httpClient.execute(target, httpPutRequest);

        DataSecurityResponse = EntityUtils.toString(httpResponse.getEntity());
        logger.debug("The Response is : [ {} ]", httpResponse);

        logger.debug("response json is: {}", DataSecurityResponse);

        Header[] headers = httpResponse.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("Response header {}", headers[i].toString());
        }
        logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
        return httpResponse;
    }

    public String getDataSecurityResponse(){
        return this.DataSecurityResponse;
    }
}
