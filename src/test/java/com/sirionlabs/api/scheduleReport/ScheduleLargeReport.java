package com.sirionlabs.api.scheduleReport;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 10/4/18.
 */
public class ScheduleLargeReport extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ScheduleLargeReport.class);
	String apiStatusCode;
	String responseScheduleLargeReportAPI;


	// for payload building
	String scheduleLargeReportConfigFilePath;
	String scheduleLargeReportConfigFileName;
	String offset;
	String size;
	String orderbycolumnname;
	String orderdirection;
	String payloadString;
	String reportId;
	String entityTypeId;


	// constructor which will take report Id and entityTypeID for building the payload
	public ScheduleLargeReport(String reportId, String entityTypeId) throws ConfigurationException {

		this.reportId = reportId;
		this.entityTypeId = entityTypeId;
		scheduleLargeReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleLargeReportConfigFilePath");
		scheduleLargeReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleLargeReportConfigFileName");
		// for creating Payload
		offset = ParseConfigFile.getValueFromConfigFile(scheduleLargeReportConfigFilePath, scheduleLargeReportConfigFileName, "offset");
		size = ParseConfigFile.getValueFromConfigFile(scheduleLargeReportConfigFilePath, scheduleLargeReportConfigFileName, "size");
		orderbycolumnname = ParseConfigFile.getValueFromConfigFile(scheduleLargeReportConfigFilePath, scheduleLargeReportConfigFileName, "orderbycolumnname");
		orderdirection = ParseConfigFile.getValueFromConfigFile(scheduleLargeReportConfigFilePath, scheduleLargeReportConfigFileName, "orderdirection");

		JSONObject payload = new JSONObject();
		JSONObject filterMap = new JSONObject();
		//@todo filterJson it's hardcoded as of now
		JSONObject filterJson = new JSONObject("{\n" +
				"      \"17\": {\n" +
				"        \"filterId\": \"17\",\n" +
				"        \"filterName\": \"functions\",\n" +
				"        \"entityFieldId\": null,\n" +
				"        \"entityFieldHtmlType\": null,\n" +
				"        \"multiselectValues\": {\n" +
				"          \"SELECTEDDATA\": [\n" +
				"            {\n" +
				"              \"id\": \"1003\",\n" +
				"              \"name\": \"Human Resources\"\n" +
				"            }\n" +
				"          ]\n" +
				"        }\n" +
				"      }\n" +
				"    }");


		filterMap.put("entityTypeId", Integer.parseInt(entityTypeId));
		filterMap.put("offset", Integer.parseInt(offset));
		filterMap.put("size", Integer.parseInt(size));
		filterMap.put("orderbycolumnname", orderbycolumnname);
		filterMap.put("orderdirection", orderdirection);
		filterMap.put("filterJson", filterJson);
		payload.put("filterMap", filterMap);


		payloadString = payload.toString();
		logger.debug("Payload is : {}", payloadString);
	}

	public String getResponseScheduleLargeReportAPI() {
		return responseScheduleLargeReportAPI;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	// this function will hit the Schedule Large Report API with payload created based on the report ID provided in constructor
	public HttpResponse hitCreateScheduleLargeReportAPI() throws Exception {

		HttpResponse response;
		String queryString = "/scheduleLargeReport/create";
		queryString += ("?id=" + reportId);

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

		response = super.postRequest(postRequest, payloadString);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug(" Schedule Large Report API: response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

		responseScheduleLargeReportAPI = EntityUtils.toString(response.getEntity());

		return response;

	}


}
