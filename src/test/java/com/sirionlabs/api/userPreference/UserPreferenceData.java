package com.sirionlabs.api.userPreference;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 11/7/17.
 */
public class UserPreferenceData extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(UserPreferenceData.class);
	String responseCreateUserPreference;
	String responseDeleteUserPreference;
	String responseUpdateUserPreference;
	String responsePinUserPreference;
	String responseUnPinUserPreference;
	String responselistUserPreference;

	public UserPreferenceData() {
	}

	public String getResponseCreateUserPreference() {
		return responseCreateUserPreference;
	}

	public String getResponseDeleteUserPreference() {
		return responseDeleteUserPreference;
	}

	public String getResponseUpdateUserPreference() {
		return responseUpdateUserPreference;
	}

	public String getResponselistUserPreference() {
		return responselistUserPreference;
	}

	public String getResponsePinUserPreference() { return responsePinUserPreference; }

	public String getResponseUnPinUserPreference() { return responseUnPinUserPreference; }

	// this function will hit the User Preference List API taking input entityURLId
	public HttpResponse hitUserPreferenceListAPI(int entityURLId) throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + entityURLId + "/userPreference";

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");

		response = APIUtils.postRequest(postRequest, "{}");
		logger.debug("Response is : {}", response.getStatusLine().toString());
		responselistUserPreference = EntityUtils.toString(response.getEntity());

		return response;
	}

	// this function will hit the User Preference Delete API taking input entityURLId
	// this function is taking isPublic for publicVisibility query param , right now we are not using it .
	public HttpResponse hitUserPreferenceDeleteAPI(int entityURLId, int viewId, boolean isPublic) throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + entityURLId + "/deleteUserPreference" + "?preferenceId=" + viewId;

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");

		response = APIUtils.postRequest(postRequest, "{}");
		logger.debug("Response is : {}", response.getStatusLine().toString());

		responseDeleteUserPreference = EntityUtils.toString(response.getEntity());
		return response;
	}


	// this function will hit the pinPreference API taking input entityURLId
	public HttpResponse hitPinUserPreferenceAPI(int entityURLId, int viewId) throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + entityURLId + "/" + viewId + "/pinPreference";

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");

		response = super.postRequest(postRequest, "{}");
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Pin User Preference API : response header {}", headers[i].toString());
		}

		responsePinUserPreference = EntityUtils.toString(response.getEntity());
		return response;
	}


	// this function will hit the deletePinPreference API taking input entityURLId
	public HttpResponse hitDeletePinUserPreferenceAPI(int entityURLId, int viewId) throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + entityURLId + "/" + viewId + "/deletePinPreference";

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");

		response = super.postRequest(postRequest, "{}");
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Unpin User Preference API : response header {}", headers[i].toString());
		}

		responseUnPinUserPreference = EntityUtils.toString(response.getEntity());
		return response;
	}


	// this function will hit the SaveDefaultUserPreference API taking input entityURLId
	// this function is taking isPublic for publicVisibility query param , right now we are not using it .
	public HttpResponse hitSaveDefaultUserPreferenceAPI(int entityURLId, int viewId) throws Exception {

		HttpResponse response;
		String queryStringBuilder = "?preferenceId=" + viewId + "&isDefault=true";
		String queryString = "/listRenderer/list/" + entityURLId + "/userpreferences/saveDefaultUserPreference" + queryStringBuilder;

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "*/*");

		response = super.postRequest(postRequest, "{}");
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Save Default User Preference API : response header {}", headers[i].toString());
		}
		return response;
	}


	// this function will hit the Create User Preference API taking input entityURLId and payload
	public HttpResponse hitUserPreferenceCreateAPI(int entityURLId, String payload) throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + entityURLId + "/userpreferences/save";

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		postRequest.addHeader("Accept-Encoding", "gzip, deflate");

		response = APIUtils.postRequest(postRequest, payload);
		responseCreateUserPreference = EntityUtils.toString(response.getEntity());
		return response;
	}

	// this function will hit the Create User Preference API taking input entityURLId and payload
	public HttpResponse hitUserPreferenceUpdateAPI(int entityURLId, String payload) throws Exception {

		HttpResponse response;
		String queryString = "/listRenderer/list/" + entityURLId + "/userpreferences/update";

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		postRequest.addHeader("Accept-Encoding", "gzip, deflate");

		response = APIUtils.postRequest(postRequest, payload);
		logger.debug("Response is : {}", response.getStatusLine().toString());

		responseUpdateUserPreference = EntityUtils.toString(response.getEntity());
		return response;
	}


	// this function will verify whether viewName exist in Response
	public Boolean verifyWhetherViewExistOrNot(String response, String viewName) throws Exception {

		String tempViewName;
		int numOfViews = response.split("\"id\"").length - 1;
		logger.debug("Number of Views is :{}", numOfViews);

		for (int i = 0; i < numOfViews; i++) {
			JSONArray viewsArray = new JSONArray(response);
			tempViewName = viewsArray.getJSONObject(i).get("name").toString();
			logger.debug("tempViewName is: {} and viewName to be verified is: {}", tempViewName, viewName);
			if (tempViewName.contentEquals(viewName))
				return true;
		}

		return false;
	}


	// this function will return the viewId based on View Name if Exist otherWise -1
	public int getViewIdByViewName(String response, String viewName) throws Exception {

		String tempViewName;
		int numofViews = response.split("\"id\"").length - 1;
		logger.debug("Number of Views is :{}", numofViews);

		for (int i = 0; i < numofViews; i++) {
			JSONArray viewsArray = new JSONArray(response);
			tempViewName = viewsArray.getJSONObject(i).get("name").toString();
			logger.debug("tempViewName is: {} and viewName to be verified is: {}", tempViewName, viewName);
			if (tempViewName.contentEquals(viewName)) {
				logger.info("tempViewName is: {} and viewName to be verified is: {}", tempViewName, viewName);
				return Integer.parseInt(viewsArray.getJSONObject(i).get("id").toString());
			}

		}

		return -1;
	}


	// this function will return the viewId based on View Name if Exist otherWise -1
	public boolean checkWhetherViewIdIsDefaultUserView(String response, String viewName) throws Exception {

		String tempViewName;
		int numofViews = response.split("\"id\"").length - 1;
		logger.debug("Number of Views is :{}", numofViews);

		for (int i = 0; i < numofViews; i++) {
			JSONArray viewsArray = new JSONArray(response);
			tempViewName = viewsArray.getJSONObject(i).get("name").toString();
			logger.debug("tempViewName is: {} and viewName to be verified is: {}", tempViewName, viewName);
			if (tempViewName.contentEquals(viewName)) {
				logger.debug("tempViewName is: {} and viewName to be verified is: {}", tempViewName, viewName);
				return (boolean) viewsArray.getJSONObject(i).get("isdefault");
			}

		}

		return false;
	}

	// this function will whether the viewId is Pinned or Not
	public boolean checkWhetherViewIdIsPinnedOrNot(String response, String viewName) throws Exception {


		JSONArray listUserPreferenceAPIResponse = new JSONArray(response);
		for(int i=0;i<listUserPreferenceAPIResponse.length();i++)
		{
			JSONObject viewdetailJson = listUserPreferenceAPIResponse.getJSONObject(i);
			if(viewdetailJson.get("id").toString().contentEquals(viewName))
			{
				if(Boolean.parseBoolean(viewdetailJson.get("pinned").toString()) == true)
					return true;
			}
			else
				continue;
		}

		return false;
	}


	// this function will whether the viewId is UnPinned or Not
	public boolean checkWhetherViewIdIsUnPinnedOrNot(String response, String viewName) throws Exception {


		JSONArray listUserPreferenceAPIResponse = new JSONArray(response);
		for(int i=0;i<listUserPreferenceAPIResponse.length();i++)
		{
			JSONObject viewdetailJson = listUserPreferenceAPIResponse.getJSONObject(i);
			if(viewdetailJson.get("id").toString().contentEquals(viewName))
			{
				if(Boolean.parseBoolean(viewdetailJson.get("pinned").toString()) == false)
					return true;
			}
			else
				continue;
		}

		return false;
	}


	// this function will return the default User View Id if exist otherWise -1 for System Default User View Case
	public int getDefaultUserView(String response) throws Exception {

		Boolean isDefault;
		int numofViews = response.split("\"id\"").length - 1;
		logger.debug("Number of Views is :{}", numofViews);

		for (int i = 0; i < numofViews; i++) {
			JSONArray viewsArray = new JSONArray(response);
			isDefault = (boolean) viewsArray.getJSONObject(i).get("isdefault");
			logger.info("isDefault Flag is : {} , for Id : {} ", isDefault, viewsArray.getJSONObject(i).get("id").toString());
			if (isDefault) {
				logger.info("Default User View Id is : {}", viewsArray.getJSONObject(i).get("id"));
				return Integer.parseInt(viewsArray.getJSONObject(i).get("id").toString());
			}

		}

		return -1;
	}

}
