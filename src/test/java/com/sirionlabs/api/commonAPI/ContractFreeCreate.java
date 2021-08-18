package com.sirionlabs.api.commonAPI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractFreeCreate extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(ContractFreeCreate.class);
    HttpResponse freeCreateHttpResponse;
    public String freeCreateAPIResponseCode;

    static String freeCreateJsonStr = null;

    public String getFreeCreateJsonStr() {
        return freeCreateJsonStr;
    }

    public String hitContractFreeCreate(int entityId, int entityTypeId, int creationTypeId) {
        String responseStr = null;
        try {
            HttpGet getRequest;
            String queryString = "/" + "admin/contractFreeCreate/"+entityId+"/"+entityTypeId+"/"+creationTypeId;
            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = super.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            responseStr = EntityUtils.toString(response.getEntity());
            freeCreateAPIResponseCode = response.getStatusLine().toString();
            freeCreateHttpResponse = response;
            freeCreateJsonStr = responseStr;
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Edit Get response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Edit Get Api. {}", e.getMessage());
        }
        return responseStr;
    }
}