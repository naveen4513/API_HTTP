package com.sirionlabs.test.invoice;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TestCurrConvDynamicFields {

    private final static Logger logger = LoggerFactory.getLogger(TestCurrConvDynamicFields.class);

    private String configFilePath;
    private String configFileName;

    private String serviceData = "service data";
    private String invoices = "invoices";
    private String invoiceLineItem = "invoice line item";

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestCustomCurrConvFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestCustomCurrConvFileName");

    }

    @DataProvider
    public Object[][] dataProviderForReports(){

        logger.info("Data Provider for Invoice Custom Fields Validation");
        List<Object[]> allTestData = new ArrayList<>();

        List<String> flowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows to test").split(","));

        for(String flowToTest : flowsToTest) {

            List<String> reportIdsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"report ids to test").split(","));

            List<String> reportIdsToSkip = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"report ids to skip").split(","));

            int customFieldId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"custom currency field id"));
            int filterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"filter id"));

            for (String reportId : reportIdsToTest) {

                if (reportIdsToSkip.contains(reportId)) {
                    continue;
                }
                String recordId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,  reportId,"entity id");

                if (recordId != null) {

                    allTestData.add(new Object[]{flowToTest,reportId, recordId, customFieldId, filterId});

                }

            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForReports",enabled = true)
    public void TestCustomCurrFieldValidationReports(String entityName,String reportId,String recordId,int customFieldId,int filterId){

        logger.info("Testing for flow " + entityName);
        CustomAssert customAssert = new CustomAssert();
        Edit edit = new Edit();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        try{

            Double conversionFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,reportId,"conv factor"));
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            int entityId = Integer.parseInt(recordId);

            if(recordId.equals("") || recordId.equals("No Data")){
                throw new SkipException("Record Id not found in configuration file Thus Skipping the test");
            }

            String editPayload = edit.getEditPayload(entityName,entityId);
            JSONObject editPayloadJson = new JSONObject(editPayload);

            int customFieldValue = RandomNumbers.getRandomNumberWithinRangeIndex(100,10000);
            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customFieldId).put("values",customFieldValue);

            String editResponse = edit.hitEdit(entityName,editPayloadJson.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit done unsuccessfully on invoice for custom field ");
            }else {

                HashMap<String, String> columnValuesMap = invoiceHelper.getReportListResp(Integer.parseInt(reportId),entityTypeId, filterId, customFieldId, String.valueOf(customFieldValue));

                if(columnValuesMap.size() == 0){
                    customAssert.assertTrue(false,"On Report Filter Response no record found");
                }else {
                    ArrayList<String> customFieldIds = new ArrayList<>();
                    customFieldIds.add(String.valueOf(customFieldId));
                    validateCustomConvValues(columnValuesMap,customFieldIds,conversionFactor,customAssert);
                }

            }


        }
        catch (Exception e){

            if(e instanceof SkipException ){
                throw new SkipException(e.getMessage());
            }

            logger.error("Exception while validating the scenario in main test method " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario in main test method " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @DataProvider
    public Object[][] dataProviderForListing(){

        logger.info("Data Provider for Custom Fields Validation Listing");
        List<Object[]> allTestData = new ArrayList<>();

        List<String> flowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows to test").split(","));

        for(String flowToTest : flowsToTest) {

            int listId = ConfigureConstantFields.getListIdForEntity(flowToTest);

            int customFieldId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "custom currency field id"));
            int listFilterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "filter id"));


            String recordId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "entity id");

            if (recordId != null) {

                allTestData.add(new Object[]{flowToTest, listId, recordId, customFieldId, listFilterId});

            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForListing")
    public void TestCustomCurrFieldValidationListing(String entityName,int listId,String recordId,int customFieldId,int filterId){

        logger.info("Testing for flow " + entityName);
        CustomAssert customAssert = new CustomAssert();
        Edit edit = new Edit();
        InvoiceHelper invoiceHelper = new InvoiceHelper();

        try{

            Double conversionFactor = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"conv factor"));
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            int entityId = Integer.parseInt(recordId);

            if(recordId.equals("") || recordId.equals("No Data")){
                throw new SkipException("Record Id not found in configuration file Thus Skipping the test");
            }

            String editPayload = edit.getEditPayload(entityName,entityId);
            JSONObject editPayloadJson = new JSONObject(editPayload);

            int customFieldValue = RandomNumbers.getRandomNumberWithinRangeIndex(100,10000);
            editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("dyn" + customFieldId).put("values",customFieldValue);

            String editResponse = edit.hitEdit(entityName,editPayloadJson.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit done unsuccessfully on invoice for custom field ");
            }else {

                HashMap<String, String> columnValuesMap = new HashMap<>();
                Boolean entityPresent = false;

                if(entityName.equals(invoices)){
                    String startDate = ShowHelper.getValueOfField(entityTypeId,entityId,"invoiceperiodfromdatevalues");
                    String endDate = ShowHelper.getValueOfField(entityTypeId,entityId,"invoiceperiodtodatevalues");

                    List<HashMap<String, String>> listResp = invoiceHelper.getListingResponseInvoice(listId,startDate,endDate,filterId,customFieldId,String.valueOf(customFieldValue),customAssert);
                    columnValuesMap = invoiceHelper.checkIfEntityIsPresent(listResp,recordId);

                }else {

                    List<HashMap<String, String>> listResp = invoiceHelper.getListResp(listId, entityTypeId, filterId, customFieldId, String.valueOf(customFieldValue),customAssert);

                    columnValuesMap = invoiceHelper.checkIfEntityIsPresent(listResp,recordId);
                }

                if(columnValuesMap.size() == 0){
                    customAssert.assertTrue(false,"On List Filter Response no record found");
                }else {
                    ArrayList<String> customFieldIds = new ArrayList<>();
                    customFieldIds.add(String.valueOf(customFieldId));
                    validateCustomConvValues(columnValuesMap,customFieldIds,conversionFactor,customAssert);
                }
            }
        }
        catch (Exception e){

            if(e instanceof SkipException ){
                throw new SkipException(e.getMessage());
            }

            logger.error("Exception while validating the scenario in main test method " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario in main test method " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    private void validateCustomConvValues(HashMap<String,String> listValuesMap,ArrayList<String> customFieldIds,Double convFactor,CustomAssert customAssert){

        try{
            String customField;
            String convertedCustomField;
            String customFieldValue;
            String actualConvCustomFieldValue;
            String expectedConvCustomFieldValue;


            for(String customFieldId : customFieldIds){

                customField = "dyn" + customFieldId;
                convertedCustomField = "client" + customField;

                if(listValuesMap.containsKey(customField)){
                    customFieldValue = listValuesMap.get(customField);

                    if(customFieldValue == null){
                        customAssert.assertTrue(false,"Field value is null for custom field id " + customFieldId);
                    }else if(customFieldValue.equals("null") || customFieldValue.equals("")){
                        customAssert.assertTrue(false,"Field value is blank for custom field id " + customFieldId);

                    }else {
                        expectedConvCustomFieldValue = String.valueOf(Double.parseDouble(customFieldValue) * convFactor);
                        actualConvCustomFieldValue = listValuesMap.get(convertedCustomField);

                        if(!actualConvCustomFieldValue.contains(expectedConvCustomFieldValue)){
                            customAssert.assertTrue(false,"Expected and Actual value of converted custom field id " + convertedCustomField + " not matched");
                        }
                    }



                }else {
                    customAssert.assertTrue(false,"Listing does not have the " + customField + " column present in the list Response ");
                }

            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Custom Conv Values");
        }

    }


}
