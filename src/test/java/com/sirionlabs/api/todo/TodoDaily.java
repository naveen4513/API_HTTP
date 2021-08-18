package com.sirionlabs.api.todo;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodoDaily extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(TodoDaily.class);
	private String todoDailyJsonStr = null;
	private List<Map<String, String>> approvals;
	private List<Map<String, String>> tasks;
	private List<Map<String, String>> meetings;

	public HttpResponse hitTodoDaily() {
		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "/todo/daily";
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = APIUtils.getRequest(getRequest, true);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.todoDailyJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Todo Daily response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Todo Daily Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitTodoDailyEntitySpecific(String enitityName, int entityId) throws IOException {
		HttpResponse response;
		String queryString = "/todo/daily?" + enitityName + "=" + entityId;
		logger.info("Verifying TodoWeekly for QueryString : [ {} ]", queryString);

		HttpGet getRequest = new HttpGet(queryString);
		getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		getRequest.addHeader("Accept-Encoding", "gzip, deflate");
		response = APIUtils.getRequest(getRequest);

		logger.debug(response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (Header oneHeader : headers) {
			logger.debug(oneHeader.toString());
		}
		todoDailyJsonStr = EntityUtils.toString(response.getEntity());
		return response;
	}


	public void setAllEntities(String jsonStr) {
		approvals = this.setEntity(jsonStr, "Pending Approvals");
		tasks = this.setEntity(jsonStr, "Pending Tasks");
		meetings = this.setEntity(jsonStr, "Governance Meetings");
	}

	private List<Map<String, String>> setEntity(String jsonStr, String entityName) {
		List<Map<String, String>> entity = new ArrayList<>();
		Map<String, String> entityMap;
		try {
			if (ParseJsonResponse.validJsonResponse(jsonStr)) {
				JSONObject jsonObj = new JSONObject(jsonStr);
				JSONArray jsonArr = jsonObj.getJSONArray(entityName);

				for (int i = 0; i < jsonArr.length(); i++) {
					jsonObj = jsonArr.getJSONObject(i);

					entityMap = new HashMap<>();
					entityMap.put("id", Integer.toString(jsonObj.getInt("id")));
					entityMap.put("entityTypeId", Integer.toString(jsonObj.getInt("entityTypeId")));
					entityMap.put("supplier", jsonObj.getString("relationName"));
					entityMap.put("dueDateTimeStamp", Long.toString(jsonObj.getLong("dueDateTimestamp")));

					entity.add(entityMap);
				}
			} else {
				logger.error("Invalid JSON Response");
			}
		} catch (Exception e) {
			logger.error("Exception while setting Entity {} in TodoDaily. {}", entityName, e.getMessage());
		}
		return entity;
	}

	public List<Map<String, String>> getApprovals() {
		return this.approvals;
	}

	public List<Map<String, String>> getTasks() {
		return this.tasks;
	}

	public String getTodoDailyJsonStr() {
		return this.todoDailyJsonStr;
	}

	public List<Map<String, String>> getMeetings() {
		return this.meetings;
	}
}
