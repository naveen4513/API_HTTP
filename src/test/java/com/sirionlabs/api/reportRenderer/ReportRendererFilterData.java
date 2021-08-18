package com.sirionlabs.api.reportRenderer;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by shivashish on 25/7/17.
 */
public class ReportRendererFilterData extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(ReportRendererFilterData.class);
	String reportRendererFilterDataJsonStr = null;

	public HttpResponse hitReportRendererFilterData(int listId) {
		return hitReportRendererFilterData(listId, "{}", null);
	}

	public HttpResponse hitReportRendererFilterData(int listId, String payload, Map<String, String> params) {
		HttpResponse response = null;

		String jsonObjectOrderDelimiter = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");
		try {
			String queryString = "/reportRenderer/list/" + listId + "/filterData";
			if (params != null) {
				String urlParams = UrlEncodedString.getUrlEncodedString(params);
				queryString += "?" + urlParams;
			}
			logger.debug("Query string url formed is {}", queryString);
			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.reportRendererFilterDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", reportRendererFilterDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ReportRenderer response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting ReportRendererFilterData Api. {}", e.getMessage());
		}
		return response;
	}

	public String getReportRendererFilterDataJsonStr() {
		return this.reportRendererFilterDataJsonStr;
	}
}
