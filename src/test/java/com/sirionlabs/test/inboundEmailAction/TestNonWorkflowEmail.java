package com.sirionlabs.test.inboundEmailAction;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.EmailActionDbHelper;
import com.sirionlabs.test.flowdown.action.TestFlowDownForAction;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;
import static com.sirionlabs.api.inboundEmailAction.InboundEmailAction.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestNonWorkflowEmail {


    private static String configFilePath = null;
    public static int contractId;
    private static String configFileName = null;
    private final static Logger logger = LoggerFactory.getLogger(TestNonWorkflowEmail.class);

    @BeforeClass()
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");

    }

    @BeforeMethod()
    public void clearEmailTable() {
        EmailActionDbHelper.cleanSystemEmailsTable();
    }

    //C10497 C13701 C10513
    @Test(enabled = true)
    public void testC10485() throws InterruptedException {

        logger.info("Executing testC10485() ");
        CustomAssert csAssert = new CustomAssert();

        // Get contract Id
        String entityId =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "nonwfmail", "contractid");
        String contract_seq_Id =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "nonwfmail", "contractseqid");
        String defaultEmail =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "defaultuseremail");

        // Archive the contract
        logger.info("Archiving the Contract");
        String action = "archive";
        String payload = createPayload(Integer.parseInt(entityId));
        APIResponse response = executor.post("/contracts/archive", getHeaders(), payload).getResponse();

        Thread.sleep(3000);

        // Observe the email and verify
        String from_email = EmailActionDbHelper.getDataForParam("from_email", defaultEmail, action);
        String to_email = EmailActionDbHelper.getDataForParam("to_email", defaultEmail, action);
        String bcc_email = EmailActionDbHelper.getDataForParam("bcc_email", defaultEmail, action );
        String subject = EmailActionDbHelper.getDataForParam("subject", defaultEmail,action);
        String body  = EmailActionDbHelper.getDataForParam("body", defaultEmail, action);

        // Verification
        csAssert.assertEquals(from_email, "admin@sirionqa.office");
        csAssert.assertEquals(to_email, defaultEmail);
        csAssert.assertEquals(bcc_email, "admin@sirionqa.office");
        csAssert.assertEquals(subject, "English ArchiveMS Supplier,, document (#"+contract_seq_Id+") has been archived");
        csAssert.assertTrue(body.contains("<html><head>"), "Body's content are incorrect");

        // Empty the emails table
        EmailActionDbHelper.cleanSystemEmailsTable();

        // Restore the contract
        logger.info("Restoring the Contract");
        action = "restore";
        payload = createPayload(Integer.parseInt(entityId));
        response = executor.post("/contracts/restore", getHeaders(), payload).getResponse();

        Thread.sleep(3000);

        // Observe the email and verify
        from_email = EmailActionDbHelper.getDataForParam("from_email", defaultEmail, action);
        to_email = EmailActionDbHelper.getDataForParam("to_email", defaultEmail, action);
        bcc_email = EmailActionDbHelper.getDataForParam("bcc_email", defaultEmail, action );
        subject = EmailActionDbHelper.getDataForParam("subject", defaultEmail,action);
        body  = EmailActionDbHelper.getDataForParam("body", defaultEmail, action);

        // Verification
        csAssert.assertEquals(from_email, "admin@sirionqa.office");
        csAssert.assertEquals(to_email, defaultEmail);
        csAssert.assertEquals(bcc_email, "admin@sirionqa.office");
        csAssert.assertEquals(subject, "English Restore MS Supplier,, document (#"+contract_seq_Id+") has been restored");
        csAssert.assertTrue(body.contains("<html><head>"), "Body's content are incorrect");

        csAssert.assertAll();

    }

    //C10485
    @Test(enabled = true)
    public void testC10499() {

        logger.info("Executing testC10499() ");
        CustomAssert csAssert = new CustomAssert();

        // Get Action Id
        String entityId =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "nonwfmail", "actionid");
        String action_seq_Id =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "nonwfmail", "actionseqid");
        String defaultEmail =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "defaultuseremail");

        // Edit the Action
        logger.info("Editing the Action");
        Edit edit = new Edit();
        String editResponse = edit.hitEdit("actions", Integer.parseInt(entityId));
        String editPayload = TestFlowDownForAction.createEditPayload(editResponse);

        try {
            editResponse = edit.hitEdit("actions",editPayload);

            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String action = "modified";

        // Observe the email and verify
        String from_email = EmailActionDbHelper.getDataForParam("from_email", defaultEmail, action);
        String to_email = EmailActionDbHelper.getDataForParam("to_email", defaultEmail, action);
        String bcc_email = EmailActionDbHelper.getDataForParam("bcc_email", defaultEmail, action );
        String subject = EmailActionDbHelper.getDataForParam("subject", defaultEmail,action);
        String body  = EmailActionDbHelper.getDataForParam("body", defaultEmail, action);

        // Verification
        csAssert.assertEquals(from_email, "admin@sirionqa.office");
        csAssert.assertEquals(to_email, defaultEmail);
        csAssert.assertEquals(bcc_email, "admin@sirionqa.office");
        csAssert.assertEquals(subject, "PacifiCorp, action (#"+action_seq_Id+") has been modified");
        csAssert.assertTrue(body.contains("<html><head>"), "Body's content are incorrect");

        csAssert.assertAll();

    }

    // Create Payload
    private String createPayload ( int entityId){
        // Hit show page page
        String showResponse = ShowHelper.getShowResponse(61, entityId);
        // Create payload from the response
        JSONObject obj = new JSONObject(showResponse);
        String payload = "{ \"body\": { \"data\": " + obj.getJSONObject("body").getJSONObject("data").toString() + "  }  }";
        return payload;
    }
}