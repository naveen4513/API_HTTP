package com.sirionlabs.test.listdatadownload;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererConfigure;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
//import com.sirionlabs.config.ConfigureEntityTypeIdMapping;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestListDataDownloadValidations{

    private final static Logger logger = LoggerFactory.getLogger(TestListDataDownloadValidations.class);
    private String csrfToken;
    private String outputFilePath;
    private String outputFileFormatForDownloadListWithData;
    private String entityIdMappingFileName;
    private String entityIdConfigFilePath;
    private String orderByColumnName = "id";
    private String orderDirection = "desc";
    private String configFilePath;
    private String configFileName;
    private String adminUserName;
    private String adminPassword;
    private List<String> allEntitySection;
    private List<String> filterSheetToCheckForEntity;

    private Integer size = 10;
    private int offset = 0;

    private Boolean testForAllEntities = false;
    private HashMap<String,TreeMap<Integer,String>> filterOrderNameMapEntityVise = new HashMap<>();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        Check check = new Check();
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListDataDownloadConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ListDataDownloadConfigFileName");
        csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
        outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
        outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");

        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        String entitySectionSplitter = ",";

        // for getting all section
        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "testforallentities").trim().equalsIgnoreCase(""))
            testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testforallentities"));


        if (!testForAllEntities) {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitytotest").split(entitySectionSplitter));
        } else {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "allentitytotest").split(entitySectionSplitter));
        }

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "filtersheettocheckforentity").trim().equalsIgnoreCase(""))
            filterSheetToCheckForEntity = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filtersheettocheckforentity").split(entitySectionSplitter));

        // getting all section Ends Here


//        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
//                "maxrandomoptions").trim().equalsIgnoreCase(""))
//            maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
//                    "maxrandomoptions").trim());

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "size").trim().equalsIgnoreCase(""))
            size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "size"));

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "offset").trim().equalsIgnoreCase(""))
            offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset"));

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "orderbycolumnname").trim().equalsIgnoreCase(""))
            orderByColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderByColumnName");

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "orderdirection").trim().equalsIgnoreCase(""))
            orderDirection = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderDirection");

//        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
//                "adminusername").trim().equalsIgnoreCase(""))
//            adminUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adminusername");
//
//        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
//                "adminpassword").trim().equalsIgnoreCase(""))
//            adminPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adminpassword");


        check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("clientUsername"), ConfigureEnvironment.getEnvironmentProperty("clientUserPassword"));
        for(String entityName : allEntitySection) {
            columnOrderNameFilters(entityName);
        }
        check.hitCheck();
    }

    @Test(dataProvider = "getAllEntitySection")
    public void testListDownload(String entityName, Integer entityTypeId, Integer entityListId) {
        CustomAssert csAssert = new CustomAssert();
        try {
            String outputFileValue;
            String filterJson = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "filterjson", String.valueOf(entityTypeId));

            ListRendererListData listObj = new ListRendererListData();
            ListRendererDefaultUserListMetaData listMetaData = new ListRendererDefaultUserListMetaData();

            listMetaData.hitListRendererDefaultUserListMetadata(entityListId);
            listObj.hitListRendererListData(entityTypeId, offset, size, orderByColumnName, orderDirection, entityListId, filterJson);

            String listRendererJsonStr = listObj.getListDataJsonStr();
            int filteredCount = size;
            try {
                filteredCount = Integer.parseInt(new JSONObject(listRendererJsonStr).get("filteredCount").toString());

            }catch(Exception e){
                logger.error("Exception while getting filtered Count");
            }//Rehiting again with new filter Count
            listObj.hitListRendererListData(entityTypeId, offset, filteredCount, orderByColumnName, orderDirection, entityListId, filterJson);
            listRendererJsonStr = listObj.getListDataJsonStr();

            String listMetaDataResponse = listMetaData.getListRendererDefaultUserListMetaDataJsonStr();

            Map<String,String > columnIdName = columnIdNameMetaData(listMetaDataResponse);

            //TreeMap<Integer,String > filterOrder = columnOrderNameFilters(entityName);

            Map<String, Map<String, String>> rowIdentifierWithColumnValues = listRowColumnMapping(listRendererJsonStr,columnIdName);

            downloadListDataForAllColumns(entityTypeId, entityName, entityListId,csAssert);
            String outputFilePath1 = outputFilePath + "/ListDataDownloadOutput/" + entityName;
            outputFileValue = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"outputFileValue");

            Boolean validationStatus = validateDownloadedExcel(outputFilePath1,outputFileValue, rowIdentifierWithColumnValues,entityName,csAssert);

            if(validationStatus){
                csAssert.assertTrue(true,"Downloaded listing excel verified successfully for entity " + entityName);
            }else {
                csAssert.assertTrue(false,"Downloaded listing excel verified unsuccessfully for entity " + entityName);
            }

        } catch (Exception e) {
            logger.error("Exception while validating downloading list data " + e.getStackTrace());
            csAssert.assertTrue(false,"Exception while validating downloading list data for entity " + entityName);
        }
        csAssert.assertAll();
    }

    private HashMap<String,TreeMap<Integer,String>> columnOrderNameFilters(String entityName){

        String name;
        String listRendererConfigureResponse;
        String filterColumnToSkip;
        List<String> filterColumnToSkipList = new ArrayList<>();
        Integer orderId;
        TreeMap<Integer, String> filterOrderNameMap = new TreeMap<>();

        try {

            String urlId = ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath,entityIdMappingFileName,entityName,"entity_url_id");

            filterColumnToSkip = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filtercolumntoskip",entityName);

            if(filterColumnToSkip != null){
                if(!filterColumnToSkip.equalsIgnoreCase("")) {
                    filterColumnToSkipList = Arrays.asList(filterColumnToSkip.split(","));
                }
            }

            ListRendererConfigure listRendererConfigure = new ListRendererConfigure();
            listRendererConfigure.hitListRendererConfigure(urlId);
            listRendererConfigureResponse = listRendererConfigure.getListRendererConfigureJsonStr();

            JSONArray filterMetaData = new JSONObject(listRendererConfigureResponse).getJSONArray("filterMetadatas");

            for (int i = 0; i < filterMetaData.length(); i++) {
                orderId = (Integer) (filterMetaData.getJSONObject(i).get("order"));
                name = filterMetaData.getJSONObject(i).get("name").toString();
                orderId = orderId * 10000;
                if (filterOrderNameMap.containsKey(orderId)) {
                    while(filterOrderNameMap.containsKey(orderId)){
                        orderId = orderId +1;
                    }

                }

                if(filterColumnToSkipList.contains(name)){
                    logger.info("Column " + name + " to be skipped from validating in filter sheet");
                }else {
                    filterOrderNameMap.put(orderId, name);
                }

            }
        }catch (Exception e){
            logger.error("Exception while creating filterOrderNameMap");
        }

        filterOrderNameMapEntityVise.put(entityName,filterOrderNameMap);

        return filterOrderNameMapEntityVise;
    }

    @DataProvider(name = "getAllEntitySection", parallel = true)
    public Object[][] getAllEntitySection() {

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

    private void downloadListDataForAllColumns(Integer entityTypeId, String entityName, Integer listId,CustomAssert csAssert) {

        try{
            String filterJson = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filterjson",String.valueOf(entityTypeId));
            Map<String, String> formParam = getFormParamDownloadList(entityTypeId,null,filterJson);

            logger.info("formParam is : [{}]", formParam);

            DownloadListWithData downloadListWithData = new DownloadListWithData();
            logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
            HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, listId);

            if (response.getStatusLine().toString().contains("200")) {

    //            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
                /*
                 * dumping response into file
                 * */
                dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, "AllColumn");

            } else {
                logger.error("Error while downloading list data for all columns for entity name +" + entityName);
                csAssert.assertTrue(false,"Error while downloading list data for all columns for entity name" + entityName);
            }
        }catch(Exception e){
            logger.error("Exception while downloading list data columns for entity name " + entityName);
            csAssert.assertTrue(false,"Exception while downloading list data columns for entity name " + entityName);
        }
    }

    private void dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;
            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status)
                logger.info("DownloadListWithData file generated at {}", outputFile);
        }
    }

    private Map<String, String> getFormParamDownloadList(Integer entityTypeId, Map<Integer, String> selectedColumnMap,String filterJson) {

        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData;

        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterJson +"}}}";
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

    private boolean isNumeric(String str) {
        try
        {
            Double.parseDouble(str);
        }
        catch(Exception e)
        {
            return false;
        }
        return true;
    }

    private Map<String, Map<String, String>> listRowColumnMapping(String listRendererJsonStr,Map<String,String > columnIdName){

        JSONObject rowDataJson;
        JSONArray dataJsonArray;

        String columnName = null;
        String columnVal = null;
        String columnId = null;

        Map<String, Map<String, String>> rowIdentifierWithColumnValues = new HashMap<>();
        //Creating a map of column id and column name
        JSONArray dataArray = new JSONObject(listRendererJsonStr).getJSONArray("data");
        int i =0;
        int j =0;
        ArrayList<String> columnIdToSkip = new ArrayList<>();
        columnIdToSkip.add("14492");
        try {

            for (i = 0; i < dataArray.length(); i++) {
                Map<String, String> columnNameVal = new HashMap<>();
                dataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));

                for (j = 0; j < dataJsonArray.length(); j++) {
                    rowDataJson = dataJsonArray.getJSONObject(j);
                    try {
                        columnId = String.valueOf(rowDataJson.get("columnId"));
                        columnVal = String.valueOf(rowDataJson.get("value"));
                        columnName = columnIdName.get(columnId);
                    } catch (Exception e) {
                        logger.error("Error parsing list data json ");
                    }
                    if(columnName == null){
                        continue;
                    }
                    if(columnIdToSkip.contains(columnId)){
                        continue;
                    }
                    if (columnVal.contains(":;")) {
                        String val="";
                        String values[] = columnVal.split("::");
                        for (String value: values) {
                            val = val + value.split(":;")[0] + ", ";
                        }

                        columnVal = val.substring(0, val.length()-2);
                    }
                    if (!columnName.equals("CHECKBOX")) {
                        columnNameVal.put(columnName, columnVal);
                    }
//                if (columnname.equals("id")) {
//                    rowidentifier = columnval;
//                }
                    columnVal = null;
                }
                rowIdentifierWithColumnValues.put(String.valueOf(i), columnNameVal);

            }
        }catch (Exception e){
            logger.error("Exception while creating rowIdentifierWithColumnValues with i = " + i + " and j = " + j);
        }
        return rowIdentifierWithColumnValues;
    }

    private Map<String,String> columnIdNameMetaData(String listMetaDataResponse){

        JSONObject listMetaDataResponseJson = new JSONObject(listMetaDataResponse);
        JSONArray columnsJsonArray = listMetaDataResponseJson.getJSONArray("columns");
        JSONObject columnDetailObject;
        String id;
        String columnName;
        Map<String,String> columnIdNameMetaData = new HashMap<>();

        for(int i = 0;i<columnsJsonArray.length();i++){
            columnDetailObject = columnsJsonArray.getJSONObject(i);
            id = String.valueOf(columnDetailObject.get("id"));
            columnName = String.valueOf(columnDetailObject.get("name")).toUpperCase();
            columnIdNameMetaData.put(id,columnName);
        }
        return columnIdNameMetaData;
    }

    private boolean validateDownloadedExcel(String outputFilePath, String outputFileValue, Map<String, Map<String, String>> rowIdentifierWithColumnValues,String entityName,CustomAssert csAssert) {

        logger.info("Inside validateDownloadedExcel method");

        Boolean excelValidationStatus = true;
//        String sheetName = "Data";

        String sheetName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"datasheetname");

        //Preparing key value pairs for excel data
        try {
            XLSUtils xlsUtils = new XLSUtils(outputFilePath, outputFileValue);

            if (validateMetaDataExcel(xlsUtils, sheetName, entityName, csAssert)) {
                logger.info("Meta data validated successfully for the excel file " + outputFilePath + outputFileValue);
            } else {
                excelValidationStatus = false;
                logger.error("Meta data validated unsuccessfully for the excel file " + outputFilePath + outputFileValue);
                csAssert.assertTrue(false, "Meta data validated unsuccessfully for the excel file " + outputFilePath + outputFileValue);
            }
            if(validateExcelData(xlsUtils, entityName,sheetName, rowIdentifierWithColumnValues,csAssert)){
                logger.info("Data validated successfully for the excel file " + outputFilePath + outputFileValue);
                csAssert.assertTrue(true, "Data validated successfully for the excel file " + outputFilePath + outputFileValue);

            }else {
                logger.error("Data validated unsuccessfully for the excel file " + outputFilePath + outputFileValue);
                csAssert.assertTrue(false, "Data validated unsuccessfully for the excel file " + outputFilePath + outputFileValue);
                excelValidationStatus = false;
            }

            if(validateFilterSheet(xlsUtils, entityName, csAssert)){

                logger.info("Filter sheet validated successfully for the excel file " + outputFilePath + outputFileValue);
                csAssert.assertTrue(true, "Filter sheet validated successfully for the excel file " + outputFilePath + outputFileValue);
            }else {
                logger.error("Filter sheet validated unsuccessfully for the excel file " + outputFilePath + outputFileValue);
                csAssert.assertTrue(false, "Filter sheet validated unsuccessfully for the excel file " + outputFilePath + outputFileValue);
                excelValidationStatus = false;
            }

        }catch (NotOfficeXmlFileException nOFE){
            logger.error("Exception while downloading excel for entity {}",entityName,nOFE.getStackTrace());
            excelValidationStatus = false;
        }
        catch (Exception e){
            logger.error("Exception while validating downloaded excel for entity {}",entityName,e.getStackTrace());
            csAssert.assertTrue(false,"Exception while validating downloaded excel for entity " + entityName+ e.getStackTrace());
            excelValidationStatus = false;
        }

        return excelValidationStatus;
    }

    private boolean validateExcelData(XLSUtils xlsUtils,String entityName,String sheetName,Map<String, Map<String, String>> rowIdentifierWithColumnValues,CustomAssert csAssert){

        Map<String,String> excelRowValue;
        Map<String,String> listRowValMap;
        Map<String,Map<String,String>> excelRowColMapping = new HashMap<>();

        String excelColName = null;
        String excelColValue;
                                                                                            String listColValue;
        String excelRowNumber;
        String excelDate = "";
        List<String> excelColumnsToSkip = new ArrayList<>();

        Double excelColNumValue;
        //Preparing excel map
        int mapKeyRowNum = 0;
        int rowNum = 3;

        Boolean status = true;
        int excelRowCount = xlsUtils.getRowCount(sheetName);
        try {
            excelColumnsToSkip = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelcolumnstoskip", entityName).split(","));
        }catch (Exception e){
            logger.debug("Exception while fetching excelColumnsToSkip value for entity {} ", entityName);
        }
        try {
            for (int excelRowNum = 4; excelRowNum < excelRowCount - 2; excelRowNum++) {

                Map<String, String> colMapping = new HashMap<>();
                for (int columnNum = 0; columnNum < xlsUtils.getColumnCount(sheetName); columnNum++) {

                    colMapping.put(xlsUtils.getCellData(sheetName, columnNum, rowNum), xlsUtils.getCellData(sheetName, columnNum, excelRowNum));

                }
                excelRowColMapping.put(String.valueOf(mapKeyRowNum), colMapping);
                mapKeyRowNum++;
            }
            if(excelRowColMapping.size() == 0){
                logger.error("Excel contains no data for downloaded excel for entity " + entityName);
            }
            //Checking excel data with list data response
            for (Map.Entry<String, Map<String, String>> excelRow : excelRowColMapping.entrySet()) {

                excelRowNumber = excelRow.getKey();
                excelRowValue = excelRow.getValue();

                listRowValMap = rowIdentifierWithColumnValues.get(excelRowNumber);

                if(listRowValMap.size() == 0){
                    logger.error("Listing page contains no data for entity " + entityName);
                }
                for (Map.Entry<String, String> excelRowColValue : excelRowValue.entrySet()) {
                    excelColName = excelRowColValue.getKey();
                    if(excelColName.equals("SERVICE DATE") || excelColName.equals("REPORTING DATE") ||excelColName.equals("SUBMISSION DATE") || excelColName.equals("APPROVAL DATE")){
                        logger.info("Skipping the flow");
                    }
                    if (excelColumnsToSkip.contains(excelColName)) {
                        logger.debug("Excel Column skipped {} for entity {}", excelColName, entityName);
                        continue;
                    }
                    excelColValue = excelRowColValue.getValue();
                    listColValue = listRowValMap.get(excelColName);
                    listColValue = StringEscapeUtils.unescapeHtml4(listColValue);
                    if(listColValue == null && excelColName !=null){
                        listColValue = listRowValMap.get(excelColName.toUpperCase());
                    }
                    if (excelColName.equals("SUPPLIER SCORE")) {
                        listColValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "score", listColValue);
                    }
                    if ((entityName.equals("invoice line item")) && (excelColName.equals("INVOICE ID"))) {
                        if (isNumeric(listColValue)) {
                            if (Integer.parseInt(listColValue) < 10000) {
                                listColValue = "INV0" + listColValue;
                            } else {
                                listColValue = "INV" + listColValue;
                            }
                        }
                    }
                    if ((entityName.equals("child service levels")) && (excelColName.equals("MASTER SL ID"))) {
                        if (isNumeric(listColValue)) {
                            if (Integer.parseInt(listColValue) < 10000) {
                                listColValue = "SL0" + listColValue;
                            } else {
                                listColValue = "SL" + listColValue;
                            }
                        }
                    }
                    if ((entityName.equals("child service levels")) && (excelColName.equals("CHILD SL ID"))) {
                        if (isNumeric(listColValue)) {
                            if (Integer.parseInt(listColValue) < 10000) {
                                listColValue = "CSL0" + listColValue + "/" + excelRowValue.get("MASTER SL ID");
                            } else {
                                listColValue = "CSL" + listColValue + "/" + excelRowValue.get("MASTER SL ID");
                            }
                        }
                    }
                    if ((entityName.equals("child service levels")) && (excelColName.equals("CREDIT/EARNBACK CLAUSE APPLIED"))) {
                        if ((listColValue.equals("false") && excelColValue.equals("false")) ||
                                (listColValue.equals("false") && excelColValue.equals("No")) ||
                                (listColValue.equals("true") && excelColValue.equals("true")) ||
                                (listColValue.equals("true") && excelColValue.equals("Yes"))) {
                            continue;
                        }
                    }
                    if (isNumeric(excelColValue)) {
                        String listColValueMinima = "0";
                        String listColValueMaxima;
                        excelColNumValue = Double.parseDouble(excelColValue);
                        excelColValue = String.valueOf(excelColNumValue);
                        if(excelColValue.contains(".")){
                            String[] excelColVal = excelColValue.split("\\.");
                            excelColValue = excelColVal[0] + "." + excelColVal[1].substring(0,1);
                        }
                        if(listRowValMap.get(excelColName).equals("null")){
                            logger.error("Listing page contains null value for column {} for entity {}",excelColName,entityName);
                        }else {
                            listColValue = String.valueOf(Double.parseDouble(listRowValMap.get(excelColName)));
                            if (listColValue.contains(".")) {
                                String[] listColVal = listColValue.split("\\.");
                                String afterDecimalValue = listColVal[1].substring(0, 1);
                                listColValue = listColVal[0] + "." + afterDecimalValue;
                                if(Integer.parseInt(afterDecimalValue) > 0) {
                                    listColValueMinima = listColVal[0] + "." + String.valueOf(Integer.parseInt(afterDecimalValue) - 1);
                                }
                                listColValueMaxima = listColVal[0] + "." + String.valueOf(Integer.parseInt(afterDecimalValue) + 1);
                                if(excelColValue.equals(listColValue) || excelColValue.equals(listColValueMinima) || excelColValue.equals(listColValueMaxima)){
                                    logger.info("List data column value and excel data column value are same for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                                    continue;
                                }
                            }
                        }
                    }
                    if (excelColName.equals("SL/ KPI MET")) {
                        if (excelColValue.contains(listColValue) || listColValue.contains(excelColValue)
                                || ((excelColValue.equals("-") && listColValue.equals("null")))
                                || (excelColValue.equals("No Data Available") && listColValue.equals("ND"))
                                || (excelColValue.equals("Not Applicable") && listColValue.equals("NA"))
                                || (excelColValue.equals("Low Volume") && listColValue.equals("LV"))
                                || (excelColValue.equals("Not Reported") && listColValue.equals("NR"))
                                || (excelColValue.equals("Work In Progress") && listColValue.equals("WIP"))) {
                            logger.info("Valid value for SL/ KPI MET in the excel sheet");
                            csAssert.assertTrue(true, "Valid value for SL/ KPI MET in the excel sheet");
                        } else {
                            logger.error("Value of ID does not contain the expected value " + listColValue);
                            csAssert.assertTrue(false, "Value of SL/ KPI MET does not contain the expected value " + listColValue);
                            status = false;
                        }
                        continue;
                    }
                    String idMetaData = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"idmetadata");
//                    if (excelColName.equals("ID")) {
                    if (excelColName.equals(idMetaData)) {
                        if (excelColValue.contains(listColValue)) {
                            logger.info("Valid value for ID in the excel sheet");
                            csAssert.assertTrue(true, "Valid value for ID in the excel sheet");
                        } else {
                            logger.error("Value of ID does not contain the expected value " + listColValue);
                            csAssert.assertTrue(false, "Value of ID does not contain the expected value " + listColValue);
                            status = false;
                        }
                        continue;
                    }
                    if (excelColName.equalsIgnoreCase("PARENT DOCUMENT ID") ){
                        if(!excelColValue.contains(listColValue)){
                            csAssert.assertTrue(true, "List data column value and excel data doesn't match for column PARENT DOCUMENT ID");
                        }
                        continue;
                    }
                    if ((listColValue == null) && (excelColValue.equals("-") || excelColValue.equals(""))) {
                        logger.info("List data column value and excel data column value is valid for null value of list data for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                        csAssert.assertTrue(true, "List data column value and excel data column value is valid for null value of list data for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                    } else if ((listColValue.equals("null") || listColValue.equals("")) && (excelColValue.equals("-") || excelColValue.equals(""))) {
                        logger.info("List data column value and excel data column value is valid for null value of list data for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                        csAssert.assertTrue(true, "List data column value and excel data column value is valid for null value of list data for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                    } else if (!(listColValue.equals("null") || listColValue.equals("")) && listColValue.equals(excelColValue)) {
                        logger.info("List data column value and excel data column value are same for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                        csAssert.assertTrue(true, "List data column value and excel data column value are same for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                    } else if (excelColName.contains("DATE") || excelColName.equals("CREATED ON") || excelColName.equals("NOTICE LEAD TIME") || excelColName.equals("REQUESTED ON")) {
                        try {
//                            excelDate = convertlistDate(excelColValue);
                            if(excelColValue.length() > 10){
                                excelColValue = excelColValue.substring(0,10);
                            }
                            if(excelColValue.contains("/")) {
                                excelDate = convExcelColDate(excelColValue,"/");
                            }else if(excelColValue.contains("-")) {
                                excelDate = convExcelColDate(excelColValue,"-");
                            }
                            String excelDateFormat = DateUtils.getDateFormat(excelDate);
                            String listDateFormat = DateUtils.getDateFormat(listColValue);
                            if(!excelDateFormat.equals(listDateFormat)){
                                listColValue = DateUtils.convertDateToAnyFormat(listColValue,excelDateFormat);
                            }
                        } catch (Exception e) {
                            logger.error("Exception while Parsing date " + e.getStackTrace());
                        }

                        if (excelDate.equals(listColValue)) {
                            logger.info("Date values are same for listing page and downloaded excel file for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                            csAssert.assertTrue(true, "Date values are same for listing page and downloaded excel file for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                        } else {
                            logger.error("Date values are different for listing page and downloaded excel file for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                            csAssert.assertTrue(false, "Date values are different for listing page and downloaded excel file for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                            status = false;
                        }
                    }
                    else if(excelColName.equals("AGING")){
                        listColValue = listRowValMap.get(excelColName);
                        excelColValue = excelRowColValue.getValue();
                        csAssert.assertEquals(Math.ceil(Double.parseDouble(listColValue)),Double.parseDouble(excelColValue),"List data column value and excel data column value are different for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));

                    }
                    else if(excelColName.equals("GB ID")){
                        listColValue = listRowValMap.get(excelColName);
                        excelColValue = excelRowColValue.getValue();

                            while(listColValue.length()<5){
                                listColValue = "0"+listColValue;
                            }
                            listColValue = "GB"+listColValue;
                        csAssert.assertEquals(listColValue,excelColValue,"List data column value and excel data column value are different for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));

                    }
                    else if(excelColName.equals("ID MASTER OBLIGATION ")){
                        listColValue = listRowValMap.get(excelColName);
                        excelColValue = excelRowColValue.getValue();

                        while(listColValue.length()<5){
                            listColValue = "0"+listColValue;
                        }
                        listColValue = "OB"+listColValue;
                        csAssert.assertTrue(excelColValue.contains(listColValue),"List data column value and excel data column value are different for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));

                    }
                    else {
                        logger.error("List data column value and excel data column value are not same for excel column " + excelColName + " and row " + (excelRowNumber));
                        logger.error("List data column value : {} Excel Column Value :{} for Excel Column Name :{}",listColValue,excelColValue,excelColName);
                        csAssert.assertTrue(false, "List data column value and excel data column value are different for excel column " + excelColName + " and row " + (excelRowNumber + rowNum));
                        status = false;
                    }

                }
            }
        }catch (Exception e) {
            logger.error("Exception while parsing value from downloaded excel sheet " + sheetName + " row num " + rowNum + " column number " + excelColName);
            status = false;
        }
        return status;
    }

    private boolean validateFilterSheet(XLSUtils xlsUtils,String entityName,CustomAssert csAssert){

        if(!filterSheetToCheckForEntity.contains(entityName)){
            return true;
        }
        Boolean status = true;

//        String filterSheet = "Filter";
        String filterSheet = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filtersheet");

        String filterName;
        TreeMap<Integer,String> filterOrderMap = filterOrderNameMapEntityVise.get(entityName);
        TreeMap<Integer,String> excelFilterOrderMap = new TreeMap<>();
        int colCount = xlsUtils.getColumnCount(filterSheet);

        if(!validateMetaDataExcel(xlsUtils,filterSheet,entityName,csAssert)){
            logger.error("Error while validating meta data for Filter sheet for entity {}",entityName);
            csAssert.assertTrue(false,"Error while validating meta data for Filter sheet for entity " + entityName);
        }
        try {
            for (int i = 0; i < colCount; i++) {

                filterName = xlsUtils.getCellData(filterSheet, i, 3);
                excelFilterOrderMap.put(i, filterName);
            }

            List<String> excelFilterList = new ArrayList<>(excelFilterOrderMap.values());
            List<String> listMetaFilterIdsList = new ArrayList<>(filterOrderMap.values());

            if(excelFilterList.size() == listMetaFilterIdsList.size()) {
            }else {
                for (int i = 0; i <listMetaFilterIdsList.size() ; i++) {
                    listMetaFilterIdsList.removeAll(excelFilterList);
                }
                System.out.println(listMetaFilterIdsList);
                logger.error("Column count different for list meta data and downloaded excel");
                csAssert.assertTrue(false,"Column count different for filter sheet of downloaded list data excel");
            }
            for(int i =0;i<excelFilterList.size();i++) {
                if(excelFilterList.contains(listMetaFilterIdsList.get(i))){

                    logger.info("Expected value of filter column name present in the downloaded excel sheet for the filter sheet for column " + i);
                    csAssert.assertTrue(true,"Expected value of filter column name present in the downloaded excel sheet for the filter sheet for column " + i);
                }
                else {
                    status =false;
                    logger.error("Expected value of filter column name not present in the downloaded excel sheet for the filter sheet for column " + i);
                    csAssert.assertTrue(false,"Expected value of filter column name not present in the downloaded excel sheet for the filter sheet for column " + i);
                }
            }
        }catch (Exception e){
            csAssert.assertTrue(false,"Exception while validating filter column values");
        }
        return status;
    }

    private boolean validateMetaDataExcel(XLSUtils xlsUtils,String sheetName,String entityName,CustomAssert csAssert){

        Boolean status = true;

        String date[] = DateUtils.getCurrentDateInDDMMMYYYY().split("/");
        String downloadedExcelExpectedDate = date[2] + date[1] +date[0];
        String entityNameExpectedInExcel = entityName;
        try {
            entityNameExpectedInExcel = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitynameexpectedinexcel", entityName);
            if(entityNameExpectedInExcel == null){
                entityNameExpectedInExcel = entityName.toUpperCase();
            }
        }catch (Exception e){
            entityNameExpectedInExcel = entityName;
            logger.debug("Exception while getting entity name from excel");
        }
        try {
            if (entityNameExpectedInExcel.toLowerCase().contains(xlsUtils.getCellData(sheetName, 0, 0).toLowerCase())) {
                logger.info("Expected value of entity name present in the downloaded excel sheet for " + entityName + " in the column 0 and row 0 for the sheet " + sheetName);

            } else {
                logger.error("Unexpected value of entity name present in the downloaded excel sheet for " + entityName + "in the column 0 and row 0 for the sheet " + sheetName);
                csAssert.assertTrue(false,"Unexpected value of entity name present in the downloaded excel sheet for " + entityName + "in the column 0 and row 0 for the sheet " + sheetName);
                status = false;
            }

            String dateMetaData = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"datemetadata");

//            if (xlsUtils.getCellData(sheetName, 0, 1).equals("Date :")) {
            if (xlsUtils.getCellData(sheetName, 0, 1).equals(dateMetaData)) {
                logger.info("Expected value Date :  present in the downloaded excel sheet for " + entityName + "in the column 0 and row 1 for the sheet " + sheetName);

            } else {
                logger.error("Expected value Date : present in the downloaded excel sheet for " + entityName + "in the column 0 and row 1 for the sheet " + sheetName);
                csAssert.assertTrue(false,"Expected value Date : present in the downloaded excel sheet for " + entityName + "in the column 0 and row 1 for the sheet " + sheetName);
                status = false;
            }

            String generatedByMetaData = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"generatedbymetadata");

//            if (xlsUtils.getCellData(sheetName, 0, 2).equals("Generated By :")) {
            if (xlsUtils.getCellData(sheetName, 0, 2).equals(generatedByMetaData)) {
                logger.info("Expected value Generated By :  present in the downloaded excel sheet for " + entityName + "in the column 0 and row 2 for the sheet " + sheetName);

            } else {
                logger.error("Expected value Generated By :  not present in the downloaded excel sheet for " + entityName + "in the column 0 and row 2 for the sheet " + sheetName);
                csAssert.assertTrue(false,"Expected value Generated By :  not present in the downloaded excel sheet for " + entityName + "in the column 0 and row 2 for the sheet " + sheetName);
                status = false;
            }

            String generatedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "generatedby");
            if (xlsUtils.getCellData(sheetName, 1, 2).equals(generatedBy)) {
                logger.info("Expected value of Generated By :  present in the downloaded excel sheet for " + entityName + "in the column 0 and row 2 for the sheet " + sheetName);

            } else {
                logger.error("Expected value of Generated By :  not present in the downloaded excel sheet for " + entityName + "in the column 0 and row 2 for the sheet " + sheetName);
                csAssert.assertTrue(false,"Expected value of Generated By :  not present in the downloaded excel sheet for " + entityName + "in the column 0 and row 2 for the sheet " + sheetName);
                status = false;
            }
        }
        catch (Exception e){
            status = false;
            logger.error("Exception while parsing excel file "+ entityName + "  " + e.getMessage());
            e.printStackTrace();
            csAssert.assertTrue(false,"Exception while parsing excel file "+ entityName);
        }
        return status;
    }

    public String convertlistDate(String date){//Date should come in YYYYMMMDD Format else exception is thrown

        String mon = null;
        String dateextracted = null;
        String year = null;
        try {
            year = date.substring(0,4);
            dateextracted = date.substring(7,9);
            mon = date.substring(4,7);

        }catch (Exception e){
            logger.error("Exception while parsing excel date " + e.getStackTrace());
            e.printStackTrace();
        }

        return mon + "-" + dateextracted + "-" + year;
    }

    private String convExcelColDate(String date,String splitter){  //Date should come in MM/DD/YY format amd Processed in MMM-DD-YYYY Format

        String[] monthSubString = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        String[] dateArray;
        String month;
        String convDate = "";
        String dateString;
        int monthInteger;
        Integer dateValue;
        try {
            dateArray = date.split(splitter);
            monthInteger = Integer.parseInt(dateArray[0]);
            month = monthSubString[monthInteger -1];
            dateValue = Integer.parseInt(dateArray[1]);
            if(dateValue < 10){
                dateString = "0" + dateValue;
            }else
            {
                dateString = String.valueOf(dateValue);
            }
            if(dateArray[2].length() < 3) {
                convDate = month + "-" + dateString + "-20" + dateArray[2];
            }else{
                convDate = month + "-" + dateString + "-" + dateArray[2];
            }

        }catch (Exception e){
            logger.error("Exception while converting date");
        }
        return convDate;
    }
}