package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AEUXAdminHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.DocumentShowPageHelper;
import com.sirionlabs.helper.autoextraction.GetDocumentIdHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

public class MetadataCrudOperations {
    private final static Logger logger = LoggerFactory.getLogger(MetadataCrudOperations.class);
    static Integer documentId;
    int newlyCreatedProjectId;
    String projectName;
    static String fieldId;
    GetDocumentIdHelper listDataHelper=new GetDocumentIdHelper();

    @BeforeClass
    public void projectCreate() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Creating a new Project");
            APIResponse projectCreateResponse = ProjectCreationAPI.projectCreateAPIResponse(ProjectCreationAPI.getAPIPath(), ProjectCreationAPI.getHeaders(), ProjectCreationAPI.getPayload());
            Integer projectResponseCode = projectCreateResponse.getResponseCode();
            String projectCreateStr = projectCreateResponse.getResponseBody();
            csAssert.assertTrue(projectResponseCode == 200, "Response Code is Invalid");
            JSONObject projectCreateJson = new JSONObject(projectCreateStr);
            csAssert.assertTrue(projectCreateJson.get("success").toString().equals("true"), "Project is not created successfully");
            newlyCreatedProjectId = Integer.valueOf(projectCreateJson.getJSONObject("response").get("id").toString());
            projectName = projectCreateJson.getJSONObject("response").get("name").toString();

        }
        catch (Exception e) {
            csAssert.assertTrue(false, "Project Create API is not working because of :" + e.getStackTrace());
        }
        try
        {
            logger.info("Uploading document in project name "+projectName);
            testUploadDocuments();
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Document is not uploaded successfully");
        }
        csAssert.assertAll();
    }

    public void testUploadDocuments()
    {
        CustomAssert csAssert = new CustomAssert();

        // File Upload API to get the key from the uploaded Files
        logger.info("File Upload API to get the key of file that has been uploaded");
        try {
            String templateFilePath = "src/test/resources/TestConfig/AutoExtraction/UploadFiles";
            String templateFileName = "Doc File API Automation.doc";
            Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);

            // Hit Global Upload API
            try {
                logger.info("Hit Global Upload API");
                String payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                String fileUploadResponse=EntityUtils.toString(httpResponse.getEntity());
                logger.info("string response "+fileUploadResponse);
                logger.info("Checking whether Extraction Status is complete or not");
                boolean isExtractionCompletedForUploadedFile = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForUploadedFile, " " + templateFileName + " is not getting completed ");
            } catch (Exception e) {
                csAssert.assertTrue(false, "Global Upload API is not working because of :  :" + e.getMessage());

            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "File Upload API is not working because of :  :" + e.getMessage());

        }
        csAssert.assertAll();
    }

    public static String metadataTabResponse() throws IOException {
        DocumentShowPageHelper resetObj = new DocumentShowPageHelper(String.valueOf(documentId));
        String metadataTabResponseStr = resetObj.getMetadataTabResponse(String.valueOf(documentId));
        return metadataTabResponseStr;
    }

    public List<String> fieldMappedToCategory(String categoryId) throws IOException {
        List<String> allMetadataLinkedToCategory=new ArrayList<>();
        HttpResponse fetchFieldCategoryResponse=AutoExtractionHelper.fetchFieldCategoryMappingsAPI(categoryId);
        String fetchFieldCategoryResponseStr=EntityUtils.toString(fetchFieldCategoryResponse.getEntity());
        JSONObject fetchFieldCategoryResponseJson=new JSONObject(fetchFieldCategoryResponseStr);
        int count=fetchFieldCategoryResponseJson.getJSONArray("response").length();
        for(int i=0;i<count;i++)
        {
            allMetadataLinkedToCategory.add(fetchFieldCategoryResponseJson.getJSONArray("response").getJSONObject(i).get("id").toString());
        }
        return allMetadataLinkedToCategory;
    }

    /* C153469: Insert the Extracted metadata from Metadata Tab */
    @Test(priority = 1,enabled = true)
    public void metadataCrudOperation() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();

        try{
            logger.info("Fetching Document Id from AE Listing");
            documentId=listDataHelper.getDocIdOfLatestDocument();
            logger.info("Checking the Data in Metadata Tab of " +documentId);
            DocumentShowPageHelper crudHelper = new DocumentShowPageHelper(String.valueOf(documentId));
            logger.info("Getting Category Id to edit from metadata tab");
            String categoryId = crudHelper.getClauseIdLinkedToMetadata();
            logger.info("Getting Text Id from metadata tab");
            String textId = crudHelper.getMetadataTextId();
            logger.info("Getting field Id for Performing create operation");
            fieldId = crudHelper.getMetadataId();

            try {
                String fieldIdToPerformAction= fieldMappedToCategory(categoryId).get(2);
                int metadataCountOld =crudHelper.metadataCountOnShowPage(String.valueOf(documentId));
                int CountListPageOld=listDataHelper.metadataCountOnListPage(csAssert);
                logger.info("Now Performing Create Operation for Metadata");
                HttpResponse metadataCreateResponse = AutoExtractionHelper.metadataCreateOperation(textId, categoryId, documentId, fieldIdToPerformAction);
                csAssert.assertTrue(metadataCreateResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String metadataResponseStr = EntityUtils.toString(metadataCreateResponse.getEntity());
                JSONObject metadataJson = new JSONObject(metadataResponseStr);
                String createResponseMessage = (String) metadataJson.getJSONObject("response").get("message");
                csAssert.assertEquals(createResponseMessage, "Action completed successfully");
                int metadataCountNew=crudHelper.metadataCountOnShowPage(String.valueOf(documentId));
                csAssert.assertTrue(metadataCountNew==metadataCountOld+1,"metadata count is not updated on show page after performing create operation for document id "+documentId);
                int CountListPageNew=listDataHelper.metadataCountOnListPage(csAssert);
                csAssert.assertTrue(CountListPageNew==CountListPageOld+1,"metadata count is not updated on list page  after performing create operation for document id "+documentId);
                /*C153470 : Test Case to Perform Update Operation on Metadata*/
                try{
                        logger.info("Performing Metadata Update Operation from Metadata Tab present on Document show page");
                        HttpResponse metadataUpdateResponse = AutoExtractionHelper.metadataUpdateOperation(textId, fieldIdToPerformAction);
                        csAssert.assertTrue(metadataUpdateResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                        String metadataUpdateStr = EntityUtils.toString(metadataUpdateResponse.getEntity());
                        JSONObject metadataUpdateJson = new JSONObject(metadataUpdateStr);
                        String updateResponseMessage = (String) metadataUpdateJson.get("response");
                        csAssert.assertEquals(updateResponseMessage, "Action completed successfully", "Update Operation is not working on Metadata from Metadata Tab");

                    /*C153471 : Test Case to perform Delete Operation on Metadata */

                    try {
                        logger.info("Performing Delete Operation on Metadata");
                        int metadataCountBeforeDeletion =crudHelper.metadataCountOnShowPage(String.valueOf(documentId));
                        int CountListPageOldBeforeDeletion=listDataHelper.metadataCountOnListPage(csAssert);
                        HttpResponse metadataDeleteResponse = AutoExtractionHelper.metadataDeleteOperation(textId, fieldIdToPerformAction);
                        csAssert.assertTrue(metadataDeleteResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                        String metadataDeleteStr = EntityUtils.toString(metadataDeleteResponse.getEntity());
                        JSONObject metadataDeleteJson = new JSONObject(metadataDeleteStr);
                        String deleteResponseMessage = (String) metadataDeleteJson.get("response");
                        csAssert.assertEquals(deleteResponseMessage, "Action completed successfully", "Delete Operation is not working on Metadata from Metadata Tab");
                        int metadataCountAfterDeletion=crudHelper.metadataCountOnShowPage(String.valueOf(documentId));
                        csAssert.assertTrue(metadataCountBeforeDeletion==metadataCountAfterDeletion+1,"metadata count is not updated on show page after performing create operation for document id "+documentId);
                        int CountListPageAfterDeletion=listDataHelper.metadataCountOnListPage(csAssert);
                        csAssert.assertTrue(CountListPageOldBeforeDeletion==CountListPageAfterDeletion+1,"metadata count is not updated on list page  after performing create operation for document id "+documentId);

                        /* C153563: Test Case to check after Reset the Newly Inserted + Updated and Deleted metadata should not appear in Metadata Tab*/
                        try {
                            logger.info("Now Reset the document and check that changes should not get removed");
                            HttpResponse documentResetResponse = AutoExtractionHelper.documentResetAPI(documentId);
                            csAssert.assertTrue(documentResetResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                            logger.info("Waiting for document Completion");
                            Thread.sleep(35000);
                            logger.info("Checking if updated metadata should exist");
                            Set<String> extractedText = new HashSet<>();
                            String metadataTabResponseStr = metadataTabResponse();
                            JSONObject metadataTabJSon = new JSONObject(metadataTabResponseStr);
                            int data = metadataTabJSon.getJSONArray("data").length();
                            if (data > 0) {
                                for (int i = 0; i < data; i++) {
                                    int metadataFieldColumnId = ListDataHelper.getColumnIdFromColumnName(metadataTabResponseStr, "extractedtext");
                                    JSONObject docJsonObj = metadataTabJSon.getJSONArray("data").getJSONObject(i);
                                    extractedText.add(docJsonObj.getJSONObject(Integer.toString(metadataFieldColumnId)).getString("value"));
                                }
                            }
                            csAssert.assertTrue(!extractedText.contains("API Automation metadata Update Operation") || !extractedText.contains("API Automation Metadata Create Operation"), "Updated Metadata is not present in metadata Tab Listing" + extractedText);
                        }
                        catch (Exception e)
                        {
                            csAssert.assertTrue(false, "Reset Operation is not working" + e.getMessage());

                        }
                    }
                    catch (Exception e)
                    {
                        csAssert.assertTrue(false, "Metadata Delete API is not working" + e.getMessage());
                    }

                }
                catch (Exception e)
                {
                    csAssert.assertTrue(false, "Metadata Update API is not working" + e.getMessage());

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
    @Test(priority = 3,enabled = true)
    public void clauseUpdateOperation()throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        try{
            logger.info("Fetching Document Id from AE Listing");
            ///documentId = listDataHelper.getDocIdOfLatestDocument();
            logger.info("Checking the Data in Clause Tab of " +documentId);
            DocumentShowPageHelper clauseHelper = new DocumentShowPageHelper(String.valueOf(documentId));
            logger.info("Getting Clause Text to update a clause");
            String clauseText = clauseHelper.getCategoryText();
            logger.info("Getting text Id to update a clause");
            String textId = clauseHelper.getCategoryTextId();
            logger.info("Fetching Page Number to perform update operation on clause");
            String clausePageNo = clauseHelper.getCategoryPageNo();

            try {
                logger.info("Performing Update Operation on a Clause");
                logger.info("Fetching the Initial Count of Clauses in General Category");
                int initialClauseCount = countOfGeneralClause();
                int finalClauseCount = 0;
                HttpResponse clauseUpdateOperationResponse = AutoExtractionHelper.clauseUpdateOperation(clauseText,textId,clausePageNo,documentId);
                csAssert.assertTrue(clauseUpdateOperationResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String clauseUpdateStr = EntityUtils.toString(clauseUpdateOperationResponse.getEntity());
                JSONObject clauseUpdateJson = new JSONObject(clauseUpdateStr);
                String responseOfClauseUpdateOperation = (String) clauseUpdateJson.get("response");
                csAssert.assertEquals(responseOfClauseUpdateOperation,"Success","Clause Update Operation Response is:");
                logger.info("Checking after successful update operation on clauses a new clause should get added in General Category");
                if(responseOfClauseUpdateOperation.equalsIgnoreCase("success"))
                {
                    finalClauseCount=countOfGeneralClause();
                }
                csAssert.assertEquals(finalClauseCount,initialClauseCount+1,"New Value of clause has not been successfully updated in General Category " +"Initial Count of Clauses in General Category: "
                        +initialClauseCount +" Final Clause COunt in General Category: " +finalClauseCount);
                try {
                    logger.info("Reset the Document and check the newly added category should be present");
                    HttpResponse resetOperationResponse = AutoExtractionHelper.documentResetAPI(documentId);
                    csAssert.assertTrue(resetOperationResponse.getStatusLine().getStatusCode() == 200, "Reset Operation is not working");
                    logger.info("Waiting for document Completion");
                    Thread.sleep(35000);
                    try {
                        int finalClauseCountAfterReset = countOfGeneralClause();
                        csAssert.assertEquals(finalClauseCountAfterReset, finalClauseCount, "After Reset Newly Added Clause is getting removed");
                    }
                    catch (Exception e)
                    {
                        csAssert.assertTrue(false, "Exception occured while hitting AE - Clause Tab Data API: " + e.getMessage());

                    }
                }
                catch (Exception e)
                {
                    csAssert.assertTrue(false, "Exception occured while Performing Document Reset Operation: " + e.getMessage());

                }

            }
            catch (Exception e)
            {
                csAssert.assertTrue(false, "List Data API is not working for Clause Tab: " + e.getMessage());

            }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "List Data API is not working for Clause Tab: " + e.getMessage());

        }
        csAssert.assertAll();
    }

    //TC:C153556 Verify to Create a New Clause
    @Test(priority = 2,enabled = true)
    public void testCreateNewClause()
    {
        CustomAssert csAssert=new CustomAssert();
        try{

            String categoryId=" ";
            documentId=listDataHelper.getDocIdOfLatestDocument();
            DocumentShowPageHelper clauseCreateHelper=new DocumentShowPageHelper(String.valueOf(documentId));
            int oldCountListingPage=listDataHelper.CountOnListPage(csAssert);
            int oldCount=clauseCreateHelper.clauseCountOnShowPage(documentId);
            logger.info("Creating new clause for documentId "+documentId);

            String text="References in this Agreement to schedules shall be deemed to be references to schedules, the terms of which shall be incorporated into and form part of this Agreement";
            List<String> allCategories= AEUXAdminHelper.getAllCategoryList();
            List<String> allLinkedCategories=clauseCreateHelper.getCategoryAllValue();
            for(String category: allCategories)
            {
                if(!(allLinkedCategories.contains(category)))
                {
                    categoryId=category;
                    break;
                }
            }

            String payload="{\"text\":\""+text+"\",\"pageNumber\":1,\"textId\":null,\"documentId\":"+documentId+",\"categoryId\":"+categoryId+",\"operation\":1,\"aeCoordinate\":null}";
            logger.info("Hitting create New Clause API for document ID "+documentId);
            HttpResponse createNewClauseResponse=AutoExtractionHelper.updateClause(payload);
            csAssert.assertTrue(createNewClauseResponse.getStatusLine().getStatusCode()==200,"Create Clause response code is not valid");
            String createClauseStr=EntityUtils.toString(createNewClauseResponse.getEntity());
            JSONObject createClauseJson=new JSONObject(createClauseStr);
            String success=createClauseJson.get("success").toString();
            logger.info("Validating create new Clause response after creating clause for category id "+categoryId);
            csAssert.assertTrue(success.equalsIgnoreCase("true"),"Insert new clause operation is not successful on document id "+documentId+" for category Id "+categoryId);
            logger.info("Validating if created category id is present extracted clause list ");
            DocumentShowPageHelper clauseCreateHelper1=new DocumentShowPageHelper(String.valueOf(documentId));
            csAssert.assertTrue(clauseCreateHelper1.getCategoryAllValue().contains(categoryId),"newly created category id "+categoryId+" is not present in the clause list");
            int newCount=clauseCreateHelper1.clauseCountOnShowPage(documentId);
            csAssert.assertTrue(newCount==oldCount+1,"Clause Count is not updated on show page for document id "+documentId);
            int newCountListingPage=listDataHelper.CountOnListPage(csAssert);
            csAssert.assertTrue(oldCountListingPage+1==newCountListingPage,"Clause count is not updated on Listing page after new clause insertion");
        }
        catch (Exception e)
        {
            logger.info("Create New clause operation is not successful due to "+e.getMessage());
            csAssert.assertTrue(false,"Create New clause operation is not successful due to "+e.getMessage());
        }
        csAssert.assertAll();
    }

    public static int countOfGeneralClause() {
        CustomAssert csAssert = new CustomAssert();
        int clauseTextCounter = 0;
        try {
            logger.info("Getting the Count of clauses in general tab");
            DocumentShowPageHelper clauseUpdate = new DocumentShowPageHelper(String.valueOf(documentId));
            String clauseTabResponseStr = clauseUpdate.getClauseTabResponse(String.valueOf(documentId));
            JSONObject clauseTabResponseJson = new JSONObject(clauseTabResponseStr);
            int totalClausesInTab = clauseTabResponseJson.getJSONArray("data").length();
            int clauseNameColumnID = ListDataHelper.getColumnIdFromColumnName(clauseTabResponseStr, "name");
            clauseTextCounter = 0;
            for (int i = 0; i < totalClausesInTab; i++) {
                JSONObject jsonObj = clauseTabResponseJson.getJSONArray("data").getJSONObject(i);
                String clauseNameAndID = (String) jsonObj.getJSONObject(Integer.toString(clauseNameColumnID)).get("value");
                String clauseName = clauseNameAndID.split(":;")[0];
                if (clauseName.equalsIgnoreCase("Genaral")||clauseName.equalsIgnoreCase("General")) {
                    clauseTextCounter++;
                }

            }

        } catch (Exception e) {
            csAssert.assertTrue(false, "List Data API is not working for Clause Tab Listing: " + e.getMessage());

        }
        return clauseTextCounter;
    }

}
