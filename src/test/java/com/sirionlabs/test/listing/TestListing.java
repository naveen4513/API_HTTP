package com.sirionlabs.test.listing;

import com.mongodb.util.JSON;
import com.sirionlabs.api.clientAdmin.customField.CustomField;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.customField.CustomFieldHelper;
import com.sirionlabs.helper.dbHelper.WorkflowButtonsDbHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.*;
import net.minidev.json.JSONUtil;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class TestListing {
    private final static Logger logger = LoggerFactory.getLogger(TestListing.class);
    private String configFilePath;
    private String configFileName;
    private String customFieldSectionName = "custom field name";
    private Map<Integer, String> deleteEntityMap = new HashMap<>();
    private String flowsConfigFilePath;
    private String flowsConfigFileName;
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
    private int serviceDataEntityTypeId = -1;
    private String serviceDataEntitySectionUrlName;
    private String consumptionEntitySectionUrlName;
    private boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private String contractEntity = "contracts";
    private String serviceDataEntity = "service data";

    @AfterMethod
    public void afterMethodForDelete() {
        for (Map.Entry<Integer, String> me : deleteEntityMap.entrySet()) {
            deleteNewEntity(me.getValue(), me.getKey());
        }
    }

    private void deleteNewEntity(String entityName, int entityId) {

        try {
            logger.info("Hitting Show API for Entity {} having Id {}.", entityName, entityId);
            Show showObj = new Show();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            showObj.hitShow(entityTypeId, entityId);
            if (ParseJsonResponse.validJsonResponse(showObj.getShowJsonStr())) {
                JSONObject jsonObj = new JSONObject(showObj.getShowJsonStr());
                String prefix = "{\"body\":{\"data\":";
                String suffix = "}}";
                String showBodyStr = jsonObj.getJSONObject("body").getJSONObject("data").toString();
                String deletePayload = prefix + showBodyStr + suffix;

                logger.info("Deleting Entity {} having Id {}.", entityName, entityId);
                Delete deleteObj = new Delete();
                deleteObj.hitDelete(entityName, deletePayload);
                String deleteJsonStr = deleteObj.getDeleteJsonStr();
                jsonObj = new JSONObject(deleteJsonStr);
                String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
                if (status.trim().equalsIgnoreCase("success"))
                    logger.info("Entity having Id {} is deleted Successfully.", entityId);
            }
        } catch (Exception e) {
            logger.error("Exception while deleting Entity {} having Id {}. {}", entityName, entityId, e.getStackTrace());
        }
    }

    @BeforeClass
    public void BeforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestListingConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestListingConfigFileName");

        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");

        flowsConfigFilePath = configFilePath;
        flowsConfigFileName = configFileName;

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

        String entityIdMappingFileName;
        String baseFilePath;

        // for publishing of service data
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));


        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            failTestIfJobNotCompletedWithinSchedulerTimeOut = false;

        deleteEntityMap = new HashMap<>();
    }

    @DataProvider
    public Object[][] dataProviderForCheckSortingForCustomFields() {

        return null;
    }

    @Test
    public void checkSortingForCustomFields() {
        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "fixed fee flow for proforma";

        String clientUserName = Check.lastLoggedInUserName;
        String clientUserPass = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();

        String serviceDataSectionNames = "default";
        int contractId;
        int billingDataReportId = 444;
        int selectBillingDataListId = 444;
        int listDataOffset = 0,listDataSize = 50;
        String orderDirection = "desc nulls last";

        String contractSectionName = "default";

        try {
            boolean dbTasKForWorkflowUpdate = creatCustomFields(customAssert, ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "contractsectionname"));
            if(!dbTasKForWorkflowUpdate){
                logger.info("Workflow update task after custom field creation is failed. So, no use of moving forward.");
                customAssert.assertTrue(false,"Workflow update task after custom field creation is failed. So, no use of moving forward.");
            }
            else{
                logger.info("Workflow update task after custom field creation is success. So, moving forward.");

                try {
                    //Get Contract that will be used for Invoice Flow Validation
                    synchronized (this) {

                        new Check().hitCheck(clientUserName,clientUserPass);

                        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractsectionname");
                        if (temp != null)
                            contractSectionName = temp.trim();

                        contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractSectionName);

                        if (contractId != -1) {
                            logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flowToTest);
                            deleteEntityMap.put(contractId, contractEntity);

                            InvoiceHelper.updateMultipleServiceDataAndInvoiceConfigDistinct(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath, invoiceConfigFileName,
                                    ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicesectionname"),
                                    ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname"), contractId);

                            temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedatasectionname");
                            if (temp != null)
                                serviceDataSectionNames = temp.trim();

                            String[] serviceDataSectionNameList = serviceDataSectionNames.split(",");

                            for(String serviceDataSectionName : serviceDataSectionNameList)
                                createBillingData(flowToTest,customAssert,serviceDataSectionName,contractId);

                            //**************************************************
                            List<String> customFields = ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, customFieldSectionName);

                            ReportRendererFilterData reportRendererFilterData = new ReportRendererFilterData();
                            reportRendererFilterData.hitReportRendererFilterData(selectBillingDataListId);
                            String optionsPayload = reportRendererFilterData.getReportRendererFilterDataJsonStr();

                            JSONObject optionPayload = new JSONObject(optionsPayload);

                            ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                            reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(selectBillingDataListId);
                            String defaultUserListMetadata = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();

                            JSONObject filterData = new JSONObject(defaultUserListMetadata);
                            JSONArray filterDataArray = filterData.getJSONArray("filterMetadatas");

                            JSONObject computedFilterPayload = new JSONObject();
                            JSONObject computedFilterJson = new JSONObject();

                            String filterToApply = customFields.get(RandomNumbers.getRandomNumberWithinRange(0,customFields.size()-1));

                            filterToApply = customFields.get(1); //ToDO to be deleted whole line

                            //computedFilterPayload.put("filterMetadatas",)

                            int filterDyn = 0;

                            try{
                                for(Object object : filterDataArray){
                                    filterData = (JSONObject) object;
                                    if(customFields.contains(filterData.getString("defaultName"))&&filterData.getString("defaultName").equals(filterToApply)){
                                        JSONObject tempObject = computeFilter(optionPayload.getJSONObject(String.valueOf(filterData.getInt("id"))));
                                        filterDyn = tempObject.getInt("entityFieldId");
                                        computedFilterJson.put(String.valueOf(tempObject.getInt("filterId")),tempObject);
                                        //computedFilterPayload.put(computeFilter());
                                    }
                                }
                            }
                            catch (Exception e){
                                logger.error("Exception caught in extracting option body out of the [optionPayload] [{}]", (Object) e.getStackTrace());
                            }

                            computedFilterPayload.put("filterJson",computedFilterJson);

                            String filterMapPayload = getPayloadForFilterMap("Service Data Billing Report","id",computedFilterJson.toString(),"desc nulls last",64,0,20);

                            //computedFilterPayload.put("entityTypeId",64);
                            //**************************************************

//                            reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
//                            reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(billingDataReportId);
//
//                            String reportRendererDefaultUserListMetaDataJsonStr = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
//
//                            JSONObject jsonObject = new JSONObject(reportRendererDefaultUserListMetaDataJsonStr);
//
//                            //ToDO remove hardcoded index in json array
//                            String columnName = jsonObject.getJSONArray("columns").getJSONObject(0).getString("queryName");
//                            int columnId = jsonObject.getJSONArray("columns").getJSONObject(0).getInt("id");
//
//                            filterMapPayload = getPayloadForFilterMap(columnName,columnName,"",orderDirection,serviceDataEntityTypeId,listDataOffset,listDataSize);

                            ReportRendererListData reportRendererListData = new ReportRendererListData();
                            reportRendererListData.hitReportRendererListData(billingDataReportId,filterMapPayload);
                            String response = reportRendererListData.getListDataJsonStr();

                            String filterValueString;
                            int filterValueInt = 0;

                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray jsonArray = jsonObject.getJSONArray("data");
                            for(Object object : jsonArray){
                                if(object instanceof JSONObject){
                                    JSONObject jsonObject1 = (JSONObject) object;
                                    JSONArray jsonArray1 = jsonObject1.names();
                                    for(int index = 0;index<jsonArray1.length();index++){
                                        if(jsonObject1.getJSONObject(jsonArray1.getString(index)).getString("columnName").equalsIgnoreCase("dyn"+filterDyn)){
                                            if(filterToApply.contains("numeric"))
                                                filterValueInt = jsonObject1.getJSONObject(jsonArray1.getString(index)).getInt("value");
                                            break;
                                        }
                                    }
                                }
                            }

                            if(filterToApply.contains("numeric")){
                                try {
                                    int min = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"custom values for fields", "min"));
                                    int max = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"custom values for fields", "max"));
                                    if(min<=filterValueInt&&filterValueInt<=max){
                                        logger.debug("List is loading with expected data, Filter for number is working fine.");
                                    }
                                    else {
                                        logger.debug("List is NOT loading with expected data, Filter for number is NOT working.");
                                        customAssert.assertTrue(false,"Numeric field filter not working");
                                    }
                                }
                                catch (NumberFormatException e){
                                    logger.error("conversion of string to number for min and max");
                                }
                            }
                        } else {
                            logger.error("Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                            //customAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                            customAssert.assertTrue(false, "Couldn't get Contract Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
                    //customAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
                    customAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception occurred in checkSortingForCustomFields()");
        }
        customAssert.assertAll();
    }

    private static String getPayloadForFilterMap(String filterName,String orderBy, String filterPayload,String orderDirection, int entityTypeId, int listDataOffset,
                                                  int listDataSize) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {}", filterName);

            payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                    ",\"orderByColumnName\":\""+orderBy+"\",\"orderDirection\":\""+orderDirection+"\",\"filterJson\":" + filterPayload + "}}";
        } catch (Exception e) {
            logger.error("Exception while creating Payload for Filter {}. {}", filterName, e.getStackTrace());
        }
        return payload;
    }

    private java.lang.String getFilterJsonPayload(String filterId, String filterName, Map optionMap){
        return filterId + "\":{\"filterId\":\"" + filterId +
                "\",\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + optionMap.get("id") +
                "\",\"name\":\"" + optionMap.get("name") + "\"}]}}";
    }

    private void createBillingData(String flowToTest,CustomAssert customAssert,String serviceDataSectionName,int contractId){

        String customFieldDYN="";

        try {

            InvoicePricingHelper pricingObj = new InvoicePricingHelper();

            int selectBillingDataListId = 445;

            String columnIdForServiceDataInSelectLineItemList = "18694";
            String columnIdForIdInSelectLineItemList = "18715";

            List<JSONObject> dataForUpdatingPayload = new ArrayList<>();

            JSONObject jsonObjectTemp = new JSONObject();
            jsonObjectTemp.put("name","service data numeric field basic information");
            jsonObjectTemp.put("type","number");
            jsonObjectTemp.put("value",5);
            dataForUpdatingPayload.add(jsonObjectTemp);

            jsonObjectTemp = new JSONObject();
            jsonObjectTemp.put("name","service data text field basic information");
            jsonObjectTemp.put("type","text");
            jsonObjectTemp.put("value","field text");
            dataForUpdatingPayload.add(jsonObjectTemp);

            jsonObjectTemp = new JSONObject();
            jsonObjectTemp.put("name","service data date field basic information");
            jsonObjectTemp.put("type","date");
            jsonObjectTemp.put("value","10-15-2019 00:00:00");
            dataForUpdatingPayload.add(jsonObjectTemp);

            jsonObjectTemp = new JSONObject();
            jsonObjectTemp.put("name","service data single select field basic information");
            jsonObjectTemp.put("type","singleselect");
            jsonObjectTemp.put("value",new JSONObject("{ \"name\": \"Select Category 3\", \"id\": 33705 }"));
            dataForUpdatingPayload.add(jsonObjectTemp);

            jsonObjectTemp = new JSONObject();
            jsonObjectTemp.put("name","service data multi select field basic information");
            jsonObjectTemp.put("type","multiselect");
            jsonObjectTemp.put("value",new JSONArray("[ { \"name\": \"Select Category 1\", \"id\": 33706 }, { \"name\": \"Select Category 2\", \"id\": 33707 } ]"));
            dataForUpdatingPayload.add(jsonObjectTemp);

            CustomFieldHelper customFieldHelper = new CustomFieldHelper();
            New newServiceData = new New();
            newServiceData.hitNew(serviceDataEntity,contractEntity,contractId);
            String newResponse = newServiceData.getNewJsonStr();
            JSONObject jsonObjectParent = new JSONObject(newResponse);
            JSONObject DYNToAdd = jsonObjectParent.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");

            for(JSONObject jsonObject : dataForUpdatingPayload){
                String name = jsonObject.getString("name");
                String type = jsonObject.getString("type");
                Object object = jsonObject.get("value");

                        try {
                            customFieldHelper.findCustomFieldNodeFromJson(jsonObjectParent.getJSONObject("body")
                                    .getJSONObject("layoutInfo").getJSONObject("layoutComponent"), name);
                        } catch (Exception e) {
                            logger.info("Exception caught {}", Arrays.toString(e.getStackTrace()));
                        }
                        customFieldDYN = customFieldHelper.getCustomFieldDYN();
                        if (customFieldDYN.length() == 0) {

                            logger.info("Cannot find custom field [{}] in the layout json object in new response", name);
                            customAssert.assertTrue(false, "Cannot find custom field [" + name
                                    + "] in the layout json object in new response");
                            customAssert.assertAll();
                        }
//                        if (type.equalsIgnoreCase("number")) {
//                            int value = (int) object;
//                            DYNToAdd.getJSONObject(customFieldDYN).put("values", Integer.parseInt(value));
//                        }
//                        else {
                                DYNToAdd.getJSONObject(customFieldDYN).put("values", object);
                        //}
            }

            //UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, "dynamicMetadata", DYNToAdd.toString());
            UpdateFile.addPropertyToConfigFile(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,serviceDataSectionName,"dynamicMetadata",DYNToAdd.toString());
            //added newly


            int serviceDataId = InvoiceHelper.getServiceDataIdForDifferentClientAndSupplierId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, serviceDataSectionName, contractId);
            logger.info("Created Service Data Id : [{}]", serviceDataId);
            if (serviceDataId != -1) {
                logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);
                deleteEntityMap.put(serviceDataId, serviceDataEntity);

                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                boolean uploadPricing = true;
                String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "uploadPricing");
                if (temp != null && temp.trim().equalsIgnoreCase("false"))
                    uploadPricing = false;


                if (uploadPricing) {
                    String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest,
                            "pricingstemplatefilename");

                    boolean pricingFile = InvoiceHelper.downloadAndEditPricingFile(flowsConfigFilePath, flowsConfigFileName, pricingTemplateFilePath, pricingTemplateFileName, flowToTest, serviceDataId, pricingObj, customAssert);
                    if (pricingFile) {

                        if (pricingFile) {

                            String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

                            if (pricingUploadResponse != null && pricingUploadResponse.trim().toLowerCase().contains("your request has been successfully submitted")) {
                                temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
                                if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                                    //Wait for Pricing Scheduler to Complete
                                    // pricingSchedulerStatus = waitForPricingScheduler(flowToTest, allTaskIds);
                                    String pricingSchedulerStatus = InvoiceHelper.waitForPricingScheduler(flowToTest, allTaskIds);

                                    if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
                                        logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
                                        customAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
                                                flowToTest + "]");
                                        customAssert.assertAll();
                                        return;
                                    } else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
                                        logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
                                        if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
                                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
                                            customAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
                                                    "Hence failing Flow [" + flowToTest + "]");
                                        } else {
                                            logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow [{}]", flowToTest);
                                        }
                                        customAssert.assertAll();
                                        return;
                                    } else // in case if get processed successfully  TC-84043 validate ARR/RRC tab , Validate Charges Tab
                                    {
                                        boolean isDataCreatedUnderChargesTab = isChargesCreated(serviceDataId);

                                        if (!isDataCreatedUnderChargesTab) {
                                            customAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
                                                    flowToTest + "]");
                                            customAssert.assertAll();
                                            return;
                                        }

                                    }
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
                } else {
                    logger.info("Upload Pricing flag is turned off. Hence skipping Pricing Uploading part and proceeding further.");
                }


                List<Integer> billingIds = new InvoiceHelper().getBillingDataIds(configFilePath,configFileName,contractId,serviceDataId);

                if(billingIds.isEmpty()){
                    customAssert.assertTrue(false,"Billing data not formed hence terminating execution");
                    customAssert.assertAll();
                }

                logger.info("Billing data {}",billingIds.toString());

            } else {
                logger.error("Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [{}]", flowToTest);
                //customAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
                customAssert.assertTrue(false, "Couldn't get Service Data Id for Invoice Flow Validation. Hence skipping Flow [" + flowToTest + "]");
            }
        }
        catch (Exception e){
            logger.info("Exception Caught in createBillingData() [{}]", (Object) e.getStackTrace());
        }
    }

    private JSONObject computeFilter(JSONObject jsonObject){

            if(jsonObject.getInt("entityFieldHtmlType")==18){ //integer
                jsonObject.put("min",ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"custom values for fields", "min"));
                jsonObject.put("max",ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "custom values for fields","max"));
            }
            else if(jsonObject.getInt("entityFieldHtmlType")==8){
                jsonObject.put("start",ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "start"));
                jsonObject.put("end",ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "end"));
                jsonObject.getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA").put(0,jsonObject.getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA").getJSONObject(0));
            }

            //for( Object index : jsonObject.getJSONArray("multiselectValues"))
            //jsonObject.getJSONArray("multiselectValues").remove(in);

        return jsonObject;
    }

    private boolean creatCustomFields(CustomAssert customAssert, String flowToTest) throws ConfigurationException {
        List<String> customFields = ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, "custom field name");
        CustomField customField = new CustomField();
        boolean customFieldCreationStatus = true;
        for (String fieldName : customFields) {
            customFieldCreationStatus = customField.checkCreateCustomField(fieldName, customAssert);
            if (!customFieldCreationStatus)
                logger.info("Custom field {} was not created. Moving forward.", fieldName);
        }

        if(customField.getCustomFieldAlreadyPresent())
            return true;
        else
            return new WorkflowButtonsDbHelper().updateWorkflowDateForCustomFieldHelpWithFlow(flowToTest);
    }

    private boolean isChargesCreated(int serviceDataId) {


        int chargesTabId = 309; // hardcoded value @todo

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

    private JSONObject getFilterJsonChildTypeNumber(String name, String type, String filterId, String filterName, int entityFieldId, int min, int max, int entityFieldHtmlType){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("suffix",JSONObject.NULL);
        jsonObject.put("max",max);
        jsonObject.put("min",min);
        jsonObject.put("entityFieldHtmlType",entityFieldHtmlType);
        jsonObject.put("entityFieldId",entityFieldId);
        jsonObject.put("filterName",filterName);
        jsonObject.put("filterId",filterId);
        jsonObject.put(name,jsonObject);

        return jsonObject;
    }
//    private JSONObject getFilterJsonChildTypeText(String name, String type, String filterId, String filterName, int entityFieldId, int min, int max, int entityFieldHtmlType){
//        JSONObject jsonObject = new JSONObject();
//        JSONArray jsonArray = new JSONArray();
//
//        jsonObject.put("id",rep)
//
//
//        jsonObject.put("start",start);
//        jsonObject.put("end",end);
//        jsonObject.put("entityFieldHtmlType",entityFieldHtmlType);
//        jsonObject.put("entityFieldId",entityFieldId);
//        jsonObject.put("filterName",filterName);
//        jsonObject.put("filterId",filterId);
//
//        return jsonObject;
//    }
}
