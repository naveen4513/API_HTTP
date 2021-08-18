package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.PurchaseOrder;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;

public class TestPOLinkDuringLineItemCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestPOLinkDuringLineItemCreation.class);

    private static String configFilePath;
    private static String configFileName;

    private static String poCreationFilePath;
    private static String poCreationFileName;

    private String poExtraFieldsConfigFilePath;
    private String poExtraFieldsConfigFileName;

    private int poEntityTypeId;

    private static String sdCreationFilePath;
    private static String sdCreationFileName;

    private String sdExtraFieldsConfigFilePath;
    private String sdExtraFieldsConfigFileName;

    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;
    private String invoiceExtraFieldsConfigFileName;

    private String invoiceLineItemConfigFilePath;
    private String invoiceLineItemConfigFileName;
    private String invoiceLineItemExtraFieldsConfigFileName;

    private int sdEntityTypeId;
    private int invoiceEntityTypeId;
    private int lineItemEntityTypeId;

    private String serviceData = "service data";
    private String invoice = "invoices";
    private String invoiceLineItem = "invoice line item";

    private int invoiceListId = 10;
    private int invoiceLineItemListId = 358;

    private String clientId;
    private String bulkCreateConfigFilePath;
    private String bulkCreateConfigFileName;

    private String poFieldId;
    String bulkCreateExcelFilePath;

    String lineItem_ColId_OnLineItemListing;
    String po_ColId_OnLineItemListing;
    String po_ColName_OnLineItemListing;
    String invoice_ColId_OnInvoiceListing;
    String po_ColId_OnInvoiceListing;
    String po_ColName_OnInvoiceListing;

    @BeforeClass
    public void beforeClass(){

        poCreationFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFilePath");
        poCreationFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFileName");
        poExtraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFilePath");
        poExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderExtraFieldsFileName");

        poEntityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");
        sdEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
        invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
        lineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");

        sdCreationFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataFilePath");
        sdCreationFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataFileName");
        sdExtraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataFilePath");
        sdExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");


        //Invoice Config files
        invoiceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
        invoiceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");
        invoiceExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceExtraFieldsFileName");

        //Invoice Line Item Config files
        invoiceLineItemConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFilePath");
        invoiceLineItemConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFileName");
        invoiceLineItemExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("invoiceLineItemExtraFieldsFileName");

        clientId = ConfigureEnvironment.getEnvironmentProperty("client_id");

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("POLinkSDConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("POLinkSDConfigFileName");

        bulkCreateConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateConfigFilePath");
        bulkCreateConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkCreateConfigFileName");

        poFieldId = "12807";

        lineItem_ColId_OnLineItemListing = "13882";
        po_ColId_OnLineItemListing = "13925";
        po_ColName_OnLineItemListing = "poNumber";
        invoice_ColId_OnInvoiceListing = "203";
        po_ColId_OnInvoiceListing = "17221";
        po_ColName_OnInvoiceListing = "purchaseorders";

    }

    @Test
    public void Test_POLinkDuringLICreation() {

        CustomAssert customAssert = new CustomAssert();

        String poFlow = "po sd link flow";
        String serviceDataFlow = "po sd link flow arc flow create";
        String invoiceSection = "po link invoice";
        String invoiceLineItemSectionName = "po link invoice line item";

        int purchaseOrderId = -1;
        int numberOfPOToCreate = 3;
        ArrayList<Integer> expectedPoList = new ArrayList<>();

        try {

            String uniqueString = DateUtils.getCurrentTimeStamp();
            for(int i =1;i<=numberOfPOToCreate;i++) {

                uniqueString = DateUtils.getCurrentTimeStamp();
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name", "randomString", uniqueString);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber", "randomString", uniqueString);
                String createResponse = PurchaseOrder.createPurchaseOrder(poCreationFilePath, poCreationFileName, poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow,
                        true);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name",  uniqueString,"randomString");
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber",  uniqueString,"randomString");

                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    JSONObject jsonObj = new JSONObject(createResponse);
                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

                        if (purchaseOrderId == -1) {

                            customAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + poFlow + "] due to " + createStatus);
                            customAssert.assertAll();
                            return;
                        }else {
                            expectedPoList.add(purchaseOrderId);
                        }

                    } else {
                        customAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + poFlow + "] is an Invalid JSON.");
                    }
                }
            }
            String poString = "";

            for(Integer poId : expectedPoList) {
                poString += "{\"name\":\"purchase order test\",\"id\":" + poId + ",\"entityTypeId\":181,\"clientId\":" + clientId + ",\"description\":\"po number\",\"multiplier\":1,\"entityFinancialData\":{\"id\":null}}" + ",";
            }

            poString = poString.substring(0,poString.length() - 1);
            UpdateFile.updateConfigFileProperty(sdExtraFieldsConfigFilePath, sdExtraFieldsConfigFileName, serviceDataFlow, "purchaseOrder", "poString", poString);
            UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, invoiceSection, "purchaseOrder", "poString", poString);
            UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, "pos", "poString", poString);

            int serviceDataId = InvoiceHelper.getServiceDataId(sdCreationFilePath, sdCreationFileName, sdExtraFieldsConfigFileName, serviceDataFlow, uniqueString);
            UpdateFile.updateConfigFileProperty(sdExtraFieldsConfigFilePath, sdExtraFieldsConfigFileName, serviceDataFlow, "purchaseOrder",  poString,"poString");

            int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName,
                    invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceSection);

            UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, invoiceSection, "purchaseOrder",  poString,"poString");

            validate_POIdsOnShowPage(invoice, invoiceEntityTypeId, invoiceId, expectedPoList, customAssert);

            validate_POIdsOnListPage(invoice, invoiceListId, invoiceEntityTypeId, po_ColId_OnInvoiceListing,po_ColName_OnInvoiceListing, invoice_ColId_OnInvoiceListing, expectedPoList, customAssert);

            int invoiceLineItemId = -1;

            logger.info("Created Invoice Id is : [{}]", invoiceId);
            if (invoiceId != -1) {
                //Get Invoice Line Item Id
                invoiceLineItemId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath,invoiceLineItemConfigFileName,invoiceLineItemExtraFieldsConfigFileName,invoiceLineItemSectionName,serviceDataId);
                logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);
                UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, "pos",  poString,"poString");

                validate_POIdsOnShowPage(invoiceLineItem,lineItemEntityTypeId,invoiceLineItemId,expectedPoList,customAssert);

                validate_POIdsOnListPage(invoiceLineItem, invoiceLineItemListId, lineItemEntityTypeId, po_ColId_OnLineItemListing,po_ColName_OnLineItemListing, lineItem_ColId_OnLineItemListing, expectedPoList, customAssert);

            } else {
                logger.error("Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [{}]", invoiceSection);
                customAssert.assertTrue(false, "Couldn't get Invoice Id for Invoice Flow Validation. Hence skipping Flow [" + invoiceSection + "]");
            }

        } catch (Exception e) {
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false, "Exception while validating the scenario " + e.getStackTrace());
        }
        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void test_POLinkDuringBulkCreateSD() {
        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "service data po link";
        String poFlow = "po sd link flow";

        int numberOfPOToCreate = 1;
        int purchaseOrderId = -1;

        ArrayList<Integer> expectedPoList = new ArrayList<>();

        BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

        try {

            String uniqueString = DateUtils.getCurrentTimeStamp();
            for(int i =1;i<=numberOfPOToCreate;i++) {

                uniqueString = DateUtils.getCurrentTimeStamp();
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name", "randomString", uniqueString);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber", "randomString", uniqueString);
                String createResponse = PurchaseOrder.createPurchaseOrder(poCreationFilePath, poCreationFileName, poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow,
                        true);
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "name",  uniqueString,"randomString");
                UpdateFile.updateConfigFileProperty(poExtraFieldsConfigFilePath, poExtraFieldsConfigFileName, poFlow, "poNumber",  uniqueString,"randomString");

                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    JSONObject jsonObj = new JSONObject(createResponse);
                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

                        if (purchaseOrderId == -1) {

                            customAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + poFlow + "] due to " + createStatus);
                            customAssert.assertAll();
                            return;
                        }else {
                            expectedPoList.add(purchaseOrderId);
                        }

                    } else {
                        customAssert.assertTrue(false, "Create API Response for Purchase Order Creation Flow [" + poFlow + "] is an Invalid JSON.");
                    }
                }
            }

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

            if (flowProperties.isEmpty()) {
                throw new SkipException("Couldn't get All Properties of Flow " + flowToTest);
            }

            Map<String,String> defaultProperties = ParseConfigFile.getAllDefaultProperties(bulkCreateConfigFilePath, bulkCreateConfigFileName);

            bulkCreateExcelFilePath = defaultProperties.get("excelfilepath");
            Long schedulerJobTimeOut = Long.parseLong(defaultProperties.get("schedulerwaittimeout"));
            Long schedulerJobPollingTime = Long.parseLong(defaultProperties.get("schedulerpollingtime"));

            logger.info("Verifying Bulk Create for Flow: [{}]", flowToTest);

            String testTemplateName = flowToTest + ".xlsm";

            int parentEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
                    "parentEntityTypeId"));
            int parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "parentId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "templateId"));

            BulkTemplate.downloadBulkCreateTemplate(bulkCreateExcelFilePath, testTemplateName, templateId,  parentEntityTypeId, parentId);
            String sheetName = flowProperties.get("sheet name");


            Map<String,String> dataMapForExcelFile = ParseConfigFile.getAllConstantProperties(configFilePath,configFileName,"fields to update");
            List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));

            Map<String, Object> dataMap= new HashMap<>();;

            for (Map.Entry<String, String> entry : dataMapForExcelFile.entrySet()) {
                if (NumberUtils.isParsable(entry.getValue())) {
                    if (dateColumnIds.contains(entry.getKey())) {
                        dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
                    } else
                        dataMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                } else {
                    if (dateColumnIds.contains(entry.getKey())) {
                        SimpleDateFormat date = new SimpleDateFormat(entry.getValue());
                        dataMap.put(entry.getKey(), date);
                    }else {
                        dataMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            String pONameValueInExcel = "";
            for(Integer poId : expectedPoList){
                pONameValueInExcel += ShowHelper.getValueOfField(poEntityTypeId,poId,"name") + ";";
            }
            pONameValueInExcel = pONameValueInExcel.substring(0,pONameValueInExcel.length() -1);

            dataMap.put("12807",pONameValueInExcel);

            XLSUtils.editRowDataUsingColumnId(bulkCreateExcelFilePath,testTemplateName,sheetName,6,dataMap);

            String entityName = flowProperties.get("entity");
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
            UserTasksHelper.removeAllTasks();

            logger.info("Hitting Fetch API.");
            Fetch fetchObj = new Fetch();
            fetchObj.hitFetch();
            List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

            //Upload Bulk Create Template
            String bulkCreateUploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateExcelFilePath, testTemplateName, parentEntityTypeId, parentId,
                    entityTypeId, templateId);

            if (bulkCreateUploadResponse.contains("Your request has been submitted")) {
                logger.info("Hitting Fetch API to Get Bulk Create Job Task Id");
                fetchObj.hitFetch();
                logger.info("Getting Task Id of Bulk Create Job");
                int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

                Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("skip")) {
                    throw new SkipException("Bulk Create Scheduler Job didn't finish and hence cannot validate further.");
                }

                if (schedulerJob.get("jobPassed").equalsIgnoreCase("true")) {
                    int newlyCreatedServiceDataId = bulkHelperObj.getNewlyCreatedRecordIdFromBulkCreateFileName(testTemplateName);

                    if (newlyCreatedServiceDataId == -1) {
                        throw new SkipException("Couldn't get Newly Created Record Id from Bulk Template.");
                    }else {



                    }


                } else {
                    String errorMessage = bulkHelperObj.getErrorMessagesForBulkCreateFileName(testTemplateName);
                    customAssert.assertTrue(false, "Bulk Create Scheduler Job Failed whereas it was supposed to Pass. Error Message: " + errorMessage);
                }
            } else {
                customAssert.assertTrue(false, "Template Upload Failed. Expected Response: " +
                        "[Your request has been submitted] and Actual Response: [" + bulkCreateUploadResponse + "]");
            }

            FileUtils.deleteFile(bulkCreateExcelFilePath + "/" + testTemplateName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Verifying BulkCreate Flow [" + flowToTest + "]. " + e.getMessage());
        }

        customAssert.assertAll();
    }

    private HashMap<String,String> getPoIds(int entityTypeId,int entityId, CustomAssert customAssert){

        HashMap<String,String> poIds = new HashMap<>();
        try{

            Show show = new Show();
            show.hitShowVersion2(entityTypeId,entityId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJson = new JSONObject(showResponse);
            JSONArray valuesArray = new JSONArray();

            if(entityTypeId == 64 || entityTypeId == 67){
                valuesArray = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("purchaseOrder").getJSONArray("values");
            }else if(entityTypeId == 165){
                valuesArray = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("pos").getJSONArray("values");
            }


            for(int i = 0;i<valuesArray.length();i++){

                String name = valuesArray.getJSONObject(i).get("name").toString();
                String id = valuesArray.getJSONObject(i).get("id").toString();

                poIds.put(id,name);
            }

        }catch (Exception e){
            logger.error("Exception while getting Po Id from service Data");
            customAssert.assertTrue(false,"Exception while getting Po Id from service Data " + e.getStackTrace());
        }
        return poIds;
    }

    private void validate_POIdsOnShowPage(String entityName,int entityTypeId,int entityId,ArrayList<Integer> expectedPoList,CustomAssert customAssert){
        try {
            HashMap<String, String> poIdsONShowPage = getPoIds(entityTypeId, entityId, customAssert);
            if (poIdsONShowPage.size() == 0) {
                logger.error("On " + entityName + " Show Page Number of purchase order ids is zero");
                customAssert.assertTrue(false, "On " + entityName + " Show Page Number of purchase order ids is zero");
            } else {

                if (poIdsONShowPage.size() != expectedPoList.size()) {
                    customAssert.assertTrue(false, "Expected number of Po Ids on " + entityName + " Show page not equal to actual number of PoIds Expected number : " + expectedPoList.size() + " Actual number : " + poIdsONShowPage.size());
                } else {

                    for (Integer poId : expectedPoList) {

                        if (!poIdsONShowPage.containsKey(poId.toString())) {
                            customAssert.assertTrue(false, "PO ID " + poId + " not present on purchase order show page");
                        } else {
                            String actualPoName = ShowHelper.getValueOfField(poEntityTypeId, poId, "name");
                            String poNameOnSDShowPage = poIdsONShowPage.get(poId.toString());
                            if (!actualPoName.equals(poNameOnSDShowPage)) {
                                logger.error("PO Name on " + entityName + " not matched with actual PO Name Expected : " + actualPoName + " Actual : " + poNameOnSDShowPage);
                                customAssert.assertTrue(false, "PO Name on " + entityName + " not matched with actual PO Name Expected : " + actualPoName + " Actual : " + poNameOnSDShowPage);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating POIds On ShowPage for entity " + entityName);
            customAssert.assertTrue(false,"Exception while validating POIds On ShowPage for entity " + entityName + e.getStackTrace());
        }
    }

    public void validate_POIdsOnListPage(String entityName,int listId,int entityTypeId,
                                         String purchaseOrderColumnId,String purchaseOrderColName,String entityColumnId,
                                         ArrayList<Integer> expectedPoIdList,CustomAssert customAssert){

        try{
            int poId = expectedPoIdList.get(0);

            String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                    "\"filterJson\":{\"253\":{\"multiselectValues\":" +
                    "{\"SELECTEDDATA\":[{\"id\":\"" + poId + "\",\"name\":\"PO \"}]}," +
                    "\"filterId\":253,\"filterName\":\"poNumber\"}}}," +
                    "\"selectedColumns\":[" +
                    "{\"columnId\":" + entityColumnId + ",\"columnQueryName\":\"id\"}," +
                    "{\"columnId\":" + purchaseOrderColumnId + ",\"columnQueryName\":\"" + purchaseOrderColName + "\"}]}";

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(listId,payload);
            String listDataResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listDataResponse)){
                logger.error("Listing Response is invalid json when checking PO on " + entityName + " Listing page");
                customAssert.assertTrue(false,"Listing Response is invalid json when checking PO on " + entityName  + " Listing page");
            }else {
                Map<Integer, Map<String, String>> listMap = listRendererListData.getListColumnIdValueMap(listDataResponse);

                if(listMap.size() == 0){
                    logger.error("While validating PO Id On " + entityName + " List Page after Applying Purchase Order Filter Listing page has zero records");
                    customAssert.assertTrue(false,"While validating PO Id On " + entityName + " List Page after Applying Purchase Order Filter Listing page has zero records");
                }else {
                    for (Map.Entry<Integer,Map<String, String>> listRowData : listMap.entrySet()){

                        Map<String, String> listColValues = listRowData.getValue();

                        String poValuesOnListingPage =  listColValues.get(purchaseOrderColumnId);

                        for(Integer poid : expectedPoIdList){
                            String actualPoName= ShowHelper.getValueOfField(poEntityTypeId,poId,"name");
                            if(!poValuesOnListingPage.contains(actualPoName)){
                                logger.error("Purchase order id " + poid + " not found on Purchase Order Column on " + entityName  + " Listing page for entity id " + listColValues.get(entityColumnId));
                                customAssert.assertTrue(false,"Purchase order id " + poid + " not found on Purchase Order Column on " + entityName  + " Listing page for service data id " + listColValues.get(entityColumnId));
                            }
                        }
                    }
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating PO Id On " + entityName  + " List Page " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating PO Id On " + entityName + " List Page " + e.getStackTrace());
        }
    }


}
