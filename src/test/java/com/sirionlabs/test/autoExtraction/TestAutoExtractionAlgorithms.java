package com.sirionlabs.test.autoExtraction;


import com.sirionlabs.api.autoExtraction.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.mongodb.MongoDBConnection;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


public class TestAutoExtractionAlgorithms extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestAutoExtractionAlgorithms.class);
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    static Integer docId;
    static String filePath;
    private static String autoExtractionServiceHostUrl;
    private static List<String> savedParsedInfoTextIds;
    private static String fetchDataUrl;
    private static Document parsedData;
    private static String mongoDBHost;
    private static int mongoDBPort;
    private static String aeHostUrl;
    private static int aePort;
    private static String aeSchema;
    private static String ae2HostUrl;
    private static int ae2Port;
    private static String backendConfigration;
    private static String algoId;

    static String clientId;
    HttpHost aeHost;

    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;
    APIUtils apiUtils = new APIUtils();
    MongoDBConnection connection;
    // extractedtext:{$not:{$size:0}}}
    // 192.168.1.54
    // TeamAutomation


    @BeforeClass
    public void beforeClass() {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        docId = Integer.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docid"));
        autoExtractionServiceHostUrl =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","scheme") + "://" +
        ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","hostname") + ":"+
        ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","port");
        mongoDBHost =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"mongo db credentionals", "hosturl");
        mongoDBPort = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"mongo db credentionals", "port"));
        connection = new MongoDBConnection(mongoDBHost,mongoDBPort);
        aeHostUrl = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae environment", "hostname");
        aePort = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae environment", "port"));
        aeSchema = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae environment", "scheme");
        ae2HostUrl = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae2 environment", "hostname");
        ae2Port = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae2 environment", "port"));
        clientId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"clientid");
        backendConfigration = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae1 configration","backendconfigration");
        algoId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"ae1 configration","algoid");
        aeHost = new HttpHost(aeHostUrl,aePort,aeSchema);
    }

    @Test(priority = 1)
    public void TestServiceCheckAPI() throws IOException {
        softAssert = new SoftAssert();
        APIValidator response=executor.get(autoExtractionServiceHostUrl,"/autoExtraction",null);
        response.validateResponseCode(200, csAssert);
        softAssert.assertTrue(response.getResponse().getResponseCode()==200,
                "AutoExtraction Service is up and running");

        HttpResponse modelInfoResponse = AutoExtractionHelper.hitFetchModelInfoAPI(aeHost,algoId);
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


    @Test(priority = 2,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestDocumentSaveAPI() throws Exception {
        softAssert = new SoftAssert();
        // Document Save API
        APIUtils apiUtils = new APIUtils();
        Map<String,String> textBodyMap = new HashMap<>();
        Map<String,File> fileToUpload = new HashMap<>();
        textBodyMap.put("clientId",clientId);
        textBodyMap.put("docId",String.valueOf(docId));
        textBodyMap.put("extractionTypes","1001,1002");
        fileToUpload.put("fileData",new File("src/test/output/AutoExtractionDownloadedFiles/SirionAppsDownloadedFiles/Extraction_Text_Mocking.docx"));

        HttpHost httpHost = new HttpHost(aeHostUrl,aePort,"http");
        HttpEntity httpEntity = apiUtils.multiPartFormData(textBodyMap,fileToUpload,"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        HttpPost httpPost = apiUtils.generateHttpPostRequestWithQueryString(DocumentSave.getAPIPath(),"","");
        httpPost.setEntity(httpEntity);
        HttpResponse response = apiUtils.postRequestWithRequestBase(httpHost,httpPost);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
                "Doc Save API is not working as expected");

        Thread.sleep(5000);

        List<String> latestDataFromKafka = AutoExtractionHelper.runConsumer("192.168.2.174:9092","aspose");

       if(latestDataFromKafka.size()==0){
           latestDataFromKafka = AutoExtractionHelper.runConsumer("192.168.2.174:9092","aspose");
       }
        boolean isDataStoredInKafka = false;
        for(String data:latestDataFromKafka){
            if(data.trim().contains(String.valueOf(docId))){
                fetchDataUrl = data.trim();
                isDataStoredInKafka = true;
                break;
            }
        }
        softAssert.assertTrue(isDataStoredInKafka,"Data is not there in kafka associated with docId = " + docId);
        Thread.sleep(5000);

        // verify whether File is saved into database on Doc Save API hit - DocumentInfo Collection
        List<Document> uploadedDocs = connection.getDBResponse("AE_CA_POD_2","DocumentInfo","docid",docId);
        softAssert.assertTrue(uploadedDocs.size() == 1,"Document is not Successfully reflecting in Database");
        softAssert.assertAll();
    }

    @Test(priority = 3,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestParsedDataAPI() throws InterruptedException {
        softAssert = new SoftAssert();
        // Extract Linking status from database
        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","DocumentParserParamLinking","docid",docId);
        List<Integer> parserParamLinking = docParserParamLinkingData.stream().map(document -> (Integer)document.get("status")).collect(Collectors.toList());
        softAssert.assertTrue(parserParamLinking.size() ==1,"Database result is not coming correct");
        while (!docParserParamLinkingData.get(0).toJson().contains("status")){
            docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","DocumentParserParamLinking","docid",docId);
        }

        LocalTime initialTime = LocalTime.now();
        JSONObject jsonObject = new JSONObject(docParserParamLinkingData.get(0).toJson());
        while (!(Integer.valueOf(jsonObject.get("status").toString())==3)){
            docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","DocumentParserParamLinking","docid",docId);
            jsonObject = new JSONObject(docParserParamLinkingData.get(0).toJson());

            LocalTime finalTime = LocalTime.now();
            Duration duration = Duration.between(initialTime, finalTime);
            logger.info("Waiting for Scheduler to pick the document for extraction = " + duration.getSeconds());

            if (duration.getSeconds() > 600) {
                throw new SkipException("Waited for 10 minutes for Extraction to Complete." +
                        "Please look manually whether their is problem in Extracting data." +
                        "For document id " + docId + " and status " + jsonObject.get("status"));
            }
        }

        // Extract Parsed data from database
       List<Document> parsedDataList= connection.getDBResponse("AE_CA_POD_2","ParsedData","documentid",docId);

        savedParsedInfoTextIds = parsedDataList.stream().map(document -> String.valueOf(document.get("textid"))).collect(Collectors.toList()).stream()
                .map(textId -> String.valueOf(Math.round(Double.valueOf(textId)))).collect(Collectors.toList());

        List<Document> parsedDataWithText =parsedDataList.stream().filter(m->m.get("text").toString().length()>50).collect(Collectors.toList());

        for(Document document:parsedDataWithText) {
            jsonObject = new JSONObject(document.toJson());
            if (jsonObject.getJSONArray("etpercategorylist").getJSONObject(0).get("categoryname").toString().length()> 5 &&
                    jsonObject.getJSONArray("etpercategorylist").getJSONObject(1).get("categoryname").toString() != "NO") {
                parsedData = document;
                break;
            }

        }
        softAssert.assertTrue(savedParsedInfoTextIds.size()>1,"No Entry in Database is getting reflected");

        // Verify Contracts kafka topic
        List<String> latestDataFromKafka;
        latestDataFromKafka = AutoExtractionHelper.runConsumer("192.168.2.174:9092","parsed-data-extraction-ae1-qa");

        if(latestDataFromKafka.size()==0){
            latestDataFromKafka = AutoExtractionHelper.runConsumer("192.168.2.174:9092","parsed-data-extraction-ae1-qa");
        }
        boolean isDataStoredInKafka = false;
        for(String data:latestDataFromKafka){
            if(data.trim().contains(String.valueOf(docId))){
                isDataStoredInKafka = true;
                break;
            }
        }
        softAssert.assertTrue(isDataStoredInKafka,"Data is not there in kafka associated with docId = " + docId);

        // Verify Obligation kafka topic
        latestDataFromKafka = AutoExtractionHelper.runConsumer("192.168.2.174:9092","parsed-data-ae1");

        if(latestDataFromKafka.size()==0){
            latestDataFromKafka =  AutoExtractionHelper.runConsumer("192.168.2.174:9092","parsed-data-ae1");
        }
        isDataStoredInKafka = false;
        for(String data:latestDataFromKafka){
            if(data.trim().contains(String.valueOf(docId))){
                isDataStoredInKafka = true;
                break;
            }
        }
        softAssert.assertTrue(isDataStoredInKafka,"Data is not there in kafka associated with docId = " + docId);
        softAssert.assertAll();
    }

    @Test(priority = 4,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestFetchDocCatInfo() throws IOException, InterruptedException {
        softAssert = new SoftAssert();
        // Extracting Random text for the uploaded doc from parsed data collection
        //String parsedDataText = parsedData.get("text").toString().replaceAll("\\s+", " ");
        String parsedDataText = parsedData.getString("text");

        // Extracting model path,model id from preferred model collection
        List<Document> preferredModelData;
        String modelPath;
        String modelId;
        String payload;

        HttpResponse response = null;
        // Hit Doc Category Info API
        if(backendConfigration.equals("AE2")) {
            // Extraction Type 1001 i.e - Contract Extraction
            preferredModelData = connection.getPreferredModelDataFromDB("AE_CA_POD_2","PreferredModel_1002",1,1,1001);
            modelPath = preferredModelData.stream().findFirst().get().get("preferredmodelpath").toString().trim();
            modelId = preferredModelData.stream().findFirst().get().get("preferredmodelid").toString().trim();
            payload = "{\"text\":"+ "\"" +parsedDataText + "\"" +",\"categoryId\" : 0,\"trainingModelInfo\":{\"modelId\":"+ "\"" + modelId + "\"" + ",\"modelPath\":" + "\"" + modelPath + "\"" +"}} ";
            response = AutoExtractionHelper.hitFetchDocCatInfoApi(ae2HostUrl, ae2Port, "/docCategorization2/fetchDocCatInfo", payload);
        }
        else if(backendConfigration.equals("AE1")){
            // Extraction Type 1001 i.e - Contract Extraction
            String fetchDocCatPayload = "{\"text\" :"+ "\""+ parsedDataText + "\""+"}";
            response = AutoExtractionHelper.hitFetchDocCatInfoAE1Config(aeHostUrl,3389,"/classify_contract_category",fetchDocCatPayload);
        }

        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200, "Response is not valid");

        String categoryInfoResponseStr = EntityUtils.toString(response.getEntity());
        JSONObject categoryInfoResponseJson = new JSONObject(categoryInfoResponseStr);
        String categoryName = categoryInfoResponseJson.get("categoryName").toString();
        String categoryScore = categoryInfoResponseJson.get("categoryScore").toString();

        Document categoryInfo = connection.getCategoryInfoFromDB("AE_CA_POD_2","CategoryInfo",categoryName,1001).stream().findFirst().get();
        String categoryId = categoryInfo.get("categoryid").toString();

        // Validate extracted data from api with database
        JSONObject parsedDataJsonStr = new JSONObject(parsedData.toJson());
        softAssert.assertTrue(parsedDataJsonStr.getJSONArray("etpercategorylist").getJSONObject(0).get("categoryname").toString().trim().equals(categoryName.trim()),"Category Name is not stored properly in db for extraction type 1001");
        softAssert.assertTrue(parsedDataJsonStr.getJSONArray("etpercategorylist").getJSONObject(0).get("categoryscore").toString().trim().equals(categoryScore.trim()),"Category Score is not stored properly in db for extraction type 1001");
        softAssert.assertTrue(parsedDataJsonStr.getJSONArray("etpercategorylist").getJSONObject(0).get("categoryid").toString().trim().equals(categoryId.split("\\.")[0].trim()),"Category Id is not stored properly in db for extraction type 1001");

        //Extraction Type 1002 i.e - Obligation Extraction

        // Verify obligation kafka topic
        List<String> latestDataFromKafka;
        List<Integer> textIds = new LinkedList<>();
        latestDataFromKafka = AutoExtractionHelper.runConsumer("192.168.2.174:9092","parsed-data-ae1-1002");

        if(latestDataFromKafka.size()==0){
            latestDataFromKafka = AutoExtractionHelper.runConsumer("192.168.2.174:9092","parsed-data-ae1-1002");
        }

        if(latestDataFromKafka.size()>0){
            for(int i=0;i<latestDataFromKafka.size();i++){
                String[] allFields = latestDataFromKafka.get(i).replaceAll("\\{","").replaceAll("}","").split(",");
                List<Integer> textId = Arrays.stream(allFields).filter(m->m.contains("textId")).map(m->Integer.valueOf(m.split(":")[1].trim())).collect(Collectors.toList());
                for(Integer text : textId){
                textIds.add(text);
                }
            }
        }
        boolean isDataStoredInKafka = false;
        for(String data:latestDataFromKafka){
            if(data.trim().contains(String.valueOf(docId))){
                isDataStoredInKafka = true;
                break;
            }
        }
        softAssert.assertTrue(isDataStoredInKafka,"Data is not there in kafka associated with docId = " + docId);

        for(int i=0;i<textIds.size();i++){
            List<Document> parsedDataObligationData = connection.getDBResponse("AE_CA_POD_2","ParsedData","textid",textIds.get(i));
            JSONObject jsonObject = new JSONObject(parsedDataObligationData.get(0).toJson());
            softAssert.assertTrue(jsonObject.getJSONArray("etpercategorylist").getJSONObject(1).get("categoryname").toString().trim().equals("Yes"),"Category Should be Yes as Obligation is occurring for text id " + textIds.get(i));
        }
        softAssert.assertAll();
    }

    @Test(priority = 5,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestFetchMetaDataAndSaveExtractedInfoAPI() throws IOException {
        softAssert = new SoftAssert();
        String parsedDataTextId = parsedData.get("textid").toString().split("\\.")[0];

        // For Extraction Type 1001
        String query = "/autoExtraction/fetchMetaData/"+clientId+"/"+ parsedDataTextId +"/"+algoId+"/1001";
        HttpResponse response = AutoExtractionHelper.hitFetchMetaDataApi(aeHostUrl,aePort,query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        String fetchMetaDataResponseStr = EntityUtils.toString(response.getEntity());
        JSONObject fetchMetaDataResponseJson = new JSONObject(fetchMetaDataResponseStr);

        List<Map<String,Object>> fieldInfoModelData_1001 = new LinkedList<>();
        if(fetchMetaDataResponseJson.get("fieldInfoList").toString().equals("null")){
            logger.info("fieldInfoList is not present in Response");
        }
        else {
            int fieldInfoModelLength = fetchMetaDataResponseJson.getJSONArray("fieldInfoList").length();
            if(fieldInfoModelLength>=1) {
                for (int i = 0; i < fieldInfoModelLength; i++) {
                    fieldInfoModelData_1001.add(fetchMetaDataResponseJson.getJSONArray("fieldInfoList").getJSONObject(i).toMap());
                }
            }
        }

        List<Document> extractedData_1001_1001 = null;
        List<Document> extractedData_1002_1001 = null;
        if(backendConfigration.equals("AE1")){
            extractedData_1001_1001 =connection.getExtractedDataFromDB("AE_CA_POD_2","ExtractedData_1001_1001",docId,Integer.valueOf(parsedDataTextId));
            softAssert.assertTrue(extractedData_1001_1001.size()==fieldInfoModelData_1001.size(), "Rows are not created for number of fields in database");
        }
        else if(backendConfigration.equals("AE2")){
            extractedData_1002_1001 =connection.getExtractedDataFromDB("AE_CA_POD_2","ExtractedData_1002_1001",docId,Integer.valueOf(parsedDataTextId));
            softAssert.assertTrue(extractedData_1002_1001.size()==fieldInfoModelData_1001.size(), "Rows are not created for number of fields in database");
        }

        String categoryModelId_1001 = null;
        String categoryModelPath_1001 = null;
        if(fetchMetaDataResponseJson.get("categoryMetadataInfo").toString().equals("null")){
            logger.info("categoryMetaData is not present");
        }
        else{
            if(fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").getJSONObject("trainingModelInfo").has("modelId")){
            categoryModelId_1001 =fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").getJSONObject("trainingModelInfo").get("modelId").toString();}
            if(fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").getJSONObject("trainingModelInfo").has("modelPath")){
            categoryModelPath_1001 =fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").getJSONObject("trainingModelInfo").get("modelPath").toString();}
        }

        if(categoryModelId_1001 == null){
            logger.info("Category Model is not present");
        }
        else{
            logger.info("Category Model is present " + categoryModelId_1001);
            logger.info("Category Model is present " + categoryModelPath_1001);
        }

        String category = null;
        if(fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").has("categoryId")){
            category =fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").get("categoryId").toString();
        }
        // Test Save Extracted Info API
        int fieldInfoData = 0;
         List<Document> fieldMetaDataInfoFromDB = null;
        if(category!=null && fieldInfoModelData_1001.size() >0){
            fieldInfoData = RandomUtils.nextInt(0,fieldInfoModelData_1001.size());
            fieldMetaDataInfoFromDB = connection.getDBResponse("AE_CA_POD_2","FieldMetaDataInfo","fieldid",Integer.valueOf(fieldInfoModelData_1001.get(fieldInfoData).get("fieldId").toString().trim()));
            logger.info("Field Meta Data Info for fieldId " + fieldInfoModelData_1001.get(fieldInfoData).get("fieldId").toString().trim() + " is" + fieldMetaDataInfoFromDB);
        }

        if(backendConfigration.equals("AE1")) {
            extractedData_1001_1001 = connection.getExtractedDataFromDB("AE_CA_POD_2", "ExtractedData_1001_1001", docId, Integer.valueOf(parsedDataTextId));
            extractedData_1001_1001 = extractedData_1001_1001.stream().filter(m->m.size()>11).collect(Collectors.toList());
            softAssert.assertTrue(extractedData_1001_1001.stream().map(m -> m.get("extractionstatus").toString()).collect(Collectors.toSet()).contains("3"), "Status is not 3 which is not expected");
        }
        else if(backendConfigration.equals("AE2")){
            extractedData_1002_1001 = connection.getExtractedDataFromDB("AE_CA_POD_2", "ExtractedData_1002_1001", docId, Integer.valueOf(parsedDataTextId));
            extractedData_1002_1001 = extractedData_1002_1001.stream().filter(m->m.size()>11).collect(Collectors.toList());
            softAssert.assertTrue(extractedData_1002_1001.stream().map(m -> m.get("extractionstatus").toString()).collect(Collectors.toSet()).contains("3"), "Status is not 3 which is not expected");
        }

        if(fieldInfoModelData_1001.size()>0) {
            StringBuilder taggedTextBuilder = new StringBuilder();
            List<String> taggedData = new LinkedList<>();
            String text = fetchMetaDataResponseJson.get("text").toString().replaceAll("\\s+", " ");
            String taggedInputStart = "<START:" + fieldInfoModelData_1001.get(fieldInfoData).get("tagIdentifier").toString() + ">";
            String taggedInputEnd = "<END>";
            String[] textBreak = text.split(" ");
            for (int i = 0; i < textBreak.length; i++) {
                if (i % 2 == 0) {
                    taggedTextBuilder.append(textBreak[i] + " ");
                } else if (i % 2 == 1) {
                    taggedTextBuilder.append(taggedInputStart + textBreak[i] + taggedInputEnd + " ");
                    taggedData.add("\"" + textBreak[i] + "\"");
                }
            }

            String payload = "{ \"clientId\":" + clientId + ",\"textId\":" + Integer.valueOf(parsedDataTextId) + ",\"documentId\":" + docId + ",\"extractionTypeId\":1001,\"status\":3,\"taggedInputText\":" + "\"" + taggedTextBuilder.toString() + "\"" + ",\"extractedFieldInfo\":[{\"fieldId\":" + Integer.valueOf(fieldInfoModelData_1001.get(fieldInfoData).get("fieldId").toString()) + ",\"tagIdentifier\":" + "\"" + fieldInfoModelData_1001.get(fieldInfoData).get("tagIdentifier").toString() + "\"" + ",\"extractedText\":" + taggedData + "}]}";
            response = AutoExtractionHelper.hitSaveExtractedInfoApi(aeHostUrl,aePort,"/autoExtraction/saveExtractedInfo/" + algoId, payload);

            softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200, "Response is not valid");
        }
        else{
            logger.warn("No fields are mapped to this category thus no chance to verify extracted data for extraction type 1001");
        }

        //////////////////////////For Extraction Type 1002//////////////////////////////
        query = "/autoExtraction/fetchMetaData/"+clientId+"/"+ parsedDataTextId +"/1001/1002";
        response = AutoExtractionHelper.hitFetchMetaDataApi(aeHostUrl,aePort,query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        fetchMetaDataResponseStr = EntityUtils.toString(response.getEntity());
        fetchMetaDataResponseJson = new JSONObject(fetchMetaDataResponseStr);

        List<Map<String,Object>> fieldInfoModelData_1002 = new LinkedList<>();
        if(fetchMetaDataResponseJson.get("fieldInfoList").toString().equals("null")){
            logger.info("fieldInfoList is not present in Response");
        }
        else {
            int fieldInfoModelLength = fetchMetaDataResponseJson.getJSONArray("fieldInfoList").length();
            if(fieldInfoModelLength>=1) {
                for (int i = 0; i < fieldInfoModelLength; i++) {
                    fieldInfoModelData_1002.add(fetchMetaDataResponseJson.getJSONArray("fieldInfoList").getJSONObject(i).toMap());
                }
            }
        }

        List<Document> extractedData_1001_1002 =connection.getExtractedDataFromDB("AE_CA_POD_2","ExtractedData_1001_1002",docId,Integer.valueOf(parsedDataTextId));

        softAssert.assertTrue(extractedData_1001_1002.size()==fieldInfoModelData_1002.size(), "Rows are not created for number of fields in database");

        String categoryModelId_1002 = null;
        String categoryModelPath_1002 = null;
        if(fetchMetaDataResponseJson.get("categoryMetadataInfo").toString().equals("null")){
            logger.info("categoryMetaData is not present");
        }
        else{
            if(fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").getJSONObject("trainingModelInfo").has("modelId")){
            categoryModelId_1002 =fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").getJSONObject("trainingModelInfo").get("modelId").toString();}
            if(fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").getJSONObject("trainingModelInfo").has("modelPath")){
            categoryModelPath_1002 =fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").getJSONObject("trainingModelInfo").get("modelPath").toString();}
        }

        if(categoryModelId_1002 == null){
            logger.info("Category Model is not present");
        }
        else{
            logger.info("Category Model is present " + categoryModelId_1002);
            logger.info("Category Model is present " + categoryModelPath_1002);
        }

        category = null;
        if(fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").has("categoryId")){
            category =fetchMetaDataResponseJson.getJSONObject("categoryMetadataInfo").get("categoryId").toString();
        }

        // Test Save Extracted Info API
        fieldInfoData = RandomUtils.nextInt(0,fieldInfoModelData_1002.size());
        if(category!=null && fieldInfoModelData_1002.size()>0){
            fieldMetaDataInfoFromDB = connection.getDBResponse("AE_CA_POD_2","FieldMetaDataInfo","fieldid",Integer.valueOf(fieldInfoModelData_1002.get(fieldInfoData).get("fieldId").toString()));
            logger.info("Field Meta Data Info for fieldId " + fieldInfoModelData_1002.get(fieldInfoData).get("fieldId").toString().trim() + " is" + fieldMetaDataInfoFromDB);
        }

        if(fieldInfoModelData_1002.size()>0) {
            StringBuilder taggedTextBuilder = new StringBuilder();
            List<String> taggedData = new LinkedList<>();
            String text = fetchMetaDataResponseJson.get("text").toString().replaceAll("\\s+", " ");
            String taggedInputStart = "<START:" + fieldInfoModelData_1002.get(fieldInfoData).get("tagIdentifier").toString() + ">";
            String taggedInputEnd = "<END>";
            String[] textBreak = text.split(" ");
            for (int i = 0; i < textBreak.length; i++) {
                if (i % 2 == 0) {
                    taggedTextBuilder.append(textBreak[i] + " ");
                } else if (i % 2 == 1) {
                    taggedTextBuilder.append(taggedInputStart + textBreak[i] + taggedInputEnd + " ");
                    taggedData.add("\"" + textBreak[i] + "\"");
                }
            }

            String payload = "{ \"clientId\":" + clientId + ",\"textId\":" + Integer.valueOf(parsedDataTextId) + ",\"documentId\":" + docId + ",\"extractionTypeId\":1002,\"status\":3,\"taggedInputText\":" + "\"" + taggedTextBuilder.toString() + "\"" + ",\"extractedFieldInfo\":[{\"fieldId\":" + Integer.valueOf(fieldInfoModelData_1002.get(fieldInfoData).get("fieldId").toString()) + ",\"tagIdentifier\":" + "\"" + fieldInfoModelData_1002.get(fieldInfoData).get("tagIdentifier").toString() + "\"" + ",\"extractedText\":" + taggedData + "}]}";
            response = AutoExtractionHelper.hitSaveExtractedInfoApi(aeHostUrl,aePort,"/autoExtraction/saveExtractedInfo/1001", payload);

            softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200, "Response is not valid");

            extractedData_1001_1002 = connection.getExtractedDataFromDB("AE_CA_POD_2", "ExtractedData_1001_1002", docId, Integer.valueOf(parsedDataTextId));

            softAssert.assertTrue(extractedData_1001_1002.stream().map(m -> m.get("extractionstatus").toString()).collect(Collectors.toSet()).contains("3"), "Status is not 3 which is not expected");
        }
        else{
            logger.warn("No fields are mapped to this category thus no chance to verify extracted data for extraction type 1002");
        }
        softAssert.assertAll();
    }

    @Test(priority = 6,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestExtractedDataAPI(){
        softAssert = new SoftAssert();
        //Extraction Type 1001
        String query ="/autoExtraction/extractedData?clientId="+ clientId +"&algoId="+algoId+"&extractionTypeId="+1001+"&docId="+ docId+"&fieldIds=null&offset=0&size=0&countQuery=false";
        HttpResponse response = AutoExtractionHelper.hitExtractedDataCategoryFieldsApi(aeHostUrl,aePort,query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not valid");

        //Extraction Type 1002
        query ="/autoExtraction/extractedData?clientId="+ clientId +"&algoId=1001&extractionTypeId="+1002+"&docId="+ docId+"&fieldIds=null&offset=0&size=0&countQuery=false";
        response = AutoExtractionHelper.hitExtractedDataCategoryFieldsApi(aeHostUrl,aePort,query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        softAssert.assertAll();
    }

    @Test(priority = 7,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestExtractedCategories() throws IOException {
        //Extraction Type 1001
        softAssert = new SoftAssert();
        List<Map<String,String>> extractedCategories_1001 = new LinkedList<>();
        Map<String,String> extractedCategory_1001 = null;
        String query = "/autoExtraction/extracted/categories?clientId="+clientId+"&docId="+docId+"&algoId="+algoId+"&extractionTypeId="+1001;
        HttpResponse response = AutoExtractionHelper.hitExtractedDataCategoryFieldsApi(aeHostUrl,aePort,query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        String extractedCategoriesResponse = EntityUtils.toString(response.getEntity());
        if(!extractedCategoriesResponse.equals("null")) {
            JSONArray jsonArray = new JSONArray(extractedCategoriesResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                extractedCategory_1001 = new LinkedHashMap<>();
                extractedCategory_1001.put("id", jsonArray.getJSONObject(i).get("id").toString());
                extractedCategory_1001.put("name", jsonArray.getJSONObject(i).get("name").toString());
                extractedCategories_1001.add(extractedCategory_1001);
            }
        }
        else{
            logger.warn("No Extracted fields are found for extraction type 1001");
        }

        //Extraction Type 1002
        List<Map<String,String>> extractedCategories_1002 = new LinkedList<>();
        Map<String,String> extractedCategory_1002= null;
        query = "/autoExtraction/extracted/categories?clientId="+clientId+"&docId="+docId+"&algoId=1001&extractionTypeId="+1002;
        response = AutoExtractionHelper.hitExtractedDataCategoryFieldsApi(aeHostUrl,aePort,query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        extractedCategoriesResponse = EntityUtils.toString(response.getEntity());
        if(!extractedCategoriesResponse.equals("null")) {
            JSONArray jsonArray = new JSONArray(extractedCategoriesResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                extractedCategory_1002 = new LinkedHashMap<>();
                extractedCategory_1002.put("id", jsonArray.getJSONObject(i).get("id").toString());
                extractedCategory_1002.put("name", jsonArray.getJSONObject(i).get("name").toString());
                extractedCategories_1002.add(extractedCategory_1002);
            }
        }
        else {
            logger.warn("No Extracted fields are found for extraction type 1002");
        }
        softAssert.assertAll();
    }

   @Test(priority = 8,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestExtractedFields() throws IOException {
        //Extraction Type 1001
        softAssert = new SoftAssert();
        List<Map<String,String>> extractedFields_1001 = new LinkedList<>();
        Map<String,String> extractedField_1001 = null;
        String query = "/autoExtraction/extracted/fields?clientId="+clientId+"&docId="+docId+"&algoId="+algoId+"&extractionTypeId="+1001;
        HttpResponse response = AutoExtractionHelper.hitExtractedDataCategoryFieldsApi(aeHostUrl,aePort,query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        String extractedFieldResponse = EntityUtils.toString(response.getEntity());
        if(!extractedFieldResponse.equals("null")) {
            JSONArray jsonArray = new JSONArray(extractedFieldResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                extractedField_1001 = new LinkedHashMap<>();
                extractedField_1001.put("id", jsonArray.getJSONObject(i).get("id").toString());
                extractedField_1001.put("name", jsonArray.getJSONObject(i).get("name").toString());
                extractedFields_1001.add(extractedField_1001);
            }
        }
        else{
            logger.warn("No Extracted fields are found for extraction type 1001");
        }

        //Extraction Type 1002
        List<Map<String,String>> extractedFields_1002 = new LinkedList<>();
        Map<String,String> extractedField_1002= null;
        query = "/autoExtraction/extracted/fields?clientId="+clientId+"&docId="+docId+"&algoId=1001&extractionTypeId="+1002;
        response = AutoExtractionHelper.hitExtractedDataCategoryFieldsApi(aeHostUrl,aePort,query);
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,"Response Code is not valid");
        extractedFieldResponse = EntityUtils.toString(response.getEntity());
        if(!extractedFieldResponse.equals("null")) {
            JSONArray jsonArray = new JSONArray(extractedFieldResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                extractedField_1002 = new LinkedHashMap<>();
                extractedField_1002.put("id", jsonArray.getJSONObject(i).get("id").toString());
                extractedField_1002.put("name", jsonArray.getJSONObject(i).get("name").toString());
                extractedFields_1002.add(extractedField_1002);
            }
        }
        else{
            logger.warn("No Extracted fields are found for extraction type 1002");
        }
        softAssert.assertAll();
    }

    @Test(priority = 9,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestFetchDocAPI() throws Exception {
        // This will corrupt the file this test case is to just to check the Document Save and Fetch Doc API
        softAssert = new SoftAssert();
        logger.info("This test case is just to check fetch document api as this is automatically triggered by scheduler and" +
                "AE service thus this will corrupt the data after auto-extraction is completed");

        //Fetch Document API Check
        String queryString ="/autoExtraction/fetchDocument";
        HttpHost httpHost = new HttpHost(aeHostUrl,aePort,"http");

        // Get FileName from database - DocumentInfo Collection
        List<Document> uploadedDocs = connection.getDBResponse("AE_CA_POD_2","DocumentInfo","docid",docId);
        List<Object> docData = connection.getDocumentData(uploadedDocs,"fileinfo","filepath");

        softAssert.assertTrue(docData.size() ==1,"Document is not saved in database");
        filePath= (String) docData.get(0);
        filePath = Base64.encodeBase64String(filePath.getBytes());

        // Get param link id from database - DocumentParserParamLinking Collection
        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","DocumentParserParamLinking","docid",docId);
        List<Double> parserParamLinkId = docParserParamLinkingData.stream().map(document -> (Double)document.get("paramdoclinkid")).collect(Collectors.toList());

        Map<String,String> params = new LinkedHashMap<>();
        params.put("filePath",filePath);
        params.put("docId",String.valueOf(docId));
        params.put("clientId",clientId);
        params.put("statusUpdate","true");
        params.put("docParamLinkId",String.valueOf(parserParamLinkId.get(0)));

        if (params != null) {
            String urlParams = UrlEncodedString.getUrlEncodedString(params);
            queryString += "?" + urlParams;
        }

        HttpGet httpGet = new HttpGet(queryString);
        HttpResponse fetchDocResponse = apiUtils.downloadAPIResponseFile("src/test/output/AutoExtractionDownloadedFiles/AEEnvironmentDownloadedFiles/"+ docData.get(0).toString().split("/")[docData.get(0).toString().split("/").length-1]  ,httpHost,httpGet);

        softAssert.assertTrue(fetchDocResponse.getStatusLine().getStatusCode()==200,"Fetch Doc API is not working");
        softAssert.assertAll();
    }

    @Test(priority = 10,dependsOnMethods = {"TestServiceCheckAPI"})
    public void TestSaveDocumentStatus(){
        softAssert = new SoftAssert();
        List<Document> uploadedDocs = connection.getDBResponse("AE_CA_POD_2","DocumentInfo","docid",docId);
        softAssert.assertTrue(uploadedDocs.size() ==1,"Document is not saved in database");

        List<Document> docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","DocumentParserParamLinking","docid",docId);
        Document paramLinkingDoc = docParserParamLinkingData.stream().findFirst().get();

        Double docParserParamLinkingId = (Double)paramLinkingDoc.get("paramdoclinkid");
        Integer status = (Integer)paramLinkingDoc.get("status");
        Integer textCount = (Integer)paramLinkingDoc.get("textcount");

        Integer updatedStatus = 3;
        Integer updatedTextCount = RandomUtils.nextInt(0,500);

        String payload ="{\"documentId\" :"+ docId +" ,\"clientId\": "+clientId+",\"status\":"+ updatedStatus +" ,\"textCount\":"+ updatedTextCount +" ,\"docParamLinkId\": "+ docParserParamLinkingId +"}";
        HttpResponse docSaveAPIResponse = AutoExtractionHelper.hitSaveDocumentStatusAPI(aeHostUrl,aePort,"/autoExtraction/saveDocumentStatus",payload);

        softAssert.assertTrue(docSaveAPIResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");

        docParserParamLinkingData = connection.getDBResponse("AE_CA_POD_2","DocumentParserParamLinking","docid",docId);
        paramLinkingDoc = docParserParamLinkingData.stream().findFirst().get();

        softAssert.assertTrue(Integer.valueOf(paramLinkingDoc.get("status").toString())==updatedStatus.intValue(),"Status is not updated in db");
        softAssert.assertTrue(Integer.valueOf(paramLinkingDoc.get("textcount").toString())==updatedTextCount.intValue(),"Status is not updated in db");

        //Reverting status and tet count to original
        payload ="{\"documentId\" :"+ docId +" ,\"clientId\": "+clientId+",\"status\":"+ status +" ,\"textCount\":"+ textCount +" ,\"docParamLinkId\": "+ docParserParamLinkingId +"}";
        docSaveAPIResponse = AutoExtractionHelper.hitSaveDocumentStatusAPI(aeHostUrl,aePort,"/autoExtraction/saveDocumentStatus",payload);

        softAssert.assertTrue(docSaveAPIResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");

        softAssert.assertAll();
    }

   @Test(priority = 11,dependsOnMethods = {"TestServiceCheckAPI"},enabled = false)
    public void TestSaveModelUpload() throws FileNotFoundException {
        softAssert = new SoftAssert();
        File file = new File("src/test/output/AutoExtractionDownloadedFiles/SaveModelUploadFile/RandomNumbers.bin");
        Map<String,String> formData = new HashMap<>();
        Map<String,File> fileToUpload = new HashMap<>();
        fileToUpload.put("inputModelFile",file);
        String categoryId = String.valueOf(RandomUtils.nextInt(0,20));
        formData.put("clientId",clientId);
        formData.put("fieldId","");
        formData.put("categoryId",categoryId);
        formData.put("extractionTypeId","1001");
        formData.put("modelType","2");
        formData.put("modelSubType","2");

        HttpEntity httpEntity = apiUtils.multiPartFormData(formData,fileToUpload);
        HttpResponse saveModelUploadResponse = AutoExtractionHelper.hitSaveModelUploadAPI(aeHostUrl,aePort,"/autoExtraction/saveModelUpload?algoUsed=1002",httpEntity);

        softAssert.assertTrue(saveModelUploadResponse.getStatusLine().getStatusCode() == 200,"Response Code is not valid");

        boolean isAddedPreferredModelDeleted = connection.deleteDocumentFromDB("AE_CA_POD_2","PreferredModel_1002","clientid",Integer.valueOf(clientId));
        softAssert.assertTrue(isAddedPreferredModelDeleted,"Added Preferred model is not deleted");

        boolean isModelInfoDeleted = connection.deleteDocumentFromDB("AE_CA_POD_2","ModelInfo_1002","clientid",Integer.valueOf(clientId));
        softAssert.assertTrue(isModelInfoDeleted,"Model Info is not deleted");

        softAssert.assertAll();
    }

    @AfterClass(alwaysRun = true)
    public void settingNewDocumentId() throws FileNotFoundException, ConfigurationException {
        ParseConfigFile.updateValueInConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,null,"docid",String.valueOf(docId +1));
    }
}
