package com.sirionlabs.api.presignature;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Template extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(Template.class);

    public HttpResponse verifyTemplateFormattingClientAdmin(String uri){
        HttpResponse httpResponse = null;

        try{
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            httpResponse =APIUtils.getRequest(httpGet);
            logger.info("API status is " + httpResponse.getStatusLine().getStatusCode());
        }
        catch (Exception e){
            logger.error("Exception Occurred while hitting client admin show page " + e.getMessage());
        }
        return httpResponse;
    }

    public HttpResponse createOrPreviewOrEditStyleTemplate(String uri,String payload){
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse httpResponse = null;

        try{
            HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"), Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")),
                    ConfigureEnvironment.getEnvironmentProperty("Scheme"));
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Accept","application/json, text/plain, */*");
            httpPost.addHeader("Content-Type","application/json;charset=UTF-8");
            httpPost.addHeader("X-Requested-With","XMLHttpRequest");
            httpPost.addHeader("Authorization",Check.getAuthorization());
            httpPost.addHeader("Accept-Encoding","gzip, deflate");

            if (payload != null)
                httpPost.setEntity(new StringEntity(payload, "UTF-8"));

            httpResponse = httpClient.execute(target,httpPost);
            logger.info("API status is " + httpResponse.getStatusLine().getStatusCode());
        }
        catch (Exception e){
            logger.error("Exception Occurred while creating/Previewing the " + e.getMessage());
        }
        return httpResponse;
    }

    public HttpResponse showTemplate(int templateId){
        String url = "/wordStyle/show?id=" + templateId +"&isDefault=false";
        HttpResponse httpResponse = null;

        try{
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Accept","application/json, text/plain, */*");
            httpResponse =APIUtils.getRequest(httpGet);
            logger.info("API status is " + httpResponse.getStatusLine().getStatusCode());
        }
        catch (Exception e){
            logger.error("Exception Occurred while hitting template show page " + e.getMessage());
        }
        return httpResponse;
    }
}
