package com.sirionlabs.test.bulkAction;

import com.sirionlabs.api.bulkaction.BulkActionCreate;
import com.sirionlabs.api.bulkaction.BulkActionSave;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestBulkAction extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkAction.class);
    private String configFilePath = null;
    private String configFileName = null;
    private String flowsConfigFileName = null;
    private Boolean killAllSchedulerTasks = false;
    private Boolean waitForScheduler = true;
    private Boolean checkShowPageIsBlocked = true;
    private Integer auditLogTabId = -1;
    private Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private Map<String, String> expectedStatusOnShowPageMap;
    private Boolean verifyEmail = false;

    private BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();
    private DefaultUserListMetadataHelper defaultHelperObj = new DefaultUserListMetadataHelper();

    @BeforeClass
    public void beforeClass() throws ConfigurationException, IOException, ExecutionException, InterruptedException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkActionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkActionConfigFileName");
        flowsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsConfigFileName");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killAllSchedulerTasks");
        if (temp != null && temp.trim().equalsIgnoreCase("true"))
            killAllSchedulerTasks = true;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitForScheduler");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            waitForScheduler = false;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            failTestIfJobNotCompletedWithinSchedulerTimeOut = false;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "checkShowPageIsBlocked");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            checkShowPageIsBlocked = false;

        auditLogTabId = TabListDataHelper.getIdForTab("audit log");

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "verifyEmail");
        if (temp != null && temp.equalsIgnoreCase("true"))
            verifyEmail = true;

        expectedStatusOnShowPageMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "expected status on show page mapping");
    }

    @DataProvider
    public Object[][] dataProviderForBulkAction() {
        List<Object[]> allTestData = new ArrayList<>();

        logger.info("Setting all Flows to Test.");
        List<String> allFlowsToTest = getFlowsToTest();
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest.trim()});
        }
        logger.info("Total Flows to Test : {}", allTestData.size());
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForBulkAction")
    public void testBulkAction(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Verifying Bulk Action for Flow: [{}]", flowToTest);
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, flowsConfigFileName, flowToTest);
            String entityName = flowProperties.get("entity").trim();

            logger.info("Getting EntityTypeId for Entity: {}", entityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            if (!defaultHelperObj.hasPermissionToPerformBulkAction(entityName)) {
                throw new SkipException("Doesn't have Permission to Perform Bulk Action on Entity " + entityName + ". Hence skipping flow " + flowToTest);
            }

            String fromStatus = flowProperties.get("fromstatus").trim();
            String toStatus = flowProperties.get("tostatus").trim();

            String toStatusLabel;
            if(flowProperties.containsKey("tostatuslabel")) {
                toStatusLabel= flowProperties.get("tostatuslabel").trim();
            }else {
                toStatusLabel = toStatus;
            }


            int listId = ConfigureConstantFields.getListIdForEntity(entityName);

            List<Integer> recordsToDelete = new ArrayList<>();
            String entityIds = getEntityIdsForFlow(flowToTest, flowProperties, entityName, entityTypeId, listId, fromStatus, toStatus, recordsToDelete);

            if (entityIds == null) {
                throw new SkipException("Couldn't get Entity Ids for Flow [" + flowToTest + "]");
            }

            //Kill All Scheduler Tasks if Flag is On.
            if (killAllSchedulerTasks) {
                logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                killAllSchedulerTasks();
            }

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String payloadForSave = getPayloadForSave(entityIds, entityTypeId, listId, fromStatus, toStatus);

            String[] recordIdsForShow = entityIds.split(",");

            List<Integer> recordIdsList = new ArrayList<>();
            for (String entityId : recordIdsForShow) {
                recordIdsList.add(Integer.parseInt(entityId));
            }

            Map<Integer, String> expectedResultsMap = getExpectedResultForAllRecords(recordIdsList, entityTypeId, toStatusLabel, flowToTest, csAssert);

            logger.info("Hitting BulkActionSave Api for EntityTypeId {}", entityTypeId);
            String bulkActionSaveResponse = executor.post(BulkActionSave.getApiPath(), BulkActionSave.getHeaders(), payloadForSave).getResponse().getResponseBody();

            if (bulkActionSaveResponse != null) {
                //Verify the response of BulkAction Save
                logger.info("Verifying BulkActionSave Response for EntityTypeId {}", entityTypeId);
                logger.info("Actual Response received: {}", bulkActionSaveResponse);
                bulkActionSaveResponse = bulkActionSaveResponse.toLowerCase();
                boolean saveSuccessfulResponse = bulkActionSaveResponse.contains("successfully submitted");
                if (!saveSuccessfulResponse) {
                    csAssert.assertTrue(false, "BulkAction Save Response received for Flow [" + flowToTest + "] and EntityTypeId " + entityTypeId +
                            " does not match required response. Hence not proceeding further. Response [" + bulkActionSaveResponse + "]");
                } else if (waitForScheduler) {
                    logger.info("Hitting Fetch API to Get Bulk Action Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Action Job for Flow [{}]", flowToTest);
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    waitForScheduler(flowToTest, entityTypeId, recordIdsForShow, newTaskId, csAssert);

                    logger.info("Hitting Fetch API to get Status of Bulk Action Job");
                    fetchObj.hitFetch();
                    String bulkActionJobStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);

                    if (bulkActionJobStatus != null && bulkActionJobStatus.trim().equalsIgnoreCase("Completed")) {
                        if (UserTasksHelper.ifAllRecordsPassedInTask(newTaskId)) {
                            if (expectedResultsMap != null) {
                                for (Map.Entry<Integer, String> resultMap : expectedResultsMap.entrySet()) {
                                    String expectedResult = resultMap.getValue();

                                    if (expectedResult != null) {
                                        if (expectedResult.equalsIgnoreCase("success")) {
                                            String expectedStatusOnShowPage = expectedStatusOnShowPageMap.getOrDefault(toStatus.trim().toLowerCase(), toStatus);

                                            //Check Status on Show Page
                                            checkStatusOnShowPage(flowToTest, entityName, entityTypeId, resultMap.getKey(), fromStatus,
                                                    expectedStatusOnShowPage, csAssert);

                                            //Verify Audit Log
                                            verifyAuditLog(flowToTest, entityName, entityTypeId, resultMap.getKey(), expectedStatusOnShowPage, csAssert);
                                        } else {
                                            //Verify that Bulk Action Job Failed.
                                            logger.info("Verifying that Bulk Action Job failed.");
                                            csAssert.assertTrue(UserTasksHelper.anyRecordFailedInTask(fetchObj.getFetchJsonStr(), newTaskId),
                                                    "No Record failed in Bulk Action Job for Flow " + flowToTest);
                                        }
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Couldn't get Expected Results Map for Flow " + flowToTest);
                            }

                            //Verify Email Part
                            if (verifyEmail) {
                                verifyEmailPart(flowToTest, entityName, recordIdsList, expectedResultsMap, csAssert);
                            }
                        } else {
                            //Get Error Message
                            String bulkRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                            String errorMessage = bulkHelperObj.getErrorMessagesForBulkEditRequestId(bulkRequestId);

                            csAssert.assertFalse(true, "Bulk Action Scheduler Job failed for Flow [" + flowToTest + "] and Entity " +
                                    entityName + ". Error Message: [" + errorMessage + "] Entity Ids: " + entityIds);
                        }
                    } else {
                        if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                            throw new SkipException("Bulk Action Job for Flow [" + flowToTest + "] is not completed yet and Flag \'" +
                                    "FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to True");
                        } else {
                            throw new SkipException("Bulk Action Job for Flow [" + flowToTest + "] is not completed yet and Flag \'" +
                                    "FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to False. Hence not Checking Status on Show Page.");
                        }
                    }
                } else {
                    logger.info("Wait for Scheduler Flag is Turned Off. Hence not checking further for Flow [{}]", flowToTest);
                }

                if (recordsToDelete.size() > 0) {
                    logger.info("Deleting Cloned Records for Flow [{}]", flowToTest);
                    EntityOperationsHelper.deleteMultipleRecords(entityName, recordsToDelete);
                }
            } else {
                throw new SkipException("Couldn't get Payload for Bulk Action Save for Flow [" + flowToTest + "]. Hence skipping test.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Verifying BulkAction. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Verifying BulkAction. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private String getEntityIdsForFlow(String flowToTest, Map<String, String> flowProperties, String entityName, int entityTypeId, int listId,
                                       String fromStatus, String toStatus, List<Integer> recordsToDelete) {
        String entityIds = null;

        try {
            boolean cloneRecords = true;

            if (flowProperties.containsKey("clone") && flowProperties.get("clone").equalsIgnoreCase("false")) {
                cloneRecords = false;
            }

            String payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
                    ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listDataOffset") + ",\"size\":" +
                    ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxRecordsForListData") + ",\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc\",\"filterJson\":{\"currentTask\":\"" + fromStatus + "\", \"nextTaskForBulk\":\"" + toStatus + "\"}}}";

            logger.info("Hitting ListRendererListData Api for EntityTypeId {}", entityTypeId);
            ListRendererListData listDataObj = new ListRendererListData();

            if (entityName.equalsIgnoreCase("contracts")) {
                listDataObj.hitListRendererListDataV2(listId, payloadForListData);
            } else {
                listDataObj.hitListRendererListData(listId, false, payloadForListData, null);
            }
            listDataObj.setListData(listDataObj.getListDataJsonStr());
            List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();

            int columnIdForBulkCheckBox;
            int columnIdForRecordId;

            if (listData.size() > 0) {
                logger.info("Getting Column Id for BulkCheckBox Column");
                columnIdForBulkCheckBox = listDataObj.getColumnIdFromColumnName("bulkCheckBox");

                logger.info("Getting Column Id for Id");
                columnIdForRecordId = listDataObj.getColumnIdFromColumnName("id");
            } else {
                logger.error("No Record found in List Data for Flow [" + flowToTest + "]");
                return null;
            }

            if (flowProperties.containsKey("recordids") && !flowProperties.get("recordids").equalsIgnoreCase("")) {
                String[] recordIdsArr = flowProperties.get("recordids").split(",");
                List<Integer> recordIdsList = new ArrayList<>();

                for (String recordId : recordIdsArr) {
                    recordIdsList.add(Integer.parseInt(recordId.trim()));
                }

                int[] indexArr = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, recordIdsList.size() - 1, recordIdsList.size());

                entityIds = setEntityIds(entityName, listId, payloadForListData, listData, indexArr, columnIdForBulkCheckBox, columnIdForRecordId,
                        recordIdsList, recordsToDelete, cloneRecords);
            } else {
                logger.info("Filtering List Data Records. i.e. Removing records which are already locked for Bulk Action.");
                List<Map<Integer, Map<String, String>>> filteredRecords = this.filterListDataRecords(flowToTest, listData, columnIdForBulkCheckBox);

                if (filteredRecords.size() > 0) {
                    int[] randomNumbersForBulkAction = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (filteredRecords.size() - 1),
                            Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxRecordsForBulkAction")));

                    ListRendererDefaultUserListMetaData listMetadataObj = new ListRendererDefaultUserListMetaData();
                    logger.info("Hitting ListRendererDefaultUserListMetaData Api for EntityTypeId {}", entityTypeId);
                    listMetadataObj.hitListRendererDefaultUserListMetadata(listId, null, "{}");
                    listMetadataObj.setFilterMetadatas(listMetadataObj.getListRendererDefaultUserListMetaDataJsonStr());
                    listMetadataObj.setColumns(listMetadataObj.getListRendererDefaultUserListMetaDataJsonStr());

                    entityIds = this.setEntityIds(entityName, listId, payloadForListData, filteredRecords, randomNumbersForBulkAction,
                            listMetadataObj.getIdFromQueryName("id"), columnIdForRecordId, null, recordsToDelete, cloneRecords);
                } else {
                    logger.error("No Record found after filtering for Flow [" + flowToTest + "]");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Entity Ids for Flow {}. {}", flowToTest, e.getMessage());
        }

        return entityIds;
    }

    private List<String> getFlowsToTest() {
        List<String> flowsToTest = new ArrayList<>();

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
                flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, flowsConfigFileName);
            } else {
                String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
                for (String flow : allFlows) {
                    if (ParseConfigFile.containsSection(configFilePath, flowsConfigFileName, flow.trim())) {
                        flowsToTest.add(flow.trim());
                    } else {
                        logger.info("Flow having name [{}] not found in Bulk Action Flows Config File.", flow.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting Flows to Test for Bulk Action Validation. {}", e.getMessage());
        }
        return flowsToTest;
    }

    private void waitForScheduler(String flowToTest, int entityTypeId, String[] entityIdsForShow, int newTaskId, CustomAssert csAssert) {
        logger.info("Waiting for Scheduler to Complete for Flow [{}].", flowToTest);
        try {
            long timeOut = 1200000;long pollingTime = 5000;
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerWaitTimeOut");
            if (temp != null && NumberUtils.isParsable(temp.trim()))
                timeOut = Long.parseLong(temp.trim());

            logger.info("Time Out for Scheduler is {} milliseconds", timeOut);
            long timeSpent = 0;

            if (newTaskId != -1) {
                logger.info("Checking if Bulk Action Task has completed or not for Flow [{}]", flowToTest);

                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerPollingTime");
                if (temp != null && NumberUtils.isParsable(temp.trim()))
                    pollingTime = Long.parseLong(temp.trim());

                while (timeSpent < timeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();

                    logger.info("Getting Status of Bulk Action Task for Flow [{}]", flowToTest);
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        logger.info("Bulk Action Task Completed for Flow [{}]", flowToTest);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Bulk Action Task is not finished yet for Flow [{}]", flowToTest);
                    }

                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("In Progress")) {
                        if (!UserTasksHelper.anyRecordFailedInTask(fetchObj.getFetchJsonStr(), newTaskId) &&
                                !UserTasksHelper.anyRecordProcessedInTask(fetchObj.getFetchJsonStr(), newTaskId)) {

                            //Verify that Show Page is not accessible for entities
                            if (checkShowPageIsBlocked) {
                                logger.info("Verifying that Show Page is Blocked for Flow [{}], EntityTypeId {} and EntityIds {}", flowToTest, entityTypeId,
                                        entityIdsForShow);
                                this.checkShowPageIsBlocked(flowToTest, entityTypeId, entityIdsForShow, csAssert);
                            }
                        } else {
                            logger.info("Bulk Action Task for Flow [{}] is In Progress but At-least One record has been processed or failed. " +
                                    "Hence Not Checking if Show Page is Blocked or not.", flowToTest);
                        }
                    } else {
                        logger.info("Bulk Action Task for Flow [{}] has not been picked by Scheduler yet.", flowToTest);
                    }
                }
            } else {
                logger.info("Couldn't get Bulk Action Task Job Id for Flow [{}]. Hence waiting for Task Time Out i.e. {}", flowToTest, timeOut);
                Thread.sleep(pollingTime);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Waiting for Scheduler to Finish for Flow [" + flowToTest + "]. " + e.getMessage());
        }
    }

    private void checkShowPageIsBlocked(String flowToTest, int entityTypeId, String[] entityIds, CustomAssert csAssert) {
        try {
            logger.info("Verifying that Show Page is blocked for Entity Type Id {} and Entity Ids {}", entityTypeId, Arrays.toString(entityIds));
            ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
            List<FutureTask<Boolean>> taskList = new ArrayList<>();

            logger.info("Total Records for EntityTypeId {} are {}", entityTypeId, entityIds.length);
            for (int i = 0; i < entityIds.length; i++) {
                final int index = i;

                FutureTask<Boolean> result = new FutureTask<>(() -> {
                    Show showObj = new Show();
                    logger.info("Hitting Show Api for Record #{} having EntityTypeId {} and Id {}", (index + 1), entityTypeId, entityIds[index]);
                    showObj.hitShow(entityTypeId, Integer.parseInt(entityIds[index]));
                    String showJsonStr = showObj.getShowJsonStr();
                    boolean showPageBlocked = showObj.isShowPageBlockedForBulkAction(showJsonStr);
                    if (!showPageBlocked) {
                        logger.error("Show Page is accessible for Record #{} having EntityTypeId {} and Id {}", (index + 1), entityTypeId, entityIds[index]);
                        csAssert.assertTrue(false, "Show Page is accessible for Record #" + (index + 1) + " having EntityTypeId " + entityTypeId +
                                " and Id " + entityIds[index]);
                    }
                    return true;
                });
                taskList.add(result);
                executor.execute(result);
            }
            for (FutureTask<Boolean> task : taskList)
                task.get();
        } catch (Exception e) {
            logger.error("Exception while Checking if Show Page is Blocked for Flow [{}]. {}", flowToTest, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Checking if Show Page is Blocked for Flow [" + flowToTest + "]. " + e.getMessage());
        }
    }

    private void checkStatusOnShowPage(String flowToTest, String entityName, int entityTypeId, Integer recordId, String fromStatus, String expectedStatus,
                                       CustomAssert csAssert) {
        try {
            String showPageStatusObject = "status";

            if (ParseConfigFile.hasProperty(configFilePath, configFileName, "entityShowPageStatusObjectMapping", entityName)) {
                showPageStatusObject = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entityShowPageStatusObjectMapping", entityName);
            }

            logger.info("Verifying that Show Page is Accessible for Flow [{}], Entity {} and Entity Id {}", flowToTest, entityName, recordId);

            Show showObj = new Show();
            logger.info("Hitting Show Api for Record Id {} for Flow [{}] having Entity {}", recordId, flowToTest, entityName);

            showObj.hitShow(entityTypeId, recordId);
            String showJsonStr = showObj.getShowJsonStr();

            if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
                boolean showPageAccessible = ShowHelper.isShowPageAccessible(showJsonStr);

                if (!showPageAccessible) {
                    throw new SkipException("Show Page is not accessible for Record Id " + recordId + " for Flow [" + flowToTest + "] having Entity " + entityName);
                }

                //Verify Current Status
                logger.info("Verifying Current Status for Record Id {} for Flow [{}]", recordId, flowToTest);

                //Special Handling for Restore Action, Activate Action
                if (expectedStatus.trim().equalsIgnoreCase("restore")) {
                    expectedStatus = getLastStatusBeforeArchive(showJsonStr, flowToTest);
                } else if (expectedStatus.trim().equalsIgnoreCase("activate")) {
                    expectedStatus = getLastStatusBeforeOnHold(showJsonStr, flowToTest);
                }

                if (expectedStatus == null) {
                    throw new SkipException("Couldn't get Expected Value for Flow [" + flowToTest + "]. Hence skipping test.");
                }

                boolean showFieldPass = showObj.verifyShowField(showJsonStr, showPageStatusObject, expectedStatus, entityTypeId);
                if (!showFieldPass) {
                    csAssert.assertTrue(false, "Record Id " + recordId + " for Flow [" + flowToTest + "] having Entity " +
                            entityName + " failed on Show Page for Field Status");
                }

                //Verify History
                verifyHistory(showJsonStr, flowToTest, entityName, fromStatus, expectedStatus, recordId, csAssert);
            } else {
                csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + ", Flow [" + flowToTest + "], Entity " +
                        entityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Verifying Status on Show Page for Record Id " + recordId + " of Flow [" +
                    flowToTest + "] and Entity " + entityName + ". " + e.getMessage());
        }
    }

    private void verifyAuditLog(String flowToTest, String entityName, int entityTypeId, Integer recordId, String expectedStatus, CustomAssert csAssert) {
        try {
            if (auditLogTabId != -1) {
                logger.info("Verifying Audit Log Entry for Flow [{}], Entity {}, Record Id {} and Action {}", flowToTest, entityName,
                        recordId, expectedStatus);

                logger.info("Hitting TabListData API for Record for Flow [{}] having Entity {} and Id {}", flowToTest, entityName, recordId);

                TabListData tabListObj = new TabListData();
                String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\"," +
                        "\"filterJson\":{}}}";
                String tabListDataResponse = tabListObj.hitTabListData(auditLogTabId, entityTypeId, recordId, payload);

                if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                    List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);

                    if (listData.size() > 0) {
                        String actionNameColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "action_name");

                        for (Map<Integer, Map<String, String>> auditLogEntry : listData) {
                            if (actionNameColumnId != null) {
                                String actualValue = auditLogEntry.get(Integer.parseInt(actionNameColumnId.trim())).get("value");

                                if (actualValue != null && actualValue.equalsIgnoreCase("Auto Update")) {
                                    continue;
                                }

                                //Handling for SL where Child SLAs are created.
                                if (entityTypeId == 14 && actualValue != null && actualValue.equalsIgnoreCase("Child SLAs Created")) {
                                    continue;
                                }

                                logger.info("Actual Action Value in Audit Log for Record Id {}, Entity {} and Flow [{}] is: {}", recordId, entityName,
                                        flowToTest, actualValue);

                                if (actualValue != null && actualValue.trim().toLowerCase().contains(expectedStatus.trim().toLowerCase())) {
                                    if (!actualValue.trim().contains("(Bulk)") && !actualValue.trim().contains("( Bulk )") && !actualValue.trim().contains("Bulk")) {
                                        csAssert.assertTrue(false, "Audit Log Action Value for Record Id " + recordId + ", Entity " + entityName +
                                                ", Flow [" + flowToTest + "] doesn't contain keyword (Bulk)");
                                    }
                                } else {
                                    csAssert.assertTrue(false, "Audit Log Action Value for Record Id " + recordId + ", Entity " + entityName +
                                            ", Flow [" + flowToTest + "] doesn't contain Expected Status " + expectedStatus);
                                }

                                break;
                            } else {
                                throw new SkipException("Couldn't get Id for Column action_name. Hence skipping test.");
                            }
                        }
                    } else {
                        throw new SkipException("Couldn't get List Data for Record Id " + recordId + ", Entity " + entityName + ", Audit Log Tab and Flow [" +
                                flowToTest + "]. Hence skipping test.");
                    }
                } else {
                    csAssert.assertTrue(false, "TabListData API for Record Id " + recordId + ", Entity " + entityName +
                            ", Audit Log Tab and Flow [" + flowToTest + "] is an Invalid JSON.");
                }
            } else {
                throw new SkipException("Couldn't get Audit Log Tab Id for Flow [" + flowToTest + "]. Hence skipping test.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Verifying Audit Log for Record Id " + recordId + " for Flow [" + flowToTest + "] and Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private String setEntityIds(String entityName, int listId, String payloadForListData, List<Map<Integer, Map<String, String>>> listData, int[] indexArray, int columnId,
                                int idColumnNo, List<Integer> recordIds, List<Integer> recordsToDelete, boolean cloneRecords) {
        String entityIds = "";
        boolean first = true;

        try {
            for (int index : indexArray) {
                Integer recordId;

                if (recordIds == null) {
                    recordId = Integer.parseInt(listData.get(index).get(columnId).get("valueId"));
                } else {
                    recordId = recordIds.get(index);
                }

                Integer newId = -1;
                Integer idToUse;

                //Clone Record
                if (cloneRecords) {
                    newId = EntityOperationsHelper.cloneRecord(entityName, recordId);
                }

                //If Record is cloned then use the newly created record otherwise use the original record.
                if (newId != -1) {
                    recordsToDelete.add(newId);
                    ListRendererListData listDataObj = new ListRendererListData();
                    listDataObj.hitListRendererListData(listId, false, payloadForListData, null);
                    String listDataResponse = listDataObj.getListDataJsonStr();

                    JSONObject jsonObj = new JSONObject(listDataResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    boolean recordIdFound = false;

                    for (int i = 0; i < jsonArr.length(); i++) {
                        String value = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");

                        if (value.contains(newId.toString())) {
                            recordIdFound = true;
                            break;
                        }
                    }

                    idToUse = recordIdFound ? newId : recordId;
                } else {
                    idToUse = recordId;
                }

                if (first) {
                    entityIds = entityIds.concat(idToUse.toString());
                    first = false;
                } else
                    entityIds = entityIds.concat("," + idToUse);
            }
        } catch (Exception e) {
            logger.error("Exception while setting Entity Ids in TestBulkAction. {}", e.getMessage());
        }
        return entityIds;
    }

    private List<Map<Integer, Map<String, String>>> filterListDataRecords(String flowToTest, List<Map<Integer, Map<String, String>>> listDataRecords,
                                                                          int columnIdForBulkCheckBox) {
        logger.info("Filtering Records for Flow [{}]", flowToTest);
        List<Map<Integer, Map<String, String>>> filteredRecords = new ArrayList<>();

        try {
            for (Map<Integer, Map<String, String>> record : listDataRecords) {
                if (columnIdForBulkCheckBox == -1 || record.get(columnIdForBulkCheckBox).get("value").toLowerCase().contains("false")) {
                    filteredRecords.add(record);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while filtering List Data Records. {}", e.getMessage());
        }
        return filteredRecords;
    }

    private void killAllSchedulerTasks() {
        UserTasksHelper.removeAllTasks();
    }

    private String getPayloadForSave(String entityIds, int entityTypeId, int listId, String fromStatus, String toStatus) {
        String payloadForSave = null;
        try {
            logger.info("Hitting Bulk Action Create API for EntityTypeId {}", entityTypeId);
            String createJsonStr = executor.get(BulkActionCreate.getApiPath(entityTypeId), BulkActionCreate.getHeaders()).getResponse().getResponseBody();

            String commentStr = "\"comment\": {";

            if (ParseJsonResponse.validJsonResponse(createJsonStr)) {
                JSONObject jsonObj = new JSONObject(createJsonStr);
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

                JSONObject commentObj = jsonObj.getJSONObject("comment");
                JSONArray jsonArr = commentObj.names();

                //Remove options from Child Objects
                int i;
                for (i = 0; i < jsonArr.length() - 1; i++) {
                    jsonObj = commentObj.getJSONObject(jsonArr.get(i).toString().trim());
                    if (jsonObj.has("options"))
                        jsonObj.remove("options");
                    commentStr = commentStr.concat("\"" + jsonArr.get(i) + "\": " + jsonObj.toString() + ",");
                }

                jsonObj = commentObj.getJSONObject(jsonArr.get(i).toString().trim());
                if (jsonObj.has("options"))
                    jsonObj.remove("options");
                commentStr += "\"" + jsonArr.get(i) + "\": " + jsonObj.toString() + "}";

                payloadForSave = "{\"entityIds\": [" + entityIds + "],\"entityTypeId\": " + entityTypeId + ",\"listId\": \"" + listId + "\",\"fromTask\": \"" +
                        fromStatus + "\",\"toTask\": \"" + toStatus + "\",\"toBeIgnoredEntityIds\": []," + commentStr + ",\"isGlobalBulk\": true, \"isSelectAll\": false, " +
                        "\"filterMap\": {\"currentTask\": \"" + fromStatus + "\",\"nextTaskForBulk\": \"" + toStatus + "\"}}";
            } else {
                logger.error("Bulk Action Create Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Payload for Bulk Action Save. {}", e.getMessage());
        }
        return payloadForSave;
    }

    private String getLastStatusBeforeArchive(String showResponse, String flowToTest) {
        try {
            return getStatus(showResponse, "Archived");
        } catch (Exception e) {
            logger.error("Exception while getting Last Status before Archive for Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return null;
    }

    private String getLastStatusBeforeOnHold(String showResponse, String flowToTest) {
        try {
            return getStatus(showResponse, "On Hold");
        } catch (Exception e) {
            logger.error("Exception while getting Last Status before On Hold for Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return null;
    }

    private String getStatus(String showResponse, String expectedStatusLabel) {
        JSONObject jsonObj = new JSONObject(showResponse);
        jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("history");
        JSONArray jsonArr = jsonObj.getJSONArray("status");

        if (jsonArr.length() > 0) {
            for (int i = jsonArr.length() - 1; i >= 0; i--) {
                String statusLabel = jsonArr.getJSONObject(i).getString("label");

                if (statusLabel.trim().equalsIgnoreCase(expectedStatusLabel)) {
                    return jsonArr.getJSONObject(i - 1).getString("label");
                }
            }
        }

        return null;
    }

    private void verifyHistory(String showResponse, String flowToTest, String entityName, String fromStatus, String expectedCurrentStatus, int recordId,
                               CustomAssert csAssert) {
        try {
            logger.info("Verifying Previous Status and Next Status i.e. History for Record Id {}, Entity {} and Flow [{}]", recordId, entityName, flowToTest);
            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("history");
            JSONArray jsonArr = jsonObj.getJSONArray("status");

            if (jsonArr.length() > 0) {
                String latestStatus = jsonArr.getJSONObject(jsonArr.length() - 1).getString("label");
                if (latestStatus == null || !latestStatus.trim().toLowerCase().contains(expectedCurrentStatus.trim().toLowerCase())) {
                    csAssert.assertTrue(false, "Latest Status doesn't match with Status " + expectedCurrentStatus + " for Record Id " + recordId +
                            ", Entity " + entityName + ", Flow [" + flowToTest + "].");
                }

                String previousStatus = jsonArr.getJSONObject(jsonArr.length() - 2).getString("label");
                if (previousStatus == null || !previousStatus.trim().toLowerCase().contains(fromStatus.trim().toLowerCase())) {
                    if (fromStatus.trim().equalsIgnoreCase("Upcoming")) {
                        if (previousStatus == null || !previousStatus.trim().toLowerCase().contains("overdue")) {
                            csAssert.assertTrue(false, "Previous Status doesn't match with Status " + fromStatus + " for Record Id " + recordId +
                                    ", Entity " + entityName + ", Flow [" + flowToTest + "].");
                        }
                    } else if (fromStatus.trim().equalsIgnoreCase("Overdue")) {
                        if (previousStatus == null || !previousStatus.trim().toLowerCase().contains("upcoming")) {
                            csAssert.assertTrue(false, "Previous Status doesn't match with Status " + fromStatus + " for Record Id " + recordId +
                                    ", Entity " + entityName + ", Flow [" + flowToTest + "].");
                        }
                    }
                }
            } else {
                csAssert.assertTrue(false, "Couldn't find Status JSONArray in History JSONObject in Show Response for Record Id " + recordId +
                        ", Entity " + entityName + ", Flow [" + flowToTest + "].");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while verifying History of Record having Id " + recordId + ", Entity " + entityName + ", Flow [" +
                    flowToTest + "]. " + e.getMessage());
        }
    }

    private void verifyEmailPart(String flowToTest, String entityName, List<Integer> entityIdsList, Map<Integer, String> expectedResultsMap,
                                 CustomAssert csAssert) {
        try {
            String bulkRequestId = bulkHelperObj.getLatestBulkEditRequestId();

            if (bulkRequestId == null) {
                csAssert.assertTrue(false, "Couldn't get Bulk Edit Request Id for Flow " + flowToTest + ". Hence cannot validate email part.");
                return;
            }

            //Verify Email Report Name
            /*String bulkActionAttachmentName = bulkHelperObj.getLatestBulkActionAttachmentName();
            String expectedAttachmentName = getExpectedAttachmentName();

            if (bulkActionAttachmentName == null) {
                csAssert.assertTrue(false, "Couldn't get Bulk Action Attachment Name from DB for Flow " + flowToTest);
            } else if (!bulkActionAttachmentName.equalsIgnoreCase(expectedAttachmentName)) {
                csAssert.assertTrue(false, "Expected Bulk Action Attachment Name: [" + expectedAttachmentName + "] and Actual Attachment Name: [" +
                        bulkActionAttachmentName + "] for Flow " + flowToTest);
            }*/

            //Verify Error Message
            for (Map.Entry<Integer, String> resultMap : expectedResultsMap.entrySet()) {
                String expectedResult = resultMap.getValue();

                if (expectedResult != null) {
                    Integer recordId = resultMap.getKey();
                    String expectedErrorMessage = expectedResult.equalsIgnoreCase("success") ? "null" : expectedResult;
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkEditRequestIdAndEntityId(bulkRequestId, recordId);

                    if (errorMessages == null) {
                        csAssert.assertTrue(false, "Couldn't get Error Messages for Bulk Edit Request Id " + bulkRequestId +
                                " from DB for Flow " + flowToTest);
                    } else if (!errorMessages.trim().toLowerCase().contains(expectedErrorMessage.toLowerCase())) {
                        csAssert.assertTrue(false, "Expected Error Message: [" + expectedErrorMessage + "] and Actual Error Message: [" + errorMessages +
                                "] for Flow " + flowToTest);
                    }
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Email Part for Flow " + flowToTest + " of Entity " + entityName +
                    " with Record Ids " + entityIdsList + e.getMessage());
        }
    }

    private String getExpectedAttachmentName() {
        String expectedAttachmentName;
        DateFormat dateFormat = new SimpleDateFormat("MMddyyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        String currentDate = dateFormat.format(date);

        expectedAttachmentName = "Bulk Action Response Report - " + currentDate + ".xls";

        return expectedAttachmentName;
    }

    private Map<Integer, String> getExpectedResultForAllRecords(List<Integer> recordIdsList, int entityTypeId, String toStatus, String flowToTest, CustomAssert csAssert) {
        Map<Integer, String> expectedResultsMap = new HashMap<>();

        try {
            Show showObj = new Show();

            for (Integer recordId : recordIdsList) {
                logger.info("Hitting Show API for Record Id {} of EntityTypeId {}", recordId, entityTypeId);

                showObj.hitShow(entityTypeId, recordId);
                String showResponse = showObj.getShowJsonStr();

                List<String> allActionLabels = ShowHelper.getAllActionLabelsFromShowResponse(showResponse);

                if (allActionLabels == null) {
                    csAssert.assertTrue(false, "Couldn't get All Action Labels for Record Id " + recordId + " of Entity Type Id " + entityTypeId +
                            " for Flow " + flowToTest);
                    expectedResultsMap.put(recordId, null);
                    continue;
                }

                if (allActionLabels.contains(toStatus)) {
                    expectedResultsMap.put(recordId, "success");
                } else {
                    expectedResultsMap.put(recordId, "Permission not granted to perform this action");
                }
            }

            return expectedResultsMap;
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Getting Expected Result for all Records for Flow " + flowToTest + ". " + e.getMessage());
        }

        return null;
    }
}