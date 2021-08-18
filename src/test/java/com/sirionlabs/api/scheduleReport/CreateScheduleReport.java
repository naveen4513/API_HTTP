package com.sirionlabs.api.scheduleReport;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by shivashish on 29/8/17.
 */
public class CreateScheduleReport extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(CreateScheduleReport.class);
	String apiStatusCode = null;
	String responseCreateReportFormAPI;
	String responseCreateScheduleReportAPI;
	String payload = null;

	String scheduleReportConfigFilePath;
	String scheduleReportConfigFileName;


	String frequencyType;
	String timezoneid;
	String[] selectedUsersFromConfigFile;
	String usersParser = ",";

	String nameOfTheReport;
	int entityTypeId = -1;

	  public CreateScheduleReport() {

	  }
	// constructor which will take the response of CreateScheduleReportForm API Response for building up the payload
	public CreateScheduleReport(String responseCreateReportFormAPI) throws ConfigurationException {
		this.responseCreateReportFormAPI = responseCreateReportFormAPI;

		scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFilePath");
		scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFileName");
		// for creating Payload
		frequencyType = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "frequencytype");
		timezoneid = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "timezoneId");
		selectedUsersFromConfigFile = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "selectedusers").split(usersParser);

		payload = createPayloadForCreateScheduleReportAPI(responseCreateReportFormAPI);

	}

	public CreateScheduleReport(String responseCreateReportFormAPI , int entityTypeId,int reportId) throws ConfigurationException {
		this.responseCreateReportFormAPI = responseCreateReportFormAPI;

		scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionScheduleReportConfigFilePath");
		scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionScheduleReportConfigFileName");
		// for creating Payload
		frequencyType = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "frequencytype");
		timezoneid = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "timezoneId");
		selectedUsersFromConfigFile = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "selectedusers").split(usersParser);
		this.entityTypeId = entityTypeId;
		payload = createPayloadForCreateScheduleReportAPI(responseCreateReportFormAPI);

	}

	// constructor which will take the response of CreateScheduleReportForm API Response for building up the payload
	public CreateScheduleReport(String responseCreateReportFormAPI , int entityTypeId) throws ConfigurationException {
		this.responseCreateReportFormAPI = responseCreateReportFormAPI;

		scheduleReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFilePath");
		scheduleReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleReportConfigFileName");
		// for creating Payload
		frequencyType = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "frequencytype");
		timezoneid = ParseConfigFile.getValueFromConfigFileCaseSensitive(scheduleReportConfigFilePath, scheduleReportConfigFileName, "timezoneId");
		selectedUsersFromConfigFile = ParseConfigFile.getValueFromConfigFile(scheduleReportConfigFilePath, scheduleReportConfigFileName, "selectedusers").split(usersParser);

		this.entityTypeId = entityTypeId;
		payload = createPayloadForCreateScheduleReportAPI(responseCreateReportFormAPI);



	}


	// this function will return the Json Object selectedUsers which will contains all the details of Users to Whom Reports is being Scheduled
	public JSONArray getSelectedUsersPayload(JSONArray allUsers) {

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
		// worst case if any usersByConfigFile is not found in allUsers Arrays then putting first Users for the shake of Scheduling Report
		if (selectedUsers.length() == 0) {
			logger.info("Any User Mentioned in ScheduleReport.cfg is not matching with All Users Json Array Object of ScheduleReport Form API Response");
			selectedUsers.put(allUsers.get(0));

		}

		return selectedUsers;
	}

	// this helper function will return the value of frequency param of Payload
	public String getFrequencyPayloadString() {

		String frequencyPayloadString = null;


		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");

		Calendar c = Calendar.getInstance();
		c.setTime(new Date()); // Now use today date.
		String todayDate = String.valueOf(sdf.format(c.getTime()));

		c.setTime(new Date());
		c.add(Calendar.DATE, 1); // Adding 7 days
		String dateAfterOneDay = String.valueOf(sdf.format(c.getTime()));

		c.setTime(new Date());
		c.add(Calendar.DATE, 7); // Adding 7 days
		String dateAfter7Days = String.valueOf(sdf.format(c.getTime()));


		c.setTime(new Date());
		c.add(Calendar.DATE, 31); // Adding 31 days
		String dateAfter1Month = String.valueOf(sdf.format(c.getTime()));


		c.setTime(new Date());
		c.add(Calendar.DATE, 365); // Adding 365 days
		String dateAfter1Year = String.valueOf(sdf.format(c.getTime()));


		if (frequencyType.contentEquals("REPEATONCE"))
			frequencyPayloadString = "{\"FREQ\":\"REPEATONCE\",\"RRULE\":\"FREQ=REPEATONCE\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"REPEATON\":\"\"}";


		if (frequencyType.contentEquals("DAILY"))
			frequencyPayloadString = "{\"FREQ\":\"DAILY\",\"RRULE\":\"FREQ=DAILY;INTERVAL=1\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"UNTIL\":\"" + dateAfter7Days + "\",\"REPEATON\":\"\"}";


		if (frequencyType.contentEquals("WEEKLY"))
			frequencyPayloadString = "{\"FREQ\":\"WEEKLY\",\"RRULE\":\"FREQ=WEEKLY;INTERVAL=1;BYDAY=MO\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"UNTIL\":\"" + dateAfter1Month + "\",\"REPEATON\":\"\"}";


		if (frequencyType.contentEquals("MONTHLY"))
			frequencyPayloadString = "{\"FREQ\":\"MONTHLY\",\"date\":\"1\",\"RRULE\":\"FREQ=MONTHLY;INTERVAL=1\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"UNTIL\":\"" + dateAfter1Year + "\",\"REPEATON\":\"DATE\"}";


		// worst case in frequencyType mentioned in config file doesn't match with any of the types specified above
		// making it REPEATEONCE for the sake of scheduling report
		if (frequencyPayloadString == null)
			frequencyPayloadString = "{\"FREQ\":\"REPEATONCE\",\"RRULE\":\"FREQ=REPEATONCE\",\"DTSTART\":\"" + todayDate + "\",\"BYHOUR\":\"23\",\"REPEATON\":\"\"}";


		return frequencyPayloadString;
	}


	public String createPayloadForCreateScheduleReportAPI(String createReportFormAPIReponse) {
		// for creating payload

		// for frequency (Done)
		String frequency = getFrequencyPayloadString();
		// frequency done

		// for timezone (Done)
		HashMap<String, String> timezoneIdMap = new HashMap<String, String>();
		timezoneIdMap.put("id", timezoneid);
		// timezone done

		// for name and comment (Done)
		JSONObject createReportFormAPIJsonResponse = new JSONObject(createReportFormAPIReponse);
		nameOfTheReport = createReportFormAPIJsonResponse.getString("subject");
		String name = "automation_" + nameOfTheReport;
		String comment = "automation_" + nameOfTheReport; // never change this text will be used in testTS80699 of TestScheduleReport
		// name and comment done

		String filterJson = "{\n" +
				"  \"filterMap\": {\n" +
				"    \"entityTypeId\": "+entityTypeId+",\n" +
				"    \"offset\": 0,\n" +
				"    \"size\": 20,\n" +
				"    \"orderByColumnName\": \"id\",\n" +
				"    \"orderDirection\": \"desc nulls last\",\n" +
				"    \"filterJson\": {}\n" +
				"  }\n" +
				"}";

		// for Getting the selected Users (Done)
		JSONArray allUsers = createReportFormAPIJsonResponse.getJSONArray("allUsers");
		JSONArray selectedUsers = getSelectedUsersPayload(allUsers);
		//Getting the selected Users done



		// removing the keys [redundant payload](Done)
//		if (createReportFormAPIJsonResponse.has("allUsers")) {
//			createReportFormAPIJsonResponse.remove("allUsers");
//		}
		// removing the keys done


		// modifying the keys -----> Building Actual Payload (Done)
		createReportFormAPIJsonResponse.put("name", name);
		createReportFormAPIJsonResponse.put("comment", comment);
		createReportFormAPIJsonResponse.put("frequencyType", frequencyType);
		createReportFormAPIJsonResponse.put("frequency", frequency);
		createReportFormAPIJsonResponse.getJSONObject("scheduleReport").put("timeZone", timezoneIdMap);
		createReportFormAPIJsonResponse.getJSONObject("scheduleReport").put("filterJson", filterJson);
		createReportFormAPIJsonResponse.put("selectedUsers", selectedUsers);


		if (createReportFormAPIJsonResponse.has("timeZones")) {
			createReportFormAPIJsonResponse.remove("timeZones");
		}
		createReportFormAPIJsonResponse.remove("calendarTypeId");
		//Building Actual Payload done

		//logger.info("Payload is : {}", createReportFormAPIJsonResponse);

		return createReportFormAPIJsonResponse.toString();

	}

	public String getResponseCreateScheduleReportAPI() {
		return responseCreateScheduleReportAPI;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	// this function will hit the Create Report For API for Given Report Id
	public HttpResponse hitCreateScheduleReportAPI() throws Exception {

		HttpResponse response;
		String queryString = "/scheduleReport/create";

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

		response = super.postRequest(postRequest, payload);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("create Schedule Report API: response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		responseCreateScheduleReportAPI = EntityUtils.toString(response.getEntity());

		return response;

	}

	public HttpResponse hitCreateScheduleReportAPI(String payloadCreateScheduleReportAPI) throws Exception {

		HttpResponse response;
		String queryString = "/scheduleReport/create";

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

		response = super.postRequest(postRequest, payloadCreateScheduleReportAPI);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("create Schedule Report API: response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		responseCreateScheduleReportAPI = EntityUtils.toString(response.getEntity());

		return response;

	}

	public boolean validateScheduleByMeReportAPIReponse(String responseScheduleByMeReportAPI) {

		JSONObject jsonObject = new JSONObject(responseScheduleByMeReportAPI);
		//JSONUtility jsonUtility = new JSONUtility(jsonObject);
		JSONArray jsonArray = jsonObject.getJSONArray("data");
		JSONObject objhavingmatchingkey = null;
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObjectInternal = jsonArray.getJSONObject(i);
			// with check for schedule name and status which should be active
			if (jsonObjectInternal.toString().contains("\"value\":\"" + "automation_" + nameOfTheReport + "\"")
					&& jsonObjectInternal.toString().contains("\"value\":\"ACTIVE\""))

			{
				objhavingmatchingkey = jsonObjectInternal;
				break;
			}
		}


		if (objhavingmatchingkey == null) {
			logger.error("ScheduleByMeReportAPIReponse don't have any Scheduled Report With Schedule Name " + "automation_" + nameOfTheReport + "and status = active");
			return false;
		} else
			return true;

	}


	// this Function will return the Db id of Schedule Report
	public int getIdOfCreatedReport(String responseScheduleByMeReportAPI) {

		int id = -1;
		JSONObject jsonObject = new JSONObject(responseScheduleByMeReportAPI);
		//JSONUtility jsonUtility = new JSONUtility(jsonObject);
		JSONArray jsonArray = jsonObject.getJSONArray("data");
		JSONObject objhavingmatchingkey = null;
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObjectInternal = jsonArray.getJSONObject(i);
			// with check for schedule name and status which should be active
			if (jsonObjectInternal.toString().contains("\"value\":\"" + "automation_" + nameOfTheReport + "\"")
					&& jsonObjectInternal.toString().contains("\"value\":\"ACTIVE\""))

			{
				objhavingmatchingkey = jsonObjectInternal;
				Set<String> keys = objhavingmatchingkey.keySet();

				for (String key : keys) {
					if (objhavingmatchingkey.getJSONObject(key).get("columnName").toString().contentEquals("actions")) {
						id = Integer.parseInt(objhavingmatchingkey.getJSONObject(key).get("value").toString().split(":;")[0]);
						return id;
					}

				}


			}
		}


		if (objhavingmatchingkey == null) {
			logger.error("ScheduleByMeReportAPIReponse don't have any Scheduled Report With Schedule Name " + "automation_" + nameOfTheReport + "and status = active");
			return -1;
		} else
			return id;

	}


}
