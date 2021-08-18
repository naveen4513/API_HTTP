package com.sirionlabs.api.commonAPI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadContractTemplateInCDR extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(DownloadContractTemplateInCDR.class);
    CustomAssert customAssert = new CustomAssert();

    public HttpResponse downloadSystemTagged( int templateID, int entityTypeID){
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/download/draft/taggeddocument/"+templateID+"/"+entityTypeID;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            getRequest.addHeader("Accept-Language", "en-US,en;q=0.9");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

            response = super.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
        }catch(Exception e){
        logger.error("Exception {} occurred while fetching Select Template Response", e.getMessage());
        customAssert.assertTrue(false,"Exception {} occurred while fetching Select Template Response");
        }
        return response;
    }
}