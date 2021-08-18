package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
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
import java.util.*;
import java.util.stream.Collectors;

public class ProjectLevelTrigger extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(ProjectLevelTrigger.class);
    CustomAssert csAssert = new CustomAssert();
    int newlyCreatedProjectId;
    String projectName;

    @BeforeClass
    public void projectCreate() throws IOException {

        try {
            CustomAssert csAssert = new CustomAssert();
            logger.info("Creating a new Project");
            APIResponse projectCreateResponse = ProjectCreationAPI.projectCreateAPIResponse(ProjectCreationAPI.getAPIPath(), ProjectCreationAPI.getHeaders(), ProjectCreationAPI.getPayload());
            Integer projectResponseCode = projectCreateResponse.getResponseCode();
            String projectCreateStr = projectCreateResponse.getResponseBody();
            csAssert.assertTrue(projectResponseCode == 200, "Response Code is Invalid");
            JSONObject projectCreateJson = new JSONObject(projectCreateStr);
            csAssert.assertTrue(projectCreateJson.get("success").toString().equals("true"), "Project is not created successfully");
             newlyCreatedProjectId = Integer.valueOf(projectCreateJson.getJSONObject("response").get("id").toString());
             projectName = projectCreateJson.getJSONObject("response").get("name").toString();
        } catch (Exception e) {
            csAssert.assertTrue(false, "Project Create API is not working because of :" + e.getStackTrace());
        }
    }

    @DataProvider
    public Object[][] dataProviderForDocumentUpload() {
        List<Object[]> referencesData = new ArrayList<>();

        String DocumentFormat = "ReferencesDocumentUploadSchedule2";
        String fileUploadName = "Schedule 2.docx";
        referencesData.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat = "ReferencesDocumentUploadSchedule3";
        fileUploadName = "Schedule 3.docx";
        referencesData.add(new Object[]{DocumentFormat, fileUploadName});

        return referencesData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForDocumentUpload", priority = 1)
    public void testReferenceFileUpload(String DocumentFormat, String fileUploadName) throws IOException {
        logger.info("Testing Global Upload API for " + DocumentFormat);
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
                csAssert.assertTrue(isExtractionCompletedForUploadedFile, " " + DocumentFormat + " is not getting completed ");
            } catch (Exception e) {
                csAssert.assertTrue(false, "Global Upload API is not working because of :  :" + e.getStackTrace());

            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "File Upload API is not working because of :  :" + e.getStackTrace());

        }
        csAssert.assertAll();
    }

    @Test(priority = 2)
    public void projectLevelTrigger () throws IOException {
        //Project Level Trigger
        try {
            logger.info("Triggering the documents at project Level");
            HttpResponse projectLevelTriggerResponse = AutoExtractionHelper.projectLevelTrigger(newlyCreatedProjectId);
            csAssert.assertTrue(projectLevelTriggerResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String projectTriggerStr = EntityUtils.toString(projectLevelTriggerResponse.getEntity());
            JSONObject projectTriggerJson = new JSONObject(projectTriggerStr);
            boolean success = (boolean) projectTriggerJson.get("success");
            csAssert.assertTrue(success==true, "Project Trigger is not working");

            //Now checking the whether reference has been triggered for all the linked documents to a Project
            try {
                logger.info("Applying Filter for the project under which documents were uploaded");
                HttpResponse projectFilterResponse = AutoExtractionHelper.projectFilter(newlyCreatedProjectId, projectName);
                csAssert.assertTrue(projectFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String projectFilterStr = EntityUtils.toString(projectFilterResponse.getEntity());
                JSONObject projectFilterJson = new JSONObject(projectFilterStr);
                int documentCount = projectFilterJson.getJSONArray("data").length();
                int columnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "referencedata");
                int documentColumnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "documentname");
                try {
                    List<Integer> documentIds = new LinkedList<>();
                    for (int i = 0; i < documentCount; i++) {
                        JSONObject referenceObj = projectFilterJson.getJSONArray("data").getJSONObject(i);
                        String referenceValue = referenceObj.getJSONObject(Integer.toString(columnId)).getString("value");
                        logger.info("When reference is null");
                        if (referenceValue.equals(null)) {
                            referenceObj = projectFilterJson.getJSONArray("data").getJSONObject(i);
                            referenceValue = referenceObj.getJSONObject(Integer.toString(documentColumnId)).getString("value");
                            String[] references = referenceValue.split(",");
                            int documentId = Integer.valueOf(references[1]);
                            documentIds.add(documentId);

                        }
                    }
                    Collections.sort(documentIds);
                    String documentIdsWithComma = (String) documentIds.stream().map(Object::toString).collect(Collectors.joining(","));
                    csAssert.assertTrue(documentIds.isEmpty(), "Project Level Trigger has not triggered Reference for these documentIds" + documentIdsWithComma);
                }
                catch (Exception e)
                {
                    csAssert.assertTrue(false, "Checking data of References :" + e.getMessage());

                }

                    } catch (Exception e) {
                        csAssert.assertTrue(false, "Filter API is not working because of :" + e.getStackTrace());

                    }

                } catch (Exception e) {
                    csAssert.assertTrue(false, "Project Level Trigger API is not working because of :  :" + e.getStackTrace());

                }
        csAssert.assertAll();
            }
    }

