package com.sirionlabs.api.sisenseBI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class IntlMetadata extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(IntlMetadata.class);
    String intlMetaDataResponseStr = null;

    public HttpResponse hitIntlMetaData(String serverName,String apitohit,Integer portNumber) throws IOException {
        HttpResponse response = null;
        String queryString = "/analytics/sirion/i18n/metadata/" + apitohit + "?servername=" + serverName;
        logger.debug("Query string url formed is {}", queryString);

        HttpGet getRequest = new HttpGet(queryString);
//        getRequest.addHeader("Accept", "application/json, text/plain, */*");
//        getRequest.addHeader("Accept-Encoding", "gzip, deflate");
        response = super.getRequest(getRequest,false,portNumber);

        intlMetaDataResponseStr = EntityUtils.toString(response.getEntity());
        logger.debug("The Response is : [ {} ]", intlMetaDataResponseStr);
        return response;
    }

    public String getintlMetaDataResponseStr() {
        return intlMetaDataResponseStr;
    }
}
