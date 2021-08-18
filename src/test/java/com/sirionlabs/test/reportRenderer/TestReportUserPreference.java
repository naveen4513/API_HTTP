package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.reportRenderer.ReportRenderListReportJson;
import com.sirionlabs.api.userPreference.ReportUserPreference;
import com.sirionlabs.api.userPreference.ReportUserPreferenceDelete;
import com.sirionlabs.api.userPreference.ReportUserPreferenceSave;
import com.sirionlabs.api.userPreference.ReportUserPreferenceSetDefault;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class TestReportUserPreference {


    private final static Logger logger = LoggerFactory.getLogger(TestReportUserPreference.class);
    static String reportUserPreferenceConfigFilePath;
    static String reportUserPreferenceConfigFileName;

    // for creating payload for creating user view
    static int size;
    static int offset;
    static String defaultSize;
    static String orderDirectionDesc;
    static String orderDirectionAsc;
    static String orderByColumnId;
    static String maxNumberOfColumns;

    ReportRenderListReportJson reportRenderListReportJsonObj;
    FilterUtils filterUtils;

    String reportRendererConfigFilePath;
    String reportRendererConfigFileName;
    String reportRenderCSVDelemeter;
    String reportRenderCSVFile;
    String baseFilePath;
    String entityIdMappingFileName;
    String entityIdConfigFilePath;
    String dateFormat;
    String testFiltersRandomData;
    String filtersRandomDataSize;

    List<String> allEntitySection;


    List<Map<String, String>> allColumnQueryName;
    List<String> skipReportIdsList = new ArrayList<>();
    Map<Integer, List<String>> entityReportIdsMap = new LinkedHashMap<>(); // it will store the entity type id and associate Report Id (which will be tested)


    int totalScenario = 0;


    @BeforeClass
    public void beforeClass() throws IOException, ConfigurationException {
        logger.info("In Before Class method");
        getReportRenderConfigData();
    }


    public void getReportRenderConfigData() throws ParseException, IOException, ConfigurationException {
        logger.debug("Initializing Test Data for Report Render");
        reportRenderListReportJsonObj = new ReportRenderListReportJson();

        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        reportRendererConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFilePath");
        reportRendererConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFileName");


        filterUtils = new FilterUtils();
        reportRenderCSVFile = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderCSVFile");
        reportRenderCSVDelemeter = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderCSVDelimiter");
        dateFormat = ConfigureConstantFields.getConstantFieldsProperty("DateFormatForReports");


        testFiltersRandomData = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "testrandondata");
        filtersRandomDataSize = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "randomcount");


        // reportUserPreference Config File Properties
        reportUserPreferenceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFilePath");
        reportUserPreferenceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportUserPreferenceConfigFileName");
        allEntitySection = ParseConfigFile.getAllSectionNames(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName);


        String skipReportIds = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName, "skipreportids");
        skipReportIdsList = Arrays.asList(skipReportIds.trim().split(Pattern.quote(",")));


        size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName,
                "size"));
        offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName,
                "offset"));
        defaultSize = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName,
                "defaultsize");
        orderDirectionDesc = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName,
                "orderdirectiondesc");
        orderDirectionAsc = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName,
                "orderdirectionasc");
        orderByColumnId = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName,
                "orderbycolumnid");
        maxNumberOfColumns = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName,
                "maxnumberofcolumns");


    }


    @BeforeMethod
    public void beforeMethod(Method method) {
        logger.debug("In Before Method");
        logger.debug("method name is: {} ", method.getName());
        logger.debug("***********************************************************************************************************************");

    }


    public Map<Integer, List<String>> getdataProviderMapIfConfigHasMoreEntries(JSONArray reportJsonArrayForEntities) {
        logger.info("Creating Data Provider Map as Config has more entries than API Response");
        Map<Integer, List<String>> dataProvidermap = new HashMap<>();
        for (int rc = 0; rc < allEntitySection.size(); rc++) {
            String entityNameInConfig = allEntitySection.get(rc);
            int entityIdInConfig = ConfigureConstantFields.getEntityIdByName(entityNameInConfig);

            for (int count = 0; count < reportJsonArrayForEntities.length(); count++) {
                JSONObject obj = reportJsonArrayForEntities.getJSONObject(count);
                JSONUtility jsonUtilObj = new JSONUtility(obj);
                int entityTypeId = jsonUtilObj.getIntegerJsonValue("entityTypeId");
                if (entityTypeId == entityIdInConfig) {
                    logger.debug("Currently creating for entityName [ {} ] , and  entityTypeId [ {} ] ", entityNameInConfig, entityTypeId);
                    JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
                    int sizeOfSubReports = subReportsJsonArray.length();
                    List<String> reportsIdsArray = new ArrayList<>();
                    for (int sc = 0; sc < sizeOfSubReports; sc++) {
                        JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
                        JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
                        String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
                        String isManualReport = jsonUtilObjForSubReport.getStringJsonValue("isManualReport");
                        if (isManualReport.equalsIgnoreCase("false")) {
                            reportsIdsArray.add(reportId);
                        }
                    }
                    dataProvidermap.put(entityTypeId, reportsIdsArray);
                } else {
                    continue;
                }
            }
        }
        return dataProvidermap;
    }

    public Map<Integer, List<String>> getdataProviderMapIfAPIResponseHasMoreEntries(JSONArray reportJsonArrayForEntities) {
        logger.info("Creating Data Provider Map as API Response has more entries than Config");
        Map<Integer, List<String>> dataProvidermap = new HashMap<>();
        for (int rc = 0; rc < reportJsonArrayForEntities.length(); rc++) {
            JSONObject obj = reportJsonArrayForEntities.getJSONObject(rc);
            JSONUtility jsonUtilObj = new JSONUtility(obj);
            Integer entityTypeId = jsonUtilObj.getIntegerJsonValue("entityTypeId");
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
            if (entityName != null) {
                if (allEntitySection.contains(entityName.toLowerCase())) {
                    logger.info("Currently creating for entityName [ {} ] , and  entityTypeId [ {} ] ", entityName, entityTypeId);
                    JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
                    int sizeOfSubReports = subReportsJsonArray.length();
                    List<String> reportsIdsArray = new ArrayList<>();
                    for (int sc = 0; sc < sizeOfSubReports; sc++) {
                        JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
                        JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
                        String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
                        String isManualReport = jsonUtilObjForSubReport.getStringJsonValue("isManualReport");
                        if (isManualReport.equalsIgnoreCase("false")) {
                            reportsIdsArray.add(reportId);
                        }
                    }
                    dataProvidermap.put(entityTypeId, reportsIdsArray);
                } else {
                    logger.warn("The Entity Name [ {} ] is not available in the Config file.", entityName);
                }
            }
        }
        return dataProvidermap;
    }

    public Object[][] getDefaultReportsList() {
        Object[][] groupArray = null;
        try {
            reportRenderListReportJsonObj.hitReportRender();
            String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
            logger.info("The Report Response is : [ {} ]", reportRenderJsonStr);
            JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
            int noOfEntitiesInReport = reportJsonArrayForEntities.length();
            logger.info("Number of Records [ {} ]", noOfEntitiesInReport);

            Map<Integer, List<String>> dataProvidermap = new HashMap<>();


            if (allEntitySection.size() > noOfEntitiesInReport) {
                logger.debug("The entries in Config are more than the API Response");
                dataProvidermap = getdataProviderMapIfConfigHasMoreEntries(reportJsonArrayForEntities);
            } else {
                logger.debug("The entries in the API Response are more than Config");
                dataProvidermap = getdataProviderMapIfAPIResponseHasMoreEntries(reportJsonArrayForEntities);
            }

            groupArray = new Object[dataProvidermap.size()][2];
            int count = 0;

            for (Map.Entry<Integer, List<String>> entry : dataProvidermap.entrySet()) {
                groupArray[count][0] = entry.getKey();
                groupArray[count][1] = entry.getValue();//.toArray(new Object[entry.getValue().size()]);
                count++;
            }
        } catch (Exception e) {
            logger.error("Got Exception while creating Data Provider for All Reports , Cause : [ {} ] , Stack : [ {} ]", e.getMessage(), e.getStackTrace());
            e.printStackTrace();
        }

        return groupArray;
    }

    // this helper method will create Payload for Creating View

    public String getPayloadForCreatingView(int reportId, int entityTypeId, boolean isPublic) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd_HH:mm:ss");
        Date date = new Date();
        logger.debug("Date : {} ", dateFormat.format(date));

        String viewName;
        if (isPublic)
            viewName = "automation" + reportId + "_" + dateFormat.format(date) + "_Public";
        else
            viewName = "automation" + reportId + "_" + dateFormat.format(date) + "_Private";

        String filterJson;
        filterJson = "{\\\"filterMap\\\":{\\\"entityTypeId\\\":" + entityTypeId + ",\\\"offset\\\":" + offset + ",\\\"size\\\":" + size + ",\\\"orderByColumnName\\\":\\" + orderDirectionDesc + "\\,\\\"orderDirection\\\":\\" + orderDirectionAsc + "\\,\\\"filterJson\\\":{}}";


        JSONObject payloadJson = new JSONObject();
        payloadJson.put("maxNumberOfColumns", maxNumberOfColumns);
        payloadJson.put("listId", reportId);
        payloadJson.put("name", viewName);
        payloadJson.put("publicVisibility", isPublic);
        payloadJson.put("filterJson", filterJson);
        logger.debug("payloadJson: {}", payloadJson);

        return payloadJson.toString();


    }

    /**
     * Here the DAtaProvider will provide Object array on the basis on ITestContext
     *
     * @return
     */
    @DataProvider(name = "TestReportUserPreference", parallel = false)
    public Object[][] getTestReportUserPreferenceReportIds(ITestContext c) throws ConfigurationException {

        logger.debug("In the Data Provider");

        String testReportsForAllEntitiesAllReports = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName, "testreportsforallentitiesallreports");
        if (testReportsForAllEntitiesAllReports.equalsIgnoreCase("true")) {
            return getDefaultReportsList();
        } else {
            int i = 0;
            Object[][] groupArray;

            String testReportsForAllEntities = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName, "testreportsforallentities");

            if (testReportsForAllEntities.equalsIgnoreCase("true")) {
                logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size());
                groupArray = new Object[allEntitySection.size()][];
            } else {
                String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName, "entitiestotest").split(",");
                logger.debug("entitiesToTest :{} , entitiesToTest.size() : {}", entitiesToTest, entitiesToTest.length);
                groupArray = new Object[entitiesToTest.length][];
                allEntitySection = Arrays.asList(entitiesToTest);
            }

            for (String entitySection : allEntitySection) {

                logger.debug("entitySection :{}", entitySection);
                Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
                groupArray[i] = new Object[2];
                List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName, entitySection);
                for (String entitySpecificProperty : allProperties) {
                    if (entitySpecificProperty.contentEquals("test")) {
                        String reportsIds = ParseConfigFile.getValueFromConfigFile(reportUserPreferenceConfigFilePath, reportUserPreferenceConfigFileName, entitySection, entitySpecificProperty);
                        logger.debug("entitySection :{} ,reportsIds :{}", entitySection, reportsIds);
                        List<String> reportsIdsArray = Arrays.asList(reportsIds.split(","));
                        groupArray[i][0] = entityTypeId;
                        groupArray[i][1] = reportsIdsArray;
                    }
                }
                i++;

            }
            logger.debug("Formed the paramter Array to be tested");
            return groupArray;
        }
    }

    @Test(priority = 0, dataProvider = "TestReportUserPreference")
    public void testReportUserPreferenceListAPI(Integer entityTypeId, List<String> reportIds) {
        SoftAssert softAssertion = new SoftAssert();

        entityReportIdsMap.put(entityTypeId, reportIds);
        String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

        logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);
        logger.info("Report ids are : {}", reportIds);


        for (String reportId : reportIds) {
            logger.info("Report id is : {}", reportId);

            if (skipReportIdsList.contains(reportId)) {
                logger.info("No need to test UserPreference for this report : [ {} ] , as it is mentioned in Config file", reportId);
                continue;
            }

            try {

                totalScenario++;
                ReportUserPreference reportUserPreference = new ReportUserPreference(Integer.parseInt(reportId));
                HttpResponse response = reportUserPreference.hitReportUserPreferenceAPI();
                String reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();

                logger.debug("Response Status Line is :{}", reportUserPreference.getApiStatusCode());
                logger.debug("Response Payload is Line is :{}", reportUserPreferenceResponse);

                softAssertion.assertTrue(response.getStatusLine().getStatusCode() == 200, "Report User Preference API Response Status Code is incorrect for reportId " + reportId);
                softAssertion.assertTrue(APIUtils.validJsonResponse(reportUserPreferenceResponse), "Report User Preference API Response is not valid Json for reportId " + reportId);


                logger.info("ViewID HashMap is : [{}]", reportUserPreference.getViewIdNameMap(reportUserPreferenceResponse));


            } catch (Exception e) {
                logger.error("Error While Hitting Testing ReportUserPreference [{}] for reportId", e.getLocalizedMessage(), reportId);

            }

        }


        logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
        softAssertion.assertAll();
    }


    @Test(priority = 1)
    public void testReportUserPreferenceCreateViewPublic() {

        Set<Integer> entityTypeIds = entityReportIdsMap.keySet();
        SoftAssert softAssertion = new SoftAssert();

        for (Integer entityTypeId : entityTypeIds) {


            List<String> reportIds = entityReportIdsMap.get(entityTypeId);
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

            logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);
            logger.info("Report ids are : {}", reportIds);


            for (String reportId : reportIds) {
                logger.info("Report id is : {}", reportId);

                if (skipReportIdsList.contains(reportId)) {
                    logger.info("No need to test UserPreference Create View (Public) for this report : [ {} ] , as it is mentioned in Config file", reportId);
                    continue;
                }

                try {

                    totalScenario++;


                    ReportUserPreference reportUserPreference = new ReportUserPreference(Integer.parseInt(reportId));
                    reportUserPreference.hitReportUserPreferenceAPI();
                    String reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();
                    LinkedHashMap<Integer, String> viewIdNameMapOld = reportUserPreference.getViewIdNameMap(reportUserPreferenceResponse);

                    Set<Integer> viewIdsOld = viewIdNameMapOld.keySet();
                    logger.info("viewIDs before Creating view are [{}]", viewIdsOld);


                    String payloadForCreatingView = getPayloadForCreatingView(Integer.parseInt(reportId), entityTypeId, true);
                    ReportUserPreferenceSave reportUserPreferenceSave = new ReportUserPreferenceSave(Integer.parseInt(reportId));
                    HttpResponse response = reportUserPreferenceSave.hitReportUserPreferenceSaveAPI(payloadForCreatingView);
                    String reportUserPreferenceSaveAPIResponse = reportUserPreferenceSave.getResponseReportUserPreferenceSaveAPI();

                    logger.debug("Response Status Line is :{}", reportUserPreferenceSave.getApiStatusCode());
                    logger.debug("Response Payload is Line is :{}", reportUserPreferenceSaveAPIResponse);

                    softAssertion.assertTrue(response.getStatusLine().getStatusCode() == 200, "Report User Preference Save API Response Status Code is incorrect for reportId " + reportId);
                    softAssertion.assertTrue(APIUtils.validJsonResponse(reportUserPreferenceSaveAPIResponse), "Report User Preference Save API Response is not valid Json for reportId " + reportId);


                    // Functionality Validation Starts Here
                    reportUserPreference = new ReportUserPreference(Integer.parseInt(reportId));
                    reportUserPreference.hitReportUserPreferenceAPI();
                    reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();
                    LinkedHashMap<Integer, String> viewIdNameMapNew = reportUserPreference.getViewIdNameMap(reportUserPreferenceResponse);

                    Set<Integer> viewIdsNew = viewIdNameMapNew.keySet();
                    logger.info("viewIDs before After view are [{}]", viewIdsNew);


                    if (viewIdsNew.size() != (viewIdsOld.size() + 1)) {// this validation can be more strict @todo
                        softAssertion.assertTrue(false, "Create User View Functionality (Public) is not working for repord Id :" + reportId);
                    }

                    // Functionality Validation Ends here


                } catch (Exception e) {
                    logger.error("Error While Hitting Testing ReportUserPreferenceSave(public) [{}] for reportId", e.getLocalizedMessage(), reportId);

                }

            }


            logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);


        }

        softAssertion.assertAll();
    }

    @Test(priority = 2)
    public void testReportUserPreferenceCreateViewPrivate() {

        Set<Integer> entityTypeIds = entityReportIdsMap.keySet();
        SoftAssert softAssertion = new SoftAssert();

        for (Integer entityTypeId : entityTypeIds) {


            List<String> reportIds = entityReportIdsMap.get(entityTypeId);
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

            logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);
            logger.info("Report ids are : {}", reportIds);


            for (String reportId : reportIds) {
                logger.info("Report id is : {}", reportId);

                if (skipReportIdsList.contains(reportId)) {
                    logger.info("No need to test UserPreference Create View (Private) for this report : [ {} ] , as it is mentioned in Config file", reportId);
                    continue;
                }

                try {

                    totalScenario++;

                    ReportUserPreference reportUserPreference = new ReportUserPreference(Integer.parseInt(reportId));
                    reportUserPreference.hitReportUserPreferenceAPI();
                    String reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();
                    LinkedHashMap<Integer, String> viewIdNameMapOld = reportUserPreference.getViewIdNameMap(reportUserPreferenceResponse);

                    Set<Integer> viewIdsOld = viewIdNameMapOld.keySet();
                    logger.info("viewIDs before Creating view are [{}]", viewIdsOld);

                    String payloadForCreatingView = getPayloadForCreatingView(Integer.parseInt(reportId), entityTypeId, false);
                    ReportUserPreferenceSave reportUserPreferenceSave = new ReportUserPreferenceSave(Integer.parseInt(reportId));
                    HttpResponse response = reportUserPreferenceSave.hitReportUserPreferenceSaveAPI(payloadForCreatingView);
                    String reportUserPreferenceSaveAPIResponse = reportUserPreferenceSave.getResponseReportUserPreferenceSaveAPI();

                    logger.debug("Response Status Line is :{}", reportUserPreferenceSave.getApiStatusCode());
                    logger.debug("Response Payload is Line is :{}", reportUserPreferenceSaveAPIResponse);

                    softAssertion.assertTrue(response.getStatusLine().getStatusCode() == 200, "Report User Preference Save API Response Status Code is incorrect for reportId " + reportId);
                    softAssertion.assertTrue(APIUtils.validJsonResponse(reportUserPreferenceSaveAPIResponse), "Report User Preference Save API Response is not valid Json for reportId " + reportId);


                    // Functionality Validation Starts Here
                    reportUserPreference = new ReportUserPreference(Integer.parseInt(reportId));
                    reportUserPreference.hitReportUserPreferenceAPI();
                    reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();
                    LinkedHashMap<Integer, String> viewIdNameMapNew = reportUserPreference.getViewIdNameMap(reportUserPreferenceResponse);

                    Set<Integer> viewIdsNew = viewIdNameMapNew.keySet();
                    logger.info("viewIDs before After view are [{}]", viewIdsNew);


                    if (viewIdsNew.size() != (viewIdsOld.size() + 1)) { // this validation can be more strict @todo
                        softAssertion.assertTrue(false, "Create User View Functionality (Private) is not working for repord Id :" + reportId);
                    }

                    // Functionality Validation Ends here


                } catch (Exception e) {
                    logger.error("Error While Hitting Testing ReportUserPreferenceSave(private)  [{}] for reportId", e.getLocalizedMessage(), reportId);

                }

            }


            logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);


        }

        softAssertion.assertAll();
    }

  //  @Test(priority = 3)
    public void testReportUserPreferenceSetDefaultViewFunctionality() {

        Set<Integer> entityTypeIds = entityReportIdsMap.keySet();
        SoftAssert softAssertion = new SoftAssert();

        for (Integer entityTypeId : entityTypeIds) {


            List<String> reportIds = entityReportIdsMap.get(entityTypeId);
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

            logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);
            logger.info("Report ids are : {}", reportIds);


            for (String reportId : reportIds) {
                logger.info("Report id is : {}", reportId);

                if (skipReportIdsList.contains(reportId)) {
                    logger.info("No need to test ReportUserPreferenceSetDefaultViewFunctionality for this report : [ {} ] , as it is mentioned in Config file", reportId);
                    continue;
                }

                try {

                    totalScenario++;
                    ReportUserPreference reportUserPreference = new ReportUserPreference(Integer.parseInt(reportId));
                    reportUserPreference.hitReportUserPreferenceAPI();
                    String reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();
                    LinkedHashMap<Integer, String> viewIdNameMap = reportUserPreference.getViewIdNameMap(reportUserPreferenceResponse);

                    Set<Integer> viewIds = viewIdNameMap.keySet();


                    for (Integer viewId : viewIds) {

                        // Setting the earlier Created public view as default View
                        if (viewIdNameMap.get(viewId).toLowerCase().contains("automation") && viewIdNameMap.get(viewId).toLowerCase().contains("public")) {

                            ReportUserPreferenceSetDefault reportUserPreferenceSetDefault = new ReportUserPreferenceSetDefault();
                            HttpResponse response = reportUserPreferenceSetDefault.hitReportUserPreferenceSetDefaultAPI(Integer.parseInt(reportId), viewId);
                            logger.debug("Response Status Line is :{}", reportUserPreferenceSetDefault.getApiStatusCode());

                            softAssertion.assertTrue(response.getStatusLine().getStatusCode() == 200, "Report User Preference Set Default API Response Status Code is incorrect for reportId  " + reportId + ": viewId " + viewId);

                            // functionality verification
                            reportUserPreference.hitReportUserPreferenceAPI();
                            reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();
                            String propertyValue = reportUserPreference.getViewProperty(viewId, "isdefault", reportUserPreferenceResponse);

                            logger.info("PropertyValue is :[{}]", propertyValue);

                            if (!propertyValue.contentEquals("notfound")) {
                                if (Boolean.parseBoolean(propertyValue) == false) {
                                    softAssertion.assertTrue(false, "Set Default View Functionality is not working for report Id " + reportId + ": view Id" + viewId);

                                }

                            } else {
                                logger.error("There is not isdefault property For View Id [{}]", viewId);
                            }

                        }
                    }


                } catch (Exception e) {
                    logger.error("Error While Testing ReportUserPreferenceSetDefaultViewFunctionality [{}] for reportId", e.getLocalizedMessage(), reportId);

                }

            }


            logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);


        }

        softAssertion.assertAll();
    }


    @Test(priority = 5, enabled = false)
    public void testReportUserPreferenceDeleteView() {

        Set<Integer> entityTypeIds = entityReportIdsMap.keySet();
        SoftAssert softAssertion = new SoftAssert();

        for (Integer entityTypeId : entityTypeIds) {


            List<String> reportIds = entityReportIdsMap.get(entityTypeId);
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

            logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);
            logger.info("Report ids are : {}", reportIds);


            for (String reportId : reportIds) {
                logger.info("Report id is : {}", reportId);

                if (skipReportIdsList.contains(reportId)) {
                    logger.info("No need to test ReportUserPreferenceDeleteView for this report : [ {} ] , as it is mentioned in Config file", reportId);
                    continue;
                }

                if(reportId.equals("90") || reportId.equals("270") || reportId.equals("50")
                        || reportId.equals("83")|| reportId.equals("23")|| reportId.equals("1000")
                        || reportId.equals("88")){
                    logger.info("No need to test ReportUserPreferenceDeleteView for this report : [ {} ] , as it is not for delete view ", reportId);
                    continue;
                }

                try {
                    totalScenario++;

                    ReportUserPreference reportUserPreference = new ReportUserPreference(Integer.parseInt(reportId));
                    reportUserPreference.hitReportUserPreferenceAPI();
                    String reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();
                    LinkedHashMap<Integer, String> viewIdNameMap = reportUserPreference.getViewIdNameMap(reportUserPreferenceResponse);

                    Set<Integer> viewIds = viewIdNameMap.keySet();


                    for (Integer viewId : viewIds) {

                        // deleting the view which have been created by automation only
                        if (viewIdNameMap.get(viewId).toLowerCase().contains("automation")) {


                            ReportUserPreferenceDelete reportUserPreferenceDelete = new ReportUserPreferenceDelete();
                            HttpResponse response = reportUserPreferenceDelete.hitReportUserPreferenceDeleteAPI(Integer.parseInt(reportId), viewId);
                            String reportUserPreferenceDeleteAPIResponse = reportUserPreferenceDelete.getResponseReportUserPreferenceDeleteAPI();

                            logger.debug("Response Status Line is :{}", reportUserPreferenceDelete.getApiStatusCode());
                            logger.debug("Response Payload is Line is :{}", reportUserPreferenceDeleteAPIResponse);

                            softAssertion.assertTrue(APIUtils.validJsonResponse(reportUserPreferenceDeleteAPIResponse), "Report User Preference Delete API Response is not valid Json for reportId " + reportId + ": viewId " + viewId);

                        }
                    }


                    // functionality verification
                    reportUserPreference.hitReportUserPreferenceAPI();
                    reportUserPreferenceResponse = reportUserPreference.getResponseReportUserPreference();
                    viewIdNameMap = reportUserPreference.getViewIdNameMap(reportUserPreferenceResponse);

                    viewIds = viewIdNameMap.keySet();


                    for (Integer viewId : viewIds) {
                        // deleting the view which have been created by automation only
                        if (viewIdNameMap.get(viewId).toLowerCase().contains("automation")) {
                            softAssertion.assertTrue(false, "Delete User view Functionality is not working for reportId " + reportId + ": viewId " + viewId);
                        }
                    }


                } catch (Exception e) {
                    logger.error("Error While Testing ReportUserPreferenceDelete [{}] for reportId", e.getLocalizedMessage(), reportId);

                }

            }


            logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);


        }

        softAssertion.assertAll();
    }


    @AfterMethod
    public void afterMethod(ITestResult result) {
        logger.debug("In After Method");
        logger.debug("method name is: {}", result.getMethod().getMethodName());
        logger.info("Total Scenario Tested are :[{}] ", totalScenario);
        logger.debug("***********************************************************************************************************************");
    }

    @AfterClass
    public void afterClass() {
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        String delete_dynamic_user_preferences_metadata = "delete from dynamic_user_preferences_metadata where user_id = 1044 and client_id = 1002";
        String delete_pin_user_preference_metadata = "delete from pin_user_preference_metadata where user_preference_id in (select id from dynamic_user_preferences_metadata where user_id = 1044 and client_id = 1002)";
        try {
            postgreSQLJDBC.deleteDBEntry(delete_dynamic_user_preferences_metadata);
            postgreSQLJDBC.deleteDBEntry(delete_pin_user_preference_metadata);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        logger.info("Total Scenario Tested are :[{}] ", totalScenario);
        logger.debug("In After Class method");
    }


}