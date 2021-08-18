package com.sirionlabs.api.massEmail;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateForm extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(UpdateForm.class);

	public String hitMassEmailUpdateForm(int massEmailId) throws Exception {
		String responseStr = null;
		try {
			HttpGet getRequest;
			String queryString = "/massEmail/updateForm?id=" + massEmailId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

			HttpResponse response = super.getRequest(getRequest, false);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Mass Email Update Form response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Mass Email Update Form Api. {}", e.getMessage());
		}
		return responseStr;
	}
}
