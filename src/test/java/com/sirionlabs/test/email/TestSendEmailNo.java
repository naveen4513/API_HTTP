package com.sirionlabs.test.email;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.dbHelper.EmailActionDbHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

import java.sql.SQLException;

import static com.sirionlabs.api.inboundEmailAction.InboundEmailAction.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestSendEmailNo {


    private static String configFilePath = null;
    public static int contractId;
    private static String configFileName = null;
    private static String contract_seq_Id = null;
    private final static Logger logger = LoggerFactory.getLogger(TestSendEmailNo.class);
    private String testingType;
    private static int count ;
    private static String defaultUserEmail;
    private static String englishLanguage;
    public static String workflowTaskId;
    private static String reviewEmailSubject;



    @Parameters({"TestingType"})
    @BeforeClass(groups = { "minor" })
    public void beforeClass(String testingType){
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");
        workflowTaskId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "workflowtaskid");
        defaultUserEmail  = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "defaultuserEmail");
        reviewEmailSubject = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "reviewemailsubject");
        englishLanguage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "englishlanguage");
    }

    @BeforeMethod(groups = { "minor" })
    public void clearEmailTable() {

        EmailActionDbHelper.cleanSystemEmailsTable();
    }

    @Test(groups = { "minor" }, priority = 1, description = "Triggering workflow email by changing status alert and checking reception of email language wise", enabled = true)
    public void testWorkflowMailSendEmailCheck() throws SQLException {

        logger.info("Executing testWorkflowMailSendEmailCheck() ");
        CustomAssert csAssert = new CustomAssert();

        contractId = createContract();
        if( contractId == -1) {
            logger.info("Unable to Create a contract for testing.");
            Assert.fail("Unable to Create a contract for testing");
        }
        changeWorkflowStateOfContract(contractId);
        try {
            count = Integer.parseInt(EmailActionDbHelper.getEmailCountInDB());

            if(count == 1) {

                String englishEmail = EmailActionDbHelper.fetchUserEmailwithSpecificLanguage(englishLanguage, reviewEmailSubject);
                csAssert.assertEquals(englishEmail, defaultUserEmail);


            } else {
                csAssert.assertTrue(false, "Test case failed as Send Email flag is not working");
            }
        } catch (Exception e) {
            logger.info("Not able to receive emails in system_emails table.");
            Assert.fail("Not able to receive emails in system_emails table.");
        }

        if(contractId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }

        csAssert.assertAll();
    }

    // Creating a new contract with particular stakeholders
    private int createContract() {

        logger.info("Creating Contract for Flow [{}]", "contract flow for email i18n");
        String contractResponse = Contract.createContract("src/test/resources/Helper/EntityCreation/Contract", "contract.cfg", "src/test/resources/Helper/EntityCreation/Contract", "contractExtraFields.cfg", "send email false",
                true);
        contractId = CreateEntity.getNewEntityId(contractResponse, "send email false");
        return contractId;
    }

    // Change the workflow state of the contract
    private void changeWorkflowStateOfContract(int contractId) {

        // Fetching the client_entity_seq_id for the newly created contract
        contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));
        String checkAuthToken = Check.authorization;
        // Changing the workflow state of the workflow for the newly created contract
        String payload = "{\"entityId\":" + contractId + ",\"entityTypeId\":61 ,\"taskId\":"+workflowTaskId+" ,\"createdOn\":\"12-12-2030 12:59:10\"}";
        postInboundEmailActionAPI(getApiPath(), getHeaders(), payload);
        // Waiting for 5 seconds as it may take some time for the DB to get updated
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }





}
