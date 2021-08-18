package com.sirionlabs.api.governancebody;


import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class AdhocMeeting extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(AdhocMeeting.class);

    public String hitAdhocMeetingApi(String governanceBodyId , String occurrenceDate , String startTime , String timeZone , String duration, String location) {
        String responseStr = null;
        String payload =  createPayload(governanceBodyId ,occurrenceDate ,startTime , timeZone , duration, location );
        try {

            HttpPost postRequest;
            String queryString = "/tblgovernancebodychild/save";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            responseStr = EntityUtils.toString(response.getEntity());
            System.out.println(responseStr);
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting  Adhoc metting Create Api. {}", e.getMessage());
        }
        return responseStr;
    }

    private String createPayload(String governanceBodyId , String occurrenceDate , String startTime , String timeZone , String duration, String location ){
        HashMap<String, String> value = new HashMap<>();
        value.put("governanceBodyId",governanceBodyId);
        value.put("occurrenceDate",occurrenceDate);
        value.put("startTime",startTime);
        value.put("timeZone",timeZone);
        value.put("duration",duration);
        value.put("location",location);
        String payload = "{\"governanceBody\":{\"id\":${governanceBodyId}},\"occurrenceDate\":\"${occurrenceDate} 00:00:00\",\"startTime\":{\"id\":43,\"name\":\"${startTime}\"},\"timeZone\":{\"id\":8,\"name\":\"${timeZone}\"},\"duration\":{\"id\":1,\"name\":\"${duration}\"},\"location\":\"${location}\"}";
        payload = StringUtils.strSubstitutor(payload,value);
        if(JSONUtility.validjson(payload)){
               return payload;
        }else{
            return  null;
        }
    }


    public String  hitDefaultValue(){
        String responseStr = null;

        try {
            HttpGet getRequest;
            String queryString = "/gb/info";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "text/html, */*; q=0.01");
            getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.getRequest(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            responseStr = EntityUtils.toString(response.getEntity());
        }catch (Exception e) {
            logger.error("Exception while hitting default value of adhoc Api. {}", e.getMessage());
        }

        return  responseStr;
    }

}
