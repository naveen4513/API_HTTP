package com.sirionlabs.api.listRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListingChartAxis extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(ListingChartAxis.class);

    public String hitListingChart(int listId) {
        HttpResponse response = null;
        String apiResponse = "";
        try {
            HttpGet getRequest;

            String queryString = "/listingchart/chartaxis/" + listId;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = super.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            apiResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting Show Api. {}", e.getMessage());
        }
        return apiResponse;
    }
}