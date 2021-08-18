package com.sirionlabs.test.cdr;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.ContractTemplate;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;


public class TestCT_WFNonWFActions extends TestAPIBase {

    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Boolean deleteEntity = true;
    public static int ctId;
    private static Integer contractTemplateEntityTypeId;
    public int entityTypeId = 140;
    private String testingType;
    private final static Logger logger = LoggerFactory.getLogger(TestCT_WFNonWFActions.class);


    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateCreationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        contractTemplateEntityTypeId = ConfigureConstantFields.getEntityIdByName("contract templates");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;

    }

    @Test(priority = 1, description = "Non-Workflow actions on CT", enabled = true)
    public void testNonWFActionCT() {

        // Creating a new CT
        String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "flow 1",
                false);
        ctId = CreateEntity.getNewEntityId(createResponse, "contract templates");

        logger.info("Executing testNonWFActionCT()");

        CustomAssert csAssert = new CustomAssert();

        String payload = createPayload(ctId);

        // Hitting the Archive API
        APIResponse response = executor.post("/contracttemplate/archive",getHeaders(), payload).getResponse();
        String expectedResponseBody = response.getResponseBody();
        JSONObject responseObject = new JSONObject(expectedResponseBody);

        String actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        String actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcontracttemplate/"+ ctId +"");

        logger.info("Executing the test cases to restore the newly created CT() ");

        // Hitting the Restore API
        response = executor.post("/contracttemplate/restore",getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcontracttemplate/"+ ctId +"");

        logger.info("Executing the test cases to delete the newly created CT() ");

        // Hitting the Delete API
        response = executor.post("/contracttemplate/delete",getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertTrue(actualSource.contains("deletedId")," Deletion of CT failed due to some reason.");

        csAssert.assertAll();


    }

    @Test(priority = 2, description = "Workflow actions on CT", enabled = true)
    public void testWFActionCT() {

        CustomAssert csAssert = new CustomAssert();

        // Creating a new CT
        String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "flow 1",
                false);
        ctId = CreateEntity.getNewEntityId(createResponse, "contract templates");

        logger.info("Executing testWFActionCT() ");

        String payload = createPayload(ctId);

        // Changing the Workflow status
        String url = getNextWorkflowURL(ctId, "SendForApproval");
        APIResponse response = executor.post(url,getHeaders(), payload).getResponse();

        String expectedResponseBody = response.getResponseBody();
        JSONObject responseObject = new JSONObject(expectedResponseBody);

        String  actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        String actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcontracttemplate/"+ ctId +"");

        logger.info("Changing the Workflow status again");

        url = getNextWorkflowURL(ctId, "Approve");
        response = executor.post(url,getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcontracttemplate/"+ ctId +"");
        csAssert.assertAll();


    }

    // Create Headers
    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        try {
            headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Content-Type", "application/json;charset=UTF-8");
            headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        } catch (Exception e) {
            logger.info("Exception occurred in creating headers for request");
        }
        return headers;
    }

    // Create Payload
    private String createPayload(int ctId) {
        // Hit show page page of the newly created CT
        String showResponse =         ShowHelper.getShowResponse(entityTypeId,ctId);
        // Create payload from the response
        JSONObject obj = new JSONObject(showResponse);
        String payload =  "{ \"body\": { \"data\": "+obj.getJSONObject("body").getJSONObject("data").toString() +"  }  }";
        return payload;
    }


    // Change the workflow state of the contract
    private String getNextWorkflowURL(int ctId, String name) {


        String actionsResponse = Actions.getActionsV3Response(entityTypeId, ctId);
        JSONObject obj = new JSONObject(actionsResponse);
        String actionURL = Actions.getAPIForActionV3(actionsResponse, name);

        return actionURL;

    }

}


