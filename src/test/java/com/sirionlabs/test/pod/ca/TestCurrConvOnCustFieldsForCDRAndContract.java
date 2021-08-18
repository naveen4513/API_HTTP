package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.CurrencyConversionHelper;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class TestCurrConvOnCustFieldsForCDRAndContract{
    private final static Logger logger = LoggerFactory.getLogger(TestCurrConvOnCustFieldsForCDRAndContract.class);
    private String configFilePath;
    private String configFileName;
    private String entityURLId;
    private String entityTypeId;
    private String automationCurrencyAmount;
    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String cdrExtraFieldsConfigFilePath;
    private String cdrExtraFieldsConfigFileName;
    private Map<String, String> defaultProperties;
    private HashMap<String,Integer> deleteEntities = new HashMap<>();

    @BeforeClass
    public void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFileName");
        defaultProperties = ParseConfigFile.getAllProperties(cdrConfigFilePath, cdrConfigFileName);

        cdrExtraFieldsConfigFilePath = defaultProperties.get("extrafieldsconfigfilepath");
        cdrExtraFieldsConfigFileName = defaultProperties.get("extrafieldsconfigfilename");

        automationCurrencyAmount = "1000";
    }

    @Test
    public void testC90006(){
        CustomAssert customAssert = new CustomAssert();
        int cdrId = createCDR(customAssert);
        boolean addCurrencyAmount = addAmount("contract draft request",true, true,cdrId,customAssert);
        try{
            if(addCurrencyAmount){
                HashMap<String, Object> getShowPageFinancialData = getShowPageData(cdrId,"contract draft request",customAssert);
                double conversionFactor = (double) getShowPageFinancialData.get("ConversionFactor");
                double customCurrencyInput = (double) getShowPageFinancialData.get("CustomCurrencyInput");
                double customCurrencyOutput = (double) getShowPageFinancialData.get("CustomCurrencyOutput");
                logger.info("Conversion Factor is {}, Custom currency Input is {} and Custom Currency Output is {}",conversionFactor,customCurrencyInput,customCurrencyOutput);
                if(customCurrencyOutput==(customCurrencyInput)*conversionFactor){
                    logger.info("Conversion in CDR is valid as per the conversion matrix");
                    int contractId = createContractFromCDR(cdrId,customAssert);
                    addCurrencyAmount = addAmount("contracts",true, true,contractId,customAssert);
                    if(addCurrencyAmount){
                        getShowPageFinancialData = getShowPageData(contractId,"contracts",customAssert);
                        conversionFactor = (double) getShowPageFinancialData.get("ConversionFactor");
                        customCurrencyInput = (double) getShowPageFinancialData.get("CustomCurrencyInput");
                        customCurrencyOutput = (double) getShowPageFinancialData.get("CustomCurrencyOutput");
                        logger.info("Conversion Factor is {}, Custom currency Input is {} and Custom Currency Output is {}",conversionFactor,customCurrencyInput,customCurrencyOutput);
                        if(customCurrencyOutput==(customCurrencyInput)*conversionFactor){
                            logger.info("Conversion in Contract is valid as per the conversion matrix");
                        }else{
                            logger.error("Conversion in Contracts is invalid as per the conversion matrix");
                            customAssert.assertTrue(false,"Conversion in Contracts is invalid as per the conversion matrix");
                        }
                    }else{
                        logger.error("Currency Amount could not be added to the Contract.");
                        customAssert.assertTrue(false,"Currency Amount could not be added to the Contract.");
                    }
                }else{
                    logger.error("Conversion in CDR is invalid as per the conversion matrix");
                    customAssert.assertTrue(false,"Conversion in CDR is invalid as per the conversion matrix");
                }
            }else{
                logger.error("CDR could not be edited");
                customAssert.assertTrue(false,"CDR could not be edited");
            }
        }catch(Exception e){
            logger.error("Exception occurred while checking the conversion rate of currency");
            customAssert.assertTrue(false,"Exception occurred while checking the conversion rate of currency");
        }finally {
            Set<String> entities = deleteEntities.keySet();
            for(String entity : entities){
                EntityOperationsHelper.deleteEntityRecord(entity, deleteEntities.get(entity));
                logger.info("Entity with entityId id {} is deleted.",deleteEntities.get(entity));
            }
        }
        customAssert.assertAll();
    }

    @Test
    public void testC90008(){
        CustomAssert customAssert = new CustomAssert();
        int cdrId = createCDR(customAssert);

        String rateCardFromDate = "10-01-2020";
        String rateCardToDate = "10-31-2020";
        try{
            boolean editEntity = editEntity("contract draft request",true, true,true,rateCardFromDate,rateCardToDate,cdrId,customAssert);
            if(editEntity) {
                HashMap<String, Object> getShowPageFinancialData = getShowPageData(cdrId, "contract draft request", customAssert);
                double conversionFactor = (double) getShowPageFinancialData.get("ConversionFactor");
                double customCurrencyInput = (double) getShowPageFinancialData.get("CustomCurrencyInput");
                double customCurrencyOutput = (double) getShowPageFinancialData.get("CustomCurrencyOutput");
                logger.info("Conversion Factor is {}, Custom currency Input is {} and Custom Currency Output is {}",conversionFactor,customCurrencyInput,customCurrencyOutput);
                if(customCurrencyOutput==(customCurrencyInput)*conversionFactor){
                    logger.info("Conversion is valid as per the conversion matrix");
                    int contractId = createContractFromCDR(cdrId,customAssert);
                    editEntity = editEntity("contract draft request",true, true, true,rateCardFromDate,rateCardToDate,cdrId,customAssert);
                    if(editEntity){
                        getShowPageFinancialData = getShowPageData(cdrId, "contract draft request", customAssert);
                        conversionFactor = (double) getShowPageFinancialData.get("ConversionFactor");
                        customCurrencyInput = (double) getShowPageFinancialData.get("CustomCurrencyInput");
                        customCurrencyOutput = (double) getShowPageFinancialData.get("CustomCurrencyOutput");
                        logger.info("Conversion Factor is {}, Custom currency Input is {} and Custom Currency Output is {}",conversionFactor,customCurrencyInput,customCurrencyOutput);
                        if(customCurrencyOutput==(customCurrencyInput)*conversionFactor){
                            logger.info("Conversion is valid as per the conversion matrix");
                        }else{
                            logger.error("Conversion is invalid as per the conversion matrix");
                            customAssert.assertTrue(false,"Conversion is invalid as per the conversion matrix");
                        }
                    }else{
                        logger.error("Could not add the rate card details to entity Contracts.");
                        customAssert.assertTrue(false,"Could not add the rate card details to entity Contracts.");
                    }
                }else{
                    logger.error("Conversion is invalid as per the conversion matrix");
                    customAssert.assertTrue(false,"Conversion is invalid as per the conversion matrix");
                }
            }else{
                logger.error("Could not add the rate card details to entity Contract Draft Request.");
                customAssert.assertTrue(false,"Could not add the rate card details to entity Contract Draft Request.");
            }
        }catch(Exception e){
            logger.error("Exception occurred while checking the conversion rate of currency");
            customAssert.assertTrue(false,"Exception occurred while checking the conversion rate of currency");
        }finally {
            Set<String> entities = deleteEntities.keySet();
            for(String entity : entities){
                EntityOperationsHelper.deleteEntityRecord(entity, deleteEntities.get(entity));
                logger.info("Entity with entityId id {} is deleted.",deleteEntities.get(entity));
            }
        }
        customAssert.assertAll();
    }

    @Test
    public void testC90016(){
        CustomAssert customAssert = new CustomAssert();
        try{
            entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contract draft request","entity_url_id");
            entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contract draft request","entity_type_id");
            String clientBaseCurrency = new CurrencyConversionHelper().getClientBaseCurrency();
            int cdrId = createCDR(customAssert);
            boolean addAmount = addAmount("contract draft request",false,true,cdrId,customAssert);
            if(addAmount){
                HashMap<String, Double> listingResponseCurrencyData= getListingResponse(cdrId, clientBaseCurrency, entityURLId, customAssert);
                double currencyAmount = listingResponseCurrencyData.get("CurrencyAmount");
                if(currencyAmount==Double.parseDouble(automationCurrencyAmount)){
                    logger.info("Data on listing \"Contract Draft Request\" is correct with same currency as Client Currency.");
                    boolean editEntity = addAmount("contract draft request",true,false,cdrId,customAssert);
                    if(editEntity){
                        listingResponseCurrencyData= getListingResponse(cdrId, "USD", entityURLId, customAssert);
                        currencyAmount = listingResponseCurrencyData.get("CurrencyAmount");
                        double conversionFactor = Double.parseDouble(getShowPageData(cdrId,"contract draft request",customAssert).get("ConversionFactor").toString());
                        if((Double.parseDouble(automationCurrencyAmount)*conversionFactor)==currencyAmount){
                            logger.info("Data on listing \"Contract Draft Request\" is correct with different currency as Client Currency.");
                            entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contracts","entity_url_id");
                            entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"contracts","entity_type_id");
                            int contractId = createContractFromCDR(cdrId,customAssert);
                            addAmount = addAmount("contracts",false,true,contractId,customAssert);
                            if(addAmount){
                                listingResponseCurrencyData= getListingResponse(cdrId, clientBaseCurrency, entityURLId, customAssert);
                                currencyAmount = listingResponseCurrencyData.get("CurrencyAmount");
                                if(currencyAmount==Double.parseDouble(automationCurrencyAmount)){
                                    logger.info("Data on listing \"Contracts\" is correct with same currency as Client Currency.");
                                    editEntity = addAmount("contracts",true,false,contractId,customAssert);
                                    if(editEntity){
                                        listingResponseCurrencyData= getListingResponse(cdrId, "USD", entityURLId, customAssert);
                                        currencyAmount = listingResponseCurrencyData.get("CurrencyAmount");
                                        conversionFactor = Double.parseDouble(getShowPageData(contractId,"contracts",customAssert).get("ConversionFactor").toString());
                                        if((Double.parseDouble(automationCurrencyAmount)*conversionFactor)==currencyAmount){
                                            logger.info("Data on listing \"Contracts\" is correct with different currency as Client Currency.");
                                        }else{
                                            logger.info("Data on listing \"Contracts\" is not correct with different currency as Client Currency.");
                                            customAssert.assertTrue(false,"Data on listing \"Contracts\" is not correct with different currency as Client Currency");
                                        }
                                    }else{
                                        logger.error("Currency could not be changed.");
                                        customAssert.assertTrue(false,"Currency could not be changed.");
                                    }
                                }else{
                                    logger.error("Data on listing \"Contracts\" is not correct with same currency as Client Currency.");
                                    customAssert.assertTrue(false,"Data on listing \"Contracts\" is not correct with same currency as Client Currency");
                                }
                            }else{
                                logger.error("Amount could not be added to the entity.");
                                customAssert.assertTrue(false,"Amount could not be added to the entity.");
                            }
                        }else{
                            logger.info("Data on listing \"Contract Draft Request\" is not correct with different currency as Client Currency.");
                            customAssert.assertTrue(false,"Data on listing \"Contract Draft Request\" is not correct with different currency as Client Currency");
                        }
                    }else{
                        logger.error("Currency could not be changed.");
                        customAssert.assertTrue(false,"Currency could not be changed.");
                    }
                }else{
                    logger.error("Data on listing \"Contract Draft Request\" is not correct with same currency as Client Currency.");
                    customAssert.assertTrue(false,"Data on listing \"Contract Draft Request\" is not correct with same currency as Client Currency");
                }
            }else{
                logger.error("Amount could not be added to the entity.");
                customAssert.assertTrue(false,"Amount could not be added to the entity.");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while comparing the list columns",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while comparing the list columns");
        }finally {
            Set<String> entities = deleteEntities.keySet();
            for(String entity : entities){
                EntityOperationsHelper.deleteEntityRecord(entity, deleteEntities.get(entity));
                logger.info("Entity with entityId id {} is deleted.",deleteEntities.get(entity));
            }
        }
        customAssert.assertAll();
    }

    @Test
    public void testC900018(){
        CustomAssert customAssert = new CustomAssert();
        try{
            String clientBaseCurrency = new CurrencyConversionHelper().getClientBaseCurrency();
            if(downloadListingData("contract draft request", clientBaseCurrency, customAssert)){
                logger.info("Columns for Custom Currency and Client Currency for CDR are available in the listing excel sheet.");
            }else{
                logger.error("Columns for Custom Currency and Client Currency for CDR are not available in the listing excel sheet.");
                customAssert.assertTrue(false,"Columns for Custom Currency and Client Currency for CDR are not available in the listing excel sheet.");
            }

            if(downloadListingData("contracts", clientBaseCurrency, customAssert)){
                logger.info("Columns for Custom Currency and Client Currency for Contracts are available in the listing excel sheet.");
            }else{
                logger.error("Either one or both columns for Custom Currency and Client Currency for Contracts are not available in the listing excel sheet.");
                customAssert.assertTrue(false,"Either one or both columns for Custom Currency and Client Currency for Contracts are not available in the listing excel sheet.");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while comparing the columns Custom Currency and Client Custom Currency",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while comparing the columns Custom Currency and Client Custom Currency");
        }finally {

        }
        customAssert.assertAll();
    }

    @Test
    public void testC90019(){
        CustomAssert customAssert = new CustomAssert();
        String rateCardFromDate = "10-01-2018";
        String rateCardToDate = "10-31-2018";
        try{
            int cdrId = createCDR(customAssert);
            if(addAmount("contract draft request",false,false,cdrId,customAssert)){
                int contractId = createContractFromCDR(cdrId,customAssert);
                boolean editEntity = addAmount("contracts",false,true,contractId,customAssert);
                if(editEntity){
                    HashMap<String, Object> contractShowData =  getShowPageData(contractId,"contracts", customAssert);
                    String contractName = contractShowData.get("Name").toString();
                    int newCdrId = createCDRFromContract(contractId,contractName, customAssert);
                    if(newCdrId!=-1){
                        addAmount("contract draft request",true,true,newCdrId,customAssert);
                        if(editEntity("contracts",true,false,true,rateCardFromDate,rateCardToDate,contractId,customAssert)){
                            HashMap<String, Object> getShowPageFinancialData = getShowPageData(contractId,"contracts",customAssert);
                            double conversionFactor = (double) getShowPageFinancialData.get("ConversionFactor");
                            double customCurrencyInput = (double) getShowPageFinancialData.get("CustomCurrencyInput");
                            double customCurrencyOutput = (double) getShowPageFinancialData.get("CustomCurrencyOutput");
                            logger.info("Conversion Factor is {}, Custom currency Input is {} and Custom Currency Output is {}",conversionFactor,customCurrencyInput,customCurrencyOutput);
                            if(customCurrencyOutput==(customCurrencyInput)*conversionFactor){
                                HashMap<String, Object> cdrShowData =  getShowPageData(newCdrId, "contract draft request",customAssert);
                                if((double)cdrShowData.get("CustomCurrencyInput")==Double.parseDouble(automationCurrencyAmount)){
                                    logger.info("Changes in Contract has not impacted the CDR");
                                }else{
                                    logger.error("Changes in Contract has impacted the CDR");
                                    customAssert.assertTrue(false,"Changes in Contract has impacted the CDR");
                                }
                            }else{
                                logger.error("Conversion is invalid as per the conversion matrix");
                                customAssert.assertTrue(false,"Conversion is invalid as per the conversion matrix");
                            }
                        }else{
                            logger.error("Contract could not be edited");
                            customAssert.assertTrue(false,"Contract could not be edited");
                        }
                        EntityOperationsHelper.deleteEntityRecord("contract draft request", newCdrId);
                        logger.info("Entity with entityId id {} is deleted.",newCdrId);
                    }else{
                        logger.error("CDR could not be created");
                        customAssert.assertTrue(false,"CDR could not be created");
                    }
                }else{
                    logger.error("Contract could not be created");
                    customAssert.assertTrue(false,"Contract could not be created");
                }
            }else{
                logger.error("CDR could not be updated.");
                customAssert.assertTrue(false,"CDR could not be updated.");
            }
        }catch(Exception e){
            logger.error("Exception {} is making the test case fail",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" is making the test case fail");
        }finally{
            Set<String> entities = deleteEntities.keySet();
            for(String entity : entities){
                EntityOperationsHelper.deleteEntityRecord(entity, deleteEntities.get(entity));
                logger.info("Entity with entityId id {} is deleted.",deleteEntities.get(entity));
            }
        }
        customAssert.assertAll();
    }

    private int createCDR(CustomAssert customAssert){
        int entityId = -1;
        String createResponse;
        try{
            createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrExtraFieldsConfigFilePath, cdrExtraFieldsConfigFileName, "cdr creation", true);
            if(ParseJsonResponse.validJsonResponse(createResponse)){
                JSONObject jsonObject = new JSONObject(createResponse);
                String createStatus = jsonObject.getJSONObject("header").getJSONObject("response").getString("status").trim();
                if (createStatus.equalsIgnoreCase("success")) {
                    entityId = CreateEntity.getNewEntityId(createResponse, "contract draft request");
                    deleteEntities.put("contract draft request", entityId);
                    logger.info("Id of the Entity Created is : {}", entityId);
                }else{
                    logger.error("CDR creation is unsuccessful.");
                    customAssert.assertTrue(false,"CDR creation is unsuccessful.");
                }
            }else{
                logger.error("Create response of CDR is not a valid JSON Response");
                customAssert.assertTrue(false,"Create response of CDR is not a valid JSON Response");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while creating the CDR",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating the CDR");
        }
        return entityId;
    }

    private int createContractFromCDR(int cdrID, CustomAssert customAssert){
        int contractID = -1;

        try{
            String newPayload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":[],\"entityTypeId\":1},\"actualParentEntity\":{\"entityIds\":[],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":["+cdrID+"],\"entityTypeId\":160}}";
            JSONObject newPayloadJSON = new JSONObject(newPayload);

            ContractFreeCreate contractFreeCreate = new ContractFreeCreate();
            contractFreeCreate.hitContractFreeCreate(cdrID,160,4);
            String freeCreateResponse = contractFreeCreate.getFreeCreateJsonStr();

            if(ParseJsonResponse.validJsonResponse(freeCreateResponse)){
                JSONObject freeCreateJSON = new JSONObject(freeCreateResponse);
                JSONArray dataArray = freeCreateJSON.getJSONObject("body").getJSONObject("data").getJSONObject("supplier").getJSONObject("options").getJSONArray("data");
                for(int index = 0; index<dataArray.length();index++){
                    int sourceId = Integer.parseInt(dataArray.getJSONObject(index).get("id").toString());
                    newPayloadJSON.getJSONObject("parentEntity").append("entityIds",sourceId);
                    newPayloadJSON.getJSONObject("actualParentEntity").append("entityIds",sourceId);
                }
                newPayload = newPayloadJSON.toString();
                New newObj = new New();
                newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
                String newResponse = newObj.getNewJsonStr();
                if(ParseJsonResponse.validJsonResponse(newResponse)){
                    JSONObject newJSON = new JSONObject(newResponse);
                    if(newJSON.getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")){
                        newJSON.remove("header");
                        newJSON.remove("session");
                        newJSON.remove("actions");
                        newJSON.remove("createLinks");
                        newJSON.getJSONObject("body").remove("layoutInfo");
                        newJSON.getJSONObject("body").remove("globalData");
                        newJSON.getJSONObject("body").remove("errors");

                        Set<String> keys = newJSON.getJSONObject("body").getJSONObject("data").keySet();
                        String options = null;
                        for(String key : keys){
                            try{
                                if(!newJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).isNull("options")) {
                                    newJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options", options);
                                }
                            }catch (Exception e){
                                continue;
                            }
                        }

                        JSONObject dynamicMetaData = newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                        Set<String> metaKeys = dynamicMetaData.keySet();
                        for(String metaKey : metaKeys){
                            try{
                                dynamicMetaData.getJSONObject(metaKey).put("options",options);
                                newJSON.getJSONObject("body").put(metaKey,dynamicMetaData.getJSONObject(metaKey));
                            }catch (Exception e){
                                newJSON.getJSONObject("body").put(metaKey,dynamicMetaData.getString(metaKey));
                            }
                        }

                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values","AUTOMATION PURPOSE (PLEASE DO NOT USE)");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("title").put("values","AUTOMATION PURPOSE (PLEASE DO NOT USE)");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("deliveryCountries").append("values",new JSONObject("{\"name\":\"India\",\"id\":111}"));
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("tier").put("values",new JSONObject("{\"name\":\"Tier - 2\",\"id\":1007}"));
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").put("values","10-01-2020");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("expirationDate").put("values","10-31-2020");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("currency").put("values",new JSONObject("{\"name\":\"Indian Rupee (INR)\",\"id\":8,\"shortName\":\"INR\",\"additionalOption\":true}"));
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("contractCurrencies").append("values",new JSONObject("{\"name\":\"Indian Rupee (INR)\",\"id\":8,\"shortName\":\"INR\",\"additionalOption\":true}"));
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardFromDate").put("values","11-01-2018");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardToDate").put("values","11-30-2018");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardsApplicable").put("values",new JSONObject("{\"name\":\"automation conv factor 3\",\"id\":1017}"));
                    }else{
                        logger.error("New API hit is unsuccessful.");
                        customAssert.assertTrue(false,"New API hit is unsuccessful.");
                    }

                    String createPayload = newJSON.toString();
                    if(ParseJsonResponse.validJsonResponse(createPayload)){
                        Create create = new Create();
                        create.hitCreate("contracts",createPayload);
                        String createResponse = create.getCreateJsonStr();
                        if(ParseJsonResponse.validJsonResponse(createResponse)){
                            JSONObject createJSON = new JSONObject(createResponse).getJSONObject("header").getJSONObject("response");
                            if(createJSON.getString("status").equalsIgnoreCase("success")){
                                contractID = Integer.parseInt(createJSON.get("entityId").toString());
                                deleteEntities.put("contracts", contractID);
                            }else{
                                logger.error("Contract creation is unsuccessful.");
                                customAssert.assertTrue(false,"Contract creation is unsuccessful.");
                            }
                        }else{
                            logger.error("MSA Create Response is not a valid JSON");
                            customAssert.assertTrue(false,"MSA Create Response is not a valid JSON");
                        }
                    }else{
                        logger.error("Payload created for Create API is not a valid JSON");
                        customAssert.assertTrue(false,"Payload created for Create API is not a valid JSON");
                    }
                }else{
                    logger.error("New API Response for MSA is not a valid JSON");
                    customAssert.assertTrue(false,"New API Response for MSA is not a valid JSON");
                }
            }else{
                logger.error("Contract Free Create response is not a valid JSON");
                customAssert.assertTrue(false,"Contract Free Create response is not a valid JSON");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while creating MSA from CDR",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating MSA from CDR");
        }
        return contractID;
    }

    private boolean editEntity(String entityName, boolean changeCurrencyToUSD,boolean addAmount, boolean updateRateCard, String rateCardFromDate,String rateCardToDate, int entityId, CustomAssert customAssert){
        boolean flag = false;
        Edit edit = new Edit();
        entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_url_id");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_type_id");
        String options = null;

        try{
            String editGetResponse = edit.getEditPayload(entityName,entityId);
            if(ParseJsonResponse.validJsonResponse(editGetResponse)){
                JSONObject editJSONObject = new JSONObject(editGetResponse);

                //Removing extra data from payload
                Set<String> keys = editJSONObject.getJSONObject("body").getJSONObject("data").keySet();
                for(String key : keys){
                    try{
                        if(!editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject(key).isNull("options")) {
                            editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options", options);
                        }
                    }catch(Exception e){
                        continue;
                    }
                }

                if(entityName.equalsIgnoreCase("contract draft request")){
                    editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject("suppliers").remove("values");
                    editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject("suppliers").append("values",new JSONObject("{\"name\":\"Berkshire Hathaway\",\"id\":1027}"));
                }

                JSONObject dynamicMetaData = editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                Set<String> dynKeys = dynamicMetaData.keySet();
                for(String dynKey : dynKeys){
                    try{
                        if(!dynamicMetaData.getJSONObject(dynKey).isNull("options")) {
                            dynamicMetaData.getJSONObject(dynKey).put("options", options);
                        }
                    }catch(Exception e){

                    }finally {
                        editJSONObject.getJSONObject("body").getJSONObject("data").put(dynKey,dynamicMetaData.getJSONObject(dynKey));
                    }
                }
//Add amount
                if(addAmount) {
                    String automationCurrencyField = ParseConfigFile.getValueFromConfigFile(cdrConfigFilePath, cdrConfigFileName, "custom currency for auto office", entityTypeId);
                    editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(automationCurrencyField).put("values", Integer.parseInt(automationCurrencyAmount));
                    editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject(automationCurrencyField).put("values", Integer.parseInt(automationCurrencyAmount));
                }

//Change INR to USD at Rate card
                if(changeCurrencyToUSD){
                    editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject("currency").put("values",new JSONObject("{\"name\":\"United States Dollar (USD)\",\"id\":1,\"shortName\":\"USD\",\"additionalOption\":true}"));
                }
//adding rate card
                if(updateRateCard) {
                            String updateCardPayload = null;
                            if (entityTypeId.equalsIgnoreCase("160"))
                                updateCardPayload = "{\"body\":{\"data\":{\"rateCard\":{\"values\":{\"name\":\"CDR Rate Card Used in Automation\",\"id\":1019}},\"rateCardFromDate\":{\"values\":\"" + rateCardFromDate + "\"},\"rateCardToDate\":{\"values\":\"" + rateCardToDate + "\"},\"entityId\":{\"values\":" + entityId + "},\"entityTypeId\":{\"values\":" + entityTypeId + "}}}}";
                            else if (entityTypeId.equalsIgnoreCase("61"))
                                updateCardPayload = "{\"body\":{\"data\":{\"rateCard\":{\"values\":{\"name\":\"CDR Rate Card Used in Automation\",\"id\":1019}},\"rateCardFromDate\":{\"values\":\"" + rateCardFromDate + "\"},\"rateCardToDate\":{\"values\":\"" + rateCardToDate + "\"},\"contractId\":{\"values\":" + entityId + "}}}}";

                            UpdateRateCards updateRateCards = new UpdateRateCards();
                            updateRateCards.hitUpdateRateCards(updateCardPayload);
                            String updateResponse = updateRateCards.getUpdateRateCardsJsonStr();
                            JSONObject updateJSON = new JSONObject(updateResponse);
                            JSONArray updateJSONArray = updateJSON.getJSONObject("data").getJSONArray("existingRateCards");
                            editJSONObject.getJSONObject("body").getJSONObject("data").getJSONObject("existingRateCards").append("values", updateJSONArray);
                        }

                String postEditResponse = null;
                try{
                    postEditResponse = edit.hitEdit(entityName, editJSONObject.toString());
                }catch(Exception e){
                    logger.error("Exception {} occurred while saving the entity {} after adding the rate card",e.getMessage(),entityName);
                    customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while saving the entity "+entityName+" after adding the rate card");
                }
                if(ParseJsonResponse.validJsonResponse(postEditResponse)){
                    JSONObject postJSON = new JSONObject(postEditResponse).getJSONObject("header").getJSONObject("response");
                    if(postJSON.getString("status").equalsIgnoreCase("success")){
                        if(postJSON.getJSONObject("properties").getString("redirectUrl").split("/")[3].equalsIgnoreCase(""+entityId)){
                            flag = true;
                        }
                    }
                }else{
                    logger.error("Post Edit Response is not a valid JSON Response");
                    customAssert.assertTrue(false,"Post Edit Response is not a valid JSON Response");
                }
            }else{
                logger.error("Get Response of Edit API of entity {} is not a valid JSON",entityName);
                customAssert.assertTrue(false,"Get Response of Edit API of entity "+entityName+" is not a valid JSON");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while Editing the entity {}",e.getMessage(),entityName);
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while Editing the entity "+entityName);
        }
        return flag;
    }

    private boolean addAmount(String entityName, boolean changeCurrencyToUSD,boolean addAmount, int entityId, CustomAssert customAssert){
        return editEntity(entityName, changeCurrencyToUSD,addAmount,false,"","", entityId, customAssert);
    }

    private HashMap<String, Double> getListingResponse(int entityId, String baseCurrency, String listId, CustomAssert customAssert){
        ListRendererListData listRendererListData = new ListRendererListData();
        String payload = null;
        HashMap<String, Double> listCurrencyDetails = new HashMap<>();
        boolean flag = false;
        String automationCurrencyField = null;
        if(listId.equalsIgnoreCase("2")){
            automationCurrencyField = ParseConfigFile.getValueFromConfigFile(cdrConfigFilePath,cdrConfigFileName,"custom currency for auto office","61");
            payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"1001715\":{\"filterId\":\"1001715\",\"filterName\":\""+automationCurrencyField.replaceAll("[^0-9]", "")+"\",\"entityFieldId\":106744,\"entityFieldHtmlType\":19,\"min\":\"1\",\"suffix\":null}}},\"selectedColumns\":[{\"columnId\":11751,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":17,\"columnQueryName\":\"id\"},{\"columnId\":17072,\"columnQueryName\":\"currency\"},{\"columnId\":110394,\"columnQueryName\":\""+automationCurrencyField+"\"}]}";
        }else if(listId.equalsIgnoreCase("279")){
            automationCurrencyField = ParseConfigFile.getValueFromConfigFile(cdrConfigFilePath,cdrConfigFileName,"custom currency for auto office","160");
            payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"1001355\":{\"filterId\":\"1001355\",\"filterName\":\""+automationCurrencyField.replaceAll("[^0-9]", "")+"\",\"entityFieldId\":106647,\"entityFieldHtmlType\":19,\"min\":\"1\",\"suffix\":null}}},\"selectedColumns\":[{\"columnId\":12259,\"columnQueryName\":\"id\"},{\"columnId\":17090,\"columnQueryName\":\"currency\"},{\"columnId\":109685,\"columnQueryName\":\""+automationCurrencyField+"\"}]}";
        }
        try{
            String listResponse = listRendererListData.hitListRendV2(Integer.parseInt(listId), payload);
            if(ParseJsonResponse.validJsonResponse(listResponse)){
                JSONArray listDataJsonArray = new JSONObject(listResponse).getJSONArray("data");
                int tempIndex = 0;
                Set<String> keys = null;
                for(int index = 0; index<listDataJsonArray.length();index++){
                    keys = listDataJsonArray.getJSONObject(index).keySet();
                    String id = null;
                    String currency = null;
                    for(String key : keys){
                        String columnName = listDataJsonArray.getJSONObject(index).getJSONObject(key).getString("columnName");
                        if(columnName.equalsIgnoreCase("id")){
                            id = listDataJsonArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[1];
                        }else if(columnName.equalsIgnoreCase("currency")){
                            currency = listDataJsonArray.getJSONObject(index).getJSONObject(key).getString("value");
                        }else
                            continue;

                        try{
                            if(Integer.parseInt(id)== entityId & currency.equalsIgnoreCase(baseCurrency)){
                                flag = true;
                                tempIndex = index;
                                break;
                            }
                        }catch (Exception e){
                            continue;
                        }
                    }
                    if(flag)
                        break;
                }

                keys = listDataJsonArray.getJSONObject(tempIndex).keySet();
                double currencyAmount = 0.00;
                for(String key : keys){
                    String columnName = listDataJsonArray.getJSONObject(tempIndex).getJSONObject(key).getString("columnName");
                    if(columnName.equalsIgnoreCase(automationCurrencyField) ) {
                        try {
                            currencyAmount = Double.parseDouble(listDataJsonArray.getJSONObject(tempIndex).getJSONObject(key).getString("value"));
                            listCurrencyDetails.put("CurrencyAmount", currencyAmount);
                            break;
                        }catch (Exception e){
                            continue;
                        }
                    }
                }
            }else{
                logger.error("Listing Response is not a valid JSON");
                customAssert.assertTrue(false,"Listing response is not a valid JSON");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while getting the listing response data.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while getting the listing response data.");
        }
        return listCurrencyDetails;
    }

    private HashMap<String, Object> getShowPageData(int showPageId, String entityName, CustomAssert customAssert) {
        entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_url_id");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_type_id");

        Show show = new Show();
        HashMap<String,Object> showPageData = new HashMap<>();
        try{
            show.hitShow(Integer.parseInt(entityTypeId), showPageId, true);
            String showPageResponse = show.getShowJsonStr();
            if(ParseJsonResponse.validJsonResponse(showPageResponse)){
                JSONObject showJSON = new JSONObject(showPageResponse);
                if(showJSON.getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")){
                    showJSON = showJSON.getJSONObject("body").getJSONObject("data");
                    showPageData.put("ConversionFactor",new CurrencyConversionHelper().getConvFacWRTClient(Integer.parseInt(entityTypeId),showPageId,showPageId,customAssert));
                    double inputCustomCurrency = 0;
                    double convertedCustomCurrency = 0;
                    String dynamicCurrencyField = ParseConfigFile.getValueFromConfigFile(cdrConfigFilePath,cdrConfigFileName,"custom currency for auto office",entityTypeId);
                    try{
                        String displayValueOfCustomCurrency = showJSON.getJSONObject("dynamicMetadata").getJSONObject(dynamicCurrencyField).getString("displayValues");
                        String inputCurrencyAmount = displayValueOfCustomCurrency.split("USD")[0].replaceAll("[^0-9]","");
                        String convertedCurrencyAmount = displayValueOfCustomCurrency.split("USD")[1].replaceAll("[^.0-9]","");
                        inputCustomCurrency=Double.parseDouble(inputCurrencyAmount);
                        convertedCustomCurrency = Double.parseDouble(convertedCurrencyAmount);
                    }catch(Exception ex){
                        inputCustomCurrency = 0.0;
                        convertedCustomCurrency = 0.0;
                    }
                    showPageData.put("CustomCurrencyInput",inputCustomCurrency);
                    showPageData.put("CustomCurrencyOutput",convertedCustomCurrency);
                }else{
                    logger.error("Show Page response is not a valid JSON");
                    customAssert.assertTrue(false,"Show Page response is not a valid JSON");
                }
                String expirationDate = showJSON.getJSONObject("expirationDate").getString("values");
                showPageData.put("ExpirationDate",expirationDate);
                String name = showJSON.getJSONObject("name").getString("values");
                showPageData.put("Name",name);
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while fetching Show page response",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while fetching Show page response");
        }
        return showPageData;
    }

    private boolean downloadListingData(String entityName, String clientBaseCurrency, CustomAssert customAssert){
        entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_url_id");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_type_id");
        DownloadListWithData downloadListWithData = new DownloadListWithData();
        HashMap<String, String> formParam = new HashMap<>();
        boolean flag = false;
        String outputPath = "src/test/java/com/sirionlabs/test/pod/ListingData/";
        String outputFileName = entityName +" - ListData.xlsx";
        try{
            formParam.put("_csrf_token","null");
            formParam.put("jsonData","{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1656\",\"name\":\"Active\"}]},\"filterId\":6,\"filterName\":\"status\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}}");

            HttpResponse downloadResponse =  downloadListWithData.hitDownloadListWithData( formParam, Integer.parseInt(entityURLId));
            Thread.sleep(5000);

            if(downloadListWithData.dumpDownloadListIntoFile(downloadResponse,outputPath,outputFileName)){
//            if(new FileUtils().writeResponseIntoFile(downloadResponse, outputPath + outputFileName)){
                flag = readExcelFile(clientBaseCurrency, outputPath, outputFileName, customAssert);
            }else{
                logger.error("List data could not be downloaded");
                customAssert.assertTrue(false,"List data could not be downloaded");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while downloading the list data.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while downloading the list data.");
        }
        return flag;
    }

    private boolean readExcelFile(String clientBaseCurrency, String excelPath, String excelFileName, CustomAssert customAssert) {
        boolean flagCurrency = false;
        boolean flagClientCurrency = false;
        boolean columnStatus = false;
        try {
            File file = new File(excelPath + excelFileName);
            FileInputStream fis = new FileInputStream(file);
            ZipSecureFile.setMinInflateRatio(-1.0d);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet("Data");
            int totalRows = sheet.getLastRowNum();
            int totalCols = sheet.getRow(0).getLastCellNum();
            DataFormatter dataFormatter = new DataFormatter();

            Object[][] dataOfExcelSheet = new Object[totalRows][totalCols];
            int rowId = 0;

            for (int rowIndex = rowId; rowIndex < totalRows - 1; rowIndex++) {
                for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                    try {
                        Cell cell = sheet.getRow(rowIndex).getCell(colIndex);
                        dataOfExcelSheet[rowIndex][colIndex] = dataFormatter.formatCellValue(cell);
                    } catch (Exception ex) {
                        dataOfExcelSheet[rowIndex][colIndex] = null;
                    }
                }
            }

            for (int index = 0; index < totalRows - 1; index++) {
                try {
                    if (dataOfExcelSheet[index][0].toString().equalsIgnoreCase("id")) {
                        rowId = index;
                        break;
                    }
                } catch (Exception ex) {
                    continue;
                }
            }

            for (int rowIndex = rowId; rowIndex < totalRows - 1; rowIndex++) {
                for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                    String value = dataOfExcelSheet[rowIndex][colIndex].toString();
                    if (value.equalsIgnoreCase("AUTOMATION CURRENCY")) {
                        flagCurrency = true;
                    }else if (value.equalsIgnoreCase("AUTOMATION CURRENCY ("+clientBaseCurrency+")")) {
                        flagClientCurrency = true;
                    }
                }
                if (flagCurrency & flagClientCurrency) {
                    columnStatus = true;
                    break;
                }else if (flagCurrency & !flagClientCurrency) {
                    columnStatus = false;
                    logger.error("No column for Client Currency is available in the listing excel sheet.");
                    customAssert.assertTrue(false,"No column for Client Currency is available in the listing excel sheet.");
                    break;
                }else if (!flagCurrency & flagClientCurrency) {
                    columnStatus = false;
                    logger.error("No column for Custom Currency is available in the listing excel sheet.");
                    customAssert.assertTrue(false,"No column for Custom Currency is available in the listing excel sheet.");
                    break;
                }else if (!flagCurrency & !flagClientCurrency) {
                    columnStatus = false;
                    logger.error("No column for Custom Currency or Client Currency is available in the listing excel sheet.");
                    customAssert.assertTrue(false,"No column for Custom Currency or Client Currency is available in the listing excel sheet.");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while reading the excel file {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while reading the excel file " + e.getMessage());
        }
        return columnStatus;
    }

    public int createCDRFromContract(int contractId, String contractName, CustomAssert customAssert){
        int cdrId = -1;
        try{

            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, "cdr creation from contract for multi-supplier", "sourcename", null, contractName);
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, "cdr creation from contract for multi-supplier", "sourceid", null, ""+contractId);

            String createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrExtraFieldsConfigFilePath, cdrExtraFieldsConfigFileName, "cdr creation from contract for multi-supplier", true);
            //check suppliers in here
            if(ParseJsonResponse.validJsonResponse(createResponse)){
                JSONObject createJSON = new JSONObject(createResponse).getJSONObject("header").getJSONObject("response");
                if(createJSON.getString("status").equalsIgnoreCase("success")){
                    cdrId = Integer.parseInt(createJSON.get("entityId").toString());
                }else{
                    logger.error("CDR creation from Contract is unsuccessful.");
                    customAssert.assertTrue(false,"CDR creation from Contract is unsuccessful.");
                }
            }else {
                logger.error("Create Response is not a valid JSON.");
                customAssert.assertTrue(false, "Create Response is not a valid JSON.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while creating CDR from Contract.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating CDR from Contract.");
        }
        return cdrId;
    }
}