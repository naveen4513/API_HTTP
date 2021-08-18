package com.sirionlabs.test.cdr;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestCDR_WFNonWFActions extends TestAPIBase {


    private static String configFilePath = null;
    public static int cdrId;
    private static String configFileName = null;
    public static String workflowTaskId;
    public int entityTypeId = 160;
    private String testingType;
    private final static Logger logger = LoggerFactory.getLogger(TestCDR_WFNonWFActions.class);


    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("preSignatureRegressionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("preSignatureRegressionConfigFileName");
        workflowTaskId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "workflowtaskid");


    }

    @Test(priority = 1, description = "Non-Workflow actions on CDR", enabled = true)
    public void testNonWFActionCDR() {

        // Creating a new CDR
        String contractDraftRequestResponseString = ContractDraftRequest.createCDR(null,null,configFilePath,configFileName,"contract draft request fields",false);
        cdrId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);

        logger.info("Executing testNonWFActionCDR() ");

        CustomAssert csAssert = new CustomAssert();

        String payload = createPayload(cdrId);

        // Hitting the Archive API
        APIResponse response = executor.post("/cdr/archive",getHeaders(), payload).getResponse();
        String expectedResponseBody = response.getResponseBody();
        JSONObject responseObject = new JSONObject(expectedResponseBody);

        String actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        String actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcdr/"+cdrId+"");

        logger.info("Executing the test cases to restore the newly created CDR() ");

        // Hitting the Restore API
        response = executor.post("/cdr/restore",getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcdr/"+cdrId+"");

        logger.info("Executing the test cases to delete the newly created CDR() ");

        // Hitting the Delete API
        response = executor.post("/cdr/delete",getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertTrue(actualSource.contains("deletedId")," Deletion of CDR failed due to some reason.");

        csAssert.assertAll();


    }

    @Test(priority = 2, description = "Workflow actions on CDR", enabled = true)
    public void testWFActionCDR() {

        CustomAssert csAssert = new CustomAssert();

        // Creating a new CDR
        String contractDraftRequestResponseString = ContractDraftRequest.createCDR(null,null,configFilePath,configFileName,"contract draft request fields",false);
        cdrId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);

        logger.info("Executing testWFActionCDR() ");

        String payload = createPayload(cdrId);

        // Changing the Workflow status
        String url = getNextWorkflowURL(cdrId, "SendForClientReview");
        APIResponse response = executor.post(url,getHeaders(), payload).getResponse();

        String expectedResponseBody = response.getResponseBody();
        JSONObject responseObject = new JSONObject(expectedResponseBody);

        String  actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        String actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcdr/"+cdrId+"");

        logger.info("Changing the Workflow status again");

        url = getNextWorkflowURL(cdrId, "TestInternationalization");
        response = executor.post(url,getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcdr/"+cdrId+"");
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
    private String createPayload(int cdrId) {
        // Hit show page page of the newly created CDR
        String showResponse =         ShowHelper.getShowResponse(entityTypeId,cdrId);
        // Create payload from the response
        JSONObject obj = new JSONObject(showResponse);
        String payload =  "{ \"body\": { \"data\": "+obj.getJSONObject("body").getJSONObject("data").toString() +"  }  }";
        return payload;
    }

    // Change the workflow state of the contract
    private String getNextWorkflowURL(int cdrId, String name) {

        String actionsResponse = Actions.getActionsV3Response(entityTypeId, cdrId);
        JSONObject obj = new JSONObject(actionsResponse);
        String actionURL = Actions.getAPIForActionV3(actionsResponse, name);

        return actionURL;

    }

}
