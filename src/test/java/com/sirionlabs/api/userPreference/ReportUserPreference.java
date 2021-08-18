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

public class ReportUserPreference extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ReportUserPreference.class);
	String responseReportUserPreference;
	int reportId;


	public ReportUserPreference(int reportId) {
		this.reportId = reportId;
	}


	public String getResponseReportUserPreference() {
		return responseReportUserPreference;
	}


	// this function will hit the User Preference List API using reportId set by Constructor
	public HttpResponse hitReportUserPreferenceAPI() throws Exception {

		HttpResponse response;
		try {

			String queryString = "/reportRenderer/list/" + reportId + "/userPreference";

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "*/*");

			response = super.postRequest(postRequest, "{}");
			logger.debug("Response is : {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Report User Preference List API: response header {}", headers[i].toString());
			}
			responseReportUserPreference = EntityUtils.toString(response.getEntity());

			return response;
		} catch (Exception e) {
			logger.error("Error While Hitting ReportUserPreference [{}]", e.getLocalizedMessage());

		}
		return null;

	}

	/**
	 * @param responseReportUserPreference it will take the response of the Report User Preference API
	 * @return and return the map of View Id , Name Map for given report Id in Constructor , null if some issue while parsing the responseReportUserPreference
	 */
	public LinkedHashMap<Integer, String> getViewIdNameMap(String responseReportUserPreference) {

		LinkedHashMap<Integer, String> viewIdNameMap = new LinkedHashMap<>();

		try {
			JSONArray jsonResponse = new JSONArray(responseReportUserPreference);

			for (int i = 0; i < jsonResponse.length(); i++) {

				JSONObject viewDetail = jsonResponse.getJSONObject(i);

				if (viewDetail.has("name") && viewDetail.has("id")) {
					viewIdNameMap.put(viewDetail.getInt("id"), viewDetail.getString("name"));

				}

			}


		} catch (Exception e) {
			logger.error("Error While Parsing Response of Report User Preference List API for report Id ");
			return null;
		}

		return  viewIdNameMap;


	}

	/**
	 *
	 * @param viewId it will take the viewId as param
	 * @return the propertyValue as String of propertyName
	 * it will return notFound if given propertyName is not Found in Json
	 */
	public String getViewProperty(int viewId,String propertyName,String responseReportUserPreference) {

		String propertyValue = "notfound";

		try {
			JSONArray jsonResponse = new JSONArray(responseReportUserPreference);

			for (int i = 0; i < jsonResponse.length(); i++) {

				JSONObject viewDetail = jsonResponse.getJSONObject(i);

				if (viewDetail.has("id") && viewDetail.getInt("id") == viewId && viewDetail.has(propertyName)) {
					return viewDetail.get(propertyName).toString();
				}

			}


		} catch (Exception e) {
			logger.error("Error While Parsing Response of Report User Preference List API for report Id ");
			return propertyValue;
		}

		return  propertyValue;


	}




}
