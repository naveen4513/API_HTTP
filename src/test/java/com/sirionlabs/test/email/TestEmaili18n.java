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
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

import java.sql.SQLException;

import static com.sirionlabs.api.inboundEmailAction.InboundEmailAction.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestEmaili18n {


    private static String configFilePath = null;
    public static int contractId;
    private static String configFileName = null;
    private static String contract_seq_Id = null;
    private final static Logger logger = LoggerFactory.getLogger(TestEmaili18n.class);
    private String testingType;
    private static Boolean languageChanged1 = null;
    private static Boolean languageChanged2 = null;
    private static String defaultUserEmail;
    private static String user2Email;
    private static String englishLanguage;
    private static String russianLanguage;
    public static String workflowTaskId;
    private static String reviewEmailSubject;
    private static String expectedsubjectClientName;
    private static String expectedbodyContractName;




    @Parameters({"TestingType"})
    @BeforeClass(groups = { "minor" })
    public void beforeClass(String testingType) throws ConfigurationException {
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");
        workflowTaskId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "workflowtaskid");
        defaultUserEmail  = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "defaultuserEmail");
        user2Email = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "user2email");
        englishLanguage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "englishlanguage");
        russianLanguage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "russianlanguage");
        reviewEmailSubject = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "reviewemailsubject");
        expectedsubjectClientName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "expectedsubjectclientname");
        expectedbodyContractName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "expectedbodycontractname");
    }

    @BeforeMethod(groups = { "minor" })
    public void clearEmailTable() {

        EmailActionDbHelper.cleanSystemEmailsTable();
    }

    @Test(groups = { "minor" }, priority = 1, description = "Triggering workflow email by changing status alert and checking reception of email language wise", enabled = true)
    public void testWorkflowMaili18nCheck() throws SQLException {

        logger.info("Executing testWorkflowMaili18nCheck() ");
        CustomAssert csAssert = new CustomAssert();
        languageChanged1 = EmailActionDbHelper.updateUserLanguage(user2Email, russianLanguage);
        languageChanged2 = EmailActionDbHelper.updateUserLanguage(defaultUserEmail, englishLanguage);

        if(languageChanged1 && languageChanged2) {
            contractId = createContract();
            if( contractId == -1) {
                logger.info("Unable to Create a contract for testing.");
                Assert.fail("Unable to Create a contract for testing");
            }
            changeWorkflowStateOfContract(contractId);
            try {
                String englishEmail = EmailActionDbHelper.fetchUserEmailwithSpecificLanguage(englishLanguage, reviewEmailSubject);
                String russianEmail = EmailActionDbHelper.fetchUserEmailwithSpecificLanguage(russianLanguage, reviewEmailSubject);

                // Verification of C10398
                String actualSubjectClientName = EmailActionDbHelper.fetchClientNameFromEmailSubject(defaultUserEmail);
                String actualBodyContractName = EmailActionDbHelper.fetchContractNameFromEmailBody(defaultUserEmail);


                csAssert.assertEquals(englishEmail, defaultUserEmail);
                csAssert.assertEquals(russianEmail, user2Email);
                csAssert.assertEquals(actualSubjectClientName, expectedsubjectClientName);
                csAssert.assertEquals(actualBodyContractName, expectedbodyContractName);
            } catch (Exception e) {
                logger.info("Not able to receive emails in system_emails table.");
                Assert.fail("Not able to receive emails in system_emails table.");
            }

            if(contractId!= -1) {
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }
        } else {
            csAssert.assertTrue(false, "Couldn't change the language for the user under test.");
        }
        csAssert.assertAll();
    }

    // Creating a new contract with particular stakeholders
    private int createContract() {

        logger.info("Creating Contract for Flow [{}]", "contract flow for email i18n");
        String contractResponse = Contract.createContract("src/test/resources/Helper/EntityCreation/Contract", "contract.cfg", "src/test/resources/Helper/EntityCreation/Contract", "contractExtraFields.cfg", "mic 80 flow 1 for inbound email action",
                true);
        contractId = CreateEntity.getNewEntityId(contractResponse, "contracts");
        return contractId;
    }

    // Change the workflow state of the contract
    private void changeWorkflowStateOfContract(int contractId) {

        // Fetching the client_entity_seq_id for the newly created contract
        contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));
        //  String checkAuthToken = Check.authorization;

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
