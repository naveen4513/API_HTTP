package com.sirionlabs.api.listRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListingChartData extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(ListingChartData.class);

    public String hitListChartData(int listId, String xaxis, int chartType, String payload) {
        HttpResponse response = null;
        String apiresponse="";

        try {
            HttpPost postRequest;
            String queryString = "/listingchart/chartdata/" + listId + "?xaxis=" + xaxis +"&yaxis=count&chartType=" + chartType;


            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
           // postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            apiresponse = EntityUtils.toString(response.getEntity());





        } catch (Exception e) {
            logger.error("Exception while hitting ListChart Data API. {}", e.getMessage());
        }
        return apiresponse;

    }


}

