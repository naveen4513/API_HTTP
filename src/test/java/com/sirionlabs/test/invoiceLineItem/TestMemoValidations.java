package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import net.minidev.json.JSONUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
//Scrpping the class as not required after creation of class TestInvMemoBillingAdjScenarios
public class TestMemoValidations {

    private final static Logger logger = LoggerFactory.getLogger(TestMemoValidations.class);

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

    private String invoiceFlowsConfigFilePath;
    private String invoiceFlowsConfigFileName;
    private String pricingTemplateFilePath;

    private int invoiceLineItemEntityTypeId;
    private int invoiceEntityTypeId;
    private int serviceDataEntityTypeId;
    private int changeRequestEntityTypeId;
    private int cdrEntityTypeId;
    private int consumptionEntityTypeId;

    private String invoiceLineItem = "invoice line item";
    private String consumptionEntity = "consumptions";

    private String approve = "Approve";
    private String approveAction = "approve";
    private String calculateMemo = "Calculatememo";
    private String publishAction = "publish";

    private Boolean memoToBeCalculated;

    private HashMap<String,Integer> flowForMemoAndLineItemIdMap = new HashMap<>();
    private HashMap<String,Integer> flowForMemoAndInvoiceIdMap = new HashMap<>();

    private List<String> approveStepsInvoice;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("testMemoConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("testMemoConfigFileName");

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

        invoiceLineItemEntityTypeId = 165;
        invoiceEntityTypeId = 67;
        serviceDataEntityTypeId = 64;
        changeRequestEntityTypeId = 63;
        consumptionEntityTypeId = 176;
        cdrEntityTypeId = 160;
        
        approveStepsInvoice = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"approve_steps_invoice").split("->"));
    }

    @DataProvider(name = "memoCreationWithPricingChangeFlows", parallel = false)
    public Object[][] memoCreationWithFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "memo creation with pricing change flows","pricing change flows").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

//
    public void testLineItemCreation(String flowToTestMemo,String flowToTest){

        CustomAssert customAssert = new CustomAssert();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();

        int serviceDataId;
        int invoiceId;
        int invoiceLineItemId;

        String serviceDataType;
        try{
//            String flowToTest = "fixed fee flow 1";
//            String flowToTestMemo = "";

            InvoiceHelper invoiceHelper = new InvoiceHelper();

            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

            serviceDataType = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
                    "servicedatatype");

            int contractId =Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contractid"));

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueString = DateUtils.getCurrentTimeStamp();
            serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,contractId,uniqueString);

            Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,
                    flowToTest,serviceDataId,
                    pricingTemplateFilePath,false,
                    "",-1,null,
                    customAssert);

            if(!uploadPricing){

                customAssert.assertTrue(false,"Upload pricing done unsuccessfully for the flow " + flowToTest);

            }

            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
            {
                boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId,serviceDataId,publishAction,customAssert);
                // if service data got published
                if (result == true) {

                    ArrayList<Integer> consumptionIds = new ArrayList<>();
                    // function to get status whether consumptions have been created or not
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false,"Consumption Creation Task failed. Hence skipping further validation for Flow " +  flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false,"Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " +  flowToTest);
                    }

                    // after consumptions have been created successfully
                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
                            "finalconsumptionvalues").trim().split(Pattern.quote(","));

                    if (!(flowToTest.equals("arc flow 6") || flowToTest.equals("forecast flow 4") ||
                            flowToTest.equals("vol pricing flow consumption unavailable"))) {

                        for (int i = 0; i < consumptionIds.size(); i++) {
                            result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));

                            if (result == false) {
                                logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
                                customAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                        "Hence skipping validation");


                            } else {
                                if (!(flowToTest.equals("arc flow 7") || flowToTest.equals("forecast flow 5")
                                        || flowToTest.equals("vol pricing flow consumption unapproved"))) {

                                    result = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId,consumptionIds.get(i),approveAction,customAssert);
                                    customAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptionEntity + " having id : " + consumptionIds.get(i));

                                    if (result == false) {
                                        logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
                                        customAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
                                                "Hence skipping validation");
                                    }
                                }
                            }

                        }

                    }
                } else {

                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");
                }
            }

            if (serviceDataType.contentEquals("forecast")) {

                    String forecastTemplateFileName = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
                            "forecasttemplatefilename");

                    String  forecastTemplateFilePath = pricingTemplateFilePath;
                    Boolean editAndUploadForecast = invoiceHelper.editAndUploadForecastSheet(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,forecastTemplateFilePath, forecastTemplateFileName, flowToTest, "newClient" + contractId, contractId);


                    if (editAndUploadForecast == false) {
                        logger.error("For Flow [{}] , edit and Upload Forecast sheet is failing so Skipping the further Part", flowToTest);
                        customAssert.assertTrue(false,"For Flow, edit and Upload Forecast sheet is failing so Skipping the further Part " +  flowToTest);
                    }
            }

            invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,flowToTest);

            invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,flowToTest,serviceDataId);

            flowForMemoAndInvoiceIdMap.put(flowToTestMemo,invoiceId);
            flowForMemoAndLineItemIdMap.put(flowToTestMemo,invoiceLineItemId);

        }catch (Exception e){
            logger.error("Exception While Validating Creation of Adjustment Line Item Memo Validations");
            customAssert.assertTrue(false,"Exception While Creation of line item for the flow " + flowToTest + e.getMessage());
        }
    }

//    C431
    @Test(enabled = true)
    public void TestMemoCreationValidationStatusSameAsOfLineItem(){

        CustomAssert customAssert = new CustomAssert();

        String currentFlow;
        String createPayload;
        String createResponse;
        String invoiceValidationStatus;
        String flowForMemo = "memo line item status as previous";
        String showResponse;

        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"validation_status_same_as_previous_line_item","flows to test").split(",");


        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        Create create = new Create();
        Show show = new Show();

        Boolean workFlowStatus;
        Boolean invoiceApprovalStatus;
        Boolean lineItemValidatedSuccessfully;
        Boolean validationStatusAdjustmentType;
        Boolean errorValidationStatus;

        int invoiceLineItemWithAdjustmentType;
        int invoiceLineItemId;
        int invoiceId;
        int serviceDataId = -1;

        try {

            for(String flowToTest : flowsToTest) {

                currentFlow = flowForMemo + " " + flowToTest;

                testLineItemCreation(currentFlow, flowToTest);
                invoiceId = flowForMemoAndInvoiceIdMap.get(currentFlow);
                invoiceLineItemId = flowForMemoAndLineItemIdMap.get(currentFlow);

                //C499 Memo should not be created before line item or invoice is approved
                String workFlowResponse = workflowActionsHelper.performWorkflowActionV2ResponseString(invoiceLineItemEntityTypeId, invoiceLineItemId, calculateMemo);

                String expectedErrorString = "Cannot Create Memo, Either Line Item Or Invoice Is Not Approved";

                errorValidationStatus = checkValidationErrorExists(workFlowResponse,expectedErrorString,customAssert);

                if(!errorValidationStatus){
                    customAssert.assertTrue(false,"Error should be there on Calculate Memo when invoice is not approved");
                }

//                approving invoice
                invoiceApprovalStatus = approveInvoice(invoiceId, approveStepsInvoice, customAssert);

                if(!invoiceApprovalStatus){
                    customAssert.assertTrue(false,"Unable to approve invoice for the flow " + currentFlow);
                    customAssert.assertTrue(false,"Test Memo Creation Validation Status Same As Of Line Item validate unsuccessfully for the flow " + currentFlow);
                    continue;
                }else {
                    show.hitShowVersion2(invoiceLineItemEntityTypeId, invoiceLineItemId);
                    showResponse = show.getShowJsonStr();

                    try {
                        serviceDataId = Integer.parseInt(ShowHelper.getValueOfField("servicedataid", showResponse));
                    } catch (Exception e) {
                        logger.error("Exception while getting service data for the flow " + currentFlow);
                    }
                }

                workFlowResponse = workflowActionsHelper.performWorkflowActionV2ResponseString(invoiceLineItemEntityTypeId, invoiceLineItemId, calculateMemo);
                expectedErrorString = "Cannot Create Memo, Either Line Item Or Invoice Is Not Approved";

                errorValidationStatus = checkValidationErrorExists(workFlowResponse,expectedErrorString,customAssert);

                if(!errorValidationStatus){
                    customAssert.assertTrue(false,"Error should be there on Calculate Memo when invoice is not approved");
                }

                workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, approve,customAssert);

                if (workFlowStatus) {
                    memoToBeCalculated = true;

                    if (memoToBeCalculated) {

                        invoiceValidationStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, currentFlow, "expectedlineitemstatus");
                        //Performing calculate memo on the line item
                        workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, calculateMemo,customAssert);

                        if (!workFlowStatus) {

                            customAssert.assertTrue(false, "Unable to perform workflow option " + calculateMemo + " on the invoice");
                            continue;
                        }
                    } else {
                        invoiceValidationStatus = "No Memo Found";
                    }

                    if (workFlowStatus) {

//                    if memo is calculated then creating a new line item adjustment type linked to this line item
                        createPayload = getCreatePayloadFromClone(invoiceLineItem, invoiceLineItemId);

                        if (createPayload != null) {

                            createPayload = updateCreatePayloadForDifferentLineItemType(createPayload, "ProjectA", "1088");
                            create.hitCreate(invoiceLineItem, createPayload);
                            createResponse = create.getCreateJsonStr();
                            invoiceLineItemWithAdjustmentType = CreateEntity.getNewEntityId(createResponse, "invoice");

                            lineItemValidatedSuccessfully = InvoiceHelper.verifyInvoiceLineItemValidationStatus(invoiceValidationStatus, invoiceLineItemWithAdjustmentType, customAssert);

                            if (!lineItemValidatedSuccessfully) {
                                customAssert.assertTrue(false, "Line Item Validation Status validated unsuccessfully");
                            }else {
                                validationStatusAdjustmentType = validateMemoStatusAdjustmentLineItemScenario(currentFlow,flowToTest,serviceDataId,invoiceLineItemId,-1,-1,null,invoiceLineItemWithAdjustmentType,customAssert);

                                if(!validationStatusAdjustmentType){
                                    customAssert.assertTrue(false,"Adjustment Type Line Item Scenario validated unsuccessfully for the flow " + currentFlow);
                                }

//                                C450
                                calculateMemoOnLineItem(invoiceLineItemWithAdjustmentType,customAssert);

                                Double new_invoice_amount = 0.0;
                                try{
                                    show.hitShowVersion2(invoiceLineItemEntityTypeId,invoiceLineItemWithAdjustmentType);
                                    showResponse = show.getShowJsonStr();
                                    JSONObject showResponseJson = new JSONObject(showResponse);
                                    new_invoice_amount = Double.parseDouble(showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("systemTotal").get("values").toString());

                                }catch (Exception e){
                                    logger.error("Exception while fetching system total");
                                }
                                validationStatusAdjustmentType = validateMemoStatusAdjustmentLineItemScenario(currentFlow,flowToTest,serviceDataId,invoiceLineItemWithAdjustmentType,-1,-1,new_invoice_amount,-1,customAssert);

                                if(!validationStatusAdjustmentType){
                                    customAssert.assertTrue(false,"Check memo creation using discrepancy validated unsuccessfully ");
                                    customAssert.assertTrue(false,"Test case C450 failed");
                                }
                            }

                        } else {
                            customAssert.assertTrue(false, "Unable to create payload for creating cloned line item");
                        }

                    } else {
                        customAssert.assertTrue(false, "Unable to perform workflow action calculate memo on line item id " + invoiceLineItemId);
                    }
                }
            }
        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Test Memo Creation Validation Status Same As Of Line Item " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //C431 and C539
    @Test(enabled = false)
    public void TestMemoCreationValidationStatusNoMemoFound(){

        CustomAssert customAssert = new CustomAssert();

        String invoiceValidationStatus;
        String flowForMemo = "memo line item status as no memo found";

        String createPayload;
        String createResponse;

        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"validation_status_no_memo_found","flows to test").split(",");

        String currentFlow;

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        Create create = new Create();

        Boolean workFlowStatus;
        Boolean lineItemValidatedSuccessfully;

        int invoiceLineItemWithAdjustmentType;
        int invoiceLineItemId;
        int invoiceId;

        try {

            for(String flowToTest : flowsToTest) {

                currentFlow = flowForMemo + " " + flowToTest;
                testLineItemCreation(currentFlow, flowToTest);

                invoiceId = flowForMemoAndInvoiceIdMap.get(currentFlow);
                invoiceLineItemId = flowForMemoAndLineItemIdMap.get(currentFlow);


                memoToBeCalculated = false;

                invoiceValidationStatus = "No Memo Found";

//                    if memo is calculated then creating a new line item adjustment type linked to this line item
                createPayload = getCreatePayloadFromClone(invoiceLineItem, invoiceLineItemId);

                if (createPayload != null) {

                    createPayload = updateCreatePayloadForDifferentLineItemType(createPayload, "ProjectA", "1088");
                    create.hitCreate(invoiceLineItem, createPayload);
                    createResponse = create.getCreateJsonStr();
                    invoiceLineItemWithAdjustmentType = CreateEntity.getNewEntityId(createResponse, "invoice");

                    lineItemValidatedSuccessfully = InvoiceHelper.verifyInvoiceLineItemValidationStatus(invoiceValidationStatus, invoiceLineItemWithAdjustmentType, customAssert);

                    if (!lineItemValidatedSuccessfully) {
                        customAssert.assertTrue(false, "Line Item Validation Status validated unsuccessfully for the flow " + currentFlow);
                    } else {

//                                approving invoice
                        Boolean approvalStatus = approveInvoice(invoiceId, approveStepsInvoice, customAssert);
                        if (!approvalStatus) {
                            customAssert.assertTrue(false, "Unable to approve invoice for the flow " + flowToTest);
                        }


                        workFlowStatus = workflowActionsHelper.performWorkflowAction(invoiceLineItemEntityTypeId, invoiceLineItemId, approve);

                        if (!workFlowStatus) {
                            customAssert.assertTrue(false, "Unable to approve invoice line item");
                        }
                        workFlowStatus = workflowActionsHelper.performWorkflowAction(invoiceLineItemEntityTypeId, invoiceLineItemId, calculateMemo);

                        if (!workFlowStatus) {

                            customAssert.assertTrue(false, "Unable to perform workflow option " + calculateMemo + " on the invoice");
                            continue;
                        }

                        //Code to added further  revalidate button invoice C866
//                                **************************************************************
//                                **************************************************************
                    }
                } else {
                    customAssert.assertTrue(false, "Unable to create payload for creating a new line item adjustment type linked to this line item");
                }

            }
        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Memo Creation Validation Status Same As Of LineItem " + e.getMessage());
        }

        customAssert.assertAll();
    }


/*
//    C866 (Scrapping the test)
    @Test(dataProvider = "memoCreationWithPricingChangeFlows",enabled = false)
    public void TestCheckMemoCreationPricingChange(String pricingChangeFlow){

        CustomAssert customAssert = new CustomAssert();

        String flowForMemo = pricingChangeFlow;
        String currentFlow = "";
        String showResponse;
        String changeRequestValue;
        String cdrValue;

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        Show show = new Show();

        Boolean workFlowStatus;
        Boolean approvalStatus;
        Boolean uploadPricing;

        int invoiceLineItemId;
        int invoiceId;
        int serviceDataId = -1;

        HashMap<Integer,Object> dataSheetUpdatedValuesMap = new HashMap<>();
        List<HashMap<String,String>> expectedValueMapList = new ArrayList<>();

        try {

            String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"memo creation with pricing change flows","flows to test").split(",");

            int changeRequestId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"change_request_id"));
            int cdrId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"cdrid"));

            for(String flowToTest : flowsToTest) {
                try {
                    currentFlow = flowForMemo + " " + flowToTest;

                    //Creating line item
                    testLineItemCreation(currentFlow, flowToTest);
                    invoiceId = flowForMemoAndInvoiceIdMap.get(currentFlow);
                    invoiceLineItemId = flowForMemoAndLineItemIdMap.get(currentFlow);

//                approving invoice
                    approvalStatus = approveInvoice(invoiceId, approveStepsInvoice, customAssert);

                    if (!approvalStatus) {
                        customAssert.assertTrue(false, "Unable to approve invoice for the flow " + flowToTest);
                    }

//                approving line item
                    //workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, "Approve", customAssert);
                    workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, approveAction,customAssert);
                    customAssert.assertTrue(workFlowStatus, "Not Being able to Perform  " + approveAction + " Action on " + invoiceLineItemEntityTypeId + " having id : " + invoiceLineItemId);
                    //if (workFlowStatus) {
                        show.hitShowVersion2(invoiceLineItemEntityTypeId, invoiceLineItemId);
                        showResponse = show.getShowJsonStr();

                        try {
                            serviceDataId = Integer.parseInt(ShowHelper.getValueOfField("servicedataid", showResponse));
                        } catch (Exception e) {
                            logger.error("Exception while getting service data for the flow " + currentFlow);
                        }

                        if (serviceDataId != -1) {
                            changeRequestValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "changerequestname");
                            cdrValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrname");

                            if(pricingChangeFlow.equals("change request flow")) {
                                dataSheetUpdatedValuesMap.put(9, changeRequestValue);
                            }else if(pricingChangeFlow.equals("cdr flow")) {
                                dataSheetUpdatedValuesMap.put(9, cdrValue);
                            }

                            dataSheetUpdatedValuesMap.put(8, "test");
                            uploadPricing = invoicePricingHelper.uploadPricingFile(configFilePath, configFileName,
                                    currentFlow, serviceDataId,
                                    pricingTemplateFilePath, true,
                                    "Data", 6, dataSheetUpdatedValuesMap,
                                    customAssert);

                            if (!uploadPricing) {

                                customAssert.assertTrue(false, "Re Upload pricing done unsuccessfully for the flow " + currentFlow);

                            } else {
                                //C10304 To write logic for test case here

                                //
                                Thread.sleep(30000);
                                workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, "Calculatememo", customAssert);

                                if (!workFlowStatus) {
                                    customAssert.assertTrue(false, "Unable to perform Calculate memo on line item id " + invoiceLineItemId);
                                } else {

                                    Boolean validateMemoStatusPricingChangeScenario = validateMemoStatusPricingChangeScenario(currentFlow,flowToTest,serviceDataId,invoiceLineItemId,changeRequestId,cdrId,customAssert);

                                    if(validateMemoStatusPricingChangeScenario){

                                        logger.info("Memo Status Pricing Change Scenario validated successfully for the flow " + currentFlow);
                                        customAssert.assertTrue(true,"Memo Status Pricing Change Scenario validate successfully for the flow " + currentFlow);

//                                        Boolean memoValidationStatusOnBillingReport = validateMemoReasonOnBillingDataReport(String.valueOf(serviceDataId),"Price Change",customAssert);
//
//                                        if(!memoValidationStatusOnBillingReport){
//                                            customAssert.assertTrue(false,"memo Validated unsuccessfully On Billing Report ");
//                                        }

                                    }else {
                                        logger.error("Memo Status Pricing Change Scenario validated unsuccessfully for the flow " + currentFlow);
                                        customAssert.assertTrue(false,"Memo Status Pricing Change Scenario validate unsuccessfully for the flow " + currentFlow);
                                    }

//                                    Boolean memoValidationStatus = validateMemoTab(expectedValueMapList, invoiceLineItemId, customAssert);
//
//                                    if (memoValidationStatus) {
//                                        logger.info("Memo Logic validated successfully for flow " + currentFlow);
//                                    } else {
//                                        logger.error("Memo Logic validated unsuccessfully for flow " + currentFlow);
//                                        customAssert.assertTrue(false, "Memo Logic validated unsuccessfully for flow " + currentFlow);
//                                    }
                                }
                            }
                        } else {
                            customAssert.assertTrue(false, "Service Data Id = -1");
                        }
//                    } else {
//                        customAssert.assertTrue(false, "Unable to approve invoice line item id " + invoiceLineItem);
//                    }
                }catch (Exception e){
                    customAssert.assertTrue(false,"Exception while validating Memo logic for the flow " + currentFlow);
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Memo Logic");
        }
        customAssert.assertAll();
    }

*/
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

    private Boolean validateMemoTab(List<HashMap<String,String>> expectedValueMapList,int invoiceLineItemId, CustomAssert customAssert){

        Boolean validationStatus = true;
        try{
            String columnName;
            String columnValue;
            String expectedColumnValue;
            String filterPayload = "{\"filterMap\":{\"entityTypeId\":188,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            String tabListResponse;

            TabListData tabListData = new TabListData();

            JSONObject tabListResponseJson = new JSONObject();
            JSONArray dataJsonArray;

            HashMap<String,String> expectedValueMap;

            int memoTabId = 337;

            long startTime = 0L;
            long pollingTime = 5000L;
            long jobTimeOut = 30000L;

            boolean expectedNumRowsFound = false;
            while (startTime < jobTimeOut) {
                try {
                    tabListData.hitTabListData(memoTabId, invoiceLineItemEntityTypeId, invoiceLineItemId, filterPayload);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataJsonArray = tabListResponseJson.getJSONArray("data");

                    if(dataJsonArray.length() ==  expectedValueMapList.size()){
                        expectedNumRowsFound = true;
                        break;
                    }

                    startTime +=pollingTime;
                    Thread.sleep(pollingTime);

                } catch (Exception e) {
                    logger.error("Exception while fetching data from adjustment tab from line item id " + invoiceLineItemId);
                    customAssert.assertTrue(false,"");
                    validationStatus = false;
                    return validationStatus;
                }
            }

            if(!expectedNumRowsFound){

                customAssert.assertTrue(false,"After timeout under Adjustment tab number of rows are not equal to expected number " + expectedValueMapList.size());
                return false;
            }

//            for(int i=expectedValueMapList.size() -1;i >= 0;i--) {
            for(int i=0;i < expectedValueMapList.size();i++) {

                expectedValueMap = expectedValueMapList.get(i);

                JSONObject indRowData = tabListResponseJson.getJSONArray("data").getJSONObject(i);

                JSONArray indRowDataArray = JSONUtility.convertJsonOnjectToJsonArray(indRowData);

                for (int columnNum = 0; columnNum < indRowDataArray.length(); columnNum++) {

                    columnName = indRowDataArray.getJSONObject(columnNum).get("columnName").toString();
                    columnValue = indRowDataArray.getJSONObject(columnNum).get("value").toString();

                    if (expectedValueMap.containsKey(columnName)) {
                        expectedColumnValue = expectedValueMap.get(columnName);

                        if (!expectedColumnValue.equalsIgnoreCase(columnValue)) {
                            if(expectedColumnValue.equalsIgnoreCase("0.000000000000") && columnValue.equalsIgnoreCase("0E-12")){
// Do nothing
                            }else {
                                customAssert.assertTrue(false, "Expected and Actual Value mismatch for column Name " + columnName + " for row number " + i);
                                customAssert.assertTrue(false, "Expected Value : " + expectedColumnValue + " Actual Value : " + columnValue);
                                validationStatus = false;
                            }
                        }
                    }
                }
            }
        }catch (Exception e){

            validationStatus = false;
            customAssert.assertTrue(false,"Exception while validating Memo Tab for invoice line item id " + e.getMessage());
        }

        return validationStatus;
    }

    private Boolean approveInvoice(int invoiceId,List<String> workFlowSteps,CustomAssert customAssert){

        Boolean approveStatus = true;
        Boolean workFlowStatus;
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try{
            for(String workFlowStep : workFlowSteps) {

                workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId, workFlowStep,customAssert);

                if(!workFlowStatus){
                    customAssert.assertTrue(false,"Unable to perform action " + workFlowStep + " on the invoice " + invoiceId);
                    approveStatus = false;
                    return approveStatus;
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while performing workFlowStep on the invoice " + invoiceId);
            approveStatus = false;
        }
        return approveStatus;
    }

    private HashMap<String,String> createExpValuesMapAdjustmentTab(
            String currentFlow,
            int serviceDataId,
            int invoiceLineItemId,
            int ccrId,
            int cdrId,
            int validateByLineItemId,
            String memoReason,
            Double paidInvAmt,
            Double newInvAmt
            ){

        HashMap<String,String> expectedValuesMap;

        String lineItemId;
        String lineItemDescription;
        String serviceData;
        String changeRequest;
        String MemoAmount;
        String validatedByLineItem;


        Show show = new Show();
        show.hitShowVersion2(invoiceLineItemEntityTypeId,invoiceLineItemId);
        String showResponseLineItem = show.getShowJsonStr();

        show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
        String showResponseServiceData = show.getShowJsonStr();

        expectedValuesMap = new HashMap<>();

        lineItemId = ShowHelper.getValueOfField("short code id",showResponseLineItem);
        lineItemId = lineItemId.substring(2);
        expectedValuesMap.put("sirion_line_item_id",lineItemId + ":;" + invoiceLineItemId);

        lineItemDescription = ShowHelper.getValueOfField("name",showResponseLineItem);
        expectedValuesMap.put("sirion_line_item_description",lineItemDescription);

        serviceData = ShowHelper.getValueOfField("name",showResponseServiceData) + " (" + ShowHelper.getValueOfField("serviceIdClient",showResponseServiceData) + ")";
        expectedValuesMap.put("servicedata",serviceData + ":;" + serviceDataId);

        expectedValuesMap.put("memo_reason",memoReason);

        changeRequest = "-";

        if(ccrId !=-1) {
            show.hitShowVersion2(changeRequestEntityTypeId, ccrId);
            String showResponseChangeRequest = show.getShowJsonStr();

            changeRequest = ShowHelper.getValueOfField("name",showResponseChangeRequest) + ":;" + ccrId;

        }
        expectedValuesMap.put("ccr_id", changeRequest);

        //Extra zeros haven been put because tab list api has these additional zeros in API Response
        expectedValuesMap.put("paid_invoice_amount", paidInvAmt + "00000000000");
        String NewInvAmt;
        if(newInvAmt.equals("0.0")){
            NewInvAmt = "0E-12";
        }else {
            NewInvAmt = newInvAmt + "00000000000";
        }

        expectedValuesMap.put("new_invoice_amount", NewInvAmt);

        Double memoAmount = newInvAmt - paidInvAmt;

        MemoAmount = String.valueOf(memoAmount);

        if(MemoAmount.equals("0.0")){
            MemoAmount = "0E-12";
        }else {
            MemoAmount = MemoAmount + "00000000000";
        }

        expectedValuesMap.put("adjustment",MemoAmount);

        if(validateByLineItemId == -1){
            validatedByLineItem = "null";
        }else {
            show.hitShowVersion2(invoiceLineItemEntityTypeId,validateByLineItemId);
            showResponseLineItem = show.getShowJsonStr();

            lineItemId = ShowHelper.getValueOfField("short code id",showResponseLineItem);
            lineItemId = lineItemId.substring(2);

            validatedByLineItem = lineItemId + ":;" + validateByLineItemId;
        }

        expectedValuesMap.put("validated_by_line_item",validatedByLineItem);

        if(memoReason.equals("Price Change")){

            if(currentFlow.equals("")) {
                show.hitShowVersion2(changeRequestEntityTypeId, ccrId);
                String changeRequestShowResponse = show.getShowJsonStr();
                try {
                    JSONObject changeRequestShowResponseJson = new JSONObject(changeRequestShowResponse);
                    String effectiveDate = changeRequestShowResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").get("displayValues").toString();
                    String name = changeRequestShowResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
                    String shortCodeId = changeRequestShowResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").get("values").toString();

                    if (shortCodeId.contains("CR0")) {
                        shortCodeId = shortCodeId.substring(3);
                    } else {
                        shortCodeId = shortCodeId.substring(2);
                    }
                    expectedValuesMap.put("effectivedate", effectiveDate);
                    expectedValuesMap.put("pricingentitytype", "Change Request");
                    expectedValuesMap.put("pricingentityname", name + ":;" + ccrId + ":;" + changeRequestEntityTypeId);
                    expectedValuesMap.put("pricingentityid", shortCodeId + ":;" + ccrId + ":;" + changeRequestEntityTypeId);

                } catch (Exception e) {

                }
            }else {
                show.hitShowVersion2(cdrEntityTypeId,cdrId);
                String changeRequestShowResponse = show.getShowJsonStr();
                try {
                    JSONObject changeRequestShowResponseJson = new JSONObject(changeRequestShowResponse);
                    String effectiveDate = changeRequestShowResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").get("displayValues").toString();
                    String name = changeRequestShowResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
                    String shortCodeId = changeRequestShowResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").get("values").toString();

                    if (shortCodeId.contains("CDR0")) {
                        shortCodeId = shortCodeId.substring(4);
                    } else {
                        shortCodeId = shortCodeId.substring(3);
                    }
                    expectedValuesMap.put("effectivedate", effectiveDate);
                    expectedValuesMap.put("pricingentitytype", "Contract Draft Request");
                    expectedValuesMap.put("pricingentityname", name + ":;" + cdrId + ":;" + cdrEntityTypeId);
                    expectedValuesMap.put("pricingentityid", shortCodeId + ":;" + cdrId + ":;" + cdrEntityTypeId);

                } catch (Exception e) {

                }
            }
        }

        return expectedValuesMap;
    }

    Boolean validateMemoStatusPricingChangeScenario(String currentFlow,String flowToTest,int serviceDataId,int invoiceLineItemId,int changeRequestId,int cdrId,CustomAssert customAssert){

        Boolean memoValidationStatus = true;
        try {
            HashMap<String, String> adjustmentValuesMapPricingChange;

            List<HashMap<String, String>> expectedValueMapList = new ArrayList<>();

            Double volumeValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest, "volumecolumnvalues"));
            Double rateValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest, "ratecolumnvalues"));

            Double paid_invoice_amount = volumeValue * rateValue;
            Double new_invoice_amount = volumeValue * rateValue;

            HashMap<String, String> adjustmentValuesMapDiscrepancy;
            adjustmentValuesMapDiscrepancy = createExpValuesMapAdjustmentTab(currentFlow,serviceDataId,
                    invoiceLineItemId, -1,cdrId,-1, "Discrepancy", paid_invoice_amount, new_invoice_amount);

            expectedValueMapList.add(adjustmentValuesMapDiscrepancy);

            volumeValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, currentFlow, "volumecolumnvalues"));
            rateValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, currentFlow, "ratecolumnvalues"));

            new_invoice_amount = volumeValue * rateValue;

            volumeValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest, "volumecolumnvalues"));
            rateValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest, "ratecolumnvalues"));

            paid_invoice_amount = volumeValue * rateValue;

            adjustmentValuesMapPricingChange = createExpValuesMapAdjustmentTab(currentFlow,serviceDataId,
                    invoiceLineItemId, changeRequestId,cdrId,-1, "Price Change", paid_invoice_amount, new_invoice_amount);

            expectedValueMapList.add(adjustmentValuesMapPricingChange);

            logger.info("Waiting for 2 minutes");
            Thread.sleep(120000);

            memoValidationStatus = validateMemoTab(expectedValueMapList, invoiceLineItemId, customAssert);

            if (memoValidationStatus) {
                logger.info("Memo Logic validated successfully for flow " + currentFlow);
            } else {
                logger.error("Memo Logic validated unsuccessfully for flow " + currentFlow);
                customAssert.assertTrue(false, "Memo Logic validated unsuccessfully for flow " + currentFlow);
                memoValidationStatus = false;
            }
        }catch (Exception e){
            logger.error("Exception while validating adjustments tab for the flow " + currentFlow);
            customAssert.assertTrue(false,"Exception while validating adjustments tab for the flow " + currentFlow);
            memoValidationStatus = false;
        }
        return memoValidationStatus;
    }

    Boolean validateMemoStatusAdjustmentLineItemScenario(String currentFlow,String flowToTest,int serviceDataId,int invoiceLineItemId,int changeRequestId,int cdrId,Double newInvoiceAmount,int validateByLineItemId,CustomAssert customAssert){

        Boolean memoValidationStatus = true;

        try {
            HashMap<String, String> adjustmentValuesMapPricingChange;

            List<HashMap<String, String>> expectedValueMapList = new ArrayList<>();

            Double volumeValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest, "volumecolumnvalues"));
            Double rateValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest, "ratecolumnvalues"));

            Double paid_invoice_amount = volumeValue * rateValue;
            Double new_invoice_amount;
            if(newInvoiceAmount == null) {
                new_invoice_amount = volumeValue * rateValue;
            }else {
                new_invoice_amount = newInvoiceAmount;
            }
            HashMap<String, String> adjustmentValuesMapDiscrepancy;
            adjustmentValuesMapDiscrepancy = createExpValuesMapAdjustmentTab(currentFlow,serviceDataId,
                    invoiceLineItemId, -1,cdrId,validateByLineItemId, "Discrepancy", paid_invoice_amount, new_invoice_amount);

            expectedValueMapList.add(adjustmentValuesMapDiscrepancy);

//            volumeValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, currentFlow, "volumecolumnvalues"));
//            rateValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, currentFlow, "ratecolumnvalues"));
//
//            new_invoice_amount = volumeValue * rateValue;

//            volumeValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest, "volumecolumnvalues"));
//            rateValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest, "ratecolumnvalues"));
//
//            paid_invoice_amount = volumeValue * rateValue;

//            adjustmentValuesMapPricingChange = createExpValuesMapAdjustmentTab(serviceDataId,
//                    invoiceLineItemId, changeRequestId, "Price Change", paid_invoice_amount, new_invoice_amount);
//
//            expectedValueMapList.add(adjustmentValuesMapPricingChange);

            memoValidationStatus = validateMemoTab(expectedValueMapList, invoiceLineItemId, customAssert);

            if (memoValidationStatus) {
                logger.info("Memo Logic validated successfully for flow " + currentFlow);
            } else {
                logger.error("Memo Logic validated unsuccessfully for flow " + currentFlow);
                customAssert.assertTrue(false, "Memo Logic validated unsuccessfully for flow " + currentFlow);
                memoValidationStatus = false;
            }
        }catch (Exception e){
            logger.error("Exception while validating adjustments tab for the flow " + currentFlow);
            customAssert.assertTrue(false,"Exception while validating adjustments tab for the flow " + currentFlow);
            memoValidationStatus = false;
        }

        return memoValidationStatus;
    }

    private Boolean calculateMemoOnLineItem(int invoiceLineItemId,CustomAssert customAssert){

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        Boolean calculateMemoStatus = true;
        Boolean workFlowStatus;

        try {
            workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, approve, customAssert);

            if (workFlowStatus) {

                //Performing calculate memo on the line item
                workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, calculateMemo, customAssert);

                if (!workFlowStatus) {

                    customAssert.assertTrue(false, "Unable to perform workflow option " + calculateMemo + " on the invoice line item");
                    calculateMemoStatus =false;
                }
            }else {
                customAssert.assertTrue(false, "Unable to perform workflow option " + approve + " on the invoice line item");
                calculateMemoStatus = false;
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while ");
            calculateMemoStatus =false;
        }
        return calculateMemoStatus;
    }

    private Boolean checkValidationErrorExists(String responseString,String errorStringToCheck,CustomAssert customAssert){

        Boolean validationStatusForErrorExists = false;

        try{
            JSONObject responseStringJson = new JSONObject(responseString);
            JSONArray genericErrors = responseStringJson.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors");

            String actualString;
            for(int i =0;i<genericErrors.length();i++){

                actualString = genericErrors.getJSONObject(i).get("message").toString();

                if(actualString!=null){
                    if(actualString.equalsIgnoreCase(errorStringToCheck)){
                        validationStatusForErrorExists = true;
                    }
                }
            }
            if(!validationStatusForErrorExists){
                customAssert.assertTrue(false,"Error String not found in the response String");
            }

        }catch (Exception e){
            logger.error("Exception while validating Error String " + e.getMessage());
            validationStatusForErrorExists = false;
        }
        return validationStatusForErrorExists;
    }

    //validating memo reason on billing report
    private Boolean validateMemoReasonOnBillingDataReport(String serviceDataId,String memoReasonExpected,CustomAssert customAssert){

        Boolean validationStatus = false;

        try{

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            int listData = 444;
            String payload = "\"filterMap\": {\"entityTypeId\": 64,\"offset\": 0,\"size\": 20,\"orderByColumnName\": " +
                    "\"id\",\"orderDirection\": \"desc nulls last\",\"filterJson\": {}}," +
                    "\"selectedColumns\": [{\"columnId\": 18637,\"columnQueryName\": \"servicedataid\"},{\"columnId\": 18614,\"columnQueryName\": \"servicedataname\"},{\"columnId\": 18813,\"columnQueryName\": \"memoreason\"}]}";

            reportRendererListData.hitReportRendererListData(listData,payload);
            String reportRendererListResponse = reportRendererListData.getListDataJsonStr();

            if(APIUtils.validJsonResponse(reportRendererListResponse)){
                JSONObject reportRendererListResponseJson = new JSONObject(reportRendererListResponse);

                JSONArray dataArray = reportRendererListResponseJson.getJSONArray("data");

                JSONObject indRowData;
                JSONArray indRowDataArray;
                String columnName;

                String serviceDataIdActual = "";
                String memoReasonActual = "";

                for(int i =0 ;i<dataArray.length();i++){

                    indRowData = dataArray.getJSONObject(i);
                    indRowDataArray = JSONUtility.convertJsonOnjectToJsonArray(indRowData);

                    for(int j =0;j<indRowDataArray.length();j++){

                        columnName = indRowDataArray.getJSONObject(j).get("columnName").toString();

                        if(columnName.equalsIgnoreCase("servicedataid")){
                            serviceDataIdActual = indRowDataArray.getJSONObject(j).get("value").toString().split(":;")[1];
                        }

                        if(columnName.equalsIgnoreCase("memoreason")){
                            memoReasonActual = indRowDataArray.getJSONObject(j).get("value").toString();
                        }
                    }

                    if(serviceDataIdActual.equalsIgnoreCase(serviceDataId)){
                        if(!memoReasonExpected.equalsIgnoreCase(memoReasonActual)){
                            logger.error("Expected and Actual Value for memo reason is not equal for service data id " + serviceDataId);
                            logger.error("Expected Memo Reason : " + memoReasonExpected + " Actual Memo Reason : " + memoReasonActual);
                            customAssert.assertTrue(false,"Expected and Actual Value for memo reason is not equal for service data id " + serviceDataId +
                                    " Expected Memo Reason : " + memoReasonExpected + " Actual Memo Reason : " + memoReasonActual);
                            validationStatus = false;

                        }
                        break;
                    }else {
                        validationStatus = true;
                    }
                }


            }else {
                customAssert.assertTrue(false,"Billing data report response is not a valid json");
            }

        }catch (Exception e){
            logger.error("Exception while validating Memo Reason On Billing Data Report " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Memo Reason On Billing Data Report " + e.getMessage() );
        }

        return validationStatus;
    }

}
