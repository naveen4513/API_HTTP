package com.sirionlabs.api.listRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListRendererTabListData extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(ListRendererTabListData.class);
    private String tabListDataJsonStr = null;
    private String apiResponseCode = null;

    public HttpResponse hitListRendererTabListData(int listId, int entityTypeId, int entityId, String payload) {
        return hitListRendererTabListData(listId, entityTypeId, entityId, payload, false);
    }

    public HttpResponse hitListRendererTabListData(int listId, int entityTypeId, int entityId, String payload, Boolean isAdmin) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/listRenderer/list/" + listId + "/tablistdata/" + entityTypeId + "/" + entityId;

            if(isAdmin != null) {
                queryString = queryString.concat("?isAdmin=" + isAdmin);
            }

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = postRequest(postRequest, payload);
            apiResponseCode = response.getStatusLine().toString();
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.tabListDataJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers)
                logger.debug("Create response header {}", oneHeader.toString());
        } catch (Exception e) {
            logger.error("Exception while hitting ListRendererTabListData Api. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitListRendererTabListMetaData(int tablistId, int entityTypeId, String payload) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/listRenderer/list/" + tablistId + "/defaultUserListMetaData/?entityTypeId=" + entityTypeId;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = super.postRequest(postRequest, payload);
            apiResponseCode = response.getStatusLine().toString();
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.tabListDataJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers)
                logger.debug("Create response header {}", oneHeader.toString());
        } catch (Exception e) {
            logger.error("Exception while hitting ListRendererTabListMetaData Api. {}", e.getMessage());
        }
        return response;
    }

    public String getTabListDataJsonStr() {
        return this.tabListDataJsonStr;
    }

    public String getAPIResponseCode() {

        return this.apiResponseCode;
    }

    public String getPayload(int entityTypeId, int offset, int size, String orderByColumnName, String orderDirection, String filterJson) {
        return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName +
                "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":" + filterJson + "}}";
    }
}
