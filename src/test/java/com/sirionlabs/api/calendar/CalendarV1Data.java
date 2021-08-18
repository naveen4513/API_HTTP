package com.sirionlabs.api.calendar;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CalendarV1Data extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(CalendarV1Data.class);

	private List<CalendarV1Data> records = new ArrayList<CalendarV1Data>();
	private String calendarV1DataJsonStr = null;

	private String title;
	private String supplier;
	private String entityStatus;
	private long start;
	private int id;
	private int entityTypeId;

	public HttpResponse hitCalendarV1Data(int month, int year) throws Exception {
		return hitCalendarV1Data(month, year, "false", "{}");
	}

	public HttpResponse hitCalendarV1Data(int month, int year, String calendarA) throws Exception {
		return hitCalendarV1Data(month, year, calendarA, "{}");
	}

	public HttpResponse hitCalendarV1Data(int month, int year, String calendarA, String payload) throws Exception {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/calendar/v1/data?month=" + month + "&year=" + year + "&calendarA=" + calendarA;
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.calendarV1DataJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Calendar V1 Data response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Calendar V1 Data Api. {}", e.getMessage());
		}
		return response;
	}

	public List<CalendarV1Data> getRecords() {
		return records;
	}

	public void setRecords(String jsonStr) {
		records.clear();

		if (ParseJsonResponse.validJsonResponse(jsonStr)) {
			try {
				JSONArray jsonArray = new JSONArray(jsonStr);

				if (jsonArray.length() != 0) {
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject jsonObj = new JSONObject(jsonArray.get(i).toString());

						JSONUtility json = new JSONUtility(jsonObj);

						CalendarV1Data calV1Data = new CalendarV1Data();

						calV1Data.setTitle(json.getStringJsonValue("title"));
						calV1Data.setSupplier(json.getStringJsonValue("supplier"));
						calV1Data.setEntityStatus(json.getStringJsonValue("entityStatus"));
						calV1Data.setStart(Long.parseLong(json.getStringJsonValue("start")));
						calV1Data.setId(Integer.parseInt(json.getStringJsonValue("id")));
						calV1Data.setEntityTypeId(Integer.parseInt(json.getStringJsonValue("entityTypeId")));

						records.add(calV1Data);
					}
				}
			} catch (Exception e) {
				logger.error("Exception while setting Records in CalendarV1Data", e.getMessage());
			}
		} else {
			logger.error("Invalid JSON Response");
		}
	}

	public String getTitle() {
		return this.title;
	}

	private void setTitle(String title) {
		this.title = title;
	}

	public String getSupplier() {
		return this.supplier;
	}

	private void setSupplier(String supplier) {
		this.supplier = supplier;
	}

	public String getEntityStatus() {
		return this.entityStatus;
	}

	private void setEntityStatus(String entityStatus) {
		this.entityStatus = entityStatus;
	}

	public long getStart() {
		return this.start;
	}

	private void setStart(long start) {
		this.start = start;
	}

	public int getId() {
		return this.id;
	}

	private void setId(int id) {
		this.id = id;
	}

	public int getEntityTypeId() {
		return this.entityTypeId;
	}

	private void setEntityTypeId(int entityTypeId) {
		this.entityTypeId = entityTypeId;
	}

	public String getCalendarV1DataJsonStr() {
		return this.calendarV1DataJsonStr;
	}
}
