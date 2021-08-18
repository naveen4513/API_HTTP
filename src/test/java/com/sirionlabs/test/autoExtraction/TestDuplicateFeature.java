package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AEDRSHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

public class TestDuplicateFeature {
    private final static Logger logger = LoggerFactory.getLogger(TestDuplicateFeature.class);
    int newlyCreatedProjectId;
    String projectName;

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
    /*
    TC: C152142 Duplicate Data and Similarity Score Column on AE listing
    */
    @Test
    public void testC152142() {
        CustomAssert customAssert = new CustomAssert();
        try {
            HttpResponse docListMetaDataResponse = AutoExtractionHelper.checkAutoExtractionDocListingMetaData("/listRenderer/list/432/defaultUserListMetaData", "{}");
            customAssert.assertTrue(docListMetaDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is not Valid");
            String listingDataResponseStr = EntityUtils.toString(docListMetaDataResponse.getEntity());
            logger.info(listingDataResponseStr);
            JSONObject jsonObject = new JSONObject(listingDataResponseStr);

            int columnsLength = jsonObject.getJSONArray("columns").length();
            logger.info("Getting all Columns from List Page");
            List<String> allDefaultColumnsFromResponse = new LinkedList<>();
            for (int i = 0; i < columnsLength; i++) {
                allDefaultColumnsFromResponse.add(jsonObject.getJSONArray("columns").getJSONObject(i).get("queryName").toString());
            }
            logger.info("Validating Duplicate data and Similarity Score Columns");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("duplicatedata"), "DUPLICATE DATA Column Name is not Present");
            customAssert.assertTrue(allDefaultColumnsFromResponse.contains("similarityscore"), "Similarity Score Column Name is not Present");
        }
        catch (Exception e)
        {
            logger.info("Error occurred while while validating column data");
            customAssert.assertTrue(false,"Error occurred while while validating TC :C152142"+e.getMessage());
        }
        customAssert.assertAll();
    }

    /* TC C152143: Duplicate Data within Single Project.
     */
    @DataProvider
    public Object[][] dataProviderForDocumentUpload() {
        List<Object[]> referencesData = new ArrayList<>();

        String fileUploadName="Doc File API Automation.doc";
        referencesData.add(new Object[]{fileUploadName});

        fileUploadName="Schedule 2.docx";
        referencesData.add(new Object[]{fileUploadName});

        fileUploadName = "DuplicateSchedule2.docx";
        referencesData.add(new Object[]{ fileUploadName});


        return referencesData.toArray(new Object[0][]);
    }
    @Test(priority = 1)
    public void testDuplicateData() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Hitting list data API after applying Duplicate Show Filter");
            String duplicateDataStr = TestDuplicateFeature.listingResponse();
            JSONObject duplicateDataJson = new JSONObject(duplicateDataStr);
            int columnId = ListDataHelper.getColumnIdFromColumnName(duplicateDataStr, "duplicatedata");
            int recordColumn = ListDataHelper.getColumnIdFromColumnName(duplicateDataStr, "id");
            ArrayList<String> recordIdTovalidate = new ArrayList<>();
            ArrayList<String> duplicateIdToValidate = new ArrayList<>();
            logger.info("Getting two top most record to validate duplicate data feature");
            for (int i = 0; i < 2; i++) {
                String[] docIdValue = duplicateDataJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(recordColumn)).get("value").toString().split(":;");
                String docId = docIdValue[1];
                recordIdTovalidate.add(docId);
                JSONObject docObj = duplicateDataJson.getJSONArray("data").getJSONObject(i);
                String duplicateDataValue = docObj.getJSONObject(Integer.toString(columnId)).get("value").toString();
                csAssert.assertTrue(!(duplicateDataValue.equalsIgnoreCase("null")),"Duplicate data value is null for record Id "+docId);
                JSONArray duplicateDataObj = new JSONArray(duplicateDataValue);
                String data = duplicateDataObj.get(0).toString();
                JSONObject obj = new JSONObject(data);
                String duplicateDataId = obj.get("id").toString();
                duplicateIdToValidate.add(duplicateDataId);

            }
            logger.info("Validating duplicate data feature for record Ids "+recordIdTovalidate.get(0)+ "and "+recordIdTovalidate.get(1));
            csAssert.assertEquals(recordIdTovalidate.get(0), duplicateIdToValidate.get(1), "record Id is not same as duplicate data Id for record Id" + recordIdTovalidate.get(0));
            csAssert.assertEquals(recordIdTovalidate.get(1), duplicateIdToValidate.get(0), "record Id is not same as duplicate data Id for record Id" + recordIdTovalidate.get(1));testC152148();
            testC152148();
        }
        catch (Exception e)
        {
            logger.info("Exception while validating duplicate data feature because of "+e.getMessage());
            csAssert.assertTrue(false,"Exception while validating duplicate data feature");
        }

        csAssert.assertAll();

    }
    //TC C152148: Verify that it should not show the documents with similarity score 100% as those documents should be under duplicate data column
    public void testC152148()
    {
        logger.info("Start Test: Verify that duplicate document should not show similarity score 100%");
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Hitting list data API after applying Duplicate Show Filter");
            String duplicateDataStr = TestDuplicateFeature.listingResponse();
            JSONObject duplicateDataJson = new JSONObject(duplicateDataStr);
            int similarityColumn=ListDataHelper.getColumnIdFromColumnName(duplicateDataStr,"similarityscore");
            logger.info("Getting two top most record to validate similarity Score Column for duplicate data feature");
            for(int i=0;i<2;i++) {
                String similarityScoreValue = duplicateDataJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(similarityColumn)).get("value").toString();
                logger.info("Validating Similarity Score column for duplicate documents");
                csAssert.assertTrue(similarityScoreValue.equalsIgnoreCase("null"),"Similarity Score column value for Duplicate data is not null");
            }
        }
        catch (Exception e)
        {
            logger.info("Exception while validating duplicate data feature because of "+e.getMessage());
        }
        csAssert.assertAll();
    }

    //C152147: Verify that if two documents are completely different then it should not show similarity score or duplicate for those documents
    @Test(priority = 2)
    public void testC152147()
    {
        CustomAssert csAssert = new CustomAssert();
        try
        {
            logger.info("Applying Filter for the project under which documents were uploaded");
            HttpResponse projectFilterResponse = AutoExtractionHelper.projectFilter(newlyCreatedProjectId,projectName);
            csAssert.assertTrue(projectFilterResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String projectFilterStr = EntityUtils.toString(projectFilterResponse.getEntity());
            JSONObject projectFilterJson = new JSONObject(projectFilterStr);
            int similarityScoreColumnId = ListDataHelper.getColumnIdFromColumnName(projectFilterStr, "similarityscore");
            int duplicateDataColumnId=ListDataHelper.getColumnIdFromColumnName(projectFilterStr,"duplicatedata");
            String similarityScoreValue=projectFilterJson.getJSONArray("data").getJSONObject(2).getJSONObject(Integer.toString(similarityScoreColumnId)).get("value").toString();
            String duplicateDataValue=projectFilterJson.getJSONArray("data").getJSONObject(2).getJSONObject(Integer.toString(duplicateDataColumnId)).get("value").toString();
            logger.info("Validating similarity score column for completely different document");
            csAssert.assertTrue(similarityScoreValue.equals("null"),"Similarity Score Value is not NULL for completely different documents");
            logger.info("Validating duplicate data column for completely different document");
            csAssert.assertTrue(duplicateDataValue.equals("null"),"Duplicate data value is not NULL for two completely different Documents");

        }
        catch (Exception e)
        {
            logger.info("Exception while validating similarity Score and duplicate data for two completely different document due to "+e.getMessage());
            csAssert.assertTrue(false,"Exception while validating similarity Score and duplicate data for two completely different document");
        }
        csAssert.assertAll();
    }

    @Test(priority = 3)
    public void testC153702()
    {
        CustomAssert customAssert=new CustomAssert();
        try{
            String duplicateListDataStr=listingResponse();
            int duplicateDocsColumnId=ListDataHelper.getColumnIdFromColumnName(duplicateListDataStr,"duplicatedocument");
            JSONObject jsonObj=new JSONObject(duplicateListDataStr);
            int count=jsonObj.getJSONArray("data").length();
            HashSet<String> duplicateDocsValue=new HashSet<String>();
            if(count>0)
            {
                for(int i=0;i<count;i++)
                {
                    duplicateDocsValue.add(jsonObj.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(duplicateDocsColumnId)).getString("value"));
                }
                logger.info("validating duplicate docs column value");
                customAssert.assertTrue(duplicateDocsValue.contains("Yes")&&duplicateDocsValue.contains("No"),"Duplicate Doc column contains other than Yes/No");
            }
            else
            {
                throw new SkipException("Could not validate this TC as No data found on AE Listing Page");
            }
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        }
        catch (Exception e)
        {
            logger.info("Exception while validating duplicate doc column value due to "+e.getMessage());
            customAssert.assertTrue(false,"Exception while validating duplicate doc column value due to "+e.getMessage());

        }
        customAssert.assertAll();
    }

    @Test(priority = 4)
    public void testDuplicateDataDownload()
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
            if(allHeader.contains("DUPLICATE DATA"))
            {
              for(int i=0;i<allHeader.size();i++)
              {
                  if(allHeader.get(i).equalsIgnoreCase("DUPLICATE DATA"))
                  {
                      columnNo=i;
                  }
              }
              logger.info("Getting duplicate data column value from Downloaded sheet");
              String duplicateData=xlsUtils.getCellData("Data",columnNo,4);
              logger.info("Checking if Duplicate data column value is not in json format");
              logger.info("Duplicate data column value is "+duplicateData);
              csAssert.assertTrue(duplicateData.startsWith("("),"Duplicate data Value is json format");
            }
            else {
                csAssert.assertTrue(false,"Duplicate data Column is not present in downloaded sheet for document id "+documentId);
            }
        }
        catch (Exception e)
        {
            logger.info("Exception while validating duplicate data download");
            csAssert.assertTrue(false,"Exception while validating duplicate data download");
        }
        finally {
            logger.info("Deleting downloaded file from location from:"+outputFilePath + "/" + "ListDataDownloadOutput"+"/"+aeDataEntity + "/" + outputFileName);
            FileUtils.deleteFile(outputFilePath + "/" + "ListDataDownloadOutput"+"/"+aeDataEntity + "/" + outputFileName);
            logger.info(outputFileName+" Downloaded sheet Deleted successfully.");
        }
        csAssert.assertAll();

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
                AEDRSHelper aedrsHelper=new AEDRSHelper();
                String drsFlag=aedrsHelper.checkDRSFlag();
                String payload=null;

                if (drsFlag.equalsIgnoreCase("true"))
                {
                    payload=aedrsHelper.getPayloadforDRS(templateFilePath,templateFileName,newlyCreatedProjectId);
                }
                else {
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                }
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

    public static String listingResponse() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        String duplicateDataStr = null;
        try {
            logger.info("Hitting list data API with duplicate docs filter");
            HttpResponse duplicateDataResponse = AutoExtractionHelper.duplicateDataFilter();
            csAssert.assertTrue(duplicateDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            duplicateDataStr = EntityUtils.toString(duplicateDataResponse.getEntity());

        }
        catch (Exception e)
        {
            logger.info("Exception while validating list data with filter duplicate show because of "+e.getMessage());
            csAssert.assertTrue(false,"Exception while validating list data with filter duplicate show because of "+e.getMessage());
        }
        return duplicateDataStr;
    }



}
