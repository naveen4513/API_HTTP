package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.api.autoExtraction.ContractShow;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.mongodb.MongoDBConnection;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.RandomUtils;
import org.jsoup.select.Elements;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;




public class TestAutoExtractionAPIs extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestAutoExtractionAPIs.class);
    private String hostUrl;
    private Map<String, String> headers;
    private static JSONObject jsonObject;
    private static String configFilePath;
    private static String configAEFlagoffFilePath;
    private static String configAEFlagoffFileName;
    private static String configFileName;
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    static String configAELocalFileName;
    static String contractCreationConfigFilePath;
    static String contractCreationConfigFileName;
    static String cdrCreationConfigFileName;
    static String parentConfigFilePath;
    static String parentConfigFileName;

    static String entity;
    static int entityId;
    static Integer docId;
    static String docName;
    static String fileExtension;
    private static String autoExtractionServiceHostUrl;
    private static String mongoDBHost;
    private static int mongoDBPort;
    private static String aeHostUrl;
    private static int aePort;
    private static String aeSchema;
    private static String postgresHost;
    private static String postgresPort;
    private static String postgresDbName;
    private static String postgresDbUsername;
    private static String postgresDbPassword;
    private static String relationId;
    HttpHost aeHost;
    static String clientId;
    private static String algoId;

    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;
    MongoDBConnection connection;

    @BeforeClass
    public void beforeClass() {
        configAEFlagoffFilePath = ConfigureConstantFields.getConstantFieldsProperty("aeOffCDRCreationConfigFilePath");
        configAEFlagoffFileName = ConfigureConstantFields.getConstantFieldsProperty("aeOffCDRCreationConfigFileName");
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("autoExtractionCDRCreationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("autoExtractionCDRCreationConfigFileName");
        parentConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("aeParentInfoConfigFilePath");
        parentConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("aeParentInforConfigFileName");
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        configAELocalFileName =ConfigureConstantFields.getConstantFieldsProperty("AELocalConfigName");
        hostUrl = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "hosturl");
        docId = Integer.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docid"));
        contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFilePath");
        cdrCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("cdrCreationConfigFileName");
        contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileName");
        entity = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "entitiytocreate");
        relationId = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "contracts","sourceid");
        entityId=ConfigureConstantFields.getEntityIdByName("contracts");
        headers = ContractShow.getHeaders();
        autoExtractionServiceHostUrl =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","scheme") + "://" +
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","hostname") + ":"+
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","port");
        mongoDBHost =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"mongo db credentionals", "hosturl");
        mongoDBPort = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"mongo db credentionals", "port"));
        connection = new MongoDBConnection(mongoDBHost,mongoDBPort);
        aeHostUrl = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae environment", "hostname");
        aePort = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae environment", "port"));
        aeSchema = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae environment", "scheme");
        postgresHost = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "host");
        postgresPort = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "port");
        postgresDbName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "dbname");
        postgresDbUsername = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "username");
        postgresDbPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "password");
        clientId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"clientid");
        aeHost = new HttpHost(aeHostUrl,aePort,aeSchema);
        algoId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae1 configration","algoid");
    }

    @AfterMethod
    public void afterMethod()
    {
        Check check = new Check();
        // Login to End User
        String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
        HttpResponse loginResponse = check.hitCheck(endUserName, endUserPassword);
        softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");


    }
    /*
    * This Test Case Check whether necessary services related to Auto Extraction are up and running
    * if failed will skip all other test cases
    * */
    @Test
    public void TestServiceCheckAPI() throws IOException {
        softAssert = new SoftAssert();
        APIValidator response=executor.get(autoExtractionServiceHostUrl,"/autoExtraction",null);
        response.validateResponseCode(200, csAssert);
        softAssert.assertTrue(response.getResponse().getResponseCode()==200,
                "AutoExtraction Service is up and running");

        HttpResponse modelInfoResponse = AutoExtractionHelper.hitFetchModelInfoAPI(aeHost,"1002");
        softAssert.assertTrue(modelInfoResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        String modelInfoResponseStr = EntityUtils.toString(modelInfoResponse.getEntity());

        if(modelInfoResponseStr.equals("null")){
            throw new SkipException("No models are there to test auto-extraction");
        }
        else{
            softAssert.assertTrue(APIUtils.validJsonResponse(modelInfoResponseStr),"Response is not a valid Json");}

        String schedulerHost = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"scheduler environment", "hostname");
        int schedulerPort = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"scheduler environment", "port"));
        String schema = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"scheduler environment", "scheme");
        HttpHost httpHost = new HttpHost(schedulerHost,schedulerPort,schema);
        HttpGet httpGet = new HttpGet("/scheduler/");
        HttpResponse schedulerResponse = APIUtils.getRequest(httpGet,httpHost,false);

        softAssert.assertTrue(schedulerResponse.getStatusLine().getStatusCode()==200, "Scheduler is not up invalid response code");
        if(schedulerResponse.getStatusLine().getStatusCode()!=200){
            throw new SkipException("Scheduler is not up");
        }
        softAssert.assertAll();
    }

    /*
    * Test Cases related to filters and Columns in Document Repository Page and verifying columns
    * value does not vanish after applying filter
    * */
    @Test(dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestAutoExtractionDocListing() throws IOException {
        softAssert = new SoftAssert();
        HttpResponse docListMetaDataResponse = AutoExtractionHelper.checkAutoExtractionDocListingMetaData("/listRenderer/list/432/defaultUserListMetaData","{}");
        softAssert.assertTrue(docListMetaDataResponse.getStatusLine().getStatusCode() ==200,"Response Code is not Valid");

        String docListMetaDataResponseStr =EntityUtils.toString(docListMetaDataResponse.getEntity());
        JSONObject jsonObject = new JSONObject(docListMetaDataResponseStr);

        int columnsLength = jsonObject.getJSONArray("columns").length();
        List<String> allDefaultColumnsFromResponse = new LinkedList<>();
        for(int i=0;i<columnsLength;i++){
            allDefaultColumnsFromResponse.add(jsonObject.getJSONArray("columns").getJSONObject(i).get("defaultName").toString());
        }

        softAssert.assertTrue(allDefaultColumnsFromResponse.size() == 12, "Default Columns should only include Document Name, Contract Name, Extraction status, Extracted Data File");
        softAssert.assertTrue(allDefaultColumnsFromResponse.contains("CONTRACT TYPE"),"CONTRACT NAME column is not present");
        softAssert.assertTrue(allDefaultColumnsFromResponse.contains("PAGE COUNT"),"PAGE COUNT column is not present");
        softAssert.assertTrue(allDefaultColumnsFromResponse.contains("EXTRACTION STATUS"),"EXTRACTION STATUS column is not present");
        softAssert.assertTrue(allDefaultColumnsFromResponse.contains("DOCUMENT NAME"),"DOCUMENT NAME column is not present");
        softAssert.assertTrue(allDefaultColumnsFromResponse.contains("PROJECT"),"PAGE COUNT column is not present");
        softAssert.assertTrue(allDefaultColumnsFromResponse.contains("CLAUSE COUNT"),"CLAUSE COUNT column is not present");
        softAssert.assertTrue(allDefaultColumnsFromResponse.contains("CREATED ON"),"CREATED ON column is not present");
        softAssert.assertTrue(allDefaultColumnsFromResponse.contains("CREATED BY"),"CREATED BY column is not present");

        softAssert.assertAll();
    }

    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = false)
    public void verifyColumnsOptionOnListing() throws IOException {
        // Without applying any filter default columns
        softAssert = new SoftAssert();
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
        String payload = "{\"filterMap\":{}}";
        HttpResponse columnsListingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
        softAssert.assertTrue(columnsListingResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        String columnsListingResponseStr = EntityUtils.toString(columnsListingResponse.getEntity());

        JSONObject columnsListingResponseJson =  new JSONObject(columnsListingResponseStr);

        List<String> allDefaultColumns = new LinkedList<>();

        Map<String,Object> rowData = columnsListingResponseJson.getJSONArray("data").getJSONObject(
                RandomUtils.nextInt(0,columnsListingResponseJson.getJSONArray("data").length())).toMap();

        for(Map.Entry<String,Object> data  : rowData.entrySet()){
            allDefaultColumns.add(columnsListingResponseJson.getJSONArray("data").getJSONObject(
                    RandomUtils.nextInt(0,columnsListingResponseJson.getJSONArray("data").length())).getJSONObject(data.getKey()).get("columnName").toString());
        }

        // Applying Column Filter
        query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
        payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":18577,\"columnQueryName\":\"documentname\"},{\"columnId\":18578,\"columnQueryName\":\"contractname\"},{\"columnId\":18580,\"columnQueryName\":\"filelink\"},{\"columnId\":18590,\"columnQueryName\":\"totalpages\"}]}";

        columnsListingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
        softAssert.assertTrue(columnsListingResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        columnsListingResponseStr = EntityUtils.toString(columnsListingResponse.getEntity());

        columnsListingResponseJson =  new JSONObject(columnsListingResponseStr);

        List<String> filteredColumnsWithoutExtractedStatus = new LinkedList<>();

        rowData = columnsListingResponseJson.getJSONArray("data").getJSONObject(
                RandomUtils.nextInt(0,columnsListingResponseJson.getJSONArray("data").length())).toMap();

        for(Map.Entry<String,Object> data  : rowData.entrySet()){
            filteredColumnsWithoutExtractedStatus.add(columnsListingResponseJson.getJSONArray("data").getJSONObject(
                    RandomUtils.nextInt(0,columnsListingResponseJson.getJSONArray("data").length())).getJSONObject(data.getKey()).get("columnName").toString());
        }

        softAssert.assertTrue(filteredColumnsWithoutExtractedStatus.size() <  allDefaultColumns.size(),
                "Column filter is not working as aspect");
        softAssert.assertTrue(!filteredColumnsWithoutExtractedStatus.contains("status"),"Status should not be there thus column filter is not working properly");

        softAssert.assertAll();
    }

    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled=true)
    public void TestColumnNotDiminishAfterFilter() {
        softAssert = new SoftAssert();
        String url = "/listRenderer/list/432/listdata?version=2.0";
        List<String> allStatus;
        String payload;
        Map<Integer,String> allStatusFilters = new HashMap<>();
        allStatusFilters.put(1,"SUBMITTED");
        allStatusFilters.put(2,"QUEUED");
        allStatusFilters.put(3,"INPROGRESS");
        allStatusFilters.put(4,"COMPLETED");
        try {
            for (Map.Entry<Integer, String> entry : allStatusFilters.entrySet()) {
                allStatus = new LinkedList<>();
                payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + entry.getKey() + "\",\"" + entry.getValue() + "\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":16369,\"columnQueryName\":\"documentname\"},{\"columnId\":16370,\"columnQueryName\":\"contractname\"},{\"columnId\":16371,\"columnQueryName\":\"status\"},{\"columnId\":16381,\"columnQueryName\":\"totalpages\"},{\"columnId\":16454,\"columnQueryName\":\"projects\"},{\"columnId\":16455,\"columnQueryName\":\"groups\"},{\"columnId\":16456,\"columnQueryName\":\"doctags\"}]}";
                HttpResponse listingDataResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(url, payload);
                softAssert.assertTrue(listingDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
                String listingDataResponseStr = EntityUtils.toString(listingDataResponse.getEntity());

                JSONObject listingDataResponseStrJson = new JSONObject(listingDataResponseStr);
                int dataLength = listingDataResponseStrJson.getJSONArray("data").length();

                if (dataLength >= 1) {
                    int columnId = ListDataHelper.getColumnIdFromColumnName(listingDataResponseStr, "status");

                    for (int i = 0; i < dataLength; i++) {
                        allStatus.add(listingDataResponseStrJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[0]);
                    }
                    softAssert.assertTrue(allStatus.stream().map(m -> m.trim()).findAny().get().trim().equals(entry.getValue().trim()), "Status Value in records are not been diminished");
                } else {
                    logger.warn("There is no enough data present to test for column " + entry.getValue());
                }
            }
        }
        catch (Exception e){
            throw new SkipException("Error in verifying column values " + e.getStackTrace());
        }
        softAssert.assertAll();
    }



    /*
    * 1). Test cases related to Project, Group , Tag Creation and verifying project filters, columns and project listing page
    * 2). Apply project, group and tag to Documents in Document Repository and verify whether they are successfully applied
    * */
   @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = true)
    public void TestCreateProject() throws IOException {
        softAssert = new SoftAssert();
        // Project Default Meta Data Listing API to get all the default columns ids
        String metaDataListQuery = "/listRenderer/list/441/defaultUserListMetaData";
        String payload = "{}";
        HttpResponse projectDefaultMetaDataListResponse = AutoExtractionHelper.projectListDataAPI(metaDataListQuery,payload);

        softAssert.assertTrue(projectDefaultMetaDataListResponse.getStatusLine().getStatusCode() == 200, "Project Default Meta Data List Data Response Code is not valid");
        String projectDefaultMetaDataListDataResponseStr = EntityUtils.toString(projectDefaultMetaDataListResponse.getEntity());
        JSONObject projectDefaultMetaDatListDataResponseJson = new JSONObject(projectDefaultMetaDataListDataResponseStr);

        List<String> defaultColumnsId = new LinkedList<>();
        int defaultColumnsLength = projectDefaultMetaDatListDataResponseJson.getJSONArray("columns").length();

        for(int i=0;i<defaultColumnsLength;i++){
            defaultColumnsId.add(projectDefaultMetaDatListDataResponseJson.getJSONArray("columns").getJSONObject(i).get("id").toString());
        }

        // Hit Project Listing and get total number of records and default column names
        String listDataQuery = "/listRenderer/list/441/listdata?version=2.0&isFirstCall=true";
        payload = "{\"filterMap\":{}}";
        HttpResponse projectListDataResponse = AutoExtractionHelper.projectListDataAPI(listDataQuery,payload);
        softAssert.assertTrue(projectListDataResponse.getStatusLine().getStatusCode() == 200, "Project List Data Response Code is not valid");
        String projectListDataResponseStr = EntityUtils.toString(projectListDataResponse.getEntity());

        softAssert.assertTrue(APIUtils.validJsonResponse(projectListDataResponseStr),"Project List Data is not a valid Json");
        JSONObject projectListDataResponseJson = new JSONObject(projectListDataResponseStr);
        int initialFilteredCount = Integer.valueOf(projectListDataResponseJson.get("filteredCount").toString());

        int dataListLength = projectListDataResponseJson.getJSONArray("data").length();
        int columnsLength = projectListDataResponseJson.getJSONArray("data").getJSONObject(RandomNumbers.getRandomNumberWithinRangeIndex(0,dataListLength)).length();

        HashMap<Integer,String> columns = new LinkedHashMap<>();
        for(int i =0;i<columnsLength;i++){
            columns.put(Integer.valueOf(projectListDataResponseJson.getJSONArray("data").getJSONObject(RandomNumbers.getRandomNumberWithinRangeIndex(0,dataListLength)).getJSONObject(defaultColumnsId.get(i)).get("columnId").toString()),projectListDataResponseJson.getJSONArray("data").getJSONObject(RandomNumbers.getRandomNumberWithinRangeIndex(0,dataListLength)).getJSONObject(defaultColumnsId.get(i)).get("columnName").toString());
        }

        // Get Total Number of Columns to be used while filters
        JSONObject filterJson = new JSONObject("{\"filterMap\":{\"entityTypeId\":321,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[]}");

        for(Map.Entry<Integer,String> data : columns.entrySet()) {
            JSONObject selectedColumns = new JSONObject();
            selectedColumns.put("columnId",data.getKey());
            selectedColumns.put("columnQueryName",data.getValue());
            filterJson.getJSONArray("selectedColumns").put(selectedColumns);
        }

        // Remove column from filter json
        int columnToRemove = RandomNumbers.getRandomNumberWithinRangeIndex(0,columns.size());
        Object deletedColumn =filterJson.getJSONArray("selectedColumns").remove(columnToRemove);
        JSONObject deletedColumnJson = new JSONObject(deletedColumn.toString());

        // Apply filter on Project Listing
        listDataQuery = "/listRenderer/list/441/listdata?version=2.0&isFirstCall=true";
        payload = filterJson.toString();
        projectListDataResponse = AutoExtractionHelper.projectListDataAPI(listDataQuery,payload);
        softAssert.assertTrue(projectListDataResponse.getStatusLine().getStatusCode() == 200, "Project List Data Response Code is not valid");
        projectListDataResponseStr = EntityUtils.toString(projectListDataResponse.getEntity());

        softAssert.assertTrue(APIUtils.validJsonResponse(projectListDataResponseStr),"Project List Data is not a valid Json");
        projectListDataResponseJson = new JSONObject(projectListDataResponseStr);

        int filteredColumnsLength = projectListDataResponseJson.getJSONArray("data").getJSONObject(RandomNumbers.getRandomNumberWithinRangeIndex(0,projectListDataResponseJson.getJSONArray("data").length())).length();
        softAssert.assertTrue(filteredColumnsLength +1 == columnsLength,"Filter is not applied successfully");

        List<String> columnsAfterFilter = new LinkedList<>();
        for(String column : defaultColumnsId) {
            if(projectListDataResponseJson.getJSONArray("data").getJSONObject(RandomNumbers.getRandomNumberWithinRangeIndex(0, projectListDataResponseJson.getJSONArray("data").length())).has(column)){
                columnsAfterFilter.add(projectListDataResponseJson.getJSONArray("data").getJSONObject(RandomNumbers.getRandomNumberWithinRangeIndex(0, projectListDataResponseJson.getJSONArray("data").length())).getJSONObject(column).get("columnName").toString());
            }
        }

        softAssert.assertTrue(!columnsAfterFilter.contains(deletedColumnJson.get("columnQueryName").toString()),"Filter is not applied successfully as the column is still present in filtered columns list");

        // Create New Project
        // Get All the Meta Data Fields in Project to extract
        String getAllFieldsUrl = "/metadataautoextraction/getAllFields";
        HttpResponse metadataFieldResponse = AutoExtractionHelper.getAllMetaDataFields(getAllFieldsUrl);
        softAssert.assertTrue(metadataFieldResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
        String metadataFieldResponseStr = EntityUtils.toString(metadataFieldResponse.getEntity());

        JSONObject metadataFieldResponseJsonStr = new JSONObject(metadataFieldResponseStr);
        int metadataFieldsLength = metadataFieldResponseJsonStr.getJSONArray("response").length();
        HashMap<Integer,String> metadataFields = new LinkedHashMap<>();
        for(int i=0;i<metadataFieldsLength;i++){
            metadataFields.put(Integer.valueOf(metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("id").toString()),metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("name").toString());
        }

        if(metadataFields.size()<1){
            throw new SkipException("No Meta Data Fields are there to select in project");
        }

        String createProjectUrl = "/metadataautoextraction/create";
        String projectName = "Test_Automation" + RandomString.getRandomAlphaNumericString(10);
        String createProjectPayload = "{\"name\":\""+ projectName +"\",\"description\":\"sgsgd\",\"projectLinkedFieldIds\":["+ metadataFields.entrySet().stream().findFirst().get().getKey().intValue() +"],\"clientId\":"+ clientId +"}";
        HttpResponse projectCreationResponse = AutoExtractionHelper.createProject(createProjectUrl,createProjectPayload);
        softAssert.assertTrue(projectCreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
        String projectCreationResponseStr = EntityUtils.toString(projectCreationResponse.getEntity());
        softAssert.assertTrue(APIUtils.validJsonResponse(projectCreationResponseStr),"Not a valid Json");

        JSONObject createProjectJson = new JSONObject(projectCreationResponseStr);

        softAssert.assertTrue(createProjectJson.get("success").toString().equals("true"),"Project is not created successfully");
        int newlyCreatedProjectId = Integer.valueOf(createProjectJson.getJSONObject("response").get("id").toString());

        logger.info("Newly created project is " + newlyCreatedProjectId);

        // Create Project with Same Name
        projectCreationResponse = AutoExtractionHelper.createProject(createProjectUrl,createProjectPayload);
        softAssert.assertTrue(projectCreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
        projectCreationResponseStr = EntityUtils.toString(projectCreationResponse.getEntity());

        createProjectJson = new JSONObject(projectCreationResponseStr);
        softAssert.assertTrue(createProjectJson.get("response").equals("Project Name already exists."),"System is allowing to create duplicate projects");

        // Verify Project Listing
        listDataQuery = "/listRenderer/list/441/listdata?version=2.0&isFirstCall=true";
        payload = "{\"filterMap\":{}}";
        projectListDataResponse = AutoExtractionHelper.projectListDataAPI(listDataQuery,payload);
        softAssert.assertTrue(projectListDataResponse.getStatusLine().getStatusCode() == 200, "Project List Data Response Code is not valid");
        projectListDataResponseStr = EntityUtils.toString(projectListDataResponse.getEntity());

        softAssert.assertTrue(APIUtils.validJsonResponse(projectListDataResponseStr),"Project List Data is not a valid Json");
        projectListDataResponseJson = new JSONObject(projectListDataResponseStr);
        int finalFilteredCount = Integer.valueOf(projectListDataResponseJson.get("filteredCount").toString());

        softAssert.assertTrue(initialFilteredCount+1 == finalFilteredCount,"Project is not added to list data");

        // Verify Project Added in Master Data API
        // Hit master Data API to verify whether created group and tag are present in master data API
        HttpResponse hitMasterDataAPI = AutoExtractionHelper.hitShowViewerAPI("/autoextraction/masterData");
        String masterDataStr = EntityUtils.toString(hitMasterDataAPI.getEntity());
        softAssert.assertTrue(hitMasterDataAPI.getStatusLine().getStatusCode() == 200,"Response Code for master data api is not valid");

        List<String> projectIds = AutoExtractionHelper.getIdsForEntityFromName(masterDataStr,"projects");

        // Check whether newly created group Id is present in master data or not
        boolean isProjectStoredInMasterData;
        isProjectStoredInMasterData = AutoExtractionHelper.verifyEntityAddedInMasterData(projectIds,newlyCreatedProjectId);
        softAssert.assertTrue(isProjectStoredInMasterData,"Data is not there in master data with projectName = " + projectName);

        softAssert.assertAll();
    }

   @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = false)
    public void testGroupTagCreation() throws IOException {
        softAssert = new SoftAssert();
        // Create new Group
        int newlyCreatedGroupId;
        String url = "/metadataautoextraction/create/2";
        String newlyCreatedGroupName = "Group" + RandomString.getRandomAlphaNumericString(10);
        String payload = "{\"name\":\""+ newlyCreatedGroupName +"\"}";
        HttpResponse groupCreationResponse = AutoExtractionHelper.hitGroupOrTagCreationAndUpdatePropertiesAPI(url,payload);
        softAssert.assertTrue(groupCreationResponse.getStatusLine().getStatusCode() == 200,"Group Creation API Response Code is not valid");
        String groupCreationResponseStr = EntityUtils.toString(groupCreationResponse.getEntity());
        JSONObject groupCreationResponseJsonStr = new JSONObject(groupCreationResponseStr);
        if(groupCreationResponseJsonStr.has("id")){
            newlyCreatedGroupId = Integer.valueOf(groupCreationResponseJsonStr.get("id").toString());}
        else {
            throw new SkipException("Error in Creating new Group with group name " + newlyCreatedGroupName);
        }

        // Create new Tag
        int newlyCreatedTagId;
        url = "/metadataautoextraction/create/3";
        String newlyCreatedTagName = "Tag" + RandomString.getRandomAlphaNumericString(10);
        payload = "{\"name\":\""+ newlyCreatedTagName +"\"}";
        HttpResponse tagCreationResponse = AutoExtractionHelper.hitGroupOrTagCreationAndUpdatePropertiesAPI(url,payload);
        softAssert.assertTrue(tagCreationResponse.getStatusLine().getStatusCode() == 200,"Tag Creation API Response Code is not valid");
        String tagCreationResponseStr = EntityUtils.toString(tagCreationResponse.getEntity());
        JSONObject tagCreationResponseJsonStr = new JSONObject(tagCreationResponseStr);
        if(tagCreationResponseJsonStr.has("id")){
            newlyCreatedTagId = Integer.valueOf(tagCreationResponseJsonStr.get("id").toString());}
        else {
            throw new SkipException("Error in Creating new Group with group name " + newlyCreatedTagName);}

        // Extract listing Data to get document id on which project, group and tag should be applied
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
        payload = "{\"filterMap\":{}}";
        HttpResponse autoExtractionListingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
        softAssert.assertTrue(autoExtractionListingResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        String autoExtractionListingResponseStr = EntityUtils.toString(autoExtractionListingResponse.getEntity());

        JSONObject autoExtractionListingResponseJson =  new JSONObject(autoExtractionListingResponseStr);

        int numberOfExtractedObligation = autoExtractionListingResponseJson.getJSONArray("data").length();
        int recordToPick = RandomNumbers.getRandomNumberWithinRangeIndex(0,numberOfExtractedObligation);
        List<String> columns = autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(recordToPick).keySet().stream().collect(Collectors.toList());

        int documentId =0;
        for(int i=0;i<columns.size();i++){
            if(autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(recordToPick).getJSONObject(columns.get(i)).get("columnName").toString().equals("documentname")){
                documentId = Integer.valueOf(autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(recordToPick).getJSONObject(columns.get(i)).get("value").toString().split(":;")[1]);
                break;
            }
        }

        // Update Properties to the document in listing with project, group and tags attaching it
        url = "/autoextraction/updateProperties";
        payload = "{\"contractDocumentId\":\""+documentId+"\",\"projectIds\":[1],\"groupIds\":["+newlyCreatedGroupId+"],\"tagIds\":["+newlyCreatedTagId+"]}";
        HttpResponse updatePropertiesResponse = AutoExtractionHelper.hitGroupOrTagCreationAndUpdatePropertiesAPI(url,payload);
        softAssert.assertTrue(updatePropertiesResponse.getStatusLine().getStatusCode() == 200,"Update Properties API Response Code is not valid");

        // Verify Groups and Tags associated with document in auto extraction listing data
        query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
        payload = "{\"filterMap\":{}}";
        autoExtractionListingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
        softAssert.assertTrue(autoExtractionListingResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        autoExtractionListingResponseStr = EntityUtils.toString(autoExtractionListingResponse.getEntity());

        autoExtractionListingResponseJson =  new JSONObject(autoExtractionListingResponseStr);

        numberOfExtractedObligation = autoExtractionListingResponseJson.getJSONArray("data").length();
        recordToPick = RandomNumbers.getRandomNumberWithinRangeIndex(0,numberOfExtractedObligation);
        columns = autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(recordToPick).keySet().stream().collect(Collectors.toList());

        int index =0;
        for(int i=0;i<numberOfExtractedObligation;i++){
            for(int j=0;j<columns.size();j++){
                if(autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(columns.get(j)).get("columnName").equals("documentname")){
                    if(Integer.valueOf(autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(columns.get(j)).get("value").toString().split(":;")[1].trim()) == documentId){
                        index = i;
                        break;
                    }
                }
            }
        }

        String expectedDoctags = null;
        String expectedGroup = null;
        // Verify on above listing index
        for(int i=0;i<columns.size();i++){
            if(autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(index).getJSONObject(columns.get(i)).get("columnName").toString().trim().equals("doctags")){
                expectedDoctags = autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(index).getJSONObject(columns.get(i)).get("value").toString().trim();
            }
            else if(autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(index).getJSONObject(columns.get(i)).get("columnName").toString().trim().equals("groups")){
                expectedGroup = autoExtractionListingResponseJson.getJSONArray("data").getJSONObject(index).getJSONObject(columns.get(i)).get("value").toString().trim();
            }
        }

        softAssert.assertTrue(expectedGroup.split(":;")[0].trim().equals(newlyCreatedGroupName),"Group Name is not matched with newly created group");
        softAssert.assertTrue(Integer.valueOf(expectedGroup.split(":;")[1].trim()) == newlyCreatedGroupId,"Group Id is not matched with newly created group");
        softAssert.assertTrue(expectedDoctags.split(":;")[0].trim().equals(newlyCreatedTagName),"Tag Name is not matched with newly created tag");
        softAssert.assertTrue(Integer.valueOf(expectedDoctags.split(":;")[1].trim()) == newlyCreatedTagId,"Tag Id is not matched with newly created tag");
        softAssert.assertAll();
    }

   @Test(dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestMasterDataAPI() throws IOException {
        softAssert = new SoftAssert();
        // Create new Group
        int newlyCreatedGroupId;
        String url = "/metadataautoextraction/create/2";
        String newlyCreatedGroupName = "Group" + RandomString.getRandomAlphaNumericString(10);
        String payload = "{\"name\":\""+ newlyCreatedGroupName +"\"}";
        HttpResponse groupCreationResponse = AutoExtractionHelper.hitGroupOrTagCreationAndUpdatePropertiesAPI(url,payload);
        softAssert.assertTrue(groupCreationResponse.getStatusLine().getStatusCode() == 200,"Group Creation API Response Code is not valid");
        String groupCreationResponseStr = EntityUtils.toString(groupCreationResponse.getEntity());
        JSONObject groupCreationResponseJsonStr = new JSONObject(groupCreationResponseStr);
        if(groupCreationResponseJsonStr.has("id")){
            newlyCreatedGroupId = Integer.valueOf(groupCreationResponseJsonStr.get("id").toString());}
        else {
            throw new SkipException("Error in Creating new Group with group name " + newlyCreatedGroupName);
        }

        // Create new Tag
        int newlyCreatedTagId;
        url = "/metadataautoextraction/create/3";
        String newlyCreatedTagName = "Group" + RandomString.getRandomAlphaNumericString(10);
        payload = "{\"name\":\""+ newlyCreatedTagName +"\"}";
        HttpResponse tagCreationResponse = AutoExtractionHelper.hitGroupOrTagCreationAndUpdatePropertiesAPI(url,payload);
        softAssert.assertTrue(tagCreationResponse.getStatusLine().getStatusCode() == 200,"Tag Creation API Response Code is not valid");
        String tagCreationResponseStr = EntityUtils.toString(tagCreationResponse.getEntity());
        JSONObject tagCreationResponseJsonStr = new JSONObject(tagCreationResponseStr);
        if(tagCreationResponseJsonStr.has("id")){
            newlyCreatedTagId = Integer.valueOf(tagCreationResponseJsonStr.get("id").toString());}
        else {
            throw new SkipException("Error in Creating new Tag with Tag name " + newlyCreatedTagName);}

        // Hit master Data API to verify whether created group and tag are present in master data API
        HttpResponse hitMasterDataAPI = AutoExtractionHelper.hitShowViewerAPI("/autoextraction/masterData");
        String masterDataStr = EntityUtils.toString(hitMasterDataAPI.getEntity());
        softAssert.assertTrue(hitMasterDataAPI.getStatusLine().getStatusCode() == 200,"Response Code for master data api is not valid");

        List<String> groupIds = AutoExtractionHelper.getIdsForEntityFromName(masterDataStr,"groups");
        List<String> tagIds = AutoExtractionHelper.getIdsForEntityFromName(masterDataStr,"tags");

        // Check whether newly created group Id is present in master data or not
        boolean isGroupsStoredInMasterData;
        isGroupsStoredInMasterData = AutoExtractionHelper.verifyEntityAddedInMasterData(groupIds,newlyCreatedGroupId);
        softAssert.assertTrue(isGroupsStoredInMasterData,"Data is not there in master data with groupName = " + newlyCreatedGroupName);

        // Check whether newly created group Id is present in master data or not
        boolean isTagsStoredInMasterData;
        isTagsStoredInMasterData = AutoExtractionHelper.verifyEntityAddedInMasterData(tagIds,newlyCreatedTagId);
        softAssert.assertTrue(isTagsStoredInMasterData,"Data is not there in master data with tagName = " + newlyCreatedTagName);
        softAssert.assertAll();
    }

   @Test(enabled = false)
    public void TestAutoExtractionGlobalUpload() throws IOException {
        // File Upload API to get the key of file uploaded
        softAssert = new SoftAssert();
        String filePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"fileuploadpath");
        String fileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "fileuploadname");
        Map<String,String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(filePath,fileName);

        // Hit Listing API to get number of records before global upload
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
        String payload = "{\"filterMap\":{}}";
        HttpResponse httpResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
        softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
        String listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
        JSONObject jsonObject = new JSONObject(listDataResponseStr);

        int initialRecords = Integer.valueOf(jsonObject.get("filteredCount").toString());

        // Hit Global Upload API
        payload = "[{\"extension\": \""+uploadedFileProperty.get("extension")+"\",\"key\": \""+uploadedFileProperty.get("key")+"\",\"name\": \""+uploadedFileProperty.get("name")+"\"}]";
        httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(),payload);
        softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");

        // Hit Listing API to get number of records after global upload
        payload = "{\"filterMap\":{}}";
        httpResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
        softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
        listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
        jsonObject = new JSONObject(listDataResponseStr);

        int finalRecords = Integer.valueOf(jsonObject.get("filteredCount").toString());

        softAssert.assertTrue(initialRecords +1 == finalRecords,"Global Upload is not Successful");
        softAssert.assertAll();
    }

    /*
    * Test Case to verify Auto Extraction functionality for Searchable Pdf
    * */
   @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = true)
    public void TestSearchablePdfAutoExtraction() throws Exception {
        softAssert = new SoftAssert();
        String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");

        String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"searchable pdf document", "fileuploadpath");
        String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "searchable pdf document","fileuploadname");

        int contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath,contractCreationConfigFileName,entity,templateFilePath,templateFileName,relationId,true);
        String apiPath =ContractShow.getAPIPath();
        apiPath=String.format(apiPath + contractId);
        HttpGet httpGet = new HttpGet(apiPath);
        httpGet.addHeader("Content-Type","application/json;charset=UTF-8");
        httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        HttpResponse newlyCreatedContractShowResponse =APIUtils.getRequest(httpGet);
        String newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());
        jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);

        docName= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("name");
        docId= (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
        fileExtension= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("extension");

        if(ConfigureEnvironment.environment.contains("sandbox")){
            boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(endUserName,endUserPassword);
            softAssert.assertTrue(isExtractionCompleted,"Extraction not Completed");
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson =  new JSONObject(listingResponseStr);
            Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
            for (String key : keys) {
                if(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("documentname")){
                    docId = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1].trim());
                    break;
                }
            }
        }

        else {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
            // Get Document Id from contract tree
            String query = "SELECT id FROM contract_document WHERE contract_id ="+ contractId + ";";
            List<List<String>> documentData = postgreSQLJDBC.doSelect(query);
            int contractTreeDocId = Integer.valueOf(documentData.get(0).get(0));

            // Verify Scheduler status and get document id from scheduler table
            query = "SELECT  id,status_id FROM autoextraction_document_request WHERE document_id = "+contractTreeDocId+";";
            List<List<String>> schedulerData = postgreSQLJDBC.doSelect(query);
            int statusId = Integer.valueOf(schedulerData.get(0).get(1));
            docId = Integer.valueOf(schedulerData.get(0).get(0));

            LocalTime initialTime = LocalTime.now();
            while(statusId !=2){
                logger.info("Scheduler hasn't picked yet " + statusId);
                schedulerData = postgreSQLJDBC.doSelect(query);
                statusId = Integer.valueOf(schedulerData.get(0).get(1));

                if (statusId == 3) {
                    throw new SkipException("Document will not be picked by scheduler as task is failed");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Scheduler to pick the document for extraction = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Waited for 10 minutes for Scheduler to pick the document." +
                            "Please look manually whether their is problem in Scheduler to pick the document." +
                            "For document id " + docId + " in Automation Listing and document id " + contractTreeDocId + " in Contract Tree");
                }
            }

            // Get extraction status from extraction status table
            query = "SELECT ads.document_id, min(extraction_status) as status  FROM  autoextraction_document_status ads where ads.document_id=" + docId + " GROUP BY ads.document_id";
            List<List<String>> documentStatusData = postgreSQLJDBC.doSelect(query);

            int documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));

            initialTime = LocalTime.now();
            while (documentExtractionStatus != 4) {
                documentStatusData = postgreSQLJDBC.doSelect(query);

                documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
                if (documentExtractionStatus == 1) {
                    logger.info("Document is submitted for Auto-Extraction");
                } else if (documentExtractionStatus == 2) {
                    logger.info("Document is in pre-processing stage");
                } else if (documentExtractionStatus == 3) {
                    logger.info("Document is in post-processing stage");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Extraction to complete Wait Time = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Extraction is working slow already waited for 10 minutes." +
                            "Please look manually whether their is problem in extraction or services are working slow." +
                            "For document id " + docId);
                }
            }
            softAssert.assertTrue(documentExtractionStatus == 4, "Extraction is not completed for the document");
        }

        // Verify Contracts Auto-Extraction Listing Data
        String contractListingPayload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"367\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":367,\"filterName\":\"fieldId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"entityId\":"+ docId +"}";
        HttpResponse listingResponse  = AutoExtractionHelper.getListingData("/listRenderer/list/433/listdata",contractListingPayload);
        String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
        softAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");

        softAssert.assertTrue(JSONUtility.validjson(listingResponseStr),"Not a valid Json means contract extraction is not happened");
        JSONObject listingResponseJsonStr = new JSONObject(listingResponseStr);
        softAssert.assertTrue(listingResponseJsonStr.getJSONArray("data").length()>1,"Extracted Data is not present");

        // Verify Obligation Auto-Extraction Listing Data
        String obligationListingPayload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"367\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":367,\"filterName\":\"fieldId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"entityId\":"+docId+"}";
        listingResponse  = AutoExtractionHelper.getListingData("/listRenderer/list/437/listdata",obligationListingPayload);
        listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
        softAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");

        softAssert.assertTrue(JSONUtility.validjson(listingResponseStr),"Not a valid Json means contract extraction is not happened");
        listingResponseJsonStr = new JSONObject(listingResponseStr);
        softAssert.assertTrue(listingResponseJsonStr.getJSONArray("data").length()>1,"Extracted Data is not present");
        EntityOperationsHelper.deleteEntityRecord("contracts",contractId);
        softAssert.assertAll();
    }

    /*
    * Test Case to verify Auto Extraction Functionality for Non Searchable Pdf
    * */
   @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = true)
    public void TestNonSearchablePdfAutoExtraction() throws SQLException, IOException, InterruptedException {
        softAssert = new SoftAssert();
        String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");

        String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"non searchable pdf document", "fileuploadpath");
        String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "non searchable pdf document","fileuploadname");

        int contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath,contractCreationConfigFileName,entity,templateFilePath,templateFileName,relationId,true);
        String apiPath =ContractShow.getAPIPath();
        apiPath=String.format(apiPath + contractId);
        HttpGet httpGet = new HttpGet(apiPath);
        httpGet.addHeader("Content-Type","application/json;charset=UTF-8");
        httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        HttpResponse newlyCreatedContractShowResponse =APIUtils.getRequest(httpGet);
        String newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());

        jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);

        docName= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("name");
        docId= (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
        fileExtension= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("extension");

        if(ConfigureEnvironment.environment.contains("sandbox")){
            boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(endUserName,endUserPassword);
            softAssert.assertTrue(isExtractionCompleted,"Extraction not Completed");
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson =  new JSONObject(listingResponseStr);
            Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
            for (String key : keys) {
                if(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("documentname")){
                    docId = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1].trim());
                    break;
                }
            }
        }
        else {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
            // Get Document Id from contract tree
            String query = "SELECT id FROM contract_document WHERE contract_id ="+ contractId + ";";
            List<List<String>> documentData = postgreSQLJDBC.doSelect(query);
            int contractTreeDocId = Integer.valueOf(documentData.get(0).get(0));

            // Verify Scheduler status and get document id from scheduler table
            query = "SELECT  id,status_id FROM autoextraction_document_request WHERE document_id = "+contractTreeDocId+";";
            List<List<String>> schedulerData = postgreSQLJDBC.doSelect(query);
            int statusId = Integer.valueOf(schedulerData.get(0).get(1));
            docId = Integer.valueOf(schedulerData.get(0).get(0));

            LocalTime initialTime = LocalTime.now();
            while(statusId !=2){
                logger.info("Scheduler hasn't picked yet " + statusId);
                schedulerData = postgreSQLJDBC.doSelect(query);
                statusId = Integer.valueOf(schedulerData.get(0).get(1));

                if (statusId == 3) {
                    throw new SkipException("Document will not be picked by scheduler as task is failed");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Scheduler to pick the document for extraction = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Waited for 10 minutes for Scheduler to pick the document." +
                            "Please look manually whether their is problem in Scheduler to pick the document." +
                            "For document id " + docId + " in Automation Listing and document id " + contractTreeDocId + " in Contract Tree");
                }
            }

            // Get extraction status from extraction status table
            query = "SELECT ads.document_id, min(extraction_status) as status  FROM  autoextraction_document_status ads where ads.document_id=" + docId + " GROUP BY ads.document_id";
            List<List<String>> documentStatusData = postgreSQLJDBC.doSelect(query);

            int documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));

            initialTime = LocalTime.now();
            while (documentExtractionStatus != 4) {
                documentStatusData = postgreSQLJDBC.doSelect(query);

                documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
                if (documentExtractionStatus == 1) {
                    logger.info("Document is submitted for Auto-Extraction");
                } else if (documentExtractionStatus == 2) {
                    logger.info("Document is in pre-processing stage");
                } else if (documentExtractionStatus == 3) {
                    logger.info("Document is in post-processing stage");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Extraction to complete Wait Time = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Extraction is working slow already waited for 10 minutes." +
                            "Please look manually whether their is problem in extraction or services are working slow." +
                            "For document id " + docId);
                }
            }
            softAssert.assertTrue(documentExtractionStatus == 4, "Extraction is not completed for the document");
        }
        EntityOperationsHelper.deleteEntityRecord("contracts",contractId);
        softAssert.assertAll();
    }

    /*
    * 1). Test Case to verify Auto Extraction functionality for doc,docx documents
    * 2). Verify Contract and Obligation Listing Data
    * 3). Verify Functionality of Doc Viewer Accessibility when viewer is enabled or disabled
    * */
    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = true)
    public void TestUploadedDocWithViewerAndNoViewer() throws Exception {
        softAssert = new SoftAssert();
        String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");

        String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"fileuploadpath");
        String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "fileuploadname");

        int contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath,contractCreationConfigFileName,entity,templateFilePath,templateFileName,relationId,true);
        String apiPath =ContractShow.getAPIPath();
        apiPath=String.format(apiPath + contractId);
        HttpGet httpGet = new HttpGet(apiPath);
        httpGet.addHeader("Content-Type","application/json;charset=UTF-8");
        httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        HttpResponse newlyCreatedContractShowResponse =APIUtils.getRequest(httpGet);
        String newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());

        jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);

        docName= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("name");
        docId= (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
        fileExtension= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("extension");

        if(ConfigureEnvironment.environment.contains("sandbox")){
           boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(endUserName,endUserPassword);
           softAssert.assertTrue(isExtractionCompleted,"Extraction not Completed");
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson =  new JSONObject(listingResponseStr);
            Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
            for (String key : keys) {
                if(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("documentname")){
                    docId = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1].trim());
                    break;
                }
            }

            HttpResponse getExtractionDocMetadataResponse = AutoExtractionHelper.hitExtractDocViewerDocId(docId);
            softAssert.assertTrue(getExtractionDocMetadataResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
            String getExtractionDocMetadataResponseStr = EntityUtils.toString(getExtractionDocMetadataResponse.getEntity());
            jsonObject = new JSONObject(getExtractionDocMetadataResponseStr);
            docId = Integer.valueOf(jsonObject.getJSONObject("response").getJSONObject("document").get("id").toString());
        }
        else {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
            // Get Document Id from contract tree
            String query = "SELECT id FROM contract_document WHERE contract_id ="+ contractId + ";";
            List<List<String>> documentData = postgreSQLJDBC.doSelect(query);
            int contractTreeDocId = Integer.valueOf(documentData.get(0).get(0));

            // Verify Scheduler status and get document id from scheduler table
            query = "SELECT  id,status_id FROM autoextraction_document_request WHERE document_id = "+contractTreeDocId+";";
            List<List<String>> schedulerData = postgreSQLJDBC.doSelect(query);
            int statusId = Integer.valueOf(schedulerData.get(0).get(1));
            docId = Integer.valueOf(schedulerData.get(0).get(0));

            LocalTime initialTime = LocalTime.now();
            while(statusId !=2){
                logger.info("Scheduler hasn't picked yet " + statusId);
                schedulerData = postgreSQLJDBC.doSelect(query);
                statusId = Integer.valueOf(schedulerData.get(0).get(1));

                if (statusId == 3) {
                    throw new SkipException("Document will not be picked by scheduler as task is failed");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Scheduler to pick the document for extraction = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Waited for 10 minutes for Scheduler to pick the document." +
                            "Please look manually whether their is problem in Scheduler to pick the document." +
                            "For document id " + docId + " in Automation Listing and document id " + contractTreeDocId + " in Contract Tree");
                }
            }

            // Get extraction status from extraction status table
            query = "SELECT ads.document_id, min(extraction_status) as status  FROM  autoextraction_document_status ads where ads.document_id=" + docId + " GROUP BY ads.document_id";
            List<List<String>> documentStatusData = postgreSQLJDBC.doSelect(query);

            int documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));

            initialTime = LocalTime.now();
            while (documentExtractionStatus != 4) {
                documentStatusData = postgreSQLJDBC.doSelect(query);

                documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
                if (documentExtractionStatus == 1) {
                    logger.info("Document is submitted for Auto-Extraction");
                } else if (documentExtractionStatus == 2) {
                    logger.info("Document is in pre-processing stage");
                } else if (documentExtractionStatus == 3) {
                    logger.info("Document is in post-processing stage");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Extraction to complete Wait Time = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Extraction is working slow already waited for 10 minutes." +
                            "Please look manually whether their is problem in extraction or services are working slow." +
                            "For document id " + docId);
                }
            }
            softAssert.assertTrue(documentExtractionStatus == 4, "Extraction is not completed for the document");
        }
        HttpResponse docViewerResponse = AutoExtractionHelper.hitShowViewerAPI("/documentviewer/show/"+ docId);
        String docViewerResponseStr = EntityUtils.toString(docViewerResponse.getEntity());
        softAssert.assertTrue(docViewerResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        softAssert.assertTrue(docViewerResponseStr.contains("\"response\":{\"type\":\"basic\",\"status\":\"success\"}"),"Viewer is not visible which is not expected");
        EntityOperationsHelper.deleteEntityRecord("contracts",contractId);

        // Auto Extraction with viewer off
        contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath,contractCreationConfigFileName,entity,templateFilePath,templateFileName,relationId,false);
        apiPath =ContractShow.getAPIPath();
        apiPath=String.format(apiPath + contractId);

        httpGet = new HttpGet(apiPath);
        httpGet.addHeader("Content-Type","application/json;charset=UTF-8");
        httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        newlyCreatedContractShowResponse =APIUtils.getRequest(httpGet);
        newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());

        jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);

        docName= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("name");
        docId= (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
        fileExtension= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("extension");

        if(ConfigureEnvironment.environment.contains("sandbox")){
            boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(endUserName,endUserPassword);
            softAssert.assertTrue(isExtractionCompleted,"Extraction not Completed");
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson =  new JSONObject(listingResponseStr);
            Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
            for (String key : keys) {
                if(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("documentname")){
                    docId = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1].trim());
                    break;
                }
            }

            HttpResponse getExtractionDocMetadataResponse = AutoExtractionHelper.hitExtractDocViewerDocId(docId);
            softAssert.assertTrue(getExtractionDocMetadataResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
            String getExtractionDocMetadataResponseStr = EntityUtils.toString(getExtractionDocMetadataResponse.getEntity());
            jsonObject = new JSONObject(getExtractionDocMetadataResponseStr);
            docId = Integer.valueOf(jsonObject.getJSONObject("response").getJSONObject("document").get("id").toString());
        }
        else {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
            // Get Document Id from contract tree
            String query = "SELECT id FROM contract_document WHERE contract_id ="+ contractId + ";";
            List<List<String>> documentData = postgreSQLJDBC.doSelect(query);
            int contractTreeDocId = Integer.valueOf(documentData.get(0).get(0));

            // Verify Scheduler status and get document id from scheduler table
            query = "SELECT  id,status_id FROM autoextraction_document_request WHERE document_id = "+contractTreeDocId+";";
            List<List<String>> schedulerData = postgreSQLJDBC.doSelect(query);
            int statusId = Integer.valueOf(schedulerData.get(0).get(1));
            docId = Integer.valueOf(schedulerData.get(0).get(0));

            LocalTime initialTime = LocalTime.now();
            while(statusId !=2){
                logger.info("Scheduler hasn't picked yet " + statusId);
                schedulerData = postgreSQLJDBC.doSelect(query);
                statusId = Integer.valueOf(schedulerData.get(0).get(1));

                if (statusId == 3) {
                    throw new SkipException("Document will not be picked by scheduler as task is failed");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Scheduler to pick the document for extraction = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Waited for 10 minutes for Scheduler to pick the document." +
                            "Please look manually whether their is problem in Scheduler to pick the document." +
                            "For document id " + docId + " in Automation Listing and document id " + contractTreeDocId + " in Contract Tree");
                }
            }

            // Get extraction status from extraction status table
            query = "SELECT ads.document_id, min(extraction_status) as status  FROM  autoextraction_document_status ads where ads.document_id=" + docId + " GROUP BY ads.document_id";
            List<List<String>> documentStatusData = postgreSQLJDBC.doSelect(query);

            int documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));

            initialTime = LocalTime.now();
            while (documentExtractionStatus != 4) {
                documentStatusData = postgreSQLJDBC.doSelect(query);

                documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
                if (documentExtractionStatus == 1) {
                    logger.info("Document is submitted for Auto-Extraction");
                } else if (documentExtractionStatus == 2) {
                    logger.info("Document is in pre-processing stage");
                } else if (documentExtractionStatus == 3) {
                    logger.info("Document is in post-processing stage");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Extraction to complete Wait Time = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Extraction is working slow already waited for 10 minutes." +
                            "Please look manually whether their is problem in extraction or services are working slow." +
                            "For document id " + docId);
                }
            }
            softAssert.assertTrue(documentExtractionStatus == 4, "Extraction is not completed for the document");
        }
        docViewerResponse = AutoExtractionHelper.hitShowViewerAPI("/documentviewer/show/"+ docId);
        docViewerResponseStr = EntityUtils.toString(docViewerResponse.getEntity());
        softAssert.assertTrue(docViewerResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        softAssert.assertTrue(docViewerResponseStr.contains("\"errorMessage\":\"Either you do not have the required permissions or requested page does not exist anymore.\""),"Viewer is visible which is not expected");
        EntityOperationsHelper.deleteEntityRecord("contracts",contractId);
        softAssert.assertAll();
    }

    /*
    * 1). Test Case to verify Meta Data Checkbox and Doc Listing Access from Client Admin
    * 2). Working Fine for Doc Listing Access
    * 3). Error in Meta Data Checkbox Access due to no restriction in API
    * */
   @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled=true)
    public void verifyMetaDataAndDocumentAccessClientAdmin() throws IOException {
        softAssert = new SoftAssert();
        Check check = new Check();

        // Login to Client Admin
        String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "username");
        String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "password");
        HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
        softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is not valid");

        String url = "/masteruserrolegroups/update/";
        String roleGroupId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"rolegroupid");
        HttpResponse masterUserRoleGroupResponse = AutoExtractionHelper.hitMasterUserRoleGroup(url,roleGroupId);
        softAssert.assertTrue(masterUserRoleGroupResponse.getStatusLine().getStatusCode()==200,"Master Role Group API Response is not valid");
        String masterUserRoleGroupResponseStr = EntityUtils.toString(masterUserRoleGroupResponse.getEntity());

        org.jsoup.nodes.Document document = Jsoup.parse(masterUserRoleGroupResponseStr);
        List<String> allSections = document.select("#permission h3").stream().map(m->m.text()).collect(Collectors.toList());
        int globalPermisssionIndex = 0;
        int contractPermissionIndex = 0;
        for(int i=0;i<allSections.size();i++){
            if(allSections.get(i).equals("GLOBAL PERMISSION:")){
                globalPermisssionIndex = i ;
            }
            else if(allSections.get(i).equals("AUTOEXTRACTION:")){
                contractPermissionIndex = i;
            }
        }
        List<String> allSectionsValue = document.select("#permission div").stream().map(m->m.text()).collect(Collectors.toList());
        softAssert.assertTrue(allSectionsValue.get(globalPermisssionIndex).contains("Autoextract Metadata"),"Auto Extract MetaData is not present under global permissions section");
        softAssert.assertTrue(allSectionsValue.get(contractPermissionIndex).contains("Auto Extracted Doc Access"),"Auto Extracted Doc Access is not present under autoextraction section");

        FileUtils fileUtils;
        Map<String, String> formData;
        Map<String, String> keyValuePair;
        String params;
        HttpResponse httpResponse;
        String endUserName = null;
        String endUserPassword = null;
        // Switch off the Meta Data Check Box
        try {
            fileUtils = new FileUtils();
            formData = new LinkedHashMap<>();
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/RoleGroupMetadataAutoExtractionCheckboxOff.txt", ":", "RoleGroup");
            for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                formData.put(m.getKey().trim(), m.getValue().trim());
            }
            params = UrlEncodedString.getUrlEncodedString(formData);
            httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");

            // Login to end user
       /* String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
        loginResponse = check.hitCheck(endUserName, endUserPassword);
        softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");

        String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"fileuploadpath");
        String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "fileuploadname");

        int contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath,contractCreationConfigFileName,entity,templateFilePath,templateFileName,relationId,true);
*/
            /////////////////////
            // Login to Client Admin
            loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is not valid");
            // Switch On the Meta Data Check Box
            fileUtils = new FileUtils();
            formData = new LinkedHashMap<>();
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/RoleGroupMetadataAutoExtractionCheckboxOn.txt", ":", "RoleGroup");
            for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                formData.put(m.getKey().trim(), m.getValue().trim());
            }
            params = UrlEncodedString.getUrlEncodedString(formData);
            httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");

            // Switch Off Auto Extraction Listing
            fileUtils = new FileUtils();
            formData = new LinkedHashMap<>();
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/RoleGroupAutomationListingOff.txt", ":", "RoleGroup");
            for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                formData.put(m.getKey().trim(), m.getValue().trim());
            }
            params = UrlEncodedString.getUrlEncodedString(formData);
            httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");

            // Login to End User
            endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
            endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
            loginResponse = check.hitCheck(endUserName, endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");
            // Verify Auto Extraction Listing
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse autoExtractionListingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            softAssert.assertTrue(autoExtractionListingResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String autoExtractionListingResponseStr = EntityUtils.toString(autoExtractionListingResponse.getEntity());
            softAssert.assertTrue(!JSONUtility.validjson(autoExtractionListingResponseStr), "Expected not a valid Json as permission for Document Listing is Disabled");

            // Login to Client Admin
            loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is not valid");
            // Switch On Auto Extraction Listing
            fileUtils = new FileUtils();
            formData = new LinkedHashMap<>();
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/RoleGroupAutomationListingOn.txt", ":", "RoleGroup");
            for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                formData.put(m.getKey().trim(), m.getValue().trim());
            }
            params = UrlEncodedString.getUrlEncodedString(formData);
            httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");

            // Login to End- User
            loginResponse = check.hitCheck(endUserName, endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");
            // Verify Auto Extraction Listing
            autoExtractionListingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            softAssert.assertTrue(autoExtractionListingResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            autoExtractionListingResponseStr = EntityUtils.toString(autoExtractionListingResponse.getEntity());
            softAssert.assertTrue(JSONUtility.validjson(autoExtractionListingResponseStr), "Expected a valid Json as permission for Document Listing is Enabled");
        }
        catch (Exception e){
            logger.error("Error while changing permission from client admin " + e.getStackTrace());
        }
        finally {
            // Login to Client Admin
            loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is not valid");

            // Switch On Meta Data Check Box
            fileUtils = new FileUtils();
            formData = new LinkedHashMap<>();
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/RoleGroupMetadataAutoExtractionCheckboxOn.txt", ":", "RoleGroup");
            for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                formData.put(m.getKey().trim(), m.getValue().trim());
            }
            params = UrlEncodedString.getUrlEncodedString(formData);
            httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");

            // Switch On Auto Extraction Listing
            fileUtils = new FileUtils();
            formData = new LinkedHashMap<>();
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/RoleGroupAutomationListingOn.txt", ":", "RoleGroup");
            for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                formData.put(m.getKey().trim(), m.getValue().trim());
            }
            params = UrlEncodedString.getUrlEncodedString(formData);
            httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");

            loginResponse = check.hitCheck(endUserName, endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");
        }
        softAssert.assertAll();
    }

    /*
    * 1). Test Case to check Auto Extraction functionality for CSV Zip Download
    * 2). Download CSV as per extraction type
    * */
   @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = false)
    public void downloadCSVAPI() throws Exception {
        softAssert = new SoftAssert();
        Check check = new Check();
        APIUtils apiUtils = new APIUtils();
        String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
        HttpResponse loginResponse = check.hitCheck(endUserName, endUserPassword);
        softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");

        String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"fileuploadpath");
        String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "fileuploadname");

        int contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath,contractCreationConfigFileName,entity,templateFilePath,templateFileName,relationId,true);
        String apiPath =ContractShow.getAPIPath();
        apiPath=String.format(apiPath + contractId);
        HttpGet httpGet = new HttpGet(apiPath);
        httpGet.addHeader("Content-Type","application/json;charset=UTF-8");
        httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        HttpResponse newlyCreatedContractShowResponse =APIUtils.getRequest(httpGet);
        String newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());

        jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);

        docName= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("name");
        docId= (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
        fileExtension= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("extension");

        if(ConfigureEnvironment.environment.contains("sandbox")){
            boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(endUserName,endUserPassword);
            softAssert.assertTrue(isExtractionCompleted,"Extraction not Completed");
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson =  new JSONObject(listingResponseStr);
            Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
                for (String key : keys) {
                    if(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("documentname")){
                        docId = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1].trim());
                        break;
                    }
                }
        }
        else {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
            // Get Document Id from contract tree
            String query = "SELECT id FROM contract_document WHERE contract_id ="+ contractId + ";";
            List<List<String>> documentData = postgreSQLJDBC.doSelect(query);
            int contractTreeDocId = Integer.valueOf(documentData.get(0).get(0));

            // Verify Scheduler status and get document id from scheduler table
            query = "SELECT  id,status_id FROM autoextraction_document_request WHERE document_id = "+contractTreeDocId+";";
            List<List<String>> schedulerData = postgreSQLJDBC.doSelect(query);
            int statusId = Integer.valueOf(schedulerData.get(0).get(1));
            docId = Integer.valueOf(schedulerData.get(0).get(0));

            LocalTime initialTime = LocalTime.now();
            while(statusId !=2){
                logger.info("Scheduler hasn't picked yet " + statusId);
                schedulerData = postgreSQLJDBC.doSelect(query);
                statusId = Integer.valueOf(schedulerData.get(0).get(1));

                if (statusId == 3) {
                    throw new SkipException("Document will not be picked by scheduler as task is failed");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Scheduler to pick the document for extraction = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Waited for 10 minutes for Scheduler to pick the document." +
                            "Please look manually whether their is problem in Scheduler to pick the document." +
                            "For document id " + docId + " in Automation Listing and document id " + contractTreeDocId + " in Contract Tree");
                }
            }

            // Get extraction status from extraction status table
            query = "SELECT ads.document_id, min(extraction_status) as status  FROM  autoextraction_document_status ads where ads.document_id=" + docId + " GROUP BY ads.document_id";
            List<List<String>> documentStatusData = postgreSQLJDBC.doSelect(query);

            int documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));

            initialTime = LocalTime.now();
            while (documentExtractionStatus != 4) {
                documentStatusData = postgreSQLJDBC.doSelect(query);

                documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
                if (documentExtractionStatus == 1) {
                    logger.info("Document is submitted for Auto-Extraction");
                } else if (documentExtractionStatus == 2) {
                    logger.info("Document is in pre-processing stage");
                } else if (documentExtractionStatus == 3) {
                    logger.info("Document is in post-processing stage");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Extraction to complete Wait Time = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Extraction is working slow already waited for 10 minutes." +
                            "Please look manually whether their is problem in extraction or services are working slow." +
                            "For document id " + docId);
                }
            }
            softAssert.assertTrue(documentExtractionStatus == 4, "Extraction is not completed for the document");
        }
        // Download Zip
        HttpGet httpGetTarget = apiUtils.generateHttpGetRequestWithQueryString("/autoExtraction/downloadCsvZip/"+clientId+"/" + docId,"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        httpGetTarget.addHeader("content-type","multipart/form-data; boundary=--------------------------796410996310866869687797");
        httpGetTarget.addHeader("content-length","0");
        HttpResponse response = apiUtils.downloadAPIResponseFile("src/test/resources/TestConfig/AutoExtraction/AutoExtractionDownloadedFiles/ZipFileDownloadsForAutoExtraction/"+clientId+"_" + docId + ".zip",aeHost,httpGetTarget);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
                "DownloadZip API is not able to download the zip for a provided docId");

        String zipFileDirectory = "src/test/resources/TestConfig/AutoExtraction/AutoExtractionDownloadedFiles/ZipFileDownloadsForAutoExtraction";
        apiUtils.unzip(zipFileDirectory, clientId+"_" + docId + ".zip",zipFileDirectory);

        String contractExtractedFile = docName + "_" + "Contract Metadata ExtractionOutput.csv";
        List<Map<String,String>> contractCsvData = AutoExtractionHelper.readFromCSV(zipFileDirectory + "/" + contractExtractedFile);

        contractCsvData = contractCsvData.stream().filter(m->!m.get("Metadata Value").isEmpty()).collect(Collectors.toList());
        // Verify Contracts Auto-Extraction Listing Data
        String contractListingPayload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"367\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":367,\"filterName\":\"fieldId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"entityId\":"+ docId +"}";
        HttpResponse listingResponse  = AutoExtractionHelper.getListingData("/listRenderer/list/433/listdata",contractListingPayload);
        String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
        softAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");

        softAssert.assertTrue(JSONUtility.validjson(listingResponseStr),"Not a valid Json means contract extraction is not happened");
        JSONObject listingResponseJsonStr = new JSONObject(listingResponseStr);
        softAssert.assertTrue(listingResponseJsonStr.getJSONArray("data").length()>1,"Extracted Data is not present");

        int numberOfExtractedObligation = listingResponseJsonStr.getJSONArray("data").length();
        int recordToPick = RandomNumbers.getRandomNumberWithinRangeIndex(0,numberOfExtractedObligation);
        List<String> columns = listingResponseJsonStr.getJSONArray("data").getJSONObject(recordToPick).keySet().stream().collect(Collectors.toList());

        Map<String,String> extractionRecord = new LinkedHashMap<>();
        for(int i=0;i<columns.size();i++){
            extractionRecord.put(listingResponseJsonStr.getJSONArray("data").getJSONObject(recordToPick).getJSONObject(columns.get(i)).get("columnName").toString().trim(),
                    listingResponseJsonStr.getJSONArray("data").getJSONObject(recordToPick).getJSONObject(columns.get(i)).get("value").toString().trim());
        }

        boolean extractedTextMatched = false;
        boolean pageNumberMatched = false;
        for(int i=0;i<contractCsvData.size();i++){
                if(contractCsvData.get(i).get("Metadata Value").replaceAll("\\|","").trim().contains(extractionRecord.get("extractedtext"))){
                    extractedTextMatched= true;
                    if(contractCsvData.get(i).get("Page Number").trim().equals(extractionRecord.get("pageno"))){
                        pageNumberMatched = true;
                        break;
                    }
                }
        }
        softAssert.assertTrue(extractedTextMatched && pageNumberMatched,"One Record is Matched in CSV with UI");

        String obligationExtractedFile = docName + "_" + "Obligation ExtractionOutput.csv";
        List<Map<String,String>> obligationCsvData = AutoExtractionHelper.readFromCSV(zipFileDirectory + "/" + obligationExtractedFile);
        obligationCsvData = obligationCsvData.stream().filter(m->m.get("Category").length()>0).collect(Collectors.toList());

        softAssert.assertTrue(obligationCsvData.size()>1,"CSV for Obligation Meta-Data is coming empty.");
        // Verify Obligation Auto-Extraction Listing Data
        String obligationListingPayload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"367\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":367,\"filterName\":\"fieldId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"entityId\":"+docId+"}";
        listingResponse  = AutoExtractionHelper.getListingData("/listRenderer/list/437/listdata",obligationListingPayload);
        listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
        softAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");

        softAssert.assertTrue(JSONUtility.validjson(listingResponseStr),"Not a valid Json means contract extraction is not happened");
        listingResponseJsonStr = new JSONObject(listingResponseStr);
        softAssert.assertTrue(listingResponseJsonStr.getJSONArray("data").length()>1,"Extracted Data is not present");

        numberOfExtractedObligation = listingResponseJsonStr.getJSONArray("data").length();
        recordToPick = RandomNumbers.getRandomNumberWithinRangeIndex(0,numberOfExtractedObligation);
        columns = listingResponseJsonStr.getJSONArray("data").getJSONObject(recordToPick).keySet().stream().collect(Collectors.toList());

        extractionRecord = new LinkedHashMap<>();
        for(int i=0;i<columns.size();i++){
            extractionRecord.put(listingResponseJsonStr.getJSONArray("data").getJSONObject(recordToPick).getJSONObject(columns.get(i)).get("columnName").toString().trim(),
                    listingResponseJsonStr.getJSONArray("data").getJSONObject(recordToPick).getJSONObject(columns.get(i)).get("value").toString().trim());
        }

        AutoExtractionHelper.deleteAllFilesFromDirectory(zipFileDirectory);

        // Download CSV Zip File for contracts and obligation auto-extraction
        httpGetTarget = apiUtils.generateHttpGetRequestWithQueryString("/autoExtraction/downloadCSV/"+ clientId +"/"+ docId +"/" + 1001,"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        httpGetTarget.addHeader("content-type","multipart/form-data; boundary=--------------------------796410996310866869687797");
        httpGetTarget.addHeader("content-length","0");
        response = apiUtils.downloadAPIResponseFile("src/test/resources/TestConfig/AutoExtraction/AutoExtractionDownloadedFiles/ZipFileDownloadsForAutoExtraction/"+ clientId +"_" +docId + fileExtension,aeHost,httpGetTarget);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
                "Download CSV API is not able to download the csv for a provided docId");

        httpGetTarget = apiUtils.generateHttpGetRequestWithQueryString("/autoExtraction/downloadCSV/"+ clientId +"/"+ docId +"/" + 1002,"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        httpGetTarget.addHeader("content-type","multipart/form-data; boundary=--------------------------796410996310866869687797");
        httpGetTarget.addHeader("content-length","0");
        response = apiUtils.downloadAPIResponseFile("src/test/resources/TestConfig/AutoExtraction/AutoExtractionDownloadedFiles/ZipFileDownloadsForAutoExtraction/"+ clientId +"_" +docId + fileExtension,aeHost,httpGetTarget);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
                "Download CSV API is not able to download the csv for a provided docId");

        AutoExtractionHelper.deleteAllFilesFromDirectory(zipFileDirectory);
        EntityOperationsHelper.deleteEntityRecord("contracts",contractId);
        softAssert.assertAll();
    }


    /*
    * Test Case to test all the access related tasks
    * 1). User Role Group
    * 2). Access Criteria
    * 3). System Access
    * 4). Supplier Access
    * */
    @Parameters("Environment")
    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = false)
    public void TestDocumentAccess(String environment) {
        // Remove System Access but end-user is stakeholder
        // Expected : User should able to see document in viewer
            if(environment.equals("autoextraction_sandbox")){
                throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
            }
            softAssert = new SoftAssert();
            String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "password");
            Check check = new Check();
            HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is not valid");

            FileUtils fileUtils = new FileUtils();
            Map<String, String> formData = new LinkedHashMap<>();
            Map<String, String> keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/Access/SystemAccessOff.txt", ":", "Access");

            for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                formData.put(m.getKey().trim(), m.getValue().trim());
            }

            String params = UrlEncodedString.getUrlEncodedString(formData);
            String endUserName = null;
            String endUserPassword = null;
            try {
            HttpResponse httpResponse = AutoExtractionHelper.updateAccessCriteria("/access-criteria/update", params);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");

            // Auto- Extraction with user as a stakeholder
            endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
            endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
            loginResponse = check.hitCheck(endUserName, endUserPassword);

            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");

            String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "fileuploadpath");
            String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "fileuploadname");

            int contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath,contractCreationConfigFileName,entity,templateFilePath,templateFileName,relationId,true);
            String apiPath =ContractShow.getAPIPath();
            apiPath=String.format(apiPath + contractId);
            HttpGet httpGet = new HttpGet(apiPath);
            httpGet.addHeader("Content-Type","application/json;charset=UTF-8");
            httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            HttpResponse newlyCreatedContractShowResponse =APIUtils.getRequest(httpGet);
            String newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());
            jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);

            docName = (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("name");
            docId = (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
            fileExtension = (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("extension");

            if(ConfigureEnvironment.environment.contains("sandbox")){
                    boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(endUserName,endUserPassword);
                    softAssert.assertTrue(isExtractionCompleted,"Extraction not Completed");
                String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
                String payload = "{\"filterMap\":{}}";
                HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
                String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
                JSONObject listingResponseJson =  new JSONObject(listingResponseStr);
                Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
                for (String key : keys) {
                    if(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("documentname")){
                        docId = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1].trim());
                        break;
                    }
                }
            }
            else {
                PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
                // Get Document Id from contract tree
                String query = "SELECT id FROM contract_document WHERE contract_id ="+ contractId + ";";
                List<List<String>> documentData = postgreSQLJDBC.doSelect(query);
                int contractTreeDocId = Integer.valueOf(documentData.get(0).get(0));

                // Verify Scheduler status and get document id from scheduler table
                query = "SELECT  id,status_id FROM autoextraction_document_request WHERE document_id = "+contractTreeDocId+";";
                List<List<String>> schedulerData = postgreSQLJDBC.doSelect(query);
                int statusId = Integer.valueOf(schedulerData.get(0).get(1));
                docId = Integer.valueOf(schedulerData.get(0).get(0));

                LocalTime initialTime = LocalTime.now();
                while(statusId !=2){
                    logger.info("Scheduler hasn't picked yet " + statusId);
                    schedulerData = postgreSQLJDBC.doSelect(query);
                    statusId = Integer.valueOf(schedulerData.get(0).get(1));

                    if (statusId == 3) {
                        throw new SkipException("Document will not be picked by scheduler as task is failed");
                    }
                    LocalTime finalTime = LocalTime.now();
                    Duration duration = Duration.between(initialTime, finalTime);
                    logger.info("Waiting for Scheduler to pick the document for extraction = " + duration.getSeconds());

                    if (duration.getSeconds() > 600) {
                        throw new SkipException("Waited for 10 minutes for Scheduler to pick the document." +
                                "Please look manually whether their is problem in Scheduler to pick the document." +
                                "For document id " + docId + " in Automation Listing and document id " + contractTreeDocId + " in Contract Tree");
                    }
                }

                // Get extraction status from extraction status table
                query = "SELECT ads.document_id, min(extraction_status) as status  FROM  autoextraction_document_status ads where ads.document_id=" + docId + " GROUP BY ads.document_id";
                List<List<String>> documentStatusData = postgreSQLJDBC.doSelect(query);

                int documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));

                initialTime = LocalTime.now();
                while (documentExtractionStatus != 4) {
                    documentStatusData = postgreSQLJDBC.doSelect(query);

                    documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
                    if (documentExtractionStatus == 1) {
                        logger.info("Document is submitted for Auto-Extraction");
                    } else if (documentExtractionStatus == 2) {
                        logger.info("Document is in pre-processing stage");
                    } else if (documentExtractionStatus == 3) {
                        logger.info("Document is in post-processing stage");
                    }
                    LocalTime finalTime = LocalTime.now();
                    Duration duration = Duration.between(initialTime, finalTime);
                    logger.info("Waiting for Extraction to complete Wait Time = " + duration.getSeconds());

                    if (duration.getSeconds() > 600) {
                        throw new SkipException("Extraction is working slow already waited for 10 minutes." +
                                "Please look manually whether their is problem in extraction or services are working slow." +
                                "For document id " + docId);
                    }
                }
                softAssert.assertTrue(documentExtractionStatus == 4, "Extraction is not completed for the document");
            }

            HttpResponse docViewerResponse = AutoExtractionHelper.hitShowViewerAPI("/documentviewer/show/" + docId);
            String docViewerResponseStr = EntityUtils.toString(docViewerResponse.getEntity());
            softAssert.assertTrue(docViewerResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            softAssert.assertTrue(docViewerResponseStr.contains("\"response\":{\"type\":\"basic\",\"status\":\"success\"}"), "Viewer is not visible which is not expected");

            // Auto- Extraction with user not as a stakeholder
            endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials without stakeholder", "username");
            endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials without stakeholder", "password");
            loginResponse = check.hitCheck(endUserName, endUserPassword);

            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");

            docViewerResponse = AutoExtractionHelper.hitShowViewerAPI("/documentviewer/show/" + docId);
            docViewerResponseStr = EntityUtils.toString(docViewerResponse.getEntity());
            softAssert.assertTrue(docViewerResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            softAssert.assertTrue(docViewerResponseStr.contains("\"errorMessage\":\"Either you do not have the required permissions or requested page does not exist anymore.\""), "Viewer is visible which is not expected");
        } catch (Exception e){
                logger.error("Exception occurred while verifying document access functionality " + e.getStackTrace());
             }
        finally {
            // Resetting System Access
            loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is not valid");
            fileUtils = new FileUtils();
            formData = new LinkedHashMap<>();
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/Access/SystemAccessOn.txt", ":", "Access");

            for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                formData.put(m.getKey().trim(), m.getValue().trim());
            }
            params = UrlEncodedString.getUrlEncodedString(formData);
            HttpResponse httpResponse = AutoExtractionHelper.updateAccessCriteria("/access-criteria/update", params);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");

            // Login to End user
            loginResponse = check.hitCheck(endUserName,endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is not valid");
        }
        softAssert.assertAll();
    }


    @Parameters("Environment")
    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = false)
    public void TestSupplierAccess(String environment) throws IOException {
        // Remove System Access And Supplier On
        // Expected : User should able to see document in viewer
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        Check check = new Check();
        String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials","username");
        String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"client admin credentials", "password");

        // System Access Off And Supplier Access Off
        HttpResponse loginResponse = check.hitCheck(clientAdminUserName,clientAdminPassword);
        softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() ==302,"Response is not valid");
        FileUtils fileUtils = new FileUtils();
        Map<String,String> formData = new LinkedHashMap<>();
        Map<String,String> keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/Access/SystemAccessOff.txt",":","Access");

        for(Map.Entry<String,String> m: keyValuePair.entrySet()){
            formData.put(m.getKey().trim(),m.getValue().trim());
        }
        String params = UrlEncodedString.getUrlEncodedString(formData);

        HttpResponse httpResponse = AutoExtractionHelper.updateAccessCriteria("/access-criteria/update",params);
        softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302,"Response Code is not valid");

        // Login to End- User
        String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
        loginResponse = check.hitCheck(endUserName, endUserPassword);
        softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");
        // Verify Auto Extraction Listing
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
        String payload = "{\"filterMap\":{}}";
        HttpResponse autoExtractionListingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
        softAssert.assertTrue(autoExtractionListingResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
        String autoExtractionListingResponseStr = EntityUtils.toString(autoExtractionListingResponse.getEntity());
        softAssert.assertTrue(Jsoup.isValid(autoExtractionListingResponseStr, Whitelist.basic()),"Response Should not be Json as permission is denied from client admin");

        softAssert.assertAll();
    }

    /*
    * Test Case for Image Auto Extraction for future References
    * */
    // For Future Reference for now it is enabled as false
   @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = false)
    public void TestImageAutoExtraction() throws InterruptedException, SQLException, IOException {
        softAssert = new SoftAssert();
        String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"image document attachment", "fileuploadpath");
        String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "image document attachment","fileuploadname");

        int contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath,contractCreationConfigFileName,entity,templateFilePath,templateFileName,relationId,true);
        String apiPath =ContractShow.getAPIPath();
        apiPath=String.format(apiPath + contractId);
        APIValidator newlyCreatedContractShowResponse = executor.get(hostUrl,apiPath,headers);

        jsonObject = new JSONObject(newlyCreatedContractShowResponse.getResponse().getResponseBody());

        docName= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("name");
        docId= (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
        fileExtension= (String) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("extension");

        // Download uploaded Document with contract from sirion app server
        FileUtils.getFileFromSFTPServer(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"sirion environment sftp","hostname"),
                Integer.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"sirion environment sftp","port")),
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"sirion environment sftp","username"),
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"sirion environment sftp","password"),
                "/data/contracts/"+clientId+"/1267/"+ entityId +"/" + contractId +"/Files" + docId +"/true1/"+ docName +"."+ fileExtension,
                String.format("src/test/output/AutoExtractionDownloadedFiles/SirionAppsDownloadedFiles/"+docName +"."+ fileExtension));

        Thread.sleep(5000);
        List<String> latestDataFromKafka = AutoExtractionHelper.runConsumer("192.168.2.174:9092","asynctextract");
        boolean isDataStoredInKafka = false;
        for(String data:latestDataFromKafka){
            if(data.trim().contains(String.valueOf(docId))){
                isDataStoredInKafka = true;
                break;
            }
        }
        softAssert.assertTrue(isDataStoredInKafka,"Data is not there in kafka associated with docId = " + docId);

        // verify whether File is saved into database on Doc Save API hit - DocumentInfo Collection
        List<Document> uploadedDocs = connection.getDBResponse("AE_CA_POD_2","DocumentInfo","docid",docId);

        softAssert.assertTrue(uploadedDocs.size() ==1,"Document is not saved in database");

        if(uploadedDocs.size()!=1){
            throw new SkipException("Scheduler is not up document is not getting saved to document info collection");
        }

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost,postgresPort,postgresDbName,postgresDbUsername,postgresDbPassword);
        String dbQuery= "SELECT ads.document_id, min(extraction_status) as status  FROM  autoextraction_document_status ads where ads.document_id="+ docId+" GROUP BY ads.document_id";
        List<List<String>> documentStatusData = postgreSQLJDBC.doSelect(dbQuery);

        int documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
        LocalTime initialTime = LocalTime.now();
        while(documentExtractionStatus!=4){
            documentStatusData = postgreSQLJDBC.doSelect(dbQuery);

            documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
            if(documentExtractionStatus==1){
                logger.info("Document is submitted for Auto-Extraction");
            }
            else if(documentExtractionStatus==2){
                logger.info("Document is in pre-processing stage");
            }
            else if(documentExtractionStatus==3){
                logger.info("Document is in post-processing stage");
            }
            LocalTime finalTime = LocalTime.now();
            Duration duration =  Duration.between(initialTime,finalTime);

            if(duration.getSeconds() > 600){
                throw new SkipException("Extraction is working slow already waited for 10 minutes." +
                        "Please look manually whether their is problem in extraction or services are working slow." +
                        "For document id " + docId);
            }
        }

        softAssert.assertTrue(documentExtractionStatus==4,"Extraction is not completed for the document");
        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","DocumentParserParamLinking","docid",docId);

        Document docParserData = docParserParamLinkingData.stream().findFirst().get();
        String initialJobId = docParserData.get("_id").toString();
        String paramDocLinkId = docParserData.get("paramdoclinkid").toString();
        String textCount = docParserData.get("textcount").toString();
        String parseParamId = docParserData.get("parserparamid").toString();
        String status = docParserData.get("status").toString();
        JSONObject extractionStatusPerTypeJson = new JSONObject(docParserData.toJson());
        String extractionTypeStatus1 = extractionStatusPerTypeJson.getJSONArray("extractionstatusperettype").getJSONObject(0).get("status").toString();
        String extractionTypeStatus2 = extractionStatusPerTypeJson.getJSONArray("extractionstatusperettype").getJSONObject(1).get("status").toString();

        String query = "/autoExtraction/saveParsingJobInfo";
        String payload ="{\"clientId\":"+clientId+",\"jobId\":"+ "\""+initialJobId +"\"" +",\"docParamLinkId\":"+paramDocLinkId+",\"documentId\":"+ docId +",\"status\":"+ status+",\"textCount\":"+textCount+",\"parserParamId\":"+parseParamId+",\"extractionTypes\":[1001,1002],\"extractionstatusperettype\":[{\"extractiontype\":1001,\"status\":" + extractionTypeStatus1 + "},{\"extractiontype\":1002,\"status\":" + extractionTypeStatus2 + "}]}";
        HttpResponse saveParseInfoResponse = AutoExtractionHelper.saveParsingJobInfoAPI(aeHost,query,payload);
        softAssert.assertTrue(saveParseInfoResponse.getStatusLine().getStatusCode() ==200,"Response Code is not valid");

        HttpResponse fetchDocumentInfoResponse = AutoExtractionHelper.getfetchDocumentInfo(aeHost,"/autoExtraction/fetchDocumentInfo/" + initialJobId);
        String fetchDocumentInfoResponseStr = EntityUtils.toString(fetchDocumentInfoResponse.getEntity());
        softAssert.assertTrue(JSONUtility.validjson(fetchDocumentInfoResponseStr),"Response is not a valid JSON");
        softAssert.assertTrue(fetchDocumentInfoResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        softAssert.assertAll();
    }

    /*Test Case to Validate the count of projects in User Role Group and on Project Listing*Story Id : AE-974*/
    @Test(dependsOnMethods = {"TestServiceCheckAPI"})
    public void testProjectAccess() throws IOException
    {
        logger.info("Test Case Id:" + "C129171");
        softAssert = new SoftAssert();
        try
        {
            logger.info("Login to Client Admin");
            Check check = new Check();
            //Login to Client Admin
            String clientAdminUsername=ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                    "client admin credentials","username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                    "client admin credentials","password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUsername,clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode()==302,"Response Code is Invalid");

            //Verify that all the projects that are present on Project listing should be there in User Role Group
            String url = "/masteruserrolegroups/update/";
            String roleGroupId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"rolegroupid");
            HttpResponse masterUserRoleGroupResponse = AutoExtractionHelper.hitMasterUserRoleGroup(url,roleGroupId);
            softAssert.assertTrue(masterUserRoleGroupResponse.getStatusLine().getStatusCode()==200,"Master Role Group API Response is not valid");
            String masterUserRoleGroupResponseStr = EntityUtils.toString(masterUserRoleGroupResponse.getEntity());

            org.jsoup.nodes.Document document = Jsoup.parse(masterUserRoleGroupResponseStr);
            int projectCount=document.getElementById("_autoExtractionProjectIds_id").children().size();

            //Getting logged In with end user to check count of projects on Projects listing
            // Login to End User
            String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
            String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
            loginResponse = check.hitCheck(endUserName, endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");

            //Hitting Project Listing API
            String listDataQuery = "/listRenderer/list/441/listdata?version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse projectListDataResponse = AutoExtractionHelper.projectListDataAPI(listDataQuery,payload);
            softAssert.assertTrue(projectListDataResponse.getStatusLine().getStatusCode() == 200, "Project List Data Response Code is not valid");
            String projectListDataResponseStr = EntityUtils.toString(projectListDataResponse.getEntity());

            softAssert.assertTrue(APIUtils.validJsonResponse(projectListDataResponseStr),"Project List Data is not a valid Json");
            JSONObject projectListDataResponseJson = new JSONObject(projectListDataResponseStr);
            int finalFilteredCount = Integer.valueOf(projectListDataResponseJson.get("filteredCount").toString());

            softAssert.assertTrue(finalFilteredCount==projectCount,"Project count mismatch on Project listing and in User Role Group");
            softAssert.assertAll();


        }
        catch(Exception e)
        {
            logger.info("Exception while hitting UserRoleGroup API");
        }
    }

    /*Test Case To validate user is accessible to only that project for which access has been given*/
    @Test(dependsOnMethods = {"TestServiceCheckAPI"})
    public void testProjectAccessOnListing() throws IOException
    {
        try
        {
            logger.info("Login to Client Admin");
            Check check = new Check();
            //Login to Client Admin
            String clientAdminUsername=ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                    "client admin credentials","username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                    "client admin credentials","password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUsername,clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode()==302,"Response Code is Invalid");

            //Hitting Master User Role Group API
            String url = "/masteruserrolegroups/update/";
            String roleGroupId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"rolegroupid");
            HttpResponse masterUserRoleGroupResponse = AutoExtractionHelper.hitMasterUserRoleGroup(url,roleGroupId);
            softAssert.assertTrue(masterUserRoleGroupResponse.getStatusLine().getStatusCode()==200,"Master Role Group API Response is not valid");
            String masterUserRoleGroupResponseStr = EntityUtils.toString(masterUserRoleGroupResponse.getEntity());

            org.jsoup.nodes.Document document = Jsoup.parse(masterUserRoleGroupResponseStr);
            List<String> projectNames = document.getElementById("_autoExtractionProjectIds_id").children().eachText();
            softAssert.assertTrue(projectNames.contains("Default_all"),"Default project access is not being given to user");
            softAssert.assertAll();

        }
        catch (Exception e)
        {
            logger.info("Exception while hitting UserRoleGroup API");
        }
    }

    /*******CDR Linking with AE****(Story Id:CA-1156******/
    /* Test Case Id: C90916--> Test Case to validate if value of field Autoextraction allowed is true then document uploaded via
    Contract Document tab of CDR should get successfully added in AE doc listing*/
    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled=false)
    public void cdrLinkingWithAE() throws IOException
    {
        try
        {
            logger.info("Creating a CDR with Auto-Extraction Allowed flag : True");
            {

                String contractDraftRequestResponseString = ContractDraftRequest.createCDR(parentConfigFilePath, parentConfigFileName, configFilePath, configFileName,
                        "c89079 cdr creation", true);
                int cdrId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);

                //Count of the documents on AutoExtraction Doc Listing before Uploading the Documents
                String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
                String payload = "{\"filterMap\":{}}";
                HttpResponse httpResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
                softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
                String listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
                JSONObject listjsonObject = new JSONObject(listDataResponseStr);
                int initialRecords = Integer.valueOf(listjsonObject.get("filteredCount").toString());
                // File Upload API to get the key of file uploaded
                softAssert = new SoftAssert();
                String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
                File fileToUpload = new File(System.getProperty("user.dir") + "\\src\\test\\resources\\TestConfig\\AutoExtraction\\UploadFiles\\KAMADALTD_DRSADraftR_3252013.docx");

                // Upload a file in Contract Document Tab of CDR
                String fileUploadDraftResponse = PreSignatureHelper.fileUploadDraftWithNewDocument("KAMADALTD_DRSADraftR_3252013","docx",randomKeyForFileUpload,"160",String.valueOf(cdrId),fileToUpload);
                ShowHelper showHelper = new ShowHelper();
                String showResponse = showHelper.getShowResponseVersion2(160, cdrId);
                JSONObject jsonObject = new JSONObject(showResponse);
                jsonObject = jsonObject.getJSONObject("body").getJSONObject("data");
                String commentsPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null," +
                        "\"multiEntitySupport\":false,\"values\":null},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409," +
                        "\"multiEntitySupport\":false,\"values\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false,\"values\":null}," +
                        "\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":null,\"multiEntitySupport\":false,\"values\":null}," +
                        "\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false,\"values\":true},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243," +
                        "\"multiEntitySupport\":false,\"values\":null},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242," +
                        "\"multiEntitySupport\":false,\"values\":null},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null," +
                        "\"multiEntitySupport\":false,\"values\":null},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false," +
                        "\"values\":null},\"commentDocuments\":{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":null,\"documentSize\":50728," +
                        "\"key\":\""+randomKeyForFileUpload+"\",\"documentStatusId\":2,\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false}," +
                        "\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false}]}}";

                jsonObject.put("comment", new JSONObject(commentsPayload));
                String submitDraftPayload ="{\"body\":{\"data\":"+ jsonObject.toString() +"}}";
                HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
                JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);

                // submit file in Contract Document Tab of CDR
                HttpResponse submitFileDraftResponse =  PreSignatureHelper.submitFileDraft(submitDraftPayload);
                String getStatus = EntityUtils.toString(submitFileDraftResponse.getEntity());
                JSONObject jobj = new JSONObject(getStatus);
                String newStatus = jobj.getJSONObject("header").getJSONObject("response").get("status").toString();
                String expectedStatus = "success";
                if(expectedStatus.equals(newStatus)){
                    logger.info("File upload successfully!");
                }else{
                    csAssert.assertTrue(false, "File upload failed!");

                }
                // Hit Automation Listing API to get number of records after uploading the documents
                payload = "{\"filterMap\":{}}";
                httpResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
                softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
                listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
                listjsonObject = new JSONObject(listDataResponseStr);

                int finalRecords = Integer.valueOf(listjsonObject.get("filteredCount").toString());

                softAssert.assertTrue(initialRecords +1 == finalRecords,"Document Uploaded Via Contract Document tab are not getting added in AE Doc listing");
                softAssert.assertAll();
                }
        }
        catch(Exception e)
        {
            logger.info("Exception while hitting CDR creation API");
        }
    }

    /* Test Case Id: C140898-->Test Case to validate when user is not having permission of Field:Auto-Extraction Allowed then document
    should not get added in AE Doc Listing*/
    @Test(priority=0,enabled=false)
    public void aePermsissionOffOnCDR() throws IOException
    {
        try
        {
            logger.info("Creating a CDR with Auto-Extraction Allowed flag : True");
            {

                String contractDraftRequestResponseString = ContractDraftRequest.createCDR(parentConfigFilePath, parentConfigFileName, configAEFlagoffFilePath, configAEFlagoffFileName,
                        "c89079 cdr creation", true);
                int cdrId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);

                //Count of the documents on AutoExtraction Doc Listing before Uploading the Documents
                String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
                String payload = "{\"filterMap\":{}}";
                HttpResponse httpResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
                softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
                String listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
                JSONObject listjsonObject = new JSONObject(listDataResponseStr);
                int initialRecords = Integer.valueOf(listjsonObject.get("filteredCount").toString());
                // File Upload API to get the key of file uploaded
                softAssert = new SoftAssert();
                String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
                File fileToUpload = new File(System.getProperty("user.dir") + "\\src\\test\\resources\\TestConfig\\AutoExtraction\\UploadFiles\\KAMADALTD_DRSADraftR_3252013.docx");
                // upload file
                String fileUploadDraftResponse = PreSignatureHelper.fileUploadDraftWithNewDocument("KAMADALTD_DRSADraftR_3252013","docx",randomKeyForFileUpload,"160",String.valueOf(cdrId),fileToUpload);


                ShowHelper showHelper = new ShowHelper();
                String showResponse = showHelper.getShowResponseVersion2(160, cdrId);
                JSONObject jsonObject = new JSONObject(showResponse);
                jsonObject = jsonObject.getJSONObject("body").getJSONObject("data");
                String commentsPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null," +
                        "\"multiEntitySupport\":false,\"values\":null},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409," +
                        "\"multiEntitySupport\":false,\"values\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false,\"values\":null}," +
                        "\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":null,\"multiEntitySupport\":false,\"values\":null}," +
                        "\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false,\"values\":true},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243," +
                        "\"multiEntitySupport\":false,\"values\":null},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242," +
                        "\"multiEntitySupport\":false,\"values\":null},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null," +
                        "\"multiEntitySupport\":false,\"values\":null},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false," +
                        "\"values\":null},\"commentDocuments\":{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":null,\"documentSize\":50728," +
                        "\"key\":\""+randomKeyForFileUpload+"\",\"documentStatusId\":2,\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false}," +
                        "\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false}]}}";

                jsonObject.put("comment", new JSONObject(commentsPayload));

                String submitDraftPayload ="{\"body\":{\"data\":"+ jsonObject.toString() +"}}";
                HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
                JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);

                // submit file
                HttpResponse submitFileDraftResponse =  PreSignatureHelper.submitFileDraft(submitDraftPayload);
                String getStatus = EntityUtils.toString(submitFileDraftResponse.getEntity());
                JSONObject jobj = new JSONObject(getStatus);
                String newStatus = jobj.getJSONObject("header").getJSONObject("response").get("status").toString();
                String expectedStatus = "success";
                if(expectedStatus.equals(newStatus)){
                    logger.info("File upload successfully!");
                }else{
                    csAssert.assertTrue(false, "File upload failed!");

                }
                // Hit Listing API to get number of records after uploading the documents
                payload = "{\"filterMap\":{}}";
                httpResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
                softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
                listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
                listjsonObject = new JSONObject(listDataResponseStr);

                int finalRecords = Integer.valueOf(listjsonObject.get("filteredCount").toString());

                softAssert.assertTrue(initialRecords +1 == finalRecords,"Document Uploaded Via Contract Document tab are getting added in AE Doc listing");
                softAssert.assertAll();
            }
        }
        catch(Exception e)
        {
            logger.info("Exception while hitting CDR creation API");
        }
    }

    /* Test Case Id:C90919 --> Test Case to validate viewAutoextractedDocuments flag is On data should be accessible on CDR showpage*/
    @Test(dependsOnMethods = {"TestServiceCheckAPI"}, enabled = false)
    public void testViewPermission() throws IOException {
        softAssert = new SoftAssert();
        try {
            logger.info("Checking view extracted text link AutoExtraction API for a particular cdr");
            String multiEntityCDRId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configAutoExtractionFilePath, configAutoExtractionFileName, "cdrId");
            HttpResponse multiEntityShowResponse = AutoExtractionHelper.getViewExtractedTextData(multiEntityCDRId);
            softAssert.assertTrue(multiEntityShowResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid and MultiEntity feature not working properly");
            String multiEntityShowResponseStr = EntityUtils.toString(multiEntityShowResponse.getEntity());
            softAssert.assertTrue(JSONUtility.validjson(multiEntityShowResponseStr), "Response is not a valid JSON");
        }
        catch (Exception e)
        {
            logger.info("Exception while hitting View Extracted Text Data when flag is On -CA-1153");
        }
        softAssert.assertAll();
    }

    /*Test Case Id:C90970-->Test Case to validate when viewAutoextractedDocuments flag is Off data should not be accessible on CDR listing*/
    @Test(enabled = false)
    public void testNoViewPermission() throws IOException {
        softAssert = new SoftAssert();
        try {
            logger.info("Checking view extracted text link AutoExtraction API when there is no access to view extracted text for a cdr");
            String entityCDRId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configAutoExtractionFilePath, configAutoExtractionFileName, "cdr metadata Info","cdrIdNoAccess");
            HttpResponse multiEntityShowResponse = AutoExtractionHelper.getViewExtractedTextData(entityCDRId);
            softAssert.assertTrue(multiEntityShowResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid and MultiEntity feature not working properly");
            String multiEntityShowResponseStr = EntityUtils.toString(multiEntityShowResponse.getEntity());
            softAssert.assertTrue(JSONUtility.validjson(multiEntityShowResponseStr), "Response is not a valid JSON");
        }
        catch (Exception e)
        {
            logger.info("Exception while hitting View Extracted Text API when flag is Off");
        }
        softAssert.assertAll();
    }

     /*Test Case Id : C140924-->Test Case for  auto-extracted data for all the documents associated with a particular CDR -- Contracts Tab Data*/
    @Test(dependsOnMethods = {"testViewPermission"},enabled = false)
    public void viewExtractedTextAllDocs() throws IOException {
        softAssert = new SoftAssert();
        try
        {
            /*Check check = new Check();
            // Login to End User
            String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
            String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
            HttpResponse loginResponse = check.hitCheck(endUserName, endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");*/

            //Verify Contract Metadata Response for All Documents
            String query = "/listRenderer/list/433/listdata?isFirstCall=false";
            String entityCDRId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configAutoExtractionFilePath, configAutoExtractionFileName, "cdrId");
            String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":"+ entityCDRId +",\"parentEntityId\":"+ entityCDRId +",\"parentEntityTypeId\":160}";;
            HttpResponse httpResponse = AutoExtractionHelper.checkDataforAllDocuments(query,payload);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
            String listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
            JSONObject listjsonObjectjson = new JSONObject(listDataResponseStr);
            JSONArray listJsonArray = listjsonObjectjson.getJSONArray("data");
            int sizeOfClauseJsonArray = listJsonArray.length();
            int internalArraySize = AutoExtractionHelper.getSizeOfInternalArray(listJsonArray);
            int expectedCountOfValues = sizeOfClauseJsonArray * internalArraySize;
            int contractValuesReceived = AutoExtractionHelper.getCountOfValuesFromArray(listJsonArray);
            softAssert.assertEquals(contractValuesReceived==expectedCountOfValues, "values extraction for Clause is not as expected");

        } catch (Exception e){
            logger.info("Exception while hitting AutoExtraction Contract Metadata for all Doc Data ");
        }
        softAssert.assertAll();
    }

    /*Test Case Id : C140925-->Test Case for  auto-extracted data for all the documents associated with a particular CDR -- Clause Tab Data*/
    @Test(dependsOnMethods = {"testViewPermission"},enabled =false)
    public void viewExtractedTextAllDocsClause() throws IOException {
        softAssert = new SoftAssert();
        try
        {
           /* Check check = new Check();
            // Login to End User
            String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
            String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
            HttpResponse loginResponse = check.hitCheck(endUserName, endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");*/
            //Verify Contract Metadata Response for All Documents
            String query = "/listRenderer/list/493/listdata?isFirstCall=false";
            String entityCDRId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configAutoExtractionFilePath, configAutoExtractionFileName, "cdrId");
            String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":" + entityCDRId + "}";
            HttpResponse httpResponse = AutoExtractionHelper.checkDataforAllDocuments(query,payload);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
            String listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
            JSONObject listjsonObjectjson = new JSONObject(listDataResponseStr);
            JSONArray listJsonArray = listjsonObjectjson.getJSONArray("data");
            int sizeOfClauseJsonArray = listJsonArray.length();
            softAssert.assertTrue(!(sizeOfClauseJsonArray ==0),"There is no Data to in Clause Tab to compare");
            int internalArraySize = AutoExtractionHelper.getSizeOfInternalArray(listJsonArray);
            int expectedCountOfValues = sizeOfClauseJsonArray * internalArraySize;
            int contractValuesReceived = AutoExtractionHelper.getCountOfValuesFromArray(listJsonArray);
            softAssert.assertEquals(contractValuesReceived==expectedCountOfValues, "values extraction for Clause is not as expected");

        } catch (Exception e){
            logger.info("Exception while hitting AutoExtraction Contract Metadata for all Doc Data ");
        }
        softAssert.assertAll();
    }

    /*Test Case Id : C140926-->Test Case for  auto-extracted data for all the documents associated with a particular CDR -- Contracts Tab Data*/
    @Test(dependsOnMethods = {"testViewPermission"},enabled = false)
    public void viewExtractedTextAllDocsMetadata() throws IOException {
        softAssert = new SoftAssert();
        try
        {
            /*Check check = new Check();
            // Login to End User
            String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
            String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
            HttpResponse loginResponse = check.hitCheck(endUserName, endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");*/
            //Verify Contract Metadata Response for All Documents
            String query = "/listRenderer/list/433/listdata?isFirstCall=false";
            String entityCDRId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configAutoExtractionFilePath, configAutoExtractionFileName, "cdrId");
            String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":"+ entityCDRId +",\"parentEntityId\":"+ entityCDRId +",\"parentEntityTypeId\":160}";;
            //String payload ="{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":2770,\"parentEntityId\":2770,\"parentEntityTypeId\":160}";
            HttpResponse httpResponse = AutoExtractionHelper.checkDataforAllDocuments(query,payload);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
            String listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
            JSONObject listjsonObjectjson = new JSONObject(listDataResponseStr);

            JSONArray listJsonArray = listjsonObjectjson.getJSONArray("data");
            int sizeOfClauseJsonArray = listJsonArray.length();
            int internalArraySize = AutoExtractionHelper.getSizeOfInternalArray(listJsonArray);
            int expectedCountOfValues = sizeOfClauseJsonArray * internalArraySize;
            int contractValuesReceived = AutoExtractionHelper.getCountOfValuesFromArray(listJsonArray);
            softAssert.assertEquals(contractValuesReceived, expectedCountOfValues, "values extraction for Clause is not as expected");

        } catch (Exception e){
            logger.info("Exception while hitting AutoExtraction Contract Metadata for all Doc Data ");
        }
        softAssert.assertAll();
    }

    /*Test Case for  auto-extracted data for all the documents associated with a particular CDR -- Obligation Tab Data*/
    @Test(dependsOnMethods = {"testViewPermission"},enabled = false)
    public void viewExtractedTextAllDocsObligation() throws IOException {
        softAssert = new SoftAssert();
        try
        {
            Check check = new Check();
            // Login to End User
            String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
            String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
            HttpResponse loginResponse = check.hitCheck(endUserName, endUserPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");
            //Verify Contract Metadata Response for All Documents
            String query = "/listRenderer/list/437/listdata?isFirstCall=false";
            String entityCDRId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configAutoExtractionFilePath, configAutoExtractionFileName, "cdrId");
            String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":" + entityCDRId + "}";
            HttpResponse httpResponse = AutoExtractionHelper.checkDataforAllDocuments(query,payload);
            softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"List Data API for Automation Listing Response Code is not valid");
            String listDataResponseStr = EntityUtils.toString(httpResponse.getEntity());
            JSONObject listjsonObjectjson = new JSONObject(listDataResponseStr);
            JSONArray listJsonArray = listjsonObjectjson.getJSONArray("data");
            int sizeOfClauseJsonArray = listJsonArray.length();
            softAssert.assertTrue(!(sizeOfClauseJsonArray ==0),"There is no Data to in Obligation Tab to compare");
            int internalArraySize = AutoExtractionHelper.getSizeOfInternalArray(listJsonArray);
            int expectedCountOfValues = sizeOfClauseJsonArray * internalArraySize;
            int contractValuesReceived = AutoExtractionHelper.getCountOfValuesFromArray(listJsonArray);
            softAssert.assertEquals(contractValuesReceived==expectedCountOfValues, "values extraction for Clause is not as expected");

        } catch (Exception e){
            logger.info("Exception while hitting AutoExtraction Contract Metadata for all Doc Data ");
        }
        softAssert.assertAll();
    }



    /* Test Case to Category creation and mapping of fields with that category for entity type:361,Test Case Id:C90782-----------Story Id: AE-811*/
    @Test(dependsOnMethods = {"TestServiceCheckAPI"})
    public void CategoryCreation() throws IOException {
        softAssert = new SoftAssert();
        try {
            logger.info("Login to Client admin");
            Check check = new Check();
            //Login to Client Admin
            String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                    "client admin credentials", "username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                    "client admin credentials", "password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is invalid");

            //GET API--->Get all the Fields that are not mapped to any Category(Test Case Id:C90782)
            logger.info("Validating create form API to get all the non mappped fields---"+ "Test Case Id:C90782");
            String getCreateFormURL = "/metadataautoextraction/category/createForm";
            HttpResponse metadataFieldResponse = AutoExtractionHelper.getAllNonMappedFields(getCreateFormURL);
            softAssert.assertTrue(metadataFieldResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String metadataFieldResponseStr = EntityUtils.toString(metadataFieldResponse.getEntity());

            JSONObject metadataFieldResponseJson = new JSONObject(metadataFieldResponseStr);
            int metadataFieldsLength = metadataFieldResponseJson.getJSONObject("response").getJSONObject("mappedFields").getJSONArray("option").length();
            HashMap<Integer,String> metadataFields = new LinkedHashMap<>();
            for(int i=0;i<metadataFieldsLength;i++){
                metadataFields.put(Integer.valueOf(metadataFieldResponseJson.getJSONObject("response").getJSONObject("mappedFields").getJSONArray("option").getJSONObject(i).get("id").toString()),metadataFieldResponseJson.getJSONObject("response").getJSONObject("mappedFields").getJSONArray("option").getJSONObject(i).get("name").toString());
            }
            //Picking up the first Field Name and Id to map while creating a new Category
            int fieldId = metadataFields.entrySet().stream().findFirst().get().getKey().intValue();
            String fieldName=metadataFields.entrySet().stream().findFirst().get().getValue().toString();

            if(metadataFields.size()<1){
                throw new SkipException("No Meta Data Fields are there to select in category");
            }


            //Test Case - Verify that a new Category is getting created successfully for entity Type : AutoExtraction (Test Case Id:C90772)
            logger.info("Verify that a category is getting created successfully---" + "Test Case Id:C90772");
            String query = "/metadataautoextraction//category/create";
            String categoryNameNew = "Test Category Name";
            //String categoryNameNew = "Test Category Name" + RandomString.getRandomAlphaNumericString(10);
            String categoryValue = "TestCategoryName"+RandomString.getRandomAlphaNumericString(10);
            String payload ="{\n" +
                    "  \"category\": {\n" +
                    "    \"name\": \""+categoryNameNew+"\",\n" +
                    "    \"value\": \""+categoryValue+"\"\n" +
                    "  },\n" +
                    "  \"extractionType\": {\n" +
                    "    \"id\": 1001,\n" +
                    "    \"value\": {\n" +
                    "      \"name\": \"\",\n" +
                    "      \"id\": 1001\n" +
                    "    },\n" +
                    "    \"type\": \"singleSelect\"\n" +
                    "  },\n" +
                    "  \"mappedFields\": {\n" +
                    "    \"value\": [\n" +
                    "      {\n" +
                    "        \"name\": \""+fieldName+"\",\n" +
                    "        \"id\":"+fieldId+"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"type\": \"multiSelect\"\n" +
                    "  }\n" +
                    "}";

            HttpResponse categoryCreationResponse = AutoExtractionHelper.categoryCreationAPI(query, payload);
            softAssert.assertTrue(categoryCreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String categoryCreationStr = EntityUtils.toString(categoryCreationResponse.getEntity());
            JSONObject createCategoryJson = new JSONObject(categoryCreationStr);

            int newlyCreatedcategoryId = (int) createCategoryJson.getJSONObject("response").getJSONObject("category").get("id");

            //Test Case******Check newly created Category Id is getting added in Category Listing( Test Case Id:C90764,C90777)******
            logger.info("Verify after creating a new AE category it should get reflected in AE Category listing"+ "Test Case Id:C90764");
            String categoryListDataQuery = "/listRenderer/list/509/listdata?version=2.0&isFirstCall=true&contractId&relationId&vendorId&_t=1582615946547";
            payload = "{\"filterMap\": {\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{},\"entityTypeId\":361}}";
            HttpResponse categoryListingResponse = AutoExtractionHelper.categoryListingAPI(categoryListDataQuery, payload);
            softAssert.assertTrue(categoryCreationResponse.getStatusLine().getStatusCode() == 200, "Response Code is invalid");
            String categoryListingStr = EntityUtils.toString(categoryListingResponse.getEntity());
            JSONObject categoryListingJSON = new JSONObject(categoryListingStr);

            int dataLength = categoryListingJSON.getJSONArray("data").length();
            boolean isNewlyAddedCategoryPresent=false;
            if (dataLength >= 1) {
                int columnId = ListDataHelper.getColumnId(categoryListingStr, "category_name");

                List<String> allCategories = new LinkedList<>();
                for (int i = 0; i < dataLength; i++)
                {
                    allCategories.add(categoryListingJSON.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[0]);
                }
                if(allCategories.contains(categoryValue))
                {
                    isNewlyAddedCategoryPresent=true;
                    logger.info("Newly Added Category is Present in Category Listing:" + categoryValue);
                }
                softAssert.assertTrue(!allCategories.contains("categoryValue"),"Newly Created category is not getting added in Category Listing");
            } else
            {
                logger.warn("There is no enough data present to test for column ");
            }

            //Test Case---->Verify that show page of newly created category is getting open (Test Case Id:C91095)
            logger.info("Verify that show page of an API is getting open" + "Test Case Id :C91095");
            String getCategoryShowpageURL = "/metadataautoextraction/category/show/"+ newlyCreatedcategoryId;
            HttpResponse categoryShowResponse = AutoExtractionHelper.categoryShowpage(getCategoryShowpageURL);
            softAssert.assertTrue(categoryShowResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String categoryShowResponseStr = EntityUtils.toString(categoryShowResponse.getEntity());
            JSONObject categoryShowResponseJson = new JSONObject(categoryShowResponseStr);

            //Test Case ---> Verify that an existing category is getting updated (Test Case Id:C90775)
            logger.info("Updating an existing AE category---" + "Test Case Id:C90775");
            String updatedValue= categoryValue + "Update";
            String categoryNameUpdate="AutomationCategory";
            String request ="/metadataautoextraction/category/update";


            String requestBody ="{\n" +
                    "  \"category\": {\n" +
                    "   \"name\": \""+categoryNameUpdate+"\",\n" +
                    "   \"id\":"+newlyCreatedcategoryId+",\n"+
                    "    \"value\": \""+updatedValue+"\"\n" +
                    "  },\n" +
                    "  \"extractionType\": {\n" +
                    "    \"id\": 1001,\n" +
                    "    \"value\": {\n" +
                    "      \"name\": \"\",\n" +
                    "      \"id\": 1001\n" +
                    "    },\n" +
                    "    \"type\": \"singleSelect\"\n" +
                    "  },\n" +
                    "  \"mappedFields\": {\n" +
                    "    \"value\": [\n" +
                    "      {\n" +
                    "        \"name\": \""+fieldName+"\",\n" +
                    "        \"id\":"+fieldId+"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"type\": \"multiSelect\"\n" +
                    "  }\n" +
                    "}";

            HttpResponse categoryUpdateResponse = AutoExtractionHelper.categoryUpdateAPI(query,requestBody);
            softAssert.assertTrue(categoryUpdateResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String categoryUpdateResponseStr=EntityUtils.toString(categoryUpdateResponse.getEntity());
            JSONObject categoryUpdateJson = new JSONObject(categoryUpdateResponseStr);

        }
        catch (Exception e)
        {
            logger.info("Exception while hitting Category creation API");
        }

        softAssert.assertAll();
    }

    /*Test Case for Creating a Category with same name and mapping(Test Case Id:C90775)*/
    @Test(dependsOnMethods = {"TestServiceCheckAPI"})
    public void creatingSameNameCategory() throws  IOException
    {

        softAssert = new SoftAssert();
        try {
            logger.info("Checking Category Creation from Client admin");
            Check check = new Check();
            //Login to Client Admin
            String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                    "client admin credentials", "username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                    "client admin credentials", "password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is invalid");

            logger.info("Updating an existing AE category---" + "Test Case Id:C90775");
            //String updatedValue= categoryValue + "Update";
            String query = "/metadataautoextraction//category/create";
            String categoryNameUpdate="AutomationCategory";
            String categoryValueUpdate = "sameNameValue";

            String payload ="{\n" +
                    "  \"category\": {\n" +
                    "    \"name\": \""+categoryNameUpdate+"\",\n" +
                    "    \"value\": \""+categoryValueUpdate+"\"\n" +
                    "  },\n" +
                    "  \"extractionType\": {\n" +
                    "    \"id\": 1001,\n" +
                    "    \"value\": {\n" +
                    "      \"name\": \"\",\n" +
                    "      \"id\": 1001\n" +
                    "    },\n" +
                    "    \"type\": \"singleSelect\"\n" +
                    "  },\n" +
                    "  \"mappedFields\": {\n" +
                    "    \"value\": [\n" +
                    "      {\n" +
                    "        \"name\": \"Effective Date\",\n" +
                    "        \"id\":12486\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"type\": \"multiSelect\"\n" +
                    "  }\n" +
                    "}";

            logger.info("Creating a Category with the same name---" + "Test Case Id: C90783");
            HttpResponse categoryCreateResponse = AutoExtractionHelper.categoryCreationAPI(query, payload);
            softAssert.assertTrue(categoryCreateResponse.getStatusLine().getStatusCode() == 200, "Some fields have already been mapped");
            String categoryCreateStr = EntityUtils.toString(categoryCreateResponse.getEntity());
            JSONObject createCategoryJson = new JSONObject(categoryCreateStr);
            softAssert.assertTrue(createCategoryJson.get("response").equals("Some fields have already been mapped"), "User is not allowed to create Category/Map with the same name");
        }
        catch (Exception e)
        {
            logger.info("Exception while hitting Category creation API");
        }

    }

    /*Test Case ----> To Validate user is able to create a category without field mapping (Test Case Id:C90808)*/

    @Test(dependsOnMethods = {"TestServiceCheckAPI"})
    public void categoryWithNoFields() throws IOException
    {
        softAssert = new SoftAssert();
        try{
                logger.info("Checking Category Creation from Client admin----" + "Test Case Id : C90808 ");
                Check check = new Check();
                //Login to Client Admin
                String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                        "client admin credentials", "username");
                String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,
                        "client admin credentials", "password");
                HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
                softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response is invalid");

                logger.info("Creating a Category without mapping a field");
                String categoryName = "Automation";
                String categoryValue= "AutomationCategories"+RandomString.getRandomAlphaNumericString(10);
                String query = "/metadataautoextraction//category/create";
                String payload ="{\n" +
                        "        \"category\": \n" +
                        "        {\n" +
                        "            \"name\": \""+categoryName+"\",\n" +
                        "            \"value\": \""+categoryValue+"\"\n" +
                        "        },\n" +
                        "        \"extractionType\": {\n" +
                        "            \"id\":1001 ,\n" +
                        "            \"value\": {\n" +
                        "                \"name\": \"\",\n" +
                        "                \"id\": 1001\n" +
                        "            },\n" +
                        "            \"type\": \"singleSelect\"\n" +
                        "        },\n" +
                        "        \"mappedFields\": {\n" +
                        "            \"value\": [\n" +
                        "               \n" +
                        "            ],\n" +
                        "            \"type\": \"multiSelect\"\n" +
                        "        }\n" +
                        "     }\n";

                HttpResponse categoryWithNoMappingResponse = AutoExtractionHelper.categoryNoField(query,payload);
                softAssert.assertTrue(categoryWithNoMappingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String categoryStr = EntityUtils.toString(categoryWithNoMappingResponse.getEntity());
                JSONObject categoryJson = new JSONObject(categoryStr);

                int CategoryId= Integer.parseInt(categoryJson.getJSONObject("response").getJSONObject("category").get("id").toString());
                logger.info("Category Id of newly created category is:"+ CategoryId);
        }
        catch (Exception e)
        {
            logger.info("Exception while hitting category creation API with no Field mapping");
        }

    }

    /*
     * Test Case for to validate working MultiEntity feature of Auto-Extraction as in Story AE-614
     */
   @Test(dependsOnMethods = {"TestServiceCheckAPI"})
    public void testMultiEntityAE614() throws IOException {
        softAssert = new SoftAssert();
        try {
            logger.info("Checking MultiEntity AutoExtraction API for a particular contract");
            String multiEntityContractId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "multicontractid");
            HttpResponse multiEntityShowResponse = AutoExtractionHelper.getMultiEntityInfo(multiEntityContractId);
            softAssert.assertTrue(multiEntityShowResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid and MultiEntity feature not working properly");
            String multiEntityShowResponseStr = EntityUtils.toString(multiEntityShowResponse.getEntity());
            softAssert.assertTrue(JSONUtility.validjson(multiEntityShowResponseStr), "Response is not a valid JSON");
        } catch (Exception e){
            logger.info("Exception while hitting Multi-Entity AE-614 API" + e.getMessage());
        }
        softAssert.assertAll();
    }

    /*
     * Test Case for MultiEntity auto-extracted data for all the documents associated with a particular contract
     */
    @Test(dependsOnMethods = {"testMultiEntityAE614"},dataProvider = "multiEntityTestDataAll")
    public void testMultiEntityAllDocs(String queryPath, String columnName , String ePayload, int count) throws IOException {
        softAssert = new SoftAssert();
        String payload = ePayload;
        try {
        logger.info("Test to check auto extracted status of all documents of a particular contract");
        HttpResponse multiEntityClauseMetaDataResponse = AutoExtractionHelper.checkMultiEntityDocListingMetadata(queryPath, payload);
        softAssert.assertTrue(multiEntityClauseMetaDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is not Valid");
        String multiEntityClauseMetaDataResponseStr = EntityUtils.toString(multiEntityClauseMetaDataResponse.getEntity());
        softAssert.assertTrue(JSONUtility.validjson(multiEntityClauseMetaDataResponseStr), "Response is not a valid JSON");
        JSONObject multiEntityClauseMetaDataResponseJson = new JSONObject(multiEntityClauseMetaDataResponseStr);
        JSONArray multiEntityClauseMetaDataResponseJsonArray = multiEntityClauseMetaDataResponseJson.getJSONArray("data");

        logger.info("Comparing values by the size of array and each value counted in nested fashion");
        int sizeOfClauseJsonArray = multiEntityClauseMetaDataResponseJsonArray.length();
        int internalArraySize = AutoExtractionHelper.getSizeOfInternalArray(multiEntityClauseMetaDataResponseJsonArray);
        int expectedCountOfValues = sizeOfClauseJsonArray * internalArraySize;
        int contractValuesReceived = AutoExtractionHelper.getCountOfValuesFromArray(multiEntityClauseMetaDataResponseJsonArray);
        softAssert.assertEquals(contractValuesReceived, expectedCountOfValues, "values extraction for Clause is not as expected");

        logger.info("Getting the individual counts of each values repeated in all documents of a contract");
        Map valuesHashMapObj = AutoExtractionHelper.findTotalValueFromJsonArray(multiEntityClauseMetaDataResponseJsonArray, columnName);
        int size = valuesHashMapObj.size();
        softAssert.assertEquals(size, count, "Wrong values extracted for Contract");

        } catch (Exception e){
            logger.info("Exception while hitting Multi-Entity all Doc Data " + e.getMessage());
        }
        softAssert.assertAll();
    }

    /*
     * Test Case for MultiEntity auto-extracted data for a single document associated with a particular contract
     */
   @Test(dependsOnMethods = {"testMultiEntityAE614"},dataProvider = "multiEntityTestDataIndividual" , priority = 3)
    public void testMultiEntitySingleDoc(String queryPath, String columnName , String ePayload, int count) throws IOException {
        softAssert = new SoftAssert();
        String payload = ePayload;
        try {
            logger.info("Test to check auto extracted data of single document");
            HttpResponse multiEntityObligationMetaDataResponse = AutoExtractionHelper.checkMultiEntityDocListingMetadata(queryPath, payload);
            softAssert.assertTrue(multiEntityObligationMetaDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is not Valid");
            String multiEntityObligationMetaDataResponseStr = EntityUtils.toString(multiEntityObligationMetaDataResponse.getEntity());
            softAssert.assertTrue(JSONUtility.validjson(multiEntityObligationMetaDataResponseStr), "Invalid Json");
            JSONObject multiEntityObligationMetaDataResponseJson = new JSONObject(multiEntityObligationMetaDataResponseStr);
            JSONArray multiEntityObligationMetaDataResponseJsonArray = multiEntityObligationMetaDataResponseJson.getJSONArray("data");

            logger.info("Comparing values by the size of array and each value counted in nested fashion");
            int sizeOfJsonArray = multiEntityObligationMetaDataResponseJsonArray.length();
            int internalArraySize = AutoExtractionHelper.getSizeOfInternalArray(multiEntityObligationMetaDataResponseJsonArray);
            int expectedCountOfValues = sizeOfJsonArray * internalArraySize;
            int contractValuesReceived = AutoExtractionHelper.getCountOfValuesFromArray(multiEntityObligationMetaDataResponseJsonArray);
            softAssert.assertEquals(contractValuesReceived, expectedCountOfValues, "values extraction for Obligation is not as expected");

            logger.info("Getting the individual counts of each values repeated in single document");
            Map valuesHashMapObj = AutoExtractionHelper.findTotalValueFromJsonArray(multiEntityObligationMetaDataResponseJsonArray, columnName);
            int size = valuesHashMapObj.size();
            softAssert.assertEquals(size, count, "Incorrect count of values found");
        }
        catch (Exception e){
            logger.info("Exception while hitting Multi-Entity Single Doc Filter Data " + e.getMessage());
        }
        softAssert.assertAll();
    }

    /*
    * Test case for story AE-645 - Derived MetaData API
     */
    @Parameters("Environment")
    @Test(enabled = false)
    public void testMetaDataInsertionForAE645(String environment) {
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        String query = "/autoExtraction/saveFieldMetadata";
        String ruleValues ="[\"INDIA\",\"CHINA\",\"RUSSIA\"]";
        int derivedFromValue = 12493;
        int extractionTypeIdValue = 1001;
        int fieldIdValue = 111;
        int referenceFieldIdValue = 111;
        int clientIdValue = 1020;
        int entityTypeIdValue  = 316;
        String payloadStr = "{\n" +
                "    \"clientId\" : "+clientIdValue+",\n" +
                "    \"fieldId\" : "+fieldIdValue+",\n" +
                "    \"referenceFieldId\" : "+referenceFieldIdValue+",\n" +
                "    \"entityTypeId\" : "+entityTypeIdValue+",\n" +
                "    \"fieldName\" : \"is India or China or Russia Presant\",\n" +
                "    \"active\" : true,\n" +
                "    \"extractionTypeId\" : "+extractionTypeIdValue+",\n" +
                "    \"tagIdentifier\" : \"CTYPE\",\n" +
                "    \"fieldOptions\" : [],\n" +
                "    \"isDerived\" : true,\n" +
                "    \"derivedFrom\" : "+derivedFromValue+",\n" +
                "    \"rules\" :" + ruleValues + "\n" +
                "\t\n" +
                "}";
        HttpResponse getInsertMetaDataResponse = AutoExtractionHelper.getAndCheckMetaDataForAE645(query,payloadStr);
        int statusCodeOfRequest = getInsertMetaDataResponse.getStatusLine().getStatusCode();
        softAssert.assertEquals(statusCodeOfRequest,200,"Wrong status code received");
        if(statusCodeOfRequest == 200) {
            int fieldID = 111;
            boolean isDataAsPerRuleFound = false;
            List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","FieldMetaDataInfo","fieldid",fieldID);
            List<Integer> isDerivedFromList = docParserParamLinkingData.stream().map(document -> (Integer)document.get("derivedfrom")).collect(Collectors.toList());
            if(isDerivedFromList.size() > 0) {
                int isDerivedVal = isDerivedFromList.get(0);
                List<Document> matchedExtractedFieldData = connection.getDBResponse("AE_CA_POD_2", "ExtractedData_1001", "fieldid", isDerivedVal);
                if (matchedExtractedFieldData.size() > 0) {
                    isDataAsPerRuleFound = true;
                }
                softAssert.assertTrue(isDataAsPerRuleFound,"Data not found as per rule in extracted text");
            }
        }
        softAssert.assertAll();
    }

     /*
     * Test case for story AE-645 - Checking saved data API
     */
    @Parameters("Environment")
    @Test(enabled = false)
    public void testSaveMetaDataForAE645(String environment) {
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        if(algoId==null) {
            algoId = "1001";
        }
        String query = "/autoExtraction/saveExtractedInfo/" + algoId;
        String extractedText  = "[\"HONGKONG\", \"RUSSIA\", \"India\", \"USA\", \"SINGAPORE\"]";
        int clientIDValue = 1020;
        int textIdValue = 329;
        int documentId = 6848;
        int status = 3;
        int derivedFromValue = 12493;
        int extractionTypeIdValue = 1001;
        String payloadStr = "{\n" +
                "\t\"clientId\": "+clientIDValue+",\n" +
                "\t\"textId\": "+textIdValue+",\n" +
                "\t\"taggedInputText\": \"null\",\n" +
                "\t\"documentId\": "+documentId+",\n" +
                "\t\"extractionTypeId\": "+extractionTypeIdValue+",\n" +
                "\t\"extractedFieldInfo\": [{\n" +
                "\t\t\"fieldId\": "+derivedFromValue+",\n" +
                "\t\t\"tagIdentifier\": \"null\",\n" +
                "\t\t\"extractedText\": "+extractedText+"\n" +
                "\t}],\n" +
                "\t\"status\": "+status+"\n" +
                "}";
        HttpResponse getSavedMetaDataResponse = AutoExtractionHelper.getAndCheckMetaDataForAE645(query,payloadStr);
        int statusCodeOfRequest = getSavedMetaDataResponse.getStatusLine().getStatusCode();
        softAssert.assertEquals(statusCodeOfRequest,200,"Wrong status code received");
        if(statusCodeOfRequest == 200) {
            Integer fieldIdVal = 12493;
            boolean isDataAsPerRuleFound = false;
            List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","ExtractedData_1001","documentid",6848);
            List<Integer> isDerivedFromList = docParserParamLinkingData.stream().map(document -> (Integer)document.get("fieldid")).collect(Collectors.toList());
            if(isDerivedFromList.size() > 0) {
               for(Integer isDerivedVal: isDerivedFromList) {
                   if (isDerivedVal.equals(fieldIdVal)) {
                       isDataAsPerRuleFound = true;
                       break;
                   }
               }
               softAssert.assertTrue(isDataAsPerRuleFound,"Data not found as per rule in extracted text");
            }
        }
        softAssert.assertAll();

    }

    /*
     * Test case for Story AE-645 to find text presence as per defined rules in extracted text
     */
    @Parameters("Environment")
    @Test(enabled = false)
    public void testAvailableTextInExtractedTextAE645(String environment) {
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        //block of code to insert metadata with rules
        String query = "/autoExtraction/saveFieldMetadata";
        String ruleValues = "[\"INDIA\",\"CHINA\",\"RUSSIA\"]";
        int clientIdValue = 1020;
        int entityTypeIdValue  = 316;
        int derivedFromValue = 12493;
        int extractionTypeIdValue = 1001;
        int fieldIdValue = 111;
        int referenceFieldIdValue = 111;
        String payload = "{\n" +
                "    \"clientId\" : "+clientIdValue+",\n" +
                "    \"fieldId\" : "+fieldIdValue+",\n" +
                "    \"referenceFieldId\" : "+referenceFieldIdValue+",\n" +
                "    \"entityTypeId\" : "+entityTypeIdValue+",\n" +
                "    \"fieldName\" : \"is India or China or Russia Presant\",\n" +
                "    \"active\" : true,\n" +
                "    \"extractionTypeId\" : "+extractionTypeIdValue+",\n" +
                "    \"tagIdentifier\" : \"CTYPE\",\n" +
                "    \"fieldOptions\" : [],\n" +
                "    \"isDerived\" : true,\n" +
                "    \"derivedFrom\" : "+derivedFromValue+",\n" +
                "    \"rules\" :" + ruleValues + "\n" +
                "\t\n" +
                "}";
        HttpResponse getInsertMetaDataResponse = AutoExtractionHelper.getAndCheckMetaDataForAE645(query,payload);
        int statusCodeOfRequest = getInsertMetaDataResponse.getStatusLine().getStatusCode();
        softAssert.assertEquals(statusCodeOfRequest,200,"Wrong status code received");
        boolean valueFoundInExtractedText = false;
        outerloop:
        if(statusCodeOfRequest == 200) {
            //block of code to save extracted info
                if(algoId==null) {
                    algoId = "1001";
                }
                String anotherQuery = "/autoExtraction/saveExtractedInfo/" + algoId;
                String payloadStr = "{\n" +
                        "\t\"clientId\": 1020,\n" +
                        "\t\"textId\": 329,\n" +
                        "\t\"taggedInputText\": \"null\",\n" +
                        "\t\"documentId\": 6848,\n" +
                        "\t\"extractionTypeId\": 1001,\n" +
                        "\t\"extractedFieldInfo\": [{\n" +
                        "\t\t\"fieldId\": 12493,\n" +
                        "\t\t\"tagIdentifier\": \"null\",\n" +
                        "\t\t\"extractedText\": [\"HONGKONG\", \"RUSSIA\", \"India\", \"USA\", \"SINGAPORE\"]\n" +
                        "\t}],\n" +
                        "\t\"status\": 3\n" +
                        "}";
                HttpResponse getSavedMetaDataResponse = AutoExtractionHelper.getAndCheckMetaDataForAE645(anotherQuery,payloadStr);
                int statusCodeOfAnotherRequest = getSavedMetaDataResponse.getStatusLine().getStatusCode();
                softAssert.assertEquals(statusCodeOfAnotherRequest,200,"Wrong status code received");
                boolean isDataAsPerRuleFound = false;
                if(statusCodeOfAnotherRequest == 200) {
                int derivedValueMapped = 12493;
               JSONObject jsonObjectFromExtracted = null;
               String valu = "";
                List<Document> docExtractedParamLinkingData = connection.getExtractedDataFromDB("AE_CA_POD_2", "ExtractedData_1001", 6848, 329);
                for(int i=0;i<docExtractedParamLinkingData.size();i++) {
                    jsonObjectFromExtracted = new JSONObject(docExtractedParamLinkingData.get(i).toJson());
                    JSONArray country = jsonObjectFromExtracted.getJSONArray("extractedtext");
                    for(int j=0;j<country.length();j++) {
                        valu = country.getString(j);
                        if(ruleValues.contains(valu)){
                            valueFoundInExtractedText = true;
                            break outerloop;
                        }
                    }
                }
            }

        }
        softAssert.assertTrue(valueFoundInExtractedText,"Text defined in rules is not found in extracted text");
        softAssert.assertAll();
    }

    /*
     * Test case for story AE-645 - Checking rule text not found in extracted text
     */
    @Parameters("Environment")
    @Test(enabled = false)
    public void testNonAvailableTextInExtractedTextAE645(String environment) {
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        //block of code to insert metadata with rules
        String query = "/autoExtraction/saveFieldMetadata";
        String ruleValues = "[\"FRANCE\",\"UK\",\"GERMANY\"]";
        int derivedFromValue = 12493;
        int extractionTypeIdValue = 1001;
        int fieldIdValue = 111;
        int referenceFieldIdValue = 111;
        int clientIdValue = 1020;
        int entityTypeIdValue  = 316;
        String payload =  "{\n" +
                "    \"clientId\" : "+clientIdValue+",\n" +
                "    \"fieldId\" : "+fieldIdValue+",\n" +
                "    \"referenceFieldId\" : "+referenceFieldIdValue+",\n" +
                "    \"entityTypeId\" : "+entityTypeIdValue+",\n" +
                "    \"fieldName\" : \"is India or China or Russia Presant\",\n" +
                "    \"active\" : true,\n" +
                "    \"extractionTypeId\" : "+extractionTypeIdValue+",\n" +
                "    \"tagIdentifier\" : \"CTYPE\",\n" +
                "    \"fieldOptions\" : [],\n" +
                "    \"isDerived\" : true,\n" +
                "    \"derivedFrom\" : "+derivedFromValue+",\n" +
                "    \"rules\" :" + ruleValues + "\n" +
                "\t\n" +
                "}";

        HttpResponse getInsertMetaDataResponse = AutoExtractionHelper.getAndCheckMetaDataForAE645(query,payload);
        int statusCodeOfRequest = getInsertMetaDataResponse.getStatusLine().getStatusCode();
        softAssert.assertEquals(statusCodeOfRequest,200,"Wrong status code received");
        boolean valueFoundInExtractedText = false;
        outerloop:
        if(statusCodeOfRequest == 405) {
            //block of code to save extracted info
            if(algoId==null) {
                algoId = "1001";
            }
            String anotherQuery = "/autoExtraction/saveExtractedInfo/" + algoId;
            String extractedText  = "[\"HONGKONG\", \"RUSSIA\", \"India\", \"USA\", \"SINGAPORE\"]";
            int clientIDValue = 1020;
            int textIdValue = 329;
            int documentId = 6848;
            int status = 3;
            String payloadStr = "{\n" +
                    "\t\"clientId\": "+clientIDValue+",\n" +
                    "\t\"textId\": "+textIdValue+",\n" +
                    "\t\"taggedInputText\": \"null\",\n" +
                    "\t\"documentId\": "+documentId+",\n" +
                    "\t\"extractionTypeId\": "+extractionTypeIdValue+",\n" +
                    "\t\"extractedFieldInfo\": [{\n" +
                    "\t\t\"fieldId\": "+derivedFromValue+",\n" +
                    "\t\t\"tagIdentifier\": \"null\",\n" +
                    "\t\t\"extractedText\": "+extractedText+"\n" +
                    "\t}],\n" +
                    "\t\"status\": "+status+"\n" +
                    "}";
            HttpResponse getSavedMetaDataResponse = AutoExtractionHelper.getAndCheckMetaDataForAE645(anotherQuery,payloadStr);
            int statusCodeOfAnotherRequest = getSavedMetaDataResponse.getStatusLine().getStatusCode();
            softAssert.assertEquals(statusCodeOfAnotherRequest,200,"Wrong status code received");
            boolean isDataAsPerRuleFound = false;
            if(statusCodeOfAnotherRequest == 405) {
                int derivedValueMapped = 12493;
                JSONObject jsonObjectFromExtracted = null;
                String valu = "";
                List<Document> docExtractedParamLinkingData = connection.getExtractedDataFromDB("AE_CA_POD_2", "ExtractedData_1001", 6848, 329);
                for(int i=0;i<docExtractedParamLinkingData.size();i++) {
                    jsonObjectFromExtracted = new JSONObject(docExtractedParamLinkingData.get(i).toJson());
                    JSONArray country = jsonObjectFromExtracted.getJSONArray("extractedtext");
                    for(int j=0;j<country.length();j++) {
                        valu = country.getString(j);
                        if(ruleValues.contains(valu)){
                            valueFoundInExtractedText = true;
                            break outerloop;
                        }
                    }
                }
            }

        }
        softAssert.assertTrue(valueFoundInExtractedText,"Text defined in rules is not found in extracted text");
        softAssert.assertAll();
    }

    /*
     * Test Case for getting Metadata and extracted text  for Contracts and Clauses of extracted document
     */
    @Parameters("Environment")
    @Test(enabled = false)
    public void testExtractedDataMetaDataContracts(String environment) throws IOException {
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        logger.info("testing extracted metadata test case");
        JSONArray MetaDataResponseJson = null;
        String query = "/autoExtraction/extracted/metadata";
        String jsonfileName = "src/test/resources/TestConfig/AutoExtraction/ExtractedDataFile.json";
        FileUtils fileUtilObj = new FileUtils();
        String dataFromFile = fileUtilObj.getDataInFile(jsonfileName);
        JSONArray jsAr = new JSONArray(dataFromFile);
        String payload = jsAr.getJSONObject(0).toString();
        HttpResponse responseOfMetadata = AutoExtractionHelper.autoExtractionMetaDataApi(query,payload);
        int statusCodeOfRequest = responseOfMetadata.getStatusLine().getStatusCode();
        softAssert.assertEquals(statusCodeOfRequest,200,"Wrong status code received");
        String responseOfMetadataStr = EntityUtils.toString(responseOfMetadata.getEntity());
        softAssert.assertTrue(JSONUtility.validjson(responseOfMetadataStr), "Invalid Json");
        boolean isValidJson =APIUtils.validJsonResponse(responseOfMetadataStr);
        softAssert.assertTrue(isValidJson,"Json is not valid");
        if(isValidJson) {
            MetaDataResponseJson = new JSONArray(responseOfMetadataStr);
            System.out.println(responseOfMetadataStr);
        }
        Set<Integer> documentIdSet = new HashSet<Integer>();
        int lengthOfJsonObj = MetaDataResponseJson.length();
        for(int i=0;i<lengthOfJsonObj;i++){
            documentIdSet.add(MetaDataResponseJson.getJSONObject(i).getInt("documentid"));
        }
        System.out.println(documentIdSet);
        softAssert.assertTrue(documentIdSet.contains("200454"),"extracted document is not present");
        softAssert.assertAll();
    }

    /*
     * Test Case for getting Metadata and extracted text  for Contracts and Clauses of non-extracted document
     */
    @Parameters("Environment")
    @Test(enabled = false)
    public void testNonExtractedDataMetaDataContracts(String environment) throws IOException {
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        logger.info("testing extracted metadata test case");
        JSONArray MetaDataResponseJson = null;
        String query = "/autoExtraction/extracted/metadata";
        String jsonfileName = "src/test/resources/TestConfig/AutoExtraction/ExtractedDataFile.json";
        FileUtils fileUtilObj = new FileUtils();
        String dataFromFile = fileUtilObj.getDataInFile(jsonfileName);
        JSONArray jsAr = new JSONArray(dataFromFile);
        String payload = jsAr.getJSONObject(0).toString();
        HttpResponse responseOfMetadata = AutoExtractionHelper.autoExtractionMetaDataApi(query,payload);
        int statusCodeOfRequest = responseOfMetadata.getStatusLine().getStatusCode();
        softAssert.assertEquals(statusCodeOfRequest,200,"Wrong status code received");
        String responseOfMetadataStr = EntityUtils.toString(responseOfMetadata.getEntity());
        softAssert.assertTrue(JSONUtility.validjson(responseOfMetadataStr), "Invalid Json");
        boolean isValidJson =APIUtils.validJsonResponse(responseOfMetadataStr);
        softAssert.assertTrue(isValidJson,"Json is not valid");
        if(isValidJson) {
            MetaDataResponseJson = new JSONArray(responseOfMetadataStr);
            System.out.println(responseOfMetadataStr);
        }
        Set<Integer> documentIdSet = new HashSet<Integer>();
        int lengthOfJsonObj = MetaDataResponseJson.length();
        for(int i=0;i<lengthOfJsonObj;i++){
            documentIdSet.add(MetaDataResponseJson.getJSONObject(i).getInt("documentid"));
        }
        System.out.println(documentIdSet);
        softAssert.assertTrue(documentIdSet.contains("200454"),"Response does not contain extracted document");
        softAssert.assertAll();

    }

    /*
     * Test Case for getting Metadata and extracted text  for Obligations of extracted document
    */
    @Parameters("Environment")
    @Test(enabled = false)
    public void testExtractedDataObligationInfo(String environment) throws IOException {
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        logger.info("testing extracted metadata test case");
        JSONArray MetaDataOblResponseJson = null;
        String query = "/autoExtraction/extracted/dno";
        String jsonfileName = "src/test/resources/TestConfig/AutoExtraction/ExtractedDataFile.json";
        FileUtils fileUtilObj = new FileUtils();
        String dataFromFile = fileUtilObj.getDataInFile(jsonfileName);
        JSONArray jsAr = new JSONArray(dataFromFile);
        String payload = jsAr.getJSONObject(1).toString();
        HttpResponse responseOfMetadata = AutoExtractionHelper.autoExtractionMetaDataApi(query,payload);
        int statusCodeOfRequest = responseOfMetadata.getStatusLine().getStatusCode();
        softAssert.assertEquals(statusCodeOfRequest,200,"Wrong status code received");
        String responseOfMetadataStr = EntityUtils.toString(responseOfMetadata.getEntity());
        softAssert.assertTrue(JSONUtility.validjson(responseOfMetadataStr), "Invalid Json");
        boolean isValidJson =APIUtils.validJsonResponse(responseOfMetadataStr);
        softAssert.assertTrue(isValidJson,"Json is not valid");
        softAssert.assertAll();
    }

    /*
     * Test Case for checking Date type of transformed text
     */
    @Parameters("Environment")
    @Test
    public void testDateDataTypeTransformedText(String environment) throws IOException {
        softAssert = new SoftAssert();
        String documentId = "";
        if(environment.equals("autoextraction_sandbox")){
            documentId = "9256";
        } else if(environment.equals("autoextraction"))
        {
            documentId = "300452";
        }
        String query = "/listRenderer/list/433/listdata?isFirstCall=false";
        String payload = "{\n" +
                "\t\"filterMap\": {\n" +
                "\t\t\"entityTypeId\": 316,\n" +
                "\t\t\"offset\": 0,\n" +
                "\t\t\"size\": 50,\n" +
                "\t\t\"orderByColumnName\": \"id\",\n" +
                "\t\t\"orderDirection\": \"asc nulls first\",\n" +
                "\t\t\"filterJson\": {\n" +
                "\t\t\t\"366\": {\n" +
                "\t\t\t\t\"multiselectValues\": {\n" +
                "\t\t\t\t\t\"SELECTEDDATA\": []\n" +
                "\t\t\t\t},\n" +
                "\t\t\t\t\"filterId\": 366,\n" +
                "\t\t\t\t\"filterName\": \"categoryId\",\n" +
                "\t\t\t\t\"entityFieldHtmlType\": null,\n" +
                "\t\t\t\t\"entityFieldId\": null\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t},\n" +
                "\t\"entityId\": "+documentId+"\n" +
                "}";
        HttpResponse response = AutoExtractionHelper.checkFieldTypeDataType(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Wrong Status Code");
        String responseStr = EntityUtils.toString(response.getEntity());
        JSONObject jsoFromStr = new JSONObject(responseStr);
        int lengthOfData = jsoFromStr.getJSONArray("data").length();
        List<Set<String>> listOfKeySets = new ArrayList<>();
        boolean isDateTypeFieldFound = false;
        for(int i =0;i<lengthOfData;i++){
            if(isDateTypeFieldFound == true)
            {
                break;
            }
            Set<String> keyIds = jsoFromStr.getJSONArray("data").getJSONObject(i).keySet();
            for(String key: keyIds) {
                String valueOfKey = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("value");
                String columnName = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("columnName");
                if(columnName.equalsIgnoreCase("transformedtext") && !valueOfKey.isEmpty()){
                    isDateTypeFieldFound = true;
                    String sDate=valueOfKey.toString();
                    try {
                        Date dateValue = new SimpleDateFormat("yyyy/MM/dd").parse(sDate);
                    } catch (ParseException e) {
                        softAssert.assertTrue(false,"transformatted text is not a date type");
                    }

                    break;
                }
            }

        }
        softAssert.assertAll();
    }

    /*
     * Test Case for checking transformed text for date type of non-extracted document
     */
    @Parameters("Environment")
    @Test
    public void testDateDataTypeTransformedTextNonExtractedDoc(String environment) throws IOException {
        softAssert = new SoftAssert();
        String documentId = "";
        if(environment.equals("autoextraction_sandbox")){
            documentId = "9256";
        } else if(environment.equals("autoextraction"))
        {
            documentId = "300452";
        }
        String query = "/listRenderer/list/433/listdata?isFirstCall=false";
        String payload = "{\n" +
                "\t\"filterMap\": {\n" +
                "\t\t\"entityTypeId\": 316,\n" +
                "\t\t\"offset\": 0,\n" +
                "\t\t\"size\": 50,\n" +
                "\t\t\"orderByColumnName\": \"id\",\n" +
                "\t\t\"orderDirection\": \"asc nulls first\",\n" +
                "\t\t\"filterJson\": {\n" +
                "\t\t\t\"366\": {\n" +
                "\t\t\t\t\"multiselectValues\": {\n" +
                "\t\t\t\t\t\"SELECTEDDATA\": []\n" +
                "\t\t\t\t},\n" +
                "\t\t\t\t\"filterId\": 366,\n" +
                "\t\t\t\t\"filterName\": \"categoryId\",\n" +
                "\t\t\t\t\"entityFieldHtmlType\": null,\n" +
                "\t\t\t\t\"entityFieldId\": null\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t},\n" +
                "\t\"entityId\": "+documentId+"\n" +
                "}";
        HttpResponse response = AutoExtractionHelper.checkFieldTypeDataType(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Wrong Status Code");
        String responseStr = EntityUtils.toString(response.getEntity());
        JSONObject jsoFromStr = new JSONObject(responseStr);
        int lengthOfData = jsoFromStr.getJSONArray("data").length();
        List<Set<String>> listOfKeySets = new ArrayList<>();
        boolean isDateTypeFieldFound = false;
        for(int i =0;i<lengthOfData;i++){
            if(isDateTypeFieldFound == true)
            {
                break;
            }
            Set<String> keyIds = jsoFromStr.getJSONArray("data").getJSONObject(i).keySet();
            for(String key: keyIds) {
                String valueOfKey = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("value");
                String columnName = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("columnName");
                if(columnName.equalsIgnoreCase("transformedtext") && !valueOfKey.isEmpty()){
                    isDateTypeFieldFound = true;
                    String sDate=valueOfKey.toString();
                    try {
                        Date dateValue = new SimpleDateFormat("yyyy/MM/dd").parse(sDate);
                    } catch (ParseException e) {
                        softAssert.assertTrue(false,"transformatted text is not a date type");
                    }

                    break;
                }
            }

        }
        softAssert.assertAll();
    }

    /*
     * Test Case for checking Term type of transformed text
     */
    @Parameters("Environment")
    @Test
    public void testTermDataTypeTransformedText(String environment) throws IOException {
        softAssert = new SoftAssert();
        String documentId = "";
        if(environment.equals("autoextraction_sandbox")){
            documentId = "9244";
        } else if(environment.equals("autoextraction"))
        {
            documentId = "300452";
        }
        String query = "/listRenderer/list/433/listdata?isFirstCall=false";
        String payload = "{\n" +
                "\t\"filterMap\": {\n" +
                "\t\t\"entityTypeId\": 316,\n" +
                "\t\t\"offset\": 0,\n" +
                "\t\t\"size\": 50,\n" +
                "\t\t\"orderByColumnName\": \"id\",\n" +
                "\t\t\"orderDirection\": \"asc nulls first\",\n" +
                "\t\t\"filterJson\": {\n" +
                "\t\t\t\"366\": {\n" +
                "\t\t\t\t\"multiselectValues\": {\n" +
                "\t\t\t\t\t\"SELECTEDDATA\": []\n" +
                "\t\t\t\t},\n" +
                "\t\t\t\t\"filterId\": 366,\n" +
                "\t\t\t\t\"filterName\": \"categoryId\",\n" +
                "\t\t\t\t\"entityFieldHtmlType\": null,\n" +
                "\t\t\t\t\"entityFieldId\": null\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t},\n" +
                "\t\"entityId\": "+documentId+"\n" +
                "}";
        HttpResponse response = AutoExtractionHelper.checkFieldTypeDataType(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Wrong Status Code");
        String responseStr = EntityUtils.toString(response.getEntity());
        JSONObject jsoFromStr = new JSONObject(responseStr);
        int lengthOfData = jsoFromStr.getJSONArray("data").length();
        List<Set<String>> listOfKeySets = new ArrayList<>();
        boolean isDateTypeFieldFound = false;
        for(int i =0;i<lengthOfData;i++){
            if(isDateTypeFieldFound == true)
            {
                break;
            }
            Set<String> keyIds = jsoFromStr.getJSONArray("data").getJSONObject(i).keySet();
            for(String key: keyIds) {
                String valueOfKey = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("value");
                String columnName = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("columnName");
                if(columnName.equalsIgnoreCase("transformedtext") && !valueOfKey.isEmpty()){
                    String termStr =valueOfKey.toString();
                    String as = termStr.substring(0,2);
                    if(as.equalsIgnoreCase("10")) {
                        isDateTypeFieldFound = true;
                        break;
                    }
                }
            }

        }
        softAssert.assertAll();
    }
    /*
     * Test Case for checking transformed text for term  type of non-extracted document
     */
    @Parameters("Environment")
    @Test
    public void testTermDataTypeTransformedTextNonExtractedDoc(String environment) throws IOException {
        softAssert = new SoftAssert();
        String documentId = "";
        if(environment.equals("autoextraction_sandbox")){
            documentId = "9244";
        } else if(environment.equals("autoextraction"))
        {
            documentId = "300452";
        }
        String query = "/listRenderer/list/433/listdata?isFirstCall=false";
        String payload = "{\n" +
                "\t\"filterMap\": {\n" +
                "\t\t\"entityTypeId\": 316,\n" +
                "\t\t\"offset\": 0,\n" +
                "\t\t\"size\": 50,\n" +
                "\t\t\"orderByColumnName\": \"id\",\n" +
                "\t\t\"orderDirection\": \"asc nulls first\",\n" +
                "\t\t\"filterJson\": {\n" +
                "\t\t\t\"366\": {\n" +
                "\t\t\t\t\"multiselectValues\": {\n" +
                "\t\t\t\t\t\"SELECTEDDATA\": []\n" +
                "\t\t\t\t},\n" +
                "\t\t\t\t\"filterId\": 366,\n" +
                "\t\t\t\t\"filterName\": \"categoryId\",\n" +
                "\t\t\t\t\"entityFieldHtmlType\": null,\n" +
                "\t\t\t\t\"entityFieldId\": null\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t},\n" +
                "\t\"entityId\": "+documentId+"\n" +
                "}";
        HttpResponse response = AutoExtractionHelper.checkFieldTypeDataType(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Wrong Status Code");
        String responseStr = EntityUtils.toString(response.getEntity());
        JSONObject jsoFromStr = new JSONObject(responseStr);
        int lengthOfData = jsoFromStr.getJSONArray("data").length();
        List<Set<String>> listOfKeySets = new ArrayList<>();
        boolean isDateTypeFieldFound = false;
        for(int i =0;i<lengthOfData;i++){
            if(isDateTypeFieldFound == true)
            {
                break;
            }
            Set<String> keyIds = jsoFromStr.getJSONArray("data").getJSONObject(i).keySet();
            for(String key: keyIds) {
                String valueOfKey = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("value");
                String columnName = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("columnName");
                if(columnName.equalsIgnoreCase("transformedtext") && !valueOfKey.isEmpty()){
                    String termStr =valueOfKey.toString();
                    String as = termStr.substring(0,2);
                    if(as.equalsIgnoreCase("10")) {
                        isDateTypeFieldFound = true;
                        break;
                    }
                }
            }

        }
        softAssert.assertAll();
    }


    /*
     * Test Case for checking transformed text for String type of extractedtext
     */
    @Parameters("Environment")
    @Test
    public void testStringDataTypeTransformedText(String environment) throws IOException {
        softAssert = new SoftAssert();
        String documentId = "";
        if(environment.equals("autoextraction_sandbox")){
            documentId = "9256";
        } else if(environment.equals("autoextraction"))
        {
            documentId = "300452";
        }
        String query = "/listRenderer/list/433/listdata?isFirstCall=false";
        String payload = "{\n" +
                "\t\"filterMap\": {\n" +
                "\t\t\"entityTypeId\": 316,\n" +
                "\t\t\"offset\": 0,\n" +
                "\t\t\"size\": 50,\n" +
                "\t\t\"orderByColumnName\": \"id\",\n" +
                "\t\t\"orderDirection\": \"asc nulls first\",\n" +
                "\t\t\"filterJson\": {\n" +
                "\t\t\t\"366\": {\n" +
                "\t\t\t\t\"multiselectValues\": {\n" +
                "\t\t\t\t\t\"SELECTEDDATA\": []\n" +
                "\t\t\t\t},\n" +
                "\t\t\t\t\"filterId\": 366,\n" +
                "\t\t\t\t\"filterName\": \"categoryId\",\n" +
                "\t\t\t\t\"entityFieldHtmlType\": null,\n" +
                "\t\t\t\t\"entityFieldId\": null\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t},\n" +
                "\t\"entityId\": "+documentId+"\n" +
                "}";
        HttpResponse response = AutoExtractionHelper.checkFieldTypeDataType(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Wrong Status Code");
        String responseStr = EntityUtils.toString(response.getEntity());
        JSONObject jsoFromStr = new JSONObject(responseStr);
        int lengthOfData = jsoFromStr.getJSONArray("data").length();
        List<Set<String>> listOfKeySets = new ArrayList<>();
        boolean isStringTypeFieldFound = false;
        String extractedTextValue = "";
        for(int i =0;i<lengthOfData;i++){
            if(isStringTypeFieldFound == true)
            {
                break;
            }
            Set<String> keyIds = jsoFromStr.getJSONArray("data").getJSONObject(i).keySet();
            for(String key: keyIds) {
                String valueOfKey = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("value");
                String columnName = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("columnName");
                if(columnName.equalsIgnoreCase("extractedtext") && !valueOfKey.isEmpty()){
                    extractedTextValue = valueOfKey;
                }
                if(columnName.equalsIgnoreCase("transformedtext")){

                    if(extractedTextValue.contains(valueOfKey) || valueOfKey.isEmpty()){
                        isStringTypeFieldFound = true;

                    }
                    break;
                }
            }
        }
        softAssert.assertAll();
    }
    /*
     * Test Case for checking transformed text for text type of non-extracted document for String Type
     */
    @Parameters("Environment")
    @Test
    public void testStringDataTypeTransformedTextNonExtractedDoc(String environment) throws IOException {
        softAssert = new SoftAssert();
        String documentId = "";
        if(environment.equals("autoextraction_sandbox")){
            documentId = "9256";
        } else if(environment.equals("autoextraction"))
        {
            documentId = "300452";
        }
        String query = "/listRenderer/list/433/listdata?isFirstCall=false";
        String payload = "{\n" +
                "\t\"filterMap\": {\n" +
                "\t\t\"entityTypeId\": 316,\n" +
                "\t\t\"offset\": 0,\n" +
                "\t\t\"size\": 50,\n" +
                "\t\t\"orderByColumnName\": \"id\",\n" +
                "\t\t\"orderDirection\": \"asc nulls first\",\n" +
                "\t\t\"filterJson\": {\n" +
                "\t\t\t\"366\": {\n" +
                "\t\t\t\t\"multiselectValues\": {\n" +
                "\t\t\t\t\t\"SELECTEDDATA\": []\n" +
                "\t\t\t\t},\n" +
                "\t\t\t\t\"filterId\": 366,\n" +
                "\t\t\t\t\"filterName\": \"categoryId\",\n" +
                "\t\t\t\t\"entityFieldHtmlType\": null,\n" +
                "\t\t\t\t\"entityFieldId\": null\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\t},\n" +
                "\t\"entityId\": "+documentId+"\n" +
                "}";
        HttpResponse response = AutoExtractionHelper.checkFieldTypeDataType(query,payload);
        softAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"Wrong Status Code");
        String responseStr = EntityUtils.toString(response.getEntity());
        JSONObject jsoFromStr = new JSONObject(responseStr);
        int lengthOfData = jsoFromStr.getJSONArray("data").length();
        List<Set<String>> listOfKeySets = new ArrayList<>();
        boolean isStringTypeFieldFound = false;
        String extractedTextValue = "";
        for(int i =0;i<lengthOfData;i++){
            if(isStringTypeFieldFound == true)
            {
                break;
            }
            Set<String> keyIds = jsoFromStr.getJSONArray("data").getJSONObject(i).keySet();
            for(String key: keyIds) {
                String valueOfKey = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("value");
                String columnName = jsoFromStr.getJSONArray("data").getJSONObject(i).getJSONObject(key).getString("columnName");
                if(columnName.equalsIgnoreCase("extractedtext") && !valueOfKey.isEmpty()){
                    extractedTextValue = valueOfKey;
                }
                if(columnName.equalsIgnoreCase("transformedtext")){

                    if(extractedTextValue.contains(valueOfKey) || valueOfKey.isEmpty()){
                        isStringTypeFieldFound = true;

                    }
                    break;
                }
            }
        }
        softAssert.assertAll();
    }



    /*
     * Test Case to check presence of fieldType to store String type in FieldMetaDataInfo collection
     */
    @Test
    public void testStringFieldTypeInCollection(){
        softAssert = new SoftAssert();
        int fieldidValue = 12551;
        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","FieldMetaDataInfo","fieldid",fieldidValue);
        List<String> parserParamLinking = docParserParamLinkingData.stream().map(document -> (String)document.get("fieldtype")).collect(Collectors.toList());
        int sizeOflistAsReceived = parserParamLinking.size();
        softAssert.assertTrue(sizeOflistAsReceived >=1,"Database result is not coming correct");
        String valueOfFieldType = parserParamLinking.get(0).toString().trim();
        softAssert.assertEquals(valueOfFieldType,"string","Invalid value returned from Database");
        JSONObject jsonObject = new JSONObject(docParserParamLinkingData.get(0).toJson());
        System.out.println(jsonObject.toString());
        softAssert.assertAll();

    }

    /*
     * Test Case to check presence of fieldType to store Date type in FieldMetaDataInfo collection
     */
    @Test(enabled = true)
    public void testDateFieldTypeInCollection(){

        softAssert = new SoftAssert();
        int fieldidValue = 12486;
        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","FieldMetaDataInfo","fieldid",fieldidValue);
        List<String> parserParamLinking = docParserParamLinkingData.stream().map(document -> (String)document.get("fieldtype")).collect(Collectors.toList());
        int sizeOflistAsReceived = parserParamLinking.size();
        softAssert.assertTrue(sizeOflistAsReceived >=1,"Database result is not coming correct");
        String valueOfFieldType = parserParamLinking.get(0).toString().trim();
        softAssert.assertEquals(valueOfFieldType,"date","Invalid value returned from Database");
        JSONObject jsonObject = new JSONObject(docParserParamLinkingData.get(0).toJson());
        System.out.println(jsonObject.toString());
        softAssert.assertAll();

    }

    /*
     * Test Case to check presence of fieldType to store Term type in FieldMetaDataInfo collection
     */
    @Test(enabled = true)
    public void testTermFieldTypeInCollection(){

        softAssert = new SoftAssert();
        int fieldidValue = 12488;
        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","FieldMetaDataInfo","fieldid",fieldidValue);
        List<String> parserParamLinking = docParserParamLinkingData.stream().map(document -> (String)document.get("fieldtype")).collect(Collectors.toList());
        int sizeOflistAsReceived = parserParamLinking.size();
        softAssert.assertTrue(sizeOflistAsReceived >=1,"Database result is not coming correct");
        String valueOfFieldType = parserParamLinking.get(0).toString().trim();
        softAssert.assertEquals(valueOfFieldType,"term","Invalid value returned from Database");
        JSONObject jsonObject = new JSONObject(docParserParamLinkingData.get(0).toJson());
        System.out.println(jsonObject.toString());
        softAssert.assertAll();

    }

    /*
     * Test Case to check presence of transformedtext in  ExtractedData_1001 collection
     */
    @Test(enabled = true)
    public void testTransformedTextInContractCollection() {
        softAssert = new SoftAssert();
        int fieldidValue = 1037;
        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","ExtractedData_1001","documentid",fieldidValue);
        List<ArrayList> parserParamLinking = docParserParamLinkingData.stream().map(document -> (ArrayList)document.get("transformedtext")).collect(Collectors.toList());
        int sizeOflistAsReceived = parserParamLinking.size();
        softAssert.assertTrue(sizeOflistAsReceived >=1,"Database result is not coming correct");
        softAssert.assertAll();
    }
    /*
     * Test Case to check presence of transformedtext in  ExtractedData_1002 collection
     */
    @Test(enabled = true)
    public void testTransformedTextInObligationCollection(){
        softAssert = new SoftAssert();
        int fieldidValue = 1037;
        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","ExtractedData_1002","documentid",fieldidValue);
        List<ArrayList> parserParamLinking = docParserParamLinkingData.stream().map(document -> (ArrayList)document.get("transformedtext")).collect(Collectors.toList());
        int sizeOflistAsReceived = parserParamLinking.size();
        softAssert.assertTrue(sizeOflistAsReceived >=1,"Database result is not coming correct");
        softAssert.assertAll();
    }

    /*
     * Test Case for getting Metadata and extracted text  for Obligations of non-extracted document
     */
    @Parameters("Environment")
    @Test(enabled = false)
    public void testNonExtractedDataObligationInfo(String environment) throws IOException {
        if(environment.equals("autoextraction_sandbox")){
            throw new SkipException("This test case has dependency on access criteria job thus skipping it for sandbox");
        }
        softAssert = new SoftAssert();
        logger.info("testing extracted metadata test case");
        JSONArray MetaDataOblResponseJson = null;
        String query = "/autoExtraction/extracted/dno";
        String jsonfileName = "src/test/resources/TestConfig/AutoExtraction/ExtractedDataFile.json";
        FileUtils fileUtilObj = new FileUtils();
        String dataFromFile = fileUtilObj.getDataInFile(jsonfileName);
        JSONArray jsAr = new JSONArray(dataFromFile);
        String payload = jsAr.getJSONObject(1).toString();
        HttpResponse responseOfMetadata = AutoExtractionHelper.autoExtractionMetaDataApi(query,payload);
        int statusCodeOfRequest = responseOfMetadata.getStatusLine().getStatusCode();
        softAssert.assertEquals(statusCodeOfRequest,200,"Wrong status code received");
        String responseOfMetadataStr = EntityUtils.toString(responseOfMetadata.getEntity());
        softAssert.assertTrue(JSONUtility.validjson(responseOfMetadataStr), "Invalid Json");
        boolean isValidJson =APIUtils.validJsonResponse(responseOfMetadataStr);
        softAssert.assertTrue(isValidJson,"Json is not valid");
        softAssert.assertAll();

    }

    //Data Provider for testMultiEntityAllDocs()
    @DataProvider
    public Object[][] multiEntityTestDataAll() {
        logger.info("Data Provider method to pass values to all documents of a contract");
        //Getting Contract ID from Test Data File
        String multiEntityContractId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"multicontractid");
        return new Object[][] {
                {"/listRenderer/list/433/listdata?isFirstCall=false", "fieldname","{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":" + multiEntityContractId + "}",7},
                {"/listRenderer/list/493/listdata?isFirstCall=false", "name","{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":"+ multiEntityContractId +"}",42},
                {"/listRenderer/list/437/listdata?isFirstCall=false", "category","{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":"+ multiEntityContractId + "}",7}
        };
    }

    //Data Provider for testMultiEntitySingleDoc()
    @DataProvider
    public Object[][] multiEntityTestDataIndividual() {
        logger.info("Data Provider method to pass values to single document for each tab");
        //Getting Document ID from Test Data File
        String multiEntityDocumentId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"multicontractdocid");
        return new Object[][] {
                {"/listRenderer/list/433/listdata?isFirstCall=false", "fieldname","{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":" + multiEntityDocumentId + "}",7},
                {"/listRenderer/list/493/listdata?isFirstCall=false", "name","{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":" + multiEntityDocumentId + "}",15},
                {"/listRenderer/list/437/listdata?isFirstCall=false", "category","{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{}},\"entityId\":" + multiEntityDocumentId + "}",7}
        };
    }
}
