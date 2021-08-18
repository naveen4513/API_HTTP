package com.sirionlabs.test.login;

import com.sirionlabs.api.commonAPI.LoginApis;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.apache.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;


public class TestLoginAPIs {

    public final static Logger logger = LoggerFactory.getLogger(TestLoginAPIs.class);

    @Test
    public void Test_ApiV1CsrfToken(){

        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();

        logger.info("Inside Test " + methodName);
        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        customAssert.assertAll();
        HttpResponse response = login.hitApiV1CsrfToken();
        checkResponseCode(response,"200","ApiV1CsrfToken",customAssert);
        checkResponseBodyJson(login.apiResponse,"ApiV1CsrfToken",customAssert);
        customAssert.assertAll();
        logger.info("Completed Test " + methodName);
    }

    @Test
    public void Test_fieldLabelMessagesGetAll(){

        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        logger.info("Inside Test " + methodName);

        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        HttpResponse response = login.fieldLabelMessagesGetAll();
        checkResponseCode(response,"200","fieldLabelMessagesGetAll",customAssert);
        checkResponseBodyJson(login.apiResponse,"fieldLabelMessagesGetAll",customAssert);
        logger.info("Completed Test " + methodName);
        customAssert.assertAll();

    }

    @Test
    public void Test_fieldLabelMessagesGetList(){

        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        logger.info("Inside Test " + methodName);

        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        HttpResponse response = login.fieldLabelMessagesGetList();
        checkResponseCode(response,"200","fieldLabelMessagesGetList",customAssert);
        checkResponseBodyJson(login.apiResponse,"fieldLabelMessagesGetList",customAssert);
        logger.info("Completed Test " + methodName);
        customAssert.assertAll();
    }

    @Test
    public void Test_fieldLabelSlaColor(){
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        logger.info("Inside Test " + methodName);

        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        HttpResponse response = login.fieldLabelSlaColor();
        checkResponseCode(response,"200","fieldLabelSlaColor",customAssert);
        checkResponseBodyJson(login.apiResponse,"fieldLabelSlaColor",customAssert);
        logger.info("Completed Test " + methodName);
        customAssert.assertAll();
    }

    @Test
    public void Test_v1SideLayoutData(){

        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        logger.info("Inside Test " + methodName);

        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        HttpResponse response = login.v1SideLayoutData();
        checkResponseCode(response,"200","v1SideLayoutData",customAssert);
        checkResponseBodyJson(login.apiResponse,"v1SideLayoutData",customAssert);
        logger.info("Completed Test " + methodName);
        customAssert.assertAll();
    }

    @Test
    public void Test_tblUsersAdditionalDetails(){

        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        logger.info("Inside Test " + methodName);

        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        HttpResponse response = login.tblUsersAdditionalDetails();
        checkResponseCode(response,"200","tblUsersAdditionalDetails",customAssert);
        checkResponseBodyJson(login.apiResponse,"tblUsersAdditionalDetails",customAssert);
        logger.info("Completed Test " + methodName);
        customAssert.assertAll();
    }

    @Test
    public void Test_uxSrcViewsHeaderRestBrandingHTML(){

        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        logger.info("Inside Test " + methodName);

        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        HttpResponse response = login.uxSrcViewsHeaderRestBrandingHTML();
        checkResponseCode(response,"200","uxSrcViewsHeaderRestBrandingHTML",customAssert);
        logger.info("Completed Test " + methodName);
        customAssert.assertAll();
    }

    @Test
    public void Test_pendingActionsDailyCount(){

        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        logger.info("Inside Test " + methodName);

        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        HttpResponse response = login.pendingActionsDailyCount();
        checkResponseCode(response,"200","pendingActionsDailyCount",customAssert);
        checkResponseBodyJson(login.apiResponse,"pendingActionsDailyCount",customAssert);
        logger.info("Completed Test " + methodName);
        customAssert.assertAll();
    }

    @Test
    public void Test_v1AuthHelperGetAccessToken(){

        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        logger.info("Inside Test " + methodName);

        LoginApis login = new LoginApis();
        CustomAssert customAssert = new CustomAssert();

        HttpResponse response = login.authHelperGetAccessTokenBI();
        checkResponseCode(response,"200","v1AuthHelperGetAccessTokenBI",customAssert);
        checkResponseBodyJson(login.apiResponse,"v1AuthHelperGetAccessTokenBI",customAssert);
        logger.info("Completed Test " + methodName);
        customAssert.assertAll();
    }

    private void checkResponseCode(HttpResponse response,String expRespCode,String apiName,CustomAssert customAssert){

        String statusLine = response.getStatusLine().toString();

        if(!statusLine.contains(expRespCode)){
            logger.error("Expected Response Code  " + expRespCode +  " not there for " + apiName);
            customAssert.assertFalse(true,"Expected Response Code  " + expRespCode +  " not there for " + apiName);
        }

    }

    private void checkResponseBodyJson(String apiResponse,String apiName,CustomAssert customAssert){

        try {

            if (!JSONUtility.validjson(apiResponse)){
                logger.error("Expected Response Body is not a valid json for API " + apiName);
                customAssert.assertFalse(true,"Expected Response Body is not a valid json for API " + apiName);
            }
        }catch (Exception e){
            logger.error("Exception during checkResponseBodyJson method for the API " + apiName);
        }
    }
}
