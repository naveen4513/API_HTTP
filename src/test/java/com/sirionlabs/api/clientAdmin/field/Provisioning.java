package com.sirionlabs.api.clientAdmin.field;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Provisioning extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Provisioning.class);

	//This method returns HTML Response
	public String hitFieldProvisioning() {
		String response = null;

		try {
			HttpGet getRequest;
			String queryString = "/field/provisioning";
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.getRequest(getRequest);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Admin Field Provisioning Response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Admin Field Provisioning Api. {}", e.getMessage());
		}
		return response;
	}

	//This method returns JSON Response. GET API
	public String hitFieldProvisioning(int userTypeId, int entityTypeId) {
		String response = null;

		try {
			HttpGet getRequest;
			String queryString = "/field/provisioning/" + userTypeId + "/" + entityTypeId;
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.getRequest(getRequest);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Admin Field Provisioning Response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Admin Field Provisioning Api for UserTypeId {} and EntityTypeId {}. {}", userTypeId, entityTypeId, e.getMessage());
		}
		return response;
	}

	public List<Map<String, String>> getAllHiddenFields(String provisioningResponse) {
		List<Map<String, String>> allHiddenFields = new ArrayList<>();

		try {
			JSONObject jsonObj = new JSONObject(provisioningResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("fields");

			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = jsonArr.getJSONObject(i);

				if (jsonObj.getBoolean("hidden")) {
					Map<String, String> hiddenFieldMap = new HashMap<>();
					hiddenFieldMap.put("name", jsonObj.getString("name"));
					hiddenFieldMap.put("entityFieldId", String.valueOf(jsonObj.getInt("entityFieldId")));

					allHiddenFields.add(hiddenFieldMap);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Hidden Fields from Provisioning Response. {}", e.getMessage());
			return null;
		}

		return allHiddenFields;
	}
}