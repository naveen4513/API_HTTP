package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.UserListMetaData;
import com.sirionlabs.api.userPreference.UserPreferenceData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.test.reportRenderer.FilterData;
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


import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by shivashish on 11/7/17.
 */
public class TestUserPreference extends TestRailBase {
	private final static Logger logger = LoggerFactory.getLogger(TestUserPreference.class);

//	CustomAssert csAssertion;
	CustomAssert customAssert;
	String createdPublicViewName;
	String createdPrivateViewName;
	String baseFilePath;
	String[] existingViewIds;

	UserPreferenceData userPreferenceData;
	ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData;
	ListRendererFilterData listRendererFilterData;

	String reportRendererConfigFilePath;
	String reportRendererConfigFileName;
	String responseCreateUserPreferencePublic;
	String responseCreateUserPreferencePrivate;
	String responseDeleteUserPreference;
	String responseUpdateUserPreference;

	String entityIdMappingFileName = "";
	List<String> allEntitySection;
	List<String> entityRequestPayloadProperties;
	String userPreferenceCfgFilePath;
	String userPreferenceCfgFileName;
	String dateFormat;
	String entitySectionSplitter = ";";

	FilterUtils filterUtils;
	Boolean testForAllEntities = false;

	String username_for_different_user;
	String password_for_different_user;

	List<String >gridViewEntities;
	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getUserPreferenceConfigData();

		testCasesMap = getTestCasesMapping();

		username_for_different_user = ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath,userPreferenceCfgFileName,"usernamefordifferentuser");
		password_for_different_user = ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath,userPreferenceCfgFileName,"passwordfordifferentuser");
	}


	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

	}

	public void getUserPreferenceConfigData() throws ParseException, IOException, ConfigurationException {
		logger.info("Getting Test Data");
		userPreferenceData = new UserPreferenceData();
		filterUtils = new FilterUtils();
		listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
		listRendererFilterData = new ListRendererFilterData();

		reportRendererConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFilePath");
		reportRendererConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFileName");


		entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		userPreferenceCfgFilePath = ConfigureConstantFields.getConstantFieldsProperty("userPreferenceCfgFilePath");
		userPreferenceCfgFileName = ConfigureConstantFields.getConstantFieldsProperty("userPreferenceCfgFileName");
		baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
		dateFormat = ConfigureConstantFields.getConstantFieldsProperty("DateFormatForReports");
		gridViewEntities = Arrays.asList(ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath,userPreferenceCfgFileName,"gridviewentities").split(","));

	}

	// helper Method for This Class Starts Here
	private String getFilterJsonforDate(String entityName, Integer filterId, String filterUiType, String filterQueryName) {

		String payLoad = null;
		Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		String date_from = "";
		String date_to = "";
		String startDate = "";
		String endDate = "";
		try {
			String duedate_fromEntitySection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "date_from");
			if (duedate_fromEntitySection == null || duedate_fromEntitySection.equalsIgnoreCase("")) {
				date_from = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "date_from");
			} else {
				date_from = duedate_fromEntitySection;
			}

			String duedate_toEntitySection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "date_to");
			if (duedate_fromEntitySection == null || duedate_fromEntitySection.equalsIgnoreCase("")) {
				date_to = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "date_to");
			} else {
				date_to = duedate_toEntitySection;
			}
		} catch (Exception e) {
			logger.error("Got Error while fetching Date Values from config files for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Exception is [ {} ]", entityName, filterUiType, e.getMessage());
		}


		String actualDate = DateUtils.getDateFromEpoch(System.currentTimeMillis(), dateFormat);

		try {
			startDate = DateUtils.getDateOfXDaysFromYDate(actualDate, Integer.parseInt(date_from), dateFormat);
			endDate = DateUtils.getDateOfXDaysFromYDate(actualDate, Integer.parseInt(date_to), dateFormat);
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}

		FilterData.DataClass dataClassObj = new FilterData.DataClass();
		dataClassObj.setDataName(startDate);
		dataClassObj.setDataValue(endDate);


		payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
		logger.debug(" The  payload is  :  {}  ", payLoad);
		return payLoad;
	}

	private String getFilterJsonforSlider(String entityName, Integer filterId, String filterUiType, String filterQueryName) {

		String payLoad = null;
		Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		String slider_min = "";
		String slider_max = "";
		try {
			String slider_min_fromEntitySection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "slider_min");
			if (slider_min_fromEntitySection == null || slider_min_fromEntitySection.equalsIgnoreCase("")) {
				slider_min = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "slider_min");
			} else {
				slider_min = slider_min_fromEntitySection;
			}

			String slider_max_toEntitySection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "slider_max");
			if (slider_max_toEntitySection == null || slider_max_toEntitySection.equalsIgnoreCase("")) {
				slider_max = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "slider_max");
			} else {
				slider_max = slider_max_toEntitySection;
			}
		} catch (Exception e) {
			logger.error("Got Error while fetching Slider Values from config files for Entity [ {} ] , filterName [ {} ] , Exception is [ {} ]", entityName, filterUiType, e.getMessage());
		}


		FilterData.DataClass dataClassObj = new FilterData.DataClass();
		dataClassObj.setDataName(slider_min);
		dataClassObj.setDataValue(slider_max);


		payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
		logger.debug(" The  payload is  :  {}  ", payLoad);
		return payLoad;
	}

	private String getFilterJsonforMultiSelect(String entityName, Integer filterId, String filterUiType, String filterQueryName, FilterData filterDataClass) {
		String payLoad;
		Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		if (filterDataClass.getDataClassList().size() > 0) {
			for (FilterData.DataClass dataClassObj : filterDataClass.getDataClassList()) {
				payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
				logger.debug("The Payload is : {}", payLoad);
				return payLoad;
			}
		} else {
			logger.warn("The filterData List Size is 0 for Entity [ {} ] , filterName [ {} ]", entityName, filterUiType);
		}

		return null;
	}

	// this function is for Creating Filter Json which is the part of Create View API Call
	private String getPayloadForCreatingView(String entityName, Integer entityUrlId, Integer entityTypeId) throws Exception {


		// listRenderer Default User List Meta Data
		listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(entityUrlId);
		String defaultMetadata = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();
		logger.debug("The default Metadata API response is  : {}", defaultMetadata);
		Map<Integer, Map<String, String>> filters = filterUtils.getFilters(defaultMetadata);
		for (Map.Entry<Integer, Map<String, String>> filter : filters.entrySet()) {
			for (Map.Entry<String, String> detail : filter.getValue().entrySet()) {
				logger.debug("Key [ {} ] , Value [ {} ]", detail.getKey(), detail.getValue());
			}
		}
		logger.info("++++++++++++++++++++++listRendererDefaultUserListMetaData++++++++++++++++++++++++++++++++++");

		// listRenderer Filter Data
		listRendererFilterData.hitListRendererFilterData(entityUrlId);
		String responseFilterData = listRendererFilterData.getListRendererFilterDataJsonStr();
		logger.debug("The Filter Data API Response is [ {} ]", responseFilterData);
		List<FilterData> filteredDataList = filterUtils.getFiltersData(responseFilterData, entityUrlId.toString(), entityTypeId.toString());

		logger.info("++++++++++++++++++++++++listRendererFilterData++++++++++++++++++++++++++++++++");

		String payloadFilterJson = null; // inner filter json
		for (FilterData filterDataClass : filteredDataList) {
			if (filters.containsKey(filterDataClass.getFilterId())) {
				String filterUiType = filters.get(filterDataClass.getFilterId()).get("uiType");
				Integer filterId = filterDataClass.getFilterId();
				String filterQueryName = filters.get(filterDataClass.getFilterId()).get("queryName");
				logger.debug("The Filter UI Type is [ {} ]", filterUiType);
				logger.debug("The Filter filterId is [ {} ]", filterId);
				logger.debug("The filterQueryName is [ {} ]", filterQueryName);

				if (filterUiType.equalsIgnoreCase("STATUS") || filterUiType.equalsIgnoreCase("MULTISELECT")) {
					payloadFilterJson = getFilterJsonforMultiSelect(entityName, filterId, filterUiType, filterQueryName, filterDataClass);
				} else if (filterUiType.equalsIgnoreCase("DATE")) {
					payloadFilterJson = getFilterJsonforDate(entityName, filterId, filterUiType, filterQueryName);
				} else if (filterUiType.equalsIgnoreCase("SLIDER")) {
					payloadFilterJson = getFilterJsonforSlider(entityName, filterId, filterUiType, filterQueryName);
				}
				if (payloadFilterJson != null && !payloadFilterJson.isEmpty()) // if we get Payload then We Will Use this to create User Preference
					break;
			} else {
				logger.error("No Filter Found in Metadata API Response for [ {} ] and , ID : [ {} ]", filterDataClass.getFilterName(), filterDataClass.getFilterId());
			}

		}

		return payloadFilterJson;

	}

	// helper Method for This Class Ends Here


	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
	@DataProvider(name = "TestUserPreferenceData", parallel = false)
	public Object[][] getTestUserDataPreferenceData() throws ConfigurationException {

		int i = 0;

		HashMap<String, String> hashMapforPayloadProperties = new HashMap<String, String>();
		HashMap<String, String> hashMapforEntityProperties = new HashMap<String, String>();
		allEntitySection = ParseConfigFile.getAllSectionNames(userPreferenceCfgFilePath, userPreferenceCfgFileName);
		entityRequestPayloadProperties = ParseConfigFile.getAllPropertiesOfSection(userPreferenceCfgFilePath, userPreferenceCfgFileName, "request_payload");

		entitySectionSplitter = ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath, userPreferenceCfgFileName, "entitysectionsplitter");


		// for getting all section
		if (!ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath, userPreferenceCfgFileName,
				"testforallentities").trim().equalsIgnoreCase(""))
			testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath, userPreferenceCfgFileName, "testforallentities"));


		if (!testForAllEntities) {
			allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath, userPreferenceCfgFileName, "entitytotest").split(entitySectionSplitter));
		} else {
			allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath, userPreferenceCfgFileName, "allentitytotest").split(entitySectionSplitter));
		}

		Object[][] groupArray = new Object[allEntitySection.size()][];


		// getting all section Ends Here
		for (String entitySection : allEntitySection) {
			groupArray[i] = new Object[5];
			if (entitySection.equalsIgnoreCase("request_payload")) {
				continue;
			}
			List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(userPreferenceCfgFilePath, userPreferenceCfgFileName, entitySection);
			Integer entitySectionUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));
			Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));

			for (String payloadProperties : entityRequestPayloadProperties) {
				hashMapforPayloadProperties.put(payloadProperties, ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath, userPreferenceCfgFileName, "request_payload", payloadProperties));
			}

			for (String entitySpecificProperty : allProperties) {
				hashMapforEntityProperties.put(entitySpecificProperty, ParseConfigFile.getValueFromConfigFile(userPreferenceCfgFilePath, userPreferenceCfgFileName, entitySection, entitySpecificProperty));
			}

			groupArray[i][0] = entitySection; // EntityName
			groupArray[i][1] = entitySectionUrlId;	 // EntityUrlId
			groupArray[i][2] = entitySectionTypeId; // EntityTypeId
			groupArray[i][3] = hashMapforEntityProperties; // All the Properties specific to Entity to Create Payload
			groupArray[i][4] = hashMapforPayloadProperties; // HashMap for Payload Properties
			logger.debug("hashMapforPayloadProperties: {} , entitySection : {} , entitySectionUrlId: {} , hashMapforEntityProperties : {} ", hashMapforPayloadProperties, entitySection, entitySectionUrlId, hashMapforEntityProperties);
			i++;
		}

		return groupArray;
	}

	// List User Preference
	@Test(dataProvider = "TestUserPreferenceData")
	public void testUserPreferenceAPI(String entityName, Integer entityUrlId, Integer entityTypeId, HashMap<String, String> allProperties, HashMap<String, String> entityRequestPayloadProperties) throws Exception {
//	@Test()
//	public void testUserPreferenceAPI() throws Exception {
		getTestUserDataPreferenceData();

//		String entityName = "";
//		Integer entityUrlId;
//		Integer entityTypeId;
//		HashMap<String, String> allProperties;
//		HashMap<String, String> entityRequestPayloadProperties;
		HashMap<String,String> listViewMap = new HashMap<>();
		HashMap<String,String> gridViewMap = new HashMap<>();
//
//		Object[][] groupArray = getTestUserDataPreferenceData();
		listViewMap.put("name","List");
		listViewMap.put("id","1");

		gridViewMap.put("name","Grid");
		gridViewMap.put("id","2");

//		for (int i = 0; i < groupArray.length; i++) {
			try {

//				entityName = (String) groupArray[i][0];
//				entityUrlId = (Integer) groupArray[i][1];
//				entityTypeId = (Integer) groupArray[i][2];
//				allProperties = (HashMap<String, String>) groupArray[i][3];
//				entityRequestPayloadProperties = (HashMap<String, String>) groupArray[i][4];
				//csAssertion = new CustomAssert();
				customAssert = new CustomAssert();

				logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);

				verifyListUserPreference(entityUrlId);

				verifyCreateUserPublicViewFunctionality(entityRequestPayloadProperties, entityName, entityUrlId, entityTypeId, allProperties,listViewMap);

				verifyCreateUserPrivateViewFunctionality(entityRequestPayloadProperties, entityName, entityUrlId, entityTypeId, allProperties,listViewMap);

				if(gridViewEntities.contains(entityName)){
					//Checking Grid view creation
					verifyCreateUserPublicViewFunctionality(entityRequestPayloadProperties, entityName, entityUrlId, entityTypeId, allProperties,gridViewMap);

					verifyCreateUserPrivateViewFunctionality(entityRequestPayloadProperties, entityName, entityUrlId, entityTypeId, allProperties,gridViewMap);
				}
				verifyPinUserViewFunctionality(entityUrlId);

				verifyUnPinUserViewFunctionality(entityUrlId);

				verifyUpdateUserViewFunctionality(entityRequestPayloadProperties, entityName, entityUrlId, entityTypeId, allProperties);

				verifySaveDefaultUserPreferenceFunctionality(entityUrlId);

				verifyDeleteUserViewFunctionality(entityUrlId);

				validateDeleteUserViewAPIFunctionalityForDefaultUserView();

				logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);

			} catch (Exception e) {
				logger.error("Got exception for EntityName: [{}] --> [{}]", entityName, e.getStackTrace());
				customAssert.assertTrue(false,"Got exception for EntityName: " + entityName + e.getStackTrace());
			}
//		}
		addTestResult(getTestCaseIdForMethodName("testUserPreferenceAPI"), customAssert);
		customAssert.assertAll();
		//csAssertion.assertAll();
	}


	// List User Preference
	public void verifyListUserPreference(Integer entityUrlId) throws Exception {

		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		int numofViews;
		String response;
		HttpResponse responseListUserPreference = userPreferenceData.hitUserPreferenceListAPI(entityUrlId);
		customAssert.assertTrue(userPreferenceData.getStatusCodeFrom(responseListUserPreference).contains("200"), "Error :List User Preference API Status Code is Incorrect");


		response = userPreferenceData.getResponselistUserPreference();
		logger.debug("Response Status Line is :{}", responseListUserPreference.getStatusLine().toString());
		logger.debug("Response Locale Line is :{}", responseListUserPreference.getLocale().toString());
		logger.debug("Response Payload is :{}", response);

		numofViews = response.split("\"id\"").length - 1;
		logger.debug("Number of Views is :{}", numofViews);
		existingViewIds = new String[numofViews];

		for (int i = 0; i < numofViews; i++) {
			JSONArray viewsArray = new JSONArray(response);
			existingViewIds[i] = viewsArray.getJSONObject(i).get("id").toString();
		}
		logger.info("Existing Views Ids is --> {} ", existingViewIds);

		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

	}

	// Create User Preference
	public void verifyCreateUserPublicViewFunctionality(HashMap<String, String> entityRequestPayloadProperties, String entityName, Integer entityUrlId, Integer entityTypeId, HashMap<String, String> allProperties,HashMap<String,String> listViewTypeMap) throws Exception {
		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		DateFormat dateFormate = new SimpleDateFormat("yyyy:MM:dd_HH:mm:ss");
		Date date = new Date();
		logger.debug("Date : {} ", dateFormate.format(date));
		createdPublicViewName = entityRequestPayloadProperties.get("viewnameprefix".toLowerCase()) + "_" + dateFormate.format(date) + "_Public";


		String filterJson;
		String payloadFilterJson = getPayloadForCreatingView(entityName, entityUrlId, entityTypeId);

		logger.debug("payloadFilterJson is : {}", payloadFilterJson);
		if (payloadFilterJson == null && payloadFilterJson.isEmpty())
			//filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + ",\"orderDirection\":" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + ",\"filterJson\":{}}";
			filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + "\"" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + "\",\"orderDirection\":\"" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + "\",\"filterJson\":{}}}";
		else {
			JSONObject jsonObj = new JSONObject(payloadFilterJson);
			if (jsonObj.has("filterMap") && jsonObj.getJSONObject("filterMap").has("filterJson")) {
				//filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + ",\"orderDirection\":" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + ",\"filterJson\":" + jsonObj.getJSONObject("filterMap").get("filterJson") + "}";
				filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + "\"" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + "\"" + ",\"orderDirection\":" + "\"" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + "\"" + ",\"filterJson\":" + jsonObj.getJSONObject("filterMap").get("filterJson") + "}}";
			} else
//				filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + ",\"orderDirection\":" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + ",\"filterJson\":{}}";
				filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + "\"" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + "\",\"orderDirection\":\"" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + "\",\"filterJson\":{}}}";

		}

		logger.info("filterJson is [{}]", filterJson);

		JSONObject payloadJson = new JSONObject();
		JSONObject listViewTypeJson = new JSONObject();
		listViewTypeJson.put("name",listViewTypeMap.get("name"));
		listViewTypeJson.put("id",Integer.parseInt(listViewTypeMap.get("id")));

		payloadJson.put("maxNumberOfColumns", allProperties.get("maxNumberOfColumns".toLowerCase()));
		payloadJson.put("listId", entityUrlId);
		payloadJson.put("name", createdPublicViewName);
		payloadJson.put("publicVisibility", true);
		payloadJson.put("maxNumberOfColumns", 20);
		payloadJson.put("filterJson", filterJson);
		payloadJson.put("listViewType", listViewTypeJson);
		logger.info("payloadJson: {}", payloadJson);

		HttpResponse response = userPreferenceData.hitUserPreferenceCreateAPI(entityUrlId, String.valueOf(payloadJson));
		customAssert.assertTrue(response.getStatusLine().toString().contains("200"), "Error: List User Preference API Status Code is Incorrect");
		responseCreateUserPreferencePublic = userPreferenceData.getResponseCreateUserPreference();
		logger.debug("Response Payload for CreateUserPublicaView is  :{}", responseCreateUserPreferencePublic);
		customAssert.assertTrue(userPreferenceData.verifyWhetherViewExistOrNot(responseCreateUserPreferencePublic, createdPublicViewName), "Error: List User Preference API is not showing the createdPublicView Name");

		Check check = new Check();
		try {
			//Checking the private view for different user

			check.hitCheck(username_for_different_user, password_for_different_user);

			UserListMetaData userListMetaData = new UserListMetaData();
			userListMetaData.hitUserPreferenceList(entityUrlId);
			String userListMetaDataResponse =  userListMetaData.getListUserPreferenceAPIResponse();

			if(userPreferenceData.verifyWhetherViewExistOrNot(userListMetaDataResponse, createdPublicViewName)){
				logger.info("Public view created by different user is visible to other user ");
			}else {
				logger.error("Public view created by different user is not visible to other user ");
				csAssert.assertTrue(false,"Public view created by different user is not visible to other user ");
			}
		}catch (Exception e){
			logger.error("Exception in retrieving listRendererDefaultUserListMetaData for user " + username_for_different_user);
		}finally {
			check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
		}
		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

	}

	// Create Private Preference
	public void verifyCreateUserPrivateViewFunctionality(HashMap<String, String> entityRequestPayloadProperties, String entityName, Integer entityUrlId, Integer entityTypeId, HashMap<String, String> allProperties,HashMap<String,String> listViewTypeMap) throws Exception {

		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		DateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd_HH:mm:ss");
		Date date = new Date();
		logger.debug("Date : {} ", dateFormat.format(date));
		createdPrivateViewName = entityRequestPayloadProperties.get("viewnameprefix".toLowerCase()) + "_" + dateFormat.format(date) + "_Private";

		String filterJson;
		String payloadFilterJson = getPayloadForCreatingView(entityName, entityUrlId, entityTypeId);

		logger.debug("payloadFilterJson is : {}", payloadFilterJson);
		if (payloadFilterJson == null && payloadFilterJson.isEmpty())
			//filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + ",\"orderDirection\":" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + ",\"filterJson\":{}}";
			filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + "\"" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + "\",\"orderDirection\":\"" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + "\",\"filterJson\":{}}}";
		else {
			JSONObject jsonObj = new JSONObject(payloadFilterJson);
			if (jsonObj.has("filterMap") && jsonObj.getJSONObject("filterMap").has("filterJson")) {
				filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + "\"" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + "\"" + ",\"orderDirection\":" + "\"" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + "\"" + ",\"filterJson\":" + jsonObj.getJSONObject("filterMap").get("filterJson") + "}}";
			} else
				//filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + ",\"orderDirection\":" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + ",\"filterJson\":{}}";
				filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + "\"" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + "\",\"orderDirection\":\"" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + "\",\"filterJson\":{}}}";

		}

		logger.info("filterJson is [{}]", filterJson);

		JSONObject listViewTypeJson = new JSONObject();
		listViewTypeJson.put("name",listViewTypeMap.get("name"));
		listViewTypeJson.put("id",listViewTypeMap.get("id"));

		JSONObject payloadJson = new JSONObject();
		filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + ",\"orderDirection\":" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + ",\"filterJson\":{}}";
		payloadJson.put("maxNumberOfColumns", allProperties.get("maxNumberOfColumns".toLowerCase()));
		payloadJson.put("listId", entityUrlId);
		payloadJson.put("name", createdPrivateViewName);
		payloadJson.put("publicVisibility", false);
		payloadJson.put("filterJson", filterJson);
		payloadJson.put("listViewType", listViewTypeJson);
		payloadJson.put("maxNumberOfColumns", 20);
		logger.info("payloadJson: {}", payloadJson);


		HttpResponse response = userPreferenceData.hitUserPreferenceCreateAPI(entityUrlId, String.valueOf(payloadJson));
		customAssert.assertTrue(response.getStatusLine().toString().contains("200"), "Error: List User Preference API Status Code is Incorrect");
		responseCreateUserPreferencePrivate = userPreferenceData.getResponseCreateUserPreference();
		logger.debug("Response Payload for CreateUserPrivateView is :{}", responseCreateUserPreferencePrivate);
		customAssert.assertTrue(userPreferenceData.verifyWhetherViewExistOrNot(responseCreateUserPreferencePrivate, createdPrivateViewName), "Error: List User Preference API is not showing the createdPrivateView Name");
		Check check = new Check();
		try {
			//Checking the private view for different user

			check.hitCheck(username_for_different_user, password_for_different_user);

			UserListMetaData userListMetaData = new UserListMetaData();
			userListMetaData.hitUserPreferenceList(entityUrlId);
			String userListMetaDataResponse =  userListMetaData.getListUserPreferenceAPIResponse();

			if(!userPreferenceData.verifyWhetherViewExistOrNot(userListMetaDataResponse, createdPrivateViewName)){
				logger.info("Private view doesn't have the private view created by different user");//At the time of writing this test nothing came in response
			}else {
				logger.error("User can access the private view created by different user");
				csAssert.assertTrue(false,"User can access the private view created by different user");
			}
		}catch (Exception e){
			logger.error("Exception in retrieving listRendererDefaultUserListMetaData for user " + username_for_different_user);
			csAssert.assertTrue(false,"User can access the private view created by different user");
		}finally {
			check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
		}
		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

	}

	// Pin User Preference
	public void verifyPinUserViewFunctionality(Integer entityUrlId) throws Exception {

		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		int viewId = userPreferenceData.getViewIdByViewName(responseCreateUserPreferencePublic, createdPublicViewName);
		logger.info("User Preference View Id:{} <---which is getting Pinned is ", viewId);

		HttpResponse response = userPreferenceData.hitPinUserPreferenceAPI(entityUrlId, viewId);
		csAssert.assertTrue(userPreferenceData.getStatusCodeFrom(response).contains("200"), "Error :Pin User Preference API Status Code is Incorrect");
		boolean isReponseValidJson = APIUtils.validJsonResponse(userPreferenceData.getResponsePinUserPreference(), "Pin User Preference API");
		customAssert.assertTrue(isReponseValidJson, "Pin User Preference API Response is not Valid Json");


		// functionality check
		HttpResponse responseHttp = userPreferenceData.hitUserPreferenceListAPI(entityUrlId);
		customAssert.assertTrue(userPreferenceData.checkWhetherViewIdIsPinnedOrNot(userPreferenceData.getResponselistUserPreference(), String.valueOf(viewId)),
				"Not Being able to Pin the View " + viewId);


		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());


	}

	// UnPin User Preference
	public void verifyUnPinUserViewFunctionality(Integer entityUrlId) throws Exception {

		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		int viewId = userPreferenceData.getViewIdByViewName(responseCreateUserPreferencePublic, createdPublicViewName);
		logger.info("User Preference View Id:{} <---which is getting UnPinned is ", viewId);

		HttpResponse response = userPreferenceData.hitDeletePinUserPreferenceAPI(entityUrlId, viewId);
		csAssert.assertTrue(userPreferenceData.getStatusCodeFrom(response).contains("200"), "Error :UnPin User Preference API Status Code is Incorrect");
		boolean isReponseValidJson = APIUtils.validJsonResponse(userPreferenceData.getResponseUnPinUserPreference(), "Pin User Preference API");
		customAssert.assertTrue(isReponseValidJson, "UnPin User Preference API Response is not Valid Json");

		// functionality check
		HttpResponse responseHttp = userPreferenceData.hitUserPreferenceListAPI(entityUrlId);
		customAssert.assertTrue(userPreferenceData.checkWhetherViewIdIsUnPinnedOrNot(userPreferenceData.getResponselistUserPreference(), String.valueOf(viewId)),
				"Not Being able to UnPin the View " + viewId);

		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

	}

	// Update User View
	public void verifyUpdateUserViewFunctionality(HashMap<String, String> entityRequestPayloadProperties, String entityName, Integer entityUrlId, Integer entityTypeId, HashMap<String, String> allProperties) throws Exception {

		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		JSONObject payloadJson = new JSONObject();
		String filterJson;
		filterJson = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + entityRequestPayloadProperties.get("offset".toLowerCase()) + ",\"size\":" + entityRequestPayloadProperties.get("size".toLowerCase()) + ",\"orderByColumnName\":\"" + entityRequestPayloadProperties.get("orderByColumnName".toLowerCase()) + "\",\"orderDirection\":\"" + entityRequestPayloadProperties.get("orderDirection".toLowerCase()) + "\",\"filterJson\":{}}}";
		payloadJson.put("filterJson", filterJson);
		payloadJson.put("maxNumberOfColumns", allProperties.get("maxNumberOfColumns".toLowerCase()));
		payloadJson.put("listId", entityUrlId);
		payloadJson.put("name", createdPrivateViewName);
		payloadJson.put("publicVisibility", false);

		logger.info("payloadJson: {}", payloadJson);

		HttpResponse response = userPreferenceData.hitUserPreferenceUpdateAPI(entityUrlId, String.valueOf(payloadJson));
		customAssert.assertTrue(response.getStatusLine().toString().contains("200"), "Error: Update User Preference API Status Code is Incorrect");

		responseUpdateUserPreference = userPreferenceData.getResponseUpdateUserPreference();
		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

	}

	// Save User Preference
	public void verifySaveDefaultUserPreferenceFunctionality(Integer entityUrlId) throws Exception {

		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		int viewId = userPreferenceData.getViewIdByViewName(responseCreateUserPreferencePublic, createdPublicViewName);
		logger.info("User Preference View Id:{} <---which is getting set as defaultUserPreference ", viewId);

		HttpResponse response = userPreferenceData.hitSaveDefaultUserPreferenceAPI(entityUrlId, viewId);
		customAssert.assertTrue(userPreferenceData.getStatusCodeFrom(response).contains("200"), "Error :Save Default User Preference API Status Code is Incorrect");

		HttpResponse responseHttp = userPreferenceData.hitUserPreferenceListAPI(entityUrlId);
		customAssert.assertTrue(userPreferenceData.getStatusCodeFrom(responseHttp).contains("200"), "Error :List User Preference API Status Code is Incorrect");

		String listUserPreferneceAPIResponse = userPreferenceData.getResponselistUserPreference();
		customAssert.assertTrue(userPreferenceData.checkWhetherViewIdIsDefaultUserView(listUserPreferneceAPIResponse, createdPublicViewName), "Error: Save Default User View API is Not Setting the given view as Default User View");
		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

	}

	// Delete User Preference
	public void verifyDeleteUserViewFunctionality(Integer entityUrlId) throws Exception {

		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		int viewId = userPreferenceData.getViewIdByViewName(responseCreateUserPreferencePublic, createdPublicViewName);
		logger.info("User Preference View Id:{} <---which is getting deleted ", viewId);

		HttpResponse response = userPreferenceData.hitUserPreferenceDeleteAPI(entityUrlId, viewId, true);
		customAssert.assertTrue(userPreferenceData.getStatusCodeFrom(response).contains("200"), "Error :Delete User Preference API Status Code is Incorrect");

		responseDeleteUserPreference = userPreferenceData.getResponseDeleteUserPreference();

		logger.debug("Response Payload for DeleteUserView is :{}", responseDeleteUserPreference);
		customAssert.assertTrue(!userPreferenceData.verifyWhetherViewExistOrNot(responseDeleteUserPreference, createdPublicViewName), "Error: List User Preference API is showing the delete User Preference View Even After Deleting It");

		// cleanup of Private View
		viewId = userPreferenceData.getViewIdByViewName(responseCreateUserPreferencePrivate, createdPrivateViewName);
		logger.info("User Preference View Id:{} <---which is getting deleted ", viewId);
		userPreferenceData.hitUserPreferenceDeleteAPI(entityUrlId, viewId, true);
		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

	}

	// this function is for verifying the functionality like when we delete any user view which is default user view It should set system default as default view
	public void validateDeleteUserViewAPIFunctionalityForDefaultUserView() throws Exception {

		logger.info("---------------------------------------------------------Starting : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());
		CustomAssert csAssert = new CustomAssert();
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		customAssert.assertTrue(userPreferenceData.getDefaultUserView(responseDeleteUserPreference) == -1, "Error: Delete User Prefrence API is not Setting Default User View as System Default while Deleting Custom Default User View");
		csAssert.assertAll();
		logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

	}

	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");


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
		logger.info("In After Class method");
	}

}
