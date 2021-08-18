package com.sirionlabs.test.listRenderer;

import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.util.*;

/**
 * Created by shivashish on 20/9/17.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestDownloadListData {
    private final static Logger logger = LoggerFactory.getLogger(TestDownloadListData.class);

    static String csrfToken;
    static String outputFilePath;
    static String outputFileFormatForDownloadListWithData;
    static String listRendererConfigFileName;
    static String listRendererConfigFilePath;
    static String entityIdMappingFileName;
    static String entityIdConfigFilePath;
    static List<String> allEntitySection;
    static int maxRandomOptions = 5;
    static Integer size = 20;
    static int offset = 0;
    static String orderByColumnName = "id";
    static String orderDirection = "desc";
    String entitySectionSplitter = ",";
    Boolean testForAllEntities = false;


    DumpResultsIntoCSV dumpResultsObj;
    String TestResultCSVFilePath;
    int globalIndex = 0;


    private List<String> setHeadersInCSVFile() {
        List<String> headers = new ArrayList<String>();
        String allColumns[] = {"Index", "TestMethodName", "EntityName", "TestMethodResult", "Comments", "ErrorMessage"};
        for (String columnName : allColumns)
            headers.add(columnName);
        return headers;
    }

    @BeforeClass
    public void setStaticFields() throws ConfigurationException {
        csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
        outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
        outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");

        listRendererConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListDataConfigFilePath");
        listRendererConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListDataConfigFileName");

        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        // for getting all section
        if (!ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName,
                "testforallentities").trim().equalsIgnoreCase(""))
            testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, "testforallentities"));


        if (!testForAllEntities) {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, "entitytotest").split(entitySectionSplitter));
        } else {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, "allentitytotest").split(entitySectionSplitter));
        }
        // getting all section Ends Here


        if (!ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName,
                "maxrandomoptions").trim().equalsIgnoreCase(""))
            maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName,
                    "maxrandomoptions").trim());

        if (!ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName,
                "size").trim().equalsIgnoreCase(""))
            size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, "size"));

        if (!ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName,
                "offset").trim().equalsIgnoreCase(""))
            offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, "offset"));

        if (!ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName,
                "orderbycolumnname").trim().equalsIgnoreCase(""))
            orderByColumnName = ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, "orderByColumnName");

        if (!ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName,
                "orderdirection").trim().equalsIgnoreCase(""))
            orderDirection = ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, "orderDirection");


        // for Storing the result of Sorting
        int indexOfClassName = this.getClass().toString().split(" ")[1].lastIndexOf(".");
        String className = this.getClass().toString().split(" ")[1].substring(indexOfClassName + 1);
        TestResultCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVFile") + className;
        logger.info("TestResultCSVFilePath is :{}", TestResultCSVFilePath);
        dumpResultsObj = new DumpResultsIntoCSV(TestResultCSVFilePath, className + ".csv", setHeadersInCSVFile());


    }

    @DataProvider(name = "getAllEntitySection", parallel = false)
    public Object[][] getAllEntitySection() throws ConfigurationException {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size()][];

        for (String entitySection : allEntitySection) {
            groupArray[i] = new Object[3];
            //Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, entitySection, "entity_type_id"));
            Integer entitySectionTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
            Integer entitySectionListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

            groupArray[i][0] = entitySection; // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            groupArray[i][2] = entitySectionListId; // EntityURlId
            i++;
        }

        return groupArray;
    }


    private Map<String, String> getDownloadListWithDataPayload(Integer entityTypeId) {
        return this.getDownloadListWithDataPayload(entityTypeId, null);
    }

    private Map<String, String> getDownloadListWithDataPayload(Integer entityTypeId, Map<Integer, String> selectedColumnMap) {
        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;

        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";
        } else {
            String selectedColumnArray = "\"selectedColumns\":[";
            for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
                selectedColumnArray += "{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},";
            }
            selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
            selectedColumnArray += "]";

            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}," + selectedColumnArray + "}";
        }

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }


    @Test(dataProvider = "getAllEntitySection")
    public void testDownloadListWithData(String entityName, Integer entityTypeId, Integer entityListId) {
        CustomAssert csAssert = new CustomAssert();
        logger.info("***************** Validating download List with data for entity {} ************", entityName);
        try {
            if (entityTypeId != 0) {
                ListRendererListData listObj = new ListRendererListData();
                listObj.hitListRendererListData(entityListId);
                String listRendererJsonStr = listObj.getListDataJsonStr();
                if (APIUtils.validJsonResponse(listRendererJsonStr, "[listRendererJsonStr response]")) {
                    String metaDataResponseStr = getMetaDataResponseStr(entityListId, entityName);
                    if (APIUtils.validJsonResponse(metaDataResponseStr, "defaultUserListMetaData Response For Entity : " + entityName)) {
                        Boolean isDownloadAvailable = (new JSONObject(metaDataResponseStr)).getBoolean("download");
                        if (isDownloadAvailable) {
                            JSONObject listRendererDataJson = new JSONObject(listRendererJsonStr);
                            if (listRendererDataJson.getInt("totalCount") == 0)
                                logger.warn("enityName = {} Listing Page doesn't have any data. Hence skipping generation of downloadListWithData file for this entity.", entityName);

                            else {
                                downloadListDataForAllColumns(entityTypeId, entityName, entityListId, csAssert);
                                downloadListDataForSelectedColumns(metaDataResponseStr, entityTypeId, entityName, entityListId, true, csAssert);
                                downloadListDataForSelectedColumns(metaDataResponseStr, entityTypeId, entityName, entityListId, false, csAssert);
                            }

                        } else {
                            logger.warn("download List is not available (since download = false) for entity {} . Hence skipping download data for this entity.", entityName);
                        }
                    } else {
                        csAssert.assertTrue(false, "Default User List Meta Data response is not valid json for entity = " + entityName);
                        logger.error("Default User List Meta Data response is not valid json for entity = " + entityName);
                    }

                }
            } else
                logger.warn("Entity Id not found for the entity {}", entityName);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception occurred in testDownloadListWithData. " + e.getMessage());
            logger.error("Exception occurred in testDownloadListWithData for the entity {}. {} ", entityName, e.getMessage());
        }
        csAssert.assertAll();
    }


    private void downloadListDataForAllColumns(Integer entityTypeId, String entityName, Integer listId, CustomAssert csAssert) {
        Map<String, String> formParam = getDownloadListWithDataPayload(entityTypeId);

        logger.info("formParam is : [{}]", formParam);

        DownloadListWithData downloadListWithData = new DownloadListWithData();
        logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
        HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, listId);

        if (response.getStatusLine().toString().contains("200")) {
            Map<String, String> resultsMap = new HashMap<String, String>();
            resultsMap.put("Index", String.valueOf(++globalIndex));
            resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
            resultsMap.put("EntityName", entityName);
            resultsMap.put("TestMethodResult", "Pass");
            resultsMap.put("Comments", "NA");
            resultsMap.put("ErrorMessage", "NA");
            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
            /*
             * dumping response into file
             * */
            csAssert.assertTrue(dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, "AllColumn"),
                    "ListData Download failed for Entity " + entityName + ", List Id: " + listId + " for All Columns.");
        } else {
            csAssert.assertTrue(false, "ListData Download API Response for Entity " + entityName + ", List Id: " + listId + " for All Columns is not 200");

            Map<String, String> resultsMap = new HashMap<String, String>();
            resultsMap.put("Index", String.valueOf(++globalIndex));
            resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
            resultsMap.put("EntityName", entityName);
            resultsMap.put("TestMethodResult", "Fail");
            resultsMap.put("Comments", "NA");
            resultsMap.put("ErrorMessage", "NA");
            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
        }

    }

    private void downloadListDataForSelectedColumns(String metaDataResponseStr, Integer entityTypeId, String entityName, Integer listId, Boolean isRandomizationRequiredOnColumn, CustomAssert csAssert) throws Exception {
        ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
        Map<Integer, String> columnIdNameMapRandom = listRendererDefaultUserListMetaData.getListColumnIdNameMap(metaDataResponseStr, isRandomizationRequiredOnColumn, maxRandomOptions);

        Map<String, String> formParam = getDownloadListWithDataPayload(entityTypeId, columnIdNameMapRandom);

        logger.info("formParam is : [{}]", formParam);

        DownloadListWithData downloadListWithData = new DownloadListWithData();
        logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
        HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, listId);

        String columnStatus = "DefaultColumn";
        if (isRandomizationRequiredOnColumn)
            columnStatus = "RandomizedColumn";

        if (response.getStatusLine().toString().contains("200")) {
            Map<String, String> resultsMap = new HashMap<String, String>();
            resultsMap.put("Index", String.valueOf(++globalIndex));
            resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
            resultsMap.put("EntityName", entityName);
            resultsMap.put("TestMethodResult", "Pass");
            if (isRandomizationRequiredOnColumn)
                resultsMap.put("Comments", "For RandomizeColumn");
            else
                resultsMap.put("Comments", "NA");
            resultsMap.put("ErrorMessage", "NA");
            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
            /*
             * dumping response into file
             * */
            csAssert.assertTrue(dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, columnStatus),
					"ListData Download failed for Entity " + entityName + ", List Id: " + listId + " for Default Columns.");
        } else {
			csAssert.assertTrue(false, "ListData Download API Response for Entity " + entityName + ", List Id: " + listId + " for Default Columns is not 200");

            Map<String, String> resultsMap = new HashMap<String, String>();
            resultsMap.put("Index", String.valueOf(++globalIndex));
            resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
            resultsMap.put("EntityName", entityName);
            resultsMap.put("TestMethodResult", "Fail");
            if (isRandomizationRequiredOnColumn)
                resultsMap.put("Comments", "For RandomizeColumn");
            else
                resultsMap.put("Comments", "NA");
            resultsMap.put("ErrorMessage", "NA");
            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);

        }


    }

    private String getMetaDataResponseStr(Integer listId, String entityName) {
        String responseStr = null;
        try {
            ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
            metaDataObj.hitListRendererDefaultUserListMetadata(listId);
            responseStr = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();
        } catch (Exception e) {
            logger.error("Exception occurred while hitting metaData API for entity {}", entityName);
        }
        return responseStr;
    }

    /*
     * Below method is for dumping the DownloadListWithData response into file(default format : xlsx). Column status is appended in the file name to differentiate among :
     * 1. All Columns
     * 2. Default Selected Columns
     * 3. Randomized selected Columns
     *
     * featureName parameter is for creating new folder in the output directory. This will help to easily analyse the downloaded files separately for downloadList and downloadListWithData feature.
     * featureName = DownloadData
     * */
    private boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;
            return fileUtil.writeResponseIntoFile(response, outputFile);
        }

        return false;
    }
}
