package com.sirionlabs.test.invoice.revalidation;

import com.sirionlabs.api.bulkRevalidate.BulkRevalidate;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class TestRevalidationScenarios {

    private final static Logger logger = LoggerFactory.getLogger(TestRevalidationScenarios.class);
    private String configFilePath;
    private String configFileName;

    private int contractId;

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

    private String publishAction = "publish";
    private String approve = "Approve";
    private String approveInvoice = "ApproveInvoice";
    private String calculateMemo = "Calculatememo";
    private String revalidate = "Revalidate";

    private String invoiceLineItem = "invoice line item";
    private String invoice = "invoices";

    private int serviceDataEntityTypeId = 64;
    private int invoiceEntityTypeId = 67;
    private int invoiceLineItemEntityTypeId = 165;
    private int consumptionsEntityTypeId = 176;

    private String filePath;

    String schedulerHost;
    String user;
    String pass;
    int port;

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestRevalidationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestRevalidationConfigFileName");

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

        filePath = "src\\test\\resources\\TestConfig\\InvoiceLineItem\\Revalidation";

        contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contractid"));

        schedulerHost = "192.168.2.205";
        user = "tomcat7";
        pass = "tomcat7@123";
        port = 22;

    }

    @DataProvider(name = "revalidationFlows", parallel = true)
    public Object[][] revalidationFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "revalidate button flows").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    /*
    * ARC Flow
        SD Creation
        Inv Creation
        Line Item Creation with Service Period not in range
        Line Item status Invalid Service Period
        Update Line Item for Valid Service Period
        Revalidate
        Line Item status Pricing Unavailable
        Upload Pricing on Service Data
        Revalidate
        Line Item status Consumption Unavailable
        Publish Service Data Wait for Consumption Generation
        Revalidate
        Line Item status Consumption Not Approved
        Approve Consumption
        Revalidate
        Line Item status Any of the status [No Discrepency Amount Dscrepecy Total Discrepency
    */
    @Test(dataProvider = "revalidationFlows",enabled = true)
    public void TestRevalidationScenarioLineItem(String flowToTest) {

        CustomAssert customAssert = new CustomAssert();

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        String lineItemStatus;

        ArrayList<Integer> consumptionIds = new ArrayList<>();

        try {
//            revalidateLineItem(7502,customAssert);
            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId, uniqueDataString);

            if (serviceDataId != -1) {

                int invoiceId1 = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, flowToTest);

                int invoiceLineItemId1 = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId, invoiceId1);

                lineItemStatus = "Pricing Unavailable";
                invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);

                invoiceHelper.updateServiceStartAndEndDate(invoiceLineItem,invoiceLineItemId1, "04-01-2018","04-30-2018", customAssert);

                lineItemStatus = "Invalid Service Period";
                invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);


                invoiceHelper.updateServiceStartAndEndDate(invoiceLineItem,invoiceLineItemId1, "03-01-2018","03-31-2018", customAssert);

                lineItemStatus = "Pricing Unavailable";
                invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                        flowToTest, serviceDataId,
                        pricingTemplateFilePath, false,
                        "", -1, null,
                        customAssert);

                if (!uploadPricing) {

                    customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);
                    customAssert.assertAll();
                    return;
                }

//                revalidateLineItem(invoiceLineItemId1,customAssert);
//                lineItemStatus = "Consumption Unavailable";
//                invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
//                checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,customAssert);
//                checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,customAssert);

                if(flowToTest.equals("arc flow 1") || flowToTest.equals("vol pricing flow amount discrepency")) {
                    boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                    // if service data got published
                    if (result == true) {


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

                    } else {

                        logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                        customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                "Hence skipping validation");
                    }

                    if (consumptionIds.size() > 0) {
                        int consumptionId1 = consumptionIds.get(0);

                        Boolean finalConsumptionUpdateStatus = invoiceHelper.updateConsumption(consumptionId1, 100.0, customAssert);

                        if (!finalConsumptionUpdateStatus) {
                            customAssert.assertTrue(false, "Error while updating final consumption");
                        }

                        //Revalidate line item
//                        lineItemStatus = "Consumption Unavailable";
//                        revalidateLineItem(invoiceLineItemId1,customAssert);        //Conversion Factor Unavailable
//                        invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
//                        checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,customAssert);
//                        checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,customAssert);

                        Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionsEntityTypeId, consumptionId1, approve, customAssert);
                        if (!consumptionApprovalStatus) {
                            customAssert.assertTrue(false, "Error while approving consumption");
                        }

                        //Revalidate line item
                        lineItemStatus = "Volume Discrepancy";
                        revalidateLineItem(invoiceLineItemId1,customAssert);
                        invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert); //Conversion Factor Unavailable
                        checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                        checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);

                        //Approve the Line Item Revalidate button should not be there
                        workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId1, approve, customAssert);
                        Boolean revalidateButton = checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,"Line Item Approved",false,customAssert);

                        if(revalidateButton){
                            logger.error("Revalidate button should not come when line item is in approved state");
                            customAssert.assertEquals("Revalidate button on line item is there when line item is in approved state","Revalidate button should not come when line item is in approved state");
                        }
                        revalidateButton = checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,"Invoice Approved",false,customAssert);
                        if(revalidateButton){
                            logger.error("Revalidate button should not come on invoice when line item is in approved state");
                            customAssert.assertEquals("Revalidate button on invoice is there when invoice is in approved state","Revalidate button should not come on invoice when line item is in approved state");
                        }

                        //Approve the Invoice Revalidate button should not be there
                        workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId1, approveInvoice, customAssert);
                        revalidateButton = checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,"Invoice Approved",false,customAssert);

                        if(revalidateButton){
                            logger.error("Revalidate button should not come when invoice is in approved state");
                            customAssert.assertEquals("Revalidate button on invoice is there when invoice is in approved state","Revalidate button should not come when invoice is in approved state");
                        }

                    }
                }


                System.out.println("");
            }
        } catch (Exception e) {
            logger.error("Exception in Main Test Method " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception in Main Test Method " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    @Test(dataProvider = "revalidationFlows",enabled = true)
    public void TestRevalidationScenarioInvoice(String flowToTest) {

        CustomAssert customAssert = new CustomAssert();

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        String lineItemStatus;

        ArrayList<Integer> consumptionIds = new ArrayList<>();

        try {

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId, uniqueDataString);

            if (serviceDataId != -1) {

                int invoiceId1 = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, flowToTest);

                int invoiceLineItemId1 = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId, invoiceId1);

                lineItemStatus = "Pricing Unavailable";
                invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);

                invoiceHelper.updateServiceStartAndEndDate(invoiceLineItem,invoiceLineItemId1, "04-01-2018","04-30-2018", customAssert);

//                revalidateInvoice(invoiceId1,customAssert);  //Commenting as auto validation is happening
                lineItemStatus = "Invalid Service Period";
                invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);


                invoiceHelper.updateServiceStartAndEndDate(invoiceLineItem,invoiceLineItemId1, "03-01-2018","03-31-2018", customAssert);

//                revalidateInvoice(invoiceId1,customAssert); //Commenting as auto validation is happening
                lineItemStatus = "Pricing Unavailable";
                invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                        flowToTest, serviceDataId,
                        pricingTemplateFilePath, false,
                        "", -1, null,
                        customAssert);

                if (!uploadPricing) {

                    customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);
                    customAssert.assertAll();
                    return;
                }

                revalidateInvoice(invoiceId1,customAssert);
                lineItemStatus = "Consumption Unavailable";
                invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);

///*                Start of comment as code not required

                if(flowToTest.equals("arc flow 1") || flowToTest.equals("vol pricing flow amount discrepency")) {
                    boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                    // if service data got published
                    if (result == true) {

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

                    } else {

                        logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                        customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                "Hence skipping validation");
                    }

                    if (consumptionIds.size() > 0) {
                        int consumptionId1 = consumptionIds.get(0);

                        Boolean finalConsumptionUpdateStatus = invoiceHelper.updateConsumption(consumptionId1, 100.0, customAssert);

                        if (!finalConsumptionUpdateStatus) {
                            customAssert.assertTrue(false, "Error while updating final consumption");
                        }

                        //Revalidate line item
                        lineItemStatus = "Consumption Not Approved"; //Consumption Unavailable
                        revalidateInvoice(invoiceId1,customAssert);
                        invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                        checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                        checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);

                        Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionsEntityTypeId, consumptionId1, approve, customAssert);
                        if (!consumptionApprovalStatus) {
                            customAssert.assertTrue(false, "Error while approving consumption");
                        }

                        //Revalidate line item
                        revalidateInvoice(invoiceId1,customAssert); //Comment
                        lineItemStatus = "Volume Discrepancy";
                        invoiceHelper.validateLineItemValidationStatus(invoiceLineItemId1,lineItemStatus,customAssert);
                        checkIfRevalidateButtonIsComing(invoiceId1,invoiceEntityTypeId,invoice,lineItemStatus,true,customAssert);
                        checkIfRevalidateButtonIsComing(invoiceLineItemId1,invoiceLineItemEntityTypeId,invoiceLineItem,lineItemStatus,true,customAssert);


                    }
                }

//*/

            }
        } catch (Exception e) {
            logger.error("Exception in Main Test Method " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception in Main Test Method " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestRevalidationScenarioInvoiceBulk() {

        CustomAssert customAssert = new CustomAssert();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        String revalidateBulkResponseFilePath = "src\\test\\output\\BulkResponse\\Revalidate";
        String revalidateBulkResponseFileName = "Bulk Invoice Revalidation Response Report.xlsx";

        try {

            List<String> lineItemIdToSkip = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk revalidation","line items not for bulk").split(","));
            String invoiceIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk revalidation","invoice ids");
            List<String> invoiceIdList = Arrays.asList(invoiceIds.split(","));
            ArrayList<String> invoiceIdListShortCodeId = new ArrayList<>();
            String approvedInvoiceId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk revalidation","approved invoice id");

            for(String invoiceId : invoiceIdList){

                invoiceIdListShortCodeId.add(ShowHelper.getValueOfField(invoiceEntityTypeId,Integer.parseInt(invoiceId),"short code id"));
            }

            String[] lineItemIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk revalidation","line item ids").split(",");

            Fetch fetch = new Fetch();
            fetch.hitFetch();
            String fetchResponse = fetch.getFetchJsonStr();
            List<Integer> taskIds = UserTasksHelper.getAllTaskIds(fetchResponse);

            String payload = "{\"entityIds\":[" + invoiceIds + "],\"entityTypeId\":67,\"listId\":\"10\"}";
            DateUtils dateUtils = new DateUtils();
            String currentTimeStamp = dateUtils.getCurrentTimeStampDB().get(0).get(0);

            String bulkRevalidateResponse =  BulkRevalidate.bulkRevalidate(payload).getResponseBody();
            String expectedMsg = "Your request has been successfully submitted.";
            if(!bulkRevalidateResponse.contains(expectedMsg)){
                logger.error("Bulk Revalidate does not contain expected response");
                customAssert.assertEquals(bulkRevalidateResponse,expectedMsg,"Bulk Revalidate does not contain expected response");
            }else {

                Thread.sleep(5000);
                fetch.hitFetch();
                fetchResponse = fetch.getFetchJsonStr();
                int newTaskId = UserTasksHelper.getNewTaskId(fetchResponse, taskIds);
                int requestId = UserTasksHelper.getRequestIdFromTaskId(newTaskId);

                Boolean anyRecordFailed = UserTasksHelper.anyRecordFailedInTask(fetchResponse, String.valueOf(newTaskId));

                if (anyRecordFailed == false) {
                    logger.error("During Bulk Revalidate all records passed but some of the records needs to be failed in scheduler Page");
                    customAssert.assertEquals(false, "During Bulk Revalidate all records passed but some of the records needs to be failed in scheduler Page");
                }

                try {

                    for (int i = 0; i < lineItemIds.length; i++) {

                        if(lineItemIdToSkip.contains(lineItemIds[i])){
                            continue;
                        }
                        String sqlQuery = "select payload,status from task where entity_id = " + lineItemIds[i] + " and date_modified > '" + currentTimeStamp + "'";
                        List<List<String>> queryOutput = postgreSQLJDBC.doSelect(sqlQuery);

                        if (queryOutput.size() == 0) {
                            customAssert.assertTrue(false, "Task table has no entry for entity " + lineItemIds[i]);
                            continue;
                        } else if (queryOutput.size() > 1) {
                            customAssert.assertTrue(false, "");
                        }
                        String invoiceId = "";
                        String payloadColumn = queryOutput.get(0).get(0);

                        try {
                            JSONObject payloadColumnJson = new JSONObject(payloadColumn);

                            invoiceId = payloadColumnJson.get("invoiceId").toString();

                        } catch (Exception e) {
                            logger.error("Invoice Id not fetched");
                        }
                        String status = queryOutput.get(0).get(1);

                        if (!status.equals("4")) {
                            logger.error("Bulk Request failed for invoice Id " + invoiceId);
                            customAssert.assertTrue(false, "Bulk Request failed for invoice Id " + invoiceId);
                        }

                    }

                    SCPUtils scpUtils = new SCPUtils(schedulerHost, user, pass, port);

                    FileUtils.deleteFile(revalidateBulkResponseFilePath, revalidateBulkResponseFileName);

                    scpUtils.downloadExcelFile(String.valueOf(requestId), revalidateBulkResponseFileName,revalidateBulkResponseFilePath);

                    if (!FileUtils.fileExists(revalidateBulkResponseFilePath, revalidateBulkResponseFileName)) {
                        customAssert.assertTrue(false, "File Download unsuccessful ");
                    } else {

                        FileUtils.deleteFile(revalidateBulkResponseFilePath, revalidateBulkResponseFileName);

                        Boolean downloadStatus = scpUtils.downloadExcelFile(String.valueOf(requestId), revalidateBulkResponseFileName, revalidateBulkResponseFilePath);

                        if (downloadStatus != true) {
                            customAssert.assertTrue(false,"Bulk Revalidate Response File Downloaded unsuccessfully from search server to local");
                        } else {
                            String sheetName = "Invoice Result";
                            List<String> excelDataRow = XLSUtils.getExcelDataOfOneRow(revalidateBulkResponseFilePath, revalidateBulkResponseFileName, sheetName, 1);

                            if (!excelDataRow.get(0).equals("Bulk ReValidation")) {
                                customAssert.assertTrue(false, "Bulk Revalidate Excel Response Expected Excel data in first row and first column should be Bulk ReValidation ACTUAL : " + excelDataRow.get(0));
                            }

                            excelDataRow = XLSUtils.getExcelDataOfOneRow(revalidateBulkResponseFilePath, revalidateBulkResponseFileName, sheetName, 1);

                            //Validating if 1st Row contains Bulk ReValidation String
                            if (!excelDataRow.get(0).equals("Bulk ReValidation")) {
                                customAssert.assertTrue(false, "Bulk Revalidate Excel Response Expected Excel data in first row and first column should be Bulk ReValidation ACTUAL : " + excelDataRow.get(0));
                            }

                            excelDataRow = XLSUtils.getExcelDataOfOneRow(revalidateBulkResponseFilePath, revalidateBulkResponseFileName, sheetName, 4);
                            //Validating if 4th row contains ID Name Status Reason Column
                            if (!excelDataRow.get(0).equalsIgnoreCase("ID") ||
                                    !excelDataRow.get(1).equalsIgnoreCase("Name") ||
                                    !excelDataRow.get(2).equalsIgnoreCase("Status") ||
                                    !excelDataRow.get(3).equalsIgnoreCase("Reason")) {
                                customAssert.assertTrue(false, "Bulk Revalidate Excel Response Expected Excel data in row 4 doesn't match ACTUAL : " + excelDataRow);
                            }

                            int numOfRows = XLSUtils.getNoOfRows(revalidateBulkResponseFilePath, revalidateBulkResponseFileName, sheetName).intValue();

                            //Validating if invoice id in excel contains all invoices which are submitted through bulk revalidate option
                            List<String> invoiceIdsExcel = XLSUtils.getOneColumnDataFromMultipleRows(revalidateBulkResponseFilePath, revalidateBulkResponseFileName, sheetName, 0, 4, numOfRows);

                            for (int i = 0; i < invoiceIdListShortCodeId.size(); i++) {
                                if (!invoiceIdsExcel.contains(invoiceIdListShortCodeId.get(i))) {
                                    customAssert.assertTrue(false, "Bulk Revalidate Excel Response doesnot contain invoice id " + invoiceIdListShortCodeId.get(i));
                                }
                            }

                            //Validating if invoice names are not null
                            List<String> invoiceIdNameExcel = XLSUtils.getOneColumnDataFromMultipleRows(revalidateBulkResponseFilePath, revalidateBulkResponseFileName, sheetName, 1, 4, numOfRows);
                            for (int i = 5; i < numOfRows; i++) {

                                if (invoiceIdNameExcel.get(i - 5) == null) {
                                    customAssert.assertTrue(false, "Invoice Id Name in Excel NULL at row number " + i);
                                } else if (invoiceIdNameExcel.get(i - 5).equalsIgnoreCase("")) {
                                    customAssert.assertTrue(false, "Invoice Id Name in Excel blank at row number " + i);
                                }
                            }

                            List<String> invoiceIdStatusExcel = XLSUtils.getOneColumnDataFromMultipleRows(revalidateBulkResponseFilePath, revalidateBulkResponseFileName, sheetName, 2, 4, numOfRows);
                            List<String> invoiceIdReasonExcel = XLSUtils.getOneColumnDataFromMultipleRows(revalidateBulkResponseFilePath, revalidateBulkResponseFileName, sheetName, 3, 4, numOfRows);

                            //Validating if 3rd column contains Successful status and 4th column has Reason if 3rd column in in failed status
                            for (int i = 5; i <= numOfRows; i++) {

                                if (invoiceIdStatusExcel.get(i - 5) == null) {
                                    customAssert.assertTrue(false, "Invoice Status in Excel NULL at row number " + i);
                                } else if (invoiceIdStatusExcel.get(i - 5).equalsIgnoreCase("Successful")) {
                                    if (invoiceIdReasonExcel.get(i - 5) == null) {
                                        customAssert.assertTrue(false, "Invoice Reason is NULL for Row Number " + i);
                                    } else if (invoiceIdReasonExcel.get(i - 5).equalsIgnoreCase("")) {
                                        customAssert.assertTrue(false, "Invoice Reason is blank for Row Number " + i);
                                    } else if (!invoiceIdReasonExcel.get(i - 5).equalsIgnoreCase("-")) {
                                        customAssert.assertTrue(false, "Invoice Reason is not - for Row Number " + i + "when status is successful");
                                    }
                                } else if (!invoiceIdStatusExcel.get(i - 5).equalsIgnoreCase("Successful")) {
                                    if (invoiceIdReasonExcel.get(i - 5) == null) {
                                        customAssert.assertTrue(false, "Reason Field is null for row number " + i + " for not success case");
                                    } else {

                                        if(!invoiceIdListShortCodeId.get(i - 5).equals(approvedInvoiceId)) {
                                            customAssert.assertTrue(false, "Revalidate failed for row number " + i + " Invoice Id : " + invoiceIdListShortCodeId.get(i));
                                        }else {
                                            if (!invoiceIdReasonExcel.get(i - 5).equals("Invoice is already approved")) {
                                                customAssert.assertTrue(false,"In the revalidation response excel when invoice is approved then reason field doesnot contain \"Invoice is already approved\"");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception in Main Test Method in Database ");
                    customAssert.assertTrue(false, "Exception in Main Test Method " + e.getStackTrace());
                }


            }

        } catch (Exception e) {
            logger.error("Exception in Main Test Method " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception in Main Test Method " + e.getStackTrace());
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();
    }

    private boolean checkIfRevalidateButtonIsComing(int entityId,int entityTypeId,String entityName,String lineItemStatus,Boolean revalButtonTobePresent,CustomAssert customAssert){

        Boolean validationStatus = false;

        try{

            String actionsResponse = Actions.getActionsV3Response(entityTypeId,entityId);

            if(JSONUtility.validjson(actionsResponse)){

                JSONArray layoutActionsArray = new JSONObject(actionsResponse).getJSONArray("layoutActions");

                for(int i =0;i<layoutActionsArray.length();i++){

                    if(layoutActionsArray.getJSONObject(i).get("name").toString().equals(revalidate)){
                        validationStatus = true;
                    }
                }

                if((validationStatus == false) && (revalButtonTobePresent == true)){
                    logger.error("Revalidate button not found for entity " + entityName + "when line Item Status is " + lineItemStatus);
                    customAssert.assertTrue(false,"Revalidate button not found for entity " + entityName + "when line Item Status is " + lineItemStatus);
                }
            }else {
                logger.error("Actions V3 Response is an invalid Json for entity " + entityName + "when line Item Status is " + lineItemStatus);
                customAssert.assertTrue(false,"Actions V3 Response is an invalid Json for entity " + entityName + "when line Item Status is " + lineItemStatus);
                validationStatus = false;
            }
        }catch (Exception e){
            logger.error("Exception while validating If Revalidate Button Is Coming " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating If Revalidate Button Is Coming " + e.getStackTrace());
            validationStatus = false;
        }
        return validationStatus;
    }

    private void revalidateLineItem(int lineItem,CustomAssert customAssert){
        try{
            String apiUrl = "/invoicelineitems/revalidate";
            String payload = "{\"entityId\":\"" + lineItem + "\"}";
            String actionResponse = Actions.hitActionApi(apiUrl,payload);

            if(!actionResponse.contains("success")){
                customAssert.assertTrue(false,"While re validating line item action API Response does not contain success");
            }


        }catch (Exception e){
            logger.error("Exception occurred while re validating line Item " + e.getStackTrace());

        }
    }

    private void revalidateInvoice(int invoiceId,CustomAssert customAssert){
        try{
            String apiUrl = "/baseInvoice/revalidate";
            String payload = "{\"entityId\":\"" + invoiceId + "\"}";
            String actionResponse = Actions.hitActionApi(apiUrl,payload);

            if(!actionResponse.contains("success")){
                customAssert.assertTrue(false,"While re validating invoice action API Response does not contain success");
            }

        }catch (Exception e){
            logger.error("Exception occurred while re validating Invoice" + e.getStackTrace());

        }
    }

    private void checkEmailInLetterBoxDB(int entityRequested,int entitySucc,int entityFail, String timeStamp,CustomAssert customAssert){

        String dbHost = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        String dbPort = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        String dbName = ConfigureEnvironment.getEnvironmentProperty("dbNameLetterBox");
        String userName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        String dbPassword = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHost, dbPort, dbName, userName, dbPassword);

        try{
            String subject = "Bulk Revalidate Invoice Request Response";

            List<List<String>> sqlOutput = postgreSQLJDBC.doSelect("select body,attachment_name,failed_count from system_emails where subject ilike '%" + subject + "%' and date_created > " + timeStamp);

            String body = sqlOutput.get(0).get(0);

            try{

                if(!body.contains("Entities requested: " + entityRequested)){
                    customAssert.assertTrue(false,"Email Body does not contain " + "Entities requested: " + entityRequested);
                }
                if(!body.contains("Entities successful: " + entitySucc)){
                    customAssert.assertTrue(false,"Email Body does not contain " + "Entities successful: " + entitySucc);
                }
                if(!body.contains("Entities failed: " + entityFail)){
                    customAssert.assertTrue(false,"Email Body does not contain " + "Entities failed: " + entityFail);
                }
            }catch (Exception e){
                logger.error("Exception while validating email body");
            }
            String attachmentName = sqlOutput.get(0).get(1);
            try{
                if(!attachmentName.equals("Bulk Invoice Revalidation Response Report.xlsx")){
                    customAssert.assertTrue(false,"Email Attachment name doesnot contain Bulk Invoice Revalidation Response Report.xlsx");
                }
            }catch (Exception e){
                logger.error("Exception while validating email attachment name");
            }

            try{
                int failedCount = Integer.parseInt(sqlOutput.get(0).get(2));
                if(failedCount != entityFail){
                    customAssert.assertTrue(false,"Expected number of failed count not matched in letter box DB");
                }

            }catch (Exception e){
                logger.error("Exception while validating Failed count in letter box db");
            }
        }catch (Exception e){
            logger.error("Exception while validating email details from letter box " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating email details from letter box " + e.getStackTrace());

        }finally {
            postgreSQLJDBC.closeConnection();
        }
    }
}
