package com.sirionlabs.api.delegation;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleGroupForEntityType extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(RoleGroupForEntityType.class);
    private static String roleGroupForEntityTypeJsonStr;

    public String getRoleGroupForEntityTypeJsonStr(){
        return this.roleGroupForEntityTypeJsonStr;
    }

    public HttpResponse getRoleGroupsForEntityType(String entityId) {
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/delegation/rolegroups/"+ entityId;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = super.getRequest(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.roleGroupForEntityTypeJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Role Groups For Entity Type API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Role Groups For Entity Type API. {}", e.getMessage());
        }
        return response;
    }
}
