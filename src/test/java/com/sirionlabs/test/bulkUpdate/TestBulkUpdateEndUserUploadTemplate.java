package com.sirionlabs.test.bulkUpdate;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestBulkUpdateEndUserUploadTemplate {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkUpdateEndUserUploadTemplate.class);
    private String templatePath;
    private Long schedulerJobTimeOut;
    private Long schedulerJobPollingTime;
    private Map<String, String> defaultProperties;

    private List<String> entitiesToTest;

    private BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

    @BeforeClass
    public void beforeClass() {
        String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkUpdateEndUserUploadTemplateConfigFilePath");
        String configFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkUpdateEndUserUploadTemplateConfigFileName");

        defaultProperties = ParseConfigFile.getAllDefaultProperties(configFilePath, configFileName);

        templatePath = defaultProperties.get("bulkupdatetemplatepath");
        schedulerJobTimeOut = Long.parseLong(defaultProperties.get("schedulerjobtimeout"));
        schedulerJobPollingTime = Long.parseLong(defaultProperties.get("schedulerjobpollingtime"));

        entitiesToTest = getEntitiesToTest();
    }

    public List<String> getEntitiesToTest() {
        String[] entitiesArr = {"contracts", "service levels", "child service levels", "disputes", "child obligations", "obligations", "consumptions"};
        return Arrays.asList(entitiesArr);
    }

    private void copyTemplateFile(String baseTemplateName, String testTemplateName) {
        logger.info("Checking if Base Template File exists at Location: [" + templatePath + "/" + baseTemplateName + "]");
        if (!FileUtils.fileExists(templatePath, baseTemplateName)) {
            throw new SkipException("Couldn't find Base Template at Location: [" + templatePath + "/" + baseTemplateName + "]");
        }

        logger.info("Creating a copy of base template file for Test.");
        if (!FileUtils.copyFile(templatePath, baseTemplateName, templatePath, testTemplateName)) {
            throw new SkipException("Couldn't create a copy of Base Template File.");
        }
    }


    /*
    TC-C3558: Validate allowed file formats.
     */
    @Test
    public void testC3558() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3558.");

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Allowed file format for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "InvalidFormat.pdf";

                copyTemplateFile(baseTemplateName, testTemplateName);

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (!uploadResponse.toLowerCase().contains("file extension pdf not supported")) {
                    csAssert.assertTrue(false, "Expected Response: [File extension pdf not supported] and Actual Response: [" + uploadResponse +
                            "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3558 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C3562: Verify Header Validation Message.
     */
    @Test
    public void testC3562() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3562.");

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Header Validation Message for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C3562.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);
                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 1, 1, "");

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Header Value in Bulk Template for Entity " + entityName);
                }

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (!uploadResponse.toLowerCase().contains("incorrect headers")) {
                    csAssert.assertTrue(false, "Expected Response: [Incorrect Headers] and Actual Response: [" + uploadResponse +
                            "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3562 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C3566: Validate Bulk Update having process column value as No.
     */
    @Test
    public void testC3566() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3566.");

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Bulk Update having Process column value as No for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C3566.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);

                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

                if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                    throw new SkipException("Couldn't get All Header Ids from Bulk Template for Entity " + entityName);
                }

                int processColumnNo = allHeaderIds.indexOf("100000002");

                if (processColumnNo == -1) {
                    throw new SkipException("Couldn't get Column No of Process Field for Entity " + entityName);
                }

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 6, processColumnNo, "No");

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Process Value as No in Bulk Template for Entity " + entityName);
                }

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Update Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Entity " + entityName);
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Update Scheduler Job Passed whereas it was supposed to fail for Entity " + entityName);
                    } else {
                        //Validate that No Record Processed.
                        String errorMessage = schedulerJob.get("errorMessage");

                        if (!errorMessage.trim().toLowerCase().contains("no record processed")) {
                            csAssert.assertTrue(false, "Expected Result: [No Record Processed] and Actual Result: " +
                                    "[Record was processed successfully] for Entity " + entityName);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3566 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C3567: Validate impact of deleting any value of SL No and Id Column.
    TC-C3525: Validate error comes on editing SL No or Id Column.
     */
    @Test
    public void testC3567() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3567.");

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Impact of Deleting any value of SL No and Id Column for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C3567.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);

                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

                if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                    throw new SkipException("Couldn't get All Header Ids from Bulk Template for Entity " + entityName);
                }

                String idColumnHeaderId = BulkTemplate.getBulkUpdateIdColumnHeaderIdForEntity(entityName);

                int idColumnNo = allHeaderIds.indexOf(idColumnHeaderId);

                if (idColumnNo == -1) {
                    throw new SkipException("Couldn't get Column No of Id Field for Entity " + entityName);
                }

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 6, idColumnNo, "");

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Id Value in Bulk Template for Entity " + entityName);
                }

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Update Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Entity " + entityName);
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Update Scheduler Job Passed whereas it was supposed to fail for Entity " + entityName);
                    } else {
                        String bulkEditRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                        String errorMessages = bulkHelperObj.getErrorDataForBulkEditRequestId(bulkEditRequestId);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB for Entity " + entityName);
                        }

                        if (!errorMessages.toLowerCase().contains("invalid entity id") || !errorMessages.contains(idColumnHeaderId)) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. " +
                                    "Expected Error Message: [Invalid Entity ID] and Field Id: [" + idColumnHeaderId + "] but Actual Error Message: [" +
                                    errorMessages + "] for Entity " + entityName);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3567 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C3569: Validate Incorrect Data Type Error for entering text in numeric field.
     */
    @Test
    public void testC3569() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3569.");

        for (String entityName : entitiesToTest) {
            try {
                if (entityName.equalsIgnoreCase("child obligations") || entityName.equalsIgnoreCase("obligations") ||
                        entityName.equalsIgnoreCase("contracts")) {
                    continue;
                }

                logger.info("Validating Incorrect Data Type Error for entering text in Numeric Field for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C3569.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);

                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

                if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                    throw new SkipException("Couldn't get All Header Ids from Bulk Template for Entity " + entityName);
                }

                String columnHeaderId = "";
                String fieldName = "";

                switch (entityName) {
                    case "child service levels":
                        fieldName = "Final Performance";
                        columnHeaderId = "1103";
                        break;

                    case "disputes":
                        fieldName = "Financial Impact";
                        columnHeaderId = "11190";
                        break;

                    case "consumptions":
                        fieldName = "Final Consumption";
                        columnHeaderId = "11910";
                        break;

                    case "service levels":
                        fieldName = "Targets - Expected";
                        columnHeaderId = "219";
                        break;
                }

                int idColumnNo = allHeaderIds.indexOf(columnHeaderId);

                if (idColumnNo == -1) {
                    throw new SkipException("Couldn't get Column No of Field " + fieldName + " for Entity " + entityName);
                }

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 6, idColumnNo, "Test Char");

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Value of Field " + fieldName + " in Bulk Template for Entity " + entityName);
                }

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Update Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Entity " + entityName);
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Update Scheduler Job Passed whereas it was supposed to fail for Entity " + entityName);
                    } else {
                        String bulkEditRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                        String errorMessages = bulkHelperObj.getErrorDataForBulkEditRequestId(bulkEditRequestId);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB for Entity " + entityName);
                        }

                        if (!errorMessages.toLowerCase().contains("please enter a") || !errorMessages.contains(columnHeaderId)) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. " +
                                    "Expected Error Message: [Please enter a value] and Field Id: [" + columnHeaderId + "] but Actual Error Message: [" +
                                    errorMessages + "] for Entity " + entityName);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3569 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C3592: Numeric field Validation for Decimal Part.
     */
    @Test
    public void testC3592() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3592.");

        FieldRenaming fieldRenamingObj = new FieldRenaming();
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1, 467);

        for (String entityName : entitiesToTest) {
            try {
                if (entityName.equalsIgnoreCase("child obligations") || entityName.equalsIgnoreCase("obligations") ||
                        entityName.equalsIgnoreCase("contracts")) {
                    continue;
                }

                logger.info("Validating Numeric Field Decimal Part Length for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C3592.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);

                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

                if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                    throw new SkipException("Couldn't get All Header Ids from Bulk Template for Entity " + entityName);
                }

                String columnHeaderId = "";
                String fieldName = "";
                String newValue = "";
                String childGroupName = null;
                String fieldLabel = "";

                switch (entityName) {
                    case "child service levels":
                        fieldName = "Final Performance";
                        columnHeaderId = "1103";
                        newValue = "90.11111111111";
                        childGroupName = "Common";
                        fieldLabel = "Please enter a value not having more than 11 digits in integral part and 10 digits in fractional part.";
                        break;

                    case "disputes":
                        fieldName = "Financial Impact";
                        columnHeaderId = "11190";
                        newValue = "100.111111";
                        fieldLabel = "Please enter a value not having more than 18 digits in integral part and 5 digits in fractional part.";
                        break;

                    case "consumptions":
                        fieldName = "Final Consumption";
                        columnHeaderId = "11910";
                        newValue = "100.1111111111111";
                        fieldLabel = "Please enter a non negative value not having more than 14 digits in integral part and 12 digits in fractional part.";
                        break;

                    case "service levels":
                        fieldName = "Targets - Expected";
                        columnHeaderId = "219";
                        newValue = "80.11111";
                        childGroupName = "Common";
                        fieldLabel = "Please enter a value not having more than 18 digits in integral part and 4 digits in fractional part.";
                        break;
                }

                String expectedErrorMessage = fieldRenamingObj.getClientFieldNameFromName(fieldRenamingResponse, childGroupName, fieldLabel);

                if (expectedErrorMessage == null) {
                    throw new SkipException("Couldn't Get Expected Error Message for Entity " + entityName);
                }

                int idColumnNo = allHeaderIds.indexOf(columnHeaderId);

                if (idColumnNo == -1) {
                    throw new SkipException("Couldn't get Column No of Field " + fieldName + " for Entity " + entityName);
                }

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 6, idColumnNo, newValue);

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Value of Field " + fieldName + " in Bulk Template for Entity " + entityName);
                }

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Update Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Entity " + entityName);
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Update Scheduler Job Passed whereas it was supposed to fail for Entity " + entityName);
                    } else {
                        String bulkEditRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                        String errorMessages = bulkHelperObj.getErrorDataForBulkEditRequestId(bulkEditRequestId);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB for Entity " + entityName);
                        }

                        if (!errorMessages.toLowerCase().contains(expectedErrorMessage.toLowerCase())
                                || !errorMessages.contains(columnHeaderId)) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. " +
                                    "Expected Error Message: [" + expectedErrorMessage + "] " + "and Field Id: [" + columnHeaderId +
                                    "] but Actual Error Message: [" + errorMessages + "] for Entity " + entityName);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3592 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C3595: Validate Bulk Update on Entity which doesn't exist.
    TC-C4248: Verify entity should be identified by Entity Id.
     */
    @Test
    public void testC3595() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3595.");

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Bulk Update on Entity which doesn't exist for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C3595.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);

                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

                if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                    throw new SkipException("Couldn't get All Header Ids from Bulk Template for Entity " + entityName);
                }

                String idColumnHeaderId = BulkTemplate.getBulkUpdateIdColumnHeaderIdForEntity(entityName);

                int idColumnNo = allHeaderIds.indexOf(idColumnHeaderId);

                if (idColumnNo == -1) {
                    throw new SkipException("Couldn't get Column No of Id Field for Entity " + entityName);
                }

                String shortCodeId = ConfigureConstantFields.getShortCodeForEntity(entityName);

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 6, idColumnNo, shortCodeId.concat("990088779"));

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Value of Field Id in Bulk Template for Entity " + entityName);
                }

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Update Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Entity " + entityName);
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Update Scheduler Job Passed whereas it was supposed to fail for Entity " + entityName);
                    } else {
                        String bulkEditRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                        String errorMessages = bulkHelperObj.getErrorDataForBulkEditRequestId(bulkEditRequestId);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB for Entity " + entityName);
                        }

                        if (!errorMessages.toLowerCase().contains("invalid entity id") || !errorMessages.contains(idColumnHeaderId)) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. " +
                                    "Expected Error Message: [Invalid Entity ID] and Field Id: [" + idColumnHeaderId + "] but Actual Error Message: [" +
                                    errorMessages + "] for Entity " + entityName);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3595 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C3571: Validate Bulk Update without changing value of fields.
    TC-C3565: Validate Bulk Update file submission without any pre processing error.
     */
    @Test
    public void testC3571() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3571.");

        for (String entityName : entitiesToTest) {
            try {
                if (entityName.equalsIgnoreCase("child obligations")) {
                    continue;
                }

                logger.info("Validating Bulk Update without changing value of fields for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C3571.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);

                Integer recordId = BulkTemplate.getRecordIdFromBulkUpdateTemplateForEntity(templatePath, testTemplateName, entityName, 7);

                if (recordId == null) {
                    csAssert.assertTrue(false, "Couldn't get Record Id from Short Code Id for Entity " + entityName);
                    continue;
                }

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Update Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Entity " + entityName);
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("false")) {
                        String bulkEditRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                        String errorMessages = bulkHelperObj.getErrorDataForBulkEditRequestId(bulkEditRequestId);

                        csAssert.assertTrue(false, "Bulk Update Scheduler Job Failed whereas it was supposed to pass for Entity " + entityName +
                                ". Error Message: [" + errorMessages + "]");
                    } else {
                        //Validate Audit Log.
                        String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
                                "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                        String auditLogResponse = TabListDataHelper.hitTabListDataAPIForAuditLogTab(entityTypeId, recordId, payload);

                        String historyColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogResponse, "history");
                        String historyValue = new JSONObject(auditLogResponse).getJSONArray("data").getJSONObject(0)
                                .getJSONObject(historyColumnId).getString("value");
                        Long fieldHistoryId = TabListDataHelper.getHistoryIdFromValue(historyValue);

                        String fieldHistoryResponse = new FieldHistory().hitFieldHistory(fieldHistoryId, entityTypeId);
                        csAssert.assertTrue(new JSONObject(fieldHistoryResponse).getJSONArray("value").length() == 0,
                                "Value modified in Audit Log for Entity " + entityName);
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3571 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C4250: Validate Read Only Field Update Impact.
    TC-C7676
     */
    @Test
    public void testC4250() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C4250.");

        for (String entityName : entitiesToTest) {
            try {
                if (entityName.equalsIgnoreCase("child obligations")) {
                    continue;
                }

                logger.info("Validating Read Only Field Update Impact for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C4250.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);

                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

                if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                    throw new SkipException("Couldn't get All Header Ids from Bulk Template for Entity " + entityName);
                }

                String headerId = "";
                String newValue = "";

                switch (entityName) {
                    case "contracts":
                        headerId = "37";
                        newValue = "TestReadOnlyFieldImpact";
                        break;

                    case "obligations":
                        headerId = "303";
                        newValue = "TestReadOnlyFieldImpact";
                        break;

                    case "child obligations":
                        headerId = "1004";
                        newValue = "TestReadOnlyFieldImpact";
                        break;

                    case "service levels":
                        headerId = "203";
                        newValue = "TestReadOnlyFieldImpact";
                        break;

                    case "child service levels":
                        headerId = "1127";
                        newValue = "TestReadOnlyFieldImpact";
                        break;

                    case "disputes":
                        headerId = "11182";
                        newValue = "TestReadOnlyFieldImpact";
                        break;

                    case "consumptions":
                        headerId = "11879";
                        newValue = "TestReadOnlyFieldImpact";
                        break;
                }

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 6, allHeaderIds.indexOf(headerId),
                        newValue);

                if (!updateValue) {
                    throw new SkipException("Couldn't update Value of Header Id " + headerId + " for Entity " + entityName);
                }

                String showPageObjectName = bulkHelperObj.getEntityBulkUpdateFieldShowPageObjectNameFromHeaderId(entityName, headerId);

                if (showPageObjectName == null) {
                    throw new SkipException("Couldn't get Show Page Object Name for Field Header Id " + headerId + " of Entity " + entityName);
                }

                Integer recordId = BulkTemplate.getRecordIdFromBulkUpdateTemplateForEntity(templatePath, testTemplateName, entityName, 7);

                if (recordId == null) {
                    csAssert.assertTrue(false, "Couldn't get Record Id from Short Code Id for Entity " + entityName);
                    continue;
                }

                String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);
                String expectedValueOnShowPage = ShowHelper.getValueOfField(showPageObjectName, showResponse);

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Update Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Entity " + entityName);
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("false")) {
                        String bulkEditRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                        String errorMessages = bulkHelperObj.getErrorDataForBulkEditRequestId(bulkEditRequestId);

                        csAssert.assertTrue(false, "Bulk Update Scheduler Job Failed whereas it was supposed to pass for Entity " + entityName +
                                ". Error Message: [" + errorMessages + "]");
                    } else {
                        //Validate Value on Show Page.
                        ShowHelper.verifyShowField(showResponse, showPageObjectName, expectedValueOnShowPage, entityTypeId, recordId, csAssert);

                        //Validate Audit Log.
                        String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
                                "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                        String auditLogResponse = TabListDataHelper.hitTabListDataAPIForAuditLogTab(entityTypeId, recordId, payload);

                        String historyColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogResponse, "history");
                        String historyValue = new JSONObject(auditLogResponse).getJSONArray("data").getJSONObject(0)
                                .getJSONObject(historyColumnId).getString("value");
                        Long fieldHistoryId = TabListDataHelper.getHistoryIdFromValue(historyValue);

                        String fieldHistoryResponse = new FieldHistory().hitFieldHistory(fieldHistoryId, entityTypeId);
                        csAssert.assertTrue(new JSONObject(fieldHistoryResponse).getJSONArray("value").length() == 0,
                                "Value modified in Audit Log for Entity " + entityName);
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C4250 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    /*
    TC-C3564: Validate the maximum no of entity that can be updated from bulk update template.
     */
    @Test
    public void testC3564() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3564.");

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Maximum No of Entity that can be updated from Bulk Update Template for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C3564.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);
                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                long maxBulkUpdateLimit = BulkTemplate.getBulkUpdateMaxRecordsLimitForEntity(entityName);

                boolean updateValue = XLSUtils.copyRowDataMultipleTimesWithIncrementalSlNo(templatePath, testTemplateName, dataSheetName, 6, maxBulkUpdateLimit);

                if (!updateValue) {
                    throw new SkipException("Couldn't Create " + maxBulkUpdateLimit + " Records in Bulk Template for Entity " + entityName);
                }

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (!uploadResponse.toLowerCase().contains("exceeded maximum number of rows allowed(" + maxBulkUpdateLimit + ") to upload")) {
                    csAssert.assertTrue(false, "Expected Response: [exceeded maximum number of rows allowed(" + maxBulkUpdateLimit +
                            ") to upload] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3564 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }

    /*
     TC-C3857: Upload the template with negative value in Final Consumption.
    */
    @Test
    public void testC3857() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3857.");

        FieldRenaming fieldRenamingObj = new FieldRenaming();
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1, 467);

        try {
            String entityName = "consumptions";
            logger.info("Validating Numeric Field Decimal Part Length for Entity {}", entityName);

            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

            String baseTemplateName = getBaseTemplateNameForEntity(entityName);
            String testTemplateName = "C3857.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Bulk Template for Entity " + entityName);
            }

            String columnHeaderId = "";
            String fieldName = "";
            String newValue = "";
            String childGroupName = null;
            String fieldLabel = "";

            switch (entityName) {
                case "consumptions":
                    fieldName = "Final Consumption";
                    columnHeaderId = "11910";
                    newValue = "-1111111111111";
                    fieldLabel = "Please enter a non negative value not having more than 14 digits in integral part and 12 digits in fractional part.";
                    break;

            }
            String expectedErrorMessage = fieldRenamingObj.getClientFieldNameFromName(fieldRenamingResponse, childGroupName, fieldLabel);
            if (expectedErrorMessage == null) {
                throw new SkipException("Couldn't Get Expected Error Message for Entity " + entityName);
            }

            int idColumnNo = allHeaderIds.indexOf(columnHeaderId);

            if (idColumnNo == -1) {
                throw new SkipException("Couldn't get Column No of Field " + fieldName + " for Entity " + entityName);
            }

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 6, idColumnNo, newValue);

            if (!updateValue) {
                throw new SkipException("Couldn't Update Value of Field " + fieldName + " in Bulk Template for Entity " + entityName);
            }

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Update Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Update Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Entity " + entityName);
                }
                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Update Scheduler Job Passed whereas it was supposed to fail for Entity " + entityName);
                } else {
                    String bulkEditRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                    String errorMessages = bulkHelperObj.getErrorDataForBulkEditRequestId(bulkEditRequestId);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB for Entity " + entityName);
                    }

                    if (!errorMessages.toLowerCase().contains(expectedErrorMessage.toLowerCase())
                            || !errorMessages.contains(columnHeaderId)) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. " +
                                "Expected Error Message: [" + expectedErrorMessage + "] " + "and Field Id: [" + columnHeaderId +
                                "] but Actual Error Message: [" + errorMessages + "] for Entity " + entityName);
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Entity " + entityName);
            }
            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C3857 for Entity  " + e.getMessage());
        }
        csAssert.assertAll();

    }

    /*
    TC-C4253: Verify the error on uploading the template after deleting any read only field.
     */
    @Test
    public void testC4253() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C4253.");

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Error Message on Uploading Bulk Update Template after deleting any read only field for Entity " + entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String baseTemplateName = getBaseTemplateNameForEntity(entityName);
                String testTemplateName = "C4253.xlsm";

                copyTemplateFile(baseTemplateName, testTemplateName);

                String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

                if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                    throw new SkipException("Couldn't get All Header Ids from Bulk Template for Entity " + entityName);
                }

                String headerId = "";

                switch (entityName) {
                    case "contracts":
                        headerId = "37";
                        break;

                    case "obligations":
                        headerId = "303";
                        break;

                    case "child obligations":
                        headerId = "1004";
                        break;

                    case "service levels":
                        headerId = "203";
                        break;

                    case "child service levels":
                        headerId = "1127";
                        break;

                    case "disputes":
                        headerId = "11182";
                        break;

                    case "consumptions":
                        headerId = "11879";
                        break;
                }

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 1, allHeaderIds.indexOf(headerId),
                        "");

                if (!updateValue) {
                    throw new SkipException("Couldn't update Value of Header Id " + headerId + " for Entity " + entityName);
                }

                String uploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templatePath, testTemplateName, entityTypeId, templateId);

                if (!uploadResponse.contains("Incorrect headers")) {
                    csAssert.assertTrue(false, "Expected Upload Response: [Incorrect Headers] and Actual Upload Response: [" + uploadResponse +
                            "] for Entity " + entityName);
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C4253. " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }

    private String getBaseTemplateNameForEntity(String entityName) {
        String baseTemplateName = null;

        switch (entityName) {
            case "contracts":
                baseTemplateName = defaultProperties.get("contractbulkupdatetemplate");
                break;

            case "child obligations":
                baseTemplateName = defaultProperties.get("cobbulkupdatetemplate");
                break;

            case "child service levels":
                baseTemplateName = defaultProperties.get("cslbulkupdatetemplate");
                break;

            case "disputes":
                baseTemplateName = defaultProperties.get("disputebulkupdatetemplate");
                break;

            case "obligations":
                baseTemplateName = defaultProperties.get("obligationbulkupdatetemplate");
                break;

            case "consumptions":
                baseTemplateName = defaultProperties.get("consumptionbulkupdatetemplate");
                break;

            case "service levels":
                baseTemplateName = defaultProperties.get("slbulkupdatetemplate");
                break;
        }

        return baseTemplateName;
    }
}