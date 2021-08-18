package com.sirionlabs.test.invoice;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.invoice.InvoiceWorkFlowAction;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

public class TestCurrencyConversionValidation {

    private final static Logger logger = LoggerFactory.getLogger(TestCurrencyConversionValidation.class);

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

    private String pricingTemplateFilePath;

    private String invoiceFlowsConfigFilePath;
    private String invoiceFlowsConfigFileName;

    private int contractId;
    private int clientId;
    String datePattern = "MM-dd-yyyy";

    private String publishAction = "publish";
    private String approve = "Approve";

    private int serviceDataEntityTypeId = 64;
    private int lineItemEntityTypeId = 165;
    private int consumptionEntityTypeId = 176;
    private int invoiceEntityTypeId = 67;
    private int poEntityTypeId = 181;

    int invoiceLineItemListId = 358;
    int invoiceListId = 10;
    int clientCurrencyId = -1;

    String clientCurreny = "";

    private String serviceData = "service data";
    private String invoices = "invoices";
    private String invoiceLineItem = "invoice line item";

    int lineItemCustCurrId;
    int lineItemCustCurrFilterId;

    int invoiceCustCurrId;
    int invoiceCustCurrFilterId;

    int serviceDataCustCurrId;
    int serviceDataCustCurrFilterId;

    Map<String,Map<String,Integer>> flowWithEntityIdMap = new HashMap();

    private ArrayList<Integer> serviceDateToDelete = new ArrayList<>();
    private ArrayList<Integer> invoiceToDelete = new ArrayList<>();
    private ArrayList<Integer> lineItemToDelete = new ArrayList<>();

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CurrencyConversionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("CurrencyConversionConfigFileName");

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

        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");

        contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contractid"));
        clientId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"client id"));

        clientCurrencyId = InvoiceHelper.getClientCurrencyId(clientId);
        clientCurreny = InvoiceHelper.getCurrency(clientCurrencyId);

        lineItemCustCurrId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","custom currency id line item"));
        lineItemCustCurrFilterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","custom currency filter id line item"));

        invoiceCustCurrId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","custom currency id invoice"));
        invoiceCustCurrFilterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","custom currency filter id invoice"));

        serviceDataCustCurrId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","custom currency id service data"));
        serviceDataCustCurrFilterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","custom currency filter id service data"));

    }

    @Test(dataProvider = "dataProviderForCurrencyConversionServiceData",enabled = true)
    public void Test_ServiceData_Flows_Curr_Conv(String flowToTest){

        CustomAssert customAssert = new CustomAssert();

        InvoicePricingHelper pricingObj  = new InvoicePricingHelper();

        int contractId = -1;
        int serviceDataId = -1;
        try {
            try {

                contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flowToTest);

                InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                        invoiceConfigFileName, flowToTest, contractId);

                serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest,contractId);

            } catch (Exception e) {
                customAssert.assertTrue(false, "Exception while creating entity");
                customAssert.assertAll();
                return;
            }
            try {
                if (serviceDataId != -1) {
                    serviceDateToDelete.add(serviceDataId);
                    logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);

                    //Kill All Scheduler Tasks if Flag is On.
                    UserTasksHelper.removeAllTasks();

                    logger.info("Hitting Fetch API.");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();
                    List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());


                    String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                            "pricingstemplatefilename");

                    Boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(configFilePath, configFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, customAssert);

                    // changes for ARC RRC FLOW
                    if (pricingFile) {

                        String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                        if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {

                            //Wait for Pricing Scheduler to Complete
                            String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

                            if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                        flowToTest + "]");

                                customAssert.assertAll();
                                return;
                            } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                                logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);

                                logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                                customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                        "Hence failing Flow [" + flowToTest + "]");

                                customAssert.assertAll();
                                return;
                            } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                            {
                                if(!validateChargesTabForCurrencyConversion(serviceDataId,flowToTest,customAssert)){
                                    customAssert.assertTrue(false,"Client base amount on charges tab validated unsuccessfully");
                                }else {
                                    customAssert.assertTrue(true,"Client base amount on charges tab validated successfully");

                                }

//                                C3772
                                validate_CusCurr_Conv_ListingShowPage(serviceDataId,serviceDataEntityTypeId,serviceDataCustCurrId,serviceDataCustCurrFilterId,customAssert);

                            }
                        }else {
                            customAssert.assertTrue(false,"Error while pricing upload");
                        }
                    }else {
                        customAssert.assertTrue(false,"Error while pricing file download and edit");
                    }
                }else {
                    logger.error("Error while creating service data for flow "+ flowToTest);
                    customAssert.assertTrue(false,"Error while creating service data for flow "+ flowToTest);
                }
            } catch (Exception e) {
                customAssert.assertTrue(false, "Exception while doing pricing upload");
                customAssert.assertAll();
                return;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating base amount on charges tab " + e.getStackTrace());
        }finally {
            EntityOperationsHelper.deleteEntityRecord("contracts",contractId);
            EntityOperationsHelper.deleteEntityRecord("service data",serviceDataId);
        }

        customAssert.assertAll();
    }

    @Test(dataProvider = "serviceDataReportsToValidate",enabled = true)
    public void Test_ServiceDataReports(int reportId){

        CustomAssert customAssert = new CustomAssert();

        try{

            int currencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","custom currency id service data"));
            int filterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"custom currency","custom currency filter id service data"));

            int recordId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,String.valueOf(reportId),"datatotest"));

            int customCurrValueExpetced = RandomNumbers.getRandomNumberWithinRangeIndex(100,10000);

            int contractId = Integer.parseInt(ShowHelper.getValueOfField(serviceDataEntityTypeId,recordId,"contractid"));

            Double convFactor = getConvFacWRTClient(serviceDataEntityTypeId,recordId,contractId,customAssert);

            EntityEditHelper.updateDynamicField(serviceData,recordId,"dyn" + currencyId,customCurrValueExpetced,customAssert);

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            String filterPayload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId + "\"," +
                    "\"filterName\":\"" + currencyId + "\",\"entityFieldId\":" + currencyId + ",\"entityFieldHtmlType\":19," +
                    "\"min\":\"" + customCurrValueExpetced + "\",\"max\":\"" + customCurrValueExpetced + "\",\"suffix\":null}}},\"selectedColumns\":[]}";

            reportRendererListData.hitReportRendererListData(reportId,filterPayload);

            String reportListResponse = reportRendererListData.getListDataJsonStr();
            ListRendererListData listRendererListData = new ListRendererListData();
            HashMap<String,String> listingColumnValues = listRendererListData.getColValOfPartRecord(reportListResponse,String.valueOf(recordId));

            String customCurrencyValue =  listingColumnValues.get("dyn" + currencyId);

            if(customCurrencyValue  == null){
                customAssert.assertTrue(false,"customCurrencyValue is null");
            }else if(!customCurrencyValue.contains(String.valueOf(customCurrValueExpetced))){
                customAssert.assertTrue(false,"Expected value of customCurrencyValue not matched with the Actual Value for Service Data ID " + recordId);
            }

            String clientCustomCurrencyValue = listingColumnValues.get("clientdyn" + currencyId);

            if(clientCustomCurrencyValue  == null){
                customAssert.assertTrue(false,"client customCurrencyValue is null");
            }else if(!clientCustomCurrencyValue.contains((String.valueOf(Double.parseDouble(customCurrencyValue) * convFactor)))){
                customAssert.assertTrue(false,"Expected value of client customCurrencyValue not matched with the Actual Value for Service Data ID " + recordId);
            }

            System.out.println("");


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception in main test method");
        }
        customAssert.assertAll();
    }

    //    C3621
    @Test(dataProvider = "dataProviderForConversionValuesLineItem",enabled = true)
    public void Test_ConversionValues_Invoice_LineItem(String flowToTest ){


        CustomAssert customAssert = new CustomAssert();

        try{
            int invoiceLineItemId = lineItemCreation(contractId,flowToTest,customAssert);

            int serviceDataId = flowWithEntityIdMap.get(flowToTest).get(serviceData);
            int invoiceId = flowWithEntityIdMap.get(flowToTest).get(invoices);

            Double invoiceAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double amountApproved = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double discrepancyAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double disputeAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));

            String invoiceStartDate = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,"invoicestartdate");
            String invoiceEndDate = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,"invoiceenddate");

            Boolean invoiceCurrencyFieldsUpdationStatus = updateinvoiceCurConvertedFields(invoiceId, invoiceAmount, amountApproved, discrepancyAmount, disputeAmount, customAssert);

            if(!invoiceCurrencyFieldsUpdationStatus){
                customAssert.assertTrue(false,"Error while updating currency fields on invoice");
            }

            String payload = getInvoiceFilterPayload(invoiceStartDate,invoiceEndDate,invoiceAmount, disputeAmount, amountApproved);

            String invoiceShowResponse = getShowResponse(invoiceEntityTypeId,invoiceId);

            Double conversionFactor = getConvFacWRTClient(invoiceEntityTypeId,invoiceId,contractId,customAssert);

            Map<String, String> expectedColumnNamesAndValuesInvoice = getColumnMapForInvoice(invoiceShowResponse,conversionFactor);
//            Map<String, String> expectedColumnNamesAndValuesInvoice = getColumnMapForInvoice(invoiceShowResponse,convFactorINRToUSD2);  To Delete

            Boolean validateListingPageInvoice = validateCurrencyConListingPage(invoiceListId,payload,expectedColumnNamesAndValuesInvoice,customAssert);

            if(!validateListingPageInvoice){
                customAssert.assertTrue(false,"Invoices Listing page validated unsuccessfully for Currency Con Values");
            }

            String updatedInvoiceDate = "03-22-2018";

            Boolean updationStatusInvoiceDate = updateInvoiceDate(invoiceId,updatedInvoiceDate,customAssert);

            if(!updationStatusInvoiceDate){
                logger.error("Error while updating invoice date");
                customAssert.assertTrue(false,"Error while updating invoice date");
            }

            expectedColumnNamesAndValuesInvoice = getColumnMapForInvoice(invoiceShowResponse,conversionFactor);
            validateListingPageInvoice = validateCurrencyConListingPage(invoiceListId,payload,expectedColumnNamesAndValuesInvoice,customAssert);

            if(!validateListingPageInvoice){
                customAssert.assertTrue(false,"Invoices Listing page validated unsuccessfully for Currency Con Values after for rate card 2");
            }

            updatedInvoiceDate = "02-22-2018";

            updationStatusInvoiceDate = updateInvoiceDate(invoiceId,updatedInvoiceDate,customAssert);

            if(!updationStatusInvoiceDate){
                logger.error("Error while updating invoice date to validate the scenario where invoice date is not in any of the rate card defined on contract ");
                customAssert.assertTrue(false,"Error while updating invoice date");
            }


//            expectedColumnNamesAndValuesInvoice = getColumnMapForInvoice(invoiceShowResponse,convFactorINRToUSD1);
            validateListingPageInvoice = validateCurrencyConListingPage(invoiceListId,payload,expectedColumnNamesAndValuesInvoice,customAssert);

            validate_CusCurr_Conv_ListingShowPage(invoiceId,invoiceEntityTypeId,invoiceCustCurrId,invoiceCustCurrFilterId,customAssert);

            if(!validateListingPageInvoice){
                customAssert.assertTrue(false,"Invoices Listing page validated unsuccessfully for Currency Con Values after for rate card 2");
            }

            Double approvedAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double supplierTotal = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double pendingAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double resolvedAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double drAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));

            int currencyId = 8;  //For INR

            Boolean updationStatus = updateLineItemCurConvertedFields(invoiceLineItemId, supplierTotal, resolvedAmount, drAmount, pendingAmount, approvedAmount,currencyId,customAssert);

            if(!updationStatus){
                customAssert.assertTrue(false,"Error while updating line item custom currency fields");
            }

            payload = getLineItemFilterPayload(approvedAmount, pendingAmount, drAmount);

            String lineItemShowResponse = getShowResponse(lineItemEntityTypeId,invoiceLineItemId);

            conversionFactor = getConvFacWRTClient(lineItemEntityTypeId,invoiceLineItemId,contractId,customAssert);

            Map<String, String> expectedColumnNamesAndValues = getColumnMapForLineItem(lineItemShowResponse,conversionFactor,customAssert);

//            C63124 C63266
            Boolean validateListingPage = validateCurrencyConListingPage(invoiceLineItemListId,payload,expectedColumnNamesAndValues,customAssert);

            if(!validateListingPage){
                customAssert.assertTrue(false,"Line Item Listing page validated unsuccessfully for Currency Con Values");
            }

//            C3508
            validate_CusCurr_Conv_ListingShowPage(invoiceLineItemId,lineItemEntityTypeId,lineItemCustCurrId,lineItemCustCurrFilterId,customAssert);

            List<String> reportsToValidate = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"line item reports to test").split(","));

            Boolean  validateReportListingPage;
            for(String reportId : reportsToValidate) {
                validateReportListingPage = validateLineItemReportsToValidate(invoiceLineItemId,Integer.parseInt(reportId),conversionFactor,customAssert);

                if(!validateReportListingPage){
                    logger.error("Currency Conv Values validated unsuccessfully for Report Id " + reportId);
                    customAssert.assertTrue(false,"Currency Conv Values validated unsuccessfully for Report Id " + reportId);
                }
            }
            //Bug here
            validateConversionAfterUpdatingTheServiceEndDate(flowToTest,serviceDataId,invoiceId,invoiceLineItemId,conversionFactor,customAssert);

            for(String reportId : reportsToValidate) {
                validateLineItemReportsToValidate(invoiceLineItemId,Integer.parseInt(reportId),conversionFactor,customAssert);

            }

        }catch (Exception e){
            logger.error("Exception Inside Test Method " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception Inside Test Method " + e.getStackTrace());

        }
        customAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderForConversionValuesLineItem",enabled = true)
    public void Test_CurrConv_RateCard_WithDateRangeNotDefined_OnContract(String flowToTest){

        CustomAssert customAssert = new CustomAssert();

        try{

            int contractIdWithoutRateCard = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contractidwithoutratecard"));
            int invoiceLineItemId = lineItemCreation(contractIdWithoutRateCard,flowToTest,customAssert);

            int invoiceId = flowWithEntityIdMap.get(flowToTest).get(invoices);

//            C3693
            Double invoiceAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double amountApproved = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double discrepancyAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double disputeAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Boolean invoiceCurrencyFieldsUpdationStatus = updateinvoiceCurConvertedFields(invoiceId, invoiceAmount, amountApproved, discrepancyAmount, disputeAmount, customAssert);

            String invoiceStartDate = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,"invoicestartdate");
            String invoiceEndDate = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,"invoiceenddate");

            if(!invoiceCurrencyFieldsUpdationStatus){
                customAssert.assertTrue(false,"Error while updating currency fields on invoice");
            }

            String payload = getInvoiceFilterPayload(invoiceStartDate,invoiceEndDate,invoiceAmount, disputeAmount, amountApproved);

            String invoiceShowResponse = getShowResponse(invoiceEntityTypeId,invoiceId);

            Double conversionFactor = getConvFacWRTClient(invoiceEntityTypeId,invoiceId,contractId,customAssert);
            Map<String, String> expectedColumnNamesAndValuesInvoice = getColumnMapForInvoice(invoiceShowResponse,conversionFactor);

            Boolean validateListingPageInvoice = validateCurrencyConListingPage(invoiceListId,payload,expectedColumnNamesAndValuesInvoice,customAssert);

            if(!validateListingPageInvoice){
                customAssert.assertTrue(false,"Invoices Listing page validated unsuccessfully for Currency Con Values");
            }

            expectedColumnNamesAndValuesInvoice = getColumnMapForInvoice(invoiceShowResponse,conversionFactor);
            validateListingPageInvoice = validateCurrencyConListingPage(invoiceListId,payload,expectedColumnNamesAndValuesInvoice,customAssert);

            if(!validateListingPageInvoice){
                customAssert.assertTrue(false,"Invoices Listing page validated unsuccessfully for Currency Con Values after for rate card 2");
            }

            Double approvedAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double supplierTotal = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double pendingAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double resolvedAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));
            Double drAmount = Double.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100, 10000));

            int currencyId = 8;  //For INR

            Boolean updationStatus = updateLineItemCurConvertedFields(invoiceLineItemId, supplierTotal, resolvedAmount, drAmount, pendingAmount, approvedAmount,currencyId,customAssert);

            if(!updationStatus){
                customAssert.assertTrue(false,"Error while updating line item custom currency fields");
            }

            payload = getLineItemFilterPayload(approvedAmount, pendingAmount, drAmount);

            String lineItemShowResponse = getShowResponse(lineItemEntityTypeId,invoiceLineItemId);

            Map<String, String> expectedColumnNamesAndValues = getColumnMapForLineItem(lineItemShowResponse,conversionFactor,customAssert);

            Boolean validateListingPage = validateCurrencyConListingPage(invoiceLineItemListId,payload,expectedColumnNamesAndValues,customAssert);

            if(!validateListingPage){
                customAssert.assertTrue(false,"Line Item Listing page validated unsuccessfully for Currency Con Values");
            }

            List<String> reportsToValidate = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"line item reports to test").split(","));

            Boolean  validateReportListingPage;
            for(String reportId : reportsToValidate) {
                validateReportListingPage = validateLineItemReportsToValidate(invoiceLineItemId,Integer.parseInt(reportId),conversionFactor,customAssert);

                if(!validateReportListingPage){
                    logger.error("Currency Conv Values validated unsuccessfully for Report Id " + reportId);
                    customAssert.assertTrue(false,"Currency Conv Values validated unsuccessfully for Report Id " + reportId);
                }
            }


        }catch (Exception e){
            logger.error("Exception Inside Test Method " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception Inside Test Method " + e.getStackTrace());

        }
        customAssert.assertAll();

    }

    @Test(dataProvider = "invoiceReportsToValidate",enabled = true)
    public void Test_Inv_Reports_Curr_Conv(int reportId){

        CustomAssert customAssert = new CustomAssert();
        InvoiceWorkFlowAction invoiceWorkFlowAction = new InvoiceWorkFlowAction();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        try{

            String customFieldValue = "";

            int filterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"dynamicfilterid"));
            int entityFieldId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entityfieldid"));

            int recordToTestInReport = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName, String.valueOf(reportId),"datatotest"));
            int contractId = Integer.parseInt(ShowHelper.getValueOfField(invoiceEntityTypeId,recordToTestInReport,"contractid"));

            Double convFactor = getConvFacWRTClient(invoiceEntityTypeId,recordToTestInReport,contractId,customAssert);

            List<String> editableColumns = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,String.valueOf(reportId),"columns editable").split(","));
            Map<String,Integer> invoiceCurrencyFieldValuesMap = createInvoiceCurrencyFieldValuesMap(editableColumns);

            String workFlowSteps = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,String.valueOf(reportId),"workflowsteptoperform");
            List<String> workflowStepToPerform = new ArrayList<>();
            if(!workFlowSteps.equalsIgnoreCase("")){

                workflowStepToPerform = Arrays.asList(workFlowSteps.split(","));

                if(workflowStepToPerform.get(0).equals("activate")|| workflowStepToPerform.get(0).equals("restore")){

                    String payload = "{\"body\":{\"data\":{\"id\":{\"name\":\"id\",\"id\":636,\"values\":" + recordToTestInReport + ",\"multiEntitySupport\":false},\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + invoiceEntityTypeId + ",\"multiEntitySupport\":false}}}}";
                    invoiceWorkFlowAction.performWfActionInvoice(workflowStepToPerform.get(0),payload,customAssert);

                    Boolean editStatusCurrencyFields = editCurrencyFields(invoices,recordToTestInReport,invoiceCurrencyFieldValuesMap,customAssert);
                    customFieldValue = updateCustomField(recordToTestInReport,entityFieldId,customAssert);

                    if(!editStatusCurrencyFields){
                        customAssert.assertTrue(false,"Invoice currency field edit done unsuccessfully");
                    }

                    invoiceWorkFlowAction.performWfActionInvoice(workflowStepToPerform.get(1),payload,customAssert);
                }else {
                    customFieldValue = updateCustomField(recordToTestInReport,entityFieldId,customAssert);
                }
            }else {
                Boolean editStatusCurrencyFields = editCurrencyFields(invoices,recordToTestInReport,invoiceCurrencyFieldValuesMap,customAssert);

                if(!editStatusCurrencyFields){
                    customAssert.assertTrue(false,"Invoice currency field edit done unsuccessfully");
                }
            }

            String reportFilterPayload = "";
            if(reportId == 90 || reportId == 201) {
                reportFilterPayload = invoiceHelper.getFilPayloadForSpecCustomFieldInvoice(filterId, entityFieldId, customFieldValue);
            }else {

                Double disputeAmountConverted = invoiceCurrencyFieldValuesMap.get("disputeAmount") * convFactor;
                Double amountApprovedConverted =invoiceCurrencyFieldValuesMap.get("amountApproved")* convFactor;

                reportFilterPayload = getReportFilterPayload(disputeAmountConverted,amountApprovedConverted);
            }
            String showResponse = getShowResponse(invoiceEntityTypeId,recordToTestInReport);

            Map<String,String> expectedColumnNamesAndValues = getColumnMapForInvoiceReport(showResponse,convFactor);

            validateCurrencyConReportPage(reportId,reportFilterPayload, expectedColumnNamesAndValues, customAssert);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validation of report id " + reportId);
        }
        customAssert.assertAll();
    }

    //    C8321 C8324
    @Test(enabled = true)
    public void Test_POCurrencyConversion(){

        CustomAssert customAssert = new CustomAssert();
        Edit edit = new Edit();
        int expectedPOAvailable = 0;
        int poTotal = 0;
        int expectedPO = 0;
        int poBurn = 0;
        int convFactor = -1;

        try{
            String purchaseOrder = "purchase orders";

            int poId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"purchase order id"));

            String editPayload = edit.getEditPayload(purchaseOrder,poId);

            if(JSONUtility.validjson(editPayload)) {
                JSONObject editPayloadJson = new JSONObject(editPayload);

                poTotal = RandomNumbers.getRandomNumberWithinRangeIndex(100,1000);
                expectedPO = poTotal - 10;
                poBurn = expectedPO - 10;
                expectedPOAvailable = poTotal - poBurn;

                editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("poTotal").put("values",poTotal);
                editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("expectedPoBurn").put("values",expectedPO);
                editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("poBurn").put("values",poBurn);

                edit.hitEdit(purchaseOrder, editPayloadJson.toString());
                String editResponse = edit.getEditDataJsonStr();

                if(editResponse.contains("success")){

                    Show show = new Show();
                    show.hitShowVersion2(poEntityTypeId,poId);
                    String showResponse = show.getShowJsonStr();

                    int parentEntityId = Integer.parseInt(ShowHelper.getValueOfField("contractid",showResponse));
                    convFactor = getConvFacWRTClient(poEntityTypeId, poId, parentEntityId, customAssert).intValue();

                    String childCurrency = ShowHelper.getValueOfField("currency short code",showResponse);

                    String pOTotalActual =  ShowHelper.getValueOfField("pototaldisplayvalues",showResponse).replace(",","");
                    String pOBurnActual =ShowHelper.getValueOfField("poburndisplayvalues",showResponse).replace(",","");
                    String pOAvailableActual = ShowHelper.getValueOfField("poavailabledisplayvalues",showResponse).replace(",","");
                    String expectedPoBurnActual = ShowHelper.getValueOfField("expectedpoburndisplayvalues",showResponse).replace(",","");

                    String pOTotalExpected;
                    String pOBurnExpected;
                    String pOAvailableExpected;
                    String expectedPoBurnExpected;

                    if(!childCurrency.equals(clientCurreny)) {

                        pOTotalExpected = poTotal + " " + childCurrency + " (" + (convFactor * poTotal) + " " + clientCurreny + ")";
                        pOBurnExpected = poBurn + " " + childCurrency + " (" + (convFactor * poBurn) + " " + clientCurreny + ")";
                        pOAvailableExpected = expectedPOAvailable + " " + childCurrency + " (" + (convFactor * expectedPOAvailable) + " " + clientCurreny + ")";
                        expectedPoBurnExpected = expectedPO + " " + childCurrency + " (" + (convFactor * expectedPO) + " " + clientCurreny + ")";

                    }else {
                        pOTotalExpected = poTotal + " " + childCurrency;
                        pOBurnExpected = poBurn + " " + childCurrency;
                        pOAvailableExpected = expectedPOAvailable + " " + childCurrency;
                        expectedPoBurnExpected = expectedPO + " " + childCurrency;

                    }

                    if(!pOTotalActual.equals(pOTotalExpected)){
                        customAssert.assertTrue(false,"PO Total Actual not equal to PO Total Expected");
                    }
                    if(!pOBurnActual.equals(pOBurnExpected)){
                        customAssert.assertTrue(false,"PO Burn Actual not equal to PO Burn Expected");
                    }
                    if(!pOAvailableActual.equals(pOAvailableExpected)){
                        customAssert.assertTrue(false,"PO Available Actual not equal to PO Available Expected");
                    }
                    if(!expectedPoBurnActual.equals(expectedPoBurnExpected)){
                        customAssert.assertTrue(false,"Expected PO Burn Available Actual not equal to Expected PO Burn Expected");
                    }
                }else {
                    customAssert.assertTrue(false,"Edit done unsuccessfully for purchase order id " + poId);
                }

            }else {
                customAssert.assertTrue(false,"Edit Payload is not a valid Json");
            }

            int reportId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"purchase order report id"));

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            String payload = "{\"filterMap\":{\"entityTypeId\":" + poEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"310\":{\"filterId\":\"310\",\"filterName\":\"poAvailable\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                    "\"min\":\"" + expectedPOAvailable + "\",\"max\":\"" + expectedPOAvailable + "\",\"suffix\":null}}},\"selectedColumns\":[]}";
            reportRendererListData.hitReportRendererListData(reportId,payload);
            String reportListResponse = reportRendererListData.getListDataJsonStr();
            ListRendererListData listRendererListData = new ListRendererListData();
            HashMap<String,String> listingColumnValues = listRendererListData.getColValOfPartRecord(reportListResponse,String.valueOf(poId));

            String poTotalAmountReport =  listingColumnValues.get("pototalamount");
            if(poTotalAmountReport  == null){
                customAssert.assertTrue(false,"poTotalAmountReport is null");
            }else if(!poTotalAmountReport.contains(String.valueOf(poTotal))){
                customAssert.assertTrue(false,"Expected value of poTotalAmountReport not matched with the Actual Value for PO ID " + poId);
            }
            String clientPoTotalAmountReport = listingColumnValues.get("clientpototalamount");

            if(clientPoTotalAmountReport  == null){
                customAssert.assertTrue(false,"clientPoTotalAmountReport is null");
            }else if(!clientPoTotalAmountReport.contains((String.valueOf(poTotal * convFactor)))){
                customAssert.assertTrue(false,"Expected value of client poTotalAmountReport not matched with the Actual Value for PO ID " + poId);
            }

            String poburnReport = listingColumnValues.get("poburn");
            if(poburnReport  == null){
                customAssert.assertTrue(false,"poburn is null");
            }else if(!poburnReport.contains((String.valueOf(poBurn)))){
                customAssert.assertTrue(false,"Expected value of poburnReport not matched with the Actual Value for PO ID " + poId);
            }

            String clientPoburnReport = listingColumnValues.get("clientpoburn");
            if(clientPoburnReport  == null){
                customAssert.assertTrue(false,"clientPoburn is null");
            }else if(!clientPoburnReport.contains((String.valueOf(poBurn * convFactor)))){
                customAssert.assertTrue(false,"Expected value of client PoburnReport not matched with the Actual Value for PO ID " + poId);
            }

            String poAvailableReport = listingColumnValues.get("poavailable");
            if(poAvailableReport  == null){
                customAssert.assertTrue(false,"poAvailable is null");
            }else if(!poAvailableReport.contains((String.valueOf(expectedPOAvailable)))){
                customAssert.assertTrue(false,"Expected value of poAvailableReport not matched with the Actual Value for PO ID " + poId);
            }

            String clientPoAvailableReport  = listingColumnValues.get("clientpoavailable");
            if(clientPoAvailableReport  == null){
                customAssert.assertTrue(false,"clientPoAvailable is null");
            }else if(!clientPoAvailableReport.contains((String.valueOf(expectedPOAvailable * convFactor)))){
                customAssert.assertTrue(false,"Expected value of client PoAvailableReport not matched with the Actual Value for PO ID " + poId);
            }

            String expectedpoburnReport = listingColumnValues.get("expectedpoburn");
            if(expectedpoburnReport  == null){
                customAssert.assertTrue(false,"expectedpoburn is null");
            }else if(!poAvailableReport.contains((String.valueOf(expectedPOAvailable)))){
                customAssert.assertTrue(false,"Expected value of poAvailableReport not matched with the Actual Value for PO ID " + poId);
            }

            String clientExpectedpoburnReport = listingColumnValues.get("clientexpectedpoburn");
            if(clientExpectedpoburnReport  == null){
                customAssert.assertTrue(false,"clientExpectedpoburn is null");
            }else if(!clientPoAvailableReport.contains((String.valueOf(expectedPOAvailable * convFactor)))){
                customAssert.assertTrue(false,"Expected value of clientExpectedpoburnReport not matched with the Actual Value for PO ID " + poId);
            }

        }catch (Exception e){
            logger.error("Exception in main test method " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception in main test method " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    public Boolean validateConversionAfterUpdatingTheServiceEndDate(String flowToTest,int serviceDataId,int invoiceId,int invoiceLineItemId,Double convFactor,CustomAssert customAssert){

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        Boolean convStatus = true;

        try{

            String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,flowToTest,"pricingstemplatefilename");

            String sheetName = "Pricing";
            int rowNumber = 7;
            Boolean editPricingSheet;

            String startDate = "04-01-2018 00:00:00";
            String endDate = "04-30-2018 00:00:00";

            Boolean serviceDataUpdationStatus = updateServiceDataStartDateEndDate(serviceDataId,startDate,endDate,customAssert);
            HashMap<Integer,Integer> columnMap = new HashMap<>();

            columnMap.put(7,20);
            columnMap.put(8,20);

            Boolean uploadPricing;
            if(!serviceDataUpdationStatus){
                customAssert.assertTrue(false,"Service Data Start Date and End Date updated unsuccessfully");
                convStatus = false;
            }else {

                editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(pricingTemplateFilePath, pricingTemplateFileName, serviceDataId, sheetName, rowNumber, columnMap, customAssert);

                if (!editPricingSheet) {
                    customAssert.assertTrue(false, "Error while editing pricing sheet");
                    convStatus = false;
                } else {
                    uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(pricingTemplateFilePath, pricingTemplateFileName, flowToTest, customAssert);

                    if (!uploadPricing) {
                        customAssert.assertTrue(false, "Error while pricing upload");
                        convStatus = false;
                    } else {
//                        int consumptionId =invoiceHelper.getLatestConsumptionCreated(serviceDataId,2,customAssert);
                        ArrayList<Integer> consumptionIds = new ArrayList<>();
//                        C89805
                        String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, 2, consumptionIds);

                        logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                        if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                            logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                            customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                        } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                            logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                            customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                        }

                        if (consumptionIds.size() != 2) {
                            customAssert.assertTrue(false, "Unable to get new consumption Id");
                            convStatus = false;
                        } else {

                            int consumptionId = consumptionIds.get(0);
                            Double finalConsumption = 200.0;

                            Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                            if (!finalConsumptionUpdationStatus) {
                                customAssert.assertTrue(false, "Final Consumption updated unsuccessfully");
                                convStatus = false;
                            } else {

                                Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                                if (!consumptionApprovalStatus) {
                                    customAssert.assertTrue(false, "Error while approving consumption");
                                    convStatus = false;
                                } else {

                                    Boolean revalidateScenario = invoiceHelper.revalidateScenario(invoiceId, customAssert);
                                    if (!revalidateScenario) {
                                        customAssert.assertTrue(false, "Error while revalidating the invoice");
                                        convStatus = false;
                                    }

                                    Boolean invoiceDateUpdationStatus = updateInvoiceLineItemServiceDataStartDateEndDate(invoiceLineItemId, startDate, endDate, customAssert);

                                    if (!invoiceDateUpdationStatus) {
                                        customAssert.assertTrue(false, "Error while updating service start data end date on line item");
                                        convStatus = false;
                                    } else {
                                        String lineItemShowResponse = getShowResponse(lineItemEntityTypeId, invoiceLineItemId);

//                                             Double convFactorINRToUSD = 8.0;  //rate card 1

                                        Map<String, String> expectedColumnNamesAndValues = getColumnMapForLineItem(lineItemShowResponse, convFactor,customAssert);

                                        Double approvedAmount = Double.parseDouble(expectedColumnNamesAndValues.get("approvedAmount"));
                                        Double pendingAmount = Double.parseDouble(expectedColumnNamesAndValues.get("discrepencyResolutionPendingAmount"));
                                        Double drAmount = Double.parseDouble(expectedColumnNamesAndValues.get("discrepancyResolutionAmount"));


                                        String payload = getLineItemFilterPayload(approvedAmount, pendingAmount, drAmount);

                                        Boolean validateListingPage = validateCurrencyConListingPage(invoiceLineItemListId, payload, expectedColumnNamesAndValues, customAssert);

                                        if (!validateListingPage) {
                                            customAssert.assertTrue(false, "Line Item Listing page validated unsuccessfully for Currency Con Values after service end date updation");
                                            convStatus = false;
                                        }

                                        List<String> reportsToValidate = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "line item reports to test").split(","));

                                        Boolean validateReportListingPage;
                                        for (String reportId : reportsToValidate) {
                                            validateReportListingPage = validateLineItemReportsToValidate(invoiceLineItemId, Integer.parseInt(reportId), convFactor, customAssert);

                                            if (!validateReportListingPage) {
                                                logger.error("Currency Conv Values validated unsuccessfully for Report Id " + reportId);
                                                customAssert.assertTrue(false, "Currency Conv Values validated unsuccessfully for Report Id " + reportId);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }catch (Exception e){
            logger.error("Exception Inside Test Method " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception Inside Test Method " + e.getStackTrace());
            convStatus = false;

        }

        return convStatus;
    }

    private Boolean updateInvoiceDate(int invoiceId,String updatedDate,CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;

        try{

            String entity = "invoices";

            editResponse = edit.hitEdit(entity,invoiceId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceDate").put("values",updatedDate);

            editResponse = edit.hitEdit(entity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Invoice updated unsuccessfully");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating Invoices " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating invoices " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;

    }

    public Boolean validateLineItemReportsToValidate(int invoiceLineItemId,int reportId,Double convFactor,CustomAssert customAssert){

        Boolean validationStatus = true;
        try{

            String lineItemShowResponse = getShowResponse(lineItemEntityTypeId,invoiceLineItemId);

            Map<String, String> expectedColumnNamesAndValues = getColumnMapForLineItem(lineItemShowResponse,convFactor,customAssert);

            Double approvedAmount = Double.parseDouble(expectedColumnNamesAndValues.get("approvedAmount"));
            Double pendingAmount = Double.parseDouble(expectedColumnNamesAndValues.get("discrepencyResolutionPendingAmount"));
            Double drAmount = Double.parseDouble(expectedColumnNamesAndValues.get("discrepancyResolutionAmount"));

            String payload = getLineItemFilterPayload(approvedAmount,pendingAmount,drAmount);

            Boolean validateReportListingPage = validateCurrencyConReportPage(reportId,payload, expectedColumnNamesAndValues, customAssert);

            if (!validateReportListingPage) {
                customAssert.assertTrue(false, "Report Line Item Listing page validated unsuccessfully for Currency Con Values on Report Page for Report ID " + reportId);
                validationStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;
    }

    @DataProvider
    public Object[][] dataProviderForCurrencyConversionInvoice(){
        logger.info("Setting all Currency Conversion Flows to Validate for invoice");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flowstotestinvoice").split(","));
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForCurrencyConversionServiceData(){
        logger.info("Setting all Currency Conversion Flows to Validate for Service Data");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flowstotestservicedata").split(","));
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] lineItemReportsToValidate(){

        logger.info("Getting Reports to validate for line item currency field conversion");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"reportstovalidate").split(","));
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);

    }

    @DataProvider
    public Object[][] invoiceReportsToValidate(){

        logger.info("Getting Reports to validate for invoice currency field conversion");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoice reports to validate").split(","));
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{Integer.parseInt(flowToTest)});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] serviceDataReportsToValidate(){

        logger.info("Getting Reports to validate for service data currency field conversion");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service data reports to validate").split(","));
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{Integer.parseInt(flowToTest)});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForConversionValuesLineItem(){

        logger.info("Getting Reports to validate for invoice currency field conversion");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flowsforconversionvalueslineitem").split(","));
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);

    }

    @AfterClass
    public void afterClass(){

        EntityOperationsHelper.deleteMultipleRecords(invoiceLineItem,lineItemToDelete);
        EntityOperationsHelper.deleteMultipleRecords(invoices,invoiceToDelete);
        EntityOperationsHelper.deleteMultipleRecords(serviceData,serviceDateToDelete);

    }

    private Boolean validateCurrencyConvertedValues(int invoiceId,String flowToTest,CustomAssert customAssert){

        int invoiceEntityTypeId = 67;
        Boolean validationStatus = true;

        List<String> fieldsToTestOnlyValues = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"fieldstotestonlyvalues").split(","));
        List<String> fieldsToTestWithBrackets = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"fieldstotestwithbrackets").split(","));

        String clientCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"clientcurrency");
        String otherCurrency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"othercurrency");

        int conversionRate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"conversionrate"));

        Show show = new Show();
        show.hitShow(invoiceEntityTypeId,invoiceId);
        String showPageResponse= show.getShowJsonStr();

        JSONObject dataJson = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");

        logger.info("Validating currency converted values");

        String fieldsToTestWithBracket = "";
        Double originalFieldValue;
        Double convertedFieldValueExpected;
        Double convertedFieldValueActual;
        String  fieldToTestOnlyValues;

        String convertedBracketValueActual;
        String convertedBracketValueExpected;

        for(int i=0;i< fieldsToTestWithBrackets.size();i++){

            try {

                fieldsToTestWithBracket = fieldsToTestWithBrackets.get(i);
                originalFieldValue = Double.parseDouble(ShowHelper.getValueOfField(fieldsToTestWithBracket, showPageResponse));
                convertedFieldValueExpected = originalFieldValue * conversionRate;
                fieldToTestOnlyValues = fieldsToTestOnlyValues.get(i);
                convertedFieldValueActual = Double.parseDouble(dataJson.getJSONObject(fieldToTestOnlyValues).get("values").toString());

                if(convertedFieldValueExpected.equals(convertedFieldValueActual)){
                    customAssert.assertTrue(true,"Converted Field Value Expected and Converted Field Value Actual matched for field " + fieldToTestOnlyValues);
                }else {
                    customAssert.assertTrue(false,"Converted Field Value Expected and Converted Field Value Actual didn't match for field " + fieldToTestOnlyValues);
                    validationStatus = false;
                }

                convertedBracketValueActual = dataJson.getJSONObject(fieldsToTestWithBracket).get("displayValues").toString();

                convertedBracketValueExpected = originalFieldValue + " " + otherCurrency + " (" + convertedFieldValueExpected + " " + clientCurrency + ")";

                if(!convertedBracketValueActual.equals(convertedBracketValueExpected)){
                    customAssert.assertTrue(false,"convertedBracketValueActual and convertedBracketValueExpected matched unsuccessfully");
                    validationStatus = false;
                }

            }catch (NumberFormatException nfe){
                logger.error("Field value is not an integer for field " + fieldsToTestWithBracket + " or "+ fieldsToTestOnlyValues + nfe.getStackTrace());
                customAssert.assertTrue(false,"Field value is not an integer for field " + fieldsToTestWithBracket + " or "+ fieldsToTestOnlyValues + nfe.getStackTrace());
                validationStatus = false;
            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while validating currency converted values for field " + fieldsToTestWithBracket + " or "+ fieldsToTestOnlyValues + e.getStackTrace());
                validationStatus = false;
            }

        }
        return validationStatus;
    }

    private boolean validateChargesTabForCurrencyConversion(int serviceDataId,String flowToTest,CustomAssert customAssert){

        int chargesTabId = 309;

        Boolean validationStatus = true;
        TabListData tabListData = new TabListData();
        tabListData.hitTabListData(chargesTabId,serviceDataEntityTypeId,serviceDataId);
        String tabListResponse = tabListData.getTabListDataResponseStr();

        JSONArray dataArray;
        JSONObject firstRowData;
        JSONArray firstRowDataArray;
        JSONObject indvColumnJson;
        Double baseAmountValue = 0.00;
        Double clientBaseAmountValue = 0.00;

        int contractId = Integer.parseInt(ShowHelper.getValueOfField(serviceDataEntityTypeId,serviceDataId,"contractid"));
        Double conversionFactor = getConvFacWRTClient(serviceDataEntityTypeId,serviceDataId,contractId,customAssert);

        if(!APIUtils.validJsonResponse(tabListResponse)){
            customAssert.assertTrue(false,"charges tab response is not a valid json");
            return false;
        }else {
            JSONObject tabListResponseJson = new JSONObject(tabListResponse);
            dataArray = tabListResponseJson.getJSONArray("data");
            firstRowData = dataArray.getJSONObject(0);

            firstRowDataArray = JSONUtility.convertJsonOnjectToJsonArray(firstRowData);

            for(int i=0;i<firstRowDataArray.length();i++){

                indvColumnJson = firstRowDataArray.getJSONObject(i);

                if (indvColumnJson.getString("columnName").equalsIgnoreCase("baseamount")) {
                    try {
                        baseAmountValue = Double.parseDouble(indvColumnJson.getString("value"));
                    }catch (Exception e){

                        customAssert.assertTrue(false,"Exception while parsing base amount value");
                        validationStatus = false;
                    }
                }
                if (indvColumnJson.getString("columnName").equalsIgnoreCase("clientbaseamount")) {
                    try {
                        clientBaseAmountValue = Double.parseDouble(indvColumnJson.getString("value"));
                    }catch (Exception e){
                        customAssert.assertTrue(false,"Exception while parsing clientBaseAmount Value");
                        validationStatus = false;
                    }
                }
            }

            Double expectedClientBaseAmount  = baseAmountValue * conversionFactor;
            if(!expectedClientBaseAmount.equals(clientBaseAmountValue)){
                customAssert.assertTrue(false,"Expected and actual client base amount are not equal Expected : " + expectedClientBaseAmount + " Actual " + clientBaseAmountValue);
                validationStatus = false;
            }

        }

        return validationStatus;
    }

    private void validateCurrencyConvertedValuesListingPage(List<String> fieldsToValidateListing,List<String> convertedFieldsToValidateListing,int conversionFactor,int entityTypeId,int listId,int entityId,CustomAssert customAssert){

        Boolean entityFound = false;
        ListRendererListData listRendererListData = new ListRendererListData();
        String payload = "{\"filterMap\": {\"entityTypeId\": " + entityTypeId + ",\"offset\": 0,\"size\": 20,\"orderByColumnName\": \"id\",\"orderDirection\": \"desc\",\"filterJson\": {}}}";

        listRendererListData.hitListRendererListDataV2(listId,payload);
        String listDataResponse = listRendererListData.getListDataJsonStr();

        JSONObject listDataResponseJson = new JSONObject(listDataResponse);
        JSONArray dataArray = listDataResponseJson.getJSONArray("data");
        JSONObject indvDataRowJson;
        JSONArray indvDataRowJsonArray = new JSONArray();

        try {
            outerLoop:
            for (int i = 0; i < dataArray.length(); i++) {
                indvDataRowJson = dataArray.getJSONObject(i);

                indvDataRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indvDataRowJson);

                for (int j = 0; j < indvDataRowJsonArray.length(); j++) {

                    if (indvDataRowJsonArray.getJSONObject(j).getString("columnName").equals("id")) {

                        if (indvDataRowJsonArray.getJSONObject(j).getString("value").split(":;")[1].equalsIgnoreCase(String.valueOf(entityId))) {
                            entityFound = true;
                            break outerLoop;
                        }
                    }
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while fetching line item details on listing page");
            return;
        }
        if(entityFound == false){

            customAssert.assertTrue(false,"Entity id " + entityId + " not found in listing response ");
            return;
        }

        logger.info("Validating currency converted values on line item listing page");
        HashMap<String,Double> columnNameValueMap = new HashMap<>();
        String columnName;
        Double columnValue;

        //Creating a map of column name and value for converted values
        for(int i =0;i<indvDataRowJsonArray.length();i++){

            columnName = indvDataRowJsonArray.getJSONObject(i).getString("columnName");

            if(fieldsToValidateListing.contains(columnName) || convertedFieldsToValidateListing.contains(columnName)){
                columnValue = Double.parseDouble(indvDataRowJsonArray.getJSONObject(i).getString("value"));
                columnNameValueMap.put(columnName,columnValue);
            }
        }

        String nonConvertedField;
        String currencyConvertedField = "";
        Double nonConvertedFieldValue;
        Double currencyConvertedFieldValue;
        Double expectedCurrencyConvertedFieldValue;
        //Validating the currency converted values
        for(int i=0;i<fieldsToValidateListing.size();i++){
            try {
                nonConvertedField = fieldsToValidateListing.get(i);
                currencyConvertedField = convertedFieldsToValidateListing.get(i);

                nonConvertedFieldValue = columnNameValueMap.get(nonConvertedField);
                currencyConvertedFieldValue = columnNameValueMap.get(currencyConvertedField);

                expectedCurrencyConvertedFieldValue = nonConvertedFieldValue * conversionFactor;

                if (!currencyConvertedFieldValue.equals(expectedCurrencyConvertedFieldValue)) {
                    customAssert.assertTrue(false, "Expected and actual value for the field mismatch for " + currencyConvertedField);
                    customAssert.assertTrue(false, "Expected value " + expectedCurrencyConvertedFieldValue + "Actual value " + currencyConvertedFieldValue);
                }
            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while validating for currency converted field " + currencyConvertedField);
            }

        }
    }

    private String getLineItemFilterPayload(Double approvedAmount,Double pendingAmount,Double resolvedAmount){

        String payload = "{\"filterMap\":{\"entityTypeId\":165,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                "{\"254\":{\"filterId\":\"254\",\"filterName\":\"approvedAmount\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                "\"min\":\"" + approvedAmount + "\",\"max\":\"" + approvedAmount + "\",\"suffix\":null}," +
                "\"261\":{\"filterId\":\"261\",\"filterName\":\"pendingAmount\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                "\"min\":\"" + pendingAmount + "\",\"max\":\"" + pendingAmount + "\",\"suffix\":null}," +
                "\"262\":{\"filterId\":\"262\",\"filterName\":\"resolvedAmount\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                "\"min\":\"" + resolvedAmount + "\",\"max\":\"" + resolvedAmount + "\",\"suffix\":null}}}," +
                "\"selectedColumns\":[" +
                "{\"columnId\":13882,\"columnQueryName\":\"id\"}," +
                "{\"columnId\":13906,\"columnQueryName\":\"amount\"}," +
                "{\"columnId\":13909,\"columnQueryName\":\"systemAmount\"}," +
                "{\"columnId\":13912,\"columnQueryName\":\"discrepancyAmount\"}," +
                "{\"columnId\":13907,\"columnQueryName\":\"total\"}," +
                "{\"columnId\":13910,\"columnQueryName\":\"systemTotal\"}," +
                "{\"columnId\":13913,\"columnQueryName\":\"discrepancyTotal\"}," +
                "{\"columnId\":13917,\"columnQueryName\":\"approvedAmount\"}," +
                "{\"columnId\":13918,\"columnQueryName\":\"discrepencyResolutionPendingAmount\"}," +
                "{\"columnId\":13916,\"columnQueryName\":\"discrepancyResolutionAmount\"}," +
                "{\"columnId\":14499,\"columnQueryName\":\"clientAmount\"}," +
                "{\"columnId\":14500,\"columnQueryName\":\"clientTotal\"}," +
                "{\"columnId\":14501,\"columnQueryName\":\"clientSystemAmount\"}," +
                "{\"columnId\":14502,\"columnQueryName\":\"clientSystemTotal\"}," +
                "{\"columnId\":14503,\"columnQueryName\":\"clientDiscrepancyAmount\"}," +
                "{\"columnId\":14504,\"columnQueryName\":\"clientDiscrepancyTotal\"}," +
                "{\"columnId\":14505,\"columnQueryName\":\"clientApprovedAmount\"}," +
                "{\"columnId\":14506,\"columnQueryName\":\"clientDiscrepencyResolutionPendingAmount\"}," +
                "{\"columnId\":14507,\"columnQueryName\":\"clientDiscrepancyResolutionAmount\"}]}";

        return payload;
    }

    private String getInvoiceFilterPayload(String startDate,String endDate,Double invoiceAmount, Double disputeAmount, Double amountApproved){

        String payload = "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":20," +
                "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                "\"filterJson\":{" +
                "\"158\":{\"filterId\":\"158\",\"filterName\":\"invoicePeriodEndDate\"," +
                "\"start\":\"" + endDate + "\",\"end\":\"" + endDate + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                "\"157\":{\"filterId\":\"157\",\"filterName\":\"invoicePeriodStartDate\"," +
                "\"start\":\"" + startDate + "\",\"end\":\"" +  startDate + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                "\"334\":{\"filterId\":\"334\",\"filterName\":\"invoiceAmountVal\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                "\"min\":\"" + invoiceAmount + "\",\"max\":\"" + invoiceAmount + "\",\"suffix\":null}," +
                "\"335\":{\"filterId\":\"335\",\"filterName\":\"disputeAmountVal\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                "\"min\":\"" + disputeAmount + "\",\"max\":\"" + disputeAmount + "\",\"suffix\":null}," +
                "\"336\":{\"filterId\":\"336\",\"filterName\":\"amountApprovedVal\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                "\"min\":\"" + amountApproved + "\",\"max\":\"" + amountApproved + "\",\"suffix\":null}}    },\"selectedColumns\":" +
                "[{\"columnId\":14620,\"columnQueryName\":\"bulkcheckbox\"}," +
                "{\"columnId\":203,\"columnQueryName\":\"id\"}," +
                "{\"columnId\":211,\"columnQueryName\":\"invoiceamount\"}," +
                "{\"columnId\":212,\"columnQueryName\":\"discrepancyamount\"}," +
                "{\"columnId\":225,\"columnQueryName\":\"amountapproved\"}," +
                "{\"columnId\":230,\"columnQueryName\":\"disputeamount\"}," +
                "{\"columnId\":287,\"columnQueryName\":\"clientinvoiceamount\"}," +
                "{\"columnId\":288,\"columnQueryName\":\"clientdiscrepancyamount\"}," +
                "{\"columnId\":289,\"columnQueryName\":\"clientamountapproved\"}," +
                "{\"columnId\":291,\"columnQueryName\":\"clientdisputeamount\"}]}";

        return payload;
    }

    private String getReportFilterPayload(double invoiceAmount,double amountApproved){

        String payload = "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{" +
                "\"57\":{\"filterId\":\"65\",\"filterName\":\"disputeAmount\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"" + invoiceAmount + "\",\"max\":\"" + invoiceAmount + "\",\"suffix\":null}," +
                "\"61\":{\"filterId\":\"61\",\"filterName\":\"amountApproved\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"" + amountApproved + "\",\"max\":\"" + amountApproved + "\",\"suffix\":null}}}," +
                "\"selectedColumns\":[" +
                "{\"columnId\":10256,\"columnQueryName\":\"id\"}," +
                "{\"columnId\":10269,\"columnQueryName\":\"invoiceamount\"}," +
                "{\"columnId\":10270,\"columnQueryName\":\"invoiceamountclientcurrency\"}," +
                "{\"columnId\":10285,\"columnQueryName\":\"amountapproved\"}," +
                "{\"columnId\":10286,\"columnQueryName\":\"amountapprovedclientcuurency\"}," +
                "{\"columnId\":10287,\"columnQueryName\":\"paidamount\"}," +
                "{\"columnId\":10288,\"columnQueryName\":\"paidamountclientcuurency\"}," +
                "{\"columnId\":10289,\"columnQueryName\":\"discrepancyamount\"}," +
                "{\"columnId\":10290,\"columnQueryName\":\"discrepancyamountclientcuurency\"}," +
                "{\"columnId\":10293,\"columnQueryName\":\"resolvediscrepancy\"}," +
                "{\"columnId\":10294,\"columnQueryName\":\"resolvediscrepancyclientcuurency\"}," +
                "{\"columnId\":10295,\"columnQueryName\":\"disputeamount\"}," +
                "{\"columnId\":10296,\"columnQueryName\":\"disputeamountclientcurrency\"}," +
                "{\"columnId\":15846,\"columnQueryName\":\"netsavings\"}," +
                "{\"columnId\":15866,\"columnQueryName\":\"clientnetsavings\"}]}";

        return payload;
    }

    private String convertValue(Double value,Double conversionFactor){

        String covertedValue = null;

        Double roundOfValue = Math.round(value * conversionFactor * 1000.0) / 1000.0;
        covertedValue = roundOfValue.toString();

        if(roundOfValue.toString().contains(".0")){
            Long roundOfValueLong = Math.round(value * conversionFactor * 100) / 100;
            covertedValue = roundOfValueLong.toString();

        }

        return covertedValue;

    }

    private Boolean updateLineItemCurConvertedFields(int entityId,
                                                     Double dRAmountSupplier,
                                                     Double dRAmountClient,
                                                     Double dRAmount,
                                                     Double dRPendingAmount,
                                                     Double approvedAmount,
                                                     int currencyId,
                                                     CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;

        try{

            String entity = "invoice line item";

            editResponse = edit.hitEdit(entity,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyResolutionAmountSupplier").put("values",dRAmountSupplier);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyResolutionAmountClient").put("values",dRAmountClient);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyResolutionAmount").put("values",dRAmount);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepencyResolutionPendingAmount").put("values",dRPendingAmount);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("approvedAmount").put("values",approvedAmount);

            JSONObject valuesJson = editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyReason").getJSONObject("options").getJSONArray("data").getJSONObject(0);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyReason").remove("options");
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyReason").put("values",valuesJson);


            valuesJson = editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepencyResolutionType").getJSONObject("options").getJSONArray("data").getJSONObject(0);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepencyResolutionType").remove("options");
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepencyResolutionType").put("values",valuesJson);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("currency").getJSONObject("values").put("id",currencyId);

            editResponse = edit.hitEdit(entity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Line item updated unsuccessfully");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating line item " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating line item " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;


    }

    private Boolean updateinvoiceCurConvertedFields(int entityId,
                                                    Double invoiceAmount,
                                                    Double amountApproved,
                                                    Double discrepancyAmount,
                                                    Double disputeAmount,
                                                    CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;

        try{

            String entity = "invoices";

            editResponse = edit.hitEdit(entity,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("amountApproved").put("values",amountApproved);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyAmount").put("values",discrepancyAmount);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("disputeAmount").put("values",disputeAmount);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceAmount").put("values",invoiceAmount);

            editResponse = edit.hitEdit(entity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Invoice currency Fields updated unsuccessfully");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating Invoice currency Fields " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating Invoice currency Fields " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;


    }

    private Boolean editCurrencyFields(String entity,int entityId,Map<String,Integer> invoiceCurrencyFieldValuesMap,
                                       CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;

        try{

            editResponse = edit.hitEdit(entity,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            for (Map.Entry<String,Integer> entry : invoiceCurrencyFieldValuesMap.entrySet()){
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject(entry.getKey()).put("values",entry.getValue());
            }

            editResponse = edit.hitEdit(entity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Currency Fields updated unsuccessfully for entity " + entity);
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating currency Fields for entity Id " + entity + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating currency Fields for entity Id " + entity + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;


    }

    private String getShowResponse(int entityTypeId,int entityId){

        Show show = new Show();
        show.hitShowVersion2(entityTypeId,entityId);
        String showResponse = show.getShowJsonStr();

        return showResponse;
    }


    private Map<String,String> getColumnMapForLineItem(String showResponse,Double convFactor,CustomAssert customAssert){

        Map<String,String> expectedColumnNamesAndValues = new HashMap<>();

        try {
            JSONObject showResponseJson = new JSONObject(showResponse);

            try {
                String supplierAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("amount").get("values").toString();
                Double supplierAmountDouble = Double.parseDouble(supplierAmount);
                expectedColumnNamesAndValues.put("amount", supplierAmount);
                expectedColumnNamesAndValues.put("clientAmount", convertValue(supplierAmountDouble, convFactor));
            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "supplierAmount");
            }

//            try {
//                String systemAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("systemAmount").get("values").toString();
//                Double systemAmountDouble = Double.parseDouble(systemAmount);
//                expectedColumnNamesAndValues.put("systemAmount", systemAmount);
//                expectedColumnNamesAndValues.put("clientSystemAmount", convertValue(systemAmountDouble, convFactor));
//            }catch (Exception e){//Error
//                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "systemAmount");
//            }
//            try {
//                String discrepancyAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyAmount").get("values").toString();
//                Double discrepancyAmountDouble = Double.parseDouble(discrepancyAmount);
//                expectedColumnNamesAndValues.put("discrepancyAmount", discrepancyAmount);
//                expectedColumnNamesAndValues.put("clientDiscrepancyAmount", convertValue(discrepancyAmountDouble, convFactor));
//            }catch (Exception e){//Error
//                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "discrepancyAmount");
//            }

//            try {
//                String systemTotal = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("systemTotal").get("values").toString();
//                Double systemTotalDouble = Double.parseDouble(systemTotal);
//                expectedColumnNamesAndValues.put("systemTotal", systemTotal);
//                expectedColumnNamesAndValues.put("clientSystemTotal", convertValue(systemTotalDouble, convFactor));
//            }catch (Exception e){//Error
//                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "systemTotal");
//            }

//            try {
//                String discrepancyTotal = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyTotal").get("values").toString();
//                Double discrepancyTotalDouble = Double.parseDouble(discrepancyTotal);
//                expectedColumnNamesAndValues.put("discrepancyTotal", discrepancyTotal);
//                expectedColumnNamesAndValues.put("clientDiscrepancyTotal", convertValue(discrepancyTotalDouble, convFactor));
//            }catch (Exception e){//Error
//                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "discrepancyTotal");
//            }
            try {
                String supplierTotal = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("total").get("values").toString();
                Double supplierTotalDouble = Double.parseDouble(supplierTotal);
                expectedColumnNamesAndValues.put("total", supplierTotal);
                expectedColumnNamesAndValues.put("clientTotal", convertValue(supplierTotalDouble, convFactor));
            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "supplierTotal");
            }

            try {

                String approvedAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("approvedAmount").get("values").toString();
                Double approvedAmountDouble = Double.parseDouble(approvedAmount);
                expectedColumnNamesAndValues.put("approvedAmount", approvedAmount);
                expectedColumnNamesAndValues.put("clientApprovedAmount", convertValue(approvedAmountDouble, convFactor));
            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "approvedAmount");
            }

            try {
                String discrepencyResolutionPendingAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepencyResolutionPendingAmount").get("values").toString();
                Double discrepencyResolutionPendingAmountDouble = Double.parseDouble(discrepencyResolutionPendingAmount);
                expectedColumnNamesAndValues.put("discrepencyResolutionPendingAmount", discrepencyResolutionPendingAmount);
                expectedColumnNamesAndValues.put("clientDiscrepencyResolutionPendingAmount", convertValue(discrepencyResolutionPendingAmountDouble, convFactor));
            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "discrepencyResolutionPendingAmount");
            }
            try {

                String drAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyResolutionAmount").get("values").toString();
                Double drAmountDouble = Double.parseDouble(drAmount);
                expectedColumnNamesAndValues.put("discrepancyResolutionAmount", drAmount);
                expectedColumnNamesAndValues.put("clientDiscrepancyResolutionAmount", convertValue(drAmountDouble, convFactor));
            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while creating ColumnMapForLineItem for show page field " + "discrepancyResolutionAmount");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while creating Column Map For LineItem");
        }
        return expectedColumnNamesAndValues;
    }

    private Map<String,String> getColumnMapForInvoice(String showResponse,Double convFactor){

        Map<String,String> expectedColumnNamesAndValues = new HashMap<>();

        JSONObject showResponseJson = new JSONObject(showResponse);

        String amountApproved = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("amountApproved").get("values").toString();
        Double amountApprovedDouble = Double.parseDouble(amountApproved);
        expectedColumnNamesAndValues.put("amountapproved",amountApproved);
        expectedColumnNamesAndValues.put("clientamountapproved",convertValue(amountApprovedDouble,convFactor));


        String discrepancyAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyAmount").get("values").toString();
        Double discrepancyAmountDouble = Double.parseDouble(discrepancyAmount);
        expectedColumnNamesAndValues.put("discrepancyamount",discrepancyAmount);
        expectedColumnNamesAndValues.put("clientdiscrepancyamount",convertValue(discrepancyAmountDouble,convFactor));

        String invoiceAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceAmount").get("values").toString();
        Double invoiceAmountDouble = Double.parseDouble(invoiceAmount);
        expectedColumnNamesAndValues.put("invoiceamount",invoiceAmount);
        expectedColumnNamesAndValues.put("clientinvoiceamount", convertValue(invoiceAmountDouble, convFactor));

        String disputeAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("disputeAmount").get("values").toString();
        Double disputeAmountDouble = Double.parseDouble(disputeAmount);
        expectedColumnNamesAndValues.put("disputeamount",disputeAmount);
        expectedColumnNamesAndValues.put("clientdisputeamount", convertValue(disputeAmountDouble, convFactor));

        return expectedColumnNamesAndValues;
    }

    private Map<String,String> getColumnMapForInvoiceReport(String showResponse,Double convFactor){

        Map<String,String> expectedColumnNamesAndValues = new HashMap<>();
        String zero = "0";
        String zerodouble = "0.0";
        Double zeroDouble = 0.0;
        JSONObject showResponseJson = new JSONObject(showResponse);

        try {
            String amountApproved = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("amountApproved").get("values").toString();
            Double amountApprovedDouble = Double.parseDouble(amountApproved);
            expectedColumnNamesAndValues.put("amountapproved", amountApproved);

            if (amountApproved.equals(zero) ||amountApproved.equals(zerodouble)) {
                expectedColumnNamesAndValues.put("amountapproved","0E-12");
                expectedColumnNamesAndValues.put("amountapprovedclientcuurency","0E-12");
            }else {
                expectedColumnNamesAndValues.put("amountapprovedclientcuurency", convertValue(amountApprovedDouble, convFactor));

            }
        }catch (Exception e){
            expectedColumnNamesAndValues.put("amountapproved", zero);
            expectedColumnNamesAndValues.put("amountapprovedclientcuurency", convertValue(zeroDouble, convFactor));
        }

        try {
            String invoiceAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoiceAmount").get("values").toString();
            Double invoiceAmountDouble = Double.parseDouble(invoiceAmount);
            expectedColumnNamesAndValues.put("invoiceamount", invoiceAmount);
            if (invoiceAmount.equals(zero) ||invoiceAmount.equals(zerodouble)) {
                expectedColumnNamesAndValues.put("invoiceamount","0E-12");
                expectedColumnNamesAndValues.put("invoiceamountclientcurrency","0E-12");
            }else {
                expectedColumnNamesAndValues.put("invoiceamountclientcurrency", convertValue(invoiceAmountDouble, convFactor));
            }
        }catch (Exception e){
            expectedColumnNamesAndValues.put("invoiceamount", zero);
            expectedColumnNamesAndValues.put("invoiceamountclientcurrency", convertValue(zeroDouble, convFactor));

        }
        try {
            String paidAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("paidAmount").get("values").toString();
            Double paidAmountDouble = Double.parseDouble(paidAmount);
            expectedColumnNamesAndValues.put("paidamount", paidAmount);

            if (paidAmount.equals(zero) ||paidAmount.equals(zerodouble)) {
                expectedColumnNamesAndValues.put("paidamount","0E-12");
                expectedColumnNamesAndValues.put("paidamountclientcuurency","0E-12");
            }else {
                expectedColumnNamesAndValues.put("paidamountclientcuurency", convertValue(paidAmountDouble, convFactor));
            }
        }catch (Exception e){
            expectedColumnNamesAndValues.put("paidamount", zero);
            expectedColumnNamesAndValues.put("paidamountclientcuurency", convertValue(zeroDouble, convFactor));
        }
        try {
            String discrepancyAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("discrepancyAmount").get("values").toString();
            Double discrepancyAmountDouble = Double.parseDouble(discrepancyAmount);
            expectedColumnNamesAndValues.put("discrepancyamount", discrepancyAmount);

            if (discrepancyAmount.equals(zero) ||discrepancyAmount.equals(zerodouble)) {
                expectedColumnNamesAndValues.put("discrepancyamount","0E-12");
                expectedColumnNamesAndValues.put("discrepancyamountclientcuurency","0E-12");
            }else {
                expectedColumnNamesAndValues.put("discrepancyamountclientcuurency", convertValue(discrepancyAmountDouble, convFactor));
            }
        }catch (Exception e){
            expectedColumnNamesAndValues.put("discrepancyamount", zero);
            expectedColumnNamesAndValues.put("discrepancyamountclientcuurency", convertValue(zeroDouble, convFactor));

        }
        try {
            String resolveDiscrepancyAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("resolvedDiscrepancy").get("values").toString();
            Double resolveDiscrepancyAmountDouble = Double.parseDouble(resolveDiscrepancyAmount);
            expectedColumnNamesAndValues.put("resolvediscrepancy", resolveDiscrepancyAmount);
            if (resolveDiscrepancyAmount.equals(zero) ||resolveDiscrepancyAmount.equals(zerodouble)) {

                expectedColumnNamesAndValues.put("resolvediscrepancy","0E-12");

                expectedColumnNamesAndValues.put("resolvediscrepancyclientcuurency","0E-12");
            }else {
                expectedColumnNamesAndValues.put("resolvediscrepancyclientcuurency", convertValue(resolveDiscrepancyAmountDouble, convFactor));
            }
        }catch (Exception e){
            expectedColumnNamesAndValues.put("resolvediscrepancy", zero);
            expectedColumnNamesAndValues.put("resolvediscrepancyclientcuurency", convertValue(zeroDouble, convFactor));

        }
        try {
            String disputeAmount = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("disputeAmount").get("values").toString();
            Double disputeAmountDouble = Double.parseDouble(disputeAmount);
            expectedColumnNamesAndValues.put("disputeamount", disputeAmount);
            if (disputeAmount.equals(zero) ||disputeAmount.equals(zerodouble)) {
                expectedColumnNamesAndValues.put("disputeamount","0E-12");
                expectedColumnNamesAndValues.put("disputeamountclientcurrency","0E-12");
            }else {
                expectedColumnNamesAndValues.put("disputeamountclientcurrency", convertValue(disputeAmountDouble, convFactor));
            }
        }catch (Exception e){
            expectedColumnNamesAndValues.put("disputeamount", zero);
            expectedColumnNamesAndValues.put("disputeamountclientcurrency", convertValue(zeroDouble, convFactor));
        }
        try {
            String netSavings = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("netSavings").get("values").toString();
            Double netSavingsDouble = Double.parseDouble(netSavings);
            expectedColumnNamesAndValues.put("netsavings", netSavings);
            if (netSavings.equals(zero) ||netSavings.equals(zerodouble)) {
                expectedColumnNamesAndValues.put("netsavings", "0E-12");
                expectedColumnNamesAndValues.put("clientnetsavings","0E-12");
            }else {
                expectedColumnNamesAndValues.put("clientnetsavings", convertValue(netSavingsDouble, convFactor));
            }
        }catch (Exception e){
            expectedColumnNamesAndValues.put("netsavings", zero);
            expectedColumnNamesAndValues.put("clientnetsavings", convertValue(zeroDouble, convFactor));
        }

        return expectedColumnNamesAndValues;
    }

    private boolean validateCurrencyConListingPage(int listId,String payload,Map<String, String> expectedColumnNamesAndValues,CustomAssert customAssert){

        Boolean validationStatus = true;

        try {
            ListRendererListData listRendererListData = new ListRendererListData();

            listRendererListData.hitListRendererListDataV2(listId, payload);
            String listDataResponse = listRendererListData.getListDataJsonStr();

            JSONObject listDataResponseJson = new JSONObject(listDataResponse);

            JSONArray dataArray = listDataResponseJson.getJSONArray("data");

            if (dataArray.length() == 0) {
                customAssert.assertTrue(false, "No records fetched in listing response");
            }else {

                JSONArray indJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(0));
                String columnName;
                String columnValue;


                String expectedValue;
                for (int i = 0; i < indJsonArray.length(); i++) {

                    columnName = indJsonArray.getJSONObject(i).get("columnName").toString();
                    columnValue = indJsonArray.getJSONObject(i).get("value").toString();

                    if (expectedColumnNamesAndValues.containsKey(columnName)) {
                        expectedValue = expectedColumnNamesAndValues.get(columnName);

                        if (!columnValue.contains(expectedValue)) {
                            customAssert.assertTrue(false, "Expected and Actual Did not match for column" + columnName);
                            validationStatus = false;
                        }
                    }

                }
            }
        }catch (Exception e){
            logger.error("Exception while validating listing page");
            customAssert.assertTrue(false,"Exception while validating listing page");
            validationStatus = false;
        }

        return validationStatus;
    }

    private boolean validateCurrencyConReportPage(int reportId,String payload,Map<String, String> expectedColumnNamesAndValues,CustomAssert customAssert){

        Boolean validationStatus = true;

        try {
            ReportRendererListData reportRendererListData = new ReportRendererListData();

            reportRendererListData.hitReportRendererListData(reportId,payload);

            String listDataResponse = reportRendererListData.getListDataJsonStr();

            JSONObject listDataResponseJson = new JSONObject(listDataResponse);

            JSONArray dataArray = listDataResponseJson.getJSONArray("data");

            if (dataArray.length() == 0) {
                customAssert.assertTrue(false, "No records fetched in listing response");
                validationStatus = false;

            }else {

                JSONArray indJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(0));
                String columnName;
                String columnValue;


                String expectedValue;
                for (int i = 0; i < indJsonArray.length(); i++) {

                    columnName = indJsonArray.getJSONObject(i).get("columnName").toString();
                    columnValue = indJsonArray.getJSONObject(i).get("value").toString();

                    if (expectedColumnNamesAndValues.containsKey(columnName)) {
                        expectedValue = expectedColumnNamesAndValues.get(columnName);

                        if (!columnValue.contains(expectedValue)) {
                            customAssert.assertTrue(false, "Expected and Actual Did not match for column" + columnName);
                            validationStatus = false;
                        }
                    }

                }
            }
        }catch (Exception e){
            logger.error("Exception while validating listing page");
            customAssert.assertTrue(false,"Exception while validating listing page");
            validationStatus = false;
        }

        return validationStatus;
    }

    private int lineItemCreation(int contractId,String flowToTest,CustomAssert customAssert){

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        int invoiceId = -1;
        int invoiceLineItemId = -1;
        int consumptionId;

        Map<String,Integer> entityIdMap = new HashMap<String,Integer>();

        ArrayList<Integer> consumptionIds = new ArrayList<>();

        try {
            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId, uniqueDataString);

            if (serviceDataId != -1) {

                serviceDateToDelete.add(serviceDataId);

                entityIdMap.put(serviceData,serviceDataId);
                Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                        flowToTest, serviceDataId,
                        pricingTemplateFilePath, false,
                        "", -1, null,
                        customAssert);

                if (!uploadPricing) {

                    customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);

                }


                boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                // if service data got published
                if (result == true) {

//                    C3835


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

                    consumptionId = consumptionIds.get(0);
                    Double finalConsumption = 100.0;

                    Boolean updateConsumptionStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                    if (!updateConsumptionStatus) {
                        logger.error("Consumption update done unsuccessfully");
                        customAssert.assertTrue(false, "Consumption update done unsuccessfully");
                    } else {
                        Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                        if (!consumptionApprovalStatus) {
                            customAssert.assertTrue(false, "Error while approving consumption");
                        }
                    }

                } else {

                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");
                }

                invoiceId = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, flowToTest);

                invoiceToDelete.add(invoiceId);
                entityIdMap.put(invoices,invoiceId);

                invoiceLineItemId = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId, invoiceId);
                lineItemToDelete.add(invoiceLineItemId);

                entityIdMap.put(invoiceLineItem,invoiceLineItemId);

                flowWithEntityIdMap.put(flowToTest,entityIdMap);

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while creation of invoice entities " + e.getStackTrace());
            logger.error("Exception while creation of line item " + e.getStackTrace());
        }
        return invoiceLineItemId;
    }

    private Boolean updateServiceDataStartDateEndDate(int entityId,String startDate,String endDate,CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;

        try{

            String entity = "service data";

            editResponse = edit.hitEdit(entity,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("startDate").put("values",startDate);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("endDate").put("values",endDate);


            editResponse = edit.hitEdit(entity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Line item updated unsuccessfully");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating line item " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating line item " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;


    }

    private Boolean updateInvoiceLineItemServiceDataStartDateEndDate(int entityId,String startDate,String endDate,CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;

        try{

            String entity = "invoice line item";

            editResponse = edit.hitEdit(entity,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceStartDate").put("values",startDate);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceEndDate").put("values",endDate);


            editResponse = edit.hitEdit(entity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Line item updated unsuccessfully");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating line item " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating line item " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;


    }

    private Map<String,Integer> createInvoiceCurrencyFieldValuesMap(List<String> columnsToEdit){

        Map<String,Integer> invoiceCurrencyFieldValuesMap = new HashMap<>();
        for(String column : columnsToEdit){

            invoiceCurrencyFieldValuesMap.put(column,RandomNumbers.getRandomNumberWithinRangeIndex(100,10000));
        }

        return invoiceCurrencyFieldValuesMap;
    }

    private Boolean updateSpecficFieldsAfterServiceDataIsPublished(int entityId,String startDate,String endDate,CustomAssert customAssert){

        Boolean updateStatus = true;

        Edit edit = new Edit();
        String editResponse;

        try{

            String entity = "serviceData";

            editResponse = edit.hitEdit(entity,entityId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("unitType").getJSONObject("").put("values",startDate);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceEndDate").put("values",endDate);

            editResponse = edit.hitEdit(entity,editResponseJson.toString());

            if(!editResponse.contains("success")) {
                customAssert.assertTrue(false,"Line item updated unsuccessfully");
                updateStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while updating line item " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating line item " + e.getStackTrace());
            updateStatus = false;
        }

        return updateStatus;


    }

    private String updateCustomField(int invoiceId,int customFieldId,CustomAssert customAssert) {

        Edit edit = new Edit();
        String editPayload = edit.getEditPayload(invoices, invoiceId);
        String customFieldValue = "";

        try {

            JSONObject editPayloadJson = new JSONObject(editPayload);
            customFieldValue = DateUtils.getCurrentTimeStamp().replace("_", "");
            customFieldValue = customFieldValue.replace(" ", "");

            Double customFieldValueDouble = Double.parseDouble(customFieldValue);
            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customFieldId).put("values", customFieldValueDouble);

            String editResponse = edit.hitEdit(invoices, editPayloadJson.toString());

            if (!editResponse.contains("success")) {
                customAssert.assertTrue(false, "Error while editing custom field dyn" + customFieldId + " on invoice");

            }

            Show show = new Show();
            show.hitShowVersion2(invoiceEntityTypeId, invoiceId);
            String showResponse = show.getShowJsonStr();
            JSONObject showResponseJson = new JSONObject(showResponse);

            try {
                customFieldValue = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customFieldId).get("values").toString();
            } catch (Exception e) {
                logger.error("Error while getting custom field value");
            }


        }catch (Exception e){

        }
        return customFieldValue;
    }

    //  Getting conversion factor by providing rate card Id currency from and currency To
    private Double getConversionFactor(int rateCardId, int currFrom, int currTo, CustomAssert customAssert) {

        Double conversionFactor = 0.0;
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        try {
            conversionFactor = Double.parseDouble(postgreSQLJDBC.doSelect("select rate_value from rate_card_conversion where rate_card_id = " + rateCardId +
                    " and currency_from = " + currFrom + " and currency_to = " + currTo).get(0).get(0));
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while fetching conversion id from DB " + e.getStackTrace());
        } finally {
            postgreSQLJDBC.closeConnection();
        }
        return conversionFactor;

    }
    //Getting conversion factor if rate not present on contract than client
    private Double getConvFacWRTClient(int entityTypeId,int entityId,int contractId,CustomAssert customAssert){

        Double conversionFactor = 0.0;
        try {
            String effectiveDate = "";

            //If entity is service data then consider service end date
            if(entityTypeId == 64){

                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "enddatevalue");
            }else if(entityTypeId == 165){//To update according to client admin
                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "invoicedatevalue");     //If from client admin invoice end date is chosen
            }else if(entityTypeId == 181){
                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "enddatevalue");     //
            }
            else {
                effectiveDate = ShowHelper.getValueOfField(entityTypeId, entityId, "invoicedatevalue");
            }
            int clientCurrencyId = InvoiceHelper.getClientCurrencyId(clientId,customAssert);

            int invoiceCurrencyId = Integer.parseInt(ShowHelper.getValueOfField(entityTypeId, entityId, "currency id"));

            int rateCardId = InvoiceHelper.getEffectiveRateCard(contractId, datePattern, effectiveDate, customAssert);

            if (rateCardId == 0) {
                rateCardId = InvoiceHelper.getClientRateCard(clientId, customAssert);
            }

            if (rateCardId != 0) {
                conversionFactor = getConversionFactor(rateCardId, invoiceCurrencyId, clientCurrencyId, customAssert);
            } else {
                customAssert.assertTrue(false, "Rate card Id value is 0");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting conversion Factor ");
        }
        return conversionFactor;
    }

    //    C3508 C3772
    private void validate_CusCurr_Conv_ListingShowPage(int entityId,int entityTypeId,int customCurrId,int customCurrFilterId,CustomAssert customAssert){

        try{

            Show show = new Show();
            show.hitShowVersion2(entityTypeId,entityId);
            String showResponse = show.getShowJsonStr();
            JSONObject showResponseJson = new JSONObject(showResponse);

            String customCurrValue = "";
            String customCurrConverted = "";
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
            int parentEntityId = Integer.parseInt(ShowHelper.getValueOfField("contractid",showResponse));
            Double convFactor = getConvFacWRTClient(entityTypeId, entityId, parentEntityId, customAssert);

            try {

                customCurrValue = String.valueOf(RandomNumbers.getRandomNumberWithinRangeIndex(100,10000));
                EntityEditHelper.updateDynamicField(entityName,entityId,"dyn" + customCurrId,Double.parseDouble(customCurrValue),customAssert);

                show.hitShowVersion2(entityTypeId,entityId);
                showResponse = show.getShowJsonStr();
                showResponseJson = new JSONObject(showResponse);

                customCurrValue = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrId).get("values").toString();

            }catch (Exception e){
                logger.error("Exception while fetching custom currency Value from show page response");
                customAssert.assertTrue(false,"Exception while fetching custom currency Value from show page response");
            }

            if(customCurrValue !=null){
                try{
                    customCurrConverted = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customCurrId).get("displayValues").toString();

                }catch (Exception e){
                    customAssert.assertTrue(false,"Exception while fetching custom currency converted Value from show page response");
                }
            }

            String childCurrency = ShowHelper.getValueOfField("currency short code",showResponse);
            String customCurrValueExpected = "";
            if(!childCurrency.equals(clientCurreny)) {

                convFactor = getConvFacWRTClient(entityTypeId, entityId, parentEntityId, customAssert);
                customCurrValueExpected = customCurrValue + " " + childCurrency + " (" + (convFactor * Double.parseDouble(customCurrValue)) + " " + clientCurreny + ")";

            }else {
                customCurrValueExpected = customCurrValue + " " + childCurrency;
            }

            customCurrConverted = customCurrConverted.replace(",", "");
            customCurrConverted = customCurrConverted.replace(".00", ".0");

            if(!customCurrValueExpected.equals(customCurrConverted)){
                logger.error("Custom Currency Field Converted Value not matched Expected Value" + "  Expected Value : " + customCurrValueExpected + " Actual Value : " + customCurrConverted);
                customAssert.assertTrue(false,"Custom Currency Field Converted Value not matched Expected Value" + "  Expected Value : " + customCurrValueExpected + " Actual Value : " + customCurrConverted);
            }

            ListRendererListData listRendererListData = new ListRendererListData();
            int listId = ConfigureConstantFields.getListIdForEntity(entityName);
            String payload;
            if(entityTypeId == 67) {
                payload = "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":1000,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                        "{\"157\":{\"filterId\":\"157\",\"filterName\":\"invoicePeriodStartDate\",\"start\":\"01-01-2000\",\"end\":\"07-19-2040\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                        "\"158\":{\"filterId\":\"158\",\"filterName\":\"invoicePeriodEndDate\",\"start\":\"01-01-2000\",\"end\":\"07-19-2040\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                        "\"" + customCurrFilterId + "\":{\"filterId\":\"" + customCurrFilterId + "\",\"filterName\":\"" + customCurrId + "\",\"entityFieldId\":" + customCurrId + ",\"entityFieldHtmlType\":19,\"min\":\"" + customCurrValue + "\",\"max\":\"" + customCurrValue + "\"}}},\"selectedColumns\":[]}";


            }else {
                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1000,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + customCurrFilterId + "\":{\"filterId\":\"" + customCurrFilterId + "\",\"filterName\":\"" + customCurrId + "\",\"entityFieldId\":" + customCurrId + "," +
                        "\"entityFieldHtmlType\":19,\"min\":\"-" + customCurrValue + "\",\"max\":\"" + customCurrValue + "\",\"suffix\":null}}},\"selectedColumns\":[]}";
            }
            listRendererListData.hitListRendererListDataV2(listId,payload);
            String listResponse = listRendererListData.getListDataJsonStr();
            HashMap<String,String> listingColumnValues = listRendererListData.getColValOfPartRecord(listResponse,String.valueOf(entityId));

            if(listingColumnValues.size() == 0){

                customAssert.assertTrue(false,"Listing Page for entity " + entityName + " doesnot contain the recordId " +
                        entityId + " after applying custom currency filter with custom currency Id as " + customCurrId);
            }else {
                String customCurrValueListingPage = listingColumnValues.get("dyn" + customCurrId);

                if(customCurrValueListingPage == null){
                    customAssert.assertTrue(false,"Custom Currency Field For " + entityName + " Listing Page is null Currency Field Id " + customCurrId);
                }else if(!customCurrValue.equals(customCurrValueListingPage)){
                    customAssert.assertTrue(false,"Custom Currency Field For " + entityName + " Listing Page does not match with " + entityName + " Show Page Currency Field Id " + customCurrId);
                }
                String convCustomCurrValueListingPage = listingColumnValues.get("clientdyn" + customCurrId);

                String expectedCustomCurrValueListingPage = "";

                expectedCustomCurrValueListingPage = String.valueOf(convFactor * Double.parseDouble(customCurrValue));
                expectedCustomCurrValueListingPage = expectedCustomCurrValueListingPage.replace(".0",".00");

                if(convCustomCurrValueListingPage == null){
                    customAssert.assertTrue(false,"Converted Custom Currency Field For " + entityName + " Listing Page is null Currency Field Id " + customCurrId);
                }else if(!convCustomCurrValueListingPage.equals(expectedCustomCurrValueListingPage)){
                    customAssert.assertTrue(false,"Converted Custom Currency Field For " + entityName + " Listing Page does not match with " + entityName + " Show Page Currency Field Id " + customCurrId);
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating Custom Currency Conversion on Listing and ShowPage " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating Custom Currency Conversion on Listing and ShowPage " + e.getStackTrace());
        }
    }
}
