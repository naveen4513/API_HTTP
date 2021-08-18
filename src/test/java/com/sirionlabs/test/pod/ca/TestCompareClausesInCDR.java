package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.DownloadContractTemplateInCDR;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.TemplateSuggestion;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Set;

public class TestCompareClausesInCDR {
    private final static Logger logger = LoggerFactory.getLogger(TestCompareClausesInCDR.class);
    private String entityName = "contract draft request";
    private String configFilePath;
    private String configFileName;
    private int entityTypeId;

    @BeforeClass
    public void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_type_id"));
    }

   //Compare Clauses In CDR
//   @Test
    public void testC152893(){
       CustomAssert customAssert = new CustomAssert();
       int entityId = -1;
       try{
           entityId = createNewCDR(customAssert);
           HashMap<String, String> templateDetails = selectTemplateAndChooseClause(entityId,customAssert);
           if(templateDetails!=null){
               if(addTemplateToCDR(entityId,templateDetails,customAssert)){
                   if(downloadSystemTagDocumentContractDocumentTab(entityId,customAssert)){
                       System.out.println("Pass");
                   }
               }
           }
       }catch(Exception e){
        }finally {
           EntityOperationsHelper.deleteEntityRecord(entityName, entityId);
           logger.info("CDR with entityId id {} is deleted.",entityId);
       }
       customAssert.assertAll();
    }

    public int createNewCDR(CustomAssert customAssert){
       int entityId = -1;
       String createResponse;
       String payloadCDR = "{\"body\":{\"data\":{\"parentShortCodeId\":{\"name\":\"parentShortCodeId\",\"values\":{\"name\":\"PRS01005\",\"id\":1005},\"multiEntitySupport\":false},\"clauseAssembly\":{\"name\":\"clauseAssembly\",\"values\":false,\"multiEntitySupport\":false},\"functions\":{\"name\":\"functions\",\"id\":11551,\"values\":[{\"name\":\"Human Resources\",\"id\":1003}],\"options\":null,\"multiEntitySupport\":false},\"integrationSystem\":{\"name\":\"integrationSystem\",\"multiEntitySupport\":false},\"parentEntityId\":{\"name\":\"parentEntityId\",\"values\":1005,\"multiEntitySupport\":false},\"globalRegions\":{\"name\":\"globalRegions\",\"id\":11013,\"values\":[{\"name\":\"APAC\",\"id\":1002,\"parentId\":1002},{\"name\":\"EMEA\",\"id\":1003,\"parentId\":1002}],\"options\":null,\"multiEntitySupport\":false},\"globalCountries\":{\"name\":\"globalCountries\",\"id\":11031,\"values\":[{\"name\":\"Australia\",\"id\":15,\"parentId\":1002},{\"name\":\"Bulgaria\",\"id\":37,\"parentId\":1003}],\"options\":null,\"multiEntitySupport\":false},\"years\":{\"name\":\"years\",\"id\":11018,\"multiEntitySupport\":false},\"documentEditable\":{\"name\":\"documentEditable\",\"multiEntitySupport\":false},\"noTouchContract\":{\"name\":\"noTouchContract\",\"id\":12461,\"values\":false,\"multiEntitySupport\":false},\"supplierAccess\":{\"name\":\"supplierAccess\",\"id\":12379,\"values\":false,\"multiEntitySupport\":false},\"documentMovementStatus\":{\"name\":\"documentMovementStatus\",\"id\":12303,\"multiEntitySupport\":false},\"rateCardForRest\":{\"name\":\"rateCardForRest\",\"multiEntitySupport\":false},\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":true,\"multiEntitySupport\":false},\"supplier\":{\"name\":\"supplier\",\"multiEntitySupport\":false},\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"id\":{\"name\":\"id\",\"id\":11037,\"multiEntitySupport\":false},\"parentEntityIds\":{\"name\":\"parentEntityIds\",\"multiEntitySupport\":false},\"state\":{\"name\":\"state\",\"id\":11845,\"values\":{},\"options\":null,\"multiEntitySupport\":false},\"templateStructure\":{\"name\":\"templateStructure\",\"id\":12663,\"multiEntitySupport\":false},\"contractingHubs\":{\"name\":\"contractingHubs\",\"id\":12350,\"values\":[],\"multiEntitySupport\":false},\"paperType\":{\"name\":\"paperType\",\"id\":11029,\"options\":null,\"multiEntitySupport\":false},\"cycleTime\":{\"name\":\"cycleTime\",\"multiEntitySupport\":false},\"clientCurrency\":{\"name\":\"clientCurrency\",\"values\":{\"name\":\"Indian Rupee (INR)\",\"id\":8,\"shortName\":\"INR\",\"active\":false,\"blocked\":false,\"createdFromListPage\":false,\"summaryGroupData\":false,\"bulkOperation\":false,\"blockedForBulk\":false,\"autoExtracted\":false,\"dynamicFieldsEncrypted\":false,\"systemAdmin\":false,\"canOverdue\":false,\"autoCreate\":false,\"draftEntity\":false,\"validationError\":false,\"isReject\":false,\"parentHalting\":false,\"autoTaskFailed\":false,\"compareHistory\":false,\"flagForClone\":false,\"createStakeHolder\":false,\"escapeValueUpdateTask\":false,\"excludeFromHoliday\":false,\"excludeWeekends\":false,\"datetimeEnabled\":false,\"uploadAllowed\":false,\"downloadAllowed\":false,\"signatureAllowed\":false,\"saveCommentDocOnValueUpdate\":false,\"overdue\":false,\"autoTask\":false},\"multiEntitySupport\":false},\"templates\":{\"name\":\"templates\",\"id\":12780,\"values\":[],\"multiEntitySupport\":false},\"stagingPrimaryKey\":{\"name\":\"stagingPrimaryKey\",\"multiEntitySupport\":false},\"timeZone\":{\"name\":\"timeZone\",\"id\":11035,\"values\":{\"name\":\"Europe/London (GMT +00:00)\",\"id\":40,\"timeZone\":\"Europe/London\"},\"options\":null,\"multiEntitySupport\":false},\"rateCardFromDate\":{\"name\":\"rateCardFromDate\",\"id\":12574,\"multiEntitySupport\":false},\"priority\":{\"name\":\"priority\",\"id\":11030,\"options\":null,\"multiEntitySupport\":false},\"parentLinkId\":{\"name\":\"parentLinkId\",\"multiEntitySupport\":false},\"leadTimes\":{\"name\":\"leadTimes\",\"multiEntitySupport\":false},\"clientHoliday\":{\"name\":\"clientHoliday\",\"multiEntitySupport\":false},\"deviationPercentage\":{\"name\":\"deviationPercentage\",\"id\":12582,\"multiEntitySupport\":false},\"projectLevels\":{\"name\":\"projectLevels\",\"multiEntitySupport\":false},\"contractDraftRequestType\":{\"name\":\"contractDraftRequestType\",\"id\":11603,\"multiEntitySupport\":false},\"expectedCompletionDate\":{\"name\":\"expectedCompletionDate\",\"id\":11239,\"multiEntitySupport\":false},\"startDate\":{\"name\":\"startDate\",\"id\":11237,\"multiEntitySupport\":false},\"status\":{\"name\":\"status\",\"id\":11038,\"values\":{},\"multiEntitySupport\":false},\"contractingMarkets\":{\"name\":\"contractingMarkets\",\"id\":12351,\"values\":[],\"multiEntitySupport\":false},\"termType\":{\"name\":\"termType\",\"id\":11021,\"options\":null,\"multiEntitySupport\":false},\"clientAcv\":{\"name\":\"clientAcv\",\"multiEntitySupport\":false},\"contractParentEntityType\":{\"name\":\"contractParentEntityType\",\"id\":12329,\"options\":null,\"multiEntitySupport\":false},\"recipientClientEntities\":{\"name\":\"recipientClientEntities\",\"id\":12356,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"supplierRatingEnabled\":{\"name\":\"supplierRatingEnabled\",\"values\":false,\"multiEntitySupport\":false},\"vendorContractingParty\":{\"name\":\"vendorContractingParty\",\"id\":12358,\"values\":[{\"name\":\"ABC News\",\"id\":1025,\"attributes\":{\"sapSourcingId\":\"ECPSAPSIDBloom\",\"id\":\"ECP01002\",\"eccId\":\"ECPECCIDBloom\"}}],\"options\":null,\"multiEntitySupport\":false},\"currency\":{\"name\":\"currency\",\"id\":11235,\"values\":{\"name\":\"Indian Rupee (INR)\",\"id\":8,\"shortName\":\"INR\"},\"options\":null,\"multiEntitySupport\":false},\"businessLines\":{\"name\":\"businessLines\",\"id\":11004,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"createdFor\":{\"name\":\"createdFor\",\"id\":11668,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"actualParentEntityId\":{\"name\":\"actualParentEntityId\",\"multiEntitySupport\":false},\"recipientMarkets\":{\"name\":\"recipientMarkets\",\"id\":12355,\"values\":[],\"multiEntitySupport\":false},\"services\":{\"name\":\"services\",\"id\":11552,\"values\":[{\"name\":\"Projects\",\"id\":1006,\"parentId\":1003},{\"name\":\"Service Desk\",\"id\":1007,\"parentId\":1003},{\"name\":\"End-User Computing\",\"id\":1008,\"parentId\":1003}],\"options\":null,\"multiEntitySupport\":false},\"parentEntityTypeId\":{\"name\":\"parentEntityTypeId\",\"values\":183,\"multiEntitySupport\":false},\"businessUnits\":{\"name\":\"businessUnits\",\"id\":11003,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"mappedContractTemplates\":{\"name\":\"mappedContractTemplates\",\"multiEntitySupport\":false},\"oldSystemId\":{\"name\":\"oldSystemId\",\"multiEntitySupport\":false},\"days\":{\"name\":\"days\",\"id\":11020,\"multiEntitySupport\":false},\"totalMissingClauses\":{\"name\":\"totalMissingClauses\",\"id\":12664,\"multiEntitySupport\":false},\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":160,\"multiEntitySupport\":false},\"relations\":{\"name\":\"relations\",\"multiEntitySupport\":false},\"contractRegions\":{\"name\":\"contractRegions\",\"multiEntitySupport\":false},\"effectiveDate\":{\"name\":\"effectiveDate\",\"id\":11017,\"values\":\"12-31-2019\",\"displayValues\":\"Dec-31-2019\",\"multiEntitySupport\":false},\"agreementType\":{\"name\":\"agreementType\",\"id\":11602,\"options\":null,\"multiEntitySupport\":false},\"suppliers\":{\"name\":\"suppliers\",\"id\":11601,\"values\":[{\"name\":\"Bloomberg\",\"id\":1025},{\"name\":\"Berkshire Hathaway\",\"id\":1027}],\"options\":null,\"multiEntitySupport\":false},\"transactionTypes\":{\"name\":\"transactionTypes\",\"id\":11016,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"contractingClientEntities\":{\"name\":\"contractingClientEntities\",\"id\":12352,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"recipientHubs\":{\"name\":\"recipientHubs\",\"id\":12354,\"values\":[],\"multiEntitySupport\":false},\"serviceCategory\":{\"name\":\"serviceCategory\",\"multiEntitySupport\":false},\"counterPartyAddress\":{\"name\":\"counterPartyAddress\",\"id\":11015,\"values\":\"\",\"multiEntitySupport\":false},\"stakeHolders\":{\"name\":\"stakeHolders\",\"values\":{\"rg_2018\":{\"values\":[{\"name\":\"Anay User\",\"id\":1044,\"type\":2,\"email\":\"srijan.samanta@sirionqa.office\",\"properties\":{\"Contact Number\":\"987654321\",\"Designation\":\"QA\",\"Default Tier\":\"View All\",\"Email\":\"srijan.samanta@sirionqa.office\",\"First Name\":\"Anay\",\"Time Zone:\":\"Asia/Kolkata (GMT +05:30)\",\"User Department\":\" - \",\"Legal Document\":\"Yes\",\"Last Name\":\"User\",\"Exclude From Filters\":\" - \",\"Business Case\":\"Yes\",\"Financial Document\":\"Yes\"}}],\"name\":\"rg_2018\",\"label\":\"Contract Draft Request Manager\",\"userType\":[2,1,3,4]}},\"options\":null,\"multiEntitySupport\":false},\"canSupplierBeParent\":true,\"acv\":{\"name\":\"acv\",\"id\":11023,\"displayValues\":\"\",\"multiEntitySupport\":false},\"actualParentEntityTypeId\":{\"name\":\"actualParentEntityTypeId\",\"multiEntitySupport\":false},\"clientTcv\":{\"name\":\"clientTcv\",\"multiEntitySupport\":false},\"addRequest\":{\"name\":\"addRequest\",\"values\":false,\"multiEntitySupport\":false},\"industryTypes\":{\"name\":\"industryTypes\",\"id\":11014,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"months\":{\"name\":\"months\",\"id\":11019,\"multiEntitySupport\":false},\"parentHalting\":{\"name\":\"parentHalting\",\"values\":false,\"multiEntitySupport\":false},\"weekType\":{\"name\":\"weekType\",\"id\":12432,\"options\":null,\"multiEntitySupport\":false},\"sourceTitle\":{\"name\":\"sourceTitle\",\"id\":12424,\"values\":{\"url\":\"null/null\"},\"multiEntitySupport\":false,\"options\":null},\"dynamicMetadata\":{\"dyn106561\":{\"name\":\"dyn106561\",\"id\":106561,\"multiEntitySupport\":false},\"dyn106680\":{\"name\":\"dyn106680\",\"id\":106680,\"options\":null,\"multiEntitySupport\":false},\"dyn103384\":{\"name\":\"dyn103384\",\"id\":103384,\"multiEntitySupport\":false},\"dyn106620\":{\"name\":\"dyn106620\",\"id\":106620,\"multiEntitySupport\":false},\"dyn106597\":{\"name\":\"dyn106597\",\"id\":106597,\"multiEntitySupport\":false},\"dyn106663\":{\"name\":\"dyn106663\",\"id\":106663,\"options\":null,\"multiEntitySupport\":false},\"dyn106701\":{\"name\":\"dyn106701\",\"id\":106701,\"multiEntitySupport\":false},\"dyn103378\":{\"name\":\"dyn103378\",\"id\":103378,\"multiEntitySupport\":false},\"dyn106647\":{\"name\":\"dyn106647\",\"id\":106647,\"multiEntitySupport\":false},\"dyn106578\":{\"name\":\"dyn106578\",\"id\":106578,\"multiEntitySupport\":false},\"dyn106700\":{\"name\":\"dyn106700\",\"id\":106700,\"options\":null,\"multiEntitySupport\":false}},\"counterPartyContractingEntity\":{\"name\":\"counterPartyContractingEntity\",\"id\":11012,\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"values\":\"CDR USED IN AUTOMATION (PLEASE DO NOT USE)\",\"multiEntitySupport\":false},\"rootInfo\":{\"name\":\"rootInfo\",\"multiEntitySupport\":false},\"reviewAllowed\":{\"name\":\"reviewAllowed\",\"values\":false,\"multiEntitySupport\":false},\"projectId\":{\"name\":\"projectId\",\"multiEntitySupport\":false},\"dealOverview\":{\"name\":\"dealOverview\",\"id\":11026,\"values\":\"\",\"multiEntitySupport\":false},\"rateCardsApplicable\":{\"name\":\"rateCardsApplicable\",\"id\":12577,\"options\":null,\"multiEntitySupport\":false},\"existingRateCards\":{\"name\":\"existingRateCards\",\"values\":[],\"multiEntitySupport\":false},\"excludeWeekends\":{\"name\":\"excludeWeekends\",\"values\":false,\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":0,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}},\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"Pre Signature\",\"id\":183},\"multiEntitySupport\":false},\"contracts\":{\"name\":\"contracts\",\"multiEntitySupport\":false},\"title\":{\"name\":\"title\",\"id\":11005,\"values\":\"CDR USED IN AUTOMATION (PLEASE DO NOT USE)\",\"multiEntitySupport\":false},\"spendType\":{\"name\":\"spendType\",\"id\":12359,\"multiEntitySupport\":false},\"cdrRecipientMarkets\":{\"name\":\"cdrRecipientMarkets\",\"id\":11669,\"values\":[],\"multiEntitySupport\":false},\"tier\":{\"name\":\"tier\",\"multiEntitySupport\":false},\"adhocUser\":{\"firstName\":{\"name\":\"firstName\",\"id\":78,\"multiEntitySupport\":false},\"lastName\":{\"name\":\"lastName\",\"id\":79,\"multiEntitySupport\":false},\"loginId\":{\"name\":\"loginId\",\"id\":80,\"multiEntitySupport\":false},\"userType\":{\"name\":\"userType\",\"id\":83,\"options\":null,\"multiEntitySupport\":false},\"uniqueLoginId\":{\"name\":\"uniqueLoginId\",\"id\":82,\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false}},\"clientContractingEntity\":{\"name\":\"clientContractingEntity\",\"id\":11011,\"options\":null,\"multiEntitySupport\":false},\"expirationDate\":{\"name\":\"expirationDate\",\"id\":11236,\"multiEntitySupport\":false},\"draftEntity\":{\"name\":\"draftEntity\",\"values\":false,\"multiEntitySupport\":false},\"ageing\":{\"name\":\"ageing\",\"multiEntitySupport\":false},\"languageType\":{\"name\":\"languageType\",\"id\":12341,\"values\":{\"name\":\"English (English)\",\"id\":1},\"multiEntitySupport\":false},\"contractSupplier\":{\"name\":\"contractSupplier\",\"id\":12328,\"options\":null,\"multiEntitySupport\":false},\"tcv\":{\"name\":\"tcv\",\"id\":11024,\"displayValues\":\"\",\"multiEntitySupport\":false},\"sourceEntityTypeId\":{\"name\":\"sourceEntityTypeId\",\"multiEntitySupport\":false},\"submissionDate\":{\"name\":\"submissionDate\",\"id\":11027,\"multiEntitySupport\":false},\"contractCountries\":{\"name\":\"contractCountries\",\"multiEntitySupport\":false},\"rateCardToDate\":{\"name\":\"rateCardToDate\",\"id\":12579,\"multiEntitySupport\":false},\"clauseAssemblyEnabled\":{\"name\":\"clauseAssemblyEnabled\",\"values\":false,\"multiEntitySupport\":false},\"recipientCompanyCodes\":{\"name\":\"recipientCompanyCodes\",\"id\":12357,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"shortCodeId\":{\"name\":\"shortCodeId\",\"id\":11039,\"values\":\"CDR10646\",\"multiEntitySupport\":false},\"excludeFromHoliday\":{\"name\":\"excludeFromHoliday\",\"values\":false,\"multiEntitySupport\":false},\"sourceEntityId\":{\"name\":\"sourceEntityId\",\"multiEntitySupport\":false},\"contractParent\":{\"name\":\"contractParent\",\"multiEntitySupport\":false},\"comment\":{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null,\"multiEntitySupport\":false},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false,\"values\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false},\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"invoiceCopy\":{\"name\":\"invoiceCopy\",\"values\":false,\"multiEntitySupport\":false},\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false,\"values\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null,\"multiEntitySupport\":false},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"values\":[]}},\"completionDate\":{\"name\":\"completionDate\",\"id\":11028,\"multiEntitySupport\":false},\"category\":{\"name\":\"category\",\"id\":12662,\"values\":[],\"multiEntitySupport\":false},\"contractingCompanyCodes\":{\"name\":\"contractingCompanyCodes\",\"id\":12353,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"customer\":{\"name\":\"customer\",\"id\":12335,\"options\":null,\"multiEntitySupport\":false},\"dyn106561\":{\"name\":\"dyn106561\",\"id\":106561,\"multiEntitySupport\":false},\"dyn106680\":{\"name\":\"dyn106680\",\"id\":106680,\"options\":null,\"multiEntitySupport\":false},\"dyn103384\":{\"name\":\"dyn103384\",\"id\":103384,\"multiEntitySupport\":false},\"dyn106620\":{\"name\":\"dyn106620\",\"id\":106620,\"multiEntitySupport\":false},\"dyn106597\":{\"name\":\"dyn106597\",\"id\":106597,\"multiEntitySupport\":false},\"dyn106663\":{\"name\":\"dyn106663\",\"id\":106663,\"options\":null,\"multiEntitySupport\":false},\"dyn106701\":{\"name\":\"dyn106701\",\"id\":106701,\"multiEntitySupport\":false},\"dyn103378\":{\"name\":\"dyn103378\",\"id\":103378,\"multiEntitySupport\":false},\"dyn106647\":{\"name\":\"dyn106647\",\"id\":106647,\"multiEntitySupport\":false},\"dyn106578\":{\"name\":\"dyn106578\",\"id\":106578,\"multiEntitySupport\":false},\"dyn106700\":{\"name\":\"dyn106700\",\"id\":106700,\"options\":null,\"multiEntitySupport\":false}}}}";
       try{
           Create create = new Create();
           create.hitCreate(entityName, payloadCDR);
           createResponse = create.getCreateJsonStr();
           if(ParseJsonResponse.validJsonResponse(createResponse)){
               JSONObject jsonObject = new JSONObject(createResponse);
               String createStatus = jsonObject.getJSONObject("header").getJSONObject("response").getString("status").trim();
               if (createStatus.equalsIgnoreCase("success"))
                   entityId = CreateEntity.getNewEntityId(createResponse, "CDR");
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

    public HashMap<String, String> selectTemplateAndChooseClause(int cdrId, CustomAssert customAssert){
        TemplateSuggestion templateSuggestion = new TemplateSuggestion();
        String payload = "{\"offset\":0,\"size\":20}";
        HashMap<String, String> templateDetails = new HashMap<>();
        try{
            String selectTemplateResponse = templateSuggestion.hitSelectTemplate(entityTypeId,cdrId,payload);
            if(ParseJsonResponse.validJsonResponse(selectTemplateResponse)){
                JSONObject jsonObject = new JSONObject(selectTemplateResponse);
                JSONObject searchResultsJson = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data").getJSONObject(0);
                Set<String> keys = searchResultsJson.keySet();
                    for(String key : keys){
                        String columnName = searchResultsJson.getJSONObject(key).getString("columnName").trim();
                        if(columnName.equalsIgnoreCase("id")){
                            templateDetails.put("id",searchResultsJson.getJSONObject(key).getString("value").trim());
                        }else if(columnName.equalsIgnoreCase("name")){
                            templateDetails.put("name",searchResultsJson.getJSONObject(key).getString("value").trim());
                        }else if(columnName.equalsIgnoreCase("hasChildren")){
                            templateDetails.put("hasChildren",searchResultsJson.getJSONObject(key).getString("value").trim());
                        }else if(columnName.equalsIgnoreCase("templateTypeId")){
                            templateDetails.put("templateTypeId",searchResultsJson.getJSONObject(key).getString("value").trim());
                        }
                    }
            }else{
                logger.error("Select Template Response is not a valid response.");
                customAssert.assertTrue(false,"Select Template Response is not a valid response.");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while selecting the template.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while selecting the template.");
        }
        return templateDetails;
    }

    public boolean addTemplateToCDR(int entityID,HashMap<String,String> templateDetails,CustomAssert customAssert){
       String id = templateDetails.get("id");
       String name = templateDetails.get("name");
       String hasChildren = templateDetails.get("hasChildren");
       String templateTypeId = templateDetails.get("templateTypeId");
       String editPostResponse = null;
       Edit edit = new Edit();
       boolean status = false;
       try{
           String editGetResponse = edit.getEditPayload(entityName,entityID);
           JSONObject jsonGetObject = new JSONObject(editGetResponse);
           jsonGetObject.remove("header");
           jsonGetObject.remove("session");
           jsonGetObject.remove("actions");
           jsonGetObject.remove("createLinks");
           editGetResponse = jsonGetObject.toString();
           if(ParseJsonResponse.validJsonResponse(editGetResponse)){
               jsonGetObject = new JSONObject(editGetResponse);
               String additionInPayload = "[{\"id\":\""+id+"\",\"name\":\""+name+"\",\"hasChildren\":\""+hasChildren+"\",\"templateTypeId\":\""+templateTypeId+"\",\"checked\":1,\"mappedContractTemplates\":null,\"uniqueIdentifier\":\""+id+"95795665739\",\"$$hashKey\":\"object:706\",\"tagsCount\":1,\"mappedTags\":{\"2565\":{\"name\":\"the\",\"id\":2565,\"identifier\":\"the\",\"tagHTMLType\":{\"name\":\"Text Field\",\"id\":1},\"orderSeq\":1,\"tagTypeId\":2,\"$$hashKey\":\"object:723\"}}}]";
               JSONObject jsonObject = jsonGetObject.getJSONObject("body").getJSONObject("data");
               Object[] keys = jsonObject.keySet().toArray();
               for(Object key : keys){
                   String dataKey = key.toString();
                   if(dataKey.equalsIgnoreCase("createdFor")|dataKey.equalsIgnoreCase("suppliers")|dataKey.equalsIgnoreCase("stakeHolders")|dataKey.equalsIgnoreCase("clientContractingEntity")|dataKey.equalsIgnoreCase("requestedBy")|dataKey.equalsIgnoreCase("changeRequest")){
                       jsonGetObject.getJSONObject("body").getJSONObject("data").getJSONObject(dataKey).getJSONObject("options").remove("api");
                       jsonGetObject.getJSONObject("body").getJSONObject("data").getJSONObject(dataKey).getJSONObject("options").append("api","\"\"");
                   }else if(dataKey.equalsIgnoreCase("mappedContractTemplates")){
                       jsonGetObject.getJSONObject("body").getJSONObject("data").getJSONObject(dataKey).remove("multiEntitySupport");
                       jsonGetObject.getJSONObject("body").getJSONObject("data").getJSONObject(dataKey).append("values",new JSONArray(additionInPayload));
                   }
               }


               String editPayload = jsonGetObject.toString();
               if(ParseJsonResponse.validJsonResponse(editPayload)){
                   editPostResponse = edit.hitEdit(entityName,editPayload);
                   if(new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")){
                       status = true;
                       logger.info("Template with ID {} is selected for CDR with ID {}",id,entityID);
                   }else{
                       logger.error("Template with ID {} could not be selected for CDR with ID {}",id,entityID);
                       customAssert.assertTrue(false,"Template with ID "+id+" could not be selected for CDR with ID "+entityID);
                   }
               }else{
                   logger.error("Edit payload formed is not a valid JSON");
                   customAssert.assertTrue(false,"Edit payload formed is not a valid JSON");
               }
           }else{
               logger.error("Edit Response is not a valid JSON Response");
               customAssert.assertTrue(false,"Edit Response is not a valid JSON Response");
           }
       }catch(Exception e){
           logger.error("Exception {} occurred while adding the template to the CDR",e.getMessage());
           customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while adding the template to the CDR");
       }
       return status;
    }

    public boolean downloadSystemTagDocumentContractDocumentTab(int entityId,CustomAssert customAssert){
        String contractDocumentTabPayload = "{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        String documentName = null;
        int documentID = -1;
        String documentExtension = null;
        String statusId = null;
        boolean flag = false;
        boolean downloadStatus = false;
        try{
            HttpResponse contractDocumentTabResponse = new TabListData().hitTabListData("367", false, "",""+entityTypeId,""+entityId,contractDocumentTabPayload);
            String contractDocumentTabResponseAsString = EntityUtils.toString(contractDocumentTabResponse.getEntity());
            JSONObject dataJsonObject = new JSONObject(contractDocumentTabResponseAsString);
            JSONArray dataArray = new JSONArray(dataJsonObject.getString("data"));
            for(int index =0;index<dataArray.length();index++) {
                Set<String> keySet = new JSONObject(dataArray.get(index)).keySet();
                Object[] keys = keySet.toArray();
                for (int newIndex = keys.length - 1; newIndex >= 0; newIndex--) {
                    String key = keys[newIndex].toString();
                    String columnName = dataJsonObject.getJSONObject(key).getString("columnName").trim();
                    if (columnName.equalsIgnoreCase("documentstatus")) {
                        String value = dataJsonObject.getJSONObject(key).getString("value").trim();
                        try {
                            statusId = value.split(":;")[0];
                        } catch (Exception ex) {
                            logger.error("Exception {} occurred while fetching document details", ex.getMessage());
                            customAssert.assertTrue(false, "Exception " + ex.getMessage() + " occurred while fetching document details");
                        }
                    }

                    if(statusId.equalsIgnoreCase("1") & columnName.equalsIgnoreCase("documentname")){
                        String value = dataJsonObject.getJSONObject(key).getString("value").trim();
                        try{
                            documentName = value.split(":;")[1];
                            documentExtension = value.split(":;")[2];
                            documentID = Integer.parseInt(value.split(":;")[4]);
                            flag = true;
                            break;
                        }catch(Exception ex){
                            logger.error("Exception {} occurred while fetching document details",ex.getMessage());
                            customAssert.assertTrue(false,"Exception "+ex.getMessage()+" occurred while fetching document details");
                        }
                    }
                }
                if(flag)
                    break;
            }
            DownloadContractTemplateInCDR downloadContractTemplateInCDR = new DownloadContractTemplateInCDR();
            HttpResponse downloadContractDocumentResponse = downloadContractTemplateInCDR.downloadSystemTagged(documentID,entityTypeId);
            String filePath = "src/test/java/com/sirionlabs/test/pod/CDRContractDocument";
            String fileName = documentName+"."+documentExtension;
            downloadStatus = new DownloadListWithData().dumpDownloadListIntoFile(downloadContractDocumentResponse,filePath,fileName);
        }catch(Exception e){
            logger.error("Exception {} occurred while fetching documents under Contract Document Tab", e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while fetching documents under Contract Document Tab");
        }
        return downloadStatus;
    }


}