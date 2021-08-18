package com.sirionlabs.api.invoice;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class InvoiceRateCard extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(InvoiceRateCard.class);
    String rateCardResponseJsonStr = null;


    public String getRateCardResponseJsonStr() {
        return this.rateCardResponseJsonStr;
    }

    public HttpResponse hitRateCardAPI(int listid,int entityTypeId,int tabid,String payload) throws IOException {

        HttpResponse httpResponse = null;
        String queryString = "/listRenderer/list/" + listid + "/tablistdata/" + entityTypeId + "/" + tabid;
        logger.debug("Query string url formed is {}", queryString);

        HttpPost httpPostRequest = new HttpPost(queryString);
        httpPostRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        httpPostRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
        httpPostRequest.addHeader("Accept-Encoding", "gzip, deflate");

        httpResponse = super.postRequest(httpPostRequest, payload);
        rateCardResponseJsonStr = EntityUtils.toString(httpResponse.getEntity());

        logger.debug("The Response is : [ {} ]", httpResponse);

        logger.debug("response json is: {}", rateCardResponseJsonStr);

        Header[] headers = httpResponse.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("Response header {}", headers[i].toString());
        }
        logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
        return httpResponse;
    }
}