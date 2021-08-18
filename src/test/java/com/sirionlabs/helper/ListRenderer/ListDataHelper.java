package com.sirionlabs.helper.ListRenderer;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.asserts.SoftAssert;

import java.util.*;
import java.util.regex.Pattern;

public class ListDataHelper {

    private final static Logger logger = LoggerFactory.getLogger(ListDataHelper.class);
    private static Show showObj = new Show();

    public static List<Map<Integer, Map<String, String>>> getListData(String listDataResponse) {
        List<Map<Integer, Map<String, String>>> listData = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            JSONArray listDataArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < listDataArr.length(); i++) {
                Map<Integer, Map<String, String>> columnData = new HashMap<>();
                jsonObj = listDataArr.getJSONObject(i);

                for (String column : JSONObject.getNames(jsonObj)) {
                    Map<String, String> columnDataMap = new HashMap<>();

                    JSONObject columnJsonObj = jsonObj.getJSONObject(column);
                    columnDataMap.put("id", String.valueOf(columnJsonObj.getInt("columnId")));
                    String columnName = columnJsonObj.getString("columnName").trim();
                    columnDataMap.put("columnName", columnName);

                    if (columnJsonObj.isNull("value") || columnJsonObj.get("value").toString().startsWith("[")) {
                        columnDataMap.put("value", null);
                    } else {
                        if (columnName.equalsIgnoreCase("id") || columnName.equalsIgnoreCase("relation") ||
                                columnName.equalsIgnoreCase("contract") || columnName.equalsIgnoreCase("supplier") ||
                                columnName.equalsIgnoreCase("vendorhierarchy")) {
                            if (columnJsonObj.get("value").toString().startsWith("{") || columnJsonObj.get("value").toString().startsWith("["))
                                continue;

                            String[] values = columnJsonObj.getString("value").split(Pattern.quote(":;"));

                            columnDataMap.put("value", values[0]);
                            if (values.length > 1)
                                columnDataMap.put("valueId", values[1]);
                        } else if(columnName.equalsIgnoreCase("name")&& columnJsonObj.getString("value").contains(":;")){
                            String[] values = columnJsonObj.getString("value").split(Pattern.quote(":;"));

                            columnDataMap.put("value", values[0]);
                            if (values.length > 1)
                                columnDataMap.put("valueId", values[1]);
                        }else {
                            columnDataMap.put("value", columnJsonObj.getString("value"));
                        }
                    }
                    columnData.put(Integer.parseInt(column), columnDataMap);
                }
                listData.add(columnData);
            }
        } catch (Exception e) {
            logger.error("Exception while setting List Data in ListRendererListData. {}", e.getMessage());
        }
        return listData;
    }

    public static List<Map<Integer, Map<String, String>>> getListDataToValidate(String listDataResponse, List<String> fieldsToVerify) {
        List<Map<Integer, Map<String, String>>> listData = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            JSONArray listDataArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < listDataArr.length(); i++) {
                Map<Integer, Map<String, String>> columnData = new HashMap<>();
                jsonObj = listDataArr.getJSONObject(i);

                for (String column : JSONObject.getNames(jsonObj)) {
                    Map<String, String> columnDataMap = new HashMap<>();

                    JSONObject columnJsonObj = jsonObj.getJSONObject(column);
                    columnDataMap.put("id", String.valueOf(columnJsonObj.getInt("columnId")));
                    String columnName = columnJsonObj.getString("columnName").trim();

                    if (!fieldsToVerify.contains(columnName)) {
                        continue;
                    }

                    columnDataMap.put("columnName", columnName);

                    if (columnJsonObj.isNull("value") || columnJsonObj.get("value").toString().startsWith("[")) {
                        columnDataMap.put("value", null);
                    } else if(columnName.equalsIgnoreCase("name") && columnJsonObj.getString("value").contains(":;")){
                        String[] values = columnJsonObj.getString("value").split(Pattern.quote(":;"));

                        columnDataMap.put("value", values[0]);
                        if (values.length > 1)
                            columnDataMap.put("valueId", values[1]);

                    }else {
                        if (columnName.equalsIgnoreCase("id") || columnName.equalsIgnoreCase("relation") ||
                                columnName.equalsIgnoreCase("contract") || columnName.equalsIgnoreCase("supplier") ||
                                columnName.equalsIgnoreCase("vendorhierarchy")) {
                            if (columnJsonObj.get("value").toString().startsWith("{") || columnJsonObj.get("value").toString().startsWith("["))
                                continue;

                            String[] values = columnJsonObj.getString("value").split(Pattern.quote(":;"));

                            columnDataMap.put("value", values[0]);
                            if (values.length > 1)
                                columnDataMap.put("valueId", values[1]);
                        } else {
                            columnDataMap.put("value", columnJsonObj.getString("value"));
                        }
                    }
                    columnData.put(Integer.parseInt(column), columnDataMap);
                }
                listData.add(columnData);
            }
        } catch (Exception e) {
            logger.error("Exception while setting List Data in ListRendererListData. {}", e.getMessage());
        }
        return listData;
    }

    public static Integer getColumnIdFromColumnName(String listDataResponse, String columnName) {
        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            if (jsonArr.length() > 0) {
                jsonObj = jsonArr.getJSONObject(0);

                for (String columnId : JSONObject.getNames(jsonObj)) {
                    if (jsonObj.getJSONObject(columnId).getString("columnName").trim().equalsIgnoreCase(columnName))
                        return Integer.parseInt(columnId);
                }
            } else {
                logger.error("Couldn't find any record in List Data Response. Hence couldn't find Id for Column {}", columnName);
                return -1;
            }

            logger.info("Couldn't find Id for Column {}", columnName);
            return -1;
        } catch (Exception e) {
            logger.error("Exception while fetching Column Id of {} in List Data. {}", columnName, e.getStackTrace());
            return -1;
        }
    }

    public static List<String> getAllColumnName(String listDataResponse) {
        List<String> allColumnNames = new LinkedList<>();
        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            if (jsonArr.length() > 0) {
                jsonObj = jsonArr.getJSONObject(0);
                for (String columnId : JSONObject.getNames(jsonObj)) {
                    allColumnNames.add(jsonObj.getJSONObject(columnId).getString("columnName"));
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching Column Names of in List Data");
        }
        return allColumnNames;
    }

    private static Integer getTotalNoOfRecordsInListing(String listDataResponse) {
        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            return jsonObj.getInt("totalCount");
        } catch (Exception e) {
            logger.error("Exception while getting Total No of Records in Listing. {}", e.getMessage());
            return -1;
        }
    }

    /*
    VerifyListingPagination: Currently it only validates that the Total No of Records returned by all pages of Listing is equal to the Total No of Records present for
     the Entity.
     */
    public static void verifyListingPagination(int entityTypeId, int listId, int listDataSize, CustomAssert csAssert) {
        String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

        try {
            ListRendererListData listDataObj = new ListRendererListData();
            String payloadForListData = getDefaultPayloadForListData(entityTypeId, listDataSize);
            listDataObj.hitListRendererListData(listId, payloadForListData);
            String listDataResponse = listDataObj.getListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                Integer totalNoOfRecordsInListing = ListDataHelper.getTotalNoOfRecordsInListing(listDataResponse);

                if (totalNoOfRecordsInListing != -1) {
                    logger.info("Total No of Records found on Listing Page for Entity {}: {}", entityName, totalNoOfRecordsInListing);
                    logger.info("Validating Pagination for {} Listing.", entityName);
                    int offset = 0;
                    int actualRecordsFound = 0;

                    while (offset < totalNoOfRecordsInListing) {
                        //Validate Pagination
                        logger.info("Hitting listData API for Entity {} with Offset {} and Size {}", entityName, offset, listDataSize);

                        payloadForListData = getPayloadForListData(entityTypeId, listDataSize, offset);
                        listDataObj.hitListRendererListData(listId, payloadForListData);
                        listDataResponse = listDataObj.getListDataJsonStr();

                        if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                            int noOfListDataRecordsFound = getNoOfRecordsFoundInListDataResponse(listDataResponse);

                            if (noOfListDataRecordsFound > 0) {
                                actualRecordsFound += noOfListDataRecordsFound;
                                int expectedNoOfRecords = -1;

                                if (actualRecordsFound < totalNoOfRecordsInListing) {
                                    expectedNoOfRecords = listDataSize;
                                } else if (actualRecordsFound == totalNoOfRecordsInListing) {
                                    expectedNoOfRecords = totalNoOfRecordsInListing - offset;
                                }

                                if (noOfListDataRecordsFound != expectedNoOfRecords) {
                                    csAssert.assertTrue(false, "Expected No of Records for List Data API for Entity " + entityName + " with Size " +
                                            listDataSize + " and Offset " + offset + ": " + expectedNoOfRecords + " and Actual No of Records found: " +
                                            noOfListDataRecordsFound);
                                }
                            }
                        } else {
                            csAssert.assertTrue(false, "List Data API Response for Entity " + entityName + " with Offset " + offset +
                                    " and Size " + listDataSize + "is an Invalid JSON.");
                        }
                        offset += listDataSize;
                    }

                    logger.info("Total Records Found. Actual {} and Expected {}", actualRecordsFound, totalNoOfRecordsInListing);
                    if (actualRecordsFound != totalNoOfRecordsInListing) {
                        csAssert.assertTrue(false, "Actual Records found in Listing Pagination of Entity " + entityName + ": " + actualRecordsFound +
                                " and Expected Records: " + totalNoOfRecordsInListing);
                    }
                } else {
                    csAssert.assertTrue(false, "Couldn't get Total No of Records on Listing Page for Entity " + entityName);
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Entity " + entityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Listing Pagination for Entity {}. {}", entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating Listing Pagination for Entity " + entityName + ". " + e.getMessage());
        }
    }

    public static void verifyTabListingPagination(int entityTypeId, int entityId, int tabListId, int listDataSize, CustomAssert csAssert) {
        String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

        try {
            ListRendererTabListData tabListObj = new ListRendererTabListData();
            String payloadForTabListData = getDefaultPayloadForTabListData(listDataSize);
            tabListObj.hitListRendererTabListData(tabListId, entityTypeId, entityId, payloadForTabListData);
            String tabListDataResponse = tabListObj.getTabListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                Integer totalNoOfRecordsInListing = ListDataHelper.getTotalNoOfRecordsInListing(tabListDataResponse);

                if (totalNoOfRecordsInListing != -1) {
                    logger.info("Total No of Records found in Tab Listing Response for Entity {} and Tab List Id {}: {}", entityName, tabListId, totalNoOfRecordsInListing);
                    logger.info("Validating Pagination for {} Tab Listing.", entityName);
                    int offset = 0;
                    int actualRecordsFound = 0;

                    while (offset < totalNoOfRecordsInListing) {
                        //Validate Pagination
                        logger.info("Hitting tabListData API for Entity {} and Tab List Id {} with Offset {} and Size {}", entityName, tabListId, offset, listDataSize);

                        payloadForTabListData = getPayloadForTabListData(listDataSize, offset);
                        tabListObj.hitListRendererTabListData(tabListId, entityTypeId, entityId, payloadForTabListData);
                        tabListDataResponse = tabListObj.getTabListDataJsonStr();

                        if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                            int noOfListDataRecordsFound = getNoOfRecordsFoundInListDataResponse(tabListDataResponse);

                            if (noOfListDataRecordsFound > 0) {
                                actualRecordsFound += noOfListDataRecordsFound;
                                int expectedNoOfRecords = -1;

                                if (actualRecordsFound < totalNoOfRecordsInListing) {
                                    expectedNoOfRecords = listDataSize;
                                } else if (actualRecordsFound == totalNoOfRecordsInListing) {
                                    expectedNoOfRecords = totalNoOfRecordsInListing - offset;
                                }

                                if (noOfListDataRecordsFound != expectedNoOfRecords) {
                                    csAssert.assertTrue(false, "Expected No of Records for Tab List Data API for Entity " + entityName + " with Size " +
                                            listDataSize + " and Offset " + offset + ": " + expectedNoOfRecords + " and Actual No of Records found: " +
                                            noOfListDataRecordsFound);
                                }
                            }
                        } else {
                            csAssert.assertTrue(false, "Tab List Data API Response for Entity " + entityName + " with Offset " + offset +
                                    " and Size " + listDataSize + "is an Invalid JSON.");
                        }
                        offset += listDataSize;
                    }

                    logger.info("Total Records Found. Actual {} and Expected {}", actualRecordsFound, totalNoOfRecordsInListing);
                    if (actualRecordsFound != totalNoOfRecordsInListing) {
                        csAssert.assertTrue(false, "Actual Records found in Listing Pagination of Entity " + entityName + ": " + actualRecordsFound +
                                " and Expected Records: " + totalNoOfRecordsInListing);
                    }
                } else {
                    csAssert.assertTrue(false, "Couldn't get Total No of Records in Tab List Data API Response for Entity " + entityName +
                            " and Tab List Id " + tabListId);
                }
            } else {
                csAssert.assertTrue(false, "Tab List Data API Response for Entity " + entityName + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Validating TabListing Pagination for Entity {}. {}", entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating TabListing Pagination for Entity " + entityName + ". " + e.getMessage());
        }
    }

    public static String getDefaultPayloadForListData(int entityTypeId, int listDataSize) {
        return getPayloadForListData(entityTypeId, listDataSize, 0);
    }

    public static String getPayloadForListData(int entityTypeId, int listDataSize, int offset) {
        return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + listDataSize + ",\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"desc\",\"filterJson\":{}},\"selectedColumns\":[]}";
    }

    private static String getPayloadForListDataValidation(int entityTypeId, int listId, int listDataSize, List<String> fieldsToVerify, int offset) {
        String selectedColumnsJson = "[";

        ListRendererDefaultUserListMetaData defaultObj = new ListRendererDefaultUserListMetaData();
        defaultObj.hitListRendererDefaultUserListMetadata(listId);
        String defaultUserListResponse = defaultObj.getListRendererDefaultUserListMetaDataJsonStr();

        if (!ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            logger.error("DefaultUserListMetaData API Response for List Id {} and EntityTypeId {} is an Invalid JSON.", listId, entityTypeId);
            return null;
        }

        JSONObject defaultJsonObj = new JSONObject(defaultUserListResponse);
        JSONArray jsonArr = defaultJsonObj.getJSONArray("columns");

        for (int i = 0; i < jsonArr.length(); i++) {
            String queryName = jsonArr.getJSONObject(i).getString("queryName").trim();

            if (fieldsToVerify.contains(queryName)) {
                selectedColumnsJson = selectedColumnsJson.concat("{\"columnId\":" + jsonArr.getJSONObject(i).getInt("id") + ",\"columnQueryName\":\"" +
                        queryName + "\"},");
            }
        }

        selectedColumnsJson = selectedColumnsJson.substring(0, selectedColumnsJson.length() - 1);
        selectedColumnsJson = selectedColumnsJson.concat("]");

        return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + listDataSize + ",\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"desc\",\"filterJson\":{}},\"selectedColumns\":" + selectedColumnsJson + "}";
    }

    private static String getDefaultPayloadForTabListData(int listDataSize) {
        return "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":" + listDataSize + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
    }

    private static String getPayloadForTabListData(int listDataSize, int offset) {
        return "{\"filterMap\":{\"entityTypeId\":null,\"offset\":" + offset + ",\"size\":" + listDataSize + ",\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
    }

    private static int getNoOfRecordsFoundInListDataResponse(String listDataResponse) {
        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            return jsonObj.getJSONArray("data").length();
        } catch (Exception e) {
            logger.error("Exception while getting No of Records found in List Data Response. {}", e.getMessage());
            return -1;
        }
    }


    public static void verifyListingRecordsData(int entityTypeId, int listId, int listDataOffset, int listDataSize, int maxNoOfRecordsToValidate, List<String> fieldsToVerify,
                                                Map<String, String> fieldsShowPageObjectMap, String showPageExpectedDateFormat, CustomAssert csAssert) {
        verifyListingRecordsData(entityTypeId, listId, listDataOffset, listDataSize, maxNoOfRecordsToValidate, fieldsToVerify, fieldsShowPageObjectMap,
                showPageExpectedDateFormat, showPageExpectedDateFormat, csAssert);
    }

    public static void verifyListingRecordsData(int entityTypeId, int listId, int listDataOffset, int listDataSize, int maxNoOfRecordsToValidate,
                                                List<String> fieldsToVerify, Map<String, String> fieldsShowPageObjectMap, String showPageExpectedDateFormat,
                                                String listDataExpectedDateFormat, CustomAssert csAssert) {

        String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
        int totalRecordsAvailable = -1;
        int noOfRecordsValidated = 0;
        int idColumnNo = -1;
        Map<String, Integer> fieldToVerifyColumnNoMap = new HashMap<>();
        int listDataSizeToHit = Math.min(listDataSize, maxNoOfRecordsToValidate);
        int recordNo = 0;

        try {
            logger.info("Validating Listing Records Data for Entity {}", entityName);
            logger.info("Maximum No of Records to Validate for Entity {} are: {}", entityName, maxNoOfRecordsToValidate);

            Map<String, String> fieldTypeMap = ShowHelper.getShowPageObjectTypesMap();

            do {
                logger.info("Hitting List Data API for Entity {} with Offset {} and Size {}.", entityName, listDataOffset, listDataSizeToHit);
                ListRendererListData listDataObj = new ListRendererListData();
                String payload = getPayloadForListDataValidation(entityTypeId, listId, listDataSizeToHit, fieldsToVerify, listDataOffset + noOfRecordsValidated);
                listDataObj.hitListRendererListData(listId, payload);
                String listDataResponse = listDataObj.getListDataJsonStr();

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    if (totalRecordsAvailable == -1) {
                        totalRecordsAvailable = ListDataHelper.getTotalListDataCount(listDataResponse);
                        logger.info("Total No of Records Available in Listing for Entity {} are: {}", entityName, totalRecordsAvailable);
                    }

                    List<Map<Integer, Map<String, String>>> listData = getListDataToValidate(listDataResponse, fieldsToVerify);

                    if (listData.size() > 0) {
                        noOfRecordsValidated += listData.size();

                        if(entityTypeId == 181){
                            idColumnNo = getColumnIdFromColumnName(listDataResponse, "name");
                        }
                        if (idColumnNo == -1) {
                            idColumnNo = getColumnIdFromColumnName(listDataResponse, "id");
                        }

                        for (Map<Integer, Map<String, String>> listDataRecordMap : listData) {
                            logger.info("***************************************************************************");
                            int recordId = Integer.parseInt(listDataRecordMap.get(idColumnNo).get("valueId"));

                            logger.info("Validating Record #{} having Id {} for Entity {}", ++recordNo, recordId, entityName);
                            logger.info("Hitting Show API for Entity {} and Record Id {}", entityName, recordId);
                            showObj.hitShowVersion2(entityTypeId, recordId);
                            String showResponse = showObj.getShowJsonStr();

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (ShowHelper.isShowPageAccessible(showResponse)) {
                                    for (String field : fieldsToVerify) {
                                        try {


                                            logger.info("Verifying List Data Field {} for Record #{} having Id {} of Entity {}", field, recordNo, recordId, entityName);
                                            if (!fieldToVerifyColumnNoMap.containsKey(field.trim())) {
                                                fieldToVerifyColumnNoMap.put(field.trim(), getColumnIdFromColumnName(listDataResponse, field));
                                            }

                                            int fieldColumnNo = fieldToVerifyColumnNoMap.get(field.trim());
                                            if (fieldColumnNo == -1) {
                                                logger.warn("Couldn't get Column No of Field {} from ListData Response. Hence couldn't validate data", field);
                                                continue;
                                            }

                                            String expectedValue;
                                            expectedValue = field.trim().equalsIgnoreCase("id") ? listDataRecordMap.get(fieldColumnNo).get("valueId") :
                                                    listDataRecordMap.get(fieldColumnNo).get("value");

                                            if(expectedValue == null){
                                                expectedValue = "null";
                                            }
                                            //Special handling for Title field
                                            if (field.trim().equalsIgnoreCase("title") || field.trim().equalsIgnoreCase("name")) {
                                                String[] temp = expectedValue.split(":;");
                                                expectedValue = temp[0].trim();
                                            }

                                            //Special handling required for SourceId field.
                                            if (field.trim().equalsIgnoreCase("sourceId")) {
                                                validateSourceIdField(entityName, listDataRecordMap.get(fieldColumnNo).get("value"), recordId, csAssert);
                                                continue;
                                            }

                                            if (field.trim().equalsIgnoreCase("parentDocumentId") && expectedValue != null) {
                                                expectedValue = String.valueOf(getRecordIdFromValue(expectedValue));
                                            }

                                            String showPageObjectName = fieldsShowPageObjectMap.get(field).trim();

                                            if (!fieldTypeMap.containsKey(showPageObjectName)) {
                                                fieldTypeMap.put(showPageObjectName,
                                                        ShowHelper.getTypeOfShowField(showResponse, showPageObjectName, recordId, entityTypeId));
                                            }

                                            String fieldType = fieldTypeMap.get(showPageObjectName);

                                            if (fieldType == null) {
                                                csAssert.assertTrue(false, "Couldn't get Type of Field " + field + " having Show Page Object Name [" +
                                                        showPageObjectName + "] from Show API Response for Record Id " + recordId + " and Entity " + entityName +
                                                        ". Hence couldn't validate data");
                                            } else {
                                                /* Below check is to handle inconsistent behaviour of listData api response for Definition entity*/
                                                if (entityTypeId == 138 && expectedValue != null && (field.trim().equalsIgnoreCase("status") ||
                                                        field.trim().equalsIgnoreCase("company_position"))) {
                                                    expectedValue = expectedValue.trim().split(":;")[0].trim();
                                                }

                                                validateListingRecordsDataOnShowPage(showResponse, entityTypeId, entityName, showPageObjectName, expectedValue,
                                                        field, fieldType, recordId, csAssert);
                                            }
                                        } catch (Exception e) {
                                            logger.error("Exception while verifying Listing Data Field {} for Record Id {} of Entity {}. {}", field, recordId,
                                                    entityName, e.getStackTrace());
                                            csAssert.assertTrue(false, "Exception while Verifying Listing Data Field " + field + " for Record Id " +
                                                    recordId + " of Entity " + entityName + ". " + e.getMessage());
                                        }
                                    }
                                } else {
                                    logger.warn("Show Page for Entity {} and Record Id {} is not accessible.", entityName, recordId);
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for Entity " + entityName + " and Record Id " + recordId +
                                        " is an Invalid JSON.");
                            }
                            logger.info("***************************************************************************");
                        }
                    } else {
                        if (getFilteredListDataCount(listDataResponse) < 1) {
                            logger.info("No Record present in List Data Response for Entity {} with ListDataSize {} and ListDataOffset {}", entityName, listDataSizeToHit,
                                    (listDataOffset + noOfRecordsValidated));
                            break;
                        } else
                            csAssert.assertTrue(false, "Couldn't create List Data Map List. Issue in code.");
                    }
                } else {
                    csAssert.assertTrue(false, "List Data API Response for Entity " + entityName + " with Offset " + listDataOffset + " and Size " +
                            listDataSize + " is an Invalid JSON.");

                    FileUtils.saveResponseInFile(entityName + " ListData API HTML.txt", listDataResponse);
                    break;
                }
                listDataSizeToHit = Math.min(maxNoOfRecordsToValidate - noOfRecordsValidated, listDataSize);
            }
            while (noOfRecordsValidated < maxNoOfRecordsToValidate && (listDataOffset + noOfRecordsValidated) < totalRecordsAvailable);

        } catch (Exception e) {
            logger.error("Exception while verifying Listing Records for Entity {}. {}", entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Listing Records for Entity " + entityName + ". " + e.getMessage());
        }
    }

    public static void validateSourceIdField(String entityName, String expectedValue, int recordId, CustomAssert csAssert) {
        try {
            if (entityName.trim().equalsIgnoreCase("contracts")) {
                if (expectedValue != null) {
                    int cdrRecordId = getRecordIdFromValue(expectedValue);

                    if (cdrRecordId == -1) {
                        csAssert.assertTrue(false, "Couldn't get Expected Record Id from Value [" + expectedValue + "].");
                        return;
                    }

                    logger.info("Hitting Show API for CDR Record Id {}", cdrRecordId);
                    Show showObj = new Show();
                    int cdrEntityTypeId = ConfigureConstantFields.getEntityIdByName("contract draft request");
                    showObj.hitShow(cdrEntityTypeId, cdrRecordId);
                    String showResponse = showObj.getShowJsonStr();

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        if (ShowHelper.isShowPageAccessible(showResponse)) {
                            logger.info("Hitting TabListData API for CDR Record Id {} and Tab [Related Contracts]", cdrRecordId);

                            Integer relatedContractsTabId = TabListDataHelper.getIdForTab("contract draft request related contracts");
                            TabListData tabListObj = new TabListData();
                            String payload = "{\"filterMap\":{\"entityTypeId\":" + cdrEntityTypeId + ",\"offset\":0,\"size\":100," +
                                    "\"orderByColumnName\":\"contract_id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
                            String tabListDataResponse = tabListObj.hitTabListData(relatedContractsTabId, cdrEntityTypeId, cdrRecordId, payload);

                            if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                                int contractIdColumnNo = getColumnIdFromColumnName(tabListDataResponse, "contract_id");

                                if (contractIdColumnNo != -1) {
                                    JSONObject jsonObj = new JSONObject(tabListDataResponse);
                                    JSONArray jsonArr = jsonObj.getJSONArray("data");

                                    for (int i = 0; i < jsonArr.length(); i++) {
                                        String contractIdValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(contractIdColumnNo)).getString("value");

                                        if (contractIdValue != null && contractIdValue.trim().contains(String.valueOf(recordId))) {
                                            return;
                                        }
                                    }

                                    csAssert.assertTrue(false, "TabListData API Response for CDR Record Id " + cdrRecordId +
                                            " and Tab [Related Contracts] doesn't contain Contract Record having Id " + recordId);
                                } else {
                                    csAssert.assertTrue(false, "Couldn't get Id of Column contract_id from TabListData Response.");
                                }
                            } else {
                                csAssert.assertTrue(false, "TabListData API Response for CDR Record Id " + cdrRecordId +
                                        " and Tab [Related Contracts] is an Invalid JSON.");
                            }
                        } else {
                            csAssert.assertTrue(false, "Show Page of CDR Record Id " + cdrRecordId + " is not accessible.");
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for CDR Record Id " + cdrRecordId + " is an Invalid JSON.");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying Field 'SourceId' for Entity {} and Record Id {}. {}", entityName, recordId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Field 'SourceId' for Entity " + entityName + " and Record Id " + recordId + ". " +
                    e.getMessage());
        }
    }

    public static void validateListingRecordsDataOnShowPage(String showResponse, int entityTypeId, String entityName, String showPageObjectName, String expectedValue,
                                                            String field, String fieldType, int recordId, CustomAssert csAssert) {
        try {
            boolean positiveTest = expectedValue != null && !expectedValue.trim().equalsIgnoreCase("") && !expectedValue.trim().equalsIgnoreCase("null");

            if(field.contains("_logic")){

                validateFieldsAccToLogic(field);

                return;
            }
            switch (fieldType.trim().toLowerCase()) {


                case "select":
                    validateListingRecordsDataOfSelectType(showResponse, entityTypeId, entityName, expectedValue, showPageObjectName, field, recordId, csAssert);
                    break;

                case "stakeholders":
                    validateListingRecordsDataOfStakeholdersType(showResponse, entityTypeId, entityName, showPageObjectName, expectedValue, recordId, csAssert);
                    break;

                default:
                    ShowHelper.verifyShowField(showResponse, showPageObjectName, fieldType, expectedValue, entityTypeId, recordId, csAssert, positiveTest);
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Listing Records Data on Show Page for Field {} of Record Id {} of Entity {}. {}", field, recordId, entityName,
                    e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating Listing Records Data on Show Page for Field " + field + " of Record Id " + recordId +
                    " of Entity " + entityName + ". " + e.getMessage());
        }
    }

    private static void validateListingRecordsDataOfSelectType(String showResponse, int entityTypeId, String entityName, String expectedValueStr, String showPageObjectName,
                                                               String field, int recordId, CustomAssert csAssert) {
        try {
            List<String> expectedValuesList = new ArrayList<>();

            if (expectedValueStr == null) {
                expectedValuesList.add(null);
            } else if (expectedValueStr.equalsIgnoreCase("null")) {
                expectedValuesList.add(null);
            } else {
                String[] allExpectedValuesArr = expectedValueStr.split(Pattern.quote(","));

                for (String value : allExpectedValuesArr) {
                    expectedValuesList.add(value.trim());
                }
            }

            for (String expectedValue : expectedValuesList) {
                boolean positiveTest = expectedValue != null && !expectedValue.trim().equalsIgnoreCase("");

                ShowHelper.verifyShowField(showResponse, showPageObjectName, "select", expectedValue, entityTypeId, recordId, csAssert, positiveTest);
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field {} of Record Id {} of Entity {}. {}", field, recordId, entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating Field " + field + " of Record Id " + recordId + " of Entity " + entityName + ". " +
                    e.getMessage());
        }
    }

    private static void validateListingRecordsDataOfStakeholdersType(String showResponse, int entityTypeId, String entityName, String showPageObjectName,
                                                                     String expectedValueStr, int recordId, CustomAssert csAssert) {
        try {
            List<String> expectedValuesList = new ArrayList<>();

            if (expectedValueStr == null) {
                expectedValuesList.add(null);
            } else {
                String[] allExpectedValuesArr = expectedValueStr.split(Pattern.quote(","));

                for (String value : allExpectedValuesArr) {
                    expectedValuesList.add(value.trim());
                }
            }

            for (String expectedValue : expectedValuesList) {
                boolean positiveTest = expectedValue != null && !expectedValue.trim().equalsIgnoreCase("");

                if (positiveTest && expectedValue.trim().toLowerCase().contains("#group")) {
                    expectedValue = expectedValue.toLowerCase().replaceAll("#group#", "");
                }

                ShowHelper.verifyShowField(showResponse, showPageObjectName, "stakeholders", expectedValue, entityTypeId, recordId, csAssert, positiveTest);
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field Stakeholders of Record Id {} of Entity {}. {}", recordId, entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating Field Stakeholders of Record Id " + recordId + " of Entity " + entityName +
                    "." + e.getMessage());
        }
    }

    public static int getFilteredListDataCount(String listDataResponse) {
        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            return jsonObj.getInt("filteredCount");
        } catch (Exception e) {
            logger.error("Exception while getting Filtered List Data Count. {}", e.getMessage());
            return -1;
        }
    }

    public static ArrayList<Integer> getAllDbIdsFromListPageResponse(String listDataResponse) {

        ArrayList<Integer> dbIds = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);


            JSONArray data = jsonObj.getJSONArray("data");
            if (data.length() != 0) {


                String keytoFetchId = "";
                JSONObject firstRecord = data.getJSONObject(0);

                Set<String> keys = firstRecord.keySet();

                for (String key : keys) {
                    if (firstRecord.getJSONObject(key).getString("columnName").toLowerCase().contentEquals("id")) {
                        keytoFetchId = key;
                        break;
                    }
                }


                if (!keytoFetchId.isEmpty()) {
                    for (int i = 0; i < data.length(); i++) {
                        String value = data.getJSONObject(i).getJSONObject(keytoFetchId).getString("value").split(":;")[1];
                        dbIds.add(Integer.parseInt(value));
                    }

                }


            }


        } catch (Exception e) {
            logger.error("Exception while getting Filtered List Data Count. {}", e.getMessage());
        }

        return dbIds;
    }

    public static int getTotalListDataCount(String listDataResponse) {
        try {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            return jsonObj.getInt("totalCount");
        } catch (Exception e) {
            logger.error("Exception while getting Total List Data Count. {}", e.getMessage());
            return -1;
        }
    }


    //  this function will return all the Entities Ids from their List Page
    public static List<Integer> getListOfEntityIds(String entityName) {
        int maxRecordsToGet = 10000;
        ListRendererListData listRendererListData = new ListRendererListData();
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        List<Integer> allDBIds = new ArrayList<>();
        String listDataResponse = listRendererListData.listDataResponse(entityTypeId, entityName, maxRecordsToGet);
        logger.debug("List Data API Response : entity={} , response={}", entityName, listDataResponse);

        boolean isListDataValidJson = APIUtils.validJsonResponse(listDataResponse);

        if (isListDataValidJson) {

            JSONObject listDataResponseObj = new JSONObject(listDataResponse);
            int noOfRecords = listDataResponseObj.getJSONArray("data").length();

            if (noOfRecords > 0) {
                listRendererListData.setListData(listDataResponse);
                int columnId = listRendererListData.getColumnIdFromColumnName("id");
                allDBIds = listRendererListData.getAllRecordDbId(columnId, listDataResponse);
            }
        }
        return allDBIds;
    }

    public static int getRecordIdFromValue(String value) {
        int recordId = -1;

        String[] temp = value.trim().split(":;");
        if (temp.length > 1) {
            return Integer.parseInt(temp[1]);
        }
        return recordId;
    }

    public static int getShortCodeIdFromValue(String value) {
        String[] temp = value.trim().split(":;");

        if (temp.length > 1) {
            return Integer.parseInt(temp[0]);
        }

        return -1;
    }

    public static String getListDataResponse(String entityName) {
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        return getListDataResponse(entityName, getDefaultPayloadForListData(entityTypeId, 20));
    }

    public static String getListDataResponse(String entityName, String payload) {
        return getListDataResponse(entityName, payload, null);
    }

    public static String getListDataResponse(String entityName, String payload, Map<String, String> params) {
        ListRendererListData listDataObj = new ListRendererListData();
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);
        logger.info("Hitting ListData API for Entity {}.", entityName);

        if (params == null) {
            listDataObj.hitListRendererListData(listId, payload);
        } else {
            listDataObj.hitListRendererListData(listId, payload, params);
        }
        return listDataObj.getListDataJsonStr();
    }

    public static String getListDataResponseVersion2(String entityName) {
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        return getListDataResponseVersion2(entityName, getDefaultPayloadForListData(entityTypeId, 20));
    }

    public static String getListDataResponseVersion2(String entityName, String payload) {
        return getListDataResponseVersion2(entityName, payload, false);
    }

    public static String getListDataResponseVersion2(String entityName, String payload, boolean isFirstCall) {
        return getListDataResponseVersion2(entityName, payload, isFirstCall, null);
    }

    public static String getListDataResponseVersion2(String entityName, String payload, boolean isFirstCall, Map<String, String> params) {
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);
        return getListDataResponseVersion2(listId, payload, isFirstCall, params);
    }

    public static String getListDataResponseVersion2(int listId, String payload, boolean isFirstCall, Map<String, String> params) {
        ListRendererListData listDataObj = new ListRendererListData();

        if (params == null) {
            params = new HashMap<>();
        }

        params.put("version", "2.0");

        listDataObj.hitListRendererListData(listId, isFirstCall, payload, params);
        return listDataObj.getListDataJsonStr();
    }

    public static String getFieldQueryNameFromName(String defaultUserListDataResponse, String fieldName) {
        logger.info("Getting Query Name for Field Name {}", fieldName);

        if (ParseJsonResponse.validJsonResponse(defaultUserListDataResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListDataResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).getString("name").equalsIgnoreCase(fieldName)) {
                    return jsonArr.getJSONObject(i).getString("queryName");
                }
            }
        }

        return null;
    }

    public static String downloadListDataForAllColumns(String filterJson, Integer entityTypeId, String entityName, Integer listId, CustomAssert csAssert) {

        try {
            String outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
            Map<String, String> formParam = getFormParamDownloadList(entityTypeId, null, filterJson);

            logger.info("formParam is : [{}]", formParam);

            DownloadListWithData downloadListWithData = new DownloadListWithData();
            logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
            HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, listId);

            if (response.getStatusLine().toString().contains("200")) {

                return dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, "AllColumn");

            } else {
                logger.error("Error while downloading list data for all columns for entity name +" + entityName);
                csAssert.assertTrue(false, "Error while downloading list data for all columns for entity name" + entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while downloading list data columns for entity name " + entityName);
            csAssert.assertTrue(false, "Exception while downloading list data columns for entity name " + entityName);
        }
        return null;
    }

    public static String downloadListDataForSelectedColumns(String filterJson, Integer entityTypeId, String entityName, Integer listId, Map<Integer, String> selectedColumnMap, SoftAssert softAssert) {

        try {
            String outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
            Map<String, String> formParam = getFormParamDownloadListForSelectedColumn(entityTypeId, selectedColumnMap, filterJson);

            logger.info("formParam is : [{}]", formParam);

            DownloadListWithData downloadListWithData = new DownloadListWithData();
            logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
            HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, listId);

            if (response.getStatusLine().toString().contains("200")) {

                return dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, "SelectedColumn");

            } else {
                logger.error("Error while downloading list data for all columns for entity name +" + entityName);
                softAssert.assertTrue(false, "Error while downloading list data for all columns for entity name" + entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while downloading list data columns for entity name " + entityName);
            softAssert.assertTrue(false, "Exception while downloading list data columns for entity name " + entityName);
        }
        return null;
    }

    private static Map<String, String> getFormParamDownloadListForSelectedColumn(Integer entityTypeId, Map<Integer, String> selectedColumnMap, String filterJson) {

        int offset = 0;
        int size = 10;
        String jsonData;
        String orderByColumnName = "id";
        String orderDirection = "desc nulls last";
        String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");

        Map<String, String> formParam = new HashMap<>();
        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterJson + "}}}";
        } else {
            String selectedColumnArray = "\"selectedColumns\":[";
            for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
                selectedColumnArray = selectedColumnArray.concat("{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},");
            }
            selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
            selectedColumnArray += "]";

            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterJson + "}}," + selectedColumnArray + "}";
        }

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private static Map<String, String> getFormParamDownloadList(Integer entityTypeId, Map<Integer, String> selectedColumnMap, String filterJson) {

        int offset = 0;
        int size = 10;
        String jsonData;
        String orderByColumnName = "id";
        String orderDirection = "desc nulls last";
        String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");

        Map<String, String> formParam = new HashMap<String, String>();
        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterJson + "}}}";
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

    private static String dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {

        String outputFile = null;
        String outputFileFormatForDownloadListWithData = ".xlsx";
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;
            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status)
                logger.info("DownloadListWithData file generated at {}", outputFile);
        }
        return outputFile;
    }

    public static boolean isListDownloadable(String listDataResponse) {

        JSONObject listDataResponseJson = new JSONObject(listDataResponse);
        int filterCount = Integer.parseInt(listDataResponseJson.get("filteredCount").toString());
        if (listDataResponseJson.has("maxListLimit")) {
            return false;
        }

        return filterCount != 0;
    }

    public static List<String> getColumnIds(String listDataResponse) {

        JSONArray dataArray;
        String columnValue;
        List<String> columnIds = new ArrayList<>();
        if (APIUtils.validJsonResponse(listDataResponse)) {

            JSONObject listDataResponseJson = new JSONObject(listDataResponse);
            JSONObject columnJson;
            JSONArray indColumnJsonArray;
            String idValue;
            dataArray = listDataResponseJson.getJSONArray("data");
            try {
                for (int i = 0; i < dataArray.length(); i++) {
                    columnJson = dataArray.getJSONObject(i);
                    indColumnJsonArray = JSONUtility.convertJsonOnjectToJsonArray(columnJson);

                    innerLoop:
                    for (int j = 0; j < indColumnJsonArray.length(); j++) {

                        if (indColumnJsonArray.getJSONObject(j).get("columnName").toString().equalsIgnoreCase("id")) {
                            columnValue = indColumnJsonArray.getJSONObject(j).get("value").toString();
                            if (columnValue.contains(":;")) {
                                idValue = columnValue.split(":;")[1];
                                columnIds.add(idValue);
                                break innerLoop;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Exception while fetching id from report List Data Response " + e.getMessage());
            }
        } else {
            logger.error("List data Response is  not a valid json");
        }

        return columnIds;
    }

    public static Integer getColumnId(String categoryListingResponse, String columnName) {
        try {
            JSONObject jsonObj = new JSONObject(categoryListingResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            if (jsonArr.length() > 0) {
                jsonObj = jsonArr.getJSONObject(0);

                for (String columnId : JSONObject.getNames(jsonObj)) {
                    if (jsonObj.getJSONObject(columnId).getString("columnName").trim().equalsIgnoreCase(columnName))
                        return Integer.parseInt(columnId);
                }
            } else {
                logger.error("Couldn't find any record in List Data Response. Hence couldn't find Id for Column {}", columnName);
                return -1;
            }

            logger.info("Couldn't find Id for Column {}", columnName);
            return -1;
        } catch (Exception e) {
            logger.error("Exception while fetching Column Id of {} in List Data. {}", columnName, e.getStackTrace());
            return -1;
        }
    }

    public static Integer getLatestRecordId(String entityName) {
        try {
            String listDataResponse = getListDataResponseVersion2(entityName);
            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            String value = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0).getJSONObject(idColumn).getString("value");
            return getRecordIdFromValue(value);
        } catch (Exception e) {
            return -1;
        }
    }

    /*Author : Gaurav Bhadani
    This method is created
    * */
    public static void validateFieldsAccToLogic(String logic){

    }

    public static HashMap<String,String> getFieldNamesOnListing(int listId,CustomAssert customAssert){

        HashMap<String,String> fieldNameMap = new HashMap<>();
        try{

            ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
            listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(listId);
            String listMetaDataResponse = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();

            if(JSONUtility.validjson(listMetaDataResponse)) {
                JSONObject listMetaDataResponseJson = new JSONObject(listMetaDataResponse);
                JSONArray columnsArray = listMetaDataResponseJson.getJSONArray("columns");
                String name;
                String defaultName;
                for(int i =0;i<columnsArray.length();i++){
                    name = columnsArray.getJSONObject(i).get("name").toString();
                    defaultName = columnsArray.getJSONObject(i).get("defaultName").toString();
                    fieldNameMap.put(defaultName,name);
                }

            }else {
                customAssert.assertEquals("List Meta Data Response is an invalid json","List Meta Data Response should be a valid json");
            }


        }catch (Exception e){
            logger.error("Exception while getting field names on listing for list id " + listId);
        }

        return fieldNameMap;
    }
}
