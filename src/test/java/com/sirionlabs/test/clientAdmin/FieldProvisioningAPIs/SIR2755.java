package com.sirionlabs.test.clientAdmin.FieldProvisioningAPIs;

import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import gnu.cajo.invoke.Client;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SIR2755 extends TestAPIBase {

    private final Logger logger = LoggerFactory.getLogger(SIR2755.class);
    private final String configFilePath = "src/test/resources/TestConfig/clientAdmin/FieldProvisioningAPIs";
    private final String configFileName = "SIR2755.cfg";

    @BeforeClass
    public void beforeClass(){
        new AdminHelper().loginWithClientAdminUser();
    }

    @Test(enabled = true)
    public void C140865(){
        CustomAssert customAssert = new CustomAssert();

        try{

            Map<String,String> requestParams = new HashMap<>();
            String settingId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140865","settingId");
            String entityTypeId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140865","entityTypeId");
            requestParams.put("settingId", settingId!=null?settingId:"");
            requestParams.put("entityTypeId",entityTypeId!=null?entityTypeId:"");

            APIValidator apiValidator = executor.get("/clientFieldProvisionings/new",requestParams,true);
            APIResponse apiResponse = apiValidator.getResponse();

            logger.info("Response body {}",apiResponse.getResponseBody());

            customAssert.assertTrue(apiResponse.getResponseCode()==200,"Response code doesn't match");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(apiResponse.getResponseBody())&&apiResponse.getResponseBody().contains("success"),"Response body not correct");

        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred "+ Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }
    @Test(enabled = true)
    public void C140869(){
        CustomAssert customAssert = new CustomAssert();

        try{

            Map<String,String> requestParams = new HashMap<>();
            String settingId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140869","settingId");
            String entityTypeId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140869","entityTypeId");
            String relationId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140869","relationId");
            requestParams.put("settingId", settingId!=null?settingId:"");
            requestParams.put("entityTypeId",entityTypeId!=null?entityTypeId:"");
            requestParams.put("relationId",relationId!=null?relationId:"");

            APIValidator apiValidator = executor.get("clientFieldProvisionings/getTemplate",requestParams,true);
            APIResponse apiResponse = apiValidator.getResponse();

            logger.info("Response body {}",apiResponse.getResponseBody());

            customAssert.assertTrue(apiResponse.getResponseCode()==200,"Response code doesn't match");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(apiResponse.getResponseBody())&&!ParseJsonResponse.containsApplicationError(apiResponse.getResponseBody()),"Response body not correct");

        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred "+ Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = true) //C140904 also covered
    public void C140870(){
        CustomAssert customAssert = new CustomAssert();

        try{

            Map<String,String> requestParams = new HashMap<>();
            String settingId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140870","settingId");
            requestParams.put("settingId", settingId!=null?settingId:"");
            String actionId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140870","actionId");
            String defaultTemplateIdForServiceData = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140870","defaultTemplateIdForServiceData");

            assert defaultTemplateIdForServiceData!=null:"defaulttemplateidforservicedata is null";

            APIValidator apiValidator = executor.get("clientFieldProvisionings/"+actionId+"/"+defaultTemplateIdForServiceData,requestParams,true);
            APIResponse apiResponse = apiValidator.getResponse();

            logger.info("Response body {}",apiResponse.getResponseBody());

            customAssert.assertTrue(apiResponse.getResponseCode()==200,"Response code doesn't match");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(apiResponse.getResponseBody())&&apiResponse.getResponseBody().contains("success"),"Response body not correct");

            JSONObject jsonObject = new JSONObject(apiResponse.getResponseBody());
            JSONObject  jsonObjectTemp = new JSONObject().put("data",jsonObject.getJSONObject("body").getJSONObject("data"));
            JSONObject jsonObjectPayload = new JSONObject().put("body",jsonObjectTemp);

            ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
            listRendererTabListData.hitListRendererTabListData(497,64,Integer.parseInt(defaultTemplateIdForServiceData),"{\"filterMap\":{}}",true);
            String response = listRendererTabListData.getTabListDataJsonStr();

            jsonObjectPayload.getJSONObject("body").getJSONObject("data").getJSONObject("clientFieldProvisioningDataV2").put("values",new JSONObject(response).getJSONArray("data"));

            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json;charset=UTF-8");
            apiValidator = executor.post("/clientFieldProvisionings/"+actionId+"?settingId="+settingId,headers,jsonObjectPayload.toString(),null);
            apiResponse = apiValidator.getResponse();

            logger.info("Response body {}",apiResponse.getResponseBody());

            customAssert.assertTrue(apiResponse.getResponseCode()==200,"Response code doesn't match");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(apiResponse.getResponseBody())&&apiResponse.getResponseBody().contains("success"),"Response body not correct");

        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred "+ Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void C140899(){
        CustomAssert customAssert = new CustomAssert();

        try{

            Map<String,String> requestParams = new HashMap<>();
            String entityTypeId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140899","entityTypeId");
            String provisioningId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140899","provisioningId");
            String relationId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140899","relationId");
            requestParams.put("provisioningId", provisioningId!=null?provisioningId:"");
            requestParams.put("entityTypeId",entityTypeId!=null?entityTypeId:"");
            requestParams.put("relationId",relationId!=null?relationId:"");

            APIValidator apiValidator = executor.get("clientFieldProvisionings/getSyncedFields/"+entityTypeId+"/"+provisioningId,requestParams,true);
            APIResponse apiResponse = apiValidator.getResponse();

            logger.info("Response body {}",apiResponse.getResponseBody());

            customAssert.assertTrue(apiResponse.getResponseCode()==200,"Response code doesn't match");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(apiResponse.getResponseBody())&&!ParseJsonResponse.containsApplicationError(apiResponse.getResponseBody()),"Response body not correct");

        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred "+ Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void C140901(){
        CustomAssert customAssert = new CustomAssert();

        try{

            Map<String,String> requestParams = new HashMap<>();
            String entityTypeId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140901","entityTypeId");
            String query = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140901","query");
            String settingId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140901","settingId");
            requestParams.put("query", query!=null?query:"");
            requestParams.put("entityTypeId",entityTypeId!=null?entityTypeId:"");
            requestParams.put("settingId",settingId!=null?settingId:"");

            APIValidator apiValidator = executor.get("clientFieldProvisionings/getRelationsForProvisioningCreation/"+entityTypeId+"/"+settingId+"/"+query,requestParams,true);
            APIResponse apiResponse = apiValidator.getResponse();

            logger.info("Response body {}",apiResponse.getResponseBody());

            customAssert.assertTrue(apiResponse.getResponseCode()==200,"Response code doesn't match");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(apiResponse.getResponseBody())&&!ParseJsonResponse.containsApplicationError(apiResponse.getResponseBody()),"Response body not correct");

        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred "+ Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void C140900(){
        CustomAssert customAssert = new CustomAssert();

        try{

            Map<String,String> requestParams = new HashMap<>();
            String entityTypeId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"C140900","entityTypeId");
            requestParams.put("entityTypeId",entityTypeId!=null?entityTypeId:"");

            APIValidator apiValidator = executor.get("clientFieldProvisionings/getDependentFields/"+entityTypeId,requestParams,true);
            APIResponse apiResponse = apiValidator.getResponse();

            logger.info("Response body {}",apiResponse.getResponseBody());

            customAssert.assertTrue(apiResponse.getResponseCode()==200,"Response code doesn't match");
            customAssert.assertTrue(ParseJsonResponse.validJsonResponse(apiResponse.getResponseBody())&&!ParseJsonResponse.containsApplicationError(apiResponse.getResponseBody()),"Response body not correct");

        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred "+ Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }


}
