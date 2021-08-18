package com.sirionlabs.test.bulkCreate;

import com.sirionlabs.api.bulkIntegration.BulkUpdateUser;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.poi.ss.usermodel.CellType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestBulkCreate {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkCreate.class);
    private String configFilePath = null;
    private Map<String, String> defaultProperties;
    private String flowsConfigFileName = null;
    private String bulkCreateExcelFilePath;
    private Long schedulerJobTimeOut;
    private Long schedulerJobPollingTime;
    private Integer auditLogTabId;

    private BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateConfigFilePath");
        String configFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateConfigFileName");

        defaultProperties = ParseConfigFile.getAllDefaultProperties(configFilePath, configFileName);

        flowsConfigFileName = defaultProperties.get("flowsconfigfilename");
        bulkCreateExcelFilePath = defaultProperties.get("excelfilepath");
        schedulerJobTimeOut = Long.parseLong(defaultProperties.get("schedulerwaittimeout"));
        schedulerJobPollingTime = Long.parseLong(defaultProperties.get("schedulerpollingtime"));

        auditLogTabId = TabListDataHelper.getIdForTab("audit log");
    }

    private void copyTemplateFile(String baseTemplateName, String testTemplateName) {
        logger.info("Checking if Base Template File exists at Location: [" + bulkCreateExcelFilePath + "/" + baseTemplateName + "]");
        if (!FileUtils.fileExists(bulkCreateExcelFilePath, baseTemplateName)) {
            throw new SkipException("Couldn't find Base Template at Location: [" + bulkCreateExcelFilePath + "/" + baseTemplateName + "]");
        }

        logger.info("Creating a copy of base template file for Test.");
        if (!FileUtils.copyFile(bulkCreateExcelFilePath, baseTemplateName, bulkCreateExcelFilePath, testTemplateName)) {
            throw new SkipException("Couldn't create a copy of Base Template File.");
        }
    }

    @DataProvider
    public Object[][] dataProviderForBulkCreateFeature() {
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
                        logger.info("Flow having name [{}] not found in Bulk Create Flows Config File.", flow.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting Flows to Test for Bulk Create Validation. {}", e.getMessage());
        }
        return flowsToTest;
    }

    @Test(dataProvider = "dataProviderForBulkCreateFeature")
    public void testBulkCreateFeature(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();

        try {
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, flowsConfigFileName, flowToTest);

            if (flowProperties.isEmpty()) {
                throw new SkipException("Couldn't get All Properties of Flow " + flowToTest);
            }

            String testDescription = flowProperties.get("description");
            logger.info("Starting Test [{}]", testDescription);
            logger.info("Verifying Bulk Create for Flow: [{}]", flowToTest);

            String baseTemplateName = flowProperties.get("basetemplatename");
            String testTemplateName = flowToTest + ".xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

//            if(flowToTest.equalsIgnoreCase("purchase order positive flow 1") ||
//                    flowToTest.equalsIgnoreCase("service data positive flow 1")){

            if(flowProperties.get("unique fields") != null){
                String[] uniqueFields = flowProperties.get("unique fields").split(",");
                String sheetName = flowProperties.get("sheet name");


                updateUniqueFields(bulkCreateExcelFilePath,testTemplateName,sheetName,uniqueFields);

            }

            String entityName = flowProperties.get("entity");
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            //Upload Bulk Create Template
            int parentEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest,
                    "parentEntityTypeId"));
            int parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "parentId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "templateId"));
            String bulkCreateUploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateExcelFilePath, testTemplateName, parentEntityTypeId, parentId,
                    entityTypeId, templateId);

            if (bulkCreateUploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    int newlyCreatedRecordId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);

                    if (newlyCreatedRecordId == -1) {
                        throw new SkipException("Couldn't get Newly Created Record Id from Bulk Template.");
                    }

                    boolean invoiceWithLineItemFlag = false;
                    if (entityName.equalsIgnoreCase("invoices")) {
                        if (flowProperties.containsKey("withlineitem") && flowProperties.get("withlineitem").trim().equalsIgnoreCase("true")) {
                            invoiceWithLineItemFlag = true;
                        }

                        if (invoiceWithLineItemFlag) {
                            //Validate Line Item
                            //Get Id of Newly Created Line Item using list data API.
                            Map<String, String> params = new HashMap<>();

                            if (parentEntityTypeId == 61) {
                                params.put("contractId", String.valueOf(parentId));
                            } else if (parentEntityTypeId == 1) {
                                params.put("relationId", String.valueOf(parentId));
                            }

                            String payloadForListData = "{\"filterMap\":{\"entityTypeId\":165,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                                    "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                            String listDataResponse = ListDataHelper.getListDataResponse("invoice line item", payloadForListData, params);

                            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                                JSONObject jsonObj = new JSONObject(listDataResponse);
                                JSONArray jsonArr = jsonObj.getJSONArray("data");

                                if (jsonArr.length() > 0) {
                                    int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                                    String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");

                                    int newlyCreatedLineItemId = ListDataHelper.getRecordIdFromValue(idValue);

                                    //Validate Show Page of Newly Created Record.
                                    validateShowPage(newlyCreatedLineItemId, "invoice line item", 165, csAssert);

                                    validateAuditLog(newlyCreatedLineItemId, "invoice line item", 165, csAssert);

                                    EntityOperationsHelper.deleteEntityRecord("invoice line item", newlyCreatedLineItemId);
                                } else {
                                    csAssert.assertTrue(false, "Couldn't find any data in ListData Response for Invoice Line Item.");
                                }
                            } else {
                                csAssert.assertTrue(false, "ListData API Response for Invoice Line Item is an Invalid JSON.");
                            }
                        }
                    }

                    //Validate Show Page of Newly Created Record.
                    validateShowPage(newlyCreatedRecordId, entityName, entityTypeId, csAssert);

                    //Validate Local Listing
					validateLocalListing(newlyCreatedRecordId, entityName, parentEntityTypeId, parentId, entityTypeId, csAssert);

                    //Validate Audit Log
                    validateAuditLog(newlyCreatedRecordId, entityName, entityTypeId, csAssert);

                    EntityOperationsHelper.deleteEntityRecord(entityName, newlyCreatedRecordId);

                    validateLocalListing(newlyCreatedRecordId, entityName, parentEntityTypeId, parentId, entityTypeId, false,csAssert);

                } else {
                    String errorMessage = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Failed whereas it was supposed to Pass. Error Message: " + errorMessage);
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + bulkCreateUploadResponse + "]");
            }

            FileUtils.deleteFile(bulkCreateExcelFilePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Verifying BulkCreate Flow [" + flowToTest + "]. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    //This method covers Part of TC-C3490
    private void validateShowPage(int newlyCreatedRecordId, String entityName, int entityTypeId, CustomAssert csAssert) {
        try {
            logger.info("Validating Show Page of Newly Created Record Id {} of Entity {}", newlyCreatedRecordId, entityName);
            String showResponse = ShowHelper.getShowResponse(entityTypeId, newlyCreatedRecordId);

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                String showStatus = ParseJsonResponse.getStatusFromResponse(showResponse);

                if (!showStatus.equalsIgnoreCase("success")) {
                    csAssert.assertTrue(false, "Show API Response validation failed for Newly Created Record Id " + newlyCreatedRecordId +
                            " of Entity " + entityName + ". Show Response Status: [" + showStatus + "]");
                }
            } else {
                csAssert.assertTrue(false, "Show API Response for Newly Created Record Id " + newlyCreatedRecordId + " of Entity " + entityName +
                        " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Show Page of Record Id " + newlyCreatedRecordId + " of Entity " + entityName + ". " +
                    e.getMessage());
        }
    }

    //This method covers Part of TC-C3490
    private void validateLocalListing(int newlyCreatedRecordId, String entityName, int parentEntityTypeId, int parentId, int entityTypeId, CustomAssert csAssert) {
        try {
            logger.info("Validating Local Listing of Newly Created Record Id {} of Entity {}", newlyCreatedRecordId, entityName);
            Map<String, String> paramsForListData = new HashMap<>();

            switch (parentEntityTypeId) {
                case 61:
                    paramsForListData.put("contractId", String.valueOf(parentId));
                    break;

                case 1:
                    paramsForListData.put("relationId", String.valueOf(parentId));
                    break;

                default:
                    paramsForListData = null;
            }

            String listDataResponse = ListDataHelper.getListDataResponse(entityName, ListDataHelper.getDefaultPayloadForListData(entityTypeId, 1),
                    paramsForListData);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                JSONObject jsonObj = new JSONObject(listDataResponse);
                String idValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                int actualRecordId = ListDataHelper.getRecordIdFromValue(idValue);

                if (actualRecordId != newlyCreatedRecordId) {
                    csAssert.assertTrue(false, "Local Listing Validation Failed for Newly Created Record Id " + newlyCreatedRecordId +
                            " of Entity " + entityName + ". Expected Record Id: " + newlyCreatedRecordId + " and Actual Record Id found: " + actualRecordId);
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Local Listing of Newly Created Record Id " + newlyCreatedRecordId + " of Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateLocalListing(int newlyCreatedRecordId, String entityName, int parentEntityTypeId, int parentId, int entityTypeId,
                                      Boolean recShouldBePresent, CustomAssert csAssert) {
        try {
            logger.info("Validating Local Listing of Newly Created Record Id {} of Entity {}", newlyCreatedRecordId, entityName);
            Map<String, String> paramsForListData = new HashMap<>();

            switch (parentEntityTypeId) {
                case 61:
                    paramsForListData.put("contractId", String.valueOf(parentId));
                    break;

                case 1:
                    paramsForListData.put("relationId", String.valueOf(parentId));
                    break;

                default:
                    paramsForListData = null;
            }

            String listDataResponse = ListDataHelper.getListDataResponse(entityName, ListDataHelper.getDefaultPayloadForListData(entityTypeId, 1),
                    paramsForListData);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                JSONObject jsonObj = new JSONObject(listDataResponse);
                String idValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                int actualRecordId = ListDataHelper.getRecordIdFromValue(idValue);

                if(recShouldBePresent == false) {
                    if (actualRecordId == newlyCreatedRecordId) {
                        csAssert.assertTrue(false, "Local Listing Validation Failed for Deleted Record Id " + newlyCreatedRecordId +
                                " of Entity " + entityName + ". Should not be present but actually present");

                    }
                }else {
                    if (actualRecordId != newlyCreatedRecordId) {
                        csAssert.assertTrue(false, "Local Listing Validation Failed for Newly created Record Id " + newlyCreatedRecordId +
                                " of Entity " + entityName + ". Should be present but actually not present");

                    }
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Local Listing of Newly Created Record Id " + newlyCreatedRecordId + " of Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    //This method covers TC-C3487
    private void validateAuditLog(int newlyCreatedRecordId, String entityName, int entityTypeId, CustomAssert csAssert) {
        try {
            logger.info("Validating Audit Log for Newly Created Record Id {} of Entity {}", newlyCreatedRecordId, entityName);
            String tabListDataResponse = TabListDataHelper.getTabListDataResponse(entityTypeId, newlyCreatedRecordId, auditLogTabId);

            if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                JSONObject jsonObj = new JSONObject(tabListDataResponse);
                int actionNameColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "action_name");
                String actionName = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(actionNameColumnNo)).getString("value");

//                String expectedActionName = (entityTypeId == 165) ? "(Bulk)" : "(Bulk)";
                String expectedActionName = "(Bulk)";

                if (!actionName.contains(expectedActionName)) {
                    csAssert.assertTrue(false, "Audit Log Validation Failed for Record Id " + newlyCreatedRecordId + " of Entity " + entityName +
                            ". Expected Action Name: [" + expectedActionName + "] and Actual Action Name: [" + actionName + "]");
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Audit Log Tab of Record Id " + newlyCreatedRecordId + " of Entity " +
                        entityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log of Record Id " + newlyCreatedRecordId + " of Entity " + entityName +
                    ". " + e.getMessage());
        }
    }

    private void updateUniqueFields(String filePath,String fileName,String sheetToUpdate,String[] uniqueFields){

        try {
            List<String> rowDataList = XLSUtils.getExcelDataOfOneRow(filePath, fileName, sheetToUpdate, 2);
            int rowToUpdate = 6;
            String updatedValue;

            for (int i = 0; i < uniqueFields.length; i++) {

                for (int j = 0; j < rowDataList.size(); j++) {

                    if (rowDataList.get(j).equalsIgnoreCase(uniqueFields[i])) {

                        String unqString = DateUtils.getCurrentTimeStamp();
                        unqString = unqString.replaceAll("_","");
                        unqString = unqString.replaceAll(" ","");

                        updatedValue = new BigDecimal(unqString).toString().substring(10);
                        List<CellType> cellTypes = XLSUtils.getAllCellTypeOfRow(filePath,fileName,sheetToUpdate,rowToUpdate);

                        XLSUtils.updateColumnValue(filePath, fileName, sheetToUpdate, rowToUpdate, j, updatedValue);


                    }
                }

            }
        }catch (Exception e){
            logger.error("Error while updating unique fields");
        }

    }

}