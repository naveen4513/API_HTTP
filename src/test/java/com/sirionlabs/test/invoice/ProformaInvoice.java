package com.sirionlabs.test.invoice;

import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.invoice.InvoiceReValidationCheck;
import com.sirionlabs.api.listRenderer.ListRendererConfigure;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

public class ProformaInvoice extends TestAPIBase {

    private static Logger logger = LoggerFactory.getLogger(ProformaInvoice.class);
    private int entityTypeId = 67;private int consumptionEntityTypeId = 176;
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
    private String waitTimeSectionNameForBillingData = "waittimeforbillingdatageneration";

    @AfterMethod
    public void afterMethodForDelete() {
        for (Map.Entry<Integer, String> me : deleteEntityMap.entrySet()) {
            deleteNewEntity(me.getValue(), me.getKey());
        }
    }

    @BeforeClass
    public void BeforeClass() {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ProformaInvoiceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ProformaInvoiceConfigFileName");
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

//    @Test(enabled = false, dataProvider = "dataProviderForCheckValidationInvoiceCreationRule") //C88376,C76646,C76644
//    public void checkValidationInvoiceCreationRule(String fieldName1, String invoiceCustomFieldCreatePayloadSectionName1, String fieldName2, String invoiceCustomFieldCreatePayloadSectionName2, String result, String formula, String ruleCreatePayload) {
//
//        CustomAssert customAssert = new CustomAssert();
//        try {
//            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());
//
//            String customFieldName1 = ParseConfigFile.getValueFromConfigFileCaseSensitive(ConfigureConstantFields.getConstantFieldsProperty("ProformaInvoiceConfigFilePath"), ConfigureConstantFields.getConstantFieldsProperty("ProformaInvoiceConfigFileName"), "field name mapping", fieldName1);
//            String customFieldName2 = ParseConfigFile.getValueFromConfigFileCaseSensitive(ConfigureConstantFields.getConstantFieldsProperty("ProformaInvoiceConfigFilePath"), ConfigureConstantFields.getConstantFieldsProperty("ProformaInvoiceConfigFileName"), "field name mapping", fieldName2);
//
//            boolean checkField1Creation = checkCreateCustomField(customFieldName1, invoiceCustomFieldCreatePayloadSectionName1, customAssert);
//            boolean checkField2Creation = checkCreateCustomField(customFieldName2, invoiceCustomFieldCreatePayloadSectionName2, customAssert);
//
//            if (!checkField1Creation || !checkField2Creation) {
//                logger.info("Custom Field not created successfully after checkCreateCustomField() method");
//                customAssert.assertTrue(false, "Custom Field not created successfully after checkCreateCustomField() method");
//            }
//
//            InvoiceCreateRule invoiceCreateRule = new InvoiceCreateRule();
//            String ruleName = "newRuleAutomation" + RandomNumbers.getRandomNumberWithinRangeIndex(10000, 99999);
//            String rule = "${" + customFieldName1 + "}@{" + formula + customFieldName2 + "}";
//
//            logger.info("Setting invoice create rule payload");
//            invoiceCreateRule.setCreatePayload(ruleName, rule, ruleCreatePayload, customAssert);
//
//            logger.info("creating invoice rule");
//            invoiceCreateRule.createRule(customAssert); //creating invoice rule
//            String createResponse = invoiceCreateRule.getCreateResponse();
//
//            if (createResponse.contains("Created Successfully")) {
//                logger.info("Invoice rule created successfully - {} {}", ruleName, rule);
//                if (result.equalsIgnoreCase("success"))
//                    customAssert.assertTrue(true, "Invoice rule created - " + ruleName + " " + rule);
//                else
//                    customAssert.assertTrue(false, "Invoice rule created - " + ruleName + " " + rule);
//            } else {
//                logger.info("Invoice rule not created - {} {}", ruleName, rule);
//                if (result.equalsIgnoreCase("failure"))
//                    customAssert.assertTrue(true, "Invoice rule not created - " + ruleName + " " + rule);
//                else
//                    customAssert.assertTrue(false, "Invoice rule not created - " + ruleName + " " + rule);
//            }
//
//        } catch (Exception e) {
//            logger.info("Exception caught while creating rule");
//            customAssert.assertTrue(false, "Exception caught while creating rule");
//        }
//
//        customAssert.assertAll();
//
//    }

    @DataProvider()
    public Object[][] dataProviderForProformaInvoice() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest("flowstovalidate");
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(enabled = false) //C76648 - todo priority p2
    public void checkTabListConfigurationOfSelectInvoiceLineItems() {
        CustomAssert customAssert = new CustomAssert();
        String urlId = "445";
        try {
            new AdminHelper().loginWithClientAdminUser();

            ListRendererConfigure listRendererConfigure = new ListRendererConfigure();
            listRendererConfigure.hitListRendererConfigure(urlId, customAssert);
            String response = listRendererConfigure.getListRendererConfigureJsonStr();
            JSONObject jsonObject = new JSONObject(response);

            int filterCount = jsonObject.getJSONArray("filterMetadatas").length();
            int columnCount = jsonObject.getJSONArray("columns").length();
            List<Integer> randomDataToManipulate = getRandomNumbersForList(filterCount);
            logger.info("Random numbers generated are {}", randomDataToManipulate);
            JSONArray jsonObjectForFilter = jsonObject.getJSONArray("filterMetadatas");

            //getting order id for changed order
            int order1, order2;

            //getting random text to be concatenated with name
            String randomTextToBeAdded = String.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 999));
            String changedFilterName1, changedFilterName2;

            //storing ids for future reference
            int idForFilterChanged1, idForFilterChanged2;

            //getting details for first filter field
            order1 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).getInt("order");
            idForFilterChanged1 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).getInt("id");
            changedFilterName1 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).getString("defaultName") + randomTextToBeAdded;
            logger.info("First filter item to be updated is {}", changedFilterName1);

            //getting details for second data field
            order2 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).getInt("order");
            idForFilterChanged2 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).getInt("id");
            changedFilterName2 = jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).getString("defaultName") + randomTextToBeAdded;
            logger.info("Second filter item to be updated is {}", changedFilterName2);

            jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).put("order", order2);
            jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).put("order", order1);
            jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(0)).put("name", changedFilterName1);
            jsonObjectForFilter.getJSONObject(randomDataToManipulate.get(1)).put("name", changedFilterName2);

            jsonObject.put("filterMetadatas", jsonObjectForFilter);

            listRendererConfigure = new ListRendererConfigure();
            listRendererConfigure.updateReportListConfigureResponse(445, jsonObject.toString(), customAssert);

        } catch (Exception e) {
            logger.info("Exception Caught {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception Caught " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = true, dataProvider = "dataProviderForProformaInvoice")
    public void proformaInvoice(String flowToTest) throws ConfigurationException {

        String publishAction = "Publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        CustomAssert csAssert = new CustomAssert();

        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int selectBillingDataListId = 445;
        int contractId;
        int serviceDataId;
        int invoiceId = -1;
        int invoiceLineItemId = -1;
        List<Integer> consumptionIds;

        String contractSectionName = "default";
        String serviceDataSectionName = "default";
        String invoiceSectionName = "default";
        String invoiceLineItemSectionName = "default";

        String columnIdForServiceDataInSelectLineItemList = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"columnIdForServiceDataInSelectLineItemList");
        String columnIdForIdInSelectLineItemList = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"columnIdForIdInSelectLineItemList");

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

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                    if (temp != null)
                        invoiceSectionName = temp.trim();

                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, serviceDataSectionName, contractId);
                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, contractId);

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


                                boolean tempBool = (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) && !flowToTest.equals("fixed fee flow for no arc/rrc");
                                if (tempBool) {

                                    pricingFile = InvoiceHelper.editPricingFileForARCRRC(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId);//editPricingFileForARCRRC(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);


                                }
                                // changes for ARC RRC FLOW Ends here


                                if (pricingFile) {


                                    String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                    if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                                        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                            //Wait for Pricing Scheduler to Complete
                                            // pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);
                                            String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

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


                                                boolean isDataCreatedUnderChargesTab = new InvoiceHelper().isChargesCreated(serviceDataId);

                                                if (!isDataCreatedUnderChargesTab) {
                                                    csAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                            flowToTest + "]");
                                                    csAssert.assertAll();
                                                    return;
                                                }


                                                if (tempBool) {

                                                    boolean isDataCreatedUnderARCRRCTab = new InvoiceHelper().isARCRRCCreated(serviceDataId);


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

                                                    boolean editAndUploadForecast = new InvoiceHelper().editAndUploadForecastSheet(configFilePath,configFileName,forecastTemplateFilePath, forecastTemplateFileName, flowToTest, "newClient" + contractId, contractId);

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
//                                                boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataId, serviceDataEntity, serviceDataEntitySectionUrlName);
                                                boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, csAssert);
                                                // if service data got published
                                                if (result) {

                                                    // function to get status whether consumptions have been created or not
                                                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId);//waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
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

                                                                csAssert.assertAll();
                                                                return;

                                                            } else {
                                                                if (!(flowToTest.equals("arc flow 7") || flowToTest.equals("forecast flow 5")
                                                                        || flowToTest.equals("vol pricing flow consumption unapproved"))) {
                                                                    result = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId,consumptionIds.get(i),approveAction,csAssert);
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

                        int waitTimeForBillingData = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,waitTimeSectionNameForBillingData));

                        List<Integer> billingIds = new ArrayList<>();

                        ListRendererTabListData selectBillingIds = new ListRendererTabListData();
                        JSONObject jsonObject = new JSONObject();

                        int timeElapsed = 0;
                        int length = 0;

                        logger.info("Wait time logged for billing data generation is {}",waitTimeForBillingData);

                        while (length==0&&timeElapsed<waitTimeForBillingData) {

                            selectBillingIds.hitListRendererTabListData(selectBillingDataListId, 61, contractId, selectBillingIds.getPayload(serviceDataEntityTypeId, 0, 20, "id", "asc", "{}"));
                            String response = selectBillingIds.getTabListDataJsonStr();
                            jsonObject = new JSONObject(response);
                            length = jsonObject.getJSONArray("data").length();

                            timeElapsed += 10000;
                            logger.info("Time elapsed while waiting for billing data {}",timeElapsed);
                            Thread.sleep(10000);
                        }
                        logger.info("length of billing data is {}",length);


                        if(length==0 && timeElapsed >= waitTimeForBillingData){
                            logger.info("wait time for billing data is over. Billing data not generated hence terminating the test case.");
                            csAssert.assertTrue(false,"wait time for billing data is over. Billing data not generated hence terminating the test case");
                            csAssert.assertAll();
                        }

                        int index=-1;
                        while(++index<length){
                            if(jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject(columnIdForServiceDataInSelectLineItemList).getString("value").contains(String.valueOf(serviceDataId)))
                                billingIds.add(Integer.parseInt(jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject(columnIdForIdInSelectLineItemList).getString("value").split(":;")[0]));
                        }

                        logger.info("Billing data {}",billingIds.toString());

                        logger.info("Hitting Fetch API.");
                        fetchObj = new Fetch();
                        fetchObj.hitFetch();
                        allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                        boolean invoiceIdBoolean = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceSectionName, billingIds);

                        assert invoiceIdBoolean:"Invoice create submission request through proforma is failed";

                        logger.info("Checking for validation of the uploaded file");
                        int schedulerTimeOut = 600000;
                        int pollingTime = 5000;
                        String result = "pass";
                        logger.info("Time Out for Invoice Create Scheduler is {} milliseconds", schedulerTimeOut);
                        long timeSpent = 0;
                        logger.info("Hitting Fetch API.");
                        fetchObj = new Fetch();
                        fetchObj.hitFetch();
                        logger.info("Getting Task Id of Invoice Create Job");
                        int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
                        String newRequestId = null;

                        fetchObj = new Fetch();
                        fetchObj.hitFetch();
                        if (newTaskId != -1) {

                            jsonObject = new JSONObject(fetchObj.getFetchJsonStr());
                            JSONArray jsonArray = jsonObject.getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");
                            for (Object object : jsonArray) {
                                JSONObject jsonObject1 = (JSONObject) object;
                                if (jsonObject1.getInt("id") == newTaskId) {
                                    newRequestId = String.valueOf(jsonObject1.getInt("requestId"));
                                    break;
                                }
                            }


                            boolean taskCompleted = false;
                            logger.info("Checking if Invoice Creation Task has completed or not.");

                            while (timeSpent < schedulerTimeOut) {
                                logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                                Thread.sleep(pollingTime);

                                logger.info("Hitting Fetch API.");
                                fetchObj.hitFetch();
                                logger.info("Getting Status of Invoice Create Task.");
                                String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                                if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                                    taskCompleted = true;
                                    logger.info("Invoice Create Task Completed. ");
                                    logger.info("Checking if Invoice Create Task failed or not.");
                                    if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                                        result = "fail";

                                    break;
                                } else {
                                    timeSpent += pollingTime;
                                    logger.info("Invoice Create Task is not finished yet.");
                                }
                            }
                            if (!taskCompleted && timeSpent >= schedulerTimeOut) {
                                //Task didn't complete within given time.
                                result = "skip";
                            }
                        } else {
                            logger.info("Couldn't get Invoice Create Task Job Id. Hence waiting for Task Time Out i.e. {}", schedulerTimeOut);
                            Thread.sleep(schedulerTimeOut);
                        }

                        logger.info("The Invoice create task status is {}", result);

                        String invoiceResult= ParseConfigFile.getValueFromConfigFile(configFilePath,flowsConfigFileName,flowToTest,"invoicecreation");

                        assert invoiceResult!=null:"Invoice creation result expected value is null";
                        assert invoiceResult.length()!=0:"Invoice creation result expected value is empty";

                        assert result.equalsIgnoreCase(invoiceResult.trim()):"Invoice creation result is : "+result;

                        if(!invoiceResult.equals("pass")){
                            logger.info("Invoice creation result is not pass, hence terminating the method");
                            return;
                        }

                        ListRendererListData listRendererListData = new ListRendererListData();
                        //HttpResponse listDataResponse = listRendererListData.hitListRendererListData(64, 0, 20, "id", "desc nulls last", 352, getFilterJSonForServiceDataListing(contractId, contractName));

                        int haltTime = 10000;
                        int checkCount = 100;
                        String payload = getInvoiceListingPayloadForContract(contractId);
                        JSONObject jsonObjectForInvoiceListing;
                        String listingInvoice="";
                        while (checkCount > 0) {
                            logger.info("Checking if invoice is created for contract {}", contractId);
                            listRendererListData.hitListRendererListData(10, payload);
                            listingInvoice = listRendererListData.getListDataJsonStr();
                            jsonObjectForInvoiceListing = new JSONObject(listingInvoice);
                            if (jsonObjectForInvoiceListing.getJSONArray("data").length() > 0)
                                break;
                            checkCount--;
                            logger.info("Halting 5 seconds to check if invoice is created for contract {}, remaining count {}", contractId, checkCount);
                            Thread.sleep(haltTime);
                        }


                        jsonObjectForInvoiceListing = new JSONObject(listingInvoice);
                        if (jsonObjectForInvoiceListing.has("data")) {
                            assert jsonObjectForInvoiceListing.getJSONArray("data").length() != 0 :
                                    "No service data found in the listing after filter applied for the created contract";
                        }

                        String columnIdInvoice = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"columnIdInvoice");
                        columnIdInvoice=columnIdInvoice==null?"203":columnIdInvoice;
                        invoiceId = Integer.parseInt(jsonObjectForInvoiceListing.getJSONArray("data").getJSONObject(0).getJSONObject(columnIdInvoice).getString("value").split(":;")[1]);

                        logger.info("Created Invoice Id is : [{}]", invoiceId);
                        if (invoiceId != -1) {
                            //Get Invoice Line Item

                            payload = getInvoiceLineItemListingPayloadForInvoice(invoiceId);
                            listRendererListData.hitListRendererListData(358, payload);
                            String listingInvoiceLineItem = listRendererListData.getListDataJsonStr();
                            JSONObject jsonObjectForInvoiceLineItemListing = new JSONObject(listingInvoiceLineItem);

                            if (jsonObjectForInvoiceLineItemListing.has("data")) {
                                assert jsonObjectForInvoiceLineItemListing.getJSONArray("data").length() != 0 :
                                        "No service data found in the listing after filter applied for the created contract";
                            }

                            String columnIdInvoiceLineItem = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"columnIdInvoiceLineItem");
                            columnIdInvoiceLineItem=columnIdInvoiceLineItem==null?"13882":columnIdInvoiceLineItem;
                            invoiceLineItemId = Integer.parseInt(jsonObjectForInvoiceLineItemListing.getJSONArray("data").getJSONObject(0).getJSONObject(columnIdInvoiceLineItem).getString("value").split(":;")[1]);

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
                assert verifyInvoiceLineItem:"Line item verification failed at last step";

                Show lineItemShow = new Show();
                lineItemShow.hitShow(165,invoiceLineItemId);
                String lineItemShowResponse = lineItemShow.getShowJsonStr();
                assert ParseJsonResponse.validJsonResponse(lineItemShowResponse):"Show response of line item is not a valid json";
                JSONObject lineItemShowJson = new JSONObject(lineItemShowResponse);

                assert lineItemShowJson.getJSONObject("body").getJSONObject("data").getJSONObject("conversionRate").has("values"):"Line item doesn't have conversion rate";
                assert lineItemShowJson.getJSONObject("body").getJSONObject("data").getJSONObject("conversionRate").get("values") instanceof Number:"Line item doesn't have conversion rate integer value";

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

    @Test(enabled = false)
    public void C89196(){
        String actionNameReject = "reject";
        String publishAction = "publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        CustomAssert csAssert = new CustomAssert();
        String flowToTest = "fixed fee flow 1";
        // Checking the arguments
        System.out.println("***************************************");
        System.out.println(flowToTest);
        String serviceDataType = "fixedFee"; //default Value for making backward compatible
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        int contractId ;
        int serviceDataId =-1;
        int invoiceId =-1;
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

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname");
                    if (temp != null)
                        invoiceSectionName = temp.trim();

                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath,serviceDataConfigFileName,invoiceConfigFilePath,invoiceConfigFileName,serviceDataSectionName,contractId);
                    InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath,serviceDataConfigFileName,invoiceConfigFilePath,invoiceConfigFileName,invoiceSectionName,contractId);

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


                                if (pricingFile) {


                                    String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                    if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
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


                                                boolean isDataCreatedUnderChargesTab = new InvoiceHelper().isChargesCreated(serviceDataId);

                                                if (!isDataCreatedUnderChargesTab) {
                                                    csAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                            flowToTest + "]");
                                                    addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                    csAssert.assertAll();
                                                    return;
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
                        } else {
                            logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
                        }


                        //Get Invoice Id
                        //invoiceId = getInvoiceId(flowToTest);

                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
                        if (temp != null)
                            invoiceLineItemSectionName = temp.trim();
                        invoiceId=InvoiceHelper.getInvoiceId(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceSectionName,invoiceLineItemSectionName);

                        logger.info("Created Invoice Id is : [{}]", invoiceId);
                        if (invoiceId != -1) {
                            //Get Invoice Line Item Id
                            //invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataId);
                            invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,invoiceLineItemSectionName,serviceDataId);
                            logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                            deleteEntityMap.put(invoiceId,invoiceEntity);
                        } else {
                            logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                            csAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
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
            if (invoiceLineItemId != -1) {
                deleteEntityMap.put(invoiceLineItemId,invoiceLineItemEntity);

                boolean reValidationOption = InvoiceReValidationCheck.revalidateInvoice(invoiceId,csAssert);
                assert !reValidationOption:"Re-validation performed successfully on a newly created invoice";

                logger.info("Hitting Fetch API. for uploading pricing second time");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();

                //changing rates and quantity in pricing data to be uploaded
                UpdateFile.updateConfigFileProperty(flowsConfigFilePath,flowsConfigFileName,flowToTest,"volumecolumnvalues",
                        ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,flowToTest,"volumecolumnvalues")+"1");
                UpdateFile.updateConfigFileProperty(flowsConfigFilePath,flowsConfigFileName,flowToTest,"ratecolumnvalues",
                        ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath,flowsConfigFileName,flowToTest,"ratecolumnvalues")+"1");

                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
                //Uploading the pricing file again
                String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                        "pricingstemplatefilename");
                boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath,flowsConfigFileName,pricingTemplateFilePath,pricingTemplateFileName,flowToTest,serviceDataId,pricingObj,csAssert);

                if (pricingFile) {


                    // getting the actual service data Type
                    if (ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                            "servicedatatype") != null) {
                        serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                "servicedatatype");
                    }

                    if (pricingFile) {


                        String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                        if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                //Wait for Pricing Scheduler to Complete
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


                                    boolean isDataCreatedUnderChargesTab = new InvoiceHelper().isChargesCreated(serviceDataId);

                                    if (!isDataCreatedUnderChargesTab) {
                                        csAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                flowToTest + "]");
                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                        csAssert.assertAll();
                                        return;
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

                reValidationOption = InvoiceReValidationCheck.revalidateInvoice(invoiceId,csAssert);
                assert reValidationOption:"Re-validation failed when new pricing is uploaded on the linked service data";

            } else {
                logger.error("Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                csAssert.assertTrue(false, "Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [" +
                        flowToTest + "]");
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
        }
        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
        csAssert.assertAll();
    }

    private List<Integer> getRandomNumbersForList(int sizeofList) {
        int lowerLimit = RandomNumbers.getRandomNumberWithinRangeIndex(0, sizeofList - 1);
        int upperLimit = RandomNumbers.getRandomNumberWithinRangeIndex(0, sizeofList);
        if (lowerLimit == upperLimit)
            getRandomNumbersForList(sizeofList);
        return Arrays.asList(lowerLimit, upperLimit);
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

    String getInvoiceListingPayloadForContract(int contractId){
        Show contractShow = new Show();
        contractShow.hitShow(61, contractId);
        String contractName = ShowHelper.getContractNameFromShowResponse(contractShow.getShowJsonStr(), 61);

//        return "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\""+contractId+"\",\"name\":\""+contractName+"\"}]},\"filterId\":2,\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":203,\"columnQueryName\":\"id\"}]}";
        return "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId + "\",\"name\":\"Automation_DefaultText8758544\"}]},\"filterId\":2,\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"157\":{\"filterId\":\"157\",\"filterName\":\"invoicePeriodStartDate\",\"start\":\"01-01-1901\",\"end\":\"12-31-2100\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}},\"158\":{\"filterId\":\"158\",\"filterName\":\"invoicePeriodEndDate\",\"start\":\"01-01-1901\",\"end\":\"12-31-2100\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}}},\"selectedColumns\":[{\"columnId\":203,\"columnQueryName\":\"id\"}]}";
    }

    String getInvoiceLineItemListingPayloadForInvoice(int invoiceId){
        Show invoiceShow = new Show();
        invoiceShow.hitShow(67, invoiceId);
        String invoiceName = ShowHelper.getContractNameFromShowResponse(invoiceShow.getShowJsonStr(), 61);

        return "{\"filterMap\":{\"entityTypeId\":165,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\""+invoiceId+"\",\"name\":\""+invoiceName+"\"}]},\"filterId\":276,\"filterName\":\"invoiceFilter\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":13882,\"columnQueryName\":\"id\"}]}";
    }

}
