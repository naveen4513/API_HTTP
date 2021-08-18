package com.sirionlabs.api.download;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Downloadentitydata extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(Downloadentitydata.class);

    public HttpResponse hitDownloadListWithData(Map<String, String> formParam, String urlname,Integer id,Integer showpageId) {
        HttpResponse response = null;
        try {
            HttpPost postRequest;
            String queryString = "/" + urlname + "/download/" + id + "/" + showpageId + "/data";


            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            String params = UrlEncodedString.getUrlEncodedString(formParam);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, params);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                logger.debug("DownloadListWithData response header {}", headers[i].toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting DownloadListWithData Api. {}", e.getMessage());
        }
        return response;
    }
}
