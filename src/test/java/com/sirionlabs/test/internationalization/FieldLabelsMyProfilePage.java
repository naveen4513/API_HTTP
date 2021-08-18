package com.sirionlabs.test.internationalization;

import com.sirionlabs.helper.accountInfo.AccountInfo;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.sirionlabs.test.internationalization.TestDisputeInternationalization.expectedPostFix;

public class FieldLabelsMyProfilePage extends TestAPIBase {
    Logger logger= LoggerFactory.getLogger(AccountInfo.class);
    String myAccountInfoqueryString = "/account/info";
    String allfieldLabelMessagesqueryString = "/fieldlabel/messages/all";

    public HashMap<String, String> getHeader() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public APIValidator getUserAccountInfoResponse() {
        return executor.get(myAccountInfoqueryString, getHeader());
    }
    public APIValidator getUserAccountGBResponse(){
       return executor.post(allfieldLabelMessagesqueryString,getHeader(),null);
    }

    public List<String> getMyAccountInfoFieldLabels(CustomAssert csAssert) {
        try {
            logger.info("Hitting user account info API");
            APIValidator apiValidator = getUserAccountInfoResponse();
            if (apiValidator.getResponse().getResponseCode() == 200) {
                String userInfoResponse = apiValidator.getResponse().getResponseBody();
                if (ParseJsonResponse.validJsonResponse(userInfoResponse)) {
                    JSONObject Obj = new JSONObject(userInfoResponse);
                    JSONArray arr = Obj.names();
                    for(int i=0;i<arr.length();i++) {

                        if (!arr.get(i).toString().contains("languageList") && !arr.get(i).toString().contains("questionList") && !arr.get(i).toString().contains("user")
                        && !arr.get(i).toString().contains("dynamicMetadata") && !arr.get(i).toString().contains("dynamicData")) {
                            List<String> temp = (ArrayList<String>)JSONUtility.parseJson(userInfoResponse, "$." + arr.get(i) + "[*].name");

                            for(String label:temp){
                                if (label.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                                    csAssert.assertTrue(false, "Field Label: [" + label + "] contain: [" + expectedPostFix + "] under My Profile page of " + arr.get(i));
                                } else {
                                    csAssert.assertTrue(true, "Field Label: [" + label + "] does not contain: [" + expectedPostFix + "]c under My profile page of " + arr.get(i));
                                }
                            }


                        }else if (arr.get(i).toString().contains("questionList")) {
                            List<String> temp = (ArrayList<String>)JSONUtility.parseJson(userInfoResponse, "$." + arr.get(i) + "[*].question");

                            for(String label:temp){
                                if (label.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                                    csAssert.assertTrue(false, "Field Label: [" + label + "] contain: [" + expectedPostFix + "] under My Profile page of " + arr.get(i));
                                } else {
                                    csAssert.assertTrue(true, "Field Label: [" + label + "] does not contain: [" + expectedPostFix + "]c under My profile page of " + arr.get(i));
                                }
                            }
                        }
                    }
                }
                else
                {
                    logger.error("Invalid Json Response for API {[]}",myAccountInfoqueryString);
                }
            }
            else
            {
                logger.error("Invalid response code for API {} and response status Code is {}",myAccountInfoqueryString,apiValidator.getResponse().getResponseCode());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public List<String> getMyAccountGBInfoFieldLabels(CustomAssert csAssert) {
        try {
            logger.info("Hitting user account info API");
            APIValidator apiValidator = getUserAccountGBResponse();
            if (apiValidator.getResponse().getResponseCode() == 200) {
                String userInfoResponse = apiValidator.getResponse().getResponseBody();
                if (ParseJsonResponse.validJsonResponse(userInfoResponse)) {
                            List<String> temp = (ArrayList<String>)JSONUtility.parseJson(userInfoResponse, "$.[*].name");

                            for(String label:temp){
                                if (label.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                                    csAssert.assertTrue(false, "Field Label: [" + label + "] contain: [" + expectedPostFix + "] under All Field Label Messages");
                                } else {
                                    csAssert.assertTrue(true, "Field Label: [" + label + "] does not contain: [" + expectedPostFix + "] under All Field Label Messages");
                                }
                            }
                }
                else
                {
                    logger.error("Invalid Json Response for API {[]}",allfieldLabelMessagesqueryString);
                }
            }
            else
            {
                logger.error("Invalid response code for API {} and response status Code is {}",allfieldLabelMessagesqueryString,apiValidator.getResponse().getResponseCode());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }
}
