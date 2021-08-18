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

public class ReportUserPreferenceDelete extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ReportUserPreferenceDelete.class);
	String responseReportUserPreferenceDelete;



	public String getResponseReportUserPreferenceDeleteAPI() {
		return responseReportUserPreferenceDelete;
	}


	// this function will hit the Save Delete User Preference API for the viewId Passed
	public HttpResponse hitReportUserPreferenceDeleteAPI(int reportId, int viewId) throws Exception {

		HttpResponse response;
		try {

			String queryString = "/reportRenderer/list/" + reportId + "/deleteUserPreference" + "?preferenceId=" + viewId;

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type","application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "*/*");

			response = super.postRequest(postRequest, "{}");
			logger.debug("Response is : {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Report User Preference Delete API: response header {}", headers[i].toString());
			}
			responseReportUserPreferenceDelete = EntityUtils.toString(response.getEntity());

			return response;
		} catch (Exception e) {
			logger.error("Error While Hitting ReportUserPreferenceDelete [{}]", e.getLocalizedMessage());

		}
		return null;

	}





}
