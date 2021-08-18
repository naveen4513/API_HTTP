package com.sirionlabs.test.bulkCreate;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.clientAdmin.field.Provisioning;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.FieldProvisioning;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.GetEntityId;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.*;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class TestBulkCreateEndUserDownloadTemplate {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkCreateEndUserDownloadTemplate.class);
    private String configFilePath;
    private String configFileName;
    private String templatePath = "src/test/output";
    private String templateName = "Template.xlsm";

    private FieldProvisioning fieldProvisioningObj = new FieldProvisioning();
    private BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();
    private Provisioning provisioningObj = new Provisioning();
    private AdminHelper adminHelperObj = new AdminHelper();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateEndUserDownloadTemplateConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateEndUserDownloadTemplateConfigFileName");

        //If user language is not English and then set it to English.
        UpdateAccount updateAccountObj = new UpdateAccount();
        String userLogin = ConfigureEnvironment.getEnvironmentProperty("j_username");
        int currentLanguageId = updateAccountObj.getCurrentLanguageIdForUser(userLogin, 1002);

        if (currentLanguageId != 1) {
            updateAccountObj.updateUserLanguage(userLogin, 1002, 1);
        }
    }

    @AfterClass
    public void afterClass() {
        FileUtils.deleteFile(templatePath + "/" + templateName);
    }


    @Test
    public void testBulkCreateTemplateColumns() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Bulk Create Template Columns.");
            String parentEntityName = "contracts";

            logger.info("Verifying Bulk Create Template of Service Data from Parent Entity {}", parentEntityName);
            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForServiceDataEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "service data");

                        if (hasBulkCreateOptionForServiceDataEntity == null) {
                            csAssert.assertTrue(false, "Couldn't find whether Record Id " + recordId + " of Entity " + parentEntityName +
                                    " has Bulk Create Option to Create Service Data Entity or not.");
                            break;
                        }

                        if (!hasBulkCreateOptionForServiceDataEntity) {
                            continue;
                        }

                        String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(showResponse, "service data");

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Service Data Entity from Record Id " +
                                    recordId + " of Entity " + parentEntityName);
                            break;
                        }

                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity " + parentEntityName +
                                " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Bulk Create Template Columns. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    /*
    TC-C3393: Verify the Downloaded Bulk Create Template File Name
    TC-C3233: Verify Bulk Create Feature Access
    TC-C3388: Verify that end user is able to download Bulk Create Action Template from Contract & Supplier show page.
     */
    @Test
    public void verifyDownloadedTemplateFileNameC3393() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3393: Verify the Downloaded Bulk Create Template File Name.");
            String[] entitiesArr = {"suppliers", "contracts"};

            for (String entityName : entitiesArr) {
                String listDataResponse = ListDataHelper.getListDataResponse(entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    JSONObject jsonObj = new JSONObject(listDataResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                        int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                        String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            Boolean hasBulkCreateOptionForActionEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "actions");

                            if (hasBulkCreateOptionForActionEntity == null) {
                                csAssert.assertTrue(false, "Couldn't find whether Record Id " + recordId + " of Entity " + entityName +
                                        " has Bulk Create Option to Create Actions Entity or not.");
                                break;
                            }

                            if (!hasBulkCreateOptionForActionEntity) {
                                continue;
                            }

                            String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(showResponse, "actions");

                            if (downloadAPIPath == null) {
                                csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Actions Entity from Record Id " +
                                        recordId + " of Entity " + entityName);
                                break;
                            }

                            logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                            Download bulkDownloadObj = new Download();
                            HttpResponse httpResponse = bulkDownloadObj.hitDownload(downloadAPIPath);
                            Header[] allHeaders = httpResponse.getAllHeaders();

                            boolean fileNameFound = false;

                            for (Header oneHeader : allHeaders) {
                                if (oneHeader.toString().contains("Content-Disposition:")) {
                                    fileNameFound = true;
                                    String[] valueArr = oneHeader.toString().split(Pattern.quote("="));
                                    String actualFileName = valueArr[valueArr.length - 1].replaceAll("\"", "");

                                    String entityTypeName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entity type mapping",
                                            "actions");

                                    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    Date date = new Date();
                                    String currentDate = dateFormat.format(date);
                                    String[] temp = ConfigureEnvironment.getEnvironmentProperty("host").split(Pattern.quote("."));
                                    String clientAliasName = temp[0];
                                    //String expectedFileName = clientAliasName + "-" + entityTypeName + "-" + currentDate + ".xlsm";
                                    String expectedFileName = entityTypeName + "-" + currentDate + ".xlsm";

                                    if (!actualFileName.equalsIgnoreCase(expectedFileName)) {
                                        csAssert.assertTrue(false, "Entity " + entityName + " Expected File Name: [" + expectedFileName +
                                                "] and Actual File Name: [" + actualFileName + "]");
                                    }

                                    break;
                                }
                            }

                            csAssert.assertTrue(fileNameFound, "Couldn't find File Name in Download API Response for Entity " + entityName);
                        } else {
                            csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity " + entityName + " is an Invalid JSON.");
                        }

                        break;
                    }
                } else {
                    csAssert.assertTrue(false, "List Data API Response for Entity " + entityName + " is an Invalid JSON.");
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Downloaded Bulk Create Template File Name TC-C3393. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3397: Verify first three columns of the Entity Action Bulk Create Template
    TC-C3396: Verify the sequence of fields in the template should be correct
    TC-C3408: Verify that Metadata in Action Bulk Create Template should be as per the field provisioning.
    TC-C3413: Verify that first row of the data sheet should contain the metadata fields
     */
    @Test
    public void testActionsBulkCreateTemplateColumns() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: Verify Action Bulk Create Template Columns.");
            String[] parentEntitiesArr = {"suppliers", "contracts"};

            int actionsEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");
            List<Map<String, String>> allBulkCreateFields = fieldProvisioningObj.getAllBulkCreateFields(actionsEntityTypeId);

            if (allBulkCreateFields == null || allBulkCreateFields.isEmpty()) {
                throw new SkipException("Couldn't get All Bulk Create Fields for Actions Entity");
            }

            List<Map<String, String>> allBulkCreateFieldsCopy = new ArrayList<>(allBulkCreateFields);

            for (int i = 0; i < allBulkCreateFieldsCopy.size(); ) {
                String fieldApiName = allBulkCreateFieldsCopy.get(i).get("apiName");

                if (fieldApiName.equalsIgnoreCase("supplier") || fieldApiName.equalsIgnoreCase("contractFromParent")) {
                    allBulkCreateFieldsCopy.remove(i);
                } else {
                    i++;
                }
            }

            for (String parentEntityName : parentEntitiesArr) {
                logger.info("Verifying Bulk Create Template of Action from Parent Entity {}", parentEntityName);
                String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
                int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    JSONObject jsonObj = new JSONObject(listDataResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                    int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                        int parentRecordId = ListDataHelper.getRecordIdFromValue(idValue);

                        String parentRecordShowResponse = ShowHelper.getShowResponse(parentEntityTypeId, parentRecordId);

                        if (ParseJsonResponse.validJsonResponse(parentRecordShowResponse)) {
                            Boolean hasBulkCreateOptionForActionEntity = ShowHelper.hasBulkCreateOptionForEntity(parentRecordShowResponse, "actions");

                            if (hasBulkCreateOptionForActionEntity == null) {
                                csAssert.assertTrue(false, "Couldn't find whether Record Id " + parentRecordId + " of Entity " + parentEntityName +
                                        " has Bulk Create Option to Create Actions Entity or not.");
                                break;
                            }

                            if (!hasBulkCreateOptionForActionEntity) {
                                continue;
                            }

                            String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(parentRecordShowResponse, "actions");

                            if (downloadAPIPath == null) {
                                csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Actions Entity from Record Id " +
                                        parentRecordId + " of Entity " + parentEntityName);
                                break;
                            }

                            logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                            Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

                            if (!fileDownloaded) {
                                throw new SkipException("Couldn't download Actions Bulk Create Template from Record Id " + parentRecordId + " of Entity " + parentEntityName);
                            }

                            List<Map<String, String>> allFieldsInBulkCreateTemplate = bulkHelperObj.getAllFieldsInBulkCreateTemplate(templatePath, templateName,
                                    "Action");

                            logger.info("Validating first three columns of Action Bulk Create Template from Entity {}", parentEntityName);

                            Map<String, String> fieldMap = allFieldsInBulkCreateTemplate.get(0);
                            String fieldId = fieldMap.get("id");

                            csAssert.assertTrue(fieldId.equalsIgnoreCase("100000001"), "Entity " + parentEntityName +
                                    " and Record Id " + parentRecordId + ". First Column is not Sl. No.");

                            String supplierFieldName = null;
                            for (Map<String, String> bulkCreateField : allBulkCreateFields) {
                                String fieldApiName = bulkCreateField.get("apiName");

                                if (fieldApiName.equalsIgnoreCase("supplier")) {
                                    supplierFieldName = bulkCreateField.get("fieldName");
                                    break;
                                }
                            }

                            if (supplierFieldName == null) {
                                csAssert.assertTrue(false, "Entity " + parentEntityName + " and Record Id " + parentRecordId +
                                        ". Couldn't find Expected Supplier Column Label.");
                            } else {
                                fieldMap = allFieldsInBulkCreateTemplate.get(1);
                                csAssert.assertTrue(fieldMap.get("label").trim().equalsIgnoreCase(supplierFieldName), "Entity " + parentEntityName +
                                        " and Record Id " + parentRecordId + ". Expected Second Column Supplier Label [" + supplierFieldName +
                                        "] and Actual Column Label [" + fieldMap.get("label") + "]");
                            }

                            String contractFieldName = null;
                            for (Map<String, String> bulkCreateField : allBulkCreateFields) {
                                String fieldApiName = bulkCreateField.get("apiName");

                                if (fieldApiName.equalsIgnoreCase("contractFromParent")) {
                                    contractFieldName = bulkCreateField.get("fieldName");
                                    break;
                                }
                            }

                            if (contractFieldName == null) {
                                csAssert.assertTrue(false, "Entity " + parentEntityName + " and Record Id " + parentRecordId +
                                        ". Couldn't find Expected Contract Column Label.");
                            } else {
                                fieldMap = allFieldsInBulkCreateTemplate.get(2);
                                csAssert.assertTrue(fieldMap.get("label").trim().equalsIgnoreCase(contractFieldName), "Entity " + parentEntityName +
                                        " and Record Id " + parentRecordId + ". Expected Third Column Contract Label [" + supplierFieldName +
                                        "] and Actual Column Label [" + fieldMap.get("label") + "]");
                            }

                            //Validate Fields in Template
                            validateBulkCreateTemplateColumns("actions", actionsEntityTypeId, parentEntityName, parentEntityTypeId, parentRecordId,
                                    parentRecordShowResponse, downloadAPIPath, csAssert);
                        } else {
                            csAssert.assertTrue(false, "Show API Response for Record Id " + parentRecordId + " of Entity " + parentEntityName + " is an Invalid JSON.");
                        }

                        break;
                    }
                } else {
                    csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
                }
            }

            testValidationMessagesForActionBulkTemplate(csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Action Bulk Create Template Columns. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    /*
    TC-C3435: Verify validation message in Template for Title Field.
    TC-C3436: Verify Validation message in Template for Requested On Field.
    TC-C3443: Verify Validation message in Template for Action Taken Field.
     */
    private void testValidationMessagesForActionBulkTemplate(CustomAssert csAssert) {
        logger.info("Starting Test TC-C3435: Verify Validation Message in Template for Title Field");
        logger.info("Getting All Headers and Ids List from Bulk Template");
        List<List<String>> bulkTemplateData = XLSUtils.getExcelDataOfMultipleRows(templatePath, templateName, "Action", 0, 5);

        if (bulkTemplateData == null || bulkTemplateData.size() != 5) {
            throw new SkipException("Couldn't get Data from Bulk Template.");
        }

        List<String> allHeaderIds = bulkTemplateData.get(1);
        List<String> allValidationMessages = bulkTemplateData.get(4);

        FieldRenaming fieldRenamingObj = new FieldRenaming();
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1, 467);

        if (allHeaderIds.contains("403")) {
            int titleColumnNo = allHeaderIds.indexOf("403");

            String actualValidationMessage = allValidationMessages.get(titleColumnNo);
            String expectedValidationMessage = fieldRenamingObj.getClientFieldNameFromId(fieldRenamingResponse, 8258);

            if (!actualValidationMessage.trim().toLowerCase().contains(expectedValidationMessage.toLowerCase())) {
                csAssert.assertTrue(false, "Expected Validation Message for Title Field: [" + expectedValidationMessage + "] and Actual Message: [" +
                        actualValidationMessage + "]");
            }
        } else {
            csAssert.assertTrue(false, "Header having Id 403 not found in All Header Ids List.");
        }

        logger.info("Starting Test TC-C3436: Verify Validation Message in Template for Requested On Field");
        if (allHeaderIds.contains("416")) {
            int requestedOnColumnNo = allHeaderIds.indexOf("416");

            String actualValidationMessage = allValidationMessages.get(requestedOnColumnNo);
            String expectedValidationMessage = fieldRenamingObj.getClientFieldNameFromName(fieldRenamingResponse, "Please select a date in past");

            if (!actualValidationMessage.toLowerCase().contains(expectedValidationMessage.toLowerCase())) {
                csAssert.assertTrue(false, "Expected Validation Message for Title Field: [" + expectedValidationMessage + "] and Actual Message: [" +
                        actualValidationMessage + "]");
            }
        } else {
            csAssert.assertTrue(false, "Header having Id 416 not found in All Header Ids List.");
        }

        logger.info("Starting Test TC-C3443: Verify Validation Message in Template for Action Taken Field");
        if (allHeaderIds.contains("424")) {
            int actionTakenColumnNo = allHeaderIds.indexOf("424");

            String actualValidationMessage = allValidationMessages.get(actionTakenColumnNo);
            String expectedValidationMessage = fieldRenamingObj.getClientFieldNameFromName(fieldRenamingResponse, "Please enter less than 1024 characters.");

            if (!actualValidationMessage.toLowerCase().contains(expectedValidationMessage.toLowerCase())) {
                csAssert.assertTrue(false, "Expected Validation Message for Title Field: [" + expectedValidationMessage + "] and Actual Message: [" +
                        actualValidationMessage + "]");
            }
        } else {
            csAssert.assertTrue(false, "Header having Id 424 not found in All Header Ids List.");
        }
    }


    /*
    TC-C63036: Verify Invoice Bulk Create Template Data
    TC-C4211: Verify Invoice & Line Item Bulk Create Template Data
     */
    @Test(enabled = false)
    public void testInvoiceBulkCreateTemplateC63036() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C63036: Verify Invoice Bulk Create Template Data.");
            String parentEntityName = "contracts";
            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int parentRecordId = ListDataHelper.getRecordIdFromValue(idValue);

                    String parentRecordShowResponse = ShowHelper.getShowResponse(parentEntityTypeId, parentRecordId);

                    if (ParseJsonResponse.validJsonResponse(parentRecordShowResponse)) {
                        Boolean hasBulkCreateOptionForInvoiceEntity = ShowHelper.hasBulkCreateOptionForEntity(parentRecordShowResponse, "invoices");

                        if (hasBulkCreateOptionForInvoiceEntity == null) {
                            csAssert.assertTrue(false, "Couldn't find whether Record Id " + parentRecordId + " of Entity " + parentEntityName +
                                    " has Bulk Create Option to Create Invoice Entity or not.");
                            break;
                        }

                        if (!hasBulkCreateOptionForInvoiceEntity) {
                            continue;
                        }

                        String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(parentRecordShowResponse, "invoices");

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Invoice Entity from Record Id " +
                                    parentRecordId + " of Entity " + parentEntityName);
                            break;
                        }

                        validateBulkCreateTemplateColumns("invoices", 67, parentEntityName, parentEntityTypeId, parentRecordId,
                                parentRecordShowResponse, downloadAPIPath, csAssert);
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + parentRecordId + " of Entity " + parentEntityName +
                                " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Invoice Bulk Create Template TC-C63036. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4012: Verify the Downloaded Template file name for Service Data Bulk Create.
     */
    @Test
    public void verifyDownloadedTemplateFileNameServiceDataC4012() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4012: Verify the Downloaded Bulk Create Template File Name for Service Data.");

            String entityTypeName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entity type mapping",
                    "service data");
            verifyServiceDataBulkCreateDownloadedTemplateName(entityTypeName, csAssert);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Downloaded Bulk Create Template File Name TC-C4012. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4014: Verify all the metadata fields are present in Downloaded Template.
    TC-C4052: Verify field/role group properties.
     */
    @Test
    public void testServiceDataBulkCreateTemplateColumns() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: Verify Service Data Bulk Create Template Columns.");
            String parentEntityName = "contracts";

            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
            List<Map<String, String>> allBulkCreateFields = fieldProvisioningObj.getAllBulkCreateFields(serviceDataEntityTypeId);

            if (allBulkCreateFields == null || allBulkCreateFields.isEmpty()) {
                throw new SkipException("Couldn't get All Bulk Create Fields for Service Data Entity");
            }

            List<Map<String, String>> allBulkCreateFieldsCopy = new ArrayList<>(allBulkCreateFields);

            for (int i = 0; i < allBulkCreateFieldsCopy.size(); ) {
                String fieldApiName = allBulkCreateFieldsCopy.get(i).get("apiName");

                if (fieldApiName.equalsIgnoreCase("supplier") || fieldApiName.equalsIgnoreCase("contract")) {
                    allBulkCreateFieldsCopy.remove(i);
                } else {
                    i++;
                }
            }

            logger.info("Verifying Bulk Create Template of Service Data from Parent Entity {}", parentEntityName);
            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int parentRecordId = ListDataHelper.getRecordIdFromValue(idValue);

                    String parentRecordShowResponse = ShowHelper.getShowResponse(parentEntityTypeId, parentRecordId);

                    if (ParseJsonResponse.validJsonResponse(parentRecordShowResponse)) {
                        Boolean hasBulkCreateOptionForServiceDataEntity = ShowHelper.hasBulkCreateOptionForEntity(parentRecordShowResponse, "service data");

                        if (hasBulkCreateOptionForServiceDataEntity == null) {
                            csAssert.assertTrue(false, "Couldn't find whether Record Id " + parentRecordId + " of Entity " + parentEntityName +
                                    " has Bulk Create Option to Create Service Data Entity or not.");
                            break;
                        }

                        if (!hasBulkCreateOptionForServiceDataEntity) {
                            continue;
                        }

                        String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(parentRecordShowResponse, "service data");

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Service Data Entity from Record Id " +
                                    parentRecordId + " of Entity " + parentEntityName);
                            break;
                        }

                        logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                        Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

                        if (!fileDownloaded) {
                            throw new SkipException("Couldn't download Service Data Bulk Create Template from Record Id " + parentRecordId + " of Entity " + parentEntityName);
                        }

                        List<Map<String, String>> allFieldsInBulkCreateTemplate = bulkHelperObj.getAllFieldsInBulkCreateTemplate(templatePath, templateName,
                                "Service Data");

                        logger.info("Validating first three columns of Service Data Bulk Create Template from Entity {}", parentEntityName);

                        Map<String, String> fieldMap = allFieldsInBulkCreateTemplate.get(0);
                        String fieldId = fieldMap.get("id");

                        csAssert.assertTrue(fieldId.equalsIgnoreCase("100000001"), "Entity " + parentEntityName +
                                " and Record Id " + parentRecordId + ". First Column is not Sl. No.");

                        String supplierFieldName = null;
                        for (Map<String, String> bulkCreateField : allBulkCreateFields) {
                            String fieldApiName = bulkCreateField.get("apiName");

                            if (fieldApiName.equalsIgnoreCase("supplier")) {
                                supplierFieldName = bulkCreateField.get("fieldName");
                                break;
                            }
                        }

                        if (supplierFieldName == null) {
                            csAssert.assertTrue(false, "Entity " + parentEntityName + " and Record Id " + parentRecordId +
                                    ". Couldn't find Expected Supplier Column Label.");
                        } else {
                            fieldMap = allFieldsInBulkCreateTemplate.get(1);
                            csAssert.assertTrue(fieldMap.get("label").trim().equalsIgnoreCase(supplierFieldName), "Entity " + parentEntityName + " and Record Id " +
                                    parentRecordId + ". Expected Second Column Supplier Label [" + supplierFieldName + "] and Actual Column Label [" +
                                    fieldMap.get("label") + "]");
                        }

                        String contractFieldName = null;
                        for (Map<String, String> bulkCreateField : allBulkCreateFields) {
                            String fieldApiName = bulkCreateField.get("apiName");

                            if (fieldApiName.equalsIgnoreCase("contract")) {
                                contractFieldName = bulkCreateField.get("fieldName");
                                break;
                            }
                        }

                        if (contractFieldName == null) {
                            csAssert.assertTrue(false, "Entity " + parentEntityName + " and Record Id " + parentRecordId +
                                    ". Couldn't find Expected Contract Column Label.");
                        } else {
                            fieldMap = allFieldsInBulkCreateTemplate.get(2);
                            csAssert.assertTrue(fieldMap.get("label").trim().equalsIgnoreCase(contractFieldName), "Entity " + parentEntityName + " and Record Id " +
                                    parentRecordId + ". Expected Third Column Contract Label [" + supplierFieldName + "] and Actual Column Label [" +
                                    fieldMap.get("label") + "]");
                        }

                        //Validate Sequence of Fields
                        validateBulkCreateTemplateColumns("service data", serviceDataEntityTypeId, parentEntityName, parentEntityTypeId, parentRecordId,
                                parentRecordShowResponse, downloadAPIPath, csAssert);
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + parentRecordId + " of Entity " + parentEntityName +
                                " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Service Data Bulk Create Template Columns. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4015: Verify Bulk Create Template contains 4 sheets: Instructions, Information, Service Data, Master Data
     */
    @Test
    public void testC4015() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4015: Verify Bulk Create Template contains 4 sheets: Instructions, Information, Service Data, Master Data");
            String parentEntityName = "contracts";

            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForServiceDataEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "service data");

                        if (hasBulkCreateOptionForServiceDataEntity == null) {
                            csAssert.assertTrue(false, "Couldn't find whether Record Id " + recordId + " of Entity " + parentEntityName +
                                    " has Bulk Create Option to Create Service Data Entity or not.");
                            break;
                        }

                        if (!hasBulkCreateOptionForServiceDataEntity) {
                            continue;
                        }

                        String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(showResponse, "service data");

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Service Data Entity from Record Id " +
                                    recordId + " of Entity " + parentEntityName);
                            break;
                        }

                        logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                        Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

                        if (!fileDownloaded) {
                            throw new SkipException("Couldn't download Service Data Bulk Create Template from Record Id " + recordId + " of Entity " + parentEntityName);
                        }

                        XLSUtils xlsObj = new XLSUtils(templatePath, templateName);
                        List<String> allSheetNames = xlsObj.getSheetNames();

                        if (allSheetNames == null || allSheetNames.isEmpty()) {
                            throw new SkipException("Couldn't get All Sheet Names of Template located at [" + templatePath + "/" + templateName + "]");
                        }

                        csAssert.assertTrue(allSheetNames.contains("Instructions"), "Instructions Sheet not present in Downloaded Bulk Create Template");
                        csAssert.assertTrue(allSheetNames.contains("Information"), "Information Sheet not present in Downloaded Bulk Create Template");
                        csAssert.assertTrue(allSheetNames.contains("Master Data"), "Master Data Sheet not present in Downloaded Bulk Create Template");
                        csAssert.assertTrue(allSheetNames.contains("Service Data"), "Service Data Sheet not present in Downloaded Bulk Create Template");
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity " + parentEntityName + " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4015. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3937: Verify the Content of Information Tab in Downloaded Template
     */
    @Test
    public void testC3937() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3937: Verify the Content of Information Tab in Downloaded Template of Service Data.");
            String parentEntityName = "contracts";

            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForServiceDataEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "service data");

                        if (hasBulkCreateOptionForServiceDataEntity == null) {
                            csAssert.assertTrue(false, "Couldn't find whether Record Id " + recordId + " of Entity " + parentEntityName +
                                    " has Bulk Create Option to Create Service Data Entity or not.");
                            break;
                        }

                        if (!hasBulkCreateOptionForServiceDataEntity) {
                            continue;
                        }

                        String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(showResponse, "service data");

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Service Data Entity from Record Id " +
                                    recordId + " of Entity " + parentEntityName);
                            break;
                        }

                        logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                        Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

                        if (!fileDownloaded) {
                            throw new SkipException("Couldn't download Service Data Bulk Create Template from Record Id " + recordId + " of Entity " + parentEntityName);
                        }

                        List<String> dataOfFirstRow = XLSUtils.getExcelDataOfOneRow(templatePath, templateName, "Information", 1);

                        if (dataOfFirstRow == null || dataOfFirstRow.isEmpty()) {
                            throw new SkipException("Couldn't get Data of First Row.");
                        }

                        csAssert.assertTrue(dataOfFirstRow.get(0).trim().equalsIgnoreCase("Bulk Create - Service Data"), "" +
                                "Expected First Row Data: [Bulk Create - Service Data] and Actual Data: [" + dataOfFirstRow.get(0) + "]");

                        List<List<String>> allRowsData = XLSUtils.getExcelDataOfMultipleRows(templatePath, templateName, "Information", 2,
                                4);

                        if (allRowsData == null || allRowsData.size() != 4) {
                            throw new SkipException("Couldn't get All Rows Data from Template.");
                        }

                        List<String> downloadedByRowData = allRowsData.get(0);
                        List<String> downloadDateRowData = allRowsData.get(1);
                        List<String> supplierRowData = allRowsData.get(2);
                        List<String> parentEntityRowData = allRowsData.get(3);

                        if (!downloadedByRowData.get(0).equalsIgnoreCase("Downloaded By") ||
                                !downloadedByRowData.get(1).equalsIgnoreCase("Anay User")) {
                            csAssert.assertTrue(false, "Data Validation Failed for Downloaded By Row.");
                        }

                        Date currentDate = new Date();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy");
                        String expectedDate = dateFormat.format(currentDate);

                        if (!downloadDateRowData.get(0).equalsIgnoreCase("Download Date & Time") ||
                                !downloadDateRowData.get(1).contains(expectedDate)) {
                            csAssert.assertTrue(false, "Data Validation Failed for Download Date & Time Row.");
                        }

                        String expectedSupplierName = ShowHelper.getValueOfField(entityTypeId, "supplier", showResponse);

                        if (expectedSupplierName == null) {
                            throw new SkipException("Couldn't get Expected Supplier Name from Contract Id " + recordId);
                        }

                        if (!supplierRowData.get(0).equalsIgnoreCase("Supplier") ||
                                !supplierRowData.get(1).toLowerCase().contains(expectedSupplierName.toLowerCase())) {
                            csAssert.assertTrue(false, "Data Validation Failed for Supplier Row.");
                        }

                        String expectedName = ShowHelper.getValueOfField(entityTypeId, "title", showResponse);

                        if (!parentEntityRowData.get(0).equalsIgnoreCase("Parent Entity") ||
                                !parentEntityRowData.get(1).toLowerCase().contains(expectedName.toLowerCase())) {
                            csAssert.assertTrue(false, "Data Validation Failed for Parent Entity Row.");
                        }

                        List<String> disclaimerRowData = XLSUtils.getExcelDataOfOneRow(templatePath, templateName, "Information", 6);

                        if (disclaimerRowData == null || disclaimerRowData.isEmpty()) {
                            throw new SkipException("Couldn't get Disclaimer Row Data.");
                        }

                        String expectedDisclaimer = "CONFIDENTIALITY AND DISCLAIMER\r\n" +
                                "The information in this document is proprietary and confidential and is provided upon the recipient's promise to keep such information " +
                                "confidential. In no event may this information be supplied to third parties without <Client's Name>'s prior written consent.\r\n" +
                                "The following notice shall be reproduced on any copies permitted to be made:\r\n" +
                                "<Client's Name> Confidential & Proprietary. All rights reserved.";

                        csAssert.assertTrue(disclaimerRowData.get(0).equalsIgnoreCase(expectedDisclaimer), "Disclaimer Data Validation Failed. " +
                                "Expected Disclaimer Data: [" + expectedDisclaimer + "] and Actual Disclaimer Data: [" + disclaimerRowData.get(0) + "]");
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity " + parentEntityName +
                                " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3937. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    /*
    TC-C3939: Verify the Content of Instructions Tab in Downloaded Template
     */
    @Test
    public void testC3939() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3939: Verify the Content of Instructions Tab in Downloaded Template");
            String parentEntityName = "contracts";

            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForServiceDataEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "service data");

                        if (hasBulkCreateOptionForServiceDataEntity == null) {
                            csAssert.assertTrue(false, "Couldn't find whether Record Id " + recordId + " of Entity " + parentEntityName +
                                    " has Bulk Create Option to Create Service Data Entity or not.");
                            break;
                        }

                        if (!hasBulkCreateOptionForServiceDataEntity) {
                            continue;
                        }

                        String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(showResponse, "service data");

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Service Data Entity from Record Id " +
                                    recordId + " of Entity " + parentEntityName);
                            break;
                        }

                        logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                        Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

                        if (!fileDownloaded) {
                            throw new SkipException("Couldn't download Service Data Bulk Create Template from Record Id " + recordId + " of Entity " + parentEntityName);
                        }

                        List<String> allKeyData = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Instructions", 5,
                                0, 3);

                        if (allKeyData == null || allKeyData.isEmpty()) {
                            throw new SkipException("Couldn't get All Key Data of Instructions Sheet.");
                        }

                        if (!allKeyData.get(0).equalsIgnoreCase("Template Type") || !allKeyData.get(1).equalsIgnoreCase("Template Version")
                                || !allKeyData.get(2).equalsIgnoreCase("Feature Instructions")) {
                            csAssert.assertTrue(false, "Data Validation Failed for All Keys in Instructions Sheet.");
                        }

                        List<String> allValueData = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Instructions", 6,
                                0, 3);

                        if (allValueData == null || allValueData.isEmpty()) {
                            throw new SkipException("Couldn't get All Value Data of Instructions Sheet.");
                        }

                        FieldRenaming fieldRenamingObj = new FieldRenaming();
                        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1, 2154);
                        String expectedValue = fieldRenamingObj.getExpectedFeatureInstructionValueForBulkCreate(fieldRenamingResponse);

                        if (expectedValue == null) {
                            throw new SkipException("Couldn't get Expected Feature Instructions Value.");
                        }

                        expectedValue = expectedValue.replaceAll("<br>", "").replaceAll("\n", "").replaceAll("\r\n", "");
                        String actualValue = allValueData.get(2).replaceAll("\n", "").replaceAll("\r\n", "");

                        if (!actualValue.equalsIgnoreCase(expectedValue)) {
                            csAssert.assertTrue(false, "Feature Instructions Validation Failed. Expected Value: [" + expectedValue +
                                    "] and Actual Value: [" + actualValue + "]");
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity " + parentEntityName +
                                " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3939. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4013: Verify the Bulk Create Downloaded File Name after changing the entity name in Field Labels for Service Data.
     */
    @Test
    public void testC4013() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInPassword = Check.lastLoggedInUserPassword;
        Check checkObj = new Check();

        try {
            logger.info("Starting Test TC-C4013: Verify the Bulk Create Downloaded File Name after changing the entity name in Field Labels for Service Data.");
            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            logger.info("Updating Entity Name label for Service Data.");
            logger.info("Hitting Field Label API findLabelsByGroupIdAndLanguageId for Group Id 431 and Language Id 1");
            FieldRenaming fieldRenamingObj = new FieldRenaming();
            String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1, 431);

            if (ParseJsonResponse.validJsonResponse(fieldRenamingResponse)) {
                JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("childGroups").getJSONObject(0).getJSONArray("fieldLabels");
                String serviceDataPreviousName = null;
                String serviceDataUpdatedName = "Service Data API";

                String updatePayload = fieldRenamingResponse;
                boolean serviceDataFieldFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    String entityName = jsonObj.getString("name");

                    if (entityName.equalsIgnoreCase("Service Data")) {
                        serviceDataFieldFound = true;
                        serviceDataPreviousName = jsonObj.getString("clientFieldName");
                        updatePayload = updatePayload.replace("clientFieldName\":\"" + serviceDataPreviousName, "clientFieldName\":\"" +
                                serviceDataUpdatedName);
                        break;
                    }
                }

                if (!serviceDataFieldFound) {
                    throw new SkipException("Couldn't find Service Data Entity Field in Field Label API Response.");
                }

                logger.info("Hitting Field Label Update API.");
                String fieldUpdateResponse = fieldRenamingObj.hitFieldUpdate(updatePayload);

                if (ParseJsonResponse.validJsonResponse(fieldUpdateResponse)) {
                    jsonObj = new JSONObject(fieldUpdateResponse);

                    if (jsonObj.getBoolean("isSuccess")) {
                        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInPassword);

                        verifyServiceDataBulkCreateDownloadedTemplateName(serviceDataUpdatedName, csAssert);

                        //Reverting Entity Name Label for Service Data.

                        adminHelperObj.loginWithClientAdminUser();
                        updatePayload = updatePayload.replace("clientFieldName\":\"" + serviceDataUpdatedName, "clientFieldName\":\"" +
                                serviceDataPreviousName);

                        fieldRenamingObj.hitFieldUpdate(updatePayload);
                    } else {
                        throw new SkipException("Couldn't Update Field Label for Service Data Entity.");
                    }
                } else {
                    throw new SkipException("Couldn't Update Field Label for Service Data Entity.");
                }
            } else {
                csAssert.assertTrue(false, "Field Renaming API findLabelsByGroupIdAndLanguageId Response for Group Id 431 and Language Id 1" +
                        " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4013. " + e.getMessage());
        }

        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInPassword);
        csAssert.assertAll();
    }

    private void verifyServiceDataBulkCreateDownloadedTemplateName(String expectedEntityName, CustomAssert csAssert) {
        try {
            String parentEntityName = "contracts";

            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForServiceDataEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "service data");

                        if (hasBulkCreateOptionForServiceDataEntity == null) {
                            csAssert.assertTrue(false, "Couldn't find whether Record Id " + recordId + " of Entity " + parentEntityName +
                                    " has Bulk Create Option to Create Service Data Entity or not.");
                            break;
                        }

                        if (!hasBulkCreateOptionForServiceDataEntity) {
                            continue;
                        }

                        String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(showResponse, "service data");

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Service Data Entity from Record Id " +
                                    recordId + " of Entity " + parentEntityName);
                            break;
                        }

                        logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                        Download bulkDownloadObj = new Download();
                        HttpResponse httpResponse = bulkDownloadObj.hitDownload(downloadAPIPath);
                        Header[] allHeaders = httpResponse.getAllHeaders();

                        boolean fileNameFound = false;

                        for (Header oneHeader : allHeaders) {
                            if (oneHeader.toString().contains("Content-Disposition:")) {
                                fileNameFound = true;
                                String[] valueArr = oneHeader.toString().split(Pattern.quote("="));
                                String actualFileName = valueArr[valueArr.length - 1].replaceAll("\"", "");

                                DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date date = new Date();
                                String currentDate = dateFormat.format(date);
                                String[] temp = ConfigureEnvironment.getEnvironmentProperty("host").split(Pattern.quote("."));
                                String clientAliasName = temp[0];

                                //String expectedFileName = clientAliasName + "-" + expectedEntityName + "-" + currentDate + ".xlsm";
                                String expectedFileName=expectedEntityName + "-" + currentDate + ".xlsm";


                                if (!actualFileName.equalsIgnoreCase(expectedFileName)) {
                                    csAssert.assertTrue(false, "Entity " + parentEntityName + " Expected File Name: [" + expectedFileName +
                                            "] and Actual File Name: [" + actualFileName + "]");
                                }

                                break;
                            }
                        }

                        csAssert.assertTrue(fileNameFound, "Couldn't find File Name in Download API Response for Entity " + parentEntityName);
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity " + parentEntityName +
                                " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Service Data Bulk Create Downloaded Template Name. " + e.getMessage());
        }
    }


    /*
    TC-C10713: Verify all Dependent Fields when Service Data Bulk Create Template is downloaded from Supplier.
    TC-C4022: Verify Values of Invoicing Type Field.
     */
    @Test
    public void testC10713() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C10713: Validating all Dependent Fields when Service Data Bulk Create Template is downloaded from Supplier.");
            String parentEntityName = "suppliers";

            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int supplierId = ListDataHelper.getRecordIdFromValue(idValue);

                    String showResponse = ShowHelper.getShowResponse(entityTypeId, supplierId);

                    boolean hasBulkCreateOptionForServiceDataEntity = false;

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        JSONObject showJsonObj = new JSONObject(showResponse);
                        String downloadAPIPath = null;

                        if (showJsonObj.has("createLinks") && !showJsonObj.isNull("createLinks")) {
                            JSONArray showJsonArr = showJsonObj.getJSONObject("createLinks").getJSONArray("fields");

                            for (int j = 0; j < showJsonArr.length(); j++) {
                                JSONObject internalJsonObj = showJsonArr.getJSONObject(j);

                                if (internalJsonObj.has("properties") && !internalJsonObj.isNull("properties")) {
                                    internalJsonObj = internalJsonObj.getJSONObject("properties");

                                    if (internalJsonObj.has("uploadAPI") && !internalJsonObj.isNull("uploadAPI")) {
                                        if (internalJsonObj.getString("uploadAPI").contains("64")) {
                                            hasBulkCreateOptionForServiceDataEntity = true;
                                            downloadAPIPath = internalJsonObj.getString("downloadAPI");
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (!hasBulkCreateOptionForServiceDataEntity) {
                            continue;
                        }

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Service Data Entity from Record Id " +
                                    supplierId + " of Entity " + parentEntityName);
                            break;
                        }

                        logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                        Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

                        if (!fileDownloaded) {
                            throw new SkipException("Couldn't download Service Data Bulk Create Template from Record Id " + supplierId + " of Entity " + parentEntityName);
                        }

                        List<String> allHeaderIdsOfMasterSheet = XLSUtils.getExcelDataOfOneRow(templatePath, templateName, "Master Data", 2);

                        if (allHeaderIdsOfMasterSheet == null || allHeaderIdsOfMasterSheet.isEmpty()) {
                            throw new SkipException("Couldn't get All Header Ids in Master Data Sheet.");
                        }

                        //Verify all Contract Options
                        logger.info("Verifying All Contract Options in Bulk Create Template");
                        ContractTreeData contractTreeObj = new ContractTreeData();
                        List<String> allExpectedContractShortCodeIds = contractTreeObj.getAllContractShortCodeIdOfSupplier(supplierId);

                        if (allExpectedContractShortCodeIds == null) {
                            throw new SkipException("Couldn't get All Contract Short Code Ids of Supplier Id " + supplierId);
                        }

                        if (!allHeaderIdsOfMasterSheet.contains("4040")) {
                            throw new SkipException("Couldn't find Header Id 4040 in Master Data Sheet.");
                        }

                        int contractColumnNo = allHeaderIdsOfMasterSheet.indexOf("4040");
                        int noOfRowsInMasterDataSheet = XLSUtils.getNoOfRows(templatePath, templateName, "Master Data").intValue();

                        List<String> allActualContractOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                contractColumnNo, 4, noOfRowsInMasterDataSheet);

                        if (allActualContractOptions == null) {
                            throw new SkipException("Couldn't get All Contract Options from Master Data Sheet.");
                        }

                        List<String> actualContractShortCodeIds = new ArrayList<>();

                        for (String contractOption : allActualContractOptions) {
                            String[] temp = contractOption.split("CO");
                            String shortCodeId = temp[1].replace(")", "").trim();

                            if (shortCodeId.startsWith("0")) {
                                shortCodeId = shortCodeId.substring(1);
                            }

                            actualContractShortCodeIds.add(shortCodeId);
                        }

                        if (allExpectedContractShortCodeIds.size() == allActualContractOptions.size()) {
                            for (String actualContractId : actualContractShortCodeIds) {
                                if (!allExpectedContractShortCodeIds.contains(actualContractId)) {
                                    csAssert.assertTrue(false, "Contract Short Code Id " + actualContractId +
                                            " found in Master Data Sheet but doesn't belong to Supplier Id " + supplierId);
                                }
                            }
                        } else {
                            //When Contract Tree Response doesn't give all the child contracts list then checking on show page of contracts.
                            GetEntityId getEntityIdObj = new GetEntityId();
                            List<String> contractRecordIds = new ArrayList<>();

                            for (String expectedContractShortCodeId : allExpectedContractShortCodeIds) {
                                String shortId = "CO0" + expectedContractShortCodeId;

                                contractRecordIds.add(getEntityIdObj.hitGetEntityId("contracts", shortId));
                            }

                            for (String actualContractShortId : actualContractShortCodeIds) {
                                if (!allExpectedContractShortCodeIds.contains(actualContractShortId)) {
                                    String recordId = getEntityIdObj.hitGetEntityId("contracts", actualContractShortId);
                                    showResponse = ShowHelper.getShowResponse(61, Integer.parseInt(recordId));

                                    String parentEntityId = ShowHelper.getValueOfField("parententityid", showResponse);

                                    if (!contractRecordIds.contains(parentEntityId)) {
                                        csAssert.assertTrue(false, "Contract Options Size doesn't match. Expected Contract Options Size: " +
                                                allExpectedContractShortCodeIds.size() + " and Actual Contract Options Size: " + allActualContractOptions.size());
                                    }
                                }
                            }
                        }

                        //Verify Functions
                        logger.info("Verifying All Functions Options in Bulk Create Template");
                        if (allHeaderIdsOfMasterSheet.contains("11731")) {
                            String functionsHierarchy = ShowHelper.getShowFieldHierarchy("functions", 1);
                            List<String> allExpectedFunctions = ShowHelper.getAllSelectValuesOfField(showResponse, "functions", functionsHierarchy,
                                    supplierId, 1);

                            if (allExpectedFunctions == null || allExpectedFunctions.isEmpty()) {
                                throw new SkipException("Couldn't get All Expected Functions of Supplier Id " + supplierId);
                            }

                            int functionsColumnNo = allHeaderIdsOfMasterSheet.indexOf("11731");

                            List<String> allActualFunctionOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                    functionsColumnNo, 4, noOfRowsInMasterDataSheet);

                            if (allActualFunctionOptions == null) {
                                throw new SkipException("Couldn't get All Function Options from Master Data Sheet.");
                            }

                            if (allExpectedFunctions.size() == allActualFunctionOptions.size()) {
                                List<String> actualFunctionNames = new ArrayList<>();

                                for (String functionOption : allActualFunctionOptions) {
                                    String[] temp = functionOption.split(Pattern.quote("::"));
                                    String functionName = temp[temp.length - 1].trim();

                                    actualFunctionNames.add(functionName.toLowerCase());
                                }

                                for (String actualFunction : actualFunctionNames) {
                                    if (!allExpectedFunctions.contains(actualFunction.toLowerCase())) {
                                        csAssert.assertTrue(false, "Function Option " + actualFunction +
                                                " found in Master Data Sheet but doesn't belong to Supplier Id " + supplierId);
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Function Options Size doesn't match. Expected Function Options Size: " +
                                        allExpectedFunctions.size() + " and Actual Function Options Size: " + allActualFunctionOptions.size());
                            }
                        } else {
                            throw new SkipException("Couldn't find Functions Header in Master Data Sheet.");
                        }

                        //Verify Services
                        logger.info("Verifying All Services Options in Bulk Create Template");
                        if (allHeaderIdsOfMasterSheet.contains("11732")) {
                            String servicesHierarchy = ShowHelper.getShowFieldHierarchy("services", 1);
                            List<String> allExpectedServices = ShowHelper.getAllSelectValuesOfField(showResponse, "services", servicesHierarchy,
                                    supplierId, 1);

                            if (allExpectedServices == null || allExpectedServices.isEmpty()) {
                                throw new SkipException("Couldn't get All Expected Services of Supplier Id " + supplierId);
                            }

                            int servicesColumnNo = allHeaderIdsOfMasterSheet.indexOf("11732");

                            List<String> allActualServiceOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                    servicesColumnNo, 4, noOfRowsInMasterDataSheet);

                            if (allActualServiceOptions == null) {
                                throw new SkipException("Couldn't get All Services Options from Master Data Sheet.");
                            }

                            if (allExpectedServices.size() == allActualServiceOptions.size()) {
                                List<String> actualServiceNames = new ArrayList<>();

                                for (String serviceOption : allActualServiceOptions) {
                                    String[] temp = serviceOption.split(Pattern.quote("::"));
                                    String serviceName = temp[temp.length - 1].trim();

                                    String temp1[]=serviceName.split(Pattern.quote("("));
                                    actualServiceNames.add(temp1[0].trim().toLowerCase());
                                }

                                for (String actualService : actualServiceNames) {
                                    if (!allExpectedServices.contains(actualService.toLowerCase())) {
                                        csAssert.assertTrue(false, "Service Option " + actualService +
                                                " found in Master Data Sheet but doesn't belong to Supplier Id " + supplierId);
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Services Options Size doesn't match. Expected Services Options Size: " +
                                        allExpectedServices.size() + " and Actual Services Options Size: " + allActualServiceOptions.size());
                            }
                        } else {
                            throw new SkipException("Couldn't find Services Header in Master Data Sheet.");
                        }

                        //Verify Regions
                        logger.info("Verifying All Regions Options in Bulk Create Template");
                        if (allHeaderIdsOfMasterSheet.contains("11733")) {
                            String regionsHierarchy = ShowHelper.getShowFieldHierarchy("globalregions", 1);
                            List<String> allExpectedRegions = ShowHelper.getAllSelectValuesOfField(showResponse, "globalregions", regionsHierarchy,
                                    supplierId, 1);

                            if (allExpectedRegions == null || allExpectedRegions.isEmpty()) {
                                throw new SkipException("Couldn't get All Expected Regions of Supplier Id " + supplierId);
                            }

                            int regionsColumnNo = allHeaderIdsOfMasterSheet.indexOf("11733");

                            List<String> allActualRegionsOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                    regionsColumnNo, 4, noOfRowsInMasterDataSheet);

                            if (allActualRegionsOptions == null) {
                                throw new SkipException("Couldn't get All Regions Options from Master Data Sheet.");
                            }

                            if (allExpectedRegions.size() == allActualRegionsOptions.size()) {
                                List<String> actualRegionNames = new ArrayList<>();

                                for (String regionOption : allActualRegionsOptions) {
                                    String[] temp = regionOption.split(Pattern.quote("::"));
                                    String regionName = temp[temp.length - 1].trim();

                                    actualRegionNames.add(regionName.toLowerCase());
                                }

                                for (String actualRegion : actualRegionNames) {
                                    if (!allExpectedRegions.contains(actualRegion.toLowerCase())) {
                                        csAssert.assertTrue(false, "Region Option " + actualRegion +
                                                " found in Master Data Sheet but doesn't belong to Supplier Id " + supplierId);
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Region Options Size doesn't match. Expected Regions Options Size: " +
                                        allExpectedRegions.size() + " and Actual Region Options Size: " + allActualRegionsOptions.size());
                            }
                        } else {
                            throw new SkipException("Couldn't find Regions Header in Master Data Sheet.");
                        }

                        //Verify Countries
                        logger.info("Verifying All Countries Options in Bulk Create Template");
                        if (allHeaderIdsOfMasterSheet.contains("11343")) {
                            String countriesHierarchy = ShowHelper.getShowFieldHierarchy("globalcountries", 1);
                            List<String> allExpectedCountries = ShowHelper.getAllSelectValuesOfField(showResponse, "globalcountries", countriesHierarchy,
                                    supplierId, 1);

                            if (allExpectedCountries == null || allExpectedCountries.isEmpty()) {
                                throw new SkipException("Couldn't get All Expected Countries of Supplier Id " + supplierId);
                            }

                            int countriesColumnNo = allHeaderIdsOfMasterSheet.indexOf("11343");

                            List<String> allActualCountriesOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                    countriesColumnNo, 4, noOfRowsInMasterDataSheet);

                            if (allActualCountriesOptions == null) {
                                throw new SkipException("Couldn't get All Countries Options from Master Data Sheet.");
                            }

                            if (allExpectedCountries.size() == allActualCountriesOptions.size()) {
                                List<String> actualCountryNames = new ArrayList<>();

                                for (String countryOption : allActualCountriesOptions) {
                                    String[] temp = countryOption.split(Pattern.quote("::"));
                                    String countryName = temp[temp.length - 1].trim();

                                    actualCountryNames.add(countryName.toLowerCase());
                                }

                                for (String actualCountry : actualCountryNames) {
                                    if (!allExpectedCountries.contains(actualCountry.toLowerCase())) {
                                        csAssert.assertTrue(false, "Countries Option " + actualCountry +
                                                " found in Master Data Sheet but doesn't belong to Supplier Id " + supplierId);
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Countries Options Size doesn't match. Expected Countries Options Size: " +
                                        allExpectedCountries.size() + " and Actual Countries Options Size: " + allActualCountriesOptions.size());
                            }
                        } else {
                            throw new SkipException("Couldn't find Countries Header in Master Data Sheet.");
                        }

                        //Verify Invoicing Type Field.
                        logger.info("Verifying All Invoicing Type Options in Bulk Create Template");
                        if (allHeaderIdsOfMasterSheet.contains("11336")) {
                            int invoicingTypeColumnNo = allHeaderIdsOfMasterSheet.indexOf("11336");

                            List<String> allActualInvoicingTypeOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                    invoicingTypeColumnNo, 4, noOfRowsInMasterDataSheet);

                            if (allActualInvoicingTypeOptions == null) {
                                throw new SkipException("Couldn't get All Invoicing Type Options from Master Data Sheet.");
                            }

                            String[] expectedTypesArr = {
                                    "arc/rrc",
                                    "fixed fee",
                                    "forecast",
                                    "volume pricing"
                            };

                            List<String> allExpectedInvoicingTypeOptions = new ArrayList<>(Arrays.asList(expectedTypesArr));

                            if (allExpectedInvoicingTypeOptions.size() == allActualInvoicingTypeOptions.size()) {
                                for (String actualInvoicingType : allActualInvoicingTypeOptions) {
                                    if (!allExpectedInvoicingTypeOptions.contains(actualInvoicingType.toLowerCase())) {
                                        csAssert.assertTrue(false, "Invoicing Type Option " + actualInvoicingType +
                                                " found in Master Data Sheet but is not expected.");
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Invoicing Type Options Size doesn't match. Expected Invoicing Type Options Size: " +
                                        allExpectedInvoicingTypeOptions.size() + " and Actual Invoicing Type Options Size: " + allActualInvoicingTypeOptions.size());
                            }
                        } else {
                            throw new SkipException("Couldn't find Invoicing Type Header in Master Data Sheet.");
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + supplierId + " of Entity " + parentEntityName +
                                " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C10713: " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4112: Verify that Currency Values should come according to selected in Contract Show Page.
     */
    @Test
    public void testC4112() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4112: Validating that Currency Values should come according to selected in Contract Show Page.");
            String parentEntityName = "contracts";

            String listDataResponse = ListDataHelper.getListDataResponse(parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int contractId = ListDataHelper.getRecordIdFromValue(idValue);

                    String showResponse = ShowHelper.getShowResponse(entityTypeId, contractId);

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        Boolean hasBulkCreateOptionForServiceDataEntity = ShowHelper.hasBulkCreateOptionForEntity(showResponse, "service data");

                        if (hasBulkCreateOptionForServiceDataEntity == null || !hasBulkCreateOptionForServiceDataEntity) {
                            continue;
                        }

                        String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(showResponse, "service data");

                        if (downloadAPIPath == null) {
                            csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Service Data Entity from Record Id " +
                                    contractId + " of Entity " + parentEntityName);
                            break;
                        }

                        logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                        Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

                        if (!fileDownloaded) {
                            throw new SkipException("Couldn't download Service Data Bulk Create Template from Record Id " + contractId + " of Entity " + parentEntityName);
                        }

                        List<String> allHeaderIdsOfMasterSheet = XLSUtils.getExcelDataOfOneRow(templatePath, templateName, "Master Data", 2);

                        if (allHeaderIdsOfMasterSheet == null || allHeaderIdsOfMasterSheet.isEmpty()) {
                            throw new SkipException("Couldn't get All Header Ids in Master Data Sheet.");
                        }

                        //Verify all Currency Options
                        logger.info("Verifying All Currency Options in Bulk Create Template");
                        if (allHeaderIdsOfMasterSheet.contains("8051")) {
                            String currencyHierarchy = ShowHelper.getShowFieldHierarchy("currency", 61);
                            List<String> allExpectedCurrencies = ShowHelper.getAllSelectValuesOfField(showResponse, "currency", currencyHierarchy,
                                    contractId, 61);

                            if (allExpectedCurrencies == null || allExpectedCurrencies.isEmpty()) {
                                throw new SkipException("Couldn't get All Expected Currencies of Contract Id " + contractId);
                            }

                            int currencyColumnNo = allHeaderIdsOfMasterSheet.indexOf("8051");
                            int noOfRowsInMasterDataSheet = XLSUtils.getNoOfRows(templatePath, templateName, "Master Data").intValue();

                            List<String> allActualCurrencyOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                    currencyColumnNo, 4, noOfRowsInMasterDataSheet);

                            if (allActualCurrencyOptions == null) {
                                throw new SkipException("Couldn't get All Currency Options from Master Data Sheet.");
                            }

                            if (allExpectedCurrencies.size() == allActualCurrencyOptions.size()) {
                                List<String> actualCurrencyNames = new ArrayList<>();

                                for (String currencyOption : allActualCurrencyOptions) {
                                    String[] temp = currencyOption.split(Pattern.quote("::"));
                                    String currencyName = temp[temp.length - 1].trim();

                                    actualCurrencyNames.add(currencyName.toLowerCase());
                                }

                                for (String actualCurrency : actualCurrencyNames) {
                                    if (!allExpectedCurrencies.contains(actualCurrency.toLowerCase())) {
                                        csAssert.assertTrue(false, "Currency Option " + actualCurrency +
                                                " found in Master Data Sheet but doesn't belong to Contract Id " + contractId);
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Currency Options Size doesn't match. Expected Currency Options Size: " +
                                        allExpectedCurrencies.size() + " and Actual Currency Options Size: " + allActualCurrencyOptions.size());
                            }
                        } else {
                            throw new SkipException("Couldn't find Currency Header in Master Data Sheet.");
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + contractId + " of Entity " + parentEntityName +
                                " is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + parentEntityName + " is an Invalid JSON.");
            }

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4112: " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4212:  Verify that all the Dependent Data is coming according to the Parent Entity in Bulk Create Template for Invoice and Line Item.
     */
    @Test(enabled = false)
    public void testC4212() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4212: Validating that all Dependent Fields data should come from parent entity for Invoice and Invoice Line Item.");
            String[] parentEntitiesArr = {"suppliers", "contracts"};
            String listDataResponse = ListDataHelper.getListDataResponse("service data");

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                    int serviceDataRecordId = ListDataHelper.getRecordIdFromValue(idValue);

                    String serviceDataShowResponse = ShowHelper.getShowResponse(64, serviceDataRecordId);

                    if (ParseJsonResponse.validJsonResponse(serviceDataShowResponse)) {
                        String isBillingAvailable = ShowHelper.getValueOfField(64, "billingavailable", serviceDataShowResponse);

                        if (!isBillingAvailable.equalsIgnoreCase("true")) {
                            continue;
                        }

                        int parentRecordId;

                        int parentSupplierId = Integer.parseInt(ShowHelper.getValueOfField(64, "supplierid", serviceDataShowResponse));
                        String supplierShowResponse = ShowHelper.getShowResponse(1, parentSupplierId);

                        Boolean supplierHasBulkCreateInvoicePermission = ShowHelper.hasBulkCreateOptionForEntity(supplierShowResponse, "invoices");

                        if (supplierHasBulkCreateInvoicePermission == null || !supplierHasBulkCreateInvoicePermission) {
                            continue;
                        }

                        for (String parentEntityName : parentEntitiesArr) {
                            int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

                            if (parentEntityName.equalsIgnoreCase("suppliers")) {
                                parentRecordId = Integer.parseInt(ShowHelper.getValueOfField(64, "supplierid", serviceDataShowResponse));
                            } else {
                                parentRecordId = Integer.parseInt(ShowHelper.getValueOfField(64, "contractid", serviceDataShowResponse));
                            }

                            String parentEntityShowResponse = ShowHelper.getShowResponse(parentEntityTypeId, parentRecordId);

                            String downloadAPIPath = ShowHelper.getBulkCreateTemplateDownloadAPIForEntity(parentEntityShowResponse, "invoices");

                            if (downloadAPIPath == null) {
                                csAssert.assertTrue(false, "Couldn't get Bulk Create Template Download API Path for Invoice Entity from Record Id " +
                                        parentRecordId + " of Entity " + parentEntityName);
                                break;
                            }

                            logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
                            Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

                            if (!fileDownloaded) {
                                throw new SkipException("Couldn't download Invoice Bulk Create Template from Record Id " + parentRecordId + " of Entity " + parentEntityName);
                            }

                            List<String> allHeaderIdsOfMasterSheet = XLSUtils.getExcelDataOfOneRow(templatePath, templateName, "Master Data", 2);

                            if (allHeaderIdsOfMasterSheet == null || allHeaderIdsOfMasterSheet.isEmpty()) {
                                throw new SkipException("Couldn't get All Header Ids in Master Data Sheet.");
                            }

                            int noOfRowsInMasterDataSheet = XLSUtils.getNoOfRows(templatePath, templateName, "Master Data").intValue();
                            ListRendererFilterData filterDataObj = new ListRendererFilterData();

                            if (parentEntityName.equalsIgnoreCase("suppliers")) {

                                //Verify all Contract Options
                                logger.info("Verifying All Contract Options in Bulk Create Template");
                                filterDataObj.hitListRendererFilterData(ConfigureConstantFields.getListIdForEntity("contracts"));
                                String filterDataResponseForContracts = filterDataObj.getListRendererFilterDataJsonStr();

                                if (ParseJsonResponse.validJsonResponse(filterDataResponseForContracts)) {
                                    String filterName = "supplier";
                                    int filterId = filterDataObj.getFilterId(filterDataResponseForContracts, filterName);

                                    String optionId = ShowHelper.getValueOfField(1, "id", supplierShowResponse);
                                    String optionName = ShowHelper.getValueOfField(1, "name", supplierShowResponse);

                                    String payloadForListData = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":200,\"orderByColumnName\":\"id\"," +
                                            "\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId + "\"," +
                                            "\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + optionId + "\",\"name\":\"" +
                                            optionName + "\"}]}}}}}";

                                    listDataResponse = ListDataHelper.getListDataResponse("contracts", payloadForListData);

                                    JSONObject listDataJsonObj = new JSONObject(listDataResponse);
                                    JSONArray listDataJsonArr = listDataJsonObj.getJSONArray("data");

                                    int contractIdColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                                    List<Integer> allExpectedContractShortCodeIds = new ArrayList<>();

                                    for (int l = 0; l < listDataJsonArr.length(); l++) {
                                        String contractIdValue = listDataJsonArr.getJSONObject(l).getJSONObject(String.valueOf(contractIdColumnNo)).getString("value");
                                        int shortCodeId = ListDataHelper.getShortCodeIdFromValue(contractIdValue);

                                        allExpectedContractShortCodeIds.add(shortCodeId);
                                    }

                                    if (!allHeaderIdsOfMasterSheet.contains("638")) {
                                        throw new SkipException("Couldn't find Header Id 638 in Master Data Sheet.");
                                    }

                                    int contractColumnNo = allHeaderIdsOfMasterSheet.indexOf("638");

                                    List<String> allActualContractOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                            contractColumnNo, 4, noOfRowsInMasterDataSheet);

                                    if (allActualContractOptions == null) {
                                        throw new SkipException("Couldn't get All Contract Options from Master Data Sheet.");
                                    }

                                    int filteredCount = ListDataHelper.getFilteredListDataCount(listDataResponse);

                                    if (filteredCount == allActualContractOptions.size()) {
                                        List<Integer> actualContractShortCodeIds = new ArrayList<>();

                                        for (String contractOption : allActualContractOptions) {
                                            String[] temp = contractOption.split("CO");
                                            String temp2 = temp[1].replace(")", "").trim();

                                            if (temp2.startsWith("0")) {
                                                temp2 = temp2.substring(1);
                                            }

                                            Integer actualShortCodeId = Integer.parseInt(temp2);
                                            actualContractShortCodeIds.add(actualShortCodeId);
                                        }

                                        for (Integer expectedContractId : allExpectedContractShortCodeIds) {
                                            if (!actualContractShortCodeIds.contains(expectedContractId)) {
                                                csAssert.assertTrue(false, "Expected Contract Short Code Id " + expectedContractId +
                                                        " but not found in Master Data Sheet");
                                            }
                                        }
                                    } else {
                                        csAssert.assertTrue(false, "Contract Options Size doesn't match. Expected Contract Options Size: " +
                                                filteredCount + " and Actual Contract Options Size: " + allActualContractOptions.size());
                                    }
                                } else {
                                    csAssert.assertTrue(false, "Filter Data Response for Contracts is an Invalid JSON.");
                                }
                            }

                            //Verify Service Data Options
                            filterDataObj.hitListRendererFilterData(ConfigureConstantFields.getListIdForEntity("service data"));
                            String filterDataResponseForServiceData = filterDataObj.getListRendererFilterDataJsonStr();

                            if (ParseJsonResponse.validJsonResponse(filterDataResponseForServiceData)) {
                                String filterName = parentEntityName.equalsIgnoreCase("suppliers") ? "supplier" : "contract";
                                int filterId = filterDataObj.getFilterId(filterDataResponseForServiceData, filterName);

                                String optionId = ShowHelper.getValueOfField(parentEntityTypeId, "id", parentEntityShowResponse);
                                String optionName = ShowHelper.getValueOfField(parentEntityTypeId, "name", parentEntityShowResponse);

                                String payloadForListData = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":200,\"orderByColumnName\":\"id\"," +
                                        "\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId + "\"," +
                                        "\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + optionId + "\",\"name\":\"" +
                                        optionName + "\"}]}}}}}";

                                listDataResponse = ListDataHelper.getListDataResponse("service data", payloadForListData);

                                JSONObject listDataJsonObj = new JSONObject(listDataResponse);
                                JSONArray listDataJsonArr = listDataJsonObj.getJSONArray("data");

                                int displayNameColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "display_name");

                                List<String> allExpectedServiceDataNames = new ArrayList<>();

                                for (int k = 0; k < listDataJsonArr.length(); k++) {
                                    String serviceDataName = listDataJsonArr.getJSONObject(k).getJSONObject(String.valueOf(displayNameColumnNo)).getString("value");
                                    String[] temp = serviceDataName.split(Pattern.quote("("));

                                    allExpectedServiceDataNames.add(temp[0].trim().toLowerCase());
                                }

                                int serviceDataColumnNo = allHeaderIdsOfMasterSheet.indexOf("11066");

                                List<String> allActualServiceDataOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                        serviceDataColumnNo, 4, noOfRowsInMasterDataSheet);

                                if (allActualServiceDataOptions == null) {
                                    throw new SkipException("Couldn't get All Service Data Options from Master Data Sheet.");
                                }

                                int filteredCount = ListDataHelper.getFilteredListDataCount(listDataResponse);

                                if (filteredCount == allActualServiceDataOptions.size()) {
                                    List<String> actualServiceDataNames = new ArrayList<>();

                                    for (String serviceDataOption : allActualServiceDataOptions) {
                                        String[] temp = serviceDataOption.split(Pattern.quote("("));
                                        String serviceDataName = temp[0].trim();

                                        actualServiceDataNames.add(serviceDataName.toLowerCase());
                                    }

                                    for (String expectedServiceData : allExpectedServiceDataNames) {
                                        if (!actualServiceDataNames.contains(expectedServiceData.toLowerCase())) {
                                            csAssert.assertTrue(false, "Service Data Option " + expectedServiceData +
                                                    " not found in Master Data Sheet ");
                                        }
                                    }
                                } else {
                                    csAssert.assertTrue(false, "Service Data Options Size doesn't match. Expected Service Data Options Size: " +
                                            filteredCount + " and Actual Service Data Options Size: " + allActualServiceDataOptions.size());
                                }

                            } else {
                                csAssert.assertTrue(false, "Filter Data Response for Service Data is an Invalid JSON.");
                            }

                            //Verify Tier
                            logger.info("Verifying All Tier Options in Bulk Create Template");
                            if (allHeaderIdsOfMasterSheet.contains("655")) {
                                String tierHierarchy = ShowHelper.getShowFieldHierarchy("tier", parentEntityTypeId);
                                List<String> allExpectedTiers = new ArrayList<>();

                                if (parentEntityName.equalsIgnoreCase("suppliers")) {
                                    allExpectedTiers = ShowHelper.getAllSelectValuesOfField(parentEntityShowResponse, "tier", tierHierarchy,
                                            parentRecordId, parentEntityTypeId);
                                } else {
                                    String expectedTierValue = ShowHelper.getActualValue(parentEntityShowResponse, tierHierarchy);

                                    if (expectedTierValue != null) {
                                        allExpectedTiers.add(expectedTierValue.toLowerCase());
                                    }
                                }

                                if (allExpectedTiers == null) {
                                    throw new SkipException("Couldn't get All Expected Tiers of Record Id " + parentRecordId + " of Entity " + parentEntityName);
                                }

                                int tierColumnNo = allHeaderIdsOfMasterSheet.indexOf("655");

                                List<String> allActualTierOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                        tierColumnNo, 4, noOfRowsInMasterDataSheet);

                                if (allActualTierOptions == null) {
                                    throw new SkipException("Couldn't get All Tier Options from Master Data Sheet.");
                                }

                                if (allExpectedTiers.size() == allActualTierOptions.size()) {
                                    List<String> actualTierNames = new ArrayList<>();

                                    for (String tierOption : allActualTierOptions) {
                                        String[] temp = tierOption.split(Pattern.quote("::"));
                                        String tierName = temp[temp.length - 1].trim();

                                        actualTierNames.add(tierName.toLowerCase());
                                    }

                                    for (String actualTier : actualTierNames) {
                                        if (!allExpectedTiers.contains(actualTier.toLowerCase())) {
                                            csAssert.assertTrue(false, "Tier Option " + actualTier +
                                                    " found in Master Data Sheet but doesn't belong to Record Id " + parentRecordId + " of Entity " + parentEntityName);
                                        }
                                    }
                                } else {
                                    csAssert.assertTrue(false, "Tier Options Size doesn't match. Expected Tier Options Size: " +
                                            allExpectedTiers.size() + " and Actual Tier Options Size: " + allActualTierOptions.size());
                                }
                            } else {
                                throw new SkipException("Couldn't find Tier Header in Master Data Sheet.");
                            }

                            //Verify Regions
                            logger.info("Verifying All Regions Options in Bulk Create Template");
                            if (allHeaderIdsOfMasterSheet.contains("621")) {
                                String regionsHierarchy = ShowHelper.getShowFieldHierarchy("globalregions", parentEntityTypeId);
                                List<String> allExpectedRegions = ShowHelper.getAllSelectValuesOfField(parentEntityShowResponse, "globalregions",
                                        regionsHierarchy, parentRecordId, parentEntityTypeId);

                                if (allExpectedRegions == null || allExpectedRegions.isEmpty()) {
                                    throw new SkipException("Couldn't get All Expected Regions of Record Id " + parentRecordId + " of Entity " + parentEntityName);
                                }

                                int regionsColumnNo = allHeaderIdsOfMasterSheet.indexOf("621");

                                List<String> allActualRegionsOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                        regionsColumnNo, 4, noOfRowsInMasterDataSheet);

                                if (allActualRegionsOptions == null) {
                                    throw new SkipException("Couldn't get All Regions Options from Master Data Sheet.");
                                }

                                if (allExpectedRegions.size() == allActualRegionsOptions.size()) {
                                    List<String> actualRegionNames = new ArrayList<>();

                                    for (String regionOption : allActualRegionsOptions) {
                                        String[] temp = regionOption.split(Pattern.quote("::"));
                                        String regionName = temp[temp.length - 1].trim();

                                        actualRegionNames.add(regionName.toLowerCase());
                                    }

                                    for (String actualRegion : actualRegionNames) {
                                        if (!allExpectedRegions.contains(actualRegion.toLowerCase())) {
                                            csAssert.assertTrue(false, "Region Option " + actualRegion +
                                                    " found in Master Data Sheet but doesn't belong to Record Id " + parentRecordId + " of Entity " + parentEntityName);
                                        }
                                    }
                                } else {
                                    csAssert.assertTrue(false, "Region Options Size doesn't match. Expected Regions Options Size: " +
                                            allExpectedRegions.size() + " and Actual Region Options Size: " + allActualRegionsOptions.size());
                                }
                            } else {
                                throw new SkipException("Couldn't find Regions Header in Master Data Sheet.");
                            }

                            //Verify Countries
                            logger.info("Verifying All Countries Options in Bulk Create Template");
                            if (allHeaderIdsOfMasterSheet.contains("622")) {
                                String countriesHierarchy = ShowHelper.getShowFieldHierarchy("globalcountries", 1);
                                List<String> allExpectedCountries = ShowHelper.getAllSelectValuesOfField(parentEntityShowResponse, "globalcountries",
                                        countriesHierarchy, parentRecordId, parentEntityTypeId);

                                if (allExpectedCountries == null || allExpectedCountries.isEmpty()) {
                                    throw new SkipException("Couldn't get All Expected Countries of Record Id " + parentRecordId + " of Entity " + parentEntityName);
                                }

                                int countriesColumnNo = allHeaderIdsOfMasterSheet.indexOf("622");

                                List<String> allActualCountriesOptions = XLSUtils.getOneColumnDataFromMultipleRows(templatePath, templateName, "Master Data",
                                        countriesColumnNo, 4, noOfRowsInMasterDataSheet);

                                if (allActualCountriesOptions == null) {
                                    throw new SkipException("Couldn't get All Countries Options from Master Data Sheet.");
                                }

                                if (allExpectedCountries.size() == allActualCountriesOptions.size()) {
                                    List<String> actualCountryNames = new ArrayList<>();

                                    for (String countryOption : allActualCountriesOptions) {
                                        String[] temp = countryOption.split(Pattern.quote("::"));
                                        String countryName = temp[temp.length - 1].trim();

                                        actualCountryNames.add(countryName.toLowerCase());
                                    }

                                    for (String actualCountry : actualCountryNames) {
                                        if (!allExpectedCountries.contains(actualCountry.toLowerCase())) {
                                            csAssert.assertTrue(false, "Countries Option " + actualCountry +
                                                    " found in Master Data Sheet but doesn't belong to Record Id " + parentRecordId + " of Entity " + parentEntityName);
                                        }
                                    }
                                } else {
                                    csAssert.assertTrue(false, "Countries Options Size doesn't match. Expected Countries Options Size: " +
                                            allExpectedCountries.size() + " and Actual Countries Options Size: " + allActualCountriesOptions.size());
                                }
                            } else {
                                throw new SkipException("Couldn't find Countries Header in Master Data Sheet.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Record Id " + serviceDataRecordId + " of Service Data is an Invalid JSON.");
                    }

                    break;
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Service Data is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4212: " + e.getMessage());
        }
        csAssert.assertAll();
    }


    private void validateBulkCreateTemplateColumns(String entityName, int entityTypeId, String parentEntityName, int parentEntityTypeId, int parentRecordId,
                                                   String parentRecordShowResponse, String downloadAPIPath, CustomAssert csAssert) {
        try {
            List<String> allSupplierIds = new ArrayList<>();

            if (parentEntityName.equalsIgnoreCase("suppliers")) {
                String supplierName = ShowHelper.getSupplierNameFromShowResponse(parentRecordShowResponse, 1);
                allSupplierIds.add(supplierName);
            } else {
                allSupplierIds = ShowHelper.getAllSelectValuesOfField(parentRecordShowResponse, "suppliers id",
                        ShowHelper.getShowFieldHierarchy("suppliers id", parentEntityTypeId), parentRecordId, parentEntityTypeId);
            }

            String supplierId = (allSupplierIds != null && allSupplierIds.size() > 0) ? allSupplierIds.get(0) : null;

            logger.info("Hitting Bulk Template Download API using QueryString [{}]", downloadAPIPath);
            Boolean fileDownloaded = BulkTemplate.downloadBulkCreateTemplate(templatePath, templateName, downloadAPIPath);

            if (!fileDownloaded) {
                throw new SkipException("Couldn't download Bulk Create Template of Entity " + entityName + " from Record Id " + parentRecordId +
                        " of Parent Entity " + parentEntityName);
            }

            //Special Handling for Invoice Line Item
            boolean ignoreLineItemInvoiceIdField = false;

            if (entityName.equalsIgnoreCase("invoice line item")) {
                XLSUtils xlsObj = new XLSUtils(templatePath, templateName);
                List<String> allSheetNames = xlsObj.getSheetNames();

                if (allSheetNames == null || allSheetNames.isEmpty()) {
                    throw new SkipException("Couldn't get All Sheet Names of Template located at [" + templatePath + "/" + templateName + "]");
                }

                if (allSheetNames.contains(BulkTemplate.getBulkCreateDataSheetForEntity("invoices"))) {
                    ignoreLineItemInvoiceIdField = true;
                }
            }

            List<Map<String, String>> allExpectedBulkCreateFields = getAllExpectedBulkCreateFields(entityTypeId, supplierId, 1002, ignoreLineItemInvoiceIdField);

            if (allExpectedBulkCreateFields == null || allExpectedBulkCreateFields.isEmpty()) {
                throw new SkipException("Couldn't get All Bulk Create Fields for Entity " + entityName);
            }

            String dataSheetName = BulkTemplate.getBulkCreateDataSheetForEntity(entityName);

            List<Map<String, String>> allFieldsInBulkCreateTemplate = bulkHelperObj.getAllFieldsInBulkCreateTemplate(templatePath, templateName, dataSheetName);

            List<Map<String, String>> allActualFieldsInBulkCreateTemplate = new ArrayList<>(allExpectedBulkCreateFields);

            for (Map<String, String> actualFieldMap : allFieldsInBulkCreateTemplate) {
                String fieldId = actualFieldMap.get("id");

                //Ignoring Fields SL No, Process and Invoice SL No (for Line Item)
                if (fieldId.equalsIgnoreCase("100000001") || fieldId.equalsIgnoreCase("100000002") ||
                        fieldId.equalsIgnoreCase("100000003")) {
                    allActualFieldsInBulkCreateTemplate.remove(actualFieldMap);
                    continue;
                }

                String fieldLabel = actualFieldMap.get("label");

                boolean fieldFound = false;

                for (Map<String, String> expectedFieldMap : allExpectedBulkCreateFields) {
                    String expectedFieldId = expectedFieldMap.get("id");

                    if (expectedFieldId.equalsIgnoreCase(fieldId)) {
                        fieldFound = true;
                        break;
                    }
                }

                csAssert.assertTrue(fieldFound, "Field [" + fieldLabel + "] having Id " + fieldId +
                        " is present in Bulk Create Template whereas not enabled in Field Provisioning.");
            }

            for (Map<String, String> expectedFieldMap : allExpectedBulkCreateFields) {
                String expectedFieldId = expectedFieldMap.get("id");
                String fieldName = expectedFieldMap.get("fieldName");

                boolean fieldFound = false;

                for (Map<String, String> actualFieldMap : allFieldsInBulkCreateTemplate) {
                    String fieldId = actualFieldMap.get("id");

                    if (fieldId.equalsIgnoreCase(expectedFieldId)) {
                        fieldFound = true;
                        break;
                    }
                }

                csAssert.assertTrue(fieldFound, "Field [" + fieldName + "] having Id " + expectedFieldId +
                        " is not present in Bulk Create Template whereas enabled in Field Provisioning for Entity " + entityName);
            }

            if (allActualFieldsInBulkCreateTemplate.size() != allExpectedBulkCreateFields.size()) {
                csAssert.assertTrue(false, "Total No of Fields Present in Bulk Create Template is: " +
                        allActualFieldsInBulkCreateTemplate.size() + " and No of Fields Expected is: " + allExpectedBulkCreateFields.size() + " for Entity " + entityName);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Bulk Create Template Columns for Entity " + entityName + ". " + e.getMessage());
        }
    }

    private List<Map<String, String>> getAllExpectedBulkCreateFields(int entityTypeId, String supplierId, int clientId, boolean ignoreLineItemInvoiceIdField) {
        List<Map<String, String>> allExpectedBulkCreateFields;
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            adminHelperObj.loginWithClientAdminUser();

            Boolean supplierSpecificFieldProvisioningPresent = fieldProvisioningObj.fieldProvisioningPresentForSupplierOfEntity(supplierId,
                    entityTypeId, 1002);

            if (supplierSpecificFieldProvisioningPresent == null) {
                logger.error("Couldn't get Whether Field Provisioning is Present for Supplier Id " + supplierId + " and EntityTypeId " +
                        entityTypeId + " or not");
                return null;
            }

            String fieldProvisioningResponse = supplierSpecificFieldProvisioningPresent ?
                    fieldProvisioningObj.hitFieldProvisioning(entityTypeId, Integer.parseInt(supplierId)) :
                    fieldProvisioningObj.hitFieldProvisioning(entityTypeId);

            allExpectedBulkCreateFields = fieldProvisioningObj.getAllBulkCreateFields(fieldProvisioningResponse);

            if (allExpectedBulkCreateFields == null) {
                logger.error("Couldn't get All Bulk Create Fields from Field Provisioning Response for EntityTypeId {}", entityTypeId);
                return null;
            }

            String fieldsResponse = provisioningObj.hitFieldProvisioning(2, entityTypeId);
            List<Map<String, String>> allHiddenFields = provisioningObj.getAllHiddenFields(fieldsResponse);

            //Removing User Level Disabled Fields from Expected Bulk Create Fields.
            if (allHiddenFields != null && !allHiddenFields.isEmpty()) {
                for (Map<String, String> hiddenFieldMap : allHiddenFields) {
                    String entityFieldId = hiddenFieldMap.get("entityFieldId");

                    for (Map<String, String> bulkCreateFieldMap : allExpectedBulkCreateFields) {
                        String id = bulkCreateFieldMap.get("id");

                        if (entityFieldId.equals(id)) {
                            allExpectedBulkCreateFields.remove(bulkCreateFieldMap);
                            break;
                        }
                    }
                }
            }

            //Special Handling for Invoice Line Item Field [Invoice Id]
            if (ignoreLineItemInvoiceIdField) {
                for (Map<String, String> fieldMap : allExpectedBulkCreateFields) {
                    String fieldId = fieldMap.get("id");

                    if (fieldId.equalsIgnoreCase("11102")) {
                        allExpectedBulkCreateFields.remove(fieldMap);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Expected Bulk Create Fields for EntityTypeId {}, Supplier Id {} and Client Id {}. {}", entityTypeId,
                    supplierId, clientId, e.getMessage());
            return null;
        } finally {
            new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
        }

        return allExpectedBulkCreateFields;
    }

}