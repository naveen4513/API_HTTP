package com.sirionlabs.test.inboundEmailAction;

import com.sirionlabs.api.EmailTokenAuthentication.EmailTokenAuthentication;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.EmailActionDbHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.*;

import static com.sirionlabs.api.inboundEmailAction.InboundEmailAction.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestInboundEmailAction {


    private static String configFilePath = null;
    public static int contractId;
    private static String configFileName = null;
    private static String contract_seq_Id = null;
    private static String expectedOutput = null;
    public static String workflowTaskId;
    private String testingType;
    private final static Logger logger = LoggerFactory.getLogger(TestInboundEmailAction.class);


    @Parameters({"TestingType"})
    @BeforeClass(groups = { "minor" })
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");
        workflowTaskId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "workflowtaskid");

    }

    @BeforeMethod(groups = { "minor" })
    public void clearEmailTable() {
        EmailActionDbHelper.cleanSystemEmailsTable();
    }

    @Test(groups = { "minor" }, priority = 1, description = "Positive scenario where workflow state gets changed with the API hit and second hit with same params doesn't change the state", enabled = true)
    public void testInboundActionEmailFlow() {

        logger.info("Executing testInboundActionEmailFlow() ");

        CustomAssert csAssert = new CustomAssert();
        APIResponse apiResponse = null;

        contractId = createContract();

        if( contractId == -1) {
            logger.info("Unable to Create a contract for testing.");
            Assert.fail("Unable to Create a contract for testing");
        }
        try {
            apiResponse = createGenerateEmailAuth(String.valueOf(contractId));
        } catch (Exception e) {
            logger.info("Not able to receive emails in system_emails table.");
            Assert.fail("Not able to receive emails in system_emails table.");
        }

        // Fetching the client_entity_seq_id for the newly created contract
        contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));

        logger.info("Extracting EmailAuth from emailTokenAuthentication API");
        String emailAuthToken = EmailTokenAuthentication.getEmailAuth(apiResponse);

        logger.info("Extracting Payload Key-Value pair wise");
        String payloadFromResponse = JSONUtility.parseJson(apiResponse.getResponseBody(), "$.payload").toString();
        payloadFromResponse = payloadFromResponse.replaceAll("\\\\", "");

        logger.info("Extracting items for Payload for workflow-next-task-execution/execute API");
        String entityTypeId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityTypeId").toString();
        String entityId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityId").toString();
        String createdOn = JSONUtility.parseJson(payloadFromResponse, "$.params.createdOn").toString();
        String taskId = JSONUtility.parseJson(payloadFromResponse, "$.params.taskId").toString();

        String payload = "{\"entityId\":" + entityId + ",\"entityTypeId\":" + entityTypeId + ",\"taskId\":" + taskId + ",\"createdOn\":\" " + createdOn + " \"}";

        logger.info("Hitting the API under test");
        APIResponse response = postInboundEmailActionAPI(getApiPath(), getHeaders(), payload);

        logger.info("Verification of the response");
        String actualresponseBody = response.getResponseBody();
        String message = JSONUtility.parseJson(actualresponseBody, "$.header.response.properties.notification").toString();
        String status = JSONUtility.parseJson(actualresponseBody, "$.header.response.status").toString();

        expectedOutput = "Action performed successfully. Please visit <a id=\"hrefElemId\" href=\"/#/show/tblcontracts/" + entityId + "\" style=\"color: #9E866A; font-size: 11px;\">CO" + contract_seq_Id + "</a> for more details.";
        csAssert.assertEquals(message, expectedOutput);
        csAssert.assertEquals(status, "success");
        csAssert.assertEquals(response.getResponseCode().toString(), "200");

        logger.info("Hitting the API under test again to check that status shouldn't change again");
        response = postInboundEmailActionAPI(getApiPath(), getHeaders(), payload);

        logger.info("Verification of the response");
        actualresponseBody = response.getResponseBody();
        message = JSONUtility.parseJson(actualresponseBody, "$.header.response.properties.notification").toString();
        status = JSONUtility.parseJson(actualresponseBody, "$.header.response.status").toString();

        expectedOutput = "The action performed is no longer valid. Please visit <a id=\"hrefElemId\" href=\"/#/show/tblcontracts/" + entityId + "\" style=\"color: #9E866A; font-size: 11px;\">CO" + contract_seq_Id + "</a> for more details.";
        csAssert.assertEquals(message, expectedOutput);
        csAssert.assertEquals(status, "validationError");
        csAssert.assertEquals(response.getResponseCode().toString(), "200");

        csAssert.assertAll();

        if(contractId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
    }

    @Test(priority = 2, description = "Entity Id doesn't exist", enabled = true)
    public void testInboundActionEmailFlowWithInvalidContract() {

        logger.info("Executing testInboundActionEmailFlowWithInvalidContract() ");

        CustomAssert csAssert = new CustomAssert();
        APIResponse apiResponse = null;

        contractId = createContract();

        if( contractId == -1) {
            logger.info("Unable to Create a contract for testing.");
            Assert.fail("Unable to Create a contract for testing");
        }
        try {
            apiResponse = createGenerateEmailAuth(String.valueOf(contractId));
        } catch (Exception e) {
            logger.info("Not able to receive emails in system_emails table.");
            Assert.fail("Not able to receive emails in system_emails table.");
        }

        // Fetching the client_entity_seq_id for the newly created contract
        contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));

        logger.info("Extracting EmailAuth from emailTokenAuthentication API");
        String emailAuthToken = EmailTokenAuthentication.getEmailAuth(apiResponse);

        logger.info("Extracting Payload Key-Value pair wise");
        String payloadFromResponse = JSONUtility.parseJson(apiResponse.getResponseBody(), "$.payload").toString();
        payloadFromResponse = payloadFromResponse.replaceAll("\\\\", "");

        logger.info("Extracting items for Payload for workflow-next-task-execution/execute API");
        String entityTypeId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityTypeId").toString();
        String createdOn = JSONUtility.parseJson(payloadFromResponse, "$.params.createdOn").toString();
        String taskId = JSONUtility.parseJson(payloadFromResponse, "$.params.taskId").toString();

        String payload = "{\"entityId\":" + 232323232 + ",\"entityTypeId\":" + entityTypeId + ",\"taskId\":" + taskId + ",\"createdOn\":\" " + createdOn + " \"}";


        logger.info("Hitting the API under test");
        APIResponse response = postInboundEmailActionAPI(getApiPath(), getHeaders(), payload);

        logger.info("Verification of the response");
        String actualresponseBody = response.getResponseBody();
        String message = JSONUtility.parseJson(actualresponseBody, "$.header.response.errorMessage").toString();
        String status = JSONUtility.parseJson(actualresponseBody, "$.header.response.status").toString();

        csAssert.assertEquals(message, "Either you do not have the required permissions or requested page does not exist anymore.");
        csAssert.assertEquals(status, "applicationError");
        csAssert.assertEquals(response.getResponseCode().toString(), "200");

        csAssert.assertAll();

        if(contractId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
    }

    @Test(priority = 3, description = "Creation date is in the past", enabled = true)
    public void testInboundActionEmailFlowWithPastCreationDate() {

        logger.info("Executing testInboundActionEmailFlowWithPastCreationDate()");

        CustomAssert csAssert = new CustomAssert();
        APIResponse apiResponse = null;

        contractId = createContract();

        if( contractId == -1) {
            logger.info("Unable to Create a contract for testing.");
            Assert.fail("Unable to Create a contract for testing");
        }
        try {
            apiResponse = createGenerateEmailAuth(String.valueOf(contractId));
        } catch (Exception e) {
            logger.info("Not able to receive emails in system_emails table.");
            Assert.fail("Not able to receive emails in system_emails table.");
        }

        // Fetching the client_entity_seq_id for the newly created contract
        contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));

        logger.info("Extracting EmailAuth from emailTokenAuthentication API");
        String emailAuthToken = EmailTokenAuthentication.getEmailAuth(apiResponse);

        logger.info("Extracting Payload Key-Value pair wise");
        String payloadFromResponse = JSONUtility.parseJson(apiResponse.getResponseBody(), "$.payload").toString();
        payloadFromResponse = payloadFromResponse.replaceAll("\\\\", "");

        logger.info("Extracting items for Payload for workflow-next-task-execution/execute API");
        String entityTypeId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityTypeId").toString();
        String entityId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityId").toString();
        String taskId = JSONUtility.parseJson(payloadFromResponse, "$.params.taskId").toString();

        String payload = "{\"entityId\":" + entityId + ",\"entityTypeId\":" + entityTypeId + ",\"taskId\":" + taskId + ",\"createdOn\":\" " + "11-04-2018 12:00:00" + " \"}";


        logger.info("Hitting the API under test");
        APIResponse response = postInboundEmailActionAPI(getApiPath(), getHeaders(), payload);

        logger.info("Verification of the response");
        String actualresponseBody = response.getResponseBody();
        String message = JSONUtility.parseJson(actualresponseBody, "$.header.response.properties.notification").toString();
        String status = JSONUtility.parseJson(actualresponseBody, "$.header.response.status").toString();

        expectedOutput = "The action performed is no longer valid. Please visit <a id=\"hrefElemId\" href=\"/#/show/tblcontracts/" + entityId + "\" style=\"color: #9E866A; font-size: 11px;\">CO" + contract_seq_Id + "</a> for more details.";
        csAssert.assertEquals(message, expectedOutput);
        csAssert.assertEquals(status, "validationError");
        csAssert.assertEquals(response.getResponseCode().toString(), "200");

        csAssert.assertAll();

        if(contractId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
    }


    @Test(priority = 4, description = "Task Id is not the next valid taskId", enabled = true)
    public void testInboundActionEmailFlowWithInvalidTaskId() {

        logger.info("Executing testInboundActionEmailFlowWithInvalidTaskId() ");

        CustomAssert csAssert = new CustomAssert();
        APIResponse apiResponse = null;

        contractId = createContract();

        if( contractId == -1) {
            logger.info("Unable to Create a contract for testing.");
            Assert.fail("Unable to Create a contract for testing");
        }
        try {
            apiResponse = createGenerateEmailAuth(String.valueOf(contractId));
        } catch (Exception e) {
            logger.info("Not able to receive emails in system_emails table.");
            Assert.fail("Not able to receive emails in system_emails table.");
        }

        // Fetching the client_entity_seq_id for the newly created contract
        contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));

        logger.info("Extracting EmailAuth from emailTokenAuthentication API");
        String emailAuthToken = EmailTokenAuthentication.getEmailAuth(apiResponse);

        logger.info("Extracting Payload Key-Value pair wise");
        String payloadFromResponse = JSONUtility.parseJson(apiResponse.getResponseBody(), "$.payload").toString();
        payloadFromResponse = payloadFromResponse.replaceAll("\\\\", "");

        logger.info("Extracting items for Payload for workflow-next-task-execution/execute API");
        String entityTypeId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityTypeId").toString();
        String entityId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityId").toString();
        String createdOn = JSONUtility.parseJson(payloadFromResponse, "$.params.createdOn").toString();

        String payload = "{\"entityId\":" + entityId + ",\"entityTypeId\":" + entityTypeId + ",\"taskId\":" + "112706" + ",\"createdOn\":\" " + createdOn + " \"}";

        logger.info("Hitting the API under test");
        APIResponse response = postInboundEmailActionAPI(getApiPath(), getHeaders(), payload);

        logger.info("Verification of the response");
        String actualresponseBody = response.getResponseBody();
        String message = JSONUtility.parseJson(actualresponseBody, "$.header.response.properties.notification").toString();
        String status = JSONUtility.parseJson(actualresponseBody, "$.header.response.status").toString();

        expectedOutput = "The action performed is no longer valid. Please visit <a id=\"hrefElemId\" href=\"/#/show/tblcontracts/" + entityId + "\" style=\"color: #9E866A; font-size: 11px;\">CO" + contract_seq_Id + "</a> for more details.";
        csAssert.assertEquals(message, expectedOutput);
        csAssert.assertEquals(status, "validationError");
        csAssert.assertEquals(response.getResponseCode().toString(), "200");

        csAssert.assertAll();

        if(contractId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
    }

    @Test(priority = 5, description = "Auth Token used is invalid", enabled = true)
    public void testInboundActionEmailFlowWithInvalidAuthToken() {

        logger.info("Executing testInboundActionEmailFlowWithInvalidAuthToken() ");

        CustomAssert csAssert = new CustomAssert();
        APIResponse apiResponse = null;

        contractId = createContract();

        if( contractId == -1) {
            logger.info("Unable to Create a contract for testing.");
            Assert.fail("Unable to Create a contract for testing");
        }
        try {
            apiResponse = createGenerateEmailAuth(String.valueOf(contractId));
        } catch (Exception e) {
            logger.info("Not able to receive emails in system_emails table.");
            Assert.fail("Not able to receive emails in system_emails table.");
        }

        // Fetching the client_entity_seq_id for the newly created contract
        contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));

        logger.info("Extracting EmailAuth from emailTokenAuthentication API");
        String emailAuthToken = EmailTokenAuthentication.getEmailAuth(apiResponse);

        logger.info("Extracting Payload Key-Value pair wise");
        String payloadFromResponse = JSONUtility.parseJson(apiResponse.getResponseBody(), "$.payload").toString();
        payloadFromResponse = payloadFromResponse.replaceAll("\\\\", "");

        logger.info("Extracting items for Payload for workflow-next-task-execution/execute API");
        String entityTypeId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityTypeId").toString();
        String entityId = JSONUtility.parseJson(payloadFromResponse, "$.params.entityId").toString();
        String createdOn = JSONUtility.parseJson(payloadFromResponse, "$.params.createdOn").toString();
        String taskId = JSONUtility.parseJson(payloadFromResponse, "$.params.taskId").toString();

        String payload = "{\"entityId\":" + entityId + ",\"entityTypeId\":" + entityTypeId + ",\"taskId\":" + taskId + ",\"createdOn\":\" " + createdOn + " \"}";

        logger.info("Hitting the API under test");
        APIResponse response = postInboundEmailActionAPI(getApiPath(), getHeadersWithInvalidAuth(emailAuthToken), payload);

        logger.info("Verification of the response");
        csAssert.assertEquals(response.getResponseCode().toString(), "400");

        csAssert.assertAll();

        if(contractId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
    }


    // Create a new MSA contract > Change its workflow state > Observe the entry in DB > Fetch the authToken from the email Body > Return the response of the emailTokenAuthentication API
    private int createContract() {
        logger.info("Validating Contract Creation Flow [{}]", "");

        CustomAssert csAssert = new CustomAssert();

        // Creating a new contract with particular stake holders for Supplier Credit
        logger.info("Creating Contract for Flow [{}]", "contract flow inbound email action");
        String contractResponse = Contract.createContract("src/test/resources/Helper/EntityCreation/Contract", "contract.cfg", "src/test/resources/Helper/EntityCreation/Contract", "contractExtraFields.cfg", "mic 80 flow 1 for inbound email action",
                true);
        contractId = CreateEntity.getNewEntityId(contractResponse, "contracts");

        return contractId;
    }


    // Change workflow state of the newly created contract > Observe the entry in DB > Fetch the authToken from the email Body > Return the response of the emailTokenAuthentication API
    private APIResponse createGenerateEmailAuth(String contractId) {

        // Changing the workflow state of the workflow for the newly created contract
        String payload = "{\"entityId\":" + contractId + ",\"entityTypeId\":61 ,\"taskId\":" + workflowTaskId + " ,\"createdOn\":\"12-12-2030 12:59:10\"}";
        postInboundEmailActionAPI(getApiPath(), getHeaders(), payload);

        // Waiting for 2 seconds as it may take some time for the DB to get updated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        // Fetching the authToken from the system_emails table
        String authToken = EmailActionDbHelper.getAuthToken();

        // Hitting the emailTokenAuthentication API to get the taskId, createdOn and the access token of LoggedIn User
        APIResponse apiResponse = EmailTokenAuthentication.getResponse(authToken);

        return apiResponse;
    }

}