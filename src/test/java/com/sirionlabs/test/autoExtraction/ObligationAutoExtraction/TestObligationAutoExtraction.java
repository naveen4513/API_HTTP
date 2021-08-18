package com.sirionlabs.test.autoExtraction.ObligationAutoExtraction;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Login;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class TestObligationAutoExtraction {

private final static Logger logger = LoggerFactory.getLogger(TestObligationAutoExtraction.class);
private static String docFilePath = null;
// Remote Server Password  18.185.148.80    BI_US_SD@7

private static HttpHost httpHost;
private static SoftAssert softAssert;
private static APIUtils apiUtils = new APIUtils();
private static String contractId =  "17306";
private static String relationId =  "231721";
private static String newDocId;
private static String clientAdminUserId = "ashutosh.vfadmin";
private static String clientAdminPassword = "Toyota@123";
private static String endUserId = "integration.user";
private static String endUserPassword = "admin12345";
private static String endUserIdWithNoAutoExtractionPermission = "akshay_chawla";
private static String endUserPasswordWithNoAutoExtractionPermission = "admin123";
private static String contractIdWithNoAutoExtraction =  "18413";
private static String relationIdWithNoAutoExtraction =  "91304";
private static int alreadyApprovedDnoId;

@BeforeClass
public void beforeclass(){
    httpHost = new HttpHost("vfsdbox.sirioncloud.io",-1,"https");
    docFilePath = "src/test/output/AutoextractionObligationUploadDocuments";
}

//@Test(dataProvider = "filesForObligationAutoExtraction",priority = 1)
public void TestObligationAutoExtraction(File fileToUpload) throws IOException, SQLException {
    //Hit Document Show API
    softAssert = new SoftAssert();
    HttpResponse contractShowResponse = hitContractShowAPI(contractId);
    String contractShowStrResponse = EntityUtils.toString(contractShowResponse.getEntity());
    softAssert.assertTrue(APIUtils.validJsonResponse(contractShowStrResponse),"Contract Show Response is not valid JSON");

    //Edit Document with new Uploaded Document
    Map<String,String> formData = new HashMap<>();
    String key = RandomStringUtils.randomAlphabetic(18);
    formData.put("key", key);
    formData.put("name", fileToUpload.getName().split("\\.")[0]);
    formData.put("extension", fileToUpload.getName().split("\\.")[1]);
    formData.put("relationId", relationId);
    String uploadDocAPIResponse = hitContractDocumentUpload(docFilePath,fileToUpload.getName(),formData);

    // Create Payload to edit
    String payload =createPayloadToEditDoc(uploadDocAPIResponse,contractShowStrResponse,key,fileToUpload);

    // Execute Edit Contract API
    HttpResponse editContractWithDocResponse = hitContractEditAPI(payload);

    softAssert.assertTrue(editContractWithDocResponse.getStatusLine().getStatusCode()==200,
            "Document is not edited successfully");

    //Get newly Updated Doc Id from Database
    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("172.31.25.58","5432","vf_sdbox_20171221","yogesh_sharma","W0Q==");
    String query = "select * from contract_document where contract_id =" + Integer.valueOf(contractId)  +" and name  = " + "'"+ fileToUpload.getName().split("\\.")[0] + "'" +" order by id desc LIMIT 1";
    List<List<String>> docIds = postgreSQLJDBC.doSelect(query);
    newDocId = docIds.stream().findFirst().get().get(0);

    query = "select * from contract_document_autoextraction_request where document_id = " + Integer.valueOf(newDocId) + " order by id desc";
    List<List<String>> contract_autoextraction = postgreSQLJDBC.doSelect(query);
    String status = contract_autoextraction.stream().findFirst().get().get(4);

    while(Integer.valueOf(status)!= 4){
        contract_autoextraction = postgreSQLJDBC.doSelect(query);
        status = contract_autoextraction.stream().findFirst().get().get(4);

        if(Integer.valueOf(status) == 0){
            logger.info("Auto Extraction INITIATION_PENDING");
        }
        else if(Integer.valueOf(status) == 1){
            logger.info("Auto Extraction INITIATION_TRIGGERED");
        }
        else if(Integer.valueOf(status) == 2){
            logger.info("Auto Extraction INITIATION_SUCCESS");
        }
        else if(Integer.valueOf(status) == 3){
            logger.error("Auto Extraction INITIATION_FAILED");
            throw new SkipException("INITIATION_FAILED states services are not up to initiate auto extraction");
        }
        else if(Integer.valueOf(status) == 5){
            logger.error("AUTO_EXTRACTION_FAILURE");
            throw new SkipException("AUTO_EXTRACTION_FAILURE document is either password protected or corrupt");
        }
    }

    softAssert.assertTrue(Integer.valueOf(status)==4,"AUTO_EXTRACTION_SUCCESS");

    // Document Show API
    HttpResponse response = hitContractDocumentShowAPI(newDocId);
    String contractDocumentShowResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject contractDocumentShowResponse = new JSONObject(contractDocumentShowResponseStr);
    String extractedObligationDataUrl = contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(1).getJSONArray("fields").getJSONObject(0).getJSONArray("fields").getJSONObject(0).get("dataURL").toString();

    // Verify Extracted Obligation Tab is visible
    int tabsLength = contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").length();

    List<String> allTabsData = new LinkedList<>();
    for(int i=0;i<tabsLength;i++){
        allTabsData.add(contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(i).get("label").toString());
    }

    softAssert.assertTrue(allTabsData.contains("Extracted Obligations"),"Extracted Obligation tab is visible even the permission of auto extraction is not given to this user");

    payload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";

    List<String> stagingStatus = new LinkedList<>();
    List<String> extractedIds = new LinkedList<>();

    // Extracted Obligation List API
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Extracted Obligation Data is not reflected");

    String extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject extractedObligationResponse = new JSONObject(extractedObligationResponseStr);
    List<String> allColumnsExtractedObligation = ListDataHelper.getAllColumnName(extractedObligationResponseStr);
    int statusColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"stagingStatus");
    int extractedObligationColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"stagingClientPrimaryKey");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        stagingStatus.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(statusColumnId)).get("value").toString());
        extractedIds.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractedObligationColumnId)).get("value").toString());
    }

    softAssert.assertTrue(stagingStatus.stream().findAny().get().contains("Pending Review"),
            "Status is not coming Pending review");
    softAssert.assertTrue(extractedIds.stream().findAny().get().startsWith("EXOB"),
            "EXOB prefix is not seen on extracted obligation");

    // Hit Download Extracted Obligation Excel
    Map<String,String> form = new HashMap<>();
    form.put("filterData","{\"offset\":0,\"fetchAllData\":true,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}");
    form.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
    form.put("fileName", fileToUpload.getName().split("\\.")[0]);

    response = hitDownloadExtractedObligationExcel("src/test/output/AutoextractionObligationUploadDocuments/DownloadAutoextractedObligation/",form,fileToUpload.getName());

    softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,
            "Extracted Obligation Data Excel is not downloaded");
    softAssert.assertAll();
}

//@Test(priority = 2)
public void verifyRejectExtractedObligationAndSubmitFeedbackForm() throws IOException {
    softAssert = new SoftAssert();
    // Document Show API
    HttpResponse response = hitContractDocumentShowAPI(newDocId);
    String contractDocumentShowResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject contractDocumentShowResponse = new JSONObject(contractDocumentShowResponseStr);
    String extractedObligationDataUrl = contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(1).getJSONArray("fields").getJSONObject(0).getJSONArray("fields").getJSONObject(0).get("dataURL").toString();

    String payload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";

    // Extracted Obligation List API
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
    String extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject extractedObligationResponse = new JSONObject(extractedObligationResponseStr);
    List<Integer> extractedIds = new LinkedList<>();
    List<String> clientPrimaryKeys = new LinkedList<>();
    List<String> extractedDataStatus = new LinkedList<>();
    int extractedId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"id");
    int clientPrimaryKeyColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"stagingClientPrimaryKey");
    int extractionStatusColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"stagingStatus");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        extractedIds.add(Integer.valueOf(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractedId)).get("value").toString()));
        clientPrimaryKeys.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(clientPrimaryKeyColumnId)).get("value").toString().split(";")[1].trim());
        extractedDataStatus.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractionStatusColumnId)).get("value").toString());
    }

    List<Integer> extractedIdsAfterSorting = extractedIds.stream().sorted().collect(Collectors.toList());

    //  Verifying Default format of Extracted ids are sorted
    softAssert.assertTrue(extractedIds.equals(extractedIdsAfterSorting),
            "Default format of extracted ids are not sorted");

    softAssert.assertTrue(!extractedDataStatus.contains("Extracted data Rejected"),
            "Error in Auto Extraction all the status should be Pending Review once Auto Extraction is Completed");

    // Verify Feedback Form
    response = hitGetFeedbackFormAPI();
    String getFeedbackFormResponseStr = EntityUtils.toString(response.getEntity());

    softAssert.assertTrue(getFeedbackFormResponseStr.equals("{\"status\": true, \"feedback_questions\": [{\"multiSelect\": false, \"options\": [{\"optionId\": 1, \"optionText\": \"partially captured\"}, {\"optionId\": 2, \"optionText\": \"extra captured\"}, {\"optionId\": 4, \"optionText\": \"correctly captured\"}, {\"optionId\": 5, \"optionText\": \"wrongly captured\"}], \"questionName\": \"Reason for editing/cancelling the obligation ?\", \"questionId\": 1}, {\"multiSelect\": true, \"options\": [{\"optionId\": 6, \"optionText\": \"responsibility\"}, {\"optionId\": 7, \"optionText\": \"frequency\"}, {\"optionId\": 8, \"optionText\": \"category\"}, {\"optionId\": 9, \"optionText\": \"sub category\"}, {\"optionId\": 10, \"optionText\": \"performance type\"}, {\"optionId\": 11, \"optionText\": \"Triggered/Non-Triggered\"}, {\"optionId\": 12, \"optionText\": \"evidence\"}], \"questionName\": \"Incorrectly captured obligation attributes ?\", \"questionId\": 2}]}"),
            "Feedback Form is not visible");

    //Hit Reject Status Update API
    String clientPrimaryKeyId = clientPrimaryKeys.stream().findFirst().get();
    response = hitUpdateRejectStatusExtractionAPI(clientPrimaryKeyId);

    String updateRejectStatusStr = EntityUtils.toString(response.getEntity());
    softAssert.assertTrue(updateRejectStatusStr.contains("\"message\":\"Updated Successfully\""),
            "Extracted Obligation Status is not updated to reject");

    // Hit Feedback API and delete extracted any one extracted Id
    int extractedObligationId = extractedIds.stream().findFirst().get();
    String feedbackPayload = "{\"feedback\":[{\"questionId\":1,\"selectedOption\":5},{\"questionId\":2,\"selectedOption\":[9]}],\"dataId\":"+ extractedObligationId +"}";

    response = hitFeedbackAPI(feedbackPayload);

    String feedbackStatusStr = EntityUtils.toString(response.getEntity());
    softAssert.assertTrue(feedbackStatusStr.contains("\"status\": true") &&
                    feedbackStatusStr.contains("\"message\": \"Feedback saved successfully\""),
            "Feedback Form is not Submitted");

    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Feedback is not submitted successfully");

    // Extracted Obligation List API after rejection of extracted obligation
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
    extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    extractedObligationResponse = new JSONObject(extractedObligationResponseStr);

    List<String> extractedDataStatusAfterRejection = new LinkedList<>();
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        extractedDataStatusAfterRejection.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractionStatusColumnId)).get("value").toString());
    }

    softAssert.assertTrue(extractedDataStatusAfterRejection.contains("Extracted data Rejected"),
            "Rejection of Auto Extraction is not working as expected");

    softAssert.assertAll();
}

//@Test(dataProvider = "verifySystemFieldsDataProvider")
public void TestNoAutoExtractionPermission(File fileToUpload) throws IOException, SQLException {
    //Hit Document Show API
    softAssert = new SoftAssert();
    Check check = new Check();
    check.hitCheck(endUserIdWithNoAutoExtractionPermission,endUserPasswordWithNoAutoExtractionPermission);

    HttpResponse contractShowResponse = hitContractShowAPI(contractIdWithNoAutoExtraction);
    String contractShowStrResponse = EntityUtils.toString(contractShowResponse.getEntity());
    softAssert.assertTrue(APIUtils.validJsonResponse(contractShowStrResponse),"Contract Show Response is not valid JSON");

    //Edit Document with new Uploaded Document
    Map<String,String> formData = new HashMap<>();
    String key = RandomStringUtils.randomAlphabetic(18);
    formData.put("key", key);
    formData.put("name", fileToUpload.getName().split("\\.")[0]);
    formData.put("extension", fileToUpload.getName().split("\\.")[1]);
    formData.put("relationId", relationIdWithNoAutoExtraction);
    String uploadDocAPIResponse = hitContractDocumentUpload(docFilePath,fileToUpload.getName(),formData);

    // Create Payload to edit
    String payload =createPayloadToEditDocForNoAutoExtractPermission(uploadDocAPIResponse,contractShowStrResponse,key,fileToUpload);

    // Execute Edit Contract API
    HttpResponse editContractWithDocResponse = hitContractEditAPI(payload);

    softAssert.assertTrue(editContractWithDocResponse.getStatusLine().getStatusCode()==200,
            "Document is not edited successfully");

    //Get newly Updated Doc Id from Database
    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("172.31.25.58","5432","vf_sdbox_20171221","yogesh_sharma","W0Q==");
    String query = "select * from contract_document where contract_id =" + Integer.valueOf(contractIdWithNoAutoExtraction)  +" and name  = " + "'"+ fileToUpload.getName().split("\\.")[0] + "'" +" order by id desc LIMIT 1";
    List<List<String>> docIds = postgreSQLJDBC.doSelect(query);
    newDocId = docIds.stream().findFirst().get().get(0);

    // Document Show API
    HttpResponse response = hitContractDocumentShowAPI(newDocId);
    String contractDocumentShowResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject contractDocumentShowResponse = new JSONObject(contractDocumentShowResponseStr);
    int tabsLength = contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").length();

    List<String> allTabsData = new LinkedList<>();
    for(int i=0;i<tabsLength;i++){
        allTabsData.add(contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(i).get("label").toString());
    }

    softAssert.assertTrue(!allTabsData.contains("Extracted Obligations"),"Extracted Obligation tab is visible even the permission of auto extraction is not given to this user");
    softAssert.assertAll();
}



    @Test
    public void verifyOnHitEditPageIsDisplayed() throws IOException {
    softAssert = new SoftAssert();
    HttpResponse response = hitEditButton();
    String editPageResponseStr = EntityUtils.toString(response.getEntity());

    softAssert.assertTrue(editPageResponseStr.contains("\"status\":\"success\""),
            "Edit Page is not displayed");
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Edit Button is not opening the edit page");
    softAssert.assertAll();
    }


//@Test(dataProvider = "verifyAcceptActionFile",priority = 3)
public void performAcceptActionForExtractedAutoExtraction(File fileToUpload) throws IOException, SQLException, ParseException {
    // New Auto Extraction
    softAssert = new SoftAssert();
    HttpResponse contractShowResponse = hitContractShowAPI(contractId);
    String contractShowStrResponse = EntityUtils.toString(contractShowResponse.getEntity());
    softAssert.assertTrue(APIUtils.validJsonResponse(contractShowStrResponse),"Contract Show Response is not valid JSON");

    //Edit Document with new Uploaded Document
    Map<String,String> formData = new HashMap<>();
    String key = RandomStringUtils.randomAlphabetic(18);
    formData.put("key", key);
    formData.put("name", fileToUpload.getName().split("\\.")[0]);
    formData.put("extension", fileToUpload.getName().split("\\.")[1]);
    formData.put("relationId", relationId);
    String uploadDocAPIResponse = hitContractDocumentUpload(docFilePath,fileToUpload.getName(),formData);

    // Create Payload to edit
    String payload =createPayloadToEditDoc(uploadDocAPIResponse,contractShowStrResponse,key,fileToUpload);

    // Execute Edit Contract API
    HttpResponse editContractWithDocResponse = hitContractEditAPI(payload);

    softAssert.assertTrue(editContractWithDocResponse.getStatusLine().getStatusCode()==200,
            "Document is not edited successfully");

    //Get newly Updated Doc Id from Database
    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("172.31.25.58","5432","vf_sdbox_20171221","yogesh_sharma","W0Q==");
    String query = "select * from contract_document where contract_id =" + Integer.valueOf(contractId)  +" and name  = " + "'"+ fileToUpload.getName().split("\\.")[0] + "'" +" order by id desc LIMIT 1";
    List<List<String>> docIds = postgreSQLJDBC.doSelect(query);
    newDocId = docIds.stream().findFirst().get().get(0);

    query = "select * from contract_document_autoextraction_request where document_id = " + Integer.valueOf(newDocId) + " order by id desc";
    List<List<String>> contract_autoextraction = postgreSQLJDBC.doSelect(query);
    String status = contract_autoextraction.stream().findFirst().get().get(4);

    while(Integer.valueOf(status)!= 4){
        contract_autoextraction = postgreSQLJDBC.doSelect(query);
        status = contract_autoextraction.stream().findFirst().get().get(4);

        if(Integer.valueOf(status) == 0){
            logger.info("Auto Extraction INITIATION_PENDING");
        }
        else if(Integer.valueOf(status) == 1){
            logger.info("Auto Extraction INITIATION_TRIGGERED");
        }
        else if(Integer.valueOf(status) == 2){
            logger.info("Auto Extraction INITIATION_SUCCESS");
        }
        else if(Integer.valueOf(status) == 3){
            logger.error("Auto Extraction INITIATION_FAILED");
            throw new SkipException("INITIATION_FAILED states services are not up to initiate auto extraction");
        }
        else if(Integer.valueOf(status) == 5){
            logger.error("AUTO_EXTRACTION_FAILURE");
            throw new SkipException("AUTO_EXTRACTION_FAILURE document is either password protected or corrupt");
        }
    }

    softAssert.assertTrue(Integer.valueOf(status)==4,"AUTO_EXTRACTION_SUCCESS");

    // Document Show API
    HttpResponse response = hitContractDocumentShowAPI(newDocId);
    String contractDocumentShowResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject contractDocumentShowResponse = new JSONObject(contractDocumentShowResponseStr);
    String extractedObligationDataUrl = contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(1).getJSONArray("fields").getJSONObject(0).getJSONArray("fields").getJSONObject(0).get("dataURL").toString();

    String obligationDataPayload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";

    List<String> extractedIds = new LinkedList<>();

    // Extracted Obligation List API
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,obligationDataPayload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Extracted Obligation Data is not reflected");

    String extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject extractedObligationResponse = new JSONObject(extractedObligationResponseStr);
    int extractedObligationColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"stagingClientPrimaryKey");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        extractedIds.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractedObligationColumnId)).get("value").toString());
    }

    List<HashMap<String,String>> allListingData = new LinkedList<>();
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        JSONObject rowJsonData = new JSONObject(extractedObligationResponse.getJSONArray("data").getJSONObject(i).toString());
        Set<String> keys = rowJsonData.keySet();
        HashMap<String,String> rowData = new LinkedHashMap<>();
        for(String s :keys){
            rowData.put(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(s).get("columnName").toString(),
                    extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(s).get("value").toString());
        }
        allListingData.add(rowData);
    }

    int listExtractedId = RandomUtils.nextInt(0,extractedIds.size());
    // Show Extracted Obligation Create Page(Dno Page)
    String dnoCreationId = extractedIds.get(listExtractedId);
    response = hitShowActionPage(dnoCreationId.split(":;")[1]);
    String actionShowPageResponseStr = EntityUtils.toString(response.getEntity());
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Not Able to go to show page of action");
    softAssert.assertTrue(JSONUtility.validjson(actionShowPageResponseStr),"Not a valid JSON");
    softAssert.assertTrue(actionShowPageResponseStr.contains("\"status\":\"success\""),
            "Status is not success");

    JSONObject actionShowPageJson = new JSONObject(actionShowPageResponseStr);

    boolean createButtonPresence = false;
    boolean cancelButtonPresence = false;
    int buttonsOnDnoCreatePage = actionShowPageJson.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").length();
    for(int i=0;i<buttonsOnDnoCreatePage;i++){
        if(actionShowPageJson.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(i).get("name").toString().contains("Create")){
            createButtonPresence = true;
        }
        else if(actionShowPageJson.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(i).get("name").toString().contains("Cancel")){
            cancelButtonPresence = true;
        }
    }

    softAssert.assertTrue(createButtonPresence,"Newly Extraction Obligation does not have create button");
    softAssert.assertTrue(cancelButtonPresence,"Newly Extraction Obligation does not have cancel button");

    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("phase").put("values",new JSONObject("{\"name\": \"Contract Term\",\"id\": 1007}"));
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_2095").getJSONArray("values").put(new JSONObject("{\"name\": \"Integration User\",\"id\": 3546,\"type\": 2,\"email\": \"neha.sharma+1@sirionlabs.com\",\"properties\": {}}"));
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("pageReference").getJSONArray("values").getJSONObject(0).put("clause",1.1);
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("weekType").put("values",new JSONObject("{\"name\": \"Five Day\",\"id\": 1}"));
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("startDate").put("values",DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),2,"MM-dd-yyyy HH:mm:ss"));
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("expDate").put("values",DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),10,"MM-dd-yyyy HH:mm:ss"));
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").put("values",DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),5,"MM-dd-yyyy HH:mm:ss"));
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("patternDate").put("values",DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"),4,"MM-dd-yyyy HH:mm:ss"));
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("frequency").put("values",new JSONObject("{\"name\": \"Annual (Date)\",\"id\": 1001,\"parentId\": 1008}"));
    actionShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("frequencyType").put("values",new JSONObject("{\"name\": \"Yearly\",\"id\": 1008}"));

    //Create Dnos
    String createDnosPayload = "{\"body\": {\"data\":" + actionShowPageJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
    response = hitCreateDnos(createDnosPayload);

    JSONObject createDnosJsonObjectResponse = new JSONObject(EntityUtils.toString(response.getEntity()));
    int createdDnosId = CreateEntity.getNewEntityId(createDnosJsonObjectResponse.toString());
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Dno is not created successfully");

    //Submit Feedback After Approval
    String feedbackPayload = "{\"feedback\":[{\"questionId\":1,\"selectedOption\":5},{\"questionId\":2,\"selectedOption\":[9]}],\"dataId\":"+ dnoCreationId.split(":;")[0].replace("EXOB","").trim() +"}";
    HttpResponse feedbackFormResponse = hitFeedbackAPI(feedbackPayload);
    softAssert.assertTrue(feedbackFormResponse.getStatusLine().getStatusCode()==200,"Approve feedback is not submitted successfully");

    // Update Approve Status
    HttpResponse approveActionResponse = hitApproveStatusExtractionAPI(dnoCreationId.split(":;")[1]);
    softAssert.assertTrue(approveActionResponse.getStatusLine().getStatusCode() ==200,"Approve Status is not updated");
    softAssert.assertTrue(EntityUtils.toString(approveActionResponse.getEntity()).equals("{\"message\":\"Updated Successfully\",\"status\":200}"),
            "Response is not as Excepted");

    //Open Approved Dnos Page
    HttpResponse createdDnoShowResponse = hitShowCreatedDno(createdDnosId);
    softAssert.assertTrue(createdDnoShowResponse.getStatusLine().getStatusCode() == 200,
            "Dno is created successfully");
    String createdDnoShowResponseStr = EntityUtils.toString(createdDnoShowResponse.getEntity());
    softAssert.assertTrue(JSONUtility.validjson(createdDnoShowResponseStr),"Response is not valid JSON");
    JSONObject dnoShowPageJsonStr = new JSONObject(createdDnoShowResponseStr);

    // To Do
    softAssert.assertTrue(dnoShowPageJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("id").get("values").toString().equals(String.valueOf(createdDnosId)),"Created Dno Id in listing does not match with id in show page");
    softAssert.assertTrue(dnoShowPageJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("priority").getJSONObject("values").get("name").toString().trim().equals(allListingData.get(listExtractedId).get("priority").trim()),"Priority in listing does not match with Priority in show page");
    softAssert.assertTrue(dnoShowPageJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString().trim().equals(allListingData.get(listExtractedId).get("name").trim()),"Name in listing does not match with Name in show page");
    softAssert.assertTrue(dnoShowPageJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("subcategory").getJSONObject("values").get("name").toString().trim().equals(allListingData.get(listExtractedId).get("subcategory").trim()),"Sub-Category in listing does not match with Sub-Category in show page");
    softAssert.assertTrue(dnoShowPageJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("category").getJSONObject("values").get("name").toString().trim().equals(allListingData.get(listExtractedId).get("category").trim()),"Category in listing does not match with Category in show page");
    softAssert.assertTrue(dnoShowPageJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("outputType").getJSONObject("values").get("name").toString().trim().equals(allListingData.get(listExtractedId).get("outputType").trim()),"Output Type in listing does not match with Output Type in show page");
    softAssert.assertTrue(dnoShowPageJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("responsibility").getJSONObject("values").get("name").toString().trim().equals(allListingData.get(listExtractedId).get("responsibility").trim()),"Responsibility in listing does not match with Responsibility in show page");

    //Setting Approved Extracted Obligation Id to global Variable
    alreadyApprovedDnoId = createdDnosId;

    // Extracted Obligation List API After Creating Dnos
    obligationDataPayload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";

    List<String> extractedIdsAfterDnosCreation = new LinkedList<>();

    // Extracted Obligation List API
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,obligationDataPayload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Extracted Obligation Data is not reflected");

    extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    extractedObligationResponse = new JSONObject(extractedObligationResponseStr);
    extractedObligationColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"stagingClientPrimaryKey");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        extractedIdsAfterDnosCreation.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractedObligationColumnId)).get("value").toString());
    }

    softAssert.assertTrue(extractedIdsAfterDnosCreation.size() + 1 == extractedIds.size(),"Dno is not created successfully");
    softAssert.assertTrue(!extractedIdsAfterDnosCreation.contains(dnoCreationId),"Dno is not created successfully");

    // Resubmit Created Dno without editing anything
    String dnoEditPayload = "{\"body\": {\"data\":" + dnoShowPageJsonStr.getJSONObject("body").getJSONObject("data").toString() + "}}";

    response = updateCreatedDnoAPI(dnoEditPayload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() ==200,"Response is not as per expected");
    String updatedDnoResponseStr = EntityUtils.toString(response.getEntity());
    softAssert.assertTrue(updatedDnoResponseStr.contains("\"status\":\"success\""),
            "Dno is not updated Successfully");

    JSONObject updatedDnoResponseJsonStr = new JSONObject(updatedDnoResponseStr);
    int length = updatedDnoResponseJsonStr.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString().split("/").length;
    int updatedCreatedDnosId = Integer.valueOf(updatedDnoResponseJsonStr.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("redirectUrl").toString().split("/")[length-1]);
    softAssert.assertTrue(updatedCreatedDnosId == createdDnosId,"Dno Id is getting changed as nothing is edited just resubmitted the dno which is not expected");
    softAssert.assertAll();
}

//@Test(priority = 4)
public void verifyApprovedObligationRemainsUnchangedOnUpdatingContractShowPage() throws IOException {
// New Auto Extraction
    softAssert = new SoftAssert();
    HttpResponse contractShowResponse = hitContractShowAPI(contractId);
    String contractShowStrResponse = EntityUtils.toString(contractShowResponse.getEntity());
    softAssert.assertTrue(APIUtils.validJsonResponse(contractShowStrResponse),"Contract Show Response is not valid JSON");

    // Create Payload to edit
    String payload =createPayloadToUpdateWithoutUploadingDoc(contractShowStrResponse);

    // Execute Edit Contract API
    HttpResponse editContractWithDocResponse = hitContractEditAPI(payload);

    softAssert.assertTrue(editContractWithDocResponse.getStatusLine().getStatusCode()==200,
            "Document is not edited successfully");

    HttpResponse createdDnoShowResponse = hitShowCreatedDno(alreadyApprovedDnoId);
    String createdDnoShowStrResponse = EntityUtils.toString(createdDnoShowResponse.getEntity());
    softAssert.assertTrue(JSONUtility.validjson(createdDnoShowStrResponse),"JSON is not valid");
    softAssert.assertTrue(createdDnoShowResponse.getStatusLine().getStatusCode() == 200,
            "On updating contract show page already approved obligation is getting altered which is not expected");

    softAssert.assertAll();
}

//@Test
public void verifyAutoExtractedFieldMappingFieldClientAdmin() throws IOException {
    softAssert = new SoftAssert();
    Check check = new Check();
    check.hitCheck(clientAdminUserId,clientAdminPassword);
    HttpResponse response = hitClientAdmin();
    String strResponse = EntityUtils.toString(response.getEntity());

    Document html = Jsoup.parse(strResponse);
    Elements elements =html.getElementsByClass("menu-box admin");

    boolean result = false;
    for(Element e :elements){
    if(e.select("h3").text().equals("Organization Setup")){
        result = true;
        Element link = e.selectFirst("a[href='optionMapping/show']");
        softAssert.assertTrue(link.text().equals("Auto-Extracted Fields Mapping"),
                "Auto Extraction Field mapping in not present under organisation setup");
        break;
    }
   }
    softAssert.assertTrue(result,"Inside Organization Setup Auto Extraction Field mapping is not there error!");
    softAssert.assertAll();
}

//@Test
public void verifyOnClickAutoExtractionFieldMappingPop() throws IOException {
    softAssert = new SoftAssert();
    Check check = new Check();
    check.hitCheck(clientAdminUserId,clientAdminPassword);
    HttpResponse response = hitFieldMappingAPI();
    String strResponse = EntityUtils.toString(response.getEntity());

    Document html = Jsoup.parse(strResponse);
    Elements elements = html.select("div#tabs");

    List<String> tabs = new LinkedList<>();
    for(Element e: elements){
        Elements tagsText = e.getElementsByTag("a");
        for(Element e1 :tagsText){
            tabs.add(e1.text());
        }
    }

    List<String> selectEntityDropdownOptionsCategory = new LinkedList<>();
    Element selectEntityDropdown = html.selectFirst("#_entityTypeId_id");
    Elements selectEntityDropdownOptions = selectEntityDropdown.getElementsByTag("option");

    for(Element e :selectEntityDropdownOptions){
        selectEntityDropdownOptionsCategory.add(e.text());
    }

    softAssert.assertTrue(selectEntityDropdownOptionsCategory.contains("Obligations"),
            "Select Entity Option is not present");

    softAssert.assertTrue(tabs.contains("Auto extraction fields mapping") &&
            tabs.contains("Audit Log"), "Pop up is not visible on clicking auto extraction field mapping");

    softAssert.assertAll();
}

@Test(dataProvider = "verifySystemFieldsDataProvider")
public void verifySystemFieldsFromAdminToEndUser(File fileToUpload) throws IOException, SQLException {
    softAssert = new SoftAssert();
    Check check = new Check();
    check.hitCheck(clientAdminUserId,clientAdminPassword);
    HttpResponse response = getSystemFieldsForObligation();
    String systemFieldsResponseStr = EntityUtils.toString(response.getEntity());

    JSONObject jsonObject = new JSONObject(systemFieldsResponseStr);
    int fieldsLength = jsonObject.getJSONArray("optionMappings").length();

    HashMap<String,List<String>> fieldsWithAllOptions = new LinkedHashMap<>();

    for(int i=0;i<fieldsLength;i++){
        String key = jsonObject.getJSONArray("optionMappings").getJSONObject(i).get("aeFieldName").toString();
        if(!fieldsWithAllOptions.containsKey(key)) {
            int optionsLength = jsonObject.getJSONArray("optionMappings").getJSONObject(i).getJSONObject("sirionFieldOptionData").getJSONArray("data").length();
            List<String> options = new LinkedList<>();
            for (int j = 0; j < optionsLength; j++) {
                options.add(jsonObject.getJSONArray("optionMappings").getJSONObject(i).getJSONObject("sirionFieldOptionData").getJSONArray("data").getJSONObject(j).get("name").toString());
            }
            fieldsWithAllOptions.put(key, options);
        }
    }
    check.hitCheck(endUserId,endUserPassword);

    //Hit Document Show API
    HttpResponse contractShowResponse = hitContractShowAPI(contractId);
    String contractShowStrResponse = EntityUtils.toString(contractShowResponse.getEntity());
    softAssert.assertTrue(APIUtils.validJsonResponse(contractShowStrResponse),"Contract Show Response is not valid JSON");

    //Edit Document with new Uploaded Document
    Map<String,String> formData = new HashMap<>();
    String key = RandomStringUtils.randomAlphabetic(18);
    formData.put("key", key);
    formData.put("name", fileToUpload.getName().split("\\.")[0]);
    formData.put("extension", fileToUpload.getName().split("\\.")[1]);
    formData.put("relationId", relationId);
    String uploadDocAPIResponse = hitContractDocumentUpload(docFilePath,fileToUpload.getName(),formData);

    // Create Payload to edit
    String payload =createPayloadToEditDoc(uploadDocAPIResponse,contractShowStrResponse,key,fileToUpload);

    // Execute Edit Contract API
    HttpResponse editContractWithDocResponse = hitContractEditAPI(payload);

    softAssert.assertTrue(editContractWithDocResponse.getStatusLine().getStatusCode()==200,
            "Document is not edited successfully");

    //Get newly Updated Doc Id from Database
    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("172.31.25.58","5432","vf_sdbox_20171221","yogesh_sharma","W0Q==");
    String query = "select * from contract_document where contract_id =" + Integer.valueOf(contractId)  +" and name  = " + "'"+ fileToUpload.getName().split("\\.")[0] + "'" +" order by id desc LIMIT 1";
    List<List<String>> docIds = postgreSQLJDBC.doSelect(query);
    newDocId = docIds.stream().findFirst().get().get(0);

    query = "select * from contract_document_autoextraction_request where document_id = " + Integer.valueOf(newDocId) + " order by id desc";
    List<List<String>> contract_autoextraction = postgreSQLJDBC.doSelect(query);
    String status = contract_autoextraction.stream().findFirst().get().get(4);

    while(Integer.valueOf(status)!= 4){
        contract_autoextraction = postgreSQLJDBC.doSelect(query);
        status = contract_autoextraction.stream().findFirst().get().get(4);

        if(Integer.valueOf(status) == 0){
            logger.info("Auto Extraction INITIATION_PENDING");
        }
        else if(Integer.valueOf(status) == 1){
            logger.info("Auto Extraction INITIATION_TRIGGERED");
        }
        else if(Integer.valueOf(status) == 2){
            logger.info("Auto Extraction INITIATION_SUCCESS");
        }
        else if(Integer.valueOf(status) == 3){
            logger.error("Auto Extraction INITIATION_FAILED");
            throw new SkipException("INITIATION_FAILED states services are not up to initiate auto extraction");
        }
        else if(Integer.valueOf(status) == 5){
            logger.error("AUTO_EXTRACTION_FAILURE");
            throw new SkipException("AUTO_EXTRACTION_FAILURE document is either password protected or corrupt");
        }
    }

    softAssert.assertTrue(Integer.valueOf(status)==4,"AUTO_EXTRACTION_SUCCESS");

    // Document Show API
    response = hitContractDocumentShowAPI(newDocId);
    String contractDocumentShowResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject contractDocumentShowResponse = new JSONObject(contractDocumentShowResponseStr);
    String extractedObligationDataUrl = contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(1).getJSONArray("fields").getJSONObject(0).getJSONArray("fields").getJSONObject(0).get("dataURL").toString();

    payload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";


    // Extracted Obligation List API
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Extracted Obligation Data is not reflected");

    String extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    JSONObject extractedObligationResponse = new JSONObject(extractedObligationResponseStr);

    List<String> performanceTypes  = new LinkedList<>();
    List<String> categories  = new LinkedList<>();
    List<String> priorityTypes  = new LinkedList<>();
    List<String> frequenices  = new LinkedList<>();
    List<String> responsibilityTypes  = new LinkedList<>();
    List<String> subCategories  = new LinkedList<>();

    int performanceTypeColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"outputType");
    int categoryTypeColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"category");
    int priorityColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"priority");
    int frequencyTypeColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"frequency");
    int responsibilitiesColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"responsibility");
    int subcategoriesColumnId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"subcategory");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        performanceTypes.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(performanceTypeColumnId)).get("value").toString());
        categories.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(categoryTypeColumnId)).get("value").toString());
        priorityTypes.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(priorityColumnId)).get("value").toString());
        frequenices.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(frequencyTypeColumnId)).get("value").toString());
        responsibilityTypes.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(responsibilitiesColumnId)).get("value").toString());
        subCategories.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(subcategoriesColumnId)).get("value").toString());
    }

    softAssert.assertTrue(fieldsWithAllOptions.get("Performance Type").containsAll(performanceTypes),"All Performance type from Client is being reflected in end-user");
    softAssert.assertTrue(fieldsWithAllOptions.get("Category").containsAll(categories),"All Category type from Client is being reflected in end-user");
    softAssert.assertTrue(fieldsWithAllOptions.get("Priority").containsAll(priorityTypes),"All Priority type from Client is being reflected in end-user");
    softAssert.assertTrue(fieldsWithAllOptions.get("Frequency").containsAll(frequenices.stream().filter(m->!m.contains("-")).collect(Collectors.toList())),"All Frequency type from Client is being reflected in end-user");
    softAssert.assertTrue(fieldsWithAllOptions.get("Responsibility").containsAll(responsibilityTypes),"All Responsibility type from Client is being reflected in end-user");
    softAssert.assertTrue(fieldsWithAllOptions.get("Sub-Category").containsAll(subCategories),"All Sub-Category type from Client is being reflected in end-user");

    //Sorting by id Column
    // verify sorting on extracted obligation tab listing
    payload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";

    // Extracted Obligation List API sorting by asc id
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Extracted Obligation Data is not reflected");

    extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    extractedObligationResponse = new JSONObject(extractedObligationResponseStr);

    List<String> extractedObligationIds = new LinkedList<>();
    int extractedObligationId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"id");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        extractedObligationIds.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractedObligationId)).get("value").toString());
    }

    List<String> extractedObligationIdsAfterSorting = extractedObligationIds.stream().sorted().collect(Collectors.toList());
    softAssert.assertTrue(extractedObligationIds.equals(extractedObligationIdsAfterSorting),"Extracted Data is not sorted as per id in asc order");

    // verify sorting on extracted obligation tab listing
    payload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";

    // Extracted Obligation List API sorting by desc id
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Extracted Obligation Data is not reflected");

    extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    extractedObligationResponse = new JSONObject(extractedObligationResponseStr);

    extractedObligationIds = new LinkedList<>();
    extractedObligationId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"id");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        extractedObligationIds.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractedObligationId)).get("value").toString());
    }

    extractedObligationIdsAfterSorting = extractedObligationIds.stream().sorted(Collections.reverseOrder()).collect(Collectors.toList());
    softAssert.assertTrue(extractedObligationIds.equals(extractedObligationIdsAfterSorting),"Extracted Data is not sorted as per id in asc order");

    //Sorting by Name Column
    // verify sorting on extracted obligation tab listing
    payload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"name\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";

    // Extracted Obligation List API sorting by asc name
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Extracted Obligation Data is not reflected");

    extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    extractedObligationResponse = new JSONObject(extractedObligationResponseStr);

    List<String> extractedObligationNames = new LinkedList<>();
    int extractedObligationNameId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"name");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        extractedObligationNames.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractedObligationNameId)).get("value").toString());
    }

    List<String> extractedObligationNamesAfterSorting = extractedObligationNames.stream().sorted().collect(Collectors.toList());
    softAssert.assertTrue(extractedObligationNames.equals(extractedObligationNamesAfterSorting),"Extracted Data is not sorted as per name in asc order");

    // verify sorting on extracted obligation tab listing
    payload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"name\",\"orderDirection\":\"desc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";

    // Extracted Obligation List API sorting by desc name
    response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
            "Extracted Obligation Data is not reflected");

    extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
    extractedObligationResponse = new JSONObject(extractedObligationResponseStr);

    extractedObligationNames = new LinkedList<>();
    extractedObligationNameId = ListDataHelper.getColumnIdFromColumnName(extractedObligationResponseStr,"name");
    for(int  i =0; i <extractedObligationResponse.getJSONArray("data").length();i++){
        extractedObligationNames.add(extractedObligationResponse.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(extractedObligationNameId)).get("value").toString());
    }

    extractedObligationNamesAfterSorting = extractedObligationNames.stream().sorted(Collections.reverseOrder()).collect(Collectors.toList());
    softAssert.assertTrue(extractedObligationNames.equals(extractedObligationNamesAfterSorting),"Extracted Data is not sorted as per name in asc order");
    softAssert.assertAll();
}

//@Test
public void testUpdateSystemFieldFromClientAdmin() throws IOException {
    softAssert = new SoftAssert();
    Check check = new Check();
    check.hitCheck(clientAdminUserId,clientAdminPassword);
    HttpResponse response = getSystemFieldsForObligation();
    String systemFieldsResponseStr = EntityUtils.toString(response.getEntity());

    response = hitUpdateButton(systemFieldsResponseStr);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 302,"System Field Properties are not getting updated");
    softAssert.assertAll();
}

@Test
public void verifyAutoExtractionPermissionOnClientAdmin() throws IOException {
    softAssert = new SoftAssert();
    Check check = new Check();
    check.hitCheck(clientAdminUserId,clientAdminPassword);
    HttpResponse response = hitShowUserApiClientAdmin("3546");

    softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Response is not valid");
    String strResponse = EntityUtils.toString(response.getEntity());

    Document html = Jsoup.parse(strResponse);
    String roleGroupUrl = html.selectFirst("#__userRoleGroups_id").childNodes().stream().filter(m->m.hasAttr("href")).findFirst().get().attributes().get("href");
    String roleGroupName = html.selectFirst("#__userRoleGroups_id").children().text();

    logger.info("For user role group " + roleGroupName + " Redirecting Url is " + roleGroupUrl);

    response = hitShowUserRoleGroupPermissions(roleGroupUrl);

    softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Response is not valid");
    strResponse = EntityUtils.toString(response.getEntity());
    html = Jsoup.parse(strResponse);

    List<String> allPermissions = new LinkedList<>();
    String[] allLinks;
    Elements elements = html.select("#_s_com_sirionlabs_model_MasterUserRoleGroup_roles_userRolesString_id");
    for(Element element :elements){
        allLinks = element.getElementsByClass("scrollableItem").text().split(",");
        allPermissions.addAll(Arrays.stream(allLinks).map(m->m.trim()).collect(Collectors.toList()));
    }

    allPermissions =allPermissions.stream().filter(m->m.contains("(Global Permission)")).collect(Collectors.toList());

    softAssert.assertTrue(allPermissions.contains("Autoextract Entities (Global Permission)"),"Field to provide Auto-Extraction is not present in user role group page");
    softAssert.assertAll();
}

//@Test
public void renameExtractedObligationFromClientAdmin() throws IOException {
    softAssert = new SoftAssert();
    Check check = new Check();
    check.hitCheck(clientAdminUserId,clientAdminPassword);

    HttpResponse response = getDocumentViewerTabsClientAdmin("1579");
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not as per expected");

    String updatedExtractedObligationClientFieldLabel = "Test Extraction";
    boolean extractionObligationClientNamePresence =false;
    String extractionObligationTabName = null;
    JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
    int length = jsonObject.getJSONArray("childGroups").length();
    for(int i=0;i<length;i++){
        if(jsonObject.getJSONArray("childGroups").getJSONObject(i).get("name").toString().trim().contains("Document Viewer Tabs")){
            int documentViewerAllTabsLength =jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").length();
            for(int j=0;j<documentViewerAllTabsLength;j++){
                if(jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).get("name").toString().trim().equals("EXTRACTED OBLIGATIONS")){
                    extractionObligationTabName = jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).get("clientFieldName").toString();
                    extractionObligationClientNamePresence = true;
                    jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).put("clientFieldName",updatedExtractedObligationClientFieldLabel);
                    break;
                }
            }
        }
    }

    softAssert.assertTrue(extractionObligationTabName.equals("Extracted Obligations"),"Extracted Obligation Field is not present in Client Admin");
    softAssert.assertTrue(extractionObligationClientNamePresence,"Extracted Obligation Field is not present in Client Admin");

    String payload = jsonObject.toString();
    response = updateFieldLabelsAPI(payload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not Expected");
    softAssert.assertTrue(EntityUtils.toString(response.getEntity()).equals("{\"isSuccess\":true,\"errorMessages\":[]}"),"Response Code is not as per Expected");

    // Login with End-User
    check.hitCheck(endUserId,endUserPassword);
    String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("obligationAutoExtractionConfigFilePath");
    String configFileName = ConfigureConstantFields.getConstantFieldsProperty("obligationAutoExtractionConfigFileName");
    String docId =ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"previousextracteddocid");
    response = hitContractDocumentShowAPI(docId);

    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not as expected");

    jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
    int tabsLength = jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").length();
    boolean labelChangeFlag = false;
    for(int i=0;i<tabsLength;i++){
        if(jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(i).get("label").toString().trim().contains(updatedExtractedObligationClientFieldLabel)){
           labelChangeFlag = true;
        }
    }

    softAssert.assertTrue(labelChangeFlag,"Flag is not changed successfully");

    // Reverting label setting it to default
    check.hitCheck(clientAdminUserId,clientAdminPassword);
    response = getDocumentViewerTabsClientAdmin("1579");
    jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

   length = jsonObject.getJSONArray("childGroups").length();
    for(int i=0;i<length;i++){
        if(jsonObject.getJSONArray("childGroups").getJSONObject(i).get("name").toString().trim().contains("Document Viewer Tabs")){
            int documentViewerAllTabsLength =jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").length();
            for(int j=0;j<documentViewerAllTabsLength;j++){
                if(jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).get("name").toString().trim().equals("EXTRACTED OBLIGATIONS")){
                    jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).put("clientFieldName",extractionObligationTabName);
                    break;
                }
            }
        }
    }
    payload = jsonObject.toString();
    response = updateFieldLabelsAPI(payload);
    softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not Expected");
    softAssert.assertTrue(EntityUtils.toString(response.getEntity()).equals("{\"isSuccess\":true,\"errorMessages\":[]}"),"Response Code is not as per Expected");

    softAssert.assertAll();
}

   //@Test(dataProvider = "dataForUnCheckAutoExtraction")
    public void TestObligationWithUnCheckAutoExtraction(File fileToUpload) throws IOException, SQLException {
        //Hit Document Show API
        softAssert = new SoftAssert();
        HttpResponse contractShowResponse = hitContractShowAPI(contractId);
        String contractShowStrResponse = EntityUtils.toString(contractShowResponse.getEntity());
        softAssert.assertTrue(APIUtils.validJsonResponse(contractShowStrResponse),"Contract Show Response is not valid JSON");

        //Edit Document with new Uploaded Document
        Map<String,String> formData = new HashMap<>();
        String key = RandomStringUtils.randomAlphabetic(18);
        formData.put("key", key);
        formData.put("name", fileToUpload.getName().split("\\.")[0]);
        formData.put("extension", fileToUpload.getName().split("\\.")[1]);
        formData.put("relationId", relationId);
        String uploadDocAPIResponse = hitContractDocumentUpload(docFilePath,fileToUpload.getName(),formData);

        // Create Payload to edit
        String payload = createPayloadToEditDocWithUncheckAutoExtraction(uploadDocAPIResponse,contractShowStrResponse,key,fileToUpload);

        // Execute Edit Contract API
        HttpResponse editContractWithDocResponse = hitContractEditAPI(payload);

        softAssert.assertTrue(editContractWithDocResponse.getStatusLine().getStatusCode()==200,
                "Document is not edited successfully");

        //Get newly Updated Doc Id from Database
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC("172.31.25.58","5432","vf_sdbox_20171221","yogesh_sharma","W0Q==");
        String query = "select * from contract_document where contract_id =" + Integer.valueOf(contractId)  +" and name  = " + "'"+ fileToUpload.getName().split("\\.")[0] + "'" +" order by id desc LIMIT 1";
        List<List<String>> docIds = postgreSQLJDBC.doSelect(query);
        newDocId = docIds.stream().findFirst().get().get(0);

        query = "select * from contract_document_autoextraction_request where document_id = " + Integer.valueOf(newDocId) + " order by id desc";
        List<List<String>> contract_autoextraction = postgreSQLJDBC.doSelect(query);
        softAssert.assertTrue(contract_autoextraction.size() == 0,"Auto Extraction Checkbox is not checked thus size is not zero there should not be any data");

        // Document Show API
        HttpResponse response = hitContractDocumentShowAPI(newDocId);
        String contractDocumentShowResponseStr = EntityUtils.toString(response.getEntity());
        JSONObject contractDocumentShowResponse = new JSONObject(contractDocumentShowResponseStr);
        String extractedObligationDataUrl = contractDocumentShowResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(1).getJSONArray("fields").getJSONObject(0).getJSONArray("fields").getJSONObject(0).get("dataURL").toString();

        payload = "{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterDataList\":[{\"filterId\":\"1\",\"filterName\":\"stagingStatus\",\"type\":\"MULTISELECT\",\"filterValue\":{\"type\":\"MULTISELECT\",\"value\":[{\"id\":\"210\"},{\"id\":\"212\"}]}},{\"filterId\":\"4\",\"filterName\":\"pageNumber\",\"filterValue\":{\"type\":\"TEXT\",\"value\":\"^1$\"}},{\"filterId\":\"5\",\"filterName\":\"references\",\"filterValue\":{\"type\":\"TEXT\",\"value\":"+ newDocId +"}}]}";
        // Extracted Obligation List API
        response = hitExtractedObligationDataApi(extractedObligationDataUrl,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
                "Extracted Obligation Data is not reflected");

        String extractedObligationResponseStr = EntityUtils.toString(response.getEntity());
        JSONObject extractedObligationResponse = new JSONObject(extractedObligationResponseStr);

        softAssert.assertTrue(Integer.valueOf(extractedObligationResponse.get("filteredCount").toString()) == 0,"Filtered Rows should be zero as auto-extraction is unchecked");
        softAssert.assertAll();
    }

    // To Do
    //@Test
    public void TestSameMetadataAllocatedToSystemFields() throws IOException {
        softAssert = new SoftAssert();
        Check check = new Check();
        check.hitCheck(clientAdminUserId,clientAdminPassword);
        HttpResponse response = getSystemFieldsForObligation();
        String systemFieldsResponseStr = EntityUtils.toString(response.getEntity());

        JSONObject jsonObject = new JSONObject(systemFieldsResponseStr);
        int fieldsLength = jsonObject.getJSONArray("optionMappings").length();

        HashMap<String,List<String>> fieldsWithAllOptions = new LinkedHashMap<>();

        for(int i=0;i<fieldsLength;i++){
            String key = jsonObject.getJSONArray("optionMappings").getJSONObject(i).get("aeFieldName").toString();
            if(!fieldsWithAllOptions.containsKey(key)) {
                int optionsLength = jsonObject.getJSONArray("optionMappings").getJSONObject(i).getJSONObject("sirionFieldOptionData").getJSONArray("data").length();
                List<String> options = new LinkedList<>();
                for (int j = 0; j < optionsLength; j++) {
                    options.add(jsonObject.getJSONArray("optionMappings").getJSONObject(i).getJSONObject("sirionFieldOptionData").getJSONArray("data").getJSONObject(j).get("name").toString());
                }
                fieldsWithAllOptions.put(key, options);
            }
        }

        softAssert.assertTrue(fieldsWithAllOptions.size() > 1,"Field are not captured");
        for(Map.Entry<String,List<String>> m : fieldsWithAllOptions.entrySet()){
            logger.info(m.getKey() + " has " + m.getValue() + " values to select from");
            softAssert.assertTrue(m.getValue().size() >1 ," same entity metadata field values can not be allocated to different system option fields as size is not greater than one which means we don't have permission to select from different entities");
        }
        softAssert.assertAll();
    }


    public HttpResponse updateFieldLabelsAPI(String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/fieldlabel/update";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse updateCreatedDnoAPI(String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/dnos/edit";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }


    public HttpResponse getDocumentViewerTabsClientAdmin(String fieldId){
        HttpResponse response = null;
        HttpGet getRequest;
        try{
            String queryString = "/fieldlabel/findLabelsByGroupIdAndLanguageId/1/" + fieldId;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");
            getRequest.addHeader("Content-Type","application/json; charset=utf-8");

            response = APIUtils.getRequest(getRequest,httpHost,false);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitShowUserRoleGroupPermissions(String url){
        HttpResponse response = null;
        HttpGet getRequest;
        try{
            String queryString = url;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

            response = APIUtils.getRequest(getRequest,httpHost,false);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

public HttpResponse hitShowUserApiClientAdmin(String userId){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/tblusers/show/" + userId;
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.getRequest(getRequest,httpHost,false);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse hitShowActionPage(String extractedObligationId){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/dnos/staging/" + extractedObligationId;
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");
        getRequest.addHeader("Content-Type", "application/json; charset=utf-8");

        response = APIUtils.getRequest(getRequest,httpHost,false);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse hitCreateDnos(String payload){
    HttpResponse response = null;
    HttpPost postRequest;
    try{
        String queryString = "/dnos/create";
        logger.debug("Query string url formed is {}", queryString);
        postRequest = new HttpPost(queryString);
        postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        postRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.postRequest(postRequest,payload);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse hitUpdateButton(String payload){
    HttpResponse response = null;
    HttpPost postRequest;
    try{
        String queryString = "/optionMapping/update";
        logger.debug("Query string url formed is {}", queryString);
        postRequest = new HttpPost(queryString);
        postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        postRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.postRequest(postRequest,payload);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse getSystemFieldsForObligation(){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/optionMapping/12";
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");
        getRequest.addHeader("Content-Type", "application/json; charset=utf-8");

        response = APIUtils.getRequest(getRequest,httpHost,false);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse hitClientAdmin(){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/admin";
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.getRequest(getRequest,httpHost,false);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

    public HttpResponse hitFieldMappingAPI(){
        HttpResponse response = null;
        HttpGet getRequest;
        try{
            String queryString = "/optionMapping/show";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

            response = APIUtils.getRequest(getRequest,httpHost,false);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

public HttpResponse hitShowCreatedDno(int dnoId){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/dnos/show/" + dnoId;
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.getRequest(getRequest,httpHost,false);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse hitEditButton(){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/contracts/edit/" + contractId;
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.getRequest(getRequest,httpHost,false);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse hitUpdateRejectStatusExtractionAPI(String rejectId){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/integration/statusUpdate/reject/12/" + rejectId;
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.getRequest(getRequest,httpHost,false);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

    public HttpResponse hitApproveStatusExtractionAPI(String rejectId){
        HttpResponse response = null;
        HttpGet getRequest;
        try{
            String queryString = "/integration/statusUpdate/approve/12/" + rejectId;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

            response = APIUtils.getRequest(getRequest,httpHost,false);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

public HttpResponse hitGetFeedbackFormAPI(){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/autoextraction/feedback/12";
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.getRequest(getRequest);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse hitFeedbackAPI(String payload){
    HttpResponse response = null;
    HttpPost postRequest;
    try{
        String queryString = "/autoextraction/feedback/12";
        logger.debug("Query string url formed is {}", queryString);
        postRequest = new HttpPost(queryString);
        postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        postRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.postRequest(postRequest,payload);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public HttpResponse hitDownloadExtractedObligationExcel(String downloadDirectory,Map<String,String> formParam,String fileName){
    HttpResponse response = null;
    try {

        HttpPost postRequest;
        APIUtils apiUtils = new APIUtils();
        String queryString = "/integrationlisting/download/1002/12";
        logger.debug("Query string url formed is {}", queryString);

        postRequest =apiUtils.generateHttpPostRequestWithQueryString(queryString,"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
               "application/x-www-form-urlencoded","gzip, deflate, br");
        HttpEntity params = apiUtils.generateNameValuePairFormDataEntity(formParam);
        postRequest.setEntity(params);

        response = apiUtils.downloadAPIResponseFile(downloadDirectory + fileName,httpHost,postRequest);

        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("DownloadListWithData response header {}", headers[i].toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting DownloadListWithData Api. {}", e.getMessage());
    }
    return response;
}

    public HttpResponse hitExtractedObligationDataApi(String query,String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

public HttpResponse hitContractDocumentShowAPI(String docId){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
        String queryString = "/documentviewer/show/" + docId;
        logger.debug("Query string url formed is {}", queryString);
        getRequest = new HttpGet(queryString);
        getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        response = APIUtils.getRequest(getRequest);
        logger.debug("Response status is {}", response.getStatusLine().toString());

        Header[] headers = response.getAllHeaders();
        for (Header oneHeader : headers) {
            logger.debug("Delete Delegation API header {}", oneHeader.toString());
        }
    } catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

public String createPayloadToEditDoc(String uploadDocAPIResponse,String contractShowStrResponse,String uploadedDocKey,File fileToUpload){

    String payload = null;
    if(uploadDocAPIResponse.contains("totalNumberOfPages")) {
        JSONObject jsonObject = new JSONObject(contractShowStrResponse);
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("parentSupplierId")
                .put("values", RandomString.getRandomAlphaNumericString(4));
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                .put("values", new JSONObject());
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                .getJSONObject("values").put("name", "No");
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                .getJSONObject("values").put("id", 515);

        JSONObject newFileObject = new JSONObject();
        newFileObject.put("key", uploadedDocKey);
        newFileObject.put("CDAction", 1);
        newFileObject.put("name", fileToUpload.getName().split("\\.")[0]);
        newFileObject.put("extension", fileToUpload.getName().split("\\.")[1]);
        newFileObject.put("viewerAvailable", true);
        newFileObject.put("search", true);
        newFileObject.put("download", true);
        newFileObject.put("financial", true);
        newFileObject.put("legal", true);
        newFileObject.put("businessCase", true);
        newFileObject.put("autoExtract", true);
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values")
                .put(newFileObject);
        payload = "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";
    }
    else {
        logger.error("Document is not uploaded Successfully");
        throw new SkipException("Document is not uploaded Successfully no need to continue");
    }
    return payload;
}

    public String createPayloadToUpdateWithoutUploadingDoc(String contractShowStrResponse){

        String payload = null;

            JSONObject jsonObject = new JSONObject(contractShowStrResponse);
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("parentSupplierId")
                    .put("values", RandomString.getRandomAlphaNumericString(4));
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                    .put("values", new JSONObject());
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                    .getJSONObject("values").put("name", "No");
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                    .getJSONObject("values").put("id", 515);

            payload = "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

        return payload;
    }

    public String createPayloadToEditDocWithUncheckAutoExtraction(String uploadDocAPIResponse,String contractShowStrResponse,String uploadedDocKey,File fileToUpload){

        String payload = null;
        if(uploadDocAPIResponse.contains("totalNumberOfPages")) {
            JSONObject jsonObject = new JSONObject(contractShowStrResponse);
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("parentSupplierId")
                    .put("values", RandomString.getRandomAlphaNumericString(4));
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                    .put("values", new JSONObject());
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                    .getJSONObject("values").put("name", "No");
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject("handoverRequired")
                    .getJSONObject("values").put("id", 515);

            JSONObject newFileObject = new JSONObject();
            newFileObject.put("key", uploadedDocKey);
            newFileObject.put("CDAction", 1);
            newFileObject.put("name", fileToUpload.getName().split("\\.")[0]);
            newFileObject.put("extension", fileToUpload.getName().split("\\.")[1]);
            newFileObject.put("viewerAvailable", true);
            newFileObject.put("search", true);
            newFileObject.put("download", true);
            newFileObject.put("financial", true);
            newFileObject.put("legal", true);
            newFileObject.put("businessCase", true);
            newFileObject.put("autoExtract", false);
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values")
                    .put(newFileObject);
            payload = "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";
        }
        else {
            logger.error("Document is not uploaded Successfully");
            throw new SkipException("Document is not uploaded Successfully no need to continue");
        }
        return payload;
    }

    public String createPayloadToEditDocForNoAutoExtractPermission(String uploadDocAPIResponse,String contractShowStrResponse,String uploadedDocKey,File fileToUpload){

        String payload = null;
        if(uploadDocAPIResponse.contains("totalNumberOfPages")) {
            JSONObject jsonObject = new JSONObject(contractShowStrResponse);

            JSONObject newFileObject = new JSONObject();
            newFileObject.put("key", uploadedDocKey);
            newFileObject.put("CDAction", 1);
            newFileObject.put("name", fileToUpload.getName().split("\\.")[0]);
            newFileObject.put("extension", fileToUpload.getName().split("\\.")[1]);
            newFileObject.put("viewerAvailable", true);
            newFileObject.put("search", true);
            newFileObject.put("download", true);
            newFileObject.put("financial", true);
            newFileObject.put("legal", true);
            newFileObject.put("businessCase", true);

            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values")
                    .put(newFileObject);
            payload = "{\"body\": {\"data\":" + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";
        }
        else {
            logger.error("Document is not uploaded Successfully");
            throw new SkipException("Document is not uploaded Successfully no need to continue");
        }
        return payload;
    }

public HttpResponse hitContractShowAPI(String contractId){
    HttpResponse response = null;
    HttpGet getRequest;
    try{
    String queryString = "/contracts/show/" + contractId;
    logger.debug("Query string url formed is {}", queryString);
    getRequest = new HttpGet(queryString);
    getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
    getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
    getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

    response = APIUtils.getRequest(getRequest,httpHost,false);
    logger.debug("Response status is {}", response.getStatusLine().toString());

    Header[] headers = response.getAllHeaders();
    for (Header oneHeader : headers) {
        logger.debug("Delete Delegation API header {}", oneHeader.toString());
    }
} catch (Exception e) {
        logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
    }
    return response;
}

    public String hitContractDocumentUpload(String filePath, String fileName, Map<String, String> payloadMap) {
        String uploadResponse = null;

        try {
            String queryString = "/file/upload/contractDocument";
            logger.debug("Query string url formed is {}", queryString);
            File fileToUpload = new File(filePath + "/" + fileName);
            String acceptHeader = "application/json, text/plain, */*";
            String contentTypeHeader = "multipart/form-data; boundary=----WebKitFormBoundarykZn8DRIyNRLBLQrV";
            String acceptEncodingHeader = "gzip, deflate, br";

            HttpPost postRequest = apiUtils.generateHttpPostRequestWithQueryString(queryString, acceptHeader, contentTypeHeader,acceptEncodingHeader);
            HttpEntity entity = APIUtils.createMultipartEntityBuilder("documentFileData", fileToUpload,"application/vnd.openxmlformats-officedocument.wordprocessingml.document", payloadMap);
            postRequest.setEntity(entity);

            HttpHost target = httpHost;
            uploadResponse = apiUtils.uploadFileToHttpsServer(target, postRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting File Upload Api. {}", e.getMessage());
        }
        return uploadResponse;
    }

    public HttpResponse hitContractEditAPI(String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/contracts/edit";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

public File getFileToUpload(String fileName){
    File fileToReturn = null;
    File file = new File(docFilePath);
    File[] files = file.listFiles();

    for(File file1 : files){
        if(file1.getName().equals(fileName)){
            fileToReturn = file1;
            break;
        }
    }
    return fileToReturn.getAbsoluteFile();
}

@DataProvider(name = "filesForObligationAutoExtraction")
public Object[][] getFilesToUploadForObligationAutoExtraction(){

    return new Object[][]{
            {getFileToUpload("1556788493SCHD#39;S)  TO AVPA-IKANDO-SOUTHAFRICA- 2016MA000458 (1).docx")},
           /* {getFileToUpload("Password_Protacted_Doc.docx")},
            {getFileToUpload("Corrupt_Document.docx")}*/
    };
}

    @DataProvider(name = "verifySystemFieldsDataProvider")
    public Object[][] getFilesToUploadToVerifySystemFields(){

        return new Object[][]{
                {getFileToUpload("1556607117SCHD3  TO AVPA-IKANDO-SOUTHAFRICA- 2016MA000458 (1).docx")},
        };
    }

    @DataProvider(name = "verifyAcceptActionFile")
    public Object[][] getFileToTestAcceptAction (){

        return new Object[][]{
                {getFileToUpload("File_For_Accept_Obligation.docx")},
        };
    }

    @DataProvider(name = "dataForUnCheckAutoExtraction")
    public Object[][] getDataForUnCheckAutoExtraction(){
        return new Object[][]{
                {getFileToUpload("1547638316FINAL VAA Ericsson - Annex G.docx")},
        };
    }

}


