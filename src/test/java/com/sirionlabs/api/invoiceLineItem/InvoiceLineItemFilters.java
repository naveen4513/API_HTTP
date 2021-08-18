package com.sirionlabs.api.invoiceLineItem;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoiceLineItemFilters extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(InvoiceLineItemFilters.class);
    public String filterresponse = null;

    public HttpResponse hitInvoiceLineItemFilter(int filterId,String payload) throws Exception {
        logger.debug("Hitting Invoice line item filter for filter id { }", filterId);

        String queryString = "/listRenderer/list/" + filterId + "/listdata?contractId=&relationId=&vendorId=&am=true";

        logger.debug("Query string url formed is {}", queryString);

        //Generate Http Post Request
        String acceptsHeader = "application/json, text/javascript, */*; q=0.01";
        String contentTypeHeader = "application/json;charset=UTF-8";
        HttpPost httpPostRequest = generateHttpPostRequestWithQueryString(queryString, acceptsHeader, contentTypeHeader);

        //Call the post Request from API Util
        HttpResponse httpResponse = super.postRequest(httpPostRequest, payload);
        logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
        this.filterresponse = EntityUtils.toString(httpResponse.getEntity());
        logger.debug("response json is: {}", filterresponse);

        Header[] headers = httpResponse.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            logger.debug("Filter response headers for filter Id {} {}",filterId, headers[i].toString());
        }
        logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
        return httpResponse;
    }
}