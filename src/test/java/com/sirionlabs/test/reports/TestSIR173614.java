package com.sirionlabs.test.reports;

import com.sirionlabs.api.scheduleReport.CreateScheduleReport;
import com.sirionlabs.api.scheduleReport.CreateScheduleReportForm;
import com.sirionlabs.api.scheduleReport.ScheduleByMeReportAPI;
import com.sirionlabs.api.scheduleReport.SharedWithMeReportAPI;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;

public class TestSIR173614 {


    private final static Logger logger = LoggerFactory.getLogger(TestReportScheduled.class);
    private String frequencyType;
    private String timeZoneId;
    private String[] selectedUsersFromConfigFile;
    private String externalEmails;
    private String allReportToTest;
    private String nameOfTheReport;
    private String scheduleReportName;
    private String[] selectedUsersName;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        String scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSIR173614ConfigFilePath");
        String scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSIR173614ConfigFileName");
        frequencyType = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "frequencytype");
        timeZoneId = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "timezoneId");
        selectedUsersFromConfigFile = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "selectedusers").split(",");
        selectedUsersName = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "selectedusersname").split(",");

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
                if (ParseJsonResponse.validJsonResponse(responseCreateScheduleReportAPI)) {
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
        scheduleReportName = "automation_" + nameOfTheReport;
        String comment = "automation_" + nameOfTheReport;
        int entityTypeId = 0;
        String filterJson = "{\n" +
                "  \"filterMap\": {\n" +
                "    \"entityTypeId\": " + entityTypeId + ",\n" +
                "    \"offset\": 0,\n" +
                "    \"size\": 20,\n" +
                "    \"orderByColumnName\": \"id\",\n" +
                "    \"orderDirection\": \"desc nulls last\",\n" +
                "    \"filterJson\": {}\n" +
                "  }\n" +
                "}";
        //String filterJson="{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"53\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3151\",\"name\":\"\",\"group\":[{\"name\":\"Anay User1\",\"id\":1054,\"idType\":2,\"$$hashKey\":\"object:1193\",\"selected\":true}],\"type\":2},{\"name\":\"Anay User1\",\"id\":1054,\"idType\":2,\"$$hashKey\":\"object:1193\",\"selected\":true},{\"id\":\"3170\",\"name\":\"\",\"group\":[{\"name\":\"Anay User1\",\"id\":1054,\"idType\":2,\"$$hashKey\":\"object:1648\",\"selected\":true}],\"type\":2},{\"name\":\"Anay User1\",\"id\":1054,\"idType\":2,\"$$hashKey\":\"object:1648\",\"selected\":true}]},\"filterId\":53,\"filterName\":\"stakeholder\",\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"uitype\":\"STAKEHOLDER\"}}},\"selectedColumns\":[{\"columnId\":1557,\"columnQueryName\":\"id\"},{\"columnId\":107234,\"columnQueryName\":\"Contract Manager Actions 3151ROLE_GROUP\"},{\"columnId\":107294,\"columnQueryName\":\"Suppliers Manager Actions 3170ROLE_GROUP\"}]}";
        JSONArray allUsers = createReportFormAPIJsonResponse.getJSONArray("allUsers");
        JSONArray selectedUsers = getSelectedUsersPayload(allUsers);
        if (createReportFormAPIJsonResponse.has("allUsers")) {
            createReportFormAPIJsonResponse.remove("allUsers");
        }
        createReportFormAPIJsonResponse.put("name", scheduleReportName);
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

    @Test(dataProvider = "dataProviderForReportListId")
    public void testReportScheduledByMe(String reportListId)
    {
        CustomAssert customAssert=new CustomAssert();
           logger.info("Verifying SCHEDULED BY ME Tab");
        try {
            ScheduleByMeReportAPI scheduleByMeReportAPI=new ScheduleByMeReportAPI();
            scheduleByMeReportAPI.hitScheduleByMeReportAPI(102,Integer.parseInt(reportListId));
           String responseScheduleByMeReportAPI =scheduleByMeReportAPI.getResponseScheduleByMeReportAPI();
            if (ParseJsonResponse.validJsonResponse(responseScheduleByMeReportAPI))
           {
              String actualScheduledData= verifyScheduledByMe(responseScheduleByMeReportAPI,customAssert);
              if(actualScheduledData!=null) {
                  JSONObject actualData = new JSONObject(actualScheduledData);
                  JSONArray jsonArrayName=actualData.names();
                  for (int i=0; i<jsonArrayName.length(); i++)
                  {
                     if(actualData.getJSONObject(jsonArrayName.getString(i)).getString("columnName").equalsIgnoreCase("schedulename"))
                     {
                         if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase(scheduleReportName))
                         {
                             customAssert.assertTrue(false,"scheduleReportName "+actualData.getJSONObject(jsonArrayName.getString(i)).getString("value")+"and Expected report name different "+scheduleReportName);
                         }
                     }
                      else if(actualData.getJSONObject(jsonArrayName.getString(i)).getString("columnName").equalsIgnoreCase("status"))
                      {
                          if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("ACTIVE"))
                          {
                              customAssert.assertTrue(false,"status "+actualData.getJSONObject(jsonArrayName.getString(i)).getString("value")+"and Expected status different "+"Active");

                          }
                      }
                     else if(actualData.getJSONObject(jsonArrayName.getString(i)).getString("columnName").equalsIgnoreCase("name"))
                      {
                          if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase(nameOfTheReport))
                          {
                              customAssert.assertTrue(false,"Report Name "+actualData.getJSONObject(jsonArrayName.getString(i)).getString("value")+"and Expected report name different "+nameOfTheReport);

                          }
                      }
                     else if(actualData.getJSONObject(jsonArrayName.getString(i)).getString("columnName").equalsIgnoreCase("frequencytype"))
                     {

                             if (frequencyType.equalsIgnoreCase("REPEATONCE")) {
                                 if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Repeat Once"))
                                     customAssert.assertTrue(false, "frequencyType " + actualData.getJSONObject(jsonArrayName.getString(i)).getString("value") + "and Expected frequency Type different " + frequencyType);
                             }
                             else if (frequencyType.equalsIgnoreCase("DAILY"))
                             {
                                 if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Daily"))
                                     customAssert.assertTrue(false, "frequencyType " + actualData.getJSONObject(jsonArrayName.getString(i)).getString("value") + "and Expected frequency Type different " + frequencyType);
                             }
                             else if (frequencyType.equalsIgnoreCase("WEEKLY"))
                             {
                                 if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Weekly"))
                                     customAssert.assertTrue(false, "frequencyType " + actualData.getJSONObject(jsonArrayName.getString(i)).getString("value") + "and Expected frequency Type different " + frequencyType);
                             }
                             else if (frequencyType.equalsIgnoreCase("MONTHLY"))
                             {
                                 if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Monthly"))
                                     customAssert.assertTrue(false, "frequencyType " + actualData.getJSONObject(jsonArrayName.getString(i)).getString("value") + "and Expected frequency Type different " + frequencyType);
                             }
                     }
                  }
              }
              else
              {
                  customAssert.assertTrue(false,"no record found in SCHEDULED BY ME report");
              }
           }
        }
        catch (Exception e)
        {
            logger.error("Exception While Verifying SCHEDULED BY ME {}",e.getMessage());
            customAssert.assertTrue(false,"Exception While Verifying SCHEDULED BY ME");
        }
        customAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderForReportListId")
    public void testSharedWithMeReport(String reportListId)
    {

        CustomAssert customAssert=new CustomAssert();
        logger.info("Verifying SHARED WITH ME Tab");
        try {
            SharedWithMeReportAPI sharedWithMeReportAPI=new SharedWithMeReportAPI();

            String payload="{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1000,\"orderByColumnName\":\"upcoming\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{},\"scheduleEntityTypeId\":1}}";

             sharedWithMeReportAPI.hitSharedWithMeReportAPI(103,reportListId,payload);
            String responseSharedWithMeReportAPI =sharedWithMeReportAPI.getResponseSharedWithMeReportAPI();

            if (ParseJsonResponse.validJsonResponse(responseSharedWithMeReportAPI))
            {
                String actualScheduledData= verifySharedWithMe(responseSharedWithMeReportAPI,customAssert);
                if(actualScheduledData!=null) {
                    JSONObject actualData = new JSONObject(actualScheduledData);
                    JSONArray jsonArrayName=actualData.names();
                    for (int i=0; i<jsonArrayName.length(); i++)
                    {
                        if(actualData.getJSONObject(jsonArrayName.getString(i)).getString("columnName").equalsIgnoreCase("sharedby"))
                        {
                            if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase(selectedUsersName[0]))
                            {
                                customAssert.assertTrue(false,"shared by "+actualData.getJSONObject(jsonArrayName.getString(i)).getString("value")+"and Expected shared by different "+selectedUsersName[0]);

                            }
                        }
                        else if(actualData.getJSONObject(jsonArrayName.getString(i)).getString("columnName").equalsIgnoreCase("status"))
                        {
                            if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Active"))
                            {
                                customAssert.assertTrue(false,"status "+actualData.getJSONObject(jsonArrayName.getString(i)).getString("value")+"and Expected status different "+"Active");

                            }
                        }
                        else if(actualData.getJSONObject(jsonArrayName.getString(i)).getString("columnName").equalsIgnoreCase("name"))
                        {
                            if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase(nameOfTheReport))
                            {
                                customAssert.assertTrue(false,"Report Name "+actualData.getJSONObject(jsonArrayName.getString(i)).getString("value")+"and Expected report name different "+nameOfTheReport);

                            }
                        }
                        else if(actualData.getJSONObject(jsonArrayName.getString(i)).getString("columnName").equalsIgnoreCase("frequencytype"))
                        {
                            if (frequencyType.equalsIgnoreCase("REPEATONCE")) {
                                if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Repeat Once"))
                                    customAssert.assertTrue(false, "frequencyType " + actualData.getJSONObject(jsonArrayName.getString(i)).getString("value") + "and Expected frequency Type different " + frequencyType);
                            }
                            else if (frequencyType.equalsIgnoreCase("DAILY"))
                            {
                                if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Daily"))
                                    customAssert.assertTrue(false, "frequencyType " + actualData.getJSONObject(jsonArrayName.getString(i)).getString("value") + "and Expected frequency Type different " + frequencyType);
                            }
                            else if (frequencyType.equalsIgnoreCase("WEEKLY"))
                            {
                                if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Weekly"))
                                    customAssert.assertTrue(false, "frequencyType " + actualData.getJSONObject(jsonArrayName.getString(i)).getString("value") + "and Expected frequency Type different " + frequencyType);
                            }
                            else if (frequencyType.equalsIgnoreCase("MONTHLY"))
                            {
                                if (!actualData.getJSONObject(jsonArrayName.getString(i)).getString("value").equalsIgnoreCase("Monthly"))
                                    customAssert.assertTrue(false, "frequencyType " + actualData.getJSONObject(jsonArrayName.getString(i)).getString("value") + "and Expected frequency Type different " + frequencyType);
                            }
                        }
                    }
                }
                else
                {
                    customAssert.assertTrue(false,"no record found SHARED WITH ME Table");
                }

            }
        }
        catch (Exception e)
        {
            logger.error("Exception While Verifying SHARED WITH ME {}",e.getMessage());
            customAssert.assertTrue(false,"Exception While Verifying SHARED WITH ME");
        }
        customAssert.assertAll();
    }
     private String verifyScheduledByMe(String response,CustomAssert customAssert)
     {
         try {
             SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy");
             Calendar c = Calendar.getInstance();
             c.setTime(new Date()); // Now use today date.
             String todayDate = sdf.format(c.getTime());
             if(ParseJsonResponse.validJsonResponse(response))
             {
                  JSONArray jsonArrayResponse=new JSONObject(response).getJSONArray("data");
                  for (int i=0; i<jsonArrayResponse.length();i++)
                  {
                       JSONArray jsonArray=jsonArrayResponse.getJSONObject(i).names();
                       for (int j=0; j<jsonArray.length(); j++)
                       {
                              if (jsonArrayResponse.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("columnName").equalsIgnoreCase("nextsend")) {
                                  if (jsonArrayResponse.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("value").equalsIgnoreCase(todayDate))
                                           return jsonArrayResponse.getJSONObject(i).toString();
                              }
                       }
                  }
             }
         }
         catch (Exception e)
         {
              logger.error("Exception While Verifying SCHEDULED BY ME And SHARED WITH ME");
         }
         return null;
     }
    private String verifySharedWithMe(String response,CustomAssert customAssert)
    {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy");
            Calendar c = Calendar.getInstance();
            c.setTime(new Date()); // Now use today date.
            String todayDate = sdf.format(c.getTime());
            if(ParseJsonResponse.validJsonResponse(response))
            {
                JSONArray jsonArrayResponse=new JSONObject(response).getJSONArray("data");
                for (int i=0; i<jsonArrayResponse.length();i++)
                {
                    JSONArray jsonArray=jsonArrayResponse.getJSONObject(i).names();
                    for (int j=0; j<jsonArray.length(); j++)
                    {
                        if (jsonArrayResponse.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("columnName").equalsIgnoreCase("upcoming")) {
                            if (jsonArrayResponse.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("value").equalsIgnoreCase(todayDate))
                                return jsonArrayResponse.getJSONObject(i).toString();
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Exception While Verifying SHARED WITH ME");
        }
        return null;
    }
}
