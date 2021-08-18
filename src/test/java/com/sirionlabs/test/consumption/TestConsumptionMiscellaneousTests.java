package com.sirionlabs.test.consumption;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestConsumptionMiscellaneousTests {

    private final static Logger logger = LoggerFactory.getLogger("TestConsumptionMiscellaneousTests.class");

    private int contractId;

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
    private String invoiceLineItem = "invoice line item";
    private String invoices = "invoices";
    private String serviceData = "service data";
    private String consumptions = "consumptions";

    @BeforeClass
    public void BeforeClass(){

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

        invoiceFlowsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFlowsConfigFilePath");

        invoiceFlowsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFlowsConfigFileName");

        contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath,invoiceFlowsConfigFileName,"basecontractid"));

        pricingTemplateFilePath = ConfigureConstantFields.getConstantFieldsProperty("PricingTemplateFilePath");
    }

    //C10806 Visibility of line item on corresponding consumption on changing service period
    @Test
    public void Test_C10806(){

        CustomAssert customAssert = new CustomAssert();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        String flowToTest = "arc flow 1";
        int serviceDataId;

        String startDate = "03-01-2018";
        String endDate = "09-30-2018";
        ArrayList<Integer> consumptionIds = new ArrayList<>();
        try{


            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            String pricingTemplateFileName = "ArcRrcFlow1Pricing.xlsm";

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                    invoiceConfigFileName, flowToTest, contractId);

            serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,uniqueDataString);
//            serviceDataId = 44773;
            invoiceHelper.updateServiceStartAndEndDate(serviceDataId,startDate,endDate,customAssert);

            Boolean pricingTemplateDownload= invoicePricingHelper.downloadPricingTemplate(pricingTemplateFilePath,pricingTemplateFileName,serviceDataId);

            if(!pricingTemplateDownload){
                customAssert.assertTrue(false,"Error while downloading pricing template file");
            }
            String pricingSheetName = "Pricing";
            long pricingSheetNoOfRows =  XLSUtils.getNoOfRows(pricingTemplateFilePath,pricingTemplateFileName,pricingSheetName);

            int endRow = (int)pricingSheetNoOfRows;

            Boolean arcSheetUpdationStatus  =  updateARCSheetPricingTemplate(pricingTemplateFilePath,pricingTemplateFileName,endRow,customAssert);
            if(!arcSheetUpdationStatus){
                customAssert.assertTrue(false,"Error while updating ARC sheet");
            }

            Boolean pricingSheetUpdationStatus  =  updatePricingSheetPricingTemplate(pricingTemplateFilePath,pricingTemplateFileName,endRow,customAssert);
            if(!pricingSheetUpdationStatus){
                customAssert.assertTrue(false,"Error while updating Pricing sheet");
            }

            Boolean pricingUpload = invoicePricingHelper.uploadPricingFileWithoutEdit(pricingTemplateFilePath,pricingTemplateFileName,flowToTest,customAssert);

            if(!pricingUpload){
                customAssert.assertTrue(false,"Error while pricing upload");
            }else {
                boolean result = workflowActionsHelper.performWorkFlowStepV2(64, serviceDataId, publishAction, customAssert);
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

                } else {

                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                    customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");
                }

                int invoiceId = InvoiceHelper.getInvoiceIdNew(invoiceConfigFilePath,invoiceConfigFileName,invoiceExtraFieldsConfigFileName,flowToTest);

                int invoiceLineItemId = InvoiceHelper.getInvoiceLineItemIdNew(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,flowToTest,serviceDataId,invoiceId);

                startDate = "04-01-2018";
                endDate = "04-30-2018";

                Boolean updationStatus = invoiceHelper.updateServiceStartAndEndDate(invoiceLineItem,invoiceLineItemId,startDate,endDate,customAssert);

                if(!updationStatus){
                    customAssert.assertTrue(false,"Error while updating service start date and end date");
                }else {
                    validateRefTabForUpdatedCons(invoiceLineItemId,startDate,endDate,customAssert);
                }

                EntityOperationsHelper.deleteEntityRecord(invoiceLineItem,invoiceLineItemId);
                EntityOperationsHelper.deleteEntityRecord(invoices,invoiceId);
                EntityOperationsHelper.deleteMultipleRecords(consumptions,consumptionIds);
                EntityOperationsHelper.deleteEntityRecord(serviceData,serviceDataId);

            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getMessage());
        }
        customAssert.assertAll();
    }

    private Boolean updateARCSheetPricingTemplate(String pricingTemplateFilePath, String pricingTemplateFileName,int endRow,CustomAssert customAssert){

        Boolean updateStatus = true;
        try {
            String sheetName = "ARCRRC";

            List<String> excelDataFirstRowData = XLSUtils.getExcelDataOfOneRow(pricingTemplateFilePath, pricingTemplateFileName, sheetName, 7);
            List<String> excelDataColumnIds = XLSUtils.getExcelDataOfOneRow(pricingTemplateFilePath, pricingTemplateFileName, sheetName, 2);
            int lowerVolume = 100;
            int upperVolume = 1000;
            int rate = 10;
            String type = "ARC";

            String columnId;
            int serviceDataIdColumnNum = 2;
            int serviceDataNameColumnNum = 3;
            int serviceIdClientColumnNum = 4;
            int lvColumnNum = 7;
            int uvColumnNum = 8;
            int rateColumnNum = 9;
            int typeColumnNum = 10;
            int serviceStartDateColumnNum = 5;
            int serviceEndDateColumnNum = 6;
            int year = 2018;

            for (int i = 0; i < excelDataColumnIds.size(); i++) {
                columnId = excelDataColumnIds.get(i);

                if (columnId.equalsIgnoreCase("8061")) {
                    lvColumnNum = i;
                } else if (columnId.equalsIgnoreCase("8062")) {
                    uvColumnNum = i;
                } else if (columnId.equalsIgnoreCase("8063")) {
                    rateColumnNum = i;
                } else if (columnId.equalsIgnoreCase("8064")) {
                    typeColumnNum = i;
                } else if (columnId.equalsIgnoreCase("11858")) {
                    serviceStartDateColumnNum = i;
                } else if (columnId.equalsIgnoreCase("11859")) {
                    serviceEndDateColumnNum = i;
                }
            }

            int serialNumber = 1;
            int startMonth = 3;
            String startDate;
            String endDate;

            int startRow = 6;
//        int endRow = 13;
            String dateFormat = "MM/dd/yyyy";
            for (int i = startRow; i < endRow; i++) {

//                startDate = DateUtils.getMonthStartDateInMMM_DDFormat(startMonth);
//                endDate = DateUtils.getMonthEndDateInMMM_DDFormat(startMonth, year);

                startDate = DateUtils.getMonthStartDateInMMDDFormat(startMonth);
                endDate = DateUtils.getMonthEndDateInMMDDFormat(startMonth, year);

                startDate = startDate + "/" + year;
                endDate = endDate + "/" + year;

                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, 0, serialNumber);
                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, 1, 1);
                XLSUtils.updateColumnValueDate(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceStartDateColumnNum,dateFormat, startDate);
                XLSUtils.updateColumnValueDate(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceEndDateColumnNum,dateFormat, endDate);

                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, lvColumnNum, lowerVolume);
                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, uvColumnNum, upperVolume);
                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, typeColumnNum, type);
                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, rateColumnNum, rate);

                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceDataIdColumnNum, excelDataFirstRowData.get(serviceDataIdColumnNum));
                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceDataNameColumnNum, excelDataFirstRowData.get(serviceDataNameColumnNum));
                XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, sheetName, i, serviceIdClientColumnNum, excelDataFirstRowData.get(serviceIdClientColumnNum));


                startMonth = startMonth + 1;
                serialNumber = serialNumber + 1;
            }

        }catch (Exception e){
            logger.error("Exception while updating ARC Sheet of Pricing Template");
            customAssert.assertTrue(false,"Exception while updating ARC Sheet of Pricing Template");
            updateStatus = false;
        }
        return updateStatus;
    }

    private Boolean updatePricingSheetPricingTemplate(String pricingTemplateFilePath,String pricingTemplateFileName,int endRow,CustomAssert customAssert){
        Boolean updateStatus = true;

        try{
            String sheetName = "Pricing";
            int startRow = 6;
            int volumeColNum = 7;
            int rateColNum = 8;
            int volume = 1000;
            int rate = 10;
            for(int i = startRow;i<endRow;i++){
                XLSUtils.updateColumnValue(pricingTemplateFilePath,pricingTemplateFileName,sheetName,i,volumeColNum,volume);
                XLSUtils.updateColumnValue(pricingTemplateFilePath,pricingTemplateFileName,sheetName,i,rateColNum,rate);
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while updating Pricing Sheet");
            updateStatus = false;
        }
        return updateStatus;
    }

    private boolean validateRefTabForUpdatedCons(int invoiceLineItemId,String startDate,String endDate,CustomAssert customAssert){

        Boolean validationStatus = true;

        int refTabId = 356;
        int consumptionEntityTypeId = 176;
        int invoiceLineItemEntityTypeId = 165;
        int consumptionId = -1;
        String payload = "{\"filterMap\":{\"entityTypeId\":176,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

        TabListData tabListData = new TabListData();
        try {

            tabListData.hitTabListData(refTabId,invoiceLineItemEntityTypeId,invoiceLineItemId,payload);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if(!JSONUtility.validjson(tabListResponse)){
                customAssert.assertTrue(false,"Ref Tab List Response is an invalid json");
            }else {
                JSONObject tabListResponseJson = new JSONObject(tabListResponse);

                JSONArray dataArray = tabListResponseJson.getJSONArray("data");
                JSONObject indRow;
                JSONArray indRowArray;
                String columnName;

                for(int i =0;i<dataArray.length();i++){
                    indRow = dataArray.getJSONObject(i);
                    indRowArray = JSONUtility.convertJsonOnjectToJsonArray(indRow);

                    for(int j=0;j<indRowArray.length();j++){

                        columnName = indRowArray.getJSONObject(j).get("columnName").toString();
                        if(columnName.equalsIgnoreCase("id")){
                            consumptionId = Integer.parseInt(indRowArray.getJSONObject(j).get("value").toString().split(":;")[1]);
                            break;
                        }
                    }

                }
                Show show = new Show();
                String consStartDate;
                String consEndDate;

                if(consumptionId!=-1) {

                    show.hitShowVersion2(consumptionEntityTypeId,consumptionId);
                    String showResponse = show.getShowJsonStr();

                    consStartDate = ShowHelper.getValueOfField("consumptionstartdatevalue",showResponse);
                    consEndDate = ShowHelper.getValueOfField("consumptionenddatevalue",showResponse);

                    if(!consStartDate.equalsIgnoreCase(startDate)){
                        customAssert.assertTrue(false,"Consumption Start Date Didn't matched");
                        validationStatus = false;
                    }

                    if(!consEndDate.equalsIgnoreCase(endDate)){
                        customAssert.assertTrue(false,"Consumption End Date Didn't matched");
                        validationStatus = false;
                    }

                }else {
                    customAssert.assertTrue(false,"Consumption is equal to -1");
                    validationStatus = false;
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while valdating Ref Tab " + e.getMessage());
            validationStatus = false;
        }
        return validationStatus;
    }

}
