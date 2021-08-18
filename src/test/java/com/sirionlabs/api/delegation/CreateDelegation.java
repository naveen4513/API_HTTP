package com.sirionlabs.api.delegation;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDelegation extends APIUtils {

    public static String createDelegationJsonStr;

    private final static Logger logger = LoggerFactory.getLogger(CreateDelegation.class);

    private void setcreateDelegationJsonStr(String createDelegationJsonStr){
        this.createDelegationJsonStr =createDelegationJsonStr;
    }

    public String getcreateDelegationJsonStr(){
        return this.createDelegationJsonStr;
    }


    public HttpResponse hitCreateDelegation(String payload){
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/delegation/create";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.createDelegationJsonStr = EntityUtils.toString(response.getEntity());
            setcreateDelegationJsonStr(this.createDelegationJsonStr);

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Create Delegation API. {}", e.getMessage());
        }
        return response;
    }


    public HttpResponse hitCreateDelegationV2(String payload){
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/delegation/createV2?version=2.0";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.createDelegationJsonStr = EntityUtils.toString(response.getEntity());
            setcreateDelegationJsonStr(this.createDelegationJsonStr);

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Create Delegation API. {}", e.getMessage());
        }
        return response;
    }

}
