package com.sirionlabs.test.invoice.SanityFlowForSandbox;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.entityWorkflowActions.EntityWorkFlowAction;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
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

public class InvoiceSanityFlow {
    private final static Logger logger = LoggerFactory.getLogger(com.sirionlabs.test.listing.TestListing.class);
    private String configFilePath;
    private String configFileName;
    private String customFieldSectionName = "custom field name";
    private Map<Integer, String> deleteEntityMap = new HashMap<>();
    private String flowsConfigFilePath;
    private String flowsConfigFileName;
    private String pricingTemplateFilePath;
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
    private boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private String contractEntity = "contracts";
    private String serviceDataEntity = "service data";
    private String consumptionEntity = "consumptions";
    Object jsonNodeValue;

    @AfterMethod
    public void afterMethodForDelete() {
        for (Map.Entry<Integer, String> me : deleteEntityMap.entrySet()) {
            deleteNewEntity(me.getValue(), me.getKey());
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

    @BeforeClass
    public void BeforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceSanityFlowConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceSanityFlowConfigFileName");

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

        String entityIdMappingFileName;
        String baseFilePath;

        // for publishing of service data
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));


        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            failTestIfJobNotCompletedWithinSchedulerTimeOut = false;

        deleteEntityMap = new HashMap<>();
    }

    @DataProvider
    public Object[][] DataProviderForInvoiceSanity() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest();
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(enabled = true, dataProvider = "DataProviderForInvoiceSanity")
    public void InvoiceSanity(String flowToTest) {
        CustomAssert customAssert = new CustomAssert();
        int filterListId = 358;

        String serviceDataSectionNames = "default";
        int contractId;
        String contractSectionName = "default";

        try {
            synchronized (this) {

                String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractsectionname");
                if (temp != null)
                    contractSectionName = temp.trim();

                contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);

                if (contractId != -1) {
                    logger.info("Using Contract Id {} for the Flow [{}]", contractId, flowToTest);
                    deleteEntityMap.put(contractId, contractEntity);

                    InvoiceHelper.updateMultipleServiceDataAndInvoiceConfigDistinct(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName,
                            ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname"),
                            ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname"), contractId);

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionNames = temp.trim();

                    String[] serviceDataSectionNameList = serviceDataSectionNames.split(",");

                    List<Integer> billingData = new ArrayList<>();

                    String serviceDataType = "fixedFee";

                    // getting the actual service data Type
                    if (ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                            "servicedatatype") != null) {
                        serviceDataType = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                "servicedatatype");
                    }

                    try {
                        int index = 0;
                        for (String serviceDataSectionName : serviceDataSectionNameList) {
                            billingData.addAll(Objects.requireNonNull(createBillingData(flowToTest, serviceDataType.split(",")[index], customAssert, serviceDataSectionName, contractId)));
                            index++;
                        }
                    } catch (NullPointerException e) {
                        logger.error("Number of service data and type of service data does not match");
                    }

                    InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest, billingData);

                    ListRendererListData listRendererListData = new ListRendererListData();
                    JSONObject jsonObject;
                    List<Integer> lineItems = new ArrayList<>();
                    int maxAllottedTime = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waittimeforlineitemgeneration"));
                    int timeElapsed = 0;
                    int sleepTime = 5000;
                    ParseJsonResponse parseJsonResponse = new ParseJsonResponse();

                    while (timeElapsed < maxAllottedTime) {
                        listRendererListData.hitListRendererListData(filterListId
                                , getFilterPayload(ParseConfigFile.getValueFromConfigFile(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionNames, "sourcename")
                                        , Integer.parseInt(ParseConfigFile.getValueFromConfigFile(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionNames, "sourceid"))));
                        String response = listRendererListData.getListDataJsonStr();
                        jsonObject = new JSONObject(response);
                        try {
                            if (jsonObject.getInt("filteredCount") > 0) {
                                for (int index = 0; index < jsonObject.getJSONArray("data").length(); index++) {
                                    parseJsonResponse.getNodeFromJsonWithValue(jsonObject.getJSONArray("data").getJSONObject(index), Collections.singletonList("columnName"), "id");
                                    int tempLineItemId = Integer.parseInt(((String) parseJsonResponse.getJsonNodeValue()).split(":;")[1]);
                                    lineItems.add(tempLineItemId);
                                }
                                break;
                            }
                        } catch (Exception e) {
                            logger.error("Exception caught in parsing the json response for filtered data {}", (Object) e.getStackTrace());
                            break;
                        }
                        logger.info("Putting the thread on sleep for {}", sleepTime);
                        Thread.sleep(sleepTime);
                        logger.info("Time spent {}", timeElapsed);
                        timeElapsed += 5000;
                    }
                    logger.info("Line items {}", lineItems);

                    if (lineItems.size() == 0) {
                        customAssert.assertTrue(false, "Line items not formed, hence test cases failed.");
                    }

                    for (int lineItem : lineItems) {
                        boolean verification = InvoiceHelper.verifyInvoiceLineItem(configFilePath, configFileName, flowToTest, lineItem, customAssert);
                    }

                } else {
                    logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                    //customAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                    customAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
            //customAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
            customAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
        }
        customAssert.assertAll();
    }

//        @Test
//        public void test() throws InterruptedException {
//            String flowToTest = "fixed fee flow for sanity";
//            List<Integer> billingData = new ArrayList<>(List.of(1120337,1120332));
//            InvoiceHelper.getInvoiceId(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,flowToTest,billingData);
//            //logger.info("invoice id {}",invoiceId);
//
//            ListRendererListData listRendererListData = new ListRendererListData();
//            JSONObject jsonObject = new JSONObject();
//            List<Integer> lineItems = new ArrayList<>();
//
//            while(true) {
//                listRendererListData.hitListRendererListData(10, getFilterPayload("Test_Automation", 12078));
//                String response = listRendererListData.getListDataJsonStr();
//                jsonObject = new JSONObject(response);
//                try{
//                    if(jsonObject.getInt("filteredCount")>0){
//                        for(int index=0;index<jsonObject.getJSONArray("data").length();index++){
//                            lineItems.add(Integer.valueOf(jsonObject.getJSONArray("data").getJSONObject(index).getJSONObject("203").getString("value").split(":;")[1]));
//                        }
//                        break;
//                    }
//                }
//                catch (Exception e){
//                    logger.error("Exception caught in parsing the json response for filtered data {}", (Object) e.getStackTrace());
//                    break;
//                }
//                Thread.sleep(5000);
//            }
//            logger.info("Line items {}",lineItems);
//
//
//        }

    private List<Integer> createBillingData(String flowToTest, String serviceDataType, CustomAssert customAssert, String serviceDataSectionName, int contractId) {

        InvoiceHelper invoiceHelper = new InvoiceHelper();
        List<Integer> consumptionIds;
        String publishAction = "publish";
        String approveAction = "approve";
        try {

            InvoicePricingHelper pricingObj = new InvoicePricingHelper();

            int serviceDataId = InvoiceHelper.getServiceDataIdForDifferentClientAndSupplierId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, contractId);
            logger.info("Created Service Data Id : [{}]", serviceDataId);
            if (serviceDataId != -1) {
                logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                deleteEntityMap.put(serviceDataId, serviceDataEntity);

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                boolean uploadPricing = true;
                String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "uploadPricing");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    uploadPricing = false;


                if (uploadPricing) {
                    String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                            "pricingstemplatefilename" + serviceDataType.toLowerCase());
//                            boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);
                    boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, customAssert);
                    // changes for ARC RRC FLOW
                    if (pricingFile) {


                        if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

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
                                        customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                                flowToTest + "]");
                                        customAssert.assertAll();
                                        return null;
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
                                        return null;
                                    } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                                    {


                                        boolean isDataCreatedUnderChargesTab = isChargesCreated(serviceDataId);

                                        if (!isDataCreatedUnderChargesTab) {
                                            customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                    flowToTest + "]");
                                            customAssert.assertAll();
                                            return null;
                                        }


                                        if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                            boolean isDataCreatedUnderARCRRCTab = isARCRRCCreated(serviceDataId);


                                            if (!isDataCreatedUnderARCRRCTab) {
                                                customAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                                        flowToTest + "]");
                                                customAssert.assertAll();
                                                return null;
                                            }
                                        }
                                    }

                                    // forecast flow : Start


                                    if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                                    {
                                        boolean result = new WorkflowActionsHelper().performWorkflowAction(serviceDataEntityTypeId,serviceDataId,publishAction);
                                        if (!result) {
                                            logger.info("Could not perform publish action for the service data {}, still continuing", serviceDataId);
                                            result = true;
                                        }
                                        // if service data got published
                                        if (result) {

                                            // function to get status whether consumptions have been created or not
                                            String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId);//waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
                                            logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                                            if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                                logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                                customAssert.assertAll();
                                                return null;
                                            } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                                logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                                                customAssert.assertAll();
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
                                                        customAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                                                "Hence skipping validation");
                                                        customAssert.assertAll();
                                                        return null;

                                                    } else {
                                                        if (flowToTest.contains("arc")) {
                                                            result = new WorkflowActionsHelper().performWorkflowAction(ConfigureConstantFields.getEntityIdByName(consumptionEntity),consumptionIds.get(i),approveAction);
                                                            //result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds.get(i), consumptionEntity, consumptionEntitySectionUrlName);
                                                            customAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptionEntity + "having id : " + consumptionIds.get(i));

                                                            if (!result) {
                                                                logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
                                                                customAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
                                                                        "Hence skipping validation");
                                                                customAssert.assertAll();
                                                                return null;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            //Consumption end
                                        } else {

                                            logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
//												customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
//														"Hence skipping validation");
                                            customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                    "Hence skipping validation");
                                            //customAssert.assertAll();
                                            customAssert.assertAll();
                                            return null;
                                        }
                                    }

                                }
                            } else {
                                logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                        pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);
//									customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
//											pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
                                customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                        pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
//									customAssert.assertAll();
                                customAssert.assertAll();
                                return null;
                            }
                        } else {
                            logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
//								customAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                            customAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                    "Hence skipping validation");
//								customAssert.assertAll();
                            customAssert.assertAll();

                            return null;

                        }

                    } else {
                        logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
//							customAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
//									"Hence skipping validation");
                        customAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                                "Hence skipping validation");
//							customAssert.assertAll();
                        customAssert.assertAll();
                        return null;
                    }
                } else {
                    logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
                }


                List<Integer> billingIds = new InvoiceHelper().getBillingDataIds(configFilePath, configFileName, contractId, serviceDataId);

                if (billingIds.isEmpty()) {
                    customAssert.assertTrue(false, "Billing data not formed hence terminating execution");
                    customAssert.assertAll();
                }

                logger.info("Billing data {}", billingIds.toString());

                return billingIds;

            } else {
                logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                //customAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                customAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
            }
        } catch (Exception e) {
            logger.info("Exception Caught in createBillingData() [{}]", (Object) e.getStackTrace());
        }

        return null;
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

    private String getFilterPayload(String contractName, int contractId) {
        return "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId + "\",\"name\":\"" + contractName + "\"}]},\"filterId\":2,\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":203,\"columnQueryName\":\"id\"},{\"columnId\":205,\"columnQueryName\":\"name\"}]}";
    }

    private List<String> getFlowsToTest() {
        List<String> flowsToTest = new ArrayList<>();

        try {
            String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstovalidate").split(Pattern.quote(","));
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

}
