package com.sirionlabs.test.serviceData;


import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.UserTasksHelper;

import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class Test_BillDataMiscScenarios {

    private final static Logger logger = LoggerFactory.getLogger(Test_BillDataMiscScenarios.class);

    String configFilePath;
    String configFileName;

    int billingReportId=444;
    int serviceDataEntityTypeId=64;
    String clientId;
    String pricingSheetName;

    @BeforeClass
    public void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BillDataMiscScenFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("BillDataMiscScenFileName");
        clientId = ConfigureEnvironment.getEnvironmentProperty("client_id");
        pricingSheetName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"pricing sheet");
    }

    @Test
    public void Test_BillingDataGen_PricingUploadFailed() {

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "PricingUploadFailed Scenario";
        Fetch fetchObj = new Fetch();
        SCPUtils scpUtils = new SCPUtils();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        try {

            String currentTimeStampSQl = "select now();";

            String timeStamp = postgreSQLJDBC.doSelect(currentTimeStampSQl).get(0).get(0);

            String remoteFilePath = "/data/temp-session/bulktask/";
            scpUtils.runChmodCommand("000", remoteFilePath);

            String serviceDataString = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "service data id pricing failure");
            int serviceDataId = Integer.parseInt(serviceDataString);

            InvoiceHelper invoiceHelper = new InvoiceHelper();
            HashMap<String, Map<String, String>> billingRecordIds = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);

            if(billingRecordIds.size() == 0){
                customFailure("Billing record not fetched for service data " + serviceDataId,customAssert);
                customAssert.assertAll();
                return;
            }
            String key = billingRecordIds.keySet().iterator().next();

            Double quantity = Double.parseDouble(billingRecordIds.get(key).get("quantity"));
            Double rate =  Double.parseDouble(billingRecordIds.get(key).get("rate"));

            Double newQuantity = quantity + 1;
            Double newRate = rate + 1;

            String pricingTemplateFilePath = "src\\test\\resources\\TestConfig\\Invoice";
            String pricingTemplateFileName = "PricingFileBiliingMisc.xlsm";
            int templateId = 1010;
            BulkTemplate.downloadPricingTemplate(pricingTemplateFilePath, pricingTemplateFileName, templateId, serviceDataEntityTypeId, serviceDataString);

            int rowNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowToTest,"pricing row num"));
            int colRate = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowToTest,"pricing col num rate"));
            int colQty = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,flowToTest,"pricing col num vol"));

            XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, pricingSheetName, rowNo, colQty, newQuantity);
            XLSUtils.updateColumnValue(pricingTemplateFilePath, pricingTemplateFileName, pricingSheetName, rowNo, colRate, newRate);

            InvoicePricingHelper pricingObj = new InvoicePricingHelper();

            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

            String sqlForBulkReqId = "select id from bulk_edit_request where file_name = '" + pricingTemplateFileName + "' and request_date >= '" + timeStamp + "'";

            List<List<String>> bulkRequest = postgreSQLJDBC.doSelect(sqlForBulkReqId);
            String bulkRequestId;
            if(bulkRequest.size() == 0){
                customFailure("Bulk Request Id not found in database after pricing upload",customAssert);
                customAssert.assertAll();
                return;

            }else {
                bulkRequestId = bulkRequest.get(0).get(0);
                logger.info("Hitting Fetch API to Get Bulk Update Job Task Id for Flow ");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Update Job for Flow ");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
                long schedulerTimeOut = 180000L;        //20 mins
                invoiceHelper.waitForScheduler(flowToTest,newTaskId,5000,schedulerTimeOut, customAssert);

                fetchObj.hitFetch();

                String fetchResponse = fetchObj.getFetchJsonStr();
                Boolean recordsFailed = UserTasksHelper.anyRecordFailedInTask(fetchResponse, newTaskId);

                if(recordsFailed){
                    customFailure("Pricing upload Failed for service data id " + serviceDataId,customAssert);
                    customAssert.assertAll();
                    return;
                }else {
                    int postProcessJobId = 75;
                    String expectedStatusCode = "5";
                    String statusSqlQuery =  "select status from task where entity_id = " + bulkRequestId + " and scheduled_job_id IN (select id from scheduled_job where job_id IN (" + postProcessJobId + ") and client_id = " + clientId + ")";
                    List<List<String>> statusRecord = postgreSQLJDBC.doSelect(statusSqlQuery);

                    if(statusRecord.size() ==  0){
                        customFailure("On Executing the query " + statusSqlQuery + " no data found was returned",customAssert);

                    }else {
                        String actualStatusCode = statusRecord.get(0).get(0);

                        if(!expectedStatusCode.equals(actualStatusCode)){
                            customFailure("Expected status code for post process task id " + expectedStatusCode + " but actual status code " + actualStatusCode,customAssert);
                        }

                        billingRecordIds = invoiceHelper.getBillingRecordAccToStartDate(serviceDataId, customAssert);

                        if(billingRecordIds.size() == 0){
                            customFailure("Billing record not fetched for service data " + serviceDataId,customAssert);
                            customAssert.assertAll();
                            return;
                        }else {
                            quantity = Double.parseDouble(billingRecordIds.get(key).get("quantity"));
                            rate =  Double.parseDouble(billingRecordIds.get(key).get("rate"));

                            if(!quantity.equals(newQuantity)){
                                customFailure("In billing Report after post process Failure Seems billing Record not regenerated Expected value of quantity " + newQuantity + " Actual value of quantity " + quantity,customAssert);
                            }

                            if(!rate.equals(newRate)){
                                customFailure("In billing Report after post process Failure Seems billing Record not regenerated Expected value of rate " + newRate + " Actual value of rate " + rate,customAssert);
                            }
                        }
                    }
                }

            }

        }catch (Exception e){
            customFailure("Exception while validating the scenario " + e.getStackTrace(),customAssert);
        }finally {
            String remoteFilePath = "/data/temp-session/bulktask/";
            scpUtils.runChmodCommand("777", remoteFilePath);
            postgreSQLJDBC.closeConnection();
            scpUtils.closeSession();
        }
        customAssert.assertAll();
    }

    private void customFailure(String message,CustomAssert customAssert){
        logger.error(message);
        customAssert.assertTrue(false,message);
    }
}
