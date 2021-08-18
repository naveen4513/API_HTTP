package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.autoExtraction.PartialResetAPI;
import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.DocumentShowPageHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AECDRPorting {
    private final static Logger logger = LoggerFactory.getLogger(AECDRPorting.class);
    static String parentConfigFilePath;
    static String parentConfigFileName;
    private static String configFilePath;
    private static String configFileName;
    int docCountInAEListing;
    boolean isExtractionCompletedForUploadedFile;
    int cdrId;
    static int fieldId;
    static String fieldName, Party;
    static Integer docId;
    Map<Integer, String> allMappedDynamicFields;
    List<String> mappedDynamicFields;
    private SubmitDraft submitDraftObj = new SubmitDraft();
    ShowHelper showHelper = new ShowHelper();
    private AuditLog auditlog;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("autoExtractionCDRCreationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("autoExtractionCDRCreationConfigFileName");
        parentConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("aeParentInfoConfigFilePath");
        parentConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("aeParentInforConfigFileName");
        auditlog = new AuditLog();
    }

    public static int getDocumentCountInAEListing()
    {
        CustomAssert csAssert = new CustomAssert();
        int docCount = 0;
        try {
            HttpResponse listingResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseObj = new JSONObject(listingResponseStr);
            docCount = (int) listingResponseObj.get("filteredCount");
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "AE listing API is not working because of :" + e.getStackTrace());

        }
        return docCount;
    }

    public static int getDocumentId() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        HttpResponse listingResponse = AutoExtractionHelper.aeDocListing();
        csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
        String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
        JSONObject listingResponseObj = new JSONObject(listingResponseStr);
        int columnId = ListDataHelper.getColumnIdFromColumnName(listingResponseStr, "documentname");
        JSONObject documentObj = listingResponseObj.getJSONArray("data").getJSONObject(0);
        String documentNameValue = documentObj.getJSONObject(Integer.toString(columnId)).getString("value");
        String[] documentId = documentNameValue.split(":;");
        docId = Integer.valueOf(documentId[1]);
        return docId;
    }

    public String auditLogAPIForCDR()
    {
        String filterMap = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
        String auditLog_response = auditlog.hitAuditLogDataApi(String.valueOf(160), String.valueOf(cdrId), filterMap);
        JSONObject auditLogJson = new JSONObject(auditLog_response);
        int actionColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "action_name");
        String actionValue = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(actionColumnId)).getString("value");
        return actionValue;
    }


    @Test(priority = 1)
    public void testCDRPorting() {
        CustomAssert csAssert = new CustomAssert();
        try {
            /* C153853: Verify that document should get uploaded successfully in AE listing after selecting the "Extract Metadata" checkbox.
            C153854: Verify that document uploaded via CDR should get completed in AE listing */

            logger.info("Getting the Initial Count of Documents in AE listing");
            docCountInAEListing = getDocumentCountInAEListing();
            logger.info("Initial Count of Documents in AE listing is: "+docCountInAEListing);
            logger.info("Now Creating a new CDR to validate CDR Porting Feature");
            String contractDraftRequestResponseString = ContractDraftRequest.createCDR(parentConfigFilePath, parentConfigFileName, configFilePath, configFileName,
                    "c89079 cdr creation", true);
            cdrId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);
            logger.info("Newly Created CDR Id : " + cdrId);
            // File Upload API to get the key of file uploaded
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            File fileToUpload = new File(System.getProperty("user.dir") + "\\src\\test\\resources\\TestConfig\\AutoExtraction\\UploadFiles\\KAMADALTD_DRSADraftR_3252013.docx");

            // Upload a file in Contract Document Tab of CDR
            String fileUploadDraftResponse = PreSignatureHelper.fileUploadDraftWithNewDocument("KAMADALTD_DRSADraftR_3252013", "docx", randomKeyForFileUpload, "160", String.valueOf(cdrId), fileToUpload);
            String showPageResponse = showHelper.getShowResponseVersion2(160, cdrId);
            JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
            JSONObject commentJsonObj = jsonData.getJSONObject("comment");
            JSONObject draftObj = commentJsonObj.getJSONObject("draft");
            draftObj.put("values", true);
            commentJsonObj.put("draft", draftObj);

            String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":null,\"documentSize\":88154," +
                    "\"key\":\"" + randomKeyForFileUpload + "\",\"documentStatusId\":1,\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false}," +
                    "\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false,\"triggerAutoExtraction\":true}]}}";

            commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));
            jsonData.put("comment", commentJsonObj);
            jsonData.put("comment", commentJsonObj);

            JSONObject finalPayload = new JSONObject();
            JSONObject body = new JSONObject();
            body.put("data", jsonData);
            finalPayload.put("body", body);
            String submitDraftPayload = finalPayload.toString();

            // submit file in Contract Document Tab of CDR
            HttpResponse submitFileDraftResponse = PreSignatureHelper.submitFileDraft(submitDraftPayload);
            String getStatus = EntityUtils.toString(submitFileDraftResponse.getEntity());
            JSONObject jsonObj = new JSONObject(getStatus);
            String newStatus = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
            String expectedStatus = "success";
            if (expectedStatus.equals(newStatus)) {
                logger.info("File upload successfully!");
                int finalCountOfDocsInAEListing = getDocumentCountInAEListing();
                csAssert.assertEquals(finalCountOfDocsInAEListing,docCountInAEListing+1,"Mismatch in doc count after uploading a Document in Contract Document Tab oF CDR"
                        + " Initial Count was : "+docCountInAEListing + " And Final Count is : " +finalCountOfDocsInAEListing);
                isExtractionCompletedForUploadedFile = AEPorting.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForUploadedFile, "Extraction not Completed");

            } else {
                csAssert.assertTrue(false, "File upload failed!");

            }

        }
        catch (Exception e) {
            csAssert.assertTrue(false, "Project Create API is not working because of :" + e.getStackTrace());

        }
        csAssert.assertAll();

    }

    @Test(dependsOnMethods = {"testCDRPorting"},priority = 2)
    public void checkMetadataPortingAfterCompletion()
    {
        CustomAssert csAssert = new CustomAssert();
            /* C153861: Verify that after porting metadata should get ported successfully in mapped dynamic fields
               C153862: Verify that after porting Clause should get ported successfully in mapped dynamic fields

             */
        if(isExtractionCompletedForUploadedFile)
        {
            try {
                logger.info("Waiting for porting to get completed");
                Thread.sleep(10000);
                logger.info("Checking the Ported Metadata on CDR show page");
                String showPageResponse = showHelper.getShowResponseVersion2(160, cdrId);
                JSONObject jsonData = new JSONObject(showPageResponse);
                allMappedDynamicFields = new HashMap<>();
                JSONArray fieldsJsonArray = jsonData.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields")
                        .getJSONObject(0).getJSONArray("fields").getJSONObject(1).getJSONArray("fields");
                int totalFields = fieldsJsonArray.length();
                for (int i = 0; i < totalFields; i++) {
                    fieldName = (String) fieldsJsonArray.getJSONObject(i).get("label");
                    if (fieldName.equalsIgnoreCase("Party")  || fieldName.equalsIgnoreCase("CDR Porting")
                            || fieldName.equalsIgnoreCase("Clause Text")) {
                        fieldId = (int) fieldsJsonArray.getJSONObject(i).get("id");
                        allMappedDynamicFields.put(fieldId, fieldName);
                    }

                }
                try {
                    List<String> metadataValue = new LinkedList<>();
                    mappedDynamicFields = new LinkedList<>();
                    logger.info("Checking the value against the mapped dynamic fields");
                    JSONObject dynamicMetadataJson = jsonData.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                    for (Map.Entry<Integer, String> entry : allMappedDynamicFields.entrySet()) {
                        mappedDynamicFields.add(entry.getValue());
                        fieldName = entry.getValue();
                        fieldId = entry.getKey();
                        String dynamicFieldSystemName = "dyn" + fieldId;
                        metadataValue.add((String) dynamicMetadataJson.getJSONObject(dynamicFieldSystemName).get("values"));
                        logger.info("Metadata Value for fieldName " + fieldName + " and fieldId: " + fieldId + "is: " + metadataValue);
                    }
                    csAssert.assertEquals(metadataValue.size(), 3, "Metadata has not been ported in all three mapped fields");
                } catch (Exception e) {
                    logger.info("Exception occured while hitting CDR show API");
                    csAssert.assertTrue(false, e.getMessage());
                }


            }
            catch (Exception e)
            {
                logger.info("Exception occured while hitting CDR show API");
                csAssert.assertTrue(false, e.getMessage());
            }
        }
        csAssert.assertAll();

    }

    @Test(dependsOnMethods = {"testCDRPorting"},priority = 3)
    public void testAuditLogAfterPorting()
    {
        CustomAssert csAssert = new CustomAssert();
            /* C153863: Verify that after successful porting, the Porting flag value should get updated
            C153885: Audit Log : Verify that after successful porting it should show the details in audit log tab */
        try{
            logger.info("Checking Audit Log of CDR After successful porting of metadata for CDR Id: " + cdrId);
            String filterMap = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String auditLog_response = auditlog.hitAuditLogDataApi(String.valueOf(160), String.valueOf(cdrId), filterMap);
            JSONObject auditLogJson = new JSONObject(auditLog_response);
            int actionColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "action_name");
            String actionValue = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(actionColumnId)).getString("value");
            logger.info("Action name for cdrId " + cdrId + "is: " + actionValue);
            csAssert.assertEquals(actionValue, "Auto Update", "Showing an incorrect Action name after CDR Porting: " + actionValue);
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
                    if (jsonObj.get("property").equals("CDR Porting")) {
                        portingSuccessFlag = (String) jsonObj.get("newValue");
                    }

                }
                csAssert.assertTrue(modifiedFields.containsAll(mappedDynamicFields), "All the mapped dynamic fields are not getting updated after porting");
                logger.info("Check CDR Porting Flag value is True after successful Porting");
                csAssert.assertTrue(portingSuccessFlag.equalsIgnoreCase("Yes")||portingSuccessFlag.equals("true"),"CDR Porting flag value is"  +portingSuccessFlag);

            } catch (Exception e) {
                logger.info("Exception occured while hitting CDR View Field History API");
                csAssert.assertTrue(false, e.getMessage());
            }

        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting CDR Audit log API");
            csAssert.assertTrue(false, e.getMessage());

        }
        csAssert.assertAll();
    }

    @Test(dependsOnMethods = {"testCDRPorting"},priority = 4)
    public void checkCommunicationTabAfterPorting()
    {
        CustomAssert csAssert = new CustomAssert();
        /*C153886: Verify that after successful porting it should show a message of successful porting in comment section as well */
        try {
            logger.info("Test Case to validate after successful porting it should show the details in comment section of CDR");
            HttpResponse commentSectionResponse = AutoExtractionHelper.commentSectionCDR(cdrId);
            csAssert.assertTrue(commentSectionResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String commentSectionStr = EntityUtils.toString(commentSectionResponse.getEntity());
            JSONObject commentSectionObj = new JSONObject(commentSectionStr);
            int commentColumnID = ListDataHelper.getColumnIdFromColumnName(commentSectionStr,"comment");
            String commentValue;
            int totalDataInCommentSection = commentSectionObj.getJSONArray("data").length();
            for(int i=0; i<totalDataInCommentSection; i++)
            {
                commentValue = String.valueOf(commentSectionObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(commentColumnID)).get("value")).trim();
                if(!commentValue.equalsIgnoreCase("null"))
                {
                    csAssert.assertTrue(commentValue.contains("Porting success for fields"),"Not getting successful Porting Message, Message shown in comment section: " +commentValue);
                    break;
                }
            }
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting CDR Communication Tab API");
            csAssert.assertTrue(false, e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(dependsOnMethods = {"testCDRPorting"},priority = 5)
    public void editMetadataAndTriggerPorting()
    {
        logger.info("Test Case to validate when user edit/Insert a metadata then the updated value should be ported");
        CustomAssert csAssert = new CustomAssert();
        boolean isNewlyAddedMetadataPresent = false;
        try{
            docId = getDocumentId();
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
                            Thread.sleep(20000);
                            logger.info("Getting CDR Show page Data");
                            String showPageResponse = showHelper.getShowResponseVersion2(160, cdrId);
                            JSONObject jsonObject = new JSONObject(showPageResponse);
                            List<String> metadataValue = new LinkedList<>();
                            JSONObject dynamicMetadataJson = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                            for (Map.Entry<Integer, String> entry : allMappedDynamicFields.entrySet()) {
                                mappedDynamicFields.add(entry.getValue());
                                fieldName = entry.getValue();
                                int fieldIdForContracts = entry.getKey();
                                String dynamicFieldSystemName = "dyn" + fieldIdForContracts;
                                metadataValue.add((String) dynamicMetadataJson.getJSONObject(dynamicFieldSystemName).get("values"));
                                if(fieldName.equalsIgnoreCase("Party"))
                                {
                                    Party = (String) dynamicMetadataJson.getJSONObject(dynamicFieldSystemName).get("values");
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
                            csAssert.assertTrue(isNewlyAddedMetadataPresent,"Newly Added metadata is not updated in CDR after porting for document Id: "+docId );
                            /*Test Case Id: C153880: Verify that it shows Double pipe "||" as a separator for successfully ported metadata*/
                            logger.info("Test Case to verify it is showing a separator || for successfully ported metadata");
                            csAssert.assertTrue(Party.contains("||") ,"Separator || is not present in metadataValue");

                        }
                        catch (Exception e)
                        {
                            csAssert.assertTrue(false, "CDR Show API is not working" + e.getMessage());

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

    @Test(dependsOnMethods = {"testCDRPorting"},priority = 6)
    public void performPartialPortingOnCDR()
    {
        CustomAssert csAssert = new CustomAssert();
        /* Test Case Id: C154059: Verify that if a user has reset the metadata partially then it should port the selected metadata for CDR*/
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
                try {
                    logger.info("Now Reset the document Partially");
                    APIResponse partialResetResponse = PartialResetAPI.partialResetAPIResponse(PartialResetAPI.getAPIPath(), PartialResetAPI.getHeaders(), PartialResetAPI.getPayload(String.valueOf(docId), "12486"));
                    Integer partialResetResponseCode = partialResetResponse.getResponseCode();
                    csAssert.assertTrue(partialResetResponseCode == 200, "Response Code is Invalid for Partial Reset API");
                    String partialResetStr = partialResetResponse.getResponseBody();
                    JSONObject partialResetJson = new JSONObject(partialResetStr);
                    csAssert.assertTrue(partialResetJson.get("success").toString().equals("true"), "Partial Reset API is not working");
                    if (ParseJsonResponse.validJsonResponse(partialResetStr)) {
                        logger.info("Now waiting for document completion for Document Id: " + docId);
                        boolean isExtractionCompletedForUploadedFile = AEPorting.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                        csAssert.assertTrue(isExtractionCompletedForUploadedFile, "Extraction not Completed");

                        if (isExtractionCompletedForUploadedFile) {
                            try {
                                logger.info("Checking the Audit Log after porting");
                                String filterMap = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                                String auditLog_response = auditlog.hitAuditLogDataApi(String.valueOf(160), String.valueOf(cdrId), filterMap);
                                JSONObject auditLogJson = new JSONObject(auditLog_response);
                                int actionColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "action_name");
                                String actionValue = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(actionColumnId)).getString("value");
                                logger.info("Action name for cdrId " + cdrId + "is: " + actionValue);
                                csAssert.assertEquals(actionValue, "Auto Update", "Showing an incorrect Action name after CDR Porting: " + actionValue);
                                try {
                                    logger.info("Verify that View History section shows the ported metadata in mapped dynamic field");
                                    int historyColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "history");
                                    String historyURL = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(historyColumnId)).getString("value");
                                    HttpResponse viewHistoryAPIResponse = AutoExtractionHelper.viewHistoryAPI(historyURL);
                                    csAssert.assertTrue(viewHistoryAPIResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid for view History API");
                                    String viewHistoryStr = EntityUtils.toString(viewHistoryAPIResponse.getEntity());
                                    JSONObject viewHistoryJson = new JSONObject(viewHistoryStr);
                                    int totalModifiedData = viewHistoryJson.getJSONArray("value").length();
                                    csAssert.assertEquals(totalModifiedData, 2, "Partial Porting has been done for one field, but showing audit Log for : " + totalModifiedData + " no. of fields");
                                    logger.info("Validate the newly added metadata should be present in respective field after Partial Porting");
                                    String metadataFieldValueOfCDR = null;
                                    boolean partialPortingStatus = false;
                                    for(int i=0;i<totalModifiedData;i++)
                                    {
                                        String newValue = viewHistoryJson.getJSONArray("value").getJSONObject(i).get("newValue").toString();
                                        if(newValue.contains("||"))
                                        {
                                            String[] individualValues = newValue.split("\\|\\|");
                                            for(int j=0;j<individualValues.length;j++)
                                            {
                                                metadataFieldValueOfCDR = individualValues[j].trim();
                                                if(metadataFieldValueOfCDR.equalsIgnoreCase("API Automation Metadata Create Operation"))
                                                {
                                                    partialPortingStatus=true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    csAssert.assertTrue(partialPortingStatus,"Metadata has not been ported in mapped Dynamic Field after initiating Partial Porting");
                                } catch (Exception e) {
                                    csAssert.assertTrue(false, "View History API for CDR is not working because of : " + e.getMessage());
                                }
                            } catch (Exception e) {
                                csAssert.assertTrue(false, "Audit log API for CDR is not working" + e.getMessage());
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    csAssert.assertTrue(false, "Partial Reset API is not working because of:" + e.getMessage());

                }
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false, "Metadata Update API is not working because of:" + e.getMessage());

            }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "List Data API is not working for Metadata Tab" + e.getMessage());

        }
        finally {
            //Delete CDR
            logger.info("Deleting Newly Created Contract Entity");
            EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
        }
        csAssert.assertAll();
    }
}