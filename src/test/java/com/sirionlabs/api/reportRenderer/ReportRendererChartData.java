package com.sirionlabs.api.reportRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportRendererChartData extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(ReportRendererChartData.class);
	String reportRendererChartDataStr = null;

	public HttpResponse hitReportRendererChartData(String payload, Integer reportId) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/reportRenderer/chart/" + reportId + "/data/";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);

			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.reportRendererChartDataStr = EntityUtils.toString(response.getEntity());
			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ReportRendererChartData response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting ReportRendererChartData Api. {}", e.getMessage());
		}
		return response;
	}

	public String getReportRendererChartDataJsonStr() {
		return this.reportRendererChartDataStr;
	}
}
