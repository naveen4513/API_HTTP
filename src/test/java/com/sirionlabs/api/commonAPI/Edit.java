package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Edit extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Edit.class);
	HttpResponse editAPIHttpResponse;
	public String editAPIResponseCode;

	public HttpResponse getEditAPIResponse() {
		return editAPIHttpResponse;
	}
	static String editDataJsonStr = null;
     public String getEditDataJsonStr() {
	 	return editDataJsonStr;
	 }
	public String hitEdit(String entityName, int entityId) {
		String responseStr = null;
		try {
			HttpGet getRequest;
			String urlName = getUrlName(entityName);
			String queryString = "/" + urlName + "/edit/" + entityId;
			logger.debug("Query string url formed is {}", queryString);

			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse response = super.getRequest(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());
			editAPIResponseCode = response.getStatusLine().toString();
			editAPIHttpResponse = response;
			editDataJsonStr = responseStr;
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Edit Get response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Edit Get Api. {}", e.getMessage());
		}
		return responseStr;
	}

	public String hitEdit(String entityName, String payload) throws Exception {
		String responseStr = null;
		try {
			HttpPost postRequest;
			String urlName = getUrlName(entityName);
			String queryString = "/" + urlName + "/edit?version=2.0";
			logger.debug("Query string url formed is {}", queryString);

			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			HttpResponse response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());
			editAPIHttpResponse = response;
			editDataJsonStr = responseStr;
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Edit Post response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Edit Post Api. {}", e.getMessage());
		}
		return responseStr;
	}

	public String hitDnoEdit(String entityName, String payload) throws Exception {
		String responseStr = null;
		try {
			HttpPost postRequest;
			String queryString = "/dnos/edit?version=2.0";
			logger.debug("Query string url formed is {}", queryString);

			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			HttpResponse response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());
			editAPIHttpResponse = response;
			editDataJsonStr = responseStr;
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Edit Post response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Edit Post Api. {}", e.getMessage());
		}
		return responseStr;
	}


	private String getUrlName(String entityName) {
		String urlName = null;
		try {
			String entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
			String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			urlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingFilePath, entityIdMappingFileName, entityName, "url_name");
		} catch (Exception e) {
			logger.error("Exception while getting URL Name of Entity {}. {}", entityName, e.getStackTrace());
		}
		return urlName;
	}

	public static String getEditGetApiPath(String entityName, int recordId) {
		String urlName = new Edit().getUrlName(entityName);
		return "/" + urlName + "/edit/" + recordId;
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
		headers.put("Content-Type", "application/json;charset=UTF-8");

		return headers;
	}

	public String getEditPayload(String entityName,int entityId){

     	String editResponse = hitEdit(entityName,entityId);

     	JSONObject editResponseJson = new JSONObject(editResponse);

		editResponseJson.remove("header");
		editResponseJson.remove("session");
		editResponseJson.remove("actions");
		editResponseJson.remove("createLinks");

		editResponseJson.getJSONObject("body").remove("layoutInfo");
		editResponseJson.getJSONObject("body").remove("globalData");
		editResponseJson.getJSONObject("body").remove("errors");

		return editResponseJson.toString();
	}

}
