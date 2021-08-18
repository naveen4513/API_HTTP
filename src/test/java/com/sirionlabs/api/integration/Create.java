package com.sirionlabs.api.integration;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Create extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Create.class);
	String createJsonStr = null;

	public HttpResponse hitCreate(String entityName, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;

			String entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
			String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			String urlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingFilePath, entityIdMappingFileName, entityName, "url_name");

			String queryString = "/" + urlName + "/create";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.createJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Create response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Create Api. {}", e.getMessage());
		}
		return response;
	}

	public String getCreateJsonStr() {
		return createJsonStr;
	}
}
