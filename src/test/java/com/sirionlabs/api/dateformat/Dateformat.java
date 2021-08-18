package com.sirionlabs.api.dateformat;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;


import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

public class Dateformat extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(Dateformat.class);

    public Integer UserOrgainzationPropertiesUpdate(Map<String, String> params) {
        Integer statusCode = -1;
        try {
            HttpHost target = super.generateHttpTargetHost();
            String queryString = "/tblclientspecificproperties";
            logger.debug("Query string url formed is {}", queryString);
            String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
            String contentTypeHeader = "multipart/form-data; boundary=----WebKitFormBoundaryS6GmewG7p7x8jpeV";

            HttpPost postRequest = super.generateHttpPostRequestWithQueryString(queryString, acceptHeader, contentTypeHeader);
            HttpEntity entity = super.createMultipartEntityBuilder(params,"----WebKitFormBoundaryS6GmewG7p7x8jpeV");
            postRequest.setEntity(entity);

            HttpClient httpClient;

            if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
                APIUtils oAPIUtils = new APIUtils();
                httpClient = oAPIUtils.getHttpsClient();
            } else {
                if (ConfigureEnvironment.isProxyEnabled) {
                    HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
                    httpClient = HttpClients.custom().setProxy(proxy).build();
                } else {
                    httpClient = HttpClientBuilder.create().build();
                }
            }
            HttpResponse response = httpClient.execute(target, postRequest);
            String[] statusLine = response.getStatusLine().toString().trim().split(Pattern.quote("HTTP/1.1"));

            if (statusLine.length > 1) {
                statusCode = Integer.parseInt(statusLine[1].trim());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting User update Organisation update tblclientspecificproperties Api. {}", e.getMessage());
        }
        return statusCode;
    }
}