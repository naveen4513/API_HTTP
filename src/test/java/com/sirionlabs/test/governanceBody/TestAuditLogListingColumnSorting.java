package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class TestAuditLogListingColumnSorting {
    private final static Logger logger = LoggerFactory.getLogger(TestAuditLogListingColumnSorting.class);
    static String coloumnSortingConfigFilePath;
    static String coloumnSortingConfigFileName;
    static int size;
    static int offset;
    static String defaultSize;
    static String orderDirectionDesc;
    static String orderDirectionAsc;
    //	CustomAssert csAssertion;
    String entityIdMappingFileName;
    String entityIdConfigFilePath;
    List<String> allEntitySection;
    List<String> columnsToAvoid;
    String entitySectionSplitter = ";";
    Boolean testForAllEntities = false;
    String testForAllColumnsGlobal;
    private AuditLog auditlog;
    private String filtermap;
    private String AuditLogConfigFileName;


    int globalIndex = 0;
    List<String> columnsToTest;
    PostgreSQLJDBC postgreSQLJDBC;




    // beforeClass Helper Method
    public void getTestColoumnSortingConfigData() throws ParseException, IOException, ConfigurationException {
        logger.info("Getting Test Data");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
    }

    public void getListDataConfigData() throws ParseException, IOException, ConfigurationException {
        logger.info("Getting Test Data for listData api");
        coloumnSortingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogConfigFilePath");
        coloumnSortingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogSortingConfigFileName");
        AuditLogConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogConfigFileName");
        filtermap = ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, AuditLogConfigFileName, "filterjson", "86");


        entitySectionSplitter = ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName, "entitysectionsplitter");
        // for getting all section
        if (!ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName,
                "testforallentities").trim().equalsIgnoreCase(""))
            testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName, "testforallentities"));


        if (!testForAllEntities) {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName, "entitytotest").split(entitySectionSplitter));
        } else {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName, "allentitytotest").split(entitySectionSplitter));
        }
        logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size() - 1);

        size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName,
                "size"));
        offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName,
                "offset"));
        defaultSize = ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName,
                "defaultsize");
        orderDirectionDesc = ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName,
                "orderdirectiondesc");
        orderDirectionAsc = ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName,
                "orderdirectionasc");


        columnsToAvoid = Arrays.asList(ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName, "columnstoavoid").split(entitySectionSplitter));

        columnsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(coloumnSortingConfigFilePath, coloumnSortingConfigFileName, "columnstotest").split(entitySectionSplitter));

        postgreSQLJDBC = new PostgreSQLJDBC();
        auditlog = new AuditLog();
    }

    @BeforeClass
    @Parameters({"MultiLingual", "TestForAllColumns"})
    public void beforeClass(String multiLingual, String testForAllColumns) throws IOException, ConfigurationException {
        logger.info("In Before Class method");
        getTestColoumnSortingConfigData();
        getListDataConfigData();

        //This qyery is to check if code to be run on multilingual or not
        postgreSQLJDBC.updateDBEntry("update client SET multilanguage_supported = '" + multiLingual + "' where id = 1002");
        testForAllColumnsGlobal = testForAllColumns;
    }


    @BeforeMethod
    public void beforeMethod(Method method) {
        logger.info("In Before Method");
        logger.info("method name is: {} ", method.getName());
        logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

    }


    @DataProvider(name = "getAllEntitySection", parallel = true)
    public Object[][] getAllEntitySection() throws ConfigurationException {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size()][];

        for (String entitySection : allEntitySection) {
            groupArray[i] = new Object[3];
            try {

                Integer entitySectionURLId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));
                String entitySectionTypeId = String.valueOf(ConfigureConstantFields.getEntityIdByName(entitySection));
                groupArray[i][0] = entitySection.trim(); // EntityName
                groupArray[i][1] = entitySectionURLId; // EntitySectionURLId
                groupArray[i][2] = entitySectionTypeId; //entityTypeId
            } catch (Exception e) {
                logger.error("Failed in Getting entitySectionURLId for : {}", entitySection);
            }
            i++;
        }

        return groupArray;
    }

    /* helper method of verifyListDataResponseForAllEntity which will return the Map of all the QueryName(Column Db Name) with the flag  of whether the QueryName(Column Db Name) is
     sortable of not and QueryName(Column Db Name) type */
    public List<Map<String, String>> getAllColumnQueryNameForSorting(String entitySection, String entityTypeId) throws Exception {

        List<Map<String, String>> allColumnQueryName;

        HashMap<String,String> param = new HashMap<>();
        param.put("entityTypeId",entityTypeId);

        ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
        listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(61,param);

        if (listRendererDefaultUserListMetaData == null) {
            Assert.assertTrue(false, "listRendererDefaultUserListMetaData API Response Code for Entity: " + entitySection + " is incorrect");
        }
        Assert.assertTrue(listRendererDefaultUserListMetaData.getApiStatusCode().contains("200"), "listRendererDefaultUserListMetaData API Response Code for Entity: " + entitySection + " is incorrect");
        Assert.assertTrue(APIUtils.validJsonResponse(listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr()), "listRendererDefaultUserListMetaData API Response for Entity: " + entitySection + " is not valid Json");


        allColumnQueryName = listRendererDefaultUserListMetaData.getAllQueryName();
        logger.debug("allColumnQueryName is : {}", allColumnQueryName);


//		return true;
        return allColumnQueryName;
    }


    /* helper method of getting total number of Records from ListRendered API Response and verify the API ListRendered API */
    public int getTotalRecordsCountForEntityName( ListRendererListData listRendererListData, String entitySection, Integer entitySectionURLId, CustomAssert csAssertion) throws Exception {

        int numberOfRecords = -1;


        if (listRendererListData == null) {
            Assert.assertTrue(false, "listRendererListData Response Code is incorrect for entity : {} " + entitySection);
            csAssertion.assertTrue(false, "listRendererListData list data response is not 200");
        }
        Assert.assertTrue(listRendererListData.getApiStatusCode().contains("200"), "listRendererListData Response Code is incorrect for entity : {} " + entitySection);
        Assert.assertTrue(APIUtils.validJsonResponse(listRendererListData.getListDataJsonStr()), "listRendererListData API Response is not valid Json for entity :  " + entitySection);
        numberOfRecords = listRendererListData.getFilteredCount();

        if (numberOfRecords < -1) {
            logger.warn("There is no data for entity : {} ", entitySection);
        }

        return numberOfRecords;

    }

    /* helper method of getting total number of Pages*/
    public int getNumberOfPages(int numberOfRecords, int size) throws Exception {

        int numberOfPages = 0;

        if (numberOfRecords % size == 0)
            numberOfPages = numberOfRecords / size;
        else
            numberOfPages = ((numberOfRecords / size) + 1);

        return numberOfPages;

    }


    @Test(dataProvider = "getAllEntitySection")
    public void verifyAuditLogTabSorting(String entitySection, Integer entitySectionURLId, String entitySectionTypeId) throws Exception {

        CustomAssert csAssertion = new CustomAssert();
        List<Map<String, String>> allColumnQueryName;

        logger.info("Verifying the Sorting of Columns for the audit log tab listing of EntityName : [{}]", entitySection);

        ListRendererListData listRendererListData = new ListRendererListData();
        listRendererListData.hitListRendererListData(entitySectionURLId, true);
        String listRendererJsonStr = listRendererListData.getListDataJsonStr();


        boolean statusCodeVerification;
        boolean isResponseJson;
        int index = 0;
        String listDataResponse;

        try {
            int getTotalRecordsOfEntity = getTotalRecordsCountForEntityName(listRendererListData,entitySection, entitySectionURLId, csAssertion);
            if (getTotalRecordsOfEntity <= 0) {
                logger.warn("There is no Record for Entity : " + entitySection + " in List Page");
                return;
            }

            String columnId = JSONUtility.parseJson(listRendererJsonStr,"$.data[0].[*][?(@.columnName=='id')].columnId").toString();
            List<Integer> entityIdList = listRendererListData.getAllRecordDbId(Integer.parseInt(columnId.substring(1,columnId.length()-1)),listRendererJsonStr);
            int entityId = entityIdList.get(0);

            logger.info("hitting audit log tab api of entity "+ entityId+" and entity "+ entitySection);

            String auditlog_response = auditlog.hitAuditLogDataApi(entitySectionTypeId,String.valueOf(entityId),filtermap);
            JSONArray data =(JSONArray) JSONUtility.parseJson(auditlog_response, "$.data");

           int  getTotalAuditLogForEntity = data.size();

            int numberOfPages = getNumberOfPages(getTotalAuditLogForEntity, size);
            allColumnQueryName = getAllColumnQueryNameForSorting(entitySection, entitySectionTypeId);

            if (allColumnQueryName.isEmpty()) {
                Assert.assertTrue(false, "Error : Default Audit Log List Data API is failing not being able to get all the column name " + entitySection);
            }


            if (allColumnQueryName.size() > 0) {
                logger.debug("Total Column for Entity : {} is : {}", entitySection, allColumnQueryName.size());
                int columnsChecked = 0;
                int columnsNumberTobeChecked = 10;
                String columnType;
                String columnName = "";


                for (Map<String, String> columnQueryNameHashMap : allColumnQueryName) {

                    columnsChecked = columnsChecked + 1;

                    if (testForAllColumnsGlobal.equalsIgnoreCase("false")) {
                        if (columnsChecked == columnsNumberTobeChecked) {
                            break;
                        }
                    }
                    // if List Page Can Be Sorted with this Particular Column and it's not a dynamic field or checkbox field
                    if (columnQueryNameHashMap.get("isSortable").toLowerCase().contentEquals("true") &&
                            !columnsToAvoid.contains(columnQueryNameHashMap.get("queryName").toLowerCase()) &&
                            !columnQueryNameHashMap.get("queryName").toLowerCase().contains("dyn")) {

//						if (!columnsToTest.contains(columnQueryNameHashMap.get("queryName"))) {
//							logger.error("ColumnName to be skipped " + columnQueryNameHashMap.get("queryName"));
//
//							continue;
//						}
                        columnType = columnQueryNameHashMap.get("type");
                        columnName = columnQueryNameHashMap.get("name");

                        logger.info("Column to be checked " + columnQueryNameHashMap.get("queryName"));

                        String lastValueOfPrevPageAscOrder = null;
                        String lastValueOfPrevPageDescOrder = null;
                        boolean isFirstPage;


                        logger.debug("Sorting Verification Started for Column name : {} ", columnName);

                        if (!testForAllColumnsGlobal.equalsIgnoreCase("true")) {
                            if (numberOfPages > 10) {
                                numberOfPages = 10;
                            }
                        } else {
                            if (numberOfPages > 30) {
                                numberOfPages = 30;
                            }
                        }
                        for (int i = 0; i < numberOfPages; i++) {
                            isFirstPage = false;
                            if (i == 0)
                                isFirstPage = true;

                            int offsetForAPICall = i * size;
                            logger.info("Sorting Verification Started for Column name : {} , and offset : {} ", columnName, offsetForAPICall);

                            // Sorting Column by Asc Order
                            APIResponse auditLogListData = auditlog.hitAuditLogDataApi(entitySectionTypeId, String.valueOf(entityId), offsetForAPICall, size, columnQueryNameHashMap.get("queryName"), orderDirectionAsc);
                            listDataResponse = auditLogListData.getResponseBody();
                            statusCodeVerification = (auditLogListData.getResponseCode()== 200);
                            isResponseJson = APIUtils.validJsonResponse(auditLogListData.getResponseBody());


                            if (!statusCodeVerification) {
                                logger.error("Audit log listRendererListData API Response Code is not proper for Entity: " + entitySection + " While Sorting with Column : " + columnName + " In Asc Direction ");
                                csAssertion.assertTrue(statusCodeVerification, "Audit log listRendererListData API Response Code for Entity: " + entitySection + " is incorrect " + " While Sorting with Column : " + columnName + " In Asc Direction ");
//
                                break;

                            } else if (!isResponseJson) {
                                logger.error("Audit log listRendererListData API Response is not proper for Entity: " + entitySection + " While Sorting with Column : " + columnName + " In Asc Direction ");
                                csAssertion.assertTrue(isResponseJson, "Audit log listRendererListData API Response for Entity: " + entitySection + " is not valid Json" + " While Sorting with Column : " + columnName + " In Asc Direction ");

                                break;
                            } else {
                                logger.debug("Audit log listRendererListData API Response Code and Response is proper for Entity: " + entitySection + " While Sorting with Column : " + columnName + " In Asc Direction ");
                                List<String> allRecords = listRendererListData.getAllRecordForParticularColumns(Integer.parseInt(columnQueryNameHashMap.get("id")), listDataResponse);
                                if (allRecords.isEmpty()) {
                                    csAssertion.assertTrue(false, "RECORDS EMPTY");
                                    break;
                                }
                                logger.debug("All Records List is : --> {}", allRecords);

                                String valueOfLastRecord = allRecords.get(allRecords.size() - 1);
                                String valueOfFirstRecord = allRecords.get(0);
                                boolean isRecordsSorted = listRendererListData.isRecordsSortedProperly(allRecords, columnType, columnName, orderDirectionAsc, postgreSQLJDBC);
                                csAssertion.assertTrue(isRecordsSorted, "Records are not Properly Sorted even after applying sorting (asc nulls first) for column " + columnQueryNameHashMap.get("queryName"));
                                Map<String, String> resultsMap = new HashMap<String, String>();
                                if (isRecordsSorted) {
                                    if (numberOfPages == 1) {


                                    } else if (isFirstPage) {
//										lastValueOfPrevPageAscOrder = valueOfLastRecord;

                                    } else {

                                        boolean isPaginationCorrect = listRendererListData.isPaginationCorrect(lastValueOfPrevPageAscOrder, valueOfFirstRecord, columnName, columnType, orderDirectionAsc, postgreSQLJDBC);

                                        if (isPaginationCorrect) {
                                            //columnSortingResultInfo = new ColumnSortingResultInfo(String.valueOf(++index), entitySection, columnName, columnType, String.valueOf(offsetForAPICall), String.valueOf(size), orderDirectionAsc, listRendererListData.getApiStatusCode(), String.valueOf(isResponseJson), String.valueOf(isRecordsSorted), "NA", "");
                                            resultsMap.put("Index", String.valueOf(++globalIndex));
                                            resultsMap.put("EntityName", entitySection);
                                            resultsMap.put("ColumnName", columnName);
                                            resultsMap.put("ColumnType", columnType);
                                            resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
                                            resultsMap.put("PageSize", String.valueOf(size));
                                            resultsMap.put("SortingOrder", orderDirectionAsc);
                                            resultsMap.put("StatusCode", listRendererListData.getApiStatusCode());
                                            resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
                                            resultsMap.put("IsResultSorted", String.valueOf(isPaginationCorrect));
                                            resultsMap.put("Comment", "NA");
                                            resultsMap.put("AllRecords", "NA");

                                        } else {
                                            logger.error("lastValueOfPrevPageAscOrder :{}", lastValueOfPrevPageAscOrder);
                                            logger.error("valueOfFirstRecord :{}", valueOfFirstRecord);
                                            csAssertion.assertTrue(false, "Although Result is Sorted but Last Value of Prev Page [" + lastValueOfPrevPageAscOrder + "] is not Less than or equal the first value of Current Page [" + valueOfFirstRecord + "]" + " for column " + columnQueryNameHashMap.get("queryName"));
                                            resultsMap.put("Index", String.valueOf(++globalIndex));
                                            resultsMap.put("EntityName", entitySection);
                                            resultsMap.put("ColumnName", columnName);
                                            resultsMap.put("ColumnType", columnType);
                                            resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
                                            resultsMap.put("PageSize", String.valueOf(size));
                                            resultsMap.put("SortingOrder", orderDirectionAsc);
                                            resultsMap.put("StatusCode", listRendererListData.getApiStatusCode());
                                            resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
                                            resultsMap.put("IsResultSorted", String.valueOf(isPaginationCorrect));
                                            resultsMap.put("Comment", "Although Result is Sorted but Last Value of Prev Page [" + lastValueOfPrevPageAscOrder + "] is not Less than or equal the first value of Current Page [" + valueOfFirstRecord + "]");
                                            resultsMap.put("AllRecords", "NA");

                                        }

                                        lastValueOfPrevPageAscOrder = valueOfLastRecord;

                                    }

                                } else {

                                    lastValueOfPrevPageAscOrder = valueOfLastRecord;

                                }
                            }


                            // Sorting Column by Desc Order
                            APIResponse auditLogListDataDesc = auditlog.hitAuditLogDataApi(entitySectionTypeId, String.valueOf(entityId), offsetForAPICall, size, columnQueryNameHashMap.get("queryName"), orderDirectionDesc);
                            listDataResponse = auditLogListDataDesc.getResponseBody();
                            statusCodeVerification = (auditLogListDataDesc.getResponseCode()== 200);
                            isResponseJson = APIUtils.validJsonResponse(auditLogListDataDesc.getResponseBody());


                            if (!statusCodeVerification) {
                                logger.error("Audit log listRendererListData API Response Code is not proper for Entity: " + entitySection + " While Sorting with Column : " + columnName + " In Desc Direction ");
                                csAssertion.assertTrue(listRendererListData.getApiStatusCode().contains("200"), "listRendererListData API Response Code for Entity: " + entitySection + " is incorrect " + " While Sorting with Column : " + columnName + " In Desc Direction ");

                                break;
                            } else if (!isResponseJson) {
                                logger.error("Audit log listRendererListData API Response is not proper for Entity: " + entitySection + " While Sorting with Column : " + columnName + " In Desc Direction ");
                                csAssertion.assertTrue(isResponseJson, "listRendererListData API Response for Entity: " + entitySection + " is not valid Json" + " While Sorting with Column : " + columnName + " In Desc Direction ");

                                break;
                            } else {
                                logger.debug("Audit log listRendererListData API Response Code and Response is proper for Entity: " + entitySection + " While Sorting with Column : " + columnName + " In Desc Direction ");
                                List<String> allRecords = listRendererListData.getAllRecordForParticularColumns(Integer.parseInt(columnQueryNameHashMap.get("id")), listDataResponse);
                                logger.debug("All Records List is : --> {}", allRecords);

                                String valueOfLastRecord = allRecords.get(allRecords.size() - 1);
                                String valueOfFirstRecord = allRecords.get(0);
                                boolean isRecordsSorted = listRendererListData.isRecordsSortedProperly(allRecords, columnType, columnName, orderDirectionDesc, postgreSQLJDBC);
                                csAssertion.assertTrue(isRecordsSorted, "Records are not Properly Sorted even after applying sorting (desc nulls first) for column " + columnQueryNameHashMap.get("queryName"));

                                if (isRecordsSorted) {

                                    if (numberOfPages == 1) {

                                    } else if (isFirstPage) {
                                        lastValueOfPrevPageDescOrder = valueOfLastRecord;
                                        //lastValueOfPrevPageAscOrder = valueOfLastRecord;

                                    } else {

                                        boolean isPaginationCorrect = listRendererListData.isPaginationCorrect(lastValueOfPrevPageDescOrder, valueOfFirstRecord, columnName, columnType, orderDirectionDesc, postgreSQLJDBC);

                                        if (isPaginationCorrect) {

                                        } else {


                                        }

                                        lastValueOfPrevPageDescOrder = valueOfLastRecord;

                                    }


                                } else {
                                    lastValueOfPrevPageDescOrder = valueOfLastRecord;

                                }




                            }

                            logger.debug("Sorting Verification Ended for Column name : {} ", columnName);

                        }

                    }

                }

            } else {
                logger.info("there is no data (columns) in entity : " + entitySection + "  list page");
            }

        } catch (Exception e) {
            csAssertion.assertTrue(false, "Exception while validating sorting " + e.getStackTrace());

            e.printStackTrace();
        }
        csAssertion.assertAll();
    }




    @AfterMethod
    public void afterMethod(ITestResult result) {
        logger.info("In After Method");
        logger.info("method name is: {}", result.getMethod().getMethodName());
        logger.info("***********************************************************************************************************************");


    }

    @AfterClass
    public void afterClass() {
        logger.info("In After Class method");
    }

}