package com.sirionlabs.api.dashboard;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vijay.thakur on 7/25/2017.
 */
public class DashboardRecords extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DashboardRecords.class);
	String dashboardRecordsJsonStr = null;

	public HttpResponse hitDashboardRecords(String chartObjValue) {
		return hitDashboardRecords(false, chartObjValue);
	}

	public HttpResponse hitDashboardRecords(boolean headersOnly, String chartObjValue) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/dashboard/records/";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("chartObj", chartObjValue);

			if (headersOnly)
				parameters.put("headersOnly", "true");

			String params = UrlEncodedString.getUrlEncodedString(parameters);
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			postRequest.addHeader("Accept", "text/html, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, params);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.dashboardRecordsJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("dashboard/records response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting dashboard/records Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDashboardRecordsJsonStr() {
		return this.dashboardRecordsJsonStr;
	}
}
