package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.api.autoExtraction.PartialResetAPI;
import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.DocumentShowPageHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestPartialReset {
    private final static Logger logger = LoggerFactory.getLogger(TestDuplicateFeature.class);
    int newlyCreatedProjectId;
    String projectName;
    String docId;


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
    }

    @DataProvider
    public Object[][] dataProviderForDocumentUpload() {
        List<Object[]> referencesData = new ArrayList<>();

        String fileUploadName="Doc File API Automation.doc";
        referencesData.add(new Object[]{fileUploadName});

        return referencesData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForDocumentUpload", priority = 0)
    public void testDuplicateFileUpload(String fileUploadName) throws IOException {
        CustomAssert csAssert = new CustomAssert();

        // File Upload API to get the key from the uploaded Files
        logger.info("File Upload API to get the key of file that has been uploaded");
        try {
            String templateFilePath = "src/test/resources/TestConfig/AutoExtraction/UploadFiles";
            String templateFileName = fileUploadName;
            Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);

            // Hit Global Upload API
            try {
                logger.info("Hit Global Upload API");
                String payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                logger.info("Checking whether Extraction Status is complete or not");
                boolean isExtractionCompletedForUploadedFile = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForUploadedFile, " " + templateFileName + " is not getting completed ");
            } catch (Exception e) {
                csAssert.assertTrue(false, "Global Upload API is not working because of :  :" + e.getStackTrace());

            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "File Upload API is not working because of :  :" + e.getStackTrace());

        }
        csAssert.assertAll();
    }

    @Test(priority = 1,enabled = true)
    public void testPartialReset() throws IOException {
            CustomAssert csAssert=new CustomAssert();
            try {
                HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
                String listDataResponseStr = EntityUtils.toString(listDataResponse.getEntity());
                csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "List Data response code is invalid");
                int recordColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
                JSONObject jsonObj=new JSONObject(listDataResponseStr);
                String[] docIdValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(recordColumn)).get("value").toString().split(":;");
                docId = docIdValue[1];
                DocumentShowPageHelper documentShowPageHelper=new DocumentShowPageHelper(docId);
                String fieldId= documentShowPageHelper.getMetadataId();
                logger.info("Validating Partial Reset on document Id "+docId);
                verifyPartialReset(docId,csAssert,fieldId);
            }
            catch (Exception e)
            {
                logger.info("Exception while validating Partial Reset Operation "+e.getMessage());
                csAssert.assertTrue(false,"Exception while validating Partial Reset Operation due to "+e.getMessage());
            }
            csAssert.assertAll();
    }

    @Test(priority = 2,enabled = true)
    public void testPartialResetAfterCreate() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        try {
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            String listDataResponseStr = EntityUtils.toString(listDataResponse.getEntity());
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "List Data response code is invalid");
            int recordColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
            JSONObject jsonObj=new JSONObject(listDataResponseStr);
            String[] docIdValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(recordColumn)).get("value").toString().split(":;");
            String docId = docIdValue[1];
            DocumentShowPageHelper documentShowPageHelper=new DocumentShowPageHelper(docId);
            String textId=documentShowPageHelper.getMetadataTextId();
            String text="API Automation Metadata Create Operation";
            String categoryId=documentShowPageHelper.getClauseIdLinkedToMetadata();
            String fieldId=documentShowPageHelper.getMetadataId();
            HttpResponse createFieldResponse=AutoExtractionHelper.metadataCreateOperation(textId,categoryId,Integer.parseInt(docId),fieldId);
            String createFieldResponseStr=EntityUtils.toString(createFieldResponse.getEntity());
            JSONObject metadataJson = new JSONObject(createFieldResponseStr);
            String createResponseMessage = (String) metadataJson.getJSONObject("response").get("message");
            csAssert.assertEquals(createResponseMessage, "Action completed successfully","Metadata create operation not completed successfully");
            logger.info("Validating Partial Reset on document Id "+docId);
            verifyPartialReset(docId,csAssert,fieldId);
            DocumentShowPageHelper documentShowPageHelper1=new DocumentShowPageHelper(docId);
            csAssert.assertTrue(documentShowPageHelper1.getMetadataAllValue().contains(text),"Created Text is not present after reset");

        }
        catch (Exception e)
        {
            logger.info("Exception while validating Partial Reset Operation "+e.getMessage());
            csAssert.assertTrue(false,"Exception while validating Partial Reset Operation due to "+e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(priority = 3,enabled = true)
    public void testPartialResetAfterUpdate() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        try {
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            String listDataResponseStr = EntityUtils.toString(listDataResponse.getEntity());
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "List Data response code is invalid");
            int recordColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
            JSONObject jsonObj=new JSONObject(listDataResponseStr);
            String[] docIdValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(recordColumn)).get("value").toString().split(":;");
            String docId = docIdValue[1];
            DocumentShowPageHelper documentShowPageHelper=new DocumentShowPageHelper(docId);
            String textId=documentShowPageHelper.getMetadataTextId();
            String text="API Automation metadata Update Operation";
            String fieldId=documentShowPageHelper.getMetadataId();
            logger.info("Performing Metadata Update Operation from Metadata Tab present on Document show page");
            HttpResponse metadataUpdateResponse = AutoExtractionHelper.metadataUpdateOperation(textId,fieldId);
            csAssert.assertTrue(metadataUpdateResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String metadataUpdateStr = EntityUtils.toString(metadataUpdateResponse.getEntity());
            JSONObject metadataUpdateJson = new JSONObject(metadataUpdateStr);
            String updateResponseMessage = (String) metadataUpdateJson.get("response");
            csAssert.assertEquals(updateResponseMessage,"Action completed successfully","Update Operation is not working on Metadata from Metadata Tab");
            logger.info("Validating Partial Reset on document Id "+docId);
            verifyPartialReset(docId,csAssert,fieldId);
            DocumentShowPageHelper documentShowPageHelper1=new DocumentShowPageHelper(docId);
            csAssert.assertTrue(documentShowPageHelper1.getMetadataAllValue().contains(text),"Updated Text  is not present after reset");

        }
        catch (Exception e)
        {
            logger.info("Exception while validating Partial Reset Operation "+e.getMessage());
            csAssert.assertTrue(false,"Exception while validating Partial Reset Operation due to "+e.getMessage());
        }
        csAssert.assertAll();
    }

    public void verifyPartialReset(String docId, CustomAssert csAssert,String fieldId) throws IOException, InterruptedException {
        try {
            logger.info("Validating Partial Reset on document Id " + docId);
            APIResponse partialReset = PartialResetAPI.partialResetAPIResponse(PartialResetAPI.getAPIPath(), PartialResetAPI.getHeaders(), PartialResetAPI.getPayload(docId, fieldId));
            csAssert.assertTrue(partialReset.getResponseCode() == 200, "Partial Reset API Response code is invalid");
            String partialResetStr = partialReset.getResponseBody();
            JSONObject partialResetJson = new JSONObject(partialResetStr);
            String success = partialResetJson.get("success").toString();
            csAssert.assertTrue(success.equalsIgnoreCase("true"), "Partial reset API is not working");
            boolean isExtractionCompletedForUploadedFile = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
            csAssert.assertTrue(isExtractionCompletedForUploadedFile, " " + docId + " is not getting completed ");
        }
        catch (Exception e)
        {
            logger.info("Exception while validating partial Reset for document id "+docId);
            csAssert.assertTrue(false,"Exception while validating partial Reset due to "+e.getMessage());
        }
        csAssert.assertAll();
    }
}