package com.sirionlabs.test.bulkUpdate;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.clientAdmin.field.Provisioning;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.FieldProvisioning;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.*;
import java.util.regex.Pattern;

public class TestBulkUpdateEndUserDownloadTemplate {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkUpdateEndUserDownloadTemplate.class);

    private List<String> entitiesToTest;
    private String outputFilePath = "src/test";
    private String endUserName;
    private String endUserPassword;

    private Map<String, Integer> entityRecordIdMap = new HashMap<>();

    private FieldProvisioning fieldProvisioningObj = new FieldProvisioning();
    private BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();
    private DefaultUserListMetadataHelper defaultListHelperObj = new DefaultUserListMetadataHelper();
    private Provisioning provisioningObj = new Provisioning();
    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();


    @BeforeClass
    public void beforeClass() {
        entitiesToTest = getEntitiesToTest();
        endUserName = Check.lastLoggedInUserName;
        endUserPassword = Check.lastLoggedInUserPassword;
    }


    @AfterClass
    public void afterClass() {
        new Check().hitCheck(endUserName, endUserPassword);
    }

    public List<String> getEntitiesToTest() {
        String[] entitiesArr = {"service levels", "child service levels", "disputes", "child obligations", "obligations", "consumptions"};

        return Arrays.asList(entitiesArr);
    }


    /*
    TC-C3419: Validate Downloaded Bulk Update Template Name and Extension
    TC-C3385: Validate Bulk Update template download.
    TC-C3405: Validate Sheets in Downloaded Bulk Update Template.
     */
    @Test
    public void testC3385() {
        CustomAssert csAssert = new CustomAssert();

        for (String entityName : entitiesToTest) {
            logger.info("Verifying Bulk Update Template Download for Entity {}", entityName);

            try {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                String defaultUserListMetadataResponse = defaultListHelperObj.getDefaultUserListMetadataResponse(entityName);

                if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                    JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);
                    boolean bulkUpdateEnabled = jsonObj.getBoolean("bulkUpdate");

                    if (bulkUpdateEnabled) {
                        String listDataResponse = ListDataHelper.getListDataResponse(entityName);

                        if (ParseJsonResponse.validJsonResponse(listDataResponse)) {

                            jsonObj = new JSONObject(listDataResponse);
                            JSONArray jsonArr = jsonObj.getJSONArray("data");
                            if (jsonArr.length() > 0) {
                                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                                String idValue = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");

                                int recordId = ListDataHelper.getRecordIdFromValue(idValue);
                                entityRecordIdMap.put(entityName, recordId);

                                String fileName = "C3385-" + entityName + ".xlsm";
                                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                                validateC3419(entityName, entityTypeId, templateId, String.valueOf(recordId), csAssert);

                                boolean templateDownloaded = BulkTemplate.downloadBulkUpdateTemplate(outputFilePath, fileName, templateId, entityTypeId,
                                        String.valueOf(recordId));

                                csAssert.assertTrue(templateDownloaded, "Couldn't download Bulk Upload Template for Entity " + entityName + " and Record Id " +
                                        recordId);

                                validateC3405(entityName, recordId, fileName, csAssert);

                                FileUtils.deleteFile(outputFilePath + "/" + fileName);
                            } else {
                                throw new SkipException("No data found in ListData API Response for Entity " + entityName);
                            }
                        } else {
                            csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
                        }
                    } else {
                        throw new SkipException("Bulk Update Option Disabled in DefaultUserListMetadata API for Entity " + entityName);
                    }
                } else {
                    csAssert.assertTrue(false, "DefaultUserList MetaData API Response for Entity " + entityName + " is an Invalid JSON.");
                }
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating Bulk Update Template Download for Entity " + entityName + ". " + e.getMessage());
            }
        }
        csAssert.assertAll();
    }

    private void validateC3405(String entityName, int recordId, String templateName, CustomAssert csAssert) {
        try {
            logger.info("Validating Sheets in Downloaded Bulk Update Template for Entity {}", entityName);
            XLSUtils xlsUtilObj = new XLSUtils(outputFilePath, templateName);

            List<String> allSheetNames = xlsUtilObj.getSheetNames();

            if (allSheetNames.isEmpty()) {
                throw new SkipException("Couldn't get All Sheet Names from Template for Entity " + entityName + " and Record Id " + recordId);
            }

            csAssert.assertTrue(allSheetNames.contains("Instructions"), "Instructions Sheet not found in Bulk Update Template for Entity " + entityName +
                    " and Record Id " + recordId);
            csAssert.assertTrue(allSheetNames.contains("Information"), "Information Sheet not found in Bulk Update Template for Entity " + entityName +
                    " and Record Id " + recordId);

            String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);
            csAssert.assertTrue(allSheetNames.contains(dataSheetName.trim()), dataSheetName + " Sheet not found in Bulk Update Template for Entity " + entityName +
                    " and Record Id " + recordId);

            csAssert.assertTrue(allSheetNames.contains("Master Data"), "Master Data Sheet not found in Bulk Update Template for Entity " + entityName +
                    " and Record Id " + recordId);
            csAssert.assertTrue(allSheetNames.contains("Stakeholders"), "Stakeholders Sheet not found in Bulk Update Template for Entity " + entityName +
                    " and Record Id " + recordId);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C405 i.e. Sheet in Downloaded Bulk Update Template for Entity " + entityName +
                    " and Record Id " + recordId + ". " + e.getMessage());
        }
    }

    private void validateC3419(String entityName, int entityTypeId, int templateId, String entityIds, CustomAssert csAssert) {
        logger.info("Verifying Downloaded Bulk Update Template Name and Extension for Entity {}", entityName);

        try {
            Download bulkDownloadObj = new Download();
            HttpResponse httpResponse = bulkDownloadObj.hitDownload(outputFilePath + "/C3419.xlsm", templateId, entityTypeId, entityIds);
            Header[] allHeaders = httpResponse.getAllHeaders();

            boolean fileNameFound = false;

            for (Header oneHeader : allHeaders) {
                if (oneHeader.toString().contains("Content-Disposition:")) {
                    fileNameFound = true;
                    String[] valueArr = oneHeader.toString().split(Pattern.quote("="));
                    String actualFileName = valueArr[valueArr.length - 1].replaceAll("\"", "");

                    String entityTypeName = getBulkUpdateDownloadTemplateExpectedEntityName(entityName);

                    String expectedFileName = entityTypeName + "-" + "Default" + ".xlsm";

                    if (!actualFileName.equalsIgnoreCase(expectedFileName)) {
                        csAssert.assertTrue(false, "Entity " + entityName + " Expected File Name: [" + expectedFileName +
                                "] and Actual File Name: [" + actualFileName + "]");
                    }

                    break;
                }
            }

            csAssert.assertTrue(fileNameFound, "Couldn't find File Name in Download API Response for Entity " + entityName);

            FileUtils.deleteFile(outputFilePath + "/C3419.xlsm");

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Downloaded Bulk Update Template Name and Extension for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private String getBulkUpdateDownloadTemplateExpectedEntityName(String entityName) {
        String[] temp = ConfigureEnvironment.getEnvironmentProperty("host").split(Pattern.quote("."));
        String clientAliasName = temp[0];

        String expectedEntityName = "";

        switch (entityName) {
            case "contracts":
                expectedEntityName = "BulkUpdate-" + clientAliasName + "-Contract";
                break;

            case "obligations":
                expectedEntityName = "BulkUpdate-" + clientAliasName + "-Obligations";
                break;

            case "service levels":
                expectedEntityName = "BulkUpdate-" + clientAliasName + "-Sla";
                break;

            case "child obligations":
                expectedEntityName = "BulkUpdate-" + clientAliasName + "-Child Obligations";
                break;

            case "child service levels":
                expectedEntityName = "BulkUpdate-" + clientAliasName + "-Child Sla";
                break;

            case "disputes":
                expectedEntityName = "BulkUpdate-" + clientAliasName + "-Dispute Management";
                break;

            case "consumptions":
                expectedEntityName = "BulkUpdate-" + clientAliasName + "-Consumption";
                break;
        }

        return expectedEntityName;
    }


    /*
    TC-C3482: Verify Order of Read and Update Fields in Bulk Update Template.
     */
    @Test(dependsOnMethods = "testC3385")
    public void testC3482() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3482.");

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Order of Read and Update Fields in Bulk Update Template for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int recordId = entityRecordIdMap.get(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                String templateFileName = "C3482-" + entityName + ".xlsm";

                boolean templateDownloaded = BulkTemplate.downloadBulkUpdateTemplate(outputFilePath, templateFileName, templateId, entityTypeId,
                        String.valueOf(recordId));

                if (templateDownloaded) {
                    String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);
                    String fieldProvisioningResponse = fieldProvisioningObj.getFieldProvisioningResponse(entityTypeId);

                    List<Map<String, String>> allReadOnlyFields = fieldProvisioningObj.getAllBulkUpdateReadOnlyFields(fieldProvisioningResponse);
                    List<Map<String, String>> allEnabledFields = fieldProvisioningObj.getAllBulkUpdateEnabledFields(fieldProvisioningResponse);

                    List<Map<String, String>> allExpectedFields = new ArrayList<>();
                    allExpectedFields.addAll(allReadOnlyFields);
                    allExpectedFields.addAll(allEnabledFields);

                    List<Map<String, String>> allFieldsInBulkUpdateTemplate = bulkHelperObj.getAllFieldsInBulkUpdateTemplate(outputFilePath, templateFileName,
                            dataSheetName);

                    if (allFieldsInBulkUpdateTemplate == null || allFieldsInBulkUpdateTemplate.isEmpty()) {
                        throw new SkipException("Couldn't get All Fields in Bulk Update Template for Entity " + entityName);
                    }

                    int totalNoOfColumnsInTemplate = allFieldsInBulkUpdateTemplate.size();

                    //Remove SL Id Field from validation.
                    allFieldsInBulkUpdateTemplate.remove(0);

                    //Remove ID Field only if it is not present in Field Provisioning.
                    boolean shortCodeIdPresent = false;
                    for (Map<String, String> readOnlyField : allReadOnlyFields) {
                        String apiName = readOnlyField.get("apiName");

                        if (apiName.equalsIgnoreCase("shortCodeId")) {
                            shortCodeIdPresent = true;
                            break;
                        }
                    }

                    if (!shortCodeIdPresent) {
                        allFieldsInBulkUpdateTemplate.remove(0);
                    }

                    //Remove Process Field from validation.
                    allFieldsInBulkUpdateTemplate.remove(allFieldsInBulkUpdateTemplate.size() - 1);

                    if (allExpectedFields.size() != allFieldsInBulkUpdateTemplate.size()) {
                        csAssert.assertTrue(false, "Entity " + entityName + " and Record Id " + recordId +
                                ". Bulk Update Template Fields Sequence Validation failed. Expected No of Fields in Excel: " +
                                allExpectedFields.size() + " and Actual No of Fields in Excel: " + allFieldsInBulkUpdateTemplate.size());

                        if (allExpectedFields.size() > allFieldsInBulkUpdateTemplate.size()) {
                            for (Map<String, String> expectedFieldMap : allExpectedFields) {
                                String fieldName = expectedFieldMap.get("fieldName");
                                boolean fieldFound = false;

                                for (Map<String, String> templateField : allFieldsInBulkUpdateTemplate) {
                                    String fieldLabel = templateField.get("label");

                                    if (fieldLabel.equalsIgnoreCase(fieldName)) {
                                        fieldFound = true;
                                        break;
                                    }
                                }

                                if (!fieldFound) {
                                    csAssert.assertTrue(false, "Expected Field " + fieldName + " not found in Bulk Update Template for Entity " +
                                            entityName);
                                }
                            }
                        } else {
                            for (Map<String, String> templateMap : allFieldsInBulkUpdateTemplate) {
                                String fieldLabel = templateMap.get("label");
                                boolean fieldFound = false;

                                for (Map<String, String> expectedField : allExpectedFields) {
                                    String fieldName = expectedField.get("fieldName");

                                    if (fieldLabel.equalsIgnoreCase(fieldName)) {
                                        fieldFound = true;
                                        break;
                                    }
                                }

                                if (!fieldFound) {
                                    csAssert.assertTrue(false, "Field " + fieldLabel +
                                            " found in Bulk Update Template whereas it is not found in Field Provisioning for Entity " + entityName);
                                }
                            }
                        }
                    }

                    validateC3529(entityName, templateFileName, dataSheetName, totalNoOfColumnsInTemplate, csAssert);

                    validateC4246(entityName, templateFileName, dataSheetName, allReadOnlyFields, csAssert);

                    validateC3543(entityName, templateFileName, csAssert);

                    validateC3383(entityName, entityTypeId, templateFileName, dataSheetName, csAssert);

                    FileUtils.deleteFile(outputFilePath + "/" + templateFileName);
                } else {
                    csAssert.assertTrue(false, "Couldn't download Bulk Upload Template for Entity " + entityName + " and Record Id " +
                            recordId);
                }
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C3482 for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }

    //TC-C3529: Verify last column of Bulk Update Template.
    private void validateC3529(String entityName, String templateFileName, String dataSheetName, int totalNoOfColumnsInTemplate, CustomAssert csAssert) {
        try {
            logger.info("Validating TC-C3529: Verify Last Column of Bulk Update Template for Entity {}", entityName);
            List<String> allDataOfLastColumn = XLSUtils.getOneColumnDataFromMultipleRows(outputFilePath, templateFileName, dataSheetName,
                    totalNoOfColumnsInTemplate - 1, 0, 7);

            if (allDataOfLastColumn.get(1).equalsIgnoreCase("100000002")) {
                csAssert.assertTrue(allDataOfLastColumn.get(6).equalsIgnoreCase("yes"),
                        "Default Value of Process is not Yes in Bulk Update Template for Entity " + entityName);
            } else {
                csAssert.assertTrue(false, "Last Column is not Process in Bulk Update Template for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C3529 for Entity " + entityName + ". " + e.getMessage());
        }
    }


    /*
    TC-C4246: Verify Read only fields - message.
    TC-C7819: Verify that Drop-down label is not present for Read Only Fields.
     */
    private void validateC4246(String entityName, String templateFileName, String dataSheetName, List<Map<String, String>> allReadOnlyFields, CustomAssert csAssert) {
        try {
            logger.info("Starting Tests TC-C4246: Verify Read only fields - messages and " +
                    "TC-C7819: Verify that Drop-down label is not present for Read Only Fields for Entity {}", entityName);

            List<String> allFieldIds = XLSUtils.getExcelDataOfOneRow(outputFilePath, templateFileName, dataSheetName, 2);
            List<String> allMessages = XLSUtils.getExcelDataOfOneRow(outputFilePath, templateFileName, dataSheetName, 5);
            List<String> allFieldTypeLabels = XLSUtils.getExcelDataOfOneRow(outputFilePath, templateFileName, dataSheetName, 6);

            for (Map<String, String> readOnlyFieldMap : allReadOnlyFields) {
                String fieldName = readOnlyFieldMap.get("fieldName");
                String fieldId = readOnlyFieldMap.get("id");

                if (allFieldIds.contains(fieldId)) {
                    int columnNo = allFieldIds.indexOf(fieldId);
                    String message = allMessages.get(columnNo);

                    if (!message.equalsIgnoreCase("Only Informational, Identifies The Entity To Be Updated. Please Don't Edit.")) {
                        csAssert.assertTrue(false, "Read Only Message validation failed for Field " + fieldName + " of Entity " + entityName);
                    }

                    //Validate C7819: No drop down label for Read Only field.
                    String fieldTypeLabel = allFieldTypeLabels.get(columnNo);

                    csAssert.assertTrue(!fieldTypeLabel.contains("Dropdown"), "Dropdown Label present for Read Only Field " + fieldName +
                            " in Bulk Update Template for Entity " + entityName);
                } else {
                    csAssert.assertTrue(false, "Field " + fieldName + " having Id " + fieldId +
                            " not found in Bulk Update Template for Entity " + entityName);
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C4246 for Entity " + entityName + "." + e.getMessage());
        }
    }

    /*
    TC-C3543: Verify the master data of Stakeholder Field in Bulk Update Template.
    TC-C46378: Verify Stakeholders Sheet in Bulk Update Template.
     */
    private void validateC3543(String entityName, String templateFileName, CustomAssert csAssert) {
        try {
            logger.info("Starting Test TC-C3543: Verify the master data of Stakeholder Field in Bulk Update Template for Entity {}", entityName);
            List<String> allHeaders = XLSUtils.getHeaders(outputFilePath, templateFileName, "Stakeholders");

            AdminHelper helperObj = new AdminHelper();

            int noOfRows = XLSUtils.getNoOfRows(outputFilePath, templateFileName, "Stakeholders").intValue();

            //Validate all Client Users
            List<String> allExpectedUsers = helperObj.getAllClientTypeUserNames();
            verifyUsersInStakeholdersSheet(entityName, "Client", templateFileName, allExpectedUsers, allHeaders, noOfRows, csAssert);

            allExpectedUsers = helperObj.getAllSupplierTypeUserNames();
            verifyUsersInStakeholdersSheet(entityName, "Supplier", templateFileName, allExpectedUsers, allHeaders, noOfRows, csAssert);

            allExpectedUsers = helperObj.getAllSirionlabsTypeUserNames();
            verifyUsersInStakeholdersSheet(entityName, "SirionLabs", templateFileName, allExpectedUsers, allHeaders, noOfRows, csAssert);

            allExpectedUsers = helperObj.getAllNonUserTypeUserNames();
            verifyUsersInStakeholdersSheet(entityName, "Non-User", templateFileName, allExpectedUsers, allHeaders, noOfRows, csAssert);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C3543 for Entity " + entityName + ". " + e.getMessage());
        }
    }

    private void verifyUsersInStakeholdersSheet(String entityName, String userType, String templateFileName, List<String> allExpectedUsers, List<String> allHeaders,
                                                int noOfRows, CustomAssert csAssert) {
        try {
            if (allExpectedUsers == null) {
                throw new SkipException("Couldn't get All Users of Type " + userType + "from Client Admin.");
            }

            if (allHeaders.contains(userType)) {
                List<String> allUsersInTemplate = XLSUtils.getOneColumnDataFromMultipleRows(outputFilePath, templateFileName, "Stakeholders",
                        allHeaders.indexOf(userType), 4, noOfRows);

                if (allExpectedUsers.size() != allUsersInTemplate.size()) {
                    csAssert.assertTrue(false, "Stakeholders Validation failed for User Type " + userType + " and Entity " + entityName +
                            ". No of Expected Users: " + allExpectedUsers.size() + " and No of Users in Template: " + allUsersInTemplate.size());
                }

                for (String expectedUser : allUsersInTemplate) {
                    if (!allUsersInTemplate.contains(expectedUser)) {
                        csAssert.assertTrue(false, "Stakeholders Validation failed for User Type: " + userType + " and Entity " + entityName +
                                ". User " + expectedUser + " not found in Template.");
                    }
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Users in Stakeholders Sheet for User Type " + userType + " and Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateC3383(String entityName, int entityTypeId, String templateFileName, String dataSheetName, CustomAssert csAssert) {
        try {
            logger.info("Starting Test TC-C3383: Verify Downloaded Template contains fields as per User Level Field Access for Entity {}", entityName);
            List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(outputFilePath, templateFileName, dataSheetName, 2);
            adminHelperObj.loginWithClientAdminUser();

            logger.info("Hitting Provisioning API for Client Type User and Entity {}", entityName);
            String provisioningResponse = provisioningObj.hitFieldProvisioning(2, entityTypeId);

            if (ParseJsonResponse.validJsonResponse(provisioningResponse)) {
                List<Map<String, String>> allHiddenFields = provisioningObj.getAllHiddenFields(provisioningResponse);

                if (allHiddenFields == null) {
                    csAssert.assertTrue(false, "Couldn't get All Hidden Fields at User Level Field Access for Entity " + entityName);
                    return;
                }

                for (Map<String, String> hiddenFieldMap : allHiddenFields) {
                    String entityFieldId = hiddenFieldMap.get("entityFieldId");
                    String fieldName = hiddenFieldMap.get("name");

                    if (allHeaderIds.contains(entityFieldId)) {
                        csAssert.assertTrue(false, "Field [" + fieldName + "] having Header Id " + entityFieldId +
                                " found in Bulk Update Template whereas it was not expected for Entity " + entityName);
                    }
                }
            } else {
                csAssert.assertTrue(false, "Provisioning Response for Client Type User and Entity " + entityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C3383 for Entity " + entityName + "." + e.getMessage());
        } finally {
            checkObj.hitCheck(endUserName, endUserPassword);
        }
    }


    /*
    TC-C40386: Verify Field Renaming in Downloaded Bulk Update Template.
     */
    @Test(dependsOnMethods = "testC3385", enabled = false)
    public void testC40386() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C40386.");

        FieldRenaming fieldRenamingObj = new FieldRenaming();
        UpdateAccount updateAccountObj = new UpdateAccount();

        int currentLanguageId = updateAccountObj.getCurrentLanguageIdForUser(endUserName, 1002);

        if (currentLanguageId == -1) {
            throw new SkipException("Couldn't get Current Language Id for User " + endUserName);
        }

        //Update Language Id for User.
        if (!updateAccountObj.updateUserLanguage(endUserName, 1002, 1000)) {
            throw new SkipException("Couldn't Change Language for User " + endUserName + " to Russian");
        }

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Field Renaming in Downloaded Bulk Update Template for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                int recordId = entityRecordIdMap.get(entityName);
                int templateId = BulkTemplate.getBulkUpdateTemplateIdForEntity(entityName);

                adminHelperObj.loginWithClientAdminUser();

                String fieldProvisioningResponse = fieldProvisioningObj.hitFieldProvisioning(entityTypeId);

                List<Map<String, String>> allBulkUpdateAvailableFields = fieldProvisioningObj.getAllBulkUpdateAvailableFields(fieldProvisioningResponse);

                if (allBulkUpdateAvailableFields == null || allBulkUpdateAvailableFields.isEmpty()) {
                    csAssert.assertTrue(false, "Couldn't Get All Bulk Update Available Fields from Field Provisioning Response for Entity " + entityName);
                    continue;
                }

                //Hit Field Labels API for Russian Language.
                int groupId = fieldRenamingObj.getFieldRenamingGroupIdForEntity(entityName);

                if (groupId == -1) {
                    throw new SkipException("Couldn't get Group Id for Entity " + entityName);
                }

                String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1000, groupId);
                String payloadForFieldRenamingUpdate = fieldRenamingResponse;

                List<Map<String, String>> templateFieldsList = new ArrayList<>();

                for (Map<String, String> bulkUpdateAvailableFieldMap : allBulkUpdateAvailableFields) {
                    Map<String, String> templateFieldMap = new HashMap<>();

                    int fieldId = Integer.parseInt(bulkUpdateAvailableFieldMap.get("id"));
                    Map<String, String> fieldProperties = fieldProvisioningObj.getFieldPropertiesFromFieldId(fieldProvisioningResponse, fieldId);

                    String apiName = fieldProperties.get("apiName");

                    if (apiName.startsWith("rg_")) {
                        continue;
                    }

                    String aliasLabelId = fieldProvisioningObj.getFieldAliasLabelIdFromApiName(apiName, entityTypeId);

                    if (aliasLabelId == null) {
                        continue;
                    }

                    String clientFieldNameAppendValue = " Russian Привет";

                    String clientFieldName = fieldRenamingObj.getMetadataClientFieldNameFromId(fieldRenamingResponse, Integer.parseInt(aliasLabelId));

                    if (clientFieldName == null) {
                        continue;
                    }

                    String newClientFieldName = clientFieldName;

                    if (!clientFieldName.endsWith(clientFieldNameAppendValue)) {
                        newClientFieldName = clientFieldName + " Russian Привет";

                        payloadForFieldRenamingUpdate = payloadForFieldRenamingUpdate.replace("clientFieldName\":\"" + clientFieldName + "\"",
                                "clientFieldName\":\"" + newClientFieldName + "\"");
                    }

                    templateFieldMap.put("id", String.valueOf(fieldId));
                    templateFieldMap.put("value", newClientFieldName);

                    templateFieldsList.add(templateFieldMap);
                }

                if (templateFieldsList.size() == 0) {
                    throw new SkipException("No Field Selected for Renaming for Entity " + entityName);
                }

                logger.info("Total No of Fields Selected for Renaming: {} for Entity {}", templateFieldsList.size(), entityName);
                String fieldUpdateResponse = fieldRenamingObj.hitFieldUpdate(payloadForFieldRenamingUpdate);

                JSONObject jsonObj = new JSONObject(fieldUpdateResponse);
                if (!jsonObj.getBoolean("isSuccess")) {
                    throw new SkipException("Couldn't update Field Labels for Entity " + entityName);
                }

                //Logging with End User
                new Check().hitCheck(endUserName, endUserPassword);

                String templateFileName = "C40386-" + entityName + ".xlsm";

                boolean templateDownloaded = BulkTemplate.downloadBulkUpdateTemplate(outputFilePath, templateFileName, templateId, entityTypeId,
                        String.valueOf(recordId));

                if (templateDownloaded) {
                    String dataSheetName = BulkTemplate.getBulkUpdateDataSheetForEntity(entityName);

                    List<String> allHeaders = XLSUtils.getHeaders(outputFilePath, templateFileName, dataSheetName);
                    List<String> allHeaderIds = XLSUtils.getExcelDataOfOneRow(outputFilePath, templateFileName, dataSheetName, 2);

                    for (Map<String, String> fieldMap : templateFieldsList) {
                        String fieldId = fieldMap.get("id");
                        String expectedValue = fieldMap.get("value");

                        if (allHeaderIds.contains(fieldId)) {
                            int index = allHeaderIds.indexOf(fieldId);
                            String actualValue = allHeaders.get(index);

                            if (!actualValue.toLowerCase().contains(expectedValue.toLowerCase())) {
                                csAssert.assertTrue(false, "Expected Value: [" + expectedValue + "] and Actual Value: [" + actualValue +
                                        "] of Field having Id " + fieldId + " for Entity " + entityName);
                            }
                        } else {
                            csAssert.assertTrue(false, "Bulk Update Template doesn't contain Field having Id " + fieldId + ", Expected Value: [" +
                                    expectedValue + "] for Entity " + entityName);
                        }
                    }

                    FileUtils.deleteFile(outputFilePath + "/" + templateFileName);
                } else {
                    csAssert.assertTrue(false, "Couldn't download Bulk Upload Template for Entity " + entityName + " and Record Id " +
                            recordId);
                }

                adminHelperObj.loginWithClientAdminUser();

                //Reverting Field Renaming.
                fieldRenamingObj.hitFieldUpdate(fieldRenamingResponse);
            } catch (SkipException e) {
                logger.error(e.getMessage());
                updateAccountObj.updateUserLanguage(endUserName, 1002, currentLanguageId);
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating TC-C40386 for Entity " + entityName + ". " + e.getMessage());
            }

            new Check().hitCheck(endUserName, endUserPassword);
        }

        //Revert Language for User.
        updateAccountObj.updateUserLanguage(endUserName, 1002, currentLanguageId);

        csAssert.assertAll();
    }
}