package com.sirionlabs.api.delegation;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DeleteDelegation extends APIUtils {

    String deleteDelegationJsonStr;

    private final static Logger logger = LoggerFactory.getLogger(DeleteDelegation.class);

    private void setdeleteDelegationJsonStr(String createDelegationJsonStr){
        this.deleteDelegationJsonStr =createDelegationJsonStr;
    }

    public String getcreateDelegationJsonStr(){
        return this.deleteDelegationJsonStr;
    }

    public HttpResponse hitDeleteDelegation(String payload) throws Exception {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/delegation/delete";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.deleteDelegationJsonStr = EntityUtils.toString(response.getEntity());
            setdeleteDelegationJsonStr(this.deleteDelegationJsonStr);

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }


    public String getClientAdminDeletePayload( List<String> delegationList, String sourceUserId){
        JSONArray delegationIds = new JSONArray();
        for (String delegation: delegationList) {
            delegationIds.put(String.valueOf(delegation));
        }
        JSONObject payload = new JSONObject();
        payload.put("delegationIds",delegationIds);
        payload.put("delegatedDomainUser",new JSONObject());
        payload.put("sourceUserId",sourceUserId);
        return  payload.toString();
    }

    public String getCreatePayload( List<String> delegationList){
        JSONArray delegationIds = new JSONArray();
        for (String delegation: delegationList) {
            delegationIds.put(String.valueOf(delegation));
        }
        JSONObject payload = new JSONObject();
        payload.put("delegationIds",delegationIds);
        return  payload.toString();
    }
}
