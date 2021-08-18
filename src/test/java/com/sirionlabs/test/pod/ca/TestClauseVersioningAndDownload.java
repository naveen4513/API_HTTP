package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.presignature.ClausePageData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static com.sirionlabs.helper.api.TestAPIBase.executor;

public class TestClauseVersioningAndDownload{
    private final static Logger logger = LoggerFactory.getLogger(TestClauseVersioningAndDownload.class);

    private String configFilePath;
    private String configFileName;
    private String entityURLId;
    private String entityTypeId;

    private String entityName = "clauses";
    private String outputPath = "src/test/java/com/sirionlabs/test/pod/ListingData/";
    private String outputFileName = entityName +" - ListData.xlsx";

    @BeforeClass
    public void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_url_id");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");
    }

    @Test
    public void testC140859() {
        CustomAssert customAssert = new CustomAssert();
        try{
            if(downloadClauseListing(entityName,customAssert)){
                customAssert.assertTrue(readExcelFile(customAssert),"TC-C140859 is failed.");
            }else{
                logger.error("Issue occurred in downloading.");
                customAssert.assertTrue(false,"Issue occurred in downloading.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while automation TC-C140859.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while automation TC-C140859.");
        }
        customAssert.assertAll();
    }

    @Test
    public void testC140856(){
        CustomAssert customAssert = new CustomAssert();
        int clauseId = -1;
        try{
            String originalText = "Dummy Test";
            String timeStamp = new DateUtils().getCurrentTimeStamp();
            String replacementText = originalText + timeStamp;
            clauseId = createClauseAndGetNewId(originalText,customAssert);
            if(performActionOnCDR(clauseId,"SendForClientReview",customAssert)){
                if(performActionOnCDR(clauseId,"Approve",customAssert)){
                    if(performActionOnCDR(clauseId,"Publish",customAssert)){
                        double versionOnPublish1 = getVersionNumber(clauseId,customAssert);
                        if(performActionOnCDR(clauseId,"Inactivate",customAssert)){
                            if(editTheText(clauseId, originalText, replacementText, customAssert)){
                                if(performActionOnCDR(clauseId,"Publish",customAssert)){
                                    double versionOnPublish2 = getVersionNumber(clauseId,customAssert);
                                    customAssert.assertTrue(versionOnPublish2 == versionOnPublish1 + 1,"Version number is not changed.");
                                }else{
                                    logger.error("Could not perform the action \"Publish\".");
                                    customAssert.assertTrue(false,"Could not perform the action \"Publish\".");
                                }
                            }else{
                                logger.error("Editing is unsuccessful.");
                                customAssert.assertTrue(false,"Editing is unsuccessful.");
                            }
                        }else{
                            logger.error("Could not perform the action \"Inactivate\".");
                            customAssert.assertTrue(false,"Could not perform the action \"Inactivate\".");
                        }
                    }else{
                        logger.error("Could not perform the action \"Publish\".");
                        customAssert.assertTrue(false,"Could not perform the action \"Publish\".");
                    }
                }else{
                    logger.error("Could not perform the action \"Approve\".");
                    customAssert.assertTrue(false,"Could not perform the action \"Approve\".");
                }
            }else{
                logger.error("Could not perform the action \"Send For Client Review\".");
                customAssert.assertTrue(false,"Could not perform the action \"Send For Client Review\".");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while creating a new clause.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating a new clause.");
        }finally {
            EntityOperationsHelper.deleteEntityRecord(entityName, clauseId);
            logger.info("Clause with clause id {} is deleted.",clauseId);
        }
        customAssert.assertAll();
    }

    @Test
    public void testC140965(){
        CustomAssert customAssert = new CustomAssert();
        try{
            customAssert.assertTrue(getListingColumns("list", customAssert),"Clause Version does not exist in List View.");
            customAssert.assertTrue(getListingColumns("grid", customAssert),"Clause Version does not exist in Grid View.");
        }catch (Exception e){
            logger.error("Exception {} occurred while automating TC-C140856.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while automating TC-C140856.");
        }
        customAssert.assertAll();
    }

    @Test
    public void testC152894(){
        CustomAssert customAssert = new CustomAssert();
        int clauseId = -1;
        try{
            String originalText = "Dummy Test";
            String timeStamp = new DateUtils().getCurrentTimeStamp();
            String replacementText = originalText + timeStamp;
            clauseId = createClauseAndGetNewId(originalText,customAssert);
            if(!checkCompareOnUI(clauseId,customAssert)){
                if(performActionOnCDR(clauseId,"SendForClientReview",customAssert)){
                    if(performActionOnCDR(clauseId,"Approve",customAssert)){
                        if(performActionOnCDR(clauseId,"Publish",customAssert)){
                            double versionOnPublish1 = getVersionNumber(clauseId,customAssert);
                            if(performActionOnCDR(clauseId,"Inactivate",customAssert)){
                                if(editTheText(clauseId, originalText, replacementText, customAssert)){
                                    if(getVersionNumber(clauseId,customAssert) == versionOnPublish1){
                                        if(performActionOnCDR(clauseId,"Publish",customAssert)){
                                            double versionOnPublish2 = getVersionNumber(clauseId,customAssert);
                                            if(versionOnPublish2 != versionOnPublish1){
                                                if(checkCompareOnUI(clauseId,customAssert)){
                                                    List<Integer> clauseVersionIds = compareClause(clauseId, customAssert);
                                                    Collections.sort(clauseVersionIds);
                                                    HashMap<Integer,List<String>> versionContent = getClauseContents(clauseVersionIds, customAssert);
                                                    customAssert.assertFalse(compareTextOfLists(versionContent,customAssert),"Content is not updated.");
                                                }else{
                                                    logger.error("TC-C152915 failed.");
                                                    customAssert.assertTrue(false,"TC-C152915 failed.");
                                                }
                                            }else{
                                                logger.error("Versions didn't get updated.");
                                                customAssert.assertTrue(false,"Versions didn't get updated.");
                                            }
                                        }else{
                                            logger.error("Could not perform the action \"Publish\".");
                                            customAssert.assertTrue(false,"Could not perform the action \"Publish\".");
                                        }
                                    }else{
                                        logger.error("TC-C152977 has failed.");
                                        customAssert.assertTrue(false,"TC-C152977 has failed.");
                                    }
                                }else{
                                    logger.error("Editing is unsuccessful.");
                                    customAssert.assertTrue(false,"Editing is unsuccessful.");
                                }
                            }else{
                                logger.error("Could not perform the action \"Inactivate\".");
                                customAssert.assertTrue(false,"Could not perform the action \"Inactivate\".");
                            }
                        }else{
                            logger.error("Could not perform the action \"Publish\".");
                            customAssert.assertTrue(false,"Could not perform the action \"Publish\".");
                        }
                    }else{
                        logger.error("Could not perform the action \"Approve\".");
                        customAssert.assertTrue(false,"Could not perform the action \"Approve\".");
                    }
                }else{
                    logger.error("Could not perform the action \"Send For Client Review\".");
                    customAssert.assertTrue(false,"Could not perform the action \"Send For Client Review\".");
                }
            }else{
                logger.error("TC-C152909 failed.");
                customAssert.assertTrue(false,"TC-C152909 failed.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while creating a new clause.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating a new clause.");
        }finally {
            EntityOperationsHelper.deleteEntityRecord(entityName, clauseId);
            logger.info("Clause with clause id {} is deleted.",clauseId);
        }
        customAssert.assertAll();
    }

    @Test
    public void testC152978(){
        CustomAssert customAssert = new CustomAssert();
        int clauseId = -1;
        try{
            clauseId = createClauseAndGetNewId("Dummy Text",customAssert);
            if(performActionOnCDR(clauseId,"SendForClientReview",customAssert)){
                if(performActionOnCDR(clauseId,"Approve",customAssert)){
                    if(performActionOnCDR(clauseId,"Publish",customAssert)){
                        double version = getVersionNumber(clauseId, customAssert);
                        if(performActionOnCDR(clauseId,"Inactivate",customAssert)) {
                            String uniqueTag = DateUtils.getCurrentTimeStamp();
                            if (addTagInClause(clauseId, uniqueTag, customAssert)) {
                                if(performActionOnCDR(clauseId,"Publish",customAssert)){
                                    customAssert.assertTrue(getVersionNumber(clauseId, customAssert) == (version+1.0),"TC-C152978 is failed.");
                                }else{
                                    logger.error("Could not perform the action \"Publish\"");
                                    customAssert.assertTrue(false,"Could not perform the action \"Publish\"");
                                }
                            } else {
                                logger.error("Tag could not be added.");
                                customAssert.assertTrue(false, "Tag could not be added.");
                            }
                        }else{
                            logger.error("Could not perform the action \"Inactivate\"");
                            customAssert.assertTrue(false,"Could not perform the action \"Inactivate\"");
                        }
                    }else{
                        logger.error("Could not perform the action \"Publish\"");
                        customAssert.assertTrue(false,"Could not perform the action \"Publish\"");
                    }
                }else{
                    logger.error("Could not perform the action \"Approve\"");
                    customAssert.assertTrue(false,"Could not perform the action \"Approve\"");
                }
            }else{
                logger.error("Could not perform the action \"SendForClientReview\"");
                customAssert.assertTrue(false,"Could not perform the action \"SendForClientReview\"");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while executing TC-C152978.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while executing TC-C152978.");
        }finally {
            EntityOperationsHelper.deleteEntityRecord(entityName, clauseId);
            logger.info("Clause with clause id {} is deleted.",clauseId);
        }
        customAssert.assertAll();
    }


    private String clausePayload(String textInClause){
        if(textInClause==null)
            return "{\"body\":{\"data\":{\"parentShortCodeId\":{\"name\":\"parentShortCodeId\",\"values\":{\"name\":\"PRS01004\",\"id\":1004},\"multiEntitySupport\":false},\"functions\":{\"name\":\"functions\",\"id\":11549,\"values\":[{\"name\":\"IT\",\"id\":1002}],\"options\":null,\"multiEntitySupport\":false},\"suppliers\":{\"name\":\"suppliers\",\"id\":7369,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"keywords\":{\"name\":\"keywords\",\"id\":7384,\"multiEntitySupport\":false},\"transactionTypes\":{\"name\":\"transactionTypes\",\"id\":7377,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"contractingClientEntities\":{\"name\":\"contractingClientEntities\",\"multiEntitySupport\":false},\"integrationSystem\":{\"name\":\"integrationSystem\",\"multiEntitySupport\":false},\"scheduleReference\":{\"name\":\"scheduleReference\",\"id\":11839,\"values\":false,\"multiEntitySupport\":false},\"parentEntityId\":{\"name\":\"parentEntityId\",\"values\":1004,\"multiEntitySupport\":false},\"recipientHubs\":{\"name\":\"recipientHubs\",\"multiEntitySupport\":false},\"type\":{\"name\":\"type\",\"id\":7370,\"options\":null,\"multiEntitySupport\":false},\"globalRegions\":{\"name\":\"globalRegions\",\"id\":7391,\"values\":[{\"name\":\"APAC\",\"id\":1002,\"parentId\":1002}],\"options\":null,\"multiEntitySupport\":false},\"globalCountries\":{\"name\":\"globalCountries\",\"id\":7392,\"values\":[{\"name\":\"Thailand\",\"id\":228,\"parentId\":1002}],\"options\":null,\"multiEntitySupport\":false},\"serviceCategory\":{\"name\":\"serviceCategory\",\"multiEntitySupport\":false},\"subHeader\":{\"name\":\"subHeader\",\"id\":12036,\"multiEntitySupport\":false},\"supplierAccess\":{\"name\":\"supplierAccess\",\"values\":false,\"multiEntitySupport\":false},\"stakeHolders\":{\"name\":\"stakeHolders\",\"values\":{\"rg_2016\":{\"values\":[{\"name\":\"Anay User\",\"id\":1044,\"type\":2,\"email\":\"srijan.samanta@sirionqa.office\",\"properties\":{\"Contact Number\":\"987654321\",\"Designation\":\"QA\",\"Default Tier\":\"View All\",\"Email\":\"srijan.samanta@sirionqa.office\",\"First Name\":\"Anay\",\"Time Zone:\":\"Asia/Kolkata (GMT +05:30)\",\"User Department\":\" - \",\"Legal Document\":\"Yes\",\"Last Name\":\"User\",\"Exclude From Filters\":\" - \",\"Business Case\":\"Yes\",\"Financial Document\":\"Yes\"}}],\"name\":\"rg_2016\",\"label\":\"Clause Manager\",\"userType\":[2,1,3,4]}},\"options\":null,\"multiEntitySupport\":false},\"canSupplierBeParent\":true,\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false},\"subClauses\":{\"name\":\"subClauses\",\"values\":[],\"multiEntitySupport\":false},\"supplier\":{\"name\":\"supplier\",\"multiEntitySupport\":false},\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"id\":{\"name\":\"id\",\"id\":7365,\"multiEntitySupport\":false},\"parentEntityIds\":{\"name\":\"parentEntityIds\",\"multiEntitySupport\":false},\"state\":{\"name\":\"state\",\"values\":{},\"multiEntitySupport\":false},\"actualParentEntityTypeId\":{\"name\":\"actualParentEntityTypeId\",\"multiEntitySupport\":false},\"contractingEntity\":{\"name\":\"contractingEntity\",\"id\":7371,\"options\":null,\"multiEntitySupport\":false},\"group\":{\"name\":\"group\",\"id\":11837,\"values\":{\"name\":\"Clause\",\"id\":1},\"options\":null,\"multiEntitySupport\":false},\"contractingHubs\":{\"name\":\"contractingHubs\",\"multiEntitySupport\":false},\"cycleTime\":{\"name\":\"cycleTime\",\"multiEntitySupport\":false},\"industryTypes\":{\"name\":\"industryTypes\",\"id\":7373,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"parentHalting\":{\"name\":\"parentHalting\",\"values\":false,\"multiEntitySupport\":false},\"weekType\":{\"name\":\"weekType\",\"multiEntitySupport\":false},\"stagingPrimaryKey\":{\"name\":\"stagingPrimaryKey\",\"multiEntitySupport\":false},\"timeZone\":{\"name\":\"timeZone\",\"multiEntitySupport\":false},\"dynamicMetadata\":{\"dyn106595\":{\"name\":\"dyn106595\",\"id\":106595,\"multiEntitySupport\":false},\"dyn106661\":{\"name\":\"dyn106661\",\"id\":106661,\"options\":null,\"multiEntitySupport\":false},\"dyn106560\":{\"name\":\"dyn106560\",\"id\":106560,\"multiEntitySupport\":false},\"dyn106576\":{\"name\":\"dyn106576\",\"id\":106576,\"multiEntitySupport\":false},\"dyn106614\":{\"name\":\"dyn106614\",\"id\":106614,\"multiEntitySupport\":false},\"dyn106645\":{\"name\":\"dyn106645\",\"id\":106645,\"multiEntitySupport\":false},\"dyn106678\":{\"name\":\"dyn106678\",\"id\":106678,\"options\":null,\"multiEntitySupport\":false}},\"formattedText\":{\"name\":\"formattedText\",\"multiEntitySupport\":false},\"leadTimes\":{\"name\":\"leadTimes\",\"multiEntitySupport\":false},\"addSubclauses\":{\"name\":\"addSubclauses\",\"id\":11229,\"values\":false,\"multiEntitySupport\":false},\"clientHoliday\":{\"name\":\"clientHoliday\",\"multiEntitySupport\":false},\"projectLevels\":{\"name\":\"projectLevels\",\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":7367,\"values\":\"API Automation 8031125\",\"multiEntitySupport\":false},\"rootInfo\":{\"name\":\"rootInfo\",\"multiEntitySupport\":false},\"overallRiskScore\":{\"name\":\"overallRiskScore\",\"multiEntitySupport\":false},\"forceTemplateRenewal\":{\"name\":\"forceTemplateRenewal\",\"id\":7383,\"multiEntitySupport\":false},\"projectId\":{\"name\":\"projectId\",\"multiEntitySupport\":false},\"status\":{\"name\":\"status\",\"id\":7366,\"values\":{},\"multiEntitySupport\":false},\"contractingMarkets\":{\"name\":\"contractingMarkets\",\"multiEntitySupport\":false},\"excludeWeekends\":{\"name\":\"excludeWeekends\",\"values\":false,\"multiEntitySupport\":false},\"companyPosition\":{\"name\":\"companyPosition\",\"id\":7374,\"options\":null,\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":0,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}},\"definitionCategories\":{\"name\":\"definitionCategories\",\"id\":11838,\"options\":null,\"multiEntitySupport\":false},\"termType\":{\"name\":\"termType\",\"id\":7372,\"options\":null,\"multiEntitySupport\":false},\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"Pre Signature\",\"id\":183},\"multiEntitySupport\":false},\"contracts\":{\"name\":\"contracts\",\"multiEntitySupport\":false},\"recipientClientEntities\":{\"name\":\"recipientClientEntities\",\"multiEntitySupport\":false},\"agreementTypes\":{\"name\":\"agreementTypes\",\"id\":7375,\"values\":[{\"name\":\"Services Agreement\",\"id\":1008}],\"options\":null,\"multiEntitySupport\":false},\"minTcv\":{\"name\":\"minTcv\",\"id\":7380,\"multiEntitySupport\":false},\"compareAllowed\":{\"name\":\"compareAllowed\",\"values\":false,\"multiEntitySupport\":false},\"tier\":{\"name\":\"tier\",\"multiEntitySupport\":false},\"adhocUser\":{\"firstName\":{\"name\":\"firstName\",\"id\":78,\"multiEntitySupport\":false},\"lastName\":{\"name\":\"lastName\",\"id\":79,\"multiEntitySupport\":false},\"loginId\":{\"name\":\"loginId\",\"id\":80,\"multiEntitySupport\":false},\"userType\":{\"name\":\"userType\",\"id\":83,\"options\":null,\"multiEntitySupport\":false},\"uniqueLoginId\":{\"name\":\"uniqueLoginId\",\"id\":82,\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false}},\"vendorContractingParty\":{\"name\":\"vendorContractingParty\",\"multiEntitySupport\":false},\"riskTypes\":{\"name\":\"riskTypes\",\"id\":7389,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"draftEntity\":{\"name\":\"draftEntity\",\"values\":false,\"multiEntitySupport\":false},\"ageing\":{\"name\":\"ageing\",\"multiEntitySupport\":false},\"contractServices\":{\"name\":\"contractServices\",\"id\":7378,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"languageType\":{\"name\":\"languageType\",\"id\":12340,\"values\":{\"name\":\"English (English)\",\"id\":1},\"options\":null,\"multiEntitySupport\":false},\"actualParentEntityId\":{\"name\":\"actualParentEntityId\",\"multiEntitySupport\":false},\"sourceEntityTypeId\":{\"name\":\"sourceEntityTypeId\",\"multiEntitySupport\":false},\"filePath\":{\"name\":\"filePath\",\"multiEntitySupport\":false},\"headerLabel\":{\"name\":\"headerLabel\",\"id\":11464,\"multiEntitySupport\":false},\"recipientMarkets\":{\"name\":\"recipientMarkets\",\"id\":11846,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"addendums\":{\"name\":\"addendums\",\"values\":[],\"multiEntitySupport\":false},\"services\":{\"name\":\"services\",\"id\":11550,\"values\":[{\"name\":\"Network MNS\",\"id\":1005,\"parentId\":1002}],\"options\":null,\"multiEntitySupport\":false},\"contractCountries\":{\"name\":\"contractCountries\",\"multiEntitySupport\":false},\"clauseTags\":{\"name\":\"clauseTags\",\"id\":7382,\"multiEntitySupport\":false},\"parentEntityTypeId\":{\"name\":\"parentEntityTypeId\",\"values\":183,\"multiEntitySupport\":false},\"maxTcv\":{\"name\":\"maxTcv\",\"id\":7381,\"multiEntitySupport\":false},\"recipientCompanyCodes\":{\"name\":\"recipientCompanyCodes\",\"multiEntitySupport\":false},\"oldSystemId\":{\"name\":\"oldSystemId\",\"multiEntitySupport\":false},\"shortCodeId\":{\"name\":\"shortCodeId\",\"id\":7393,\"values\":\"CL08329\",\"multiEntitySupport\":false},\"formattingProperties\":{\"name\":\"formattingProperties\",\"id\":7388,\"multiEntitySupport\":false},\"excludeFromHoliday\":{\"name\":\"excludeFromHoliday\",\"values\":false,\"multiEntitySupport\":false},\"sourceEntityId\":{\"name\":\"sourceEntityId\",\"multiEntitySupport\":false},\"clauseText\":{\"name\":\"clauseText\",\"id\":7386,\"values\":{\"text\":\"\",\"htmlText\":\"\"},\"multiEntitySupport\":false},\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":138,\"multiEntitySupport\":false},\"comment\":{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null,\"multiEntitySupport\":false},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false},\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":null,\"multiEntitySupport\":false},\"invoiceCopy\":{\"name\":\"invoiceCopy\",\"values\":false,\"multiEntitySupport\":false},\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null,\"multiEntitySupport\":false},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"values\":[]}},\"relations\":{\"name\":\"relations\",\"multiEntitySupport\":false},\"category\":{\"name\":\"category\",\"id\":7368,\"values\":{\"name\":\"Limited Warranty\",\"id\":1200},\"options\":null,\"multiEntitySupport\":false},\"contractRegions\":{\"name\":\"contractRegions\",\"multiEntitySupport\":false},\"clauseVersion\":{\"name\":\"clauseVersion\",\"id\":12657,\"multiEntitySupport\":false},\"contractingCompanyCodes\":{\"name\":\"contractingCompanyCodes\",\"multiEntitySupport\":false},\"dyn106595\":{\"name\":\"dyn106595\",\"id\":106595,\"multiEntitySupport\":false},\"dyn106661\":{\"name\":\"dyn106661\",\"id\":106661,\"options\":null,\"multiEntitySupport\":false},\"dyn106560\":{\"name\":\"dyn106560\",\"id\":106560,\"multiEntitySupport\":false},\"dyn106576\":{\"name\":\"dyn106576\",\"id\":106576,\"multiEntitySupport\":false},\"dyn106614\":{\"name\":\"dyn106614\",\"id\":106614,\"multiEntitySupport\":false},\"dyn106645\":{\"name\":\"dyn106645\",\"id\":106645,\"multiEntitySupport\":false},\"dyn106678\":{\"name\":\"dyn106678\",\"id\":106678,\"options\":null,\"multiEntitySupport\":false}}}}";
        else
            return "{\"body\":{\"data\":{\"parentShortCodeId\":{\"name\":\"parentShortCodeId\",\"values\":{\"name\":\"PRS01004\",\"id\":1004},\"multiEntitySupport\":false},\"functions\":{\"name\":\"functions\",\"id\":11549,\"values\":[{\"name\":\"IT\",\"id\":1002}],\"options\":null,\"multiEntitySupport\":false},\"suppliers\":{\"name\":\"suppliers\",\"id\":7369,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"keywords\":{\"name\":\"keywords\",\"id\":7384,\"multiEntitySupport\":false},\"transactionTypes\":{\"name\":\"transactionTypes\",\"id\":7377,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"contractingClientEntities\":{\"name\":\"contractingClientEntities\",\"multiEntitySupport\":false},\"integrationSystem\":{\"name\":\"integrationSystem\",\"multiEntitySupport\":false},\"scheduleReference\":{\"name\":\"scheduleReference\",\"id\":11839,\"values\":false,\"multiEntitySupport\":false},\"parentEntityId\":{\"name\":\"parentEntityId\",\"values\":1004,\"multiEntitySupport\":false},\"recipientHubs\":{\"name\":\"recipientHubs\",\"multiEntitySupport\":false},\"type\":{\"name\":\"type\",\"id\":7370,\"options\":null,\"multiEntitySupport\":false},\"globalRegions\":{\"name\":\"globalRegions\",\"id\":7391,\"values\":[{\"name\":\"APAC\",\"id\":1002,\"parentId\":1002}],\"options\":null,\"multiEntitySupport\":false},\"globalCountries\":{\"name\":\"globalCountries\",\"id\":7392,\"values\":[{\"name\":\"Thailand\",\"id\":228,\"parentId\":1002}],\"options\":null,\"multiEntitySupport\":false},\"serviceCategory\":{\"name\":\"serviceCategory\",\"multiEntitySupport\":false},\"subHeader\":{\"name\":\"subHeader\",\"id\":12036,\"multiEntitySupport\":false},\"supplierAccess\":{\"name\":\"supplierAccess\",\"values\":false,\"multiEntitySupport\":false},\"stakeHolders\":{\"name\":\"stakeHolders\",\"values\":{\"rg_2016\":{\"values\":[{\"name\":\"Anay User\",\"id\":1044,\"type\":2,\"email\":\"srijan.samanta@sirionqa.office\",\"properties\":{\"Contact Number\":\"987654321\",\"Designation\":\"QA\",\"Default Tier\":\"View All\",\"Email\":\"srijan.samanta@sirionqa.office\",\"First Name\":\"Anay\",\"Time Zone:\":\"Asia/Kolkata (GMT +05:30)\",\"User Department\":\" - \",\"Legal Document\":\"Yes\",\"Last Name\":\"User\",\"Exclude From Filters\":\" - \",\"Business Case\":\"Yes\",\"Financial Document\":\"Yes\"}}],\"name\":\"rg_2016\",\"label\":\"Clause Manager\",\"userType\":[2,1,3,4]}},\"options\":null,\"multiEntitySupport\":false},\"canSupplierBeParent\":true,\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false},\"subClauses\":{\"name\":\"subClauses\",\"values\":[],\"multiEntitySupport\":false},\"supplier\":{\"name\":\"supplier\",\"multiEntitySupport\":false},\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"id\":{\"name\":\"id\",\"id\":7365,\"multiEntitySupport\":false},\"parentEntityIds\":{\"name\":\"parentEntityIds\",\"multiEntitySupport\":false},\"state\":{\"name\":\"state\",\"values\":{},\"multiEntitySupport\":false},\"actualParentEntityTypeId\":{\"name\":\"actualParentEntityTypeId\",\"multiEntitySupport\":false},\"contractingEntity\":{\"name\":\"contractingEntity\",\"id\":7371,\"options\":null,\"multiEntitySupport\":false},\"group\":{\"name\":\"group\",\"id\":11837,\"values\":{\"name\":\"Clause\",\"id\":1},\"options\":null,\"multiEntitySupport\":false},\"contractingHubs\":{\"name\":\"contractingHubs\",\"multiEntitySupport\":false},\"cycleTime\":{\"name\":\"cycleTime\",\"multiEntitySupport\":false},\"industryTypes\":{\"name\":\"industryTypes\",\"id\":7373,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"parentHalting\":{\"name\":\"parentHalting\",\"values\":false,\"multiEntitySupport\":false},\"weekType\":{\"name\":\"weekType\",\"multiEntitySupport\":false},\"stagingPrimaryKey\":{\"name\":\"stagingPrimaryKey\",\"multiEntitySupport\":false},\"timeZone\":{\"name\":\"timeZone\",\"multiEntitySupport\":false},\"dynamicMetadata\":{\"dyn106595\":{\"name\":\"dyn106595\",\"id\":106595,\"multiEntitySupport\":false},\"dyn106661\":{\"name\":\"dyn106661\",\"id\":106661,\"options\":null,\"multiEntitySupport\":false},\"dyn106560\":{\"name\":\"dyn106560\",\"id\":106560,\"multiEntitySupport\":false},\"dyn106576\":{\"name\":\"dyn106576\",\"id\":106576,\"multiEntitySupport\":false},\"dyn106614\":{\"name\":\"dyn106614\",\"id\":106614,\"multiEntitySupport\":false},\"dyn106645\":{\"name\":\"dyn106645\",\"id\":106645,\"multiEntitySupport\":false},\"dyn106678\":{\"name\":\"dyn106678\",\"id\":106678,\"options\":null,\"multiEntitySupport\":false}},\"formattedText\":{\"name\":\"formattedText\",\"multiEntitySupport\":false},\"leadTimes\":{\"name\":\"leadTimes\",\"multiEntitySupport\":false},\"addSubclauses\":{\"name\":\"addSubclauses\",\"id\":11229,\"values\":false,\"multiEntitySupport\":false},\"clientHoliday\":{\"name\":\"clientHoliday\",\"multiEntitySupport\":false},\"projectLevels\":{\"name\":\"projectLevels\",\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":7367,\"values\":\"API Automation 8031125\",\"multiEntitySupport\":false},\"rootInfo\":{\"name\":\"rootInfo\",\"multiEntitySupport\":false},\"overallRiskScore\":{\"name\":\"overallRiskScore\",\"multiEntitySupport\":false},\"forceTemplateRenewal\":{\"name\":\"forceTemplateRenewal\",\"id\":7383,\"multiEntitySupport\":false},\"projectId\":{\"name\":\"projectId\",\"multiEntitySupport\":false},\"status\":{\"name\":\"status\",\"id\":7366,\"values\":{},\"multiEntitySupport\":false},\"contractingMarkets\":{\"name\":\"contractingMarkets\",\"multiEntitySupport\":false},\"excludeWeekends\":{\"name\":\"excludeWeekends\",\"values\":false,\"multiEntitySupport\":false},\"companyPosition\":{\"name\":\"companyPosition\",\"id\":7374,\"options\":null,\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":0,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}},\"definitionCategories\":{\"name\":\"definitionCategories\",\"id\":11838,\"options\":null,\"multiEntitySupport\":false},\"termType\":{\"name\":\"termType\",\"id\":7372,\"options\":null,\"multiEntitySupport\":false},\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"Pre Signature\",\"id\":183},\"multiEntitySupport\":false},\"contracts\":{\"name\":\"contracts\",\"multiEntitySupport\":false},\"recipientClientEntities\":{\"name\":\"recipientClientEntities\",\"multiEntitySupport\":false},\"agreementTypes\":{\"name\":\"agreementTypes\",\"id\":7375,\"values\":[{\"name\":\"Services Agreement\",\"id\":1008}],\"options\":null,\"multiEntitySupport\":false},\"minTcv\":{\"name\":\"minTcv\",\"id\":7380,\"multiEntitySupport\":false},\"compareAllowed\":{\"name\":\"compareAllowed\",\"values\":false,\"multiEntitySupport\":false},\"tier\":{\"name\":\"tier\",\"multiEntitySupport\":false},\"adhocUser\":{\"firstName\":{\"name\":\"firstName\",\"id\":78,\"multiEntitySupport\":false},\"lastName\":{\"name\":\"lastName\",\"id\":79,\"multiEntitySupport\":false},\"loginId\":{\"name\":\"loginId\",\"id\":80,\"multiEntitySupport\":false},\"userType\":{\"name\":\"userType\",\"id\":83,\"options\":null,\"multiEntitySupport\":false},\"uniqueLoginId\":{\"name\":\"uniqueLoginId\",\"id\":82,\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false}},\"vendorContractingParty\":{\"name\":\"vendorContractingParty\",\"multiEntitySupport\":false},\"riskTypes\":{\"name\":\"riskTypes\",\"id\":7389,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"draftEntity\":{\"name\":\"draftEntity\",\"values\":false,\"multiEntitySupport\":false},\"ageing\":{\"name\":\"ageing\",\"multiEntitySupport\":false},\"contractServices\":{\"name\":\"contractServices\",\"id\":7378,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"languageType\":{\"name\":\"languageType\",\"id\":12340,\"values\":{\"name\":\"English (English)\",\"id\":1},\"options\":null,\"multiEntitySupport\":false},\"actualParentEntityId\":{\"name\":\"actualParentEntityId\",\"multiEntitySupport\":false},\"sourceEntityTypeId\":{\"name\":\"sourceEntityTypeId\",\"multiEntitySupport\":false},\"filePath\":{\"name\":\"filePath\",\"multiEntitySupport\":false},\"headerLabel\":{\"name\":\"headerLabel\",\"id\":11464,\"multiEntitySupport\":false},\"recipientMarkets\":{\"name\":\"recipientMarkets\",\"id\":11846,\"values\":[],\"options\":null,\"multiEntitySupport\":false},\"addendums\":{\"name\":\"addendums\",\"values\":[],\"multiEntitySupport\":false},\"services\":{\"name\":\"services\",\"id\":11550,\"values\":[{\"name\":\"Network MNS\",\"id\":1005,\"parentId\":1002}],\"options\":null,\"multiEntitySupport\":false},\"contractCountries\":{\"name\":\"contractCountries\",\"multiEntitySupport\":false},\"clauseTags\":{\"name\":\"clauseTags\",\"id\":7382,\"multiEntitySupport\":false},\"parentEntityTypeId\":{\"name\":\"parentEntityTypeId\",\"values\":183,\"multiEntitySupport\":false},\"maxTcv\":{\"name\":\"maxTcv\",\"id\":7381,\"multiEntitySupport\":false},\"recipientCompanyCodes\":{\"name\":\"recipientCompanyCodes\",\"multiEntitySupport\":false},\"oldSystemId\":{\"name\":\"oldSystemId\",\"multiEntitySupport\":false},\"shortCodeId\":{\"name\":\"shortCodeId\",\"id\":7393,\"values\":\"CL08282\",\"multiEntitySupport\":false},\"formattingProperties\":{\"name\":\"formattingProperties\",\"id\":7388,\"multiEntitySupport\":false},\"excludeFromHoliday\":{\"name\":\"excludeFromHoliday\",\"values\":false,\"multiEntitySupport\":false},\"sourceEntityId\":{\"name\":\"sourceEntityId\",\"multiEntitySupport\":false},\"clauseText\":{\"name\":\"clauseText\",\"id\":7386,\"values\":{\"text\":\""+textInClause+"\",\"htmlText\":\"<style>.table-bordered, .table-bordered>tbody>tr>td, .table-bordered>tbody>tr>th, .table-bordered>tfoot>tr>td, .table-bordered>tfoot>tr>th, .table-bordered>thead>tr>td, .table-bordered>thead>tr>th{t border: 1px solid #ddd;      border-collapse: collapse;}.table {   width: 100%;    max-width: 100%;    margin-bottom: 20px;}</style><p>"+textInClause+"</p>\"},\"multiEntitySupport\":false},\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":138,\"multiEntitySupport\":false},\"comment\":{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null,\"multiEntitySupport\":false},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false},\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":null,\"multiEntitySupport\":false},\"invoiceCopy\":{\"name\":\"invoiceCopy\",\"values\":false,\"multiEntitySupport\":false},\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null,\"multiEntitySupport\":false},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"values\":[]}},\"relations\":{\"name\":\"relations\",\"multiEntitySupport\":false},\"category\":{\"name\":\"category\",\"id\":7368,\"values\":{\"name\":\"Limited Warranty\",\"id\":1200},\"options\":null,\"multiEntitySupport\":false},\"contractRegions\":{\"name\":\"contractRegions\",\"multiEntitySupport\":false},\"clauseVersion\":{\"name\":\"clauseVersion\",\"id\":12657,\"values\":\"1.0\",\"multiEntitySupport\":false},\"contractingCompanyCodes\":{\"name\":\"contractingCompanyCodes\",\"multiEntitySupport\":false},\"dyn106595\":{\"name\":\"dyn106595\",\"id\":106595,\"multiEntitySupport\":false},\"dyn106661\":{\"name\":\"dyn106661\",\"id\":106661,\"options\":null,\"multiEntitySupport\":false},\"dyn106560\":{\"name\":\"dyn106560\",\"id\":106560,\"multiEntitySupport\":false},\"dyn106576\":{\"name\":\"dyn106576\",\"id\":106576,\"multiEntitySupport\":false},\"dyn106614\":{\"name\":\"dyn106614\",\"id\":106614,\"multiEntitySupport\":false},\"dyn106645\":{\"name\":\"dyn106645\",\"id\":106645,\"multiEntitySupport\":false},\"dyn106678\":{\"name\":\"dyn106678\",\"id\":106678,\"options\":null,\"multiEntitySupport\":false}}}}";
    }

    private int createClauseAndGetNewId(String textInClause, CustomAssert customAssert){
        int entityId = -1;
        String createResponse;
        try{
            Create create = new Create();
            create.hitCreate(entityName, clausePayload(textInClause));
            createResponse = create.getCreateJsonStr();
            if(ParseJsonResponse.validJsonResponse(createResponse)){
                JSONObject jsonObject = new JSONObject(createResponse);
                String createStatus = jsonObject.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Entity :  {}", createStatus);
                if (createStatus.equalsIgnoreCase("success"))
                    entityId = CreateEntity.getNewEntityId(createResponse, "clauses");
                logger.info("Id of the Entity Created is : {}",entityId);
            }else{
                logger.error("Create response for Clause is not a valid JSON");
                customAssert.assertTrue(false,"Create response for Clause is not a valid JSON");
            }
        }catch(Exception e){
            logger.error("Exception {} occurred while creating the Clause",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating the Clause");
        }
        return entityId;
    }

    private boolean performActionOnCDR(int entityId, String actionName, CustomAssert customAssert) {
        try {
            boolean actionFlag = new WorkflowActionsHelper().performWorkflowAction(Integer.parseInt(entityTypeId), entityId, actionName);
            return actionFlag;
        } catch (Exception e) {
            logger.error("Exception {} occurred while performing action {} on Clause.", e.getMessage(), actionName.toUpperCase());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while performing action " + actionName.toUpperCase() + " on Clause.");
            return false;
        }
    }

    private boolean downloadClauseListing(String entityName, CustomAssert customAssert){
        HashMap<String,String> formsMap = new HashMap<>();
        boolean downloadFlag = false;
        entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_url_id");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_type_id");
        DownloadListWithData downloadListWithData = new DownloadListWithData();
        try{
            formsMap.put("_csrf_token","null");
            formsMap.put("jsonData","{\"filterMap\":{\"entityTypeId\":"+Integer.parseInt(entityTypeId)+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1643\",\"name\":\"Approved\"}]},\"filterId\":6,\"filterName\":\"status\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}}");
            HttpResponse downloadResponse =  downloadListWithData.hitDownloadListWithData( formsMap, Integer.parseInt(entityURLId));
            Thread.sleep(5000);
            if(downloadListWithData.dumpDownloadListIntoFile(downloadResponse,outputPath,outputFileName)){
                downloadFlag = true;
            }else{
                logger.error("Download could not be done.");
                customAssert.assertTrue(false,"Download could not be done.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while downloading the listing.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while downloading the listing.");
        }
        return downloadFlag;
    }

    private boolean readExcelFile(CustomAssert customAssert){
        boolean versionFlag = false;
        try{
            File file = new File(outputPath + outputFileName);
            FileInputStream fis = new FileInputStream(file);
            ZipSecureFile.setMinInflateRatio(-1.0d);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet("Data");
            int totalRows = sheet.getLastRowNum();
            int totalCols = sheet.getRow(0).getLastCellNum();
            DataFormatter dataFormatter = new DataFormatter();

            Object[][] dataOfExcelSheet = new Object[totalRows][totalCols];
            int rowId = 0;

            for (int rowIndex = 0; rowIndex < totalRows - 1; rowIndex++) {
                for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                    try {
                        Cell cell = sheet.getRow(rowIndex).getCell(colIndex);
                        dataOfExcelSheet[rowIndex][colIndex] = dataFormatter.formatCellValue(cell);
                    } catch (Exception ex) {
                        dataOfExcelSheet[rowIndex][colIndex] = null;
                    }
                }
            }

            for (int rowIndex = rowId; rowIndex < totalRows - 1; rowIndex++) {
                for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                    String value = dataOfExcelSheet[rowIndex][colIndex].toString();
                    if (value.equalsIgnoreCase("VERSION NUMBER")) {
                        versionFlag = true;
                        break;
                    }
                }
                if(versionFlag){
                    break;
                }
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while reading the excel file.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while reading the excel file.");
        }
        return versionFlag;
    }

    private double getVersionNumber(int entityId, CustomAssert customAssert){
        Show show = new Show();
        double version = -1;
        entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_url_id");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entity_type_id");
        try{
            show.hitShow(Integer.parseInt(entityTypeId),entityId);
            String showResponse = show.getShowJsonStr();
            if(ParseJsonResponse.validJsonResponse(showResponse)){
                version =Double.parseDouble(new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("clauseVersion").getString("values"));
            }else{
                logger.error("Show Response is not a valid JSON.");
                customAssert.assertTrue(false,"Show Response is not a valid JSON.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while fetching the version number of the Clause.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while fetching the version number of the Clause.");
        }
        return version;
    }

    private boolean editTheText(int entityId, String originalText, String replacementText, CustomAssert customAssert){
        boolean editFlag = false;
        Edit edit = new Edit();
        try{
            String editGetResponse = edit.getEditPayload(entityName,entityId);
            if(ParseJsonResponse.validJsonResponse(editGetResponse)){
                JSONObject editGetJSON = new JSONObject(editGetResponse);
                String htmlText = editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").getString("htmlText").replace(originalText,replacementText);
                String text = editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").getString("text").replace(originalText,replacementText);
                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText",htmlText);
                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("text",text);

                String editPostPayload = editGetJSON.toString();
                edit.hitEdit(entityName,editPostPayload);
                String editPostResponse = edit.getEditDataJsonStr();
                if(ParseJsonResponse.validJsonResponse(editPostResponse)){
                    if(new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")){
                        editFlag = true;
                    }else{
                        logger.error("Editing the text is unsuccessful.");
                        customAssert.assertTrue(false,"Editing the text is unsuccessful.");
                    }
                }else{
                    logger.error("Edit POST Response is not a valid JSON.");
                    customAssert.assertTrue(false,"Edit POST Response is not a valid JSON.");
                }
            }else{
                logger.error("Edit GET Response is not a valid JSON.");
                customAssert.assertTrue(false,"Edit GET Response is not a valid JSON.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while editing the text of the Clause.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while editing the text of the Clause.");
        }
        return editFlag;
    }

    private boolean getListingColumns(String viewName, CustomAssert customAssert){
        ListRendererListData listRendererListData = new ListRendererListData();
        boolean versionFlag = false;
        String payload = "";
        if(viewName.equalsIgnoreCase("grid")){
            payload = "{\"filterMap\":{\"offset\":0,\"size\":10,\"orderByColumnName\":null,\"orderDirection\":null,\"entityTypeId\":"+Integer.parseInt(entityTypeId)+",\"hasDefinitionCategoryIds\":true,\"filterJson\":{},\"isApprovedClauses\":null,\"currentClauseId\":null,\"selectedClauses\":[],\"searchName\":\"\",\"agreement_type_id\":[],\"startsWith\":\"\",\"onlyMandatory\":null}}";
        }else if(viewName.equalsIgnoreCase("list")){
            payload = "{\"filterMap\":{\"entityTypeId\":"+Integer.parseInt(entityTypeId)+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":18258,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":12150,\"columnQueryName\":\"name\"}]}";
        }
        listRendererListData.hitListRendererListDataV2isFirstCall(Integer.parseInt(entityURLId),payload);
        String listingResponse  = listRendererListData.getListDataJsonStr();
        if(ParseJsonResponse.validJsonResponse(listingResponse)){
            JSONArray responseJSONArray = new JSONObject(listingResponse).getJSONArray("data");
            for(int index=0;index<responseJSONArray.length();index++){
                try{
                    for(String key : responseJSONArray.getJSONObject(index).keySet()){
                        try{
                            if(responseJSONArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("clause_version")){
                                versionFlag = true;
                                break;
                            }
                        }catch (Exception e){
                            continue;
                        }
                    }
                    if(versionFlag)
                        break;
                }catch (Exception e){
                    continue;
                }
            }
        }else{
            logger.error("Listing response is not a valid JSON.");
            customAssert.assertTrue(false,"Listing response is not a valid JSON.");
        }
        return versionFlag;
    }

    private List<Integer> compareClause(int clauseId, CustomAssert customAssert){
        List<Integer> clauseVersionIds = new ArrayList<>();
        ClausePageData clausePageData = new ClausePageData();
        clausePageData.hitCompareClause(clauseId);
        String compareClauseResponse = clausePageData.getCompareClauseResponseStr();
        //String compareClauseResponse = executor.get("/tblclause/getAllVersion/"+clauseId, ApiHeaders.getDefaultLegacyHeaders()).getResponse().getResponseBody();
        if(ParseJsonResponse.validJsonResponse(compareClauseResponse)){
            JSONObject compareJSON = new JSONObject(compareClauseResponse);
            for(String key : compareJSON.getJSONObject("versions").keySet())
            clauseVersionIds.add(Integer.parseInt(compareJSON.getJSONObject("versions").getJSONObject(key).get("id").toString()));
        }else{
            logger.error("Compare Clause Response is not a valid JSON.");
            customAssert.assertTrue(false,"Compare Clause Response is not a valid JSON.");
        }
        return clauseVersionIds;
    }

    private int getMinimumValue(List<Integer> clauseVersionIds){
        int temp = clauseVersionIds.get(0);
        for(int version : clauseVersionIds){
            if(version<temp){
                temp = version;
            }
        }
        return temp;
    }

    private HashMap<Integer,List<String>> getClauseContents(List<Integer> clauseVersionIds, CustomAssert customAssert){
        HashMap<Integer,List<String>> versionContent = new HashMap<>();
        String payload = "";
        String response = "";
        try{
            for(int index=0; index<clauseVersionIds.size()-1;index++){
                for(int token=1; token<clauseVersionIds.size();token++){
                    payload = "{\"versionFrom\":"+clauseVersionIds.get(index)+",\"versionTo\":"+clauseVersionIds.get(token)+",\"sourceDocumentFileId\":"+getMinimumValue(clauseVersionIds)+"}";
                    response = executor.post("/tblclause/getCompareData", ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
                    if(ParseJsonResponse.validJsonResponse(response)){
                        JSONObject responseJSON = new JSONObject(response);
                        if(responseJSON.getBoolean("compare")){
                            for(int version : clauseVersionIds){
                                List<String> versionContentWords = new ArrayList<>();
                                try{
                                    for(String str : responseJSON.getJSONObject("clauseInfo").getJSONObject(""+version).getString("text").split(" ")){
                                        versionContentWords.add(str);
                                    }
                                    versionContent.put(version,versionContentWords);
                                }catch (Exception e){
                                    continue;
                                }
                            }
                        }else{
                            logger.error("Compare value is false.");
                            customAssert.assertTrue(false,"Compare value is false.");
                        }
                    }else{
                        logger.error("Compare Text Response is not a valid JSON.");
                        customAssert.assertTrue(false,"Compare Text Response is not a valid JSON.");
                    }
                }
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while comparing the text of two versions of the clause.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while comparing the text of two versions of the clause.");
        }
        return versionContent;
    }

    private boolean compareTextOfLists(HashMap<Integer,List<String>> versionContent,CustomAssert customAssert){
        boolean differenceFlag = false;
        try{
            Object[] versions = versionContent.keySet().toArray();
            for(int index=0;index<versions.length-1;index++){
                for(int token=1;token<versions.length;token++){
                    differenceFlag = versionContent.get(Integer.parseInt(versions[index].toString())).equals(versionContent.get(Integer.parseInt(versions[token].toString())));
                    if(differenceFlag){
                        break;
                    }
                }
                if(differenceFlag){
                    break;
                }
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while comparing two texts.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while comparing two texts.");
        }
        return differenceFlag;
    }

    private boolean checkCompareOnUI(int entityId, CustomAssert customAssert){
        Show show = new Show();
        boolean compareLink = false;
        try{
            show.hitShow(Integer.parseInt(entityTypeId),entityId);
            String showResponse = show.getShowJsonStr();
            if(ParseJsonResponse.validJsonResponse(showResponse)){
                JSONObject showJSON = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("compareAllowed");
                compareLink = showJSON.getBoolean("values");
            }else{
                logger.error("Show page response is not a valid JSON.");
                customAssert.assertTrue(false,"Show page response is not a valid JSON.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while getting the Compare link on show page.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while getting the Compare link on show page.");
        }
        return compareLink;
    }

    private boolean addTagInClause(int clauseId, String tagName, CustomAssert customAssert){
        String nullString = null;
        boolean addTag = false;
        Edit edit = new Edit();

        try{
            String editGetResponse = edit.getEditPayload(entityName,clauseId);
            if(ParseJsonResponse.validJsonResponse(editGetResponse)){
                JSONObject editJSON = new JSONObject(editGetResponse);
                int tagId = createTag(tagName, customAssert);
                String tagValue = "[{\"id\":"+tagId+",\"name\":\""+tagName+"\",\"tagHTMLType\":{\"id\":\"1\",\"name\":\"Text Field\"}}]";
                String htmlTextAdd = "<p><span class=\"\" contenteditable=\"true\"><span class=\"tag_"+tagId+" tag\" contenteditable=\"false\" htmltagtype=\"1\"><span style=\"display:none\">${"+tagId+":</span>"+tagName+"<span style=\"display:none\">}</span></span></span></p>";
                if(tagId!=-1){
                    for(String key : editJSON.getJSONObject("body").getJSONObject("data").keySet()){
                        try{
                            editJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options",nullString);
                        }catch (Exception e){
                            continue;
                        }
                    }

                    for(String key : editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").keySet()){
                        try{
                            editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(key).put("options",nullString);
                            editJSON.getJSONObject("body").getJSONObject("data").put(key,editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(key));
                        }catch (Exception e){
                            continue;
                        }
                    }

                    editJSON.getJSONObject("body").getJSONObject("data").remove("definitionCategory");

                    editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("subHeader").put("values",nullString);

                    editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("definitionCategories").remove("values");

                    editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").append("values",tagValue);

                    String payloadText = editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").getString("text");
                    editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("text",payloadText+"<br>"+tagName);

                    String payloadHTMLText = editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").getString("htmlText");
                    payloadHTMLText = "<p>"+payloadHTMLText+"</p>"+htmlTextAdd;
                    editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText",payloadHTMLText);


                    String editPayload = editJSON.toString();
                    edit.hitEdit(entityName,editPayload);
                    String editPostResponse = edit.getEditDataJsonStr();
                    if(ParseJsonResponse.validJsonResponse(editPostResponse)){
                        if(new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")){
                            addTag = true;
                        }
                    }else{
                        logger.error("Edit Post response is not a valid JSON.");
                        customAssert.assertTrue(false,"Edit Post response is not a valid JSON.");
                    }
                }else{
                    logger.error("Tag is not created.");
                    customAssert.assertTrue(false,"Tag is not created.");
                }
            }else{
                logger.error("Edit Get Response is not a valid JSON.");
                customAssert.assertTrue(false,"Edit Get Response is not a valid JSON.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while adding Tag in Clause.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while adding Tag in Clause.");
        }
        return addTag;
    }

    public int createTag(String tagName, CustomAssert customAssert){
        int tagId = -1;
        try{
            String tagPayload = "{\"name\":\""+tagName+"\",\"tagHTMLType\":{\"id\":1}}";
            String compareClauseResponse = executor.post("/clauseTag/create", ApiHeaders.getDefaultLegacyHeaders(),tagPayload).getResponse().getResponseBody();
            if(ParseJsonResponse.validJsonResponse(compareClauseResponse)){
                JSONObject tagJSON =  new JSONObject(compareClauseResponse).getJSONObject("data");
                if(tagJSON.getString("message").contains("successfully")){
                    tagId = Integer.parseInt(tagJSON.get("id").toString());
                }
            }else{
                logger.error("Create Tag response is not a valid JSON.");
                customAssert.assertTrue(false,"Create Tag response is not a valid JSON.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while creating the tag.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating the tag.");
        }
        return tagId;
    }
}