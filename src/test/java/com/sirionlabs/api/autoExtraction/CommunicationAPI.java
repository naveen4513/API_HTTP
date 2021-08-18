package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

public class CommunicationAPI extends TestAPIBase{
    private final static Logger logger = LoggerFactory.getLogger(CommunicationAPI.class);
    public static String getAPIPath() {
        return "/autoextraction/v1/comment?version=2.0";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getPayload() throws IOException {
        JSONObject payload = new JSONObject();

        try{
            logger.info("Getting list data after applying Assigned document filter");
            HttpResponse listDataResponse = AutoExtractionHelper.assignedFilter();
            String listDataResponseStr = EntityUtils.toString(listDataResponse.getEntity());
            JSONObject jsonObject = new JSONObject(listDataResponseStr);
            JSONArray jsonArr = jsonObject.getJSONArray("data");
            JSONObject listdata = jsonArr.getJSONObject(0);
            int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "id");
            String[] idValue = listdata.getJSONObject(Integer.toString(columnId)).getString("value").trim().split(":;");
            String docId = idValue[1];
            logger.info("Hitting Show API for 1st assigned document of the list, i.e " + docId);
            HttpResponse showPageResponse = AutoExtractionHelper.docShowAPI(Integer.parseInt(docId));
            String showResponseStr = EntityUtils.toString(showPageResponse.getEntity());
            logger.info("Adding comment in communication tab for document id " + docId);
            JSONObject obj = new JSONObject(showResponseStr);
            JSONObject body = obj.getJSONObject("body");
            JSONObject data = body.getJSONObject("data");
            JSONObject comment = data.getJSONObject("comment");
            logger.info("Creating payload for adding Comment for document id "+docId);
            JSONObject cmnts = comment.getJSONObject("comments");
            cmnts.put("values", "API Automation comments");
            JSONObject payload_body = new JSONObject();
            data.put("comment", comment);
            payload_body.put("data", data);
            payload.put("body", payload_body);
        }
        catch (Exception e)
        {
            logger.info("Exception while creating payload for adding communication due to "+e.getMessage());
        }
        return payload.toString();
    }

    public static APIResponse communicationAPIResponse(String apiPath, HashMap<String, String> headers, String payload) {
        return executor.post(apiPath, headers, payload).getResponse();
    }
}
