package com.sirionlabs.api.autoExtraction.API.ContractDocumentGetEntities;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;


public class ContractDocumentGetEntitiesAPI {

    private final static Logger logger = LoggerFactory.getLogger(ContractDocumentGetEntitiesAPI.class);

    public static String getApiPath(int documentId,int pageNumber,String version) {
        return "/tblcontractdocuments/getentities/" + documentId + "/" + pageNumber + "/" + version;
    }

    public static HttpResponse hitContractDocumentGetEntitiesAPI(String apiPath,String authorizationHeader, String acceptHeader){
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse httpResponse = null;

        try {
            HttpGet httpGet = new HttpGet(apiPath);
            httpGet.addHeader("Authorization",authorizationHeader);
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpGet.addHeader("Accept",acceptHeader);
            HttpHost httpHost = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("host"),Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("port")),ConfigureEnvironment.getEnvironmentProperty("scheme"));
            httpResponse = httpClient.execute(httpHost,httpGet);
        }
        catch (Exception e){
            logger.error("Error in hitting Global Upload API " + e.getStackTrace());
        }
        return httpResponse;
    }

    public static HashMap<String,String> getContractDocumentGetEntitiesResponse(HttpResponse httpResponse) throws IOException {
        HashMap<String,String> contractDocumentGetEntities = new HashMap<>();
        String jsonResponseStr = EntityUtils.toString(httpResponse.getEntity());
        JSONObject jsonObject = new JSONObject(jsonResponseStr);
        contractDocumentGetEntities.put("active",jsonObject.getJSONObject("ae").get("active").toString());
        contractDocumentGetEntities.put("type",jsonObject.getJSONObject("ae").get("type").toString());
        contractDocumentGetEntities.put("url",jsonObject.getJSONObject("ae").get("url").toString());
        return contractDocumentGetEntities;
    }
}
