package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AEDRSHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.GetDocumentIdHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Clusters {
    private final static Logger logger = LoggerFactory.getLogger(Clusters.class);
    CustomAssert csAssert = new CustomAssert();
    int newlyCreatedProjectId;
    String projectName;
    static int documentId=-1;

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
        List<Object[]> allDataToTest = new ArrayList<>();

        String DocumentFormat = "SimilarDoc1APIAutomation";
        String fileUploadName = "Similar Doc1 API Automation.docx";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat = "SimilarDoc2APIAutomation";
        fileUploadName = "Similar Doc2 API Automation.docx";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="SimilarDoc2APIAutomation";
        fileUploadName="11 page KAMADALTD_DRSADraftR_3252013.docx";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        logger.info("Total Flows to Test : {}", allDataToTest.size());
        return allDataToTest.toArray(new Object[0][]);
    }

    @Test(dataProvider ="dataProviderForDocumentUpload")
    public void TestFileUpload(String DocumentFormat,String fileUploadName)throws IOException {
        logger.info("Testing Global Upload API for "+DocumentFormat);
        CustomAssert csAssert = new CustomAssert();
        // File Upload API to get the key of file uploaded
        logger.info("File Upload API to get the key of file that has been uploaded");
        try {
            String templateFilePath ="src/test/resources/TestConfig/AutoExtraction/UploadFiles";
            String templateFileName=fileUploadName;
            String drsFlag;
            AEDRSHelper obj = new AEDRSHelper();
            drsFlag = obj.checkDRSFlag();
            String payload=null;
            Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);

            try {
                // Hit Global Upload API
                logger.info("Hit Global Upload API");
                if(drsFlag.equalsIgnoreCase("true"))
                {
                    payload=obj.getPayloadforDRS(templateFilePath,templateFileName,newlyCreatedProjectId);
                }
                else {
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                }
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                logger.info("Checking whether Extraction Status is complete or not");
                boolean isExtractionCompletedForUploadedFile = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForUploadedFile," "+DocumentFormat+" is not getting completed ");


            }
            catch (Exception e)
            {
                csAssert.assertTrue(false,"Global Upload API is not working because of :"+ e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "File Upload API is not working because of :" + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test
    public void validateClusters() throws IOException
    {
        logger.info("Checking whether the clusters have been created or not");
        try {
            logger.info("Applying Filter for the project under which documents were uploaded");
            HttpResponse projectFilterResponse = AutoExtractionHelper.projectFilter(newlyCreatedProjectId, projectName);
            csAssert.assertTrue(projectFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String projectFilterStr = EntityUtils.toString(projectFilterResponse.getEntity());
            JSONObject projectFilterJson = new JSONObject(projectFilterStr);
            int documentCount = projectFilterJson.getJSONArray("data").length();
            int columnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "clusters");
            int documentColumnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "documentname");
            for(int i =0;i<documentCount;i++)
            {
                JSONObject clusterObj = projectFilterJson.getJSONArray("data").getJSONObject(i);
                String clusterValue = clusterObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String documentDetails = clusterObj.getJSONObject(Integer.toString(documentColumnId)).getString("value");
                String[] documentName = documentDetails.split(":;");
                int documentId = Integer.valueOf(documentName[1]);
                csAssert.assertTrue(!clusterValue.equalsIgnoreCase(null),"No cluster has been extracted out of these documents"+ " " + documentId);
                logger.info("Cluster Name for" +" " +documentId + " " + clusterValue);
                logger.info("Validating Similarity Score..");
                validateSimilarityScore();
            }

        }
        catch (Exception e)
        {
            logger.info("Getting Exception while validating cluster/similarity score due to "+e.getMessage());
            csAssert.assertTrue(false,"Getting Exception while validating cluster/similarity score due to  "+ e.getMessage());

        }

        csAssert.assertAll();
    }

    /*TC: C152144 Similarity Score within Single Project
          C152146 Threshold for Similarity Score within the documents
    */
    public void validateSimilarityScore()
    {
        CustomAssert csAssert=new CustomAssert();
        logger.info("Checking whether the Similarity Score have been created or not");
        try {
            logger.info("Applying Filter for the project under which documents were uploaded");
            HttpResponse projectFilterResponse = AutoExtractionHelper.projectFilter(newlyCreatedProjectId, projectName);
            csAssert.assertTrue(projectFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String projectFilterStr = EntityUtils.toString(projectFilterResponse.getEntity());
            JSONObject projectFilterJson = new JSONObject(projectFilterStr);
            int documentCount = projectFilterJson.getJSONArray("data").length();
            int columnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "similarityscore");
            int documentColumnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "documentname");
            List<String> expectedDocumentIds=new ArrayList<>();
            List<String> actualDocumentId=new ArrayList<>();
            List<String> similarityScoreList=new ArrayList<>();
            for(int i =0;i<documentCount;i++)
            {
                JSONObject listDataObj = projectFilterJson.getJSONArray("data").getJSONObject(i);
                String similarityScoreValue = listDataObj.getJSONObject(Integer.toString(columnId)).getString("value");
                String documentDetails = listDataObj.getJSONObject(Integer.toString(documentColumnId)).getString("value");
                String[] documentName = documentDetails.split(":;");
                String documentId = documentName[1];
                expectedDocumentIds.add(documentId);
                JSONArray similarDataObj = new JSONArray(similarityScoreValue);
                String data = similarDataObj.get(0).toString();
                JSONObject obj = new JSONObject(data);
                String similarDataId = obj.get("id").toString();
                actualDocumentId.add(similarDataId);
                String similarityScore=obj.get("similarityScore").toString();
                similarityScoreList.add(similarityScore);

            }
            logger.info("Validating Similarity score for documents " +expectedDocumentIds.get(0)+" and "+expectedDocumentIds.get(1));
            csAssert.assertEquals(expectedDocumentIds.get(0),actualDocumentId.get(1),"Document Id is not same as Similar Document ID for document Id "+expectedDocumentIds.get(0));
            csAssert.assertEquals(expectedDocumentIds.get(1),actualDocumentId.get(0),"Document Id is not same as Similar Document ID for document Id "+expectedDocumentIds.get(1));
            csAssert.assertEquals(similarityScoreList.get(1),similarityScoreList.get(0),"Similarity score value for document id "+expectedDocumentIds.get(0)+" is not same as document Id "+expectedDocumentIds.get(0));
            logger.info("Validating if similarity score is greater than or equal to 99.0 % for document id "+expectedDocumentIds.get(0));
            csAssert.assertTrue(Float.parseFloat(similarityScoreList.get(0))>=99.0,"Similarity score is less than 99% for document id "+expectedDocumentIds.get(0));
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception while validating Similarity Score :"+ e.getMessage());

        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testSimilarityScoreDataDownload()
    {
        CustomAssert csAssert=new CustomAssert();
        String outputFileName = "Autoextraction Doc Listing.xlsx";
        String outputFilePath="src/test/output";
        String aeDataEntity = "auto extraction";
        int entityUrlId=432;
        try {
            logger.info("Applying Filter for the project under which documents were uploaded");
            HttpResponse projectFilterResponse = AutoExtractionHelper.projectFilter(newlyCreatedProjectId, projectName);
            csAssert.assertTrue(projectFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String projectFilterStr = EntityUtils.toString(projectFilterResponse.getEntity());
            JSONObject projectFilterJson = new JSONObject(projectFilterStr);
            int documentColumnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "id");
            String[] idValue = projectFilterJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(documentColumnId)).getString("value").split(":;");
            String documentId= idValue[1];
            logger.info("Downloading Auto Extraction Selected Listing Data");
            DownloadListWithData DownloadListWithDataObj = new DownloadListWithData();
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{},\"customFilter\":{\"downloadSelectedIds\":{\"selectIds\":["+documentId+"]}}}}";
            Map<String, String> formParam = new HashMap<>();
            formParam.put("_csrf_token", "null");
            formParam.put("jsonData", payload);
            HttpResponse downloadResponse = DownloadListWithDataObj.hitDownloadListWithData(formParam, entityUrlId);
            logger.info("Checking if Auto extraction listing is downloaded");
            String downloadStatus =DownloadListWithDataObj.dumpDownloadListWithDataResponseIntoFile(downloadResponse, outputFilePath, "ListDataDownloadOutput",aeDataEntity,"Autoextraction Doc Listing");
            if (downloadStatus == null) {
                csAssert.assertTrue(false, "Auto Extraction List Download is unsuccessful");
            }
            else {
                logger.info("list data Excel has been downloaded");
            }
            String filePath=outputFilePath + "/"+"ListDataDownloadOutput/" +aeDataEntity;
            String fileName=outputFileName;
            XLSUtils xlsUtils=new XLSUtils(filePath,fileName);
            logger.info("Getting all sheet names of downloaded sheet");
            List<String> allSheet=xlsUtils.getSheetNames();
            csAssert.assertTrue(allSheet.size()==2,"There is more than 2 sheet in Downloaded Extracted data Excel sheet for Document id "+documentId);
            logger.info("Checking if Data sheet is present in downloaded extracted data sheet for document id "+documentId);
            boolean isDataPresent=xlsUtils.isSheetExist("Data");
            csAssert.assertTrue(isDataPresent,"Data Sheet is not present in downloaded extracted data sheet for document id "+documentId);
            List<String> allHeader=xlsUtils.getOffSetHeaders(filePath,fileName,"Data");
            int columnNo=-1;
            if(allHeader.contains("SIMILARITY SCORE"))
            {
                for(int i=0;i<allHeader.size();i++)
                {
                    if(allHeader.get(i).equalsIgnoreCase("SIMILARITY SCORE"))
                    {
                        columnNo=i;
                    }
                }
                logger.info("Getting SIMILARITY SCORE column value from Downloaded sheet");
                String similarityScore=xlsUtils.getCellData("Data",columnNo,4);
                logger.info("Checking if SIMILARITY SCORE column value is not in json format");
                logger.info("Similarity Score column value is "+similarityScore);
                csAssert.assertTrue(similarityScore.startsWith("("),"similarity Score Value is in json format");
            }
            else {
                csAssert.assertTrue(false,"SIMILARITY SCORE Column is not present in downloaded sheet for document id "+documentId);
            }
        }
        catch (Exception e)
        {
            logger.info("Exception while validating SIMILARITY SCORE download");
            csAssert.assertTrue(false,"Exception while validating SIMILARITY SCORE download");
        }
        finally {
            logger.info("Deleting downloaded file from location from:"+outputFilePath + "/" + "ListDataDownloadOutput"+"/"+aeDataEntity + "/" + outputFileName);
            FileUtils.deleteFile(outputFilePath + "/" + "ListDataDownloadOutput"+"/"+aeDataEntity + "/" + outputFileName);
            logger.info(outputFileName+" Downloaded sheet Deleted successfully.");
        }
        csAssert.assertAll();

    }

    @Test(enabled = true)
    public  void testMaxSimilarityScore()
    {
        CustomAssert csAssert=new CustomAssert();
        try
        {
            documentId= GetDocumentIdHelper.getDocIdOfLatestDocument();
            logger.info("Applying Filter for the project under which documents were uploaded");
            HttpResponse projectFilterResponse = AutoExtractionHelper.projectFilter(newlyCreatedProjectId,projectName);
            csAssert.assertTrue(projectFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String projectFilterStr = EntityUtils.toString(projectFilterResponse.getEntity());
            JSONObject projectFilterJson = new JSONObject(projectFilterStr);

            int similarityScoreColumnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "similarityscore");
            int maxSimilarityScoreColumnId=ListDataHelper.getColumnIdFromColumnName(projectFilterStr,"maxsimilarityscore");
            HashMap<String,String> similarityScoreData=new HashMap<>();
            HashMap<String,String> maxSimilarityScoreData=new HashMap<>();
            JSONObject listDataObj = projectFilterJson.getJSONArray("data").getJSONObject(0);

            //Storing similarity score data in map
            String similarityScoreValue = listDataObj.getJSONObject(Integer.toString(similarityScoreColumnId)).getString("value");
            JSONArray similarityDataList=new JSONArray(similarityScoreValue);
            for(int i=0;i<similarityDataList.length();i++) {
                String id=similarityDataList.getJSONObject(i).get("id").toString();
                String score=similarityDataList.getJSONObject(i).get("similarityScore").toString();

                similarityScoreData.put(id, score);
            }

            Map.Entry<String, String> maxEntry = null;

            for (Map.Entry<String, String> entry : similarityScoreData.entrySet())
            {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                {
                    maxEntry = entry;
                }
            }
            logger.info("Maximum similarity score value is "+maxEntry);

            //Storing maximum Similarity Score data in map
            String maxSimilarityScoreValue=listDataObj.getJSONObject(Integer.toString(maxSimilarityScoreColumnId)).getString("value");
            JSONArray maxSimilarityDataList=new JSONArray(maxSimilarityScoreValue);
            for(int i=0;i<maxSimilarityDataList.length();i++) {
                String id=maxSimilarityDataList.getJSONObject(i).get("id").toString();
                String score=maxSimilarityDataList.getJSONObject(i).get("similarityScore").toString();
                maxSimilarityScoreData.put(id, score);
            }
            for(String key:maxSimilarityScoreData.keySet()) {
                csAssert.assertTrue(maxSimilarityScoreData.get(key).equals(maxEntry.getValue()), "Maximum Similarity score value is not equal to maximum value of Similarity score column");
            }
        }
        catch (Exception e)
        {
            logger.info("Getting exception while validating maximum similarity Score due to "+e.getMessage());
            csAssert.assertTrue(false,"Getting exception while validating maximum similarity Score due to "+e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public  void testPageSimilarityScore() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        try {
            documentId = GetDocumentIdHelper.getDocIdOfLatestDocument();
            logger.info("Applying Filter for the project under which documents were uploaded");
            HttpResponse projectFilterResponse = AutoExtractionHelper.projectFilter(newlyCreatedProjectId, projectName);
            csAssert.assertTrue(projectFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String projectFilterStr = EntityUtils.toString(projectFilterResponse.getEntity());
            JSONObject projectFilterJson = new JSONObject(projectFilterStr);
            int pageSimilarityScoreColumnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "maxpagesimilarityscore");
            HashMap<String,String> pageSimilarityScoreData=new HashMap<>();
            //Storing page Similarity Score data in a map
            JSONObject listDataObj = projectFilterJson.getJSONArray("data").getJSONObject(0);
            String pageSimilarityScoreValue = listDataObj.getJSONObject(Integer.toString(pageSimilarityScoreColumnId)).getString("value");
            JSONArray pageSimilarityDataList=new JSONArray(pageSimilarityScoreValue);
            for(int i=0;i<pageSimilarityDataList.length();i++) {
                String id=pageSimilarityDataList.getJSONObject(i).get("id").toString();
                String score=pageSimilarityDataList.getJSONObject(i).get("maxPageScoreData").toString();
                pageSimilarityScoreData.put(id,score);
            }
            for(String key: pageSimilarityScoreData.keySet()) {
                csAssert.assertTrue(Float.parseFloat(pageSimilarityScoreData.get(key))>0.0,"Page score value is not greater than 0.0 for document id "+documentId);
            }
            logger.info(" max page similarity score for document id "+documentId+" is "+pageSimilarityScoreData);

        }
        catch (Exception e)
        {
            logger.info("Getting exception while validating page similarity score");
            csAssert.assertTrue(false,"Getting exception while validating page similarity score due to "+e.getMessage());

        }
        csAssert.assertAll();



    }


}
