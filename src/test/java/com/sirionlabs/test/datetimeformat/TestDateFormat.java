package com.sirionlabs.test.datetimeformat;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.dateformat.Dateformat;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestDateFormat {
    private final static Logger logger = LoggerFactory.getLogger(TestDateFormat.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String dateFormattobechecked = null;
    private static String listDataConfigFilePath;
    private static String listDataConfigFileName;

    List<String> allEntitySection;
    List<Integer> allDBid;
    List<Integer> allDBidForContracts = null;

    ListRendererListData listDataObj = new ListRendererListData();
    ReportRendererListData reportlistobj = new ReportRendererListData();
    Check checkobj = new Check();

    static List<Integer> entitiesToSkip = new ArrayList<Integer>();

    String entityIdMappingFileName;
    String entityIdConfigFilePath;
    String baseFilePath;

    int dbIdCountToTestForSmoke = 10;
    int size;
    int offset;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() throws IOException {
        logger.info("In Before Class method");
        try {
            configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DateFormatConfigFilePath");
            configFileName = ConfigureConstantFields.getConstantFieldsProperty("DateFormatConfigFileName");
            entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
            size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "size"));
            offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset"));
            //allEntitySection = ParseConfigFile.getAllSectionNames(entityIdConfigFilePath, entityIdMappingFileName);
            dateFormattobechecked = ParseConfigFile.getValueFromConfigFile(TestDateFormat.configFilePath, TestDateFormat.configFileName,
                    "dateformat");
            baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

            getListDataConfigData();

        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception occurred while getting config data for listData api. {}", e.getMessage());
        }
    }
    @Test(groups = { "minor" }, priority = 0)
    public void testsetdateformatfromadmin() throws IOException{
        Dateformat df = new Dateformat();
        FileUtils fileutil = new FileUtils();
        Map<String,String> params = new HashMap<String, String>();
        CustomAssert csAssertion = new CustomAssert();

        try {
            String adminusername = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "msadminusername");
            String adminpassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "msadminpassword");

            checkobj.hitCheck(adminusername,adminpassword);

            String userupdateparamsfilepath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "userupdateparamsfilepath");
            params =  fileutil.ReadKeyValueFromFile(userupdateparamsfilepath,"=","default properties");
            Integer statuscode = df.UserOrgainzationPropertiesUpdate(params);
            if(statuscode == 302){
                logger.info("Dateformat updated successsfully from admin user");
                csAssertion.assertTrue(true, "Dateformat updated successsfully from admin user");
            }
            else{
                logger.error("Error in updating date format from admin");
                csAssertion.assertTrue(false, "Dateformat updated unsuccesssfully from admin user");
            }

        }catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "Exception while updating data from admin\n" + errors.toString());
        }
        finally {
            checkobj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        }
        csAssertion.assertAll();
    }

    @Test(groups = { "minor" }, priority = 1, dataProvider = "getAllEntitySection", enabled = true)
    public void testcheckdateformatListPage(String entitySection, Integer entityTypeId) {
        CustomAssert csAssertion = new CustomAssert();
        Map<String, String> listDataReportMap = new HashMap<>();
        int count = 0;
        try {
            int actualTotalCount = 0;
            // Iterate over each entitySection
            logger.info("validating date format list data page of entity ", entitySection);

            Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

            ListRendererListData listDataObj = new ListRendererListData();
            //for the first call empty payload is passed
            String payload = "{\"filterMap\":{}}";

            listDataObj.hitListRendererListData(urlId, true, payload, null);

            String listDataJsonStr = listDataObj.getListDataJsonStr();

            JSONObject obj = new JSONObject(listDataJsonStr);
            JSONObject datasubaarayobjectStringjsonobj;
            JSONObject dataarrayobj = null;

            JSONArray dataarray = obj.getJSONArray("data");
            JSONArray datasubaaray;

            int numOfRecords = dataarray.length();

            Object datasubaarayobject = null;
            Object colname;
            Object colval;

            String datasubaarayobjectString = null;
            String dateformat;
            int datasubaaray_lengthcounter =0;
            int datasubaaray_length = 0;
            while (count < numOfRecords){

                dataarrayobj = dataarray.getJSONObject(count);
                datasubaaray = JSONUtility.convertJsonOnjectToJsonArray(dataarrayobj);
                datasubaaray_length = datasubaaray.length();
                datasubaaray_lengthcounter = 0;
                while (datasubaaray_lengthcounter < datasubaaray_length)
                {
                    datasubaarayobject = datasubaaray.get(datasubaaray_lengthcounter);

                    datasubaarayobjectString = datasubaarayobject.toString();
                    datasubaarayobjectStringjsonobj = new JSONObject(datasubaarayobjectString);
                    colname = datasubaarayobjectStringjsonobj.get("columnName");
                    if(colname.toString().contains("date"))
                    {
                        colval = datasubaarayobjectStringjsonobj.get("value");
                        if(!colval.toString().contains("null")){
                            dateformat = DateUtils.getDateFormat(colval.toString());
                            if(dateformat.equals(dateFormattobechecked)){
                                logger.info("Date format valid for entityTypeId " + entityTypeId);
                                csAssertion.assertTrue(true,"Date format valid for entity type id" + entityTypeId);
                            }
                            else{
                                logger.error("Date format invalid for entity type id" + entityTypeId);
                                csAssertion.assertTrue(false,"Date format invalid for " + entityTypeId);
                            }
                        }
                    }
                    datasubaaray_lengthcounter++;
                }
                count++;
            }
        }
        catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "Date Format List Data Exception\n" + errors.toString());
        }
        csAssertion.assertAll();
    }

    @Test(groups = { "minor" }, priority = 2,dataProvider = "reportIdsToTest")
    public void testcheckdateformatReportPage(String listID){

        CustomAssert csAssertion = new CustomAssert();
        try {
            reportlistobj.hitReportRendererListData(Integer.parseInt(listID));
            String reportlistdataString = reportlistobj.getListDataJsonStr();

            JSONObject obj = new JSONObject(reportlistdataString);
            JSONObject datasubaarayobjectStringjsonobj;
            JSONObject dataarrayobj = null;

            JSONArray dataarray = obj.getJSONArray("data");
            JSONArray datasubaaray;

            int numOfRecords = dataarray.length();
            int count = 0;

            Object datasubaarayobject = null;
            Object colname;
            Object colval;

            String datasubaarayobjectString = null;
            String dateformat;

            while (count < numOfRecords) {

                dataarrayobj = dataarray.getJSONObject(count);
                datasubaaray = JSONUtility.convertJsonOnjectToJsonArray(dataarrayobj);
                int datasubaaray_length = datasubaaray.length();
                int datasubaaray_lengthcounter = 0;
                while (datasubaaray_lengthcounter < datasubaaray_length) {
                    datasubaarayobject = datasubaaray.get(datasubaaray_lengthcounter);

                    datasubaarayobjectString = datasubaarayobject.toString();
                    datasubaarayobjectStringjsonobj = new JSONObject(datasubaarayobjectString);
                    colname = datasubaarayobjectStringjsonobj.get("columnName");
                    if (colname.toString().contains("date")) {
                        colval = datasubaarayobjectStringjsonobj.get("value");
                        if (!colval.toString().contains("null")) {
                            dateformat = DateUtils.getDateFormat(colval.toString());
                            if (dateformat.equals(dateFormattobechecked)) {
                                logger.info("Date format valid for report list id " + listID);
                                csAssertion.assertTrue(true, "Date format valid for report list id " + listID);
                            } else {
                                logger.error("Date format invalid for entity type id" + listID);
                                csAssertion.assertTrue(false, "Date format invalid for " + listID);
                            }
                        }
                    }
                    datasubaaray_lengthcounter++;
                }
                count++;
            }
            }catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                csAssertion.assertTrue(false, "Date Format Report Renderer List Data Exception\n" + errors.toString());
            }
        }

    @DataProvider(name = "getAllEntitySection", parallel = false)
    public Object[][] getAllEntitySection() throws ConfigurationException {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size() - entitiesToSkip.size()][];

        for (String entitySection : allEntitySection) {

            Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
            if (entitiesToSkip.contains(entitySectionTypeId)) {
                continue;
            }
            groupArray[i] = new Object[2];
            groupArray[i][0] = entitySection; // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            i++;
        }

        return groupArray;
    }
    @DataProvider(name = "reportIdsToTest", parallel = false)
    public Object[][] getReportIds() throws ConfigurationException {

        int i = 0;
        String reportIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"reportidstotest");
        String reportIDs[] = reportIds.split(",");

        Object[][] groupArray = new Object[reportIDs.length][];

        for (String reportID : reportIDs) {

            groupArray[i] = new Object[1];
            groupArray[i][0] = reportID.trim(); // EntityName
            i++;
        }

        return groupArray;
    }
    public void getListDataConfigData() throws ParseException, IOException, ConfigurationException {
        logger.info("Getting Test Data for listData api");
        TestDateFormat.listDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DateFormatConfigFilePath");
        TestDateFormat.listDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DateFormatConfigFileName");

        allEntitySection = Arrays.asList((ParseConfigFile.getValueFromConfigFile(TestDateFormat.listDataConfigFilePath, TestDateFormat.listDataConfigFileName,
                "allentitytotest")).split(","));
        entitiesToSkip = this.getEntityIdsToSkip("entitiestoskip");

    }
    private List<Integer> getEntityIdsToSkip(String propertyName) throws ConfigurationException {
        String value = ParseConfigFile.getValueFromConfigFile(this.listDataConfigFilePath, this.listDataConfigFileName, propertyName);
        List<Integer> idList = new ArrayList<Integer>();

        if (!value.trim().equalsIgnoreCase("")) {
            String entityIds[] = ParseConfigFile.getValueFromConfigFile(this.listDataConfigFilePath, this.listDataConfigFileName, propertyName).split(",");

            for (int i = 0; i < entityIds.length; i++)
                idList.add(Integer.parseInt(entityIds[i].trim()));
        }
        return idList;
    }
    @DataProvider(name = "TestShowPageAPIData")
    public Object[][] getTestShowPageAPIData(ITestContext c) throws ConfigurationException {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size()][];

        for (String entitySection : allEntitySection) {
            groupArray[i] = new Object[2];
            Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
            groupArray[i][0] = entitySection; // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            i++;
        }

        return groupArray;
    }

    @Test(priority = 3,dataProvider = "TestShowPageAPIData", enabled = true)
    public void testcheckdateformatShowPage(String entitySection, Integer entityTypeId) {
        CustomAssert csAssertion = new CustomAssert();
        JSONObject pageresponsejsonobj;
        Object dateformatshowpage;

        if (entitiesToSkip.contains(entityTypeId)) {
            logger.warn("Skipping the show page validation for entity = {}", entitySection);
            //throw new SkipException("Skipping the show page validation for entity = "+entitySection);
        } else {

            try {
                // Iterate over each entitySection
                logger.info("validating date format on show page for entity {} ", entitySection);
                String listDataResponse = listDataResponse(entityTypeId, entitySection);
                logger.info("List Data API Response : entity={} , response={}", entitySection, listDataResponse);

                boolean isListDataValidJson = APIUtils.validJsonResponse(listDataResponse, "[listData response]");
                csAssertion.assertTrue(isListDataValidJson, "Response is not a valid JSON.");
                if (isListDataValidJson) {
                    boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataResponse);
                    boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataResponse);

                    csAssertion.assertFalse(isListDataApplicationError, "Application error found while hitting API = listData  for entity = " + entitySection);
                    csAssertion.assertFalse(isListDataPermissionDenied, "Permission Denied error found while hitting listData API  for entity = " + entitySection);
                    if (!isListDataApplicationError && !isListDataPermissionDenied) {

                        JSONObject listDataResponseObj = new JSONObject(listDataResponse);
                        int noOfRecords = listDataResponseObj.getJSONArray("data").length();

                        if (noOfRecords > 0) {
                            listDataObj.setListData(listDataResponse);
                            int columnId = listDataObj.getColumnIdFromColumnName("id");

                            allDBid = listDataObj.getAllRecordDbId(columnId, listDataResponse);

                            if (entitySection.contentEquals("contracts")) {
                                allDBidForContracts = allDBid;
                            }

                            // Dbids is for selected id on which validation would be perform
                            List<Integer> DBids = allDBid;
                            // we will pick random dbIdCountToTestForSmoke ids or less in case of smoke testing
                            if (ConfigureEnvironment.getTestingType().toLowerCase().contains("smoke") && dbIdCountToTestForSmoke < DBids.size()) {
                                DBids = new ArrayList<>();
                                int[] randomIndex = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allDBid.size(), dbIdCountToTestForSmoke);
                                for (int i = 0; i < randomIndex.length; i++) {
                                    DBids.add(allDBid.get(randomIndex[i]));
                                }
                            }

                            logger.info("Select Db Ids is : {}", DBids);

                            for (Integer dbId : DBids) {

                                String showPageResponseStr = getShowResponse(entityTypeId, dbId);
                                //JSONArray body = new JSONArray(showPageResponseStr);
                                pageresponsejsonobj = new JSONObject(showPageResponseStr);
                                dateformatshowpage = pageresponsejsonobj.getJSONObject("body").getJSONObject("globalData").get("dateFormatToShow");
                                //JSONArray ggg = JSONUtility.convertJsonOnjectToJsonArray(abc);
                                if(dateformatshowpage.toString().trim().equals(dateFormattobechecked)){
                                    logger.info("Dateformat valid for show page for DBID " + dbId.toString());
                                }
                                else {
                                    logger.error("Dateformat invalid for show page for DBID " + dbId.toString());
                                }
                                dateformatshowpage = null;
                                pageresponsejsonobj = null;

                                logger.info("Show API Response : entity={} , DB_ID = {} , response={}", entitySection, dbId, showPageResponseStr);
                                //Assertion for valid JSON response

                            }
                        } else
                            logger.warn("no records found for entity : {} ", entitySection);
                    }
                } else
                    logger.error("ListData response is not valid json for entity ={}", entitySection);
            } catch (Exception e) {
                logger.error(e.getMessage());
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                csAssertion.assertTrue(false, "TestShow Exception\n" + errors.toString());
            }
        }
        csAssertion.assertAll();
    }
    public String getShowResponse(int entityTypeId, int dbId) {
        Show showObj = new Show();
        showObj.hitShow(entityTypeId, dbId);

        return showObj.getShowJsonStr();

    }
    public String listDataResponse(int entityTypeId, String entitySection) throws Exception {

        Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

        String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
                offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"desc\",\"filterJson\":{}}}";

        logger.info("Hitting ListRendererListData");

        listDataObj.hitListRendererListData(urlId, false,
                listDataPayload, null);

        String listDataJsonStr = listDataObj.getListDataJsonStr();
        return listDataJsonStr;
    }
}
