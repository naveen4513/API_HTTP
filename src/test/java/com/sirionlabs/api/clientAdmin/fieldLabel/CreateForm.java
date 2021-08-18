package com.sirionlabs.api.clientAdmin.fieldLabel;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateForm extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(CreateForm.class);

	public String hitFieldLabelCreateForm() {
		String response = null;

		try {
			HttpGet getRequest;
			String queryString = "/fieldlabel/createForm";
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = getRequest(getRequest);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Field Label CreateForm  Response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Field Label CreateForm Api. {}", e.getMessage());
		}
		return response;
	}
}