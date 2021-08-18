package com.sirionlabs.api.auditLogs;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldHistory extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(FieldHistory.class);

    public String hitFieldHistory(Long historyId, int entityTypeId) {
        return hitFieldHistory(historyId, entityTypeId, null);
    }

    public String hitFieldHistory(Long historyId, int entityTypeId, Boolean isAdmin) {
        String fieldHistoryResponse = null;
        try {
            HttpGet getRequest;
            String queryString = "/tblauditlogs/fieldHistory/" + historyId + "/" + entityTypeId;

            if (isAdmin != null) {
				queryString = queryString.concat("?isAdmin=" + isAdmin);
            }

            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.getRequest(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            fieldHistoryResponse = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Field History response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Field History Api. {}", e.getMessage());
        }
        return fieldHistoryResponse;
    }

    public String hitFieldHistory(String queryString, Boolean isAdmin) {
        String fieldHistoryResponse = null;
        try {
            HttpGet getRequest;
            if (isAdmin != null) {
                queryString = queryString.concat("?isAdmin=" + isAdmin);
            }

            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.getRequest(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            fieldHistoryResponse = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Field History response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Field History Api. {}", e.getMessage());
        }
        return fieldHistoryResponse;
    }

    public String hitFieldHistory(String queryString) {
        String fieldHistoryResponse = null;
        try {
            HttpGet getRequest;

            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            HttpResponse response = APIUtils.getRequest(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            fieldHistoryResponse = EntityUtils.toString(response.getEntity());


        } catch (Exception e) {
            logger.error("Exception while hitting Field History Api. {}", e.getMessage());
        }
        return fieldHistoryResponse;
    }
}
