package com.sirionlabs.test.calendar;

import com.sirionlabs.api.calendar.CalendarData;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.governancebody.AdhocMeeting;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.Action;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityCreation.Invoice;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import static com.sirionlabs.helper.api.TestAPIBase.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestCalendarMisc extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCalendarMisc.class);
    private String configFilePath = null;
    private String configFileName = null;
    private String dateFormat = null;
    private String calendarA = "false";
    private int entityId;
    private int month = 8;
    private int year =  2020;
    private String expectedEndDate = "16-09-2020";
    private String expectedStatus = null;
    private String payload = null, url = null;
    private int entityTypeId = 18;
    private APIResponse response = null;
    private int gbEntityTypeId = 86;
    private int cgbEntityTypeId =  87;


    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CalendarConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("CalendarDataConfigFileName");
        dateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateformat");
        calendarA = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "calendarA");
    }

    @Test(enabled = true)
    public void testC10357  () throws ParseException {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Setting all Calendar Months to Test Calendar Data API.");

        // Create a new action from the selected parent contract
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c10357", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Checking for Newly Created status
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Submitting the Action
        expectedStatus = "Approved";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Submit", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Closing the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Close API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Close", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Archiving the Action
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Action
        logger.info("Hitting the Restore API");
        if(entityTypeId==18) {
            response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
        }

        // Moving the Action to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/onhold", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/activate", getHeaders(), payload).getResponse();
        }

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10341  () throws ParseException {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Setting all Calendar Months to Test Calendar Data API.");

        // Create a new action from the selected parent contract
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c10341", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Checking for Newly Created status
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Submitting the Action
        expectedStatus = "Approved";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Submit", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Closing the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Close API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Close", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Archiving the Action
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Action
        logger.info("Hitting the Restore API");
        if(entityTypeId==18) {
            response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
        }

        // Moving the Action to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/onhold", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/activate", getHeaders(), payload).getResponse();
        }

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10359  () throws ParseException {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Setting all Calendar Months to Test Calendar Data API.");

        // Create a new action from the selected parent contract
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c10341", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Checking for Newly Created status
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Submitting the Action
        expectedStatus = "Approved";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Submit", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Closing the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Close API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Close", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Archiving the Action
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Action
        logger.info("Hitting the Restore API");
        if(entityTypeId==18) {
            response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
        }

        // Moving the Action to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/onhold", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/activate", getHeaders(), payload).getResponse();
        }

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10370  () throws ParseException {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Setting all Calendar Months to Test Calendar Data API.");

        // Create a new action from the selected parent contract
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c10370", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Checking for Newly Created status
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Submitting the Action
        expectedStatus = "Approved";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Submit", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Closing the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Close API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Close", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Archiving the Action
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Action
        logger.info("Hitting the Restore API");
        if(entityTypeId==18) {
            response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
        }

        // Moving the Action to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/onhold", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/activate", getHeaders(), payload).getResponse();
        }

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10371  () throws ParseException {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Setting all Calendar Months to Test Calendar Data API.");

        // Create a new action from the selected parent contract
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c10371", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Checking for Newly Created status
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Submitting the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        /*// Closing the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Close API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Close", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));*/

        // Archiving the Action
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Action
        logger.info("Hitting the Restore API");
        if(entityTypeId==18) {
            response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
        }

        // Moving the Action to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/onhold", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/activate", getHeaders(), payload).getResponse();
        }

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10340  () throws ParseException {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Setting all Calendar Months to Test Calendar Data API.");

        // Create a new action from the selected parent contract
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c10340", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Checking for Newly Created status
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Submitting the Action
        expectedStatus = "Approved";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Submit", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Closing the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Close API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Close", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Archiving the Action
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Action
        logger.info("Hitting the Restore API");
        if(entityTypeId==18) {
            response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
        }

        // Moving the Action to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/onhold", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/activate", getHeaders(), payload).getResponse();
        }

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10334  () throws ParseException {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Setting all Calendar Months to Test Calendar Data API.");

        // Create a new action from the selected parent contract
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c10334", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Checking for Newly Created status
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Submitting the Action
        expectedStatus = "Approved";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Submit", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Closing the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Close API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Close", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Archiving the Action
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Action
        logger.info("Hitting the Restore API");
        if(entityTypeId==18) {
            response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
        }

        // Moving the Action to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/onhold", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/activate", getHeaders(), payload).getResponse();
        }

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10372  () throws ParseException {
        CustomAssert csAssert = new CustomAssert();

        // Create a new action from the selected parent Invoice
        String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", "c10372", true);
        entityId = CreateEntity.getNewEntityId(actionResponseString, "action");

        // Checking for Newly Created status
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Submitting the Action
        expectedStatus = "Approved";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Submit", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Closing the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Close API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Close", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Action
        expectedStatus = "Submitted";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        if(entityTypeId==18) {
            changeWFStep(entityId, "Approve", csAssert);
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Archiving the Action
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Action
        logger.info("Hitting the Restore API");
        if(entityTypeId==18) {
            response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
        }

        // Moving the Action to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/onhold", getHeaders(), payload).getResponse();
        }
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        if(entityTypeId == 18) {
            response = executor.post("/actionitemmgmts/activate", getHeaders(), payload).getResponse();
        }

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10420  () throws ParseException {
        WorkflowActionsHelper wfHelper = new WorkflowActionsHelper();
        CustomAssert csAssert = new CustomAssert();
        entityTypeId =67;

        // Create a new Invoice from the selected Parent
        String responseString = Invoice.createInvoice("src/test/resources/Helper/EntityCreation/Invoice", "invoice.cfg", "src/test/resources/Helper/EntityCreation/Invoice", "invoiceExtraFields.cfg", "c10420", true);
        entityId = CreateEntity.getNewEntityId(responseString, "invoice");

        // Checking for Newly Created Invoice
        expectedStatus = "Newly Created";
        logger.info("Verifying for status : {}", expectedStatus);
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Invoice
        expectedStatus = "Approved";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Approve API for entity : {}", entityId);
        wfHelper.performWorkflowAction(entityTypeId, entityId, "ApproveInvoice");
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Approving the Invoice
        expectedStatus = "Active";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Submit API for entity : {}", entityId);
        wfHelper.performWorkflowAction(entityTypeId, entityId, "InvoicetobePaid");
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Archiving the Invoice
        expectedStatus = "Archived";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Archive API for entityId:{}", entityId);
        payload = createPayload(entityId);

        response = executor.post("/baseInvoice/archive", getHeaders(), payload).getResponse();
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Restoring the Invoice
        logger.info("Hitting the Restore API");
        response = executor.post("/baseInvoice/restore", getHeaders(), payload).getResponse();

        // Moving the Invoice to On-hold
        expectedStatus = "On Hold";
        logger.info("Verifying for status : {}", expectedStatus);
        logger.info("Hitting the Onhold API for entityId:{}", entityId);
        payload = createPayload(entityId);

        response = executor.post("/baseInvoice/onhold", getHeaders(), payload).getResponse();
        verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

        // Activating the Action
        response = executor.post("/baseInvoice/activate", getHeaders(), payload).getResponse();

        if(entityId!= -1) {
            EntityOperationsHelper.deleteEntityRecord("actions", entityId);
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testC10407  () throws ParseException {
        WorkflowActionsHelper wfHelper = new WorkflowActionsHelper();
        CustomAssert csAssert = new CustomAssert();
        entityTypeId =87;
        int gbEntityId = 0;
        dateFormat = "MM-dd-yyyy";

        // Create a new GB and then a CGB
        logger.info("create Gb and Adhoc meeting");
        gbEntityId = createGB(csAssert, "c10395");
        entityId = createAdhocCGB(gbEntityId, dateFormat, gbEntityTypeId, csAssert);

        try{
            // Checking for Newly Created CGB
            expectedStatus = "Upcoming Meeting";
            logger.info("Verifying for status : {}", expectedStatus);
            verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

            // Archiving the CGB
            expectedStatus = "Archived";
            logger.info("Verifying for status : {}", expectedStatus);
            logger.info("Hitting the Archive API for entityId:{}", entityId);
            payload = createPayload(entityId);

            response = executor.post("/governancebodychild/archive", getHeaders(), payload).getResponse();
            verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

            // Restoring the CGB
            logger.info("Hitting the Restore API");
            response = executor.post("/governancebodychild/restore", getHeaders(), payload).getResponse();

            // Moving the CGB to On-hold
            expectedStatus = "On Hold";
            logger.info("Verifying for status : {}", expectedStatus);
            logger.info("Hitting the Onhold API for entityId:{}", entityId);
            payload = createPayload(entityId);

            response = executor.post("/governancebodychild/onhold", getHeaders(), payload).getResponse();
            verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

            // Activating the CGB
            response = executor.post("/governancebodychild/activate", getHeaders(), payload).getResponse();

            // Starting the meeting
            expectedStatus = "Meeting Started";
            logger.info("Verifying for status : {}", expectedStatus);
            logger.info("Hitting the Approve API for entity : {}", entityId);
            wfHelper.performWorkflowAction(entityTypeId, entityId, "StartMeeting");
            verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));

            // Ending the meeting
            expectedStatus = "Meeting Completed";
            logger.info("Verifying for status : {}", expectedStatus);
            logger.info("Hitting the Submit API for entity : {}", entityId);
            wfHelper.performWorkflowAction(entityTypeId, entityId, "EndMeeting");
            verifyCalendar(csAssert,expectedStatus, String.valueOf(entityTypeId));


        } catch (Exception e) {
            csAssert.assertTrue(false, "error in testC10407 " + e.getMessage());
        } finally {
            logger.info("******************GB and CGB Deleted***********************************");
            EntityOperationsHelper.deleteEntityRecord("governance body meetings", entityId);
            EntityOperationsHelper.deleteEntityRecord("governance body", gbEntityId);
        }
        csAssert.assertAll();
    }

    public void verifyCalendar(CustomAssert csAssert , String expectedStatus, String entityTypeId){
        CalendarData calDataObj = new CalendarData();
        calDataObj.hitCalendarData(month, year, calendarA);
        String response = calDataObj.getCalendarDataJsonStr();
        boolean isFound = false;
        JSONArray array = new JSONArray(response);

        for (int i = 0 ; i < array.length() ; i++){
            JSONObject jsonObject= array.getJSONObject(i);
            if(Integer.parseInt(jsonObject.getString("id"))==entityId && jsonObject.getString("entityTypeId").equals(entityTypeId)) {
                isFound = true;
                long dueDate = jsonObject.getLong("start");
                String actualDueDate = DateUtils.getDateFromEpoch(Long.parseLong(dueDate + "000"), "dd-MM-yyyy");
                csAssert.assertEquals(actualDueDate,expectedEndDate, "Date mismatch for status "+expectedStatus+"");
                String actualStatus = jsonObject.getString("entityStatus");
                csAssert.assertEquals(actualStatus, expectedStatus, "Status mismatch for status "+expectedStatus+"");
                break;
            }
        }
        csAssert.assertFalse(!isFound, "Entity "+ entityId +" of Type "+entityTypeId+" was not present in the Calendar Data. It should be.");
    }

    // Create Payload
    private String createPayload ( int entityId){
        // Hit show page page
        String showResponse = ShowHelper.getShowResponse(entityTypeId, entityId);
        // Create payload from the response
        JSONObject obj = new JSONObject(showResponse);
        String payload = "{ \"body\": { \"data\": " + obj.getJSONObject("body").getJSONObject("data").toString() + "  }  }";
        return payload;
    }

    // Change the workflow state of the contract
    private void changeWFStep ( int entityId, String status , CustomAssert csAssert){

        url = getNextWorkflowURL(entityId, status);
        payload = createPayload(entityId);
        APIResponse response = executor.post(url, getHeaders(), payload).getResponse();
        String expectedResponseBody = response.getResponseBody();
        JSONObject responseObject = new JSONObject(expectedResponseBody);
        String actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        csAssert.assertEquals(actualStatus, "success");

    }

    // Get the workflow state of the contract
    private String getNextWorkflowURL ( int entityId, String name){

        String actionsResponse = Actions.getActionsV3Response(entityTypeId, entityId);
        JSONObject obj = new JSONObject(actionsResponse);
        String actionURL = Actions.getAPIForActionV3(actionsResponse, name);
        return actionURL;

    }

    // get headers
    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    // Create a GB
    private int createGB(CustomAssert customAssert, String flow) {
        logger.info("***********************************creating GB*************************************");
        try {
            boolean isLocal = true;
            String gbResponse = GovernanceBody.createGB(flow, isLocal);
            if (ParseJsonResponse.validJsonResponse(gbResponse)) {
                int gbEntityId = CreateEntity.getNewEntityId(gbResponse);
                logger.info("Gb successfully created with ID ->" + gbEntityId);
                return gbEntityId;
            }
        } catch (Exception e) {
            logger.error("GB is not creating");
            customAssert.assertTrue(false, "Exception while GB is creating");
        }
        return 0;
    }

    // Create an adhoc CGB
    private int createAdhocCGB(int gbEntityId, String dateFormat, int gbEntityTypeId, CustomAssert customAssert) {
        logger.info("********************************creating CGB********************************");
        try {
            AdhocMeeting meeting = new AdhocMeeting();
            String adhocMeetingResponse = meeting.hitAdhocMeetingApi(String.valueOf(gbEntityId), "09-15-2020", "21:00", "Asia/Kolkata (GMT +05:30)", "30 Min", "delhi");

            if (adhocMeetingResponse.contains("Meeting Scheduled")) {
                logger.info("Adhoc meeting created");
                // getting meeting id
                TabListData listData = new TabListData();
                String gb_res = listData.hitTabListData(Integer.valueOf("213"), gbEntityTypeId, gbEntityId);
                List<String> meetingIds = ListDataHelper.getColumnIds(gb_res);
                logger.info("meeting created is :  " + meetingIds.get(0));
                return Integer.parseInt(meetingIds.get(0));
            } else {
                customAssert.assertTrue(false, "Adhoc meeting not created");
            }
        } catch (Exception e) {
            logger.error("CGB is not creating");
            customAssert.assertTrue(false, "CGB is not creating");
        }
        return 0;
    }
}