
package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.invoice.ForecastUploadHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.test.invoice.flow.TestInvoiceFlow;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;
import java.util.*;
import java.util.regex.Pattern;

public class TestInvoiceLineItemRejectValidation extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceFlow.class);
    private String configFilePath;
    private String configFileName;
    private String flowsConfigFilePath;
    private String flowsConfigFileName;
    private boolean killAllSchedulerTasks = false;
    private String pricingTemplateFilePath;
    private String forecastTemplateFilePath;
    private boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
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
    private int serviceDataEntityTypeId = -1;
    private String serviceDataEntitySectionUrlName;
    private String consumptionEntitySectionUrlName;
    //	private Show show;
    private String approveAction = "approve";
    //	private Edit edit;
    private String contractEntity = "contracts";
    private String serviceDataEntity = "service data";
    private String consumptionEntity = "consumptions";
    private String invoiceEntity = "invoices";
    private String invoiceLineItemEntity = "invoice line item";

    private Map<Integer,String> deleteEntityMap = new HashMap<>();

    @BeforeClass
    public void beforeClass() throws Exception {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceMultipleLineItemValidationPath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceMultipleLineItemValidationName");
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

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killallschedulertasks");
        if (temp != null && temp.trim().equalsIgnoreCase("true"))
            killAllSchedulerTasks = true;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            failTestIfJobNotCompletedWithinSchedulerTimeOut = false;


        String entityIdMappingFileName;
        String baseFilePath;

        // for publishing of service data
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));
        consumptionEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "url_name");

        testCasesMap = getTestCasesMapping();

        deleteEntityMap = new HashMap<>();
    }

    @BeforeMethod
    public void beforeTestInvoiceLineItemRejectValidation(){
        deleteEntityMap = new HashMap<>();
    }

    @AfterMethod
    public void afterTestInvoiceLineItemRejectValidation(){
        for(Map.Entry<Integer,String> me : deleteEntityMap.entrySet()){
            deleteNewEntity(me.getValue(),me.getKey());
        }
    }

    @DataProvider()
    public Object[][] dataProviderForInvoiceRejectValidation() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest("flowstovalidate");
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider()
    public Object[][] dataProviderForAlreadyRejectInvoice() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest("flowstovalidatemultiple");
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }
    @DataProvider()
    public Object[][] dataProviderForUserRejectInvoice() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest("flowstovalidatemultiple");
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForInvoiceRejectValidation")
    public void testInvoiceFlow(String flowToTest) {
        String actionNameReject = "reject";
        String publishAction = "publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();

//	@Test()
//	public void testInvoiceFlow() throws IOException {
        //CustomAssert csAssert = new CustomAssert();
        CustomAssert csAssert = new CustomAssert();
        // Checking the arguments
        System.out.println("***************************************");
        System.out.println(flowToTest);

//		List<String> allFlowsToTest = getFlowsToTest();
//		for(String flowToTest : allFlowsToTest) {        //added by gaurav bhadani for test rail integration

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId ;
        int serviceDataId ;
        int invoiceId ;
        int invoiceLineItemId = -1;
        List<Integer> consumptionIds;

        String contractSectionName = "default";
        String serviceDataSectionName = "default";
        String invoiceSectionName = "default";
        String invoiceLineItemSectionName = "default";

        try {
            //Get Contract that will be used for Invoice Flow Validation
            synchronized (this) {
                String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname");
                if (temp != null)
                    contractSectionName = temp.trim();



                contractId= InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,contractSectionName);
                //contractId = getContractId();
                if (contractId != -1) {
                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                    deleteEntityMap.put(contractId,contractEntity);


                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath,serviceDataConfigFileName,invoiceConfigFilePath,invoiceConfigFileName,flowToTest,contractId);

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();

                    serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,serviceDataSectionName,contractId);

                    logger.info("Created Service Data Id : [{}]", serviceDataId);
                    if (serviceDataId != -1) {
                        logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                        deleteEntityMap.put(serviceDataId,serviceDataEntity);


                        //Kill All Scheduler Tasks if Flag is On.
                        if (killAllSchedulerTasks) {
                            logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                            UserTasksHelper.removeAllTasks();
                        }

                        logger.info("Hitting Fetch API.");
                        Fetch fetchObj = new Fetch();
                        fetchObj.hitFetch();
                        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                        boolean uploadPricing = true;
                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "uploadPricing");
                        if (temp != null && temp.trim().equalsIgnoreCase("false"))
                            uploadPricing = false;


                        if (uploadPricing) {
                            String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                    "pricingstemplatefilename");
//                            boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);
                            boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath,flowsConfigFileName,pricingTemplateFilePath,pricingTemplateFileName,flowToTest,serviceDataId,pricingObj,csAssert);
                            // changes for ARC RRC FLOW
                            if (pricingFile) {


                                // getting the actual service data Type
                                if (ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                        "servicedatatype") != null) {
                                    serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                            "servicedatatype");
                                }


                                if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                    pricingFile = InvoiceHelper.editPricingFileForARCRRC(flowsConfigFilePath,flowsConfigFileName,pricingTemplateFilePath,pricingTemplateFileName,flowToTest,serviceDataId);//editPricingFileForARCRRC(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);


                                }
                                // changes for ARC RRC FLOW Ends here


                                if (pricingFile) {


                                    String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                    if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {
                                        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                            //Wait for Pricing Scheduler to Complete
                                            // pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);
                                            String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest,allTaskIds);

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
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
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
                                            }

                                            // forecast flow : Start

                                            if (serviceDataType.contentEquals("forecast")) {
                                                if (!flowToTest.equals("forecast flow 6")) {
                                                    String forecastTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                            "forecasttemplatefilename");

                                                    boolean editAndUploadForecast = editAndUploadForecastSheet(forecastTemplateFilePath, forecastTemplateFileName, flowToTest, "newClient" + contractId, contractId, pricingObj);

                                                    if (!editAndUploadForecast ) {
                                                        logger.error("For Flow [{}] , edit and Upload Forecast sheet is failing so Skipping the further Part", flowToTest);
                                                        throw new SkipException("Skipping this test");
                                                    }
                                                }
                                            }
                                            // only for forecast flow : End
                                            // Consumption Part will start here only if flow is not fixed fee

                                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                                            {


                                                boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataId, serviceDataEntity, serviceDataEntitySectionUrlName);

                                                // if service data got published
                                                if (result ) {

                                                    // function to get status whether consumptions have been created or not
                                                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest,serviceDataId);//waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
                                                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                                                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                        csAssert.assertAll();
                                                        return;
                                                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        addTestResultAsSkip(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                        csAssert.assertAll();
                                                        throw new SkipException("Skipping this test");
                                                    }
                                                    consumptionIds= invoiceHelper.consumptionIds;
                                                    // after consumptions have been created successfully
                                                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                                                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                            "finalconsumptionvalues").trim().split(Pattern.quote(","));


                                                    if (!(flowToTest.equals("arc flow 6") || flowToTest.equals("forecast flow 4") ||
                                                            flowToTest.equals("vol pricing flow consumption unavailable"))) {
                                                        for (int i = 0; i < consumptionIds.size(); i++) {
                                                           // result = updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                                                            result = InvoiceHelper.updateFinalConsumption(flowToTest,consumptionIds.get(i),Double.parseDouble(finalConsumptions[i]));
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
                                                                    result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds.get(i), consumptionEntity, consumptionEntitySectionUrlName);
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
                                                //Consumption end
                                                } else {

                                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
//												csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
//														"Hence skipping validation");
                                                    csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                            "Hence skipping validation");
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                    //csAssert.assertAll();
                                                    csAssert.assertAll();
                                                    return;
                                                }
                                            }

                                        }
                                    } else {
                                        logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                                pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);
//									csAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
//											pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
                                        csAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                                pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
//									csAssert.assertAll();
                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                        csAssert.assertAll();
                                        return;
                                    }
                                } else {
                                    logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
//								csAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                    csAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");
//								csAssert.assertAll();
                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                    csAssert.assertAll();

                                    return;

                                }

                            } else {
                                logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
//							csAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
//									"Hence skipping validation");
                                csAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                                        "Hence skipping validation");
//							csAssert.assertAll();
                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                csAssert.assertAll();
                                return;
                            }
                        } else {
                            logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
                        }


                        //Get Invoice Id
                        //invoiceId = getInvoiceId(flowToTest);

                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                        if (temp != null)
                            invoiceSectionName = temp.trim();
                        invoiceId=InvoiceHelper.getInvoiceId(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceSectionName);

                        logger.info("Created Invoice Id is : [{}]", invoiceId);
                        if (invoiceId != -1) {
                            //Get Invoice Line Item Id
                            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
                            if (temp != null)
                                invoiceLineItemSectionName = temp.trim();
                            //invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataId);
                            invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,invoiceLineItemSectionName,serviceDataId);
                            logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                            deleteEntityMap.put(invoiceId,invoiceEntity);
                        } else {
                            logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                            //csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                            csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                        }

                    } else {
                        logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                        //csAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                        csAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    }
                } else {
                    logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                    //csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                }
            }
            if (invoiceLineItemId != -1) {
                deleteEntityMap.put(invoiceLineItemId,invoiceLineItemEntity);

                boolean verifyInvoiceLineItem = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath,flowsConfigFileName,flowToTest,invoiceLineItemId,csAssert);
                int newInvoiceLineItem;
                int newInvoiceLineItem2;
                if(verifyInvoiceLineItem){


                    Create create = new Create();
                    Clone clone = new Clone();
                    try {
                        String cloneResponse = clone.hitClone(invoiceLineItemEntity,invoiceLineItemId);

                        JSONObject cloneResponseJson = new JSONObject(cloneResponse);
                        JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");

                        JSONObject createEntityJson = new JSONObject();
                        JSONObject createEntityBodyJson = new JSONObject();
                        createEntityBodyJson.put("data",dataJson);
                        createEntityJson.put("body",createEntityBodyJson);

                        create.hitCreate(invoiceLineItemEntity,createEntityJson.toString());
                        String createResponse = create.getCreateJsonStr();
                        newInvoiceLineItem = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

                        if(newInvoiceLineItem == -1){
                            logger.error("Unable to create duplicate invoice line item");
                            csAssert.assertTrue(false,"Unable to create duplicate invoice line item");
                            csAssert.assertAll();
                            return;
                        }
                        else {
                            deleteEntityMap.put(newInvoiceLineItem,invoiceLineItemEntity);
                            verifyInvoiceLineItem = verifyInvoiceLineItemForDuplicate(flowsConfigFilePath,flowsConfigFileName,flowToTest,newInvoiceLineItem,csAssert);

                            if(verifyInvoiceLineItem){

                                WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                                boolean rejectWorkflowDone = workflowActionsHelper.performWorkflowAction(ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity),invoiceLineItemId,actionNameReject);

                                if(rejectWorkflowDone){
                                    verifyInvoiceLineItem = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath,flowsConfigFileName,flowToTest,newInvoiceLineItem,csAssert);

                                    if(verifyInvoiceLineItem){
                                        try {
                                            cloneResponse = clone.hitClone(invoiceLineItemEntity,newInvoiceLineItem);

                                            cloneResponseJson = new JSONObject(cloneResponse);
                                            dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");

                                            createEntityJson = new JSONObject();
                                            createEntityBodyJson = new JSONObject();
                                            createEntityBodyJson.put("data",dataJson);
                                            createEntityJson.put("body",createEntityBodyJson);

                                            create.hitCreate(invoiceLineItemEntity,createEntityJson.toString());
                                            createResponse = create.getCreateJsonStr();
                                            newInvoiceLineItem2 = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

                                            if(newInvoiceLineItem2 == -1){
                                                logger.error("Unable to create duplicate invoice line item");
                                                csAssert.assertTrue(false,"Unable to create duplicate invoice line item");
                                                csAssert.assertAll();
                                                return;
                                            }
                                            else
                                                deleteEntityMap.put(newInvoiceLineItem2,invoiceLineItemEntity);{
                                                verifyInvoiceLineItem = verifyInvoiceLineItemForDuplicate(flowsConfigFilePath,flowsConfigFileName,flowToTest,newInvoiceLineItem2,csAssert);
                                                if(verifyInvoiceLineItem) {
                                                    Edit editInvoiceLineItem = new Edit();
                                                    String editResponse = editInvoiceLineItem.hitEdit(invoiceLineItemEntity, invoiceLineItemId);
                                                    if (editResponse != null) {
                                                        try {
                                                            JSONObject editResponseJson = new JSONObject(editResponse);
                                                            JSONObject editDataJson = editResponseJson.getJSONObject("body").getJSONObject("data");
                                                            logger.info("Got the line item response to edit {}", editDataJson);

                                                            logger.info("Editing the serviceEndDate values from {} to {}", editDataJson.getJSONObject("serviceEndDate").get("values"), editDataJson.getJSONObject("serviceStartDate").get("values"));

                                                            editDataJson.getJSONObject("serviceEndDate").put("values",editDataJson.getJSONObject("serviceStartDate").get("values"));

                                                            logger.info("After editing serviceEndDate line item data is {}", editDataJson);
                                                            createEntityJson = new JSONObject();
                                                            createEntityBodyJson = new JSONObject();
                                                            createEntityBodyJson.put("data", editDataJson);
                                                            createEntityJson.put("body", createEntityBodyJson);

                                                            editResponse = editInvoiceLineItem.hitEdit(invoiceLineItemEntity,createEntityJson.toString());

                                                            logger.info("line item edit response is {}",editResponse);
                                                            if(editResponse.toLowerCase().contains("success")){
                                                                logger.info("line item edit completed successfully");
                                                                verifyInvoiceLineItemForEdit(flowToTest,invoiceLineItemId,csAssert);
                                                            }
                                                            else{
                                                                logger.error("line item edit task failed");
                                                                csAssert.assertTrue(false, "line item edit task failed");
                                                            }

                                                        } catch (Exception e) {
                                                            logger.error("Unable to edit Line Item");
                                                            csAssert.assertTrue(false, "Unable to edit Line Item "+e.toString());
                                                        }
                                                    }
                                                }
                                                else
                                                    return;
                                            }
                                        }
                                        catch (Exception e){
                                            logger.error("Exception while cloning invoice line item");
                                            csAssert.assertTrue(false,"Exception while cloning invoice line item" + e.toString());
                                        }
                                    }
                                }
                                else{
                                    logger.error("Cannot perform workflow type reject on invoice line item "+invoiceLineItemId);
                                    csAssert.assertTrue(false,"Cannot perform workflow type reject on invoice line item "+invoiceLineItemId);
                                }
                            }
                        }
                    }
                    catch (Exception e){
                        logger.error("Exception while cloning invoice line item");
                        csAssert.assertTrue(false,"Exception while cloning invoice line item" + e.toString());
                    }
                }
            } else {
                logger.error("Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                //csAssert.assertTrue(false, "Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [" +
//									flowToTest + "]");
                csAssert.assertTrue(false, "Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [" +
                        flowToTest + "]");
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
            //csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
        }
        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
        csAssert.assertAll();
    }


    @Test(dataProvider = "dataProviderForAlreadyRejectInvoice",enabled = true)
    public void testAlreadyRejectInvoiceFlow(String flowToTest)  {

        CustomAssert csAssert = new CustomAssert();
        String publishAction = "publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        // Checking the arguments

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId ;
        int serviceDataId = -1;
        int invoiceId ;
        int invoiceLineItemId ;
        List<Integer> consumptionIds;

        String contractSectionName = "default";
        String serviceDataSectionName = "default";
        String invoiceSectionNameList,invoiceLISectionNameList;

        //Map<String,String> invoiceLineItemStore = new LinkedHashMap<>();
        List<StoreInvoice> invoiceLineItemStore = new ArrayList() {};

        try {

            //setEntitySectionsForFlow(flowToTest,contractSectionName,serviceDataSectionName,invoiceSectionName,invoiceLineItemSectionName);

            //Get Contract that will be used for Invoice Flow Validation
            synchronized (this) {
                String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname");
                if (temp != null)
                    contractSectionName = temp.trim();



                contractId= InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,contractSectionName);
                //contractId = getContractId();
                if (contractId != -1) {
                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                    deleteEntityMap.put(contractId,contractEntity);


/*boolean rateCardUpdated = updateRateCard(flowToTest, contractId);
				if (rateCardUpdated) {*/

                    //updateServiceDataAndInvoiceConfig(flowToTest, contractId);
                    InvoiceHelper.updateServiceDataAndInvoiceConfigForMultipleInvoice(serviceDataConfigFilePath,serviceDataConfigFileName,invoiceConfigFilePath,invoiceConfigFileName,flowToTest,contractId,2);

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();
                    //serviceDataId = getServiceDataId(flowToTest, contractId);
                    serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,serviceDataSectionName,contractId);

                    logger.info("Created Service Data Id : [{}]", serviceDataId);


                    if (serviceDataId != -1) {
                        logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                        deleteEntityMap.put(serviceDataId,serviceDataEntity);


                        //Kill All Scheduler Tasks if Flag is On.
                        if (killAllSchedulerTasks) {
                            logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                            UserTasksHelper.removeAllTasks();
                        }

                        logger.info("Hitting Fetch API.");
                        Fetch fetchObj = new Fetch();
                        fetchObj.hitFetch();
                        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                        boolean uploadPricing = true;
                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "uploadPricing");
                        if (temp != null && temp.trim().equalsIgnoreCase("false"))
                            uploadPricing = false;


                        if (uploadPricing) {
                            String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                    "pricingstemplatefilename");
//                            boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);
                            boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath,flowsConfigFileName,pricingTemplateFilePath,pricingTemplateFileName,flowToTest,serviceDataId,pricingObj,csAssert);
                            // changes for ARC RRC FLOW
                            if (pricingFile) {


                                // getting the actual service data Type
                                if (ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                        "servicedatatype") != null) {
                                    serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                            "servicedatatype");
                                }


                                if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                    pricingFile = InvoiceHelper.editPricingFileForARCRRC(flowsConfigFilePath,flowsConfigFileName,pricingTemplateFilePath,pricingTemplateFileName,flowToTest,serviceDataId);//editPricingFileForARCRRC(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);


                                }
                                // changes for ARC RRC FLOW Ends here


                                if (pricingFile) {


                                    String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                    if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {
                                        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                            //Wait for Pricing Scheduler to Complete
                                            // pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);
                                            String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest,allTaskIds);

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
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
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
                                            }

                                            // forecast flow : Start

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
                                            // only for forecast flow : End
                                            // Consumption Part will start here only if flow is not fixed fee

                                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                                            {


                                                boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataId, serviceDataEntity, serviceDataEntitySectionUrlName);

                                                // if service data got published
                                                if (result) {

                                                    // function to get status whether consumptions have been created or not
                                                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest,serviceDataId);//waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
                                                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                                                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                        csAssert.assertAll();
                                                        return;
                                                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        addTestResultAsSkip(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                        csAssert.assertAll();
                                                        throw new SkipException("Skipping this test");
                                                    }
                                                    consumptionIds= invoiceHelper.consumptionIds;
                                                    // after consumptions have been created successfully
                                                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                                                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                            "finalconsumptionvalues").trim().split(Pattern.quote(","));


                                                    if (!(flowToTest.equals("arc flow 6") || flowToTest.equals("forecast flow 4") ||
                                                            flowToTest.equals("vol pricing flow consumption unavailable"))) {
                                                        for (int i = 0; i < consumptionIds.size(); i++) {
                                                            // result = updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                                                            result = InvoiceHelper.updateFinalConsumption(flowToTest,consumptionIds.get(i),Double.parseDouble(finalConsumptions[i]));
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
                                                                    result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds.get(i), consumptionEntity, consumptionEntitySectionUrlName);
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
                                                    //Consumption end
                                                } else {

                                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
//												csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
//														"Hence skipping validation");
                                                    csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                            "Hence skipping validation");
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                    //csAssert.assertAll();
                                                    csAssert.assertAll();
                                                    return;
                                                }
                                            }

                                        }
                                    } else {
                                        logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                                pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);
//									csAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
//											pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
                                        csAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                                pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
//									csAssert.assertAll();
                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                        csAssert.assertAll();
                                        return;
                                    }
                                } else {
                                    logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
//								csAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                    csAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");
//								csAssert.assertAll();
                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                    csAssert.assertAll();

                                    return;

                                }

                            } else {
                                logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
//							csAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
//									"Hence skipping validation");
                                csAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                                        "Hence skipping validation");
//							csAssert.assertAll();
                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                csAssert.assertAll();
                                return;
                            }
                        } else {
                            logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
                        }
                        String[] invoiceSectionNameArray = {};
                        char invoiceNumber;
                        invoiceSectionNameList = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                        if (invoiceSectionNameList != null)
                            invoiceSectionNameArray = invoiceSectionNameList.trim().split(",");

                        for(String invoiceNameTemp : invoiceSectionNameArray){

                            invoiceNumber = invoiceNameTemp.charAt(invoiceNameTemp.length()-1);
                            invoiceId=InvoiceHelper.getInvoiceIdForMultipleLI(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceNameTemp,2);

                            logger.info("Created Invoice Id is : [{}]", invoiceId);
                            if (invoiceId != -1) {
                                //Get Invoice Line Item Id
                                deleteEntityMap.put(invoiceId,invoiceEntity);
                                String[] invoiceLISectionNameArray={};
                                invoiceLISectionNameList = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
                                if (invoiceLISectionNameList != null)
                                    invoiceLISectionNameArray = invoiceLISectionNameList.trim().split(",");

                                for(String invoiceLINameTemp : invoiceLISectionNameArray){
                                    if(invoiceLINameTemp.charAt(invoiceLINameTemp.length()-2)==invoiceNumber) {

                                        invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLINameTemp, serviceDataId);
                                        logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                                        if(invoiceLineItemId!=-1){
                                            deleteEntityMap.put(invoiceLineItemId,invoiceLineItemEntity);
                                            StoreInvoice storeInvoice = new StoreInvoice();
                                            storeInvoice.invoiceId = String.valueOf(invoiceId);
                                            storeInvoice.invoiceLIId = String.valueOf(invoiceLineItemId);
                                            storeInvoice.invoiceLISectionName = invoiceLINameTemp;
                                            storeInvoice.invoiceSectionName = invoiceNameTemp;
                                            invoiceLineItemStore.add(storeInvoice);
                                        }
                                        else {
                                            logger.error("Couldn't get InvoiceLineItem Id for InvoiceLineItem Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                            //csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                            csAssert.assertTrue(false, "Couldn't get InvoiceLineItem Id for InvoiceLineItem Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                        }
                                    }
                                }

                                if(invoiceNameTemp.contains("1")){
                                    WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                                    boolean submitWorkflowDone = workflowActionsHelper.performWorkflowAction(ConfigureConstantFields.getEntityIdByName(invoiceEntity),invoiceId,"submit");
                                    if(submitWorkflowDone) {
                                        String showResponse = ShowHelper.getShowResponse(ConfigureConstantFields.getEntityIdByName(invoiceEntity), invoiceId);
                                        JSONObject jsonObj = new JSONObject(showResponse);
                                        jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values","test");
                                        showResponse = jsonObj.toString();
                                        workflowActionsHelper = new WorkflowActionsHelper();
                                        boolean rejectWorkflowDone = workflowActionsHelper.performWorkflowAction(showResponse,ConfigureConstantFields.getEntityIdByName(invoiceEntity), invoiceId, "Reject");
                                        if (!rejectWorkflowDone) {
                                            logger.error("Couldn't perform reject workflow action on invoice. Hence skipping Flow [{}]", flowToTest);
                                            csAssert.assertTrue(false, "Couldn't perform reject workflow action on invoice. Hence skipping Flow [" + flowToTest + "]");
                                        }
                                    }else {
                                        logger.error("Couldn't perform submit workflow action on invoice. Hence skipping Flow [{}]", flowToTest);
                                        csAssert.assertTrue(false, "Couldn't perform submit workflow action on invoice. Hence skipping Flow [" + flowToTest + "]");
                                    }
                                }

                            } else {
                                logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                //csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                            }

                        }

                        logger.info(invoiceLineItemStore.toString());

                    } else {
                        logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                        //csAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                        csAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    }
                } else {
                    logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                    //csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                }
            }

            for(StoreInvoice storeInvoice : invoiceLineItemStore){
                if(storeInvoice.invoiceSectionName.contains("2")){
                    boolean invoiceLineIDValidation = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath,flowsConfigFileName,flowToTest,Integer.parseInt(storeInvoice.invoiceLIId),csAssert);
                    if(!invoiceLineIDValidation){
                        logger.error("Couldn't Validate Invoice Flow [{}] [{}]", storeInvoice.invoiceLISectionName,storeInvoice.invoiceLIId);
                        csAssert.assertTrue(false, "Couldn't Validate Invoice Flow "+storeInvoice.invoiceLISectionName+" "+storeInvoice.invoiceLIId);
                    }

                }
            }


            int newLineItemDuplicateToReject = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemStore.get(0).invoiceLISectionName, serviceDataId);
            boolean invoiceLineIDValidation = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath,flowsConfigFileName,flowToTest,newLineItemDuplicateToReject,csAssert);
            if(!invoiceLineIDValidation){
                logger.error("Couldn't Validate Invoice Flow [{}] [{}]", invoiceLineItemStore.get(0).invoiceLISectionName,newLineItemDuplicateToReject);
                csAssert.assertTrue(false, "Couldn't Validate Invoice Flow "+invoiceLineItemStore.get(0).invoiceLISectionName+" "+newLineItemDuplicateToReject);
            }
            else{
                int newLineItemDuplicateToNotReject = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemStore.get(2).invoiceLISectionName, serviceDataId);
                invoiceLineIDValidation = verifyInvoiceLineItemForDuplicate(flowsConfigFilePath,flowsConfigFileName,flowToTest,newLineItemDuplicateToNotReject,csAssert);
                if(!invoiceLineIDValidation){
                    logger.error("Couldn't Validate Invoice Flow [{}] [{}]", invoiceLineItemStore.get(2).invoiceLISectionName,newLineItemDuplicateToReject);
                    csAssert.assertTrue(false, "Couldn't Validate Invoice Flow "+invoiceLineItemStore.get(2).invoiceLISectionName+" "+newLineItemDuplicateToReject);
                }
            }

        } catch (Exception e) {
            logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
            //csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
        }
        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
        csAssert.assertAll();
    }


    @Test(dataProvider = "dataProviderForUserRejectInvoice",enabled = true)
    public void testUserRejectInvoiceFlow(String flowToTest) {

        String publishAction = "publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        CustomAssert csAssert = new CustomAssert();
        // Checking the arguments

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId ;
        int serviceDataId ;
        int invoiceId ;
        int invoiceLineItemId;
        List<Integer> consumptionIds;

        String contractSectionName = "default";
        String serviceDataSectionName = "default";
        String invoiceSectionNameList,invoiceLISectionNameList;

        //Map<String,String> invoiceLineItemStore = new LinkedHashMap<>();
        List<StoreInvoice> invoiceLineItemStore = new ArrayList();

        try {
            //setEntitySectionsForFlow(flowToTest,contractSectionName,serviceDataSectionName,invoiceSectionName,invoiceLineItemSectionName);

            //Get Contract that will be used for Invoice Flow Validation
            synchronized (this) {
                String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname");
                if (temp != null)
                    contractSectionName = temp.trim();


                contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);
                //contractId = getContractId();
                if (contractId != -1) {
                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                    deleteEntityMap.put(contractId, contractEntity);


/*boolean rateCardUpdated = updateRateCard(flowToTest, contractId);
				if (rateCardUpdated) {*/

                    //updateServiceDataAndInvoiceConfig(flowToTest, contractId);
                    InvoiceHelper.updateServiceDataAndInvoiceConfigForMultipleInvoice(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, flowToTest, contractId, 2);

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();
                    //serviceDataId = getServiceDataId(flowToTest, contractId);
                    serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, contractId);

                    logger.info("Created Service Data Id : [{}]", serviceDataId);


                    if (serviceDataId != -1) {
                        logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                        deleteEntityMap.put(serviceDataId, serviceDataEntity);


                        //Kill All Scheduler Tasks if Flag is On.
                        if (killAllSchedulerTasks) {
                            logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                            UserTasksHelper.removeAllTasks();
                        }

                        logger.info("Hitting Fetch API.");
                        Fetch fetchObj = new Fetch();
                        fetchObj.hitFetch();
                        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                        boolean uploadPricing = true;
                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "uploadPricing");
                        if (temp != null && temp.trim().equalsIgnoreCase("false"))
                            uploadPricing = false;


                        if (uploadPricing) {
                            String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                    "pricingstemplatefilename");
//                            boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);
                            boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, csAssert);
                            // changes for ARC RRC FLOW
                            if (pricingFile) {


                                // getting the actual service data Type
                                if (ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                        "servicedatatype") != null) {
                                    serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                            "servicedatatype");
                                }


                                if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                    pricingFile = InvoiceHelper.editPricingFileForARCRRC(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId);//editPricingFileForARCRRC(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);


                                }
                                // changes for ARC RRC FLOW Ends here


                                if (pricingFile) {


                                    String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                    if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {
                                        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                            //Wait for Pricing Scheduler to Complete
                                            // pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);
                                            String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

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
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
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
                                                    }
                                                }
                                            }

                                            // forecast flow : Start

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
                                            // only for forecast flow : End
                                            // Consumption Part will start here only if flow is not fixed fee

                                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                                            {


                                                boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataId, serviceDataEntity, serviceDataEntitySectionUrlName);

                                                // if service data got published
                                                if (result) {

                                                    // function to get status whether consumptions have been created or not
                                                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId);//waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
                                                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                                                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                        csAssert.assertAll();
                                                        return;
                                                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                                                        addTestResultAsSkip(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                        csAssert.assertAll();
                                                        throw new SkipException("Skipping this test");
                                                    }
                                                    consumptionIds = invoiceHelper.consumptionIds;
                                                    // after consumptions have been created successfully
                                                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                                                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                            "finalconsumptionvalues").trim().split(Pattern.quote(","));


                                                    if (!(flowToTest.equals("arc flow 6") || flowToTest.equals("forecast flow 4") ||
                                                            flowToTest.equals("vol pricing flow consumption unavailable"))) {
                                                        for (int i = 0; i < consumptionIds.size(); i++) {
                                                            // result = updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                                                            result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                                                            if (!result ) {
                                                                logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
                                                                csAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                                                        "Hence skipping validation");
                                                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                                csAssert.assertAll();
                                                                return;

                                                            } else {
                                                                if (!(flowToTest.equals("arc flow 7") || flowToTest.equals("forecast flow 5")
                                                                        || flowToTest.equals("vol pricing flow consumption unapproved"))) {
                                                                    result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds.get(i), consumptionEntity, consumptionEntitySectionUrlName);
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
                                                    //Consumption end
                                                } else {

                                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
//												csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
//														"Hence skipping validation");
                                                    csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                            "Hence skipping validation");
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                    //csAssert.assertAll();
                                                    csAssert.assertAll();
                                                    return;
                                                }
                                            }

                                        }
                                    } else {
                                        logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                                pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);
//									csAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
//											pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
                                        csAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                                pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
//									csAssert.assertAll();
                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                        csAssert.assertAll();
                                        return;
                                    }
                                } else {
                                    logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
//								csAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                    csAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                            "Hence skipping validation");
//								csAssert.assertAll();
                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                    csAssert.assertAll();

                                    return;

                                }

                            } else {
                                logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
//							csAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
//									"Hence skipping validation");
                                csAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                                        "Hence skipping validation");
//							csAssert.assertAll();
                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                csAssert.assertAll();
                                return;
                            }
                        } else {
                            logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
                        }
                        String[] invoiceSectionNameArray = {};
                        char invoiceNumber;
                        invoiceSectionNameList = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                        if (invoiceSectionNameList != null)
                            invoiceSectionNameArray = invoiceSectionNameList.trim().split(",");

                        for (String invoiceNameTemp : invoiceSectionNameArray) {

                            invoiceNumber = invoiceNameTemp.charAt(invoiceNameTemp.length() - 1);
                            int s = 2;
                            if (invoiceNumber == '1')
                                s = 3;
                            invoiceId = InvoiceHelper.getInvoiceIdForMultipleLI(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceNameTemp, s);

                            logger.info("Created Invoice Id is : [{}]", invoiceId);
                            if (invoiceId != -1) {
                                //Get Invoice Line Item Id
                                deleteEntityMap.put(invoiceId, invoiceEntity);
                                String[] invoiceLISectionNameArray = {};
                                invoiceLISectionNameList = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
                                if (invoiceLISectionNameList != null)
                                    invoiceLISectionNameArray = invoiceLISectionNameList.trim().split(",");

                                for (String invoiceLINameTemp : invoiceLISectionNameArray) {
                                    if (invoiceLINameTemp.charAt(invoiceLINameTemp.length() - 2) == invoiceNumber) {

                                        invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLINameTemp, serviceDataId);
                                        logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                                        if (invoiceLineItemId != -1) {
                                            deleteEntityMap.put(invoiceLineItemId, invoiceLineItemEntity);
                                            StoreInvoice storeInvoice = new StoreInvoice();
                                            storeInvoice.invoiceId = String.valueOf(invoiceId);
                                            storeInvoice.invoiceLIId = String.valueOf(invoiceLineItemId);
                                            storeInvoice.invoiceLISectionName = invoiceLINameTemp;
                                            storeInvoice.invoiceSectionName = invoiceNameTemp;
                                            invoiceLineItemStore.add(storeInvoice);
                                        } else {
                                            logger.error("Couldn't get InvoiceLineItem Id for InvoiceLineItem Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                            //csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                            csAssert.assertTrue(false, "Couldn't get InvoiceLineItem Id for InvoiceLineItem Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                        }
                                    }
                                }

                                if (invoiceNameTemp.contains("1")) {
                                    WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                                    boolean submitWorkflowDone = workflowActionsHelper.performWorkflowAction(ConfigureConstantFields.getEntityIdByName(invoiceEntity), invoiceId, "submit");
                                    if (!submitWorkflowDone) {
                                        logger.error("Couldn't perform submit workflow action on invoice. Hence skipping Flow [{}]", flowToTest);
                                        csAssert.assertTrue(false, "Couldn't perform submit workflow action on invoice. Hence skipping Flow [" + flowToTest + "]");
                                    }
                                }

                            } else {
                                logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                                //csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                                csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                            }

                        }

                        logger.info(invoiceLineItemStore.toString());


                        for (StoreInvoice storeInvoice : invoiceLineItemStore) {
                            if (storeInvoice.invoiceSectionName.contains("2")) {
                                boolean invoiceLineIDValidation = verifyInvoiceLineItemForDuplicate(flowsConfigFilePath, flowsConfigFileName, flowToTest, Integer.parseInt(storeInvoice.invoiceLIId), csAssert);
                                if (!invoiceLineIDValidation) {
                                    logger.error("Couldn't Validate Invoice Flow [{}] [{}]", storeInvoice.invoiceLISectionName, storeInvoice.invoiceLIId);
                                    csAssert.assertTrue(false, "Couldn't Validate Invoice Flow " + storeInvoice.invoiceLISectionName + " " + storeInvoice.invoiceLIId);
                                }

                            }
                        }
                        int invoiceToBeRejected = Integer.parseInt(invoiceLineItemStore.get(0).invoiceId);
                        String showResponse = ShowHelper.getShowResponse(ConfigureConstantFields.getEntityIdByName(invoiceEntity), invoiceToBeRejected);
                        JSONObject jsonObj = new JSONObject(showResponse);
                        jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values", "test");
                        showResponse = jsonObj.toString();
                        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                        boolean rejectWorkflowDone = workflowActionsHelper.performWorkflowAction(showResponse, ConfigureConstantFields.getEntityIdByName(invoiceEntity), invoiceToBeRejected, "Reject");
                        if (rejectWorkflowDone) {

                            for (StoreInvoice storeInvoice : invoiceLineItemStore) {
                                if (storeInvoice.invoiceSectionName.contains("2")) {
                                    boolean invoiceLineIDValidation = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath, flowsConfigFileName, flowToTest, Integer.parseInt(storeInvoice.invoiceLIId), csAssert);
                                    if (!invoiceLineIDValidation) {
                                        logger.error("Couldn't Validate Invoice Flow [{}] [{}]", storeInvoice.invoiceLISectionName, storeInvoice.invoiceLIId);
                                        csAssert.assertTrue(false, "Couldn't Validate Invoice Flow " + storeInvoice.invoiceLISectionName + " " + storeInvoice.invoiceLIId);
                                    }

                                }
                            }

                        } else {
                            logger.error("Couldn't perform reject workflow action on invoice. Hence skipping Flow [{}]", flowToTest);
                            csAssert.assertTrue(false, "Couldn't perform reject workflow action on invoice. Hence skipping Flow [" + flowToTest + "]");
                        }


                        int newLineItemUnderReject = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemStore.get(0).invoiceSectionName + "3", serviceDataId);
                        boolean invoiceLineIDValidation = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath, flowsConfigFileName, flowToTest, newLineItemUnderReject, csAssert);
                        if (!invoiceLineIDValidation) {
                            logger.error("Couldn't Validate Invoice Flow [{}] [{}]", invoiceLineItemStore.get(0).invoiceLISectionName, newLineItemUnderReject);
                            csAssert.assertTrue(false, "Couldn't Validate Invoice Flow " + invoiceLineItemStore.get(0).invoiceLISectionName + " " + newLineItemUnderReject);
                        }

                    } else {
                        logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                        //csAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                        csAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    }
                } else {
                    logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                    //csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                }


            }

        } catch(Exception e){
            logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
            //csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
        }
        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
        csAssert.assertAll();

    }


    private List<String> getFlowsToTest(String flowstovalidate) {
        List<String> flowsToTest = new ArrayList<>();

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testallflows");
            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                logger.info("TestAllFlows property is set to True. Therefore all the flows are to be validated");
                flowsToTest = ParseConfigFile.getAllSectionNames(flowsConfigFilePath, flowsConfigFileName);
            } else {
                String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowstovalidate).split(Pattern.quote(","));
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


                innerValuesMap.put(Integer.parseInt(columnNumbers[j]), values[j]);
            }
            valuesMap.put(Integer.parseInt(rowNumber), innerValuesMap);

        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Forecast Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    // this will update the forecast sheet based on the deleteEntityMap created in getValuesMapForForecastSheet
    private synchronized boolean editForecastSheet(String templateFileName, String flowToTest, String clientId, Integer contractId, InvoicePricingHelper pricingObj) {

        boolean pricingFile = false;
        Map<Integer, Map<Integer, Object>> forcastValuesMap = getValuesMapForForecastSheet(flowToTest, clientId);
        String forecastSheetNameInXLSXFile = "Forecast Data";



        try {

            boolean editTemplate = pricingObj.editPricingTemplateMultipleRows(forecastTemplateFilePath, templateFileName, forecastSheetNameInXLSXFile,
                    forcastValuesMap);

            if (editTemplate ) {
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

    // this will update the forecast sheet based on the deleteEntityMap created in getValuesMapForForecastSheet and then upload that file
    private synchronized boolean editAndUploadForecastSheet(String forecastTemplateFilePath, String templateFileName, String flowToTest, String clientId, Integer contractId, InvoicePricingHelper pricingObj) {


        boolean result = true;

        boolean flag = editForecastSheet(templateFileName, flowToTest, clientId, contractId, pricingObj);

        if (flag) {


            //Kill All Scheduler Tasks if Flag is On.
            if (killAllSchedulerTasks) {
                logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                UserTasksHelper.removeAllTasks();
            }

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());


            String forecastUploadResponse = ForecastUploadHelper.uploadSheet(forecastTemplateFilePath, templateFileName, contractId);

            if (forecastUploadResponse != null && forecastUploadResponse.trim().contains("Your request has been successfully submitted")) {

                //Wait for Forecast Scheduler to Complete
                String forecastSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest,allTaskIds);

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
    private boolean isForecastCreated(int contractId) {

        int forecastTabId = 313; // hardcoded value @todo

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
    private boolean isARCRRCCreated(int serviceDataId) {

        int ARCRRCTabId = 311; // hardcoded value @todo

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
    private boolean isChargesCreated(int serviceDataId) {


        int chargesTabId = 309; // hardcoded value @todo

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

    private boolean verifyInvoiceLineItemForDuplicate(String flowsConfigFilePath,String flowsConfigFileName,String flowToTest, int invoiceLineItemId, CustomAssert csAssert) {

        boolean lineItemValidationStatus = false;
        String expectedResultForInvoiceLineItem ="Duplicate Line Item";

//        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//        workflowActionsHelper.performWorkflowAction()
        try {

            String invoiceLineItemEntity = "invoice line item";

            logger.info("Verifying Invoice Line Item for Flow [{}] having Id {}.", flowToTest, invoiceLineItemId);
            int invoiceLineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity);
            long timeSpent = 0;
            Long lineItemValidationTimeOut = 1200000L;

            while (timeSpent <= lineItemValidationTimeOut && ShowHelper.isLineItemUnderOngoingValidation(invoiceLineItemEntityTypeId, invoiceLineItemId)) {
                logger.info("Invoice Line Item having Id {} is still Under Ongoing Validation. Waiting for it to finish validation.", invoiceLineItemId);
                logger.info("time spent is : [{}]", timeSpent);
                Thread.sleep(10000);
                timeSpent += 10000;
            }

            if (timeSpent < lineItemValidationTimeOut) {
                //Line Item Validation is Completed.
                //String expectedResult = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "expectedresult");
                String actualResult = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invoiceLineItemId, "validationStatus");
                logger.info("Expected Result is [{}] and Actual Result is [{}].", expectedResultForInvoiceLineItem, actualResult);
                if(actualResult.trim().toLowerCase().contains(expectedResultForInvoiceLineItem.trim().toLowerCase())){
                    lineItemValidationStatus = true;
                }else {

                    csAssert.assertTrue(false,
                            "Invoice Line Item Validation failed as Expected Value is " + expectedResultForInvoiceLineItem + " and Actual Value is " + actualResult);
                }
            } else {
                //Line Item Validation is not yet Completed.
                logger.info("Invoice Line Item Validation couldn't be completed for Flow [{}] within TimeOut {} milliseconds", flowToTest, lineItemValidationTimeOut);

                logger.error("FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [{}]", flowToTest);
                csAssert.assertTrue(false, "FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [" +
                        flowToTest + "]");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Invoice Line Item for Flow [{}] having Id {}. {}", flowToTest, invoiceLineItemId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while verifying Invoice Line Item for Flow [" + flowToTest + "] having Id " + invoiceLineItemId +
                    ". " + e.getMessage());
        }
        return lineItemValidationStatus;
    }

    private void verifyInvoiceLineItemForEdit(String flowToTest, int invoiceLineItemId, CustomAssert csAssert) {

//        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//        workflowActionsHelper.performWorkflowAction()
        try {

            String invoiceLineItemEntity = "invoice line item";

            logger.info("Verifying Invoice Line Item for Flow [{}] having Id {}.", flowToTest, invoiceLineItemId);
            int invoiceLineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity);
            long timeSpent = 0;
            Long lineItemValidationTimeOut = 1200000L;

            while (timeSpent <= lineItemValidationTimeOut && ShowHelper.isLineItemUnderOngoingValidation(invoiceLineItemEntityTypeId, invoiceLineItemId)) {
                logger.info("Invoice Line Item having Id {} is still Under Ongoing Validation. Waiting for it to finish validation.", invoiceLineItemId);
                logger.info("time spent is : [{}]", timeSpent);
                Thread.sleep(10000);
                timeSpent += 10000;
            }

            if (timeSpent < lineItemValidationTimeOut) {
                //Line Item Validation is Completed.
                //String expectedResult = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "expectedresult");
                String actualResult = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invoiceLineItemId, "validationStatus");
                logger.info("Expected Result is [{}] and Actual Result is [{}].", "[Invalid Service Period]", actualResult);
                if(actualResult.trim().toLowerCase().contains("no discrepancy")){
                    csAssert.assertTrue(false,
                            "Invoice Line Item Validation failed as Expected Value is [Invalid Service Period] and Actual Value is " + actualResult);
                }
            } else {
                //Line Item Validation is not yet Completed.
                logger.info("Invoice Line Item Validation couldn't be completed for Flow [{}] within TimeOut {} milliseconds", flowToTest, lineItemValidationTimeOut);

                logger.error("FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [{}]", flowToTest);
                csAssert.assertTrue(false, "FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [" +
                        flowToTest + "]");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Invoice Line Item for Flow [{}] having Id {}. {}", flowToTest, invoiceLineItemId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while verifying Invoice Line Item for Flow [" + flowToTest + "] having Id " + invoiceLineItemId +
                    ". " + e.getMessage());
        }

    }

    private void deleteNewEntity(String entityName, int entityId) {


        try {
            logger.info("Hitting Show API for Entity {} having Id {}.", entityName, entityId);
            Show showObj = new Show();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            showObj.hitShow(entityTypeId, entityId);
            if (ParseJsonResponse.validJsonResponse(showObj.getShowJsonStr())) {
                JSONObject jsonObj = new JSONObject(showObj.getShowJsonStr());
                String prefix = "{\"body\":{\"data\":";
                String suffix = "}}";
                String showBodyStr = jsonObj.getJSONObject("body").getJSONObject("data").toString();
                String deletePayload = prefix + showBodyStr + suffix;

                logger.info("Deleting Entity {} having Id {}.", entityName, entityId);
                Delete deleteObj = new Delete();
                deleteObj.hitDelete(entityName, deletePayload);
                String deleteJsonStr = deleteObj.getDeleteJsonStr();
                jsonObj = new JSONObject(deleteJsonStr);
                String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
                if (status.trim().equalsIgnoreCase("success"))
                    logger.info("Entity having Id {} is deleted Successfully.", entityId);
            }
        } catch (Exception e) {
            logger.error("Exception while deleting Entity {} having Id {}. {}", entityName, entityId, e.getStackTrace());
        }
    }

}
class StoreInvoice{

    String invoiceSectionName;
    String invoiceLISectionName;
    String invoiceId;
    String invoiceLIId;
}
