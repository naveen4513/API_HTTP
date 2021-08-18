package com.sirionlabs.test.SL;

import com.sirionlabs.api.clientAdmin.emailConfig.SystemEmailConfig;
import com.sirionlabs.api.clientAdmin.slMetStatus.SlMetStatus;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class TestClientAdminTests_SL {

    @BeforeClass
    public void beforeClass(){

        AdminHelper adminHelper = new AdminHelper();
        adminHelper.loginWithClientAdminUser();
    }

    @Test()
    public void TestUpdateEmailTemplate(){

        CustomAssert customAssert = new CustomAssert();

        try{
            int entityTypeId = 14;

            APIResponse systemEmailConfig = SystemEmailConfig.getSystemEmailConfiguration(entityTypeId);
            String systemEmailConfigResponseBody = systemEmailConfig.getResponseBody();
            JSONArray systemEmailConfigResponseBodyJsonArray = new JSONArray(systemEmailConfigResponseBody);
            String entityActionEmail = systemEmailConfigResponseBodyJsonArray.getJSONObject(0).get("name").toString();
            entityActionEmail = entityActionEmail.replace(" ","%20");
            String isNonWorkFlowEmail = systemEmailConfigResponseBodyJsonArray.getJSONObject(0).get("isNonWorkFlowEmail").toString();


            APIResponse languageForEmailConfig = SystemEmailConfig.getLanguagesForEmailAction(entityTypeId,entityActionEmail);
            String languageForEmailConfigResponseBody = languageForEmailConfig.getResponseBody();
            JSONObject languageForEmailConfigResponseBodyJson = new JSONObject(languageForEmailConfigResponseBody);
            JSONArray languagesJsonArray = languageForEmailConfigResponseBodyJson.getJSONArray("languages");
            String languageId = languagesJsonArray.getJSONObject(0).get("id").toString();

            APIResponse emailData = SystemEmailConfig.getEmailData(entityTypeId,entityActionEmail,languageId);
            String emailDataResponseBody = emailData.getResponseBody();
            JSONObject emailDataResponseBodyJson = new JSONObject(emailDataResponseBody);
            JSONObject toCCData = emailDataResponseBodyJson.getJSONObject("toCcData");

            String toRoleGroup  = toCCData.getJSONArray("toGroupRole").get(0).toString();
            String ccRoleGroup  = toCCData.getJSONArray("ccGroupRole").get(0).toString();
            String bccRoleGroup  = toCCData.getJSONArray("bccGroupRole").get(0).toString();
            String subjectData  = emailDataResponseBodyJson.get("subjectData").toString();
            String workFlowActionAdded  = emailDataResponseBodyJson.get("workFlowActionAdded").toString();

            LinkedHashMap paramsMap = new LinkedHashMap<>();


            paramsMap.put("entityTypeId", entityTypeId);
            paramsMap.put("entityActionEmail", entityActionEmail);
            paramsMap.put("userLanguageId", languageId);

            paramsMap.put("_workFlowActionAdded", "on");

            paramsMap.put("toCcBccDetailsHolder.toGroupRole", Integer.parseInt(toRoleGroup));
            paramsMap.put("toCcBccDetailsHolder.ccGroupRole", Integer.parseInt(ccRoleGroup));
            paramsMap.put("toCcBccDetailsHolder.bccGroupRole", Integer.parseInt(bccRoleGroup));

            paramsMap.put("isNonWorkflowEmail", Boolean.valueOf(isNonWorkFlowEmail));
            paramsMap.put("statusFilterEnabled", null);
            paramsMap.put("subjectDataString", subjectData);

            paramsMap.put("bodyDataString", "");
            paramsMap.put("bodyDataString_textarea", true);

            paramsMap.put("_csrf_token", "null");

            APIResponse emailConfigurationUpdateResponse = SystemEmailConfig.setSysTemEmailConfiguration(paramsMap);

            int responseCode = emailConfigurationUpdateResponse.getResponseCode();

//            if 302 not an error
            if(responseCode != 200){
                customAssert.assertTrue(false,"Expected Response code is 200 Actual Status Code " + responseCode);
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    @Test
    public void TestSLMetStatusConfiguration(){

        CustomAssert customAssert = new CustomAssert();

        try{

            APIResponse apiResponse = SlMetStatus.getSlMetStatus();
            String responseBody = apiResponse.getResponseBody();

            Document html = Jsoup.parse(responseBody);

            List<String> expectedSLMetStatus = Arrays.asList("Met Exp","Met Min","Met Significantly Minimum","Not Met","LV","No Data Available","Not Reported","Work In Progress","Not Applicable");

            String slMetStatus;
            List<String> slMetStatusList = new ArrayList<>();

            int childNodeSize = html.getElementById("l_com_sirionlabs_model_MasterSlacategory").child(1).childNodeSize();

            for(int i =0;i<childNodeSize -1;i++){

                slMetStatus = html.getElementById("l_com_sirionlabs_model_MasterSlacategory").child(1).child(i).child(5).getElementsByClass("listTdDivFont").text();;

                slMetStatusList.add(slMetStatus);
            }

            for(String slMet : slMetStatusList){

                if(!expectedSLMetStatus.contains(slMet)){
                    customAssert.assertTrue(false,"sl met status " + slMet + " not found in the expected List Of Sl met status");
                }
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    @AfterClass
    public void afterClass(){

        AdminHelper adminHelper = new AdminHelper();
        adminHelper.loginWithEndUser();
    }


}
