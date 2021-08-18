package com.sirionlabs.helper.entityWorkflowAction;

import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import net.minidev.json.JSONArray;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EntityWorkflowActionHelper extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(EntityWorkflowActionHelper.class);
    private String JsonStr;

    public String getResponse() {
          return JsonStr;
    }

    public void hitWorkflowAction(String entityName, int entityTypeId ,int entityId, String actionLabel){
        HttpResponse response = null;
        HttpPost postRequest;
        String showApiResponse = ShowHelper.getShowResponse(entityTypeId,entityId);
        if (showApiResponse != null) {

               //  Get action URI PATH
                JSONArray actionArray = (JSONArray)JSONUtility.parseJson(showApiResponse,"$.body.layoutInfo.actions[?(@.label == \""+actionLabel+"\")]");
                Object uriObj =   JSONUtility.parseJson(actionArray.toJSONString(),"$.[*].api");

                try{
                    String queryString = ((List<String> )uriObj).get(0);

                    // Create Payload
                    JSONObject obj = new JSONObject(showApiResponse);
                    JSONObject body = obj.getJSONObject("body");
                    JSONObject data = body.getJSONObject("data");
                    JSONObject paylaod  = new JSONObject();
                    JSONObject payload_body  = new JSONObject();
                    payload_body.put("data",data);
                    paylaod.put("body",payload_body);

                    // Hit workflow api
                    logger.debug("Query string url formed is {}", queryString);
                    postRequest = new HttpPost(queryString);
                    postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
                    postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
                    postRequest.addHeader("Accept-Encoding", "aaa");
                    response = super.postRequest(postRequest, paylaod.toString());
                    logger.debug("Response status is {}", response.getStatusLine().toString());
                    JsonStr = EntityUtils.toString(response.getEntity());
                    String status =  JSONUtility.parseJson(JsonStr,"$.header.response.status").toString();
                    if(!status.equals("success")){
                        logger.error("Exception while hitting workflow API for Entity {}",entityName);
                    }
                    else{
                        logger.info(actionLabel+" workflow action done");
                    }

                }catch(Exception e){
                    logger.error("workflow url not found in show api response for Entity {}. Hence not hitting EntityWorkflowAction API", entityName);
                }
            }
            else{
                logger.error(" getshowApiResponse is null for Entity {}. Hence not hitting EntityWorkflowAction API", entityName);
            }





    }


}
