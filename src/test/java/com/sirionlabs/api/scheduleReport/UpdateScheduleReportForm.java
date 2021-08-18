package com.sirionlabs.api.scheduleReport;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * created by shivashish on 28/8/17.
 */
public class UpdateScheduleReportForm extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(UpdateScheduleReportForm.class);
	String apiStatusCode = null;
	String responseUpdateScheduleReportFormAPI;
	int reportId;

	public UpdateScheduleReportForm(int reportId) {
	this.reportId = reportId;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getResponseUpdateScheduleReportFormAPI() {
		return responseUpdateScheduleReportFormAPI;
	}

	// this function will hit the Update Schedule Report For API for Given Report Id
	public HttpResponse hitUpdateScheduleReportFormAPI(HashMap<String, String> queryStringParams) throws Exception {

		HttpResponse response;
		String queryString = "/scheduleReport/updateForm";
		if (queryStringParams != null) {
			String urlParams = UrlEncodedString.getUrlEncodedString(queryStringParams);
			queryString += "?" + urlParams;
		}
		logger.info("Query string url formed is {}", queryString);

		HttpGet getRequest = new HttpGet(queryString);
		getRequest.addHeader("Accept", "*/*");

		response = super.getRequest(getRequest);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Update Report form API: response header {}", headers[i].toString());
		}

		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		responseUpdateScheduleReportFormAPI = EntityUtils.toString(response.getEntity());
		return response;
	}


}
