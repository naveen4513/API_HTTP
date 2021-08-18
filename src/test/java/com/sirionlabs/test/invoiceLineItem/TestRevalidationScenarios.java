package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.config.UpdateConfigFiles;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.test.CustomerDataCreation.DocumentTreeDownLoad;
import com.sirionlabs.test.invoice.InvoiceValidationStatusForInvoice;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.kafka.common.protocol.types.Field;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestRevalidationScenarios {

    private final static Logger logger = LoggerFactory.getLogger(TestRevalidationScenarios.class);

    private String configFilePath;
    private String configFileName;

    private int contractId;

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

    private String invoiceFlowsConfigFilePath;
    private String invoiceFlowsConfigFileName;

    private String pricingTemplateFilePath;

    private String publishAction = "publish";
    private String approve = "Approve";
    private String approveInvoice = "ApproveInvoice";
    private String calculateMemo = "Calculatememo";

    private String invoiceLineItem = "invoice line item";

    private int serviceDataEntityTypeId = 64;
    private int invoiceEntityTypeId = 67;
    private int invoiceLineItemEntityTypeId = 165;
    private int consumptionsEntityTypeId = 176;

    private String filePath;

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestRevalidationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestRevalidationConfigFileName");

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

        invoiceFlowsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoiceflowsconfigfilepath");
        invoiceFlowsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoiceflowsconfigfilename");

        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"pricingtemplatefilepath");

        filePath = "src\\test\\resources\\TestConfig\\InvoiceLineItem\\Revalidation";

        contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contractid"));
    }

    @DataProvider(name = "consumptionOverideFlows", parallel = false)
    public Object[][] consumptionOverideFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"consumption_overide_flows", "flows to test").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(name = "changesinlineitemscenarios", parallel = false)
    public Object[][] changesInLineItemScenarios() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"changes_in_lineitem_scenarios", "flows to test").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    //    C10305 Validated on 21 April
    @Test(enabled = true)
    public void TestLineItemValidationAfterForecastOverride(){

        CustomAssert customAssert = new CustomAssert();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        String flowToTest = "forecast flow 1";
        try{

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,flowToTest);

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,uniqueDataString);

            if(serviceDataId != -1){

                String forecastTemplateFilePath = "src\\test\\resources\\TestConfig\\InvoiceLineItem\\Revalidation";
                String forecastTemplateFileName = "Forecast_Template_Sheet.xlsx";


                boolean editAndUploadForecast = invoiceHelper.editAndUploadForecastSheet(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,forecastTemplateFilePath, forecastTemplateFileName, flowToTest, "newClient" + contractId, contractId);


                if (!editAndUploadForecast) {
                    logger.error("Edit and Upload Forecast sheet is failing ", flowToTest);
                    customAssert.assertTrue(false,"Edit and Upload Forecast sheet is failing ");
                }

                int invoiceId= InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName, flowToTest);

                int invoiceLineItemId= InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,
                        flowToTest,serviceDataId,invoiceId);

                editAndUploadForecast = invoiceHelper.editAndUploadForecastSheet(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,forecastTemplateFilePath, forecastTemplateFileName, flowToTest, "newClient" + contractId, contractId);


                if (!editAndUploadForecast) {
                    logger.error("Edit and Upload Forecast sheet is failing ", flowToTest);
                    customAssert.assertTrue(false,"Edit and Upload Forecast sheet is failing second time");
                }else {
                    Boolean revalidateScenario = revalidateScenario(invoiceId,customAssert);

                    if(!revalidateScenario){
                        customAssert.assertTrue(false,"Revalidate scenario validated unsuccessfully");
                    }
                }

                //Revalidate logic to be implemented


            }else {
                logger.error("Error creating Service Data");
                customAssert.assertTrue(false,"Error creating Service Data");
            }


        }catch (Exception e){
            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();
    }

    //    C10302 Validated 20 April
    @Test(dataProvider = "consumptionOverideFlows",enabled = true)
    public void TestLineItemValidationAfterConsumptionOverride(String scenario){

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "arc flow 1";

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        Long schedulerJobTimeOut = Long.valueOf(300000);
        Long schedulerJobPollingTime = Long.valueOf(5000);

        int invoiceId = -1;
        int invoiceLineItemId;

        ArrayList<Integer> consumptionIds = new ArrayList<>();

        try {

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,contractId,uniqueDataString);

            if(serviceDataId != -1) {

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                        flowToTest, serviceDataId,
                        pricingTemplateFilePath, false,
                        "", -1, null,
                        customAssert);

                if (!uploadPricing) {

                    customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);
                    customAssert.assertAll();
                }


                boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                // if service data got published
                if (result == true) {


                    // function to get status whether consumptions have been created or not
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    }

                    // after consumptions have been created successfully
                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
                            "finalconsumptionvalues").trim().split(Pattern.quote(","));


                } else {

                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");
                }

                invoiceId = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,flowToTest);

                invoiceLineItemId = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,flowToTest,serviceDataId,invoiceId);

                int consumptionId = consumptionIds.get(0);
                Double finalConsumption = 110.0;

                if(scenario.equalsIgnoreCase("bulk scenario")){

                    Download download = new Download();
                    int templateId = 1014;
                    String entityIds = String.valueOf(consumptionId);
                    String bulkUpdateConsumptionFileName = "BulkUpdateConsumption.xlsx";
                    String sheetName = "Consumption";

                    int rowNo = 6;
                    int colNo = 31;
                    int finalConsumptionValue = 151;

                    Boolean downStatus = download.hitDownload(filePath,bulkUpdateConsumptionFileName,templateId,consumptionsEntityTypeId,entityIds);

                    if(!downStatus){
                        customAssert.assertTrue(false,"Bulk Update Template Downloaded unsuccessfully for consumptions");
                        customAssert.assertAll();
                    }else {
                        XLSUtils.updateColumnValue(filePath,bulkUpdateConsumptionFileName,sheetName,rowNo,colNo,finalConsumptionValue);

                        Map<String, String> payloadMap = new HashMap<>();
                        payloadMap.put("_csrf_token", "null");
                        payloadMap.put("entityTypeId", String.valueOf(consumptionsEntityTypeId));
                        payloadMap.put("upload", "Submit");

                        Fetch fetchObj = new Fetch();
                        fetchObj.hitFetch();
                        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                        UploadBulkData uploadObj = new UploadBulkData();
                        uploadObj.hitUploadBulkData(consumptionsEntityTypeId, templateId, filePath, bulkUpdateConsumptionFileName, payloadMap);

                        String bulkUpdateResponse = uploadObj.getUploadBulkDataJsonStr();
                        if(bulkUpdateResponse.contains("200")){
                            logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");

                            fetchObj.hitFetch();
                            logger.info("Getting Task Id of Bulk Create Job");
                            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
                            Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                            if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                                customAssert.assertTrue(true,"Bulk update done successfully");
                            }else {
                                customAssert.assertTrue(false,"Bulk update done unsuccessfully");
                            }

                        }else {
                            customAssert.assertTrue(false,"Error while uploading Bulk Update file");
                        }

                    }

                }else if(scenario.equalsIgnoreCase("normal scenario")){
                    Boolean updateConsumptionStatus = updateConsumption(consumptionId, finalConsumption, customAssert);

                    if (!updateConsumptionStatus) {
                        logger.error("Consumption update done unsuccessfully");
                        customAssert.assertTrue(false, "Consumption update done unsuccessfully");
                    }
                }
                String invoiceWorkFlowResponse = Actions.getActionsV3Response(invoiceEntityTypeId,invoiceId);
                Boolean revalidationButtonFound = false;
                String expectedButtonName = "Revalidate";
                String revalidateUrl = null;

                if(JSONUtility.validjson(invoiceWorkFlowResponse)){

                    JSONObject invoiceWorkFlowResponseJson = new JSONObject(invoiceWorkFlowResponse);
                    JSONArray layoutActionsArray = invoiceWorkFlowResponseJson.getJSONArray("layoutActions");

                    for(int i = 0;i<layoutActionsArray.length();i++){

                        if(layoutActionsArray.getJSONObject(i).get("name").toString().equals(expectedButtonName)){
                            revalidateUrl = layoutActionsArray.getJSONObject(i).get("api").toString();
                            logger.info(expectedButtonName +  " Button found on invoice show page");
                            revalidationButtonFound = true;
                            break;
                        }
                    }

                    if(!revalidationButtonFound){
                        customAssert.assertTrue(false,expectedButtonName +  " Button not found on invoice show page");
                    }else {
                        String payload = "{\"entityId\":" + invoiceId + "}";
                        String response = Actions.hitActionApi(revalidateUrl,payload);

                        if(!response.contains("success")){
                            customAssert.assertTrue(false,"Error while revalidating the invoice");
                        }
                    }

                }else {
                    logger.error("invoice WorkFlowResponse is an invalid json");
                    customAssert.assertTrue(false,"invoice WorkFlowResponse is an invalid json");
                }


            }else {
                logger.error("ServiceData Not Created");
                customAssert.assertTrue(false,"ServiceData Not Created");
            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }


        customAssert.assertAll();
    }

    //    C10662 Test case not completed
    @Test(dataProvider = "changesinlineitemscenarios",enabled = false)
    public void TestReValidationAfterChangesInLineItem(String scenario){

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "arc flow 1";

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        EntityEditHelper entityEditHelper = new EntityEditHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        Boolean reValidationStarted;

        int invoiceId = -1;
        int invoiceLineItemId;

        ArrayList<Integer> consumptionIds = new ArrayList<>();

        try{

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,contractId,uniqueDataString);

            if(serviceDataId != -1) {

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                        flowToTest, serviceDataId,
                        pricingTemplateFilePath, false,
                        "", -1, null,
                        customAssert);

                if (!uploadPricing) {

                    customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);
                    customAssert.assertAll();
                }


                boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                // if service data got published
                if (result == true) {


                    // function to get status whether consumptions have been created or not
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    }

                    // after consumptions have been created successfully
                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
                            "finalconsumptionvalues").trim().split(Pattern.quote(","));


                } else {

                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");
                }

                invoiceId = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, flowToTest);

                invoiceLineItemId = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId,invoiceId);

                int consumptionId = consumptionIds.get(0);

                if(scenario.equals("consumption approve scenario")){
                    Boolean workflowAction = workflowActionsHelper.performWorkFlowStepV2(consumptionsEntityTypeId,consumptionId,"Approve",customAssert);
                    if(!workflowAction){
                        customAssert.assertTrue(false,"Workflow Step Approve consumption performed unsuccessfully");
                    }else {
                        reValidationStarted = validateReValidationStarted(invoiceId,customAssert);
                        if(!reValidationStarted){
                            customAssert.assertTrue(false,"ReValidation not started for consumption approve scenario");
                        }

                    }
                }else if(scenario.equals("line item type updation")){
                    int updatedLineItemTypeId = 1090;

                    Boolean updateStatus = entityEditHelper.updateFieldDropDown(invoiceLineItem,invoiceLineItemId,"lineItemType",updatedLineItemTypeId,customAssert);

                    if(!updateStatus){
                        customAssert.assertTrue(false,"Field updated unsuccessfully for scenario line item type updation");
                    }else {
                        reValidationStarted = validateReValidationStarted(invoiceId,customAssert);
                        if(!reValidationStarted){
                            customAssert.assertTrue(false,"ReValidation not started for line item type updation scenario");
                        }
                    }
                }else if(scenario.equals("attribute value updation")){

                    String updatedAttributeValue = "TestAbc";
                    Boolean updateStatus = entityEditHelper.updateField(invoiceLineItem,invoiceLineItemId,"attributeValue",updatedAttributeValue,customAssert);

                    if(!updateStatus){
                        customAssert.assertTrue(false,"Field updated unsuccessfully for scenario attribute value updation");
                    }else {
                        reValidationStarted = validateReValidationStarted(invoiceId,customAssert);
                        if(!reValidationStarted){
                            customAssert.assertTrue(false,"ReValidation not started for line item type updation scenario");
                        }
                    }
                }

                System.out.println("");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    //C63363 Validated on 21 April
    @Test(enabled = true)
    public void TestReValidationAdjustmentLineItem(){

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "arc flow 1";

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        int invoiceId1 = -1;
        int invoiceId2 = -1;
        int invoiceLineItemId1;
        int invoiceLineItemId2;

        ArrayList<Integer> consumptionIds = new ArrayList<>();

        try{

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,contractId,uniqueDataString);

            if(serviceDataId != -1) {

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                        flowToTest, serviceDataId,
                        pricingTemplateFilePath, false,
                        "", -1, null,
                        customAssert);

                if (!uploadPricing) {

                    customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);
                    customAssert.assertAll();
                }


                boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                // if service data got published
                if (result == true) {


                    // function to get status whether consumptions have been created or not
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    }

                    // after consumptions have been created successfully
                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
                            "finalconsumptionvalues").trim().split(Pattern.quote(","));


                } else {

                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");
                }

                int consumptionId1 = consumptionIds.get(0);

                Boolean finalConsumptionUpdateStatus =  updateConsumption(consumptionId1,100.0,customAssert);

                if(!finalConsumptionUpdateStatus){
                    customAssert.assertTrue(false,"Error while updating final consumption");
                }

                Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionsEntityTypeId,consumptionId1,approve,customAssert);
                if(!consumptionApprovalStatus){
                    customAssert.assertTrue(false,"Error while approving consumption");
                }

                invoiceId1 = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, flowToTest);

                invoiceLineItemId1 = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,flowToTest,serviceDataId,invoiceId1);

                invoiceId2 = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, flowToTest);

                invoiceLineItemId2 = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,flowToTest,serviceDataId,invoiceId2);

                String createPayload = getCreatePayloadFromClone(invoiceLineItem, invoiceLineItemId2);

                createPayload = updateCreatePayloadForDifferentLineItemType(createPayload, "ProjectA", "1088");

                Create create = new Create();
                create.hitCreate(invoiceLineItem, createPayload);
                String createResponse = create.getCreateJsonStr();

                int invoiceLineItemWithAdjustmentType = CreateEntity.getNewEntityId(createResponse, "invoice");

                Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(invoiceLineItemEntityTypeId, invoiceLineItemId1, approve);
                if(workFlowStatus) {
                    workFlowStatus = workflowActionsHelper.performWorkflowAction(invoiceEntityTypeId, invoiceId1, approveInvoice);
                    if (workFlowStatus) {
                        workFlowStatus = workflowActionsHelper.performWorkflowAction(invoiceLineItemEntityTypeId, invoiceLineItemId1, calculateMemo);

                        if (workFlowStatus) {
                            Boolean reValidationStatus = revalidateScenario(invoiceId2, customAssert);

                            if (!reValidationStatus) {

                                customAssert.assertTrue(false, "ReValidation done unsuccessfully for invoice 2");
                            }

                        } else {
                            customAssert.assertTrue(false, "Unable to calculate memo on line item 1");
                        }

                    }else {
                        customAssert.assertTrue(false, "Unable to approve invoice 1");
                    }
                }else {
                    customAssert.assertTrue(false, "Unable to approve line item 1");
                }
                System.out.println("");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();
    }

//    C10304 Validated 20 April
    @Test(enabled = true)
    public void TestReValidationPricingOverride(){

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "arc flow 1";
        String pricingChangeFlow = "change request flow";

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        int invoiceId = -1;
        int invoiceLineItemId;

        ArrayList<Integer> consumptionIds = new ArrayList<>();

        HashMap<Integer,Object> dataSheetUpdatedValuesMap = new HashMap<>();

        String newPricingUploadScenario = "pricing change scenario";

        try{

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,contractId,uniqueDataString);

            if(serviceDataId != -1) {

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                        flowToTest, serviceDataId,
                        pricingTemplateFilePath, false,
                        "", -1, null,
                        customAssert);

                if (!uploadPricing) {

                    customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);
                    customAssert.assertAll();
                }


                boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                // if service data got published
                if (result == true) {


                    // function to get status whether consumptions have been created or not
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    }

                    // after consumptions have been created successfully
                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
                            "finalconsumptionvalues").trim().split(Pattern.quote(","));


                } else {

                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");
                }

                invoiceId = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, flowToTest);

                invoiceLineItemId = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId,invoiceId);

                int consumptionId = consumptionIds.get(0);

                String changeRequestValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "changerequestname");
                String cdrValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrname");

                if(pricingChangeFlow.equals("change request flow")) {
                    dataSheetUpdatedValuesMap.put(9, changeRequestValue);
                }else if(pricingChangeFlow.equals("cdr flow")) {
                    dataSheetUpdatedValuesMap.put(9, cdrValue);
                }

                dataSheetUpdatedValuesMap.put(8, "test");
                uploadPricing = invoicePricingHelper.uploadPricingFile(configFilePath, configFileName,
                        newPricingUploadScenario, serviceDataId,
                        pricingTemplateFilePath, true,
                        "Data", 6, dataSheetUpdatedValuesMap,
                        customAssert);

                if(!uploadPricing){
                    customAssert.assertTrue(false,"Error while pricing upload second time");
                }

                Boolean revalidateScenario = revalidateScenario(invoiceId,customAssert);

                if(!revalidateScenario){
                    customAssert.assertTrue(false,"Revalidate scenario validated unsuccessfully");
                }


            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    private boolean updateConsumption(int consumptionId, Double finalConsumption, CustomAssert customAssert){

        Boolean consumptionUpdateStatus = true;
        Edit edit = new Edit();
        Show show = new Show();

        String editResponse = null;
        String showResponse = null;
        String consumptionsEntity = "consumptions";
        try{

            editResponse = edit.hitEdit(consumptionsEntity,consumptionId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption").put("values",finalConsumption);
            editResponse = edit.hitEdit(consumptionsEntity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Consumtions updated unsuccessfully");
            }else {

                show.hitShowVersion2(consumptionsEntityTypeId, consumptionId);
                showResponse = show.getShowJsonStr();
                JSONObject showResponseJson = new JSONObject(showResponse);
                String finalConsumptionsValueShowPage = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption").get("values").toString();

                if(!finalConsumptionsValueShowPage.equalsIgnoreCase(String.valueOf(finalConsumption))){
                    logger.error("Final Consumption value from show page and expected didn't matched");
                    customAssert.assertTrue(false,"Final Consumption value from show page and expected didn't matched");
                }

            }

        }catch (Exception e){
            logger.error("Exception while updating consumption " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating consumption " + e.getStackTrace());
        }

        return consumptionUpdateStatus;
    }

    private boolean revalidateScenario(int invoiceId,CustomAssert customAssert){

        String invoiceWorkFlowResponse = Actions.getActionsV3Response(invoiceEntityTypeId,invoiceId);

        Boolean revalidateScenario = true;
        Boolean reValidationButtonFound = true;

        String expectedButtonName = "Revalidate";
        String revalidateUrl = null;

        if(JSONUtility.validjson(invoiceWorkFlowResponse)){

            JSONObject invoiceWorkFlowResponseJson = new JSONObject(invoiceWorkFlowResponse);
            JSONArray layoutActionsArray = invoiceWorkFlowResponseJson.getJSONArray("layoutActions");

            for(int i = 0;i<layoutActionsArray.length();i++){

                if(layoutActionsArray.getJSONObject(i).get("name").toString().equals(expectedButtonName)){
                    revalidateUrl = layoutActionsArray.getJSONObject(i).get("api").toString();
                    logger.info(expectedButtonName +  " Button found on invoice show page");
                    reValidationButtonFound = true;
                    break;
                }
            }

            if(!reValidationButtonFound){
                customAssert.assertTrue(false,expectedButtonName +  " Button not found on invoice show page");
                revalidateScenario = false;
            }else {
                String payload = "{\"entityId\":" + invoiceId + "}";
                String response = Actions.hitActionApi(revalidateUrl,payload);

                if(!response.contains("success")){
                    customAssert.assertTrue(false,"Error while revalidating the invoice");
                    revalidateScenario = false;
                }
            }

        }else {
            logger.error("invoice WorkFlowResponse is an invalid json");
            customAssert.assertTrue(false,"invoice WorkFlowResponse is an invalid json");
            revalidateScenario = false;
        }

        return revalidateScenario;
    }

    private boolean validateReValidationStarted(int invoiceId,CustomAssert customAssert){

        Boolean revalidationStarted = true;
        try {
            String actionsResponse = Actions.getActionsV3Response(invoiceEntityTypeId, invoiceId);

            JSONObject actionsResponseJson = new JSONObject(actionsResponse);
            JSONArray layoutActionsArray = actionsResponseJson.getJSONArray("layoutActions");
            String name;
            for (int i = 0; i < layoutActionsArray.length(); i++) {

                name = layoutActionsArray.getJSONObject(i).get("name").toString();

                if (name.equals("ShowLineItems")) {
                    String validationStatusValue = layoutActionsArray.getJSONObject(i).getJSONObject("properties").get("validationStatusValue").toString();

                    if (!validationStatusValue.equals("In Progress")) {
                        customAssert.assertTrue(false, "Expected value In Progress Actual value " + validationStatusValue);
                        revalidationStarted = false;
                    }
                    break;
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while checking validation in progress " + e.getStackTrace());
            revalidationStarted = false;
        }
        return revalidationStarted;
    }

    private String getCreatePayloadFromClone(String entityName,int entityId){

        Clone clone = new Clone();

        String cloneResponse;
        String createPayload = null;
        cloneResponse = clone.hitCloneV2(entityName,entityId);
        try {
            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            cloneResponseJson.remove("header");
            cloneResponseJson.remove("session");
            cloneResponseJson.remove("actions");
            cloneResponseJson.remove("createLinks");
            cloneResponseJson.getJSONObject("body").remove("layoutInfo");
            cloneResponseJson.getJSONObject("body").remove("globalData");
            cloneResponseJson.getJSONObject("body").remove("errors");

            createPayload = cloneResponseJson.toString();

        }catch (Exception e){
            logger.error("Exception while creating payload for Create for invoice Line Item ID " + entityId);
        }

        return createPayload;
    }

    private String updateCreatePayloadForDifferentLineItemType(String createPayload,String valuesOptionName,String valuesOptionId){

        String updatedCreatePayload = null;
        try {
            JSONObject updatedCreatePayloadJson = new JSONObject(createPayload);
            JSONObject lineItemTypeJson = updatedCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("lineItemType");
            JSONObject valuesJson = new JSONObject();
            if(lineItemTypeJson.has("values")){

                valuesJson = lineItemTypeJson.getJSONObject("values");
                valuesJson.put("name",valuesOptionName);
                valuesJson.put("id",valuesOptionId);
                lineItemTypeJson.put("values",valuesJson);
            }else {
                valuesJson.put("name",valuesOptionName);
                valuesJson.put("id",valuesOptionId);
                lineItemTypeJson.append("values",valuesJson);
            }

            updatedCreatePayloadJson.getJSONObject("body").getJSONObject("data").put("lineItemType",lineItemTypeJson).toString();
            updatedCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("lineItemType").remove("options");

            updatedCreatePayload = updatedCreatePayloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while creating uploaded payload for create " + e.getMessage());
        }

        return  updatedCreatePayload;
    }

}
