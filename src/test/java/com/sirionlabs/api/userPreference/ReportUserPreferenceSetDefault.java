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

public class ReportUserPreferenceSetDefault extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ReportUserPreferenceSetDefault.class);
	String responseReportUserPreferenceSetDefault;



	// as of now this method is of no use since this api doesn't return anything SIR-149694
	public String getResponseReportUserPreferenceSetDefaultAPI() {
		return responseReportUserPreferenceSetDefault;
	}


	// this function will hit the set default view API for given reportId and ViewId
	public HttpResponse hitReportUserPreferenceSetDefaultAPI(int reportId, int viewId) throws Exception {

		HttpResponse response;
		try {

			String queryString = "/reportRenderer/list/" + reportId + "/userpreferences/saveDefaultUserPreference" + "?preferenceId=" + viewId + "&isDefault=true";

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "*/*");

			response = super.postRequest(postRequest, "{}");
			logger.debug("Response is : {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Report User Preference Set Default API: response header {}", headers[i].toString());
			}
			responseReportUserPreferenceSetDefault = EntityUtils.toString(response.getEntity());

			return response;
		} catch (Exception e) {
			logger.error("Error While Hitting ReportUserPreferenceSetDefault [{}]", e.getLocalizedMessage());

		}
		return null;

	}





}
