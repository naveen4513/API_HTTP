package com.sirionlabs.api.calendar;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CalendarData extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(CalendarData.class);
    private String calendarDataJsonStr = null;
    private List<CalendarData> records = new ArrayList<>();
    private String title;
    private String supplier;
    private String entityStatus;
    private long start;
    private int id;
    private int entityTypeId;

    public void hitCalendarData(int month, int year) {
        hitCalendarData(month, year, "false", "{}");
    }

    public void hitCalendarData(int month, int year, String calendarA) {
        hitCalendarData(month, year, calendarA, "{}");
    }

    public void hitCalendarData(int month, int year, String calendarA, String payload) {
        HttpResponse response;
        try {
            HttpPost postRequest;
            String queryString = "/calendar/data?month=" + month + "&year=" + year + "&calendarA=" + calendarA;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.calendarDataJsonStr = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            logger.error("Exception while hitting Calendar Data Api. {}", e.getMessage());
        }
        records.clear();
    }

    public List<CalendarData> getRecords() {
        return records;
    }

    public void setRecords(String jsonStr) {
        records.clear();

        if (ParseJsonResponse.validJsonResponse(jsonStr)) {
            JSONArray jsonArray = new JSONArray(jsonStr);

            try {
                if (jsonArray.length() != 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObj = new JSONObject(jsonArray.get(i).toString());
                        JSONUtility json = new JSONUtility(jsonObj);
                        CalendarData calData = new CalendarData();

                        calData.setTitle(json.getStringJsonValue("title"));
                        calData.setSupplier(json.getStringJsonValue("supplier"));
                        calData.setEntityStatus(json.getStringJsonValue("entityStatus"));
                        calData.setStart(Long.parseLong(json.getStringJsonValue("start")));
                        calData.setId(Integer.parseInt(json.getStringJsonValue("id")));
                        calData.setEntityTypeId(Integer.parseInt(json.getStringJsonValue("entityTypeId")));

                        records.add(calData);
                    }
                }
            } catch (Exception e) {
                logger.error("Exception while setting Records in Calendar Data {}", e.getMessage());
            }
        } else {
            logger.error("Invalid JOSN Response");
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

    public String getCalendarDataJsonStr() {
        return this.calendarDataJsonStr;
    }
}