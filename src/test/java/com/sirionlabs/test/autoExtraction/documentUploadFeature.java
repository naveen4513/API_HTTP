package com.sirionlabs.test.autoExtraction;
import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AEDRSHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

public class documentUploadFeature extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(documentUploadFeature.class);
    static long duration = 0;
    static int newlyCreatedProjectId;
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
        } catch (Exception e) {
            csAssert.assertTrue(false, "Project Create API is not working because of :" + e.getStackTrace());
        }
    }

    @DataProvider
    public Object[][] dataProviderForDocumentUpload() {
        List<Object[]> allDataToTest = new ArrayList<>();

        String DocumentFormat="HtmFileDocumentUpload";
        String fileUploadName ="HTM File API Automation.htm";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="HtmlFileDocumentUpload";
        fileUploadName = "HTML File API Automation.html";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="DocFileDocumentUpload";
        fileUploadName = "Doc File API Automation.doc";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="EmlFileDocumentUpload";
        fileUploadName ="EML File API Automation.eml";
        allDataToTest.add(new Object[]{DocumentFormat,fileUploadName});

        DocumentFormat="MSGFileDocumentUpload";
        fileUploadName = "MSG File API Automation.msg";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="TextFileDocumentUpload";
        fileUploadName = "TXT File API Automation.txt";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="ZipFileDocumentUpload";
        fileUploadName = "Zip File API Automation.zip";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="JPGImageFileDocumentUpload";
        fileUploadName = "JPG Image File API Automation.jpg";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="TiffImageFileDocumentUpload";
        fileUploadName = "Tiff Image File API Automation.tiff";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="PngImageFileDocumentUpload";
        fileUploadName = "PNG Image File API Automation.png";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="JpegImageFileDocumentUpload";
        fileUploadName = "JPEG Image File API Automation.jpeg";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="TifImageFileDocumentUpload";
        fileUploadName = "Tif Image File API Automation.tif";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        logger.info("Total Flows to Test : {}", allDataToTest.size());
        return allDataToTest.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForEmbeddedUpload() {
        List<Object[]> allDataToTest = new ArrayList<>();
        String DocumentFormat = "MSGFileDocumentUploadEmbedded";
        String fileUploadName = "Embedded MSG File API Automation.msg";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat = "EMLFileDocumentUploadEmbedded";
        fileUploadName = "Embedded EML File API Automation.eml";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat = "DOCSFileDocumentUploadEmbedded";
        fileUploadName = "Embedded Docx File API Automation.docx";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        DocumentFormat="ZipFileDocumentUpload";
        fileUploadName = "Zip File API Automation.zip";
        allDataToTest.add(new Object[]{DocumentFormat, fileUploadName});

        logger.info("Total Flows to Test : {}", allDataToTest.size());
        return allDataToTest.toArray(new Object[0][]);
    }

    //TC :C152432 Verify that Document upload and completion is working fine for new file extensions

    @Test(dataProvider ="dataProviderForDocumentUpload",enabled = true)
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
            try {
                // Hit Global Upload API
                logger.info("Hit Global Upload API");
                if(drsFlag.equalsIgnoreCase("true"))
                {
                    payload=obj.getPayloadforDRS(templateFilePath,templateFileName,newlyCreatedProjectId);
                }
                else {
                    Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                }
               // String payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":["+newlyCreatedProjectId+"]}]";
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                logger.info("Checking whether Extraction Status is complete or not");
                boolean isExtractionCompletedForUploadedFile = checkExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
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
    /*
       TC:C152284 All embedded files should be Completed And there extraction should be successfully Completed
       TC:C152328
     */
    @Test(dataProvider = "dataProviderForEmbeddedUpload",enabled = true)
    public static void embeddedFileUpload(String DocumentFormat,String fileUploadName) throws IOException, InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        try
        {
            Thread.sleep(5000);
            String templateFilePath ="src/test/resources/TestConfig/AutoExtraction/UploadFiles";
            String templateFileName=fileUploadName;
            String drsFlag;
            AEDRSHelper obj = new AEDRSHelper();
            drsFlag = obj.checkDRSFlag();
            String payload=null;
            logger.info("Uploading Embedded Document."+templateFileName);
            Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);
            try{
                logger.info("Hit Global Upload API");
                if(drsFlag.equalsIgnoreCase("true"))
                {
                    payload=obj.getPayloadforDRS(templateFilePath,templateFileName,newlyCreatedProjectId);
                }
                else {
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                }
               // String payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":["+newlyCreatedProjectId+"]}]";
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                logger.info("Checking whether Source document name of Extracted document is same as parent document.");
                Thread.sleep(5000);
                String actualSourceDocumentFromList = AutoExtractionHelper.getSourceDocument();
                String[] actualSourceDocument = actualSourceDocumentFromList.split("\\)");
                String actualDocument = actualSourceDocument[1].trim();
                String documentId = actualSourceDocument[0].trim().split("\\(")[1];
                logger.info("Validating Source document for Extracted document from embedded file" +DocumentFormat+" whose parent Document ID is" + documentId);
                csAssert.assertEquals(actualDocument, templateFileName, "Source document is not same as Parent document whose parent document ID" + documentId);
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false,"Global Upload API is not working because of :"+ e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Embedded file extraction for the file "+DocumentFormat+" " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void FolderUpload(){
        CustomAssert csAssert = new CustomAssert();
        logger.info("Testing Global Upload for Folder Upload");
        // File Upload API to get the key of file uploaded
        logger.info("File Upload API to get the key of file that has been uploaded");
        try {
            String templateFilePath = "src/test/resources/TestConfig/AutoExtraction/UploadFiles/TestFolder1/TestFolder";
            String templateFileName="Text File Within Folder Automation.txt";
            String templateFolder="TestFolder1/TestFolder";
            String templateFolderName = "TestFolder";
            String drsFlag;
            AEDRSHelper obj = new AEDRSHelper();
            drsFlag = obj.checkDRSFlag();
            String payload=null;
            Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);

            try {
                // Hit Global Upload API
                if(drsFlag.equalsIgnoreCase("true"))
                {
                    payload=obj.getPayloadforDRS(templateFilePath,templateFileName,newlyCreatedProjectId);
                }
                else {
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                }
               //String payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"relativePath\": \""+templateFolder+"\",\"projectIds\":["+newlyCreatedProjectId+"]}]";
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                boolean isExtractionCompletedDocument = checkExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedDocument,"Folder Upload are not getting completed ");
                try{
                    HttpResponse listResponse=AutoExtractionHelper.duplicateDataFilter();
                    csAssert.assertTrue(listResponse.getStatusLine().getStatusCode()==200,"Response code is Invalid");
                    String listResponseStr= EntityUtils.toString(listResponse.getEntity());
                    int folderColumnId = ListDataHelper.getColumnIdFromColumnName(listResponseStr, "folder");
                    JSONObject listingResponseJson = new JSONObject(listResponseStr);
                    String folderName=listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(folderColumnId)).get("value").toString();
                    csAssert.assertEquals(templateFolderName,folderName.trim(),"Folder Name is not same as uploaded folder Name. ");
                }
                catch (Exception e)
                {
                    csAssert.assertTrue(false,"Exception while validating Folder Column:"+ e.getMessage());
                }
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false,"Global Upload API is not working because:"+ e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Folder Upload API is not working because :" + e.getMessage());
        }
        csAssert.assertAll();
    }

    public static boolean checkExtractionStatus(String endUserName,String endUserPassword) throws IOException, InterruptedException
    {
        {
            boolean isExtractionCompleted = false;
            Check check = new Check();
            CustomAssert csAssert = new CustomAssert();
            check.hitCheck(endUserName, endUserPassword);
            LocalTime initialTime = LocalTime.now();
            Thread.sleep(20000);
            try {
                String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
                String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"438\":{\"filterId\":\"438\",\"filterName\":\"duplicatedocs\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"AssignedShow\"}]}}}},\"selectedColumns\":[]}";
                HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
                csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode()==200,"Response code is Invalid");
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
                                    Assert.fail("Extraction is working slow waited for 10 minutes.Please look manually whether their is problem in extraction or services are working slow."+ "Waited for:" + duration + "seconds for the document completion");
                                } else
                                {
                                    isExtractionCompleted = checkExtractionStatus(endUserName, endUserPassword);
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

                }
                catch (Exception e)
                {
                    logger.error("Exception while hitting Project Listing API. {}", e.getMessage());

                }
            }
            catch (Exception e)
            {
                logger.error("Exception while hitting Automation List Data API. {}", e.getMessage());
            }

            finally {
                duration = 0;
            }
            return isExtractionCompleted;
        }
    }
}

