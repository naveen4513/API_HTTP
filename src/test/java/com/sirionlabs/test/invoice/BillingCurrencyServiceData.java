package com.sirionlabs.test.invoice;

import com.sirionlabs.api.clientAdmin.report.ReportRendererListConfigure;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.reportRenderer.DownloadGraphWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.ForecastUploadHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Pattern;

public class BillingCurrencyServiceData extends TestAPIBase {

    private static Logger logger = LoggerFactory.getLogger(BillingCurrencyServiceData.class);
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
    public void afterTestBillingCurrencyServiceData(){
       for(Map.Entry<Integer,String> me : deleteEntityMap.entrySet()){
            deleteNewEntity(me.getValue(),me.getKey());
        }
    }

    @BeforeClass
    public void BeforeClass() {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BillingCurrencyServiceDataConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("BillingCurrencyServiceDataConfigFileName");
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

    @Test(enabled = false)//C88456
    public void checkInvoicingCurrencyPriceBookReportFiltersAndExcelColumns() {

        CustomAssert customAssert = new CustomAssert();

        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;
        String url = "";
        String invoicingCurrency ="Invoicing Currency";
        boolean setupValidationCheck;
        try {

            //Checking Client Admin
            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());
            url = "/reportRenderer/list/442/configure";
            setupValidationCheck = checkValuesInFilterColumnExcel(url, customAssert,invoicingCurrency,false,true,true);
            if (!setupValidationCheck) {
                logger.info("Invoice Validation Status not found in Client admin Filter or List Header");
                customAssert.assertTrue(false, "Invoice Validation Status not found in Client admin Filter or List Header");
                customAssert.assertAll();
            }

        } catch (Exception e) {
            logger.info("Exception while hitting {} and getting response", url);
            customAssert.assertTrue(false, "Exception while hitting " + url + " and getting response");
        }
    }

    @DataProvider
    public Object[][] dataProviderForCheckValidationsInvoicingCurrencyUpdatePage() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest("flowstovalidate");
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForCheckCurrencyConversionLineItemValidation() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest("flowstovalidateinvalidcurrency");
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForCheckValidationsInvoicingCurrencyUpdatePage",enabled = false) //C88409
    public void checkValidationsInvoicingCurrencyUpdatePage(String flowToTest){
        CustomAssert customAssert = new CustomAssert();

        try{

            String lastUserName = Check.lastLoggedInUserName;
            String lastUserPassword = Check.lastLoggedInUserPassword;

            //Login with client admin
            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(),ConfigureEnvironment.getClientAdminPassword());

            APIUtils apiUtils = new APIUtils();
            HttpGet httpGet;
            HttpResponse httpResponse;
            Document html;
            Elements elements;

            //starting create list item type
            logger.info("starting create list item type");
            httpGet = apiUtils.generateHttpGetRequestWithQueryString("/tblclients/show/1002", "");
            httpGet.addHeader("Content-Type", "text/html;charset=UTF-8");
            httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");

            httpResponse = apiUtils.getRequest(httpGet);
            String response = EntityUtils.toString(httpResponse.getEntity());
            html = Jsoup.parse(response);
            String[] clientCurrencyArray = html.getElementById("_c_com_sirionlabs_model_clientcurrencies_clientCurrencyList_id").getElementsByTag("pre").text().toString().split("\n");

            new Check().hitCheck(lastUserName,lastUserPassword);

            String publishAction = "publish";
            InvoiceHelper invoiceHelper = new InvoiceHelper();

            CustomAssert csAssert = new CustomAssert();

            String serviceDataType = "fixedFee"; //default Value for making backward compatible
            InvoicePricingHelper pricingObj = new InvoicePricingHelper();

            int contractId;
            int serviceDataId;
            List<Integer> consumptionIds;

            String contractSectionName = "default";
            String serviceDataSectionName = "default";

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

                        //checking currency on create page
                        New _new = new New();
                        _new.hitNew(serviceDataEntity,contractEntity,contractId);
                        response = _new.getNewJsonStr();
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingCurrency").getJSONObject("options").getJSONArray("data");
                        ArrayList<String> currencyList = new ArrayList<>();
                        for(int index =0;index<jsonArray.length();index++){
                            currencyList.add(jsonArray.getJSONObject(index).get("name").toString());
                        }
                        for(int index=0;index<clientCurrencyArray.length;index++){
                            if(!currencyList.contains(clientCurrencyArray[index])){
                                logger.info("{} currency doesnot exist in create page currency",clientCurrencyArray[index]);
                                customAssert.assertTrue(false,clientCurrencyArray[index]+" currency doesnot exist in create page currency");
                                break;
                            }
                        }

                        InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, contractSectionName, contractId);

                        temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                        if (temp != null)
                            serviceDataSectionName = temp.trim();

                        serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, contractId);

                        logger.info("Created Service Data Id : [{}]", serviceDataId);
                        if (serviceDataId != -1) {
                            logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                            deleteEntityMap.put(serviceDataId, serviceDataEntity);

                            Show show = new Show();
                            show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
                            response = show.getShowJsonStr();

                            JSONObject jsonObject2 = new JSONObject();
                            jsonObject2.put("name","Switzerland (CHF)");
                            jsonObject2.put("id","52");
                            jsonObject2.put("shortName","CHF");

                            jsonObject = new JSONObject(response);
                            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingCurrency").put("values",jsonObject2);
                            jsonObject.remove("session");
                            jsonObject.remove("header");
                            jsonObject.remove("actions");
                            jsonObject.remove("createLinks");
                            jsonObject.getJSONObject("body").remove("layoutInfo");
                            jsonObject.getJSONObject("body").remove("globalData");
                            jsonObject.getJSONObject("body").remove("errors");

                            response = new Edit().hitEdit(serviceDataEntity,jsonObject.toString());
                            if(!response.contains("success")){
                                logger.info("Updating the service Data [{}] failed",serviceDataId);
                                customAssert.assertTrue(false,"Updating the service Data ["+serviceDataId+"] failed");
                            }
                            else{

                                logger.info("Updating the invoicing currency after publishing service Data [{}] passed successfully",serviceDataId);
                            }

                            if(flowToTest.equalsIgnoreCase("fixed fee flow 1"))
                                customAssert.assertAll();
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

                                        if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                                            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                                //Wait for Pricing Scheduler to Complete
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
                                                            csAssert.assertAll();
                                                            return;
                                                        }
                                                    }
                                                }

                                                // forecast flow : Start
                                                if (serviceDataType.contentEquals("forecast")) {
                                                        String forecastTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                                                                "forecasttemplatefilename");

                                                        boolean editAndUploadForecast = editAndUploadForecastSheet(forecastTemplateFilePath, forecastTemplateFileName, flowToTest, "newClient" + contractId, contractId, pricingObj);

                                                        if (!editAndUploadForecast) {
                                                            logger.error("For Flow [{}] , edit and Upload Forecast sheet is failing so Skipping the further Part", flowToTest);
                                                            throw new SkipException("Skipping this test");
                                                        }
                                                }
                                                // only for forecast flow : End
                                                // Consumption Part will start here only if flow is not fixed fee

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
                                                                    result = workflowActionsHelper.performWorkflowAction(ConfigureConstantFields.getEntityIdByName(consumptionEntity),consumptionIds.get(i),approveAction);
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

                                                        show = new Show();
                                                        show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
                                                        response = show.getShowJsonStr();

                                                        jsonObject2 = new JSONObject();
                                                        jsonObject2.put("name","Indian Rupee (INR)");
                                                        jsonObject2.put("id","8");
                                                        jsonObject2.put("shortName","INR");

                                                        jsonObject = new JSONObject(response);
                                                        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingCurrency").put("values",jsonObject2);
                                                        jsonObject.remove("session");
                                                        jsonObject.remove("header");
                                                        jsonObject.remove("actions");
                                                        jsonObject.remove("createLinks");
                                                        jsonObject.getJSONObject("body").remove("layoutInfo");
                                                        jsonObject.getJSONObject("body").remove("globalData");
                                                        jsonObject.getJSONObject("body").remove("errors");

                                                        response = new Edit().hitEdit(serviceDataEntity,jsonObject.toString());

                                                        show = new Show();
                                                        show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
                                                        response = show.getShowJsonStr();
                                                        jsonObject = new JSONObject(response);
                                                        if(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingCurrency").getJSONObject("values").get("shortName").toString().equalsIgnoreCase("inr")){
                                                            logger.info("Updating the invoicing currency after publishing service Data [{}] succeeded while which have failed",serviceDataId);
                                                            customAssert.assertTrue(false,"Updating the invoicing currency after publishing service Data ["+serviceDataId+"] succeeded while which have failed");
                                                        }
                                                        else{
                                                            logger.info("Updating the invoicing currency after publishing service Data [{}] failed successfully",serviceDataId);
                                                        }


                                                        //Consumption end
                                                    } else {

                                                        logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
                                                        csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                                "Hence skipping validation");
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
            } catch (Exception e) {
                logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
                //csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
                csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
            }
        }
        catch (Exception e){
            logger.info("Exception Caught {}",e.toString());
            customAssert.assertTrue(false,"Exception Caught "+e.toString());
        }

        customAssert.assertAll();
    }

    @Test(enabled = false) //C88338
    public void priceBookCheckReportHeadersEndUser(){

       List<String> filterHeadersAdmin = new ArrayList<>();
       List<String> excelHeadersAdmin = new ArrayList<>();;
       List<String> temp = new ArrayList<>();;

        String lastUserName,lastUserPassword,url;

        CustomAssert customAssert = new CustomAssert();
        ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
        String response= "";
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try{
            lastUserName = Check.lastLoggedInUserName;
            lastUserPassword = Check.lastLoggedInUserPassword;

            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(),ConfigureEnvironment.getClientAdminPassword());
            url = "/reportRenderer/list/442/configure";
            filterHeadersAdmin = checkPriceBookReportFilter(url,customAssert,"filterMetadatas","defaultName");
            excelHeadersAdmin = checkPriceBookReportFilter(url,customAssert,"ecxelColumns","defaultName");

            temp = new ArrayList<>(filterHeadersAdmin);

            new Check().hitCheck(lastUserName,lastUserPassword);
            reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetaData(442,null,"{}");
            response = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
            jsonObject = new JSONObject(response);
            jsonArray = jsonObject.getJSONArray("filterMetadatas");

            for(int index=0;index<jsonArray.length();index++){
                if(!filterHeadersAdmin.contains(jsonArray.getJSONObject(index).get("name").toString().toLowerCase())){
                    logger.info("Cannot find {} in the filter Header List",jsonArray.getJSONObject(index).get("name").toString());
                    customAssert.assertTrue(false,"Cannot find "+jsonArray.getJSONObject(index).get("name").toString()+" in the filter Header List");
                    break;
                }
                else{
                    filterHeadersAdmin.remove(jsonArray.getJSONObject(index).get("name").toString().toLowerCase());
                }
            }

            if(filterHeadersAdmin.size()!=0){
                logger.info("Few headers are found in client admon but not found at the end user {}",filterHeadersAdmin);
                customAssert.assertTrue(false,"Few headers are found in client admon but not found at the end user " + filterHeadersAdmin);
            }
        }
        catch (Exception e){
            logger.info("Exception Caught {}",e.toString());
            customAssert.assertTrue(false,"Exception Caught "+e.toString());
        }

        try{
            //download the report
            DownloadGraphWithData downloadObj = new DownloadGraphWithData();
            String entityName = "Service data";
            int reportId = 442;
            String reportName = "Service Data - Price Book Report";
            Map<String, String> formParam = new HashMap<>();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            formParam.put("jsonData", "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}");
            formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            HttpResponse response2 = downloadObj.hitDownloadGraphWithData(formParam, reportId);
            String outputFilePath = "src/test/output";
            String outputFileName = reportName + ".xlsx";
            FileUtils fileUtil = new FileUtils();
            Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response2, outputFilePath + "/" + outputFileName);
            if (!fileDownloaded) {
                throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
            }

            List<String> allHeadersInExcelDataSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 4);
            List<String> allHeadersInExcelFilterSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Filter", 4);
            filterHeadersAdmin = new ArrayList<>(temp);

            //checking the filters in excel
            for(int index=0;index<allHeadersInExcelFilterSheet.size();index++){
                if(!filterHeadersAdmin.contains(allHeadersInExcelFilterSheet.get(index).toLowerCase())){
                    logger.info("Cannot find {} in the filter Header List in excel ",allHeadersInExcelFilterSheet.get(index).toLowerCase());
                    customAssert.assertTrue(false,"Cannot find "+allHeadersInExcelFilterSheet.get(index).toLowerCase()+" in the filter Header List in excel");
                    break;
                }
                else{
                    filterHeadersAdmin.remove(allHeadersInExcelFilterSheet.get(index).toLowerCase());
                }
            }

            if(filterHeadersAdmin.size()!=0){
                logger.info("Few headers are found in client admon but not found at the end user {}",filterHeadersAdmin);
                customAssert.assertTrue(false,"Few headers are found in client admon but not found at the end user " + filterHeadersAdmin);
            }

            //checking the columns in excel
            for(int index=0;index<allHeadersInExcelDataSheet.size();index++){
                if(!excelHeadersAdmin.contains(allHeadersInExcelDataSheet.get(index).toLowerCase()
                )){
                    logger.info("Cannot find {} in the filter Header List in excel ",allHeadersInExcelDataSheet.get(index).toLowerCase());
                    customAssert.assertTrue(false,"Cannot find "+allHeadersInExcelDataSheet.get(index).toLowerCase()+" in the filter Header List in excel");
                    break;
                }
                else{
                    excelHeadersAdmin.remove(allHeadersInExcelDataSheet.get(index).toLowerCase());
                }
            }

            if(excelHeadersAdmin.size()!=0){
                logger.info("Few headers are found in client admon but not found at the end user {}",filterHeadersAdmin);
                customAssert.assertTrue(false,"Few headers are found in client admon but not found at the end user " + filterHeadersAdmin);
            }



        }
        catch (Exception e){
            logger.info("Exception Caught {}",e.toString());
            customAssert.assertTrue(false,"Exception Caught "+e.toString());
        }

        customAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderForCheckCurrencyConversionLineItemValidation", enabled = true) //C88464  //C88463
    public void checkCurrencyConversionLineItemValidation(String flowToTest){



        String publishAction = "publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        CustomAssert csAssert = new CustomAssert();
        // Checking the argument

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

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();

                    InvoiceHelper.updateServiceDataAndInvoiceConfigDistinct(serviceDataConfigFilePath,serviceDataConfigFileName,invoiceConfigFilePath,invoiceConfigFileName,contractSectionName,serviceDataSectionName,contractId);

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
                                                WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                                                boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId,serviceDataId,publishAction);

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
                                                                    result = workflowActionsHelper.performWorkflowAction(ConfigureConstantFields.getEntityIdByName(consumptionEntity),consumptionIds.get(i),approveAction);
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

                if(flowToTest.contains("no invoicing currency"))//||flowToTest.contains("invalid currency"))
                {
                    Show show = new Show();
                    int time = 0, timeOut=60000;
                    int systemConversionRate = -1;

                    while(systemConversionRate==-1&&time<=timeOut) {
                        try{
                            show.hitShowVersion2(ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity),invoiceLineItemId);
                            String res = show.getShowJsonStr();
                            JSONObject jsonObject = new JSONObject(res);
                            systemConversionRate = Math.round(Float.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("systemConversionRate").get("values").toString()));
                        }
                        catch (Exception e){
                            logger.info("System Conversion Rate not found");
                        }
                        finally {
                            time+=5000;
                            Thread.sleep(5000);
                            logger.info("Putting thread on sleep for 5000 ms");
                            if(systemConversionRate!=-1){
                                logger.info("System Conversion Rate found");
                            }
                        }
                    }

                    if(systemConversionRate!=1){
                        logger.info("System Conversion Rate value is [{}], hence test case not cleared. Required value was 1.",systemConversionRate);
                        csAssert.assertTrue(false,"System Conversion Rate value is ["+systemConversionRate+"], hence test case not cleared. Required value was 1.");
                        csAssert.assertAll();
                    }
                    else {
                        logger.info("System Conversion Rate value ,matched [{}]",systemConversionRate);
                        csAssert.assertTrue(true,"System Conversion Rate value ,matched");
                        csAssert.assertAll();
                        return;
                    }
                }

                boolean verifyInvoiceLineItem = InvoiceHelper.verifyInvoiceLineItem(flowsConfigFilePath,flowsConfigFileName,flowToTest,invoiceLineItemId,csAssert);
                if(!verifyInvoiceLineItem){
                    csAssert.assertTrue(false,"Invoice Line item validation unsuccessful");
                    csAssert.assertAll();
                }
                else if(flowToTest.contains(" 1")){
                    Show show = new Show();
                    show.hitShowVersion2(ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity),invoiceLineItemId);
                    String res = show.getShowJsonStr();
                    JSONObject jsonObject = new JSONObject(res);
                    int systemConversionRate = Math.round(Float.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("systemConversionRate").get("values").toString()));

                    int conversionDataMatchProduct = getConversionDataFromMatrix("/conversionMatrix/getById?id=1011",csAssert,flowToTest);
                    if(conversionDataMatchProduct==-1){
                        logger.info("Conversion factor Data not matched");
                        csAssert.assertTrue(false,"Conversion factor Data not matched");
                    }
                    else{
                        if(conversionDataMatchProduct==systemConversionRate){
                            logger.info("Conversion factor matched");
                        }
                        else{
                            logger.info("Conversion factor doesnot match");
                            csAssert.assertTrue(false,"Conversion factor doesnot match");
                        }
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
        addTestResult(getTestCaseIdForMethodName("checkCurrencyConversionLineItemValidation"), csAssert);
        csAssert.assertAll();
    }

    @Test(enabled = false) //C88337
    public void PriceBookCheckFiltersAndHeadersFlowDownClientAdmin(){

        CustomAssert customAssert = new CustomAssert();
        try {

            new ClientSetupHelper().loginWithClientSetupUser();
            String url = "/reportRenderer/list/442/listJson?clientId=1002";

            List<String> filterListSetupAdmin = checkPriceBookReportFilter(url, customAssert, "filterMetadatas","defaultName");
            List<String> excelColumnSetupAdmin = checkPriceBookReportFilter(url, customAssert, "ecxelColumns","defaultName");


            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());
            url = "/reportRenderer/list/442/configure";

            List<String> filterListClientAdmin = checkPriceBookReportFilter(url, customAssert, "filterMetadatas","defaultName");
            List<String> excelColumnClientAdmin = checkPriceBookReportFilter(url, customAssert, "ecxelColumns","defaultName");

            Collections.sort(filterListClientAdmin);
            Collections.sort(filterListSetupAdmin);
            Collections.sort(excelColumnClientAdmin);
            Collections.sort(excelColumnSetupAdmin);

            int index = 0;
            for (String s : filterListClientAdmin) {
                if (!s.equalsIgnoreCase(filterListSetupAdmin.get(index))) {
                    logger.info("Filter header value does not match {} | {}", filterListClientAdmin, filterListSetupAdmin);
                    customAssert.assertTrue(false, "Filter header value does not match " + filterListClientAdmin + " | " + filterListSetupAdmin);
                    break;
                }
                index++;
            }
            index = 0;
            for (String s : excelColumnClientAdmin) {
                if (!s.equalsIgnoreCase(excelColumnSetupAdmin.get(index))) {
                    logger.info("Filter header value does not match {} | {}", excelColumnClientAdmin, excelColumnSetupAdmin);
                    customAssert.assertTrue(false, "Filter header value does not match " + excelColumnClientAdmin + " | " + excelColumnSetupAdmin);
                    break;
                }
                index++;
            }

            String response = ReportRendererListConfigure.getReportListConfigureResponse(442);
            JSONObject jsonObject = new JSONObject(response);
            int filterLength = jsonObject.getJSONArray("filterMetadatas").length();
            int excelColumnLength = jsonObject.getJSONArray("ecxelColumns").length();
            boolean filter = false, excel = false;
            int order1 = 0, order2 = 0;

            for (index = 0; index < filterLength; index++) {
                if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    filter = jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).getBoolean("deleted");
                    jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).put("deleted", !filter);
                    break;
                }
            }
            for (index = 0; index < excelColumnLength; index++) {
                if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    excel = jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).getBoolean("deleted");
                    jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).put("deleted", !excel);
                    break;
                }
            }

            order1 = jsonObject.getJSONArray("ecxelColumns").getJSONObject(0).getInt("order");
            order2 = jsonObject.getJSONArray("ecxelColumns").getJSONObject(1).getInt("order");

            //swapping the values of the order for first two excel column headers
            jsonObject.getJSONArray("ecxelColumns").getJSONObject(0).put("order", order2);
            jsonObject.getJSONArray("ecxelColumns").getJSONObject(1).put("order", order1);

            String storeResponse = jsonObject.toString();
            ReportRendererListConfigure.updateReportListConfigureResponse(442, storeResponse);

            //checking the updated task by again hitting show api
            String responseSecond = ReportRendererListConfigure.getReportListConfigureResponse(442);
            jsonObject = new JSONObject(responseSecond);

            filterLength = jsonObject.getJSONArray("filterMetadatas").length();
            excelColumnLength = jsonObject.getJSONArray("ecxelColumns").length();

            for (index = 0; index < filterLength; index++) {
                if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).getBoolean("deleted") == filter) {
                        logger.info("Filter Data not matched after updating");
                        customAssert.assertTrue(false, "Filter Data not matched after updating");
                        break;
                    } else {
                        logger.info("Filter data matched after updating");
                        break;
                    }
                }
            }
            for (index = 0; index < excelColumnLength; index++) {
                if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).getBoolean("deleted") == excel) {
                        logger.info("Excel column not matched after updating");
                        customAssert.assertTrue(false, "Excel column not matched after updating");
                        break;
                    } else {
                        logger.info("Excel column matched after updating");
                        break;
                    }
                }
            }


            if (order1 == jsonObject.getJSONArray("ecxelColumns").getJSONObject(0).getInt("order")
                    || order2 == jsonObject.getJSONArray("ecxelColumns").getJSONObject(1).getInt("order")) {
                logger.info("Excel columns not swapped while updating");
                customAssert.assertTrue(false, "Excel columns not swapped while updating");
            } else {
                logger.info("Excel columns swapped and updated");
            }
        }
        catch (Exception e){
            logger.info("Exception Caught in PriceBookCheckFiltersAndHeadersFlowDownClientAdmin()");
            customAssert.assertTrue(false,"Exception caught in PriceBookCheckFiltersAndHeadersFlowDownClientAdmin()");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)//C46349
    public void clientSetupAdminReportConfiguration(){

        CustomAssert customAssert = new CustomAssert();
        try {

            List<String> listFromDBExcel = convertList(getListOfValuesFromDatabase("select id from dynamic_column_metadata where list_id =355 and is_excel = true",customAssert));
            List<String> listFromDBColumns = convertList(getListOfValuesFromDatabase("select id from dynamic_column_metadata where list_id =355 and is_excel = false",customAssert));
            List<String> listFromDBFilter =convertList(getListOfValuesFromDatabase("select id from dynamic_list_filter_metadata where list_id=355",customAssert));

            new ClientSetupHelper().loginWithClientSetupUser();
            String url = "/reportRenderer/list/355/listJson?clientId=1002";

            //List<String> listFromDBFilter = checkPriceBookReportFilter(url, customAssert, "filterMetadatas");
            //List<String> listFromDBExcel = checkPriceBookReportFilter(url, customAssert, "ecxelColumns");

            //new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());
            //url = "/reportRenderer/list/442/configure";

            List<String> filterListClientAdmin = checkPriceBookReportFilter(url, customAssert, "filterMetadatas","listFilterId");
            List<String> excelColumnClientAdmin = checkPriceBookReportFilter(url, customAssert,"ecxelColumns", "id");
            List<String> columnClientAdmin = checkPriceBookReportFilter(url, customAssert,"columns", "id");

            Collections.sort(filterListClientAdmin);
            Collections.sort(listFromDBFilter);
            Collections.sort(excelColumnClientAdmin);
            Collections.sort(listFromDBExcel);
            Collections.sort(columnClientAdmin);
            Collections.sort(listFromDBColumns);

            int index = 0;
            for (String s : filterListClientAdmin) {
                if (!s.equalsIgnoreCase(listFromDBFilter.get(index))) {
                    logger.info("Filter header value does not match {} | {}", filterListClientAdmin, listFromDBFilter);
                    customAssert.assertTrue(false, "Filter header value does not match " + filterListClientAdmin + " | " + listFromDBFilter);
                    break;
                }
                index++;
            }
            index = 0;
            for (String s : excelColumnClientAdmin) {
                if (!s.equalsIgnoreCase(listFromDBExcel.get(index))) {
                    logger.info("Filter header value does not match {} | {}", excelColumnClientAdmin, listFromDBExcel);
                    customAssert.assertTrue(false, "Filter header value does not match " + excelColumnClientAdmin + " | " + listFromDBExcel);
                    break;
                }
                index++;
            }
            index = 0;
            for (String s : columnClientAdmin) {
                if (!s.equalsIgnoreCase(listFromDBColumns.get(index))) {
                    logger.info("Filter header value does not match {} | {}", columnClientAdmin, listFromDBColumns);
                    customAssert.assertTrue(false, "Filter header value does not match " + columnClientAdmin + " | " + listFromDBColumns);
                    break;
                }
                index++;
            }

            String response = getReportListJsonResponse("/reportRenderer/list/355/listJson?clientId=1002");
            JSONObject jsonObject = new JSONObject(response);

            int filterLength = jsonObject.getJSONArray("filterMetadatas").length();
            int excelColumnLength = jsonObject.getJSONArray("ecxelColumns").length();
            int columnLength = jsonObject.getJSONArray("columns").length();

            boolean filter = false, excel = false, columns=false;

            for (index = 0; index < filterLength; index++) {
                if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    filter = jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).getBoolean("deleted");
                    jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).put("deleted", !filter);
                    break;
                }
            }
            for (index = 0; index < excelColumnLength; index++) {
                if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    excel = jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).getBoolean("deleted");
                    jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).put("deleted", !excel);
                    break;
                }
            }
            for (index = 0; index < columnLength; index++) {
                if (jsonObject.getJSONArray("columns").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    columns = jsonObject.getJSONArray("columns").getJSONObject(index).getBoolean("deleted");
                    jsonObject.getJSONArray("columns").getJSONObject(index).put("deleted", !excel);
                    break;
                }
            }

            String storeResponse = jsonObject.toString();
            updateReportListConfigureResponse(442, storeResponse);

            //checking the updated task by again hitting show api
            String responseSecond = getReportListJsonResponse("/reportRenderer/list/355/listJson?clientId=1002");
            jsonObject = new JSONObject(responseSecond);

            filterLength = jsonObject.getJSONArray("filterMetadatas").length();
            excelColumnLength = jsonObject.getJSONArray("ecxelColumns").length();
            columnLength = jsonObject.getJSONArray("columns").length();

            for (index = 0; index < filterLength; index++) {
                if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).getBoolean("deleted") == filter) {
                        logger.info("Filter Data not matched after updating");
                        customAssert.assertTrue(false, "Filter Data not matched after updating");
                        break;
                    } else {
                        logger.info("Filter data matched after updating");
                        break;
                    }
                }
            }
            for (index = 0; index < excelColumnLength; index++) {
                if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).getBoolean("deleted") == excel) {
                        logger.info("Excel column not matched after updating");
                        customAssert.assertTrue(false, "Excel column not matched after updating");
                        break;
                    } else {
                        logger.info("Excel column matched after updating");
                        break;
                    }
                }
            }
            for (index = 0; index < columnLength; index++) {
                if (jsonObject.getJSONArray("columns").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    if (jsonObject.getJSONArray("columns").getJSONObject(index).getBoolean("deleted") == columns) {
                        logger.info("column not matched after updating");
                        customAssert.assertTrue(false, "column not matched after updating");
                        break;
                    } else {
                        logger.info("column matched after updating");
                        break;
                    }
                }
            }
        }
        catch (Exception e){
            logger.info("Exception Caught in PriceBookCheckFiltersAndHeadersFlowDownClientAdmin()");
            customAssert.assertTrue(false,"Exception caught in PriceBookCheckFiltersAndHeadersFlowDownClientAdmin()");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false) //C88336
    public void PriceBookCheckFiltersAndHeadersFlowDownClientSetupAdmin(){

        CustomAssert customAssert = new CustomAssert();
        try {

            List<String> listFromDBExcel = convertList(getListOfValuesFromDatabase("select id from dynamic_column_metadata where list_id =442 and is_excel = true",customAssert));
            List<String> listFromDBFilter =convertList(getListOfValuesFromDatabase("select id from dynamic_list_filter_metadata where list_id=442",customAssert));

            new ClientSetupHelper().loginWithClientSetupUser();
            String url = "/reportRenderer/list/442/listJson?clientId=1002";

            //List<String> listFromDBFilter = checkPriceBookReportFilter(url, customAssert, "filterMetadatas");
            //List<String> listFromDBExcel = checkPriceBookReportFilter(url, customAssert, "ecxelColumns");

            //new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());
            //url = "/reportRenderer/list/442/configure";

            List<String> filterListClientAdmin = checkPriceBookReportFilter(url, customAssert, "filterMetadatas","listFilterId");
            List<String> excelColumnClientAdmin = checkPriceBookReportFilter(url, customAssert,"ecxelColumns", "id");

            Collections.sort(filterListClientAdmin);
            Collections.sort(listFromDBFilter);
            Collections.sort(excelColumnClientAdmin);
            Collections.sort(listFromDBExcel);

            int index = 0;
            for (String s : filterListClientAdmin) {
                if (!s.equalsIgnoreCase(listFromDBFilter.get(index))) {
                    logger.info("Filter header value does not match {} | {}", filterListClientAdmin, listFromDBFilter);
                    customAssert.assertTrue(false, "Filter header value does not match " + filterListClientAdmin + " | " + listFromDBFilter);
                    break;
                }
                index++;
            }
            index = 0;
            for (String s : excelColumnClientAdmin) {
                if (!s.equalsIgnoreCase(listFromDBExcel.get(index))) {
                    logger.info("Filter header value does not match {} | {}", excelColumnClientAdmin, listFromDBExcel);
                    customAssert.assertTrue(false, "Filter header value does not match " + excelColumnClientAdmin + " | " + listFromDBExcel);
                    break;
                }
                index++;
            }

            String response = getReportListJsonResponse("/reportRenderer/list/442/listJson?clientId=1002");
            JSONObject jsonObject = new JSONObject(response);
            int filterLength = jsonObject.getJSONArray("filterMetadatas").length();
            int excelColumnLength = jsonObject.getJSONArray("ecxelColumns").length();
            boolean filter = false, excel = false;

            for (index = 0; index < filterLength; index++) {
                if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    filter = jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).getBoolean("deleted");
                    jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).put("deleted", !filter);
                    break;
                }
            }
            for (index = 0; index < excelColumnLength; index++) {
                if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    excel = jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).getBoolean("deleted");
                    jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).put("deleted", !excel);
                    break;
                }
            }

            String storeResponse = jsonObject.toString();
            updateReportListConfigureResponse(442, storeResponse);

            //checking the updated task by again hitting show api
            String responseSecond = getReportListJsonResponse("/reportRenderer/list/442/listJson?clientId=1002");
            jsonObject = new JSONObject(responseSecond);

            filterLength = jsonObject.getJSONArray("filterMetadatas").length();
            excelColumnLength = jsonObject.getJSONArray("ecxelColumns").length();

            for (index = 0; index < filterLength; index++) {
                if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    if (jsonObject.getJSONArray("filterMetadatas").getJSONObject(index).getBoolean("deleted") == filter) {
                        logger.info("Filter Data not matched after updating");
                        customAssert.assertTrue(false, "Filter Data not matched after updating");
                        break;
                    } else {
                        logger.info("Filter data matched after updating");
                        break;
                    }
                }
            }
            for (index = 0; index < excelColumnLength; index++) {
                if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).get("defaultName").toString().equalsIgnoreCase("Supplier")) {
                    if (jsonObject.getJSONArray("ecxelColumns").getJSONObject(index).getBoolean("deleted") == excel) {
                        logger.info("Excel column not matched after updating");
                        customAssert.assertTrue(false, "Excel column not matched after updating");
                        break;
                    } else {
                        logger.info("Excel column matched after updating");
                        break;
                    }
                }
            }
        }
        catch (Exception e){
            logger.info("Exception Caught in PriceBookCheckFiltersAndHeadersFlowDownClientAdmin()");
            customAssert.assertTrue(false,"Exception caught in PriceBookCheckFiltersAndHeadersFlowDownClientAdmin()");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)//C88463
    public void checkInvoicingCurrencyImpactBillingDataCreation(String flowToTest){

        String publishAction = "publish";
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        CustomAssert csAssert = new CustomAssert();
        // Checking the arguments

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

                    temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                    if (temp != null)
                        serviceDataSectionName = temp.trim();

                    InvoiceHelper.updateServiceDataAndInvoiceConfigDistinct(serviceDataConfigFilePath,serviceDataConfigFileName,invoiceConfigFilePath,invoiceConfigFileName,contractSectionName,serviceDataSectionName,contractId);

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
                                                WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                                                boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId,serviceDataId,publishAction);

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
                                                                    result = workflowActionsHelper.performWorkflowAction(ConfigureConstantFields.getEntityIdByName(consumptionEntity),consumptionIds.get(i),approveAction);
                                                                    csAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptionEntity + "having id : " + consumptionIds.get(i));

                                                                    if (!result) {
                                                                        logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
                                                                        csAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
                                                                                "Hence skipping validation");
                                                                        addTestResult(getTestCaseIdForMethodName("checkInvoicingCurrencyImpactBillingDataCreation"), csAssert);
                                                                        csAssert.assertAll();
                                                                        return;
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


                Show show = new Show();
                show.hitShowVersion2(ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity),invoiceLineItemId);
                String res = show.getShowJsonStr();
                JSONObject jsonObject = new JSONObject(res);
                int systemConversionRate = Math.round(Float.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("systemConversionRate").get("values").toString()));

                if(systemConversionRate!=1){
                    logger.info("System Conversion Rate value is [{}], hence test case not cleared. Required value was 1.",systemConversionRate);
                    csAssert.assertTrue(false,"System Conversion Rate value is ["+systemConversionRate+"], hence test case not cleared. Required value was 1.");
                }
                else {
                    logger.info("System Conversion Rate value ,matched [{}]",systemConversionRate);
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
        addTestResult(getTestCaseIdForMethodName("checkInvoicingCurrencyImpactBillingDataCreation"), csAssert);
        csAssert.assertAll();
    }

    private boolean checkValuesInFilterColumnExcel(String url, CustomAssert customAssert,String stringToCheck, boolean checkColumns, boolean checkExcelColumns , boolean checkFilters) {

        boolean foundInListHeaders = false, foundInFilters = false, foundInExcel = false;
        try {

            APIUtils apiUtils = new APIUtils();
            HttpPost httpPost = apiUtils.generateHttpPostRequestWithQueryString(url, "application/json, text/javascript, */*; q=0.01", "application/json;charset=UTF-8");
            HttpResponse httpResponse = APIUtils.postRequest(httpPost, "{}");
            String response = EntityUtils.toString(httpResponse.getEntity());

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray;

            logger.info("Testing with url {}", url);

            //checking validation in list header json array
            if(checkFilters) {
                jsonArray = jsonObject.getJSONArray("filterMetadatas");
                for (int index = 0; index < jsonArray.length(); index++) {
                    if (jsonArray.getJSONObject(index).get("name").toString().equalsIgnoreCase(stringToCheck)
                            || jsonArray.getJSONObject(index).get("defaultName").toString().equalsIgnoreCase(stringToCheck)) {
                        foundInFilters = true;
                        break;
                    }
                }
            }
            //checking validation in filter json array
            if(checkColumns) {
                jsonArray = jsonObject.getJSONArray("columns");
                for (int index = 0; index < jsonArray.length(); index++) {
                    if (jsonArray.getJSONObject(index).get("name").toString().equalsIgnoreCase(stringToCheck)
                            || jsonArray.getJSONObject(index).get("defaultName").toString().equalsIgnoreCase(stringToCheck)) {
                        foundInListHeaders = true;
                        break;
                    }
                }
            }

            //checking validation in excel json array
            if(checkExcelColumns) {
                jsonArray = jsonObject.getJSONArray("ecxelColumns");
                for (int index = 0; index < jsonArray.length(); index++) {
                    if (jsonArray.getJSONObject(index).get("name").toString().equalsIgnoreCase(stringToCheck)
                            || jsonArray.getJSONObject(index).get("defaultName").toString().equalsIgnoreCase(stringToCheck)) {
                        foundInExcel = true;
                        break;
                    }
                }
            }

            logger.info("foundInFilters [{}] : foundInListHeaders [{}] : foundInExcel [{}]", foundInFilters, foundInListHeaders, foundInExcel);

        } catch (Exception e) {
            logger.info("Exception caught in checkValuesInFilterColumnExcel()");
            customAssert.assertTrue(false,"Exception caught in checkValuesInFilterColumnExcel()");
        }

        return ((!checkFilters||foundInFilters) & (!checkColumns||foundInListHeaders)&(!checkExcelColumns||foundInExcel));
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

            if (forecastUploadResponse != null && forecastUploadResponse.trim().contains("200:;")) {

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

    private List<String> checkPriceBookReportFilter(String url, CustomAssert customAssert, String stringToFind, String valueToSelect) {

        List<String> filterHeaders = new ArrayList<>();

        try {

            APIUtils apiUtils = new APIUtils();
            HttpPost httpPost = apiUtils.generateHttpPostRequestWithQueryString(url, "application/json, text/javascript, */*; q=0.01", "application/json;charset=UTF-8");
            HttpResponse httpResponse = APIUtils.postRequest(httpPost, "{}");
            String response = EntityUtils.toString(httpResponse.getEntity());

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray(stringToFind);

            logger.info("Testing with url {}", url);

            //add to list in filter header json array
            for (int index = 0; index < jsonArray.length(); index++) {
                filterHeaders.add(jsonArray.getJSONObject(index).get(valueToSelect).toString().toLowerCase());
            }

            logger.info("filterHeaders length [{}] : ", filterHeaders);

        } catch (Exception e) {
            logger.info("Exception caught in checkPriceBookReportFilter()");
            customAssert.assertTrue(false,"Exception caught in checkPriceBookReportFilter()");
        }

        return filterHeaders;
    }

    private int getConversionDataFromMatrix(String urlConversionData,CustomAssert customAssert,String flowToTest){
        try{
            //urlConversionData = "/conversionMatrix/getById?id=1011";

            String lastUserName = Check.lastLoggedInUserName;
            String lastUserPassword = Check.lastLoggedInUserPassword;

            //Login with client admin
            new Check().hitCheck(ConfigureEnvironment.getClientAdminUser(),ConfigureEnvironment.getClientAdminPassword());

            APIUtils apiUtils = new APIUtils();
            HttpGet httpGet;
            HttpResponse httpResponse;

            //starting create list item type
            logger.info("Extracting conversion matric from CSA");
            httpGet = apiUtils.generateHttpGetRequestWithQueryString(urlConversionData, "");
            httpGet.addHeader("Content-Type", "text/html;charset=UTF-8");
            httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");

            httpResponse = apiUtils.getRequest(httpGet);
            String response = EntityUtils.toString(httpResponse.getEntity());
            response = response.replace("\\","");
            response = response.replace("\"{","{");
            response = response.replace("}\"","}");

            JSONObject jsonObject1;
            String invoiceCurrency="",serviceDataCurrency="",intermediateCurrency="";

            JSONObject jsonObject;
            String temp;
            String defaultFields = "common extra fields";

            try {
                //from common extra fields
                temp = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, defaultFields, "invoicingCurrency");
                if (temp != null) {
                    temp = temp.split("->")[1].trim();
                    jsonObject = new JSONObject(temp);
                    invoiceCurrency = jsonObject.get("shortName").toString();
                }
                temp = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, defaultFields, "intermediateCurrency");
                if (temp != null) {
                    temp = temp.split("->")[1].trim();
                    jsonObject = new JSONObject(temp);
                    intermediateCurrency = jsonObject.get("shortName").toString();
                }
                temp = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, defaultFields, "currency");
                if (temp != null) {
                    temp = temp.split("->")[1].trim();
                    jsonObject = new JSONObject(temp);
                    serviceDataCurrency = jsonObject.get("shortName").toString();
                }

                //overriding common extra fields if found further
                temp = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "invoicingCurrency");
                if (temp != null) {
                    temp = temp.split("->")[1].trim();
                    jsonObject = new JSONObject(temp);
                    invoiceCurrency = jsonObject.get("shortName").toString();
                }
                temp = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "intermediateCurrency");
                if (temp != null) {
                    temp = temp.split("->")[1].trim();
                    jsonObject = new JSONObject(temp);
                    intermediateCurrency = jsonObject.get("shortName").toString();
                }
                temp = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "currency");
                if (temp != null) {
                    temp = temp.split("->")[1].trim();
                    jsonObject = new JSONObject(temp);
                    serviceDataCurrency = jsonObject.get("shortName").toString();
                }
            }
            catch (Exception e){
                logger.info("Exception Occurred while getting currency values from the configuration files");
                customAssert.assertTrue(false,"Exception Occurred while getting currency values from the configuration files");
            }


            int productOne=-1,productTwo=-1;
            try{
                jsonObject1 = new JSONObject(response);
                productOne = Math.round(Float.valueOf(jsonObject1.getJSONObject("conversionMatrix").getJSONObject("conversionRatesJson").getJSONObject(invoiceCurrency).get(serviceDataCurrency).toString()));
                productTwo = Math.round(Float.valueOf(jsonObject1.getJSONObject("conversionMatrix").getJSONObject("conversionRatesJson").getJSONObject(serviceDataCurrency).get(intermediateCurrency).toString()));
            }
            catch (Exception e){
                logger.info("Error occurred while extracting json values out of conversion data matrix");
                customAssert.assertTrue(false,"Error occurred while extracting json values out of conversion data matrix");
            }

            if(productOne!=-1&&productTwo!=-1)
                return productOne*productTwo;
        }
        catch (Exception e){
            logger.info("Error occurred while getting json values out of conversion data matrix");
            customAssert.assertTrue(false,"Error occurred while getting json values out of conversion data matrix");
        }
        return -1;
    }

    private List<List<String>> getListOfValuesFromDatabase(String query, CustomAssert customAssert){
        List<List<String>> result = new ArrayList<>();
        try{
            logger.info("Starting search for DB entry");

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            logger.info("Running query {}",query);
            result = postgreSQLJDBC.doSelect(query);
            logger.info("Result : {}",result);
            return result;

        }
        catch (Exception e){
            logger.info("Exception Caught while fetching Data from DB {}",e.toString());
            customAssert.assertFalse(true,"Exception Caught while fetching Data from DB "+e.toString());
        }

        return new ArrayList<>();
    }

    private List<String> convertList(List<List<String>> list){
        List<String> l = new ArrayList<>();
        for(List<String> listTemp : list ){
            l.add(listTemp.get(0));
        }
        return l;
    }

    private String getReportListJsonResponse(String url) {

        String reportListConfigureResponse = executor.post(url, getHeaders(), null).getResponse().getResponseBody();

        return reportListConfigureResponse;
    }

    private static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();

        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Content-Type", "application/json;charset=UTF-8");

        return headers;
    }

    private static void updateReportListConfigureResponse(int reportId,String payload) {

        String reportListConfigureResponse = executor.post("/reportRenderer/list/" + reportId + "/listSetupUpdate?clientId=1002&reportName=undefined", getHeaders(), payload).getResponse().getResponseBody();

    }

}
