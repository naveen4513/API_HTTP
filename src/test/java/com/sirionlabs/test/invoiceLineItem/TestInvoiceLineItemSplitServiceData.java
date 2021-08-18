package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestInvoiceLineItemSplitServiceData extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceLineItemSplitServiceData.class);

    private String configFilePath;
    private String configFileName;
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


    private String serviceDataEntitySectionUrlName;
    private String consumptionEntitySectionUrlName;
    private String invoiceEntitySectionUrlName;

    private Integer serviceDataEntityTypeId;

    private Boolean killAllSchedulerTasks = false;
    private Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private static Boolean failTestIfLineItemValidationNotCompletedWithinTimeOut = true;
    private static Long lineItemValidationTimeOut = 1200000L;
    private static Long pricingSchedulerTimeOut = 1200000L;

    private static Long pollingTime = 5000L;

    private String contractSectionName;
    private String serviceDataSectionName;
    private String invoiceSectionName;
    private String invoiceLineItemSectionName;

    private String serviceDataEntity = "service data";
    private String consumptionEntity = "consumptions";
    private String invoiceLineItemEntity = "invoice line item";

    private int[] consumptionIds;

    private Show show;
    private Edit edit;

    @BeforeClass
    public void beforeClass() {

        String entityIdMappingFileName;
        String baseFilePath;

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemSplitServiceDataFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemSplitServiceDataFileName");
        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");

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

//        invoicePricingHelperConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFilePath");
//        invoicePricingHelperConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFileName");

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

        // for publising of service data
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));
        consumptionEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "url_name");
        invoiceEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "url_name");

        show = new Show();
        edit = new Edit();

        testCasesMap = getTestCasesMapping();
    }

    @Test()
    public void testInvoiceLineItemSplitServiceData(){

        CustomAssert csAssert = new CustomAssert();
        InvoicePricingHelper pricingObj = new InvoicePricingHelper();

        String flowToTest = "invoice line item split service data";

        int contractId;
        int serviceDataId;
        int invoiceId;
        int invoiceLineItemId;
        String serviceDataType = "fixedFee";
        String publishAction = "publish";
        String approveAction = "approve";
        String submitForApproval = "submitforapproval";

        //Tobe deleted
        {

        }
        //

        try {

            clearEntitySections();
            setEntitySectionsForFlow(flowToTest);

            //Get Contract that will be used for Invoice Flow Validation
            contractId = getContractId(flowToTest);
            if (contractId != -1) {
                logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);

                updateServiceDataAndInvoiceConfig(flowToTest, contractId);

                serviceDataId = getServiceDataId(flowToTest, contractId);

                logger.info("Created Service Data Id : [{}]", serviceDataId);


                if (serviceDataId != -1) {
                    logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);

                    if(!(updateServiceDataDates(serviceDataId))) {
                        //Skip the test
                        csAssert.assertTrue(false,"Error while updating service data dates to staring date of the month");
                        return;
                    }
                    //Kill All Scheduler Tasks if Flag is On.
                    if (killAllSchedulerTasks) {
                        logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
                        killAllSchedulerTasks();
                    }

                    logger.info("Hitting Fetch API.");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();
                    List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                    Boolean uploadPricing = true;
                    String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "uploadPricing");
                    if (temp != null && temp.trim().equalsIgnoreCase("false"))
                        uploadPricing = false;


                    if (uploadPricing) {
                        String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                                "pricingstemplatefilename");
                        Boolean pricingFile = downloadAndEditPricingFile(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);

                        // changes for ARC RRC FLOW
                        if (pricingFile) {
                            // getting the actual service data Type
                            if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                                    "servicedatatype") != null) {
                                serviceDataType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                                        "servicedatatype");
                            }


                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {
                                pricingFile = editPricingFileForARCRRC(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);
                                pricingFile = editPricingFileForSplit(pricingTemplateFileName, flowToTest, serviceDataId, pricingObj);

                            }
                            // changes for ARC RRC FLOW Ends here


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
                                        } else
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
                                        // Consumption Part will start here only if flow is not fixed fee

                                        if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc") || serviceDataType.contentEquals("forecast"))  // later Condition to be modified as not fixed fee
                                        {
                                            boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataId, serviceDataEntity, serviceDataEntitySectionUrlName);

                                            // if service data got published
                                            if (result) {

                                                // function to get status whether consumptions have been created or not
                                                String consumptionCreatedStatus = waitForConsumptionToBeCreated(flowToTest, serviceDataId);
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

                                                // after consumptions have been created successfully
                                                logger.info("Consumption Ids are : [{}]", consumptionIds);
                                                String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                                                        "finalconsumptionvalues").trim().split(Pattern.quote(","));


                                                for (int i = 0; i < consumptionIds.length; i++) {
                                                    result = updateFinalConsumption(flowToTest, consumptionIds[i], Double.parseDouble(finalConsumptions[i]));
                                                    if (!result) {
                                                        logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds[i], flowToTest);
                                                        csAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
                                                                "Hence skipping validation");
                                                        addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                        csAssert.assertAll();
                                                        return;

                                                    } else {
                                                        result = EntityWorkFlowActionsHelper.performAction(approveAction, consumptionIds[i], consumptionEntity, consumptionEntitySectionUrlName);
                                                        csAssert.assertTrue(result, "Not Being able to Perform  " + approveAction + " Action on " + consumptionEntity + "having id : " + consumptionIds[i]);

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
                                            } else {
                                                logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
                                                csAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                        "Hence skipping validation");
                                                addTestResult(getTestCaseIdForMethodName("testInvoiceFlow"), csAssert);
                                                csAssert.assertAll();
                                                return;
                                            }
                                        }

                                        // Consumption Part will End here

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
                    //Get Invoice Id
                    invoiceId = getInvoiceId(flowToTest);
                    logger.info("Created Invoice Id is : [{}]", invoiceId);
                    if (invoiceId != -1) {
                        //Get Invoice Line Item Id
                        invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataId);
                        logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);

                        if (invoiceLineItemId != -1) {

                            if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {

                                boolean result = EntityWorkFlowActionsHelper.performAction(submitForApproval, invoiceLineItemId, invoiceLineItemEntity, invoiceEntitySectionUrlName);

                                if (!result) {
                                    logger.error("Not Being able to Perform  " + submitForApproval + " Action on " + invoiceLineItemEntity + "having id : " + invoiceLineItemId);
                                    logger.info("So skipping further Validation : Not Marking test As Fail in this case");
                                    return;
                                }
                            }

                            verifyInvoiceLineItem(flowToTest, invoiceLineItemId, csAssert);

                        } else {
                            logger.error("Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);

                            csAssert.assertTrue(false, "Couldn't get Invoice Line Item Id for Invoice Flow Validation. Hence skipping Flow [" +
                                    flowToTest + "]");
                        }
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

                csAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());

            csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
        }

    }

    private void setEntitySectionsForFlow(String flowToTest) {
        logger.info("Setting Entity Sections for Flow [{}]", flowToTest);
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractsectionname");
            if (temp != null)
                contractSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "servicedatasectionname");
            if (temp != null)
                serviceDataSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicesectionname");
            if (temp != null)
                invoiceSectionName = temp.trim();

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();
        } catch (Exception e) {
            logger.error("Exception while Setting Entity Sections for Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
    }
    private int getContractId(String flowToTest) {
        int contractId = -1;
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createcontract");
            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractid"));
            } else {
                //Create New Contract
                Boolean createLocalContract = true;
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createlocalcontract");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    createLocalContract = false;

                String createResponse = Contract.createContract(contractConfigFilePath, contractConfigFileName, contractConfigFilePath,
                        contractExtraFieldsConfigFileName, contractSectionName, createLocalContract);

                contractId = CreateEntity.getNewEntityId(createResponse, "contracts");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Contract Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return contractId;
    }

    private void clearEntitySections() {
        contractSectionName = serviceDataSectionName = invoiceSectionName = invoiceLineItemSectionName = "default";
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
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataSectionName, "sourceid",
                        String.valueOf(contractId));

                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicesectionname");
                if (temp != null)
                    invoiceSectionName = temp.trim();

                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, "sourcename", contractName);
                UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceConfigFileName, invoiceSectionName, "sourceid", String.valueOf(contractId));
            }
        } catch (Exception e) {
            logger.error("Exception while updating Service Data & Invoice Config File for Flow [{}] and Contract Id {}. {}", flowToTest, contractId, e.getStackTrace());
        }
    }
    private int getServiceDataId(String flowToTest, int contractId) {
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
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                        "new client", "newClient" + contractId);
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                        "new supplier", "newSupplier" + contractId);

                Boolean createLocalServiceData = true;
                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "createlocalservicedata");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    createLocalServiceData = false;

                String createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                        serviceDataExtraFieldsConfigFileName, serviceDataSectionName, createLocalServiceData);

                serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

                //Revert Service Data Extra Fields changes.
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdClient",
                        "newClient" + contractId, "new client");
                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "serviceIdSupplier",
                        "newSupplier" + contractId, "new supplier");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Service Data Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return serviceDataId;
    }

    private int getInvoiceId(String flowToTest) {
        int invoiceId = -1;
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicesectionname");
            if (temp != null)
                invoiceSectionName = temp.trim();

            String createResponse = Invoice.createInvoice(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath, invoiceExtraFieldsConfigFileName,
                    invoiceSectionName, true);
            invoiceId = CreateEntity.getNewEntityId(createResponse, "invoice");

            logger.info("Updating Invoice Line Item Config File for Flow [{}] and Invoice Id {}.", flowToTest, invoiceId);
            int invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
            String invoiceName = ShowHelper.getValueOfField(invoiceEntityTypeId, invoiceId, "title");

            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();

            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemSectionName, "sourcename",
                    invoiceName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemSectionName, "sourceid",
                    String.valueOf(invoiceId));
        } catch (Exception e) {
            logger.error("Exception while getting Invoice Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return invoiceId;
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
    private void killAllSchedulerTasks() {
        UserTasksHelper.removeAllTasks();
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

    // this will create the row , <columnNumber,value> for editing the ARC/RRC Sheet
    private Map<Integer, Map<Integer, Object>> getValuesMapForSplitSheet(String flowToTest) {
        Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

        try {

            int numberOfColumnToEditForEachRow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "numberofcolumntoeditforeachrowforsplit"));

            String[] splitRowNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "splitrownumber").trim().split(Pattern.quote(","));

            String[] splitColumnNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "splitcolumnnumber").trim().split(Pattern.quote(","));

            String[] values = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "splitvalue").trim().split(Pattern.quote(","));

            for (int i = 0; i < splitRowNumber.length; i++) {

                Map<Integer, Object> innerValuesMap = new HashMap<>();
                for (int j = 0; j < numberOfColumnToEditForEachRow; j++) {
                    innerValuesMap.put(Integer.parseInt(splitColumnNumber[i * numberOfColumnToEditForEachRow + j]), values[i * numberOfColumnToEditForEachRow + j]);
                }
                valuesMap.put(Integer.parseInt(splitRowNumber[i]), innerValuesMap);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Values Map for Pricing Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return valuesMap;
    }

    // this function will edit the ARC/RRC sheet based on the map created in getValuesMapForArcRrcSheet
    private Boolean editPricingFileForARCRRC(String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj) {

        String ARCRRCSheetNameInXLSXFile = "ARCRRC";
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

    // this function will edit the Split sheet based on the map created in getValuesMapForArcRrcSheet
    private Boolean editPricingFileForSplit(String templateFileName, String flowToTest, Integer serviceDataId, InvoicePricingHelper pricingObj) {

        String SplitSheetNameInXLSXFile = "Split";
        Boolean pricingFile = false;
        try {
            XLSUtils.copyRowData(pricingTemplateFilePath, templateFileName, SplitSheetNameInXLSXFile, 6,7);
            Map<Integer, Map<Integer, Object>> splitValuesMap = getValuesMapForSplitSheet(flowToTest);
            boolean editTemplate = pricingObj.editPricingTemplateMultipleRows(pricingTemplateFilePath, templateFileName, SplitSheetNameInXLSXFile,
                    splitValuesMap);

            if (editTemplate == true) {
                return true;
            } else {
                logger.error("Error While Updating the Split Sheet for [{}] : [{}] : [{}] : ", templateFileName, flowToTest, serviceDataId);
                return false;
            }


        } catch (Exception e) {
            logger.error("Exception while getting ARC/RRC Sheet using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return pricingFile;
    }

    // this function will check whether any data has been created under Charges tab of Service Data or Not
    private boolean isChargesCreated(int serviceDataId) {

        int chargesTabId = 309;
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
    boolean isARCRRCCreated(int serviceDataId) {

        int ARCRRCTabId = 311;

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

    // this will update the final Consumption in Created Consumption
    private boolean updateFinalConsumption(String flowToTest, int consumptionId, double finalConsumption) {
        boolean result = true;


        try {
            String editAPIResponse = edit.hitEdit(consumptionEntity, consumptionId);
            JSONObject editAPIResponseJson = new JSONObject(editAPIResponse);

            JSONObject editPostAPIPayload = editAPIResponseJson;
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
            //return result;

        }

        return result;
    }

    private int getInvoiceLineItemId(String flowToTest, int serviceDataId) {
        int lineItemId = -1;
        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFileName, configFileName, flowToTest, "invoicelineitemsectionname");
            if (temp != null)
                invoiceLineItemSectionName = temp.trim();

            logger.info("Updating Invoice Line Item Property Service Id Supplier in Extra Fields Config File for Flow [{}] and Service Data Id {}.",
                    invoiceLineItemSectionName, serviceDataId);
            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName(serviceDataEntity);
            String serviceDataName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "title");
            String serviceIdSupplierName = ShowHelper.getValueOfField(serviceDataEntityTypeId, serviceDataId, "serviceIdSupplier");
            String serviceIdSupplierUpdatedName = serviceDataName + " (" + serviceIdSupplierName + ")";
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new name", serviceIdSupplierUpdatedName);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", "new id", String.valueOf(serviceDataId));

            String createResponse = InvoiceLineItem.createInvoiceLineItem(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemConfigFilePath,
                    invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, true);
            lineItemId = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

            //Reverting Invoice Line Item Extra Fields changes.
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", serviceIdSupplierUpdatedName, "new name");
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName,
                    "serviceIdSupplier", String.valueOf(serviceDataId), "new id");
        } catch (Exception e) {
            logger.error("Exception while getting Invoice Line Item Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
        }
        return lineItemId;
    }
    // this method will check whether consumption has been created or not
    // return "pass" if created
    // return "fail" if something went wrong in code or assumption
    // return "skip" if consumption would not created in specified time consumptionToBeCreatedTimeOut
    private String waitForConsumptionToBeCreated(String flowToTest, int serviceDataId) {
        int offset = 0; // default value for creating payload
        int size = 20; // default value for creating payload
        Long consumptionToBeCreatedTimeOut = 1200000L;
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
    /*
        This method will return the status of Pricing Scheduler as String.
        Possible Values are 'Pass', 'Fail', 'Skip'.
        'Pass' specifies that pricing scheduler completed and records processed successfully.
        'Fail' specifies that pricing scheduler failed
        'Skip' specifies that pricing scheduler didn't finish within time.
         */
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

    private Boolean updateServiceDataDates(int serviceDataId) {

        String editResponse;
        Boolean serviceDataUpdationStatus = true;
        JSONObject editResponseJson;
        String entityName = "service data";
        editResponse = edit.hitEdit(entityName, serviceDataId);
        try {
            String startDate = DateUtils.getCurrentMonthStartDateinMMDDYYYY();
            String endDate = DateUtils.getCurrentMonthEndDateinMMDDYYYY();
            editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("startDate").put("values",startDate);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("endDate").put("values",endDate);

//            editResponseJson.toString();

            edit.hitEdit(entityName,editResponseJson.toString());
            if(!(edit.editAPIResponseCode.contains("200"))){
                logger.error("Error while updating service data dates");
                serviceDataUpdationStatus =false;
            }
        }catch (Exception e){
            logger.error("Exception while updating service data dates");
            serviceDataUpdationStatus =false;
        }

        return serviceDataUpdationStatus;
    }
}
