package com.sirionlabs.test.consumption;

import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;

import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;

import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.util.*;
import java.util.regex.Pattern;


@Listeners(value = MyTestListenerAdapter.class)
public class TestConsumptionValidation {
    private final static Logger logger = LoggerFactory.getLogger(TestConsumptionValidation.class);

    private String configFilePath;
    private String configFileName;

    private String serviceDataEntity = "service data";
    private String serviceDataEntitySectionUrlName;
    private String baseFilePath;
    private String entityIdMappingFileName;
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
    private String ARCRRCSheetNameInXLSXFile = "ARCRRC";
    private String splitSheetNameInXLSXFile = "Split";

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestConsumptionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestConsumptionConfigFileName");

        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");

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

        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");
    }

    @DataProvider
    public Object[][] dataProviderForConsumptionCreation(){
        logger.info("Setting all Consumption Creation Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest();
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForReferenceTabValidation(){
        logger.info("Setting Reference Tab Validation Flows");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTestForRefValidation();
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForConsumptionCreation",enabled = true)
    public void TestConsumptionCreationDifferentFrequencies(String flowToTest) {

        CustomAssert csAssert = new CustomAssert();

        String publishAction = "publish";

        InvoicePricingHelper pricingObj = new InvoicePricingHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, "fixed fee flow 1");

        int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId);

        if(flowToTest.equalsIgnoreCase("split quarterly frequency") || flowToTest.equalsIgnoreCase("split monthly frequency")) {

            String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "pricingstemplatefilename");
            Boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, csAssert);

            if (!pricingFile) {

                csAssert.assertTrue(false, "download and edit Pricing Upload file failed for flow " + flowToTest);
                csAssert.assertAll();
                return;
            } else {
                String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {

                    List<Integer> allTaskIds = InvoiceHelper.getAllTaskIds();
                    //Wait for Pricing Scheduler to Complete
                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

                    if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                        csAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                flowToTest + "]");

                        csAssert.assertAll();
                        return;
                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                        logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);

                        logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                        csAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                "Hence failing Flow [" + flowToTest + "]");

                        return;
                    }
                }
            }
        }
        //boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataId, serviceDataEntity, serviceDataEntitySectionUrlName);

        boolean result = new WorkflowActionsHelper().performWorkflowAction(ConfigureConstantFields.getEntityIdByName(serviceDataEntity),serviceDataId,publishAction);
        if (!result) {
            logger.info("Could not perform publish action for the service data {}, still continuing", serviceDataId);
            result = true;
        }

        if (result) {

            // function to get status whether consumptions have been created or not
            String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId);
            logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

            if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                csAssert.assertTrue(false,"Consumption Creation Task failed. Hence skipping further validation for Flow "+ flowToTest);
                return;
            } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                csAssert.assertTrue(false,"Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                throw new SkipException("Skipping this test");
            }

            // after consumptions have been created successfully
            logger.info("Consumption Ids are : [{}]", invoiceHelper.consumptionIds);
            csAssert.assertTrue(true,"Consumptions have been created successfully");

            if(flowToTest.equalsIgnoreCase("service data monthly frequency")){

                if(!(invoiceHelper.consumptionIds.size() == 12)){
                    logger.error("Number of expected and actual consumptions generated differ for serviceDataId " + serviceDataId);
                    csAssert.assertTrue(false,"Number of expected and actual consumptions generated differ for serviceDataId " + serviceDataId + "expected 12 and actual " + invoiceHelper.consumptionIds.size());
                }else {
                    logger.info("Deleting Contract Id ");
                    String urlName = EntityOperationsHelper.getUrlNameOfEntity("contracts");
                    EntityOperationsHelper.deleteEntityRecord("contracts", contractId, urlName);

                    logger.info("Deleting Service Data ");
                    urlName = EntityOperationsHelper.getUrlNameOfEntity("service data");
                    EntityOperationsHelper.deleteEntityRecord("service data", serviceDataId, urlName);
                }
            }else if(flowToTest.equalsIgnoreCase("service data quarterly frequency")){
                if(!(invoiceHelper.consumptionIds.size() == 4)){
                    logger.error("Number of expected and actual consumptions generated differ for serviceDataId " + serviceDataId);
                    csAssert.assertTrue(false,"Number of expected and actual consumptions generated differ for serviceDataId " + serviceDataId + "expected 4 and actual " + invoiceHelper.consumptionIds.size());
                }else {

                    logger.info("Deleting Contract Id ");
                    String urlName = EntityOperationsHelper.getUrlNameOfEntity("contracts");
                    EntityOperationsHelper.deleteEntityRecord("contracts", contractId, urlName);

                    logger.info("Deleting Service Data ");
                    urlName = EntityOperationsHelper.getUrlNameOfEntity("service data");
                    EntityOperationsHelper.deleteEntityRecord("service data", serviceDataId, urlName);
                }
            }else if(flowToTest.equalsIgnoreCase("split quarterly frequency") || flowToTest.equalsIgnoreCase("split monthly frequency")){
                if(!(invoiceHelper.consumptionIds.size() == 2)){
                    logger.error("Number of expected and actual consumptions generated differ for serviceDataId " + serviceDataId);
                    csAssert.assertTrue(false,"Number of expected and actual consumptions generated differ for serviceDataId " + serviceDataId + "expected 4 and actual " + invoiceHelper.consumptionIds.size());
                }else {

                    logger.info("Deleting Contract Id ");
                    String urlName = EntityOperationsHelper.getUrlNameOfEntity("contracts");
                    EntityOperationsHelper.deleteEntityRecord("contracts", contractId, urlName);

                    logger.info("Deleting Service Data ");
                    urlName = EntityOperationsHelper.getUrlNameOfEntity("service data");
                    EntityOperationsHelper.deleteEntityRecord("service data", serviceDataId, urlName);
                }
            }
        }else {

            logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
            csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                    "Hence skipping validation");

        }
        csAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderForReferenceTabValidation",enabled = true)
    public void validateReferenceTabConsumptions(String flowToTest) {

        CustomAssert csAssert = new CustomAssert();

        String publishAction = "publish";

        InvoicePricingHelper pricingObj = new InvoicePricingHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        //Get Contract that will be used for Invoice Flow Validation
        int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flowToTest);
        int serviceDataId;
        ArrayList<Integer> consumptionIds = null;
        if (contractId != -1) {
            logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName, flowToTest, contractId);

            serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId);

            logger.info("Created Service Data Id : [{}]", serviceDataId);

            if (serviceDataId != -1) {
                logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);

                UserTasksHelper.removeAllTasks();

                logger.info("Hitting Fetch API.");
                List<Integer> allTaskIds = InvoiceHelper.getAllTaskIds();

                String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                        "pricingstemplatefilename");

                Boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, csAssert);

                if (pricingFile) {

                    String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                    if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {

                        //Wait for Pricing Scheduler to Complete
                        String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

                        if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                            logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                            csAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                    flowToTest + "]");

                            csAssert.assertAll();
                            return;
                        } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                            logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);

                            csAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                    "Hence failing Flow [" + flowToTest + "]");

                            csAssert.assertAll();
                            return;
                        } else { // in case if get processed successfully

                            //boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataId, serviceDataEntity, serviceDataEntitySectionUrlName);

                            boolean result = new WorkflowActionsHelper().performWorkflowAction(ConfigureConstantFields.getEntityIdByName(serviceDataEntity),serviceDataId,publishAction);
                            if (!result) {
                                logger.info("Could not perform publish action for the service data {}, still continuing", serviceDataId);
                                result = true;
                            }

                            if (result) {

                                String consumptionCreatedStatus = invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId);
                                logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                                if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                    logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                    csAssert.assertTrue(false, "Consumption Creation Task failed. Hence skipping further validation for Flow " + flowToTest);
                                    csAssert.assertAll();
                                    return;
                                } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                    logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                                    csAssert.assertTrue(false, "Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow " + flowToTest);
                                    csAssert.assertAll();
                                    throw new SkipException("Skipping this test");
                                }

                                // after consumptions have been created successfully
                                consumptionIds = invoiceHelper.consumptionIds;
                                logger.info("Consumption Ids are : [{}]", consumptionIds);
                                String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                                        "finalconsumptionvalues").trim().split(Pattern.quote(","));

                                for (int i = 0; i < consumptionIds.size(); i++) {
                                    result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
                                    if (result == false) {
                                        logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
                                        csAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                                "Hence skipping validation");

                                        csAssert.assertAll();
                                        return;


                                    } else {
                                        String approveAction = "approve";
                                        String consumptionEntity = "consumptions";
                                        String consumptionEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "url_name");

                                        //result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds.get(i), consumptionEntity, consumptionEntitySectionUrlName);
                                        result = new WorkflowActionsHelper().performWorkflowAction(ConfigureConstantFields.getEntityIdByName(consumptionEntity),consumptionIds.get(i),approveAction);
                                        csAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptionEntity + "having id : " + consumptionIds.get(i));

                                        if (!result) {
                                            logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
                                            csAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
                                                    "Hence skipping validation");

                                            csAssert.assertAll();
                                            return;
                                        }
                                    }
                                }
                            } else {
                                logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                                csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                        "Hence skipping validation");

                                csAssert.assertAll();
                                return;
                            }
                        }
                    }
                }
            } else {
                csAssert.assertTrue(false, "Unable to create service data");
                csAssert.assertAll();
                return;
            }
        } else {
            csAssert.assertTrue(false, "Unable to create contract");
            csAssert.assertAll();
            return;
        }

        ArrayList<Integer> invoiceLineItemIdList = new ArrayList<>();
        ArrayList<Integer> invoiceIdList = new ArrayList<>();
        HashMap<Integer,Integer> invoiceLineItemIdMapExpected = new HashMap<>();
        //Get Invoice Id
        for (int j = 0; j < 2; j++) {
            int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName, invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest);
            logger.info("Created Invoice Id is : [{}]", invoiceId);

            int invoiceLineItemId = -1;

            if (invoiceId != -1) {
                invoiceIdList.add(invoiceId);
                logger.info("Creating multiple line items from same invoice");
                for (int i = 0; i < 2; i++) {

                    //Get Invoice Line Item Id
                    invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId);
                    logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);

                    if (invoiceLineItemId != -1) {
                        invoiceLineItemIdList.add(invoiceLineItemId);
                        invoiceLineItemIdMapExpected.put(invoiceLineItemId,invoiceId);
                        String submitForApproval = "submitforapproval";
                        String invoiceLineItemEntity = "invoice line item";
                        String invoiceEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "url_name");
                        boolean result = EntityWorkFlowActionsHelper.performAction(submitForApproval, invoiceLineItemId, invoiceLineItemEntity, invoiceEntitySectionUrlName);

                        if (!result) {
                            logger.error("Not Being able to Perform  " + submitForApproval + " Action on " + invoiceLineItemEntity + "having id : " + invoiceLineItemId);
                            csAssert.assertTrue(false, "Not Being able to Perform  " + submitForApproval + " Action on " + invoiceLineItemEntity + "having id : " + invoiceLineItemId);
                            logger.info("So skipping further Validation : Not Marking test As Fail in this case");
                            return;
                        }else {
                            invoiceLineItemIdMapExpected.put(invoiceLineItemId,invoiceId);
                        }
                    } else {
                        csAssert.assertTrue(false, "Error while creating invoice line item for the flow " + flowToTest);
                        csAssert.assertAll();
                        return;
                    }
                }
            } else {
                csAssert.assertTrue(false, "Error while creating invoice for the flow " + flowToTest);
                csAssert.assertAll();
                return;
            }
        }

        if(!validateReferenceTabConsumptionForLineItem(consumptionIds.get(0),invoiceLineItemIdMapExpected,csAssert)){
            csAssert.assertTrue(false,"validation of reference tab unsuccessful for consumption id "+ consumptionIds.get(0));
        }
        csAssert.assertAll();
    }


    private Boolean downloadAndEditPricingFile(String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj,CustomAssert csAssert) {
        Boolean pricingFile = true;
        try {
            //Download Pricing Template
            logger.info("Downloading Pricing Template for Flow [{}]", flowToTest);
            if (!pricingObj.downloadPricingTemplate(pricingTemplateFilePath, templateFileName, serviceDataId)) {
                logger.error("Pricing Template Download failed for Flow [{}].", flowToTest);
                return false;
            }
            if(flowToTest.contains("split")) {
                if (!(editPricingFileForSplit(templateFileName, flowToTest, serviceDataId))) {
                    csAssert.assertTrue(false, "Split file edit failed for flow " + flowToTest);
                    return false;
                }
            }
            if (!editPricingFileForPricing(templateFileName, flowToTest)) {
                csAssert.assertTrue(false, "Pricing file edit failed for flow " + flowToTest);
                return false;
            }

            if (!(editPricingFileForARCRRC(templateFileName, flowToTest, serviceDataId))) {
                csAssert.assertTrue(false, "ARC/RRC file edit failed for flow " + flowToTest);
                return false;
            }

        }catch (Exception e) {
            logger.error("Exception while getting Pricing Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;

    }

    private List<String> getIdList(String configFilePath, String configFileName, String sectionName, String propertyName) {

        String value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName);
        List<String> idList = new ArrayList<>();

        if (!value.trim().equalsIgnoreCase("")) {
            String ids[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName).split(",");

            for (String id : ids)
                idList.add(id.trim());
        }
        return idList;
    }

    // this function wSill edit the ARC/RRC sheet based on the map created in getValuesMapForArcRrcSheet
    private Boolean editPricingFileForARCRRC(String templateFileName, String flowToTest,Integer serviceDataId) {
        Boolean pricingFile = false;
        try {

            Map<Integer, Map<Integer, Object>> arcValuesMap = InvoiceHelper.getValuesMapForArcRrcSheet(configFilePath,configFileName,flowToTest);
            boolean editTemplate = XLSUtils.editMultipleRowsData(pricingTemplateFilePath, templateFileName, ARCRRCSheetNameInXLSXFile, arcValuesMap);

            if (editTemplate == true) {
                return true;
            } else {
                logger.error("Error While Updating the ARC Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
                return false;
            }


        } catch (Exception e) {
            logger.error("Exception while getting ARC/RRC Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;
    }

    // this function will edit the split sheet based on the map created in getValuesMapForSplitSheet
    private Boolean editPricingFileForSplit(String templateFileName, String flowToTest,Integer serviceDataId) {
        Boolean pricingFile;
        try {
            int splitRowNumber = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"startingrownum"));
            XLSUtils.copyRowData(pricingTemplateFilePath,templateFileName,splitSheetNameInXLSXFile,splitRowNumber,splitRowNumber +1);

            Map<Integer, Map<Integer, Object>> splitValuesMap = InvoiceHelper.getValuesMapForSplitSheet(configFilePath,configFileName,flowToTest);
            boolean editTemplate = XLSUtils.editMultipleRowsData(pricingTemplateFilePath, templateFileName, splitSheetNameInXLSXFile, splitValuesMap);

            if (editTemplate) {
                pricingFile = true;
                return pricingFile;
            } else {
                logger.error("Error While Updating the Split Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
                pricingFile = false;
                return pricingFile;
            }


        } catch (Exception e) {
            logger.error("Exception while getting Split Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
            pricingFile =false;
        }
        return pricingFile;
    }

    private Boolean editPricingFileForPricing(String templateFileName, String flowToTest){

        //Edit Pricing Template
        logger.info("Editing Pricing Template for Flow [{}]", flowToTest);

        String pricingTemplateSheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "pricingstemplatesheetname");
        Integer totalRowsToEdit = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                "numberofrowstoedit"));
        Integer startingRowNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                "startingrownum"));
        Integer volumeColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                "volumecolumnnum"));
        Integer rateColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                "ratecolumnnum"));
        List<String> volumeColumnValues = this.getIdList(configFilePath, configFileName, flowToTest, "volumecolumnvalues");
        List<String> rateColumnValues = this.getIdList(configFilePath, configFileName, flowToTest, "ratecolumnvalues");

        int count = 0;
        for (int rowNum = startingRowNum; rowNum < (startingRowNum + totalRowsToEdit); rowNum++) {
            Map<Integer, Object> columnNumAndValueMap = new HashMap<>();
            columnNumAndValueMap.put(volumeColumnNum, volumeColumnValues.get(count));
            columnNumAndValueMap.put(rateColumnNum, rateColumnValues.get(count));

            Boolean isSuccess = XLSUtils.editRowData(pricingTemplateFilePath, templateFileName, pricingTemplateSheetName, rowNum, columnNumAndValueMap);

            count++;
            if (!isSuccess) {
                logger.error("Pricing Template Editing Failed for Flow [{}].", flowToTest);

                return false;
            }

        }
        return true;
    }

    private List<String> getFlowsToTest() {
        List<String> flowsToTest = new ArrayList<>();

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testallflows");
            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
                flowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"allflowsconsumptioncreafreqwise").split(","));
            } else {
                flowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstotest").split(Pattern.quote(",")));

            }
        } catch (Exception e) {
            logger.error("Exception while getting Flows to Test for Invoice Validation. {}", e.getMessage());
        }
        return flowsToTest;
    }

    private List<String> getFlowsToTestForRefValidation() {
        List<String> flowsToTest = new ArrayList<>();

        try {

            flowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "referencetabvalidationflows").split(Pattern.quote(",")));

        } catch (Exception e) {
            logger.error("Exception while getting Flows to Test for Invoice Validation. {}", e.getMessage());
        }
        return flowsToTest;
    }

    //Test case C10786 and C10789 for multiple line items with multiple invoices
    private Boolean validateReferenceTabConsumptionForLineItem(int consumptionId,HashMap<Integer,Integer> invoiceLineItemIdMapExpected,CustomAssert csAssert){

        logger.info("Validating References tab of consumption show page");
        Boolean validationRefTab = true;

        TabListData tabListData = new TabListData();
        int referenceTabID = 429;
        tabListData.hitTabListData(referenceTabID,176,consumptionId);
        String tabListResponse = tabListData.getTabListDataResponseStr();

        JSONObject tabListResponseJson = new JSONObject(tabListResponse);

        JSONArray dataArray = tabListResponseJson.getJSONArray("data");
        JSONObject indvDataJson;
        JSONArray indvRowDetails;
        JSONObject indvColumn;
        String columnName;

        HashMap<Integer,Integer> invoiceLineItemIdMapActual = new HashMap<>();
        Integer lineItemIdReferenceTab = 0;
        Integer invoiceIdReferenceTab = 0;
        for(int i =0;i<dataArray.length();i++){

            indvDataJson = dataArray.getJSONObject(i);
            indvRowDetails = JSONUtility.convertJsonOnjectToJsonArray(indvDataJson);

            for(int j=0;j<indvRowDetails.length();j++){

                indvColumn = indvRowDetails.getJSONObject(j);
                columnName = indvColumn.get("columnName").toString();

                switch (columnName){
                    case "lineitemid":

                        lineItemIdReferenceTab = Integer.parseInt(indvColumn.get("columnName").toString().split(":;")[1]);
                        break;
                    case "invoiceid":
                        invoiceIdReferenceTab = Integer.parseInt(indvColumn.get("columnName").toString().split(":;")[1]);
                        break;

                }
            }

            invoiceLineItemIdMapActual.put(lineItemIdReferenceTab,invoiceIdReferenceTab);
        }
        Integer lineItemId;
        Integer invoiceId;
        for (Map.Entry<Integer,Integer> entry : invoiceLineItemIdMapExpected.entrySet()){
            lineItemId = entry.getKey();
            invoiceId = entry.getValue();
            if(invoiceLineItemIdMapActual.containsKey(lineItemId)){
                if(invoiceLineItemIdMapActual.get(lineItemId).equals(invoiceId)){
                    csAssert.assertTrue(true,"Expected invoice id " + invoiceId +" matched for invoice line item id "+ lineItemId + " for reference tab of consumption id " + consumptionId);
                }else {
                    validationRefTab = false;
                    csAssert.assertTrue(false,"Expected invoice id " + invoiceId +" mismatch for invoice line item id "+ lineItemId + " for reference tab of consumption id " + consumptionId);
                }
            }else {
                validationRefTab = false;
                csAssert.assertTrue(false,"line item id : " + lineItemId + "not found in reference tab of consumption id : " + consumptionId);
            }
        }
        return validationRefTab;
    }
}
