package com.sirionlabs.helper.clientAdmin;

import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchServer
{
    private final static Logger logger = LoggerFactory.getLogger(SwitchServer.class);

    //Switch to Server A API
    public static HttpResponse originalVersion(String userId){
        HttpResponse response = null;
        HttpGet getRequest;
        try{
            String queryString = "/tblusers/update-ab-version/" + userId +"/original";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept","application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    //Switch to Server B API
    public static HttpResponse customVersion(String userId){
        HttpResponse response = null;
        HttpGet getRequest;
        try{
            String queryString = "/tblusers/update-ab-version/" + userId +"/custom";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept","application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

}
