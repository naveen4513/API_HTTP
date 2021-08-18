package com.sirionlabs.api.invoice;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import java.util.Map;


public class InvoiceWorkFlowAction extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(InvoiceWorkFlowAction.class);
    public String responseBody;
    private Map<String, String> getHeaders() {

        Map<String, String> headers = new HashMap<>();

        try {
            headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Content-Type", "application/json;charset=UTF-8");
            headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        } catch (Exception e) {
            logger.info("Exception occurred in creating headers for request");
        }

        return headers;
    }

    public void activateInvoice(String payload,CustomAssert customAssert) {

        try {
            String createUrl = "/baseInvoice/activate?version=2.0";
            APIValidator apiValidator = executor.post(createUrl, getHeaders(), payload);
            APIResponse apiResponse = apiValidator.getResponse();
            responseBody = apiResponse.getResponseBody();
        } catch (Exception e) {
            logger.info("Exception found in creating Invoice Rule");
            customAssert.assertTrue(false, "Exception found in creating Invoice Rule");
        }
    }

    public void onHoldInvoice(String payload,CustomAssert customAssert) {

        try {
            String createUrl = "/baseInvoice/onhold?version=2.0";
            APIValidator apiValidator = executor.post(createUrl, getHeaders(), payload);
            APIResponse apiResponse = apiValidator.getResponse();
            responseBody = apiResponse.getResponseBody();
        } catch (Exception e) {
            logger.info("Exception found in creating Invoice Rule");
            customAssert.assertTrue(false, "Exception found in creating Invoice Rule");
        }
    }

    public void performWfActionInvoice(String actionName,String payload,CustomAssert customAssert) {

        try {
            String createUrl = "/baseInvoice/" + actionName + "?version=2.0";
            APIValidator apiValidator = executor.post(createUrl, getHeaders(), payload);
            APIResponse apiResponse = apiValidator.getResponse();
            responseBody = apiResponse.getResponseBody();
        } catch (Exception e) {
            logger.info("Exception found in creating Invoice Rule");
            customAssert.assertTrue(false, "Exception found in creating Invoice Rule");
        }
    }
}
