package com.sirionlabs.test.todo;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.governancebody.AdhocMeeting;
import com.sirionlabs.api.governancebody.DeleteAdhocMeeting;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.todo.TodoDaily;
import com.sirionlabs.api.todo.TodoWeekly;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestTodoMeetings extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestTodoMeetings.class);
    private static String configFilePath;
    private static String configFileName;
    private static String gbconfigFileName;
    private static String gbconfigFilePath;
    private static String gbextraFieldsConfigFileName;
    private int weeklybufferBeforeDate;
    private int weeklybufferAfterDate;
    private int dailybufferBeforeDate;
    private int dailybufferAfterDate;
    private String dateFormat;
    private boolean applyRandomization = false;
    private int maxRecordsToValidate = 3;
    private String sectionName = "todo";
    int governanceBodyId =0;


    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TodoConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TodoMeetingsConfigFileName");
        gbconfigFileName = ConfigureConstantFields.getConstantFieldsProperty("GBFileName");
        gbconfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("GBFilePath");
        gbextraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty( "GBExtraFieldsFileName");

        weeklybufferBeforeDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "weekly","datevalidationbufferbefore"));
        weeklybufferAfterDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"weekly", "datevalidationbufferafter"));
        dailybufferBeforeDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "daily","datevalidationbufferbefore"));
        dailybufferAfterDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"daily", "datevalidationbufferafter"));

        dateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateformat");
        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyrandomization");
        if (temp != null && temp.trim().equalsIgnoreCase("true"))
            applyRandomization = true;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordstovalidate");
        if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
            maxRecordsToValidate = Integer.parseInt(temp);

        testCasesMap = getTestCasesMapping();

          String createResponse = GovernanceBody.createGB(gbconfigFilePath, gbconfigFileName, gbconfigFilePath, gbextraFieldsConfigFileName, sectionName,
                true);
        governanceBodyId = CreateEntity.getNewEntityId(createResponse);
    }


    @Test
    public void testDailyMeetings() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Hitting Todo Daily Api");
        TodoDaily todoDailyObj = new TodoDaily();
        todoDailyObj.hitTodoDaily();

        if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {
            todoDailyObj.setAllEntities(todoDailyObj.getTodoDailyJsonStr());
            List<Map<String, String>> dailyMeetings = todoDailyObj.getMeetings();
            logger.info("Total Weekly Meeting found: {}", dailyMeetings.size());

            if (dailyMeetings.size() > 0) {
                List<Map<String, String>> meetingsToValidate = new ArrayList<>();
                if (applyRandomization) {
                    logger.info("Maximum No of Records to Validate: {}", maxRecordsToValidate);
                    int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, dailyMeetings.size() - 1, maxRecordsToValidate);
                    for (int randomNumber : randomNumbersForRecords) {
                        meetingsToValidate.add(dailyMeetings.get(randomNumber));
                    }
                } else {
                    meetingsToValidate.addAll(dailyMeetings);
                }

                for (Map<String, String> meeting : meetingsToValidate) {
                    verifyTodoMeetings(meeting, csAssert,dailybufferBeforeDate,dailybufferAfterDate);
                }
            }
        }
        addTestResult(getTestCaseIdForMethodName("testDailyTasks"), csAssert);
        csAssert.assertAll();
    }



    @Test
    public void testWeeklyMeetings() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Hitting Todo Weekly Api");
        TodoWeekly todoWeeklyObj = new TodoWeekly();
        todoWeeklyObj.hitTodoWeekly();

        if (ParseJsonResponse.validJsonResponse(todoWeeklyObj.getTodoWeeklyJsonStr())) {
            todoWeeklyObj.setAllEntities(todoWeeklyObj.getTodoWeeklyJsonStr());
            List<Map<String, String>> weeklyMeetings = todoWeeklyObj.getMeetings();
            logger.info("Total Weekly Meeting found: {}", weeklyMeetings.size());

            if (weeklyMeetings.size() > 0) {
                List<Map<String, String>> meetingsToValidate = new ArrayList<>();
                if (applyRandomization) {
                    logger.info("Maximum No of Records to Validate: {}", maxRecordsToValidate);
                    int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, weeklyMeetings.size() - 1, maxRecordsToValidate);
                    for (int randomNumber : randomNumbersForRecords) {
                        meetingsToValidate.add(weeklyMeetings.get(randomNumber));
                    }
                } else {
                    meetingsToValidate.addAll(weeklyMeetings);
                }

                for (Map<String, String> meeting : meetingsToValidate) {
                    verifyTodoMeetings(meeting, csAssert,weeklybufferBeforeDate,weeklybufferAfterDate);
                }
            }
        }
        addTestResult(getTestCaseIdForMethodName("testWeeklyTasks"), csAssert);
        csAssert.assertAll();
    }

    @Test
    public  void newDailyMeeting (){
        CustomAssert csAssert = new CustomAssert();

        AdhocMeeting meet = new AdhocMeeting();
       String adhocResponse = meet.hitAdhocMeetingApi(String.valueOf(governanceBodyId),DateUtils.getCurrentDateInMM_DD_YYYY(),"23:30","Asia/Kolkata (GMT +05:30)","30 Min","Delhi");
       if(!adhocResponse.equals("Cannot create conflicting meeting instances.")){
           logger.info("adhoc meeting successfully created on "+DateUtils.getCurrentDateInMM_DD_YYYY()+" for governanceBodyId-->  "+governanceBodyId);
       }else{
           logger.error("adhoc meeting not created on "+DateUtils.getCurrentDateInMM_DD_YYYY());
       }

        TabListData listData = new TabListData();
        String gb_res =  listData.hitTabListData(Integer.valueOf(213),Integer.valueOf(86),Integer.valueOf(governanceBodyId));
        List<String> mettingIds = ListDataHelper.getColumnIds(gb_res);

        logger.info("Hitting Todo Daily Api");
        TodoDaily todoDailyObj = new TodoDaily();
        todoDailyObj.hitTodoDaily();

        if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {

            net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray)JSONUtility.parseJson(todoDailyObj.getTodoDailyJsonStr(), "$.['Governance Meetings'][?(@.id =="+mettingIds.get(0)+" )]");
            String timezone = (String) JSONUtility.parseJson(actionArray.toJSONString(),"$.[0].timeZone.name");
            csAssert.assertEquals(timezone,"Asia/Kolkata (GMT +05:30)","timezone is not correct");
            String date = DateUtils.getUTCDateFromEpoch((long)JSONUtility.parseJson(actionArray.toJSONString(),"$.[0].dueDateTimestamp"),dateFormat);
            csAssert.assertEquals(date,DateUtils.getCurrentDateInMM_DD_YYYY(),"Due date is not correct");

        }
        else{
            logger.error("Todo Daily Api response is not a valid json");
        }


        EntityWorkflowActionHelper helper = new EntityWorkflowActionHelper();
        helper.hitWorkflowAction("CGB", 87, Integer.parseInt(mettingIds.get(0)), "On Hold");
        todoDailyObj.hitTodoDaily();
        if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {

            net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray) JSONUtility.parseJson(todoDailyObj.getTodoDailyJsonStr(), "$.['Pending Approvals'][?(@.id ==" + mettingIds.get(0) + ")]");
            if (actionArray.size() != 0) {
                csAssert.assertTrue(false, "CGB --> " + mettingIds.get(0) + " Should not be visible under TODO approval with status On hold");
            }

        }else {
            logger.error("Todo Daily Api response is not a valid json");
        }


        csAssert.assertAll();
        helper.hitWorkflowAction("CGB", 87, Integer.parseInt(mettingIds.get(0)), "Activate");
        DeleteAdhocMeeting.deleteAdhocMeeting(Integer.valueOf(mettingIds.get(0)));
       }

   @Test
    public  void newWeeklyMeeting () throws ParseException {
        boolean resultPass = false;
        String failureMsg ="";
        CustomAssert csAssert = new CustomAssert();
        AdhocMeeting meet = new AdhocMeeting();
        String adhocResponse1 = meet.hitAdhocMeetingApi(String.valueOf(governanceBodyId),DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(),1,dateFormat),"23:30","Asia/Kolkata (GMT +05:30)","30 Min","Delhi");
        if(!adhocResponse1.equals("Cannot create conflicting meeting instances.")){
            logger.info("adhoc meeting successfully created on "+DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(),1,dateFormat)+" for governanceBodyId-->  "+1633);
        }else{
            logger.error("adhoc meeting not created");
       csAssert.assertTrue(false,"adhoc meeting not created");
        }

        TabListData listData = new TabListData();
        String gb_res =  listData.hitTabListData(Integer.valueOf(213),Integer.valueOf(86),Integer.valueOf(governanceBodyId));
       List<String> mettingIds = ListDataHelper.getColumnIds(gb_res);

        logger.info("Hitting Todo Weekly Api");
        TodoWeekly todoWeeklyObj = new TodoWeekly();
        todoWeeklyObj.hitTodoWeekly();

        if (ParseJsonResponse.validJsonResponse(todoWeeklyObj.getTodoWeeklyJsonStr())) {

            net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray)JSONUtility.parseJson(todoWeeklyObj.getTodoWeeklyJsonStr(), "$.['Governance Meetings'][?(@.id =="+mettingIds.get(0)+" )]");
            String timezone = (String) JSONUtility.parseJson(actionArray.toJSONString(),"$.[0].timeZone.name");
            int entityTypeId = (int) JSONUtility.parseJson(actionArray.toJSONString(),"$.[0].entityTypeId");
            String supplier = (String) JSONUtility.parseJson(actionArray.toJSONString(),"$.[0].relationName");
            csAssert.assertEquals(timezone,"Asia/Kolkata (GMT +05:30)","timezone is not correct");
            String date = DateUtils.getUTCDateFromEpoch((long)JSONUtility.parseJson(actionArray.toJSONString(),"$.[0].dueDateTimestamp"),dateFormat);
            csAssert.assertEquals(date,DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(),1,dateFormat),"Due date is not correct");
            Show showObj = new Show();
            showObj.hitShow(87, Integer.parseInt(mettingIds.get(0)));
            String showJsonStr = showObj.getShowJsonStr();
            //Verify entitytypeid
            resultPass = showObj.verifyShowField(showJsonStr, "entitytypeid", Integer.toString(87), entityTypeId, "int");
            if (!resultPass) {
                failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + mettingIds.get(0) + " failed on Show Page for Field EntityTypeId";
                csAssert.assertTrue(false, failureMsg);
            }
            //Verify Supplier
            logger.info("Verifying Supplier");
             resultPass = showObj.verifyShowField(showJsonStr, "supplier", supplier, 87, "text");
            if (!resultPass) {
                failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + mettingIds.get(0) + " failed on Show Page for Field Supplier";
                csAssert.assertTrue(false, failureMsg);
            }
        }
        else{
            logger.error("Todo Weekly Api response is not a valid json");
        }
        EntityWorkflowActionHelper helper = new EntityWorkflowActionHelper();
        helper.hitWorkflowAction("CGB", 87, Integer.parseInt(mettingIds.get(0)), "On Hold");
        todoWeeklyObj.hitTodoWeekly();
        if (ParseJsonResponse.validJsonResponse(todoWeeklyObj.getTodoWeeklyJsonStr())) {

            net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray) JSONUtility.parseJson(todoWeeklyObj.getTodoWeeklyJsonStr(), "$.['Pending Approvals'][?(@.id ==" + mettingIds.get(0) + ")]");
            if (actionArray.size() != 0) {
                csAssert.assertTrue(false, "CGB --> " + mettingIds.get(0) + " Should not be visible under TODO approval with status On hold");
            }

        }else {
            logger.error("Todo Daily Api response is not a valid json");
        }


        csAssert.assertAll();
        helper.hitWorkflowAction("CGB", 87, Integer.parseInt(mettingIds.get(0)), "Activate");
        DeleteAdhocMeeting.deleteAdhocMeeting(Integer.valueOf(mettingIds.get(0)));
    }


    private void verifyTodoMeetings(Map<String, String> record, CustomAssert csAssert,int bufferBeforeDate, int bufferAfterDate) {
        String currentDate = null;
        try {
            int entityTypeId = Integer.parseInt(record.get("entityTypeId"));

            Show showObj = new Show();
            logger.info("Hitting Show Api for Record having EntityTypeId {} and Id {}", record.get("entityTypeId"), record.get("id"));
            showObj.hitShow(Integer.parseInt(record.get("entityTypeId")), Integer.parseInt(record.get("id")));
            String showJsonStr = showObj.getShowJsonStr();

            if (!ParseJsonResponse.validJsonResponse(showJsonStr)) {
                logger.error("Invalid Show Json Response for Record having EntityTypeId {} and Id {}", record.get("entityTypeId"), record.get("id"));
                csAssert.assertTrue(false, "Invalid Show Json Response for Record having EntityTypeId " + record.get("entityTypeId") + " and Id " +
                        record.get("id"));
            } else {
                logger.info("Verifying Record");

                //Verify if No Error
                logger.info("Verifying if No Error in response");
                JSONObject jsonObj = new JSONObject(showJsonStr);
                jsonObj = jsonObj.getJSONObject("header").getJSONObject("response");

                if (jsonObj.getString("status").equalsIgnoreCase("applicationError")) {
                    logger.error("Application Error in Show Response for Record having EntityId {} and Id {}.", entityTypeId, record.get("id"));
                    csAssert.assertTrue(false, "Application Error in Show Response for Record having EntityTypeId " + entityTypeId + " and Id " +
                            record.get("id"));
                } else {
                    String failureMsg;
                    boolean resultPass;

                    //Verify Id
                    logger.info("Verifying Id");
                    resultPass = showObj.verifyShowField(showJsonStr, "id", record.get("id"), entityTypeId, "int");
                    if (!resultPass) {
                        failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + record.get("id") + " failed on Show Page for Field Id";
                        csAssert.assertTrue(false, failureMsg);
                    }

                    //Verify EntityTypeId
                    logger.info("Verifying EntityTypeId");
                    resultPass = showObj.verifyShowField(showJsonStr, "entitytypeid", Integer.toString(entityTypeId), entityTypeId, "int");
                    if (!resultPass) {
                        failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + record.get("id") + " failed on Show Page for Field EntityTypeId";
                        csAssert.assertTrue(false, failureMsg);
                    }

                    //Verify Supplier
                    if (record.get("supplier") != null && !record.get("supplier").trim().equalsIgnoreCase("")) {
                        logger.info("Verifying Supplier");
                        resultPass = showObj.verifyShowField(showJsonStr, "supplier", record.get("supplier"), entityTypeId, "text");
                        if (!resultPass) {
                            failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + record.get("id") + " failed on Show Page for Field Supplier";
                            csAssert.assertTrue(false, failureMsg);
                        }
                    } else {
                        logger.info("Couldn't get Supplier for Record having Id {}. Hence not verifying Supplier.", record.get("id"));
                    }

                    //Verify Due Date
					long currentDateTimeStamp =System.currentTimeMillis();
					if (currentDateTimeStamp != 0) {
						logger.info("Verifying Due Date");
						Date date = new Date(currentDateTimeStamp);
						SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
						 currentDate = sdf.format(date);
					}else{
					    logger.error("cuurent date is not gettting fetched");
                    }
                    String actualDate = (record.get("dueDateStr").split(" "))[0];
                    if (actualDate != "") {
                    String beforeDate = DateUtils.getDateOfXDaysFromYDate(currentDate,(-bufferAfterDate), dateFormat);
						String afterDate = DateUtils.getDateOfXDaysFromYDate(currentDate, bufferAfterDate, dateFormat);
						String expectedDateRange = beforeDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + afterDate;

						String fieldName = null;
						if (ParseConfigFile.hasProperty(configFilePath, configFileName, "todoshowpageduedatemapping", String.valueOf(entityTypeId)))
							fieldName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todoshowpageduedatemapping",
									String.valueOf(entityTypeId));

						else if (ParseConfigFile.hasProperty(configFilePath, configFileName, "todoshowpageduedatemapping", "0"))
							fieldName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todoshowpageduedatemapping", "0");

						else {
							logger.info("Todo Show Page Due Date Mapping not found for Entity Type Id {} and also Default Mapping is not present. " +
									"Hence skipping Due Date Validation");
						}

						if (fieldName != null) {
							resultPass = showObj.verifyShowField(showJsonStr, fieldName.trim().toLowerCase(), expectedDateRange, entityTypeId, "date",
									dateFormat);
							if (!resultPass) {
								failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + record.get("id") + " failed on Show Page for Due Date";
								csAssert.assertTrue(false, failureMsg);
							}
						}
					}
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying Tasks. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Verifying Tasks. " + e.getMessage());
        }
    }

}
