package com.sirionlabs.test.bulkUpdate;

import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestBulkUpdateFeature {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkUpdateFeature.class);

    private String configFilePath;
    private String flowsConfigFileName;
    private String templatePath;
    private Long schedulerJobTimeOut;
    private Long schedulerJobPollingTime;
    private Map<String, String> defaultProperties;

    private BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkUpdateFeatureConfigFilePath");
        String configFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkUpdateFeatureConfigFileName");
        defaultProperties = ParseConfigFile.getAllDefaultProperties(configFilePath, configFileName);

        flowsConfigFileName = defaultProperties.get("flowsconfigfilename");

        templatePath = defaultProperties.get("excelfilepath");
        schedulerJobTimeOut = Long.parseLong(defaultProperties.get("schedulerwaittimeout"));
        schedulerJobPollingTime = Long.parseLong(defaultProperties.get("schedulerpollingtime"));
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

    @DataProvider
    public Object[][] dataProviderForBulkUpdateFeature() {
        List<Object[]> allTestData = new ArrayList<>();

        logger.info("Setting all Flows to Test.");

        List<String> allFlowsToTest = getFlowsToTest();
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest.trim()});
        }

        logger.info("Total Flows to Test : {}", allTestData.size());
        return allTestData.toArray(new Object[0][]);
    }

    private List<String> getFlowsToTest() {
        List<String> flowsToTest = new ArrayList<>();

        try {
            String temp = defaultProperties.get("testallflows");

            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
                flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, flowsConfigFileName);
            } else {
                String[] allFlows = defaultProperties.get("flowstovalidate").split(Pattern.quote(","));

                for (String flow : allFlows) {
                    if (ParseConfigFile.containsSection(configFilePath, flowsConfigFileName, flow.trim())) {
                        flowsToTest.add(flow.trim());
                    } else {
                        logger.info("Flow having name [{}] not found in Bulk Update Flows Config File.", flow.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting Flows to Test for Bulk Update Validation. {}", e.getMessage());
        }
        return flowsToTest;
    }


    @Test(dataProvider = "dataProviderForBulkUpdateFeature")
    public void testBulkUpdateFeature(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Bulk Update Feature Flow {}", flowToTest);
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, flowsConfigFileName, flowToTest);

            String entityName = flowProperties.get("entity");
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

            String baseTemplateName = flowProperties.get("excelfilename");
            String testTemplateName = flowToTest + ".xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<List<String>> allFieldNamesList = new ArrayList<>();
            List<List<String>> allFieldValuesList = new ArrayList<>();

            String fieldsToUpdateStr = flowProperties.get("fieldstoupdate");

            int noOfRecords = Integer.parseInt(flowProperties.get("noofrecords"));
            List<Integer> allRecordIds = new ArrayList<>();
            List<String> allShowResponseBeforeUpdate = new ArrayList<>();

            String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);
            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, dataSheetName, 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Template [" + testTemplateName + "] for Flow [" + flowToTest + "].");
            }

            for (int i = 0; i < noOfRecords; i++) {
                int recordNo = i + 1;

                List<String> allFieldHeaderIds = new ArrayList<>();
                List<String> allFieldNames = new ArrayList<>();
                List<String> allFieldValues = new ArrayList<>();

                String[] setOfRecords = fieldsToUpdateStr.split(Pattern.quote("|||"));

                int recordSetNo = setOfRecords.length < (i + 1) ? setOfRecords.length - 1 : i;
                String recordFieldsSet = setOfRecords[recordSetNo];

                boolean fieldValuesSet = setFieldValues(flowToTest, entityName, recordFieldsSet, allFieldHeaderIds, allFieldNames, allFieldValues);

                if (!fieldValuesSet) {
                    throw new SkipException("Couldn't Set Field Values for Record #" + recordNo + " of Flow [" + flowToTest + "]");
                }

                if (flowProperties.get("updatefieldsintemplate").equalsIgnoreCase("true")) {
                    boolean fieldsUpdated = updateFieldsInTemplate(flowToTest, testTemplateName, dataSheetName, recordNo, allHeaderIds, allFieldHeaderIds, allFieldValues);

                    if (!fieldsUpdated) {
                        throw new SkipException("Couldn't Update Fields in Template for Record #" + recordNo + " of Flow [" + flowToTest + "]");
                    }
                }

                Integer recordId = BulkTemplate.getRecordIdFromBulkUpdateTemplateForEntity(templatePath, testTemplateName, entityName, 6 + recordNo);

                if (recordId == null) {
                    throw new SkipException("Couldn't get Record Id from Short Code Id of Record #" + recordNo + " for Flow [" + flowToTest + "]");
                }

                allRecordIds.add(recordId);
                allShowResponseBeforeUpdate.add(ShowHelper.getShowResponse(entityTypeId, recordId));

                allFieldNamesList.add(allFieldNames);
                allFieldValuesList.add(allFieldValues);
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
                    throw new SkipException("Bulk Update Scheduler Job didn't finish and hence cannot validate further for Flow [" + flowToTest + "]");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("false")) {
                    String bulkEditRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                    String errorMessages = bulkHelperObj.getErrorDataForBulkEditRequestId(bulkEditRequestId);

                    csAssert.assertTrue(false, "Bulk Update Scheduler Job Failed whereas it was supposed to pass for Flow [" + flowToTest +
                            "]. Error Message: [" + errorMessages + "]");
                } else {
                    //Validate Values on Show Page.
                    validateRecordsOnShowPage(flowToTest, entityName, entityTypeId, allRecordIds, allFieldNamesList, allFieldValuesList, csAssert);

                    //Validate Audit Log.
                    bulkHelperObj.verifyBulkUpdateAuditLog(entityName, entityTypeId, allRecordIds, allShowResponseBeforeUpdate, allFieldNamesList, allFieldValuesList,
                            csAssert);

                    //Restore Record to Original State.
                    restoreRecordsAfterUpdate(entityName, allShowResponseBeforeUpdate, allRecordIds);
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for Flow [" + flowToTest + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Bulk Feature for Flow [" + flowToTest + "]. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private boolean setFieldValues(String flowToTest, String entityName, String fieldsToUpdateStr, List<String> allFieldHeaderIds, List<String> allFieldNames,
                                   List<String> allFieldValues) {
        try {
            String[] setOfFields = fieldsToUpdateStr.split(Pattern.quote(":::"));

            for (String oneField : setOfFields) {
                String[] temp = oneField.split(Pattern.quote("->"));

                allFieldHeaderIds.add(temp[0].trim());

                if (temp.length > 1) {
                    allFieldValues.add(temp[1].trim());
                } else {
                    allFieldValues.add("");
                }

                String fieldName = bulkHelperObj.getEntityBulkUpdateFieldShowPageObjectNameFromHeaderId(entityName, temp[0]).trim();
                allFieldNames.add(fieldName);
            }

            return true;
        } catch (Exception e) {
            logger.error("Couldn't Set Field Values for Flow [{}]", flowToTest);
        }

        return false;
    }

    private boolean updateFieldsInTemplate(String flowToTest, String testTemplateName, String dataSheetName, int recordNo, List<String> allHeaderIds,
                                           List<String> allFieldHeaderIds, List<String> allFieldValues) {
        try {
            for (int i = 0; i < allFieldHeaderIds.size(); i++) {
                String fieldHeaderId = allFieldHeaderIds.get(i);

                if (!allHeaderIds.contains(fieldHeaderId)) {
                    logger.error("Field Header Id {} is not present in Template [{}] for Flow [{}].", fieldHeaderId, testTemplateName, flowToTest);
                    return false;
                }

                int index = allHeaderIds.indexOf(fieldHeaderId);

                logger.info("Updating Value of Field having Header Id {} in Template [{}] for Flow [{}]", fieldHeaderId, testTemplateName, flowToTest);

                if (!XLSUtils.updateColumnValue(templatePath, testTemplateName, dataSheetName, 5 + recordNo, index, allFieldValues.get(i))) {
                    logger.error("Couldn't Update Field having Header Id {} in Template [{}] for Flow [{}].", fieldHeaderId, testTemplateName, flowToTest);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Exception while Updating Fields in Template [{}] for Flow [{}]. {}", testTemplateName, flowToTest, e.getMessage());
        }

        return false;
    }

    private void validateRecordsOnShowPage(String flowToTest, String entityName, int entityTypeId, List<Integer> recordIdsList, List<List<String>> allFieldNamesList,
                                           List<List<String>> allFieldValuesList, CustomAssert csAssert) {
        logger.info("Validating Records on Show Page for Flow [{}]", flowToTest);

        for (int i = 0; i < recordIdsList.size(); i++) {
            int recordId = recordIdsList.get(i);

            try {
                List<String> allFieldNames = allFieldNamesList.get(i);
                List<String> allFieldValues = allFieldValuesList.get(i);

                logger.info("Hitting Show Api for Record having Id {} for Flow [{}] and Entity {}", recordId, flowToTest, entityName);

                String showJsonStr = ShowHelper.getShowResponse(entityTypeId, recordId);

                if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
                    for (int j = 0; j < allFieldNames.size(); j++) {
                        String fieldName = allFieldNames.get(j);
                        String expectedValue = allFieldValues.get(j);

                        boolean positiveTest = !expectedValue.equalsIgnoreCase("");

                        Map<String, String> fieldAttributesMap = ParseJsonResponse.getFieldByName(showJsonStr, fieldName);

                        boolean fieldOfMultiSelectType = false;

                        if (fieldAttributesMap != null && !fieldAttributesMap.isEmpty()) {
                            if (fieldAttributesMap.get("multiple").equalsIgnoreCase("true")) {
                                fieldOfMultiSelectType = true;
                            }
                        }

                        if (!fieldOfMultiSelectType) {
                            ShowHelper.verifyShowField(showJsonStr, fieldName, expectedValue, entityTypeId, recordId, csAssert, positiveTest);
                        } else {
                            String[] expectedValuesArr = expectedValue.split(Pattern.quote(";"));

                            for (String expectedVal : expectedValuesArr) {
                                ShowHelper.verifyShowField(showJsonStr, fieldName, "select", expectedVal, entityTypeId, recordId, csAssert, positiveTest);
                            }
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " for Flow [" + flowToTest +
                            "] and Entity " + entityName + " is not a valid JSON.");
                }
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Verifying Bulk Update Record Id " + recordId + " on Show Page for Flow [" + flowToTest +
                        "] and Entity " + entityName + ". " + e.getMessage());
            }
        }
    }

    private void restoreRecordsAfterUpdate(String entityName, List<String> allShowResponseBeforeUpdate, List<Integer> recordIdsList) {
        for (int i = 0; i < recordIdsList.size(); i++) {
            EntityOperationsHelper.restoreRecord(entityName, recordIdsList.get(i), allShowResponseBeforeUpdate.get(i));
        }
    }
}