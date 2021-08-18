package com.sirionlabs.test.reports;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.scheduleReport.CreateScheduleReport;
import com.sirionlabs.api.scheduleReport.CreateScheduleReportForm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestReportScheduled {
    private final static Logger logger = LoggerFactory.getLogger(TestReportScheduled.class);
    private String dataFilePath = "src/test/resources/TestConfig/ScheduleReportFlowDownTestData";
    private String dataFileNameSupplier = "ScheduleReportFlowDownTestDataSupplier.json";
    private String dataFileNameContract="ScheduleReportFlowDownTestDataContract.json";
    private String frequencyType;
    private String timeZoneId;
    private String[] selectedUsersFromConfigFile;
    private String nameOfTheReport;
    private String allReportToTest;
    private String externalEmails;
    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        String scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("FDScheduleReportConfigFilePath");
        String scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("FDScheduleReportConfigFileName");
        frequencyType = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "frequencytype");
        timeZoneId = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "timezoneId");
        selectedUsersFromConfigFile = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "selectedusers").split(",");
        frequencyType = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "frequencytype");
        timeZoneId = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "timezoneId");
        allReportToTest = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "reporttotest");
        externalEmails = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "externalemails");

    }

    @DataProvider
    public Object[][] dataProviderForReportListId() {
        List<Object[]> allTestData = new ArrayList<>();
        if (!allReportToTest.isEmpty()) {
            String[] allReportListId = allReportToTest.split(",");
            for (String reportListId : allReportListId)
                allTestData.add(new Object[]{reportListId.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }
    @DataProvider
    public Object[][] dataProviderForEntityName() {
        List<Object[]> allTestData = new ArrayList<>();
            String[] allEntityName =new String[]{"contracts","suppliers",};
            for (String reportListId : allEntityName)
                allTestData.add(new Object[]{reportListId.trim()});
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForEntityName")
    public void editSupplierAndContract(String entityName){

         CustomAssert customAssert=new CustomAssert();
        try {
            logger.info("get all list data for entity {}",entityName);
            ArrayList<String> allEntityId= getAllEntity(entityName);
            Edit edit = new Edit();
            Map<String, String> allRecordData = new HashMap<>();
            for (String s : allEntityId) {

                logger.info("hitting entity edit API");
                edit.hitEdit(entityName, Integer.parseInt(s));
                String responseAccordingTOEntityId = edit.getEditDataJsonStr();
                if (ParseJsonResponse.validJsonResponse(responseAccordingTOEntityId)) {
                    JSONObject jsonObjectData = new JSONObject(responseAccordingTOEntityId).getJSONObject("body").getJSONObject("data");

                    logger.info("remove all options from data");
                    removeOptionsFields(jsonObjectData, "options");

                    logger.info("save json body according to entityId {}",s);

                    allRecordData.put(s, "{\"body\":{\"data\":" + jsonObjectData.toString() + "}}");

                    if (entityName.equalsIgnoreCase("suppliers")) {
                        logger.info("add supplier manager");
                        addSupplierManager(jsonObjectData, new JSONObject("{\"name\":\"Anay User1\",\"id\":1054,\"type\":2}"));
                    }
                    else {
                        logger.info("add contract manager");
                        addContractManager(jsonObjectData, new JSONObject("{\"name\":\"Anay User1\",\"id\":1054,\"type\":2}"));
                    }
                    String modifyJsonObject = "{\"body\":{\"data\":" + jsonObjectData.toString() + "}}";
                    logger.info("hitting edit API");
                     edit.hitEdit(entityName,modifyJsonObject);
                    String editDataJsonStr=edit.getEditDataJsonStr();
                    if (ParseJsonResponse.validJsonResponse(editDataJsonStr))
                    {
                        String result=new JSONObject(editDataJsonStr).getJSONObject("header").getJSONObject("response").getString("status");
                        if(!result.equalsIgnoreCase("success"))
                        {
                            customAssert.assertTrue(false,entityName+"is not edit");
                        }
                    }
                }
            }
            logger.info("save all record data");
            if (entityName.equalsIgnoreCase("suppliers"))
                 FileUtils.saveResponseInFile(dataFilePath,dataFileNameSupplier,new JSONObject(allRecordData).toString());
            else
                FileUtils.saveResponseInFile(dataFilePath,dataFileNameContract,new JSONObject(allRecordData).toString());

        }
        catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false, "Exception while Editing StackHolder for  " + entityName + ". " + e.getMessage());

        }
       customAssert.assertAll();
    }

   private void addSupplierManager(JSONObject jsonObject, JSONObject values){
             JSONArray rgValue= jsonObject.getJSONObject("stakeHolders").getJSONObject("values").names();
             for (int i=0; i<rgValue.length(); i++)
             {
                 if (jsonObject.getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(rgValue.getString(i)).getString("label").equalsIgnoreCase("Suppliers Manager"))
                 {
                    JSONArray supplierManagerValue= jsonObject.getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(rgValue.getString(i)).getJSONArray("values");
                    boolean allReadyPresent=false;
                    for (int k=0; k<supplierManagerValue.length(); k++)
                    {
                        if(jsonObject.getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(rgValue.getString(i)).getJSONArray("values").getJSONObject(k).getInt("id")==1054)
                        {
                             allReadyPresent=true;
                             break;
                        }
                    }
                    if (!allReadyPresent)
                    {
                        jsonObject.getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(rgValue.getString(i)).getJSONArray("values").put(values);
                    }

                 }
             }

   }
    private void addContractManager(JSONObject jsonObject, JSONObject values){
        JSONArray rgValue= jsonObject.getJSONObject("stakeHolders").getJSONObject("values").names();
        for (int i=0; i<rgValue.length(); i++)
        {
            if (jsonObject.getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(rgValue.getString(i)).getString("label").equalsIgnoreCase("Contract Manager"))
            {
                JSONArray supplierManagerValue= jsonObject.getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(rgValue.getString(i)).getJSONArray("values");
                boolean allReadyPresent=false;
                for (int k=0; k<supplierManagerValue.length(); k++)
                {
                    if(jsonObject.getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(rgValue.getString(i)).getJSONArray("values").getJSONObject(k).getInt("id")==1054)
                    {
                        allReadyPresent=true;
                        break;
                    }
                }
                if (!allReadyPresent)
                {
                    jsonObject.getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(rgValue.getString(i)).getJSONArray("values").put(values);
                }

            }
        }

    }
    private void removeOptionsFields(JSONObject jsonObject, String key) {
        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()) {
            nextKey = iterator.next();
            if (key.equalsIgnoreCase(nextKey)) {
                jsonObject.put(key,JSONObject.NULL);
            }
            else {
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    removeOptionsFields((JSONObject) jsonObject.get(nextKey),key);
                } else if (jsonObject.get(nextKey) instanceof JSONArray)
                    removeOptionsFields((JSONArray) jsonObject.get(nextKey),key);
            }
        }
    }

    private void removeOptionsFields(JSONArray jsonArray,String key) {

        for (int index = 0; index < jsonArray.length(); index++) {
            if (jsonArray.get(index) instanceof JSONArray)
                removeOptionsFields((JSONArray) jsonArray.get(index),key);
            else if (jsonArray.get(index) instanceof JSONObject)
                removeOptionsFields((JSONObject) jsonArray.get(index),key);
        }
    }
    public ArrayList<String> getAllEntity(String entityName) {
        ArrayList<String> allEntityId=new ArrayList<String>();
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Validating Listing Pagination for Entity {}.", entityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            String payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc\",\"filterJson\":{}},\"selectedColumns\":[]}";
            String listDataV2Response = ListDataHelper.getListDataResponseVersion2(entityName, payloadForListData, true);
            int size = 10;
            int totalRecords = ListDataHelper.getFilteredListDataCount(listDataV2Response);
            if (totalRecords == -1) {
                throw new SkipException("Couldn't get Total No of Records in Listing of Entity " + entityName);
            }
            if (totalRecords == 0) {
                logger.info("No Record found in Listing of Entity {}", entityName);
                return allEntityId;
            }
            logger.info("Total Records present for Entity {} are: {}", entityName, totalRecords);
                int offset = 0;
                do {
                    logger.info("Hitting ListData API for Entity {} with Size: {} and Offset: {}", entityName, size, offset);
                    payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size +
                            ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"6\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"5\",\"name\":\"Active\"},{\"id\":\"6\",\"name\":\"Inactive\"},{\"id\":\"1602\",\"name\":\"Newly Created\"}]},\"filterId\":6,\"filterName\":\"status\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[]}";

                    listDataV2Response = ListDataHelper.getListDataResponseVersion2(entityName, payloadForListData, true);
                    totalRecords = ListDataHelper.getFilteredListDataCount(listDataV2Response);
                    if (ParseJsonResponse.validJsonResponse(listDataV2Response)) {
                        JSONObject jsonObj = new JSONObject(listDataV2Response);
                        JSONArray jsonArr = jsonObj.getJSONArray("data");
                        for (int i=0; i<jsonArr.length(); i++)
                        {
                               JSONArray jsonArrayKey= jsonArr.getJSONObject(0).names();
                               for (int j=0; j<jsonArrayKey.length(); j++)
                               {
                                   jsonArr.getJSONObject(i).getJSONObject(jsonArrayKey.getString(j)).getString("columnName");
                                   if(jsonArr.getJSONObject(i).getJSONObject(jsonArrayKey.getString(j)).getString("columnName").equalsIgnoreCase("id")) {
                                       String []value=jsonArr.getJSONObject(i).getJSONObject(jsonArrayKey.getString(j)).getString("value").split(":;");
                                       allEntityId.add(value[1]);
                                       break;
                                   }
                               }
                        }
                    } else {
                        csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + ",size " + size + " and offset " +
                                offset + " is an Invalid JSON.");
                    }
                    offset += size;
                } while (offset <= totalRecords);
                return allEntityId;
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while get all Listing data for Entity " + entityName + ". " + e.getMessage());
        }
        csAssert.assertAll();
        return allEntityId;
    }
    @Test(dataProvider = "dataProviderForReportListId")
    public void scheduleReport(String reportListId) {
        CustomAssert customAssert = new CustomAssert();
        try {
            logger.info("************Hitting createScheduleReportForm API**************");
            HashMap<String, String> params = new HashMap<>();
            params.put("id", reportListId);
            CreateScheduleReportForm createScheduleReportForm = new CreateScheduleReportForm();
            createScheduleReportForm.hitCreateReportFormAPI(params);
            String responseCreateReportFormAPI = createScheduleReportForm.getResponseCreateReportFormAPI();
            if (ParseJsonResponse.validJsonResponse(responseCreateReportFormAPI)) {
                CreateScheduleReport createScheduleReport = new CreateScheduleReport();
                createScheduleReport.hitCreateScheduleReportAPI(createPayload(responseCreateReportFormAPI));
                logger.info("********************Hitting createScheduleReport API ***********************");
                String responseCreateScheduleReportAPI = createScheduleReport.getResponseCreateScheduleReportAPI();
                if (ParseJsonResponse.validJsonResponse(responseCreateReportFormAPI)) {
                    if (!responseCreateScheduleReportAPI.equalsIgnoreCase("create"))
                        customAssert.assertTrue(false, "report " + nameOfTheReport + "not schedule successfully");

                }
            }
        } catch (Exception e) {
            logger.error("Exception While Scheduling Report {}", nameOfTheReport);
            customAssert.assertTrue(false, e.getMessage());
        }
        customAssert.assertAll();
    }

    private String createPayload(String createReportFormAPIReponse) {
        String frequency = getFrequencyPayloadString();
        HashMap<String, String> timezoneIdMap = new HashMap<>();
        timezoneIdMap.put("id", timeZoneId);
        JSONObject createReportFormAPIJsonResponse = new JSONObject(createReportFormAPIReponse);
        nameOfTheReport = createReportFormAPIJsonResponse.getString("subject");
        String name = "automation_" + nameOfTheReport;
        String comment = "automation_" + nameOfTheReport;
        int entityTypeId = 0;
//        String filterJson = "{\n" +
//                "  \"filterMap\": {\n" +
//                "    \"entityTypeId\": " + entityTypeId + ",\n" +
//                "    \"offset\": 0,\n" +
//                "    \"size\": 20,\n" +
//                "    \"orderByColumnName\": \"id\",\n" +
//                "    \"orderDirection\": \"desc nulls last\",\n" +
//                "    \"filterJson\": {}\n" +
//                "  }\n" +
//                "}";
        String filterJson="{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"53\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3151\",\"name\":\"\",\"group\":[{\"name\":\"Anay User1\",\"id\":1054,\"idType\":2,\"$$hashKey\":\"object:1193\",\"selected\":true}],\"type\":2},{\"name\":\"Anay User1\",\"id\":1054,\"idType\":2,\"$$hashKey\":\"object:1193\",\"selected\":true},{\"id\":\"3170\",\"name\":\"\",\"group\":[{\"name\":\"Anay User1\",\"id\":1054,\"idType\":2,\"$$hashKey\":\"object:1648\",\"selected\":true}],\"type\":2},{\"name\":\"Anay User1\",\"id\":1054,\"idType\":2,\"$$hashKey\":\"object:1648\",\"selected\":true}]},\"filterId\":53,\"filterName\":\"stakeholder\",\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"uitype\":\"STAKEHOLDER\"}}},\"selectedColumns\":[{\"columnId\":1557,\"columnQueryName\":\"id\"},{\"columnId\":107234,\"columnQueryName\":\"Contract Manager Actions 3151ROLE_GROUP\"},{\"columnId\":107294,\"columnQueryName\":\"Suppliers Manager Actions 3170ROLE_GROUP\"}]}";
        JSONArray allUsers = createReportFormAPIJsonResponse.getJSONArray("allUsers");
        JSONArray selectedUsers = getSelectedUsersPayload(allUsers);
        if (createReportFormAPIJsonResponse.has("allUsers")) {
            createReportFormAPIJsonResponse.remove("allUsers");
        }
        createReportFormAPIJsonResponse.put("name", name);
        createReportFormAPIJsonResponse.put("comment", comment);
        createReportFormAPIJsonResponse.put("frequencyType", frequencyType);
        createReportFormAPIJsonResponse.put("frequency", frequency);
        createReportFormAPIJsonResponse.getJSONObject("scheduleReport").put("timeZone", timezoneIdMap);
        createReportFormAPIJsonResponse.getJSONObject("scheduleReport").put("filterJson", filterJson);
        createReportFormAPIJsonResponse.put("selectedUsers", selectedUsers);
        createReportFormAPIJsonResponse.put("externalEmails", new JSONArray().put(externalEmails));
        if (createReportFormAPIJsonResponse.has("timeZones")) {
            createReportFormAPIJsonResponse.remove("timeZones");
        }
        // createReportFormAPIJsonResponse.put("calendarTypeId",);
        logger.info("Payload is : {}", createReportFormAPIJsonResponse);
        return createReportFormAPIJsonResponse.toString();
    }

    private JSONArray getSelectedUsersPayload(JSONArray allUsers) {
        JSONArray selectedUsers = new JSONArray();
        for (String userByConfigFile : selectedUsersFromConfigFile) {
            boolean isExist = false;
            for (int i = 0; i < allUsers.length(); i++) {
                if (allUsers.get(i).toString().contains(userByConfigFile)) {
                    isExist = true;
                    selectedUsers.put(allUsers.get(i));
                    break;
                }
            }
            if (!isExist) {
                logger.info(userByConfigFile + " doesn't exist in AllUsers Json Array Object of ScheduleReport Form API Response");
            }
        }
        if (selectedUsers.length() == 0) {
            logger.info("Any User Mentioned in ScheduleReport.cfg is not matching with All Users Json Array Object of ScheduleReport Form API Response");
            selectedUsers.put(allUsers.get(0));
        }
        return selectedUsers;
    }

    private String getFrequencyPayloadString() {
        String frequencyPayloadString = null;
        SimpleDateFormat sdf = new SimpleDateFormat("M-dd-yyyy");
        Calendar c = Calendar.getInstance();
        c.setTime(new Date()); // Now use today date.
        String todayDate = sdf.format(c.getTime());

        c.setTime(new Date());
        c.add(Calendar.DATE, 1); // Adding 7 days
        String dateAfterOneDay = sdf.format(c.getTime());

        c.setTime(new Date());
        c.add(Calendar.DATE, 7); // Adding 7 days
        String dateAfter7Days = sdf.format(c.getTime());

        c.setTime(new Date());
        c.add(Calendar.DATE, 31); // Adding 31 days
        String dateAfter1Month = sdf.format(c.getTime());

        c.setTime(new Date());
        c.add(Calendar.DATE, 365); // Adding 365 days
        String dateAfter1Year = sdf.format(c.getTime());

        if (frequencyType.contentEquals("REPEATONCE"))
            frequencyPayloadString = "{\"FREQ\":\"REPEATONCE\",\"RRULE\":\"FREQ=REPEATONCE\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"16\",\"REPEATON\":\"\"}";

        if (frequencyType.contentEquals("DAILY"))
            frequencyPayloadString = "{\"FREQ\":\"DAILY\",\"RRULE\":\"FREQ=DAILY;INTERVAL=1\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"UNTIL\":\"" + dateAfter7Days + "\",\"REPEATON\":\"\"}";

        if (frequencyType.contentEquals("WEEKLY"))
            frequencyPayloadString = "{\"FREQ\":\"WEEKLY\",\"RRULE\":\"FREQ=WEEKLY;INTERVAL=1;BYDAY=MO\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"UNTIL\":\"" + dateAfter1Month + "\",\"REPEATON\":\"\"}";

        if (frequencyType.contentEquals("MONTHLY"))
            frequencyPayloadString = "{\"FREQ\":\"MONTHLY\",\"date\":\"1\",\"RRULE\":\"FREQ=MONTHLY;INTERVAL=1\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"UNTIL\":\"" + dateAfter1Year + "\",\"REPEATON\":\"DATE\"}";

        if (frequencyPayloadString == null)
            frequencyPayloadString = "{\"FREQ\":\"REPEATONCE\",\"RRULE\":\"FREQ=REPEATONCE\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"REPEATON\":\"\"}";

        return frequencyPayloadString;
    }
}
