package com.sirionlabs.api.scheduleReport;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * created by shivashish on 29/8/17.
 */
public class UpdateScheduleReport extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(UpdateScheduleReport.class);
	String apiStatusCode = null;
	String responseupdateReportFormAPI;
	String responseUpdateScheduleReportAPI;
	String payload = null;
	String nameOfTheReport;

	// constructor which will take the response of UpdateScheduleReportForm API Response for building up the payload
	public UpdateScheduleReport(String responseupdateReportFormAPI) throws ConfigurationException {
		this.responseupdateReportFormAPI = responseupdateReportFormAPI;
		payload = createPayloadForUpdateScheduleReportAPI(responseupdateReportFormAPI);

	}


	// here we are updating the comment of the Schedule report - Only
	public String createPayloadForUpdateScheduleReportAPI(String updateReportFormAPIReponse) {

		JSONObject updateReportFormAPIJsonResponse = new JSONObject(updateReportFormAPIReponse);
		nameOfTheReport = updateReportFormAPIJsonResponse.getString("subject");
		String comment = "automation_" + nameOfTheReport + "_update"; // this is for updating the Schedule Report

		updateReportFormAPIJsonResponse.remove("comment");
		updateReportFormAPIJsonResponse.put("comment", comment);

		logger.info("Payload is : {}", updateReportFormAPIJsonResponse);
		return updateReportFormAPIJsonResponse.toString();

	}

	public String getResponseUpdateScheduleReportAPI() {
		return responseUpdateScheduleReportAPI;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	// this function will hit the update Report For API for Given Report Id
	public HttpResponse hitUpdateScheduleReportAPI() throws Exception {

		HttpResponse response;
		String queryString = "/scheduleReport/update";

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

		response = super.postRequest(postRequest, payload);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("update Schedule Report API: response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		responseUpdateScheduleReportAPI = EntityUtils.toString(response.getEntity());

		return response;

	}

}
