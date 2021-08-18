package com.sirionlabs.test.notificationAlert;

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
public class TestNotificationAlert {


    private static String configFilePath = null;
    public static int contractId;
    private static String configFileName = null;
    private static String contract_seq_Id = null;
    private final static Logger logger = LoggerFactory.getLogger(TestNotificationAlert.class);
    private String testingType;
    private static Boolean languageChanged1 = null;
    private static Boolean languageChanged2 = null;
    private static String defaultUserEmail;
    private static String user2Email;
    private static String englishLanguage;
    private static String russianLanguage;
    private static String serbianLanguage;
    private static String expirationDateEmailMessage;
    private static String archiveEmailMessage;
    private static String workflowTaskId;



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
        serbianLanguage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "serbianlanguage");
        expirationDateEmailMessage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "expirationdateemailmessage");
        archiveEmailMessage  = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "archiveemailmessage");

    }

    @BeforeMethod(groups = { "minor" })
    public void clearEmailTable() {

        EmailActionDbHelper.cleanSystemEmailsTable();
    }

    @Test(groups = { "minor" }, priority = 1, description = "Triggering notification alert and checking reception of email language wise", enabled = true)
    public void testNotificationAlertLanguageCheck() throws SQLException {

        logger.info("Executing testNotificationAlertLanguageCheck() ");
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
                String englishEmail = EmailActionDbHelper.fetchUserEmailwithSpecificLanguage(englishLanguage, expirationDateEmailMessage);
                String russianEmail = EmailActionDbHelper.fetchUserEmailwithSpecificLanguage(russianLanguage, expirationDateEmailMessage);

                csAssert.assertEquals(englishEmail, defaultUserEmail);
                csAssert.assertEquals(russianEmail, user2Email);
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

    @Test(priority = 2, description = "Triggering notification alert and checking if English language alert is received by default if the alert is not configured for receiver's language", enabled = true)
    public void testNotificationAlertDefaultLanguageCheck() throws SQLException {

        logger.info("Executing testNotificationAlertDefaultDefaultLanguageCheck() ");
        CustomAssert csAssert = new CustomAssert();

        languageChanged1 = EmailActionDbHelper.updateUserLanguage(user2Email, serbianLanguage);
        languageChanged2 = EmailActionDbHelper.updateUserLanguage(defaultUserEmail, englishLanguage);

        if(languageChanged1 && languageChanged2) {
            contractId = createContract();
            if( contractId == -1) {
                logger.info("Unable to Create a contract for testing.");
                Assert.fail("Unable to Create a contract for testing");
            }

            changeWorkflowStateOfContract(contractId);
            try {
                String subjectUser1 = EmailActionDbHelper.fetchEmailSubjectForGivenUser(defaultUserEmail);
                String subjectUser2 = EmailActionDbHelper.fetchEmailSubjectForGivenUser(user2Email);

                csAssert.assertEquals(subjectUser1, subjectUser2);
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

    @Test(priority = 3, description = "Triggering time based notification alert and checking reception of email language wise", enabled = false)
    public void testTimeBasedNotificationAlertLanguageCheck() throws SQLException, InterruptedException {

        logger.info("Executing testTimeBasedNotificationAlertLanguageCheck() ");
        CustomAssert csAssert = new CustomAssert();

        try {

            // Delete all existing contracts
            EmailActionDbHelper.contractDeletion("true");

            languageChanged1 = EmailActionDbHelper.updateUserLanguage(user2Email, russianLanguage);
            languageChanged2 = EmailActionDbHelper.updateUserLanguage(defaultUserEmail, englishLanguage);

            if(languageChanged1 && languageChanged2) {
                contractId = createContract();
                if( contractId == -1) {
                    logger.info("Unable to Create a contract for testing.");
                    Assert.fail("Unable to Create a contract for testing");
                }

                // Fetching the client_entity_seq_id for the newly created contract
                contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));

                // Wait for the alert to get triggered as scheduler picks it up
                //    Thread.sleep(120000);

                String englishEmail = EmailActionDbHelper.fetchUserEmailwithSpecificLanguage(englishLanguage, archiveEmailMessage);
                String russianEmail = EmailActionDbHelper.fetchUserEmailwithSpecificLanguage(russianLanguage, archiveEmailMessage);

                csAssert.assertEquals(englishEmail, defaultUserEmail);
                csAssert.assertEquals(russianEmail, user2Email);

                if(contractId!= -1) {
                    EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
                }

            } else {
                csAssert.assertTrue(false, "Couldn't change the language for the user under test.");
            }
        } catch(Exception e) {
            // Undo deletion of all existing contracts
            EmailActionDbHelper.contractDeletion("false");

            csAssert.assertTrue(false, "Something failed during test execution testTimeBasedNotificationAlertLanguageCheck()");
        }

        csAssert.assertAll();
    }


    // Creating a new contract with particular stake holders and expiration date in the past for Supplier Credit
    private int createContract() {

        logger.info("Creating Contract for Flow [{}]", "contract flow for Notification Alert");
        String contractResponse = Contract.createContract("src/test/resources/Helper/EntityCreation/Contract", "contract.cfg", "src/test/resources/Helper/EntityCreation/Contract", "contractExtraFields.cfg", "notification alert",
                true);
        contractId = CreateEntity.getNewEntityId(contractResponse, "contracts");
        return contractId;
    }

    // Change the workflow state of the contract
    private void changeWorkflowStateOfContract(int contractId) {

        // Fetching the client_entity_seq_id for the newly created contract
        contract_seq_Id = EmailActionDbHelper.getClientEntitySeqId(String.valueOf(contractId));
        // String checkAuthToken = Check.authorization;
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
