package com.sirionlabs.api.layout;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static com.sirionlabs.helper.api.TestAPIBase.executor;

public class ActionsOnShow extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(ActionsOnShow.class);


    public String hitGetActionButton(int entityTypeId, int entityId){


        String responseStr = null;

        String queryString = "/v3/actions/"+entityTypeId+"/"+entityId;

        APIResponse response = executor.get(queryString, getHeaders()).getResponse();
        logger.debug("Response status is {}", response.getResponseCode());
        responseStr = response.getResponseBody();
        return  responseStr;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }


}
