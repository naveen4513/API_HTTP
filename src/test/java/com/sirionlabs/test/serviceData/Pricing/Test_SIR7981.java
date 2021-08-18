package com.sirionlabs.test.serviceData.Pricing;

import com.codepine.api.testrail.model.User;
import com.google.inject.internal.util.$AsynchronousComputationException;
import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;

import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Test_SIR7981 {

    private final static Logger logger = LoggerFactory.getLogger(Test_SIR7981.class);

    private String configFilePath;
    private String configFileName;

    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;

    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;

    private String invoiceFlowsConfigFilePath;
    private String invoiceFlowsConfigFileName;

    private String uploadFilePath;

    private int serviceDataEntityTypeId = 64;
    private Long schedulerJobTimeOut = 180000L;
    private Long schedulerJobPollingTime = 5000L;
    private String consumptions = "consumptions";
    private String serviceDataEntity = "service data";

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSIR7981FilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSIR7981FileName");

        //Contract Config files
        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        invoiceFlowsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFlowsConfigFilePath");
        invoiceFlowsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFlowsConfigFileName");

        uploadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"uploadfilepath");
    }

    @DataProvider(name = "flowsToValidate")
    public Object[][] flowsToValidate() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstotest").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

//    C151230 C151229
    @Test(dataProvider = "flowsToValidate",enabled = false)
    public void TestC151230(String flowToTest){

        CustomAssert customAssert = new CustomAssert();

        String uniqueDataString = DateUtils.getCurrentTimeStamp();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();

        int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flowToTest);
        if (contractId == -1) {
            logger.info("Cannot create contract");
            customAssert.assertTrue(false, "Cannot create contract");
        }

        InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flowToTest, contractId);

        int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,contractId);

        if (serviceDataId == -1) {
            logger.info("Cannot create service data");
            customAssert.assertTrue(false, "Cannot create service data");
        }

        String entityIds = String.valueOf(serviceDataId);
        String fieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"field id ui","pricing for reporting");

        String pricingForReporting = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit field id","pricing for reporting");

        Boolean pricingForReportingValue = true;

        String bulkEditPayload  = createPayloadForBulkEdit(entityIds,fieldIds,pricingForReporting,pricingForReportingValue);

        Fetch fetchObj = new Fetch();
        fetchObj.hitFetch();
        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
//        UserTasksHelper.removeAllTasks();
        BulkeditEdit editObj = new BulkeditEdit();
        editObj.hitBulkeditEdit(serviceDataEntityTypeId,bulkEditPayload);
        String editJsonStr = editObj.getBulkeditEditJsonStr();

        if(!editJsonStr.contains("success")){
            customAssert.assertTrue(false,"Error during hitting bulk edit ");
        }

        int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

        Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

        String jobStatus = schedulerJob.get("jobPassed");
        if(jobStatus.equals("false")){
            logger.info("Expected result should be Bulk Edit unsuccessful");
            customAssert.assertTrue(true, "Expected result should be Bulk Edit unsuccessful");
        }
        else {
            logger.error("Bulk Edit done successfully");
            customAssert.assertTrue(false,"Expected result should be Bulk Edit unsuccessful but actually bulk edit passed");
        }

        int templateId = 1027;

        String bulkUpdateFileName = "BulkUpdate.xlsm";
        String sheetName = "Service Data";

        Boolean downloadStatus =BulkTemplate.downloadBulkUpdateTemplate(uploadFilePath,bulkUpdateFileName,templateId,serviceDataEntityTypeId,String.valueOf(serviceDataId));

        if(!downloadStatus){
            customAssert.assertTrue(false,"Error while bulk template download");
        }else {
            List<String> rowData = XLSUtils.getExcelDataOfOneRow(uploadFilePath,bulkUpdateFileName,sheetName,1);

            int pricingForReportingColNum = -1;

            for(int i=0;i<rowData.size();i++){

                if(rowData.get(i).equals(fieldIds)){
                    pricingForReportingColNum = i;
                    break;
                }
            }

            pricingForReportingValue = true;
            XLSUtils.updateColumnValue(uploadFilePath,bulkUpdateFileName,sheetName,6,pricingForReportingColNum,String.valueOf(pricingForReportingValue));
            int bulkUpdateTemplateId = 1027;

            Boolean bulkUpdateStatus  = UserTasksHelper.updateBulkUpdate(uploadFilePath,bulkUpdateFileName,serviceDataEntityTypeId,bulkUpdateTemplateId,customAssert);

            if(bulkUpdateStatus){
                customAssert.assertTrue(false,"Bulk Update should fail as Pricing Available is set to true");
            }

        }

    }

//  C151223 C151200
    @Test(dataProvider = "flowsToValidate",enabled = false)
    public void TestC151223(String flowToTest){

        CustomAssert customAssert = new CustomAssert();

        String uniqueDataString = DateUtils.getCurrentTimeStamp();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();

        int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flowToTest);
        if (contractId == -1) {
            logger.info("Cannot create contract");
            customAssert.assertTrue(false, "Cannot create contract");
        }

        InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flowToTest, contractId);

        int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToTest,contractId);

        if (serviceDataId == -1) {
            logger.info("Cannot create service data");
            customAssert.assertTrue(false, "Cannot create service data");
        }

        String entityIds = String.valueOf(serviceDataId);
        String fieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"field id ui","consumption available");

        String consumptionAvailable = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk edit field id","consumption available");

        Boolean consumptionAvailableValue = true;

        String bulkEditPayload  = createPayloadForBulkEdit(entityIds,fieldIds,consumptionAvailable,consumptionAvailableValue);

        Boolean bulkEditStatus = UserTasksHelper.updateBulkEdit(serviceDataEntityTypeId,bulkEditPayload,customAssert);

        if(!bulkEditStatus){
            customAssert.assertTrue(false,"Bulk Edit job failed for consumption approved true");
        }

        int templateId = 1027;

        String bulkUpdateFileName = "BulkUpdate.xlsm";
        String sheetName = "Service Data";

        Boolean downloadStatus =BulkTemplate.downloadBulkUpdateTemplate(uploadFilePath,bulkUpdateFileName,templateId,serviceDataEntityTypeId,String.valueOf(serviceDataId));

        if(!downloadStatus){
            customAssert.assertTrue(false,"Error while bulk template download");
        }else {
            List<String> rowData = XLSUtils.getExcelDataOfOneRow(uploadFilePath,bulkUpdateFileName,sheetName,1);

            int consumptionAvailColNum = -1;

            for(int i=0;i<rowData.size();i++){

                if(rowData.get(i).equals(fieldIds)){
                    consumptionAvailColNum = i;
                    break;
                }
            }

            consumptionAvailableValue= true;
            XLSUtils.updateColumnValue(uploadFilePath,bulkUpdateFileName,sheetName,6,consumptionAvailColNum,String.valueOf(consumptionAvailableValue));
            int bulkUpdateTemplateId = 1027;

            Boolean bulkUpdateStatus  = UserTasksHelper.updateBulkUpdate(uploadFilePath,bulkUpdateFileName,serviceDataEntityTypeId,bulkUpdateTemplateId,customAssert);

            if(!bulkUpdateStatus){
                customAssert.assertTrue(false,"Bulk Update failed for consumption available true");
            }

        }

    }

    //  C151223
    @Test(dataProvider = "flowsToValidate",enabled = true)
    public void TestC151227(String flowToTest){

        CustomAssert customAssert = new CustomAssert();

        InvoiceHelper invoiceHelper = new InvoiceHelper();
        InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        ArrayList<Integer> consumptionIds = new ArrayList<>();

        String publishAction = "publish";
        String approveAction = "approve";

        try {
            String uniqueDataString = DateUtils.getCurrentTimeStamp();

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flowToTest);
            if (contractId == -1) {
                logger.info("Cannot create contract");
                customAssert.assertTrue(false, "Cannot create contract");
            }

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flowToTest, contractId);

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId);
//        int serviceDataId = 22000;
            if (serviceDataId == -1) {
                logger.info("Cannot create service data");
                customAssert.assertTrue(false, "Cannot create service data");
            }

            Boolean uploadPricing = invoicePricingHelper.uploadPricingFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName,
                    flowToTest, serviceDataId,
                    uploadFilePath, false,
                    "", -1, null,
                    customAssert);

            if (!uploadPricing) {

                customAssert.assertTrue(false, "Upload pricing done unsuccessfully for the flow " + flowToTest);

            } else {
                boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, serviceDataId, publishAction);
                if (!result) {
                    logger.info("Could not perform publish action for the service data", serviceDataId);
                    customAssert.assertTrue(false, "Could not perform publish action for the service data");
                } else {

                    // function to get status whether consumptions have been created or not
                    String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId);//waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
                    logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                    if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);

                        customAssert.assertAll();

                    } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                        logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                        customAssert.assertAll();

                    }

                    consumptionIds = invoiceHelper.consumptionIds;
                    // after consumptions have been created successfully
                    logger.info("Consumption Ids are : [{}]", consumptionIds);

                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(invoiceFlowsConfigFilePath, invoiceFlowsConfigFileName, flowToTest,
                            "finalconsumptionvalues").trim().split(Pattern.quote(","));

                    for (int i = 0; i < consumptionIds.size(); i++) {

                        result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                        if (!result) {
                            logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
                            customAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                    "Hence skipping validation");
                            customAssert.assertAll();

                        }

                        if (flowToTest.contains("arc")) {
                            result = workflowActionsHelper.performWorkflowAction(ConfigureConstantFields.getEntityIdByName(consumptions), consumptionIds.get(i), approveAction);
                            //result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds.get(i), consumptionEntity, consumptionEntitySectionUrlName);
                            customAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptions + "having id : " + consumptionIds.get(i));

                            if (!result) {
                                logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
                                customAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
                                        "Hence skipping validation");
                                customAssert.assertAll();

                            }
                        }
                    }
                }
            }

            List<Integer> billingIds = invoiceHelper.getBillingDataIds(contractId, serviceDataId);

            if (billingIds.isEmpty()) {
                customAssert.assertTrue(false, "Billing data not formed hence terminating execution");
                customAssert.assertAll();
            }

            logger.info("Billing data {}", billingIds.toString());

            String entityIds = String.valueOf(serviceDataId);
            String fieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "field id ui", "consumption available");

            String consumptionAvailable = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk edit field id", "consumption available");

            Boolean consumptionAvailableValue = true;

            String bulkEditPayload = createPayloadForBulkEdit(entityIds, fieldIds, consumptionAvailable, consumptionAvailableValue);

            Boolean bulkEditStatus = UserTasksHelper.updateBulkEdit(serviceDataEntityTypeId, bulkEditPayload, customAssert);

            if (!bulkEditStatus) {
                customAssert.assertTrue(false, "Bulk Edit job failed for consumption approved true");
            }

            int templateId = 1027;

            String bulkUpdateFileName = "BulkUpdate.xlsm";
            String sheetName = "Service Data";

            Boolean downloadStatus = BulkTemplate.downloadBulkUpdateTemplate(uploadFilePath, bulkUpdateFileName, templateId, serviceDataEntityTypeId, String.valueOf(serviceDataId));

            if (!downloadStatus) {
                customAssert.assertTrue(false, "Error while bulk template download");
            } else {
                List<String> rowData = XLSUtils.getExcelDataOfOneRow(uploadFilePath, bulkUpdateFileName, sheetName, 1);

                int consumptionAvailColNum = -1;

                for (int i = 0; i < rowData.size(); i++) {

                    if (rowData.get(i).equals(fieldIds)) {
                        consumptionAvailColNum = i;
                        break;
                    }
                }

                consumptionAvailableValue = true;
                XLSUtils.updateColumnValue(uploadFilePath, bulkUpdateFileName, sheetName, 6, consumptionAvailColNum, String.valueOf(consumptionAvailableValue));
                int bulkUpdateTemplateId = 1027;

                Boolean bulkUpdateStatus = UserTasksHelper.updateBulkUpdate(uploadFilePath, bulkUpdateFileName, serviceDataEntityTypeId, bulkUpdateTemplateId, customAssert);

                if (!bulkUpdateStatus) {
                    customAssert.assertTrue(false, "Bulk Update failed for consumption available true");
                }

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }
    }

    @Test(dataProvider = "flowsToValidate",enabled = true)
    public void TestC151221(String flowToTest){

        CustomAssert customAssert = new CustomAssert();

        try {

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flowToTest);
            if (contractId == -1) {
                logger.info("Cannot create contract");
                customAssert.assertTrue(false, "Cannot create contract");
            }

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flowToTest, contractId);

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId);

            if (serviceDataId == -1) {
                logger.info("Cannot create service data");
                customAssert.assertTrue(false, "Cannot create service data");
            }
            int serviceDataIdParent = serviceDataId;

            Clone clone = new Clone();
            String cloneResponse = clone.hitClone(serviceDataEntity, serviceDataIdParent);

            JSONObject cloneResponseJson = new JSONObject(cloneResponse);
            JSONObject dataJson = cloneResponseJson.getJSONObject("body").getJSONObject("data");
            if (!dataJson.getJSONObject("parentService").has("values")) {
                String parentClientId = dataJson.getJSONObject("serviceIdClient").getString("values");

                JSONObject valuesJson = new JSONObject();

                valuesJson.put("name", parentClientId);
                dataJson.getJSONObject("parentService").put("values", valuesJson);
            }

            String serviceIdClientChild = dataJson.getJSONObject("serviceIdClient").getString("values");
            serviceIdClientChild = serviceIdClientChild + "_Child12";
            dataJson.getJSONObject("serviceIdClient").put("values", serviceIdClientChild);

            String serviceIdSupplierChild = dataJson.getJSONObject("serviceIdSupplier").getString("values");
            serviceIdSupplierChild = serviceIdSupplierChild + "_Child12";
            dataJson.getJSONObject("serviceIdSupplier").put("values", serviceIdSupplierChild);
            dataJson.getJSONObject("billingAvailable").put("values", false);
            dataJson.getJSONObject("pricingAvailable").put("values", false);
            dataJson.remove("history");

            JSONObject createEntityJson = new JSONObject();
            JSONObject createEntityBodyJson = new JSONObject();
            createEntityBodyJson.put("data", dataJson);
            createEntityJson.put("body", createEntityBodyJson);

            Create create = new Create();
            create.hitCreate(serviceDataEntity, createEntityJson.toString());
            String createResponse = create.getCreateJsonStr();
            int newServiceDataIdChild = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            if (newServiceDataIdChild == -1) {
                logger.error("Unable to create child service data");
                customAssert.assertTrue(false, "Unable to create child service data");
            }

            int templateId = 1027;

            String bulkUpdateFileName = "BulkUpdate.xlsm";
            String sheetName = "Service Data";
            String pricingAvailFieldId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"field id ui","pricing available");
            String isBillableFieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"field id ui","billing data");
            Boolean downloadStatus = BulkTemplate.downloadBulkUpdateTemplate(uploadFilePath, bulkUpdateFileName, templateId, serviceDataEntityTypeId, String.valueOf(newServiceDataIdChild));

            if (!downloadStatus) {
                customAssert.assertTrue(false, "Error while bulk template download");
            } else {
                List<String> rowData = XLSUtils.getExcelDataOfOneRow(uploadFilePath, bulkUpdateFileName, sheetName, 1);

                String pricingAvailableValue = "Yes";

                int pricingAvailColNum = -1;
                int isBillableColNum = -1;

                for (int i = 0; i < rowData.size(); i++) {

                    if (rowData.get(i).equals(pricingAvailFieldId)) {
                        pricingAvailColNum = i;
                    }

                    if (rowData.get(i).equals(isBillableFieldIds)) {
                        isBillableColNum = i;
                    }

                }

                XLSUtils.updateColumnValue(uploadFilePath, bulkUpdateFileName, sheetName, 6, pricingAvailColNum, pricingAvailableValue);
                int bulkUpdateTemplateId = 1027;

                Boolean bulkUpdateStatus = UserTasksHelper.updateBulkUpdate(uploadFilePath, bulkUpdateFileName, serviceDataEntityTypeId, bulkUpdateTemplateId, customAssert);

                if (bulkUpdateStatus) {
                    customAssert.assertTrue(false, "Bulk Update should fail when Pricing Available is true for child service data");
                }

                pricingAvailableValue = "No";
                String isBillableValue = "Yes";

                XLSUtils.updateColumnValue(uploadFilePath, bulkUpdateFileName, sheetName, 6, pricingAvailColNum, pricingAvailableValue);
                XLSUtils.updateColumnValue(uploadFilePath, bulkUpdateFileName, sheetName, 6, isBillableColNum, isBillableValue);

                bulkUpdateStatus = UserTasksHelper.updateBulkUpdate(uploadFilePath, bulkUpdateFileName, serviceDataEntityTypeId, bulkUpdateTemplateId, customAssert);

                if (bulkUpdateStatus) {
                    customAssert.assertTrue(false, "Bulk Update should fail when Is Billable is true for child service data");
                }

                Boolean pricingAvailableValueBoolean = true;
                Boolean isBillableBoolean = true;

                //Testing the bulk edit scenario
                String bulkEditPayload = createPayloadForBulkEdit(String.valueOf(newServiceDataIdChild), pricingAvailFieldId,pricingAvailFieldId , pricingAvailableValueBoolean);

                Boolean bulkEditStatus = UserTasksHelper.updateBulkEdit(serviceDataEntityTypeId, bulkEditPayload, customAssert);

                if (bulkEditStatus) {
                    customAssert.assertTrue(false, "Bulk Edit job failed for consumption approved true");
                }

                bulkEditPayload = createPayloadForBulkEdit(String.valueOf(newServiceDataIdChild), isBillableFieldIds,isBillableFieldIds, isBillableBoolean);

                bulkEditStatus = UserTasksHelper.updateBulkEdit(serviceDataEntityTypeId, bulkEditPayload, customAssert);

                if (bulkEditStatus) {
                    customAssert.assertTrue(false, "Bulk Edit job failed for consumption approved true");
                }

            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario");
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }
    }

    private String createPayloadForBulkEdit(String entityIds,String fieldIds,String pricingForReportingId,Boolean pricingForReportingValue){

        String bulkEditCreatePayload = "{\"body\":{\"data\":{\"pricingForReporting\":" +
                "{\"name\":\"pricingForReporting\",\"id\":" + pricingForReportingId + ",\"values\":" + pricingForReportingValue + ",\"multiEntitySupport\":false}}," +
                "\"globalData\":{\"entityIds\":[" + entityIds + "],\"fieldIds\":[" + fieldIds + "],\"isGlobalBulk\":true}}}";

        return bulkEditCreatePayload;

    }

}
