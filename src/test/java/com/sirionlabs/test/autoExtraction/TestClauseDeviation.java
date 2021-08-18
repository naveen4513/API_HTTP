package com.sirionlabs.test.autoExtraction;

import com.codepine.api.testrail.model.Priority;
import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
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
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

public class TestClauseDeviation {
    private final static Logger logger = LoggerFactory.getLogger(TestClauseDeviation.class);
    int newlyCreatedProjectId;
    String projectName;

    //TC: C154211:End User: Verify user is able to create with multiple/Single category
    @BeforeClass
    public void projectCreate() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Creating a new Project");
            APIResponse projectCreateResponse = ProjectCreationAPI.projectCreateAPIResponse(ProjectCreationAPI.getAPIPath(), ProjectCreationAPI.getHeaders(), ProjectCreationAPI.getPayloadWithCategories());
            Integer projectResponseCode = projectCreateResponse.getResponseCode();
            String projectCreateStr = projectCreateResponse.getResponseBody();
            csAssert.assertTrue(projectResponseCode == 200, "Response Code is Invalid");
            JSONObject projectCreateJson = new JSONObject(projectCreateStr);
            csAssert.assertTrue(projectCreateJson.get("success").toString().equals("true"), "Project is not created successfully");
            newlyCreatedProjectId = Integer.valueOf(projectCreateJson.getJSONObject("response").get("id").toString());
            projectName = projectCreateJson.getJSONObject("response").get("name").toString();
        }
        catch (Exception e) {
            csAssert.assertTrue(false, "Project Create API is not working because of :" + e.getMessage());
        }
        csAssert.assertAll();
    }
    @Test(priority = 0,enabled = true)
    public void testUploadDocuments()
    {
        CustomAssert csAssert = new CustomAssert();

        // File Upload API to get the key from the uploaded Files
        logger.info("File Upload API to get the key of file that has been uploaded");
        try {
            String templateFilePath = "src/test/resources/TestConfig/AutoExtraction/UploadFiles";
            String templateFileName = "KAMADALTD_DRSADraftR_3252013.docx";
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
                csAssert.assertTrue(false, "Global Upload API is not working because of :  :" + e.getMessage());

            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "File Upload API is not working because of :  :" + e.getMessage());

        }
        csAssert.assertAll();
    }
    //TC C154037: End User: Verify Deviation score value for document against clause text
    @Test(priority = 1)
    public void testClauseDeviationOnShowPage()
    {
        CustomAssert csAssert=new CustomAssert();
        try {
            logger.info("Hitting list data API ");
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            String listDataResponseStr = EntityUtils.toString(listDataResponse.getEntity());
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "List Data response code is invalid");
            int recordColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
            JSONObject jsonObj=new JSONObject(listDataResponseStr);
            String[] docIdValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(recordColumn)).get("value").toString().split(":;");
            String docId = docIdValue[1];
            logger.info("Hitting clause list data API for document id "+docId);
            String clauseStr=DocumentShowPageHelper.getClauseTabResponse(docId);
            int clauseDeviationCol=ListDataHelper.getColumnIdFromColumnName(clauseStr,"deviationscore");
            int categoryIdColumn=ListDataHelper.getColumnIdFromColumnName(clauseStr,"categoryId");
            JSONObject clauseListJson=new JSONObject(clauseStr);
            int count=clauseListJson.getJSONArray("data").length();
            double deviationScore=0;
            String deviationObjectId="";
            for(int i=0;i<count;i++)
            {
                String categoryId=clauseListJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(categoryIdColumn)).getString("value");
                logger.info("Checking if General category is present in clause list data response");
                if(categoryId.equalsIgnoreCase("1046"))
                {
                    String[] deviationScoreValue=clauseListJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(clauseDeviationCol)).getString("value").split(":;");
                    deviationScore=Double.parseDouble(deviationScoreValue[0]);
                    deviationObjectId =deviationScoreValue[1];
                    break;
                }

            }
            logger.info("Validating Deviation score value in clause list data for general category for document id "+docId);
            csAssert.assertTrue(deviationScore>=0,"Deviation score value is not null for document id "+docId);
            csAssert.assertTrue(!(deviationObjectId.equalsIgnoreCase("")),"Clause base text object id is null for general category for document id "+docId);

        }
        catch (Exception e)
        {
            logger.info("Exception while validating Deviation Score on Show Page due to "+e.getMessage());
            csAssert.assertTrue(false,"Getting Exception while validating Deviation Score on show page due to "+e.getMessage());
        }
        csAssert.assertAll();
    }

    //TC C154038: End User: Verify deviation score value against document
    @Test(priority=2)
    public void testClauseDeviationOnListPage() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        try {
            Thread.sleep(60000);
            logger.info("Hitting AE List data API");
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            String listDataResponseStr = EntityUtils.toString(listDataResponse.getEntity());
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "List Data response code is invalid");
            int recordColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
            int deviationScoreCol=ListDataHelper.getColumnIdFromColumnName(listDataResponseStr,"deviationscore");
            JSONObject jsonObj = new JSONObject(listDataResponseStr);
            String[] docIdValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(recordColumn)).get("value").toString().split(":;");
            String docId = docIdValue[1];
            logger.info("Checking deviation score column value on AE Listing page for document id "+docId);
            String deviationScore=jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(deviationScoreCol)).get("value").toString();
            csAssert.assertTrue(!(deviationScore.equalsIgnoreCase("null")),"Deviation score value is null on AE listing page for document id "+docId);
        }
        catch (Exception e)
        {
            logger.info("Exception while validating Deviation Score on list page due to "+e.getMessage());
            csAssert.assertTrue(false,"Getting Exception while validating Deviation Score on list page due to "+e.getMessage());
        }
        csAssert.assertAll();
    }

}
