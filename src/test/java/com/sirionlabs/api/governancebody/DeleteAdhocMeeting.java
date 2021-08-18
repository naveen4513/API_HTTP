package com.sirionlabs.api.governancebody;

import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteAdhocMeeting extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(AdhocMeeting.class);

    public static String deleteAdhocMeeting(int meetingId) {
        String responseStr = null;
        JSONObject payload = null;// createPayload(governanceBodyId ,occurrenceDate ,startTime , timeZone , duration, location );
        try {
            String showApiResponse = ShowHelper.getShowResponse(87,meetingId);
            if (showApiResponse != null) {
                JSONObject obj = new JSONObject(showApiResponse);
                JSONObject body = obj.getJSONObject("body");
                JSONObject data = body.getJSONObject("data");
                payload  = new JSONObject();
                JSONObject payload_body  = new JSONObject();
                payload_body.put("data",data);
                payload.put("body",payload_body);
            }

            HttpPost postRequest;
            String queryString = "/governancebodychild/delete";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.postRequest(postRequest, payload.toString());
            logger.debug("Response status is {}", response.getStatusLine().toString());
            responseStr = EntityUtils.toString(response.getEntity());
            if(JSONUtility.parseJson(responseStr,"$.header.response.status").equals("success")){
                logger.info(meetingId+" Governance Body Meeting successfully deleted");
            }
            else{
                logger.error("errro in deletion of Governance Body Meeting " + meetingId );
            }
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting  Adhoc metting delete Api. {}", e.getMessage());
        }
        return responseStr;
}
}
