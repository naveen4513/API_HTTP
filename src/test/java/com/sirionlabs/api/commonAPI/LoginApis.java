package com.sirionlabs.api.commonAPI;


import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginApis extends APIUtils {

    public static final Logger logger = LoggerFactory.getLogger(LoginApis.class);
    public String apiResponse;

    public HttpResponse hitApiV1CsrfToken() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/api/v1/csrf/token";


            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {

        }
        return response;
    }

    public HttpResponse fieldLabelMessagesGetAll() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/fieldlabel/messages/get/all";


            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {

        }
        return response;
    }


    public HttpResponse fieldLabelMessagesGetList() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/fieldlabel/messages/get/list";


            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());


        } catch (Exception e) {

        }
        return response;
    }

    public HttpResponse fieldLabelSlaColor() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/fieldlabel/sla/color";


            logger.info("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());


        } catch (Exception e) {

        }
        return response;
    }

    public HttpResponse v1SideLayoutData() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/v1/sideLayout/data";


            logger.info("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {

        }
        return response;
    }

    public HttpResponse tblUsersAdditionalDetails() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/tblusers/additional/details";


            logger.info("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {

        }
        return response;
    }

    public HttpResponse uxSrcViewsHeaderRestBrandingHTML() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/ux/src/views/header-rest-branding.html";


            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {

        }
        return response;
    }

    public HttpResponse pendingActionsDailyCount() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/pending-actions/daily/count";


            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {

        }
        return response;
    }

    public HttpResponse authHelperGetAccessTokenBI() {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/v1/authHelper/getAccessToken";


            logger.debug("Query string url formed is {}", queryString);

            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest);
            logger.info("Response status is {}", response.getStatusLine().toString());
            apiResponse = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {

        }
        return response;
    }
}
