package com.sirionlabs.api.listRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UserListMetaData extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(UserListMetaData.class);
	private String apiStatusCode;
	private String userPreferenceListDataJsonStr;

	public HttpResponse hitUserPreferenceList(int listId) {
		HttpResponse response = null;
		try {
			String queryString = "/listRenderer/list/" + listId + "/userPreference";

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = APIUtils.postRequest(postRequest, "");
			logger.debug(response.getStatusLine().toString());


			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = response.getStatusLine().toString();
			this.userPreferenceListDataJsonStr = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting list userPreference API for listId [{}] , Exception is [{}]", listId, e.getLocalizedMessage());
		}

		return response;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getListUserPreferenceAPIResponse() {
		return userPreferenceListDataJsonStr;
	}

	public String hitUserListMetaData(int listId, Map<String, String> queryStringParams) {
		String responseCode = null;

		try {
			String queryString = "/listRenderer/list/" + listId + "/userListMetaData";

			if (queryStringParams != null) {
				String params = UrlEncodedString.getUrlEncodedString(queryStringParams);
				queryString = queryString.concat("?" + params);
			}

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "*/*");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			logger.info("Query String: {}", queryString);

			HttpResponse httResponse = APIUtils.postRequest(postRequest, "");
			responseCode = String.valueOf(httResponse.getStatusLine().getStatusCode());
		} catch (Exception e) {
			logger.error("Exception while hitting UserListMetaData API for listId [{}] , Exception is [{}]", listId, e.getLocalizedMessage());
		}

		return responseCode;
	}
}
