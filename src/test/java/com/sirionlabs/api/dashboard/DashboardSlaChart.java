package com.sirionlabs.api.dashboard;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vijay.thakur on 7/25/2017.
 */
public class DashboardSlaChart extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DashboardSlaChart.class);
	String dashboardSlaChartJsonStr = null;

	public HttpResponse hitDashboardSlaChart(String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/dashboard/slachart/";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);

			postRequest.addHeader("Content-Type", "application/json");
			postRequest.addHeader("Accept", "text/plain, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.dashboardSlaChartJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("dashboard slachart response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting dashboard/slachart Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDashboardSlaChartJsonStr() {
		return this.dashboardSlaChartJsonStr;
	}
}
