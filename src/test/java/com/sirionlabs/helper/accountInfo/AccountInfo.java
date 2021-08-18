package com.sirionlabs.helper.accountInfo;

import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class AccountInfo extends TestAPIBase {
    Logger logger= LoggerFactory.getLogger(AccountInfo.class);
    String queryString = "/account/info";

    public HashMap<String, String> getHeader() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public APIValidator getUserAccountInfoResponse() {
        return executor.get(queryString, getHeader());
    }

    public String getUserName() {
        try {
            logger.info("Hitting user account info API");
            APIValidator apiValidator = getUserAccountInfoResponse();
            if (apiValidator.getResponse().getResponseCode() == 200) {
                String userInfoResponse = apiValidator.getResponse().getResponseBody();
                if (ParseJsonResponse.validJsonResponse(userInfoResponse)) {
                    return new JSONObject(userInfoResponse).getJSONObject("user").getString("name");
                }
                else
                {
                    logger.error("Invalid Json Response for API {[]}",queryString);
                }
            }
            else
            {
                logger.error("Invalid response code for API {} and response status Code is {}",queryString,apiValidator.getResponse().getResponseCode());
            }
        } catch (Exception e) {
             logger.error(e.getMessage());
        }
        return null;
    }
    public int getUserId()
    {
        try {
            logger.info("Hitting user account info API");
            APIValidator apiValidator = getUserAccountInfoResponse();
            if (apiValidator.getResponse().getResponseCode() == 200) {
                String userInfoResponse = apiValidator.getResponse().getResponseBody();
                if (ParseJsonResponse.validJsonResponse(userInfoResponse)) {
                    return new JSONObject(userInfoResponse).getJSONObject("user").getInt("id");
                }
                else
                {
                    logger.error("Invalid Json Response for API {[]}",queryString);
                }
            }
            else
            {
                logger.error("Invalid response code for API {} and response status Code is {}",queryString,apiValidator.getResponse().getResponseCode());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return 0;

    }
}
