package com.sirionlabs.api.presignature;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubmitDraft extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(SubmitDraft.class);
    String submitDraftJsonStr = null;

    public static String hitSubmitDraft(String entity, String payload) {
        try {
            HttpPost postRequest;
            String urlName = ConfigureConstantFields.getUrlNameForEntity(entity);
            String queryString = "/" + urlName + "/submitdraft?version=2.0";

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            return EntityUtils.toString(APIUtils.postRequest(postRequest, payload).getEntity());
        } catch (Exception e) {
            logger.error("Exception while hitting SubmitDraft Api. {}", e.getMessage());
        }
        return null;
    }

    public HttpResponse hitSubmitDraft(String payload) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/cdr/submitdraft";

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            submitDraftJsonStr = EntityUtils.toString(response.getEntity());
            logger.debug("response json is: {}", submitDraftJsonStr);

            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                logger.debug("SubmitDraft response header {}", headers[i].toString());
            }

        } catch (Exception e) {
            logger.error("Exception while hitting SubmitDraft Api. {}", e.getMessage());
        }
        return response;

    }

    public String getSubmitDraftJsonStr() {
        return this.submitDraftJsonStr;
    }
}