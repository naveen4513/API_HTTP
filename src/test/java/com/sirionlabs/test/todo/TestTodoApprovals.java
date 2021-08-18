package com.sirionlabs.test.todo;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.governancebody.AdhocMeeting;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestTodoApprovals extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestTodoApprovals.class);
	private static String configFilePath;
	private static String configFileName;
	private int bufferBeforeDate;
	private int bufferAfterDate;
	private String dateFormat;
	private boolean applyRandomization = false;
	private int maxRecordsToValidate = 3;
	private String validGBStatus = null;
	private static String gbconfigFileName;
	private static String gbconfigFilePath;
	private static String gbextraFieldsConfigFileName;
	private int governanceBodyId = 0;
	private String sectionName = "todo";

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TodoConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("TodoApprovalsConfigFileName");
		bufferBeforeDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datevalidationbufferbefore"));
		bufferAfterDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datevalidationbufferafter"));
		dateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateformat");
		validGBStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "gbstatus");

		gbconfigFileName = ConfigureConstantFields.getConstantFieldsProperty("GBFileName");
		gbconfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("GBFilePath");
		gbextraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("GBExtraFieldsFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyrandomization");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			applyRandomization = true;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordstovalidate");
		if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
			maxRecordsToValidate = Integer.parseInt(temp);

		testCasesMap = getTestCasesMapping();
	}


	@Test
	public void TestApprovalonstatus() {
		boolean resultPass = false;
		String failureMsg = "";
		CustomAssert csAssert = new CustomAssert();

		String createResponse = GovernanceBody.createGB(gbconfigFilePath, gbconfigFileName, gbconfigFilePath, gbextraFieldsConfigFileName, sectionName,
				true);
		governanceBodyId = CreateEntity.getNewEntityId(createResponse);

		logger.info("Hitting Todo Daily Api");
		TodoDaily todoDailyObj = new TodoDaily();
		todoDailyObj.hitTodoDaily();
		if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {

			net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray) JSONUtility.parseJson(todoDailyObj.getTodoDailyJsonStr(), "$.['Pending Approvals'][?(@.id ==" + governanceBodyId + " && @.statusName =='Newly Created' )]");
			if (actionArray.size() == 1) {
				String timezone = (String) JSONUtility.parseJson(actionArray.toJSONString(), "$.[0].timeZone.name");
				int entityTypeId = (int) JSONUtility.parseJson(actionArray.toJSONString(), "$.[0].entityTypeId");
				String supplier = (String) JSONUtility.parseJson(actionArray.toJSONString(), "$.[0].relationName");
				String name = (String) JSONUtility.parseJson(actionArray.toJSONString(), "$.[0].name");

				csAssert.assertEquals(timezone, "Asia/Kolkata (GMT +05:30)", "timezone is not correct");
				Show showObj = new Show();
				showObj.hitShow(86, governanceBodyId);
				String showJsonStr = showObj.getShowJsonStr();
				//Verify entitytypeid
				resultPass = showObj.verifyShowField(showJsonStr, "entitytypeid", Integer.toString(86), entityTypeId, "int");
				if (!resultPass) {
					failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + governanceBodyId + " failed on Show Page for Field EntityTypeId";
					csAssert.assertTrue(false, failureMsg);
				}
				//Verify Supplier
				logger.info("Verifying Supplier");
				resultPass = showObj.verifyShowField(showJsonStr, "supplier", supplier, 86, "text");
				if (!resultPass) {
					failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + governanceBodyId + " failed on Show Page for Field Supplier";
					csAssert.assertTrue(false, failureMsg);
				}

				//verify Name
				logger.info("Verifying Name");
				resultPass = showObj.verifyShowField(showJsonStr, "name", name, 86, "text");
				if (!resultPass) {
					failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + governanceBodyId + " failed on Show Page for Field Supplier";
					csAssert.assertTrue(false, failureMsg);
				}
			} else {
				csAssert.assertTrue(false, "GB --> " + governanceBodyId + " not visible under TODO approval with status Newly Created");
			}
		} else {
			logger.error("Todo Daily Api response is not a valid json");
		}

		logger.info("workflow steps started");

		EntityWorkflowActionHelper helper = new EntityWorkflowActionHelper();
		helper.hitWorkflowAction("GB", 86, governanceBodyId, "Send For Internal Review");
		logger.info("Hitting Todo Daily Api");
		todoDailyObj.hitTodoDaily();
		if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {

			net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray) JSONUtility.parseJson(todoDailyObj.getTodoDailyJsonStr(), "$.['Pending Approvals'][?(@.id ==" + governanceBodyId + " && @.statusName =='Awaiting Internal Review' )]");
			if (actionArray.size() != 0) {
				csAssert.assertTrue(false, "GB --> " + governanceBodyId + " Should not be visible under TODO approval with status awaiting internal review");
			}
		} else {
			logger.error("Todo Daily Api response is not a valid json");
		}

			csAssert.assertAll();
	}


	@Test
	public void TestApprovalonChildstatus() throws ParseException {
		boolean resultPass = false;
		String failureMsg ="";
		CustomAssert csAssert = new CustomAssert();
		AdhocMeeting meet = new AdhocMeeting();
		String adhocResponse1 = meet.hitAdhocMeetingApi(String.valueOf(governanceBodyId),DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(),1,dateFormat),"23:30","Asia/Kolkata (GMT +05:30)","30 Min","Delhi");
		if(!adhocResponse1.equals("Cannot create conflicting meeting instances.")){
			logger.info("adhoc meeting successfully created on "+DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(),1,dateFormat)+" for governanceBodyId-->  "+ governanceBodyId);
		}else{
			logger.error("adhoc meeting not created");
			csAssert.assertTrue(false,"adhoc meeting not created");
		}

		TabListData listData = new TabListData();
		String gb_res =  listData.hitTabListData(Integer.valueOf(213),Integer.valueOf(86),Integer.valueOf(governanceBodyId));
		List<String> mettingIds = ListDataHelper.getColumnIds(gb_res);
		logger.info("Hitting Todo Daily Api");
		TodoDaily todoDailyObj = new TodoDaily();
		todoDailyObj.hitTodoDaily();

		if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {
			net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray)JSONUtility.parseJson(todoDailyObj.getTodoDailyJsonStr(), "$.['Pending Approvals'][?(@.id =="+mettingIds.get(0)+"  && @.statusName =='Upcoming Meeting' )]");
			if (actionArray.size() == 1) {
				String timezone = (String) JSONUtility.parseJson(actionArray.toJSONString(), "$.[0].timeZone.name");
				int entityTypeId = (int) JSONUtility.parseJson(actionArray.toJSONString(), "$.[0].entityTypeId");
				String supplier = (String) JSONUtility.parseJson(actionArray.toJSONString(), "$.[0].relationName");
				String name = (String) JSONUtility.parseJson(actionArray.toJSONString(), "$.[0].name");

				csAssert.assertEquals(timezone, "Asia/Kolkata (GMT +05:30)", "timezone is not correct");
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

				//verify Name
				logger.info("Verifying Name");
				resultPass = showObj.verifyShowField(showJsonStr, "name", name, 87, "text");
				if (!resultPass) {
					failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + mettingIds.get(0) + " failed on Show Page for Field Supplier";
					csAssert.assertTrue(false, failureMsg);
				}
			} else {
				csAssert.assertTrue(false, "GB --> " + mettingIds.get(0) + " not visible under TODO approval with status UPCOMING Meeting");
			}
		}else {
			logger.error("Todo Daily Api response is not a valid json");
		}

		EntityWorkflowActionHelper helper = new EntityWorkflowActionHelper();
		helper.hitWorkflowAction("CGB", 87, Integer.parseInt(mettingIds.get(0)), "On Hold");
		logger.info("Hitting Todo Daily Api");
		todoDailyObj.hitTodoDaily();
		if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {

			net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray) JSONUtility.parseJson(todoDailyObj.getTodoDailyJsonStr(), "$.['Pending Approvals'][?(@.id ==" + mettingIds.get(0) + ")]");
			if (actionArray.size() != 0) {
				csAssert.assertTrue(false, "CGB --> " + mettingIds.get(0) + " Should not be visible under TODO approval with status On hold");
			}

		}else {
			logger.error("Todo Daily Api response is not a valid json");
		}

		helper.hitWorkflowAction("CGB", 87, Integer.parseInt(mettingIds.get(0)), "Activate");
		logger.info("Hitting Todo Daily Api");
		todoDailyObj.hitTodoDaily();
		if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {

			net.minidev.json.JSONArray actionArray = (net.minidev.json.JSONArray) JSONUtility.parseJson(todoDailyObj.getTodoDailyJsonStr(), "$.['Pending Approvals'][?(@.id ==" + mettingIds.get(0)+")]" );
			if (actionArray.size() != 1) {
				csAssert.assertTrue(false, "CGB --> " + mettingIds.get(0) + " Should  be visible under TODO approval with status Upcoming Meeting");
			}

		}else {
			logger.error("Todo Daily Api response is not a valid json");
		}

		helper.hitWorkflowAction("GB", 86, governanceBodyId, "On Hold");
		logger.info("Hitting Todo Daily Api");
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
	}

}