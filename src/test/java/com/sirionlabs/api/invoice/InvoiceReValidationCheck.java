package com.sirionlabs.api.invoice;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoiceReValidationCheck {

    private final static Logger logger = LoggerFactory.getLogger(InvoiceReValidationCheck.class);

    //added by srijan
    public static HttpResponse hitActionUrl(int invoiceTypeId, int invoiceId) {
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/v2/actions/" + invoiceTypeId + "/" + invoiceId;

            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

        } catch (Exception e) {
            logger.error("Exception while hitting DownloadGraph Api. {}", e.getMessage());
        }
        return response;
    }

    public static boolean revalidateInvoice(int entityId, CustomAssert customAssert){
        String url = "/baseInvoice/revalidate";

        logger.info("Revalidating with url {}", url);
        try {
            APIUtils apiUtils = new APIUtils();
            HttpPost httpPost = apiUtils.generateHttpPostRequestWithQueryStringAndPayload(url, "application/json, text/javascript, */*; q=0.01", "application/json;charset=UTF-8","{\"entityId\":"+entityId+"}");
            HttpResponse httpResponse = APIUtils.postRequest(httpPost, null);
            String response = EntityUtils.toString(httpResponse.getEntity());
            if (response.contains("success")){
                return true;
            }
            else return false;

        } catch (Exception e) {
            logger.info("Exception caught in checkInvoiceValidationInFilterAndColumn()");
            customAssert.assertTrue(false,"Exception caught in checkInvoiceValidationInFilterAndColumn()");
        }

        return false;
    }
}
