package com.sirionlabs.test.invoice.flow;


import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.invoiceLineItem.GetDetailsMetadata;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;

import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.invoice.ForecastUploadHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;
import org.testng.annotations.Optional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;


class InvoiceDetails {
    public InvoiceDetails(int contractId, int serviceDataId, String flowToTest) {
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

public class TestInvoiceFlow extends TestRailBase {
    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceFlow.class);
    private static String invoicePricingHelperConfigFilePath = null;
    private static String invoicePricingHelperConfigFileName = null;
    private static Boolean failTestIfLineItemValidationNotCompletedWithinTimeOut = true;
    private static Long lineItemValidationTimeOut = 120000L;
    private static Long pricingSchedulerTimeOut = 120000L;
    private static Long forcastSchedulerTimeOut = 300000L;
    private static Long consumptionToBeCreatedTimeOut = 120000L;
    private static Long pollingTime = 5000L;
    int forecastTabId = 313;
    int ARCRRCTabId = 311;
    int chargesTabId = 309;
    int referenceTabId = 356;
    int subServiceDataTabId = 63;
    private String configFilePath;
    private String configFileName;
    private String flowsConfigFilePath;
    private String flowsConfigFileName;
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
    private String changeRequestConfigFilePath;
    private String changeRequestConfigFileName;
    private String changeRequestExtraFieldsConfigFileName;
    //	private String contractSectionName;
//	private String serviceDataSectionName;
//	private String invoiceSectionName;
//	private String invoiceLineItemSectionName;
    private int serviceDataEntityTypeId = -1;
    private int contractEntityTypeId = -1;
    private int invoiceLineItemEntityTypeId = -1;
    private int consumptionsEntityTypeId = -1;
    private int invoiceEntityTypeId = 67;

    private String serviceDataEntitySectionUrlName;
    private String consumptionEntitySectionUrlName;
    private String invoiceEntitySectionUrlName;
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
    private String contractEntity = "contracts";
    private String changeRequestEntity = "change requests";
    private String invoiceEntity = "invoices";

    private String forecastSheetNameInXLSXFile = "Forecast Data";
    private String ARCRRCSheetNameInXLSXFile = "ARC RRC";
//    private String ARCRRCSheetNameInXLSXFile = "ARC RRC  VGE";

    private String rateCardConfigFilePath;
    private String rateCardConfigFileName;

    private Map<String, String> flowIdContractIdMap = new HashMap<>();
    private Map<String, String> flowIdServiceDataIdMap = new HashMap<>();

    private int consumptionEntityTypeId;
    private String flowsToValidate = "", testAllFlows = "";

    private List<InvoiceDetails> listOfInvoiceTestDetails = new ArrayList<>();
    private int listOfInvoiceTestDetailsCount=0;
    private int testCaseCount = -1;
    private boolean checkLIValidation=true;

    private String approveInvoice = "ApproveInvoice";
    private String approveLineItem = "Approve";
    private String calculateMemo = "Calculatememo";

    private List<String> billingScenariosList;

    private String listShowPageColFilePath;
    private String listShowPageColFileName;

    @BeforeClass
    public void beforeClass() throws Exception {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceTestConfigFileName");
        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");
        forecastTemplateFilePath = pricingTemplateFilePath;
        flowsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsconfigfilepath");
        flowsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsconfigfilename");
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

        changeRequestConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFilePath");
        changeRequestConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFileName");
        changeRequestExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestExtraFieldsFileName");

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
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));
        consumptionEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "url_name");
        invoiceEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "url_name");

        //rate card specific configs
        rateCardConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("RateCardConfigFilePath");
        rateCardConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("RateCardConfigFileName");
        invoiceLineItemEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "entity_type_id"));
        consumptionsEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "entity_type_id"));
        contractEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, contractEntity, "entity_type_id"));
        consumptionEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "entity_type_id"));

        String billingScenarios = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"billing record flows");

        if(billingScenarios != null){
            billingScenariosList = Arrays.asList(billingScenarios.split(","));
        }

        listShowPageColFilePath = ConfigureConstantFields.getConstantFieldsProperty("listShowPageColFilePath");
        listShowPageColFileName = ConfigureConstantFields.getConstantFieldsProperty("listShowPageColFileName");

        testCasesMap = getTestCasesMapping();

    }

    @DataProvider(parallel = false)
    public Object[][] dataProviderForInvoiceFlow() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest();
        testCaseCount = allFlowsToTest.size();
        for (String flowToTest : allFlowsToTest) {

            allTestData.add(new Object[]{flowToTest});
        }

        logger.info("testCaseCount : {}",testCaseCount);
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForSubServiceDataTabValidation() {

        String[] flowsToTestForSubServiceDataTabValidation;
        List<Object[]> allTestData = new ArrayList<>();
        try {
            flowsToTestForSubServiceDataTabValidation = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsforsubservicedatatabvalidation").split(",");
            for (String flowToTest : flowsToTestForSubServiceDataTabValidation) {
                allTestData.add(new Object[]{flowToTest});
            }

        } catch (Exception e) {
            logger.error("Exception while preparing data provider for SubServiceDataTabValidation " + e.getStackTrace());
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForInvoiceFlow", enabled = true)
    public void testInvoiceFlowPart1(String flowToTest) {

        CustomAssert csAssert = new CustomAssert();

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId = -1;
        int serviceDataId = -1;

        String contractSectionName = "default";
        String serviceDataSectionName = "default";
        String invoiceSectionName = "default";
        String invoiceLineItemSectionName = "default";

        try {

            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

            clearEntitySections(contractSectionName, serviceDataSectionName, invoiceSectionName, invoiceLineItemSectionName);

            //Get Contract that will be used for Invoice Flow Validation
            synchronized (this) {
                contractId = getContractId(flowToTest);
                if (contractId != -1) {
                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                    flowIdContractIdMap.put(flowToTest, String.valueOf(contractId));

                    updateServiceDataAndInvoiceConfig(flowToTest, contractId);

                    serviceDataId = getServiceDataId(flowToTest, contractId);

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
                            synchronized (this) {
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
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
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
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                    csAssert.assertAll();
                                                    return;
                                                } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                                                {
                                                    boolean isDataCreatedUnderChargesTab = isChargesCreated(serviceDataId);

                                                    if (!isDataCreatedUnderChargesTab) {
                                                        csAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                                flowToTest + "]");
//                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                        csAssert.assertAll();
                                                        return;
                                                    }


                                                    if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                                        boolean isDataCreatedUnderARCRRCTab = isARCRRCCreated(serviceDataId);

                                                        if (!isDataCreatedUnderARCRRCTab) {
                                                            csAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                                                    flowToTest + "]");
                                                            addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                            csAssert.assertAll();
                                                            return;
                                                        }

                                                    }

                                                    if (publishFlag.equals("true")) {

                                                        serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                                "servicedatatype");

                                                        if (serviceDataType.equals("fixed fee")) {
                                                            listOfInvoiceTestDetails.add(new InvoiceDetails(contractId, serviceDataId, flowToTest));
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
                                                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);

                                                                csAssert.assertAll();
                                                                return;


                                                            } else {

                                                                if (serviceDataType.contentEquals("forecast")) {
                                                                    if (!flowToTest.equals("forecast flow 6")) {
                                                                        String forecastTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                                                "forecasttemplatefilename");

                                                                        boolean editAndUploadForecast = editAndUploadForecastSheet(forecastTemplateFilePath, forecastTemplateFileName, flowToTest, "newClient" + contractId, contractId, pricingObj);


                                                                        if (!editAndUploadForecast) {
                                                                            logger.error("For Flow [{}] , edit and Upload Forecast sheet is failing so Skipping the further Part", flowToTest);
                                                                            throw new SkipException("Skipping this test");
                                                                        }
                                                                    }

                                                                }

                                                            }
                                                            listOfInvoiceTestDetails.add(new InvoiceDetails(contractId, serviceDataId, flowToTest));
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

                                            addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                            csAssert.assertAll();
                                            return;
                                        }
                                    } else {
                                        logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                                        csAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                                "Hence skipping validation");

                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                        csAssert.assertAll();

                                        return;

                                    }

                                } else {
                                    logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);

                                    csAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");
                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                    csAssert.assertAll();
                                    return;
                                }
                            }
                        } else if (publishFlag.equals("true")) {

                            serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                    "servicedatatype");

                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                            {

                                boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, serviceDataId, publishAction);
                                // if service data got published

                                if (!result) {

                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                                    csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");
                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);

                                    csAssert.assertAll();
                                    return;

                                } else {

                                    if (serviceDataType.contentEquals("forecast")) {
                                        if (!flowToTest.equals("forecast flow 6")) {
                                            String forecastTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                    "forecasttemplatefilename");

                                            boolean editAndUploadForecast = editAndUploadForecastSheet(forecastTemplateFilePath, forecastTemplateFileName, flowToTest, "newClient" + contractId, contractId, pricingObj);


                                            if (!editAndUploadForecast) {
                                                logger.error("For Flow [{}] , edit and Upload Forecast sheet is failing so Skipping the further Part", flowToTest);
                                                throw new SkipException("Skipping this test");
                                            }
                                        }

                                    }


                                    listOfInvoiceTestDetails.add(new InvoiceDetails(contractId, serviceDataId, flowToTest));
                                    listOfInvoiceTestDetailsCount++;
                                }
                            } else {
                                listOfInvoiceTestDetails.add(new InvoiceDetails(contractId, serviceDataId, flowToTest));
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
        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);

        csAssert.assertAll();
    }

    @AfterMethod(groups = "part1")
    public void afterPart1(){
        testCaseCount--;
    }

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


                    if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                    {

                        if (flowToTest.equalsIgnoreCase("vol pricing flow consumption unavailable") ||
                                flowToTest.equalsIgnoreCase("forecast flow 4")) {
                        } else {
                            // function to get status whether consumptions have been created or not
                            String consumptionCreatedStatus = waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
                            logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                            if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                csAssert.assertTrue(false,"Consumption Creation Task failed");
                                csAssert.assertAll();
                                return;
                            } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                                csAssert.assertTrue(false,"Consumption Creation Task failed");
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
                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
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
                                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                csAssert.assertAll();
                                                return;
                                            }
                                        }
                                    }

                                }

                            }
                        }
                    }// Consumption Part will End here
//                    }
                    if (consumptionIds.size() != 0) {
                        consumptionId = consumptionIds.get(0);
                    } else {
                        consumptionId = -1;
                    }

                    validateBillingDataScenarios(flowToTest, serviceDataId, consumptionId, "-1", "-1", csAssert);

                    //Get Invoice Id
                    int invoiceId = getInvoiceId(flowToTest);
                    int invoiceLineItemId = -1;
                    logger.info("Created Invoice Id is : [{}]", invoiceId);
                    if (invoiceId != -1) {
                        //Get Invoice Line Item Id
                        invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataId);
                        logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                    } else {
                        logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                        csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    }


                    if (invoiceLineItemId != -1) {

                        validateBillingDataScenarios(flowToTest, serviceDataId, consumptionId, String.valueOf(invoiceId), String.valueOf(invoiceLineItemId), csAssert);

                        verifyInvoiceLineItem(flowToTest, invoiceLineItemId, csAssert);

//                        validateAmountValues()
                        validateSystemAmt(flowToTest, serviceDataId, consumptionId, invoiceLineItemId, csAssert);

                        validateMemoScenarios(flowToTest, serviceDataId, consumptionId, invoiceId, invoiceLineItemId, csAssert);

                        validateListPageValues(flowToTest, invoiceId, csAssert);

                        HashMap<String, String> expectedValues = expectedValuesAuditLog(flowToTest);

                        valAuditLogAfterValidation(invoiceLineItemId, expectedValues, flowToTest, csAssert);

                        //Added by gaurav bhadani for rate card validation
//                        if (flowToTest.equals("arc flow rate card") || flowToTest.equals("fixed fee flow rate card") || flowToTest.equals("forecast flow rate card")) {
                        Boolean rateCardValidationStatus = rateCardValidation(invoiceId, invoiceLineItemId, flowToTest, csAssert);

                        if (rateCardValidationStatus) {
                            csAssert.assertTrue(true, "Rate Card Validated successfully for the flow " + flowToTest + " and invoice " + invoiceId);
                        } else {
                            csAssert.assertTrue(false, "Rate Card Validated unsuccessfully for the flow " + flowToTest + " and invoice " + invoiceId);
                        }
//                        }

                        //Check references tab test case : C4265
                        if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast")) {
                            if (validateReferencesTab(serviceDataId, invoiceLineItemId, consumptionIds, csAssert)) {
                                logger.info("References tab validated successfully for invoice line item " + invoiceLineItemId);
                                csAssert.assertTrue(true, "References tab validated successfully for invoice line item " + invoiceLineItemId);
                            } else {
                                logger.error("References tab validated unsuccessfully for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                                csAssert.assertTrue(false, "References tab validated unsuccessfully for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                            }
                        }
                        validate_SIR_14832(invoiceLineItemId, flowToTest, csAssert);


                    } else {
                        logger.error("Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                        csAssert.assertTrue(false, "Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [" +
                                flowToTest + "]");
                    }
                    listOfInvoiceTestDetails.remove(0);
                }
            } catch (Exception e) {
                logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());

                csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
            }


            //testCaseCount--;


            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            logger.info("listOfInvoiceTestDetails [{}] ,testCaseCount [{}]", listOfInvoiceTestDetails, testCaseCount);
            logger.info("listOfInvoiceTestDetailsCount [{}]", listOfInvoiceTestDetailsCount);

        } while (listOfInvoiceTestDetails.size() != 0 || testCaseCount != 0);

        csAssert.assertAll();

    }

    @Test(dataProvider = "dataProviderForSubServiceDataTabValidation", priority = 1, enabled = false)
    public void testSubServiceDataTabValidation(String flowToTest) {

        logger.info("Validating Sub Service Data Tab for Service Data");

        CustomAssert csAssert = new CustomAssert();

        String parentClientId;
        Boolean subServiceDataValidationStatus;
        Create create = new Create();
        Clone clone = new Clone();
        JSONObject valuesJson = new JSONObject();
        try {
            String serviceData = flowIdServiceDataIdMap.get(flowToTest);
            if (serviceData == null) {
                throw new SkipException("Skipping the test as Service Data Id is null");
            }
            int serviceDataIdParent = Integer.parseInt(serviceData);
            String cloneResponse = clone.hitClone(serviceDataEntity, serviceDataIdParent);

            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");
            if (!dataJson.getJSONObject("parentService").has("values")) {
                parentClientId = dataJson.getJSONObject("serviceIdClient").getString("values");
                valuesJson.put("name", parentClientId);
                dataJson.getJSONObject("parentService").put("values", valuesJson);
            }
            String serviceIdClientChild = dataJson.getJSONObject("serviceIdClient").getString("values");
            serviceIdClientChild = serviceIdClientChild + "_Child12";
            dataJson.getJSONObject("serviceIdClient").put("values", serviceIdClientChild);

            String serviceIdSupplierChild = dataJson.getJSONObject("serviceIdSupplier").getString("values");
            serviceIdSupplierChild = serviceIdSupplierChild + "_Child12";
            dataJson.getJSONObject("serviceIdSupplier").put("values", serviceIdSupplierChild);
            dataJson.remove("history");

            JSONObject createEntityJson = new JSONObject();
            JSONObject createEntityBodyJson = new JSONObject();
            createEntityBodyJson.put("data", dataJson);
            createEntityJson.put("body", createEntityBodyJson);

            create.hitCreate(serviceDataEntity, createEntityJson.toString());
            String createResponse = create.getCreateJsonStr();
            int newServiceDataIdChild = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            if (newServiceDataIdChild == -1) {
                logger.error("Unable to create child service data");
                csAssert.assertTrue(false, "Unable to create child service data");
                csAssert.assertAll();
                return;
            }
            Map<Integer, Map<String, String>> subServiceDataRowWiseColumnMapping = getRowColumnMappingForSubServiceData(serviceDataIdParent);

            if (subServiceDataRowWiseColumnMapping.size() == 0) {
                logger.error("No row created for child service data for Master service data " + serviceDataIdParent);
                csAssert.assertTrue(false, "No row created for child service data for Master service data " + serviceDataIdParent);
            } else {
                subServiceDataValidationStatus = validateSubServiceColumnValues(subServiceDataRowWiseColumnMapping, newServiceDataIdChild, csAssert);
                if (subServiceDataValidationStatus == true) {
                    csAssert.assertTrue(true, "Sub Service Data Validated successfully for service data " + serviceDataEntity);
                } else {
                    csAssert.assertTrue(false, "Sub Service Data Validated unsuccessfully for service data " + serviceDataEntity);
                }
            }

        } catch (Exception e) {
            logger.error("Exception while validating tabs on service data entity");
            csAssert.assertTrue(false, "Exception while validating tabs on service data entity " + e.getStackTrace());
        }
        csAssert.assertAll();
    }

    private Boolean validateSubServiceColumnValues(Map<Integer, Map<String, String>> subServiceDataRowWiseColumnMapping, int newServiceDataIdChild, CustomAssert csAssert) {

        logger.info("Inside validateSubServiceColumnValues method");

        Boolean validationStatus = true;
        Boolean childServiceDataExistence = false;
        Integer showPageIdChildServiceData;
        String showPageResponse;
        String invoicingType;
        String columnName = "";
        String columnValueOnSubServiceDataTab;
        String valueOnShowPage;
        Show show = new Show();
        Map<String, String> columnNameValueMap;
        for (int row = 0; row < subServiceDataRowWiseColumnMapping.size(); row++) {

            columnNameValueMap = subServiceDataRowWiseColumnMapping.get(row);
            try {
                showPageIdChildServiceData = Integer.parseInt(columnNameValueMap.get("id").split(":;")[1]);
            } catch (Exception e) {
                logger.error("Exception while getting show page id from columnNameValueMap");
                csAssert.assertTrue(false, "Exception while getting show page id from columnNameValueMap");
                validationStatus = false;
                continue;
            }
            invoicingType = columnNameValueMap.get("invoicingtype");
            show.hitShow(serviceDataEntityTypeId, showPageIdChildServiceData);
            showPageResponse = show.getShowJsonStr();
            innerLoop:
            for (Map.Entry<String, String> entry : columnNameValueMap.entrySet()) {
                try {
                    columnName = entry.getKey();
                    columnValueOnSubServiceDataTab = entry.getValue();

                    if ((invoicingType.equals("ARC/RRC")) && (columnName.equals("forecastperiod") || columnName.equals("forecastfrequency") || columnName.equals("forecasttype"))) {

                        if (columnValueOnSubServiceDataTab.equals("null")) {
                            logger.info("Values of forecast related columns equals to null for ARC/RRC invoicing data type");
                        } else {
                            logger.error("Values of forecast related columns not equals to null for ARC/RRC invoicing data type");
                            csAssert.assertTrue(false, "Values of forecast related columns not equals to null for ARC/RRC invoicing data type");
                            validationStatus = false;
                        }
                        continue innerLoop;
                    } else if (invoicingType.equals("Forecast") && (columnName.equals("forecastperiod"))) {
                        String forecastPeriod = ShowHelper.getValueOfField(serviceDataEntityTypeId, columnName, showPageResponse);
                        if (forecastPeriod.contains(columnValueOnSubServiceDataTab)) {
                            logger.info("Value for column " + columnName + " on sub Service data equals value on show page for child service data " + showPageIdChildServiceData);
                        } else {
                            logger.error("Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                            csAssert.assertTrue(false, "Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                        }
                        continue innerLoop;
                    }
                    if (columnName.equals("country") || columnName.equals("state") || columnName.equals("bulkcheckbox")) {
                        logger.info("Skipping the column " + columnName);
                        continue innerLoop;
                    }
                    if (columnName.equals("display_name")) {
                        String display_name = ShowHelper.getValueOfField(serviceDataEntityTypeId, "name", showPageResponse)
                                + " ("
                                + ShowHelper.getValueOfField(serviceDataEntityTypeId, "serviceidclient", showPageResponse)
                                + ")";

                        if (display_name.equals(columnValueOnSubServiceDataTab)) {
                            logger.info("Value for column " + columnName + " on sub Service data equals value on show page for child service data " + showPageIdChildServiceData);
                        } else {
                            logger.error("Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                            csAssert.assertTrue(false, "Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                            validationStatus = false;
                        }
                        continue innerLoop;
                    }
                    valueOnShowPage = ShowHelper.getValueOfField(serviceDataEntityTypeId, columnName, showPageResponse);
                    if (columnName.equals("id")) {
                        if (String.valueOf(showPageIdChildServiceData).equals(valueOnShowPage)) {
                            logger.info("Value for column " + columnName + " on sub Service data equals value on show page for child service data " + showPageIdChildServiceData);
                            if (showPageIdChildServiceData.equals(newServiceDataIdChild)) {
                                childServiceDataExistence = true;
                            }
                        } else {
                            logger.error("Value for column " + columnName + " on sub Service data does not equal value on show page for child service data " + showPageIdChildServiceData);
                            csAssert.assertTrue(false, "Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                            validationStatus = false;
                        }
                        continue innerLoop;
                    }
                    if (columnName.equals("contractservice")) {
                        if (columnValueOnSubServiceDataTab.equals("null") && valueOnShowPage.equals("false")) {
                            logger.info("Value for column " + columnName + " on sub Service data equals value on show page for child service data " + showPageIdChildServiceData);
                        } else if ((!columnValueOnSubServiceDataTab.equals("null")) && valueOnShowPage.equals("true")) {
                            logger.info("Value for column " + columnName + " on sub Service data equals value on show page for child service data " + showPageIdChildServiceData);
                        } else {
                            csAssert.assertTrue(false, "Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                            logger.error("Value for column " + columnName + " on sub Service data does not equal value on show page for child service data " + showPageIdChildServiceData);
                            validationStatus = false;
                        }
                        continue innerLoop;
                    }
                    if (columnName.equals("contract")) {
                        if (columnValueOnSubServiceDataTab.split(":;")[0].equals(valueOnShowPage)) {
                            logger.info("Value for column " + columnName + " on sub Service data equals value on show page for child service data " + showPageIdChildServiceData);
                        } else {
                            logger.error("Value for column " + columnName + " on sub Service data equals value on show page for child service data " + showPageIdChildServiceData);
                            csAssert.assertTrue(false, "Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                            validationStatus = false;
                        }
                        continue innerLoop;
                    }
                    if (valueOnShowPage.equals(columnValueOnSubServiceDataTab)) {
                        logger.info("Value for column " + columnName + " on sub Service data equals value on show page for child service data " + showPageIdChildServiceData);
                    } else {
                        logger.error("Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                        csAssert.assertTrue(false, "Value for column " + columnName + " on sub Service data does not equals value on show page for child service data " + showPageIdChildServiceData);
                        validationStatus = false;
                    }
                } catch (Exception e) {
                    logger.error("Exception while validating for row " + row + " and column " + columnName);
                    csAssert.assertTrue(false, "Exception while validating for row " + row + " and column " + columnName);
                    validationStatus = false;
                }
            }
        }
        if (childServiceDataExistence == false) {
            logger.error("Child Service data " + newServiceDataIdChild + " does not exist under sub service data for Service Data");
            csAssert.assertTrue(false, "Child Service data " + newServiceDataIdChild + " does not exist under sub service data for Service Data");
            validationStatus = false;
        }
        return validationStatus;
    }

    private Map<Integer, Map<String, String>> getRowColumnMappingForSubServiceData(int serviceDataIdParent) {

        String filterPayload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
        ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
        listRendererTabListData.hitListRendererTabListData(subServiceDataTabId, serviceDataEntityTypeId, serviceDataIdParent, filterPayload);
        String tabListDataResponse = listRendererTabListData.getTabListDataJsonStr();
        JSONArray tabListDataJsonArray = new JSONObject(tabListDataResponse).getJSONArray("data");

        Map<Integer, Map<String, String>> subServiceDataRowWiseColumnMapping = new HashMap<>();
        Map<String, String> columnNameValueMap;
        JSONObject individualDataRowJson;
        JSONObject individualColumnJson;
        JSONArray columnValuesIndividualRowJSONArray;
        String columnName;
        String columnValue;
        for (int row = 0; row < tabListDataJsonArray.length(); row++) {
            individualDataRowJson = tabListDataJsonArray.getJSONObject(row);
            columnValuesIndividualRowJSONArray = JSONUtility.convertJsonOnjectToJsonArray(individualDataRowJson);
            columnNameValueMap = new HashMap<>();
            for (int j = 0; j < columnValuesIndividualRowJSONArray.length(); j++) {
                individualColumnJson = columnValuesIndividualRowJSONArray.getJSONObject(j);
                columnName = individualColumnJson.get("columnName").toString();
                columnValue = individualColumnJson.get("value").toString();
                columnNameValueMap.put(columnName, columnValue);
            }
            subServiceDataRowWiseColumnMapping.put(row, columnNameValueMap);
        }
        return subServiceDataRowWiseColumnMapping;
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
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, testAllFlows);
            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
                flowsToTest = ParseConfigFile.getAllSectionNames(flowsConfigFilePath, flowsConfigFileName);
            } else {
                String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowsToValidate).split(Pattern.quote(","));
                for (String flow : allFlows) {
                    if (ParseConfigFile.containsSection(flowsConfigFilePath, flowsConfigFileName, flow.trim())) {
                        flowsToTest.add(flow.trim());
                    } else {
                        logger.info("Flow having name [{}] not found in Invoice Config File.", flow.trim());
                    }
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

                logger.info("Contract Creation Response for the flow " + flowToTest + " " + createResponse);
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

                logger.info("Service Data Creation Response for the flow " + flowToTest + " " + createResponse);

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

            logger.info("Invoice Creation Response for the flow " + flowToTest + " " + createResponse);

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

            logger.info("Invoice Line Item Creation Response for the flow " + flowToTest + " " + createResponse);

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

    /*
   This method will return the status of Forecast Scheduler as String.
   Possible Values are 'Pass', 'Fail', 'Skip'.
   'Pass' specifies that Forecast scheduler completed and records processed successfully.
   'Fail' specifies that Forecast scheduler failed
   'Skip' specifies that Forecast scheduler didn't finish within time.
 */
    private String waitForForecastScheduler(String flowToTest, List<Integer> oldIds) {
        String result = "pass";
        logger.info("Waiting for Forecast Scheduler to Complete for Flow [{}].", flowToTest);
        try {
            logger.info("Time Out for Forecast Scheduler is {} milliseconds", forcastSchedulerTimeOut);
            long timeSpent = 0;
            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            logger.info("Getting Task Id of Forecast Upload Job");
            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), oldIds);

            if (newTaskId != -1) {
                Boolean taskCompleted = false;
                logger.info("Checking if Forecast Upload Task has completed or not.");

                while (timeSpent < forcastSchedulerTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    fetchObj.hitFetch();
                    logger.info("Getting Status of Forecast Upload Task.");
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
//                    if(newTaskStatus.equals("Completed With Errors")){
//                        result = "fail";
//                        break;
//                    }
                    if (newTaskStatus != null && newTaskStatus.trim().contains("Completed")) {
                        taskCompleted = true;
                        logger.info("Forecast Upload Task Completed. ");
                        logger.info("Checking if Forecast Upload Task failed or not.");
                        if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                            result = "fail";

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Forecast Upload Task is not finished yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= forcastSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }
            } else {
                logger.info("Couldn't get Forecast Upload Task Job Id. Hence waiting for Task Time Out i.e. {}", forcastSchedulerTimeOut);
                Thread.sleep(forcastSchedulerTimeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Forecast Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
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

    private void setEntitySectionsForFlow(String flowToTest, String contractSectionName,
                                          String serviceDataSectionName, String invoiceSectionName,
                                          String invoiceLineItemSectionName
    ) {
        logger.info("Setting Entity Sections for Flow [{}]", flowToTest);
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname");
            if (temp != null)
                contractSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
            if (temp != null)
                serviceDataSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
            if (temp != null)
                invoiceSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();
        } catch (Exception e) {
            logger.error("Exception while Setting Entity Sections for Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
    }

    private void clearEntitySections(String contractSectionName, String serviceDataSectionName,
                                     String invoiceSectionName, String invoiceLineItemSectionName) {
        contractSectionName = serviceDataSectionName = invoiceSectionName = invoiceLineItemSectionName = "default";
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

            if(consumptionIds.size() == 0){
                result = "fail";
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


    // this will create the row , <columnNumber,value> for editing the Forecast Sheet
    private Map<Integer, Map<Integer, Object>> getValuesMapForForecastSheet(String flowToTest, String clientId) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {


            String rowNumber = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "forecastsheetrownumber");

            String[] columnNumbers = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "forecastsheetcolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                    "forecastsheetvalue").trim().split(Pattern.quote(","));


            Map<Integer, Object> innerValuesMap = new HashMap<>();
            for (int j = 0; j < columnNumbers.length; j++) {

                if (values[j].toLowerCase().contentEquals("clientid")) {

                    innerValuesMap.put(Integer.parseInt(columnNumbers[j]), clientId);
                    continue;
                }
                try{
                    int val = Integer.parseInt(values[j]);
                    innerValuesMap.put(Integer.parseInt(columnNumbers[j]), val);
                }catch (Exception e){
                    innerValuesMap.put(Integer.parseInt(columnNumbers[j]), values[j]);
                }

            }
            valuesMap.put(Integer.parseInt(rowNumber), innerValuesMap);

        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Forecast Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    // this will update the forecast sheet based on the map created in getValuesMapForForecastSheet
    synchronized boolean editForecastSheet(String templateFileName, String flowToTest, String clientId, Integer contractId, InvoicePricingHelper pricingObj) {

        Boolean pricingFile = false;
        Map<Integer, Map<Integer, Object>> forcastValuesMap = getValuesMapForForecastSheet(flowToTest, clientId);


        try {

            boolean editTemplate = pricingObj.editPricingTemplateMultipleRows(forecastTemplateFilePath, templateFileName, forecastSheetNameInXLSXFile,
                    forcastValuesMap);

            if (editTemplate == true) {
                return true;
            } else {
                logger.error("Error While Updating the Forecast Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, contractId);
                return false;
            }


        } catch (Exception e) {
            logger.error("Exception while getting Forecast Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;


    }

    // this will update the forecast sheet based on the map created in getValuesMapForForecastSheet and then upload that file
    synchronized boolean editAndUploadForecastSheet(String forecastTemplateFilePath, String templateFileName, String flowToTest, String clientId, Integer contractId, InvoicePricingHelper pricingObj) {


        boolean result = true;

        boolean flag = editForecastSheet(templateFileName, flowToTest, clientId, contractId, pricingObj);

        if (flag) {


            //Kill All Scheduler Tasks if Flag is On.
//            if (killAllSchedulerTasks) {
//                logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
//                killAllSchedulerTasks();
//            }

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());


            String forecastUploadResponse = ForecastUploadHelper.uploadSheet(forecastTemplateFilePath, templateFileName, contractId);

            if (forecastUploadResponse != null && forecastUploadResponse.trim().contains("200:;")) {

                //Wait for Forecast Scheduler to Complete
                String forecastSchedulerStatus = waitForForecastScheduler(flowToTest, allTaskIds);

                if (forecastSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                    logger.error("Forecast Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                    return false;
                } else if (forecastSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                    logger.info("Forecast Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
                    if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                        logger.error("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                        return false;
                    } else {
                        logger.warn("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow and Skipping it[{}]", flowToTest);
                        throw new SkipException("Skipping this test");
                    }


                }

                boolean isForecastCreated = isForecastCreated(contractId);
                if (!isForecastCreated) {
                    logger.error("Forecast is not getting listed under forecast tab for Contract Id : [{}]", contractId);
                    return false;
                }


            } else {
                logger.error("Error While Uploading the Forecast sheet  : [{}]", flowToTest);
                return false;
            }
        } else {

            logger.error("Error in Editing the Forecast Sheet for this flow : [{}]", flowToTest);
            return false;
        }


        return result;
    }


    // this function will check whether any data has been created under forecast tab of Contract or Not
    boolean isForecastCreated(int contractId) {

        logger.info("Checking whether forecast has/have been created and visible under contract Forecast Tab");

        int contractTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
        GetTabListData getTabListData = new GetTabListData(contractTypeId, forecastTabId, contractId);
        getTabListData.hitGetTabListData();
        String forecastTabListDataResponse = getTabListData.getTabListDataResponse();


        boolean isListDataValidJson = APIUtils.validJsonResponse(forecastTabListDataResponse, "[forecast tab list data response]");


        if (isListDataValidJson) {


            JSONObject jsonResponse = new JSONObject(forecastTabListDataResponse);
            int filterCount = jsonResponse.getInt("filteredCount");
            if (filterCount > 0) //very lenient check cab be modified
            {
                return true;
            } else {
                logger.error("There is no data in Forecast tab for Contract Id : [{}]", contractId);
                return false;
            }


        } else {
            logger.error("Forecast tab List Data Response is not valid Json for Contract Id :[{}] ", contractId);
            return false;

        }


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

    //Method rateCardValidation is written to validate rate card fields on Details tab of Invoices
    //Author : Gaurav Bhadani Date : 8-Sept-2020
    Boolean rateCardValidation(int invoiceId, int invoiceLineItemId, String flowToTest,CustomAssert customAssert) {

         logger.info("Inside rateCardValidation method");
        logger.info("Validating rate Card for Flow [{}], Invoice ID [{}], Invoice Line Item ID [{}] ", flowToTest, invoiceId, invoiceLineItemId);
        Boolean rateCardValidation = true;
        try {
            String expectedRateCardPermission = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,flowToTest,"rate card to disp");
            int listId1 = 0;
            int listId2 = 0;

            GetDetailsMetadata getDetailsMetadata = new GetDetailsMetadata();
            String metadataResponse = getDetailsMetadata.hitGetDetailsMetadata(invoiceLineItemId);

            String rateCardPermission = "false";
            if (JSONUtility.validjson(metadataResponse)) {

                JSONObject metadataRespJson = new JSONObject(metadataResponse);

                rateCardPermission = metadataRespJson.get("rateCardPermission").toString();

                if(expectedRateCardPermission.equalsIgnoreCase("true")) {
                    if (!rateCardPermission.equalsIgnoreCase("true")) {
                        customAssert.assertTrue(false, "On Rate Card Window View Rate Card button is not displayed wheres it was supposed to be present for the flow " + flowToTest);
                    }
                }else {
                    if (!rateCardPermission.equalsIgnoreCase("false")) {
                        customAssert.assertTrue(false, "On Rate Card Window View Rate Card button is displayed wheres it was supposed to be not present for the flow " + flowToTest);
                        rateCardValidation = false;
                    }
                    return rateCardValidation;
                }

            } else {
                customAssert.assertTrue(false, "Rate card Response is an invalid json for View Rate Card Button Metadata for the flow " + flowToTest);
                rateCardValidation = false;
            }

            if(expectedRateCardPermission.equalsIgnoreCase("false") || rateCardPermission.equalsIgnoreCase("false")){
               return  rateCardValidation;
            }

            try {
                listId1 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(rateCardConfigFilePath, rateCardConfigFileName, "listid1"));
                listId2 = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(rateCardConfigFilePath, rateCardConfigFileName, "listid2"));

            } catch (Exception e) {
                logger.error("Exception while parsing " + rateCardConfigFileName + e.getStackTrace());
                customAssert.assertTrue(false,"Exception while parsing " + rateCardConfigFileName + e.getStackTrace());
            }

            String payload = "{\"filterMap\":{\"entityTypeId\":165,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

            logger.info("Hitting List Renderer tab list data for getting rate card response");
            ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
            listRendererTabListData.hitListRendererTabListData(listId1, invoiceLineItemEntityTypeId, invoiceLineItemId, payload);
            String tabListResponse = listRendererTabListData.getTabListDataJsonStr();

            if (APIUtils.validJsonResponse(tabListResponse)) {
                logger.info("Valid json response for tabListResponse");
            } else {
                logger.error("Invalid json response for tabListResponse");
                customAssert.assertTrue(false, "Rate Card Tab Window response is an invalid json for list id " + listId1 + " for the flow " + flowToTest);
                rateCardValidation = false;
            }

            listRendererTabListData.hitListRendererTabListData(listId2, invoiceLineItemEntityTypeId, invoiceLineItemId, payload);
            String tabListResponse2 = listRendererTabListData.getTabListDataJsonStr();

            if (APIUtils.validJsonResponse(tabListResponse2)) {
                logger.info("Valid json response for tabListResponse");
            } else {
                logger.error("Invalid json response for tabListResponse");
                customAssert.assertTrue(false, "Rate Card Tab Window response is an invalid json for list id " + listId2 + " for the flow " + flowToTest);
                rateCardValidation = false;
            }
            Map<Integer, HashMap<String, String>> rateCardMap = getRateCard(tabListResponse);

            String numOfRows = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "exp num of rows");

            if (numOfRows != null) {
                int noRows = Integer.parseInt(numOfRows);
                HashMap<String, String> expectedColValues = new HashMap<>();
                HashMap<String, String> actualColValues = new HashMap<>();

                for (int i = 0; i < noRows; i++) {

                    if(rateCardValidation == false){
                        break;
                    }
                    String[] rateCardColValues = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "rate card row " + i).split(",");

                    for (int j = 0; j < rateCardColValues.length; j++) {

                        String[] rateCardColNameValue = rateCardColValues[j].split("->");
                        try {
                            expectedColValues.put(rateCardColNameValue[0], rateCardColNameValue[1]);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            expectedColValues.put(rateCardColNameValue[0], "");
                        }
                    }
                    actualColValues = rateCardMap.get(i);

                    for (Map.Entry<String, String> expectedColVal : expectedColValues.entrySet()) {
                        String expColName = expectedColVal.getKey();
                        String expColValue = expectedColVal.getValue();

                        try {

                            String actualColValue = actualColValues.get(expColName);

                            if (!expColValue.equalsIgnoreCase(actualColValue)) {
                                customAssert.assertTrue(false, "Rate Card Window Expected and Actual Column Value didn't match for column name " + expColName + " and row number " + i + " for the flow " + flowToTest + " Expected value " + expColValue + " Actual ColValue " + actualColValue );rateCardValidation = false;
                            }
                        } catch (Exception e) {
                            customAssert.assertTrue(false, "Exception while validating rate card window for column name " + expColName + " and row number " + i + " for the flow " + flowToTest);
                            rateCardValidation = false;
                            break;
                        }
                    }
                }
            }

//            }

        } catch (Exception e) {
            logger.error("Exception while rate card validation for the flow " + flowToTest);
            customAssert.assertTrue(false, "Exception while rate card validation for the flow " + flowToTest);
            rateCardValidation = false;

        }
        return rateCardValidation;
    }

    Boolean validateCurrencyConvRate(String tabListResponse, Float expectedCurrencyConvRate) {

        logger.info("Getting actual value of currency conv rate from tab list response");
        Boolean currencyConvRateValidation = false;
        try {

            if (APIUtils.validJsonResponse(tabListResponse)) {
                logger.info("Valid json response for tablistResponse");
            } else {
                logger.error("Invalid json response for tablistResponse");
                currencyConvRateValidation = false;
            }

            JSONObject tabListResponseJsonObject = new JSONObject(tabListResponse);
            JSONArray dataArray = tabListResponseJsonObject.getJSONArray("data");
            JSONObject dataObject;
            JSONArray indvdataArray;
            String columnName;
            Float actualValueOfCurrConvRate;


            for (int i = 0; i < dataArray.length(); i++) {
                dataObject = dataArray.getJSONObject(i);
                indvdataArray = JSONUtility.convertJsonOnjectToJsonArray(dataObject);

                for (int j = 0; j < indvdataArray.length(); j++) {

                    columnName = indvdataArray.getJSONObject(j).get("columnName").toString();
                    if (columnName.equals("currency_conversion_rate")) {
                        actualValueOfCurrConvRate = Float.parseFloat(indvdataArray.getJSONObject(j).get("value").toString());

                        if (actualValueOfCurrConvRate.equals(expectedCurrencyConvRate)) {

                            logger.info("Expected and Actual value of Currency Conversion Rate are equal");
                            currencyConvRateValidation = true;
                        } else {
                            logger.error("Expected and Actual value of Currency Conversion Rate are not equal");
                            currencyConvRateValidation = false;
                        }
                        return currencyConvRateValidation;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while parsing tab list response " + e.getStackTrace());
            currencyConvRateValidation = false;
        }
        return currencyConvRateValidation;
    }

    String getEntityCurrency(String entityShowPageResponse) {

        logger.info("Getting currency for entity");
        String entityCurrency = null;

        if (APIUtils.validJsonResponse(entityShowPageResponse)) {
            logger.info("Valid json response for entityShowPageResponse");
        } else {
            logger.error("Invalid json response for entityShowPageResponse");
        }

        JSONObject entityShowPageResponseJson = new JSONObject(entityShowPageResponse);
        try {
            entityCurrency = entityShowPageResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("currency").getJSONObject("values").get("shortName").toString();
        } catch (Exception e) {
            logger.error("Exception while parsing show page response for for currency field " + e.getStackTrace());
            entityCurrency = "null";
        }
        return entityCurrency;
    }

    Boolean validateVolumeBandARC(String volumeBandRateCard, int serviceDataId) {

        logger.info("Validating rate card volume band with volume band of service data ARC tab");
        Boolean volumeBandValidationStatus;

        String volumeBandServiceDataArcTab = getArcVolumeBand(serviceDataId);

        if (volumeBandRateCard.equals(volumeBandServiceDataArcTab)) {
            volumeBandValidationStatus = true;
        } else {
            logger.error("Different values for volume for rate card and service data arc tab");
            volumeBandValidationStatus = false;
        }

        return volumeBandValidationStatus;
    }

    String getArcVolumeBand(int serviceDataId) {

        logger.info("Getting volume bands from service data ARC Tab ");

        String lowerLevel = null;
        String upperLevel = null;

        String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"enddate\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";

        int arcListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(rateCardConfigFilePath, rateCardConfigFileName, "arclistid"));

        ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
        listRendererTabListData.hitListRendererTabListData(arcListId, serviceDataEntityTypeId, serviceDataId, payload);
        String arcTabListResponse = listRendererTabListData.getTabListDataJsonStr();

        try {
            if (APIUtils.validJsonResponse(arcTabListResponse)) {
                logger.info("Valid json response for arcTabListResponse");
            } else {
                logger.error("Invalid json response for arcTabListResponse");
            }

            JSONObject arcTabListResponseJson = new JSONObject(arcTabListResponse);
            JSONArray dataArray = arcTabListResponseJson.getJSONArray("data");
            JSONObject dataObject;
            dataObject = dataArray.getJSONObject(0);
            JSONArray indvDataArray = JSONUtility.convertJsonOnjectToJsonArray(dataObject);

            String columnName;

            for (int i = 0; i < indvDataArray.length(); i++) {
                columnName = indvDataArray.getJSONObject(i).get("columnName").toString();

                if (columnName.equals("lowerlevel")) {

                    lowerLevel = indvDataArray.getJSONObject(i).get("value").toString();
                }
                if (columnName.equals("upperlevel")) {

                    upperLevel = indvDataArray.getJSONObject(i).get("value").toString();
                }
            }

        } catch (Exception e) {
            logger.error("Exception while parsing arc/rrc tab data of service data entity " + e.getStackTrace());
        }
        return lowerLevel + " - " + upperLevel;
    }

    Map<Integer,HashMap<String, String>> getRateCard(String tabListResponse) {

        logger.info(" Getting volume band for invoice rate card ");
        Map<Integer,HashMap<String, String>> rateCardMap  = new HashMap<>();

        try {
            if (APIUtils.validJsonResponse(tabListResponse)) {
                logger.info("Valid json response for tabListResponse");
            } else {
                logger.error("Invalid json response for tabListResponse");
            }

            JSONObject tablistResponseJson = new JSONObject(tabListResponse);
            JSONArray dataArray = tablistResponseJson.getJSONArray("data");
            JSONObject indvdatajson;

            String columnName;
            String columnValue;

            for (int i = 0; i < dataArray.length(); i++) {

                indvdatajson = dataArray.getJSONObject(i);

                Iterator<String> keys = indvdatajson.keys();

                HashMap<String, String> columnValueMap = new HashMap<>();

                while(keys.hasNext()) {
                    String key = keys.next();
                    columnName = indvdatajson.getJSONObject(key).get("columnName").toString();
                    columnValue = indvdatajson.getJSONObject(key).get("value").toString();

                    columnValueMap.put(columnName,columnValue);
                }

                rateCardMap.put(i,columnValueMap);

            }

        } catch (Exception e) {
            logger.error("Exception while parsing rate card tabListResponse Json " + e.getStackTrace());
        }
        return rateCardMap;
    }

    Boolean validateVolumeBandChargesForecast(HashMap<String, String> volumeBandMap) {

        logger.info("Validating volume band for charge type other than ARC");
        Boolean validationStatusVolumeBandChargesForecast = true;

        for (Map.Entry<String, String> entry : volumeBandMap.entrySet()) {

            if (entry.getKey().equals("ARC")) {
                continue;
            } else {
                if (entry.getValue().equals("-") || entry.getValue().equals("null")) {
                    logger.info("Valid value of volume band for charges type [{}]", entry.getKey());
                    validationStatusVolumeBandChargesForecast = true;

                } else {
                    logger.error("Value of volume band for charges type [{}] is invalid", entry.getKey());
                    validationStatusVolumeBandChargesForecast = false;
                }
            }
        }
        return validationStatusVolumeBandChargesForecast;
    }

    Boolean validateVolumeRateFixedFee(String rateCardTabListResponse, int serviceDataId) {

        logger.info("Validating volume and rate for fixed fee invoice line item service data");

        Boolean volumeRateFixedFeeValidationStatus = true;
        Double rateValueInvoiceLineItem = 0.00;
        Double volumeValueInvoiceLineItem = 0.00;
        Double volumeValueServiceData = 0.00;
        Double unitRateValueServiceData = 0.00;

        try {

            if (APIUtils.validJsonResponse(rateCardTabListResponse)) {
                logger.info("Valid json response for rateCardTabListResponse");
            } else {
                logger.error("Invalid json response for rateCardTabListResponse");
                volumeRateFixedFeeValidationStatus = false;
            }

            JSONObject rateCardTabListResponseJson = new JSONObject(rateCardTabListResponse);
            JSONArray dataArray = rateCardTabListResponseJson.getJSONArray("data");
            JSONObject indvdatajson = dataArray.getJSONObject(0);
            JSONArray indvdatajsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvdatajson);
            JSONObject indvjson;
            String columnName;

            for (int i = 0; i < indvdatajsonArray.length(); i++) {
                indvjson = indvdatajsonArray.getJSONObject(i);
                columnName = indvjson.get("columnName").toString();

                if (columnName.equals("rate")) {
                    rateValueInvoiceLineItem = Double.parseDouble(indvjson.get("value").toString());
                }

                if (columnName.equals("volume")) {
                    volumeValueInvoiceLineItem = Double.parseDouble(indvjson.get("value").toString());
                }
            }
        } catch (Exception e) {
            logger.error("Exception while parsing rate Card Tab List Response " + e.getStackTrace());
            volumeRateFixedFeeValidationStatus = false;
        }

        logger.info("Getting service data tab charges response");

        String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"enddate\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
        int chargesListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(rateCardConfigFilePath, rateCardConfigFileName, "chargeslistid"));

        logger.info("Hitting service data tab charges api");

        ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
        listRendererTabListData.hitListRendererTabListData(chargesListId, serviceDataEntityTypeId, serviceDataId, payload);
        String chargesTabListResponse = listRendererTabListData.getTabListDataJsonStr();

        try {
            if (APIUtils.validJsonResponse(chargesTabListResponse)) {
                logger.info("Valid json response for chargesTabListResponse");
            } else {
                logger.error("Invalid json response for chargesTabListResponse");
                volumeRateFixedFeeValidationStatus = false;
            }
            JSONObject chargesTabListResponseJson = new JSONObject(chargesTabListResponse);
            JSONArray dataArray = chargesTabListResponseJson.getJSONArray("data");
            JSONObject dataObject;
            dataObject = dataArray.getJSONObject(0);
            JSONArray indvDataArray = JSONUtility.convertJsonOnjectToJsonArray(dataObject);

            String columnName;

            for (int i = 0; i < indvDataArray.length(); i++) {
                columnName = indvDataArray.getJSONObject(i).get("columnName").toString();

                if (columnName.equals("volume")) {

                    volumeValueServiceData = Double.parseDouble(indvDataArray.getJSONObject(i).get("value").toString());
                }
                if (columnName.equals("unitrate")) {

                    unitRateValueServiceData = Double.parseDouble(indvDataArray.getJSONObject(i).get("value").toString());
                }
            }

        } catch (Exception e) {
            logger.error("Exception while parsing service data charges tab response " + e.getStackTrace());
            volumeRateFixedFeeValidationStatus = false;
        }

        logger.info("Validating values for volume on service data and invoice line item");
        if (volumeValueServiceData.equals(volumeValueInvoiceLineItem)) {
            logger.info("Volume value validated successfully on invoice line item rate card and service data charges tab");
        } else {
            logger.error("Volume value validated unsuccessfully on invoice line item rate card and service data charges tab");
            volumeRateFixedFeeValidationStatus = false;
        }

        if (unitRateValueServiceData.equals(rateValueInvoiceLineItem)) {
            logger.info("Rate value validated successfully on invoice line item rate card and service data charges tab");

        } else {
            logger.error("Rate value validated unsuccessfully on invoice line item rate card and service data charges tab");
            volumeRateFixedFeeValidationStatus = false;
        }

        return volumeRateFixedFeeValidationStatus;
    }

    Boolean validateRateColumnARC(String rateCardTabListResponse, String rateCardTabListResponse2, int serviceDataId) {

        logger.info("Validating values coming in rate card for ARC Service Data");
        Boolean rateCardColumnARCStatus = true;
        HashMap<Integer, HashMap<String, String>> rowWise_rateCardMapValue = new HashMap();

        try {
            if (APIUtils.validJsonResponse(rateCardTabListResponse)) {
                logger.info("Valid json response for rateCardTabListResponse");
            } else {
                logger.error("Invalid json response for rateCardTabListResponse");
                rateCardColumnARCStatus = false;
            }
            JSONArray dataarray = new JSONObject(rateCardTabListResponse).getJSONArray("data");
            JSONObject indvdataobject;
            JSONArray indvdataobjectjsonarray;
            JSONObject indvJson;
            String columnName;
            String columnValue;


            for (int i = 0; i < dataarray.length(); i++) {

                HashMap<String, String> rateCardMap = new HashMap<>();
                indvdataobject = dataarray.getJSONObject(i);
                indvdataobjectjsonarray = JSONUtility.convertJsonOnjectToJsonArray(indvdataobject);

                for (int j = 0; j < indvdataobjectjsonarray.length(); j++) {

                    indvJson = indvdataobjectjsonarray.getJSONObject(j);
                    columnName = indvJson.get("columnName").toString();
                    columnValue = indvJson.get("value").toString();
                    rateCardMap.put(columnName, columnValue);
                }
                rowWise_rateCardMapValue.put(i, rateCardMap);
            }

        } catch (Exception e) {
            logger.error("Exception occur while parsing Json response" + e.getStackTrace());
        }

        HashMap<String, String> rateCardMap;
        String chargeType;
        Double actualARCRate = 0.0;
        Double actual_line_item_amount;
        Double currencyConvFactor;
        Double amount;
        Double exp_line_item_amount;
        Double net_line_item_amount = 0.0;

        for (HashMap.Entry<Integer, HashMap<String, String>> entry : rowWise_rateCardMapValue.entrySet()) {
            rateCardMap = entry.getValue();
            chargeType = rateCardMap.get("charge_type");

            currencyConvFactor = Double.parseDouble(rateCardMap.get("currency_conversion_rate"));
            amount = Double.parseDouble(rateCardMap.get("amount"));
            exp_line_item_amount = currencyConvFactor * amount;
            actual_line_item_amount = Double.parseDouble(rateCardMap.get("line_item_amount"));

            net_line_item_amount += actual_line_item_amount;

            if (exp_line_item_amount.equals(actual_line_item_amount)) {
                logger.info("Expected and actual line_item_amount are same");
            } else {
                logger.error("Expected and actual line_item_amount are different");
            }

            if (chargeType.equals("ARC")) {
                actualARCRate = Double.parseDouble(rateCardMap.get("rate"));
            }

        }
        if (APIUtils.validJsonResponse(rateCardTabListResponse2)) {
            logger.info("Valid json response for rateCardTabListResponse2");
        } else {
            logger.error("Invalid json response for rateCardTabListResponse2");
            rateCardColumnARCStatus = false;
        }
        JSONObject tabListResponse2 = new JSONObject(rateCardTabListResponse2);
        JSONArray dataArray = tabListResponse2.getJSONArray("data");
        JSONObject indvJson = dataArray.getJSONObject(0);
        JSONArray indvJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvJson);
        JSONObject indvdatajson;
        String columnName;

        Double actual_LineItemAmount = 0.0;
        for (int i = 0; i < indvJsonArray.length(); i++) {
            indvdatajson = indvJsonArray.getJSONObject(i);
            columnName = indvdatajson.get("columnName").toString();

            if (columnName.equals("line_item_amount")) {
                actual_LineItemAmount = Double.parseDouble(indvdatajson.get("value").toString());
            }

        }

        logger.info("Validating line item amount total");
        if (actual_LineItemAmount.equals(net_line_item_amount)) {
            logger.info("Total of line item amount and sum total of line item amount are equal");
        } else {
            logger.error("Total of line item amount and sum total of line item amount are different");
            rateCardColumnARCStatus = false;
        }

        logger.info("Getting the value of rate from ARC Tab");

        String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"enddate\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
        int arcListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(rateCardConfigFilePath, rateCardConfigFileName, "arclistid"));

        logger.info("Hitting service data tab ARC/RRC api");

        ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
        listRendererTabListData.hitListRendererTabListData(arcListId, serviceDataEntityTypeId, serviceDataId, payload);
        String arcTabListResponse = listRendererTabListData.getTabListDataJsonStr();

        if (APIUtils.validJsonResponse(arcTabListResponse)) {
            logger.info("Valid json response for arcTabListResponse");
        } else {
            logger.error("Invalid json response for arcTabListResponse");
            rateCardColumnARCStatus = false;
        }

        JSONObject arcTabJson = new JSONObject(arcTabListResponse);
        dataArray = arcTabJson.getJSONArray("data");

        JSONObject indvDataJson = dataArray.getJSONObject(0);
        JSONArray indvDataArray = JSONUtility.convertJsonOnjectToJsonArray(indvDataJson);

        Double expectedRateValue = 0.0;
        JSONObject indvjson;
        for (int i = 0; i < indvDataArray.length(); i++) {

            indvjson = indvDataArray.getJSONObject(i);
            columnName = indvjson.get("columnName").toString();
            if (columnName.equals("rate")) {
                expectedRateValue = Double.parseDouble(indvjson.get("value").toString());
            }
        }

        if (expectedRateValue.equals(actualARCRate)) {
            logger.info("Rate values are same on ARC tab and Rate Card");

        } else {
            logger.error("Rate values are different on ARC tab and Rate Card");
            rateCardColumnARCStatus = false;
        }

        return rateCardColumnARCStatus;
    }

    Boolean rateCardValidationStatusForeCastType(String tabListResponse, String tabListResponse2, int serviceDataID) {

        logger.info("Validating invoice rate card for service date of forecast type");

        Boolean rateCardValidationStatusForeCastType = true;
        Map<String, String> rateCardValueMap = new HashMap<>();
        try {

            if (APIUtils.validJsonResponse(tabListResponse)) {
                logger.info("Valid json response for tabListResponse");
            } else {
                logger.error("Invalid json response for tabListResponse");
                rateCardValidationStatusForeCastType = false;
            }

            JSONObject tabListJson = new JSONObject(tabListResponse);
            JSONArray dataArray = tabListJson.getJSONArray("data");
            JSONObject indvDataJson = dataArray.getJSONObject(0);

            JSONArray indvDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvDataJson);
            JSONObject indvJson;
            String columnName;
            String columnVal;

            for (int i = 0; i < indvDataJsonArray.length(); i++) {

                indvJson = indvDataJsonArray.getJSONObject(i);
                columnName = indvJson.get("columnName").toString();
                columnVal = indvJson.get("value").toString();

                rateCardValueMap.put(columnName, columnVal);
            }
        } catch (Exception e) {
            logger.error("Exception while parsing json" + e.getStackTrace());
        }
        logger.info("Validating invoicing type");

        String invoicing_type = rateCardValueMap.get("invoicing_type");

        if (invoicing_type.equals("Forecast")) {
            logger.info("Valid value for invoicing type");
        } else {
            logger.error("Value for invoicing type other than Forecast");
            rateCardValidationStatusForeCastType = false;
        }

        logger.info("Validating rate");
        Float actualRate = Float.parseFloat(rateCardValueMap.get("rate"));
        Float expectedRate = 0f;
        try {
            GetTabListData getTabListData = new GetTabListData(serviceDataEntityTypeId, chargesTabId, serviceDataID);
            getTabListData.hitGetTabListData();
            String chargesTabListDataResponse = getTabListData.getTabListDataResponse();

            if (APIUtils.validJsonResponse(chargesTabListDataResponse)) {
                logger.info("Valid json response for chargesTabListDataResponse");
            } else {
                logger.error("Invalid json response for chargesTabListDataResponse");
                rateCardValidationStatusForeCastType = false;
            }

            JSONObject chargesTabListDataResponseJson = new JSONObject(chargesTabListDataResponse);
            JSONObject indvDataJson = chargesTabListDataResponseJson.getJSONArray("data").getJSONObject(0);

            JSONArray indvjsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvDataJson);
            String columnName;

            for (int i = 0; i < indvjsonArray.length(); i++) {
                columnName = indvjsonArray.getJSONObject(i).get("columnName").toString();
                if (columnName.equals("unitrate")) {
                    expectedRate = Float.parseFloat(indvjsonArray.getJSONObject(i).get("value").toString());
                }
            }
        } catch (Exception e) {
            logger.error("Exception while parsing json charges tab forecast service data" + e.getStackTrace());
        }
        if (expectedRate.equals(actualRate)) {
            logger.info("Expected and Actual value for rate are same");
        } else {
            logger.error("Expected and Actual value for rate are different");
            rateCardValidationStatusForeCastType = false;
        }

        return rateCardValidationStatusForeCastType;
    }

    private Boolean validateReferencesTab(int serviceDataId, int invoiceLineItemId, ArrayList<Integer> consumptionIds, CustomAssert csAssert) {

        Boolean referencesTabValidationStatus = true;

        try {
            int serviceDataIdExpected = serviceDataId;
            Show show = new Show();
            show.hitShow(serviceDataEntityTypeId, serviceDataId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJson = new JSONObject(showResponse);

            String serviceIdSupplier = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdSupplier").get("values").toString();
            String serviceDataName = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
            String expected_Service_Data = serviceDataName + " ( " + serviceIdSupplier + " ) ";

            ListRendererTabListData listRendererTabListData = new ListRendererTabListData();

            String tabListPayload = "{\"filterMap\":{\"entityTypeId\":" + consumptionsEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

            listRendererTabListData.hitListRendererTabListData(referenceTabId, invoiceLineItemEntityTypeId, invoiceLineItemId, tabListPayload);
            if (listRendererTabListData.getAPIResponseCode().contains("200")) {
                String referenceTabResponse = listRendererTabListData.getTabListDataJsonStr();
                if (APIUtils.validJsonResponse(referenceTabResponse)) {
                    JSONObject referencesTabListJson = new JSONObject(referenceTabResponse);
                    JSONArray dataArray = referencesTabListJson.getJSONArray("data");
                    JSONObject dataJson;
                    JSONArray dataJsonArray;
                    JSONObject indvColumnJson;
                    String actualConsumptionId = "";
                    String expectedConsumptionId;
                    String actualServiceDataName = "";
                    String actualServiceData = "";
                    String actualStatus = "";

                    for (int i = 0; i < dataArray.length(); i++) {
                        expectedConsumptionId = String.valueOf(consumptionIds.get(i));
                        dataJson = dataArray.getJSONObject(i);
                        dataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataJson);

                        for (int j = 0; j < dataJsonArray.length(); j++) {
                            indvColumnJson = dataJsonArray.getJSONObject(j);
                            if (indvColumnJson.get("columnName").equals("id")) {

                                actualConsumptionId = indvColumnJson.get("value").toString().split(":;")[1];
                            } else if (indvColumnJson.get("columnName").equals("name")) {
                                actualServiceDataName = indvColumnJson.get("value").toString();
                            } else if (indvColumnJson.get("columnName").equals("service_data")) {
                                actualServiceData = indvColumnJson.get("value").toString().split(":;")[0];
                            } else if (indvColumnJson.get("columnName").equals("status")) {
                                actualStatus = indvColumnJson.get("value").toString();
                            }
                        }
                        if (actualConsumptionId.equals(expectedConsumptionId)) {
                            logger.info("Expected and actual value matched on references tab for column consumption id ");
                            csAssert.assertTrue(true, "Expected and actual value matched on references tab for column  consumption id for invoice line item " + invoiceLineItemId);
                        } else {
                            logger.error("Expected and actual value mismatch on references tab for column consumption id ");
                            csAssert.assertTrue(false, "Expected and actual value mismatch on references tab for column consumption id for invoice line item " + invoiceLineItemId);
                            referencesTabValidationStatus = false;
                        }

                        if (actualServiceDataName.equals(serviceDataName)) {
                            logger.info("Expected and actual value matched on references tab for column service Data Name");
                            csAssert.assertTrue(true, "Expected and actual value matched on references tab for column service Data Name for invoice line item " + invoiceLineItemId);
                        } else {
                            logger.error("Expected and actual value mismatch on references tab for column service Data Name");
                            csAssert.assertTrue(false, "Expected and actual value mismatch on references tab for column service Data Name for invoice line item " + invoiceLineItemId);
                            referencesTabValidationStatus = false;
                        }

                        if (actualServiceData.equals(expected_Service_Data)) {
                            logger.info("Expected and actual value matched on references tab for column service Data");
                            csAssert.assertTrue(true, "Expected and actual value matched on references tab for column service Data for invoice line item " + invoiceLineItemId);
                        } else {
                            logger.error("Expected and actual value mismatch on references tab for column service Data");
                            csAssert.assertTrue(false, "Expected and actual value mismatch on references tab for column service Data for invoice line item " + invoiceLineItemId);
                            referencesTabValidationStatus = false;
                        }

                        if (actualStatus.equals("Upcoming")) {
                            logger.info("Expected and actual value matched on references tab for column Status");
                            csAssert.assertTrue(true, "Expected and actual value matched on references tab for column Status for invoice line item " + invoiceLineItemId);
                        } else {
                            logger.error("Expected and actual value mismatch on references tab for column service Data");
                            csAssert.assertTrue(false, "Expected and actual value mismatch on references tab for column service Data for invoice line item " + invoiceLineItemId);
                            referencesTabValidationStatus = false;
                        }
                    }
                } else {
                    logger.error("Reference tab response is not a valid json");
                    referencesTabValidationStatus = false;
                }

            } else {
                logger.error("Error while opening reference tab");
                referencesTabValidationStatus = false;
            }

        } catch (Exception e) {
            logger.error("Exception while validating references tab");
            referencesTabValidationStatus = false;
        }

        return referencesTabValidationStatus;
    }

    private Boolean validateAmountValues(String flowToTest, int invoiceLineItemId, HashMap<String, String> expectedAmountValuesMap, CustomAssert csAssert) {

        Boolean validationStatus = true;
        String showResponse;
        JSONObject showResponseJson;
        try {
            Show show = new Show();

            String finalConsumption = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "finalconsumptionvalues");

            show.hitShow(invoiceLineItemEntityTypeId, invoiceLineItemId);
            showResponse = show.getShowJsonStr();

            String supplierRateActual = ShowHelper.getValueOfField("rate", showResponse);
            if (!supplierRateActual.equals(expectedAmountValuesMap.get("supplierRate"))) {
                csAssert.assertEquals(supplierRateActual,expectedAmountValuesMap.get("supplierRate"), "Expected and Actual Value of supplierRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String systemRateActual = ShowHelper.getValueOfField("systemrate", showResponse);
            if (!systemRateActual.equals(expectedAmountValuesMap.get("systemRate"))) {
                csAssert.assertEquals(systemRateActual,expectedAmountValuesMap.get("systemRate"), "Expected and Actual Value of systemRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String discrepancyRateActual = ShowHelper.getValueOfField("discrepancyrate", showResponse);
            if (!discrepancyRateActual.equals(expectedAmountValuesMap.get("discrepancyRate"))) {
                csAssert.assertEquals(discrepancyRateActual,expectedAmountValuesMap.get("discrepancyRate"), "Expected and Actual Value of discrepancyRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String supplierConversionRateActual = ShowHelper.getValueOfField("conversionrate", showResponse);
            if (!supplierConversionRateActual.equals(expectedAmountValuesMap.get("supplierConversionRate"))) {
                csAssert.assertEquals(supplierConversionRateActual,expectedAmountValuesMap.get("supplierConversionRate"), "Expected and Actual Value of supplierConversionRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String systemConversionRateActual = ShowHelper.getValueOfField("systemconversionrate", showResponse);
            if (!systemConversionRateActual.equals(expectedAmountValuesMap.get("systemConversionRate"))) {
                csAssert.assertEquals(systemConversionRateActual,expectedAmountValuesMap.get("systemConversionRate"), "Expected and Actual Value of systemConversionRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }
            String discrepancyConversionRateActual = ShowHelper.getValueOfField("discrepancyconversionrate", showResponse);
            if (!discrepancyConversionRateActual.equals(expectedAmountValuesMap.get("discrepancyConversionRate"))) {
                csAssert.assertEquals(discrepancyConversionRateActual,expectedAmountValuesMap.get("discrepancyConversionRate"), "Expected and Actual Value of discrepancyConversionRate doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String supplierQuantityActual = ShowHelper.getValueOfField("quantity", showResponse);
            if (!supplierQuantityActual.equals(expectedAmountValuesMap.get("supplierQuantity"))) {
                csAssert.assertEquals(supplierQuantityActual,expectedAmountValuesMap.get("supplierQuantity"), "Expected and Actual Value of supplierQuantity doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }
            String systemQuantityActual = ShowHelper.getValueOfField("systemquantity", showResponse);
            if (!systemQuantityActual.equals(expectedAmountValuesMap.get("systemQuantity"))) {
                csAssert.assertEquals(systemQuantityActual,expectedAmountValuesMap.get("systemQuantity"), "Expected and Actual Value of systemQuantity doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String discrepancyQuantityActual = ShowHelper.getValueOfField("discrepancyquantity", showResponse);
            if (!discrepancyQuantityActual.equals(expectedAmountValuesMap.get("discrepancyQuantity"))) {
                csAssert.assertEquals(discrepancyQuantityActual,expectedAmountValuesMap.get("discrepancyQuantity"), "Expected and Actual Value of discrepancyQuantity doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String amountActual = ShowHelper.getValueOfField("amount", showResponse);
            if (!amountActual.equals(expectedAmountValuesMap.get("amount"))) {
                csAssert.assertEquals(amountActual,expectedAmountValuesMap.get("amount"), "Expected and Actual Value of amount doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String systemAmountActual = ShowHelper.getValueOfField("systemamount", showResponse);
            if (!systemAmountActual.equals(expectedAmountValuesMap.get("systemAmount"))) {
                csAssert.assertEquals(systemAmountActual,expectedAmountValuesMap.get("systemAmount"), "Expected and Actual Value of systemAmount doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }
            String discrepancyAmountActual = ShowHelper.getValueOfField("discrepancyamount", showResponse);
            if (!discrepancyAmountActual.equals(expectedAmountValuesMap.get("discrepancyAmount"))) {
                csAssert.assertEquals(discrepancyAmountActual,expectedAmountValuesMap.get("discrepancyAmount"), "Expected and Actual Value of discrepancyAmount doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String totalActual = ShowHelper.getValueOfField("total", showResponse);
            if (!totalActual.equals(expectedAmountValuesMap.get("total"))) {
                csAssert.assertEquals(totalActual,expectedAmountValuesMap.get("total"), "Expected and Actual Value of totalActual doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String systemTotalActual = ShowHelper.getValueOfField("systemtotal", showResponse);
            if (!systemTotalActual.equals(expectedAmountValuesMap.get("systemTotal"))) {
                csAssert.assertEquals(systemTotalActual,expectedAmountValuesMap.get("systemTotal"), "Expected and Actual Value of systemTotal doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

            String discrepancyTotalActual = ShowHelper.getValueOfField("discrepancytotal", showResponse);
            if (!discrepancyTotalActual.equals(expectedAmountValuesMap.get("discrepancyTotal"))) {
                csAssert.assertEquals(discrepancyTotalActual,expectedAmountValuesMap.get("discrepancyTotal"), "Expected and Actual Value of discrepancyTotal doesn't match for invoice line item " + invoiceLineItemId + " for the flow " + flowToTest);
                validationStatus = false;
            }

        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Amount values on invoice line item " + invoiceLineItemId + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;

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

    private String updateSupplierAmount(String createPayload,int supplierTotal){

        String updatedCreatePayload = null;
        try {
            JSONObject updatedCreatePayloadJson = new JSONObject(createPayload);

            updatedCreatePayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("total").put("values",supplierTotal);

            updatedCreatePayload = updatedCreatePayloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while creating payload for create during supplier amount updation" + e.getMessage());
        }

        return  updatedCreatePayload;
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

    private void validateMemoScenarios(String flowToTest,int serviceDataId,int consumptionId,int invoiceId,int invoiceLineItemId,CustomAssert csAssert) {

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try {

//            Memo 1 scenario LI 1 100 Disc -> Adjustment LI 2 After Validation No Memo Found
//            Memo 2 LI 1 100 Disc Calculate memo - > Adjustment LI 2  Supplier amount 100 No Discrepancy ->
//            Memo 3 LI 1 100 Disc Calculate memo - > Adjustment LI 2  Supplier amount 100 No Discrepancy -> Adjustment LI 3 No memo Found
//            Memo 4 LI 1 100 Disc Calculate memo - > Adjustment LI 2  Supplier amount 70 Total Discrepancy of 30- > Li 2 pe calculate memo ->  Adjustment LI 3 Supplier amount No Discrepancy

            if (flowToTest.equals("memo flow 1") || flowToTest.equals("memo flow 2") ||
                    flowToTest.equals("memo flow 3") || flowToTest.equals("memo flow 4")) {


                if (flowToTest.equals("memo flow 2") || flowToTest.equals("memo flow 3")
                        || flowToTest.equals("memo flow 4")) {

                    workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId, approveInvoice, csAssert);
                    workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, approveLineItem, csAssert);
                    workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, calculateMemo, csAssert);

                }

                String invoiceValidationStatus = "";
                //String adjustmentType = "1090";//"1314";
                String adjustmentType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adj line item type");
                if (adjustmentType == null) {
                    adjustmentType = "1090";   // For Auto Office Env
                }

                InvoiceHelper invoiceHelper = new InvoiceHelper();
                Map<String, Map<String, String>> billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, csAssert);

                if (billingRecords.size() != 2) {
                    csAssert.assertTrue(false, "Memo Billing Record Not Generated for the flow " + flowToTest);
                } else {
                    for (Map.Entry<String, Map<String, String>> entry : billingRecords.entrySet()) {

                        String billingRecord = entry.getKey();

                        if (billingRecord.contains("_1")) {

                            Map<String, String> billingRecordDetails = entry.getValue();
                            invoiceHelper.validateBillingDataListingPageData(serviceDataId, consumptionId, "", "-1", billingRecordDetails,
                                    false, "After First Memo Creation For Adjustment Line Item 1", csAssert);

                            String memoReason = billingRecordDetails.get("memoreason");

                            if (!memoReason.equals("Discrepancy")) {
                                csAssert.assertTrue(false, "Discrepancy memo not generated for the flow " + flowToTest);
                            }

                        }

                    }

                }

                String suppAmt = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "supplier total");

                int supplierTotal = 36;

                if (suppAmt != null) {
                    supplierTotal = Integer.parseInt(suppAmt);
                }

                if (flowToTest.equals("memo flow 1")) {
                    invoiceValidationStatus = "No Memo Found";
                } else if (flowToTest.equals("memo flow 2") || flowToTest.equals("memo flow 3")) {
                    invoiceValidationStatus = "No Discrepancy";
                } else if (flowToTest.equals("memo flow 4")) {
                    invoiceValidationStatus = "Total Discrepancy";
                }

                int adjustmentLI1 = createAdjustmentLi(invoiceLineItemId, adjustmentType, supplierTotal);

                if (adjustmentLI1 == -1) {
                    csAssert.assertTrue(false, "Adjustment LineItem not created for the flow " + flowToTest);
                    return;
                }

                billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, csAssert);

                //Checking linkage of adjustment Line Item with memo billing record
                for (Map.Entry<String, Map<String, String>> entry : billingRecords.entrySet()) {

                    String billingRecord = entry.getKey();

                    if (billingRecord.contains("_1")) {

                        Map<String, String> billingRecordDetails = entry.getValue();
                        invoiceHelper.validateBillingDataListingPageData(serviceDataId, consumptionId, String.valueOf(invoiceId), String.valueOf(adjustmentLI1), billingRecordDetails,
                                false, "Checking linkage of adjustment Line Item with memo billing record", csAssert);

                        String memoReason = billingRecordDetails.get("memoreason");

                        if (!memoReason.equals("Discrepancy")) {
                            csAssert.assertTrue(false, "Discrepancy memo not generated for the flow " + flowToTest + " After Adjustment Line Item Creation");
                        }

                    }
                }
                Boolean lineItemValidatedSuccessfully = InvoiceHelper.verifyInvoiceLineItemValidationStatus(invoiceValidationStatus, adjustmentLI1, csAssert);

                if (!lineItemValidatedSuccessfully) {
                    csAssert.assertTrue(false, "Adjustment Line Item Validation Status validated unsuccessfully for the flow " + flowToTest);
                }
                int adjustmentLI2 = -1;
                if (flowToTest.equals("memo flow 3")) {

                    invoiceValidationStatus = "No Memo Found";

                    adjustmentLI2 = createAdjustmentLi(invoiceLineItemId, adjustmentType, supplierTotal);

                    lineItemValidatedSuccessfully = InvoiceHelper.verifyInvoiceLineItemValidationStatus(invoiceValidationStatus, adjustmentLI2, csAssert);

                    if (!lineItemValidatedSuccessfully) {
                        csAssert.assertTrue(false, "Adjustment Line Item Validation Status after new LI validated unsuccessfully for the flow " + flowToTest);
                    }
                }

                if (flowToTest.equals("memo flow 4")) {

                    workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, adjustmentLI1, approveLineItem, csAssert);
                    workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, adjustmentLI1, calculateMemo, csAssert);
                    adjustmentLI2 = createAdjustmentLi(adjustmentLI1, adjustmentType, supplierTotal);
                    supplierTotal = 1688;

                    //Memo billing record should be generated for adjustment type line item
                    billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, csAssert);

                    if (billingRecords.size() != 3) {
                        csAssert.assertTrue(false, "Adjustmemt Memo Billing Record Not Generated after calculating memo for the adjustment line item flow " + flowToTest + " ");
                    } else {

                        invoiceValidationStatus = "No Discrepancy";

                        lineItemValidatedSuccessfully = InvoiceHelper.verifyInvoiceLineItemValidationStatus(invoiceValidationStatus, adjustmentLI2, csAssert);

                        if (!lineItemValidatedSuccessfully) {
                            csAssert.assertTrue(false, "Adjustment Line Item Validation Status after new LI validated unsuccessfully for the flow " + flowToTest);
                        }

                        //Checking linkage of adjustment Line Item with memo billing record
                        for (Map.Entry<String, Map<String, String>> entry : billingRecords.entrySet()) {

                            String billingRecord = entry.getKey();

                            if (billingRecord.contains("_1")) {

                                Map<String, String> billingRecordDetails = entry.getValue();
                                invoiceHelper.validateBillingDataListingPageData(serviceDataId, consumptionId, String.valueOf(invoiceId), String.valueOf(adjustmentLI1), billingRecordDetails,
                                        false, "After Second Memo Creation For Adjustment Line Item 1", csAssert);

                                String memoReason = billingRecordDetails.get("memoreason");

                                if (!memoReason.equals("Discrepancy")) {
                                    csAssert.assertTrue(false, "Discrepancy memo not generated for the flow " + flowToTest + " After Adjustment Line Item Creation");
                                }


                            } else if (billingRecord.contains("_2")) {

                                Map<String, String> billingRecordDetails = entry.getValue();
                                invoiceHelper.validateBillingDataListingPageData(serviceDataId, consumptionId, String.valueOf(invoiceId), String.valueOf(adjustmentLI2), billingRecordDetails,
                                        false, "After Second Memo Creation For Adjustment Line Item 2", csAssert);

                                String memoReason = billingRecordDetails.get("memoreason");

                                if (!memoReason.equals("Discrepancy")) {
                                    csAssert.assertTrue(false, "Discrepancy memo not generated for the flow " + flowToTest + " After Adjustment Line Item Creation");
                                }

                            }
                        }
                    }

                }

            } else {
                return;
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating memo flow " + flowToTest);
        }

    }

    private int createAdjustmentLi(int invoiceLi,String adjustmentType,int supplierTotal){

        int adjustmentLI = -1;

        try {
            String createPayload = getCreatePayloadFromClone(invoiceLineItemEntity, invoiceLi);

            if (createPayload != null) {

                createPayload = updateCreatePayloadForDifferentLineItemType(createPayload, "", adjustmentType);
                createPayload = updateSupplierAmount(createPayload, supplierTotal);

                Create create = new Create();
                create.hitCreate(invoiceLineItemEntity, createPayload);
                String createResponse = create.getCreateJsonStr();

                //adjustment line item
                adjustmentLI = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);


            }
        }catch (Exception e){
            logger.error("Exception while creating adjustment LI");
        }
        return adjustmentLI;
    }

    private BigDecimal calcSysAmountARC(int serviceDataId,int consumptionId,CustomAssert customAssert) {

        BigDecimal systemAmt = new BigDecimal(0.0);

        try {

            TabListData tabListData = new TabListData();
            String chargesTabResponse = tabListData.hitTabListDataV2(chargesTabId, serviceDataEntityTypeId, serviceDataId);

            ListRendererListData listRendererListData = new ListRendererListData();

            Map<String, String> chargesColMap = listRendererListData.getListColumnNameValueMap(chargesTabResponse);

            BigDecimal baseVolume = new BigDecimal(chargesColMap.get("volume"));
            BigDecimal rate = new BigDecimal(chargesColMap.get("unitrate"));

            String finalConsumption = ShowHelper.getValueOfField(consumptionEntityTypeId, consumptionId, "finalconsumption");

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
//                                customAssert.assertTrue(false, "Exception while parsing double value for lower level");
                                logger.debug("Exception while parsing double value for lower level");
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
//                                customAssert.assertTrue(false, "Exception while parsing double value for upper level");
                                logger.debug("Exception while parsing double value for upper level");
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

    private BigDecimal calcSysAmountRRC(int serviceDataId,int consumptionId,CustomAssert customAssert) {

        BigDecimal systemAmt = new BigDecimal(0.0);

        try {

            TabListData tabListData = new TabListData();
            String chargesTabResponse = tabListData.hitTabListDataV2(chargesTabId, serviceDataEntityTypeId, serviceDataId);

            ListRendererListData listRendererListData = new ListRendererListData();

            Map<String, String> chargesColMap = listRendererListData.getListColumnNameValueMap(chargesTabResponse);

            BigDecimal baseVolume = new BigDecimal(chargesColMap.get("volume"));
            BigDecimal rate = new BigDecimal(chargesColMap.get("unitrate"));

            String finalConsumption = ShowHelper.getValueOfField(consumptionEntityTypeId, consumptionId, "finalconsumption");

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

            NavigableMap<BigDecimal, HashMap<String, String>> arcMapDesc = arcMap.descendingMap();

            HashMap<String, String> values;
            //1 means Greater than base volume || 0 means equal || -1 means less than
            if (finalConsumptionBigDec.compareTo(baseVolume) == -1) {

                BigDecimal remainingAmount = finalConsumptionBigDec;

                systemAmt = baseVolume.multiply(rate);

                remainingAmount = baseVolume.subtract(finalConsumptionBigDec);

                for (Map.Entry<BigDecimal, HashMap<String, String>> entry : arcMapDesc.entrySet()) {

                    if (remainingAmount.equals(new BigDecimal(0.0))) {
                        break;
                    }
//                    BigDecimal band = entry.getKey();

                    values = entry.getValue();

                    String lowerlevel = values.get("lowerlevel");
                    String upperlevel = values.get("upperlevel");

                    if (upperlevel == null) {
                        System.out.println("");
                    } else {
                        BigDecimal interval = new BigDecimal(upperlevel).subtract(new BigDecimal(lowerlevel));

                        BigDecimal newRemainingAmount = remainingAmount.subtract(interval);

                        if (newRemainingAmount.equals(0)) {
                            systemAmt = systemAmt.subtract(new BigDecimal(values.get("rate")).multiply(newRemainingAmount));
                            remainingAmount = new BigDecimal(0.0);

                        } else if (newRemainingAmount.compareTo(new BigDecimal(0)) == -1) {

                            systemAmt = systemAmt.subtract(new BigDecimal(values.get("rate")).multiply(remainingAmount));
                            remainingAmount = new BigDecimal(0.0);

                        }else if (newRemainingAmount.compareTo(new BigDecimal(0)) == 1) {

                            systemAmt = systemAmt.subtract(new BigDecimal(values.get("rate")).multiply(interval));
                            remainingAmount = newRemainingAmount;
                        }
                    }
                }
            } else if (finalConsumptionBigDec.compareTo(baseVolume) == 0) {

                systemAmt = systemAmt.add(rate.multiply(baseVolume));

            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while calculating system amount RRC");
        }
        return systemAmt;
    }


    private BigDecimal calcSysAmountARCBaseLIYes(int serviceDataId,int consumptionId,String liType,CustomAssert customAssert) {

        BigDecimal systemAmt = new BigDecimal(0.0);

        try {

            TabListData tabListData = new TabListData();
            String chargesTabResponse = tabListData.hitTabListDataV2(chargesTabId, serviceDataEntityTypeId, serviceDataId);

            ListRendererListData listRendererListData = new ListRendererListData();

            Map<String, String> chargesColMap = listRendererListData.getListColumnNameValueMap(chargesTabResponse);

            BigDecimal baseVolume = new BigDecimal(chargesColMap.get("volume"));
            BigDecimal rate = new BigDecimal(chargesColMap.get("unitrate"));

            String finalConsumption = ShowHelper.getValueOfField(consumptionEntityTypeId, consumptionId, "finalconsumption");

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

                if(liType.equalsIgnoreCase("base charges")){
                    systemAmt = baseVolume.multiply(rate);
                    return systemAmt;
                }else {

                }

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

    private BigDecimal calcSysAmountVolPricing(int serviceDataId,int consumptionId,String flowToTest,CustomAssert customAssert) {

        BigDecimal systemAmt = new BigDecimal(0.0);

        try {

            TabListData tabListData = new TabListData();

            String finalConsumption = ShowHelper.getValueOfField(consumptionEntityTypeId, consumptionId, "finalconsumption");

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
            BigDecimal upperLevel;
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
            }else {
                customAssert.assertTrue(false,"ARC / RRC Tab Response is an invalid json for the flow " + flowToTest);
            }

            for (Map.Entry<BigDecimal, HashMap<String, String>> entry : arcMap.entrySet()) {

                BigDecimal upperVol = new BigDecimal(entry.getValue().get("upperlevel"));
                BigDecimal lowerVol = new BigDecimal(entry.getValue().get("lowerlevel"));

                if((finalConsumptionBigDec.compareTo(upperVol) == 0) || (finalConsumptionBigDec.compareTo(upperVol) == -1) && finalConsumptionBigDec.compareTo(lowerVol) == 1){

                    BigDecimal rate = new BigDecimal(entry.getValue().get("rate"));
                    systemAmt = rate.multiply(finalConsumptionBigDec);
                    break;
                }else if((finalConsumptionBigDec.compareTo(upperVol) == -1) && finalConsumptionBigDec.compareTo(lowerVol) == -1){
                    systemAmt = new BigDecimal(0);
                }

            }


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while calculating system amount Vol Pricing");
        }
        return systemAmt;
    }

    private boolean valAuditLogAfterValidation(int lineItemId,HashMap<String,String> expectedValues,String flowToTest,CustomAssert customAssert) {

        Boolean validationStatus = true;

        try {

            String systemUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "audit log system user");
            String requested_by_user = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "audit log requested by user");
            String updationStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "audit log action name after validation");

            if(updationStatus == null){
                updationStatus = "Validated";
            }
            AuditLog auditLog = new AuditLog();
            Map<String, String> auditLogMap = auditLog.getAuditLogMap(invoiceLineItemEntityTypeId, lineItemId, customAssert);

            for (Map.Entry<String, String> auditLogM : auditLogMap.entrySet()) {

                String colName = auditLogM.getKey();
                String colValue = auditLogM.getValue();

                if (colName.equalsIgnoreCase("action_name")) {
                    if (!colValue.equalsIgnoreCase(updationStatus)) {
                        customAssert.assertTrue(false, "After line item validation Action Name \"Updated\" Expected while actual value " + colValue + " for the flow " + flowToTest);
                        validationStatus = false;
                    }
                }

                if (colName.equalsIgnoreCase("requested_by")) {
                    if (!colValue.equalsIgnoreCase(requested_by_user)) {
                        customAssert.assertTrue(false, "After line item validation value in requested by " + requested_by_user + " Expected while actual value " + colValue + " for the flow " + flowToTest);
                        validationStatus = false;
                    }
                }

                if (colName.equalsIgnoreCase("completed_by")) {
                    if (!colValue.equalsIgnoreCase(systemUser)) {
                        customAssert.assertTrue(false, "After line item validation value in completed_by " + systemUser + " Expected while actual value " + colValue + " for the flow " + flowToTest);
                        validationStatus = false;
                    }
                }

                if (colName.equalsIgnoreCase("audit_log_date_created") || colName.equalsIgnoreCase("audit_log_user_date")) {
                    if (colValue == null) {
                        customAssert.assertTrue(false, "After line item validation value in " + colName + " is null" + " for the flow " + flowToTest);
                        validationStatus = false;
                    } else if (colValue.equalsIgnoreCase("")) {
                        customAssert.assertTrue(false, "After line item validation value in " + colName + " is blank" + " for the flow " + flowToTest);
                        validationStatus = false;
                    }
                }

                if (colName.equalsIgnoreCase("history")) {

                    FieldHistory fieldHistory = new FieldHistory();
                    String fieldHistoryResponse = fieldHistory.hitFieldHistory(colValue);

                    if (!JSONUtility.validjson(fieldHistoryResponse)) {
                        customAssert.assertTrue(false, "View History Response is an invalid json" + " for the flow " + flowToTest);
                        validationStatus = false;
                    } else {
                        JSONObject fieldHistResp = new JSONObject(fieldHistoryResponse);
                        JSONArray valueArray = fieldHistResp.getJSONArray("value");

                        if (valueArray.length() == 0) {
                            customAssert.assertTrue(false, "View History does not have any data while it is supposed to contain data" + " for the flow " + flowToTest);
                            validationStatus = false;
                        } else {
                            for (int j = 0; j < valueArray.length(); j++) {

                                String propertyName = valueArray.getJSONObject(j).get("property").toString();
                                String newValue = valueArray.getJSONObject(j).get("newValue").toString();

                                String expectedValue = expectedValues.get(propertyName);if(expectedValue == null){continue;}
                                if (newValue == null) {
                                    customAssert.assertTrue(false, "On clicking view history button the property name " + propertyName + " value is null" + " for the flow " + flowToTest);
                                    validationStatus = false;

                                } else if (!newValue.contains(expectedValue)) {
                                    customAssert.assertTrue(false, "On clicking view history button the property name " + propertyName + " not matched with the expected property " + " for the flow " + flowToTest + " Expected " + expectedValue + " Actual " + newValue);
                                    validationStatus = false;
                                }

                            }
                        }

                    }
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating audit log after Line ItemValidation" + " for the flow " + flowToTest);
            validationStatus = false;
        }
        return validationStatus;
    }

    private HashMap<String,String> expectedValuesAuditLog(String flowToTest){

        HashMap<String,String> expectedValuesAuditLog = new HashMap<>();
        try{

            String[] auditLogValues = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,flowToTest,"audit log values").split(",");

            for (int j = 0; j < auditLogValues.length; j++) {

                String[] rateCardColNameValue = auditLogValues[j].split("->");
                try {
                    expectedValuesAuditLog.put(rateCardColNameValue[0], rateCardColNameValue[1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    expectedValuesAuditLog.put(rateCardColNameValue[0], "");
                }
            }

        }catch (Exception e){
            logger.error("Exception while fetching expected Values On Audit Log");
        }

        return expectedValuesAuditLog;
    }

    private void validateBillingDataScenarios(String flowToTest,int serviceDataId,int consumptionId,String invoiceId,
                                              String invoiceLineItemId,CustomAssert customAssert){

        InvoiceHelper invoiceHelper = new InvoiceHelper();
        try {
            String billingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFilePath");
            String billingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFileName");


            if(billingScenariosList.contains(flowToTest)) {

                Map<String, Map<String, String>> billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);

                if(billingRecords.size() == 0){
                    logger.error("Billing Records not generated for the flow " + flowToTest);
                    customAssert.assertTrue(false,"Billing Records not generated for the flow " + flowToTest);
                    return;
                }
                invoiceHelper.validateBillingDataListingPageData(serviceDataId, consumptionId, String.valueOf(invoiceId), String.valueOf(invoiceLineItemId),
                                                                billingConfigFilePath,billingConfigFileName,
                                                                billingRecords,false,flowToTest, customAssert);


            }
        }catch (Exception e){
            logger.error("Exception while validating billing data scenarios for the flow " + flowToTest);
            customAssert.assertTrue(false,"Exception while validating billing data scenarios for the flow " + flowToTest);
        }
    }

    private Boolean validateSystemAmt(String flowToTest,int serviceDataId,int consumptionId,int invLineItem,CustomAssert customAssert) {
        Boolean validationStatus = true;

        try {
            BigDecimal systemAmtExp = new BigDecimal(0.0);

            if (flowToTest.equals("vol pricing flow volume discrepancy") ||
                    flowToTest.equals("vol pricing flow total discrepancy") ||
                    flowToTest.equals("vol pricing flow amount discrepancy") ||
                    flowToTest.equals("vol pricing flow system values calc 1") ||
                    flowToTest.equals("vol pricing flow system values calc 2") ||
                    flowToTest.equals("arc flow 10") ||
                    flowToTest.equals("arc flow 11") ||
                    flowToTest.equals("arc flow 12") ||
                    flowToTest.equals("fixed fee flow 2")
            ) {
                logger.info("Validating System Amount Calculation for the flow " + flowToTest);
                if (flowToTest.contains("arc")) {

                    String baseSpecific = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "basespecific");
                    String lineItemType = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invLineItem, "lineitemtype");

                    if (baseSpecific.equals("true")) {
                        systemAmtExp = calcSysAmountARCBaseLIYes(serviceDataId, consumptionId, lineItemType, customAssert);
                    } else {

                        TabListData tabListData = new TabListData();
                        int arcRrcTabId = 311;
                        String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"enddate\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";

                        String tabListResponse = tabListData.hitTabListDataV2(arcRrcTabId, serviceDataEntityTypeId, serviceDataId, payload);
                        if (JSONUtility.validjson(tabListResponse)) {

                            JSONObject tabListRespJson = new JSONObject(tabListResponse);
                            String type = "ARC";
                            try {
                                JSONArray data = tabListRespJson.getJSONArray("data");

                                if (data.length() != 0) {
                                    String typeColId = "12527";
                                    type = data.getJSONObject(0).getJSONObject(typeColId).get("value").toString();
                                }


                            } catch (Exception e) {

                            }
                            if (type.equals("ARC")) {
                                systemAmtExp = calcSysAmountARC(serviceDataId, consumptionId, customAssert);
                            } else if (type.equals("RRC")) {
                                systemAmtExp = calcSysAmountRRC(serviceDataId, consumptionId, customAssert);
                            }
                        } else {
                            customAssert.assertEquals("While Validating System amount ARC/RRC tab Response is an invalid json", "ARC/RRC tab Response should be a valid json");
                        }

                    }
                } else if (flowToTest.contains("fixed")) {
                    systemAmtExp = calcSysAmountFixFee(serviceDataId, flowToTest, customAssert);
                } else if (flowToTest.contains("vol")) {
                    systemAmtExp = calcSysAmountVolPricing(serviceDataId, consumptionId, flowToTest, customAssert);
                }

                String systemAmountActual = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invLineItem, "systemamount");

                if (systemAmountActual == null) {
                    logger.error("System Amount Actual is null for the flow " + flowToTest);
                    customAssert.assertEquals("null", systemAmtExp);
                    validationStatus = false;
                } else if (systemAmountActual.equalsIgnoreCase("null")) {
                    logger.error("System Amount Actual is null for the flow " + flowToTest);
                    customAssert.assertEquals("null", systemAmtExp);
                    validationStatus = false;
                } else if (!systemAmountActual.equals(String.valueOf(Double.parseDouble(systemAmtExp.toString())))) {
                    logger.error("System Amount Actual is not matched with the Expected for the flow " + flowToTest);
                    customAssert.assertEquals(systemAmountActual, systemAmtExp, "System Amount Actual is not matched with the Expected for the flow " + flowToTest);
                    validationStatus = false;
                } else {
                    logger.info("System Amount Actual is matched with the Expected for the flow " + flowToTest);
                }
            } else {
                logger.info("Skipping the system amount calculation for the flow " + flowToTest);
            }

        } catch (Exception e) {
            logger.error("Exception while validating system amount for the flow " + flowToTest);
            validationStatus = false;
        }

        return validationStatus;
    }

    private BigDecimal calcSysAmountFixFee(int serviceDataId,String flowToTest,CustomAssert customAssert) {

        BigDecimal systemAmt = new BigDecimal(0.0);

        try {

            TabListData tabListData = new TabListData();
            String chargesTabResponse = tabListData.hitTabListDataV2(chargesTabId, serviceDataEntityTypeId, serviceDataId);

            ListRendererListData listRendererListData = new ListRendererListData();

            Map<String, String> chargesColMap = listRendererListData.getListColumnNameValueMap(chargesTabResponse);

            BigDecimal baseVolume = new BigDecimal(chargesColMap.get("volume"));
            BigDecimal rate = new BigDecimal(chargesColMap.get("unitrate"));

            systemAmt = baseVolume.multiply(rate);
        }catch (Exception e){
            customAssert.assertEquals("Exception while calculating system amount for the flow " + flowToTest,"Exception should not occur");
        }

        return systemAmt;
    }

    private Boolean validateListPageValues(String flowToTest,int invoiceId,CustomAssert customAssert){
        Boolean validationStatus = true;
        try{
            int invoiceListId = 10;
            logger.info("Validating Invoice Record having Id {} on Listing Page for Flow [{}].", invoiceId, flowToTest);
            ListRendererListData listDataObj = new ListRendererListData();
            Map<String, String> params = new HashMap<>();
            String contractId = flowIdContractIdMap.get(flowToTest);

            Show show = new Show();
            show.hitShowVersion2(invoiceEntityTypeId,invoiceId);

            String showResponse = show.getShowJsonStr();

            String parentId = ShowHelper.getValueOfField("parententityid",showResponse);
            params.put("contractId", parentId);

            int entityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";

            logger.info("Hitting List Data API for invoices Flow [{}]", flowToTest);
            listDataObj.hitListRendererListData(invoiceListId, payload, params);
            String listDataJsonStr = listDataObj.getListDataJsonStr();
            listDataObj.setListData(listDataJsonStr);

            List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
            Map<String,String> listRecord = new HashMap<>();

            if (listData.size() > 0) {
                int columnId = listDataObj.getColumnIdFromColumnName("id");
                Boolean recordFound = false;

                for (Map<Integer, Map<String, String>> oneData : listData) {
                    if (oneData.get(columnId).get("valueId").trim().equalsIgnoreCase(String.valueOf(invoiceId))) {
                        listRecord = oneData.get(columnId);

                        for (Map.Entry<Integer,Map<String,String>> listRec : oneData.entrySet()) {

                            listRecord.put(listRec.getValue().get("columnName"),listRec.getValue().get("value"));

                        }

                        recordFound = true;
                        break;
                    }
                }

                if (!recordFound) {
                    logger.error("Couldn't find newly created Invoice having Id {} on Listing Page for Flow [{}]", invoiceId, flowToTest);
                    customAssert.assertTrue(false, "Couldn't find newly created Invoice having Id " + invoiceId +
                            " on Listing Page for Flow [" + flowToTest + "]");
                }else {

                    String[] listColumnsToVal = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,"list columns to validate").split(",");
                    if(listColumnsToVal == null){
                        logger.debug("No Listing Columns To validate");
                        return validationStatus;
                    }

                    for(String listCol:listColumnsToVal){

                        try {
                            String listColValue = listRecord.get(listCol);

                            String showPageEntity = ParseConfigFile.getValueFromConfigFile(listShowPageColFilePath,listShowPageColFileName,invoiceEntity,listCol).split("->")[0];
                            String showPageField = ParseConfigFile.getValueFromConfigFile(listShowPageColFilePath,listShowPageColFileName,invoiceEntity,listCol).split("->")[1];
                            int showPageEntityTypeId = ConfigureConstantFields.getEntityIdByName(showPageEntity);

                            Map<String, String> fieldTypeMap = ShowHelper.getShowPageObjectTypesMap();

                            String fieldType = fieldTypeMap.get(showPageField);

                            if(fieldType == null){
                                logger.error("Field Type is null for list column " + listCol);
                                continue;
                            }
                            show.hitShowVersion2(showPageEntityTypeId,invoiceId);
                            showResponse = show.getShowJsonStr();

                            ListDataHelper.validateListingRecordsDataOnShowPage(showResponse, showPageEntityTypeId, showPageEntity,
                                    showPageField,listColValue,listCol,fieldType,invoiceId,customAssert);

                        }catch (Exception e){
                            logger.error("Exception while getting configuration for list column " + listCol);
                            continue;
                        }
                    }

                }
            }else {
                customAssert.assertTrue(false,"Invoice Listing page has zero records for the flow " + flowToTest);
            }



        }catch (Exception e){
            logger.error("Exception while validating list page values " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating list page values");
            validationStatus = false;
        }
        return validationStatus;
    }

//    Service data field is appearing blank on line item
    //SIR-14832
    //SIR-15084
    private void validate_SIR_14832(int invoiceLineItem,String flowToTest,CustomAssert customAssert){

        try{

            Show show = new Show();
            show.hitShowVersion2(invoiceLineItemEntityTypeId,invoiceLineItem);
            String showResponseLineItem = show.getShowJsonStr();

            int serviceDataId = Integer.parseInt(flowIdServiceDataIdMap.get(flowToTest));

            show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
            String showResponseSD = show.getShowJsonStr();

            String name = ShowHelper.getValueOfField("name",showResponseSD);
            String supplierId = ShowHelper.getValueOfField("serviceidsupplier",showResponseSD);

            String expSDNameOnLineItem = name + " (" + supplierId + ")";
            String serviceDataIdOnLineItem = ShowHelper.getValueOfField("serviceidsupplierlineitem",showResponseLineItem);

            if(serviceDataIdOnLineItem == null || serviceDataIdOnLineItem.equals("null")){
                customAssert.assertTrue(false,"Service Data Value on line item is null");
            }else if(!expSDNameOnLineItem.equals(serviceDataIdOnLineItem)){
                customAssert.assertEquals(serviceDataIdOnLineItem,expSDNameOnLineItem,"Expected and Actual value of service data mismatched on line item for the flow " + flowToTest + " Expected Value " + expSDNameOnLineItem + " Actual Value " + serviceDataIdOnLineItem);
            }

            EntityOperationsHelper.deleteEntityRecord(serviceDataEntity,serviceDataId);

            show.hitShowVersion2(invoiceLineItemEntityTypeId,invoiceLineItem);
            showResponseLineItem = show.getShowJsonStr();

            serviceDataIdOnLineItem = ShowHelper.getValueOfField("serviceidsupplierlineitem",showResponseLineItem);

            if(serviceDataIdOnLineItem == null || serviceDataIdOnLineItem.equals("null")){
                customAssert.assertTrue(false,"Service Data Value on line item is null after Service Data Deletion");
            }else if(!expSDNameOnLineItem.equals(serviceDataIdOnLineItem)){
                customAssert.assertEquals(serviceDataIdOnLineItem,expSDNameOnLineItem,"Expected and Actual value of service data mismatched on line item for the flow after Deletion" + flowToTest + " Expected Value " + expSDNameOnLineItem + " Actual Value " + serviceDataIdOnLineItem);
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating SIR_14832 for the flow " + flowToTest);
        }
    }

}
