package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.reportRenderer.ReportRenderListReportJson;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.scheduleReport.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by shivashish on 25/8/17.
 */
public class TestScheduleReport extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestScheduleReport.class);
    ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData;
    ReportRendererFilterData reportRendererFilterData;
    ReportRendererListData reportRendererListData;
    FilterUtils filterUtils;
    Map<Integer, List<String>> allReportAccordingToEntity = new HashMap<>();
    Map<Integer, String> entityNameAccordingToEntityTypeId = new HashMap<>();
    Map<Integer, Map<String, String>> reportNameAccordingToReportId = new HashMap<>();
    String scheduleReportConfigFilePath;
    String scheduleReportConfigFileName;
    String reportRenderCSVDelemeter;
    String reportRenderCSVFile;
    List<String> allEntitySection;

    String baseFilePath;
    String entityIdMappingFileName;
    String dateFormat;
    String verifyFromEmail = null;
    Map<String, Map<String, String>> reportsMap;
    DumpResultsIntoCSV dumpResultsObj;
    String TestResultCSVFilePath;
    String tableName;
    List<String> columnName;
    List<String> comparator;
    List<Object> columnValue;
    int dayWindow;
    List<String> tableColumnNameToSelect;
    String filterRecordsOrderByQuery;
    private ArrayList<String> skipReports;


    @BeforeClass
    public void beforeClass() throws IOException, ConfigurationException {
        logger.info("In Before Class method");
        getReportRenderConfigData();
        testCasesMap = getTestCasesMapping();
    }


    @BeforeMethod
    public void beforeMethod(Method method) {
        logger.info("In Before Method");
        logger.info("method name is: {} ", method.getName());
        logger.info("***********************************************************************************************************************");

    }


    public void getReportRenderConfigData() throws ParseException, ConfigurationException {
        logger.info("Initializing Test Data for Report Render");

        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

        scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFilePath");
        scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFileName");

        verifyFromEmail = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "verifyfromemail");
        skipReports=new ArrayList<>(Arrays.asList(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName,"default", "skipreportids").split(",")));

        allEntitySection = ParseConfigFile.getAllSectionNames(scheduleReportConfigFilePath, scheduleReportConfigFileName);

        reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
        reportRendererFilterData = new ReportRendererFilterData();
        reportRendererListData = new ReportRendererListData();


        filterUtils = new FilterUtils();
        reportRenderCSVFile = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderCSVFile");
        reportRenderCSVDelemeter = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderCSVDelimiter");
        dateFormat = ConfigureConstantFields.getConstantFieldsProperty("DateFormatForReports");

        ReportRenderListReportJson reportRenderListReportJsonObj = new ReportRenderListReportJson();
        reportRenderListReportJsonObj.hitReportRender();
        String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
        logger.debug("The Report Response is : [ {} ]", reportRenderJsonStr);
        JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
        reportsMap = reportRenderListReportJsonObj.generateReportsMap(reportJsonArrayForEntities);


        // for Storing the result of Sorting
        int indexOfClassName = this.getClass().toString().split(" ")[1].lastIndexOf(".");
        String className = this.getClass().toString().split(" ")[1].substring(indexOfClassName + 1);
        TestResultCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVFile") + className;
        logger.info("TestResultCSVFilePath is :{}", TestResultCSVFilePath);
        dumpResultsObj = new DumpResultsIntoCSV(TestResultCSVFilePath, className + ".csv", setHeadersInCSVFile());


    }


    // this method will be called By getTestReportRendererData internally if testableReports is equal to All , It means we want to test all Reports for All Entities
    public Map<Integer, List<String>> getDefaultReportsList() {
        ReportRenderListReportJson reportRenderListReportJsonObj = new ReportRenderListReportJson();
        reportRenderListReportJsonObj.hitReportRender();
        String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
        logger.info("The Report Response is : [ {} ]", reportRenderJsonStr);
        JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
        int noOfEntitiesInReport = reportJsonArrayForEntities.length();
        for (int rc = 0; rc < noOfEntitiesInReport; rc++) {
            try {
                JSONObject obj = reportJsonArrayForEntities.getJSONObject(rc);
                JSONUtility jsonUtilObj = new JSONUtility(obj);
                Integer entityTypeId = jsonUtilObj.getIntegerJsonValue("entityTypeId");
                String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
                entityNameAccordingToEntityTypeId.put(entityTypeId, entityName);
                if (entityName != null) {
                    logger.info("Currently creating for entityName [ {} ] , and  entityTypeId [ {} ] ", entityName, entityTypeId);
                    JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
                    int sizeOfSubReports = subReportsJsonArray.length();
                    List<String> reportsIdsArray = new ArrayList<>();
                    Map<String, String> reportNameAccordingToId = new HashMap<>();
                    for (int sc = 0; sc < sizeOfSubReports; sc++) {
                        JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
                        JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
                        String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
                        String reportName = jsonUtilObjForSubReport.getStringJsonValue("name");
                        String isManualReport = jsonUtilObjForSubReport.getStringJsonValue("isManualReport");
                        if (isManualReport.equalsIgnoreCase("false")&&(!skipReports.contains(reportId))) {
                            reportsIdsArray.add(reportId);
                            reportNameAccordingToId.put(reportId, reportName);
                        }
                    }
                    allReportAccordingToEntity.put(entityTypeId, reportsIdsArray);
                    reportNameAccordingToReportId.put(entityTypeId, reportNameAccordingToId);
                }
            } catch (Exception e) {
                logger.info("Got Exception : [ {} ]", e.getMessage());
                e.printStackTrace();

            }

        }
        return allReportAccordingToEntity;
    }


    /**
     * Here the DAtaProvider will provide Object array on the basis on ITestContext
     *
     * @return
     */
    @DataProvider(name = "TestReportRendererData", parallel = true)
    public Object[][] getTestReportRendererData() throws ConfigurationException {

        logger.info("In the Data Provider");
        List<Object[]> allTestData = new ArrayList<>();
        String testableReports = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "schedulereportsforentities");
        if (testableReports.equalsIgnoreCase("all")) {
            Map<Integer, List<String>> allEntityWithReport = getDefaultReportsList();
            for (Map.Entry<Integer, List<String>> entityWithReport : allEntityWithReport.entrySet()) {
                Integer entity = entityWithReport.getKey();
                List<String> report = entityWithReport.getValue();
                for (String s : report) {
                    ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                    try {
                        reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(Integer.parseInt(s));
                        String reportRendererDefaultUserListMetaDataJsonStr = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                        if (!ParseJsonResponse.validJsonResponse(reportRendererDefaultUserListMetaDataJsonStr)) {
                            allTestData.add(new Object[]{entity, s, -2});
                            continue;
                        }
                        JSONObject jsonObj = new JSONObject(reportRendererDefaultUserListMetaDataJsonStr);
                        JSONUtility json = new JSONUtility(jsonObj);
                        if (jsonObj.has("body") && json.getStringJsonValue("body") == null) {
                            logger.info("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
                            allTestData.add(new Object[]{entity, s, -2});
                            continue;
                        }
                        JSONObject reportMetadataJson = json.getJsonObject("reportMetadataJson");
                        boolean isListing = reportMetadataJson.getBoolean("isListing");
                        if (isListing) {
                            allTestData.add(new Object[]{entity, s, 0});
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            for (String entitySection : allEntitySection) {
                logger.debug("entitySection :{}", entitySection);
                if (!entitySection.equalsIgnoreCase("default")) {
                    Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
                    List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(scheduleReportConfigFilePath, scheduleReportConfigFileName, entitySection);
                    for (String entitySpecificProperty : allProperties) {
                        if (entitySpecificProperty.contentEquals("test")) {
                            String reportsIds = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, entitySection, entitySpecificProperty);
                            logger.debug("entitySection :{} ,reportsIds :{}", entitySection, reportsIds);
                            String[] reportsIdsArray = reportsIds.split(",");
                            for (String s : reportsIdsArray) {
                                ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                                try {
                                    reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(Integer.parseInt(s));
                                    String reportRendererDefaultUserListMetaDataJsonStr = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                                    if (!ParseJsonResponse.validJsonResponse(reportRendererDefaultUserListMetaDataJsonStr)) {
                                        allTestData.add(new Object[]{entityTypeId, s, -2});
                                        continue;
                                    }
                                    JSONObject jsonObj = new JSONObject(reportRendererDefaultUserListMetaDataJsonStr);
                                    JSONUtility json = new JSONUtility(jsonObj);
                                    if (jsonObj.has("body") && json.getStringJsonValue("body") == null) {
                                        logger.info("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
                                        allTestData.add(new Object[]{entityTypeId, s, -2});
                                        continue;
                                    }
                                    JSONObject reportMetadataJson = json.getJsonObject("reportMetadataJson");
                                    boolean isListing = reportMetadataJson.getBoolean("isListing");
                                    if (isListing) {
                                        allTestData.add(new Object[]{entityTypeId, s, 0});
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
            logger.debug("Formed the parameter Array to be tested");
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "TestReportRendererData", priority = 1)
    public void testScheduleReport(int entityTypeId, String reportId, int flag) {
        CustomAssert customAssert = new CustomAssert();
        try {
            if (flag == -2) {
                customAssert.assertTrue(false, "ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
                throw new SkipException("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
            }
            logger.info("###################################################:Tests Starting for ReportId:{}##################################################################", reportId);
            logger.info("Report Id is :  [{}]", reportId);

            // Hitting the CreateScheduleReportForm API and It it's Succeed then only we will hit CreateScheduleReport API
            // then why validation has hard Assert for this API

            CreateScheduleReportForm createScheduleReportForm = new CreateScheduleReportForm();
            HashMap<String, String> queryStringParams = new HashMap<>();
            queryStringParams.put("id", String.valueOf(reportId));
            createScheduleReportForm.hitCreateReportFormAPI(queryStringParams);


             customAssert.assertTrue(createScheduleReportForm.getApiStatusCode().contains("200"), "API status code is not correct While hitting CreateScheduleReportForm API for Report Id " + reportId);

             customAssert.assertTrue(APIUtils.validJsonResponse(createScheduleReportForm.getResponseCreateReportFormAPI()), "CreateScheduleReportFrom API Response in not a valid Json");

            // Hitting the CreateScheduleReport API
            CreateScheduleReport createScheduleReport = new CreateScheduleReport(createScheduleReportForm.getResponseCreateReportFormAPI(), entityTypeId);
            createScheduleReport.hitCreateScheduleReportAPI();

            customAssert.assertTrue(createScheduleReport.getApiStatusCode().contains("200"), "API status code is not correct While hitting CreateScheduleReport API for Report Id " + reportId);

            // Hitting the ScheduleByMeReport API
            ScheduleByMeReportAPI scheduleByMeReportAPI = new ScheduleByMeReportAPI(entityTypeId, Integer.parseInt(reportId));
            scheduleByMeReportAPI.hitScheduleByMeReportAPI();

            customAssert.assertTrue(scheduleByMeReportAPI.getApiStatusCode().contains("200"), "API status code is not correct While hitting ScheduleByMeReport API for Report Id " + reportId);
            customAssert.assertTrue(APIUtils.validJsonResponse(scheduleByMeReportAPI.getResponseScheduleByMeReportAPI()), "ScheduleByMeReport API Response in not a valid Json");

            // verifying the ScheduleByMeReport Response it should have CreateScheduleReport Entry with active status
            customAssert.assertTrue(createScheduleReport.validateScheduleByMeReportAPIReponse(scheduleByMeReportAPI.getResponseScheduleByMeReportAPI()), "ScheduleByMeReport API Response don't have any entry having earlier created schedule Report for report id : " + reportId);


            int dbIdOfTheReport = createScheduleReport.getIdOfCreatedReport(scheduleByMeReportAPI.getResponseScheduleByMeReportAPI());
            logger.info("Db Id Of The Schedule Report is : [{}]", dbIdOfTheReport);

            if (dbIdOfTheReport != -1) {
                // Hitting the UpdateScheduleReport Form API
                UpdateScheduleReportForm updateScheduleReportForm = new UpdateScheduleReportForm(dbIdOfTheReport);
                queryStringParams = new HashMap<>();
                queryStringParams.put("id", String.valueOf(dbIdOfTheReport));
                updateScheduleReportForm.hitUpdateScheduleReportFormAPI(queryStringParams);

                customAssert.assertTrue(updateScheduleReportForm.getApiStatusCode().contains("200"), "API status code is not correct While hitting UpdateScheduleReportForm API for Report Db Id " + dbIdOfTheReport);
                customAssert.assertTrue(APIUtils.validJsonResponse(updateScheduleReportForm.getResponseUpdateScheduleReportFormAPI()), "UpdateScheduleReportFrom API Response in not a valid Json");

                // Hitting the UpdateScheduleReport API

                UpdateScheduleReport updateScheduleReport = new UpdateScheduleReport(updateScheduleReportForm.getResponseUpdateScheduleReportFormAPI());
                updateScheduleReport.hitUpdateScheduleReportAPI();

                customAssert.assertTrue(updateScheduleReport.getApiStatusCode().contains("200"), "API status code is not correct While hitting updateScheduleReport API for Report DB Id " + dbIdOfTheReport);

            } else {
                logger.error("Not being able to identify the Report Id which has been created earlier so skipping the updation part");
            }
        } catch (Exception e) {
            logger.error("Got exception. {}", e.getMessage());
            customAssert.assertTrue(false, "Got Exception. " + e.getMessage());
        } finally {
            addTestResult(getTestCaseIdForMethodName("testScheduleReport"), customAssert);
        }
        customAssert.assertAll();
    }


    public void getScheduleReportDefaultEmailConfigData() throws ParseException {
        logger.info("Initializing Test Data for Schedule Report Default Email Config Data");


        scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFilePath");
        scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFileName");


        tableName = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "tablename");
        String parser = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "parser");

        columnName = Arrays.asList(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "columnname").split(parser));
        comparator = Arrays.asList(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "comparator").split(parser));
        columnValue = Arrays.asList(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "columnValue").split(parser));

        dayWindow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "daywindow"));
        tableColumnNameToSelect = Arrays.asList(ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "tablecolumnnametoselect").split(parser));
        filterRecordsOrderByQuery = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "filterrecordsorderbyquery");


    }


    /**
     * Here the DAtaProvider will provide Object array on the basis on ITestContext
     *
     * @return
     */
    @DataProvider(name = "getScheduleReportEmailData")
    public Object[][] getScheduleReportEmailData() throws ConfigurationException {

        logger.info("In the Data Provider");
        List<Object[]> allTestData = new ArrayList<>();
        if (Boolean.parseBoolean(verifyFromEmail)) {
            getScheduleReportDefaultEmailConfigData();
            logger.info("In the Data Provider");
            String testableReports = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "default", "schedulereportsforentities");
            if (testableReports.equalsIgnoreCase("all")) {
                for (Map.Entry<Integer, List<String>> entityWithReport : allReportAccordingToEntity.entrySet()) {
                    Integer entity = entityWithReport.getKey();
                    List<String> report = entityWithReport.getValue();
                    for (String s : report) {
                        ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                        try {
                            reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(Integer.parseInt(s));
                            String reportRendererDefaultUserListMetaDataJsonStr = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                            if (!ParseJsonResponse.validJsonResponse(reportRendererDefaultUserListMetaDataJsonStr)) {
                                allTestData.add(new Object[]{entityNameAccordingToEntityTypeId.get(entity), reportNameAccordingToReportId.get(entity).get(s), -2});
                                continue;
                            }
                            JSONObject jsonObj = new JSONObject(reportRendererDefaultUserListMetaDataJsonStr);
                            JSONUtility json = new JSONUtility(jsonObj);
                            if (jsonObj.has("body") && json.getStringJsonValue("body") == null) {
                                logger.info("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
                                allTestData.add(new Object[]{entityNameAccordingToEntityTypeId.get(entity), reportNameAccordingToReportId.get(entity).get(s), -2});
                                continue;
                            }
                            JSONObject reportMetadataJson = json.getJsonObject("reportMetadataJson");
                            boolean isListing = reportMetadataJson.getBoolean("isListing");
                            if (isListing) {
                                allTestData.add(new Object[]{entityNameAccordingToEntityTypeId.get(entity), reportNameAccordingToReportId.get(entity).get(s), 0});
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size());
                for (String entitySection : allEntitySection) {
                    if (!entitySection.equalsIgnoreCase("default")) {
                        logger.debug("entitySection :{}", entitySection);
                        List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(scheduleReportConfigFilePath, scheduleReportConfigFileName, entitySection);
                        for (String entitySpecificProperty : allProperties) {
                            if (entitySpecificProperty.contentEquals("reportnames")) {
                                String reportsName = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, entitySection, entitySpecificProperty);
                                logger.debug("entitySection :{} ,reportsNames :{}", entitySection, reportsName);
                                String[] reportsNameArray = reportsName.split(",");
                                for (String s : reportsNameArray) {
                                    allTestData.add(new Object[]{entitySection, s, 0});
                                }
                            }

                        }
                    }
                }
            }
            return allTestData.toArray(new Object[0][]);
        }
        allTestData.add(new Object[]{"SkipThisTest", null, 0});
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "getScheduleReportEmailData", priority = 2,enabled = false)
    public void verifyScheduleEmailsFromDB(String entityName, String reportName, int flag) {
        CustomAssert csAssert = new CustomAssert();
        try {
            if (flag == -2) {
                csAssert.assertTrue(false, "ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
                throw new SkipException("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
            }
            logger.info("EntityName is : [{}]", entityName);
            if (!entityName.contentEquals("SkipThisTest")) {
                logger.info("Reports to be verified is/are : [{}]", reportName);
                if ((columnName.size() == comparator.size()) && (columnName.size() == columnValue.size())) {
                    Map<String, Object> columnNameValueMap = new LinkedHashMap<>();
                    for (int i = 0; i < columnName.size(); i++) {
                        columnNameValueMap.put(columnName.get(i), columnValue.get(i));
                    }
                    columnNameValueMap.put("date_created", getDateFromWhichRecordsNeedsToBeFilter(dayWindow));
                    columnNameValueMap.put("subject", "%" + reportName + "%");
                    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
                    String query = postgreSQLJDBC.getQueryClauses(tableName, tableColumnNameToSelect, columnNameValueMap, comparator) + " " + filterRecordsOrderByQuery + " ";

                    logger.debug("query is {}", query);
                    List<List<String>> result = postgreSQLJDBC.doSelect(query);

                    if (result.isEmpty()) {
                        csAssert.assertTrue(false, "There is no such entry in " + tableName + " table based on given filter in config file ");
                    } else {

                        String htmlbodytext = result.get(0).get(result.get(0).size() - 1); //putting hardcoded 0 index because there is always going to be one record on which we will verify the body content
                        String attachment = result.get(0).get(result.get(0).size() - 2); //putting hardcoded 0 index because there is always going to be one record on which we will verify the body content

                        // these following assertion is for checking 'report title' , 'user name' who has schedule the report and 'additional comment' in email body
                        csAssert.assertTrue(htmlbodytext.contains("The report titled " + reportName + " has been scheduled"), "Email body don't have  Report Name");
                        csAssert.assertTrue(htmlbodytext.toLowerCase().contains(("scheduled by " + ConfigureEnvironment.getEnvironmentProperty("j_username").replace("_", " ")).toLowerCase()), "Email body don't have  correct schedular User Name");
                        csAssert.assertTrue(htmlbodytext.contains("automation_" + reportName), "Email body don't have  correct additional comment ");
                        csAssert.assertTrue(attachment != null && !attachment.isEmpty(), "Schedule Email Don't any attachment in it ");


                        // TODO: 6/4/18 check for schedule date need to be done which is subjected to date pattern in email needed to be discussed

                    }

                } else {
                    csAssert.assertTrue(false, "Config File is incorrect for this test cast : Plz Check Again");
                }
                logger.info("Verified the entry for EntityName [{}] having ReportName [{}]", entityName, reportName);
            } else {
                logger.info("Verify from DB flag is false or not exist in config file so skipping this test for schedule reports");
                throw new SkipException("Verify from DB flag is false or not exist in config file so skipping this test for schedule reports");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, e.getMessage());
        }
        csAssert.assertAll();
    }


    private String getDateFromWhichRecordsNeedsToBeFilter(int dayWindow) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, dayWindow); //
        return sdf.format(c.getTime());


    }


    private List<String> setHeadersInCSVFile() {
        String[] allColumns = {"Index", "TestMethodName", "reportId", "createScheduleReportFormAPI", "createScheduleReportAPI", "scheduleByMeReportAPI", "isScheduleReportIsInActiveStatus", "TestMethodResult", "Comments", "ErrorMessage"};
        return new ArrayList<>(Arrays.asList(allColumns));
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
