package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
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
public class TestInvoiceLineItemReferenceTab {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceLineItemReferenceTab.class);

    private String baseFilePath;
    private String configFilePath = null;
    private String configFileName = null;

    private String pricingTemplateFilePath;
    private String entityIdMappingFileName;
    private String entityCreationFilePath;
    private String entityCreationRequiredFieldsFileName;
    private String entityCreationExtraFieldsFileName;
    private String forecastTemplateFilePath;
    private String invoicePricingHelperConfigFilePath = null;
    private String invoicePricingHelperConfigFileName = null;

    private Boolean killAllSchedulerTasks = false;
    private Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private Boolean failTestIfLineItemValidationNotCompletedWithinTimeOut = true;

    private Long pollingTime = 5000L;
    private Long pricingSchedulerTimeOut = 1200000L;
    private Long forcastSchedulerTimeOut = 1200000L;
    private Long lineItemValidationTimeOut = 1200000L;
    private Long consumptionToBeCreatedTimeOut = 1200000L;

    private String invoiceSectionName;
    private String contractSectionName;
    private String serviceDataSectionName;
    private String invoiceLineItemSectionName;

    private String invoiceEntitySectionUrlName;
    private String serviceDataEntitySectionUrlName;
    private String consumptionEntitySectionUrlName;

    private String publishAction = "publish";
    private String approveAction = "approve";
    private String submitForApproval = "submitforapproval";

    private String serviceDataEntity = "service data";
    private String consumptionEntity = "consumptions";
    private String invoiceLineItemEntity = "invoice line item";

    private int serviceDataEntityTypeId;
    private int consumptionsEntityTypeId;
    private int invoiceLineItemEntityTypeId;

    private int[] consumptionIds;
    private ArrayList<Integer> consumptionIdsList;
    private String ARCRRCSheetNameInXLSXFile = "ARCRRC";

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

    Show show = new Show();
    Edit edit = new Edit();

    @BeforeClass
    public void beforeClass(){
        try {
            configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestInvoiceLineItemReferencesConfigFilePath");
            configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestInvoiceLineItemReferencesConfigFileName");

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


            invoicePricingHelperConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFilePath");
            invoicePricingHelperConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFileName");

            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killallschedulertasks");
            if (temp != null && temp.trim().equalsIgnoreCase("true"))
                killAllSchedulerTasks = true;

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
            if (temp != null && temp.trim().equalsIgnoreCase("false"))
                failTestIfJobNotCompletedWithinSchedulerTimeOut = false;

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfLineItemValidationNotCompletedWithinTimeOut");
            if (temp != null && temp.trim().equalsIgnoreCase("false"))
                failTestIfLineItemValidationNotCompletedWithinTimeOut = false;

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lineItemValidationTimeOut");
            if (temp != null && NumberUtils.isParsable(temp.trim()))
                lineItemValidationTimeOut = Long.parseLong(temp);

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingSchedulerWaitTimeOut");
            if (temp != null && NumberUtils.isParsable(temp.trim()))
                pricingSchedulerTimeOut = Long.parseLong(temp);

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingschedulerpollingtime");
            if (temp != null && NumberUtils.isParsable(temp.trim()))
                pollingTime = Long.parseLong(temp);

            pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");
            forecastTemplateFilePath = pricingTemplateFilePath;

            // for publising of service data
            entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
            serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
            serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));
            consumptionEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "url_name");
            invoiceEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "url_name");

            invoiceLineItemEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "entity_type_id"));
            consumptionsEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "entity_type_id"));

            entityCreationFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "EntityCreationFilePath");
            entityCreationRequiredFieldsFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "EntityCreationFileName");
            entityCreationExtraFieldsFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "EntityCreationExtraFieldsFileName");
        }catch (Exception e){
            logger.error("Exception while setting variables in before class");
        }
    }

    @DataProvider
    public Object[][] dataProviderForReferencesTab() {
        logger.info("Setting all Invoice Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getFlowsToTest();
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForReferencesTab")
    public void testReferenceTabLineItem(String flowToTest){

        CustomAssert csAssert = new CustomAssert();
        int contractId = 1;
        int serviceDataIdParent;
        int serviceDataIdChild;
        int invoiceId;
        int invoiceLineItemId;
        int consumptionId;
        int numberOfChildServiceDataToCreate;
        ArrayList<Integer> childServiceDataList = new ArrayList<>();
        HashMap<Integer,String> consumptionIdsMap = new HashMap<>();
        String serviceDataType = "";
        try {

            invoiceSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"invoicesectionname");
            contractSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"contractsectionname");
            serviceDataSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"servicedatasectionname");
            invoiceLineItemSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"invoicelineitemsectionname");

            numberOfChildServiceDataToCreate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"numberofchilddervicedatatocreate"));

            InvoicePricingHelper pricingObj = new InvoicePricingHelper();

            contractId = InvoiceHelper.getContractId(entityCreationFilePath,entityCreationRequiredFieldsFileName,entityCreationExtraFieldsFileName,contractSectionName);
            if(contractId != -1) {

                logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                updateServiceDataAndInvoiceConfig(flowToTest, contractId);

                serviceDataSectionName = "arc flow linkedservicedata service data";
                serviceDataIdParent = getServiceDataId(serviceDataSectionName, contractId,1);
                logger.info("Created Parent Service Data Id : [{}]", serviceDataIdParent);

                serviceDataSectionName = "arc flow child service data";
                //creating child service data
                updateServiceDataAndInvoiceConfig("arc flow child service data", contractId);

                //Updating Parent in child service data
                updateParentServiceInServiceDataConfig("arc flow child service data",serviceDataIdParent);

                //Creating child service data
                for(int i=1;i<=numberOfChildServiceDataToCreate;i++){
                    int serviceDataNumber = i + 1;
                    serviceDataIdChild = getServiceDataId("arc flow child service data", contractId,serviceDataNumber);
                    childServiceDataList.add(serviceDataIdChild);
                }

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                boolean uploadPricing = true;
                String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "uploadPricing");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    uploadPricing = false;


                if (uploadPricing) {
                    String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                            "pricingstemplatefilename");
                    Boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataIdParent, pricingObj);

                    // changes for ARC RRC FLOW
                    if (pricingFile) {

                        if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                                "servicedatatype") != null) {
                            serviceDataType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                                    "servicedatatype");
                        }


                        if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                            pricingFile = editPricingFileForARCRRC(pricingTemplateFileName, flowToTest, serviceDataIdParent, pricingObj);
                            if (pricingFile) {
                                String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                                if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                                    temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                    if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                        //Wait for Pricing Scheduler to Complete
                                        String pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);

                                        if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                            logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                            csAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                                    flowToTest + "]");

                                            csAssert.assertAll();
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
                                        } else // in case if get processed successfully
                                        {
                                            for (int childServiceData : childServiceDataList) {
                                                boolean result = new WorkflowActionsHelper().performWorkFlowStepV2(serviceDataEntityTypeId,childServiceData,publishAction,csAssert);
                                                        //EntityWorkFlowActionsHelper.performAction(publishAction, childServiceData, serviceDataEntity, serviceDataEntitySectionUrlName);

                                                // if service data got published
                                                if (result) {

                                                    // function to get status whether consumptions have been created or not
                                                    String consumptionCreatedStatus = waitForConsumptionToBeCreated(flowToTest, childServiceData);
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
                                                    for (int id : consumptionIds) {
                                                        consumptionId = id;
                                                        consumptionIdsMap.put(consumptionId, String.valueOf(childServiceData));
                                                    }

                                                    // after consumptions have been created successfully
                                                    logger.info("Consumption Ids are : [{}]", consumptionIds);
                                                    String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                                                            "finalconsumptionvalues").trim().split(Pattern.quote(","));

                                                } else {

                                                    logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);

                                                    csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                            "Hence skipping validation");

                                                    //csAssert.assertAll();
                                                    csAssert.assertAll();
                                                    return;


                                                }
                                            }

                                            //Get Invoice Id
                                            invoiceId = getInvoiceId(flowToTest);
                                            logger.info("Created Invoice Id is : [{}]", invoiceId);
                                            if (invoiceId != -1) {
                                                //Get Invoice Line Item Id
                                                invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataIdParent);
                                                logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);

                                                if (invoiceLineItemId != -1) {

                                                    if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                                        boolean result = new WorkflowActionsHelper().performWorkFlowStepV2(invoiceLineItemEntityTypeId,invoiceLineItemId,approveAction,csAssert);
                                                                //EntityWorkFlowActionsHelper.performAction(submitForApproval, invoiceLineItemId, invoiceLineItemEntity, invoiceEntitySectionUrlName);


                                                        if (!result) {
                                                            logger.error("Not Being able to Perform  " + submitForApproval + " Action on " + invoiceLineItemEntity + "having id : " + invoiceLineItemId);
                                                            logger.info("So skipping further Validation : Not Marking test As Fail in this case");
                                                            return;
                                                        }

                                                        if(!validateReferencesTab(consumptionIdsMap,invoiceLineItemId,csAssert)){
                                                            logger.error("References tab validated unsuccessfully for invoice line item " + invoiceLineItemId);
                                                        }
                                                    }
                                                }else{
                                                    csAssert.assertTrue(false,"Invoice line item not created, returning.");
                                                }
                                            }else{
                                                csAssert.assertTrue(false,"Invoice not created, returning.");
                                            }
                                        }
                                    }else{
                                        csAssert.assertTrue(false,"Pricing upload task failed, returning.");
                                    }
                                }else{
                                    csAssert.assertTrue(false,"Pricing upload task failed, returning.");
                                }
                            }else{
                                csAssert.assertTrue(false,"Pricing edit for ARC RRC failed, returning.");
                            }
                        }
                    }else{
                        csAssert.assertTrue(false,"Pricing not uploaded, returning.");
                    }
                }else{
                    csAssert.assertTrue(false,"Pricing upload is not set, returning.");
                }
            }
            else{
                csAssert.assertTrue(false,"Contract not created, returning.");
            }

        }catch (Exception e){
            logger.error("Exception while validating references tab on invoice line item");
        }

        csAssert.assertAll();
    }

    private Boolean editPricingFileForARCRRC(String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj) {
        Boolean pricingFile = false;
        try {

            Map<Integer, Map<Integer, Object>> arcValuesMap = getValuesMapForArcRrcSheet(flowToTest);
            boolean editTemplate = pricingObj.editPricingTemplateMultipleRows(pricingTemplateFilePath, templateFileName, ARCRRCSheetNameInXLSXFile,
                    arcValuesMap);

            if (editTemplate == true) {
                return true;
            } else {
                logger.error("Error While Updating the Pricing Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
                return false;
            }


        } catch (Exception e) {
            logger.error("Exception while getting ARC/RRC Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;
    }

    private List<String> getFlowsToTest() {
        List<String> flowsToTest = new ArrayList<>();

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testallflows");
            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
                flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
            } else {
                String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstovalidate").split(Pattern.quote(","));
                for (String flow : allFlows) {
                    if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
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

    private int getContractId(String flowToTest) {
        int contractId = -1;
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createcontract");
            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractid"));
            } else {
                //Create New Contract
                boolean createLocalContract = true;
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createlocalcontract");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    createLocalContract = false;

                String createResponse = Contract.createContract(entityCreationFilePath, entityCreationRequiredFieldsFileName, entityCreationFilePath,
                        entityCreationExtraFieldsFileName, contractSectionName, createLocalContract);

                contractId = CreateEntity.getNewEntityId(createResponse, "contracts");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Contract Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return contractId;
    }

    private void updateServiceDataAndInvoiceConfig(String flowToTest, int contractId) {
        try {
            if (contractId != -1) {
                logger.info("Updating Service Data Config File for Flow [{}] and Contract Id {}.", flowToTest, contractId);
                String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedatasectionname");
                if (temp != null)
                    serviceDataSectionName = temp.trim();

                int contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
                String contractName = ShowHelper.getValueOfField(contractEntityTypeId, contractId, "title");
                UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationRequiredFieldsFileName, serviceDataSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationRequiredFieldsFileName, serviceDataSectionName, "sourceid",
                        String.valueOf(contractId));

                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicesectionname");
                if (temp != null)
                    invoiceSectionName = temp.trim();

                UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationRequiredFieldsFileName, invoiceSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationRequiredFieldsFileName, invoiceSectionName, "sourceid", String.valueOf(contractId));
            }
        } catch (Exception e) {
            logger.error("Exception while updating Service Data & Invoice Config File for Flow [{}] and Contract Id {}. {}", flowToTest, contractId, e.getStackTrace());
        }
    }

    private int getServiceDataId(String flowToTest, int contractId,int serviceDataNumberForTheContract) {
        int serviceDataId = -1;
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createservicedata");

            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedataid"));
            } else {
                //Create New Service Data
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedatasectionname");
                if (temp != null)
                    serviceDataSectionName = temp.trim();

                //Update Service Data Extra Fields.
                UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, serviceDataSectionName, "serviceIdClient",
                        "new client", "newClient" + contractId + "_" + serviceDataNumberForTheContract);
                UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, serviceDataSectionName, "serviceIdSupplier",
                        "new supplier", "newSupplier" + contractId + "_" + serviceDataNumberForTheContract);

                Boolean createLocalServiceData = true;
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createlocalservicedata");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    createLocalServiceData = false;

                String createResponse = ServiceData.createServiceData(entityCreationFilePath, entityCreationRequiredFieldsFileName, entityCreationFilePath,
                        entityCreationExtraFieldsFileName, serviceDataSectionName, createLocalServiceData);

                serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

                //Revert Service Data Extra Fields changes.
                UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, serviceDataSectionName, "serviceIdClient",
                        "newClient" + contractId + "_" + serviceDataNumberForTheContract, "new client");
                UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, serviceDataSectionName, "serviceIdSupplier",
                        "newSupplier" + contractId + "_" + serviceDataNumberForTheContract, "new supplier");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return serviceDataId;
    }

    private void killAllSchedulerTasks() {
        UserTasksHelper.removeAllTasks();
    }

    private Boolean downloadAndEditPricingFile(String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj) {
        Boolean pricingFile = false;
        try {
            //Download Pricing Template
            logger.info("Downloading Pricing Template for Flow [{}]", flowToTest);
            if (!pricingObj.downloadPricingTemplate(pricingTemplateFilePath, templateFileName, serviceDataId)) {
                logger.error("Pricing Template Download failed for Flow [{}].", flowToTest);
                return false;
            }

            //Edit Pricing Template
            logger.info("Editing Pricing Template for Flow [{}]", flowToTest);

            String sheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "pricingstemplatesheetname");
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

                Boolean isSuccess = pricingObj.editPricingTemplate(pricingTemplateFilePath, templateFileName, sheetName, rowNum, columnNumAndValueMap);

                count++;
                if (!isSuccess) {
                    logger.error("Pricing Template Editing Failed for Flow [{}].", flowToTest);
                    pricingFile = false;
                    break;
                } else
                    pricingFile = true;
            }
        } catch (Exception e) {
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

    // this will create the row , <columnNumber,value> for editing the ARC/RRC Sheet
    private Map<Integer, Map<Integer, Object>> getValuesMapForArcRrcSheet(String flowToTest) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {


            int numberOfColumnToEditForEachRow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "numberofcolumntoeditforeachrowforarc"));

            String[] arcRowNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "arcrownumber").trim().split(Pattern.quote(","));

            String[] arcColumnNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "arccolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "arcvalue").trim().split(Pattern.quote(","));

            for (int i = 0; i < arcRowNumber.length; i++) {

                Map<Integer, Object> innerValuesMap = new HashMap<>();
                for (int j = 0; j < numberOfColumnToEditForEachRow; j++) {
                    innerValuesMap.put(Integer.parseInt(arcColumnNumber[i * numberOfColumnToEditForEachRow + j]), values[i * numberOfColumnToEditForEachRow + j]);
                }
                valuesMap.put(Integer.parseInt(arcRowNumber[i]), innerValuesMap);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Pricing Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    private String waitForPricingScheduler(String flowToTest, List<Integer> oldIds) {
        String result = "pass";
        logger.info("Waiting for Pricing Scheduler to Complete for Flow [{}].", flowToTest);
        try {
            logger.info("Time Out for Pricing Scheduler is {} milliseconds", pricingSchedulerTimeOut);
            long timeSpent = 0;
            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            logger.info("Getting Task Id of Pricing Upload Job");
            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), oldIds);

            if (newTaskId != -1) {
                Boolean taskCompleted = false;
                logger.info("Checking if Pricing Upload Task has completed or not.");

                while (timeSpent < pricingSchedulerTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    fetchObj.hitFetch();
                    logger.info("Getting Status of Pricing Upload Task.");
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        taskCompleted = true;
                        logger.info("Pricing Upload Task Completed. ");
                        logger.info("Checking if Pricing Upload Task failed or not.");
                        if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                            result = "fail";

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Pricing Upload Task is not finished yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= pricingSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }
            } else {
                logger.info("Couldn't get Pricing Upload Task Job Id. Hence waiting for Task Time Out i.e. {}", pricingSchedulerTimeOut);
                Thread.sleep(pricingSchedulerTimeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Pricing Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }
        return result;
    }
    private void updateParentServiceInServiceDataConfig(String flowToTest,int parentServiceData){

        logger.info("Updating Service Data Config File for Flow [{}] and Contract Id {}.", flowToTest);
        try {

            String parentServiceDataName = ShowHelper.getValueOfField(serviceDataEntityTypeId,parentServiceData,"serviceidclient");

            String parentServiceDataString = "values -> {\"name\": \"" + parentServiceDataName + "\",\"id\": " + parentServiceData+ ",\"url\": \"/tblcontractservicedatas/show/" + parentServiceData +"\"}";
            UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, serviceDataSectionName, "parentService", parentServiceDataString);
            String parentEntityId = "{\"name\":\"parentEntityId\",\"values\":4041}";

        }catch (Exception e){
            logger.error("Exception while updating Service Data Section for the Service Data ", flowToTest, parentServiceData, e.getStackTrace());
        }
    }

    private String waitForConsumptionToBeCreated(String flowToTest, int serviceDataId) {
        int offset = 0; // default value for creating payload
        int size = 20; // default value for creating payload
        String result = "pass";
        logger.info("Waiting for Consumption to be Created for Flow [{}].", flowToTest);
        try {
            show.hitShow(serviceDataEntityTypeId, serviceDataId);
            String showPageResponseStr = show.getShowJsonStr();
            List<String> dataUrl = show.getShowPageTabUrl(showPageResponseStr, Show.TabURL.dataURL);

            String consumptionDataURL = null;
            for (String Url : dataUrl) {
                if (Url.contains("listRenderer/list/376/tablistdata")) // for consumption
                {
                    consumptionDataURL = Url;
                    break;
                }
            }

            if (consumptionDataURL != null) {

                logger.info("Time Out for Consumption to be created is {} milliseconds", consumptionToBeCreatedTimeOut);
                long timeSpent = 0;


                Boolean taskCompleted = false;
                logger.info("Checking if Consumption has been created or not.");

                String payload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":" +
                        offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc\",\"filterJson\":{}}}";

                while (timeSpent < consumptionToBeCreatedTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);
                    logger.info("Hitting tab data API for Consumption for service data Id : [{}]", serviceDataId);

                    String showPageDataUrlResponseStr = Show.hitshowPageTabUrl(consumptionDataURL, payload);
                    JSONObject showPageDataUrlResponseJson = new JSONObject(showPageDataUrlResponseStr);

                    int numberOfConsumption = showPageDataUrlResponseJson.getJSONArray("data").length();


                    if (numberOfConsumption > 0) {
                        taskCompleted = true;
                        logger.info("Consumptions have been created. ");


                        consumptionIds = new int[numberOfConsumption];

                        for (int i = 0; i < numberOfConsumption; i++) {

                            JSONObject data = showPageDataUrlResponseJson.getJSONArray("data").getJSONObject(i);
                            Set<String> keys = data.keySet();
                            for (String key : keys) {
                                String columnName = data.getJSONObject(key).getString("columnName");
                                if (columnName.contentEquals("id")) {
                                    consumptionIds[i] = Integer.parseInt(data.getJSONObject(key).getString("value").split(":;")[1]);
                                    break;
                                }
                            }
                        }

                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("time spent is : [{}]", timeSpent);
                        logger.info("Consumptions haven't been created yet.");
                    }
                }
                if (!taskCompleted && timeSpent >= pricingSchedulerTimeOut) {
                    //Task didn't complete within given time.
                    result = "skip";
                }


            } else {
                logger.error("There is no consumption URL in the show page response of Service data : [{}] for flow : [{}]", serviceDataId, flowToTest);
                result = "skip";
                return result;
            }


        } catch (Exception e) {
            logger.error("Exception while Waiting for Consumption to get created to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            result = "fail";
        }


        return result;
    }

    // this will update the final Consumption in Created Consumption
    private boolean updateFinalConsumption(String flowToTest, int consumptionId, double finalConsumption) {
        boolean result = true;


        try {
            String editAPIResponse = edit.hitEdit(consumptionEntity, consumptionId);
            JSONObject editPostAPIPayload = new JSONObject(editAPIResponse);

            editPostAPIPayload.getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption").put("values", finalConsumption);

            logger.info("editPostAPIPayload is : {}", editPostAPIPayload);

            String editPostAPIResponse = edit.hitEdit(consumptionEntity, editPostAPIPayload.toString());
            JSONObject editPostAPIResponseJson = new JSONObject(editPostAPIResponse);

            if ((editPostAPIResponseJson.has("header") && editPostAPIResponseJson.getJSONObject("header").has("response")
                    && editPostAPIResponseJson.getJSONObject("header").getJSONObject("response").has("status")
                    && editPostAPIResponseJson.getJSONObject("header").getJSONObject("response").getString("status")
                    .trim().equalsIgnoreCase("success"))) {
                logger.info("Consumption has been updated successfully for flow [{}]", flowToTest);
            } else {
                logger.error("Error While Updating final Consumption in Created Consumption having id : [{}] for flow [{}]", consumptionId, flowToTest);
                result = false;
                return result;

            }


        } catch (Exception e) {
            logger.error("Error While Updating final Consumption in Created Consumption having id : [{}]", consumptionId);
            result = false;
        }

        return result;
    }

    private int getInvoiceId(String flowToTest) {
        int invoiceId = -1;
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicesectionname");
            if (temp != null)
                invoiceSectionName = temp.trim();

            String createResponse = Invoice.createInvoice(entityCreationFilePath, entityCreationRequiredFieldsFileName, entityCreationFilePath, entityCreationExtraFieldsFileName,
                    invoiceSectionName, true);
            invoiceId = CreateEntity.getNewEntityId(createResponse, "invoice");

            logger.info("Updating Invoice Line Item Config File for Flow [{}] and Invoice Id {}.", flowToTest, invoiceId);
            int invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
            String invoiceName = ShowHelper.getValueOfField(invoiceEntityTypeId, invoiceId, "title");

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();

            UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationRequiredFieldsFileName, invoiceLineItemSectionName, "sourcename",
                    invoiceName);
            UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationRequiredFieldsFileName, invoiceLineItemSectionName, "sourceid",
                    String.valueOf(invoiceId));
        } catch (Exception e) {
            logger.error("Exception while getting Invoice Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return invoiceId;
    }

    private int getInvoiceLineItemId(String flowToTest, int serviceDataId) {
        int lineItemId = -1;
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();

            logger.info("Updating Invoice Line Item Property Service Id Supplier in Extra Fields Config File for Flow [{}] and Service Data Id {}.",
                    invoiceLineItemSectionName, serviceDataId);
            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName(serviceDataEntity);
            String serviceDataName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "title");
            String serviceIdSupplierName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "serviceIdSupplier");
            String serviceIdSupplierUpdatedName = serviceDataName + " (" + serviceIdSupplierName + ")";
            UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new name", serviceIdSupplierUpdatedName);
            UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new id", String.valueOf(serviceDataId));

            String createResponse = InvoiceLineItem.createInvoiceLineItem(entityCreationFilePath, entityCreationRequiredFieldsFileName, entityCreationFilePath,
                    entityCreationExtraFieldsFileName, invoiceLineItemSectionName, true);
            lineItemId = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

            //Reverting Invoice Line Item Extra Fields changes.
            UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", serviceIdSupplierUpdatedName, "new name");
            UpdateFile.updateConfigFileProperty(entityCreationFilePath, entityCreationExtraFieldsFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", String.valueOf(serviceDataId), "new id");
        } catch (Exception e) {
            logger.error("Exception while getting Invoice Line Item Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return lineItemId;
    }

    private void verifyInvoiceLineItem(String flowToTest, int invoiceLineItemId, CustomAssert csAssert) {
        try {
            logger.info("Verifying Invoice Line Item for Flow [{}] having Id {}.", flowToTest, invoiceLineItemId);
            int invoiceLineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName(invoiceLineItemEntity);
            long timeSpent = 0;

            while (timeSpent <= lineItemValidationTimeOut && ShowHelper.isLineItemUnderOngoingValidation(invoiceLineItemEntityTypeId, invoiceLineItemId)) {
                logger.info("Invoice Line Item having Id {} is still Under Ongoing Validation. Waiting for it to finish validation.", invoiceLineItemId);
                logger.info("time spent is : [{}]", timeSpent);
                Thread.sleep(10000);
                timeSpent += 10000;
            }

            if (timeSpent < lineItemValidationTimeOut) {
                //Line Item Validation is Completed.
                String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedresult");
                String actualResult = ShowHelper.getValueOfField(invoiceLineItemEntityTypeId, invoiceLineItemId, "validationStatus");
                logger.info("Expected Result is [{}] and Actual Result is [{}].", expectedResult, actualResult);
                csAssert.assertTrue(actualResult.trim().toLowerCase().contains(expectedResult.trim().toLowerCase()),
                        "Invoice Line Item Validation failed as Expected Value is " + expectedResult + " and Actual Value is " + actualResult);
            } else {
                //Line Item Validation is not yet Completed.
                logger.info("Invoice Line Item Validation couldn't be completed for Flow [{}] within TimeOut {} milliseconds", flowToTest, lineItemValidationTimeOut);
                if (failTestIfLineItemValidationNotCompletedWithinTimeOut) {
                    logger.error("FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [{}]", flowToTest);
                    csAssert.assertTrue(false, "FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned on. Hence failing flow [" +
                            flowToTest + "]");
                } else {
                    logger.info("FailTestIfLineItemValidationNotCompletedWithinTimeOut flag is turned off. Hence not failing flow [{}]", flowToTest);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Invoice Line Item for Flow [{}] having Id {}. {}", flowToTest, invoiceLineItemId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while verifying Invoice Line Item for Flow [" + flowToTest + "] having Id " + invoiceLineItemId +
                    ". " + e.getMessage());
        }
    }

    private Boolean validateReferencesTab(HashMap<Integer,String> consumptionIdsMap,int invoiceLineItemId,CustomAssert csAssert ){

        boolean referencesTabValidationStatus = true;
        Map<Integer,String> actualConsumptionIdMap = new HashMap<>();


        try {
            int referenceTabId = 356;

            String serviceDataIdExpected;
            String showResponse;
            String serviceIdSupplier;
            String serviceDataName;
            String expected_Service_Data;
            JSONObject showResponseJson;

            ListRendererTabListData listRendererTabListData = new ListRendererTabListData();

            String tabListPayload = "{\"filterMap\":{\"entityTypeId\":" + consumptionsEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

            listRendererTabListData.hitListRendererTabListData(referenceTabId, invoiceLineItemEntityTypeId, invoiceLineItemId, tabListPayload);
            if (listRendererTabListData.getAPIResponseCode().contains("200")) {
                String referenceTabResponse = listRendererTabListData.getTabListDataJsonStr();
                if (APIUtils.validJsonResponse(referenceTabResponse)) {
                    JSONObject referencesTabListJson = new JSONObject(referenceTabResponse);
                    JSONArray dataArray = referencesTabListJson.getJSONArray("data");
                    JSONObject dataJson;
                    JSONArray dataJsonArray;
                    JSONObject indvColumnJson;

                    Integer expectedConsumptionId;
                    String actualServiceDataName = "";
                    String actualServiceData = "";
                    int actualConsumptionId = -1;

                    String actualStatus = "";

                    for (int i = 0; i < dataArray.length(); i++) {

                        dataJson = dataArray.getJSONObject(i);
                        dataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataJson);

                        for (int j = 0; j < dataJsonArray.length(); j++) {
                            indvColumnJson = dataJsonArray.getJSONObject(j);
                            if (indvColumnJson.get("columnName").equals("id")) {

                                actualConsumptionId = Integer.parseInt(indvColumnJson.get("value").toString().split(":;")[1]);
                            } else if (indvColumnJson.get("columnName").equals("name")) {
                                actualServiceDataName = indvColumnJson.get("value").toString();
                            } else if (indvColumnJson.get("columnName").equals("service_data")) {
                                actualServiceData = indvColumnJson.get("value").toString().split(":;")[0];
                            } else if (indvColumnJson.get("columnName").equals("status")) {
                                actualStatus = indvColumnJson.get("value").toString();
                                if(!actualStatus.equals("Upcoming")){
                                    logger.error("Status mismatch for invoice line item " + invoiceLineItemId);
                                    csAssert.assertTrue(false,"Status mismatch for invoice line item " + invoiceLineItemId);
                                    referencesTabValidationStatus = false;
                                }
                            }

                        }

                        actualConsumptionIdMap.put(actualConsumptionId,actualServiceData + ":;" + actualServiceDataName);
                    }
                    String actualServiceDataDetails;
                    for (Map.Entry<Integer,String> consumptionIdsMapEntry : consumptionIdsMap.entrySet()){

                        expectedConsumptionId =  consumptionIdsMapEntry.getKey();
                        serviceDataIdExpected = consumptionIdsMapEntry.getValue();

                        if(actualConsumptionIdMap.containsKey(expectedConsumptionId)){

                            actualServiceDataDetails = actualConsumptionIdMap.get(expectedConsumptionId);
                            actualServiceDataName = actualServiceDataDetails.split(":;")[1];
                            actualServiceData = actualServiceDataDetails.split(":;")[0];

                            show.hitShow(serviceDataEntityTypeId, Integer.parseInt(serviceDataIdExpected));
                            showResponse = show.getShowJsonStr();

                            showResponseJson = new JSONObject(showResponse);
                            serviceIdSupplier = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdSupplier").get("values").toString();
                            serviceDataName = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
                            expected_Service_Data = serviceDataName + " ( " + serviceIdSupplier + " ) ";


                            if(serviceDataName.equals(actualServiceDataName) ){
                                logger.info("Service Data matched for references tab for invoice line item " + invoiceLineItemId + "and Consumption id " + actualConsumptionId);
                            }else {
                                logger.error("Service Data Mismatch for references tab for invoice line item " + invoiceLineItemId + "and Consumption id " + actualConsumptionId);
                                csAssert.assertTrue(false,"Service Data Mismatch for references tab for invoice line item " + invoiceLineItemId + "and Consumption id " + actualConsumptionId);
                                referencesTabValidationStatus = false;
                            }


                            if(actualServiceData.equals(expected_Service_Data)){
                                logger.info("Service Data Name matched for references tab for invoice line item " + invoiceLineItemId + "and Consumption id " + actualConsumptionId);
                            }else {
                                logger.error("Service Data Name Mismatch for references tab for invoice line item " + invoiceLineItemId + "and Consumption id " + actualConsumptionId);
                                csAssert.assertTrue(false,"Service Data Name Mismatch for references tab for invoice line item " + invoiceLineItemId + "and Consumption id " + actualConsumptionId);
                                referencesTabValidationStatus = false;
                            }
                        }else {
                            logger.error("Expected consumption id" + expectedConsumptionId + "not present in references tab of invoice line item " + invoiceLineItemId);
                        }
                    }
                } else {
                    logger.error("Reference tab response is not a valid json");
                    referencesTabValidationStatus = false;
                }

            } else {
                logger.error("Error while opening reference tab");
                referencesTabValidationStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while validating references tab");
            referencesTabValidationStatus = false;
        }

        return referencesTabValidationStatus;
    }
}
