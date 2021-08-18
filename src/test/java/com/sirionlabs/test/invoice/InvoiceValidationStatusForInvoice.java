package com.sirionlabs.test.invoice;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.invoice.InvoiceReValidationCheck;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.Reports.ReportsDownloadHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.ForecastUploadHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class InvoiceValidationStatusForInvoice extends TestRailBase {
    private final static Logger logger = LoggerFactory.getLogger(InvoiceValidationStatusForInvoice.class);
    private Map<String, String> validationStatusCellColorMap = new HashMap<>();
    private int entityTypeId = 67;
    private Map<Integer, String> deleteEntityMap = new HashMap<>();
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

    @AfterMethod
    public void afterTestInvoiceLineItemRejectValidation(){
        for(Map.Entry<Integer,String> me : deleteEntityMap.entrySet()){
            deleteNewEntity(me.getValue(),me.getKey());
        }
    }

    @BeforeClass
    public void BeforeClass() {
        //Initializing a hashmap for the action to their corresponding cell color in xlxs file
        validationStatusCellColorMap.put("Action Required", "ffd40f0f");
        validationStatusCellColorMap.put("Completed", "ff00a74c");
        validationStatusCellColorMap.put("In Progress", "ffffc200");
        validationStatusCellColorMap.put("Re-validation Required", "ff007ACC");


        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceValidationStatusInvoiceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceValidationStatusInvoiceConfigFileName");
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

    @Test(enabled = false)//C88333
    public void downloadInvoiceTracker() {

        CustomAssert customAssert = new CustomAssert();
        int reportId = 51;
        String reportName = "Invoices - Tracker";
        String entityTypeName = "invoices";
        try {
            ReportsDownloadHelper reportsDownloadHelper = new ReportsDownloadHelper();

            logger.info("calling reportGraphDownload() for downloading graph");
            String response = reportsDownloadHelper.reportGraphDownload(reportId, reportName, entityTypeId, entityTypeName);

            logger.info("Now checking if the downloaded file exists");
            File downloadedGraph = new File(response);
            boolean existsDownloadedGraph = downloadedGraph.exists();

            logger.info("Response is {}", response);

            if (!existsDownloadedGraph) {
                logger.error("Downloaded graph doesn't exist");
                customAssert.assertTrue(false, "Downloaded graph doesn't exist");
            } else
                logger.info("Downloaded graph exists, So continuing with success");

            reportsDownloadHelper = new ReportsDownloadHelper();
            logger.info("calling reportGraphDownloadWithData() for downloading graph with data");

            String filterJson = "\"373\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"Action Required\"},{\"id\":\"3\",\"name\":\"Completed\"},{\"id\":\"1\",\"name\":\"In Progress\"},{\"id\":\"2\",\"name\":\"Revalidation Required\"}]},\"filterId\":373,\"filterName\":\"invoiceValidationStatus\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}";
            response = reportsDownloadHelper.reportGraphDownloadWithData(51, "Invoices - Tracker", 67, "invoices", filterJson, false);

            downloadedGraph = new File(response);
            existsDownloadedGraph = downloadedGraph.exists();

            if (!existsDownloadedGraph) {
                logger.info("Downloaded graph doesn't exist");
                customAssert.assertTrue(false, "Downloaded graph doesn't exist");
            } else
                logger.info("Downloaded graph exists, So continuing with success");

            logger.info("Response is {}", response);

            try {
                XLSUtils xlsUtils = new XLSUtils(response);
                int rowCount = xlsUtils.getRowCount("Data");
                String colorHex = "";
                int columnIndexToCheck = 56;
                for (int rowIndex = 4; rowIndex < rowCount - 2; rowIndex++) {

                    String data = xlsUtils.getCellData("Data", 56, rowIndex);
                    if (data == null)
                        continue;
                    Color color = xlsUtils.getCellColor("Data", rowIndex, columnIndexToCheck);
                    if (color instanceof XSSFColor) {
                        colorHex = ((XSSFColor) color).getARGBHex();
                    }

                    logger.info("Cell Data [{}] Color [{}]", data, colorHex);

                    if (validationStatusCellColorMap.containsKey(data)) {
                        String mapValue = validationStatusCellColorMap.get(data);
                        if (!mapValue.equalsIgnoreCase(colorHex)) {
                            logger.info("Color mismatch. Required is {} but found {}", mapValue, colorHex);
                            customAssert.assertTrue(false, "Color mismatch. Required is " + mapValue + " but found " + colorHex);
                        } else
                            logger.info("Color match success Required is {} and found {}", mapValue, colorHex);
                    }

                }
            } catch (Exception e) {
                logger.error(e.toString());
            }
        } catch (Exception e) {
            logger.error("Error Occurred " + e.toString());
            customAssert.assertTrue(false, "Error Occurred " + e.toString());
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)//C76593
    public void InvoiceValidationStatusOnShowPage() {

        CustomAssert customAssert = new CustomAssert();
        final int listIdInvoice = 10;

        try {

            String filterJson = "\"373\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"Action Required\"},{\"id\":\"1\",\"name\":\"In Progress\"},{\"id\":\"2\",\"name\":\"Revalidation Required\"}]},\"filterId\":373,\"filterName\":\"invoiceValidationStatus\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}";

            //payload for listing invoices
            String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{" + filterJson + "}}}";

            ListRendererListData invoiceListData = new ListRendererListData();
            invoiceListData.hitListRendererListDataV2(listIdInvoice, listDataPayload);
            String res = invoiceListData.getListDataJsonStr();

            JSONObject jsonObject = new JSONObject(res);
            try {
                String invoiceIdString = jsonObject.getJSONArray("data").getJSONObject(0).getJSONObject("203").get("value").toString();//Getting the Invoice at the zeroth index
                int invoiceId = Integer.parseInt(invoiceIdString.trim().split(":;")[1]);
                logger.info("invoiceId : {}", invoiceId);

                HttpResponse response = hitActionUrl(entityTypeId, invoiceId);
                String invoiceData = EntityUtils.toString(response.getEntity());
                JSONArray actionJsonObject = new JSONArray(invoiceData);
                String obj;
                boolean validationStatusFound = false;//To check whether the response contains any value with the below mentioned label
                for (int jsonIndex = 0; jsonIndex < actionJsonObject.length(); jsonIndex++) {
                    obj = actionJsonObject.getJSONObject(jsonIndex).get("label").toString();
                    if (obj.equalsIgnoreCase("Show lineitems")) {
                        validationStatusFound = true;
                        break;
                    }
                }
                if (!validationStatusFound) {
                    customAssert.assertTrue(false, "Validation Status Line not found");
                    logger.info("Validation Status Line not found");
                } else {
                    logger.info("Validation Status Line found, Test Successful");
                }

                customAssert.assertAll();

            } catch (Exception e) {
                logger.error("Exception caught : " + e.toString());
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception caught " + e.toString());
        }
    }

    @DataProvider()
    public Object[][] dataProviderForInvoiceStatusIndicatorReValidation() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest("flowstovalidate");
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider()
    public Object[][] dataProviderForInvoiceValidationStatusCheck() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest("flowstovalidate");
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(enabled = true)//C88318
    public void InvoiceStatusIndicatorReValidation() {

        String flowToTest ="arc flow for revalidation required";

        String publishAction = "publish";

        CustomAssert csAssert = new CustomAssert();

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId;
        int serviceDataId;
        int invoiceId;
        int invoiceLineItemId = -1;
        List<Integer> consumptionIds;

        String contractSectionName = "default";
        String serviceDataSectionName = "default";
        String invoiceSectionName = "default";
        String invoiceLineItemSectionName = "default";

        try {
            //Get Contract that will be used for Invoice Flow Validation
            synchronized (this) {
                InvoiceHelper invoiceHelper = new InvoiceHelper();
                String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname");
                if (temp != null)
                    contractSectionName = temp.trim();


                contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);

                if (contractId != -1) {
                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                    deleteEntityMap.put(contractId, contractEntity);

                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, contractSectionName, contractId);

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();

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

                                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                                            {

                                                WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                                                boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId,serviceDataId,publishAction);
                                                //boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataId, serviceDataEntity, serviceDataEntitySectionUrlName);

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

                                                            result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
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
                                                                    result = workflowActionsHelper.performWorkflowAction(ConfigureConstantFields.getEntityIdByName(consumptionEntity),consumptionIds.get(i),approveAction);
                                                                    //result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds.get(i), consumptionEntity, consumptionEntitySectionUrlName);
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
                                                } else {

                                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
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

                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                        if (temp != null)
                            invoiceSectionName = temp.trim();
                        invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceSectionName);

                        logger.info("Created Invoice Id is : [{}]", invoiceId);
                        if (invoiceId != -1) {
                            deleteEntityMap.put(invoiceId, invoiceEntity);
                            //Get Invoice Line Item Id
                            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
                            if (temp != null)
                                invoiceLineItemSectionName = temp.trim();

                            invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, serviceDataId);
                            logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                            int listIdInvoice = 10;
                            if (invoiceLineItemId != -1) {
                                deleteEntityMap.put(invoiceLineItemId, invoiceLineItemEntity);
                                Boolean verifyInvoiceLineItem = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath, flowsConfigFileName, flowToTest, invoiceLineItemId, csAssert);
                                if (verifyInvoiceLineItem) {

//                                    try {
//
//                                        String filterJson = "\"373\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"Action Required\"},{\"id\":\"1\",\"name\":\"In Progress\"},{\"id\":\"2\",\"name\":\"Revalidation Required\"}]},\"filterId\":373,\"filterName\":\"invoiceValidationStatus\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}";
//
//                                        //payload for listing invoices
//                                        String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{" + filterJson + "}}}";
//
//                                        ListRendererListData invoiceListData = new ListRendererListData();
//                                        invoiceListData.hitListRendererListDataV2(listIdInvoice, listDataPayload);
//                                        String res = invoiceListData.getListDataJsonStr();
//                                        String notFoundField = "";
//                                        JSONObject jsonObject = new JSONObject(res);
//                                        int listDataCount = jsonObject.getJSONArray("data").length();
//                                        for (int listDataIndex = 0; listDataIndex < listDataCount; listDataIndex++) {
//                                            try {
//                                                String invoiceIdString = jsonObject.getJSONArray("data").getJSONObject(listDataIndex).getJSONObject("203").get("value").toString();//Getting the Invoice at the zeroth index
//                                                invoiceId = Integer.parseInt(invoiceIdString.trim().split(":;")[1]);
//                                                logger.info("invoiceId : {}", invoiceId);
//
//                                                HttpResponse response = hitActionUrl(entityTypeId, invoiceId);
//                                                String invoiceData = EntityUtils.toString(response.getEntity());
//                                                JSONArray actionJsonObject = new JSONArray(invoiceData);
//                                                String obj = "";
//                                                String actionRequired = "Action Required";
//                                                boolean validationStatusFound = true;//To check whether the response contains any value with the below mentioned label
//                                                for (int jsonIndex = 0; jsonIndex < actionJsonObject.length(); jsonIndex++) {
//                                                    try {
//                                                        obj = actionJsonObject.getJSONObject(jsonIndex).get("label").toString();
//                                                    } catch (Exception e) {
//                                                        continue;
//                                                    }
//                                                    if (obj.equalsIgnoreCase("Show lineitems")) {
//                                                        if (!actionRequired.equalsIgnoreCase(actionJsonObject.getJSONObject(jsonIndex).getJSONObject("properties").get("validationStatusValue").toString())) {
//                                                            validationStatusFound = false;
//                                                            notFoundField = actionJsonObject.getJSONObject(jsonIndex).getJSONObject("properties").get("validationStatusValue").toString();
//                                                            break;
//                                                        }
//                                                    }
//                                                }
//                                                if (!validationStatusFound) {
//                                                    csAssert.assertTrue(false, "Validation Status String not matched " + notFoundField);
//                                                    logger.info("Validation Status Line not found {}", notFoundField);
//                                                } else {
//                                                    logger.info("Validation Status String matched, Test Successful");
//                                                }
//
//
//                                            } catch (Exception e) {
//                                                logger.error("Exception caught : " + e.toString());
//                                                csAssert.assertTrue(false, "Exception caught : " + e.toString());
//                                            }
//                                        }
//
//                                    } catch (Exception e) {
//                                        csAssert.assertTrue(false, "Exception caught " + e.toString());
//                                    }
                                    String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                            "pricingstemplatefilename");
                                    boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, csAssert);
                                    if(pricingFile)

                                        pricingFile = InvoiceHelper.editPricingFileForARCRRC(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId);//editPricingFileForARCRRC(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);

                                    if(pricingFile){
                                        String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                        if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {
                                            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {

                                            }
                                                String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

                                                if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                                    logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                                    csAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                                            flowToTest + "]");
                                                    addTestResult(getTestCaseIdForMethodName("dataProviderForInvoiceStatusIndicatorReValidation"), csAssert);
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
                                                    addTestResult(getTestCaseIdForMethodName("dataProviderForInvoiceStatusIndicatorReValidation"), csAssert);
                                                    csAssert.assertAll();
                                                    return;
                                                } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                                                {
                                                    boolean verifyInvoice = InvoiceHelper.verifyInvoiceValidationStatus(flowsConfigFilePath,flowsConfigFileName,flowToTest,invoiceId,csAssert);
                                                    if(verifyInvoice){
                                                        logger.info("Invoice Validation status Revalidation Required found");
                                                        boolean revalidate = InvoiceReValidationCheck.revalidateInvoice(invoiceId,csAssert);
                                                        if(!revalidate){
                                                            logger.info("Couldn't revalidate invoice first time {}",invoiceId);
                                                            csAssert.assertTrue(false,"Couldn't revalidate invoice first time "+invoiceId);
                                                            csAssert.assertAll();
                                                        }
                                                        //updating consumption
                                                        consumptionIds = invoiceHelper.consumptionIds;
                                                        // after consumptions have been created successfully
                                                        logger.info("Consumption Ids are : [{}]", consumptionIds);
                                                        String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                                "finalconsumptionvalues").trim().split(Pattern.quote(","));
                                                        boolean result;
                                                        for (int i = 0; i < consumptionIds.size(); i++) {

                                                            result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                                                            if (!result) {
                                                                logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
                                                                csAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                                                        "Hence skipping validation");
                                                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                                csAssert.assertAll();
                                                                return;

                                                            }
                                                        }
                                                        verifyInvoice = InvoiceHelper.verifyInvoiceValidationStatus(flowsConfigFilePath,flowsConfigFileName,flowToTest,invoiceId,csAssert);
                                                        if(verifyInvoice){
                                                            logger.info("Invoice Validation status Revalidation Required found");
                                                            revalidate = InvoiceReValidationCheck.revalidateInvoice(invoiceId,csAssert);
                                                            if(!revalidate){
                                                                logger.info("Couldn't revalidate invoice second time {}",invoiceId);
                                                                csAssert.assertTrue(false,"Couldn't revalidate invoice second time "+invoiceId);
                                                                csAssert.assertAll();
                                                            }
                                                        }else{
                                                            logger.info("Invoice Revalidation status not found hence teminating");
                                                            csAssert.assertTrue(false,"Invoice Revalidation status not found");
                                                            csAssert.assertAll();
                                                        }
                                                    }
                                                    else{
                                                        logger.info("Invoice Revalidation status not found hence teminating");
                                                        csAssert.assertTrue(false,"Invoice Revalidation status not found");
                                                        csAssert.assertAll();
                                                    }
                                                }
                                                }
                                    }
                                }
                            }

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

    @Test(enabled = false)//C88375
    public void InternationalizationInvoiceValidationStatus() {

        Map<String, String> fieldNames = new HashMap<>();
        int random = RandomNumbers.getRandomNumberWithinRange(100, 999);
        fieldNames.put("Completed", "Completed" + random);
        fieldNames.put("Action Required", "Action Required" + random);
        fieldNames.put("Revalidation Required", "Revalidation Required" + random);
        fieldNames.put("In Progress", "In Progress" + random);
        fieldNames.put("Invoice Validation Status", "Invoice Validation Status" + random);

        Map<String, String> fieldNamesRev = new HashMap<>();
        for (Map.Entry<String, String> entry : fieldNames.entrySet()) {
            fieldNamesRev.put(entry.getValue(), entry.getKey());
        }
        fieldNamesRev.remove("Invoice Validation Status" + random);


        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C88375.");

        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        FieldRenaming fieldRenamingObj = new FieldRenaming();
        UpdateAccount updateAccountObj = new UpdateAccount();

        int currentLanguageId = updateAccountObj.getCurrentLanguageIdForUser(lastLoggedInUserName, 1005);

        if (currentLanguageId == -1) {
            throw new SkipException("Couldn't get Current Language Id for User " + lastLoggedInUserName);
        }

        //Update Language Id for User.
        if (!updateAccountObj.updateUserLanguage(lastLoggedInUserName, 1005, 1000)) {
            throw new SkipException("Couldn't Change Language for User " + lastLoggedInUserName + " to Russian");
        }

        //new Check().hitCheck(adminUserName,adminPassword);
        new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminUser());

        int groupId = 91;
        int fieldsReplacedCount = 0;
        try {
            String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1000, groupId);
            String payloadForFieldRenamingUpdate = fieldRenamingResponse;
            JSONObject jsonObject = new JSONObject(payloadForFieldRenamingUpdate);
            JSONArray jsonArray = jsonObject.getJSONArray("childGroups");
            int i;
            for (i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.getJSONObject(i).get("name").toString().equalsIgnoreCase("Metadata")) {
                    jsonArray = jsonArray.getJSONObject(i).getJSONArray("fieldLabels");
                    break;
                }
            }
            for (int counter = 0; counter < jsonArray.length(); counter++) {
                if (fieldNames.containsKey(jsonArray.getJSONObject(counter).get("name").toString())) {
                    jsonArray.getJSONObject(counter).put("clientFieldName", fieldNames.get(jsonArray.getJSONObject(counter).get("name").toString()));
                    fieldsReplacedCount++;
                }
            }
            jsonObject.getJSONArray("childGroups").getJSONObject(i).put("fieldLabels", jsonArray);
            payloadForFieldRenamingUpdate = jsonObject.toString();

            if (fieldsReplacedCount == 0) {
                logger.info("No matching fields found, so asserting false");
                csAssert.assertTrue(false, "No matching fields found");
            } else {
                logger.info("Total No of Fields Selected for Renaming: {}", fieldsReplacedCount);
                String fieldUpdateResponse = fieldRenamingObj.hitFieldUpdate(payloadForFieldRenamingUpdate);

                JSONObject jsonObj = new JSONObject(fieldUpdateResponse);
                if (!jsonObj.getBoolean("isSuccess")) {
                    throw new SkipException("Couldn't update Field Labels");
                }

                new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
                if (!updateAccountObj.updateUserLanguage(lastLoggedInUserName, 1005, 1000)) {
                    throw new SkipException("Couldn't Change Language for User " + lastLoggedInUserName + " to Russian");
                }
                //login with user credential

                final int listIdInvoice = 10;

//                ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
//                listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(listIdInvoice);
//                String filterDataResponse = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();

                ListRendererListData listRendererListData = new ListRendererListData();
                listRendererListData.hitListRendererFilterData(listIdInvoice, "{}");
                String filterDataResponse = listRendererListData.getListDataJsonStr();

                boolean gotChangedFields = true;
                String notFoundField = "";

                //checking presence of fields in filter
                for (Map.Entry<String, String> entry : fieldNamesRev.entrySet()) {
                    if (!filterDataResponse.contains(entry.getValue())) {
                        gotChangedFields = false;
                        notFoundField = entry.getValue();
                        break;
                    }
                }

                ReportRendererFilterData reportRendererFilterData = new ReportRendererFilterData();
                reportRendererFilterData.hitReportRendererFilterData(51);
                filterDataResponse = reportRendererFilterData.getReportRendererFilterDataJsonStr();

                gotChangedFields = true;
                notFoundField = "";

                //checking presence of fields in filter for report
                for (Map.Entry<String, String> entry : fieldNamesRev.entrySet()) {
                    if (!filterDataResponse.contains(entry.getValue())) {
                        gotChangedFields = false;
                        notFoundField = entry.getValue();
                        break;
                    }
                }

                if (!gotChangedFields) {
                    logger.info("Report Filter data doesn't contain the changed fields name : {}", notFoundField);
                    csAssert.assertTrue(false, "Report Filter data doesn't contain the changed fields name : " + notFoundField);
                    csAssert.assertAll();
                }//stop when any of the field is not found changed


                try {

                    String filterJson = "\"373\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"" + fieldNames.get("Action Required") + "\"},{\"id\":\"1\",\"name\":\"" + fieldNames.get("In Progress") + "\"},{\"id\":\"2\",\"name\":\"" + fieldNames.get("Revalidation Required") + "\"}]},\"filterId\":373,\"filterName\":\"invoiceValidationStatus\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}";

                    //payload for listing invoices
                    String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{" + filterJson + "}}}";

                    ListRendererListData invoiceListData = new ListRendererListData();
                    invoiceListData.hitListRendererListDataV2(listIdInvoice, listDataPayload);
                    String res = invoiceListData.getListDataJsonStr();

                    jsonObject = new JSONObject(res);
                    int listDataCount = jsonObject.getJSONArray("data").length();
                    for (int listDataIndex = 0; listDataIndex < listDataCount; listDataIndex++) {
                        try {
                            String invoiceIdString = jsonObject.getJSONArray("data").getJSONObject(listDataIndex).getJSONObject("203").get("value").toString();//Getting the Invoice at the zeroth index
                            int invoiceId = Integer.parseInt(invoiceIdString.trim().split(":;")[1]);
                            logger.info("invoiceId : {}", invoiceId);

                            updateAccountObj.updateUserLanguage(lastLoggedInUserName, 1005, 1000);
                            HttpResponse response = hitActionUrl(entityTypeId, invoiceId);
                            String invoiceData = EntityUtils.toString(response.getEntity());
                            JSONArray actionJsonObject = new JSONArray(invoiceData);
                            String obj = "";
                            boolean validationStatusFound = true;//To check whether the response contains any value with the below mentioned label
                            for (int jsonIndex = 0; jsonIndex < actionJsonObject.length(); jsonIndex++) {
                                try {
                                    obj = actionJsonObject.getJSONObject(jsonIndex).get("label").toString();
                                } catch (Exception e) {
                                    continue;
                                }
                                if (obj.equalsIgnoreCase("Show lineitems")) {
                                    if (!fieldNamesRev.containsKey(actionJsonObject.getJSONObject(jsonIndex).getJSONObject("properties").get("validationStatusValue").toString())) {
                                        validationStatusFound = false;
                                        notFoundField = actionJsonObject.getJSONObject(jsonIndex).getJSONObject("properties").get("validationStatusValue").toString();
                                        break;
                                    }
                                }
                            }
                            if (!validationStatusFound) {
                                csAssert.assertTrue(false, "Validation Status String not matched " + notFoundField);
                                logger.info("Validation Status Line not found {}", notFoundField);
                            } else {
                                logger.info("Validation Status String matched, Test Successful");
                            }


                        } catch (Exception e) {
                            logger.error("Exception caught : " + e.toString());
                            csAssert.assertTrue(false, "Exception caught : " + e.toString());
                        }
                    }

                } catch (Exception e) {
                    csAssert.assertTrue(false, "Exception caught " + e.toString());
                }

                new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminUser());

                fieldRenamingObj.hitFieldUpdate(fieldRenamingResponse);

            }
        } catch (Exception e) {
            logger.info("Exception Caught {}", e.toString());
            csAssert.assertTrue(false, "Exception Caught " + e.toString());
        }
        //Revert Language for User.
        updateAccountObj.updateUserLanguage(lastLoggedInUserName, 1002, currentLanguageId);

        csAssert.assertAll();

    }

    //TODO
    @Test(dataProvider = "dataProviderForInvoiceValidationStatusCheck", enabled = false)
    public void InvoiceValidationStatusCheck(String flowToTest) {

        String publishAction = "publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        CustomAssert csAssert = new CustomAssert();

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId;
        int serviceDataId;
        int invoiceId = -1;
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


                contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);
                //contractId = getContractId();
                if (contractId != -1) {
                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                    deleteEntityMap.put(contractId, contractEntity);


                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, contractSectionName, contractId);

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();

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


                                if ((serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) && !flowToTest.equals("fixed fee flow for no arc/rrc")) {

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


                                                if ((serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) && !flowToTest.equals("fixed fee flow for no arc/rrc")) {

                                                    boolean isDataCreatedUnderARCRRCTab = isARCRRCCreated(serviceDataId);


                                                    if (!isDataCreatedUnderARCRRCTab) {
                                                        csAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                                                flowToTest + "]");
                                                        //addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
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

                                            if (flowToTest.equals("fixed fee flow for no arc/rrc")) {

                                                JSONObject valueType = new JSONObject();
                                                valueType.put("name", "ARC/RRC");
                                                valueType.put("id", "3");

                                                Show show = new Show();
                                                show.hitShowVersion2(ConfigureConstantFields.getEntityIdByName(serviceDataEntity), serviceDataId);
                                                String response = show.getShowJsonStr();
                                                JSONObject jsonObject = new JSONObject(response);
                                                jsonObject.remove("header");
                                                jsonObject.remove("session");
                                                jsonObject.getJSONObject("body").remove("layoutInfo");
                                                jsonObject.getJSONObject("body").remove("globalData");
                                                jsonObject.getJSONObject("body").remove("errors");
                                                jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingType").put("values", valueType);
                                                jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("consumptionAvailable").put("values", "true");

                                                Edit edit = new Edit();
                                                edit.hitEdit(serviceDataEntity, jsonObject.toString());
                                                response = edit.editAPIResponseCode;
                                            }

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


                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                        if (temp != null)
                            invoiceSectionName = temp.trim();
                        invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceSectionName);


                        logger.info("Created Invoice Id is : [{}]", invoiceId);
                        if (invoiceId != -1) {
                            //Get Invoice Line Item Id
                            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
                            if (temp != null)
                                invoiceLineItemSectionName = temp.trim();
                            //invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataId);
                            invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, serviceDataId);
                            logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                            deleteEntityMap.put(invoiceId, invoiceEntity);
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
                deleteEntityMap.put(invoiceLineItemId, invoiceLineItemEntity);

                boolean verifyInvoiceLineItem = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath, flowsConfigFileName, flowToTest, invoiceLineItemId, csAssert);

                if ((flowToTest.equalsIgnoreCase("fixed fee flow 1") || flowToTest.equalsIgnoreCase("arc flow 1") || flowToTest.equalsIgnoreCase("forecast flow 1")) && verifyInvoiceLineItem) {

                    Create create = new Create();
                    Clone clone = new Clone();
                    try {
                        String cloneResponse = clone.hitClone(invoiceLineItemEntity, invoiceLineItemId);

                        JSONObject cloneResponseJson = new JSONObject(cloneResponse);
                        JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");

                        JSONObject createEntityJson = new JSONObject();
                        JSONObject createEntityBodyJson = new JSONObject();
                        createEntityBodyJson.put("data", dataJson);
                        createEntityJson.put("body", createEntityBodyJson);

                        create.hitCreate(invoiceLineItemEntity, createEntityJson.toString());
                        String createResponse = create.getCreateJsonStr();
                        int newInvoiceLineItem = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);
                        verifyInvoiceLineItem = verifyInvoiceLineItemForDuplicate(flowsConfigFilePath, flowsConfigFileName, flowToTest, newInvoiceLineItem, csAssert);

                    } catch (Exception e) {
                        logger.error("Exception while cloning invoice line item");
                        csAssert.assertTrue(false, "Exception while cloning invoice line item" + e.toString());
                    }

                }
//                else{
//                    customAssert.assertTrue(true,"Invoice line item verification failed");
//                }

                if (verifyInvoiceLineItem) {
                    Boolean verifyInvoice = InvoiceHelper.verifyInvoiceValidationStatus(flowsConfigFilePath, flowsConfigFileName, flowToTest, invoiceId, csAssert);
                    if (!verifyInvoice) {
                        csAssert.assertTrue(false, "Invoice Validation Check failed");
                    }
                } else {
                    //customAssert.assertTrue(true,"Cloned Invoice line item verification failed");
                    csAssert.assertTrue(false, "Invoice line item verification failed");
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

        addTestResult(getTestCaseIdForMethodName("InvoiceValidationStatusFilterCheck"), csAssert);
        csAssert.assertAll();
    }

    //ToDo
    @Test(dataProvider = "dataProviderForInvoiceValidationStatusCheck", enabled = false)
    public void invoiceValidationStatusIndicatorCompleted(String flowToTest) {

        String publishAction = "publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        CustomAssert csAssert = new CustomAssert();

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId;
        int serviceDataId;
        int invoiceId = -1;
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


                contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);
                //contractId = getContractId();
                if (contractId != -1) {
                    logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                    deleteEntityMap.put(contractId, contractEntity);


                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, contractSectionName, contractId);

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();

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
                                                        //addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
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


                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                        if (temp != null)
                            invoiceSectionName = temp.trim();
                        invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceSectionName);


                        logger.info("Created Invoice Id is : [{}]", invoiceId);
                        if (invoiceId != -1) {
                            //Get Invoice Line Item Id
                            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
                            if (temp != null)
                                invoiceLineItemSectionName = temp.trim();
                            //invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataId);
                            invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, serviceDataId);
                            logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                            deleteEntityMap.put(invoiceId, invoiceEntity);
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
                deleteEntityMap.put(invoiceLineItemId, invoiceLineItemEntity);

                boolean verifyInvoiceLineItem = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath, flowsConfigFileName, flowToTest, invoiceLineItemId, csAssert);

                if ((flowToTest.equalsIgnoreCase("fixed fee flow 1") || flowToTest.equalsIgnoreCase("arc flow 1") || flowToTest.equalsIgnoreCase("forecast flow 1")) && verifyInvoiceLineItem) {

                    Create create = new Create();
                    Clone clone = new Clone();
                    try {
                        String cloneResponse = clone.hitClone(invoiceLineItemEntity, invoiceLineItemId);

                        JSONObject cloneResponseJson = new JSONObject(cloneResponse);
                        JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");

                        JSONObject createEntityJson = new JSONObject();
                        JSONObject createEntityBodyJson = new JSONObject();
                        createEntityBodyJson.put("data", dataJson);
                        createEntityJson.put("body", createEntityBodyJson);

                        create.hitCreate(invoiceLineItemEntity, createEntityJson.toString());
                        String createResponse = create.getCreateJsonStr();
                        int newInvoiceLineItem = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);
                        verifyInvoiceLineItem = verifyInvoiceLineItemForDuplicate(flowsConfigFilePath, flowsConfigFileName, flowToTest, newInvoiceLineItem, csAssert);

                    } catch (Exception e) {
                        logger.error("Exception while cloning invoice line item");
                        csAssert.assertTrue(false, "Exception while cloning invoice line item" + e.toString());
                    }

                }
//                else{
//                    customAssert.assertTrue(false,"Invoice line item verification failed");
//                }

                if (verifyInvoiceLineItem) {
                    Boolean verifyInvoice = InvoiceHelper.verifyInvoiceValidationStatus(flowsConfigFilePath, flowsConfigFileName, flowToTest, invoiceId, csAssert);
                    if (!verifyInvoice) {
                        csAssert.assertTrue(false, "Invoice Validation Check failed");
                    }
                } else {
                    //customAssert.assertTrue(true,"Cloned Invoice line item verification failed");
                    csAssert.assertTrue(false, "Invoice line item verification failed");
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

        addTestResult(getTestCaseIdForMethodName("InvoiceValidationStatusFilterCheck"), csAssert);
        csAssert.assertAll();

    }

    @Test(enabled = true) //C76594
    public void CheckColumnsFiltersInvoiceListingProvisionedFromClientAdminClientSetupAdmin() {

        CustomAssert customAssert = new CustomAssert();

        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;
        String url = "";
        try {
            //Checking Setup admin
            ClientSetupHelper clientSetupHelper = new ClientSetupHelper();
            clientSetupHelper.loginWithClientSetupUser();
            url = "/listRenderer/list/10/listJson?clientId=1002";
            boolean setupValidationCheck = checkInvoiceValidationInFilterAndColumn(url, customAssert);
            if (!setupValidationCheck) {
                logger.info("Invoice Validation Status not found in Client setup admin Filter or List Header");
                customAssert.assertTrue(false, "Invoice Validation Status not found in Client setup admin Filter or List Header");
                customAssert.assertAll();
            }
            //Checking Client Admin
            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());
            url = "/listRenderer/list/10/configure";
            setupValidationCheck = checkInvoiceValidationInFilterAndColumn(url, customAssert);
            if (!setupValidationCheck) {
                logger.info("Invoice Validation Status not found in Client admin Filter or List Header");
                customAssert.assertTrue(false, "Invoice Validation Status not found in Client admin Filter or List Header");
                customAssert.assertAll();
            }
            //Checking End User
            new Check().hitCheck(lastUserName, lastUserPassword);
            url = "/listRenderer/list/10/defaultUserListMetaData?am=true";
            setupValidationCheck = checkInvoiceValidationInFilterAndColumn(url, customAssert);
            if (!setupValidationCheck) {
                logger.info("Invoice Validation Status not found in End User Filter or List Header");
                customAssert.assertTrue(false, "Invoice Validation Status not found in End User Filter or List Header");
                customAssert.assertAll();
            }

        } catch (Exception e) {
            logger.info("Exception while hitting {} and getting response", url);
            customAssert.assertTrue(false, "Exception while hitting " + url + " and getting response");
        }
    }

    @Test(enabled = true) //C76595
    public void CheckColumnsFiltersExcelInvoiceListingProvisionedFromClientAdminClientSetupAdmin() {

        CustomAssert customAssert = new CustomAssert();
        //ToDO check and run the method

        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;
        String url = "";
        try {
            //Checking Setup admin
            ClientSetupHelper clientSetupHelper = new ClientSetupHelper();
            clientSetupHelper.loginWithClientSetupUser();
            url = "/reportRenderer/list/51/listJson?clientId=1002";
            boolean setupValidationCheck = checkInvoiceValidationInExcel(url, customAssert);
            if (!setupValidationCheck) {
                logger.info("Invoice Validation Status not found in Client setup admin Filter or List Header");
                customAssert.assertTrue(false, "Invoice Validation Status not found in Client setup admin Filter or List Header");
                customAssert.assertAll();
            }
            //Checking Client Admin
            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());
            url = "/reportRenderer/list/51/configure";
            setupValidationCheck = checkInvoiceValidationInExcel(url, customAssert);
            if (!setupValidationCheck) {
                logger.info("Invoice Validation Status not found in Client admin Filter or List Header");
                customAssert.assertTrue(false, "Invoice Validation Status not found in Client admin Filter or List Header");
                customAssert.assertAll();
            }
            //Checking End User
            new Check().hitCheck(lastUserName, lastUserPassword);
            setupValidationCheck = checkInvoiceValidationInExcel(url, customAssert);
            url = "/reportRenderer/list/51/defaultUserListMetaData";
            if (!setupValidationCheck) {
                logger.info("Invoice Validation Status not found in End User Filter or List Header");
                customAssert.assertTrue(false, "Invoice Validation Status not found in End User Filter or List Header");
                customAssert.assertAll();
            }

        } catch (Exception e) {
            logger.info("Exception while hitting {} and getting response", url);
            customAssert.assertTrue(false, "Exception while hitting " + url + " and getting response");
        }
    }

    private HttpResponse hitActionUrl(int invoiceTypeId, int invoiceId) {
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/v2/actions/" + invoiceTypeId + "/" + invoiceId;

            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

        } catch (Exception e) {
            logger.error("Exception while hitting DownloadGraph Api. {}", e.getMessage());
        }
        return response;
    }

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
                String forecastSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

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

    // this will update the forecast sheet based on the deleteEntityMap created in getValuesMapForForecastSheet
    private synchronized boolean editForecastSheet(String templateFileName, String flowToTest, String clientId, Integer contractId, InvoicePricingHelper pricingObj) {

        boolean pricingFile = false;
        Map<Integer, Map<Integer, Object>> forecastValuesMap = getValuesMapForForecastSheet(flowToTest, clientId);
        String forecastSheetNameInXLSXFile = "Forecast Data";


        try {

            boolean editTemplate = pricingObj.editPricingTemplateMultipleRows(forecastTemplateFilePath, templateFileName, forecastSheetNameInXLSXFile,
                    forecastValuesMap);

            if (editTemplate) {
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

    private List<String> getFlowsToTest(String flowsToValidate) {
        List<String> flowsToTest = new ArrayList<>();

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testallflows");
            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                logger.info("TestAllFlows property is set to True. Therefore all the flows are to be validated");
                flowsToTest = ParseConfigFile.getAllSectionNames(flowsConfigFilePath, flowsConfigFileName);
            } else {
                String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowsToValidate).split(Pattern.quote(","));
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

    private boolean verifyInvoiceLineItemForDuplicate(String flowsConfigFilePath, String flowsConfigFileName, String flowToTest, int invoiceLineItemId, CustomAssert csAssert) {

        boolean lineItemValidationStatus = false;
        String expectedResultForInvoiceLineItem = "Duplicate Line Item";

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
                if (actualResult.trim().toLowerCase().contains(expectedResultForInvoiceLineItem.trim().toLowerCase())) {
                    lineItemValidationStatus = true;
                } else {

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

    private boolean checkInvoiceValidationInFilterAndColumn(String url, CustomAssert customAssert) {

        boolean foundInListHeaders = false, foundInFilters = false;
        try {

            APIUtils apiUtils = new APIUtils();
            HttpPost httpPost = apiUtils.generateHttpPostRequestWithQueryString(url, "application/json, text/javascript, */*; q=0.01", "application/json;charset=UTF-8");
            HttpResponse httpResponse = APIUtils.postRequest(httpPost, "{}");
            String response = EntityUtils.toString(httpResponse.getEntity());

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("filterMetadatas");

            String invoiceValidationStatus = "Invoice Validation Status";

            logger.info("Testing with url {}", url);
            //checking validation in list header json array
            for (int index = 0; index < jsonArray.length(); index++) {
                if (jsonArray.getJSONObject(index).get("name").toString().equalsIgnoreCase(invoiceValidationStatus)
                        || jsonArray.getJSONObject(index).get("defaultName").toString().equalsIgnoreCase(invoiceValidationStatus)) {
                    foundInFilters = true;
                    break;
                }
            }
            //checking validation in filter json array
            jsonArray = jsonObject.getJSONArray("columns");
            for (int index = 0; index < jsonArray.length(); index++) {
                if (jsonArray.getJSONObject(index).get("name").toString().equalsIgnoreCase(invoiceValidationStatus)
                        || jsonArray.getJSONObject(index).get("defaultName").toString().equalsIgnoreCase(invoiceValidationStatus)) {
                    foundInListHeaders = true;
                    break;
                }
            }

            logger.info("foundInFilters [{}] : foundInListHeaders [{}]", foundInFilters, foundInListHeaders);

        } catch (Exception e) {
            logger.info("Exception caught in checkInvoiceValidationInFilterAndColumn()");
            customAssert.assertTrue(false,"Exception caught in checkInvoiceValidationInFilterAndColumn()");
        }

        return foundInFilters & foundInListHeaders;
    }

    private boolean checkInvoiceValidationInExcel(String url, CustomAssert customAssert) {

        boolean foundInListHeaders = false, foundInFilters = false, foundInExcel = false;
        try {

            APIUtils apiUtils = new APIUtils();
            HttpPost httpPost = apiUtils.generateHttpPostRequestWithQueryString(url, "application/json, text/javascript, */*; q=0.01", "application/json;charset=UTF-8");
            HttpResponse httpResponse = APIUtils.postRequest(httpPost, null);
            String response = EntityUtils.toString(httpResponse.getEntity());

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("filterMetadatas");

            String invoiceValidationStatus = "Invoice Validation Status";

            logger.info("Testing with url {}", url);
            //checking validation in list header json array
            for (int index = 0; index < jsonArray.length(); index++) {
                if (jsonArray.getJSONObject(index).get("name").toString().equalsIgnoreCase(invoiceValidationStatus)
                        || jsonArray.getJSONObject(index).get("defaultName").toString().equalsIgnoreCase(invoiceValidationStatus)) {
                    foundInFilters = true;
                    break;
                }
            }
            //checking validation in filter json array
            jsonArray = jsonObject.getJSONArray("columns");
            for (int index = 0; index < jsonArray.length(); index++) {
                if (jsonArray.getJSONObject(index).get("name").toString().equalsIgnoreCase(invoiceValidationStatus)
                        || jsonArray.getJSONObject(index).get("defaultName").toString().equalsIgnoreCase(invoiceValidationStatus)) {
                    foundInListHeaders = true;
                    break;
                }
            }
            //checking validation in excel json array
            jsonArray = jsonObject.getJSONArray("ecxelColumns");
            for (int index = 0; index < jsonArray.length(); index++) {
                if (jsonArray.getJSONObject(index).get("name").toString().equalsIgnoreCase(invoiceValidationStatus)
                        || jsonArray.getJSONObject(index).get("defaultName").toString().equalsIgnoreCase(invoiceValidationStatus)) {
                    foundInExcel = true;
                    break;
                }
            }

            logger.info("foundInFilters [{}] : foundInListHeaders [{}] : foundInExcel [{}]", foundInFilters, foundInListHeaders, foundInExcel);

        } catch (Exception e) {
            logger.info("Exception caught in checkInvoiceValidationInFilterAndColumn()");
            customAssert.assertTrue(false,"Exception caught in checkInvoiceValidationInFilterAndColumn()");
        }

        return (foundInFilters & foundInListHeaders)&foundInExcel;
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

