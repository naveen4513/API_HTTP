package com.sirionlabs.api.listRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SLDetails extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(SLDetails.class);

    private String sLDetailsResponse = null;

    public HttpResponse hitSLDetailsGlobalList(String payload) {

        HttpResponse response = null;

        try {
            HttpPost postRequest;
            String queryString = "/sldetails/globallist";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.sLDetailsResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting SL Details Global List Api. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitAddException(String payload) {

        HttpResponse response = null;

        try {
            HttpPost postRequest;
            String queryString = "/sldetails/addException";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.sLDetailsResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting SL Details Add Exception API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitRemoveException(String payload) {

        HttpResponse response = null;

        try {
            HttpPost postRequest;
            String queryString = "/sldetails/removeException";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.sLDetailsResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting SL Details Remove Exception API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitUpdateData(String payload) {

        HttpResponse response = null;

        try {
            HttpPost postRequest;
            String queryString = "/sldetails/updateData";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.sLDetailsResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting SL Details Update Details API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitRemoveNoise(String payload) {

        HttpResponse response = null;

        try {
            HttpPost postRequest;
            String queryString = "/sldetails/removeNoise";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.sLDetailsResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting SL Details Update Details API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitIgnorePerformanceDataFiles(String payload) {

        HttpResponse response = null;

        try {
            HttpPost postRequest;
            String queryString = "/sldetails/ignorePerformanceDataFiles";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.sLDetailsResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting SL Details ignorePerformanceDataFiles API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitViewTemplate(int cslID,String payload) {

        HttpResponse response = null;

        try {
            HttpPost postRequest;
            String queryString = "/sldetails/viewTemplate/" + cslID;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.sLDetailsResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting SL Details view Template API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitDownloadRawData(String sOutputFile,Map<String, String> formDataMap) {

        HttpResponse response = null;

        try {
            //Generate HttpHost using API Utils
            HttpHost target = generateHttpTargetHost();
            HttpPost postRequest;
            String queryString = "/sldetails/downloadRawdata";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            //Generate Http Post Request
            String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3";
            String contentTypeHeader = "application/x-www-form-urlencoded";
            HttpPost httpPostRequest = generateHttpPostRequestWithQueryString(queryString, acceptsHeader, contentTypeHeader);

            //Generate Name Value Pair Form Data Entity
            HttpEntity nameValuePairFormDataEntity = generateNameValuePairFormDataEntity(formDataMap);

            //Add the nave value pair form data entity with post request
            httpPostRequest.setEntity(nameValuePairFormDataEntity);

            //Call the DownloadFile method
            return downloadAPIResponseFile(sOutputFile, target, httpPostRequest);


        } catch (Exception e) {
            logger.error("Exception while hitting SL Details downloadRawdata API. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitSLDetailsList(String payload) {

        HttpResponse response = null;

        try {
            HttpPost postRequest;
            String queryString = "/sldetails/list";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.postRequest(postRequest, payload);

            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.sLDetailsResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting SL Details Global List Api. {}", e.getMessage());
        }
        return response;
    }
    public String getSLDetailsResponseStr() {
        return this.sLDetailsResponse;
    }


}
