package com.sirionlabs.test.bulkCreate;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class TestBulkCreateEndUserUploadTemplate {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkCreateEndUserUploadTemplate.class);
    private String templatePath;
    private Long schedulerJobTimeOut;
    private Long schedulerJobPollingTime;
    private Map<String, String> defaultProperties;
    private int contractEntityTypeId;
    private int actionEntityTypeId;
    private int serviceDataEntityTypeId;
    private int invoiceEntityTypeId;
    private int lineItemEntityTypeId;

    private BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

    @BeforeClass
    public void beforeClass() {
        String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateEndUserUploadTemplateConfigFilePath");
        String configFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateEndUserUploadTemplateConfigFileName");

        defaultProperties = ParseConfigFile.getAllDefaultProperties(configFilePath, configFileName);

        templatePath = defaultProperties.get("bulkcreatetemplatepath");
        schedulerJobTimeOut = Long.parseLong(defaultProperties.get("schedulerjobtimeout"));
        schedulerJobPollingTime = Long.parseLong(defaultProperties.get("schedulerjobpollingtime"));

        contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
        actionEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");
        serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
        invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
        lineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");

        //If user language is not English and then set it to English.
        UpdateAccount updateAccountObj = new UpdateAccount();
        String userLogin = ConfigureEnvironment.getEnvironmentProperty("j_username");
        int currentLanguageId = updateAccountObj.getCurrentLanguageIdForUser(userLogin, 1002);

        if (currentLanguageId != 1) {
            updateAccountObj.updateUserLanguage(userLogin, 1002, 1);
        }
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
    TC-C3449: Verity Template Pre-Processing. Error for invalid template format.
     */
    @Test
    public void testC3449() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3449: Verify Template Pre-Processing Error for Invalid Template Format");

            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "InvalidFormat.txt";

            copyTemplateFile(baseTemplateName, testTemplateName);

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (!uploadResponse.contains("File extension txt not supported")) {
                csAssert.assertTrue(false, "Expected Response: [File extension txt not supported] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test C3449. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3455: Verify that Rows which have no SL Number or Process Field Value should not be processed.
     */
    @Test
    public void testC3455() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3455: Verify that Rows which have no SL Number or Process Field Value should not be processed.");

            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "C3455.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, 0, "");

            if (!updateValue) {
                throw new SkipException("Couldn't Update SL No Value in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB.");
                    }

                    if (!errorMessages.toLowerCase().contains("only positive number allowed") || !errorMessages.contains("100000001")) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [only positive number allowed] and " +
                                "Field Id: [100000001] but Actual Error Message: [" + errorMessages + "]");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3455. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3457: Verify Template Pre-Processing. File Uploaded from Incorrect Parent.
    TC-C3458: Verify Template Pre-Processing. File Uploaded from Different Contract or Supplier.
     */
    @Test
    public void testC3457() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3457: Verify Template Pre-Processing. File Uploaded from Incorrect Parent.");
            String listDataResponse = ListDataHelper.getListDataResponse("contracts");
            int correctParentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    if (recordId == correctParentContractId) {
                        continue;
                    }

                    String showResponse = ShowHelper.getShowResponse(contractEntityTypeId, recordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForActionEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "actions");

                        if (hasBulkCreateOptionForActionEntity == null) {
                            throw new SkipException("Couldn't find whether Record Id " + recordId +
                                    " of Entity Contract has Bulk Create Option to Create Actions Entity or not.");
                        }

                        if (!hasBulkCreateOptionForActionEntity) {
                            continue;
                        }

                        String testTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
                        String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId,
                                recordId, actionEntityTypeId, 1023);

                        if (!uploadResponse.contains("Incorrect Template: Template Belongs To Different Parent Entity")) {
                            csAssert.assertTrue(false, "Bulk Template Upload Response Failed. " +
                                    "Expected Response: [Incorrect Template: Template Belongs To Different Parent Entity] and Actual Response: [" + uploadResponse + "]");
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity Contract is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity Contract is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3457. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3463: Verify that the rows having process column value as yes only should be processed.
     */
    @Test
    public void testC3463() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3463: Verify that the rows having process column value as yes only should be processed.");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "C3463.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Bulk Template.");
            }

            int processColumnNo = allHeaderIds.indexOf("100000002");

            if (processColumnNo == -1) {
                throw new SkipException("Couldn't get Column No of Process Field.");
            }

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, processColumnNo, "No");

            if (!updateValue) {
                throw new SkipException("Couldn't update Process Value as No in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                } else {
                    //Validate that No Record Processed.
                    String errorMessage = schedulerJob.get("errorMessage");

                    if (!errorMessage.trim().toLowerCase().contains("no record processed")) {
                        csAssert.assertTrue(false, "Expected Result: [No Record Processed] and Actual Result: [Record was processed successfully]");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3463. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3454: Verify Template Pre-Processing. Header Validation that no extra headers should be allowed.
     */
    @Test
    public void testC3454() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3454: Verify Template Pre-Processing Headers Validation. It should throw error when extra header is present in the template.");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "C3454.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaders = XLSUtils.getHeaders(templatePath, testTemplateName, "Action");

            if (allHeaders == null || allHeaders.isEmpty()) {
                throw new SkipException("Couldn't get All Headers from the Template.");
            }

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 0, allHeaders.size(), "Extra Header");

            if (!updateValue) {
                throw new SkipException("Couldn't Add Extra Header in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (!uploadResponse.toLowerCase().contains("incorrect headers")) {
                csAssert.assertTrue(false, "Bulk Template Upload Response Failed. Expected Response: [Incorrect Headers] and Actual Response: [" +
                        uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3454. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3501: Verify Entity should not get created when Contract field is empty in Template
    TC-C3502: Verify Error message when Contract field is empty in Template.
     */
    @Test
    public void testC3501() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3501: Verify Entity should not get created when Contract Field is empty in Template");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "C3501.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from the Template.");
            }

            if (allHeaderIds.contains("406")) {
                int contractColumnNo = allHeaderIds.indexOf("406");
                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, contractColumnNo, "");

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Contract Value in Template");
                }

                int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                        1023);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Create Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                    } else {
                        logger.info("Validating Validation Message from DB.");
                        String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB.");
                        }

                        if (!errorMessages.toLowerCase().contains("contract cell value cannot be empty") || !errorMessages.contains("406")) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. " +
                                    "Expected Error Message: [contract cell value cannot be empty] and Field Id: [406] but Actual Error Message: [" + errorMessages + "]");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } else {
                throw new SkipException("Couldn't get Column No of Contract Field from the Template.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3501. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C13367: Verify the Validation Messages for target level values in SL Bulk Create
     */
    @Test(enabled = false)
    public void testC13367() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-13367: Verify the Validation Messages for Target Level Values in SL Bulk Create");
            String baseTemplateName = defaultProperties.get("slbulkcreatetemplate1");
            String testTemplateName = "C13367.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "SLA", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from the Template.");
            }

            if (allHeaderIds.contains("216") && allHeaderIds.contains("219") && allHeaderIds.contains("218") && allHeaderIds.contains("220")) {
                int targetsFieldColumnNo = allHeaderIds.indexOf("216");
                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "SLA", 6, targetsFieldColumnNo, "");
                if (!updateValue) {
                    throw new SkipException("Couldn't Update Targets Field Value in Template");
                }

                int targetsExpectedFieldColumnNo = allHeaderIds.indexOf("219");
                updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "SLA", 6, targetsExpectedFieldColumnNo, "1");
                if (!updateValue) {
                    throw new SkipException("Couldn't Update Targets Expected Field Value in Template");
                }

                int targetsMaximumFieldColumnNo = allHeaderIds.indexOf("218");
                updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "SLA", 6, targetsMaximumFieldColumnNo, "2");
                if (!updateValue) {
                    throw new SkipException("Couldn't Update Targets Maximum Field Value in Template");
                }

                int targetsSignificantlyMaximumColumnNo = allHeaderIds.indexOf("220");
                updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "SLA", 6, targetsSignificantlyMaximumColumnNo, "3");
                if (!updateValue) {
                    throw new SkipException("Couldn't Update Targets Significantly Maximum Field Value in Template");
                }

                int parentContractId = Integer.parseInt(defaultProperties.get("slbulkcreatetemplate1parentcontractid"));
                int slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, slEntityTypeId,
                        1015);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Create Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                    } else {
                        logger.info("Validating Validation Message from DB.");

                        String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB.");
                        }

                        if (!errorMessages.toLowerCase().contains("targets cannot be set without selecting minimum/maximum? field") ||
                                !errorMessages.contains("216")) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Error Message: " +
                                    "[Targets Cannot Be Set Without Selecting Minimum/Maximum? Field] and Field Id: [216] but Actual Error Message: [" + errorMessages + "]");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } else {
                throw new SkipException("Couldn't get Column No of Target Fields from the Template.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-13367. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3476: Verify the Error Message for Contract Countries Field in Bulk Create Template
     */
    @Test(enabled = false)
    public void testC3476() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3476: Verify the Error message for Contract countries field in Bulk Create Template.");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplateforola");
            String testTemplateName = "C3476.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from the Template.");
            }

            if (allHeaderIds.contains("438")) {
                int contractCountriesFieldColumnNo = allHeaderIds.indexOf("438");

                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6,
                        contractCountriesFieldColumnNo, "");

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Contract Countries Field Value in Template");
                }

                int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplateforolaparentcontractid"));
                int slEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, slEntityTypeId,
                        1023);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Create Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                    } else {
                        logger.info("Validating Validation Message from DB.");

                        String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB.");
                        }

                        if (!errorMessages.toLowerCase().contains("please enter 'contract countries'") ||
                                !errorMessages.contains("438")) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Error Message: " +
                                    "[Please Enter 'Contract Countries'] and Field Id: [438] but Actual Error Message: [" + errorMessages + "]");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } else {
                throw new SkipException("Couldn't get Column No of Contract Countries Field from the Template.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3476. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3504: Verify error message when no Supplier is provided in Template.
     */
    @Test
    public void testC3504() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3504: Verify Error Message when no Supplier is Provided in Template.");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "C3504.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from the Template.");
            }

            if (allHeaderIds.contains("405")) {
                int supplierColumnNo = allHeaderIds.indexOf("405");
                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, supplierColumnNo, "");

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Supplier Value in Template");
                }

                int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                        1023);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Create Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                    } else {
                        logger.info("Validating Validation Message from DB.");
                        String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB.");
                        }

                        if (!errorMessages.toLowerCase().contains("cell value cannot be empty") || !errorMessages.contains("405")) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. " +
                                    "Expected Error Message: [cell value cannot be empty] and Field Id: [406] but Actual Error Message: [" + errorMessages + "]");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } else {
                throw new SkipException("Couldn't get Column No of Supplier Field from the Template.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3504. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3444: Verify System Validation - Title Field for Action Entity
    TC-C3468: Verify System Validation Error Message - Title field in Request Response Template
     */
    @Test
    public void testC3444() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3444: Verify System Validation for Title Field of Action Entity. Title Field having more than 512 characters");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "C3444.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Action Bulk Template");
            }

            if (!allHeaderIds.contains("403")) {
                throw new SkipException("Couldn't find Title Field Column in Action Bulk Template");
            }

            int titleColumnNo = allHeaderIds.indexOf("403");

            String titleValue = "Action Title Field Test More than 512 characters Action Title Field Test More than 512 characters Action Title Field " +
                    "Test More than 512 characters Action Title Field Test More than 512 characters Action Title Field Test More than 512 characters " +
                    "Action Title Field Test More than 512 characters Action Title Field Test More than 512 characters Action Title Field Test More than 512 characters " +
                    "Action Title Field Test More than 512 characters Action Title Field Test More than 512 characters Action Title Field Test";
            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, titleColumnNo, titleValue);

            if (!updateValue) {
                throw new SkipException("Couldn't Update Title Value in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                } else {
                    logger.info("Validating Validation Message from DB.");
                    FieldRenaming fieldRenamingObj = new FieldRenaming();
                    String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1, 467);

                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);
                    String expectedErrorMessage = fieldRenamingObj.getClientFieldNameFromId(fieldRenamingResponse, 8258);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB.");
                    }

                    if (!errorMessages.toLowerCase().contains(expectedErrorMessage.trim().toLowerCase()) || !errorMessages.contains("403")) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [" + expectedErrorMessage + "] and " +
                                "Field Id: [403] but Actual Error Message: [" + errorMessages + "]");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3444. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3505: Verify System Validation - Requested On Field for Action Entity
     */
    @Test
    public void testC3505() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3505: Verify System Validation - Requested On Field for Action Entity. Field having date in future.");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplateforsow");
            String testTemplateName = "C3505.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Action Bulk Template");
            }

            if (!allHeaderIds.contains("416")) {
                throw new SkipException("Couldn't find Requested On Field Column in Action Bulk Template");
            }

            int requestedOnFieldColumnNo = allHeaderIds.indexOf("416");

            String requestedOnValue = "05/01/2050";
            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, requestedOnFieldColumnNo, requestedOnValue);

            if (!updateValue) {
                throw new SkipException("Couldn't Update Requested On Value in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplateforsowparentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB.");
                    }

                    if (!errorMessages.toLowerCase().contains("please select a date in past") || !errorMessages.contains("416")) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [Please Select A Date In Past] and " +
                                "Field Id: [416] but Actual Error Message: [" + errorMessages + "]");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3505. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3506: Verify System Validation - Action Taken Field for Action Entity
    TC-C3474: Verify System Validation Error Message - Action Taken Field in Request Response Template
     */
    @Test(enabled = false)
    public void testC3506() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3506: Verify System Validation - Action Taken Field. Field having more than 1024 characters.");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplateforola");
            String testTemplateName = "C3506.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Action Bulk Template");
            }

            if (!allHeaderIds.contains("424")) {
                throw new SkipException("Couldn't find Action Taken Field Column in Action Bulk Template");
            }

            int actionTakenColumnNo = allHeaderIds.indexOf("424");

            String actionTakenValue = "Action Field Action Taken having more than 1024 characters Action Field Action Taken having more than 1024 characters Action " +
                    "Field Action Taken having more than 1024 characters Action Field Action Taken having more than 1024 characters Action Field Action Taken having " +
                    "more than 1024 characters Action Field Action Taken having more than 1024 characters Action Field Action Taken having more than 1024 characters " +
                    "Action Field Action Taken having more than 1024 characters Action Field Action Taken having more than 1024 characters Action Field Action Taken " +
                    "having more than 1024 characters Action Field Action Taken having more than 1024 characters Action Field Action Taken having more than 1024 characters " +
                    "Action Field Action Taken having more than 1024 characters Action Field Action Taken having more than 1024 characters Action Field Action Taken having " +
                    "more than 1024 characters Action Field Action Taken having more than 1024 characters Action Field Action Taken having more than 1024 characters Action " +
                    "Field Action Take";

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, actionTakenColumnNo, actionTakenValue);

            if (!updateValue) {
                throw new SkipException("Couldn't Update Action Taken Value in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplateforolaparentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB.");
                    }

                    if (!errorMessages.toLowerCase().contains("please enter less than 1024 characters") || !errorMessages.contains("424")) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [Please enter less than 1024 characters] and " +
                                "Field Id: [424] but Actual Error Message: [" + errorMessages + "]");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3506. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3477: Verify System Validation - Financial Impact Field for Action Entity.
     */
    @Test
    public void testC3477() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3477: Verify System Validation for Financial Impact Field of Action Entity having more than 18 digits.");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "C3477.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Action Bulk Template");
            }

            if (!allHeaderIds.contains("422")) {
                throw new SkipException("Couldn't find Financial Impact Field Column in Action Bulk Template");
            }

            int financialImpactColumnNo = allHeaderIds.indexOf("422");

            String financialImpactValue = "1234567891234567891";

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, financialImpactColumnNo, financialImpactValue);

            if (!updateValue) {
                throw new SkipException("Couldn't Update Financial Impact Value in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB.");
                    }

                    if (!errorMessages.toLowerCase().contains("please enter a value not having more than 18 digits in integral part and 5 digits in fractional part") ||
                            !errorMessages.contains("422")) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: " +
                                "[Please enter a value not having more than 18 digits in integral part and 5 digits in fractional part] and " +
                                "Field Id: [422] but Actual Error Message: [" + errorMessages + "]");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3477. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3434: Verify System Mandatory Field Validation - TimeZone Field should be Mandatory
     */
    @Test
    public void testC3434() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3434: Verify System Mandatory Field Validation. Time Zone field should be mandatory.");
            String baseTemplateName = defaultProperties.get("actionbulkcreatetemplate1");
            String testTemplateName = "C3434.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Action", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Action Bulk Template");
            }

            if (!allHeaderIds.contains("414")) {
                throw new SkipException("Couldn't find Time Zone Field Column in Action Bulk Template");
            }

            int timeZoneColumnNo = allHeaderIds.indexOf("414");

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Action", 6, timeZoneColumnNo, "");

            if (!updateValue) {
                throw new SkipException("Couldn't Update Time Zone Value in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("actionbulkcreatetemplate1parentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, actionEntityTypeId,
                    1023);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB.");
                    }

                    if (!errorMessages.toLowerCase().contains("time zone cannot be empty") || !errorMessages.contains("414")) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [Time Zone cannot be empty] and " +
                                "Field Id: [414] but Actual Error Message: [" + errorMessages + "]");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3434. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3967: Verify Error for Uploading Template with Changes in Format and Headers for Service Data.
     */
    @Test
    public void testC3967() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3967: Verify Error for Uploading Template with Changes in Format and Headers for Service Data.");
            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforfixedfee");
            String testTemplateName = "C3967.xlsm";
            String sheetName = "Service Data";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, sheetName, 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Service Data Bulk Template");
            }

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, sheetName, 1, 0, "-1");

            if (!updateValue) {
                throw new SkipException("Couldn't Update SL No Header Id in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforfixedfeeparentcontractid"));

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, serviceDataEntityTypeId,
                    1001);

            if (!uploadResponse.toLowerCase().contains("incorrect headers")) {
                csAssert.assertTrue(false, "Upload Response Failed. Expected Response: [Incorrect Headers] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3967. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3968: Verify Errors for Uploading Template with Addition/Deletion of Columns and Sheet
     */
    @Test
    public void testC3968() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3968: Verify Errors for Uploading Template with Addition/Deletion of Columns and Sheet.");
            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforfixedfee");
            String testTemplateName = "C3968.xlsm";
            String sheetName = "Service Data";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaders = XLSUtils.getHeaders(templatePath, testTemplateName, sheetName);

            if (allHeaders == null || allHeaders.isEmpty()) {
                throw new SkipException("Couldn't get All Headers from Service Data Bulk Template");
            }

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, sheetName, 0, allHeaders.size(), "New Extra Header");

            if (!updateValue) {
                throw new SkipException("Couldn't Add New Extra Header in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforfixedfeeparentcontractid"));

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, serviceDataEntityTypeId,
                    1001);

            if (!uploadResponse.toLowerCase().contains("incorrect headers")) {
                csAssert.assertTrue(false, "Upload Response Failed. Expected Response: [Incorrect Headers] and Actual Response: [" + uploadResponse +
                        "] for Adding Extra Header.");
            }

            copyTemplateFile(baseTemplateName, testTemplateName);

            updateValue = XLSUtils.createNewSheet(templatePath, testTemplateName, "Extra Sheet");

            if (!updateValue) {
                throw new SkipException("Couldn't Add New Extra Sheet in Bulk Template.");
            }

            uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, serviceDataEntityTypeId,
                    1001);

            if (!uploadResponse.toLowerCase().contains("template is not correct")) {
                csAssert.assertTrue(false, "Upload Response Failed. Expected Response: [Template is not correct] and Actual Response: [" +
                        uploadResponse + "] for Adding Extra Sheet.");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3968. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4122: Verify Error for Uploading Template with Start Date greater than End Date.
     */
    @Test
    public void testC4122() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4122: Verify Error for Uploading Template with Start Date greater than End Date.");
            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforc4122");
            String testTemplateName = "C4122.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforc4122parentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, serviceDataEntityTypeId,
                    1001);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB.");
                    }

                    if (!errorMessages.toLowerCase().contains("start date cannot be after end date") || !errorMessages.contains("8054")) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [Start Date Cannot Be After End Date] and " +
                                "Field Id: [8054] but Actual Error Message: [" + errorMessages + "]");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4122. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3925: Verify Mandatory Fields Validation for Service Data Bulk Create Template
     */
    @Test
    public void testC3925() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3925: Verify Mandatory Fields Validation for Service Data Bulk Create Template.");
            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforfixedfee");
            String testTemplateName = "C3925.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Service Data", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Service Data Bulk Template");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforfixedfeeparentcontractid"));

            //Verify Part 1: Blank Value in Mandatory Field.
            verifyServiceDataFieldValidation(testTemplateName, "C3925-1.xlsm", allHeaderIds, "4038", "Name", "",
                    "Name Cannot Be Empty", "actions", contractEntityTypeId, parentContractId,
                    "Mandatory Field Name is Blank in Service Data", csAssert);

            //Verify Part 2: Text Field exceeding characters limit.
            String updatedValue = "Test Service Data Name having more than 512 characters Test Service Data Name having more than 512 characters Test Service Data " +
                    "Name having more than 512 characters Test Service Data Name having more than 512 characters Test Service Data Name having more than 512 " +
                    "characters Test Service Data Name having more than 512 characters Test Service Data Name having more than 512 characters Test Service Data Name " +
                    "having more than 512 characters Test Service Data Name having more than 512 characters Test Service DataN";

            verifyServiceDataFieldValidation(testTemplateName, "C3925-2.xlsm", allHeaderIds, "4038", "Name", updatedValue,
                    "Please enter less than 512 characters", "actions", contractEntityTypeId, parentContractId,
                    "Text Field Exceeding Characters limit", csAssert);

            //Verify Part 3: Without Serial Number
            verifyServiceDataFieldValidation(testTemplateName, "C3925-3.xlsm", allHeaderIds, "100000001", "SL No", "",
                    "Only positive Number allowed", "actions", contractEntityTypeId, parentContractId,
                    "Without Serial Number", csAssert);

            //Verify Part 4: Negative Integer Values.
            verifyServiceDataFieldValidation(testTemplateName, "C3925-4.xlsm", allHeaderIds, "11339", "Acceptable Forecast Period",
                    "-1", "Please enter positive integers only", "actions", contractEntityTypeId, parentContractId,
                    "Negative Integer Value", csAssert);

            //Verify Part 5: Values other than Integer in Numeric Field.
            verifyServiceDataFieldValidation(testTemplateName, "C3925-5.xlsm", allHeaderIds, "11339", "Acceptable Forecast Period",
                    "T1", "Please enter positive integers only", "actions", contractEntityTypeId, parentContractId,
                    "Values other than Integer in Numeric Field", csAssert);

            //Verify Part 6: Values other than Date in Date Field.
            verifyServiceDataFieldValidation(testTemplateName, "C3925-6.xlsm", allHeaderIds, "8054", "Service Start Date",
                    "Date Value", "Only date values are allowed", "actions", contractEntityTypeId, parentContractId,
                    "Values other than Date in Date Field", csAssert);

            //Verify Part 8: Without Supplier Name
            verifyServiceDataFieldValidation(testTemplateName, "C3925-8.xlsm", allHeaderIds, "11767", "Supplier",
                    "", "Cell Value Cannot Be Empty", "actions", contractEntityTypeId, parentContractId,
                    "Without Supplier Name", csAssert);

            //Verify Part 9: Without Contract Name
            verifyServiceDataFieldValidation(testTemplateName, "C3925-9.xlsm", allHeaderIds, "4040", "Contract",
                    "", "Cell Value Cannot Be Empty", "actions", contractEntityTypeId, parentContractId,
                    "Without Contract Name", csAssert);

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3925: " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void verifyServiceDataFieldValidation(String baseTemplateName, String testTemplateName, List<String> allHeaderIds, String headerId, String headerName,
                                                  String updatedValue, String expectedErrorMessage, String entityName, int contractEntityTypeId, int parentContractId,
                                                  String additionalInfo, CustomAssert csAssert) {
        try {
            logger.info("Validating Case {}", additionalInfo);
            copyTemplateFile(baseTemplateName, testTemplateName);

            if (!allHeaderIds.contains(headerId)) {
                throw new SkipException("Couldn't find " + headerName + " Field Column in Service Data Bulk Template for case. " + additionalInfo);
            }

            int fieldColumnNo = allHeaderIds.indexOf(headerId);

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Service Data", 6, fieldColumnNo, updatedValue);

            if (!updateValue) {
                throw new SkipException("Couldn't Update " + headerName + " Value in Bulk Template for case. " + additionalInfo);
            }

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, serviceDataEntityTypeId,
                    1001);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further for case. " + additionalInfo);
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail for case. " + additionalInfo);

                    int newlyCreatedRecordId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);
                    EntityOperationsHelper.deleteEntityRecord(entityName, newlyCreatedRecordId);
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB for case. " + additionalInfo);
                    }

                    if (!errorMessages.toLowerCase().contains(expectedErrorMessage.toLowerCase()) || !errorMessages.contains(headerId)) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [" + expectedErrorMessage + "] and " +
                                "Field Id: [" + headerId + "] but Actual Error Message: [" + errorMessages + "] for case. " + additionalInfo);
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for case. " + additionalInfo);
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating " + additionalInfo + ". " + e.getMessage());
        }
    }


    /*
    TC-C4040: Verify that the rows having process column value as 'yes' should only be processed for Service Data Entity.
     */
    @Test
    public void testC4040() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4040: Verify that Rows which have no Process Field Value as No should not be processed for Service Data.");

            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforfixedfee");
            String testTemplateName = "C4040.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Service Data", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids.");
            }

            int processColumnNo = allHeaderIds.indexOf("100000002");

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Service Data", 6, processColumnNo, "No");

            if (!updateValue) {
                throw new SkipException("Couldn't Update Process Value in Bulk Template.");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforfixedfeeparentcontractid"));

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, serviceDataEntityTypeId,
                    1001);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                fetchObj.hitFetch();
                int noOfSubmittedRecords = UserTasksHelper.getNoOfSubmittedRecordsForTask(fetchObj.getFetchJsonStr(), newTaskId);

                if (noOfSubmittedRecords != 0) {
                    csAssert.assertTrue(false, "Expected No of Submitted Records in Fetch API Response: 0 and Actual No of Submitted Records: " +
                            noOfSubmittedRecords);
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4040. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4044: Verify error message when contract field is empty in template for Service Data Entity.
     */
    @Test
    public void testC4044() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4044: Verify Error Message when Contract Field is empty in Template for Service Data.");
            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforfixedfee");
            String testTemplateName = "C4044.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Service Data", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from the Template.");
            }

            if (allHeaderIds.contains("4040")) {
                int contractColumnNo = allHeaderIds.indexOf("4040");
                boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Service Data", 6, contractColumnNo, "");

                if (!updateValue) {
                    throw new SkipException("Couldn't Update Contract Value in Template");
                }

                int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforfixedfeeparentcontractid"));

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId, serviceDataEntityTypeId,
                        1001);

                if (uploadResponse.contains("Your request has been submitted")) {
                    logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Create Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                        throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                    }

                    if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                        csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail.");
                    } else {
                        logger.info("Validating Validation Message from DB.");
                        String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                        if (errorMessages == null) {
                            throw new SkipException("Couldn't get Error Messages from DB.");
                        }

                        if (!errorMessages.toLowerCase().contains("cell value cannot be empty") || !errorMessages.contains("4040")) {
                            csAssert.assertTrue(false, "Error Messages Validation Failed. " +
                                    "Expected Error Message: [contract cell value cannot be empty] and Field Id: [4040] but Actual Error Message: [" + errorMessages + "]");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                            "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
                }

                FileUtils.deleteFile(templatePath + "/" + testTemplateName);
            } else {
                throw new SkipException("Couldn't get Column No of Contract Field from the Template.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4044. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4076: Verify that Service Data should not be created due to multiple reasons.
     */
    @Test(enabled = false)
    public void testC4076() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4076: Verify that Service Data should not be created due to multiple reasons.");
            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforfixedfee");
            String testTemplateName = "C4076.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Service Data", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Service Data Bulk Template");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforfixedfeeparentcontractid"));

            //Verify Part 2: Invoicing Type Fixed Fee and Consumption Flag on.
            verifyServiceDataFieldValidation(testTemplateName, "C4076-2.xlsm", allHeaderIds, "11335", "Consumption Available",
                    "Yes", "Fixed Fee Services Cannot Have Consumption", "service data", contractEntityTypeId, parentContractId,
                    "Invoicing Type Fixed Fee and Consumption Flag On", csAssert);

            //Verify Part 3: Forecast Invoicing Type and other Forecast fields empty.
            verifyServiceDataFieldValidation(testTemplateName, "C4076-3.xlsm", allHeaderIds, "11336", "Invoicing Type",
                    "Forecast", "Forecast", "service data",
                    contractEntityTypeId, parentContractId, "Forecast Invoicing Type and other Forecast fields empty.", csAssert);

            //Verify Part 4: Service Id (Supplier) same as existing.
            verifyServiceDataFieldValidation(testTemplateName, "C4076-4.xlsm", allHeaderIds, "11041", "Service Id (Supplier)",
                    "SD Pricing Template Validation Supplier", "Service Id Supplier already used in system", "service data",
                    contractEntityTypeId, parentContractId, "Service Id (Supplier) same as existing", csAssert);

            //Verify Part 5: Service Id (Client) same as existing.
            verifyServiceDataFieldValidation(testTemplateName, "C4076-5.xlsm", allHeaderIds, "11040", "Service Id (Client)",
                    "SD Pricing Template Validation Client", "Service Id Client already used in system", "service data",
                    contractEntityTypeId, parentContractId, "Service Id (Client) same as existing", csAssert);

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4076: " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C46359: Serial Number Validation in Bulk Create Template
     */
    @Test
    public void testC46359() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C46359: Verify Serial Number Validation in Bulk Create Template.");
            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforfixedfee");
            String testTemplateName = "C46359.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Service Data", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Service Data Bulk Template");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforfixedfeeparentcontractid"));

            //Verify Part 1: SL No Empty.
            verifyServiceDataFieldValidation(testTemplateName, "C46359-1.xlsm", allHeaderIds, "100000001", "SL No",
                    "", "Only positive Number allowed", "service data", contractEntityTypeId, parentContractId,
                    "SL No is Empty", csAssert);

            //Verify Part 2: SL No as 0 Value.
            verifyServiceDataFieldValidation(testTemplateName, "C46359-2.xlsm", allHeaderIds, "100000001", "SL No",
                    "0", "Only positive Number allowed", "service data", contractEntityTypeId, parentContractId,
                    "SL No as 0.", csAssert);

            //Verify Part 3: SL No as Negative Value.
            verifyServiceDataFieldValidation(testTemplateName, "C46359-3.xlsm", allHeaderIds, "100000001", "SL No",
                    "-1", "Only positive Number allowed", "service data", contractEntityTypeId, parentContractId,
                    "SL No has Negative Value", csAssert);

            //Verify Part 4: SL No has alphabet.
            verifyServiceDataFieldValidation(testTemplateName, "C46359-4.xlsm", allHeaderIds, "100000001", "SL No",
                    "a", "Only positive Number allowed", "service data", contractEntityTypeId, parentContractId,
                    "SL No has alphabet", csAssert);

            //Verify Part 5: SL No has special character.
            verifyServiceDataFieldValidation(testTemplateName, "C46359-5.xlsm", allHeaderIds, "100000001", "SL No",
                    "@", "Only positive Number allowed", "service data", contractEntityTypeId, parentContractId,
                    "SL No has special character", csAssert);

            //Verify Part 6: SL No has decimal value.
            verifyServiceDataFieldValidation(testTemplateName, "C46359-6.xlsm", allHeaderIds, "100000001", "SL No",
                    "1.2", "Only positive Number allowed", "service data", contractEntityTypeId, parentContractId,
                    "SL No has decimal value", csAssert);

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C46359: " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4125: Verify that for Repeated Serial Number only the last record should be created.
     */
    @Test(enabled = false)
    public void testC4125() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4125: Verify that for Repeated Serial Number only the last record should be created.");
            String baseTemplateName = defaultProperties.get("servicedatabulkcreatetemplateforfixedfee");
            String testTemplateName = "C4125.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Service Data", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Service Data Bulk Template");
            }

            int parentContractId = Integer.parseInt(defaultProperties.get("servicedatabulkcreatetemplateforfixedfeeparentcontractid"));

            Boolean rowCopied = XLSUtils.copyRowData(templatePath, testTemplateName, "Service Data", 6, 7);

            if (!rowCopied) {
                throw new SkipException("Couldn't Copy Row in Bulk Create Template.");
            }

            int nameColumnNo = allHeaderIds.indexOf("4038");
            String newNameValue = "API Automation Bulk Create Repeated SL No.";

            boolean updatedValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Service Data", 6, nameColumnNo,
                    newNameValue);

            if (!updatedValue) {
                throw new SkipException("Couldn't update Name Value in Bulk Create Template.");
            }

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String bulkCreateUploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentContractId,
                    serviceDataEntityTypeId, 1001);

            if (bulkCreateUploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("false")) {
                    int newlyCreatedRecordId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);

                    if (newlyCreatedRecordId != -1) {
                        String showResponse = ShowHelper.getShowResponse(serviceDataEntityTypeId, newlyCreatedRecordId);

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            String nameHierarchy = ShowHelper.getShowFieldHierarchy("name", serviceDataEntityTypeId);
                            String actualNameValue = ShowHelper.getActualValue(showResponse, nameHierarchy);

                            if (actualNameValue == null) {
                                throw new SkipException("Couldn't get Actual Name of Newly Created Service Data having Id " + newlyCreatedRecordId);
                            }

                            if (!actualNameValue.equalsIgnoreCase(newNameValue)) {
                                csAssert.assertTrue(false, "Expected Name Value: " + newNameValue + " and Actual Name Value: " + actualNameValue);
                            }
                        } else {
                            csAssert.assertTrue(false, "Show Response of Newly Created Service Data having Id " + newlyCreatedRecordId +
                                    " is an Invalid JSON.");
                        }

                        EntityOperationsHelper.deleteEntityRecord("service data", newlyCreatedRecordId);
                    } else {
                        csAssert.assertTrue(false, "TC-C4125 failed. No record created.");
                    }
                } else {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to Fail.");
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + bulkCreateUploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4125: " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3992: Verify SL No. and Process Column Validation.
     */
    @Test
    public void testC3992() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3992.");
            String baseTemplateName = defaultProperties.get("lineitembulkcreatetemplatebase1");
            String testTemplateName = "C3992.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Invoice Line Item", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids.");
            }

            int parentInvoiceId = Integer.parseInt(defaultProperties.get("lineitembulkcreatetemplatebase1parentinvoiceid"));

            verifyC3992Part1(baseTemplateName, allHeaderIds, parentInvoiceId, csAssert);
            verifyC3992Part2(baseTemplateName, allHeaderIds, parentInvoiceId, csAssert);

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3992. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void verifyC3992Part1(String baseTemplateName, List<String> allHeaderIds, int parentInvoiceId, CustomAssert csAssert) {
        try {
            logger.info("Validating TC-C3992 Part 1. Verify that Rows which have no Process Field Value as No should not be processed for Invoice Line Item.");
            String testTemplateName = "C3992-1.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            int processColumnNo = allHeaderIds.indexOf("100000002");

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Invoice Line Item", 6, processColumnNo, "No");

            if (!updateValue) {
                throw new SkipException("Couldn't Update Process Value in Bulk Template.");
            }

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, invoiceEntityTypeId, parentInvoiceId, lineItemEntityTypeId,
                    1004);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                fetchObj.hitFetch();
                int noOfSubmittedRecords = UserTasksHelper.getNoOfSubmittedRecordsForTask(fetchObj.getFetchJsonStr(), newTaskId);

                if (noOfSubmittedRecords != 0) {
                    csAssert.assertTrue(false, "Expected No of Submitted Records in Fetch API Response: 0 and Actual No of Submitted Records: " +
                            noOfSubmittedRecords);
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C3992 Part 1. " + e.getMessage());
        }
    }

    private void verifyC3992Part2(String baseTemplateName, List<String> allHeaderIds, int parentInvoiceId, CustomAssert csAssert) {
        try {
            logger.info("Validating TC-C3992 Part 2. If more than 2 rows have the same S.No then first record should be created.");
            String testTemplateName = "C3992-2.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            Boolean rowCopied = XLSUtils.copyRowData(templatePath, testTemplateName, "Invoice Line Item", 6, 7);

            if (!rowCopied) {
                throw new SkipException("Couldn't Copy Row in Bulk Create Template.");
            }

            int nameColumnNo = allHeaderIds.indexOf("11047");
            String newNameValue = "API Automation Bulk Create Repeated SL No.";

            boolean updatedValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Invoice Line Item", 6, nameColumnNo,
                    newNameValue);

            if (!updatedValue) {
                throw new SkipException("Couldn't update Name Value in Bulk Create Template.");
            }

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String bulkCreateUploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, invoiceEntityTypeId, parentInvoiceId,
                    lineItemEntityTypeId, 1004);

            if (bulkCreateUploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("false")) {
                    int newlyCreatedRecordId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);

                    if (newlyCreatedRecordId == -1) {
                        throw new SkipException("Couldn't get Newly Created Record Id from Bulk Template.");
                    }

                    String showResponse = ShowHelper.getShowResponse(lineItemEntityTypeId, newlyCreatedRecordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        String nameHierarchy = ShowHelper.getShowFieldHierarchy("name", lineItemEntityTypeId);
                        String actualNameValue = ShowHelper.getActualValue(showResponse, nameHierarchy);

                        if (actualNameValue == null) {
                            throw new SkipException("Couldn't get Actual Name of Newly Created Line Item having Id " + newlyCreatedRecordId);
                        }

                        if (!actualNameValue.equalsIgnoreCase(newNameValue)) {
                            csAssert.assertTrue(false, "Expected Name Value: " + newNameValue + " and Actual Name Value: " + actualNameValue);
                        }
                    } else {
                        csAssert.assertTrue(false, "Show Response of Newly Created Line Item having Id " + newlyCreatedRecordId +
                                " is an Invalid JSON.");
                    }

                    EntityOperationsHelper.deleteEntityRecord("invoice line item", newlyCreatedRecordId);
                } else {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to Fail.");
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + bulkCreateUploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C3992 Part 2. " + e.getMessage());
        }
    }


    /*
    TC-C4034: Verify Various Error Messages for Invoice & Line Item Bulk Create Template
     */
    @Test
    public void testC4034() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4034: Verify Various Error Messages for Invoice & Line Item Bulk Create Template");
            String[] parentEntitiesArr = {"suppliers", "contracts", "invoices"};
            String testTemplateName = "C4034.xlsm";

            for (String parentEntityName : parentEntitiesArr) {
                String baseTemplateName;

                if (parentEntityName.equalsIgnoreCase("invoices")) {
                    baseTemplateName = defaultProperties.get("lineitembulkcreatetemplatebase1");
                } else {
                    baseTemplateName = parentEntityName.equalsIgnoreCase("suppliers") ? defaultProperties.get("invoicebulkcreatetemplatesupplierbase") :
                            defaultProperties.get("invoicebulkcreatetemplatecontractbase");
                }

                copyTemplateFile(baseTemplateName, testTemplateName);

                String sheetName = parentEntityName.equalsIgnoreCase("invoices") ? "Invoice Line Item" : "Invoice";
                String entityName = parentEntityName.equalsIgnoreCase("invoices") ? "invoice line item" : "invoices";
                int entityTypeId = parentEntityName.equalsIgnoreCase("invoices") ? lineItemEntityTypeId : invoiceEntityTypeId;
                int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);
                int templateId = parentEntityName.equalsIgnoreCase("invoices") ? 1004 : 1013;

                int parentRecordId;
                if (parentEntityName.equalsIgnoreCase("invoices")) {
                    parentRecordId = Integer.parseInt(defaultProperties.get("lineitembulkcreatetemplatebase1parentinvoiceid"));
                } else {
                    parentRecordId = Integer.parseInt(parentEntityName.equalsIgnoreCase("contracts") ?
                            defaultProperties.get("invoicebulkcreatetemplatecontractbaseparentcontractid") :
                            defaultProperties.get("invoicebulkcreatetemplatesupplierbaseparentsupplierid"));
                }

                List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, sheetName, 2);

                if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                    throw new SkipException("Couldn't get All Header Ids from Invoice Bulk Template [" + testTemplateName + "]");
                }

                if (parentEntityName.equalsIgnoreCase("invoices")) {
                    //Verify Mandatory Field Check.
                    verifyInvoiceAndLineItemFieldValidation(testTemplateName, "C4034-1.xlsm", allHeaderIds, sheetName, "11047",
                            "Line Item Description", "", "Cell Value Cannot Be Empty", entityName, entityTypeId,
                            parentEntityTypeId, parentRecordId, templateId,
                            "Line Item Description Field (Mandatory) is blank for Line Item Bulk Create for Parent Entity " + parentEntityName, csAssert);

                    //Verify Limit Exceeded Check.
                    verifyInvoiceAndLineItemFieldValidation(testTemplateName, "C4034-2.xlsm", allHeaderIds, sheetName, "11072",
                            "Supplier - Rate", "12345678910111213",
                            "Please enter a non negative value not having more than 14 digits in integral part and 12 digits in fractional part",
                            entityName, entityTypeId, parentEntityTypeId, parentRecordId, templateId,
                            "Limit Exceeding in Supplier - Rate Field for Line Item Bulk Create for Parent Entity " + parentEntityName, csAssert);

                    //Verify Incorrect Data Type Check.
                    verifyInvoiceAndLineItemFieldValidation(testTemplateName, "C4034-3.xlsm", allHeaderIds, sheetName, "11042",
                            "Line Item Number", "Incorrect Data Type",
                            "Please enter positive integers only", entityName, entityTypeId, parentEntityTypeId, parentRecordId, templateId,
                            "Incorrect Data Type in Line Item Number Field for Line Item Bulk Create for Parent Entity " + parentEntityName, csAssert);

                    //Verify Invalid Data Check.
                    verifyInvoiceAndLineItemFieldValidation(testTemplateName, "C4034-4.xlsm", allHeaderIds, sheetName, "11052",
                            "Supplier - Currency", "Invalid Currency (INC)",
                            "Invalid option", entityName, entityTypeId, parentEntityTypeId, parentRecordId, templateId,
                            "Invalid Currency in Supplier - Currency Field for Line Item Bulk Create for Parent Entity " + parentEntityName, csAssert);
                } else {
                    //Verify Mandatory Field Check.
                    verifyInvoiceAndLineItemFieldValidation(testTemplateName, "C4034-1.xlsm", allHeaderIds, sheetName, "637",
                            "Supplier", "", "Cell Value Cannot Be Empty", entityName, entityTypeId, parentEntityTypeId,
                            parentRecordId, templateId, "Supplier Field (Mandatory) is blank for Invoice Bulk Create for Parent Entity " + parentEntityName,
                            csAssert);

                    //Verify Limit Exceeded Check.
                    FieldRenaming fieldRenamingObj = new FieldRenaming();
                    String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1, 467);
                    String expectedErrorMessage = fieldRenamingObj.getClientFieldName(fieldRenamingResponse, "Please enter less than 18 characters");

                    verifyInvoiceAndLineItemFieldValidation(testTemplateName, "C4034-2.xlsm", allHeaderIds, sheetName, "604",
                            "PO Number", "Limit Exceeding Test", expectedErrorMessage, entityName, entityTypeId, parentEntityTypeId,
                            parentRecordId, templateId, "Limit Exceeding in PO Number Field for Invoice Bulk Create for Parent Entity " + parentEntityName,
                            csAssert);

                    //Verify Incorrect Data Type Check.
                    verifyInvoiceAndLineItemFieldValidation(testTemplateName, "C4034-3.xlsm", allHeaderIds, sheetName, "11077",
                            "Payment Term", "Incorrect Data Type", "Please enter positive integers only", entityName,
                            entityTypeId, parentEntityTypeId, parentRecordId, templateId,
                            "Incorrect Data Type in Payment Term Field for Invoice Bulk Create for Parent Entity " + parentEntityName, csAssert);
                }
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C4034. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void verifyInvoiceAndLineItemFieldValidation(String baseTemplateName, String testTemplateName, List<String> allHeaderIds, String sheetName, String headerId,
                                                         String headerName, String updatedValue, String expectedErrorMessage, String entityName, int entityTypeId,
                                                         int parentEntityTypeId, int parentRecordId, int templateId, String additionalInfo, CustomAssert csAssert) {
        try {
            logger.info("Validating Case {}", additionalInfo);
            copyTemplateFile(baseTemplateName, testTemplateName);

            if (!allHeaderIds.contains(headerId)) {
                throw new SkipException("Couldn't find " + headerName + " Field Column in Invoice Bulk Template for case. " + additionalInfo);
            }

            int fieldColumnNo = allHeaderIds.indexOf(headerId);

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, sheetName, 6, fieldColumnNo, updatedValue);

            if (!updateValue) {
                throw new SkipException("Couldn't Update " + headerName + " Value in Bulk Template for case. " + additionalInfo);
            }

            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, parentEntityTypeId, parentRecordId, entityTypeId,
                    templateId);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further for case. " + additionalInfo);
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    csAssert.assertTrue(false, "Bulk Create Scheduler Job Passed whereas it was supposed to fail for case. " + additionalInfo);

                    int newlyCreatedRecordId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);
                    EntityOperationsHelper.deleteEntityRecord(entityName, newlyCreatedRecordId);
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB for case. " + additionalInfo);
                    }

                    if (!errorMessages.toLowerCase().contains(expectedErrorMessage.toLowerCase()) || !errorMessages.contains(headerId)) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [" + expectedErrorMessage + "] and " +
                                "Field Id: [" + headerId + "] but Actual Error Message: [" + errorMessages + "] for case. " + additionalInfo);
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for case. " + additionalInfo);
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating " + additionalInfo + ". " + e.getMessage());
        }
    }


    /*
    TC-C4328: Verify Field Label Validation Error Message in Bulk Template
     */
    @Test
    public void testC4328() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInPassword = Check.lastLoggedInUserPassword;
        Check checkObj = new Check();

        try {
            logger.info("Starting Test TC-C4328: Verify Field Label Validation Error Message in Bulk Template.");
            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            logger.info("Updating Validation Message for PO Number in Invoice Bulk Create.");
            logger.info("Hitting Field Label API findLabelsByGroupIdAndLanguageId for Group Id 467 and Language Id 1");
            FieldRenaming fieldRenamingObj = new FieldRenaming();
            String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1, 467);

            if (ParseJsonResponse.validJsonResponse(fieldRenamingResponse)) {
                JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("fieldLabels");
                String originalFieldLabel = null;
                String updatedFieldLabel = "Updated Error Message for PO Number Field";
                String updatePayload = fieldRenamingResponse;
                boolean fieldFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    String fieldName = jsonObj.getString("name");

                    if (fieldName.equalsIgnoreCase("Please enter less than 18 characters")) {
                        fieldFound = true;
                        originalFieldLabel = jsonObj.getString("clientFieldName");
                        updatePayload = updatePayload.replace("clientFieldName\":\"" + originalFieldLabel, "clientFieldName\":\"" +
                                updatedFieldLabel);
                        break;
                    }
                }

                if (!fieldFound) {
                    throw new SkipException("Couldn't find [Please enter less than 18 characters] Field in Field Label API Response.");
                }

                logger.info("Hitting Field Label Update API.");
                String fieldUpdateResponse = fieldRenamingObj.hitFieldUpdate(updatePayload);

                if (ParseJsonResponse.validJsonResponse(fieldUpdateResponse)) {
                    jsonObj = new JSONObject(fieldUpdateResponse);

                    if (jsonObj.getBoolean("isSuccess")) {

                        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInPassword);

                        String baseTemplateName = defaultProperties.get("invoicebulkcreatetemplatecontractbase");
                        String testTemplateName = "C4328.xlsm";

                        List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, baseTemplateName, "Invoice", 2);

                        if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                            throw new SkipException("Couldn't get All Header Ids from Invoice Bulk Template [" + testTemplateName + "]");
                        }

                        int parentRecordId = Integer.parseInt(defaultProperties.get("invoicebulkcreatetemplatecontractbaseparentcontractid"));

                        verifyInvoiceAndLineItemFieldValidation(baseTemplateName, testTemplateName, allHeaderIds, "Invoice", "604",
                                "PO Number", "Characters limit exceeding 18", updatedFieldLabel, "invoices", 67,
                                61, parentRecordId, 1013, "Updating Field Label Error Message for PO Number Field.", csAssert);

                        //Reverting Error Message for PO Number Field.
                        adminHelperObj.loginWithClientAdminUser();
                        updatePayload = updatePayload.replace("clientFieldName\":\"" + updatedFieldLabel, "clientFieldName\":\"" +
                                originalFieldLabel);

                        fieldRenamingObj.hitFieldUpdate(updatePayload);
                    } else {
                        throw new SkipException("Couldn't Update Field Label for Field [Please enter less than 18 characters].");
                    }
                } else {
                    throw new SkipException("Couldn't Update Field Label for Field [Please enter less than 18 characters].");
                }
            } else {
                csAssert.assertTrue(false, "Field Renaming API findLabelsByGroupIdAndLanguageId Response for Group Id 467 and Language Id 1" +
                        " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C4328. " + e.getMessage());
        }

        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInPassword);
        csAssert.assertAll();
    }


    /*
    TC-C4017:  Verify Invoice & Line Item Creation and Failure if error in any field.
     */
    @Test
    public void testC4017() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4017: Verify Invoice & Line Item Creation and Failure if error in any field.");
            String baseTemplateName = defaultProperties.get("invoicewithlineitembulkcreatetemplatecontractbase");
            String testTemplateName = "C4017.xlsm";

            copyTemplateFile(baseTemplateName, testTemplateName);

            int parentRecordId = Integer.parseInt(defaultProperties.get("invoicewithlineitembulkcreatetemplatecontractbaseparentcontractid"));

            verifyC4017Part1(parentRecordId, csAssert);
            verifyC4017Part2(parentRecordId, csAssert);
            verifyC4017Part3(parentRecordId, csAssert);

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C4017. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void verifyC4017Part1(int parentRecordId, CustomAssert csAssert) {
        String additionalInfo = "C4017 Part 1. Both Invoice & Line Item should not be created if any error in Invoice Sheet.";

        try {
            logger.info("Validating Case {}", additionalInfo);
            String testTemplateName = "C4017-1.xlsm";
            copyTemplateFile("C4017.xlsm", testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Invoice", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Invoice with Line Item Bulk Template [" + testTemplateName + "]");
            }

            if (!allHeaderIds.contains("602")) {
                throw new SkipException("Couldn't find Title Field Column in Invoice with Line Item Bulk Template for case " + additionalInfo);
            }

            int fieldColumnNo = allHeaderIds.indexOf("602");

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Invoice", 6, fieldColumnNo, "");

            if (!updateValue) {
                throw new SkipException("Couldn't Update Title Value in Bulk Template for case. " + additionalInfo);
            }

            verifyInvoiceWithLineItemValidation("C4017-1.xlsm", "602", "sheetId: 6, messages: [Cell Value Cannot Be Empty]",
                    61, parentRecordId, additionalInfo, csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating case " + additionalInfo + ". " + e.getMessage());
        }
    }

    private void verifyC4017Part2(int parentRecordId, CustomAssert csAssert) {
        String additionalInfo = "C4017 Part 2. Both Invoice & Line Item should not be created if any error in Line Item Sheet.";

        try {
            logger.info("Validating Case {}", additionalInfo);
            String testTemplateName = "C4017-2.xlsm";
            copyTemplateFile("C4017.xlsm", testTemplateName);

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Invoice Line Item", 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Invoice with Line Item Bulk Template [" + testTemplateName + "]");
            }

            if (!allHeaderIds.contains("11047")) {
                throw new SkipException("Couldn't find Description Field Column in Invoice with Line Item Bulk Template for case " + additionalInfo);
            }

            int fieldColumnNo = allHeaderIds.indexOf("11047");

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Invoice Line Item", 6, fieldColumnNo, "");

            if (!updateValue) {
                throw new SkipException("Couldn't Update Description Value in Bulk Template for case. " + additionalInfo);
            }

            verifyInvoiceWithLineItemValidation("C4017-2.xlsm", "11047", "sheetId: 7, messages: [Cell Value Cannot Be Empty]",
                    61, parentRecordId, additionalInfo, csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating case " + additionalInfo + ". " + e.getMessage());
        }
    }

    private void verifyC4017Part3(int parentRecordId, CustomAssert csAssert) {
        String additionalInfo = "C4017 Part 3. Both Invoice & Line Item should not be created if error in both Sheets.";

        try {
            logger.info("Validating Case {}", additionalInfo);
            String testTemplateName = "C4017-3.xlsm";
            copyTemplateFile("C4017.xlsm", testTemplateName);

            List<String> allHeaderIdsOfInvoiceSheet = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Invoice", 2);

            if (allHeaderIdsOfInvoiceSheet == null || allHeaderIdsOfInvoiceSheet.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Invoice with Line Item Bulk Template [" + testTemplateName + "]");
            }

            if (!allHeaderIdsOfInvoiceSheet.contains("602")) {
                throw new SkipException("Couldn't find Title Field Column in Invoice with Line Item Bulk Template for case " + additionalInfo);
            }

            int fieldColumnNo = allHeaderIdsOfInvoiceSheet.indexOf("602");

            boolean updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Invoice", 6, fieldColumnNo, "");

            if (!updateValue) {
                throw new SkipException("Couldn't Update Title Value in Bulk Template for case. " + additionalInfo);
            }

            List<String> allHeaderIdsOfLineItemSheet = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, "Invoice Line Item", 2);

            if (allHeaderIdsOfLineItemSheet == null || allHeaderIdsOfLineItemSheet.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Invoice with Line Item Bulk Template [" + testTemplateName + "]");
            }

            if (!allHeaderIdsOfLineItemSheet.contains("11047")) {
                throw new SkipException("Couldn't find Description Field Column in Invoice with Line Item Bulk Template for case " + additionalInfo);
            }

            fieldColumnNo = allHeaderIdsOfLineItemSheet.indexOf("11047");

            updateValue = XLSUtils.updateColumnValue(templatePath, testTemplateName, "Invoice Line Item", 6, fieldColumnNo, "");

            if (!updateValue) {
                throw new SkipException("Couldn't Update Description Value in Bulk Template for case. " + additionalInfo);
            }

            verifyInvoiceWithLineItemValidation("C4017-3.xlsm", "11047", "sheetId: 6, messages: [Cell Value Cannot Be Empty]",
                    61, parentRecordId, additionalInfo, csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating case " + additionalInfo + ". " + e.getMessage());
        }
    }

    private void verifyInvoiceWithLineItemValidation(String testTemplateName, String headerId, String expectedErrorMessage, int parentEntityTypeId,
                                                     int parentRecordId, String additionalInfo, CustomAssert csAssert) {
        try {
            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, parentEntityTypeId, parentRecordId, 67,
                    1013);

            if (uploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further for case. " + additionalInfo);
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    int newlyCreatedInvoiceId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);
                    EntityOperationsHelper.deleteEntityRecord("invoices", newlyCreatedInvoiceId);
                } else {
                    logger.info("Validating Validation Message from DB.");
                    String errorMessages = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);

                    if (errorMessages == null) {
                        throw new SkipException("Couldn't get Error Messages from DB for case. " + additionalInfo);
                    }

                    if (!errorMessages.toLowerCase().contains(expectedErrorMessage.toLowerCase()) || !errorMessages.contains("fieldId: " + headerId)) {
                        csAssert.assertTrue(false, "Error Messages Validation Failed. Expected Message: [" + expectedErrorMessage + "] and " +
                                "Field Id: [" + headerId + "] but Actual Error Message: [" + errorMessages + "] for case. " + additionalInfo);
                    }
                }
            } else {
                csAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + uploadResponse + "] for case. " + additionalInfo);
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating " + additionalInfo + ". " + e.getMessage());
        }
    }


    /*
    TC-C3949: Verify Template Extension for Entities Invoice & Line Item.
     */
    @Test
    public void testC3949() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3949: Verify Template Extension for Entities Invoice and Invoice Line Item.");

            String baseTemplateName = defaultProperties.get("lineitembulkcreatetemplatebase1");

            //Verify Part 1. Line Item Template with Extension pdf.
            String testTemplateName = "InvalidFormat.pdf";
            copyTemplateFile(baseTemplateName, testTemplateName);
            int parentRecordId = Integer.parseInt(defaultProperties.get("lineitembulkcreatetemplatebase1parentinvoiceid"));

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, 67, parentRecordId, lineItemEntityTypeId,
                    1004);

            if (!uploadResponse.contains("File extension pdf not supported")) {
                csAssert.assertTrue(false, "Expected Response: [File extension pdf not supported] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);

            //Verify Part 3
            testTemplateName = "InvalidFormat.docx";
            copyTemplateFile(baseTemplateName, testTemplateName);

            uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, 67, parentRecordId, lineItemEntityTypeId,
                    1004);

            if (!uploadResponse.contains("File extension docx not supported")) {
                csAssert.assertTrue(false, "Expected Response: [File extension docx not supported] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);

            //Verify Part 4
            baseTemplateName = defaultProperties.get("invoicebulkcreatetemplatecontractbase");

            testTemplateName = "InvalidFormat.pdf";
            copyTemplateFile(baseTemplateName, testTemplateName);
            parentRecordId = Integer.parseInt(defaultProperties.get("invoicebulkcreatetemplatecontractbaseparentcontractid"));

            uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, 61, parentRecordId, invoiceEntityTypeId,
                    1013);

            if (!uploadResponse.contains("File extension pdf not supported")) {
                csAssert.assertTrue(false, "Expected Response: [File extension pdf not supported] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);

            //Verify Part 6
            testTemplateName = "InvalidFormat.docx";
            copyTemplateFile(baseTemplateName, testTemplateName);

            uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, 61, parentRecordId, invoiceEntityTypeId,
                    1013);

            if (!uploadResponse.contains("File extension docx not supported")) {
                csAssert.assertTrue(false, "Expected Response: [File extension docx not supported] and Actual Response: [" + uploadResponse + "]");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test C3949. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4042: Verify Errors for Uploading Template with Extra Sheet/Formula for Numeric Fields for Entities Invoice & Line Item.
    TC-C3900: Verify Pre-Processing Errors for Invoice & Line Item.
     */
    @Test
    public void testC4042() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4042: Verify Errors for Uploading Template with Extra Sheet and Formula in Numeric Fields for Entities Invoice & Line Item.");
            String baseTemplateName = defaultProperties.get("invoicewithlineitembulkcreatetemplatecontractbase");

            //Verify Part 1. Adding Extra Sheet in Invoice with Line Item Template.
            String testTemplateName = "C4042-1.xlsm";
            copyTemplateFile(baseTemplateName, testTemplateName);
            String sheetName = "Invoice";

            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, sheetName, 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Invoice with Line Item Bulk Template");
            }

            boolean updateValue = XLSUtils.createNewSheet(templatePath, testTemplateName, "Extra Sheet");

            int parentId = Integer.parseInt(defaultProperties.get("invoicewithlineitembulkcreatetemplatecontractbaseparentcontractid"));

            if (!updateValue) {
                throw new SkipException("Couldn't Add New Extra Sheet in Invoice with Line Item Bulk Template.");
            }

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentId, invoiceEntityTypeId,
                    1013);

            if (!uploadResponse.toLowerCase().contains("template is not correct")) {
                csAssert.assertTrue(false, "Upload Response Failed for Invoice with Line Item Bulk Template Part 1. " +
                        "Expected Response: [Template is not correct] and Actual Response: [" + uploadResponse + "] for Adding Extra Sheet.");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);

            //Verify Part 2. Adding Formula in Invoice with Line Item Template.
            testTemplateName = "C4042-2.xlsm";
            copyTemplateFile(baseTemplateName, testTemplateName);

            int paymentTermColumnNo = allHeaderIds.indexOf("11077");

            updateValue = XLSUtils.updateColumnValueAsFormula(templatePath, testTemplateName, "Invoice", 6, paymentTermColumnNo,
                    "SUM(B1:B3)");

            if (!updateValue) {
                throw new SkipException("Couldn't Add Formula in Invoice with Line Item Bulk Template.");
            }

            uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, parentId, invoiceEntityTypeId,
                    1013);

            if (!uploadResponse.toLowerCase().contains("uploaded contains excel formulas & functions")) {
                csAssert.assertTrue(false, "Upload Response Failed for Invoice with Line Item Bulk Template Part 2. " +
                        "Expected Response: [TUploaded Contains Excel Formulas & Functions] and Actual Response: [" + uploadResponse + "] for Adding Extra Sheet.");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);


            //Verify TC-C3900 Part 2. Uploading Invoice with Line Item Template to different Parent Contract.
            testTemplateName = "C3900-2.xlsm";
            copyTemplateFile(baseTemplateName, testTemplateName);

            String listDataResponse = ListDataHelper.getListDataResponse("contracts");

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    if (recordId == parentId) {
                        continue;
                    }

                    String showResponse = ShowHelper.getShowResponse(contractEntityTypeId, recordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForInvoiceEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "invoices");

                        if (hasBulkCreateOptionForInvoiceEntity == null) {
                            throw new SkipException("Couldn't find whether Record Id " + recordId +
                                    " of Entity Contract has Bulk Create Option to Create Invoice Entity or not.");
                        }

                        if (!hasBulkCreateOptionForInvoiceEntity) {
                            continue;
                        }

                        uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, contractEntityTypeId, recordId, invoiceEntityTypeId,
                                1013);

                        if (!uploadResponse.contains("Incorrect Template: Template Belongs To Different Parent Entity")) {
                            csAssert.assertTrue(false, "Bulk Template Upload Response Failed. " +
                                    "Expected Response: [Incorrect Template: Template Belongs To Different Parent Entity] and Actual Response: [" + uploadResponse + "]");
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity Contract is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity Contract is an Invalid JSON.");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);

            //Verify Part 3. Adding Extra Sheet in Line Item Template.
            baseTemplateName = defaultProperties.get("lineitembulkcreatetemplatebase1");
            testTemplateName = "C4042-3.xlsm";
            copyTemplateFile(baseTemplateName, testTemplateName);
            sheetName = "Invoice Line Item";

            allHeaderIds = XLSUtils.getExcelDataOfOneRow(templatePath, testTemplateName, sheetName, 2);

            if (allHeaderIds == null || allHeaderIds.isEmpty()) {
                throw new SkipException("Couldn't get All Header Ids from Line Item Bulk Template");
            }

            updateValue = XLSUtils.createNewSheet(templatePath, testTemplateName, "Extra Sheet");

            if (!updateValue) {
                throw new SkipException("Couldn't Add Extra Sheet in Line Item Bulk Template.");
            }

            parentId = Integer.parseInt(defaultProperties.get("lineitembulkcreatetemplatebase1parentinvoiceid"));

            uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, invoiceEntityTypeId, parentId, lineItemEntityTypeId,
                    1004);

            if (!uploadResponse.toLowerCase().contains("template is not correct")) {
                csAssert.assertTrue(false, "Upload Response Failed for Line Item Bulk Template Part 3. " +
                        "Expected Response: [Template is not correct] and Actual Response: [" + uploadResponse + "] for Adding Extra Sheet.");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);

            //Verify Part 4. Adding Formula in Line Item Template.
            testTemplateName = "C4042-4.xlsm";
            copyTemplateFile(baseTemplateName, testTemplateName);

            int lineItemNumberColumnNo = allHeaderIds.indexOf("11042");

            updateValue = XLSUtils.updateColumnValueAsFormula(templatePath, testTemplateName, "Invoice Line Item", 6, lineItemNumberColumnNo,
                    "SUM(B1:B3)");

            if (!updateValue) {
                throw new SkipException("Couldn't Add Formula in Line Item Bulk Template.");
            }

            uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, invoiceEntityTypeId, parentId, lineItemEntityTypeId,
                    1004);

            if (!uploadResponse.toLowerCase().contains("uploaded contains excel formulas & functions")) {
                csAssert.assertTrue(false, "Upload Response Failed for Line Item Bulk Template Part 4. " +
                        "Expected Response: [TUploaded Contains Excel Formulas & Functions] and Actual Response: [" + uploadResponse + "] for Adding Extra Sheet.");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);

            //Verify TC-C3900 Part 2. Uploading Line Item Template to different Parent Invoice.
            testTemplateName = "C3900-2.xlsm";
            copyTemplateFile(baseTemplateName, testTemplateName);

            listDataResponse = ListDataHelper.getListDataResponse("invoices");

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    if (recordId == parentId) {
                        continue;
                    }

                    String showResponse = ShowHelper.getShowResponse(invoiceEntityTypeId, recordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForLineItemEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "invoice line item");

                        if (hasBulkCreateOptionForLineItemEntity == null) {
                            throw new SkipException("Couldn't find whether Record Id " + recordId +
                                    " of Entity Invoice has Bulk Create Option to Create Invoice Line Item Entity or not.");
                        }

                        if (!hasBulkCreateOptionForLineItemEntity) {
                            continue;
                        }

                        uploadResponse = BulkTemplate.uploadBulkCreateTemplate(templatePath, testTemplateName, invoiceEntityTypeId, recordId, lineItemEntityTypeId,
                                1004);

                        if (!uploadResponse.contains("Incorrect Template: Template Belongs To Different Parent Entity")) {
                            csAssert.assertTrue(false, "Bulk Template Upload Response Failed. " +
                                    "Expected Response: [Incorrect Template: Template Belongs To Different Parent Entity] and Actual Response: [" + uploadResponse + "]");
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity Contract is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity Contract is an Invalid JSON.");
            }

            FileUtils.deleteFile(templatePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4042. " + e.getMessage());
        }
        csAssert.assertAll();
    }
}