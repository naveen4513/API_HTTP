package com.sirionlabs.test.autoExtraction;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.autoExtraction.ContractShow;
import com.sirionlabs.api.autoExtraction.PartialResetAPI;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.DocumentShowPageHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class AEPorting {
    private final static Logger logger = LoggerFactory.getLogger(AEPorting.class);
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    static String contractCreationConfigFilePath;
    static String contractCreationConfigFileName;
    static String contractCreationConfigFileNameVfSandbox;
    private static JSONObject jsonObject;
    public static String newlyCreatedContractShowResponseStr;
    String templateFileName;
    static Integer docId, contractId;
    static String docName;
    static String fileExtension;
    static long duration = 0;
    static int fieldId;
    static String fieldName,COPO1,COPO2;
    static String fieldActualName;
    static String entity;
    static String entityForVFSandbox;
    private static String relationId;
    private static String relationIdForVFSandbox;
    private static String relationIdForVFProd;
    private String AuditLogConfigFilePath;
    private String AuditLogConfigFileName;
    private AuditLog auditlog;
    private String filterMap;
    private int initialCountOfDNO;
    private int finalCountOfDNO;
    private Show show = new Show();
    List<String> mappedDynamicFields;
    Map<Integer, String> allMappedDynamicFields;
    Map<Integer, String> staticFieldNameAndIds;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFilePath");
        contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileName");
        contractCreationConfigFileNameVfSandbox = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileNameVFSandbox");
        entity = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "entitiytocreate");
        entityForVFSandbox = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "entitiytocreate");
        relationId = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "contracts", "sourceid");
        relationIdForVFSandbox = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, "contracts", "sourceid");
        relationIdForVFProd = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, "contractsvfprod", "sourceid");
        AuditLogConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionTestAuditLogConfigFilePath");
        AuditLogConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionTestAuditLogConfigFileName");
        filterMap = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName, "filterjson", "61");
        auditlog = new AuditLog();

    }

    public static JSONObject ObligationListing() {
        CustomAssert csAssert = new CustomAssert();
        JSONObject dnoListDataObj = null;
        try {
            HttpResponse dnoListDataResponse = AutoExtractionHelper.dnoListDataResponse(4, "{\"filterMap\":{}}");
            csAssert.assertTrue(dnoListDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is invalid for DNO listing");
            String dnoListDataStr = EntityUtils.toString(dnoListDataResponse.getEntity());
            dnoListDataObj = new JSONObject(dnoListDataStr);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Hitting list data API for DNO" + e.getMessage());

        }
        return dnoListDataObj;
    }

    public static JSONObject contractShowResponse() throws IOException {
        String apiPath = ContractShow.getAPIPath();
        apiPath = String.format(apiPath + contractId);
        HttpGet httpGet = new HttpGet(apiPath);
        httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");
        httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        HttpResponse newlyCreatedContractShowResponse = APIUtils.getRequest(httpGet);
        newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());
        jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);
        return jsonObject;
    }

    public static boolean getExtractionStatus(String endUserName, String endUserPassword) throws IOException, InterruptedException {
        boolean isExtractionCompleted = false;
        Check check = new Check();
        CustomAssert csAssert = new CustomAssert();
        check.hitCheck(endUserName, endUserPassword);
        LocalTime initialTime = LocalTime.now();
        Thread.sleep(20000);
        try {
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse listingResponse = AutoExtractionHelper.duplicateDataFilter();
            csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200, "Response code is Invalid");
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson = new JSONObject(listingResponseStr);
            Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
            try {
                for (String key : keys) {
                    if (listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("status")) {
                        int status = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1]);
                        while (status != 4) {
                            LocalTime finalTime = LocalTime.now();
                            duration = duration + Duration.between(initialTime, finalTime).getSeconds();
                            logger.info("Waiting for Extraction to complete Wait Time = " + duration + " seconds");
                            if (duration > 600) {
                                Assert.fail("Extraction is working slow waited for 10 minutes.Please look manually whether their is problem in extraction or services are working slow." + "Waited for:" + duration + "seconds for the document completion");
                            } else {
                                isExtractionCompleted = getExtractionStatus(endUserName, endUserPassword);
                                if (isExtractionCompleted == true) {
                                    return isExtractionCompleted;
                                }
                            }
                        }
                        if (status == 4) {
                            isExtractionCompleted = true;
                            duration = 0;
                            logger.info("Extraction Completed");
                            return isExtractionCompleted;
                        }

                        break;
                    }

                }

            } catch (Exception e) {
                logger.error("Exception while hitting Project Listing API. {}", e.getMessage());

            }
        } catch (Exception e) {
            logger.error("Exception while hitting Automation List Data API. {}", e.getMessage());
        } finally {
            duration = 0;
        }
        return isExtractionCompleted;
    }

    @Parameters("Environment")
    @Test(priority = 1,enabled = true)
    public void TestAEDocumentUploadFromContracts(String environment) {
        CustomAssert csAssert = new CustomAssert();
        try {
            JSONObject dnoListDataObj = AEPorting.ObligationListing();
            initialCountOfDNO = (int) dnoListDataObj.get("filteredCount");
            String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docx attachment", "fileuploadpath");
            templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docx attachment", "fileuploadname");
            if (environment.equals("autoextraction")) {
                contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileName, entity, templateFilePath, templateFileName, relationId, true);
            }
            else if(environment.equals("Sandbox/AEVF"))
            {
                contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, entityForVFSandbox, templateFilePath, templateFileName, relationIdForVFSandbox, true);
            }

            if (contractId == -1) {
                throw new Exception("Couldn't Create Contract. Hence couldn't validate further.");
            }
            logger.info("Checking whether Extraction Status is complete or not");
            if (!(contractId == -1)) {
                boolean isExtractionCompletedForUploadedFile = AEPorting.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForUploadedFile, "Extraction not Completed");
                if (isExtractionCompletedForUploadedFile) {
                    logger.info("Getting Contract Show page Data");
                    jsonObject = contractShowResponse();
                    docName = (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("name");
                    docId = (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
                    fileExtension = (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("extension");
                    HttpResponse listingResponse = AutoExtractionHelper.duplicateDataFilter();
                    String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
                    JSONObject listingResponseJson = new JSONObject(listingResponseStr);
                    Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
                    for (String key : keys) {
                        if (listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("documentname")) {
                            docId = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1].trim());
                            break;
                        }

                    }
                }

            }

        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Creating a Contract" + e.getMessage());
        }
        csAssert.assertAll();
    }

    public static String getProjectNameCorrespondingToDoc()
    {
        CustomAssert csAssert = new CustomAssert();
        String projectName = null;
        try {
            HttpResponse listingResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseObj = new JSONObject(listingResponseStr);
            int projectNameColumnId = ListDataHelper.getColumnIdFromColumnName(listingResponseStr,"projects");
            projectName = String.valueOf(listingResponseObj.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(projectNameColumnId)).get("value")).split(":;")[0];
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "AE listing API is not working because of :" + e.getStackTrace());

        }
        return projectName;
    }

    @Test(priority = 2, dependsOnMethods = "TestAEDocumentUploadFromContracts", enabled = true)
    public void TestAEContractPorting() {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Test Case to metadata porting is working fine in Contracts");
            Thread.sleep(20000);
            allMappedDynamicFields = new HashMap<>();
            JSONArray fieldsJsonArray = jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields")
                    .getJSONObject(0).getJSONArray("fields").getJSONObject(0).getJSONArray("fields");
            int totalFields = fieldsJsonArray.length();
            for (int i = 0; i < totalFields; i++) {
                fieldName = (String) fieldsJsonArray.getJSONObject(i).get("label");
                if (fieldName.equalsIgnoreCase("COPO1") || fieldName.equalsIgnoreCase("COPO2") || fieldName.equalsIgnoreCase("Contract Porting Status")
                        || fieldName.equalsIgnoreCase("Clause Text")) {
                    fieldId = (int) fieldsJsonArray.getJSONObject(i).get("id");
                    allMappedDynamicFields.put(fieldId, fieldName);
                }

            }
            try {
                List<String> metadataValue = new LinkedList<>();
                mappedDynamicFields = new LinkedList<>();
                logger.info("Checking the value against the mapped dynamic fields");
                JSONObject dynamicMetadataJson = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                for (Map.Entry<Integer, String> entry : allMappedDynamicFields.entrySet()) {
                    mappedDynamicFields.add(entry.getValue());
                    fieldName = entry.getValue();
                    fieldId = entry.getKey();
                    String dynamicFieldSystemName = "dyn" + fieldId;
                    metadataValue.add((String) dynamicMetadataJson.getJSONObject(dynamicFieldSystemName).get("values"));
                    logger.info("Metadata Value for fieldName " + fieldName + " and fieldId: " + fieldId + "is: " + metadataValue);
                }
                csAssert.assertEquals(metadataValue.size(), 4, "Metadata has not been ported in all three mapped fields");
            } catch (Exception e) {
                logger.info("Exception occured while hitting contract show API");
                csAssert.assertTrue(false, e.getMessage());
            }

        } catch (Exception e) {
            logger.info("Exception occured while hitting Contract show API");
            csAssert.assertTrue(false, e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(priority = 3,enabled = true, dependsOnMethods = "TestAEDocumentUploadFromContracts")
    public void TestAuditLogAfterPorting() {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Checking Audit Log of Contract After successful porting of metadata for Contract Id: " + contractId);
            String auditLog_response = auditlog.hitAuditLogDataApi(String.valueOf(61), String.valueOf(contractId), filterMap);
            JSONObject auditLogJson = new JSONObject(auditLog_response);
            int actionColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "action_name");
            String actionValue = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(actionColumnId)).getString("value");
            logger.info("Action name for contractId " + contractId + "is: " + actionValue);
            csAssert.assertEquals(actionValue, "Auto Update", "Showing an incorrect Action name after Contract Porting: " + actionValue);

            try {
                logger.info("Verify that View History section shows the ported metadata in mapped dynamic field");
                int historyColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "history");
                String historyURL = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(historyColumnId)).getString("value");
                HttpResponse viewHistoryAPIResponse = AutoExtractionHelper.viewHistoryAPI(historyURL);
                csAssert.assertTrue(viewHistoryAPIResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid for view History API");
                String viewHistoryStr = EntityUtils.toString(viewHistoryAPIResponse.getEntity());
                JSONObject viewHistoryJson = new JSONObject(viewHistoryStr);
                int totalModifiedData = viewHistoryJson.getJSONArray("value").length();
                List<String> modifiedFields = new LinkedList<>();
                String portingSuccessFlag = null;
                for (int i = 0; i < totalModifiedData; i++) {
                    JSONObject jsonObj = viewHistoryJson.getJSONArray("value").getJSONObject(i);
                    modifiedFields.add((String) jsonObj.get("property"));
                    if (jsonObj.get("property").equals("Contract Porting Status")) {
                        portingSuccessFlag = (String) jsonObj.get("newValue");
                    }

                }
                csAssert.assertTrue(modifiedFields.containsAll(mappedDynamicFields), "All the mapped dynamic fields are not getting updated after porting");
                logger.info("Check Contract Porting Flag value is True after successful Porting");
                csAssert.assertTrue(portingSuccessFlag.equalsIgnoreCase("Yes")||portingSuccessFlag.equals("true"),"Contract Porting flag value is"  +portingSuccessFlag);

            } catch (Exception e) {
                logger.info("Exception occured while hitting Contract View Field History API");
                csAssert.assertTrue(false, e.getMessage());
            }

        } catch (Exception e) {
            logger.info("Exception occured while hitting Contract Audit log API");
            csAssert.assertTrue(false, e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(priority = 4, dependsOnMethods = "TestAEDocumentUploadFromContracts",enabled = true)
    public void PortingDNOCreation() {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Waiting for complete DNO creation as auto-porting takes few seconds to complete");
            Thread.sleep(30000);
            JSONObject dnoListDataObj = AEPorting.ObligationListing();
            finalCountOfDNO = (int) dnoListDataObj.get("filteredCount");
            logger.info("Final count of DNO:" +finalCountOfDNO);
            try {
                logger.info("Check the Total count of obligation extracted in a Document in Obligation Tab");
                String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]}," +
                        "\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"386\":{\"filterId\":\"386\"," +
                        "\"filterName\":\"score\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"0\"}}},\"entityId\":"+docId+"}";

                HttpResponse obligationTabResponse = AutoExtractionHelper.getTabData(payload, 437);
                csAssert.assertTrue(obligationTabResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid for Ob tab API");
                String obligationTabDataStr = EntityUtils.toString(obligationTabResponse.getEntity());
                JSONObject obligationTabJson = new JSONObject(obligationTabDataStr);
                int totalExtractedDNO = obligationTabJson.getJSONArray("data").length();
                logger.info("Checking the DNO create by the system");
                /*Subtracted one DNO from the initial count because one of the clause text has been marked as False- Positive
                Test Case Id : C153744 : Verify that if a clause has been marked as False-positive then the system should not
                create an obligation for that clause*/
                csAssert.assertEquals(finalCountOfDNO, initialCountOfDNO + totalExtractedDNO-1, "Mismatch in DNO auto-created via AE and DNO extracted within a document" +
                        "Extracted DNO count: " + totalExtractedDNO + " DNO Created by the System " + (finalCountOfDNO - initialCountOfDNO));
            } catch (Exception e) {
                logger.info("Exception occured while hitting AE-DNO Tab Data API");
                csAssert.assertTrue(false, e.getMessage());
            }
        } catch (Exception e) {
            logger.info("Exception occured while hitting DNO ListData API");
            csAssert.assertTrue(false, e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(priority = 5,enabled = true, dependsOnMethods = "TestAEDocumentUploadFromContracts")
    public void ValidateMetadataAfterDNOPorting() {
        CustomAssert csAssert = new CustomAssert();
        try {
            int entityTypeId = 12;
            JSONObject dnoListDataObj = AEPorting.ObligationListing();
            String dnoListStr = dnoListDataObj.toString();
            int obColumnId = ListDataHelper.getColumnIdFromColumnName(dnoListStr, "id");
            int entityId = Integer.parseInt(dnoListDataObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(obColumnId)).getString("value").split(":;")[1]);
            show.hitShowGetAPI(entityTypeId, entityId);
            String obligationShow = show.getShowJsonStr();
            JSONObject obligationObj = new JSONObject(obligationShow);
            Map<Integer, String> dnoMappedDynamicFields = new HashMap<>();
            JSONArray fieldsJsonArray = obligationObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent")
                    .getJSONArray("fields").getJSONObject(0).getJSONArray("fields");
            staticFieldNameAndIds = new HashMap<>();
            int totalSections = fieldsJsonArray.length();
            for (int j = 0; j < totalSections; j++) {
                int totalFieldsInEachSection = fieldsJsonArray.getJSONObject(j).getJSONArray("fields").length();
                for(int i =0;i<totalFieldsInEachSection;i++) {
                    fieldName = (String) fieldsJsonArray.getJSONObject(j).getJSONArray("fields").getJSONObject(i).get("label");
                    if (fieldName.equalsIgnoreCase("Performance Type") || fieldName.equalsIgnoreCase("DNO Porting Status")) {
                        fieldId = (int) fieldsJsonArray.getJSONObject(j).getJSONArray("fields").getJSONObject(i).get("id");
                        fieldActualName = (String) fieldsJsonArray.getJSONObject(j).getJSONArray("fields").getJSONObject(i).get("name");
                        dnoMappedDynamicFields.put(fieldId, fieldName);
                        staticFieldNameAndIds.put(fieldId, fieldActualName);
                    }
                }
                break;
            }
            try {
                logger.info("Checking the value against the mapped dynamic fields");
                JSONObject staticMetadataDataJson = obligationObj.getJSONObject("body").getJSONObject("data");
                String dnoPortingFlagValue = null;
                String staticFieldValue = null;
                for (Map.Entry<Integer, String> entry : staticFieldNameAndIds.entrySet()) {
                    fieldName = entry.getValue();
                    fieldId = entry.getKey();
                    if(fieldName.equals("outputType")) {
                        staticFieldValue = (String) staticMetadataDataJson.getJSONObject(fieldName).getJSONObject("values").get("name");
                    }
                    else {
                        dnoPortingFlagValue = (String) staticMetadataDataJson.getJSONObject("dynamicMetadata").getJSONObject(fieldName).get("values");
                    }
                    logger.info("Metadata Value for fieldName " + fieldName + " and fieldId: " + fieldId + "is: " + staticFieldValue);
                }
                csAssert.assertTrue(!staticFieldValue.isEmpty(),"No Value has been ported in static field named as Performance Type");
                csAssert.assertEquals(dnoPortingFlagValue, "true", "DNO Porting status is False");

                //Test Case to validate References for DNO
                String contractReferenceName = (String) obligationObj.getJSONObject("body").getJSONObject("data").getJSONObject("pageReference").getJSONArray("values").getJSONObject(0)
                        .getJSONObject("contractReference").get("name");
                csAssert.assertEquals(contractReferenceName, templateFileName, "DNO Reference is incorrect " + contractReferenceName + "Actual Reference " + templateFileName);



            } catch (Exception e)
            {
                logger.info("Exception occured while hitting DNO Show page API");
                csAssert.assertTrue(false, e.getMessage());
            }

        } catch (Exception e) {

            logger.info("Exception occured while hitting DNO Listing API");
            csAssert.assertTrue(false, e.getMessage());
        }

        csAssert.assertAll();
    }

    @Test(priority = 6,dependsOnMethods = "TestAEDocumentUploadFromContracts",enabled = true)
    public void TestReferencesForDocs() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        List<Integer> fieldIds = new LinkedList<>();
        try {
            logger.info("Check References for the mapped dynamic fields of contract");
            for (Map.Entry<Integer, String> entry : allMappedDynamicFields.entrySet()) {
                fieldId = entry.getKey();
                fieldIds.add(fieldId);
            }
            HttpResponse referencesResponse = AutoExtractionHelper.referencesContractPorting(contractId,fieldIds.get(0));
            csAssert.assertTrue(referencesResponse.getStatusLine().getStatusCode()==200, "Response code is Invalid");
            String referencesStr = EntityUtils.toString(referencesResponse.getEntity());
            JSONArray referencesObj = new JSONArray(referencesStr);
            int referencesCount = referencesObj.length();
            if(referencesCount>0) {
                String referenceDocumentName = (String) referencesObj.getJSONObject(0).getJSONObject("contractReference").get("name");
                String actualDocName = referenceDocumentName + ".docx";
                csAssert.assertEquals(actualDocName,templateFileName,"Not getting the same reference that is being uploaded by the user");
            }

        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting References API");
            csAssert.assertTrue(false, e.getMessage());
        }

        csAssert.assertAll();
    }
    /* Test Case Id:C153738- Verify that if a user add a new metadata value and trigger porting
    against the document then new value should get ported successfully*/
    @Test(priority = 7,enabled = true,dependsOnMethods = "TestAEDocumentUploadFromContracts")
    public void EditMetadataAndTriggerPorting()
    {
        logger.info("Test Case to validate when user edit/Insert a metadata then the updated value should be ported");
        CustomAssert csAssert = new CustomAssert();
        boolean isNewlyAddedMetadataPresent = false;
        try {
            logger.info("Checking the Data in Metadata Tab of " + docId);
            DocumentShowPageHelper crudHelper = new DocumentShowPageHelper(String.valueOf(docId));
            logger.info("Getting Category Id to edit from metadata tab");
            String categoryId = crudHelper.getClauseIdLinkedToMetadata();
            logger.info("Getting Text Id from metadata tab");
            String textId = crudHelper.getMetadataTextId();
            logger.info("Getting field Id for Performing create operation");

            try {
                logger.info("Now Performing Create Operation for Metadata");
                HttpResponse metadataCreateResponse = AutoExtractionHelper.metadataCreateOperation(textId, categoryId, docId, "12485");
                csAssert.assertTrue(metadataCreateResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String metadataResponseStr = EntityUtils.toString(metadataCreateResponse.getEntity());
                JSONObject metadataJson = new JSONObject(metadataResponseStr);
                String createResponseMessage = (String) metadataJson.getJSONObject("response").get("message");
                csAssert.assertEquals(createResponseMessage, "Action completed successfully");

                if(ParseJsonResponse.validJsonResponse(metadataResponseStr))
                {
                    try{
                        HttpResponse initiatePortingResponse = AutoExtractionHelper.initiatePorting(docId);
                        csAssert.assertTrue(initiatePortingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                        String initiatePortingStr = EntityUtils.toString(initiatePortingResponse.getEntity());
                        csAssert.assertEquals(initiatePortingStr,"Porting api triggered","Porting Trigger API is not working getting the message : " + initiatePortingStr);
                        try {
                            logger.info("Wait for porting to complete");
                            boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                            logger.info("Extraction Completed:"+isExtractionCompleted);
                            csAssert.assertTrue(isExtractionCompleted=true, "Extraction not Completed for the documents");
                            logger.info("Getting Contract Show page Data");
                            jsonObject = contractShowResponse();
                            List<String> metadataValue = new LinkedList<>();
                            JSONObject dynamicMetadataJson = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                            for (Map.Entry<Integer, String> entry : allMappedDynamicFields.entrySet()) {
                                mappedDynamicFields.add(entry.getValue());
                                fieldName = entry.getValue();
                                int fieldIdForContracts = entry.getKey();
                                String dynamicFieldSystemName = "dyn" + fieldIdForContracts;
                                metadataValue.add((String) dynamicMetadataJson.getJSONObject(dynamicFieldSystemName).get("values"));
                                if(fieldName.equalsIgnoreCase("COPO1"))
                                {
                                    COPO1 = (String) dynamicMetadataJson.getJSONObject(dynamicFieldSystemName).get("values");
                                }
                                else if(fieldName.equalsIgnoreCase("COPO2"))
                                {
                                    COPO2 = (String) dynamicMetadataJson.getJSONObject(dynamicFieldSystemName).get("values");
                                }
                                logger.info("Metadata Value for fieldName " + fieldName + "is: " + metadataValue);
                            }
                            String newlyInsertedMetadataValue = "API Automation Metadata Create Operation";
                            for(int i=0;i<metadataValue.size();i++)
                            {
                                String listOfElements = metadataValue.get(i);

                                String[] singleValue = null;
                                if(listOfElements.contains("||"))
                                {
                                    singleValue = listOfElements.split("\\|\\|");
                                    for(int j=0;j<singleValue.length;j++)
                                    {
                                        String actualValue = singleValue[j].trim();
                                        if(actualValue.equalsIgnoreCase(newlyInsertedMetadataValue))
                                        {
                                            isNewlyAddedMetadataPresent=true;
                                            break;
                                        }
                                    }
                                }
                            }
                            csAssert.assertTrue(isNewlyAddedMetadataPresent,"Newly Added metadata is not updated in Contracts after porting for document Id: "+docId );
                            /*Test Case Id: C153745: Verify that it shows Double pipe "||" as a separator for successfully ported metadata*/
                            logger.info("Test Case to verify it is showing a separator || for successfully ported metadata");
                            csAssert.assertTrue(COPO1.contains("||") || COPO2.contains("||"),"Separator || is not present in metadataValue");

                        }
                        catch (Exception e)
                        {
                            csAssert.assertTrue(false, "Contract Show API is not working" + e.getMessage());

                        }

                    }
                    catch (Exception e)
                    {
                        csAssert.assertTrue(false, "Trigger Porting API is not working" + e.getMessage());

                    }

                }
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false, "Metadata Create API is not working" + e.getMessage());

            }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "List Data API is not working for Metadata Tab" + e.getMessage());

        }
        csAssert.assertAll();
    }

    @Parameters("Environment")
    @Test(priority = 8, dependsOnMethods = "TestAEDocumentUploadFromContracts")
    public void SortOrderByPageNo(String environment)
    {
        CustomAssert csAssert = new CustomAssert();
        String portedMetadataValuesForParties = null;
        logger.info("Verify that it shows sort order by Page Number Instead of Score after porting");
        if (environment.equals("autoextraction")) {
            portedMetadataValuesForParties = COPO1;
        }
        else if(environment.equals("Sandbox/AEVF"))
        {
            portedMetadataValuesForParties = COPO2;
        }

        try {
            boolean tabAndPortedMetadataOrder=false;
            logger.info("Hitting the Tab List Data API for Metadata to get the values of Field Parties");
            logger.info("Checking the Data in Metadata Tab of " + docId);
            DocumentShowPageHelper crudHelper = new DocumentShowPageHelper(String.valueOf(docId));
            String metadataStr = crudHelper.getMetadataTabResponse(String.valueOf(docId));
            JSONObject metadataJson = new JSONObject(metadataStr);
            int columnIdForMetadata = ListDataHelper.getColumnIdFromColumnName(metadataStr,"fieldname");
            int columnIdForPageNo = ListDataHelper.getColumnIdFromColumnName(metadataStr,"pageno");
            int columnIdForExtractedText = ListDataHelper.getColumnIdFromColumnName(metadataStr,"extractedtext");
            logger.info("Storing the elements in Multimap to allow duplicate keys in sorted order");
            Multimap<String, String> fieldValueAndPageNo = ArrayListMultimap.create();
            int totalData = metadataJson.getJSONArray("data").length();
            for(int i=0;i<totalData;i++)
            {
                String metadataFieldName = metadataJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnIdForMetadata)).get("value").toString().split(":;")[0];
                if(metadataFieldName.equalsIgnoreCase("Parties"))
                {
                    String fieldValue = (String) metadataJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnIdForExtractedText)).get("value");
                    String pageNo = (String) metadataJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnIdForPageNo)).get("value");
                    boolean exists = fieldValueAndPageNo.containsValue(fieldValue);
                    if(!exists) {
                        fieldValueAndPageNo.put(pageNo, fieldValue);
                    }
                }

            }
            Collection<String> extractedValuesOfTab = fieldValueAndPageNo.values();
            List<String> extractedValues = extractedValuesOfTab.stream().collect(Collectors.toList());
            extractedValues.remove("API Automation Metadata Create Operation");
            String[] portedMetadataValues = portedMetadataValuesForParties.split("\\|\\|");
            logger.info("Now comparing values extracted by system is ported in a sorting order by page no.");
            for(int i=0;i<extractedValues.size();i++)
            {
                    if(extractedValues.get(i).equalsIgnoreCase(portedMetadataValues[i].trim()))
                    {
                        tabAndPortedMetadataOrder = true;
                        logger.info("First element of Metadata Tab "+extractedValues.get(i) + " = first element of dynamic Field " +portedMetadataValues[i]);
                    }

            }
            csAssert.assertTrue(tabAndPortedMetadataOrder,"Insertion order of metadata is not sorted by page no.");
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Tab List Data API is not working for Metadata Tab" + e.getMessage());

        }

        csAssert.assertAll();
    }

    @Test(priority = 9, dependsOnMethods = "TestAEDocumentUploadFromContracts")
    public void checkProjectOfUploadedDoc()
    {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Verify that the Project Name for the document uploaded under a project in Contract Tree should be same in AE listing : against the Jira:AE-3287 ");
        try{
            String actualProjectName = getProjectNameCorrespondingToDoc();
            csAssert.assertEquals(actualProjectName,"TestDeDuplicationServiceWMsX2","Mismatch in Project Name Uploaded under contracts and in AE listing");
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "AE Listing API is not working because : " + e.getMessage());

        }
        csAssert.assertAll();
    }
    @Test(priority = 10, dependsOnMethods = "TestAEDocumentUploadFromContracts")
    public void testPartialPortingForContract()
    {
        CustomAssert csAssert = new CustomAssert();
        /* Test Case Id: C154058: Verify that if a user has reset the metadata partially then it should port the selected metadata */
        try{
            logger.info("Checking the Data in Metadata Tab of " + docId);
            DocumentShowPageHelper crudHelper = new DocumentShowPageHelper(String.valueOf(docId));
            logger.info("Getting Category Id to edit from metadata tab");
            String categoryId = crudHelper.getClauseIdLinkedToMetadata();
            logger.info("Getting Text Id from metadata tab");
            String textId = crudHelper.getMetadataTextId();
            logger.info("Getting field Id for Performing create operation");

            try {
                logger.info("Now Performing Create Operation for Metadata");
                HttpResponse metadataCreateResponse = AutoExtractionHelper.metadataCreateOperation(textId, categoryId, docId, "12486");
                csAssert.assertTrue(metadataCreateResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String metadataResponseStr = EntityUtils.toString(metadataCreateResponse.getEntity());
                JSONObject metadataJson = new JSONObject(metadataResponseStr);
                String createResponseMessage = (String) metadataJson.getJSONObject("response").get("message");
                csAssert.assertEquals(createResponseMessage, "Action completed successfully");

                try{
                    logger.info("Now Reset the document Partially");
                    APIResponse partialResetResponse = PartialResetAPI.partialResetAPIResponse(PartialResetAPI.getAPIPath(),PartialResetAPI.getHeaders(),PartialResetAPI.getPayload(String.valueOf(docId),"12486"));
                    Integer partialResetResponseCode = partialResetResponse.getResponseCode();
                    csAssert.assertTrue(partialResetResponseCode==200,"Response Code is Invalid for Partial Reset API");
                    String partialResetStr = partialResetResponse.getResponseBody();
                    JSONObject partialResetJson = new JSONObject(partialResetStr);
                    csAssert.assertTrue(partialResetJson.get("success").toString().equals("true"), "Partial Reset is not working");
                    if(ParseJsonResponse.validJsonResponse(partialResetStr))
                    {
                        logger.info("Now waiting for document completion for Document Id: "+docId);
                        boolean isExtractionCompletedForUploadedFile = AEPorting.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                        csAssert.assertTrue(isExtractionCompletedForUploadedFile, "Extraction not Completed");

                        if(isExtractionCompletedForUploadedFile)
                        {
                            try {
                                logger.info("Checking Audit Log of Contract After successful porting of metadata for Contract Id: " + contractId);
                                String auditLog_response = auditlog.hitAuditLogDataApi(String.valueOf(61), String.valueOf(contractId), filterMap);
                                JSONObject auditLogJson = new JSONObject(auditLog_response);
                                int actionColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "action_name");
                                String actionValue = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(actionColumnId)).getString("value");
                                logger.info("Action name for contractId " + contractId + "is: " + actionValue);
                                csAssert.assertEquals(actionValue, "Auto Update", "Showing an incorrect Action name after Contract Porting: " + actionValue);
                                try{
                                    logger.info("Verify that View History section shows the ported metadata in mapped dynamic field");
                                    int historyColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "history");
                                    String historyURL = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(historyColumnId)).getString("value");
                                    HttpResponse viewHistoryAPIResponse = AutoExtractionHelper.viewHistoryAPI(historyURL);
                                    csAssert.assertTrue(viewHistoryAPIResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid for view History API");
                                    String viewHistoryStr = EntityUtils.toString(viewHistoryAPIResponse.getEntity());
                                    JSONObject viewHistoryJson = new JSONObject(viewHistoryStr);
                                    int totalModifiedData = viewHistoryJson.getJSONArray("value").length();
                                    csAssert.assertEquals(totalModifiedData,1,"Showing an incorrect count in view history after partial porting");
                                    logger.info("Now Checking after partial porting value has been ported successfully in the mapped field");
                                    String metadataFieldValue = null;
                                    Boolean isUpdatedMetadataPorted = false;
                                    if(totalModifiedData==1)
                                    {
                                        String newValue = viewHistoryJson.getJSONArray("value").getJSONObject(0).get("newValue").toString();
                                        String[] updatedMetadataValue = newValue.split("\\|\\|");
                                        for(int i=0;i<updatedMetadataValue.length;i++)
                                        {
                                            metadataFieldValue=updatedMetadataValue[i].trim();
                                            if(metadataFieldValue.equalsIgnoreCase("API Automation Metadata Create Operation"))
                                            {
                                                isUpdatedMetadataPorted=true;
                                            }
                                        }

                                    }
                                    csAssert.assertTrue(isUpdatedMetadataPorted,"Updated metadata value has not been ported after partial Porting");
                                }
                                catch (Exception e)
                                {
                                    csAssert.assertTrue(false,"Exception occured while hitting Field History API: "+e.getMessage());

                                }

                            }
                            catch (Exception e)
                            {
                                csAssert.assertTrue(false,"Audit Log API for contracts is not working: "+e.getMessage());

                            }
                        }

                    }

                }
                catch (Exception e)
                {
                    csAssert.assertTrue(false,"{Partial Reset is not working: "+e.getMessage());

                }
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false,"Metadata Create Operation is not working: "+e.getMessage());

            }
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "List Data API is not working for Metadata Tab" + e.getMessage());
        }
        finally {
            //Delete Contract
            logger.info("Deleting Newly Created Contract Entity");
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
        csAssert.assertAll();

    }

}
