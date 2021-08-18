package com.sirionlabs.test.FM;

import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;

import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

import static com.sirionlabs.helper.invoice.InvoiceHelper.*;

public class Test_RC1_36_Bugs {
    private final static Logger logger = LoggerFactory.getLogger(Test_RC1_36_Bugs.class);

    private String configFilePath;
    private String configFileName;

    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;

    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;

    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;
    private String invoiceExtraFieldsConfigFileName;

    private String invoiceLineItemConfigFilePath;
    private String invoiceLineItemConfigFileName;
    private String invoiceLineItemExtraFieldsConfigFileName;

    private String invoiceFlowsFilePath;
    private String invoiceFlowsFileName;

    private Boolean killAllSchedulerTasks = false;
    private String pricingTemplateFilePath;
    private String ARCRRCSheetNameInXLSXFile = "ARCRRC";

    private String publishAction = "publish";
    private String approveAction = "approve";
    private String Approve = "Approve";
    private String calculateMemo = "Calculatememo";

    private Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private String serviceDataEntity = "service data";
    private String consumptionEntity = "consumptions";
    private String invoicesEntity = "invoices";
    private String invoiceLineItemEntity = "invoice line item";
    private String serviceDataEntitySectionUrlName;
    private String consumptionEntitySectionUrlName;

    private int serviceDataEntityTypeId;
    private int consumptionEntityTypeId;
    private int invoiceEntityTypeId;
    private int invoiceLineItemEntityTypeId;

    private int serviceDataForBillingReport;

    private HashMap<String, Integer> entityToBeDeletedMap = new HashMap<>();
    private String changeRequestId = "CR06379";
    private String changeRequestName = "Change Request Name";

    @BeforeClass
    public void BeforeClass() {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("RC136FMConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("RC136FMConfigFileName");

        //Contract Config files
        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        //Invoice Config files
        invoiceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
        invoiceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");
        invoiceExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceExtraFieldsFileName");

        //Invoice Line Item Config files
        invoiceLineItemConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFilePath");
        invoiceLineItemConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFileName");
        invoiceLineItemExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("invoiceLineItemExtraFieldsFileName");

        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");

        invoiceFlowsFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoiceflowsconfigfilepath");
        invoiceFlowsFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoiceflowsconfigfilename");

        String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
        consumptionEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "url_name");

        consumptionEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "entity_type_id"));
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));
        invoiceEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoicesEntity, "entity_type_id"));
        invoiceLineItemEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "entity_type_id"));

    }

    @Test()
    public void Test_SIR_218791_Checking_DuplicateLineItemIDInvoiceWithAdjustmentYes() {

        CustomAssert customAssert = new CustomAssert();

        try {
            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

            String flowToTest = "arc flow 1";
            String serviceDataType = "";
            String cloneResponse;
            String createResponse;

            String lineItemTypeWithAdjustmentYes = "Adjustment ARC";
            String lineItemTypeWithAdjustmentYesID = "1090";
            Clone clone = new Clone();
            Create create = new Create();

            JSONObject createPayload;

            InvoicePricingHelper pricingObj = new InvoicePricingHelper();
            InvoiceHelper invoiceHelper = new InvoiceHelper();
            ArrayList<Integer> consumptionIds = new ArrayList<>();
            int invoiceLineItemIdWithAdjustmentNo;
            int lineItemIdWithAdjustmentYes = -1;
            int clonedLineItemIdWithAdjustmentNo = -1;

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flowToTest);

            entityToBeDeletedMap.put("contracts", contractId);

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName,
                    flowToTest, contractId);
            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId);

            serviceDataForBillingReport = serviceDataId;

            entityToBeDeletedMap.put("service data", serviceDataId);

            if (killAllSchedulerTasks) {
                logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                killAllSchedulerTasks();
            }

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            Boolean uploadPricing = true;
            String temp = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest, "uploadPricing");
            if (temp != null && temp.trim().equalsIgnoreCase("false"))
                uploadPricing = false;


            if (uploadPricing) {
                String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                        "pricingstemplatefilename");
                Boolean pricingFile = pricingObj.downloadAndEditPricingFile(pricingTemplateFilePath, pricingTemplateFileName, invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest, serviceDataId, pricingObj);

                // changes for ARC RRC FLOW
                if (pricingFile) {


                    // getting the actual service data Type
                    if (ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                            "servicedatatype") != null) {
                        serviceDataType = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                                "servicedatatype");
                    }


                    if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                        Map<Integer, Map<Integer, Object>> arcValuesMap = getValuesMapForArcRrcSheet(flowToTest);

                        pricingFile = pricingObj.editPricingFileForARCRRC(pricingTemplateFilePath, pricingTemplateFileName, flowToTest, ARCRRCSheetNameInXLSXFile, arcValuesMap, serviceDataId);

                    }
                    // changes for ARC RRC FLOW Ends here

                    if (pricingFile) {

                        XLSUtils.updateColumnValue(pricingTemplateFilePath,pricingTemplateFileName,"Data",6,9,"(" + changeRequestId + ") " + changeRequestName);
                        XLSUtils.updateColumnValue(pricingTemplateFilePath,pricingTemplateFileName,"Data",6,8,"test");

                        String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                        if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {

                            //Wait for Pricing Scheduler to Complete
                            String pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);

                            if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                        flowToTest + "]");

                                customAssert.assertAll();
                                return;
                            } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                                logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
                                if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                                    logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                                    customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                            "Hence failing Flow [" + flowToTest + "]");
                                } else {
                                    logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow [{}]", flowToTest);
                                }

                                customAssert.assertAll();
                            } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                            {

                                boolean isDataCreatedUnderChargesTab = invoiceHelper.isChargesCreated(serviceDataId);

                                if (!isDataCreatedUnderChargesTab) {
                                    customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                            flowToTest + "]");
                                    customAssert.assertAll();
                                }


                                if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                    boolean isDataCreatedUnderARCRRCTab = invoiceHelper.isARCRRCCreated(serviceDataId);

                                    if (!isDataCreatedUnderARCRRCTab) {
                                        customAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                                flowToTest + "]");

                                        customAssert.assertAll();
                                    }
                                }
                            }

                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                            {
                                boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, serviceDataId, publishAction);

                                // if service data got published
                                if (result == true) {

                                    // function to get status whether consumptions have been created or not
                                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId);
                                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);

                                        customAssert.assertAll();

                                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);

                                        customAssert.assertAll();
                                        throw new SkipException("Skipping this test");
                                    }

                                    // after consumptions have been created successfully
                                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                                            "finalconsumptionvalues").trim().split(Pattern.quote(","));

                                    consumptionIds = invoiceHelper.consumptionIds;
                                    for (int i = 0; i < consumptionIds.size(); i++) {

                                        result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                                        if (result == false) {
                                            logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
                                            customAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                                    "Hence skipping validation");

                                            customAssert.assertAll();

                                        } else {

                                            result = workflowActionsHelper.performWorkflowAction(consumptionEntityTypeId, consumptionIds.get(i), approveAction);
                                            customAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptionEntity + "having id : " + consumptionIds.get(i));

                                            if (result == false) {
                                                logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
                                                customAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
                                                        "Hence skipping validation");

                                                customAssert.assertAll();
                                            }
                                        }
                                    }
                                } else {

                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");

                                    customAssert.assertAll();
                                }
                            }

                        } else {
                            logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                    pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);

                            customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                    pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");

                            customAssert.assertAll();
                        }
                    } else {
                        logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                        customAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                "Hence skipping validation");


                        customAssert.assertAll();

                    }

                } else {
                    logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");

                    customAssert.assertAll();
                }
            } else {
                logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
            }


            int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName,
                    invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest);

            entityToBeDeletedMap.put("invoices", invoiceId);

            logger.info("Created Invoice Id is : [{}]", invoiceId);
            if (invoiceId != -1) {

                //Get Invoice Line Item Id
                invoiceLineItemIdWithAdjustmentNo = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId);
                logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemIdWithAdjustmentNo);
                InvoiceHelper.verifyInvoiceLineItem(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest, invoiceLineItemIdWithAdjustmentNo, customAssert);

                if (!workflowActionsHelper.performWorkflowAction(invoiceLineItemEntityTypeId, invoiceLineItemIdWithAdjustmentNo, Approve)) {
                    customAssert.assertTrue(false, "Unable to perform workflowAction " + Approve);
                } else {
                    if (!workflowActionsHelper.performWorkflowAction(invoiceLineItemEntityTypeId, invoiceLineItemIdWithAdjustmentNo, calculateMemo)) {
                        customAssert.assertTrue(false, "Unable to perform workflowAction " + calculateMemo);
                    } else {

                        cloneResponse = clone.hitClone(invoiceLineItemEntity, invoiceLineItemIdWithAdjustmentNo);

                        createPayload = getPayloadForCreateAfterClone(cloneResponse);
                        JSONObject createPayloadForLineItemTypeWithAdjustmentYes = new JSONObject(createPayload.toString());

                        createPayloadForLineItemTypeWithAdjustmentYes.getJSONObject("body").getJSONObject("data").getJSONObject("lineItemType").getJSONObject("values").put("name", lineItemTypeWithAdjustmentYes);
                        createPayloadForLineItemTypeWithAdjustmentYes.getJSONObject("body").getJSONObject("data").getJSONObject("lineItemType").getJSONObject("values").put("id", lineItemTypeWithAdjustmentYesID);
                        create.hitCreate(invoiceLineItemEntity, createPayloadForLineItemTypeWithAdjustmentYes.toString());
                        createResponse = create.getCreateJsonStr();

                        if (!createResponse.contains("success")) {
                            customAssert.assertTrue(false, "Unable to create the clone of line item " + invoiceLineItemIdWithAdjustmentNo);
                        } else {
                            lineItemIdWithAdjustmentYes = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);
                        }

                        create.hitCreate(invoiceLineItemEntity, createPayload.toString());
                        createResponse = create.getCreateJsonStr();

                        if (!createResponse.contains("success")) {
                            customAssert.assertTrue(false, "Unable to create the clone of line item " + invoiceLineItemIdWithAdjustmentNo);
                        } else {
                            clonedLineItemIdWithAdjustmentNo = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);
                        }

                        TabListData tabListData = new TabListData();
                        int tabId = 341;
                        String tabListPayload = "{\"filterMap\":{\"entityTypeId\":null,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
                        String tabListResponse = tabListData.hitTabListData(tabId, invoiceLineItemEntityTypeId, clonedLineItemIdWithAdjustmentNo, tabListPayload);

                        JSONObject tabListResponseJson = new JSONObject(tabListResponse);
                        JSONArray dataArray = tabListResponseJson.getJSONArray("data");

                        if (dataArray.length() == 0) {
                            customAssert.assertTrue(false, "No row is created after duplicate line item ");
                        } else if (dataArray.length() > 1) {
                            customAssert.assertTrue(false, "Only one row expected for the invoice line item");
                        } else {
                            JSONObject individualLineJson = dataArray.getJSONObject(0);
                            JSONArray individualLineJsonArray = JSONUtility.convertJsonOnjectToJsonArray(individualLineJson);
                            JSONObject individualColumnLineJson;
                            String columnName;
                            String columnValue;
                            for (int i = 0; i < individualLineJsonArray.length(); i++) {
                                individualColumnLineJson = individualLineJsonArray.getJSONObject(i);
                                columnName = individualColumnLineJson.get("columnName").toString();
                                if (columnName.equalsIgnoreCase("invoiceid")) {
                                    columnValue = individualColumnLineJson.get("value").toString().split(":;")[1];

                                    if (!columnValue.equalsIgnoreCase(String.valueOf(invoiceId))) {

                                        customAssert.assertTrue(false, "Expected value for invoice id during tab open for duplicate line item id " + invoiceId + " Actual value for invoice id during tab open for duplicate line item id " + columnValue);
                                    }
                                }

                                if (columnName.equalsIgnoreCase("lineitemid")) {
                                    columnValue = individualColumnLineJson.get("value").toString().split(":;")[1];

                                    if (!columnValue.equalsIgnoreCase(String.valueOf(invoiceLineItemIdWithAdjustmentNo))) {

                                        customAssert.assertTrue(false, "Expected value for invoice line item id during tab open for duplicate line item id " + invoiceId + " Actual value for invoice line item id during tab open for duplicate line item id " + columnValue);
                                    }
                                }
                            }
                        }
                    }
                }


            } else {
                logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);

                customAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validation " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test()
    public void Test_SIR__218791_Checking_ReValidationOfDuplicateLineItem() {

        CustomAssert customAssert = new CustomAssert();

        try {
            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

            String flowToTest = "arc flow 1";
            String serviceDataType = "";
            String cloneResponse;
            String createResponse;

            Clone clone = new Clone();
            Create create = new Create();

            JSONObject createPayload;

            InvoicePricingHelper pricingObj = new InvoicePricingHelper();
            InvoiceHelper invoiceHelper = new InvoiceHelper();
            ArrayList<Integer> consumptionIds = new ArrayList<>();
            int invoiceLineTobeDeleted;
            int clonedLineItemIdWithDuplicateStatus;

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flowToTest);

            entityToBeDeletedMap.put("contracts", contractId);

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName,
                    flowToTest, contractId);
            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId);

            entityToBeDeletedMap.put("service data", serviceDataId);

            if (killAllSchedulerTasks) {
                logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                killAllSchedulerTasks();
            }

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            Boolean uploadPricing = true;
            String temp = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest, "uploadPricing");
            if (temp != null && temp.trim().equalsIgnoreCase("false"))
                uploadPricing = false;


            if (uploadPricing) {
                String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                        "pricingstemplatefilename");
                Boolean pricingFile = pricingObj.downloadAndEditPricingFile(pricingTemplateFilePath, pricingTemplateFileName, invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest, serviceDataId, pricingObj);

                // changes for ARC RRC FLOW
                if (pricingFile) {

                    // getting the actual service data Type
                    if (ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                            "servicedatatype") != null) {
                        serviceDataType = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                                "servicedatatype");
                    }


                    if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                        Map<Integer, Map<Integer, Object>> arcValuesMap = getValuesMapForArcRrcSheet(flowToTest);

                        pricingFile = pricingObj.editPricingFileForARCRRC(pricingTemplateFilePath, pricingTemplateFileName, flowToTest, ARCRRCSheetNameInXLSXFile, arcValuesMap, serviceDataId);

                    }
                    // changes for ARC RRC FLOW Ends here

                    if (pricingFile) {


                        String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                        if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {

                            //Wait for Pricing Scheduler to Complete
                            String pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);

                            if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                        flowToTest + "]");

                                customAssert.assertAll();
                                return;
                            } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                                logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
                                if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                                    logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                                    customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                            "Hence failing Flow [" + flowToTest + "]");
                                } else {
                                    logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow [{}]", flowToTest);
                                }

                                customAssert.assertAll();
                            } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                            {

                                boolean isDataCreatedUnderChargesTab = invoiceHelper.isChargesCreated(serviceDataId);

                                if (!isDataCreatedUnderChargesTab) {
                                    customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                            flowToTest + "]");
                                    customAssert.assertAll();
                                }


                                if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                    boolean isDataCreatedUnderARCRRCTab = invoiceHelper.isARCRRCCreated(serviceDataId);

                                    if (!isDataCreatedUnderARCRRCTab) {
                                        customAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                                flowToTest + "]");

                                        customAssert.assertAll();
                                    }
                                }
                            }

                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                            {
                                boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, serviceDataId, publishAction);

                                // if service data got published
                                if (result == true) {

                                    // function to get status whether consumptions have been created or not
                                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId);
                                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);

                                        customAssert.assertAll();

                                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);

                                        customAssert.assertAll();
                                        throw new SkipException("Skipping this test");
                                    }

                                    // after consumptions have been created successfully
                                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                                            "finalconsumptionvalues").trim().split(Pattern.quote(","));

                                    consumptionIds = invoiceHelper.consumptionIds;
                                    for (int i = 0; i < consumptionIds.size(); i++) {

                                        result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                                        if (result == false) {
                                            logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
                                            customAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                                    "Hence skipping validation");

                                            customAssert.assertAll();

                                        } else {

                                            result = workflowActionsHelper.performWorkflowAction(consumptionEntityTypeId, consumptionIds.get(i), approveAction);
                                            customAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptionEntity + "having id : " + consumptionIds.get(i));

                                            if (result == false) {
                                                logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
                                                customAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
                                                        "Hence skipping validation");

                                                customAssert.assertAll();
                                            }
                                        }
                                    }
                                } else {

                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");

                                    customAssert.assertAll();
                                }
                            }

                        } else {
                            logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                    pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);

                            customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                    pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");

                            customAssert.assertAll();
                        }
                    } else {
                        logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                        customAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                "Hence skipping validation");


                        customAssert.assertAll();

                    }

                } else {
                    logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");

                    customAssert.assertAll();
                }
            } else {
                logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
            }

            int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName,
                    invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest);

            entityToBeDeletedMap.put("invoices", invoiceId);

            logger.info("Created Invoice Id is : [{}]", invoiceId);
            if (invoiceId != -1) {

                //Get Invoice Line Item Id
                invoiceLineTobeDeleted = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId);
                logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineTobeDeleted);
                InvoiceHelper.verifyInvoiceLineItem(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest, invoiceLineTobeDeleted, customAssert);

                if (!workflowActionsHelper.performWorkflowAction(invoiceLineItemEntityTypeId, invoiceLineTobeDeleted, Approve)) {
                    customAssert.assertTrue(false, "Unable to perform workflowAction " + Approve);
                } else {

                    cloneResponse = clone.hitClone(invoiceLineItemEntity, invoiceLineTobeDeleted);

                    createPayload = getPayloadForCreateAfterClone(cloneResponse);

                    create.hitCreate(invoiceLineItemEntity, createPayload.toString());
                    createResponse = create.getCreateJsonStr();

                    clonedLineItemIdWithDuplicateStatus = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

                    InvoiceHelper.verifyInvoiceLineItemValidationStatus("Duplicate Line Item",clonedLineItemIdWithDuplicateStatus,customAssert);

                    EntityOperationsHelper.deleteEntityRecord(invoiceLineItemEntity,invoiceLineTobeDeleted);

                    InvoiceHelper.verifyInvoiceLineItem(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest, clonedLineItemIdWithDuplicateStatus, customAssert);

                }
            } else {
                logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);

                customAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validation " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(dependsOnMethods = "Test_SIR_218791_Checking_DuplicateLineItemIDInvoiceWithAdjustmentYes")
    public void Test_SIR_218556_CheckingBillingFileReportWhenPricingSheetUploadWithCCR() {

        CustomAssert customAssert = new CustomAssert();

        try{

            int billingReportListID = 444;
            String serviceDataName = ShowHelper.getValueOfField(serviceDataEntityTypeId,serviceDataForBillingReport,"serviceidsupplier");
            String reportListPayload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{\"248\":{\"filterId\":\"248\",\"filterName\":\"serviceData\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + serviceDataForBillingReport + "\",\"name\":\"" + serviceDataName + "\"}]}}}}}";

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(billingReportListID,reportListPayload);
            String reportRendererListDataResponse =  reportRendererListData.getListDataJsonStr();

            JSONObject indDataJson = new JSONObject(reportRendererListDataResponse).getJSONArray("data").getJSONObject(0);
            JSONArray indRowArray = JSONUtility.convertJsonOnjectToJsonArray(indDataJson);
            String columnName;
            String columnValue;

            for(int i =0;i<indRowArray.length();i++){

                columnName = indRowArray.getJSONObject(i).get("columnName").toString();

                if(columnName.equalsIgnoreCase("changerequest")){

                    columnValue = indRowArray.getJSONObject(i).get("value").toString();

                    if(!columnValue.equalsIgnoreCase(changeRequestName)){
                        customAssert.assertTrue(false,"Change Request Name validated unsuccessfully on Billing Report for service Data Id " + serviceDataForBillingReport);
                    }
                    break;
                }

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while Checking Billing File Report When Pricing Sheet Upload With CCR " + e.getMessage());
        }

        customAssert.assertAll();
    }

    private void killAllSchedulerTasks() {
        UserTasksHelper.removeAllTasks();
    }

    // this will create the row , <columnNumber,value> for editing the ARC/RRC Sheet
    private Map<Integer, Map<Integer, Object>> getValuesMapForArcRrcSheet(String flowToTest) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {


            int numberOfColumnToEditForEachRow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                    "numberofcolumntoeditforeachrowforarc"));

            String[] arcRowNumber = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                    "arcrownumber").trim().split(Pattern.quote(","));

            String[] arcColumnNumber = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                    "arccolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(invoiceFlowsFilePath, invoiceFlowsFileName, flowToTest,
                    "arcvalue").trim().split(Pattern.quote(","));

            for (int i = 0; i < arcRowNumber.length; i++) {

                Map<Integer, Object> innerValuesMap = new HashMap<>();
                for (int j = 0; j < numberOfColumnToEditForEachRow; j++) {
                    innerValuesMap.put(Integer.parseInt(arcColumnNumber[i * numberOfColumnToEditForEachRow + j]), values[i * numberOfColumnToEditForEachRow + j]);
                }
                valuesMap.put(Integer.parseInt(arcRowNumber[i]), innerValuesMap);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Pricing Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    private JSONObject getPayloadForCreateAfterClone(String cloneResponse) {

        JSONObject createPayload = new JSONObject(cloneResponse);

        createPayload.remove("header");
        createPayload.remove("session");
        createPayload.remove("actions");
        createPayload.remove("createLinks");

        createPayload.getJSONObject("body").remove("layoutInfo");
        createPayload.getJSONObject("body").remove("globalData");
        createPayload.getJSONObject("body").remove("errors");


        return createPayload;

    }

    @AfterMethod
    public void AfterClass() {

        for (Map.Entry<String, Integer> entityTobeDeleted : entityToBeDeletedMap.entrySet()) {
            EntityOperationsHelper.deleteEntityRecord(entityTobeDeleted.getKey(),entityTobeDeleted.getValue());
        }
    }
}
