package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;

import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.*;

import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;

import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;
import org.testng.annotations.Optional;

import java.util.*;
import java.util.regex.Pattern;


class InvoiceDetails3 {
    public InvoiceDetails3(int contractId, int serviceDataId, String flowToTest) {
        this.contractId = contractId;
        this.serviceDataId = serviceDataId;
        this.flowToTest = flowToTest;
    }

    public int getContractId() {
        return contractId;
    }

    public int getServiceDataId() {
        return serviceDataId;
    }

    public String getFlowToTest() {
        return flowToTest;
    }

    private int contractId, serviceDataId;
    private String flowToTest;
}

public class Test_BillingDataScenBaseSpecificYes {
    private final static Logger logger = LoggerFactory.getLogger(Test_BillingDataScenBaseSpecificYes.class);

    private static Boolean failTestIfLineItemValidationNotCompletedWithinTimeOut = true;
    private static Long lineItemValidationTimeOut = 1200000L;
    private static Long pricingSchedulerTimeOut = 1200000L;

    private static Long consumptionToBeCreatedTimeOut = 1200000L;
    private static Long pollingTime = 5000L;

    int ARCRRCTabId = 311;
    int chargesTabId = 309;
    private String configFilePath;
    private String configFileName;
    private String flowsConfigFilePath;
    private String flowsConfigFileName;
    private Boolean killAllSchedulerTasks = false;
    private String pricingTemplateFilePath;

    private Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
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

    //	private String contractSectionName;
//	private String serviceDataSectionName;
//	private String invoiceSectionName;
//	private String invoiceLineItemSectionName;
    private int serviceDataEntityTypeId = -1;
    private int invoiceEntityTypeId = 67;


    //	private Show show;
    private String publishAction = "publish";
    private String approveAction = "approve";
    private String submitForApproval = "submitforapproval";
    private String entityIdMappingFileName;
    private String baseFilePath;
    //	private Edit edit;
    private String serviceDataEntity = "service data";
    private String consumptionEntity = "consumptions";
    private String invoiceLineItemEntity = "invoice line item";
    private String ARCRRCSheetNameInXLSXFile = "ARC RRC";


    private Map<String, String> flowIdContractIdMap = new HashMap<>();
    private Map<String, String> flowIdServiceDataIdMap = new HashMap<>();

    private int consumptionEntityTypeId;
    private String flowsToValidate = "", testAllFlows = "";

    private List<InvoiceDetails3> listOfInvoiceTestDetails = new ArrayList<>();
    private int listOfInvoiceTestDetailsCount=0;
//    private int testCaseCount = -1;
    private boolean checkLIValidation=true;


    @BeforeClass
    public void beforeClass() throws Exception {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvBillScenBaseSpecYesFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvBillScenBaseSpecYesFileName");

        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");

        flowsConfigFilePath = configFilePath;
        flowsConfigFileName = configFileName;

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

        // for publising of service data
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));
        consumptionEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "entity_type_id"));

    }

    @DataProvider(name = "dataProviderForInvoiceFlow",parallel = false)
    public Object[][] dataProviderForInvoiceFlow() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest();

        for (String flowToTest : allFlowsToTest) {

            allTestData.add(new Object[]{flowToTest});
        }


        return allTestData.toArray(new Object[0][]);
    }

//    C153505 C153467
    @Test(dataProvider = "dataProviderForInvoiceFlow", priority = 0,enabled = true)
    public void testInvoiceFlowPart1(String flowToTest) {

        CustomAssert csAssert = new CustomAssert();

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId = -1;
        int serviceDataId = -1;

        try {

            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
            //Get Contract that will be used for Invoice Flow Validation
            synchronized (this) {

                contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"basecontractid"));

                if (contractId != -1) {
                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                    flowIdContractIdMap.put(flowToTest, String.valueOf(contractId));

                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                            invoiceConfigFileName, flowToTest, contractId);

                    String uniqueDataString = DateUtils.getCurrentTimeStamp();

                    serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,uniqueDataString);

                    logger.info("Created Service Data Id : [{}]", serviceDataId);


                    if (serviceDataId != -1) {
                        logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                        flowIdServiceDataIdMap.put(flowToTest, String.valueOf(serviceDataId));

                        logger.info("Hitting Fetch API.");
                        Fetch fetchObj = new Fetch();
                        fetchObj.hitFetch();
                        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                        boolean uploadPricing = true;
                        String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "uploadPricing");
                        if (temp != null && temp.trim().equalsIgnoreCase("false"))
                            uploadPricing = false;

                        String publishFlag = "true";

                        publishFlag = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                "publish");

                        if (publishFlag == null) {
                            publishFlag = "true";
                        }

                        if (uploadPricing) {
                            String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                    "pricingstemplatefilename");
                            Boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);

                            // changes for ARC RRC FLOW
                            if (pricingFile) {


                                // getting the actual service data Type
                                if (ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                        "servicedatatype") != null) {
                                    serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                            "servicedatatype");
                                }


                                if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                    pricingFile = editPricingFileForARCRRC(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);


                                }
                                // changes for ARC RRC FLOW Ends here


                                if (pricingFile) {

                                    String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                    if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                                        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                            //Wait for Pricing Scheduler to Complete
                                            String pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);

                                            if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                                logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                                csAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                                        flowToTest + "]");

                                                csAssert.assertAll();
                                                return;
                                            } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                                                logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
                                                if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                                                    logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                                                    csAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                                            "Hence failing Flow [" + flowToTest + "]");
                                                } else {
                                                    logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow [{}]", flowToTest);
                                                }

                                                csAssert.assertAll();
                                                return;
                                            } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                                            {

                                                if (publishFlag.equals("true")) {

                                                    serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                            "servicedatatype");

                                                    if (serviceDataType.equals("fixed fee")) {
                                                        listOfInvoiceTestDetails.add(new InvoiceDetails3(contractId, serviceDataId, flowToTest));
                                                        listOfInvoiceTestDetailsCount++;
                                                    }

                                                    if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                                                    {

                                                        boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, serviceDataId, publishAction);
                                                        // if service data got published

                                                        if (!result) {

                                                            logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                                                            csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                                    "Hence skipping validation");

                                                            csAssert.assertAll();
                                                            return;

                                                        }
                                                        listOfInvoiceTestDetails.add(new InvoiceDetails3(contractId, serviceDataId, flowToTest));
                                                        listOfInvoiceTestDetailsCount++;
                                                    }
                                                }
                                            }
                                        }

                                    } else {
                                        logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                                pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);
                                        csAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                                pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");


                                        csAssert.assertAll();
                                        return;
                                    }
                                } else {
                                    logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                                    csAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");

                                    csAssert.assertAll();

                                    return;

                                }

                            } else {
                                logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                                csAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                                        "Hence skipping validation");

                                csAssert.assertAll();
                                return;
                            }
                        } else if (publishFlag.equals("true")) {

                            serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                    "servicedatatype");

                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))
                            {

                                boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, serviceDataId, publishAction);
                                // if service data got published

                                if (!result) {

                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                                    csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");


                                    csAssert.assertAll();
                                    return;


                                } else {

                                    listOfInvoiceTestDetails.add(new InvoiceDetails3(contractId, serviceDataId, flowToTest));
                                    listOfInvoiceTestDetailsCount++;
                                }
                            } else {
                                listOfInvoiceTestDetails.add(new InvoiceDetails3(contractId, serviceDataId, flowToTest));
                                listOfInvoiceTestDetailsCount++;
                            }

                        }

                    } else {
                        logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                        csAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    }
                } else {
                    logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                    csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                }
            }

        } catch (Exception e) {
            logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());

            csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
        }

        csAssert.assertAll();
    }

//    @AfterMethod(groups = "part1")
//    public void afterPart1(){
//        testCaseCount--;
//    }

    @Test(enabled = true)
    public void testInvoiceFlowPart2() {

        CustomAssert csAssert = new CustomAssert();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        String flowToTest = "";
        String serviceDataType = "";

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        do {
            try {

                int contractId = -1;
                int consumptionId = -1;

                ArrayList<Integer> consumptionIds = new ArrayList<>();
                if (listOfInvoiceTestDetails.size() > 0) {
                    contractId = listOfInvoiceTestDetails.get(0).getContractId();
                    int serviceDataId = listOfInvoiceTestDetails.get(0).getServiceDataId();
                    flowToTest = listOfInvoiceTestDetails.get(0).getFlowToTest();

//                    if (!flowToTest.equals("arc rrc unavailable")) {

                    serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                            "servicedatatype");


                    // function to get status whether consumptions have been created or not
                    String consumptionCreatedStatus = waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);

                        csAssert.assertAll();
                        return;
                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);

                        csAssert.assertAll();
                        throw new SkipException("Skipping this test");
                    }

                    // after consumptions have been created successfully
                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                            "finalconsumptionvalues").trim().split(Pattern.quote(","));

                    if (!(flowToTest.equals("arc flow 6") || flowToTest.equals("forecast flow 4") ||
                            flowToTest.equals("vol pricing flow consumption unavailable"))) {
                        for (int i = 0; i < consumptionIds.size(); i++) {
                            boolean result = updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                            if (!result) {
                                logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
                                csAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                        "Hence skipping validation");

                                csAssert.assertAll();
                                return;

                            } else {
                                if (!(flowToTest.equals("arc flow 7") || flowToTest.equals("forecast flow 5")
                                        || flowToTest.equals("vol pricing flow consumption unapproved"))) {
//																	result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds.get(i), consumptionEntity, consumptionEntitySectionUrlName);
                                    result = workflowActionsHelper.performWorkflowAction(consumptionEntityTypeId, consumptionIds.get(i), approveAction);
                                    csAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptionEntity + "having id : " + consumptionIds.get(i));

                                    if (!result) {
                                        logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
                                        csAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
                                                "Hence skipping validation");

                                        csAssert.assertAll();
                                        return;
                                    }
                                }
                            }

                        }

                    }
                    if (consumptionIds.size() != 0) {
                        consumptionId = consumptionIds.get(0);
                    } else {
                        consumptionId = -1;
                    }
                    InvoiceHelper invoiceHelper = new InvoiceHelper();


                    String expAmtInSDCurrencyBaseCharge = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"billing record sd amount base charge line item");
                    String expAmtInSDCurrencyArc = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"billing record sd amount arc line item");
                    Thread.sleep(5000);logger.info("Waiting for  5 seconds for billing record generation");
                    HashMap<String,Map<String,String>> billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, csAssert);
                    int expNoOfRecords =2;
                    if(billingRecords.size() == 0){
                        logger.error("Billing Records not generated for the flow " + flowToTest);
                        csAssert.assertTrue(false,"Billing Records not generated for the flow " + flowToTest);

                    }else if(billingRecords.size() != expNoOfRecords){
                        logger.error("Expected billing records for the flow is " + expNoOfRecords + "");
                        csAssert.assertTrue(false,"Billing Records not generated for the flow " + flowToTest);
                    }else {
                        Map<String,String> indvBillingRecord;
                        Boolean arcRrcBillingRecordFound = false;
                        Boolean baseChargeBillingRecordFound = false;
                        for (Map.Entry<String, Map<String,String>> entry : billingRecords.entrySet()) {

                            indvBillingRecord = entry.getValue();

                            if(indvBillingRecord.containsKey("lineitemtype")) {

                                if (indvBillingRecord.get("lineitemtype").equals("ARC-RRC and Transaction Based")) {
                                    arcRrcBillingRecordFound = true;
                                    if (indvBillingRecord.get("amountInServiceDataCurrency").equals(expAmtInSDCurrencyArc)) {
                                        logger.info("AmountInServiceDataCurrency Column validated successfully for the billing record of ARC-RRC and Transaction Based line item type with service data id " + serviceDataId + " for the flow " + flowToTest);
                                    } else {
                                        logger.error("AmountInServiceDataCurrency Column validated unsuccessfully for the billing record of ARC-RRC and Transaction Based line item type with service data id " + serviceDataId + " for the flow " + flowToTest);
                                        csAssert.assertTrue(false, "AmountInServiceDataCurrency Column validated unsuccessfully for the billing record of ARC-RRC and Transaction Based line item type with service data id " + serviceDataId + " for the flow " + flowToTest + " Expected " + expAmtInSDCurrencyArc + " Actual " + indvBillingRecord.get("amountInServiceDataCurrency") );
                                    }
                                } else if (indvBillingRecord.get("lineitemtype").equals("Base Charges and Passthrough")) {
                                    baseChargeBillingRecordFound = true;
                                    if (indvBillingRecord.get("amountInServiceDataCurrency").equals(expAmtInSDCurrencyBaseCharge)) {
                                        logger.info("AmountInServiceDataCurrency Column validated successfully for the billing record of Base Charge line item type with service data id " + serviceDataId + " for the flow " + flowToTest);
                                    } else {
                                        logger.error("AmountInServiceDataCurrency Column validated unsuccessfully for the billing record of Base Charge line item type with service data id " + serviceDataId + " for the flow " + flowToTest);
                                        csAssert.assertTrue(false, "AmountInServiceDataCurrency Column validated unsuccessfully for the billing record of Base Charge line item type with service data id " + serviceDataId + " for the flow " + flowToTest);
                                    }
                                }
                            }else {
                                csAssert.assertTrue(false,"Line Item Type Column not found in billing record");
                            }
                        }

                        if(!baseChargeBillingRecordFound){
                            csAssert.assertTrue(false,"Base Charge Line Item Billing Record not found ");
                        }

                        if(!arcRrcBillingRecordFound){
                            csAssert.assertTrue(false,"ARC /RRC  Line Item Billing Record not found");
                        }
                    }

                    listOfInvoiceTestDetails.remove(0);
                }
            } catch (Exception e) {
                logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());

                csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }



        } while (listOfInvoiceTestDetails.size() != 0);

        csAssert.assertAll();

    }

    private void verifyInvoiceLineItem(String flowToTest, int invoiceLineItemId, CustomAssert csAssert) {
        if(!checkLIValidation)
            return;
        try {
            logger.info("Verifying Invoice Line Item for Flow [{}] having Id {}.", flowToTest, invoiceLineItemId);
            int invoiceLineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity);
            long timeSpent = 0;

            while (timeSpent <= lineItemValidationTimeOut && ShowHelper.isLineItemUnderOngoingValidation(invoiceLineItemEntityTypeId, invoiceLineItemId)) {
                logger.info("Invoice Line Item having Id {} is still Under Ongoing Validation. Waiting for it to finish validation.", invoiceLineItemId);
                logger.info("time spent is : [{}]", timeSpent);
                Thread.sleep(10000);
                timeSpent += 10000;
            }

            if (timeSpent < lineItemValidationTimeOut) {
                //Line Item Validation is Completed.
                String expectedResult = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "expectedresult");
                String actualResult = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invoiceLineItemId, "validationStatus");
                logger.info("Expected Result is [{}] and Actual Result is [{}].", expectedResult, actualResult);
                csAssert.assertTrue(actualResult.trim().toLowerCase().contains(expectedResult.trim().toLowerCase()),
                        "Invoice Line Item Validation failed as Expected Value is " + expectedResult + " and Actual Value is " + actualResult + " for the flow " + flowToTest);
            } else {
                //Line Item Validation is not yet Completed.
                logger.info("Invoice Line Item Validation couldn't be completed for Flow [{}] within TimeOut {} milliseconds", flowToTest, lineItemValidationTimeOut);
                if (failTestIfLineItemValidationNotCompletedWithinTimeOut) {
                    logger.error("FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [{}]", flowToTest);
                    csAssert.assertTrue(false, "FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [" +
                            flowToTest + "]");
                } else {
                    logger.info("FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned off. Hence not failing flow [{}]", flowToTest);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Invoice Line Item for Flow [{}] having Id {}. {}", flowToTest, invoiceLineItemId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while verifying Invoice Line Item for Flow [" + flowToTest + "] having Id " + invoiceLineItemId +
                    ". " + e.getMessage());
        }
    }

    @BeforeClass
    @Parameters({"run type"})
    public void getTestingTypeFlowsName(@Optional("regression") String param) {
        if (param.equalsIgnoreCase("sanity")) {
            flowsToValidate = "flowstovalidatesanity";
            testAllFlows = "testallflowssanity";
        } else {
            flowsToValidate = "flowstovalidate";
            testAllFlows = "testallflows";
        }
        logger.info("Setting Parameters for {}", param);
    }
    @BeforeClass
    @Parameters({"line item validation"})
    public void checkValidation(@Optional("no") String param) {
        checkLIValidation = param.equalsIgnoreCase("yes");
        logger.info("Setting Parameters for {}", param);
    }

    private List<String> getFlowsToTest() {
        List<String> flowsToTest = new ArrayList<>();

        try {

            String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows to test").split(Pattern.quote(","));
            for (String flow : allFlows) {
                if (ParseConfigFile.containsSection(flowsConfigFilePath, flowsConfigFileName, flow.trim())) {
                    flowsToTest.add(flow.trim());
                } else {
                    logger.info("Flow having name [{}] not found in Invoice Config File.", flow.trim());
                }
            }

        } catch (Exception e) {
            logger.error("Exception while getting Flows to Test for Invoice Validation. {}", e.getMessage());
        }
        return flowsToTest;
    }

    private synchronized int getContractId(String flowToTest) {
        int contractId = -1;
        String contractSectionName = "default";

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname");
            if (temp != null)
                contractSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "createcontract");
            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractid"));
            } else {
                //Create New Contract
                Boolean createLocalContract = true;
                temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "createlocalcontract");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    createLocalContract = false;

                String createResponse = Contract.createContract(contractConfigFilePath, contractConfigFileName, contractConfigFilePath,
                        contractExtraFieldsConfigFileName, contractSectionName, createLocalContract);

                contractId = CreateEntity.getNewEntityId(createResponse, "contracts");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Contract Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return contractId;
    }

    private synchronized int getServiceDataId(String flowToTest, int contractId) {
        int serviceDataId = -1;
        String serviceDataSectionName = "default";
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "createservicedata");

            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, configFileName, flowToTest, "servicedataid"));
            } else {
                //Create New Service Data
                temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                if (temp != null)
                    serviceDataSectionName = temp.trim();

                //Update Service Data Extra Fields.
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                        "new client", "newClient" + contractId);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                        "new supplier", "newSupplier" + contractId);

                Boolean createLocalServiceData = true;
                temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "createlocalservicedata");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    createLocalServiceData = false;

                String createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                        serviceDataExtraFieldsConfigFileName, serviceDataSectionName, createLocalServiceData);

                serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

                //Revert Service Data Extra Fields changes.
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                        "newClient" + contractId, "new client");
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                        "newSupplier" + contractId, "new supplier");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return serviceDataId;
    }

    private synchronized int getInvoiceId(String flowToTest) {
        int invoiceId = -1;
        String invoiceSectionName = "default";
        String invoiceLineItemSectionName = "default";

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
            if (temp != null)
                invoiceSectionName = temp.trim();

            String createResponse = Invoice.createInvoice(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath, invoiceExtraFieldsConfigFileName,
                    invoiceSectionName, true);
            invoiceId = CreateEntity.getNewEntityId(createResponse, "invoice");

            logger.info("Updating Invoice Line Item Config File for Flow [{}] and Invoice Id {}.", flowToTest, invoiceId);
            int invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
            String invoiceName = ShowHelper.getValueOfField(invoiceEntityTypeId, invoiceId, "title");

            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();

            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemSectionName, "sourcename",
                    invoiceName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemSectionName, "sourceid",
                    String.valueOf(invoiceId));
        } catch (Exception e) {
            logger.error("Exception while getting Invoice Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return invoiceId;
    }

    private synchronized int getInvoiceLineItemId(String flowToTest, int serviceDataId) {
        int lineItemId = -1;
        String invoiceLineItemSectionName = "default";
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();

            logger.info("Updating Invoice Line Item Property Service Id Supplier in Extra Fields Config File for Flow [{}] and Service Data Id {}.",
                    invoiceLineItemSectionName, serviceDataId);
            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName(serviceDataEntity);
            String serviceDataName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "title");
            String serviceIdSupplierName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "serviceIdSupplier");
            String serviceIdSupplierUpdatedName = serviceDataName + " (" + serviceIdSupplierName + ")";
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new name", serviceIdSupplierUpdatedName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new id", String.valueOf(serviceDataId));

            String createResponse = InvoiceLineItem.createInvoiceLineItem(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemConfigFilePath,
                    invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, true);
            lineItemId = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

            if(lineItemId == -1){
                logger.error("Create Response when line item not created for the flow " + flowToTest + " " + createResponse);
            }
            //Reverting Invoice Line Item Extra Fields changes.
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", serviceIdSupplierUpdatedName, "new name");
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", String.valueOf(serviceDataId), "new id");
        } catch (Exception e) {
            logger.error("Exception while getting Invoice Line Item Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return lineItemId;
    }

    private synchronized Boolean downloadAndEditPricingFile(String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj) {
        boolean pricingFile = false;
        try {
            //Download Pricing Template
            logger.info("Downloading Pricing Template for Flow [{}]", flowToTest);
            Map<String,String> formData = new HashMap<>();
            formData.put("contractId","");
            formData.put("relationId","");
            formData.put("entityIds",String.valueOf(serviceDataId));
            formData.put("_csrf_token",null);

            if (!pricingObj.downloadPricingTemplate(pricingTemplateFilePath, templateFileName, serviceDataId)) {
                logger.error("Pricing Template Download failed for Flow [{}].", flowToTest);
                return false;
            }

            //Edit Pricing Template
            logger.info("Editing Pricing Template for Flow [{}]", flowToTest);

            String sheetName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "pricingstemplatesheetname");
            Integer totalRowsToEdit = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "numberofrowstoedit"));
            Integer startingRowNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "startingrownum"));
            Integer volumeColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "volumecolumnnum"));
            Integer rateColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "ratecolumnnum"));
            List<String> volumeColumnValues = this.getIdList(flowsConfigFilePath, flowsConfigFileName, flowToTest, "volumecolumnvalues");
            List<String> rateColumnValues = this.getIdList(flowsConfigFilePath, flowsConfigFileName, flowToTest, "ratecolumnvalues");

            int count = 0;
            for (int rowNum = startingRowNum; rowNum < (startingRowNum + totalRowsToEdit); rowNum++) {
                Map<Integer, Object> columnNumAndValueMap = new HashMap<>();
                columnNumAndValueMap.put(volumeColumnNum, volumeColumnValues.get(count));
                columnNumAndValueMap.put(rateColumnNum, rateColumnValues.get(count));

                Boolean isSuccess = pricingObj.editPricingTemplate(pricingTemplateFilePath, templateFileName, sheetName, rowNum, columnNumAndValueMap);

                count++;
                if (!isSuccess) {
                    logger.error("Pricing Template Editing Failed for Flow [{}].", flowToTest);
                    pricingFile = false;
                    break;
                } else
                    pricingFile = true;
            }
        } catch (Exception e) {
            logger.error("Exception while getting Pricing Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;
    }


    private List<String> getIdList(String configFilePath, String configFileName, String sectionName, String propertyName) throws ConfigurationException {

        String value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName);
        List<String> idList = new ArrayList<>();

        if (!value.trim().equalsIgnoreCase("")) {
            String ids[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName).split(",");

            for (String id : ids)
                idList.add(id.trim());
        }
        return idList;
    }

    private void killAllSchedulerTasks() {
        UserTasksHelper.removeAllTasks();
    }

    /*
    This method will return the status of Pricing Scheduler as String.
    Possible Values are 'Pass', 'Fail', 'Skip'.
    'Pass' specifies that pricing scheduler completed and records processed successfully.
    'Fail' specifies that pricing scheduler failed
    'Skip' specifies that pricing scheduler didn't finish within time.
     */
    private String waitForPricingScheduler(String flowToTest, List<Integer> oldIds) {
        String result = "pass";
        logger.info("Waiting for Pricing Scheduler to Complete for Flow [{}].", flowToTest);
        try {
            logger.info("Time Out for Pricing Scheduler is {} milliseconds", pricingSchedulerTimeOut);
            long timeSpent = 0;
            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            logger.info("Getting Task Id of Pricing Upload Job");
            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), oldIds);

            if (newTaskId != -1) {
                Boolean taskCompleted = false;
                logger.info("Checking if Pricing Upload Task has completed or not.");

                while (timeSpent < pricingSchedulerTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    fetchObj.hitFetch();
                    logger.info("Getting Status of Pricing Upload Task.");
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        taskCompleted = true;
                        logger.info("Pricing Upload Task Completed. ");
                        logger.info("Checking if Pricing Upload Task failed or not.");
                        if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                            result = "fail";

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Pricing Upload Task is not finished yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= pricingSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }
            } else {
                logger.info("Couldn't get Pricing Upload Task Job Id. Hence waiting for Task Time Out i.e. {}", pricingSchedulerTimeOut);
                Thread.sleep(pricingSchedulerTimeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Pricing Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }
        return result;
    }

    private synchronized void updateServiceDataAndInvoiceConfig(String flowToTest, int contractId) {
        String serviceDataSectionName = "default";
        String invoiceSectionName = "default";
        try {
            if (contractId != -1) {
                logger.info("Updating Service Data Config File for Flow [{}] and Contract Id {}.", flowToTest, contractId);
                String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                if (temp != null)
                    serviceDataSectionName = temp.trim();

                int contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
                String contractName = ShowHelper.getValueOfField(contractEntityTypeId, contractId, "title");
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourceid",
                        String.valueOf(contractId));

                temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                if (temp != null)
                    invoiceSectionName = temp.trim();

                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, "sourceid", String.valueOf(contractId));
            }
        } catch (Exception e) {
            logger.error("Exception while updating Service Data & Invoice Config File for Flow [{}] and Contract Id {}. {}", flowToTest, contractId, e.getStackTrace());
        }
    }

    // this method will check whether consumption has been created or not
    // return "pass" if created
    // return "fail" if something went wrong in code or assumption
    // return "skip" if consumption would not created in specified time consumptionToBeCreatedTimeOut
    private String waitForConsumptionToBeCreated(String flowToTest, int serviceDataId, ArrayList<Integer> consumptionIds) {
        int offset = 0; // default value for creating payload
        int size = 20; // default value for creating payload

        Show show = new Show();
        String result = "pass";
        logger.info("Waiting for Consumption to be Created for Flow [{}].", flowToTest);
        try {
            show.hitShow(serviceDataEntityTypeId, serviceDataId);
            String showPageResponseStr = show.getShowJsonStr();
            List<String> dataUrl = show.getShowPageTabUrl(showPageResponseStr, Show.TabURL.dataURL);

            String consumptionDataURL = null;
            for (String Url : dataUrl) {
                if (Url.contains("listRenderer/list/376/tablistdata")) // for consumption
                {
                    consumptionDataURL = Url;
                    break;
                } else
                    continue;

            }


            if (consumptionDataURL != null) {

                logger.info("Time Out for Consumption to be created is {} milliseconds", consumptionToBeCreatedTimeOut);
                long timeSpent = 0;


                Boolean taskCompleted = false;
                logger.info("Checking if Consumption has been created or not.");

                String payload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":" +
                        offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc\",\"filterJson\":{}}}";

                while (timeSpent < consumptionToBeCreatedTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);
                    logger.info("Hitting tab data API for Consumption for service data Id : [{}]", serviceDataId);

                    String showPageDataUrlResponseStr = Show.hitshowPageTabUrl(consumptionDataURL, payload);
                    JSONObject showPageDataUrlResponseJson = new JSONObject(showPageDataUrlResponseStr);

                    int numberOfConsumption = showPageDataUrlResponseJson.getJSONArray("data").length();


                    if (numberOfConsumption > 0) {
                        taskCompleted = true;
                        logger.info("Consumptions have been created. ");

                        for (int i = 0; i < numberOfConsumption; i++) {

                            JSONObject data = showPageDataUrlResponseJson.getJSONArray("data").getJSONObject(i);
                            Set<String> keys = data.keySet();
                            for (String key : keys) {
                                String columnName = data.getJSONObject(key).getString("columnName");
                                if (columnName.contentEquals("id")) {
//									consumptionIds[i] = Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]);
                                    consumptionIds.add(Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]));
                                    break;
                                } else
                                    continue;

                            }
                        }

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("time spent is : [{}]", timeSpent);
                        logger.info("Consumptions haven't been created yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= pricingSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }


            } else {
                logger.error("There is no consumption URL in the show page response of Service data : [{}] for flow : [{}]", serviceDataId, flowToTest);
                result = "skip";
                return result;
            }


        } catch (Exception e) {
            logger.error("Exception while Waiting for Consumption to get created to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }


        return result;
    }

    // this will update the final Consumption in Created Consumption
    private boolean updateFinalConsumption(String flowToTest, int consumptionId, double finalConsumption) {
        boolean result = true;

        Edit edit = new Edit();
        try {
            String editAPIResponse = edit.hitEdit(consumptionEntity, consumptionId);
            JSONObject editAPIResponseJson = new JSONObject(editAPIResponse);

            JSONObject editPostAPIPayload = editAPIResponseJson;
            editPostAPIPayload.getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption").put("values", finalConsumption);

            logger.info("editPostAPIPayload is : {}", editPostAPIPayload);

            String editPostAPIResponse = edit.hitEdit(consumptionEntity, editPostAPIPayload.toString());
            JSONObject editPostAPIResponseJson = new JSONObject(editPostAPIResponse);

            if ((editPostAPIResponseJson.has("header") && editPostAPIResponseJson.getJSONObject("header").has("response")
                    && editPostAPIResponseJson.getJSONObject("header").getJSONObject("response").has("status")
                    && editPostAPIResponseJson.getJSONObject("header").getJSONObject("response").getString("status")
                    .trim().equalsIgnoreCase("success"))) {
                logger.info("Consumption has been updated successfully for flow [{}]", flowToTest);
            } else {
                logger.error("Error While Updating final Consumption in Created Consumption having id : [{}] for flow [{}]", consumptionId, flowToTest);
                result = false;
                return result;

            }


        } catch (Exception e) {
            logger.error("Error While Updating final Consumption in Created Consumption having id : [{}]", consumptionId);
            result = false;
            return result;

        }

        return result;
    }

    // this will create the row , <columnNumber,value> for editing the ARC/RRC Sheet
    private Map<Integer, Map<Integer, Object>> getValuesMapForArcRrcSheet(String flowToTest) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {


            int numberOfColumnToEditForEachRow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "numberofcolumntoeditforeachrowforarc"));

            String[] arcRowNumber = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "arcrownumber").trim().split(Pattern.quote(","));

            String[] arcColumnNumber = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "arccolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
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

    // this function will edit the ARC/RRC sheet based on the map created in getValuesMapForArcRrcSheet
    private synchronized Boolean editPricingFileForARCRRC(String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj) {
        Boolean pricingFile = false;
        try {

            Map<Integer, Map<Integer, Object>> arcValuesMap = getValuesMapForArcRrcSheet(flowToTest);
            boolean editTemplate = pricingObj.editPricingTemplateMultipleRows(pricingTemplateFilePath, templateFileName, ARCRRCSheetNameInXLSXFile,
                    arcValuesMap);

            if (editTemplate) {
                return true;
            } else {
                logger.error("Error While Updating the Pricing Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
                return false;
            }


        } catch (Exception e) {
            logger.error("Exception while getting ARC/RRC Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;
    }




    // this function will check whether any data has been created under ARC/RRC tab of Service Data or Not
    boolean isARCRRCCreated(int serviceDataId) {

        logger.info("Checking whether data under ARR/RRC tab has/have been created and visible for serviceData" + serviceDataId);
        GetTabListData getTabListData = new GetTabListData(serviceDataEntityTypeId, ARCRRCTabId, serviceDataId);
        getTabListData.hitGetTabListData();
        String ARCRRCTabListDataResponse = getTabListData.getTabListDataResponse();


        boolean isListDataValidJson = APIUtils.validJsonResponse(ARCRRCTabListDataResponse, "[ARR/RRC tab list data response]");


        if (isListDataValidJson) {


            JSONObject jsonResponse = new JSONObject(ARCRRCTabListDataResponse);
            int filterCount = jsonResponse.getInt("filteredCount");
            if (filterCount > 0) //very lenient check cab be modified
            {
                return true;
            } else {
                logger.error("There is no data in ARR/RRC tab for Service Data Id : [{}]", serviceDataId);
                return false;
            }


        } else {
            logger.error("ARR/RRC tab List Data Response is not valid Json for Service Data  Id :[{}] ", serviceDataId);
            return false;

        }

    }

    // this function will check whether any data has been created under Charges tab of Service Data or Not
    boolean isChargesCreated(int serviceDataId) {

        logger.info("Checking whether data under Charges tab has/have been created and visible for serviceData" + serviceDataId);
        GetTabListData getTabListData = new GetTabListData(serviceDataEntityTypeId, chargesTabId, serviceDataId);
        getTabListData.hitGetTabListData();
        String chargesTabListDataResponse = getTabListData.getTabListDataResponse();


        boolean isListDataValidJson = APIUtils.validJsonResponse(chargesTabListDataResponse, "[Charges tab list data response]");


        if (isListDataValidJson) {


            JSONObject jsonResponse = new JSONObject(chargesTabListDataResponse);
            int filterCount = jsonResponse.getInt("filteredCount");
            if (filterCount > 0) //very lenient check cab be modified
            {
                return true;
            } else {
                logger.error("There is no data in Charges tab for Service Data Id : [{}]", serviceDataId);
                return false;
            }


        } else {
            logger.error("Charges tab List Data Response is not valid Json for Service Data  Id :[{}] ", serviceDataId);
            return false;

        }

    }

    private void validateBillingDataScenarios(String flowToTest,int serviceDataId,int consumptionId,String invoiceId,
                                              String invoiceLineItemId,CustomAssert customAssert){

        InvoiceHelper invoiceHelper = new InvoiceHelper();
        try {
            String billingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFilePath");
            String billingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFileName");


//            if(flowToTest.equals("")) {

            Map<String, Map<String, String>> billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);

            if(billingRecords.size() == 0){
                logger.error("Billing Records not generated for the flow " + flowToTest);
                customAssert.assertTrue(false,"Billing Records not generated for the flow " + flowToTest);
                return;
            }
            invoiceHelper.validateBillingDataListingPageData(serviceDataId, consumptionId, String.valueOf(invoiceId), String.valueOf(invoiceLineItemId),
                    billingConfigFilePath,billingConfigFileName,
                    billingRecords,false,flowToTest, customAssert);


//            }
        }catch (Exception e){
            logger.error("Exception while validating billing data scenarios for the flow " + flowToTest);
            customAssert.assertTrue(false,"Exception while validating billing data scenarios for the flow " + flowToTest);
        }

//        private Boolean


    }
}
