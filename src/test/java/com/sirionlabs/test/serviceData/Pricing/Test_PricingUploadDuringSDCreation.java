package com.sirionlabs.test.serviceData.Pricing;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;

import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.crypto.spec.IvParameterSpec;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/*
SIR-10057
 */
public class Test_PricingUploadDuringSDCreation {

    private final static Logger logger = LoggerFactory.getLogger(Test_PricingUploadDuringSDCreation.class);

    private String creationFilePath;
    private String creationFileName;
    private String extraFieldsCreationFileName;

    private String serviceData = "service data";

    private String configFilePath;
    private String configFileName;

    List<Integer> entityToDelete = new ArrayList<>();
    int sdEntityTypeId = 64;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSDWithPricingUploadFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSDWithPricingUploadFileName");

        creationFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataFilePath");
        creationFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataFileName");
        extraFieldsCreationFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

    }

    @DataProvider(name = "serviceDataFlows", parallel = false)
    public Object[][] serviceDataFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flows to test").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "serviceDataFlows")
    public void TestPricingUploadDuringSDCreation(String flowToTest) {

        CustomAssert customAssert = new CustomAssert();

        try {

            String sectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sd section");

            String uniqueString = DateUtils.getCurrentTimeStamp();

            UpdateFile.updateConfigFileProperty(creationFilePath, extraFieldsCreationFileName, sectionName, "serviceIdClient",
                    "new client", "newClient" + uniqueString);
            UpdateFile.updateConfigFileProperty(creationFilePath, extraFieldsCreationFileName, sectionName, "serviceIdSupplier",
                    "new supplier", "newSupplier" + uniqueString);

            CreateEntity createEntity = new CreateEntity(creationFilePath, creationFileName, creationFilePath, extraFieldsCreationFileName, sectionName);

            String createPayload = createEntity.getCreatePayload(serviceData, true, false);

            List<Map<String, Object>> pricingValuesList = createPricingValuesList(flowToTest);
            List<Map<String, Object>> arcValuesList = createArcValuesList(flowToTest);

            createPayload = addPricingDetails(createPayload, pricingValuesList);
            createPayload = addARCRRCDetails(createPayload, arcValuesList);
            createPayload = updateStartDateEndDate(flowToTest, createPayload);

            if (createPayload != null) {
                logger.info("Hitting Create Api for Entity {}.", serviceData);
                Create createObj = new Create();
                createObj.hitCreate(serviceData, createPayload);
                String createResponse = createObj.getCreateJsonStr();

                int serviceDataId = CreateEntity.getNewEntityId(createResponse, serviceData);

                if (serviceDataId != -1) {
                    List<Map<String, String>> chargesTabDataActual = getChargesTabData(serviceDataId, customAssert);
                    List<Map<String, String>> expectedChargesTabDataList = createPricingValuesListChargesTab(flowToTest);
                    validateTabData(chargesTabDataActual, expectedChargesTabDataList, "Charges", customAssert);

                    if (flowToTest.equals("arc pos flow")) {
                        List<Map<String, String>> arcTabDataActual = getArcTabData(serviceDataId, customAssert);
                        List<Map<String, String>> expectedArcTabDataList = createArcValuesListArcTab(flowToTest);
                        validateTabData(arcTabDataActual, expectedArcTabDataList, "ARC /RRC", customAssert);

                    }

                    if (flowToTest.equals("arc pos flow") || flowToTest.equals("vol pricing pos flow")) {
                        InvoiceHelper invoiceHelper = new InvoiceHelper();

                        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                        workflowActionsHelper.performWorkFlowStepV2(sdEntityTypeId, serviceDataId, "Publish", customAssert);
                        ArrayList<Integer> consumptionIds = new ArrayList<>();
                        invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);

                        if (consumptionIds.size() == 0) {
                            customAssert.assertTrue(false, "Consumptions not generated for service data");
                        }
                    }
                    entityToDelete.add(serviceDataId);
                } else {
                    customAssert.assertTrue(false, "Service Data Is not created");
                }

                UpdateFile.updateConfigFileProperty(creationFilePath, extraFieldsCreationFileName, sectionName, "serviceIdClient",
                        "newClient" + uniqueString, "new client");
                UpdateFile.updateConfigFileProperty(creationFilePath, extraFieldsCreationFileName, sectionName, "serviceIdSupplier",
                        "newSupplier" + uniqueString, "new supplier");

            }


        } catch (Exception e) {
            logger.error("Exception in main test method while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    @AfterClass
    public void AfterClass(){

//        EntityOperationsHelper.deleteMultipleRecords(serviceData,entityToDelete);
    }

    private String addPricingDetails(String createPayload, List<Map<String,Object>> pricingValues){

        try{
            JSONObject createPayloadJson = new JSONObject(createPayload);
            JSONArray valuesJsonArray = new JSONArray();

            for(Map<String,Object> pricingValue : pricingValues){
                JSONObject valuesJson = new JSONObject();

                for (Map.Entry<String,Object> entry : pricingValue.entrySet()){

                    valuesJson.put(entry.getKey(),entry.getValue());

                }

                valuesJsonArray.put(valuesJson);
            }

            createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("pricings").put("values",valuesJsonArray);

            createPayload = createPayloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while updating pricing details in create Payload");
        }
        return createPayload;
    }

    private String addARCRRCDetails(String createPayload,List<Map<String,Object>> arcValues){

        try{

            JSONObject createPayloadJson = new JSONObject(createPayload);
            JSONArray valuesJsonArray = new JSONArray();

            for(Map<String,Object> arcValue : arcValues){
                JSONObject valuesJson = new JSONObject();

                for (Map.Entry<String,Object> entry : arcValue.entrySet()){

                    if(entry.getKey().equals("type")){

                        JSONObject typeJson = new JSONObject();

                        typeJson.put("id",entry.getValue());
                        valuesJson.put("type",typeJson);

                        continue;
                    }
                    valuesJson.put(entry.getKey(),entry.getValue());

                }

                valuesJsonArray.put(valuesJson);
            }

            createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("arcRrc").put("values",valuesJsonArray);

            createPayload = createPayloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while updating arc details in create Payload");
        }

        return createPayload;
    }

    private String updateStartDateEndDate(String flowToTest,String createPayload){

        try{
            String startDate = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"service start date");
            String endDate = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"service end date");

            JSONObject createPayloadJson = new JSONObject(createPayload);

            createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("startDate").put("values",startDate);
            createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("endDate").put("values",endDate);

            createPayload = createPayloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while updating start date and end date in create Payload");
        }

        return createPayload;
    }

    private List<Map<String,Object>> createPricingValuesList(String flowToTest){

        List<Map<String,Object>> pricingValuesList = new ArrayList<>();

        try{

            List<String> pricingStartDates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing start dates").split(","));
            List<String> pricingEndDates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing end dates").split(","));
            List<String> pricingVolumes = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing volume").split(","));
            List<String> pricingUnitRates =Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing unit rate").split(","));

            for(int i =0;i<pricingStartDates.size();i++){

                Map<String,Object> pricingValues = new HashMap<>();

                pricingValues.put("startDate",pricingStartDates.get(i));
                pricingValues.put("endDate",pricingEndDates.get(i));
                pricingValues.put("volume",Double.parseDouble(pricingVolumes.get(i)));
                pricingValues.put("unitRate",Integer.parseInt(pricingUnitRates.get(i)));

                pricingValuesList.add(pricingValues);
            }

        }catch (Exception e){
            logger.error("Exception while creating pricing Values List");
        }

        return pricingValuesList;
    }

    private List<Map<String,String>> createPricingValuesListChargesTab(String flowToTest){

        List<Map<String,String>> pricingValuesList = new ArrayList<>();

        try{

            List<String> pricingStartDates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing start date charges tab").split(","));
            List<String> pricingEndDates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing end date charges tab").split(","));
            List<String> pricingVolumes = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing volume").split(","));
            List<String> pricingUnitRates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing unit rate").split(","));
            String unit = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing unit charges tab");
            String currency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"pricing currency charges tab");

            for(int i =0;i<pricingStartDates.size();i++){

                Map<String,String> pricingValues = new HashMap<>();

                pricingValues.put("startdate",pricingStartDates.get(i));
                pricingValues.put("enddate",pricingEndDates.get(i));
                pricingValues.put("volume",pricingVolumes.get(i));
                pricingValues.put("unitrate",pricingUnitRates.get(i));

                pricingValues.put("currency",currency);
                pricingValues.put("unit",unit);

                pricingValues.put("baseamount",String.valueOf(Double.parseDouble(pricingVolumes.get(i)) * Double.parseDouble(pricingUnitRates.get(i))));

                pricingValuesList.add(pricingValues);
            }

        }catch (Exception e){
            logger.error("Exception while creating pricing Values List");
        }

        return pricingValuesList;
    }

    private List<Map<String,String>> createArcValuesListArcTab(String flowToTest){

        List<Map<String,String>> arcValuesList = new ArrayList<>();

        try{

            List<String> arcStartDates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"start date arc tab").split(","));
            List<String> arcEndDates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"end date arc tab").split(","));
            List<String> lowerLevels = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"arc lower volume").split(","));
            List<String> upperLevels = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"arc upper volume").split(","));
            List<String> rate = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"arc rate").split(","));
            List<String> type = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"type").split(","));


            for(int i =0;i<arcStartDates.size();i++){

                Map<String,String> arcValues = new HashMap<>();

                arcValues.put("startdate",arcStartDates.get(i));
                arcValues.put("enddate",arcEndDates.get(i));
                arcValues.put("lowerlevel",lowerLevels.get(i));
                arcValues.put("upperlevel",upperLevels.get(i));

                arcValues.put("rate",rate.get(i));
                arcValues.put("type",type.get(i));

                arcValuesList.add(arcValues);
            }

        }catch (Exception e){
            logger.error("Exception while creating pricing Values List");
        }

        return arcValuesList;
    }

    private List<Map<String,Object>> createArcValuesList(String flowToTest){

        List<Map<String,Object>> arcValuesList = new ArrayList<>();

        try{

            List<String> arcStartDates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"arc start dates").split(","));
            List<String> arcEndDates = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"arc end dates").split(","));
            List<String> arcLVs = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"arc lower volume").split(","));
            List<String> arcUVs =Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"arc upper volume").split(","));
            List<String> arcRates =Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"arc rate").split(","));
            List<String> invoicingTypes =Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"invoicing type").split(","));

            for(int i =0;i<arcStartDates.size();i++){

                Map<String,Object> arcValues = new HashMap<>();

                arcValues.put("startDate",arcStartDates.get(i));
                arcValues.put("endDate",arcEndDates.get(i));
                arcValues.put("lowerVolume",Double.parseDouble(arcLVs.get(i)));
                arcValues.put("upperVolume",Double.parseDouble(arcUVs.get(i)));
                arcValues.put("rate",Integer.parseInt(arcRates.get(i)));
                arcValues.put("type",Integer.parseInt(invoicingTypes.get(i)));

                arcValuesList.add(arcValues);

            }

        }catch (Exception e){
            logger.error("Exception while creating arc Values List");
        }

        return arcValuesList;
    }

    private List<Map<String,String>> getChargesTabData(int serviceDataId,CustomAssert customAssert){

        List<Map<String,String>> chargesTabData = new ArrayList<>();

        int chargesTabId = 309;
        TabListData tabListData = new TabListData();
        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"enddate\",\"orderDirection\":\"asc nulls last\",\"filterJson\":{}}}";

            chargesTabData = tabListData.getTabDataList(chargesTabId,sdEntityTypeId,serviceDataId,payload,customAssert);

        }catch (Exception e){
            logger.error("Exception while validating charges tab");
            customAssert.assertTrue(false,"Exception while validating charges tab");

        }
        return chargesTabData;
    }

    private List<Map<String,String>> getArcTabData(int serviceDataId,CustomAssert customAssert){

        List<Map<String,String>> arcTabData = new ArrayList<>();

        int arcTabId = 311;
        TabListData tabListData = new TabListData();
        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"enddate\",\"orderDirection\":\"asc nulls last\",\"filterJson\":{}}}";

            arcTabData = tabListData.getTabDataList(arcTabId,sdEntityTypeId,serviceDataId,payload,customAssert);


        }catch (Exception e){
            logger.error("Exception while getting arc tab data");
            customAssert.assertTrue(false,"Exception while getting arc tab data");

        }
        return arcTabData;
    }



    private Boolean validateTabData(List<Map<String,String>> actualData,List<Map<String,String>> expectedData,String tabName,CustomAssert customAssert){

        AtomicReference<Boolean> validationStatus = new AtomicReference<>(true);
        try{

            if(actualData.size() != expectedData.size()){
                customAssert.assertTrue(false,"Actual and Expected number of rows didn't matched");
            }else {

                for(int i =0;i <actualData.size();i++){

                    Map<String,String> actualDataMap  = actualData.get(i);
                    Map<String,String> expectedDataMap  = expectedData.get(i);

                    actualDataMap.forEach((key,value) -> {
                        logger.info("Inside Lambda expression ");

                        String expectedValue =  expectedDataMap.get(key);
                        if(expectedValue !=null) {
                            if (!value.contains(expectedValue)) {
                                logger.error("Expected value not matched for column " + key + " in tab : " + tabName);
                                customAssert.assertTrue(false, "Expected value not matched for column " + key + " in tab : " + tabName);
                                validationStatus.set(false);
                            }
                        }else {
                            logger.debug("Expected Value is null for column " + key);
                        }
                    });

                }

            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating charges tab data ");
            validationStatus.set(false);
        }

        return validationStatus.get();
    }

}
