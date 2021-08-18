package com.sirionlabs.api.recentlyViewed;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class RecentlyViewed extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(RecentlyViewed.class);
    private String jsonStr = null;

    public HttpResponse hitRecentlyViewed() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/userActivityTracker/list/";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.getRequest(getRequest);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.jsonStr = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting MetaDataSearch Api. {}", e.getMessage());
        }
        return response;
    }

    public String getJsonStr() {
        return this.jsonStr;
    }

}
