package com.sirionlabs.test.serviceData.Pricing;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.invoice.InvoicePricingTemplateDownload;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.velocity.runtime.directive.Parse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

//SIR-5994
public class ServiceDataPricing {

    private final Logger logger = LoggerFactory.getLogger(ServiceDataPricing.class);
    private String configFilePath = "src/test/resources/TestConfig/ServiceData/Pricing";
    private String configFileName = "ServiceDataPricing.cfg";
    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;
    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;
    private String changeRequestConfigFilePath;
    private String changeRequestConfigFileName;
    private String changeRequestExtraFieldsConfigFileName;
    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String cdrExtraFieldsConfigFileName;
    private int serviceDataTypeId = 64;
    private int crEntityTypeId = 63;
    private int cdrEntityTypeId = 160;

    @BeforeClass
    public void beforeClass() throws Exception {

        //Contract Config files
        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        changeRequestConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFilePath");
        changeRequestConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFileName");
        changeRequestExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestExtraFieldsFileName");

        //CDR Config files
        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("CDRFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRFileName");
        cdrExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRExtraFieldsFileName");

    }

    @Test(enabled = false)
    public void C3509() {

        /*
        call listing api for pricing download
        - check that all the data in the list contains pricing available as yes/pricing on report as yes

        - download pricing template for pricing available and pricing for report iteratively for each

        - download pricing template for pricing available and pricing for report both
         */
        CustomAssert customAssert = new CustomAssert();

        try {
            String filterListingForPricing = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "pricingfilter");
            assert filterListingForPricing != null : "Filter for listing is null";

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListData(352, filterListingForPricing);
            String response = listRendererListData.getListDataJsonStr();

            JSONObject listingResponse = new JSONObject(response);
            JSONArray listArrayJson = listingResponse.getJSONArray("data");

            String pricingAvailableEntityId = null, pricingForReportEntityId = null;
            for (Object object : listArrayJson) {
                if (object instanceof JSONObject) {
                    if (((JSONObject) object).getJSONObject("19412").getString("value").equalsIgnoreCase("yes") && pricingAvailableEntityId == null)
                        pricingAvailableEntityId = ((JSONObject) object).getJSONObject("14219").getString("value").split(":;")[1];

                    if (((JSONObject) object).getJSONObject("19413").getString("value").equalsIgnoreCase("yes") && pricingForReportEntityId == null)
                        pricingForReportEntityId = ((JSONObject) object).getJSONObject("14219").getString("value").split(":;")[1];

                    if (((JSONObject) object).getJSONObject("19412").getString("value").equalsIgnoreCase("no")
                            && ((JSONObject) object).getJSONObject("19413").getString("value").equalsIgnoreCase("no")) {
                        customAssert.assertTrue(false, "List is not filtered as expected " + ((JSONObject) object).getJSONObject("14219").getString("value"));
                    }
                }
            }

            //point 1 checked

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "downloadfilepath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "downloadfilename");

            assert downloadFileName != null && downloadFilePath != null : "Download file path and name not found";
            if(!(pricingAvailableEntityId != null && pricingForReportEntityId != null))
                throw new SkipException("Data of entity not found for pricing download");

            FileUtils.deleteFile(downloadFilePath, downloadFileName);

            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(pricingAvailableEntityId));
            if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for only pricing available entity");
            } else {
                FileUtils.deleteFile(downloadFilePath + downloadFileName);
            }

            invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(pricingForReportEntityId));
            if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for pricing for report entity");
            } else {
                FileUtils.deleteFile(downloadFilePath + downloadFileName);
            }

            //point 2 checked

            invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Arrays.asList(pricingAvailableEntityId, pricingForReportEntityId));
            if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for both");
            } else {
                FileUtils.deleteFile(downloadFilePath + downloadFileName);
            }

            //point 3 checked
        }
        catch (SkipException se){}
        catch (Exception e) {
            customAssert.assertTrue(false, "Exception caught {}" + Arrays.toString(e.getStackTrace()));
            logger.error("Exception caught {}", (Object) e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    /*
    C63068 - verify the values in charges tab and ARC RRC tab
     */
    public void C63067() {

        CustomAssert customAssert = new CustomAssert();

        try {

            String flow = "vol pricing flow consumption unapproved";

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flow);
            if (contractId != -1) {

                logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flow);
                InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flow, contractId);

                int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flow, contractId);
                if (serviceDataId == -1) {
                    logger.info("Cannot create service data");
                    customAssert.assertTrue(false, "Cannot create service data with invoicing currency filled");
                }

                String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "downloadfilepath");
                String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "downloadfilename");

                assert downloadFileName != null && downloadFilePath != null : "Download file path and name not found";

                InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
                invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(serviceDataId)));
                if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                    customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for only pricing available entity");
                }

                String jsonDataFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C63067", "jsondatafilepath");
                String jsonDataFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C63067", "jsondatafilename");

                InputStream inputStream = new FileInputStream(jsonDataFilePath + jsonDataFileName);
                BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream));

                String line = buf.readLine();
                StringBuilder sb = new StringBuilder();

                while (line != null) {
                    sb.append(line).append("\n");
                    line = buf.readLine();
                }

                String jsonData = sb.toString();

                JSONObject jsonObject = new JSONObject(jsonData);


                //****************************** case 1**************************
                setValuesInExcel(jsonObject.getJSONArray("C63067").getJSONObject(0), downloadFilePath, downloadFileName);

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                String pricingUploadResponse = BulkTemplate.uploadPricingTemplate(downloadFilePath, downloadFileName, serviceDataTypeId, 1010);

                logger.info("Pricing upload response {}", pricingUploadResponse);

                if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                    String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                    if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                        //Wait for Pricing Scheduler to Complete
                        String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flow, allTaskIds);

                        if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                            logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flow);
                            customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                    flow + "]");

                            customAssert.assertAll();
                            return;
                        } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                            logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flow);

                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flow);
                            customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                    "Hence failing Flow [" + flow + "]");
                            customAssert.assertAll();
                            return;
                        } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                        {
                            boolean isDataCreatedUnderARCRRCTab = new InvoiceHelper().isARCRRCCreated(serviceDataId);

                            if (!isDataCreatedUnderARCRRCTab) {
                                customAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                        flow + "]");

                                customAssert.assertAll();
                                return;
                            }
                        }
                    }
                }

                //****************************** case 2**************************
                setValuesInExcel(jsonObject.getJSONArray("C63067").getJSONObject(1), downloadFilePath, downloadFileName);

                logger.info("Hitting Fetch API.");
                fetchObj.hitFetch();
                allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                pricingUploadResponse = BulkTemplate.uploadPricingTemplate(downloadFilePath, downloadFileName, serviceDataTypeId, 1010);

                logger.info("Pricing upload response {}", pricingUploadResponse);

                if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                    String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                    if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                        //Wait for Pricing Scheduler to Complete
                        String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flow, allTaskIds);

                        if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                            logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flow);
                            customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                    flow + "]");

                            customAssert.assertAll();
                            return;
                        } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                            logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flow);

                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flow);
                            customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                    "Hence failing Flow [" + flow + "]");
                            customAssert.assertAll();
                            return;
                        } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                        {


                            boolean isDataCreatedUnderChargesTab = new InvoiceHelper().verifyChargesCreated(serviceDataId, jsonObject.getJSONArray("C63067").getJSONObject(1).getJSONObject("Pricing").getJSONObject("6").getDouble("8052"), jsonObject.getJSONArray("C63067").getJSONObject(1).getJSONObject("Pricing").getJSONObject("6").getDouble("11348"));

                            if (!isDataCreatedUnderChargesTab) {
                                customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                        flow + "]");
                                customAssert.assertAll();
                                return;
                            }

                            boolean isDataCreatedUnderARCRRCTab = new InvoiceHelper().verifyARCRRCCreated(serviceDataId,
                                    jsonObject.getJSONArray("C63067").getJSONObject(1).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8061"),
                                    jsonObject.getJSONArray("C63067").getJSONObject(1).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8062"),
                                    jsonObject.getJSONArray("C63067").getJSONObject(1).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8063"));

                            if (!isDataCreatedUnderARCRRCTab) {
                                customAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                        flow + "]");

                                customAssert.assertAll();
                                return;
                            }
                        }
                    }
                }
                //****************************** case 3**************************
                setValuesInExcel(jsonObject.getJSONArray("C63067").getJSONObject(2), downloadFilePath, downloadFileName);

                logger.info("Hitting Fetch API.");
                fetchObj.hitFetch();
                allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                pricingUploadResponse = BulkTemplate.uploadPricingTemplate(downloadFilePath, downloadFileName, serviceDataTypeId, 1010);

                logger.info("Pricing upload response {}", pricingUploadResponse);

                if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                    String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                    if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                        //Wait for Pricing Scheduler to Complete
                        String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flow, allTaskIds);

                        if (pricingSchedulerStatus.trim().equalsIgnoreCase("true")) {
                            logger.error("Pricing Upload Task passed. But supposed to fail");
                            customAssert.assertTrue(false, "Pricing Upload Task passed. But supposed to fail");

                            customAssert.assertAll();
                            return;
                        } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                            logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flow);

                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flow);
                            customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                    "Hence failing Flow [" + flow + "]");
                            customAssert.assertAll();
                            return;
                        }

                        List<Integer> tempTasks = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

//                        String failureMessage = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"C63067","failuremessage");
//                        //SCPUtils extractBulkExcelFromSSHClient = new SCPUtils("172.60.7.143","tomcat7", Paths.get("C:/Users/srijan.samanta/Documents/sirion-vpc.ppk"),22);
//                        //extractBulkExcelFromSSHClient.downloadExcelFile(String.valueOf(tempTasks.get(0)),"temp.xlsm","src/test/output");
//
//                        String cellValue = XLSUtils.getOneCellValue("src/test/output","temp.xlsm","Data",6,11);
//                        logger.info("Cell value in the downloaded excel is {}",cellValue);

                    }
                }

                FileUtils.deleteFile(downloadFilePath + downloadFileName);
            }

        } catch (Exception e) {
            logger.error("Exception caught {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void C90575() {
        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "service data version change scenario";
        String flow = "vol pricing flow consumption unapproved";
        try {

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, flow);
            if (contractId == -1) {
                logger.info("Cannot create contract");
                customAssert.assertTrue(false, "Cannot create contract with invoicing currency filled");
            }

            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, flowToTest, "sourceid", String.valueOf(contractId));
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, "cdr creation for pricing with effective date", "sourceid", String.valueOf(contractId));

            //Validate CR Creation
            logger.info("Creating CR for Flow [{}]", flowToTest);

            String createResponse = ChangeRequest.createChangeRequest(changeRequestConfigFilePath, changeRequestConfigFileName, changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, flowToTest,
                    true);
            int crId = -1;
            String crToBeFilledInPricingSheet = null;
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    crId = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (crId != -1) {
                        logger.info("CR Created Successfully with Id {}: ", crId);
                        logger.info("Hitting Show API for CR Id {}", crId);
                        Show showObj = new Show();
                        showObj.hitShow(crEntityTypeId, crId);
                        String showResponse = showObj.getShowJsonStr();

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!showObj.isShowPageAccessible(showResponse)) {
                                customAssert.assertTrue(false, "Show Page is Not Accessible for CR Id " + crId);
                            }

                            crToBeFilledInPricingSheet = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                        } else {
                            customAssert.assertTrue(false, "Show API Response for CR Id " + crId + " is an Invalid JSON.");
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Couldn't create CR for Flow [" + flowToTest + "] due to " + createStatus);
                }
            } else {
                customAssert.assertTrue(false, "Create API Response for CR Creation Flow [" + flowToTest + "] is an Invalid JSON.");
            }

            ///*********************** create CDR ********************
//            createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath,cdrConfigFileName, cdrConfigFilePath, cdrExtraFieldsConfigFileName, "cdr creation for pricing with effective date",
//                    true);
//            int cdrId=-1;
//            String cdrToBeFilledInPricingSheet=null;
//            if (ParseJsonResponse.validJsonResponse(createResponse)) {
//                JSONObject jsonObj = new JSONObject(createResponse);
//                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
//                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);
//
//                if (createStatus.equalsIgnoreCase("success"))
//                    cdrId = CreateEntity.getNewEntityId(createResponse, "change requests");
//
//                if (createStatus.equalsIgnoreCase("success")) {
//                        if (cdrId != -1) {
//                            logger.info("CDR Created Successfully with Id {}: ", cdrId);
//                            logger.info("Hitting Show API for CDR Id {}", cdrId);
//                            Show showObj = new Show();
//                            showObj.hitShow(cdrEntityTypeId, cdrId);
//                            String showResponse = showObj.getShowJsonStr();
//
//                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
//                                if (!showObj.isShowPageAccessible(showResponse)) {
//                                    customAssert.assertTrue(false, "Show Page is Not Accessible for CDR Id " + cdrId);
//                                }
//
//                                cdrToBeFilledInPricingSheet = "("+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values")+") "+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
//                            } else {
//                                customAssert.assertTrue(false, "Show API Response for CDR Id " + cdrId + " is an Invalid JSON.");
//                            }
//                        }
//                    } else {
//                        customAssert.assertTrue(false, "Couldn't create CDR for Flow [" + flowToTest + "] due to " + createStatus);
//                    }
//            } else {
//                customAssert.assertTrue(false, "Create API Response for CDR Creation Flow [" + flowToTest + "] is an Invalid JSON.");
//            }
//
//            assert crToBeFilledInPricingSheet!=null&&cdrToBeFilledInPricingSheet!=null:"crToBeFilledInPricingSheet or cdrToBeFilledInPricingSheet is null";

            assert crToBeFilledInPricingSheet != null : "crToBeFilledInPricingSheet or cdrToBeFilledInPricingSheet is null";

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flow, contractId);

            int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flow, contractId);
            if (serviceDataId == -1) {
                logger.info("Cannot create service data");
                customAssert.assertTrue(false, "Cannot create service data with invoicing currency filled");
                customAssert.assertAll();
            }

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "downloadfilepath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "downloadfilename");

            assert downloadFileName != null && downloadFilePath != null : "Download file path and name not found";

            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(serviceDataId)));
            if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for only pricing available entity");
            }

            String jsonDataFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C63067", "jsondatafilepath");
            String jsonDataFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C63067", "jsondatafilename");

            InputStream inputStream = new FileInputStream(jsonDataFilePath + jsonDataFileName);
            BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream));

            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();

            while (line != null) {
                sb.append(line).append("\n");
                line = buf.readLine();
            }

            String jsonData = sb.toString();

            JSONObject jsonObject = new JSONObject(jsonData);


            //****************************** case 1 **************************
            setValuesInExcel(jsonObject.getJSONArray("C90575").getJSONObject(0), downloadFilePath, downloadFileName);
            XLSUtils.updateColumnValue(downloadFilePath, downloadFileName, "Data", 6, 9, crToBeFilledInPricingSheet);

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            String pricingUploadResponse = BulkTemplate.uploadPricingTemplate(downloadFilePath, downloadFileName, serviceDataTypeId, 1010);

            logger.info("Pricing upload response {}", pricingUploadResponse);

            if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
                String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                    //Wait for Pricing Scheduler to Complete
                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flow, allTaskIds);

                    if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                        logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flow);
                        customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                flow + "]");

                        customAssert.assertAll();
                        return;
                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                        logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flow);

                        logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flow);
                        customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                "Hence failing Flow [" + flow + "]");
                        customAssert.assertAll();
                        return;
                    } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                    {


                        boolean isDataCreatedUnderChargesTab = new InvoiceHelper().verifyChargesCreated(serviceDataId,
                                jsonObject.getJSONArray("C90575").getJSONObject(0).getJSONObject("Pricing").getJSONObject("6").getDouble("8052"),
                                jsonObject.getJSONArray("C90575").getJSONObject(0).getJSONObject("Pricing").getJSONObject("6").getDouble("11348"));

                        if (!isDataCreatedUnderChargesTab) {
                            customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                    flow + "]");
                            customAssert.assertAll();
                            return;
                        }

                        boolean isDataCreatedUnderARCRRCTab = new InvoiceHelper().verifyARCRRCCreated(serviceDataId,
                                jsonObject.getJSONArray("C90575").getJSONObject(0).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8061"),
                                jsonObject.getJSONArray("C90575").getJSONObject(0).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8062"),
                                jsonObject.getJSONArray("C90575").getJSONObject(0).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8063"));

                        if (!isDataCreatedUnderARCRRCTab) {
                            customAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                    flow + "]");

                            customAssert.assertAll();
                            return;
                        }
                    }
                }
            }

            //****************************** case 2 **************************
//            setValuesInExcel(jsonObject.getJSONArray("C90575").getJSONObject(1), downloadFilePath, downloadFileName);
//            XLSUtils.updateColumnValue(downloadFilePath,downloadFileName,"Data",6,9,cdrToBeFilledInPricingSheet);
//
//            logger.info("Hitting Fetch API.");
//            fetchObj = new Fetch();
//            fetchObj.hitFetch();
//            allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
//
//            pricingUploadResponse = BulkTemplate.uploadPricingTemplate(downloadFilePath, downloadFileName, serviceDataTypeId, 1010);
//
//            logger.info("Pricing upload response {}", pricingUploadResponse);
//
//            if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("200:;")) {
//                String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
//                if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
//                    //Wait for Pricing Scheduler to Complete
//                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flow, allTaskIds);
//
//                    if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
//                        logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flow);
//                        customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
//                                flow + "]");
//
//                        customAssert.assertAll();
//                        return;
//                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
//                        logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flow);
//
//                        logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flow);
//                        customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
//                                "Hence failing Flow [" + flow + "]");
//                        customAssert.assertAll();
//                        return;
//                    } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
//                    {
//
//
//                        boolean isDataCreatedUnderChargesTab = new InvoiceHelper().verifyChargesCreated(serviceDataId,
//                                jsonObject.getJSONArray("C90575").getJSONObject(1).getJSONObject("Pricing").getJSONObject("6").getDouble("8052"),
//                                jsonObject.getJSONArray("C90575").getJSONObject(1).getJSONObject("Pricing").getJSONObject("6").getDouble("11348"));
//
//                        if (!isDataCreatedUnderChargesTab) {
//                            customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
//                                    flow + "]");
//                            customAssert.assertAll();
//                            return;
//                        }
//
//                        boolean isDataCreatedUnderARCRRCTab = new InvoiceHelper().verifyARCRRCCreated(serviceDataId,
//                                jsonObject.getJSONArray("C90575").getJSONObject(1).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8061"),
//                                jsonObject.getJSONArray("C90575").getJSONObject(1).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8062"),
//                                jsonObject.getJSONArray("C90575").getJSONObject(1).getJSONObject("ARCRRC").getJSONObject("6").getDouble("8063"));
//
//                        if (!isDataCreatedUnderARCRRCTab) {
//                            customAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
//                                    flow + "]");
//
//                            customAssert.assertAll();
//                            return;
//                        }
//                    }
//                }
//            }


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception occurred " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void C91007() {
        CustomAssert customAssert = new CustomAssert();

        try {

            String flowToTest = "fixed fee flow 1 for pricing for report true";
            String errorMessage = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C91007", "errormessage");
            assert errorMessage != null : "error message is null";

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, "fixed fee flow 1");
            if (contractId == -1) {
                logger.info("Cannot create contract");
                customAssert.assertTrue(false, "Cannot create contract with invoicing currency filled");
            }

            logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flowToTest, contractId);

            String createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, true);
            if (!createResponse.contains(errorMessage)) {
                customAssert.assertTrue(false, "Service data error message not fond as expected. Response : " + createResponse);
                customAssert.assertAll();
            }

            String pricingAvailablePayload = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "pricingAvailable");
            String pricingForReporting = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "pricingForReporting");

            assert pricingAvailablePayload != null && pricingForReporting != null : "pricingAvailable or pricingAvailablePayload is found null";

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "pricingAvailable", new JSONObject(pricingAvailablePayload).put("values", false).toString());

            int serviceDataId = -1;
            serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId);
            if (serviceDataId == -1) {
                logger.info("Cannot create service data");
                customAssert.assertTrue(false, "Cannot create service data with pricing available payload filled");
            }

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "pricingAvailable", new JSONObject(pricingAvailablePayload).put("values", true).toString());
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "pricingForReporting", new JSONObject(pricingForReporting).put("values", false).toString());
            serviceDataId = InvoiceHelper.getServiceDataIdForDifferentClientAndSupplierId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, contractId);
            if (serviceDataId == -1) {
                logger.info("Cannot create service data");
                customAssert.assertTrue(false, "Cannot create service data with pricing for report payload filled");
            }


        } catch (Exception e) {
            logger.error("Exception caught {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void C44165() {

        CustomAssert customAssert = new CustomAssert();
        try {

            String flowToTest = "fixed fee flow 1 for hierarchy";

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, "fixed fee flow 1");
            if (contractId == -1) {
                logger.info("Cannot create contract");
                customAssert.assertTrue(false, "Cannot create contract with invoicing currency filled");
            }

            logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, "", "", flowToTest, contractId);


            String pricingReportPayload = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flowToTest,"pricingForReporting");
            String parentServicePayload = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flowToTest,"parentService");

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flowToTest,"pricingForReporting",new JSONObject(pricingReportPayload).put("values",false).toString());
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flowToTest,"parentService",new JSONObject(parentServicePayload).put("values",JSONObject.NULL).toString());

            int serviceDataId = -1;

            String serviceDataEntity = "service data";
            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "serviceIdClient",
                    "new client", "newClient" + contractId);
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "serviceIdSupplier",
                    "new supplier", "newSupplier" + contractId);

            boolean createLocalServiceData = true;

            String createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                    serviceDataExtraFieldsConfigFileName, flowToTest, createLocalServiceData);

            serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "serviceIdClient",
                    "newClient" + contractId, "new client");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "serviceIdSupplier",
                    "newSupplier" + contractId, "new supplier");

            if (serviceDataId == -1) {
                logger.info("Cannot create service data");
                customAssert.assertTrue(false, "Cannot create service data with pricing available payload filled");
            }

            //**************************** child service data ****************
            int newServiceDataId = -1;
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flowToTest,"parentService", parentServicePayload.replace("parent_service_client_id","newClient"+contractId));
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flowToTest,"pricingForReporting",new JSONObject(pricingReportPayload).put("values",true).toString());
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flowToTest,"pricingAvailable",new JSONObject(parentServicePayload).put("values",false).toString());

            //Update Service Data Extra Fields.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "serviceIdClient",
                    "new client", "newClient" + contractId+"1");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "serviceIdSupplier",
                    "new supplier", "newSupplier" + contractId+"1");

            createLocalServiceData = true;

            createResponse = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
                    serviceDataExtraFieldsConfigFileName, flowToTest, createLocalServiceData);

            newServiceDataId = CreateEntity.getNewEntityId(createResponse, serviceDataEntity);

            //Revert Service Data Extra Fields changes.
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "serviceIdClient",
                    "newClient" + contractId+"1", "new client");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, flowToTest, "serviceIdSupplier",
                    "newSupplier" + contractId+"1", "new supplier");

            if (newServiceDataId == -1) {
                logger.info("Cannot create service data");
                customAssert.assertTrue(false, "Cannot create service data");
            }



        } catch (Exception e) {
            logger.error("Exception caught {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();

    }

    /*
    C129153 - also covered
     */
    @Test(enabled = true)
    public void C10394(){
        CustomAssert customAssert = new CustomAssert();

        String pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "downloadfilepath");
        String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "C3509", "downloadfilename");


        try{
            String flowToTest = "arc flow 1";

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

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());
            WorkflowActionsHelper workflowActionsHelper= new WorkflowActionsHelper();

            InvoicePricingHelper pricingObj = new InvoicePricingHelper();

                Boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(configFilePath,configFileName,pricingTemplateFilePath,pricingTemplateFileName, flowToTest, serviceDataId, pricingObj,customAssert);

                if (pricingFile) {

                        pricingFile = InvoiceHelper.editPricingFileForARCRRC(configFilePath,configFileName,pricingTemplateFilePath,pricingTemplateFileName, flowToTest, serviceDataId);

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


                                    boolean isDataCreatedUnderChargesTab =new  InvoiceHelper().isChargesCreated(serviceDataId);

                                    if (!isDataCreatedUnderChargesTab) {
                                        customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                flowToTest + "]");
                                        customAssert.assertAll();
                                        return;
                                    }

                                        boolean isDataCreatedUnderARCRRCTab = new InvoiceHelper().isARCRRCCreated(serviceDataId);

                                        if (!isDataCreatedUnderARCRRCTab) {
                                            customAssert.assertTrue(isDataCreatedUnderARCRRCTab, "no data in ARR/RRC tab is getting created. Hence skipping further validation for Flow [" +
                                                    flowToTest + "]");
                                            customAssert.assertAll();
                                            return;
                                        }
                                }
                                    boolean result = workflowActionsHelper.performWorkflowAction(serviceDataTypeId, serviceDataId, "publish");
                                    // if service data got published

                                    if (result) {
                                        ArrayList<Integer> consumptionIds = new ArrayList<>();

                                        String consumptionCreatedStatus =new InvoiceHelper().waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);
                                        logger.info("consumptionCreatedStatus is [{}]", consumptionCreatedStatus);

                                        if (consumptionCreatedStatus.trim().equalsIgnoreCase("fail")) {
                                            logger.error("Consumption Creation Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                            customAssert.assertAll();
                                            return;
                                        } else if (consumptionCreatedStatus.trim().equalsIgnoreCase("skip")) {
                                            logger.error("Consumption Creation Task didn't complete in specified time or there is not consumption tab in show page of service Data. Hence skipping further validation for Flow [{}]", flowToTest);
                                            customAssert.assertAll();
                                            throw new SkipException("Skipping this test");
                                        }

                                        Show show = new Show();
                                        show.hitShow(64,serviceDataId);
                                        String showResponse = show.getShowJsonStr();

                                        JSONObject jsonObject = new JSONObject(showResponse);
                                        jsonObject.remove("header");
                                        jsonObject.remove("session");
                                        jsonObject.remove("actions");
                                        jsonObject.remove("createLinks");
                                        jsonObject.getJSONObject("body").remove("layoutInfo");
                                        jsonObject.getJSONObject("body").remove("globalData");
                                        jsonObject.getJSONObject("body").remove("errors");

                                        String newName = "new name test con";
                                        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values",newName);

                                        Edit edit = new Edit();
                                        String editResponse = edit.hitEdit("service data",jsonObject.toString());

                                        if(!editResponse.contains("success")){
                                            customAssert.assertTrue(false,"edit action not performed successfully");
                                            customAssert.assertAll();
                                        }

                                        //checking audit log

                                        TabListData tabListData = new TabListData();
                                        String tabResponse = tabListData.hitTabListData(61,64,serviceDataId);
                                        JSONObject jsonObject1 = new JSONObject(tabResponse);
                                        String historyUrl = jsonObject1.getJSONArray("data").getJSONObject(jsonObject1.getJSONArray("data").length()-1).getJSONObject("5094").getString("value");

                                        long historyId = Long.parseLong(historyUrl.substring(1).split("/")[2]);

                                        FieldHistory fieldHistory = new FieldHistory();
                                        String historyResponse = fieldHistory.hitFieldHistory(historyId,64);

                                        JSONObject jsonObject2 = new JSONObject(historyResponse);
                                        String newValue = jsonObject2.getJSONArray("value").getJSONObject(0).getString("newValue");

                                        customAssert.assertTrue(newValue.equalsIgnoreCase(newName),"Audit log values not matching");

                                        show = new Show();
                                        show.hitShow(176,consumptionIds.get(0));
                                        showResponse = show.getShowJsonStr();

                                        jsonObject = new JSONObject(showResponse);

                                        customAssert.assertTrue(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values").equalsIgnoreCase(newName),"Changed name is not found in the consumption");


                                        // after consumptions have been created successfully
//                                        logger.info("Consumption Ids are : [{}]", consumptionIds);
//                                        String[] finalConsumptions = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
//                                                "finalconsumptionvalues").trim().split(Pattern.quote(","));
//                                        for (int i = 0; i < consumptionIds.size(); i++) {
//                                                result = InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), Double.parseDouble(finalConsumptions[i]));
//                                                if (!result) {
//                                                    logger.error("Couldn't update the final consumption for consumption Id [{}] for Flow [{}] Hence skipping validation.", consumptionIds.get(i), flowToTest);
//                                                    customAssert.assertTrue(false, "Couldn't update the final consumption for Flow [" + flowToTest + "]. " +
//                                                            "Hence skipping validation");
//                                                    return;
//
//                                                } else {
//                                                    result = workflowActionsHelper.performWorkflowAction(176, consumptionIds.get(i), "approve");
//                                                        customAssert.assertTrue(result, "Not Being able to Perform [approve] Action on consumptions having id : " + consumptionIds.get(i));
//
//                                                        if (!result) {
//                                                            logger.error("Couldn't approve the Consumption data after updating the final consumption for Flow [{}]. Hence skipping validation.", flowToTest);
//                                                            customAssert.assertTrue(false, "Couldn't approve the Consumption data after updating the final consumption for Flow [" + flowToTest + "]. " +
//                                                                    "Hence skipping validation");
//                                                            customAssert.assertAll();
//                                                            return;
//                                                        }
//                                                }
//
//                                            }

                                    }
                                    else {

                                        logger.error("Couldn't publish the service data after uploading the pricing for Flow [{}]. Hence skipping validation.", flowToTest);
                                        customAssert.assertTrue(false, "Couldn't publish the service data after uploading the pricing for Flow [" + flowToTest + "]. " +
                                                "Hence skipping validation");
                                        customAssert.assertAll();
                                        return;


                                    }
                        } else {
                            logger.error("Pricing Upload failed for Invoice Flow Validation using File [{}] and Flow [{}]. Hence skipping further validation",
                                    pricingTemplateFilePath + "/" + pricingTemplateFileName, flowToTest);
                            customAssert.assertTrue(false, "Pricing Upload failed for Invoice Flow Validation using File [" +
                                    pricingTemplateFilePath + "/" + pricingTemplateFileName + "] and Flow [" + flowToTest + "]. Hence skipping further validation");
                            customAssert.assertAll();
                            return;
                        }
                    } else {
                        logger.error("Couldn't get Updated ARC/RRC Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
                        customAssert.assertTrue(false, "Couldn't get Updated ARC/RRC Template Sheet for Flow [" + flowToTest + "]. " +
                                "Hence skipping validation");
                        customAssert.assertAll();

                        return;

                    }

                } else {
                    logger.error("Couldn't get Updated Pricing Template Sheet for Flow [{}]. Hence skipping validation.", flowToTest);
                    customAssert.assertTrue(false, "Couldn't get Updated Pricing Template Sheet for Flow [" + flowToTest + "]. " +
                            "Hence skipping validation");
                    customAssert.assertAll();
                    return;
                }

        }
        catch (Exception e) {
            logger.error("Exception caught {}", (Object) e.getStackTrace());
            customAssert.assertTrue(false, "Exception caught " + Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();
    }

    private void setValuesInExcel(JSONObject jsonObject, String downloadFilePath, String downloadFileName) {

        Set<String> set = jsonObject.keySet();

        for (String s : set) {
            JSONObject jsonTemp1 = jsonObject.getJSONObject(s);

            Set<String> set2 = jsonTemp1.keySet();

            for (String s2 : set2) {
                JSONObject jsonTemp2 = jsonTemp1.getJSONObject(s2);
                Map<String, Object> map = new HashMap<>();

                Set<String> set3 = jsonTemp2.keySet();

                for (String s3 : set3) {

                    if (NumberUtils.isParsable(jsonTemp2.getString(s3))) {
                        map.put(s3, Double.parseDouble(jsonTemp2.getString(s3)));
                    } else {
                        map.put(s3, jsonTemp2.getString(s3));
                    }

                }

                XLSUtils.editRowDataUsingColumnId(downloadFilePath, downloadFileName, s, Integer.parseInt(s2), map);
            }
        }

    }

}
