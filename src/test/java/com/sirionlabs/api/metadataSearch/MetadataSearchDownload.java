package com.sirionlabs.api.metadataSearch;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MetadataSearchDownload extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(MetadataSearchDownload.class);

    public HttpResponse downloadMetadataSearchFile(int entityTypeId, String payload) {
        try {
            String queryString = "/metadatasearch/download/" + entityTypeId;
            Map<String, String> parameters = new HashMap<>();
            parameters.put("jsonData", payload);
            String params = UrlEncodedString.getUrlEncodedString(parameters);

            HttpPost postRequest;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            return postRequest(postRequest, params);
        } catch (Exception e) {
            logger.error("Exception while Downloading MetaDataSearch File. {}", e.getMessage());
            return null;
        }
    }
}
