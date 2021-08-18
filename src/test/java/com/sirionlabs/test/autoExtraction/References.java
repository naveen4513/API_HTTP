package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

public class References {
    private final static Logger logger = LoggerFactory.getLogger(References.class);
    CustomAssert csAssert = new CustomAssert();

    public static String listingResponse() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        HttpResponse duplicateDataResponse = AutoExtractionHelper.duplicateDataFilter();
        csAssert.assertTrue(duplicateDataResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
        String duplicateDataStr = EntityUtils.toString(duplicateDataResponse.getEntity());
        return duplicateDataStr;
    }

    @DataProvider
    public Object[][] dataProviderForDocumentUpload() {
        List<Object[]> referencesData = new ArrayList<>();

        String DocumentFormat = "ReferencesDocumentUploadSchedule2";
        String fileUploadName = "Schedule 2.docx";
        referencesData.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="ReferencesDocumentUploadSchedule3";
        fileUploadName = "Schedule 3.docx";
        referencesData.add(new Object[]{DocumentFormat, fileUploadName});

         DocumentFormat="ReferencesDocumentUploadSchedule4";
         fileUploadName = "Schedule 4.docx";
        referencesData.add(new Object[]{DocumentFormat, fileUploadName});
        return referencesData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForDocumentUpload",enabled = true)
    public void testReferenceFileUpload (String DocumentFormat, String fileUploadName)throws IOException
    {
        logger.info("Testing Global Upload API for " + DocumentFormat);
        CustomAssert csAssert = new CustomAssert();

            // File Upload API to get the key from the uploaded Files
            logger.info("File Upload API to get the key of file that has been uploaded");
            try {
                String templateFilePath = "src/test/resources/TestConfig/AutoExtraction/UploadFiles";
                String templateFileName = fileUploadName;
                Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);

                try {
                    // Hit Global Upload API
                    logger.info("Hit Global Upload API");
                    String payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\"}]";
                    HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                    csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                    logger.info("Checking whether Extraction Status is complete or not");
                    boolean isExtractionCompletedForUploadedFile = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                    csAssert.assertTrue(isExtractionCompletedForUploadedFile, " " + DocumentFormat + " is not getting completed ");

                } catch (Exception e) {
                    csAssert.assertTrue(false, "Global Upload API is not working because of :" + e.getStackTrace());
                }
            } catch (Exception e) {
                csAssert.assertTrue(false, "File Upload API is not working because of :" + e.getStackTrace());
            }
            csAssert.assertAll();
        }

        @Test
        public void triggerReferencePerDocument()
        {
            try{

                //Getting the top three document Ids that are being uploaded for Reference
                logger.info("Applying Duplicate Data Filter on AE listing");
                String duplicateDataStr = References.listingResponse();
                JSONObject duplicateDataJson = new JSONObject(duplicateDataStr);
                int docCount = duplicateDataJson.getJSONArray("data").length();
                int columnId = ListDataHelper.getColumnIdFromColumnName(duplicateDataStr, "documentname");
                List<Integer> documentIds = new LinkedList<>();
                for(int i=0;i<docCount;i++)
                {
                    JSONObject docObj = duplicateDataJson.getJSONArray("data").getJSONObject(i);
                    String documentNameValue = docObj.getJSONObject(Integer.toString(columnId)).getString("value");
                    String[] documentNames = documentNameValue.split(":;");
                    documentIds.add(Integer.valueOf(documentNames[1]));
                }

                //Hitting Trigger Reference API
                try {
                    logger.info("Trigger Reference API");
                    HttpResponse triggerReferenceResponse = AutoExtractionHelper.triggerReference(documentIds.get(0));
                    csAssert.assertTrue(triggerReferenceResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                    String triggerReferenceStr = EntityUtils.toString(triggerReferenceResponse.getEntity());
                    JSONObject triggerReferenceJson = new JSONObject(triggerReferenceStr);
                    boolean success = (boolean) triggerReferenceJson.get("success");
                    csAssert.assertTrue(success == true, "Trigger Reference is not working for the documents");

                    //Waiting for Reference to get complete
                    logger.info("Waiting for Reference to complete:");
                    Thread.sleep(35000);

                    //Now validating the reference has been extracted out of the document in AE listing
                    try {
                        logger.info("Validating the document Level Reference");
                        duplicateDataStr = References.listingResponse();
                        duplicateDataJson = new JSONObject(duplicateDataStr);
                        columnId = ListDataHelper.getColumnIdFromColumnName(duplicateDataStr, "referencedata");

                        JSONObject referenceObj = duplicateDataJson.getJSONArray("data").getJSONObject(0);
                        String referenceValue = referenceObj.getJSONObject(Integer.toString(columnId)).getString("value");
                        if(referenceValue.equals("[]"))
                        {
                            logger.info("No Reference document found");
                            csAssert.assertTrue(false,"No Reference document found");
                        }
                        else {
                            String[] references = referenceValue.split(",");
                            String referenceDoc = String.valueOf(references[1]);
                            csAssert.assertTrue(referenceDoc.contains("EXHIBIT A.docx") || referenceDoc.contains("Schedule 3.docx") , "Not extracting any Reference for a document");
                        }
                   try{
                       //Now validating the Clause Level Reference for a document
                       logger.info("Validating the clause level Reference of a document");
                       HttpResponse clauseTabResponse = AutoExtractionHelper.clauseTabListing(documentIds.get(0));
                       csAssert.assertTrue(clauseTabResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                       String clauseTabStr = EntityUtils.toString(clauseTabResponse.getEntity());
                       JSONObject clauseTabJson = new JSONObject(clauseTabStr);
                       int totalClauses = clauseTabJson.getJSONArray("data").length();
                       columnId = ListDataHelper.getColumnIdFromColumnName(clauseTabStr, "extractedreference");
                       List<String> clauseReferences = new LinkedList<>();
                       for(int i=0;i<totalClauses;i++)
                       {
                           JSONObject clauseReferenceObj = clauseTabJson.getJSONArray("data").getJSONObject(i);
                           String clauseReferenceValue = clauseReferenceObj.getJSONObject(Integer.toString(columnId)).getString("value");
                           if(clauseReferenceValue.isEmpty())
                           {
                               logger.info("Clause Level Reference value is null");
                           }
                           else {
                               String[] textReferences = clauseReferenceValue.split(",");
                               clauseReferences.add(String.valueOf(textReferences[1]));
                           }
                       }

                       Collections.sort(clauseReferences);
                       csAssert.assertTrue(!(clauseReferences.equals(null)),"Not extracting clause level Reference for a document");
                   }
                   catch (Exception e)
                   {
                       logger.info("Error occured while hitting clause Tab Data API");
                       csAssert.assertTrue(false,e.getMessage());
                   }

                    }
                    catch (Exception e)
                    {
                        logger.info("Error occured while hitting List Data API");
                        csAssert.assertTrue(false,e.getMessage());
                    }

                }
                catch (Exception e)
                {
                    logger.info("Error occured while Triggering Reference for a document");
                    csAssert.assertTrue(false,e.getMessage());
                }
            }
            catch (Exception e)
            {
                logger.info("Error occured while applying duplicate Data Filter");
                csAssert.assertTrue(false,e.getMessage());
            }
            csAssert.assertAll();
        }

}
