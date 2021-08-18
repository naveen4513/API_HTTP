package com.sirionlabs.api.userPreference;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

public class ReportUserPreferenceSave extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ReportUserPreferenceSave.class);
	String responseReportUserPreferenceSave;
	int reportId;


	public ReportUserPreferenceSave(int reportId) {
		this.reportId = reportId;
	}


	public String getResponseReportUserPreferenceSaveAPI() {
		return responseReportUserPreferenceSave;
	}


	// this function will hit the Save User Preference List API for the reportId set by Constructor with given payload
	public HttpResponse hitReportUserPreferenceSaveAPI(String payload) throws Exception {

		HttpResponse response;
		try {

			String queryString = "/reportRenderer/list/" + reportId + "/userpreferences/save";

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type","application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "*/*");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response is : {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Report User Preference Save API: response header {}", headers[i].toString());
			}
			responseReportUserPreferenceSave = EntityUtils.toString(response.getEntity());

			return response;
		} catch (Exception e) {
			logger.error("Error While Hitting ReportUserPreferenceSave [{}]", e.getLocalizedMessage());

		}
		return null;

	}
	
}
