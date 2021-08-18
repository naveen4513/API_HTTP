package com.sirionlabs.api.listRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SlaSpecificGraph extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(SlaSpecificGraph.class);
	String slaSpecificGraphJsonStr = null;

	public String hitSlaSpecificGraph(String tabUrlId, String slaChartId) {

		String slaSpecificGraphResponse = null;

		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "listRenderer/list/" + tabUrlId + "/slaSpecificGraph/" + slaChartId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.getRequest(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("SlaSpecificGraph response header {}", headers[i].toString());
			}
			this.slaSpecificGraphJsonStr = EntityUtils.toString(response.getEntity());

		} catch (Exception e) {
			logger.error("Exception occurred while hitting slaSpecificGraph api for the url . {}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
		}
		return slaSpecificGraphResponse;
	}

	public String getSlaSpecificGraphJsonStr() {
		return this.slaSpecificGraphJsonStr;
	}
}
