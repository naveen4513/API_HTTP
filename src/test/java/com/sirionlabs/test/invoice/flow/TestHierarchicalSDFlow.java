package com.sirionlabs.test.invoice.flow;



import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;

import com.sirionlabs.api.invoice.InvoicePricingTemplateDownload;
import com.sirionlabs.api.listRenderer.ListRendererListData;

import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;


import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.invoice.InvoiceHelper;

import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;


class InvoiceDetails1 {
    public InvoiceDetails1(int contractId, int serviceDataId, String flowToTest) {
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


public class TestHierarchicalSDFlow extends TestRailBase {
    private final static Logger logger = LoggerFactory.getLogger(TestHierarchicalSDFlow.class);
    private static String invoicePricingHelperConfigFilePath = null;
    private static String invoicePricingHelperConfigFileName = null;
    private static Boolean failTestIfLineItemValidationNotCompletedWithinTimeOut = true;
    private static Long lineItemValidationTimeOut = 1200000L;
    private static Long pricingSchedulerTimeOut = 1200000L;
    private static Long forcastSchedulerTimeOut = 1200000L;
    private static Long consumptionToBeCreatedTimeOut = 1200000L;
    private static Long pollingTime = 5000L;
    int forecastTabId = 313; // hardcoded value @todo
    int ARCRRCTabId = 311; // hardcoded value @todo
    int chargesTabId = 309; // hardcoded value @todo

    private String configFilePath;
    private String configFileName;
//    private String flowsConfigFilePath;
//    private String flowsConfigFileName;
    private Boolean killAllSchedulerTasks = false;
    private String pricingTemplateFilePath;
    private String forecastTemplateFilePath;
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
    private int contractEntityTypeId = -1;
    private int invoiceLineItemEntityTypeId = -1;
    private int consumptionsEntityTypeId = -1;
    private int invoiceEntityTypeId = 67;
    private int cdrEntityTypeId = 160;
    private int changeRequestEntityTypeId = 63;

    //	private Show show;
    private String publishAction = "publish";
    private String approveAction = "approve";

    private String entityIdMappingFileName;
    private String baseFilePath;
    //	private Edit edit;
    private String serviceDataEntity = "service data";
    private String consumptionEntity = "consumptions";
    private String invoiceLineItemEntity = "invoice line item";
    private String contractEntity = "contracts";
    private String changeRequestEntity = "change requests";

    private String forecastSheetNameInXLSXFile = "Forecast Data";
    private String ARCRRCSheetNameInXLSXFile = "ARCRRC";

    private Map<String, String> flowIdContractIdMap = new HashMap<>();
    private Map<String, String> flowIdServiceDataIdMap = new HashMap<>();

    private String approveInvoice = "ApproveInvoice";
    private String approveLineItem = "Approve";

    private int consumptionEntityTypeId;
    private boolean checkLIValidation=true;

    @BeforeClass
    public void beforeClass() throws Exception {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataHeirarchyConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataHeirarchyConfigFileName");
        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");
        forecastTemplateFilePath = pricingTemplateFilePath;
//        flowsConfigFilePath = configFilePath;
//        flowsConfigFileName = configFileName;

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

        invoicePricingHelperConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFilePath");
        invoicePricingHelperConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFileName");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killallschedulertasks");
        if (temp != null && temp.trim().equalsIgnoreCase("true"))
            killAllSchedulerTasks = true;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            failTestIfJobNotCompletedWithinSchedulerTimeOut = false;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfLineItemValidationNotCompletedWithinTimeOut");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            failTestIfLineItemValidationNotCompletedWithinTimeOut = false;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lineItemValidationTimeOut");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            lineItemValidationTimeOut = Long.parseLong(temp);

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingSchedulerWaitTimeOut");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            pricingSchedulerTimeOut = Long.parseLong(temp);

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingschedulerpollingtime");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            pollingTime = Long.parseLong(temp);


        // for publising of service data
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));

        //rate card specific configs
        invoiceLineItemEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "entity_type_id"));
        consumptionsEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "entity_type_id"));
        contractEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, contractEntity, "entity_type_id"));
        consumptionEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "entity_type_id"));

    }

//    http://wiki.sirionlabs.office/display/PM/Invoice+Validations
//    Hierarchical Service Data
//    C44287

//    @Test(enabled = false)
//    public void testHeirarchialSDFlow() {
//
//        CustomAssert csAssert = new CustomAssert();
//
//        int contractId = -1;
//        int serviceDataId = -1;
//        int invoiceId = -1;
//        int invoiceLineItemId = -1;
//
//        int currencyId;
//        int dateType;
//        int conMatrixId;
//
//        ArrayList<Integer> consumptionId1 = new ArrayList<>();
//        ArrayList<Integer> consumptionId2 = new ArrayList<>();
//        ArrayList<Integer> consumptionId3 = new ArrayList<>();
//
//        String flowToTest = "hierarchical flow";
//
//        try {
//
//            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//
//            //Get Contract that will be used for Invoice Flow Validation
//            synchronized (this) {
//                contractId = getContractId(flowToTest);
//
//                if (contractId != -1) {
//                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
//                    flowIdContractIdMap.put(flowToTest, String.valueOf(contractId));
//
//                    updateServiceDataAndInvoiceConfig(flowToTest, contractId);
//
////                  ****************************Parent Service Data**************************************************
//                    serviceDataId = getServiceDataId(flowToTest, contractId);
//
//                    logger.info("Created Service Data Id : [{}]", serviceDataId);
//
////                        Level 2 starts here
//
//                    conMatrixId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,flowToTest,"conv matrix id level 2"));
//                    dateType = 1;
//                    currencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,flowToTest,"currency id level 2"));
//
//                    if (serviceDataId != -1) {
//                        logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
//                        flowIdServiceDataIdMap.put(flowToTest, String.valueOf(serviceDataId));
//
//                        int serviceDataIdParent = serviceDataId;
//
//                        int newServiceDataIdChild11 = getSDFromClone(serviceDataIdParent, false, true, false, "_Child11",conMatrixId,dateType,currencyId);
//
//                        if (newServiceDataIdChild11 == -1) {
//                            logger.error("Unable to create child service data 11");
//                            csAssert.assertTrue(false, "Unable to create child service data11");
//                            csAssert.assertAll();
//                            return;
//                        }
//
//
//                        int newServiceDataIdChild12 = getSDFromClone(serviceDataIdParent, false, true, false, "_Child12",conMatrixId,dateType,currencyId);
//
//                        if (newServiceDataIdChild12 == -1) {
//                            logger.error("Unable to create child service data 12");
//                            csAssert.assertTrue(false, "Unable to create child service data 12");
//                            csAssert.assertAll();
//                            return;
//                        }
//
////                      Level 2 ends here
//
////                      Level 3 starts here
//                        conMatrixId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,flowToTest,"conv matrix id level 3"));
//                        dateType = 1;
//                        currencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,flowToTest,"currency id level 3"));
//
//                        int newServiceDataIdChild21 = getSDFromClone(newServiceDataIdChild11, false, false, true, "_Child21",conMatrixId,dateType,currencyId);
//
//                        if (newServiceDataIdChild21 == -1) {
//                            logger.error("Unable to create child service data 21");
//                            csAssert.assertTrue(false, "Unable to create child service data21");
//                            csAssert.assertAll();
//                            return;
//                        }
//
//                        int newServiceDataIdChild22 = getSDFromClone(newServiceDataIdChild11, false, false, true, "_Child22",conMatrixId,dateType,currencyId);
//
//                        if (newServiceDataIdChild22 == -1) {
//                            logger.error("Unable to create child service data 22");
//                            csAssert.assertTrue(false, "Unable to create child service data 22");
//                            csAssert.assertAll();
//                            return;
//                        }
//
//                        int newServiceDataIdChild23 = getSDFromClone(newServiceDataIdChild12, false, false, true, "_Child22",conMatrixId,dateType,currencyId);
//
//                        if (newServiceDataIdChild23 == -1) {
//                            logger.error("Unable to create child service data 23");
//                            csAssert.assertTrue(false, "Unable to create child service data 23");
//                            csAssert.assertAll();
//                            return;
//                        }
//
//                        boolean result1 = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, newServiceDataIdChild21, publishAction);
//                        boolean result2 = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, newServiceDataIdChild22, publishAction);
//                        boolean result3 = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, newServiceDataIdChild23, publishAction);
//
//                        if (result1 == false || result2 == false || result3 == false) {
//
//                            logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
//
//                            csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
//                                    "Hence skipping validation");
//
//                            csAssert.assertAll();
//                            return;
//
//                        }
//
//                        String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
//                                "finalconsumptionvalues").trim().split(Pattern.quote(","));
//
//
//                        String consumptionCreatedStatus1 = waitForConsumptionToBeCreated(flowToTest, newServiceDataIdChild21, consumptionId1);
//                        String consumptionCreatedStatus2 = waitForConsumptionToBeCreated(flowToTest, newServiceDataIdChild22, consumptionId2);
//                        String consumptionCreatedStatus3 = waitForConsumptionToBeCreated(flowToTest, newServiceDataIdChild23, consumptionId3);
//
//                        if (consumptionCreatedStatus1.trim().equalsIgnoreCase("fail") || consumptionCreatedStatus2.trim().equalsIgnoreCase("fail") ||
//                                consumptionCreatedStatus3.trim().equalsIgnoreCase("fail")) {
//
//                            csAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow ");
//                            csAssert.assertAll();
//                            return;
//                        } else if ((consumptionCreatedStatus1.trim().equalsIgnoreCase("skip") || consumptionCreatedStatus2.trim().equalsIgnoreCase("skip") ||
//                                consumptionCreatedStatus3.trim().equalsIgnoreCase("skip"))) {
//                            logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
//                            csAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow ");
//                            csAssert.assertAll();
//                            throw new SkipException("Skipping this test");
//                        }
//
//                        for (int i = 0; i < consumptionId1.size(); i++) {
//                            result1 = updateFinalConsumption(flowToTest, consumptionId1.get(i), Double.parseDouble(finalConsumptions[i]));
//                            result2 = updateFinalConsumption(flowToTest, consumptionId2.get(i), Double.parseDouble(finalConsumptions[i]));
//                            result3 = updateFinalConsumption(flowToTest, consumptionId3.get(i), Double.parseDouble(finalConsumptions[i]));
//
//                            if (result1 == false || result2 == false || result3 == false) {
//
//                                csAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + i + "  " + flowToTest + "]. " +
//                                        "Hence skipping validation");
//
//                                csAssert.assertAll();
//                                return;
//
//                            } else {
//                                result1 = workflowActionsHelper.performWorkflowAction(consumptionEntityTypeId, consumptionId1.get(i), approveAction);
//                                result2 = workflowActionsHelper.performWorkflowAction(consumptionEntityTypeId, consumptionId2.get(i), approveAction);
//                                result3 = workflowActionsHelper.performWorkflowAction(consumptionEntityTypeId, consumptionId3.get(i), approveAction);
//
//                                if (result1 == false || result2 == false || result3 == false) {
//                                    logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
//                                    csAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
//                                            "Hence skipping validation");
//
//                                    csAssert.assertAll();
//                                    return;
//                                }
//                            }
//                        }
//
//                        BigDecimal systemAmt;
//                        BigDecimal systemAmtConv;
//
////                        Level 3 ends here
//                        //Get Invoice Id
//                        invoiceId = getInvoiceId(flowToTest);
//                        invoiceLineItemId = -1;
//                        logger.info("Created Invoice Id is : [{}]", invoiceId);
//                        if (invoiceId != -1) {
//                            //Get Invoice Line Item Id
//                            invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataId);
//
//                            if(invoiceLineItemId == -1){
//                                csAssert.assertTrue(false,"Unable to create invoice line item");
//                            }else {
//                                String finalCons = String.valueOf(Double.parseDouble(finalConsumptions[0]) + Double.parseDouble(finalConsumptions[0]));
//
//                                systemAmt = calcSysAmountARC(newServiceDataIdChild11,finalCons,csAssert);
//
//                                systemAmtConv = calcConvChildToParentSD(newServiceDataIdChild11,systemAmt,invoiceLineItemId,csAssert);
//
//                                finalCons = String.valueOf(Double.parseDouble(finalConsumptions[0]));
//                                systemAmt = calcSysAmountARC(newServiceDataIdChild12,finalCons,csAssert);
//                                systemAmtConv = systemAmtConv.add(calcConvChildToParentSD(newServiceDataIdChild12,systemAmt,invoiceLineItemId,csAssert));
//
//                                String actualSysAmt = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invoiceLineItemId, "systemamount");
//                                if(String.valueOf(systemAmtConv).equals("0") && !actualSysAmt.equals("null")){
//                                    csAssert.assertTrue(false,"During System Amount Validation on Line Item Show Page System Amount expected is 0 and actual Value is not null" + flowToTest);
//                                }else if(!String.valueOf(systemAmtConv.doubleValue()).equalsIgnoreCase(actualSysAmt))
//                                    csAssert.assertTrue(false,"During System Amount Validation on Line Item Show Page Actual and Expected System Amount unmatched for the flow " + flowToTest);
//
//
//                                verifyInvoiceLineItem(flowToTest,invoiceLineItemId,csAssert);
//
//                                InvoiceHelper invoiceHelper = new InvoiceHelper();
//                                HashMap<String,Map<String,String>> billingRec = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId,csAssert);
//
//                                if(billingRec.size() == 0){
//
//                                }
//
//
//                            }
//
//                            logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
//                        } else {
//                            logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
//                            csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
//                        }
//
//
//                    } else {
//                        logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
//                        csAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
//                    }
//                } else {
//                    logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
//                    csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
//                }
//            }
//
//        } catch (Exception e) {
//            logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
//
//            csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
//        }
//
//        csAssert.assertAll();
//    }

    @DataProvider(name = "dataProviderFor_SDHeirarcyFlows")
    public Object[][] dataProviderFor_SDHeirarcyFlows(){

        List<Object[]> allTestData = new ArrayList<>();

        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"test_sdheirarcyflows").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);

    }

    /*
    C153599  Fixed Fee Case
    C153600  Billing Data Generation Case
    C153586  Invoice Validation Case
    C153601  Pricing Memo Scenario  CCR Case
    C153596  Pricing Memo Scenario  CDR Case
    C153597  Pricing Change Billing Record Scenario
    C153434 Check billing records after pricing updation with different versions CCR and CDR
    C90587 C89805 C90587
    C153587  Re validation Case
     */
    @Test(dataProvider = "dataProviderFor_SDHeirarcyFlows")
    public void test_SDHeirarcyFlows(String flowToTest){
        CustomAssert customAssert = new CustomAssert();

        InvoiceHelper invoiceHelper =new InvoiceHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        Boolean consumptionAvail = null;
        try {

            int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "basecontractid"));
            int invoiceId = -1;
            int invLiId1 = -1;
            int invLiId2 = -1;

            int expectedNumOfBilRec = 12;
            ArrayList<Integer> consumptionIdsChild1 = new ArrayList<>();
            ArrayList<Integer> consumptionIdsChild2 = new ArrayList<>();
            Double finalConsumption = 0.0;
            String expAmtSDCurChild1 = "";
            String expAmtSDCurChild2 = "";

            String startDate1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"start date 1");
            String endDate1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"end date 1");
            String startDate2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"start date 2");
            String endDate2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"end date 2");
            String child1ServiceYear = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"child 1 service year");
            String child2ServiceYear = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"child 2 service year");

            String type = "";
            String liValidationSatus = "";
            if (flowToTest.contains("arc flow")) {
                type = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"type");
//                "ARC";
                expAmtSDCurChild1 =ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"exp amt sd cur child 1");
//                "1050";
                expAmtSDCurChild2 =ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"exp amt sd cur child 2");
//                        "4000";
                consumptionAvail = true;
                liValidationSatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"expectedresult");

            } else if (flowToTest.contains("vol")) {
                expAmtSDCurChild1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"exp amt sd cur child 1");
                expAmtSDCurChild2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"exp amt sd cur child 2");
                type = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"type");
                consumptionAvail =true;
                liValidationSatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"expectedresult");
            }else if (flowToTest.contains("fixed")){
//                C153599
                expAmtSDCurChild1 =ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"exp amt sd cur child 1");
//                        "1000";
                expAmtSDCurChild2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"exp amt sd cur child 2");
//                        "4000";
                type = "";
                consumptionAvail = null;
                liValidationSatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"expectedresult");
            }

            if(consumptionAvail != null){
                finalConsumption = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "finalconsumptionvalues"));
            }
            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            int serviceDataIdParent = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, uniqueDataString);

            if (serviceDataIdParent == -1) {
                throw new SkipException("Skipping the test as Parent Service Data not created");
            }

//            startDate = "01-01-" + child1ServiceYear;
//            endDate = "12-31-" + child2ServiceYear;

            invoiceHelper.updateServiceStartAndEndDate(serviceDataIdParent, startDate1, endDate2, customAssert);
            invoiceHelper.updateSDisBill_PriAvail_ConsAvail(serviceDataIdParent, true, false, false, customAssert);

            int childSd1 = createChildSD(serviceDataIdParent, "Child11", false, true, consumptionAvail, customAssert);

            if (childSd1 == -1) {
                logger.error("Child Service Data 1 not created");
                customAssert.assertTrue(false, "Child Service Data 1 not created");
                customAssert.assertAll();
                return;
            }

            invoiceHelper.updateServiceStartAndEndDate(childSd1, startDate1, endDate1, customAssert);
//            int volChild1 = 100;int rateChild1 = 10;int lowVolChild1 = 100;int upVolChild1 = 120;int rateArcChild1 = 10;
            int volChild1 = 100;int rateChild1 = 10;int lowVolChild1 = 100;int upVolChild1 = 120;int rateArcChild1 = 10;
            uploadPricing(childSd1, flowToTest, volChild1, rateChild1, lowVolChild1, upVolChild1, rateArcChild1, type, null, customAssert);

            if (type.equals("ARC") || type.equals("Volume Pricing")) {
                Boolean publishStatus = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, childSd1, publishAction, customAssert);

                if (!publishStatus) {
                    customAssert.assertTrue(false, "Unable to Publish Child Service Data 1");
                } else {

                    String consCreationStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, childSd1, expectedNumOfBilRec, consumptionIdsChild1);
                    if(consCreationStatus.equals("fail")){
                        customAssert.assertTrue(false,"Consumptions not generated for Child Service Data 2");
                        customAssert.assertAll();
                        return;
                    }
                    for (int i = 0; i < consumptionIdsChild1.size(); i++) {

                        InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIdsChild1.get(i), finalConsumption);
                        workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionIdsChild1.get(i), approveAction);
                    }
                }
            }
            Thread.sleep(10000);
            HashMap<String,Map<String,String>> billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataIdParent,customAssert);

            if(billingRecords.size() != expectedNumOfBilRec){
                logger.error("Expected Number Of Billing Records not matched with the actual number of billing records after creation of Child 1 Expected " + expectedNumOfBilRec + " Actual " + billingRecords.size());
                customAssert.assertTrue(false,"Expected Number Of Billing Records not matched with the actual number of billing records Expected " + expectedNumOfBilRec + " Actual " + billingRecords.size());
            }

            int childSd2 = createChildSD(serviceDataIdParent,"Child12",false,true,consumptionAvail,customAssert);

            if(childSd2 == -1){
                logger.error("Child Service Data 2 not created");
                customAssert.assertTrue(false,"Child Service Data 2 not created");
                customAssert.assertAll();
                return;
            }

            invoiceHelper.updateServiceStartAndEndDate(childSd2, startDate2, endDate2, customAssert);
            int volChild2 = 200;int rateChild2 = 20;int lowVolChild2 = 200;int upVolChild2 = 220;int rateArcChild2 = 20;
            finalConsumption = 210.0;
            uploadPricing(childSd2,flowToTest,volChild2,rateChild2,lowVolChild2,upVolChild2,rateArcChild2,type,null,customAssert);

            if (type.equals("ARC") || type.equals("Volume Pricing")) {
                Boolean publishStatus = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, childSd2, publishAction, customAssert);

                if (!publishStatus) {
                    customAssert.assertTrue(false, "Unable to Publish Child Service Data 2");
                } else {

                    String consCreationStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, childSd2, expectedNumOfBilRec, consumptionIdsChild2);

                    if(consCreationStatus.equals("fail")){
                        customAssert.assertTrue(false,"Consumptions not generated for Child Service Data 2");
                        customAssert.assertAll();
                        return;
                    }
                    for (int i = 0; i < consumptionIdsChild2.size(); i++) {

                        InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIdsChild2.get(i), finalConsumption);
                        workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionIdsChild2.get(i), approveAction);
                    }
                }
            }

//            C153600
            expectedNumOfBilRec = 24;billingRecords.clear();Thread.sleep(10000);
            billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataIdParent,customAssert);

            if(billingRecords.size() != expectedNumOfBilRec){
                logger.error("Expected Number Of Billing Records not matched with the actual number of billing records Expected " + expectedNumOfBilRec + " Actual " + billingRecords.size());
                customAssert.assertTrue(false,"Expected Number Of Billing Records not matched with the actual number of billing records after creation of Child 2 Expected " + expectedNumOfBilRec + " Actual " + billingRecords.size());
                customAssert.assertAll();
                return;
            }

            checkPricingForBillingRecords(billingRecords,child1ServiceYear,child2ServiceYear,rateChild1,rateChild2,
                    expAmtSDCurChild1,expAmtSDCurChild2,customAssert);

            invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,
                                        invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,flowToTest);

            if(invoiceId == -1){
                logger.error("Invoice Id not created");
                customAssert.assertTrue(false,"Invoice Id not created");
                customAssert.assertAll();
                return;
            }else{
                String invoiceDate = "01-03-2019";          //3-Jan-2019
                String liStartDate1 = "03-01-2019";
                String liEndDate1 = "03-31-2019";

                invoiceHelper.updateInvoiceDate(invoiceId,invoiceDate,customAssert);

                invLiId1 = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName,flowToTest,
                        liStartDate1,liEndDate1,invoiceDate,serviceDataIdParent);

                if(invLiId1 == -1){
                    logger.error("Invoice Line Item Id not created corresponding to child service data 1");
                    customAssert.assertTrue(false,"Invoice Line Item Id not created corresponding to child service data 1");
                    customAssert.assertAll();
                    return;
                }else {
                    invoiceHelper.verifyInvoiceLineItemValidationStatus(liValidationSatus,invLiId1,customAssert);
                }

                //Fixed Fee Scenario Case Ends here
                if(flowToTest.contains("fixed")){
                    customAssert.assertAll();
                    return;
                }

                String liStartDate2 = "03-01-2020";
                String liEndDate2 = "03-31-2020";

                invLiId2 = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName,flowToTest,
                        liStartDate2,liEndDate2,invoiceDate,serviceDataIdParent);

                if(invLiId2 == -1){
                    logger.error("Invoice Line Item Id not created corresponding to child service data 2");
                    customAssert.assertTrue(false,"Invoice Line Item Id not created corresponding to child service data 2");
                    customAssert.assertAll();
                    return;
                }else {

                }
            }

            workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invLiId1,approveLineItem);
            workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invLiId2,approveLineItem);
            workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId,approveInvoice);

            volChild1 = 300;rateChild1 = 30;
            if(flowToTest.contains("vol pricing")){
                rateArcChild1 = 50;
            }
            String entityPricingCCR = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"change request value");
            String crId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"change request id");

            uploadPricing(childSd1, flowToTest, volChild1, rateChild1, lowVolChild1, upVolChild1, rateArcChild1, type, entityPricingCCR, customAssert);

            logger.info("Waiting for Pricing Memo generation 1");
            Thread.sleep(60000);

//            C153596  Step 1  // 58 job id to check in case of issue
            String new_invoice_amount = "-1";
            String adjustment = "-1";
            String paid_invoice_amount = "-1";

            if(flowToTest.equals("vol pricing sd heirarchy flow")){
                new_invoice_amount ="5050";
                adjustment="4040";
                paid_invoice_amount="1010";
            }
            checkPricingMemoCreation("CCR Flow Memo generation 1",invLiId1,serviceDataIdParent,1,
                    paid_invoice_amount,new_invoice_amount,adjustment,"Price Change",
                    "Change Request",crId,customAssert);


            volChild1 = 400;
            rateChild1 = 40;


//            C153596  Step 2
            uploadPricing(childSd2, flowToTest, volChild1, rateChild1, lowVolChild2, upVolChild2, rateArcChild1, type, entityPricingCCR, customAssert);

            logger.info("Waiting for Pricing Memo generation 2");
            Thread.sleep(60000);

            if(flowToTest.equals("vol pricing sd heirarchy flow")){
                new_invoice_amount = "10500";
                adjustment= "6300";
                paid_invoice_amount = "4200";
            }

            checkPricingMemoCreation("CCR Flow Memo generation 2",invLiId2,serviceDataIdParent,1,
                    paid_invoice_amount,new_invoice_amount,adjustment,"Price Change",
                    "Change Request",crId,customAssert);


            String entityPricingCDR = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"cdr value");
            String cdrId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"cdr id");
            if(flowToTest.contains("vol pricing")){
                rateArcChild1 = 70;
            }
            uploadPricing(childSd1, flowToTest, volChild1, rateChild1, lowVolChild1, upVolChild1, rateArcChild1, type, entityPricingCDR, customAssert);
            Thread.sleep(60000);

            if(flowToTest.equals("vol pricing sd heirarchy flow")){
                new_invoice_amount = "7070";
                adjustment= "2020";
                paid_invoice_amount = "5050";
            }
            checkPricingMemoCreation("CDR Flow Memo generation 1",invLiId1,serviceDataIdParent,2,
                    paid_invoice_amount,new_invoice_amount,adjustment,"Price Change",
                    "Contract Draft Request",cdrId,customAssert);

            volChild1 = 500;
            rateChild1 = 50;

            uploadPricing(childSd2, flowToTest, volChild1, rateChild1, lowVolChild1, upVolChild1, rateArcChild1, type, entityPricingCDR, customAssert);
            Thread.sleep(60000);

            if(flowToTest.equals("vol pricing sd heirarchy flow")){
                new_invoice_amount = "0";
                adjustment= "-10500";
                paid_invoice_amount = "10500";
            }

            checkPricingMemoCreation("CDR Flow Memo generation 2",invLiId2,serviceDataIdParent,2,
                    paid_invoice_amount,new_invoice_amount,adjustment,"Price Change",
                    "Contract Draft Request",cdrId,customAssert);

            validateMemoBillingRec("Memo Pricing Change Scenario",serviceDataIdParent,crId,cdrId,customAssert);

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();

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
                String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedresult");
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


    private synchronized int getContractId(String flowToTest) {
        int contractId = -1;
        String contractSectionName = "default";

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractsectionname");
            if (temp != null)
                contractSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createcontract");
            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractid"));
            } else {
                //Create New Contract
                Boolean createLocalContract = true;
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createlocalcontract");
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
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createservicedata");

            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedataid"));
            } else {
                //Create New Service Data
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedatasectionname");
                if (temp != null)
                    serviceDataSectionName = temp.trim();

                //Update Service Data Extra Fields.
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                        "new client", "newClient" + contractId);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                        "new supplier", "newSupplier" + contractId);

                Boolean createLocalServiceData = true;
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createlocalservicedata");
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
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicesectionname");
            if (temp != null)
                invoiceSectionName = temp.trim();

            String createResponse = Invoice.createInvoice(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath, invoiceExtraFieldsConfigFileName,
                    invoiceSectionName, true);
            invoiceId = CreateEntity.getNewEntityId(createResponse, "invoice");

            logger.info("Updating Invoice Line Item Config File for Flow [{}] and Invoice Id {}.", flowToTest, invoiceId);
            int invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
            String invoiceName = ShowHelper.getValueOfField(invoiceEntityTypeId, invoiceId, "title");

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicelineitemsectionname");
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
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicelineitemsectionname");
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

    private void killAllSchedulerTasks() {
        UserTasksHelper.removeAllTasks();
    }



    private synchronized void updateServiceDataAndInvoiceConfig(String flowToTest, int contractId) {
        String serviceDataSectionName = "default";
        String invoiceSectionName = "default";
        try {
            if (contractId != -1) {
                logger.info("Updating Service Data Config File for Flow [{}] and Contract Id {}.", flowToTest, contractId);
                String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedatasectionname");
                if (temp != null)
                    serviceDataSectionName = temp.trim();

                int contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
                String contractName = ShowHelper.getValueOfField(contractEntityTypeId, contractId, "title");
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourceid",
                        String.valueOf(contractId));

                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicesectionname");
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


            int numberOfColumnToEditForEachRow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "numberofcolumntoeditforeachrowforarc"));

            String[] arcRowNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "arcrownumber").trim().split(Pattern.quote(","));

            String[] arcColumnNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "arccolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
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



    // this will create the row , <columnNumber,value> for editing the Forecast Sheet
    private Map<Integer, Map<Integer, Object>> getValuesMapForForecastSheet(String flowToTest, String clientId) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {


            String rowNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "forecastsheetrownumber");

            String[] columnNumbers = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "forecastsheetcolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "forecastsheetvalue").trim().split(Pattern.quote(","));


            Map<Integer, Object> innerValuesMap = new HashMap<>();
            for (int j = 0; j < columnNumbers.length; j++) {

                if (values[j].toLowerCase().contentEquals("clientid")) {
                    innerValuesMap.put(Integer.parseInt(columnNumbers[j]), clientId);
                    continue;

                }


                innerValuesMap.put(Integer.parseInt(columnNumbers[j]), values[j]);
            }
            valuesMap.put(Integer.parseInt(rowNumber), innerValuesMap);

        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Forecast Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    private BigDecimal calcSysAmountARC(int serviceDataId,String finalConsumption,CustomAssert customAssert) {

        BigDecimal systemAmt = new BigDecimal(0.0);

        try {

            TabListData tabListData = new TabListData();
            String chargesTabResponse = tabListData.hitTabListDataV2(chargesTabId, serviceDataEntityTypeId, serviceDataId);

            ListRendererListData listRendererListData = new ListRendererListData();

            Map<String, String> chargesColMap = listRendererListData.getListColumnNameValueMap(chargesTabResponse);

            BigDecimal baseVolume = new BigDecimal(chargesColMap.get("volume"));
            BigDecimal rate = new BigDecimal(chargesColMap.get("unitrate"));

            BigDecimal finalConsumptionBigDec;
            if (finalConsumption == null) {
                customAssert.assertTrue(false, "Final consumption is null while validating values for ARC ");
                return systemAmt;
            } else {
                finalConsumptionBigDec = new BigDecimal(finalConsumption);
            }

            String arcTabResponse = tabListData.hitTabListDataV2(ARCRRCTabId, serviceDataEntityTypeId, serviceDataId);

            TreeMap<BigDecimal, HashMap<String, String>> arcMap = new TreeMap<>();
            HashMap<String, String> columnValueMap;

            BigDecimal lowerLevel = new BigDecimal(0.0);
            BigDecimal upperLevel = new BigDecimal(0.0);
            if (JSONUtility.validjson(arcTabResponse)) {

                JSONObject arcTabRespJson = new JSONObject(arcTabResponse);

                JSONArray dataArray = arcTabRespJson.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {

                    columnValueMap = new HashMap<>();

                    JSONObject indvJson = dataArray.getJSONObject(i);
                    String[] columnIds = indvJson.getNames(indvJson);

                    for (String columnId : columnIds) {

                        String columnName = indvJson.getJSONObject(columnId).get("columnName").toString();
                        String columnValue = indvJson.getJSONObject(columnId).get("value").toString();

                        if (columnName.equals("lowerlevel")) {
                            try {
                                lowerLevel = new BigDecimal(columnValue);

                                lowerLevel = (baseVolume.multiply(lowerLevel).divide(new BigDecimal(100)));
                                columnValue = lowerLevel.toString();

                            } catch (Exception e) {
                                customAssert.assertTrue(false, "Exception while parsing double value for lower level");
                            }
                        }else if (columnName.equals("upperlevel")) {
                            try {
                                if(columnValue == "null"){

                                    columnValue = String.valueOf(Integer.MAX_VALUE);

                                }else {
                                    upperLevel = new BigDecimal(columnValue);

                                    upperLevel = (baseVolume.multiply(upperLevel).divide(new BigDecimal(100)));
                                    columnValue = upperLevel.toString();
                                }

                            } catch (Exception e) {
                                customAssert.assertTrue(false, "Exception while parsing double value for upper level");
                            }
                        }
                        columnValueMap.put(columnName, columnValue);

                    }
                    arcMap.put(lowerLevel, columnValueMap);
                }
            }
            HashMap<String, String> values;
            //1 means Greater than base volume || 0 means equal || -1 means less than
            if (finalConsumptionBigDec.compareTo(baseVolume) == 1) {

                BigDecimal remainingAmount = finalConsumptionBigDec;

                systemAmt = baseVolume.multiply(rate);

                remainingAmount = finalConsumptionBigDec.subtract(baseVolume);

                for (Map.Entry<BigDecimal, HashMap<String, String>> entry : arcMap.entrySet()) {

                    if (remainingAmount.equals(new BigDecimal(0.0))) {
                        break;
                    }
//                    BigDecimal band = entry.getKey();

                    values = entry.getValue();

                    String lowerlevel = values.get("lowerlevel");
                    String upperlevel = values.get("upperlevel");

                    if (upperlevel == null) {

                    } else {
                        BigDecimal interval = new BigDecimal(upperlevel).subtract(new BigDecimal(lowerlevel));

                        BigDecimal newRemainingAmount = remainingAmount.subtract(interval);

                        if (newRemainingAmount.equals(0)) {
                            systemAmt = systemAmt.add(new BigDecimal(values.get("rate")).multiply(newRemainingAmount));
                            remainingAmount = new BigDecimal(0.0);

                        } else if (newRemainingAmount.compareTo(new BigDecimal(0)) == 1) {

                            systemAmt = systemAmt.add(new BigDecimal(values.get("rate")).multiply(interval));
                            remainingAmount = newRemainingAmount;

                        }else if (newRemainingAmount.compareTo(new BigDecimal(0)) == -1) {

                            systemAmt = systemAmt.add(new BigDecimal(values.get("rate")).multiply(remainingAmount));
                            remainingAmount = new BigDecimal(0.0);

                        }
                    }
                }
            } else if (finalConsumptionBigDec.compareTo(baseVolume) == 0) {

                systemAmt = systemAmt.add(rate.multiply(baseVolume));

            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while calculating system amount ARC");
        }
        return systemAmt;
    }

    private int getSDFromClone(int serviceDataIdParent,Boolean billingAvailable,
                               Boolean pricingAvailable,Boolean consumAvail,
                               String newString,int convDataMatrix,
                               int dateType,
                               int currencyId) {

        Clone clone = new Clone();
        String cloneResponse = clone.hitClone(serviceDataEntity, serviceDataIdParent);

        JSONObject cloneResponseJson = new JSONObject(cloneResponse);
        JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");

        String parentClientId = dataJson.getJSONObject("serviceIdClient").getString("values");
        JSONObject valuesJson = new JSONObject();
        valuesJson.put("name", parentClientId);
        dataJson.getJSONObject("parentService").put("values", valuesJson);

        String serviceIdClientChild2 = dataJson.getJSONObject("serviceIdClient").getString("values");
        serviceIdClientChild2 = serviceIdClientChild2 + newString;
        dataJson.getJSONObject("serviceIdClient").put("values", serviceIdClientChild2);

        String serviceIdSupplierChild2 = dataJson.getJSONObject("serviceIdSupplier").getString("values");
        serviceIdSupplierChild2 = serviceIdSupplierChild2 + newString;
        dataJson.getJSONObject("serviceIdSupplier").put("values", serviceIdSupplierChild2);
        dataJson.getJSONObject("billingAvailable").put("values", billingAvailable);
        dataJson.getJSONObject("pricingAvailable").put("values", pricingAvailable);
        dataJson.getJSONObject("consumptionAvailable").put("values", consumAvail);

        dataJson.getJSONObject("conversionData").put("values", new JSONObject("{\"name\": \"\",\"id\": " + convDataMatrix + "}"));
        dataJson.getJSONObject("currencyConversionDateType").put("values", new JSONObject("{\"name\": \"\",\"id\": " + dateType + "}"));
        dataJson.getJSONObject("currency").put("values", new JSONObject("{\"name\":\"\",\"id\":" + currencyId + ",\"shortName\":\"\"}"));

        if (pricingAvailable == true) {

            dataJson.getJSONObject("pricings").put("values", new JSONArray("[{\"volume\":80,\"endDate\":\"03-31-2018\",\"unitRate\":10,\"startDate\":\"03-01-2018\"}]"));
            dataJson.getJSONObject("arcRrc").put("values", new JSONArray("[{\"lowerVolume\":100,\"endDate\":\"03-31-2018\",\"rate\":5,\"upperVolume\":105,\"type\":{\"id\":1},\"startDate\":\"03-01-2018\"},{\"lowerVolume\":105,\"endDate\":\"03-31-2018\",\"rate\":10,\"upperVolume\":130,\"type\":{\"id\":1},\"startDate\":\"03-01-2018\"},{\"lowerVolume\":130,\"endDate\":\"03-31-2018\",\"rate\":15,\"type\":{\"id\":1},\"startDate\":\"03-01-2018\"}]"));

        }

        dataJson.remove("history");

        JSONObject createEntityJson = new JSONObject();
        JSONObject createEntityBodyJson = new JSONObject();
        createEntityBodyJson.put("data", dataJson);
        createEntityJson.put("body", createEntityBodyJson);

        Create create = new Create();
        create.hitCreate(serviceDataEntity, createEntityJson.toString());
        String createResponse = create.getCreateJsonStr();
        int newServiceDataIdChild2 = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

        return newServiceDataIdChild2;
    }

    /*
    This method calculates conversion from child to parent Service Data
    Source(From) Currency is child Currency
    Destination(To) Currency is parent Currency
    Conversion matrix is of child Service Data
     */
    private BigDecimal calcConvChildToParentSD(int childSD,BigDecimal sysAmt,int invoiceLineItemId,CustomAssert csAssert){
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        BigDecimal calcAmt = new BigDecimal(0);

        try {
            String parentServiceData = ShowHelper.getValueOfField(serviceDataEntityTypeId, childSD, "parentservicedataid");
            String parentSDCurrencyId = "-1";
            if (parentServiceData == null) {
                csAssert.assertTrue(false, "Parent Service Data Value is null");
                return null;
            } else if (parentServiceData.equalsIgnoreCase("null")) {
                csAssert.assertTrue(false, "Parent Service Data Value is null");
                csAssert.assertAll();
                return null;
            } else {
                parentSDCurrencyId = ShowHelper.getValueOfField(serviceDataEntityTypeId, Integer.parseInt(parentServiceData), "currency id");
                if (parentSDCurrencyId == null) {
                    csAssert.assertTrue(false, "Parent Service Data currency id is null ");
                    csAssert.assertAll();
                    return null;
                }

            }

            String childCurrencyId = ShowHelper.getValueOfField(serviceDataEntityTypeId, childSD, "currency id");
            if (childCurrencyId == null) {
                csAssert.assertTrue(false, "Child Service Data currency id is null ");
                csAssert.assertAll();
                return null;
            }

            String convMatrixIdChildSD = ShowHelper.getValueOfField(serviceDataEntityTypeId, childSD, "conversion data");
            String convMatrixIdParentSD = ShowHelper.getValueOfField(serviceDataEntityTypeId, Integer.parseInt(parentServiceData), "conversion data");


            try {

                List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select conversion_rates from conversion_matrix where conversion_data_id  = " + convMatrixIdChildSD);

                if (sqlOutput.size() == 0) {
                    csAssert.assertTrue(false, "Conversion rates not found from conversion_matrix table where conversion_data_id  = " + convMatrixIdChildSD);
                    csAssert.assertAll();
                    return null;
                } else {

                    String conversionRates = sqlOutput.get(0).get(0);

                    JSONObject conversionRatesJson = new JSONObject(conversionRates);

                    BigDecimal convRate = new BigDecimal(conversionRatesJson.getJSONObject(childCurrencyId).get(parentSDCurrencyId).toString());

                    calcAmt = convRate.multiply(sysAmt);

                    String lineItemCurrencyId = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId,invoiceLineItemId,"currency id");

                    sqlOutput = postgreSQLJDBC.doSelect("select conversion_rates from conversion_matrix where conversion_data_id  = " + convMatrixIdParentSD);

                    if (sqlOutput.size() == 0) {
                        csAssert.assertTrue(false, "Conversion rates not found from conversion_matrix table where conversion_data_id  = " + convMatrixIdParentSD);
                        csAssert.assertAll();
                        return null;
                    }else {

                        conversionRates = sqlOutput.get(0).get(0);

                        conversionRatesJson = new JSONObject(conversionRates);

                        convRate = new BigDecimal(conversionRatesJson.getJSONObject(parentSDCurrencyId).get(lineItemCurrencyId).toString());

                        calcAmt = convRate.multiply(calcAmt);

                    }

                }

            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while fetching conversion rate");
            } finally {

            }

        }catch (Exception e){
            csAssert.assertTrue(false, "Exception while calculating  Conversion amount ");
            csAssert.assertAll();
            return null;
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        return calcAmt;
    }

    private int createChildSD(int serviceDataIdParent,String subString,
                               Boolean billAvail,Boolean pricingAvail,
                               Boolean consAvail,CustomAssert customAssert){

        int newServiceDataIdChild1 =-1;
        try {
            Clone clone = new Clone();
            Create create = new Create();
            String cloneResponse = clone.hitClone(serviceDataEntity, serviceDataIdParent);
            JSONObject cloneResponseJson = new JSONObject(cloneResponse);

            JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");
            if (!dataJson.getJSONObject("parentService").has("values")) {
                String parentClientId = dataJson.getJSONObject("serviceIdClient").getString("values");
                JSONObject valuesJson = new JSONObject();
                valuesJson.put("name", parentClientId);
                dataJson.getJSONObject("parentService").put("values", valuesJson);
            }
            String serviceIdClientChild = dataJson.getJSONObject("serviceIdClient").getString("values");
//            serviceIdClientChild = serviceIdClientChild + "_Child12";
            serviceIdClientChild = serviceIdClientChild + subString;
            dataJson.getJSONObject("serviceIdClient").put("values", serviceIdClientChild);

            String serviceIdSupplierChild = dataJson.getJSONObject("serviceIdSupplier").getString("values");
//            serviceIdSupplierChild = serviceIdSupplierChild + "_Child12";
            serviceIdSupplierChild = serviceIdSupplierChild + subString;
            dataJson.getJSONObject("serviceIdSupplier").put("values", serviceIdSupplierChild);
            dataJson.getJSONObject("billingAvailable").put("values", billAvail);
            dataJson.getJSONObject("pricingAvailable").put("values", pricingAvail);
            dataJson.getJSONObject("consumptionAvailable").put("values", consAvail);

            dataJson.remove("history");

            JSONObject createEntityJson = new JSONObject();
            JSONObject createEntityBodyJson = new JSONObject();
            createEntityBodyJson.put("data", dataJson);
            createEntityJson.put("body", createEntityBodyJson);

            create.hitCreate(serviceDataEntity, createEntityJson.toString());
            String createResponse = create.getCreateJsonStr();
            newServiceDataIdChild1 = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            if (newServiceDataIdChild1 == -1) {
                logger.error("Unable to create child service data");
                customAssert.assertTrue(false, "Unable to create child service data");

            }
        }catch (Exception e){
            logger.error("Exception while creating child service data ");

        }

        return newServiceDataIdChild1;
    }

    private synchronized Boolean uploadPricing(int serviceDataId,String flowToTest,
                                               int volume,int rate,
                                               int lowerVol,int upperVol,int rateArc,String type,
                                               String entityNamePricing,
                                               CustomAssert customAssert) {

        Boolean uploadPricing = true;
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        String pricingTemplateFileName = "PricingFile.xlsm";
        String serviceDataType = "";

        try {
            String dataSheet = "Data";
            String pricingSheet = "Pricing";
            String arcRRCSheet = "ARC RRC";

            Boolean downPricingFile = invoicePricingHelper.downloadPricingTemplate(pricingTemplateFilePath, pricingTemplateFileName, serviceDataId);

            if (downPricingFile) {
// getting the actual service data Type
                if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                        "servicedatatype") != null) {
                    serviceDataType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                            "servicedatatype");
                }

                if(entityNamePricing != null){
                    int entityNameColNum = XLSUtils.getExcelColNumber(pricingTemplateFilePath,pricingTemplateFileName,dataSheet,2,"11653");
                    XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName,dataSheet,6,entityNameColNum,entityNamePricing);
                }

                int pricingStartRow = 6;
                int pricingEndRow = XLSUtils.getNoOfRows(pricingTemplateFilePath,pricingTemplateFileName,pricingSheet).intValue();

                int volColumn = XLSUtils.getExcelColNumber(pricingTemplateFilePath,pricingTemplateFileName,pricingSheet,2,"8052");
                int rateColumn = XLSUtils.getExcelColNumber(pricingTemplateFilePath,pricingTemplateFileName,pricingSheet,2,"11348");

                Map<Integer,Object> colValueMap = new HashMap<>();
                int loweVolColNum = XLSUtils.getExcelColNumber(pricingTemplateFilePath,pricingTemplateFileName,arcRRCSheet,2,"8061");
                int upperVolColNum = XLSUtils.getExcelColNumber(pricingTemplateFilePath,pricingTemplateFileName,arcRRCSheet,2,"8062");
                int rateColNum = XLSUtils.getExcelColNumber(pricingTemplateFilePath,pricingTemplateFileName,arcRRCSheet,2,"8063");
                int typeColNum = XLSUtils.getExcelColNumber(pricingTemplateFilePath,pricingTemplateFileName,arcRRCSheet,2,"8064");

                colValueMap.put(volColumn,volume);
                colValueMap.put(rateColumn,rate);

                invoicePricingHelper.updatePricingSheet(pricingTemplateFilePath,pricingTemplateFileName,pricingStartRow,pricingEndRow,colValueMap,customAssert);

                if(serviceDataType.equals("arc")) {
                    colValueMap.clear();
                    colValueMap.put(loweVolColNum,lowerVol);
                    colValueMap.put(upperVolColNum,upperVol);
                    colValueMap.put(rateColNum,rateArc);
                    colValueMap.put(typeColNum,type);
                    int arcStartRow = 6;
                    int arcEndRow = XLSUtils.getNoOfRows(pricingTemplateFilePath,pricingTemplateFileName,arcRRCSheet).intValue();

                    invoicePricingHelper.updateARCSheet(pricingTemplateFilePath, pricingTemplateFileName, arcStartRow,arcEndRow, colValueMap, customAssert);
                }

                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String pricingUploadResponse = invoicePricingHelper.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {

                    String pricingSchedulerStatus = invoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

                    if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                flowToTest + "]");

                    }
                }else {
                    customAssert.assertTrue(false,"Error while pricing download on Service Data");
                }
            }else {
                customAssert.assertTrue(false,"Error while pricing download on Service Data");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while upload Pricing on service Data");
            uploadPricing = false;
        }
        return uploadPricing;
    }


    //Checking pricing values based on different service data
    private Boolean checkPricingForBillingRecords(HashMap<String,Map<String,String>> billingRecords,
                                                  String child1ServiceYear,String child2ServiceYear,
                                                  int rateChild1BillingRecord,int rateChild2BillingRecord,
                                                  String amtSDCurrencyChild1BillingRecord,String amtSDCurrencyChild2BillingRecord,
                                                  CustomAssert customAssert){

        Boolean pricingValStatus = true;
        try {

            Boolean pricingCheckForChild1 = true;
            Boolean pricingCheckForChild2 = true;
            Boolean billingRecPresentForChild1 = false;
            Boolean billingRecPresentForChild2 = false;
            DecimalFormat df = new DecimalFormat("0.00");
            for (Map.Entry<String, Map<String, String>> billingRec : billingRecords.entrySet()) {

                String billingStartDate = billingRec.getKey();
                Map<String, String> billingRecord = billingRec.getValue();

                if (billingStartDate.contains(child1ServiceYear)) {
                    billingRecPresentForChild1=true;
                    if(pricingCheckForChild1) {
                        if (!billingRecord.get("rate").equals(df.format(Double.valueOf(rateChild1BillingRecord)))){
                            customAssert.assertTrue(false,"Expected value of Rate For Child Service Data 1 not matched with Actual Expected : " + rateChild1BillingRecord + " Actual : " + billingRecord.get("rate"));
                            pricingCheckForChild1 = false;
                        }
                        if (!billingRecord.get("amountInServiceDataCurrency").equals(df.format(Double.valueOf(amtSDCurrencyChild1BillingRecord)))) {
                            customAssert.assertTrue(false,"Expected value of Amount In Service Data Currency For Child Service Data 1 not matched with Actual Expected : " + amtSDCurrencyChild1BillingRecord + " Actual :" + billingRecord.get("amountInServiceDataCurrency"));
                            pricingCheckForChild1 = false;
                        }
                    }

                } else if (billingStartDate.contains(child2ServiceYear)) {
                    billingRecPresentForChild2=true;
                    if(pricingCheckForChild2) {
                        if (!billingRecord.get("rate").equals(df.format(Double.valueOf(rateChild2BillingRecord)))) {
                            customAssert.assertTrue(false, "Expected value of Rate For Child Service Data 2 not matched with Actual Expected : " + rateChild2BillingRecord + " Actual : " + billingRecord.get("rate"));
                            pricingCheckForChild2 = false;
                        }
                        if (!billingRecord.get("amountInServiceDataCurrency").equals(df.format(Double.valueOf(amtSDCurrencyChild2BillingRecord)))) {
                            customAssert.assertTrue(false,"Expected value of Amount In Service Data Currency For Child Service Data 2 not matched with Actual Expected : " + amtSDCurrencyChild2BillingRecord + " Actual : " + billingRecord.get("amountInServiceDataCurrency"));
                            pricingCheckForChild2 = false;
                        }
                    }
                }
            }

            if(billingRecPresentForChild1 == false){
                customAssert.assertTrue(false,"Billing Record not present for Child 1 with Service Year " + child1ServiceYear);
            }

            if(billingRecPresentForChild2 == false){
                customAssert.assertTrue(false,"Billing Record not present for Child 2 with Service Year " + child2ServiceYear);
            }
        }catch (Exception e){
            logger.error("Exception while checking Pricing For Different Billing Records");
            customAssert.assertTrue(false,"Exception while checking Pricing For Different Billing Records");
            pricingValStatus = false;
        }
        return pricingValStatus;
    }


    private Boolean checkPricingMemoCreation(String flowToTest,int lineItemId,int serviceDataId,
                                             int rowToChecked,
                                             String paid_invoice_amount_exp,String new_invoice_amount_exp,
                                             String adjustment,String memoReason,
                                             String pricingEntityType,String pricingEntityId,

                                             CustomAssert customAssert){

        Boolean pricingMemoCreation = true;
        TabListData tabListData = new TabListData();
        try{
            int expectedMemoTabId = 337;
            String payload = "{\"filterMap\":{\"entityTypeId\":188,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

            String tabListResponse = tabListData.hitTabListDataV2(expectedMemoTabId,invoiceLineItemEntityTypeId,lineItemId,payload);

            if(JSONUtility.validjson(tabListResponse)){

                JSONObject tabListRespJson = new JSONObject(tabListResponse);
                JSONArray dataArray = tabListRespJson.getJSONArray("data");

                if(!(dataArray.length() == rowToChecked)){
                    customAssert.assertTrue(false,"Expected Row Number Does not exists in the expected Memo Tab" +
                            " Either the Row Number Does not exists OR PRICE CHANGE MEMO not created for the flow" + flowToTest);
                }else {
                    JSONObject rowToCheck = dataArray.getJSONObject(rowToChecked -1);
                    Iterator<String> keys = rowToCheck.keys();
                    String colName="";
                    String colVal="";
                    while(keys.hasNext()){
                        String key = keys.next();

                        colName = rowToCheck.getJSONObject(key).get("columnName").toString();
                        colVal = rowToCheck.getJSONObject(key).get("value").toString();

                        switch (colName) {
                            case "paid_invoice_amount":
                                if (!colVal.contains(paid_invoice_amount_exp)) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                    pricingMemoCreation = false;
                                }
                                break;
                            case "new_invoice_amount":
                                if (!colVal.contains(new_invoice_amount_exp)) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                    pricingMemoCreation = false;
                                }
                                break;
                            case "adjustment":
                                if (!colVal.contains(adjustment)) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                    pricingMemoCreation = false;
                                }
                                break;
                            case "sirion_line_item_description":
                                String lineItemDesc = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, lineItemId, "name");
                                if (!colVal.equals(lineItemDesc)) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                    pricingMemoCreation = false;
                                }
                                break;
                            case "sirion_line_item_id":
                                if (!colVal.contains(String.valueOf(lineItemId))) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                }
                                break;
                            case "servicedata":
                                if (!colVal.contains(String.valueOf(serviceDataId))) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                }
                                break;
                            case "memo_reason":
                                if (!colVal.equals(memoReason)) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                    pricingMemoCreation = false;
                                }
                                break;
                            case "pricingentitytype":
                                if (!colVal.equals(pricingEntityType)) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                    pricingMemoCreation = false;
                                }
                                break;
                            case "pricingentityid":
                                if (!colVal.contains(pricingEntityId)) {
                                    customAssert.assertEquals(colVal, paid_invoice_amount_exp, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                    pricingMemoCreation = false;
                                }
                                break;

                            case "effectivedate":
                                String effectiveDate = "";
                                if(pricingEntityType.equals("Change Request")) {
                                    effectiveDate = ShowHelper.getValueOfField(changeRequestEntityTypeId, Integer.parseInt(pricingEntityId), "effectivedate");
                                }else if(pricingEntityType.equals("Contract Draft Request")){
                                    effectiveDate = ShowHelper.getValueOfField(cdrEntityTypeId, Integer.parseInt(pricingEntityId), "effectivedate");
                                }

                                if (!colVal.equals(effectiveDate)) {
                                    customAssert.assertEquals(colVal, effectiveDate, "Expected and Actual value for " + colName + " not matched on Expected Memo Tab for the flow " + flowToTest);
                                    pricingMemoCreation = false;
                                }

                                break;

                        }
                    }
                }
            }else {
                logger.error("Expected Memo Tab List Response is not a valid json for the flow " + flowToTest);
                customAssert.assertTrue(false,"Expected Memo Tab List Response is not a valid json for the flow " + flowToTest);
            }
        }catch (Exception e){
            logger.error("Exception while checking Pricing Memo Creation for the flow " + flowToTest);
            customAssert.assertTrue(false,"Exception while checking Pricing Memo Creation for the flow " + flowToTest);
        }

        return pricingMemoCreation;
    }

    private Boolean validateMemoBillingRec(String flowToTest,int serviceDataId,String crId,String cdrId,
                                           CustomAssert customAssert) {

        Boolean validationStatus = true;
        try {

            InvoiceHelper invoiceHelper = new InvoiceHelper();
            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"248\":{\"multiselectValues\":" +
                    "{\"SELECTEDDATA\":[{\"id\":\"" + serviceDataId + "\",\"name\":\"s\"}]}," +
                    "\"filterId\":248,\"filterName\":\"serviceData\"}," +
                    "\"384\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Price Change\"}]}," +
                    "\"filterId\":384,\"filterName\":\"memoReason\"}}},\"selectedColumns\":[]}";

            HashMap<String, Map<String, String>> billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, payload, customAssert);

            if (billingRecords.size() == 0) {
                logger.error("Billing records not generated for the flow " + flowToTest + " Or the Filter of Service Data Or Memo Reason not working properly");
                customAssert.assertEquals("Billing records not generated for the flow Pricing Change Memo Flow " + flowToTest + " Or the Filter of Service Data Or Memo Reason not working properly", "Billing records not generated for the flow " + flowToTest);
                validationStatus = false;
            } else {

                for (Map.Entry<String, Map<String, String>> billingRec : billingRecords.entrySet()) {

                    Map<String, String> bilRecord = billingRec.getValue();
                    if (bilRecord.get("pricingentitytypeid").equals("Change Request")
                            && bilRecord.get("memoreason").equals("Price Change")) {
                        if (bilRecord.get("pricingentityid").contains(crId) &&
                                bilRecord.get("pricingentityname").contains(crId)) {
                            logger.info("Change Request Value in Entity ID (Pricing) OR Entity Name (Pricing) Billing Record is as Expected");
                        } else {
                            logger.error("Change Request Value in Entity ID (Pricing) OR Entity Name (Pricing) Billing Record is not as Expected");
                            customAssert.assertEquals(bilRecord.get("pricingentityid"), crId, "Change Request Value in Entity ID (Pricing) OR Entity Name (Pricing) Billing Report Record is not as Expected");
                            validationStatus = false;

                        }
                    } else if (bilRecord.get("pricingentitytypeid").equals("Change Draft Request")
                            && bilRecord.get("memoreason").equals("Price Change")) {
                        if (bilRecord.get("pricingentityid").contains(cdrId) &&
                                bilRecord.get("pricingentityname").contains(cdrId)) {
                            logger.info("Contract Draft Request Value in Entity ID (Pricing) OR Entity Name (Pricing) in Billing Report Record is as Expected");
                        } else {
                            logger.error("Contract Request Value in Entity ID (Pricing) OR Entity Name (Pricing) Billing Record is not as Expected");
                            customAssert.assertEquals(bilRecord.get("pricingentityid"), cdrId, "Contract Request Value in Entity ID (Pricing) OR Entity Name (Pricing) Billing Record is not as Expected");
                            validationStatus = false;
                        }
                    } else {
                        customAssert.assertEquals("During validation of Memo Billing Record in Billing Report Either \"Memo Reason\" Column does not have \"Price Change\" value or Entity Type(Pricing) column does not have \"Change Request OR \"Change Draft Request value\"", "During validation of Memo Billing Record in Billing Report Either Memo Reason Column should have \"Price Change\" value or \"Entity Type(Pricing)\" column should have \"Change Request OR Change Draft Request value\"");
                        validationStatus = false;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Exception while validating memo billing record for the flow " + flowToTest);
            customAssert.assertEquals("Exception is there while validating Memo Billing Record For the Flow", "Exception should not occur");
            validationStatus = false;
        }
        return validationStatus;
    }



}
