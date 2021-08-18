package com.sirionlabs.test.cdr;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.ContractTemplateStructure;
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
public class TestCTS_WFNonWFActions extends TestAPIBase {


    private static String configFilePath = null;
    private static String configFileName = null;
    public int ctsId;
    public int entityTypeId = 139;
    private String testingType;
    private final static Logger logger = LoggerFactory.getLogger(TestCTS_WFNonWFActions.class);


    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("preSignatureRegressionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("preSignatureRegressionConfigFileName");

    }

    @Test(priority = 1, description = "Non-Workflow actions on CTS", enabled = true)
    public void testNonWFActionCTS() {

        // Create new Contract Template Structure
        String createdContractTemplateStructureResponse = ContractTemplateStructure.createContractTemplateStructure(null,null,configFilePath,configFileName,"contract template structure",false);
        ctsId = PreSignatureHelper.getNewlyCreatedId(createdContractTemplateStructureResponse);

        logger.info("Executing testNonWFActionCTS() ");

        CustomAssert csAssert = new CustomAssert();

        String payload = createPayload(ctsId);

        // Hitting the Archive API
        APIResponse response = executor.post("/contracttemplatestructure/archive",getHeaders(), payload).getResponse();
        String expectedResponseBody = response.getResponseBody();
        JSONObject responseObject = new JSONObject(expectedResponseBody);

        String actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        String actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcontracttemplatestructure/"+ctsId+"");

        logger.info("Executing the test cases to restore the newly created CTS() ");

        // Hitting the Restore API
        response = executor.post("/contracttemplatestructure/restore",getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcontracttemplatestructure/"+ctsId+"");

        logger.info("Executing the test cases to delete the newly created CTS() ");

        // Hitting the Delete API
        response = executor.post("/contracttemplatestructure/delete",getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertTrue(actualSource.contains("deletedId")," Deletion of CTS failed due to some reason.");

        csAssert.assertAll();


    }

    @Test(priority = 2, description = "Workflow actions on CTS", enabled = true)
    public void testWFActionCTS() {

        CustomAssert csAssert = new CustomAssert();

        // Create new Contract Template Structure
        String createdContractTemplateStructureResponse = ContractTemplateStructure.createContractTemplateStructure(null,null,configFilePath,configFileName,"contract template structure",false);
        int ctsId = PreSignatureHelper.getNewlyCreatedId(createdContractTemplateStructureResponse);

        logger.info("Executing testWFActionCTS() ");

        String payload = createPayload(ctsId);

        // Changing the Workflow status
        String url = getNextWorkflowURL(ctsId, "SendForClientReview");
        APIResponse response = executor.post(url,getHeaders(), payload).getResponse();

        String expectedResponseBody = response.getResponseBody();
        JSONObject responseObject = new JSONObject(expectedResponseBody);

        String  actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        String actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcontracttemplatestructure/"+ctsId+"");

        logger.info("Changing the Workflow status again");

        url = getNextWorkflowURL(ctsId, "Reject");
        response = executor.post(url,getHeaders(), payload).getResponse();

        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);

        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        actualSource = responseObject.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString();

        csAssert.assertEquals(actualStatus,"success");
        csAssert.assertEquals(actualSource,"/show/tblcontracttemplatestructure/"+ctsId+"");
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
    private String createPayload(int ctsId) {
        // Hit show page page of the newly created CTS
        String showResponse =         ShowHelper.getShowResponse(entityTypeId,ctsId);
        // Create payload from the response
        JSONObject obj = new JSONObject(showResponse);
        String payload =  "{ \"body\": { \"data\": "+obj.getJSONObject("body").getJSONObject("data").toString() +"  }  }";
        return payload;
    }

    // Change the workflow state of the contract
    private String getNextWorkflowURL(int ctsId, String name) {

        String actionsResponse = Actions.getActionsV3Response(entityTypeId, ctsId);
        JSONObject obj = new JSONObject(actionsResponse);
        String actionURL = Actions.getAPIForActionV3(actionsResponse, name);

        return actionURL;

    }

}
