package com.sirionlabs.api.delegation;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegationEntityTypes extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(DelegationEntityTypes.class);
    private static String delegationEntityTypesJsonStr;

    public String getDelegationEntityTypesJsonStr(){
            return this.delegationEntityTypesJsonStr;
    }

    public HttpResponse getDelegationEntityTypes() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/delegation/entitytypes";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.getRequest(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.delegationEntityTypesJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delegation Entity Types API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delegation Entity Types API. {}", e.getMessage());
        }
        return response;
    }
}
