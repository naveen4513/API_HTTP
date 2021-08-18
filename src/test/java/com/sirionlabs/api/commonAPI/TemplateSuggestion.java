package com.sirionlabs.api.commonAPI;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class TemplateSuggestion extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(TemplateSuggestion.class);
    CustomAssert customAssert = new CustomAssert();

    public String hitSelectTemplate(int entityTypeID, int entityID, String payload){
        HttpResponse response;
        String selectTemplateResponse = null;
        try {
            HttpPost postRequest;
            String queryString = "/suggestion/contractTemplates/"+entityTypeID+"/"+entityID;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            response = super.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            selectTemplateResponse = EntityUtils.toString(response.getEntity());
        }catch(Exception e){
        logger.error("Exception {} occurred while fetching Select Template Response", e.getMessage());
        customAssert.assertTrue(false,"Exception {} occurred while fetching Select Template Response");
        }
        return selectTemplateResponse;
    }
}