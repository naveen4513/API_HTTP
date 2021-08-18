package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Delete extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(Create.class);
	private String deleteJsonStr = null;

	public HttpResponse hitDelete(String entityName, String payload) {
		return hitDelete(entityName, payload, null);
	}

	public HttpResponse hitDelete(String entityName, String payload, String urlName) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;

			if (urlName == null) {
				String entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
				String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
				urlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingFilePath, entityIdMappingFileName, entityName, "url_name");
			}

			String queryString = "/" + urlName + "/delete";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = APIUtils.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.deleteJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				logger.debug("Delete response header {}", header.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Delete Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDeleteJsonStr() {
		return deleteJsonStr;
	}
}
