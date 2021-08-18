package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestMultiSupplierContractCreationFromCDR{
    private final static Logger logger = LoggerFactory.getLogger(TestMultiSupplierContractCreationFromCDR.class);

    private String configFilePath;
    private String configFileName;
    private String entityTypeId;
    private static String entityName;
    private String client_id;
    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String extraFieldsConfigFilePath;
    private String extraFieldsConfigFileName;
    private Map<String, String> defaultProperties;
    private List <String> deleteEntity = new ArrayList<>();
    private List<String> inputMultiSuppliers = new ArrayList<>();

    @BeforeClass
    public void beforeClass(){
        client_id = ConfigureEnvironment.getEnvironmentProperty("client_id");

        entityName = "contract draft request";
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_type_id");

        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFileName");
        defaultProperties = ParseConfigFile.getAllProperties(cdrConfigFilePath, cdrConfigFileName);

        extraFieldsConfigFilePath = defaultProperties.get("extrafieldsconfigfilepath");
        extraFieldsConfigFileName = defaultProperties.get("extrafieldsconfigfilename");

        inputMultiSuppliers.add("{\"name\":\"Berkshire Hathaway\",\"id\":1027}");
        inputMultiSuppliers.add("{\"name\":\"CMH Hodgenville Inc\",\"id\":1034}");
    }

    @Test
    public void testC90454AndC90456(){
        CustomAssert customAssert = new CustomAssert();
        List<String> inputSupplierNames = new ArrayList<>();
        int cdrId = -1;
        int contractId = -1;
        try{
            if(getMultiPartyContractCheck(customAssert)){
                cdrId = createCDR(customAssert);
                deleteEntity.add(entityName+"-"+cdrId);

                HashMap<String,List<String>> showPageData = getShowPageResponse(cdrId, entityName, customAssert);
                List<String> supplierListOnShowPage = showPageData.get("Suppliers");
                List<String> contractSupplierListOnShowPage = showPageData.get("ContractSuppliers");

                if(supplierListOnShowPage.size()>1 & contractSupplierListOnShowPage.size()>1){
                    logger.info("Multiple suppliers i.e. {} suppliers are present in the CDR.",supplierListOnShowPage.size());
                }else{
                    logger.info("No supplier is present in the CDR.");
                    logger.info("Adding the suppliers");
                    if(editTheCDR(cdrId, customAssert)){
                        showPageData = getShowPageResponse(cdrId, entityName, customAssert);
                        supplierListOnShowPage = showPageData.get("Suppliers");
                        contractSupplierListOnShowPage = showPageData.get("ContractSuppliers");
                    }else{
                        logger.error("CDR could not be edited.");
                        customAssert.assertTrue(false,"CDR could not be edited.");
                    }
                }

                inputSupplierNames = supplierListOnShowPage;
                if(supplierListOnShowPage.equals(contractSupplierListOnShowPage)){
                    logger.info("Contract Suppliers are the same as chosen suppliers in CDR Basic Information.");
                    //C90456 passed.
                    contractId = createMSAContractFromCDR(cdrId,customAssert);

                    if(contractId>0){
                        //C90457 passed.
                        deleteEntity.add("contracts -"+contractId);
                        logger.info("Contract MSA is created successfully with multiple suppliers.");
                        HashMap<String,List<String>> dataOnShowPage = getShowPageResponse(contractId, "contracts", customAssert);
                        String contractName = dataOnShowPage.get("Name").get(0);
                        try{
                            int newCDRId = createCDRFromMSAContract(contractId, contractName, customAssert);
                            if(newCDRId>0) {
                                //C90458 passed.
                                deleteEntity.add(entityName + " - " + newCDRId);
                                logger.info("CDR is created from the Contract {} which was created from the CDR {}", contractId, newCDRId);

                                showPageData = getShowPageResponse(newCDRId, entityName, customAssert);
                                supplierListOnShowPage = showPageData.get("Suppliers");
                                contractSupplierListOnShowPage = showPageData.get("ContractSuppliers");
                                if(supplierListOnShowPage.equals(inputSupplierNames) & supplierListOnShowPage.equals(contractSupplierListOnShowPage)){
                                    logger.info("CDR generated has same suppliers which are present in Contract.");
                                    //C90460 passed.
                                }else{
                                    logger.error("CDR generated does not has same suppliers which are present in Contract.");
                                    customAssert.assertTrue(false,"CDR generated does not has same suppliers which are present in Contract.");
                                    //C90460 failed.
                                }
                            }else{
                                //C90458 failed.
                                logger.error("CDR is not created from the Contract {} which was created from the CDR {}",contractId,cdrId);
                                customAssert.assertTrue(false,"CDR is not created from the Contract "+contractId+" which was created from the CDR "+cdrId);
                            }
                        }catch (Exception e){
                            logger.error("Exception {} occurred while creating CDR from Contract.",e.getMessage());
                            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating CDR from Contract.");
                        }

                    }else{
                        ////C90457 failed.
                        logger.error("Contract MSA is not created with multiple suppliers.");
                        customAssert.assertTrue(false,"Contract MSA is not created with multiple suppliers.");
                    }

                }else{
                    //C90456 failed.
                    logger.error("Contract Suppliers are different then the chosen suppliers in CDR Basic Information.");
                    customAssert.assertTrue(false,"Contract Suppliers are different then the chosen suppliers in CDR Basic Information.");
                }
            }else{
                logger.error("Multi-Party Contracts is unchecked for Client with ID {}",client_id);
                customAssert.assertTrue(false,"Multi-Party Contracts is unchecked for Client with ID "+client_id);
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while verifying CDR creation with Multiple Suppliers.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while verifying CDR creation with Multiple Suppliers.");
        }finally {
            for(String entity : deleteEntity) {
                try {
                    EntityOperationsHelper.deleteEntityRecord(entity.split("-")[0].trim(), Integer.parseInt(entity.split("-")[1].trim()));
                    logger.info("Entity {} with entityId id {} is deleted.", entity.split("-")[0].trim(), Integer.parseInt(entity.split("-")[1].trim()));
                }catch (Exception e){
                    continue;
                }
            }
        }
        customAssert.assertAll();
    }

    public boolean getMultiPartyContractCheck(CustomAssert customAssert){
        boolean multiPartyContractChecked = false;
        String roleIds = null;
        PostgreSQLJDBC sqlObj = null;
        try{
            sqlObj = new PostgreSQLJDBC();
            String query = "select accessible_roles from client_provisioning_data where client_id = "+client_id+";";
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                roleIds = results.get(0).get(0);
            }
            String[] roleIdArray = roleIds.split(",");
            for(String roleId : roleIdArray){
                if(roleId.equalsIgnoreCase("10")) {
                    multiPartyContractChecked = true;
                    break;
                }
                else
                    continue;
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while establishing a connection with the DataBase.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while establishing a connection with the DataBase.");
        }finally {
            sqlObj.closeConnection();
        }
        return multiPartyContractChecked;
    }

    public int createCDR(CustomAssert customAssert){
        int entityId = -1;
        String createResponse;
        try{
            createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "cdr creation for no supplier", true);
            if(ParseJsonResponse.validJsonResponse(createResponse)){
                JSONObject jsonObject = new JSONObject(createResponse);
                String createStatus = jsonObject.getJSONObject("header").getJSONObject("response").getString("status").trim();
                if (createStatus.equalsIgnoreCase("success"))
                    entityId = CreateEntity.getNewEntityId(createResponse, entityName);
                logger.info("Id of the Entity Created is : {}",entityId);
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

    public HashMap<String, List<String>> getShowPageResponse(int entityId, String entity, CustomAssert customAssert){
        Show show = new Show();
        HashMap<String,List<String>> dataOnShowPage = new HashMap<>();
        List<String> name = new ArrayList<>();
        List<String> supplierListOnShowPage = new ArrayList<>();
        List<String> contractSupplierListOnShowPage = new ArrayList<>();
        try{
            if(entity.equalsIgnoreCase(entityName)) {
                show.hitShow(Integer.parseInt(entityTypeId), entityId, true);
            }else if(entity.equalsIgnoreCase("contracts")){
                show.hitShow(61, entityId, true);
            }
            String showPageResponse = show.getShowJsonStr();
            if(ParseJsonResponse.validJsonResponse(showPageResponse)){
                JSONObject showPageJSON = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");

                name.add(showPageJSON.getJSONObject("name").getString("values"));
                dataOnShowPage.put("Name",name);

                try{
                    JSONArray supplierArray = showPageJSON.getJSONObject("suppliers").getJSONArray("values");
                    for(int index=0; index<supplierArray.length();index++){
                        supplierListOnShowPage.add(supplierArray.getJSONObject(index).getString("name"));
                    }
                }catch (Exception e){
                    supplierListOnShowPage.add(null);
                }
                dataOnShowPage.put("Suppliers",supplierListOnShowPage);

                try{
                    JSONArray contractSupplierArray = showPageJSON.getJSONObject("contractSupplier").getJSONObject("options").getJSONArray("data");
                    for(int index=0; index<contractSupplierArray.length();index++){
                        contractSupplierListOnShowPage.add(contractSupplierArray.getJSONObject(index).getString("name"));
                    }
                }catch (Exception e){
                    contractSupplierListOnShowPage.add(null);
                }
                dataOnShowPage.put("ContractSuppliers",contractSupplierListOnShowPage);
            }else{
                logger.error("Show page response is not a valid JSON.");
                customAssert.assertTrue(false,"Show page response is not a valid JSON.");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while getting the show page response.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while getting the show page response.");
        }
        return dataOnShowPage;
    }

    public boolean editTheCDR(int entityId, CustomAssert customAssert){
        Edit edit = new Edit();
        String editGetResponse = null;
        boolean editFlag = false;
        try{
            editGetResponse = edit.getEditPayload(entityName,entityId);
            if(ParseJsonResponse.validJsonResponse(editGetResponse)){
                JSONObject editGetJSON = new JSONObject(editGetResponse);
                Set<String> keys = editGetJSON.getJSONObject("body").getJSONObject("data").keySet();
                for(String key : keys){
                    try{
                        String options = null;
                        //add if-else
                        editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).remove("options");
                        editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options",options);
                    }catch(Exception e){
                        continue;
                    }
                }

                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("suppliers").remove("values");

                for(String supplier : inputMultiSuppliers){
                    editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("suppliers").append("values",new JSONObject(supplier));
                }

                String editPostResponse = edit.hitEdit(entityName, editGetJSON.toString());
                if(ParseJsonResponse.validJsonResponse(editPostResponse)){
                    JSONObject editPostJSON = new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response");
                    if(editPostJSON.getString("status").equalsIgnoreCase("success")){
                        editFlag = true;
                    }
                }else{
                    logger.error("Edit POST Response is not a valid JSON.");
                    customAssert.assertTrue(false,"Edit POST Response is not a valid JSON.");
                }
            }else{
                logger.error("Edit GET Response is not a valid JSON.");
                customAssert.assertTrue(false,"Edit GET Response is not a valid JSON.");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while editing the CDR.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while editing the CDR.");
        }
        return editFlag;
    }

    public int createMSAContractFromCDR(int cdrID, CustomAssert customAssert){
        int contractID = -1;

        try{
            String newPayload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":[],\"entityTypeId\":1},\"actualParentEntity\":{\"entityIds\":[],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":["+cdrID+"],\"entityTypeId\":160}}";
            JSONObject newPayloadJSON = new JSONObject(newPayload);

            ContractFreeCreate contractFreeCreate = new ContractFreeCreate();
            contractFreeCreate.hitContractFreeCreate(cdrID,Integer.parseInt(entityTypeId),4);
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
                                newJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).remove("options");
                                newJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options",options);
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
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardFromDate").put("values","11-01-2020");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardToDate").put("values","11-30-2020");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardsApplicable").put("values",new JSONObject("{\"name\":\"CDR Rate Card Used in Automation\",\"id\":1019}"));
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

    public int createCDRFromMSAContract(int contractId, String contractName, CustomAssert customAssert){
        int cdrId = -1;
        try{

            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, "cdr creation from contract for multi-supplier", "sourcename", null, contractName);
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, "cdr creation from contract for multi-supplier", "sourceid", null, ""+contractId);

            String createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "cdr creation from contract for multi-supplier", true);
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