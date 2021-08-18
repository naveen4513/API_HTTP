package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.bulkaction.BulkActionCreate;
import com.sirionlabs.api.bulkaction.BulkActionSave;
import com.sirionlabs.api.clientAdmin.conversionMatrix.ConversionMatrixUpdate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Invoice;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;


//SIR7303
public class Test_BillingDataScenarios extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(Test_BillingDataScenarios.class);

    private String configFilePath;
    private String configFileName;

    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;

    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;
    private String invoiceExtraFieldsConfigFileName;

    private String invoiceLineItemConfigFilePath;
    private String invoiceLineItemConfigFileName;
    private String invoiceLineItemExtraFieldsConfigFileName;

    private String uploadFilePath = "src\\test\\resources\\TestConfig\\Invoice";

    int consumptionEntityTypeId = 176;
    int consumptionListId = 375;
    int serviceDataEntityTypeId = 64;
    int invoiceEntityTypeId = 67;
    int invoiceLineItemEntityTypeId = 165;

    private String approve = "Approve";
    private String publishAction = "publish";

    String serviceDataEntity = "service data";
    String calculateMemo = "Calculatememo";

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFileName");

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

        uploadFilePath = configFilePath;
    }

    @DataProvider(name = "flowsToTestBillingDataGeneration", parallel = false)
    public Object[][] flowsToTestBillingDataGeneration() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstovalidate").split(",");
        String[] excelfilenames = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelfilename").split(",");

        for (int i =0;i<flowsToTest.length;i++) {
            allTestData.add(new Object[]{flowsToTest[i],excelfilenames[i]});
        }
        return allTestData.toArray(new Object[0][]);
    }

//    C151201 C63425  C153467 (C89425 C151313 ToCover)Tested Ok 28 Oct vol pricing has issue
    @Test(enabled = true,dataProvider = "flowsToTestBillingDataGeneration")
    public void Test_BillingDataGeneration_ARC_AND_VolumePricing(String flowToTest,String pricingFileName) {

        CustomAssert customAssert = new CustomAssert();


        InvoiceHelper invoiceHelper = new InvoiceHelper();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        int expectedNumOfBillingIds = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "exp num of billing ids"));
        int expectedNumOfConsumptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "exp num of consumptions"));
        SCPUtils scpUtils = new SCPUtils();
        String bulkTaskFilePath = "/data/temp-session/bulktask/";

        try {

            String pricingSheetName = "Pricing";
            HashMap<Integer, Integer> columnMap = new HashMap<>();
            Boolean editPricingSheet = true;
            columnMap.put(7, 20);
            columnMap.put(8, 20);

            int startRow = 6;
            int numberOfRows = 0;
            int endRow = 0;

            int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "basecontractid"));

            String conv_data_id = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "conversion data id");

            String conversionRate1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "convrate");
            String conversionRate2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 2", "convrate");
            String conversionRate3 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 3", "convrate");
            String conv_matrix_id1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "conversion matrix id");
            String conv_matrix_id2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 2", "conversion matrix id");

            String valid_to = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "valid to");
            String valid_from = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "valid from");

            String filePath = "src/test/resources/TestConfig/ServiceData/ConversionMatrix";
            String convMatrixFileName1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "filename");
            String convMatrixFileName2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 2", "filename");
            String convMatrixFileName3 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 3", "filename");

            updConvMatClientAdmin(valid_to, valid_from, conv_data_id, conv_matrix_id1, filePath, convMatrixFileName1, customAssert);
            updConvMatClientAdmin(valid_to, valid_from, conv_data_id, conv_matrix_id2, filePath, convMatrixFileName2, customAssert);

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, flowToTest, contractId);
            String uniqueDataString = DateUtils.getCurrentTimeStamp();
            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId, uniqueDataString);

            if (serviceDataId == -1) {
                customAssert.assertEquals("Service Data Not Created", "Service Data should be Created");
                customAssert.assertAll();
                return;
            }
            String startDate = "01-01-2019";
            String endDate = "12-31-2019";

            invoiceHelper.updateServiceStartAndEndDate(serviceDataId, startDate, endDate, customAssert);
            invoiceHelper.updateConversionData(serviceDataId, Integer.parseInt(conv_data_id), customAssert);

//            String pricingFileName = "ArcRrcFlow1Pricing.xlsm";
            boolean uploadPricing = true;
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "uploadPricing");
            if (temp != null && temp.trim().equalsIgnoreCase("false"))
                uploadPricing = false;

            if (uploadPricing) {
                Boolean downloadPricingTemplateStatus = invoicePricingHelper.downloadPricingTemplate(uploadFilePath, pricingFileName, serviceDataId);

                if (!downloadPricingTemplateStatus) {
                    customAssert.assertTrue(false, "Error while downloading pricing template");
                } else {
                    String sheetNamePricing = "Pricing";

                    numberOfRows = XLSUtils.getNoOfRows(uploadFilePath, pricingFileName, sheetNamePricing).intValue();
                    endRow = numberOfRows;

                    int startMonth = 1;
                    int year = 2019;
                    String type = null;

                    if (flowToTest.contains("vol pricing")) {
                        type = "Volume Pricing";
                    } else if (flowToTest.contains("arc")) {
                        type = "ARC";
                    }

                    invoicePricingHelper.updateARCSheetPricingTemplate(uploadFilePath, pricingFileName, type, startMonth, year, numberOfRows, customAssert);

                    editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(uploadFilePath, pricingFileName, pricingSheetName, startRow, endRow, columnMap, customAssert);

                    if (!editPricingSheet) {
                        customAssert.assertTrue(false, "Error while editing pricing sheet");
                    }

                    uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                    if (!uploadPricing) {
                        customAssert.assertTrue(false, "Error while pricing upload");

                    }
                }
            }
            boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
            // if service data got published
            if (result != true) {
                customAssert.assertTrue(false, "Error while publishing service data");
            }
            ArrayList<Integer> consumptionIds = new ArrayList<>();
            String serviceDataType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedatatype");
            if (serviceDataType.equals("arc")) {
//                    C89805
                String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, expectedNumOfConsumptions, consumptionIds);

                logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);
                String consumptionIdsStr = "";

                if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                    logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                    customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);
                    customAssert.assertAll();
                    return;

                } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                    logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                    customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    customAssert.assertAll();
                    return;
                }

                if (consumptionIds.size() != expectedNumOfConsumptions) {
                    customAssert.assertTrue(false, "Expected number of consumption " + expectedNumOfConsumptions + " But Actual " + consumptionIds.size());
                } else {

                    for (int i = 0; i < consumptionIds.size(); i++) {
                        int consumptionId = consumptionIds.get(i);
                        consumptionIdsStr = consumptionIdsStr + consumptionId + ",";
                        Double finalConsumption = 200.0;


                        Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                        if (!finalConsumptionUpdationStatus) {
                            customAssert.assertTrue(false, "Final Consumption updated unsuccessfully");

                        } else {
                            if (!flowToTest.equals("arc flow bulk approve consumption")) {

                                Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                                if (!consumptionApprovalStatus) {
                                    customAssert.assertTrue(false, "Error while approving consumption");

                                }
                            }
                        }

                    }
                    consumptionIdsStr = consumptionIdsStr.substring(0, consumptionIdsStr.length() - 1);

                    if (flowToTest.equals("arc flow bulk approve consumption")) {

                        scpUtils.runChmodCommand("000", bulkTaskFilePath);
                        Boolean bulkActionStatus = bulkActionStatus(flowToTest, consumptionIdsStr, customAssert);

                        if (!bulkActionStatus) {

                            scpUtils.runChmodCommand("777", bulkTaskFilePath);
                            customAssert.assertTrue(false, "Bulk Approve of consumptions happened unsuccessfully for the flow " + flowToTest);
                            customAssert.assertAll();
                            return;
                        }
                    }
                }
                logger.info("Thread sleeping for 60 seconds to wait for billing data generation");
                Thread.sleep(60000);

                int numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataId, customAssert);


                if (numberOfBillingIds == 0) {scpUtils.runChmodCommand("777", bulkTaskFilePath);
                    logger.error("Billing records not generated in time for service Data ID " + serviceDataId);
                    customAssert.assertTrue(false, "Billing records not generated in time for service Data ID " + serviceDataId);
                    customAssert.assertAll();
                    return;
                }

                if (numberOfBillingIds != expectedNumOfBillingIds) {
                    logger.error("Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                    customAssert.assertTrue(false, "Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                }

                if (flowToTest.equals("arc flow bulk approve consumption")) {
                    scpUtils.runChmodCommand("777", bulkTaskFilePath);
                    customAssert.assertAll();
                    return;
                }
                if (serviceDataType.equals("arc")) {
//                C153440  Check billing data updation after updation is done in consumption
                    validate_C153440_NormalEditScenario("normal edit scenario", serviceDataId, consumptionIds, customAssert);

                    validate_C153440_BulkUpdateScenario("bulk update scenario", serviceDataId, consumptionIdsStr, customAssert);
                }
            }else { //Fixed fee scenario
                Thread.sleep(120000);
                int numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataId, customAssert);


                if (numberOfBillingIds == 0) {
                    logger.error("Billing records not generated in time for service Data ID " + serviceDataId);
                    customAssert.assertTrue(false, "Billing records not generated in time for service Data ID " + serviceDataId);
                    customAssert.assertAll();
                    return;
                }

                if (numberOfBillingIds != expectedNumOfBillingIds) {
                    logger.error("Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                    customAssert.assertTrue(false, "Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                }

            }
            Map<String, Map<String, String>> billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);

            //Jan 2019 to Jun 2019
            //Jul 2019 to Dec 2019
            int breakMonth = 7;
            Boolean validationStatusConvRateReportListPage = validateConversionRate(billingRecordAccToStartDate, conversionRate1, conversionRate2, breakMonth, customAssert);

            if (!validationStatusConvRateReportListPage) {
                customAssert.assertTrue(false, "Conversion Rate validated unsuccessfully on ReportListing Page before Matrix Change");
            }

            //Billing data generation
            updConvMatClientAdmin(valid_to, valid_from, conv_data_id, conv_matrix_id1, filePath, convMatrixFileName3, customAssert);

            logger.info("Waiting for 60 seconds for billing data regeneration");
            Thread.sleep(60000);
            billingRecordAccToStartDate.clear();
            billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);
//                        validationStatusConvRateReportListPage = validateConversionRate(billingRecordAccToStartDate,conversionRate1,conversionRate3,breakMonth,customAssert);
            validationStatusConvRateReportListPage = validateConversionRate(billingRecordAccToStartDate, conversionRate3, conversionRate2, breakMonth, customAssert);

            if (!validationStatusConvRateReportListPage) {
                customAssert.assertTrue(false, "Conversion Rate validated unsuccessfully on ReportListing Page after Matrix Change");
            }

            //Check memo billing data is generated with Pricing Update with CCR

            int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName,
                    invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest);

            String invoiceDate = startDate;
            invoiceHelper.updateInvoiceDate(invoiceId, invoiceDate, customAssert);

            int invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId);

            String lineItemEndDate = "01-31-2019";
            invoiceHelper.updateInvoiceLineItemDate(invoiceLineItemId, startDate, lineItemEndDate, customAssert);

            if (invoiceId == -1 || invoiceLineItemId == -1) {
                customAssert.assertTrue(false, "Either of invoice ID or line item value is -1");
                customAssert.assertAll();
                return;
            } else {
                String approveInvoice = "ApproveInvoice";
                workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId, approveInvoice);

                workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, approve);

                //Check updation of Billing data on updating service data metadata
                String memoReason = "Discrepancy";

//                            Check memo billing data is not generated
                validate_C88807(serviceDataId, memoReason, customAssert);

                columnMap = new HashMap<>();

                columnMap.put(7, 30);
                columnMap.put(8, 30);

                startRow = 6;
                String entityNamePricingColNum = "11653";
                String dataSheetName = "Data";
                invoicePricingHelper.downloadPricingTemplate(uploadFilePath, pricingFileName, serviceDataId);
                int entityNamePricingColNo = XLSUtils.getExcelColNumber(uploadFilePath, pricingFileName, dataSheetName, 2, entityNamePricingColNum);

                endRow = numberOfRows;
                editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(uploadFilePath, pricingFileName, pricingSheetName, startRow, endRow, columnMap, customAssert);

                if (!editPricingSheet) {
                    customAssert.assertTrue(false, "Error while editing pricing sheet for memo billing CCR case");
                }

                String changeRequestVal = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "change request value");
                //Updating CCR Value
                XLSUtils.updateColumnValue(uploadFilePath, pricingFileName, "Data", startRow - 1, entityNamePricingColNo, changeRequestVal);

                uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                if (!uploadPricing) {
                    customAssert.assertTrue(false, "Error while pricing upload for memo billing case");

                } else {

                    workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, calculateMemo);


                    memoReason = "Discrepancy";
//                                Check memo billing data is generated with Pricing Update with CCR
                    validate_C88814(serviceDataId, memoReason, customAssert);
                }
            }

            //Check discrepancy memo billing data is generated adjustment type Line Item
            validate_C88813(serviceDataId, invoiceLineItemId, customAssert);

            //Chvpeck memo billing data is generated with Pricing Update with CDR
//                        validate_C90587();

            //Check updation of Billing data on updating service data metadata
            validate_C89297(serviceDataId, customAssert);

//                        Check deletion of Billing data on updating service data metadata
            expectedNumOfBillingIds = 0; //As Discrepancy Billing Records are not deleted
            validate_C151248(serviceDataId, expectedNumOfBillingIds, customAssert);


        } catch (Exception e) {
            logger.error("Exception while validating the scenario " + e.getMessage());
            customAssert.assertTrue(false, "Exception while validating the scenario " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //C151202 C89392 C89391 (C89399 C89424 To cover)
    // invoice date scenario
    @Test(enabled = false)//In Progress Scenario To Automate 28 Oct
    public void Test_C151202(){

        CustomAssert customAssert = new CustomAssert();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        String flowToTest = "arc flow 1";
        String type = null;
        if(flowToTest.contains("vol pricing")){
            type = "Volume Pricing";
        }else if(flowToTest.contains("arc")){
            type = "ARC";
        }
        try{

            int contractId =Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"basecontractid"));
            int expectedNumOfConsumptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "exp num of consumptions"));

            String conv_data_id = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conv matrix 1","conversion data id");

            String conversionRate1 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conv matrix 1","convrate");
            String conversionRate2 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conv matrix 2","convrate");
            String conversionRate3 = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"conv matrix 3","convrate");
            String conv_matrix_id1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "conversion matrix id");
            String conv_matrix_id2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 2", "conversion matrix id");

            String valid_to = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "valid to");
            String valid_from = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "valid from");

            String filePath = "src/test/resources/TestConfig/ServiceData/ConversionMatrix";
            String convMatrixFileName1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 1", "filename");
            String convMatrixFileName2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 2", "filename");
            String convMatrixFileName3 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "conv matrix 3", "filename");

            updConvMatClientAdmin(valid_to, valid_from, conv_data_id, conv_matrix_id1, filePath, convMatrixFileName1, customAssert);
            updConvMatClientAdmin(valid_to, valid_from, conv_data_id, conv_matrix_id2, filePath, convMatrixFileName2, customAssert);

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flowToTest, contractId);
            String uniqueDataString = DateUtils.getCurrentTimeStamp();
            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId,uniqueDataString);
            //Updating currency Conversion Date as Invoice Date
            int convDateType = 2;
            invoiceHelper.updateConversionDate(serviceDataId,convDateType,customAssert);

            String startDate = "01-01-2019";
            String endDate = "12-31-2019";
            String pricingSheetName = "Pricing";
            HashMap<Integer, Integer> columnMap = new HashMap<>();

            columnMap.put(7, 20);
            columnMap.put(8, 20);

            int startRow = 6;
            int numberOfRows;
            int endRow = 0;

            invoiceHelper.updateServiceStartAndEndDate(serviceDataId,startDate,endDate,customAssert);
            invoiceHelper.updateConversionData(serviceDataId,Integer.parseInt(conv_data_id),customAssert);

            String pricingFileName = "ArcRrcFlow1Pricing.xlsm";

            Boolean downloadPricingTemplateStatus =  invoicePricingHelper.downloadPricingTemplate(uploadFilePath,pricingFileName,serviceDataId);

            if(!downloadPricingTemplateStatus){
                customAssert.assertTrue(false,"Error while downloading pricing template");
            }else {
                String sheetNamePricing = "Pricing";

                numberOfRows = XLSUtils.getNoOfRows(uploadFilePath, pricingFileName, sheetNamePricing).intValue();

                int startMonth = 1;
                int year = 2019;
                invoicePricingHelper.updateARCSheetPricingTemplate(uploadFilePath, pricingFileName,type, startMonth, year, numberOfRows, customAssert);

                Boolean editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(uploadFilePath, pricingFileName, pricingSheetName, startRow, endRow, columnMap, customAssert);

                if (!editPricingSheet) {
                    customAssert.assertTrue(false, "Error while editing pricing sheet");
                }

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                if (!uploadPricing) {
                    customAssert.assertTrue(false, "Error while pricing upload");

                } else {
                    boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                    // if service data got published
                    if (result != true) {
                        customAssert.assertTrue(false, "Error while publishing service data");
                    }
                    ArrayList<Integer> consumptionIds = new ArrayList<>();
//                        C89805
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, 12, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    }

                    if (consumptionIds.size() != expectedNumOfConsumptions) {
                        customAssert.assertTrue(false, "Unable to get new consumption Id");
                    } else {

                        for (int i = 0; i < consumptionIds.size(); i++) {
                            int consumptionId = consumptionIds.get(i);
                            Double finalConsumption = 200.0;


                            Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                            if (!finalConsumptionUpdationStatus) {
                                customAssert.assertTrue(false, "Final Consumption updated unsuccessfully");

                            } else {

                                Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                                if (!consumptionApprovalStatus) {
                                    customAssert.assertTrue(false, "Error while approving consumption");

                                }
                            }
                        }
                        logger.info("Thread sleeping for 5 minutes to wait for billing data generation");
                        Thread.sleep(60000);
                        int numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataId,customAssert);

                        int expectedNumOfBillingIds = 12;

                        if(numberOfBillingIds != expectedNumOfBillingIds){
                            logger.error("Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                            customAssert.assertTrue(false,"Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                        }

                        Map<String,Map<String,String>> billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId,customAssert);

                        //Jan 2019 to Jun 2019
                        //Jul 2019 to Dec 2019

                        int breakMonth = 7;
                        Boolean validationStatusConvRateReportListPage = validateConversionRate(billingRecordAccToStartDate,conversionRate1,conversionRate2,breakMonth,customAssert);

                        if(!validationStatusConvRateReportListPage){
                            customAssert.assertTrue(false,"Conversion Rate validated unsuccessfully on ReportListing Page before Matrix Change");
                        }

                        //Billing data generation
                        updConvMatClientAdmin(valid_to, valid_from, conv_data_id, conv_matrix_id1, filePath, convMatrixFileName3, customAssert);


                        logger.info("Waiting for 3 minutes for billing data regeneration");
                        Thread.sleep(18000);
                        billingRecordAccToStartDate.clear();
                        billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId,customAssert);
                        validationStatusConvRateReportListPage = validateConversionRate(billingRecordAccToStartDate,conversionRate1,conversionRate3,breakMonth,customAssert);

                        if(!validationStatusConvRateReportListPage){
                            customAssert.assertTrue(false,"Conversion Rate validated unsuccessfully on ReportListing Page after Matrix Change");
                        }

                    }
                }

            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getMessage());
        }

        customAssert.assertAll();
    }


    //C63424 C3426
    @Test(enabled = false)
    public void Test_SplitServiceDataScenarios() {

        CustomAssert customAssert = new CustomAssert();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        String flowToTest = "arc flow 1";
        String splitConsDateOnWhichBillingDataIsCreated = "06-01-2019";
        String attributeValue1 = "Automation 1";
        String attributeValue2 = "Automation 2";
        int expectedNumberOfConsumption = 24;
        try {

            int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName, "basecontractid"));

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, flowToTest, contractId);
            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId, uniqueDataString);

            String startDate = "01-01-2019";
            String endDate = "12-31-2019";

            String splitAttributeType = "3";   //Department
            String splitRatioType = "2";       //Percentage

            invoiceHelper.updateServiceStartAndEndDate(serviceDataId, startDate, endDate, customAssert);
            invoiceHelper.updateSplitAttributesServiceData(serviceDataId, splitAttributeType, splitRatioType, customAssert);


            String uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceData\\BillingData";
            String pricingFileName = "ArcRrcFlow1PricingSplitServiceData.xlsm";

            HashMap<String,String> showPageColumnNameValuesSD = getShowPageColumnNameValuesServiceDataId(serviceDataId);

            String serviceDataIdShortCode = showPageColumnNameValuesSD.get("shortCodeId");
            String serviceDataIdClient = showPageColumnNameValuesSD.get("serviceIdClient");

            Boolean updationPricingUploadSheetStatus = updateSplitPricingSheet(uploadFilePath,pricingFileName,serviceDataIdShortCode,serviceDataIdClient,customAssert);

            if(!updationPricingUploadSheetStatus){
                logger.error("Error while updating Pricing upload sheet");
                customAssert.assertTrue(false,"Error while updating Pricing upload sheet");
            }

            Boolean uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

            if (!uploadPricing) {
                customAssert.assertTrue(false, "Error while pricing upload");

            } else {
                boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                // if service data got published
                if (result != true) {
                    customAssert.assertTrue(false, "Error while publishing service data");
                }
                ArrayList<Integer> consumptionIds = new ArrayList<>();
//                        C89805
                String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, expectedNumberOfConsumption, consumptionIds);

                logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                    logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                    customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                    logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                    customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                }

                if (consumptionIds.size() != expectedNumberOfConsumption) {
                    customAssert.assertTrue(false, "Unable to get new consumption Id");
                } else {
                    String consumptionStartDate = null;
                    for (int i = 0; i < consumptionIds.size(); i++) {
                        int consumptionId = consumptionIds.get(i);
                        Double finalConsumption = 200.0;

                        consumptionStartDate = getconsumptionStartDate(consumptionId,customAssert);


                        if(consumptionStartDate.equals(splitConsDateOnWhichBillingDataIsCreated)) {
                            Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                            if (!finalConsumptionUpdationStatus) {
                                customAssert.assertTrue(false, "Final Consumption updated unsuccessfully");

                            } else {

                                Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                                if (!consumptionApprovalStatus) {
                                    customAssert.assertTrue(false, "Error while approving consumption");

                                }
                            }
                        }
                    }

                    logger.info("Thread sleeping for 3 minutes to wait for billing data generation");
                    Thread.sleep(180000);
                    int numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataId, customAssert);

                    int expectedNumOfBillingIds = 2;

                    if (numberOfBillingIds != expectedNumOfBillingIds) {
                        logger.error("Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                        customAssert.assertTrue(false, "Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                    }

                    Map<String, Map<String, String>> billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);

                    billingRecordAccToStartDate.clear();

                    billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);
                    int expectedNumOfBilRecGenerated = 2;

                    if(billingRecordAccToStartDate.size() !=expectedNumOfBilRecGenerated){
                        customAssert.assertTrue(false,"Expected number of billing records generated " + expectedNumOfBillingIds + " Actual number of billing records generated " + billingRecordAccToStartDate.size());
                    }else {
                        String actualAttributeValue1 = billingRecordAccToStartDate.get(splitConsDateOnWhichBillingDataIsCreated).get("attributevalue");
                        String actualAttributeValue2 = billingRecordAccToStartDate.get(splitConsDateOnWhichBillingDataIsCreated + "_1").get("attributevalue");

                        if((actualAttributeValue1.equals(attributeValue1) && actualAttributeValue2.equals(attributeValue2))
                        || (actualAttributeValue1.equals(attributeValue2) && actualAttributeValue2.equals(attributeValue1))){

                            logger.info("Attribute Value value validated successfully for billing data");
                        }else {
                            logger.error("Attribute Value value validated unsuccessfully for billing data");
                            customAssert.assertTrue(false,"Attribute Value value validated unsuccessfully for billing data");
                        }
                    }

                }
            }


        } catch (Exception e) {
            logger.error("Exception while validating the scenario " + e.getMessage());
            customAssert.assertTrue(false, "Exception while validating the scenario " + e.getMessage());
        }

        customAssert.assertAll();
    }

  /*  Commenting as covered in test_SDHeirarcyFlows

    //C63426 Tested Ok 7 Oct 2020
    @Test(enabled = false)
    public void Test_BillingDataGenerationForParentChildHierarchy() {

        CustomAssert customAssert = new CustomAssert();

        String parentClientId;

        Create create = new Create();
        Clone clone = new Clone();

        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        JSONObject valuesJson = new JSONObject();

        int newServiceDataIdChild = -1;
        int serviceDataIdParent = -1;

        String flowToTest = "arc flow 1";

        String pricingFileName = "ArcRrcFlow1Pricing.xlsm";
        String splitConsDateOnWhichBillingDataIsCreated = "06-01-2019";
        try {
            int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName, "basecontractid"));
            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            serviceDataIdParent = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, uniqueDataString);

            String startDate = "01-01-2019";
            String endDate = "12-31-2019";

            invoiceHelper.updateServiceStartAndEndDate(serviceDataIdParent, startDate, endDate, customAssert);
            invoiceHelper.updateSDisBilAndPricingAvail(serviceDataIdParent,false,false,customAssert);

            if (serviceDataIdParent == -1) {
                throw new SkipException("Skipping the test as Service Data Id is null");
            }

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
            dataJson.getJSONObject("billingAvailable").put("values", true);
            dataJson.getJSONObject("pricingAvailable").put("values", true);

            dataJson.remove("history");

            JSONObject createEntityJson = new JSONObject();
            JSONObject createEntityBodyJson = new JSONObject();
            createEntityBodyJson.put("data", dataJson);
            createEntityJson.put("body", createEntityBodyJson);

            create.hitCreate(serviceDataEntity, createEntityJson.toString());
            String createResponse = create.getCreateJsonStr();
            newServiceDataIdChild = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            if (newServiceDataIdChild == -1) {
                logger.error("Unable to create child service data");
                customAssert.assertTrue(false, "Unable to create child service data");
                customAssert.assertAll();
                return;
            }


            Boolean downloadPricingTemplateStatus = invoicePricingHelper.downloadPricingTemplate(uploadFilePath, pricingFileName, newServiceDataIdChild);

            if (!downloadPricingTemplateStatus) {
                customAssert.assertTrue(false, "Error while downloading pricing template");
            } else {
                String sheetNamePricing = "Pricing";

                int numberOfRows = XLSUtils.getNoOfRows(uploadFilePath, pricingFileName, sheetNamePricing).intValue();

                int startMonth = 1;
                int year = 2019;
                String type = null;


//            String pricingFileName = "ArcRrcFlow1Pricing.xlsm";
                if (flowToTest.contains("vol pricing")) {
                    type = "Volume Pricing";
                } else if (flowToTest.contains("arc")) {
                    type = "ARC";
                }

                invoicePricingHelper.updateARCSheetPricingTemplate(uploadFilePath, pricingFileName, type, startMonth, year, numberOfRows, customAssert);

                String pricingSheetName = "Pricing";
                HashMap<Integer, Integer> columnMap = new HashMap<>();

                columnMap.put(7, 20);
                columnMap.put(8, 20);

                int startRow = 6;
                int endRow = numberOfRows;
                Boolean editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(uploadFilePath, pricingFileName, pricingSheetName, startRow, endRow, columnMap, customAssert);

                if (!editPricingSheet) {
                    customAssert.assertTrue(false, "Error while editing pricing sheet");
                }

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                if (!uploadPricing) {

                    customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);
                    customAssert.assertAll();
                } else {

                    boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, newServiceDataIdChild, publishAction, customAssert);
                    // if service data got published
                    if (result != true) {
                        customAssert.assertTrue(false, "Error while publishing service data");
                    }
                    ArrayList<Integer> consumptionIds = new ArrayList<>();
//                        C89805
                    int expectedNumberOfConsumption = 12;
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, newServiceDataIdChild, expectedNumberOfConsumption, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    }

                    if (consumptionIds.size() != expectedNumberOfConsumption) {
                        customAssert.assertTrue(false, "Unable to get new consumption Id");
                    } else {
                        String consumptionStartDate = null;
                        for (int i = 0; i < consumptionIds.size(); i++) {
                            int consumptionId = consumptionIds.get(i);
                            Double finalConsumption = 200.0;

                            consumptionStartDate = getconsumptionStartDate(consumptionId, customAssert);


                            if (consumptionStartDate.equals(splitConsDateOnWhichBillingDataIsCreated)) {
                                Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                                if (!finalConsumptionUpdationStatus) {
                                    customAssert.assertTrue(false, "Final Consumption updated unsuccessfully");

                                } else {

                                    Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                                    if (!consumptionApprovalStatus) {
                                        customAssert.assertTrue(false, "Error while approving consumption");

                                    }
                                }
                            }
                        }

                        logger.info("Thread sleeping for 3 minutes to wait for billing data generation");
                        Thread.sleep(60000);
                        int numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataIdParent, customAssert);

                        int expectedNumOfBillingIds = 0;

                        if (numberOfBillingIds != expectedNumOfBillingIds) {
                            logger.error("For parent Service Data Expected number of billing records not created Expected No OF billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                            customAssert.assertTrue(false, "For parent Service Data Expected number of billing records not created Expected No Of billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                        }

                        numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(newServiceDataIdChild, customAssert);

                        expectedNumOfBillingIds = 12;

                        if (numberOfBillingIds != expectedNumOfBillingIds) {
                            logger.error("For parent Service Data Expected number of billing records not created Expected No OF billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                            customAssert.assertTrue(false, "For parent Service Data Expected number of billing records not created Expected No Of billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                        }

                        Map<String, Map<String, String>> billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(newServiceDataIdChild, customAssert);

//                        C63452
                        String billingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFilePath");
                        String billingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestBillingDataFileName");

                        invoiceHelper.validateBillingDataListingPageData(newServiceDataIdChild,consumptionIds.get(0),"","",
                                billingConfigFilePath,billingConfigFileName,
                                billingRecordAccToStartDate,true,"For the flow Parent Child Service Data", customAssert);


//                        C89376 C151313
                        validateAmountInInvoicingCurrency(billingRecordAccToStartDate,customAssert);


                    }

                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception in main test method");
        }

        customAssert.assertAll();
    }
Commenting as covered in test_SDHeirarcyFlows
*/

/*Commenting as covered in test_SDHeirarcyFlows
    //C90587 C89805     //C90587 7OCt 2020 Failing at last step CDR linking after calc memo
    @Test(enabled = false)
    public void TestMemoBillingDataGenWithPricingUpdateWithCDR() {

        CustomAssert customAssert = new CustomAssert();


        InvoiceHelper invoiceHelper = new InvoiceHelper();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        String flowToTest = "arc flow 1";
        try {
//            int serviceDataId1 = 22139;
//            int invoiceLineItemId1 = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId1);

            int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName, "basecontractid"));

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, flowToTest, contractId);
            String uniqueDataString = DateUtils.getCurrentTimeStamp();
            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId, uniqueDataString);

            String startDate = "01-01-2019";
            String endDate = "03-31-2019";

            invoiceHelper.updateServiceStartAndEndDate(serviceDataId, startDate, endDate, customAssert);

            String pricingFileName = "ArcRrcFlow1Pricing.xlsm";

            Boolean downloadPricingTemplateStatus = invoicePricingHelper.downloadPricingTemplate(uploadFilePath, pricingFileName, serviceDataId);
            ArrayList<Integer> alreadyApprovedConsumption = new ArrayList<>();
            if (!downloadPricingTemplateStatus) {
                customAssert.assertTrue(false, "Error while downloading pricing template");
            } else {
                String sheetNamePricing = "Pricing";

                int numberOfRows = XLSUtils.getNoOfRows(uploadFilePath, pricingFileName, sheetNamePricing).intValue();

                int startMonth = 1;
                int year = 2019;
                String type = null;

                if (flowToTest.contains("vol pricing")) {
                    type = "Volume Pricing";
                } else if (flowToTest.contains("arc")) {
                    type = "ARC";
                }

                invoicePricingHelper.updateARCSheetPricingTemplate(uploadFilePath, pricingFileName, type, startMonth, year, numberOfRows, customAssert);

                String pricingSheetName = "Pricing";
                HashMap<Integer, Integer> columnMap = new HashMap<>();

                columnMap.put(7, 20);
                columnMap.put(8, 20);

                int startRow = 6;
                int endRow = numberOfRows;
                Boolean editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(uploadFilePath, pricingFileName, pricingSheetName, startRow, endRow, columnMap, customAssert);

                if (!editPricingSheet) {
                    customAssert.assertTrue(false, "Error while editing pricing sheet");
                }

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                if (!uploadPricing) {
                    customAssert.assertTrue(false, "Error while pricing upload");

                } else {
                    boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                    // if service data got published
                    if (result != true) {
                        customAssert.assertTrue(false, "Error while publishing service data");
                    }
                    ArrayList<Integer> consumptionIds = new ArrayList<>();
//                        C89805
                    int numOfCons = 3;
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, numOfCons, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    }

                    if (consumptionIds.size() != numOfCons) {
                        customAssert.assertTrue(false, "Unable to get new consumption Id");
                    } else {

                        for (int i = 0; i < consumptionIds.size(); i++) {
                            int consumptionId = consumptionIds.get(i);
                            Double finalConsumption = 200.0;


                            Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                            if (!finalConsumptionUpdationStatus) {
                                customAssert.assertTrue(false, "Final Consumption updated unsuccessfully");

                            } else {

                                Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                                if (!consumptionApprovalStatus) {
                                    customAssert.assertTrue(false, "Error while approving consumption");

                                }else {alreadyApprovedConsumption.add(consumptionId);}
                            }
                        }
                        logger.info("Thread sleeping for 1 minutes to wait for billing data generation");
                        Thread.sleep(60000);
                        int numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataId, customAssert);

                        int expectedNumOfBillingIds = numOfCons;

                        if (numberOfBillingIds == 0) {
                            logger.error("Billing records not generated in time for service Data ID " + serviceDataId);
                            customAssert.assertTrue(false, "Billing records not generated in time for service Data ID " + serviceDataId);
                            customAssert.assertAll();
                            return;
                        }

                        if (numberOfBillingIds != expectedNumOfBillingIds) {
                            logger.error("Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                            customAssert.assertTrue(false, "Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                        }

                        startDate = "01-01-2019";
                        endDate = "12-31-2019";
                        invoiceHelper.updateServiceStartAndEndDate(serviceDataId, startDate, endDate, customAssert);
                        //Jan 2019 to Jun 2019
                        //Jul 2019 to Dec 2019

                        numOfCons = 12;
                        consumptionIds.clear();
                        consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, numOfCons, consumptionIds);

                        logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                        if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                            logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                            customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                        } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                            logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                            customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                        }

                        if (consumptionIds.size() != numOfCons) {
                            customAssert.assertTrue(false, "Expected number of consumption regenerated not equal to actual Expected " + numOfCons + " Actual " + consumptionIds.size());
                        } else {

                            downloadPricingTemplateStatus = invoicePricingHelper.downloadPricingTemplate(uploadFilePath, pricingFileName, serviceDataId);

                            if (!downloadPricingTemplateStatus) {
                                customAssert.assertTrue(false, "Error while downloading pricing template");
                            } else {
                                sheetNamePricing = "Pricing";

                                numberOfRows = XLSUtils.getNoOfRows(uploadFilePath, pricingFileName, sheetNamePricing).intValue();

                                startMonth = 1;
                                year = 2019;
                                type = null;

                                if (flowToTest.contains("vol pricing")) {
                                    type = "Volume Pricing";
                                } else if (flowToTest.contains("arc")) {
                                    type = "ARC";
                                }

                                invoicePricingHelper.updateARCSheetPricingTemplate(uploadFilePath, pricingFileName, type, startMonth, year, numberOfRows, customAssert);

                                pricingSheetName = "Pricing";
                                columnMap = new HashMap<>();

                                columnMap.put(7, 20);
                                columnMap.put(8, 20);

                                startRow = 6;
                                endRow = numberOfRows;
                                editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(uploadFilePath, pricingFileName, pricingSheetName, startRow, endRow, columnMap, customAssert);

                                if (!editPricingSheet) {
                                    customAssert.assertTrue(false, "Error while editing pricing sheet");
                                }

                                uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                                if (!uploadPricing) {
                                    customAssert.assertTrue(false, "Error while pricing upload");
                                } else {
                                    for (int i = 0; i < consumptionIds.size(); i++) {
                                        int consumptionId = consumptionIds.get(i);
                                        Double finalConsumption = 200.0;


                                        Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                                        if (!finalConsumptionUpdationStatus) {
                                            customAssert.assertTrue(false, "Final Consumption updated unsuccessfully");

                                        } else {
                                            if(!alreadyApprovedConsumption.contains(consumptionId)){
                                            Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                                            if (!consumptionApprovalStatus) {
                                                customAssert.assertTrue(false, "Error while approving consumption After Regeneration");

                                            }}
                                        }
                                    }

                                    logger.info("Thread sleeping for 1 minutes to wait for billing data generation");
                                    Thread.sleep(60000);
                                    numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataId, customAssert);

                                    expectedNumOfBillingIds = numOfCons;

                                    if (numberOfBillingIds == 0) {
                                        logger.error("Billing records not re generated in time for service Data ID " + serviceDataId + " After service start date and end date update");
                                        customAssert.assertTrue(false, "Billing records not re generated in time for service Data ID " + serviceDataId + " After service start date and end date update");
                                        customAssert.assertAll();
                                        return;
                                    }

                                    if (numberOfBillingIds != expectedNumOfBillingIds) {
                                        logger.error("Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                                        customAssert.assertTrue(false, "Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                                    } else {
                                        Map<String, Map<String, String>> billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);


                                    }
                                    int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName,
                                            invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest);

                                    String invoiceDate = startDate;
                                    invoiceHelper.updateInvoiceDate(invoiceId, invoiceDate, customAssert);

                                    int invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId);

                                    String lineItemEndDate = "01-31-2019";
                                    invoiceHelper.updateInvoiceLineItemDate(invoiceLineItemId, startDate, lineItemEndDate, customAssert);

                                    if (invoiceId == -1 || invoiceLineItemId == -1) {
                                        customAssert.assertTrue(false, "Either of invoice ID or line item value is -1");
                                    } else {
                                        String approveInvoice = "ApproveInvoice";
                                        workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId, approveInvoice);

                                        workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, approve);

                                        //Check updation of Billing data on updating service data metadata
                                        String memoReason = "Discrepancy";

                                        columnMap = new HashMap<>();

                                        columnMap.put(7, 30);
                                        columnMap.put(8, 30);

                                        startRow = 6;
                                        String entityNamePricingColNum = "11653";
                                        String dataSheetName = "Data";

                                        int entityNamePricingColNo = XLSUtils.getExcelColNumber(uploadFilePath, pricingFileName, dataSheetName, 2, entityNamePricingColNum);

                                        endRow = numberOfRows;
                                        editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(uploadFilePath, pricingFileName, pricingSheetName, startRow, endRow, columnMap, customAssert);

                                        if (!editPricingSheet) {
                                            customAssert.assertTrue(false, "Error while editing pricing sheet for memo billing CCR case");
                                        }

                                        String cdrVal = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdr value");
                                        //Updating CDR Value
                                        XLSUtils.updateColumnValue(uploadFilePath, pricingFileName, "Data", startRow - 1, entityNamePricingColNo, cdrVal);

                                        uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                                        if (!uploadPricing) {
                                            customAssert.assertTrue(false, "Error while pricing upload for memo billing case");

                                        } else {

                                            workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemId, calculateMemo);

                                            memoReason = "Discrepancy";
//                                  Check memo billing data is generated with Pricing Update with CCR
                                            validate_C88814(serviceDataId, memoReason, customAssert);

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while validating the scenario " + e.getMessage());
            customAssert.assertTrue(false, "Exception while validating the scenario " + e.getMessage());
        }

        customAssert.assertAll();
    }

Commenting as covered in test_SDHeirarcyFlows
*/
    // C91035 (Check OK on 28 Oct 2020)
    @Test(enabled = false)
    public void TestNegCaseBillingDataNotGenerated(){

        CustomAssert customAssert = new CustomAssert();

        InvoiceHelper invoiceHelper = new InvoiceHelper();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        String flowToTest = "arc flow 1";
        try{

            int contractId =Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"basecontractid"));

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, flowToTest, contractId);
            String uniqueDataString = DateUtils.getCurrentTimeStamp();
            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId,uniqueDataString);

            String startDate = "01-01-2019";
            String endDate = "04-30-2019";
            int expNoOfConsumptions = 4;
            String pricingFileName = "ArcRrcFlow1Pricing.xlsm";

            invoiceHelper.updateServiceStartAndEndDate(serviceDataId,startDate,endDate,customAssert);
            invoiceHelper.updateIsBillableAndPricingForReporting(serviceDataId,true,true,customAssert);

//            String pricingFileName = "ArcRrcFlow1Pricing.xlsm";

            Boolean downloadPricingTemplateStatus =  invoicePricingHelper.downloadPricingTemplate(uploadFilePath,pricingFileName,serviceDataId);

            if(!downloadPricingTemplateStatus){
                customAssert.assertTrue(false,"Error while downloading pricing template");
            }else {
                String sheetNamePricing = "Pricing";

                int numberOfRows = XLSUtils.getNoOfRows(uploadFilePath, pricingFileName, sheetNamePricing).intValue();

                int startMonth = 1;
                int year = 2019;
                String type = null;

                if(flowToTest.contains("vol pricing")){
                    type = "Volume Pricing";
                }else if(flowToTest.contains("arc")){
                    type = "ARC";
                }

                invoicePricingHelper.updateARCSheetPricingTemplate(uploadFilePath, pricingFileName,type, startMonth, year, numberOfRows, customAssert);

                String pricingSheetName = "Pricing";
                HashMap<Integer, Integer> columnMap = new HashMap<>();

                columnMap.put(7, 20);
                columnMap.put(8, 20);

                int startRow = 6;
                int endRow = numberOfRows;
                Boolean editPricingSheet = invoicePricingHelper.editPricingFilePricingSheet(uploadFilePath, pricingFileName, pricingSheetName, startRow, endRow, columnMap, customAssert);

                if (!editPricingSheet) {
                    customAssert.assertTrue(false, "Error while editing pricing sheet");
                }

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                if (!uploadPricing) {
                    customAssert.assertTrue(false, "Error while pricing upload");

                } else {
                    boolean result = workflowActionsHelper.performWorkFlowStepV2(serviceDataEntityTypeId, serviceDataId, publishAction, customAssert);
                    // if service data got published
                    if (result != true) {
                        customAssert.assertTrue(false, "Error while publishing service data");
                    }
                    ArrayList<Integer> consumptionIds = new ArrayList<>();
//                        C89805
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, 4, consumptionIds);

                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                    }

                    if (consumptionIds.size() != 4) {
                        customAssert.assertTrue(false, "Unable to get new consumption Id");
                    } else {

                        for (int i = 0; i < consumptionIds.size(); i++) {
                            int consumptionId = consumptionIds.get(i);
                            Double finalConsumption = 200.0;


                            Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, finalConsumption, customAssert);

                            if (!finalConsumptionUpdationStatus) {
                                customAssert.assertTrue(false, "Final Consumption updated unsuccessfully");

                            } else {

                                Boolean consumptionApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionId, approve, customAssert);
                                if (!consumptionApprovalStatus) {
                                    customAssert.assertTrue(false, "Error while approving consumption");

                                }
                            }
                        }
                        logger.info("Thread sleeping for 1 minutes to wait for billing data generation");
                        Thread.sleep(60000);
                        int numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataId,customAssert);

                        int expectedNumOfBillingIds = 0;

                        if(numberOfBillingIds != expectedNumOfBillingIds){
                            logger.error("Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                            customAssert.assertTrue(false,"Expected number of billing records not created Expected billing Data " + expectedNumOfBillingIds + " Actual Billing Ids " + numberOfBillingIds);
                        }

                    }
                }

            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }
        customAssert.assertAll();
    }

    /*Commenting as covered in test_SDHeirarcyFlows

//    C153434 Check billing records after pricing updation with different versions CCR and CDR
//    Have disabled the test will restart after SIR-14139 is resolved
    @Test(enabled = false)
    public void Test_C153434(){

        CustomAssert customAssert = new CustomAssert();
        String testCaseId = "C153434";

        try{
            String scenario = "Billing Data For Fixed Fee Type";
            String flowToTest = "fixed fee flow 2";
            String pricingFileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"","pricingstemplatefilename");
            String sheetNameData = "Pricing";

            int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "basecontractid"));

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, flowToTest, contractId);
            String uniqueDataString = DateUtils.getCurrentTimeStamp();
            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId, uniqueDataString);

            InvoiceHelper invoiceHelper = new InvoiceHelper();
            HashMap<String,Map<String,String>> billingRecords = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId,customAssert);
            int expectedNumOfBillingRec = 1;
            if(billingRecords.size() == 0){
                customAssert.assertTrue(false,"Billing Records not generated for the scenario " + scenario);
            }else if(billingRecords.size() == expectedNumOfBillingRec){
                for (Map.Entry<String, Map<String, String>> entry : billingRecords.entrySet()) {

                    Map<String, String> billingRecord = entry.getValue();

                    valBillRecForPricingVerColNullVal(billingRecord,customAssert);

                    validate_C151321(serviceDataId,billingRecord,customAssert);
                }
            }
            InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();

            Boolean downloadPricingTemplateStatus = invoicePricingHelper.downloadPricingTemplate(uploadFilePath, pricingFileName, serviceDataId);

            if (!downloadPricingTemplateStatus) {
                customAssert.assertTrue(false, "Error while downloading pricing template");
            } else {
                int colNum = XLSUtils.getExcelColNumber(uploadFilePath,pricingFileName,sheetNameData,2,"11653");
                String change_request_value = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"change request value");

                XLSUtils.updateColumnValue(uploadFilePath,pricingFileName,sheetNameData, 6,colNum ,change_request_value);

                Boolean uploadPricing = invoicePricingHelper.uploadPricingFileWithoutEdit(uploadFilePath, pricingFileName, flowToTest, customAssert);

                if (!uploadPricing) {
                    customAssert.assertTrue(false, "Error while pricing upload After CCR Change");
                }
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating test case id " + testCaseId);
        }

        customAssert.assertAll();
    }
Commenting as covered in test_SDHeirarcyFlows
*/


//    To implement C153467 C153440 C153434 C153505
    private void updConvMatClientAdmin(String validTo,String validFrom,
                                          String convDataId,String convMatrixId,
                                          String filePath,String fileName,
                                          CustomAssert customAssert){

        Check check = new Check();
        try{

            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

            check.hitCheck(adminUserName,adminPassword);

            ConversionMatrixUpdate conversionMatrixUpdate = new ConversionMatrixUpdate();

            Map<String,String> payloadMap = new HashMap<>();
            payloadMap.put("validTo",validTo);
            payloadMap.put("validFrom",validFrom);
            payloadMap.put("conversionDataId",convDataId);
            payloadMap.put("conversionMatrixId",convMatrixId);

            conversionMatrixUpdate.hitConversionMatrixUpdate(filePath,fileName,payloadMap);
            String updateResponse = conversionMatrixUpdate.updateResponse;

            if(!updateResponse.contains("200")){
                customAssert.assertTrue(false,"Error while conversion Matrix Upload");
            }


        }catch (Exception e){
            logger.error("Exception while updating ConversionMatrix on Client Admin");
            customAssert.assertTrue(false,"Exception while updating ConversionMatrix on Client Admin");
        }finally {

            String endUserUserName = ConfigureEnvironment.getEnvironmentProperty("j_username");
            String endUserPassword = ConfigureEnvironment.getEnvironmentProperty("password");

            check.hitCheck(endUserUserName,endUserPassword);

        }

    }

    private String getReportPayload(int serviceDataId){

        String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"" +
                "orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                "{\"248\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + serviceDataId +
                "\",\"name\":\"servicedataarcflow1 (newSupplier2020_05_11 15_15_27_433)\"}]}," +
                "\"filterId\":248,\"filterName\":\"serviceData\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":18652,\"columnQueryName\":\"servicedataid\"},{\"columnId\":18630,\"columnQueryName\":\"servicedataname\"},{\"columnId\":18631,\"columnQueryName\":\"contract\"},{\"columnId\":18761,\"columnQueryName\":\"conversionRate\"}]}";

        return payload;
    }

    private Boolean validateConversionRate(Map<String,Map<String,String>> billingRecordAccToStartDate,String conversionRate1,String conversionRate2,int expectedMonth,CustomAssert customAssert){

        Boolean validationStatus = true;
        int month;
        Map<String,String> columnValues;
        String actualConversionRate;
        try {
            for (Map.Entry<String, Map<String, String>> entry : billingRecordAccToStartDate.entrySet()) {
                month = Integer.parseInt(entry.getKey().split("-")[1]);
                columnValues = entry.getValue();

                actualConversionRate = columnValues.get("conversionRate");
//                actualConversionRate = columnValues.get("conversionRateAsOnDate");

                if(month < expectedMonth && !actualConversionRate.equals(conversionRate1)){
                    customAssert.assertTrue(false,"For the month of " + month + " expected conversion rate not matched " + " Expected Conversion Rate " + conversionRate1);
                    validationStatus = false;
                }else if(month >= expectedMonth && !actualConversionRate.equals(conversionRate2)){
                    customAssert.assertTrue(false,"For the month of " + month + " expected conversion rate not matched " + " Expected Conversion Rate " + conversionRate2);
                    validationStatus = false;
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Conversion Rate " + e.getMessage());
            validationStatus = false;
        }
        return  validationStatus;
    }


    private boolean updateSplitPricingSheet(String uploadFilePath,String uploadFileName,String serviceDataId,String serviceDataIdClient,CustomAssert customAssert){

        Boolean updateSplitPricingSheet = true;
        try{
            String dataSheet = "Data";
            String pricingSheet = "Pricing";
            String arcrrcSheet = "ARCRRC";
            String splitSheet = "Split";

            String columnID_id = "4037";
            String columnID_serviceIdClient = "11040";

            int columnNumberDataSheet_Id = 1;
            int columnNumberDataSheet_ServiceIDClient = 4;

            ///*********Updating Data Sheet Pricing Upload *********************///
            List<String> columnIdsDataSheet = XLSUtils.getExcelDataOfOneRow(uploadFilePath,uploadFileName,dataSheet,2);

            for(int i=0;i<columnIdsDataSheet.size();i++){
                if(columnIdsDataSheet.get(i).equals(columnID_id)){
                    columnNumberDataSheet_Id = i;
                }
                if(columnIdsDataSheet.get(i).equals(columnID_serviceIdClient)){
                    columnNumberDataSheet_ServiceIDClient = i;
                }
            }

            int rowNumberForDataSheet = 6;
            XLSUtils.updateColumnValue(uploadFilePath,uploadFileName,dataSheet,rowNumberForDataSheet,columnNumberDataSheet_Id,serviceDataId);
            XLSUtils.updateColumnValue(uploadFilePath,uploadFileName,dataSheet,rowNumberForDataSheet,columnNumberDataSheet_ServiceIDClient,serviceDataIdClient);

            ///***************************************************************************//

            ///*********Updating Pricing Sheet Pricing Upload///////////////
            int columnNumberPricingSheet_Id = 2;
            int columnNumberPricingSheet_ServiceIDClient = 4;

            List<String> columnIdsPricingSheet = XLSUtils.getExcelDataOfOneRow(uploadFilePath,uploadFileName,pricingSheet,2);

            for(int i=0;i<columnIdsPricingSheet.size();i++){
                if(columnIdsPricingSheet.get(i).equals(columnID_id)){
                    columnNumberPricingSheet_Id = i;
                }
                if(columnIdsPricingSheet.get(i).equals(columnID_serviceIdClient)){
                    columnNumberPricingSheet_ServiceIDClient = i;
                }
            }
            ///********************************************************************//

            ///*********Updating ARC RRC Sheet Pricing Upload*********************//
            List<String> columnIdsARCRRCSheet = XLSUtils.getExcelDataOfOneRow(uploadFilePath,uploadFileName,pricingSheet,2);

            for(int i=0;i<columnIdsARCRRCSheet.size();i++){
                if(columnIdsARCRRCSheet.get(i).equals(columnID_id)){
                    columnNumberPricingSheet_Id = i;
                }
                if(columnIdsARCRRCSheet.get(i).equals(columnID_serviceIdClient)){
                    columnNumberPricingSheet_ServiceIDClient = i;
                }
            }

            Map<Integer, Map<Integer, Object>> columnDataMap = new HashMap<>();
            Map<Integer, Object> columnValuesMap = new HashMap<>();

            for(int i =6;i<=17;i++){
                columnValuesMap = new HashMap<>();

                columnValuesMap.put(columnNumberPricingSheet_Id,serviceDataId);
                columnValuesMap.put(columnNumberPricingSheet_ServiceIDClient,serviceDataIdClient);
                columnDataMap.put(i,columnValuesMap);

                XLSUtils.editMultipleRowsData(uploadFilePath,uploadFileName,pricingSheet,columnDataMap);
                XLSUtils.editMultipleRowsData(uploadFilePath,uploadFileName,arcrrcSheet,columnDataMap);

            }
            ///********************************************************************//

            ///*********Updating Split Sheet Pricing Upload********************//

            int columnNumberSplitSheet_Id = 2;
            int columnNumberSplitSheet_ServiceIDClient = 4;
            List<String> columnIdsSplitSheet = XLSUtils.getExcelDataOfOneRow(uploadFilePath,uploadFileName,splitSheet,2);

            for(int i=0;i<columnIdsSplitSheet.size();i++){
                if(columnIdsSplitSheet.get(i).equals(columnID_id)){
                    columnNumberSplitSheet_Id = i;
                }
                if(columnIdsSplitSheet.get(i).equals(columnID_serviceIdClient)){
                    columnNumberSplitSheet_ServiceIDClient = i;
                }
            }

            XLSUtils.updateColumnValue(uploadFilePath,uploadFileName,splitSheet,6,columnNumberSplitSheet_Id,serviceDataId);
            XLSUtils.updateColumnValue(uploadFilePath,uploadFileName,splitSheet,6,columnNumberSplitSheet_ServiceIDClient,serviceDataIdClient);
            XLSUtils.updateColumnValue(uploadFilePath,uploadFileName,splitSheet,7,columnNumberSplitSheet_Id,serviceDataId);
            XLSUtils.updateColumnValue(uploadFilePath,uploadFileName,splitSheet,7,columnNumberSplitSheet_ServiceIDClient,serviceDataIdClient);
            ///********************************************************************//

        }catch (Exception e){
            logger.error("Exception while updating Pricing Sheet");
            customAssert.assertTrue(false,"Exception while updating Pricing Sheet");
            updateSplitPricingSheet = false;
        }
        return  updateSplitPricingSheet;

    }

    public HashMap<String,String> getShowPageColumnNameValuesServiceDataId(int serviceDataId){

        HashMap<String,String> showPageColumnNameValues = new HashMap<>();
        String showResponse;
        try{
            Show show = new Show();
            show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
            showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id",showResponse);
            String serviceIdClient = ShowHelper.getValueOfField("serviceidclient",showResponse);

            showPageColumnNameValues.put("shortCodeId",shortCodeId);
            showPageColumnNameValues.put("serviceIdClient",serviceIdClient);


        }catch (Exception e){
            logger.error("Exception while getting showPage ColumnName Values from ServiceData Id");
        }

        return showPageColumnNameValues;
    }

    public String getconsumptionStartDate(int consumptionId,CustomAssert customAssert){

        String startDate = null;
        try{
            Show show = new Show();
            show.hitShowVersion2(consumptionEntityTypeId,consumptionId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJson = new JSONObject(showResponse);
            startDate = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("consumptionStartDate").get("values").toString();


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while getting consumption start date");
        }

        return startDate;
    }

    public boolean validateBillingDataListingPageData(int serviceDataId,Map<String, Map<String, String>> billingRecordAccToStartDate,Boolean parentHeirarchyToBeChecked, CustomAssert customAssert){

        Boolean validationBillingDataListingPage = true;
        try{

            Show show = new Show();
            show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
            String showResponse = show.getShowJsonStr();

            Map<String,String> listColNameValue = new HashMap<>();
            String listColumnName;
            String listColumnValue;
            String showPageColumnName;
            String showPageColumnValue;
            String parentName = "null";
            int parentId = -1;

            for (Map.Entry<String,Map<String,String>> entry : billingRecordAccToStartDate.entrySet()){

                entry.getKey();
                listColNameValue = entry.getValue();


                for (Map.Entry<String,String> entry1 : listColNameValue.entrySet()) {

                    listColumnName = entry1.getKey();
                    listColumnValue = entry1.getValue();

                    if(parentHeirarchyToBeChecked && listColumnName.equals("parentid")){
                        listColumnValue = entry1.getValue();

                        if(listColumnValue == null){
                            customAssert.assertTrue(false,"Parent Service Data Id not found in child service data");
                        }else {
                            try{
                                parentId = Integer.parseInt(listColumnValue.split(":;")[1]);
                            }catch (Exception e){
                                customAssert.assertTrue(false,"Exception while getting parent service data id from child service data in Billing Data Report");
                            }
                        }
                    }

                    if(parentHeirarchyToBeChecked && listColumnName.equals("parentname")){
                        listColumnValue = entry1.getValue();

                        if(listColumnValue == null){
                            customAssert.assertTrue(false,"Parent Service Data Name not found in child service data");
                        }else {
                            try{
                                parentName = listColumnValue;
                            }catch (Exception e){
                                customAssert.assertTrue(false,"Exception while getting parent service data name from child service data in Billing Data Report");
                            }
                        }
                    }

                    showPageColumnName =  ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"show page mapping listing page",listColumnName.toLowerCase());

                        if(showPageColumnName == null){
                        logger.info("Show Page Field not defined for listing Column Name " + listColumnName);
                        continue;
                    }
                    else if(showPageColumnName.equals("")){
                        logger.info("Show Page Field Value not defined for listing Column Name " + listColumnName);
                        continue;
                    }else {

                        try {

                            showPageColumnValue = ShowHelper.getValueOfField(showPageColumnName, showResponse);

                            if(listColumnName.equals("country") || listColumnName.equals("region")) {
                                String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageColumnName,serviceDataEntityTypeId);
                                List<String> fieldValuesList = ShowHelper.getAllSelectValuesOfField(showResponse, showPageColumnName,fieldHierarchy,serviceDataId,serviceDataEntityTypeId);
                                listColumnValue = listColumnValue.toLowerCase();
                                for(int i = 0;i<fieldValuesList.size();i++){

                                    if(!listColumnValue.contains(fieldValuesList.get(i).toLowerCase())){
                                        customAssert.assertTrue(false,"For listing column Name " + listColumnName + " listing page does not contain the value "  + fieldValuesList.get(i));
                                    }
                                }

                            }else if(listColumnName.toLowerCase().equals("service data mrole_group")){
                                String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageColumnName,serviceDataEntityTypeId);
                                List<String> fieldValuesList = ShowHelper.getAllSelectedStakeholdersFromShowResponse(showResponse);
                                listColumnValue = listColumnValue.toLowerCase();
                                for(int i = 0;i<fieldValuesList.size();i++){

                                    if(!listColumnValue.contains(fieldValuesList.get(i).toLowerCase())){
                                        customAssert.assertTrue(false,"For listing column Name " + listColumnName + " listing page does not contain the value "  + fieldValuesList.get(i));
                                    }
                                }
                            }else if(listColumnName.equals("servicedataname")){
                                String showPageValue = ShowHelper.getValueOfField("name",showResponse) + " (" + ShowHelper.getValueOfField("serviceclient",showResponse) + ")";

                                if(!listColumnValue.equals(showPageValue)){
                                    customAssert.assertTrue(false, "Listing and Show Page value validated unsuccessfully for listing column " + listColumnName);
                                }
                            }else {
                                if(listColumnValue.contains(":;")){
                                    listColumnValue = listColumnValue.split(":;")[0];
                                }
                                if (!listColumnValue.equals(showPageColumnValue)) {
                                    customAssert.assertTrue(false, "Listing and Show Page value validated unsuccessfully for listing column " + listColumnName);
                                }
                            }

                        }catch (Exception e){
                            customAssert.assertTrue(false,"Exception while validating Listing and Show Page value for listing column " + listColumnName);
                        }
                    }

                }

                if(parentHeirarchyToBeChecked){
                    show.hitShowVersion2(serviceDataEntityTypeId,parentId);
                    String showResponseParent = show.getShowJsonStr();

                    String showPageValue = ShowHelper.getValueOfField("name",showResponseParent) + " (" + ShowHelper.getValueOfField("serviceclient",showResponseParent) + ")";

                    if(!parentName.equals(showPageValue)){
                        customAssert.assertTrue(false, "Listing and Show Page value validated unsuccessfully for listing column parent name");
                    }
                }
                break;
            }



        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Billing Data Listing Page Data");
            validationBillingDataListingPage = false;
        }

        return validationBillingDataListingPage;

    }

    public Boolean validateAmountInInvoicingCurrency(Map<String, Map<String, String>> billingRecordAccToStartDate,CustomAssert customAssert){

        Boolean validationStatus = true;

        try{
            String conversionRate=null;
            String amountInServiceDataCurrency =null;
            String amountInInvoicingCurrencyAsOfDate=null;

            Double conversionRateDouble = 0.0;
            Double amountInServiceDataCurrencyDouble = 0.0;
            Double amountInInvoicingCurrencyAsOfDateDouble = 0.0;

            String listColumnName;
            String listColumnValue;

            Map<String,String> listColNameValue;

            for (Map.Entry<String,Map<String,String>> entry : billingRecordAccToStartDate.entrySet()) {

                entry.getKey();
                listColNameValue = entry.getValue();


                for (Map.Entry<String, String> entry1 : listColNameValue.entrySet()) {

                    listColumnName = entry1.getKey();
                    listColumnValue = entry1.getValue();

                    if(listColumnName.equals("amountInServiceDataCurrency")){
                        amountInServiceDataCurrency = listColumnValue;
                    }
                    if(listColumnName.equals("conversionRate")){
                        conversionRate = listColumnValue;
                    }
                    if(listColumnName.equals("amountInInvoicingCurrencyAsOfDate")){
                        amountInInvoicingCurrencyAsOfDate = listColumnValue;
                    }

                }
                if(amountInServiceDataCurrency == null){
                    customAssert.assertTrue(false,"Amount in service data currency is null");
                }else {
                    amountInInvoicingCurrencyAsOfDateDouble = convertValueToDouble(amountInServiceDataCurrency,"amountInInvoicingCurrencyAsOfDateDouble",customAssert);
                }
                if(conversionRate == null){
                    customAssert.assertTrue(false,"Amount in conversion rate is null");
                }else {
                    conversionRateDouble = convertValueToDouble(conversionRate,"conversionRate",customAssert);
                }
                if(amountInInvoicingCurrencyAsOfDate == null){
                    customAssert.assertTrue(false,"Amount in Invoicing Currency As Of Date is null");
                }else {
                    amountInInvoicingCurrencyAsOfDateDouble = convertValueToDouble(amountInInvoicingCurrencyAsOfDate,"amountInInvoicingCurrencyAsOfDate",customAssert);
                }

                if(amountInServiceDataCurrency!=null && conversionRate!=null && amountInInvoicingCurrencyAsOfDate!=null){
                       Double expectedAmountInInvoicingCurrencyAsOfDate = amountInServiceDataCurrencyDouble * conversionRateDouble;

                       if(!expectedAmountInInvoicingCurrencyAsOfDate.equals(amountInInvoicingCurrencyAsOfDateDouble)){
                           customAssert.assertTrue(false,"Expected and Actual Value for amount In Invoicing Currency AsOfDate not matched in the billing record ");
                       }

                }else {
                    customAssert.assertTrue(false,"Any of the value of amountInServiceDataCurrency conversionRate amountInInvoicingCurrencyAsOfDate is null");
                }
                break;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Amount In Invoicing Currency");
        }
        return validationStatus;
    }

    private Double convertValueToDouble(String value,String fieldName,CustomAssert customAssert){

        Double convValue = 0.0;
        try{
            convValue = Double.parseDouble(value);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while converting " + fieldName + " to Double Value");
        }
        return convValue;
    }

    //Check updation of Billing data on updating service data metadata
    public Boolean validate_C89297(int serviceDataId,CustomAssert customAssert) {

        Boolean validationStatus = true;
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        // Starting of C89297 //

        try {

            Show show = new Show();
            show.hitShowVersion2(serviceDataEntityTypeId, serviceDataId);
            String showResponse = show.getShowJsonStr();

            int serviceDataCategory = 1121;         //"General Accounting";
            String serviceDataCategoryName = "General Accounting";
            int serviceDataSubCategory = 1139;          //Print and Post Print Services
            String serviceDataSubCategoryName = "Print and Post Print Services";

            String serviceIdSupplier = ShowHelper.getValueOfField("serviceclient", showResponse) + "_1";
            String serviceIdClient = ShowHelper.getValueOfField("serviceidsupplier", showResponse) + "_1";

            String name = ShowHelper.getValueOfField("name", showResponse) + "_1";

            invoiceHelper.updateServiceDataMetaData(serviceDataId,
                    serviceDataCategory, serviceDataSubCategory,
                    serviceIdSupplier, serviceIdClient, name, customAssert);

            logger.info("Thread sleeping for 18 seconds to check updated values on service data are reflected on billing records");
            Thread.sleep(18000);

            Map<String, Map<String, String>> billingRecordAccToStartDate = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);

            Map<String, String> columnNameValues;
            for (Map.Entry<String, Map<String, String>> entry : billingRecordAccToStartDate.entrySet()) {

                columnNameValues = entry.getValue();

                if (!columnNameValues.get("servicedataname").equals(name + " (" + serviceIdClient + ")")) {
                    logger.error("On Billing Data Record servicedataname is not updated");
                    customAssert.assertTrue(false, "On Billing Data Record servicedataname is not updated");
                    validationStatus = false;
                }

                if (!columnNameValues.get("servicecategory").equals(serviceDataCategoryName)) {
                    logger.error("On Billing Data Record servicecategory is not updated");
                    customAssert.assertTrue(false, "On Billing Data Record servicecategory is not updated");
                    validationStatus = false;
                }

                if (!columnNameValues.get("servicesubcategory").equals(serviceDataSubCategoryName)) {
                    logger.error("On Billing Data Record servicesubcategory is not updated");
                    customAssert.assertTrue(false, "On Billing Data Record servicesubcategory is not updated");
                    validationStatus = false;
                }

                if (!columnNameValues.get("serviceIdSupplier").equals(serviceIdSupplier)) {
                    logger.error("On Billing Data Record serviceIdSupplier is not updated");
                    customAssert.assertTrue(false, "On Billing Data Record serviceIdSupplier is not updated");
                    validationStatus = false;
                }
                break;
            }


        } catch (Exception e) {
            logger.error("Exception while validating the C89297");
            customAssert.assertTrue(false, "Exception while validating the C89297");
            validationStatus = false;
        }

        // Ending of C89297 //

        return validationStatus;
    }

    //    Check deletion of Billing data on updating service data metadata
    private Boolean validate_C151248(int serviceDataId,int expectedNumOfBillingIds,CustomAssert customAssert){

        // Starting of C151248 //

        logger.info("Validating the deletion of Billing ID on updation of isBillable flag to false");

        InvoiceHelper invoiceHelper = new InvoiceHelper();
        Boolean validationStatus = true;
        try {
            invoiceHelper.updateSDisBilAndPricingAvail(serviceDataId, false, true, customAssert);

            logger.info("Thread sleeping for 60 seconds to check for billing records deletion");
            Thread.sleep(60000);

            int numberOfBillingIds = invoiceHelper.getNumberOfBillingRecords(serviceDataId, customAssert);

            if (numberOfBillingIds != expectedNumOfBillingIds) {
                logger.error("Expected number of billing records after should be zero after updation of is billable flag to false");
                customAssert.assertTrue(false, "Expected number of billing records after should be zero after updation of is billable flag to false");
                validationStatus = false;
            }
        }catch (Exception e) {
            logger.error("Exception while validating the C89297");
            customAssert.assertTrue(false, "Exception while validating the C89297");
            validationStatus = false;
        }
        // Ending  of C151248 //

        return validationStatus;
    }

    //Check memo billing data is generated with Pricing Update with CCR
    private Boolean validate_C88814(int serviceDataId,String memoReason,CustomAssert customAssert){

        Boolean validationStatus = true;
        try {
            int memoReasonId = -1;
            int reportId = 444;
            if(memoReason.equals("Discrepancy")){
                memoReasonId = 2;
            }

            String startDate = "01-01-2019";
            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"51\":{\"filterId\":\"51\"," +
                    "\"filterName\":\"startDate\",\"start\":\"" + startDate + "\",\"end\":\"" + startDate + "\"," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"248\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + serviceDataId + "\",\"name\":\"\"}]}," +
                    "\"filterId\":248,\"filterName\":\"serviceData\"}," +
                    "\"384\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + memoReasonId + "\",\"name\":\"Discrepancy\"}]}," +
                    "\"filterId\":384,\"filterName\":\"memoReason\"}}},\"selectedColumns\":[]}";

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(reportId,payload);
            String reportListResponse = reportRendererListData.getListDataJsonStr();

            JSONObject reportListResponseJson = new JSONObject(reportListResponse);

            JSONArray dataArray = reportListResponseJson.getJSONArray("data");

            if(dataArray.length() != 1){
                customAssert.assertTrue(false,"After Applying Filter start date " +startDate + " service data id " + serviceDataId + " memo reason " + memoReason + "Billing Record created is " + dataArray.length() + " Expected is " + 1);
                validationStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating C88814");
            validationStatus = false;
        }
        return validationStatus;

    }

    //Check memo billing data is not generated
    private Boolean validate_C88807(int serviceDataId,String memoReason,CustomAssert customAssert){

        Boolean validationStatus = true;
        try {
            int memoReasonId = -1;
            int reportId = 444;
            if(memoReason.equals("Discrepancy")){
                memoReasonId = 2;
            }

            String startDate = "01-01-2019";
            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{" +
                    "\"51\":{\"filterId\":\"51\",\"filterName\":\"startDate\",\"start\":\"" + startDate + "\",\"end\":\"" + startDate + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"248\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + serviceDataId + "\",\"name\":\"\"}]},\"filterId\":248,\"filterName\":\"serviceData\"}," +
                    "\"384\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + memoReasonId + "\",\"name\":\"\"}]},\"filterId\":384,\"filterName\":\"memoReason\"}}}," +
                    "\"selectedColumns\":[]}";

            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(reportId,payload);
            String reportListResponse = reportRendererListData.getListDataJsonStr();

            JSONObject reportListResponseJson = new JSONObject(reportListResponse);

            JSONArray dataArray = reportListResponseJson.getJSONArray("data");

            if(dataArray.length() >= 1){
                customAssert.assertTrue(false,"Expected Memo Billing Item is 0 were as actual Number of Memo Billing Item " + dataArray.length());
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating C88814");
        }
        return validationStatus;

    }

    private Boolean validate_C88813(int serviceDataId,int invoiceLineItemId,CustomAssert customAssert){

        Boolean validationStatus = true;
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        int expectedNumberOfBillingIds = 2;
        try{

            String invoiceLineItem = "invoice line item";

            //Creating Adjustment Type Line Item for the line Item
            String createPayload = invoiceHelper.getCreatePayloadFromClone(invoiceLineItem,invoiceLineItemId);
            String lineItemType = "ARC RRC Adjustments";
            String valuesOptionId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"adj line item type id");

            String createPayloadAdjLineItem = invoiceHelper.updatePayloadForDiffLineItemType(createPayload,lineItemType,valuesOptionId);

            Create create = new Create();
            create.hitCreate(invoiceLineItem, createPayloadAdjLineItem);
            String createResponse = create.getCreateJsonStr();
            int invoiceLineItemWithAdjustmentType1 = CreateEntity.getNewEntityId(createResponse, invoiceLineItem);

//            invoiceHelper.validateExpectedMemoLinkedLineItem(invoiceLineItemId,invoiceLineItemWithAdjustmentType1,customAssert);

            String invoiceValidationStatus = "Total Discrepancy";

            InvoiceHelper.verifyInvoiceLineItemValidationStatus(invoiceValidationStatus, invoiceLineItemWithAdjustmentType1, customAssert);

            String workFlowStepName = approve;
            workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemWithAdjustmentType1, workFlowStepName);

            //Calculating memo on adjustment type line Item
            workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemWithAdjustmentType1, calculateMemo);

            Thread.sleep(60000);
            invoiceHelper.validateExpectedMemoLinkedLineItem(invoiceLineItemId,invoiceLineItemWithAdjustmentType1,customAssert);
            String startDate = "01-01-2019";
            int memoReasonId = 2;           //For Discrepency

            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{" +
                    "\"51\":{\"filterId\":\"51\",\"filterName\":\"startDate\",\"start\":\"" + startDate + "\",\"end\":\"" + startDate + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"248\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + serviceDataId + "\",\"name\":\"\"}]},\"filterId\":248,\"filterName\":\"serviceData\"}," +
                    "\"384\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + memoReasonId + "\",\"name\":\"\"}]},\"filterId\":384,\"filterName\":\"memoReason\"}}}," +
                    "\"selectedColumns\":[]}";

            invoiceHelper.validateNumberOfBillingDataGenerated(payload,expectedNumberOfBillingIds,customAssert);

            //Creating again an invoice Line Item 2
            create.hitCreate(invoiceLineItem, createPayloadAdjLineItem);
            createResponse = create.getCreateJsonStr();
            int invoiceLineItemWithAdjustmentType2 = CreateEntity.getNewEntityId(createResponse, invoiceLineItem);

            invoiceValidationStatus = "Total Discrepancy";

            InvoiceHelper.verifyInvoiceLineItemValidationStatus(invoiceValidationStatus, invoiceLineItemWithAdjustmentType2, customAssert);

            workFlowStepName = approve;
            workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemWithAdjustmentType2, workFlowStepName);

            //Calculating memo on adjustment type line Item
            workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invoiceLineItemWithAdjustmentType2, calculateMemo);

            invoiceHelper.validateExpectedMemoLinkedLineItem(invoiceLineItemWithAdjustmentType1,invoiceLineItemWithAdjustmentType2,customAssert);

            expectedNumberOfBillingIds = 3;
            invoiceHelper.validateNumberOfBillingDataGenerated(payload,expectedNumberOfBillingIds,customAssert);

            EntityOperationsHelper.deleteEntityRecord(invoiceLineItem,invoiceLineItemWithAdjustmentType1);

            logger.info("Thread sleeping for 15 seconds to check linking of line Item");
            Thread.sleep(15000);
            invoiceHelper.validateExpectedMemoLinkedLineItem(invoiceLineItemId,invoiceLineItemWithAdjustmentType2,customAssert);


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating C88813 " + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;
    }

    private Boolean bulkActionStatus(String flowToTest,String entityIds, CustomAssert csAssert){

        Boolean bulkActionStatus = false;

        logger.info("Hitting BulkActionSave Api for EntityTypeId {}", consumptionEntityTypeId);
        String fromStatus = "Upcoming";
        String toStatus = "Approve";

        Fetch fetchObj = new Fetch();
        BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

        String createJsonStr = executor.get(BulkActionCreate.getApiPath(consumptionEntityTypeId), BulkActionCreate.getHeaders()).getResponse().getResponseBody();

        String payloadForSave = BulkActionSave.getPayloadForSave(createJsonStr,entityIds,consumptionEntityTypeId,consumptionListId,fromStatus,toStatus);
        fetchObj.hitFetch();List<Integer> oldIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
        String bulkActionSaveResponse = executor.post(BulkActionSave.getApiPath(), BulkActionSave.getHeaders(), payloadForSave).getResponse().getResponseBody();

        if (bulkActionSaveResponse != null) {
            //Verify the response of BulkAction Save
            logger.info("Verifying BulkActionSave Response for EntityTypeId {}", consumptionEntityTypeId);
            logger.info("Actual Response received: {}", bulkActionSaveResponse);
            bulkActionSaveResponse = bulkActionSaveResponse.toLowerCase();
            boolean saveSuccessfulResponse = bulkActionSaveResponse.contains("successfully submitted");
            if (!saveSuccessfulResponse) {
                csAssert.assertTrue(false, "BulkAction Save Response received for Flow [" + flowToTest + "] and EntityTypeId " + consumptionEntityTypeId +
                        " does not match required response. Hence not proceeding further. Response [" + bulkActionSaveResponse + "]");
            } else {
                logger.info("Hitting Fetch API to Get Bulk Action Job Task Id");
                fetchObj.hitFetch();
//                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
//                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Action Job for Flow [{}]", flowToTest);
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), oldIds);

                waitForScheduler(flowToTest, newTaskId, csAssert);

                logger.info("Hitting Fetch API to get Status of Bulk Action Job");
                fetchObj.hitFetch();
                String bulkActionJobStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);

                if (bulkActionJobStatus != null && bulkActionJobStatus.trim().equalsIgnoreCase("Completed")) {
                    if (UserTasksHelper.ifAllRecordsPassedInTask(newTaskId)) {
                        logger.info("Bulk Action Successful From Status " + fromStatus + " To Status " + toStatus);
                        bulkActionStatus = true;
                    } else {
                        //Get Error Message
                        String bulkRequestId = bulkHelperObj.getLatestBulkEditRequestId();
                        String errorMessage = bulkHelperObj.getErrorMessagesForBulkEditRequestId(bulkRequestId);

                        csAssert.assertFalse(true, "Bulk Action Scheduler Job failed for Flow [" + flowToTest + "] and Entity Type Id " +
                                consumptionEntityTypeId + ". Error Message: [" + errorMessage + "] Entity Ids: " + entityIds);
                    }
                } else {

                }
            }
        }
        return bulkActionStatus;
    }

    private void waitForScheduler(String flowToTest, int newTaskId, CustomAssert csAssert) {
        logger.info("Waiting for Scheduler to Complete for Flow [{}].", flowToTest);
        try {
            long timeOut = 120000;long pollingTime = 5000;
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerWaitTimeOut");
            if (temp != null && org.apache.commons.lang3.math.NumberUtils.isParsable(temp.trim()))
                timeOut = Long.parseLong(temp.trim());

            logger.info("Time Out for Scheduler is {} milliseconds", timeOut);
            long timeSpent = 0;

            if (newTaskId != -1) {
                logger.info("Checking if Bulk Action Task has completed or not for Flow [{}]", flowToTest);

                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerPollingTime");
                if (temp != null && NumberUtils.isParsable(temp.trim()))
                    pollingTime = Long.parseLong(temp.trim());

                while (timeSpent < timeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();

                    logger.info("Getting Status of Bulk Action Task for Flow [{}]", flowToTest);
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        logger.info("Bulk Action Task Completed for Flow [{}]", flowToTest);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Bulk Action Task is not finished yet for Flow [{}]", flowToTest);
                    }

                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("In Progress")) {
                        if (!UserTasksHelper.anyRecordFailedInTask(fetchObj.getFetchJsonStr(), newTaskId) &&
                                !UserTasksHelper.anyRecordProcessedInTask(fetchObj.getFetchJsonStr(), newTaskId)) {

                        } else {
                            logger.info("Bulk Action Task for Flow [{}] is In Progress but At-least One record has been processed or failed. " +
                                    "Hence Not Checking if Show Page is Blocked or not.", flowToTest);
                        }
                    } else {
                        logger.info("Bulk Action Task for Flow [{}] has not been picked by Scheduler yet.", flowToTest);
                    }
                }
            } else {
                logger.info("Couldn't get Bulk Action Task Job Id for Flow [{}]. Hence waiting for Task Time Out i.e. {}", flowToTest, timeOut);
                Thread.sleep(timeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Waiting for Scheduler to Finish for Flow [" + flowToTest + "]. " + e.getMessage());
        }
    }

//   C153440 Check billing data updation after updation is done in final consumption
    private void validate_C153440_NormalEditScenario(String scenario,int serviceDataId,ArrayList<Integer> consumptionIds,CustomAssert customAssert){

        logger.info("Validating the test case C153440 Check billing data updation after updation is done in final consumption for the scenario " + scenario);
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        String testCaseId = "C153440";
        String finalConsumption = "100";
        try {
            for (int i = 0; i < consumptionIds.size(); i++) {
                int consumptionId = consumptionIds.get(i);

                Boolean finalConsumptionUpdationStatus = invoiceHelper.updateConsumption(consumptionId, Double.parseDouble(finalConsumption), customAssert);

                if (!finalConsumptionUpdationStatus) {
                    customAssert.assertTrue(false, "Final Consumption updated unsuccessfully while testing " + testCaseId + " for the scenario " + scenario);

                }
            }
            Thread.sleep(12000);
            HashMap<String,Map<String,String>> billingRecords= invoiceHelper.getBillingRecordAccToStartDate(serviceDataId,customAssert);

            if (billingRecords.size() == 0) {
                customAssert.assertTrue(false, "Billing Record Not Generated while validating the test Case Id " + testCaseId);
            } else {
                for (Map.Entry<String, Map<String, String>> entry : billingRecords.entrySet()) {

                    Map<String, String> billingRecordDetails = entry.getValue();
                    if(billingRecordDetails.containsKey("quantity")) {
                        String actualQty = billingRecordDetails.get("quantity");

                        if (actualQty == null) {
                            customAssert.assertTrue(false, "While validating testCaseID " + testCaseId + " for the scenario " + scenario + " new Quantity Value is null Either Quantity not updated or new Billing Record not generated");return;
                        } else if (actualQty.equals("null")) {
                            customAssert.assertTrue(false, "While validating testCaseID " + testCaseId + " for the scenario " + scenario + " new Quantity Value is null String Either Quantity not updated or new Billing Record not generated");return;
                        } else if (!actualQty.contains(finalConsumption)) {
                            customAssert.assertTrue(false, "While validating testCaseID " + testCaseId + " for the scenario " + scenario + " new Quantity Value is not as Expected String Either Quantity not updated or new Billing Record not generated");return;
                        }
                    }else {
                        customAssert.assertTrue(false,"Billing Record Listing Does not have Quantity Column");
                    }
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating testCaseId " + testCaseId + " for the scenario " + scenario);
            customAssert.assertTrue(false,"Exception while validating testCaseId " + testCaseId + " for the scenario " + scenario);
        }
    }

    private void validate_C153440_BulkUpdateScenario(String scenario,int serviceDataId,String consumptionStr,CustomAssert customAssert){

        logger.info("Validating the test case C153440 Check billing data updation after updation is done in final consumption for the scenario " + scenario);
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        String testCaseId = "C153440";
        int finalConsumption = 300;
        try {

            String templateDownloadPath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk update template download",
                    "template path");
            String templateDownloadName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk update template download",
                    "template name");
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk update template download",
                    "template id"));
            int entityTypeId = consumptionEntityTypeId;

            String entityIds = consumptionStr;

            Boolean templateDownloaded = BulkTemplate.downloadBulkUpdateTemplate(templateDownloadPath, templateDownloadName, templateId, entityTypeId, entityIds);

            if (!templateDownloaded) {
                logger.info("Bulk Update Template Download failed using Template Id {}, EntityTypeId {} and EntityIds {}.", templateId, entityTypeId,
                        entityIds);
                customAssert.assertTrue(templateDownloaded, "Bulk Update Template Download failed using Template Id " + templateId + ", EntityTypeId " +
                        entityTypeId + " and EntityIds " + entityIds);
            }else {
                String sheetName = "Consumption";String finalConsColId = "11910";//Final Cons Col Id
                int columnNum = XLSUtils.getExcelColNumber(templateDownloadPath,templateDownloadName,sheetName,2,finalConsColId);

                int numOfRows = XLSUtils.getNoOfRows(templateDownloadPath,templateDownloadName,sheetName).intValue();

                for(int i = 6;i<numOfRows;i++){
                    XLSUtils.updateColumnValue(templateDownloadPath,templateDownloadName,sheetName,i,columnNum,finalConsumption);
                }
            }

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String bulkUpdateUploadResponse = BulkTemplate.uploadBulkUpdateTemplate(templateDownloadPath, templateDownloadName, entityTypeId, templateId);
            String entityName = "consumptions";

            if (bulkUpdateUploadResponse == null) {
                logger.error("Upload Bulk Data Response for Bulk Update, Flow [{}] and Entity {} is Null.", scenario, entityName);
                customAssert.assertTrue(false, "Upload Bulk Data Response for Bulk Update, Flow [" + scenario + "] and Entity " + entityName + " is Null.");
            } else {

                logger.info("Hitting Fetch API to Get Bulk Update Job Task Id for Flow [{}]", scenario);
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Update Job for Flow [{}]", scenario);
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                waitForScheduler(scenario, newTaskId, customAssert);

                logger.info("Hitting Fetch API to get Status of Bulk Update Job for Flow [{}]", scenario);
                fetchObj.hitFetch();
                String bulkUpdateJobStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);

                if (bulkUpdateJobStatus != null && bulkUpdateJobStatus.trim().equalsIgnoreCase("Completed")) {

                }else {
                    customAssert.assertTrue(false,"Bulk update Fail for the scenario " + scenario);
                }
            }
            Thread.sleep(5000);
            HashMap<String,Map<String,String>> billingRecords= invoiceHelper.getBillingRecordAccToStartDate(serviceDataId,customAssert);

            if (billingRecords.size() == 0) {
                customAssert.assertTrue(false, "Billing Record Not Generated while validating the test Case Id " + testCaseId);
            } else {
                for (Map.Entry<String, Map<String, String>> entry : billingRecords.entrySet()) {

                    Map<String, String> billingRecordDetails = entry.getValue();
                    if(billingRecordDetails.containsKey("quantity")) {
                        String actualQty = billingRecordDetails.get("quantity");

                        if (actualQty == null) {
                            customAssert.assertTrue(false, "While validating testCaseID " + testCaseId + " for the scenario " + scenario + " new Quantity Value is null Either Quantity not updated or new Billing Record not generated");
                        } else if (actualQty.equals("null")) {
                            customAssert.assertTrue(false, "While validating testCaseID " + testCaseId + " for the scenario " + scenario + " new Quantity Value is null String Either Quantity not updated or new Billing Record not generated");
                        } else if (!actualQty.contains(String.valueOf(finalConsumption))) {
                            customAssert.assertTrue(false, "While validating testCaseID " + testCaseId + " for the scenario " + scenario + " new Quantity Value is not as Expected String Either Quantity not updated or new Billing Record not generated");
                        }
                    }else {
                        customAssert.assertTrue(false,"Billing Record Listing Does not have Quantity Column");
                    }
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating testCaseId " + testCaseId + " for the scenario " + scenario);
            customAssert.assertTrue(false,"Exception while validating testCaseId " + testCaseId + " for the scenario " + scenario);
        }
    }

    //To validate billing record For Null Values of pricing columns
    private void valBillRecForPricingVerColNullVal(Map<String, String> billingRecord,CustomAssert customAssert){

        try {
            if (!billingRecord.containsKey("pricingentitytypeid")) {
                customAssert.assertTrue(false, "Entity Type (Pricing) column not found in the billing report");
            } else {
                if (billingRecord.get("pricingentitytypeid") != null) {
                    customAssert.assertTrue(false, "In the billing Report expected value of Entity Type (Pricing) column should be null but actual column value " + billingRecord.get("pricingentitytypeid"));
                }
            }

            if (!billingRecord.containsKey("pricingentityid")) {
                customAssert.assertTrue(false, "Entity ID column not found in the billing report");
            } else {
                if (billingRecord.get("pricingentityid") != null) {
                    if (!(billingRecord.get("pricingentityid").equals(""))) {
                        customAssert.assertTrue(false, "In the billing Report expected value of Entity ID column should be null but actual column value " + billingRecord.get("pricingentityid"));
                    }
                }
            }
            if (!billingRecord.containsKey("pricingentityname")) {
                customAssert.assertTrue(false, "Entity name (Pricing) column not found in the billing report");
            } else {
                if (billingRecord.get("pricingentityname") != null) {
                    if (!(billingRecord.get("pricingentityname").equals(""))) {
                        customAssert.assertTrue(false, "In the billing Report expected value of Entity name (Pricing) column should be null but actual column value " + billingRecord.get("pricingentityname"));
                    }
                }
            }
            if (!billingRecord.containsKey("effectivedate")) {
                customAssert.assertTrue(false, "Effective date column not found in the billing report");
            } else {
                if (billingRecord.get("effectivedate") != null) {
                    if (!(billingRecord.get("effectivedate").equals(""))) {
                        customAssert.assertTrue(false, "In the billing Report expected value of Effective date column should be null but actual column value " + billingRecord.get("effectivedate"));
                    }
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating null values for Pricing version Columns in Billing Report");
        }
    }

    //To validate billing record for execution of billing Rule
    private void validate_C151321(int serviceDataId,Map<String, String> billingRecord,CustomAssert customAssert){
        String testCaseId = "C151321";
        //Billing rule is such that line item description should contain
        // service data name for fixed fee type service data
        try{
            String colToValidate = "lineitemdescription";
            if(billingRecord.containsKey(colToValidate)){

                if(billingRecord.get(colToValidate) == null){
                    customAssert.assertTrue(false,"While validating test case " + testCaseId + " Billing Report is contains null value for column" +
                            colToValidate + " Expected Service Data Name According to billing rule");
                }else if(!billingRecord.get(colToValidate).equals(ShowHelper.getValueOfField(serviceDataEntityTypeId,serviceDataId,"name"))){
                    customAssert.assertTrue(false,"While validating test case " + testCaseId + " Billing Report has column value "
                                    + billingRecord.get(colToValidate) + " for column name " + colToValidate +
                                    " But Expected is \"Service Data Name\" According to billing rule");
                }

            }else {
                customAssert.assertTrue(false,"While validating test case " + testCaseId + " Billing Report is not having column Line Item Description");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating test case " + testCaseId + " billing record after execution of billing Rule");
        }
    }

}
